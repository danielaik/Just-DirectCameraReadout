
/*
 * Need to be installed:
 * PVCam_3.9.0.4-PMQI_Release_Setup 
 * bundling pvcam64.dll and pvcamDDI.dll does not work

 * develop with PVCAM SDK 3.10.1.1
 * Tested on Photometrics Evolve 512 monochrome EMCCD camera SN A10D1033015
 * Tested on Photometrics 95B "GS144BSI" 
 * Tested on Photometrics TMP-Kinetix
 */
package directCameraReadout.pvcamsdk;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
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
import directCameraReadout.control.FrameCounter;
import directCameraReadout.control.FrameCounterX;
import directCameraReadout.gui.cameraConstant;
import directCameraReadout.workers.Workers.*;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV2Instant;
import static directCameraReadout.workers.Workers.SynchronizerWorkerInstant;
import static directCameraReadout.workers.Workers.LiveVideoWorkerV3Instant;
import static directCameraReadout.workers.Workers.BufferToStackWorkerInstant;
import static directCameraReadout.workers.Workers.CumulativeACFWorkerV3Instant;
import static directCameraReadout.workers.Workers.ICCSWorkerInstant;
import static directCameraReadout.workers.Workers.NonCumulativeACFWorkerV3Instant;
import static version.VERSION.DCR_VERSION;
import static version.VERSION.PVCAMSDK4_VERSION;
import static directCameraReadout.gui.cameraConstant.Common_Photometrics;

public class Photometrics_PVCAM_SDK {

    // NOTE: 
    // DCR_VERSION of the used PVCAM library.
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

    //pvcam 3.9.0.4-PMQI_Relase_Setup
    private static final String PVCAM64 = "pvcam64.dll";
    private static final String PVCAMDDI = "pvcamDDI.dll";

    final static int impwinposx = 1110;
    final static int impwinposy = 125;

    static {

        printlogdll("*********************PVCAM***********************");

//        try {
//            System.load("C:\\Users\\Administrator\\Documents\\GitHub\\PVCamSDK_GitHubRepo\\java\\pvcam_sdk\\src\\pvcamsdk\\JNIPhotometricsPVCAMsdk.dll");
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
//        String[] dllToBeChecked = {PVCAM64, PVCAMDDI}; 
        String[] dllToBeChecked = {}; //user has to install PVCAM release setup to operate Photometrics camera
        Boolean[] isDLLinSystem32 = new Boolean[dllToBeChecked.length];
        Boolean[] WriteDLL = new Boolean[dllToBeChecked.length];
        Arrays.fill(WriteDLL, Boolean.FALSE);
        String thisAlert;

        String osname = System.getProperty("os.name");
        String osnamelc = osname.toLowerCase();
        if (osnamelc.contains("win")) {
            libName = "JNIPhotometricsPVCAMsdk.dll";
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
                tempdir = new File(tempdir.toString() + "/PhotometricsSDK");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                File liveReadoutPhotometricsSDK_dir = new File(tempdir.toString() + "/" + PVCAMSDK4_VERSION);
                libDirPath = liveReadoutPhotometricsSDK_dir.toString();

//                boolean Write_pvcam64 = IsWindows, Write_pvcamddi = IsWindows; //pvcam
                boolean Write_SDKlibrary = true; //native program

                if (liveReadoutPhotometricsSDK_dir.exists() && liveReadoutPhotometricsSDK_dir.isDirectory()) {
                    // Directory exists. Check if the libraries are in the folder.
                    if (IsWindows) {
                        Write_SDKlibrary = !(new File(liveReadoutPhotometricsSDK_dir.toString() + "/" + libName).exists());
//                        Write_pvcam64 = !(new File(liveReadoutPhotometricsSDK_dir.toString() + "/" + PVCAM64).exists());
//                        Write_pvcamddi = !(new File(liveReadoutPhotometricsSDK_dir.toString() + "/" + PVCAMDDI).exists());
                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                WriteDLL[i] = !(new File(liveReadoutPhotometricsSDK_dir.toString() + "/" + dllToBeChecked[i]).exists());
                            }
                        }
                    }
                } else {
                    liveReadoutPhotometricsSDK_dir.mkdir();
                }

