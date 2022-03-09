/*
 * Copyright by Photometrics
 */
#include "Common.h"

 // System
#include <assert.h>
#include <algorithm> // std::min
#include <cstring> // strlen
#include <fstream>
#include <iostream>
#include <limits>
#include <sstream>

// Local
#include "version.h"

/*
 * Definition of common global variables
 */

std::vector<READOUT_OPTION> g_SpeedTable;
int16 g_hCam = -1;
int16 g_NrOfCameras = 0;
uns16 g_SensorResX = 0;
uns16 g_SensorResY = 0;
char g_Camera0_Name[CAM_NAME_LEN] = "";
char chipName[CCD_NAME_LEN];
rs_bool g_IsFrameTransfer = FALSE;
rs_bool g_IsSmartStreaming = FALSE;
rgn_type g_Region = { 0, 0, 0, 0, 0, 0 };
std::mutex g_EofMutex;
std::condition_variable g_EofCond;
bool g_EofFlag = false;
FRAME_INFO* g_pFrameInfo = NULL;
smart_stream_type* g_pSmartStreamStruct = NULL;

/*
 * Module local variables
 */

rs_bool s_IsPvcamInitialized = FALSE;
rs_bool s_IsCameraOpen = FALSE;

/*
 * Common function implementations
 */

bool StrToDouble(const std::string& str, double& number)
{
    try
    {
        size_t idx;
        number = std::stod(str, &idx);
        if (idx == str.length())
            return true;
    }
    catch (...) {};
    return false;
}

bool StrToInt(const std::string& str, int& number)
{
    try
    {
        size_t idx;
        long nr = std::stoul(str, &idx);
        if (idx == str.length()
            && nr >= (std::numeric_limits<int>::min)()
            && nr <= (std::numeric_limits<int>::max)())
        {
            number = (int)nr;
            return true;
        }
    }
    catch (...) {};
    return false;
}

// Returns true and a value of selected item assigned to NVP item.
// When user enters nothing or anything but not valid value, this function
// returns false.
bool GetMenuSelection(const std::string& title, const NVPC& menu,
    int32& selection)
{
    const std::string underline(title.length() + 1, '-');
    printf("\n%s:\n%s\n", title.c_str(), underline.c_str());
    for (size_t n = 0; n < menu.size(); n++)
        std::cout << menu[n].value << ") " << menu[n].name << std::endl;
    printf("Type your choice and press Enter: ");
    const std::string input = WaitForInput();
    printf("\n");

    if (!StrToInt(input, selection))
        return false;
    for (size_t n = 0; n < menu.size(); n++)
        if (menu[n].value == selection)
            return true;
    return false;
}

std::string WaitForInput()
{
    std::string str;
    std::cin.clear();
    getline(std::cin, str);
    return str;
}

bool ShowAppInfo(int argc, char* argv[])
{
    const char* appName = "<unable to get name>";
    if (argc > 0 && argv != NULL && argv[0] != NULL)
        appName = argv[0];

    // Read PVCAM library version
    uns16 pvcamVersion;
    if (PV_OK != pl_pvcam_get_ver(&pvcamVersion))
    {
        PrintErrorMessage(pl_error_code(), "pl_pvcam_get_ver() error");
        return false;
    }

    printf("************************************************************\n");
    printf("Application  : %s\n", appName);
    printf("App. version : %d.%d.%d\n",
        VERSION_MAJOR,
        VERSION_MINOR,
        VERSION_BUILD);
    printf("PVCAM version: %d.%d.%d\n",
        (pvcamVersion >> 8) & 0xFF,
        (pvcamVersion >> 4) & 0x0F,
        (pvcamVersion >> 0) & 0x0F);
    printf("************************************************************\n\n");

    return true;
}

void PrintErrorMessage(int16 errorCode, const char* message)
{
    char pvcamErrMsg[ERROR_MSG_LEN];
    pl_error_message(errorCode, pvcamErrMsg);
    printf("%s\nError code: %d\nError message: %s\n", message, errorCode, pvcamErrMsg);
}

