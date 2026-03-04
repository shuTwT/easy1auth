<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tenantApi } from '@/api/tenant'
import type { Tenant, CreateTenantDto, UpdateTenantDto, TenantQueryDto } from '@/types/tenant'

const loading = ref(false)
const tenants = ref<Tenant[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const currentTenant = ref<Partial<Tenant>>({})

const queryForm = reactive<TenantQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  status: undefined,
  plan: undefined
})

const tenantForm = reactive<CreateTenantDto & UpdateTenantDto>({
  name: '',
  logo: '',
  domain: '',
  plan: 'basic',
  maxUsers: 100,
  maxApps: 10
})

const rules = {
  name: [
    { required: true, message: '请输入租户名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  domain: [
    { pattern: /^[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})+$/, message: '请输入有效的域名', trigger: 'blur' }
  ],
  maxUsers: [
    { required: true, message: '请输入最大用户数', trigger: 'blur' },
    { type: 'number', min: 1, message: '最小值为 1', trigger: 'blur' }
  ],
  maxApps: [
    { required: true, message: '请输入最大应用数', trigger: 'blur' },
    { type: 'number', min: 1, message: '最小值为 1', trigger: 'blur' }
  ]
}

const loadTenants = async () => {
  loading.value = true
  try {
    const res = await tenantApi.getList(queryForm)
    tenants.value = res.tenants
    total.value = res.total
  } catch (error) {
    console.error('加载租户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadTenants()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.status = undefined
  queryForm.plan = undefined
  queryForm.page = 1
  loadTenants()
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  Object.assign(tenantForm, {
    name: '',
    logo: '',
    domain: '',
    plan: 'basic',
    maxUsers: 100,
    maxApps: 10
  })
  currentTenant.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: Tenant) => {
  dialogTitle.value = '编辑租户'
  Object.assign(tenantForm, row)
  currentTenant.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: Tenant) => {
  try {
    await ElMessageBox.confirm('确定要删除该租户吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await tenantApi.delete(row.id)
    ElMessage.success('删除成功')
    loadTenants()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除租户失败:', error)
    }
  }
}

const handleStatusChange = async (row: Tenant, status: string) => {
  try {
    await tenantApi.updateStatus(row.id, status)
    ElMessage.success('状态更新成功')
    loadTenants()
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

const handleSubmit = async () => {
  try {
    if (currentTenant.value.id) {
      await tenantApi.update(currentTenant.value.id, tenantForm)
      ElMessage.success('更新成功')
    } else {
      await tenantApi.create(tenantForm as CreateTenantDto)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTenants()
  } catch (error) {
    console.error('保存租户失败:', error)
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadTenants()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadTenants()
}

onMounted(() => {
  loadTenants()
})
</script>

<template>
  <div class="tenant-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增租户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="租户名称">
          <el-input v-model="queryForm.name" placeholder="请输入租户名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="停用" value="suspended" />
            <el-option label="已删除" value="deleted" />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="queryForm.plan" placeholder="请选择套餐" clearable>
            <el-option label="基础版" value="basic" />
            <el-option label="专业版" value="professional" />
            <el-option label="企业版" value="enterprise" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tenants" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="租户名称" width="180" />
        <el-table-column prop="domain" label="域名" width="200" />
        <el-table-column prop="plan" label="套餐" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.plan === 'basic'" type="info">基础版</el-tag>
            <el-tag v-else-if="row.plan === 'professional'" type="warning">专业版</el-tag>
            <el-tag v-else type="success">企业版</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : row.status === 'suspended' ? 'warning' : 'danger'">
              {{ row.status === 'active' ? '正常' : row.status === 'suspended' ? '停用' : '已删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxUsers" label="用户上限" width="100" />
        <el-table-column prop="maxApps" label="应用上限" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              link 
              :type="row.status === 'active' ? 'warning' : 'success'" 
              @click="handleStatusChange(row, row.status === 'active' ? 'suspended' : 'active')"
            >
              {{ row.status === 'active' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="primary">统计</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="tenantForm" :rules="rules" label-width="100px">
        <el-form-item label="租户名称" prop="name">
          <el-input v-model="tenantForm.name" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="Logo" prop="logo">
          <el-input v-model="tenantForm.logo" placeholder="请输入Logo URL" />
        </el-form-item>
        <el-form-item label="域名" prop="domain">
          <el-input v-model="tenantForm.domain" placeholder="请输入域名" />
        </el-form-item>
        <el-form-item label="套餐" prop="plan">
          <el-select v-model="tenantForm.plan" placeholder="请选择套餐">
            <el-option label="基础版" value="basic" />
            <el-option label="专业版" value="professional" />
            <el-option label="企业版" value="enterprise" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户上限" prop="maxUsers">
          <el-input-number v-model="tenantForm.maxUsers" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="应用上限" prop="maxApps">
          <el-input-number v-model="tenantForm.maxApps" :min="1" :max="1000" />
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
.tenant-management {
  padding: 20px;
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
