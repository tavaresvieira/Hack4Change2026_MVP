<script lang="ts">
  import { onMount } from 'svelte';
  import { getUnfulfilled } from '$lib/api/needs';
  import { getAll as getItems } from '$lib/api/items';
  import { getAll as getOrgs } from '$lib/api/organizations';
  import { createDonation } from '$lib/api/donations';
  import { showToast } from '$lib/stores/toast';
  import UrgencyBadge from '$lib/components/UrgencyBadge.svelte';
  import type { Need, Item, Donation, ItemCategory, Organization } from '$lib/types';

  // Greater Moncton pickup zone (~20 km)
  const PICKUP_AREAS = ['Moncton', 'Dieppe', 'Riverview'];
  const ALL_AREAS    = [...PICKUP_AREAS, 'Shediac', 'Sackville', 'Other'];

  // Nearest drop-off org per city (voluntary mode)
  const NEAREST_ORG_BY_CITY: Record<string, string> = {
    'Moncton':   'House of Nazareth',
    'Dieppe':    'Second Mile Food Bank',
    'Riverview': 'Salvation Army',
  };

  // Out-of-range org recommendations (pickup mode)
  const OUT_OF_RANGE_ORGS: Record<string, { name: string; address: string; phone: string; email: string; note: string }> = {
    'Shediac':  { name: 'Second Mile Food Bank',             address: '243 Lewisville Rd, Moncton, NB', phone: '506-857-9121', email: 'info@secondmilefoodbank.ca',    note: 'Serves the Shediac & Dieppe corridor — they may be able to arrange collection.' },
    'Sackville':{ name: 'Salvation Army Community Services', address: '32 King St, Moncton, NB',        phone: '506-389-9901', email: 'moncton@salvationarmy.ca',       note: 'Accepts donations from the Sackville area and can advise on logistics.' },
    'Other':    { name: 'Peter McKee Community Food Centre', address: '66 Capitol St, Moncton, NB',     phone: '506-383-4281', email: 'info@petermckeecfc.ca',          note: 'General donation inquiries — they can connect you with the right partner.' },
  };

  const URGENCY_ORDER: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

  // ── Data ──────────────────────────────────────────────
  let needs   = $state<Need[]>([]);
  let items   = $state<Item[]>([]);
  let orgs    = $state<Organization[]>([]);
  let loading = $state(true);

  // ── UI state ──────────────────────────────────────────
  let activeTab         = $state<'browse' | 'donate'>('browse');
  let submittedDonation = $state<Donation | null>(null);

  // Browse filters — single dropdown value like 'cat:FOOD' or 'urg:CRITICAL'
  let searchQuery  = $state('');
  let activeFilter = $state('');

  // ── Donation mode (derived — targeted when an org is chosen) ───
  let selectedOrgId = $state<number | null>(null);
  let donationMode  = $derived<'targeted' | 'voluntary'>(selectedOrgId ? 'targeted' : 'voluntary');

  // ── Step flow (donate tab) ────────────────────────────
  let step = $state<1 | 2 | 3>(1);

  // Donation form
  let donorName     = $state('');
  let donorEmail    = $state('');
  let donorPhone    = $state('');
  let selectedItemId = $state<number | null>(null);
  let quantity      = $state(1);
  let expiryDate    = $state('');
  let donationType  = $state<'DROP_OFF' | 'PICKUP_REQUEST'>('DROP_OFF');
  let donorCity     = $state(PICKUP_AREAS[0]);   // used for pickup range + voluntary nearest
  let pickupAddress = $state('');
  let submitting    = $state(false);

  // ── Derived ───────────────────────────────────────────
  let selectedOrg     = $derived(orgs.find(o => o.id === selectedOrgId) ?? null);
  let selectedItem    = $derived(items.find(i => i.id === Number(selectedItemId)) ?? null);
  let pickupAvailable = $derived(PICKUP_AREAS.includes(donorCity));
  let outOfRangeOrg   = $derived(!pickupAvailable ? (OUT_OF_RANGE_ORGS[donorCity] ?? OUT_OF_RANGE_ORGS['Other']) : null);

  // Unfulfilled needs for the selected org (targeted mode)
  let orgNeeds = $derived(
    donationMode === 'targeted' && selectedOrgId
      ? needs.filter(n => !n.fulfilled && n.organization.id === selectedOrgId)
      : []
  );

  // Item dropdown options:
  //   targeted → only items the selected org needs
  //   voluntary → all items (donor brings whatever they have)
  let availableItems = $derived(
    donationMode === 'targeted'
      ? [...new Map(orgNeeds.map(n => [n.item.id, n.item])).values()]
      : items
  );

  // Nearest shelter for voluntary drop-off
  let nearestOrg = $derived(
    donationMode === 'voluntary' && donationType === 'DROP_OFF' && NEAREST_ORG_BY_CITY[donorCity]
      ? orgs.find(o => o.name === NEAREST_ORG_BY_CITY[donorCity]) ?? null
      : null
  );

  // Parse filter dropdown value
  let filterCategory = $derived(activeFilter.startsWith('cat:') ? activeFilter.slice(4) as ItemCategory : '' as ItemCategory | '');
  let filterUrgency  = $derived(activeFilter.startsWith('urg:') ? activeFilter.slice(4) : '');

  // Browse tab derived — filtered + sorted by urgency
  let filteredNeeds = $derived(
    [...needs.filter(n => {
      if (n.fulfilled) return false;
      if (filterUrgency && n.urgency !== filterUrgency) return false;
      if (filterCategory && n.item?.category !== filterCategory) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        if (!n.item?.name?.toLowerCase().includes(q) && !n.organization?.name?.toLowerCase().includes(q)) return false;
      }
      return true;
    })].sort((a, b) => (URGENCY_ORDER[a.urgency] ?? 4) - (URGENCY_ORDER[b.urgency] ?? 4))
  );

  // Split into critical spotlight + rest (only when not already urgency-filtering)
  let criticalNeeds = $derived(!filterUrgency ? filteredNeeds.filter(n => n.urgency === 'CRITICAL') : []);
  let otherNeeds    = $derived(!filterUrgency ? filteredNeeds.filter(n => n.urgency !== 'CRITICAL') : filteredNeeds);

  // Reset selected item when available items change
  $effect(() => {
    if (availableItems.length > 0 && !availableItems.find(i => i.id === Number(selectedItemId))) {
      selectedItemId = availableItems[0].id;
    }
  });

  // ── Validation ────────────────────────────────────────
  let phoneError   = $state('');
  let emailError   = $state('');
  let addressError = $state('');

  function validateEmail(val: string): string {
    if (!val.trim()) return 'Please enter your email address';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim()))
      return 'Please enter a valid email (e.g. jane@email.com)';
    return '';
  }

  function validatePhone(val: string): string {
    if (!val.trim()) return '';
    const digits = val.replace(/\D/g, '');
    if (digits.length < 10 || digits.length > 11)
      return 'Enter a valid 10-digit number (e.g. 506-555-0123)';
    return '';
  }

  function validateAddress(val: string): string {
    if (!val.trim()) return 'Please enter your street address';
    if (val.trim().length < 5) return 'Please enter a complete address';
    if (!/\d/.test(val)) return 'Include a street number (e.g. 45 Elmwood Dr)';
    return '';
  }

  // ── Lifecycle ─────────────────────────────────────────
  onMount(async () => {
    const params = typeof window !== 'undefined' ? new URLSearchParams(window.location.search) : null;
    if (params?.get('tab') === 'donate') activeTab = 'donate';
    try {
      const [n, i, o] = await Promise.all([getUnfulfilled(), getItems(), getOrgs()]);
      needs = n; items = i; orgs = o;
      if (i.length > 0) selectedItemId = i[0].id;
      // Pre-fill from "Donate this" on main/browse pages
      const orgId = params?.get('orgId');
      const itemId = params?.get('itemId');
      if (orgId) selectedOrgId = Number(orgId);
      if (itemId) selectedItemId = Number(itemId);
    } catch {}
    finally { loading = false; }
  });

  // Pre-fill from "Donate this →" on a need card → targeted mode
  function preFill(need: Need) {
    selectedOrgId  = need.organization.id;
    selectedItemId = need.item.id;
    step           = 1;
    activeTab      = 'donate';
  }

  async function handleDonate() {
    if (!selectedItemId) return;
    if (donationType === 'PICKUP_REQUEST' && !pickupAddress.trim()) {
      showToast('Please enter your pickup address', 'error');
      return;
    }
    submitting = true;
    try {
      const body: any = {
        donorName,
        donorEmail,
        donorPhone: donorPhone.trim() || null,
        item:       { id: Number(selectedItemId) },
        quantity:   Number(quantity),
        donationType,
      };
      if (donationMode === 'targeted' && selectedOrgId) {
        body.preferredOrganization = { id: selectedOrgId };
      }
      if (selectedItem?.expiryRelevant && expiryDate) body.expiryDate = expiryDate;
      if (donationType === 'PICKUP_REQUEST') {
        body.pickupAddress = pickupAddress;
        body.pickupCity    = donorCity;
      }
      submittedDonation = await createDonation(body);
      showToast('Thank you! Your donation has been submitted.');
      donorName = ''; donorEmail = ''; donorPhone = ''; quantity = 1; expiryDate = '';
      pickupAddress = ''; donationType = 'DROP_OFF';
      selectedOrgId = null; step = 1;
    } catch (e: any) {
      showToast(e.message || 'Failed to submit donation', 'error');
    } finally {
      submitting = false;
    }
  }
