#include <iostream>
#include <assert.h>
#include "ImageClsSDK4.h"


ImageSDK4::ImageSDK4() {
	std::cout << "constructor imgcls" << std::endl;
}

ImageSDK4::~ImageSDK4() {
	std::cout << "destructor imgcls iDevice: " << iDevice << std::endl;
}

// Select ROI for acquisition
void ImageSDK4::setPixelDimParam(int w, int h, int l, int t, int incamerabin) {
	this->hbin = incamerabin;
	this->vbin = incamerabin;
	this->hstart = (l - 1) * hbin;
	this->hend = hstart + (w * hbin) - 1;
	this->vstart = (t - 1) * vbin;
	this->vend = vstart + (h * vbin) - 1;
	assert((hbin == vbin) && "error 23; Hamamatsu only support in camera binning of equal dimension");
}

void ImageSDK4::getnoPixelXY() {
	nopixelX_ = (hend - hstart + 1) / hbin;
	nopixelY_ = (vend - vstart + 1) / vbin;
}

void ImageSDK4::setnoElementArray() {
	if (acqmode_ == 1) {
		arraysize_ = nopixelX_ * nopixelY_;
	}
	if (acqmode_ == 2 || acqmode_ == 3) {
		arraysize_ = nopixelX_ * nopixelY_;//Alternatively I could also set arraysize_ =  nopixelX_ * nopixelY_ * size_b_
	}
}

void ImageSDK4::InitArray() {
	pImageArray1_ = (short*)malloc(arraysize_ * sizeof(short));
}

void ImageSDK4::freeImArray() {
	if (pImageArray1_) {
		free(pImageArray1_);
		pImageArray1_ = NULL;
	}
}

int ImageSDK4::setbinning() {

	DCAMERR err;
	// check whether the camera supports or not. (refer sample program propertylist to get the list of support values)
	err = dcamprop_queryvalue(hdcam, DCAM_IDPROP_BINNING, &hbin);
	if (failed(err))
	{
		std::cout << "error dcamprop_queryvalue DCAM_IDPROP_BINNING" << std::endl;
		return 1;
	}
	assert((failed(err) == 0) && "error dcamprop_queryvalue DCAM_IDPROP_BINNING");

	// set binning value to the camera
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_BINNING, hbin);
	if (failed(err))
	{
		std::cout << "error dcamprop_setvalue DCAM_IDPROP_BINNING" << std::endl;
		return 1;
	}
	assert((failed(err) == 0) && "error dcamprop_setvalue DCAM_IDPROP_BINNING");

	return 0;
}

int ImageSDK4::setsubarray() {

	DCAMERR err;

	// set subarray mode off. This setting is not mandatory, but you have to control the setting order of offset and size when mode is on. 
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYMODE, DCAMPROP_MODE__OFF);
	if (failed(err))
	{
		std::cout << "error DCAM_IDPROP_SUBARRAYMODE  DCAMPROP_MODE__OFF" << std::endl;
		assert((false) && "error DCAM_IDPROP_SUBARRAYMODE  DCAMPROP_MODE__OFF");
		return 1;
	}
	

	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYHPOS, hstart);
	if (failed(err))
	{
		std::cout << "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYHPOS" << std::endl;
		assert((false) && "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYHPOS");
		return 1;
	}
	

	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYHSIZE, (hend - hstart + 1));
	if (failed(err))
	{
		std::cout << "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYHSIZE" << std::endl;
		assert((false) && "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYHSIZE");
		return 1;
	}
	
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYVPOS, vstart);
	if (failed(err))
	{
		std::cout << "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYVPOS" << std::endl;
		assert((false) && "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYVPOS");
		return 1;
	}
	
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYVSIZE, (vend - vstart + 1));
	if (failed(err))
	{
		std::cout << "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYVSIZE" << std::endl;
		assert((false) && "error dcamprop_setvalue DCAM_IDPROP_SUBARRAYVSIZE");
		return 1;
	}
	

	// set subarray mode on. The combination of offset and size is checked on this timing.
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_SUBARRAYMODE, DCAMPROP_MODE__ON);
	if (failed(err))
	{
		std::cout << "error DCAM_IDPROP_SUBARRAYMODE, DCAMPROP_MODE__ON" << std::endl;
		assert((false) && "error DCAM_IDPROP_SUBARRAYMODE, DCAMPROP_MODE__ON");
		return 1;
	}
	

	return 0;
}

