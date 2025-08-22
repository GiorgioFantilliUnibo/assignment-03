package pcd.ass01;

import akka.actor.*;
import pcd.ass01.SimulationMessages.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Optional;

public class ViewActor extends AbstractActor implements ChangeListener {

    private final ActorRef guardianActor;

    private final JFrame frame;
    private final InitialPanel initialPanel;
    private SimulationPanel simulationPanel;
    private final int width, height;
    private double environmentWidth;

    private boolean paused = false;
    private int frameRate = 0;
    private long lastFrameTime = System.currentTimeMillis();

    private Optional<Integer> nBoids = Optional.empty();

    public ViewActor(ActorRef guardianActor, int width, int height, int environmentWidth) {
        this.guardianActor = guardianActor;
        this.width = width;
        this.height = height;
        this.environmentWidth = environmentWidth;

        frame = new JFrame("Boids Simulation");
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initialPanel = new InitialPanel(this);
        frame.setContentPane(initialPanel);
        frame.setVisible(true);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorldReady.class, this::onStartRendering)
                .match(PauseSimulation.class, this::onPauseSimulation)
                .match(ResumeSimulation.class, this::onResumeSimulation)
                .match(StopSimulation.class, this::onStopSimulation)
                .match(RenderFrame.class, this::onRenderFrame)
                .build();
    }

    private void onStartRendering(WorldReady msg) {
        SwingUtilities.invokeLater(() -> {
            paused = false;
            frameRate = 0;
            showSimulationScreen(msg);
        });
    }

    private void onPauseSimulation(PauseSimulation msg) {
        SwingUtilities.invokeLater(() -> {
            paused = true;
            updateSuspendResumeButtonText("Resume");
        });
    }

    private void onResumeSimulation(ResumeSimulation msg) {
        SwingUtilities.invokeLater(() -> {
            paused = false;
            updateSuspendResumeButtonText("Suspend");
        });
    }

    private void onStopSimulation(StopSimulation msg) {
        SwingUtilities.invokeLater(() -> {
            paused = false;
            resetToInitialScreen();
        });
    }

    private void onRenderFrame(RenderFrame msg) {
        SwingUtilities.invokeLater(() -> {
            if (paused) return;

            long now = System.currentTimeMillis();
            if (now - lastFrameTime > 0) {
                frameRate = (int) (1000.0 / (now - lastFrameTime));
            }
            lastFrameTime = now;
            update(frameRate, msg);
        });
    }

    public void update(int frameRate, RenderFrame msg) {
        if (simulationPanel != null) {
            simulationPanel.update(frameRate, msg);
        }
    }

    public void updateSuspendResumeButtonText(String text) {
        if (simulationPanel != null) {
            simulationPanel.updateSuspendResumeButtonText(text);
        }
    }

    public void resetToInitialScreen() {
        frame.setContentPane(initialPanel);
        frame.revalidate();
        frame.repaint();
    }

    public void startSimulation(int nBoids) {
        this.nBoids = Optional.of(nBoids);
        guardianActor.tell(new StartSimulation(nBoids), getSelf());
    }

    public void showSimulationScreen(WorldReady msg) {
        this.nBoids.ifPresentOrElse(
                x -> simulationPanel = new SimulationPanel(this, environmentWidth, x, msg.states()),
                () -> { throw new IllegalStateException("nBoids is not set before showSimulationScreen."); }
        );
//        this.nBoids.ifPresentOrElse(x -> simulationPanel = new SimulationPanel(this, environmentWidth, x), IllegalStateException::new);
        frame.setContentPane(simulationPanel);
        frame.revalidate();
        frame.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (simulationPanel == null) return;
        JSlider source = (JSlider) e.getSource();
        double value = source.getValue() * 0.1;
        if (source == simulationPanel.getSeparationSlider()) {
            guardianActor.tell(new SeparationChange(value), getSelf());
        } else if (source == simulationPanel.getCohesionSlider()) {
            guardianActor.tell(new CohesionChange(value), getSelf());
        } else if (source == simulationPanel.getAlignmentSlider()) {
            guardianActor.tell(new AlignmentChange(value), getSelf());
        }
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public void toggleSuspendResume() {
        guardianActor.tell(new SuspendResumeSimulation(), getSelf());
    }

    public void stopSimulation() {
        guardianActor.tell(new StopSimulation(), getSelf());
    }
}
