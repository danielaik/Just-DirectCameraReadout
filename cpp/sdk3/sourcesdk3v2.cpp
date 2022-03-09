#ifdef _MSC_VER
#define _CRT_SECURE_NO_WARNINGS

#include <jni.h>
#include <iostream>
#include <string>
#include <exception>
#include <codecvt>
#include <assert.h>
#include "atcore.h"
#include "ImageClsSDK3.h"
#include "directCameraReadout_andorsdk3v2_AndorSDK3v2.h"
#include "TimeKeeper.h"

#define EXTRACTLOWPACKED(SourcePtr) ( (SourcePtr[0] << 4) + (SourcePtr[1] & 0xF) )
#define EXTRACTHIGHPACKED(SourcePtr) ( (SourcePtr[2] << 4) + (SourcePtr[1] >> 4) )


/* /////////////////////////////////////


List of Sona enum feature:
AOIBinning, AOILayout, AuxiliaryOutSource, AuxOutSourceTwo, BitDepth,
CycleMode, ElectronicShutteringMode, EventSelector, FanSpeed, FrameGenMode,
GainMode, IOSelector, PixelEncoding, PixelReadoutRate, ShutterMode, SimplePreAmpGainControl,
TemperatureControl, TemperatureStatus, TriggerMode, TriggerSource

List of SOna int feature:
AOIHBin, AOIWidth, AOILeft, AOIVBin, AOIHeight, AOITop, AOIStride, Baseline,
BufferOverflowEvent, CameraImagesWaiting, EventsMissedEvent, ExposureEndEvent, ExposureStartEvent,
FrameGenFixedPixelValue, FrameCount, ImageSizeBytes, MultitrackCount, MultitrackEnd, MultitrackSelector, MultitrackStart,
RowNExposureStartEvenet, RowNExposureEndEvenet, SensorHeight, SensorWidth, TimestampClock, TimestampClockFrequency

List of SOna float feature:
BytesPerPixel, ExposureTime, FrameRate, MaxInterfaceTransferRate, PixelHeight, PixelWidth, ReadoutTime, RowReadTime,
SensorTemperature, ShutterTransferTime, TargetSensorTemperature,

List of Sona boolean featuer:
CameraAcquiring, CameraPresent, EventEnable, FullAOIControl, IOInvert, MetadataEnable, MetadataFrame, MetadataTimestamp, MultitrackBinned, MultitrackCount,
Overlap, SensorCooling, SpuriousNoiseFilter, VerticallyCentreAOI

List of Sona String feature:
CameraFamily, CameraModel, CameraName, FirmwareVersion, InterfaceType, SerialNumber


*/ /////////////////////////////////////

ImageSDK3 imgobj;
TimeKeeper tk1;
TimeKeeper tk2;
TimeKeeper tk3;

// prototyle methods
int isSCMOSconnected(); // V
void InitializeSystem(); // V
AT_WC* getEnumString(const wchar_t* feature);  // return enumerated string value
AT_WC* getStringVal(const wchar_t* feature);
void setParamSingle();
void resetImageClsparam();
void extract2from3single(unsigned char* _buffer, short* _i_returns, int _nopixperframe);
void extractMono12or16single(unsigned char* _buffer, short* _i_returns, int _nopixperframe);
void setParamInfiniteLoop();
int performAcquisition_infiniteLoop(int _handle, int _numberAcquisitions, int _intervalFrame, JNIEnv* env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);
void createBuffers(int _handle);
unsigned char* QueueBuffer(int _handle);
int doLoopOfAcquisition(int _handle, int _totalAc, int _i_interval, JNIEnv* env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy);
void extract2from3Inf_everyInterval(unsigned char* _buffer, short* _i_returns, long long _size, int intervalnum);
void extractMono12or16Inf_everyInterval(unsigned char* _buffer, short* _i_returns, long long _size, int intervalnum);
void deleteBuffers();

