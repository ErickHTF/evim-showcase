import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from 'react-oidc-context'
import { oidcConfig } from './auth/oidcConfig'
import './index.css'
import App from './App.jsx'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

function onSigninCallback() {
  window.history.replaceState({}, document.title, '/');
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AuthProvider {...oidcConfig} onSigninCallback={onSigninCallback}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
)