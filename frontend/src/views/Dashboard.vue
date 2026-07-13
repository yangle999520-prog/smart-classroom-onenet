<template>
  <div class="dashboard">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">📊 实时监控面板</h2>
        <p class="page-desc">教室环境数据实时监测与设备状态展示</p>
      </div>
      <div class="header-actions">
        <span class="update-time" v-if="latestData.createTime">
          最后更新: {{ formatTime(latestData.createTime) }}
        </span>
        <span class="status-badge" :class="connected ? 'online' : 'offline'">
          <span class="status-dot"></span>
          {{ connected ? '设备在线' : '设备离线' }}
        </span>
      </div>
    </div>

    <!-- 数据卡片区域 -->
    <div class="cards-grid">
      <!-- 温度卡片 -->
      <div class="data-card temp-card">
        <div class="card-content">
          <div class="card-icon-wrapper temp-icon">
            <span class="card-icon">🌡️</span>
          </div>
          <div class="card-info">
            <div class="card-label">室内温度</div>
            <div class="card-value">
              {{ latestData.temperature ?? '--' }}
              <span class="card-unit">°C</span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <div class="indicator" v-if="latestData.temperature !== null">
            <span class="indicator-dot" :class="tempStatus"></span>
            {{ tempStatusText }}
          </div>
        </div>
      </div>

      <!-- 光照卡片 -->
      <div class="data-card light-card">
        <div class="card-content">
          <div class="card-icon-wrapper light-icon">
            <span class="card-icon">☀️</span>
          </div>
          <div class="card-info">
            <div class="card-label">环境光照</div>
            <div class="card-value">
              {{ latestData.light ?? '--' }}
              <span class="card-unit">%</span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <div class="indicator" v-if="latestData.light !== null">
            <span class="indicator-dot" :class="lightStatus"></span>
            {{ lightStatusText }}
          </div>
        </div>
    </div>

      <!-- LED状态卡片 -->
      <div class="data-card led-card">
        <div class="card-content">
          <div class="card-icon-wrapper led-icon">
            <span class="card-icon">💡</span>
          </div>
          <div class="card-info">
            <div class="card-label">照明状态</div>
            <div class="card-value">
              <span class="led-badge" :class="ledClass">
                {{ ledText }}
              </span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <div class="indicator">
            <span class="indicator-dot" :class="ledClassDot"></span>
            {{ ledText }}
          </div>
        </div>
      </div>

      <!-- 模式卡片 -->
      <div class="data-card mode-card">
        <div class="card-content">
          <div class="card-icon-wrapper mode-icon">
            <span class="card-icon">⚙️</span>
          </div>
          <div class="card-info">
            <div class="card-label">工作模式</div>
            <div class="card-value">
              <span class="mode-badge" :class="modeClass">
                {{ modeText }}
              </span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <span class="indicator">
            {{ modeDesc }}
          </span>
        </div>
      </div>

      <!-- 系统统计卡片 -->
      <div class="data-card stats-card">
        <div class="card-content">
          <div class="card-icon-wrapper stats-icon">
            <span class="card-icon">📦</span>
          </div>
          <div class="card-info">
            <div class="card-label">数据总量</div>
            <div class="card-value">
              {{ statistics.totalRecords || '--' }}
              <span class="card-unit">条</span>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <span class="indicator">
            24h均温 {{ statistics.avgTemperature24h || '--' }}°C
          </span>
        </div>
      </div>
    </div>

    <!-- 最新数据详情和LED阈值控制 -->
    <div class="detail-section">
      <div class="detail-card card">
        <div class="card-title">📋 最新数据详情</div>
        <table class="data-table" v-if="latestData.createTime">
          <thead>
            <tr>
              <th>参数</th>
              <th>数值</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>🌡️ 温度</td>
              <td><strong>{{ latestData.temperature }}°C</strong></td>
              <td>
                <span class="tag" :class="tempTag">{{ tempStatusText }}</span>
              </td>
            </tr>
            <tr>
              <td>☀️ 光照</td>
              <td><strong>{{ latestData.light }}%</strong></td>
              <td>
                <span class="tag" :class="lightTag">{{ lightStatusText }}</span>
              </td>
            </tr>
            <tr>
              <td>💡 LED</td>
              <td><strong>{{ ledText }}</strong></td>
              <td>
                <span class="tag" :class="ledClassTag">{{ ledText }}</span>
              </td>
            </tr>
            <tr>
              <td>⚙️ 模式</td>
              <td><strong>{{ modeText }}</strong></td>
              <td>
                <span class="tag" :class="modeTag">{{ modeDesc }}</span>
              </td>
            </tr>
            <tr>
              <td>🕐 采集时间</td>
              <td colspan="2">{{ formatTime(latestData.createTime) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="no-data" v-else>
          <p>暂无传感器数据，请确保设备已连接并上传数据</p>
        </div>
      </div>

      <div class="detail-card card">
        <div class="card-title">⚙️ 工作模式与控制逻辑</div>
        <div class="logic-box">
          <div class="logic-item mode-info">
            <div class="mode-row">
              <span class="mode-label" :class="modeClass">{{ modeText }}</span>
            </div>
            <span class="logic-desc">{{ modeDesc }}</span>
          </div>
          <div class="logic-divider"></div>
          <div class="logic-item">
            <code>自动模式 mode=0 ：根据光照自动控制</code>
            <span class="logic-desc">光照 &lt; 30% → 开灯，光照 ≥ 30% → 关灯</span>
          </div>
          <div class="logic-item">
            <code>手动模式 mode=1 ：OneNET远程控制</code>
            <span class="logic-desc">通过云平台下发 led=true/false 直接控制灯光</span>
          </div>
        </div>
        <div class="threshold-bar">
          <div class="threshold-label">
            <span>当前光照</span>
            <strong>{{ latestData.light ?? '--' }}%</strong>
          </div>
          <div class="threshold-track">
            <div class="threshold-fill" :style="thresholdStyle"></div>
            <div class="threshold-marker" style="left: 30%;">
              <span class="marker-text">阈值 30%</span>
            </div>
          </div>
          <div class="threshold-labels">
            <span>0%</span>
            <span>100%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近记录迷你表格 -->
    <div class="card recent-section">
      <div class="card-title">
        📝 最近数据记录
        <span class="recent-hint">（最近20条）</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table full-table" v-if="recentList.length > 0">
          <thead>
            <tr>
              <th>#</th>
              <th>温度(°C)</th>
              <th>光照(%)</th>
              <th>LED状态</th>
              <th>工作模式</th>
              <th>采集时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in recentList" :key="item.id">
              <td>{{ index + 1 }}</td>
              <td>{{ item.temperature }}</td>
              <td>{{ item.light }}</td>
              <td>
                <span class="led-dot" :class="item.ledStatus === 1 ? 'on' : 'off'"></span>
                {{ item.ledStatus === 1 ? '开' : '关' }}
              </td>
              <td>
                <span class="mode-dot" :class="item.mode === 1 ? 'manual' : 'auto'"></span>
                {{ item.mode === 1 ? '手动' : '自动' }}
              </td>
              <td>{{ formatTime(item.createTime) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="no-data" v-else>
          <p>暂无记录数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getLatestData, getHistoryData, getStatistics, getDataList, getDeviceStatus, formatTime } from '@/api/request'

// ==================== 状态变量 ====================
const latestData = ref({})
const statistics = ref({})
const recentList = ref([])

/** 设备在线状态（来自 OneNET 平台 /device/detail API） */
const deviceStatus = ref({
  online: false,
  status: -1,
  deviceName: '',
  statusText: '未知',
  lastTime: null
})

/** 连接状态基于后端返回的在线检测 */
const connected = computed(() => deviceStatus.value.online)

let pollTimer = null
let deviceStatusTimer = null
let ws = null
let wsReconnectTimer = null

/** WebSocket 更新节流：至少间隔 500ms 才更新一次页面 */
let lastWsUpdateTime = 0
const WS_UPDATE_THROTTLE_MS = 500

// ==================== WebSocket 实时推送 ====================

/** 建立 WebSocket 连接 */
function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/sensor`

  try {
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      console.log('📡 WebSocket 已连接')
    }

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'sensor_update') {
          // 节流控制：500ms 内不重复更新，让界面有节奏感
          const now = Date.now()
          if (now - lastWsUpdateTime < WS_UPDATE_THROTTLE_MS) {
            return
          }
          lastWsUpdateTime = now

          // WebSocket 实时推送：直接更新最新数据和统计
          latestData.value = {
            temperature: msg.temperature,
            light: msg.light,
            ledStatus: msg.ledStatus,
            mode: msg.mode,
            createTime: msg.createTime
          }
          // 数据总量 +1（实时递增，不需等 HTTP 轮询）
          if (statistics.value.totalRecords !== undefined) {
            statistics.value.totalRecords++
          }
        }
      } catch (e) {
        console.warn('WebSocket 消息解析失败:', e)
      }
    }

    ws.onclose = () => {
      console.log('📡 WebSocket 已断开，3 秒后重连...')
      ws = null
      wsReconnectTimer = setTimeout(connectWebSocket, 3000)
    }

    ws.onerror = () => {
      console.warn('📡 WebSocket 连接异常')
      ws && ws.close()
    }
  } catch (e) {
    console.warn('WebSocket 创建失败，3 秒后重试:', e)
    wsReconnectTimer = setTimeout(connectWebSocket, 3000)
  }
}

// ==================== 温度状态 ====================
const tempStatus = computed(() => {
  const t = latestData.value.temperature
  if (t === null || t === undefined) return 'normal'
  if (t > 30) return 'danger'
  if (t > 26) return 'warning'
  if (t < 18) return 'warning'
  return 'normal'
})

const tempStatusText = computed(() => {
  const t = latestData.value.temperature
  if (t === null || t === undefined) return '--'
  if (t > 30) return '温度过高'
  if (t > 26) return '温度偏高'
  if (t < 18) return '温度偏低'
  return '温度适宜'
})

const tempTag = computed(() => `${tempStatus.value}-tag`)

// ==================== 光照状态 ====================
const lightStatus = computed(() => {
  const l = latestData.value.light
  if (l === null || l === undefined) return 'normal'
  if (l < 20) return 'danger'
  if (l < 40) return 'warning'
  if (l > 90) return 'warning'
  return 'normal'
})

const lightStatusText = computed(() => {
  const l = latestData.value.light
  if (l === null || l === undefined) return '--'
  if (l < 20) return '光照不足'
  if (l < 40) return '光照偏暗'
  if (l > 90) return '光照过强'
  return '光照充足'
})

const lightTag = computed(() => `${lightStatus.value}-tag`)

// ==================== LED状态 ====================
const ledClass = computed(() => latestData.value.ledStatus === 1 ? 'led-on' : 'led-off')
const ledText = computed(() => latestData.value.ledStatus === 1 ? '已开启' : '已关闭')
const ledClassDot = computed(() => ledClass.value)
const ledClassTag = computed(() => ledClass.value)

// ==================== 工作模式 ====================
const modeClass = computed(() => {
  const m = latestData.value.mode
  if (m === null || m === undefined) return 'mode-auto'
  return m === 1 ? 'mode-manual' : 'mode-auto'
})
const modeText = computed(() => {
  const m = latestData.value.mode
  if (m === null || m === undefined) return '--'
  return m === 1 ? '手动模式' : '自动模式'
})
const modeDesc = computed(() => {
  const m = latestData.value.mode
  if (m === null || m === undefined) return '--'
  return m === 1 ? '手动控制中，由OneNET远程操控' : '根据环境光照自动调节灯光'
})
const modeTag = computed(() => modeClass.value)

// ==================== 光照阈值进度条 ====================
const thresholdStyle = computed(() => {
  const l = latestData.value.light
  if (l === null || l === undefined) return { width: '0%' }
  const pct = Math.min(l, 100)
  return { width: `${pct}%` }
})

// ==================== 数据加载 ====================
async function loadData() {
  try {
    // 分别请求，互不影响，一个失败不影响其他数据更新
    const latestResp = getLatestData().catch(e => {
      console.warn('latest数据加载失败:', e.message)
      return null
    })
    const statsResp = getStatistics().catch(e => {
      console.warn('统计数据加载失败:', e.message)
      return null
    })
    const listResp = getDataList(0, 20).catch(e => {
      console.warn('历史数据加载失败:', e.message)
      return null
    })

    const [latest, stats, list] = await Promise.all([latestResp, statsResp, listResp])

    if (latest !== null) {
      latestData.value = latest
    }

    if (stats !== null) {
      statistics.value = stats
    }

    if (list !== null) {
      recentList.value = list
    }
  } catch (e) {
    console.warn('数据加载失败，请检查后端服务是否启动:', e.message)
  }
}

// ==================== 设备状态检测 ====================
async function checkDeviceStatus() {
  try {
    const status = await getDeviceStatus()
    if (status) {
      deviceStatus.value = status
    }
  } catch (e) {
    // 后端不可达时设备视为离线
    deviceStatus.value.online = false
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadData()
  checkDeviceStatus()
  // 连接 WebSocket 接收实时推送（主要数据通道，500ms 节流展示）
  connectWebSocket()
  // HTTP 轮询仅作为 WebSocket 断线时的降级方案，不必频繁请求
  pollTimer = setInterval(loadData, 30000)
  // 每 10 秒检测设备在线状态
  deviceStatusTimer = setInterval(checkDeviceStatus, 10000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (deviceStatusTimer) {
    clearInterval(deviceStatusTimer)
    deviceStatusTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
})
</script>

<style scoped>
/* ==================== 页面结构 ==================== */
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a237e;
  margin-bottom: 4px;
}

.page-desc {
  font-size: 13px;
  color: #999;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.update-time {
  font-size: 12px;
  color: #999;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.online {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.offline {
  background: #fff3e0;
  color: #e65100;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge.online .status-dot {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ==================== 数据卡片 ==================== */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.data-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.data-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.temp-icon { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.light-icon { background: linear-gradient(135deg, #fff8e1, #ffecb3); }
.led-icon { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.mode-icon { background: linear-gradient(135deg, #f3e5f5, #e1bee7); }
.stats-icon { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }

.card-info {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a237e;
  line-height: 1.2;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.card-unit {
  font-size: 14px;
  font-weight: 400;
  color: #999;
}

.card-footer {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #888;
}

.indicator {
  display: flex;
  align-items: center;
  gap: 6px;
}

.indicator-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.indicator-dot.normal { background: #4caf50; }
.indicator-dot.warning { background: #ff9800; }
.indicator-dot.danger { background: #f44336; }

.led-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.led-badge.led-on {
  background: #e3f2fd;
  color: #1565c0;
}

.led-badge.led-off {
  background: #eceff1;
  color: #607d8b;
}

.indicator-dot.led-on { background: #1565c0; }
.indicator-dot.led-off { background: #90a4ae; }

/* ==================== 模式卡片 ==================== */
.mode-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.mode-badge.mode-auto {
  background: #e8f5e9;
  color: #2e7d32;
}

.mode-badge.mode-manual {
  background: #fff3e0;
  color: #e65100;
}

.mode-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.mode-dot.auto { background: #4caf50; }
.mode-dot.manual { background: #ff9800; }

.mode-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
}

.mode-row {
  margin-bottom: 6px;
}

.mode-label {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
}

.mode-label.mode-auto {
  background: #e8f5e9;
  color: #2e7d32;
}

.mode-label.mode-manual {
  background: #fff3e0;
  color: #e65100;
}

.logic-divider {
  height: 1px;
  background: #e0e0e0;
  margin: 4px 0;
}

/* ==================== 最近记录 ==================== */
.detail-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-card {
  padding: 20px 24px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  text-align: left;
  padding: 8px 12px;
  font-size: 12px;
  color: #999;
  font-weight: 500;
  border-bottom: 2px solid #f0f0f0;
}

.data-table td {
  padding: 10px 12px;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.normal-tag { background: #e8f5e9; color: #2e7d32; }
.warning-tag { background: #fff3e0; color: #e65100; }
.danger-tag { background: #ffebee; color: #c62828; }
.led-on { background: #e3f2fd; color: #1565c0; }
.led-off { background: #eceff1; color: #607d8b; }
.mode-auto-tag { background: #e8f5e9; color: #2e7d32; }
.mode-manual-tag { background: #fff3e0; color: #e65100; }

.no-data {
  padding: 30px 0;
  text-align: center;
  color: #bbb;
  font-size: 14px;
}

/* ==================== 控制逻辑 ==================== */
.logic-box {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logic-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logic-item code {
  background: #f5f5f5;
  padding: 8px 12px;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #e65100;
  border: 1px solid #e0e0e0;
}

.logic-desc {
  font-size: 12px;
  color: #999;
  padding-left: 4px;
}

.threshold-bar {
  margin-top: 20px;
}

.threshold-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.threshold-track {
  position: relative;
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: visible;
}

.threshold-fill {
  height: 100%;
  background: linear-gradient(90deg, #4caf50, #8bc34a, #ffeb3b, #ff9800, #f44336);
  border-radius: 6px;
  transition: width 0.5s ease;
}

.threshold-marker {
  position: absolute;
  top: -16px;
  transform: translateX(-50%);
}

.marker-text {
  font-size: 10px;
  color: #e65100;
  font-weight: 600;
  white-space: nowrap;
  background: #fff3e0;
  padding: 1px 6px;
  border-radius: 4px;
}

.threshold-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
}

/* ==================== 最近记录表格 ==================== */
.recent-section {
  margin-top: 0;
}

.recent-hint {
  font-size: 12px;
  color: #aaa;
  font-weight: 400;
}

.table-wrapper {
  overflow-x: auto;
}

.full-table td {
  padding: 8px 14px;
}

.led-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.led-dot.on { background: #1565c0; box-shadow: 0 0 6px rgba(21, 101, 192, 0.4); }
.led-dot.off { background: #90a4ae; }

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .cards-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }

  .page-header {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
