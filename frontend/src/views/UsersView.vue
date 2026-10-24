<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserApi } from '../api'

const users = ref([]), loading = ref(false), keyword = ref(''), enabled = ref(null)

async function load() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (enabled.value !== null) params.enabled = enabled.value
    users.value = (await adminUserApi.list(params)).data
  } finally { loading.value = false }
}

function resetFilters() {
  keyword.value = ''
  enabled.value = null
  load()
}

async function updateStatus(row) {
  const nextEnabled = !row.enabled
  const action = nextEnabled ? '启用' : '停用'
  await ElMessageBox.confirm(
    `确定${action}账号“${row.username}”吗？${nextEnabled ? '' : '停用后该用户现有登录将立即失效。'}`,
    `${action}账号`,
    { type: nextEnabled ? 'info' : 'warning' }
  )
  await adminUserApi.updateStatus(row.id, nextEnabled)
  ElMessage.success(`账号已${action}`)
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-actions">
      <div><strong>用户与访问权限</strong><p>管理员可以查询并启用或停用普通用户，停用立即对现有 JWT 生效。</p></div>
    </div>

    <div class="panel user-filter">
      <el-input v-model="keyword" clearable placeholder="搜索用户名或昵称" style="width:260px" @keyup.enter="load" />
      <el-select v-model="enabled" placeholder="全部状态" clearable style="width:150px" @change="load">
        <el-option label="已启用" :value="true" />
        <el-option label="已停用" :value="false" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="users" class="panel user-table" stripe>
      <el-table-column prop="username" label="用户名" min-width="160" />
      <el-table-column prop="nickname" label="昵称" min-width="150" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="plain">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <span :class="['status-dot', row.enabled ? 'enabled' : 'disabled']"></span>
          {{ row.enabled ? '已启用' : '已停用' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            :type="row.enabled ? 'danger' : 'primary'"
            :disabled="row.role === 'ADMIN'"
            @click="updateStatus(row)"
          >{{ row.enabled ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