int InitPVCAM()
{

    // Initialize PVCAM library
    if (PV_OK != pl_pvcam_init())
    {
        PrintErrorMessage(pl_error_code(), "pl_pvcam_init() error");
        return pl_error_code();
    }
    s_IsPvcamInitialized = TRUE;
    printf("PVCAM initialized\n");

    // Read number of cameras in the system.
    // This will return total number of PVCAM cameras regardless of interface.
    if (PV_OK != pl_cam_get_total(&g_NrOfCameras))
    {
        PrintErrorMessage(pl_error_code(), "pl_cam_get_total() error");
        g_NrOfCameras = 0;
        return pl_error_code();
    }

    // Get PVCAM-name of camera 0 (can be modified via RSConfig)
    if (PV_OK != pl_cam_get_name(0, g_Camera0_Name))
    {
        PrintErrorMessage(pl_error_code(), "pl_cam_get_name() error");
        return pl_error_code();
    }
    printf("Camera 0 name: %s\n", g_Camera0_Name);

    return 0;
}

int GetCameraInfos() {
    // Open camera with the specified camera name obtained in InitPVCAM() function
   // Error 195 = third party connecting same camera
    if (PV_OK != pl_cam_open(g_Camera0_Name, &g_hCam, OPEN_EXCLUSIVE))
    {
        PrintErrorMessage(pl_error_code(), "pl_cam_open() error");
        return pl_error_code();
    }
    s_IsCameraOpen = TRUE;
    printf("Camera %s opened\n", g_Camera0_Name);

    // get chip name
    if (PV_OK != pl_get_param(g_hCam, PARAM_CHIP_NAME, ATTR_CURRENT,
        (void*)chipName))
    {
        assert((false) && "error retrieving chip name");
    }
    printf("Sensor chip name: %s\n", chipName);
    return 0;
}

