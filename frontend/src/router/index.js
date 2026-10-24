import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { title: '登录', public: true } },
    { path: '/', component: DashboardView, meta: { title: '学习总览' } },
    { path: '/plans', component: () => import('../views/PlansView.vue'), meta: { title: '学习计划' } },
    { path: '/chapters', component: () => import('../views/ChaptersView.vue'), meta: { title: '章节掌握' } },
    { path: '/wrong-questions', component: () => import('../views/WrongQuestionsView.vue'), meta: { title: '错题复盘' } },
    { path: '/calculator', component: () => import('../views/CalculatorView.vue'), meta: { title: '挣值计算器' } },
    { path: '/users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理', requiresAdmin: true } }
  ]
})

router.beforeEach(to => {
  const token = localStorage.getItem('ruankao_token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/'
  if (to.meta.requiresAdmin) {
    try {
      const user = JSON.parse(localStorage.getItem('ruankao_user') || '{}')
      if (user.role !== 'ADMIN') return '/'
    } catch (_) { return '/' }
  }
})

export default router
