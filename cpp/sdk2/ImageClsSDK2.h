#ifndef _IMAGECLSSDK2_H_
#define _IMAGECLSSDK2_H_


class ImageSDK2 {

private:

public:
	ImageSDK2();
	~ImageSDK2();

	//sanity check
	int temparraysize;

	//CriticalAccess to Java Heap
	int enableCriticalAccess = 0; //0-no 1-yes

	//Timer
	double timeelapsed1;
	double timeelapsed2;
	double timeelapsed3;

	int CameraModel;

	void setExposureTime(float value); //s
	void setPixelDimParam(int hstart, int hend, int vstart, int vend, int incamerabin);//s
	void setCropModePixelDimParam(int cWidth, int cHeight, int cLeft, int cTop);
	void getnoPixelXY();//s
	void setnoElementArray(); //s
	void InitArraySingle(); //s
	void setSize_b(int size_b); //inf
	void setframeInterval(int frameInterval); //inf
	void setKinetic(float value); //inf
	void InitArrayInf(); //inf
	void InitArrayInfV2(int size);
	bool isPointerNull(); //inf

	//stop mechanism
	void SetIsStopPressed(bool somebol);
	bool isStopPressed;
	bool GetIsStopPressed();

	//zero frame problem
	int discaredZeroFramecounter = 0;
	bool isZeroFrameOccur;
	bool isDiscardFrame;
	int nondiscardedZeroFramecounter = 0;

	int errorValue; 

	int SystemState; // 0 = off; 1 = on;

	//temperature control
	int mintemp, maxtemp;
	int cooler_on;
	int desiredTemp = -80;
	int currTemp;

	// ROI
	int hbin; 
	int vbin; 
	int hstart; //index start at 1
	int hend;
	int vstart; //index start at 1
	int vend;
	int nopixelX_, nopixelY_;

	//Crop Mode 
	int isCropMode;
	int cWidth;
	int cHeight;
	int cLeft;
	int cTop;

	// Setted early when initilizing camera
	int iAD; //index of AD used throughout; get called during camera initialization//0
	int iHspeedFastest; //index HSpeed tied to iAD//0

	// Vertical Shift Speed
	int iVSpeed; // last index by default fastest
	float VSpeed;

	// Vertical Clock Voltage
	int iVAmp; //0 for normal
	float VAmp;

	// Horizontal Speed/Readout Rate
	int iHSpeed; //recommended 0 for the fastest rate
	float HSpeed;

	// Pre-Amp Gains
	int iPreAmpGain; //last index by default largest

	float exposureTime_;
	int acquisitionMode;
	int totalFrame_; // only acquisition mode

	int gain;
	int isFrameTransfer = 1; //1 by default is yes, 0 is no FT
	int baselineClampState = 1; //1 yes, 0 no
	int baselineOffeset = 0;

	//int noElementArray_;
	int arraysize_;
	int size_b_;
	int frameTransferInterval_;
	float kinetic_;

	//for single scan
	long* pImageArray1_;
	short* pImageArrayBuf1_;

	//for inf loop: live video, non-cumualtive/calibration, cumulative
	long* pImageArray_; 
	short* pImageArrayBuf_;

	//for inf loop v2
	long* pImageArrayV2_;
	short* pImageArrayBufV2_;



};

class zeroFrame
{
public:
	int* array; // array contain exact location of frame(zeros)
	int* arraydiscardzero; // discarded zero frame
	int* arrayndzero; // non discarded zero frame
	int counter;
	int counter_nd; // for non discarded frame
	int counter_disc; // for discarded frame
	int sum_pixel; // sum of counts in all pixel of problematic frame (supposedly zero)
	int noframe;

public:
	zeroFrame() {
		counter = 0;
		counter_nd = 0;
		counter_disc = 0;
		array = new int[10];
		arraydiscardzero = new int[10];
		arrayndzero = new int[10];
		fillzero();
	}
	~zeroFrame()
	{
		if (array != NULL)
			delete[] array;
		if (arraydiscardzero != NULL)
			delete[] arraydiscardzero;
		if (arrayndzero != NULL)
			delete[] arrayndzero;

	}

	void resetAll() {
		counter = 0;
		counter_nd = 0;
		counter_disc = 0;
		array = new int[10];
		arraydiscardzero = new int[10];
		arrayndzero = new int[10];
		fillzero();

	}

	void fillzero() {
		for (int i = 0; i < 10; i++) {
			array[i] = 0;
			arrayndzero[i] = 0;
			arraydiscardzero[i] = 0;
		}
	}
	void appendarray(int noframe) {
		array[counter] = noframe;
		counter++;
	}

	void appendarray_nd() {
		arrayndzero[counter_nd] = noframe;
		counter_nd++;

	}

	void appendarray_discarded() {
		arraydiscardzero[counter_disc] = noframe;
		counter_disc++;

	}


	void sumpixelcount(int framesize, long* ptr, int i, int frameInterval, int j) {
		for (int z = 0; z < framesize; z++) {
			sum_pixel += *(ptr + z);
		}

		if (sum_pixel == 0) {
			noframe = (i * frameInterval) + (j + framesize) / framesize;
			appendarray(noframe);
		}
	}

	void reset() {
		sum_pixel = 0;
		noframe = 0;
	}




};







#endif /* _IMAGECLSSDK2_H_ */