#ifndef _COMMON_H_
#define _COMMON_H_



void dcamcon_show_dcamerr(HDCAM hdcam, DCAMERR errid, const char* apiname, const char* fmt = 0, ...);

void dcamcon_show_dcamdev_info(HDCAM hdcam);

#endif /* _COMMON_H_ */