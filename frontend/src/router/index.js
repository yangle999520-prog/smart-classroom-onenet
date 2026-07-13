import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import History from '@/views/History.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '实时监控' }
  },
  {
    path: '/history',
    name: 'History',
    component: History,
    meta: { title: '历史数据' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 动态修改页面标题
router.beforeEach((to) => {
  document.title = `${to.meta.title || '首页'} - 智能教室环境监测系统`
})

export default router
