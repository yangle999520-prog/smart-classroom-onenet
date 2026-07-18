# 智能教室环境监测系统：启动文档

本文档说明如何启动 Web 前后端、小程序开发端，以及如何连接真实 STM32+ESP8266 设备。

## 1. 启动方式选择

| 场景 | 后端配置 | 是否需要硬件/OneNET |
| --- | --- | --- |
| 只验证页面和接口 | `onenet.pulsar.enabled=false` | 否，可用 `/upload` 写入测试数据 |
| 接收真实 OneNET 数据 | `onenet.pulsar.enabled=true` | 是，需要有效 Pulsar 服务端订阅 |
| 查看设备在线状态 | `onenet.api.enabled=true` | 需要 OneNET API 参数 |

推荐第一次启动使用“只验证页面和接口”模式，确认数据库、后端和前端均正常后，再接入真实云平台。

## 2. 环境要求

### Web 本地运行

- JDK 8 或更高版本，项目编译目标为 Java 8；
- Maven 3.6+；
- Node.js 18+，建议使用 npm；
- MySQL 8.x；
- 可访问 Maven Central、npm registry 和 OneNET 的网络环境（首次安装依赖时需要）。

### 真实设备运行

- Keil MDK/兼容的 ARM 编译环境；
- STM32F103 开发板；
- ESP8266 模块；
- DS18B20、光照传感器和 LED；
- OneNET 产品、设备、设备密钥及 MQTT/Pulsar 服务端订阅配置。

### 小程序运行

- 微信开发者工具；
- 项目目录：`微信小程序4.0`；
- 手机或模拟器能够访问运行后端的局域网 IP。

## 3. 数据库准备

先启动 MySQL，然后执行 `backend/src/main/resources/schema.sql`。

该脚本会删除并重建 `iot_classroom` 数据库，可能造成数据丢失。已有数据时不要直接执行，可改为手动创建数据库和 `sensor_data` 表，或先备份数据库。

默认连接配置为：

```text
地址：localhost:3306
数据库：iot_classroom
用户名：root
密码：123456
```

如果本机配置不同，请修改 `backend/src/main/resources/application.yml`，或通过 Spring Boot 外部配置覆盖 `spring.datasource.*`。

## 4. 后端启动

在项目根目录执行：

```powershell
cd backend
mvn spring-boot:run
```

也可以先打包再运行：

```powershell
cd backend
mvn clean package
java -jar target/classroom-backend-1.0.0.jar
```

后端默认端口为 `8080`。

### 本地无 OneNET 时的配置

将 `backend/src/main/resources/application.yml` 中的：

```yaml
onenet:
  pulsar:
    enabled: false
```

保留或设置为 `false`。`simulator.enabled` 当前默认关闭；即使关闭 Pulsar，仍可使用下面的上传接口写入测试数据。

### OneNET 实时接入配置

需要配置以下参数：

```yaml
onenet:
  pulsar:
    enabled: true
    access-id: <OneNET 消费组 ID>
    secret-key: <OneNET 消费组密钥>
    subscription: <订阅名称>
  api:
    enabled: true
    product-id: <产品 ID>
    device-name: <设备名称>
    access-key: <设备或产品 AccessKey>
    version: 2022-05-01
```

不要把真实密钥提交到 Git。建议使用环境变量、`application-local.yml` 或部署平台的 Secret 管理功能覆盖这些值。

启动日志中应重点确认：

- Spring Boot 已监听 8080；
- Pulsar 消费者已连接到 OneNET Topic；
- 没有数据库连接错误；
- `/device-status` 查询时 OneNET 参数完整。

## 5. 前端启动

另开一个 PowerShell 窗口执行：

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：<http://localhost:3000>

Vite 已配置代理：

- `/api` → `http://localhost:8080`
- `/ws` → `ws://localhost:8080`

因此开发环境不需要在前端代码里填写后端地址。打开页面后访问 `/dashboard`，也可以进入 `/history` 查看历史图表。

## 6. 快速验证

### 6.1 检查后端健康响应

```powershell
Invoke-WebRequest http://localhost:8080/api/sensor/latest
```

没有数据时，响应中的 `data` 为 `null` 属于正常现象。

### 6.2 写入一条测试数据

```powershell
$body = @{
  temperature = 25.6
  light = 42
  ledStatus = 0
  mode = 0
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/sensor/upload `
  -ContentType 'application/json' `
  -Body $body
```

