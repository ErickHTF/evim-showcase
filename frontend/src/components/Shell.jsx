import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import Icon from './Icons';
import s from './css/Shell.module.css';
import { useOutsideClick } from '../hooks/useOutsideClick';

function Rail({ activeSection, setActiveSection, unreadCount, onLogout }) {
  const navigate = useNavigate();
  return (
    <div className={s.rail}>
      <div className={s.railLogo} title="EVIM"><Icon.Logo className={s.railLogoSvg} /></div>
      <button
        className={`${s.railBtn}${activeSection === 'inbox' ? ` ${s.active}` : ''}`}
        onClick={() => setActiveSection('inbox')}
        title="Caixa de Entrada">
        <Icon.Inbox className="ic" />
        {unreadCount > 0 && <span className={s.count}>{unreadCount}</span>}
      </button>
      <button className={`${s.railBtn}`} title="Deploy" onClick={() => navigate('/deploy')}><Icon.Git className="ic" /></button>
      <div className={s.railSpacer} />
      <button className={s.railBtn} title="Métricas de SLA" onClick={() => navigate('/metrics')}><Icon.Activity className="ic" /></button>
      <button className={s.railBtn} title="Configurações" onClick={() => navigate('/admin')}><Icon.Settings className="ic" /></button>
      {onLogout && (
        <button className={`${s.railBtn} ${s.danger}`} title="Sair" onClick={onLogout}>
          <Icon.Logout className="ic" />
        </button>
      )}
    </div>
  );
}

function getInitials(profile) {
  if (!profile) return '?';
  if (profile.given_name && profile.family_name)
    return (profile.given_name[0] + profile.family_name[0]).toUpperCase();
  if (profile.name)
    return profile.name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
  if (profile.email)
    return profile.email[0].toUpperCase();
  return '?';
}

