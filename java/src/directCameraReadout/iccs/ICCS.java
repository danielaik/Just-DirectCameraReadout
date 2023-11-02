package directCameraReadout.iccs;

import directCameraReadout.gui.DirectCapturePanel.Common;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.FloatProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

public class ICCS {

    int oW;
    int oH;

    ImagePlus imp; // Intensity image

    //ICCS heatmap
    ImageWindow impICCSmapWin;
    FloatProcessor ipICCS;
    ImagePlus impICCS;

    //Pearson plot
    Plot iplot;
    PlotWindow pearsonScatterWindow;

    private Rectangle rect1;
    private int CCFx;
    private int CCFy;
    private int shiftX;
    private int shiftY;

    private double valICCS[][];//[2*shifty + 1][2*shiftx + 1]contain CCF value
    private int listICCSCoordinate[][][]; //[2*shifty + 1][2*shiftx + 1][j or y corrdinate] index start from 0
    double[] optimalValues; //2D gauss fitted parameter
    int data_width_;

    boolean isPlotICCSmap = true;
    boolean isPlotScatterPearson = true;
    public volatile boolean isFittingICCS = false; //volatile needed GUI acessing flag
    boolean isVisualCentroid = true;

    // Array to store fitted MeanX and MeanY
    ArrayList<ArrayList<Double>> tempCentroidArr = null;
    int nopar = 3; //0 xaxis, 1 meanX, 2 meanY

    //Plot window
    Plot MeanXYPlot; //Mean X, Mean Y plot
    PlotWindow MeanXYWindow;
    String $MeanXYWindowTitle = "Misalignment Plot";
    int MeanXYWindowPosX = 1110;
    int MeanXYWindowPosY = 590;
    int MeanXYDimX = 250;
    int MeanXYDimY = 100;

    int ICCSWindowPosX = 700;
    int ICCSWindowPosY = 565;

    int ScatterPearsonWindowDimX = 200;
    int ScatterPearsonWindowDimY = 200;
    int ScatterPearsonWindowPosX = 1505;
    int ScatterPearsonWindowPosY = 590;

    public ICCS(int oW, int oH) {
        this.oW = oW;
        this.oH = oH;
    }

