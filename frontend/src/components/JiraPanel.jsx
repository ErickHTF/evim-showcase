import { useState, useEffect } from 'react';
import styles from './css/JiraPanel.module.css';
import { useJiraCard } from '../hooks/useJiraCard';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getAssignableUsers,
  getPriorities,
  addCardComment,
  patchCard,
  attachmentContentUrl,
  getAuthToken,
} from '../services/api';
import { textAdf } from '../utils/string';
import { useToast } from '../hooks/useToast.jsx';

function AuthImage({ src, alt, className, title, onClick }) {
  const [blobUrl, setBlobUrl] = useState(null);
  useEffect(() => {
    let revoke;
    const token = getAuthToken();
    fetch(src, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then(r => { if (r.ok) return r.blob(); throw new Error(r.status); })
      .then(blob => {
        const url = URL.createObjectURL(blob);
        setBlobUrl(url);
        revoke = url;
      })
      .catch(() => setBlobUrl(null));
    return () => { if (revoke) URL.revokeObjectURL(revoke); };
  }, [src]);
  if (!blobUrl) return null;
  return <img src={blobUrl} alt={alt ?? ''} className={className} title={title} onClick={onClick} loading="lazy" />;
}

function AuthFileLink({ href, className, children }) {
  function handleClick(e) {
    e.preventDefault();
    const token = getAuthToken();
    fetch(href, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then(r => { if (r.ok) return r.blob(); throw new Error(r.status); })
      .then(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = '';
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
      })
      .catch(() => alert('Erro ao baixar anexo'));
  }
  return <a className={className} href={href} onClick={handleClick} rel="noreferrer">{children}</a>;
}

export default function JiraPanel({ jiraId, onClose }) {
  const { card, transitions, loading, error } = useJiraCard(jiraId);
  const queryClient = useQueryClient();

  const { data: users = [] }      = useQuery({ queryKey: ['users'],      queryFn: ({ signal }) => getAssignableUsers(signal) });
  const { data: priorities = [] } = useQuery({ queryKey: ['priorities'], queryFn: ({ signal }) => getPriorities(signal) });

  const [comment, setComment] = useState('');
  const [draft, setDraft] = useState({});
  const [newLabel, setNewLabel] = useState('');
  const [lightbox, setLightbox] = useState(null);
  const { showToast } = useToast();

  const notify = (ok, msg) => showToast(msg, { type: ok ? 'success' : 'error' });

  useEffect(() => {
    if (!card) return;
    setDraft({
      summary: card.summary ?? '',
      vendorTicket: card.vendorTicket ?? '',
      accountId: card.assigneeAccountId ?? '',
      priorityId: card.priorityId ?? '',
      component: card.component ?? '',
      labels: card.labels ?? [],
      transitionId: '',
    });
  }, [card]);

  function setField(field, value) {
    setDraft(prev => ({ ...prev, [field]: value }));
  }

  function addLabel() {
    if (!newLabel.trim()) return;
    setDraft(prev => ({
      ...prev,
      labels: [...(prev.labels || []), newLabel.trim()]
    }));
    setNewLabel('');
  }

  function removeLabel(index) {
    setDraft(prev => ({
      ...prev,
      labels: prev.labels.filter((_, i) => i !== index)
    }));
  }

  function buildPatch() {
    if (!card) return null;
    const patch = {};
    if (draft.summary !== (card.summary ?? '')) patch.summary = draft.summary;
    if (draft.vendorTicket !== (card.vendorTicket ?? '')) patch.vendorTicket = draft.vendorTicket;
    if (draft.accountId !== (card.assigneeAccountId ?? '')) patch.accountId = draft.accountId;
    if (draft.priorityId !== (card.priorityId ?? '')) patch.priorityId = draft.priorityId;
    if (draft.component !== (card.component ?? '')) patch.component = draft.component;
    if (JSON.stringify(draft.labels) !== JSON.stringify(card.labels || [])) patch.labels = draft.labels;
    if (draft.transitionId) patch.transitionId = draft.transitionId;
    return Object.keys(patch).length > 0 ? patch : null;
  }

  const isDirty = !!buildPatch();

  const saveMutation = useMutation({
    mutationFn: (patch) => patchCard(card.key, patch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['card', jiraId] });
      queryClient.invalidateQueries({ queryKey: ['transitions', jiraId] });
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      notify(true, 'Salvo com sucesso');
    },
    onError: (err) => {
      console.error('Erro ao salvar:', err);
      notify(false, 'Erro ao salvar');
    },
  });

  const commentMutation = useMutation({
    mutationFn: (text) => addCardComment(card.key, textAdf(text)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['card', jiraId] });
      setComment('');
      notify(true, 'Comentário adicionado');
    },
    onError: (err) => {
      console.error('Erro ao comentar:', err);
      notify(false, 'Erro ao comentar');
    },
  });

  function handleSave() {
    const patch = buildPatch();
    if (patch) saveMutation.mutate(patch);
  }

  function handleAddComment() {
    if (comment.trim()) commentMutation.mutate(comment);
  }

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <strong>Campos Jira</strong>
        <button className={styles.btnFechar} onClick={onClose}>Fechar</button>
      </div>

      <div className={styles.content}>
        {loading && <p className={styles.loading}>Carregando...</p>}
        {error && <p className={styles.erro}>{error}</p>}

        {card && (
          <>
            <div className={styles.metaGroup}>
              <h4 className={styles.metaTitle}>
                <svg viewBox="0 0 24 24" style={{width: '10px', height: '10px', color: '#2684ff'}}>
                  <path fill="currentColor" d="M11.53 2c0 2.4 1.97 4.35 4.35 4.35h1.78v1.72a4.35 4.35 0 0 0 4.34 4.35V2.84a.84.84 0 0 0-.84-.84zM7.21 6.3c0 2.4 1.95 4.34 4.34 4.34h1.78v1.72a4.35 4.35 0 0 0 4.35 4.35V7.15a.85.85 0 0 0-.85-.84zM2.89 10.61c0 2.4 1.95 4.35 4.34 4.35h1.78v1.72a4.35 4.35 0 0 0 4.35 4.34v-9.55a.85.85 0 0 0-.85-.85z" />
                </svg>
                {card.key}
              </h4>

              <div className={styles.metaRow}>
                <span className={styles.metaLabel}>Responsável</span>
                <span className={styles.metaValue}>
                  <select value={draft.accountId} onChange={e => setField('accountId', e.target.value)}>
                    <option value="">— Não atribuído —</option>
                    {users.map(u => (
                      <option key={u.accountId} value={u.accountId}>{u.displayName}</option>
                    ))}
                  </select>
                </span>
              </div>

              <div className={styles.metaRow}>
                <span className={styles.metaLabel}>Prioridade</span>
                <span className={styles.metaValue}>
                  <select value={draft.priorityId} onChange={e => setField('priorityId', e.target.value)}>
                    {priorities.map(p => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </span>
              </div>

              <div className={styles.metaRow} style={{ borderTop: '1px solid var(--border)', paddingTop: '12px', marginTop: '12px' }}>
                <span className={styles.metaLabel}>Status</span>
                <span className={styles.metaValue}>
                  <select value={draft.transitionId} onChange={e => setField('transitionId', e.target.value)}>
                    <option value="">{card.status}</option>
                    {transitions.map(t => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </span>
              </div>
              <div className={styles.metaRow}>
                <span className={styles.metaLabel}>ID Fornecedor</span>
                <span className={styles.metaValue}>
                  <input
                    value={draft.vendorTicket ?? ''}
                    onChange={e => setField('vendorTicket', e.target.value)}
                    placeholder="Preencha com o ID do ticket"
                  />
                </span>
              </div>

              <div className={styles.metaRow}>
                <span className={styles.metaLabel}>Título</span>
                <span className={styles.metaValue}>
                  <input
                    value={draft.summary ?? ''}
                    onChange={e => setField('summary', e.target.value)}
                  />
                </span>
              </div>
            </div>

            <div className={styles.section}>
              <button
                className={styles.btnSave}
                onClick={handleSave}
                disabled={!isDirty || saveMutation.isPending}
              >
                {saveMutation.isPending ? 'Salvando...' : 'Salvar alterações'}
              </button>
            </div>

            {card.attachments && card.attachments.length > 0 && (
              <div className={styles.section}>
                <p className={styles.sectionTitle}>Anexos</p>
                <div className={styles.attachments}>
                  {card.attachments.map(att => (
                    (att.mimeType?.startsWith('image/') || /\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i.test(att.filename)) ? (
                      <AuthImage
                        key={att.id}
                        className={`${styles.attachmentThumb} ${styles.clickable}`}
                        src={attachmentContentUrl(att.id)}
                        alt={att.filename}
                        title={att.filename}
                        onClick={() => setLightbox(attachmentContentUrl(att.id))}
                      />
                    ) : (
                      <AuthFileLink key={att.id} className={styles.attachmentFile} href={attachmentContentUrl(att.id)}>
                        📎 {att.filename}
                      </AuthFileLink>
                    )
                  ))}
                </div>
              </div>
            )}

            <div className={styles.section}>
              <p className={styles.sectionTitle}>Comentários</p>

              <textarea
                className={styles.textarea}
                placeholder="Novo comentário..."
                value={comment}
                onChange={e => setComment(e.target.value)}
                rows={3}
              />
              <button className={styles.btnAdd} onClick={handleAddComment}
                disabled={commentMutation.isPending}>
                {commentMutation.isPending ? 'Enviando...' : 'Comentar'}
              </button>

              <div style={{ borderTop: '2px solid var(--border)', margin: '4px 0 20px', opacity: 0.6 }} />

              {(!card.comments || card.comments.length === 0) && (
                <p className={styles.noComments}>Sem comentários.</p>
              )}

              {card.comments?.map((c, i) => (
                <div key={`${c.author}-${i}`} className={styles.commentItem}>
                  <div className={styles.commentAuthor}>
                    <div className={styles.avatar} style={{ overflow: 'hidden' }}>
                      {c.authorAvatarUrl
                        ? <img src={c.authorAvatarUrl} alt={c.author} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
                        : c.author?.charAt(0)}
                    </div>
                    <span>{c.author}</span>
                  </div>
                  <p className={styles.commentBody}>{c.body}</p>
                  {c.attachmentIds?.length > 0 && (
                    <div className={styles.commentAttachments}>
                      {c.attachmentIds.map(id => (
                        <AuthImage
                          key={id}
                          className={`${styles.attachmentThumb} ${styles.clickable}`}
                          src={attachmentContentUrl(id)}
                          alt="anexo"
                          onClick={() => setLightbox(attachmentContentUrl(id))}
                        />
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>

          </>
        )}
      </div>

      {lightbox && (
        <div className={styles.lightboxOverlay} onClick={() => setLightbox(null)}>
          <AuthImage className={styles.lightboxImg} src={lightbox} alt="preview" onClick={e => e.stopPropagation()} />
        </div>
      )}
    </div>
  );
}
