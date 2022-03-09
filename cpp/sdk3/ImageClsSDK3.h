#ifndef _IMAGECLSSDK3_H_
#define _IMAGECLSSDK3_H_

#include "atcore.h"

class ImageSDK3 {

private:

public:
	ImageSDK3();
	~ImageSDK3();

	//sanity check
	int temparraysize;

	//Timer
	double timeelapsed1;
	double timeelapsed2;
	double timeelapsed3;
	int framecounter;

	//CriticalAccess to Java Heap
	int enableCriticalAccess = 0; //0-no 1-yes

	int i_retCode; 
	AT_H Hndl; 
	AT_64 iNumberDevices = 0; 

	double iExposureTime;
	double iFrameRate;

	int iAOIHBin;
	int iAOIVBin;
	int iAOIWidth;
	int iAOIHeight;
	int iAOILeft;
	int iAOITop;

	int PixelEncodingIndex;//0= Mono12; 1= Mono12Packed; 2= Mono16
	const wchar_t* iPixelEncoding;
	const wchar_t* iGainMode;
	const wchar_t* iPixelReadoutRate;
	const wchar_t* iElectronicShutteringMode = L"Rolling";
	AT_BOOL iOverlap = 1; // 1 = true; 0 = false
	AT_BOOL iSpuriousNoiseFilter = 0; // 1 = true; 0 = false

	//AuxilliaryOutSource (output timing TTL)
	const wchar_t* iAuxOutSource; //0-FireRow1; 1-FireRowN; 3-FireAll; 4-FireAny

	//control flow
	boolean isStopPressed;

	AT_64 iImageSizeBytes;


	long long ArraySizeSingle;
	short* pImageArraySingle;
	void setArraySizeSingle();
	void InitArraySingle();
	void freeImArraySingle();

	int size_b;
	long long ArraySizeInf;
	void setArraySizeInf();
	void freeImArrayInf();
	short* pImageArrayInf;
	void InitArrayInf();

	int totalFrame;
	int intervalFrame;

	//Buffer
	unsigned char* acqBuffer;
	unsigned char* acqBuffer1;
	unsigned char* acqBuffer2;
	unsigned char* acqBuffer3;
	unsigned char* acqBuffer4;
	unsigned char* acqBuffer5;
	unsigned char* acqBuffer6;
	unsigned char* acqBuffer7;
	unsigned char* acqBuffer8;
	unsigned char* acqBuffer9;
	unsigned char* acqBuffer10;
	unsigned char* acqBuffer11;
	unsigned char* acqBuffer12;
	unsigned char* acqBuffer13;
	unsigned char* acqBuffer14;
	unsigned char* acqBuffer15;
	unsigned char* acqBuffer16;
	unsigned char* acqBuffer17;
	unsigned char* acqBuffer18;
	unsigned char* acqBuffer19;
	unsigned char* acqBuffer20;

	//dummy 
	AT_64 maxval; 
	AT_64 minval; 
	int GMcount;
	AT_WC GMstring[64];
	int GMindex;
	int PRRcount;
	int PRRindex;
	AT_WC PRRstring[64];
	int ShutterModecount;
	int ShutterModeindex;
	AT_WC ShutterModestring[64];
	double minExpSingle;
	double maxExpSingle;
	double minFrameRate;
	double maxFrameRate;
	int AOILayoutindex;
	AT_WC AOILayoutstring[64];
	int TriggerModeindex;
	AT_WC TriggerModestring[64];
	int PixelEncodingindex;
	AT_WC PixelEncodingstring[64];
	int CycleModeindex;
	AT_WC CycleModestring[64];
	AT_BOOL Overlapval;
	AT_64 FrameCountval;
	double realExposure;
	double realFramerate;
	int BitDepthindex;
	AT_WC BitDepthstring[64];
	AT_64 Baselineval;
	double Bytesperpixelval;
	AT_BOOL Spuriousnoisefilterval;
	AT_64 AOIHbinval, AOIVbinval, AOIWidthval, AOILeftval, AOIHeightval, AOITopval;



};








#endif /* _IMAGECLSSDK3_H_ */