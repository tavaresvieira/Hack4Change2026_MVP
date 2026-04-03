<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/auth';
  import { getFinancialDonations, getFinancialDonationStats } from '$lib/api/financialDonations';
  import type { FinancialDonation, FinancialDonationStats } from '$lib/api/financialDonations';

  let donations = $state<FinancialDonation[]>([]);
  let stats = $state<FinancialDonationStats | null>(null);
  let loading = $state(true);
  let search = $state('');

  let filtered = $derived(
    search.trim()
      ? donations.filter(d =>
          d.donorName.toLowerCase().includes(search.toLowerCase()) ||
          d.donorEmail.toLowerCase().includes(search.toLowerCase())
        )
      : donations
  );

  onMount(async () => {
    try {
      const [d, s] = await Promise.all([
        getFinancialDonations($auth.token!),
        getFinancialDonationStats($auth.token!)
      ]);
      donations = d;
      stats = s;
    } catch {}
    loading = false;
  });
</script>

<svelte:head><title>Financial Donations — ResourceBridge</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500"></div>
  </div>
{:else}
<div class="space-y-6">

  <!-- Header -->
  <div>
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">💰 Financial Donations</h1>
    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">All monetary donations from supporters</p>
  </div>

  <!-- Total raised banner -->
  {#if stats}
  <div class="bg-gradient-to-r from-green-500 to-emerald-600 rounded-2xl p-5 text-white">
    <p class="text-sm font-medium opacity-80 uppercase tracking-wide">Total Raised</p>
    <p class="text-4xl font-bold mt-1">${Number(stats.total).toFixed(2)} <span class="text-xl font-normal opacity-80">CAD</span></p>
    <p class="text-sm opacity-75 mt-1">from {stats.count} donation{stats.count !== 1 ? 's' : ''}</p>
  </div>
  {/if}

  <!-- Search -->
  <div class="relative max-w-sm">
    <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
    </svg>
    <input
      type="text"
      bind:value={search}
      placeholder="Search by name or email…"
      class="w-full pl-9 pr-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-brand-400"/>
  </div>

  <!-- Donations list -->
  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700">
    {#if filtered.length === 0}
      <div class="text-center py-12">
        <p class="text-3xl mb-2">💸</p>
        <p class="text-sm text-gray-400">{search ? 'No donations match your search' : 'No financial donations yet'}</p>
      </div>
    {:else}
      <div class="divide-y divide-gray-50 dark:divide-gray-700">
        {#each filtered as donation}
          <div class="p-4 flex items-start justify-between gap-4">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold text-gray-900 dark:text-white">{donation.donorName}</span>
                <span class="text-xs text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full">{donation.donorEmail}</span>
              </div>
              {#if donation.message}
                <p class="text-sm text-gray-600 dark:text-gray-300 mt-1">"{donation.message}"</p>
              {/if}
              <p class="text-xs text-gray-400 mt-1">
                {new Date(donation.createdAt).toLocaleDateString('en-CA', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
                · {new Date(donation.createdAt).toLocaleTimeString('en-CA', { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-xl font-bold text-green-600 dark:text-green-400">${Number(donation.amount).toFixed(2)}</p>
              <p class="text-xs text-gray-400">CAD</p>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  </div>

</div>
{/if}
