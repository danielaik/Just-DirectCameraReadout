/*
 * SDK4 Version 20.12.6051, 20.12.6136, 22.2.6391
 * Compiled with DCAMSDK4 v21066291
 * Link to dcam-api installer: https://dcam-api.com/downloads/
 * Tested on Orca Flash 4.0 v2 C11440-22C (SN 100261), Orca Flash 4.0 v2 C11440-22CU (SN 750941), Flash 4.0 v3 C13440-20CU (SN 300374), Flash 4.0 C13440-20C (Lucas Flatten), Quest C15550-20UP
 */
package directCameraReadout.hamadcamsdk4;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import java.util.Arrays;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.gui.DirectCapturePanel.Common;
import directCameraReadout.gui.cameraConstant.Common_Orca;
import directCameraReadout.control.FrameCounter;
import directCameraReadout.control.FrameCounterX;
import directCameraReadout.workers.Workers.*;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV2Instant;
import static directCameraReadout.workers.Workers.SynchronizerWorkerInstant;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV3Instant;
import static directCameraReadout.workers.Workers.BufferToStackWorkerInstant;
import static directCameraReadout.workers.Workers.CumulativeACFWorkerV3Instant;
import static directCameraReadout.workers.Workers.NonCumulativeACFWorkerV3Instant;
import static directCameraReadout.workers.Workers.ICCSWorkerInstant;
import static version.VERSION.DCR_VERSION;
import static version.VERSION.HAMASDK_VERSION;

public class Hamamatsu_DCAM_SDK4 {

