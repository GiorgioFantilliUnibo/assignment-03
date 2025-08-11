package pcd.ass01;

import akka.actor.AbstractActor;
import pcd.ass01.SimulationMessages.*;

import java.util.ArrayList;
import java.util.List;

public class BoidActor extends AbstractActor {

    private final int id;
    private P2d pos;
    private V2d vel;


    public BoidActor(int id, P2d pos, V2d vel) {
        this.id = id;
        this.pos = pos;
        this.vel = vel;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Snapshot.class, this::onSnapshot)
                .match(Tick.class, this::onTick)
                .build();
    }

    private void onSnapshot(Snapshot snap) {
        updateVelocityFromSnapshot(snap);
    }

    private void onTick(Tick msg) {
        updatePosition(msg);
        // Manda stato aggiornato al world
        getSender().tell(new BoidUpdate(id, pos, vel), getSelf());
    }

    /* --------- LOGICA VELOCITÀ E POSIZIONE --------- */

    private void updateVelocityFromSnapshot(Snapshot snap) {
        List<BoidState> nearbyBoids = getNearbyBoids(snap);

        V2d separation = calculateSeparation(nearbyBoids, snap);
        V2d alignment = calculateAlignment(nearbyBoids);
        V2d cohesion = calculateCohesion(nearbyBoids);

        vel = vel.sum(alignment.mul(snap.aliW()))
                .sum(separation.mul(snap.sepW()))
                .sum(cohesion.mul(snap.cohW()));

        // limita velocità
        double speed = vel.abs();
        if (speed > snap.maxSpeed()) {
            vel = vel.getNormalized().mul(snap.maxSpeed());
        }
    }

    private void updatePosition(Tick msg) {
        pos = pos.sum(vel);

        /* environment wrap-around */
        if (pos.x() < msg.minX()) pos = pos.sum(new V2d(msg.width(), 0));
        if (pos.x() >= msg.maxX()) pos = pos.sum(new V2d(-msg.width(), 0));
        if (pos.y() < msg.minY()) pos = pos.sum(new V2d(0, msg.height()));
        if (pos.y() >= msg.maxY()) pos = pos.sum(new V2d(0, -msg.height()));
    }

    /* --------- METODI DI CALCOLO --------- */

    private List<BoidState> getNearbyBoids(Snapshot snap) {
        var list = new ArrayList<BoidState>();
        for (BoidState other : snap.states()) {
            if (other.id() != this.id) {
                double distance = pos.distance(other.pos());
                if (distance < snap.perceptionRadius()) {
                    list.add(other);
                }
            }
        }
        return list;
    }

    private V2d calculateAlignment(List<BoidState> nearbyBoids) {
        if (nearbyBoids.isEmpty()) return new V2d(0, 0);

        double avgVx = 0, avgVy = 0;
        for (BoidState other : nearbyBoids) {
            avgVx += other.vel().x();
            avgVy += other.vel().y();
        }
        avgVx /= nearbyBoids.size();
        avgVy /= nearbyBoids.size();

        return new V2d(avgVx - vel.x(), avgVy - vel.y()).getNormalized();
    }

    private V2d calculateCohesion(List<BoidState> nearbyBoids) {
        if (nearbyBoids.isEmpty()) return new V2d(0, 0);

        double centerX = 0, centerY = 0;
        for (BoidState other : nearbyBoids) {
            centerX += other.pos().x();
            centerY += other.pos().y();
        }
        centerX /= nearbyBoids.size();
        centerY /= nearbyBoids.size();

        return new V2d(centerX - pos.x(), centerY - pos.y()).getNormalized();
    }

    private V2d calculateSeparation(List<BoidState> nearbyBoids, Snapshot snap) {
        double dx = 0, dy = 0;
        int count = 0;
        for (BoidState other : nearbyBoids) {
            double distance = pos.distance(other.pos());
            if (distance < snap.avoidRadius()) {
                dx += pos.x() - other.pos().x();
                dy += pos.y() - other.pos().y();
                count++;
            }
        }
        if (count > 0) {
            dx /= count;
            dy /= count;
            return new V2d(dx, dy).getNormalized();
        } else {
            return new V2d(0, 0);
        }
    }
}