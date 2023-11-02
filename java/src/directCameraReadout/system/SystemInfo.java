package directCameraReadout.system;

import directCameraReadout.gui.DirectCapturePanel.Common;
import ij.IJ;

public class SystemInfo {

    public static long totalDesignatedMemory() {
        return IJ.maxMemory();
//        return Runtime.getRuntime().maxMemory();
    }

    public static long currentAllocatedFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public static long totalAllocatedMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long usedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public static long totalFreeMemory() {
        return (IJ.maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//        return (Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
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
