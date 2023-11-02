package directCameraReadout.util;

import directCameraReadout.gui.DirectCapturePanel;
import directCameraReadout.system.SystemInfo;

public class Utilities {

    public static void setSizeAandSizeB(int oW, int oH, int maxElem, int minPlotInt, int maxPlotInt) {//size_a * size_b = number of frame buffer
        //recommended size_b = 50*size_a
        int c = 50;
        int tempSize = (int) Math.floor(maxElem / (oW * oH));
        if (tempSize < 1.5 * minPlotInt) {
            // check if tempSize is at least 1.5x the size of minPlotInt
            DirectCapturePanel.Common.size_a = (int) Math.floor(Math.sqrt(tempSize / c));
            DirectCapturePanel.Common.size_b = DirectCapturePanel.Common.size_a * c;

        } else {
            // check if tempSize is unnecessarily large
            if (tempSize > 1.5 * maxPlotInt) {
                DirectCapturePanel.Common.size_a = (int) Math.floor(Math.sqrt(1.5 * maxPlotInt / c));
                DirectCapturePanel.Common.size_b = DirectCapturePanel.Common.size_a * c;
            } else {
                DirectCapturePanel.Common.size_a = (int) Math.floor(Math.sqrt(tempSize / c));
                DirectCapturePanel.Common.size_b = DirectCapturePanel.Common.size_a * c;
            }
        }
    }

    public static boolean setSizeRandSizeC(int f, int w, int h) {

//        long availmem = SystemInfo.totalAllocatedMemory(); //byte
        long availmem = SystemInfo.totalDesignatedMemory(); //byte

        long byteperpix = 2;//currently short //TODO: change this when implenting int[]
        double overhead = 0.7;  //TODO: find out a better way to profile
        final long size_c = 300_000_000;
        long size_r = 1;
        final double totpix = (double) f * w * h;

        int iterf = 0;
        for (int i = 0; i < (Math.ceil((double) totpix / size_c)); i++) {
            iterf += Math.floor((double) size_c / (w * h));
            if (iterf >= f) {
                break;
            } else {
                size_r++;
            }
        }

//        IJ.log("size_r: " + size_r + ", size_c: " + size_c);
        if (size_r * size_c * byteperpix > (overhead * availmem)) {
            return false;
        }

        DirectCapturePanel.Common.size_c = (int) size_c;
        DirectCapturePanel.Common.size_r = (int) size_r;

        return true;
    }

    public static int retMaxAllowablePlotInterval(int sizeA, int sizeB) {

        return (int) Math.floor(sizeA * sizeB / 1.5);
    }

}
