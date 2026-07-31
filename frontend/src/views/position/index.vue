<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'antdv-next'
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

const levelSliderValue = ref([1])

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
  levelSliderValue.value = [1]
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
  levelSliderValue.value = [row.level]
  currentPosition.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: Position) => {
  const confirmed = window.confirm('确定要删除该岗位吗？删除后无法恢复！')
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
  try {
    if (currentPosition.value.id) {
      await positionApi.update(currentPosition.value.id, positionForm)
      message.success('更新成功')
    } else {
      await positionApi.create(positionForm as CreatePositionDto)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadPositions()
    loadStats()
  } catch (error: any) {
    console.error('保存岗位失败:', error)
    message.error(error.response?.data?.msg || '保存岗位失败')
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadPositions()
}


const getLevelColor = (level?: number) => {
  if (!level) return 'secondary'
  if (level >= 9) return 'destructive'
  if (level >= 7) return 'default'
  if (level >= 5) return 'default'
  if (level >= 3) return 'secondary'
  return 'outline'
}

const getLevelText = (level?: number) => {
  if (!level) return '未指定'
  if (level >= 9) return '高管'
  if (level >= 7) return '总监'
  if (level >= 5) return '经理'
  if (level >= 3) return '主管'
  return '员工'
}

const totalPages = Math.ceil(total.value / queryForm.pageSize!)

onMounted(() => {
  loadPositions()
  loadStats()
})
</script>

<template>
  <div class="position-management">
    <div class="grid grid-cols-4 gap-5 mb-5">
      <Card>
        <div class="pt-6">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.totalPositions || 0 }}</div>
            <div class="stat-label">岗位总数</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.filledPositions || 0 }}</div>
            <div class="stat-label">已分配岗位</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.vacantPositions || 0 }}</div>
            <div class="stat-label">空缺岗位</div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.averageLevel || 0 }}</div>
            <div class="stat-label">平均级别</div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div>
        <div class="flex justify-between items-center">
          <h3>岗位管理</h3>
          <Button @click="handleAdd">
            <Plus class="w-4 h-4 mr-2" />
            新增岗位
          </Button>
        </div>
      </div>
      <div>
        <div class="flex flex-wrap gap-4 mb-5">
          <div class="grid gap-2">
            <Input v-model:value="queryForm.name" placeholder="请输入岗位名称" class="w-48" />
          </div>
          <div class="grid gap-2">
            <Input v-model:value="queryForm.code" placeholder="请输入岗位编码" class="w-48" />
          </div>
          <div class="grid gap-2">
            <Select v-model:value="queryForm.level" placeholder="请选择级别">
              <div class="w-40">

              </div>

                <SelectOption :value="1">员工 (1-2级)</SelectOption>
                <SelectOption :value="3">主管 (3-4级)</SelectOption>
                <SelectOption :value="5">经理 (5-6级)</SelectOption>
                <SelectOption :value="7">总监 (7-8级)</SelectOption>
                <SelectOption :value="9">高管 (9-10级)</SelectOption>

            </Select>
          </div>
          <Button @click="handleSearch">
            <Search class="w-4 h-4 mr-2" />
            搜索
          </Button>
          <Button  @click="handleReset">
            <RefreshCw class="w-4 h-4 mr-2" />
            重置
          </Button>
        </div>

        <table>
          <thead>
            <tr>
              <th class="w-44">岗位名称</th>
              <th class="w-36">岗位编码</th>
              <th>描述</th>
              <th class="w-28 text-center">岗位级别</th>
              <th class="w-24 text-center">在职人数</th>
              <th class="w-44">创建时间</th>
              <th class="w-36 text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="text-center py-8 text-muted-foreground">
                加载中...
              </td>
            </tr>
            <tr v-else-if="positions.length === 0">
              <td colspan="7" class="text-center py-8 text-muted-foreground">
                暂无数据
              </td>
            </tr>
            <tr v-for="position in positions" :key="position.id">
              <td>{{ position.name }}</td>
              <td>
                <span style="font-family: monospace;">{{ position.code }}</span>
              </td>
              <td>{{ position.description || '-' }}</td>
              <td class="text-center">
                <Tag :color="getLevelColor(position.level)">
                  {{ position.level }} - {{ getLevelText(position.level) }}
                </Tag>
              </td>
              <td class="text-center">
                {{ position.userCount }}{{ position.maxCount ? ` / ${position.maxCount}` : '' }}
              </td>
              <td>{{ new Date(position.createdAt).toLocaleString() }}</td>
              <td class="text-right">
                <div class="flex justify-end gap-2">
                  <Button size="small"  @click="handleEdit(position)">编辑</Button>
                  <Button size="small" color="error" @click="handleDelete(position)">删除</Button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-1">
            <Button  size="small" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">
              上一页
            </Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages || 1 }}</span>
            <Button  size="small" :disabled="queryForm.page! >= totalPages" @click="handlePageChange(queryForm.page! + 1)">
              下一页
            </Button>
          </div>
        </div>
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="sm:max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <form class="grid gap-4">
          <div class="grid gap-2">
            <label class="text-sm font-medium">岗位名称 <span class="text-destructive">*</span></label>
            <Input v-model:value="positionForm.name" placeholder="请输入岗位名称" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">岗位编码 <span class="text-destructive">*</span></label>
            <Input v-model:value="positionForm.code"
              placeholder="请输入岗位编码（大写字母和下划线）"
              :disabled="!!currentPosition.id"
            />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">描述</label>
            <InputTextArea v-model:value="positionForm.description"
              placeholder="请输入岗位描述"
              :rows="3"
            />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">岗位级别 <span class="text-destructive">*</span></label>
            <Slider v-model:value="levelSliderValue"
              :min="1"
              :max="10"
              :step="1"
              @update:value="positionForm.level = levelSliderValue[0]"
            />
            <div class="text-center mt-2">
              <Tag :color="getLevelColor(positionForm.level)">
                {{ positionForm.level }} - {{ getLevelText(positionForm.level) }}
              </Tag>
            </div>
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">最大人数</label>
            <InputNumber v-model:value="positionForm.maxCount" :min="1" :max="999" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">排序</label>
            <Input v-model:value="positionForm.sequence" placeholder="请输入排序标识" />
          </div>
        </form>
        <div>
          <Button  @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.position-management {
  padding: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: hsl(var(--primary));
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}
</style>
