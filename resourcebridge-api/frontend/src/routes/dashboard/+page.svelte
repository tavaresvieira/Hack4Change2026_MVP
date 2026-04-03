<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { auth } from '$lib/stores/auth';

  onMount(() => {
    auth.init();
    if (!$auth.token) { goto('/login'); return; }
    if ($auth.role === 'ADMIN') {
      goto('/dashboard/admin');
    } else {
      goto('/dashboard/staff');
    }
  });
</script>
