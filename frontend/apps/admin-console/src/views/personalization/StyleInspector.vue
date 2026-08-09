<script setup lang="ts">
import type { LoginStyleConfig, LoginStyleLegalDocuments } from '@easy1auth/types'

const props = defineProps<{
  config: LoginStyleConfig
  legalDocuments: LoginStyleLegalDocuments
  tab: 'global' | 'standard' | 'qr'
}>()
const emit = defineEmits<{ (event: 'change'): void }>()

function changed() { emit('change') }
function toggleMethod(method: 'password' | 'email' | 'social') {
  const methods = props.config.standard.methods
  const index = methods.indexOf(method)
  if (index >= 0) methods.splice(index, 1)
  else methods.push(method)
  if (!methods.length) methods.push('password')
  changed()
}
</script>

<template>
  <aside class="inspector-panel">
    <section v-if="tab === 'global'" class="inspector-section">
      <div class="section-heading"><div><strong>整体样式</strong><span>登录页的品牌基础和布局</span></div></div>
      <label>背景模式<select v-model="config.global.background.mode" @change="changed"><option value="solid">纯色</option><option value="image">图片</option></select></label>
      <label>背景颜色<div class="color-row"><input v-model="config.global.background.color" type="color" @input="changed"><input v-model="config.global.background.color" class="text-input" placeholder="#f5f7fa" @input="changed"></div></label>
      <label v-if="config.global.background.mode === 'image'">背景图片 URL<input v-model="config.global.background.imageUrl" class="text-input" placeholder="https://cdn.example.com/background.jpg" @input="changed"></label>
      <label v-if="config.global.background.mode === 'image'">遮罩透明度<input v-model.number="config.global.background.overlayOpacity" type="range" min="0" max="1" step="0.05" @input="changed"><span class="range-value">{{ Math.round((config.global.background.overlayOpacity ?? 0) * 100) }}%</span></label>
      <label>Logo URL<input v-model="config.global.logoUrl" class="text-input" placeholder="https://cdn.example.com/logo.svg" @input="changed"></label>
      <label>深色 Logo URL<input v-model="config.global.logoDarkUrl" class="text-input" placeholder="https://cdn.example.com/logo-dark.svg" @input="changed"></label>
      <label>主色<div class="color-row"><input v-model="config.global.primaryColor" type="color" @input="changed"><input v-model="config.global.primaryColor" class="text-input" placeholder="#0369A1" @input="changed"></div></label>
      <div class="two-column">
        <label>卡片宽度<input v-model.number="config.global.card.width" type="number" min="320" max="720" @input="changed"></label>
        <label>圆角<input v-model.number="config.global.card.radius" type="number" min="0" max="32" @input="changed"></label>
      </div>
      <label>卡片位置<select v-model="config.global.card.position" @change="changed"><option value="left">左侧</option><option value="center">居中</option><option value="right">右侧</option></select></label>
      <label class="switch-row"><input v-model="config.global.card.shadow" type="checkbox" @change="changed"><span>显示卡片阴影</span></label>
    </section>

    <section v-else-if="tab === 'standard'" class="inspector-section">
      <div class="section-heading"><div><strong>常规登录</strong><span>配置真实授权页可用的入口</span></div></div>
      <label>页面标题<input v-model="config.global.title" class="text-input" maxlength="200" @input="changed"></label>
      <label>页面副标题<input v-model="config.global.subtitle" class="text-input" maxlength="500" @input="changed"></label>
      <div class="option-list"><span class="field-label">登录方式</span><label><input type="checkbox" :checked="config.standard.methods.includes('password')" @change="toggleMethod('password')">密码登录</label><label><input type="checkbox" :checked="config.standard.methods.includes('email')" @change="toggleMethod('email')">邮箱验证码</label><label><input type="checkbox" :checked="config.standard.methods.includes('social')" @change="toggleMethod('social')">社会化登录</label></div>
      <label class="switch-row"><input v-model="config.standard.registrationEnabled" type="checkbox" @change="changed"><span>显示注册入口</span></label>
      <label class="switch-row"><input v-model="config.standard.termsRequired" type="checkbox" @change="changed"><span>登录前必须同意法律条款</span></label>
      <div class="legal-fields">
        <label>服务条款<textarea v-model="legalDocuments.termsOfService" rows="5" maxlength="10000" placeholder="支持纯文本或 Markdown" @input="changed"></textarea></label>
        <label>隐私条款<textarea v-model="legalDocuments.privacyPolicy" rows="5" maxlength="10000" placeholder="支持纯文本或 Markdown" @input="changed"></textarea></label>
      </div>
    </section>

    <section v-else class="inspector-section">
      <div class="section-heading"><div><strong>扫码登录</strong><span>仅保存和预览，不影响真实授权页</span></div></div>
      <label class="switch-row"><input v-model="config.qr.enabled" type="checkbox" @change="changed"><span>启用扫码预览</span></label>
      <label>扫码标题<input v-model="config.qr.title" class="text-input" maxlength="200" @input="changed"></label>
      <label>扫码副标题<input v-model="config.qr.subtitle" class="text-input" maxlength="500" @input="changed"></label>
      <label>扫码图标 URL<input v-model="config.qr.iconUrl" class="text-input" placeholder="https://cdn.example.com/qr-icon.svg" @input="changed"></label>
      <div class="info-note">扫码登录暂未接入真实认证。发布后，auth-portal 不会显示扫码入口或调用扫码接口。</div>
    </section>

    <section class="inspector-section advanced-section">
      <div class="section-heading"><div><strong>高级设置</strong><span>仅作用于授权门户根节点</span></div></div>
      <label>自定义 CSS<textarea v-model="config.global.customCss" rows="6" maxlength="12000" placeholder="例如：font-family: Inter, sans-serif;" @input="changed"></textarea></label>
      <div class="warning-note">只支持作用于门户根节点的 CSS 声明，不支持选择器、花括号、@import、&lt;script&gt; 或 javascript:。</div>
    </section>
  </aside>
