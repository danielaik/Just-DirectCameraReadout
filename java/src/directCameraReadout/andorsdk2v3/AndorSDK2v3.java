/**
 *  SDK2 version = 2.103.30031.0
 *  Tested on DU860(Thorsten Lab DU-860D-CS0-BV-500-X-3667, DU-860E-CS0-BV-X-9388, DU-860D-CS0-BV-X-6682), DU888 (Loan from Andor UK), DU897 (Winston Lab)
 */
package directCameraReadout.andorsdk2v3;

import ij.IJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.process.ShortProcessor;
import java.awt.Color;
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
import java.sql.Timestamp;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.gui.DirectCapturePanel.Common;
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
import static version.VERSION.SDK2_VERSION;

public class AndorSDK2v3 {

    // NOTE: 
    // DCR_VERSION of the used SDK2 library.
    // DCR_VERSION must be updated when .dll/.so files are changed so that they are placed in a new sub-folder named after this DCR_VERSION num in Fiji.App > jars.
    private static void printlogthread(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

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

    //SDK2
    private static final String ATMCD64D = "atmcd64d.dll"; //sdk2

    //dll dependencies 
    private static final String UCRTBASED = "ucrtbased.dll"; //dependencies parent
    private static final String KERNEL32 = "kernel32.dll"; //dependencies parent
    private static final String VCRUNTIME140D = "vcruntime140d.dll"; //dependencies parent 
    private static final String VCRUNTIME140_1D = "vcruntime140_1d.dll"; //dependencies parent
    private static final String MSVCP140D = "msvcp140d.dll"; //dependencies parent

    //UNUSED dll dependencies
    private static final String NTDLL = "ntdll.dll";
    private static final String KERNELBASE = "KernelBase.dll";
    private static final String CONCRT140D = "concrt140d.dll";
    // dependencies for ATMCD64D
    private static final String MSVCRT = "msvcrt.dll";
    private static final String RPCRT4 = "rpcrt4.dll";
    private static final String CFGMGR32 = "cfgmgr32.dll";
    private static final String BCRYPTPRIMITIVES = "bcryptprimitives.dll";
    private static final String BCRYPT = "bcrypt.dll";
    private static final String CABINET = "cabinet.dll";
    private static final String MSASN1 = "msasn1.dll";
    private static final String CRYPTSP = "cryptsp.dll";
    private static final String NCRYPT = "ncrypt.dll";
    private static final String SSPICLI = "sspicli.dll";
    private static final String CRYPTBASE = "cryptbase.dll";
    private static final String PROFAPI = "profapi.dll";
    private static final String DNSAPI = "dnsapi.dll";
    private static final String DSPARSE = "dsparse.dll";
    private static final String SAMLIB = "samlib.dll";
    private static final String DPAPI = "dpapi.dll";
    private static final String CRYPT32 = "crypt32.dll";
    private static final String IPHLPAPI = "IPHLPAPI.dll";
    private static final String NETUTILS = "netutils.dll";
    private static final String LOGONCLI = "logoncli.dll";
    private static final String USERENV = "userenv.dll";
    private static final String MPR = "mpr.dll";
    private static final String DEVRTL = "devrtl.dll";
    private static final String DEVOBJ = "devobj.dll";
    private static final String GDI32 = "gdi32.dll";
    private static final String USER32 = "user32.dll";
    private static final String OLEAUT32 = "oleaut32.dll";
    private static final String WS2_32 = "ws2_32.dll";
    private static final String UMPDC = "umpdc.dll";
    private static final String DBGCORE = "dbgcore.dll";
    private static final String DBGHELP = "dbghelp.dll";
    private static final String WKSCLI = "wkscli.dll";
    private static final String SRVCLI = "srvcli.dll";
    private static final String MSVCP110_WIN = "msvcp110_win.dll";
    private static final String MSVCP_WIN = "msvcp_win.dll";
    private static final String TBS = "tbs.dll";
    private static final String WINHTTP = "winhttp.dll";
    private static final String XMLLITE = "xmllite.dll";
    private static final String DMENTERPRISEDIAGNOSTICS = "dmenterprisediagnostics.dll";
    private static final String POLICYMANAGER = "policymanager.dll";
    private static final String TPMCOREPROVISIONING = "TpmCoreProvisioning.dll";
    private static final String CRYPTNGC = "cryptngc.dll";
    private static final String WLDAP32 = "Wldap32.dll";
    private static final String CRYPTNET = "cryptnet.dll";
    private static final String DSROLE = "dsrole.dll";
    private static final String AUTHZ = "authz.dll";
    private static final String CERTCA = "certca.dll";
    private static final String CRYPTTPMEKSVC = "crypttpmeksvc.dll";
    private static final String RMCLIENT = "rmclient.dll";
    private static final String COMBASE = "combase.dll";
    private static final String WINTRUST = "wintrust.dll";
    private static final String BCD = "bcd.dll";
    private static final String FVECERTS = "fvecerts.dll";
    private static final String SAMCLI = "samcli.dll";
    private static final String FVEAPI = "fveapi.dll";
    private static final String FVESKYBACKUP = "fveskybackup.dll";
    private static final String FLTLIB = "fltLib.dll";
    private static final String VIRTDISK = "virtdisk.dll";
    private static final String WLDP = "wldp.dll";
    private static final String COREMESSAGING = "CoreMessaging.dll";
    private static final String SHCORE = "SHCore.dll";
    private static final String COREUICOMPONENTS = "CoreUIComponents.dll";
    private static final String WIN32U = "win32u.dll";
    private static final String D3DSCACHE = "D3DSCache.dll";
    private static final String DXILCONV = "dxilconv.dll";
    private static final String DXGI = "dxgi.dll";
    private static final String WER = "wer.dll";
    private static final String D3D12 = "D3D12.dll";
    private static final String D2D1 = "d2d1.dll";
    private static final String DCOMP = "dcomp.dll";
    private static final String WUCEFFECTS = "wuceffects.dll";
    private static final String DSCLIENT = "dsclient.dll";
    private static final String USERDATATYPEHELPERUTIL = "UserDataTypeHelperUtil.dll";
    private static final String CONTACTACTIVATION = "ContactActivation.dll";
    private static final String TWINAPIAPPCORE = "twinapi.appcore.dll";
    private static final String VAULTCLI = "vaultcli.dll";
    private static final String W32TOPL = "w32topl.dll";
    private static final String NTDSAPI = "ntdsapi.dll";
    private static final String WEBSERVICES = "webservices.dll";
    private static final String SRPAPI = "srpapi.dll";
    private static final String HTTPAPI = "httpapi.dll";
    private static final String ESENT = "esent.dll";
    private static final String IMAGEHLP = "imagehlp.dll";
    private static final String DBGMODEL = "DbgModel.dll";
    private static final String WINDOWSPERFORMANCERECORDERCONTROL = "windowsperformancerecordercontrol.dll";
    private static final String DBGENG = "dbgeng.dll";
    private static final String IERTUTIL = "iertutil.dll";
    private static final String ICU = "icu.dll";
    private static final String ICUUC = "icuuc.dll";
    private static final String ICUIN = "icuin.dll";
    private static final String CHAKRA = "Chakra.dll";
    private static final String SHLWAPI = "shlwapi.dll";
    private static final String ADVAPI32 = "advapi32.dll";
    private static final String OLE32 = "ole32.dll";
    private static final String EDGEISO = "edgeIso.dll";
    private static final String OLEACC = "oleacc.dll";
    private static final String BCP47LANGS = "BCP47Langs.dll";
    private static final String UIAUTOMATIONCORE = "UIAutomationCore.dll";
    private static final String WINDOWSCODECS = "WindowsCodecs.dll";
    private static final String WPAXHOLDER = "WpAXHolder.dll";
    private static final String FWBASE = "fwbase.dll";
    private static final String FWPOLICYIOMGR = "fwpolicyiomgr.dll";
    private static final String FIREWALLAPI = "FirewallAPI.dll";
    private static final String PROPSYS = "propsys.dll";
    private static final String URLMON = "urlmon.dll";
    private static final String TOKENBINDING = "tokenbinding.dll";
    private static final String WININET = "wininet.dll";
    private static final String CERTENROLL = "CertEnroll.dll";
    private static final String CREDUI = "credui.dll";
    private static final String HID = "hid.dll";
    private static final String WEBAUTHN = "webauthn.dll";
    private static final String NGCRECOVERY = "ngcrecovery.dll";
    private static final String DMCMNUTILS = "dmcmnutils.dll";
    private static final String IRI = "iri.dll";
    private static final String DMPUSHPROXY = "dmpushproxy.dll";
    private static final String OMADMAPI = "omadmapi.dll";
    private static final String CRYPTXML = "cryptxml.dll";
    private static final String MDMREGISTRATION = "mdmregistration.dll";
    private static final String DSREG = "dsreg.dll";
    private static final String NETAPI32 = "netapi32.dll";
    private static final String COML2 = "coml2.dll";
    private static final String SHELL32 = "shell32.dll";
    private static final String VERSIONDLL = "version.dll";
    private static final String MSILTCFG = "msiltcfg.dll";
    private static final String APPHELP = "apphelp.dll";
    private static final String BCP47MRM = "BCP47mrm.dll";
    private static final String MRMCORER = "MrmCoreR.dll";
    private static final String SCECLI = "scecli.dll";
    private static final String ACTIVEDS = "activeds.dll";
    private static final String DWMAPI = "dwmapi.dll";
    private static final String UXTHEME = "uxtheme.dll";
    private static final String DRVSTORE = "drvstore.dll";
    private static final String SPINF = "spinf.dll";
    private static final String SPFILEQ = "spfileq.dll";
    private static final String DRVSETUP = "drvsetup.dll";
    private static final String SETUPAPI = "setupapi.dll";

    final static int impwinposx = 1110;
    final static int impwinposy = 125;

    static {
        printlogdll("*********************SDK2***********************");

//        try {
//            System.load("C:\\Users\\Administrator\\Documents\\GitHub\\sdk2_ver3_GitHubRepo\\java\\andorsdk2_v3\\src\\andorsdk2v3\\JNIAndorSDK2v3.dll");
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
        String[] dllToBeChecked = {
            UCRTBASED,
            KERNEL32,
            VCRUNTIME140D,
            VCRUNTIME140_1D,
            MSVCP140D
        };
        Boolean[] isDLLinSystem32 = new Boolean[dllToBeChecked.length];
        Boolean[] WriteDLL = new Boolean[dllToBeChecked.length];

        Arrays.fill(WriteDLL, Boolean.FALSE);
        String thisAlert;

        String osname = System.getProperty("os.name");
        String osnamelc = osname.toLowerCase();

        if (osnamelc.contains(
                "win")) {
            libName = "JNIAndorSDK2v3.dll";
            IsWindows = true;
        } else {
            proceed = false;
            thisAlert = "Direct Camera Readout only supported in Windows.";
        }

        if (proceed) {

            IsOperatingSystemOK = true;

            File curr_dir = new File(System.getProperty("java.class.path"));
            File Fiji_jars_dir = curr_dir.getAbsoluteFile().getParentFile();

            //Checking if 5 dll(s) present on Windows/System32 
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
                tempdir = new File(tempdir.toString() + "/AndorSDK2");
                if (!tempdir.exists()) {
                    tempdir.mkdir();
                }

                File liveReadoutSDK2_dir = new File(tempdir.toString() + "/" + SDK2_VERSION);
                libDirPath = liveReadoutSDK2_dir.toString();
                boolean Write_Atmcd = IsWindows; //sdk3
                boolean Write_SDKlibrary = true; //native program

                if (liveReadoutSDK2_dir.exists() && liveReadoutSDK2_dir.isDirectory()) {
                    // Directory exists. Check if the libraries are in the folder.
                    if (IsWindows) {
                        Write_SDKlibrary = !(new File(liveReadoutSDK2_dir.toString() + "/" + libName).exists());

                        Write_Atmcd = !(new File(liveReadoutSDK2_dir.toString() + "/" + ATMCD64D).exists());

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                WriteDLL[i] = !(new File(liveReadoutSDK2_dir.toString() + "/" + dllToBeChecked[i]).exists());
                            }
                        }
                    }

                } else {
                    liveReadoutSDK2_dir.mkdir();
                }

                if (Fiji_jars_dir.canWrite()) {
                    //user computer with admin rights
                    WriteToTempDir = false;
                    if (Write_Atmcd) {
                        writeLibraryFile(liveReadoutSDK2_dir, ATMCD64D, false);
                    }

                    if (Write_SDKlibrary) {
                        writeLibraryFile(liveReadoutSDK2_dir, libName, false);
                    }

                    for (int i = 0; i < dllToBeChecked.length; i++) {
                        if (WriteDLL[i]) {
                            writeLibraryFile(liveReadoutSDK2_dir, dllToBeChecked[i], false);
                        }
                    }

                } else {
                    WriteToTempDir = Write_Atmcd || Write_SDKlibrary || Arrays.asList(WriteDLL).contains(true);
                }

            }

