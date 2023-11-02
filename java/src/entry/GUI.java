package entry;

/*
 * Created with NetBeans IDE 12.0
 * User: Daniel Y.K. Aik <daniel.aik@u.nus.edu> GitHub @danielaik
 * Date: Feb 2022
 */
import ij.IJ;
import ij.plugin.BrowserLauncher;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import directCameraReadout.DirectCapture;
import gpufitImFCS.GpufitImFCS;
import static version.VERSION.DCR_VERSION;

public class GUI extends JFrame implements ActionListener {

    Font font12 = new Font("Lucida Grande", Font.PLAIN, 12);

    private JPanel contentPane; //where everything is docked
    private JTabbedPane informationPane;
    private JButton linkBtn;
    private JLabel startTxt;
    private JButton startBtn;
    private DirectCapture dcrobj;
    private int isgpupresent = 0;

    public GUI() {

        //---------------------Check Nvidia GPU---------------------
        try {
            if (GpufitImFCS.isCudaAvailable()) {
                isgpupresent = 1;
            }
        } catch (Exception e) {
        }

        //---------------------Welcome GUI setup---------------------
        createWelcomeGUI();

        //---------------------Setup environment for camera---------------------
        createDCR();
    }

    // create window to trigger direct camera readout feature
    private void createDCR() {
        if (dcrobj == null) {
            dcrobj = new DirectCapture(isgpupresent);
        }
    }

