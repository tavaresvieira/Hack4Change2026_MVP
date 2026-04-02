import { writable } from 'svelte/store';
import { browser } from '$app/environment';

function createDarkModeStore() {
  const { subscribe, set } = writable(false);

  return {
    subscribe,
    init: () => {
      if (!browser) return;
      const dark = localStorage.getItem('darkMode') === 'true';
      set(dark);
      document.documentElement.classList.toggle('dark', dark);
    },
    toggle: () => {
      if (!browser) return;
      const dark = !document.documentElement.classList.contains('dark');
      document.documentElement.classList.toggle('dark', dark);
      localStorage.setItem('darkMode', String(dark));
      set(dark);
    },
    disable: () => {
      if (!browser) return;
      document.documentElement.classList.remove('dark');
      set(false);
    }
  };
}

export const darkMode = createDarkModeStore();
