package pcd.ass01;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import pcd.ass01.BoidActor.BoidState;

public class BoidsPanel extends JPanel {

	private final ViewActor view;
    private final double nBoids;
    private final double width;
    private List<BoidState> boidStates;
    private int framerate;

    public BoidsPanel(ViewActor view, double width, int nBoids, List<BoidState> initialStates) {
        this.boidStates = initialStates;
        this.nBoids = nBoids;
        this.width = width;
    	this.view = view;
    }

    public void setFrameRate(int framerate) {
    	this.framerate = framerate;
    }

    public void setState(List<BoidState> boidStates){
        this.boidStates = boidStates;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);
        
        var w = view.getWidth();
        var h = view.getHeight();
        var xScale = w/ this.width;

        g.setColor(Color.BLUE);
        for (BoidState boid : boidStates) {
        	var x = boid.pos().x();
        	var y = boid.pos().y();
        	int px = (int)(w/2 + x*xScale);
        	int py = (int)(h/2 - y*xScale);
            g.fillOval(px,py, 5, 5);
        }
        
        g.setColor(Color.BLACK);
        g.drawString("Num. Boids: " + this.nBoids, 10, 25);
        g.drawString("Framerate: " + framerate, 10, 40);
   }
}
