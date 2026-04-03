import { apiFetch } from './client';

export const getCommunityAnnouncements = () => apiFetch<any[]>('/announcements');

export const createExchangeRequest = (announcementId: number, requestingOrgId: number, token: string) =>
  apiFetch<any>('/exchange-requests', { method: 'POST', body: JSON.stringify({ announcementId, requestingOrgId }) }, token);

export const getIncomingRequests = (orgId: number, token: string) =>
  apiFetch<any[]>(`/exchange-requests/incoming/${orgId}`, {}, token);

export const getOutgoingRequests = (orgId: number, token: string) =>
  apiFetch<any[]>(`/exchange-requests/outgoing/${orgId}`, {}, token);

export const updateRequestStatus = (id: number, status: 'ACCEPTED' | 'REJECTED', token: string) =>
  apiFetch<any>(`/exchange-requests/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }, token);
