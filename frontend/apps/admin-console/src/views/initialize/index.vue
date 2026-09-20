<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Button, Form, FormItem, Input, message } from 'antdv-next'
import type { FormInstance } from 'antdv-next'
import { Check, KeyRound, LockKeyhole, Mail, ShieldCheck, UserRoundCog } from '@lucide/vue'
import { systemInitializationApi } from '@/api/systemInitialization'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const model = reactive({
  account: '',
  password: '',
  confirmPassword: '',
})

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,128}$/
const passwordRules = [
  { required: true, message: '请输入管理员密码' },
  {
    pattern: passwordPattern,
    message: '密码需为 8–128 位，并包含大写字母、小写字母和数字',
  },
]
const confirmPasswordRules = [
  { required: true, message: '请再次输入密码' },
  {
    validator: async (_rule: unknown, value: string) => {
      if (value === model.password) {
        return Promise.resolve()
      }
      return Promise.reject(new Error('两次输入的密码不一致'))
    },
  },
]

async function handleSubmit() {
  submitting.value = true
  try {
    await systemInitializationApi.initialize({
      account: model.account.trim(),
      password: model.password,
    })
    message.success('系统初始化完成，请使用管理员账号登录')
    await router.replace({ name: 'Login' })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '系统初始化失败，请稍后重试')
    const initialized = await systemInitializationApi.getStatus(true).catch(() => false)
    if (initialized) {
      await router.replace({ name: 'Login' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="initialize-page">
    <div class="ambient ambient-top" aria-hidden="true"></div>
    <div class="ambient ambient-bottom" aria-hidden="true"></div>

    <section class="initialize-shell" aria-labelledby="initialize-title">
      <aside class="trust-panel">
        <div>
          <div class="brand-mark" aria-label="Easy1Auth">
            <ShieldCheck :size="30" stroke-width="1.8" />
          </div>
          <p class="eyebrow">EASY1AUTH · FIRST RUN</p>
          <h1 id="initialize-title">
            <span>建立安全的</span>
            <span>管理入口</span>
          </h1>
          <p class="trust-copy">
            创建首个系统管理员后，Easy1Auth 将启用系统租户和平台级管理权限。
          </p>
        </div>

        <div class="security-summary" aria-label="初始化内容">
          <div class="summary-item">
            <span class="summary-icon"><UserRoundCog :size="20" /></span>
            <span>
              <strong>系统管理员</strong>
              <small>拥有平台配置与租户管理权限</small>
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-icon"><LockKeyhole :size="20" /></span>
            <span>
              <strong>一次性初始化</strong>
              <small>完成后此入口将自动关闭</small>
            </span>
          </div>
        </div>

        <p class="trust-footnote">
          <Check :size="16" aria-hidden="true" />
          初始化过程在单个数据库事务中完成
        </p>
      </aside>

      <div class="form-panel">
        <div class="form-heading">
          <span class="step-label">初始化设置</span>
          <h2>创建系统管理员</h2>
          <p>该账号将作为平台的最高权限管理员，请妥善保管登录信息。</p>
        </div>

        <Form
          ref="formRef"
          :model="model"
          layout="vertical"
          required-mark="optional"
          :disabled="submitting"
          scroll-to-first-error
          class="initialize-form"
          @finish="handleSubmit"
        >
          <FormItem
            label="系统管理员账号"
            name="account"
            :rules="[
              { required: true, message: '请输入系统管理员账号' },
              { type: 'email', message: '请输入有效的邮箱地址' },
              { max: 320, message: '邮箱地址不能超过 320 个字符' },
            ]"
          >
            <Input
              v-model:value="model.account"
              size="large"
              type="email"
              autocomplete="username"
              placeholder="admin@example.com"
            >
              <template #prefix><Mail :size="18" aria-hidden="true" /></template>
            </Input>
            <p class="field-hint">使用邮箱作为管理员登录账号，后续也可用于安全通知。</p>
          </FormItem>

          <FormItem label="管理员密码" name="password" :rules="passwordRules" has-feedback>
            <Input.Password
              v-model:value="model.password"
              size="large"
              autocomplete="new-password"
              placeholder="设置高强度密码"
              @change="formRef?.validateFields(['confirmPassword'])"
            >
              <template #prefix><KeyRound :size="18" aria-hidden="true" /></template>
            </Input.Password>
          </FormItem>

          <div class="password-policy" aria-label="密码要求">
            <span :class="{ met: model.password.length >= 8 }"><Check :size="14" />至少 8 位</span>
            <span :class="{ met: /[A-Z]/.test(model.password) && /[a-z]/.test(model.password) }"><Check :size="14" />大小写字母</span>
            <span :class="{ met: /\d/.test(model.password) }"><Check :size="14" />至少 1 个数字</span>
          </div>

          <FormItem label="确认密码" name="confirmPassword" :rules="confirmPasswordRules" has-feedback>
            <Input.Password
              v-model:value="model.confirmPassword"
              size="large"
              autocomplete="new-password"
              placeholder="再次输入管理员密码"
            >
              <template #prefix><LockKeyhole :size="18" aria-hidden="true" /></template>
            </Input.Password>
          </FormItem>

          <Button type="primary" html-type="submit" size="large" block :loading="submitting" class="submit-button">
            {{ submitting ? '正在初始化…' : '完成初始化' }}
          </Button>
        </Form>

        <p class="form-footnote">
          提交即表示创建系统租户并授予此账号平台最高管理权限。
        </p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.initialize-page {
  position: relative;
  display: grid;
  min-height: 100vh;
  min-height: 100dvh;
  place-items: center;
  overflow: hidden;
  padding: 32px;
  background:
    radial-gradient(circle at 10% 5%, rgba(14, 165, 233, 0.12), transparent 30%),
    radial-gradient(circle at 95% 90%, rgba(3, 105, 161, 0.10), transparent 34%),
    var(--color-background);
}

.ambient {
  position: absolute;
  border: 1px solid rgba(3, 105, 161, 0.12);
  border-radius: 999px;
  pointer-events: none;
}

.ambient-top {
  width: 420px;
  height: 420px;
  top: -260px;
  right: 8%;
}

.ambient-bottom {
  width: 280px;
  height: 280px;
  bottom: -190px;
  left: 7%;
}

.initialize-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(300px, 0.86fr) minmax(420px, 1.14fr);
  width: min(100%, 960px);
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--border-radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-lg);
}

.trust-panel {
  display: flex;
  min-height: 630px;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 42px;
  color: #fff;
  background:
    linear-gradient(150deg, rgba(56, 189, 248, 0.10), transparent 42%),
    linear-gradient(180deg, var(--color-sidebar), var(--color-sidebar-deep));
}

.brand-mark {
  display: grid;
  width: 54px;
  height: 54px;
  margin-bottom: 40px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 14px;
  place-items: center;
  color: #fff;
  background: rgba(56, 189, 248, 0.16);
}

.eyebrow {
  margin: 0 0 14px;
  color: #7DD3FC;
  font-family: 'Fira Code', ui-monospace, monospace;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.trust-panel h1 {
  max-width: 330px;
  margin: 0;
  color: #fff;
  font-size: clamp(30px, 4vw, 40px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.035em;
}

.trust-panel h1 span {
  display: block;
}

.trust-copy {
  max-width: 340px;
  margin: 20px 0 0;
  color: #C7D9E8;
  font-size: 15px;
  line-height: 1.75;
}

.security-summary {
  display: grid;
  gap: 22px;
  margin: auto 0 44px;
  padding-top: 56px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 10px;
  place-items: center;
  color: #7DD3FC;
  background: rgba(255, 255, 255, 0.08);
}

.summary-item strong,
.summary-item small {
  display: block;
}

.summary-item strong {
  font-size: 14px;
  font-weight: 600;
}

.summary-item small {
  margin-top: 3px;
  color: #B7C9D9;
  font-size: 12px;
  line-height: 1.5;
}

.trust-footnote {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #B7C9D9;
  font-size: 12px;
}

.form-panel {
  display: flex;
  min-height: 630px;
  flex-direction: column;
  justify-content: center;
  padding: 48px 56px;
}

.form-heading {
  margin-bottom: 30px;
}

.step-label {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  font-size: 12px;
  font-weight: 600;
}

.form-heading h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.025em;
}

.form-heading p {
  margin: 9px 0 0;
  color: var(--color-text-muted);
  font-size: 14px;
  line-height: 1.65;
}

.initialize-form :deep(.ant-form-item) {
  margin-bottom: 21px;
}

.initialize-form :deep(.ant-form-item-label > label) {
  color: var(--color-text);
  font-weight: 600;
}

.initialize-form :deep(.ant-input-affix-wrapper) {
  border-radius: 8px;
}

.initialize-form :deep(.ant-input-prefix) {
  margin-right: 10px;
  color: var(--color-text-subtle);
}

.field-hint {
  margin: 7px 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.password-policy {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin: -11px 0 20px;
}

.password-policy span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-subtle);
  font-size: 12px;
  transition: color var(--transition-fast);
}

.password-policy span.met {
  color: var(--color-success);
}

.submit-button {
  height: 44px;
  margin-top: 3px;
  border-radius: 8px;
  font-weight: 600;
  box-shadow: 0 8px 20px rgba(3, 105, 161, 0.18);
}

.form-footnote {
  margin: 22px 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

@media (max-width: 767px) {
  .initialize-page {
    align-items: start;
    padding: 16px;
  }

  .initialize-shell {
    grid-template-columns: 1fr;
  }

  .trust-panel {
    min-height: auto;
    padding: 30px 26px;
  }

  .brand-mark {
    margin-bottom: 24px;
  }

  .trust-panel h1 {
    font-size: 28px;
  }

  .security-summary {
    margin-bottom: 28px;
    padding-top: 32px;
  }

  .form-panel {
    min-height: auto;
    padding: 34px 24px 38px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .password-policy span {
    transition: none;
  }
}
</style>
