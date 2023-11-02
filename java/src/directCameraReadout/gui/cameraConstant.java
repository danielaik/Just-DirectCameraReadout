package directCameraReadout.gui;

public class cameraConstant {

    public static class Common_iXon860 {

        public final static int minHeight = 6;
        public final static int defaultTemp = -80;

        public final static int[][] RecommendedCentralCrop = {
            {32, 32, 49, 80, 49, 80},
            {64, 64, 33, 96, 33, 96},
            {128, 128, 1, 128, 1, 128}
        };
    }

    public static class Common_iXon888 {

        public final static int minHeight = 4;
        public final static int defaultTemp = -45;

        //w, h, l, r, t(actually bottom according to Andormanual), b(actually top according to andor manual)
        public final static int[][] RecommendedCentralCrop = {
            //            {
            //                32, 32, 1, 32, 1, 32
            //            },
            //            {
            //                64, 64, 1, 64, 1, 64
            //            },
            //            {
            //                128, 128, 1, 128, 1, 128
            //            },
            //            {
            //                256, 256, 1, 256, 1, 256
            //
            //            },
            //            {
            //                512, 512, 1, 512, 1, 512
            //            },
            //            {
            //                1024, 4, 1, 1024, 1, 4
            //            },g
            //            {
            //                1024, 8, 1, 1024, 1, 8
            //            },
            //            {
            //                1024, 16, 1, 1024, 1, 16
            //
            //            },
            //            {
            //                1024, 32, 1, 1024, 496, 527
            //            }
            {
                32, 32, 487, 518, 496, 527
            },
            {
                64, 64, 476, 539, 480, 543
            },
            {
                128, 128, 433, 560, 448, 575
            },
            {
                256, 256, 369, 624, 384, 639

            },
            {
                512, 512, 241, 752, 256, 767
            },
            {
                1024, 4, 1, 1024, 510, 513
            },
            {
                1024, 8, 1, 1024, 508, 515
            },
            {
                1024, 16, 1, 1024, 504, 519

            },
            {
                1024, 32, 1, 1024, 496, 527
            }
        };
    }

    public static class Common_iXon897 {

        public final static int minHeight = 4;
        public final static int defaultTemp = -45;

        //w, h, l, r, t, b
        public final static int[][] RecommendedCentralCrop = {
            //            {
            //                32, 32, 1, 32, 1, 32
            //            },
            //            {
            //                64, 64, 1, 32, 1, 32
            //            },
            //            {
            //                96, 96, 1, 96, 1, 96
            //            },
            //            {
            //                128, 128, 1, 128, 1, 128
            //            },
            //            {
            //                192, 192, 1, 192, 1, 192
            //            },
            //            {
            //                256, 256, 1, 256, 1, 256
            //            },
            //            {
            //                496, 4, 1, 496, 1, 4
            //            },
            //            {
            //                496, 8, 1, 496, 1, 8
            //            },
            //            {
            //                496, 16, 1, 496, 1, 16
            //            }
            {
                32, 32, 241, 272, 240, 271
            },
            {
                64, 64, 219, 282, 224, 287
            },
            {
                96, 96, 209, 304, 208, 303
            },
            {
                128, 128, 189, 316, 192, 319
            },
            {
                192, 192, 157, 348, 160, 351
            },
            {
                256, 256, 123, 378, 128, 383
            },
            {
                496, 4, 8, 503, 254, 257
            },
            {
                496, 8, 8, 503, 252, 259
            },
            {
                496, 16, 8, 503, 249, 262
            }

        };
    }

    public static class Common_SONA {

        public static String[] listPixelEncoding;//0=Mono12, 1=Mono12Packed, 2=Mono16

        public static int PixelEncoding;
        public final static int defaultTemp = -45;
        public final static int minHeight = 32;//25
        public static int isOverlap; //Overlap readout and exposure
        public static String[] OutputTriggerKindArr;//FireRow1 //FireRowN //FireAll //FireAny
        public static int OutputTriggerKind;//0-FireRow1 //1-FireRowN //2-FireAll //3-FireAny

    }

    public static class Common_Orca {//Common for Orca Flash and Orca Quest

        public final static int minHeight = 32;
        //PixelWidth and height must be a multiple of 4

        public static String[] OutputTriggerKindArr;//Disabled //Programmable //Global
        public static int OutputTriggerKind;//0-Disabled //1-Programmable //2-Global
        public static double outTriggerDelay;//valid for Programmable only //sec
        public static double outTriggerPeriod;//valid for Programmable only//sec

        public static String[] readoutSpeedArr;//SLOWEST // FASTEST
        public static int readoutSpeed;//0-DCAMPROP_READOUTSPEED__SLOWEST; 1-DCAMPROP_READOUTSPEED__FASTEST (default for both orca flash and orca quest)

        public static String[] sensorModeArr; //AREA //PHOTONNUMBERRESOLVING (photon count mode run on Quest only)
        public static int sensorMode;//0-DCAMPROP_SENSORMODE__AREA; 1-DCAMPROP_SENSORMODE__PHOTONNUMBERRESOLVING (photon count run on Quest only)
    }

    public static class Common_Photometrics {

        public final static int minHeight = 8;
        
        // Readout speed
        public static int readoutSpeedIndex; //
        public static int readoutPortIndex;//
        public static String[][] readoutSpeedDescription; // [0]combination, [1] port index, port speed, speed (MHz), bit-depth, description
    }
}
