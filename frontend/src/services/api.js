const BASE_URL = import.meta.env.VITE_API_BASE_URL;

let _authToken = null;
let _onAtlassianReauth = null;

export function setAuthToken(token) {
  _authToken = token;
}

export function getAuthToken() {
  return _authToken;
}

export function onAtlassianReauth(callback) {
  _onAtlassianReauth = callback;
}

function authHeaders(extra = {}) {
  return _authToken
    ? { Authorization: `Bearer ${_authToken}`, ...extra }
    : { ...extra };
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: { ...authHeaders(), ...options.headers },
  });

  if (res.headers.get('X-Atlassian-Reauth') === 'required') {
    _onAtlassianReauth?.();
    return null;
  }

  if (!res.ok) throw new Error(`${options.method ?? 'GET'} ${path} — ${res.status}`);
  if (res.status === 204) return null;
  return res.json();
}

function json(method, path, body) {
  return request(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export const getPublicConfig   = (signal) => request('/admin/config/public', { signal });
export const getAdminConfigs   = (signal) => request('/admin/config', { signal });
export const updateAdminConfig = (key, data) => json('PUT', `/admin/config/${key}`, data);

export const getSprints         = (signal) => request('/card/sprints', { signal });
export const searchCards        = (query, signal) => request(`/card/search?${query}`, { signal });
export const getAssignableUsers = (signal) => request('/card/users/assignable/default', { signal });
export const getStatuses        = (signal) => request('/card/statuses', { signal });
export const getPriorities      = (signal) => request('/card/priorities', { signal });
export const getFilterCounts    = ({ accountId, sprintId } = {}, signal) => {
  const params = new URLSearchParams();
  if (accountId) params.append('accountId', accountId);
  if (sprintId != null) params.append('sprintId', sprintId);
  const qs = params.toString();
  return request(`/card/counts${qs ? `?${qs}` : ''}`, { signal })
    .catch(() => ({ all: 0, mine: null, unassigned: 0, noVendor: 0 }));
};
export const getCard            = (key, signal) => request(`/card/${key}`, { signal });
export const getCardTransitions = (key, signal) =>
  request(`/card/${key}/transitions`, { signal }).then(d => d ?? []);

export const addCardComment = (key, adf) => json('POST', `/card/${key}/comment`, adf);

export const getConversations   = (ticketId, signal) => request(`/conversations?ticketId=${ticketId}&size=1`, { signal });
export const getLastInteraction = (ticketId) =>
  request(`/conversations/ticket/${ticketId}/last-interaction`).catch(() => null);

export const getEmailsByTicket = (ticketId, signal) => request(`/email/by-ticket/${ticketId}`, { signal });
export const getThreadMessagesByTicket = (ticketId, signal) => request(`/email/by-ticket/${ticketId}/thread-messages`, { signal });

export const patchCard = (key, dto) => json('PATCH', `/card/${key}`, dto);

export const attachmentContentUrl = (attachmentId) => `${BASE_URL}/card/attachment/${attachmentId}/content`;

export function sendEmail({ files, ...dto }) {
  const form = new FormData();
  form.append('data', new Blob([JSON.stringify(dto)], { type: 'application/json' }));
  if (files?.length) {
    files.forEach(f => form.append('files', f));
  }
  return request('/email/send', { method: 'POST', body: form });
}

export const getAtlassianStatus    = (signal) => request('/auth/atlassian/status', { signal });
export const initiateAtlassianOAuth = () => request('/auth/atlassian/initiate');

export const getMetrics = (sprintId, signal) =>
  request(`/metrics${sprintId != null ? '?sprintId=' + sprintId : ''}`, { signal });

export const getStarredKeys = (signal) => request('/card/starred/keys', { signal });
export const starTicket     = (key) => request(`/card/${key}/star`, { method: 'PUT' });
export const unstarTicket   = (key) => request(`/card/${key}/star`, { method: 'DELETE' });