</template>

<style scoped>
.inspector-panel { height: 100%; overflow: auto; background: #fff; }
.inspector-section { display: grid; gap: 1rem; padding: 1.25rem; border-bottom: 1px solid #e5edf4; }
.section-heading { display: flex; justify-content: space-between; margin-bottom: .15rem; }
.section-heading strong { display: block; color: #102a43; font-size: .94rem; }
.section-heading span { display: block; margin-top: .25rem; color: #8295a8; font-size: .72rem; line-height: 1.4; }
label, .field-label { display: grid; gap: .45rem; color: #52677d; font-size: .76rem; font-weight: 700; }
.text-input, select, textarea, input[type='number'] { width: 100%; border: 1px solid #d7e2ec; border-radius: .55rem; padding: .58rem .65rem; color: #102a43; background: #fff; outline: none; font-size: .78rem; font-weight: 500; }
textarea { min-height: 5rem; resize: vertical; line-height: 1.55; }
.text-input:focus, select:focus, textarea:focus, input[type='number']:focus { border-color: #0369a1; box-shadow: 0 0 0 3px rgba(3,105,161,.1); }
.color-row { display: grid; grid-template-columns: 2.4rem 1fr; gap: .5rem; align-items: center; }
input[type='color'] { width: 2.4rem; height: 2.15rem; padding: .15rem; border: 1px solid #d7e2ec; border-radius: .5rem; background: #fff; cursor: pointer; }
.two-column { display: grid; grid-template-columns: 1fr 1fr; gap: .65rem; }
.switch-row, .option-list label { display: flex; grid-template-columns: none; align-items: center; gap: .55rem; font-weight: 600; }
.switch-row input, .option-list input { accent-color: #0369a1; }
.option-list { display: grid; gap: .55rem; }
.option-list .field-label { margin-bottom: .1rem; }
.range-value { color: #8295a8; font-size: .72rem; font-weight: 600; }
.info-note, .warning-note { padding: .7rem .75rem; border-radius: .55rem; font-size: .72rem; line-height: 1.55; }
.info-note { color: #075985; background: #e0f2fe; }
.warning-note { color: #92400e; background: #fff7ed; }
.advanced-section { border-bottom: 0; }
@media (max-width: 900px) { .inspector-panel { position: absolute; inset: 0; } }
</style>