// method definition
int isSCMOSconnected() {

    AT_BOOL campresent = 0;
    int someint = 0;

    /* requires atcore.dll and atdevchamcam.dll at the bare minimum
    int i_retCode;
    std::cout << "Initialising ..." << std::endl << std::endl;
    i_retCode = AT_InitialiseLibrary();
    if (i_retCode != AT_SUCCESS) {
        std::cout << "Error initialising library" << std::endl << std::endl;
    }
    else {
        AT_64 iNumberDevices = 0;
        AT_GetInt(AT_HANDLE_SYSTEM, L"Device Count", &iNumberDevices);
        if (iNumberDevices <= 0) {
            std::cout << "No cameras detected" << std::endl;
        }
        else {
            AT_H Hndl;
            i_retCode = AT_Open(0, &Hndl);
            if (i_retCode != AT_SUCCESS) {
                std::cout << "Error condition, could not initialise camera" << std::endl << std::endl;
            }
            else {
                std::cout << "Successfully initialised camera" << std::endl << std::endl;
            }
            AT_WC szValue[64];
            i_retCode = AT_GetString(Hndl, L"Serial Number", szValue, 64);
            if (i_retCode == AT_SUCCESS) {
                //The serial number of the camera is szValue
                std::wcout << L"The serial number is " << szValue << std::endl;
            }
            else {
                std::cout << "Error obtaining Serial number" << std::endl << std::endl;
            }
            AT_Close(Hndl);
        }
        AT_FinaliseLibrary();
    }
    */


    imgobj.i_retCode = AT_InitialiseLibrary();
    assert((imgobj.i_retCode == AT_SUCCESS) && "error AT_InitialiseLibrary");
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "Error initialising library" << std::endl << std::endl;
    }
    else {
        imgobj.i_retCode = AT_GetInt(AT_HANDLE_SYSTEM, L"Device Count", &imgobj.iNumberDevices);

        //assert((imgobj.iNumberDevices > 0) && "camera is not connected");
        if (imgobj.iNumberDevices <= 0) {
            std::cout << "No cameras detected" << std::endl;
            AT_FinaliseLibrary();
            return 1; // camera is not connected
        }
        else {
            imgobj.i_retCode = AT_Open(0, &imgobj.Hndl);

            if (imgobj.i_retCode != AT_SUCCESS) {
                std::cout << "Error condition, could not initialise camera; i_retCode: " << imgobj.i_retCode << std::endl << std::endl;
                AT_FinaliseLibrary();
                return imgobj.i_retCode; // error code 17 is due to Solis/umanger is still open
            }
            else {

                imgobj.i_retCode = AT_GetBool(imgobj.Hndl, L"CameraPresent", &campresent);

                if (imgobj.i_retCode != AT_SUCCESS) {
                    std::cout << "error CameraPresent, error code: " << imgobj.i_retCode << std::endl;
                }
            }
        }
    }
    /*
    std::cout << "isSCMOSconnectedSDK3 closing and finalising library, Hndl: " << imgobj.Hndl << " &Hndl: " << &imgobj.Hndl << std::endl;
   
    AT_Close(imgobj.Hndl);
    AT_FinaliseLibrary();
    */
    

    if (campresent == 1) {
        someint = 0; // camera is connected and Solis/umanager is not turned on
    }
    else {
        someint = 99; // some other error
    }

    return someint;

}
void InitializeSystem() {

    // InitializeLibrary, detect camera, Open Handle
    imgobj.i_retCode = AT_InitialiseLibrary();
    assert((imgobj.i_retCode == AT_SUCCESS) && "error AT_InitialiseLibrary");
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "Error Initialize library" << std::endl;
    }
    else {
        AT_GetInt(AT_HANDLE_SYSTEM, L"DeviceCount", &imgobj.iNumberDevices);
        //assert((imgobj.iNumberDevices > 0) && "no cameras detected");
        if (imgobj.iNumberDevices <= 0) {
            std::cout << "No cameras detected" << std::endl;
        }
        else {
            imgobj.i_retCode = AT_Open(0, &imgobj.Hndl);
            //assert((imgobj.i_retCode == AT_SUCCESS) && "error Opening Handle");
        }
    }

}
AT_WC* getEnumString(const wchar_t* feature) {

    int indexenum;
    AT_WC* iFeatureValue = new AT_WC[64];
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, feature, &indexenum);
    if (imgobj.i_retCode == AT_SUCCESS) {
        imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, feature, indexenum, iFeatureValue, 64);
        if (imgobj.i_retCode != AT_SUCCESS) {
            std::cout << "Error get enum string" << std::endl;
            std::cout << "error code: " << imgobj.i_retCode << std::endl << std::endl;
        }
        else {
            return iFeatureValue;
        }
    }
}
AT_WC* getStringVal(const wchar_t* feature) {

    AT_WC* iFeatureValue = new AT_WC[64];
    imgobj.i_retCode = AT_GetString(imgobj.Hndl, feature, iFeatureValue, 64);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "Error get string value" << std::endl;
        std::cout << "error code: " << imgobj.i_retCode << std::endl << std::endl;
    }
    else {
        return iFeatureValue;
    }

}
void setParamSingle() {

    /*
    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"FrameGenFixedPixelValue", 200);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "error FrameGenPixelValue: " << imgobj.i_retCode << std::endl;
    }

    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"FrameGenMode", imgobj.iFrameGenMode);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "error FrameGenMode: " << imgobj.i_retCode << std::endl;
    }
    else {
        std::wcout << imgobj.iFrameGenMode << std::endl;
    }
    */

    /*
    * TriggerMode = Internal
    * PixelEncoding = Mono12Packed
    * CycleMode = Fixed
    * FrameCount = 1
    * ElectronicShutteringMode = Rolling
    * PixelReadoutRate = 200MHz
    * GainMode = Fastest frame rate (12-bit)
    * AOILayout = Image
    * Spurious Nosie Filter = false;
    */

    int iret = 0;

    // set trigger mode to Software
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"TriggerMode", L"Internal");
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void TrigerMode");

    
    // set GainMode
    imgobj.i_retCode = AT_GetEnumCount(imgobj.Hndl, L"GainMode", &imgobj.GMcount);

    for (int i = 0; i < imgobj.GMcount; i++) {
        AT_GetEnumStringByIndex(imgobj.Hndl, L"GainMode", i, imgobj.GMstring, 64);
        //std::cout << "GainMode index: " << i << std::endl;
        //std::wcout << "GainMode string: " << imgobj.GMstring << std::endl;
    }
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"GainMode", &imgobj.GMindex);

    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"GainMode", imgobj.GMindex, imgobj.GMstring, 64);


    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"GainMode", imgobj.iGainMode);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void GainModeset");
    

    //Set the pixel Encoding to the desired settings Mono12Packed Data
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"Pixel Encoding", imgobj.iPixelEncoding);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void PixelEncoding");

    
    //Set the pixel Readout Rate
    imgobj.i_retCode = AT_GetEnumCount(imgobj.Hndl, L"PixelReadoutRate", &imgobj.PRRcount);

    for (int i = 0; i < imgobj.PRRcount; i++) {
        AT_GetEnumStringByIndex(imgobj.Hndl, L"PixelReadoutRate", i, imgobj.PRRstring, 64);
    }
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"PixelReadoutRate", &imgobj.PRRindex);
 
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"PixelReadoutRate", imgobj.PRRindex, imgobj.PRRstring, 64);

    


    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"PixelReadoutRate", imgobj.iPixelReadoutRate); // 100MHz or 200MHz for sona
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void PixelReadoutRate");

    // Set Fixed
    imgobj.i_retCode = AT_SetEnumString(imgobj.Hndl, L"CycleMode", L"Fixed");
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setParamSingle() CycleMode");

    // frame count
    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"FrameCount", 1);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setParamSingle() FrameCount");


    // Set Rolling shutter    
    imgobj.i_retCode = AT_GetEnumCount(imgobj.Hndl, L"ElectronicShutteringMode", &imgobj.ShutterModecount);

    for (int i = 0; i < imgobj.ShutterModecount; i++) {
        AT_GetEnumStringByIndex(imgobj.Hndl, L"ElectronicShutteringMode", i, imgobj.ShutterModestring, 64);
    }
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"ElectronicShutteringMode", &imgobj.ShutterModeindex);

    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"ElectronicShutteringMode", imgobj.ShutterModeindex, imgobj.ShutterModestring, 64);
   
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"ElectronicShutteringMode", imgobj.iElectronicShutteringMode);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void ElectronicShutteringMode");

    // set overlap
    imgobj.i_retCode = AT_SetBool(imgobj.Hndl, L"Overlap", imgobj.iOverlap);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setoverlap");

    // set spuriouse noise filter
    imgobj.i_retCode = AT_SetBool(imgobj.Hndl, L"SpuriousNoiseFilter", imgobj.iSpuriousNoiseFilter);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setspuriousnoisefilter");


    //Set AOI in the following order AOIHBin, AOIVBin, AOIWidth, AOIleft, AOIHeight, AOITop
    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIHBin", imgobj.iAOIHBin);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIHBin");

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIVBin", imgobj.iAOIVBin);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIVBin");

    AT_GetIntMax(imgobj.Hndl, L"AOIWidth", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOIWidth", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIWidth", imgobj.iAOIWidth);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
        std::cout << "error AOIWidth set: " << imgobj.i_retCode << std::endl;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIWidth");

    AT_GetIntMax(imgobj.Hndl, L"AOILeft", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOILeft", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOILeft", imgobj.iAOILeft);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOILeft");

    AT_GetIntMax(imgobj.Hndl, L"AOIHeight", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOIHeight", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIHeight", imgobj.iAOIHeight);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIHeight");

    AT_GetIntMax(imgobj.Hndl, L"AOITop", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOITop", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOITop", imgobj.iAOITop);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOITop");

    // Max Interface Transfer Rate
    imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"MaxInterfaceTransferRate", &imgobj.maxFrameRate);
    assert((imgobj.i_retCode == 0) && "error void setParamSingle() MaxTransferRate");
    

    // note that it is important to set EXPOSURE TIME and FRAMETIME once all other parameter are set such as readout rate, shutterimg mode, AOI as these parameter affect how fast we can record
    // set exposure time and frame rate
    AT_GetFloatMin(imgobj.Hndl, L"Exposure Time", &imgobj.minExpSingle);
    if (imgobj.iExposureTime <= imgobj.minExpSingle) {
        imgobj.iExposureTime = imgobj.minExpSingle;
    }
    AT_GetFloatMax(imgobj.Hndl, L"Exposure Time", &imgobj.maxExpSingle);
    if (imgobj.iExposureTime >= imgobj.maxExpSingle) {
        imgobj.iExposureTime = imgobj.maxExpSingle;
    }
    

    imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Exposure Time", imgobj.iExposureTime);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetExposureTime");

    imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"ExposureTime", &imgobj.iExposureTime);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void GetExposureTime23");

    AT_GetFloatMin(imgobj.Hndl, L"FrameRate", &imgobj.minFrameRate);
    AT_GetFloatMax(imgobj.Hndl, L"FrameRate", &imgobj.maxFrameRate);
    if (imgobj.iFrameRate <= imgobj.minFrameRate) {
        imgobj.iFrameRate = imgobj.minFrameRate;
    }
    if (imgobj.iFrameRate >= imgobj.maxFrameRate) {
        imgobj.iFrameRate = imgobj.maxFrameRate;
    }
    imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Framerate", imgobj.iFrameRate);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error SetFrameRate");

    if (imgobj.iExposureTime < (1 / imgobj.iFrameRate)) {
        imgobj.iExposureTime = (1 / imgobj.iFrameRate);
        imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Exposure Time", imgobj.iExposureTime);
        if (imgobj.i_retCode != AT_SUCCESS) {
            iret = 1;
        }
        assert((imgobj.i_retCode == 0) && "error void reSetExposureTime25");

        imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"ExposureTime", &imgobj.iExposureTime);
        if (imgobj.i_retCode != AT_SUCCESS) {
            iret = 1;
        }
        assert((imgobj.i_retCode == 0) && "error void GetExposureTime24");
    }





    /*
    * double checking acquisition parameter
    */
    //Pizxel readout rate
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"PixelReadoutRate", &imgobj.PRRindex);
    std::cout << "selected pixelreadoutrate index: " << imgobj.PRRindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"PixelReadoutRate", imgobj.PRRindex, imgobj.PRRstring, 64);
    std::wcout << "selected pixelreadoutrate string: " << imgobj.PRRstring << std::endl;

    //GainMode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"GainMode", &imgobj.GMindex);
    std::cout << "selected GainMode index: " << imgobj.GMindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"GainMode", imgobj.GMindex, imgobj.GMstring, 64);
    std::wcout << "selected GainMode string: " << imgobj.GMstring << std::endl;

    // Rolling shutter
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"ElectronicShutteringMode", &imgobj.ShutterModeindex);
    std::cout << "selected ElectronicShutterMode index: " << imgobj.ShutterModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"ElectronicShutteringMode", imgobj.ShutterModeindex, imgobj.ShutterModestring, 64);
    std::wcout << "selected ElectronicShutterMode string: " << imgobj.ShutterModestring << std::endl;

    // AOI Layout
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"AOILayout", &imgobj.AOILayoutindex);
    std::cout << "selected AOILayoute index: " << imgobj.AOILayoutindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"AOILayout", imgobj.AOILayoutindex, imgobj.AOILayoutstring, 64);
    std::wcout << "selected AOILayout string: " << imgobj.AOILayoutstring << std::endl;

    // Trigger Mode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"TriggerMode", &imgobj.TriggerModeindex);
    std::cout << "selected TriggerMode index: " << imgobj.TriggerModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"TriggerMode", imgobj.TriggerModeindex, imgobj.TriggerModestring, 64);
    std::wcout << "selected TriggerMode string: " << imgobj.TriggerModestring << std::endl;

    //PixelEncoding
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"Pixel Encoding", &imgobj.PixelEncodingindex);
    std::cout << "selected PixelEncoding index: " << imgobj.PixelEncodingindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"Pixel Encoding", imgobj.PixelEncodingindex, imgobj.PixelEncodingstring, 64);
    std::wcout << "selected PixelEncoding string: " << imgobj.PixelEncodingstring << std::endl;

    // CycleMode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"CycleMode", &imgobj.CycleModeindex);
    std::cout << "selected CycleMode index: " << imgobj.CycleModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"CycleMode", imgobj.CycleModeindex, imgobj.CycleModestring, 64);
    std::wcout << "selected CycleMode string: " << imgobj.CycleModestring << std::endl;

    // Overlap
    imgobj.i_retCode = AT_GetBool(imgobj.Hndl, L"Overlap", &imgobj.Overlapval);
    std::cout << "selected Overlap mode: " << imgobj.Overlapval << std::endl;

    // FrameCount
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"FrameCount", &imgobj.FrameCountval);
    std::cout << "selected FrameCount: " << imgobj.FrameCountval << std::endl;

    // RealExposure
    AT_GetFloat(imgobj.Hndl, L"Exposure Time", &imgobj.realExposure);
    std::cout << "real exposure: " << imgobj.realExposure << std::endl;

    // FrameRate
    AT_GetFloat(imgobj.Hndl, L"FrameRate", &imgobj.realFramerate);
    std::cout << "real framrerate: " << imgobj.realFramerate << std::endl;

    //Bit depth
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"BitDepth", &imgobj.BitDepthindex);
    std::cout << "selected BitDepth index: " << imgobj.BitDepthindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"BitDepth", imgobj.BitDepthindex, imgobj.BitDepthstring, 64);
    std::wcout << "selected BitDepth string: " << imgobj.BitDepthstring << std::endl;

    //Baseline
    AT_GetInt(imgobj.Hndl, L"Baseline", &imgobj.Baselineval);
    std::cout << "Baseline cnts: " << imgobj.Baselineval << std::endl;

    //Bytesperpixel
    AT_GetFloat(imgobj.Hndl, L"BytesPerPixel", &imgobj.Bytesperpixelval);
    std::cout << "Bytesperpixel: " << imgobj.Bytesperpixelval << std::endl;

    //Spuriosnoisefilter
    AT_GetBool(imgobj.Hndl, L"SpuriousNoiseFilter", &imgobj.Spuriousnoisefilterval);
    std::cout << "Spurious nose filter: " << imgobj.Spuriousnoisefilterval << std::endl;

    //AOIHbin
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIHBin", &imgobj.AOIHbinval);
    std::cout << "setted AOIHbin: " << imgobj.AOIHbinval << std::endl;

    //AOIVbin
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIVBin", &imgobj.AOIVbinval);
    std::cout << "setted AOIVbin: " << imgobj.AOIVbinval << std::endl;

    //AOIWidth
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIWidth", &imgobj.AOIWidthval);
    std::cout << "setted AOIWidth: " << imgobj.AOIWidthval << std::endl;

    //AOILeft
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOILeft", &imgobj.AOILeftval);
    std::cout << "setted AOILeft: " << imgobj.AOILeftval << std::endl;

    //AOIHeight
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIHeight", &imgobj.AOIHeightval);
    std::cout << "setted AOIHeight: " << imgobj.AOIHeightval << std::endl;

    //AOILTop
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOITop", &imgobj.AOITopval);
    std::cout << "setted AOITop: " << imgobj.AOITopval << std::endl;

}
void resetImageClsparam() {
    imgobj.isStopPressed = false;
}
void extract2from3single(unsigned char* _buffer, short* _i_returns, int _nopixperframe) {

    /* // for debugging purpose; please print first 20 pixel
    _buffer = _buffer - 30; //need to subtract by 30 since previously 20 pixel was read out from buffer
    */

    int counter; // working part:zero 0 instances
    counter = 0;
    for (int i = 0; i < (3 * _nopixperframe / 2); i += 3) {
        *(_i_returns + counter) = EXTRACTLOWPACKED(_buffer); // second
        counter++;
        *(_i_returns + counter) = EXTRACTHIGHPACKED(_buffer); // first
        counter++;
        _buffer += 3;
    }

}
void extractMono12or16single(unsigned char* _buffer, short* _i_returns, int _nopixperframe) {

    unsigned short* ImagePixels = reinterpret_cast<unsigned short*>(_buffer);

    int counter;
    counter = 0;
    for (int i = 0; i < _nopixperframe; i++) {
        *(_i_returns + counter) = ImagePixels[i];
        counter++;
    }

}
void setParamInfiniteLoop() {

    int iret = 0;

    // Configures which signal appears on the auxiliary output pin
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"AuxiliaryOutSource", imgobj.iAuxOutSource);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error set AuxiliaryOutSource");

    // set trigger mode to Software
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"TriggerMode", L"Internal");
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void TrigerMode");

    //GainMode
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"GainMode", imgobj.iGainMode);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void GainModeset");

    //Set the pixel Encoding to the desired settings 
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"Pixel Encoding", imgobj.iPixelEncoding);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void PixelEncoding");

    // Set Pixel Readout Rate
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"PixelReadoutRate", imgobj.iPixelReadoutRate); // 100MHz or 200MHz for sona
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void PixelReadoutRate");

    // Set CycleMode
    imgobj.i_retCode = AT_SetEnumString(imgobj.Hndl, L"CycleMode", L"Continuous");
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void CycleMode");

    // ElectronicShutteringMode
    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, L"ElectronicShutteringMode", imgobj.iElectronicShutteringMode);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void ElectronicShutteringMode");

    // set overlap
    imgobj.i_retCode = AT_SetBool(imgobj.Hndl, L"Overlap", imgobj.iOverlap);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setoverlap");

    // set spuriouse noise filter
    imgobj.i_retCode = AT_SetBool(imgobj.Hndl, L"SpuriousNoiseFilter", imgobj.iSpuriousNoiseFilter);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void setspuriousnoisefilter");


    //Set AOI in the following order AOIHBin, AOIVBin, AOIWidth, AOIleft, AOIHeight, AOITop
    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIHBin", imgobj.iAOIHBin);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIHBin");

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIVBin", imgobj.iAOIVBin);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIVBin");

    AT_GetIntMax(imgobj.Hndl, L"AOIWidth", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOIWidth", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIWidth", imgobj.iAOIWidth);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIWidth");

    AT_GetIntMax(imgobj.Hndl, L"AOILeft", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOILeft", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOILeft", imgobj.iAOILeft);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOILeft");

    AT_GetIntMax(imgobj.Hndl, L"AOIHeight", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOIHeight", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOIHeight", imgobj.iAOIHeight);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOIHeight");

    AT_GetIntMax(imgobj.Hndl, L"AOITop", &imgobj.maxval);
    AT_GetIntMin(imgobj.Hndl, L"AOITop", &imgobj.minval);

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, L"AOITop", imgobj.iAOITop);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetAOITop");
   
    
    // Max Interface Transfer Rate
    imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"MaxInterfaceTransferRate", &imgobj.maxFrameRate);
    assert((imgobj.i_retCode == 0) && "error void setParamSingle() MaxTransferRate");

    // note that it is important to set EXPOSURE TIME and FRAMETIME once all other parameter are set such as readout rate, shutterimg mode, AOI as these parameter affect how fast we can record
    // set exposure time and frame rate
    AT_GetFloatMin(imgobj.Hndl, L"Exposure Time", &imgobj.minExpSingle);
    if (imgobj.iExposureTime <= imgobj.minExpSingle) {
        imgobj.iExposureTime = imgobj.minExpSingle;
    }
    AT_GetFloatMax(imgobj.Hndl, L"Exposure Time", &imgobj.maxExpSingle);
    if (imgobj.iExposureTime >= imgobj.maxExpSingle) {
        imgobj.iExposureTime = imgobj.maxExpSingle;
    }

    imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Exposure Time", imgobj.iExposureTime);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void SetExposureTime");

    imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"ExposureTime", &imgobj.iExposureTime);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error void GetExposureTime23");

    AT_GetFloatMin(imgobj.Hndl, L"FrameRate", &imgobj.minFrameRate);
    AT_GetFloatMax(imgobj.Hndl, L"FrameRate", &imgobj.maxFrameRate);
    if (imgobj.iFrameRate <= imgobj.minFrameRate) {
        imgobj.iFrameRate = imgobj.minFrameRate;
    }
    if (imgobj.iFrameRate >= imgobj.maxFrameRate) {
        imgobj.iFrameRate = imgobj.maxFrameRate;
    }
    imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Framerate", imgobj.iFrameRate);
    if (imgobj.i_retCode != AT_SUCCESS) {
        iret = 1;
    }
    assert((imgobj.i_retCode == 0) && "error SetFrameRate");

    if (imgobj.iExposureTime < (1 / imgobj.iFrameRate)) {
        imgobj.iExposureTime = (1 / imgobj.iFrameRate);
        imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, L"Exposure Time", imgobj.iExposureTime);
        if (imgobj.i_retCode != AT_SUCCESS) {
            iret = 1;
        }
        assert((imgobj.i_retCode == 0) && "error void reSetExposureTime25");

        imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, L"ExposureTime", &imgobj.iExposureTime);
        if (imgobj.i_retCode != AT_SUCCESS) {
            iret = 1;
        }
        assert((imgobj.i_retCode == 0) && "error void GetExposureTime24");
    }

    /*
   * double checking acquisition parameter
   */
    std::cout << "Double checking acquisition paramter" << std::endl;

    //Pizxel readout rate
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"PixelReadoutRate", &imgobj.PRRindex);
    std::cout << "selected pixelreadoutrate index: " << imgobj.PRRindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"PixelReadoutRate", imgobj.PRRindex, imgobj.PRRstring, 64);
    std::wcout << "selected pixelreadoutrate string: " << imgobj.PRRstring << std::endl;

    //GainMode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"GainMode", &imgobj.GMindex);
    std::cout << "selected GainMode index: " << imgobj.GMindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"GainMode", imgobj.GMindex, imgobj.GMstring, 64);
    std::wcout << "selected GainMode string: " << imgobj.GMstring << std::endl;

    // Rolling shutter
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"ElectronicShutteringMode", &imgobj.ShutterModeindex);
    std::cout << "selected ElectronicShutterMode index: " << imgobj.ShutterModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"ElectronicShutteringMode", imgobj.ShutterModeindex, imgobj.ShutterModestring, 64);
    std::wcout << "selected ElectronicShutterMode string: " << imgobj.ShutterModestring << std::endl;

    // AOI Layout
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"AOILayout", &imgobj.AOILayoutindex);
    std::cout << "selected AOILayoute index: " << imgobj.AOILayoutindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"AOILayout", imgobj.AOILayoutindex, imgobj.AOILayoutstring, 64);
    std::wcout << "selected AOILayout string: " << imgobj.AOILayoutstring << std::endl;

    // Trigger Mode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"TriggerMode", &imgobj.TriggerModeindex);
    std::cout << "selected TriggerMode index: " << imgobj.TriggerModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"TriggerMode", imgobj.TriggerModeindex, imgobj.TriggerModestring, 64);
    std::wcout << "selected TriggerMode string: " << imgobj.TriggerModestring << std::endl;

    //PixelEncoding
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"Pixel Encoding", &imgobj.PixelEncodingindex);
    std::cout << "selected PixelEncoding index: " << imgobj.PixelEncodingindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"Pixel Encoding", imgobj.PixelEncodingindex, imgobj.PixelEncodingstring, 64);
    std::wcout << "selected PixelEncoding string: " << imgobj.PixelEncodingstring << std::endl;

    // CycleMode
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"CycleMode", &imgobj.CycleModeindex);
    std::cout << "selected CycleMode index: " << imgobj.CycleModeindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"CycleMode", imgobj.CycleModeindex, imgobj.CycleModestring, 64);
    std::wcout << "selected CycleMode string: " << imgobj.CycleModestring << std::endl;

    // Overlap
    imgobj.i_retCode = AT_GetBool(imgobj.Hndl, L"Overlap", &imgobj.Overlapval);
    std::cout << "selected Overlap mode: " << imgobj.Overlapval << std::endl;

    // FrameCount
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"FrameCount", &imgobj.FrameCountval);
    std::cout << "selected FrameCount: " << imgobj.FrameCountval << std::endl;

    // RealExposure
    AT_GetFloat(imgobj.Hndl, L"Exposure Time", &imgobj.realExposure);
    std::cout << "real exposure: " << imgobj.realExposure << std::endl;

    // FrameRate
    AT_GetFloat(imgobj.Hndl, L"FrameRate", &imgobj.realFramerate);
    std::cout << "real framrerate: " << imgobj.realFramerate << std::endl;

    //Bit depth
    imgobj.i_retCode = AT_GetEnumIndex(imgobj.Hndl, L"BitDepth", &imgobj.BitDepthindex);
    std::cout << "selected BitDepth index: " << imgobj.BitDepthindex << std::endl;
    imgobj.i_retCode = AT_GetEnumStringByIndex(imgobj.Hndl, L"BitDepth", imgobj.BitDepthindex, imgobj.BitDepthstring, 64);
    std::wcout << "selected BitDepth string: " << imgobj.BitDepthstring << std::endl;

    //Baseline
    AT_GetInt(imgobj.Hndl, L"Baseline", &imgobj.Baselineval);
    std::cout << "Baseline cnts: " << imgobj.Baselineval << std::endl;

    //Bytesperpixel
    AT_GetFloat(imgobj.Hndl, L"BytesPerPixel", &imgobj.Bytesperpixelval);
    std::cout << "Bytesperpixel: " << imgobj.Bytesperpixelval << std::endl;

    //Spuriosnoisefilter
    AT_GetBool(imgobj.Hndl, L"SpuriousNoiseFilter", &imgobj.Spuriousnoisefilterval);
    std::cout << "Spurious nose filter: " << imgobj.Spuriousnoisefilterval << std::endl;

    //AOIHbin
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIHBin", &imgobj.AOIHbinval);
    std::cout << "setted AOIHbin: " << imgobj.AOIHbinval << std::endl;

    //AOIVbin
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIVBin", &imgobj.AOIVbinval);
    std::cout << "setted AOIVbin: " << imgobj.AOIVbinval << std::endl;

    //AOIWidth
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIWidth", &imgobj.AOIWidthval);
    std::cout << "setted AOIWidth: " << imgobj.AOIWidthval << std::endl;

    //AOILeft
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOILeft", &imgobj.AOILeftval);
    std::cout << "setted AOILeft: " << imgobj.AOILeftval << std::endl;

    //AOIHeight
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOIHeight", &imgobj.AOIHeightval);
    std::cout << "setted AOIHeight: " << imgobj.AOIHeightval << std::endl;

    //AOILTop
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"AOITop", &imgobj.AOITopval);
    std::cout << "setted AOITop: " << imgobj.AOITopval << std::endl;

    return;

}
int performAcquisition_infiniteLoop(int _handle, int _numberAcquisitions, int _intervalFrame, JNIEnv* env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {
    int iret = 0;

    createBuffers(_handle);
    AT_Command(_handle, L"AcquisitionStart");
    Sleep(100); //To Give acquisition time to start

    /* //debugging
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"CameraImagesWaiting", &CameraImageWaitingval);
    std::cout << "after sleep, images waiting: " << CameraImageWaitingval << std::endl;
    */

    if (iret == 0) {
        if (doLoopOfAcquisition(_handle, _numberAcquisitions, _intervalFrame, env, _outArray, framecounterObj, increment_id, carray, isCopy) != 0) {
            iret = 1;
        }
    }

    return iret;
}
void createBuffers(int _handle){
    imgobj.acqBuffer = QueueBuffer(_handle);
    imgobj.acqBuffer1 = QueueBuffer(_handle);
    imgobj.acqBuffer2 = QueueBuffer(_handle);
    imgobj.acqBuffer3 = QueueBuffer(_handle);
    imgobj.acqBuffer4 = QueueBuffer(_handle);
    imgobj.acqBuffer5 = QueueBuffer(_handle);
    imgobj.acqBuffer6 = QueueBuffer(_handle);
    imgobj.acqBuffer7 = QueueBuffer(_handle);
    imgobj.acqBuffer8 = QueueBuffer(_handle);
    imgobj.acqBuffer9 = QueueBuffer(_handle);
    imgobj.acqBuffer10 = QueueBuffer(_handle);
    imgobj.acqBuffer11 = QueueBuffer(_handle);
    imgobj.acqBuffer12 = QueueBuffer(_handle);
    imgobj.acqBuffer13 = QueueBuffer(_handle);
    imgobj.acqBuffer14 = QueueBuffer(_handle);
    imgobj.acqBuffer15 = QueueBuffer(_handle);
    imgobj.acqBuffer16 = QueueBuffer(_handle);
    imgobj.acqBuffer17 = QueueBuffer(_handle);
    imgobj.acqBuffer18 = QueueBuffer(_handle);
    imgobj.acqBuffer19 = QueueBuffer(_handle);
    imgobj.acqBuffer20 = QueueBuffer(_handle);
}
unsigned char* QueueBuffer(int _handle) {
    // Get the number of bytes required to store one frame
    imgobj.i_retCode = AT_GetInt(_handle, L"ImageSizeBytes", &imgobj.iImageSizeBytes);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "AT_GetInt failed - ImageSizeBytes - return code " << imgobj.i_retCode << std::endl;
    }
    assert((imgobj.i_retCode == AT_SUCCESS) && "error getting ImageSizeBytes");

    unsigned char* acqBuffer = NULL;
    if (imgobj.i_retCode == AT_SUCCESS) {
        int BufferSize = static_cast<int>(imgobj.iImageSizeBytes);
        /*int BS = BufferSize + 7;*/
        int BS = BufferSize;
        /*    int BS = BufferSize + (7 * imgobj.intervalFrame);*/
            // Allocate a memory buffer to store one frame
        acqBuffer = new unsigned char[BS];
        // Pass this buffer to the SDK
        //std::cout << "Imagesizebytes: " << imgobj.iImageSizeBytes << " ; BufferSize: " << BufferSize << std::endl;
        imgobj.i_retCode = AT_QueueBuffer(_handle, acqBuffer, BufferSize);
        if (imgobj.i_retCode != AT_SUCCESS) {
            std::cout << "AT_QueueBuffer failed - Image Size Bytes - return code " << imgobj.i_retCode << std::endl;
        }
        assert((imgobj.i_retCode == AT_SUCCESS) && "error AT_QueueBuffer");

    }

    return acqBuffer;

}
int doLoopOfAcquisition(int _handle, int _totalAc, int _i_interval, JNIEnv* env, jshortArray _outArray, jobject framecounterObj, jmethodID increment_id, jshort* carray, jboolean isCopy) {

    int c = _totalAc / _i_interval;
    int new_c;
    long long i_pixperinter = _i_interval * imgobj.iAOIHeight * imgobj.iAOIWidth;
    imgobj.framecounter = 0;
 
    for (int j = 0; j < c; j++) {

        new_c = j % imgobj.size_b;
        
        if (imgobj.isStopPressed == true) {
            break;
        }

        for (int i = 0; i < _i_interval; i++) {

            unsigned char* pBuf;
            int BufSize;

            /* //debug
            imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"CameraImagesWaiting", &CameraImageWaitingval);
            std::cout << "Before WaitBuffer, images waiting: " << CameraImageWaitingval << std::endl;
            */

            imgobj.i_retCode = AT_WaitBuffer(_handle, &pBuf, &BufSize, 10000);
            assert((imgobj.i_retCode == AT_SUCCESS) && "Waitbuffer insufficient code 13");
            if (imgobj.i_retCode != AT_SUCCESS) {
                std::cout << "Error:Acquisition timeout when not expecting, retcode " << imgobj.i_retCode << std::endl;
                return 1;
            }
            if (imgobj.PixelEncodingIndex == 1) {
                extract2from3Inf_everyInterval(pBuf, imgobj.pImageArrayInf, (imgobj.iAOIHeight * imgobj.iAOIWidth), i); 
            }
            else {
                extractMono12or16Inf_everyInterval(pBuf, imgobj.pImageArrayInf, (imgobj.iAOIHeight * imgobj.iAOIWidth), i);
            }


            //requeue buffer
            AT_QueueBuffer(_handle, pBuf, static_cast<int>(imgobj.iImageSizeBytes));

        }
    
        if (isCopy == true || imgobj.enableCriticalAccess == 0) {
            tk3.setTimeStart();
            env->SetShortArrayRegion(_outArray, (new_c * i_pixperinter), i_pixperinter, imgobj.pImageArrayInf);
            env->CallVoidMethod(framecounterObj, increment_id);
            imgobj.timeelapsed3 += tk3.getTimeElapsed();
        }
        else {
            std::copy(imgobj.pImageArrayInf, imgobj.pImageArrayInf + i_pixperinter, carray + (new_c * i_pixperinter));
            env->CallVoidMethod(framecounterObj, increment_id);
        }

        imgobj.framecounter++;


    }

    return 0;

}
void extract2from3Inf_everyInterval(unsigned char* _buffer, short* _i_returns, long long _size, int intervalnum) {

    int counter; // working part:zero 0 instances
    counter = 0;
    for (int i = 0; i < (3 * _size / 2); i += 3) {
        *(_i_returns + (intervalnum * _size) + counter) = EXTRACTLOWPACKED(_buffer); // second
        counter++;
        *(_i_returns + (intervalnum * _size) + counter) = EXTRACTHIGHPACKED(_buffer); // first
        counter++;
        _buffer += 3;
    }

}
void extractMono12or16Inf_everyInterval(unsigned char* _buffer, short* _i_returns, long long _size, int intervalnum) {

    unsigned short* ImagePixels = reinterpret_cast<unsigned short*>(_buffer);

    int counter;
    counter = 0;
    for (int i = 0; i < _size; i++) {
        *(_i_returns + (intervalnum * _size) + counter) = ImagePixels[i];
        counter++;
    }

}
void deleteBuffers(){
    delete[]imgobj.acqBuffer;
    imgobj.acqBuffer = NULL;
    delete[]imgobj.acqBuffer1;
    imgobj.acqBuffer1 = NULL;
    delete[]imgobj.acqBuffer2;
    imgobj.acqBuffer2 = NULL;
    delete[]imgobj.acqBuffer3;
    imgobj.acqBuffer3 = NULL;
    delete[]imgobj.acqBuffer4;
    imgobj.acqBuffer4 = NULL;
    delete[]imgobj.acqBuffer5;
    imgobj.acqBuffer5 = NULL;
    delete[]imgobj.acqBuffer6;
    imgobj.acqBuffer6 = NULL;
    delete[]imgobj.acqBuffer7;
    imgobj.acqBuffer7 = NULL;
    delete[]imgobj.acqBuffer8;
    imgobj.acqBuffer8 = NULL;
    delete[]imgobj.acqBuffer9;
    imgobj.acqBuffer9 = NULL;
    delete[]imgobj.acqBuffer10;
    imgobj.acqBuffer10 = NULL;
    delete[]imgobj.acqBuffer11;
    imgobj.acqBuffer11 = NULL;
    delete[]imgobj.acqBuffer12;
    imgobj.acqBuffer12 = NULL;
    delete[]imgobj.acqBuffer13;
    imgobj.acqBuffer13 = NULL;
    delete[]imgobj.acqBuffer14;
    imgobj.acqBuffer14 = NULL;
    delete[]imgobj.acqBuffer15;
    imgobj.acqBuffer15 = NULL;
    delete[]imgobj.acqBuffer16;
    imgobj.acqBuffer16 = NULL;
    delete[]imgobj.acqBuffer17;
    imgobj.acqBuffer17 = NULL;
    delete[]imgobj.acqBuffer18;
    imgobj.acqBuffer18 = NULL;
    delete[]imgobj.acqBuffer19;
    imgobj.acqBuffer19 = NULL;
    delete[]imgobj.acqBuffer20;
    imgobj.acqBuffer20 = NULL;
}