    private void printlog(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    public boolean SetParam(ImagePlus imp, Rectangle rect1, int ccfx, int ccfy, int pxs, int pys) {
        this.imp = imp;
        this.rect1 = rect1;
        this.CCFx = ccfx;
        this.CCFy = ccfy;
        this.shiftX = pxs;
        this.shiftY = pys;

        boolean isvalid = true;
        if (((int) rect1.getX() + ccfx + pxs + (int) rect1.getWidth()) > oW || ((int) rect1.getX() - 1 + ccfx - pxs) < 0 || ((int) rect1.getY() - 1 + ccfy + pys + (int) rect1.getHeight()) > oH || ((int) rect1.getY() - 1 + ccfy - pys) < 0 || (int) rect1.getX() - 1 + (int) rect1.getWidth() > oW || (int) rect1.getY() - 1 + (int) rect1.getWidth() > oH) {
            isvalid = false;
        }
        if (!Common.isICCSValid) {
            isvalid = false;
        }

        InitArray();

        return isvalid;
    }

    private void InitArray() {
        if (tempCentroidArr == null) {
            tempCentroidArr = new ArrayList<ArrayList<Double>>(2);
            for (int i = 0; i < nopar; i++) {
                tempCentroidArr.add(new ArrayList<Double>());
            }
        }
    }

    public boolean runICCS() {

        boolean isReset = false; // evaluate true when dimension of ICCS map changes

        int wid = 2 * shiftX + 1;
        int hei = 2 * shiftY + 1;
        valICCS = new double[hei][wid];
        listICCSCoordinate = getShiftCoordD_ICCS(rect1, CCFx, CCFy, shiftX, shiftY);

        if (impICCS != null) {
            if (impICCS.getWidth() != wid || impICCS.getHeight() != hei) {
                isReset = true;
            }
        }
//        IJ.log("isReset= " + isReset + ", shiftX: " + shiftX + ", shiftY: " + shiftY);

        // calculating spatial correlation at defined shift
        if (isPlotICCSmap) {
            calcICCS(valICCS, rect1, oW);
            // plot correlation maps
            plotICCSMap(valICCS, isReset);
//            IJ.log("in ICCS fitOn: " + isFittingICCS);
            if (isFittingICCS) {//fit 2D correlation with gaussian true

                boolean fitsuccess = fitGauss2D(valICCS);
                if (fitsuccess && isVisualCentroid) {
                    fillMeanXYValue();
                    plotMeanXYValue();
                }
            }
        }

        if (isPlotScatterPearson) {
            //plot scatter at mid point (this value approach maximum with improved alignment)
            plotScatterPearsonCenterPix(rect1);
        }

        return true;
    }

    private void fillMeanXYValue() {
        int midpoint = (int) data_width_ / 2;
//        IJ.log("Misalignment res X: " + (optimalValues[1] - midpoint) + ", res Y: " + (optimalValues[2] - midpoint));

        // If window closed or window not open, clear arraylist and append value. Otherwise append arraylist
        if (MeanXYWindow == null || MeanXYWindow.isClosed() == true) {//window closed or null
            for (int i = 0; i < nopar; i++) {
                tempCentroidArr.get(i).clear();
            }
        }

        // settle x-axis
        if (tempCentroidArr.get(0).isEmpty()) {
            tempCentroidArr.get(0).add(1.00); //initialize x-axis to 1 every single cycle
        } else {
            double nextval = (double) tempCentroidArr.get(0).get(tempCentroidArr.get(0).size() - 1) + 1;
            tempCentroidArr.get(0).add(nextval);
        }

        // fill meanX
        tempCentroidArr.get(1).add(optimalValues[1] - midpoint);

        // fill meanY
        tempCentroidArr.get(2).add(optimalValues[2] - midpoint);

    }

    private void plotMeanXYValue() {

        double minamp;
        double maxamp;
        double minxx;

        if (MeanXYWindow == null || MeanXYWindow.isClosed() == true) {//(double) Collections.min(tempCentroidArr.get(1))
            minamp = Math.min((double) Collections.min(tempCentroidArr.get(1)), (double) Collections.min(tempCentroidArr.get(2)));
            maxamp = Math.max((double) Collections.max(tempCentroidArr.get(1)), (double) Collections.max(tempCentroidArr.get(2)));

            minamp -= 3;
            maxamp += 3;
            minxx = tempCentroidArr.get(0).get(0);

        } else {
            //Remember last set x-y limit by user
            double[] minmax = MeanXYPlot.getLimits();

            minxx = minmax[0];
            maxamp = minmax[3];
            minamp = minmax[2];
        }

        //plot intensity traces
        MeanXYPlot = new Plot($MeanXYWindowTitle, "Instance", "Deviation (Pixel)");
        MeanXYPlot.setColor(java.awt.Color.BLUE);
        MeanXYPlot.addPoints(tempCentroidArr.get(0), tempCentroidArr.get(1), MeanXYPlot.CONNECTED_CIRCLES);
        MeanXYPlot.addLabel(0.05, 0.20, "X= " + IJ.d2s(tempCentroidArr.get(1).get(tempCentroidArr.get(1).size() - 1), 2));
        MeanXYPlot.setColor(java.awt.Color.MAGENTA);
        MeanXYPlot.addPoints(tempCentroidArr.get(0), tempCentroidArr.get(2), MeanXYPlot.CONNECTED_CIRCLES);
        MeanXYPlot.addLabel(0.25, 0.20, "Y= " + IJ.d2s(tempCentroidArr.get(2).get(tempCentroidArr.get(2).size() - 1), 2));
        MeanXYPlot.setFrameSize(MeanXYDimX, MeanXYDimY);
        MeanXYPlot.setLimits(minxx, tempCentroidArr.get(0).get(tempCentroidArr.get(0).size() - 1), minamp, maxamp);
        MeanXYPlot.setJustification(Plot.CENTER);
        MeanXYPlot.draw();

        if (MeanXYWindow == null || MeanXYWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
            MeanXYWindow = MeanXYPlot.show();
            MeanXYWindow.setLocation(MeanXYWindowPosX, MeanXYWindowPosY);

        } else {
            MeanXYWindow.drawPlot(MeanXYPlot);
            MeanXYWindow.setTitle($MeanXYWindowTitle + "-live");
        }
    }

    private int[][][] getShiftCoordD_ICCS(Rectangle rect, int ccfx, int ccfy, int shiftx, int shifty) {

        int roi1_l = (int) rect.getX();
        int roi1_t = (int) rect.getY();
        int w = (int) rect.getWidth();
        int h = (int) rect.getHeight();

        int lenrow = 2 * shifty + 1;
        int lencol = 2 * shiftx + 1;
        int[][][] outputList = new int[lenrow][lencol][2];

        int tposx = roi1_l - shiftx + ccfx; //posX of roi2
        int tposy = roi1_t - shifty + ccfy; //posY of roi2

        for (int i = 0; i < lenrow; i++) {
            for (int j = 0; j < lencol; j++) {
                outputList[i][j][0] = tposx - roi1_l;
                outputList[i][j][1] = tposy - roi1_t;
                if (tposx != (roi1_l + shiftx + ccfx)) {
                    tposx += 1;
                } else {
                    tposx = roi1_l - shiftx + ccfx;
                    if (tposy != (roi1_t + shifty + ccfy)) {
                        tposy += 1;
                    }
                }
            }
        }
        return outputList;

    }

    private void calcICCS(double[][] PCCValue, Rectangle rect1, int width) {
        //total number of CCF to performed serially = trow * tcol (only CPU implementation)
        int trow = listICCSCoordinate.length;
        int tcol = listICCSCoordinate[0].length; //works for square or rectangular ROI

        double maxPCC = 0;
        for (int i = 0; i < trow; i++) {
            for (int j = 0; j < tcol; j++) {

                PCCValue[i][j] = getPCC(i, j, rect1, width);

                if (PCCValue[i][j] > maxPCC) {
                    maxPCC = PCCValue[i][j];
                }

            }
        }
//        IJ.log("max PCC: " + maxPCC);
    }

    private double getPCC(int nrow, int ncol, Rectangle rect1, int width) {
        int ol = (int) rect1.getX();
        int ot = (int) rect1.getY();
        int ow = (int) rect1.getWidth();
        int oh = (int) rect1.getHeight();

        int CCFxdist = listICCSCoordinate[nrow][ncol][0];
        int CCFydist = listICCSCoordinate[nrow][ncol][1];
        int irl = ol + CCFxdist;
        int irt = ot + CCFydist;
        int sizetocor = ow * oh;
        int indexG = (ot * width + ol);
        int indexR = (irt * width + irl);
        short[] flatten;
        double[] greenChannel = new double[sizetocor];
        double[] redChannel = new double[sizetocor];

        // set green and red roi for visual (not necessary)
        //flatten pixels of size width*height
        flatten = (short[]) imp.getStack().getPixels(1);

        int counterG = 0;
        int counterR = 0;
        for (int i = 0; i < sizetocor; i++) {
            //fill green array
            greenChannel[i] = flatten[indexG];
            if (counterG == (ow - 1)) {
                counterG = 0;
                indexG = indexG + width - ow + 1;
            } else {
                indexG++;
                counterG++;
            }

            //fill red array
            redChannel[i] = flatten[indexR];
            if (counterR == (ow - 1)) {
                counterR = 0;
                indexR = indexR + width - ow + 1;
            } else {
                indexR++;
                counterR++;
            }
        }
        //calculate and return PCC value
        return new PearsonsCorrelation().correlation(greenChannel, redChannel);
    }

    private double getPCC(int nrow, int ncol, Rectangle rect1, int width, ArrayList<double[]> outarray) {
        int ol = (int) rect1.getX();
        int ot = (int) rect1.getY();
        int ow = (int) rect1.getWidth();
        int oh = (int) rect1.getHeight();

        int CCFxdist = listICCSCoordinate[nrow][ncol][0];
        int CCFydist = listICCSCoordinate[nrow][ncol][1];
        int irl = ol + CCFxdist;
        int irt = ot + CCFydist;
        int sizetocor = ow * oh;
        int indexG = (ot * width + ol);
        int indexR = (irt * width + irl);
        short[] flatten;

        // set green and red roi for visual (not necessary)
        //flatten pixels of size width*height
        flatten = (short[]) imp.getStack().getPixels(1);

        int counterG = 0;
        int counterR = 0;
        for (int i = 0; i < sizetocor; i++) {
            //fill green array
            outarray.get(0)[i] = flatten[indexG];
            if (counterG == (ow - 1)) {
                counterG = 0;
                indexG = indexG + width - ow + 1;
            } else {
                indexG++;
                counterG++;
            }

            //fill red array
            outarray.get(1)[i] = flatten[indexR];
            if (counterR == (ow - 1)) {
                counterR = 0;
                indexR = indexR + width - ow + 1;
            } else {
                indexR++;
                counterR++;
            }
        }

        return new PearsonsCorrelation().correlation(outarray.get(0), outarray.get(1));
    }

    private void plotICCSMap(double[][] valICCS, boolean isReset) {

        int hei = valICCS.length;
        int wid = valICCS[0].length;

        if (impICCSmapWin == null || impICCSmapWin.isClosed() == true || isReset) {
            settingICCSImage(wid, hei, isReset);
        }

        //do Display
        for (int y = 0; y < hei; y++) {
            for (int x = 0; x < wid; x++) {
                ipICCS.putPixelValue(x, y, valICCS[y][x]);
            }
        }
        ipICCS.resetMinAndMax();
        impICCS.updateAndDraw();
        if (!impICCSmapWin.isVisible()) {
            impICCSmapWin.setVisible(true);// Problematic: startle settings combobox when called sequentially at fast rate; threfore is the if statement
        }

    }

    private double[][] getArtificialData(ImagePlus imp32bit) {
        int w = imp32bit.getWidth();
        int h = imp32bit.getHeight();
        double[][] resARTIFICIAL = new double[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                resARTIFICIAL[y][x] = imp32bit.getStack().getVoxel​(x, y, 0);
            }
        }

        return resARTIFICIAL;
    }

