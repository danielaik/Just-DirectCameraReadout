#include <iostream>
#include "directCameraReadout_pvcamsdk_Photometrics_PVCAM_SDK.h"
#include <jni.h>
#include "ImageClsPVCam.h"
#include "TimeKeeper.h"

ImagePVCam imgobj;
TimeKeeper tk1;
TimeKeeper tk2;
TimeKeeper tk3;
TimeKeeper tk4;
TimeKeeper tk5;

//prototype
int setParameter(double exptime, int oW, int oH, int oL, int oT, int incamerabin, int acqmode, int totalframe, int size_b, int arraysize, int portIndex, int speedIndex);
void readBufferSingle(int pixelperframe);// read buffer (not so elegant way); TODO:any faster way?
void readBufferSingle8bit(int pixelperframe);// read buffer (not so elegant way); TODO:any faster way?
void readBufferCpyJNI(jshort* buffer, int pixelperframe, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);
void readBufferCpy8bitJNI(uns8* buffer8bit, int pixelperframe, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);


//method definition
int setParameter(double exptime, int oW, int oH, int oL, int oT, int incamerabin, int acqmode, int totalframe, int size_b, int arraysize, int portIndex, int speedIndex) {
	tk2.setTimeStart();

	int iRet = 0;
	//std::cout << "cpp exptime: " << exptime << ", oW: " << oW << ", oH: " << oH << ", oL: " << oL << ", oT: " << oT << ", incamerabin: " << incamerabin << ", acqmode: " << acqmode << ", totalfraem: " << totalframe << ", size_b: " << size_b << std::endl;

	imgobj.reset();

	imgobj.setFrameTransfer();
	imgobj.setPortAndSpeedPair(portIndex, speedIndex);

	imgobj.acqmode_ = acqmode;
	imgobj.totalframe_ = totalframe;
	imgobj.setExposureTime(exptime);
	

	imgobj.setPixelDimParam(oW, oH, oL, oT, incamerabin);
	imgobj.getnoPixelXY();
	imgobj.temparraysize = arraysize;
	imgobj.size_b_ = arraysize - (arraysize % (imgobj.nopixelX_ * imgobj.nopixelY_));
	imgobj.size_b_ /= (imgobj.nopixelX_ * imgobj.nopixelY_);
	imgobj.setnoElementArray();


	imgobj.setAOI();
	imgobj.setupSequence();
	imgobj.InitArray();

	// check bit depth
	pl_get_param(g_hCam, PARAM_BIT_DEPTH, ATTR_CURRENT, (void*)&imgobj.bitDepth_);
	if (imgobj.bitDepth_ == 8) {
		imgobj.is8bit = true;
	}
	else {
		imgobj.is8bit = false;
	}

	//std::cout << ", arraysize: " << imgobj.arraysize_ << ", nopixelX: " << imgobj.nopixelX_ << ", nopixelY: " << imgobj.nopixelY_ << ", buffSize: " << imgobj.buffSize_ << std::endl;
	imgobj.timeelapsed2 = tk2.getTimeElapsed();
	return iRet;
}
void readBufferSingle(int pixelperframe) {

	//TODO: Time it!
	// copy uns16 1Darray to short 1Darray
	for (int i = 0; i < pixelperframe; i++) {
		*(imgobj.pImageArrayShort_ + i) = *(imgobj.pImageArray_ + i);
	}

	return;
}

