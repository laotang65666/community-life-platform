<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, DataLine, Document, Shop } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const phone = computed(() => sessionStorage.getItem('adminPhone') || '管理员')

function logout(): void {
  sessionStorage.removeItem('adminToken')
  sessionStorage.removeItem('adminPhone')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="logo">邻</div>
        <div>
          <div class="title">生活服务平台</div>
          <div class="sub">管理后台</div>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/merchants">
          <el-icon><Shop /></el-icon>
          <span>商户审核</span>
        </el-menu-item>
        <el-menu-item index="/blogs">
          <el-icon><Document /></el-icon>
          <span>内容审核</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title">{{ route.meta.title || '管理后台' }}</div>
        <div class="right">
          <span class="phone">{{ phone }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #111827 0%, #1e3a8a 100%);
  color: #fff;
  padding: 20px 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px 24px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.title {
  font-size: 15px;
  font-weight: 700;
}

.sub {
  font-size: 12px;
  opacity: 0.65;
  margin-top: 2px;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.72);
  margin: 4px 12px;
  border-radius: 10px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}

.header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.phone {
  font-size: 13px;
  color: var(--text-muted);
}

.main {
  padding: 24px;
}
</style>
