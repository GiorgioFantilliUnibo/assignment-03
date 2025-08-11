package pcd.ass01;

import java.util.List;

public class SimulationMessages {

    private SimulationMessages() {}

    public interface Command {}

    public static record StartSimulation(
            int nBoids,
            double sepW, double aliW, double cohW,
            double perceptionRadius, double avoidRadius, double maxSpeed,
            long tickMs,
            double width, double height
    ) implements Command {}

    public static record startGuardian()

    public static record PauseSimulation() implements Command {}
    public static record ResumeSimulation() implements Command {}
    public static record StopSimulation() implements Command {}

    public static record Tick(
           double width, double height, double minX, double maxX, double minY, double maxY
    ) implements Command {}

    public static record Snapshot(
            long tickId,
            List<BoidState> states,
            double sepW, double aliW, double cohW,
            double perceptionRadius, double avoidRadius, double maxSpeed
    ) implements Command {}

    public static record BoidUpdate(int id, P2d pos, V2d vel) implements Command {}

    public static record RenderFrame(long tickId, List<BoidState> states) implements Command {}

    public static record BoidState(int id, P2d pos, V2d vel) {}

}
