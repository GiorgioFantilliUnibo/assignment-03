package pcd.ass01;

import akka.actor.*;
import pcd.ass01.BoidActor.BoidState;
import pcd.ass01.SimulationMessages.*;

import java.util.*;


public class WorldActor extends AbstractActor {

    private Optional<ActorRef> guardian;
    private final List<ActorRef> boidActors = new ArrayList<>();
    private final Map<Integer, BoidState> currentStates = new HashMap<>();
    private long currentTick = 0;

    private double sepW, aliW, cohW;
    private double perceptionRadius, avoidRadius, maxSpeed;
    private double height, width;

    private enum Phase { IDLE, UPDATING_VELOCITIES, UPDATING_POSITIONS }
    private Phase phase = Phase.IDLE;
    private int pendingResponses = 0;


    public WorldActor(double aliW, double cohW, double perceptionRadius, double maxSpeed, double avoidRadius, int height, int width, double sepW) {
        this.aliW = aliW;
        this.cohW = cohW;
        this.perceptionRadius = perceptionRadius;
        this.maxSpeed = maxSpeed;
        this.avoidRadius = avoidRadius;
        this.height = height;
        this.width = width;
        this.sepW = sepW;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::onStartSimulation)
                .match(TickGuardian.class, this::onTick)
                .match(BoidUpdate.class, this::onBoidUpdate)
                .match(SeparationChange.class, this::onSeparationChange)
                .match(CohesionChange.class, this::onCohesionChange)
                .match(AlignmentChange.class, this::onAlignmentChange)
                .match(GuardianActorAttachment.class, this::onGuardianActorAttachment)
                .build();
    }

    private void onAlignmentChange(AlignmentChange alignmentChange) {
        this.aliW = alignmentChange.value();
    }

    private void onCohesionChange(CohesionChange cohesionChange) {
        this.cohW = cohesionChange.value();
    }

    private void onSeparationChange(SeparationChange separationChange) {
        this.sepW = separationChange.value();
    }

    private void onStartSimulation(StartSimulation msg) {
        log("Starting simulation with " + msg.nBoids() + " boids");

        boidActors.clear();
        currentStates.clear();
        for (int i = 0; i < msg.nBoids(); i++) {
            P2d pos = new P2d(-width/2 + Math.random() * width, -height/2 + Math.random() * height);
            V2d vel = new V2d(Math.random() * maxSpeed/2 - maxSpeed/4, Math.random() * maxSpeed/2 - maxSpeed/4);
            BoidState initState = new BoidState(i, pos, vel);
            currentStates.put(i, initState);
            ActorRef boid = getContext().actorOf(Props.create(BoidActor.class, initState));
            boidActors.add(boid);
        }
        guardian.ifPresent(x -> x.tell(new WorldReady(new ArrayList<>(currentStates.values())), getSelf()));

    }

    private void onTick(TickGuardian msg) {
        if (phase != Phase.IDLE) {
            log("Ignoring Tick, still processing previous step");
            return;
        }
        currentTick++;
        phase = Phase.UPDATING_VELOCITIES;

        List<BoidState> statesList = new ArrayList<>(currentStates.values());
        Snapshot snap = new Snapshot(currentTick, statesList, sepW, aliW, cohW,
                perceptionRadius, avoidRadius, maxSpeed);

        for (ActorRef boid : boidActors) {
            boid.tell(snap, getSelf());
        }

        phase = Phase.UPDATING_POSITIONS;
        pendingResponses = boidActors.size();
        for (ActorRef boid : boidActors) {
            boid.tell(new Tick(width, height, -width/2, width/2, -height/2, height/2), getSelf());
        }
    }

    private void onBoidUpdate(BoidUpdate upd) {
        currentStates.put(upd.id(),
                new BoidState(upd.id(), upd.pos(), upd.vel()));

        pendingResponses--;
        if (pendingResponses == 0 && phase == Phase.UPDATING_POSITIONS) {
            System.out.println("Boidstate lenght: " + boidActors.size());
            guardian.ifPresent((x) -> x.tell(new RenderFrame(currentTick, new ArrayList<>(currentStates.values())), getSelf()));
            phase = Phase.IDLE;
        }
    }

    private void log(String msg) {
        System.out.println("[WorldActor] " + msg);
    }

    private void onGuardianActorAttachment(GuardianActorAttachment msg){
        this.guardian = Optional.of(msg.guardian());
    }
}
