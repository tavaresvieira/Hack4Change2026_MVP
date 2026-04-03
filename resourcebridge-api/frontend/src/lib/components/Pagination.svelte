<script lang="ts">
  interface Props {
    page: number;         // 0-indexed current page
    totalPages: number;
    totalElements: number;
    size: number;
    onchange: (page: number) => void;
  }

  let { page, totalPages, totalElements, size, onchange }: Props = $props();

  let from = $derived(page * size + 1);
  let to   = $derived(Math.min((page + 1) * size, totalElements));

  // Build the page number buttons: always show first, last, current ±1, with ellipsis gaps
  let pages = $derived(() => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i);
    const set = new Set([0, totalPages - 1, page, page - 1, page + 1].filter(p => p >= 0 && p < totalPages));
    return [...set].sort((a, b) => a - b);
  });
</script>

{#if totalPages > 1}
<div class="flex items-center justify-between mt-4 px-1">
  <p class="text-xs text-gray-400 dark:text-gray-500">
    {from}–{to} of {totalElements}
  </p>

  <div class="flex items-center gap-1">
    <!-- Prev -->
    <button
      onclick={() => onchange(page - 1)}
      disabled={page === 0}
      class="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 dark:hover:text-gray-200
             hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      aria-label="Previous page">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
      </svg>
    </button>

    <!-- Page numbers -->
    {#each pages() as p, i}
      {#if i > 0 && p - pages()[i - 1] > 1}
        <span class="px-1 text-gray-300 dark:text-gray-600 text-sm select-none">…</span>
      {/if}
      <button
        onclick={() => onchange(p)}
        class="min-w-[32px] h-8 px-2 rounded-lg text-sm font-medium transition-colors
               {p === page
                 ? 'bg-brand-500 text-white'
                 : 'text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 hover:text-gray-800 dark:hover:text-gray-200'}">
        {p + 1}
      </button>
    {/each}

    <!-- Next -->
    <button
      onclick={() => onchange(page + 1)}
      disabled={page >= totalPages - 1}
      class="p-1.5 rounded-lg text-gray-400 hover:text-gray-700 dark:hover:text-gray-200
             hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      aria-label="Next page">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
      </svg>
    </button>
  </div>
</div>
{/if}
