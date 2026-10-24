import axios from "axios";
import { ElMessage } from "element-plus";

const http = axios.create({ baseURL: "/api", timeout: 10000 });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("ruankao_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("ruankao_token");
      localStorage.removeItem("ruankao_user");
      if (window.location.pathname !== "/login")
        window.location.href = "/login";
    }
    ElMessage.error(
      error.response?.data?.message || "请求失败，请确认后端和 MySQL 已启动",
    );
    return Promise.reject(error);
  },
);

export const authApi = {
  login: (data) => http.post("/auth/login", data),
  register: (data) => http.post("/auth/register", data),
  logout: () => http.post("/auth/logout"),
  me: () => http.get("/auth/me"),
  changePassword: (data) => http.put("/auth/password", data),
};

export const softwareApi = {
  info: () => http.get("/software/info"),
};

export const adminUserApi = {
  list: (params) => http.get("/admin/users", { params }),
  updateStatus: (id, enabled) =>
    http.patch(`/admin/users/${id}/status`, { enabled }),
  updateRole: (id, role) => http.patch(`/admin/users/${id}/role`, { role }),
};

export const dashboardApi = { get: () => http.get("/dashboard") };
export const planApi = {
  list: () => http.get("/plans"),
  create: (data) => http.post("/plans", data),
  update: (id, data) => http.put(`/plans/${id}`, data),
  toggle: (id) => http.patch(`/plans/${id}/toggle`),
  remove: (id) => http.delete(`/plans/${id}`),
};
export const chapterApi = {
  list: (params) => http.get("/chapters", { params }),
  options: () => http.get("/chapters/options"),
  update: (id, data) => http.put(`/chapters/${id}`, data),
};
export const wrongQuestionApi = {
  list: () => http.get("/wrong-questions"),
  create: (data, chapterId) =>
    http.post("/wrong-questions", data, { params: { chapterId } }),
  review: (id, mastered) =>
    http.patch(`/wrong-questions/${id}/review`, { mastered }),
  remove: (id) => http.delete(`/wrong-questions/${id}`),
};
export const calculatorApi = {
  earnedValue: (data) => http.post("/calculators/earned-value", data),
};
export const practiceApi = {
  chapters: () => http.get("/practice/chapters"),
  create: (data) => http.post("/practice/attempts", data),
  submit: (id, answers) =>
    http.post(`/practice/attempts/${id}/submit`, { answers }),
  detail: (id) => http.get(`/practice/attempts/${id}`),
  history: () => http.get("/practice/history"),
  stats: () => http.get("/practice/stats"),
};
export const questionBankApi = {
  list: (params) => http.get("/admin/questions", { params }),
  create: (data) => http.post("/admin/questions", data),
  update: (id, data) => http.put(`/admin/questions/${id}`, data),
  setEnabled: (id, enabled) =>
    http.patch(`/admin/questions/${id}/status`, { enabled }),
  remove: (id) => http.delete(`/admin/questions/${id}`),
  template: () =>
    http.get("/admin/questions/template", { responseType: "blob" }),
  importExcel: (file) => {
    const data = new FormData();
    data.append("file", file);
    return http.post("/admin/questions/import", data);
  },
};
export const tutorialApi = {
  list: (chapter) =>
    http.get("/tutorials", { params: { chapter: chapter || undefined } }),
  progress: () => http.get("/tutorials/progress"),
  detail: (id) => http.get(`/tutorials/${id}`),
  setCompleted: (id, sectionKey, completed) =>
    http.put(`/tutorials/${id}/sections/${sectionKey}/progress`, { completed }),
};
export const adminTutorialApi = {
  sources: () => http.get("/admin/tutorials/sources"),
  create: (data) => http.post("/admin/tutorials/sources", data),
  update: (id, data) => http.put(`/admin/tutorials/sources/${id}`, data),
  sync: (id) => http.post(`/admin/tutorials/sources/${id}/sync`),
  versions: (id) => http.get(`/admin/tutorials/sources/${id}/versions`),
  preview: (id, versionId) =>
    http.get(`/admin/tutorials/sources/${id}/versions/${versionId}`),
  publish: (id, versionId) =>
    http.post(`/admin/tutorials/sources/${id}/versions/${versionId}/publish`),
  unpublish: (id) => http.post(`/admin/tutorials/sources/${id}/unpublish`),
};
