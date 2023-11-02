package directCameraReadout.util;

import ij.IJ;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;

public class TimeTaggedStorage {

    String[] paramName;
    String[] subParamName; //String[] calibrationTypeList 5 element

    Map<String, Object> map;
    Map<String, Object> map21;
    Map<Integer, String> map22;
    List<ArrayList<ArrayList<Integer>>> map31;

    int counter;

    FileWriter jsonFile;

    boolean isAvailForSavings;

    public TimeTaggedStorage(String[] calibrationTypeList) {
        fillJsonKey(calibrationTypeList);
        counter = 0;
        initialize();
    }

    private void fillJsonKey(String[] in) {
        //subParamName
        subParamName = new String[in.length - 1];
        System.arraycopy(in, 0, subParamName, 0, subParamName.length);

        //paramName
        paramName = new String[]{
            "calibration type",
            in[in.length - 1] //user note to mark other situation: store frame index and user observation (chemical addition, changes in laser power etc)
        };

    }

    private void initialize() {

        /* Example of JSON data structure
    
            {
            "other remarks":{"2000": "some remarks"},
            "calibration type":{
                                "SPIM illumination objective":[[1,100],[1000,2000],...],
                                "SPIM detection objective":[[X,X],[X,X],...],
                                "TIRF angle":[[X,X],[X,X],...],
                                "Optosplit alignment":[[X,X],[X,X],...],
                                "TIRF focus":[[X,X],[X,X],...]
                                }
            }
    
         */
        map = new HashMap<>();
        map21 = new HashMap<>();
        map22 = new HashMap<>();
        map31 = new ArrayList<>();

        map.put(paramName[0], map21);
        map.put(paramName[1], map22);

        for (int i = 0; i < subParamName.length; i++) {
            map31.add(new ArrayList<>());
            map21.put(subParamName[i], map31.get(i));
        }

        isAvailForSavings = false;

    }

    public boolean fillCalibrationFrameIdx(int calibIdx, int frameIdx) {

        if (!isAvailForSavings) {
            isAvailForSavings = true;
        }

        //frameIdx starting from 1
        //calibIdx correspond to type of calibration as in subParamName
        //TODO: add catch for invalid frameIdx and remove previous index
        if (counter % 2 == 0) {
            //fill calib start index
            map31.get(calibIdx).add(new ArrayList<>());
            map31.get(calibIdx).get(map31.get(calibIdx).size() - 1).add(frameIdx);
            IJ.log("Adjustment " + calibIdx + " stored; frame start: " + frameIdx);
        } else {
            //fill calib end index
            map31.get(calibIdx).get(map31.get(calibIdx).size() - 1).add(frameIdx);
            IJ.log("Adjustment " + calibIdx + " stored; frame end: " + frameIdx);
        }

        counter++;
        return true;

    }

    public boolean fillOtherRemarksFrameIdx(String remarks, int frameIdx) {

        if (!isAvailForSavings) {
            isAvailForSavings = true;
        }

        //TODO: add catch for invalid frameIdx
        map22.put(frameIdx, remarks);
        IJ.log("Remarks stored: " + remarks + "; frame: " + frameIdx);
        return true;
    }

    public boolean isDataForSavingAvailable() {
        return isAvailForSavings;
    }

    public void saveAsJson(String savePath) throws IOException {

        // Convert hashmap to json object
        JSONObject jsnobj = new JSONObject(map);

        //Save json object to jsonFile location
        try {
            // Constructs a FileWriter given a jsonFile name, using the platform's default charset
            jsonFile = new FileWriter(savePath);
            jsonFile.write(jsnobj.toJSONString());
            IJ.log("Time-tagged data saved");
//            IJ.log("\nJSON Object: " + jsnobj);
        } catch (IOException e) {
        } finally {
            try {
                jsonFile.flush();
                jsonFile.close();
            } catch (IOException e) {
            }
        }
    }

}
