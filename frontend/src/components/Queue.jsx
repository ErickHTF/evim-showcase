import { useContext } from 'react';
import Icon from './Icons';
import { StatusColorsContext } from '../context/StatusColorsContext';
import { SkeletonTicketRow } from './Skeleton';
import s from './css/Queue.module.css';

function PriorityIcon({ p }) {
  const bars = {
    p1: [1, 1, 1, 1],
    p2: [1, 1, 1, 0],
    p3: [1, 1, 0, 0],
    p4: [1, 0, 0, 0],
  }[p] ?? [1, 1, 0, 0];
  return (
    <span className={'priority ' + p} title={p?.toUpperCase()}>
      <svg viewBox="0 0 12 12" width="11" height="11">
        {bars.map((on, i) => (
          <rect key={i} x={i * 3} y={9 - i * 2} width="2" height={2 + i * 2}
            fill={on ? 'currentColor' : 'var(--border-strong)'} />
        ))}
      </svg>
      {p?.toUpperCase()}
    </span>
  );
}

function StatusPill({ status, label }) {
  const colors = useContext(StatusColorsContext);
  const color = (label && colors[label]) ? colors[label] : colors[status];
  const labels = { open: 'Aberto', progress: 'Em andamento', review: 'Em revisão', blocked: 'Bloqueado', done: 'Concluído' };
  const text = label ?? labels[status] ?? status;
  if (color) {
    return (
      <span className="status-pill" style={{ '--pill-bg': color + '1a', '--pill-color': color, '--pill-border': color + '33' }}>
        <span className={s.statusDotDynamic} style={{ '--pill-color': color }} />
        {text}
      </span>
    );
  }
  return (
    <span className={'status-pill ' + status}>
      <span className="sdot" />
      {text}
    </span>
  );
}

function TicketRow({ t, selected, onSelect, checked, onToggleCheck }) {
  const vendorColor = t.vendor?.color ?? '#888';
  const vendorName = t.vendor?.name ?? '—';
  return (
    <div
      className={`${s.ticketRow}${selected ? ` ${s.selected}` : ''}${t.unread ? ` ${s.unread}` : ''}`}
      onClick={onSelect}>
      <input type="checkbox" checked={!!checked} onClick={e => e.stopPropagation()} onChange={onToggleCheck} />
      <div className={s.ticketBody}>
        <div className={s.ticketLine1}>
          <span className={s.ticketVendor}>
            <span className={s.vendorDotDynamic} style={{ '--vendor-color': vendorColor }} />
            <span className={s.vendorLabel}>{vendorName}</span>
          </span>
          {t.jiraKey && (
            <span className={s.jiraKeyChip}>
              <Icon.JiraLogo className={s.jiraLogoIcon} />
              {t.jiraKey}
            </span>
          )}
          {t.sla && <span className={'ticket-sla-chip ' + t.sla.state + ' ' + s.slaChip}>{t.sla.label}</span>}
        </div>
        <div className={s.ticketTitle}>{t.title}</div>
        <div className={s.ticketMetaRow}>
          <StatusPill status={t.status} label={t.jiraStatus} />
          <PriorityIcon p={t.priority} />
          {t.threadCount > 0 && (
            <span className={s.threadCount}>
              <Icon.Mail className="ic-sm" /> {t.threadCount}
            </span>
          )}
        </div>
      </div>
      <div className={s.ticketAside}>
        <span className={s.ticketTime}>{t.updated}</span>
        {t.starred && <Icon.Star className={`ic-sm ${s.starredIndicator}`} />}
        {t.assignee && (
          <div className="avatar sm" style={{ '--avatar-color': t.assignee.color }}>
            {t.assignee.avatarUrl
              ? <img src={t.assignee.avatarUrl} alt={t.assignee.initials} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
              : t.assignee.initials}
          </div>
        )}
      </div>
    </div>
  );
}

function Queue({ tickets, isLoading, selectedId, setSelectedId, queueTab, setQueueTab, checked, setChecked, currentPage, hasPrev, hasNext, goNext, goPrev, onBulkStar, onBulkUnstar }) {
  const toggleCheck = (id) => {
    setChecked(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const showSkeleton = isLoading && tickets.length === 0;
  const showEmpty = !isLoading && tickets.length === 0;

  return (
    <div className={s.queue}>
      <div className={s.queueHeader}>
        <div className={s.queueTitle}>
          Caixa de Entrada
          <span className="meta">· {tickets.length}</span>
        </div>
        <div className={s.headerActions}>
          <button className="btn ghost icon" title="Ordenar"><Icon.Sort className="ic" /></button>
          <button className="btn ghost icon" title="Mais"><Icon.More className="ic" /></button>
        </div>
      </div>

      {checked.length > 0 && (
        <div className={s.bulkBar}>
          <span className={s.bulkCount}>{checked.length} selecionados</span>
          <button className="btn sm" onClick={() => { onBulkStar?.(checked); setChecked([]); }}><Icon.Star className="ic-sm" /> Favoritar</button>
          <button className="btn sm" onClick={() => { onBulkUnstar?.(checked); setChecked([]); }}><Icon.Star className="ic-sm" /> Desfavoritar</button>
          <button className={`btn ghost sm ${s.clearBtn}`} onClick={() => setChecked([])}>Desmarcar</button>
        </div>
      )}

      <div className={s.queueList}>
        {showSkeleton && (
          <>
            {Array.from({ length: 6 }).map((_, i) => <SkeletonTicketRow key={i} />)}
          </>
        )}
        {showEmpty && (
          <div className={s.queueEmpty}>
            <div className={s.queueEmptyIcon}><Icon.Inbox className="ic-lg" /></div>
            <div className={s.queueEmptyTitle}>Nenhum ticket por aqui</div>
            <div className={s.queueEmptyHint}>
              Tente ajustar os filtros à esquerda ou aguarde novas conversas chegarem.
            </div>
          </div>
        )}
        {!showSkeleton && !showEmpty && tickets.map(t => (
          <TicketRow key={t.id} t={t}
            selected={selectedId === t.id}
            onSelect={() => setSelectedId(t.id)}
            checked={checked.includes(t.id)}
            onToggleCheck={() => toggleCheck(t.id)} />
        ))}
      </div>

      <div className={s.pagination}>
          <button className={`btn ghost icon ${s.pageBtn}`} onClick={goPrev} disabled={!hasPrev}>
            ‹
          </button>
          <span className={s.pageNum}>{currentPage}</span>
          <button className={`btn ghost icon ${s.pageBtn}`} onClick={goNext} disabled={!hasNext}>
            ›
          </button>
        </div>
    </div>
  );
}

export { Queue, TicketRow, StatusPill, PriorityIcon };
export default Queue;
