<template>
  <div class="personalization-settings p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground mb-2">个性化设置</h1>
      <p class="text-sm text-muted-foreground">自定义域名、登录页面样式和消息模板</p>
    </div>

    <Card>
      <CardContent class="pt-6">
        <Tabs v-model="activeTab">
          <TabsList class="mb-6">
            <TabsTrigger value="domains">自定义域名</TabsTrigger>
            <TabsTrigger value="loginStyle">登录页面</TabsTrigger>
            <TabsTrigger value="templates">消息模板</TabsTrigger>
          </TabsList>

          <TabsContent value="domains" class="px-2">
            <div class="flex justify-between items-center mb-5">
              <h3 class="text-base font-semibold">域名管理</h3>
              <Button @click="showDomainDialog = true">
                <Plus class="size-4 mr-2" />
                添加域名
              </Button>
            </div>

            <div class="rounded-md border mb-6">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead class="min-w-[200px]">域名</TableHead>
                    <TableHead class="w-[120px]">SSL状态</TableHead>
                    <TableHead class="w-[100px]">验证方式</TableHead>
                    <TableHead class="w-[180px]">创建时间</TableHead>
                    <TableHead class="w-[200px] text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-if="domainsLoading">
                    <TableCell colspan="5" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
                  </TableRow>
                  <TableRow v-else-if="domains.length === 0">
                    <TableCell colspan="5" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
                  </TableRow>
                  <TableRow v-for="domain in domains" :key="domain.id">
                    <TableCell>
                      <div class="flex items-center gap-2">
                        <span class="font-medium">{{ domain.domain }}</span>
                        <Badge v-if="domain.status === 'verified'" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已验证</Badge>
                        <Badge v-else-if="domain.status === 'pending'" variant="secondary" class="bg-yellow-500/10 text-yellow-600 hover:bg-yellow-500/20">待验证</Badge>
                        <Badge v-else-if="domain.status === 'verifying'" variant="outline">验证中</Badge>
                        <Badge v-else variant="destructive">验证失败</Badge>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge v-if="domain.sslStatus === 'active'" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已配置</Badge>
                      <Badge v-else variant="outline">未配置</Badge>
                    </TableCell>
                    <TableCell>{{ domain.verificationMethod === 'dns' ? 'DNS记录' : '文件验证' }}</TableCell>
                    <TableCell>{{ formatDate(domain.createdAt) }}</TableCell>
                    <TableCell class="text-right">
                      <div class="flex justify-end gap-2">
                        <Button v-if="domain.status !== 'verified'" variant="link" size="sm" class="h-auto p-0" disabled title="阶段 6 仅登记域名">验证未启用</Button>
                        <Button v-if="domain.status === 'verified'" variant="link" size="sm" class="h-auto p-0" disabled title="TLS 由外部网关管理">TLS由网关管理</Button>
                        <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDeleteDomain(domain)">删除</Button>
                      </div>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>

            <Alert class="bg-muted border">
              <Info class="size-4" />
              <AlertTitle class="flex items-center gap-2 font-semibold">
                阶段 6 域名管理说明
              </AlertTitle>
              <AlertDescription class="mt-2 space-y-2">
                <p>当前仅登记域名和验证令牌，不执行自动所有权验证。</p>
                <p>TLS 证书由部署网关或证书控制面管理，Easy1Auth 不接收证书私钥。</p>
              </AlertDescription>
            </Alert>
          </TabsContent>

          <TabsContent value="loginStyle" class="px-2">
            <form class="max-w-[800px]">
              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">基础信息</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">页面标题</label>
                    <Input v-model="loginStyle.title" placeholder="请输入登录页面标题" />
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">页面副标题</label>
                    <Input v-model="loginStyle.subtitle" placeholder="请输入登录页面副标题" />
                  </div>
                </div>
              </div>

              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">品牌元素</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">Logo</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="(options: { file: { raw: File } }) => handleLogoUpload(options, 'logo')"
                        :before-upload="beforeLogoUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="loginStyle.logo" :src="loginStyle.logo" class="w-[120px] h-[120px] object-contain rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-[120px] h-[120px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传Logo</span>
                        </div>
                      </Upload>
                      <Button v-if="loginStyle.logo" variant="link" class="text-destructive justify-start p-0 h-auto" @click="loginStyle.logo = undefined">
                        删除Logo
                      </Button>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">背景图片</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="(options: { file: { raw: File } }) => handleLogoUpload(options, 'backgroundImage')"
                        :before-upload="beforeBgUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="loginStyle.backgroundImage" :src="loginStyle.backgroundImage" class="w-full max-w-[400px] h-[200px] object-cover rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-full max-w-[400px] h-[200px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传背景图片</span>
                        </div>
                      </Upload>
                      <Button v-if="loginStyle.backgroundImage" variant="link" class="text-destructive justify-start p-0 h-auto" @click="loginStyle.backgroundImage = undefined">
                        删除背景图片
                      </Button>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">背景颜色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="loginStyle.backgroundColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="loginStyle.backgroundColor" placeholder="#f5f7fa" class="w-[200px]" />
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">主题色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="loginStyle.primaryColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="loginStyle.primaryColor" placeholder="#0369A1" class="w-[200px]" />
                    </div>
                    <div class="flex items-center gap-2 mt-3">
                      <span class="text-sm text-muted-foreground">预设颜色：</span>
                      <div 
                        v-for="color in colorPresets" 
                        :key="color.value" 
                        class="w-7 h-7 rounded flex items-center justify-center cursor-pointer transition-transform hover:scale-110 border-2 border-transparent"
                        :style="{ backgroundColor: color.value }"
                        @click="loginStyle.primaryColor = color.value"
                      >
                        <Check v-if="loginStyle.primaryColor === color.value" class="size-3.5 text-white" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">登录方式</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">启用方式</label>
                  <div class="flex flex-wrap gap-4">
                    <label class="flex items-center gap-2 cursor-pointer">
                      <Checkbox 
                        :checked="loginStyle.loginMethods?.includes('password')" 
                        @update:checked="toggleLoginMethod('password')"
                      />
                      <span class="text-sm">账号密码</span>
                    </label>
                    <label class="flex items-center gap-2 cursor-pointer">
                      <Checkbox 
                        :checked="loginStyle.loginMethods?.includes('email')" 
                        @update:checked="toggleLoginMethod('email')"
                      />
                      <span class="text-sm">邮箱验证码</span>
                    </label>
                    <label class="flex items-center gap-2 cursor-pointer">
                      <Checkbox 
                        :checked="loginStyle.loginMethods?.includes('passkey')" 
                        @update:checked="toggleLoginMethod('passkey')"
                      />
                      <span class="text-sm">Passkey</span>
                    </label>
                  </div>
                </div>
              </div>

              <div class="mb-8">
                <h3 class="text-base font-semibold mb-5">自定义样式</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">自定义CSS</label>
                  <Textarea
                    v-model="loginStyle.customCSS"
                    :rows="8"
                    placeholder="请输入自定义CSS样式"
                    class="font-mono text-[13px] leading-relaxed bg-muted/50"
                  />
                </div>
              </div>

              <Button @click="handleSaveLoginStyle" :disabled="savingLoginStyle">
                保存设置
              </Button>
            </form>
          </TabsContent>

          <TabsContent value="templates" class="px-2">
            <div class="flex justify-between items-center mb-5">
              <h3 class="text-base font-semibold">模板管理</h3>
              <div class="flex gap-3">
                <Button variant="outline" @click="handleInitTemplates" :disabled="initingTemplates">
                  初始化默认模板
                </Button>
                <Button @click="showTemplateDialog = true">
                  <Plus class="size-4 mr-2" />
                  新建模板
                </Button>
              </div>
            </div>

            <Tabs v-model="templateType">
              <TabsList class="mb-4">
                <TabsTrigger value="email">邮件模板</TabsTrigger>
                <TabsTrigger value="sms">短信模板</TabsTrigger>
              </TabsList>

              <TabsContent value="email">
                <div class="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead class="w-[150px]">模板名称</TableHead>
                        <TableHead class="w-[150px]">模板代码</TableHead>
                        <TableHead class="min-w-[200px]">邮件主题</TableHead>
                        <TableHead class="w-[80px]">默认</TableHead>
                        <TableHead class="w-[80px]">状态</TableHead>
                        <TableHead class="w-[150px] text-right">操作</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      <TableRow v-if="templatesLoading">
                        <TableCell colspan="6" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
                      </TableRow>
                      <TableRow v-else-if="emailTemplates.length === 0">
                        <TableCell colspan="6" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
                      </TableRow>
                      <TableRow v-for="template in emailTemplates" :key="template.id">
                        <TableCell>{{ template.name }}</TableCell>
                        <TableCell>{{ template.code }}</TableCell>
                        <TableCell>{{ template.subject }}</TableCell>
                        <TableCell>
                          <Badge v-if="template.isDefault" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</Badge>
                          <Badge v-else variant="outline">否</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge v-if="template.status === 'active'" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</Badge>
                          <Badge v-else variant="outline">禁用</Badge>
                        </TableCell>
                        <TableCell class="text-right">
                          <div class="flex justify-end gap-2">
                            <Button variant="link" size="sm" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</Button>
                            <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </div>
              </TabsContent>

              <TabsContent value="sms">
                <div class="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead class="w-[150px]">模板名称</TableHead>
                        <TableHead class="w-[150px]">模板代码</TableHead>
                        <TableHead class="min-w-[300px]">模板内容</TableHead>
                        <TableHead class="w-[80px]">默认</TableHead>
                        <TableHead class="w-[80px]">状态</TableHead>
                        <TableHead class="w-[150px] text-right">操作</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      <TableRow v-if="templatesLoading">
                        <TableCell colspan="6" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
                      </TableRow>
                      <TableRow v-else-if="smsTemplates.length === 0">
                        <TableCell colspan="6" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
                      </TableRow>
                      <TableRow v-for="template in smsTemplates" :key="template.id">
                        <TableCell>{{ template.name }}</TableCell>
                        <TableCell>{{ template.code }}</TableCell>
                        <TableCell class="max-w-[300px] truncate">{{ template.content }}</TableCell>
                        <TableCell>
                          <Badge v-if="template.isDefault" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</Badge>
                          <Badge v-else variant="outline">否</Badge>
                        </TableCell>
                        <TableCell>
                          <Badge v-if="template.status === 'active'" variant="default" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</Badge>
                          <Badge v-else variant="outline">禁用</Badge>
                        </TableCell>
                        <TableCell class="text-right">
                          <div class="flex justify-end gap-2">
                            <Button variant="link" size="sm" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</Button>
                            <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    </TableBody>
                  </Table>
                </div>
              </TabsContent>
            </Tabs>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>

    <Dialog v-model:open="showDomainDialog">
      <DialogContent class="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>添加域名</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">域名</label>
              <div class="flex">
                <div class="flex items-center px-3 bg-muted border border-r-0 rounded-l-md text-sm text-muted-foreground">https://</div>
                <Input v-model="domainForm.domain" placeholder="例如：login.yourcompany.com" class="rounded-l-none" />
              </div>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">验证方式</label>
              <RadioGroup v-model="domainForm.verificationMethod">
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="dns" />
                  <label class="text-sm cursor-pointer">DNS记录验证</label>
                </div>
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="file" />
                  <label class="text-sm cursor-pointer">文件验证</label>
                </div>
              </RadioGroup>
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="showDomainDialog = false">取消</Button>
          <Button @click="handleCreateDomain" :disabled="creatingDomain">添加</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="showSSLDialog">
      <DialogContent class="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>配置SSL证书</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">SSL证书</label>
              <Textarea
                v-model="sslForm.sslCertificate"
                :rows="8"
                placeholder="请粘贴SSL证书内容（PEM格式）"
              />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">私钥</label>
              <Textarea
                v-model="sslForm.sslPrivateKey"
                :rows="8"
                placeholder="请粘贴SSL私钥内容（PEM格式）"
              />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="showSSLDialog = false">取消</Button>
          <Button @click="handleSaveSSL" :disabled="savingSSL">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="showTemplateDialog">
      <DialogContent class="sm:max-w-[700px]">
        <DialogHeader>
          <DialogTitle>{{ editingTemplate ? '编辑模板' : '新建模板' }}</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板类型</label>
              <RadioGroup v-model="templateForm.type" :disabled="!!editingTemplate">
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="email" />
                  <label class="text-sm cursor-pointer">邮件</label>
                </div>
                <div class="flex items-center gap-2">
                  <RadioGroupItem value="sms" />
                  <label class="text-sm cursor-pointer">短信</label>
                </div>
              </RadioGroup>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板代码</label>
              <Input v-model="templateForm.code" placeholder="例如：verification_code" :disabled="!!editingTemplate" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板名称</label>
              <Input v-model="templateForm.name" placeholder="请输入模板名称" />
            </div>
            <div v-if="templateForm.type === 'email'" class="grid gap-2">
              <label class="text-sm font-medium">邮件主题</label>
              <Input v-model="templateForm.subject" placeholder="请输入邮件主题，支持变量如 {{appName}}" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板内容</label>
              <Textarea
                v-model="templateForm.content"
                :rows="10"
                placeholder="请输入模板内容，支持变量如 {{code}}, {{username}} 等"
              />
            </div>
            <div class="flex items-center gap-2">
              <Switch :checked="templateForm.isDefault" @update:checked="templateForm.isDefault = $event" />
              <label class="text-sm cursor-pointer">设为默认</label>
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="showTemplateDialog = false">取消</Button>
          <Button @click="handleSaveTemplate" :disabled="savingTemplate">保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="showVerifyDialog">
      <DialogContent class="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>域名验证</DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <Alert class="mb-5">
            <AlertTitle>请按照以下步骤完成验证</AlertTitle>
          </Alert>
          <div v-if="verifyingDomain?.verificationMethod === 'dns'" class="space-y-3">
            <h4 class="font-semibold">DNS记录验证</h4>
            <p class="text-sm text-muted-foreground">在您的DNS服务商处添加以下TXT记录：</p>
            <div class="bg-muted p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>记录类型：</strong>TXT</p>
              <p class="text-sm"><strong>主机记录：</strong>@ 或 留空</p>
              <p class="text-sm"><strong>记录值：</strong></p>
              <pre class="bg-background p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken }}</pre>
            </div>
          </div>
          <div v-else class="space-y-3">
            <h4 class="font-semibold">文件验证</h4>
            <p class="text-sm text-muted-foreground">在您的网站根目录创建以下文件：</p>
            <div class="bg-muted p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>文件路径：</strong>/.well-known/easy1auth-verification.txt</p>
              <p class="text-sm"><strong>文件内容：</strong></p>
              <pre class="bg-background p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken?.split('=')[1] }}</pre>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="showVerifyDialog = false">取消</Button>
          <Button @click="handleVerifyDomainConfirm" :disabled="verifying">验证</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { message } from 'antdv-next'
import { Plus, Check, Info } from '@lucide/vue'
import { Button } from '@/components/antd-compat'
import { Input } from '@/components/antd-compat'
import { Textarea } from '@/components/antd-compat'
import { Card, CardContent } from '@/components/antd-compat'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/antd-compat'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/antd-compat'
import { Badge } from '@/components/antd-compat'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/antd-compat'
import { Alert, AlertDescription, AlertTitle } from '@/components/antd-compat'
import { RadioGroup, RadioGroupItem } from '@/components/antd-compat'
import { Checkbox } from '@/components/antd-compat'
import { Switch } from '@/components/antd-compat'
import { ColorPicker } from '@/components/antd-compat'
import { Upload, type UploadFile } from '@/components/antd-compat'
import { customDomainApi, type CustomDomain, type CreateDomainDto, type UpdateSSLDto } from '@/api/customDomain'
import { messageTemplateApi, type MessageTemplate, type CreateTemplateDto } from '@/api/messageTemplate'
import { loginStyleApi, type UpdateLoginStyleDto } from '@/api/loginStyle'

const activeTab = ref('domains')
const templateType = ref('email')

const domains = ref<CustomDomain[]>([])
const domainsLoading = ref(false)
const showDomainDialog = ref(false)
const creatingDomain = ref(false)
const domainForm = reactive<CreateDomainDto>({
  domain: '',
  verificationMethod: 'dns',
})

const showSSLDialog = ref(false)
const savingSSL = ref(false)
const sslForm = reactive<UpdateSSLDto>({
  sslCertificate: '',
  sslPrivateKey: '',
})
const currentDomainId = ref('')

const showVerifyDialog = ref(false)
const verifying = ref(false)
const verifyingDomain = ref<CustomDomain | null>(null)

const loginStyle = reactive<UpdateLoginStyleDto>({
  title: 'Easy1Auth',
  subtitle: '企业级身份管理平台',
  backgroundColor: '#f5f7fa',
  primaryColor: '#0369A1',
  loginMethods: ['password', 'email', 'passkey'],
})

const savingLoginStyle = ref(false)

const templates = ref<MessageTemplate[]>([])
const templatesLoading = ref(false)
const showTemplateDialog = ref(false)
const savingTemplate = ref(false)
const editingTemplate = ref<MessageTemplate | null>(null)
const templateForm = reactive<CreateTemplateDto>({
  type: 'email',
  code: '',
  name: '',
  subject: '',
  content: '',
  isDefault: false,
})
const initingTemplates = ref(false)

const emailTemplates = computed(() => templates.value.filter(t => t.type === 'email'))
const smsTemplates = computed(() => templates.value.filter(t => t.type === 'sms'))

const colorPresets = [
  { name: '默认蓝', value: '#0369A1' },
  { name: '翠绿', value: '#10B981' },
  { name: '紫罗兰', value: '#7C3AED' },
  { name: '橙红', value: '#F97316' },
  { name: '玫红', value: '#EC4899' },
  { name: '青色', value: '#06B6D4' },
]

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const toggleLoginMethod = (method: string) => {
  if (!loginStyle.loginMethods) {
    loginStyle.loginMethods = []
  }
  const index = loginStyle.loginMethods.indexOf(method)
  if (index > -1) {
    loginStyle.loginMethods.splice(index, 1)
  } else {
    loginStyle.loginMethods.push(method)
  }
}

const loadDomains = async () => {
  domainsLoading.value = true
  try {
    const response = await customDomainApi.list()
    domains.value = response
  } catch (error) {
    console.error('加载域名列表失败:', error)
    message.error('加载域名列表失败')
  } finally {
    domainsLoading.value = false
  }
}

const handleCreateDomain = async () => {
  if (!domainForm.domain) {
    message.warning('请输入域名')
    return
  }

  creatingDomain.value = true
  try {
    await customDomainApi.create(domainForm)
    message.success('域名添加成功，请完成验证')
    showDomainDialog.value = false
    domainForm.domain = ''
    domainForm.verificationMethod = 'dns'
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '添加域名失败')
  } finally {
    creatingDomain.value = false
  }
}

const handleVerifyDomainConfirm = async () => {
  if (!verifyingDomain.value) return

  verifying.value = true
  try {
    await customDomainApi.verify(verifyingDomain.value.id)
    message.success('域名验证成功')
    showVerifyDialog.value = false
    verifyingDomain.value = null
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '域名验证失败')
  } finally {
    verifying.value = false
  }
}

