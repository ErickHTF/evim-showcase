import { useState, useEffect } from 'react';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { searchCards, getLastInteraction } from '../services/api';

const mapIssueToConversation = (issue) => ({
  id:              issue.key,
  ticketId:        issue.vendorTicket,
  subject:         issue.summary,
  status:          issue.status,
  priority:        issue.priorityName,
  priorityId:      issue.priorityId,
  priorityIconUrl: issue.priorityIconUrl,
  assignee:        issue.assignee,
  assigneeAvatarUrl: issue.assigneeAvatarUrl || null,
  jiraId:          issue.key,
  lastUpdate:      null,
});

function buildCardParams(filters, pageToken) {
  const params = new URLSearchParams();
  if (filters.ticketId)   params.append('conversationId', filters.ticketId);
  if (filters.assignee === 'unassigned') {
    params.append('unassigned', 'true');
  } else if (filters.assignee !== 'all') {
    params.append('accountId', filters.assignee);
  }
  if (filters.sprintId != null) params.append('sprintId', filters.sprintId);
  if (filters.text) params.append('text', filters.text);
  if (filters.vendorOnly === false) params.append('vendorOnly', 'false');
  if (filters.statuses?.length) params.append('status', filters.statuses.join(','));
  if (pageToken) params.append('nextPageToken', pageToken);
  params.append('size', filters.size ?? 20);
  return params.toString().replace(/\+/g, '%20');
}

export function useConversations(filters) {
  const [pageToken, setPageToken]     = useState(null);
  const [tokenStack, setTokenStack]   = useState([]);
  const [enriched, setEnriched]       = useState({});

  useEffect(() => {
    setPageToken(null);
    setTokenStack([]);
    setEnriched({});
  }, [filters.ticketId, filters.assignee, filters.sprintId, filters.size, filters.text, filters.vendorOnly, filters.statuses]);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['conversations', filters, pageToken],
    queryFn:  ({ signal }) => searchCards(buildCardParams(filters, pageToken), signal),
    placeholderData: keepPreviousData,
    staleTime: 15_000,
  });

  const rawConversations = (data?.content ?? []).map(mapIssueToConversation);
  const nextToken = data?.nextPageToken ?? null;

  const ticketIdKey = rawConversations.map(c => c.ticketId).join(',');
  useEffect(() => {
    if (!ticketIdKey) return;
    rawConversations.forEach(conv => {
      if (!conv.ticketId) return;
      getLastInteraction(conv.ticketId).then(d => {
        if (!d) return;
        setEnriched(prev => ({
          ...prev,
          [conv.ticketId]: { isVendor: d.vendor, lastInteraction: d.received },
        }));
      });
    });
  }, [ticketIdKey]);

  const conversations = rawConversations.map(c =>
    enriched[c.ticketId] ? { ...c, ...enriched[c.ticketId] } : c
  );

  function goNext() {
    if (!nextToken) return;
    setTokenStack(prev => [...prev, nextToken]);
    setPageToken(nextToken);
  }

  function goPrev() {
    if (tokenStack.length === 0) return;
    const stack = [...tokenStack];
    stack.pop();
    setTokenStack(stack);
    setPageToken(stack.length > 0 ? stack[stack.length - 1] : null);
  }

  return {
    conversations,
    currentPage: tokenStack.length + 1,
    hasPrev: tokenStack.length > 0,
    hasNext: !!nextToken,
    isLoading,
    isFetching,
    goNext,
    goPrev,
  };
}
