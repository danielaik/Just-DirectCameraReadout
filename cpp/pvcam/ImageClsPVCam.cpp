#include <iostream>
#include <assert.h>
#include "ImageClsPVCam.h"
#include "version.h"


ImagePVCam::ImagePVCam() {
    std::cout << "ImagePVCam constructor" << std::endl << std::endl;

}

ImagePVCam::~ImagePVCam() {
    std::cout << "ImagePVCam destructor" << std::endl << std::endl;
}






int ImagePVCam::isCameraConnected() {
    int iret = 0;

    iret = InitPVCAM();
    if (iret != 0)
    {
        CloseCameraAndUninit();
        return iret;
    }

    iret = GetCameraInfos();
    if (iret != 0)
    {
        CloseCameraAndUninit();
        return iret;
    }

    CloseCameraAndUninit();
    return iret;

}

int ImagePVCam::initSystem() {

    if (!InitAndOpenFirstCamera()) {
        assert((false) && "failed initSystem*()");
        return 1;
    }
    else {
        return 0;
    }
}

int ImagePVCam::uninitSystem() {
    CloseCameraAndUninit();
    return 0;
}

int* ImagePVCam::getchipSize() {
    // Get number of sensor columns
    if (PV_OK != pl_get_param(g_hCam, PARAM_SER_SIZE, ATTR_CURRENT,
        (void*)&g_SensorResX))
    {
        PrintErrorMessage(pl_error_code(), "Couldn't read CCD X-resolution");
        assert((false) && "Couldn't read CCD X-resolution");
    }
    // Get number of sensor lines
    if (PV_OK != pl_get_param(g_hCam, PARAM_PAR_SIZE, ATTR_CURRENT,
        (void*)&g_SensorResY))
    {
        PrintErrorMessage(pl_error_code(), "Couldn't read CCD Y-resolution");
        assert((false) && "Couldn't read CCD Y-resolution");
    }
    int res[2];
    res[0] = (int)g_SensorResX;
    res[1] = (int)g_SensorResY;

    return res;
}

char* ImagePVCam::getChipName() {
    return chipName;
}

char* ImagePVCam::getCameraName() {
    return g_Camera0_Name;
}

void ImagePVCam::setPixelDimParam(int w, int h, int l, int t, int incamerabin) {
    this->hbin = incamerabin;
    this->vbin = incamerabin;
    this->hstart = (l - 1) * hbin;
    this->hend = hstart + (w * hbin) - 1;
    this->vstart = (t - 1) * vbin;
    this->vend = vstart + (h * vbin) - 1;
    assert((hbin == vbin) && "error 23; Photometrics only support in camera binning of equal dimension");
    //std::cout << "inside setPixelDimParam hbin: " << hbin << ", vbin: " << vbin << ", hstart: " << hstart << ", hend:  " << hend << ", vstart: " << vstart << ", vend: " << vend << std::endl;
}

void ImagePVCam::setnoElementArray() {
    if (acqmode_ == 1) {
        arraysize_ = nopixelX_ * nopixelY_;
    }
    if (acqmode_ == 2 || acqmode_ == 3) {
        arraysize_ = nopixelX_ * nopixelY_;//Alternatively I could also set arraysize_ =  nopixelX_ * nopixelY_ * size_b_
    }
    //std::cout << "cpp arraysize: " << arraysize_ << std::endl;
}

void ImagePVCam::getnoPixelXY() {
    nopixelX_ = (hend - hstart + 1) / hbin;
    nopixelY_ = (vend - vstart + 1) / vbin;
    //std::cout << "inside getnoPixelXY x: " << nopixelX_ << ", y: " << nopixelY_ << std::endl;
}

void ImagePVCam::reset() {
    g_EofFlag = false;
    isStopPressed_ = false;
    frameTime_ = 0;
    timeelapsed1 = 0;
    timeelapsed2 = 0;
    timeelapsed3 = 0;
    timeelapsed4 = 0;
    timeelapsed5 = 0;
}

