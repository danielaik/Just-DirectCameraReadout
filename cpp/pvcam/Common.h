/*
 * Copyright by Photometrics
 */
#pragma once
#ifndef COMMON_H_
#define COMMON_H_

 // System
#include <condition_variable>
#include <mutex>
#include <string>
#include <vector>

// PVCAM
#include <master.h>
#include <pvcam.h>

// The code returned when application exits on error
#define APP_EXIT_ERROR 1

/*
 * Common data types
 */

typedef struct SampleContext
{
    int myData1;
    int myData2;
}
SampleContext;

 // Name-Value Pair type - an item in enumeration type
typedef struct NVP
{
    int32 value;
    std::string name;
}
NVP;
// Name-Value Pair Container type - an enumeration type
typedef std::vector<NVP> NVPC;

// Each camera has one or more ports, this structure holds information with port
// descriptions. Each camera port has one or more speeds (readout frequencies).
// On most EM cameras there are two ports - one EM and one non-EM port with one
// or two speeds per port.
// On non-EM camera there is usually one port only with multiple speeds.
typedef struct READOUT_OPTION
{
    NVP port;
    int16 speedIndex;
    float readoutFrequency;
    int16 bitDepth;
    std::vector<int16> gains;
}
READOUT_OPTION;

/*
 * Declaration of common global variables
 */

 // Vector of camera readout options
extern std::vector<READOUT_OPTION> g_SpeedTable;

// Camera handle
extern int16 g_hCam;

// Number of cameras
extern int16 g_NrOfCameras;

// Camera serial and parallel resolution
extern uns16 g_SensorResX;
extern uns16 g_SensorResY;

// Camera name
extern char g_Camera0_Name[CAM_NAME_LEN];

// Chip name
extern  char chipName[CCD_NAME_LEN];

// control flow
extern rs_bool s_IsPvcamInitialized;

extern rs_bool s_IsCameraOpen;

// Sensor type (if not Frame Transfer then camera is most likely Interline)
extern rs_bool g_IsFrameTransfer;

// Flag marking camera Smart Streaming capable
extern rs_bool g_IsSmartStreaming;

// Sensor region to be used for the acquisition
extern rgn_type g_Region;

// Following 3 variables are platform-independent replacement for Windows event
// Mutex that guards all non-atomic g_Eof* variables
extern std::mutex g_EofMutex;
// Condition, the acquisition thread waits on, for handling new frame
extern std::condition_variable g_EofCond;
// New frame flag that helps with spurious wakeups.
// For really fast acquisitions is better to replace this flag with some queue
// storing new frame address and frame info delayed processing.
// Otherwise some frames might be lost.
extern bool g_EofFlag;

// Frame info structure used to store data in EOF callbacks
extern FRAME_INFO* g_pFrameInfo;

// Smart Streaming structure used to store an array of exposures
extern smart_stream_type* g_pSmartStreamStruct;

/*
 * Common function prototypes
 */

 // Converts string to double precision float number
bool StrToDouble(const std::string& str, double& number);

// Converts string to unsigned integer number
bool StrToInt(const std::string& str, int& number);

// Returns true and a value of selected item assigned to NVP item.
// When user enters nothing or anything but not valid value, this function
// returns false.
bool GetMenuSelection(const std::string& title, const NVPC& menu,
    int32& selection);

// Reads a string from standard input until Enter/Return key is pressed
std::string WaitForInput();

// Displays application name and version
bool ShowAppInfo(int argc, char* argv[]);

// Retrieves last PVCAM error code and displays an error message
void PrintErrorMessage(int16 errorCode, const char* message);

// Initializes PVCAM library, gets basic camera availability information,
// opens the camera and retrieves basic camera parameters and characteristics.
bool InitAndOpenFirstCamera();

// Closes the camera and uninitializes PVCAM
void CloseCameraAndUninit();

// Checks parameter availability
bool IsParamAvailable(uns32 paramID, const char* paramName);

// Reads name-value pairs for given PVCAM enumeration
bool ReadEnumeration(NVPC* nvpc, uns32 paramID, const char* paramName);

// "Shows" the image (prints pixel values, opens external image viewer, ...)
// The title is optional and can be NULL
void ShowImage(uns16* buffer, uns32 bufferSize, const char* title);

// Saves the image pixels to a file.
// The image is saved as raw data, however it can be imported into ImageJ
// or other application that allows importing raw data.
// For ImageJ use drag & drop or File->Import->Raw and then specify
// 16-bit unsigned type, width & height and Little-endian byte order.
bool SaveImage(uns16* buffer, uns32 bufferSize, const char* path);

// Prints the Extended metadata to console output, this function is
// called from PrintMetaFrame() and PrintMetaRoi() as well
void PrintMetaExtMd(void* pMetaData, uns32 metaDataSize);

// Prints the ROI descriptor to console output, this function is
// called from PrintMetaFrame() as well
void PrintMetaRoi(md_frame_roi* pRoiDesc);

// Prints the frame descriptor to console output including ROIs
// and Extended metadata structures.
// If printAllRois is false only first ROI is printed out.
void PrintMetaFrame(md_frame* pFrameDesc, bool printAllRois);

// Reads number from console window
void ConsoleReadNumber(int& out, int min, int max, int def);

// If Smart Streaming is supported enable it and load given exposures to camera.
// If this call succeeds do not forget to release g_pSmartStreamStruct and set
// it to NULL when not needed. Otherwise it is released in CloseCameraAndUninit
// function.
bool UploadSmartStreamingExposures(const uns32* exposures, uns16 exposuresCount);

int InitPVCAM();

void CloseCameraAndUninit();

int GetCameraInfos();

// Function that gets called from PVCAM when EOF event arrives
void PV_DECL NewFrameHandler(FRAME_INFO* pFrameInfo, void* context);

#endif // COMMON_H_