// Test JNI
JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_seyHello(JNIEnv* env, jclass cls, jint n1, jint n2) {
	jfloat result;
	result = ((jfloat)n1 + n2) / 2.0;
	return result;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_sayHello(JNIEnv* env, jobject thisObj) {
	std::cout << "hello from cpp" << std::endl;
	return 1;
}

//SDK3
JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_isSCMOSconnectedSDK3(JNIEnv* env, jclass cls) {
    //check and return camera status; JNI call did not close and finalize library (need to be called separately)
	return isSCMOSconnected();
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_ShutDownSDK3(JNIEnv* env, jclass cls) {
    //close and finalize library
    AT_Close(imgobj.Hndl);
    AT_FinaliseLibrary();
    return;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SystemShutDownSDK3(JNIEnv* env, jclass cls) {
    //turn off cooling, close and finalize library
    AT_SetBool(imgobj.Hndl, L"SensorCooling", AT_FALSE);
    AT_Close(imgobj.Hndl);
    AT_FinaliseLibrary();
    return;
}

JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_InitializeSystemSDK3(JNIEnv* env, jclass cls) {
    // initialize library and open handle
    InitializeSystem();
    return true; //TODO
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetEnumeratedStringSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;

    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);

    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);

    const wchar_t* szName = name.c_str();

    AT_WC* EnumString = getEnumString(szName);

    // convert wchar_t* to char*   
    char* outCStr = new char[4048];
    wcstombs(outCStr, EnumString, 64);

    delete[] EnumString;

    // convert const char* to char*
    const char* outCStrcons = (const char*)outCStr;

    return (*env).NewStringUTF(outCStrcons);
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetIntegerValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    AT_64 result;
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, szName, &result);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "error: " << imgobj.i_retCode << std::endl;
        return 99;
    }
    return result;
}

