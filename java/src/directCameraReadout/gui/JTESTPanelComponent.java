package directCameraReadout.gui;

import ij.IJ;
import ij.gui.GenericDialog;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import directCameraReadout.gui.DirectCapturePanel.*;
import directCameraReadout.gui.DirectCapturePanel.ORpanel;
import directCameraReadout.system.SystemInfo;
import ij.ImagePlus;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;

public class JTESTPanelComponent extends JFrame {

    ORpanel ORPobj;
    JPanel JTESTPane;
    JButton btnTest;
    JButton btnTest3;
    JButton btnTest4;

    public JTESTPanelComponent(ORpanel ORobj) {

        final int TestpanelPosX = 420;										// control panel, "ImFCS", position and dimensions
        final int TestpanelPosY = 125;
        final int TestpanelDimX = 250;
        final int TestpanelDimY = 150;

        this.btnTest = DirectCapturePanel.btnTest;
        this.ORPobj = ORobj;

        JTESTPane = new JPanel(new GridLayout(3, 2));
        JTESTPane.setBorder(BorderFactory.createTitledBorder("For-debugging"));

        //initialize
        btnTest = new JButton("Mem check");
        JButton btnTest2 = new JButton("");
        JButton btnTest3 = new JButton("");
        JButton btnTest4 = new JButton("");

        //FocusFinderPane (top panel)
        JTESTPane.add(btnTest);
        JTESTPane.add(btnTest2);
        JTESTPane.add(btnTest3);
        JTESTPane.add(btnTest4);
        JTESTPane.add(new JLabel(""));
        JTESTPane.add(new JLabel(""));

        //add listeners
        btnTest.addActionListener(btnTestPressed);
        btnTest2.addActionListener(btnTest2Pressed);
        btnTest3.addActionListener(btnTest3Pressed);
        btnTest4.addActionListener(btnTest4Pressed);

        Container cp = this.getContentPane();
        cp.setLayout(new BorderLayout(1, 1));
        cp.add(JTESTPane, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setTitle("Debug Panel");
        setSize(TestpanelDimX, TestpanelDimY);
        setLocation(new Point(TestpanelPosX, TestpanelPosY));
        setFocusable(true);
        setResizable(false);
        setVisible(true);

    }

    private boolean cumulativePlotIntervalDialog() {
        GenericDialog gd = new GenericDialog("Cumulative Plot Interval");
        gd.addMessage("Setting to 1 will cause back-to-back calculation at expense of drop in screen fps");
        gd.addMessage("Increase this paramter as you go higher ROI and framerate");
        gd.addNumericField("cumul PI: ", Common.cumulativePlotInterval, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return false;
        }
        Common.cumulativePlotInterval = (int) gd.getNextNumber();
        IJ.log("cumulativePlotInterval: " + Common.cumulativePlotInterval);
        return true;
    }

    private boolean bckgDialog() {
        GenericDialog gd = new GenericDialog("Background");
        gd.addMessage("Setting bacgkound >= 1,000,000 will set min counts as bacgkround");
        gd.addNumericField("background: ", Common.background, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {

            return false;
        }
        Common.background = (int) gd.getNextNumber();
        IJ.log("background: " + Common.background);

        return true;
    }

    private boolean maxSizePerStacksDialog() {
        int sizeInGB = (int) Math.floor(Common.maximumBytePerStack / Math.pow(10, 9));
        int ret;
        GenericDialog gd = new GenericDialog("File saving");
        gd.addMessage("Enter your preferred maxium size per imagestacks (in GB). File larger than this value will be splitted into multiple .tiff");
        gd.addNumericField("Max GB per stack", sizeInGB, 0);
        gd.showDialog();
        if (gd.wasOKed()) {
            ret = (int) gd.getNextNumber();
            Common.maximumBytePerStack = ret * Math.pow(10, 9);
            IJ.log("maximumBytePerStack: " + Common.maximumBytePerStack);
            return true;
        }
        return false;

    }

    ActionListener btnTest4Pressed = (ActionEvent event) -> {
//        SystemInfo.explicitGC();
//        long time = System.nanoTime();
//        /* 
//            Excplicit GC
//         */
//        Common.bufferArray1D = null;
//        Common.imp_cum = null;
//        Common.ims_cum = null;
//        Common.ims_nonCumRed = null;
//        Common.ims_nonCumGreen = null;
//        System.gc();
//        /*
//            Excplicit GC
//         */
//        IJ.log("ran explicit GC: " + (System.nanoTime() - time) / 1000000 + " ms");

//        IJ.log("Green ROI lLeft: " + Common.lLeft + ", lTop: " + Common.lTop + ", lWidth: " + Common.lWidth + ", lHeight: " + Common.lHeight);
//        IJ.log("CCFx: " + Common.CCFdistX + ", CCFy: " + Common.CCFdistY + ", ShiftX: " + Common.ICCSShiftX + ", ShiftY: " + Common.ICCSShiftY);
//        IJ.log("isICCSValid: " + Common.isICCSValid);
    };

    ActionListener btnTest3Pressed = (ActionEvent event) -> {
//        maxSizePerStacksDialog();
    };

    ActionListener btnTest2Pressed = (ActionEvent event) -> {
//        bckgDialog();
    };

    ActionListener btnTestPressed = (ActionEvent event) -> {

        IJ.log("***********");
        IJ.log("Total designated memory: " + SystemInfo.totalDesignatedMemory() / 1000000 + " MB");
        IJ.log("Total allocated memory: " + SystemInfo.totalAllocatedMemory() / 1000000 + " MB");
        IJ.log("Current allocated free memory: " + SystemInfo.currentAllocatedFreeMemory() / 1000000 + " MB");
        IJ.log("Used memory: " + SystemInfo.usedMemory() / 1000000 + " MB");
        IJ.log("Total free memory: " + SystemInfo.totalFreeMemory() / 1000000 + " MB");
//
//        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
//        IJ.log("Non-heap: " + m.getNonHeapMemoryUsage().getMax());
//        IJ.log("Heap: " + m.getHeapMemoryUsage().getMax());
//        for (MemoryPoolMXBean mp : ManagementFactory.getMemoryPoolMXBeans()) {
//            IJ.log("Pool: " + mp.getName()
//                    + " (type " + mp.getType() + ")"
//                    + " = " + mp.getUsage().getMax());
//        }
//        IJ.log("pass by constructor: " + ORPobj.TEST_ORP);
//
//        IJ.log("--------");
//        IJ.log("PlotAmplitude: " + DirectCapturePanel.Common.plotCalibAmplitude);
//        IJ.log("PlotDiffusion: " + DirectCapturePanel.Common.plotCalibDiffusion);
//        IJ.log("Plot Interval: " + DirectCapturePanel.Common.plotInterval);
//        IJ.log("binX: " + Common.BinXSoft + ", binY: " + Common.BinYSoft);
//        IJ.log("ccfX: " + Common.CCFdistX + ", ccfY: " + Common.CCFdistY);
//        IJ.log("no pts aver: " + Common.noptsavr);
//        IJ.log("background gui: " + Common.background);
//        IJ.log("--------");
//        IJ.log("no averages: " + Common.noptsavr);
//        IJ.log("---");
//        IJ.log("lWidth: " + Common.lWidth + ", lHeight: " + Common.lHeight + ", lLeft: " + Common.lLeft + ", pTop: " + Common.lTop);
//        IJ.log("ICCSShiftX: " + Common.ICCSShiftX + ", ICCSShiftY: " + Common.ICCSShiftY);
//        IJ.log("CCFx: " + Common.CCFdistX + ", CCFy: " + Common.CCFdistY);
//        IJ.log("--------");
//            IJ.log("$camera: " + $camera);
//            IJ.log("iscropmode: " + Common.isCropMode);
//            IJ.log("size_a: " + Common.size_a + ", size_b: " + Common.size_b + ", fti: " + Common.transferFrameInterval + ", fps: " + Common.fps);
//
//            if (Common.selectedMode == "Live Video" || Common.selectedMode == "Calibration") {
//                IJ.log("framecounter: " + Common.framecounter.getCount());
//                IJ.log("direct call framecounter: " + Common.framecounter.count);
//            }
//            if (Common.selectedMode == "Acquisition") {
//                IJ.log("framecounter: " + Common.framecounter.getCount());
//                IJ.log("direct call framecounter: " + Common.framecounter.count);
//                IJ.log("framecounterIMS: " + Common.framecounterIMS.getCount());
//                IJ.log("direct call framecounterIMS: " + Common.framecounterIMS.count);
//            }
//
//            if (DirectCapturePanel.$camera.equals("EVOLVE- 512")) {
//                IJ.log("mydata1: " + Photometrics_PVCAM_SDK.debugMyData1PVCAM());
//                IJ.log("mydata2: " + Photometrics_PVCAM_SDK.debugMyData2PVCAM());
//                
//                IJ.log("CPP time elapsed total: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("CPPtime1"));
//                IJ.log("CPP time elapsed setparameter: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("CPPtime2"));
//                IJ.log("CPP average uns16 to float transfer: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("CPPtime3"));
//                IJ.log("CPP average JNI transfer: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("CPPtime4"));
//                IJ.log("CPP average uns16tofloat + JNI: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("CPPtime5"));
//                        
//                IJ.log("CPP exposuretime: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("exposuretime"));
//                IJ.log("CPP frametime: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("frametime"));
//                IJ.log("CPP width: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("width"));
//                IJ.log("CPP height: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("height"));
//                IJ.log("CPP left: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("left"));
//                IJ.log("CPP top: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("top"));
//                IJ.log("CPP incamerabin: " + Photometrics_PVCAM_SDK.getDoubleValuePVCAM("incamerabin"));
//                
//                IJ.log("Detector dim X: " + Photometrics_PVCAM_SDK.GetDetectorDimPVCAM()[0]);
//                IJ.log("Detector dim Y: " + Photometrics_PVCAM_SDK.GetDetectorDimPVCAM()[1]);
//                IJ.log("Model: " + Photometrics_PVCAM_SDK.GetModelPVCAM());
//                IJ.log("Cam name: " + Photometrics_PVCAM_SDK.GetCameraNamePVCAM());
//                
//            }
//
//            IJ.log("isSaveDone: " + Common.isSaveDone);
//            IJ.log("Common.imp_cum is null: " + (Common.imp_cum == null));
//MODEL, CAMERAID, BUS
//            IJ.log("Size T: ");
//            IJ.log("Size X: " + Hamamatsu_DCAM_SDK4.getIntegerSDK4("WIDTH"));
//            IJ.log("Size Y: " + Hamamatsu_DCAM_SDK4.getIntegerSDK4("HEIGHT"));
//            IJ.log("AOI Left: " +  Hamamatsu_DCAM_SDK4.getIntegerSDK4("LEFT"));
//            IJ.log("AOI Top: " + Hamamatsu_DCAM_SDK4.getIntegerSDK4("TOP"));
//            IJ.log("AOI Horizontal Bin: " + Hamamatsu_DCAM_SDK4.getIntegerSDK4("BIN"));
//            IJ.log("AOI Vertical Bin: " + Hamamatsu_DCAM_SDK4.getIntegerSDK4("BIN"));
//            IJ.log("Camera Model: " + Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL"));
//            IJ.log("Camera ID: " + Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID"));
//            IJ.log("Interface Type: " + Hamamatsu_DCAM_SDK4.GetStringSDK4("BUS"));
//            IJ.log("Acquisition cycle time (Hz): " +  Hamamatsu_DCAM_SDK4.getKineticCycleSDK4());
//            IJ.log("Exposure time (in secondsL: " + Hamamatsu_DCAM_SDK4.getExposureTimeSDK4());
//            IJ.log("Pixel Height (um): " +  Hamamatsu_DCAM_SDK4.getChipSizeSDK4()[0]);
//            IJ.log("Pixel Width (um): " + Hamamatsu_DCAM_SDK4.getChipSizeSDK4()[1]);
//            IJ.log("Sensor Height (pixels): " +  Hamamatsu_DCAM_SDK4.getDetectorDimensionSDK4()[0]);
//            IJ.log("Sensor Width (pixels): " + Hamamatsu_DCAM_SDK4.getDetectorDimensionSDK4()[1]);
//            IJ.log("Coeff: " + Hamamatsu_DCAM_SDK4.GetDoubleSDK4("CONVERSIONFACTOR_COEFF"));
//            IJ.log("Offset: " + Hamamatsu_DCAM_SDK4.GetDoubleSDK4("CONVERSIONFACTOR_OFFSET"));
//            IJ.log("Bits per channel: " + Hamamatsu_DCAM_SDK4.GetDoubleSDK4("BITSPERCHANNEL"));
//            IJ.log("global $camear: " + $camera);
//            IJ.log("camera: " + Common.$cameraHeadModel);
//            IJ.log("serial no: " + Common.$serialNumber);
//            IJ.log("----------------------------");
//            IJ.log("isshutsystempressed: " + Common.isShutSystemPressed + ", isstoppreeedd: " + Common.isStopPressed + ",isacquisitioRuning: " + Common.isAcquisitionRunning + ", isPrematureTermination: " + Common.isPrematureTermination + ",isSaveDone: " + Common.isSaveDone + ", isPlotACF: " + Common.isPlotACFdone + ", isImageready: " + Common.isImageReady);
//            IJ.log("size_a: " + Common.size_a + ", size_b: " + Common.size_b + ", fti: " + Common.transferFrameInterval + ", fps: " + Common.fps);
//            IJ.log("CameraHeadmodel/serial: " + Common.$cameraHeadModel + "/" + Common.$serialNumber + ", selectedmode: " + Common.selectedMode);
//            IJ.log("ExposureTime: " + Common.exposureTime + ", totalFrame: " + Common.totalFrame + ", plotInterval: " + Common.plotInterval + ", kinetic cycle: " + Common.kineticCycleTime);
//            IJ.log("BinXSoft: " + Common.BinXSoft + " , BinYSoft: " + Common.BinYSoft + ", incamerabining: " + Common.inCameraBinning);
//            IJ.log("MaxPixelWidth: " + Common.MAXpixelwidth + ", MaxPixelHeight: " + Common.MAXpixelheight + ", minHeight: " + Common.minHeight);
//            IJ.log("oWidth: " + Common.oWidth + ", oHeight: " + Common.oHeight + ", oLeft: " + Common.oLeft + ", oTop: " + Common.oTop + ", oRight: " + Common.oRight + ", oBottom: " + Common.oBottom);
//            IJ.log("temperature: " + Common.temperature);
//            IJ.log("min temp: " + Common.mintemp + ", maxtemp: " + Common.maxtemp + ", isCooling: " + Common.isCooling);
//            IJ.log("RunLiveReadoutonGPU: " + Common.RunLiveReadOutOnGPU + ", isgpupresent: " + Common.isgpupresent);
//            IJ.log("BleachCor: " + Common.bleachCor + ", polydegree: " + Common.polynomDegree + ", p: " + Common.correlator_p + ",q: " + Common.correlator_q);
//            IJ.log("is plot display ACF: " + Common.plotACFCurves + ", trace: " + Common.plotTrace + ", live: " + Common.showLiveVideoCumul);
//            IJ.log("live left: " + Common.lLeft + ", live top: " + Common.lTop + ", live width: " + Common.lWidth + ", live height: " + Common.lHeight);
//            IJ.log("----------------------------");
//            IJ.log("Sona debug---");
//
//            IJ.log("sona max chip size x: " + AndorSDK3v2.GetIntegerValueSDK3("SensorWidth"));
//            IJ.log("sona max chip size y: " + AndorSDK3v2.GetIntegerValueSDK3("SensorHeight"));
//            IJ.log("sona min temp: " + (int) AndorSDK3v2.GetFloatMinSDK3("TargetSensorTemperature"));
//            IJ.log("sona max temp: " + (int) AndorSDK3v2.GetFloatMaxSDK3("TargetSensorTemperature"));
//            IJ.log("min temp: " + Common.mintemp + ", maxtemp: " + Common.maxtemp + ", isCooling: " + Common.isCooling);
//            IJ.log("target sensor temp: " + AndorSDK3v2.GetDoubleValueSDK3("TargetSensorTemperature"));
//            IJ.log("get sensor temperature: " + AndorSDK3v2.GetDoubleValueSDK3("SensorTemperature"));
//            IJ.log("fan status: " + AndorSDK3v2.GetEnumeratedStringSDK3("FanSpeed"));
//            IJ.log("tempstatus: " + AndorSDK3v2.GetEnumeratedStringSDK3("TemperatureStatus"));
//            IJ.log("Temperature status: " + Common.tempStatus[1] + ", Temperature: " + Common.tempStatus[0]);
//            IJ.log("BinXSoft: " + Common.BinXSoft + " , BinYSoft: " + Common.BinYSoft + ", incamerabining: " + Common.inCameraBinning);
//            IJ.log("CameraHeadmodel/serial: " + Common.$cameraHeadModel + "/" + Common.$serialNumber + ", selectedmode: " + Common.selectedMode);
//            IJ.log("isshutsystempressed: " + Common.isShutSystemPressed + ", isstoppreeedd: " + Common.isStopPressed + ",isacquisitioRuning: " + Common.isAcquisitionRunning + ", isPrematureTermination: " + Common.isPrematureTermination + ",isSaveDone: " + Common.isSaveDone + ", isPlotACF: " + Common.isPlotACFdone + ", isImageready: " + Common.isImageReady);
//            IJ.log("size_a: " + Common.size_a + ", size_b: " + Common.size_b + ", fti: " + Common.transferFrameInterval + ", fps: " + Common.fps);
//            IJ.log("MaxPixelWidth: " + Common.MAXpixelwidth + ", MaxPixelHeight: " + Common.MAXpixelheight + ", minHeight: " + Common.minHeight);
//            IJ.log("oWidth: " + Common.oWidth + ", oHeight: " + Common.oHeight + ", oLeft: " + Common.oLeft + ", oTop: " + Common.oTop + ", oRight: " + Common.oRight + ", oBottom: " + Common.oBottom);
//            IJ.log("ExposureTime: " + Common.exposureTime + ", totalFrame: " + Common.totalFrame + ", plotInterval: " + Common.plotInterval + ", kinetic cycle: " + Common.kineticCycleTime);
//            IJ.log("Selected pixel encoding: " + Common_SONA.PixelEncoding);
//
//            IJ.log("Sona debug---");
//            if (Common.isAcquisitionRunning) {
//                IJ.log("framecounter: " + Common.framecounter.getCount());
//                IJ.log("framecounterIMS: " + Common.framecounterIMS.getCount());
//            }
//            IJ.log("----------------------------");
//            IJ.log("isshutsystempressed: " + Common.isShutSystemPressed + ", isstoppreeedd: " + Common.isStopPressed + ",isacquisitioRuning: " + Common.isAcquisitionRunning + ", isPrematureTermination: " + Common.isPrematureTermination + ",isSaveDone: " + Common.isSaveDone + ", isPlotACF: " + Common.isPlotACFdone + ", isImageready: " + Common.isImageReady);
//            IJ.log("size_a: " + Common.size_a + ", size_b: " + Common.size_b + ", fti: " + Common.transferFrameInterval + ", fps: " + Common.fps);
//            IJ.log("CameraHeadmodel/serial: " + Common.$cameraHeadModel + "/" + Common.$serialNumber + ", selectedmode: " + Common.selectedMode);
//            IJ.log("ExposureTime: " + Common.exposureTime + ", totalFrame: " + Common.totalFrame + ", plotInterval: " + Common.plotInterval + ", kinetic cycle: " + Common.kineticCycleTime);
//            IJ.log("BinXSoft: " + Common.BinXSoft + " , BinYSoft: " + Common.BinYSoft + ", incamerabining: " + Common.inCameraBinning);
//            IJ.log("MaxPixelWidth: " + Common.MAXpixelwidth + ", MaxPixelHeight: " + Common.MAXpixelheight + ", minHeight: " + Common.minHeight);
//            IJ.log("oWidth: " + Common.oWidth + ", oHeight: " + Common.oHeight + ", oLeft: " + Common.oLeft + ", oTop: " + Common.oTop + ", oRight: " + Common.oRight + ", oBottom: " + Common.oBottom);
//            IJ.log("EmGain: " + Common.EMgain + ", temperature: " + Common.temperature);
//            IJ.log("min temp: " + Common.mintemp + ", maxtemp: " + Common.maxtemp + ", isCooling: " + Common.isCooling);
//            IJ.log("RunLiveReadoutonGPU: " + Common.RunLiveReadOutOnGPU + ", isgpupresent: " + Common.isgpupresent);
//            IJ.log("BleachCor: " + Common.bleachCor + ", polydegree: " + Common.polynomDegree + ", p: " + Common.correlator_p + ",q: " + Common.correlator_q);
//            IJ.log("is plot display ACF: " + Common.plotACFCurves + ", trace: " + Common.plotTrace + ", live: " + Common.showLiveVideoCumul);
//            IJ.log("live left: " + Common.lLeft + ", live top: " + Common.lTop + ", live width: " + Common.lWidth + ", live height: " + Common.lHeight);
//            IJ.log("index Vspeed: " + Common.iVSpeed + "/" + Common.VspeedArr.get(Common.iVSpeed).toString() + ", index Vamp: " + Common.iVSamp + "/" + Common.VSAmpArr.get(Common.iVSamp).toString() + ", index Hspeed: " + Common.iHSpeed + "/" + Common.HspeedArr.get(Common.iHSpeed).toString() + ", index preamp: " + Common.iPreamp + "/" + Common.PreAmpGainArr.get(Common.iPreamp).toString());
//            IJ.log("isCropMode: " + Common.isCropMode + "cWidth: " + Common.cWidth + ", cHeight: " + Common.cHeight + ", cLeft: " + Common.cLeft + ", cRight: " + Common.cRight + ", cTop: " + Common.cTop + ", cBottom: " + Common.cBottom);
//            IJ.log("----------------------------");
//
//            int[] arrayzero = AndorSDK2v3.GetArrayZero();
//            int[] arrayndzero = AndorSDK2v3.GetArrayndZero();
//            int[] arraydicardzero = AndorSDK2v3.GetArraydiscardZero();
//
//            for (int i = 0; i < arrayzero.length; i++) {
//                IJ.log("arrayzero: " + arrayzero[i]);
//            }
//            for (int i = 0; i < arrayndzero.length; i++) {
//                IJ.log("arrayndzero: " + arrayndzero[i]);
//            }
//            for (int i = 0; i < arraydicardzero.length; i++) {
//                IJ.log("arraydicardzero: " + arraydicardzero[i]);
//            }
    };
}
