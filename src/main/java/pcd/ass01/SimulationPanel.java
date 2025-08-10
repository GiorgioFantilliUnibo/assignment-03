
package pcd.ass01;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class SimulationPanel extends JPanel {
    private final BoidsPanel boidsPanel;
    private final JSlider cohesionSlider, separationSlider, alignmentSlider;
    private final JButton suspendResumeButton, stopButton;

    public SimulationPanel(BoidsView view, BoidsModel model) {
        setLayout(new BorderLayout());

        boidsPanel = new BoidsPanel(view, model);
        add(boidsPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JPanel slidersPanel = new JPanel();
        separationSlider = makeSlider("Separation", view);
        alignmentSlider = makeSlider("Alignment", view);
        cohesionSlider = makeSlider("Cohesion", view);
        
        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);
        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);
        controlPanel.add(slidersPanel);

        JPanel buttonPanel = new JPanel();
        suspendResumeButton = new JButton("Suspend");
        stopButton = new JButton("Stop");
        buttonPanel.add(suspendResumeButton);
        buttonPanel.add(stopButton);
        controlPanel.add(buttonPanel);

        add(controlPanel, BorderLayout.SOUTH);

        suspendResumeButton.addActionListener(e -> view.getSimulator().toggleSuspendResume());
        stopButton.addActionListener(e -> view.getSimulator().stopSimulation());
    }

    private JSlider makeSlider(String name, BoidsView view) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(20, new JLabel("2"));
        slider.setLabelTable(labelTable);
        slider.setPaintLabels(true);
        slider.addChangeListener(view);
        return slider;
    }

    public void update(int frameRate) {
        boidsPanel.setFrameRate(frameRate);
        boidsPanel.repaint();
    }

    public void updateSuspendResumeButtonText(String text) {
        suspendResumeButton.setText(text);
    }

    public JSlider getSeparationSlider() { return separationSlider; }
    public JSlider getCohesionSlider() { return cohesionSlider; }
    public JSlider getAlignmentSlider() { return alignmentSlider; }
}
