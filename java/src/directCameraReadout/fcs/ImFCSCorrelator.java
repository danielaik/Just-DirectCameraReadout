/*
 * Remarks: Software correlator scheme adapted from ImFCS version 1.52 
 *      (https://github.com/ImagingFCS/Imaging_FCS_1_52)
 *      (https://www.dbs.nus.edu.sg/lab/BFL/imfcs_image_j_plugin.html)
 */
package directCameraReadout.fcs;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.HistogramWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingWorker;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.special.Erf;

import gpufitImFCS.GpufitImFCS;

import directCameraReadout.gui.DirectCapturePanel;
import static directCameraReadout.gui.DirectCapturePanel.$mode;
import directCameraReadout.gui.DirectCapturePanel.Common;

public class ImFCSCorrelator {

    public void printlog(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    public void printtimer(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    public void settingImp(ImagePlus imp) {
        this.imp = imp;
    }

    public void settingImp2(ImagePlus imp2) {
        this.imp2 = imp2;
    }

    public boolean settingPlotOption(boolean plotACF, boolean plotTrace, boolean plotAverage, boolean plotJustCCF, int CCFdistX, int CCFdistY, boolean plotCalibrationAmplitude, boolean plotCalibrationDiffusion, boolean isResetCalibPlot, int noaver, int InputBackground, boolean plotCalibrationIntensity, boolean isCalibFixScale) {
        this.plotACFCurves = plotACF;
        this.plotIntensityCurves = plotTrace;
        this.plotAverage = plotAverage;
        this.plotJustCCF = plotJustCCF;
        this.plotCalibrationAmplitude = plotCalibrationAmplitude;
        this.plotCalibrationDiffusion = plotCalibrationDiffusion;
        this.plotCalibrationIntensity = plotCalibrationIntensity;
        this.isResetCalib = isResetCalibPlot;
        this.isCalibFixScale = isCalibFixScale;

        this.cfXDistance = CCFdistX;
        this.cfYDistance = CCFdistY;

        this.noaverpts = noaver;

        if (cfXDistance != 0 || cfYDistance != 0) {
            if (this.plotJustCCF) {
                fitModel = "FCS";
            } else {
                fitModel = "DC-FCCS";
            }
        } else {
            fitModel = "FCS";
        }
        fixfitpar = true;

        if ((cfXDistance != 0 || cfYDistance != 0) && (DirectCapturePanel.Common.$selectedMode.equals("Calibration"))) {
            use2imp = true;
            if (imp.getStackSize() != imp2.getStackSize()) {
                IJ.log("imp imp2 incompatible size");
                return false;
            }
        } else {
            use2imp = false;
        }

        //setting param
        width = imp.getWidth();
        height = imp.getHeight();
        frames = imp.getStackSize();
        firstframe = 1;
        lastframe = frames;
        frametime = DirectCapturePanel.Common.kineticCycleTime; // kinetic cycle not exposure time
        polyOrder = DirectCapturePanel.Common.polynomDegree;
        binningX = DirectCapturePanel.Common.BinXSoft;
        binningY = DirectCapturePanel.Common.BinYSoft;
        correlatorp = DirectCapturePanel.Common.correlator_p;
        if (DirectCapturePanel.Common.$selectedMode.equals("Acquisition")) { //improve Q with more frame
            correlatorq = DirectCapturePanel.multiTauCorrelatorCalculator.getQgivenFrame(DirectCapturePanel.Common.correlator_p, DirectCapturePanel.Common.correlator_q, frames, DirectCapturePanel.Common.dataPtsLastChannel); //adjust Q according to avaialble frame ( recommended Q <= specified Q
        } else { // Q indepenent of no frame
            correlatorq = DirectCapturePanel.multiTauCorrelatorCalculator.getQgivenFrame(DirectCapturePanel.Common.correlator_p, DirectCapturePanel.Common.correlator_q, frames, DirectCapturePanel.Common.dataPtsLastChannel_calibration);
//                correlatorq = EMCCD.correlator_q; //correlate constant q regardless of currently available frame
        }

        bleachCorMem = DirectCapturePanel.Common.bleachCor;
        doFit = false;
        overlap = false;
        if (InputBackground >= 1000000) {
            impmin = minDetermination(imp);//calculate the minimum of the image stack; this will be used as the default background value
        } else {
            impmin = InputBackground;
        }
        setImp = true;

        if ("FCS".equals(fitModel)) {
            if (use2imp) {
                cfXshift = 0;
                cfYshift = 0;
            } else {
                cfXshift = cfXDistance;
                cfYshift = cfYDistance;
            }
        } else {
            cfXshift = 0;
            cfYshift = 0;
        }

        return true;

    }

    public void settingExpParameters(double pixsize, int objmag, double na, int em, double sigma) {
        this.pixelsize = pixsize;
        this.objmag = objmag;
        this.NA = na;
        this.emlambda = em;
        this.sigma = sigma;
    }

    //settings needed to be updated OR
    private int firstframe;		// first frame to be used in the correlation
    private int lastframe;		// last frame to be used in the correlation
    private double frametime;	// acquisition time per frame
    private int frames;			// number of frames in stack
    private int polyOrder = 0;	// polynomial order for bleach correction
    private int width;			// width of loaded stack
    private int height;			// height of loaded stack
    private int binningX = 1;
    private int binningY = 1;
    private double correlatorp;	// parameters for the correlator structure; correlatorp refers to the number of bins in the first channel group.
    private double correlatorq;	// all higher groups have correlatorp/2 channels; correaltorq refers to the number of higher groups
    private String fitModel;
    private String bleachCorMem = "None";
    private boolean doMSD = false;				// should MSD be performed alongside the ACF?
    private boolean doFit = false;					// should fits be performed?
    private boolean fixfitpar;          // should fits initial value be fixed?

    // Parameter for MSD calculation: false is 2D, true is 3D
    private boolean MSDmode = false;

    //for data fitting
    private double[] initparam;				// store the first fit parameters given by the user
    private double[] paraminitval;			// intitial values for the parameters used in the fit (can be the same as initparam or can be continously set; both options are given but only the first is used momentarily)
    private double q2;			// fit parameter, ratio of brightness of second component to first component (if it exists)
    private double q3;			// fit parameter, ratio of brightness of third component to first component (if it exists)
    private double pixeldimx;	// pixel size in object space in [m] used in fit
    private double pixeldimy;	// pixel size in object space in [m] used in fit
    private double sigmaZ = 100000;		// axial PSF factor: sigma * lambda / NA (for simplicity)
    private double sigmaZ2 = 100000;		// axial PSF factor: sigma2 * lambda2 / NA (for simplicity)
    private double fitobsvol;  // normalization factor including the observation volume; for ACFs this is correct; note that N for spatial cross-correaltions has no physical meaning
    private double psfsize;		// actual lateral PSF in [m] for laser 1, used in fit; this is the 1/e2 radius
    private double lsthickness;	// light sheet thickness for SPIM for laser 1 given as 1/e2 radius
    private String filterMem = "none";
    private double[][][][] fitres;			// fit parameter results; [ccf: ACF1, ACF2, CCF][width][height][noparam]
    private double[][][] chi2;				// chi2 values
    private double[][] CCFq;				// map of cross-correlation amount
    private double[][][] pixvalid;			// filtering mask, 1 if pixel valid, NaN if not
    private boolean[][][] pixfitted;		// whether pixel has been successfully fitted or not; in the absence of user-defined thresholds, this array determines pixvalid[][][]
    private int isdlawcalculatedingpu = 0;
    private final int fitMaxIterations = 2000;	// maximum number of iterations allowed in the fits; the maximum number is Integer.MAX_VALUE but that can lead to very long evaluations with typically little improvement on the results
    private final int fitMaxEvaluations = 2000;	// maximum number of evaluations allowed in the fits; the maximum number is Integer.MAX_VALUE but that can lead to very long evaluations with typically little improvement on the result
    private int filterLL = 0;			// intensity filter values with lower and upper limits LL and UL
    private int filterUL = 65536;
    private String currentmodel;		// remember the current model that is used for fitting; this is not necessarily the cbFitModel setting as in some cases multiple

    // parameters determined form the open image stack and defined by the user
    private boolean setImp = false;			// check whether an image was loaded or an existing one assigned
    private int noSettings = 31;
    private String[] panelSettings = new String[noSettings];	// array to store the individual settings, the settings are stored in the same order as used to create the results table
    private boolean[] keyParam = {true, true, true, true, true, true, true, true, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false};
    private boolean askOnRewrite = false; 		// whether setParameters should ask before Results arrays
    private double objmag;		// microscope objective magnification; note the variable 'magnification' is used for the magnification of the stack image in ImageJ
    private double pixelsize;	// pixel size in micrometer on camera chip (variable "a")
    private double NA;			// numerical aperture
    private double emlambda;	// emission wavelength
    private double emlambda2;	// emission wavelength 2
    private double sigma = 0.8;		// lateral PSF factor: sigma * lambda / NA
    private double sigma2;		// lateral PSF factor: sigma2 * lambda2 / NA

    private int background;		// background is determined from the smallest count value from the stack; this can be maually changed in the control panel
    private int background2;	// for FCCS background for the second region is determined as the minimum value for that region
    private int cfXshift;		// x,y distance used in fitting of FCS correlations between two pixels; (0, 0) is for ACFs
    private int cfYshift;
    private boolean overlap;				// can pixels overlap if binning is used?
    private boolean checkroi = false;	//determines whether setParameters() checks for the correct setting of the ROI
    private int fitstart = 1;	// parameter for settign the range of points of the CF to be fitted
    private int fitend = 1;
    HistogramWindow histWin;							// define Histogram Windows

    // parameters for cursor position in window for correlations
    private int pixbinX;			// this is a multiplication factor to determine positions in the image window; it is 1 for overlap and equal to binning for non-overlap
    private int pixbinY;
    private int parcormode;
    private int cfXDistance;	// x,y distance between pixels to be correlated; in DC-FCCS this is the distance between corresponding pixels in red and green channels
    private int cfYDistance;
    private int pixelWidthX;		// determines the number of pixels that can be correlated, depending on whether overlap is allowed for the width and height of the image
    private int pixelHeightY;
    private int roi1StartX = 0;		// first pixel of the ROI used for correlation
    private int roi1StartY = 0;
    private int roi1WidthX = 0;		// dimensions of the ROI used for correlations
    private int roi1HeightY = 0;
    private int maxcposx;			// max positions allowed in image depending on actual image width/height, binning, and distance between pixels;
    private int maxcposy;
    private int mincposx;			// min positions allowed in image depending on actual image width/height, binning, and distance between pixels;
    private int mincposy;

    // arrays and parameters used for computation of correlations and storage of results
    private double[][] currentCovmats;		// the regularized covariance matrix is at the moment not stored but only updated for the current pixel if "GLS Fit" is selected
    private double[][][][] sdacf; 			// standerd deviation of the ACF; dimensions [ccf: ACF1, ACF2, CCF][width][height][chanum]
    private double[][][][] acf = null; 			// ACF array to store all FCS functions for the pixels in the image; dimensions [ccf: ACF1, ACF2, CCF][width][height][chanum]
    private double[][][][] varacf; 			// standerd deviation of the ACF; dimensions [ccf: ACF1, ACF2, CCF][width][height][chanum]
    private double[][][][] fitacf;			// fit functions; [ccf: ACF1, ACF2, CCF][width][height][chanum]
    private double[][][][] res;				// fit residuals; [ccf: ACF1, ACF2, CCF][width][height][chanum]
    private double[][][][] msd; 			// MSD array to store all MSD plots for the pixels in the image; dimensions [ccf: MSD1, MSD2, MSD of CCF][width][height][chanum]
    private double[][][] blocked;			// store yes/no whether blocking was succesful
    private float[][] filterArray;			// filter array; intensity filters to select the pixels to be correlated
    private double[][] aveacf; 			// Array to store average FCS function [ave: ACF1, ACF2, CCF][chanum]
    private double[] intTrace1;				// intensity traces
    private double[] intTrace2;
    private double[] intTime;				// time for intensity traces
    private double fitaveacf[];				// Array to store fit of average FCS function
    private double[][] varaveacf;		// Array to store variance of the average FCS function
    private double msdaveacf[];				// Array to store variance of the average FCS function
    private double resaveacf[];				// Array to store residuals of the average FCS function
    private double chi2aveacf;				// chi2 value for fit of average ACF function
    private double[][] datac;				// temporary array which stores the values along two pixel columns to be correlated through the stack

    private int noparam = 11;
    private final double empty[] = {0.0};			// dummy array used for initilizing plot (should be removed later on)

    private boolean[] paramfit;

    private int[] lag;						// lag for each correlation channel counted as multiples of the smallest basic time; independent of time; lagtime = lag * frametime
    private int base; 					// base = number of channels in first group
    private int hbase; 				// hbase = number of channels in all higher groups
    private double[] mtab; 					// number of samples for each correlation channel; dimensions [chanum]; defined in setParameters()
    private double[] samp;					// sampletime (or bin width) of each channel; dimensions [chanum]; defined in setParameters()
    private int lagnum; 				// number of lag groups for the correlator
    private int chanum; 				// number of total channels of the correlator
    private double[] lagtime; 				// lagtime array which stores the correlation times; dimensions [chanum]; defined in setParameters()
    private double imaxsc; 				// scale for intensity plot
    private double iminsc;// array with information whether a parameter is fit (true) or hold (false)
    private double maxsc; 				// scale for ACF plot
    private double minsc;
    private double maxsx;                               // x-axis scale limit for ACF plot
    private double minsx;
    private double maxamp;                          // amplitude calibration plot scale
    private double minamp;
    private double minint;
    private double maxint;
    private double mind;
    private double maxd;
    private int nopit = 1;				// points in the shortened intensity traces for plotting
    private int blockIndS;				// Index whether block Transformation is succesful (1) or maximum blocking is used (0)

    // Image window
    private int impmin;			// minimum value in the stack
    private double scimp;		// scaling factor to adjust window to an acceptable size for the user

    //background image
    private boolean bgrloaded = false;	// flag to indicate whether backgroudn image was loaded by user 
    private double[][] bgrmean;			// mean values (pixel, row, coumn) for background file

    // Parameter map stack window; in this window the fitted parameters are depicted (e.g. diffusion coefficient maps)
    ImagePlus impPara1;

    // zoom factor: The factor is used to determine the size of the windows on screen; this can be adapted 
    private final int zoomFactor = 250;

    // ImFCS fit panel
    private boolean GLS = false;

    //plotting options & control flow
    private boolean plotACFCurves = true;
    private boolean plotSDCurves = false;
    private boolean plotIntensityCurves = true;
    private boolean plotBlockingCurve = false;
    private boolean plotAverage = false;
    private boolean plotJustCCF = true;
    private boolean use2imp = false;
    private boolean plotCalibrationAmplitude = false;
    private boolean plotCalibrationDiffusion = false;
    private boolean plotCalibrationIntensity = false;
    private boolean isResetCalib = false;
    private boolean isCalibFixScale; // setting true will remove autoscaling for 3 calibration plot: diffusion, amplitude, and intensity
    // blocking curve will not be plotted by default	

    // Plot window
    Plot iplot; //Intensity plot
    Plot acfPlot; //ACF plot
    Plot CalibAmplitudePlot; //Calibration Amplitude plot
    Plot CalibIntensityPlot; //Calibration Intensity Plot
    Plot CalibDiffusionPlot; //Calibration Diffusion Plot

    // Plot windows
    PlotWindow acfWindow;		// ACF
    PlotWindow intWindow;		// Intensity Trace
    PlotWindow sdWindow;		// Standard Devition of the CFs
    PlotWindow blockingWindow;	// Window for blocking analysis; not shown by default; to show, set plotBlockingCurve = true
    private String $acfWindowTitle;
    private String $sdWindowTitle;
    private String $intWindowTitle;
    private String $ampWindowTitle = "Focus-finder (amplitude)";
    private String $CalibIntWindowTitle = "Focus-finder (intensity)";
    private String $CalibDWindowTitle = "Focus-finder (diffusion)";
    PlotWindow ampCalibWindow;           // Average amplitude
    PlotWindow intCalibWindow;          // Average intensity
    PlotWindow DiffCalibWindow;          // Average Diffusion

    //Window and panel positions and dimensions; this has been adjusted for a 1920x1080 screen
    private final int panelPosX = 10;										// control panel, "ImFCS", position and dimensions
    private final int panelPosY = 125;
    private final int panelDimX = 370;
    private final int panelDimY = 370;

    private final int acfWindowPosX = 80;			// ACF window positions and size, with correlation function plots
    private final int acfWindowPosY = 125;
    private final int acfWindowDimX = 200;
    private final int acfWindowDimY = 200;

    private final int sdWindowPosX = acfWindowPosX + acfWindowDimX + 115;	// fit standard deviation window position and size
    private final int sdWindowPosY = acfWindowPosY;
    private final int sdWindowDimX = acfWindowDimX;
    private final int sdWindowDimY = 50;

    private final int intWindowPosX = acfWindowPosX;						// intensity trace window positions and size
    private final int intWindowPosY = acfWindowPosY + acfWindowDimY + 145;
    private final int intWindowDimX = acfWindowDimX;
    private final int intWindowDimY = 50;

    private final int blockingWindowPosX = sdWindowPosX + sdWindowDimX + 110;	// Parameter Blocking Plot Window position and size
    private final int blockingWindowPosY = sdWindowPosY;
    private final int blockingWindowDimX = 200;
    private final int blockingWindowDimY = 100;

    private final int ampCalibWindowDimX = 250;
    private final int ampCalibWindowDimY = 80;
    private final int DCalibWindowDimX = 250;
    private final int DCalibWindowDimY = 80;
    private final int IntCalibWindowDimX = 250;
    private final int IntCalibWindowDimY = 80;
    private final int ampCalibWindowPosX = 700;
    private final int ampCalibWindowPosY = 650;
    private final int IntCalibWindowPosX = 1095;
    private final int IntCalibWindowPosY = 650;
    private final int DCalibWindowPosX = 1490;
    private final int DCalibWindowPosY = 650;

    // Plot live readout
    // synchronization
    private ImagePlus imp;
    private ImagePlus imp2;

    // Array to store calibration parameter such as amplitude and diffusion(average fitted)
    private ArrayList<ArrayList<Double>> calibParamListofList = null;
    private int nocalpar = 4; //0-xaxis, 1-amplitude, 2-diffusion coeficient, 3-intensity
    private int noaverpts; //no of correlation to be averaged excluding zero time lag. useage in focus finder algo
    private double tempD;
    private double[] calibThreshold = { // 0-Diffusion coeff (um2/s)
        100
    };

    public ImFCSCorrelator() {

    }

    public void closeWindowsACF() {
        if (acfWindow != null && acfWindow.isClosed() == false) {
            acfWindow.close();
        }
    }

    public void closeWindowsTrace() {
        if (intWindow != null && intWindow.isClosed() == false) {
            intWindow.close();
        }
    }

    public void closeWindows() {
        if (acfWindow != null && acfWindow.isClosed() == false) {
            acfWindow.close();
        }
        if (intWindow != null && intWindow.isClosed() == false) {
            intWindow.close();
        }
        if (ampCalibWindow != null && ampCalibWindow.isClosed() == false) {
            ampCalibWindow.close();
        }
        if (intCalibWindow != null && intCalibWindow.isClosed() == false) {
            intCalibWindow.close();
        }
    }

    public void runPlotACF() {

        if (!plotACFCurves && !plotIntensityCurves) {
            return;
        }

        if ((lastframe - firstframe + 1) < Common.minAllowableFrame) {
            return;
        }

        if (imp != null) {
//            long time = System.nanoTime();
            obtainImage();
//            IJ.log("ObtainImage called, lastframe: " + lastframe + ", time elapse: " + (System.nanoTime() - time)/1000000 + " ms");
        }
        printParam();
        if (setParameters()) {
            if (imp.getOverlay() != null) {
                imp.getOverlay().clear();
                imp.setOverlay(imp.getOverlay());
            }

            if (use2imp) {
                if (imp2.getOverlay() != null) {
                    imp2.getOverlay().clear();
                    imp2.setOverlay(imp2.getOverlay());
                }
            }

//            int roi2StartX;
//            int roi2WidthX;
//            int roi2StartY;
//            int roi2HeightY;
//            if (cfXDistance > 0) {
//                roi1StartX = 0;
//                roi2StartX = cfXDistance;
//            } else {
//                roi1StartX = -cfXDistance;
//                roi2StartX = 0;
//            }
//            if (cfYDistance > 0) {
//                roi1StartY = 0;
//                roi2StartY = cfYDistance;
//            } else {
//                roi1StartY = -cfYDistance;
//                roi2StartY = 0;
//            }
            if (use2imp) {
                if (overlap) {
                    roi1WidthX = width - Math.abs(cfXDistance);
                    roi1HeightY = height - Math.abs(cfYDistance);
                } else {
                    roi1WidthX = (int) Math.floor((width - Math.abs(0)) / binningX) * binningX;
                    roi1HeightY = (int) Math.floor((height - Math.abs(0)) / binningY) * binningY;
                }

            } else {
                if (overlap) {
                    roi1WidthX = width - Math.abs(cfXDistance);
                    roi1HeightY = height - Math.abs(cfYDistance);
                } else {
                    roi1WidthX = (int) Math.floor((width - Math.abs(cfXDistance)) / binningX) * binningX;
                    roi1HeightY = (int) Math.floor((height - Math.abs(cfYDistance)) / binningY) * binningY;
                }
            }

            Roi impRoi1;
            Roi impRoi2;

            if (DirectCapturePanel.Common.$selectedMode.equals($mode[2])) { //calibration
                impRoi1 = new Roi(0, 0, roi1WidthX, roi1HeightY);
                if (use2imp) {
                    impRoi2 = new Roi(0, 0, roi1WidthX, roi1HeightY);
                    imp2.setRoi(impRoi2);
                }
            } else { //acquisition
                impRoi1 = new Roi(DirectCapturePanel.Common.lLeft - 1, DirectCapturePanel.Common.lTop - 1, DirectCapturePanel.Common.lWidth, DirectCapturePanel.Common.lHeight);
            }

            imp.setRoi(impRoi1);

//            Roi impRoi2 = new Roi(roi2StartX, roi2StartY, roi2WidthX, roi2HeightY);
//            if (cfXDistance != 0 || cfYDistance != 0) {
//                impRoi2.setStrokeColor(java.awt.Color.RED);
//                Overlay cfov = new Overlay(impRoi2);
//                imp.setOverlay(cfov);
//            }
            checkroi = true;
            // a Swing Worker class is used to call the parent method runPlotACF instead. This will let us know when the ACF analysis has been compelted.
//                    correlateRoiWorker correlateRoiInstant = new correlateRoiWorker(impRoi1);
//                    correlateRoiInstant.execute();

            correlateROI(impRoi1);

        }

    }

    // UNUSED; not required to wrap swing worker as parent function is already wrapped with swing worker
    public class correlateRoiWorker extends SwingWorker<Void, Void> {

        private void failIfInterrupted() throws InterruptedException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted while calcualting CFs");
            }
        }

        private final Roi currentROI;

        public correlateRoiWorker(final Roi currentROI) {
            this.currentROI = currentROI;
        }

        @Override
        protected Void doInBackground() throws Exception {
            correlateROI(currentROI);
            return null;
        }

        @Override
        protected void done() {
            DirectCapturePanel.Common.isPlotACFdone = true;

        }

        protected Void isdoneVoid() throws Exception {

            return null;
        }
    }

    // Calculate correlations for a ROI
    public void correlateROI(Roi improi) {

        IJ.showStatus("Correlating ROI");

        boolean IsGPUCalculationOK = true;

        if ((DirectCapturePanel.Common.isgpupresent == 1) && DirectCapturePanel.Common.RunLiveReadOutOnGPU) {

            // The user can only perform 2 particle fits in the GPU version.
            prepareFit();
            IsGPUCalculationOK = GPU_Calculate_ACF_All(improi);
            if (IsGPUCalculationOK) {
                IJ.showStatus("Done");
            }
        }

        if (!IsGPUCalculationOK) {
            System.out.println("An error was encountered while performing calculations on GPU. Calculations will be done on CPU instead.");
        }

        // CPU calculations
        if (DirectCapturePanel.Common.isgpupresent != 1 || !IsGPUCalculationOK || !DirectCapturePanel.Common.RunLiveReadOutOnGPU) {

            Rectangle imprect = improi.getBounds();
            int startXmap = (int) Math.ceil(imprect.getX() / pixbinX);
            int startYmap = (int) Math.ceil(imprect.getY() / pixbinY);
            int startX = startXmap * pixbinX;
            int startY = startYmap * pixbinY;
            int endXmap = (int) Math.floor((imprect.getX() + imprect.getWidth() - binningX) / pixbinX);
            int endYmap = (int) Math.floor((imprect.getY() + imprect.getHeight() - binningY) / pixbinY);
            int endX = endXmap * pixbinX;
            int endY = endYmap * pixbinY;
            int pixcount = 0;
            double q1;
            double q2;

//            //switch the FCCS display off; this would result in too many functions to be displayed
//            tbFCCSDisplay.setSelected(false);
//            tbFCCSDisplay.setText("Off");
            filterArray = new float[width][height]; // calculate the mean image of the stack

            for (int x1 = startX; x1 <= endX; x1 = x1 + pixbinX) {
                for (int x2 = startY; x2 <= endY; x2 = x2 + pixbinY) {
                    for (int x3 = 0; x3 < binningX; x3++) {
                        for (int x4 = 0; x4 < binningY; x4++) {
                            if (improi.contains(x1, x2) && improi.contains(x1, x2 + binningY - 1) && improi.contains(x1 + binningX - 1, x2) && improi.contains(x1 + binningX - 1, x2 + binningY - 1)) {
                                filterArray[x1][x2] += imp.getStack().getProcessor(firstframe).get(x1 + x3, x2 + x4);
                                pixcount++;
                            } else {
                                filterArray[x1][x2] = Float.NaN;
                            }
                        }
                    }
                }
            }

//            IJ.log("startXmap: " + startXmap + ", endXmap: " + endXmap + ", startYmap: " + startYmap + ", endymap: " + endYmap);
            //do the FCS or DC-FCCS evaluation
            if ("FCS".equals(fitModel)) {
                if (doFit) {
//                    prepareFit();
//                    for (int x = startXmap; x <= endXmap; x++) {
//                        for (int y = startYmap; y <= endYmap; y++) {
//                            if (filterArray[x * pixbinX][y * pixbinY] >= filterLL * binningX * binningY && filterArray[x * pixbinX][y * pixbinY] <= filterUL * binningX * binningY) {
//                                calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
//                                correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 0, firstframe, lastframe);
//                                if (!fit(x, y, 0, "FCS")) {
//                                    return;
//                                }
//                            }
//                        }
//                        IJ.showProgress(x - startXmap, startXmap - endXmap);
//                    }
                } else {
                    for (int x = startXmap; x <= endXmap; x++) {
                        for (int y = startYmap; y <= endYmap; y++) {
                            if (!Float.isNaN(filterArray[x * pixbinX][y * pixbinY])) {
                                if (use2imp) {
                                    calcIntensityTrace(imp, imp2, x * pixbinX, y * pixbinY, firstframe, lastframe);
                                    correlate(imp, imp2, x * pixbinX, y * pixbinY, 2, firstframe, lastframe);
                                } else {
                                    calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
                                    correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 0, firstframe, lastframe);
                                }

                            }
                            IJ.showProgress(x - startXmap, startXmap - endXmap);
                        }
                    }
                }
            }

            if ("DC-FCCS".equals(fitModel)) { //DAN only 2D DC-FCCS implemented
                if (doFit) {
//                    prepareFit();
//                    for (int x = startXmap; x <= endXmap; x++) {
//                        for (int y = startYmap; y <= endYmap; y++) {
//                            if (filterArray[x * pixbinX][y * pixbinY] >= filterLL * binningX * binningY && filterArray[x * pixbinX][y * pixbinY] <= filterUL * binningX * binningY) {
//                                calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX, y * pixbinY, firstframe, lastframe);
//                                correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX, y * pixbinY, 0, firstframe, lastframe);
//                                if (!fit(x, y, 0, "ITIR-FCS (2D)")) {
//                                    return;
//                                }
//                                calcIntensityTrace(imp, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
//                                correlate(imp, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 1, firstframe, lastframe);
//                                if (!fit(x, y, 1, "ITIR-FCS (2D)")) {
//                                    return;
//                                }
//                                calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
//                                correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 2, firstframe, lastframe);
//                                if (!fit(x, y, 2, "DC-FCCS")) {
//                                    return;
//                                }
//                                //calculate q value
//                                q1 = fitres[0][x][y][0] / fitres[2][x][y][0];
//                                q2 = fitres[1][x][y][0] / fitres[2][x][y][0];
//                                if (q1 > q2) {
//                                    CCFq[x][y] = q1;
//                                } else {
//                                    CCFq[x][y] = q2;
//                                }
//                                if (CCFq[x][y] > 1) {
//                                    CCFq[x][y] = Double.NaN;	//cross-correlation > 1 is non-physical, most likely due to very noisy CFs, better discarded
//                                }
//                            }
//                        }
//                        IJ.showProgress(x - startXmap, startXmap - endXmap);
//                    }
                } else {
                    for (int x = startXmap; x <= endXmap; x++) {
                        for (int y = startYmap; y <= endYmap; y++) {
                            if (filterArray[x * pixbinX][y * pixbinY] >= 0) {
                                if (use2imp) {
                                    calcIntensityTrace(imp, imp2, x * pixbinX, y * pixbinY, firstframe, lastframe);
                                    correlate(imp, imp2, x * pixbinX, y * pixbinY, 0, firstframe, lastframe);
                                    correlate(imp, imp2, x * pixbinX, y * pixbinY, 1, firstframe, lastframe);
                                    correlate(imp, imp2, x * pixbinX, y * pixbinY, 2, firstframe, lastframe);
                                } else {
                                    calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX, y * pixbinY, firstframe, lastframe);
                                    correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX, y * pixbinY, 0, firstframe, lastframe);
                                    calcIntensityTrace(imp, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
                                    correlate(imp, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 1, firstframe, lastframe);
                                    calcIntensityTrace(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, firstframe, lastframe);
                                    correlate(imp, x * pixbinX, y * pixbinY, x * pixbinX + cfXDistance, y * pixbinY + cfYDistance, 2, firstframe, lastframe);
                                }
                            }
                        }
                        IJ.showProgress(x - startXmap, startXmap - endXmap);
                    }
                }
            }

