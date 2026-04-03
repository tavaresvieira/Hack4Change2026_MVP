<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/auth';
  import { getAll as getOrgs } from '$lib/api/organizations';
  import { createInvite } from '$lib/api/invitations';
  import { showToast } from '$lib/stores/toast';
  import type { Organization } from '$lib/types';

  let organizations = $state<Organization[]>([]);
  let loading = $state(true);

  let inviteEmail = $state('');
  let inviteOrgId = $state<number | null>(null);
  let inviteRole = $state<'STAFF' | 'ADMIN'>('STAFF');
  let inviteSubmitting = $state(false);
  let inviteError = $state('');

  interface GeneratedInvite { email: string; orgName: string; url: string; role: string; copied: boolean; }
  let generatedInvites = $state<GeneratedInvite[]>([]);

  onMount(async () => {
    try {
      const orgs = await getOrgs();
      organizations = orgs;
      if (orgs.length > 0) inviteOrgId = orgs[0].id;
    } catch {}
    loading = false;
  });

  async function sendInvite() {
    if (!inviteEmail || !inviteOrgId) return;
    inviteSubmitting = true;
    inviteError = '';
    try {
      const res = await createInvite(inviteEmail, inviteOrgId, $auth.token!, inviteRole);
      generatedInvites = [
        { email: res.email, orgName: res.organizationName, url: res.inviteUrl!, role: inviteRole, copied: false },
        ...generatedInvites,
      ];
      showToast(`Invite created for ${res.email}`);
      inviteEmail = '';
      inviteRole = 'STAFF';
    } catch (e: any) {
      inviteError = e.message || 'Failed to create invitation';
    } finally {
      inviteSubmitting = false;
    }
  }

  function copyLink(index: number) {
    const url = generatedInvites[index].url;
    const doFallback = () => {
      const ta = document.createElement('textarea');
      ta.value = url;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.focus(); ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
    };
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).catch(doFallback);
    } else {
      doFallback();
    }
    generatedInvites[index] = { ...generatedInvites[index], copied: true };
    setTimeout(() => { generatedInvites[index] = { ...generatedInvites[index], copied: false }; }, 2000);
  }
</script>

<svelte:head><title>Invite Member — ResourceBridge</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500"></div>
  </div>
{:else}
<div class="space-y-6 max-w-lg mx-auto">

  <div>
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">✉️ Invite a Team Member</h1>
    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
      Generate an invite link. An invitation email will be sent automatically — you can also copy the link to share directly as a backup.
    </p>
  </div>

  {#if inviteError}
    <div class="bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800 text-red-700 dark:text-red-300 text-sm rounded-lg px-4 py-3">
      {inviteError}
    </div>
  {/if}

  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-6">
    <form onsubmit={(e) => { e.preventDefault(); sendInvite(); }} class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Email address</label>
        <input type="email" bind:value={inviteEmail} required placeholder="staff@shelter.ca"
          class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Role</label>
        <div class="flex gap-2">
          <button type="button" onclick={() => inviteRole = 'STAFF'}
            class="flex-1 py-2 rounded-lg text-sm font-medium border transition-colors
              {inviteRole === 'STAFF' ? 'bg-brand-500 text-white border-brand-500' : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border-gray-200 dark:border-gray-600 hover:border-brand-300'}">
            Shelter Staff
          </button>
          <button type="button" onclick={() => inviteRole = 'ADMIN'}
            class="flex-1 py-2 rounded-lg text-sm font-medium border transition-colors
              {inviteRole === 'ADMIN' ? 'bg-purple-600 text-white border-purple-600' : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border-gray-200 dark:border-gray-600 hover:border-purple-300'}">
            Admin
          </button>
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Organization</label>
        <select bind:value={inviteOrgId}
          class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:border-brand-500 focus:ring-brand-500 text-sm">
          {#each organizations as org}
            <option value={org.id}>{org.name}</option>
          {/each}
        </select>
      </div>
      <button type="submit" disabled={inviteSubmitting}
        class="w-full disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm
          {inviteRole === 'ADMIN' ? 'bg-purple-600 hover:bg-purple-700' : 'bg-brand-500 hover:bg-brand-600'}">
        {inviteSubmitting ? 'Generating…' : `Generate ${inviteRole === 'ADMIN' ? 'admin' : 'staff'} invite link`}
      </button>
    </form>
  </div>

  {#if generatedInvites.length > 0}
    <div class="space-y-3">
      <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300">Generated this session</h3>
      {#each generatedInvites as invite, i}
        <div class="bg-white dark:bg-gray-800 border border-gray-100 dark:border-gray-700 rounded-xl p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <p class="text-sm font-medium text-gray-900 dark:text-white truncate">{invite.email}</p>
                <span class="text-xs font-medium px-1.5 py-0.5 rounded-full shrink-0
                  {invite.role === 'ADMIN' ? 'bg-purple-100 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400' : 'bg-brand-100 text-brand-700 dark:bg-brand-900/20 dark:text-brand-400'}">
                  {invite.role}
                </span>
                <span class="text-xs text-green-600 dark:text-green-400 shrink-0">✉️ Email sent</span>
              </div>
              <p class="text-xs text-gray-400 dark:text-gray-500">{invite.orgName}</p>
              <p class="text-xs text-gray-400 dark:text-gray-500 mt-1 truncate font-mono">{invite.url}</p>
            </div>
            <button onclick={() => copyLink(i)}
              class="flex-shrink-0 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors
                {invite.copied ? 'bg-green-100 text-green-700' : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200'}">
              {invite.copied ? 'Copied!' : 'Copy link'}
            </button>
          </div>
        </div>
      {/each}
      <p class="text-xs text-gray-400 dark:text-gray-500">Links expire in 7 days and can only be used once.</p>
    </div>
  {/if}

</div>
{/if}
