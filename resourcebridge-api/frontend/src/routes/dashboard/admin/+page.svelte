<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/auth';
  import { getStats } from '$lib/api/admin';
  import type { AdminStats } from '$lib/api/admin';
  import { getFinancialDonations, getFinancialDonationStats } from '$lib/api/financialDonations';
  import type { FinancialDonation, FinancialDonationStats } from '$lib/api/financialDonations';

  let stats = $state<AdminStats | null>(null);
  let financialDonations = $state<FinancialDonation[]>([]);
  let financialStats = $state<FinancialDonationStats | null>(null);
  let loading = $state(true);

  onMount(async () => {
    try {
      stats = await getStats($auth.token!);
    } catch {}
    try {
      const [donations, fStats] = await Promise.all([
        getFinancialDonations($auth.token!),
        getFinancialDonationStats($auth.token!)
      ]);
      financialDonations = donations;
      financialStats = fStats;
    } catch {}
    loading = false;
  });

  function expiryLabel(days: number): string {
    if (days < 0) return `expired ${Math.abs(days)} day${Math.abs(days) !== 1 ? 's' : ''} ago`;
    if (days === 0) return 'expires today';
    if (days === 1) return 'expires tomorrow';
    return `expires in ${days} days`;
  }

  function expiryColor(days: number): string {
    if (days <= 0) return 'text-red-600 dark:text-red-400 font-semibold';
    if (days <= 2) return 'text-orange-600 dark:text-orange-400 font-medium';
    return 'text-amber-600 dark:text-amber-400';
  }

  const urgencyColor: Record<string, string> = {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-gray-100 text-gray-600',
  };
  const urgencyBar: Record<string, string> = {
    CRITICAL: 'bg-red-500', HIGH: 'bg-orange-400', MEDIUM: 'bg-yellow-400', LOW: 'bg-gray-300',
  };
  const statusColor: Record<string, string> = {
    OFFERED: 'bg-blue-400', ASSIGNED: 'bg-yellow-400', DELIVERED: 'bg-orange-400', RECEIVED: 'bg-green-500',
  };
</script>