            if (pixcount > 0) {
                IJ.showStatus("Plotting data.");
                //Correlation curve
                if (plotAverage) {
                    Roi tmproi = new Roi(0, 0, 0, 0);
                    if (cfXDistance != 0 || cfYDistance != 0) {
                        if (plotJustCCF) {
                            calcAveCF(0);//acquisition mode CCF just CCF plot; calibration mode CCF (single function) just CCF
                            plotCF(tmproi, 4, false);//plot average CCF, or ACF
                        } else {
                            calcAveCF(2);//acquisition CCF multiple functions averaged; calibration mode CCF multiple functions
                            plotCF(tmproi, 5, false);//plot average of CCF, ACF1, ACF2 in a single plot
                        }
                    } else {
                        calcAveCF(0);//calibration and acquisition mode ACF
                        plotCF(tmproi, 4, false);//plot average CCF, or ACF
                    }
                } else {
                    if ((cfXDistance != 0 || cfYDistance != 0) && !plotJustCCF) {
                        plotCF(improi, 3, false); //acquisition CCF multiple functions; calibration CCF multiple functions
                    } else {
                        if (use2imp) {
                            plotCF(improi, 6, false); //calibration CCF single function
                        } else {
                            plotCF(improi, 2, false); //calibration and acquisition mode ACF, acquisition mode CCF justCCF,
                        }
                    }
                }

                //Intensity trace
                if (plotIntensityCurves) {
                    if (cfXDistance != 0 || cfYDistance != 0) {
                        plotIntensityTrace(improi, 1);
                    } else {
                        calcAverageIntensityTrace(filterArray, startX, startY, endX, endY, firstframe, lastframe);//calibration and acquisition mode ACF
                        plotIntensityTrace(improi, 2);//calibration and acquisition mode ACF
                    }
                }

                if ((plotCalibrationAmplitude || plotCalibrationDiffusion || plotCalibrationIntensity) && (DirectCapturePanel.Common.$selectedMode.equals($mode[2]))) {

                    fillCalibList(noaverpts); // by default average first 3 point of correlatione excluding 0

                    if (plotCalibrationAmplitude) {
                        plotCalibTrace(0);
                    }
                    if (plotCalibrationDiffusion) {
                        plotCalibTrace(1);
                    }
                    if (plotCalibrationIntensity) {
                        plotCalibTrace(2);
                    }

                }
                //if calibration mode is true, check if either plot amplitude and plot diffusion is true. if so calculate averageacf array and check flag restart plot is false, then do plotting
                //Calibration amplitude
                //Calibration Diffusion constant

//                if (doMSD) { 					// plot MSD if selected
//                    plotMSD(improi, 2, false);
//                }
//                if (doFit) {	// create parameter map window
//                    createParaImp(maxcposx - mincposx + 1, maxcposy - mincposy + 1);
//                }
            } else {
                IJ.log("ROI does not cover a whole single pixel in the binned image. Select larger area or reduce pixel binning");
            }