bool OpenCamera()
{
    // Open camera with the specified camera name obtained in InitPVCAM() function
    if (PV_OK != pl_cam_open(g_Camera0_Name, &g_hCam, OPEN_EXCLUSIVE))
    {
        PrintErrorMessage(pl_error_code(), "pl_cam_open() error");
        return false;
    }
    s_IsCameraOpen = TRUE;
    printf("Camera %s opened\n", g_Camera0_Name);

    // Read the version of Device Driver
    if (!IsParamAvailable(PARAM_DD_VERSION, "PARAM_DD_VERSION"))
        return false;
    uns16 ddVersion;
    if (PV_OK != pl_get_param(g_hCam, PARAM_DD_VERSION, ATTR_CURRENT,
        (void*)&ddVersion))
    {
        PrintErrorMessage(pl_error_code(), "pl_get_param(PARAM_DD_VERSION) error");
        return false;
    }
    printf("Device driver version: %d.%d.%d\n",
        (ddVersion >> 8) & 0xFF,
        (ddVersion >> 4) & 0x0F,
        (ddVersion >> 0) & 0x0F);

    // Get camera chip name string. Typically holds both chip and camera model
    // name, therefore is the best camera identifier for most models
    if (!IsParamAvailable(PARAM_CHIP_NAME, "PARAM_CHIP_NAME"))
        return false;
    char chipName[CCD_NAME_LEN];
    if (PV_OK != pl_get_param(g_hCam, PARAM_CHIP_NAME, ATTR_CURRENT,
        (void*)chipName))
    {
        PrintErrorMessage(pl_error_code(), "pl_get_param(PARAM_CHIP_NAME) error");
        return false;
    }
    printf("Sensor chip name: %s\n", chipName);

    // Get camera firmware version
    if (!IsParamAvailable(PARAM_CAM_FW_VERSION, "PARAM_CAM_FW_VERSION"))
        return false;
    uns16 fwVersion;
    if (PV_OK != pl_get_param(g_hCam, PARAM_CAM_FW_VERSION, ATTR_CURRENT,
        (void*)&fwVersion))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_get_param(PARAM_CAM_FW_VERSION) error");
        return false;
    }
    printf("Camera firmware version: %d.%d\n",
        (fwVersion >> 8) & 0xFF,
        (fwVersion >> 0) & 0xFF);

    // Find out if the sensor is a frame transfer or other (typically interline)
    // type. This is a two-step process.
    // Please, follow the procedure below in your applications.
    if (PV_OK != pl_get_param(g_hCam, PARAM_FRAME_CAPABLE, ATTR_AVAIL,
        (void*)&g_IsFrameTransfer))
    {
        g_IsFrameTransfer = 0;
        PrintErrorMessage(pl_error_code(),
            "pl_get_param(PARAM_FRAME_CAPABLE) error");
        return false;
    }

    if (g_IsFrameTransfer == TRUE)
    {
        if (PV_OK != pl_get_param(g_hCam, PARAM_FRAME_CAPABLE, ATTR_CURRENT,
            (void*)&g_IsFrameTransfer))
        {
            g_IsFrameTransfer = 0;
            PrintErrorMessage(pl_error_code(),
                "pl_get_param(PARAM_FRAME_CAPABLE) error");
            return false;
        }
        if (g_IsFrameTransfer == TRUE)
            printf("Camera with Frame Transfer capability sensor\n");
    }
    if (g_IsFrameTransfer == FALSE)
    {
        g_IsFrameTransfer = 0;
        printf("Camera without Frame Transfer capability sensor\n");
    }

    // If this is a Frame Transfer sensor set PARAM_PMODE to PMODE_FT.
    // The other common mode for these sensors is PMODE_ALT_FT.
    if (!IsParamAvailable(PARAM_PMODE, "PARAM_PMODE"))
        return false;
    if (g_IsFrameTransfer == TRUE)
    {
        int32 PMode = PMODE_FT;
        if (PV_OK != pl_set_param(g_hCam, PARAM_PMODE, (void*)&PMode))
        {
            PrintErrorMessage(pl_error_code(), "pl_set_param(PARAM_PMODE) error");
            return false;
        }
    }
    // If not a Frame Transfer sensor (i.e. Interline), set PARAM_PMODE to
    // PMODE_NORMAL, or PMODE_ALT_NORMAL.
    else
    {
        int32 PMode = PMODE_NORMAL;
        if (PV_OK != pl_set_param(g_hCam, PARAM_PMODE, (void*)&PMode))
        {
            PrintErrorMessage(pl_error_code(), "pl_set_param(PARAM_PMODE) error");
            return false;
        }
    }

    printf("\n");

    // This code iterates through all available camera ports and their readout
    // speeds and creates a Speed Table which holds indices of ports and speeds,
    // readout frequencies and bit depths.

    NVPC ports;
    if (!ReadEnumeration(&ports, PARAM_READOUT_PORT, "PARAM_READOUT_PORT"))
        return false;

    if (!IsParamAvailable(PARAM_SPDTAB_INDEX, "PARAM_SPDTAB_INDEX"))
        return false;
    if (!IsParamAvailable(PARAM_PIX_TIME, "PARAM_PIX_TIME"))
        return false;
    if (!IsParamAvailable(PARAM_BIT_DEPTH, "PARAM_BIT_DEPTH"))
        return false;

    // Iterate through available ports and their speeds
    for (size_t pi = 0; pi < ports.size(); pi++)
    {
        // Set readout port
        if (PV_OK != pl_set_param(g_hCam, PARAM_READOUT_PORT,
            (void*)&ports[pi].value))
        {
            PrintErrorMessage(pl_error_code(),
                "pl_set_param(PARAM_READOUT_PORT) error");
            return false;
        }

        // Get number of available speeds for this port
        uns32 speedCount;
        if (PV_OK != pl_get_param(g_hCam, PARAM_SPDTAB_INDEX, ATTR_COUNT,
            (void*)&speedCount))
        {
            PrintErrorMessage(pl_error_code(),
                "pl_get_param(PARAM_SPDTAB_INDEX) error");
            return false;
        }

        // Iterate through all the speeds
        for (int16 si = 0; si < (int16)speedCount; si++)
        {
            // Set camera to new speed index
            if (PV_OK != pl_set_param(g_hCam, PARAM_SPDTAB_INDEX, (void*)&si))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_set_param(g_hCam, PARAM_SPDTAB_INDEX) error");
                return false;
            }

            // Get pixel time (readout time of one pixel in nanoseconds) for the
            // current port/speed pair. This can be used to calculate readout
            // frequency of the port/speed pair.
            uns16 pixTime;
            if (PV_OK != pl_get_param(g_hCam, PARAM_PIX_TIME, ATTR_CURRENT,
                (void*)&pixTime))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_get_param(g_hCam, PARAM_PIX_TIME) error");
                return false;
            }

            // Get bit depth of the current readout port/speed pair
            int16 bitDepth;
            if (PV_OK != pl_get_param(g_hCam, PARAM_BIT_DEPTH, ATTR_CURRENT,
                (void*)&bitDepth))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_get_param(PARAM_BIT_DEPTH) error");
                return false;
            }

            int16 gainMin;
            if (PV_OK != pl_get_param(g_hCam, PARAM_GAIN_INDEX, ATTR_MIN,
                (void*)&gainMin))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_get_param(PARAM_GAIN_INDEX) error");
                return false;
            }

            int16 gainMax;
            if (PV_OK != pl_get_param(g_hCam, PARAM_GAIN_INDEX, ATTR_MAX,
                (void*)&gainMax))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_get_param(PARAM_GAIN_INDEX) error");
                return false;
            }

            int16 gainIncrement;
            if (PV_OK != pl_get_param(g_hCam, PARAM_GAIN_INDEX, ATTR_INCREMENT,
                (void*)&gainIncrement))
            {
                PrintErrorMessage(pl_error_code(),
                    "pl_get_param(PARAM_GAIN_INDEX) error");
                return false;
            }

            // Save the port/speed information to our Speed Table
            READOUT_OPTION ro;
            ro.port = ports[pi];
            ro.speedIndex = si;
            ro.readoutFrequency = 1000 / (float)pixTime;
            ro.bitDepth = bitDepth;
            ro.gains.clear();

            int16 gainValue = gainMin;

            while (gainValue <= gainMax)
            {
                ro.gains.push_back(gainValue);
                gainValue += gainIncrement;
            }

            g_SpeedTable.push_back(ro);

            printf("g_SpeedTable[%lu].Port = %ld (%d - %s)\n",
                (unsigned long)(g_SpeedTable.size() - 1),
                (unsigned long)pi,
                ro.port.value,
                ro.port.name.c_str());
            printf("g_SpeedTable[%lu].SpeedIndex = %d\n",
                (unsigned long)(g_SpeedTable.size() - 1),
                ro.speedIndex);
            printf("g_SpeedTable[%lu].PortReadoutFrequency = %.3f MHz\n",
                (unsigned long)(g_SpeedTable.size() - 1),
                ro.readoutFrequency);
            printf("g_SpeedTable[%lu].bitDepth = %d bit\n",
                (unsigned long)(g_SpeedTable.size() - 1),
                ro.bitDepth);
            for (int16 gi = 0; gi < (int16)ro.gains.size(); gi++)
            {
                printf("g_SpeedTable[%lu].gains[%d] = %d \n",
                    (unsigned long)(g_SpeedTable.size() - 1),
                    (int)gi,
                    ro.gains[gi]);
            }
            printf("\n");
        }
    }

    // Speed Table has been created

    // Set camera to first port
    if (PV_OK != pl_set_param(g_hCam, PARAM_READOUT_PORT,
        (void*)&g_SpeedTable[0].port.value))
    {
        PrintErrorMessage(pl_error_code(), "Readout port could not be set");
        return false;
    }
    printf("Setting readout port to %s\n", g_SpeedTable[0].port.name.c_str());

    // Set camera to speed 0
    if (PV_OK != pl_set_param(g_hCam, PARAM_SPDTAB_INDEX,
        (void*)&g_SpeedTable[0].speedIndex))
    {
        PrintErrorMessage(pl_error_code(), "Readout port could not be set");
        return false;
    }
    printf("Setting readout speed index to %d\n", g_SpeedTable[0].speedIndex);

    // Set gain index to one (the first one)
    if (PV_OK != pl_set_param(g_hCam, PARAM_GAIN_INDEX,
        (void*)&g_SpeedTable[0].gains[0]))
    {
        PrintErrorMessage(pl_error_code(), "Gain index could not be set");
        return false;
    }
    printf("Setting gain index to %d\n", g_SpeedTable[0].gains[0]);

    printf("\n");

    // Get number of sensor columns
    if (!IsParamAvailable(PARAM_SER_SIZE, "PARAM_SER_SIZE"))
        return false;
    if (PV_OK != pl_get_param(g_hCam, PARAM_SER_SIZE, ATTR_CURRENT,
        (void*)&g_SensorResX))
    {
        PrintErrorMessage(pl_error_code(), "Couldn't read CCD X-resolution");
        return false;
    }
    // Get number of sensor lines
    if (!IsParamAvailable(PARAM_PAR_SIZE, "PARAM_PAR_SIZE"))
        return false;
    if (PV_OK != pl_get_param(g_hCam, PARAM_PAR_SIZE, ATTR_CURRENT,
        (void*)&g_SensorResY))
    {
        PrintErrorMessage(pl_error_code(), "Couldn't read CCD Y-resolution");
        return false;
    }
    printf("Sensor size: %dx%d\n", g_SensorResX, g_SensorResY);

    // Set number of sensor clear cycles to 2 (default)
    if (!IsParamAvailable(PARAM_CLEAR_CYCLES, "PARAM_CLEAR_CYCLES"))
        return false;
    uns16 ClearCycles = 2;
    if (PV_OK != pl_set_param(g_hCam, PARAM_CLEAR_CYCLES, (void*)&ClearCycles))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_set_param(PARAM_CLEAR_CYCLES) error");
        return false;
    }

    // Check Smart Streaming support on the camera.
    // We do not exit application here if the parameter is unavailable as this
    // parameter is available on Evolve-512 and Evolve-512 Delta only at the
    // moment.
    // We do not use IsParamAvailable function as it print error messages
    // unwanted here.
    if (PV_OK != pl_get_param(g_hCam, PARAM_SMART_STREAM_MODE, ATTR_AVAIL,
        (void*)&g_IsSmartStreaming))
    {
        PrintErrorMessage(pl_error_code(),
            "Smart streaming availability check failed");
        return false;
    }
    if (g_IsSmartStreaming == TRUE)
        printf("Smart Streaming is available\n");
    else
        printf("Smart Streaming not available\n");

    printf("\n");

    return true;
}

