import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import static java.lang.System.out;

public class MainFrame extends JFrame{
    static void startFrame() {
        String appDir = System.getProperty("user.dir");
        out.println(appDir);
        Properties props = new Properties();
        final String settingsFilename = appDir + File.separator + "config" + File.separator + "conf.txt";
        out.println(settingsFilename);
        try {
            FileInputStream input = new FileInputStream(settingsFilename);
            props.load(input);
            input.close();
            out.println("Error loading conf");
        } catch(Exception ignore){}
        int savedX, savedY, savedWidth, savedHeight;
        try {
            savedX = Integer.parseInt(props.getProperty("xPos"));
            savedY = Integer.parseInt(props.getProperty("yPos"));
            savedWidth = Integer.parseInt(props.getProperty("frameWidth"));
            savedHeight = Integer.parseInt(props.getProperty("frameHeight"));
        } catch(NumberFormatException e) {
            out.println("Error reading conf");
            savedX = 100;
            savedY = 100;
            savedWidth = 500;
            savedHeight = 200;
        }
        final JFrame frame = new JFrame("WTF");
        frame.setBounds(savedX, savedY, savedWidth, savedHeight);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        String workingDir = System.getProperty("user.dir");
        String iconFilename = workingDir + File.separator +
                "res" + File.separator + "Icon32.png";
        ImageIcon icon = new ImageIcon(iconFilename);
        frame.setIconImage(icon.getImage());
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                Rectangle frameBounds = frame.getBounds();
                props.setProperty("xPos", String.valueOf(frameBounds.x));
                props.setProperty("yPos", String.valueOf(frameBounds.y));
                props.setProperty("width", String.valueOf(frameBounds.width));
                props.setProperty("height", String.valueOf(frameBounds.height));
                try {
                    FileOutputStream output = new FileOutputStream(settingsFilename);
                    props.store(output, "Saved settings");
                    output.close();
                    out.println("Conf wrote");
                } catch(Exception ignore) {
                    out.println("Error writing conf");}

                System.exit(0);
            }
        });
    }
}

