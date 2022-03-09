#include <jni.h>
#include <iostream>
#include <vector>
#include "directCameraReadout_andorsdk2v3_AndorSDK2v3.h"
extern "C" {
#include "ATMCD32D.h"
}
#include <exception>
#include <codecvt>
#include "ImageClsSDK2.h"
#include <assert.h>
#include <string>
#include <chrono>
#include "TimeKeeper.h"




ImageSDK2 imgobj;
zeroFrame zeroFrameobj;
TimeKeeper tk1;
TimeKeeper tk2;
TimeKeeper tk3;

/*
Prototype method
*/
void resetImageClsparam();
void copyLongArrayToShortArray(int size);
void copyToJavaV2(JNIEnv* env, jshortArray outarr, int new_c, int size, int frameSize, int transferFrame);
int getADnumber();
void setSettingsParameterA();
void setSettingsParameterB(); //Set EMgain, ADnumber, Vertical Shift Speed (usecs), Vertical Clock Amplitude, Horizontal Speed/Readout (Mhz), Pre-Amp Gains


/*
//Method definition
*/

void resetImageClsparam() { //TODO: impement this
	imgobj.isStopPressed = false;
	imgobj.discaredZeroFramecounter = 0;
	imgobj.isZeroFrameOccur = false;
	imgobj.isDiscardFrame = false;
	imgobj.nondiscardedZeroFramecounter = 0;
}

void copyLongArrayToShortArray(int size) {
	if (imgobj.pImageArray_ && imgobj.pImageArrayBuf_) {
		for (int j = 0; j < size; j++) {
			*(imgobj.pImageArrayBuf_ + j) = *(imgobj.pImageArray_ + j);
		}
	}
}
void copyToJavaV2(JNIEnv* env, jshortArray outarr, int new_c, int size, int frameSize, int transferFrame) {
	if (!imgobj.isPointerNull()) {
		if (new_c + transferFrame <= imgobj.size_b_) { // more than enough space in 1D array
			env->SetShortArrayRegion(outarr, (new_c * frameSize), size, imgobj.pImageArrayBuf_);
		}
		else {
			int sizeFirstSection = (new_c + transferFrame - imgobj.size_b_) * frameSize;
			int sizeSeconSection = (transferFrame - sizeFirstSection) * frameSize;
			env->SetShortArrayRegion(outarr, (new_c * frameSize), sizeFirstSection, imgobj.pImageArrayBuf_);
			env->SetShortArrayRegion(outarr, 0, sizeSeconSection, imgobj.pImageArrayBuf_ + (sizeFirstSection));
		}
	}
		
}
int getADnumber() {
	int numberAllowedHSpeed;
	int nAD;
	int iAD;
	int iSpeed;
	float hspeed;
	float STemp = 0;
	int HSnumber;
	int ADnumber;
	GetNumberADChannels(&nAD);

	for (iAD = 0; iAD < nAD; iAD++) {
		GetNumberHSSpeeds(iAD, 0, &numberAllowedHSpeed);

		for (iSpeed = 0; iSpeed < numberAllowedHSpeed; iSpeed++) {

			GetHSSpeed(iAD, 0, iSpeed, &hspeed);
			if (hspeed > STemp) {
				STemp = hspeed;
				HSnumber = iSpeed;
				ADnumber = iAD;

			}
		}
	}

	imgobj.iHspeedFastest = HSnumber;
	imgobj.iAD = ADnumber;
	return ADnumber; //return index of ADchannel
}
void setSettingsParameterA() {
	
	if (imgobj.isCropMode == 1) {
		assert((imgobj.acquisitionMode != 1) && "error 393 does not run single capture mode under Crop Mode");
		//crop mode
		imgobj.errorValue = SetReadMode(4);// 0 Full Vertical Binning   1 Multi-Track   2 Random-Track   3 Single-Track   4 Image
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 21 SetReadMode");
		imgobj.errorValue = SetAcquisitionMode(imgobj.acquisitionMode); // 1 - single scan; 2 - accumulate; 3 - kinetics; 4 - fast kinetics; 5 - run till abort;
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 22 SetAcquisitionMode");
		imgobj.errorValue = SetFrameTransferMode(imgobj.isFrameTransfer);
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 23 SetFrameTransferMode");
		if (imgobj.CameraModel == 860) {
			imgobj.errorValue = SetIsolatedCropMode(imgobj.isCropMode, imgobj.cHeight, imgobj.cWidth, imgobj.vbin, imgobj.hbin); //only centralized
			if (imgobj.errorValue != DRV_SUCCESS) {
				std::cout << "error SetIsolatedCropMode: " << imgobj.errorValue << std::endl;
			}

			assert((imgobj.errorValue == DRV_SUCCESS) && "error 24 SetIsolatedCropModeEx");
		}
		else {
			imgobj.errorValue = SetIsolatedCropModeEx(imgobj.isCropMode, imgobj.cHeight, imgobj.cWidth, imgobj.vbin, imgobj.hbin, imgobj.cLeft, imgobj.cTop); //variable position
			if (imgobj.errorValue != DRV_SUCCESS) {
				std::cout << "error SetIsolatedCropModeEx: " << imgobj.errorValue << std::endl;
			}

			assert((imgobj.errorValue == DRV_SUCCESS) && "error 24 SetIsolatedCropModeEx");
		}
	}
	else {
		if (imgobj.CameraModel == 860) {
			imgobj.errorValue = SetIsolatedCropMode(imgobj.isCropMode, imgobj.cHeight, imgobj.cWidth, imgobj.vbin, imgobj.hbin); //only centralized
			assert((imgobj.errorValue == DRV_SUCCESS) && "error 24 turning off crop mode SetIsolatedCropModeEx");
		}
		else {
			imgobj.errorValue = SetIsolatedCropModeEx(imgobj.isCropMode, imgobj.cHeight, imgobj.cWidth, imgobj.vbin, imgobj.hbin, imgobj.cLeft, imgobj.cTop); //variable position
			assert((imgobj.errorValue == DRV_SUCCESS) && "error 24 turning off crop modeSetIsolatedCropModeEx");
		}
		imgobj.errorValue = SetReadMode(4);// 0 Full Vertical Binning   1 Multi-Track   2 Random-Track   3 Single-Track   4 Image
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 21 SetReadMode");
		imgobj.errorValue = SetAcquisitionMode(imgobj.acquisitionMode); // 1 - single scan; 2 - accumulate; 3 - kinetics; 4 - fast kinetics; 5 - run till abort;
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 22 SetAcquisitionMode");
		imgobj.errorValue = SetFrameTransferMode(imgobj.isFrameTransfer);
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 23 SetFrameTransferMode");
		imgobj.errorValue = SetImage(imgobj.hbin, imgobj.vbin, imgobj.hstart, imgobj.hend, imgobj.vstart, imgobj.vend);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "error SetImage: " << imgobj.errorValue << std::endl;
		}
	
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 26 SetImage");
	}
}
void setSettingsParameterB() {
	imgobj.errorValue = SetADChannel(imgobj.iAD);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 11 SetADChannel");
	imgobj.errorValue = SetVSSpeed(imgobj.iVSpeed);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 12 SetVSSpeed");
	imgobj.errorValue = SetVSAmplitude(imgobj.iVAmp);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 13 SetVSAmplitude");
	imgobj.errorValue = SetHSSpeed(0, imgobj.iHSpeed);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 14 SetHSSpeed");
	imgobj.errorValue = SetPreAmpGain(imgobj.iPreAmpGain);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 15 SetPreAmpGain");
	imgobj.errorValue = SetEMCCDGain(imgobj.gain);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 16 SetEMCCDGainn");
}

