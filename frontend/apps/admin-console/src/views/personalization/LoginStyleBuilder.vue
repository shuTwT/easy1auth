<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'antdv-next'
import { Palette } from '@lucide/vue'
import { socialIdentitySourceApi } from '@/api/socialIdentitySource'
import type { SocialIdentitySource } from '@/types/socialIdentitySource'
import PreviewToolbar from './PreviewToolbar.vue'
import LoginStylePreview from './LoginStylePreview.vue'
import StyleInspector from './StyleInspector.vue'
import { useLoginStyleDraft } from './useLoginStyleDraft'

const tabs = [
  { key: 'global', label: '整体样式', description: '品牌、背景与布局' },
  { key: 'standard', label: '常规登录', description: '真实可用的认证入口' },
  { key: 'qr', label: '扫码登录', description: '仅编辑器预览' },
] as const
type BuilderTab = typeof tabs[number]['key']

const activeTab = ref<BuilderTab>('global')
const device = ref<'desktop' | 'mobile'>('desktop')
const fullscreen = ref(false)
const inspectorOpen = ref(false)
const { config, socialProviderIds, legalDocuments, loading, saving, publishing, resetting, dirty, status, publishedAt, load, markDirty, save, publish, reset } = useLoginStyleDraft()
const socialSources = ref<SocialIdentitySource[]>([])
const socialSourcesLoading = ref(false)

const publishedText = computed(() => publishedAt.value ? `最近发布：${new Date(publishedAt.value).toLocaleString('zh-CN')}` : '尚未发布，将使用默认样式')

