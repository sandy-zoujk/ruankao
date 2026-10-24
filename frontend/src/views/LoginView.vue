<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";
import SliderVerify from "../components/SliderVerify.vue";
import { authApi, softwareApi } from "../api";

const router = useRouter();
const mode = ref("login"),
  loading = ref(false),
  loginFormRef = ref(),
  registerFormRef = ref();
const verifyVisible = ref(false),
  verifyPassed = ref(false),
  pendingAction = ref("");
const softwareInfo = reactive({
  name: "知行软考学习与知识管理系统",
  version: "V1.0",
});
const loginForm = reactive({
  username: "",
  password: "",
  sliderVerified: false,
});
const registerForm = reactive({
  username: "",
  nickname: "",
  password: "",
  confirmPassword: "",
  sliderVerified: false,
});
const usernameRule = {
  pattern: /^[A-Za-z0-9_]{4,30}$/,
  message: "请输入4-30位字母、数字或下划线",
  trigger: "blur",
};
const loginRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};
const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    usernameRule,
  ],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 50, message: "密码长度为6-50位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    {
      validator: (_, value, callback) =>
        value === registerForm.password
          ? callback()
          : callback(new Error("两次密码不一致")),
      trigger: "blur",
    },
  ],
};

async function login() {
  await loginFormRef.value.validate();
  if (!loginForm.sliderVerified) return openVerify("login");
  await submitLogin();
}

async function submitLogin() {
  loading.value = true;
  try {
    const response = await authApi.login(loginForm);
    localStorage.setItem("ruankao_token", response.data.token);
    localStorage.setItem("ruankao_user", JSON.stringify(response.data));
    ElMessage.success(`欢迎回来，${response.data.nickname}`);
    router.replace("/");
  } finally {
    loading.value = false;
  }
}

async function register() {
  await registerFormRef.value.validate();
  if (!registerForm.sliderVerified) return openVerify("register");
  await submitRegister();
}

async function submitRegister() {
  loading.value = true;
  try {
    await authApi.register({
      username: registerForm.username,
      nickname: registerForm.nickname,
      password: registerForm.password,
      sliderVerified: true,
    });
    ElMessage.success("注册成功，请使用新账号登录");
    loginForm.username = registerForm.username;
    loginForm.password = "";
    loginForm.sliderVerified = false;
    mode.value = "login";
  } finally {
    loading.value = false;
  }
}

function openVerify(action) {
  pendingAction.value = action;
  verifyPassed.value = false;
  verifyVisible.value = true;
}

function finishVerify(value) {
  verifyPassed.value = value;
  if (!value) return;

  // 留出短暂的成功状态展示时间，再关闭弹窗并执行原来的登录或注册动作。
  const action = pendingAction.value;
  window.setTimeout(() => {
    verifyVisible.value = false;
    pendingAction.value = "";
    if (action === "login") {
      loginForm.sliderVerified = true;
      submitLogin();
    } else if (action === "register") {
      registerForm.sliderVerified = true;
      submitRegister();
    }
  }, 350);
}

function resetVerifyDialog() {
  if (!verifyPassed.value) pendingAction.value = "";
  verifyPassed.value = false;
}

function switchMode(value) {
  mode.value = value;
  verifyVisible.value = false;
  pendingAction.value = "";
  verifyPassed.value = false;
  loginForm.sliderVerified = false;
  registerForm.sliderVerified = false;
}

onMounted(async () => {
  try {
    Object.assign(softwareInfo, (await softwareApi.info()).data);
  } catch (_) {
    /* 保留本地软件标识 */
  }
});
</script>

