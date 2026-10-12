import { useState } from 'react';
import { initiateAtlassianOAuth } from '../services/api';

export default function LinkAtlassianPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleConnect() {
    setLoading(true);
    setError(null);
    try {
      const { redirectUrl } = await initiateAtlassianOAuth();
      window.location.href = redirectUrl;
    } catch (e) {
      setError('Erro ao iniciar conexão com Atlassian.');
      setLoading(false);
    }
  }

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', height: '100vh', gap: 12,
      background: 'var(--bg)', color: 'var(--fg)',
    }}>
      <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>Conectar conta Atlassian</h2>
      <p style={{ margin: 0, fontSize: 13, color: 'var(--fg-3)', maxWidth: 340, textAlign: 'center' }}>
        Para que suas ações no Jira apareçam com sua identidade, conecte sua conta Atlassian.
      </p>
      <button
        className="btn-primary"
        onClick={handleConnect}
        disabled={loading}
        style={{ padding: '10px 28px', fontSize: 14, marginTop: 8 }}
      >
        {loading ? 'Redirecionando...' : 'Conectar Atlassian'}
      </button>
      {error && (
        <p style={{ color: 'var(--red, #ef4444)', fontSize: 13, margin: 0 }}>{error}</p>
      )}
    </div>
  );
}
