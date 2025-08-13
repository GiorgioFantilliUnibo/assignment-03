package pcd.ass01;

import akka.actor.AbstractActor;
import pcd.ass01.SimulationMessages.*;

import java.util.ArrayList;
import java.util.List;

public class BoidActor extends AbstractActor {

    public static class BoidState {
        private final int id;
        private P2d pos;
        private V2d vel;

        public BoidState(int id, P2d pos, V2d vel) {
            this.id = id;
            this.pos = pos;
            this.vel = vel;
        }

        public int id() {
            return id;
        }

        public P2d pos() {
            return new P2d(pos.x(), pos.y()); 
        }

        public void setPos(double x, double y) {
            this.pos = new P2d(x, y);
        }

        public void setPos(P2d pos) {
            this.pos = new P2d(pos.x(), pos.y());
        }

        public V2d vel() {
            return new V2d(vel.x(), vel.y());
        }

        public void setVel(double x, double y) {
            this.vel = new V2d(x, y);
        }

        public void setVel(V2d vel) {
            this.vel = new V2d(vel.x(), vel.y());
        }
    }


    private final BoidState state;

    public BoidActor(BoidState state) {
        this.state = state;
    }

    public BoidState getState() {
        return this.state;
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
        getSender().tell(new BoidUpdate(this.state.id, this.state.pos, this.state.vel), getSelf());
    }

    /* --------- LOGICA VELOCITÀ E POSIZIONE --------- */

    private void updateVelocityFromSnapshot(Snapshot snap) {
        List<BoidState> nearbyBoids = getNearbyBoids(snap);

        V2d separation = calculateSeparation(nearbyBoids, snap);
        V2d alignment = calculateAlignment(nearbyBoids);
        V2d cohesion = calculateCohesion(nearbyBoids);

        this.state.setVel(
            this.state.vel()
                .sum(alignment.mul(snap.aliW()))
                .sum(separation.mul(snap.sepW()))
                .sum(cohesion.mul(snap.cohW()))
        );

        // limita velocità
        double speed = this.state.vel.abs();
        if (speed > snap.maxSpeed()) {
            this.state.vel = this.state.vel.getNormalized().mul(snap.maxSpeed());
        }
    }

    private void updatePosition(Tick msg) {
        this.state.setPos(
            this.state.pos().sum(this.state.vel())
        );

        /* environment wrap-around */
        if (this.state.pos.x() < msg.minX()) this.state.setPos( this.state.pos.sum(new V2d(msg.width(), 0)) );
        if (this.state.pos.x() >= msg.maxX()) this.state.setPos( this.state.pos.sum(new V2d(-msg.width(), 0)) );
        if (this.state.pos.y() < msg.minY()) this.state.setPos( this.state.pos.sum(new V2d(0, msg.height())) );
        if (this.state.pos.y() >= msg.maxY()) this.state.setPos( this.state.pos.sum(new V2d(0, -msg.height())) );
    }

    /* --------- METODI DI CALCOLO --------- */

    private List<BoidState> getNearbyBoids(Snapshot snap) {
        var list = new ArrayList<BoidState>();
        for (BoidState other : snap.states()) {
            if (other.id() != this.state.id) {
                double distance = this.state.pos.distance(other.pos());
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

        return new V2d(avgVx - this.state.vel.x(), avgVy - this.state.vel.y()).getNormalized();
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

        return new V2d(centerX - this.state.pos.x(), centerY - this.state.pos.y()).getNormalized();
    }

    private V2d calculateSeparation(List<BoidState> nearbyBoids, Snapshot snap) {
        double dx = 0, dy = 0;
        int count = 0;
        for (BoidState other : nearbyBoids) {
            double distance = this.state.pos.distance(other.pos());
            if (distance < snap.avoidRadius()) {
                dx += this.state.pos.x() - other.pos().x();
                dy += this.state.pos.y() - other.pos().y();
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