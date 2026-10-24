<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminUserApi } from "../api";

const users = ref([]),
  total = ref(0),
  loading = ref(false),
  keyword = ref(""),
  enabled = ref(null),
  role = ref(null);
const page = reactive({ current: 1, size: 20 });

async function load() {
  loading.value = true;
  try {
    const params = {};
    if (keyword.value.trim()) params.keyword = keyword.value.trim();
    if (enabled.value !== null) params.enabled = enabled.value;
    if (role.value) params.role = role.value;
    params.page = page.current;
    params.size = page.size;
    const result = (await adminUserApi.list(params)).data;
    users.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.current = 1;
  load();
}
function changeSize() {
  page.current = 1;
  load();
}

function resetFilters() {
  keyword.value = "";
  enabled.value = null;
  role.value = null;
  page.current = 1;
  load();
}

async function updateStatus(row) {
  const nextEnabled = !row.enabled;
  const action = nextEnabled ? "启用" : "停用";
  await ElMessageBox.confirm(
    `确定${action}账号“${row.username}”吗？${nextEnabled ? "" : "停用后该用户现有登录将立即失效。"}`,
    `${action}账号`,
    { type: nextEnabled ? "info" : "warning" },
  );
  await adminUserApi.updateStatus(row.id, nextEnabled);
  ElMessage.success(`账号已${action}`);
  load();
}

async function updateRole(row) {
  const nextRole = row.role === "ADMIN" ? "USER" : "ADMIN";
  const label = nextRole === "ADMIN" ? "管理员" : "普通用户";
  await ElMessageBox.confirm(
    `确定将“${row.username}”设置为${label}吗？该账号需要重新登录。`,
    "修改用户角色",
    { type: "warning" },
  );
  await adminUserApi.updateRole(row.id, nextRole);
  ElMessage.success(`已设置为${label}`);
  load();
}

onMounted(load);
</script>

<template>
  <div>
    <div class="page-actions">
      <div>
        <strong>用户与访问权限</strong>
        <p>管理员可分配角色和管理账号状态，变更后该账号需要重新登录。</p>
      </div>
    </div>

    <div class="panel user-filter">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索用户名或昵称"
        style="width: 260px"
        @keyup.enter="search"
      />
      <el-select
        v-model="enabled"
        placeholder="全部状态"
        clearable
        style="width: 150px"
        @change="search"
      >
        <el-option label="已启用" :value="true" />
        <el-option label="已停用" :value="false" />
      </el-select>
      <el-select
        v-model="role"
        placeholder="全部角色"
        clearable
        style="width: 150px"
        @change="search"
      >
        <el-option label="管理员" value="ADMIN" />
        <el-option label="普通用户" value="USER" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="users" class="panel user-table" stripe>
      <el-table-column prop="username" label="用户名" min-width="160" />
      <el-table-column prop="nickname" label="昵称" min-width="150" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag
            :type="row.role === 'ADMIN' ? 'warning' : 'info'"
            effect="plain"
          >
            {{ row.role === "ADMIN" ? "管理员" : "普通用户" }}
          </el-tag>
          <el-tag
            v-if="row.defaultAdmin"
            type="danger"
            effect="plain"
            style="margin-left: 6px"
            >默认</el-tag
          >
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <span
            :class="['status-dot', row.enabled ? 'enabled' : 'disabled']"
          ></span>
          {{ row.enabled ? "已启用" : "已停用" }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="warning"
            :disabled="row.defaultAdmin"
            @click="updateRole(row)"
            >{{
              row.role === "ADMIN" ? "设为普通用户" : "设为管理员"
            }}</el-button
          >
          <el-button
            link
            :type="row.enabled ? 'danger' : 'primary'"
            :disabled="row.defaultAdmin"
            @click="updateStatus(row)"
            >{{ row.enabled ? "停用" : "启用" }}</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="changeSize"
    />
  </div>
</template>