            if (WriteToTempDir) {
                // write files to temporary folder.
                File tmpDir;
                try {
                    tmpDir = Files.createTempDirectory("liveReadoutSDK2-lib").toFile();
                    tmpDir.deleteOnExit();
                    libDirPath = tmpDir.toString();
                    if (IsWindows) {
                        writeLibraryFile(tmpDir, ATMCD64D, true);
                        writeLibraryFile(tmpDir, libName, true);

                        for (int i = 0; i < dllToBeChecked.length; i++) {
                            if (!isDLLinSystem32[i]) {
                                writeLibraryFile(tmpDir, dllToBeChecked[i], true);
                            }
                        }
                    }

                } catch (IOException ex) {
                    Logger.getLogger(AndorSDK2v3.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (IsWindows) { //loading sequences matter. If encounter can't find dependencis library error --> load the dll last

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

    // JNI TEST
    public static native float seyHello(int n1, int n2);

    public native int sayHello();

    /*
    JNI EMCCD SDK2 starts here
     */
    public static native boolean InitializeEMCCDSDK2(); //Initialize SDK2 and turn on cooler

    public static native void SystemShutDownSDK2();//Turn off cooler and shutdown sdk2

    public static native void ShutDownSDK2(); // trigger ShutDown() API function

    public static native int[] getMinMaxTemperatureSDK2();

    public static native void SetTemperatureSDK2(int temp);

    public static native void SetCoolingSDK2(int iscooling);

    public static native int[] GetTemperatureAndStatusSDK2(); ////20037 temp not reached //20035 temp not stabilized //20034 temp off //20036 temp stabilized

    public static native int SetFanModeSDK2(int iFan);

    public static native void DoCameraAcquisitionSDK2(); //Trigger acquisition 

    public static native void setStopMechanismSDK2(boolean isStopCalled);

    public static native int isEMCCDconnectedSDK2();//20002 SUCCESS; 20990 NO CAMERA CONNECTED; 20992 Other application such as Andor Solis/uManager is accessing

    public static native void setParameterSingleSDK2(float exposuretime, int width, int height, int left, int top, int acqmode, int gain, int incamerabinning, int ixonmodel, int iVspeed, int iVamp, int iHspeed, int iPreAmpGain, int isCropMode, int croppedWidth, int croppedHeight, int croppedLeft, int croppedTop);// acqMode = 1 for single scan; AcqMode = 5 for runtillAbort

    public static native short[] runSingleScanSDK2();

    public static native boolean setParameterInfiniteLoopSDK2(int size_b, int transferFrameInterval, float exposureTime, int width, int height, int left, int top, int acqmode, int gain, int incamerabinning, int ixonmodel, int iVspeed, int iVamp, int iHspeed, int iPreAmpGain, int isCropMode, int croppedWidth, int croppedHeight, int croppedLeft, int croppedTop, int arraysize);

    public static synchronized native void runInfiniteLoopSDK2(short[] outArray, FrameCounter fcObj);

    public static native boolean setParameterContinuousAcquisitionSDK2(int size_b, int totalFrame, int transferFrameInterval, float exposureTimeCont, int width, int height, int left, int top, int acqmode, int gain, int incamerabinning, int ixonmodel, int iVspeed, int iVamp, int iHspeed, int iPreAmpGain, int isCropMode, int croppedWidth, int croppedHeight, int croppedLeft, int croppedTop, int arraysize);

    public static synchronized native void runContinuousScanAcquisitionSDK2(short[] outArray, FrameCounter fcObj);

    public static native boolean setParameterInfiniteLoopV2SDK2(int size_b, int totalFrame, float exposureTime, int width, int height, int left, int top, int acqmode, int gain, int incamerabinning, int ixonmodel, int iVspeed, int iVamp, int iHspeed, int iPreAmpGain, int isCropMode, int croppedWidth, int croppedHeight, int croppedLeft, int croppedTop); //UNUSED

    public static synchronized native void runInfiniteLoopV2SDK2(short[] outArray); // UNUSED

    // metadata
    public static native int[] getDetectorDimensionSDK2();

    public static native int getEMGainSDK2();

    public static native int getFrameTransferSDK2();

    public static native float getPreAmpGainSDK2();

    public static native float getVSSpeedSDK2();

    public static native int getVSClockVoltageSDK2();

    public static native int getnADchannelsSDK2();

    public static native float getHSSpeedSDK2();

    public static native int getNoAvailableHSSpeedSDK2();

    public static native float getFastestVerticalSpeedSDK2();

    public static native float getKineticCycleSDK2(); // getter; kinetic cycle != exposure time set

    public static native float getExposureTimeSDK2();

    public static native int getBaseLineClampStateSDK2();

    public static native int getBaseLineOffsetSDK2();

    public static native int getBitDepthSDK2(); //get bit depth per pixel for a particular AD converter

    public static native int getisCoolerOnSDK2();

    public static native int getWidthSDK2();

    public static native int getHeightSDK2();

    public static native int getLeftSDK2(); // index at 1

    public static native int getTopSDK2(); // index at 1

    public static native int getCameraSerialNumSDK2();

    public static native String getHeadModelSDK2();

    //zero frame problem
    public static native int[] GetArrayZero(); // inclusive

    public static native int[] GetArrayndZero(); // non discarded zero frame

    public static native int[] GetArraydiscardZero(); // discarded zero frame

    public static native String[] GetStringArray();

    public static native String[] GetAvailableVSAmplitudeSDK2();//V
    //888: Normal, +1, +2, +3, +4
    //886: Normal, +1, +2, +3, +4

    public static native String[] GetAvailableVSSpeedsSDK2();//V
    //888: 0.6, 1.13, 2.20, 4.33
    //860: 0.09, 0.10, 0.15, 0.25, 0.45

    public static native String[] GetAvailableHSSpeedsSDK2(); //V
    //888: 30, 20, 10, 1
    //860: 10, 5, 3

    public static native String[] GetAvailablePreAmpGainSDK2();// 99 means NA
    //888: 1, 2, 99 (for 888 lets set to Gain1 and Gain2
    //860: 1, 2.2, 4.5

    public static native void ShutterControlSDK2(boolean isOpen);

    public static native void TESTsetParam(int w, int h, int l, int t, int incamerabin);

    public static native void TESTcropMode(int isCropMode, int cropW, int cropH, int cropWbin, int cropHbin, int cropLeft, int cropTop); //left/top ; left/bottom result int the flip

    /*
    JNI EMCCD SDK2 ends here
     */
    public static void runThread_updatetemp() {
        SwingWorker<Void, List<Integer>> worker = new SwingWorker<Void, List<Integer>>() {
            @Override
            protected Void doInBackground() throws Exception {

                while (!Common.isShutSystemPressed) {
                    Common.tempStatus = GetTemperatureAndStatusSDK2();
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
                if (currtempstatus == 20036) {//20037 temp not reached //20035 temp not stabilized //20034 temp off //20036 temp stabilized
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLUE);
                } else if (currtempstatus == 20034) {
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLACK);
                } else {
                    DirectCapturePanel.tfTemperature.setBackground(Color.RED);
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
                setParameterSingleSDK2((float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 1, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop);
                Common.kineticCycleTime = getKineticCycleSDK2(); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                Common.arraysingleS = runSingleScanSDK2();

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
                setParameterInfiniteLoopSDK2(fbuffersize, Common.transferFrameInterval, (float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 5, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop, Common.arraysize); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant = new CppToJavaTransferInfWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV2Instant = new LiveVideoWorkerV2(Common.tempWidth, Common.tempHeight, latch);
                SynchronizerWorkerInstant = new SynchronizerWorker(latch);

                long timeelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
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
                setParameterInfiniteLoopSDK2(fbuffersize, Common.transferFrameInterval, (float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 5, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop, Common.arraysize); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant = new CppToJavaTransferInfWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
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
                setParameterContinuousAcquisitionSDK2(fbuffersize, Common.totalFrame, Common.transferFrameInterval, (float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 5, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop, Common.arraysize);
                //                setParameterInfiniteLoopV2SDK2(fbuffersize, Common.totalFrame, (float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 5, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop);
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");

                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferAcqWorkerEXTENDEDV2 CppToJavaTransferAcqWorkerEXTENDEDV2Instant = new CppToJavaTransferAcqWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                BufferToStackWorkerInstant = new BufferToStackWorker(Common.tempWidth, Common.tempHeight, Common.totalFrame, latch, Common.arraysize);
                CumulativeACFWorkerV3Instant = new CumulativeACFWorkerV3(latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferAcqWorkerEXTENDEDV2Instant.execute();
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
                setParameterInfiniteLoopSDK2(fbuffersize, Common.transferFrameInterval, (float) Common.exposureTime, Common.oWidth, Common.oHeight, Common.oLeft, Common.oTop, 5, Common.EMgain, Common.inCameraBinning, DirectCapturePanel.cameraint, Common.iVSpeed, Common.iVSamp, Common.iHSpeed, Common.iPreamp, Common.isCropMode, Common.cWidth, Common.cHeight, Common.cLeft, Common.cTop, Common.arraysize); //Setting parameter for infinite loop recordimg
                printlog("Time SetParameter: " + (System.currentTimeMillis() - timer1) + "ms");
                // Receive real kinetic cycle and display in GUI
                Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f", Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant = new CppToJavaTransferInfWorkerEXTENDEDV2(Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant = new LiveVideoWorkerV3(Common.tempWidth, Common.tempHeight, latch);
                ICCSWorkerInstant = new ICCSWorker(Common.tempWidth, Common.tempHeight, latch, Common.arraysize);

                long timelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
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

    private static class CppToJavaTransferInfWorkerEXTENDEDV2 extends CppTOJavaTransferWorkerV2 {

        public CppToJavaTransferInfWorkerEXTENDEDV2(short[] array, CountDownLatch latch) {
            super(array, latch);
        }

        @Override
        protected void runInfinteLoop() {
            runInfiniteLoopSDK2(array, Common.framecounter);
        }

    }

    private static class CppToJavaTransferAcqWorkerEXTENDEDV2 extends CppTOJavaTransferWorkerV2 {

        public CppToJavaTransferAcqWorkerEXTENDEDV2(short[] array, CountDownLatch latch) {
            super(array, latch);
        }

        @Override
        protected void runInfinteLoop() {
            runContinuousScanAcquisitionSDK2(array, Common.framecounter);
        }
    }

    private static void writeLibraryFile(File Directory, String LibName, Boolean DeleteOnExit) {
        try {
            //NOTE: include package name, which becomes the folder name in .jar file.'
            InputStream in = ClassLoader.class
                    .getResourceAsStream("/directCameraReadout/andorsdk2v3/" + LibName);
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
        int nopm = 26;
        String[] metadatatag = new String[nopm];
        String[] metadatavalue = new String[nopm];
        t = 0;
        metadatatag[t++] = "SizeT";
        metadatatag[t++] = "sizeX";
        metadatatag[t++] = "sizeY";
        metadatatag[t++] = "Image Coordinate Left (index start at 1)"; // Index start at 1
        metadatatag[t++] = "Image Coordinate Top (index start at 1)"; // Index start at 1
        metadatatag[t++] = "Acquisition cycle time (in seconds)";
        metadatatag[t++] = "Exposure time (in seconds)";
        metadatatag[t++] = "Baseline clamp";
        metadatatag[t++] = "Baseline offset";
        metadatatag[t++] = "Frame transfer";
        metadatatag[t++] = "Camera Model";
        metadatatag[t++] = "Camera Serial";
        metadatatag[t++] = "Physical binning X";
        metadatatag[t++] = "Physical binning Y";
        metadatatag[t++] = "Chip size X";
        metadatatag[t++] = "Chip size Y";
        metadatatag[t++] = "EM DAC";
        metadatatag[t++] = "Pre-amp";
        metadatatag[t++] = "Readout rate (Mhz)";
        metadatatag[t++] = "Bit depth AD converter";
        metadatatag[t++] = "Vertical clock voltage";
        metadatatag[t++] = "Vertical shift speed (usecs)";
        metadatatag[t++] = "Actual Temperature (C)";
        metadatatag[t++] = "Software";
        metadatatag[t++] = "SDK";
        metadatatag[t++] = "Time Stamp";

        t = 0;
        metadatavalue[t++] = Integer.toString(Common.framecounterIMSX.getCount());
        metadatavalue[t++] = Integer.toString(getWidthSDK2());
        metadatavalue[t++] = Integer.toString(getHeightSDK2());
        metadatavalue[t++] = Integer.toString(getLeftSDK2());
        metadatavalue[t++] = Integer.toString(getTopSDK2());
        metadatavalue[t++] = Float.toString(getKineticCycleSDK2());
        metadatavalue[t++] = Float.toString(getExposureTimeSDK2());
        metadatavalue[t++] = Integer.toString(getBaseLineClampStateSDK2());
        metadatavalue[t++] = Integer.toString(getBaseLineOffsetSDK2());
        metadatavalue[t++] = Integer.toString(getFrameTransferSDK2());
        metadatavalue[t++] = getHeadModelSDK2();
        metadatavalue[t++] = Integer.toString(getCameraSerialNumSDK2());
        metadatavalue[t++] = Integer.toString(Common.inCameraBinning);
        metadatavalue[t++] = Integer.toString(Common.inCameraBinning);
        int[] chipsize = getDetectorDimensionSDK2();
        metadatavalue[t++] = Integer.toString(chipsize[0]);
        metadatavalue[t++] = Integer.toString(chipsize[1]);
        metadatavalue[t++] = Integer.toString(getEMGainSDK2());
        metadatavalue[t++] = Float.toString(getPreAmpGainSDK2());
        metadatavalue[t++] = Float.toString(getHSSpeedSDK2());
        metadatavalue[t++] = Integer.toString(getBitDepthSDK2());
        metadatavalue[t++] = Integer.toString(getVSClockVoltageSDK2());
        metadatavalue[t++] = Float.toString(getVSSpeedSDK2());
        metadatavalue[t++] = Integer.toString(GetTemperatureAndStatusSDK2()[0]);
        metadatavalue[t++] = "DirectCameraReadout_" + DCR_VERSION;
        metadatavalue[t++] = "SDK_" + SDK2_VERSION;
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