    private boolean fitGauss2D(double[][] res) {

        //TODO Remove; use artificial 32-bit dataset
        // imp to resARTIFICIAL
        //double[][] resARTIFICIAL = getArtificialData(Common.imp_sample);
        //TODO end
        boolean isFitSuccess;
        TwoDGaussFunction tdgauss;
        double[] InitParam_ = { //initial fit parameters
            255,
            1,
            1,
            1,
            1,
            1
        };
        double[] data_; // flatten double [][] row by row // Contains data to be fitted // support n x n dimension
        // Set initial fit parameters automatically
        double[] maxVal = {//value, x coor, y coor
            0, 0, 0
        };
        double[] minVal = {//value, x coor, y coor
            10000, 0, 0
        };

        // supports n x n dimension currently
        if (res.length != res[0].length) {
            IJ.log("Invalid dimension");
        }
        data_width_ = res[0].length;

        for (int y = 0; y < data_width_; y++) {
            for (int x = 0; x < data_width_; x++) {
                double a = res[y][x];
                if (a > maxVal[0]) {
                    maxVal[0] = a;
                    maxVal[1] = x;
                    maxVal[2] = y;
                }
                if (a < minVal[0]) {
                    minVal[0] = a;
                    minVal[1] = x;
                    minVal[2] = y;
                }
            }
        }
        printlog("maxValue: " + maxVal[0] + ", x " + maxVal[1] + " , y " + maxVal[2]);
        printlog("minValue: " + minVal[0] + ", x " + minVal[1] + " , y " + minVal[2]);

        InitParam_[0] = maxVal[0] * 4;
        InitParam_[1] = maxVal[1];
        InitParam_[2] = maxVal[2];
        InitParam_[3] = 2; //TODO: estiamte sdx
        InitParam_[4] = 2; //TODO: estimate sdy
        InitParam_[5] = minVal[0];

        // 2D to 1D
        data_ = flattenArr(res);

        printlog("Init Amplitude: " + InitParam_[0] + ", Init MeanX: " + InitParam_[1] + ", Init MeanY: " + InitParam_[2] + ", Init SigmaX: " + InitParam_[3] + ", Init SigmaY: " + InitParam_[4] + ", Init Offset: " + InitParam_[5]);

        tdgauss = new TwoDGaussFunction(data_, InitParam_, data_width_, new int[]{1000, 100});//10000,1000

        try {
            //do LevenbergMarquardt optimization and get optimized parameters
            LeastSquaresOptimizer.Optimum opt = tdgauss.fit2dGauss();
            optimalValues = opt.getPoint().toArray(); //Fitted result

//            //fill textfield UI
//            tfAmplitude.setText(Double.toString(optimalValues[0]));
//            tfMeanX.setText(Double.toString(optimalValues[1]));
//            tfMeanY.setText(Double.toString(optimalValues[2]));
//            tfSigmaX.setText(Double.toString(optimalValues[3]));
//            tfSigmaY.setText(Double.toString(optimalValues[4]));
//            tfOffset.setText(Double.toString(optimalValues[5]));
            //output data
            printlog("v0: " + optimalValues[0]);
            printlog("v1: " + optimalValues[1]);
            printlog("v2: " + optimalValues[2]);
            printlog("v3: " + optimalValues[3]);
            printlog("v4: " + optimalValues[4]);
            printlog("v5: " + optimalValues[5]);
            printlog("Iteration number: " + opt.getIterations());
            printlog("Evaluation number: " + opt.getEvaluations());

            isFitSuccess = !(optimalValues[0] < 0 || optimalValues[1] < 0 || optimalValues[2] < 0 || optimalValues[3] < 0 || optimalValues[4] < 0 || optimalValues[1] > data_width_ || optimalValues[2] > data_width_ || optimalValues[3] > data_width_ || optimalValues[4] > data_width_ || optimalValues[5] > maxVal[0]);

        } catch (Exception e) {
            for (int i = 0; i < 6; i++) {
                optimalValues[i] = Double.NaN;
            }
            System.out.println(e.toString());
            isFitSuccess = false;
        }

        return isFitSuccess;

    }

