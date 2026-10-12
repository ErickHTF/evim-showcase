import { useState, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import s from "./css/AdminPanel.module.css";
import SecondaryLayout from "./SecondaryLayout";
import Tooltip from "./Tooltip";
import {
  METRICS_DEFAULTS, INTERVAL_OPTIONS, COLOR_FIELDS, THRESHOLD_FIELDS,
  loadMetricsSettings, saveMetricsSettings,
} from "../services/metricsSettings";
import { getAdminConfigs, updateAdminConfig, getStatuses } from "../services/api";
import { STATUS_PALETTE } from "../constants/colors";
import { statusSlug } from "../utils/string";
import { useFeedback } from "../hooks/useFeedback";

const GROUPS = [
  {
    id: 'jira',
    title: 'Integrações Jira',
    desc: 'Conexão e mapeamento de campos com o Jira Cloud',
    color: '#2684ff',
    icon: (
      <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M11.53 2c0 2.4 1.97 4.35 4.35 4.35h1.78v1.72a4.35 4.35 0 0 0 4.34 4.35V2.84a.84.84 0 0 0-.84-.84zM7.21 6.3c0 2.4 1.95 4.34 4.34 4.34h1.78v1.72a4.35 4.35 0 0 0 4.35 4.35V7.15a.84.84 0 0 0-.85-.84zM2.89 10.61c0 2.4 1.95 4.35 4.34 4.35h1.78v1.72a4.35 4.35 0 0 0 4.35 4.34v-9.55a.85.85 0 0 0-.85-.85z"/></svg>
    ),
    keys: ['jira.base-url', 'jira.project-key', 'jira.vendor-ticket-field', 'jira.sprints-enabled', 'jira.board-id'],
  },
  {
    id: 'access',
    title: 'Acesso e SLA',
    desc: 'Domínios internos e identificação de remetentes',
    color: '#22c55e',
    icon: (
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
    ),
    keys: ['sla.internal.domain'],
  },
];

const INPUT_WIDTHS = {
  'jira.base-url':            340,
  'jira.project-key':         120,
  'jira.vendor-ticket-field': 180,
  'sla.internal.domain':      220,
  'jira.board-id':            120,
};

function DescriptionWithTooltip({ description }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
      <span className={s.rowDesc}>{description}</span>
      <Tooltip content={description} position="right" delay={100}>
        <button
          className={s.helpIcon}
          title="Mais informações"
          type="button"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 16v-4" />
            <path d="M12 8h.01" />
          </svg>
        </button>
      </Tooltip>
    </div>
  );
}