int ImageSDK4::setexposuretime(double exptime) {
	exposureTime_ = exptime;
	DCAMERR err;
	err = dcamprop_setvalue(hdcam, DCAM_IDPROP_EXPOSURETIME, exposureTime_);
	if (failed(err))
	{
		std::cout << "error setvalue DCAM_IDPROP_EXPOSURETIME" << std::endl;
		assert((false) && "error setvalue DCAM_IDPROP_EXPOSURETIME");
		return 1;
	}

	return 0;
}

double ImageSDK4::getframerate() {
	double res;
	err = dcamprop_getvalue(hdcam, DCAM_IDPROP_INTERNALFRAMERATE, &res);
	if (failed(err))
	{
		std::cout << "error dcamprop_getvalue DCAM_IDPROP_INTERNALFRAMERATE" << std::endl;
		assert((false) && "error dcamprop_getvalue DCAM_IDPROP_INTERNALFRAMERATE");
		return 1;
	}
	return res;
}

void ImageSDK4::setOutTrigger(int outTriggerKind, double outTrigDelay, double outTrigPeriod) {

	outTriggerKind_ = outTriggerKind;
	outTriggerDelay_ = outTrigDelay;
	outTriggerPeriod_ = outTrigPeriod;

	if (outTriggerKind_ == 0) {
		////////////////////////////////////////////////////////////////////////////////////////////////////
		//DISABLED
		////////////////////////////////////////////////////////////////////////////////////////////////////
		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_KIND, DCAMPROP_OUTPUTTRIGGER_KIND__LOW);
		if (failed(err))
		{
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_KIND__LOW");
			assert((false) && "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_KIND__LOW");
		}
		return;
	}

	if (outTriggerKind_ == 1) { //Programmable rear-end output timing
		////////////////////////////////////////////////////////////////////////////////////////////////////
		//REAREND
		////////////////////////////////////////////////////////////////////////////////////////////////////
		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_KIND, DCAMPROP_OUTPUTTRIGGER_KIND__PROGRAMABLE);
		if (failed(err))
		{
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_KIND__PROGRAMABLE");
			assert((false) && "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_KIND__PROGRAMABLE");
		}

		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_ACTIVE, DCAMPROP_OUTPUTTRIGGER_ACTIVE__EDGE);
		if (failed(err))
		{
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_ACTIVE__EDGE");
			assert((false) && "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_ACTIVE__EDGE");
		}

		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_SOURCE, DCAMPROP_OUTPUTTRIGGER_SOURCE__READOUTEND);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_SOURCE__READOUTEND");
			assert((false) && "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_SOURCE__READOUTEND");
		}

		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_POLARITY, DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE");
			assert((false) && "dcamprop_setvalue DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE");
		}

		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_DELAY, outTriggerDelay_);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAM_IDPROP_OUTPUTTRIGGER_DELAY");
			assert((false) && "dcamprop_setvalue DCAM_IDPROP_OUTPUTTRIGGER_DELAY");
		}

		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_PERIOD, outTriggerPeriod_);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcamprop_setvalue DCAM_IDPROP_OUTPUTTRIGGER_PERIOD");
			assert((false) && "dcamprop_setvalue DCAM_IDPROP_OUTPUTTRIGGER_PERIOD");
		}
	}
	else if (outTriggerKind_ == 2) {
		////////////////////////////////////////////////////////////////////////////////////////////////////
		//GLOBAL
		////////////////////////////////////////////////////////////////////////////////////////////////////
		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_KIND, DCAMPROP_OUTPUTTRIGGER_KIND__EXPOSURE);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcam set DCAMPROP_OUTPUTTRIGGER_KIND__EXPOSURE");
			assert((false) && "dcamprop_setvalueDCAMPROP_OUTPUTTRIGGER_KIND__EXPOSURE");
		}
	
		err = dcamprop_setvalue(hdcam, DCAM_IDPROP_OUTPUTTRIGGER_POLARITY, DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE);
		if (failed(err)) {
			dcamcon_show_dcamerr(hdcam, err, "dcam set DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE");
			assert((false) && "dcam set DCAMPROP_OUTPUTTRIGGER_POLARITY__POSITIVE");
		}

	}
}

void ImageSDK4::reset() {
	isStopPressed_ = false;
}