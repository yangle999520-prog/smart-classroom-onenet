

//单片机头文件
#include "stm32f10x.h"

//硬件驱动
#include "led.h"


LED_INFO led_info = {0};



void LED_Init(void)
{

	GPIO_InitTypeDef gpio_initstruct;
	
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);		//打开GPIOB的时钟

	gpio_initstruct.GPIO_Mode = GPIO_Mode_Out_PP;				//设置为输出
	gpio_initstruct.GPIO_Pin = GPIO_Pin_11;						//将初始化的Pin脚
	gpio_initstruct.GPIO_Speed = GPIO_Speed_50MHz;				//可承载的最大频率
	
	GPIO_Init(GPIOA, &gpio_initstruct);							//初始化GPIO
	
	LED_Set(LED_OFF);											

}

void LED_Set(_Bool status)
{
	
	GPIO_WriteBit(GPIOA, GPIO_Pin_11, status == LED_ON ?  Bit_RESET:Bit_SET );		//如果status等于BEEP_ON，则返回Bit_SET，否则返回Bit_RESET
	
	led_info.LED_Status = status;

}
