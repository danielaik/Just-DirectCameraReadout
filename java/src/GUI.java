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

        //---------------------GUI----------------------
        setSize(300, 400);//400, 585);
        setTitle("Direct Camera Readoiut v 19/02/22");
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
        informationPane.setBorder(new TitledBorder(null, "About", TitledBorder.CENTER, TitledBorder.TOP, null, null));
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
        aboutTxt.setText("Please refer to and cite:\n\nDYK Aik and Wohland T.\nMicroscope Alignment Using Real-Time\nImaging FCS.....\n\nDownloadable from:\nwww.");
        aboutTxt.setLineWrap(true);
        aboutTxt.setEditable(false);
        aboutTxt.setBackground(getBackground());
        aboutPanel.add(aboutTxt);

        linkBtn = new JButton("Go to BPJ");
        sl_aboutPanel.putConstraint(SpringLayout.NORTH, linkBtn, 5, SpringLayout.SOUTH, aboutTxt);
        sl_aboutPanel.putConstraint(SpringLayout.WEST, linkBtn, 25, SpringLayout.WEST, aboutPanel);
        sl_aboutPanel.putConstraint(SpringLayout.EAST, linkBtn, -25, SpringLayout.EAST, aboutPanel);
        linkBtn.setFont(font12);
        linkBtn.addActionListener(this);
        aboutPanel.add(linkBtn);

        //--------------Camera requirement Panel-------------
        JPanel requirementPanel = new JPanel();
        requirementPanel.setFont(font12);
        informationPane.addTab("Camera Requirements", null, requirementPanel, null);
        SpringLayout sl_requirementPanel = new SpringLayout();
        requirementPanel.setLayout(sl_requirementPanel);

        JTextArea requirementTxt = new JTextArea();
        sl_requirementPanel.putConstraint(SpringLayout.SOUTH, requirementTxt, -70, SpringLayout.SOUTH, requirementPanel);
        requirementTxt.setFont(font12);
        sl_requirementPanel.putConstraint(SpringLayout.NORTH, requirementTxt, 5, SpringLayout.NORTH, requirementPanel);
        sl_requirementPanel.putConstraint(SpringLayout.WEST, requirementTxt, 5, SpringLayout.WEST, requirementPanel);
        sl_requirementPanel.putConstraint(SpringLayout.EAST, requirementTxt, -5, SpringLayout.EAST, requirementPanel);
        requirementTxt.setText("Suppored cameras:\n\nAndor iXon 860, 888, 897\nAndor Sona 11\nHamamatsu ORCA Flash, QUEST \nPhotometrics Evolve 512, Prime 95B");
        requirementTxt.setLineWrap(true);
        requirementTxt.setEditable(false);
        requirementTxt.setBackground(getBackground());
        requirementPanel.add(requirementTxt);

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

        //---------------------Check Nvidia GPU---------------------
        try {
            if (GpufitImFCS.isCudaAvailable()) {
                isgpupresent = 1;
            }
        } catch (Exception e) {
        }
    }

//    public static void main(String[] args) {
//        EventQueue.invokeLater(() -> {
//            if (IJ.versionLessThan("1.52i")) {
//                return;
//            }
//
//            GUI frame = new GUI();
//        });
//    }
    @Override
    public void actionPerformed(ActionEvent arg0) {
        Object origin = arg0.getSource();
        if (origin == linkBtn) {
            try {
                BrowserLauncher.openURL("https://www.google.com");
            } catch (IOException ie) {
            }
        }

        if (origin == startBtn) {
            if (dcrobj == null) {
                dcrobj = new DirectCapture(isgpupresent);
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
