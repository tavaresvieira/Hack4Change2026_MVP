import { apiFetch } from './client';
import type { Need } from '$lib/types';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const getUnfulfilled = () => apiFetch<Need[]>('/needs');

export const getUnfulfilledPaged = (page = 0, size = 12) =>
  apiFetch<PageResponse<Need>>(`/needs/page?page=${page}&size=${size}`);
export const getAll = (token: string) => apiFetch<Need[]>('/needs/all', {}, token);
export const getByOrg = (orgId: number, token: string) => apiFetch<Need[]>(`/needs/organization/${orgId}`, {}, token);
export const getByUrgency = (urgency: string, token: string) => apiFetch<Need[]>(`/needs/urgency/${urgency}`, {}, token);
export const createNeed = (data: object, token: string) =>
  apiFetch<Need>('/needs', { method: 'POST', body: JSON.stringify(data) }, token);
export const updateNeed = (id: number, data: object, token: string) =>
  apiFetch<Need>(`/needs/${id}`, { method: 'PUT', body: JSON.stringify(data) }, token);
export const fulfillNeed = (id: number, token: string) =>
  apiFetch<Need>(`/needs/${id}/fulfill`, { method: 'PATCH' }, token);