const handleSaveSSL = async () => {
  if (!sslForm.sslCertificate || !sslForm.sslPrivateKey) {
    message.warning('请填写SSL证书和私钥')
    return
  }

  savingSSL.value = true
  try {
    await customDomainApi.updateSSL(currentDomainId.value, sslForm)
    message.success('SSL证书配置成功')
    showSSLDialog.value = false
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '配置SSL证书失败')
  } finally {
    savingSSL.value = false
  }
}

const handleDeleteDomain = async (domain: CustomDomain) => {
  if (!window.confirm('确定要删除该域名吗？')) return
  
  try {
    await customDomainApi.delete(domain.id)
    message.success('域名删除成功')
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除域名失败')
  }
}

const loadLoginStyle = async () => {
  try {
    const response = await loginStyleApi.get()
    Object.assign(loginStyle, response)
  } catch (error) {
    console.error('加载登录样式失败:', error)
  }
}

const handleSaveLoginStyle = async () => {
  savingLoginStyle.value = true
  try {
    await loginStyleApi.update(loginStyle)
    message.success('保存成功')
  } catch (error: any) {
    message.error(error.response?.data?.msg || '保存失败')
  } finally {
    savingLoginStyle.value = false
  }
}

const beforeLogoUpload = (file: UploadFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const beforeBgUpload = (file: UploadFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleLogoUpload = (options: { file: { raw: File } }, field: 'logo' | 'logoDark' | 'backgroundImage') => {
  return new Promise<void>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      (loginStyle as any)[field] = e.target?.result as string
      resolve()
    }
    reader.readAsDataURL(options.file.raw)
  })
}

