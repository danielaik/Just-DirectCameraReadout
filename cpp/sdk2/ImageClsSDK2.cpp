#include <iostream>
#include "ImageClsSDK2.h"

ImageSDK2::ImageSDK2() {
}


ImageSDK2::~ImageSDK2() {
}

// setting exposure time (not frametime)
void ImageSDK2::setExposureTime(float value) {
	exposureTime_ = value;
}

// Select ROI for acquisition
void ImageSDK2::setPixelDimParam(int w, int h, int l, int t, int incamerabin) {
	this->hbin = incamerabin;
	this->vbin = incamerabin;
	this->hstart = (l - 1) * hbin + 1;
	this->hend = hstart + (w * hbin) - 1;
	this->vstart = (t - 1) * vbin + 1;
	this->vend = vstart + (h * vbin) - 1;
}

void ImageSDK2::setCropModePixelDimParam(int cWidth, int cHeight, int cLeft, int cTop) {
	this->cLeft = cLeft;
	this->cTop = cTop;
	this->cWidth = cWidth - (cWidth % hbin);
	this->cHeight = cHeight - (cHeight % vbin);
}

void ImageSDK2::getnoPixelXY() {

	if (isCropMode == 1) {
		nopixelX_ = cWidth / hbin;
		nopixelY_ = cHeight / vbin;
	}
	else {
		nopixelX_ = (hend - hstart + 1) / hbin;
		nopixelY_ = (vend - vstart + 1) / vbin;
	}

}

void ImageSDK2::setnoElementArray() {
	if (acquisitionMode == 1) {
		arraysize_ = nopixelX_ * nopixelY_;
	}
	if (acquisitionMode == 5) {
		arraysize_ = nopixelX_ * nopixelY_; //arraysize_ = nopixelX_ * nopixelY_ * size_b_
	}
}

void ImageSDK2::InitArraySingle() {
	pImageArray1_ = (long*)malloc(arraysize_ * sizeof(long));
	pImageArrayBuf1_ = (short*)malloc(arraysize_ * sizeof(short));
}

void ImageSDK2::setSize_b(int arrsize) {
	this->temparraysize = arrsize;
	this->size_b_ = arrsize - (arrsize % (nopixelX_ * nopixelY_));
	this->size_b_ /= (nopixelX_ * nopixelY_);
}

void ImageSDK2::setframeInterval(int frameInterval) {
	frameTransferInterval_ = frameInterval;
}

void ImageSDK2::setKinetic(float value) {
	kinetic_ = value;
}

void ImageSDK2::InitArrayInf() {
	pImageArray_ = (long*)malloc(arraysize_ * sizeof(long));
	pImageArrayBuf_ = (short*)malloc(arraysize_ * sizeof(short));
}

void ImageSDK2::InitArrayInfV2(int size) {
	if (pImageArray_ && pImageArrayBuf_) {
		free(pImageArray_);
		free(pImageArrayBuf_);
		pImageArrayBuf_ = NULL;
		pImageArray_ = NULL;
	}
	pImageArray_ = (long*)malloc(size * sizeof(long));
	pImageArrayBuf_ = (short*)malloc(size * sizeof(short));
}


bool ImageSDK2::isPointerNull() {
	return (!pImageArray_) ? true : false;
}

// stop mechanism
void ImageSDK2::SetIsStopPressed(bool somebol) {
	isStopPressed = somebol;
}

bool ImageSDK2::GetIsStopPressed() {
	return isStopPressed;
}

