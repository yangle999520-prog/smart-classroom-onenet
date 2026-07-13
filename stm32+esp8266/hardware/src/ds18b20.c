
#include "ds18b20.h"
#include "delay.h"

static void DS18B20_PinOutput(void);
static void DS18B20_PinInput(void);
static void DS18B20_WriteBit(uint8_t bit_value);
static uint8_t DS18B20_ReadBit(void);
static void DS18B20_WriteByte(uint8_t data);
static uint8_t DS18B20_ReadByte(void);
static uint8_t DS18B20_ResetPulse(void);
static uint8_t ds18b20_conversion_started = 0;
static uint16_t ds18b20_conversion_ticks = 0;

void DS18B20_Init(void)
{
    RCC_APB2PeriphClockCmd(DS18B20_GPIO_CLK, ENABLE);
    DS18B20_PinOutput();
    GPIO_SetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
}

uint8_t DS18B20_StartConvert(void)
{
    if (!DS18B20_ResetPulse())
    {
        return 0;
    }

    DS18B20_WriteByte(0xCC);
    DS18B20_WriteByte(0x44);
    return 1;
}

float DS18B20_ReadTemperature(void)
{
    uint8_t low_byte;
    uint8_t high_byte;
    short raw_value;

    if (!DS18B20_StartConvert())
    {
        return -1000.0f;
    }

    DelayXms(750);

    if (!DS18B20_ResetPulse())
    {
        return -1000.0f;
    }

    DS18B20_WriteByte(0xCC);
    DS18B20_WriteByte(0xBE);

    low_byte = DS18B20_ReadByte();
    high_byte = DS18B20_ReadByte();
    raw_value = (short)((high_byte << 8) | low_byte);

    return (float)raw_value / 16.0f;
}

uint8_t DS18B20_ReadTemperatureNonBlocking(float *temperature)
{
    uint8_t low_byte;
    uint8_t high_byte;
    short raw_value;

    if (temperature == 0)
    {
        return 0;
    }

    if (!ds18b20_conversion_started)
    {
        if (!DS18B20_StartConvert())
        {
            return 0;
        }

        ds18b20_conversion_started = 1;
        ds18b20_conversion_ticks = 75; /* main loop uses DelayXms(10), 75 ticks ~= 750ms */
        return 0;
    }

    if (ds18b20_conversion_ticks > 0)
    {
        ds18b20_conversion_ticks--;
        return 0;
    }

    if (!DS18B20_ResetPulse())
    {
        ds18b20_conversion_started = 0;
        return 0;
    }

    DS18B20_WriteByte(0xCC);
    DS18B20_WriteByte(0xBE);

    low_byte = DS18B20_ReadByte();
    high_byte = DS18B20_ReadByte();
    raw_value = (short)((high_byte << 8) | low_byte);

    *temperature = (float)raw_value / 16.0f;
    ds18b20_conversion_started = 0;

    return 1;
}

static void DS18B20_PinOutput(void)
{
    GPIO_InitTypeDef gpio_init;

    gpio_init.GPIO_Pin = DS18B20_GPIO_PIN;
    gpio_init.GPIO_Mode = GPIO_Mode_Out_OD;
    gpio_init.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(DS18B20_GPIO_PORT, &gpio_init);
}

static void DS18B20_PinInput(void)
{
    GPIO_InitTypeDef gpio_init;

    gpio_init.GPIO_Pin = DS18B20_GPIO_PIN;
    gpio_init.GPIO_Mode = GPIO_Mode_IN_FLOATING;
    gpio_init.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_Init(DS18B20_GPIO_PORT, &gpio_init);
}

static uint8_t DS18B20_ResetPulse(void)
{
    uint8_t presence;

    DS18B20_PinOutput();
    GPIO_ResetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
    DelayUs(750);
    GPIO_SetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
    DelayUs(15);

    DS18B20_PinInput();
    DelayUs(60);
    presence = GPIO_ReadInputDataBit(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN) == Bit_RESET;
    DelayUs(420);

    return presence;
}

static void DS18B20_WriteBit(uint8_t bit_value)
{
    DS18B20_PinOutput();
    GPIO_ResetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);

    if (bit_value)
    {
        DelayUs(10);
        GPIO_SetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
        DelayUs(55);
    }
    else
    {
        DelayUs(65);
        GPIO_SetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
        DelayUs(5);
    }
}

static uint8_t DS18B20_ReadBit(void)
{
    uint8_t bit_value;

    DS18B20_PinOutput();
    GPIO_ResetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
    DelayUs(3);
    GPIO_SetBits(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
    DS18B20_PinInput();
    DelayUs(10);
    bit_value = GPIO_ReadInputDataBit(DS18B20_GPIO_PORT, DS18B20_GPIO_PIN);
    DelayUs(50);

    return bit_value;
}

static void DS18B20_WriteByte(uint8_t data)
{
    uint8_t i;

    for (i = 0; i < 8; i++)
    {
        DS18B20_WriteBit(data & 0x01);
        data >>= 1;
    }
}

static uint8_t DS18B20_ReadByte(void)
{
    uint8_t i;
    uint8_t value = 0;

    for (i = 0; i < 8; i++)
    {
        value >>= 1;
        if (DS18B20_ReadBit())
        {
            value |= 0x80;
        }
    }

    return value;
}