bool InitAndOpenFirstCamera()
{
    if (InitPVCAM() != 0)
    {
        CloseCameraAndUninit();
        return false;
    }

    if (!OpenCamera())
    {
        CloseCameraAndUninit();
        return false;
    }

    // Create this structure that will be used to received extended information
    // about the frame.
    // Support on interfaces may vary, full support on Firewire at the moment,
    // partial support on PCIe LVDS and USB interfaces, no support on legacy
    // LVDS.
    // FRAME_INFO can be allocated on stack bu ve demonstrate here how to do the
    // same on heap.
    if (PV_OK != pl_create_frame_info_struct(&g_pFrameInfo))
    {
        PrintErrorMessage(pl_error_code(), "pl_create_frame_info_struct() error");
        CloseCameraAndUninit();
        return false;
    }

    return true;
}

void CloseCameraAndUninit()
{

    //// Findout why error when reopening (error 56)
    //// Release Smart Streaming structure
    //if (g_pSmartStreamStruct != NULL)
    //    if (PV_OK != pl_release_smart_stream_struct(&g_pSmartStreamStruct))
    //        PrintErrorMessage(pl_error_code(), "pl_release_smart_stream_struct() error");

    //// Release frame info
    //if (g_pFrameInfo != NULL)
    //    if (PV_OK != pl_release_frame_info_struct(g_pFrameInfo))
    //        PrintErrorMessage(pl_error_code(), "pl_release_frame_info_struct() error");


    // Do not close camera if none has been detected and open
    if (s_IsCameraOpen == TRUE)
    {
        if (PV_OK != pl_cam_close(g_hCam))
            PrintErrorMessage(pl_error_code(), "pl_cam_close() error");
        else
            s_IsCameraOpen = FALSE;
            printf("Camera closed\n");
    }

    // Uninitialize PVCAM library
    if (s_IsPvcamInitialized == TRUE)
    {
        if (PV_OK != pl_pvcam_uninit())
            PrintErrorMessage(pl_error_code(), "pl_pvcam_uninit() error");
        else
            s_IsPvcamInitialized = FALSE;
            printf("PVCAM uninitializated\n");
    }


}

