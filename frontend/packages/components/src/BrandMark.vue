<script setup lang="ts">
withDefaults(defineProps<{
  logo?: string | null
  title?: string
  size?: 'sm' | 'md' | 'lg'
}>(), { title: 'Easy1Auth', size: 'md' })
</script>

<template>
  <div class="easy1auth-brand-mark" :class="`easy1auth-brand-mark--${size}`">
    <img v-if="logo" :src="logo" :alt="title" class="easy1auth-brand-mark__image">
    <span v-else class="easy1auth-brand-mark__glyph" aria-hidden="true">E</span>
    <span class="easy1auth-brand-mark__title">{{ title }}</span>
  </div>
</template>

<style scoped>
.easy1auth-brand-mark { display: inline-flex; align-items: center; gap: .7rem; color: var(--easy1auth-text, #0f172a); font-weight: 700; letter-spacing: -.02em; }
.easy1auth-brand-mark__glyph, .easy1auth-brand-mark__image { width: 2.5rem; height: 2.5rem; border-radius: .8rem; object-fit: cover; }
.easy1auth-brand-mark__glyph { display: grid; place-items: center; color: #fff; background: var(--easy1auth-primary, #0369a1); box-shadow: 0 8px 18px color-mix(in srgb, var(--easy1auth-primary, #0369a1) 28%, transparent); }
.easy1auth-brand-mark--sm .easy1auth-brand-mark__glyph, .easy1auth-brand-mark--sm .easy1auth-brand-mark__image { width: 2rem; height: 2rem; border-radius: .6rem; }
.easy1auth-brand-mark--lg .easy1auth-brand-mark__glyph, .easy1auth-brand-mark--lg .easy1auth-brand-mark__image { width: 3.25rem; height: 3.25rem; border-radius: 1rem; }
.easy1auth-brand-mark--sm { gap: .5rem; font-size: .95rem; }
.easy1auth-brand-mark--lg { gap: .85rem; font-size: 1.25rem; }
</style>
