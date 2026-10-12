import { useState, useMemo, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getMetrics, getSprints } from '../services/api';
import { loadMetricsSettings } from '../services/metricsSettings';
import SecondaryLayout from './SecondaryLayout';
import s from './css/MetricsPage.module.css';

function fmtDuration(seconds) {
  if (seconds == null || seconds === 0) return '—';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function ownerBadge(owner) {
  if (owner === 'VENDOR')   return <span className={`${s.badge} ${s.vendor}`}>Vendor</span>;
  if (owner === 'INTERNAL') return <span className={`${s.badge} ${s.internal}`}>Interno</span>;
  return <span className={`${s.badge} ${s.closed}`}>Encerrado</span>;
}

function thresholdStatus(seconds, t) {
  if (!seconds || !t) return null;
  if (seconds >= t.crit) return 'crit';
  if (seconds >= t.warn) return 'warn';
  return null;
}

function StatCard({ label, value, indicator, color, onClick, active, target, valueStatus }) {
  const valueClass = valueStatus === 'crit' ? s.statValueCrit
                   : valueStatus === 'warn' ? s.statValueWarn
                   : s.statValue;
  return (
    <div
      className={s.statCard + (onClick ? ` ${s.statCardClickable}` : '') + (active ? ` ${s.statCardActive}` : '')}
      style={{ '--accent': color }}
      onClick={onClick}
    >
      <span className={s.statLabel}>{label}</span>
      <div className={s.statBottom}>
        <span className={valueClass}>{value}</span>
        {indicator && <span className={s.statIndicator}>{indicator}</span>}
      </div>
      {target && <span className={s.statTarget}>{target}</span>}
    </div>
  );
}

function SortHeader({ label, field, sortField, sortDir, onSort }) {
  const active = sortField === field;
  return (
    <th className={s.sortable} onClick={() => onSort(field)}>
      {label}
      <span className={s.sortIcon}>{active ? (sortDir === 'desc' ? ' ↓' : ' ↑') : ' ↕'}</span>
    </th>
  );
}

function PollingTimer({ dataUpdatedAt, refetchMs }) {
  const [countdown, setCountdown] = useState(refetchMs / 1000);
  const progress = countdown / (refetchMs / 1000);

  useEffect(() => {
    if (!dataUpdatedAt) return;
    const tick = () => {
      const remaining = Math.max(0, Math.round((refetchMs - (Date.now() - dataUpdatedAt)) / 1000));
      setCountdown(remaining);
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [dataUpdatedAt, refetchMs]);

  return (
    <div className={s.timer}>
      <div className={s.timerBar}>
        <div className={s.timerFill} style={{ width: `${progress * 100}%` }} />
      </div>
      <span className={s.timerLabel}>Atualiza em {countdown}s</span>
    </div>
  );
}

export default function MetricsPage() {
  const navigate = useNavigate();
  const settings = loadMetricsSettings();
  const [ownerFilter,      setOwnerFilter]      = useState(null); // null | 'VENDOR' | 'INTERNAL'
  const [sortField,        setSortField]         = useState('vendorTimeSeconds');
  const [sortDir,          setSortDir]           = useState('desc');
  const [selectedSprintId, setSelectedSprintId] = useState(null);
  const [page,             setPage]              = useState(1);
  const [pageSize,         setPageSize]          = useState(25);

  const { data: sprints = [] } = useQuery({
    queryKey: ['sprints'],
    queryFn: ({ signal }) => getSprints(signal),
    staleTime: 300_000,
  });

  const { data, isLoading, isError, dataUpdatedAt } = useQuery({
    queryKey: ['metrics', selectedSprintId],
    queryFn: ({ signal }) => getMetrics(selectedSprintId, signal),
    refetchInterval: settings.refetchMs,
  });

  function handleSort(field) {
    if (sortField === field) setSortDir(d => d === 'desc' ? 'asc' : 'desc');
    else { setSortField(field); setSortDir('desc'); }
  }

  function handleCardFilter(owner) {
    setOwnerFilter(prev => prev === owner ? null : owner);
  }

  const sorted = useMemo(() => {
    if (!data?.tickets) return [];
    const filtered = ownerFilter
      ? data.tickets.filter(t => t.currentOwner === ownerFilter)
      : data.tickets;
    return [...filtered].sort((a, b) => {
      const av = a[sortField] ?? (sortField === 'jiraKey' || sortField === 'currentOwner' ? '' : -1);
      const bv = b[sortField] ?? (sortField === 'jiraKey' || sortField === 'currentOwner' ? '' : -1);
      if (av < bv) return sortDir === 'desc' ? 1 : -1;
      if (av > bv) return sortDir === 'desc' ? -1 : 1;
      return 0;
    });
  }, [data, sortField, sortDir, ownerFilter]);

  const totalRows = sorted.length;
  const totalPages = Math.max(1, Math.ceil(totalRows / pageSize));
  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);
  useEffect(() => { setPage(1); }, [ownerFilter, sortField, sortDir, selectedSprintId, pageSize]);

  const pageStart = (page - 1) * pageSize;
  const pageRows = sorted.slice(pageStart, pageStart + pageSize);

  if (isLoading) return <SecondaryLayout><div className={s.state}>Carregando métricas…</div></SecondaryLayout>;
  if (isError)   return <SecondaryLayout><div className={s.state}>Erro ao carregar métricas.</div></SecondaryLayout>;
  if (!data)     return <SecondaryLayout><div className={s.state}>Sem dados.</div></SecondaryLayout>;

  const { summary } = data;
  const c = settings.colors;

  return (
    <SecondaryLayout>
    <div className={s.page}>

      <div className={s.header}>
        <div>
          <h1 className={s.title}>Métricas de SLA</h1>
          <p className={s.subtitle}>Rastreamento de ownership por ticket</p>
        </div>
        <div className={s.headerRight}>
          {sprints.length > 0 && (
            <select
              className={s.sprintSelect}
              value={selectedSprintId ?? ''}
              onChange={e => setSelectedSprintId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Todas as sprints</option>
              {sprints.map(sp => (
                <option key={sp.id} value={sp.id}>
                  {sp.name}{sp.state === 'active' ? ' ●' : ''}
                </option>
              ))}
            </select>
          )}
          <PollingTimer dataUpdatedAt={dataUpdatedAt} refetchMs={settings.refetchMs} />
        </div>
      </div>

      <div className={s.summaryGrid}>
        <StatCard
          label="Tickets Ativos" value={summary.totalTracked} indicator="↑" color={c.total}
          onClick={() => setOwnerFilter(null)} active={ownerFilter === null} />
        <StatCard
          label="Com Vendor"  value={summary.waitingForVendor}   indicator="●" color={c.vendor}
          onClick={() => handleCardFilter('VENDOR')} active={ownerFilter === 'VENDOR'} />
        <StatCard
          label="Com Interno" value={summary.waitingForInternal} indicator="●" color={c.internal}
          onClick={() => handleCardFilter('INTERNAL')} active={ownerFilter === 'INTERNAL'} />
        <StatCard label="Méd. Vendor"    value={fmtDuration(summary.avgVendorTimeSeconds)}    color={c.avgVendor}
          valueStatus={thresholdStatus(summary.avgVendorTimeSeconds, settings.thresholds?.avgVendor)}
          target={settings.targets?.avgVendor || null} />
        <StatCard label="First Response" value={fmtDuration(summary.avgFirstResponseSeconds)} color={c.firstResponse}
          valueStatus={thresholdStatus(summary.avgFirstResponseSeconds, settings.thresholds?.firstResponse)}
          target={settings.targets?.firstResponse || null} />
      </div>

      <div className={s.tableWrap}>
        <table className={s.table}>
          <thead>
            <tr>
              <SortHeader label="Ticket"             field="jiraKey"              sortField={sortField} sortDir={sortDir} onSort={handleSort} />
              <SortHeader label="Aguardando"         field="currentOwner"         sortField={sortField} sortDir={sortDir} onSort={handleSort} />
              <SortHeader label="Em posse do vendor" field="vendorTimeSeconds"    sortField={sortField} sortDir={sortDir} onSort={handleSort} />
              <SortHeader label="First response"     field="firstResponseSeconds" sortField={sortField} sortDir={sortDir} onSort={handleSort} />
              <SortHeader label="Interações"         field="pingPongScore"        sortField={sortField} sortDir={sortDir} onSort={handleSort} />
              <SortHeader label="Desde"              field="trackingSince"        sortField={sortField} sortDir={sortDir} onSort={handleSort} />
            </tr>
          </thead>
          <tbody>
            {sorted.length === 0 && (
              <tr><td colSpan={6} className={s.empty}>Nenhum ticket rastreado ainda.</td></tr>
            )}
            {pageRows.map(t => (
              <tr key={t.conversationId ?? t.jiraKey}>
                <td
                  className={s.jiraKeyLink}
                  onClick={() => navigate('/', { state: { selectJiraKey: t.jiraKey, selectTicketId: t.ticketId } })}
                >
                  {t.jiraKey || (t.ticketId ? `#${t.ticketId}` : '—')}
                </td>
                <td>{ownerBadge(t.currentOwner)}</td>
                <td>{fmtDuration(t.vendorTimeSeconds)}</td>
                <td>{fmtDuration(t.firstResponseSeconds)}</td>
                <td className={s.center}>{t.pingPongScore}</td>
                <td className={s.muted}>
                  {t.trackingSince
                    ? new Date(t.trackingSince).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
                    : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {totalRows > 0 && (
        <div className={s.pagination}>
          <div className={s.pageInfo}>
            Mostrando <strong>{pageStart + 1}</strong>–<strong>{Math.min(pageStart + pageSize, totalRows)}</strong> de <strong>{totalRows}</strong>
          </div>
          <div className={s.pageControls}>
            <label className={s.pageSizeLabel}>
              Por página:
              <select
                className={s.pageSizeSelect}
                value={pageSize}
                onChange={e => setPageSize(Number(e.target.value))}
              >
                {[10, 25, 50, 100].map(n => <option key={n} value={n}>{n}</option>)}
              </select>
            </label>
            <button
              className={s.pageBtn}
              onClick={() => setPage(1)}
              disabled={page === 1}
              title="Primeira página"
            >«</button>
            <button
              className={s.pageBtn}
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
              title="Página anterior"
            >‹</button>
            <span className={s.pageNumber}>Página <strong>{page}</strong> de <strong>{totalPages}</strong></span>
            <button
              className={s.pageBtn}
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page >= totalPages}
              title="Próxima página"
            >›</button>
            <button
              className={s.pageBtn}
              onClick={() => setPage(totalPages)}
              disabled={page >= totalPages}
              title="Última página"
            >»</button>
          </div>
        </div>
      )}
    </div>
    </SecondaryLayout>
  );
}
