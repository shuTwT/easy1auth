<script setup lang="ts">
import { ref } from 'vue'

const stats = ref([
  { title: '租户总数', value: 128, icon: 'OfficeBuilding', color: '#409EFF' },
  { title: '用户总数', value: 12580, icon: 'User', color: '#67C23A' },
  { title: '应用总数', value: 356, icon: 'Monitor', color: '#E6A23C' },
  { title: '今日登录', value: 8920, icon: 'Connection', color: '#F56C6C' }
])
</script>

<template>
  <div class="dashboard">
    <div class="stats-grid">
      <el-card v-for="stat in stats" :key="stat.title" class="stat-card">
        <div class="stat-content">
          <div class="stat-icon" :style="{ backgroundColor: stat.color }">
            <el-icon :size="32">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近登录</span>
          </template>
          <el-table :data="[]" style="width: 100%">
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="ip" label="IP地址" />
            <el-table-column prop="time" label="登录时间" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>系统公告</span>
          </template>
          <div class="announcement">
            <p>欢迎使用 Easy1Auth 企业级统一身份管理平台</p>
            <p>当前版本: v1.0.0</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.announcement {
  padding: 20px;
  color: #666;
  line-height: 1.8;
}
</style>
