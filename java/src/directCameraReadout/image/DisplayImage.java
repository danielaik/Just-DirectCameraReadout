package directCameraReadout.image;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import directCameraReadout.gui.DirectCapturePanel;
import static directCameraReadout.gui.DirectCapturePanel.DisplayImageObj;
import static directCameraReadout.gui.DirectCapturePanel.JDimensionpanelComponentPanel;
import static directCameraReadout.gui.DirectCapturePanel.getRoiCoordinateFromCenter;
import static directCameraReadout.gui.DirectCapturePanel.tfPixelDimension;
import static directCameraReadout.gui.DirectCapturePanel.tfPlotInterval;
import static directCameraReadout.util.Utilities.retMaxAllowablePlotInterval;
import static directCameraReadout.util.Utilities.setSizeAandSizeB;
import static directCameraReadout.updater.Updater.*;

public class DisplayImage {

    final int impwinposx = 1110;
    final int impwinposy = 125;
    boolean isReadConfig;
    ImagePlus imp;
    ShortProcessor ip;
    public ImageWindow impwin;
    ImageCanvas impcan;
    int posRoiX;
    int posRoiY;
    double scimp;
    final int multiplierFactor = 30000;//49152;
    boolean isHamamatsu;

    public ArrayList<ArrayList<int[]>> aListArrayBuffer = new ArrayList<>(); //o index, 1 value
//        private ArrayList<ArrayList<int[]>> aListCroppedModeArrayBuffer = new ArrayList<>(); //o index, 1 value

    public DisplayImage(boolean isHamamatsu) {

        isReadConfig = false;
        this.isHamamatsu = isHamamatsu;

        if (isReadConfig) {
//            IJ.log("able to read config file, proceed to fill from config");
            //TODO if can read from config file
            //fill arraybin1,2,3,4

        } else {
//            IJ.log("Unable to read config file, proceed to fill zero");
            //fullframe
            ArrayList<int[]> arFindex = new ArrayList<>();
            ArrayList<int[]> ar0array = new ArrayList<>();
            int[] tempindex;
            int[] temparray;
            int[] lis = {1, 2, 3, 4, 8};

            for (int i : lis) {
                tempindex = new int[1];
                tempindex[0] = i;
                temparray = new int[DirectCapturePanel.Common.chipSizeX * DirectCapturePanel.Common.chipSizeY]; //TODO remove this
                temparray = fillzero(i, DirectCapturePanel.Common.chipSizeX, DirectCapturePanel.Common.chipSizeY);
                arFindex.add(tempindex);
                ar0array.add(temparray);
            }
            aListArrayBuffer.add(arFindex);
            aListArrayBuffer.add(ar0array);

        }
    }

    private int[] fillzero(int bin, int maxdimensionX, int maxdimensionY) {
        int newdimX = (int) Math.floor(maxdimensionX / bin);
        int newdimY = (int) Math.floor(maxdimensionY / bin);
        int totalpix = newdimX * newdimY;
        int[] res = new int[totalpix];
        for (int i = 0; i < totalpix; i++) {
            res[i] = 0;
        }
        return res;
    }

    public void toggleDisplay(boolean isOn) {
        if (isOn) {
            boolean isBinChanged = false;
            int sizeX = (int) Math.floor(DirectCapturePanel.Common.MAXpixelwidth / DirectCapturePanel.Common.inCameraBinning);
            int sizeY = (int) Math.floor(DirectCapturePanel.Common.MAXpixelheight / DirectCapturePanel.Common.inCameraBinning);

            if (ip != null) {
                isBinChanged = (DirectCapturePanel.Common.MAXpixelwidth / ip.getWidth()) != DirectCapturePanel.Common.inCameraBinning;
            }

            if (!isBinChanged && ip != null) {
            } else {
                createImp(sizeX, sizeY);
            }

            DisplayImageObj.performROIselection(DirectCapturePanel.Common.oLeft - 1, DirectCapturePanel.Common.oTop - 1, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight);
            DisplayImageObj.impwin.setVisible(true);

        } else {
            DisplayImageObj.impwin.setVisible(false);
        }

    }

