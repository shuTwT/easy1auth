<script setup lang="ts">
import { ref } from 'vue'

const tenants = ref([])
const loading = ref(false)

const handleSearch = () => {
  // TODO: 实现搜索功能
}

const handleAdd = () => {
  // TODO: 实现添加租户功能
}
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

      <el-form :inline="true" class="search-form">
        <el-form-item label="租户名称">
          <el-input placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select placeholder="请选择状态">
            <el-option label="全部" value="" />
            <el-option label="正常" value="active" />
            <el-option label="停用" value="suspended" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tenants" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="租户名称" />
        <el-table-column prop="domain" label="域名" />
        <el-table-column prop="plan" label="套餐" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
              {{ row.status === 'active' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button link type="primary">编辑</el-button>
            <el-button link type="primary">配置</el-button>
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