bool IsParamAvailable(uns32 paramID, const char* paramName)
{
    if (paramName == NULL)
        return false;

    rs_bool isAvailable;
    if (PV_OK != pl_get_param(g_hCam, paramID, ATTR_AVAIL, (void*)&isAvailable))
    {
        printf("Error reading ATTR_AVAIL of %s\n", paramName);
        return false;
    }
    if (isAvailable == FALSE)
    {
        printf("Parameter %s is not available\n", paramName);
        return false;
    }

    return true;
}

bool ReadEnumeration(NVPC* nvpc, uns32 paramID, const char* paramName)
{
    if (nvpc == NULL || paramName == NULL)
        return false;

    if (!IsParamAvailable(paramID, paramName))
        return false;

    uns32 count;
    if (PV_OK != pl_get_param(g_hCam, paramID, ATTR_COUNT, (void*)&count))
    {
        const std::string msg =
            "pl_get_param(" + std::string(paramName) + ") error";
        PrintErrorMessage(pl_error_code(), msg.c_str());
        return false;
    }

    // Actually get the triggering/exposure names
    for (uns32 i = 0; i < count; ++i)
    {
        // Ask how long the string is
        uns32 strLength;
        if (PV_OK != pl_enum_str_length(g_hCam, paramID, i, &strLength))
        {
            const std::string msg =
                "pl_enum_str_length(" + std::string(paramName) + ") error";
            PrintErrorMessage(pl_error_code(), msg.c_str());
            return false;
        }

        // Allocate the destination string
        char* name = new (std::nothrow) char[strLength];

        // Actually get the string and value
        int32 value;
        if (PV_OK != pl_get_enum_param(g_hCam, paramID, i, &value, name, strLength))
        {
            const std::string msg =
                "pl_get_enum_param(" + std::string(paramName) + ") error";
            PrintErrorMessage(pl_error_code(), msg.c_str());
            delete[] name;
            return false;
        }

        NVP nvp;
        nvp.value = value;
        nvp.name = name;
        nvpc->push_back(nvp);

        delete[] name;
    }

    return !nvpc->empty();
}