// InitializeEMCCD will be called when application open: InitSystem, turn on cooler, set shutter open
JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_InitializeEMCCDSDK2(JNIEnv* env, jclass cls) {

	char 				aBuffer[MAX_PATH + 1];
	BOOL				errorFlag;

	GetCurrentDirectoryA(sizeof(aBuffer), aBuffer); // **** win32 specific ****
	std::cout << aBuffer << "\n";
	imgobj.errorValue = Initialize(aBuffer);  // Initialize driver in current directory
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Initialize Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 1 Initialize buffer");

	imgobj.errorValue = CoolerON();
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Turning cooler on Error: " << imgobj.errorValue << std::endl;
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 3 Turning on cooler");

	imgobj.errorValue = IsCoolerOn(&imgobj.cooler_on);
	if (imgobj.cooler_on != 1) {
		std::cout << "Cooler not on: " << imgobj.errorValue << std::endl;
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 4 Cooler is off");


	int ttl = 1;
	int shutter = 1; //0 auto, 1 open permanent
	int openclose = 1;
	// Set shutter
	imgobj.errorValue = SetShutter(ttl, shutter, openclose, openclose);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set shutter error: " << imgobj.errorValue << std::endl;
	}

	imgobj.iAD = getADnumber();


	imgobj.SystemState = 1;

	return true;

}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_SetCoolingSDK2(JNIEnv* env, jclass cls, jint iscool) {
	imgobj.cooler_on = iscool;
	if (iscool == 1) {
		imgobj.errorValue = CoolerON();
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 31 Turning on cooler");
	}
	else {
		imgobj.errorValue = CoolerOFF();        // Switch off cooler (if used)
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 51 Turning cooler off");
	}
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_SetTemperatureSDK2(JNIEnv* env, jclass cls, jint desiredtemp) {
	imgobj.desiredTemp = desiredtemp;
	imgobj.errorValue = SetTemperature(imgobj.desiredTemp);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set temperature error: " << imgobj.errorValue << std::endl;
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 8 SetTemperature()");
}

JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetTemperatureAndStatusSDK2(JNIEnv* env, jclass cls) {
	// return temp status and detector current temp
	//20037 temp not reached //20035 temp not stabilized //20034 temp off //20036 temp stabilized
	int tempstatus = GetTemperature(&imgobj.currTemp);
	jint outCArray[] = { imgobj.currTemp, tempstatus };
	jintArray outArray = env->NewIntArray(2);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 2, outCArray);
	return outArray;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_SetFanModeSDK2(JNIEnv* env, jclass cls, jint iFan) {

	imgobj.errorValue = SetFanMode(iFan);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 9 SetFanMode");
	if (imgobj.errorValue != DRV_SUCCESS) {
		return imgobj.errorValue;
	}
	else {
		return 0;
	}

}

// System Shut down
JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_SystemShutDownSDK2(JNIEnv* env, jclass cls) {

	imgobj.SystemState = 0;

	imgobj.errorValue = CoolerOFF();        // Switch off cooler (if used)
	if (imgobj.errorValue != DRV_SUCCESS)
		std::cout << "Error switching cooler off: " << imgobj.errorValue << std::endl;
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 5 Turning cooler off");

	// close shutter shutter
	imgobj.errorValue = SetShutter(1, 2, 1, 1);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set shutter error: " << imgobj.errorValue << std::endl;
	}

	imgobj.errorValue = ShutDown();
	if (imgobj.errorValue != DRV_SUCCESS)
		std::cout << "Error shutting down: " << imgobj.errorValue << std::endl;
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 6 ShutDown()");

}

// API shutdown
JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_ShutDownSDK2(JNIEnv*, jclass) {
	imgobj.errorValue = ShutDown();
	if (imgobj.errorValue != DRV_SUCCESS)
		std::cout << "Error ShutDown()" << std::endl;
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 35 ShutDown()");
	return;
}

JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_seyHello(JNIEnv* env, jclass cls, jint n1, jint n2) {
	jfloat result;
	result = ((jfloat)n1 + (jfloat)n2) / 2;
	return result;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_sayHello(JNIEnv* env, jobject thisObj) {
	return 1;
}

//Start acquisition
JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_DoCameraAcquisitionSDK2(JNIEnv* env, jclass cls) {
	// start acquisition
	imgobj.errorValue = StartAcquisition();
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Start acquisition error\n";
		AbortAcquisition();
	}
	return;
}


//Premature Termination
JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_setStopMechanismSDK2(JNIEnv* env, jclass cls, jboolean isStopCalled) {
	imgobj.isStopPressed = isStopCalled;
	return;
}

//Check camera
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_isEMCCDconnectedSDK2(JNIEnv* env, jclass cls) {
	char 				aBuffer[MAX_PATH + 1];
	BOOL				errorFlag;

	GetCurrentDirectoryA(sizeof(aBuffer), aBuffer); // **** win32 specific ****

	imgobj.errorValue = Initialize(aBuffer);  // Initialize driver in current directory

	if (imgobj.errorValue != DRV_SUCCESS) {
		/*assert((imgobj.errorValue != DRV_ERROR_NOCAMERA) && "No EMCCD found");
		assert((imgobj.errorValue != DRV_VXDNOTINSTALLED) && "VxD not loaded");
		assert((imgobj.errorValue != DRV_INIERROR) && "Unable to load 'Detector.INI");
		assert((imgobj.errorValue != DRV_COFERROR) && "Unable to load COF");
		assert((imgobj.errorValue != DRV_FLEXERROR) && "Unable to load RBF");
		assert((imgobj.errorValue != DRV_ERROR_ACK) && "Unable to communicate with card");
		assert((imgobj.errorValue != DRV_ERROR_FILELOAD) && "Unable to load COF and RBF files");
		assert((imgobj.errorValue != DRV_ERROR_PAGELOCK) && "Unable to acquire lock on requested memory");
		assert((imgobj.errorValue != DRV_USBERROR) && "Unable to detect USB device or not USB2.0");*/

		//return false;
	}

	/* I call ShutDown() directly from java instead, this is to allow copying serial number and camera series
	// attempting to shutdown everytime we check if iXon860 is available
	if (imgobj.errorValue == DRV_SUCCESS) {
		imgobj.errorValue = ShutDown();
		if (imgobj.errorValue != DRV_SUCCESS)
			std::cout << "Error shutting down" << "Error" << "\n";
	}
	*/

	return imgobj.errorValue;
}

