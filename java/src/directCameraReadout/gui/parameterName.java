package directCameraReadout.gui;

public class parameterName {

    public static class calibrationType {

        public static final String[] calibrationTypeList = new String[]{
            "TIRF focus",
            "TIRF angle",
            "SPIM illumination objective",
            "SPIM detection objective",
            "Optosplit alignment",
            "XY stage movement",
            "Other remarks"
        };

    }

    public static class modeType {

        //TODO: enum it?
        public static final String[] $amode = {
            "None",
            "NonCumulative",
            "Cumulative",
            "Iccs"
        };
    }

    public static class mode {

        public static enum modeEnum {
            SINGLECAPTURE(0),
            LIVEVIDEO(1),
            CALIBRATION(2),
            ACQUISITION(3),
            ICCS(4);

            private final int label;

            modeEnum(int label) {
                this.label = label;
            }

            public int getValue() {
                return label;
            }

            public static modeEnum getEnum(String label) {
                if (label.equals(modeList[0])) {
                    return SINGLECAPTURE;
                }
                if (label.equals(modeList[1])) {
                    return LIVEVIDEO;
                }
                if (label.equals(modeList[2])) {
                    return CALIBRATION;
                }
                if (label.equals(modeList[3])) {
                    return ACQUISITION;
                }
                if (label.equals(modeList[4])) {
                    return ICCS;
                }
                return null;
            }

        }

        public static final String[] modeList = { // Current available modeList
            "Single Capture", // Support analysisMode = None (index 0)
            "Live Video",// Support analysisMode = None (amode[0])
            "Calibration",// Support analysisMode = None (amode[0]), NonCumulative(amode[1])
            "Acquisition",// Support analysisMode = None (amode[0]), NonCumulative(amode[1]), Cumulative (amode[2])
            "ICCS" // Support analysisMode = None (amode[0]), Iccs (amode[3])
        };

        public static String getStringValue(int idx) {
            return modeList[idx];
        }

        public static int size() {
            return modeList.length;
        }
    }

    public static class liveVideoBinMode {

        public static enum liveVideoBinModeEnum {
            NO_BINNING(0),
            AVERAGE_BINNING(1),
            SUM_BINNING(2);

            private final int label;

            liveVideoBinModeEnum(int label) {
                this.label = label;
            }

            public int getValue() {
                return label;
            }

            public static liveVideoBinModeEnum getEnum(String label) {
                if (label.equals(modeList[0])) {
                    return NO_BINNING;
                }
                if (label.equals(modeList[1])) {
                    return AVERAGE_BINNING;
                }
                if (label.equals(modeList[2])) {
                    return SUM_BINNING;
                }
                return null;
            }

        }

        public static final String[] modeList = {
            "No binning",
            "Average binning",
            "Sum binning"
        };

        public static String getStringValue(int idx) {
            return modeList[idx];
        }

        public static int size() {
            return modeList.length;
        }
    }

}
