import { useState, useEffect, useRef } from 'react';
import Icon from './Icons';
import { PriorityIcon, StatusPill } from './Queue';
import JiraPanel from './JiraPanel';
import { attachmentContentUrl, getAuthToken } from '../services/api';
import { useOutsideClick } from '../hooks/useOutsideClick';
import s from './css/Detail.module.css';

function AuthImage({ src, alt, className, title, onClick, onMouseEnter, onMouseLeave }) {
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
  return <img src={blobUrl} alt={alt ?? ''} className={className} title={title} onClick={onClick} onMouseEnter={onMouseEnter} onMouseLeave={onMouseLeave} loading="lazy" />;
}

function DetailHeader({ ticket, detailTab, setDetailTab, jiraBaseUrl, onBack, starred, onToggleStar }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  useOutsideClick(menuRef, () => setMenuOpen(false));

  return (
    <div className={s.detailHeader}>
      <div className={s.detailBreadcrumb}>
        <button className={s.backBtn} onClick={onBack} aria-label="Voltar">
          ‹ Voltar
        </button>
        <span className={s.jiraLogo}>
          <Icon.JiraLogo className={s.jiraLogoLarge} />
          Jira
        </span>
        <Icon.Chev className={s.chevIcon} />
        <a href={jiraBaseUrl ? `${jiraBaseUrl}/jira/software/projects/${ticket.jiraKey.split('-')[0]}/boards` : '#'} target="_blank" rel="noreferrer">{ticket.jiraKey.split('-')[0]}</a>
        <Icon.Chev className={s.chevIcon} />
        <a
          href={jiraBaseUrl ? `${jiraBaseUrl}/browse/${ticket.jiraKey}` : '#'}
          target="_blank"
          rel="noreferrer"
          title={`Abrir ${ticket.jiraKey} no Jira`}
        >
          {ticket.jiraKey} <Icon.ExtLink className={s.breadcrumbExtIcon} />
        </a>
      </div>
      <div className={s.detailTitleRow}>
        <div className={s.detailTitle} title={ticket.title}>{ticket.title}</div>
        <div className={s.detailActions}>
          <StatusPill status={ticket.status} label={ticket.jiraStatus} />
          <div className={s.moreWrap} ref={menuRef}>
            <button className="btn icon ghost" onClick={() => setMenuOpen(o => !o)}>
              <Icon.More className="ic" />
            </button>
            {menuOpen && (
              <div className={s.moreMenu}>
                <button className={s.menuItem} onClick={() => { onToggleStar?.(ticket.jiraKey); setMenuOpen(false); }}>
                  <Icon.Star className={`ic-sm${starred ? ` ${s.starredIcon}` : ''}`} />
                  {starred ? 'Desfavoritar' : 'Favoritar'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
      <div className={s.detailSubhead}>
        {[
          { id: 'conv', label: 'Interações Vendor', badge: ticket.activity?.filter(a => a.kind.includes('email') || a.kind === 'comment').length },
          { id: 'jira', label: 'Visualizar Jira' },
        ].map(t => (
          <button key={t.id}
            className={`${s.dtab}${detailTab === t.id ? ` ${s.active}` : ''}`}
            onClick={() => setDetailTab(t.id)}>
            {t.label}
            {t.badge ? <span className={s.badge}>{t.badge}</span> : null}
          </button>
        ))}
      </div>
    </div>
  );
}

function ActivityItem({ item }) {
  if (item.kind === 'internal-email') {
    return (
      <div className={`${s.activityItem} ${s.internal}`}>
        <div className={s.avaWrap}>
          <div className="avatar" style={{ '--avatar-color': '#2684ff' }}>{item.from?.split(' ').map(w => w[0]).join('').slice(0, 2)}</div>
        </div>
        <div className={s.activityBody}>
          <div className={s.activityHead}>
            <span className={s.who}>{item.from}</span>
            <span className={s.what}>enviou para <strong>{item.to}</strong></span>
            <span className={s.when}>{item.time}</span>
          </div>
          <div className={`${s.commentCard} ${s.outbound}`} dangerouslySetInnerHTML={{ __html: item.body }} />
        </div>
      </div>
    );
  }
  if (item.kind === 'vendor-email') {
    const vendorColor = item.vendor?.color ?? '#888';
    const vendorName = item.vendor?.name ?? 'Vendor';
    return (
      <div className={s.activityItem}>
        <div className={s.avaWrap}>
          <div className="avatar" style={{ '--avatar-color': vendorColor }}>{item.from?.split(' ').map(w => w[0]).join('').slice(0, 2)}</div>
        </div>
        <div className={s.activityBody}>
          <div className={s.activityHead}>
            <span className={s.who}>{item.from}</span>
            {item.company && <span className={s.what}>de <span className={s.vendorNameHighlight} style={{ '--vendor-color': vendorColor }}>{item.company}</span></span>}
            {!item.company && vendorName !== 'Vendor' && <span className={s.what}>de <span className={s.vendorNameHighlight} style={{ '--vendor-color': vendorColor }}>{vendorName}</span></span>}
            <span className={s.when}>{item.time}</span>
          </div>
          <div className={`${s.commentCard} ${s.vendor}`} dangerouslySetInnerHTML={{ __html: item.body }} />
        </div>
      </div>
    );
  }
  if (item.kind === 'comment') {
    return (
      <div className={s.activityItem}>
        <div className={s.avaWrap}>
          <div className="avatar" style={{ overflow: 'hidden' }}>
            {item.whoAvatarUrl
              ? <img src={item.whoAvatarUrl} alt={item.who} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
              : item.who?.split(' ').map(w => w[0]).join('').slice(0, 2)}
          </div>
        </div>
        <div className={s.activityBody}>
          <div className={s.activityHead}>
            <span className={s.who}>{item.who}</span>
            <span className={s.what}>comentou</span>
            <span className={s.when}>{item.time}</span>
          </div>
          <div className={s.commentCard} dangerouslySetInnerHTML={{ __html: item.body }} />
          {item.attachmentIds?.length > 0 && (
            <div className={s.attachmentList}>
              {item.attachmentIds.map(id => (
                <AuthImage key={id} src={attachmentContentUrl(id)}
                  className={s.attachmentThumb} />
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }
  if (item.kind === 'change') {
    return (
      <div className={s.activityItem}>
        <div className={s.avaWrap}>
          <div className={s.sysAva}><Icon.Activity className="ic-sm" /></div>
        </div>
        <div className={s.activityBody}>
          <div className={s.activityHead}>
            <span className={`${s.who} ${s.activityWho}`}>{item.who}</span>
            <span className={s.what}>
              {item.what}
              {item.diff?.from && <span className={`${s.pillOld} ${s.pillMargin}`}>{item.diff.from}</span>}
              <span className={s.diffTo}>{item.diff?.to}</span>
            </span>
            <span className={s.when}>{item.time}</span>
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className={s.activityItem}>
      <div className={s.avaWrap}>
        <div className={`${s.sysAva} ${s.jira}`}>
          <Icon.JiraLogo className={s.jiraLogoMedium} />
        </div>
      </div>
      <div className={s.activityBody}>
        <div className={s.activityHead}>
          <span className={`${s.who} ${s.activityWho}`}>{item.who}</span>
          <span className={s.what}>{item.what} <span className={`${s.ticketKey} ${s.ticketKeyGreen}`}>{item.diff?.to}</span></span>
          <span className={s.when}>{item.time}</span>
        </div>
      </div>
    </div>
  );
}

const MAX_FILE_SIZE = 25 * 1024 * 1024;
const MAX_TOTAL_SIZE = 50 * 1024 * 1024;

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function Composer({ ticket, onSend, sending, sendSuccess }) {
  const [files, setFiles] = useState([]);
  const [fileError, setFileError] = useState(null);
  const [replicateToJira, setReplicateToJira] = useState(true);
  const [showMentions, setShowMentions] = useState(false);
  const [mentionFilter, setMentionFilter] = useState('');
  const [draftRestored, setDraftRestored] = useState(false);
  const fileRef = useRef(null);
  const editorRef = useRef(null);


  const draftKey = ticket?.jiraKey ? `evim:draft:${ticket.jiraKey}` : null;

  useEffect(() => {
    if (!draftKey || !editorRef.current) return;
    try {
      const saved = localStorage.getItem(draftKey);
      if (saved) {
        editorRef.current.innerHTML = saved;
        setDraftRestored(true);
      } else {
        editorRef.current.innerHTML = '';
        setDraftRestored(false);
      }
    } catch {}
  }, [draftKey]);

  useEffect(() => {
    if (!draftKey) return;
    const el = editorRef.current;
    if (!el) return;
    let timer = null;
    const save = () => {
      const html = el.innerHTML || '';
      try {
        if (html && el.textContent.trim()) localStorage.setItem(draftKey, html);
        else localStorage.removeItem(draftKey);
      } catch {}
    };
    const onInput = () => {
      clearTimeout(timer);
      timer = setTimeout(save, 400);
    };
    el.addEventListener('input', onInput);
    return () => {
      clearTimeout(timer);
      el.removeEventListener('input', onInput);
    };
  }, [draftKey]);

  const clearDraft = () => {
    if (!draftKey) return;
    try { localStorage.removeItem(draftKey); } catch {}
    if (editorRef.current) editorRef.current.innerHTML = '';
    setDraftRestored(false);
  };

  const totalSize = files.reduce((sum, f) => sum + f.size, 0);
  const contacts = ticket.emailContacts || [];

  const addFiles = (newFiles) => {
    setFileError(null);
    const incoming = Array.from(newFiles);
    const combined = [...files, ...incoming];

    const tooBig = incoming.find(f => f.size > MAX_FILE_SIZE);
    if (tooBig) {
      setFileError(`"${tooBig.name}" excede o limite de 25 MB`);
      return;
    }
    const newTotal = combined.reduce((sum, f) => sum + f.size, 0);
    if (newTotal > MAX_TOTAL_SIZE) {
      setFileError(`Total de anexos excede 50 MB`);
      return;
    }
    setFiles(combined);
  };

  const execCmd = (cmd, value = null) => {
    editorRef.current?.focus();
    document.execCommand(cmd, false, value);
  };

  const toggleCode = () => {
    const sel = window.getSelection();
    if (!sel.rangeCount) return;
    editorRef.current?.focus();
    const range = sel.getRangeAt(0);
    const parentCode = sel.anchorNode?.parentElement?.closest('code');
    if (parentCode && editorRef.current.contains(parentCode)) {
      const text = document.createTextNode(parentCode.textContent);
      parentCode.parentNode.replaceChild(text, parentCode);
      const newRange = document.createRange();
      newRange.selectNodeContents(text);
      sel.removeAllRanges();
      sel.addRange(newRange);
    } else if (!range.collapsed) {
      const code = document.createElement('code');
      range.surroundContents(code);
      sel.removeAllRanges();
      const newRange = document.createRange();
      newRange.selectNodeContents(code);
      sel.addRange(newRange);
    }
  };

  const insertMention = (contact) => {
    editorRef.current?.focus();
    document.execCommand('insertText', false, `@${contact} `);
    setShowMentions(false);
    setMentionFilter('');
  };

  const filteredContacts = contacts.filter(c =>
    c.toLowerCase().includes(mentionFilter.toLowerCase())
  );

  const getEditorHtml = () => editorRef.current?.innerHTML || '';
  const getEditorText = () => editorRef.current?.textContent || '';

  const send = () => {
    if (!getEditorText().trim() || sending) return;
    onSend('vendor', getEditorHtml(), { replicateToJira, files });
    if (editorRef.current) editorRef.current.innerHTML = '';
    if (draftKey) { try { localStorage.removeItem(draftKey); } catch {} }
    setDraftRestored(false);
  };
  useEffect(() => {
    if (sendSuccess) {
      setFiles([]); setFileError(null);
      if (draftKey) { try { localStorage.removeItem(draftKey); } catch {} }
      setDraftRestored(false);
    }
  }, [sendSuccess, draftKey]);
  const removeFile = (idx) => { setFiles(prev => prev.filter((_, i) => i !== idx)); setFileError(null); };

  return (
    <div className={s.composer}>
      <div className={s.composerInner}>
        <div className={s.composerTabs}>
          <button className={`${s.composerTab} ${s.active}`}>
            <Icon.Mail className="ic-sm" /> Responder E-mail
          </button>
          <div className={s.composerToggle}>
            <label className={s.composerToggleLabel}>
              <input type="checkbox" checked={replicateToJira} onChange={e => setReplicateToJira(e.target.checked)} className={s.checkboxInput} />
              Replicar para Jira
            </label>
          </div>
        </div>
        <div className={s.composerBody}>
          {draftRestored && (
            <div className={s.draftBanner} role="status">
              <span>Rascunho restaurado.</span>
              <button type="button" className={s.draftBannerBtn} onClick={clearDraft}>
                Descartar
              </button>
            </div>
          )}
          <div
            ref={editorRef}
            className={s.editorArea}
            contentEditable
            data-placeholder={`Responder para ${ticket.vendor?.name ?? 'Vendor'}…`}
            suppressContentEditableWarning
          />
        </div>
        {files.length > 0 && (
          <div className={s.attachmentBar}>
            {files.map((f, i) => (
              <span key={i} className={s.attachmentChip}>
                <Icon.Attachment className="ic-sm" />
                <span className={s.attachmentName}>{f.name}</span>
                <span className={s.attachmentSize}>{formatSize(f.size)}</span>
                <button className={s.attachmentRemove} onClick={() => removeFile(i)} aria-label="Remover">&times;</button>
              </span>
            ))}
            <span className={s.attachmentMeta}>{files.length} {files.length === 1 ? 'arquivo' : 'arquivos'} · {formatSize(totalSize)}/50 MB</span>
          </div>
        )}
        {fileError && <div className={s.fileError}>{fileError}</div>}
        {showMentions && (
          <div className={s.mentionDropdown}>
            <input
              className={s.mentionSearch}
              placeholder="Buscar contato…"
              value={mentionFilter}
              onChange={e => setMentionFilter(e.target.value)}
              autoFocus
            />
            <div className={s.mentionList}>
              {filteredContacts.length === 0 && <div className={s.mentionEmpty}>Nenhum contato encontrado</div>}
              {filteredContacts.map(c => (
                <button key={c} className={s.mentionItem} onClick={() => insertMention(c)}>
                  <span className="avatar sm">{c.charAt(0).toUpperCase()}</span>
                  {c}
                </button>
              ))}
            </div>
          </div>
        )}
        <input type="file" multiple ref={fileRef} className={s.hiddenInput}
          onChange={e => { addFiles(e.target.files); e.target.value = ''; }} />
        <div className={s.composerFoot}>
          <div className={s.tools}>
            <button className={s.tool} title="Negrito" onMouseDown={e => { e.preventDefault(); execCmd('bold'); }}><Icon.Bold className="ic-sm" /></button>
            <button className={s.tool} title="Código" onMouseDown={e => { e.preventDefault(); toggleCode(); }}><Icon.Code className="ic-sm" /></button>
            <button className={s.tool} title="Mencionar" onClick={() => setShowMentions(!showMentions)}><Icon.At className="ic-sm" /></button>
            <button className={s.tool} title="Anexar" onClick={() => fileRef.current?.click()}><Icon.Attachment className="ic-sm" /></button>
          </div>
          <div className={s.send}>
            <div className={s.splitSend}>
              <button className="btn primary" onClick={send} disabled={sending}>
                {sending
                  ? <span className={s.spinner} />
                  : <Icon.Send className="ic-sm" />}
                {sending ? 'Enviando…' : 'Enviar e-mail'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function DetailAside({ ticket, showAi }) {
  const [lightbox, setLightbox] = useState(null);
  return (
    <div className={s.detailAside}>
      <div className={s.metaGroup}>
        <h4>Fornecedor · <span className={s.vendorNameHighlightMeta} style={{ '--vendor-color': ticket.vendor?.color ?? '#888' }}>{ticket.vendor?.name ?? '—'}</span></h4>
        <div className={s.metaRow}>
          <span className={s.k}>Tipo</span>
          <span className={`${s.v} ${s.metaValueMuted}`}>{ticket.vendor?.type ?? '—'}</span>
        </div>
        <div className={s.metaRow}>
          <span className={s.k}>E-mail</span>
          <span className={s.v} style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>{ticket.from}</span>
        </div>
        <div className={s.metaRow}>
          <span className={s.k}>SLA</span>
          <span className={s.v}>
            <span className={'ticket-sla-chip ' + (ticket.sla?.state ?? 'green')} style={{ fontSize: 11 }}>{ticket.sla?.label ?? '—'}</span>
          </span>
        </div>
      </div>

      <div className={s.metaGroup}>
        <h4>
          <Icon.JiraLogo className={s.jiraLogoMedium} style={{ color: '#2684ff' }} />
          {ticket.jiraKey}
        </h4>
        <div className={s.metaRow}>
          <span className={s.k}>Responsável</span>
          <span className={`${s.v} ${s.editable}`}>
            {ticket.assignee ? (
              <span className={s.assigneeChip}>
                <span className="avatar sm" style={{ background: ticket.assignee.color, overflow: 'hidden' }}>
                  {ticket.assignee.avatarUrl
                    ? <img src={ticket.assignee.avatarUrl} alt={ticket.assignee.initials} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
                    : ticket.assignee.initials}
                </span>
                {ticket.assignee.name}
              </span>
            ) : <span style={{ color: 'var(--fg-4)' }}>Não atribuído</span>}
          </span>
        </div>
        <div className={s.metaRow}>
          <span className={s.k}>Prioridade</span>
          <span className={`${s.v} ${s.editable}`}><PriorityIcon p={ticket.priority} /></span>
        </div>
      </div>

      <div className={s.metaGroup}>
        <h4>Relator</h4>
        <div>
          {ticket.reporter ? (
            <span className={s.assigneeChip}>
              <span className="avatar sm" style={{ overflow: 'hidden' }}>
                {ticket.reporterAvatarUrl
                  ? <img src={ticket.reporterAvatarUrl} alt={ticket.reporter} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} loading="lazy" />
                  : ticket.reporter.charAt(0).toUpperCase()}
              </span>
              {ticket.reporter}
            </span>
          ) : (
            <span style={{ fontSize: 12.5, color: 'var(--fg-4)' }}>{ticket.from || '—'}</span>
          )}
        </div>
      </div>

      {ticket.description && (
        <div className={s.metaGroup}>
          <h4>Descrição</h4>
          <div
            className={s.descriptionBody}
            dangerouslySetInnerHTML={{ __html: ticket.description }}
          />
          {ticket.descriptionAttachmentIds?.length > 0 && (
            <div className={s.descriptionAttachments}>
              {ticket.descriptionAttachmentIds.map(id => (
                <AuthImage key={id} src={attachmentContentUrl(id)}
                  className={s.descriptionThumb}
                  onClick={() => setLightbox(attachmentContentUrl(id))}
                  onMouseEnter={e => { e.target.style.opacity = '0.8'; e.target.style.boxShadow = 'var(--sh-sm)'; }}
                  onMouseLeave={e => { e.target.style.opacity = '1'; e.target.style.boxShadow = 'none'; }} />
              ))}
            </div>
          )}
        </div>
      )}

      {ticket.attachments?.filter(a => a.mimeType?.startsWith('image/') || /\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i.test(a.filename)).length > 0 && (
        <div className={s.metaGroup}>
          <h4>Imagens</h4>
          <div className={s.descriptionAttachments}>
            {ticket.attachments
              .filter(a => a.mimeType?.startsWith('image/') || /\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i.test(a.filename))
              .map(att => (
                <AuthImage key={att.id} src={attachmentContentUrl(att.id)}
                  className={s.descriptionThumb}
                  title={att.filename}
                  onClick={() => setLightbox(attachmentContentUrl(att.id))}
                  onMouseEnter={e => { e.target.style.opacity = '0.8'; e.target.style.boxShadow = 'var(--sh-sm)'; }}
                  onMouseLeave={e => { e.target.style.opacity = '1'; e.target.style.boxShadow = 'none'; }} />
              ))}
          </div>
        </div>
      )}
      {lightbox && (
        <div className={s.lightboxOverlay} onClick={() => setLightbox(null)}>
          <AuthImage src={lightbox} alt="preview" className={s.lightboxImg} onClick={e => e.stopPropagation()} />
        </div>
      )}
    </div>
  );
}

function Detail({ ticket, onStatusChange, onSend, sending, sendSuccess, jiraBaseUrl, jiraStatuses, defaultTab, onTabSet, onBack, starred, onToggleStar }) {
  const [detailTab, setDetailTab] = useState(defaultTab ?? 'conv');

  useEffect(() => {
    if (defaultTab) {
      setDetailTab(defaultTab);
      onTabSet?.();
    }
  }, [defaultTab]);

  return (
    <div className={s.detail}>
      <DetailHeader ticket={ticket} detailTab={detailTab} setDetailTab={setDetailTab} jiraBaseUrl={jiraBaseUrl} onBack={onBack} starred={starred} onToggleStar={onToggleStar} />
      {detailTab === 'jira' ? (
        <JiraPanel jiraId={ticket.jiraKey} onClose={() => setDetailTab('conv')} jiraBaseUrl={jiraBaseUrl} />
      ) : (
        <div className={s.detailBody}>
          {ticket.threadCount === 0 ? (
            <div className={s.emptyActivity}>
              <Icon.Mail className={s.emptyActivityIcon} />
              <div className={s.emptyActivityText}>
                Nenhuma interação registrada para <strong>{ticket.from}</strong>.
                Confirme se a caixa compartilhada está em cópia no ticket e o campo no jira está mapeado corretamente. Assim que um e-mail for registrado, ele aparecerá aqui e você poderá responder diretamente por este painel.
              </div>
            </div>
          ) : (
            <div className={s.vendorBanner}>
              <span className={s.vtag}>VENDOR</span>
              <span>Conversa com <strong>{ticket.from}</strong></span>
              <span className={s.sep}>·</span>
              <span style={{ color: 'var(--fg-3)' }}>{ticket.threadCount} e-mails</span>
            </div>
          )}

          <div className={s.blockLabel}>Atividade</div>
          <div className={s.activity}>
            {(ticket.activity || []).filter(a => a.kind === 'vendor-email' || a.kind === 'internal-email').map((a, i) => <ActivityItem key={i} item={a} />)}
          </div>

          <Composer ticket={ticket} onSend={onSend} sending={sending} sendSuccess={sendSuccess} />
        </div>
      )}
    </div>
  );
}

export { Detail, DetailAside };
export default Detail;
