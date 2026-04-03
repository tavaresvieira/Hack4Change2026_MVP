import { apiFetch } from './client';
import type { Organization } from '$lib/types';

export interface AdminStats {
  totalDonations: number;
  donationsByStatus: Record<string, number>;
  totalNeeds: number;
  openNeeds: number;
  fulfilledNeeds: number;
  openNeedsByUrgency: Record<string, number>;
  totalTransfers: number;
  transfersByStatus: Record<string, number>;
  totalOrganizations: number;
  totalStaff: number;
  topUrgentNeeds: {
    itemName: string;
    organizationName: string;
    quantityNeeded: number;
    urgency: string;
  }[];
  expiringItems: {
    itemName: string;
    unit: string;
    organizationName: string;
    quantity: number;
    expiryDate: string;
    daysUntilExpiry: number;
  }[];
}

export interface StaffMember {
  id: number;
  name: string;
  email: string;
  role: string;
  organization: { id: number; name: string } | null;
  deletedAt: string | null;
}

export const getStats = (token: string) =>
  apiFetch<AdminStats>('/admin/stats', {}, token);

export const getAllStaff = (token: string) =>
  apiFetch<StaffMember[]>('/admin/staff', {}, token);

export const deactivateStaff = (id: number, token: string) =>
  apiFetch<void>(`/admin/staff/${id}/deactivate`, { method: 'PATCH' }, token);

export const restoreStaff = (id: number, token: string) =>
  apiFetch<void>(`/admin/staff/${id}/restore`, { method: 'PATCH' }, token);

export const restoreOrganization = (id: number, token: string) =>
  apiFetch<void>(`/admin/organizations/${id}/restore`, { method: 'PATCH' }, token);

export const archiveOrganization = (id: number, token: string) =>
  apiFetch<void>(`/organizations/${id}`, { method: 'DELETE' }, token);

export const updateOrganization = (id: number, data: Partial<Organization>, token: string) =>
  apiFetch<Organization>(`/organizations/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }, token);