function validate() {
  const urls = [config.value.global.logoUrl, config.value.global.logoDarkUrl, config.value.global.background.imageUrl, config.value.qr.iconUrl].filter(Boolean) as string[]
  if (urls.some(url => !/^https:\/\/[^\s]+$/i.test(url))) {
    message.error('Logo、背景图和扫码图标必须使用 HTTPS URL')
    return false
  }
  if (![config.value.global.background.color, config.value.global.primaryColor].every(color => /^#[0-9a-f]{6}$/i.test(color))) {
    message.error('颜色必须是六位十六进制值，例如 #0369A1')
    return false
  }
  if (config.value.global.background.overlayColor && !/^#[0-9a-f]{6}$/i.test(config.value.global.background.overlayColor)) {
    message.error('背景遮罩颜色必须是六位十六进制值')
    return false
  }
  if (!config.value.global.title.trim() || !config.value.global.subtitle.trim() || config.value.global.title.length > 200 || config.value.global.subtitle.length > 500) {
    message.error('标题最多 200 个字符，副标题最多 500 个字符且不能为空')
    return false
  }
  if (!config.value.standard.methods.length) {
    message.error('至少启用一种常规登录方式')
    return false
  }
  if (config.value.standard.methods.includes('social') && !socialProviderIds.value.length) {
    message.error('启用社会化登录后，至少选择一个已启用身份源')
    return false
  }
  if ((config.value.global.customCss ?? '').match(/<script|javascript:|[{}]|@import/i)) {
    message.error('自定义 CSS 只能包含门户根节点的样式声明')
    return false
  }
  return true
}

async function saveDraft() {
  if (!validate()) return
  try { await save(); message.success('草稿已保存，线上登录页未改变') }
  catch (error) { message.error(error instanceof Error ? error.message : '保存草稿失败') }
}

async function publishDraft() {
  if (!validate()) return
  try {
    if (dirty.value) await save()
    await publish()
    message.success('登录页样式和法律条款已发布')
  }
  catch (error) { message.error(error instanceof Error ? error.message : '发布失败') }
}

async function resetDraft() {
  const confirmed = await Modal.confirm({ title: '恢复默认配置', content: '样式和法律条款草稿都会恢复默认，线上已发布内容不会改变。确定继续吗？', okText: '恢复默认', cancelText: '取消' })
  if (!confirmed) return
  try { await reset(); message.success('草稿已恢复默认，请保存或发布') }
  catch (error) { message.error(error instanceof Error ? error.message : '恢复默认失败') }
}

async function loadSocialSources() {
  socialSourcesLoading.value = true
  try {
    const response = await socialIdentitySourceApi.getList({ status: 'active', page: 1, pageSize: 100 })
    socialSources.value = response.items.filter(source => source.status === 'active')
  } catch (error) {
    message.error('加载社会化身份源失败')
  } finally {
    socialSourcesLoading.value = false
  }
}

onMounted(async () => {
  try { await Promise.all([load(), loadSocialSources()]) }
  catch (error) { message.error(error instanceof Error ? error.message : '加载登录页草稿失败') }
})
</script>

<template>
  <div class="builder-page" :class="{ fullscreen }">
    <header class="builder-header">
      <div class="builder-title"><div class="title-mark"><Palette :size="18" aria-hidden="true" /></div><div><h1>个性化登录页面</h1><p>{{ status }} · {{ publishedText }}</p></div></div>
      <div class="builder-actions"><button class="button button-ghost" :disabled="loading || saving || publishing || resetting" @click="resetDraft">恢复默认</button><button class="button button-secondary" :disabled="loading || saving || publishing || resetting || !dirty" @click="saveDraft">{{ saving ? '保存中…' : '保存草稿' }}</button><button class="button button-primary" :disabled="loading || saving || publishing || resetting" @click="publishDraft">{{ publishing ? '发布中…' : '发布' }}</button></div>
    </header>

    <div class="builder-tabs">
      <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key; inspectorOpen = false"><span>{{ tab.label }}</span><small>{{ tab.description }}</small></button>
    </div>

    <main class="builder-workspace">
      <section class="preview-column">
        <PreviewToolbar v-model:device="device" :fullscreen="fullscreen" @toggle-fullscreen="fullscreen = !fullscreen" />
        <div v-if="loading" class="builder-loading">正在加载草稿…</div>
        <LoginStylePreview v-else :config="config" :legal-documents="legalDocuments" :tab="activeTab" :device="device" />
      </section>
      <button class="inspector-toggle" type="button" @click="inspectorOpen = !inspectorOpen">{{ inspectorOpen ? '关闭配置' : '打开配置' }}</button>
      <section class="inspector-column" :class="{ open: inspectorOpen }">
        <div class="mobile-inspector-header"><strong>配置面板</strong><button type="button" @click="inspectorOpen = false">×</button></div>
        <StyleInspector :config="config" :legal-documents="legalDocuments" :social-provider-ids="socialProviderIds" :social-sources="socialSources" :social-sources-loading="socialSourcesLoading" :tab="activeTab" @change="markDirty" @update:social-provider-ids="socialProviderIds = $event; markDirty()" />
      </section>
    </main>

    <div class="builder-footnote">草稿不会影响真实 auth-portal，只有发布后才会切换线上登录页。扫码登录仅用于编辑器预览。</div>
  </div>
</template>

<style scoped>
.builder-page { display: grid; grid-template-rows: auto auto minmax(0, 1fr) auto; min-height: calc(100vh - 64px); color: #102a43; background: #f4f8fb; }
.builder-page.fullscreen { position: fixed; z-index: 100; inset: 0; min-height: 100vh; }
.builder-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1.1rem 1.5rem; border-bottom: 1px solid #dbe4ee; background: #fff; }
.builder-title { display: flex; align-items: center; gap: .75rem; min-width: 0; }
.title-mark { display: grid; place-items: center; width: 2.25rem; height: 2.25rem; border-radius: .7rem; color: #fff; background: #0369a1; box-shadow: 0 8px 18px rgba(3,105,161,.22); }
h1 { margin: 0; font-size: 1.1rem; letter-spacing: -.02em; }
.builder-title p { margin: .25rem 0 0; color: #8295a8; font-size: .72rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.builder-actions { display: flex; gap: .5rem; flex-shrink: 0; }
.button { min-height: 2.3rem; border: 1px solid transparent; border-radius: .55rem; padding: .55rem .85rem; cursor: pointer; font-size: .78rem; font-weight: 750; }
.button:disabled { cursor: not-allowed; opacity: .5; }
.button-primary { color: #fff; background: #0369a1; }
.button-secondary { border-color: #b8cad9; color: #075985; background: #fff; }
.button-ghost { color: #64748b; background: transparent; }
.builder-tabs { display: flex; gap: .25rem; padding: .7rem 1.5rem 0; border-bottom: 1px solid #dbe4ee; background: #fff; }
.builder-tabs button { display: grid; gap: .18rem; border: 0; border-bottom: 2px solid transparent; padding: .65rem .9rem .8rem; color: #64748b; background: transparent; cursor: pointer; text-align: left; }
.builder-tabs button.active { border-bottom-color: #0369a1; color: #075985; }
.builder-tabs span { font-size: .82rem; font-weight: 800; }
.builder-tabs small { font-size: .68rem; }
.builder-workspace { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) 340px; min-height: 0; overflow: hidden; }
.preview-column { min-width: 0; overflow: auto; }
.inspector-column { min-width: 0; overflow: hidden; border-left: 1px solid #dbe4ee; background: #fff; }
.builder-loading { display: grid; place-items: center; min-height: 620px; color: #8295a8; font-size: .85rem; }
.builder-footnote { padding: .65rem 1.5rem; border-top: 1px solid #dbe4ee; color: #8295a8; background: #fff; font-size: .7rem; }
.inspector-toggle, .mobile-inspector-header { display: none; }
@media (max-width: 900px) {
  .builder-header { align-items: flex-start; flex-direction: column; padding: 1rem; }
  .builder-actions { width: 100%; }
  .builder-actions .button { flex: 1; }
  .builder-tabs { padding-left: .75rem; padding-right: .75rem; overflow-x: auto; }
  .builder-workspace { display: block; }
  .inspector-column { position: absolute; z-index: 20; inset: 0 0 0 18%; display: none; box-shadow: -16px 0 36px rgba(15, 23, 42, .16); }
  .inspector-column.open { display: block; }
  .mobile-inspector-header { display: flex; align-items: center; justify-content: space-between; padding: .85rem 1rem; border-bottom: 1px solid #e5edf4; }
  .mobile-inspector-header button { border: 0; color: #64748b; background: transparent; cursor: pointer; font-size: 1.3rem; }
  .inspector-toggle { position: absolute; z-index: 5; right: 1rem; bottom: 1rem; display: block; border: 1px solid #b8cad9; border-radius: 999px; padding: .65rem .9rem; color: #075985; background: #fff; box-shadow: 0 8px 20px rgba(15,23,42,.12); font-size: .75rem; font-weight: 800; }
}
@media (max-width: 480px) { .builder-title p { max-width: 300px; } .inspector-column { left: 8%; } .builder-footnote { padding: .6rem 1rem; } }
</style>