    // NOTE: 
    // DCR_VERSION of the used SDK4 library.
    // DCR_VERSION must be updated when .dll/.so files are changed so that they are placed in a new sub-folder named after this DCR_VERSION num in Fiji.App > jars.
    private static void printlog(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    private static void printlogdll(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    private static boolean IsOperatingSystemOK = false;
    public static boolean IsLoadingLibraryOK = false;

    final static int impwinposx = 1110;
    final static int impwinposy = 125;

    static {

        printlogdll("*********************SDK4***********************");

//        try {
//            System.load("C:\\Users\\Administrator\\Documents\\GitHub\\DCAM_sdk4_GitHubRepo\\java\\hamamatsu_dcam_sdk4\\src\\hamadcamsdk4\\JNIHamamatsuDCAMsdk4.dll");
//        } catch (Exception e) {
//            System.out.println("unable to load alert: " + e);
//        }
        String libName = null;
        String libDirPath = null;
        boolean WriteToFijiAppJars = true;
        boolean WriteToTempDir = true;
        boolean IsWindows = false;
        boolean hasError = false;
        boolean proceed = true;
        String[] dllToBeChecked = {};
        Boolean[] isDLLinSystem32 = new Boolean[dllToBeChecked.length];
        Boolean[] WriteDLL = new Boolean[dllToBeChecked.length];
        Arrays.fill(WriteDLL, Boolean.FALSE);
        String thisAlert;

        String osname = System.getProperty("os.name");
        String osnamelc = osname.toLowerCase();
        if (osnamelc.contains("win")) {
            libName = "JNIHamamatsuDCAMsdk4.dll";
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
                tempdir = new File(tempdir.toString() + "/HamaSDK4");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                File liveReadoutHamaSDK4_dir = new File(tempdir.toString() + "/" + HAMASDK_VERSION);
                libDirPath = liveReadoutHamaSDK4_dir.toString();

//                boolean Write_Atmcd = IsWindows; //sdk3
//                boolean Write_atcore = IsWindows, Write_atdevsimcam = IsWindows, Write_atspooler = IsWindows, Write_atutility = IsWindows, Write_atdevchamcam = IsWindows;
                boolean Write_SDKlibrary = true; //native program

                if (liveReadoutHamaSDK4_dir.exists() && liveReadoutHamaSDK4_dir.isDirectory()) {
                    // Directory exists. Check if the libraries are in the folder.
                    if (IsWindows) {
                        Write_SDKlibrary = !(new File(liveReadoutHamaSDK4_dir.toString() + "/" + libName).exists());

//                        Write_Atmcd = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATMCD64D).exists());
//                        Write_atcore = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATCORE).exists());
//                        Write_atdevchamcam = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATDEVCHAMCAM).exists());
//                        Write_atspooler = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATSPOOLER).exists());
//                        Write_atutility = !(new File(liveReadoutSDK3_dir.toString() + "/" + ATUTILITY).exists());
                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                WriteDLL[i] = !(new File(liveReadoutHamaSDK4_dir.toString() + "/" + dllToBeChecked[i]).exists());
                            }
                        }
                    }
                } else {
                    liveReadoutHamaSDK4_dir.mkdir();
                }

                if (Fiji_jars_dir.canWrite()) {
                    //user computer with admin rights
                    WriteToTempDir = false;

                    if (Write_SDKlibrary) {
                        writeLibraryFile(liveReadoutHamaSDK4_dir, libName, false);
                    }

//                    if (Write_Atmcd) {
//                        writeLibraryFile(liveReadoutSDK3_dir, ATMCD64D, false);
//                    }
//
//                    if (Write_atcore) {
//                        writeLibraryFile(liveReadoutSDK3_dir, ATCORE, false);
//                    }
//
//                    if (Write_atdevchamcam) {
//                        writeLibraryFile(liveReadoutSDK3_dir, ATDEVCHAMCAM, false);
//                    }
//
//                    if (Write_atutility) {
//                        writeLibraryFile(liveReadoutSDK3_dir, ATUTILITY, false);
//                    }
//
//                    if (Write_atspooler) {
//                        writeLibraryFile(liveReadoutSDK3_dir, ATSPOOLER, false);
//                    }
                    for (int i = 0; i < dllToBeChecked.length; i++) {
                        if (WriteDLL[i]) {
                            writeLibraryFile(liveReadoutHamaSDK4_dir, dllToBeChecked[i], false);
                        }
                    }
                } else {
                    WriteToTempDir = Write_SDKlibrary || Arrays.asList(WriteDLL).contains(true);
//                    WriteToTempDir = Write_SDKlibrary || Write_Atmcd || Write_atcore || Write_atdevchamcam || Write_atspooler || Write_atutility || Arrays.asList(WriteDLL).contains(true);
                }

            }

            if (WriteToTempDir) {
                // write files to temporary folder.
                File tmpDir;
                try {
                    tmpDir = Files.createTempDirectory("liveReadoutHamaSDK4-lib").toFile();
                    tmpDir.deleteOnExit();
                    libDirPath = tmpDir.toString();
                    if (IsWindows) {
                        writeLibraryFile(tmpDir, libName, true);
//                        writeLibraryFile(tmpDir, ATMCD64D, true);
//                        writeLibraryFile(tmpDir, ATCORE, true);
//                        writeLibraryFile(tmpDir, ATDEVCHAMCAM, true);
//                        writeLibraryFile(tmpDir, ATUTILITY, true);
//                        writeLibraryFile(tmpDir, ATSPOOLER, true);

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                writeLibraryFile(tmpDir, dllToBeChecked[i], true);
                            }
                        }

                    }

                } catch (IOException ex) {
                    Logger.getLogger(Hamamatsu_DCAM_SDK4.class.getName()).log(Level.SEVERE, null, ex);
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

//                try {
//                    System.load(libDirPath + "/" + ATCORE);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + ATCORE;
//                    IJ.log(thisAlert);
//                }
//
//                try {
//                    System.load(libDirPath + "/" + ATDEVCHAMCAM);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + ATDEVCHAMCAM;
//                    IJ.log(thisAlert);
//                }
//
//                try {
//                    System.load(libDirPath + "/" + ATUTILITY);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + ATUTILITY;
//                    IJ.log(thisAlert);
//                }
//
//                try {
//                    System.load(libDirPath + "/" + ATSPOOLER);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + ATSPOOLER;
//                    IJ.log(thisAlert);
//                }
//
//                try {
//                    System.load(libDirPath + "/" + ATMCD64D);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + ATMCD64D;
//                    IJ.log(thisAlert);
//                }
                try {
                    System.load(libDirPath + "/" + libName);
                } catch (Exception e) {
                    thisAlert = "Unable to load " + libName;
                    IJ.log(thisAlert);
                } catch (java.lang.UnsatisfiedLinkError er) {
                    //throw new UnsatisfiedLinkError("Missing DCAM-API");
                    if (er.getMessage().contains("Can't find dependent libraries")) {
                        Runnable r = () -> {
                            String html = "<html><body width='%1s'><h3>Missing DCAM-API</h3>"
                                    + "<p>To run Hamamatsu camera, please install dcam-api.<br><br>"
                                    + "<p>Link to dcam-api installer: https://dcam-api.com/downloads/";
                            // change to alter the width 
                            int w = 175;

                            JOptionPane.showMessageDialog(null, String.format(html, w, w));
                        };
                        SwingUtilities.invokeLater(r);
                    } else {
                        throw er;
                    }
                }

            }

