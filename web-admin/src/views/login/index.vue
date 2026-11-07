<template>
  <main class="login">
    <section class="brand">
      <div class="brand-top">
        <span class="logo">FM</span>
        <span class="brand-name">消防监控平台</span>
      </div>

      <div class="brand-copy">
        <h1>一屏掌握<br>每一处火警与隐患</h1>
        <p>设备报警接入、值守核警、派单处置、隐患整改与巡查，全部收敛到同一条闭环链路。</p>
        <div class="brand-points">
          <span>火警处置</span>
          <span>派单处置</span>
          <span>隐患审核</span>
          <span>数据大屏</span>
        </div>
      </div>

      <svg class="illustration" viewBox="0 0 520 420" fill="none" aria-hidden="true">
        <g stroke="#38bdf8" stroke-width="1.2" opacity="0.8">
          <path d="M40 360h440" />
          <path d="M90 360V190l70-40v210" />
          <path d="M160 190h70v170" />
          <path d="M230 360V120l90-52v292" />
          <path d="M320 132h90v228" />
          <path d="M90 230h70M90 270h70M90 310h70" />
          <path d="M250 170h50M250 210h50M250 250h50M250 290h50" />
          <path d="M340 180h50M340 220h50M340 260h50M340 300h50" />
          <circle cx="196" cy="150" r="3" fill="#22d3ee" stroke="none" />
          <circle cx="356" cy="150" r="3" fill="#22d3ee" stroke="none" />
          <circle cx="356" cy="332" r="3" fill="#ff7a45" stroke="none" />
        </g>
        <g stroke="#ff7a45" stroke-width="1.4" opacity="0.9">
          <path d="M420 120c0-18-12-28-20-42-2 16-12 22-12 38a16 16 0 0 0 32 4z" />
          <path d="M402 128c-8 0-14-6-14-14" />
        </g>
        <g stroke="#38bdf8" stroke-width="0.8" opacity="0.35">
          <path d="M0 380h520M0 340h520M0 300h520M0 260h520M0 220h520M0 180h520" />
        </g>
      </svg>

      <div class="brand-foot">
        <span>版本 v1.0 · P1</span>
        <span>值守 · 处置 · 巡查</span>
      </div>
    </section>

    <section class="form-side">
      <div class="form-box">
        <h2>登录</h2>
        <p class="hint">使用平台管理员、租户管理员或值守账号进入后台</p>

        <div v-if="errorMessage" class="form-alert" role="alert">
          {{ errorMessage }}
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="userName">
            <el-input
              v-model="form.userName"
              size="large"
              placeholder="请输入账号"
              autocomplete="username"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>

          <div class="row-between">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="muted">忘记密码请联系管理员</span>
          </div>

          <el-button
            class="login-btn"
            type="primary"
            size="large"
            :loading="loading"
            @click="submit"
          >
            {{ loading ? '登录中…' : '登录' }}
          </el-button>
        </el-form>

        <p class="form-foot">登录即代表你已获得平台授权</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';

interface LoginForm {
  userName: string;
  password: string;
}

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const app = useAppStore();

const formRef = ref<FormInstance>();
const form = reactive<LoginForm>({ userName: '', password: '' });
const remember = ref(true);
const loading = ref(false);
const errorMessage = ref('');

const rules: FormRules<LoginForm> = {
  userName: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

onMounted(() => {
  const account = localStorage.getItem(auth.accountKey) ?? '';
  if (account) {
    form.userName = account;
    remember.value = true;
  }
});

async function submit(): Promise<void> {
  if (loading.value) {
    return;
  }
  errorMessage.value = '';
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  loading.value = true;
  try {
    await auth.login({ userName: form.userName.trim(), password: form.password });
    if (!remember.value) {
      localStorage.removeItem(auth.accountKey);
    }
    app.resetTabs();
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '';
    const target = redirect && auth.hasMenuPath(redirect) ? redirect : auth.landingPath();
    await router.replace(target === '/login' ? auth.landingPath() : target);
  } catch (error) {
    // 原始错误仅留在控制台/上报占位，不上屏，避免泄露后端细节或枚举账号
    console.error('[login] 登录失败', error);
    errorMessage.value = '用户名或密码错误';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login {
  display: grid;
  grid-template-columns: 45% 55%;
  min-height: 100vh;
  background: #ffffff;
}

.brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  overflow: hidden;
  background:
    radial-gradient(circle at 15% 20%, rgb(34 211 238 / 14%), transparent 42%),
    radial-gradient(circle at 85% 80%, rgb(242 100 48 / 16%), transparent 45%),
    var(--fm-navy-900);
  color: #e6f1ff;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgb(230 241 255 / 24%);
  border-radius: 10px;
  color: #22d3ee;
  font-family: var(--fm-font-number);
  font-size: 18px;
  font-weight: 700;
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.brand-copy {
  position: relative;
  z-index: 2;
  max-width: 460px;
  margin: 48px 0;
}

.brand-copy h1 {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.3;
}

.brand-copy p {
  margin-top: 14px;
  color: #9fb3c8;
  font-size: 14px;
  line-height: 1.8;
}

.brand-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 24px;
}

.brand-points span {
  padding: 5px 10px;
  border: 1px solid rgb(56 189 248 / 28%);
  border-radius: 999px;
  color: #b9d9f0;
  font-size: 12px;
}

.illustration {
  position: absolute;
  right: -40px;
  bottom: 40px;
  width: 520px;
  opacity: 0.5;
  pointer-events: none;
}

.brand-foot {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  color: #6f8aa5;
  font-size: 12px;
}

.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-box {
  width: 360px;
}

.form-box h2 {
  font-size: 26px;
  font-weight: 700;
}

.hint {
  margin-top: 6px;
  color: var(--fm-text-secondary);
  font-size: 13px;
}

.form-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding: 9px 12px;
  border: 1px solid #fecdca;
  border-radius: var(--fm-radius-sm);
  background: var(--fm-danger-soft);
  color: var(--fm-danger);
  font-size: 13px;
}

.form-box :deep(.el-form) {
  margin-top: 20px;
}

.form-box :deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 13px;
  font-weight: 500;
}

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 18px;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 14px;
}

.form-foot {
  margin-top: 22px;
  color: var(--fm-text-muted);
  font-size: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .login {
    grid-template-columns: 1fr;
  }

  .brand {
    display: none;
  }
}
</style>
