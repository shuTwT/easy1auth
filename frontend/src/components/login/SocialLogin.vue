<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'

const { loading, socialLogin } = useAuth()

const socialProviders = [
  {
    name: '微信',
    icon: 'ChatDotRound',
    provider: 'wechat',
    color: '#07c160'
  },
  {
    name: '钉钉',
    icon: 'ChatLineSquare',
    provider: 'dingtalk',
    color: '#0089ff'
  },
  {
    name: '飞书',
    icon: 'ChatRound',
    provider: 'feishu',
    color: '#3370ff'
  },
  {
    name: 'GitHub',
    icon: 'Eleme',
    provider: 'github',
    color: '#24292e'
  }
]

function handleSocialLogin(provider: string) {
  socialLogin(provider)
}
</script>

<template>
  <div class="social-login">
    <div class="divider">
      <span>第三方账号登录</span>
    </div>

    <div class="social-buttons">
      <el-tooltip
        v-for="item in socialProviders"
        :key="item.provider"
        :content="item.name"
        placement="top"
      >
        <el-button
          circle
          size="large"
          :loading="loading"
          :style="{ backgroundColor: item.color, borderColor: item.color }"
          @click="handleSocialLogin(item.provider)"
        >
          <el-icon color="white">
            <component :is="item.icon" />
          </el-icon>
        </el-button>
      </el-tooltip>
    </div>
  </div>
</template>

<script lang="ts">
import { ChatDotRound, ChatLineSquare, ChatRound, Eleme } from '@element-plus/icons-vue'

export default {
  components: {
    ChatDotRound,
    ChatLineSquare,
    ChatRound,
    Eleme
  }
}
</script>

<style scoped>
.social-login {
  margin-top: 24px;
}

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background-color: #dcdfe6;
}

.divider span {
  padding: 0 16px;
  font-size: 14px;
  color: #909399;
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.social-buttons .el-button {
  width: 48px;
  height: 48px;
}
</style>
