package com.github.cregrant.gui;

import javax.swing.*;
import java.awt.*;

public class Frame extends JFrame {

    public void MainFrame() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int frameHeight = screenSize.height * 2/3;
        int frameWidth = screenSize.width * 2/3;
        setBounds(200, 100, frameWidth, frameHeight);

        JPanel projectsPanel = new JPanel();
        projectsPanel.setLayout(new BoxLayout(projectsPanel, BoxLayout.Y_AXIS));

        projectsPanel.add(Box.createRigidArea(new Dimension(6,30)));
        projectsPanel.setBorder(BorderFactory.createTitledBorder("Picker project:"));

        String[] projects = {"Discord", "CoolCam", "Smth"};
        JList<String> list1 = new JList<>(projects);
        list1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        projectsPanel.add(new JScrollPane(list1));



        /*        JProgressBar progressBar = new ProgressBar().makeProgressBar(0, 100);
        progressBar.setForeground(Color.green);
        progressBar.setValue(33);
        projectsPanel.add(progressBar);*/
        add(projectsPanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new mainFrame());

    }
}

