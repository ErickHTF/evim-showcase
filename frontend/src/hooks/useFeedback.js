import { useState } from 'react';

export function useFeedback(duration = 2500) {
  const [state, setState] = useState({});

  function show(key, ok) {
    setState(prev => ({ ...prev, [key]: ok ? 'ok' : 'err' }));
    setTimeout(() => setState(prev => { const s = { ...prev }; delete s[key]; return s; }), duration);
  }

  return [state, show];
}