    private double[] flattenArr(double[][] array2d) {
        // works for 16-bit; debug for 32-bit data

        int w = array2d[0].length;
        int h = array2d.length;
        double[] res = new double[w * h];

        for (int y = 0; y < h; y++) {
            System.arraycopy(array2d[y], 0, res, y * w, w);
        }

        //Uncomment to print value
        /*
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                printlog("y: " + y + ", x: " + x + " : " + res[x + (y * w)]);
            }
        }
         */
        return res;

    }

    public void settingICCSImage(int wid, int hei, boolean isReset) { //called once everytime shfitx or shifyy changes

        if ((impICCSmapWin != null) && (impICCSmapWin.isClosed() == false) && isReset) {
            impICCSmapWin.close();
        }

        ipICCS = new FloatProcessor(wid, hei);
        impICCS = new ImagePlus("ICCS", ipICCS);
        impICCS.show();
        impICCSmapWin = impICCS.getWindow();
        impICCSmapWin.setVisible(false);
        impICCSmapWin.setLocation(ICCSWindowPosX, ICCSWindowPosY);

        int sizeX = (int) Math.floor(wid);
        int sizeY = (int) Math.floor(hei);
        double scimp = 30000 * Math.pow(sizeX, -1);
        IJ.run(impICCS, "HiLo", "");	// apply LUT to correlelogram
        IJ.run(impICCS, "Original Scale", ""); 	//first set image to original scale
        IJ.run(impICCS, "Set... ", "zoom=" + scimp + " x=" + (int) Math.floor(sizeX / 2) + " y=" + (int) Math.floor(sizeY / 2));//then zoom to fit within application
        IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
        // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary
        IJ.run(impICCS, "Enhance Contrast", "saturated=0.35");	//autoscaling the contrast for slice 1 
    }