//Single Scan
JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_setParameterSingleSDK2(JNIEnv* env, jclass cls, jfloat exposuretime, jint width, jint height, jint left, jint top, jint acqmode, jint gain, jint incamerabin, jint cammodel, jint iVspeed, jint iVamp, jint iHspeed, jint iPreAmp, jint isCropMode, jint cWidth, jint cHeight, jint cLeft, jint cTop) {

	imgobj.CameraModel = cammodel;
	imgobj.isCropMode = isCropMode;
	imgobj.setExposureTime(exposuretime);
	imgobj.acquisitionMode = acqmode;
	imgobj.setPixelDimParam(width, height, left, top, incamerabin);
	imgobj.setCropModePixelDimParam(cWidth, cHeight, cLeft, cTop);
	imgobj.getnoPixelXY();
	imgobj.gain = gain;
	imgobj.iVSpeed = iVspeed;
	imgobj.iVAmp = iVamp;
	imgobj.iHSpeed = iHspeed;
	imgobj.iPreAmpGain = iPreAmp;

	



	//std::cout << "cpp SetParameter hbin: " << imgobj.hbin << ", vbin: " << imgobj.vbin << ", hstart: " << imgobj.hstart << ", hend: " << imgobj.hend << ", vstart: " << imgobj.vstart << ", vend: " << imgobj.vend << ", nopixelX: " << imgobj.nopixelX_ << ", nopixelY: " << imgobj.nopixelY_ << std::endl;

	setSettingsParameterA();

	setSettingsParameterB(); // set ADchannel, Vertical shift speed, Vertical amp, Horizontal speed, Preamp, emgain

	// Initialize array size
	imgobj.setnoElementArray();

	// initialize array memory allocation
	imgobj.InitArraySingle();

	// set exposure time
	imgobj.errorValue = SetExposureTime(imgobj.exposureTime_);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set exposure time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 67 SetExposureTime");

	// set trigger mode
	int trigger_mode = 0; // 0 - internal; 1 - external; 6 - external start; 7 - external exposure (bulb); 9 - external FVB EM; 10 - software trigger; 12 - external charge shifting;
	imgobj.errorValue = SetTriggerMode(trigger_mode);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set trigger mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 68 SetTriggerMode");

	float exposure, accumulate, kinetic;
	imgobj.errorValue = GetAcquisitionTimings(&exposure, &accumulate, &kinetic);
	imgobj.setKinetic(kinetic);
	imgobj.exposureTime_ = exposure;
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 69 GetAcquisitionTimings");


	//Only needed for kinetic capture
	imgobj.errorValue = SetBaselineClamp(imgobj.baselineClampState);
	assert((imgobj.errorValue) && "error 70 SetBaselineClamp");

	/*
	// Setting of baseline offset
	imgobj.errorValue = SetBaselineOffset(imgobj.baselineOffeset);
	assert((imgobj.errorValue) && "error 39; SetBaselineOffset");
	*/

	return;
}

JNIEXPORT jshortArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_runSingleScanSDK2(JNIEnv* env, jclass cls) {

	

	/*
	// Wait for 2 seconds to allow MCD to calibrate fully before allowing an
	// acquisition to begin
	int 				test, test2;

	test = GetTickCount();
	do {
		test2 = GetTickCount() - test;
	} while (test2 < 2000);
	*/

	int status;
	imgobj.errorValue = GetStatus(&status);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	while (status != DRV_IDLE) {
		imgobj.errorValue = GetStatus(&status);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "Get acquisition timings Error\n";
			std::cout << "Error: " << imgobj.errorValue << "\n";
		}
	}

	// start acquisition
	imgobj.errorValue = StartAcquisition();
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Start acquisition error\n";
		AbortAcquisition();
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 45 SingleScan StartAcquisition()");

	// make sure camera is in idle state
	imgobj.errorValue = GetStatus(&status);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition status Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	while (status != DRV_IDLE) {
		imgobj.errorValue = GetStatus(&status);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "Get acquisition timings Error\n";
			std::cout << "Error: " << imgobj.errorValue << "\n";
		}
	}

	// copy counts from buffer to 1Darray
	imgobj.errorValue = GetAcquiredData(imgobj.pImageArray1_, imgobj.arraysize_);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition data Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 46 SingleScan GetAcquiredData()");

	// copy long 1Darray to float 1Darray
	for (int i = 0; i < imgobj.arraysize_; i++) {
		*(imgobj.pImageArrayBuf1_ + i) = *(imgobj.pImageArray1_ + i);
	}

	jshortArray outArray;
	outArray = env->NewShortArray(imgobj.arraysize_);
	env->SetShortArrayRegion(outArray, 0, imgobj.arraysize_, imgobj.pImageArrayBuf1_);
	free(imgobj.pImageArrayBuf1_);
	free(imgobj.pImageArray1_);
	imgobj.pImageArrayBuf1_ = NULL;
	imgobj.pImageArray1_ = NULL;

	return outArray;
}

