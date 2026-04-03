import { apiFetch } from './client';

export interface InviteResponse {
  token: string;
  email: string;
  organizationName: string;
  inviteUrl: string | null;
}

export const createInvite = (
  email: string,
  organizationId: number,
  authToken: string,
  role: 'STAFF' | 'ADMIN' = 'STAFF'
) =>
  apiFetch<InviteResponse>('/invitations', {
    method: 'POST',
    body: JSON.stringify({ email, organizationId, role }),
  }, authToken);

export const validateInviteToken = (token: string) =>
  apiFetch<InviteResponse>(`/invitations/validate/${token}`);
