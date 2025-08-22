package pcd.ass01;

import akka.actor.*;
import pcd.ass01.SimulationMessages.*;

import java.time.Duration;
import java.util.Optional;

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
                .match(StopApplication.class, this::onStopApplication)
                .match(SeparationChange.class, this::onParametersChange)
                .match(CohesionChange.class, this::onParametersChange)
                .match(AlignmentChange.class, this::onParametersChange)
                .match(SuspendResumeSimulation.class, this::onSuspendResumeSimulation)
                .match(RenderFrame.class, this::onRenderFrame)
                .match(StartSimulation.class, this::onStartGuardian)
                .match(ViewActorAttachment.class, this::onAttachView)
                .match(WorldReady.class, this::onWorldReady)
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
    }

    private void onWorldReady(WorldReady msg) {
        renderer.ifPresent((x) -> x.tell(msg, getSelf()));
        running = true;
        scheduleNextTick();
    }

    private void onStop(StopSimulation msg) {
        log("Stopping simulation");
        running = false;
        renderer.ifPresent(x -> x.tell(msg, getSelf()));
    }

    private void onStopApplication(StopApplication msg) {
        log("Stopping application");
        running = false;
        getContext().stop(world);
        getContext().stop(self());
        System.exit(0);
    }

    private void onAttachView(ViewActorAttachment msg){
        this.renderer = Optional.of(msg.view());
    }

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