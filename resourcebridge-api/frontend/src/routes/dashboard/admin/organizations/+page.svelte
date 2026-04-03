<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/auth';
  import { getAll as getOrgs, createOrganization } from '$lib/api/organizations';
  import { archiveOrganization, restoreOrganization, updateOrganization } from '$lib/api/admin';
  import { showToast } from '$lib/stores/toast';
  import type { Organization } from '$lib/types';

  let organizations = $state<Organization[]>([]);
  let allOrganizations = $state<Organization[]>([]);
  let loading = $state(true);

  // ── Add org form ────────────────────────────────────────────────────────────
  let showAddOrg = $state(false);
  let orgName = $state('');
  let orgType = $state('Emergency Shelter');
  let orgAddress = $state('');
  let orgPopulation = $state('');
  let orgEmail = $state('');
  let orgPhone = $state('');
  let orgSubmitting = $state(false);
  let orgError = $state('');

  // ── Edit org form ────────────────────────────────────────────────────────────
  let editingOrgId = $state<number | null>(null);
  let editOrgName = $state('');
  let editOrgType = $state('');
  let editOrgAddress = $state('');
  let editOrgPopulation = $state('');
  let editOrgEmail = $state('');
  let editOrgPhone = $state('');
  let editOrgSubmitting = $state(false);
  let editOrgError = $state('');

  let confirmingArchive = $state<number | null>(null);

  const ORG_TYPES = [
    'Emergency Shelter', 'Transition Shelter', 'Supportive Housing',
    'Transitional Housing', 'Food Bank', 'Community Services',
    'Community Kitchen', 'Clothing + Household Depot', 'Meals + Outreach', 'Other',
  ];

  onMount(async () => {
    try {
      const orgs = await getOrgs();
      organizations = orgs;
      allOrganizations = orgs;
    } catch {}
    loading = false;
  });

  let archivedOrgs = $derived(allOrganizations.filter(o => !organizations.find(a => a.id === o.id)));

  async function addOrganization() {
    orgSubmitting = true;
    orgError = '';
    try {
      const newOrg = await createOrganization({
        name: orgName, type: orgType, address: orgAddress,
        populationServed: orgPopulation, contactEmail: orgEmail, contactPhone: orgPhone,
      }, $auth.token!);
      organizations = [...organizations, newOrg];
      allOrganizations = [...allOrganizations, newOrg];
      showToast(`${newOrg.name} added successfully`);
      orgName = ''; orgType = 'Emergency Shelter'; orgAddress = '';
      orgPopulation = ''; orgEmail = ''; orgPhone = '';
      showAddOrg = false;
    } catch (e: any) {
      orgError = e.message || 'Failed to add organization';
    } finally {
      orgSubmitting = false;
    }
  }

  function startEditOrg(org: Organization) {
    editingOrgId = org.id;
    editOrgName = org.name;
    editOrgType = org.type ?? '';
    editOrgAddress = org.address ?? '';
    editOrgPopulation = org.populationServed ?? '';
    editOrgEmail = org.contactEmail ?? '';
    editOrgPhone = org.contactPhone ?? '';
    editOrgError = '';
  }

  function cancelEditOrg() {
    editingOrgId = null;
    editOrgError = '';
  }

  async function saveEditOrg() {
    if (!editingOrgId) return;
    editOrgSubmitting = true;
    editOrgError = '';
    try {
      const updated = await updateOrganization(editingOrgId, {
        name: editOrgName, type: editOrgType, address: editOrgAddress,
        populationServed: editOrgPopulation, contactEmail: editOrgEmail, contactPhone: editOrgPhone,
      }, $auth.token!);
      organizations = organizations.map(o => o.id === updated.id ? updated : o);
      allOrganizations = allOrganizations.map(o => o.id === updated.id ? updated : o);
      showToast(`${updated.name} updated`);
      editingOrgId = null;
    } catch (e: any) {
      editOrgError = e.message || 'Failed to update organization';
    } finally {
      editOrgSubmitting = false;
    }
  }

  async function confirmArchiveOrg(id: number) {
    try {
      await archiveOrganization(id, $auth.token!);
      organizations = organizations.filter(o => o.id !== id);
      showToast('Organization archived');
    } catch (e: any) {
      showToast(e.message || 'Failed to archive organization');
    } finally {
      confirmingArchive = null;
    }
  }

  async function handleRestoreOrg(id: number) {
    try {
      await restoreOrganization(id, $auth.token!);
      const restored = allOrganizations.find(o => o.id === id);
      if (restored) organizations = [...organizations, restored];
      showToast('Organization restored');
    } catch (e: any) {
      showToast(e.message || 'Failed to restore organization');
    }
  }
