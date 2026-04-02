<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth } from '$lib/stores/auth';
  import { darkMode } from '$lib/stores/darkMode';
  import { onMount, onDestroy } from 'svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  onMount(() => {
    auth.init();
    if (!$auth.token) goto('/login');
    darkMode.init();
  });

  onDestroy(() => {
    darkMode.disable();
  });

  let confirmingLogout = $state(false);

  function logout() {
    auth.clear();
    goto('/');
  }

  let currentPath = $derived($page.url.pathname + $page.url.hash);

  const staffNav = [
    { href: '/dashboard/staff', label: 'Dashboard', icon: '📊' },
    { href: '/dashboard/staff#inventory', label: 'Inventory', icon: '📦' },
    { href: '/dashboard/staff#needs', label: 'Our Needs', icon: '📋' },
    { href: '/dashboard/staff#transfers', label: 'Incoming', icon: '🚚' },
    { href: '/dashboard/staff#announcements', label: 'Announcements', icon: '📢' },
  ];
</script>

{#if $auth.token}
<div class="min-h-screen flex bg-gray-50 dark:bg-gray-900">
  <!-- SIDEBAR -->
  <aside class="w-56 bg-white dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700 flex flex-col fixed h-full shadow-sm">
    <div class="p-4 border-b border-gray-100 dark:border-gray-700">
      <a href="/" class="flex items-center gap-2">
        <div class="w-8 h-8 bg-brand-500 rounded-lg flex items-center justify-center flex-shrink-0">
          <span class="text-white font-bold text-sm">RB</span>
        </div>
        <span class="font-bold text-gray-900 dark:text-white text-sm">ResourceBridge</span>
      </a>
    </div>

    <nav class="flex-1 p-3 space-y-1">
      {#each staffNav as item}
        <a
          href={item.href}
          class="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors
            {currentPath === item.href ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white'}">
          <span>{item.icon}</span>
          {item.label}
        </a>
      {/each}
    </nav>

    <div class="p-3 border-t border-gray-100 dark:border-gray-700">
      <div class="px-3 py-2 mb-1">
        <div class="flex items-center justify-between mb-1">
          <div class="text-xs font-semibold text-gray-900 dark:text-gray-100 truncate">{$auth.name}</div>
          <button onclick={darkMode.toggle} class="text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors" title="Toggle dark mode">
            {#if $darkMode}☀️{:else}🌙{/if}
          </button>
        </div>
        <div class="text-xs text-gray-400 truncate">{$auth.email}</div>
        <div class="inline-flex items-center mt-1 px-2 py-0.5 bg-brand-100 dark:bg-brand-900/20 text-brand-700 dark:text-brand-400 rounded-full text-xs font-medium">
          Shelter Staff
        </div>
      </div>
      {#if confirmingLogout}
        <div class="bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800 rounded-lg px-3 py-2 space-y-2">
          <p class="text-xs text-red-700 dark:text-red-300 font-medium">Are you sure you want to sign out?</p>
          <div class="flex gap-2">
            <button onclick={logout} class="flex-1 bg-red-500 text-white text-xs py-1.5 rounded-md font-medium hover:bg-red-600 transition-colors">Yes, sign out</button>
            <button onclick={() => confirmingLogout = false} class="flex-1 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs py-1.5 rounded-md font-medium hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">Cancel</button>
          </div>
        </div>
      {:else}
        <button
          onclick={() => confirmingLogout = true}
          class="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg font-medium transition-colors">
          Sign out
        </button>
      {/if}
    </div>
  </aside>

  <!-- MAIN CONTENT -->
  <main class="flex-1 ml-56 p-6">
    {@render children()}
  </main>
</div>
{/if}
