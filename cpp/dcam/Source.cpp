//#include <jni.h>
#include <iostream>
//#include <assert.h>
#include "directCameraReadout_hamadcamsdk4_Hamamatsu_DCAM_SDK4.h"
#include "dcamprop.h"
#include "ImageClsSDK4.h"
#include "OrcaJob.h"
#include "TimeKeeper.h"




class Synchron {
public:
	OrcaJob* ptrOrcajobobj;
	Synchron() {
		std::cout << "syncrhon constructor" << std::endl;
	}
	~Synchron() {
		std::cout << "syncrhon destructor" << std::endl;
	}
};

ImageSDK4 imgobj;
Synchron synchronobj;
TimeKeeper tk1;
TimeKeeper tk2;
TimeKeeper tk3;


//prototype
inline const int my_dcamdev_string(DCAMERR& err, HDCAM hdcam, int32 idStr, char* text, int32 textbytes);
float* get_propertyattr_range(DCAMPROP_ATTR propattr);
float* dcamcon_ret_propertyattr_range(int32 iProp);
int setParameter(double exptime, int oW, int oH, int oL, int oT, int incamerabin, int acqmode, int totalframe, int size_b, int arraysize, int outTriggerKind, double outTrigDelay, double outTrigPeriod, int readoutSpeedIdx, int sensorModeIdx);
void run_acquisition(HDCAM hdcam, HDCAMWAIT hwait); //UNUSED
void run_acquisition(HDCAM hdcam, HDCAMWAIT hwait, JNIEnv* _env, jshortArray _outArray);
void sample_wait_single(HDCAM hdcam, HDCAMWAIT hwait);
int readBufferSingle(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height);
int performAcquisition_infiniteLoop(HDCAM hdcam, HDCAMWAIT hwait, int totalframe, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);
int readBufferCpyJNI(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);

//method definition
inline const int my_dcamdev_string(DCAMERR& err, HDCAM hdcam, int32 idStr, char* text, int32 textbytes)
{
	DCAMDEV_STRING	param;
	memset(&param, 0, sizeof(param));
	param.size = sizeof(param);
	param.text = text;
	param.textbytes = textbytes;
	param.iString = idStr;

	err = dcamdev_getstring(hdcam, &param);
	return !failed(err);
}

float* get_propertyattr_range(DCAMPROP_ATTR propattr) {
	float* range = new float[2];
	if (propattr.attribute & DCAMPROP_ATTR_HASRANGE)
	{
		range[0] = (float)propattr.valuemin;
		range[1] = (float)propattr.valuemax;
	}
	else {
		return NULL;
	}
	return range;
}

float* dcamcon_ret_propertyattr_range(int32 iProp) {
	memset(&imgobj.basepropattr, 0, sizeof(imgobj.basepropattr));
	imgobj.basepropattr.cbSize = sizeof(imgobj.basepropattr);
	imgobj.basepropattr.iProp = iProp;

	imgobj.err = dcamprop_getattr(imgobj.hdcam, &imgobj.basepropattr);
	if (!failed(imgobj.err)) {
		float* res = get_propertyattr_range(imgobj.basepropattr);
		return res;
	}
	else {
		return NULL;
	}
}

int setParameter(double exptime, int oW, int oH, int oL, int oT, int incamerabin, int acqmode, int totalframe, int size_b, int arraysize, int outTriggerKind, double outTrigDelay, double outTrigPeriod, int readoutSpeedIdx, int sensorModeIdx) {
	int iRet;
	imgobj.reset();
	imgobj.acqmode_ = acqmode;
	imgobj.totalframe_ = totalframe;

	imgobj.setPixelDimParam(oW, oH, oL, oT, incamerabin);
	imgobj.getnoPixelXY();
	imgobj.temparraysize = arraysize;
	imgobj.size_b_ = arraysize - (arraysize % (imgobj.nopixelX_ * imgobj.nopixelY_));
	imgobj.size_b_ /= (imgobj.nopixelX_ * imgobj.nopixelY_);
	imgobj.setnoElementArray();
	imgobj.InitArray();
	iRet = imgobj.setbinning();
	if (iRet != 0) {
		return iRet;
	}
	iRet = imgobj.setsubarray();
	if (iRet != 0) {
		return iRet;
	}

	iRet = imgobj.setexposuretime(exptime);
	if (iRet != 0) {
		return iRet;
	}

	//setOutputTrigger
	imgobj.setOutTrigger(outTriggerKind, outTrigDelay, outTrigPeriod);

	//setReadoutSpeed
	imgobj.setReadoutSpeed(readoutSpeedIdx);

	//setSensorMode
	imgobj.setSensorMode(sensorModeIdx);

	return iRet;
}