    // create window to trigger welcome GUI
    private void createWelcomeGUI() {
        //---------------------GUI----------------------
        setSize(360, 400);//400, 585);
        setTitle("DCR " + DCR_VERSION);
        setResizable(false);
        setLocation(0, (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2 - getHeight() / 2));

        //---------------------Where everything is arranged----------------------
        contentPane = new JPanel();
        setContentPane(contentPane);
        SpringLayout sl_contentPane = new SpringLayout();
        contentPane.setLayout(sl_contentPane);

        //---------------------Handles information tab panel: About, Requirement----------------------
        informationPane = new JTabbedPane(JTabbedPane.BOTTOM);
        sl_contentPane.putConstraint(SpringLayout.SOUTH, informationPane, 300, SpringLayout.NORTH, contentPane);
        informationPane.setBorder(new TitledBorder(null, "Direct camera readout (231102)", TitledBorder.CENTER, TitledBorder.TOP, null, null));
        sl_contentPane.putConstraint(SpringLayout.NORTH, informationPane, 0, SpringLayout.NORTH, contentPane);
        sl_contentPane.putConstraint(SpringLayout.WEST, informationPane, 0, SpringLayout.WEST, contentPane);
        sl_contentPane.putConstraint(SpringLayout.EAST, informationPane, 0, SpringLayout.EAST, contentPane);
        contentPane.add(informationPane);
        informationPane.setFont(font12);

        //--------------About Panel-------------
        JPanel aboutPanel = new JPanel();
        aboutPanel.setFont(font12);
        informationPane.addTab("About", null, aboutPanel, null);
        SpringLayout sl_aboutPanel = new SpringLayout();
        aboutPanel.setLayout(sl_aboutPanel);

        JTextArea aboutTxt = new JTextArea();
        sl_aboutPanel.putConstraint(SpringLayout.SOUTH, aboutTxt, -70, SpringLayout.SOUTH, aboutPanel);
        aboutTxt.setFont(font12);
        sl_aboutPanel.putConstraint(SpringLayout.NORTH, aboutTxt, 5, SpringLayout.NORTH, aboutPanel);
        sl_aboutPanel.putConstraint(SpringLayout.WEST, aboutTxt, 5, SpringLayout.WEST, aboutPanel);
        sl_aboutPanel.putConstraint(SpringLayout.EAST, aboutTxt, -5, SpringLayout.EAST, aboutPanel);
        aboutTxt.setText("If you found it useful please refer to and cite\n\nArticle: \nAik DYK, Wohland T.\nMicroscope alignment using real-time\nImaging FCS. (2022). 10.1016/j.bpj.2022.06.009."
                + "\n\nThis plugin: \nhttps://doi.org/10.5281/zenodo.6685875.\n\n");
        aboutTxt.setLineWrap(true);
        aboutTxt.setEditable(false);
        aboutTxt.setBackground(getBackground());
        aboutPanel.add(aboutTxt);

        linkBtn = new JButton("Go to article");
        sl_aboutPanel.putConstraint(SpringLayout.NORTH, linkBtn, 5, SpringLayout.SOUTH, aboutTxt);
        sl_aboutPanel.putConstraint(SpringLayout.WEST, linkBtn, 25, SpringLayout.WEST, aboutPanel);
        sl_aboutPanel.putConstraint(SpringLayout.EAST, linkBtn, -25, SpringLayout.EAST, aboutPanel);
        linkBtn.setFont(font12);
        linkBtn.addActionListener(this);
        aboutPanel.add(linkBtn);

        //--------------Supported camera Panel-------------
        JPanel requirementPanel = new JPanel();
        requirementPanel.setFont(font12);
        informationPane.addTab("Supported camera", null, requirementPanel, null);
        SpringLayout sl_requirementPanel = new SpringLayout();
        requirementPanel.setLayout(sl_requirementPanel);

        JTextArea requirementTxt = new JTextArea();
        sl_requirementPanel.putConstraint(SpringLayout.SOUTH, requirementTxt, -5, SpringLayout.SOUTH, requirementPanel);
        requirementTxt.setFont(font12);
        sl_requirementPanel.putConstraint(SpringLayout.NORTH, requirementTxt, 5, SpringLayout.NORTH, requirementPanel);
        sl_requirementPanel.putConstraint(SpringLayout.WEST, requirementTxt, 5, SpringLayout.WEST, requirementPanel);
        sl_requirementPanel.putConstraint(SpringLayout.EAST, requirementTxt, -5, SpringLayout.EAST, requirementPanel);
        requirementTxt.setText("Suppored cameras:\n"
                + "\nAndor iXon 860 (\"DU860_BV\"), 888 (\"DU888_BV\"),\n"
                + "897 (\"DU897_BV\")"
                + "\n\nAndor Sona 11 (\"SONA-4BV11\")"
                + "\n\nHamamatsu ORCA Flash (\"C13440-20C, C11440-22CU, \n"
                + "C11440-22C, C13440-20CU\"), QUEST (\"C15550-20UP\") "
                + "\n\nPhotometrics Evolve 512 (\"EVOLVE- 512\"), \n"
                + "Prime 95B (\"GS144BSI\"), Kinetix (\"TMP-Kinetix\")");
        requirementTxt.setLineWrap(true);
        requirementTxt.setEditable(false);
        requirementTxt.setBackground(getBackground());
        requirementPanel.add(requirementTxt);

        //--------------Runtime dependencies Panel-------------
        JPanel runtimeDependenciesPanel = new JPanel();
        runtimeDependenciesPanel.setFont(font12);
        informationPane.addTab("Runtime dependencies", null, runtimeDependenciesPanel, null);
        SpringLayout sl_runtimeDependenciesPanel = new SpringLayout();
        runtimeDependenciesPanel.setLayout(sl_runtimeDependenciesPanel);

        JTextArea runtimeDependenciesTxt = new JTextArea();
        sl_runtimeDependenciesPanel.putConstraint(SpringLayout.SOUTH, runtimeDependenciesTxt, -5, SpringLayout.SOUTH, runtimeDependenciesPanel);
        runtimeDependenciesTxt.setFont(font12);
        sl_runtimeDependenciesPanel.putConstraint(SpringLayout.NORTH, runtimeDependenciesTxt, 5, SpringLayout.NORTH, runtimeDependenciesPanel);
        sl_runtimeDependenciesPanel.putConstraint(SpringLayout.WEST, runtimeDependenciesTxt, 5, SpringLayout.WEST, runtimeDependenciesPanel);
        sl_runtimeDependenciesPanel.putConstraint(SpringLayout.EAST, runtimeDependenciesTxt, -5, SpringLayout.EAST, runtimeDependenciesPanel);
        runtimeDependenciesTxt.setText("Fiji // jars:\n"
                + "\n(1) commons-collections4-4.4.jar"
                + "\n(2) poi-ooxml-schemas-3.17.jar"
                + "\n(3) xmlbeans-5.1.3.jar"
                + "\n(4) json-simple-1.1.1.jar"
                + "\n(5) ij-1.53f.jar"
                + "\n(6) imagescience.jar"
                + "\n(7) commons-math3-3.6.1.jar"
                + "\n(8) poi-3.17.jar"
                + "\n(9) poi-ooxml-3.17.jar");

        runtimeDependenciesTxt.setLineWrap(true);
        runtimeDependenciesTxt.setEditable(false);
        runtimeDependenciesTxt.setBackground(getBackground());
        runtimeDependenciesPanel.add(runtimeDependenciesTxt);

        //--------------Version Panel-------------
        JPanel versionPanel = new JPanel();
        versionPanel.setFont(font12);
        informationPane.addTab("Runtime dependencies", null, versionPanel, null);
        SpringLayout sl_versionPanel = new SpringLayout();
        versionPanel.setLayout(sl_versionPanel);

        JTextArea versionTxt = new JTextArea();
        sl_versionPanel.putConstraint(SpringLayout.SOUTH, versionTxt, -5, SpringLayout.SOUTH, versionPanel);
        versionTxt.setFont(font12);
        sl_versionPanel.putConstraint(SpringLayout.NORTH, versionTxt, 5, SpringLayout.NORTH, versionPanel);
        sl_versionPanel.putConstraint(SpringLayout.WEST, versionTxt, 5, SpringLayout.WEST, versionPanel);
        sl_versionPanel.putConstraint(SpringLayout.EAST, versionTxt, -5, SpringLayout.EAST, versionPanel);
        versionTxt.setText("Version:\n"
                + "\nDCR: " + version.VERSION.DCR_VERSION
                + "\nAndor SDK2: " + version.VERSION.SDK2_VERSION
                + "\nAndor SDK3: " + version.VERSION.SDK3_VERSION
                + "\nHAMAMATSU SDK: " + version.VERSION.HAMASDK_VERSION
                + "\nPVCAM: " + version.VERSION.PVCAMSDK4_VERSION
                + "\nGPUFIT: " + version.VERSION.GPUFIT_VERSION);

        versionTxt.setLineWrap(true);
        versionTxt.setEditable(false);
        versionTxt.setBackground(getBackground());
        versionPanel.add(versionTxt);

        //--------------Launch Panel-------------
        JPanel launchPanel = new JPanel();
        sl_contentPane.putConstraint(SpringLayout.NORTH, launchPanel, 0, SpringLayout.SOUTH, informationPane);
        sl_contentPane.putConstraint(SpringLayout.SOUTH, launchPanel, 0, SpringLayout.SOUTH, contentPane);
        launchPanel.setFont(font12);
        sl_contentPane.putConstraint(SpringLayout.WEST, launchPanel, 0, SpringLayout.WEST, contentPane);
        sl_contentPane.putConstraint(SpringLayout.EAST, launchPanel, 0, SpringLayout.EAST, contentPane);
        contentPane.add(launchPanel);
        SpringLayout sl_launchPanel = new SpringLayout();
        launchPanel.setLayout(sl_launchPanel);

        startTxt = new JLabel();
        startTxt.setFont(font12);
        startTxt.setHorizontalAlignment(SwingConstants.CENTER);
        sl_launchPanel.putConstraint(SpringLayout.NORTH, startTxt, 5, SpringLayout.NORTH, launchPanel);
        sl_launchPanel.putConstraint(SpringLayout.WEST, startTxt, 0, SpringLayout.WEST, launchPanel);
        sl_launchPanel.putConstraint(SpringLayout.EAST, startTxt, 0, SpringLayout.EAST, launchPanel);
        startTxt.setText("Press 'Start' to turn on camera");
        startTxt.setForeground(Color.RED);
        startTxt.setBackground(getBackground());
        launchPanel.add(startTxt);

        startBtn = new JButton("Start");
        sl_launchPanel.putConstraint(SpringLayout.NORTH, startBtn, 5, SpringLayout.SOUTH, startTxt);
        sl_launchPanel.putConstraint(SpringLayout.WEST, startBtn, 100, SpringLayout.WEST, launchPanel);
        sl_launchPanel.putConstraint(SpringLayout.SOUTH, startBtn, -5, SpringLayout.SOUTH, launchPanel);
        sl_launchPanel.putConstraint(SpringLayout.EAST, startBtn, -100, SpringLayout.EAST, launchPanel);
        startBtn.setFont(font12);
        startBtn.addActionListener(this);
        launchPanel.add(startBtn);

        //---------------------Initialize---------------------
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        Object origin = arg0.getSource();
        if (origin == linkBtn) {
            try {
                BrowserLauncher.openURL("https://doi.org/10.1016/j.bpj.2022.06.009");
            } catch (IOException ie) {
            }
        }

        if (origin == startBtn) {

            if (dcrobj == null) {
                return;
            }
            //check if computer running Windows OS
            boolean proceed = true;
            String osname = System.getProperty("os.name");
            String osnamelc = osname.toLowerCase();
            if (!osnamelc.contains("win")) {
                proceed = false;
            }
            if (proceed) {
                dcrobj.check();
            } else {
                IJ.showMessage("Direct Capture only supported in Windows");
            }

        }
    }

}