            IJ.showStatus("Done");
        }
    }

    public void plotCF(Roi plotroi, int cormode, boolean map) {

        // plotroi: roi for which CFs are to be plotted
        // cormode = 1: plot a single function; correlation calculated between (cpx1, cpy1) to (cpx2, cpy2) 
        // cormode = 2: plot all CFs in a ROI; note that this can be ACFs or CCFs depending on the setting
        // of cfXDistance and cfYDistance
        // cormode = 3, plot ACF and CCFs for FCCS
        // map: is roi selected in parameter window (reproduction of values only) or is it from a new calculation?
        // cormode = 4, plot average ACF
        // cormode = 5, plot average CCF, ACF1, and ACF2 in a graph
        // cormode = 6; plot calibration CCFs in a graph without ACF1 and ACF2
        if (!plotACFCurves) {
            return;
        }

        parcormode = cormode;

        int ct = 0;
        int cpx1;
        int cpy1;
        int cpxf;
        int cpyf;
        int cpx2;
        int cpy2;

//            if (fitModel == "DC-FCCS" && cormode == 1) { 	// index ct ensures that in cormode 1 when DC-FCCS is selected the CCF is plotted
//                ct = 2;
//            }
        Rectangle plotrect = plotroi.getBounds();
        if (map == true) {										// if the ROI is selected in the parameter map
            cpx1 = (int) plotrect.getX();
            cpy1 = (int) plotrect.getY();
            if (cfXDistance < 0) {
                cpx1 = cpx1 - (int) Math.floor(cfXDistance / pixbinX);
            }
            if (cfYDistance < 0) {
                cpy1 = cpy1 - (int) Math.floor(cfYDistance / pixbinY);
            }
            cpxf = (int) (cpx1 + plotrect.getWidth());
            cpyf = (int) (cpy1 + plotrect.getHeight());
            cpx2 = (cpx1 * pixbinX + cfXDistance);
            cpy2 = (cpy1 * pixbinY + cfYDistance);
        } else {
            cpx1 = (int) Math.ceil(plotrect.getX() / pixbinX);
            cpy1 = (int) Math.ceil(plotrect.getY() / pixbinY);
            cpxf = (int) Math.floor((plotrect.getX() + plotrect.getWidth() - binningX) / pixbinX);
            cpyf = (int) Math.floor((plotrect.getY() + plotrect.getHeight() - binningY) / pixbinY);
            cpx2 = (cpx1 * pixbinX + cfXDistance);
            cpy2 = (cpy1 * pixbinY + cfYDistance);
        }

        maxsc = acf[ct][cpx1][cpy1][1];		// minimum and maximum setting for plot window
        minsc = 1000;						// set minsc to a high value to make sure it is not 0

        if (DirectCapturePanel.Common.$selectedMode == $mode[2]) {
            minsx = lagtime[1];
            maxsx = 2 * lagtime[chanum - 1];
        } else {
            minsx = lagtime[1];
            maxsx = 2 * 5; //5 sec is usually enough to cover SLB, cells measurement
        }

        int tempXdim = acfWindowDimX;
        int tempYdim = acfWindowDimY;

        if (cormode == 1) {					// if single ACF or CCF
            for (int z = 1; z <= (chanum - 1); z++) {	// find minimum and maximum values in correlation functions that will be plotted
                if (maxsc < acf[ct][cpx1][cpy1][z]) {
                    maxsc = acf[ct][cpx1][cpy1][z];
                }
                if (minsc > acf[ct][cpx1][cpy1][z]) {
                    minsc = acf[ct][cpx1][cpy1][z];
                }
            }

            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                //setting scale
                double[] minmax = acfPlot.getLimits();
                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            // plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)", lagtime, acf[ct][cpx1][cpy1]);
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.BLUE);
            acfPlot.setJustification(Plot.CENTER);

            // create plot label for ACF of CCF
            if (cfXDistance + cfYDistance != 0) {
                acfPlot.addLabel(0.5, 0, " CCF of (" + cpx1 * pixbinX + ", " + cpy1 * pixbinY + ")" + " and (" + cpx2 + ", " + cpy2 + ")" + " at (" + binningX + "x" + binningY + "binning");
            } else if (cfXDistance + cfYDistance != 0 && fitModel == "DC-FCCS") {
                acfPlot.addLabel(0.5, 0, " CCF of (" + cpx1 * pixbinX + ", " + cpy1 * pixbinY + ")" + " and (" + cpx2 + ", " + cpy2 + ")" + " at " + binningX + "x" + binningY + "binning");
            } else {
                acfPlot.addLabel(0.5, 0, " ACFss of (" + cpx1 * pixbinX + ", " + cpy1 * pixbinY + ") at " + binningX + "x" + binningY + "binning");
            }
            acfPlot.draw();
            int num1 = 0;
            if (DirectCapturePanel.Common.isgpupresent == 1) {
                for (int i = 0; i < noparam; i++) {
                    if (paramfit[i]) {
                        num1++;
                    }
                }
            }
//            if (isgpupresent == 1) {
//                ParametricUnivariateFunction theofunction = new FCS_3p();
//                double[] parameters = new double[num1];
//                num1 = 0;
//                for (int i = 0; i < noparam; i++) {
//                    if (paramfit[i]) {
//                        parameters[num1] = fitres[0][cpx1][cpy1][i];
//                        num1++;
//                    }
//                    for (int j = 1; j < chanum; j++) {
//                        fitacf[0][cpx1][cpy1][j] = theofunction.value(lagtime[j], parameters);
//                    }
//                }
//            }

            // if fit has been performed add the fit to the plot
//            if (doFit) {
//                plot.setColor(java.awt.Color.RED);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[ct][cpx1][cpy1], fitstart, fitend + 1), Plot.LINE);
//                //plot.setColor(java.awt.Color.BLUE);
//                plot.draw();
//                if (plotResCurves) {
//                    plotResiduals(cpx1, cpy1); // and add a residual window
//                }
//            }
            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {
                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            if (plotSDCurves) {
                plotSD(cpx1, cpy1, ct);
            }

        }

        if (cormode == 2) {					// if multiple ACF or CCF

            for (int x = cpx1; x <= cpxf; x++) {		// find minimum and maximum values in correlation functions that will be plotted
                for (int y = cpy1; y <= cpyf; y++) {
                    for (int z = 1; z <= (chanum - 1); z++) {
                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                            if (maxsc < acf[0][x][y][z]) {
                                maxsc = acf[0][x][y][z];
                            }
                            if (acf[0][x][y][z] != 0 && minsc > acf[0][x][y][z]) { // make sure minsc is not set to 0 because of a missing CF
                                minsc = acf[0][x][y][z];
                            }
                        }
                    }
                }
            }

            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                // settting scale limit
                double[] minmax = acfPlot.getLimits();
                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                // setting frame size
//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //create empty plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)", empty, empty);
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.BLUE);
            acfPlot.setJustification(Plot.CENTER);

            // If this is a DC-FCCS plot where all functions are supposed to be plotted then plot the cross-correlations only
            int cftype;
            if ("DC-FCCS".equals(fitModel)) {
                cftype = 2;		// cross-correlations are stored in acf[2]; the autocorrelations in acf[0] and acf[1] are not shown in this view
            } else {
                cftype = 0;		// autocorrelations are stored in acf[0];
            }

            // create plot label for ACF of CCF
            if (cfXDistance + cfYDistance != 0 && cftype != 2) {
                acfPlot.addLabel(0.5, 0, " CCFs of pixels in the ROIs at " + binningX + "x" + binningY + "binning");
            } else if (cfXDistance + cfYDistance != 0 && cftype == 2) {
                acfPlot.addLabel(0.5, 0, " CCFs of pixels in the ROIs at " + binningX + "x" + binningY + "binning");
            } else {
                acfPlot.addLabel(0.5, 0, " ACFs from the ROI at " + binningX + "x" + binningY + "binning");
            }

            // plot all CFs and if fit has been performed, add fits
            if (doFit) {
//                for (int y = cpy1; y <= cpyf; y++) {
//                    for (int x = cpx1; x <= cpxf; x++) {
//                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
//                            plot.setColor(java.awt.Color.BLUE);
//                            plot.addPoints(lagtime, acf[cftype][x][y], Plot.LINE);
//                            plot.setColor(java.awt.Color.RED);
//                            int num1 = 0;
//                            if (isgpupresent == 1) {
//                                for (int i = 0; i < noparam; i++) {
//                                    if (paramfit[i]) {
//                                        num1++;
//                                    }
//                                }
//                            }
//
//                            if (isgpupresent == 1) {
//                                double[] parameters = new double[num1];
//                                ParametricUnivariateFunction theofunction;
//                                theofunction = new FCS_3p();
//                                num1 = 0;
//                                for (int i = 0; i < noparam; i++) {
//                                    if (paramfit[i]) {
//                                        parameters[num1] = fitres[0][x][y][i];
//                                        num1++;
//                                    }
//                                }
//
//                                for (int i = 1; i < chanum; i++) {
//                                    fitacf[0][x][y][i] = theofunction.value(lagtime[i], parameters);
//                                }
//                            }
//                            plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[cftype][x][y], fitstart, fitend + 1), Plot.LINE);
//                        }
//                    }
//                }
            } else {
                for (int y = cpy1; y <= cpyf; y++) {
                    for (int x = cpx1; x <= cpxf; x++) {
                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                            acfPlot.setColor(java.awt.Color.BLUE);
                            acfPlot.addPoints(lagtime, acf[cftype][x][y], Plot.LINE);

                        }
                    }
                }
            }

            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {
                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            if (plotSDCurves) {
                plotSD(cpx1, cpy1, ct);
            }
        }

        if (cormode == 3) {
            // if FCCS plot with ACF1, ACF2 and CCF
            for (int x = cpx1; x <= cpxf; x++) {
                for (int y = cpy1; y <= cpyf; y++) {
                    if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                        for (int z = 1; z <= (chanum - 1); z++) {	// find minimum and maximum values in correlation functions that will be plotted
                            if (maxsc < acf[0][x][y][z]) {
                                maxsc = acf[0][x][y][z];
                            }
                            if (maxsc < acf[1][x][y][z]) {
                                maxsc = acf[1][x][y][z];
                            }
                            if (maxsc < acf[2][x][y][z]) {
                                maxsc = acf[2][x][y][z];
                            }
                            if (minsc > acf[0][x][y][z]) {
                                minsc = acf[0][x][y][z];
                            }
                            if (minsc > acf[1][x][y][z]) {
                                minsc = acf[1][x][y][z];
                            }
                            if (minsc > acf[2][x][y][z]) {
                                minsc = acf[2][x][y][z];
                            }
                        }
                    }
                }
            }

            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                double[] minmax = acfPlot.getLimits();

                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            // plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)");
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.GREEN);
            for (int y = cpy1; y <= cpyf; y++) {
                for (int x = cpx1; x <= cpxf; x++) {
                    if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                        acfPlot.setColor(java.awt.Color.GREEN);
                        acfPlot.addPoints(lagtime, acf[0][x][y], Plot.LINE);
                    }
                }
            }
            acfPlot.setJustification(Plot.CENTER);
            acfPlot.setColor(java.awt.Color.RED);
            for (int y = cpy1; y <= cpyf; y++) {
                for (int x = cpx1; x <= cpxf; x++) {
                    if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                        acfPlot.setColor(java.awt.Color.RED);
                        acfPlot.addPoints(lagtime, acf[1][x][y], Plot.LINE);
                    }
                }
            }
            acfPlot.setColor(java.awt.Color.BLUE);
            for (int y = cpy1; y <= cpyf; y++) {
                for (int x = cpx1; x <= cpxf; x++) {
                    if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                        acfPlot.setColor(java.awt.Color.BLUE);
                        acfPlot.addPoints(lagtime, acf[2][x][y], Plot.LINE);
                    }
                }
            }
            acfPlot.addLabel(0.5, 0, " ACF1, ACF2, CCF of (" + cpx1 * pixbinX + ", " + cpy1 * pixbinY + ")" + " and (" + cpx2 * pixbinX + ", " + cpy2 * pixbinY + ")" + " at (" + binningX + "x" + binningY + "binning");
            acfPlot.draw();

            // if fit has been performed add the fit to the plot
            if (doFit) {
//                plot.setColor(java.awt.Color.BLACK);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[0][cpx1][cpy1], fitstart, fitend + 1), Plot.LINE);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[1][cpx1][cpy1], fitstart, fitend + 1), Plot.LINE);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[2][cpx1][cpy1], fitstart, fitend + 1), Plot.LINE);
//                plot.draw();
//                plot.setColor(java.awt.Color.GREEN);
//                if (plotResCurves) {
//                    plotResiduals(cpx1, cpy1); // and add a residual window
//                }
            }

            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {

                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            if (plotSDCurves) {
                plotSD(cpx1, cpy1, 2);
            }
        }

        if (cormode == 4) {					// if average ACF
            int kcf;
            if (use2imp) { //CCF
                kcf = 2;
            } else {
                kcf = 0; // ACF
            }

            maxsc = aveacf[kcf][1];		// minimum and maximum setting for plot window
            minsc = 1000;
            for (int z = 1; z <= (chanum - 1); z++) {	// find minimum and maximum values in correlation functions that will be plotted
                if (maxsc < aveacf[kcf][z]) {
                    maxsc = aveacf[kcf][z];
                }
                if (minsc > aveacf[kcf][z]) {
                    minsc = aveacf[kcf][z];
                }
            }

//            //Print G(1), average G(1,2,3), average G(1,2,3,4,5). TODO: Plot the graph live
//            if (DirectCapturePanel.Common.$selectedMode == "Calibration") {
//                double ampMean = 0;
//                IJ.log("---------------------");
//                IJ.log("G(1): " + aveacf[kcf][1]);
//                for (int z = 1; z <= 3; z++) {
//                    ampMean += aveacf[kcf][z];
//                }
//                ampMean /= 3;
//                IJ.log("ave G(1,2,3): " + ampMean);
//                ampMean = 0;
//                for (int z = 1; z <= 5; z++) {
//                    ampMean += aveacf[kcf][z];
//                }
//                ampMean /= 5;
//                IJ.log("ave G(1,2,3,4,5): " + ampMean);
//            }
            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                double[] minmax = acfPlot.getLimits();

                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            // plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)", lagtime, aveacf[kcf]);
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.BLUE);
            acfPlot.setJustification(Plot.CENTER);

            // create plot label for ACF of CCF
            acfPlot.addLabel(0.5, 0, " Average ACF ");

            acfPlot.draw();

            // if fit has been performed add the fit to the plot
            if (doFit) {
//                plot.setColor(java.awt.Color.RED);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitaveacf, fitstart, fitend + 1), Plot.LINE);
//                plot.setColor(java.awt.Color.BLUE);
//                plot.draw();
//                /*if (plotResCurves) {
//					plotResiduals(cpx1, cpy1); // and add a residual window
//				}*/
            }

            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {
                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            /*if (plotSDCurves) {
                plotSD(cpx1, cpy1, ct);
            }*/
        }

        if (cormode == 5) {
//            maxsc = aveacf[2][1];		// minimum and maximum setting for plot window
//            minsc = 1000;

            for (int z = 1; z <= (chanum - 1); z++) {	// find minimum and maximum values in correlation functions that will be plotted

                if (maxsc < aveacf[0][z]) {
                    maxsc = aveacf[0][z];
                }
                if (maxsc < aveacf[1][z]) {
                    maxsc = aveacf[1][z];
                }
                if (maxsc < aveacf[2][z]) {
                    maxsc = aveacf[2][z];
                }
                if (minsc > aveacf[0][z]) {
                    minsc = aveacf[0][z];
                }
                if (minsc > aveacf[1][z]) {
                    minsc = aveacf[1][z];
                }
                if (minsc > aveacf[2][z]) {
                    minsc = aveacf[2][z];
                }
            }

            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                double[] minmax = acfPlot.getLimits();

                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            // plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)");
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.GREEN);
            acfPlot.addPoints(lagtime, aveacf[0], Plot.LINE);
            acfPlot.setJustification(Plot.CENTER);
            acfPlot.setColor(java.awt.Color.RED);
            acfPlot.addPoints(lagtime, aveacf[1], Plot.LINE);
            acfPlot.setColor(java.awt.Color.BLUE);
            acfPlot.addPoints(lagtime, aveacf[2], Plot.LINE);

            // create plot label for ACF of CCF
            acfPlot.addLabel(0.5, 0, " Average ACF1, ACF2, CCF of (" + cpx1 * pixbinX + ", " + cpy1 * pixbinY + ")" + " and (" + cpx2 * pixbinX + ", " + cpy2 * pixbinY + ")" + " at (" + binningX + "x" + binningY + "binning");
            acfPlot.draw();

            // if fit has been performed add the fit to the plot
            if (doFit) {
//                plot.setColor(java.awt.Color.RED);
//                plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitaveacf, fitstart, fitend + 1), Plot.LINE);
//                plot.setColor(java.awt.Color.BLUE);
//                plot.draw();
//                /*if (plotResCurves) {
//					plotResiduals(cpx1, cpy1); // and add a residual window
//				}*/
            }

            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {
                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            /*if (plotSDCurves) {
                plotSD(cpx1, cpy1, ct);
            }*/
        }

        if (cormode == 6) {					// if multiple ACF or CCF
            for (int x = cpx1; x <= cpxf; x++) {		// find minimum and maximum values in correlation functions that will be plotted
                for (int y = cpy1; y <= cpyf; y++) {
                    for (int z = 1; z <= (chanum - 1); z++) {
                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                            if (maxsc < acf[2][x][y][z]) {
                                maxsc = acf[2][x][y][z];
                            }
                            if (acf[2][x][y][z] != 0 && minsc > acf[2][x][y][z]) { // make sure minsc is not set to 0 because of a missing CF
                                minsc = acf[2][x][y][z];
                            }
                        }
                    }
                }
            }

            // maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            minsc -= minsc * 0.1;
            maxsc += maxsc * 0.1;

            if (acfWindow == null || acfWindow.isClosed() == true) {
            } else {
                double[] minmax = acfPlot.getLimits();

                // setting y-axis scale
                maxsc = minmax[3];
                minsc = minmax[2];

                // setting x-axis scale
                maxsx = minmax[1];
                minsx = minmax[0];

//                //setting frame size
//                Dimension tempDim = acfPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //create empty plot
            acfPlot = new Plot($acfWindowTitle, "tau [s]", "G(tau)", empty, empty);
            acfPlot.setFrameSize(tempXdim, tempYdim);
            acfPlot.setLogScaleX();
            acfPlot.setLimits(minsx, maxsx, minsc, maxsc);
            acfPlot.setColor(java.awt.Color.BLUE);
            acfPlot.setJustification(Plot.CENTER);

            // plot spatial CCF
            int cftype = 2;

            // create plot label for ACF of CCF
            if (cfXDistance + cfYDistance != 0 && cftype != 2) {
                acfPlot.addLabel(0.5, 0, " CCFs of pixels in the ROIs at " + binningX + "x" + binningY + "binning");
            } else if (cfXDistance + cfYDistance != 0 && cftype == 2) {
                acfPlot.addLabel(0.5, 0, " CCFs of pixels in the ROIs at " + binningX + "x" + binningY + "binning");
            } else {
                acfPlot.addLabel(0.5, 0, " ACFs from the ROI at " + binningX + "x" + binningY + "binning");
            }

            // plot all CFs and if fit has been performed, add fits
            if (doFit) {
//                for (int y = cpy1; y <= cpyf; y++) {
//                    for (int x = cpx1; x <= cpxf; x++) {
//                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
//                            plot.setColor(java.awt.Color.BLUE);
//                            plot.addPoints(lagtime, acf[cftype][x][y], Plot.LINE);
//                            plot.setColor(java.awt.Color.RED);
//                            int num1 = 0;
//                            if (isgpupresent == 1) {
//                                for (int i = 0; i < noparam; i++) {
//                                    if (paramfit[i]) {
//                                        num1++;
//                                    }
//                                }
//                            }
//
//                            if (isgpupresent == 1) {
//                                double[] parameters = new double[num1];
//                                ParametricUnivariateFunction theofunction;
//                                theofunction = new FCS_3p();
//                                num1 = 0;
//                                for (int i = 0; i < noparam; i++) {
//                                    if (paramfit[i]) {
//                                        parameters[num1] = fitres[0][x][y][i];
//                                        num1++;
//                                    }
//                                }
//
//                                for (int i = 1; i < chanum; i++) {
//                                    fitacf[0][x][y][i] = theofunction.value(lagtime[i], parameters);
//                                }
//                            }
//                            plot.addPoints(Arrays.copyOfRange(lagtime, fitstart, fitend + 1), Arrays.copyOfRange(fitacf[cftype][x][y], fitstart, fitend + 1), Plot.LINE);
//                        }
//                    }
//                }
            } else {
                for (int y = cpy1; y <= cpyf; y++) {
                    for (int x = cpx1; x <= cpxf; x++) {
                        if ((!map && plotroi.contains(x * pixbinX, y * pixbinY) && plotroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && plotroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && plotroi.contains(x, y))) {
                            acfPlot.setColor(java.awt.Color.BLUE);
                            acfPlot.addPoints(lagtime, acf[cftype][x][y], Plot.LINE);

                        }
                    }
                }
            }

            // either create a new plot window or plot within the existing window
            if (acfWindow == null || acfWindow.isClosed() == true) {
                acfWindow = acfPlot.show();
                acfWindow.setLocation(acfWindowPosX, acfWindowPosY);
            } else {
                acfWindow.drawPlot(acfPlot);
                acfWindow.setTitle($acfWindowTitle);
            }

            // plot the standard deviation of the CF
            if (plotSDCurves) {
                plotSD(cpx1, cpy1, ct);
            }
        }

    }

    // plot the standard deviation
    public void plotSD(int sdpx1, int sdpy1, int cormode) {
        // sdpx1, sdpy1: pixel coordinates
        // cormode 0: ACF, cormode 2: CCF
        double sdminsc = sdacf[cormode][sdpx1][sdpy1][1];		// minimum and maximum setting for plot window
        double sdmaxsc = sdacf[cormode][sdpx1][sdpy1][1];

        for (int x = 1; x < chanum; x++) {	// find minimum and maximum values in sdacf that will be plotted
            if (sdmaxsc < sdacf[cormode][sdpx1][sdpy1][x]) {
                sdmaxsc = sdacf[cormode][sdpx1][sdpy1][x];
            }
            if (sdminsc > sdacf[cormode][sdpx1][sdpy1][x]) {
                sdminsc = sdacf[cormode][sdpx1][sdpy1][x];
            }
        }

        sdminsc -= sdminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
        sdmaxsc += sdmaxsc * 0.1;

        //plot standard deviation
        Plot sdplot = new Plot($sdWindowTitle, "time [s]", "SD", lagtime, sdacf[cormode][sdpx1][sdpy1]);
        sdplot.setFrameSize(sdWindowDimX, sdWindowDimY);
        sdplot.setLogScaleX();
        sdplot.setLimits(lagtime[1], lagtime[chanum - 1], sdminsc, sdmaxsc);
        sdplot.setColor(java.awt.Color.BLUE);
        sdplot.setJustification(Plot.CENTER);
        sdplot.addLabel(0.5, 0, " StdDev (" + sdpx1 * pixbinX + ", " + sdpy1 * pixbinY + ")");
        sdplot.draw();

        if (sdWindow == null || sdWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
            sdWindow = sdplot.show();
            sdWindow.setLocation(sdWindowPosX, sdWindowPosY);
        } else {
            sdWindow.drawPlot(sdplot);
            sdWindow.setTitle($sdWindowTitle);
        }
    }

    // plot the intensity traces
    public void plotIntensityTrace(Roi plotroi, int cormode) {
        // plotroi: roi for which Intensities are to be plotted
        // cormode = 1: plot all intensity traces for a ROI for ACF 
        // cormode = 2: plot all intensity traces in a ROI; ACFs or CCFs depending on the setting of cfXDistance and cfYDistance
        // cormode = 3, plot all intensity traces for a ROI for ACF and CCFs for FCCS

        Rectangle plotrect = plotroi.getBounds();
        int ipx1 = (int) Math.ceil(plotrect.getX() / pixbinX) * pixbinX;
        int ipy1 = (int) Math.ceil(plotrect.getY() / pixbinY) * pixbinY;
        int ipx2 = (ipx1 + cfXDistance);
        int ipy2 = (ipy1 + cfYDistance);

        iminsc = intTrace1[1];		// minimum and maximum setting for plot window
        imaxsc = intTrace1[1];
        int tempXdim = intWindowDimX;
        int tempYdim = intWindowDimY;

        if (cormode == 1) {

            for (int x = 0; x < nopit; x++) {	// find minimum and maximum values in intensity trace 1 that will be plotted
                if (imaxsc < intTrace1[x]) {
                    imaxsc = intTrace1[x];
                }
                if (iminsc > intTrace1[x]) {
                    iminsc = intTrace1[x];
                }
            }

            if ((ipx1 - ipx2) != 0 || (ipy1 - ipy2) != 0) { // in the case of a CCF find also the max and min value in the intensity trace 2
                for (int x = 0; x < nopit; x++) {	// find minimum and maximum values in intensity trace 1 that will be plotted
                    if (imaxsc < intTrace2[x]) {
                        imaxsc = intTrace2[x];
                    }
                    if (iminsc > intTrace2[x]) {
                        iminsc = intTrace2[x];
                    }
                }
            }

            iminsc -= iminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            imaxsc += imaxsc * 0.1;

            if (intWindow == null || intWindow.isClosed() == true) {
            } else {
//                Dimension tempDim = iplot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number

            }

            //plot intensity traces
            iplot = new Plot($intWindowTitle, "time [s]", "Intensity", intTime, intTrace1);
            iplot.setFrameSize(tempXdim, tempYdim);
            iplot.setLimits(intTime[1], intTime[nopit - 1], iminsc, imaxsc);

            if ((ipx1 - ipx2) != 0 || (ipy1 - ipy2) != 0) {
                iplot.setColor(java.awt.Color.GREEN);
            } else {
                iplot.setColor(java.awt.Color.BLUE);
            }

            iplot.setJustification(Plot.CENTER);

            if ((ipx1 - ipx2) != 0 || (ipy1 - ipy2) != 0) {
                iplot.addLabel(0.5, 0, $intWindowTitle + " (" + ipx1 + ", " + ipy1 + ") and (" + ipx2 + ", " + ipy2 + ")");
            } else {
                iplot.addLabel(0.5, 0, $intWindowTitle + " (" + ipx1 + ", " + ipy1 + ")");
            }

            iplot.draw();

            if ((ipx1 - ipx2) != 0 || (ipy1 - ipy2) != 0) {
                iplot.setColor(java.awt.Color.RED);
                iplot.addPoints(intTime, intTrace2, Plot.LINE);
                iplot.setColor(java.awt.Color.GREEN);
            }

            if (intWindow == null || intWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
                intWindow = iplot.show();
                intWindow.setLocation(intWindowPosX, intWindowPosY);
            } else {
                intWindow.drawPlot(iplot);
                intWindow.setTitle($intWindowTitle);
            }
        }

        if (cormode == 2) {

            for (int x = 0; x < nopit; x++) {	// find minimum and maximum values in intensity trace 1 that will be plotted
                if (imaxsc < intTrace1[x]) {
                    imaxsc = intTrace1[x];
                }
                if (iminsc > intTrace1[x]) {
                    iminsc = intTrace1[x];
                }
            }

            iminsc -= iminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
            imaxsc += imaxsc * 0.1;

            if (intWindow == null || intWindow.isClosed() == true) {
            } else {
//                Dimension tempDim = iplot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //plot intensity traces
            iplot = new Plot($intWindowTitle, "time [s]", "Intensity", intTime, intTrace1);
            iplot.setFrameSize(tempXdim, tempYdim);
            iplot.setLimits(intTime[1], intTime[nopit - 1], iminsc, imaxsc);
            iplot.addLabel(0, 0, "Average intensity trace of whole ROI");
            iplot.setJustification(Plot.LEFT);
            iplot.setColor(java.awt.Color.BLUE);
            iplot.draw();

            if (intWindow == null || intWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
                intWindow = iplot.show();
                intWindow.setLocation(intWindowPosX, intWindowPosY);
            } else {
                intWindow.drawPlot(iplot);
                intWindow.setTitle($intWindowTitle);
            }
        }

    }

//        // MSD plots; MSD calcualtion requires prior ACF calculation
//        public void plotMSD(Roi MSDroi, int cormode, boolean map) {
//            // MSDroi: ROI for which MSD is to be calculated
//            // cormode 1: ACF, cormode 2: ACF or CCF, cormode 3: DC-FCCS
//            // map: is roi selected in parameter window (reproduction of values only) or is it from a new calculation?
//
//            int cutoff = chanum - 1;
//            int ct = 0;
//
//            if (cbFitModel.getSelectedItem() == "DC-FCCS") {
//                ct = 2; //in cormode 1, when DC-FCCS is selected, MSD of the cross-correlation is ploted
//            }
//            Rectangle MSDrect = MSDroi.getBounds();
//
//            int msdpx1 = (int) Math.ceil(MSDrect.getX() / pixbinX);
//            int msdpy1 = (int) Math.ceil(MSDrect.getY() / pixbinY);
//            int msdpxf = (int) Math.floor((MSDrect.getX() + MSDrect.getWidth() - binningX) / pixbinX);
//            int msdpyf = (int) Math.floor((MSDrect.getY() + MSDrect.getHeight() - binningY) / pixbinY);
//            if (map == true) {										// if the ROI is selected in the parameter map
//                msdpx1 = (int) MSDrect.getX();
//                msdpy1 = (int) MSDrect.getY();
//                if (cfXDistance < 0) {
//                    msdpx1 = msdpx1 - (int) Math.floor(cfXDistance / pixbinX);
//                }
//                if (cfYDistance < 0) {
//                    msdpy1 = msdpy1 - (int) Math.floor(cfYDistance / pixbinY);
//                }
//                msdpxf = (int) (msdpx1 + MSDrect.getWidth());
//                msdpyf = (int) (msdpy1 + MSDrect.getHeight());
//            }
//            double msdminsc = msd[ct][msdpx1][msdpy1][1];		// minimum and maximum setting for plot window
//            double msdmaxsc = msd[ct][msdpx1][msdpy1][1];
//
//            if (cormode == 1) {
//
//                int i = chanum - 1;
//                while ((msd[ct][msdpx1][msdpy1][i] == 0) && (i > 1)) {
//                    i--;
//                }
//                cutoff = i + 1;
//
//                double[] msdtime = new double[cutoff];
//                msdtime = Arrays.copyOfRange(lagtime, 0, cutoff);
//                double[] msdvalue = new double[cutoff];
//                msdvalue = Arrays.copyOfRange(msd[ct][msdpx1][msdpy1], 0, cutoff);
//
//                for (int x = 1; x < cutoff; x++) {
//                    if (msdmaxsc < msd[ct][msdpx1][msdpy1][x]) {
//                        msdmaxsc = msd[ct][msdpx1][msdpy1][x];
//                    }
//                    if (msdminsc > msd[ct][msdpx1][msdpy1][x]) {
//                        msdminsc = msd[ct][msdpx1][msdpy1][x];
//                    }
//                }
//
//                msdminsc -= msdminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
//                msdmaxsc += msdmaxsc * 0.1;
//
//                //plot MSD
//                Plot msdplot = new Plot($msdWindowTitle, "time [s]", "MSD (um^2)", msdtime, msdvalue);
//                msdplot.setFrameSize(msdWindowDimX, msdWindowDimY);
//                msdplot.setLimits(msdtime[0], msdtime[cutoff - 1], msdminsc, msdmaxsc);
//                msdplot.setColor(java.awt.Color.BLUE);
//                msdplot.setJustification(Plot.CENTER);
//                msdplot.addLabel(0.5, 0, " MSD (" + msdpx1 * pixbinX + ", " + msdpy1 * pixbinY + ")");
//                msdplot.draw();
//
//                if (msdWindow == null || msdWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
//                    msdWindow = msdplot.show();
//                    msdWindow.setLocation(msdWindowPosX, msdWindowPosY);
//                } else {
//                    msdWindow.drawPlot(msdplot);
//                    msdWindow.setTitle($msdWindowTitle);
//                }
//            }
//
//            if (cormode == 2) {
//
//                int i = chanum;						// find cutoff channel for the whole plot 
//                boolean zerofound = true;
//                while (zerofound && i > 2) {
//                    i--;
//                    zerofound = false;
//                    for (int x = msdpx1; x <= msdpxf; x++) {
//                        for (int y = msdpy1; y <= msdpyf; y++) {
//                            if (((!map && MSDroi.contains(x * pixbinX, y * pixbinY) && MSDroi.contains(x * pixbinX, y * pixbinY + binningX - 1) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map && MSDroi.contains(x, y))) && (msd[ct][x][y][i] == 0.0)) {
//                                zerofound = true;
//                            }
//                        }
//                    }
//                }
//                cutoff = i + 1;
//
//                double[] msdtime = new double[cutoff];
//                msdtime = Arrays.copyOfRange(lagtime, 0, cutoff);
//                double[] msdvalue = new double[cutoff];
//
//                for (int x = msdpx1; x <= msdpxf; x++) {		// find minimum and maximum values in MSD plots that will be plotted
//                    for (int y = msdpy1; y <= msdpyf; y++) {
//                        for (int z = 1; z < cutoff; z++) {
//                            if ((!map && MSDroi.contains(x * pixbinX, y * pixbinY) && MSDroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && MSDroi.contains(x, y))) {
//                                if (msdmaxsc < msd[ct][x][y][z]) {
//                                    msdmaxsc = msd[ct][x][y][z];
//                                }
//                                if (msdminsc > msd[ct][x][y][z]) {
//                                    msdminsc = msd[ct][x][y][z];
//                                }
//                            }
//                        }
//                    }
//                }
//
//                msdminsc -= msdminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
//                msdmaxsc += msdmaxsc * 0.1;
//
//                //plot MSD
//                Plot msdplot = new Plot($msdWindowTitle, "time [s]", "MSD (um^2)", empty, empty);
//                msdplot.setFrameSize(msdWindowDimX, msdWindowDimY);
//                msdplot.setLimits(msdtime[0], msdtime[cutoff - 1], msdminsc, msdmaxsc);
//                msdplot.setColor(java.awt.Color.BLUE);
//                msdplot.setJustification(Plot.CENTER);
//                msdplot.addLabel(0.5, 0, "MSDs of pixels in the ROIs at " + binningX + "x" + binningY + "binning");
//                msdplot.draw();
//
//                if (msdWindow == null || msdWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
//                    msdWindow = msdplot.show();
//                    msdWindow.setLocation(msdWindowPosX, msdWindowPosY);
//                } else {
//                    msdWindow.drawPlot(msdplot);
//                    msdWindow.setTitle($msdWindowTitle);
//                }
//
//                for (int y = msdpy1; y <= msdpyf; y++) {
//                    for (int x = msdpx1; x <= msdpxf; x++) {
//                        if ((!map && MSDroi.contains(x * pixbinX, y * pixbinY) && MSDroi.contains(x * pixbinX, y * pixbinY + binningY - 1) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY) && MSDroi.contains(x * pixbinX + binningX - 1, y * pixbinY + binningY - 1)) || (map == true && MSDroi.contains(x, y))) {
//                            msdvalue = Arrays.copyOfRange(msd[ct][x][y], 0, cutoff);
//                            msdplot.setColor(java.awt.Color.BLUE);
//                            msdplot.addPoints(msdtime, msdvalue, Plot.LINE);
//                        }
//                    }
//                }
//                msdWindow.drawPlot(msdplot);
//            }
//
//            if (cormode == 3) {
//
//                int i = chanum - 1;
//                while ((msd[0][msdpx1][msdpy1][i] == 0 || msd[1][msdpx1][msdpy1][i] == 0 || msd[2][msdpx1][msdpy1][i] == 0) && (i > 2)) {
//                    i--;
//                }
//                cutoff = i + 1;
//
//                double[] msdtime = new double[cutoff];
//                msdtime = Arrays.copyOfRange(lagtime, 0, cutoff);
//                double[][] msdvalue = new double[3][cutoff];
//                msdvalue[0] = Arrays.copyOfRange(msd[0][msdpx1][msdpy1], 0, cutoff);
//                msdvalue[1] = Arrays.copyOfRange(msd[1][msdpx1][msdpy1], 0, cutoff);
//                msdvalue[2] = Arrays.copyOfRange(msd[2][msdpx1][msdpy1], 0, cutoff);
//
//                for (int z = 1; z < cutoff; z++) { // find minimum and maximum values in msd that will be plotted
//                    if (msdmaxsc < msd[0][msdpx1][msdpy1][z]) {
//                        msdmaxsc = msd[0][msdpx1][msdpy1][z];
//                    }
//                    if (msdmaxsc < msd[1][msdpx1][msdpy1][z]) {
//                        msdmaxsc = msd[1][msdpx1][msdpy1][z];
//                    }
//                    if (msdmaxsc < msd[2][msdpx1][msdpy1][z]) {
//                        msdmaxsc = msd[2][msdpx1][msdpy1][z];
//                    }
//                    if (msdminsc > msd[0][msdpx1][msdpy1][z]) {
//                        msdminsc = msd[0][msdpx1][msdpy1][z];
//                    }
//                    if (msdminsc > msd[1][msdpx1][msdpy1][z]) {
//                        msdminsc = msd[1][msdpx1][msdpy1][z];
//                    }
//                    if (msdminsc > msd[2][msdpx1][msdpy1][z]) {
//                        msdminsc = msd[2][msdpx1][msdpy1][z];
//                    }
//                }
//
//                msdminsc -= msdminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
//                msdmaxsc += msdmaxsc * 0.1;
//
//                //plot MSD
//                Plot msdplot = new Plot($msdWindowTitle, "time [s]", "MSD (um^2)", empty, empty);
//                msdplot.setFrameSize(msdWindowDimX, msdWindowDimY);
//                msdplot.setLimits(msdtime[0], msdtime[cutoff - 1], msdminsc, msdmaxsc);
//                msdplot.setColor(java.awt.Color.BLUE);
//                msdplot.setJustification(Plot.CENTER);
//                msdplot.addLabel(0.5, 0, " MSD (" + msdpx1 * pixbinX + ", " + msdpy1 * pixbinY + ")");
//                msdplot.draw();
//                msdplot.setColor(java.awt.Color.GREEN);
//                msdplot.addPoints(msdtime, msdvalue[0], Plot.LINE);
//                msdplot.setColor(java.awt.Color.RED);
//                msdplot.addPoints(msdtime, msdvalue[1], Plot.LINE);
//                msdplot.setColor(java.awt.Color.BLUE);
//                msdplot.addPoints(msdtime, msdvalue[2], Plot.LINE);
//
//                if (msdWindow == null || msdWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
//                    msdWindow = msdplot.show();
//                    msdWindow.setLocation(msdWindowPosX, msdWindowPosY);
//                } else {
//                    msdWindow.drawPlot(msdplot);
//                    msdWindow.setTitle($msdWindowTitle);
//                }
//            }
//
//            if (cormode == 4) {
//
//                int i = chanum - 1;
//                while ((msdaveacf[i] == 0) && (i > 1)) {
//                    i--;
//                }
//                cutoff = i + 1;
//
//                double[] msdtime = new double[cutoff];
//                msdtime = Arrays.copyOfRange(lagtime, 0, cutoff);
//                double[] msdvalue = new double[cutoff];
//                msdvalue = Arrays.copyOfRange(msdaveacf, 0, cutoff);
//
//                for (int x = 1; x < cutoff; x++) {
//                    if (msdmaxsc < msdaveacf[x]) {
//                        msdmaxsc = msdaveacf[x];
//                    }
//                    if (msdminsc > msdaveacf[x]) {
//                        msdminsc = msdaveacf[x];
//                    }
//                }
//
//                msdminsc -= msdminsc * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
//                msdmaxsc += msdmaxsc * 0.1;
//
//                //plot MSD
//                Plot msdplot = new Plot($msdWindowTitle, "time [s]", "MSD (um^2)", msdtime, msdvalue);
//                msdplot.setFrameSize(msdWindowDimX, msdWindowDimY);
//                msdplot.setLimits(msdtime[0], msdtime[cutoff - 1], msdminsc, msdmaxsc);
//                msdplot.setColor(java.awt.Color.BLUE);
//                msdplot.setJustification(Plot.CENTER);
//                msdplot.addLabel(0.5, 0, " MSD of average ACF");
//                msdplot.draw();
//
//                if (msdWindow == null || msdWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
//                    msdWindow = msdplot.show();
//                    msdWindow.setLocation(msdWindowPosX, msdWindowPosY);
//                } else {
//                    msdWindow.drawPlot(msdplot);
//                    msdWindow.setTitle($msdWindowTitle);
//                }
//            }
//        }
//        // create parameter maps
//        public void createParaImp(int wx, int hy) {
//            // wx, hy: image width and height
//            int nochannels = 1;		// number of correlation channels; 1 for FCS, 3 for DC-FCCS (for cross-correlation and autocorrelation in the 2 channels respectively)
//            int xm = mincposx;		//introducing pixel shifts in order to align all channels
//            int ym = mincposy;
//            int cshift = 0;			// index shift for renumbering the correlation channels so that cross-correlation parameter maps are in the upper slices of the stack
//            int cm;					// index of the correlation channel in the loop
//            int cq = 0;				// additional one frame for q map in DC-FCCS mode
//
//            impPara1exists = true;
//
//            if (cbFitModel.getSelectedItem() == "DC-FCCS") {
//                nochannels = 3;
//                cshift = 2;
//                cq = 1;
//            }
//
//            if (impPara1 != null) {		// close parameter window if it exists
//                impPara1.close();
//            }
//
//            if (histWin != null && histWin.isClosed() == false) {		// close histogram window if it exists
//                histWin.close();
//            }
//
//            if (doFiltering) {
//                userThreshold[0] = true;
//                userThreshold[2] = true;
//                ReadFilteringFrame(); 									// reads the current Thresholding settings
//            } else {
//                userThreshold[2] = false;
//            }
//
//            impPara1 = IJ.createImage($impPara1Title, "GRAY32", wx, hy, nochannels * (noparam + 3) + cq);	// create a stack for the fit parameters plus chi2, blocking status, filtering mask plus q map
//
//            for (int m = 0; m < nochannels; m++) {							//loop over individual correlation channels and the cross-correlation
//                cm = (m + cshift) % 3;
//                for (int x = 0; x < wx; x++) {
//                    for (int y = 0; y < hy; y++) {
//                        if (doFiltering) {											//if thresholding on, apply the thresholds	
//                            if (filterPix(cm, x + xm, y + ym)) {
//                                pixvalid[cm][x + xm][y + ym] = 1.0;
//                            } else {
//                                pixvalid[cm][x + xm][y + ym] = Double.NaN;
//                            }
//                        } else {
//                            if (pixfitted[cm][x + xm][y + ym]) {
//                                pixvalid[cm][x + xm][y + ym] = 1.0;
//                            } else {
//                                pixvalid[cm][x + xm][y + ym] = Double.NaN;
//                            }
//                        }
//                    }
//                }
//            }
//
//            if (cq == 1) {
//                ImageProcessor ipPara1 = impPara1.getStack().getProcessor(nochannels * (noparam + 3) + cq);	// if q map calculated set the stack to the last frame
//                for (int x = 0; x < wx; x++) {					// fill the frame with q map
//                    for (int y = 0; y < hy; y++) {
//                        ipPara1.putPixelValue(x, y, CCFq[x + xm][y + ym] * pixvalid[0][x + xm][y + ym] * pixvalid[1][x + xm][y + ym] * pixvalid[2][x + xm][y + ym]);
//                        if (userThreshold[4]) {
//                            if ((pixvalid[2][x + xm][y + ym] != 1.0) && (pixvalid[0][x + xm][y + ym] * pixvalid[1][x + xm][y + ym] == 1)) {
//                                ipPara1.putPixelValue(x, y, 0.0);
//                            }
//                        }
//                    }
//                }
//            }
//
//            for (int m = 0; m < nochannels; m++) {							//loop over individual correlation channels and the cross-correlation
//                cm = (m + cshift) % 3;
//
//                for (int p = 0; p < noparam; p++) {					// put pixel values for fit parameters in the maps
//                    ImageProcessor ipPara1 = impPara1.getStack().getProcessor((m * noparam) + p + 1);
//                    for (int x = 0; x < wx; x++) {
//                        for (int y = 0; y < hy; y++) {
//                            ipPara1.putPixelValue(x, y, fitres[cm][x + xm][y + ym][p] * pixvalid[cm][x + xm][y + ym]);
//                        }
//                    }
//                }
//
//                ImageProcessor ipPara1 = impPara1.getStack().getProcessor(nochannels * noparam + m + 1);	// set the stack to the frame for chi2
//                for (int x = 0; x < wx; x++) {					// fill the frame with chi2 values
//                    for (int y = 0; y < hy; y++) {
//                        ipPara1.putPixelValue(x, y, chi2[cm][x + xm][y + ym] * pixvalid[cm][x + xm][y + ym]);
//                    }
//                }
//
//                ipPara1 = impPara1.getStack().getProcessor(nochannels * (noparam + 1) + m + 1);	// set the stack to the frame for blocked values
//                for (int x = 0; x < wx; x++) {					// fill the frame with blocked values
//                    for (int y = 0; y < hy; y++) {
//                        ipPara1.putPixelValue(x, y, blocked[cm][x + xm][y + ym]);
//                    }
//                }
//
//                ipPara1 = impPara1.getStack().getProcessor(nochannels * (noparam + 2) + m + 1);	// set the stack to the frame for filtering mask
//                for (int x = 0; x < wx; x++) {					// fill the frame with filtering mask
//                    for (int y = 0; y < hy; y++) {
//                        ipPara1.putPixelValue(x, y, pixvalid[cm][x + xm][y + ym]);
//                    }
//                }
//
//                impPara1.show();
//                impPara1Win = impPara1.getWindow();
//                impPara1Win.setLocation(para1PosX, para1PosY);
//
//                for (int i = noparam; i >= 1; i--) {		// set label for each parameter map
//                    impPara1.setSlice(i + m * noparam);
//                    IJ.run("Set Label...", "label=" + $param[i - 1] + $channel[m]);
//                }
//
//                impPara1.setSlice(nochannels * noparam + m + 1);		// set label for Chi2
//                IJ.run("Set Label...", "label=" + $param[noparam] + $channel[m]);
//
//                impPara1.setSlice(nochannels * (noparam + 1) + m + 1);		// set label for blocking success
//                IJ.run("Set Label...", "label=" + $param[noparam + 1] + $channel[m]);
//
//                impPara1.setSlice(nochannels * (noparam + 3) + cq);		// set label for q map
//                IJ.run("Set Label...", "label=" + $param[noparam + 3]);
//
//                impPara1.setSlice(nochannels * (noparam + 2) + m + 1);		// set label for filtering mask
//                IJ.run("Set Label...", "label=" + $param[noparam + 2] + $channel[m]);
//            }
//
//            IJ.run(impPara1, "Red Hot", "");	// apply "Fire" LUT
//            IJ.run(impPara1, "Original Scale", ""); 	//first set image to original scale
//            IJ.run(impPara1, "Set... ", "zoom=" + scimp + " x=" + (int) Math.floor(wx / 2) + " y=" + (int) Math.floor(hy / 2)); //then zoom to fit within application
//            IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
//            // this might be a bug and is an ad hoc solution for the moment; before only the "Set" command was necessary
//
//            impPara1.setSlice(1);				// set back to slice 1 for viewing
//            IJ.run(impPara1, "Enhance Contrast", "saturated=0.35");	//autoscaling the contrast for slice 1 
//            Component[] impPara1comp = impPara1Win.getComponents();	// check which component is the scrollbar and add an AdjustmentListener
//            ScrollbarWithLabel impPara1scrollbar;
//            for (int i = 0; i < impPara1comp.length; i++) {
//                if (impPara1comp[i] instanceof ScrollbarWithLabel) {
//                    impPara1scrollbar = (ScrollbarWithLabel) impPara1Win.getComponent(i);
//                    impPara1scrollbar.addAdjustmentListener(impPara1Adjusted);
//                }
//            }
//
//            // create histogram window
//            impPara1.setSlice(1);
//            double histMin = impPara1.getStatistics().min;
//            double histMax = impPara1.getStatistics().max;
//            int histYMax = impPara1.getStatistics().histYMax;
//            int pixelCount = impPara1.getStatistics().pixelCount;in
//            double stdDev = impPara1.getStatistics().stdDev;
//            int q1 = 0;			// determine first quartile
//            int countQ = 0;
//            while (countQ < Math.ceil(pixelCount / 4.0)) {
//                countQ += impPara1.getStatistics().getHistogram()[q1++];
//            }
//            int q3 = 0;			// determine third quartile
//            countQ = 0;
//            while (countQ < Math.ceil(3.0 * pixelCount / 4.0)) {
//                countQ += impPara1.getStatistics().getHistogram()[q3++];
//            }
//            double iqr = (q3 - q1) * impPara1.getStatistics().binSize;		// calculate interquartile distance
//            int nBins;
//            if (iqr > 0) {
//                nBins = (int) Math.ceil(Math.cbrt(pixelCount) * (histMax - histMin) / (2.0 * iqr)); // Freedman-Diaconis rule for number of bins in histogram
//            } else {
//                nBins = 10;
//            }
//
//            $histWindowTitle = $param[0] + " - " + $impTitle;
//            histWin = new HistogramWindow($histWindowTitle, impPara1, nBins, histMin, histMax, histYMax);
//            histWin.setLocationAndSize(histPosX, histPosY, histDimX, histDimY);
//
//            impPara1.setSlice(1);
//            impPara1Can = impPara1.getCanvas();		// get canvas
//            impPara1Can.setFocusable(true);			// set focusable
//
//            // add listeners
//            impPara1Can.addMouseListener(para1MouseClicked);
//        }
    // calcualte the average intensity for the image
    public void calcAverageIntensityTrace(float[][] filterArray, int startX, int startY, int endX, int endY, int initialframe, int finalframe) {
        // introi: roi over which average is to be determined
        // initialframe and finalframe provide the range of frames to be used
        int bckg = 0; // artificially setted to zero for plotting intensity purpose only

        int ave;
        int pixcount = 0;
        ave = (int) Math.floor((finalframe - initialframe + 1) / nopit);
        intTrace1 = new double[nopit]; // reset before updating.

        for (int i = 0; i < nopit; i++) {
            for (int j = firstframe + i * ave; j <= firstframe + (i + 1) * ave - 1; j++) {
                pixcount = 0;
                for (int x1 = startX; x1 <= endX; x1 = x1 + pixbinX) {
                    for (int x2 = startY; x2 <= endY; x2 = x2 + pixbinY) {
                        for (int x4 = 0; x4 < binningX; x4++) {
                            for (int x5 = 0; x5 < binningY; x5++) {
                                if (!Float.isNaN(filterArray[x1][x2])) {
                                    intTrace1[i] += imp.getStack().getProcessor(j).get(x1 + x4, x2 + x5) - bckg;
                                    pixcount++;
                                    intTime[i] = frametime * (i + 0.5) * ave;
                                }
                            }
                        }
                    }
                }
            }
            intTrace1[i] /= (ave * pixcount);	// calculate average intensity for the 'ave' points
        }
    }

    // calculate and fit average 
    public void calcAveCF(int mode) { //mode 0-average of one functions; 1-average of 3 functions
        int mof;
        if (use2imp && plotJustCCF) {
            mof = 2;
        } else {
            mof = 0;
        }

        int count = 0;
        double correlatedwidth = 0; //parameters to account for space correlations and binning
        double correlatedheight = 0;
//        Roi tmproi = new Roi(0, 0, 0, 0); //moved out

        try {
            for (int m = (0 + mof); m <= (mode + mof); m++) {
                for (int k = 0; k < chanum; k++) {
                    aveacf[m][k] = 0.0;
                    varaveacf[m][k] = 0.0;
                    correlatedwidth = width - Math.abs(cfXshift);
                    correlatedheight = height - Math.abs(cfYshift);
                    if (!overlap) {
                        correlatedwidth /= binningX;
                        correlatedheight /= binningY;
                    }

                    if (doFit) {
                        for (int i = 0; i < (correlatedwidth); i++) {
                            for (int j = 0; j < (correlatedheight); j++) {
                                if (acf[m][i][j][k] != 0 && !Double.isNaN(acf[m][i][j][k])) { // use this if only fitted pixels to be averaged:  pixvalid[0][i][j] == 1.0
                                    aveacf[m][k] += acf[0][i][j][k];
                                    varaveacf[m][k] += Math.pow(acf[0][i][j][k], 2.0);
                                    count++;
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < (correlatedwidth); i++) {
                            for (int j = 0; j < (correlatedheight); j++) {
                                if (acf[m][i][j][k] != 0 && !Double.isNaN(acf[m][i][j][k])) {
                                    aveacf[m][k] += acf[m][i][j][k];
                                    varaveacf[m][k] += Math.pow(acf[m][i][j][k], 2.0);
                                    count++;
                                }
                            }
                        }
                    }

                    if (count > 0) {
                        aveacf[m][k] /= count;
                        varaveacf[m][k] /= count;

                    }
                    varaveacf[m][k] = varaveacf[m][k] - Math.pow(aveacf[m][k], 2.0);
                    count = 0;
                }
            }

            if (doMSD) {
                if (!MSDmode) { // 2D if MSDmode is false, otherwise 3D
                    msdaveacf = correlationToMSD(aveacf[0], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6));
                } else {
                    msdaveacf = correlationToMSD3D(aveacf[0], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6), lsthickness * Math.pow(10, 6));
                }
//                    plotMSD(tmproi, 4, false);
            }

            if (doFit) {
//                    prepareFit();
//                    if (initparam[1] < 0 || initparam[6] < 0 || initparam[8] < 0 || initparam[5] < 0 || initparam[7] < 0) {
//                        JOptionPane.showMessageDialog(null, "Fit parameters are out of range. Either D < 0 or F < 0.");
//                        return;
//                    }
//                    averageFit afit = new averageFit();
//                    afit.doFit(cbFitModel.getSelectedItem().toString());
            }

        } catch (Exception e) {/* just ignore the command */        }

        // moved out
//        if (mode == 0) { //plot average CCF, or ACF
//            plotCF(tmproi, 4, false);
//        } else { //plot average of CCF, ACF1, ACF2 in a single plot
//            plotCF(tmproi, 5, false);
//        }
    }

    // fill calibration list
    private void fillCalibList(int nopts) {

        double valD = 0.0;
        double valAmp = 0.0;

        if (!plotAverage) {
            //clculating average
            if (cfXDistance != 0 || cfYDistance != 0) {
                if (plotJustCCF) {
                    calcAveCF(0);
                } else {
                    calcAveCF(2);
                }
            } else {
                calcAveCF(0);
            }
        }

        // Clear arraylist whenever user make changes to ROI corrdinate or width or height
        if (isResetCalib) {
            for (int i = 0; i < nocalpar; i++) {
                calibParamListofList.get(i).clear();
            }
        }

        // settle x-axis
        if (calibParamListofList.get(0).isEmpty()) {
            calibParamListofList.get(0).add(1.00); //initialize x-axis to 1 every single cycle
        } else {
            double nextval = (double) calibParamListofList.get(0).get(calibParamListofList.get(0).size() - 1) + 1;
            calibParamListofList.get(0).add(nextval);
        }

        if (cfXDistance != 0 || cfYDistance != 0) {
            //average from aveacf[2][chanum]
            //transfer aveacf to arraylist (reset arraylist if needed)

            if (plotCalibrationAmplitude) {
                double val = getAverageAmp(nopts, 2);
                if (true) { //check if value is valid //Akshat could help here
                    calibParamListofList.get(1).add(val);
                }
//                IJ.log("added amplitude avr: " + val);
            } else {
                calibParamListofList.get(1).add(Double.NaN);
            }

            if (plotCalibrationDiffusion) {
                IJ.log("Diffusion plot is irrelevant. Set CCFx and CCFy = 0");
                //not implemented: for DC-FCCS(kcf2) or spatial FCS(kcf2) //todo fix cfxshift and cfyshift currently set to 0
            }

            if (plotCalibrationIntensity) {

                //calculate green channel only
                double val = getAverageInt();
                if (true) {//check if value is valid //Akshat could help here
                    calibParamListofList.get(3).add(val);
                }
            } else {
                calibParamListofList.get(3).add(Double.NaN);
            }

        } else {
            //average from aveacf[0][chanum]
            //transfer aveacf to arraylist (reset arraylist if needed)
            if (plotCalibrationAmplitude) {
                double val = getAverageAmp(nopts, 0);
                if (true) {//check if value is valid //Akshat could help here
                    calibParamListofList.get(1).add(val);
                }
//                IJ.log("added amplitude avr: " + val);
            } else {
                calibParamListofList.get(1).add(Double.NaN);
            }

            if (plotCalibrationDiffusion && plotCalibrationAmplitude) {
                // kcf = 0
                double val = getAverageD();
//                IJ.log("D= " + String.format("%.1f", val) + " um2/s");
                if (val < calibThreshold[0]) {//check if value is valid //Akshat could help here
                    calibParamListofList.get(2).add(val);
                } else {
                    calibParamListofList.get(2).add(Double.NaN);
                }

            } else {
                if (plotCalibrationDiffusion) {
                    IJ.log("switch on plotAmplitude");
                }
                calibParamListofList.get(2).add(Double.NaN);
            }

            if (plotCalibrationIntensity) {
                double val = getAverageInt();
                if (true) {//check if value is valid //Akshat could help here
                    calibParamListofList.get(3).add(val);
                }
            } else {
                calibParamListofList.get(3).add(Double.NaN);
            }

        }

    }

    private double getAverageInt() {
        //calculate average counts of size RoiX * RoiY * RoiZ
        ImageProcessor ip;
        double res = 0.0;
        int startXmap = 0;
        int startYmap = 0;
        int endXmap = (int) Math.floor((0 + width - binningX) / pixbinX);
        int endYmap = (int) Math.floor((0 + height - binningY) / pixbinY);

//        IJ.log("inside getAverageInt check parameter: startXmap: " + startXmap + ", startYmap: " + startYmap + ", endXmap: " + endXmap + ", endYmap" + endYmap);
        int counter = 0;
        for (int z = firstframe; z <= lastframe; z++) {
            ip = imp.getStack().getProcessor(z);
            for (int x = startXmap; x <= endXmap; x++) {
                for (int y = startYmap; y <= endYmap; y++) {

                    for (int j = 0; j < binningX; j++) {
                        for (int k = 0; k < binningY; k++) {
                            res += ip.getPixel((x * pixbinX) + j, (y * pixbinY) + k);
                        }
                    }

                    counter++;

                }
            }
        }

        res /= counter;

        return res;
    }

    private double getAverageAmp(int nopts, int kcf) {
        // nopts = number of first few points of correlation fucntion to be averaged (excluding time lag 0)
        // aveacf[kcf][chanum]; kcf 0 - ACF, kcf 2 - CCF
        double res = 0.0;
        if (nopts > (chanum - 1)) {
            IJ.log("error nopts > chanum-1");
        }

        for (int i = 1; i <= nopts; i++) {
            res += aveacf[kcf][i];
        }
        res /= nopts;
        return res;
    }

    private double getAverageD() {

        prepareFit();
        averageFit afit = new averageFit();
        afit.doFit(fitModel);

        return tempD;
    }

    // plotting amplitude and diffusion calibration
    private void plotCalibTrace(int pmode) {

        if (pmode == 0) {
            double minxx;
            int tempXdim = ampCalibWindowDimX;
            int tempYdim = ampCalibWindowDimY;

            //plot amplitude
            if (ampCalibWindow == null || ampCalibWindow.isClosed() == true) {
                minamp = (double) Collections.min(calibParamListofList.get(pmode + 1));
                maxamp = (double) Collections.max(calibParamListofList.get(pmode + 1));

                minamp -= Math.abs(minamp * 0.5);
                maxamp += Math.abs(maxamp * 0.5);
                minxx = calibParamListofList.get(0).get(0);

            } else {
                //Setting Plot limit
                double[] minmax = CalibAmplitudePlot.getLimits();
                minxx = minmax[0];
                if (isCalibFixScale) {
                    maxamp = minmax[3];
                    minamp = minmax[2];
                } else {
                    minamp = (double) Collections.min(calibParamListofList.get(pmode + 1));
                    maxamp = (double) Collections.max(calibParamListofList.get(pmode + 1));

                    minamp -= Math.abs(minamp * 0.5);
                    maxamp += Math.abs(maxamp * 0.5);
                }

////                //Setting border size (if user chose to maximis or minimize plot for clarity)
//                Dimension tempDim = CalibAmplitudePlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //plot intensity traces
            CalibAmplitudePlot = new Plot($ampWindowTitle, "instance", "Amplitude");
            CalibAmplitudePlot.addPoints(calibParamListofList.get(0), calibParamListofList.get(pmode + 1), CalibAmplitudePlot.CONNECTED_CIRCLES);
            CalibAmplitudePlot.setFrameSize(tempXdim, tempYdim);
            CalibAmplitudePlot.setLimits(minxx, calibParamListofList.get(0).get(calibParamListofList.get(0).size() - 1), minamp, maxamp);
            CalibAmplitudePlot.addLabel(0, 0, "Average amplitude of the whole ROI");
            CalibAmplitudePlot.setJustification(Plot.LEFT);
            CalibAmplitudePlot.setColor(java.awt.Color.BLACK);
            CalibAmplitudePlot.draw();

            if (ampCalibWindow == null || ampCalibWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
                ampCalibWindow = CalibAmplitudePlot.show();
                ampCalibWindow.setLocation(ampCalibWindowPosX, ampCalibWindowPosY);

            } else {
                ampCalibWindow.drawPlot(CalibAmplitudePlot);
                ampCalibWindow.setTitle($ampWindowTitle);
            }

        }

        if (pmode == 1) {
            //plot diffusion
            double minxx;
            int tempXdim = DCalibWindowDimX;
            int tempYdim = DCalibWindowDimY;

            if (DiffCalibWindow == null || DiffCalibWindow.isClosed() == true) {
                mind = (double) Collections.min(calibParamListofList.get(pmode + 1));
                maxd = (double) Collections.max(calibParamListofList.get(pmode + 1));

                mind -= Math.abs(mind * 0.1);
                maxd += Math.abs(maxd * 0.1);
                minxx = calibParamListofList.get(0).get(0);

            } else {
                double[] minmax = CalibDiffusionPlot.getLimits();
                minxx = minmax[0];
                if (isCalibFixScale) {
                    maxd = minmax[3];
                    mind = minmax[2];
                } else {
                    mind = (double) Collections.min(calibParamListofList.get(pmode + 1));
                    maxd = (double) Collections.max(calibParamListofList.get(pmode + 1));

                    mind -= Math.abs(mind * 0.1);
                    maxd += Math.abs(maxd * 0.1);
                }

//                //                //Setting border size (if user chose to maximis or minimize plot for clarity)
//                Dimension tempDim = CalibDiffusionPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //plot intensity traces
            CalibDiffusionPlot = new Plot($CalibDWindowTitle, "instance", "Diffusion");
            CalibDiffusionPlot.addPoints(calibParamListofList.get(0), calibParamListofList.get(pmode + 1), CalibDiffusionPlot.CONNECTED_CIRCLES);
            CalibDiffusionPlot.setFrameSize(tempXdim, tempYdim);
            CalibDiffusionPlot.setLimits(minxx, calibParamListofList.get(0).get(calibParamListofList.get(0).size() - 1), mind, maxd);
            CalibDiffusionPlot.addLabel(0, 0, "Average diffusion of the whole ROI");
            CalibDiffusionPlot.setJustification(Plot.LEFT);
            CalibDiffusionPlot.setColor(java.awt.Color.BLACK);
            CalibDiffusionPlot.draw();

            if (DiffCalibWindow == null || DiffCalibWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
                DiffCalibWindow = CalibDiffusionPlot.show();
                DiffCalibWindow.setLocation(DCalibWindowPosX, DCalibWindowPosY);

            } else {
                DiffCalibWindow.drawPlot(CalibDiffusionPlot);
                DiffCalibWindow.setTitle($CalibDWindowTitle);
            }
        }

        if (pmode == 2) {
            //plot average intensity trace considering pixel binning
            double minxx;
            int tempXdim = IntCalibWindowDimX;
            int tempYdim = IntCalibWindowDimY;
            //plot intensity
            if (intCalibWindow == null || intCalibWindow.isClosed() == true) {
                minint = (double) Collections.min(calibParamListofList.get(pmode + 1));
                maxint = (double) Collections.max(calibParamListofList.get(pmode + 1));

                minint -= minint * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
                maxint += maxint * 0.1;
                minxx = calibParamListofList.get(0).get(0);

            } else {
                double[] minmax = CalibIntensityPlot.getLimits();
                minxx = minmax[0];
                if (isCalibFixScale) {
                    maxint = minmax[3];
                    minint = minmax[2];
                } else {
                    minint = (double) Collections.min(calibParamListofList.get(pmode + 1));
                    maxint = (double) Collections.max(calibParamListofList.get(pmode + 1));

                    minint -= minint * 0.1;		// maximum scales are to be 10% larger than maximum value and 10% smaller than minimum value
                    maxint += maxint * 0.1;
                }

//                //                //Setting border size (if user chose to maximis or minimize plot for clarity)
//                Dimension tempDim = CalibIntensityPlot.getSize();
//                tempXdim = (int) tempDim.getWidth() - 96; // 96 is an arbitrary number
//                tempYdim = (int) tempDim.getHeight() - 63; // 63 is an arbitrary number
            }

            //plot intensity traces
            CalibIntensityPlot = new Plot($intWindowTitle, "instance", "Intensity");
            CalibIntensityPlot.addPoints(calibParamListofList.get(0), calibParamListofList.get(pmode + 1), CalibIntensityPlot.CONNECTED_CIRCLES);
            CalibIntensityPlot.setFrameSize(tempXdim, tempYdim);
            CalibIntensityPlot.setLimits(minxx, calibParamListofList.get(0).get(calibParamListofList.get(0).size() - 1), minint, maxint);
            CalibIntensityPlot.addLabel(0, 0, "Average intensity of the whole ROI");
            CalibIntensityPlot.setJustification(Plot.LEFT);
            CalibIntensityPlot.setColor(java.awt.Color.BLACK);
            CalibIntensityPlot.draw();

            if (intCalibWindow == null || intCalibWindow.isClosed() == true) {	// create new plot if window doesn't exist, or reuse existing window
                intCalibWindow = CalibIntensityPlot.show();
                intCalibWindow.setLocation(IntCalibWindowPosX, IntCalibWindowPosY);

            } else {
                intCalibWindow.drawPlot(CalibIntensityPlot);
                intCalibWindow.setTitle($CalibIntWindowTitle);
            }

        }

    }

    public boolean setParameters() {

        int index = 0;
        boolean resetResults = false; 								// whether Result arrays need to be reset
        boolean proceed = true;									// whether program should proceed resetting the Results 
        boolean onlySigmaOrBinChanged = true;							// whether sigma0 is the only parameter changed in the panel - PSF calibration is not reset in that case
        boolean onlyBinChanged = true;							// whether binning is the only parameter changed in the panel - diff law is not reset in that case
        String[] newPanelSettings = new String[noSettings];		// an array to temporarily hold the settings from the Panel

        checkImp();		// check if image was loaded or assigned
        // read settings from the panel and store them temporarily in newPanelSettings
        int tmpct = 0;
        newPanelSettings[tmpct++] = Integer.toString(firstframe);		// index 0
        newPanelSettings[tmpct++] = Integer.toString(lastframe);
        newPanelSettings[tmpct++] = Double.toString(frametime);
        String strbin = "1 x 1";
        int strlen = strbin.length();
        newPanelSettings[tmpct++] = strbin.substring(0, strbin.indexOf(" x "));
        newPanelSettings[tmpct++] = strbin.substring(strbin.indexOf(" x ") + 3);
        newPanelSettings[tmpct++] = "0";
        newPanelSettings[tmpct++] = "0";
        newPanelSettings[tmpct++] = "16";
        newPanelSettings[tmpct++] = "8";
        newPanelSettings[tmpct++] = "FCS";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = Double.toString(pixelsize);
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = Double.toString(objmag);
        newPanelSettings[tmpct++] = Double.toString(NA);
        newPanelSettings[tmpct++] = Double.toString(emlambda);
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = Double.toString(sigma);
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = Integer.toString(impmin);
        newPanelSettings[tmpct++] = Integer.toString(impmin);
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "Polynomial";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "4";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "";
        newPanelSettings[tmpct++] = "";

        // check whether any settings have changed
        for (int i = 0; i < noSettings; i++) {
            if (!(newPanelSettings[i].equals(panelSettings[i])) && keyParam[i]) {
                resetResults = true;
                if (i != 17 && i != 19 && i != 3 && i != 4) {
                    onlySigmaOrBinChanged = false;
                }
                if (i != 3) {
                    onlyBinChanged = false;
                }
                if (i != 4) {
                    onlyBinChanged = false;
                }
            }
        }

//            if (resetResults && askOnRewrite) {
//                GenericDialog gd = new GenericDialog("Delete the Results and start new?");
//                gd.addMessage("Some of the parameter settings in the main panel have been changed");
//                gd.addMessage("Continuing will results in deleting some Results");
//                gd.addMessage("Do you wish to proceed?");
//                gd.showDialog();
//                if (gd.wasOKed()) {
//                    proceed = true;
//                }
//                if (gd.wasCanceled()) {
//                    proceed = false;
//                }
//            }
        if (proceed) {
            // assign the values to the variables used in the calculations

            try {
//                    firstframe = 1;
//                    lastframe = Integer.parseInt(newPanelSettings[1]);
//                    frametime = Double.parseDouble(newPanelSettings[2]);
//                    binningX = Integer.parseInt(newPanelSettings[3]);
//                    binningY = Integer.parseInt(newPanelSettings[4]);
//                cfXDistance = Integer.parseInt(newPanelSettings[5]);
//                cfYDistance = Integer.parseInt(newPanelSettings[6]);
//                    correlatorp = Integer.parseInt(newPanelSettings[7]);
//                    correlatorq = Integer.parseInt(newPanelSettings[8]);
                pixelsize = Double.parseDouble(newPanelSettings[11]);
                objmag = Double.parseDouble(newPanelSettings[13]);
                NA = Double.parseDouble(newPanelSettings[14]);
                emlambda = Double.parseDouble(newPanelSettings[15]);
//                emlambda2 = Double.parseDouble(newPanelSettings[16]);
                sigma = Double.parseDouble(newPanelSettings[17]);
//                sigmaZ = Double.parseDouble(newPanelSettings[18]);
//                sigma2 = Double.parseDouble(newPanelSettings[19]);
//                sigmaZ2 = Double.parseDouble(newPanelSettings[20]);
                background = Integer.parseInt(newPanelSettings[21]);
                background2 = Integer.parseInt(newPanelSettings[22]);
            } catch (NumberFormatException nfe) {
                IJ.showMessage("A parameter in the panel has an invalid format");
                throw new NumberFormatException("Number format error.");
            }

            // set maximum, and minimum cursor positions possible in the image, depending on binning and whether overlap is allowed
            // parameters need to be checked according to these settings as allowed parameter ranges differ in overlap and non-overlap mode
            if (overlap) {
                pixelWidthX = width - binningX;		// these values are the correct maximum if counting from 0
                pixelHeightY = height - binningY;
                pixbinX = 1;
                pixbinY = 1;
            } else {
                pixelWidthX = (int) Math.floor(width / binningX) - 1;
                pixelHeightY = (int) Math.floor(height / binningY) - 1;
                pixbinX = binningX;
                pixbinY = binningY;
            }

//            // check parameter settings
//            // check that numbers are not out of bounds and make sense
//            if (firstframe < 1 || firstframe > frames || lastframe < 1 || lastframe > frames || firstframe >= lastframe) {
//                JOptionPane.showMessageDialog(null, "Frames set incorrectly");
//                return false;
//            }
//
//            if (binningX < 1 || binningY < 1) {
//                JOptionPane.showMessageDialog(null, "Parameter \"binning\" is smaller than 1.");
//                return false;
//            }
//
//            if (binningX > width || binningY > height) {
//                JOptionPane.showMessageDialog(null, "Parameter \"binning\" is larger than image size.");
//                return false;
//            }
//
//            // check that the distance between pixels to be correlated is smaller than the size of the image
//            if (Math.abs(cfXDistance) > width - binningX || Math.abs(cfYDistance) > height - binningY) {
//                JOptionPane.showMessageDialog(null, "Correlation distance is larger than image size.");
//                return false;
//            }
//
//            // check that pixel values are within image
//            if (checkroi) {
//                if ((cfXDistance < 0 && Math.ceil((double) roi1StartX / pixbinX) * pixbinX < cfXDistance * (-1)) || (cfYDistance < 0 && Math.ceil((double) roi1StartY / pixbinY) * pixbinY < cfYDistance * (-1))) {
//                    JOptionPane.showMessageDialog(null, "Correlation points are not within image.");
//                    return false;
//                }
//
//                if ((cfXDistance >= 0 && Math.floor(((double) roi1StartX + roi1WidthX - binningX) / pixbinX) * pixbinX + cfXDistance > width - binningX) || (cfYDistance >= 0 && Math.floor(((double) roi1StartY + roi1HeightY - binningY) / pixbinY) * pixbinY + cfYDistance > height - binningY)) {
//                    JOptionPane.showMessageDialog(null, "Correlation points are not within image.");
//                    return false;
//                }
//
//                if (fitModel == "DC-FCCS") { //this applies only for dual-color cross-correlations
//                    // check that the correlation areas don't overlap
//                    if (Math.abs(cfXDistance) < roi1WidthX && Math.abs(cfYDistance) < roi1HeightY) {
//                        JOptionPane.showMessageDialog(null, "Cross-correlation areas overlap.");
//                        return false;
//                    }
//                }
//            }
//
//            if (cfXDistance % pixbinX != 0 || cfXDistance % pixbinX != 0) {
//                JOptionPane.showMessageDialog(null, "Warning: CF distance is not a multiple of pixel bin.");
//            }
//
//            // check that background1 value is sensible
//            if (background < 0) {
//                JOptionPane.showMessageDialog(null, "Invalid background 1 value");
//                return false;
//            } else if (background > impmin) {
//                JOptionPane.showMessageDialog(null, "Warning: Background 1 value is larger than smallest pixel value");
//            }
//
//            // check that background2 value is sensible
//            if (background2 < 0) {
//                JOptionPane.showMessageDialog(null, "Invalid background 2 value");
//                return false;
//            } else if (background2 > impmin) {
//                JOptionPane.showMessageDialog(null, "Warning: Background 2 value is larger than smallest pixel value");
//            }
//
//            // check whether correlator Q is sensible
//            if (correlatorq < 1) {
//                JOptionPane.showMessageDialog(null, "Correlator Q is smaller than 1");
//                return false;
//            }
//
//            // check whether there are enough frames for the correlation and give a warning if there are less than minFrameReq frames for the last correlation channel
//            if ((lastframe - firstframe + 1 - Math.pow(2, correlatorq - 1) * correlatorp / 2 - correlatorp / 4 * Math.pow(2, correlatorq)) / Math.pow(2, correlatorq - 1) < 1) {
//                JOptionPane.showMessageDialog(null, "Not enough frames");
//                return false;
//            } else if ((lastframe - firstframe + 1 - Math.pow(2, correlatorq - 1) * correlatorp / 2 - correlatorp / 4 * Math.pow(2, correlatorq)) / Math.pow(2, correlatorq - 1) < minFrameReq) {
//                JOptionPane.showMessageDialog(null, "Warning: Less than " + minFrameReq + " data point for last correlation channel.");
//            }
            // set common arrays and parameters according to user settings in the panel
            if ((lastframe - firstframe + 1) < 1000) {	// use 1000 points for the intensity, except when less than 1000 frames are present
                nopit = (lastframe - firstframe + 1);
            } else {
                nopit = 1000;
            }

            // if sliding window correction is needed then the correlator structure has to be adapted to the smaller number of lagtimes
            int num; 	// total number of frames to be correlated; sliding window length or lastframe-firstframe+1
//            String $bcmode = (String) cbBleachCor.getSelectedItem();
//            if ("Sliding Window".equals($bcmode)) {
//                lagnum = (int) Math.floor((Math.log(slidingWindowLength / (swMinFrameReq + correlatorp)) + 1) / Math.log(2));
//                if (lagnum < correlatorq) {	// allow smaller correlatorq values as minimum but not larger
//                    correlatorq = lagnum;
//                    tfCorrelatorQ.setText(Integer.toString(lagnum));
//                } else {
//                    lagnum = (int) correlatorq;
//                }
//                chanum = (int) (correlatorp + (lagnum - 1) * correlatorp / 2 + 1);
//                num = slidingWindowLength;
//            } else {
//                chanum = (int) (correlatorp + (correlatorq - 1) * correlatorp / 2 + 1);
//                lagnum = (int) correlatorq;
//                num = (lastframe - firstframe + 1);
//            }

            chanum = (int) (correlatorp + (correlatorq - 1) * correlatorp / 2 + 1);
            lagnum = (int) correlatorq;
            num = (lastframe - firstframe + 1);

            // initialize arrays required for calculations; they change with each new paramter setting and are thus re-initialized
            intTrace1 = new double[nopit];
            intTrace2 = new double[nopit];
            intTime = new double[nopit];

//            // the arrays for CFs and results of fitting are reinitialized only if key parameters have changed to prevent mixing results for different settings in a single parameter map
//            if (resetResults) {
//                initializeArrays();
//                tfFitEnd.setText(Integer.toString(chanum - 1));			// reset the fitting range for data
//                fitstart = Integer.parseInt(tfFitStart.getText());
//                fitend = Integer.parseInt(tfFitEnd.getText());
//                if (!onlyBinChanged && !expload) {					// if other parameter than binning has been changed, reset diff. law data and close diff. law window if it exists; but don't do anything if htis is an experimetn load
//                    difflaw = new double[3][1];
//                    difflawbin = 1;
//                    if (difflawWindow != null && !difflawWindow.isClosed()) {
//                        difflawWindow.close();
//                    }
//                    //YYY(close difflawmap and initializearrays)
//                    if (impDLMap != null) {
//                        impDLMap.close();		// close diffusion law map window if it exists
//                    }
//                    difflawarray = new double[1][1][3][1];
//                    diffLawFitMap = new double[1][1][2];
//                    diffLawMapwidth = 1;
//                    diffLawMapheight = 1;
//                    difflawmapbin = 1;
//                    difflawallbin = 1;
//                    for (int k = 0; k < 2; k++) {
//                        difflawfit[k] = 0.0;
//                        diffLawFitLim[k] = 0;
//                    }
//                    if (!onlySigmaOrBinChanged) {				// if other parameter than sigma or bin have been changed, reset PSF data and close PSF window if it exists
//                        psfData = new double[1][3][1];
//                        psfmaxbin = 1;
//                        numofpsf = 1;
//                        if (PSFWindow != null && !PSFWindow.isClosed()) {
//                            PSFWindow.close();
//                        }
//                    }
//                }
//            }
            // initialize arrays required for calculations; they change with each new paramter setting				
            base = (int) correlatorp; 		// base = number of channels in first group
            hbase = (int) correlatorp / 2; 	// hbase = number of channels in all higher groups
            mtab = new double[chanum]; 		// number of samples for each correlation channel
            lag = new int[chanum];			// lag for each correlation channel; indepenednet of time
            samp = new double[chanum];		// sampletime (or bin width) of each channel
            lagtime = new double[chanum];	// lagtime = lag*frametime; this is the actual lagtime in seconds for each channel

            for (int x = 0; x <= hbase; x++) {	// calculate lag and lagtimes for the 0 lagtime channel and the first 8 channels
                lag[x] = x;
                lagtime[x] = lag[x] * frametime;
            }

            for (int x = 1; x <= lagnum; x++) {	// calculate lag and lagtimes for all higher channels
                for (int y = 1; y <= hbase; y++) {
                    lag[x * hbase + y] = (int) (Math.pow(2, x - 1) * y + (base / 4) * Math.pow(2, x));
                    lagtime[x * hbase + y] = lag[x * hbase + y] * frametime;
                }
            }

            for (int x = 0; x <= base; x++) {	// calculate sampletimes (bin width) for the 0 lagtime channel and the first 8 channels
                samp[x] = 1;
            }

            for (int x = 2; x <= lagnum; x++) {	// calculate sampletimes (bin width) for all higher channels
                for (int y = 1; y <= hbase; y++) {
                    samp[x * hbase + y] = Math.pow(2, x - 1);
                }
            }

            // calculate the number of samples for each channel including 0 lagtime; this differs for sliding window correction
            // the variable num takes care of this
            for (int x = 0; x <= (chanum - 1); x++) {
                mtab[x] = (int) Math.floor((num - lag[x]) / samp[x]);
            }

            // set initial, maximum, and minimum cursor positions possible in the image
            if (cfXDistance >= 0) {
                maxcposx = pixelWidthX - (int) Math.ceil(((double) cfXDistance - (width - (pixelWidthX * pixbinX + binningX))) / pixbinX);
                mincposx = 0;
            } else {
                maxcposx = pixelWidthX;
                mincposx = -(int) Math.floor((double) cfXDistance / pixbinX);
            }

            if (cfYDistance >= 0) {
                maxcposy = pixelHeightY - (int) Math.ceil(((double) cfYDistance - (height - (pixelHeightY * pixbinY + binningY))) / pixbinY);
                mincposy = 0;
            } else {
                maxcposy = pixelHeightY;
                mincposy = -(int) Math.floor((double) cfYDistance / pixbinY);
            }

            if (true) {
                initparam[0] = 1;
                initparam[1] = 1 / Math.pow(10, 12);
                initparam[2] = 0 / Math.pow(10, 6);
                initparam[3] = 0 / Math.pow(10, 6);
                initparam[4] = 0;
                initparam[5] = 0;
                initparam[6] = 0 / Math.pow(10, 12);
                initparam[7] = 0;
                initparam[8] = 0 / Math.pow(10, 12);
                initparam[9] = 0;
                initparam[10] = 0 / Math.pow(10, 6);
            }

//            // set values in the fit window
//            tfParama.setText(IJ.d2s(pixelsize * 1000 / objmag * binningX));	// XXX
//            tfParamw.setText(IJ.d2s(sigma * emlambda / NA, decformat));
//            tfParamw2.setText(IJ.d2s(sigma2 * emlambda2 / NA, decformat));
//            tfParamz.setText(IJ.d2s(sigmaZ * emlambda / NA, decformat));
//            tfParamz2.setText(IJ.d2s(sigmaZ2 * emlambda2 / NA, decformat));
            pixeldimx = (pixelsize * 1000 / objmag * binningX) / Math.pow(10, 9);
            pixeldimy = (pixelsize * 1000 / objmag * binningY) / Math.pow(10, 9);
            psfsize = (sigma * emlambda / NA) / Math.pow(10, 9);
//            IJ.log("psfsize: " + psfsize + ", NA: " + NA + ", emlambda: " + emlambda + ", sigma: " + sigma);
//            psfsize2 = (sigma2 * emlambda2 / NA) / Math.pow(10, 9);
            lsthickness = (sigmaZ * emlambda / NA) / Math.pow(10, 9);
//            lsthickness2 = (sigmaZ2 * emlambda2 / NA) / Math.pow(10, 9);
//
//            tfParamRx.setText(IJ.d2s(pixelsize * 1000 / objmag * cfXshift, decformat)); // set the value of rx
//            tfParamRy.setText(IJ.d2s(pixelsize * 1000 / objmag * cfYshift, decformat)); // set the value of ry
//            // Rz is set to 0 at the moment, no fit possible. Can be added later.
//            // read initial values from Fit window only when they are to be refreshed
//            if (tbFixPar.getText().equals("Free")) {
//                initparam[0] = Double.parseDouble(tfParamN.getText());
//                initparam[1] = Double.parseDouble(tfParamD.getText()) / Math.pow(10, 12);
//                initparam[2] = Double.parseDouble(tfParamVx.getText()) / Math.pow(10, 6);
//                initparam[3] = Double.parseDouble(tfParamVy.getText()) / Math.pow(10, 6);
//                initparam[4] = Double.parseDouble(tfParamG.getText());
//                initparam[5] = Double.parseDouble(tfParamF2.getText());
//                initparam[6] = Double.parseDouble(tfParamD2.getText()) / Math.pow(10, 12);
//                initparam[7] = Double.parseDouble(tfParamF3.getText());
//                initparam[8] = Double.parseDouble(tfParamD3.getText()) / Math.pow(10, 12);
//                initparam[9] = Double.parseDouble(tfParamFtrip.getText());
//                initparam[10] = Double.parseDouble(tfParamTtrip.getText()) / Math.pow(10, 6);
//            }
            // save the new settings in panelSettings
            System.arraycopy(newPanelSettings, 0, panelSettings, 0, noSettings);
            askOnRewrite = true;
            return true;
        } else {
            return false;
        }

    }

    // check whether an image was either loaded or an existing image assigned to the plugin for treatment
    public void checkImp() {
        if (setImp == false) {
            IJ.showMessage("Image not loaded or assigned.\nPlease use \"Load\" or \"Use\" button.");
            throw new RuntimeException("No Image loaded/assigned");
        }
    }

    public void obtainImage() {
        // define array for this picture which will be used to store the results; as these arrays should stay with the window they are defined here; 
        // other changeable arrays are defined in setParameters(); however, if parameters are changed the arrays are re-initialized.
        //  chanum = (int) (16 + (Integer.parseInt($CorrelQ) - 1) * 8 + 1); 	// value of chanum based on default Q and P, used for arrays initialization
        // NOTE: calculation of chanum has been revised. It is now based on values/settings on GUI. Instead of default values as the commented line above.
        // Otherwise, there may be a downstream error, ie. array out of bound error, example in plotCF function, cormode 2, fitacf array
        chanum = (int) (correlatorp + (correlatorq - 1) * correlatorp / 2 + 1);
        lagnum = (int) correlatorq;

        fitstart = 1;
        fitend = chanum - 1;
        initializeArrays();

        //initialize arrays for fitting
        paramfit = new boolean[noparam];
        paraminitval = new double[noparam];
        initparam = new double[noparam];

        //TODO:NNB
        /*
            nnb code
         */
    }

    public void printParam() {
        printlog("--fromImFCSCPUGPU class");
        printlog("use2imp: " + use2imp);
        printlog("selectedMode: " + Common.$selectedMode);
        printlog("fitModel: " + fitModel);
        printlog("CCF x: " + cfXDistance + ", CCF y: " + cfYDistance);
        printlog("width: " + width + ", height: " + height + ", frames: " + frames + ", firstframe: " + firstframe + ", lastframe: " + lastframe + ", frametime: " + frametime);
        printlog("polyorder: " + polyOrder + ",bleachCorMem: " + bleachCorMem + ", binningX: " + binningX + ", binningY: " + binningY + ", correlator p: " + correlatorp + ", correlator q: " + correlatorq + ", doFit: " + doFit + ", overlap: " + overlap);
        printlog("isgpupresent: " + Common.isgpupresent + ", runongpu: " + Common.RunLiveReadOutOnGPU + ", impmin: " + impmin);
        printlog("--fromImFCSCPUGPU class");

    }

    // determine minimum value in stack
    public int minDetermination(ImagePlus image) {
        int min;
        min = image.getStack().getProcessor(1).get(0, 0);
        for (int z = 1; z <= frames; z++) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (image.getStack().getProcessor(z).get(x, y) < min) {
                        min = image.getStack().getProcessor(z).get(x, y);
                    }
                }
            }
        }

        return min;
    }

    public void initializeArrays() {
        acf = new double[3][width][height][chanum];
        varacf = new double[3][width][height][chanum];
        sdacf = new double[3][width][height][chanum];
        fitacf = new double[3][width][height][chanum];
        res = new double[3][width][height][chanum];
        msd = new double[3][width][height][chanum];
        blocked = new double[3][width][height];
        aveacf = new double[3][chanum];
        fitaveacf = new double[chanum];
        varaveacf = new double[3][chanum];
        msdaveacf = new double[chanum];
        resaveacf = new double[chanum];
        ////initializedCCF(dccfMax, width, height);		//initialize array for dCCF
        //initializeFitres(3, width, height, noparam);	// initialize fitres and pixfiltered
        //initializepixvalid(3, width, height);			// initialize filtering mask
        //userThreshold = new boolean[5];
        //userThreshold[0] = false;			//user has not set any threshold
        //userThreshold[2] = false;						// tresholds haven't been applied on the data

        if ((plotCalibrationAmplitude || plotCalibrationDiffusion || plotCalibrationIntensity) && (DirectCapturePanel.Common.$selectedMode == "Calibration")) {
            // instantiate everysingle cycle
            if (calibParamListofList == null) {
                calibParamListofList = new ArrayList<ArrayList<Double>>(nocalpar);
                for (int i = 0; i < nocalpar; i++) {
                    calibParamListofList.add(new ArrayList<Double>());
                }
            }
        }

        if (impPara1 != null && impPara1.isVisible()) {		//close parameter and histograms windows if they exists
            impPara1.close();
        }
        if (histWin != null && !histWin.isClosed()) {
            histWin.close();
        }
    }

    // calculate reduced intensity traces for plotting average and use not more than 'nopit' (defined in setParameters()) points for a trace
    public void calcIntensityTrace(ImagePlus image, int ipx1, int ipy1, int ipx2, int ipy2, int initialframe, int finalframe) {
        // image: imp form which intensity will be taken
        // px1, py1, px2, py2: coordinates of pixels to be correlated
        // initialframe and finalframe provide the range of frames to be used
        int ave = (int) Math.floor((finalframe - initialframe + 1) / nopit); // calculate number of data points which are averaged
        int sum1;
        int sum2;
        int bckg1 = background; // artificially setted to zero to make sure we dont subrtract background when displaying intensity trance; does not affect ACF
        int bckg2 = background;	//needs to be adapted for background2 once available

        if (fitModel == "DC-FCCS") {
            bckg2 = background2;
        }

        for (int x = 0; x < nopit; x++) {
            sum1 = 0;	// initialize arrays with 0
            sum2 = 0;
            for (int i = 0; i < binningX; i++) {
                for (int k = 0; k < binningY; k++) {
                    for (int y = initialframe + x * ave; y <= initialframe + (x + 1) * ave - 1; y++) {
                        if (bgrloaded) { // if a background image is loaded, then subtract the mean of the background image for each pixel
                            bckg1 = (int) Math.round(bgrmean[ipx1 + i][ipy1 + k]);
                            bckg2 = (int) Math.round(bgrmean[ipx1 + i][ipy1 + k]);
                        }
                        sum1 += image.getStack().getProcessor(y).get(ipx1 + i, ipy1 + k) - bckg1;
                        sum2 += image.getStack().getProcessor(y).get(ipx2 + i, ipy2 + k) - bckg2;
                    }
                }
            }
            intTime[x] = frametime * (x + 0.5) * ave;
            intTrace1[x] = sum1 / ave;	// calculate average intensity for the 'ave' points
            intTrace2[x] = sum2 / ave;
        }
    }

    // calculate reduced intensity traces from 2 imageplus
    public void calcIntensityTrace(ImagePlus image, ImagePlus image2, int ipx1, int ipy1, int initialframe, int finalframe) {
        // image: imp form which intensity will be taken
        // image2: imp2 form which intensity will be taken
        // px1, py1: coordinates of pixel to be correlated
        // initialframe and finalframe provide the range of frames to be used

        int ave = (int) Math.floor((finalframe - initialframe + 1) / nopit); // calculate number of data points which are averaged
        int sum1;
        int sum2;
        int bckg1 = background; // artificially setted to zero to make sure we dont subrtract background when displaying intensity trance; does not affect ACF
        int bckg2 = background;	//needs to be adapted for background2 once available

        if (fitModel == "DC-FCCS") {
            bckg2 = background2;
        }

        for (int x = 0; x < nopit; x++) {
            sum1 = 0;	// initialize arrays with 0
            sum2 = 0;
            for (int i = 0; i < binningX; i++) {
                for (int k = 0; k < binningY; k++) {
                    for (int y = initialframe + x * ave; y <= initialframe + (x + 1) * ave - 1; y++) {
                        if (bgrloaded) { // if a background image is loaded, then subtract the mean of the background image for each pixel
                            bckg1 = (int) Math.round(bgrmean[ipx1 + i][ipy1 + k]);
                            bckg2 = (int) Math.round(bgrmean[ipx1 + i][ipy1 + k]);
                        }
                        sum1 += image.getStack().getProcessor(y).get(ipx1 + i, ipy1 + k) - bckg1;
                        sum2 += image2.getStack().getProcessor(y).get(ipx1 + i, ipy1 + k) - bckg2;
                    }
                }
            }
            intTime[x] = frametime * (x + 0.5) * ave;
            intTrace1[x] = sum1 / ave;	// calculate average intensity for the 'ave' points
            intTrace2[x] = sum2 / ave;
        }
    }

    //correlate one pixel with itself or two pixels with each other
    public void correlate(ImagePlus image, int px1, int py1, int px2, int py2, int kcf, int initialframe, int finalframe) {
        // image: the imp to be used
        // px1, py1, px2, py2: pixel cooredinates for pixel 1 and pixel 2 which are to be correalted
        // if px1 = px2 AND py1 = py2: then a autocorrelation is calculated
        // kcf (0, 1, or 2) determines whether ACF1, ACF2, or CCF is calculated
        // initialframe and finalframe provide the range of frames to be used for the correlation
        int num = (finalframe - initialframe + 1); 	// total number of frames to be correlated
        int numofsw; 							// number of sliding windows
        int swinitialframe; 					// if sliding window (bleach) correction is selected these are the initial and final frames of the sub-windows
        int swfinalframe;
        int pxm1;								// pixel coordinates on the binned grid used to store the output and map it to the parameter map
        int pym1;

        if (kcf == 1 && px1 == px2 && py1 == py2) {	// if red channel in the DC-FCCS mode, map the output to the corresponding green-channel pixels on the binned grid
            pxm1 = (int) (px1 - cfXDistance) / pixbinX;
            pym1 = (int) (py1 - cfYDistance) / pixbinY;
        } else {										// otherwise map to the pixel on the pixel on the binned grid
            pxm1 = (int) px1 / pixbinX;
            pym1 = (int) py1 / pixbinY;
        }

        String $bcmode = (String) bleachCorMem;

        // Sliding window is not selected, correlate the full intensity trace
        datac = new double[2][num + 1];							// get the intensity data for the correlation 
        datac[0] = getIntensity(image, px1, py1, 1, initialframe, finalframe);		// getIntensity for first pixel; performs a bleach correction if indicated in the panel
        if (px1 != px2 || py1 != py2) {						// if the two pixels are not equal (i.e. for a cross-correlation)
            datac[1] = getIntensity(image, px2, py2, 2, initialframe, finalframe);	// getIntensity for second pixel
        } else {

            datac[1] = datac[0];			// otherwise perform an autocorrelation
        }

        Map result;
        result = correlator(datac, initialframe, finalframe);		// correlate the data
        acf[kcf][pxm1][pym1] = (double[]) result.get("corav");			// acf
        varacf[kcf][pxm1][pym1] = (double[]) result.get("blockvar");		// variance of the ACF; blocked
        sdacf[kcf][pxm1][pym1] = (double[]) result.get("blocksd");		// standard deviation of the ACF; blocked
        currentCovmats = (double[][]) result.get("covmats");

        blocked[kcf][pxm1][pym1] = blockIndS;			// store whether blocking worked successfully for the pixel

        // calculate MSD if switched on
        /*
        if (doMSD) {
            if (!MSDmode) { // 2D if MSDmode is false, otherwise 3D
                msd[kcf][pxm1][pym1] = correlationToMSD(acf[kcf][pxm1][pym1], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6));
            } else {
                msd[kcf][pxm1][pym1] = correlationToMSD3D(acf[kcf][pxm1][pym1], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6), lsthickness * Math.pow(10, 6));
            }
        }
         */
    }

    //correlate two imageplus (allpixels)
    public void correlate(ImagePlus image, ImagePlus image2, int px1, int py1, int kcf, int initialframe, int finalframe) {
        // image: the imp to be used
        // image2: the imp2 to be used
        // px1, py1: pixel cooredinates to be correalted
        // kcf (0, 1, or 2) determines whether ACF1, ACF2, or CCF is calculated
        // initialframe and finalframe provide the range of frames to be used for the correlation
        int num = (finalframe - initialframe + 1); 	// total number of frames to be correlated
        int numofsw; 							// number of sliding windows
        int swinitialframe; 					// if sliding window (bleach) correction is selected these are the initial and final frames of the sub-windows
        int swfinalframe;
        int pxm1;								// pixel coordinates on the binned grid used to store the output and map it to the parameter map
        int pym1;

        pxm1 = (int) px1 / pixbinX;
        pym1 = (int) py1 / pixbinY;

        String $bcmode = (String) bleachCorMem;

        // Sliding window is not selected, correlate the full intensity trace
        // Sliding window is not selected, correlate the full intensity trace
        datac = new double[2][num + 1];
        // get the intensity data for the correlation
        if (kcf == 0) { //perform autocorrelation on green channel
            datac[0] = getIntensity(image, px1, py1, 1, initialframe, finalframe, 1);
            datac[1] = datac[0];
        }
        if (kcf == 1) {//perform autocorrelation on red channel
            datac[0] = getIntensity(image2, px1, py1, 2, initialframe, finalframe, 2);
            datac[1] = datac[0];
        }
        if (kcf == 2) {//perform ccf
            datac[0] = getIntensity(image, px1, py1, 1, initialframe, finalframe, 1);
            datac[1] = getIntensity(image2, px1, py1, 2, initialframe, finalframe, 2);
        }

        Map result;
        result = correlator(datac, initialframe, finalframe);		// correlate the data
        acf[kcf][pxm1][pym1] = (double[]) result.get("corav");			// acf
        varacf[kcf][pxm1][pym1] = (double[]) result.get("blockvar");		// variance of the ACF; blocked
        sdacf[kcf][pxm1][pym1] = (double[]) result.get("blocksd");		// standard deviation of the ACF; blocked
        currentCovmats = (double[][]) result.get("covmats");

        blocked[kcf][pxm1][pym1] = blockIndS;			// store whether blocking worked successfully for the pixel

        // calculate MSD if switched on
        /*
        if (doMSD) {
            if (!MSDmode) { // 2D if MSDmode is false, otherwise 3D
                msd[kcf][pxm1][pym1] = correlationToMSD(acf[kcf][pxm1][pym1], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6));
            } else {
                msd[kcf][pxm1][pym1] = correlationToMSD3D(acf[kcf][pxm1][pym1], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6), lsthickness * Math.pow(10, 6));
            }
        }
         */
    }

    // correlator calculates correlation functions
    public Map correlator(double[][] intcor, int initialframe, int finalframe) {
        // intcor contains the array of intensity values to be correlated for pixels 1 and 2
        // initialframe and finalframe provide the range of frames to be used for the correlation
        int num = (finalframe - initialframe + 1); 			// total number of frames to be correlated
        int blockIndex;									// index at which optimal blocking is reached; if it fails maximal blocking is used

        blockIndex = blockTransform(intcor, num, 1);			// perform blocking on the first channel to determine when intensity bins are independent

        Map result;
        result = calculateCF(intcor, num, blockIndex, 1);			// perform optimal blocking and return the CF, SD and covariance matrix

        return result;
    }

    // calculate the standard deviation by blocking
    public Map calculateCF(double[][] intcor, int num, int ind, int blocklag) {
        // intcor is the array of intensity values for the two traces which are correlated
        // num is the number of frames which are correlated
        // ind is the blockindex, at which the SD has converged, previously found in blockSD()
        // blocklag defines for which lag the blocking will be done; typically we use the smalles, i.e. 1
        int numbin = num;		// number of data points when they are binned
        int del;				// delay or correlation time expressed in lags
        int currentIncrement;
        int ctbin = 0;			// count how often the data was binned
        int binct;
        int pnum = (int) Math.floor(mtab[chanum - 1] / Math.pow(2, Math.max(ind - Math.log(samp[chanum - 1]) / Math.log(2), 0)));	// minimum number of prodcuts given the used correlator structure and the blockIndex ind
        int[] prodnum = new int[chanum];
        double sumprod;		// sum of all intensity products; divide by num to get the average <i(n)i(n+del)>
        double sumprod2;	// sum of all intensity products squared; divide by num to get the average <(i(n)i(n+del))^2>
        double[] directm = new double[chanum];		// direct monitor required for ACF normalization
        double[] delayedm = new double[chanum];	// delayed monitor required for ACF normalization
        double[] blockvar;
        double[] blocksd;
        double[][] intblock;
        double[][] prod = new double[chanum][num];
        double[] corav = new double[chanum];
        double[] mcov = new double[chanum];
        double[] diagcovmat = new double[chanum];
        double[][] covmat = new double[chanum][chanum];
        double[][] covmats = new double[chanum - 1][chanum - 1];	//the final results does not contain information about the zero lagtime channel
        double[][] cormat = new double[chanum][chanum];
        double[][] denomshrink = new double[chanum][chanum];
        double numerator = 0;
        double denominator = 0;
        double median = 0;
        double lamvar;
        double lamcov;

        intblock = new double[2][num];
        blockvar = new double[chanum];
        blocksd = new double[chanum];

        for (int x = 0; x < numbin; x++) {	//re-initialize the intensities
            intblock[0][x] = intcor[0][x + 1];
            intblock[1][x] = intcor[1][x + 1];
        }

        currentIncrement = 1;		// at the moment we always do blocking for smallest lag
        blocksd[0] = 0;				// we do not calcualte the SD for the 0 lagtime as it is not used for fitting (shot noise)

        for (int x = 0; x < chanum; x++) {						// run over all channels except the 0 lag time

            if (currentIncrement != samp[x]) {					// check whether the channel width has changed
                numbin = (int) Math.floor(numbin / 2);				// if yes, correct the number of actual data points
                currentIncrement = (int) samp[x];					// set the currentIncrement accordingly
                ctbin++;											// count how often the data was binned
                for (int y = 0; y < numbin; y++) {					// and bin the data according to the width of the current channel
                    intblock[0][y] = (intblock[0][2 * y] + intblock[0][2 * y + 1]);
                    intblock[1][y] = (intblock[1][2 * y] + intblock[1][2 * y + 1]);
                }
            }

            del = lag[x] / currentIncrement;							// calculate the delay, i.e. the correlation time ...
            prodnum[x] = numbin - del;								// and the number of products for that delay; //(int) (mtab[chanum-1]*(samp[chanum-1]/samp[x]));//IJ.log(Double.toString(prodnum[x])); 
            for (int y = 0; y < prodnum[x]; y++) {				// calculate the ...
                directm[x] += intblock[0][y];						// direct and ...
                delayedm[x] += intblock[1][y + del];				// delayed monitor
            }
            directm[x] /= prodnum[x];		// calculate average of direct and delayed monitor, i.e. the average intensity <n(0)> and <n(tau)>
            delayedm[x] /= prodnum[x];

            sumprod = 0;
            sumprod2 = 0;

            for (int y = 0; y < prodnum[x]; y++) {					// calculate the correlation
                prod[x][y] = intblock[0][y] * intblock[1][y + del] - delayedm[x] * intblock[0][y] - directm[x] * intblock[1][y + del] + delayedm[x] * directm[x];
                sumprod += prod[x][y];								// calculate the sum of prod, i.e. the raw correlation value ...
                sumprod2 += Math.pow(prod[x][y], 2);				// ... and the sum of the squares
            }

            corav[x] = sumprod / (prodnum[x] * directm[x] * delayedm[x]);	// calculate the ACF, i.e. the mean for the later calculations of the variance-covariance matrix

            binct = ind - ctbin; // determine whether data needs to be further binned or is already exceeding the blocking number
            sumprod = 0;
            sumprod2 = 0;

            for (int y = 1; y <= binct; y++) {					// bin the data until block time is reached
                prodnum[x] = (int) Math.floor(prodnum[x] / 2);		// for each binning the number of data points is halfed
                for (int z = 0; z < prodnum[x]; z++) {			// do the binning and divide by 2 so that average value does not change
                    prod[x][z] = (prod[x][2 * z] + prod[x][2 * z + 1]) / 2;
                }
            }

            prodnum[x] = pnum;										// use only the minimal number of products to achieve a symmetric variance matrix
            for (int z = 0; z < prodnum[x]; z++) {
                sumprod += prod[x][z];								// calculate the sum of prod, i.e. the raw correlation value ...
                sumprod2 += Math.pow(prod[x][z], 2);				// ... and the sum of the squares
            }

            blockvar[x] = (sumprod2 / prodnum[x] - Math.pow(sumprod / prodnum[x], 2)) / ((prodnum[x] - 1) * Math.pow(directm[x] * delayedm[x], 2));	// variance after blocking; extra division by prodnum to obtain SEM
            blocksd[x] = Math.sqrt(blockvar[x]);																									// standard deviation after blocking
        }

//        // if GLS is selected then calulate the regularized covariance matrix
//        if (GLS) {
//            // Calculate the mean of the products used for the variance-covariance matrix
//            for (int x = 1; x < chanum; x++) {
//                for (int z = 0; z < pnum; z++) {
//                    mcov[x] += prod[x][z] / (directm[x] * delayedm[x]);
//                }
//                mcov[x] /= pnum;		// normalize by the number of products
//            }
//
//            // Calculate the variance-covariance matrix
//            for (int x = 1; x < chanum; x++) {
//                for (int y = 1; y <= x; y++) {	// calculate only the upper triangular part as the matrix is symmetric
//                    for (int z = 0; z < pnum; z++) {
//                        covmat[x][y] += (prod[x][z] / (directm[x] * delayedm[x]) - mcov[x]) * (prod[y][z] / (directm[y] * delayedm[y]) - mcov[y]);
//                    }
//                    covmat[x][y] /= (pnum - 1);		// normalize by the number of products
//                    covmat[y][x] = covmat[x][y];	// lower triangular part is equal to upper triangular part
//                }
//            }
//
//            // Regularize variance-covariance matrix
//            // first determine the shrinkage weight for the variance 
//            for (int x = 1; x < chanum; x++) {			// get the variance (diagonal of covariance matrix) ...
//                diagcovmat[x] = covmat[x][x];
//            }
//
//            Arrays.sort(diagcovmat);						// ... and determine the median
//            double pos1 = Math.floor((diagcovmat.length - 1.0) / 2.0);
//            double pos2 = Math.ceil((diagcovmat.length - 1.0) / 2.0);
//            if (pos1 == pos2) {
//                median = diagcovmat[(int) pos1];
//            } else {
//                median = (diagcovmat[(int) pos1] + diagcovmat[(int) pos2]) / 2.0;
//            }
//
//            double tmpnum;									// determine the variance of the variance
//            for (int x = 1; x < chanum; x++) {
//                tmpnum = 0;
//                for (int z = 0; z < pnum; z++) {
//                    tmpnum += Math.pow((Math.pow(prod[x][z] / (directm[x] * delayedm[x]) - mcov[x], 2) - covmat[x][x]), 2);
//                }
//                tmpnum *= (pnum) / Math.pow(pnum - 1, 3);
//                numerator += tmpnum;
//                denominator += Math.pow(covmat[x][x] - median, 2);
//            }
//            lamvar = Math.min(1, numerator / denominator);		// shrinkage weight for the variance	
//            lamvar = Math.max(lamvar, 0);
//
//            // determine the shrinkage weight for the covariance
//            for (int x = 1; x < chanum; x++) {						// calculate the sample correlation matrix
//                for (int y = 1; y < chanum; y++) {
//                    cormat[x][y] = covmat[x][y] / Math.sqrt(covmat[x][x] * covmat[y][y]);
//                }
//            }
//
//            numerator = 0;
//            denominator = 0;
//            double cmx;										// tmp variables to simplify ... 
//            double cmy;										// ... in the loop
//            for (int x = 1; x < chanum; x++) {			// determine the variance of the covariance
//                tmpnum = 0;
//                for (int y = 1; y < x; y++) {				//sum only over the upper triangle as the matrix is symmetric
//                    for (int z = 0; z < pnum; z++) {
//                        cmx = (prod[x][z] / (directm[x] * delayedm[x]) - mcov[x]) / Math.sqrt(covmat[x][x]);
//                        cmy = (prod[y][z] / (directm[y] * delayedm[y]) - mcov[y]) / Math.sqrt(covmat[y][y]);
//                        tmpnum += Math.pow(cmx * cmy - cormat[x][y], 2);
//                    }
//                    tmpnum *= (pnum) / Math.pow(pnum - 1, 3);
//                    numerator += tmpnum;
//                    denominator += Math.pow(cormat[x][y], 2);		// sum of squares of off-diagonal elements of correlation matrix
//                }
//            }
//            lamcov = Math.min(1, numerator / denominator);		// shrinkage weight for the covariance
//            lamcov = Math.max(lamcov, 0);
//
//            // calculate the off-diagonal elements of the regularized variance-covariance matrix
//            for (int x = 1; x < chanum; x++) {		// do not include zero lagtime channel as we don't use it for fitting				
//                for (int y = 1; y < x; y++) {
//                    cmx = lamvar * median + (1 - lamvar) * covmat[x][x];
//                    cmy = lamvar * median + (1 - lamvar) * covmat[y][y];
//                    covmats[x - 1][y - 1] = (1 - lamcov) * cormat[x][y] * Math.sqrt(cmx * cmy) / pnum;
//                    covmats[y - 1][x - 1] = covmats[x - 1][y - 1];
//                }
//            }
//            for (int x = 1; x < chanum; x++) {	// diagonal elements of the regularized variance-covariance matrix
//                covmats[x - 1][x - 1] = (lamvar * median + (1 - lamvar) * covmat[x][x]) / pnum;
//            }
//
//            // Plot covariance matrix if selected
//            if (plotCovmats == true) {
//                if (impCov != null) {		// close covariance window if it exists
//                    impCov.close();
//                }
//                impCov = IJ.createImage($impCovTitle, "GRAY32", chanum - 1, chanum - 1, 1);
//                impCovIp = impCov.getProcessor();
//                for (int x = 1; x < chanum; x++) {		// calculate the covariance
//                    for (int y = 1; y < chanum; y++) {
//                        impCovIp.putPixelValue(x - 1, y - 1, covmats[x - 1][y - 1]);			// regularized var-cov matrix
//                        //impCovIp.putPixelValue(x, y, Math.sqrt(covmat[x][y]));	// non-regularized var-cov matrix
//                        //impCovIp.putPixelValue(x, y, Math.sqrt(cormat[x][y]));	// sample correlation matrix
//                    }
//                }
//
//                impCov.show();
//                IJ.run(impCov, "Spectrum", "");							// apply "Spectrum" LUT
//                IJ.run(impCov, "Enhance Contrast", "saturated=0.35");	// enhance contrast
//                impCovWin = impCov.getWindow();
//                impCovWin.setLocation(covPosX, covPosY);
//                IJ.run(impCov, "Set... ", "zoom=" + 200 + " x=" + 0 + " y=" + 0); //then zoom to fit within application
//                IJ.run("In [+]", ""); 	// This needs to be used since ImageJ 1.48v to set the window to the right size; 
//            }
//        } // end of if GLS selected statement
        Map<String, Object> map = new HashMap<>();
        if (GLS) {	//hand over either the correlation function corav or the actual function used to calcualte the covariance matrix; they differ only slightly
            map.put("corav", mcov);
        } else {
            map.put("corav", corav);
        }
        map.put("blockvar", blockvar);
        map.put("blocksd", blocksd);
        map.put("covmats", covmats);
        return map;

    }

    public int blockTransform(double[][] intcor, int num, int blocklag) {
        // intcor is the array of intensity values for the two traces which are correlated
        // num is the number of frames which are correlated
        // blocklag defines for which lag the blocking will be done; typically we use the smalles, i.e. 1
        int blocknum = (int) Math.floor(Math.log(mtab[blocklag]) / Math.log(2)) - 2;	// number of blocking operations that can be performed given blocklag
        int numbin = num;		// number of data points when they are binned
        int del;				// delay or correlation time expressed in lags
        int currentIncrement;
        int crwin = 2;			// 3 points that fit the error bar overlap criterion 
        double sumprod = 0.0;		// sum of all intensity products; divide by num to get the average <i(n)i(n+del)>
        double sumprod2 = 0.0;	// sum of all intensity products squared; divide by num to get the average <(i(n)i(n+del))^2>
        double directm = 0.0;		// direct monitor required for ACF normalization
        double delayedm = 0.0;	// delayed monitor required for ACF normalization
        double[][] intblock;
        double[] prod = new double[num];
        double[][] varblock;
        double[] upper;
        double[] lower;
        double[][] blockpoints;
        double[] blocksd;
        int[] crt;
        int[] cr12;			// do neighbouring points have overlapping error bars; together with crwin=2 this tests for three points that have overlapping erro bars
        int[] cr3;
        int[] diffpos;
        int last0 = 0;
        int ind = 0;
        double[] prodnum;
        double minblock;
        double maxblock;

        varblock = new double[3][blocknum];
        prodnum = new double[blocknum];
        intblock = new double[2][num];
        blocksd = new double[chanum];
        upper = new double[blocknum];
        lower = new double[blocknum];
        crt = new int[blocknum - 1];
        cr12 = new int[blocknum - 2];
        cr3 = new int[blocknum - 2];
        diffpos = new int[blocknum - 1];
        blockpoints = new double[3][3];

        for (int x = 0; x < numbin; x++) {
            intblock[0][x] = intcor[0][x];
            intblock[1][x] = intcor[1][x];
        }

        currentIncrement = blocklag;		// at the moment we always do blocking for smallest lag which is 1 but in general it can be used freely

        for (int x = 1; x < chanum; x++) {									// run over all channels
            if (currentIncrement != samp[x]) {								// check whether the channel width has changed
                currentIncrement = (int) samp[x];								// set the currentIncrement accordingly
                numbin = (int) Math.floor(numbin / 2);							// and correct the number of actual data points accordingly
                for (int y = 0; y < numbin; y++) {								// if yes, bin the data according to the width of the current channel
                    intblock[0][y] = (intblock[0][2 * y] + intblock[0][2 * y + 1]);
                    intblock[1][y] = (intblock[1][2 * y] + intblock[1][2 * y + 1]);
                }

            }

            if (x == blocklag) {										// if the channel number is equal to the blocklag ...
                del = lag[x] / currentIncrement;							// calculate the delay, i.e. the correlation time
                for (int y = 0; y < numbin - del; y++) {				// calculate the ...
                    directm += intblock[0][y];							// direct and ...
                    delayedm += intblock[1][y + del];					// delayed monitor
                }
                prodnum[0] = numbin - del; 								// number of correlation products
                directm /= prodnum[0];									// calculate average of direct and delayed monitor, 
                delayedm /= prodnum[0];									// i.e. the average intesity <n(0)> and <n(tau)>

                for (int y = 0; y < prodnum[0]; y++) {					// calculate the correlation
                    prod[y] = intblock[0][y] * intblock[1][y + del] - delayedm * intblock[0][y] - directm * intblock[1][y + del] + delayedm * directm;
                    sumprod += prod[y];									// calculate the sum of prod, i.e. the raw correlation value ...
                    sumprod2 += Math.pow(prod[y], 2);					// ... and the sum of the squares
                }

                varblock[0][0] = currentIncrement * frametime;			// the time of the block curve
                varblock[1][0] = (sumprod2 / prodnum[0] - Math.pow(sumprod / prodnum[0], 2)) / (prodnum[0] * Math.pow(directm * delayedm, 2));	// value of the block curve

                for (int y = 1; y < blocknum; y++) {					// perform blocking operations
                    prodnum[y] = (int) Math.floor(prodnum[y - 1] / 2);	// the number of samples for the blocking curve decreases by a factor 2 with every step
                    sumprod = 0;
                    sumprod2 = 0;
                    for (int z = 0; z < prodnum[y]; z++) {			// bin the correlation data and calculate the blocked values for the SD
                        prod[z] = (prod[2 * z] + prod[2 * z + 1]) / 2;
                        sumprod += prod[z];
                        sumprod2 += Math.pow(prod[z], 2);
                    }
                    varblock[0][y] = (currentIncrement * Math.pow(2, y)) * frametime;	// the time of the block curve
                    varblock[1][y] = (sumprod2 / prodnum[y] - Math.pow(sumprod / prodnum[y], 2)) / (prodnum[y] * Math.pow(directm * delayedm, 2));	// value of the block curve
                }
            }
        }

        for (int x = 0; x < blocknum; x++) {
            varblock[1][x] = Math.sqrt(varblock[1][x]);							// calculate the standard deviation
            varblock[2][x] = varblock[1][x] / Math.sqrt(2 * (prodnum[x] - 1));	// calculate the error 
            upper[x] = varblock[1][x] + varblock[2][x];							// upper and lower quartile
            lower[x] = varblock[1][x] - varblock[2][x];
        }

        // determine index where blocking criteria are fulfilled
        for (int x = 0; x < blocknum - 1; x++) {							// do neighboring points have overlapping error bars?
            if (upper[x] > lower[x + 1] && upper[x + 1] > lower[x]) {
                crt[x] = 1;
            }
        }

        for (int x = 0; x < blocknum - 2; x++) {							// do three adjacent points have overlapping error bars?
            if (crt[x] * crt[x + 1] == 1) {
                cr12[x] = 1;
            }
        }

        for (int x = 0; x < blocknum - 1; x++) {							// do neighboring points have a positive difference (increasing SD)?
            if (varblock[1][x + 1] - varblock[1][x] > 0) {
                diffpos[x] = 1;
            }
        }

        for (int x = 0; x < blocknum - 2; x++) {							// do three neighboring points monotonically increase?
            if (diffpos[x] * diffpos[x + 1] == 1) {
                cr3[x] = 1;
            }
        }

        for (int x = 0; x < blocknum - 2; x++) {							// find the last triple of points with monotonically increasing differences and non-overlapping error bars
            if ((cr3[x] == 1 && cr12[x] == 0)) {
                last0 = x;
            }
        }

        for (int x = 0; x <= last0; x++) {								// indices of two pairs that pass criterion 1 an 2
            cr12[x] = 0;
        }

        cr12[blocknum - 3] = 0;												// criterion 3, the last two points can't be part of the blocking triple
        cr12[blocknum - 4] = 0;

        for (int x = blocknum - 5; x > 0; x--) {							// index of triplet with overlapping error bars and after which no other triplet has a significant monotonic increase
            if (cr12[x] == 1) {											// or 4 increasing points
                ind = x + 1;												// take the middle of the three points as the blocking limit
            }
        }

        if (ind == 0) {													// if optimal blocking is not possible, use maximal blocking
            blockIndS = 0;
            if (blocknum - 3 > 0) {
                ind = blocknum - 3; 											// maximal blocking is performed for the 3rd last point in the blocking curve if that exists
            } else {
                ind = blocknum - 1;
            }
        } else {
            blockIndS = 1;
        }

        ind = (int) Math.max(ind, correlatorq - 1);				// block at least until maximum sample time

        // Plot the blocking curve if selected
        if (plotBlockingCurve == true) {
            minblock = varblock[1][0];
            maxblock = varblock[1][0];
            for (int x = 0; x < blocknum; x++) {
                if (varblock[1][x] > maxblock) {
                    maxblock = varblock[1][x];
                }
                if (varblock[1][x] < minblock) {
                    minblock = varblock[1][x];
                }
            }
            minblock *= 0.9;
            maxblock *= 1.1;

            Plot plot = new Plot("blocking", "x", "SD", varblock[0], varblock[1]);
            plot.setFrameSize(blockingWindowDimX, blockingWindowDimY);
            plot.setLogScaleX();
            plot.setLimits(varblock[0][0] / 2, 2 * varblock[0][blocknum - 1], minblock, maxblock);
            plot.setColor(java.awt.Color.BLUE);
            plot.setJustification(Plot.CENTER);
            plot.addPoints(varblock[0], varblock[1], varblock[2], Plot.CIRCLE);
            plot.draw();
            if (ind != 0) {								// Plot the points where blocking is succesful in red
                blockpoints[0][0] = varblock[0][ind - 1];
                blockpoints[1][0] = varblock[1][ind - 1];
                blockpoints[2][0] = varblock[2][ind - 1];
                blockpoints[0][1] = varblock[0][ind];
                blockpoints[1][1] = varblock[1][ind];
                blockpoints[2][1] = varblock[2][ind];
                blockpoints[0][2] = varblock[0][ind + 1];
                blockpoints[1][2] = varblock[1][ind + 1];
                blockpoints[2][2] = varblock[2][ind + 1];
                plot.setColor(java.awt.Color.RED);
                plot.addPoints(blockpoints[0], blockpoints[1], blockpoints[2], Plot.CIRCLE);
            }
            plot.draw();
            // either create a new plot window or plot within the existing window
            if (blockingWindow == null || blockingWindow.isClosed() == true) {
                blockingWindow = plot.show();
                blockingWindow.setLocation(blockingWindowPosX, blockingWindowPosY);
            } else {
                blockingWindow.drawPlot(plot);
            }
        }

        return ind;	// return the blockIndex
    }

    // get intensity data for correlate() and correct for bleaching if required; note that you should call calcIntensityTrace() before to obtain intTrace1 and 2
    public double[] getIntensity(ImagePlus image, int px, int py, int mode, int initialframe, int finalframe) {
        // image: imp form which intensity will be taken
        // px, py: coordinates of pixel within image
        // mode: determines whether intensity for pixel 1 or pixel 2 is read, and in case of DC-FCCS, 
        // whether background1 or background 2 is to be subtracted from the intensity trace
        // initialframe and finalframe provide the range of frames to be used
        int num = (finalframe - initialframe + 1);
        double[] intdat = new double[num + 1];
        double[] res = new double[5];
        int bckg;

        if (mode == 2 && fitModel == "DC-FCCS") {
            bckg = background2;
        } else {
            bckg = background;
        }

        for (int x = 1; x <= num; x++) {	//read data from all relevant pixels, depending on the selected frames and binning
            for (int i = 0; i < binningX; i++) {
                for (int k = 0; k < binningY; k++) {
                    if (bgrloaded) {
                        bckg = (int) Math.round(bgrmean[px + i][py + k]);
                    }
                    intdat[x] += image.getStack().getProcessor(initialframe + x - 1).get(px + i, py + k) - bckg;
                }
            }

        }

        String $bcmode = (String) bleachCorMem;	// perform single or double exponential bleach corrections if selected 

//        if ("Single Exp".equals($bcmode)) {
//            SingleExpFit efit = new SingleExpFit();
//            if (mode == 1) {
//                res = efit.doFit(intTrace1);			// note that the bleach correction is performed on the averaged intensity traces to make it faster
//            } else {									// while you may have 20,000 intensity points, intTrace1 and 2 contain only 1,000 points
//                res = efit.doFit(intTrace2);			// see definition in setParameters()
//            }
//            if (res[0] * res[1] != 0) {				// correct the full intensity trace if the fit was succesful
//                for (int x = 1; x <= num; x++) {
//                    intdat[x] = intdat[x] / Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2]) / (res[0] + res[2])));
//                }
//                if (mode == 1) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace1[x] = intTrace1[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])));
//                    }
//                }
//                if (mode == 2) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace2[x] = intTrace2[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])));
//                    }
//                }
//            } else {
//                IJ.log("Exponential Fit not successful for (" + px + ", " + py + ")");
//            }
//        }
//        if ("Double Exp".equals($bcmode)) {		// same as single exponential fit only for double exponential
//            DoubleExpFit defit = new DoubleExpFit();
//            if (mode == 1) {
//                res = defit.doFit(intTrace1);
//            } else {
//                res = defit.doFit(intTrace2);
//            }
//            if (res[0] * res[1] * res[2] * res[3] != 0) {
//                for (int x = 1; x <= num; x++) {
//                    intdat[x] = intdat[x] / Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2] * Math.exp(-frametime * (x + 0.5) / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2] * Math.exp(-frametime * (x + 0.5) / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                }
//                if (mode == 1) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace1[x] = intTrace1[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                    }
//                }
//                if (mode == 2) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace2[x] = intTrace2[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                    }
//                }
//            } else {
//                IJ.log("Double Exponential Fit not successful for (" + px + ", " + py + ")");
//            }
//        }
        if ("Polynomial".equals($bcmode)) {		//fitting with a polynomial of selected order
            PolynomFit polfit = new PolynomFit();
            double corfunc;
            int maxord = polyOrder;
            if (mode == 1) {
                res = polfit.doFit(intTrace1);			// note that the bleach correction is performed on the averaged intensity traces to make it faster
            } else {									// while you may have 20,000 intensity points, intTrace1 and 2 contain only 1,000 points
                res = polfit.doFit(intTrace2);			// see definition in setParameters()
            }

            for (int x = 1; x <= num; x++) {
                corfunc = 0;
                for (int i = 0; i <= maxord; i++) {
                    corfunc += res[i] * Math.pow(frametime * (x + 0.5), i);
                }
                intdat[x] = intdat[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
            }
            if (mode == 1) {
                for (int x = 0; x < nopit; x++) {
                    corfunc = 0;
                    for (int i = 0; i <= maxord; i++) {
                        corfunc += res[i] * Math.pow(intTime[x], i);
                    }
                    intTrace1[x] = intTrace1[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
                }
            }
            if (mode == 2) {
                for (int x = 0; x < nopit; x++) {
                    corfunc = 0;
                    for (int i = 0; i <= maxord; i++) {
                        corfunc += res[i] * Math.pow(intTime[x], i);
                    }
                    intTrace2[x] = intTrace2[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
                }
            }
        }

//        if ("Lin Segment".equals($bcmode)) {		// approximating bleaching by a partially linear function
//            int ave = (int) Math.floor((finalframe - initialframe + 1) / nopit); // number of points averaged in intTrace
//            int bcNopit = (int) num / slidingWindowLength; // number of linear segments
//            int bcAve = (int) Math.floor((finalframe - initialframe + 1) / bcNopit);
//            double[] bcTrace = new double[bcNopit];
//            int nf;
//            double bcInt0;
//            double sum;
//
//            for (int x = 0; x < bcNopit; x++) {		// calculating the average intensity in each segment
//                sum = 0;	// initialize arrays with 0
//                for (int y = 1 + x * bcAve; y < (x + 1) * bcAve; y++) {
//                    sum += intdat[y];
//                }
//                bcTrace[x] = sum / bcAve;
//            }
//
//            bcInt0 = bcTrace[0] + (bcTrace[0] - bcTrace[1]) / 2;	// Initial intensity obtained by extrapolating the line between average intensities in 1st and second segment to 0
//
//            for (int x = 1; x < (int) Math.floor(bcAve / 2); x++) {
//                intdat[x] = intdat[x] / Math.sqrt((bcTrace[0] + (bcTrace[0] - bcTrace[1]) / bcAve * (bcAve / 2 - x)) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[0] + (bcTrace[0] - bcTrace[1]) / bcAve * (bcAve / 2 - x)) / bcInt0));
//            }
//
//            for (int x = 1; x < bcNopit; x++) {
//                for (int y = 0; y < bcAve; y++) {
//                    nf = (x - 1) * bcAve + (int) Math.floor(bcAve / 2) + y;
//                    intdat[nf] = intdat[nf] / Math.sqrt((bcTrace[x - 1] + (bcTrace[x] - bcTrace[x - 1]) * y / bcAve) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[x - 1] + (bcTrace[x] - bcTrace[x - 1]) * y / bcAve) / bcInt0));
//                }
//            }
//
//            for (int x = (bcNopit - 1) * bcAve + (int) Math.floor(bcAve / 2); x <= num; x++) {
//                nf = x - (bcNopit - 1) * bcAve + (int) Math.floor(bcAve / 2) + bcAve;
//                intdat[x] = intdat[x] / Math.sqrt((bcTrace[bcNopit - 2] + (bcTrace[bcNopit - 1] - bcTrace[bcNopit - 2]) * nf / bcAve) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[bcNopit - 2] + (bcTrace[bcNopit - 1] - bcTrace[bcNopit - 2]) * nf / bcAve) / bcInt0));
//            }
//
//            if (mode == 1) {
//                for (int x = 0; x < nopit; x++) {
//                    intTrace1[x] = intdat[(int) (x + 0.5) * ave + 1];
//                }
//            }
//            if (mode == 2) {
//                for (int x = 0; x < nopit; x++) {
//                    intTrace2[x] = intdat[(int) (x + 0.5) * ave + 1];
//                }
//            }
//        }
        return (intdat);
    }

    // get intensity data for correlate() and correct for bleaching if required; note that you should call calcIntensityTrace() before to obtain intTrace1 and 2
    // 2 imageplus variation
    public double[] getIntensity(ImagePlus image, int px, int py, int mode, int initialframe, int finalframe, int image1or2) {
        // image: imp form which intensity will be taken
        // px, py: coordinates of pixel within image
        // mode: determines whether intensity for pixel 1 or pixel 2 is read, and in case of DC-FCCS, 
        // whether background1 or background 2 is to be subtracted from the intensity trace
        // initialframe and finalframe provide the range of frames to be used

        int num = (finalframe - initialframe + 1);
        double[] intdat = new double[num + 1];
        double[] res = new double[5];
        int bckg;

        if (image1or2 == 1) {
            bckg = background;
        } else {
            bckg = background2;
        }

        for (int x = 1; x <= num; x++) {	//read data from all relevant pixels, depending on the selected frames and binning
            for (int i = 0; i < binningX; i++) {
                for (int k = 0; k < binningY; k++) {
                    if (bgrloaded) {
                        bckg = (int) Math.round(bgrmean[px + i][py + k]);
                    }
                    intdat[x] += image.getStack().getProcessor(initialframe + x - 1).get(px + i, py + k) - bckg;
                }
            }

        }

        String $bcmode = (String) bleachCorMem;	// perform single or double exponential bleach corrections if selected 

//        if ("Single Exp".equals($bcmode)) {
//            SingleExpFit efit = new SingleExpFit();
//            if (mode == 1) {
//                res = efit.doFit(intTrace1);			// note that the bleach correction is performed on the averaged intensity traces to make it faster
//            } else {									// while you may have 20,000 intensity points, intTrace1 and 2 contain only 1,000 points
//                res = efit.doFit(intTrace2);			// see definition in setParameters()
//            }
//            if (res[0] * res[1] != 0) {				// correct the full intensity trace if the fit was succesful
//                for (int x = 1; x <= num; x++) {
//                    intdat[x] = intdat[x] / Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2]) / (res[0] + res[2])));
//                }
//                if (mode == 1) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace1[x] = intTrace1[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])));
//                    }
//                }
//                if (mode == 2) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace2[x] = intTrace2[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])) + (res[0] + res[2]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2]) / (res[0] + res[2])));
//                    }
//                }
//            } else {
//                IJ.log("Exponential Fit not successful for (" + px + ", " + py + ")");
//            }
//        }
//        if ("Double Exp".equals($bcmode)) {		// same as single exponential fit only for double exponential
//            DoubleExpFit defit = new DoubleExpFit();
//            if (mode == 1) {
//                res = defit.doFit(intTrace1);
//            } else {
//                res = defit.doFit(intTrace2);
//            }
//            if (res[0] * res[1] * res[2] * res[3] != 0) {
//                for (int x = 1; x <= num; x++) {
//                    intdat[x] = intdat[x] / Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2] * Math.exp(-frametime * (x + 0.5) / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-frametime * (x + 0.5) / res[1]) + res[2] * Math.exp(-frametime * (x + 0.5) / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                }
//                if (mode == 1) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace1[x] = intTrace1[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                    }
//                }
//                if (mode == 2) {
//                    for (int x = 0; x < nopit; x++) {
//                        intTrace2[x] = intTrace2[x] / Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])) + (res[0] + res[2] + res[4]) * (1 - Math.sqrt((res[0] * Math.exp(-intTime[x] / res[1]) + res[2] * Math.exp(-intTime[x] / res[3]) + res[4]) / (res[0] + res[2] + res[4])));
//                    }
//                }
//            } else {
//                IJ.log("Double Exponential Fit not successful for (" + px + ", " + py + ")");
//            }
//        }
        if ("Polynomial".equals($bcmode)) {		//fitting with a polynomial of selected order
            PolynomFit polfit = new PolynomFit();
            double corfunc;
            int maxord = polyOrder;
            if (mode == 1) {
                res = polfit.doFit(intTrace1);			// note that the bleach correction is performed on the averaged intensity traces to make it faster
            } else {									// while you may have 20,000 intensity points, intTrace1 and 2 contain only 1,000 points
                res = polfit.doFit(intTrace2);			// see definition in setParameters()
            }

            for (int x = 1; x <= num; x++) {
                corfunc = 0;
                for (int i = 0; i <= maxord; i++) {
                    corfunc += res[i] * Math.pow(frametime * (x + 0.5), i);
                }
                intdat[x] = intdat[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
            }
            if (mode == 1) {
                for (int x = 0; x < nopit; x++) {
                    corfunc = 0;
                    for (int i = 0; i <= maxord; i++) {
                        corfunc += res[i] * Math.pow(intTime[x], i);
                    }
                    intTrace1[x] = intTrace1[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
                }
            }
            if (mode == 2) {
                for (int x = 0; x < nopit; x++) {
                    corfunc = 0;
                    for (int i = 0; i <= maxord; i++) {
                        corfunc += res[i] * Math.pow(intTime[x], i);
                    }
                    intTrace2[x] = intTrace2[x] / Math.sqrt(corfunc / res[0]) + res[0] * (1 - Math.sqrt(corfunc / res[0]));
                }
            }
        }

//        if ("Lin Segment".equals($bcmode)) {		// approximating bleaching by a partially linear function
//            int ave = (int) Math.floor((finalframe - initialframe + 1) / nopit); // number of points averaged in intTrace
//            int bcNopit = (int) num / slidingWindowLength; // number of linear segments
//            int bcAve = (int) Math.floor((finalframe - initialframe + 1) / bcNopit);
//            double[] bcTrace = new double[bcNopit];
//            int nf;
//            double bcInt0;
//            double sum;
//
//            for (int x = 0; x < bcNopit; x++) {		// calculating the average intensity in each segment
//                sum = 0;	// initialize arrays with 0
//                for (int y = 1 + x * bcAve; y < (x + 1) * bcAve; y++) {
//                    sum += intdat[y];
//                }
//                bcTrace[x] = sum / bcAve;
//            }
//
//            bcInt0 = bcTrace[0] + (bcTrace[0] - bcTrace[1]) / 2;	// Initial intensity obtained by extrapolating the line between average intensities in 1st and second segment to 0
//
//            for (int x = 1; x < (int) Math.floor(bcAve / 2); x++) {
//                intdat[x] = intdat[x] / Math.sqrt((bcTrace[0] + (bcTrace[0] - bcTrace[1]) / bcAve * (bcAve / 2 - x)) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[0] + (bcTrace[0] - bcTrace[1]) / bcAve * (bcAve / 2 - x)) / bcInt0));
//            }
//
//            for (int x = 1; x < bcNopit; x++) {
//                for (int y = 0; y < bcAve; y++) {
//                    nf = (x - 1) * bcAve + (int) Math.floor(bcAve / 2) + y;
//                    intdat[nf] = intdat[nf] / Math.sqrt((bcTrace[x - 1] + (bcTrace[x] - bcTrace[x - 1]) * y / bcAve) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[x - 1] + (bcTrace[x] - bcTrace[x - 1]) * y / bcAve) / bcInt0));
//                }
//            }
//
//            for (int x = (bcNopit - 1) * bcAve + (int) Math.floor(bcAve / 2); x <= num; x++) {
//                nf = x - (bcNopit - 1) * bcAve + (int) Math.floor(bcAve / 2) + bcAve;
//                intdat[x] = intdat[x] / Math.sqrt((bcTrace[bcNopit - 2] + (bcTrace[bcNopit - 1] - bcTrace[bcNopit - 2]) * nf / bcAve) / bcInt0) + bcInt0 * (1 - Math.sqrt((bcTrace[bcNopit - 2] + (bcTrace[bcNopit - 1] - bcTrace[bcNopit - 2]) * nf / bcAve) / bcInt0));
//            }
//
//            if (mode == 1) {
//                for (int x = 0; x < nopit; x++) {
//                    intTrace1[x] = intdat[(int) (x + 0.5) * ave + 1];
//                }
//            }
//            if (mode == 2) {
//                for (int x = 0; x < nopit; x++) {
//                    intTrace2[x] = intdat[(int) (x + 0.5) * ave + 1];
//                }
//            }
//        }
        return (intdat);
    }

    // Polynomial fit for bleach correction
    public class PolynomFit extends AbstractCurveFitter {

        @Override
        protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
            final int len = points.size();
            final double[] target = new double[len];
            final double[] weights = new double[len];
            final double[] initialGuess = new double[polyOrder + 1];

            int i = 0;
            for (WeightedObservedPoint point : points) {
                target[i] = point.getY();
                weights[i] = point.getWeight();
                i += 1;
            }

            // initial guesses
            initialGuess[0] = target[len - 1];							// use the last point as offset estimate
            for (int j = 1; j <= polyOrder; j++) {						// use a straight line as the first estimate
                initialGuess[j] = 0;
            }

            ParametricUnivariateFunction function;
            function = new Polynomial();

            final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction(function, points);

            return new LeastSquaresBuilder().
                    maxEvaluations(Integer.MAX_VALUE).
                    maxIterations(Integer.MAX_VALUE).
                    start(initialGuess).
                    target(target).
                    weight(new DiagonalMatrix(weights)).
                    model(model.getModelFunction(), model.getModelFunctionJacobian()).
                    build();
        }

        public double[] doFit(double[] itrace) {
            PolynomFit fitter = new PolynomFit();
            ArrayList<WeightedObservedPoint> points = new ArrayList<>();
            int num = itrace.length;

            // Add points here
            for (int i = 0; i < num; i++) {
                WeightedObservedPoint point = new WeightedObservedPoint(1, intTime[i], itrace[i]);
                points.add(point);
            }

            double result[] = fitter.fit(points);
            return (result);
        }
    }

    // FCS model: 3D fit assuming 3 components and flow in x and y direction; this is the general fit formula; parameters can be set to 0 to obtain simpler models
    // the models and their derivation are provided on our website in CDF files (http://staff.science.nus.edu.sg/~chmwt/)
    class FCS_3p implements ParametricUnivariateFunction {
        // general parameters

        double pi = 3.14159265359;
        double sqrpi = Math.sqrt(pi);
        double ax = pixeldimx;
        double ay = pixeldimy;
        double s = psfsize;
        double sz = lsthickness;
        double psfz = 2 * emlambda / Math.pow(10, 9.0) * 1.33 / Math.pow(NA, 2.0); // size of PSF in axial direction
        //double szeff = Math.sqrt( 1 / ( Math.pow(sz, -2.0) + Math.pow(psfz, -2.0) ) ); // convolution of two Gaussians depending on illumination profile and detection PSF
        double szeff = sz;
        double rx = ax * cfXshift / binningX;
        double ry = ay * cfYshift / binningY;

        @Override
        public double[] gradient(double x, double[] params) {
            double[] pareq = new double[noparam];
            int num = 0;
            for (int i = 0; i < noparam; i++) {
                if (paramfit[i]) {
                    pareq[i] = params[num];
                    num++;
                } else {
                    pareq[i] = paraminitval[i];
                }
            }

            // note that x is used here as the time variable instead of t; that can be come confusing as x and y are used in the names for the paramaters to indicate spatial directions
            // pareq[0] = N
            // pareq[1] = D
            // pareq[2] = vx
            // pareq[3] = vy
            // pareq[4] = G
            // pareq[5] = F2
            // pareq[6] = D2
            // pareq[7] = F3
            // pareq[8] = D3
            // pareq[9] = Ftrip
            // pareq[10] = Ttrip
            //COMPONENT1
            // help variables, which are dependent on time, to write the full function
            double p0t = Math.sqrt(4 * pareq[1] * x + Math.pow(s, 2));
            double p1xt = ax + rx - pareq[2] * x;
            double p2xt = ax - rx + pareq[2] * x;
            double p3xt = rx - pareq[2] * x;
            double p4xt = 2 * Math.pow(ax, 2) + 3 * Math.pow(rx, 2) - 6 * x * rx * pareq[2] + 3 * Math.pow(x * pareq[2], 2);
            double p5xt = Math.pow(p3xt, 2) + Math.pow(p1xt, 2);
            double p6xt = Math.pow(p3xt, 2) + Math.pow(p2xt, 2);
            double p7xt = 2 * (Math.pow(ax, 2) + Math.pow(rx, 2) - 2 * x * rx * pareq[2] + Math.pow(x * pareq[2], 2));
            double p1yt = ay + ry - pareq[3] * x;
            double p2yt = ay - ry + pareq[3] * x;
            double p3yt = ry - pareq[3] * x;
            double p4yt = 2 * Math.pow(ay, 2) + 3 * Math.pow(ry, 2) - 6 * x * ry * pareq[3] + 3 * Math.pow(x * pareq[3], 2);
            double p5yt = Math.pow(p3yt, 2) + Math.pow(p1yt, 2);
            double p6yt = Math.pow(p3yt, 2) + Math.pow(p2yt, 2);
            double p7yt = 2 * (Math.pow(ay, 2) + Math.pow(ry, 2) - 2 * x * ry * pareq[3] + Math.pow(x * pareq[3], 2));
            double pexpxt = Math.exp(-Math.pow(p1xt / p0t, 2)) + Math.exp(-Math.pow(p2xt / p0t, 2)) - 2 * Math.exp(-Math.pow(p3xt / p0t, 2));
            double perfxt = p1xt * Erf.erf(p1xt / p0t) + p2xt * Erf.erf(p2xt / p0t) - 2 * p3xt * Erf.erf(p3xt / p0t);
            double dDpexpxt = 2 * Math.exp(-p4xt / Math.pow(p0t, 2)) * (Math.exp(p5xt / Math.pow(p0t, 2)) + Math.exp(p6xt / Math.pow(p0t, 2)) - 2 * Math.exp(p7xt / Math.pow(p0t, 2)));
            double dvxperfxt = (Erf.erf(p2xt / p0t) + 2 * Erf.erf(p3xt / p0t) - Erf.erf(p1xt / p0t)) * x;
            double pexpyt = Math.exp(-Math.pow(p1yt / p0t, 2)) + Math.exp(-Math.pow(p2yt / p0t, 2)) - 2 * Math.exp(-Math.pow(p3yt / p0t, 2));
            double dDpexpyt = 2 * Math.exp(-p4yt / Math.pow(p0t, 2)) * (Math.exp(p5yt / Math.pow(p0t, 2)) + Math.exp(p6yt / Math.pow(p0t, 2)) - 2 * Math.exp(p7yt / Math.pow(p0t, 2)));
            double dvyperfyt = (Erf.erf(p2yt / p0t) + 2 * Erf.erf(p3yt / p0t) - Erf.erf(p1yt / p0t)) * x;
            double perfyt = p1yt * Erf.erf(p1yt / p0t) + p2yt * Erf.erf(p2yt / p0t) - 2 * p3yt * Erf.erf(p3yt / p0t);

            //CF for the lateral dimension (x, y) and its derivative for D
            double plat = (p0t / sqrpi * pexpxt + perfxt) * (p0t / sqrpi * pexpyt + perfyt) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double dDplat = (1 / (sqrpi * p0t)) * (dDpexpyt * x * (p0t / sqrpi * pexpxt + perfxt) + dDpexpxt * x * (p0t / sqrpi * pexpyt + perfyt)) / (4 * Math.pow(ax * ay, 2) / fitobsvol);

            //CF for the axial dimension (z) and its derivative for D
            double pspim = 1 / Math.sqrt(1 + (4 * pareq[1] * x) / Math.pow(szeff, 2));
            double dDpspim = -4 * x / (2 * Math.pow(szeff, 2) * Math.pow(Math.sqrt(1 + (4 * pareq[1] * x) / Math.pow(szeff, 2)), 3));

            double acf1 = plat * pspim;

            //COMPONENT2
            // help variables, which are dependent on time, to write the full function
            double p0t2 = Math.sqrt(4 * pareq[6] * x + Math.pow(s, 2));
            double p1xt2 = ax + rx - pareq[2] * x;
            double p2xt2 = ax - rx + pareq[2] * x;
            double p3xt2 = rx - pareq[2] * x;
            double p4xt2 = 2 * Math.pow(ax, 2) + 3 * Math.pow(rx, 2) - 6 * x * rx * pareq[2] + 3 * Math.pow(x * pareq[2], 2);
            double p5xt2 = Math.pow(p3xt2, 2) + Math.pow(p1xt2, 2);
            double p6xt2 = Math.pow(p3xt2, 2) + Math.pow(p2xt2, 2);
            double p7xt2 = 2 * (Math.pow(ax, 2) + Math.pow(rx, 2) - 2 * x * rx * pareq[2] + Math.pow(x * pareq[2], 2));
            double p1yt2 = ay + ry - pareq[3] * x;
            double p2yt2 = ay - ry + pareq[3] * x;
            double p3yt2 = ry - pareq[3] * x;
            double p4yt2 = 2 * Math.pow(ay, 2) + 3 * Math.pow(ry, 2) - 6 * x * ry * pareq[3] + 3 * Math.pow(x * pareq[3], 2);
            double p5yt2 = Math.pow(p3yt2, 2) + Math.pow(p1yt2, 2);
            double p6yt2 = Math.pow(p3yt2, 2) + Math.pow(p2yt2, 2);
            double p7yt2 = 2 * (Math.pow(ay, 2) + Math.pow(ry, 2) - 2 * x * ry * pareq[3] + Math.pow(x * pareq[3], 2));
            double pexpxt2 = Math.exp(-Math.pow(p1xt2 / p0t2, 2)) + Math.exp(-Math.pow(p2xt2 / p0t2, 2)) - 2 * Math.exp(-Math.pow(p3xt2 / p0t2, 2));
            double perfxt2 = p1xt2 * Erf.erf(p1xt2 / p0t2) + p2xt2 * Erf.erf(p2xt2 / p0t2) - 2 * p3xt2 * Erf.erf(p3xt2 / p0t2);
            double dDpexpxt2 = 2 * Math.exp(-p4xt2 / Math.pow(p0t2, 2)) * (Math.exp(p5xt2 / Math.pow(p0t2, 2)) + Math.exp(p6xt2 / Math.pow(p0t2, 2)) - 2 * Math.exp(p7xt2 / Math.pow(p0t2, 2)));
            double dvxperfxt2 = (Erf.erf(p2xt2 / p0t2) + 2 * Erf.erf(p3xt2 / p0t2) - Erf.erf(p1xt2 / p0t2)) * x;
            double pexpyt2 = Math.exp(-Math.pow(p1yt2 / p0t2, 2)) + Math.exp(-Math.pow(p2yt2 / p0t2, 2)) - 2 * Math.exp(-Math.pow(p3yt2 / p0t2, 2));
            double dDpexpyt2 = 2 * Math.exp(-p4yt2 / Math.pow(p0t2, 2)) * (Math.exp(p5yt2 / Math.pow(p0t2, 2)) + Math.exp(p6yt2 / Math.pow(p0t2, 2)) - 2 * Math.exp(p7yt2 / Math.pow(p0t2, 2)));
            double dvyperfyt2 = (Erf.erf(p2yt2 / p0t2) + 2 * Erf.erf(p3yt2 / p0t2) - Erf.erf(p1yt2 / p0t2)) * x;
            double perfyt2 = p1yt2 * Erf.erf(p1yt2 / p0t2) + p2yt2 * Erf.erf(p2yt2 / p0t2) - 2 * p3yt2 * Erf.erf(p3yt2 / p0t2);

            //CF for the lateral dimension (x, y) and its derivative for D
            double plat2 = (p0t2 / sqrpi * pexpxt2 + perfxt2) * (p0t2 / sqrpi * pexpyt2 + perfyt2) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double dDplat2 = (1 / (sqrpi * p0t2)) * (dDpexpyt2 * x * (p0t2 / sqrpi * pexpxt2 + perfxt2) + dDpexpxt2 * x * (p0t2 / sqrpi * pexpyt2 + perfyt2)) / (4 * Math.pow(ax * ay, 2) / fitobsvol);

            //CF for the axial dimension (z) and its derivative for D
            double pspim2 = 1 / Math.sqrt(1 + (4 * pareq[6] * x) / Math.pow(szeff, 2));
            double dDpspim2 = -4 * x / (2 * Math.pow(szeff, 2) * Math.pow(Math.sqrt(1 + (4 * pareq[6] * x) / Math.pow(szeff, 2)), 3));

            double acf2 = plat2 * pspim2;

            //COMPONENT3
            // help variables, which are dependent on time, to write the full function
            double p0t3 = Math.sqrt(4 * pareq[8] * x + Math.pow(s, 2));
            double p1xt3 = ax + rx - pareq[2] * x;
            double p2xt3 = ax - rx + pareq[2] * x;
            double p3xt3 = rx - pareq[2] * x;
            double p4xt3 = 2 * Math.pow(ax, 2) + 3 * Math.pow(rx, 2) - 6 * x * rx * pareq[2] + 3 * Math.pow(x * pareq[2], 2);
            double p5xt3 = Math.pow(p3xt2, 2) + Math.pow(p1xt2, 2);
            double p6xt3 = Math.pow(p3xt2, 2) + Math.pow(p2xt2, 2);
            double p7xt3 = 2 * (Math.pow(ax, 2) + Math.pow(rx, 2) - 2 * x * rx * pareq[2] + Math.pow(x * pareq[2], 2));
            double p1yt3 = ay + ry - pareq[3] * x;
            double p2yt3 = ay - ry + pareq[3] * x;
            double p3yt3 = ry - pareq[3] * x;
            double p4yt3 = 2 * Math.pow(ay, 2) + 3 * Math.pow(ry, 2) - 6 * x * ry * pareq[3] + 3 * Math.pow(x * pareq[3], 2);
            double p5yt3 = Math.pow(p3yt3, 2) + Math.pow(p1yt3, 2);
            double p6yt3 = Math.pow(p3yt3, 2) + Math.pow(p2yt3, 2);
            double p7yt3 = 2 * (Math.pow(ay, 2) + Math.pow(ry, 2) - 2 * x * ry * pareq[3] + Math.pow(x * pareq[3], 2));
            double pexpxt3 = Math.exp(-Math.pow(p1xt3 / p0t3, 2)) + Math.exp(-Math.pow(p2xt3 / p0t3, 2)) - 2 * Math.exp(-Math.pow(p3xt3 / p0t3, 2));
            double perfxt3 = p1xt3 * Erf.erf(p1xt3 / p0t3) + p2xt3 * Erf.erf(p2xt3 / p0t3) - 2 * p3xt3 * Erf.erf(p3xt3 / p0t3);
            double dDpexpxt3 = 2 * Math.exp(-p4xt3 / Math.pow(p0t3, 2)) * (Math.exp(p5xt3 / Math.pow(p0t3, 2)) + Math.exp(p6xt3 / Math.pow(p0t3, 2)) - 2 * Math.exp(p7xt3 / Math.pow(p0t3, 2)));
            double dvxperfxt3 = (Erf.erf(p2xt3 / p0t3) + 2 * Erf.erf(p3xt3 / p0t3) - Erf.erf(p1xt3 / p0t3)) * x;
            double pexpyt3 = Math.exp(-Math.pow(p1yt3 / p0t3, 2)) + Math.exp(-Math.pow(p2yt3 / p0t3, 2)) - 2 * Math.exp(-Math.pow(p3yt3 / p0t3, 2));
            double dDpexpyt3 = 2 * Math.exp(-p4yt3 / Math.pow(p0t3, 2)) * (Math.exp(p5yt3 / Math.pow(p0t3, 2)) + Math.exp(p6yt3 / Math.pow(p0t3, 2)) - 2 * Math.exp(p7yt3 / Math.pow(p0t3, 2)));
            double dvyperfyt3 = (Erf.erf(p2yt3 / p0t3) + 2 * Erf.erf(p3yt3 / p0t3) - Erf.erf(p1yt3 / p0t3)) * x;
            double perfyt3 = p1yt3 * Erf.erf(p1yt3 / p0t3) + p2yt3 * Erf.erf(p2yt3 / p0t3) - 2 * p3yt3 * Erf.erf(p3yt3 / p0t3);

            // TRIPLET
            double triplet = 1 + pareq[9] / (1 - pareq[9]) * Math.exp(-x / pareq[10]);
            double dtripletFtrip = Math.exp(-x / pareq[10]) * (1 / (1 - pareq[9]) + pareq[9] / Math.pow(1 - pareq[9], 2));
            double dtripletTtrip = Math.exp(-x / pareq[10]) * (pareq[9] * x) / ((1 - pareq[9]) * Math.pow(pareq[10], 2));

            //CF for the lateral dimension (x, y) and its derivative for D
            double plat3 = (p0t3 / sqrpi * pexpxt3 + perfxt3) * (p0t3 / sqrpi * pexpyt3 + perfyt3) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double dDplat3 = (1 / (sqrpi * p0t3)) * (dDpexpyt3 * x * (p0t3 / sqrpi * pexpxt3 + perfxt3) + dDpexpxt3 * x * (p0t3 / sqrpi * pexpyt3 + perfyt3)) / (4 * Math.pow(ax * ay, 2) / fitobsvol);

            //CF for the axial dimension (z) and its derivative for D
            double pspim3 = 1 / Math.sqrt(1 + (4 * pareq[8] * x) / Math.pow(szeff, 2));
            double dDpspim3 = -4 * x / (2 * Math.pow(szeff, 2) * Math.pow(Math.sqrt(1 + (4 * pareq[8] * x) / Math.pow(szeff, 2)), 3));

            double acf3 = plat3 * pspim3;

            double pf1 = (1 - pareq[5] - pareq[7]) / (1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7]);
            double pf2 = (Math.pow(q2, 2) * pareq[5]) / (1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7]);
            double pf3 = (Math.pow(q3, 2) * pareq[7]) / (1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7]);
            double dfnom = Math.pow(1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7], 3);
            double df21 = 1 - pareq[5] - pareq[7] + q2 * pareq[5] - q3 * pareq[7] + 2 * q2 * pareq[7] - 2 * q2;
            double df22 = Math.pow(q2, 2) * (1 + pareq[5] - pareq[7] - q2 * pareq[5] + q3 * pareq[7]);
            double df23 = 2 * pareq[7] * Math.pow(q3, 2) * (1 - q2);
            double df31 = 1 - pareq[5] - pareq[7] - q2 * pareq[5] + 2 * q3 * pareq[5] - 2 * q3 + q3 * pareq[7];
            double df32 = 2 * pareq[5] * Math.pow(q2, 2) * (1 - q3);
            double df33 = Math.pow(q3, 2) * (1 - pareq[5] + pareq[7] + q2 * pareq[5] - q3 * pareq[7]);

            double pacf = (1 / pareq[0]) * ((1 - pareq[5] - pareq[7]) * acf1 + Math.pow(q2, 2) * pareq[5] * acf2 + Math.pow(q3, 2) * pareq[7] * acf3) / Math.pow(1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7], 2) * triplet + pareq[4];

            double[] grad = new double[]{
                (-1 / Math.pow(pareq[0], 2)) * (pf1 * acf1 + pf2 * acf2 + pf3 * acf3) * triplet,
                (1 / pareq[0]) * pf1 * (plat * dDpspim + pspim * dDplat),
                (1 / pareq[0]) * (pf1 * ((p0t / sqrpi * pexpyt + perfyt) * dvxperfxt) * pspim / (4 * Math.pow(ax * ay, 2) / fitobsvol) + pf2 * ((p0t2 / sqrpi * pexpyt2 + perfyt2) * dvxperfxt2) * pspim2 / (4 * Math.pow(ax * ay, 2) / fitobsvol) + pf3 * ((p0t3 / sqrpi * pexpyt3 + perfyt3) * dvxperfxt3) * pspim3 / (4 * Math.pow(ax * ay, 2) / fitobsvol)) * triplet,
                (1 / pareq[0]) * (pf1 * ((p0t / sqrpi * pexpxt + perfxt) * dvyperfyt) * pspim / (4 * Math.pow(ax * ay, 2) / fitobsvol) + pf2 * ((p0t2 / sqrpi * pexpxt2 + perfxt2) * dvyperfyt2) * pspim2 / (4 * Math.pow(ax * ay, 2) / fitobsvol) + pf3 * ((p0t3 / sqrpi * pexpxt3 + perfxt3) * dvyperfyt3) * pspim3 / (4 * Math.pow(ax * ay, 2) / fitobsvol)) * triplet,
                1,
                (1 / pareq[0]) * (1 / dfnom) * (df21 * acf1 + df22 * acf2 + df23 * acf3) * triplet,
                (1 / pareq[0]) * pf2 * (plat2 * dDpspim2 + pspim2 * dDplat2) * triplet,
                (1 / pareq[0]) * (1 / dfnom) * (df31 * acf1 + df32 * acf2 + df33 * acf3) * triplet,
                (1 / pareq[0]) * pf3 * (plat3 * dDpspim3 + pspim3 * dDplat3) * triplet,
                dtripletFtrip * pacf,
                dtripletTtrip * pacf
            };

            double[] gradret = new double[num]; // return the gradients of the fit model in respect to the fit parameters
            num = 0;
            for (int i = 0; i < noparam; i++) {
                if (paramfit[i] == true) {
                    gradret[num] = grad[i];
                    num++;
                }
            }

            return gradret;
        }

        @Override
        public double value(double x, double[] params) {
            double[] pareq = new double[noparam];
            int num = 0;
            for (int i = 0; i < noparam; i++) {
                if (paramfit[i]) {
                    pareq[i] = params[num];
                    num++;
                } else {
                    pareq[i] = paraminitval[i];
                }
            }

            // note that x is used here as the time variable instead of t; that can be come confusing as x and y are used in the names for the paramaters to indicate spatial directions
            // pareq[0] = N
            // pareq[1] = D
            // pareq[2] = vx
            // pareq[3] = vy
            // pareq[4] = G
            // pareq[5] = F2
            // pareq[6] = D2
            // pareq[7] = F3
            // pareq[8] = D3
            // pareq[9] = Ftrip
            // pareq[10] = Dtrip
            //q2 and q3, the brightness of the second and third components are fixed parameters and have been globaly defined; see prepareFit()
            // COMPONENT 1
            // help variables, which are dependent on time, to write the full function
            double p0t = Math.sqrt(4 * pareq[1] * x + Math.pow(s, 2));
            double p1xt = ax + rx - pareq[2] * x;
            double p2xt = ax - rx + pareq[2] * x;
            double p3xt = rx - pareq[2] * x;
            double p1yt = ay + ry - pareq[3] * x;
            double p2yt = ay - ry + pareq[3] * x;
            double p3yt = ry - pareq[3] * x;
            double pexpxt = Math.exp(-Math.pow(p1xt / p0t, 2)) + Math.exp(-Math.pow(p2xt / p0t, 2)) - 2 * Math.exp(-Math.pow(p3xt / p0t, 2));
            double perfxt = p1xt * Erf.erf(p1xt / p0t) + p2xt * Erf.erf(p2xt / p0t) - 2 * p3xt * Erf.erf(p3xt / p0t);
            double pexpyt = Math.exp(-Math.pow(p1yt / p0t, 2)) + Math.exp(-Math.pow(p2yt / p0t, 2)) - 2 * Math.exp(-Math.pow(p3yt / p0t, 2));
            double perfyt = p1yt * Erf.erf(p1yt / p0t) + p2yt * Erf.erf(p2yt / p0t) - 2 * p3yt * Erf.erf(p3yt / p0t);

            double pplane1 = (p0t / sqrpi * pexpxt + perfxt) * (p0t / sqrpi * pexpyt + perfyt) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double pspim1 = 1 / Math.sqrt(1 + (4 * pareq[1] * x) / Math.pow(szeff, 2));
            double acf1 = pplane1 * pspim1;

            // COMPONENT 2
            // help variables, which are dependent on time, to write the full function
            double p0t2 = Math.sqrt(4 * pareq[6] * x + Math.pow(s, 2));
            double p1xt2 = ax + rx - pareq[2] * x;
            double p2xt2 = ax - rx + pareq[2] * x;
            double p3xt2 = rx - pareq[2] * x;
            double p1yt2 = ay + ry - pareq[3] * x;
            double p2yt2 = ay - ry + pareq[3] * x;
            double p3yt2 = ry - pareq[3] * x;
            double pexpxt2 = Math.exp(-Math.pow(p1xt2 / p0t2, 2)) + Math.exp(-Math.pow(p2xt2 / p0t2, 2)) - 2 * Math.exp(-Math.pow(p3xt2 / p0t2, 2));
            double perfxt2 = p1xt * Erf.erf(p1xt2 / p0t2) + p2xt2 * Erf.erf(p2xt2 / p0t2) - 2 * p3xt2 * Erf.erf(p3xt2 / p0t2);
            double pexpyt2 = Math.exp(-Math.pow(p1yt2 / p0t2, 2)) + Math.exp(-Math.pow(p2yt2 / p0t2, 2)) - 2 * Math.exp(-Math.pow(p3yt2 / p0t2, 2));
            double perfyt2 = p1yt2 * Erf.erf(p1yt2 / p0t2) + p2yt2 * Erf.erf(p2yt2 / p0t2) - 2 * p3yt2 * Erf.erf(p3yt2 / p0t2);

            double pplane2 = (p0t2 / sqrpi * pexpxt2 + perfxt2) * (p0t2 / sqrpi * pexpyt2 + perfyt2) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double pspim2 = 1 / Math.sqrt(1 + (4 * pareq[6] * x) / Math.pow(szeff, 2));
            double acf2 = pplane2 * pspim2;

            // COMPONENT 3
            // help variables, which are dependent on time, to write the full function
            double p0t3 = Math.sqrt(4 * pareq[8] * x + Math.pow(s, 2));
            double p1xt3 = ax + rx - pareq[2] * x;
            double p2xt3 = ax - rx + pareq[2] * x;
            double p3xt3 = rx - pareq[2] * x;
            double p1yt3 = ay + ry - pareq[3] * x;
            double p2yt3 = ay - ry + pareq[3] * x;
            double p3yt3 = ry - pareq[3] * x;
            double pexpxt3 = Math.exp(-Math.pow(p1xt3 / p0t3, 2)) + Math.exp(-Math.pow(p2xt3 / p0t3, 2)) - 2 * Math.exp(-Math.pow(p3xt3 / p0t3, 2));
            double perfxt3 = p1xt * Erf.erf(p1xt3 / p0t3) + p2xt3 * Erf.erf(p2xt3 / p0t3) - 2 * p3xt3 * Erf.erf(p3xt3 / p0t3);
            double pexpyt3 = Math.exp(-Math.pow(p1yt3 / p0t3, 2)) + Math.exp(-Math.pow(p2yt3 / p0t3, 2)) - 2 * Math.exp(-Math.pow(p3yt3 / p0t3, 2));
            double perfyt3 = p1yt3 * Erf.erf(p1yt3 / p0t3) + p2yt3 * Erf.erf(p2yt3 / p0t3) - 2 * p3yt3 * Erf.erf(p3yt3 / p0t3);

            double pplane3 = (p0t3 / sqrpi * pexpxt3 + perfxt3) * (p0t3 / sqrpi * pexpyt3 + perfyt3) / (4 * Math.pow(ax * ay, 2) / fitobsvol);
            double pspim3 = 1 / Math.sqrt(1 + (4 * pareq[8] * x) / Math.pow(szeff, 2));
            double acf3 = pplane3 * pspim3;

            // TRIPLET
            double triplet = 1 + pareq[9] / (1 - pareq[9]) * Math.exp(-x / pareq[10]);

            return (1 / pareq[0]) * ((1 - pareq[5] - pareq[7]) * acf1 + Math.pow(q2, 2) * pareq[5] * acf2 + Math.pow(q3, 2) * pareq[7] * acf3) / Math.pow(1 - pareq[5] - pareq[7] + q2 * pareq[5] + q3 * pareq[7], 2) * triplet + pareq[4];
        }
    }

    // polynomial for bleach correction
    class Polynomial implements ParametricUnivariateFunction {

        @Override
        public double[] gradient(double x, double[] params) {
            int maxord = polyOrder;

            double[] grad = new double[maxord + 1];
            for (int i = 0; i <= maxord; i++) {
                grad[i] = Math.pow(x, i);
            }

            return grad;
        }

        @Override
        public double value(double x, double[] params) {
            int maxord = polyOrder;
            double[] A = new double[maxord + 1];
            for (int i = 0; i <= maxord; i++) {
                A[i] = params[i];
            }

            double val = 0;
            for (int i = 0; i <= maxord; i++) {
                val += A[i] * Math.pow(x, i);
            }

            return val;
        }
    }

    // calculate mean square displacement from ITIR-FCS correlation function
    public double[] correlationToMSD(double[] corrfunc, double psize, double psfwidth) {
        // corrfunc: correlation fucntion to be inverted
        // pszie: pixel size in um
        // psfwidth: pixelwidth in um
        double pi = 3.14159265359;
        double a = -17.0 * 1260.0 / 29.0 / 180.0;
        double b = 1260.0 / 29.0 / 3.0;
        double c = -1260.0 / 29.0;
        double d;
        int cutoffInd = chanum - 1;
        double[] msdarray = new double[chanum]; // array is defined as full length even if it is not completely used

        for (int i = chanum - 1; i > 1; i--) {
            if (corrfunc[i] / corrfunc[1] < 0.1) {
                cutoffInd = i;
            }
        }

        for (int i = 1; i < cutoffInd; i++) {
            d = pi * corrfunc[i] / corrfunc[1] / (obsvolFCS_ST2D1p(2) * Math.pow(10, 12)) * Math.pow(psize, 2) * 1260.0 / 29.0;
            double[] g = quarticFunction(a, b, c, d);
            msdarray[i] = (psize * psize / (g[1]) - psfwidth * psfwidth);
        }

        for (int i = 0; i < msdarray.length; i++) {
            if (Double.isNaN(msdarray[i])) {
                msdarray[i] = 0;
            }
        }
        return msdarray;
    }

    // calculate mean square displacement from SPIM correlation function
    public double[] correlationToMSD3D(double[] corrfunc, double psize, double psfwidth, double psfwidthz) {
        // corrfunc: correlation function to be inverted
        // psize: pixel size in um
        // psfwidth: psf width in um
        // psfwidthz: z psf width in um	

        int cutoffInd = chanum - 1;
        double[] msdarray = new double[chanum]; // array is defined as full length even if it is not completely used

        msdarray[0] = 0.0;
        msdarray[1] = 0.0;

        for (int i = chanum - 1; i > 1; i--) {
            if (corrfunc[i] / corrfunc[1] < 0.1) {
                cutoffInd = i;
            }
        }

        final double temp1_msd_3d = corrfunc[1];
        final double temp_psfwidth = psfwidth;
        final double temp_psfwidthz = psfwidthz;
        final double temp_psize = psize;

        for (int i = 2; i < cutoffInd; i++) {
            final double temp_msd_3d = corrfunc[i];
            UnivariateFunction f = (double x) -> {
                double pi = 3.14159265359;
                double sqrpi = Math.sqrt(pi);

                double p = 1 - Math.pow(temp_psfwidth, 2.0) / Math.pow(temp_psfwidthz, 2.0);
                double q = Math.pow(temp_psize, 2.0) / Math.pow(temp_psfwidthz, 2.0);
                double p1 = -15.0 * Math.pow(p, 2.0) + 20.0 * p * q + 2.0 * Math.pow(q, 2.0);
                double p2 = 945.0 * Math.pow(p, 3.0) - 630.0 * Math.pow(p, 2.0) * q;

                double coef_0 = -1.0;
                double coef_2 = 1 / 63.0 / q * (p2 - 420.0 * p * Math.pow(q, 2.0) - 64.0 * Math.pow(q, 3.0)) / p1;
                double coef_3 = -1 / pi / Math.sqrt(q);
                double coef_4 = 1 / 7560.0 / Math.pow(q, 2.0) / p1 * (14175.0 * Math.pow(p, 4.0) + 37800.0 * Math.pow(p, 3.0) * q - 30240.0 * Math.pow(p, 2.0) * Math.pow(q, 2.0) - 3840.0 * p * Math.pow(q, 3.0) - 1132.0 * Math.pow(q, 4.0));
                double coef_5 = 1 / 126.0 / pi / Math.pow(q, 1.5) * (p2 + 126.0 * p * Math.pow(q, 2.0) - 44.0 * Math.pow(q, 3.0)) / p1;

                double corrp = temp_msd_3d * sqrpi * temp_psize * temp_psize * temp_psfwidthz / (obsvolFCS_ST2D1p(3) * Math.sqrt(pi) * temp_psfwidthz * Math.pow(10, 12)) / temp1_msd_3d;
                return coef_5 * Math.pow(x, 5.0) - coef_4 * corrp * Math.pow(x, 4.0) + coef_3 * Math.pow(x, 3.0) - coef_2 * corrp * Math.pow(x, 2.0) - coef_0 * corrp;
            };

            try {
                UnivariateSolver solver = new BrentSolver();
                double result = solver.solve(100, f, 0, psize / psfwidth);
                msdarray[i] = 1.5 * (Math.pow(psize, 2.0) / Math.pow(result, 2.0) - Math.pow(psfwidth, 2.0));
            } catch (RuntimeException ex) {
                IJ.log(ex.getMessage());
                throw ex;
            }

        }

        for (int i = 0; i < msdarray.length; i++) {
            if (Double.isNaN(msdarray[i])) {
                msdarray[i] = 0;
            }
        }

        return msdarray;
    }

    //Fourth order polynomial solver
    public double[] quarticFunction(double a, double b, double c, double d) {
        double AA = -b;
        double BB = a * c - 4.0 * d;
        double CC = -a * a * d + 4.0 * b * d - c * c;
        double y1 = cubicrootFunction(AA, BB, CC);
        double RR = Math.sqrt(a * a / 4.0 - b + y1);
        double DD, EE, x1, x2, x3, x4;

        if (RR == 0) {
            DD = Math.sqrt(3.0 * a * a / 4.0 - 2.0 * b + 2.0 * Math.sqrt(y1 * y1 - 4.0 * d));
            EE = Math.sqrt(3.0 * a * a / 4.0 - 2.0 * b - 2.0 * Math.sqrt(y1 * y1 - 4.0 * d));
        } else {
            DD = Math.sqrt(3.0 * a * a / 4.0 - RR * RR - 2.0 * b + (4.0 * a * b - 8.0 * c - a * a * a) / (4.0 * RR));
            EE = Math.sqrt(3.0 * a * a / 4.0 - RR * RR - 2.0 * b - (4.0 * a * b - 8.0 * c - a * a * a) / (4.0 * RR));
        }

        a = a / 4.0;
        RR = RR / 2.0;
        DD = DD / 2.0;
        x1 = -a + RR + DD;
        x2 = -a + RR - DD;
        EE = EE / 2.0;
        x3 = -a - RR + EE;
        x4 = -a - RR - EE;
        double[] returningarray = {x1, x2, x3, x4};
        return returningarray;
    }

    //This calculates the solution to a third order polynomial which is obtained from the fourth order polynomial. The cubic which appears is called resolvent cubic equation
    public double cubicrootFunction(double a, double b, double c) {
        double pi = 3.14159265359;
        double root = 0.0;
        double qq, pp;
        double Qa, y1 = 0.0, y2, y3 = 0.0;
        double AA = 0.0, BB = 0.0, sQ = 0.0;
        double cosA, alpha;

        pp = b - (a * a) / 3.0;
        qq = c + 2.0 * (a / 3.0) * (a / 3.0) * (a / 3.0) - a * b / 3.0;
        Qa = (pp / 3.0) * (pp / 3.0) * (pp / 3.0) + (qq / 2.0) * (qq / 2.0);
        a = a / 3.0;
        if (Qa >= 0.0) {
            sQ = Math.sqrt(Qa);
            if (sQ > qq / 2.0) {
                AA = Math.pow(sQ - qq / 2.0, 0.33333333333333333);
            } else {
                AA = -Math.pow(qq / 2.0 - sQ, 0.33333333333333333);
            }
            if (-sQ - qq / 2.0 > 0.0) {
                BB = Math.pow(-sQ - qq / 2.0, 0.33333333333333333);
            } else {
                BB = -Math.pow(sQ + qq / 2.0, 0.33333333333333333);
            }
            y1 = AA + BB - a;
        } else {
            pp = pp / 3;
            cosA = -qq / (2 * Math.sqrt(-pp * pp * pp));
            alpha = Math.acos(cosA) / 3.0;
            if (alpha == 0) {
                alpha = 2.0 * pi / 3.0;
            }
            y1 = 2 * Math.sqrt(-pp) * Math.cos(alpha) - a;
        }
        return y1;
    }

    // data fitting
    public void prepareFit() {

        String $fitmode = fitModel;

        // always start with the same initial values; the initial values are first defined in createImFCSFit(); 
        // subsequently they are read in setParameters() from the fit window. One thus should set the prameters to 
        // realistic values, otherwise the fit might not converge and hang the plugin for a long time
        paraminitval[0] = initparam[0];
        paramfit[0] = true; //TODO: currently free N, D and G(inf)

        paraminitval[1] = initparam[1];
        paramfit[1] = true;

        paraminitval[2] = initparam[2];
        paramfit[2] = false;

        paraminitval[3] = initparam[3];
        paramfit[3] = false;

        paraminitval[4] = initparam[4];
        paramfit[4] = true;

        paraminitval[5] = initparam[5];
        paramfit[5] = false;

        paraminitval[6] = initparam[6];
        paramfit[6] = false;

        paraminitval[7] = initparam[7];
        paramfit[7] = false;

        paraminitval[8] = initparam[8];
        paramfit[8] = false;

        paraminitval[9] = initparam[9];
        paramfit[9] = false;

        paraminitval[10] = initparam[10];
        paramfit[10] = false;

        q2 = 1; //TODO: set to 1

        q3 = 1; //TODO: set to 1

        pixeldimx = (pixelsize * 1000 / objmag * binningX) / Math.pow(10, 9);
        pixeldimy = (pixelsize * 1000 / objmag * binningY) / Math.pow(10, 9);
        if (sigmaZ <= 0.0 || sigmaZ2 > 100) {
            fitobsvol = obsvolFCS_ST2D1p(2);
        } else {
            fitobsvol = obsvolFCS_ST2D1p(3);
        }

//        IJ.log("paraminitval[0]: " + paraminitval[0] + ", paraminitval[1]: " + paraminitval[1] + ", paraminitval[2]: " + paraminitval[2] + ", paraminitval[3]: " + paraminitval[3]);
//        IJ.log("paraminitval[4]: " + paraminitval[4] + ", paraminitval[5]: " + paraminitval[5] + ", paraminitval[6]: " + paraminitval[6] + ", paraminitval[7]: " + paraminitval[7]);
//        IJ.log("paraminitval[8]: " + paraminitval[8] + ", paraminitval[9]: " + paraminitval[9] + ", paraminitval[10] : " + paraminitval[10]);
//        IJ.log("q2: " + q2 + ", q3: " + q3);
//        IJ.log("pixeldimx: " + pixeldimx + ", pixelsize: " + pixelsize + ", objmag: " + objmag + ", binningX: " + binningX + ", binningY: " + binningY);
//        IJ.log("fitobsvol: " + fitobsvol);
    }

    // calculation of the observation area; this is used in the Diffusion Law Plot as the y-axis
    // the calculation of the observation area/volume is provided on our website in CDF files (http://www.dbs.nus.edu.sg/lab/BFL/index.html)
    public double obsvolFCS_ST2D1p(int dim) {
        // general parameters
        double pi = 3.14159265359;
        double sqrpi = Math.sqrt(pi);
        double ax = pixeldimx;
        double ay = pixeldimy;
        double s = psfsize;
        double sz = lsthickness;
        double psfz = 2 * emlambda / Math.pow(10, 9.0) * 1.33 / Math.pow(NA, 2.0); // size of PSF in axial direction
        double szeff = Math.sqrt(1 / (Math.pow(sz, -2.0) + Math.pow(psfz, -2.0))); // convolution of two Gaussians depending on illumination profile and detection PSF
        double rx = ax * cfXshift / binningX;
        double ry = ay * cfYshift / binningY;
//        IJ.log("inside obsvol cfXshift: " + cfXshift + ", cfYshift: " + cfYshift);

        // help variables, for t = 0, to write the full fit function
        double p00 = s;
        double p1x0 = ax;
        double p2x0 = ax;
        double p1y0 = ay;
        double p2y0 = ay;
        double pexpx0 = 2 * Math.exp(-Math.pow(p1x0 / p00, 2)) - 2;
        double perfx0 = 2 * p1x0 * Erf.erf(p1x0 / p00);
        double pexpy0 = 2 * Math.exp(-Math.pow(p1y0 / p00, 2)) - 2;
        double perfy0 = 2 * p1y0 * Erf.erf(p1y0 / p00);

        //return (p00/sqrpi * pexpx0 + perfx0) * (p00/sqrpi * pexpy0 + perfy0) * Math.pow(sz, 2);
        if (dim == 2) {
            return 4 * Math.pow(ax * ay, 2) / ((p00 / sqrpi * pexpx0 + perfx0) * (p00 / sqrpi * pexpy0 + perfy0));
        } else {
            //return sqrpi * szeff * 4 * Math.pow(ax*ay, 2)/( (p00/sqrpi * pexpx0 + perfx0) * (p00/sqrpi * pexpy0 + perfy0) );
            return 4 * Math.pow(ax * ay, 2) / ((p00 / sqrpi * pexpx0 + perfx0) * (p00 / sqrpi * pexpy0 + perfy0));
        }

    }

    // Nonlinear Least Squares fit for the average ACF
    public class averageFit extends AbstractCurveFitter {

        private int numfreefitpar;

        @Override
        protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
            final int len = points.size();
            final double[] target = new double[len];
            final double[] weights = new double[len];
            final double[] initialGuess;

            int i = 0;				// add data 
            for (WeightedObservedPoint point : points) {
                target[i] = point.getY();
                weights[i] = point.getWeight();
                i++;
            }

            int numparfit = 0;		// count how many paramters will be fit
            for (i = 0; i < noparam; i++) {
                if (paramfit[i] == true) {
                    numparfit++;
                }
            }

            numfreefitpar = numparfit;

            initialGuess = new double[numfreefitpar];	// setup the initial guesses for the parameters
            int num = 0;
            for (i = 0; i < noparam; i++) {
                if (paramfit[i] == true) {
                    initialGuess[num] = paraminitval[i];
                    num++;
                }
            }

            ParametricUnivariateFunction fitfunction;
            switch (currentmodel) { //DAN   // select the fit model to be used; extra fit models can be added here
                case "FCS":
                    fitfunction = new FCS_3p();
                    break;
                case "SPIM-FCS (3D)":
                    fitfunction = new FCS_3p();
//                    fitfunction = new FCS_3p_SPIM();
                    break;
                default:
                    fitfunction = new FCS_3p();
//                    fitfunction = new FCCS_2p();
                    break;
            }

            final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction(fitfunction, points);

            return new LeastSquaresBuilder().
                    maxEvaluations(fitMaxIterations).
                    maxIterations(fitMaxEvaluations).
                    start(initialGuess).
                    target(target).
                    weight(new DiagonalMatrix(weights)).
                    model(model.getModelFunction(), model.getModelFunctionJacobian()).
                    build();
        }

        public Map doFit(String model) {
            //standardFit fitter = new standardFit();
            ArrayList<WeightedObservedPoint> points = new ArrayList<>();

            currentmodel = model;	//set the presently used model

            ParametricUnivariateFunction fitfunction;

            //DAN
            if (currentmodel.equals("FCS")) {	// select the fit model to be used; extra fit models can be added here
                fitfunction = new FCS_3p();
            } else if (currentmodel.equals("SPIM-FCS (3D)")) {
                fitfunction = new FCS_3p();
//                fitfunction = new FCS_3p_SPIM();
            } else {
                fitfunction = new FCS_3p();
//                fitfunction = new FCCS_2p();
            }

            // Add points here
            if (use2imp) { //DAN
                for (int i = 1; i < chanum; i++) {
                    WeightedObservedPoint point = new WeightedObservedPoint(1 / varaveacf[2][i], lagtime[i], aveacf[2][i]);
                    points.add(point);
                }
            } else {
                for (int i = 1; i < chanum; i++) {
                    WeightedObservedPoint point = new WeightedObservedPoint(1 / varaveacf[0][i], lagtime[i], aveacf[0][i]);
                    points.add(point);
                }
            }

            Map<String, Object> map = new HashMap<>();
            try {
                final LeastSquaresOptimizer.Optimum topt = getOptimizer().optimize(getProblem(points));
                double result[] = topt.getPoint().toArray();
                double tmpres[] = topt.getResiduals().toArray();

                // store results and create fit and residual function for the plot
                for (int i = 0; i < chanum; i++) {
                    fitaveacf[i] = fitfunction.value(lagtime[i], result);			// calculate the fit function
                    if (i == 0) {
                        if (use2imp) { //DAN
                            resaveacf[i] = aveacf[2][i] - fitaveacf[i];	// calculate the residuals
                        } else {
                            resaveacf[i] = aveacf[0][i] - fitaveacf[i];	// calculate the residuals
                        }
                    } else {
                        resaveacf[i] = tmpres[i - 1];									// use the weighted residuals
                    }
                }

                chi2aveacf = 0; // initialize chi2 value for this pixel	
                for (int i = 1; i < chanum; i++) {
                    chi2aveacf += Math.pow(resaveacf[i], 2) / (chanum - numfreefitpar - 1);	// calculate chi2 value; do not include the 0 lagtime channel which contains shot noise
                }

                int num = 0;										// store the fit results
                double[] avefitres = new double[noparam];
                for (int i = 0; i < noparam; i++) {
                    if (paramfit[i] == true) {
                        avefitres[i] = result[num];		// for free parameters use the fit results
                        num++;
                    } else {
                        avefitres[i] = paraminitval[i];	// for fixed parameters use the initial values
                    }
                }

                //DAN
//                // update parameters in fit window
//                tfParamN.setText(IJ.d2s(avefitres[0], decformat));
//                tfParamD.setText(IJ.d2s(avefitres[1] * Math.pow(10, 12), decformat));
                tempD = avefitres[1] * Math.pow(10, 12);
//                tfParamVx.setText(IJ.d2s(avefitres[2] * Math.pow(10, 6), decformat));
//                tfParamVy.setText(IJ.d2s(avefitres[3] * Math.pow(10, 6), decformat));
//                tfParamG.setText(IJ.d2s(avefitres[4], decformat2));
//                tfParamF2.setText(IJ.d2s(avefitres[5], decformat));
//                tfParamD2.setText(IJ.d2s(avefitres[6] * Math.pow(10, 12), decformat));
//                tfParamF3.setText(IJ.d2s(avefitres[7], decformat));
//                tfParamD3.setText(IJ.d2s(avefitres[8] * Math.pow(10, 12), decformat));
//                tfParamFtrip.setText(IJ.d2s(avefitres[9], decformat));
//                tfParamTtrip.setText(IJ.d2s(avefitres[10] * Math.pow(10, 6), decformat));
                map.put("results", result);
                map.put("covariance", topt.getCovariances(1).getData());

            } catch (Exception e) {
                IJ.log(e.getClass().getName() + " for average ACF ");
                ArrayList<Double> result = new ArrayList<>();
                int num = 0;
                for (int i = 0; i < noparam; i++) {
                    if (paramfit[i] == true) {
                        result.add(initparam[i]);		// return the initial values
                        num++;
                    }
                }
                for (int i = 0; i < chanum; i++) {
                    fitaveacf[i] = 0.0;
                    resaveacf[i] = 0.0;	// calculate the residuals

                }

                map.put("results", result.toArray());
            }

            return map;
        }
    }

    private void getVoxels(int this_width, int this_height, int this_framediff, float[] pixels, boolean IsNBCalculation) {
        int startX = IsNBCalculation ? 0 : roi1StartX;
        int startY = IsNBCalculation ? 0 : roi1StartY;
        int startFrame = firstframe - 1;

        try {
            imp.getStack().getVoxels(startX, startY, startFrame, this_width, this_height, this_framediff, pixels);
        } catch (Exception e) {
            for (int t = startFrame; t < this_framediff; t++) {
                for (int j = startY; j < this_height; j++) {
                    for (int i = startX; i < this_width; i++) {
                        pixels[t * this_height * this_width + j * this_width + i] = (float) imp.getStack().getVoxel(i, j, t);
                    }
                }
            }
        }
    }

    /*
    arrays pixels1, blockvararray, NBmeanGPU, and NBcovarianceGPU are passed by reference.
     */
    private boolean GPU_Calculate_ACF(float pixels[], double pixels1[], double blockvararray[],
            double NBmeanGPU[], double NBcovarianceGPU[],
            double bleachcorr_params[], GpufitImFCS.ACFParameters GPUparams) {

        boolean IsGPUCalculationOK = true;

        double[] blocked1D = new double[GPUparams.width * GPUparams.height * GPUparams.chanum];

        // run GPU code
        try {
            GpufitImFCS.calcACF(pixels, pixels1, blockvararray, NBmeanGPU, NBcovarianceGPU, blocked1D, bleachcorr_params, samp, lag, GPUparams);
        } catch (Exception e) {
            IsGPUCalculationOK = false;
            e.printStackTrace(System.out);
        }

        //reset blocked
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                blocked[0][i][j] = 0;
            }
        }

        // copy result to blocked
        for (int y = 0; y < GPUparams.height; y++) {
            for (int x = 0; x < GPUparams.width; x++) {
                blocked[0][x + roi1StartX][y + roi1StartY] = (int) blocked1D[GPUparams.width * GPUparams.height + y * GPUparams.width + x];
            }
        }
        return IsGPUCalculationOK;
    }

    public void initializeFitres(int a, int b, int c, int d) {    //initializes arrays for fit parameters, chi2 and cross-correlation amount with NaN
        fitres = new double[a][b][c][d];
        chi2 = new double[a][b][c];
        CCFq = new double[b][c];
        pixfitted = new boolean[a][b][c];
        for (int q = 0; q < a; q++) {
            for (int r = 0; r < b; r++) {
                for (int s = 0; s < c; s++) {
                    chi2[q][r][s] = Double.NaN;
                    pixfitted[q][r][s] = false;
                    for (int t = 0; t < d; t++) {
                        fitres[q][r][s][t] = Double.NaN;
                    }
                }
            }
        }
        for (int r = 0; r < b; r++) {
            for (int s = 0; s < c; s++) {
                CCFq[r][s] = Double.NaN;
            }
        }
    }

    private boolean GPU_Initialize_GPUparams(GpufitImFCS.ACFParameters GPUparams, boolean isNBcalculation) {
        //pixbinX and pixbinY in the cpu plugin are strides in width and height respectively. They  are determined automatically depending on the overlap in the plugin.
        int w_out = isNBcalculation ? width : (int) Math.floor((Math.min(roi1WidthX + cfXDistance, width) - cfXDistance) / pixbinX);
        int h_out = isNBcalculation ? height : (int) Math.floor((Math.min(roi1HeightY + cfYDistance, height) - cfYDistance) / pixbinY);

        if (w_out <= 0 || h_out <= 0) {
            return false;
        } else {
            // win_star and hin_star determines the necessary area/pixels of all required data for GPU calculations. See function getVoxels too.
            int win_star = w_out * pixbinX + cfXDistance;
            int hin_star = h_out * pixbinY + cfYDistance;

            // w_temp and h_temp are 'intermediate' dimensions after accounting for binning.
            // example, if binning is not 1x1, w_temp x h_temp is an area smaller than win_star x hin_star.
            // it is essentially binning with stride 1 (even if overlap is false), 
            // so that we have all the necessary data for all scenarios of our GPU calculations. It simplifies/generalizes the indexing task in our CUDA kernels. 
            // else if binning is 1x1, w_temp = win_star and h_temp = hin_star.
            int w_temp = win_star - binningX + 1;
            int h_temp = hin_star - binningY + 1;

            // stores the current/actual image width and height
            // perform single or double exponential bleach corrections if selected
            String $bcmode = bleachCorMem;

            if ("Polynomial".equals($bcmode)) {
                GPUparams.bleachcorr_gpu = true;
                GPUparams.bleachcorr_order = polyOrder + 1;
            } else {
                GPUparams.bleachcorr_gpu = false;
                GPUparams.bleachcorr_order = 0;
            }

            GPUparams.width = w_out;  //output width
            GPUparams.height = h_out;  //output height
            GPUparams.win_star = isNBcalculation ? width : win_star;
            GPUparams.hin_star = isNBcalculation ? height : hin_star;
            GPUparams.w_temp = isNBcalculation ? width : w_temp; // win_star - pixbinX + 1
            GPUparams.h_temp = isNBcalculation ? height : h_temp; // hin_start - pixbinY + 1
            GPUparams.pixbinX = isNBcalculation ? 1 : pixbinX;
            GPUparams.pixbinY = isNBcalculation ? 1 : pixbinY;
            GPUparams.binningX = isNBcalculation ? 1 : binningX; // binning in X axis
            GPUparams.binningY = isNBcalculation ? 1 : binningY; // binning in Y axis
            GPUparams.firstframe = firstframe;
            GPUparams.lastframe = lastframe;
            GPUparams.framediff = lastframe - firstframe + 1;
            GPUparams.cfXDistance = isNBcalculation ? 0 : cfXDistance;
            GPUparams.cfYDistance = isNBcalculation ? 0 : cfYDistance;
            GPUparams.correlatorp = correlatorp;
            GPUparams.correlatorq = correlatorq;
            GPUparams.frametime = frametime;
            GPUparams.background = background;
            GPUparams.mtab1 = mtab[1]; // mtab[1], used to calculate blocknumgpu.
            GPUparams.mtabchanumminus1 = mtab[chanum - 1]; // mtab[chanum-1], used to calculate pnumgpu[counter_indexarray]
            GPUparams.sampchanumminus1 = samp[chanum - 1]; // samp[chanum-1], used to calculate pnumgpu[counter_indexarray]
            GPUparams.chanum = chanum;
            GPUparams.isNBcalculation = isNBcalculation; // true;
            GPUparams.nopit = nopit;
            GPUparams.ave = (int) Math.floor((lastframe - firstframe + 1) / nopit);

            return true;
        }

    }

    private boolean GPU_get_pixels(GpufitImFCS.ACFParameters GPUparams, float[] pixels, boolean isNBcalculation) {
        // pixels is the input intensity array on which the auto and cross-correlation will be calculated.

        boolean IsGPUCalculationOK = true;

        if (GPUparams.binningY == 1 && GPUparams.binningX == 1) {
            try {
                getVoxels(GPUparams.w_temp, GPUparams.h_temp, GPUparams.framediff, pixels, isNBcalculation);
            } catch (Exception e) {
                IsGPUCalculationOK = false;
                e.printStackTrace(System.out);
            }

        } else {
            float[] pixels_approach2 = new float[GPUparams.win_star * GPUparams.hin_star * GPUparams.framediff];
            getVoxels(GPUparams.win_star, GPUparams.hin_star, GPUparams.framediff, pixels_approach2, isNBcalculation);

            // We found that the JNI function SetFloatArrayRegion fails when the output array is too huge. Tentatively, we set a limit of 96*96*50000.
            boolean WithinSizeLimit = (GPUparams.w_temp * GPUparams.h_temp * GPUparams.framediff) < 96 * 96 * 50000;

            if (WithinSizeLimit && GpufitImFCS.isBinningMemorySufficient(GPUparams)) {
                // Binning on GPU                
                try {
                    GpufitImFCS.calcBinning(pixels_approach2, pixels, GPUparams);
                } catch (Exception e) {
                    IsGPUCalculationOK = false;
                    e.printStackTrace(System.out);
                }
            } else {
                // Binning on CPU
                try {
                    float sum;
                    for (int z = 0; z < GPUparams.framediff; z++) {
                        for (int y = 0; y < GPUparams.h_temp; y++) {
                            for (int x = 0; x < GPUparams.w_temp; x++) {
                                sum = 0;
                                for (int k = 0; k < GPUparams.binningY; k++) {
                                    for (int i = 0; i < GPUparams.binningX; i++) {
                                        sum += (float) pixels_approach2[z * GPUparams.win_star * GPUparams.hin_star + (y + k) * GPUparams.win_star + x + i];
                                    }
                                }
                                pixels[z * GPUparams.w_temp * GPUparams.h_temp + y * GPUparams.w_temp + x] = sum;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }

        if (bgrloaded) {
            float[] bgrmeangpu = new float[GPUparams.w_temp * GPUparams.h_temp];

            for (int j = 0; j < GPUparams.h_temp; j++) {
                for (int i = 0; i < GPUparams.w_temp; i++) {
                    bgrmeangpu[j * GPUparams.w_temp + i] = (float) (int) Math.round(bgrmean[i][j]);
                    //bgrmeangpu[j * GPUparams.w_temp + i] = (float) bgrmean[i][j];
                    //bckg = (int) Math.round(bgrmean[px + i][py + k]);
                }

            }

            for (int k = 0; k < GPUparams.framediff; k++) {
                for (int j = 0; j < GPUparams.h_temp; j++) {
                    for (int i = 0; i < GPUparams.w_temp; i++) {
                        pixels[k * GPUparams.h_temp * GPUparams.w_temp + j * GPUparams.w_temp + i] -= bgrmeangpu[j * GPUparams.w_temp + i];
                    }
                }
            }
        } else {
            int background_correction = background * GPUparams.binningX * GPUparams.binningY;
            for (int i = 0; i < GPUparams.w_temp * GPUparams.h_temp * GPUparams.framediff; i++) {
                pixels[i] -= background_correction;
            }
        }

        return IsGPUCalculationOK;
    }

    private boolean GPU_Calculate_BleachCorrection(GpufitImFCS.ACFParameters GPUparams, float[] pixels, double[] bleachcorr_params) {
        boolean IsGPUCalculationOK = true;

        try {
            float[] datableach_correction = new float[GPUparams.w_temp * GPUparams.h_temp * nopit];
            GpufitImFCS.calcDataBleachCorrection(pixels, datableach_correction, GPUparams);
            int numberFitsbleach_correction = GPUparams.w_temp * GPUparams.h_temp, numberPoints_bleach_correction = nopit;
            float tolerance_bleachcorrection = 0.0000000000000001f; // similar to GPU_ACF_Fit
            int maxNumberIterations_bleachcorrection = fitMaxIterations;
            GpufitImFCS.Model model_bleachcorrection = GpufitImFCS.Model.LINEAR_1D;
            GpufitImFCS.Estimator estimator_bleachcorrection = GpufitImFCS.Estimator.LSE;

            Boolean[] parameters_to_fit_bleachcorrection = new Boolean[model_bleachcorrection.numberParameters];
            parameters_to_fit_bleachcorrection[0] = true;
            for (int i = 1; i < model_bleachcorrection.numberParameters; i++) {
                parameters_to_fit_bleachcorrection[i] = i < GPUparams.bleachcorr_order;
            }

            // NOTE: initialization of 0-th term is different from CPU code, where in class PolynomFit (extends AbstractCurveFitter), the last point in target array is used as offset estimate.
            float[] initialParameters_bleachcorrection = new float[numberFitsbleach_correction * model_bleachcorrection.numberParameters];
            for (int i = 0; i < numberFitsbleach_correction; i++) {
                int offset = i * model_bleachcorrection.numberParameters;
                for (int j = 0; j < model_bleachcorrection.numberParameters; j++) {
                    initialParameters_bleachcorrection[offset + j] = (j == 0) ? datableach_correction[(i + 1) * nopit - 1] : (float) 0; // last value of every nopit points. This works the best.
                }
            }

            float[] intTime_bleachcorrection = new float[nopit];
            for (int z1 = 0; z1 < nopit; z1++) {
                intTime_bleachcorrection[z1] = (float) (frametime * (z1 + 0.5) * GPUparams.ave);
            }

            float[] weights_bleachcorr = new float[numberFitsbleach_correction * numberPoints_bleach_correction];
            for (int i = 0; i < numberFitsbleach_correction * numberPoints_bleach_correction; i++) {
                weights_bleachcorr[i] = (float) 1.0;
            }

            GpufitImFCS.FitModel fitModel_bleachcorrection = new GpufitImFCS.FitModel(numberFitsbleach_correction, numberPoints_bleach_correction, true, model_bleachcorrection, tolerance_bleachcorrection, maxNumberIterations_bleachcorrection, GPUparams.bleachcorr_order, parameters_to_fit_bleachcorrection, estimator_bleachcorrection, nopit * Float.SIZE / 8);
            fitModel_bleachcorrection.weights.clear();
            fitModel_bleachcorrection.weights.put(weights_bleachcorr);
            fitModel_bleachcorrection.userInfo.clear();
            fitModel_bleachcorrection.userInfo.put(intTime_bleachcorrection);
            fitModel_bleachcorrection.data.clear();
            fitModel_bleachcorrection.data.put(datableach_correction);
            fitModel_bleachcorrection.initialParameters.clear();
            fitModel_bleachcorrection.initialParameters.put(initialParameters_bleachcorrection);
            GpufitImFCS.FitResult fitResult_bleachcorrection = GpufitImFCS.fit(fitModel_bleachcorrection);
            // boolean[] converged_bleachcorrection = new boolean[numberFitsbleach_correction];

            int numberConverged_bleachcorrection = 0, numberMaxIterationExceeded_bleachcorrection = 0, numberSingularHessian_bleachcorrection = 0, numberNegativeCurvatureMLE_bleachcorrection = 0;
            for (int i = 0; i < numberFitsbleach_correction; i++) {
                GpufitImFCS.FitState fitState_bleachcorrection = GpufitImFCS.FitState.fromID(fitResult_bleachcorrection.states.get(i));
                // converged_bleachcorrection[i] = fitState_bleachcorrection.equals(FitState.CONVERGED);
                switch (fitState_bleachcorrection) {
                    case CONVERGED:
                        numberConverged_bleachcorrection++;
                        break;
                    case MAX_ITERATIONS:
                        numberMaxIterationExceeded_bleachcorrection++;
                        break;
                    case SINGULAR_HESSIAN:
                        numberSingularHessian_bleachcorrection++;
                        break;
                    case NEG_CURVATURE_MLE:
                        numberNegativeCurvatureMLE_bleachcorrection++;
                }
            }

            int counter4 = 0;
            for (int y1 = 0; y1 < GPUparams.h_temp; y1++) {
                for (int x1 = 0; x1 < GPUparams.w_temp; x1++) {
                    for (int ii = 0; ii < GPUparams.bleachcorr_order; ii++) {
                        bleachcorr_params[counter4] = fitResult_bleachcorrection.parameters.get((y1 * GPUparams.w_temp + x1) * model_bleachcorrection.numberParameters + ii);
                        counter4 += 1;
                    }
                }
            }

            fitModel_bleachcorrection.reset();
            fitResult_bleachcorrection.reset();
        } catch (Exception e) {
            IsGPUCalculationOK = false;
            e.printStackTrace(System.out);
        }

        return IsGPUCalculationOK;
    }

    private boolean GPU_ACF_Fit(GpufitImFCS.ACFParameters GPUparams, double[] pixels1, double[] blockvararray) {
        boolean IsGPUCalculationOK = true;

        try {
            prepareFit();

            int numberFits = GPUparams.width * GPUparams.height;
            int numberPoints = chanum - 1;
            float tolerance = 0.0000000000000001f;
            int maxNumberIterations = fitMaxIterations;
            GpufitImFCS.Model model = GpufitImFCS.Model.ACF_1D;
            GpufitImFCS.Estimator estimator = GpufitImFCS.Estimator.LSE;
            float[] trueParameters;

            trueParameters = new float[]{
                //                    (float) Double.parseDouble(tfParamN.getText()),
                //                    (float) Double.parseDouble(tfParamD.getText()) / (float) Math.pow(10, 12),
                //                    (float) Double.parseDouble(tfParamVx.getText()) / (float) Math.pow(10, 6),
                //                    (float) Double.parseDouble(tfParamVy.getText()) / (float) Math.pow(10, 6),
                //                    (float) Double.parseDouble(tfParamG.getText()),
                //                    (float) Double.parseDouble(tfParamF2.getText()),
                //                    (float) Double.parseDouble(tfParamD2.getText()) / (float) Math.pow(10, 12),
                //                    (float) Double.parseDouble(tfParamF3.getText()),
                //                    (float) Double.parseDouble(tfParamD3.getText()) / (float) Math.pow(10, 12),
                //                    (float) Double.parseDouble(tfParamFtrip.getText()),
                //                    (float) Double.parseDouble(tfParamTtrip.getText()) / (float) Math.pow(10, 6),
                (float) pixeldimx,
                (float) pixeldimy,
                (float) psfsize,
                (float) lsthickness,
                (float) pixeldimx * cfXshift / binningX,
                (float) pixeldimy * cfYshift / binningY,
                (float) fitobsvol, //                    (float) Double.parseDouble(tfParamQ2.getText()),
            //                    (float) Double.parseDouble(tfParamQ3.getText())
            };

            float[] initialParameters = new float[numberFits * model.numberParameters];

            for (int i = 0; i < numberFits; i++) {
                int offset = i * model.numberParameters;
                // System.arraycopy(trueParameters, 0, initialParameters, offset, model.numberParameters);
                for (int j = 0; j < model.numberParameters; j++) {
                    initialParameters[offset + j] = trueParameters[j];
                }
            }

            float[] user_info = new float[numberPoints];
            for (int i = 0; i < GPUparams.chanum - 1; i++) {
                user_info[i] = (float) lagtime[i + 1];
            }

            float[] data = new float[numberFits * numberPoints];
            int counter = 0;

            // Out of the 20 parameters passed to the GPUfit, only the first 11 are fitting parameters.
            Boolean[] parameters_to_fit = new Boolean[model.numberParameters];
//                parameters_to_fit[0] = !rbtnHoldN.isSelected();
//                parameters_to_fit[1] = !rbtnHoldD.isSelected();
//                parameters_to_fit[2] = !rbtnHoldVx.isSelected();
//                parameters_to_fit[3] = !rbtnHoldVy.isSelected();
//                parameters_to_fit[4] = !rbtnHoldG.isSelected();
//                parameters_to_fit[5] = !rbtnHoldF2.isSelected();
//                parameters_to_fit[6] = !rbtnHoldD2.isSelected();
//                parameters_to_fit[7] = !rbtnHoldF3.isSelected();
//                parameters_to_fit[8] = !rbtnHoldD3.isSelected();
//                parameters_to_fit[9] = !rbtnHoldFtrip.isSelected();
//                parameters_to_fit[10] = !rbtnHoldTtrip.isSelected();
            for (int i = 11; i < model.numberParameters; i++) {
                parameters_to_fit[i] = false;
            }

            for (int y = 0; y < GPUparams.height; y++) {
                for (int x = 0; x < GPUparams.width; x++) {
                    for (int z = 0; z < GPUparams.chanum - 1; z++) {
                        data[counter] = (float) pixels1[(z + 1) * GPUparams.width * GPUparams.height + y * GPUparams.width + x];
                        counter += 1;
                    }
                }
            }

            float[] weights = new float[numberFits * numberPoints];
            int counter1 = 0;
            for (int y = 0; y < GPUparams.height; y++) {
                for (int x = 0; x < GPUparams.width; x++) {
                    for (int z = 0; z < GPUparams.chanum - 1; z++) {
                        weights[counter1] = (float) 1.0 / (float) blockvararray[(z + 1) * GPUparams.width * GPUparams.height + y * GPUparams.width + x];
                        varacf[0][x][y][z] = blockvararray[(z + 1) * GPUparams.width * GPUparams.height + y * GPUparams.width + x];
                        sdacf[0][x][y][z] = Math.sqrt((double) varacf[0][x][y][z]);
                        counter1 = counter1 + 1;
                    }
                }
            }

            float[] userinfo2 = new float[numberPoints];
            // System.arraycopy(user_info, 0, userinfo2, 0, numberPoints);
            for (int i = 0; i < numberPoints; i++) {
                userinfo2[i] = user_info[i];
            }

            GpufitImFCS.FitModel fitModel = new GpufitImFCS.FitModel(numberFits, numberPoints, true, model, tolerance, maxNumberIterations, GPUparams.bleachcorr_order, parameters_to_fit, estimator, (GPUparams.chanum - 1) * Float.SIZE / 8);
            fitModel.weights.clear();
            fitModel.weights.put(weights);
            fitModel.userInfo.clear();
            fitModel.userInfo.put(userinfo2);
            fitModel.data.clear();
            fitModel.data.put(data);
            fitModel.initialParameters.clear();
            fitModel.initialParameters.put(initialParameters);

            GpufitImFCS.FitResult fitResult = GpufitImFCS.fit(fitModel);

            boolean[] converged = new boolean[numberFits];
            int numberConverged = 0, numberMaxIterationExceeded = 0, numberSingularHessian = 0, numberNegativeCurvatureMLE = 0;
            for (int i = 0; i < numberFits; i++) {
                GpufitImFCS.FitState fitState = GpufitImFCS.FitState.fromID(fitResult.states.get(i));
                converged[i] = fitState.equals(GpufitImFCS.FitState.CONVERGED);
                switch (fitState) {
                    case CONVERGED:
                        numberConverged++;
                        break;
                    case MAX_ITERATIONS:
                        numberMaxIterationExceeded++;
                        break;
                    case SINGULAR_HESSIAN:
                        numberSingularHessian++;
                        break;
                    case NEG_CURVATURE_MLE:
                        numberNegativeCurvatureMLE++;
                }
            }

            float[] convergedParameterMean = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            // float[] convergedParameterStd = new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            for (int i = 0; i < numberFits; i++) {
                for (int j = 0; j < model.numberParameters; j++) {
                    if (converged[i]) {
                        convergedParameterMean[j] += fitResult.parameters.get(i * model.numberParameters + j);
                    }
                }
            }

            int parfitcounters = 0;
            for (int i = 0; i < noparam; i++) {
                if (paramfit[i] == true) {
                    parfitcounters++;
                }
            }

            double[][][] gpuresultarray = new double[width][height][model.numberParameters];

            // reset
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    chi2[0][i][j] = Double.NaN;
                    pixfitted[0][i][j] = false;
                    pixvalid[0][i][j] = Double.NaN;

                    for (int k = 0; k < model.numberParameters; k++) {
                        gpuresultarray[i][j][k] = Double.NaN;
                    }

                    for (int k = 0; k < noparam; k++) {
                        fitres[0][i][j][k] = Double.NaN;
                    }
                }
            }

            int numfreefitpar = parfitcounters;
            for (int i1 = 0; i1 < GPUparams.width; i1++) {
                for (int i2 = 0; i2 < GPUparams.height; i2++) {
                    chi2[0][i1 + roi1StartX][i2 + roi1StartY] = fitResult.chiSquares.get(i2 * GPUparams.width + i1) / ((fitend - fitstart) - numfreefitpar - 1);

                    if (converged[i2 * GPUparams.width + i1]) {
                        pixfitted[0][i1 + roi1StartX][i2 + roi1StartY] = true;
                        pixvalid[0][i1 + roi1StartX][i2 + roi1StartY] = 1.0;

                        for (int j = 0; j < model.numberParameters; j++) {
                            gpuresultarray[i1 + roi1StartX][i2 + roi1StartY][j] = (double) fitResult.parameters.get((i2 * GPUparams.width + i1) * model.numberParameters + j);
                        }
                    }
                }
            }

            int LBoundX = roi1StartX;
            int UBoundX = roi1StartX + GPUparams.width;
            int LBoundY = roi1StartY;
            int UBoundY = roi1StartY + GPUparams.height;

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < noparam; k++) {
                        if (i >= LBoundX && i < UBoundX && j >= LBoundY && j < UBoundY) {
                            if (filterArray[i * pixbinX][j * pixbinY] >= filterLL * binningX * binningY && filterArray[i * pixbinX][j * pixbinY] <= filterUL * binningX * binningY) {
                                fitres[0][i][j][k] = gpuresultarray[i][j][k];
                            }
                        }
                    }
                }
            }

            fitModel.reset();
            fitResult.reset();
        } catch (NumberFormatException e) {
            IsGPUCalculationOK = false;
            e.printStackTrace(System.out);
        }

        return IsGPUCalculationOK;
    }

    public boolean GPU_Calculate_ACF_All(Roi improi) {

        boolean IsGPUCalculationOK;

        if (!"none".equals(bleachCorMem) && !"Polynomial".equals(bleachCorMem)) {
            System.out.println("Calculation on GPU is currently supported only for Polynomial bleach correction.");
            return false;
        }

        if (cfXDistance < 0 || cfYDistance < 0) {
            System.out.println("Calculations on GPU current supports only non negative CF X and CF Y distance currently.");
            return false;
        }

        // ********************************************************************************************************************
        // Parameters
        // ********************************************************************************************************************
        int framediff = lastframe - firstframe + 1;

        Rectangle imprect = improi.getBounds();
        int startXmap = (int) Math.ceil(imprect.getX() / pixbinX);
        int startYmap = (int) Math.ceil(imprect.getY() / pixbinY);
        int endXmap = (int) Math.floor((imprect.getX() + imprect.getWidth() - binningX) / pixbinX);
        int endYmap = (int) Math.floor((imprect.getY() + imprect.getHeight() - binningY) / pixbinY);

        int startX = startXmap * pixbinX;
        int startY = startYmap * pixbinY;
        int endX = endXmap * pixbinX;
        int endY = endYmap * pixbinY;

        //TODO fitting
//            tbFCCSDisplay.setSelected(false);
//            tbFCCSDisplay.setText("Off");
        filterArray = new float[width][height]; // calculate the mean image of the stack

        if (filterMem == "Mean" || filterMem == "Intensity") {
            initializeFitres(3, width, height, noparam); // reset the fitresult array and thus the parameter window
            if (filterMem == "Mean") {
                for (int x1 = startX; x1 <= endX; x1 = x1 + pixbinX) {
                    for (int x2 = startY; x2 <= endY; x2 = x2 + pixbinY) {
                        for (int x3 = firstframe; x3 <= lastframe; x3++) {
                            for (int x4 = 0; x4 < binningX; x4++) {
                                for (int x5 = 0; x5 < binningY; x5++) {
                                    if (improi.contains(x1, x2) && improi.contains(x1, x2 + binningY - 1) && improi.contains(x1 + binningX - 1, x2) && improi.contains(x1 + binningX - 1, x2 + binningY - 1)) {
                                        filterArray[x1][x2] += imp.getStack().getProcessor(x3).get(x1 + x4, x2 + x5);
                                    } else {
                                        filterArray[x1][x2] = Float.NaN;
                                    }
                                }
                            }
                        }
                        filterArray[x1][x2] /= framediff;
                    }
                }
            } else { // if "Intensity" was selcted, then get the first frame and bin if necessary
                for (int x1 = startX; x1 <= endX; x1 = x1 + pixbinX) {
                    for (int x2 = startY; x2 <= endY; x2 = x2 + pixbinY) {
                        for (int x3 = 0; x3 < binningX; x3++) {
                            for (int x4 = 0; x4 < binningY; x4++) {
                                if (improi.contains(x1, x2) && improi.contains(x1, x2 + binningY - 1) && improi.contains(x1 + binningX - 1, x2) && improi.contains(x1 + binningX - 1, x2 + binningY - 1)) {
                                    filterArray[x1][x2] += imp.getStack().getProcessor(firstframe).get(x1 + x3, x2 + x4);
                                } else {
                                    filterArray[x1][x2] = Float.NaN;
                                }
                            }
                        }
                    }
                }
            }
        } else { // if "none" was selected
            for (int x1 = startX; x1 <= endX; x1 = x1 + pixbinX) {
                for (int x2 = startY; x2 <= endY; x2 = x2 + pixbinY) {
                    for (int x3 = 0; x3 < binningX; x3++) {
                        for (int x4 = 0; x4 < binningY; x4++) {
                            if (improi.contains(x1, x2) && improi.contains(x1, x2 + binningY - 1) && improi.contains(x1 + binningX - 1, x2) && improi.contains(x1 + binningX - 1, x2 + binningY - 1)) {
                                filterArray[x1][x2] += imp.getStack().getProcessor(firstframe).get(x1 + x3, x2 + x4);
                            } else {
                                filterArray[x1][x2] = Float.NaN;
                            }
                        }
                    }
                }
            }
        }

        if (isdlawcalculatedingpu == 0) {
            IJ.showProgress(5, 100); // 5%
        }

        // Object to store some of input values for GPU calculations
        GpufitImFCS.ACFParameters GPUparams = new GpufitImFCS.ACFParameters();
        IsGPUCalculationOK = GPU_Initialize_GPUparams(GPUparams, false);

        assert GPUparams.width == (endXmap - startXmap + 1) : "Invalid GPUparams.width";
        assert GPUparams.height == (endYmap - startYmap + 1) : "Invalid GPUparams height";

        if (!GpufitImFCS.isACFmemorySufficient(GPUparams)) {
            IsGPUCalculationOK = false;
            System.out.println("Insufficient GPU memory. Cannot perform ACF calculations on GPU.");
        }

        // ********************************************************************************************************************
        // Get pixels.
        // ******************************************************************************************************************** 
        float[] pixels = new float[GPUparams.w_temp * GPUparams.h_temp * GPUparams.framediff];
        if (IsGPUCalculationOK) {
            IsGPUCalculationOK = GPU_get_pixels(GPUparams, pixels, false);
        }

        if (isdlawcalculatedingpu == 0) {
            IJ.showProgress(10, 100);
        }

        // ********************************************************************************************************************
        // Bleach correction
        // ********************************************************************************************************************   
        double[] bleachcorr_params = new double[GPUparams.w_temp * GPUparams.h_temp * GPUparams.bleachcorr_order];
        if (GPUparams.bleachcorr_gpu && IsGPUCalculationOK) {
            IsGPUCalculationOK = GPU_Calculate_BleachCorrection(GPUparams, pixels, bleachcorr_params);

            if (isdlawcalculatedingpu == 0) {
                IJ.showProgress(40, 100);
            }
        }

        // ********************************************************************************************************************
        // Calculate ACF
        // ********************************************************************************************************************  
        // pixels1 is the output array in which the auto and cross-calculations values are stored.
        double[] pixels1 = new double[GPUparams.width * GPUparams.height * GPUparams.chanum];
        double[] blockvararray = new double[GPUparams.width * GPUparams.height * GPUparams.chanum];

        if (IsGPUCalculationOK) {

            // N&B Calculations. Note cfXdistance and cfYdistance should be zero.
            double[] NBmeanGPU = new double[GPUparams.width * GPUparams.height];
            double[] NBcovarianceGPU = new double[GPUparams.width * GPUparams.height];

            try {
                IsGPUCalculationOK = GPU_Calculate_ACF(pixels, pixels1, blockvararray, NBmeanGPU, NBcovarianceGPU, bleachcorr_params, GPUparams);

                if (!IsGPUCalculationOK) {
                    throw new Exception("Error in ACF calculation.");
                }

                // clear acf array
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        for (int k = 0; k < chanum; k++) {
                            acf[0][i][j][k] = 0.0;
                        }
                    }
                }

                for (int z = 0; z < GPUparams.chanum; z++) {
                    for (int y = 0; y < GPUparams.height; y++) {
                        for (int x = 0; x < GPUparams.width; x++) {
//                            acf[0][x][y][z] = pixels1[z * GPUparams.width * GPUparams.height + y * GPUparams.width + x];
                            acf[0][x + roi1StartX][y + roi1StartY][z] = pixels1[z * GPUparams.width * GPUparams.height + y * GPUparams.width + x];
                        }
                    }
                }

                if (doMSD) {
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            if (!MSDmode) { // 2D if MSDmode is false, otherwise 3D                
                                msd[0][x][y] = correlationToMSD(acf[0][x][y], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6));
                            } else {
                                msd[0][x][y] = correlationToMSD3D(acf[0][x][y], pixeldimx * Math.pow(10, 6), psfsize * Math.pow(10, 6), lsthickness * Math.pow(10, 6));
                            }
                        }
                    }
                }

                if (isdlawcalculatedingpu == 0) {
                    IJ.showProgress(70, 100);
                }

            } catch (Exception e) {
                e.printStackTrace(System.out);
            }

        }

        // ********************************************************************************************************************
        // ACF_1D fit
        // ********************************************************************************************************************       
        if (IsGPUCalculationOK) {
            if (doFit) {
                IsGPUCalculationOK = GPU_ACF_Fit(GPUparams, pixels1, blockvararray);
            }

            if (isdlawcalculatedingpu == 0) {
                IJ.showProgress(100, 100);
            }

            if (IsGPUCalculationOK) {

                if (isdlawcalculatedingpu == 0) {
                    IJ.showStatus("Plotting data.");
                    plotCF(improi, 2, false);
                    calcAverageIntensityTrace(filterArray, startX, startY, endX, endY, firstframe, lastframe);
                    plotIntensityTrace(improi, 2);
                    // plot MSD if selected
                    if (doMSD) {
                        //plotMSD(improi, 2, false); //TODO: if calc msd true
                    }
                    // create parameter map window
                    if (doFit) {
                        //createParaImp(maxcposx - mincposx + 1, maxcposy - mincposy + 1); //TODO: if doFit true
//                        createParaImp(GPUparams.width, GPUparams.height);
                    }
                }
            }
        }

        try {
            GpufitImFCS.resetGPU();
        } catch (Exception e) {
            System.out.println("Unable to reset GPU");
        }

        return IsGPUCalculationOK;
    }
}
