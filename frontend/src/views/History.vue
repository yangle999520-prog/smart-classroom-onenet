<template>
  <div class="history-page">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div>
        <h2 class="page-title">📈 历史数据曲线</h2>
        <p class="page-desc">传感器历史数据趋势分析与可视化展示</p>
      </div>
      <div class="header-actions">
        <div class="time-selector">
          <button
            v-for="opt in timeOptions"
            :key="opt.value"
            :class="['time-btn', { active: selectedHours === opt.value }]"
            @click="changeTimeRange(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>
        <button class="refresh-btn" @click="loadHistory" :disabled="loading">
          <span :class="{ rotating: loading }">🔄</span>
          {{ loading ? '加载中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-icon temp-stat">🌡️</div>
        <div class="stat-info">
          <div class="stat-label">平均温度</div>
          <div class="stat-value">{{ statistics.avgTemperature24h || '--' }}°C</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon temp-stat">🔺</div>
        <div class="stat-info">
          <div class="stat-label">最高温度</div>
          <div class="stat-value highlight-warm">{{ statistics.maxTemperature24h || '--' }}°C</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon temp-stat">🔻</div>
        <div class="stat-info">
          <div class="stat-label">最低温度</div>
          <div class="stat-value highlight-cool">{{ statistics.minTemperature24h || '--' }}°C</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon light-stat">☀️</div>
        <div class="stat-info">
          <div class="stat-label">平均光照</div>
          <div class="stat-value">{{ statistics.avgLight24h || '--' }}%</div>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon led-stat">💡</div>
        <div class="stat-info">
          <div class="stat-label">LED开启率</div>
          <div class="stat-value">{{ statistics.ledOnRate24h || '--' }}%</div>
        </div>
      </div>
    </div>

    <!-- 温度曲线图 -->
    <div class="chart-container">
      <div class="card">
        <div class="card-title">
          🌡️ 温度变化趋势
          <span class="chart-hint">（{{ selectedHours }}小时内）</span>
        </div>
        <div class="chart-wrapper" ref="tempChartRef"></div>
      </div>
    </div>

    <!-- 光照曲线图 -->
    <div class="chart-container">
      <div class="card">
        <div class="card-title">
          ☀️ 光照变化趋势
          <span class="chart-hint">（{{ selectedHours }}小时内）</span>
        </div>
        <div class="chart-wrapper" ref="lightChartRef"></div>
      </div>
    </div>

    <!-- LED状态分布 -->
    <div class="chart-container" style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
      <div class="card">
        <div class="card-title">💡 LED状态分布</div>
        <div class="chart-wrapper-small" ref="ledChartRef"></div>
      </div>
      <div class="card">
        <div class="card-title">📊 温湿度对照</div>
        <div class="chart-wrapper-small" ref="scatterChartRef"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getHistoryData, getStatistics, formatTimeShort } from '@/api/request'

// ==================== 状态变量 ====================
const tempChartRef = ref(null)
const lightChartRef = ref(null)
const ledChartRef = ref(null)
const scatterChartRef = ref(null)

const historyData = ref([])
const statistics = ref({})
const selectedHours = ref(24)
const loading = ref(false)

let tempChart = null
let lightChart = null
let ledChart = null
let scatterChart = null
let resizeObserver = null

const timeOptions = [
  { label: '1小时', value: 1 },
  { label: '6小时', value: 6 },
  { label: '12小时', value: 12 },
  { label: '24小时', value: 24 },
  { label: '7天', value: 168 },
  { label: '30天', value: 720 }
]

// ==================== 切换时间范围 ====================
function changeTimeRange(hours) {
  selectedHours.value = hours
  loadHistory()
}

// ==================== 加载历史数据 ====================
async function loadHistory() {
  loading.value = true
  try {
    const [data, stats] = await Promise.all([
      getHistoryData(selectedHours.value),
      getStatistics()
    ])
    historyData.value = data || []
    statistics.value = stats || {}
    nextTick(() => {
      renderCharts()
    })
  } catch (e) {
    console.warn('加载历史数据失败:', e.message)
  } finally {
    loading.value = false
  }
}

// ==================== 渲染图表 ====================
function renderCharts() {
  renderTempChart()
  renderLightChart()
  renderLedChart()
  renderScatterChart()
}

function getChartTimes() {
  return historyData.value.map(d => formatTimeShort(d.createTime))
}

// 1. 温度曲线图
function renderTempChart() {
  if (tempChart) tempChart.dispose()
  if (!tempChartRef.value) return

  const times = getChartTimes()
  const values = historyData.value.map(d => d.temperature)

  tempChart = echarts.init(tempChartRef.value)
  tempChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: 13 },
      formatter: function(params) {
        const p = params[0]
        return `<div style="font-weight:600;margin-bottom:4px;">${p.axisValue}</div>
                <div>🌡️ 温度: <strong>${p.value}°C</strong></div>`
      }
    },
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: times,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#999', fontSize: 11, interval: Math.max(Math.floor(times.length / 12), 1) },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '温度 (°C)',
      nameTextStyle: { color: '#999', fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 11 }
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2.5, color: '#ff7043' },
      itemStyle: { color: '#ff7043' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,112,67,0.25)' },
          { offset: 1, color: 'rgba(255,112,67,0.02)' }
        ])
      },
      markLine: {
        silent: true,
        data: [
          { yAxis: 26, label: { formatter: '警告线 26°C', color: '#ff9800', fontSize: 10 } },
          { yAxis: 30, label: { formatter: '危险线 30°C', color: '#f44336', fontSize: 10 } }
        ],
        lineStyle: { type: 'dashed', opacity: 0.5 }
      }
    }]
  })
}