<svelte:head><title>Admin Dashboard — ResourceBridge</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center h-64">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500"></div>
  </div>
{:else}
<div class="space-y-8">

  <!-- Header -->
  <div>
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Admin Dashboard</h1>
    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Welcome back, {$auth.name}</p>
  </div>

  {#if stats}
  <!-- ── STAT CARDS ─────────────────────────────────────────────────────────── -->
  <div class="grid grid-cols-2 lg:grid-cols-5 gap-4">
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-5">
      <p class="text-xs font-medium text-gray-400 uppercase tracking-wide">Donations</p>
      <p class="text-3xl font-bold text-gray-900 dark:text-white mt-1">{stats.totalDonations}</p>
      <p class="text-xs text-green-600 mt-1">{stats.donationsByStatus['RECEIVED'] ?? 0} received</p>
    </div>
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-5">
      <p class="text-xs font-medium text-gray-400 uppercase tracking-wide">Open Needs</p>
      <p class="text-3xl font-bold text-gray-900 dark:text-white mt-1">{stats.openNeeds}</p>
      <p class="text-xs text-gray-400 mt-1">{stats.fulfilledNeeds} fulfilled</p>
    </div>
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-5">
      <p class="text-xs font-medium text-gray-400 uppercase tracking-wide">Transfers</p>
      <p class="text-3xl font-bold text-gray-900 dark:text-white mt-1">{stats.totalTransfers}</p>
      <p class="text-xs text-blue-600 mt-1">{stats.transfersByStatus['IN_TRANSIT'] ?? 0} in transit</p>
    </div>
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-5">
      <p class="text-xs font-medium text-gray-400 uppercase tracking-wide">Organizations</p>
      <p class="text-3xl font-bold text-gray-900 dark:text-white mt-1">{stats.totalOrganizations}</p>
      <p class="text-xs text-gray-400 mt-1">active shelters</p>
    </div>
    <div class="bg-green-50 dark:bg-green-900/20 border border-green-100 dark:border-green-800/50 rounded-2xl p-5">
      <p class="text-xs font-medium text-green-600 dark:text-green-400 uppercase tracking-wide">💰 Money Raised</p>
      <p class="text-3xl font-bold text-green-700 dark:text-green-300 mt-1">
        ${financialStats ? Number(financialStats.total).toFixed(2) : '0.00'}
      </p>
      <p class="text-xs text-green-600 dark:text-green-400 mt-1">{financialStats?.count ?? 0} donation{(financialStats?.count ?? 0) !== 1 ? 's' : ''}</p>
    </div>
  </div>

  <!-- ── EXPIRY ALERT ──────────────────────────────────────────────────────── -->
  {#if stats.expiringItems && stats.expiringItems.length > 0}
    {@const expiredCount = stats.expiringItems.filter(i => i.daysUntilExpiry < 0).length}
    {@const soonCount = stats.expiringItems.length - expiredCount}
    <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800/50 rounded-xl p-4">
      <div class="flex items-center gap-2 mb-3">
        <span class="text-lg">⚠️</span>
        <span class="font-semibold text-amber-800 dark:text-amber-300 text-sm">
          {#if expiredCount > 0 && soonCount > 0}
            {expiredCount} item{expiredCount !== 1 ? 's' : ''} expired · {soonCount} expiring within 7 days across the network
          {:else if expiredCount > 0}
            {expiredCount} item{expiredCount !== 1 ? 's' : ''} already expired across the network
          {:else}
            {soonCount} item{soonCount !== 1 ? 's' : ''} expiring within 7 days across the network
          {/if}
        </span>
      </div>
      <div class="space-y-2">
        {#each stats.expiringItems as inv}
          <div class="flex items-center justify-between bg-white dark:bg-gray-800 rounded-lg px-3 py-2 text-sm">
            <div class="min-w-0">
              <span class="font-medium text-gray-800 dark:text-gray-100">{inv.itemName}</span>
              <span class="text-xs text-gray-400 ml-2">{inv.organizationName}</span>
            </div>
            <div class="flex items-center gap-4 shrink-0">
              <span class="text-gray-500 dark:text-gray-400 text-xs">{inv.quantity} {inv.unit}</span>
              <span class="text-xs {expiryColor(inv.daysUntilExpiry)}">{expiryLabel(inv.daysUntilExpiry)}</span>
            </div>
          </div>
        {/each}
      </div>
    </div>
  {/if}

  <!-- ── CHARTS ROW ─────────────────────────────────────────────────────────── -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-6">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Open Needs by Urgency</h2>
      <div class="space-y-3">
        {#each Object.entries(stats.openNeedsByUrgency) as [urgency, count]}
          {@const total = stats.openNeeds || 1}
          {@const pct = Math.round((count / total) * 100)}
          <div>
            <div class="flex justify-between text-xs mb-1">
              <span class="font-medium text-gray-600 dark:text-gray-400">{urgency}</span>
              <span class="text-gray-500">{count}</span>
            </div>
            <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
              <div class="h-full rounded-full {urgencyBar[urgency]}" style="width: {pct}%"></div>
            </div>
          </div>
        {/each}
      </div>
    </div>
    <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-6">
      <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Donations by Status</h2>
      <div class="space-y-3">
        {#each Object.entries(stats.donationsByStatus) as [status, count]}
          {@const total = stats.totalDonations || 1}
          {@const pct = Math.round((count / total) * 100)}
          <div>
            <div class="flex justify-between text-xs mb-1">
              <span class="font-medium text-gray-600 dark:text-gray-400">{status}</span>
              <span class="text-gray-500">{count}</span>
            </div>
            <div class="h-2 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
              <div class="h-full rounded-full {statusColor[status] ?? 'bg-gray-400'}" style="width: {pct}%"></div>
            </div>
          </div>
        {/each}
      </div>
    </div>
  </div>

  <!-- ── TOP URGENT NEEDS ───────────────────────────────────────────────────── -->
  {#if stats.topUrgentNeeds.length > 0}
  <div class="bg-white dark:bg-gray-800 rounded-2xl border border-gray-100 dark:border-gray-700 p-6">
    <h2 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Top Urgent Needs Across Network</h2>
    <div class="space-y-2">
      {#each stats.topUrgentNeeds as need}
        <div class="flex items-center justify-between py-2 border-b border-gray-50 dark:border-gray-700 last:border-0">
          <div>
            <span class="text-sm font-medium text-gray-900 dark:text-white">{need.itemName}</span>
            <span class="text-xs text-gray-400 ml-2">{need.organizationName}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-sm text-gray-500">{need.quantityNeeded} units</span>
            <span class="text-xs font-medium px-2 py-0.5 rounded-full {urgencyColor[need.urgency]}">{need.urgency}</span>
          </div>
        </div>
      {/each}
    </div>
  </div>
  {/if}
  {/if}

</div>
{/if}
