import { useState, useMemo, useEffect, useRef, lazy, Suspense } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Routes, Route, useLocation } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { StatusColorsContext } from './context/StatusColorsContext';
import { setAuthToken } from './services/api';
import { STATUS_PALETTE } from './constants/colors';
import { statusSlug, getNameInitials, textAdf } from './utils/string';

import 'driver.js/dist/driver.css';
import './styles/index.css';

import Icon from './components/Icons';
import { Rail, TopBar, Sidebar } from './components/Shell';
import Queue from './components/Queue';
import Detail, { DetailAside } from './components/Detail';
const AdminPanel = lazy(() => import('./components/AdminPanel'));
const MetricsPage = lazy(() => import('./components/MetricsPage'));
const Tour = lazy(() => import('./components/Tour'));
import { useConversations } from './hooks/useConversations';
import {
  getCard, getEmailsByTicket, getThreadMessagesByTicket, getCardTransitions, patchCard, addCardComment,
  getAssignableUsers, getStatuses, getPublicConfig, getAtlassianStatus,
  getSprints, onAtlassianReauth, sendEmail, getStarredKeys, starTicket, unstarTicket,
  getFilterCounts,
} from './services/api';
import LinkAtlassianPage from './components/LinkAtlassianPage';
const DeployPage = lazy(() => import('./components/DeployPage'));
import { ToastProvider, useToast } from './hooks/useToast.jsx';

const TWEAK_DEFAULTS = {
  accent: 'green',
  density: 'comfortable',
  showAiPanel: false,
  showSlaBars: false,
};

const TWEAKS_STORAGE_KEY = 'mm:tweaks';

function loadTweaks() {
  try {
    const raw = localStorage.getItem(TWEAKS_STORAGE_KEY);
    if (!raw) return TWEAK_DEFAULTS;
    return { ...TWEAK_DEFAULTS, ...JSON.parse(raw) };
  } catch { return TWEAK_DEFAULTS; }
}

function normalizeStatus(s) {
  if (!s) return 'open';
  const l = s.toLowerCase();
  if (l.includes('progress') || l.includes('doing')) return 'progress';
  if (l.includes('review') || l.includes('testing')) return 'review';
  if (l.includes('block')) return 'blocked';
  if (l.includes('done') || l.includes('close') || l.includes('resolv')) return 'done';
  return 'open';
}

const VENDOR_COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4', '#f97316', '#14b8a6'];
function vendorColor(name) {
  if (!name) return '#888';
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = ((hash << 5) - hash + name.charCodeAt(i)) | 0;
  return VENDOR_COLORS[Math.abs(hash) % VENDOR_COLORS.length];
}

