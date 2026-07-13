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

		/* Sample sensors about every 100 ms. */
		if(++sensorTick >= 10)
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

		/* Upload about every 5 s. */
		if(++uploadTick >= 300)
		{
			UsartPrintf(USART_DEBUG, "OneNet_SendData\r\n");
			OneNet_SendData();
			uploadTick = 0;
		}

		/* Print status about every 1 s. */
		if(++logTick >= 100)
		{
			logTick = 0;
			UsartPrintf(USART_DEBUG,
						"mode=%d temp=%.1f light=%d led=%d\r\n",
						ledMode, temp, light, ledState);
		}

		DelayXms(10);
	}
}
