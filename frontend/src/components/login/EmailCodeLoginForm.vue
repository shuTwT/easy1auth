<script setup lang="ts">
import { reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{
  switchToPassword: []
  switchToRegister: []
}>()

const { loading, sendingCode, countdown, isCountingDown, login, sendCode } = useAuth()

const form = reactive({
  email: '',
  code: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

async function handleSendCode() {
  if (!form.email) {
    return
  }
  
  await sendCode({
    email: form.email,
    type: 'login'
  })
}

async function handleSubmit() {
  await login({
    email: form.email,
    code: form.code,
    loginType: 'email'
  })
}
</script>

<template>
  <el-form :model="form" :rules="rules" @submit.prevent="handleSubmit">
    <el-form-item prop="email">
      <el-input
        v-model="form.email"
        placeholder="邮箱地址"
        prefix-icon="Message"
        size="large"
        clearable
      />
    </el-form-item>

    <el-form-item prop="code">
      <el-input
        v-model="form.code"
        placeholder="验证码"
        prefix-icon="Key"
        size="large"
        maxlength="6"
        @keyup.enter="handleSubmit"
      >
        <template #append>
          <el-button
            :disabled="isCountingDown || !form.email"
            :loading="sendingCode"
            @click="handleSendCode"
          >
            {{ isCountingDown ? `${countdown}s` : '获取验证码' }}
          </el-button>
        </template>
      </el-input>
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        style="width: 100%"
        @click="handleSubmit"
      >
        登录
      </el-button>
    </el-form-item>

    <div class="form-footer">
      <el-button
        type="text"
        @click="emit('switchToPassword')"
      >
        账号密码登录
      </el-button>
      <el-button
        type="text"
        @click="emit('switchToRegister')"
      >
        立即注册
      </el-button>
    </div>
  </el-form>
</template>

<style scoped>
.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
</style>