                if (Fiji_jars_dir.canWrite()) {
                    //user computer with admin rights
                    WriteToTempDir = false;

                    if (Write_SDKlibrary) {
                        writeLibraryFile(liveReadoutPhotometricsSDK_dir, libName, false);
                    }

//                    if (Write_pvcam64) {
//                        writeLibraryFile(liveReadoutPhotometricsSDK_dir, PVCAM64, false);
//                    }
//
//                    if (Write_pvcamddi) {
//                        writeLibraryFile(liveReadoutPhotometricsSDK_dir, PVCAMDDI, false);
//                    }
                    for (int i = 0; i < dllToBeChecked.length; i++) {
                        if (WriteDLL[i]) {
                            writeLibraryFile(liveReadoutPhotometricsSDK_dir, dllToBeChecked[i], false);
                        }
                    }
                } else {
                    WriteToTempDir = Write_SDKlibrary || Arrays.asList(WriteDLL).contains(true);
//                    WriteToTempDir = Write_SDKlibrary || Write_pvcam64 || Write_pvcamddi;
                }

            }

            if (WriteToTempDir) {
                // write files to temporary folder.
                File tmpDir;
                try {
                    tmpDir = Files.createTempDirectory("liveReadoutPhotometricsSDK-lib").toFile();
                    tmpDir.deleteOnExit();
                    libDirPath = tmpDir.toString();
                    if (IsWindows) {
                        writeLibraryFile(tmpDir, libName, true);
//                        writeLibraryFile(tmpDir, PVCAM64, true);
//                        writeLibraryFile(tmpDir, PVCAMDDI, true);

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                writeLibraryFile(tmpDir, dllToBeChecked[i], true);
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Photometrics_PVCAM_SDK.class.getName()).log(Level.SEVERE, null, ex);
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
//                    System.load(libDirPath + "/" + PVCAM64);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + PVCAM64;
//                    IJ.log(thisAlert);
//                }
//
//                try {
//                    System.load(libDirPath + "/" + PVCAMDDI);
//                } catch (Exception e) {
//                    thisAlert = "Unable to load " + PVCAMDDI;
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
                            String html = "<html><body width='%1s'><h3>Missing PVCAM driver</h3>"
                                    + "<p>To run Photometrics camera, please install PVCAM driver .<br><br>"
                                    + "<p>Link to installer: https://www.photometrics.com/support/software-and-drivers";
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

    public static native int isPHOTOMETRICSconnectedPVCAM(); // 0 = good to go; 195 = third party software accessing same camera; 186 = no camera detected; 3 = PVCAM Released setup not installed

    public static native String GetModelPVCAM(); // return model name previously storen when checking for camera presences

    public static native String GetCameraNamePVCAM(); // return camera name previously storen when checking for camera presences

    public static native int InitializePVCAM(); // only initialize, need to call uninit when the program close

    public static native int SystemShutDownPVCAM(); //uninit

    public static native int[] GetDetectorDimPVCAM();

    public static native int setParameterPVCAM(double exposureTime, int Width, int Height, int Left, int Top, int Incamerabin, int acqmode, int totalframe, int buffersizeFrame, int arraysizePixel, int readoutPortIndex, int readoutSpeedIndex); //index Left Top start from 1; Width and Heught represent pixel size after factored in "physical binning/incamerabin"

    public static native double getKineticCyclePVCAM();

    public static native short[] runSingleScanPVCAM();

    public static native void runInfiniteLoopPVCAM(short[] outArray, FrameCounter fcObj);

    public static native void setStopMechanismPVCAM(boolean isStopPressed);

    public static native int debugMyData1PVCAM();

    public static native int debugMyData2PVCAM();

    public static native double getDoubleValuePVCAM(String ID); //ID - exposuretime; ID - frametime; ID - top; ID - left; ID - width; ID - height; ID - CPPtime1; ID - CPPtime2; ID - CPPtime3;ID - CPPtime4;ID - CPPtime5;ID - incamerabin; "frameTransfer"; "PMODE; "BIT_DEPTH"; "readoutFrequency";
    //1 total time elapse from start - end including JNI transfer
    //2 time set parameter
    //3 uns16 buffer to float array (average)
    //4 JNI transfer (average)
    //5 average transfer + JNI
    
    public static native int getPortSize(); //Get available port size
    
    public static native int getSpeedCount(int portIndex); //Get number of available speeds for this port
    
    public static native void setPortAndSpeedPair(int portIndex, int speedIndex);
    
    

    /*- inca
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
                int err = setParameterPVCAM(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 1, 1, Common.size_b, Common.arraysize, Common_Photometrics.readoutPortIndex, Common_Photometrics.readoutSpeedIndex);
                if (err != 0) {
                    IJ.showMessage("SetParameter return " + err);
                }
                Common.kineticCycleTime = Common.exposureTime;
//                Common.kineticCycleTime = 1 / getKineticCyclePVCAM(); // TODO: get real kinetic cycle time

                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                Common.arraysingleS = runSingleScanPVCAM();

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

                if (!isFF) { // display extra images onto Fiji
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
                            Common.ip.putPixel(x, y, (short) Common.arraysingleS[index]);
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
                setParameterPVCAM(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Photometrics.readoutPortIndex, Common_Photometrics.readoutSpeedIndex);

                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = Common.exposureTime;
//                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
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
                setParameterPVCAM(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Photometrics.readoutPortIndex, Common_Photometrics.readoutSpeedIndex);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = Common.exposureTime;
//                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
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
                setParameterPVCAM(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, Common.totalFrame, fbuffersize, Common.arraysize, Common_Photometrics.readoutPortIndex, Common_Photometrics.readoutSpeedIndex);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = Common.exposureTime;
//                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
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
                System.out.println("***Acquisition V3");
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
                setParameterPVCAM(Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, Common.inCameraBinning, 2, 1000000000, fbuffersize, Common.arraysize, Common_Photometrics.readoutPortIndex, Common_Photometrics.readoutSpeedIndex);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = Common.exposureTime;
//                Common.kineticCycleTime = 1 / getKineticCycleSDK4(); // TODO: get real kinetic cycle time
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
            runInfiniteLoopPVCAM(array, Common.framecounter);
        }

    }

    private static void writeLibraryFile(File Directory, String LibName, Boolean DeleteOnExit) {
        try {
            //NOTE: include package name, which becomes the folder name in .jar file.'
            InputStream in = ClassLoader.class.getResourceAsStream("/directCameraReadout/pvcamsdk/" + LibName);
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
        int nopm = 20;
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
        metadatatag[t++] = "Frame time(s)";
        metadatatag[t++] = "Exposure time (ms)";
        metadatatag[t++] = "Sensor Height (pixels)";
        metadatatag[t++] = "Sensor Width (pixels)";
        metadatatag[t++] = "Frame Transfer";
        metadatatag[t++] = "PMode";
        metadatatag[t++] = "Bit-depth";
        metadatatag[t++] = "Readout frequency (MHz)";
        metadatatag[t++] = "Software";
        metadatatag[t++] = "SDK";
        metadatatag[t++] = "Time Stamp";

        t = 0;
        metadatavalue[t++] = Integer.toString(Common.framecounterIMSX.getCount());
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("width"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("height"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("left"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("top"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("incamerabin"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("incamerabin"));
        metadatavalue[t++] = Photometrics_PVCAM_SDK.GetModelPVCAM();
        metadatavalue[t++] = Photometrics_PVCAM_SDK.GetCameraNamePVCAM();
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("frametime"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("exposuretime"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.GetDetectorDimPVCAM()[0]);
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.GetDetectorDimPVCAM()[1]);
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("frameTransfer"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("PMODE"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("BIT_DEPTH"));
        metadatavalue[t++] = Double.toString(Photometrics_PVCAM_SDK.getDoubleValuePVCAM("readoutFrequency"));
        metadatavalue[t++] = "DirectCameraReadout_" + DCR_VERSION;
        metadatavalue[t++] = "SDK_" + PVCAMSDK4_VERSION;
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
