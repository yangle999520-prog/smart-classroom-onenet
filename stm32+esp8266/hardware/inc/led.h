#ifndef _LED_H_
#define _LED_H_



typedef struct
{

	_Bool LED_Status;

} LED_INFO;


/* LED工作模式枚举 */
typedef enum
{
	MODE_AUTO = 0,    // 自动模式：根据光敏传感器自动控制
	MODE_MANUAL = 1   // 手动模式：通过OneNET云平台远程控制
} LED_MODE;

#define LED_ON		1

#define LED_OFF		0

extern LED_INFO led_info;

extern LED_MODE ledMode;	// 当前工作模式
extern uint8_t ledState;	// 当前灯光状态（1-开，0-关）


void LED_Init(void);

void LED_Set(_Bool status);


#endif