// Infinite Loop: Live video and non-cumulative/calibration mode
JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_setParameterInfiniteLoopSDK2(JNIEnv* env, jclass cls, jint size_b, jint transferFrameInterval, jfloat exposureTime, jint width, jint height, jint left, jint top, jint acqmode, jint gain, jint incamerabin, jint cammodel, jint iVspeed, jint iVamp, jint iHspeed, jint iPreAmp, jint isCropMode, jint cWidth, jint cHeight, jint cLeft, jint cTop, jint arraysize) {

	imgobj.CameraModel = cammodel;
	imgobj.isCropMode = isCropMode;
	imgobj.setExposureTime(exposureTime);
	imgobj.acquisitionMode = acqmode;
	imgobj.setPixelDimParam(width, height, left, top, incamerabin);
	imgobj.setCropModePixelDimParam(cWidth, cHeight, cLeft, cTop);
	imgobj.getnoPixelXY();
	imgobj.setSize_b(arraysize);
	imgobj.setframeInterval(transferFrameInterval);
	imgobj.gain = gain;
	imgobj.iVSpeed = iVspeed;
	imgobj.iVAmp = iVamp;
	imgobj.iHSpeed = iHspeed;
	imgobj.iPreAmpGain = iPreAmp;

	//std::cout << "cpp SetParameter hbin: " << imgobj.hbin << ", vbin: " << imgobj.vbin << ", hstart: " << imgobj.hstart << ", hend: " << imgobj.hend << ", vstart: " << imgobj.vstart << ", vend: " << imgobj.vend << ", nopixelX: " << imgobj.nopixelX_ << ", nopixelY: " << imgobj.nopixelY_ << std::endl;

	//reset zero frame obj
	zeroFrameobj.resetAll();

	assert(((imgobj.size_b_ % imgobj.frameTransferInterval_) == 0) && "error 26; total frame is not a multiple of frame inteval");
	

	/*
	// Get camera capabilities
	AndorCapabilities caps;
	caps.ulSize = sizeof(AndorCapabilities);
	errorValue = GetCapabilities(&caps);
	if (errorValue != DRV_SUCCESS) {
	std::cout << "Get Andor Capabilities information Error\n";
	std::cout << "Error: " << errorValue << "\n";
	}
	*/

	setSettingsParameterA();

	setSettingsParameterB(); // set ADchannel, Vertical shift speed, Vertical amp, Horizontal speed, Preamp, emgain

	/*
	int noScans = 2;
	if (acquisitionMode == 3) {

		// set number of frames for kinetic modes
		errorValue = SetNumberKinetics(noScans);
		std::cout << "number frames: " << noScans << "\n";
		if (errorValue != DRV_SUCCESS) {

			std::cout << "Set number kinetics error \n";
		}

	}
	*/

	// set exposure time
	imgobj.errorValue = SetExposureTime(imgobj.exposureTime_);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set exposure time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 67 SetExposureTime");

	// set trigger mode
	int trigger_mode = 0; // 0 - internal; 1 - external; 6 - external start; 7 - external exposure (bulb); 9 - external FVB EM; 10 - software trigger; 12 - external charge shifting;
	imgobj.errorValue = SetTriggerMode(trigger_mode);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set trigger mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 68 SetTriggerMode");

	float exposure, accumulate, kinetic;
	imgobj.errorValue = GetAcquisitionTimings(&exposure, &accumulate, &kinetic);
	imgobj.setKinetic(kinetic);
	imgobj.exposureTime_ = exposure;
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 69 GetAcquisitionTimings");


	//Only needed for kinetic capture
	imgobj.errorValue = SetBaselineClamp(imgobj.baselineClampState);
	assert((imgobj.errorValue) && "error 70 SetBaselineClamp");

	/*
	// set kinetic cycle time
	float cycleTime = 0.0f;
	imgobj.errorValue = SetKineticCycleTime(cycleTime);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set kinetic cycle time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	*/
	
	/*
	// Setting of baseline offset
	imgobj.errorValue = SetBaselineOffset(imgobj.baselineOffeset);
	assert((imgobj.errorValue) && "error 39; SetBaselineOffset");
	*/
	
    /*
	// return maximum number of images circular buffer can store based on current acquisiton setting
	long buffer;
	imgobj.errorValue = GetSizeOfCircularBuffer(&buffer);
	assert((imgobj.errorValue) && "error 41; GetSizeOfCircularBuffer");
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get size of circular buffer Error \n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	else {
		std::cout << "Number of images circular buffer can hold: " << buffer << std::endl;
	}
	*/

	// Initialize array size
	imgobj.setnoElementArray();

	// initialize array memory allocation
	imgobj.InitArrayInf();

	/*
	// Wait for 2 seconds to allow MCD to calibrate fully before allowing an
	// acquisition to begin
	int 				test, test2;

	test = GetTickCount();
	do {
		test2 = GetTickCount() - test;
	} while (test2 < 2000);
	*/
	
	// make sure camera is in idle state
	int status;
	imgobj.errorValue = GetStatus(&status);
	assert((imgobj.errorValue) && "error 42; GetStatus");
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	while (status != DRV_IDLE) {
		imgobj.errorValue = GetStatus(&status);
		assert((imgobj.errorValue) && "error 43; GetStatus");
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "Get acquisition timings Error\n";
			std::cout << "Error: " << imgobj.errorValue << "\n";
		}
	}

	return true;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_runInfiniteLoopSDK2(JNIEnv* env, jclass cls, jshortArray outArray, jobject framecounterObj) {

	assert((env->GetArrayLength(outArray) == imgobj.temparraysize) && "1Darraysize mismatch");

	static jclass framecounter_class = env->FindClass("directCameraReadout/control/FrameCounter"); //method caching
	static jmethodID increment_id = env->GetMethodID(framecounter_class, "increment", "()V");  //cached
	static jfieldID time1_id = env->GetFieldID(framecounter_class, "time1", "D");
	static jfieldID time2_id = env->GetFieldID(framecounter_class, "time2", "D");
	static jfieldID time3_id = env->GetFieldID(framecounter_class, "time3", "D");

	jboolean isCopy{ false };
	jshort* carray = NULL;
	if (imgobj.enableCriticalAccess == 1) {
		carray = (jshort*)env->GetPrimitiveArrayCritical(outArray, &isCopy);
		if (isCopy == true) {
			env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
		}
	}
	tk1.setTimeStart();

	resetImageClsparam();
	imgobj.SetIsStopPressed(false); //reset
	//TODO reset zeroframe counter

	int c = 0;
	while (!imgobj.GetIsStopPressed()) {////c != 100 Tmporary stop after total frame = 99+1 * frameinterval
		// Stop this thread when stop button (Java GUI) is pressed
		if (imgobj.GetIsStopPressed() == true) {
			break;
		}

		long totalImAcq;
		do {
			imgobj.errorValue = GetTotalNumberImagesAcquired(&totalImAcq);
		} while (totalImAcq < (c + 1) * imgobj.frameTransferInterval_ + imgobj.discaredZeroFramecounter);
	
		// Get first and last unretrieved image index from buffer
		long first, last, lastTRUE;
		int numberFrames;
		imgobj.errorValue = GetNumberNewImages(&first, &last);

		lastTRUE = first + imgobj.frameTransferInterval_ - 1; // TODO: confirmed correct allocation by passing -1 into element in pImageArray
		numberFrames = imgobj.frameTransferInterval_;

		// Get images and copy to buffer
		long validfirst;
		long validlast;
		unsigned long sizee;
		sizee = imgobj.nopixelX_ * imgobj.nopixelY_ * numberFrames;

		/*
		// The following is necassary to discard first frames. Fix absurd number appearing on first frames of dimension larger than 64 x 64 pixels. UNUSED.
		if (i == 0) {
			first = 2;
			lastTRUE = 2 + imgobj.acquisitionFrameInterval_ - 1;
		}
		*/

		// The following is to remove frames of zero
		long& first_ref = first;
		long& lastTRUE_ref = lastTRUE;

		// zero frame
		if (imgobj.isZeroFrameOccur) {
			first_ref = c * imgobj.frameTransferInterval_ + 1 + imgobj.discaredZeroFramecounter;
			lastTRUE_ref = first + imgobj.frameTransferInterval_ - 1;
		}

		//new index
		int new_c = c % (imgobj.size_b_ / imgobj.frameTransferInterval_);
		// Andor get image buffer to long array
		imgobj.errorValue = GetImages(first, lastTRUE, imgobj.pImageArray_, sizee, &validfirst, &validlast); 
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "error 55 getimages: " << imgobj.errorValue << std::endl;
		}
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 55; GetImages error(camera buffer to cpp)");
		

		// reset
		imgobj.isZeroFrameOccur = false;

		tk2.setTimeStart();
		if (isCopy == true || imgobj.enableCriticalAccess == 0) {// copy elem of long array into double or short array
			/*// for loop is slower; us estd::copy
		if (imgobj.pImageArray_ && imgobj.pImageArrayBuf_) {
			for (int j = 0; j < sizee; j++) {
				*(imgobj.pImageArrayBuf_ + j) = *(imgobj.pImageArray_ + j);
			}
		}
		*/

			tk3.setTimeStart();
			std::copy(imgobj.pImageArray_, imgobj.pImageArray_ + sizee, imgobj.pImageArrayBuf_); //find out if faster
			if (!imgobj.isPointerNull()) {
				env->SetShortArrayRegion(outArray, (new_c * sizee), sizee, imgobj.pImageArrayBuf_);
			}
			env->CallVoidMethod(framecounterObj, increment_id);
			imgobj.timeelapsed3 += tk3.getTimeElapsed();
		}
		else {
			//slow 0.1ms overhead 40ms 128x128
			std::copy(imgobj.pImageArray_, imgobj.pImageArray_ + sizee, imgobj.pImageArrayBuf_); //find out if faster
			std::copy(imgobj.pImageArrayBuf_, imgobj.pImageArrayBuf_ + sizee, carray + (new_c * sizee));
			env->CallVoidMethod(framecounterObj, increment_id);
		}
		imgobj.timeelapsed2 += tk2.getTimeElapsed();


		c++;
	}

	//clean up resources and abort acquisition
	imgobj.errorValue = AbortAcquisition();
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 66 Abort Acquisition");
	if (imgobj.acquisitionMode == 5) {
		free(imgobj.pImageArray_);
		free(imgobj.pImageArrayBuf_);
		imgobj.pImageArray_ = NULL;
		imgobj.pImageArrayBuf_ = NULL;
	}

	imgobj.timeelapsed1 = tk1.getTimeElapsed(); //full
	imgobj.timeelapsed2 /= c; // average read buffer and copy
	imgobj.timeelapsed3 /= c;//native heap to java heap

	env->SetDoubleField(framecounterObj, time1_id, imgobj.timeelapsed1);
	env->SetDoubleField(framecounterObj, time2_id, imgobj.timeelapsed2);
	env->SetDoubleField(framecounterObj, time3_id, imgobj.timeelapsed3);

	if (isCopy == false && imgobj.enableCriticalAccess == 1) {
		env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
	}

	return;
}