    private void createImp(int sizeX, int sizeY) {
        ip = new ShortProcessor(sizeX, sizeY);
        fillIpfromBuffer(sizeX, sizeY, DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.Common.isCropMode);
        imp = new ImagePlus("ROI selection", ip);
        if (impwin != null) {
            impwin.close();
        }
        imp.show();
        impwin = imp.getWindow();
        impcan = imp.getCanvas();

        //set location
        impwin.setLocation(impwinposx, impwinposy);

        //To achieve same frame dimension given size of pixels follows power regression y = a*x^b
        scimp = multiplierFactor * Math.pow(sizeX, -1);

        IJ.run(imp, "Original Scale", "");
        IJ.run(imp, "Set... ", "zoom=" + scimp + " x=" + (int) Math.floor(sizeX / 2) + " y=" + (int) Math.floor(sizeX / 2));
        IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
        // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary

        impcan.setFocusable(true);
        impcan.addMouseListener(impcanDisplayImageMouseUsed);
    }

    public int getIndexOfBin(ArrayList<ArrayList<int[]>> aList, int bin) {
        ArrayList<int[]> aList1 = aList.get(0);
        int index = 0;
        for (int[] l : aList1) {
            if (l[0] == bin) {
                break;
            }
            index++;
        }
        return index;
    }

    private void fillIpfromBuffer(int sizeX, int sizeY, int bin, int isCropMode) {
        int nsizeX = sizeX / bin;
        int nsizeY = sizeY / bin;

        int index = getIndexOfBin(aListArrayBuffer, bin);
        for (int y = 0; y < nsizeY; y++) {
            for (int x = 0; x < nsizeX; x++) {
                ip.putPixelValue(x, y, aListArrayBuffer.get(1).get(index)[(y * nsizeX) + x]);
            }
        }

        ip.resetMinAndMax();
    }

    private void fillIpfromBuffer(int sizeX, int sizeY, int bin, int[] array, int isCropMode) {
        int nsizeX = sizeX / bin;
        int nsizeY = sizeY / bin;

        int index = getIndexOfBin(aListArrayBuffer, bin);
        aListArrayBuffer.get(1).set(index, array);
        for (int y = 0; y < nsizeY; y++) {
            for (int x = 0; x < nsizeX; x++) {
                ip.putPixelValue(x, y, aListArrayBuffer.get(1).get(index)[(y * nsizeX) + x]);
            }
        }
        ip.resetMinAndMax();
    }

    public void updateImage(int[] array, int bin, int sizeX, int sizeY, int isCropMode) {//to beupdated when fullframe iamge captured or change o physical bin //int[] newarray, int bin

        if (ip != null) {
            boolean isBinChanged = (DirectCapturePanel.Common.MAXpixelwidth / ip.getWidth()) != bin;
            int updatedsizeX = sizeX / bin;
            int updatedsizeY = sizeY / bin;
            if (isBinChanged) {
                createImp(updatedsizeX, updatedsizeY);
            }
        }

        fillIpfromBuffer(sizeX, sizeY, bin, array, isCropMode);
        imp.updateAndDraw();

    }

    public void performROIselection(int px, int py, int width, int height) {

        // set theb ROI in the image
        if (imp.getOverlay() != null) {
            imp.getOverlay().clear();
            imp.setOverlay(imp.getOverlay());
        }

        Roi roi1 = new Roi(px, py, width, height);
        roi1.setStrokeColor(Color.GREEN);
        imp.setRoi(roi1);

    }

    private boolean checkLeftOrTopValid(int left, int bin) {
        //Only for hamamatsu ROI requirement
        // applied to top
        int scaledleft = (left * bin) - (bin - 1);
        return (scaledleft - 1) % 4 == 0;
    }

    private boolean checkWidthOrHeightValid(int left, int right, int bin) {
        //Only for hamamatsu ROI requirement
        // applied to top;bottom
        return ((right - left + 1) * bin) % 4 == 0;
    }

