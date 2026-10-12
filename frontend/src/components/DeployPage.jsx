import { useState, useEffect, useRef, useMemo } from 'react';
import SecondaryLayout from './SecondaryLayout';
import Icon from './Icons';
import s from './css/DeployPage.module.css';

const EXT_COLORS = {
  js:'#f7df1e', jsx:'#61dafb', ts:'#3178c6', tsx:'#61dafb',
  css:'#264de4', scss:'#c6538c', html:'#e34c26', vue:'#42b883',
  json:'#5a9c4f', md:'#083fa1', yml:'#cc1018', yaml:'#cc1018',
  sh:'#4eaa25', bash:'#4eaa25', py:'#3776ab', java:'#b07219',
  xml:'#f60', sql:'#336791', rb:'#cc342d', go:'#00add8',
  png:'#a07040', jpg:'#a07040', jpeg:'#a07040', gif:'#a07040', svg:'#ffb13b',
  pdf:'#e53935', zip:'#888', gz:'#888', jar:'#b07219',
};

function buildTree(items) {
  const root = { children: {} };
  for (const { path, type } of items) {
    const parts = path.split('/');
    let node = root;
    for (let i = 0; i < parts.length; i++) {
      const name = parts[i];
      const nodePath = parts.slice(0, i + 1).join('/');
      if (!node.children[name]) {
        node.children[name] = {
          name, path: nodePath,
          type: i === parts.length - 1 ? type : 'tree',
          children: {},
        };
      }
      node = node.children[name];
    }
  }
  return root;
}

function sortedChildren(node) {
  return Object.values(node.children).sort((a, b) => {
    if (a.type !== b.type) return a.type === 'tree' ? -1 : 1;
    return a.name.localeCompare(b.name);
  });
}

function FileIcon({ name }) {
  const ext = name.includes('.') ? name.split('.').pop().toLowerCase() : '';
  const color = EXT_COLORS[ext] || 'var(--fg-4)';
  return (
    <svg viewBox="0 0 10 13" width="10" height="12" fill="none" style={{ flexShrink: 0 }}>
      <path d="M1 1h5.5L9 3.5V12a1 1 0 01-1 1H2a1 1 0 01-1-1V2a1 1 0 011-1z"
            stroke={color} strokeWidth="1.1"/>
      <path d="M6 1v3h3" stroke={color} strokeWidth="1.1"/>
    </svg>
  );
}

function FolderIcon({ open }) {
  const color = open ? 'var(--yellow-400, #fbbf24)' : 'var(--fg-4)';
  return (
    <svg viewBox="0 0 15 12" width="14" height="12" style={{ flexShrink: 0 }} fill={color}>
      <path d="M0 2a1 1 0 011-1h4l1.5 1.5H14a1 1 0 011 1v7a1 1 0 01-1 1H1a1 1 0 01-1-1V2z"/>
    </svg>
  );
}

function ChevIcon({ open }) {
  return (
    <svg viewBox="0 0 6 8" width="6" height="8" fill="currentColor"
         style={{ flexShrink: 0, color: 'var(--fg-4)',
                  transform: open ? 'rotate(90deg)' : 'none', transition: 'transform 140ms' }}>
      <path d="M1 1l4 3-4 3V1z"/>
    </svg>
  );
}

function FileLabel({ name }) {
  const dot = name.lastIndexOf('.');
  if (dot <= 0) return <span className={s.treeName}>{name}</span>;
  return (
    <span className={s.treeName}>
      {name.slice(0, dot)}<span className={s.treeExt}>{name.slice(dot)}</span>
    </span>
  );
}

function TreeNode({ node, depth, onSelect }) {
  const [open, setOpen] = useState(false);
  const isFolder = node.type === 'tree';
  const indent = depth * 14 + 8;

  if (!isFolder) {
    return (
      <div className={s.treeItem} style={{ paddingLeft: indent + 18 }}>
        <FileIcon name={node.name} />
        <FileLabel name={node.name} />
      </div>
    );
  }

  return (
    <>
      <div
        className={`${s.treeItem} ${s.treeClickable}`}
        style={{ paddingLeft: indent }}
        onClick={() => { setOpen(o => !o); onSelect(node.path); }}
        title={node.path}>
        <ChevIcon open={open} />
        <FolderIcon open={open} />
        <span className={s.treeName}>{node.name}</span>
      </div>
      {open && sortedChildren(node).map(child => (
        <TreeNode key={child.path} node={child} depth={depth + 1} onSelect={onSelect} />
      ))}
    </>
  );
}

