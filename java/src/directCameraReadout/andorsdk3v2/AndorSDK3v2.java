/*
 * SDK 3 version = 3.15.20005.0
 * Tested on sCMOS SONA-4BV11-CSC-00245-March19
 */
package directCameraReadout.andorsdk3v2;

import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ShortProcessor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.sql.Timestamp;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import directCameraReadout.control.FrameCounter;
import directCameraReadout.control.FrameCounterX;
import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.gui.DirectCapturePanel.Common;
import directCameraReadout.gui.cameraConstant.Common_SONA;
import directCameraReadout.workers.Workers.*;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV2Instant;
import static directCameraReadout.workers.Workers.SynchronizerWorkerInstant;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV3Instant;
import static directCameraReadout.workers.Workers.BufferToStackWorkerInstant;
import static directCameraReadout.workers.Workers.CumulativeACFWorkerV3Instant;
import static directCameraReadout.workers.Workers.ICCSWorkerInstant;
import static directCameraReadout.workers.Workers.NonCumulativeACFWorkerV3Instant;
import static version.VERSION.DCR_VERSION;
import static version.VERSION.SDK3_VERSION;

public class AndorSDK3v2 {

    // NOTE: 
    // DCR_VERSION of the used SDK3 library.
    // DCR_VERSION must be updated when .dll/.so files are changed so that they are placed in a new sub-folder named after this DCR_VERSION num in Fiji.App > jars.
//    public static final String SDK3_VERSION = "v1_1_2";
    private static void printlogdll(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    private static void printlog(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    private static boolean IsSaveExcel = true;
    private static boolean IsOperatingSystemOK = false;
    public static boolean IsLoadingLibraryOK = false;

    //sdk3
    private static final String ATCORE = "atcore.dll";//sdk3
    private static final String ATDEVCHAMCAM = "atdevchamcam.dll";//sdk3
    private static final String ATUTILITY = "atutility.dll";//sdk3
    private static final String ATSPOOLER = "atspooler.dll";//sdk3
    private static final String ATMCD64D = "atmcd64d.dll"; //sdk3

    // 18 dlls need to check Windows/System32
    private static final String UCRTBASED = "ucrtbased.dll"; //dependencies
    private static final String KERNEL32 = "kernel32.dll"; //dependencies
    private static final String VCRUNTIME140D = "vcruntime140d.dll"; //dependencies
    private static final String VCRUNTIME140_1D = "vcruntime140_1d.dll"; //dependencies
    private static final String MSVCP140D = "msvcp140d.dll"; //dependencies
    private static final String VCRUNTIME140 = "vcruntime140.dll"; //dependencies
    private static final String VCRUNTIME140_1 = "vcruntime140_1.dll"; //dependencies
    private static final String MSVCP140 = "msvcp140.dll";
    private static final String SHELL32 = "shell32.dll";
    private static final String MSVCR120 = "msvcr120.dll";
    private static final String MSVCP120 = "msvcp120.dll";
    private static final String GCBASE_MD_VC120_V3_0 = "GCBase_MD_VC120_v3_0.dll";
    private static final String MATHPARSER_MD_VC120_V3_0 = "MathParser_MD_VC120_v3_0.dll";
    private static final String NODEMAPDATA_MD_VC120_V3_0 = "NodeMapData_MD_VC120_v3_0.dll";
    private static final String XMLPARSER_MD_VC120_V3_0 = "XmlParser_MD_VC120_v3_0.dll";
    private static final String LOG_MD_VC120_V3_0 = "Log_MD_VC120_v3_0.dll";
    private static final String WS2_32 = "ws2_32.dll";
    private static final String GENAPI_MD_VC120_V3_0 = "GenApi_MD_VC120_v3_0.dll";

    final static int impwinposx = 1110;
    final static int impwinposy = 125;

    static {

        printlogdll("*********************SDK3***********************");

//        try {
//            System.load("C:\\Users\\Administrator\\Documents\\GitHub\\sdk3_ver2_GitHubRepo\\java\\andorsdk3_v2\\src\\andorsdk3v2\\JNIAndorSDK3v2.dll");
//        } catch (Exception e) {
//            System.out.println("unable to load alert: " + e);
//        }
        // Loading sequences does matter to prevent "can't found dependencies error"
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\atcore.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\atdevsimcam.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\atspooler.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\atutility.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\kernel32.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\msvcp140d.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\ucrtbased.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\vcruntime140_1d.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\vcruntime140d.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\JNIAndorSDK3.dll");
//        System.load("C:\\Users\\user\\Documents\\NetBeansProjects\\andorsdk3\\src\\andorsdk3\\atmcd64d.dll");
        String libName = null;
        String libDirPath = null;
        boolean WriteToFijiAppJars = true;
        boolean WriteToTempDir = true;
        boolean IsWindows = false;
        boolean hasError = false;
        boolean proceed = true;
        String[] dllToBeChecked = {
            UCRTBASED,
            KERNEL32,
            VCRUNTIME140D,
            VCRUNTIME140_1D,
            MSVCP140D,
            VCRUNTIME140,
            VCRUNTIME140_1,
            MSVCP140,
            SHELL32,
            MSVCR120,
            MSVCP120,
            GCBASE_MD_VC120_V3_0,
            MATHPARSER_MD_VC120_V3_0,
            NODEMAPDATA_MD_VC120_V3_0,
            XMLPARSER_MD_VC120_V3_0,
            LOG_MD_VC120_V3_0,
            WS2_32,
            GENAPI_MD_VC120_V3_0
        };
        Boolean[] isDLLinSystem32 = new Boolean[dllToBeChecked.length];
        Boolean[] WriteDLL = new Boolean[dllToBeChecked.length];
        Arrays.fill(WriteDLL, Boolean.FALSE);
        String thisAlert;

        String osname = System.getProperty("os.name");
        String osnamelc = osname.toLowerCase();
        if (osnamelc.contains("win")) {
            libName = "JNIAndorSDK3v2.dll";
            IsWindows = true;
        } else {
            proceed = false;
            thisAlert = "Live readout only supported in Windows.";
        }

        if (proceed) {

            IsOperatingSystemOK = true;

            File curr_dir = new File(System.getProperty("java.class.path"));
            File Fiji_jars_dir = curr_dir.getAbsoluteFile().getParentFile();

            //Checking if 18 dll(s) present on Windows/System32 
            printlogdll("-------------START Checking if dlls are present in Windows/System32");

            for (int i = 0; i < dllToBeChecked.length; i++) {
                isDLLinSystem32[i] = new File("C://Windows//System32" + "//" + dllToBeChecked[i]).exists();
                if (IsWindows && !isDLLinSystem32[i]) {
                    WriteDLL[i] = true;
                }
                printlogdll(dllToBeChecked[i] + " is in system32: " + isDLLinSystem32[i]);
                printlogdll(dllToBeChecked[i] + " write to SDK folder: " + WriteDLL[i]);
            }
            printlogdll("-------------END Checking if dlls are present in Windows/System32");

            if (WriteToFijiAppJars && Fiji_jars_dir.canRead()) {

                // check if folder is present
                File tempdir = new File(Fiji_jars_dir.toString() + "/liveImFCS-SDK");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                // check if folder is present
                tempdir = new File(tempdir.toString() + "/AndorSDK3");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                File liveReadoutSDK3_dir = new File(tempdir.toString() + "/" + SDK3_VERSION);
                libDirPath = liveReadoutSDK3_dir.toString();

                boolean Write_Atmcd = IsWindows; //sdk3
                boolean Write_atcore = IsWindows, Write_atdevsimcam = IsWindows, Write_atspooler = IsWindows, Write_atutility = IsWindows, Write_atdevchamcam = IsWindows;
                boolean Write_SDKlibrary = true; //native program

                if (liveReadoutSDK3_dir.exists() && liveReadoutSDK3_dir.isDirectory()) {
                    // Directory exists. Check if the libraries are in the folder.
                    if (IsWindows) {
                        Write_SDKlibrary = !(new File(liveReadoutSDK3_dir.toString() + "/" + libName).exists());

                        Write_Atmcd = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATMCD64D).exists());
                        Write_atcore = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATCORE).exists());
                        Write_atdevchamcam = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATDEVCHAMCAM).exists());
                        Write_atspooler = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATSPOOLER).exists());
                        Write_atutility = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATUTILITY).exists());

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                WriteDLL[i] = !(new File(liveReadoutSDK3_dir.toString() + "/" + dllToBeChecked[i]).exists());
                            }
                        }
                    }
                } else {
                    liveReadoutSDK3_dir.mkdir();
                }

                if (Fiji_jars_dir.canWrite()) {
                    //user computer with admin rights
                    WriteToTempDir = false;

                    if (Write_SDKlibrary) {
                        writeLibraryFile(liveReadoutSDK3_dir, libName, false);
                    }

                    if (Write_Atmcd) {
                        writeLibraryFile(liveReadoutSDK3_dir, ATMCD64D, false);
                    }

                    if (Write_atcore) {
                        writeLibraryFile(liveReadoutSDK3_dir, ATCORE, false);
                    }

                    if (Write_atdevchamcam) {
                        writeLibraryFile(liveReadoutSDK3_dir, ATDEVCHAMCAM, false);
                    }

                    if (Write_atutility) {
                        writeLibraryFile(liveReadoutSDK3_dir, ATUTILITY, false);
                    }

                    if (Write_atspooler) {
                        writeLibraryFile(liveReadoutSDK3_dir, ATSPOOLER, false);
                    }

                    for (int i = 0; i < dllToBeChecked.length; i++) {
                        if (WriteDLL[i]) {
                            writeLibraryFile(liveReadoutSDK3_dir, dllToBeChecked[i], false);
                        }
                    }
                } else {

                    WriteToTempDir = Write_SDKlibrary || Write_Atmcd || Write_atcore || Write_atdevchamcam || Write_atspooler || Write_atutility || Arrays.asList(WriteDLL).contains(true);
                }

            }

            if (WriteToTempDir) {
                // write files to temporary folder.
                File tmpDir;
                try {
                    tmpDir = Files.createTempDirectory("liveReadoutSDK3-lib").toFile();
                    tmpDir.deleteOnExit();
                    libDirPath = tmpDir.toString();
                    if (IsWindows) {
                        writeLibraryFile(tmpDir, libName, true);
                        writeLibraryFile(tmpDir, ATMCD64D, true);
                        writeLibraryFile(tmpDir, ATCORE, true);
                        writeLibraryFile(tmpDir, ATDEVCHAMCAM, true);
                        writeLibraryFile(tmpDir, ATUTILITY, true);
                        writeLibraryFile(tmpDir, ATSPOOLER, true);

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                writeLibraryFile(tmpDir, dllToBeChecked[i], true);
                            }
                        }

                    }

                } catch (IOException ex) {
                    Logger.getLogger(AndorSDK3v2.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (IsWindows) {

                for (int i = 0; i < dllToBeChecked.length; i++) {
                    try {
                        if (isDLLinSystem32[i]) {
                            System.load("C://Windows//System32" + "//" + dllToBeChecked[i]);
                            printlogdll("loading " + dllToBeChecked[i] + " from System32");
                        } else {
                            System.load(libDirPath + "/" + dllToBeChecked[i]);
                            printlogdll("loading " + dllToBeChecked[i] + " from libdir");
                        }

                    } catch (Exception e) {
                        thisAlert = "Unable to load " + dllToBeChecked[i];
                        IJ.log(thisAlert);
                    }
                }

                try {
                    System.load(libDirPath + "/" + ATCORE);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + ATCORE;
                    IJ.log(thisAlert);
                }

                try {
                    System.load(libDirPath + "/" + ATDEVCHAMCAM);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + ATDEVCHAMCAM;
                    IJ.log(thisAlert);
                }

                try {
                    System.load(libDirPath + "/" + ATUTILITY);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + ATUTILITY;
                    IJ.log(thisAlert);
                }

                try {
                    System.load(libDirPath + "/" + ATSPOOLER);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + ATSPOOLER;
                    IJ.log(thisAlert);
                }

                try {
                    System.load(libDirPath + "/" + ATMCD64D);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + ATMCD64D;
                    IJ.log(thisAlert);
                }

                try {
                    System.load(libDirPath + "/" + libName);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + libName;
                    IJ.log(thisAlert);
                }

            }

            IsLoadingLibraryOK = true;
        }

    }

    /*
    JNI START
     */
    // JNI TEST
    public static native float seyHello(int n1, int n2);

    public native int sayHello();

    // SDK3
    public static native int isSCMOSconnectedSDK3();// 0 = all ok; 17 = either soliFs or umanager is on; 1 = camera is not connected; 99 = some other error; // need to call ShutDownSDK3

    public static native void ShutDownSDK3(); // close and finalize library

    public static native void SystemShutDownSDK3();//Turn off cooler and shutdown sdk3

    public static native boolean InitializeSystemSDK3(); // V 31 July 2020

    //getter
    public static native String GetEnumeratedStringSDK3(String feature); // return enumerated string value

    public static native int GetIntegerValueSDK3(String feature); // return integer value //return 99 if error

    public static native double GetDoubleValueSDK3(String feature); // "SensorTemperature"

    public static native String GetStringValueSDK3(String feature); // return string value

    public static native boolean GetBooleanValueSDK3(String feature); // return bool value

    public static native int GetEnumCountSDK3(String feature); //return 99 if error

    public static native String GetEnumStringByIndexSDK3(String feature, int index);

    public static native float GetFloatMaxSDK3(String feature);

    public static native float GetFloatMinSDK3(String feature);

    public static native int GetIntMaxSDK3(String feature);

    public static native int GetIntMinSDK3(String feature);

    //setter
    public static native int SetEnumeratedStringSDK3(String feature, String value); // "FanSpeed" Off,Low,Medium,On

    public static native int SetIntegerValueSDK3(String feature, int value); // set integer value

    public static native int SetDoubleValueSDK3(String feature, double value); // set float value

    public static native int SetBooleanValueSDK3(String feature, int somebool); // "SensorCooling" 1 0

    public static native int SetEnumIndexSDK3(String feature, int index); //

    //main
    public static native void setParameterSingleSDK3(double exposuretime, int width, int height, int left, int top, int inCamBinning, int pixelencoding);

    public static native short[] runSingleScanSDK3();

    public static native void setParameterInfiniteLoopSDK3(int size_b, int totalFrame, int transferInterval, double exposuretime, int width, int height, int left, int top, int incamerabinning, int pixelencoding, int arraysize, int auxOutSource, int isOverlap);

    public static native void runInfiniteLoopSDK3(short[] outArray, FrameCounter fcObj);

    public static native void setStopMechanismSDK3(boolean isStopCalled);

    /*
    JNI END
     */
    public static void runThread_updatetemp() {
        SwingWorker<Void, List<Integer>> worker = new SwingWorker<Void, List<Integer>>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!Common.isShutSystemPressed) {
                    Common.tempStatus[0] = (int) GetDoubleValueSDK3("SensorTemperature");
                    String status = GetEnumeratedStringSDK3("TemperatureStatus");
                    switch (status) {
                        case "Cooler Off":
                            Common.tempStatus[1] = 0;
                            break;
                        case "Cooling":
                            Common.tempStatus[1] = 1;
                            break;
                        case "Stabilised":
                            Common.tempStatus[1] = 2;
                            break;
                        case "Drift":
                            Common.tempStatus[1] = 3;
                            IJ.showMessage("Sensor temperature status: Drift");
                            break;
                        case "Not Stabilised":
                            Common.tempStatus[1] = 4;
                            break;
                        case "Fault":
                            Common.tempStatus[1] = 5;
                            IJ.showMessage("Sensor temperature status: Fault");
                            break;
                        case "Sensor Over Temperature":
                            Common.tempStatus[1] = 6;
                            IJ.showMessage("Sensor temperature status: Sensor Over Temperature");
                            break;
                    }

                    List<Integer> list = Arrays.asList(0, 0);
                    list.set(0, Common.tempStatus[0]);
                    list.set(1, Common.tempStatus[1]);
                    publish(list);
                    Thread.sleep(5000);
                }
                return null;
            }

            @Override
            protected void process(List<List<Integer>> chunks) {
                List<Integer> tempandstatusList = chunks.get(chunks.size() - 1);
                Integer currtemp = tempandstatusList.get(0);
                Integer currtempstatus = tempandstatusList.get(1);
                DirectCapturePanel.tfTemperature.setText(Integer.toString(currtemp) + " " + (char) 186 + " C");
                if (currtempstatus == 2) {//0 = Cooler Off; 1 = Cooling; 2 = Stabilized; 3 = Drift; 4 =Not Stabilized ; 5 = Fault; 6 = Sensor Over Temperature
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLUE);
                } else if (currtempstatus == 1 || currtempstatus == 4) {
                    DirectCapturePanel.tfTemperature.setBackground(Color.RED);
                } else {
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLACK);
                }
            }

        };
        worker.execute();

    }

    // Working Version (single capture mode)
    public static void runThread_singlecapture(boolean isFF) {

        if (isFF) {
            //Recall previous ROI setting
            Common.mem_oWidth = Common.oWidth;
            Common.mem_oHeight = Common.oHeight;
            Common.mem_oLeft = Common.oLeft;
            Common.mem_oTop = Common.oTop;

            //Set setting for maximum coverage before capturing a single image for display
            Common.oWidth = Common.MAXpixelwidth / Common.inCameraBinning;
            Common.oHeight = Common.MAXpixelheight / Common.inCameraBinning;
            Common.oLeft = 1;
            Common.oTop = 1;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                setParameterSingleSDK3(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, Common_SONA.PixelEncoding);
                Common.kineticCycleTime = 1 / GetDoubleValueSDK3("FrameRate"); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                Common.arraysingleS = runSingleScanSDK3();

                //Update Display Image for ROI ifcapturing full frame or full crop mode
                int newX = (int) Math.floor(Common.MAXpixelwidth / Common.inCameraBinning);
                int newY = (int) Math.floor(Common.MAXpixelheight / Common.inCameraBinning);

                if (newX * newY == Common.arraysingleS.length) {//Fullframe captured
                    int[] tempIntArray = new int[Common.arraysingleS.length];
                    for (int i = 0; i < Common.arraysingleS.length; i++) {
                        tempIntArray[i] = (int) Common.arraysingleS[i];
                    }
                    DirectCapturePanel.DisplayImageObj.updateImage(tempIntArray, Common.inCameraBinning, Common.MAXpixelwidth, Common.MAXpixelheight, Common.isCropMode);
                }

                if (!isFF) {// display extra images onto Fiji
                    boolean alteredDim = false;
                    if (Common.ip != null) {
                        alteredDim = (Common.ip.getHeight() * Common.ip.getWidth()) != Common.arraysingleS.length;
                    }

                    if (Common.impwin == null || Common.imp == null || (Common.impwin != null && Common.imp != null) && Common.impwin.isClosed() || alteredDim) {
                        Common.ip = new ShortProcessor(Common.oWidth, Common.oHeight);
                    }

                    for (int y = 0; y < Common.oHeight; y++) {
                        for (int x = 0; x < Common.oWidth; x++) {
                            int index = (y * Common.oWidth) + x;
                            Common.ip.putPixel(x, y, (int) Common.arraysingleS[index]);
                        }
                    }

                    if (Common.impwin == null || Common.imp == null || (Common.impwin != null && Common.imp != null) && Common.impwin.isClosed() || alteredDim) {
                        if (Common.impwin != null) {
                            Common.impwin.close();
                        }
                        Common.imp = new ImagePlus("Single Scan", Common.ip);
                        Common.imp.show();

                        Common.impwin = Common.imp.getWindow();
                        Common.impcan = Common.imp.getCanvas();
                        Common.impwin.setLocation(impwinposx, impwinposy);

                        //enlarge image to see better pixels
                        if (Common.oWidth >= Common.oHeight) {
                            Common.scimp = Common.zoomFactor / Common.oWidth; //adjustable: zoomFactor is by default 250 (see parameter definitions), a value chosen as it produces a good size on the screen
                        } else {
                            Common.scimp = Common.zoomFactor / Common.oHeight;
                        }
                        if (Common.scimp < 1.0) {
                            Common.scimp = 1.0;
                        }
                        Common.scimp *= 100;// transfrom this into %tage to run ImageJ command
                        IJ.run(Common.imp, "Original Scale", "");
                        IJ.run(Common.imp, "Set... ", "zoom=" + Common.scimp + " x=" + (int) Math.floor(Common.oWidth / 2) + " y=" + (int) Math.floor(Common.oHeight / 2));
                        IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
                        // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary

                        Common.impcan.setFocusable(true);
                    } else {
                        //(Common.impwin != null && Common.imp != null) && !Common.impwin.isClosed()
                        Common.ip.resetMinAndMax();
                        Common.imp.updateAndDraw();
                    }
                }

                return null;
            }

            @Override
            protected void done() {

                //Reset previos setting
                if (isFF) {
                    Common.oWidth = Common.mem_oWidth;
                    Common.oHeight = Common.mem_oHeight;
                    Common.oLeft = Common.mem_oLeft;
                    Common.oTop = Common.mem_oTop;
                }
            }
        };
        worker.execute();
    }

    // Working Version (live video mode)
    public static void runThread_livevideoV2() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                Thread.currentThread().setName("runThread_livevideoV2");

                final int noThread = 3; // number of working threads for LiveVideo Mode
                final int fbuffersize = Common.size_a * Common.size_b; // number of frame

                // Control flow reset, buffer reset
                Common.arraysize = fbuffersize * Common.tempWidth * Common.tempHeight;
                printlog("Size JavaBuffer cr8ed: " + (Common.arraysize * 2 / 1000000) + " MB");
                long timer1 = System.currentTimeMillis();
                Common.bufferArray1D = new short[Common.arraysize];
                printlog("Time cr8 bufferArray1D: " + (System.currentTimeMillis() - timer1) + " ms");
                Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK3(fbuffersize, 1000000, Common.transferFrameInterval, Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, Common_SONA.PixelEncoding, Common.arraysize, Common_SONA.OutputTriggerKind, Common_SONA.isOverlap); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                Common.kineticCycleTime = 1 / GetDoubleValueSDK3("FrameRate"); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI

                CppToJavaTransferWorkerEXTENDEDV2 CppToJavaTransferWorkerEXTENDEDV2Instant = new CppToJavaTransferWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV2Instant = new LiveVideoWorkerV2(Common.tempWidth, Common.tempHeight, latch);
                SynchronizerWorkerInstant = new SynchronizerWorker(latch);

                long timeelapse = System.nanoTime();
                CppToJavaTransferWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV2Instant.execute();
                SynchronizerWorkerInstant.execute();

                latch.await();
                System.out.println("***Live V2***");
                System.out.println("Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " + Common.kineticCycleTime);

                return null;
            }

            @Override
            protected void done() {
                Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                System.out.println("Native counter cpp: " + Common.framecounter.getCounter() + ", time overall: " + Common.framecounter.time1 + ", time average readbuffer combin: " + Common.framecounter.time2 + ", time JNI copy: " + Common.framecounter.time3 + ", JNI transfer: " + (Common.tempWidth * Common.tempHeight * 2 / Common.framecounter.time3) + " MBps, fps(cap): " + (1 / Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("Live done");
            }

        };
        worker.execute();
    }

    // Working Version (calibration mode)
    public static void runThread_noncumulativeV3() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                Thread.currentThread().setName("runThread_noncumulativeV3");

                final int noThread = 3; // number of working threads 
                final int fbuffersize = Common.size_a * Common.size_b; // number of frame

                // Control flow reset, buffer reset
                Common.arraysize = fbuffersize * Common.tempWidth * Common.tempHeight;
                printlog("Size JavaBuffer cr8ed: " + (Common.arraysize * 2 / 1000000) + " MB");
                long timer1 = System.currentTimeMillis();
                Common.bufferArray1D = new short[Common.arraysize];
                printlog("Time cr8 bufferArray1D: " + (System.currentTimeMillis() - timer1) + " ms");
                Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK3(fbuffersize, 1000000, Common.transferFrameInterval, Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, Common_SONA.PixelEncoding, Common.arraysize, Common_SONA.OutputTriggerKind, Common_SONA.isOverlap); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = 1 / GetDoubleValueSDK3("FrameRate"); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI

                CppToJavaTransferWorkerEXTENDEDV2 CppToJavaTransferWorkerEXTENDEDV2Instant = new CppToJavaTransferWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                NonCumulativeACFWorkerV3Instant.execute();

                latch.await();
                System.out.println("***Calibration V3***");
                System.out.println("Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " + Common.kineticCycleTime);

                return null;
            }

            @Override
            protected void done() {
                Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                System.out.println("Native counter cpp: " + Common.framecounter.getCounter() + ", time overall: " + Common.framecounter.time1 + ", time average readbuffer combin: " + Common.framecounter.time2 + ", time JNI copy: " + Common.framecounter.time3 + ", JNI transfer: " + (Common.tempWidth * Common.tempHeight * 2 / Common.framecounter.time3) + " MBps, fps(cap): " + (1 / Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("Calibration done");
            }

        };
        worker.execute();
    }

    // Working Version (acquisition mode)
    public static void runThread_cumulativeV3() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.currentThread().setName("runThread_noncumulativeV3");

                final int noThread = 5; // number of working threads  
                final int fbuffersize = Common.size_a * Common.size_b; // number of frame

                // Control flow reset, buffer reset
                Common.arraysize = fbuffersize * Common.tempWidth * Common.tempHeight;
                printlog("Size JavaBuffer cr8ed: " + (Common.arraysize * 2 / 1000000) + " MB");
                long timer1 = System.currentTimeMillis();
                Common.bufferArray1D = new short[Common.arraysize];
                printlog("Time cr8 bufferArray1D: " + (System.currentTimeMillis() - timer1) + " ms");
                Common.ims_cum = new ImageStack(Common.tempWidth, Common.tempHeight);
                Common.framecounterIMSX = new FrameCounterX();
                Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK3(fbuffersize, Common.totalFrame, Common.transferFrameInterval, Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, Common_SONA.PixelEncoding, Common.arraysize, Common_SONA.OutputTriggerKind, Common_SONA.isOverlap); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUImg
                Common.kineticCycleTime = 1 / GetDoubleValueSDK3("FrameRate"); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI

                CppToJavaTransferWorkerEXTENDEDV2 CppToJavaTransferWorkerEXTENDEDV2Instant = new CppToJavaTransferWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                BufferToStackWorkerInstant = new BufferToStackWorker(Common.tempWidth, Common.tempHeight, Common.totalFrame, latch, Common.arraysize);
                CumulativeACFWorkerV3Instant = new CumulativeACFWorkerV3(latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                BufferToStackWorkerInstant.execute();
                CumulativeACFWorkerV3Instant.execute();
                NonCumulativeACFWorkerV3Instant.execute();

                latch.await();
                System.out.println("***Acquisition V3***");
                System.out.println("Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " + Common.kineticCycleTime + ", totalframe: " + Common.ims_cum.getSize());
                return null;

            }

            @Override
            protected void done() {
                Common.isAcquisitionRunning = false;
                DirectCapturePanel.tbStartStop.setSelected(false);
                DirectCapturePanel.tfTotalFrame.setEditable(true);
                DirectCapturePanel.tfExposureTime.setEditable(true);
                Common.fitStartCumulative = 1;
                DirectCapturePanel.tfCumulativeFitStart.setText(Integer.toString(Common.fitStartCumulative));
                System.out.println("Native counter cpp: " + Common.framecounter.getCounter() + ", time overall: " + Common.framecounter.time1 + ", time average readbuffer combin: " + Common.framecounter.time2 + ", time JNI copy: " + Common.framecounter.time3 + ", JNI transfer: " + (Common.tempWidth * Common.tempHeight * 2 / Common.framecounter.time3) + " MBps, fps(cap): " + (1 / Common.framecounter.time3) + "Common.framecounterIMSX: " + Common.framecounterIMSX.getCount());
                System.out.println("***");
                System.out.println("");

                IJ.showMessage("Acquisition done");

            }

        };
        worker.execute();
    }

    // Working Version (ICCS mode)
    public static void runThread_ICCS() {

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.currentThread().setName("runThread_ICCS");

                final int noThread = 3; //number of working threads
                final int fbuffersize = Common.size_a * Common.size_b;

                // Control flow reset, buffer reset
                Common.arraysize = fbuffersize * Common.tempWidth * Common.tempHeight;
                printlog("Size JavaBuffer cr8ed: " + (Common.arraysize * 2 / 1000000) + " MB");
                long timer1 = System.currentTimeMillis();
                Common.bufferArray1D = new short[Common.arraysize];
                printlog("Time cr8 bufferArray1D: " + (System.currentTimeMillis() - timer1) + " ms");
                Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call setParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK3(fbuffersize, 1000000, Common.transferFrameInterval, Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, Common_SONA.PixelEncoding, Common.arraysize, Common_SONA.OutputTriggerKind, Common_SONA.isOverlap); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = 1 / GetDoubleValueSDK3("FrameRate"); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI

                CppToJavaTransferWorkerEXTENDEDV2 CppToJavaTransferWorkerEXTENDEDV2Instant = new CppToJavaTransferWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                ICCSWorkerInstant = new ICCSWorker(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timelapse = System.nanoTime();
                CppToJavaTransferWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                ICCSWorkerInstant.execute();

                latch.await();
                System.out.println("***ICCS***");
                System.out.println("Java Time elapse: " + (System.nanoTime() - timelapse) / 1000000 + " ms; kinetic cycle: " + Common.kineticCycleTime);

                return null;
            }

            @Override
            protected void done() {

                Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                ICCSWorkerInstant.setNullICCS();

                System.out.println("Native counter cpp: " + Common.framecounter.getCounter() + ", time overall: "
                        + Common.framecounter.time1 + ", time average readbuffer combin: " + Common.framecounter.time2
                        + ", time JNI copy: " + Common.framecounter.time3 + ", JNI transfer: "
                        + (Common.tempWidth * Common.tempHeight * 2 / Common.framecounter.time3) + " MBps, fps(cap): "
                        + (1 / Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("ICCS done");

            }

        };
        worker.execute();

    }

    private static class CppToJavaTransferWorkerEXTENDEDV2 extends CppTOJavaTransferWorkerV2 {

        public CppToJavaTransferWorkerEXTENDEDV2(short[] array, CountDownLatch latch) {
            super(array, latch);
        }

        @Override
        protected void runInfinteLoop() {
            runInfiniteLoopSDK3(array, Common.framecounter);
        }
    }

    private static void writeLibraryFile(File Directory, String LibName, Boolean DeleteOnExit) {
        try {
            //NOTE: include package name, which becomes the folder name in .jar file.'
            InputStream in = ClassLoader.class.getResourceAsStream("/directCameraReadout/andorsdk3v2/" + LibName);
            if (in == null) {
                throw new FileNotFoundException("Library " + LibName + " is not available");
            }

            File temp = new File(Directory, LibName);
            if (DeleteOnExit) {
                temp.deleteOnExit();
            }

            byte[] buffer = new byte[1024];
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                int read;
                //NOTE: other methods didn't work. the read = -1 seems important. Examples of other methods:
                // 1) length = is.read(buffer); os.write(buffer, 0, length);
                // 2) while ((readBytes = stream.read(buffer)) > 0) { resStreamOut.write(buffer, 0, readBytes);}
                // 3) try (InputStream in = url.openStream()) { Files.copy(in, nativeLibTmpFile.toPath());}
                // 4) InputStream source = ClassLoader.class.getResourceAsStream("/libagpufit.so"); File outfile = new File(tempdir, "libagpufit.so"); FileOutputStream fos = new java.io.FileOutputStream(outfile); while (source.available() > 0) {  // write contents of 'is' to 'fos' fos.write(source.read());}
                while ((read = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // update metadata and timer (acquisition mode)
    public static void writeExcel(File sfile, String $exception, boolean showlog) {
        //There is a limit of 1,048,576 rows and 16,384 (corresponds to 128 x 128(
        //columns in xlsx file as of 2019

        File file;
        String $sfile = sfile.toString();
        int dotind = $sfile.lastIndexOf('.');
        if (dotind != -1) {
            $sfile = $sfile.substring(0, dotind);
        }
        file = new File($sfile + ".xlsx");

        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet metaSheet = wb.createSheet("Metadata");

        Row row;

        //write Metadata 
        int t;
        int nopm = 41;
        String[] metadatatag = new String[nopm];
        String[] metadatavalue = new String[nopm];
        t = 0;
        metadatatag[t++] = "SizeT";
        metadatatag[t++] = "sizeX";
        metadatatag[t++] = "sizeY";
        metadatatag[t++] = "AOI Left"; // Index start at 1
        metadatatag[t++] = "AOI Top"; // Index start at 1
        metadatatag[t++] = "AOI Horizontal Bin"; // in pixel
        metadatatag[t++] = "AOI Vertical Bin"; // in pixel
        metadatatag[t++] = "AOI Layout"; // Index start at 1
        metadatatag[t++] = "AOI Sride"; // Index start at 1
        metadatatag[t++] = "Vertically Centre AOI";
        metadatatag[t++] = "Camera Family";
        metadatatag[t++] = "Camera Model";
        metadatatag[t++] = "Camera Name";
        metadatatag[t++] = "Serial Number";
        metadatatag[t++] = "Firmware Version";
        metadatatag[t++] = "Acquisition cycle time (Hz)";
        metadatatag[t++] = "Exposure time (in seconds)";
        metadatatag[t++] = "Electronic Shuttering Mode";
        metadatatag[t++] = "Overlap";
        metadatatag[t++] = "Spurious Noise Filter";
        metadatatag[t++] = "Pixel Encoding";
        metadatatag[t++] = "Pixel Readout Rate";
        metadatatag[t++] = "Readout Time";
        metadatatag[t++] = "Row Read Time";
        metadatatag[t++] = "Baseline";
        metadatatag[t++] = "Bitdepth";
        metadatatag[t++] = "Bytes Per Pixel";
        metadatatag[t++] = "Image Size Bytes";
        metadatatag[t++] = "Pixel Height (um)";
        metadatatag[t++] = "Pixel Width (um)";
        metadatatag[t++] = "Sensor Height (pixels)";
        metadatatag[t++] = "Sensor Width (pixels)";
        metadatatag[t++] = "Fan Speed";
        metadatatag[t++] = "Sensor Temperature";
        metadatatag[t++] = "Cycle Mode";
        metadatatag[t++] = "Interface Type";
        metadatatag[t++] = "Trigger Source";
        metadatatag[t++] = "Auxiliary Out Source";
        metadatatag[t++] = "Software";
        metadatatag[t++] = "SDK";
        metadatatag[t++] = "Time Stamp";

        t = 0;
        metadatavalue[t++] = Integer.toString(Common.framecounterIMSX.getCount());
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOIWidth"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOIHeight"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOILeft"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOITop"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOIHBin"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOIVBin"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("AOILayout");
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("AOIStride"));
        metadatavalue[t++] = Boolean.toString(GetBooleanValueSDK3("VerticallyCentreAOI"));
        metadatavalue[t++] = GetStringValueSDK3("CameraFamily");
        metadatavalue[t++] = GetStringValueSDK3("CameraModel");
        metadatavalue[t++] = GetStringValueSDK3("CameraName");
        metadatavalue[t++] = GetStringValueSDK3("SerialNumber");
        metadatavalue[t++] = GetStringValueSDK3("FirmwareVersion");
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("FrameRate"));
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("ExposureTime"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("ElectronicShutteringMode");
        metadatavalue[t++] = Boolean.toString(GetBooleanValueSDK3("Overlap"));
        metadatavalue[t++] = Boolean.toString(GetBooleanValueSDK3("SpuriousNoiseFilter"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("PixelEncoding");
        metadatavalue[t++] = GetEnumeratedStringSDK3("PixelReadoutRate");
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("ReadoutTime"));
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("RowReadTime"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("Baseline"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("BitDepth");
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("BytesPerPixel"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("ImageSizeBytes"));
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("PixelHeight"));
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("PixelWidth"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("SensorHeight"));
        metadatavalue[t++] = Integer.toString(GetIntegerValueSDK3("SensorWidth"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("FanSpeed");
        metadatavalue[t++] = Double.toString(GetDoubleValueSDK3("SensorTemperature"));
        metadatavalue[t++] = GetEnumeratedStringSDK3("CycleMode");
        metadatavalue[t++] = GetStringValueSDK3("InterfaceType");
        metadatavalue[t++] = GetEnumeratedStringSDK3("TriggerSource");
        metadatavalue[t++] = GetEnumeratedStringSDK3("AuxiliaryOutSource");
        metadatavalue[t++] = "DirectCameraReadout_" + DCR_VERSION;
        metadatavalue[t++] = "SDK_" + SDK3_VERSION;
        Date date = new Date();
        long time = date.getTime();
        Timestamp ts = new Timestamp(time);
        metadatavalue[t++] = ts.toString();

        for (int i = 0; i < nopm; i++) {
            row = metaSheet.createRow(i);
            row.createCell(0).setCellValue(metadatatag[i]);
            row.createCell(1).setCellValue(metadatavalue[i]);
        }

        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            wb.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException($exception, e);
        }

        if (showlog) {
            IJ.log("Metadata file saved");
        }

    }

    public static boolean isSDKload() {
        return IsLoadingLibraryOK;
    }

}
