<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Key } from '@element-plus/icons-vue'
import { login, sendSmsCode } from '../api/auth'

const router = useRouter()

const phone = ref('13900000000')
const code = ref('')
const loading = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

async function sendCode(): Promise<void> {
  if (!phone.value) {
    ElMessage.warning('请输入手机号')
    return
  }

  try {
    await sendSmsCode(phone.value)
    ElMessage.success('验证码已发送,请在服务端日志搜索 [SMS]')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch {
    // 错误提示由 axios 拦截器统一处理
  }
}

async function doLogin(): Promise<void> {
  if (!phone.value || !code.value) {
    ElMessage.warning('请输入手机号和验证码')
    return
  }

  loading.value = true
  try {
    const data = await login(phone.value, code.value)
    sessionStorage.setItem('adminToken', data.accessToken)
    sessionStorage.setItem('adminPhone', phone.value)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <div class="logo">邻</div>
        <div>
          <div class="title">生活服务平台</div>
          <div class="sub">管理后台</div>
        </div>
      </div>

      <h2>管理员登录</h2>
      <p class="hint">
        需要 ADMIN 账号。开发环境验证码会打印在服务端日志中,搜索
        <code>[SMS]</code> 即可看到。
      </p>

      <el-form @submit.prevent>
        <el-form-item>
          <el-input
            v-model="phone"
            size="large"
            placeholder="手机号"
            :prefix-icon="Iphone"
          />
        </el-form-item>

        <el-form-item>
          <div class="code-row">
            <el-input
              v-model="code"
              size="large"
              placeholder="验证码"
              :prefix-icon="Key"
              @keyup.enter="doLogin"
            />
            <el-button
              size="large"
              :disabled="countdown > 0"
              @click="sendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="login-btn"
          :loading="loading"
          @click="doLogin"
        >
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(37, 99, 235, 0.18), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(16, 185, 129, 0.12), transparent 45%),
    #f4f6fb;
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
  padding: 34px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #1e3a8a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.title {
  font-size: 16px;
  font-weight: 700;
}

.sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.hint {
  margin: 0 0 24px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.hint code {
  background: #f1f5f9;
  border-radius: 4px;
  padding: 1px 5px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.login-btn {
  width: 100%;
  margin-top: 6px;
}
</style>
