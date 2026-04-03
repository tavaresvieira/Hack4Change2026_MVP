import { apiFetch } from './client';

export interface FinancialDonation {
  id: number;
  donorName: string;
  donorEmail: string;
  amount: number;
  message?: string;
  createdAt: string;
}

export interface FinancialDonationStats {
  count: number;
  total: number;
}

export const submitFinancialDonation = (body: { donorName: string; donorEmail: string; amount: number; message?: string }) =>
  apiFetch<FinancialDonation>('/financial-donations', { method: 'POST', body: JSON.stringify(body) });

export const getFinancialDonations = (token: string) =>
  apiFetch<FinancialDonation[]>('/financial-donations', {}, token);

export const getFinancialDonationStats = (token: string) =>
  apiFetch<FinancialDonationStats>('/financial-donations/stats', {}, token);