    MouseListener impcanDisplayImageMouseUsed = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {// update acquisition AOIs namely left and top 

            if (DirectCapturePanel.Common.isCropMode == 0) {
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = false;
                int px = e.getX();
                int py = e.getY();
                int initposx, initposy;
                int pixbinX = 1; // currently only support in camera bin=1;
                int pixbinY = 1;
                int[] AOIsparam;

                // add expression for expansion
                if (true) {
                    // geting the mouse coordinates
                    initposx = (int) Math.floor(impcan.offScreenX(px) / pixbinX);
                    initposy = (int) Math.floor(impcan.offScreenY(py) / pixbinY);
                    posRoiX = initposx + 1;
                    posRoiY = initposy + 1;

                    if (isHamamatsu) {
                        boolean isLeftValid = false, isTopValid = false;
                        // Width and Height should be valid at this stage

                        int i, j;

                        // check whether Left is valid
                        for (i = posRoiX; i >= 1; i--) {
                            AOIsparam = getRoiCoordinateFromCenter(i, posRoiY, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, imp.getWidth(), imp.getHeight()); //TODO add real width and height for acquisition
                            int templeft = AOIsparam[0];
                            isLeftValid = checkLeftOrTopValid(templeft, DirectCapturePanel.Common.inCameraBinning);

                            if (isLeftValid) {
                                break;
                            }
                        }

                        // check whether Top is valid
                        for (j = posRoiY; j >= 1; j--) {
                            AOIsparam = getRoiCoordinateFromCenter(i, j, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, imp.getWidth(), imp.getHeight()); //TODO add real width and height for acquisition
                            int temptop = AOIsparam[2];
                            isTopValid = checkLeftOrTopValid(temptop, DirectCapturePanel.Common.inCameraBinning);

                            if (isTopValid) {
                                break;
                            }
                        }

                        AOIsparam = getRoiCoordinateFromCenter(i, j, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, imp.getWidth(), imp.getHeight());

                    } else {
                        AOIsparam = getRoiCoordinateFromCenter(posRoiX, posRoiY, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, imp.getWidth(), imp.getHeight());
                    }

                    DirectCapturePanel.Common.oLeft = AOIsparam[0];
                    DirectCapturePanel.Common.oTop = AOIsparam[2];
                    DirectCapturePanel.Common.oRight = DirectCapturePanel.Common.oLeft + DirectCapturePanel.Common.oWidth - 1;
                    DirectCapturePanel.Common.oBottom = DirectCapturePanel.Common.oTop + DirectCapturePanel.Common.oHeight - 1;
                    performROIselection(DirectCapturePanel.Common.oLeft - 1, DirectCapturePanel.Common.oTop - 1, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight);
                }
                setSizeAandSizeB(DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.maxE, DirectCapturePanel.Common.minPI, DirectCapturePanel.Common.maxPI);
                if (DirectCapturePanel.Common.plotInterval > retMaxAllowablePlotInterval(DirectCapturePanel.Common.size_a, DirectCapturePanel.Common.size_b)) {
                    DirectCapturePanel.Common.plotInterval = retMaxAllowablePlotInterval(DirectCapturePanel.Common.size_a, DirectCapturePanel.Common.size_b);
                    tfPlotInterval.setText(Integer.toString(DirectCapturePanel.Common.plotInterval));
                }
                UpdateDimTextField();
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = true;
            }

        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent arg0) {

            if (DirectCapturePanel.Common.isCropMode == 0) {
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = false;
                Roi roi2 = imp.getRoi();

                if (roi2 != null) {

                    Rectangle rect = roi2.getBounds();

                    if (isHamamatsu) {

                        boolean isLeftValid = false, isTopValid = false, isWidthValid = false, isHeightValid = false;

                        int templeft = (int) rect.getX() + 1;
                        int temptop = (int) rect.getY() + 1;
                        int tempwidth = (int) rect.getWidth();
                        int tempheight = (int) rect.getHeight();
                        if (tempheight < DirectCapturePanel.Common.minHeight) {
                            tempheight = DirectCapturePanel.Common.minHeight;
                        }
                        if (tempwidth < DirectCapturePanel.Common.minHeight) {
                            tempwidth = DirectCapturePanel.Common.minHeight;
                        }

                        int i, w, j, h;

                        //check if Left is valid
                        for (i = templeft; i >= 1; i--) {
                            isLeftValid = checkLeftOrTopValid(i, DirectCapturePanel.Common.inCameraBinning);
                            if (isLeftValid) {
                                break;
                            }
                        }

                        //check if Width is valid
                        for (w = tempwidth; w >= DirectCapturePanel.Common.minHeight; w--) {
                            isWidthValid = checkWidthOrHeightValid(i, (i + w - 1), DirectCapturePanel.Common.inCameraBinning);
                            if (isWidthValid) {
                                break;
                            }
                        }

                        //check if Top is valid
                        for (j = temptop; j >= 1; j--) {
                            isTopValid = checkLeftOrTopValid(j, DirectCapturePanel.Common.inCameraBinning);
                            if (isTopValid) {
                                break;
                            }
                        }
                        //check if Height is valid
                        for (h = tempheight; h >= DirectCapturePanel.Common.minHeight; h--) {
                            isHeightValid = checkWidthOrHeightValid(j, (j + h - 1), DirectCapturePanel.Common.inCameraBinning);
                            if (isHeightValid) {
                                break;
                            }
                        }

                        DirectCapturePanel.Common.oLeft = i;
                        DirectCapturePanel.Common.oTop = j;
                        DirectCapturePanel.Common.oWidth = w;
                        DirectCapturePanel.Common.oHeight = h;
                        if (DirectCapturePanel.Common.oHeight < DirectCapturePanel.Common.minHeight) {
                            DirectCapturePanel.Common.oHeight = DirectCapturePanel.Common.minHeight;
                        }
                        if (DirectCapturePanel.Common.oWidth < DirectCapturePanel.Common.minHeight) {
                            DirectCapturePanel.Common.oWidth = DirectCapturePanel.Common.minHeight;
                        }
                        DirectCapturePanel.Common.oRight = DirectCapturePanel.Common.oLeft + DirectCapturePanel.Common.oWidth - 1;
                        DirectCapturePanel.Common.oBottom = DirectCapturePanel.Common.oTop + DirectCapturePanel.Common.oHeight - 1;

                    } else {
                        DirectCapturePanel.Common.oLeft = (int) rect.getX() + 1;
                        DirectCapturePanel.Common.oTop = (int) rect.getY() + 1;
                        DirectCapturePanel.Common.oWidth = (int) rect.getWidth();
                        DirectCapturePanel.Common.oHeight = (int) rect.getHeight();
                        if (DirectCapturePanel.Common.oHeight < DirectCapturePanel.Common.minHeight) {
                            DirectCapturePanel.Common.oHeight = DirectCapturePanel.Common.minHeight;
                        }
                        if (DirectCapturePanel.Common.oWidth < DirectCapturePanel.Common.minHeight) {
                            DirectCapturePanel.Common.oWidth = DirectCapturePanel.Common.minHeight;
                        }
                        DirectCapturePanel.Common.oRight = DirectCapturePanel.Common.oLeft + DirectCapturePanel.Common.oWidth - 1;
                        DirectCapturePanel.Common.oBottom = DirectCapturePanel.Common.oTop + DirectCapturePanel.Common.oHeight - 1;
                    }
                }

                Roi roi3 = new Roi(DirectCapturePanel.Common.oLeft - 1, DirectCapturePanel.Common.oTop - 1, DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight);
                roi3.setStrokeColor(Color.green);
                imp.setRoi(roi3);

                setSizeAandSizeB(DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.maxE, DirectCapturePanel.Common.minPI, DirectCapturePanel.Common.maxPI);
                if (DirectCapturePanel.Common.plotInterval > retMaxAllowablePlotInterval(DirectCapturePanel.Common.size_a, DirectCapturePanel.Common.size_b)) {
                    DirectCapturePanel.Common.plotInterval = retMaxAllowablePlotInterval(DirectCapturePanel.Common.size_a, DirectCapturePanel.Common.size_b);
                    tfPlotInterval.setText(Integer.toString(DirectCapturePanel.Common.plotInterval));
                }
                UpdateDimTextField();
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = true;
                tfPixelDimension.setText(Integer.toString(DirectCapturePanel.Common.oWidth) + " x " + Integer.toString(DirectCapturePanel.Common.oHeight));
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent arg0) {
        }
    };

}
