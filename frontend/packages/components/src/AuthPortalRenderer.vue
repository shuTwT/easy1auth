<script setup lang="ts">
import {computed} from 'vue'
import BrandMark from './BrandMark.vue'

type PortalConfig = {
  global: {
    title: string
    subtitle: string
    logoUrl?: string | null
    logoDarkUrl?: string | null
    background: {
      mode: 'solid' | 'image';
      color: string;
      imageUrl?: string | null;
      overlayColor?: string | null;
      overlayOpacity?: number
    }
    primaryColor: string
    card: { width: number; radius: number; shadow: boolean; position: 'left' | 'center' | 'right' }
  }
}

const props = withDefaults(defineProps<{
  config: PortalConfig
  mode?: 'login' | 'register' | 'qr' | 'mfa' | 'consent' | 'expired'
  legal?: { termsOfService?: string | null; privacyPolicy?: string | null }
  titleOverride?: string
  subtitleOverride?: string
}>(), {mode: 'login'})

const global = computed(() => props.config.global)

function rgba(color: string | null | undefined, opacity: number) {
  const match = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(color ?? '#ffffff')
  if (!match) return `rgba(255,255,255,${opacity})`
  return `rgba(${Number.parseInt(match[1]!, 16)},${Number.parseInt(match[2]!, 16)},${Number.parseInt(match[3]!, 16)},${opacity})`
}

const shellStyle = computed(() => ({
  '--portal-primary': global.value.primaryColor,
  '--portal-background': global.value.background.color,
  '--portal-card-width': `${global.value.card.width}px`,
  '--portal-card-radius': `${global.value.card.radius}px`,
  '--portal-card-shadow': global.value.card.shadow ? '0 24px 80px rgba(15, 23, 42, .12)' : 'none',
  '--portal-card-align': global.value.card.position === 'left' ? 'start' : global.value.card.position === 'right' ? 'end' : 'center',
  backgroundImage: global.value.background.mode === 'image' && global.value.background.imageUrl
      ? `linear-gradient(${rgba(global.value.background.overlayColor, global.value.background.overlayOpacity ?? 0)}, ${rgba(global.value.background.overlayColor, global.value.background.overlayOpacity ?? 0)}), url(${JSON.stringify(global.value.background.imageUrl)})`
      : undefined,
}))
</script>

<template>
  <main class="portal-shell" :style="shellStyle">
    <section class="portal-card">
      <header class="portal-header">
        <BrandMark :logo="global.logoUrl" :logo-dark="global.logoDarkUrl" :title="global.title" size="lg"/>
        <span class="portal-secure">安全连接</span>
      </header>

      <slot name="alert"/>
      <template v-if="mode === 'expired'">
        <div class="portal-state">
          <div class="state-icon">!</div>
          <h1>请求已过期</h1>
          <p>请从原应用重新发起登录或授权。</p></div>
      </template>
      <template v-else>
        <slot name="content"/>
        <slot name="legal"/>
      </template>

      <footer class="portal-footer">本次交互由 Easy1Auth 授权服务器保护 · 请勿在公共设备保存密码</footer>
    </section>
  </main>
</template>

<style scoped>
.portal-shell {
  min-height: 100vh;
  display: grid;
  align-items: center;
  justify-items: var(--portal-card-align);
  padding: 2rem 1rem;
  background-color: var(--portal-background);
  background-position: center;
  background-size: cover;
  transition: background-color .2s ease, background-image .2s ease;
}

.portal-card {
  width: min(100%, var(--portal-card-width));
  padding: clamp(1.5rem, 5vw, 3rem);
  border: 1px solid color-mix(in srgb, var(--portal-primary) 13%, #d7e2ec);
  border-radius: var(--portal-card-radius);
  background: rgba(255, 255, 255, .94);
  box-shadow: var(--portal-card-shadow);
  backdrop-filter: blur(16px);
}

.portal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2.5rem;
}

.portal-secure {
  color: var(--portal-primary);
  font-size: .72rem;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.portal-copy {
  margin-bottom: 1.75rem;
}

.eyebrow {
  color: var(--portal-primary);
  font-size: .72rem;
  font-weight: 800;
  letter-spacing: .12em;
  text-transform: uppercase;
}

h1 {
  margin: .55rem 0 .6rem;
  font-size: clamp(1.8rem, 5vw, 2.2rem);
  line-height: 1.12;
  letter-spacing: -.045em;
}

.portal-copy p {
  margin: 0;
  color: #52677d;
  line-height: 1.7;
}

.portal-footer {
  margin-top: 2.25rem;
  color: #94a3b8;
  font-size: .7rem;
  line-height: 1.5;
  text-align: center;
}

.portal-state {
  display: grid;
  justify-items: center;
  gap: .6rem;
  padding: 2.5rem 0;
  text-align: center;
}

.state-icon {
  display: grid;
  place-items: center;
  width: 3rem;
  height: 3rem;
  border-radius: 1rem;
  color: #fff;
  background: var(--portal-primary);
  font-size: 1.5rem;
  font-weight: 800;
}

@media (max-width: 480px) {
  .portal-card {
    padding: 1.35rem;
  }

  .portal-header {
    margin-bottom: 2.25rem;
  }
}
</style>
