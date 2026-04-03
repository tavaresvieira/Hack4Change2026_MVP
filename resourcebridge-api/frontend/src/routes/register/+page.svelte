<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { register, registerByInvite } from '$lib/api/auth';
  import { validateInviteToken } from '$lib/api/invitations';
  import { getAll as getOrgs } from '$lib/api/organizations';
  import { auth } from '$lib/stores/auth';
  import { showToast } from '$lib/stores/toast';
  import type { Organization } from '$lib/types';

  // Invite-token mode
  let inviteToken = $state<string | null>(null);
  let inviteEmail = $state('');
  let inviteOrgName = $state('');
  let tokenError = $state('');

  // Normal mode
  let organizations = $state<Organization[]>([]);
  let organizationId = $state<number | null>(null);

  // Shared fields
  let name = $state('');
  let email = $state('');
  let password = $state('');
  let loading = $state(false);
  let error = $state('');

  onMount(async () => {
    const token = $page.url.searchParams.get('token');
    if (token) {
      inviteToken = token;
      try {
        const info = await validateInviteToken(token);
        inviteEmail = info.email;
        inviteOrgName = info.organizationName;
        email = info.email;
      } catch (e: any) {
        tokenError = e.message || 'This invite link is invalid or has expired.';
      }
    }
    // No token — registration requires an invitation, nothing to load
  });

  async function handleSubmit() {
    loading = true;
    error = '';
    try {
      let res;
      if (inviteToken) {
        res = await registerByInvite({ token: inviteToken, name, password });
      } else {
        if (!organizationId) { error = 'Please select an organization'; loading = false; return; }
        res = await register({ name, email, password, role: 'STAFF', organizationId });
      }
      auth.setAuth(res);
      showToast(`Welcome, ${res.name}!`);
      goto('/dashboard');
    } catch (e: any) {
      error = e.message || 'Registration failed';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head><title>Register — ResourceBridge</title></svelte:head>

<div class="min-h-screen bg-gray-50 flex items-center justify-center px-4 py-10">
  <div class="w-full max-w-sm">
    <div class="text-center mb-8">
      <div class="w-14 h-14 bg-brand-500 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
        <span class="text-white font-bold text-xl">RB</span>
      </div>
      <h1 class="text-2xl font-bold text-gray-900">Create staff account</h1>
      {#if inviteToken && !tokenError}
        <p class="text-sm text-gray-500 mt-1">You've been invited to join <span class="font-medium text-gray-700">{inviteOrgName}</span></p>
      {:else}
        <p class="text-sm text-gray-500 mt-1">For shelter staff members</p>
      {/if}
    </div>

    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
      {#if !inviteToken}
        <div class="text-center py-4">
          <div class="text-4xl mb-3">🔒</div>
          <p class="text-sm font-medium text-gray-700 mb-1">Registration is by invitation only</p>
          <p class="text-xs text-gray-400">Ask your admin for an invite link to create your account.</p>
        </div>
      {:else if tokenError}
        <div class="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg px-4 py-3">
          {tokenError}
        </div>
      {:else}
        {#if error}
          <div class="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">{error}</div>
        {/if}

        <form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5">Full name</label>
            <input type="text" bind:value={name} required class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm" placeholder="Jane Smith" />
          </div>

          {#if inviteToken}
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">Email</label>
              <input type="email" value={inviteEmail} disabled class="w-full rounded-lg border-gray-200 bg-gray-50 text-gray-500 text-sm cursor-not-allowed" />
              <p class="text-xs text-gray-400 mt-1">Set by your invitation.</p>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">Organization</label>
              <input type="text" value={inviteOrgName} disabled class="w-full rounded-lg border-gray-200 bg-gray-50 text-gray-500 text-sm cursor-not-allowed" />
            </div>
          {:else}
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">Email</label>
              <input type="email" bind:value={email} required class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm" placeholder="you@organization.ca" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5">Your organization</label>
              <select bind:value={organizationId} class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm">
                {#each organizations as org}
                  <option value={org.id}>{org.name}</option>
                {/each}
              </select>
            </div>
          {/if}

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
            <input type="password" bind:value={password} required class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm" placeholder="••••••••" />
          </div>

          <button type="submit" disabled={loading} class="w-full bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm">
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>
      {/if}
    </div>

    {#if inviteToken && !tokenError}
    <p class="text-center text-sm text-gray-500 mt-4">
      Already have an account? <a href="/login" class="text-brand-600 font-medium hover:underline">Sign in</a>
    </p>
    {/if}
  </div>
</div>
