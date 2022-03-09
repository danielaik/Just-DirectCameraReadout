/*
 * Created with NetBeans IDE 12.0
 * User: Daniel Y.K. Aik <daniel.aik@u.nus.edu> GitHub @danielaik
 * Date: Feb 2022
 */
import ij.IJ;
import ij.plugin.PlugIn;

public class DCR_ implements PlugIn {

    @Override
    public void run(String arg) {
        if (IJ.versionLessThan("1.52i")) {
            return;
        }
        GUI frame = new GUI();
    }

}