void run_acquisition(HDCAM hdcam, HDCAMWAIT hwait) {

	if (hdcam == NULL || hwait == NULL)
		return;

	OrcaJob orcajobobj;
	synchronobj.ptrOrcajobobj = &orcajobobj;

	orcajobobj.m_hdcam = hdcam;
	orcajobobj.m_hwait = hwait;
	orcajobobj.setter(&imgobj);

	std::cout << "from main thread start: " << std::this_thread::get_id() << std::endl;
	std::thread th([&]() {
		std::cout << "from child thread: " << std::this_thread::get_id() << std::endl;
		orcajobobj.run();
		});

	th.join();
	std::cout << "from main thread end: " << std::this_thread::get_id() << std::endl;

	// abort signal to dcamwait_start
	dcamwait_abort(hwait);
}

void run_acquisition(HDCAM hdcam, HDCAMWAIT hwait, JNIEnv* _env, jshortArray _outArray) {

	if (hdcam == NULL || hwait == NULL)
		return;

	OrcaJob orcajobobj;
	synchronobj.ptrOrcajobobj = &orcajobobj;

	orcajobobj.m_hdcam = hdcam;
	orcajobobj.m_hwait = hwait;
	orcajobobj.setter(&imgobj);
	orcajobobj.penv = &_env;
	orcajobobj.poutArray = &_outArray;

	std::cout << "from main thread start: " << std::this_thread::get_id() << std::endl;
	std::thread th([&]() {
		std::cout << "from child thread: " << std::this_thread::get_id() << std::endl;
		orcajobobj.run();
		});

	th.join();
	std::cout << "from main thread end: " << std::this_thread::get_id() << std::endl;

	// abort signal to dcamwait_start
	dcamwait_abort(hwait);



}

void sample_wait_single(HDCAM hdcam, HDCAMWAIT hwait) {

	// wait start param
	DCAMWAIT_START	waitstart;
	memset(&waitstart, 0, sizeof(waitstart));
	waitstart.size = sizeof(waitstart);
	waitstart.eventmask = DCAMWAIT_CAPEVENT_FRAMEREADY;
	waitstart.timeout = 1000;

	// prepare frame param
	DCAMBUF_FRAME	bufframe;
	memset(&bufframe, 0, sizeof(bufframe));
	bufframe.size = sizeof(bufframe);
	bufframe.iFrame = -1;				// latest frame

	DCAMERR err;

	// wait image
	err = dcamwait_start(hwait, &waitstart);
	if (failed(err))
	{
		std::cout << "dcamwait_start fail" << std::endl;
		assert((false) && "dcamwait_start fail");
	}

	// transferinfo param
	DCAMCAP_TRANSFERINFO captransferinfo;
	memset(&captransferinfo, 0, sizeof(captransferinfo));
	captransferinfo.size = sizeof(captransferinfo);

	// get number of captured image
	err = dcamcap_transferinfo(hdcam, &captransferinfo);
	if (failed(err))
	{
		std::cout << "dcamcap_transferinfo failed" << std::endl;
		assert((false) && "dcamcap_transferinfo failed");
	}

	if (captransferinfo.nFrameCount < 1)
	{
		std::cout << "not capture image" << std::endl;
		assert((false) && "not capture image");
	}
	else {
		//std::cout << "pass 1 after get number of capture imagecaptransferinfo.size: " << captransferinfo.size << ", captransferinfo.nFrameCount: " << captransferinfo.nFrameCount << ", captransferinfo.iKind: " << captransferinfo.iKind << ", nNewestFrameIndex: " << captransferinfo.nNewestFrameIndex << std::endl;
	}

	// access image
	err = dcambuf_lockframe(hdcam, &bufframe);
	if (failed(err))
	{
		std::cout << "dcambuf_lockframe failed" << std::endl;
		assert((false) && "dcambuf_lockframe failed");
	}

	//a frame has come; read buffer and JNI transfer
	int iRet = readBufferSingle(bufframe.buf, bufframe.rowbytes, bufframe.type, bufframe.width, bufframe.height);

	return;
}

