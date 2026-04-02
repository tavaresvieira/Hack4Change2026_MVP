import { apiFetch } from './client';
import type { Inventory } from '$lib/types';

export const getByOrg = (orgId: number, token: string) => apiFetch<Inventory[]>(`/inventory/organization/${orgId}`, {}, token);
export const getExpiring = (orgId: number, days: number, token: string) =>
  apiFetch<Inventory[]>(`/inventory/expiring/organization/${orgId}?days=${days}`, {}, token);
export const createInventory = (data: object, token: string) =>
  apiFetch<Inventory>('/inventory', { method: 'POST', body: JSON.stringify(data) }, token);
export const updateInventory = (id: number, data: object, token: string) =>
  apiFetch<Inventory>(`/inventory/${id}`, { method: 'PUT', body: JSON.stringify(data) }, token);
export const deleteInventory = (id: number, token: string) =>
  apiFetch<void>(`/inventory/${id}`, { method: 'DELETE' }, token);
