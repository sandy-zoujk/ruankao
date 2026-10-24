import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use(config => {
  const token = localStorage.getItem('ruankao_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ruankao_token')
      localStorage.removeItem('ruankao_user')
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    ElMessage.error(error.response?.data?.message || '请求失败，请确认后端和 MySQL 已启动')
    return Promise.reject(error)
  }
)

export const authApi = {
  login: data => http.post('/auth/login', data),
  register: data => http.post('/auth/register', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  changePassword: data => http.put('/auth/password', data)
}

export const adminUserApi = {
  list: params => http.get('/admin/users', { params }),
  updateStatus: (id, enabled) => http.patch(`/admin/users/${id}/status`, { enabled })
}

export const dashboardApi = { get: () => http.get('/dashboard') }
export const planApi = {
  list: () => http.get('/plans'),
  create: data => http.post('/plans', data),
  update: (id, data) => http.put(`/plans/${id}`, data),
  toggle: id => http.patch(`/plans/${id}/toggle`),
  remove: id => http.delete(`/plans/${id}`)
}
export const chapterApi = {
  list: () => http.get('/chapters'),
  update: (id, data) => http.put(`/chapters/${id}`, data)
}
export const wrongQuestionApi = {
  list: () => http.get('/wrong-questions'),
  create: (data, chapterId) => http.post('/wrong-questions', data, { params: { chapterId } }),
  review: (id, mastered) => http.patch(`/wrong-questions/${id}/review`, { mastered }),
  remove: id => http.delete(`/wrong-questions/${id}`)
}
export const calculatorApi = { earnedValue: data => http.post('/calculators/earned-value', data) }