const GH_ICON = (
  <svg viewBox="0 0 16 16" fill="currentColor" style={{ width: 14, height: 14 }}>
    <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
  </svg>
);

const STATUS_LABELS = {
  PENDING: 'Pendente',
  QUEUED: 'Na fila',
  IN_PROGRESS: 'Executando',
  SUCCESS: 'Sucesso',
  FAILURE: 'Falha',
  CANCELLED: 'Cancelado',
  UNKNOWN: 'Desconhecido',
};

function StatusBadge({ status }) {
  return (
    <span className={`${s.badge} ${s['badge_' + status] || ''}`}>
      {STATUS_LABELS[status] || status}
    </span>
  );
}

export default function DeployPage() {
  const [authState, setAuthState] = useState('loading');
  const [githubLogin, setGithubLogin] = useState(null);

  const [repos, setRepos] = useState([]);
  const [reposLoading, setReposLoading] = useState(false);
  const [selectedRepo, setSelectedRepo] = useState('');

  const [branches, setBranches] = useState([]);
  const [branchesLoading, setBranchesLoading] = useState(false);
  const [baseBranch, setBaseBranch] = useState('');
  const [targetBranch, setTargetBranch] = useState('');
  const [newBranchName, setNewBranchName] = useState('');
  const [autoMerge, setAutoMerge] = useState(false);

  const [folders, setFolders] = useState([]);
  const [foldersLoading, setFoldersLoading] = useState(false);
  const [folderLoaded, setFolderLoaded] = useState(false);
  const [rootOpen, setRootOpen] = useState(false);
  const [targetFolder, setTargetFolder] = useState('');
  const [useCustomFolder, setUseCustomFolder] = useState(false);

  const [files, setFiles] = useState([]);
  const [dragOver, setDragOver] = useState(false);

  const [phase, setPhase] = useState('idle');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [logLines, setLogLines] = useState([]);
  const [prUrl, setPrUrl] = useState(null);
  const [branchUrl, setBranchUrl] = useState(null);
  const [runUrl, setRunUrl] = useState(null);

  const [confirmDialog, setConfirmDialog] = useState(null);

  const [history, setHistory] = useState([]);
  const [historySearch, setHistorySearch] = useState('');
  const [historyStatusFilter, setHistoryStatusFilter] = useState('');

  const treeData = useMemo(() => folders.length > 0 ? buildTree(folders) : null, [folders]);

  const filteredHistory = useMemo(() => {
    const q = historySearch.toLowerCase();
    return history.filter(r => {
      const matchesSearch = !q ||
        [r.repo, r.fileName, r.newBranchName, r.triggeredBy]
          .some(v => v?.toLowerCase().includes(q));
      const matchesStatus = !historyStatusFilter || r.workflowStatus === historyStatusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [history, historySearch, historyStatusFilter]);

  const stepLinesRef = useRef({});
  const logScrollRef = useRef(null);
  const esRef = useRef(null);
  const lineIdRef = useRef(0);

  function nextId() { return ++lineIdRef.current; }

  useEffect(() => {
    fetch('/deploy-api/auth/me', { credentials: 'include' })
      .then(r => { if (r.ok) return r.json(); throw new Error(); })
      .then(data => { setGithubLogin(data.login); setAuthState('authenticated'); })
      .catch(() => setAuthState('unauthenticated'));
  }, []);

  function handleGithubLogout() {
    fetch('/deploy-api/logout', { credentials: 'include', redirect: 'manual' })
      .finally(() => {
        setGithubLogin(null);
        setAuthState('unauthenticated');
      });
  }

  useEffect(() => {
    if (authState !== 'authenticated') return;
    setReposLoading(true);
    fetch('/deploy-api/repos', { credentials: 'include' })
      .then(r => r.json())
      .then(data => { setRepos(data); setReposLoading(false); })
      .catch(() => setReposLoading(false));
    loadHistory();
  }, [authState]);

  useEffect(() => {
    if (!selectedRepo) { setBranches([]); return; }
    setBranchesLoading(true);
    setBaseBranch('');
    setTargetBranch('');
    setFolders([]); setFolderLoaded(false); setRootOpen(false); setTargetFolder(''); setUseCustomFolder(false);
    fetch(`/deploy-api/branches?repo=${encodeURIComponent(selectedRepo)}`, { credentials: 'include' })
      .then(r => r.json())
      .then(data => { setBranches(data); setBranchesLoading(false); })
      .catch(() => setBranchesLoading(false));
  }, [selectedRepo]);

  useEffect(() => {
    setFolders([]); setFolderLoaded(false); setRootOpen(false); setTargetFolder(''); setUseCustomFolder(false);
  }, [baseBranch]);

  useEffect(() => {
    if (logScrollRef.current) {
      logScrollRef.current.scrollTop = logScrollRef.current.scrollHeight;
    }
  }, [logLines]);

  function loadHistory() {
    fetch('/deploy-api/history', { credentials: 'include' })
      .then(r => r.json())
      .then(setHistory)
      .catch(() => {});
  }

  function appendLog(text, status = 'info') {
    const id = nextId();
    setLogLines(prev => [...prev, { id, text, status }]);
    return id;
  }

  function updateLog(id, text, status) {
    setLogLines(prev => prev.map(l => l.id === id ? { ...l, text, status } : l));
  }

  function appendDivider(text) {
    const id = nextId();
    setLogLines(prev => [...prev, { id, text, status: 'divider' }]);
    return id;
  }

  function statusToLogStatus(raw) {
    if (raw === 'SUCCESS') return 'success';
    if (raw === 'FAILURE') return 'error';
    if (raw === 'IN_PROGRESS') return 'running';
    if (raw === 'SKIPPED') return 'muted';
    return 'info';
  }

  function pollUntilDone(recordId, attempts = 0) {
    if (attempts >= 60) return;
    fetch(`/deploy-api/history/${recordId}/status`, { credentials: 'include' })
      .then(r => r.json())
      .then(updated => {
        setHistory(prev => prev.map(h => h.id === updated.id ? updated : h));
        const terminal = ['SUCCESS', 'FAILURE', 'CANCELLED'].includes(updated.workflowStatus);
        if (!terminal) setTimeout(() => pollUntilDone(recordId, attempts + 1), 15_000);
      })
      .catch(() => {});
  }

  function loadFolderTree() {
    setFoldersLoading(true);
    fetch(`/deploy-api/tree?repo=${encodeURIComponent(selectedRepo)}&branch=${encodeURIComponent(baseBranch)}`, { credentials: 'include' })
      .then(r => r.json())
      .then(data => { setFolders(data); setFolderLoaded(true); setFoldersLoading(false); })
      .catch(() => { setFolderLoaded(true); setFoldersLoading(false); });
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOver(false);
    const dropped = Array.from(e.dataTransfer.files || []);
    if (dropped.length) setFiles(prev => [...prev, ...dropped]);
  }

  function removeFile(index) {
    setFiles(prev => prev.filter((_, i) => i !== index));
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!files.length || !selectedRepo || !baseBranch || !targetBranch || !newBranchName) return;

    if (branches.includes(newBranchName)) {
      setConfirmDialog({ branchName: newBranchName, fileCount: files.length });
      return;
    }

    startUpload();
  }

  function startUpload() {
    setConfirmDialog(null);
    setPhase('uploading');
    setUploadProgress(0);
    setLogLines([]);
    setPrUrl(null);
    setBranchUrl(null);
    setRunUrl(null);
    stepLinesRef.current = {};

    const formData = new FormData();
    files.forEach(f => formData.append('files', f));
    formData.append('repo', selectedRepo);
    formData.append('baseBranch', baseBranch);
    formData.append('targetBranch', targetBranch);
    formData.append('newBranchName', newBranchName);
    formData.append('autoMerge', String(autoMerge));
    formData.append('targetFolder', targetFolder.trim());

    const xhr = new XMLHttpRequest();
    xhr.open('POST', '/deploy-api/upload');
    xhr.withCredentials = true;

    xhr.upload.addEventListener('progress', ev => {
      if (ev.lengthComputable) setUploadProgress(Math.round(ev.loaded / ev.total * 100));
    });

    xhr.addEventListener('load', () => {
      if (xhr.status !== 200) {
        try {
          const err = JSON.parse(xhr.responseText);
          appendLog(err.error || 'Erro no upload', 'error');
        } catch {
          appendLog('Erro no upload', 'error');
        }
        setPhase('error');
        return;
      }
      const { jobId } = JSON.parse(xhr.responseText);
      setPhase('streaming');
      openStream(jobId);
    });

    xhr.addEventListener('error', () => {
      appendLog('Falha na conexão com o servidor', 'error');
      setPhase('error');
    });

    xhr.send(formData);
  }

  function openStream(jobId) {
    if (esRef.current) esRef.current.close();

    const es = new EventSource(`/deploy-api/upload/stream/${jobId}`, { withCredentials: true });
    esRef.current = es;

    es.addEventListener('step', ev => appendLog(ev.data, 'info'));

    es.addEventListener('workflowStart', ev => appendDivider(ev.data));

    es.addEventListener('workflowStep', ev => {
      const parts = ev.data.split('|');
      const statusPart = parts[0];
      const jobName = parts[1] || '';
      const stepName = parts[2] || '';
      const key = `${jobName}|${stepName}`;
      const text = stepName ? `${jobName} › ${stepName}` : jobName;
      const logStatus = statusToLogStatus(statusPart);

      if (stepLinesRef.current[key] != null) {
        updateLog(stepLinesRef.current[key], text, logStatus);
      } else {
        const id = appendLog(text, logStatus);
        stepLinesRef.current[key] = id;
      }
    });

    es.addEventListener('done', ev => {
      const [pr, branch, , run] = ev.data.split('|');
      setPrUrl(pr || null);
      setBranchUrl(branch || null);
      setRunUrl(run || null);
      setPhase('done');
      es.close();
      esRef.current = null;
      fetch('/deploy-api/history', { credentials: 'include' })
        .then(r => r.json())
        .then(data => {
          setHistory(data);
          const active = data.find(r => ['PENDING', 'QUEUED', 'IN_PROGRESS'].includes(r.workflowStatus));
          if (active) setTimeout(() => pollUntilDone(active.id), 10_000);
        })
        .catch(() => {});
    });

    es.addEventListener('error', ev => {
      appendLog(ev.data || 'Erro no processamento', 'error');
      setPhase('error');
      es.close();
      esRef.current = null;
    });

    es.onerror = () => {
      if (esRef.current !== es) return;
      appendLog('Conexão perdida com o servidor', 'error');
      setPhase('error');
      es.close();
      esRef.current = null;
    };
  }

  const canSubmit = files.length > 0 && selectedRepo && baseBranch && targetBranch && newBranchName
    && phase !== 'uploading' && phase !== 'streaming';

  if (authState === 'loading') {
    return (
      <SecondaryLayout>
        <div className={s.page}><span className={s.loadingText}>Carregando…</span></div>
      </SecondaryLayout>
    );
  }

  return (
    <SecondaryLayout>
      <div className={s.page}>
        <div className={s.header}>
          <div>
            <h1 className={s.title}>EVIM Deploy</h1>
            <p className={s.subtitle}>Faça upload de arquivos e inicie workflows do GitHub Actions</p>
          </div>
          {githubLogin && (
            <div className={s.headerRight}>
              <span className={s.githubBadge}>
                {GH_ICON}
                {githubLogin}
              </span>
              <button
                type="button"
                className={s.logoutBtn}
                title="Desconectar GitHub"
                onClick={handleGithubLogout}>
                <Icon.Logout className="ic ic-sm" />
              </button>
            </div>
          )}
        </div>

        {authState === 'unauthenticated' && (
          <div className={s.connectBanner}>
            <p className={s.connectText}>Conecte sua conta GitHub.</p>
            <a href="/deploy-api/oauth2/authorization/github" className={s.connectBtn}>
              {GH_ICON}
              Conectar com GitHub
            </a>
          </div>
        )}

        {authState === 'authenticated' && (
          <>
            <div className={s.mainGrid}>
              <form className={s.card} onSubmit={handleSubmit}>
                <div className={s.fieldGroup}>
                  <label className={s.label}>Repositório</label>
                  <select
                    className={s.select}
                    value={selectedRepo}
                    onChange={e => setSelectedRepo(e.target.value)}
                    disabled={reposLoading}>
                    <option value="">{reposLoading ? 'Carregando…' : 'Selecione um repositório'}</option>
                    {repos.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>

                <div className={s.branchRow}>
                  <div className={s.fieldGroup}>
                    <label className={s.label}>Branch base</label>
                    <select
                      className={s.select}
                      value={baseBranch}
                      onChange={e => setBaseBranch(e.target.value)}
                      disabled={!selectedRepo || branchesLoading}>
                      <option value="">{branchesLoading ? 'Carregando…' : 'Selecione'}</option>
                      {branches.map(b => <option key={b} value={b}>{b}</option>)}
                    </select>
                  </div>
                  <div className={s.fieldGroup}>
                    <label className={s.label}>Branch alvo</label>
                    <select
                      className={s.select}
                      value={targetBranch}
                      onChange={e => setTargetBranch(e.target.value)}
                      disabled={!selectedRepo || branchesLoading}>
                      <option value="">Selecione</option>
                      {branches.map(b => <option key={b} value={b}>{b}</option>)}
                    </select>
                  </div>
                </div>

                <div className={s.fieldGroup}>
                  <label className={s.label}>Nova branch</label>
                  <input
                    className={s.input}
                    type="text"
                    placeholder="ex: feature/meu-arquivo-v2"
                    value={newBranchName}
                    onChange={e => setNewBranchName(e.target.value)} />
                </div>

                <div className={s.toggleRow}>
                  <div className={s.toggleInfo}>
                    <span className={s.toggleLabel}>Auto-merge do Pull Request</span>
                    <span className={s.toggleHint}>Merge automático após abertura do PR</span>
                  </div>
                  <button
                    type="button"
                    className={`${s.toggle}${autoMerge ? ` ${s.toggleOn}` : ''}`}
                    onClick={() => setAutoMerge(v => !v)}
                    aria-pressed={autoMerge}>
                    <span className={s.toggleThumb} />
                  </button>
                </div>

                <div className={s.fieldGroup}>
                  <div className={s.folderLabelRow}>
                    <label className={s.label}>Pasta de destino</label>
                    {selectedRepo && baseBranch && !folderLoaded && (
                      <button type="button" className={s.loadFoldersBtn}
                        disabled={foldersLoading} onClick={loadFolderTree}>
                        {foldersLoading ? 'Carregando…' : 'Carregar pastas'}
                      </button>
                    )}
                  </div>

                  {folderLoaded && treeData && (
                    <>
                      <div className={s.treeWrap}>
                        <div className={s.treeItem} style={{ paddingLeft: 8 }}>
                          <span
                            className={s.treeClickable}
                            style={{ display: 'flex', alignItems: 'center', padding: '0 4px', margin: '0 -4px' }}
                            onClick={() => setRootOpen(o => !o)}>
                            <ChevIcon open={rootOpen} />
                          </span>
                          <span
                            className={`${s.treeClickable} ${s.treeRootLabel}`}
                            onClick={() => setTargetFolder('')}>
                            <FolderIcon open={rootOpen} />
                            <span className={s.treeName}>/ (raiz)</span>
                          </span>
                        </div>
                        {rootOpen && sortedChildren(treeData).map(node => (
                          <TreeNode key={node.path} node={node} depth={1} onSelect={setTargetFolder} />
                        ))}
                      </div>
                      <div className={s.treePathBadge}>
                        <FolderIcon open />
                        <span>{targetFolder ? targetFolder + '/' : '/ (raiz)'}</span>
                      </div>
                    </>
                  )}

                </div>

                <div
                  className={`${s.dropZone}${dragOver ? ` ${s.dropZoneOver}` : ''}${files.length > 0 ? ` ${s.dropZoneHasFile}` : ''}`}
                  onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                  onDragLeave={() => setDragOver(false)}
                  onDrop={handleDrop}
                  onClick={() => document.getElementById('deployFileInput').click()}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => e.key === 'Enter' && document.getElementById('deployFileInput').click()}>
                  <input
                    id="deployFileInput"
                    type="file"
                    multiple
                    style={{ display: 'none' }}
                    onChange={e => {
                      const picked = Array.from(e.target.files || []);
                      if (picked.length) setFiles(prev => [...prev, ...picked]);
                      e.target.value = '';
                    }} />
                  {files.length === 0
                    ? (
                      <>
                        <span className={s.dropText}>Arraste arquivos ou clique para selecionar</span>
                        <span className={s.dropHint}>Múltiplos arquivos permitidos · Exceto scripts executáveis</span>
                      </>
                    )
                    : (
                      <span className={s.dropText}>{files.length} arquivo(s) selecionado(s) — clique para adicionar mais</span>
                    )
                  }
                </div>

                {files.length > 0 && (
                  <ul className={s.fileList}>
                    {files.map((f, i) => (
                      <li key={i} className={s.fileItem}>
                        <span className={s.fileName}>{f.name}</span>
                        <span className={s.fileSize}>{(f.size / 1024 / 1024).toFixed(1)} MB</span>
                        <button type="button" className={s.fileRemove} onClick={() => removeFile(i)}>✕</button>
                      </li>
                    ))}
                  </ul>
                )}

                {phase === 'uploading' && (
                  <div className={s.progressWrap}>
                    <div className={s.progressBar}>
                      <div className={s.progressFill} style={{ width: `${uploadProgress}%` }} />
                    </div>
                    <span className={s.progressLabel}>{uploadProgress}%</span>
                  </div>
                )}

                {confirmDialog && (
                  <div className={s.confirmBox}>
                    <strong className={s.confirmTitle}>
                      A branch <code className={s.confirmCode}>{confirmDialog.branchName}</code> já existe.
                    </strong>
                    <p className={s.confirmMsg}>
                      {confirmDialog?.fileCount === 1 ? 'O arquivo' : `Os ${confirmDialog?.fileCount} arquivos`} serão
                      commitados nessa branch em um único commit.{' '}
                      <br />Arquivos com o mesmo nome serão substituídos.
                    </p>
                    <div className={s.confirmActions}>
                      <button
                        type="button"
                        className={s.confirmCancel}
                        onClick={() => setConfirmDialog(null)}>
                        Cancelar
                      </button>
                      <button
                        type="button"
                        className={s.confirmProceed}
                        onClick={startUpload}>
                        Continuar
                      </button>
                    </div>
                  </div>
                )}

                <button type="submit" className={s.submitBtn} disabled={!canSubmit || !!confirmDialog}>
                  {phase === 'uploading' ? 'Enviando…' : phase === 'streaming' ? 'Processando…' : 'Criar Branch & Deploy'}
                </button>
              </form>

              {logLines.length > 0 && (
                <div className={s.activityCard}>
                  <div className={s.activityHeader}>Log de atividade</div>
                  <div className={s.logScroll} ref={logScrollRef}>
                    {logLines.map(line => (
                      <div key={line.id} className={`${s.logLine} ${s['log_' + line.status] || ''}`}>
                        {line.status === 'divider'
                          ? <span className={s.logDivider}>{line.text}</span>
                          : (
                            <>
                              <span className={`${s.logDot} ${s['dot_' + line.status] || ''}`} />
                              <span>{line.text}</span>
                            </>
                          )
                        }
                      </div>
                    ))}
                  </div>
                  {phase === 'done' && (prUrl || branchUrl || runUrl) && (
                    <div className={s.resultLinks}>
                      {prUrl && (
                        <a href={prUrl} target="_blank" rel="noreferrer" className={s.resultLink}>
                          Visualizar Pull Request
                          <Icon.ExtLink className={s.resultLinkIcon} />
                        </a>
                      )}
                      {branchUrl && (
                        <a href={branchUrl} target="_blank" rel="noreferrer" className={s.resultLink}>
                          Visualizar Branch
                          <Icon.ExtLink className={s.resultLinkIcon} />
                        </a>
                      )}
                      {runUrl && (
                        <a href={runUrl} target="_blank" rel="noreferrer" className={s.resultLink}>
                          Visualizar Workflow
                          <Icon.ExtLink className={s.resultLinkIcon} />
                        </a>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>

            <div className={s.historySection}>
              <div className={s.historyHeader}>
                <span className={s.historyTitle}>Histórico de deploys</span>
                <div className={s.historyFilters}>
                  <input
                    className={s.historySearch}
                    placeholder="Buscar repo, arquivo, branch…"
                    value={historySearch}
                    onChange={e => setHistorySearch(e.target.value)} />
                  <select
                    className={s.historyStatusSelect}
                    value={historyStatusFilter}
                    onChange={e => setHistoryStatusFilter(e.target.value)}>
                    <option value="">Todos os status</option>
                    {Object.entries(STATUS_LABELS).map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                  {(historySearch || historyStatusFilter) && (
                    <button
                      className={s.historyClearBtn}
                      onClick={() => { setHistorySearch(''); setHistoryStatusFilter(''); }}>
                      Limpar
                    </button>
                  )}
                  {history.length > 0 && (historySearch || historyStatusFilter) && (
                    <span className={s.historyCount}>
                      {filteredHistory.length}/{history.length}
                    </span>
                  )}
                </div>
              </div>
              {history.length === 0
                ? <p className={s.emptyHistory}>Nenhum deploy realizado ainda.</p>
                : filteredHistory.length === 0
                  ? <p className={s.emptyHistory}>Nenhum resultado para os filtros aplicados.</p>
                  : (
                  <div className={s.tableWrap}>
                    <table className={s.table}>
                      <thead>
                        <tr>
                          <th>Data</th>
                          <th>Repositório</th>
                          <th>Arquivo</th>
                          <th>Branch criada</th>
                          <th>PR</th>
                          <th>Workflow</th>
                          <th>Por</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredHistory.map(r => (
                          <tr key={r.id}>
                            <td>{r.createdAt ? new Date(r.createdAt).toLocaleString('pt-BR') : '—'}</td>
                            <td>
                              <a href={`https://github.com/${r.repo}`} target="_blank" rel="noreferrer" className={s.tableLink}>
                                {r.repo} <Icon.ExtLink className={s.resultLinkIcon} />
                              </a>
                            </td>
                            <td>
                              {r.fileName
                                ? r.fileName.split(', ').map((f, i) => (
                                    <div key={i} className={s.mono} style={{ whiteSpace: 'nowrap' }}>{f}</div>
                                  ))
                                : '—'}
                            </td>
                            <td className={s.mono}>
                              {r.branchUrl
                                ? <a href={r.branchUrl} target="_blank" rel="noreferrer" className={s.tableLink}>{r.newBranchName} <Icon.ExtLink className={s.resultLinkIcon} /></a>
                                : r.newBranchName}
                            </td>
                            <td>
                              {r.prUrl
                                ? <a href={r.prUrl} target="_blank" rel="noreferrer" className={s.tableLink}>PR <Icon.ExtLink className={s.resultLinkIcon} /></a>
                                : '—'}
                            </td>
                            <td>
                              {r.workflowRunId
                                ? <a href={`https://github.com/${r.repo}/actions/runs/${r.workflowRunId}`} target="_blank" rel="noreferrer" className={s.tableLink}>Run <Icon.ExtLink className={s.resultLinkIcon} /></a>
                                : '—'}
                            </td>
                            <td>
                              {r.triggeredBy
                                ? <a href={`https://github.com/${r.triggeredBy}`} target="_blank" rel="noreferrer" className={s.tableLink}>
                                    {r.triggeredBy} <Icon.ExtLink className={s.resultLinkIcon} />
                                  </a>
                                : '—'}
                            </td>
                            <td><StatusBadge status={r.workflowStatus} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )
              }
            </div>
          </>
        )}
      </div>
    </SecondaryLayout>
  );
}
