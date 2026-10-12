import { useEffect } from 'react';
import { driver } from 'driver.js';

const TOUR_STORAGE_KEY = 'evim:tourCompleted';

const STEPS = [
  {
    popover: {
      title: 'Bem-vindo ao EVIM',
      description:
        'Vamos fazer um tour rápido pelos principais recursos. Use as setas para navegar ou ESC para sair.',
    },
  },
  {
    element: '[data-tour="rail"]',
    popover: {
      title: 'Navegação principal',
      description: 'Alterne entre Caixa de Entrada, Deploy, Métricas e Configurações.',
      side: 'right',
    },
  },
  {
    element: '[data-tour="sidebar"]',
    popover: {
      title: 'Filtros',
      description: 'Filtre tickets por status, responsável, sprint, favoritos.',
      side: 'right',
    },
  },
  {
    element: '[data-tour="sidebar-toggle"]',
    popover: {
      title: 'Minimizar filtros',
      description: 'Esconde/mostra o painel de filtros.',
      side: 'right',
    },
  },
  {
    element: '[data-tour="topbar-search"]',
    popover: {
      title: 'Busca global',
      description: 'Pesquise por tickets ou chaves Jira.',
      side: 'bottom',
    },
  },
  {
    element: '[data-tour="queue"]',
    popover: {
      title: 'Fila de tickets',
      description: 'A lista de tickets do Jira. Clique em um item para ver os detalhes.',
      side: 'right',
    },
  },
  {
    element: '[data-tour="queue-resizer"]',
    popover: {
      title: 'Redimensionar colunas',
      description: 'Arraste esta borda para ajustar a largura. Duplo clique restaura o tamanho padrão.',
      side: 'right',
    },
  },
  {
    element: '[data-tour="detail"]',
    popover: {
      title: 'Detalhes do ticket',
      description: 'Toda a conversa, anexos, histórico e ações de resposta/comentário.',
      side: 'left',
    },
  },
  {
    popover: {
      title: 'Tudo pronto!',
      description: 'Você pode rever este tour pelo menu do usuário > "Repetir tour".',
    },
  },
];

function getSteps() {
  const wide = window.innerWidth > 1500;
  return STEPS.filter(s => {
    if (!s.element) return true;
    if (s.element.includes('queue-resizer') && !wide) return false;
    if (!document.querySelector(s.element)) return false;
    return true;
  });
}

function buildDriver() {
  return driver({
    showProgress: true,
    allowClose: true,
    overlayOpacity: 0.55,
    stagePadding: 8,
    stageRadius: 8,
    smoothScroll: true,
    animate: true,
    popoverClass: 'evim-tour',
    nextBtnText: 'Próximo',
    prevBtnText: 'Voltar',
    doneBtnText: 'Concluir',
    progressText: '{{current}} de {{total}}',
    steps: getSteps(),
    onDestroyed: () => {
      try { window.localStorage.setItem(TOUR_STORAGE_KEY, '1'); } catch {}
    },
  });
}

export function startTour() {
  buildDriver().drive();
}

export function resetTour() {
  try { window.localStorage.removeItem(TOUR_STORAGE_KEY); } catch {}
  startTour();
}

export default function Tour() {
  useEffect(() => {
    const completed = window.localStorage.getItem(TOUR_STORAGE_KEY);
    if (completed) return;
    const t = setTimeout(() => startTour(), 800);
    return () => clearTimeout(t);
  }, []);

  return null;
}