export default function AdminPanel() {
  const queryClient = useQueryClient();
  const [configs, setConfigs]         = useState([]);
  const [editing, setEditing]         = useState({});
  const [jiraStatuses, setJiraStatuses]   = useState([]);
  const [statusColors, setStatusColors]   = useState({});
  const [restoring, setRestoring]     = useState(false);
  const [metricsSettings, setMetricsSettings] = useState(loadMetricsSettings);

  const [feedback,       showFeedback]       = useFeedback();
  const [colorFeedback,  showColorFeedback]  = useFeedback();
  const [metricsFeedback, showMetricsFeedback] = useFeedback();

  useEffect(() => {
    getAdminConfigs().then(data => {
      setConfigs(data);
      const colors = {};
      data.forEach(c => { if (c.key.startsWith('status.color.')) colors[c.key] = c.value; });
      setStatusColors(colors);
    }).catch(console.error);
    getStatuses().then(setJiraStatuses).catch(console.error);
  }, []);

  const configMap = Object.fromEntries(configs.map(c => [c.key, c]));

  function handleChange(key, value) {
    setEditing(prev => ({ ...prev, [key]: value }));
  }

  function handleSave(key) {
    const cfg = configMap[key];
    if (!cfg) return;
    updateAdminConfig(key, { value: editing[key] ?? cfg.value, description: cfg.description })
      .then(updated => {
        setConfigs(prev => prev.map(c => c.key === updated.key ? updated : c));
        setEditing(prev => { const e = { ...prev }; delete e[key]; return e; });
        if (key === 'jira.board-id') queryClient.invalidateQueries({ queryKey: ['sprints'] });
        showFeedback(key, true);
      })
      .catch(() => showFeedback(key, false));
  }

  function handleColorChange(status, value) {
    setStatusColors(prev => ({ ...prev, ['status.color.' + statusSlug(status)]: value }));
  }

  function handleColorSave(status) {
    const key = 'status.color.' + statusSlug(status);
    updateAdminConfig(key, { value: statusColors[key] ?? '#888888', description: `Cor da pílula: ${status}` })
      .then(() => showColorFeedback(status, true))
      .catch(() => showColorFeedback(status, false));
  }

  function handleRestoreColors() {
    setRestoring(true);
    const saves = jiraStatuses.map((name, i) => {
      const key = 'status.color.' + statusSlug(name);
      const color = STATUS_PALETTE[i % STATUS_PALETTE.length];
      return updateAdminConfig(key, { value: color, description: `Cor da pílula: ${name}` })
        .then(() => [key, color]);
    });
    Promise.all(saves)
      .then(pairs => {
        const updated = Object.fromEntries(pairs);
        setStatusColors(prev => ({ ...prev, ...updated }));
      })
      .finally(() => setRestoring(false));
  }

  function handleMetricsColor(key, value) {
    setMetricsSettings(prev => ({ ...prev, colors: { ...prev.colors, [key]: value } }));
  }

  function saveMetricsColor(key) {
    saveMetricsSettings(metricsSettings);
    showMetricsFeedback(key, true);
  }

  function handleMetricsInterval(value) {
    const next = { ...metricsSettings, refetchMs: value };
    setMetricsSettings(next);
    saveMetricsSettings(next);
  }

  function handleRestoreMetrics() {
    const next = { ...metricsSettings, colors: METRICS_DEFAULTS.colors };
    setMetricsSettings(next);
    saveMetricsSettings(next);
  }

  const [metricsColorsOpen, setMetricsColorsOpen] = useState(false);
  const [pillColorsOpen, setPillColorsOpen]       = useState(false);
  const [adminTab, setAdminTab]                   = useState('config');

  const knownKeys = new Set(GROUPS.flatMap(g => g.keys));

  const HIDDEN_KEYS = new Set(['jira.sprint-field']);

  const extraConfigs = configs.filter(c =>
    !knownKeys.has(c.key) && !c.key.startsWith('status.color.') && !HIDDEN_KEYS.has(c.key)
  );

  return (
    <SecondaryLayout>
    <div className={s.page}>
      <div className={s.pageHeader}>
        <div>
          <h1 className={s.pageTitle}>Configurações do Sistema</h1>
          <p className={s.pageDesc}>Gerencie integrações, domínios e aparência do EVIM</p>
        </div>
        <div className={s.adminTabs}>
          {[
            { id: 'config', label: 'Configuração' },
            { id: 'visual', label: 'Visual' },
          ].map(t => (
            <button
              key={t.id}
              className={s.adminTab + (adminTab === t.id ? ` ${s.adminTabActive}` : '')}
              onClick={() => setAdminTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      <div className={s.groups}>

        {adminTab === 'config' && GROUPS.map(group => (
          <div key={group.id} className={s.card}>
            <div className={s.cardHeader}>
              <span className={s.cardIcon} style={{ background: group.color + '18', color: group.color }}>
                {group.icon}
              </span>
              <div>
                <div className={s.cardTitle}>{group.title}</div>
                <div className={s.cardDesc}>{group.desc}</div>
              </div>
            </div>
            <div className={s.cardBody}>
              {group.keys.map((key, idx) => {
                const cfg = configMap[key];
                if (!cfg) return null;
                const rowStyle = idx > 0 ? { borderTop: '1px solid var(--border)' } : {};

                if (key === 'jira.board-id' && configMap['jira.sprints-enabled']?.value !== 'true') return null;

                if (key === 'jira.sprints-enabled') {
                  const enabled = cfg.value === 'true';
                  return (
                    <div key={key} className={s.row} style={rowStyle}>
                      <div className={s.rowMeta}>
                        <code className={s.rowKey}>{key}</code>
                        <DescriptionWithTooltip description={cfg.description} />
                      </div>
                      <div className={s.rowControl}>
                        <button
                          className={s.saveBtn}
                          style={enabled ? { background: 'var(--active)', color: 'var(--green-700)', borderColor: 'var(--green-200)', fontWeight: 600 } : {}}
                          onClick={() =>
                            updateAdminConfig(key, { value: String(!enabled), description: cfg.description })
                              .then(updated => {
                                setConfigs(prev => prev.map(c => c.key === updated.key ? updated : c));
                                queryClient.invalidateQueries({ queryKey: ['sprints'] });
                              })
                              .catch(console.error)
                          }
                        >
                          {enabled ? 'Ativado' : 'Desativado'}
                        </button>
                      </div>
                    </div>
                  );
                }

                const val = editing[key] ?? cfg.value;
                const width = INPUT_WIDTHS[key] ?? 240;
                return (
                  <div key={key} className={s.row} style={rowStyle}>
                    <div className={s.rowMeta}>
                      <code className={s.rowKey}>{key}</code>
                      <DescriptionWithTooltip description={cfg.description} />
                    </div>
                    <div className={s.rowControl}>
                      <input
                        className={s.input}
                        style={{ width }}
                        value={val}
                        onChange={e => handleChange(key, e.target.value)}
                        placeholder={cfg.value}
                      />
                      <button
                        className={`${s.saveBtn}${feedback[key] === 'ok' ? ` ${s.saveBtnOk}` : feedback[key] === 'err' ? ` ${s.saveBtnErr}` : ''}`}
                        onClick={() => handleSave(key)}
                      >Salvar</button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {adminTab === 'visual' && jiraStatuses.length > 0 && (
          <div className={s.card}>
            <div className={s.cardHeader}>
              <span className={s.cardIcon} style={{ background: '#8b5cf618', color: '#8b5cf6' }}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="13.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="10.5" r="2.5"/>
                  <circle cx="8.5" cy="7.5" r="2.5"/><circle cx="6.5" cy="12.5" r="2.5"/>
                  <path d="M12 20v-4"/><path d="M12 16c-4 0-6-2-6-2s2 2 6 2 6-2 6-2-2 2-6 2z"/>
                </svg>
              </span>
              <div style={{ flex: 1 }}>
                <div className={s.cardTitle}>Aparência</div>
                <div className={s.cardDesc}>Cor das pílulas de status por nome real do Jira</div>
              </div>
              <button className={s.restoreBtn} onClick={handleRestoreColors} disabled={restoring}>
                {restoring ? 'Restaurando…' : 'Restaurar padrões'}
              </button>
            </div>
            <div className={s.cardBody}>
              <div className={s.row} style={{ cursor: 'pointer' }}
                onClick={() => setPillColorsOpen(o => !o)}>
                <div className={s.rowMeta}>
                  <span className={s.rowDesc} style={{ fontWeight: 500, color: 'var(--fg-2)' }}>Cores das pílulas</span>
                </div>
                <div className={s.rowControl} style={{ color: 'var(--fg-4)', fontSize: 12, gap: 6 }}>
                  {pillColorsOpen ? 'Recolher' : 'Expandir'}
                  <span style={{ display: 'inline-block', transition: 'transform 200ms', transform: pillColorsOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}>▾</span>
                </div>
              </div>
              {pillColorsOpen && jiraStatuses.map((status, idx) => {
                const key = 'status.color.' + statusSlug(status);
                const color = statusColors[key] ?? STATUS_PALETTE[idx % STATUS_PALETTE.length];
                return (
                  <div key={status} className={s.row} style={{ borderTop: '1px solid var(--border)' }}>
                    <div className={s.rowMeta}>
                      <span className={s.statusPreview} style={{ background: color + '22', color, border: `1px solid ${color}44` }}>
                        <span style={{ width: 7, height: 7, borderRadius: '50%', background: color, display: 'inline-block', marginRight: 5 }} />
                        {status}
                      </span>
                    </div>
                    <div className={s.rowControl}>
                      <input
                        type="color"
                        className={s.colorInput}
                        value={color}
                        onChange={e => handleColorChange(status, e.target.value)}
                      />
                      <button
                        className={`${s.saveBtn}${colorFeedback[status] === 'ok' ? ` ${s.saveBtnOk}` : colorFeedback[status] === 'err' ? ` ${s.saveBtnErr}` : ''}`}
                        onClick={() => handleColorSave(status)}
                      >Salvar</button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {adminTab === 'config' && (
          <div className={s.card}>
            <div className={s.cardHeader}>
              <span className={s.cardIcon} style={{ background: '#3a6b8a18', color: '#3a6b8a' }}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </span>
              <div>
                <div className={s.cardTitle}>Métricas de SLA</div>
                <div className={s.cardDesc}>Intervalo de atualização e limites de alerta do dashboard</div>
              </div>
            </div>
            <div className={s.cardBody}>
              <div className={s.row}>
                <div className={s.rowMeta}>
                  <span className={s.rowKey}>polling.interval</span>
                  <span className={s.rowDesc}>Frequência de atualização automática da página de métricas</span>
                </div>
                <div className={s.rowControl}>
                  {INTERVAL_OPTIONS.map(opt => (
                    <button
                      key={opt.value}
                      className={s.saveBtn}
                      style={metricsSettings.refetchMs === opt.value
                        ? { background: 'var(--active)', color: 'var(--green-700)', borderColor: 'var(--green-200)', fontWeight: 600 }
                        : {}}
                      onClick={() => handleMetricsInterval(opt.value)}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <p className={s.hint}>Limites de Alerta — valores em minutos</p>
            {THRESHOLD_FIELDS.map(f => {
              const t = metricsSettings.thresholds?.[f.key] ?? METRICS_DEFAULTS.thresholds[f.key];
              const target = metricsSettings.targets?.[f.key] ?? '';
              return (
                <div key={f.key} className={s.thresholdRow} style={{ borderTop: '1px solid var(--border)' }}>
                  <span className={s.thresholdLabel}>{f.label}</span>
                  <label className={s.thresholdField}>
                    <span>Aviso (min)</span>
                    <input type="number" min={0} className={s.thresholdInput}
                      value={Math.round(t.warn / 60)}
                      onChange={e => setMetricsSettings(prev => ({
                        ...prev,
                        thresholds: { ...prev.thresholds, [f.key]: { ...prev.thresholds[f.key], warn: +e.target.value * 60 } }
                      }))} />
                  </label>
                  <label className={s.thresholdField}>
                    <span>Crítico (min)</span>
                    <input type="number" min={0} className={s.thresholdInput}
                      value={Math.round(t.crit / 60)}
                      onChange={e => setMetricsSettings(prev => ({
                        ...prev,
                        thresholds: { ...prev.thresholds, [f.key]: { ...prev.thresholds[f.key], crit: +e.target.value * 60 } }
                      }))} />
                  </label>
                  <label className={s.thresholdField}>
                    <span>Meta</span>
                    <input type="text" placeholder="ex: < 30m" className={s.thresholdInput}
                      value={target}
                      onChange={e => setMetricsSettings(prev => ({
                        ...prev,
                        targets: { ...prev.targets, [f.key]: e.target.value }
                      }))} />
                  </label>
                  <button
                    className={`${s.saveBtn}${metricsFeedback[f.key] === 'ok' ? ` ${s.saveBtnOk}` : ''}`}
                    onClick={() => { saveMetricsSettings(metricsSettings); showMetricsFeedback(f.key, true); }}
                  >Salvar</button>
                </div>
              );
            })}
          </div>
        )}

        {adminTab === 'visual' && (
          <div className={s.card}>
            <div className={s.cardHeader}>
              <span className={s.cardIcon} style={{ background: '#3a6b8a18', color: '#3a6b8a' }}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
                </svg>
              </span>
              <div style={{ flex: 1 }}>
                <div className={s.cardTitle}>Métricas de SLA</div>
                <div className={s.cardDesc}>Cores dos cards do dashboard</div>
              </div>
              <button className={s.restoreBtn} onClick={handleRestoreMetrics}>Restaurar padrões</button>
            </div>
            <div className={s.cardBody}>
              <div className={s.row} style={{ cursor: 'pointer' }}
                onClick={() => setMetricsColorsOpen(o => !o)}>
                <div className={s.rowMeta}>
                  <span className={s.rowDesc} style={{ fontWeight: 500, color: 'var(--fg-2)' }}>Cores dos cards</span>
                </div>
                <div className={s.rowControl} style={{ color: 'var(--fg-4)', fontSize: 12, gap: 6 }}>
                  {metricsColorsOpen ? 'Recolher' : 'Expandir'}
                  <span style={{ display: 'inline-block', transition: 'transform 200ms', transform: metricsColorsOpen ? 'rotate(180deg)' : 'rotate(0deg)' }}>▾</span>
                </div>
              </div>
              {metricsColorsOpen && COLOR_FIELDS.map((field) => {
                const color = metricsSettings.colors[field.key] ?? METRICS_DEFAULTS.colors[field.key];
                return (
                  <div key={field.key} className={s.row} style={{ borderTop: '1px solid var(--border)' }}>
                    <div className={s.rowMeta}>
                      <span className={s.statusPreview} style={{ background: color + '22', color, border: `1px solid ${color}44` }}>
                        <span style={{ width: 7, height: 7, borderRadius: '50%', background: color, display: 'inline-block', marginRight: 5 }} />
                        {field.label}
                      </span>
                    </div>
                    <div className={s.rowControl}>
                      <input
                        type="color"
                        className={s.colorInput}
                        value={color}
                        onChange={e => handleMetricsColor(field.key, e.target.value)}
                      />
                      <button
                        className={`${s.saveBtn}${metricsFeedback[field.key] === 'ok' ? ` ${s.saveBtnOk}` : ''}`}
                        onClick={() => saveMetricsColor(field.key)}
                      >Salvar</button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {adminTab === 'config' && extraConfigs.length > 0 && (
          <div className={s.card}>
            <div className={s.cardHeader}>
              <span className={s.cardIcon} style={{ background: '#6b728018', color: '#6b7280' }}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="3"/><path d="M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4M4.22 19.78l2.83-2.83M16.95 7.05l2.83-2.83"/>
                </svg>
              </span>
              <div>
                <div className={s.cardTitle}>Avançado</div>
                <div className={s.cardDesc}>Configurações adicionais do sistema</div>
              </div>
            </div>
            <div className={s.cardBody}>
              {extraConfigs.map((cfg, idx) => (
                <div key={cfg.key} className={s.row} style={idx > 0 ? { borderTop: '1px solid var(--border)' } : {}}>
                  <div className={s.rowMeta}>
                    <code className={s.rowKey}>{cfg.key}</code>
                    <DescriptionWithTooltip description={cfg.description} />
                  </div>
                  <div className={s.rowControl}>
                    <input
                      className={s.input}
                      style={{ width: 240 }}
                      value={editing[cfg.key] ?? cfg.value}
                      onChange={e => handleChange(cfg.key, e.target.value)}
                    />
                    <button
                      className={`${s.saveBtn}${feedback[cfg.key] === 'ok' ? ` ${s.saveBtnOk}` : feedback[cfg.key] === 'err' ? ` ${s.saveBtnErr}` : ''}`}
                      onClick={() => handleSave(cfg.key)}
                    >Salvar</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
    </SecondaryLayout>
  );
}