// 2. 光照曲线图
function renderLightChart() {
  if (lightChart) lightChart.dispose()
  if (!lightChartRef.value) return

  const times = getChartTimes()
  const values = historyData.value.map(d => d.light)

  lightChart = echarts.init(lightChartRef.value)
  lightChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: { color: '#333', fontSize: 13 },
      formatter: function(params) {
        const p = params[0]
        return `<div style="font-weight:600;margin-bottom:4px;">${p.axisValue}</div>
                <div>☀️ 光照: <strong>${p.value}%</strong></div>`
      }
    },
    grid: { left: 60, right: 30, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: times,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#999', fontSize: 11, interval: Math.max(Math.floor(times.length / 12), 1) },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '光照 (%)',
      nameTextStyle: { color: '#999', fontSize: 12 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 11 }
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2.5, color: '#ffb300' },
      itemStyle: { color: '#ffb300' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,179,0,0.25)' },
          { offset: 1, color: 'rgba(255,179,0,0.02)' }
        ])
      },
      markLine: {
        silent: true,
        data: [
          { yAxis: 30, label: { formatter: '阈值 30%', color: '#e65100', fontSize: 10 } }
        ],
        lineStyle: { type: 'dashed', color: '#e65100', opacity: 0.5 }
      }
    }]
  })
}

// 3. LED状态分布（饼图）
function renderLedChart() {
  if (ledChart) ledChart.dispose()
  if (!ledChartRef.value) return

  const onCount = historyData.value.filter(d => d.ledStatus === 1).length
  const offCount = historyData.value.filter(d => d.ledStatus === 0).length
  const total = onCount + offCount

  ledChart = echarts.init(ledChartRef.value)
  ledChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)'
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      label: {
        show: true,
        fontSize: 13,
        fontWeight: 600,
        formatter: '{b}\n{d}%'
      },
      emphasis: {
        label: { show: true, fontSize: 15, fontWeight: 'bold' }
      },
      data: [
        { value: onCount, name: 'LED开启', itemStyle: { color: '#1565c0' } },
        { value: offCount, name: 'LED关闭', itemStyle: { color: '#90a4ae' } }
      ]
    }]
  })
}

// 4. 温-光照散点图
function renderScatterChart() {
  if (scatterChart) scatterChart.dispose()
  if (!scatterChartRef.value) return

  const data = historyData.value.map(d => [d.temperature, d.light, d.ledStatus])

  scatterChart = echarts.init(scatterChartRef.value)
  scatterChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        return `🌡️ 温度: ${params.value[0]}°C<br/>☀️ 光照: ${params.value[1]}%`
      }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      name: '温度 (°C)',
      nameTextStyle: { color: '#999', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      splitLine: { show: false },
      axisLabel: { color: '#999', fontSize: 11 }
    },
    yAxis: {
      name: '光照 (%)',
      nameTextStyle: { color: '#999', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
      axisLabel: { color: '#999', fontSize: 11 }
    },
    series: [{
      type: 'scatter',
      data: data,
      symbolSize: 10,
      itemStyle: {
        color: function(params) {
          return params.value[2] === 1 ? '#1565c0' : '#90a4ae'
        },
        opacity: 0.7
      }
    }]
  })
}

// ==================== 响应式适配 ====================
function setupResizeObserver() {
  resizeObserver = new ResizeObserver(() => {
    tempChart?.resize()
    lightChart?.resize()
    ledChart?.resize()
    scatterChart?.resize()
  })

  const containers = [tempChartRef, lightChartRef, ledChartRef, scatterChartRef]
  containers.forEach(ref => {
    if (ref.value) resizeObserver.observe(ref.value)
  })
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadHistory()
  nextTick(() => {
    setupResizeObserver()
  })
})

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect()
  tempChart?.dispose()
  lightChart?.dispose()
  ledChart?.dispose()
  scatterChart?.dispose()
})
</script>

<style scoped>
.history-page {
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
  gap: 12px;
  flex-wrap: wrap;
}

.time-selector {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 3px;
  border-radius: 8px;
}

.time-btn {
  padding: 5px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.time-btn:hover {
  color: #333;
}

.time-btn.active {
  background: #1a237e;
  color: #fff;
  font-weight: 500;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #d0d0d0;
  border-radius: 8px;
  color: #555;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  border-color: #1a237e;
  color: #1a237e;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.rotating {
  display: inline-block;
  animation: spin 1s linear infinite;
}

/* ==================== 统计概览 ==================== */
.stats-row {
  display: flex;
  gap: 12px;
}

.stat-item {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.temp-stat { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.light-stat { background: linear-gradient(135deg, #fff8e1, #ffecb3); }
.led-stat { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }

.stat-info {
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: #888;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1a237e;
}

.stat-value.highlight-warm { color: #e65100; }
.stat-value.highlight-cool { color: #1565c0; }

/* ==================== 图表容器 ==================== */
.chart-container {
  margin-bottom: 0;
}

.chart-wrapper {
  width: 100%;
  height: 360px;
}

.chart-wrapper-small {
  width: 100%;
  height: 300px;
}

.chart-hint {
  font-size: 12px;
  color: #aaa;
  font-weight: 400;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1024px) {
  .stats-row {
    flex-wrap: wrap;
  }
  .stat-item {
    min-width: calc(33% - 10px);
  }
  .chart-container[style] {
    grid-template-columns: 1fr !important;
  }
}

@media (max-width: 600px) {
  .stats-row {
    flex-direction: column;
  }
  .stat-item {
    min-width: 100%;
  }
  .page-header {
    flex-direction: column;
    gap: 12px;
  }
  .header-actions {
    width: 100%;
  }
  .time-selector {
    flex-wrap: wrap;
  }
  .chart-wrapper {
    height: 260px;
  }
  .chart-wrapper-small {
    height: 220px;
  }
}
</style>