//Acqusition mode
JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_setParameterContinuousAcquisitionSDK2(JNIEnv* env, jclass cls, jint size_b, jint totalFrame, jint transferFrameInterval, jfloat exposureTimeCont, jint width, jint height, jint left, jint top, jint acqmode, jint gain, jint incamerabin, jint cammodel, jint iVspeed, jint iVamp, jint iHspeed, jint iPreAmp, jint isCropMode, jint cWidth, jint cHeight, jint cLeft, jint cTop, jint arraysize) {

	imgobj.CameraModel = cammodel;
	imgobj.isCropMode = isCropMode;
	imgobj.setExposureTime(exposureTimeCont);
	imgobj.acquisitionMode = acqmode;
	imgobj.setPixelDimParam(width, height, left, top, incamerabin);
	imgobj.setCropModePixelDimParam(cWidth, cHeight, cLeft, cTop);
	imgobj.getnoPixelXY();
	imgobj.setSize_b(arraysize);
	imgobj.setframeInterval(transferFrameInterval);
	imgobj.gain = gain;
	imgobj.iVSpeed = iVspeed;
	imgobj.iVAmp = iVamp;
	imgobj.iHSpeed = iHspeed;
	imgobj.iPreAmpGain = iPreAmp;
	imgobj.totalFrame_ = totalFrame;


	//std::cout << "cpp SetParameter hbin: " << imgobj.hbin << ", vbin: " << imgobj.vbin << ", hstart: " << imgobj.hstart << ", hend: " << imgobj.hend << ", vstart: " << imgobj.vstart << ", vend: " << imgobj.vend << ", nopixelX: " << imgobj.nopixelX_ << ", nopixelY: " << imgobj.nopixelY_ << std::endl;

	//reset zero frame obj
	zeroFrameobj.resetAll();

	assert(((imgobj.size_b_ % imgobj.frameTransferInterval_) == 0) && "error 26; total frame is not a multiple of frame inteval");

	/*
	// Get camera capabilities
	AndorCapabilities caps;
	caps.ulSize = sizeof(AndorCapabilities);
	errorValue = GetCapabilities(&caps);
	if (errorValue != DRV_SUCCESS) {
	std::cout << "Get Andor Capabilities information Error\n";
	std::cout << "Error: " << errorValue << "\n";
	}
	*/

	setSettingsParameterA();

	setSettingsParameterB(); // set ADchannel, Vertical shift speed, Vertical amp, Horizontal speed, Preamp, emgain

	/*
	int noScans = 2;
	if (acquisitionMode == 3) {

		// set number of frames for kinetic modes
		errorValue = SetNumberKinetics(noScans);
		std::cout << "number frames: " << noScans << "\n";
		if (errorValue != DRV_SUCCESS) {

			std::cout << "Set number kinetics error \n";
		}

	}
	*/

	// set exposure time
	imgobj.errorValue = SetExposureTime(imgobj.exposureTime_);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set exposure time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 67 SetExposureTime");

	// set trigger mode
	int trigger_mode = 0; // 0 - internal; 1 - external; 6 - external start; 7 - external exposure (bulb); 9 - external FVB EM; 10 - software trigger; 12 - external charge shifting;
	imgobj.errorValue = SetTriggerMode(trigger_mode);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set trigger mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 68 SetTriggerMode");

	float exposure, accumulate, kinetic;
	imgobj.errorValue = GetAcquisitionTimings(&exposure, &accumulate, &kinetic);
	imgobj.setKinetic(kinetic);
	imgobj.exposureTime_ = exposure;
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 69 GetAcquisitionTimings");


	//Only needed for kinetic capture
	imgobj.errorValue = SetBaselineClamp(imgobj.baselineClampState);
	assert((imgobj.errorValue) && "error 70 SetBaselineClamp");

	/*
	// set kinetic cycle time
	float cycleTime = 0.0f;
	imgobj.errorValue = SetKineticCycleTime(cycleTime);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set kinetic cycle time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	*/

	/*
	// Setting of baseline offset
	imgobj.errorValue = SetBaselineOffset(imgobj.baselineOffeset);
	assert((imgobj.errorValue) && "error 39; SetBaselineOffset");
	*/

	/*
	// return maximum number of images circular buffer can store based on current acquisiton setting
	long buffer;
	imgobj.errorValue = GetSizeOfCircularBuffer(&buffer);
	assert((imgobj.errorValue) && "error 41; GetSizeOfCircularBuffer");
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get size of circular buffer Error \n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	else {
		std::cout << "Number of images circular buffer can hold: " << buffer << std::endl;
	}
	*/

	// Initialize array size
	imgobj.setnoElementArray();

	// initialize array memory allocation
	imgobj.InitArrayInf();

	/*
	// Wait for 2 seconds to allow MCD to calibrate fully before allowing an
	// acquisition to begin
	int 				test, test2;

	test = GetTickCount();
	do {
		test2 = GetTickCount() - test;
	} while (test2 < 2000);
	*/

	// make sure camera is in idle state
	int status;
	imgobj.errorValue = GetStatus(&status);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	while (status != DRV_IDLE) {
		imgobj.errorValue = GetStatus(&status);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "Get acquisition timings Error\n";
			std::cout << "Error: " << imgobj.errorValue << "\n";
		}
	}

	return true;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_runContinuousScanAcquisitionSDK2(JNIEnv* env, jclass cls, jshortArray outArray, jobject framecounterObj) {

	assert((env->GetArrayLength(outArray) == imgobj.temparraysize) && "1Darraysize mismatch");

	static jclass framecounter_class = env->FindClass("directCameraReadout/control/FrameCounter"); //method caching
	static jmethodID increment_id = env->GetMethodID(framecounter_class, "increment", "()V");  //cached
	static jfieldID time1_id = env->GetFieldID(framecounter_class, "time1", "D");
	static jfieldID time2_id = env->GetFieldID(framecounter_class, "time2", "D");
	static jfieldID time3_id = env->GetFieldID(framecounter_class, "time3", "D");

	jboolean isCopy{ false };
	jshort* carray = NULL;
	if (imgobj.enableCriticalAccess == 1) {
		carray = (jshort*)env->GetPrimitiveArrayCritical(outArray, &isCopy);
		if (isCopy == true) {
			env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
		}
	}

	tk1.setTimeStart();

	resetImageClsparam();
	imgobj.SetIsStopPressed(false); //reset

	int limit = imgobj.totalFrame_ / imgobj.frameTransferInterval_;
	int c = 0;
	while (c != limit) {//!imgobj.GetIsStopPressed()
		// Stop this thread when stop button (Java GUI) is pressed
		if (imgobj.GetIsStopPressed() == true) {
			break;
		}

		long totalImAcq;
		do {
			imgobj.errorValue = GetTotalNumberImagesAcquired(&totalImAcq);
		} while (totalImAcq < (c + 1) * imgobj.frameTransferInterval_ + imgobj.discaredZeroFramecounter);


		// Get first and last unretrieved image index from buffer
		long first, last, lastTRUE;
		int numberFrames;
		imgobj.errorValue = GetNumberNewImages(&first, &last);


		lastTRUE = first + imgobj.frameTransferInterval_ - 1; // TODO: confirmed correct allocation by passing -1 into element in pImageArray
		numberFrames = imgobj.frameTransferInterval_;

		// Get images and copy to buffer
		long validfirst;
		long validlast;
		unsigned long sizee;
		sizee = imgobj.nopixelX_ * imgobj.nopixelY_ * numberFrames;

		/*
		// The following is necassary to discard first frames. Fix absurd number appearing on first frames of dimension larger than 64 x 64 pixels. UNUSED.
		if (i == 0) {
			first = 2;
			lastTRUE = 2 + imgobj.acquisitionFrameInterval_ - 1;
		}
		*/

		// The following is to remove frames of zero
		long& first_ref = first;
		long& lastTRUE_ref = lastTRUE;

		// zero frame
		if (imgobj.isZeroFrameOccur) {
			first_ref = c * imgobj.frameTransferInterval_ + 1 + imgobj.discaredZeroFramecounter;
			lastTRUE_ref = first + imgobj.frameTransferInterval_ - 1;
		}
		

		//TODO: new index
		int new_c = c % (imgobj.size_b_ / imgobj.frameTransferInterval_);

		// Andor get image buffer to long array
		imgobj.errorValue = GetImages(first, lastTRUE, imgobj.pImageArray_, sizee, &validfirst, &validlast); 
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 55; GetImages error(camera buffer to cpp)");

		// reset
		imgobj.isZeroFrameOccur = false;

		tk2.setTimeStart();
		if (isCopy == true || imgobj.enableCriticalAccess == 0) {// copy elem of long array into double or short array
			/*// for loop is slower; us estd::copy
		if (imgobj.pImageArray_ && imgobj.pImageArrayBuf_) {
			for (int j = 0; j < sizee; j++) {
				*(imgobj.pImageArrayBuf_ + j) = *(imgobj.pImageArray_ + j);
			}
		}
		*/

			tk3.setTimeStart();
			std::copy(imgobj.pImageArray_, imgobj.pImageArray_ + sizee, imgobj.pImageArrayBuf_); //find out if faster
			if (!imgobj.isPointerNull()) {
				env->SetShortArrayRegion(outArray, (new_c * sizee), sizee, imgobj.pImageArrayBuf_);
			}
			env->CallVoidMethod(framecounterObj, increment_id);
			imgobj.timeelapsed3 += tk3.getTimeElapsed();
		}
		else {
			//slow 0.1ms overhead 40ms 128x128
			std::copy(imgobj.pImageArray_, imgobj.pImageArray_ + sizee, imgobj.pImageArrayBuf_); //find out if faster
			std::copy(imgobj.pImageArrayBuf_, imgobj.pImageArrayBuf_ + sizee, carray + (new_c * sizee));
			env->CallVoidMethod(framecounterObj, increment_id);
		}
		imgobj.timeelapsed2 += tk2.getTimeElapsed();

		c++;
	}

	//clean up resources and abort acquisition
	imgobj.errorValue = AbortAcquisition();
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 66 Abort Acquisition");
	if (imgobj.acquisitionMode == 5) {
		free(imgobj.pImageArray_);
		free(imgobj.pImageArrayBuf_);
		imgobj.pImageArray_ = NULL;
		imgobj.pImageArrayBuf_ = NULL;
	}
	
	imgobj.timeelapsed1 = tk1.getTimeElapsed(); //full
	imgobj.timeelapsed2 /= c; // average read buffer and copy
	imgobj.timeelapsed3 /= c;//native heap to java heap

	env->SetDoubleField(framecounterObj, time1_id, imgobj.timeelapsed1);
	env->SetDoubleField(framecounterObj, time2_id, imgobj.timeelapsed2);
	env->SetDoubleField(framecounterObj, time3_id, imgobj.timeelapsed3);

	if (isCopy == false && imgobj.enableCriticalAccess == 1) {
		env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
	}

	return;
}

