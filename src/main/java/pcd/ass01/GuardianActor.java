package pcd.ass01;

import akka.actor.*;
import pcd.ass01.SimulationMessages.*;

import java.time.Duration;
import java.util.Optional;

/**
 * GuardianActor:
 * - Riceve comandi dalla GUI (Start, Pause, Resume, Stop)
 * - Crea il WorldActor e il RendererActor
 * - Gestisce il ciclo di simulazione (tick) senza Cancellable
 */
public class GuardianActor extends AbstractActor {

    private final static long TICK_MS = 1000/25;

    private ActorRef world;
    private Optional<ActorRef> renderer;
    private boolean running = false;
    private long tickMs = TICK_MS;

    public GuardianActor(ActorRef world){
        this.world = world;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StopSimulation.class, this::onStop)
                .match(SeparationChange.class, this::onParametersChange)
                .match(CohesionChange.class, this::onParametersChange)
                .match(AlignmentChange.class, this::onParametersChange)
                .match(SuspendResumeSimulation.class, this::onSuspendResumeSimulation)
                .match(RenderFrame.class, this::onRenderFrame)
                .match(StartSimulation.class, this::onStartGuardian)
                .match(ViewActorAttachment.class, this::onAttachView)
                .build();
    }

    private void onRenderFrame(RenderFrame renderFrame) {
        renderer.ifPresent((x) -> x.tell(renderFrame, getSelf()));
    }

    private void onParametersChange(Command msg) {
        world.tell(msg, getSelf());
    }

    private void onSuspendResumeSimulation(SuspendResumeSimulation msg) {
        log("Guardian starting simulation...");
        if (running) {
            log("Pausing simulation");
            running = false;
            renderer.ifPresent((x) -> x.tell(new PauseSimulation(), getSelf()));
        } else {
            log("Resuming simulation");
            running = true;
            renderer.ifPresent((x) -> x.tell(new ResumeSimulation(), getSelf()));
            scheduleNextTick();
        }
    }

    private void onStartGuardian(StartSimulation msg) {
        log("Guardian starting simulation...");

        world.tell(msg, getSelf());
        renderer.ifPresent((x) -> x.tell(msg, getSelf()));
        running = true;
        scheduleNextTick();
    }

    private void onStop(StopSimulation msg) {
        log("Stopping simulation");
        running = false;
        getContext().stop(world);
        renderer.ifPresent(x -> getContext().stop(renderer.get()));
    }

    private void onAttachView(ViewActorAttachment msg){
        this.renderer = Optional.of(msg.view());
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
                            world.tell(new TickGuardian(), getSelf());
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