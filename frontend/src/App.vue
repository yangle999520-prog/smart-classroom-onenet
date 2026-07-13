<template>
  <div id="app-container">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-left">
        <div class="logo-icon">🏫</div>
        <div class="header-title">
          <h1>智能教室环境监测与照明控制系统</h1>
          <p class="header-subtitle">Smart Classroom IoT Monitoring System</p>
        </div>
      </div>
      <nav class="header-nav">
        <router-link to="/dashboard" class="nav-item" active-class="active">
          <span class="nav-icon">📊</span>
          <span>实时监控</span>
        </router-link>
        <router-link to="/history" class="nav-item" active-class="active">
          <span class="nav-icon">📈</span>
          <span>历史数据</span>
        </router-link>
      </nav>
    </header>

    <!-- 页面主体 -->
    <main class="app-main">
      <router-view />
    </main>

    <!-- 底部信息 -->
    <footer class="app-footer">
      <span>智能教室环境监测系统 &copy; 2026</span>
      <span class="footer-divider">|</span>
      <span>基于 STM32 + ESP8266 + OneNET + SpringBoot + Vue3</span>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 自动路由到仪表盘
onMounted(() => {
  if (router.currentRoute.value.path === '/') {
    router.push('/dashboard')
  }
})
</script>

<style>
/* ==================== 全局样式重置 ==================== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f5;
  color: #333;
  min-height: 100vh;
}

#app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ==================== 顶部导航栏 ==================== */
.app-header {
  background: linear-gradient(135deg, #1a237e 0%, #283593 50%, #3949ab 100%);
  color: #fff;
  padding: 0 40px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-icon {
  font-size: 32px;
}

.header-title h1 {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.header-subtitle {
  font-size: 11px;
  opacity: 0.7;
  letter-spacing: 0.5px;
  margin-top: 2px;
}

.header-nav {
  display: flex;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 500;
}

.nav-icon {
  font-size: 16px;
}

/* ==================== 页面主体 ==================== */
.app-main {
  flex: 1;
  padding: 24px 40px;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
}

/* ==================== 底部信息 ==================== */
.app-footer {
  background: #fff;
  padding: 14px 40px;
  text-align: center;
  font-size: 13px;
  color: #999;
  border-top: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.footer-divider {
  color: #ddd;
}

/* ==================== 通用工具样式 ==================== */
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px 24px;
  transition: box-shadow 0.3s ease;
}

.card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #555;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* ==================== 响应式适配 ==================== */
@media (max-width: 768px) {
  .app-header {
    padding: 0 16px;
    height: 60px;
    flex-wrap: wrap;
  }

  .header-title h1 {
    font-size: 15px;
  }

  .header-subtitle {
    display: none;
  }

  .nav-item {
    padding: 6px 12px;
    font-size: 13px;
  }

  .app-main {
    padding: 16px;
  }
}
</style>
