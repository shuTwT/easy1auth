<script setup lang="ts">
withDefaults(defineProps<{
  logo?: string | null
  logoDark?: string | null
  title?: string
  subTitle?:string
  size?: 'sm' | 'md' | 'lg'
}>(), { title: 'Easy1Auth', size: 'md' })
</script>

<template>
  <div class="easy1auth-brand-mark" :class="`easy1auth-brand-mark--${size}`">
    <picture v-if="logo || logoDark" class="easy1auth-brand-mark__picture">
      <source v-if="logoDark" :srcset="logoDark" media="(prefers-color-scheme: dark)">
      <img :src="logo || logoDark || undefined" :alt="title" class="easy1auth-brand-mark__image">
    </picture>
    <span v-else class="easy1auth-brand-mark__glyph" aria-hidden="true">E</span>
    <div>
      <span class="easy1auth-brand-mark__title">{{ title }}</span>
      <span class="easy1auth-brand-mark__subtitle">{{subTitle}}</span>
    </div>
  </div>
</template>

<style scoped>
.easy1auth-brand-mark { display: inline-flex; align-items: center; gap: .7rem; color: var(--easy1auth-text, #0f172a); font-weight: 700; letter-spacing: -.02em; }
.easy1auth-brand-mark__picture { display: inline-flex; }
.easy1auth-brand-mark__glyph, .easy1auth-brand-mark__image { width: 2.5rem; height: 2.5rem; border-radius: .8rem; object-fit: cover; }
.easy1auth-brand-mark__glyph { display: grid; place-items: center; color: #fff; background: var(--easy1auth-primary, #0369a1); box-shadow: 0 8px 18px color-mix(in srgb, var(--easy1auth-primary, #0369a1) 28%, transparent); }
.easy1auth-brand-mark--sm .easy1auth-brand-mark__glyph, .easy1auth-brand-mark--sm .easy1auth-brand-mark__image { width: 2rem; height: 2rem; border-radius: .6rem; }
.easy1auth-brand-mark--lg .easy1auth-brand-mark__glyph, .easy1auth-brand-mark--lg .easy1auth-brand-mark__image { width: 3.25rem; height: 3.25rem; border-radius: 1rem; }
.easy1auth-brand-mark--sm { gap: .5rem; font-size: .95rem; }
.easy1auth-brand-mark--lg { gap: .85rem; font-size: 1.25rem; }
.easy1auth-brand-mark__subtitle{
  color: #52677d;
  line-height: 1.35;
  font-size: 13px;
}
</style>