// Infinite Loop: Live video and non-cumulative/calibration mode V2
// Difference from v1: remove transferFrameInterval Dependencies
JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_setParameterInfiniteLoopV2SDK2(JNIEnv* env, jclass cls, jint size_b, jint totalFrame, jfloat exposureTime, jint width, jint height, jint left, jint top, jint acqmode, jint gain, jint incamerabin, jint cammodel, jint iVspeed, jint iVamp, jint iHspeed, jint iPreAmp, jint isCropMode, jint cWidth, jint cHeight, jint cLeft, jint cTop) {
	imgobj.CameraModel = cammodel;
	imgobj.isCropMode = isCropMode;
	imgobj.setExposureTime(exposureTime);
	imgobj.acquisitionMode = acqmode;
	imgobj.setPixelDimParam(width, height, left, top, incamerabin);
	imgobj.setCropModePixelDimParam(cWidth, cHeight, cLeft, cTop);
	imgobj.getnoPixelXY();
	imgobj.setSize_b(size_b);
	imgobj.gain = gain;
	imgobj.iVSpeed = iVspeed;
	imgobj.iVAmp = iVamp;
	imgobj.iHSpeed = iHspeed;
	imgobj.iPreAmpGain = iPreAmp;
	imgobj.totalFrame_ = totalFrame;

	zeroFrameobj.resetAll();

	setSettingsParameterA();

	setSettingsParameterB(); // set ADchannel, Vertical shift speed, Vertical amp, Horizontal speed, Preamp, emgain


	/*
	// Get camera capabilities
	AndorCapabilities caps;
	caps.ulSize = sizeof(AndorCapabilities);
	errorValue = GetCapabilities(&caps);
	if (errorValue != DRV_SUCCESS) {
	std::cout << "Get Andor Capabilities information Error\n";
	std::cout << "Error: " << errorValue << "\n";
	}
	*/

	/*
	int noScans = 2;
	if (acquisitionMode == 3) {

		// set number of frames for kinetic modes
		errorValue = SetNumberKinetics(noScans);
		std::cout << "number frames: " << noScans << "\n";
		if (errorValue != DRV_SUCCESS) {

			std::cout << "Set number kinetics error \n";
		}

	}
	*/

	// set exposure time
	imgobj.errorValue = SetExposureTime(imgobj.exposureTime_);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set exposure time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 67 SetExposureTime");

	// set trigger mode
	int trigger_mode = 0; // 0 - internal; 1 - external; 6 - external start; 7 - external exposure (bulb); 9 - external FVB EM; 10 - software trigger; 12 - external charge shifting;
	imgobj.errorValue = SetTriggerMode(trigger_mode);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set trigger mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 68 SetTriggerMode");

	float exposure, accumulate, kinetic;
	imgobj.errorValue = GetAcquisitionTimings(&exposure, &accumulate, &kinetic);
	imgobj.setKinetic(kinetic);
	imgobj.exposureTime_ = exposure;
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	assert((imgobj.errorValue) && "error 69 GetAcquisitionTimings");


	//Only needed for kinetic capture
	imgobj.errorValue = SetBaselineClamp(imgobj.baselineClampState);
	assert((imgobj.errorValue) && "error 70 SetBaselineClamp");

	/*
	// set kinetic cycle time
	float cycleTime = 0.0f;
	imgobj.errorValue = SetKineticCycleTime(cycleTime);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set kinetic cycle time Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	*/

	/*
	// Setting of baseline offset
	imgobj.errorValue = SetBaselineOffset(imgobj.baselineOffeset);
	assert((imgobj.errorValue) && "error 39; SetBaselineOffset");
	*/

	/*
	// return maximum number of images circular buffer can store based on current acquisiton setting
	long buffer;
	imgobj.errorValue = GetSizeOfCircularBuffer(&buffer);
	assert((imgobj.errorValue) && "error 41; GetSizeOfCircularBuffer");
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get size of circular buffer Error \n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	else {
		std::cout << "Number of images circular buffer can hold: " << buffer << std::endl;
	}
	*/

	/*
	// Wait for 2 seconds to allow MCD to calibrate fully before allowing an
	// acquisition to begin
	int 				test, test2;

	test = GetTickCount();
	do {
		test2 = GetTickCount() - test;
	} while (test2 < 2000);
	*/

	// make sure camera is in idle state
	int status;
	imgobj.errorValue = GetStatus(&status);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Get acquisition timings Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	while (status != DRV_IDLE) {
		imgobj.errorValue = GetStatus(&status);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "Get acquisition timings Error\n";
			std::cout << "Error: " << imgobj.errorValue << "\n";
		}
	}

	return true;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_runInfiniteLoopV2SDK2(JNIEnv* env, jclass cls, jshortArray outArray) {

	resetImageClsparam();
	imgobj.SetIsStopPressed(false); //reset

	int frameSize = imgobj.nopixelX_ * imgobj.nopixelY_;
	long first, last = 0, validFirst, validLast;
	int c = 0;
	while (last < imgobj.totalFrame_) {
		if (imgobj.GetIsStopPressed() == true) {
			break;
		}
		WaitForAcquisition();
		
		// Alternativ3 to WaitForAcquisition()
		long totalImAcq;
		do {
			GetTotalNumberImagesAcquired(&totalImAcq);
		} while (totalImAcq < (c + 1));
		

		imgobj.errorValue = GetNumberNewImages(&first, &last);
		if (imgobj.errorValue != DRV_SUCCESS) {
			std::cout << "GetNumebrneImages error: " << imgobj.errorValue << std::endl;
		}
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 45 GetNumebrNewImages");

		int tbtFrame = (last - first + 1); //total frame to be transferred
		int size = frameSize * tbtFrame;
		int new_c = c % imgobj.size_b_;

		imgobj.InitArrayInfV2(size);

		// Andor get image buffer to long array
		//imgobj.errorValue = GetImages(first, last, imgobj.pImageArray_ + (new_c * frameSize), size, &validFirst, &validLast); 
		imgobj.errorValue = GetImages(first, last, imgobj.pImageArray_, size, &validFirst, &validLast);
		assert((imgobj.errorValue == DRV_SUCCESS) && "error 55; GetImages error(camera buffer to cpp)");

		// copy elem of long array into double or short array
		copyLongArrayToShortArray(size);

		// short native array to Java Array
		copyToJavaV2(env, outArray, new_c, size, frameSize, tbtFrame);

		c = c + tbtFrame;
	}
	return;
}