            IsLoadingLibraryOK = true;
        }
    }

    /*
    JNI START
     */
    public static native float seyHello(int n1, int n2);

    public native int sayHello();

    public static native int isHAMAconnectedSDK4(); //init and check if camera is connected// //0 - good to go; 1 - no camera; 2 - other error; 3 - missing dependent libraries 

    public static native String GetModelSDK4(); // will init and uninit before getting camera model

    public static native int InitializeHamaSDK4(); // need to call uninit

    public static native int SystemShutDownSDK4(); //uninit

    public static native String GetStringSDK4(String IDSTR);
    //MODEL, CAMERAID, BUS

    public static native double GetDoubleSDK4(String IDPROP);
    //CONVERSIONFACTOR_COEFF, CONVERSIONFACTOR_OFFSET, BITSPERCHANNEL

    public static native int getIntegerSDK4(String param);
    //WIDTH, HEIGHT, TOP, LEFT, BIN

    public static native int[] getDetectorDimensionSDK4();//0=width 1=height in pixels

    public static native float[] getChipSizeSDK4();//0=width 1=height in um

    public static native double getKineticCycleSDK4();

    public static native int setParameterSDK4(double exposureTime, int Width, int Height, int Left, int Top, int Incamerabin, int acqmode, int totalframe, int size_b, int arraysize, int outTriggerKind, double outTriggerDelay, double outTriggerPeriod, int readoutSpeed, int sensorMode); //index Left Top start from 1; Width and Heught represent pixel size after factored in "physical binning/incamerabin"

    public static native short[] runSingleScanSDK4();

    public static native void runInfiniteLoopSDK4(short[] outArray, FrameCounter frameObj);

    public static native void setStopMechanismSDK4(boolean isStopPressed);


    /*
    JNI END
     */
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

                int err = setParameterSDK4(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 1, 1, Common.size_b, Common.arraysize, Common_Orca.OutputTriggerKind, Common_Orca.outTriggerDelay, Common_Orca.outTriggerPeriod, Common_Orca.readoutSpeed, Common_Orca.sensorMode);
                if (err != 0) {
                    IJ.showMessage("SetParameter return " + err);
                }
                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                Common.arraysingleS = runSingleScanSDK4();

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
                setParameterSDK4(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Orca.OutputTriggerKind, Common_Orca.outTriggerDelay, Common_Orca.outTriggerPeriod, Common_Orca.readoutSpeed, Common_Orca.sensorMode);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                Common.kineticCycleTime = 1 / getKineticCycleSDK4();
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
                setParameterSDK4(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Orca.OutputTriggerKind, Common_Orca.outTriggerDelay, Common_Orca.outTriggerPeriod, Common_Orca.readoutSpeed, Common_Orca.sensorMode);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // Get real kinetic cycle time
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
                setParameterSDK4(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, Common.totalFrame, fbuffersize, Common.arraysize, Common_Orca.OutputTriggerKind, Common_Orca.outTriggerDelay, Common_Orca.outTriggerPeriod, Common_Orca.readoutSpeed, Common_Orca.sensorMode);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
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
                setParameterSDK4(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Orca.OutputTriggerKind, Common_Orca.outTriggerDelay, Common_Orca.outTriggerPeriod, Common_Orca.readoutSpeed, Common_Orca.sensorMode);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // Get real kinetic cycle time
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
            runInfiniteLoopSDK4(array, Common.framecounter);
        }

    }

    private static void writeLibraryFile(File Directory, String LibName, Boolean DeleteOnExit) {
        try {
            //NOTE: include package name, which becomes the folder name in .jar file.'
            InputStream in = ClassLoader.class.getResourceAsStream("/directCameraReadout/hamadcamsdk4/" + LibName);
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

    // update metadata (acquisition mode)
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
        int nopm = 34;
        String[] metadatatag = new String[nopm];
        String[] metadatavalue = new String[nopm];
        t = 0;
        metadatatag[t++] = "SizeT";
        metadatatag[t++] = "Size X";
        metadatatag[t++] = "Size Y";
        metadatatag[t++] = "AOI Left"; // Index start at 1
        metadatatag[t++] = "AOI Top"; // Index start at 1
        metadatatag[t++] = "AOI Horizontal Bin"; // in pixel
        metadatatag[t++] = "AOI Vertical Bin"; // in pixel
        metadatatag[t++] = "Camera Model";
        metadatatag[t++] = "Camera ID";
        metadatatag[t++] = "Interface Type";
        metadatatag[t++] = "Acquisition cycle time (Hz)";
        metadatatag[t++] = "Exposure time (in seconds)";
        metadatatag[t++] = "Pixel Height (um)";
        metadatatag[t++] = "Pixel Width (um)";
        metadatatag[t++] = "Sensor Height (pixels)";
        metadatatag[t++] = "Sensor Width (pixels)";
        metadatatag[t++] = "Coeff";
        metadatatag[t++] = "Offset";
        metadatatag[t++] = "Bits per channel";
        metadatatag[t++] = "OUTPUTTRIGGER_KIND";
        metadatatag[t++] = "OUTPUTTRIGGER_ACTIVE";
        metadatatag[t++] = "OUTPUTTRIGGER_SOURCE";
        metadatatag[t++] = "OUTPUTTRIGGER_POLARITY";
        metadatatag[t++] = "OUTPUTTRIGGER_DELAY";
        metadatatag[t++] = "OUTPUTTRIGGER_PERIOD";
        metadatatag[t++] = "INTERNALLINESPEED";
        metadatatag[t++] = "INTERNAL_LINEINTERVAL";
        metadatatag[t++] = "TIMING_READOUTTIME";
        metadatatag[t++] = "TIMING_GLOBALEXPOSUREDELAY";
        metadatatag[t++] = "READOUT SPEED";
        metadatatag[t++] = "SENSOR MODE";
        metadatatag[t++] = "Software";
        metadatatag[t++] = "SDK";
        metadatatag[t++] = "Time Stamp";

        t = 0;
        metadatavalue[t++] = Integer.toString(Common.framecounterIMSX.getCount());
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("WIDTH"));
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("HEIGHT"));
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("LEFT"));
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("TOP"));
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("BIN"));
        metadatavalue[t++] = Integer.toString(Hamamatsu_DCAM_SDK4.getIntegerSDK4("BIN"));
        metadatavalue[t++] = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
        metadatavalue[t++] = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
        metadatavalue[t++] = Hamamatsu_DCAM_SDK4.GetStringSDK4("BUS");
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.getKineticCycleSDK4());
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("EXPOSURETIME"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.getChipSizeSDK4()[0]);
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.getChipSizeSDK4()[1]);
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.getDetectorDimensionSDK4()[0]);
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.getDetectorDimensionSDK4()[1]);
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("CONVERSIONFACTOR_COEFF"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("CONVERSIONFACTOR_OFFSET"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("BITSPERCHANNEL"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_KIND"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_ACTIVE"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_SOURCE"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_POLARITY"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_DELAY"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("OUTPUTTRIGGER_PERIOD"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("INTERNALLINESPEED"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("INTERNAL_LINEINTERVAL"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("TIMING_READOUTTIME"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("TIMING_GLOBALEXPOSUREDELAY"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("READOUTSPEED"));
        metadatavalue[t++] = Double.toString(Hamamatsu_DCAM_SDK4.GetDoubleSDK4("SENSORMODE"));
        metadatavalue[t++] = "DirectCameraReadout_" + DCR_VERSION;
        metadatavalue[t++] = "SDK_" + HAMASDK_VERSION;
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
