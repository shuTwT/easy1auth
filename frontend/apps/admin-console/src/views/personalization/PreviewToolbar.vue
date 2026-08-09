<script setup lang="ts">
defineProps<{ device: 'desktop' | 'mobile'; fullscreen: boolean }>()
const emit = defineEmits<{
  (event: 'update:device', value: 'desktop' | 'mobile'): void
  (event: 'toggle-fullscreen'): void
}>()
</script>

<template>
  <div class="preview-toolbar" aria-label="预览设备切换">
    <div class="toolbar-segment">
      <button :class="{ active: device === 'desktop' }" type="button" @click="emit('update:device', 'desktop')">桌面</button>
      <button :class="{ active: device === 'mobile' }" type="button" @click="emit('update:device', 'mobile')">手机</button>
    </div>
    <button class="toolbar-action" type="button" @click="emit('toggle-fullscreen')">{{ fullscreen ? '退出全屏' : '全屏预览' }}</button>
  </div>
</template>

<style scoped>
.preview-toolbar { display: flex; align-items: center; justify-content: space-between; gap: .75rem; padding: .75rem 1rem; border-bottom: 1px solid #e2e8f0; background: rgba(255,255,255,.86); }
.toolbar-segment { display: inline-flex; gap: .2rem; padding: .2rem; border: 1px solid #dbe4ee; border-radius: .7rem; background: #f8fafc; }
.toolbar-segment button, .toolbar-action { border: 0; border-radius: .5rem; padding: .45rem .75rem; color: #52677d; background: transparent; cursor: pointer; font-size: .78rem; font-weight: 700; }
.toolbar-segment button.active { color: #075985; background: #e0f2fe; }
.toolbar-action { border: 1px solid #dbe4ee; background: #fff; }
</style>
