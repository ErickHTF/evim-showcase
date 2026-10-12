package com.example.EVIM.adapter.in.web;

import com.example.EVIM.application.EmailIngestionService;
import com.example.EVIM.application.ThreadReparseService;
import com.example.EVIM.application.metrics.ParserMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final int REPARSE_CHUNK_SIZE = 200;
    private static final int REPARSE_PARALLELISM = Math.max(2,
            Math.min(8, Runtime.getRuntime().availableProcessors()));

    private final EmailIngestionService ingestionService;
    private final ThreadReparseService reparseService;
    private final ParserMetrics parserMetrics;

    private final AtomicBoolean reparseRunning = new AtomicBoolean(false);
    private final AtomicBoolean reparseCancel = new AtomicBoolean(false);
    private final AtomicInteger reparseProgress = new AtomicInteger(0);
    private final AtomicInteger reparseTotal = new AtomicInteger(0);
    private volatile Map<String, Object> lastReparseResult = null;
    private volatile String lastReparseError = null;
    private volatile Instant lastReparseStartedAt = null;
    private volatile Instant lastReparseFinishedAt = null;

    public AdminController(EmailIngestionService ingestionService,
                           ThreadReparseService reparseService,
                           ParserMetrics parserMetrics) {
        this.ingestionService = ingestionService;
        this.reparseService = reparseService;
        this.parserMetrics = parserMetrics;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Void> triggerIngest() {
        ingestionService.ingestNewEmails();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/parser-metrics")
    public ResponseEntity<Map<String, Object>> parserMetrics() {
        return ResponseEntity.ok(parserMetrics.snapshot());
    }

    @DeleteMapping("/parser-metrics")
    public ResponseEntity<Void> resetParserMetrics() {
        parserMetrics.reset();
        return ResponseEntity.noContent().build();
    }


    public record ReparseScopeRequest(List<Long> conversationIds, List<Long> emailIds) {}

    @PostMapping("/reparse-threads")
    public ResponseEntity<Map<String, Object>> reparseThreadMessages(
            @RequestBody(required = false) ReparseScopeRequest scope) {
        if (!reparseRunning.compareAndSet(false, true)) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "already_running");
            status.put("progress", reparseProgress.get());
            status.put("total", reparseTotal.get());
            return ResponseEntity.ok(status);
        }

        reparseCancel.set(false);
        reparseProgress.set(0);
        reparseTotal.set(0);
        lastReparseResult = null;
        lastReparseError = null;
        lastReparseStartedAt = Instant.now();
        lastReparseFinishedAt = null;

        final List<Long> convIds = scope != null && scope.conversationIds() != null
                ? scope.conversationIds() : List.of();
        final List<Long> mailIds = scope != null && scope.emailIds() != null
                ? scope.emailIds() : List.of();
        final boolean scoped = !convIds.isEmpty() || !mailIds.isEmpty();

        Thread.startVirtualThread(() -> runReparse(convIds, mailIds));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("scope", scoped ? "scoped" : "all");
        if (scoped) {
            response.put("conversationIds", convIds);
            response.put("emailIds", mailIds);
        }
        response.put("message", "Reparse started in background. GET /admin/reparse-status to check progress.");
        return ResponseEntity.accepted().body(response);
    }

    @DeleteMapping("/reparse-threads")
    public ResponseEntity<Map<String, Object>> cancelReparse() {
        Map<String, Object> response = new HashMap<>();
        if (!reparseRunning.get()) {
            response.put("status", "not_running");
            return ResponseEntity.ok(response);
        }
        reparseCancel.set(true);
        response.put("status", "cancel_requested");
        response.put("progress", reparseProgress.get());
        response.put("total", reparseTotal.get());
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/reparse-status")
    public ResponseEntity<Map<String, Object>> reparseStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", reparseRunning.get());
        status.put("cancelRequested", reparseCancel.get());
        status.put("progress", reparseProgress.get());
        status.put("total", reparseTotal.get());
        if (lastReparseStartedAt != null) status.put("startedAt", lastReparseStartedAt.toString());
        if (lastReparseFinishedAt != null) status.put("finishedAt", lastReparseFinishedAt.toString());
        if (lastReparseResult != null) status.put("result", lastReparseResult);
        if (lastReparseError != null) status.put("error", lastReparseError);
        return ResponseEntity.ok(status);
    }

    private void runReparse(List<Long> conversationIds, List<Long> emailIds) {
        boolean scoped = (conversationIds != null && !conversationIds.isEmpty())
                || (emailIds != null && !emailIds.isEmpty());
        log.info("Starting thread messages reparse (scope={}) with parallelism={}",
                scoped ? "scoped" : "all", REPARSE_PARALLELISM);
        ThreadReparseService.Counters counters = new ThreadReparseService.Counters();
        boolean cancelled = false;
        ExecutorService pool = Executors.newFixedThreadPool(REPARSE_PARALLELISM, r -> {
            Thread t = new Thread(r, "reparse-worker");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Long> allIds = reparseService.resolveScopedEmailIds(conversationIds, emailIds);
            reparseTotal.set(allIds.size());

            List<Future<?>> futures = new ArrayList<>();
            for (int from = 0; from < allIds.size(); from += REPARSE_CHUNK_SIZE) {
                final int chunkFrom = from;
                final int chunkTo = Math.min(from + REPARSE_CHUNK_SIZE, allIds.size());


                final List<Long> chunk = List.copyOf(allIds.subList(chunkFrom, chunkTo));
                futures.add(pool.submit(() -> {
                    if (reparseCancel.get()) return;
                    try {
                        reparseService.reparseChunk(chunk, counters);
                    } catch (Exception chunkEx) {
                        log.error("Reparse chunk [{}..{}] failed: {}", chunkFrom, chunkTo,
                                chunkEx.getMessage(), chunkEx);
                        counters.failed.addAndGet(chunk.size());
                    } finally {
                        reparseProgress.set(Math.min(
                                counters.processed.get() + counters.failed.get(),
                                reparseTotal.get()));
                    }
                }));
            }

            for (Future<?> f : futures) {
                if (reparseCancel.get()) {
                    cancelled = true;
                    f.cancel(true);
                    continue;
                }
                try {
                    f.get();
                } catch (Exception e) {
                    log.warn("Reparse worker future failed: {}", e.getMessage());
                }
            }

            log.info("Reparse {}: {} updated, {} fallback, {} failed, {} newly parsed",
                    cancelled ? "cancelled" : "complete",
                    counters.updated.get(), counters.fallback.get(),
                    counters.failed.get(), counters.newlyParsed.get());

            Map<String, Object> result = new HashMap<>();
            result.put("status", cancelled ? "cancelled" : "complete");
            result.put("updatedMessages", counters.updated.get());
            result.put("fallbackEmails", counters.fallback.get());
            result.put("failedEmails", counters.failed.get());
            result.put("newlyParsed", counters.newlyParsed.get());
            result.put("totalEmails", reparseTotal.get());
            result.put("processed", counters.processed.get());
            lastReparseResult = result;
        } catch (Exception e) {
            log.error("Reparse aborted: {}", e.getMessage(), e);
            lastReparseError = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            lastReparseFinishedAt = Instant.now();
            reparseRunning.set(false);
        }
    }
}
