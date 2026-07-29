<script setup lang="ts">
import { computed } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { KeyRound } from '@lucide/vue'
import {
  PROVIDER_CONFIGS,
  isSocialProviderType,
  type SocialProviderType,
} from '@/types/socialIdentityProvider'

const props = defineProps<{
  providers?: readonly string[]
}>()

const { loading, socialLogin } = useAuth()

const providerIcons = {
  oidc: KeyRound,
} satisfies Record<SocialProviderType, typeof KeyRound>

const socialProviders = computed(() =>
  (props.providers ?? [])
    .filter(isSocialProviderType)
    .map((provider) => ({
      ...PROVIDER_CONFIGS[provider],
      provider,
      icon: providerIcons[provider],
    })),
)

function handleSocialLogin(provider: string) {
  socialLogin(provider)
}
</script>

<template>
  <div v-if="socialProviders.length > 0" class="mt-6 flex flex-col gap-5">
    <div class="flex items-center gap-4">
      <Separator class="flex-1" />
      <span class="text-sm text-muted-foreground">第三方账号登录</span>
      <Separator class="flex-1" />
    </div>

    <div class="flex flex-wrap justify-center gap-3">
      <TooltipProvider>
        <Tooltip v-for="item in socialProviders" :key="item.provider">
          <TooltipTrigger as-child>
            <Button
              variant="outline"
              size="icon-lg"
              :aria-label="item.name"
              :disabled="loading"
              :style="{ backgroundColor: item.color, borderColor: item.color, color: 'white' }"
              class="size-12 rounded-full"
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