function TopBar({ searchQuery = '', onSearchChange, userProfile, jiraUser, onLogout, density = 'comfortable', setDensity }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const initials = getInitials(userProfile);
  const keycloakName = userProfile?.name ?? userProfile?.email ?? 'Usuário';

  useOutsideClick(menuRef, menuOpen, () => setMenuOpen(false));

  const isDense = density === 'dense';
  const toggleDensity = () => setDensity?.(isDense ? 'comfortable' : 'dense');

  return (
    <div className={s.topbar}>
      <div className={s.topbarSearch} data-tour="topbar-search">
        <Icon.Search className="ic ic-sm" style={{ color: 'var(--fg-4)' }} />
        <input
          placeholder="Buscar tickets, fornecedores, chaves Jira…"
          value={searchQuery}
          onChange={e => onSearchChange?.(e.target.value)} />
        <kbd>⌘K</kbd>
      </div>
      <div className={s.syncPill} title="Sincronização Jira em tempo real">
        <span className={s.dot} />
        <Icon.JiraLogo style={{ width: 11, height: 11 }} />
        <span className={s.syncPillLabel}>Jira · sincronizado</span>
      </div>
      <div className={s.topbarRight}>
        <button
          className={`btn ghost icon ${isDense ? s.densityActive : ''}`}
          onClick={toggleDensity}
          aria-pressed={isDense}
          title={isDense ? 'Densidade: compacta (clique para confortável)' : 'Densidade: confortável (clique para compacta)'}>
          <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
            {isDense ? (
              <>
                <rect x="2" y="3"  width="10" height="1.4" rx="0.7" fill="currentColor" />
                <rect x="2" y="6.3" width="10" height="1.4" rx="0.7" fill="currentColor" />
                <rect x="2" y="9.6" width="10" height="1.4" rx="0.7" fill="currentColor" />
              </>
            ) : (
              <>
                <rect x="2" y="2.5" width="10" height="2" rx="0.8" fill="currentColor" />
                <rect x="2" y="6"   width="10" height="2" rx="0.8" fill="currentColor" />
                <rect x="2" y="9.5" width="10" height="2" rx="0.8" fill="currentColor" />
              </>
            )}
          </svg>
        </button>
        <button className="btn ghost sm"><Icon.Sparkle className="ic ic-sm" /> IA</button>
        <span style={{ width: 1, height: 20, background: 'var(--border)' }} />

        <div className={s.menuWrapper} ref={menuRef}>
          <div className="avatar" onClick={() => setMenuOpen(o => !o)}
               style={{ padding: 0, overflow: 'hidden', cursor: 'pointer' }}>
            {jiraUser?.avatarUrl
              ? <img src={jiraUser.avatarUrl} alt={initials} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              : initials}
          </div>

          {menuOpen && (
            <div className={s.userMenu}>
              <div className={s.menuHeader}>
                <div className={`avatar ${s.menuAvatar}`}>
                  {jiraUser?.avatarUrl
                    ? <img src={jiraUser.avatarUrl} alt={initials} />
                    : initials}
                </div>
                <div style={{ minWidth: 0 }}>
                  <div className={s.menuUserName}>
                    {keycloakName}
                  </div>
                  {userProfile?.preferred_username && (
                    <div className={s.menuUserEmail}>
                      {userProfile.preferred_username}
                    </div>
                  )}
                </div>
              </div>

              {jiraUser?.displayName && (
                <div className={s.menuSection}>
                  <div className={s.menuSectionLabel}>Conta Jira</div>
                  <div className={s.menuJiraAccount}>
                    <Icon.JiraLogo style={{ width: 12, height: 12, flexShrink: 0 }} />
                    {jiraUser.displayName}
                  </div>
                </div>
              )}

              <div className={s.menuLogout}>
                <button
                  onClick={() => {
                    setMenuOpen(false);
                    import('./Tour').then(m => m.resetTour());
                  }}
                  className={s.menuLogoutBtn}
                  style={{ color: 'var(--fg-2)' }}>
                  <Icon.Sparkle className="ic ic-sm" />
                  Repetir tour
                </button>
                <button onClick={() => { setMenuOpen(false); onLogout?.(); }}
                  className={s.menuLogoutBtn}>
                  <Icon.Logout className="ic ic-sm" />
                  Sair
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Sidebar({
  sidebarOpen, setSidebarOpen,
  quickFilter, setQuickFilter,
  noVendorFilter, setNoVendorFilter,
  selectedUser, setSelectedUser,
  selectedStatuses, setSelectedStatuses,
  users = [], jiraStatuses = [], statusColors = {},
  sprints = [], selectedSprintId, setSelectedSprintId,
  pageSize, setPageSize,
  counts = {},
}) {
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [userSearch, setUserSearch] = useState('');
  const [statusOpen, setStatusOpen] = useState(false);
  const dropdownRef = useRef(null);

  useOutsideClick(dropdownRef, userDropdownOpen, () => { setUserDropdownOpen(false); setUserSearch(''); });

  const filteredUsers = users.filter(u =>
    u.displayName?.toLowerCase().includes(userSearch.toLowerCase())
  );

  function toggleStatus(st) {
    setSelectedStatuses(
      selectedStatuses.includes(st)
        ? selectedStatuses.filter(x => x !== st)
        : [...selectedStatuses, st]
    );
  }

  const filters = [
    { id: 'all', label: 'Todas abertas', icon: Icon.Inbox, count: counts.all },
    { id: 'urgent', label: 'Precisa resposta', icon: Icon.Flag },
    { id: 'mine', label: 'Atribuído a mim', icon: Icon.Mine, count: counts.mine },
    { id: 'sla', label: 'Risco de SLA', icon: Icon.Clock },
    { id: 'starred', label: 'Favoritos', icon: Icon.Star, count: counts.starred },
    { id: 'unassigned', label: 'Não atribuído', icon: Icon.User, count: counts.unassigned },
  ];

  const selectedUserName = users.find(u => u.accountId === selectedUser)?.displayName;

  return (
    <div className={s.sidebarWrapper}>
      <button
        className={s.sidebarToggleBtn}
        data-tour="sidebar-toggle"
        onClick={() => setSidebarOpen(!sidebarOpen)}
        title={sidebarOpen ? 'Minimizar filtros' : 'Expandir filtros'}>
        <Icon.Chev style={{
          width: 14, height: 14,
          transform: sidebarOpen ? 'rotate(180deg)' : 'rotate(0deg)',
        }} />
      </button>
      <div className={`${s.sidebar} app-sidebar${sidebarOpen ? ` ${s.sidebarOpen}` : ''}`} data-tour="sidebar">
        <div className={s.sidebarBody}>
          <h3>Filtros</h3>
        {filters.map(f => (
          <div key={f.id}
            title={f.label}
            className={`${s.sidebarItem}${quickFilter === f.id ? ` ${s.active}` : ''}`}
            onClick={() => setQuickFilter(f.id)}>
            <f.icon className={s.sIcon} />
            <span>{f.label}</span>
            {Number.isFinite(f.count) && f.count > 0 && (
              <span className={s.filterBadge} aria-label={`${f.count} itens`}>{f.count}</span>
            )}
          </div>
        ))}

        <div
          title="Sem vendor"
          className={`${s.sidebarItem}${noVendorFilter ? ` ${s.active}` : ''}`}
          onClick={() => setNoVendorFilter(!noVendorFilter)}>
          <Icon.ExtLink className={s.sIcon} />
          <span>Sem vendor</span>
          {Number.isFinite(counts.noVendor) && counts.noVendor > 0 && (
            <span className={s.filterBadge} aria-label={`${counts.noVendor} itens`}>{counts.noVendor}</span>
          )}
        </div>

        {users.length > 0 && (
          <>
            <h3>Pessoa</h3>
            <div ref={dropdownRef} className={s.dropdownContainer}>
              <div
                className={`${s.sidebarItem}${selectedUser ? ` ${s.active}` : ''}`}
                onClick={() => setUserDropdownOpen(o => !o)}
                style={{ justifyContent: 'space-between', cursor: 'pointer' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Icon.User className={s.sIcon} />
                  <span>{selectedUserName ?? 'Selecionar pessoa'}</span>
                </span>
                <Icon.ChevDown className="ic-sm" style={{ color: 'var(--fg-4)', flexShrink: 0 }} />
              </div>
              {userDropdownOpen && (
                <div className={s.dropdownMenu}>
                  <input
                    className={s.dropdownSearch}
                    placeholder="Buscar pessoa…"
                    value={userSearch}
                    onChange={e => setUserSearch(e.target.value)}
                    autoFocus
                    onClick={e => e.stopPropagation()}
                  />
                  {selectedUser && (
                    <div className={`${s.dropdownItem} ${s.dropdownItemClear}`} onClick={() => { setSelectedUser(null); setUserDropdownOpen(false); setUserSearch(''); }}>
                      Limpar filtro
                    </div>
                  )}
                  {filteredUsers.map(u => (
                    <div key={u.accountId}
                      className={`${s.dropdownItem}${selectedUser === u.accountId ? ` ${s.dropdownItemActive}` : ''}`}
                      onClick={() => { setSelectedUser(u.accountId); setUserDropdownOpen(false); setUserSearch(''); }}>
                      <div className={s.dropdownItemAvatar}>
                        {u.avatarUrls?.['24x24']
                          ? <img src={u.avatarUrls['24x24']} alt={u.displayName} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
                          : u.displayName?.charAt(0).toUpperCase()}
                      </div>
                      {u.displayName}
                      {selectedUser === u.accountId && <Icon.Check className={`ic-sm ${s.dropdownItemCheck}`} />}
                    </div>
                  ))}
                  {filteredUsers.length === 0 && (
                    <div className={s.dropdownItem} style={{ color: 'var(--fg-4)', cursor: 'default' }}>Nenhum resultado</div>
                  )}
                </div>
              )}
            </div>
          </>
        )}

        {jiraStatuses.length > 0 && (
          <>
            <h3 className={s.collapsibleHeader} onClick={() => setStatusOpen(o => !o)}>
              <span>
                Status
                {selectedStatuses.length > 0 && (
                  <button className={s.clearBtn} onClick={e => { e.stopPropagation(); setSelectedStatuses([]); }}>limpar</button>
                )}
              </span>
              <Icon.ChevDown className="ic-sm" style={{ color: 'var(--fg-4)', transform: statusOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform .2s' }} />
            </h3>
            {statusOpen && (
            <div className={`${s.statusPills} status-pills-desktop`}>
              {jiraStatuses.map(st => {
                const color = statusColors[st] ?? 'var(--fg-4)';
                const active = selectedStatuses.includes(st);
                return (
                  <button
                    key={st}
                    className={`${s.statusPill}${active ? ` ${s.statusPillActive}` : ''}`}
                    style={active ? { background: color + '18', color } : undefined}
                    onClick={() => toggleStatus(st)}>
                    <span className={s.statusDot} style={{ background: color }} />
                    {st}
                  </button>
                );
              })}
            </div>
            )}
            <select
              className={`${s.statusSelect} status-select-mobile`}
              value={selectedStatuses[0] ?? ''}
              onChange={e => setSelectedStatuses(e.target.value ? [e.target.value] : [])}>
              <option value="">Todos os status</option>
              {jiraStatuses.map(st => (
                <option key={st} value={st}>{st}</option>
              ))}
            </select>
          </>
        )}

        {sprints.length > 0 && (
          <>
            <h3>Sprint</h3>
            <select
              className={s.sprintSelect}
              value={selectedSprintId ?? ''}
              onChange={e => setSelectedSprintId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">Todas as sprints</option>
              {sprints.map(sp => (
                <option key={sp.id} value={sp.id}>
                  {sp.name}{sp.state === 'active' ? ' ●' : ''}
                </option>
              ))}
            </select>
          </>
        )}

        <h3>Itens por página</h3>
        <select
          className={s.sprintSelect}
          value={pageSize}
          onChange={e => setPageSize(Number(e.target.value))}>
          {[10, 20, 50, 100].map(n => (
            <option key={n} value={n}>{n}</option>
          ))}
        </select>
      </div>
    </div>
    </div>
  );
}

export { Rail, TopBar, Sidebar };