<template>
  <main class="auth-page">
    <section class="auth-story">
      <div class="story-mark">软</div>
      <span class="story-kicker">RUANKAO STUDY WORKSPACE</span>
      <h1>把每一天的积累，<br />变成上岸的底气。</h1>
      <p>计划、章节、错题与计算练习集中管理，让备考进度清晰可见。</p>
      <div class="story-points">
        <span>✓ 重点章节追踪</span><span>✓ 错题周期复盘</span
        ><span>✓ 学习计划量化</span>
      </div>
    </section>
    <section class="auth-panel">
      <div class="auth-card">
        <div class="auth-heading">
          <span>{{ softwareInfo.name }}</span>
          <h2>{{ mode === "login" ? "登录软考进阶台" : "创建备考账号" }}</h2>
          <p>
            {{
              mode === "login"
                ? "继续你的学习计划"
                : "注册后即可保存个人学习数据"
            }}
          </p>
        </div>
        <div class="auth-tabs">
          <button
            :class="{ active: mode === 'login' }"
            @click="switchMode('login')"
          >
            账号登录</button
          ><button
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >
            注册账号
          </button>
        </div>

        <el-form
          v-if="mode === 'login'"
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
          @submit.prevent="login"
        >
          <el-form-item prop="username"
            ><el-input
              v-model="loginForm.username"
              :prefix-icon="User"
              placeholder="用户名"
              autocomplete="username"
          /></el-form-item>
          <el-form-item prop="password"
            ><el-input
              v-model="loginForm.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="密码"
              autocomplete="current-password"
              @keyup.enter="login"
          /></el-form-item>
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
            class="submit-button"
            >登录</el-button
          >
        </el-form>

        <el-form
          v-else
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          size="large"
          @submit.prevent="register"
        >
          <el-form-item prop="username"
            ><el-input
              v-model="registerForm.username"
              :prefix-icon="User"
              placeholder="用户名（字母、数字或下划线）"
              autocomplete="username"
          /></el-form-item>
          <el-form-item prop="nickname"
            ><el-input v-model="registerForm.nickname" placeholder="昵称"
          /></el-form-item>
          <el-form-item prop="password"
            ><el-input
              v-model="registerForm.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="密码（至少6位）"
              autocomplete="new-password"
          /></el-form-item>
          <el-form-item prop="confirmPassword"
            ><el-input
              v-model="registerForm.confirmPassword"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="确认密码"
              autocomplete="new-password"
          /></el-form-item>
          <el-button
            type="primary"
            size="large"
            native-type="submit"
            :loading="loading"
            class="submit-button"
            >注册并开始学习</el-button
          >
        </el-form>
        <div class="auth-foot">
          {{ softwareInfo.name }} {{ softwareInfo.version }}<br />密码使用
          BCrypt 加密保存 · 请妥善保管账号信息
        </div>
      </div>
    </section>

    <el-dialog
      v-model="verifyVisible"
      title="安全验证"
      width="400px"
      append-to-body
      align-center
      :close-on-click-modal="false"
      @closed="resetVerifyDialog"
    >
      <p class="verify-tip">请按住滑块拖动到最右边，完成后将自动继续。</p>
      <SliderVerify
        :model-value="verifyPassed"
        @update:model-value="finishVerify"
      />
    </el-dialog>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: #f3f6f4;
}
.auth-story {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 9vw;
  background: #113b36;
  color: white;
}
.auth-story::after {
  content: "";
  position: absolute;
  width: 440px;
  height: 440px;
  right: -170px;
  bottom: -180px;
  border: 80px solid rgba(232, 190, 102, 0.12);
  border-radius: 50%;
}
.story-mark {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  margin-bottom: 42px;
  border-radius: 16px;
  background: #e7bc66;
  color: #143c37;
  font-size: 28px;
  font-weight: 900;
}
.story-kicker {
  color: #8eb6ae;
  font-size: 11px;
  letter-spacing: 2px;
}
.auth-story h1 {
  margin: 18px 0 22px;
  font-family: Georgia, "Songti SC", serif;
  font-size: clamp(40px, 4.2vw, 64px);
  line-height: 1.22;
  font-weight: 600;
}
.auth-story p {
  max-width: 500px;
  color: #b7d0cb;
  font-size: 16px;
  line-height: 1.9;
}
.story-points {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 38px;
  color: #e5c883;
  font-size: 13px;
}
.auth-panel {
  display: grid;
  place-items: center;
  padding: 50px;
}
.auth-card {
  width: min(440px, 100%);
  padding: 42px;
  border: 1px solid #e0e7e3;
  border-radius: 22px;
  background: white;
  box-shadow: 0 24px 70px rgba(30, 57, 52, 0.1);
}
.auth-heading span {
  color: #1b766b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1px;
}
.auth-heading h2 {
  margin: 8px 0 7px;
  font-size: 27px;
  color: #173b36;
}
.auth-heading p {
  margin: 0;
  color: #8a9893;
  font-size: 13px;
}
.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
  margin: 28px 0 24px;
  padding: 4px;
  border-radius: 10px;
  background: #f1f5f3;
}
.auth-tabs button {
  height: 38px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #7a8884;
  cursor: pointer;
}
.auth-tabs button.active {
  background: white;
  color: #176b61;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(30, 58, 53, 0.08);
}
.submit-button {
  width: 100%;
  margin-top: 4px;
}
.auth-foot {
  margin-top: 23px;
  color: #a1aaa7;
  font-size: 11px;
  text-align: center;
}
.verify-tip {
  margin: 0 0 18px;
  color: #65756f;
  font-size: 13px;
  line-height: 1.7;
}
:deep(.el-form-item) {
  margin-bottom: 18px;
}
:deep(.el-input__wrapper) {
  border-radius: 8px;
}
@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .auth-story {
    display: none;
  }
  .auth-panel {
    padding: 25px;
  }
}
</style>
