package pcd.ass01;

import akka.actor.*;
import java.util.List;

public class SimulationMessages {

    private SimulationMessages() {}

    public interface Command {}

//    public static record StartSimulation(
//            int nBoids,
//            double sepW, double aliW, double cohW,
//            double perceptionRadius, double avoidRadius, double maxSpeed,
//            long tickMs,
//            double width, double height
//    ) implements Command {}

    public static record StartSimulation(
            int nBoids
    ) implements Command {}

    public static record TickGuardian() implements Command {}

    public static record SuspendResumeSimulation() implements Command {}
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

    public static record SeparationChange(double value) implements Command {}

    public static record CohesionChange(double value) implements Command {}

    public static record AlignmentChange(double value) implements Command {}

    public static record GuardianActorAttachment(ActorRef guardian) implements Command {}

    public static record ViewActorAttachment(ActorRef view) implements Command {}

}
