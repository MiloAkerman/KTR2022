package burntherobot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.List;
import java.util.ArrayList;

public class Unit extends RobotPlayer {
    public static int homeID;
    public static MapLocation destination = null;
    public static MapLocation lastDestination = null;
    public static boolean hasReachedDestination = false;
    public static int roleIndex;
    public static int lastDirIndex = -1;

    public static void readPulse() throws GameActionException {
        int rawInt = rc.readSharedArray(4);
        roleIndex = rawInt & 0b1111;
        homeID = rawInt>>4;
        System.out.println("Read " + homeID);
    }

    public static void djikstraWander() throws GameActionException {
        List<Direction> allowedDirs = new ArrayList<>();
        for(int i = 0; i < Constants.DIRECTIONS.length; i++) {
            if (i != lastDirIndex) {
                allowedDirs.add(Constants.DIRECTIONS[i].opposite());
            }
        }

        Direction dir = allowedDirs.get(rng.nextInt(8));
        if(rc.canMove(dir)) rc.move(dir);
    }

    public static void djikstraBiasedStep(Direction dir) throws GameActionException {
        if(rc.canMove(dir)) rc.move(dir);
    }

    public static void movement() throws GameActionException {
        if(destination != null) {
            if(!destination.equals(lastDestination)) {
                hasReachedDestination = false;
            }
            if(!hasReachedDestination) {
                Direction dir = rc.getLocation().directionTo(destination);
                if(rc.canMove(dir)) {
                    rc.move(dir);
                } else {
                    djikstraWander();
                }
                lastDestination = destination;
            }
            if(rc.getLocation().distanceSquaredTo(destination) == 0 && !hasReachedDestination) {
                hasReachedDestination = true;
            }
        }
    }
}