void ShowImage(uns16* buffer, uns32 bufferSize, const char* title)
{
    std::string subTitle;
    if (title != NULL && strlen(title) > 0)
        subTitle += std::string(", ") + title;

    // Print first up to 5 pixel values
    const uns32 pixelCount = std::min<uns32>(5, bufferSize);

    printf("First %d pixel values of the frame%s: ",
        pixelCount, subTitle.c_str());

    std::ostringstream pixels;
    for (uns32 n = 0; n < pixelCount; n++)
    {
        if (n > 0)
            pixels << " - ";
        pixels << *(buffer + n);
    }
    printf("%s\n", pixels.str().c_str());
}

bool SaveImage(uns16* buffer, uns32 bufferSize, const char* path)
{
    std::ofstream stream(path, std::ios::binary);
    if (!stream.is_open())
    {
        printf("Unable to open '%s' for writing\n", path);
        return false;
    }
    try
    {
        stream.write(reinterpret_cast<const char*>(buffer), bufferSize * sizeof(uns16));
    }
    catch (const std::exception& e)
    {
        printf("Failed to write data to file:\n");
        printf("%s\n", e.what());
        return false;
    }
    return true;
}

void PrintMetaExtMd(void* pMetaData, uns32 metaDataSize)
{
    printf("============================= EXTENDED ROI METADATA ===========================\n");
    md_ext_item_collection extMdCol;
    if (PV_OK != pl_md_read_extended(&extMdCol, pMetaData, metaDataSize))
    {
        PrintErrorMessage(pl_error_code(), "pl_md_read_extended() error");
        return;
    }

    for (int i = 0; i < extMdCol.count; ++i)
    {
        std::stringstream str;
        str << " TAG " << extMdCol.list[i].tagInfo->tag << ": "
            << "'" << extMdCol.list[i].tagInfo->name << "': ";
        switch (extMdCol.list[i].tagInfo->type)
        {
        case TYPE_UNS32:
            str << *(uns32*)extMdCol.list[i].value;
            break;
        case TYPE_UNS8:
            str << (int)*(uns8*)extMdCol.list[i].value;
            break;
        case TYPE_UNS16:
            str << *(uns16*)extMdCol.list[i].value;
            break;
        case TYPE_FLT64:
            str << *(flt64*)extMdCol.list[i].value;
            break;
        default:
            str << "Unsupported value type ("
                << extMdCol.list[i].tagInfo->type << ")";
            break;
        }
        str << std::endl;

        printf("%s", str.str().c_str());
    }
}

