<template>
  <section class="screen-panel" :class="{ 'screen-panel--grow': grow }">
    <header class="screen-panel__head">
      <span class="screen-panel__title">{{ title }}</span>
      <slot name="extra" />
    </header>
    <div class="screen-panel__body" :class="{ 'screen-panel__body--scroll': scroll }">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    grow?: boolean;
    scroll?: boolean;
  }>(),
  {
    grow: false,
    scroll: false,
  },
);
</script>
