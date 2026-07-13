import axios from 'axios'
import dayjs from 'dayjs'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    console.error('API异常:', res.message)
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('网络错误:', error.message)
    return Promise.reject(error)
  }
)

// ==================== API 接口封装 ====================

/** 获取最新传感器数据 */
export function getLatestData() {
  return request.get('/sensor/latest')
}

/** 获取历史数据 */
export function getHistoryData(hours = 24) {
  return request.get('/sensor/history', { params: { hours } })
}

/** 获取统计数据 */
export function getStatistics() {
  return request.get('/sensor/statistics')
}

/** 获取分页数据列表 */
export function getDataList(page = 0, size = 20) {
  return request.get('/sensor/list', { params: { page, size } })
}

/** 上传传感器数据（模拟） */
export function uploadSensorData(data) {
  return request.post('/sensor/upload', data)
}

/** 获取设备在线状态 */
export function getDeviceStatus() {
  return request.get('/sensor/device-status')
}

/**
 * 格式化时间
 */
export function formatTime(time) {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

export function formatTimeShort(time) {
  if (!time) return ''
  return dayjs(time).format('HH:mm:ss')
}

export default request