JNIEXPORT jdouble JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetDoubleValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();


    double result;
    imgobj.i_retCode = AT_GetFloat(imgobj.Hndl, szName, &result);

    return result;
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetStringValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    AT_WC* StringVal = getStringVal(szName);


    // convert wchar_t* to char*   
    char* outCStr = new char[4048];
    wcstombs(outCStr, StringVal, 64);

    delete[] StringVal;

    // convert const char* to char*
    const char* outCStrcons = (const char*)outCStr;

    return (*env).NewStringUTF(outCStrcons);
}

JNIEXPORT jboolean JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetBooleanValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();


    AT_BOOL result;
    imgobj.i_retCode = AT_GetBool(imgobj.Hndl, szName, &result);
    return result;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetEnumCountSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    int count;
    imgobj.i_retCode = AT_GetEnumCount(imgobj.Hndl, szName, &count);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "error: " << imgobj.i_retCode << std::endl;
        return 99;
    }
    return count;
    
}

JNIEXPORT jstring JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetEnumStringByIndexSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jint index) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    AT_WC* GMString = new AT_WC[64];
    AT_GetEnumStringByIndex(imgobj.Hndl, szName, index, GMString, 64);
    // convert wchar_t* to char*   
    char* outCStr = new char[4048];
    wcstombs(outCStr, GMString, 64);

    delete[] GMString;

    // convert const char* to char*
    const char* outCStrcons = (const char*)outCStr;

    return (*env).NewStringUTF(outCStrcons);
}

JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetFloatMaxSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    double max;
    imgobj.i_retCode = AT_GetFloatMax(imgobj.Hndl, szName, &max);

    return max;
}

JNIEXPORT jfloat JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetFloatMinSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    double min;
    imgobj.i_retCode = AT_GetFloatMin(imgobj.Hndl, szName, &min);

    return min;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetIntMaxSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    AT_64 max;
    imgobj.i_retCode = AT_GetIntMax(imgobj.Hndl, szName, &max);

    return max;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_GetIntMinSDK3(JNIEnv* env, jclass cls, jstring inJNIStr) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    AT_64 min;
    imgobj.i_retCode = AT_GetIntMin(imgobj.Hndl, szName, &min);

    return min;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SetEnumeratedStringSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jstring value) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    // obtain C++ const char
    const char* inCStr2 = (*env).GetStringUTFChars(value, NULL);
    if (NULL == inCStr2) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert2;
    std::wstring name2 = convert2.from_bytes(inCStr2);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr2);
    const wchar_t* szValue = name2.c_str();

    imgobj.i_retCode = AT_SetEnumeratedString(imgobj.Hndl, szName, szValue);
    if (imgobj.i_retCode != AT_SUCCESS) {
        std::cout << "error: " << imgobj.i_retCode << std::endl;
    }
    return imgobj.i_retCode;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SetIntegerValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jint value) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    imgobj.i_retCode = AT_SetInt(imgobj.Hndl, szName, value);
    return imgobj.i_retCode;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SetDoubleValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jdouble value) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    imgobj.i_retCode = AT_SetFloat(imgobj.Hndl, szName, value);
    return imgobj.i_retCode;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SetBooleanValueSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jint somebool) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    imgobj.i_retCode = AT_SetBool(imgobj.Hndl, szName, somebool);
    return imgobj.i_retCode;
}

