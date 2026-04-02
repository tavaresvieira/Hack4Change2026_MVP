<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { auth } from '$lib/stores/auth';
  import { onMount } from 'svelte';

  onMount(() => { auth.init(); });

  let { children } = $props();
</script>

{@render children()}
<Toast />