</script>

<svelte:head><title>Donate — ResourceBridge</title></svelte:head>

<div class="min-h-screen bg-gray-50">
  <!-- NAV -->
  <nav class="bg-white border-b border-gray-100 px-4 py-3">
    <div class="max-w-5xl mx-auto flex items-center justify-between">
      <a href="/" class="flex items-center gap-2">
        <div class="w-8 h-8 bg-brand-500 rounded-lg flex items-center justify-center">
          <span class="text-white font-bold text-sm">RB</span>
        </div>
        <span class="font-bold text-gray-900">ResourceBridge</span>
      </a>
      <a href="/login" class="text-sm text-gray-500 hover:text-brand-600">Staff login →</a>
    </div>
  </nav>

  <div class="max-w-5xl mx-auto px-4 py-8">
    <div class="mb-6 text-center">
      <h1 class="text-2xl font-bold text-gray-900 mb-4">Find items shelters need</h1>
      <div class="inline-flex gap-1 bg-gray-100 p-1 rounded-xl">
        <button
          class="px-5 py-2 rounded-lg text-sm font-medium transition-colors {activeTab === 'browse' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}"
          onclick={() => activeTab = 'browse'}>Browse Needs</button>
        <button
          class="px-5 py-2 rounded-lg text-sm font-medium transition-colors {activeTab === 'donate' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}"
          onclick={() => activeTab = 'donate'}>Make a Donation</button>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════ -->
    <!--  BROWSE TAB                                       -->
    <!-- ══════════════════════════════════════════════════ -->
    {#if activeTab === 'browse'}

      <!-- Search + Filter row -->
      <div class="flex gap-2 mb-6">
        <div class="relative flex-1">
          <div class="absolute inset-y-0 left-3 flex items-center pointer-events-none">
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
          <input type="text" bind:value={searchQuery}
            placeholder="Search by item or organization…"
            class="w-full pl-9 pr-8 py-2.5 rounded-xl border border-gray-200 bg-white text-sm text-gray-900 focus:border-brand-500 focus:ring-brand-500 placeholder-gray-400" />
          {#if searchQuery}
            <button onclick={() => searchQuery = ''}
              class="absolute inset-y-0 right-3 flex items-center text-gray-400 hover:text-gray-600">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          {/if}
        </div>

        <select bind:value={activeFilter}
          class="rounded-xl border border-gray-200 bg-white text-sm px-3 py-2.5 focus:border-brand-500 focus:ring-brand-500 text-gray-700 min-w-[130px]">
          <option value="">All needs</option>
          <optgroup label="── Category">
            <option value="cat:FOOD">Food</option>
            <option value="cat:CLOTHING">Clothing</option>
            <option value="cat:HYGIENE">Hygiene</option>
            <option value="cat:BEDDING">Bedding</option>
            <option value="cat:OTHER">Other</option>
          </optgroup>
          <optgroup label="── Urgency">
            <option value="urg:CRITICAL">Critical only</option>
            <option value="urg:HIGH">High only</option>
            <option value="urg:MEDIUM">Medium only</option>
            <option value="urg:LOW">Low only</option>
          </optgroup>
        </select>

        {#if activeFilter || searchQuery}
          <button onclick={() => { activeFilter = ''; searchQuery = ''; }}
            class="text-sm text-gray-400 hover:text-brand-600 font-medium px-1 transition-colors whitespace-nowrap">
            Clear
          </button>
        {/if}
      </div>

      {#if loading}
        <div class="grid sm:grid-cols-2 gap-4">
          {#each [1,2,3,4] as _}
            <div class="h-28 bg-gray-100 rounded-xl animate-pulse"></div>
          {/each}
        </div>

      {:else if filteredNeeds.length === 0}
        <div class="text-center py-16">
          <div class="text-4xl mb-3">🔍</div>
          <div class="font-medium text-gray-700">No needs match your search</div>
          <div class="text-sm text-gray-400 mt-1">Try a different filter or search term</div>
          <button onclick={() => { activeFilter = ''; searchQuery = ''; }}
            class="mt-3 text-sm text-brand-600 font-medium hover:underline">Show all needs</button>
        </div>

      {:else}
        <!-- Critical spotlight (only shown when no urgency filter active) -->
        {#if criticalNeeds.length > 0}
          <div class="mb-6">
            <h2 class="text-sm font-semibold text-red-600 uppercase tracking-wide mb-3">🚨 Critical — needed urgently</h2>
            <div class="grid sm:grid-cols-2 gap-4">
              {#each criticalNeeds as need}
                <div class="bg-white rounded-xl p-4 shadow-sm border-l-4 border-l-red-400 border border-gray-100 flex flex-col justify-between">
                  <div>
                    <div class="flex items-start justify-between gap-2 mb-1">
                      <div class="font-semibold text-gray-900">{need.item?.name}</div>
                      <UrgencyBadge urgency={need.urgency} />
                    </div>
                    <div class="text-sm text-gray-500">{need.organization?.name}</div>
                    <div class="text-sm text-gray-700 mt-2">
                      Needs <span class="font-semibold text-brand-600">{need.quantityNeeded} {need.item?.unit}</span>
                    </div>
                  </div>
                  <button onclick={() => preFill(need)}
                    class="mt-4 w-full bg-brand-500 hover:bg-brand-600 text-white text-sm font-medium py-2 rounded-lg transition-colors">
                    Donate this →
                  </button>
                </div>
              {/each}
            </div>
          </div>
        {/if}

        <!-- Remaining needs -->
        {#if otherNeeds.length > 0}
          {#if criticalNeeds.length > 0}
            <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">Other needs</h2>
          {/if}
          <div class="grid sm:grid-cols-2 gap-4">
            {#each otherNeeds as need}
              <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-100 flex flex-col justify-between">
                <div>
                  <div class="flex items-start justify-between gap-2 mb-1">
                    <div class="font-semibold text-gray-900">{need.item?.name}</div>
                    <UrgencyBadge urgency={need.urgency} />
                  </div>
                  <div class="text-sm text-gray-500">{need.organization?.name}</div>
                  <div class="text-sm text-gray-700 mt-2">
                    Needs <span class="font-semibold text-brand-600">{need.quantityNeeded} {need.item?.unit}</span>
                  </div>
                </div>
                <button onclick={() => preFill(need)}
                  class="mt-4 w-full bg-brand-50 text-brand-700 hover:bg-brand-100 text-sm font-medium py-2 rounded-lg transition-colors">
                  Donate this →
                </button>
              </div>
            {/each}
          </div>
        {/if}
      {/if}

    <!-- ══════════════════════════════════════════════════ -->
    <!--  DONATE TAB — 3-step flow                        -->
    <!-- ══════════════════════════════════════════════════ -->
    {:else}
      {#if submittedDonation}
        <!-- SUCCESS STATE -->
        <div class="max-w-sm mx-auto bg-green-50 border border-green-100 rounded-2xl p-8 text-center">
          <div class="text-4xl mb-4">✅</div>
          <h3 class="font-bold text-green-800 text-xl mb-2">Thank you!</h3>
          <p class="text-green-700 text-sm">
            Your donation of <strong>{submittedDonation.quantity} {submittedDonation.item?.unit}</strong>
            of <strong>{submittedDonation.item?.name}</strong> has been received.
          </p>
          {#if submittedDonation.donationType === 'PICKUP_REQUEST'}
            <p class="text-sm text-green-600 mt-3">
              A shelter representative will contact you at <strong>{submittedDonation.donorEmail}</strong> to arrange pickup.
            </p>
          {:else}
            <p class="text-sm text-green-600 mt-3">
              You'll receive drop-off details at <strong>{submittedDonation.donorEmail}</strong>.
            </p>
          {/if}
          <p class="text-gray-400 text-xs mt-4">Donation ID: #{submittedDonation.id}</p>
          <button onclick={() => submittedDonation = null}
            class="mt-5 text-sm text-brand-600 font-medium hover:underline">
            Make another donation
          </button>
        </div>

      {:else}
        <div class="max-w-sm mx-auto">

          <!-- STEP INDICATOR -->
          <div class="flex items-center gap-2 mb-8">
            {#each [1, 2, 3] as s}
              <div class="flex-1 h-1.5 rounded-full transition-colors duration-200
                          {step >= s ? 'bg-brand-500' : 'bg-gray-200'}"></div>
            {/each}
            <span class="text-xs text-gray-400 whitespace-nowrap ml-1">Step {step} of 3</span>
          </div>

          <!-- ────────────────────────────────────────── -->
          <!-- STEP 1 — What are you donating?           -->
          <!-- ────────────────────────────────────────── -->
          {#if step === 1}
            <h2 class="text-xl font-bold text-gray-900 mb-6">What are you donating?</h2>

            <div class="space-y-5">
              <!-- Optional: specific shelter -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  Shelter <span class="text-gray-400 font-normal">(optional — leave blank to let the system match)</span>
                </label>
                <select bind:value={selectedOrgId}
                  class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white">
                  <option value={null}>Any shelter — best match</option>
                  {#each orgs as org}
                    {@const c = needs.filter(n => !n.fulfilled && n.organization.id === org.id).length}
                    <option value={org.id}>{org.name}{c > 0 ? ` (${c} need${c !== 1 ? 's' : ''})` : ''}</option>
                  {/each}
                </select>
              </div>

              <!-- Org needs chips (targeted mode) -->
              {#if donationMode === 'targeted' && orgNeeds.length > 0}
                <div class="bg-brand-50 rounded-xl p-3">
                  <p class="text-xs font-medium text-brand-700 mb-2">Tap to select what they need:</p>
                  <div class="flex flex-wrap gap-1.5">
                    {#each orgNeeds as n}
                      <button type="button" onclick={() => selectedItemId = n.item.id}
                        class="px-2.5 py-1 rounded-full text-xs font-medium border transition-all
                          {Number(selectedItemId) === n.item.id
                            ? 'bg-brand-500 text-white border-brand-500'
                            : 'bg-white text-gray-700 border-brand-200 hover:border-brand-400'}">
                        {n.item.name} · {n.quantityNeeded} {n.item.unit}
                      </button>
                    {/each}
                  </div>
                </div>
              {/if}

              <!-- Item -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Item</label>
                {#if availableItems.length > 0}
                  <select bind:value={selectedItemId}
                    class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white">
                    {#each availableItems as item}
                      <option value={item.id}>{item.name}</option>
                    {/each}
                  </select>
                {:else}
                  <p class="text-sm text-gray-400 italic px-3 py-2.5 bg-gray-50 rounded-xl border border-gray-200">
                    No active needs for this shelter right now
                  </p>
                {/if}
              </div>

              <!-- Quantity -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Quantity</label>
                <div class="flex items-center gap-3">
                  <input type="number" bind:value={quantity} min="1" required
                    class="flex-1 rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white" />
                  {#if selectedItem}
                    <span class="text-sm text-gray-400">{selectedItem.unit}</span>
                  {/if}
                </div>
              </div>

              <!-- Expiry (only if relevant) -->
              {#if selectedItem?.expiryRelevant}
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">
                    Expiry date <span class="text-gray-400 font-normal">(optional)</span>
                  </label>
                  <input type="date" bind:value={expiryDate}
                    class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white" />
                </div>
              {/if}

              <button
                onclick={() => step = 2}
                disabled={!selectedItemId || quantity < 1}
                class="w-full bg-brand-500 hover:bg-brand-600 disabled:opacity-40 disabled:cursor-not-allowed
                       text-white font-semibold py-3 rounded-xl transition-colors text-base mt-2">
                Continue →
              </button>
            </div>

          <!-- ────────────────────────────────────────── -->
          <!-- STEP 2 — How will you deliver it?         -->
          <!-- ────────────────────────────────────────── -->
          {:else if step === 2}
            <h2 class="text-xl font-bold text-gray-900 mb-6">How will you deliver it?</h2>

            <div class="space-y-4">
              <!-- Primary: drop-off -->
              <label class="flex items-start gap-3 p-4 bg-white rounded-xl border-2 cursor-pointer transition-colors
                            {donationType === 'DROP_OFF' ? 'border-brand-500' : 'border-gray-200 hover:border-gray-300'}">
                <input type="radio" name="deliveryType" value="DROP_OFF"
                  bind:group={donationType} class="mt-0.5 accent-brand-500" />
                <div>
                  <div class="font-semibold text-gray-900">I'll drop it off</div>
                  <div class="text-sm text-gray-500 mt-0.5">I'll bring the donation to the shelter</div>
                </div>
              </label>

              <!-- Drop-off location info -->
              {#if donationType === 'DROP_OFF'}
                {#if donationMode === 'targeted' && selectedOrg?.address}
                  <div class="ml-4 bg-gray-50 rounded-xl p-3 border border-gray-100">
                    <p class="text-xs text-gray-500 font-medium mb-1">Drop off at:</p>
                    <p class="text-sm font-semibold text-gray-900">{selectedOrg.name}</p>
                    <p class="text-sm text-gray-500">{selectedOrg.address}</p>
                    {#if selectedOrg.contactPhone}
                      <a href="tel:{selectedOrg.contactPhone}"
                        class="text-sm text-brand-600 hover:underline font-medium mt-1 block">
                        📞 {selectedOrg.contactPhone}
                      </a>
                    {/if}
                  </div>
                {:else}
                  <!-- Voluntary: ask city for nearest org -->
                  <div class="ml-4 space-y-3">
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-2">Your city</label>
                      <select bind:value={donorCity}
                        class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white">
                        {#each ALL_AREAS as area}<option value={area}>{area}</option>{/each}
                      </select>
                    </div>
                    {#if nearestOrg}
                      <div class="bg-gray-50 rounded-xl p-3 border border-gray-100">
                        <p class="text-xs text-gray-500 font-medium mb-1">📍 Nearest drop-off location</p>
                        <p class="text-sm font-semibold text-gray-900">{nearestOrg.name}</p>
                        <p class="text-sm text-gray-500">{nearestOrg.address}</p>
                        {#if nearestOrg.contactPhone}
                          <a href="tel:{nearestOrg.contactPhone}"
                            class="text-sm text-brand-600 hover:underline font-medium mt-1 block">
                            📞 {nearestOrg.contactPhone}
                          </a>
                        {/if}
                      </div>
                    {/if}
                  </div>
                {/if}
              {/if}

              <!-- Secondary: pickup -->
              <label class="flex items-start gap-3 p-4 bg-white rounded-xl border-2 cursor-pointer transition-colors
                            {donationType === 'PICKUP_REQUEST' ? 'border-brand-500' : 'border-gray-200 hover:border-gray-300'}">
                <input type="radio" name="deliveryType" value="PICKUP_REQUEST"
                  bind:group={donationType} class="mt-0.5 accent-brand-500" />
                <div>
                  <div class="font-semibold text-gray-900">Request pickup</div>
                  <div class="text-sm text-gray-500 mt-0.5">Shelter comes to collect from my location</div>
                </div>
              </label>

              <!-- Pickup details (revealed inline) -->
              {#if donationType === 'PICKUP_REQUEST'}
                <div class="ml-4 space-y-3">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 mb-2">Your city</label>
                    <select bind:value={donorCity}
                      class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5">
                      {#each ALL_AREAS as area}<option value={area}>{area}</option>{/each}
                    </select>
                  </div>

                  {#if !pickupAvailable}
                    <div class="bg-amber-50 border border-amber-200 rounded-xl p-3 text-sm">
                      <p class="font-semibold text-amber-800">Outside pickup range</p>
                      <p class="text-amber-700 mt-0.5">Pickup is only available within Greater Moncton (~20 km).</p>
                    </div>
                    {#if outOfRangeOrg}
                      <div class="bg-white border border-gray-200 rounded-xl p-3">
                        <p class="text-xs font-medium text-gray-500 mb-1">🤝 Nearest contact</p>
                        <p class="text-sm font-semibold text-gray-900">{outOfRangeOrg.name}</p>
                        <p class="text-xs text-gray-500 mt-0.5">{outOfRangeOrg.note}</p>
                        <div class="flex gap-2 mt-2">
                          <a href="tel:{outOfRangeOrg.phone}"
                            class="text-xs font-medium text-white bg-brand-500 hover:bg-brand-600 px-3 py-1.5 rounded-lg">
                            📞 Call
                          </a>
                          <a href="mailto:{outOfRangeOrg.email}?subject=Donation%20inquiry%20from%20{encodeURIComponent(donorCity)}"
                            class="text-xs font-medium text-brand-700 bg-brand-50 hover:bg-brand-100 border border-brand-200 px-3 py-1.5 rounded-lg">
                            ✉️ Email
                          </a>
                        </div>
                      </div>
                    {/if}
                  {:else}
                    <div class="text-sm text-green-600 font-medium flex items-center gap-1.5">
                      <span>✓</span> Within Greater Moncton pickup range
                    </div>
                    <div>
                      <label class="block text-sm font-medium text-gray-700 mb-2">Your street address</label>
                      <input type="text" bind:value={pickupAddress}
                        oninput={() => addressError = validateAddress(pickupAddress)}
                        onblur={() => addressError = validateAddress(pickupAddress)}
                        class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white
                               {addressError ? 'border-red-400 focus:border-red-400 focus:ring-red-400' : ''}"
                        placeholder="45 Elmwood Dr" />
                      {#if addressError}
                        <p class="text-xs text-red-500 mt-1">{addressError}</p>
                      {:else}
                        <p class="text-xs text-gray-400 mt-1">Shared only with the matched shelter's staff.</p>
                      {/if}
                    </div>
                  {/if}
                </div>
              {/if}
            </div>

            <!-- Navigation -->
            <div class="flex items-center gap-3 mt-8">
              <button type="button" onclick={() => step = 1}
                class="text-sm text-gray-400 hover:text-gray-600 font-medium">
                ← Back
              </button>
              <button type="button"
                onclick={() => {
                  if (donationType === 'PICKUP_REQUEST') addressError = validateAddress(pickupAddress);
                  if (!addressError) step = 3;
                }}
                disabled={donationType === 'PICKUP_REQUEST' && (!pickupAvailable || !!validateAddress(pickupAddress))}
                class="flex-1 bg-brand-500 hover:bg-brand-600 disabled:opacity-40 disabled:cursor-not-allowed
                       text-white font-semibold py-3 rounded-xl transition-colors">
                Continue →
              </button>
            </div>

          <!-- ────────────────────────────────────────── -->
          <!-- STEP 3 — Your details                     -->
          <!-- ────────────────────────────────────────── -->
          {:else if step === 3}
            <h2 class="text-xl font-bold text-gray-900 mb-6">Your details</h2>

            <form onsubmit={(e) => { e.preventDefault(); handleDonate(); }} class="space-y-5">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Your name</label>
                <input type="text" bind:value={donorName} required
                  class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white"
                  placeholder="Jane Smith" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">Email</label>
                <input type="email" bind:value={donorEmail} required
                  oninput={() => emailError = validateEmail(donorEmail)}
                  onblur={() => emailError = validateEmail(donorEmail)}
                  class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white
                         {emailError ? 'border-red-400 focus:border-red-400 focus:ring-red-400' : ''}"
                  placeholder="jane@email.com" />
                {#if emailError}
                  <p class="text-xs text-red-500 mt-1">{emailError}</p>
                {/if}
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-2">
                  Phone <span class="text-gray-400 font-normal">(optional)</span>
                </label>
                <input type="tel" bind:value={donorPhone}
                  oninput={() => phoneError = validatePhone(donorPhone)}
                  onblur={() => phoneError = validatePhone(donorPhone)}
                  class="w-full rounded-xl border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm py-2.5 text-gray-900 bg-white
                         {phoneError ? 'border-red-400 focus:border-red-400 focus:ring-red-400' : ''}"
                  placeholder="506-555-0123" />
                {#if phoneError}
                  <p class="text-xs text-red-500 mt-1">{phoneError}</p>
                {/if}
              </div>

              <div class="flex items-center gap-3 pt-2">
                <button type="button" onclick={() => step = 2}
                  class="text-sm text-gray-400 hover:text-gray-600 font-medium">
                  ← Back
                </button>
                <button type="submit"
                  disabled={submitting || !donorName.trim() || !donorEmail.trim() || !!emailError || !!phoneError}
                  class="flex-1 bg-brand-500 hover:bg-brand-600 disabled:opacity-40 disabled:cursor-not-allowed
                         text-white font-semibold py-3 rounded-xl transition-colors">
                  {#if submitting}
                    Submitting…
                  {:else if donationType === 'PICKUP_REQUEST'}
                    Request Pickup
                  {:else}
                    Submit Donation
                  {/if}
                </button>
              </div>
            </form>
          {/if}

        </div>
      {/if}
    {/if}
  </div>
</div>
