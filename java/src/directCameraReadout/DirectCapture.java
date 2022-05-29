/*
 *  Supported camera
 *  1) Andor iXon "DU860_BV" "DU888_BV" "DU897_BV", Sona "SONA-4BV11"  
 *  2) Photometrics "EVOLVE- 512" "GS144BSI"
 *  3) Hamamatsu Orca Flash 4.0 "C11440-22CU" "C11440-22C" "C13440-20CU" "C13440-20C" "C15550-20UP"
 */
package directCameraReadout;

import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.gui.DirectCapturePanel.Common;
import directCameraReadout.andorsdk2v3.AndorSDK2v3;
import directCameraReadout.andorsdk3v2.AndorSDK3v2;
import directCameraReadout.hamadcamsdk4.Hamamatsu_DCAM_SDK4;
import directCameraReadout.pvcamsdk.Photometrics_PVCAM_SDK;
import ij.IJ;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class DirectCapture {

    private int isgpupresent;

    private String UserSelectedCameraModel;
    private String[] CameraModelAvail = new String[4]; //arraylist would be good
    private boolean[] isCameraAvailable = new boolean[4]; //0 = iXon860 or iXon888 or iXon897 ; 1 = Sona11 or Sona65 ; 2 = Hamamatsu Orca Flash 4.0 v2 or v3 ; 3 = Photometrics Evolve 512
    private String[] CameraType = {
        "none",
        "none",
        "none",
        "none"
    };

    private DirectCapturePanel.ORpanel ORpanelobj; //universal for all camera

    //iXon860 iXon888 iXon897
    private boolean isEMCCDconnected;
    private boolean isEMCCDpreviouslyConnected;
    private int SDK2InitializationErrorCode;
    //Sona11 Sona65
    private boolean isSCMOSconnected;
    private boolean isSCMOSpreviouslyConnected;
    private int SDK3InitializationErrorCode;
    //Hamamatsu Orca Flash 4.0 v2 and v3
    private boolean isHAMAconnected;
    private boolean isHAMApreviouslyConnected;
    private int SDK4InitializationErrorCode;
    //Photometrics Evolve512, Prime95B
    private boolean isPHOTOMETRICSconnected;
    private boolean isPHOTOMETRICSpreviouslyConnected;
    private int PVCAMInitializationErrorCode;

    // Direct Capture Selection frame
    JFrame DCframe;
    private JComboBox<String> cbCameraSelection;
    private JTextField tfDCsomeinfo;
    private JButton btnStartCamera;
    private final int DCPosX = 390;
    private final int DCPosY = 50;
    private final int DCDimX = 300;
    private final int DCDimY = 100;

    public DirectCapture(int isgpupresent) {
        this.isgpupresent = isgpupresent;
        System.out.println("Direct Camera Readout: " + DirectCapturePanel.VERSION);
        System.out.println("SDK2: " + AndorSDK2v3.VERSION + " loaded " + AndorSDK2v3.isSDKload());
        System.out.println("SDK3: " + AndorSDK3v2.VERSION + " loaded " + AndorSDK3v2.isSDKload());
        System.out.println("SDK4: " + Hamamatsu_DCAM_SDK4.VERSION + " loaded " + Hamamatsu_DCAM_SDK4.isSDKload());
        System.out.println("PVCAM: " + Photometrics_PVCAM_SDK.VERSION + " loaded " + Photometrics_PVCAM_SDK.isSDKload());
    }

    private void TRYonecamera() {
        //start camera adn display messae if third party software is accessing camera
        for (int i = 0; i < isCameraAvailable.length; i++) {
            if (isCameraAvailable[i] == true) {
                //Do something to turn on
                switch (CameraType[i]) {
                    case "DU860_BV":
                        // if program alrady running iXon860
                        if (isEMCCDpreviouslyConnected) {
                            IJ.showMessage("program is already running DU860_BV");
                            break;
                        }
                        if (SDK2InitializationErrorCode == 20992) {
                            IJ.showMessage("Please exit Andor Solis/uManager");
                            break;
                        }
                        //otherwise if iXon is not already running
                        isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                        if (isEMCCDconnected) {
                            Common.isShutSystemPressed = false;
                            isEMCCDpreviouslyConnected = true;
                            AndorSDK2v3.InitializeEMCCDSDK2();
                            AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon860.defaultTemp);
                            AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                            Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                            Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;

                    case "DU888_BV":
                        // if program alrady running iXon888
                        if (isEMCCDpreviouslyConnected) {
                            IJ.showMessage("program is already running DU860_BV");
                            break;
                        }
                        if (SDK2InitializationErrorCode == 20992) {
                            IJ.showMessage("Please exit Andor Solis/uManager");
                            break;
                        }
                        //otherwise if iXon is not already running
                        isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                        if (isEMCCDconnected) {
                            Common.isShutSystemPressed = false;
                            isEMCCDpreviouslyConnected = true;
                            AndorSDK2v3.InitializeEMCCDSDK2();
                            AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon888.defaultTemp);
                            AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                            Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                            Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;

                    case "DU897_BV":
                        // if program alrady running iXon888
                        if (isEMCCDpreviouslyConnected) {
                            IJ.showMessage("program is already running DU897_BV");
                            break;
                        }
                        if (SDK2InitializationErrorCode == 20992) {
                            IJ.showMessage("Please exit Andor Solis/uManager");
                            break;
                        }
                        //otherwise if iXon is not already running
                        isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                        if (isEMCCDconnected) {
                            Common.isShutSystemPressed = false;
                            isEMCCDpreviouslyConnected = true;
                            AndorSDK2v3.InitializeEMCCDSDK2();
                            AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon897.defaultTemp);
                            AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                            Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                            Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;

                    case "SONA-4BV11":
                        if (isSCMOSpreviouslyConnected) {
                            IJ.showMessage("program is already running Sona11");
                            break;
                        }

                        if (SDK3InitializationErrorCode == 17) {
                            IJ.showMessage("Please exit Andor Solis/uManager");
                        }

                        isSCMOSconnected = (SDK3InitializationErrorCode == 0);
                        if (isSCMOSconnected) {
                            Common.isShutSystemPressed = false;
                            isSCMOSpreviouslyConnected = true;
                            AndorSDK3v2.InitializeSystemSDK3();
                            AndorSDK3v2.SetBooleanValueSDK3("SensorCooling", 1); //turn on cooling
                            AndorSDK3v2.SetEnumeratedStringSDK3("FanSpeed", "Low"); //setting fan to low
                            AndorSDK3v2.SetEnumIndexSDK3("TemperatureControl", AndorSDK3v2.GetEnumCountSDK3("TemperatureControl") - 1); // setting to -45C or lowest temp
                            Common.$serialNumber = AndorSDK3v2.GetStringValueSDK3("SerialNumber");
                            Common.$cameraHeadModel = AndorSDK3v2.GetStringValueSDK3("CameraModel");
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;

                    case "C11440-22CU":
                    case "C11440-22C":
                    case "C13440-20CU":
                    case "C13440-20C":
                    case "C15550-20UP":
                        if (isHAMApreviouslyConnected) {
                            IJ.showMessage("program is already running Hamamatsu");
                            break;
                        }

                        isHAMAconnected = (SDK4InitializationErrorCode == 0);
                        if (isHAMAconnected) {
                            Common.isShutSystemPressed = false;
                            isHAMApreviouslyConnected = true;
                            int del = Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                            Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                            Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;

                    case "EVOLVE- 512":
                    case "GS144BSI":
                        if (isPHOTOMETRICSpreviouslyConnected) {
                            IJ.showMessage("program is already running Photometrics Evolve");
                            break;
                        }

                        isPHOTOMETRICSconnected = (PVCAMInitializationErrorCode == 0);
                        if (isPHOTOMETRICSconnected) {
                            Common.isShutSystemPressed = false;
                            isPHOTOMETRICSpreviouslyConnected = true;
                            Photometrics_PVCAM_SDK.InitializePVCAM();
                            Common.$serialNumber = Photometrics_PVCAM_SDK.GetCameraNamePVCAM();
                            Common.$cameraHeadModel = Photometrics_PVCAM_SDK.GetModelPVCAM();
                            ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[i]);
                            DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                            Common.isgpupresent = isgpupresent;
                        }
                        break;
                    case "none":
                        if (i == 0) {
                            if (SDK2InitializationErrorCode == 20992) {
                                IJ.showMessage("Please exit Andor Solis/uManager");
                            }
                        }
                        if (i == 1) {
                            if (SDK3InitializationErrorCode == 17) {
                                IJ.showMessage("Please exit Andor Solis/uManager");
                            }
                            if (SDK3InitializationErrorCode == 1) {
                                IJ.showMessage("Please connect Andor Sona");
                            }
                            if (SDK3InitializationErrorCode == 99) {
                                IJ.showMessage("Sona other error");
                            }
                        }
                        if (i == 2) {
                            if (SDK4InitializationErrorCode == 1) {
                                IJ.showMessage("no cam connected/camera accessed by third party software");
                            }
                            if (SDK4InitializationErrorCode == 2) {
                                IJ.showMessage("Hamamatsu other error");
                            }
                        }
                        if (i == 3) {
                            if (PVCAMInitializationErrorCode == 195) {
                                IJ.showMessage("Please exit third party software accessing camera");
                            }
                            if (PVCAMInitializationErrorCode == 186) {
                                IJ.showMessage("no camera is connected");
                            }
                            if (PVCAMInitializationErrorCode != 195 || PVCAMInitializationErrorCode != 186) {
                                IJ.showMessage("Photometrics other cmaera relted error");
                            }
                        }
                }
            }
        }

    }

    private void TRYmultiplecamera() {
        //Start ixon860 
        if (UserSelectedCameraModel == "DU860_BV") {
            // if program alrady running iXon860
            if (isEMCCDpreviouslyConnected) {
                IJ.showMessage("program is already running DU860_BV");
            } else if (SDK2InitializationErrorCode == 20992) {
                IJ.showMessage("Please exit Andor Solis/uManager");
            } else {
                //otherwise if iXon is not already running
                isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                if (isEMCCDconnected) {
                    Common.isShutSystemPressed = false;
                    isEMCCDpreviouslyConnected = true;
                    AndorSDK2v3.InitializeEMCCDSDK2();
                    AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon860.defaultTemp);
                    AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                    Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                    Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[0]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        if (UserSelectedCameraModel == "DU888_BV") {
            // if program alrady running iXon860
            if (isEMCCDpreviouslyConnected) {
                IJ.showMessage("program is already running DU888_BV");
            } else if (SDK2InitializationErrorCode == 20992) {
                IJ.showMessage("Please exit Andor Solis/uManager");
            } else {
                //otherwise if iXon is not already running
                isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                if (isEMCCDconnected) {
                    Common.isShutSystemPressed = false;
                    isEMCCDpreviouslyConnected = true;
                    AndorSDK2v3.InitializeEMCCDSDK2();
                    AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon888.defaultTemp);
                    AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                    Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                    Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[0]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        if (UserSelectedCameraModel == "DU897_BV") {
            // if program alrady running iXon860
            if (isEMCCDpreviouslyConnected) {
                IJ.showMessage("program is already running DU897_BV");
            } else if (SDK2InitializationErrorCode == 20992) {
                IJ.showMessage("Please exit Andor Solis/uManager");
            } else {
                //otherwise if iXon is not already running
                isEMCCDconnected = (SDK2InitializationErrorCode == 20002);
                if (isEMCCDconnected) {
                    Common.isShutSystemPressed = false;
                    isEMCCDpreviouslyConnected = true;
                    AndorSDK2v3.InitializeEMCCDSDK2();
                    AndorSDK2v3.SetTemperatureSDK2(DirectCapturePanel.Common_iXon897.defaultTemp);
                    AndorSDK2v3.SetFanModeSDK2(0); //Setting to full by default
                    Common.$serialNumber = Integer.toString(AndorSDK2v3.getCameraSerialNumSDK2());
                    Common.$cameraHeadModel = AndorSDK2v3.getHeadModelSDK2();
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[0]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        //Start sona11
        if (UserSelectedCameraModel == "SONA-4BV11") {
            if (isSCMOSpreviouslyConnected) {
                IJ.showMessage("program is already running SONA-4BV11");
            } else if (SDK3InitializationErrorCode == 17) {
                IJ.showMessage("Please exit Andor Solis/uManager");
            } else {
                isSCMOSconnected = (SDK3InitializationErrorCode == 0);
                if (isSCMOSconnected) {
                    Common.isShutSystemPressed = false;
                    isSCMOSpreviouslyConnected = true;
                    AndorSDK3v2.InitializeSystemSDK3();
                    AndorSDK3v2.SetBooleanValueSDK3("SensorCooling", 1); //turn on cooling
                    AndorSDK3v2.SetEnumeratedStringSDK3("FanSpeed", "Low"); //setting fan to low
                    AndorSDK3v2.SetEnumIndexSDK3("TemperatureControl", AndorSDK3v2.GetEnumCountSDK3("TemperatureControl") - 1); // setting to -45C or lowest temp
                    Common.$serialNumber = AndorSDK3v2.GetStringValueSDK3("SerialNumber");
                    Common.$cameraHeadModel = AndorSDK3v2.GetStringValueSDK3("CameraModel");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[1]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        if (UserSelectedCameraModel == "C11440-22CU") {
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("program is already running Hamamatsu");
            } else {
                isHAMAconnected = (SDK4InitializationErrorCode == 0);
                if (isHAMAconnected) {
                    Common.isShutSystemPressed = false;
                    isHAMApreviouslyConnected = true;
                    Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                    Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                    Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[2]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        if (UserSelectedCameraModel == "C11440-22C") {
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("program is already running Hamamatsu");
            } else {
                isHAMAconnected = (SDK4InitializationErrorCode == 0);
                if (isHAMAconnected) {
                    Common.isShutSystemPressed = false;
                    isHAMApreviouslyConnected = true;
                    Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                    Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                    Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[2]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }

        }

        if (UserSelectedCameraModel == "C13440-20CU") {
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("program is already running Hamamatsu");
            } else {
                isHAMAconnected = (SDK4InitializationErrorCode == 0);
                if (isHAMAconnected) {
                    Common.isShutSystemPressed = false;
                    isHAMApreviouslyConnected = true;
                    Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                    Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                    Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[2]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }
        if (UserSelectedCameraModel == "C13440-20C") {
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("program is already running Hamamatsu");
            } else {
                isHAMAconnected = (SDK4InitializationErrorCode == 0);
                if (isHAMAconnected) {
                    Common.isShutSystemPressed = false;
                    isHAMApreviouslyConnected = true;
                    Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                    Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                    Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[2]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }
        if (UserSelectedCameraModel == "C15550-20UP") {
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("program is already running Hamamatsu");
            } else {
                isHAMAconnected = (SDK4InitializationErrorCode == 0);
                if (isHAMAconnected) {
                    Common.isShutSystemPressed = false;
                    isHAMApreviouslyConnected = true;
                    Hamamatsu_DCAM_SDK4.InitializeHamaSDK4();
                    Common.$serialNumber = Hamamatsu_DCAM_SDK4.GetStringSDK4("CAMERAID");
                    Common.$cameraHeadModel = Hamamatsu_DCAM_SDK4.GetStringSDK4("MODEL");
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[2]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }
        

        if (UserSelectedCameraModel == "EVOLVE- 512") {

            if (isPHOTOMETRICSpreviouslyConnected) {
                IJ.showMessage("program is already running Photometrics camera");
            } else {
                isPHOTOMETRICSconnected = (PVCAMInitializationErrorCode == 0);
                if (isPHOTOMETRICSconnected) {
                    Common.isShutSystemPressed = false;
                    isPHOTOMETRICSpreviouslyConnected = true;
                    Photometrics_PVCAM_SDK.InitializePVCAM();
                    Common.$serialNumber = Photometrics_PVCAM_SDK.GetCameraNamePVCAM();
                    Common.$cameraHeadModel = Photometrics_PVCAM_SDK.GetModelPVCAM();
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[3]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }

        if (UserSelectedCameraModel == "GS144BSI") {

            if (isPHOTOMETRICSpreviouslyConnected) {
                IJ.showMessage("program is already running Photometrics camera");
            } else {
                isPHOTOMETRICSconnected = (PVCAMInitializationErrorCode == 0);
                if (isPHOTOMETRICSconnected) {
                    Common.isShutSystemPressed = false;
                    isPHOTOMETRICSpreviouslyConnected = true;
                    Photometrics_PVCAM_SDK.InitializePVCAM();
                    Common.$serialNumber = Photometrics_PVCAM_SDK.GetCameraNamePVCAM();
                    Common.$cameraHeadModel = Photometrics_PVCAM_SDK.GetModelPVCAM();
                    ORpanelobj = new DirectCapturePanel().new ORpanel(CameraType[3]);
                    DirectCapturePanel.JDirectCapturepanelComponentPanel.setVisible(true);
                    Common.isgpupresent = isgpupresent;
                }
            }
        }
    }

    private String returnCamera(String full) {
        if (full.contains("DU860")) {
            return "DU860_BV";
        }
        if (full.contains("DU888")) {
            return "DU888_BV";
        }
        if (full.contains("DU897")) {
            return "DU897_BV";
        }
        if (full.contains("SONA-4BV11")) {
            return "SONA-4BV11";
        }
        if (full.contains("C11440-22CU")) {
            return "C11440-22CU";
        }
        if (full.contains("C11440-22C")) {
            return "C11440-22C";
        }
        if (full.contains("C13440-20CU")) {
            return "C13440-20CU";
        }
        if (full.contains("C13440-20C")) {
            return "C13440-20C";
        }
        if (full.contains("C15550-20UP")) {
            return "C15550-20UP";
        }
        if (full.contains("EVOLVE- 512")) {
            return "EVOLVE- 512";
        }
        if (full.contains("GS144BSI")) {
            return "GS144BSI";
        }

        return "none";
    }

    public void check() {
        boolean proceed;

        //control flow
        if (isEMCCDconnected && Common.isShutSystemPressed) {
            isEMCCDpreviouslyConnected = false;
            isEMCCDconnected = false;
        }
        if (isSCMOSconnected && Common.isShutSystemPressed) {
            isSCMOSpreviouslyConnected = false;
            isSCMOSconnected = false;
        }
        if (isHAMAconnected && Common.isShutSystemPressed) {
            isHAMApreviouslyConnected = false;
            isHAMAconnected = false;
        }
        if (isPHOTOMETRICSconnected && Common.isShutSystemPressed) {
            isPHOTOMETRICSpreviouslyConnected = false;
            isPHOTOMETRICSconnected = false;
        }

        proceed = !isEMCCDpreviouslyConnected && !isSCMOSpreviouslyConnected && !isHAMApreviouslyConnected && !isPHOTOMETRICSpreviouslyConnected;

        if (proceed) {

            SDK2InitializationErrorCode = AndorSDK2v3.isEMCCDconnectedSDK2();
            SDK3InitializationErrorCode = AndorSDK3v2.isSCMOSconnectedSDK3();
            try {
                SDK4InitializationErrorCode = Hamamatsu_DCAM_SDK4.isHAMAconnectedSDK4();
            } catch (java.lang.UnsatisfiedLinkError er) {
                SDK4InitializationErrorCode = 3; //DCAM-API not installed
            }

            try {
                PVCAMInitializationErrorCode = Photometrics_PVCAM_SDK.isPHOTOMETRICSconnectedPVCAM();
            } catch (java.lang.UnsatisfiedLinkError er) {
                PVCAMInitializationErrorCode = 3; //PVCAM Release Setup not installed
            }

            isCameraAvailable[0] = (SDK2InitializationErrorCode == 20002 || SDK2InitializationErrorCode == 20992); //return true if camera is available
            isCameraAvailable[1] = (SDK3InitializationErrorCode == 0 || SDK3InitializationErrorCode == 17); //return true if camera is available
            isCameraAvailable[2] = (SDK4InitializationErrorCode == 0); //return true if camera is available and not in used by other programs
            isCameraAvailable[3] = (PVCAMInitializationErrorCode == 0 || PVCAMInitializationErrorCode == 195); //return true if camera is available

            // check if each cameara is available
            int nocam = 0;
            for (boolean value : isCameraAvailable) {
                if (value) {
                    nocam++;
                }
            };

            //recoding head model and serial number
            if (SDK2InitializationErrorCode == 20002) {
                CameraModelAvail[0] = "Andor iXon " + AndorSDK2v3.getHeadModelSDK2() + " " + AndorSDK2v3.getCameraSerialNumSDK2();
                CameraType[0] = AndorSDK2v3.getHeadModelSDK2();
                AndorSDK2v3.ShutDownSDK2();
            }

            if (SDK3InitializationErrorCode == 0) {
                CameraModelAvail[1] = "Andor Sona " + AndorSDK3v2.GetStringValueSDK3("CameraModel") + " " + AndorSDK3v2.GetStringValueSDK3("SerialNumber");
                CameraType[1] = AndorSDK3v2.GetStringValueSDK3("CameraModel");
                AndorSDK3v2.ShutDownSDK3();
            }

            if (SDK4InitializationErrorCode == 0) {
                CameraType[2] = Hamamatsu_DCAM_SDK4.GetModelSDK4();
                CameraModelAvail[2] = "Hamamatsu " + CameraType[2];
            }

            if (PVCAMInitializationErrorCode == 0) {
                CameraType[3] = Photometrics_PVCAM_SDK.GetModelPVCAM();
                CameraModelAvail[3] = "Photometrics Evolve " + Photometrics_PVCAM_SDK.GetModelPVCAM() + " " + Photometrics_PVCAM_SDK.GetCameraNamePVCAM();
            }

            if (nocam > 1) {// if available camera > 1, open a selection dialog. Dispose dialog once camera started\
                //open selection dialog
                createDCSelectionPanel(isCameraAvailable);
            } else if (nocam == 1) {// if available camera == 1, start the camera
                TRYonecamera();
            } else {
                IJ.showMessage("no camera is connected");
            }

        } else {
            //show message which camera is already turned on
            if (isEMCCDpreviouslyConnected) {
                IJ.showMessage("iXon is already connected");
            }
            if (isSCMOSpreviouslyConnected) {
                IJ.showMessage("Sona is already connected");
            }
            if (isHAMApreviouslyConnected) {
                IJ.showMessage("Hamamatsu is already connected");
            }
            if (isPHOTOMETRICSpreviouslyConnected) {
                IJ.showMessage("Photometrics is already connected");
            }
        }
    }

    /*
    * Direct camera selection panel (multiple cameras)
    *
     */
    public void createDCSelectionPanel(boolean[] availcameralist) {
        DCframe = new JFrame("Camera Selection");
        DCframe.setFocusable(true);
        DCframe.setVisible(true);
        DCframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        DCframe.setLayout(new GridLayout(2, 2));
        DCframe.setLocation(new Point(DCPosX, DCPosY));
        DCframe.setSize(new Dimension(DCDimX, DCDimY));
        DCframe.setResizable(false);

        cbCameraSelection = new JComboBox<>();
        int count = 0;
        for (int i = 0; i < availcameralist.length; i++) {
            if (availcameralist[i] == true) {
                count++;
            }
        }
        IJ.log("Multiple cameras detected: " + count);
        if (availcameralist[0] == true) {
            cbCameraSelection.addItem(CameraType[0]);
        }
        if (availcameralist[1] == true) {
            cbCameraSelection.addItem(CameraType[1]);
        }
        if (availcameralist[2] == true) {
            cbCameraSelection.addItem(CameraType[2]);
        }
        if (availcameralist[3] == true) {
            cbCameraSelection.addItem(CameraType[3]);
        }

        btnStartCamera = new JButton("Start");

        // row 1
        DCframe.add(new JLabel("Camera selection"));
        DCframe.add(cbCameraSelection);
        UserSelectedCameraModel = returnCamera(cbCameraSelection.getSelectedItem().toString());

        // row 2
        DCframe.add(new JLabel("Start camera"));
        DCframe.add(btnStartCamera);

        // add listener
        btnStartCamera.addActionListener(btnStartCameraPressed);
        cbCameraSelection.addActionListener(cbCameraSelectionChanged);
    }

    ActionListener cbCameraSelectionChanged = (ActionEvent event) -> {
        UserSelectedCameraModel = returnCamera(cbCameraSelection.getSelectedItem().toString());
    };

    ActionListener btnStartCameraPressed = (ActionEvent ev) -> {
        TRYmultiplecamera();
        DCframe.setVisible(false);
    };

}