然后打开：

```text
http://localhost:3000/dashboard
```

### 6.3 检查历史与统计接口

```powershell
Invoke-RestMethod 'http://localhost:8080/api/sensor/history?hours=24'
Invoke-RestMethod 'http://localhost:8080/api/sensor/statistics'
Invoke-RestMethod 'http://localhost:8080/api/sensor/list?page=0&size=20'
```

### 6.4 检查 WebSocket

浏览器打开仪表盘后，开发者工具 Network 中应看到 `/ws/sensor` 连接。执行一次 `/upload` 后，如果实时连接正常，仪表盘最新数据应更新。

## 7. 小程序启动

1. 确认 Spring Boot 后端已启动，并监听 `0.0.0.0:8080` 或可被局域网访问。
2. 打开 `微信小程序4.0/key.js`，将 `BACKEND_BASE_URL` 修改为运行后端电脑的局域网地址，例如：

   ```js
   const BACKEND_BASE_URL = 'http://192.168.0.111:8080'
   ```

   不能填写 `localhost`，因为小程序运行在手机或微信开发者工具环境中。
3. 使用微信开发者工具导入 `微信小程序4.0` 目录，项目 AppID 以 `project.config.json` 为准。
4. 编译并预览首页，页面会显示设备状态、温度、光照、工作模式和台灯状态。
5. 小程序会每 3 秒请求 `/api/sensor/latest`，每 10 秒请求 `/api/sensor/device-status`。
6. 自动模式下台灯开关不可手动操作；切换到手动模式后，可通过 OneNET 物模型接口下发 `led` 属性控制台灯。

小程序控制请求直接发送到：

```text
POST https://iot-api.heclouds.com/thingmodel/set-device-property
```

真实部署时应配置微信小程序合法域名、HTTPS 和安全的密钥管理，不建议将长期有效 AccessKey 直接打包在客户端。

## 8. 真实硬件启动流程

1. 在 OneNET 创建或确认产品、设备、数据流和 MQTT 凭据。
2. 修改嵌入式工程中的设备标识、产品信息、设备密钥和 Wi-Fi/ESP8266 参数。相关代码主要位于 `stm32+esp8266/NET/onenet` 和 `stm32+esp8266/NET/device`。
3. 用 Keil 打开 `stm32+esp8266/stm32f103.uvprojx`，确认目标芯片和下载器配置。
4. 编译并烧录固件；已有 `stm32+esp8266/output/stm32f103.hex` 可作为现成固件参考，但不保证包含你当前的云平台参数。
5. 通过串口工具以 115200 波特率观察启动日志，确认 ESP8266 连接 MQTT 成功、OneNET 设备登录成功并持续上报。
6. 启动已配置 OneNET Pulsar 的后端。
7. 打开前端仪表盘，确认最新数据、设备状态和历史数据均有更新。

嵌入式主循环的行为大致为：约 100 ms 采样一次、约 400 ms 上报一次、约 1 s 打印一次状态日志。自动模式下光照低于 30% 会开启 LED。

## 9. 常见问题

### 页面能打开，但没有数据

先调用 `/api/sensor/upload` 写入测试数据。如果测试数据可见，说明前后端和数据库正常，问题集中在 OneNET/Pulsar 或设备上报链路。

### 后端启动时 Pulsar 连接失败

本地开发先将 `onenet.pulsar.enabled` 设为 `false`。真实接入时检查消费组 ID、密钥、订阅名、Topic 权限和网络连通性。

### 设备在线状态显示未配置或查询失败

检查 `onenet.api.enabled`、`product-id`、`device-name` 和 `access-key`，并确认 AccessKey 格式正确、系统时间准确。

### WebSocket 连接失败

确认后端 8080 已启动；开发环境应通过前端的 `/ws` 代理访问，不要把 `ws://localhost:8080/ws/sensor` 写死到浏览器页面之外的地址。生产部署时需同步配置反向代理的 WebSocket Upgrade。

### 中文显示乱码

项目部分已有源码文件包含编码异常。建议统一将源码和配置文件转换为 UTF-8，并检查编辑器、终端和数据库连接的字符集设置。

## 10. 停止服务

前端窗口按 `Ctrl+C` 停止 Vite；后端窗口按 `Ctrl+C` 停止 Spring Boot；真实设备则断电或通过下载器重新烧录固件。
