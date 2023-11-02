package directCameraReadout.workers;

import directCameraReadout.iccs.ICCS;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.Overlay;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingWorker;

import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.gui.DirectCapturePanel.Common;
import directCameraReadout.fcs.ImFCSCorrelator;
import static directCameraReadout.gui.DirectCapturePanel.Common.selected_livevideo_binningMode;
import static directCameraReadout.gui.DirectCapturePanel.DisplayImageObj;

import static directCameraReadout.gui.DirectCapturePanel.iccsObj1;
import directCameraReadout.gui.parameterName;
import directCameraReadout.gui.parameterName.liveVideoBinMode.liveVideoBinModeEnum;
import directCameraReadout.gui.parameterName.mode.modeEnum;

import static directCameraReadout.gui.parameterName.modeType.$amode;
import ij.WindowManager;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;

public class Workers {

    //V2 start here
    public static LiveVideoWorkerV2 LiveVideoWorkerV2Instant;
    public static SynchronizerWorker SynchronizerWorkerInstant;
    public static LiveVideoWorkerV3 LiveVideoWorkerV3Instant;
    public static BufferToStackWorker BufferToStackWorkerInstant;
    public static CumulativeACFWorkerV3 CumulativeACFWorkerV3Instant;
    public static NonCumulativeACFWorkerV3 NonCumulativeACFWorkerV3Instant;

    public static ICCSWorker ICCSWorkerInstant;

    //V2 end here
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

    public static class LiveVideoWorkerV2 extends SwingWorker<Void, Void> {

        int width;
        int height;
        int size;
        int size_f;
        CountDownLatch latch;
        boolean isSettingDynamicRange;
        int sleepTime;
        final int impwinposx = 1110;
        final int impwinposy = 125;
        String $impLiveVideo;
        boolean imagewindowready;

        public LiveVideoWorkerV2(int width, int height, CountDownLatch latch) {
            this.width = width;
            this.height = height;
            this.latch = latch;
            size = width * height;
            this.size_f = (Common.arraysize - (Common.arraysize % size)) / size;

            //reset live ROI 
            if (width < 6 || height < 6) {
                Common.lLeft = 1;
                Common.lTop = 1;
                Common.lWidth = 1;
                Common.lHeight = 1;
            } else {
                Common.lLeft = 1;
                Common.lTop = 1;
                Common.lWidth = 6;
                Common.lHeight = 6;
            }
            isSettingDynamicRange = true; //wheter to reset dynamic range every single frame; Setting to true might causes suddent flash when there is sudden change in max or min counts; setting to false allow user to use built in Fiji brightness tool
            imagewindowready = false;
            sleepTime = optimalLiveVideoSleepTime(Common.fps, Common.kineticCycleTime, Common.transferFrameInterval);
        }

        MouseListener impcanLiveMouseUsed = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                int memLeft = Common.lLeft;
                int memTop = Common.lTop;
                int memWidth = Common.lWidth;
                int memHeight = Common.lHeight;

                int px = e.getX();
                int py = e.getY();
                int initposx, initposy;
                int pixbinX = 1;
                int pixbinY = 1;
                int[] AOIsparam;

                boolean isCCFselectionOK;

                // geting the mouse coordinates
                initposx = (int) Math.floor(Common.impcan.offScreenX(px) / pixbinX);
                initposy = (int) Math.floor(Common.impcan.offScreenY(py) / pixbinY);

                AOIsparam = DirectCapturePanel.getRoiCoordinateFromCenter(initposx + 1, initposy + 1, Common.lWidth, Common.lHeight, Common.imp.getWidth(), Common.imp.getHeight()); //TODO add real width and height for acquisition

                // checker if selected ROI is valid for PCC mode
                if (Common.analysisMode.equals($amode[3])) {//Iccs
                    if ((AOIsparam[0] - 1 + Common.CCFdistX + Common.ICCSShiftX + Common.BinXSoft) > Common.oWidth || (AOIsparam[0] - 1 + Common.CCFdistX - Common.ICCSShiftX) < 0 || (AOIsparam[2] - 1 + Common.CCFdistY + Common.ICCSShiftY + Common.BinYSoft) > Common.oHeight || (AOIsparam[2] - 1 + Common.CCFdistY - Common.ICCSShiftY) < 0 || AOIsparam[0] - 1 + Common.BinXSoft > Common.oWidth || AOIsparam[2] - 1 + Common.BinYSoft > Common.oHeight) {

                        if (Common.isCCFmode) {
                            Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                            Common.impRoiLive.setStrokeColor(Color.GREEN);
                            Common.imp.setRoi(Common.impRoiLive);

                            Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX, Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY, Common.lWidth + (2 * Common.ICCSShiftX), Common.lHeight + (2 * Common.ICCSShiftY));
                            Common.impRoiLive2.setStrokeColor(Color.RED);
                            Overlay impov = new Overlay(Common.impRoiLive2);
                            Common.imp.setOverlay(impov);
                        } else {
                            Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                            Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                            Common.imp.setRoi(Common.impRoiLive);
                        }
                        return;
                    }
                }

                Common.lLeft = AOIsparam[0];
                Common.lTop = AOIsparam[2];
                Common.lWidth = Common.BinXSoft;
                Common.lHeight = Common.BinYSoft;

