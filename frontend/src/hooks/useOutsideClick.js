import { useEffect } from 'react';

export function useOutsideClick(ref, isOpen, onClose) {
  useEffect(() => {
    if (!isOpen) return;
    function handle(e) {
      if (ref.current && !ref.current.contains(e.target)) onClose();
    }
    document.addEventListener('mousedown', handle);
    return () => document.removeEventListener('mousedown', handle);
  }, [isOpen, onClose, ref]);
}
