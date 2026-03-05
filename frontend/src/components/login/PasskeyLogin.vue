<script setup lang="ts">
import { Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/composables/useAuth'

const { loading, passkeyLogin } = useAuth()

async function handlePasskeyLogin() {
  if (!window.PublicKeyCredential) {
    ElMessage.error('您的浏览器不支持 Passkey，请使用现代浏览器')
    return
  }
  
  await passkeyLogin()
}
</script>

<template>
  <div class="passkey-login">
    <div class="passkey-info">
      <el-icon :size="48" color="#409eff">
        <Key />
      </el-icon>
      <h3>Passkey 登录</h3>
      <p>使用生物识别或安全密钥快速登录</p>
    </div>

    <el-button
      type="primary"
      size="large"
      :loading="loading"
      style="width: 100%"
      @click="handlePasskeyLogin"
    >
      <el-icon><Key /></el-icon>
      使用 Passkey 登录
    </el-button>

    <div class="passkey-tips">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          Passkey 是一种更安全、更便捷的登录方式
        </template>
        <p>支持指纹、面部识别或安全密钥</p>
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.passkey-login {
  text-align: center;
}

.passkey-info {
  margin-bottom: 24px;
}

.passkey-info h3 {
  margin: 16px 0 8px;
  font-size: 18px;
  color: #303133;
}

.passkey-info p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.passkey-tips {
  margin-top: 20px;
  text-align: left;
}

.passkey-tips p {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}
</style>
