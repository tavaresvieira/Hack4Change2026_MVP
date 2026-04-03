<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { login } from '$lib/api/auth';
  import { auth } from '$lib/stores/auth';
  import { showToast } from '$lib/stores/toast';

  let email = $state('');
  let password = $state('');
  let loading = $state(false);
  let error = $state('');

  onMount(() => {
    auth.init();
    if ($auth.token && $auth.role === 'ADMIN') goto('/dashboard/admin');
  });

  async function handleSubmit() {
    loading = true;
    error = '';
    try {
      const res = await login(email, password);
      if (res.role !== 'ADMIN') {
        error = 'This portal is for admins only. Staff should use the staff login.';
        return;
      }
      auth.setAuth(res);
      showToast(`Welcome, ${res.name}!`);
      goto('/dashboard/admin');
    } catch (e: any) {
      error = e.message || 'Invalid email or password';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head><title>Admin Portal — ResourceBridge</title></svelte:head>

<div class="min-h-screen flex items-center justify-center px-4" style="background-color: #111827;">
  <div class="w-full max-w-sm">
    <div class="text-center mb-8">
      <div class="w-14 h-14 bg-purple-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
        <span class="text-white text-2xl">🛡️</span>
      </div>
      <h1 class="text-2xl font-bold text-white">Admin Portal</h1>
      <p class="text-sm text-gray-400 mt-1">ResourceBridge administration</p>
    </div>

    <div class="rounded-2xl shadow-xl p-6" style="background-color: #1f2937; border: 1px solid #374151;">
      {#if error}
        <div class="text-sm rounded-lg px-4 py-3 mb-4" style="background-color: rgba(127,29,29,0.4); border: 1px solid #b91c1c; color: #fca5a5;">
          {error}
        </div>
      {/if}

      <form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1.5" for="email">Email</label>
          <input
            id="email" type="email" bind:value={email} required
            class="w-full rounded-lg text-white text-sm focus:border-purple-500 focus:ring-purple-500"
            style="background-color: #374151; border-color: #4b5563; color: white;"
            placeholder="admin@organization.ca"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-300 mb-1.5" for="password">Password</label>
          <input
            id="password" type="password" bind:value={password} required
            class="w-full rounded-lg text-white text-sm focus:border-purple-500 focus:ring-purple-500"
            style="background-color: #374151; border-color: #4b5563; color: white;"
            placeholder="••••••••"
          />
        </div>
        <button
          type="submit" disabled={loading}
          class="w-full bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
        >
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>

    <p class="text-center text-sm text-gray-500 mt-4">
      Are you staff? <a href="/login" class="text-purple-400 font-medium hover:underline">Staff login →</a>
    </p>
  </div>
</div>
