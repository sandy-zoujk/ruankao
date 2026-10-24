<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Collection, DataAnalysis, EditPen, HomeFilled, UserFilled } from '@element-plus/icons-vue'
import { authApi } from './api'

const route = useRoute()
const router = useRouter()
const pageTitle = computed(() => route.meta.title || '软考进阶台')
let savedUser = {}
try { savedUser = JSON.parse(localStorage.getItem('ruankao_user') || '{}') } catch (_) { /* 使用空用户 */ }
const currentUser = reactive({ ...savedUser })
const username = computed(() => currentUser.username || '备考用户')
const isAdmin = computed(() => currentUser.role === 'ADMIN')

const passwordVisible = ref(false), passwordLoading = ref(false), passwordFormRef = ref()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '新密码长度为6-50位', trigger: 'blur' }
  ],
  confirmPassword: [{
    validator: (_, value, callback) => value === passwordForm.newPassword
      ? callback() : callback(new Error('两次输入的新密码不一致')),
    trigger: 'blur'
  }]
}

const baseMenus = [
  { path: '/', label: '学习总览', icon: HomeFilled },
  { path: '/plans', label: '学习计划', icon: Calendar },
  { path: '/chapters', label: '章节掌握', icon: Collection },
  { path: '/wrong-questions', label: '错题复盘', icon: EditPen },
  { path: '/calculator', label: '挣值计算', icon: DataAnalysis }
]
const menus = computed(() => isAdmin.value
  ? [...baseMenus, { path: '/users', label: '用户管理', icon: UserFilled }]
  : baseMenus)

function clearLogin() {
  localStorage.removeItem('ruankao_token')
  localStorage.removeItem('ruankao_user')
}

async function refreshProfile() {
  if (!localStorage.getItem('ruankao_token')) return
  const response = await authApi.me()
  Object.assign(currentUser, response.data)
  localStorage.setItem('ruankao_user', JSON.stringify({ ...currentUser }))
}

async function logout() {
  try { await authApi.logout() } catch (_) { /* 本地仍需退出 */ }
  clearLogin()
  router.replace('/login')
}

function openPasswordDialog() {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordVisible.value = true
}

async function changePassword() {
  await passwordFormRef.value.validate()
  passwordLoading.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    clearLogin()
    router.replace('/login')
  } finally { passwordLoading.value = false }
}

onMounted(() => refreshProfile().catch(() => {}))
</script>

<template>
  <router-view v-if="route.meta.public" />
  <el-container v-else class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">软</div>
        <div><strong>软考进阶台</strong><span>项目管理师备考</span></div>
      </div>
      <el-menu :default-active="route.path" router>
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="exam-card">
        <small>备考目标</small>
        <strong>2027 上半年</strong>
        <span>信息系统项目管理师</span>
      </div>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div><span class="eyebrow">STUDY WORKSPACE</span><h1>{{ pageTitle }}</h1></div>
        <div class="user-box">
          <div class="user-avatar">{{ username.slice(0, 1).toUpperCase() }}</div>
          <div class="user-identity"><strong>{{ username }}</strong><span>{{ isAdmin ? '管理员' : '普通用户' }}</span></div>
          <el-button link type="primary" @click="openPasswordDialog">修改密码</el-button>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>

    <el-dialog v-model="passwordVisible" title="修改密码" width="440px" append-to-body>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible=false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>
