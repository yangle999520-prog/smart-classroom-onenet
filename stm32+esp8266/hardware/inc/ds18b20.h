#ifndef __DS18B20_H
#define __DS18B20_H

#include "stm32f10x.h"

#define DS18B20_GPIO_PORT GPIOA
#define DS18B20_GPIO_PIN  GPIO_Pin_1
#define DS18B20_GPIO_CLK  RCC_APB2Periph_GPIOA

void DS18B20_Init(void);
uint8_t DS18B20_StartConvert(void);
float DS18B20_ReadTemperature(void);
uint8_t DS18B20_ReadTemperatureNonBlocking(float *temperature);

#endif
