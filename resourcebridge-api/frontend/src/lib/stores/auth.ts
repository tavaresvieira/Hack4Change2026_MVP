import { writable, derived } from 'svelte/store';
import type { Role } from '$lib/types';

interface AuthState {
  id: number | null;
  token: string | null;
  refreshToken: string | null;
  name: string | null;
  email: string | null;
  role: Role | null;
  organizationId: number | null;
  organizationName: string | null;
}

const defaultState: AuthState = {
  id: null, token: null, refreshToken: null, name: null, email: null, role: null, organizationId: null, organizationName: null
};

function createAuthStore() {
  const { subscribe, set, update } = writable<AuthState>(defaultState);

  return {
    subscribe,
    init: () => {
      if (typeof localStorage === 'undefined') return;
      const stored = localStorage.getItem('auth');
      if (stored) {
        try { set(JSON.parse(stored)); } catch { set(defaultState); }
      }
    },
    setAuth: (data: { id: number; token: string; refreshToken: string; name: string; email: string; role: Role; organizationId: number; organizationName: string | null }) => {
      set(data);
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem('auth', JSON.stringify(data));
      }
    },
    clear: () => {
      set(defaultState);
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('auth');
      }
    }
  };
}

export const auth = createAuthStore();
export const isLoggedIn = derived(auth, $a => !!$a.token);
export const isCoordinator = derived(auth, $a => $a.role === 'COORDINATOR');
export const isStaff = derived(auth, $a => $a.role === 'STAFF');
export const isAdmin = derived(auth, $a => $a.role === 'ADMIN');
