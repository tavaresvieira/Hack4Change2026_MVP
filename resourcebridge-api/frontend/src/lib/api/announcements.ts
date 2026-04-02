import { apiFetch } from './client';
import type { Announcement } from '$lib/types';

export const getLatest = () => apiFetch<Announcement[]>('/announcements');
export const getByOrg = (orgId: number, token: string) => apiFetch<Announcement[]>(`/announcements/organization/${orgId}`, {}, token);
export const createAnnouncement = (data: object, token: string) =>
  apiFetch<Announcement>('/announcements', { method: 'POST', body: JSON.stringify(data) }, token);
export const deleteAnnouncement = (id: number, token: string) =>
  apiFetch<void>(`/announcements/${id}`, { method: 'DELETE' }, token);
