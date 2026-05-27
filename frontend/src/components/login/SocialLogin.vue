<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Button } from '@/components/ui/button'
import { MessageCircle, MessageSquare, GitFork } from '@lucide/vue'

const { loading, socialLogin } = useAuth()

const socialProviders = [
  {
    name: '微信',
    icon: MessageCircle,
    provider: 'wechat',
    color: '#07c160'
  },
  {
    name: '钉钉',
    icon: MessageSquare,
    provider: 'dingtalk',
    color: '#0089ff'
  },
  {
    name: '飞书',
    icon: MessageCircle,
    provider: 'feishu',
    color: '#3370ff'
  },
  {
    name: 'GitHub',
    icon: GitFork,
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
      <TooltipProvider>
        <Tooltip v-for="item in socialProviders" :key="item.provider">
          <TooltipTrigger as-child>
            <Button
              variant="outline"
              size="icon-lg"
              :disabled="loading"
              :style="{ backgroundColor: item.color, borderColor: item.color, color: 'white' }"
              class="social-button"
              @click="handleSocialLogin(item.provider)"
            >
              <component :is="item.icon" class="size-5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>
            <p>{{ item.name }}</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    </div>
  </div>
</template>

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

.social-button {
  width: 48px !important;
  height: 48px !important;
  border-radius: 50% !important;
}
</style>
