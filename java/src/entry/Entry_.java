package entry;

/*
 * Created with NetBeans IDE 12.0
 * User: Daniel Y.K. Aik <daniel.aik@u.nus.edu> GitHub @danielaik
 * Date: Feb 2022
 */
import ij.plugin.PlugIn;

public class Entry_ implements PlugIn {

    @Override
    public void run(String arg) {
        GUI frame = new GUI();
    }
}
