import { apiFetch } from './client';
import type { Transfer, TransferStatus } from '$lib/types';

export const getAll = (token: string) => apiFetch<Transfer[]>('/transfers', {}, token);
export const getByOrg = (orgId: number, token: string) => apiFetch<Transfer[]>(`/transfers/organization/${orgId}`, {}, token);
export const createTransfer = (data: object, token: string) =>
  apiFetch<Transfer>('/transfers', { method: 'POST', body: JSON.stringify(data) }, token);
export const updateStatus = (id: number, status: TransferStatus, token: string) =>
  apiFetch<Transfer>(`/transfers/${id}/status?status=${status}`, { method: 'PATCH' }, token);
export const deleteTransfer = (id: number, token: string) =>
  apiFetch<void>(`/transfers/${id}`, { method: 'DELETE' }, token);
