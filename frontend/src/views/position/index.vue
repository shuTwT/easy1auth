<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

const rules = {
  name: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入岗位编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '编码只能包含大写字母和下划线', trigger: 'blur' }
  ],
  level: [
    { required: true, message: '请选择岗位级别', trigger: 'change' }
  ]
}

const loadPositions = async () => {
  loading.value = true
  try {
    const res = await positionApi.getList(queryForm)
    positions.value = res.data.positions
    total.value = res.data.total
  } catch (error) {
    console.error('加载岗位列表失败:', error)
    ElMessage.error('加载岗位列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await positionApi.getStats()
    stats.value = res.data
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
  try {
    await ElMessageBox.confirm('确定要删除该岗位吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await positionApi.delete(row.id)
    ElMessage.success('删除成功')
    loadPositions()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除岗位失败:', error)
      ElMessage.error('删除岗位失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    if (currentPosition.value.id) {
      await positionApi.update(currentPosition.value.id, positionForm)
      ElMessage.success('更新成功')
    } else {
      await positionApi.create(positionForm as CreatePositionDto)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadPositions()
    loadStats()
  } catch (error: any) {
    console.error('保存岗位失败:', error)
    ElMessage.error(error.response?.data?.message || '保存岗位失败')
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadPositions()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadPositions()
}

const getLevelColor = (level?: number) => {
  if (!level) return 'info'
  if (level >= 9) return 'danger'
  if (level >= 7) return 'warning'
  if (level >= 5) return 'primary'
  if (level >= 3) return 'success'
  return 'info'
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
  <div class="position-management">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.totalPositions || 0 }}</div>
            <div class="stat-label">岗位总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.filledPositions || 0 }}</div>
            <div class="stat-label">已分配岗位</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.vacantPositions || 0 }}</div>
            <div class="stat-label">空缺岗位</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.averageLevel || 0 }}</div>
            <div class="stat-label">平均级别</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>岗位管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增岗位
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="岗位名称">
          <el-input v-model="queryForm.name" placeholder="请输入岗位名称" clearable />
        </el-form-item>
        <el-form-item label="岗位编码">
          <el-input v-model="queryForm.code" placeholder="请输入岗位编码" clearable />
        </el-form-item>
        <el-form-item label="岗位级别">
          <el-select v-model="queryForm.level" placeholder="请选择级别" clearable style="width: 150px">
            <el-option label="员工 (1-2级)" :value="1" />
            <el-option label="主管 (3-4级)" :value="3" />
            <el-option label="经理 (5-6级)" :value="5" />
            <el-option label="总监 (7-8级)" :value="7" />
            <el-option label="高管 (9-10级)" :value="9" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="positions" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="岗位名称" width="180" />
        <el-table-column prop="code" label="岗位编码" width="150">
          <template #default="{ row }">
            <span style="font-family: monospace;">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="level" label="岗位级别" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelColor(row.level)">
              {{ row.level }} - {{ getLevelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="在职人数" width="100" align="center">
          <template #default="{ row }">
            {{ row.userCount }}{{ row.maxCount ? ` / ${row.maxCount}` : '' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="positionForm" :rules="rules" label-width="100px">
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="positionForm.name" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码" prop="code">
          <el-input 
            v-model="positionForm.code" 
            placeholder="请输入岗位编码（大写字母和下划线）"
            :disabled="!!currentPosition.id"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="positionForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入岗位描述"
          />
        </el-form-item>
        <el-form-item label="岗位级别" prop="level">
          <el-slider v-model="positionForm.level" :min="1" :max="10" :step="1" show-stops />
          <div style="margin-top: 10px; text-align: center;">
            <el-tag :type="getLevelColor(positionForm.level)">
              {{ positionForm.level }} - {{ getLevelText(positionForm.level) }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="positionForm.maxCount" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="positionForm.sequence" placeholder="请输入排序标识" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.position-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
