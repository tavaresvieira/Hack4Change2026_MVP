<script lang="ts">
  import { goto } from '$app/navigation';
  import { login } from '$lib/api/auth';
  import { auth } from '$lib/stores/auth';
  import { showToast } from '$lib/stores/toast';

  let email = $state('');
  let password = $state('');
  let loading = $state(false);
  let error = $state('');

  async function handleSubmit() {
    loading = true;
    error = '';
    try {
      const res = await login(email, password);
      if (res.role === 'ADMIN') {
        // Admin accidentally used the staff portal — redirect them
        auth.setAuth(res);
        goto('/dashboard/admin');
        return;
      }
      auth.setAuth(res);
      showToast(`Welcome back, ${res.name}!`);
      goto('/dashboard/staff');
    } catch (e: any) {
      error = e.message || 'Invalid email or password';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head><title>Login — ResourceBridge</title></svelte:head>

<div class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
  <div class="w-full max-w-sm">
    <div class="text-center mb-8">
      <div class="w-14 h-14 bg-brand-500 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
        <span class="text-white font-bold text-xl">RB</span>
      </div>
      <h1 class="text-2xl font-bold text-gray-900">Staff Portal</h1>
      <p class="text-sm text-gray-500 mt-1">Sign in to your staff account</p>
    </div>

    <div class="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
      {#if error}
        <div class="bg-red-50 border border-red-100 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">
          {error}
        </div>
      {/if}

      <form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5" for="email">Email</label>
          <input
            id="email" type="email" bind:value={email} required
            class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm"
            placeholder="you@organization.ca"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5" for="password">Password</label>
          <input
            id="password" type="password" bind:value={password} required
            class="w-full rounded-lg border-gray-200 focus:border-brand-500 focus:ring-brand-500 text-sm"
            placeholder="••••••••"
          />
        </div>
        <button
          type="submit" disabled={loading}
          class="w-full bg-brand-500 hover:bg-brand-600 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
        >
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>

    <p class="text-center text-xs text-gray-400 mt-4">
      Staff accounts are created by invitation only.
    </p>
    <p class="text-center text-sm text-gray-500 mt-2">
      <a href="/donate" class="text-brand-600 font-medium hover:underline">← Browse needs as a donor</a>
    </p>
    <p class="text-center text-xs text-gray-400 mt-2">
      Admin? <a href="/admin" class="text-purple-500 hover:text-purple-700 underline">Admin portal →</a>
    </p>
  </div>
</div>
