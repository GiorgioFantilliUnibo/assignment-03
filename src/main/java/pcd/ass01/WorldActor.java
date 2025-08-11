package pcd.ass01;

import akka.actor.*;
import pcd.ass01.SimulationMessages.*;

import java.util.*;


public class WorldActor extends AbstractActor {

    private final ActorRef renderer; // view actor
    private final List<ActorRef> boidActors = new ArrayList<>();
    private final Map<Integer, BoidState> currentStates = new HashMap<>();
    private long currentTick = 0;

    // Parametri di simulazione
    private double sepW, aliW, cohW;
    private double perceptionRadius, avoidRadius, maxSpeed;
    private double height, width;

    // Stato interno per sincronizzazione
    private enum Phase { IDLE, UPDATING_VELOCITIES, UPDATING_POSITIONS }
    private Phase phase = Phase.IDLE;
    private int pendingResponses = 0;

    public WorldActor(ActorRef renderer) {
        this.renderer = renderer;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::onStartSimulation)
                .match(Tick.class, this::onTick)
                .match(BoidUpdate.class, this::onBoidUpdate)
                .build();
    }

    private void onStartSimulation(StartSimulation msg) {
        log("Starting simulation with " + msg.nBoids() + " boids");

        this.sepW = msg.sepW();
        this.aliW = msg.aliW();
        this.cohW = msg.cohW();
        this.perceptionRadius = msg.perceptionRadius();
        this.avoidRadius = msg.avoidRadius();
        this.maxSpeed = msg.maxSpeed();
        this.width = msg.width();
        this.height = msg.height();

        // Creazione boid attori
        boidActors.clear();
        currentStates.clear();
        for (int i = 0; i < msg.nBoids(); i++) {
            P2d pos = new P2d(-width/2 + Math.random() * width, -height/2 + Math.random() * height);
            V2d vel = new V2d(Math.random() * maxSpeed/2 - maxSpeed/4, Math.random() * maxSpeed/2 - maxSpeed/4);
            BoidState initState = new BoidState(i, pos, vel);
            currentStates.put(i, initState);
            ActorRef boid = getContext().actorOf(Props.create(BoidActor.class, i, getSelf()), "boid-" + i);
            boidActors.add(boid);
        }

    }

    private void onTick(Tick msg) {
        if (phase != Phase.IDLE) {
            log("Ignoring Tick, still processing previous step");
            return;
        }
        currentTick++;
        phase = Phase.UPDATING_VELOCITIES;

        // Manda snapshot a tutti i boid per aggiornare velocità
        List<BoidState> statesList = new ArrayList<>(currentStates.values());
        Snapshot snap = new Snapshot(currentTick, statesList, sepW, aliW, cohW,
                perceptionRadius, avoidRadius, maxSpeed);

        for (ActorRef boid : boidActors) {
            boid.tell(snap, getSelf());
        }

        // Passaggio immediato alla fase 2: dopo aver inviato snapshot,
        // mandiamo un Tick a tutti per aggiornare le posizioni.
        phase = Phase.UPDATING_POSITIONS;
        pendingResponses = boidActors.size();
        for (ActorRef boid : boidActors) {
            boid.tell(new Tick(width, height, -width/2, width/2, -height/2, height/2), getSelf());
        }
    }

    /**
     * Riceve aggiornamenti di posizione dai boid (FASE 2).
     */
    private void onBoidUpdate(BoidUpdate upd) {
        currentStates.put(upd.id(),
                new BoidState(upd.id(), upd.pos(), upd.vel()));

        pendingResponses--;
        if (pendingResponses == 0 && phase == Phase.UPDATING_POSITIONS) {
            // Tutti i boid hanno aggiornato posizione → invia frame al renderer
            renderer.tell(new RenderFrame(currentTick, new ArrayList<>(currentStates.values())), getSelf());
            phase = Phase.IDLE;
        }
    }

    private void log(String msg) {
        System.out.println("[WorldActor] " + msg);
    }
}