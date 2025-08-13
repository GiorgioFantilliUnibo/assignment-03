package pcd.ass01;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import pcd.ass01.SimulationMessages.*;

public class BoidsSimulation {

	final static double SEPARATION_WEIGHT = 1.0;
    final static double ALIGNMENT_WEIGHT = 1.0;
    final static double COHESION_WEIGHT = 1.0;
    final static int ENVIRONMENT_WIDTH = 1000;
	final static int ENVIRONMENT_HEIGHT = 1000;
    static final double MAX_SPEED = 4.0;
    static final double PERCEPTION_RADIUS = 50.0;
    static final double AVOID_RADIUS = 20.0;

	final static int SCREEN_WIDTH = 800; 
	final static int SCREEN_HEIGHT = 800; 
	

    public static void main(String[] args) {
		ActorSystem system = ActorSystem.create("MySystem");
		ActorRef world = system.actorOf(Props.create(WorldActor.class,
				ALIGNMENT_WEIGHT,
				COHESION_WEIGHT,
				PERCEPTION_RADIUS,
				MAX_SPEED,
				AVOID_RADIUS,
				ENVIRONMENT_HEIGHT,
				ENVIRONMENT_WIDTH,
				SEPARATION_WEIGHT));

		ActorRef guardian = system.actorOf(Props.create(GuardianActor.class, world));
		ActorRef view = system.actorOf(Props.create(ViewActor.class, guardian, SCREEN_WIDTH, SCREEN_HEIGHT, ENVIRONMENT_HEIGHT));

		guardian.tell(new ViewActorAttachment(view), ActorRef.noSender());
		world.tell(new GuardianActorAttachment(guardian), ActorRef.noSender());
    }
}
