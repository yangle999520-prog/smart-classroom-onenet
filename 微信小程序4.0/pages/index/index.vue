<template>
	<view class="page">
		<!-- 设备状态栏 -->
		<view class="status-bar" :class="deviceOnline ? 'status-online' : 'status-offline'">
			<view class="status-dot"></view>
			<text class="status-text">{{ deviceOnline ? '设备在线' : '设备离线' }}</text>
		</view>

		<view class="header">
			<text class="header-title">智能教室管理系统</text>
			<text class="header-subtitle">设备控制面板</text>
		</view>

		<view class="card mode-card" :class="{ 'mode-manual': mode === 'manual' }">
			<view class="card-left">
				<view class="card-icon mode-icon-bg">
					<text class="icon-text">{{mode === 'auto' ? '🤖' : '✋'}}</text>
				</view>
				<view class="card-info">
					<text class="card-label">工作模式</text>
					<text class="card-status mode-status">{{mode === 'auto' ? '自动运行中' : '手动控制中'}}</text>
				</view>
			</view>
			<view class="mode-segment">
				<view
					class="mode-seg-btn"
					:class="{ 'mode-seg-active': mode === 'auto', 'mode-seg-disabled': !deviceOnline }"
					@tap="setMode('auto')"
				>
					<text class="mode-seg-text">自动</text>
				</view>
				<view
					class="mode-seg-btn"
					:class="{ 'mode-seg-active': mode === 'manual', 'mode-seg-disabled': !deviceOnline }"
					@tap="setMode('manual')"
				>
					<text class="mode-seg-text">手动</text>
				</view>
			</view>
		</view>

		<view class="card temp-card">
			<view class="card-left">
				<view class="card-icon temp-icon-bg">
					<text class="icon-text">🌡️</text>
				</view>
				<view class="card-info">
					<text class="card-label">当前温度</text>
					<view class="card-value-row">
						<text class="card-value temp-value">{{temp}}</text>
						<text class="card-unit">℃</text>
					</view>
				</view>
			</view>
			<view class="temp-thermometer">
				<view class="thermometer-track">
					<view class="thermometer-fill" :style="{height: tempBarWidth}"></view>
				</view>
			</view>
		</view>

		<view class="card light-card">
			<view class="card-left">
				<view class="card-icon light-icon-bg">
					<text class="icon-text">☀️</text>
				</view>
				<view class="card-info">
					<text class="card-label">环境光照</text>
					<view class="card-value-row">
						<text class="card-value light-value">{{light}}</text>
						<text class="card-unit">%</text>
					</view>
				</view>
			</view>
			<view class="light-progress">
				<view class="progress-track">
					<view class="progress-fill light-fill" :style="{width: light + '%'}"></view>
				</view>
			</view>
		</view>

		<view class="card lamp-card" :class="{ 'lamp-on': led }">
			<view class="card-left">
				<view class="card-icon lamp-icon-bg">
					<text class="icon-text">{{led ? '💡' : '🔌'}}</text>
				</view>
				<view class="card-info">
					<text class="card-label">台灯</text>
					<text class="card-status">{{mode === 'auto' ? '自动托管中' : (led ? '已开启' : '已关闭')}}</text>
				</view>
			</view>
			<view class="lamp-switch-area">
				<switch :checked="led" :disabled="mode === 'auto' || !deviceOnline" @change="onLedSwitch" />
			</view>
		</view>

		<view class="update-time-card">
			<view class="update-time-row">
				<text class="update-icon">🕐</text>
				<text class="update-label">最近更新</text>
			</view>
			<text class="update-time-value">{{lastUpdateTime || '--:--:--'}}</text>
		</view>
	</view>
</template>

