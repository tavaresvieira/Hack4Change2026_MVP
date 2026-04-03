<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { auth, isAdmin } from '$lib/stores/auth';
  import { darkMode } from '$lib/stores/darkMode';
  import { logout as apiLogout } from '$lib/api/auth';
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

  let sidebarOpen = $state(false);
  let confirmingLogout = $state(false);

  async function logout() {
    const refreshToken = $auth.refreshToken;
    auth.clear();
    if (refreshToken) {
      await apiLogout(refreshToken).catch(() => {});
    }
    goto('/');
  }

  let currentPath = $derived($page.url.pathname + $page.url.hash);

  const staffNav = [
    { href: '/dashboard/staff', label: 'Dashboard', icon: '📊' },
    { href: '/dashboard/staff#inventory', label: 'Inventory', icon: '📦' },
    { href: '/dashboard/staff#needs', label: 'Our Needs', icon: '📋' },
    { href: '/dashboard/staff#transfers', label: 'Incoming', icon: '🚚' },
    { href: '/dashboard/staff#announcements', label: 'Announcements', icon: '📢' },
    { href: '/dashboard/staff#community', label: 'Community', icon: '🌐' },
    { href: '/dashboard/staff#exchanges', label: 'Exchanges', icon: '🤝' },
  ];

  const adminNav = [
    { href: '/dashboard/admin', label: 'Dashboard', icon: '🛡️' },
    { href: '/dashboard/admin/organizations', label: 'Organizations', icon: '🏠' },
    { href: '/dashboard/admin/staff', label: 'Staff', icon: '👥' },
    { href: '/dashboard/admin/invite', label: 'Invite Member', icon: '✉️' },
    { href: '/dashboard/admin/financial-donations', label: 'Financial Donations', icon: '💰' },
  ];

  let navItems = $derived($isAdmin ? adminNav : staffNav);
  let roleLabel = $derived($isAdmin ? 'Admin' : 'Shelter Staff');
  let roleBadgeClass = $derived($isAdmin
    ? 'bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400'
    : 'bg-brand-100 dark:bg-brand-900/20 text-brand-700 dark:text-brand-400'
  );
</script>

{#if $auth.token}
<div class="min-h-screen flex bg-gray-50 dark:bg-gray-900">

  <!-- MOBILE OVERLAY -->
  {#if sidebarOpen}
    <div
      class="fixed inset-0 bg-black/40 z-20 lg:hidden"
      onclick={() => sidebarOpen = false}
      role="button"
      tabindex="-1"
      aria-label="Close sidebar">
    </div>
  {/if}

  <!-- SIDEBAR -->
  <aside class="
    w-64 bg-white dark:bg-gray-800 border-r border-gray-100 dark:border-gray-700
    flex flex-col fixed h-full shadow-sm z-30 transition-transform duration-200
    {sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
  ">
    <div class="p-4 border-b border-gray-100 dark:border-gray-700 flex items-center justify-between">
      <a href="/dashboard" class="flex items-center gap-2">
        <div class="w-8 h-8 bg-brand-500 rounded-lg flex items-center justify-center flex-shrink-0">
          <span class="text-white font-bold text-sm">RB</span>
        </div>
        <span class="font-bold text-gray-900 dark:text-white text-sm">ResourceBridge</span>
      </a>
      <!-- Close button (mobile only) -->
      <button
        onclick={() => sidebarOpen = false}
        class="lg:hidden p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        aria-label="Close menu">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <nav class="flex-1 p-3 space-y-1 overflow-y-auto">
      {#each navItems as item}
        <a
          href={item.href}
          onclick={() => sidebarOpen = false}
          class="flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors
            {currentPath === item.href
              ? 'bg-brand-50 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'
              : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700 hover:text-gray-900 dark:hover:text-white'}">
          <span class="text-base">{item.icon}</span>
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
        <div class="inline-flex items-center mt-1 px-2 py-0.5 {roleBadgeClass} rounded-full text-xs font-medium">
          {roleLabel}
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
  <div class="flex-1 flex flex-col min-w-0 lg:ml-64">
    <!-- MOBILE TOPBAR -->
    <header class="lg:hidden sticky top-0 z-10 bg-white dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700 px-4 py-3 flex items-center gap-3">
      <button
        onclick={() => sidebarOpen = true}
        class="p-1.5 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        aria-label="Open menu">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      </button>
      <div class="flex items-center gap-2">
        <div class="w-6 h-6 bg-brand-500 rounded flex items-center justify-center">
          <span class="text-white font-bold text-xs">RB</span>
        </div>
        <span class="font-semibold text-gray-900 dark:text-white text-sm">ResourceBridge</span>
      </div>
    </header>

    <main class="flex-1 p-4 lg:p-6 overflow-x-hidden">
      {@render children()}
    </main>
  </div>
</div>
{/if}
