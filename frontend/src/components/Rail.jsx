import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import Icon from './Icons';
import s from './css/Rail.module.css';

export default function Rail({ activeSection, setActiveSection, unreadCount }) {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const path = location.pathname;

  function isActive(section) {
    if (section === 'inbox') return path === '/' || activeSection === 'inbox';
    return path === `/${section}`;
  }

  return (
    <div className={s.rail} data-tour="rail">
      <div className={s.railLogo} title="EVIM" onClick={() => navigate('/')}><Icon.Logo className={s.railLogoSvg} /></div>

      <button
        className={`${s.railBtn}${isActive('inbox') ? ` ${s.active}` : ''}`}
        onClick={() => setActiveSection ? setActiveSection('inbox') : navigate('/')}
        title="Caixa de Entrada">
        <Icon.Inbox className="ic" />
        {unreadCount > 0 && <span className={s.count}>{unreadCount}</span>}
      </button>

      <button
        className={`${s.railBtn}${path === '/deploy' ? ` ${s.active}` : ''}`}
        onClick={() => navigate('/deploy')}
        title="Deploy">
        <Icon.Git className="ic" />
      </button>

      <div className={s.spacer} />

      <button
        className={`${s.railBtn}${path === '/metrics' ? ` ${s.active}` : ''}`}
        onClick={() => navigate('/metrics')}
        data-tour="rail-metrics"
        title="Métricas de SLA">
        <Icon.Activity className="ic" />
      </button>

      <button
        className={`${s.railBtn}${path === '/admin' ? ` ${s.active}` : ''}`}
        onClick={() => navigate('/admin')}
        title="Configurações">
        <Icon.Settings className="ic" />
      </button>

      {auth.isAuthenticated && (
        <button
          className={`${s.railBtn} ${s.danger}`}
          title="Sair"
          onClick={() => auth.signoutRedirect()}>
          <Icon.Logout className="ic" />
        </button>
      )}
    </div>
  );
}
