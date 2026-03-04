<script setup lang="ts">
import { ref } from 'vue'

const applications = ref([])
const loading = ref(false)

const handleSearch = () => {
  // TODO: 实现搜索功能
}

const handleAdd = () => {
  // TODO: 实现添加应用功能
}
</script>

<template>
  <div class="application-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>应用管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增应用
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="应用名称">
          <el-input placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用类型">
          <el-select placeholder="请选择类型">
            <el-option label="全部" value="" />
            <el-option label="Web应用" value="web" />
            <el-option label="SPA应用" value="spa" />
            <el-option label="移动应用" value="native" />
            <el-option label="后端服务" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select placeholder="请选择状态">
            <el-option label="全部" value="" />
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="applications" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="应用名称" />
        <el-table-column prop="type" label="应用类型">
          <template #default="{ row }">
            <el-tag>
              {{ row.type === 'web' ? 'Web应用' : row.type === 'spa' ? 'SPA' : row.type === 'native' ? '移动应用' : '后端服务' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientId" label="Client ID" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button link type="primary">查看</el-button>
            <el-button link type="primary">编辑</el-button>
            <el-button link type="primary">SSO配置</el-button>
            <el-button link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="0"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<style scoped>
.application-management {
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
