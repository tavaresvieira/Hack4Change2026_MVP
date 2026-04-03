<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/auth';
  import { getAllStaff, deactivateStaff, restoreStaff } from '$lib/api/admin';
  import type { StaffMember } from '$lib/api/admin';
  import { showToast } from '$lib/stores/toast';

  let staff = $state<StaffMember[]>([]);
  let loading = $state(true);

  let confirmingDeactivate = $state<number | null>(null);

  onMount(async () => {
    try {
      staff = await getAllStaff($auth.token!);
    } catch {}
    loading = false;
  });

  let activeStaff = $derived(staff.filter(s => !s.deletedAt));
  let deactivatedStaff = $derived(staff.filter(s => !!s.deletedAt));

  async function confirmDeactivate(id: number) {
    try {
      await deactivateStaff(id, $auth.token!);
      staff = staff.map(s => s.id === id ? { ...s, deletedAt: new Date().toISOString() } : s);
      showToast('Staff member deactivated');
    } catch (e: any) {
      showToast(e.message || 'Failed to deactivate staff member');
    } finally {
      confirmingDeactivate = null;
    }
  }

  async function handleRestore(id: number) {
    try {
      await restoreStaff(id, $auth.token!);
      staff = staff.map(s => s.id === id ? { ...s, deletedAt: null } : s);
      showToast('Staff member restored');
    } catch (e: any) {
      showToast(e.message || 'Failed to restore staff member');
    }
  }


</script>

<svelte:head><title>Staff — ResourceBridge</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500"></div>
  </div>
{:else}
<div class="space-y-8">

  <!-- Header -->
  <div>
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Staff</h1>
    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{activeStaff.length} active members across the network</p>
  </div>

  <!-- ── ACTIVE STAFF TABLE ─────────────────────────────────────────────────── -->
  {#if activeStaff.length > 0}
  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="bg-gray-50 dark:bg-gray-700/50 text-left">
          <th class="px-4 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Name</th>
          <th class="px-4 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide hidden sm:table-cell">Email</th>
          <th class="px-4 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Role</th>
          <th class="px-4 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Organization</th>
          <th class="px-4 py-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide">Status</th>
          <th class="px-4 py-3"></th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-50 dark:divide-gray-700">
        {#each activeStaff as member}
          <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors">
            <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{member.name}</td>
            <td class="px-4 py-3 text-gray-500 dark:text-gray-400 hidden sm:table-cell">{member.email}</td>
            <td class="px-4 py-3">
              <span class="text-xs font-medium px-2 py-0.5 rounded-full
                {member.role === 'ADMIN' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400' : 'bg-brand-100 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'}">
                {member.role}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-sm">
              {member.organization?.name ?? '—'}
            </td>
            <td class="px-4 py-3">
              <span class="text-xs font-medium px-2 py-0.5 rounded-full bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400">Active</span>
            </td>
            <td class="px-4 py-3 text-right">
              {#if confirmingDeactivate === member.id}
                <span class="text-xs text-gray-500 mr-1">Deactivate?</span>
                <button onclick={() => confirmDeactivate(member.id)}
                  class="text-xs bg-red-500 hover:bg-red-600 text-white font-medium px-2 py-1 rounded mr-1 transition-colors">
                  Yes
                </button>
                <button onclick={() => confirmingDeactivate = null}
                  class="text-xs bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 text-gray-600 dark:text-gray-300 font-medium px-2 py-1 rounded transition-colors">
                  No
                </button>
              {:else}
                <button onclick={() => confirmingDeactivate = member.id}
                  class="text-xs text-red-500 hover:text-red-600 font-medium px-2 py-1 rounded hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">
                  Deactivate
                </button>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
  {:else}
    <p class="text-sm text-gray-400">No active staff members yet.</p>
  {/if}

  <!-- Deactivated staff -->
  {#if deactivatedStaff.length > 0}
  <div class="bg-gray-50 dark:bg-gray-800/50 rounded-2xl border border-gray-100 dark:border-gray-700 overflow-hidden">
    <div class="px-4 py-3 border-b border-gray-100 dark:border-gray-700">
      <h3 class="text-sm font-medium text-gray-400 dark:text-gray-500">Deactivated ({deactivatedStaff.length})</h3>
    </div>
    <table class="w-full text-sm">
      <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
        {#each deactivatedStaff as member}
          <tr>
            <td class="px-4 py-3 text-gray-400 dark:text-gray-500 font-medium">{member.name}</td>
            <td class="px-4 py-3 text-gray-400 dark:text-gray-500 hidden sm:table-cell">{member.email}</td>
            <td class="px-4 py-3">
              <span class="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-200 text-gray-500 dark:bg-gray-700 dark:text-gray-400">Deactivated</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button onclick={() => handleRestore(member.id)}
                class="text-xs text-brand-600 dark:text-brand-400 hover:text-brand-700 font-medium px-2 py-1 rounded hover:bg-brand-50 dark:hover:bg-brand-900/20 transition-colors">
                ↩ Restore
              </button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
  {/if}


</div>
{/if}
