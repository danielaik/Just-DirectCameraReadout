#ifndef _IMAGECLSPVCAM_H_
#define _IMAGECLSPVCAM_H_

// PVCAM
#include <master.h>
#include <pvcam.h>

#include "Common.h"
#include <assert.h>



class ImagePVCam {

private:


public:
	ImagePVCam();
	~ImagePVCam();

	SampleContext dataContext;

	//sanity check
	int temparraysize;

	//CriticalAccess to Java Heap
	int enableCriticalAccess = 0; //0-no 1-yes

	//timer
	double timeelapsed1; //ms // total time elapse from start - end including JNI transfer
	double timeelapsed2; //time set parameter
	double timeelapsed3; //uns16 buffer to float array (average)
	double timeelapsed4; //JNI transfer (average)
	double timeelapsed5; // average transfer + JNI
	
	// ROI
	double hbin; //hbin == vbin. Hamamatsu support equal binning
	double vbin;
	int hstart;// index start at 0
	int hend;
	int vstart;// index start at 0
	int vend;
	int nopixelX_, nopixelY_;

	int acqmode_;
	int totalframe_;
	int size_b_;
	int arraysize_;
	double exposureTime_; // milliseconds
	double frameTime_ = 0; //sec
	uns32 exposureBytes_;
	uns32 oneFrameBytes_;

	//control flow
	bool isStopPressed_;

	// buffer continuous acquisition
	const uns16 circBufferFrames_ = 200;
	const int16 bufferMode_ = CIRC_OVERWRITE;
	int buffSize_; // in pixel

	uns16* pImageCircularBuffer_;
	short* pImageArray_; //uns16*
	short* pImageArrayShort_;
	void InitArray();
	void freeImArray();

	int setAOI();
	void setupSequence();


	// 0 = good to go; 195 = third party software accessing same camera; 186 = no camera detected
	int isCameraConnected();

	// 
	int initSystem();

	//
	int uninitSystem();

	//return chip size in pixels
	int* getchipSize();

	//Getter
	char* getChipName();

	char* getCameraName();

	void setPixelDimParam(int w, int h, int l, int t, int incamerabin);

	void setnoElementArray();

	void getnoPixelXY();

	void reset();





};


#endif /* _IMAGECLSPVCAM_H_ */
