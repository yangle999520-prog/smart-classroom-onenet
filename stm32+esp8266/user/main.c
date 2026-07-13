

//单片机头文件
#include "stm32f10x.h"

//网络协议层
#include "onenet.h"

//网络设备
#include "esp8266.h"

//硬件驱动
#include "delay.h"
#include "usart.h"
#include "led.h"
#include "ds18b20.h"
#include "light_sensor.h"

//C库
#include <string.h>

//mqtts.heclouds.com:1883  onenet 的端口号以及地址

#define ESP8266_ONENET_INFO		"AT+CIPSTART=\"TCP\",\"studio-mqtt.heclouds.com\",1883\r\n"   //TCP通讯协议  ，ip地址，端口号

float temp;
uint16_t light;


void Hardware_Init(void)
{
	
	NVIC_PriorityGroupConfig(NVIC_PriorityGroup_2);	//中断控制器分组设置

	Delay_Init();									//systick初始化
	
	Usart1_Init(115200);							//串口1，打印信息用
	
	Usart2_Init(115200);							//串口2，驱动ESP8266用
//	
//	IIC_Init();										//软件IIC初始化
	
	LED_Init();									//蜂鸣器初始化
	
	UsartPrintf(USART_DEBUG, " Hardware init OK\r\n");
//	KEY_Init();
	
		DS18B20_Init();
		LightSensor_Init();
		DelayMs(1000);
}

/*
************************************************************
*	函数名称：	main
*
*	函数功能：	
*
*	入口参数：	无
*
*	返回参数：	0
*
*	说明：		
************************************************************
*/
int main(void)
{
//	
	unsigned short timeCount = 0;	//发送间隔变量
//	unsigned char *dataPtr = NULL;
	
	Hardware_Init();				//初始化外围硬件

	ESP8266_Init();					//初始化ESP8266

	UsartPrintf(USART_DEBUG, "Connect MQTTs Server...\r\n");
	while(ESP8266_SendCmd(ESP8266_ONENET_INFO, "CONNECT"))
		DelayXms(500);
	UsartPrintf(USART_DEBUG, "Connect MQTT Server  Success\r\n");
	while(OneNet_DevLink())			//接入OneNET
		DelayXms(500);

	while(1)
	{
	temp=DS18B20_ReadTemperature();
	light =LightSensor_ReadPercent();
	UsartPrintf(USART_DEBUG,
							"temp=%.1f light=%d\r\n",
							temp,
							light);
		if(light < 30)
{
    LED_Set(LED_ON);
}
else
{
    LED_Set(LED_OFF);
}
		
		if(++timeCount >= 5)									//发送间隔5s
		{
			UsartPrintf(USART_DEBUG, "OneNet_SendData\r\n");
			OneNet_SendData();									//发送数据
			
			timeCount = 0;
			ESP8266_Clear();
		}

        	DelayXms(10);
	}
	
}