int readBufferSingle(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height) {
	if (type != DCAM_PIXELTYPE_MONO16)
	{
		// not implement
		return -1;
	}

	int32 cx = imgobj.nopixelX_;
	int32 cy = imgobj.nopixelY_;

	// read from buffer
	const char* src = (const char*)buf;
	const unsigned short* s = (const unsigned short*)src;
	int32 x, y;

	for (y = 0; y < cy; y++)
	{
		for (x = 0; x < cx; x++)
		{
			*(imgobj.pImageArray1_ + (y * cx) + x) = *s++;
		}
	}

	/*
	double total = 0;
	int totala = cx * cy;
	std::cout << "total elem to be average: " << totala << std::endl;
	for (int i = 0; i < totala; i++) {
		total += *(imgobj.pImageArray1_ + i);
	}
	total = total / totala;
	std::cout << "average: " << total << std::endl;
	*/

	return 0;
}

int performAcquisition_infiniteLoop(HDCAM hdcam, HDCAMWAIT hwait, int totalframe, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {
	// wait start param
	DCAMWAIT_START	waitstart;
	memset(&waitstart, 0, sizeof(waitstart));
	waitstart.size = sizeof(waitstart);
	waitstart.eventmask = DCAMWAIT_CAPEVENT_FRAMEREADY;
	waitstart.timeout = 1000;

	// prepare frame param
	DCAMBUF_FRAME	bufframe;
	memset(&bufframe, 0, sizeof(bufframe));
	bufframe.size = sizeof(bufframe);
	bufframe.iFrame = -1;				// latest frame

	int32 i;
	int new_c;
	imgobj.framecounter = 0;
	for (i = 0; i < totalframe; i++)
	{
		if (imgobj.isStopPressed_ == true) {
			break;
		}

		new_c = i % imgobj.size_b_;

		DCAMERR err;

		// wait image
		err = dcamwait_start(hwait, &waitstart);
		if (failed(err))
		{
			assert((false) && "dcamwait_start fail");
		}

		// transferinfo param
		DCAMCAP_TRANSFERINFO captransferinfo;
		memset(&captransferinfo, 0, sizeof(captransferinfo));
		captransferinfo.size = sizeof(captransferinfo);

		// get number of captured image
		err = dcamcap_transferinfo(hdcam, &captransferinfo);
		if (failed(err))
		{
			assert((false) && "dcamcap_transferinfo failed");
		}

		if (captransferinfo.nFrameCount < 1)
		{
			assert((false) && "not capture image");
		}
		//else {
		//	//std::cout << "frame " << i << ", new_c: " << new_c << std::endl;
		//	//std::cout << "pass 1 after get number of capture imagecaptransferinfo.size: " << captransferinfo.size << ", captransferinfo.nFrameCount: " << captransferinfo.nFrameCount << ", captransferinfo.iKind: " << captransferinfo.iKind << ", nNewestFrameIndex: " << captransferinfo.nNewestFrameIndex << std::endl;
		//}

		// access image
		err = dcambuf_lockframe(hdcam, &bufframe);
		if (failed(err))
		{
			assert((false) && "dcambuf_lockframe failed");
		}

		//a frame has come; read buffer and JNI transfer
		int iRet = readBufferCpyJNI(bufframe.buf, bufframe.rowbytes, bufframe.type, bufframe.width, bufframe.height, new_c, _env, _outArray, framecounterObj, increment_id, carray, isCopy);
		imgobj.framecounter++;
	}

	return 0;

	
}

int readBufferCpyJNI(const void* buf, int32 rowbytes, DCAM_PIXELTYPE type, int32 width, int32 height, int new_c, JNIEnv* _env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {

	if (type != DCAM_PIXELTYPE_MONO16)
	{
		// not implement
		return -1;
	}

	int32 cx = imgobj.nopixelX_;
	int32 cy = imgobj.nopixelY_;
	int32 noelemperframe = cx * cy;
	int32 x, y;

	if (isCopy == true || imgobj.enableCriticalAccess == 0) {
		// read from buffer
		const char* src = (const char*)buf;
		//const unsigned short* s = (const unsigned short*)src;
		const jshort* s = (const jshort*)src;


		/*//UNUSED std::copy is faster
		for (y = 0; y < cy; y++)
		{
			for (x = 0; x < cx; x++)
			{
				*(imgobj.pImageArray1_ + (y * cx) + x) = *s++;
			}
		}
		*/

		//std::copy(s, s + noelemperframe, imgobj.pImageArray1_); 
		//_env->SetShortArrayRegion(_outArray, (new_c * noelemperframe), noelemperframe, imgobj.pImageArray1_);
		
		tk3.setTimeStart();
		_env->SetShortArrayRegion(_outArray, (new_c * noelemperframe), noelemperframe, s);
		_env->CallVoidMethod(framecounterObj, increment_id);
		imgobj.timeelapsed3 += tk3.getTimeElapsed();
	}
	else {
		const char* src = (const char*)buf;
		const jshort* s = (const jshort*)src;
		std::copy(s, s + noelemperframe, carray + (new_c * noelemperframe));
		_env->CallVoidMethod(framecounterObj, increment_id);
	}

	
	

	return 0;
}


JNIEXPORT jfloat JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_seyHello(JNIEnv* env, jclass cls, jint n1, jint n2) {
	jfloat result;
	result = ((jfloat)n1 + n2) / 2.0;
	return result;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_sayHello(JNIEnv* env, jobject thisObj) {
	std::cout << "hello from cpp" << std::endl;
	return 1;

}

JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_isHAMAconnectedSDK4(JNIEnv* env, jclass cls) {
	//0 = good to go; 1 = no camera; 2 = other error
	int iret = 0;

	// initialize DCAM-API
	memset(&imgobj.apiinit, 0, sizeof(imgobj.apiinit));
	imgobj.apiinit.size = sizeof(imgobj.apiinit);
	
	imgobj.err = dcamapi_init(&imgobj.apiinit);
	std::cout << "nDevice isHAMAconnectedSDK4: " << imgobj.apiinit.iDeviceCount << std::endl;
	if (failed(imgobj.err))
	{
		if (imgobj.err == DCAMERR_NOCAMERA) {
			iret = 1;
			dcamapi_uninit();
			return iret;
		}
		else {
			iret = 2;
			dcamapi_uninit();
			return iret;
		}

		
	}

	// finalize DCAM-API
	dcamapi_uninit();	// recommended call dcamapi_uninit() when dcamapi_init() is called even if it failed.
	return iret;
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_GetModelSDK4(JNIEnv* env, jclass cls) {
	//method gets called if there is at least one camera attached

	// initialize DCAM-API
	memset(&imgobj.apiinit, 0, sizeof(imgobj.apiinit));
	imgobj.apiinit.size = sizeof(imgobj.apiinit);

	imgobj.err = dcamapi_init(&imgobj.apiinit);
	

	int32 nDevice = imgobj.apiinit.iDeviceCount;
	int32 iDevice;

	std::cout << "nDevice GetModelSDK4: " << nDevice << std::endl;

	if (nDevice > 1) {
		iDevice = 1;
	}
	else {
		assert((nDevice == 1) && "err 36");
		iDevice = 0;
	}
	std::cout << "iDevice GetModelSDK4: " << iDevice << std::endl;

	char model[256];
	my_dcamdev_string(imgobj.err, (HDCAM)(intptr_t)iDevice, DCAM_IDSTR_MODEL, model, sizeof(model));
	// convert const char* to char*
	const char* outCStrcons = (const char*)model;

	// finalize DCAM-API
	dcamapi_uninit();	// recommended call dcamapi_uninit() when dcamapi_init() is called even if it failed.

	return (*env).NewStringUTF(outCStrcons);


	/*
	for (iDevice = 0; iDevice < nDevice; iDevice++) // only return info of first camera
	{
		char model[256];
		my_dcamdev_string(imgobj.err, (HDCAM)(intptr_t)iDevice, DCAM_IDSTR_MODEL, model, sizeof(model));
		// convert const char* to char*
		const char* outCStrcons = (const char*)model;

		// finalize DCAM-API
		dcamapi_uninit();	// recommended call dcamapi_uninit() when dcamapi_init() is called even if it failed.

		return (*env).NewStringUTF(outCStrcons);
	}
	*/
}

JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_InitializeHamaSDK4(JNIEnv* env, jclass cls) {

	// initialize DCAM-API and open device
	memset(&imgobj.apiinit, 0, sizeof(imgobj.apiinit));
	imgobj.apiinit.size = sizeof(imgobj.apiinit);

	imgobj.err = dcamapi_init(&imgobj.apiinit);
	if (failed(imgobj.err))
	{
		dcamapi_uninit();
		return 1;
	}

	int32 nDevice = imgobj.apiinit.iDeviceCount;

	std::cout << "nDevice InitializeHamaSDK4: " << nDevice << std::endl;

	if (nDevice > 1) {
		imgobj.iDevice = 1; //select second in line camera if > 1 camera found // umanager or HCImage will initialize first in line
	}
	else {
		assert((nDevice == 1) && "err 37");
		imgobj.iDevice = 0;
	}

	std::cout << "iDevice InitializeHamaSDK4: " << imgobj.iDevice << std::endl;

	// open specified camera
	memset(&imgobj.devopen, 0, sizeof(imgobj.devopen));
	imgobj.devopen.size = sizeof(imgobj.devopen);
	imgobj.devopen.index = imgobj.iDevice;
	imgobj.err = dcamdev_open(&imgobj.devopen);
	if (!failed(imgobj.err))
	{
		imgobj.hdcam = imgobj.devopen.hdcam;
		dcamcon_show_dcamdev_info(imgobj.hdcam);
	}
	else {
		dcamdev_close(imgobj.hdcam);
		dcamapi_uninit();
		return 1;
	}

	/*
	if (0 <= imgobj.iDevice && imgobj.iDevice < nDevice)
	{
		// open specified camera
		memset(&imgobj.devopen, 0, sizeof(imgobj.devopen));
		imgobj.devopen.size = sizeof(imgobj.devopen);
		imgobj.devopen.index = imgobj.iDevice;
		imgobj.err = dcamdev_open(&imgobj.devopen);
		if (!failed(imgobj.err))
		{
			imgobj.hdcam = imgobj.devopen.hdcam;
		}
		else {
			dcamdev_close(imgobj.hdcam);
			dcamapi_uninit();
			return 1;
		}
	}
	*/
	return 0;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_SystemShutDownSDK4(JNIEnv* env, jclass cls) {
	dcamdev_close(imgobj.hdcam);
	std::cout << "close hdcam handle" << std::endl;
	dcamapi_uninit();	// recommended call dcamapi_uninit() when dcamapi_init() is called even if it failed.
	std::cout << "uninit" << std::endl;
	return 0;
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_GetStringSDK4(JNIEnv* env, jclass cls, jstring inJNIStr) {//MODEL, CAMERAID, BUS
	// obtain C++ const char
	const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
	if (NULL == inCStr) return NULL;

	const std::string name = inCStr;
	char result[256];

	if (name == "MODEL") {
		std::cout << "cpp getting model" << std::endl;
		my_dcamdev_string(imgobj.err, (HDCAM)(intptr_t)imgobj.iDevice, DCAM_IDSTR_MODEL, result, sizeof(result));
	}
	else if (name == "CAMERAID") {
		std::cout << "cpp getting cameraid" << std::endl;
		my_dcamdev_string(imgobj.err, (HDCAM)(intptr_t)imgobj.iDevice, DCAM_IDSTR_CAMERAID, result, sizeof(result));
	}
	else if (name == "BUS") {
		std::cout << "getting bus" << std::endl;
		my_dcamdev_string(imgobj.err, (HDCAM)(intptr_t)imgobj.iDevice, DCAM_IDSTR_BUS, result, sizeof(result));
	}
	else {
		assert((false) && "get DCAM_IDSTR id not implemented");
	}

	const char* outCStrcons = (const char*)result;
	return (*env).NewStringUTF(outCStrcons);
}

JNIEXPORT jdouble JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_GetDoubleSDK4(JNIEnv* env, jclass cls, jstring inJNIStr) {
	//CONVERSIONFACTOR_COEFF, CONVERSIONFACTOR_OFFSET, BITSPERCHANNEL

	const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
	if (NULL == inCStr) return NULL;

	const std::string name = inCStr;
	double res;

	if (name == "CONVERSIONFACTOR_COEFF") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_CONVERSIONFACTOR_COEFF, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_CONVERSIONFACTOR_COEFF");
	}
	else if (name == "CONVERSIONFACTOR_OFFSET") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_CONVERSIONFACTOR_OFFSET, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_CONVERSIONFACTOR_OFFSET");
	}
	else if (name == "BITSPERCHANNEL") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_BITSPERCHANNEL, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_BITSPERCHANNEL");
	}
	else if (name == "OUTPUTTRIGGER_KIND") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_KIND, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_KIND");
	}
	else if (name == "OUTPUTTRIGGER_ACTIVE") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_ACTIVE, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_ACTIVE");
	}
	else if (name == "OUTPUTTRIGGER_SOURCE") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_SOURCE, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_SOURCE");
	}
	else if (name == "OUTPUTTRIGGER_POLARITY") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_POLARITY, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_POLARITY");
	}
	else if (name == "OUTPUTTRIGGER_DELAY") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_DELAY, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_DELAY");
	}
	else if (name == "OUTPUTTRIGGER_PERIOD") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_OUTPUTTRIGGER_PERIOD, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_OUTPUTTRIGGER_PERIOD");
	}
	else if (name == "INTERNALLINESPEED") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_INTERNALLINESPEED, &res);
		if (failed(imgobj.err)) {
			res = 0;
		}
	}
	else if (name == "INTERNAL_LINEINTERVAL") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_INTERNAL_LINEINTERVAL, &res);
		if (failed(imgobj.err)) {
			res = 0;
		}
	}
	else if (name == "TIMING_READOUTTIME") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_TIMING_READOUTTIME, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_TIMING_READOUTTIME");
	}
	else if (name == "TIMING_GLOBALEXPOSUREDELAY") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_TIMING_GLOBALEXPOSUREDELAY, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_TIMING_GLOBALEXPOSUREDELAY");
	}
	else if (name == "EXPOSURETIME") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_EXPOSURETIME, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_EXPOSURETIME");
	}
	else if (name == "READOUTSPEED") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_READOUTSPEED, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalue DCAM_IDPROP_READOUTSPEED");
	}
	else if (name == "SENSORMODE") {
		imgobj.err = dcamprop_getvalue(imgobj.hdcam, DCAM_IDPROP_SENSORMODE, &res);
		assert((!failed(imgobj.err)) && "error dcamprop_getvalueDCAM_IDPROP_SENSORMODE");
	}
	else {
		assert((false) && "GetDoubleSDK4 unmatched argument");
	}

	return res;

}

JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_getIntegerSDK4(JNIEnv* env, jclass cls, jstring inJNIStr) {
	//WIDTH, HEIGHT, TOP, LEFT, BIN
	const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
	if (NULL == inCStr) return NULL;

	const std::string name = inCStr;

	if (name == "WIDTH") {
		return imgobj.nopixelX_;
	}
	else if (name == "HEIGHT") {
		return imgobj.nopixelY_;
	}
	else if (name == "TOP") {
		return imgobj.vstart + 1;
	}
	else if (name == "LEFT") {
		return imgobj.hstart + 1;
	}
	else if (name == "BIN") {
		return imgobj.hbin;
	}
	else {
		return NULL;
		assert((false) && "getIntegerSDK4 unmatched argument");
	}
}

JNIEXPORT jintArray JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_getDetectorDimensionSDK4(JNIEnv* env, jclass cls) {

	int* res = new int[2];
	float* imw = dcamcon_ret_propertyattr_range(4325904);//ImageWidth
	float* imh = dcamcon_ret_propertyattr_range(4325920);//ImageHeight
	res[0] = (int) imw[1];
	res[1] = (int) imh[1];
	delete[] imw;
	delete[] imh;

	jint outCArray[] = { 0,0 };
	outCArray[0] = res[0];
	outCArray[1] = res[1];
	delete[] res;

	jintArray outArray = env->NewIntArray(2);
	if (NULL == outArray) return NULL;
	env->SetIntArrayRegion(outArray, 0, 2, outCArray);

	return outArray;
}

