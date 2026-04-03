import { apiFetch } from './client';
import type { Organization } from '$lib/types';

export const getAll = () => apiFetch<Organization[]>('/organizations');

export const createOrganization = (data: Omit<Organization, 'id'>, token: string) =>
  apiFetch<Organization>('/organizations', {
    method: 'POST',
    body: JSON.stringify(data),
  }, token);
