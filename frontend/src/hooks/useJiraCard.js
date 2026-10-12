import { useQuery } from '@tanstack/react-query';
import { getCard, getCardTransitions } from '../services/api';

export function useJiraCard(jiraId) {
  const { data: card = null, isLoading: loading, error } = useQuery({
    queryKey: ['card', jiraId],
    queryFn: ({ signal }) => getCard(jiraId, signal),
    enabled: !!jiraId,
  });

  const { data: transitions = [] } = useQuery({
    queryKey: ['transitions', jiraId],
    queryFn: ({ signal }) => getCardTransitions(jiraId, signal),
    enabled: !!jiraId,
  });

  return {
    card,
    transitions,
    loading,
    error: error ? 'Erro ao carregar card.' : null,
  };
}