void PrintMetaRoi(md_frame_roi* pRoiDesc)
{
    printf("================================ ROI DESCRIPTOR ===============================\n");
    printf(" DataSize:%u, ExtMdDataSize:%u\n",
        pRoiDesc->dataSize, pRoiDesc->extMdDataSize);
    printf("================================== ROI HEADER =================================\n");
    md_frame_roi_header* pRoiHdr = pRoiDesc->header;
    const rgn_type& roi = pRoiHdr->roi;
    printf(" RoiNr:%u, Roi:[%u,%u,%u,%u,%u,%u]\n",
        pRoiHdr->roiNr, roi.s1, roi.s2, roi.sbin, roi.p1, roi.p2, roi.pbin);
    printf("TimestampBOR:%u, TimestampEOR:%u, ExtendedMdSize:%u, Flags:0x%1x\n",
        pRoiHdr->timestampBOR, pRoiHdr->timestampEOR,
        pRoiHdr->extendedMdSize, pRoiHdr->flags);
    if (pRoiDesc->extMdDataSize > 0)
        PrintMetaExtMd(pRoiDesc->extMdData, pRoiDesc->extMdDataSize);
    printf("=================================== ROI DATA ==================================\n");
    const uns16* pRoiPixels = static_cast<uns16*>(pRoiDesc->data);
    const uns32 roiPixelCount = pRoiDesc->dataSize / sizeof(uns16);
    const uns32 countPerLine = 80 / 6; // 13 "65535"s (inc space) will fit on a line of 80 chars
    const uns32 maxPrintCount = 2 * countPerLine; // Max number of lines to print
    const int printCount = (std::min)(roiPixelCount, maxPrintCount);
    for (int i = 0; i < printCount; ++i)
    {
        printf(" %05u", pRoiPixels[i]);
        if (i == countPerLine - 1)
            printf("\n");
    }
    printf("\n");
}

void PrintMetaFrame(md_frame* pFrameDesc, bool printAllRois)
{
    const rgn_type& impRoi = pFrameDesc->impliedRoi;
    const md_frame_header* pFrameHdr = pFrameDesc->header;
    printf("=============================== FRAME DESCRIPTOR ==============================\n");
    printf(" RoiCount:%u, Implied ROI:[%u,%u,%u,%u,%u,%u]\n",
        pFrameDesc->roiCount, impRoi.s1, impRoi.s2, impRoi.sbin, impRoi.p1,
        impRoi.p2, impRoi.pbin);
    printf("================================= FRAME HEADER ================================\n");
    printf(" FrameNr:%03u, RoiCount:%03u, BitDepth:%u, Version:%u, Sig:%u, Flags:0x%1x\n",
        pFrameHdr->frameNr, pFrameHdr->roiCount, pFrameHdr->bitDepth,
        pFrameHdr->version, pFrameHdr->signature, pFrameHdr->flags);
    printf(" ExpTime:%u, ExpTimeResNs:%u, TimestampBOF:%u, TimestampEOF:%u\n",
        pFrameHdr->exposureTime, pFrameHdr->exposureTimeResNs,
        pFrameHdr->timestampBOF, pFrameHdr->timestampEOF);
    printf(" TimestampResNs:%u, RoiTimestampResNs:%u, ExtendedMdSz:%u\n",
        pFrameHdr->timestampResNs, pFrameHdr->roiTimestampResNs,
        pFrameHdr->extendedMdSize);
    if (pFrameDesc->extMdDataSize > 0)
        PrintMetaExtMd(pFrameDesc->extMdData, pFrameDesc->extMdDataSize);

    if (printAllRois)
    {
        for (uns32 i = 0; i < pFrameDesc->roiCount; ++i)
            PrintMetaRoi(&pFrameDesc->roiArray[i]);
    }
    else
    {
        PrintMetaRoi(&pFrameDesc->roiArray[0]);
    }

    printf("===============================================================================\n");
}

