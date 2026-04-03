<script lang="ts">
  import { onMount } from 'svelte';
  import { getUnfulfilledPaged } from '$lib/api/needs';
  import UrgencyBadge from '$lib/components/UrgencyBadge.svelte';
  import Pagination from '$lib/components/Pagination.svelte';
  import type { Need } from '$lib/types';

  let needs        = $state<Need[]>([]);
  let loading      = $state(true);
  let currentPage  = $state(0);
  let totalPages   = $state(0);
  let totalElements = $state(0);
  const PAGE_SIZE  = 12;

  const URGENCY_ORDER: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
  let urgentNeeds = $derived(
    needs
      .filter(n => n.urgency === 'CRITICAL' || n.urgency === 'HIGH')
      .sort((a, b) => (URGENCY_ORDER[a.urgency] ?? 2) - (URGENCY_ORDER[b.urgency] ?? 2))
  );
  let otherNeeds = $derived(
    needs.filter(n => n.urgency !== 'CRITICAL' && n.urgency !== 'HIGH')
  );

  async function loadPage(page: number) {
    loading = true;
    try {
      const result = await getUnfulfilledPaged(page, PAGE_SIZE);
      needs = result.content;
      currentPage = result.page;
      totalPages = result.totalPages;
      totalElements = result.totalElements;
    } catch {}
    finally { loading = false; }
  }

  onMount(() => loadPage(0));
</script>

<svelte:head><title>ResourceBridge — Donate to Moncton Homeless Organisations</title></svelte:head>

<div class="min-h-screen bg-gray-50 flex flex-col">

  <!-- NAV -->
  <nav class="bg-white border-b border-gray-200 px-4 py-4">
    <div class="max-w-3xl mx-auto flex items-center justify-between">
      <div class="flex items-center gap-2">
        <div class="w-9 h-9 bg-brand-500 rounded-xl flex items-center justify-center">
          <span class="text-white font-bold text-sm">RB</span>
        </div>
        <span class="font-bold text-gray-900 text-lg">ResourceBridge</span>
      </div>
      <a href="/login" class="text-sm text-gray-500 hover:text-brand-600 font-medium transition-colors">
        Staff login →
      </a>
    </div>
  </nav>

  <div class="max-w-3xl mx-auto w-full px-4 py-10 flex flex-col gap-10">

    <!-- BIG DONATE BUTTON -->
    <div class="text-center">
      <p class="text-gray-500 text-base mb-6">Helping Moncton homeless organisations get what they need most.</p>
      <a href="/donate?tab=donate"
        class="inline-flex items-center gap-3 bg-brand-500 hover:bg-brand-600 active:bg-brand-700
               text-white font-bold text-2xl px-12 py-6 rounded-3xl shadow-lg
               hover:shadow-xl transition-all duration-150 select-none">
        <span class="text-3xl">🎁</span>
        I want to donate
      </a>
      <p class="text-sm text-gray-400 mt-4">Takes about 1 minute · No account needed</p>
    </div>

    <!-- NEEDS LIST -->
    <div>
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-gray-900 flex items-center gap-2">
          <span>🚨</span> Urgent Needs
          {#if !loading && totalElements > 0}
            <span class="text-sm font-normal text-gray-400">({totalElements} open)</span>
          {/if}
        </h2>
        <a href="/donate" class="text-sm text-brand-600 font-medium hover:underline">Donate →</a>
      </div>

      {#if loading}
        <div class="space-y-4">
          {#each [1,2,3,4] as _}
            <div class="h-24 bg-gray-100 rounded-2xl animate-pulse"></div>
          {/each}
        </div>

      {:else if needs.length === 0}
        <div class="bg-white rounded-2xl p-8 text-center text-gray-400 border border-gray-100">
          <div class="text-3xl mb-2">🎉</div>
          <div class="font-medium">No urgent needs right now</div>
          <a href="/donate" class="text-sm text-brand-600 hover:underline mt-2 block">Browse all needs →</a>
        </div>

      {:else}
        <!-- Critical / High first -->
        {#if urgentNeeds.length > 0}
          <div class="space-y-3 mb-4">
            {#each urgentNeeds as need}
              <div class="bg-white rounded-2xl p-5 shadow-sm border border-gray-100
                          {need.urgency === 'CRITICAL' ? 'border-l-4 border-l-red-400' : 'border-l-4 border-l-orange-300'}">
                <div class="flex items-center justify-between gap-4">
                  <div class="min-w-0">
                    <div class="font-bold text-gray-900 text-lg leading-tight">{need.item?.name}</div>
                    <div class="text-sm text-gray-500 mt-1">{need.organization?.name}</div>
                    <div class="text-sm text-gray-600 mt-1">
                      Needs <span class="font-semibold text-brand-600">{need.quantityNeeded} {need.item?.unit}</span>
                    </div>
                  </div>
                  <div class="flex flex-col items-end gap-2 shrink-0">
                    <UrgencyBadge urgency={need.urgency} />
                    <a href="/donate?tab=donate&orgId={need.organization?.id}&itemId={need.item?.id}"
                      class="bg-brand-500 hover:bg-brand-600 text-white text-sm font-semibold
                             px-5 py-2 rounded-xl transition-colors whitespace-nowrap">
                      Donate this
                    </a>
                  </div>
                </div>
              </div>
            {/each}
          </div>
        {/if}

        <!-- Medium / Low -->
        {#if otherNeeds.length > 0}
          {#if urgentNeeds.length > 0}
            <h3 class="text-sm font-semibold text-gray-500 mb-3 mt-2">Other needs</h3>
          {/if}
          <div class="space-y-2">
            {#each otherNeeds as need}
              <div class="bg-white rounded-2xl px-5 py-4 shadow-sm border border-gray-100 flex items-center justify-between gap-4">
                <div class="min-w-0">
                  <span class="font-medium text-gray-900">{need.item?.name}</span>
                  <span class="text-xs text-gray-400 ml-2">{need.organization?.name}</span>
                  <span class="text-xs text-gray-500 ml-2">{need.quantityNeeded} {need.item?.unit}</span>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  <UrgencyBadge urgency={need.urgency} />
                  <a href="/donate?tab=donate&orgId={need.organization?.id}&itemId={need.item?.id}"
                    class="text-brand-600 hover:text-brand-700 text-sm font-medium whitespace-nowrap">
                    Donate →
                  </a>
                </div>
              </div>
            {/each}
          </div>
        {/if}

        <Pagination
          page={currentPage}
          {totalPages}
          {totalElements}
          size={PAGE_SIZE}
          onchange={(p) => { loadPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
        />
      {/if}
    </div>

  </div>

  <!-- FOOTER -->
  <footer class="mt-auto bg-white border-t border-gray-100 text-center text-xs text-gray-400 py-5">
    Built for Hack4Change 2026 · Moncton, NB
  </footer>
</div>