JNIEXPORT jint JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_SetEnumIndexSDK3(JNIEnv* env, jclass cls, jstring inJNIStr, jint index) {
    // obtain C++ const char
    const char* inCStr = (*env).GetStringUTFChars(inJNIStr, NULL);
    if (NULL == inCStr) return NULL;
    // convert char* to const wchar_t*
    std::wstring_convert < std::codecvt_utf8_utf16<wchar_t>, wchar_t> convert;
    std::wstring name = convert.from_bytes(inCStr);
    (*env).ReleaseStringUTFChars(inJNIStr, inCStr);
    const wchar_t* szName = name.c_str();

    imgobj.i_retCode = AT_SetEnumIndex(imgobj.Hndl, szName, index); 
    return imgobj.i_retCode;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_setParameterSingleSDK3(JNIEnv* env, jclass cls, jdouble exposureTime, jint width, jint height, jint left, jint top, jint incamerabin, jint iPixelEncoding) {
    imgobj.iExposureTime = exposureTime;
    imgobj.iFrameRate = 1 / exposureTime;
    imgobj.iAOIHBin = incamerabin;
    imgobj.iAOIVBin = incamerabin;
    imgobj.iAOIWidth = width; // for 2x2 AOI bin; max widht is 1024 / min width 25
    imgobj.iAOIHeight = height; // for 2x2 AOI bin; max height is 1024 / min height 1
    imgobj.iAOILeft = ((AT_64)left - 1) * imgobj.iAOIHBin + 1; // previously: left
    imgobj.iAOITop = ((AT_64)top - 1) * imgobj.iAOIVBin + 1; // previously: top
    imgobj.PixelEncodingIndex = iPixelEncoding;
    if (iPixelEncoding == 0) {
        imgobj.iPixelEncoding = L"Mono12";
        imgobj.iGainMode = L"Fastest frame rate (12-bit)";
        imgobj.iPixelReadoutRate = L"200MHz";
    }
    else if (iPixelEncoding == 1) {
        imgobj.iPixelEncoding = L"Mono12Packed";
        imgobj.iGainMode = L"Fastest frame rate (12-bit)";
        imgobj.iPixelReadoutRate = L"200MHz";
    }
    else if (iPixelEncoding == 2) {
        imgobj.iPixelEncoding = L"Mono16";
        imgobj.iGainMode = L"High dynamic range (16-bit)";
        imgobj.iPixelReadoutRate = L"100MHz";
    }

    setParamSingle();
    resetImageClsparam();

    return;
    
}

JNIEXPORT jshortArray JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_runSingleScanSDK3(JNIEnv* env, jclass cls) {
    // initializing 1D array for single frame acquisition
    imgobj.setArraySizeSingle();
    imgobj.InitArraySingle();

    //Get the number of bytes required to store one frame
    imgobj.i_retCode = AT_GetInt(imgobj.Hndl, L"Image Size Bytes", &imgobj.iImageSizeBytes);
    assert((imgobj.i_retCode == AT_SUCCESS) && "error getting ImageSizeBytes");

    int BufferSize = static_cast<int>(imgobj.iImageSizeBytes);

    //Allocate a memory buffer to store one frame
    unsigned char* UserBuffer = new unsigned char[BufferSize];

    // Passing this buffer to SDK
    AT_QueueBuffer(imgobj.Hndl, UserBuffer, BufferSize);

    //Start the Acquisition running
    imgobj.i_retCode = AT_Command(imgobj.Hndl, L"Acquisition Start");
    assert((imgobj.i_retCode == AT_SUCCESS) && "error Acquisition Start");

    //Sleep in this thread until data is ready, in this case set
    //the timeout to infinite for simplicity
    unsigned char* Buffer;
    if (AT_WaitBuffer(imgobj.Hndl, &Buffer, &BufferSize, 10000) == AT_SUCCESS) {

        /* // for debugging purpose: print first 20 pixels; remmeber to alter _buffer = _buffer - 30;
        std::cout << "Print out of first 20 pixels " << std::endl;
        //Unpack the 12 bit packed data and print out first 20 pixels of data
        for (int i = 0; i < 3 * 10; i += 3) {
            std::cout << "loop " << i << " ; &Buffer: " << &Buffer << std::endl;
            AT_64 LowPixel = EXTRACTLOWPACKED(Buffer);
            AT_64 HighPixel = EXTRACTHIGHPACKED(Buffer);
            std::cout << HighPixel << std::endl << LowPixel << std::endl;
            Buffer += 3;
        }
        */

        //Unpack data using extractor function
        if (imgobj.PixelEncodingIndex == 1) {
            //Mono12Packed
            extract2from3single(Buffer, imgobj.pImageArraySingle, (imgobj.iAOIHeight * imgobj.iAOIWidth)); // took 10ms for 2560x2160 single frame
        }
        else {
            //Mono12 or Mono16
            extractMono12or16single(Buffer, imgobj.pImageArraySingle, (imgobj.iAOIHeight * imgobj.iAOIWidth));
        }

        /*
        int numnegone, numlarge, numzero;
        numnegone = 0;
        numlarge = 0;
        numzero = 0;
        for (int i = 0; i < imgobj.ArraySizeSingle; i++) {
            if (*(imgobj.pImageArraySingle + i) == -1.0) {
                numnegone++;
            }
            if (*(imgobj.pImageArraySingle + i) > 10000) {
                numlarge++;
            }
            if (*(imgobj.pImageArraySingle + i) == 0) {
                numzero++;
            }
        }
        std::cout << "SINGLE FRAME before copying array interval to java" << std::endl;
        std::cout << "number element with -1: " << numnegone << std::endl;
        std::cout << "number element large > 10,000: " << numlarge << std::endl;
        std::cout << "number element == 0: " << numzero << std::endl;
        */

        //Free the allocated buffer
        delete[] UserBuffer;
    }

    jshortArray outArray;
    outArray = env->NewShortArray(imgobj.ArraySizeSingle);
    env->SetShortArrayRegion(outArray, 0, imgobj.ArraySizeSingle, imgobj.pImageArraySingle);
    imgobj.freeImArraySingle();

    //Stop the Acquisition
    AT_Command(imgobj.Hndl, L"Acquisition Stop");

    // flush any remaining buffer that has been queued using AT_QueueBuffer; should only be alled after L"Acquisition Stop"
    AT_Flush(imgobj.Hndl);

    //AT_Close(imgobj.Hndl);// this should only be called after the program ends
    //AT_FinaliseLibrary(); // this should only be called after the program ends

    return outArray;
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_setParameterInfiniteLoopSDK3(JNIEnv* env, jclass cls, jint size_b, jint totalFrame, jint transferInterval, jdouble exposureTime, jint width, jint height, jint left, jint top, jint incamerabin, jint iPixelEncoding, jint arraysize, jint auxOutSource, jint isOverlap) {
    imgobj.temparraysize = arraysize;
    imgobj.size_b = arraysize - (arraysize % (width * height));
    imgobj.size_b /= (width * height);
    imgobj.totalFrame = totalFrame;
    imgobj.intervalFrame = transferInterval;
    imgobj.iExposureTime = exposureTime;
    imgobj.iAOIHBin = incamerabin;
    imgobj.iAOIVBin = incamerabin;
    imgobj.iAOIWidth = width;
    imgobj.iAOIHeight = height;
    imgobj.iAOILeft = ((AT_64)left - 1) * imgobj.iAOIHBin + 1;
    imgobj.iAOITop = ((AT_64)top - 1) * imgobj.iAOIVBin + 1; 
    imgobj.iFrameRate = 1 / exposureTime;
    imgobj.PixelEncodingIndex = iPixelEncoding;
    imgobj.iOverlap = isOverlap;
    if (iPixelEncoding == 0) {
        imgobj.iPixelEncoding = L"Mono12";
        imgobj.iGainMode = L"Fastest frame rate (12-bit)";
        imgobj.iPixelReadoutRate = L"200MHz";
    }
    else if (iPixelEncoding == 1) {
        imgobj.iPixelEncoding = L"Mono12Packed";
        imgobj.iGainMode = L"Fastest frame rate (12-bit)";
        imgobj.iPixelReadoutRate = L"200MHz";
    } else if (iPixelEncoding == 2) {
        imgobj.iPixelEncoding = L"Mono16";
        imgobj.iGainMode = L"High dynamic range (16-bit)";
        imgobj.iPixelReadoutRate = L"100MHz";
    }

    if (auxOutSource == 0) {
        imgobj.iAuxOutSource = L"FireRow1";
    }
    else if (auxOutSource == 1) {
        imgobj.iAuxOutSource = L"FireRowN";
    }
    else if (auxOutSource == 2) {
        imgobj.iAuxOutSource = L"FireAll";
    }
    else if (auxOutSource == 3) {
        imgobj.iAuxOutSource = L"FireAny";
    }


    setParamInfiniteLoop();
    imgobj.setArraySizeInf();
    imgobj.freeImArrayInf();
    imgobj.InitArrayInf();
    resetImageClsparam();
}

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_runInfiniteLoopSDK3(JNIEnv* env, jclass cls, jshortArray outArray, jobject framecounterObj) {
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

    tk2.setTimeStart();
    int iret = performAcquisition_infiniteLoop(imgobj.Hndl, imgobj.totalFrame, imgobj.intervalFrame, env, outArray, framecounterObj, increment_id, carray, isCopy);
    imgobj.timeelapsed2 += tk2.getTimeElapsed();

    imgobj.freeImArrayInf();

    if (iret == 0) {
        AT_Command(imgobj.Hndl, L"AcquisitionStop");
        AT_Flush(imgobj.Hndl);
        deleteBuffers();
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

JNIEXPORT void JNICALL Java_directCameraReadout_andorsdk3v2_AndorSDK3v2_setStopMechanismSDK3(JNIEnv* env, jclass cls, jboolean isStoppressed) {
    imgobj.isStopPressed = isStoppressed;
    return;
}




#endif