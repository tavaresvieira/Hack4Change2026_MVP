<script lang="ts">
  interface Props {
    value: string;
    class?: string;
  }
  let { value = $bindable(''), class: cls = '' }: Props = $props();

  let open = $state(false);
  let yearPicker = $state(false);           // when true, show year grid instead of calendar
  const today = new Date();
  let viewYear  = $state(value ? parseInt(value.split('-')[0]) : today.getFullYear());
  let viewMonth = $state(value ? parseInt(value.split('-')[1]) - 1 : today.getMonth());
  let yearRangeStart = $state(today.getFullYear()); // start from current year, no past

  const DAYS   = ['Su','Mo','Tu','We','Th','Fr','Sa'];
  const MONTHS = ['January','February','March','April','May','June',
                  'July','August','September','October','November','December'];

  let grid = $derived.by(() => {
    const first = new Date(viewYear, viewMonth, 1).getDay();
    const last  = new Date(viewYear, viewMonth + 1, 0).getDate();
    const cells: (number | null)[] = Array(first).fill(null);
    for (let d = 1; d <= last; d++) cells.push(d);
    return cells;
  });

  let yearGrid = $derived(Array.from({ length: 12 }, (_, i) => yearRangeStart + i));

  function select(day: number) {
    const mm = String(viewMonth + 1).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
    value = `${viewYear}-${mm}-${dd}`;
    open = false;
  }

  function selectYear(y: number) {
    viewYear = y;
    yearPicker = false;
  }

  function prev() {
    if (yearPicker) { yearRangeStart -= 12; return; }
    if (viewMonth === 0) { viewMonth = 11; viewYear--; } else viewMonth--;
  }
  function next() {
    if (yearPicker) { yearRangeStart += 12; return; }
    if (viewMonth === 11) { viewMonth = 0; viewYear++; } else viewMonth++;
  }

  function toggleYearPicker() {
    yearPicker = !yearPicker;
    if (yearPicker) yearRangeStart = today.getFullYear();
  }

  function isSelected(day: number) {
    if (!value) return false;
    const [y, m, d] = value.split('-').map(Number);
    return y === viewYear && m - 1 === viewMonth && d === day;
  }
  function isToday(day: number) {
    return today.getFullYear() === viewYear && today.getMonth() === viewMonth && today.getDate() === day;
  }
  function isPast(day: number) {
    const d = new Date(viewYear, viewMonth, day);
    d.setHours(0, 0, 0, 0);
    const t = new Date(today); t.setHours(0, 0, 0, 0);
    return d < t;
  }
  let canGoPrev = $derived(
    viewYear > today.getFullYear() ||
    (viewYear === today.getFullYear() && viewMonth > today.getMonth())
  );
</script>

<div class="relative {cls}">
  <button type="button" onclick={() => open = !open}
    class="w-full flex items-center justify-between rounded-lg border border-gray-200 dark:border-gray-600
           bg-white dark:bg-gray-700 text-xs px-2.5 py-1.5
           focus:outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500 transition-colors">
    <span class="{value ? 'text-gray-700 dark:text-gray-100' : 'text-gray-400 dark:text-gray-500'}">{value || 'Select date'}</span>
    <svg class="w-3.5 h-3.5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
    </svg>
  </button>

  {#if open}
    <!-- backdrop -->
    <button type="button" class="fixed inset-0 z-40" onclick={() => open = false} tabindex="-1" aria-hidden="true"></button>

    <!-- calendar — opens above -->
    <div class="absolute bottom-full left-0 mb-1.5 bg-white dark:bg-gray-800
                border border-gray-200 dark:border-gray-600 shadow-xl rounded-xl p-3 z-50 w-64">

      <!-- header: prev / month+year label / next -->
      <div class="flex items-center justify-between mb-2">
        <button type="button" onclick={prev} disabled={!yearPicker && !canGoPrev}
          class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
        </button>

        <button type="button" onclick={toggleYearPicker}
          class="flex items-center gap-1 text-xs font-semibold text-gray-700 dark:text-gray-200
                 hover:text-brand-600 dark:hover:text-brand-400 transition-colors rounded px-1.5 py-0.5
                 hover:bg-gray-100 dark:hover:bg-gray-700">
          {#if yearPicker}
            {yearRangeStart}–{yearRangeStart + 11}
          {:else}
            {MONTHS[viewMonth]} {viewYear}
          {/if}
          <svg class="w-3 h-3 transition-transform {yearPicker ? 'rotate-180' : ''}" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
          </svg>
        </button>

        <button type="button" onclick={next}
          class="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
      </div>

      {#if yearPicker}
        <!-- year grid -->
        <div class="grid grid-cols-4 gap-1">
          {#each yearGrid as y}
            <button type="button" onclick={() => selectYear(y)}
              class="py-1.5 rounded-lg text-xs font-medium transition-colors
                {y === viewYear
                  ? 'bg-brand-500 text-white'
                  : y === today.getFullYear()
                    ? 'border border-brand-400 text-brand-600 dark:text-brand-400 hover:bg-brand-50 dark:hover:bg-brand-900/20'
                    : 'text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700'}">
              {y}
            </button>
          {/each}
        </div>
      {:else}
        <!-- day names -->
        <div class="grid grid-cols-7 mb-1">
          {#each DAYS as d}
            <div class="text-center text-xs text-gray-400 dark:text-gray-500 font-medium py-0.5">{d}</div>
          {/each}
        </div>

        <!-- day cells -->
        <div class="grid grid-cols-7 gap-y-0.5">
          {#each grid as day}
            {#if day === null}
              <div></div>
            {:else}
              <button type="button" onclick={() => !isPast(day) && select(day)} disabled={isPast(day)}
                class="aspect-square flex items-center justify-center text-xs rounded-full transition-colors
                  {isPast(day)
                    ? 'text-gray-300 dark:text-gray-600 cursor-not-allowed'
                    : isSelected(day)
                      ? 'bg-brand-500 text-white font-semibold'
                      : isToday(day)
                        ? 'border border-brand-400 text-brand-600 dark:text-brand-400 font-medium hover:bg-brand-50 dark:hover:bg-brand-900/20'
                        : 'text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700'}">
                {day}
              </button>
            {/if}
          {/each}
        </div>
      {/if}
    </div>
  {/if}
</div>
