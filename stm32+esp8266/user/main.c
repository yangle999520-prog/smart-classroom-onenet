#include "stm32f10x.h"
#include "onenet.h"
#include "esp8266.h"
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "ds18b20.h"
#include "light_sensor.h"
#include <string.h>

#define ESP8266_ONENET_INFO "AT+CIPSTART=\"TCP\",\"studio-mqtt.heclouds.com\",1883\r\n"

/* 上报节奏匹配前端显示间隔 */
#define SAMPLE_INTERVAL       10u    /* 每 10 个循环采集一次传感器（约100ms） */
#define UPLOAD_INTERVAL       40u    /* 每 40 个循环上报一次数据（约400ms，+管道延迟≈0.5s） */
#define LOG_INTERVAL          100u   /* 每 100 个循环打印一次日志（约1s） */

/* 主循环单步延时 */
#define LOOP_DELAY_MS         10u

float temp = 0.0f;
uint16_t light = 0;

static void Hardware_Init(void)
{
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);

	Delay_Init();
	Usart1_Init(115200);
	Usart2_Init(115200);

	LED_Init();
	DS18B20_Init();
	LightSensor_Init();

	UsartPrintf(USART_DEBUG, "Hardware init OK\r\n");
	DelayMs(1000);
}

int main(void)
{
	unsigned short sensorTick = 0;
	unsigned short uploadTick = 0;
	unsigned short logTick = 0;
	unsigned char *dataPtr = NULL;

	Hardware_Init();
	ESP8266_Init();

	UsartPrintf(USART_DEBUG, "Connect MQTTs Server...\r\n");
	while(ESP8266_SendCmd(ESP8266_ONENET_INFO, "CONNECT"))
		DelayXms(500);

	UsartPrintf(USART_DEBUG, "Connect MQTT Server Success\r\n");
	while(OneNet_DevLink())
		DelayXms(500);

	OneNET_Subscribe();

	while(1)
	{
		float newTemp = 0.0f;

		/* Poll cloud messages every loop to keep UART buffer short. */
		dataPtr = ESP8266_GetIPD(0);
		if(dataPtr != NULL)
			OneNet_RevPro(dataPtr);

		/* Sample sensors */
		if(++sensorTick >= SAMPLE_INTERVAL)
		{
			sensorTick = 0;
			light = LightSensor_ReadPercent();
			if(DS18B20_ReadTemperatureNonBlocking(&newTemp))
				temp = newTemp;
		}

		if(ledMode == MODE_AUTO)
		{
			if(light < 30)
			{
				LED_Set(LED_ON);
				ledState = 1;
			}
			else
			{
				LED_Set(LED_OFF);
				ledState = 0;
			}
		}

		/* 最快速度上报（约100ms一次） */
		if(++uploadTick >= UPLOAD_INTERVAL)
		{
			uploadTick = 0;
			UsartPrintf(USART_DEBUG, "OneNet_SendData\r\n");
			OneNet_SendData();
		}

		/* Print status */
		if(++logTick >= LOG_INTERVAL)
		{
			logTick = 0;
			UsartPrintf(USART_DEBUG,
						"mode=%d temp=%.1f light=%d led=%d\r\n",
						ledMode, temp, light, ledState);
		}

		DelayXms(LOOP_DELAY_MS);
	}
}