</script>

<svelte:head><title>Organizations — ResourceBridge</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500"></div>
  </div>
{:else}
<div class="space-y-6">

  <!-- Header -->
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Organizations</h1>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">{organizations.length} active shelters & organizations</p>
    </div>
    <button
      onclick={() => { showAddOrg = !showAddOrg; orgError = ''; cancelEditOrg(); }}
      class="flex items-center gap-1.5 px-4 py-2 bg-brand-500 hover:bg-brand-600 text-white text-sm font-medium rounded-lg transition-colors">
      {showAddOrg ? '✕ Cancel' : '+ Add shelter'}
    </button>
  </div>

  <!-- Add org form -->
  {#if showAddOrg}
  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-brand-200 dark:border-brand-800 p-6 max-w-2xl">
    <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">New Shelter / Organization</h3>
    {#if orgError}
      <div class="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">{orgError}</div>
    {/if}
    <form onsubmit={(e) => { e.preventDefault(); addOrganization(); }} class="space-y-4">
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div class="sm:col-span-2">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Organization name *</label>
          <input type="text" bind:value={orgName} required placeholder="e.g. House of Nazareth"
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Type *</label>
          <select bind:value={orgType}
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm">
            {#each ORG_TYPES as t}<option value={t}>{t}</option>{/each}
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Population served</label>
          <input type="text" bind:value={orgPopulation} placeholder="e.g. Men, women, families"
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
        </div>
        <div class="sm:col-span-2">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Address</label>
          <input type="text" bind:value={orgAddress} placeholder="e.g. 75 Albert St, Moncton, NB"
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Contact email</label>
          <input type="email" bind:value={orgEmail} placeholder="info@shelter.ca"
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Contact phone</label>
          <input type="tel" bind:value={orgPhone} placeholder="506-000-0000"
            class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
        </div>
      </div>
      <button type="submit" disabled={orgSubmitting}
        class="bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white font-semibold px-6 py-2.5 rounded-lg transition-colors text-sm">
        {orgSubmitting ? 'Adding…' : 'Add organization'}
      </button>
    </form>
  </div>
  {/if}

  <!-- Active org list -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
    {#each organizations as org (org.id)}
      {#if editingOrgId === org.id}
      <!-- Inline edit form -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-brand-300 dark:border-brand-700 p-4 lg:col-span-2">
        <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Edit — {org.name}</h3>
        {#if editOrgError}
          <div class="bg-red-50 border border-red-100 text-red-700 text-xs rounded-lg px-3 py-2 mb-3">{editOrgError}</div>
        {/if}
        <form onsubmit={(e) => { e.preventDefault(); saveEditOrg(); }} class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div class="sm:col-span-2">
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Name *</label>
            <input type="text" bind:value={editOrgName} required
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Type</label>
            <select bind:value={editOrgType}
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm">
              {#each ORG_TYPES as t}<option value={t}>{t}</option>{/each}
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Population served</label>
            <input type="text" bind:value={editOrgPopulation}
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
          </div>
          <div class="sm:col-span-2">
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Address</label>
            <input type="text" bind:value={editOrgAddress}
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Contact email</label>
            <input type="email" bind:value={editOrgEmail}
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Contact phone</label>
            <input type="tel" bind:value={editOrgPhone}
              class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
          </div>
          <div class="sm:col-span-2 flex gap-2 pt-1">
            <button type="submit" disabled={editOrgSubmitting}
              class="bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors">
              {editOrgSubmitting ? 'Saving…' : 'Save changes'}
            </button>
            <button type="button" onclick={cancelEditOrg}
              class="bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-600 dark:text-gray-300 text-sm font-medium px-4 py-2 rounded-lg transition-colors">
              Cancel
            </button>
          </div>
        </form>
      </div>

      {:else}
      <!-- Org card -->
      <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-100 dark:border-gray-700 p-4">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <p class="text-sm font-semibold text-gray-900 dark:text-white truncate">{org.name}</p>
            <p class="text-xs text-brand-600 dark:text-brand-400 mt-0.5">{org.type}</p>
            {#if org.populationServed}
              <p class="text-xs text-gray-400 mt-1">{org.populationServed}</p>
            {/if}
          </div>
          <div class="text-right shrink-0">
            {#if org.contactPhone}
              <p class="text-xs text-gray-500 dark:text-gray-400">{org.contactPhone}</p>
            {/if}
            {#if org.contactEmail}
              <p class="text-xs text-gray-400 truncate max-w-[140px]">{org.contactEmail}</p>
            {/if}
          </div>
        </div>
        {#if org.address}
          <p class="text-xs text-gray-400 mt-2">📍 {org.address}</p>
        {/if}
        <div class="flex gap-2 mt-3 pt-3 border-t border-gray-50 dark:border-gray-700">
          <button onclick={() => startEditOrg(org)}
            class="text-xs text-brand-600 dark:text-brand-400 hover:text-brand-700 font-medium px-2 py-1 rounded hover:bg-brand-50 dark:hover:bg-brand-900/20 transition-colors">
            ✏️ Edit
          </button>
          {#if confirmingArchive === org.id}
            <span class="text-xs text-gray-500 dark:text-gray-400 ml-1 self-center">Archive?</span>
            <button onclick={() => confirmArchiveOrg(org.id)}
              class="text-xs bg-red-500 hover:bg-red-600 text-white font-medium px-2 py-1 rounded transition-colors">
              Yes
            </button>
            <button onclick={() => confirmingArchive = null}
              class="text-xs bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 text-gray-600 dark:text-gray-300 font-medium px-2 py-1 rounded transition-colors">
              No
            </button>
          {:else}
            <button onclick={() => confirmingArchive = org.id}
              class="text-xs text-red-500 hover:text-red-600 font-medium px-2 py-1 rounded hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors">
              🗄️ Archive
            </button>
          {/if}
        </div>
      </div>
      {/if}
    {/each}
  </div>

  <!-- Archived orgs -->
  {#if archivedOrgs.length > 0}
  <div>
    <h3 class="text-sm font-medium text-gray-400 dark:text-gray-500 mb-3">Archived ({archivedOrgs.length})</h3>
    <div class="space-y-2">
      {#each archivedOrgs as org}
        <div class="flex items-center justify-between bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-100 dark:border-gray-700 px-4 py-3">
          <div>
            <span class="text-sm font-medium text-gray-500 dark:text-gray-400">{org.name}</span>
            <span class="text-xs text-gray-400 ml-2">{org.type}</span>
          </div>
          <button onclick={() => handleRestoreOrg(org.id)}
            class="text-xs text-brand-600 dark:text-brand-400 hover:text-brand-700 font-medium px-3 py-1.5 rounded-lg hover:bg-brand-50 dark:hover:bg-brand-900/20 transition-colors">
            ↩ Restore
          </button>
        </div>
      {/each}
    </div>
  </div>
  {/if}

</div>
{/if}
