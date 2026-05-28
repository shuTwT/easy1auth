<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import { Copy } from '@lucide/vue'
import { applicationApi } from '@/api/application'
import type { Application } from '@/types/application'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

const loading = ref(false)
const applications = ref<Application[]>([])
const baseUrl = ref(window.location.origin)
const stats = ref({
  totalApps: 0,
  activeApps: 0,
  webApps: 0,
  spaApps: 0,
  nativeApps: 0,
  machineApps: 0
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList({ pageSize: 1000 })
    applications.value = res.data.applications
    
    stats.value = {
      totalApps: res.data.total,
      activeApps: applications.value.filter(app => app.status === 'active').length,
      webApps: applications.value.filter(app => app.type === 'web').length,
      spaApps: applications.value.filter(app => app.type === 'spa').length,
      nativeApps: applications.value.filter(app => app.type === 'native').length,
      machineApps: applications.value.filter(app => app.type === 'machine').length
    }
  } catch (error) {
    console.error('加载应用列表失败:', error)
    toast.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

const getTypeText = (type: string) => {
  switch (type) {
    case 'web':
      return 'Web应用'
    case 'spa':
      return '单页应用'
    case 'native':
      return '原生应用'
    case 'machine':
      return '机器对机器'
    default:
      return '未知'
  }
}

const getTypeVariant = (type: string) => {
  switch (type) {
    case 'web':
      return 'default'
    case 'spa':
      return 'secondary'
    case 'native':
      return 'outline'
    case 'machine':
      return 'secondary'
    default:
      return 'secondary'
  }
}

const getStatusText = (status: string) => {
  return status === 'active' ? '启用' : '禁用'
}

const getStatusVariant = (status: string) => {
  return status === 'active' ? 'default' : 'destructive'
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    toast.success('已复制到剪贴板')
  }).catch(() => {
    toast.error('复制失败')
  })
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="sso-management p-5">
    <div class="grid grid-cols-6 gap-5 mb-5">
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.totalApps }}</div>
            <div class="text-sm text-muted-foreground">应用总数</div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.activeApps }}</div>
            <div class="text-sm text-muted-foreground">启用应用</div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.webApps }}</div>
            <div class="text-sm text-muted-foreground">Web应用</div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.spaApps }}</div>
            <div class="text-sm text-muted-foreground">单页应用</div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.nativeApps }}</div>
            <div class="text-sm text-muted-foreground">原生应用</div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ stats.machineApps }}</div>
            <div class="text-sm text-muted-foreground">机器应用</div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <CardTitle>单点登录配置</CardTitle>
      </CardHeader>
      <CardContent>
        <Alert class="mb-5">
          <AlertTitle>单点登录说明</AlertTitle>
          <AlertDescription>
            <p>单点登录（SSO）允许用户使用一个账号登录多个应用。系统支持 OAuth 2.0 和 OpenID Connect 协议。</p>
            <p class="mt-3">
              <strong>OAuth 2.0 授权端点：</strong>
              <Button variant="link" size="sm" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/authorize`)">
                {{ baseUrl }}/oauth2/authorize
              </Button>
            </p>
            <p class="mt-1">
              <strong>Token 端点：</strong>
              <Button variant="link" size="sm" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/token`)">
                {{ baseUrl }}/oauth2/token
              </Button>
            </p>
            <p class="mt-1">
              <strong>用户信息端点：</strong>
              <Button variant="link" size="sm" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/userinfo`)">
                {{ baseUrl }}/oauth2/userinfo
              </Button>
            </p>
          </AlertDescription>
        </Alert>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[200px]">应用名称</TableHead>
              <TableHead class="w-[120px]">应用类型</TableHead>
              <TableHead class="w-[300px]">Client ID</TableHead>
              <TableHead class="min-w-[200px]">回调地址</TableHead>
              <TableHead class="w-[150px] text-center">Access Token 有效期</TableHead>
              <TableHead class="w-[150px] text-center">Refresh Token 有效期</TableHead>
              <TableHead class="w-[100px] text-center">状态</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="7" class="text-center text-muted-foreground">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="applications.length === 0">
              <TableCell colspan="7" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="item in applications" :key="item.id">
              <TableCell>{{ item.name }}</TableCell>
              <TableCell>
                <Badge :variant="getTypeVariant(item.type)">
                  {{ getTypeText(item.type) }}
                </Badge>
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs">{{ item.clientId }}</span>
                  <Button variant="ghost" size="icon" class="h-6 w-6" @click="copyToClipboard(item.clientId)">
                    <Copy class="w-3 h-3" />
                  </Button>
                </div>
              </TableCell>
              <TableCell>
                <div v-if="Array.isArray(item.redirectUris)">
                  <div v-for="(uri, index) in item.redirectUris" :key="index" class="text-xs mb-1">
                    {{ uri }}
                  </div>
                </div>
                <span v-else class="text-muted-foreground">-</span>
              </TableCell>
              <TableCell class="text-center">{{ item.accessTokenLifetime }} 秒</TableCell>
              <TableCell class="text-center">{{ item.refreshTokenLifetime }} 秒</TableCell>
              <TableCell class="text-center">
                <Badge :variant="getStatusVariant(item.status)">
                  {{ getStatusText(item.status) }}
                </Badge>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </CardContent>
    </Card>

    <Card class="mt-5">
      <CardHeader>
        <CardTitle>集成指南</CardTitle>
      </CardHeader>
      <CardContent>
        <Tabs default-value="oauth2">
          <TabsList>
            <TabsTrigger value="oauth2">OAuth 2.0 授权码流程</TabsTrigger>
            <TabsTrigger value="pkce">PKCE 安全增强</TabsTrigger>
            <TabsTrigger value="client">客户端凭证流程</TabsTrigger>
          </TabsList>
          <TabsContent value="oauth2">
            <div class="p-5">
              <h4 class="text-base font-semibold mb-2">1. 获取授权码</h4>
              <p class="text-sm text-muted-foreground mb-2">将用户重定向到授权端点：</p>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE</pre>
              
              <h4 class="text-base font-semibold mt-5 mb-2">2. 使用授权码换取 Token</h4>
              <p class="text-sm text-muted-foreground mb-2">向 Token 端点发送 POST 请求：</p>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET</pre>
              
              <h4 class="text-base font-semibold mt-5 mb-2">3. 获取用户信息</h4>
              <p class="text-sm text-muted-foreground mb-2">使用 Access Token 获取用户信息：</p>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">GET {{ baseUrl }}/oauth2/userinfo
Authorization: Bearer ACCESS_TOKEN</pre>
            </div>
          </TabsContent>
          
          <TabsContent value="pkce">
            <div class="p-5">
              <h4 class="text-base font-semibold mb-2">1. 生成 Code Verifier 和 Code Challenge</h4>
              <p class="text-sm text-muted-foreground">Code Verifier: 随机字符串（43-128字符）</p>
              <p class="text-sm text-muted-foreground mb-2">Code Challenge: BASE64URL(SHA256(code_verifier))</p>
              
              <h4 class="text-base font-semibold mt-5 mb-2">2. 授权请求</h4>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE&code_challenge=CODE_CHALLENGE&code_challenge_method=S256</pre>
              
              <h4 class="text-base font-semibold mt-5 mb-2">3. Token 请求</h4>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&code_verifier=CODE_VERIFIER</pre>
            </div>
          </TabsContent>
          
          <TabsContent value="client">
            <div class="p-5">
              <h4 class="text-base font-semibold mb-2">适用于机器对机器通信</h4>
              <pre class="bg-muted p-3 rounded-md text-sm overflow-x-auto font-mono whitespace-pre-wrap break-all">POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&scope=read write</pre>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  </div>
</template>