JNIEXPORT jfloatArray JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_getChipSizeSDK4(JNIEnv* env, jclass cls) {

	float* res = new float[2];
	float* imw = dcamcon_ret_propertyattr_range(4327440);//ChipSizeX
	float* imh = dcamcon_ret_propertyattr_range(4327456);//ChipSizeY
	res[0] = imw[0];
	res[1] = imh[0];
	delete[] imw;
	delete[] imh;

	jfloat outCArray[] = { 0,0 };
	outCArray[0] = res[0];
	outCArray[1] = res[1];
	delete[] res;

	jfloatArray outArray = env->NewFloatArray(2);
	if (NULL == outArray) return NULL;
	env->SetFloatArrayRegion(outArray, 0, 2, outCArray);
	
	return outArray;
}


JNIEXPORT jint JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_setParameterSDK4(JNIEnv* env, jclass cls, jdouble exposureTime, jint Width, jint Height, jint Left, jint Top, jint Incamerabin, jint acqmode, jint totalframe, jint size_b, jint arraysize, jint ouTriggerKind, jdouble outDelay, jdouble outPeriod, jint readoutSpeedIdx, jint sensorModeIdx) {
	int iRet;
	iRet = setParameter(exposureTime, Width, Height, Left, Top, Incamerabin, acqmode, totalframe, size_b, arraysize, ouTriggerKind, outDelay, outPeriod, readoutSpeedIdx, sensorModeIdx);
	return iRet;
}