<script>
	const {
		BACKEND_BASE_URL,
		createCommonToken
	} = require(
		'@/key.js'
	)
	export default {
		data() {
			return {
				temp: '0',
				light: '0',
				led: false,
				token: '',
				lastUpdateTime: '',
				mode: 'auto',
				isOperating: false, 
				lockTimer: null,
				/** 定时器ID（用于清理） */
				fetchTimer: null,
				statusTimer: null,
				/** 设备是否在线（从后端 API /api/sensor/device-status 获取） */
				deviceOnline: true,
				/** 设备状态文本 */
				deviceStatusText: '在线'
			}
		},
		computed: {
			tempBarWidth() {
				const t = parseFloat(this.temp) || 0
				const percent = Math.min(Math.max((t / 50) * 100, 0), 100)
				return percent + '%'
			}
		},
		onLoad() {
			const params = {
				access_key: 'mVGNFVfJaWdrvdWZPftvTauZ5b0ZU8mVyRlD5SEr5Vc=',
				version: '2022-05-01',
				productid: '6x3n4Y0FuX',
			}
			this.token = createCommonToken(params);
		},
		onShow(){
			// 先清理旧的定时器，防止重复创建
			this.clearTimers();
			this.fetchDevData();
			this.checkDeviceStatus();
			this.fetchTimer = setInterval(() => {
				this.fetchDevData();
			}, 3000);
			this.statusTimer = setInterval(() => {
				this.checkDeviceStatus();
			}, 10000);
		},
		onHide(){
			// 页面隐藏时清理定时器，下次 onShow 重新创建
			this.clearTimers();
		},
		onUnload(){
			this.clearTimers();
		},
		methods: {
			/** 清理所有定时器 */
			clearTimers() {
				if (this.fetchTimer) {
					clearInterval(this.fetchTimer);
					this.fetchTimer = null;
				}
				if (this.statusTimer) {
					clearInterval(this.statusTimer);
					this.statusTimer = null;
				}
				if (this.lockTimer) {
					clearTimeout(this.lockTimer);
					this.lockTimer = null;
				}
			},
			startOperatingLock() {
				this.isOperating = true;
				if (this.lockTimer) clearTimeout(this.lockTimer);
				this.lockTimer = setTimeout(() => {
					this.isOperating = false;
				}, 3500);
			},

			/** 查询设备在线状态（通过后端 API 代理） */
			checkDeviceStatus() {
				uni.request({
					url: BACKEND_BASE_URL + '/api/sensor/device-status',
					method:'GET',
					success: (res) => {
						if(res.data && res.data.code === 200 && res.data.data) {
							const status = res.data.data;
							this.deviceOnline = (status.online === true);
							this.deviceStatusText = status.statusText || '未知';
						} else {
							this.deviceOnline = false;
							this.deviceStatusText = '接口异常';
						}
					},
					fail: () => {
						// 请求失败时认为设备离线
						this.deviceOnline = false;
						this.deviceStatusText = '查询失败';
					}
				});
			},

			fetchDevData(){
				uni.request({
					url: BACKEND_BASE_URL + '/api/sensor/latest',
					method:'GET',
					success: (res) => {
						if(res.data && res.data.code === 200 && res.data.data) {
							const d = res.data.data;
							this.temp = d.temperature != null ? d.temperature.toString() : '0';
							this.light = d.light != null ? d.light.toString() : '0';

							// 仅在非操作锁定期间更新控制类状态
							if (!this.isOperating) {
								this.led = (d.ledStatus === 1);
								this.mode = (d.mode === 1) ? 'manual' : 'auto';
							}

							this.lastUpdateTime = this.formatTime(new Date());
						}
					},
					fail: () => {
						// 请求失败时不做处理，保持上次数据不变
						console.log('后端数据获取失败');
					}
				});
			},
			onLedSwitch(event){
				if(this.mode === 'auto') {
					uni.showToast({
						title: '自动模式下无法手动控制灯光',
						icon: 'none'
					});
					return;
				}

				// 设备离线时禁止控制
				if (!this.deviceOnline) {
					uni.showToast({
						title: '设备已离线，无法控制',
						icon: 'none'
					});
					return;
				}

				let value = event.detail.value; 
				
				this.startOperatingLock();
				this.led = value; 

				uni.request({
					url: 'https://iot-api.heclouds.com/thingmodel/set-device-property', 
					method:'POST',
					data: {
						product_id:'6x3n4Y0FuX',
						device_name:'D1',
						params: { "led": value } 
					},
					header: {
						'authorization': this.token 
					},
					success: (res) => {
						console.log("台灯控制下发成功", res.data);
					}
				});
			},
			setMode(m) {
				// 设备离线时禁止控制
				if (!this.deviceOnline) {
					uni.showToast({
						title: '设备已离线，无法控制',
						icon: 'none'
					});
					return;
				}

				this.startOperatingLock();
				this.mode = m;
				
				// 🌟 调整下发逻辑：当切换到 'manual'（手动）时下发 true，切换到 'auto'（自动）时下发 false
				const sendValue = (m === 'manual');
				
				uni.request({
					url: 'https://iot-api.heclouds.com/thingmodel/set-device-property',
					method: 'POST',
					data: {
						product_id: '6x3n4Y0FuX',
						device_name: 'D1',
						params: { "mode": sendValue } 
					},
					header: {
						'authorization': this.token
					},
					success: (res) => {
						console.log("工作模式下发成功", res.data);
					},
					fail: (err) => {
						console.error("工作模式下发失败", err);
					}
				});
			},
			formatTime(date) {
				const h = date.getHours().toString().padStart(2, '0')
				const m = date.getMinutes().toString().padStart(2, '0')
				const s = date.getSeconds().toString().padStart(2, '0')
				return h + ':' + m + ':' + s
			}
		}
	}
