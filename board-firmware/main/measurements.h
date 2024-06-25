//
// Created by alec on 6/24/24.
//

#ifndef BOARD_FIRMWARE_MEASUREMENTS_H
#define BOARD_FIRMWARE_MEASUREMENTS_H

#include "MCP3461.h"


#define CH_V_SOURCE_1 0
#define CH_V_GRID 1
#define CH_I_SOURCE_1 2
#define CH_I_LOAD_1 3

#define CH_I_LOAD_2 4
#define CH_I_SOURCE_2 5
#define CH_V_SOURCE_2 6

struct adc_output {
	float grid_voltage;

	float source_voltage[2];

	float source_current[2];
	float load_current[2];
};

//extern float volatile grid_v_rms;
//extern float volatile grid_v_freq;
//extern float volatile grid_v_thd;
//
//extern float volatile inner_v_rms[2][2];
//extern float volatile inner_v_freq[2][2];
//extern float volatile inner_v_thd[2][2];
//
//extern float volatile inner_i_rms[2][2];
//extern float volatile inner_i_freq[2][2];
//extern float volatile inner_i_thd[2][2];

void measurements_loop(void);
void thd_loop(void);

#endif //BOARD_FIRMWARE_MEASUREMENTS_H
