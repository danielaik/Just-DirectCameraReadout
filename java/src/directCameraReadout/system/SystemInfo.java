package directCameraReadout.system;

import ij.IJ;
import directCameraReadout.gui.DirectCapturePanel.Common;

public class SystemInfo {

    private final Runtime runtime = Runtime.getRuntime();       // Program currently runs on one JVM/machine

    public long totalMem() {
        return Runtime.getRuntime().totalMemory();
    }

    public long usedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long freeMem() {
        return Runtime.getRuntime().freeMemory();
    }

    public static void explicitGC() {
        long time = System.nanoTime();

        Common.bufferArray1D = null;
        Common.imp_cum = null;
        Common.ims_cum = null;
        Common.ims_nonCumRed = null;
        Common.ims_nonCumGreen = null;
        System.gc();

//        IJ.log("ran explicit GC: " + (System.nanoTime() - time) / 1000000 + " ms");
    }

}