const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const response = await messageTemplateApi.list()
    templates.value = response
  } catch (error) {
    console.error('加载模板列表失败:', error)
    message.error('加载模板列表失败')
  } finally {
    templatesLoading.value = false
  }
}

const handleInitTemplates = async () => {
  initingTemplates.value = true
  try {
    await messageTemplateApi.initDefaults()
    message.success('默认模板初始化成功')
    loadTemplates()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '初始化模板失败')
  } finally {
    initingTemplates.value = false
  }
}

const handleEditTemplate = (template: MessageTemplate) => {
  editingTemplate.value = template
  templateForm.type = template.type
  templateForm.code = template.code
  templateForm.name = template.name
  templateForm.subject = template.subject || ''
  templateForm.content = template.content
  templateForm.isDefault = template.isDefault
  showTemplateDialog.value = true
}

const handleSaveTemplate = async () => {
  if (!templateForm.code || !templateForm.name || !templateForm.content) {
    message.warning('请填写完整信息')
    return
  }

  savingTemplate.value = true
  try {
    if (editingTemplate.value) {
      await messageTemplateApi.update(editingTemplate.value.id, {
        name: templateForm.name,
        subject: templateForm.subject,
        content: templateForm.content,
        isDefault: templateForm.isDefault,
      })
      message.success('模板更新成功')
    } else {
      await messageTemplateApi.create(templateForm)
      message.success('模板创建成功')
    }
    showTemplateDialog.value = false
    editingTemplate.value = null
    templateForm.type = 'email'
    templateForm.code = ''
    templateForm.name = ''
    templateForm.subject = ''
    templateForm.content = ''
    templateForm.isDefault = false
    loadTemplates()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '保存模板失败')
  } finally {
    savingTemplate.value = false
  }
}

const handleDeleteTemplate = async (template: MessageTemplate) => {
  if (!window.confirm('确定要删除该模板吗？')) return
  
  try {
    await messageTemplateApi.delete(template.id)
    message.success('模板删除成功')
    loadTemplates()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除模板失败')
  }
}

onMounted(() => {
  loadDomains()
  loadLoginStyle()
  loadTemplates()
})
</script>
