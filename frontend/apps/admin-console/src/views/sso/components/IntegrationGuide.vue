<script setup lang="ts">
import { ref } from 'vue'
import type { GuideTab } from '../utils/types'

defineProps<{
  baseUrl: string
}>()

const guideTab = ref<GuideTab>('oauth2')
</script>

<template>
  <a-card class="mt-6">
    <div class="pt-6">
      <h3>集成指南</h3>
    </div>
    <div>
      <div class="flex gap-2">
        <a-button :type="guideTab === 'oauth2' ? 'primary' : 'default'" @click="guideTab = 'oauth2'">OAuth 2.0 授权码流程</a-button>
        <a-button :type="guideTab === 'pkce' ? 'primary' : 'default'" @click="guideTab = 'pkce'">PKCE 安全增强</a-button>
        <a-button :type="guideTab === 'client' ? 'primary' : 'default'" @click="guideTab = 'client'">客户端凭证流程</a-button>
      </div>

      <div v-show="guideTab === 'oauth2'" class="p-5">
        <h4 class="text-base font-semibold mb-2">1. 获取授权码</h4>
        <p class="text-sm text-[#52677D] mb-2">将用户重定向到授权端点：</p>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE</pre>

        <h4 class="text-base font-semibold mt-5 mb-2">2. 使用授权码换取 Token</h4>
        <p class="text-sm text-[#52677D] mb-2">向 Token 端点发送 POST 请求：</p>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET</pre>

        <h4 class="text-base font-semibold mt-5 mb-2">3. 获取用户信息</h4>
        <p class="text-sm text-[#52677D] mb-2">使用 Access Token 获取用户信息：</p>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">GET {{ baseUrl }}/oauth2/userinfo
Authorization: Bearer ACCESS_TOKEN</pre>
      </div>

      <div v-show="guideTab === 'pkce'" class="p-5">
        <h4 class="text-base font-semibold mb-2">1. 生成 Code Verifier 和 Code Challenge</h4>
        <p class="text-sm text-[#52677D]">Code Verifier: 随机字符串（43-128字符）</p>
        <p class="text-sm text-[#52677D] mb-2">Code Challenge: BASE64URL(SHA256(code_verifier))</p>

        <h4 class="text-base font-semibold mt-5 mb-2">2. 授权请求</h4>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE&code_challenge=CODE_CHALLENGE&code_challenge_method=S256</pre>

        <h4 class="text-base font-semibold mt-5 mb-2">3. Token 请求</h4>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&code_verifier=CODE_VERIFIER</pre>
      </div>

      <div v-show="guideTab === 'client'" class="p-5">
        <h4 class="text-base font-semibold mb-2">适用于机器对机器通信</h4>
        <pre class="bg-[#F4F8FB] p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&scope=read write</pre>
      </div>
    </div>
  </a-card>
</template>