JNIEXPORT jdouble JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_getKineticCycleSDK4(JNIEnv* env, jclass cls) {
	return imgobj.getframerate();
}


JNIEXPORT jshortArray JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_runSingleScanSDK4(JNIEnv* env, jclass cls) {

	// open wait handle
	DCAMWAIT_OPEN	waitopen;
	memset(&waitopen, 0, sizeof(waitopen));
	waitopen.size = sizeof(waitopen);
	waitopen.hdcam = imgobj.hdcam;

	imgobj.err = dcamwait_open(&waitopen);
	if (failed(imgobj.err))
	{
		std::cout << "error dcamwait_open" << std::endl;
		assert((false) && "error dcamwait_open");
	}
	else {
		HDCAMWAIT hwait = waitopen.hwait;

		// allocate buffer
		int32 number_of_buffer = 10; //TODO: optimize
		imgobj.err = dcambuf_alloc(imgobj.hdcam, number_of_buffer);
		if (failed(imgobj.err))
		{
			std::cout << "error dcambuf_alloc" << std::endl;
			assert((false) && "error dcambuf_alloc");
		}
		else {
			// start capture
			imgobj.err = dcamcap_start(imgobj.hdcam, DCAMCAP_START_SEQUENCE);
			if (failed(imgobj.err))
			{
				std::cout << "error dcamcap_start" << std::endl;
				assert((false) && "error dcamcap_start");
			}
			else {

				// stop to capture on the specified number
				sample_wait_single(imgobj.hdcam, hwait);

				// abort signal to dcamwait_start
				dcamwait_abort(hwait);

				// stop capture
				dcamcap_stop(imgobj.hdcam);
			}

			// release buffer
			dcambuf_release(imgobj.hdcam);

		}

		// close wait handle
		dcamwait_close(hwait);
		std::cout << "close hwait handle" << std::endl;
	}

	jshortArray outArray;
	outArray = env->NewShortArray(imgobj.arraysize_);
	env->SetShortArrayRegion(outArray, 0, imgobj.arraysize_, imgobj.pImageArray1_);
	imgobj.freeImArray();
	return outArray;
}

