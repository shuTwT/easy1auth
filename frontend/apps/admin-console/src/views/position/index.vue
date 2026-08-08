<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Form, FormItem, Modal, Pagination as AntPagination, Table, message } from 'antdv-next'
import { Plus, Search, RefreshCw } from '@lucide/vue'
import { positionApi } from '@/api/position'
import type { Position, CreatePositionDto, UpdatePositionDto, PositionQueryDto, PositionStats } from '@/types/position'
const loading = ref(false)
const positions = ref<Position[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const currentPosition = ref<Partial<Position>>({})
const stats = ref<PositionStats | null>(null)

const [modal, contextHolder] = Modal.useModal()

const queryForm = reactive<PositionQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  code: '',
  departmentId: undefined,
  level: undefined
})

const positionForm = reactive<CreatePositionDto & UpdatePositionDto>({
  name: '',
  code: '',
  description: '',
  departmentId: undefined,
  level: 1,
  sequence: '',
  maxCount: undefined
})

const positionFormRules = {
  name: [{ required: true, message: '请输入岗位名称' }],
  code: [{ required: true, message: '请输入岗位编码' }],
  level: [{ required: true, message: '请选择岗位级别' }],
}

const submitting = ref(false)

const loadPositions = async () => {
  loading.value = true
  try {
    const res = await positionApi.getList(queryForm)
    positions.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载岗位列表失败:', error)
    message.error('加载岗位列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await positionApi.getStats()
    stats.value = res
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载岗位统计数据失败')
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadPositions()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.code = ''
  queryForm.departmentId = undefined
  queryForm.level = undefined
  queryForm.page = 1
  loadPositions()
}

const handleAdd = () => {
  dialogTitle.value = '新增岗位'
  Object.assign(positionForm, {
    name: '',
    code: '',
    description: '',
    departmentId: undefined,
    level: 1,
    sequence: '',
    maxCount: undefined
  })
  currentPosition.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: Position) => {
  dialogTitle.value = '编辑岗位'
  Object.assign(positionForm, {
    name: row.name,
    code: row.code,
    description: row.description || '',
    departmentId: row.departmentId || undefined,
    level: row.level,
    sequence: row.sequence || '',
    maxCount: row.maxCount || undefined
  })
  currentPosition.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: Position) => {
  const confirmed = await modal.confirm({
    title: '删除岗位',
    content: '确定要删除该岗位吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await positionApi.delete(row.id)
    message.success('删除成功')
    loadPositions()
    loadStats()
  } catch (error: any) {
    console.error('删除岗位失败:', error)
    message.error(error.response?.data?.msg || '删除岗位失败')
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = {
      ...positionForm,
      name: positionForm.name.trim(),
      code: positionForm.code.trim(),
      level: positionForm.level ?? 1,
    }
    if (currentPosition.value.id) {
      await positionApi.update(currentPosition.value.id, payload)
      message.success('更新成功')
    } else {
      await positionApi.create(payload)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadPositions()
    loadStats()
  } catch (error: any) {
    console.error('保存岗位失败:', error)
    message.error(error.response?.data?.msg || '保存岗位失败')
  } finally {
    submitting.value = false
  }
}

const handlePageChange = (page: number, ps?: number) => {
  queryForm.page = page
  if (ps !== undefined) queryForm.pageSize = ps
  loadPositions()
}


const getLevelColor = (level?: number) => {
  if (!level) return 'default'
  if (level >= 9) return 'red'
  if (level >= 7) return 'purple'
  if (level >= 5) return 'blue'
  if (level >= 3) return 'green'
  return 'cyan'
}

const getLevelText = (level?: number) => {
  if (!level) return '未指定'
  if (level >= 9) return '高管'
  if (level >= 7) return '总监'
  if (level >= 5) return '经理'
  if (level >= 3) return '主管'
  return '员工'
}

onMounted(() => {
  loadPositions()
  loadStats()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">岗位管理</h1>
        <p class="text-sm text-muted-foreground">管理系统岗位，包括添加、编辑和删除</p>
      </div>
      <div class="flex gap-3">
        <Button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增岗位
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <Card>
        <div class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-foreground">{{ stats?.totalPositions || 0 }}</div>
            <div class="text-sm text-muted-foreground">岗位总数</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-foreground">{{ stats?.filledPositions || 0 }}</div>
            <div class="text-sm text-muted-foreground">已分配岗位</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-foreground">{{ stats?.vacantPositions || 0 }}</div>
            <div class="text-sm text-muted-foreground">空缺岗位</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-foreground">{{ stats?.averageLevel || 0 }}</div>
            <div class="text-sm text-muted-foreground">平均级别</div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div class="pt-6">
        <div class="flex flex-wrap gap-4 mb-5">
          <div class="grid gap-2">
            <Input v-model:value="queryForm.name" placeholder="请输入岗位名称" class="w-48" />
          </div>
          <div class="grid gap-2">
            <Input v-model:value="queryForm.code" placeholder="请输入岗位编码" class="w-48" />
          </div>
          <div class="grid gap-2">
            <Select v-model:value="queryForm.level" class="w-40" allow-clear placeholder="请选择级别">
                <SelectOption :value="1">员工 (1-2级)</SelectOption>
                <SelectOption :value="3">主管 (3-4级)</SelectOption>
                <SelectOption :value="5">经理 (5-6级)</SelectOption>
                <SelectOption :value="7">总监 (7-8级)</SelectOption>
                <SelectOption :value="9">高管 (9-10级)</SelectOption>

            </Select>
          </div>
          <Button @click="handleSearch">
            <Search class="size-4 mr-2" />
            搜索
          </Button>
          <Button  @click="handleReset">
            <RefreshCw class="size-4 mr-2" />
            重置
          </Button>
        </div>

        <Table :columns="[
          { title: '岗位名称', dataIndex: 'name', width: 176 }, { title: '岗位编码', key: 'code', width: 144 },
          { title: '描述', key: 'description' }, { title: '岗位级别', key: 'level', width: 112, align: 'center' },
          { title: '在职人数', key: 'userCount', width: 96, align: 'center' }, { title: '创建时间', key: 'createdAt', width: 176 },
          { title: '操作', key: 'actions', width: 144, align: 'right' }
        ]" :data-source="positions" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1050 }">
          <template #bodyCell="{ column, record: position }">
            <template v-if="column.key === 'code'">
                <span class="font-mono">{{ position.code }}</span>
            </template>
            <template v-else-if="column.key === 'description'">{{ position.description || '-' }}</template>
            <template v-else-if="column.key === 'level'">
                <Tag :color="getLevelColor(position.level)">
                  {{ position.level }} - {{ getLevelText(position.level) }}
                </Tag>
            </template>
            <template v-else-if="column.key === 'userCount'">
                {{ position.userCount }}{{ position.maxCount ? ` / ${position.maxCount}` : '' }}
            </template>
            <template v-else-if="column.key === 'createdAt'">{{ new Date(position.createdAt).toLocaleString() }}</template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex justify-end gap-1">
                  <Button type="link" size="small" class="h-auto p-0" @click="handleEdit(position)">编辑</Button>
                  <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDelete(position)">删除</Button>
                </div>
            </template>
          </template>
        </Table>

        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <AntPagination
            :current="queryForm.page"
            :page-size="queryForm.pageSize"
            :total="total"
            :show-size-changer="false"
            size="small"
            @change="handlePageChange"
          />
        </div>
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <Form :model="positionForm" :rules="positionFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <FormItem label="岗位名称" name="name">
              <Input v-model:value="positionForm.name" placeholder="请输入岗位名称" />
            </FormItem>
            <FormItem label="岗位编码" name="code">
              <Input v-model:value="positionForm.code"
                placeholder="请输入岗位编码（大写字母和下划线）"
                :disabled="!!currentPosition.id"
              />
            </FormItem>
            <FormItem label="描述" name="description">
              <InputTextArea v-model:value="positionForm.description"
                placeholder="请输入岗位描述"
                :rows="3"
              />
            </FormItem>
            <FormItem label="岗位级别" name="level">
              <Slider v-model:value="positionForm.level"
                :min="1"
                :max="10"
                :step="1"
              />
              <div class="text-center mt-2">
                <Tag :color="getLevelColor(positionForm.level)">
                  {{ positionForm.level }} - {{ getLevelText(positionForm.level) }}
                </Tag>
              </div>
            </FormItem>
            <FormItem label="最大人数" name="maxCount">
              <InputNumber v-model:value="positionForm.maxCount" :min="1" :max="999" />
            </FormItem>
            <FormItem label="排序" name="sequence">
              <Input v-model:value="positionForm.sequence" placeholder="请输入排序标识" />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2">
            <Button @click="dialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit" :loading="submitting">确定</Button>
          </div>
        </Form>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