</script>

<style>
	/* 页面背景 */
	.page {
		min-height: 100vh;
		background-color: #1a1a2e;
		padding: 32rpx 28rpx;
		box-sizing: border-box;
	}

	/* ==================== 设备状态栏 ==================== */
	.status-bar {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: center;
		padding: 12rpx 0;
		margin-bottom: 8rpx;
		border-radius: 12rpx;
	}
	.status-bar.status-online {
		background-color: rgba(76, 175, 80, 0.12);
	}
	.status-bar.status-offline {
		background-color: rgba(244, 67, 54, 0.15);
	}
	.status-dot {
		width: 14rpx;
		height: 14rpx;
		border-radius: 50%;
		margin-right: 10rpx;
	}
	.status-online .status-dot {
		background-color: #4caf50;
		box-shadow: 0 0 12rpx rgba(76, 175, 80, 0.6);
		animation: pulse 2s infinite;
	}
	.status-offline .status-dot {
		background-color: #f44336;
	}
	.status-text {
		font-size: 26rpx;
		font-weight: 500;
	}
	.status-online .status-text {
		color: #4caf50;
	}
	.status-offline .status-text {
		color: #f44336;
	}
	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	/* 顶部标题 */
	.header {
		text-align: center;
		padding: 30rpx 0 28rpx;
	}
	.header-title {
		display: block;
		font-size: 44rpx;
		font-weight: bold;
		color: #e8e8e8;
		letter-spacing: 4rpx;
	}
	.header-subtitle {
		display: block;
		font-size: 24rpx;
		color: #778899;
		margin-top: 6rpx;
	}

	/* 卡片通用 */
	.card {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		background-color: rgba(255, 255, 255, 0.06);
		border-radius: 20rpx;
		padding: 30rpx 26rpx;
		margin-bottom: 22rpx;
		border: 1px solid rgba(255, 255, 255, 0.08);
	}
	.card-left {
		display: flex;
		flex-direction: row;
		align-items: center;
		flex: 1;
	}
	.card-icon {
		width: 84rpx;
		height: 84rpx;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-right: 20rpx;
		flex-shrink: 0;
	}
	.icon-text {
		font-size: 40rpx;
		line-height: 1;
	}
	.card-info {
		display: flex;
		flex-direction: column;
	}
	.card-label {
		font-size: 24rpx;
		color: #778899;
	}
	.card-value-row {
		display: flex;
		flex-direction: row;
		align-items: baseline;
	}
	.card-value {
		font-size: 50rpx;
		font-weight: bold;
		color: #ffffff;
		line-height: 1.2;
	}
	.card-unit {
		font-size: 26rpx;
		color: #778899;
		margin-left: 4rpx;
	}

	/* 模式卡片 */
	.mode-card.mode-manual {
		background-color: rgba(33, 150, 243, 0.1);
		border-color: rgba(33, 150, 243, 0.25);
	}
	.mode-icon-bg {
		background-color: rgba(255, 255, 255, 0.08);
	}
	.mode-manual .mode-icon-bg {
		background-color: rgba(33, 150, 243, 0.22);
	}
	.mode-status {
		font-size: 22rpx;
		color: #4caf50;
		margin-top: 2rpx;
	}
	.mode-manual .mode-status {
		color: #2196f3;
	}
	/* 分段选择器 */
	.mode-segment {
		display: flex;
		flex-direction: row;
		flex-shrink: 0;
		margin-left: 16rpx;
		background-color: rgba(255, 255, 255, 0.08);
		border-radius: 20rpx;
		overflow: hidden;
	}
	.mode-seg-btn {
		padding: 12rpx 24rpx;
		border-radius: 20rpx;
	}
	.mode-seg-btn.mode-seg-active {
		background-color: #2196f3;
	}
	.mode-seg-btn.mode-seg-disabled {
		opacity: 0.4;
	}
	.mode-seg-text {
		font-size: 24rpx;
		color: #8899aa;
	}
	.mode-seg-btn.mode-seg-active .mode-seg-text {
		color: #ffffff;
		font-weight: bold;
	}

	/* 温度卡片 */
	.temp-icon-bg {
		background-color: rgba(255, 107, 107, 0.2);
	}
	.temp-value {
		color: #ff6b6b;
	}
	/* 温度计 */
	.temp-thermometer {
		flex-shrink: 0;
		margin-left: 16rpx;
	}
	.thermometer-track {
		width: 56rpx;
		height: 110rpx;
		border-radius: 28rpx;
		background-color: rgba(255, 255, 255, 0.08);
		position: relative;
		overflow: hidden;
	}
	.thermometer-fill {
		position: absolute;
		bottom: 0;
		left: 0;
		right: 0;
		background-color: #ff6b6b;
		border-radius: 0 0 28rpx 28rpx;
	}

	/* 光照卡片 */
	.light-icon-bg {
		background-color: rgba(255, 193, 7, 0.2);
	}
	.light-value {
		color: #ffc107;
	}
	.light-progress {
		flex-shrink: 0;
		margin-left: 16rpx;
		width: 110rpx;
	}
	.progress-track {
		width: 100%;
		height: 10rpx;
		border-radius: 5rpx;
		background-color: rgba(255, 255, 255, 0.08);
		overflow: hidden;
	}
	.progress-fill {
		height: 100%;
		border-radius: 5rpx;
	}
	.light-fill {
		background-color: #ffc107;
	}

	/* 台灯卡片 */
	.lamp-card.lamp-on {
		background-color: rgba(255, 235, 59, 0.1);
		border-color: rgba(255, 235, 59, 0.25);
	}
	.lamp-icon-bg {
		background-color: rgba(255, 255, 255, 0.08);
	}
	.lamp-on .lamp-icon-bg {
		background-color: rgba(255, 235, 59, 0.22);
	}
	.card-status {
		font-size: 22rpx;
		color: #778899;
		margin-top: 2rpx;
	}
	.lamp-on .card-status {
		color: #e6b800;
	}

	/* 台灯开关 */
	.lamp-switch-area {
		flex-shrink: 0;
		margin-left: 16rpx;
		padding: 8rpx;
	}
	.switch-track {
		width: 88rpx;
		height: 48rpx;
		border-radius: 24rpx;
		background-color: rgba(255, 255, 255, 0.12);
		position: relative;
	}
	.switch-track.switch-on {
		background-color: #ffc107;
	}
	.switch-thumb {
		width: 38rpx;
		height: 38rpx;
		border-radius: 50%;
		background-color: #ffffff;
		position: absolute;
		top: 5rpx;
		left: 5rpx;
		box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.18);
	}
	.switch-thumb.switch-thumb-on {
		left: 45rpx;
	}

	/* 更新时间卡片 */
	.update-time-card {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		background-color: rgba(255, 255, 255, 0.04);
		border-radius: 20rpx;
		padding: 26rpx 30rpx;
		margin-top: 8rpx;
		border: 1px solid rgba(255, 255, 255, 0.05);
	}
	.update-time-row {
		display: flex;
		flex-direction: row;
		align-items: center;
	}
	.update-icon {
		font-size: 32rpx;
		margin-right: 12rpx;
	}
	.update-label {
		font-size: 26rpx;
		color: #778899;
	}
	.update-time-value {
		font-size: 30rpx;
		color: #b0c4de;
		font-weight: bold;
	}
</style>
