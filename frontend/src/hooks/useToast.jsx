import { createContext, useContext, useState, useCallback, useRef, useEffect } from 'react';

const ToastContext = createContext(null);

let nextId = 1;
const DEFAULT_DURATION = 3000;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timers = useRef(new Map());

  const dismiss = useCallback((id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
    const tm = timers.current.get(id);
    if (tm) { clearTimeout(tm); timers.current.delete(id); }
  }, []);

  const showToast = useCallback((message, options = {}) => {
    const id = nextId++;
    const type = options.type ?? 'info';
    const duration = options.duration ?? DEFAULT_DURATION;
    const toast = { id, message, type, action: options.action ?? null };
    setToasts(prev => [...prev, toast]);
    if (duration > 0) {
      const tm = setTimeout(() => dismiss(id), duration);
      timers.current.set(id, tm);
    }
    return id;
  }, [dismiss]);

  useEffect(() => () => { timers.current.forEach(clearTimeout); }, []);

  return (
    <ToastContext.Provider value={{ showToast, dismiss }}>
      {children}
      <div className="toast-stack" role="region" aria-live="polite" aria-label="Notificações">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast--${t.type}`} role="status">
            <span className="toast-icon" aria-hidden="true" />
            <span className="toast-msg">{t.message}</span>
            {t.action && (
              <button
                className="toast-action"
                onClick={() => { t.action.onClick(); dismiss(t.id); }}>
                {t.action.label}
              </button>
            )}
            <button
              className="toast-close"
              aria-label="Fechar"
              onClick={() => dismiss(t.id)}>×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}
