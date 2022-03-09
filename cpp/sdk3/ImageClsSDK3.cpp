#include <iostream>
#include "ImageClsSDK3.h"

ImageSDK3::ImageSDK3() {
	pImageArraySingle = NULL;
	pImageArrayInf = NULL;
	acqBuffer = NULL;
	acqBuffer1 = NULL;
	acqBuffer2 = NULL;
	acqBuffer3 = NULL;
	acqBuffer4 = NULL;
	acqBuffer5 = NULL;
	acqBuffer6 = NULL;
	acqBuffer7 = NULL;
	acqBuffer8 = NULL;
	acqBuffer9 = NULL;
	acqBuffer10 = NULL;
	acqBuffer11 = NULL;
	acqBuffer12 = NULL;
	acqBuffer13 = NULL;
	acqBuffer14 = NULL;
	acqBuffer15 = NULL;
	acqBuffer16 = NULL;
	acqBuffer17 = NULL;
	acqBuffer18 = NULL;
	acqBuffer19 = NULL;
	acqBuffer20 = NULL;
}

ImageSDK3::~ImageSDK3() {
}

void ImageSDK3::setArraySizeSingle() {
	ArraySizeSingle = iAOIHeight * iAOIWidth;
}

void ImageSDK3::InitArraySingle() {
	if (pImageArraySingle == NULL) {
		pImageArraySingle = (short*)malloc(ArraySizeSingle * sizeof(short));
	}
}

void ImageSDK3::freeImArraySingle() {
	if (pImageArraySingle) {
		free(pImageArraySingle);
		pImageArraySingle = NULL;
	}
}

void ImageSDK3::setArraySizeInf() { 
	ArraySizeInf = iAOIHeight * iAOIWidth * (long long)intervalFrame;
}

void ImageSDK3::freeImArrayInf() { 
	if (pImageArrayInf != NULL) {
		free(pImageArrayInf);
		pImageArrayInf = NULL;
	}
}

void ImageSDK3::InitArrayInf() {
	if (pImageArrayInf == NULL) {
		pImageArrayInf = (short*)malloc(ArraySizeInf * sizeof(short));
	}
}