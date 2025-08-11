package pcd.ass01;

import akka.actor.*;
import pcd.ass01.SimulationMessages.*;

import java.time.Duration;

/**
 * GuardianActor:
 * - Riceve comandi dalla GUI (Start, Pause, Resume, Stop)
 * - Crea il WorldActor e il RendererActor
 * - Gestisce il ciclo di simulazione (tick) senza Cancellable
 */
public class GuardianActor extends AbstractActor {

    private ActorRef world;
    private ActorRef renderer;
    private boolean running = false;
    private long tickMs;

    public GuardianActor(ActorRef world){
        this.world = world;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartSimulation.class, this::onStart)
                .match(PauseSimulation.class, this::onPause)
                .match(ResumeSimulation.class, this::onResume)
                .match(StopSimulation.class, this::onStop)
                .build();
    }

    private void onStart(StartSimulation msg) {
        log("Guardian starting simulation...");

        world.tell(msg, getSelf());
        this.tickMs = msg.tickMs();
        running = true;
        scheduleNextTick();
    }

    private void onPause(PauseSimulation msg) {
        if (running) {
            log("Pausing simulation");
            running = false;
        }
    }

    private void onResume(ResumeSimulation msg) {
        if (!running) {
            log("Resuming simulation");
            running = true;
            scheduleNextTick();
        }
    }

    private void onStop(StopSimulation msg) {
        log("Stopping simulation");
        running = false;
        getContext().stop(world);
        getContext().stop(renderer);
    }

    /**
     * Pianifica il prossimo tick solo se la simulazione è in esecuzione.
     */
    private void scheduleNextTick() {
        if (running) {
            getContext().getSystem().scheduler().scheduleOnce(
                    Duration.ofMillis(tickMs),
                    () -> {
                        if (running && world != null) {
                            world.tell(new Tick(), getSelf());
                            scheduleNextTick();
                        }
                    },
                    getContext().getSystem().dispatcher()
            );
        }
    }

    private void log(String msg) {
        System.out.println("[GuardianActor] " + msg);
    }
}