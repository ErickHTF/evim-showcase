package com.example.EVIM.adapter.out.graph;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.example.EVIM.domain.model.Attachment;
import com.example.EVIM.domain.model.Email;
import com.example.EVIM.domain.port.out.EmailFetchPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(name = "graph.mock.enabled", havingValue = "false", matchIfMissing = true)
public class GraphMailFetchAdapter implements EmailFetchPort {

    private static final Logger log = LoggerFactory.getLogger(GraphMailFetchAdapter.class);
    private static final int PAGE_SIZE = 50;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final GraphServiceClient graphClient;
    private final GraphEmailMapper mapper;
    private final ClientSecretCredential credential;
    private final HttpClient httpClient;

    @Value("${graph.mailbox-user-id}")
    private String mailboxUserId;

    @Value("${graph.folder-id}")
    private String folderId;

    public GraphMailFetchAdapter(GraphServiceClient graphClient,
                                  GraphEmailMapper mapper,
                                  ClientSecretCredential credential) {
        this.graphClient = graphClient;
        this.mapper      = mapper;
        this.credential  = credential;
        this.httpClient  = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    private String getAccessToken() {
        TokenRequestContext ctx = new TokenRequestContext();
        ctx.addScopes("https://graph.microsoft.com/.default");
        return credential.getToken(ctx).block().getToken();
    }

    @Override
    public int fetchNewEmails(Consumer<List<Email>> pageConsumer) {
        int totalFetched = 0;
        int page = 0;

        String token = getAccessToken();
        log.info("Token acquired, calling Graph API via HttpClient — user={}, folder={}", mailboxUserId, folderId);

        String select = "id,subject,sender,toRecipients,body,receivedDateTime,hasAttachments";
        String filter = URLEncoder.encode("isRead eq false", StandardCharsets.UTF_8);




        String orderBy = URLEncoder.encode("receivedDateTime asc", StandardCharsets.UTF_8);
        String url = String.format(
                "https://graph.microsoft.com/v1.0/users/%s/mailFolders/%s/messages?$select=%s&$filter=%s&$orderby=%s&$top=%d",
                mailboxUserId, folderId, select, filter, orderBy, PAGE_SIZE);

        while (url != null) {
            long start = System.currentTimeMillis();
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long elapsed = System.currentTimeMillis() - start;

                if (resp.statusCode() != 200) {
                    log.error("Graph API returned {} in {}ms: {}", resp.statusCode(), elapsed, resp.body().substring(0, Math.min(500, resp.body().length())));
                    break;
                }

                JsonNode root = JSON.readTree(resp.body());
                JsonNode values = root.get("value");
                if (values == null || !values.isArray() || values.isEmpty()) {
                    log.info("Graph API returned 0 messages in {}ms — done", elapsed);
                    break;
                }

                List<Email> pageEmails = new ArrayList<>();
                for (JsonNode node : values) {
                    Email email = mapper.fromJsonNode(node);
                    if (node.has("hasAttachments") && node.get("hasAttachments").asBoolean()) {
                        email.setAttachments(fetchAttachments(email.getMessageId()));
                    }
                    pageEmails.add(email);
                }

                page++;
                totalFetched += pageEmails.size();
                log.info("Fetched page {} ({} emails, {}ms, total so far: {})", page, pageEmails.size(), elapsed, totalFetched);

                pageConsumer.accept(pageEmails);


                JsonNode nextLink = root.get("@odata.nextLink");
                if (nextLink != null && !nextLink.isNull()) {
                    String next = nextLink.asText();
                    if (next.startsWith("https://graph.microsoft.com/")) {
                        url = next;
                    } else {
                        log.warn("Ignoring untrusted @odata.nextLink: {}", next);
                        url = null;
                    }
                } else {
                    url = null;
                }

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                log.error("Graph API call failed after {}ms: {}", elapsed, e.getMessage(), e);
                break;
            }
        }

        log.info("Total fetched: {} unread emails in {} pages", totalFetched, page);
        return totalFetched;
    }

    @Override
    public void markAsRead(String messageId) {
        Message message = new Message();
        message.setIsRead(true);
        graphClient
                .users().byUserId(mailboxUserId)
                .mailFolders().byMailFolderId(folderId)
                .messages().byMessageId(messageId)
                .patch(message);
    }

    private List<Attachment> fetchAttachments(String messageId) {
        var response = graphClient
                .users().byUserId(mailboxUserId)
                .mailFolders().byMailFolderId(folderId)
                .messages().byMessageId(messageId)
                .attachments()
                .get();

        if (response == null || response.getValue() == null) return List.of();

        return response.getValue().stream()
                .filter(a -> a instanceof FileAttachment)
                .map(a -> mapper.toAttachmentDomain((FileAttachment) a))
                .toList();
    }
}