/*
get metadata info
*/
// get min and max temperature
JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getMinMaxTemperatureSDK2(JNIEnv* env, jclass cls) {
	imgobj.errorValue = GetTemperatureRange(&imgobj.mintemp, &imgobj.maxtemp);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 2 GetTemperatureRange");

	jint outCArray[] = { 0,0 };
	outCArray[0] = imgobj.mintemp;
	outCArray[1] = imgobj.maxtemp;

	jintArray outArray = env->NewIntArray(2);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 2, outCArray);
	return outArray;
}

// get detector max dimension
JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getDetectorDimensionSDK2(JNIEnv* env, jclass cls) {
	// Get detector information
	int								gblXPixels;       				// dims of
	int								gblYPixels;       				// CCD chip
	imgobj.errorValue = GetDetector(&gblXPixels, &gblYPixels);
	assert((imgobj.errorValue) && "error 26; getdetectordim");

	jint outCArray[] = { 0,0 };
	outCArray[0] = gblXPixels;
	outCArray[1] = gblYPixels;

	jintArray outArray = env->NewIntArray(2);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 2, outCArray);
	return outArray;

}

//get em gain
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getEMGainSDK2(JNIEnv* env, jclass cls) {
	jint result;
	int EMgain = 0;
	imgobj.errorValue = GetEMCCDGain(&EMgain);
	result = EMgain;
	//assert((imgobj.errorValue == DRV_SUCCESS) && "error 81; get EM Gain");
	return result;
}

// is frame transfer
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getFrameTransferSDK2(JNIEnv* env, jclass cls) {
	jint result;
	result = imgobj.isFrameTransfer;
	return result;
}

// get PreAmpGains
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getPreAmpGainSDK2(JNIEnv* env, jclass cls) {
	jfloat result;
	float preampgain;
	imgobj.errorValue = GetPreAmpGain(imgobj.iPreAmpGain, &preampgain);
	result = preampgain;
	return result;

}

// get VS Speed in usecs
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getVSSpeedSDK2(JNIEnv* env, jclass cls) {
	float speed;
	imgobj.errorValue = GetVSSpeed(imgobj.iVSpeed, &speed);
	return (jfloat)speed;
}

// get VS Clock Voltage amplitude
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getVSClockVoltageSDK2(JNIEnv* env, jclass cls) {
	
	int Vampvalue;
	GetVSAmplitudeValue(imgobj.iVAmp, &Vampvalue);
	
	return (jint)Vampvalue;
}

// get number AD channel
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getnADchannelsSDK2(JNIEnv* env, jclass cls) {
	int nChannels;
	imgobj.errorValue = GetNumberADChannels(&nChannels);
	return (jint)nChannels;
}

// get Horizontal Speed/readout rate in MHz: we typically desired the fastest speed available given hardware
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getHSSpeedSDK2(JNIEnv* env, jclass cls) {
	float speed;
	imgobj.errorValue = GetHSSpeed(imgobj.iAD, 0, imgobj.iHSpeed, &speed);
	return (jfloat)speed;
}

// get number of availakble horizontal speed/readout rate
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getNoAvailableHSSpeedSDK2(JNIEnv* env, jclass cls) {
	int numberSpeed;
	imgobj.errorValue = GetNumberHSSpeeds(imgobj.iAD, 0, &numberSpeed);
	return (jint)(numberSpeed);
}

// get fastet vertical speed
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getFastestVerticalSpeedSDK2(JNIEnv* env, jclass cls) {
	float speed;
	int ispeed;
	GetFastestRecommendedVSSpeed(&ispeed, &speed);
	return (jfloat)speed;
}

// get exposure time setted
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getExposureTimeSDK2(JNIEnv* env, jclass cls) {
	return (jfloat)imgobj.exposureTime_;
}

// get kinetic cycle
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getKineticCycleSDK2(JNIEnv* env, jclass cls) {
	return(imgobj.kinetic_);
}

// get baseline clamp state
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getBaseLineClampStateSDK2(JNIEnv* env, jclass cls) {
	int BaselineClampState;
	imgobj.errorValue = GetBaselineClamp(&BaselineClampState);
	return (jint)BaselineClampState;
}

//get baseline offset
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getBaseLineOffsetSDK2(JNIEnv* env, jclass cls) {
	return (jint)imgobj.baselineOffeset;
}

//get bit depth given AD converter
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getBitDepthSDK2(JNIEnv* env, jclass cls) {
	int bitdepth;
	imgobj.errorValue = GetBitDepth(imgobj.iAD, &bitdepth);
	return (jint)bitdepth;
}

// is cooler on
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getisCoolerOnSDK2(JNIEnv* env, jclass cls) {

	int cooler_on;
	imgobj.errorValue = IsCoolerOn(&cooler_on);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 78 IsCoolerOn");
	return (jint)cooler_on;

}