JNIEXPORT void JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_runInfiniteLoopSDK4(JNIEnv* env, jclass cls, jshortArray outArray, jobject framecounterObj) {

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

	// open wait handle
	DCAMWAIT_OPEN	waitopen;
	memset(&waitopen, 0, sizeof(waitopen));
	waitopen.size = sizeof(waitopen);
	waitopen.hdcam = imgobj.hdcam;

	imgobj.err = dcamwait_open(&waitopen);
	if (failed(imgobj.err))
	{
		std::cout << "error dcamwait_open" << std::endl;
		assert((false) && "error dcamwait_open");
	}
	else {
		HDCAMWAIT hwait = waitopen.hwait;

		// allocate buffer
		int32 number_of_buffer = 10; //TODO: optimize
		imgobj.err = dcambuf_alloc(imgobj.hdcam, number_of_buffer);
		if (failed(imgobj.err))
		{
			std::cout << "error dcambuf_alloc" << std::endl;
			assert((false) && "error dcambuf_alloc");
		}
		else {
			// start capture
			imgobj.err = dcamcap_start(imgobj.hdcam, DCAMCAP_START_SEQUENCE);
			if (failed(imgobj.err))
			{
				std::cout << "error dcamcap_start" << std::endl;
				assert((false) && "error dcamcap_start");
			}
			else {
	
				// stop to capture on the specified number
				tk2.setTimeStart();
				int iret = performAcquisition_infiniteLoop(imgobj.hdcam, hwait, imgobj.totalframe_, env, outArray, framecounterObj, increment_id, carray, isCopy);
				imgobj.timeelapsed2 += tk2.getTimeElapsed();

				// abort signal to dcamwait_start
				dcamwait_abort(hwait);

				// stop capture
				dcamcap_stop(imgobj.hdcam);

			}

			// release buffer
			dcambuf_release(imgobj.hdcam);

		}

		// close wait handle
		dcamwait_close(hwait);

		imgobj.freeImArray();
	}


	imgobj.timeelapsed1 = tk1.getTimeElapsed(); //full
	imgobj.timeelapsed2 /= imgobj.framecounter; // average read buffer and copy
	imgobj.timeelapsed3 /= imgobj.framecounter;//native heap to java heap

	env->SetDoubleField(framecounterObj, time1_id, imgobj.timeelapsed1);
	env->SetDoubleField(framecounterObj, time2_id, imgobj.timeelapsed2);
	env->SetDoubleField(framecounterObj, time3_id, imgobj.timeelapsed3);


	if (isCopy == false && imgobj.enableCriticalAccess == 1) {
		env->ReleasePrimitiveArrayCritical(outArray, carray, 0);
	}

}

JNIEXPORT void JNICALL Java_directCameraReadout_hamadcamsdk4_Hamamatsu_1DCAM_1SDK4_setStopMechanismSDK4(JNIEnv* env, jclass cls, jboolean isstoppressed) {
	imgobj.isStopPressed_ = isstoppressed;
	return;
}