                if (Common.isCCFmode) {
                    isCCFselectionOK = directCameraReadout.gui.DirectCapturePanel.CCFselectorChecker(Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, Common.BinXSoft, Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    if (!isCCFselectionOK) {
                        Common.lLeft = memLeft;
                        Common.lTop = memTop;
                        Common.lWidth = memWidth;
                        Common.lHeight = memHeight;
                    }
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);

                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive2.setStrokeColor(Color.RED);
                    Overlay impov = new Overlay(Common.impRoiLive2);
                    Common.imp.setOverlay(impov);
                } else {
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);
                }

                if ((memLeft != Common.lLeft) || (memTop != Common.lTop) || (memWidth != Common.lWidth) || (memHeight != Common.lHeight)) {
                    Common.isResetCalibPlot = true;
                }

                if (Common.analysisMode.equals(parameterName.modeType.$amode[3])) {//Iccs
                    DirectCapturePanel.tfICCSRoi1Coord.setText(Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " + Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop));
                }

            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                int memLeft = Common.lLeft;
                int memTop = Common.lTop;
                int memWidth = Common.lWidth;
                int memHeight = Common.lHeight;

                Roi roi2 = Common.imp.getRoi();
                int templleft;
                int templtop;
                int templw;
                int templh;
                boolean isCCFselectionOK;

                if (roi2 != null) {
                    Rectangle rect = roi2.getBounds();
                    templleft = (int) rect.getX() + 1;
                    templtop = (int) rect.getY() + 1;
                    templw = (int) rect.getWidth();
                    templh = (int) rect.getHeight();

                    // checker if selected ROI is valid for PCC mode
                    if (Common.analysisMode.equals($amode[3])) {// Iccs
                        if ((templleft - 1 + Common.CCFdistX + Common.ICCSShiftX + templw) > Common.oWidth || (templleft - 1 + Common.CCFdistX - Common.ICCSShiftX) < 0 || (templtop - 1 + Common.CCFdistY + Common.ICCSShiftY + templh) > Common.oHeight || (templtop - 1 + Common.CCFdistY - Common.ICCSShiftY) < 0 || templleft - 1 + templw > Common.oWidth || templtop - 1 + templh > Common.oHeight) {

                            if (Common.isCCFmode) {
                                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                                Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX, Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY, Common.lWidth + (2 * Common.ICCSShiftX), Common.lHeight + (2 * Common.ICCSShiftY));
                                Common.impRoiLive2.setStrokeColor(Color.RED);
                                Overlay impov = new Overlay(Common.impRoiLive2);
                                Common.imp.setOverlay(impov);
                            } else {
                                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);
                            }

                            return;
                        }
                    }

                    Common.lLeft = (int) rect.getX() + 1;
                    Common.lTop = (int) rect.getY() + 1;
                    Common.lWidth = (int) rect.getWidth();
                    Common.lHeight = (int) rect.getHeight();
                }

                //check if selected ROI > current Bin; reassign bin if false and update tfBin
                if (Common.lWidth < Common.BinXSoft) {
                    Common.BinXSoft = Common.lWidth;
                }
                if (Common.lHeight < Common.BinYSoft) {
                    Common.BinYSoft = Common.lHeight;
                }

                if (Common.isCCFmode) {
                    isCCFselectionOK = directCameraReadout.gui.DirectCapturePanel.CCFselectorChecker(Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, Common.BinXSoft, Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    if (!isCCFselectionOK) {
                        Common.lLeft = memLeft;
                        Common.lTop = memTop;
                        Common.lWidth = memWidth;
                        Common.lHeight = memHeight;
                    }
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);

                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive2.setStrokeColor(Color.RED);
                    Overlay impov = new Overlay(Common.impRoiLive2);
                    Common.imp.setOverlay(impov);
                } else {
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);
                }

                if ((memLeft != Common.lLeft) || (memTop != Common.lTop) || (memWidth != Common.lWidth) || (memHeight != Common.lHeight)) {
                    Common.isResetCalibPlot = true;
                }

                if (Common.analysisMode.equals($amode[3])) {//Iccs
                    DirectCapturePanel.tfICCSRoi1Coord.setText(Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " + Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop));
                }

            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };

        private static int optimalLiveVideoSleepTime(int fps, double exposuretime, int frametransferinterval) {//how long to sleep thread in order to achieve certain fps

            double invbin = 1 / (exposuretime * frametransferinterval);
            if (invbin < fps) {
                return (int) Math.ceil(1000 / invbin); // perhaps round up
            } else {
                return (int) (1000 / fps);
            }

        }

        private void settingLiveImage() {
            Common.ip = new ShortProcessor(width, height);
            Common.imp = new ImagePlus("Live image", Common.ip);
            if (Common.impwin != null) {
                Common.impwin.close();
            }
            Common.imp.show();

            Common.impwin = Common.imp.getWindow();
            Common.impcan = Common.imp.getCanvas();
            Common.impcan.setFocusable(true);
            $impLiveVideo = Common.imp.getTitle();

            //prevent minimize and exit of image window
            Common.impwin.addWindowListener(getWindowAdapter());

            if (Common.showLiveVideoCumul) {
                Common.impwin.setVisible(true);
            } else {
                Common.impwin.setVisible(false);
            }

            //set location
            Common.impwin.setLocation(impwinposx, impwinposy);

            //enlarge image to see better pixels
            if (width >= height) {
                Common.scimp = Common.zoomFactor / width; //adjustable: zoomFactor is by default 250 (see parameter definitions), a value chosen as it produces a good size on the screen
            } else {
                Common.scimp = Common.zoomFactor / height;
            }
            if (Common.scimp < 1.0) {
                Common.scimp = 1.0;
            }
            Common.scimp *= 100;// transfrom this into %tage to run ImageJ command
            IJ.run(Common.imp, "Original Scale", "");
            IJ.run(Common.imp, "Set... ", "zoom=" + Common.scimp + " x=" + (int) Math.floor(width / 2) + " y=" + (int) Math.floor(height / 2));
            IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
            // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary

            //setting default ROI selector for live display 
            if (Common.selectedMode == modeEnum.CALIBRATION || Common.selectedMode == modeEnum.ACQUISITION) {
                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                Common.impRoiLive.setStrokeColor(Color.GREEN);
                Common.imp.setRoi(Common.impRoiLive);
                if (Common.isCCFmode) {
                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive2.setStrokeColor(Color.RED);
                    Overlay impov = new Overlay(Common.impRoiLive2);
                    Common.imp.setOverlay(impov);
                }
            }

            Common.impcan.addMouseListener(impcanLiveMouseUsed);
            imagewindowready = true;
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    try {
                        imagewindowready = false;
                        settingLiveImage();
                    } catch (Exception e) {
                    }

                }

                @Override
                public void windowIconified(WindowEvent we) {
                    try {
                        Frame liveframe = WindowManager.getFrame($impLiveVideo);
                        if (liveframe != null) {
                            WindowManager.toFront(liveframe);
                        }
                    } catch (Exception e) {
                        IJ.log("No window available.");
                    }
                }
            };
        }

        private int getBufferIndex(int frameAcquired) {
            // return index of first element of last available image
            if (frameAcquired == 0) {
                IJ.log("error25");
                return -1;
            }
            return (int) ((frameAcquired - Common.transferFrameInterval) % size_f) * size;
        }

        private void doDisplay(short[] tempPixelArr) {
            // assign last available image for display
            int count = SynchronizerWorkerInstant.getCounter();
            System.arraycopy(Common.bufferArray1D, getBufferIndex(count), tempPixelArr, 0, size); //
            Common.ip.setPixels(tempPixelArr);

            // Saturation warning
            if (Common.ip.getMax() > Common.WarningCounts) {
                IJ.log("Warning (Saturation): reduce exposure time or laser power");
            }

            // decide if were to reset dynamic range for every single frame 
            if (Common.selectedMode == modeEnum.LIVEVIDEO || Common.selectedMode == modeEnum.CALIBRATION) {
                Common.ip.resetMinAndMax(); //reset dynamic range for live mode and calibration mode
            } else { //fix dynamic range for acquisiton mode
                if (isSettingDynamicRange) {
                    Common.ip.setMinAndMax(Common.ip.getMin(), Common.ip.getMax());
                    isSettingDynamicRange = false;
                }
            }

            Common.imp.updateAndDraw();
            if (!Common.impwin.isVisible()) {
                Common.impwin.setVisible(true);// Problematic: startle settings combobox when called sequentially at fast rate; threfore is the if statement
            }
        }

        @Override
        protected Void doInBackground() throws Exception {

            Thread.currentThread().setName("LiveVideoWorkerV2");
            printlogthread("Staring thread: " + Thread.currentThread().getName());

            short[] tempPixelArr = new short[size]; //reusing tempPixelArray to hold portion of Java1Dbuffer

            settingLiveImage();

            while (!Common.isPrematureTermination) {
                if (Common.isPrematureTermination == true) {
                    break;
                }

                Common.cIsDisplayLatestFrame = false;
                synchronized (Common.locker1) {
                    while (!Common.cIsDisplayLatestFrame) {
                        Common.locker1.wait();
                        Common.cIsDisplayLatestFrame = true;
                    }
                }
                /*
                    Start work
                 */
                if (imagewindowready) {
                    doDisplay(tempPixelArr);
                }

                /*
                    End work
                 */
            }
            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return null;
        }

        @Override
        protected void done() {
            if (Common.imp != null) {
                Common.impwin.close();
                Common.imp.close();
                Common.imp = null;
                Common.impwin = null;
            }

            latch.countDown();
        }

    }

    public static class LiveVideoWorkerV3 extends SwingWorker<Void, Void> {

        int width;
        int height;
        int size;
        int size_f;
        CountDownLatch latch;
        boolean isSettingDynamicRange; // flag to ensure to reset the dynamic range when window is closed
        int sleepTime;
        final int impwinposx = 1110;
        final int impwinposy = 125;
        String $impLiveVideo;
        boolean imagewindowready;

        public LiveVideoWorkerV3(int width, int height, CountDownLatch latch) {
            this.width = width;
            this.height = height;
            this.latch = latch;
            size = width * height;
            this.size_f = (Common.arraysize - (Common.arraysize % size)) / size;

            //reset live ROI 
            resetROIselection(width, height);
            isSettingDynamicRange = true;
            imagewindowready = false;
            sleepTime = optimalLiveVideoSleepTime(Common.fps, Common.kineticCycleTime, Common.transferFrameInterval);
        }

        MouseListener impcanLiveMouseUsed = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (Common.analysisMode.equals($amode[3])) {//Iccs
                    return;
                }

                int memLeft = Common.lLeft;
                int memTop = Common.lTop;
                int memWidth = Common.lWidth;
                int memHeight = Common.lHeight;

                int px = e.getX();
                int py = e.getY();
                int initposx, initposy;
                int pixbinX = 1;
                int pixbinY = 1;
                int[] AOIsparam;

                boolean isCCFselectionOK;

                // geting the mouse coordinates
                initposx = (int) Math.floor(Common.impcan.offScreenX(px) / pixbinX);
                initposy = (int) Math.floor(Common.impcan.offScreenY(py) / pixbinY);

                AOIsparam = DirectCapturePanel.getRoiCoordinateFromCenter(initposx + 1, initposy + 1, Common.lWidth, Common.lHeight, Common.imp.getWidth(), Common.imp.getHeight()); //TODO add real width and height for acquisition

                Common.lLeft = AOIsparam[0];
                Common.lTop = AOIsparam[2];
                Common.lWidth = Common.BinXSoft;
                Common.lHeight = Common.BinYSoft;

                if (Common.isCCFmode) {
                    isCCFselectionOK = directCameraReadout.gui.DirectCapturePanel.CCFselectorChecker(Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, Common.BinXSoft, Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    if (!isCCFselectionOK) {
                        Common.lLeft = memLeft;
                        Common.lTop = memTop;
                        Common.lWidth = memWidth;
                        Common.lHeight = memHeight;
                    }
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);

                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive2.setStrokeColor(Color.RED);
                    Overlay impov = new Overlay(Common.impRoiLive2);
                    Common.imp.setOverlay(impov);
                } else {
                    Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                    Common.imp.setRoi(Common.impRoiLive);
                }

                if ((memLeft != Common.lLeft) || (memTop != Common.lTop) || (memWidth != Common.lWidth) || (memHeight != Common.lHeight)) {
                    Common.isResetCalibPlot = true;
                }

            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                int memLeft = Common.lLeft;
                int memTop = Common.lTop;
                int memWidth = Common.lWidth;
                int memHeight = Common.lHeight;

                Roi roi2 = Common.imp.getRoi();
                int templleft;
                int templtop;
                int templw;
                int templh;
                boolean isCCFselectionOK;

                if (roi2 != null) {
                    Rectangle rect = roi2.getBounds();
                    templleft = (int) rect.getX() + 1;
                    templtop = (int) rect.getY() + 1;
                    templw = (int) rect.getWidth();
                    templh = (int) rect.getHeight();

                    // checker if selected ROI is valid for PCC mode
                    if (Common.analysisMode.equals($amode[3])) {//Iccs
                        if ((templleft - 1 + Common.CCFdistX + Common.ICCSShiftX + templw) > Common.oWidth || (templleft - 1 + Common.CCFdistX - Common.ICCSShiftX) < 0 || (templtop - 1 + Common.CCFdistY + Common.ICCSShiftY + templh) > Common.oHeight || (templtop - 1 + Common.CCFdistY - Common.ICCSShiftY) < 0 || templleft - 1 + templw > Common.oWidth || templtop - 1 + templh > Common.oHeight) {
                            if (Common.isCCFmode) {
                                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                                Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX, Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY, Common.lWidth + (2 * Common.ICCSShiftX), Common.lHeight + (2 * Common.ICCSShiftY));
                                Common.impRoiLive2.setStrokeColor(Color.RED);
                                Overlay impov = new Overlay(Common.impRoiLive2);
                                Common.imp.setOverlay(impov);
                            } else {
                                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);
                            }

                            return;
                        } else {
                            Common.isICCSValid = true;
                        }
                    }

                    Common.lLeft = (int) rect.getX() + 1;
                    Common.lTop = (int) rect.getY() + 1;
                    Common.lWidth = (int) rect.getWidth();
                    Common.lHeight = (int) rect.getHeight();
                }

                //check if selected ROI > current Bin; reassign bin if false and update tfBin
                if (Common.lWidth < Common.BinXSoft) {
                    Common.BinXSoft = Common.lWidth;
                }
                if (Common.lHeight < Common.BinYSoft) {
                    Common.BinYSoft = Common.lHeight;
                }

                if (Common.analysisMode.equals($amode[3])) {//Iccs

                    if (Common.isCCFmode) {
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);

                        Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX, Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY, Common.lWidth + (2 * Common.ICCSShiftX), Common.lHeight + (2 * Common.ICCSShiftY));
                        Common.impRoiLive2.setStrokeColor(Color.RED);
                        Overlay impov = new Overlay(Common.impRoiLive2);
                        Common.imp.setOverlay(impov);
                    } else {
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);
                    }

                } else {
                    if (Common.isCCFmode) {
                        isCCFselectionOK = directCameraReadout.gui.DirectCapturePanel.CCFselectorChecker(Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, Common.BinXSoft, Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        if (!isCCFselectionOK) {
                            Common.lLeft = memLeft;
                            Common.lTop = memTop;
                            Common.lWidth = memWidth;
                            Common.lHeight = memHeight;
                        }
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);

                        Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive2.setStrokeColor(Color.RED);
                        Overlay impov = new Overlay(Common.impRoiLive2);
                        Common.imp.setOverlay(impov);
                    } else {
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(java.awt.Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);
                    }
                }

                if ((memLeft != Common.lLeft) || (memTop != Common.lTop) || (memWidth != Common.lWidth) || (memHeight != Common.lHeight)) {
                    Common.isResetCalibPlot = true;
                }

                if (Common.analysisMode.equals($amode[3])) {//Iccs
                    DirectCapturePanel.tfICCSRoi1Coord.setText(Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " + Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop));
                }

            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        };

        private static void resetROIselection(int w, int h) {

            if (Common.selectedMode != modeEnum.ICCS) {//Iccs 
                if (w < 6 || h < 6) {
                    Common.lLeft = 1;
                    Common.lTop = 1;
                    Common.lWidth = 1;
                    Common.lHeight = 1;
                } else {
                    Common.lLeft = 1;
                    Common.lTop = 1;
                    Common.lWidth = 6;
                    Common.lHeight = 6;
                }
            }

        }

        private static int optimalLiveVideoSleepTime(int fps, double exposuretime, int frametransferinterval) {//how long to sleep thread in order to achieve certain fps

            double invbin = 1 / (exposuretime * frametransferinterval);
            if (invbin < fps) {
                return (int) Math.ceil(1000 / invbin); // perhaps round up
            } else {
                return (int) (1000 / fps);
            }

        }

        private void settingLiveImage() {
            Common.ip = new ShortProcessor(width, height);
            Common.imp = new ImagePlus("Live image", Common.ip);
            if (Common.impwin != null) {
                Common.impwin.close();
            }
            Common.imp.show();

            Common.impwin = Common.imp.getWindow();
            Common.impcan = Common.imp.getCanvas();
            Common.impcan.setFocusable(true);

            if (Common.showLiveVideoCumul) {
                Common.impwin.setVisible(true);
            } else {
                Common.impwin.setVisible(false);
            }

            $impLiveVideo = Common.imp.getTitle();

            //prevent minimize and exit of image window
            Common.impwin.addWindowListener(getWindowAdapter());

            //set location
            Common.impwin.setLocation(impwinposx, impwinposy);

            //enlarge image to see better pixels
            if (width >= height) {
                Common.scimp = Common.zoomFactor / width; //adjustable: zoomFactor is by default 250 (see parameter definitions), a value chosen as it produces a good size on the screen
            } else {
                Common.scimp = Common.zoomFactor / height;
            }
            if (Common.scimp < 1.0) {
                Common.scimp = 1.0;
            }
            Common.scimp *= 100;// transfrom this into %tage to run ImageJ command
            IJ.run(Common.imp, "Original Scale", "");
            IJ.run(Common.imp, "Set... ", "zoom=" + Common.scimp + " x=" + (int) Math.floor(width / 2) + " y=" + (int) Math.floor(height / 2));
            IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
            // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary

            //setting default ROI selector for live display 
            if (Common.selectedMode == modeEnum.CALIBRATION || Common.selectedMode == modeEnum.ACQUISITION) {
                Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                Common.impRoiLive.setStrokeColor(Color.GREEN);
                Common.imp.setRoi(Common.impRoiLive);
                if (Common.isCCFmode) {
                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                    Common.impRoiLive2.setStrokeColor(Color.RED);
                    Overlay impov = new Overlay(Common.impRoiLive2);
                    Common.imp.setOverlay(impov);
                }
            }

            Common.impcan.addMouseListener(impcanLiveMouseUsed);
            imagewindowready = true;
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    try {
                        imagewindowready = false;
                        isSettingDynamicRange = true; //ensure to reset the dynamic range when window is closed
                        settingLiveImage();
                    } catch (Exception e) {
                    }

                }

                @Override
                public void windowIconified(WindowEvent we) {
                    try {
                        Frame liveframe = WindowManager.getFrame($impLiveVideo);
                        if (liveframe != null) {
                            WindowManager.toFront(liveframe);
                        }
                    } catch (Exception e) {
                        IJ.log("No window available.");
                    }
                }
            };
        }

        private int getBufferIndex(int frameAcquired) {
            // return index of first element of last available image
            if (frameAcquired == 0) {
                IJ.log("error25");
                return -1;
            }
            return (int) ((frameAcquired - Common.transferFrameInterval) % size_f) * size;
        }

        private void doDisplay_DEPRECATED(short[] tempPixelArr) {
            // assign last available image for display
            int count = Common.framecounter.getCounter();
            System.arraycopy(Common.bufferArray1D, getBufferIndex(count), tempPixelArr, 0, size);
            Common.ip.setPixels(tempPixelArr);

            // Saturation warning
            if (Common.ip.getMax() > Common.WarningCounts) {
                IJ.log("Warning (Saturation): reduce exposure time or laser power");
            }

            // decide if were to reset dynamic range for every single frame
            if (Common.getAutoAdjustImageDynamicRange()) {
                Common.ip.resetMinAndMax(); //reset dynamic range
            } else {
                if (isSettingDynamicRange) {
                    Common.ip.setMinAndMax(Common.ip.getMin(), Common.ip.getMax());
                    isSettingDynamicRange = false;
                }
            }

            Common.imp.updateAndDraw();
            if (!Common.impwin.isVisible()) {
                Common.impwin.setVisible(true);// Problematic: startle settings combobox when called sequentially at fast rate; threfore is the if statement
            }
        }

        private void doDisplayImage(short[] PixelArrForDisp) {

            // Setting pixel value
            Common.ip.setPixels(PixelArrForDisp);

            // Saturation warning
            if (Common.ip.getMax() > Common.WarningCounts) {
                IJ.log("Warning (Saturation): reduce exposure time or laser power");
            }

            // decide if were to reset dynamic range for every single frame
            if (Common.getAutoAdjustImageDynamicRange()) {
                Common.ip.resetMinAndMax(); //reset dynamic range
            } else {
                if (isSettingDynamicRange) {
                    Common.ip.setMinAndMax(Common.ip.getMin(), Common.ip.getMax());
                    isSettingDynamicRange = false;
                }
            }

            Common.imp.updateAndDraw();
            if (!Common.impwin.isVisible()) {
                Common.impwin.setVisible(true);// Problematic: startle settings combobox when called sequentially at fast rate; threfore is the if statement
            }
        }

        private void fillImageProcessorArray(short[] tempPixelArr, int count) {
            System.arraycopy(Common.bufferArray1D, getBufferIndex(count), tempPixelArr, 0, size);
        }

        private void performSumOperation(short[] tempPixelArr, short[] PixelArrForDisp) {
            for (int i = 0; i < tempPixelArr.length; i++) {
                PixelArrForDisp[i] = (short) (PixelArrForDisp[i] + tempPixelArr[i]);
            }
        }

        private void performDivisionOperation(short[] PixelArrForDisp, int div) {
            for (int i = 0; i < PixelArrForDisp.length; i++) {
                PixelArrForDisp[i] = (short) (PixelArrForDisp[i] / div);
            }
        }

        @Override
        protected Void doInBackground() throws Exception {

            Thread.currentThread().setName("LiveVideoWorkerV3");
            printlogthread("Staring thread: " + Thread.currentThread().getName());

            short[] PixelArrForDisplay = new short[size]; //reusing tempPixelArray to hold portion of Java1Dbuffer (cumulative depending on the type of binning)
            short[] tempArr = new short[size]; //hold at any one time temporary value of a pixel at certain frame

            settingLiveImage();

            while (!Common.isPrematureTermination) {
                if (Common.isPrematureTermination == true) {
                    break;
                }

                // Retrieve UI paremeters
                int temp_livevideo_displayFramesMode = Common.livevideo_displayFramesMode;
                int temp_livevideo_binningNo = Common.livevideo_binningNo;
                liveVideoBinModeEnum temp_selectedliveVideoBinMode = Common.selected_livevideo_binningMode;

                if (imagewindowready && Common.showLiveVideoCumul && (Common.framecounter.getCounter() > (Common.transferFrameInterval - 1))) {

//                    doDisplay_DEPRECATED(PixelArrForDisplay);
                    if (temp_selectedliveVideoBinMode.equals(liveVideoBinModeEnum.NO_BINNING)) {

                        int currcount = Common.framecounter.getCounter();
//                        IJ.log("no binning currcount: " + currcount);
                        boolean proceed = false;

                        // add a check to plot curcount - 1 if frame is valid
                        switch (temp_livevideo_displayFramesMode) {
                            case 1:
                                // odd frames
                                if (currcount % 2 == 1) {
                                    proceed = true;
                                } else {
                                    if (currcount - 1 > 0) {
                                        currcount = currcount - 1;
                                        proceed = true;
                                    }
                                }
                                break;
                            case 2:
                                // even frames
                                if (currcount % 2 == 0) {
                                    proceed = true;
                                } else {
                                    if (currcount - 1 > 0) {
                                        currcount = currcount - 1;
                                        proceed = true;
                                    }
                                }
                                break;
                            default:
                                // all frames
                                proceed = true;
                        }

                        if (proceed) {
//                            IJ.log("proceed currcount: " + currcount);
                            fillImageProcessorArray(PixelArrForDisplay, currcount); //retrieve array counts from circular buffer of size w * h (one frame worth of intensity)
                            doDisplayImage(PixelArrForDisplay);

                        }

                    } else {

                        //reset PixelArrForDisplay
                        for (int i = 0; i < PixelArrForDisplay.length; i++) {
                            PixelArrForDisplay[i] = 0;
                        }

                        int accumulatedFrame = 0;
                        int memcount2 = 0;

                        while (accumulatedFrame != temp_livevideo_binningNo) {

                            if (Common.isPrematureTermination == true) {
                                break;
                            }

                            int currcount = Common.framecounter.getCounter();
                            boolean proceed = false;

                            if (memcount2 != 0) {
                                //check if valid new frame available
                                if (currcount >= memcount2) {
                                    proceed = true;
                                }

                            } else {
                                memcount2 = currcount;
                                proceed = true;
                            }

                            boolean proceed2 = false;
                            if (proceed) {
                                switch (temp_livevideo_displayFramesMode) {
                                    case 1:
                                        // odd frames
                                        if (memcount2 % 2 == 1) {
                                            proceed2 = true;
                                        }
                                        break;
                                    case 2:
                                        // even frames
                                        if (memcount2 % 2 == 0) {
                                            proceed2 = true;
                                        }
                                        break;
                                    default:
                                        // all frames
                                        proceed2 = true;
                                }
                            }

                            if (proceed2) {
                                fillImageProcessorArray(tempArr, memcount2); //retrieve array counts from circular buffer of size w * h (one frame worth of intensity)
                                performSumOperation(tempArr, PixelArrForDisplay); //sum two array
                                accumulatedFrame++;
                                switch (temp_livevideo_displayFramesMode) {
                                    case 0:
                                        // all frames
                                        memcount2++;
                                        break;

                                    default:
                                        // odd or even frames
                                        memcount2 = memcount2 + 2;
                                }
                            }

                            if (accumulatedFrame == 0) {
                                memcount2 = 0;
                            }

                        }

                        if (temp_selectedliveVideoBinMode.equals(liveVideoBinModeEnum.AVERAGE_BINNING)) {
                            performDivisionOperation(PixelArrForDisplay, accumulatedFrame);
                        }

                        doDisplayImage(PixelArrForDisplay);
                    }

                } else {
                    try {
                        Thread.sleep(10); //100
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return null;
        }

        @Override
        protected void done() {
            if (Common.imp != null) {
                Common.impwin.close();
                Common.imp.close();
                Common.imp = null;
                Common.impwin = null;
            }

            latch.countDown();
        }

    }

    public static class SynchronizerWorker extends SwingWorker<Void, Void> {
        // This Worker is to prevent multiple call to counter in case program gets more complicated in future
        // Alternative solution is to have multiple private volatile int counter

        CountDownLatch latch;
        int sleepTime; //Live video
        long timelastDisplay; //Live video
        long timelastDisplay2; //GUI clock
        int counter; //native frame counter
        final int sleepTimeGUI = 40;//ms //GUI clock

        private Object L1 = new Object();

        public int getCounter() {
            synchronized (L1) {
                return counter;
            }
        }

        private boolean checkLiveVideo() { //check if needed to plot display

            if (!(Common.showLiveVideoCumul && (counter != 0))) {
                return false;
            }

            if (System.currentTimeMillis() > (long) timelastDisplay + sleepTime) {
                timelastDisplay = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }

        }

        private boolean checkGUIclock() {
            if (System.currentTimeMillis() > (long) timelastDisplay2 + sleepTimeGUI) {
                timelastDisplay2 = System.currentTimeMillis();
                Common.tempGUIcounter = counter;
                return true;
            } else {
                return false;
            }
        }

        private int optimalLiveVideoSleepTime(int fps, double exposuretime, int frametransferinterval) {
            //milisseconds
            double invbin = 1 / (exposuretime * frametransferinterval);
            if (invbin < fps) {
                return (int) Math.ceil(1000 / invbin);
            } else {
                return (int) (1000 / fps);
            }
        }

        public SynchronizerWorker(CountDownLatch latch) {
            counter = 0;
            this.latch = latch;
            sleepTime = optimalLiveVideoSleepTime(Common.fps, Common.kineticCycleTime, Common.transferFrameInterval);
        }

        @Override
        protected Void doInBackground() throws Exception {
            Thread.currentThread().setName("SynchronizerWorker");
            printlogthread("Staring thread: " + Thread.currentThread().getName());

            timelastDisplay = System.currentTimeMillis();
            timelastDisplay2 = System.currentTimeMillis();

            // This is preferred instead of regular checking for coutner != 0
            while (Common.framecounter.getCounter() == 0) {
                Thread.sleep(100);
            }

            while (!Common.isPrematureTermination) {
                if (Common.isPrematureTermination == true) {
                    break;
                }

                counter = Common.framecounter.getCounter(); //get frame counter incremented by native C++

                //Live video
                if (Common.showLiveVideoCumul && !Common.cIsDisplayLatestFrame) {
                    synchronized (Common.locker1) {
                        if (checkLiveVideo()) { //else statement valid 
                            Common.locker1.notify();
                        }
                    }
                }
                //GUI clock
                if (!Common.cIsDisplayGUIcounter) {
                    synchronized (Common.locker2) {
                        if (checkGUIclock()) {//else statement valid
                            Common.locker2.notify();
                        }
                    }
                }

            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return null;
        }

        @Override
        protected void done() {
            if (Common.locker1 != null) {
                synchronized (Common.locker1) {
                    Common.locker1.notify();
                }
            }
            if (Common.locker2 != null) {
                synchronized (Common.locker2) {
                    Common.locker2.notify();
                }
            }
            latch.countDown();
        }

    }

    public static class NonCumulativeACFWorkerV3 extends SwingWorker<Boolean, Void> {

        private final int oWidth; //original Width dimesntion available to java //stored in WidthDisp variable
        private final int oHeight; // original Height dimension available to Java // stored in HeighDisp  variable
        private final CountDownLatch latch;
        private final int size;
        private final int size_f;

        private int lWidth;
        private int lHeight;
        private int lLeft;
        private int lTop;

        private boolean isCCFmode;
        private int CCFdistX;
        private int CCFdistY;
        private int lLeftRed;//coordiante of red channel 
        private int lTopRed;//coordinate of red channel

        private int inct1;
        private int inct2R;
        private int inct2G;
        private int idxResetR;
        private int idxResetG;

        private int framestart; // eg. 1
        public volatile int frameend; //  eg. 5001 for 5000 plot interval
        private int tempPlotInterval;

        boolean proceed;

        public NonCumulativeACFWorkerV3(int width, int height, CountDownLatch latch, int arraysize) {
            this.oWidth = width;
            this.oHeight = height;
            this.latch = latch;
            this.size = width * height;
            this.size_f = (arraysize - (arraysize % size)) / size;
            Common.fromImFCSobj1 = new ImFCSCorrelator("Non-cumulative");
        }

        private void holdUserInputToTemp() {
            // Needed for ImFCSCorrelator class might process with different parameters depending if user make any changes via GUI
            lWidth = Common.lWidth;
            lHeight = Common.lHeight;
            lLeft = Common.lLeft;
            lTop = Common.lTop;
            isCCFmode = Common.isCCFmode;
            CCFdistX = Common.CCFdistX;
            CCFdistY = Common.CCFdistY;

            if (isCCFmode) {
                lLeftRed = lLeft + Common.CCFdistX;
                lTopRed = lTop + Common.CCFdistY;
            }

            inct1 = oWidth - lWidth + 1;
            inct2G = (oWidth * (oHeight - (lTop - 1) - lHeight) + (oWidth - (lLeft - 1) - lWidth) + oWidth * (lTop - 1) + (lLeft - 1) + 1);
            inct2G -= oWidth - lWidth + 1;
            if (isCCFmode) {
                inct2R = (oWidth * (oHeight - (lTopRed - 1) - lHeight) + (oWidth - (lLeftRed - 1) - lWidth) + oWidth * (lTopRed - 1) + (lLeftRed - 1) + 1);
                inct2R -= oWidth - lWidth + 1;
            }
            idxResetG = (lTop - 1) * oWidth + (lLeft - 1);
            idxResetR = (lTopRed - 1) * oWidth + (lLeftRed - 1);

            tempPlotInterval = Common.plotInterval;
            framestart = Common.framecounter.getCounter();
            frameend = framestart + tempPlotInterval; // framestart + tempPlotInterval - 1
        }

        private void InitStack() {
            //Green and Red of same size
            Common.ims_nonCumGreen = new ImageStack(lWidth, lHeight);
            if (isCCFmode) {
                Common.ims_nonCumRed = new ImageStack(lWidth, lHeight);
            }
        }

        private boolean needToReset() {
            return getBufferIndex(framestart) / size + tempPlotInterval > size_f; //need reset
        }

        private int getBufferIndex(int frameAcquired) {

            // return index of first element of first image to be transferred to imagestack
            if (frameAcquired == 0) {
                assert false : " invalid frameAcquired";
                return -1;
            }
            return (int) ((frameAcquired - Common.transferFrameInterval) % size_f) * size;
        }

        private void fillImageStack(boolean needReset, ImageStack ims, String channel) {
//            long timestart = System.nanoTime();

            int idx = getIndexGR(channel);
            int inct2;
            int idxReset;
            if (channel.equals("green")) {
                inct2 = inct2G;
                idxReset = idxResetG;
            } else {
                //red
                inct2 = inct2R;
                idxReset = idxResetR;

            }

            if (needReset) {
                int zReset = tempPlotInterval - (((framestart - Common.transferFrameInterval) % size_f) + tempPlotInterval - size_f) - 1;
                for (int z = 0; z < tempPlotInterval; z++) {
                    ShortProcessor ip = new ShortProcessor(lWidth, lHeight);
                    for (int y = 0; y < lHeight; y++) {
                        for (int x = 0; x < lWidth; x++) {
                            ip.set(x, y, Common.bufferArray1D[idx]);
                            if (x == (lWidth - 1)) {
                                idx += inct1;
                            } else {
                                idx++;
                            }
                        }
                        if (y == (lHeight - 1)) {
                            idx += inct2;
                        }
                    }
                    ims.addSlice(ip);
                    if (z == zReset) {
                        idx = idxReset;
                    }
                }
            } else {
                for (int z = 0; z < tempPlotInterval; z++) {
                    ShortProcessor ip = new ShortProcessor(lWidth, lHeight);
                    for (int y = 0; y < lHeight; y++) {
                        for (int x = 0; x < lWidth; x++) {
                            ip.set(x, y, Common.bufferArray1D[idx]);
                            if (x == (lWidth - 1)) {
                                idx += inct1;
                            } else {
                                idx++;
                            }
                        }
                        if (y == (lHeight - 1)) {
                            idx += inct2;
                        }
                    }
                    ims.addSlice(ip);
                }
            }
//            IJ.log("PI: " + tempPlotInterval + ", lW: " + lWidth + ", lH: " + lHeight + " calibration stack time: " + (System.nanoTime() - timestart) / 1000 + " us");
        }

        private int getIndexGR(String channel) {
            int idx;
            if (channel.equals("green")) {
                idx = getBufferIndex(framestart) + (lTop - 1) * oWidth + (lLeft - 1); //index start of 0
            } else {
                //red
                idx = getBufferIndex(framestart) + (lTopRed - 1) * oWidth + (lLeftRed - 1); //index start of 0
            }

            return idx;
        }

        private ImagePlus getPlus(String mode) {
            if (mode.equals("green")) {
                return new ImagePlus("Calibration Green", Common.ims_nonCumGreen);
            }
            if (mode.equals("red")) {
                return new ImagePlus("Calibration Red", Common.ims_nonCumRed);
            }
            return null;
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            Thread.currentThread().setName("NonCumulativeACFWorkerV3");
            printlogthread("Starting thread: " + Thread.currentThread().getName());
            while (Common.framecounter.getCounter() == 0) {
                Thread.sleep(100);
            }

            while (!Common.isPrematureTermination) {
                if (Common.isPrematureTermination == true) {
                    break;
                }

                proceed = Common.analysisMode.equals($amode[1]); //non-cumulative

                if (proceed) {
                    holdUserInputToTemp();

                    InitStack();    //instantiate ImageStack object(s)

                    //wait untill enough frames is available to buffer
                    while (Common.framecounter.getCounter() < frameend) {
                        if (Common.isPrematureTermination == true) {
                            break;
                        }
                        Thread.sleep(100); // not relly important. can't noticeably differentiate 100ms delay
                    }

                    /*
                    START
                    Fill ImageStack array to be passed for calculation and display
                     */
                    //Fill Green channel  and Red Channel if CCF mode is true
                    fillImageStack(needToReset(), Common.ims_nonCumGreen, "green");
                    if (isCCFmode) {
                        fillImageStack(needToReset(), Common.ims_nonCumRed, "red");
                    }

                    //Set Imp green to imfcs obj //TODO use Ims instead
                    Common.fromImFCSobj1.settingImp(getPlus("green"));
                    if (isCCFmode) {//Set Imp red to imfcs obj if CCFmode is true
                        Common.fromImFCSobj1.settingImp2(getPlus("red"));
                    }

                    if (Common.isPrematureTermination == true) {
                        break;
                    }

                    if (Common.fromImFCSobj1.settingPlotOption(Common.plotACFCurves, Common.plotTrace, Common.plotAverage, Common.plotJustCCF, CCFdistX, CCFdistY, Common.plotCalibAmplitude, Common.plotCalibDiffusion, Common.isResetCalibPlot, Common.noptsavr, Common.background, Common.plotCalibIntensity, Common.isCalibFixScale, $amode[1], 1)) {
                        Common.fromImFCSobj1.settingExpParameters(Common.pixelSize * Common.inCameraBinning, Common.objMag, Common.NA, Common.emlambda, Common.sigmaxy);
                        Common.isResetCalibPlot = false;
                        Common.fromImFCSobj1.runPlotACF();
                    }

                    /*
                    END
                    Fill ImageStack array to be passed for calculation and display
                     */
                }

            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return true;
        }

        @Override
        protected void done() {
            latch.countDown();
        }

    }

    public static class BufferToStackWorker extends SwingWorker<Boolean, Void> {

        /*
        Fill StackStorage with Counts from circular buffer
         */
        private final int width;
        private final int height;
        private CountDownLatch latch;
        private final int totalframe;
        private final int size;
        private final int size_f;

        public BufferToStackWorker(int w, int h, int f, CountDownLatch latch, int arraysize) {
            this.width = w;
            this.height = h;
            this.latch = latch;
            this.totalframe = f;
            size = width * height;
            this.size_f = (arraysize - (arraysize % size)) / size; // per frame unit

            InitStack();
        }

        private void InitStack() {
            Common.ims_cum = new ImageStack(width, height);
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            Thread.currentThread().setName("BufferToStackWorker");
            printlogthread("Staring thread: " + Thread.currentThread().getName());

            for (int i = 1; i <= totalframe; i++) {

                while (Common.framecounter.getCounter() < (i * Common.transferFrameInterval)) {
                    if (Common.isPrematureTermination == true) {
                        break;
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (Common.isPrematureTermination == true) {
                    break;
                }

                //copy 1D circular buffer to ims
                ShortProcessor ip = new ShortProcessor(width, height);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        ip.set(x, y, Common.bufferArray1D[((i - 1) % size_f) * size + (y * width) + x]);
                    }
                }
                Common.ims_cum.addSlice(ip);

                Common.framecounterIMSX.incrementby(1);
            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return true;
        }

        @Override
        protected void done() {
            latch.countDown();
        }

    }

    public static class CumulativeACFWorkerV3 extends SwingWorker<Boolean, Void> {

        // perform ACF calculation from frame = 1.
        // Stack storage
        private CountDownLatch latch;
        private static int previousFC;
        boolean proceed;

        public CumulativeACFWorkerV3(CountDownLatch latch) {
            this.latch = latch;
            resetPreviousFC();
        }

        private static void resetPreviousFC() {
            previousFC = 0;
        }

        public static boolean isImageReady(int frameInterval, int plotInterval, int frameCounterStack) {

            if (frameCounterStack == 0) {
                return false;
            }
            if (frameCounterStack < 100) {
                return false;
            }
            double test = (double) frameCounterStack / (double) plotInterval;
            if ((test % 1) == 0) {
                if (previousFC == frameCounterStack) {
                    return false;
                } else {
                    previousFC = frameCounterStack;
                    return true;
                }
            }
            return false;
//            //alternative, step-by-step 
//            double divisor = (double) plotInterval / (double) frameInterval;
//            double runner = (double) frameCounterStack / (double) frameInterval;
//            return ((runner / divisor) % 1 == 0);
        }

        @Override
        protected Boolean doInBackground() throws Exception {

            Thread.currentThread().setName("CumulativeACFWorkerV3");
            printlogthread("Staring thread: " + Thread.currentThread().getName());

            Common.fromImFCSobj2 = new ImFCSCorrelator("Cumulative");

            while (Common.framecounterIMSX.getCount() < Common.totalFrame) {
                if (Common.isPrematureTermination == true) {
                    break;
                }
                // sleep necessary
                Thread.sleep(10);//100

                proceed = Common.analysisMode.equals($amode[2]); //cumulative

                if (proceed) {
                    //setting plotinterval argument to 1 will plot as soon as calculation is done
                    if (!isImageReady(Common.transferFrameInterval, Common.cumulativePlotInterval, Common.ims_cum.getSize())) {
                        continue;
                    }

                    Common.imp_cum = new ImagePlus("imp acquisition", Common.ims_cum);

                    Common.fromImFCSobj2.settingImp(Common.imp_cum);
                    if (Common.fromImFCSobj2.settingPlotOption(Common.plotACFCurves, Common.plotTrace, Common.plotAverage, Common.plotJustCCF, Common.CCFdistX, Common.CCFdistY, Common.plotCalibAmplitude, Common.plotCalibDiffusion, Common.isResetCalibPlot, Common.noptsavr, Common.background, Common.plotCalibIntensity, Common.isCalibFixScale, $amode[2], Common.fitStartCumulative)) {
                        Common.fromImFCSobj2.runPlotACF();//does not wrap SwingWorker on CorrelateROI
                    }
                }

            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return true;
        }

        @Override
        protected void done() {
            latch.countDown();
        }

    }

    public static class CppTOJavaTransferWorkerV2 extends SwingWorker<Boolean, Void> {

        protected short[] array;
        protected CountDownLatch latch;

        public CppTOJavaTransferWorkerV2(short[] array, CountDownLatch latch) {
            this.array = array;
            this.latch = latch;
        }

        protected void runInfinteLoop() {
            IJ.showMessage("error 56, class CppTOJavaTransferWorker runInfiniteLoop method not overrrided");
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            Thread.currentThread().setName("CppTOJavaTransferWorkerV2");
            printlogthread("Starting thread: " + Thread.currentThread().getName());
            runInfinteLoop();
            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return true;
        }

        @Override
        protected void done() {
            try {
                Common.isPrematureTermination = get(); //TODO: if there is a delay in BufferToStack worker (which there is //Solution is to use replace ImageStack storage with eitehr 2D primitive array)
            } catch (InterruptedException ex) {
                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(Workers.class.getName()).log(Level.SEVERE, null, ex);
            }
            latch.countDown();
        }
    }

    public static class ICCSWorker extends SwingWorker<Boolean, Void> {

        private final int oWidth; //original Width dimesntion available to java //stored in WidthDisp variable
        private final int oHeight; // original Height dimension available to Java // stored in HeighDisp  variable
        private final CountDownLatch latch;
        private final int size;
        private final int size_f;

        private ShortProcessor ip_ICCS;
        private ImagePlus imp_ICCS;
        private int frameIdx; //1 -- first frame //frame index to correlate

        // parameter to correlate (temporary holder)
        private Rectangle rect1;
        private int CCFdistX;
        private int CCFdistY;
        private int pXShift;
        private int pYShift;

        long timestart;
        final int refreshInterval = 40; // 40 ms -- 1/40 = 25 frame per seconds

        public ICCSWorker(int width, int height, CountDownLatch latch, int arraysize) {

            this.oWidth = width;
            this.oHeight = height;
            this.latch = latch;
            this.size = width * height;
            this.size_f = (arraysize - (arraysize % size)) / size;

            iccsObj1 = new ICCS(oWidth, oHeight);

            InitStack();

        }

        public void setNullICCS() {
            if (iccsObj1 != null) {
                iccsObj1 = null;
            }

        }

        private void InitStack() {
            ip_ICCS = new ShortProcessor(oWidth, oHeight);
            imp_ICCS = new ImagePlus("ICCS", ip_ICCS);
        }

        private boolean isReady() {

            if ((System.currentTimeMillis() - timestart) > refreshInterval) {
                frameIdx = Common.framecounter.getCounter();
                return true;
            }
            return false;

        }

        private int getBufferIndex(int frameAcquired) {
            // return index of first element of last available image
            if (frameAcquired == 0) {
                IJ.log("error25");
                return -1;
            }
            return (int) ((frameAcquired - Common.transferFrameInterval) % size_f) * size;
        }

        private void fillImageStack() {
            for (int y = 0; y < oHeight; y++) {
                for (int x = 0; x < oWidth; x++) {
                    ip_ICCS.set(x, y, Common.bufferArray1D[((frameIdx - 1) % size_f) * size + (y * oWidth) + x]);
                }
            }

//            short[] PixelArrForDisplay = new short[size]; //reusing tempPixelArray to hold portion of Java1Dbuffer
//            System.arraycopy(Common.bufferArray1D, getBufferIndex(frameIdx), PixelArrForDisplay, 0, size); //
//            ip_ICCS.setPixels(PixelArrForDisplay);
        }

        private void holdUserInputToTemp() {
            // Needed for ICCS class might process with different parameters depending if user make any changes via GUI
            rect1 = new Rectangle(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
            CCFdistX = Common.CCFdistX;
            CCFdistY = Common.CCFdistY;
            pXShift = Common.ICCSShiftX;
            pYShift = Common.ICCSShiftY;
//            IJ.log("holdUserInputToTemp() pXShift: " + pXShift + ", pYShift: " + pYShift + ", CCFX: " + CCFdistX + ", CCFY: " + CCFdistY);
//            IJ.log("holdUserInputToTemp() green ROI lLeft: " + (rect1.getX() + 1) + ", lTop: " + (rect1.getY() + 1) + ", lWidth: " + (rect1.getWidth()) + ", lHeight: " + (rect1.getHeight()));
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            Thread.currentThread().setName("ICCSWorker");
            printlogthread("Starting thread: " + Thread.currentThread().getName());

            while (Common.framecounter.getCounter() == 0) {
                Thread.sleep(100);
            }

            while (!Common.isPrematureTermination) {
                if (Common.isPrematureTermination == true) {
                    break;
                }

                if (!Common.isICCSValid) {
                    IJ.log("***Make selection with Rectangular Selection Tool***");
                }

                timestart = System.currentTimeMillis();

                //Takes user parameter
                holdUserInputToTemp();

                //wait for reasonable refresh rate
                while (!isReady()) {
                    if (Common.isPrematureTermination == true) {
                        break;
                    }
                    Thread.sleep(100); // not relly important. can't noticeably differentiate 100ms delay
                }

                /*
                    START
                    Fill ImageStack array to be passed for calculation and display
                 */
                frameIdx = Common.framecounter.getCounter();
                fillImageStack();

                /*
                // uncomment to show image
                imp_ICCS.show();
                if (imp_ICCS != null) {
                    imp_ICCS.updateAndDraw();
                }
                 */
                if (Common.isPrematureTermination == true) {
                    break;
                }

                if (true) { //if ICCS plot option true
                    if (iccsObj1.SetParam(imp_ICCS, rect1, CCFdistX, CCFdistY, pXShift, pYShift)) {
                        iccsObj1.runICCS();
                    }
                }

                /*
                    END
                    Fill ImageStack array to be passed for calculation and display
                 */
            }

            printlogthread("Ending thread: " + Thread.currentThread().getName());
            return true;
        }

        @Override
        protected void done() {
            latch.countDown();
        }

    }

}
