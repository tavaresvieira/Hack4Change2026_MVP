import { get } from 'svelte/store';
import { auth } from '$lib/stores/auth';

const BASE = '/api';

let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

async function attemptRefresh(): Promise<string | null> {
  const { refreshToken } = get(auth);
  if (!refreshToken) return null;

  const res = await fetch(`${BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    auth.clear();
    return null;
  }

  const data = await res.json();
  auth.setAuth(data);
  return data.token;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, { ...options, headers });

  if (res.status === 401 && token) {
    // Try to refresh once
    if (!isRefreshing) {
      isRefreshing = true;
      refreshPromise = attemptRefresh().finally(() => {
        isRefreshing = false;
        refreshPromise = null;
      });
    }

    const newToken = await refreshPromise;
    if (!newToken) {
      throw new Error('Session expired — please log in again');
    }

    // Retry with new token
    const retryHeaders = { ...headers, Authorization: `Bearer ${newToken}` };
    const retryRes = await fetch(`${BASE}${path}`, { ...options, headers: retryHeaders });
    if (!retryRes.ok) {
      const msg = await retryRes.text().catch(() => `HTTP ${retryRes.status}`);
      throw new Error(msg || `HTTP ${retryRes.status}`);
    }
    if (retryRes.status === 204) return undefined as T;
    return retryRes.json();
  }

  if (!res.ok) {
    const msg = await res.text().catch(() => `HTTP ${res.status}`);
    throw new Error(msg || `HTTP ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}