void ImagePVCam::InitArray() {
    
    if (acqmode_ == 1) {
        pImageArray_ = (short*)malloc(arraysize_ * sizeof(short));
        if (pImageArray_ == NULL)
        {
            CloseCameraAndUninit();
            assert((false) && "unable to allocate memory uns16");
        }

        pImageArrayShort_ = (short*)malloc(arraysize_ * sizeof(short));
        if (pImageArrayShort_ == NULL)
        {
            CloseCameraAndUninit();
            assert((false) && "unable to allocate memory short");
        }
    }
    else if (acqmode_ == 2) {
        buffSize_ = arraysize_ * circBufferFrames_; // in pixel
        pImageCircularBuffer_ = (uns16*)malloc(buffSize_ * sizeof(uns16));
        if (pImageCircularBuffer_ == NULL)
        {
            CloseCameraAndUninit();
            assert((false) && "unable to allocate circular buffer memory");
        }

        pImageArrayShort_ = (short*)malloc(arraysize_ * sizeof(short));
        if (pImageArrayShort_ == NULL)
        {
            CloseCameraAndUninit();
            assert((false) && "unable to allocate memory short");
        }
    }
    else {
        assert((false) && "err 66 acqmode_ neihter setted to 1 or 2");
    }
    
}

void ImagePVCam::freeImArray() {

    if (acqmode_ == 1) {
        if (pImageArray_) {
            free(pImageArray_);
            pImageArray_ = NULL;
        }

        if (pImageArrayShort_) {
            free(pImageArrayShort_);
            pImageArrayShort_ = NULL;
        }
    }
    else if (acqmode_ == 2) {
        if (pImageCircularBuffer_) {
            free(pImageCircularBuffer_);
            pImageCircularBuffer_ = NULL;
        }

        if (pImageArrayShort_) {
            free(pImageArrayShort_);
            pImageArrayShort_ = NULL;
        }
    }
    else {
        assert((false) && "err 67 acqmode_ neihter setted to 1 or 2");
    }
    
}

int ImagePVCam::setAOI() {
    int iret = 0;

    // Set the CCD g_Region fo Full Frame, serial and parallel binning is set to 1
    g_Region.s1 = hstart;
    g_Region.s2 = hend;
    g_Region.sbin = hbin;
    g_Region.p1 = vstart;
    g_Region.p2 = vend;
    g_Region.pbin = vbin;

    return iret;
}

void ImagePVCam::setupSequence() {

    if (acqmode_ == 1) {
        // Setup the acquisition
        // FramesToAcquire specifies number of frames to acquire in this sequence.
        // exposureBytes will now hold the size of the whole buffer for all the
        // images in the sequence.
        if (PV_OK != pl_exp_setup_seq(g_hCam, totalframe_, 1, &g_Region, TIMED_MODE,
            exposureTime_, &exposureBytes_))
        {
            CloseCameraAndUninit();
            assert((false) && "error 32 setupSequence");
        }

        //// Calculate size of each frame
        //oneFrameBytes_ = exposureBytes_ / totalframe_;
        //std::cout << "framesToAcquire: " << totalframe_ << " frames" << std::endl;
        //std::cout << "exposureBytes: " << exposureBytes_ << " bytes" << std::endl;
        //std::cout << "size of each frames: " << oneFrameBytes_ << " bytes" << std::endl;

        assert(((exposureBytes_ / sizeof(uns16)) == arraysize_) && "error 32 setupSequence unmatched arraysize_");
    }
    else if (acqmode_ == 2) {
        dataContext.myData1 = 0;
        dataContext.myData2 = totalframe_;

        // Register callback to receive a notification when EOF event arrives.
        // The NewFrameHandler function will be called by PVCAM, last parameter
        // might hold a pointer to user-specific content that will be passed with
        // the callback once it is invoked.
        if (PV_OK != pl_cam_register_callback_ex3(g_hCam, PL_CALLBACK_EOF, (void*)NewFrameHandler, (void*)&dataContext))
        {
            //PrintErrorMessage(pl_error_code(), "pl_cam_register_callback() error");
            CloseCameraAndUninit();
            assert((false) && "error 33 register_callback");
        }

        // Setup continuous acquisition with circular buffer mode. TIMED_MODE
        // indicates this is software trigger mode and each acquisition is after
        // initial pl_exp_start_cont() call started internally from the camera.
        // To run in hardware trigger mode use either STROBED_MODE, BULB_MODE or
        // TRIGGER_FIRST_MODE.
        if (PV_OK != pl_exp_setup_cont(g_hCam, 1, &g_Region, TIMED_MODE,
            exposureTime_, &exposureBytes_, bufferMode_))
        {
            //PrintErrorMessage(pl_error_code(), "pl_exp_setup_cont() error");
            CloseCameraAndUninit();
            assert((false) && "error 34 setupSequence");
        }

        assert(((exposureBytes_ / sizeof(uns16)) == arraysize_) && "error 32 setupSequence unmatched arraysize_");
    }
    else {
        assert((false) && "acqmode_ neihter setted to 1 or 2");
    }
    

}