    private void plotScatterPearsonCenterPix(Rectangle rect) {

        // find x,y coordinate of center pixel
        int midH = (int) valICCS.length / 2;
        int midW = (int) valICCS[0].length / 2;
//        IJ.log("Center PCC val(maximize this value): " + valICCS[midH][midW]);

        int sizetocor = (int) rect.getWidth() * (int) rect.getHeight();
        ArrayList<double[]> arrayToCorrelate = new ArrayList<double[]>(2); //0 - green, 1 - red
        for (int i = 0; i < 2; i++) {
            arrayToCorrelate.add(new double[sizetocor]);
        }

        double centerPCC = getPCC(midH, midW, rect, oW, arrayToCorrelate);

        iplot = new Plot("Center correlation (" + midW + "," + midH + ")", "Green", "Red");
        iplot.setColor(java.awt.Color.BLACK);
        iplot.addPoints(arrayToCorrelate.get(0), arrayToCorrelate.get(1), iplot.CIRCLE);
        iplot.setFrameSize(ScatterPearsonWindowDimX, ScatterPearsonWindowDimY);
//        iplot.setLimits(iminsx, imaxsx, iminsc, imaxsc);
        iplot.setJustification(Plot.LEFT);
        iplot.setColor(java.awt.Color.BLUE);
        iplot.addLabel(0.1, 0.1, IJ.d2s(valICCS[midH][midW], 2));
        iplot.draw();

        // either create a new plot window or plot within the existing window
        if (pearsonScatterWindow == null || pearsonScatterWindow.isClosed() == true) {
            pearsonScatterWindow = iplot.show();
            pearsonScatterWindow.setLocation(ScatterPearsonWindowPosX, ScatterPearsonWindowPosY);
        } else {
            pearsonScatterWindow.drawPlot(iplot);
            pearsonScatterWindow.setTitle("center PCC (" + midW + "," + midH + ")");
        }

    }

    //Unused //TODO
    public void hideUnhidePlot(boolean makeVisible) {
        if (MeanXYWindow != null) {
            MeanXYWindow.setVisible(makeVisible);
        }
    }

    //Unused //TODO
    private void disposeAllWIndow() {
        if (pearsonScatterWindow != null && pearsonScatterWindow.isClosed() == false) {
            pearsonScatterWindow.close();
        }

        if (MeanXYWindow != null && MeanXYWindow.isClosed() == false) {
            MeanXYWindow.close();
        }
        if (impICCSmapWin != null && impICCSmapWin.isClosed() == false) {
            impICCSmapWin.close();
        }

    }

}