function dedupeActivities(activities) {
  const seen = new Set();
  return activities.filter(a => {
    const text = (a.body ?? '').replace(/<[^>]+>/g, '').trim().slice(0, 100);
    const key = `${a.from ?? a.who ?? ''}|${text}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function buildDetailTicket(issue, emails, threadMessages, conv) {
  const emailActivities = threadMessages.length > 0
    ? [...threadMessages]
        .sort((a, b) => (b.ordinal ?? 0) - (a.ordinal ?? 0))
        .map(tm => ({
        kind: tm.isVendor === false ? 'internal-email' : 'vendor-email',
        id: `tm-${tm.id}`,
        from: tm.author ?? emails?.[0]?.sender ?? 'Portal',
        time: tm.sentAt ? new Date(tm.sentAt).toLocaleString('pt-BR') : '',
        sortDate: tm.sentAt ? new Date(tm.sentAt) : new Date(0),
        body: tm.body ?? '',
        company: tm.company ?? null,
        vendor: { color: vendorColor(tm.company ?? tm.author), name: tm.company ?? 'Vendor' },
      }))
    : (emails || []).map(e => ({
        kind: e.isVendor ? 'vendor-email' : 'internal-email',
        id: e.id,
        from: e.senderName ?? e.sender,
        time: e.received ? new Date(e.received).toLocaleString('pt-BR') : '',
        sortDate: e.received ? new Date(e.received) : new Date(0),
        body: e.body ?? '',
        to: e.receiver,
        vendor: { color: vendorColor(e.senderName ?? e.sender), name: e.senderName ?? e.sender?.split('@')[1] ?? 'Vendor' },
      }));
  const commentActivities = (issue.comments || []).map((c, i) => ({
    kind: 'comment',
    id: `comment-${i}`,
    who: c.author,
    whoAvatarUrl: c.authorAvatarUrl || null,
    time: '',
    body: `<p>${c.body}</p>`,
    attachmentIds: c.attachmentIds,
  }));
  const activityCount = threadMessages.length > 0 ? threadMessages.length : (emails?.length ?? 0);
  return {
    id: conv.id,
    jiraKey: issue.key,
    title: issue.summary,
    jiraStatus: issue.status,
    status: normalizeStatus(issue.status),
    priority: priorityMap[issue.priorityId] ?? 'p3',
    from: emails?.[0]?.sender ?? conv.id,
    assignee: (issue.assignee && issue.assignee !== 'Unassigned') ? {
      name: issue.assignee,
      initials: getNameInitials(issue.assignee),
      color: '#4f8f24',
      avatarUrl: issue.assigneeAvatarUrl || null,
    } : null,
    reporter: issue.reporterName || null,
    reporterAvatarUrl: issue.reporterAvatarUrl || null,
    description: issue.description ?? null,
    descriptionAttachmentIds: issue.descriptionAttachmentIds ?? [],
    attachments: issue.attachments ?? [],
    vendor: { color: '#888', name: 'Vendor', type: 'External' },
    threadCount: activityCount,
    activity: dedupeActivities([...emailActivities, ...commentActivities].sort((a, b) => {
      if (a.sortDate && b.sortDate) return b.sortDate - a.sortDate;
      return 0;
    })),
    emailContacts: [...new Set(
      (emails || []).map(e => e.sender).filter(Boolean)
        .concat((emails || []).map(e => e.receiver).filter(Boolean))
        .concat(threadMessages.map(tm => tm.author).filter(Boolean))
    )],
    sla: null,
  };
}

const priorityMap = { '1': 'p1', '2': 'p2', '3': 'p3', '4': 'p4', '5': 'p4' };

function convToTicket(conv) {
  return {
    id: conv.id,
    jiraKey: conv.jiraId,
    title: conv.subject,
    status: normalizeStatus(conv.status),
    jiraStatus: conv.status,
    priority: priorityMap[conv.priorityId] ?? 'p3',
    from: conv.id,
    vendor: { color: '#888', name: 'Vendor' },
    assignee: conv.assignee ? {
      name: conv.assignee,
      initials: conv.assignee.slice(0, 2).toUpperCase(),
      color: '#4f8f24',
      avatarUrl: conv.assigneeAvatarUrl || null,
    } : null,
    pendingInternal: conv.isVendor ?? false,
    unread: false,
    starred: false,
    sla: null,
    threadCount: 0,
    updated: conv.lastInteraction
      ? new Date(conv.lastInteraction).toLocaleDateString('pt-BR')
      : '',
  };
}

function Tweaks({ open, tweaks, setTweaks }) {
  if (!open) return null;
  const setKey = (k, v) => setTweaks(prev => ({ ...prev, [k]: v }));
  return (
    <div className="tweaks">
      <h3><Icon.Settings className="ic-sm" /> Ajustes</h3>
      <div className="tweak-row">
        <label>Painel de sugestões IA</label>
        <button className={'tweak-toggle' + (tweaks.showAiPanel ? ' on' : '')}
          onClick={() => setKey('showAiPanel', !tweaks.showAiPanel)} />
      </div>
      <div className="tweak-row">
        <label>Barra de progresso SLA</label>
        <button className={'tweak-toggle' + (tweaks.showSlaBars ? ' on' : '')}
          onClick={() => setKey('showSlaBars', !tweaks.showSlaBars)} />
      </div>
      <div className="tweak-row">
        <label>Densidade</label>
        <div className="tweak-seg">
          {['comfortable', 'dense'].map(d => (
            <button key={d} className={tweaks.density === d ? 'on' : ''}
              onClick={() => setKey('density', d)}>
              {d === 'comfortable' ? 'Confortável' : 'Compacto'}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

const ASIDE_W_KEY = 'evim:asideWidth';
const ASIDE_W_MIN = 240;
const ASIDE_W_MAX = 560;
const ASIDE_W_DEFAULT = 320;

const QUEUE_W_KEY = 'evim:queueWidth';
const QUEUE_W_MIN = 260;
const QUEUE_W_MAX = 640;
const QUEUE_W_DEFAULT = 320;

const RESIZER_CONFIG = {
  aside: {
    cssVar: '--aside-w',
    storageKey: ASIDE_W_KEY,
    min: ASIDE_W_MIN,
    max: ASIDE_W_MAX,
    def: ASIDE_W_DEFAULT,
    edge: 'right', // distance from right edge of viewport
    resizingClass: 'is-resizing-aside',
    handleClass: 'aside-resizer',
  },
  queue: {
    cssVar: '--queue-w',
    storageKey: QUEUE_W_KEY,
    min: QUEUE_W_MIN,
    max: QUEUE_W_MAX,
    def: QUEUE_W_DEFAULT,
    edge: 'left', // distance from left edge of the column
    resizingClass: 'is-resizing-queue',
    handleClass: 'queue-resizer',
  },
};

function applyVarWidth(cssVar, px, min, max) {
  const clamped = Math.max(min, Math.min(max, px));
  document.documentElement.style.setProperty(cssVar, `${clamped}px`);
  return clamped;
}

(() => {
  if (typeof window === 'undefined') return;
  for (const cfg of Object.values(RESIZER_CONFIG)) {
    const stored = parseInt(window.localStorage.getItem(cfg.storageKey) ?? '', 10);
    applyVarWidth(cfg.cssVar, Number.isFinite(stored) ? stored : cfg.def, cfg.min, cfg.max);
  }
})();

function ColResizer({ kind }) {
  const cfg = RESIZER_CONFIG[kind];
  const draggingRef = useRef(false);
  const handleRef = useRef(null);

  const onMouseDown = (e) => {
    e.preventDefault();
    draggingRef.current = true;
    handleRef.current?.classList.add('active');
    const mainEl = handleRef.current?.closest('.main');
    mainEl?.classList.add(cfg.resizingClass);

    const colEl = handleRef.current?.parentElement;
    const startRect = colEl?.getBoundingClientRect();
    const startLeft = startRect?.left ?? 0;

    const onMove = (ev) => {
      if (!draggingRef.current) return;
      const newW = cfg.edge === 'right'
        ? window.innerWidth - ev.clientX
        : ev.clientX - startLeft;
      applyVarWidth(cfg.cssVar, newW, cfg.min, cfg.max);
    };
    const onUp = () => {
      if (!draggingRef.current) return;
      draggingRef.current = false;
      handleRef.current?.classList.remove('active');
      mainEl?.classList.remove(cfg.resizingClass);
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
      const cur = parseInt(getComputedStyle(document.documentElement).getPropertyValue(cfg.cssVar), 10);
      if (Number.isFinite(cur)) {
        try { window.localStorage.setItem(cfg.storageKey, String(cur)); } catch {}
      }
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  };

  const onDoubleClick = () => {
    applyVarWidth(cfg.cssVar, cfg.def, cfg.min, cfg.max);
    try { window.localStorage.setItem(cfg.storageKey, String(cfg.def)); } catch {}
  };

  return (
    <div
      ref={handleRef}
      className={cfg.handleClass}
      data-tour={kind === 'queue' ? 'queue-resizer' : undefined}
      onMouseDown={onMouseDown}
      onDoubleClick={onDoubleClick}
      role="separator"
      aria-orientation="vertical"
      title="Arraste para redimensionar (duplo clique para resetar)"
    />
  );
}

function MainApp({ jiraUser }) {
  const auth     = useAuth();
  const location = useLocation();
  const { showToast } = useToast();
  const [tweaks, setTweaks] = useState(loadTweaks);

  useEffect(() => {
    try { localStorage.setItem(TWEAKS_STORAGE_KEY, JSON.stringify(tweaks)); } catch {}
  }, [tweaks]);

  const setDensity = (d) => setTweaks(prev => ({ ...prev, density: d }));
  const [pendingTab, setPendingTab] = useState(null);
  const [tweaksOpen, setTweaksOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(() => {
    if (typeof window === 'undefined') return true;
    return window.matchMedia?.('(min-width: 1301px)').matches ?? true;
  });
  const [activeSection, setActiveSection] = useState('inbox');
  const [quickFilter, setQuickFilter] = useState('all');
  const [noVendorFilter, setNoVendorFilter] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedStatuses, setSelectedStatuses] = useState([]);
  const [selectedSprintId, setSelectedSprintId] = useState(null);
  const [queueTab, setQueueTab] = useState('all');
  const [checked, setChecked] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [pageSize, setPageSize] = useState(20);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchQuery), 400);
    return () => clearTimeout(timer);
  }, [searchQuery]);
  const [mobilePanel, setMobilePanel] = useState('queue');
  const [selectedId, setSelectedId] = useState(() => {
    try { return localStorage.getItem('mm:selected') || null; } catch { return null; }
  });

  const queryClient = useQueryClient();

  const myAccountId = jiraUser?.accountId ?? null;

  const filters = useMemo(() => {
    const f = { size: pageSize };
    if (selectedUser) f.assignee = selectedUser;
    else if (quickFilter === 'mine' && myAccountId) f.assignee = myAccountId;
    else if (quickFilter === 'unassigned') f.assignee = 'unassigned';
    else f.assignee = 'all';
    if (selectedSprintId != null) f.sprintId = selectedSprintId;
    if (debouncedSearch.trim()) f.text = debouncedSearch.trim();
    if (noVendorFilter) f.vendorOnly = false;
    if (selectedStatuses.length) f.statuses = selectedStatuses;
    return f;
  }, [quickFilter, selectedUser, myAccountId, selectedSprintId, pageSize, debouncedSearch, selectedStatuses, noVendorFilter]);

  const { data: users = [] }        = useQuery({ queryKey: ['users'],    queryFn: ({ signal }) => getAssignableUsers(signal) });
  const { data: jiraStatuses = [] } = useQuery({ queryKey: ['statuses'], queryFn: ({ signal }) => getStatuses(signal) });
  const { data: config }            = useQuery({ queryKey: ['config'],   queryFn: ({ signal }) => getPublicConfig(signal) });
  const { data: sprints = [] }      = useQuery({ queryKey: ['sprints'],  queryFn: ({ signal }) => getSprints(signal), staleTime: 300_000 });
  const { data: starredKeys = [] }  = useQuery({ queryKey: ['starred-keys'], queryFn: ({ signal }) => getStarredKeys(signal) });

  const { data: filterCounts } = useQuery({
    queryKey: ['filter-counts', myAccountId, selectedSprintId],
    queryFn:  ({ signal }) => getFilterCounts({ accountId: myAccountId, sprintId: selectedSprintId }, signal),
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  });

  const starMutation = useMutation({
    mutationFn: ({ key, starred }) => starred ? unstarTicket(key) : starTicket(key),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['starred-keys'] }),
  });

  useEffect(() => {
    if (!sprints.length || selectedSprintId !== null) return;
    const active = sprints.find(s => s.state === 'active');
    if (active) setSelectedSprintId(active.id);
  }, [sprints, selectedSprintId]);
  const jiraBaseUrl = (config?.jiraBaseUrl ?? '').replace(/\/$/, '');

  const { data: cardData }  = useQuery({
    queryKey: ['card', selectedConv?.jiraId],
    queryFn:  ({ signal }) => getCard(selectedConv.jiraId, signal),
    enabled:  !!selectedConv?.jiraId,
  });
  const { data: emailsData } = useQuery({
    queryKey: ['emails', selectedConv?.ticketId],
    queryFn:  ({ signal }) => getEmailsByTicket(selectedConv.ticketId, signal),
    enabled:  !!selectedConv?.ticketId,
  });
  const { data: threadMessagesData } = useQuery({
    queryKey: ['threadMessages', selectedConv?.ticketId],
    queryFn:  ({ signal }) => getThreadMessagesByTicket(selectedConv.ticketId, signal),
    enabled:  !!selectedConv?.ticketId,
  });
  const { data: transitionsData = [] } = useQuery({
    queryKey: ['transitions', selectedConv?.jiraId],
    queryFn:  ({ signal }) => getCardTransitions(selectedConv.jiraId, signal),
    enabled:  !!selectedConv?.jiraId,
  });

  const detailTicket = useMemo(() => {
    if (!cardData || !selectedConv) return null;
    return buildDetailTicket(cardData, emailsData ?? [], threadMessagesData ?? [], selectedConv);
  }, [cardData, emailsData, threadMessagesData, selectedConv]);

  const { conversations, currentPage, hasPrev, hasNext, isLoading: convLoading, goNext, goPrev } = useConversations(filters);

  useEffect(() => {
    const jiraKey = location.state?.selectJiraKey;
    if (!jiraKey || !conversations.length) return;
    const conv = conversations.find(c => c.jiraId === jiraKey);
    if (conv) {
      handleSelect(conv);
      setPendingTab('jira');
      window.history.replaceState({}, document.title);
    }
  }, [conversations.length, location.state?.selectJiraKey]);

  function handleSelect(conv) {
    if (!conv) return;
    setSelectedId(conv.id);
    setSelectedConv(conv);
    setMobilePanel('detail');
    try { localStorage.setItem('mm:selected', conv.id ?? ''); } catch {}
  }

  const statusMutation = useMutation({
    mutationFn: ({ key, patch }) => patchCard(key, patch),
    onMutate: async ({ key, patch, optimisticStatus }) => {
      const queryKey = ['card', selectedConv?.jiraId];
      await queryClient.cancelQueries({ queryKey });
      const previous = queryClient.getQueryData(queryKey);
      if (previous && optimisticStatus) {
        queryClient.setQueryData(queryKey, { ...previous, status: optimisticStatus });
      }
      return { previous, queryKey };
    },
    onError: (err, _vars, context) => {
      if (context?.previous) queryClient.setQueryData(context.queryKey, context.previous);
      console.error('Status change failed:', err);
      showToast('Falha ao mudar status', { type: 'error' });
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['card', selectedConv?.jiraId] }),
  });

  function onStatusChange(rawStatus) {
    if (!detailTicket) return;
    const t = transitionsData.find(tr => tr.toStatus === rawStatus || tr.name === rawStatus);
    if (t) statusMutation.mutate({ key: detailTicket.jiraKey, patch: { transitionId: t.id }, optimisticStatus: rawStatus });
    queryClient.invalidateQueries({ queryKey: ['filter-counts'] });
    showToast(`Status → ${rawStatus}`, { type: 'success' });
  }

  const commentMutation = useMutation({
    mutationFn: ({ key, adf }) => addCardComment(key, adf),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['card', selectedConv?.jiraId] });
      showToast(`Comentário adicionado em ${detailTicket?.jiraKey}`, { type: 'success' });
    },
    onError: (err) => {
      console.error('Comment failed:', err);
      showToast('Erro ao postar comentário', { type: 'error' });
    },
  });

  const emailMutation = useMutation({
    mutationFn: (dto) => sendEmail(dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['emails', selectedConv?.ticketId] });
      showToast('E-mail enviado com sucesso', { type: 'success' });
    },
    onError: (err) => {
      console.error('Email send failed:', err);
      showToast('Erro ao enviar e-mail', { type: 'error' });
    },
  });

  function onSend(tab, text, opts = {}) {
    if (!detailTicket) return;
    if (tab === 'jira') {
      commentMutation.mutate({ key: detailTicket.jiraKey, adf: textAdf(text) });
    } else {
      emailMutation.mutate({
        ticketId: detailTicket.jiraKey,
        to: 'vendor@example.com',
        subject: 'Re: ' + (detailTicket.title ?? ''),
        body: text,
        replicateToJira: opts.replicateToJira ?? true,
        files: opts.files ?? [],
      });
    }
  }

  const statusColors = useMemo(() => {
    const map = {};
    jiraStatuses.forEach((s, i) => {
      const configured = config?.['status.color.' + statusSlug(s)];
      map[s] = configured || STATUS_PALETTE[i % STATUS_PALETTE.length];
    });
    return map;
  }, [jiraStatuses, config]);

  const starredSet = useMemo(() => new Set(starredKeys), [starredKeys]);

  const allTickets = conversations.map(c => {
    const t = convToTicket(c);
    t.starred = starredSet.has(t.jiraKey);
    return t;
  });
  const tickets = (() => {
    let result = allTickets;
    if (quickFilter === 'starred') result = result.filter(t => t.starred);
    else if (quickFilter === 'urgent') result = result.filter(t => t.pendingInternal);
    result = [...result.filter(t => t.starred), ...result.filter(t => !t.starred)];
    return result;
  })();

  return (
    <StatusColorsContext.Provider value={statusColors}>
    <>
      <div className={'app' + (tweaks.density === 'dense' ? ' dense' : '') + (!sidebarOpen ? ' sidebar-collapsed' : '')}>
        <Rail
          activeSection={activeSection}
          setActiveSection={setActiveSection}
          unreadCount={conversations.length}
          onLogout={() => auth.signoutRedirect()}
          />
        <TopBar searchQuery={searchQuery} onSearchChange={setSearchQuery} userProfile={auth.user?.profile} jiraUser={jiraUser} onLogout={() => auth.signoutRedirect()} density={tweaks.density} setDensity={setDensity} />
        <Sidebar
          sidebarOpen={sidebarOpen}
          setSidebarOpen={setSidebarOpen}
          quickFilter={quickFilter}
          setQuickFilter={f => { setQuickFilter(f); setSelectedConv(null); }}
          noVendorFilter={noVendorFilter}
          setNoVendorFilter={v => { setNoVendorFilter(v); setSelectedConv(null); }}
          selectedUser={selectedUser}
          setSelectedUser={u => { setSelectedUser(u); setSelectedConv(null); }}
          selectedStatuses={selectedStatuses}
          setSelectedStatuses={ss => { setSelectedStatuses(ss); setSelectedConv(null); }}
          users={users}
          jiraStatuses={jiraStatuses}
          statusColors={statusColors}
          sprints={sprints}
          selectedSprintId={selectedSprintId}
          setSelectedSprintId={id => { setSelectedSprintId(id); setSelectedConv(null); }}
          pageSize={pageSize}
          setPageSize={setPageSize}
          counts={{
            all: filterCounts?.all,
            mine: filterCounts?.mine,
            unassigned: filterCounts?.unassigned,
            noVendor: filterCounts?.noVendor,
            starred: starredKeys.length,
          }} />
        <div className={`main${mobilePanel === 'detail' ? ' main--show-detail' : ''}${!selectedConv ? ' main--no-detail' : ''}`}>
          <div className="col-queue" data-tour="queue">
            <Queue
              tickets={tickets}
              isLoading={convLoading}
              selectedId={selectedId}
              setSelectedId={id => {
                const conv = conversations.find(c => c.id === id);
                if (conv) handleSelect(conv);
              }}
              queueTab={queueTab}
              setQueueTab={setQueueTab}
              checked={checked}
              setChecked={setChecked}
              currentPage={currentPage}
              hasPrev={hasPrev}
              hasNext={hasNext}
              goNext={goNext}
              goPrev={goPrev}
              onBulkStar={(ids) => {
                const keys = ids.map(id => conversations.find(c => c.id === id)?.jiraId).filter(Boolean);
                keys.forEach(key => { if (!starredSet.has(key)) starMutation.mutate({ key, starred: false }); });
              }}
              onBulkUnstar={(ids) => {
                const keys = ids.map(id => conversations.find(c => c.id === id)?.jiraId).filter(Boolean);
                keys.forEach(key => { if (starredSet.has(key)) starMutation.mutate({ key, starred: true }); });
              }} />
            <ColResizer kind="queue" />
          </div>
          <div className="col-detail" data-tour="detail">
            {detailTicket
              ? <div key={detailTicket.jiraKey} className="detailFadeIn"><Detail ticket={detailTicket} onStatusChange={onStatusChange} onSend={onSend} sending={emailMutation.isPending || commentMutation.isPending} sendSuccess={emailMutation.isSuccess} jiraBaseUrl={jiraBaseUrl} jiraStatuses={jiraStatuses} defaultTab={pendingTab} onTabSet={() => setPendingTab(null)} onBack={() => setMobilePanel('queue')} starred={starredSet.has(detailTicket.jiraKey)} onToggleStar={(key) => starMutation.mutate({ key, starred: starredSet.has(key) })} /></div>
              : selectedConv
                ? <div className="emptyStateContainer detailLoading">
                    <div className="detailSpinner" aria-label="Carregando ticket" />
                    <div className="empty-hint">Carregando ticket…</div>
                  </div>
                : <div className="emptyStateContainer">
                    <div className="empty-illus"><Icon.Mail className="ic-lg" /></div>
                    <div className="empty-title">Nenhum ticket selecionado</div>
                    <div className="empty-hint">
                      Escolha uma conversa na fila à esquerda para ver detalhes.                  
                    </div>
                  </div>
            }
          </div>
          {selectedConv && (
            <div className="col-aside">
              <ColResizer kind="aside" />
              {detailTicket && <DetailAside ticket={detailTicket} showAi={tweaks.showAiPanel} />}
            </div>
          )}
        </div>
      </div>
      <Tweaks open={tweaksOpen} tweaks={tweaks} setTweaks={setTweaks} />
      <Suspense fallback={null}><Tour /></Suspense>
    </>
    </StatusColorsContext.Provider>
  );
}

function LoginScreen() {
  const auth = useAuth();
  return (
    <div className="loginScreenContainer">
      <h2 className="loginScreenTitle">EVIM</h2>
      <button
        className="btn-primary loginScreenButton"
        onClick={() => auth.signinRedirect()}>
        Entrar com Keycloak
      </button>
      {auth.error && (
        <p className="loginScreenError">
          {auth.error.message}
        </p>
      )}
    </div>
  );
}

function App() {
  const auth = useAuth();
  const queryClient = useQueryClient();

  useEffect(() => {
    setAuthToken(auth.user?.access_token ?? null);
  }, [auth.user]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('atlassian') === 'linked') {
      window.history.replaceState({}, document.title, '/');
      queryClient.invalidateQueries({ queryKey: ['atlassian-status'] });
    }
  }, [queryClient]);

  useEffect(() => {
    onAtlassianReauth(() => {
      queryClient.invalidateQueries({ queryKey: ['atlassian-status'] });
    });
  }, [queryClient]);

  const { data: atlassianStatus, isLoading: atlassianLoading } = useQuery({
    queryKey: ['atlassian-status'],
    queryFn: ({ signal }) => getAtlassianStatus(signal),
    enabled: auth.isAuthenticated,
  });

  if (auth.isLoading || (auth.isAuthenticated && atlassianLoading)) {
    return (
      <div className="loadingScreenContainer">
        <span className="loadingScreenText">Carregando...</span>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return <LoginScreen />;
  }

  if (!atlassianStatus?.linked) {
    return <LinkAtlassianPage />;
  }

  return (
    <ToastProvider>
      <Suspense fallback={<div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--fg-4)' }}>Carregando…</div>}>
      <Routes>
        <Route path="/" element={<MainApp jiraUser={atlassianStatus} />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/metrics" element={<MetricsPage />} />
        <Route path="/deploy" element={<DeployPage />} />
      </Routes>
      </Suspense>
    </ToastProvider>
  );
}

export default App;