void ConsoleReadNumber(int& out, int min, int max, int def)
{
    bool bSuccess = false;
    while (!bSuccess)
    {
        printf(" Enter number [%d - %d] or hit <Enter> for default (%d): ",
            min, max, def);
        const std::string numStr = WaitForInput();

        // If zero length is received the user simply pressed <Enter>
        const size_t len = numStr.length();
        if (len == 0)
        {
            out = def;
            return;
        }

        int convertedNum;
        if (!StrToInt(numStr, convertedNum))
        {
            printf("  Not a number. Please retry.\n");
            continue;
        }
        if (convertedNum < min || convertedNum > max)
        {
            printf("  Number out of range. Please retry.\n");
            continue;
        }
        out = convertedNum;
        bSuccess = true;
    }
}

bool UploadSmartStreamingExposures(const uns32* exposures, uns16 exposuresCount)
{
    if (g_IsSmartStreaming == FALSE)
    {
        printf("This camera does not support Smart Streaming\n");
        return false;
    }

    if (exposures == NULL || exposuresCount == 0)
    {
        printf("No exposures given\n");
        return false;
    }

    if (!IsParamAvailable(PARAM_SMART_STREAM_MODE_ENABLED,
        "PARAM_SMART_STREAM_MODE_ENABLED"))
        return false;
    if (!IsParamAvailable(PARAM_SMART_STREAM_EXP_PARAMS,
        "PARAM_SMART_STREAM_EXP_PARAMS"))
        return false;

    // Enable Smart Streaming in the camera
    rs_bool enableSS = TRUE;
    if (PV_OK != pl_set_param(g_hCam, PARAM_SMART_STREAM_MODE_ENABLED,
        (void*)&enableSS))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_set_param(PARAM_SMART_STREAM_MODE_ENABLED) error");
        return false;
    }
    printf("Smart Streaming enabled successfully\n");

    // We have to check what is the total number of supported exposures.
    // That is a bit tricky because the parameter is not numeric but returns
    // smart_stream_type structure. The number we want is stored in its
    // "entries" member.
    // At the time of writing this sample code on Evolve-512 and Evolve-512 Delta
    // this value is 12.
    smart_stream_type maxExposuresStruct;
    if (PV_OK != pl_get_param(g_hCam, PARAM_SMART_STREAM_EXP_PARAMS, ATTR_MAX,
        (void*)&maxExposuresStruct))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_get_param(PARAM_SMART_STREAM_EXP_PARAMS, ATTR_MAX) error");
        return false;
    }

    // Limit the number of exposures if needed
    const uns16 maxExposures =
        (uns16)(std::min<size_t>)(maxExposuresStruct.entries, exposuresCount);

    // Allocate the structure for correct number of exposures
    if (PV_OK != pl_create_smart_stream_struct(&g_pSmartStreamStruct, maxExposures))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_create_smart_stream_struct() error");
        return false;
    }
    // ... and fill it with values
    for (uns16 n = 0; n < maxExposures; n++)
        g_pSmartStreamStruct->params[n] = exposures[n];

    // Send the data to the camera
    if (PV_OK != pl_set_param(g_hCam, PARAM_SMART_STREAM_EXP_PARAMS,
        (void*)g_pSmartStreamStruct))
    {
        PrintErrorMessage(pl_error_code(),
            "pl_set_param(PARAM_SMART_STREAM_EXP_PARAMS) error");
        if (PV_OK != pl_release_smart_stream_struct(&g_pSmartStreamStruct))
            PrintErrorMessage(pl_error_code(),
                "pl_release_smart_stream_struct() error");
        return false;
    }
    printf("Smart Streaming parameters loaded correctly\n");

    return true;
}

void PV_DECL NewFrameHandler(FRAME_INFO* pFrameInfo, void* context) {

    SampleContext ctx = *(SampleContext*)context;
    // Read extended frame information
    g_pFrameInfo->FrameNr = pFrameInfo->FrameNr;
    g_pFrameInfo->TimeStamp = pFrameInfo->TimeStamp;
 /*   printf("Context myData1 = %d\n", ctx.myData1);
    printf("Context myData2 = %d\n", ctx.myData2);*/

    // Unblock main thread
    {
        std::lock_guard<std::mutex> lock(g_EofMutex);
        g_EofFlag = true; // Set flag
    }
    g_EofCond.notify_one();

}
