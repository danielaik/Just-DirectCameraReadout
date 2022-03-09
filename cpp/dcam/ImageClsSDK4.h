#ifndef _IMAGECLSSDK4_H_
#define _IMAGECLSSDK4_H_

#include "dcamapi4.h"
#include "dcamprop.h"
#include "common.h"

class ImageSDK4 {
private:

public:
	ImageSDK4();
	~ImageSDK4();

	DCAMERR err;
	DCAMAPI_INIT apiinit;
	DCAMDEV_OPEN devopen;
	HDCAM hdcam;
	DCAMPROP_ATTR basepropattr;
	int32 iDevice = 0; // currenty only works if one hamamatsu camera is connected

	int acqmode_; //1 = single capture; 2 = acquisition; 3 = infinite loop
	double exposureTime_;

	//output trigger
	int outTriggerKind_; //0-Disabled, 1-Programmable, 2-Global
	double outTriggerDelay_;//s
	double outTriggerPeriod_;//s



	//Timer
	double timeelapsed1;
	double timeelapsed2;
	double timeelapsed3;
	int framecounter;


	//sanity check
	int temparraysize;

	//CriticalAccess to Java Heap
	int enableCriticalAccess = 0; //0-no 1-yes

	// ROI
	double hbin; //hbin == vbin. Hamamatsu support equal binning
	double vbin;
	int hstart;// index start at 0
	int hend;
	int vstart;// index start at 0
	int vend;
	int nopixelX_, nopixelY_;
	void setPixelDimParam(int w, int h, int l, int t, int incamerabin);
	void getnoPixelXY();

	int totalframe_;
	int size_b_;
	int arraysize_;
	void setnoElementArray();

	short* pImageArray1_; //short OK// float OK
	void InitArray();
	void freeImArray();
	void reset();

	//control flow
	bool isStopPressed_;

	int setbinning();
	int setsubarray();
	int setexposuretime(double exptime);
	double getframerate();

	//Set Programable OutputTrigger
	void setOutTrigger(int outTriggerKind, double outTrigDelay, double outTrigPeriod);

};

#endif /* _IMAGECLSSDK3_H_ */

