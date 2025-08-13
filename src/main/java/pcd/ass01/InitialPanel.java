
package pcd.ass01;

import javax.swing.*;
import java.awt.*;

public class InitialPanel extends JPanel {

    public InitialPanel(ViewActor view) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel label = new JLabel("Number of Boids:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);

        JTextField boidsInput = new JTextField(10);
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(boidsInput, gbc);

        JButton startButton = getjButton(view, boidsInput);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        add(startButton, gbc);
    }

    private JButton getjButton(ViewActor view, JTextField boidsInput) {
        JButton startButton = new JButton("Start simulation");
        startButton.addActionListener(e -> {
            try {
                int numBoids = Integer.parseInt(boidsInput.getText());
                if (numBoids > 0) {
                    view.startSimulation(numBoids);
                } else {
                    JOptionPane.showMessageDialog(this, "Please enter a positive number");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number");
            }
        });
        return startButton;
    }
}