void readBufferSingle8bit(int pixelperframe) {

	//TODO: Time it!
	// copy uns8 1Darray to short 1Darray
	for (int i = 0; i < pixelperframe; i++) {
		*(imgobj.pImageArrayShort_ + i) = *(imgobj.pImageArray8bit_ + i);
	}

	return;
}
void readBufferCpyJNI(jshort* buffer, int pixelperframe, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {
	//10k, 32x32, 1ms / 5k, 128x128, 1ms (average tk3 + tk4)
	// Casting -> 0 + 0.106 ms  / 0 + 0.73 ms
	// std::copy -> 0 + 0.0983 ms / 0.003 + 0.786 ms
	// for loop ->	0.0016 + 0.15 ms	/ 0.0254 + 0.856 ms

	if (isCopy == true || imgobj.enableCriticalAccess == 0) {

		tk3.setTimeStart();
		const jshort* s = (const jshort*)buffer;

		//copy uns16 circularbuffer to short 1Darray
		/*// UNUSED; std::copy is faster
		for (int i = 0; i < pixelperframe; i++) {
			*(imgobj.pImageArrayShort_ + i) = *(buffer + i);
		}
		*/

		//std::copy(buffer, buffer + pixelperframe, imgobj.pImageArrayShort_); 
		imgobj.timeelapsed3 += tk3.getTimeElapsed();

		tk4.setTimeStart();
		// make frame available to java via JNI
		//_env->SetShortArrayRegion(_outArray, (new_c * pixelperframe), pixelperframe, imgobj.pImageArrayShort_);
		_env->SetShortArrayRegion(_outArray, (new_c * pixelperframe), pixelperframe, s);
		_env->CallVoidMethod(framecounterObj, increment_id);
		imgobj.timeelapsed4 += tk4.getTimeElapsed();

	}
	else {
		std::copy(buffer, buffer + pixelperframe, carray + (new_c * pixelperframe));
		_env->CallVoidMethod(framecounterObj, increment_id);
	}



	return;
}

void readBufferCpy8bitJNI(uns8* buffer8bit, int pixelperframe, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {
	//10k, 32x32, 1ms / 5k, 128x128, 1ms (average tk3 + tk4)
	// Casting -> 0 + 0.106 ms  / 0 + 0.73 ms
	// std::copy -> 0 + 0.0983 ms / 0.003 + 0.786 ms
	// for loop ->	0.0016 + 0.15 ms	/ 0.0254 + 0.856 ms

	if (isCopy == true || imgobj.enableCriticalAccess == 0) {

		tk3.setTimeStart();
		const uns8* s8bit = (const uns8*)buffer8bit;

		//copy uns8 circularbuffer to short 1Darray
		// UNUSED; std::copy is faster
		/*
		for (int i = 0; i < pixelperframe; i++) {
			*(imgobj.pImageArrayShort_ + i) = *(buffer8bit + i);
		}
		*/
		
		std::copy(buffer8bit, buffer8bit + pixelperframe, imgobj.pImageArrayShort_);

		imgobj.timeelapsed3 += tk3.getTimeElapsed();

		tk4.setTimeStart();
		// make frame available to java via JNI
		//_env->SetShortArrayRegion(_outArray, (new_c * pixelperframe), pixelperframe, imgobj.pImageArrayShort_);
		_env->SetShortArrayRegion(_outArray, (new_c * pixelperframe), pixelperframe, imgobj.pImageArrayShort_);
		_env->CallVoidMethod(framecounterObj, increment_id);
		imgobj.timeelapsed4 += tk4.getTimeElapsed();

	}
	else {
		std::copy(buffer8bit, buffer8bit + pixelperframe, carray + (new_c * pixelperframe));
		_env->CallVoidMethod(framecounterObj, increment_id);
	}



	return;
}

JNIEXPORT jfloat JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_seyHello(JNIEnv* env, jclass cls, jint n1, jint n2) {
	jfloat result;
	result = ((jfloat)n1 + n2) / 2.0;
	return result;
}


JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_sayHello(JNIEnv* env, jobject thisObj) {
	std::cout << "hello from cpp" << std::endl;
	return 1;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_isPHOTOMETRICSconnectedPVCAM(JNIEnv* env, jclass cls) {
	// 0 = good to go; 195 = third party software accessing same camera; 186 = no camera detected
	int iret = imgobj.isCameraConnected();
	return iret;

}

JNIEXPORT jstring JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_GetModelPVCAM(JNIEnv* env, jclass cls) {
	return (*env).NewStringUTF(imgobj.getChipName());
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_GetCameraNamePVCAM(JNIEnv* env, jclass cls) {
	return (*env).NewStringUTF(imgobj.getCameraName());
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_InitializePVCAM(JNIEnv* env, jclass cls) {
	return imgobj.initSystem();
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_SystemShutDownPVCAM(JNIEnv* env, jclass cls) {
	return imgobj.uninitSystem();
}

JNIEXPORT jintArray JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_GetDetectorDimPVCAM(JNIEnv* env, jclass cls) {
	int* res = new int[2];
	res = imgobj.getchipSize();

	jint outCArray[] = { 0, 0 };
	outCArray[0] = res[0];
	outCArray[1] = res[1];

	jintArray outArray = env->NewIntArray(2);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 2, outCArray);

	return outArray;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_setParameterPVCAM(JNIEnv* env, jclass cls, jdouble exposuretime, jint width, jint height, jint left, jint top, jint incamerabin, jint acqmode, jint totalframe, jint size_b, jint arraysize, jint portIndex, jint speedIndex) {
	return setParameter(exposuretime, width, height, left, top, incamerabin, acqmode, totalframe, size_b, arraysize, portIndex, speedIndex);
}

JNIEXPORT jdouble JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_getKineticCyclePVCAM(JNIEnv* env, jclass cls) {
	std::cout << "JNICALL cpp getKinteit cycle not implemetned" << std::endl;
	return 0.01;
}

JNIEXPORT jshortArray JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_runSingleScanPVCAM(JNIEnv* env, jclass cls) {

	// Enable BOF/EOF counter
	uns32 paramEofEnable = END_FRAME_IRQS;
	if (PV_OK != pl_set_param(g_hCam, PARAM_BOF_EOF_ENABLE, (void*)&paramEofEnable))
	{
		//PrintErrorMessage(pl_error_code(), "pl_set_param(PARAM_BOF_EOF_ENABLE) error");
		CloseCameraAndUninit();
		assert((false) && "failed enable EOF");
	}

	// Clear BOF/EOF counter
	rs_bool bofEofClear = true;
	if (PV_OK != pl_set_param(g_hCam, PARAM_BOF_EOF_CLR, (void*)&bofEofClear))
	{
		//PrintErrorMessage(pl_error_code(), "pl_set_param(PARAM_BOF_EOF_CLR) error");
		CloseCameraAndUninit();
		assert((false) && "failed clear EOF");
	}

	// Call this after each clear of BOF/EOF counter
	if (PV_OK != pl_exp_abort(g_hCam, CCS_CLEAR))
	{
		//PrintErrorMessage(pl_error_code(), "pl_exp_abort() error");
		CloseCameraAndUninit();
		assert((false) && "failed CCS_CLEAR");
	}

	// Start the acqusition - it is used as software trigger in TIMED trigger mode.
	// In hardware trigger mode (Strobe or Bulb) after this call camera waits for
	// external trigger signal.
	if (imgobj.is8bit) {
		if (PV_OK != pl_exp_start_seq(g_hCam, (void*)imgobj.pImageArray8bit_))
		{
			//PrintErrorMessage(pl_error_code(), "pl_exp_start_seq() error");
			imgobj.freeImArray();
			CloseCameraAndUninit();
		}
	}
	else {
		if (PV_OK != pl_exp_start_seq(g_hCam, (void*)imgobj.pImageArray_))
		{
			//PrintErrorMessage(pl_error_code(), "pl_exp_start_seq() error");
			imgobj.freeImArray();
			CloseCameraAndUninit();
		}
	}
	
	
	int16 status;
	uns32 byte_cnt;

	uns32 eofCount = 0;
	uns32 oldEofCount = 0;

	// Keep checking the camera readout status.
	// Function returns FALSE if status is READOUT_FAILED
	while (pl_exp_check_status(g_hCam, &status, &byte_cnt)
		&& status != READOUT_COMPLETE && status != READOUT_NOT_ACTIVE)
	{
		// In each loop you can optionally make this call to see how many frames
		// have been acquired so far
		if (PV_OK != pl_get_param(g_hCam, PARAM_BOF_EOF_COUNT, ATTR_CURRENT,
			(void*)&eofCount))
		{
			//PrintErrorMessage(pl_error_code(),"pl_get_param(PARAM_BOF_EOF_COUNT) error");
		}
		else if (eofCount > oldEofCount)
		{
			oldEofCount = eofCount;
			//printf("Acquired frame #%u out of %u\n", eofCount, imgobj.totalframe_);
		}

		std::this_thread::sleep_for(std::chrono::milliseconds(10));
	}

	if (status == READOUT_FAILED)
	{
		assert((false) && "single capture Frame readout failed");
	}

	// Same call as in while loop above for the last frame since EOF counter
	// was incremented only after READOUT_COMPLETE status was reported and
	// the loope exited.
	if (PV_OK != pl_get_param(g_hCam, PARAM_BOF_EOF_COUNT, ATTR_CURRENT, (void*)&eofCount)) {
		//PrintErrorMessage(pl_error_code(), "pl_get_param(PARAM_BOF_EOF_COUNT) error");
	}
	else {
		//printf("Acquired frame #%u out of %u\n", eofCount, imgobj.totalframe_);
	}

	//read from buffer
	if (imgobj.is8bit) {
		readBufferSingle8bit(imgobj.arraysize_);
	}
	else {
		readBufferSingle(imgobj.arraysize_);
	}
	

	// When acquiring single frames with callback notifications call this
	// after each frame before new acquisition is started with pl_exp_start_seq()
	if (imgobj.is8bit) {
		if (PV_OK != pl_exp_finish_seq(g_hCam, imgobj.pImageArray8bit_, 0)) {
			//PrintErrorMessage(pl_error_code(), "pl_exp_finish_seq() error");
			assert((false) && "err 56 pl_exp_finish_seq");
		}
	}
	else {
		if (PV_OK != pl_exp_finish_seq(g_hCam, imgobj.pImageArray_, 0)) {
			//PrintErrorMessage(pl_error_code(), "pl_exp_finish_seq() error");
			assert((false) && "err 56 pl_exp_finish_seq");
		}
	}
	
		

	jshortArray outArray;
	outArray = env->NewShortArray(imgobj.arraysize_);
	env->SetShortArrayRegion(outArray, 0, imgobj.arraysize_, imgobj.pImageArrayShort_);
	imgobj.freeImArray();
	return outArray;
}

JNIEXPORT void JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_runInfiniteLoopPVCAM(JNIEnv* env, jclass cls, jshortArray outArray, jobject framecounterObj) {

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
	
	

	
	// Start the continuous acquisition, again tell this function size of buffer
	// it has for the frames. Camera starts the first acquisition based on this
	// host command and subsequent acquisitions are triggered internally by the
	// camera itself. In hardware trigger mode this sets the camera to waiting
	// state awaiting external trigger signals.
	tk1.setTimeStart();

	
	if (PV_OK != pl_exp_start_cont(g_hCam, imgobj.circBufferInMemory_, imgobj.circBufferBytes_)) {
		//PrintErrorMessage(pl_error_code(), "pl_exp_start_cont error");
		imgobj.freeImArray();
		CloseCameraAndUninit();
		assert((false) && "err 57 pl_exp_start_cont");
	}
	
	/*
	if (PV_OK != pl_exp_start_cont(g_hCam, imgobj.pImageCircularBuffer_, imgobj.buffSize_))
	{
		//PrintErrorMessage(pl_error_code(), "pl_exp_start_cont error");
		imgobj.freeImArray();
		CloseCameraAndUninit();
		assert((false) && "err 57 pl_exp_start_cont");
	}
	*/

	// loop over frames
	rs_bool errorOccured = FALSE;
	uns32 framesAcquired = 0;
	long64 timefirstframe;
	int new_c;

	while (framesAcquired < imgobj.totalframe_) {

		imgobj.dataContext.myData1++;
		imgobj.dataContext.myData2--;

		// Here we need to wait for a frame readout notification signaled by
		// g_EofCond which is raised in the NewFrameHandler() callback.
		{

			std::unique_lock<std::mutex> lock(g_EofMutex);
			if (!g_EofFlag)
			{
				g_EofCond.wait_for(lock, std::chrono::seconds(5), []() {
					return (g_EofFlag);
					});
			}
			if (!g_EofFlag)
			{
				CloseCameraAndUninit();
				errorOccured = TRUE;
				assert((false) && "Camera timed out waiting for a frame");
				break;
			}
			g_EofFlag = false; // Reset flag
		}

		if (framesAcquired == 0) {
			timefirstframe = g_pFrameInfo->TimeStamp * 100; //usec
		}
		if (framesAcquired == 1) {
			imgobj.frameTime_ = (double)(g_pFrameInfo->TimeStamp * 100 - timefirstframe) / 1000000;//sec
		}


		//// Timestamp is in hundreds of microseconds
		//printf("Frame #%d acquired, timestamp = %lldus\n", g_pFrameInfo->FrameNr, 100 * g_pFrameInfo->TimeStamp);

		// Get the address of the latest frame in the circular buffer
		new_c = framesAcquired % imgobj.size_b_;

		if (imgobj.is8bit) {
			uns8* frameAddress = NULL;

			if (PV_OK != pl_exp_get_latest_frame(g_hCam, (void**)&frameAddress))
			{
				CloseCameraAndUninit();
				/*PrintErrorMessage(pl_error_code(), "pl_exp_get_latest_frame() error");*/
				errorOccured = TRUE;
				assert((false) && "error Get latest address");
				break;
			}

			tk5.setTimeStart();
			//copy from uns16 buffer to short array one frame at at time and make frame available to java
			readBufferCpy8bitJNI(frameAddress, imgobj.arraysize_, new_c, env, outArray, framecounterObj, increment_id, carray, isCopy);//8bit
		}
		else {
			jshort* frameAddress = NULL;
			//uns16* frameAddress; //works too

			if (PV_OK != pl_exp_get_latest_frame(g_hCam, (void**)&frameAddress))
			{
				CloseCameraAndUninit();
				/*PrintErrorMessage(pl_error_code(), "pl_exp_get_latest_frame() error");*/
				errorOccured = TRUE;
				assert((false) && "error Get latest address");
				break;
			}

			tk5.setTimeStart();
			//copy from uns16 buffer to short array one frame at at time and make frame available to java
			readBufferCpyJNI(frameAddress, imgobj.arraysize_, new_c, env, outArray, framecounterObj, increment_id, carray, isCopy);//16bit and 12bit
		}


		////display first uns16 5 pixel
		//ShowImage(frameAddress, imgobj.arraysize_, NULL);
	
	
		
		
		imgobj.timeelapsed5 += tk5.getTimeElapsed();

		if (imgobj.isStopPressed_ == true) {
			break;
		}

		framesAcquired++;
	}

	// Once we have acquired the number of frames needed the acquisition can be
	// stopped, no other call is needed to stop the acquisition.
	if (PV_OK != pl_exp_stop_cont(g_hCam, CCS_CLEAR)) {
		CloseCameraAndUninit();
		//PrintErrorMessage(pl_error_code(), "pl_exp_stop_cont() error");
		assert((false) && "error pl_exp_stop_cont");
	}
	imgobj.timeelapsed1 = tk1.getTimeElapsed(); //full
	imgobj.timeelapsed3 /= framesAcquired; //average copy
	imgobj.timeelapsed4 /= framesAcquired; //native heap to java heap
	imgobj.timeelapsed5 /= framesAcquired;// average read buffer and copy

	env->SetDoubleField(framecounterObj, time1_id, imgobj.timeelapsed1);
	env->SetDoubleField(framecounterObj, time2_id, imgobj.timeelapsed5);
	env->SetDoubleField(framecounterObj, time3_id, imgobj.timeelapsed4);

	if (isCopy == false && imgobj.enableCriticalAccess == 1) {
		if (carray != NULL) {
			env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
		}
		
	}

	imgobj.freeImArray();

	if (errorOccured)
		assert((false) && "errorOccured!");

	return;
}

JNIEXPORT void JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_setStopMechanismPVCAM(JNIEnv* env, jclass cls, jboolean isstoppressed) {
	imgobj.isStopPressed_ = isstoppressed;
	return;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_debugMyData1PVCAM(JNIEnv* env, jclass cls) {
	return imgobj.dataContext.myData1;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_debugMyData2PVCAM(JNIEnv* env, jclass cls) {
	return imgobj.dataContext.myData2;
}

JNIEXPORT jdouble JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_getDoubleValuePVCAM(JNIEnv* env, jclass cls, jstring inJNIStr) {
	//ID - exposuretime; ID - frametime; ID - top; ID - left; ID - width; ID - height; ID - CPPtime1; ID - CPPtime2; ID - incamerabin;
	const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
	if (NULL == inCStr) return NULL;

	const std::string name = inCStr;
	double res;

	if (name == "exposuretime") {
		return imgobj.exposureTime_;
	}
	else if (name == "frametime") {
		return imgobj.frameTime_;
	}
	else if (name == "top") {
		return (double)imgobj.vstart + 1; //index start at 1
	}
	else if (name == "left") {
		return (double)imgobj.hstart + 1; //index start at 1
	}
	else if (name == "width") {
		return (double)imgobj.nopixelX_; 
	}
	else  if (name == "height") {
		return (double)imgobj.nopixelY_;
	}
	else if (name == "incamerabin") {
		return (double)imgobj.hbin;
	}
	else if (name == "CPPtime1") {
		return imgobj.timeelapsed1;
	}
	else  if (name == "CPPtime2") {
		return imgobj.timeelapsed2;
	}
	else  if (name == "CPPtime3") {
		return imgobj.timeelapsed3;
	}
	else  if (name == "CPPtime4") {
		return imgobj.timeelapsed4;
	}
	else  if (name == "CPPtime5") {
		return imgobj.timeelapsed5;
	}
	else if (name == "frameTransfer") {
		return (double) g_IsFrameTransfer;
	}
	else if (name == "PMODE") {
		pl_get_param(g_hCam, PARAM_PMODE, ATTR_CURRENT, (void*)&imgobj.PMode_);
		return (double)imgobj.PMode_;
	}
	else if (name == "BIT_DEPTH") {
		pl_get_param(g_hCam, PARAM_BIT_DEPTH, ATTR_CURRENT, (void*)&imgobj.bitDepth_);
		return (double)imgobj.bitDepth_;
	}
	else if (name == "readoutFrequency") {
		pl_get_param(g_hCam, PARAM_PIX_TIME, ATTR_CURRENT, (void*)&imgobj.pixTime_);
		imgobj.readoutFrequency_ = 1000 / (float)imgobj.pixTime_;
		return (double)imgobj.readoutFrequency_;
	}
	else {
		assert((false) && "getDoubleValuePVCAM unmatched argument");
	}
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_getPortSize
(JNIEnv* env, jclass cls) {
	int noPort = imgobj.getTotalPortNo();
	assert((noPort != 0) && "getTotalPortNo error");
	return noPort;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_getSpeedCount
(JNIEnv* env, jclass cls, jint portidx) {
	int speedCount = imgobj.getSpeedCount(portidx);
	assert((speedCount != 0) && "getSpeedCount error");
	return speedCount;
}


JNIEXPORT void JNICALL Java_directCameraReadout_pvcamsdk_Photometrics_1PVCAM_1SDK_setPortAndSpeedPair
(JNIEnv* env, jclass cls, jint portidx, jint speedidx) {
	int proceed = imgobj.setPortAndSpeedPair(portidx, speedidx);
	assert((proceed != 0) && "getSpeedCount error");
	return;
}