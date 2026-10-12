package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.jira.ProviderStatusRepository;
import com.example.EVIM.adapter.out.persistence.entity.ProviderConfig;
import jakarta.annotation.PostConstruct;
import com.example.EVIM.domain.port.out.ProviderConfigPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderConfigService implements ProviderConfigPort {

    private static final Map<String, String[]> DEFAULTS = Map.of(
            "jira.base-url",            new String[]{"https://your-instance.atlassian.net", "URL base da instância do Jira"},
            "jira.project-key",         new String[]{"TDJ", "Chave do projeto no Jira (ex: EVIM)"},
            "jira.vendor-ticket-field", new String[]{"customfield_10106", "Campo personalizado que armazena o ID do vendor"},
            "sla.internal.domain",      new String[]{"empresa.com", "Domínio de email interno (ex: suporte@empresa.com)"},
            "status.color.open",        new String[]{"#d97706", "Cor da pílula: Aberto"},
            "status.color.progress",    new String[]{"#3b82f6", "Cor da pílula: Em andamento"},
            "status.color.review",      new String[]{"#6b52d5", "Cor da pílula: Em revisão"},
            "status.color.blocked",     new String[]{"#ef4444", "Cor da pílula: Bloqueado"},
            "status.color.done",        new String[]{"#22c55e", "Cor da pílula: Concluído"}
    );

    private static final Map<String, String[]> EXTRA_DEFAULTS = Map.of(
            "jira.sprints-enabled", new String[]{"false", "Ativa o seletor de sprint na sidebar e em métricas"},
            "jira.board-id",        new String[]{"0",     "ID do board Scrum no Jira (necessário para filtro de sprints)"}
    );

    private final ProviderConfigRepository repository;
    private final ProviderStatusRepository statusRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ProviderConfigService(ProviderConfigRepository repository,
                                 @Lazy ProviderStatusRepository statusRepository) {
        this.repository       = repository;
        this.statusRepository = statusRepository;
    }

    @PostConstruct
    public void init() {
        DEFAULTS.forEach((key, values) ->
                repository.findByKey(key).orElseGet(() ->
                        repository.save(ProviderConfig.builder()
                                .key(key).value(values[0]).description(values[1]).build())
                )
        );
        EXTRA_DEFAULTS.forEach((key, values) ->
                repository.findByKey(key).orElseGet(() ->
                        repository.save(ProviderConfig.builder()
                                .key(key).value(values[0]).description(values[1]).build())
                )
        );
        reloadCache();
    }

    private void reloadCache() {
        cache.clear();
        repository.findAll().forEach(c -> cache.put(c.getKey(), c.getValue()));
    }

    public String getValue(String key) {
        String value = cache.get(key);
        if (value == null)
            throw new RuntimeException("Config not found: " + key);
        return value;
    }

    public String getBaseUrl()          {
        return getValue("jira.base-url");
    }

    public String getProjectKey()       {
        return getValue("jira.project-key");
    }
    public String getVendorTicketField(){
        return getValue("jira.vendor-ticket-field");
    }

    public String getInternalDomain()  { return getValue("sla.internal.domain"); }
    public long getBoardId()           { return Long.parseLong(cache.getOrDefault("jira.board-id", "0")); }
    public boolean isSprintsEnabled()  { return Boolean.parseBoolean(cache.getOrDefault("jira.sprints-enabled", "false")); }

    public List<ProviderConfig> findAll() {
        return repository.findAll();
    }

    public ProviderConfig save(String key, String value, String description) {
        ProviderConfig config = repository.findByKey(key).orElseGet(ProviderConfig::new);
        config.setKey(key);
        config.setValue(value);
        if (description != null) config.setDescription(description);
        ProviderConfig saved = repository.save(config);
        cache.put(key, value);
        if ("jira.project-key".equals(key) || "jira.base-url".equals(key)) {
            statusRepository.invalidateCache();
        }
        return saved;
    }

    public void delete(Long id) {
        repository.findById(id).ifPresent(c -> cache.remove(c.getKey()));
        repository.deleteById(id);
    }
}