import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'

import AdminLayout from '../layouts/AdminLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import MerchantAuditView from '../views/MerchantAuditView.vue'
import BlogModerationView from '../views/BlogModerationView.vue'
import KnowledgeView from '../views/KnowledgeView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: DashboardView, meta: { title: '数据看板' } },
      { path: 'merchants', component: MerchantAuditView, meta: { title: '商户审核' } },
      { path: 'blogs', component: BlogModerationView, meta: { title: '内容审核' } },
      { path: 'knowledge', component: KnowledgeView, meta: { title: '知识库' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = sessionStorage.getItem('adminToken')
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