// get width, hegith, left, top

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getWidthSDK2(JNIEnv* env, jclass cls) {
	return (jint)imgobj.nopixelX_;

}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getHeightSDK2(JNIEnv* env, jclass cls) {
	return (jint)imgobj.nopixelY_;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getLeftSDK2(JNIEnv* env, jclass cls) {
	return (jint)imgobj.hstart;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getTopSDK2(JNIEnv* env, jclass cls) {
	return (jint)imgobj.vstart;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getCameraSerialNumSDK2(JNIEnv* env, jclass cls) {
	int serial;
	imgobj.errorValue = GetCameraSerialNumber(&serial);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 79 getSerialNumber");
	return (jint)serial;
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_getHeadModelSDK2(JNIEnv* env, jclass cls) {
	char model[32];
	imgobj.errorValue = GetHeadModel(model);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error 80 getHeadModel");
	return (*env).NewStringUTF(model);
}

// zero frame
JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetArrayZero(JNIEnv* env, jclass cls) {
	/*
	int* array = new int[5];
	array[0] = 1;
	array[1] = 2;
	array[2] = 3;
	array[3] = 4;
	array[4] = 5;
	*/
	// hardcoded 10 element in array
	jint outCArray[] = { 0,0,0,0,0,0,0,0,0,0 };
	for (int i = 0; i < 10; i++) {
		outCArray[i] = zeroFrameobj.array[i];
	}

	jintArray outArray = env->NewIntArray(10);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 10, outCArray);
	return outArray;

}

JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetArrayndZero(JNIEnv* env, jclass cls) {
	/*
	int* array = new int[5];
	array[0] = 1;
	array[1] = 2;
	array[2] = 3;
	array[3] = 4;
	array[4] = 5;
	*/
	// hardcoded 10 element in array
	jint outCArray[] = { 0,0,0,0,0,0,0,0,0,0 };
	for (int i = 0; i < 10; i++) {
		outCArray[i] = zeroFrameobj.arrayndzero[i];
	}

	jintArray outArray = env->NewIntArray(10);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 10, outCArray);
	return outArray;

}

JNIEXPORT jintArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetArraydiscardZero(JNIEnv* env, jclass cls) {
	/*
	int* array = new int[5];
	array[0] = 1;
	array[1] = 2;
	array[2] = 3;
	array[3] = 4;
	array[4] = 5;
	*/
	// hardcoded 10 element in array
	jint outCArray[] = { 0,0,0,0,0,0,0,0,0,0 };
	for (int i = 0; i < 10; i++) {
		outCArray[i] = zeroFrameobj.arraydiscardzero[i];
	}

	jintArray outArray = env->NewIntArray(10);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 10, outCArray);
	return outArray;

}

JNIEXPORT jobjectArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetStringArray(JNIEnv* env, jclass cls) {
	jobjectArray result;
	result = (jobjectArray)env->NewObjectArray(3, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	for (int i = 0; i < 3; i++) {
		env->SetObjectArrayElement(result, i, env->NewStringUTF(std::to_string(i).c_str()));
	}
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetAvailableVSAmplitudeSDK2(JNIEnv* env, jclass cls) {
	//functional for 860 888 897
	int numberAvailVS;
	GetNumberVSAmplitudes(&numberAvailVS);

	// Fill array with VSamplitude
	std::vector<std::string> VSAmpArray{""};
	VSAmpArray.push_back("Normal");
	for (int i = 1; i < numberAvailVS; i++) {
		std::string prefix = "+";
		std::string val = std::to_string(i);
		prefix.append(val);
		VSAmpArray.push_back(prefix);
	}
	VSAmpArray.erase(VSAmpArray.begin());

	jobjectArray result;
	result = (jobjectArray)env->NewObjectArray(numberAvailVS, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	for (int i = 0; i < numberAvailVS; i++) {
		env->SetObjectArrayElement(result, i, env->NewStringUTF(VSAmpArray[i].c_str()));
	}

	return result;

}

JNIEXPORT jobjectArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetAvailableVSSpeedsSDK2(JNIEnv* env, jclass cls) {
	//functional for 860 888 897
	int numberAvailVSpeed;
	GetNumberVSSpeeds(&numberAvailVSpeed);

	std::vector<std::string> VSpeedArray{""};
	float speed;
	for (int i = 0; i < numberAvailVSpeed; i++) {
		GetVSSpeed(i, &speed);
		VSpeedArray.push_back(std::to_string(speed));
	}
	VSpeedArray.erase(VSpeedArray.begin());

	jobjectArray result;
	result = (jobjectArray)env->NewObjectArray(numberAvailVSpeed, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	for (int i = 0; i < numberAvailVSpeed; i++) {
		env->SetObjectArrayElement(result, i, env->NewStringUTF(VSpeedArray[i].c_str()));
	}

	return result;
}

JNIEXPORT jobjectArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetAvailableHSSpeedsSDK2(JNIEnv* env, jclass cls) {

	int iAD = getADnumber();
	int numberAvailHSpeed;
	float HSpeed;
	GetNumberHSSpeeds(iAD, 0, &numberAvailHSpeed);

	std::vector<std::string> HSpeedArray{ "" };
	for (int i = 0; i < numberAvailHSpeed; i++) {
		
		GetHSSpeed(iAD, 0, i, &HSpeed);

		HSpeedArray.push_back(std::to_string(HSpeed));
	}
	HSpeedArray.erase(HSpeedArray.begin());

	jobjectArray result;
	result = (jobjectArray)env->NewObjectArray(numberAvailHSpeed, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	for (int i = 0; i < numberAvailHSpeed; i++) {
		env->SetObjectArrayElement(result, i, env->NewStringUTF(HSpeedArray[i].c_str()));
	}

	return result;
}

JNIEXPORT jobjectArray JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_GetAvailablePreAmpGainSDK2(JNIEnv* env, jclass cls) {

	int noGains;
	int ihs = imgobj.iHspeedFastest; //index hspeed to cheack (iset this to maximum horizontal speed just
	int status;
	float gain;

	GetNumberPreAmpGains(&noGains);

	std::vector<std::string> GainsArray{ "" };

	int removecnts = 0;
	for (int i = 0; i < noGains; i++) {
		IsPreAmpGainAvailable(imgobj.iAD, 0, ihs, i, &status);
		if (status == 1) {//if pre amp gain is available
			GetPreAmpGain(i, &gain);
			GainsArray.push_back(std::to_string(gain));
		}
		else {
			removecnts++;
		}
	}

	noGains = noGains - removecnts;
	GainsArray.erase(GainsArray.begin());
	jobjectArray result;
	result = (jobjectArray)env->NewObjectArray(noGains, env->FindClass("java/lang/String"), env->NewStringUTF(""));
	for (int i = 0; i < noGains; i++) {
		env->SetObjectArrayElement(result, i, env->NewStringUTF(GainsArray[i].c_str()));
	}

	

	return result;
	
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_ShutterControlSDK2(JNIEnv* env, jclass cls, jboolean isShutterON) {
	int shutter; //1 open permanent,2 closed
	if (isShutterON) {
		shutter = 1;
	}
	else {
		shutter = 2;
	}
	// Set shutter
	imgobj.errorValue = SetShutter(1, shutter, 1, 1);
	assert((imgobj.errorValue == DRV_SUCCESS) && "error shutter control");

	return;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_TESTsetParam(JNIEnv* env, jclass cls, jint w, jint h, jint l, jint t, jint incamerabin) {
	imgobj.errorValue = SetReadMode(4);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "error SetReadMode: " << imgobj.errorValue << std::endl;
	}
	
	// Setting ROI to center with 1 x 1 binning. //TODO: binning
	int hbin = incamerabin;
	int vbin = incamerabin;
	int hstart = (l - 1) * hbin + 1;
	int hend = hstart + (w * hbin) - 1;
	int vstart = (t - 1) * vbin + 1;
	int vend = vstart + (h * vbin) - 1;

	imgobj.errorValue = SetImage(hbin, vbin, hstart, hend, vstart, vend);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "error SetImage: " << imgobj.errorValue << std::endl;
	}
	

}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk2v3_AndorSDK2v3_TESTcropMode(JNIEnv* env, jclass cls, jint isCropMode, jint cropW, jint cropH, jint binW, jint binH , jint cropL, jint cropT) {
	
	imgobj.errorValue = SetReadMode(4);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "error SetReadMode: " << imgobj.errorValue << std::endl;
	}


	
	imgobj.errorValue = SetAcquisitionMode(5); //5
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set Acquisition Mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}
	

	imgobj.errorValue = SetFrameTransferMode(1);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "error SetFrameTransferMode: " << imgobj.errorValue << std::endl;
	}




	imgobj.errorValue = SetIsolatedCropModeEx(isCropMode, cropH, cropW, binH, binW, cropL, cropT);
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "error SetIsolatedCropModeEx: " << imgobj.errorValue << std::endl;
	}

	imgobj.errorValue = SetAcquisitionMode(1); //5
	if (imgobj.errorValue != DRV_SUCCESS) {
		std::cout << "Set Acquisition Mode Error\n";
		std::cout << "Error: " << imgobj.errorValue << "\n";
	}


}
