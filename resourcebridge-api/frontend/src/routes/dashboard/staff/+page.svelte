<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { auth } from '$lib/stores/auth';
  import { getByOrg as getInventory, getExpiring, createInventory, updateInventory, deleteInventory } from '$lib/api/inventory';
  import { getByOrg as getOrgNeeds, createNeed, updateNeed, fulfillNeed } from '$lib/api/needs';
  import { getByOrg as getTransfers, updateStatus as updateTransferStatus, deleteTransfer } from '$lib/api/transfers';
  import { getByOrg as getAnnouncements, createAnnouncement, deleteAnnouncement } from '$lib/api/announcements';
  import { getCommunityAnnouncements, createExchangeRequest, getIncomingRequests, getOutgoingRequests, updateRequestStatus } from '$lib/api/exchange';
  import { getAll as getItems } from '$lib/api/items';
  import { showToast } from '$lib/stores/toast';
  import StatusBadge from '$lib/components/StatusBadge.svelte';
  import UrgencyBadge from '$lib/components/UrgencyBadge.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import Pagination from '$lib/components/Pagination.svelte';
  import DatePicker from '$lib/components/DatePicker.svelte';
  import type { Inventory, Need, Item, ItemCategory } from '$lib/types';

  const INV_CATEGORIES: { value: ItemCategory | ''; label: string; emoji: string }[] = [
    { value: '',         label: 'All',      emoji: '📦' },
    { value: 'FOOD',     label: 'Food',     emoji: '🍎' },
    { value: 'CLOTHING', label: 'Clothing', emoji: '👕' },
    { value: 'HYGIENE',  label: 'Hygiene',  emoji: '🧴' },
    { value: 'BEDDING',  label: 'Bedding',  emoji: '🛏️' },
    { value: 'OTHER',    label: 'Other',    emoji: '📁' },
  ];

  let invCategoryFilter = $state<ItemCategory | ''>('');

  let inventory = $state<Inventory[]>([]);
  let expiring = $state<Inventory[]>([]);
  let needs = $state<Need[]>([]);
  let transfers = $state<any[]>([]);
  let announcements = $state<any[]>([]);
  let items = $state<Item[]>([]);
  let loading = $state(true);

  // New Need form
  let needItemId = $state<number | null>(null);
  let needQty = $state(1);
  let needUrgency = $state('MEDIUM');
  let needSubmitting = $state(false);

  // New Inventory form
  let invItemId = $state<number | null>(null);
  let invQty = $state(1);
  let invExpiry = $state('');
  let invSubmitting = $state(false);

  // New Announcement form
  let annType = $state('SURPLUS');
  let annItemId = $state<number | null>(null);
  let annQty = $state(1);
  let annMessage = $state('');
  let annExpiryDate = $state('');
  let annSubmitting = $state(false);

  onMount(async () => {
    const token = $auth.token!;
    const orgId = $auth.organizationId!;
    try {
      const [inv, exp, n, t, a, i] = await Promise.all([
        getInventory(orgId, token),
        getExpiring(orgId, 7, token),
        getOrgNeeds(orgId, token),
        getTransfers(orgId, token),
        getAnnouncements(orgId, token),
        getItems()
      ]);
      inventory = inv;
      expiring = exp;
      needs = n;
      transfers = t;
      announcements = a;
      items = i;
      if (i.length > 0) {
        needItemId = i[0].id;
        invItemId = i[0].id;
        annItemId = i[0].id;
      }
      // Community board — uses public /announcements endpoint, always works
      try {
        const community = await getCommunityAnnouncements();
        communityAnnouncements = community.filter(ann => ann.organization?.id !== Number(orgId));
      } catch { /* non-critical */ }

      // Exchange requests — require exchange_requests table (backend must be restarted after deploy)
      try {
        const [incoming, outgoing] = await Promise.all([
          getIncomingRequests(orgId, token),
          getOutgoingRequests(orgId, token)
        ]);
        incomingRequests = incoming;
        outgoingRequests = outgoing;
      } catch { /* non-critical — table may not exist yet */ }
    } catch (e: any) {
      showToast('Failed to load data', 'error');
    } finally {
      loading = false;
    }
  });

  async function submitNeed() {
    if (!needItemId) return;
    needSubmitting = true;
    try {
      const created = await createNeed({
        organization: { id: $auth.organizationId },
        item: { id: needItemId },
        quantityNeeded: Number(needQty),
        urgency: needUrgency
      }, $auth.token!);
      const fullItem = items.find(i => i.id === needItemId);
      needs = [...needs, { ...created, item: fullItem ?? created.item }];
      showToast('Need posted successfully!');
      needQty = 1; needUrgency = 'MEDIUM';
    } catch (e: any) {
      showToast(e.message || 'Failed to post need', 'error');
    } finally {
      needSubmitting = false;
    }
  }

  async function submitInventory() {
    if (!invItemId) return;
    invSubmitting = true;
    try {
      const fullItem = items.find(i => i.id === invItemId);
      const isFood = fullItem?.expiryRelevant ?? false;
      // For non-food items: merge into existing entry if one exists
      const existing = !isFood ? inventory.find(i => i.item?.id === invItemId) : null;

      if (existing) {
        const newQty = existing.quantity + Number(invQty);
        const updated = await updateInventory(existing.id, { quantity: newQty }, $auth.token!);
        inventory = inventory.map(i => i.id === existing.id ? { ...i, quantity: updated.quantity } : i);
        showToast(`Added ${invQty} — ${fullItem?.name ?? 'item'} now at ${updated.quantity} ${fullItem?.unit ?? ''}`);
      } else {
        const body: any = {
          organization: { id: $auth.organizationId },
          item: { id: invItemId },
          quantity: Number(invQty)
        };
        if (invExpiry) body.expiryDate = invExpiry;
        const created = await createInventory(body, $auth.token!);
        inventory = [...inventory, { ...created, item: fullItem ?? created.item }];
        showToast('Inventory updated!');
      }
      invQty = 1; invExpiry = '';
    } catch (e: any) {
      showToast(e.message || 'Failed to update inventory', 'error');
    } finally {
      invSubmitting = false;
    }
  }

  async function submitAnnouncement() {
    if (!annItemId) return;
    annSubmitting = true;
    try {
      const body: any = {
        organization: { id: $auth.organizationId },
        item: { id: annItemId },
        quantity: Number(annQty),
        type: annType,
        message: annMessage
      };
      if (annExpiryDate) body.expiryDate = annExpiryDate;
      const created = await createAnnouncement(body, $auth.token!);
      const fullItem = items.find(i => i.id === annItemId);
      announcements = [{ ...created, item: fullItem ?? created.item }, ...announcements];
      showToast('Announcement posted!');
      annMessage = ''; annQty = 1; annExpiryDate = '';
    } catch (e: any) {
      showToast(e.message || 'Failed to post announcement', 'error');
    } finally {
      annSubmitting = false;
    }
  }

  async function markNeedFulfilled(id: number) {
    try {
      await fulfillNeed(id, $auth.token!);
      needs = needs.map(n => n.id === id ? { ...n, fulfilled: true } : n);
      showToast('Need marked as fulfilled');
    } catch {
      showToast('Failed to update need', 'error');
    }
  }

  let transferPendingDelete = $state<number | null>(null);
  let invPendingDelete = $state<number | null>(null);
  let invEditingId = $state<number | null>(null);
  let invEditQty = $state(1);
  let annPendingDelete = $state<number | null>(null);

  const PAGE_SIZE = 10;
  let invPage = $state(0);
  let needsPage = $state(0);
  let transfersPage = $state(0);
  let annPage = $state(0);

  let invSearch = $state('');
  let needsSearch = $state('');
  let transfersSearch = $state('');
  let annSearch = $state('');

  // Community board & exchanges
  let communityAnnouncements = $state<any[]>([]);
  let incomingRequests = $state<any[]>([]);
  let outgoingRequests = $state<any[]>([]);
  let respondingId = $state<number | null>(null); // announcement being responded to

  $effect(() => { invCategoryFilter; invSearch; invPage = 0; });
  $effect(() => { needsSearch; needsPage = 0; });
  $effect(() => { transfersSearch; transfersPage = 0; });
  $effect(() => { annSearch; annPage = 0; });

  async function saveInventoryEdit(id: number) {
    try {
      const updated = await updateInventory(id, { quantity: Number(invEditQty) }, $auth.token!);
      inventory = inventory.map(i => i.id === id ? { ...i, quantity: updated.quantity } : i);
      invEditingId = null;
      showToast('Quantity updated');
    } catch {
      showToast('Failed to update quantity', 'error');
    }
  }

  async function confirmDeleteInventory(id: number) {
    try {
      await deleteInventory(id, $auth.token!);
      inventory = inventory.filter(i => i.id !== id);
      expiring = expiring.filter(i => i.id !== id);
      invPendingDelete = null;
      showToast('Item removed from inventory');
    } catch {
      showToast('Failed to delete inventory item', 'error');
    }
  }

  async function confirmDeleteAnnouncement(id: number) {
    try {
      await deleteAnnouncement(id, $auth.token!);
      announcements = announcements.filter(a => a.id !== id);
      annPendingDelete = null;
      showToast('Announcement deleted');
    } catch {
      showToast('Failed to delete announcement', 'error');
    }
  }

  async function markTransferReceived(t: any) {
    try {
      await updateTransferStatus(t.id, 'COMPLETED', $auth.token!);
      await deleteTransfer(t.id, $auth.token!);
      transfers = transfers.filter(tr => tr.id !== t.id);

      const fullItem = items.find(i => i.id === t.donation.item.id);
      const isFood = fullItem?.expiryRelevant ?? false;
      const existing = !isFood ? inventory.find(i => i.item?.id === t.donation.item.id) : null;

      if (existing) {
        const newQty = existing.quantity + t.quantityAssigned;
        const updated = await updateInventory(existing.id, { quantity: newQty }, $auth.token!);
        inventory = inventory.map(i => i.id === existing.id ? { ...i, quantity: updated.quantity } : i);
      } else {
        const newInv = await createInventory({
          organization: { id: $auth.organizationId },
          item: { id: t.donation.item.id },
          quantity: t.quantityAssigned,
          ...(t.donation.expiryDate ? { expiryDate: t.donation.expiryDate } : {})
        }, $auth.token!);
        inventory = [...inventory, { ...newInv, item: fullItem ?? newInv.item }];
      }

      // Update matching open needs for this item
      const matchingNeed = needs.find(n => !n.fulfilled && n.item?.id === t.donation.item.id);
      if (matchingNeed) {
        const remaining = matchingNeed.quantityNeeded - t.quantityAssigned;
        if (remaining <= 0) {
          await fulfillNeed(matchingNeed.id, $auth.token!);
          needs = needs.map(n => n.id === matchingNeed.id ? { ...n, fulfilled: true } : n);
          showToast('Received! Need fully fulfilled ✓');
        } else {
          const updated = await updateNeed(matchingNeed.id, {
            ...matchingNeed,
            organization: { id: matchingNeed.organization.id },
            item: { id: matchingNeed.item.id },
            quantityNeeded: remaining
          }, $auth.token!);
          needs = needs.map(n => n.id === matchingNeed.id ? { ...n, quantityNeeded: updated.quantityNeeded } : n);
          showToast(`Received! ${remaining} ${fullItem?.unit ?? ''} still needed.`);
        }
      } else {
        showToast('Received! Added to inventory.');
      }
    } catch {
      showToast('Failed to receive transfer', 'error');
    }
  }

  async function confirmDeleteTransfer(id: number) {
    try {
      await deleteTransfer(id, $auth.token!);
      transfers = transfers.filter(t => t.id !== id);
      transferPendingDelete = null;
      showToast('Transfer deleted');
    } catch {
      showToast('Failed to delete transfer', 'error');
    }
  }

  function daysUntilExpiry(dateStr: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiry = new Date(dateStr + 'T00:00:00');
    return Math.round((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  function expiryLabel(days: number): string {
    if (days < 0) return `expired ${Math.abs(days)} day${Math.abs(days) !== 1 ? 's' : ''} ago`;
    if (days === 0) return 'expires today';
    if (days === 1) return 'expires tomorrow';
    return `expires in ${days} days`;
  }

  function expiryColor(days: number): string {
    if (days <= 0) return 'text-red-600 dark:text-red-400 font-semibold';
    if (days <= 2) return 'text-orange-600 dark:text-orange-400 font-medium';
    return 'text-amber-500 dark:text-amber-400';
  }

  async function respondToAnnouncement(announcementId: number) {
    respondingId = announcementId;
    try {
      const req = await createExchangeRequest(announcementId, $auth.organizationId!, $auth.token!);
      outgoingRequests = [...outgoingRequests, req];
      showToast('Request sent! The shelter will be notified.');
    } catch (e: any) {
      if (e.message?.includes('409') || e.status === 409) {
        showToast('You already sent a request for this announcement', 'error');
      } else {
        showToast(e.message || 'Failed to send request', 'error');
      }
    } finally {
      respondingId = null;
    }
  }

  async function handleExchangeStatus(id: number, status: 'ACCEPTED' | 'REJECTED') {
    try {
      const updated = await updateRequestStatus(id, status, $auth.token!);
      incomingRequests = incomingRequests.map(r => r.id === id ? updated : r);
      showToast(status === 'ACCEPTED' ? 'Request accepted!' : 'Request declined');
    } catch {
      showToast('Failed to update request', 'error');
    }
  }

  let activeSection = $derived($page.url.hash.replace('#', '') || 'dashboard');
  let openNeeds = $derived.by(() => {
    const q = needsSearch.toLowerCase();
    const list = needs.filter(n => !n.fulfilled);
    return q ? list.filter(n => n.item?.name?.toLowerCase().includes(q)) : list;
  });
  let filteredTransfers = $derived.by(() => {
    const q = transfersSearch.toLowerCase();
    return q ? transfers.filter(t => t.donation?.item?.name?.toLowerCase().includes(q)) : transfers;
  });
  let filteredAnnouncements = $derived.by(() => {
    const q = annSearch.toLowerCase();
    return q ? announcements.filter(a => a.item?.name?.toLowerCase().includes(q) || a.message?.toLowerCase().includes(q)) : announcements;
  });
  let invItemIsFood = $derived(items.find(i => i.id === invItemId)?.expiryRelevant ?? false);
  let annItemIsFood = $derived(items.find(i => i.id === annItemId)?.expiryRelevant ?? false);
  let filteredInventory = $derived.by(() => {
    const q = invSearch.toLowerCase();
    let list = invCategoryFilter
      ? inventory.filter(inv => inv.item?.category === invCategoryFilter)
      : inventory;
    if (q) list = list.filter(inv => inv.item?.name?.toLowerCase().includes(q));
    // expiring items first, then no-expiry items
    return [...list].sort((a, b) => {
      if (a.expiryDate && b.expiryDate) return daysUntilExpiry(a.expiryDate) - daysUntilExpiry(b.expiryDate);
      if (a.expiryDate) return -1;
      if (b.expiryDate) return 1;
      return 0;
    });
  });
  // Items already expired (not covered by 7-day backend window)
  let alreadyExpired = $derived(
    inventory.filter(inv => inv.expiryDate && daysUntilExpiry(inv.expiryDate) < 0)
  );
  // Combined alert list: expired first, then soon-expiring (deduplicated)
  let expiryAlertItems = $derived([
    ...alreadyExpired,
    ...expiring.filter(e => !alreadyExpired.find(x => x.id === e.id))
  ].sort((a, b) => daysUntilExpiry(a.expiryDate!) - daysUntilExpiry(b.expiryDate!)));
</script>

<svelte:head><title>Staff Dashboard — ResourceBridge</title></svelte:head>

<div class="space-y-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Staff Dashboard</h1>
    <p class="text-sm text-gray-500 dark:text-gray-400 mt-0.5">Welcome, {$auth.name}
      {#if $auth.organizationName}
        · <span class="font-medium text-gray-700 dark:text-gray-300">{$auth.organizationName}</span>
      {/if}
    </p>
  </div>

  <!-- EXPIRY ALERT — always visible at the top when items are expiring -->
  {#if expiryAlertItems.length > 0}
    {@const expiredCount = alreadyExpired.length}
    <div class="bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800/50 rounded-xl p-4">
      <div class="flex items-center gap-2 mb-3">
        {#if expiredCount > 0}
          <span class="text-red-600 dark:text-red-400 font-semibold text-sm">🚨 {expiredCount} item{expiredCount !== 1 ? 's' : ''} expired · {expiryAlertItems.length - expiredCount} expiring soon</span>
        {:else}
          <span class="text-red-600 dark:text-red-400 font-semibold text-sm">⚠️ {expiryAlertItems.length} item{expiryAlertItems.length !== 1 ? 's' : ''} expiring within 7 days</span>
        {/if}
      </div>
      <div class="space-y-2">
        {#each expiryAlertItems as inv}
          {@const days = daysUntilExpiry(inv.expiryDate!)}
          <div class="flex items-center justify-between bg-white dark:bg-gray-700 rounded-lg px-3 py-2 text-sm">
            <span class="font-medium text-gray-800 dark:text-gray-100">{inv.item?.name}</span>
            <span class="text-gray-500 dark:text-gray-400">{inv.quantity} {inv.item?.unit}</span>
            <span class="{expiryColor(days)} text-xs capitalize">{expiryLabel(days)}</span>
          </div>
        {/each}
      </div>
    </div>
  {/if}

  <!-- INVENTORY -->
  {#if activeSection === 'dashboard' || activeSection === 'inventory'}
  <div class="{activeSection === 'dashboard' ? 'grid lg:grid-cols-2 gap-6' : ''}">
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
        <h2 class="font-semibold text-gray-900 dark:text-white">📦 Inventory</h2>
        <span class="text-xs text-gray-400">{filteredInventory.length} / {inventory.length} items</span>
      </div>
      {#if activeSection === 'inventory'}
      <div class="px-4 pt-3 pb-2 border-b border-gray-50 dark:border-gray-700">
        <input bind:value={invSearch} type="search" placeholder="Search inventory…"
          class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
      </div>
      {/if}
      <div class="px-4 pt-3 pb-2 flex gap-1.5 flex-wrap border-b border-gray-50 dark:border-gray-700">
        {#each INV_CATEGORIES as cat}
          {@const count = cat.value === '' ? inventory.length : inventory.filter(i => i.item?.category === cat.value).length}
          {#if count > 0 || cat.value === ''}
            <button
              onclick={() => invCategoryFilter = cat.value}
              class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border transition-all
                {invCategoryFilter === cat.value
                  ? 'bg-brand-500 text-white border-brand-500'
                  : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300 border-gray-200 dark:border-gray-600 hover:border-brand-300'}">
              {cat.emoji} {cat.label}
              {#if count > 0}<span class="opacity-70">{count}</span>{/if}
            </button>
          {/if}
        {/each}
      </div>
      <div class="divide-y divide-gray-50 dark:divide-gray-700 {activeSection !== 'inventory' ? 'max-h-48 overflow-y-auto' : ''}">
        {#if loading}
          <div class="p-4 space-y-2">{#each [1,2,3] as _}<div class="h-10 bg-gray-50 dark:bg-gray-700 animate-pulse rounded"></div>{/each}</div>
        {:else if filteredInventory.length === 0}
          {@const activeCat = INV_CATEGORIES.find(c => c.value === invCategoryFilter)}
          <EmptyState message={invCategoryFilter ? `No ${activeCat?.label} items yet` : 'No inventory yet'} icon={activeCat?.emoji ?? '📦'} />
        {:else}
          {#each (activeSection === 'inventory' ? filteredInventory.slice(invPage * PAGE_SIZE, (invPage + 1) * PAGE_SIZE) : filteredInventory) as inv}
            {@const cat = INV_CATEGORIES.find(c => c.value === inv.item?.category)}
            <div class="px-4 py-3 text-sm">
              <div class="flex items-center justify-between">
                <div>
                  <span class="font-medium text-gray-800 dark:text-gray-100">{inv.item?.name}</span>
                  {#if cat?.value}<span class="ml-1.5 text-xs text-gray-400">{cat.emoji}</span>{/if}
                </div>
                <div class="flex items-center gap-2">
                  {#if invEditingId === inv.id}
                    <input type="number" bind:value={invEditQty} min="1"
                      class="w-16 rounded border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs py-0.5 focus:border-brand-500 focus:ring-brand-500" />
                    <span class="text-gray-400 text-xs">{inv.item?.unit}</span>
                    <button onclick={() => saveInventoryEdit(inv.id)} class="text-xs text-brand-600 dark:text-brand-400 font-medium hover:underline">Save</button>
                    <button onclick={() => invEditingId = null} class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" title="Cancel">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                    </button>
                  {:else}
                    <span class="text-gray-600 dark:text-gray-300 font-medium">{inv.quantity} <span class="text-gray-400 font-normal">{inv.item?.unit}</span></span>
                    {#if inv.expiryDate}
                      {@const days = daysUntilExpiry(inv.expiryDate)}
                      <span class="text-xs {expiryColor(days)}" title={inv.expiryDate}>{expiryLabel(days)}</span>
                    {/if}
                    <button onclick={() => { invEditingId = inv.id; invEditQty = inv.quantity; invPendingDelete = null; }}
                      class="text-gray-400 hover:text-brand-500 dark:hover:text-brand-400 transition-colors" title="Edit quantity">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
                    </button>
                    <button onclick={() => { invPendingDelete = inv.id; invEditingId = null; }}
                      class="text-gray-400 hover:text-red-500 transition-colors" title="Remove">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                    </button>
                  {/if}
                </div>
              </div>
              {#if invPendingDelete === inv.id}
                <div class="mt-2 flex items-center gap-2 bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800/50 rounded-lg px-3 py-2">
                  <span class="text-xs text-red-700 dark:text-red-300 flex-1">Remove this item from inventory?</span>
                  <button onclick={() => confirmDeleteInventory(inv.id)} class="text-xs bg-red-500 text-white px-2.5 py-1 rounded-md font-medium hover:bg-red-600">Yes, delete</button>
                  <button onclick={() => invPendingDelete = null} class="text-xs text-gray-500 dark:text-gray-400 hover:underline">Cancel</button>
                </div>
              {/if}
            </div>
          {/each}
        {/if}
      </div>
      {#if activeSection === 'inventory' && filteredInventory.length > PAGE_SIZE}
        <div class="px-4 pb-2">
          <Pagination page={invPage} totalPages={Math.ceil(filteredInventory.length / PAGE_SIZE)} totalElements={filteredInventory.length} size={PAGE_SIZE} onchange={(p) => invPage = p} />
        </div>
      {/if}
      <div class="p-4 border-t border-gray-50 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-700/30 space-y-2">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400">Add to inventory</div>
        <div class="flex gap-2">
          <select bind:value={invItemId} class="flex-1 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
            {#each items as item}<option value={item.id}>{item.name}</option>{/each}
          </select>
          <input type="number" bind:value={invQty} min="1" class="w-16 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" placeholder="Qty" />
          <button onclick={submitInventory} disabled={invSubmitting} class="bg-brand-500 text-white text-xs px-3 py-1.5 rounded-lg font-medium hover:bg-brand-600 transition-colors disabled:opacity-60">
            {invSubmitting ? '…' : 'Add'}
          </button>
        </div>
        {#if invItemIsFood}
          <div class="flex items-center gap-2">
            <label class="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">Expiry date</label>
            <DatePicker bind:value={invExpiry} class="flex-1" />
          </div>
        {/if}
      </div>
    </div>

    <!-- NEEDS (shown beside inventory on dashboard) -->
    {#if activeSection === 'dashboard'}
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
        <h2 class="font-semibold text-gray-900 dark:text-white">📋 Our Needs</h2>
        <span class="text-xs text-gray-400">{openNeeds.length} open</span>
      </div>
      <div class="divide-y divide-gray-50 dark:divide-gray-700 max-h-48 overflow-y-auto">
        {#if loading}
          <div class="p-4 space-y-2">{#each [1,2,3] as _}<div class="h-10 bg-gray-50 dark:bg-gray-700 animate-pulse rounded"></div>{/each}</div>
        {:else if openNeeds.length === 0}
          <EmptyState message="No open needs" icon="✅" />
        {:else}
          {#each openNeeds as need}
            <div class="flex items-center justify-between px-4 py-3 text-sm">
              <div>
                <span class="font-medium text-gray-800 dark:text-gray-100">{need.item?.name}</span>
                <span class="text-gray-400 text-xs ml-2">{need.quantityNeeded} {need.item?.unit}</span>
              </div>
              <div class="flex items-center gap-2">
                <UrgencyBadge urgency={need.urgency} />
                <button onclick={() => markNeedFulfilled(need.id)} class="text-xs text-green-600 hover:underline">Fulfilled</button>
              </div>
            </div>
          {/each}
        {/if}
      </div>
      <div class="p-4 border-t border-gray-50 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-700/30">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Post a need</div>
        <div class="flex gap-2 flex-wrap">
          <select bind:value={needItemId} class="flex-1 min-w-0 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
            {#each items as item}<option value={item.id}>{item.name}</option>{/each}
          </select>
          <input type="number" bind:value={needQty} min="1" class="w-14 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
          <select bind:value={needUrgency} class="rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
            {#each ['LOW','MEDIUM','HIGH','CRITICAL'] as u}<option>{u}</option>{/each}
          </select>
          <button onclick={submitNeed} disabled={needSubmitting} class="bg-brand-500 text-white text-xs px-3 py-1.5 rounded-lg font-medium hover:bg-brand-600 transition-colors disabled:opacity-60">
            {needSubmitting ? '…' : 'Post'}
          </button>
        </div>
      </div>
    </div>
    {/if}
  </div>
  {/if}

  <!-- NEEDS (full width when selected directly) -->
  {#if activeSection === 'needs'}
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
    <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
      <h2 class="font-semibold text-gray-900 dark:text-white">📋 Our Needs</h2>
      <span class="text-xs text-gray-400">{openNeeds.length} open</span>
    </div>
    <div class="px-4 pt-3 pb-2 border-b border-gray-50 dark:border-gray-700">
      <input bind:value={needsSearch} type="search" placeholder="Search needs…"
        class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
    </div>
    <div class="divide-y divide-gray-50 dark:divide-gray-700">
      {#if loading}
        <div class="p-4 space-y-2">{#each [1,2,3] as _}<div class="h-10 bg-gray-50 dark:bg-gray-700 animate-pulse rounded"></div>{/each}</div>
      {:else if openNeeds.length === 0}
        <EmptyState message="No open needs" icon="✅" />
      {:else}
        {#each openNeeds.slice(needsPage * PAGE_SIZE, (needsPage + 1) * PAGE_SIZE) as need}
          <div class="flex items-center justify-between px-4 py-3 text-sm">
            <div>
              <span class="font-medium text-gray-800 dark:text-gray-100">{need.item?.name}</span>
              <span class="text-gray-400 text-xs ml-2">{need.quantityNeeded} {need.item?.unit}</span>
            </div>
            <div class="flex items-center gap-2">
              <UrgencyBadge urgency={need.urgency} />
              <button onclick={() => markNeedFulfilled(need.id)} class="text-xs text-green-600 hover:underline">Fulfilled</button>
            </div>
          </div>
        {/each}
      {/if}
    </div>
    {#if openNeeds.length > PAGE_SIZE}
      <div class="px-4 pb-2">
        <Pagination page={needsPage} totalPages={Math.ceil(openNeeds.length / PAGE_SIZE)} totalElements={openNeeds.length} size={PAGE_SIZE} onchange={(p) => needsPage = p} />
      </div>
    {/if}
    <div class="p-4 border-t border-gray-50 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-700/30">
      <div class="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">Post a need</div>
      <div class="flex gap-2 flex-wrap">
        <select bind:value={needItemId} class="flex-1 min-w-0 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
          {#each items as item}<option value={item.id}>{item.name}</option>{/each}
        </select>
        <input type="number" bind:value={needQty} min="1" class="w-14 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
        <select bind:value={needUrgency} class="rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
          {#each ['LOW','MEDIUM','HIGH','CRITICAL'] as u}<option>{u}</option>{/each}
        </select>
        <button onclick={submitNeed} disabled={needSubmitting} class="bg-brand-500 text-white text-xs px-3 py-1.5 rounded-lg font-medium hover:bg-brand-600 transition-colors disabled:opacity-60">
          {needSubmitting ? '…' : 'Post'}
        </button>
      </div>
    </div>
  </div>
  {/if}

  <!-- TRANSFERS -->
  {#if activeSection === 'dashboard' || activeSection === 'transfers'}
  <div class="{activeSection === 'dashboard' ? 'grid lg:grid-cols-2 gap-6' : ''}">
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
        <h2 class="font-semibold text-gray-900 dark:text-white">🚚 Incoming Transfers</h2>
        <span class="text-xs text-gray-400">{filteredTransfers.length} total</span>
      </div>
      {#if activeSection === 'transfers'}
      <div class="px-4 pt-3 pb-2 border-b border-gray-50 dark:border-gray-700">
        <input bind:value={transfersSearch} type="search" placeholder="Search transfers…"
          class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
      </div>
      {/if}
      {#if loading}
        <div class="p-4 space-y-2">{#each [1,2] as _}<div class="h-12 bg-gray-50 dark:bg-gray-700 animate-pulse rounded"></div>{/each}</div>
      {:else if filteredTransfers.length === 0}
        <EmptyState message="No incoming transfers" icon="📭" />
      {:else}
        <div class="divide-y divide-gray-50 dark:divide-gray-700">
          {#each (activeSection === 'transfers' ? filteredTransfers.slice(transfersPage * PAGE_SIZE, (transfersPage + 1) * PAGE_SIZE) : filteredTransfers) as t}
            <div class="px-4 py-3 text-sm">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="font-medium text-gray-800 dark:text-gray-100">{t.donation?.item?.name}</span>
                  <span class="text-xs text-gray-400">{t.quantityAssigned} {t.donation?.item?.unit}</span>
                  {#if t.donation?.donationType === 'PICKUP_REQUEST'}
                    <span class="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded-full text-xs font-medium">🚐 Pickup</span>
                  {:else}
                    <span class="inline-flex items-center gap-1 px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded-full text-xs font-medium">🏠 Drop-off</span>
                  {/if}
                </div>
                <div class="flex items-center gap-2">
                  <StatusBadge status={t.status} />
                  {#if t.status !== 'COMPLETED'}
                    <button onclick={() => markTransferReceived(t)} class="text-xs text-green-600 hover:underline font-medium">Received</button>
                  {/if}
                  <button onclick={() => transferPendingDelete = t.id} class="text-xs text-red-400 hover:underline">Delete</button>
                </div>
              </div>
              {#if transferPendingDelete === t.id}
                <div class="mt-2 flex items-center gap-2 bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800/50 rounded-lg px-3 py-2">
                  <span class="text-xs text-red-700 dark:text-red-300 flex-1">Are you sure you want to delete this transfer?</span>
                  <button onclick={() => confirmDeleteTransfer(t.id)} class="text-xs bg-red-500 text-white px-2.5 py-1 rounded-md font-medium hover:bg-red-600">Yes, delete</button>
                  <button onclick={() => transferPendingDelete = null} class="text-xs text-gray-500 dark:text-gray-400 hover:underline">Cancel</button>
                </div>
              {/if}
              {#if t.donation?.donationType === 'PICKUP_REQUEST' && t.donation?.pickupAddress}
                <div class="mt-2 flex items-start gap-1.5 bg-blue-50 dark:bg-blue-900/20 rounded-lg px-3 py-2">
                  <span class="text-blue-400 text-xs mt-0.5">📍</span>
                  <div>
                    <div class="text-xs font-medium text-blue-800 dark:text-blue-200">{t.donation.pickupAddress}, {t.donation.pickupCity}</div>
                    <div class="text-xs text-blue-500 dark:text-blue-400">
                      {t.donation.donorName}
                      {#if t.donation.donorPhone}· <a href="tel:{t.donation.donorPhone}" class="hover:underline font-medium">📞 {t.donation.donorPhone}</a>{/if}
                      · <a href="mailto:{t.donation.donorEmail}" class="hover:underline">{t.donation.donorEmail}</a>
                    </div>
                  </div>
                </div>
              {/if}
            </div>
          {/each}
        </div>
      {/if}
      {#if activeSection === 'transfers' && filteredTransfers.length > PAGE_SIZE}
        <div class="px-4 pb-2">
          <Pagination page={transfersPage} totalPages={Math.ceil(filteredTransfers.length / PAGE_SIZE)} totalElements={filteredTransfers.length} size={PAGE_SIZE} onchange={(p) => transfersPage = p} />
        </div>
      {/if}
    </div>

    <!-- ANNOUNCEMENTS (shown beside transfers on dashboard) -->
    {#if activeSection === 'dashboard'}
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700">
        <h2 class="font-semibold text-gray-900 dark:text-white dark:text-white">📢 Announcements</h2>
      </div>
      <div class="divide-y divide-gray-50 dark:divide-gray-700 max-h-40 overflow-y-auto">
        {#if announcements.length === 0}
          <EmptyState message="No announcements yet" icon="📢" />
        {:else}
          {#each announcements as ann}
            <div class="px-4 py-3 text-sm">
              <div class="flex items-center justify-between">
                <div>
                  <span class="text-gray-700 dark:text-gray-300 text-xs font-medium">{ann.item?.name}</span>
                  <span class="text-gray-400 text-xs ml-1.5">· {ann.quantity} {ann.item?.unit}</span>
                  {#if ann.expiryDate}<span class="text-orange-500 text-xs ml-1.5">exp {ann.expiryDate}</span>{/if}
                  {#if ann.message}<div class="text-gray-500 dark:text-gray-400 text-xs mt-0.5">{ann.message}</div>{/if}
                </div>
                <div class="flex items-center gap-2">
                  <StatusBadge status={ann.type} />
                  <button onclick={() => annPendingDelete = ann.id}
                    class="text-gray-400 hover:text-red-500 transition-colors" title="Delete">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
                  </button>
                </div>
              </div>
              {#if annPendingDelete === ann.id}
                <div class="mt-2 flex items-center gap-2 bg-red-50 dark:bg-red-900/20 border border-red-100 dark:border-red-800/50 rounded-lg px-3 py-2">
                  <span class="text-xs text-red-700 dark:text-red-300 flex-1">Delete this announcement?</span>
                  <button onclick={() => confirmDeleteAnnouncement(ann.id)} class="text-xs bg-red-500 text-white px-2.5 py-1 rounded-md font-medium hover:bg-red-600">Yes, delete</button>
                  <button onclick={() => annPendingDelete = null} class="text-xs text-gray-500 dark:text-gray-400 hover:underline">Cancel</button>
                </div>
              {/if}
            </div>
          {/each}
        {/if}
      </div>
      <div class="p-4 border-t border-gray-50 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-700/30 space-y-2">
        <div class="text-xs font-medium text-gray-500 dark:text-gray-400">Post announcement</div>
        <div class="flex gap-2 flex-wrap">
          <select bind:value={annType} class="rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
            {#each ['SURPLUS','EXPIRY','URGENT'] as t}<option>{t}</option>{/each}
          </select>
          <select bind:value={annItemId} class="flex-1 min-w-0 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
            {#each items as item}<option value={item.id}>{item.name}</option>{/each}
          </select>
          <input type="number" bind:value={annQty} min="1" class="w-14 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs" />
        </div>
        {#if annItemIsFood}
          <div class="flex items-center gap-2">
            <label class="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">Expiry date</label>
            <input type="date" bind:value={annExpiryDate} class="flex-1 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
          </div>
        {/if}
        <div class="flex gap-2">
          <input type="text" bind:value={annMessage} class="flex-1 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" placeholder="Optional message…" />
          <button onclick={submitAnnouncement} disabled={annSubmitting} class="bg-brand-500 text-white text-xs px-3 py-1.5 rounded-lg font-medium hover:bg-brand-600 transition-colors disabled:opacity-60">
            {annSubmitting ? '…' : 'Post'}
          </button>
        </div>
      </div>
    </div>
    {/if}
  </div>
  {/if}

  <!-- ANNOUNCEMENTS (full width when selected directly) -->
  {#if activeSection === 'announcements'}
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
    <div class="p-4 border-b border-gray-50">
      <h2 class="font-semibold text-gray-900 dark:text-white">📢 Announcements</h2>
    </div>
    <div class="px-4 pt-3 pb-2 border-b border-gray-50 dark:border-gray-700">
      <input bind:value={annSearch} type="search" placeholder="Search announcements…"
        class="w-full rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
    </div>
    <div class="divide-y divide-gray-50 dark:divide-gray-700">
      {#if filteredAnnouncements.length === 0}
        <EmptyState message="No announcements yet" icon="📢" />
      {:else}
        {#each filteredAnnouncements.slice(annPage * PAGE_SIZE, (annPage + 1) * PAGE_SIZE) as ann}
          <div class="flex items-center justify-between px-4 py-3 text-sm">
            <div>
              <span class="text-gray-700 dark:text-gray-300 text-xs font-medium">{ann.item?.name}</span>
              <span class="text-gray-400 text-xs ml-1.5">· {ann.quantity} {ann.item?.unit}</span>
              {#if ann.expiryDate}<span class="text-orange-500 text-xs ml-1.5">exp {ann.expiryDate}</span>{/if}
              {#if ann.message}<div class="text-gray-500 dark:text-gray-400 text-xs mt-0.5">{ann.message}</div>{/if}
            </div>
            <StatusBadge status={ann.type} />
          </div>
        {/each}
      {/if}
    </div>
    {#if filteredAnnouncements.length > PAGE_SIZE}
      <div class="px-4 pb-2">
        <Pagination page={annPage} totalPages={Math.ceil(filteredAnnouncements.length / PAGE_SIZE)} totalElements={filteredAnnouncements.length} size={PAGE_SIZE} onchange={(p) => annPage = p} />
      </div>
    {/if}
    <div class="p-4 border-t border-gray-50 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-700/30 space-y-2">
      <div class="text-xs font-medium text-gray-500 dark:text-gray-400">Post announcement</div>
      <div class="flex gap-2 flex-wrap">
        <select bind:value={annType} class="rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
          {#each ['SURPLUS','EXPIRY','URGENT'] as t}<option>{t}</option>{/each}
        </select>
        <select bind:value={annItemId} class="flex-1 min-w-0 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500">
          {#each items as item}<option value={item.id}>{item.name}</option>{/each}
        </select>
        <input type="number" bind:value={annQty} min="1" class="w-14 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs" />
      </div>
      {#if annItemIsFood}
        <div class="flex items-center gap-2">
          <label class="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap">Expiry date</label>
          <input type="date" bind:value={annExpiryDate} class="flex-1 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" />
        </div>
      {/if}
      <div class="flex gap-2">
        <input type="text" bind:value={annMessage} class="flex-1 rounded-lg border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 text-xs focus:border-brand-500 focus:ring-brand-500" placeholder="Optional message…" />
        <button onclick={submitAnnouncement} disabled={annSubmitting} class="bg-brand-500 text-white text-xs px-3 py-1.5 rounded-lg font-medium hover:bg-brand-600 transition-colors disabled:opacity-60">
          {annSubmitting ? '…' : 'Post'}
        </button>
      </div>
    </div>
  </div>
  {/if}

  <!-- COMMUNITY BOARD -->
  {#if activeSection === 'community'}
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
    <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
      <div>
        <h2 class="font-semibold text-gray-900 dark:text-white">🌐 Community Board</h2>
        <p class="text-xs text-gray-400 mt-0.5">Announcements from other shelters — connect to exchange resources</p>
      </div>
      <span class="text-xs text-gray-400">{communityAnnouncements.length} posts</span>
    </div>
    {#if loading}
      <div class="p-4 space-y-2">{#each [1,2,3] as _}<div class="h-14 bg-gray-50 dark:bg-gray-700 animate-pulse rounded"></div>{/each}</div>
    {:else if communityAnnouncements.length === 0}
      <EmptyState message="No community announcements yet" icon="🌐" />
    {:else}
      <div class="divide-y divide-gray-50 dark:divide-gray-700">
        {#each communityAnnouncements as ann}
          {@const alreadyRequested = outgoingRequests.some(r => r.announcement?.id === ann.id)}
          <div class="px-4 py-3">
            <div class="flex items-start justify-between gap-3">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-medium text-gray-800 dark:text-gray-100 text-sm">{ann.item?.name}</span>
                  <span class="text-gray-400 text-xs">· {ann.quantity} {ann.item?.unit}</span>
                  {#if ann.expiryDate}<span class="text-orange-500 text-xs">· exp {ann.expiryDate}</span>{/if}
                  <StatusBadge status={ann.type} />
                </div>
                <div class="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                  <span class="font-medium text-gray-700 dark:text-gray-300">{ann.organization?.name}</span>
                  {#if ann.message}<span class="ml-1">— {ann.message}</span>{/if}
                </div>
              </div>
              <div class="flex-shrink-0">
                {#if alreadyRequested}
                  <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 border border-green-100 dark:border-green-800/50">
                    ✓ Requested
                  </span>
                {:else}
                  <button
                    onclick={() => respondToAnnouncement(ann.id)}
                    disabled={respondingId === ann.id}
                    class="text-xs bg-brand-500 hover:bg-brand-600 text-white px-3 py-1.5 rounded-lg font-medium transition-colors disabled:opacity-60">
                    {respondingId === ann.id ? '…' : ann.type === 'SURPLUS' || ann.type === 'EXPIRY' ? '🙋 I need this' : '🤝 I can give this'}
                  </button>
                {/if}
              </div>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  </div>
  {/if}

  <!-- EXCHANGE REQUESTS -->
  {#if activeSection === 'exchanges'}
  <div class="space-y-6">
    <!-- Incoming: requests on YOUR announcements -->
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700 flex items-center justify-between">
        <div>
          <h2 class="font-semibold text-gray-900 dark:text-white">📥 Incoming Requests</h2>
          <p class="text-xs text-gray-400 mt-0.5">Other shelters responding to your announcements</p>
        </div>
        <span class="text-xs text-gray-400">{incomingRequests.filter(r => r.status === 'PENDING').length} pending</span>
      </div>
      {#if incomingRequests.length === 0}
        <EmptyState message="No incoming requests yet" icon="📥" />
      {:else}
        <div class="divide-y divide-gray-50 dark:divide-gray-700">
          {#each incomingRequests as req}
            <div class="px-4 py-3">
              <div class="flex items-center justify-between gap-3">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-medium text-gray-800 dark:text-gray-100 text-sm">{req.requestingOrg?.name}</span>
                    <span class="text-xs text-gray-400">
                      {req.announcement?.type === 'SURPLUS' || req.announcement?.type === 'EXPIRY'
                        ? 'needs your'
                        : 'can give you'}
                      <span class="font-medium text-gray-600 dark:text-gray-300 ml-1">{req.announcement?.item?.name}</span>
                      <span class="ml-1">({req.announcement?.quantity} {req.announcement?.item?.unit})</span>
                    </span>
                  </div>
                  <div class="flex items-center gap-2 mt-1">
                    <StatusBadge status={req.status} />
                    {#if req.requestingOrg?.contactEmail}
                      <a href="mailto:{req.requestingOrg.contactEmail}" class="text-xs text-brand-500 hover:underline">{req.requestingOrg.contactEmail}</a>
                    {/if}
                    {#if req.requestingOrg?.contactPhone}
                      <a href="tel:{req.requestingOrg.contactPhone}" class="text-xs text-gray-500 hover:underline">📞 {req.requestingOrg.contactPhone}</a>
                    {/if}
                  </div>
                </div>
                {#if req.status === 'PENDING'}
                  <div class="flex items-center gap-2 flex-shrink-0">
                    <button onclick={() => handleExchangeStatus(req.id, 'ACCEPTED')}
                      class="text-xs bg-green-500 hover:bg-green-600 text-white px-3 py-1.5 rounded-lg font-medium transition-colors">
                      Accept
                    </button>
                    <button onclick={() => handleExchangeStatus(req.id, 'REJECTED')}
                      class="text-xs text-gray-400 hover:text-red-500 transition-colors">
                      Decline
                    </button>
                  </div>
                {/if}
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>

    <!-- Outgoing: requests YOU made -->
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
      <div class="p-4 border-b border-gray-50 dark:border-gray-700">
        <h2 class="font-semibold text-gray-900 dark:text-white">📤 My Requests</h2>
        <p class="text-xs text-gray-400 mt-0.5">Requests you sent to other shelters</p>
      </div>
      {#if outgoingRequests.length === 0}
        <EmptyState message="No outgoing requests yet" icon="📤" />
      {:else}
        <div class="divide-y divide-gray-50 dark:divide-gray-700">
          {#each outgoingRequests as req}
            <div class="px-4 py-3 flex items-center justify-between gap-3">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 flex-wrap text-sm">
                  <span class="font-medium text-gray-700 dark:text-gray-300">{req.announcement?.organization?.name}</span>
                  <span class="text-xs text-gray-400">·</span>
                  <span class="text-xs text-gray-600 dark:text-gray-300">{req.announcement?.item?.name} ({req.announcement?.quantity} {req.announcement?.item?.unit})</span>
                </div>
                {#if req.status === 'ACCEPTED' && req.announcement?.organization?.contactEmail}
                  <div class="text-xs text-green-600 dark:text-green-400 mt-0.5">
                    ✓ Accepted — contact:
                    <a href="mailto:{req.announcement.organization.contactEmail}" class="hover:underline ml-1">{req.announcement.organization.contactEmail}</a>
                    {#if req.announcement.organization.contactPhone}
                      · <a href="tel:{req.announcement.organization.contactPhone}" class="hover:underline">{req.announcement.organization.contactPhone}</a>
                    {/if}
                  </div>
                {/if}
              </div>
              <StatusBadge status={req.status} />
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
  {/if}

</div>
