package testtherobot;

import battlecode.common.*;
import scala.collection.immutable.Stream;

public class Unit extends RobotPlayer {
    public static MapLocation home;
    public static MapLocation destination;
    public static int roleIndex;
    public static int lastDirIndex;

    public static void readPulse() throws GameActionException {
        int rawInt = rc.readSharedArray(4);
        roleIndex = rawInt & 0b1111;
        home = new MapLocation(((rawInt>>4)/width), ((rawInt>>4)%width));
    }

    public static void djikstraWander() throws GameActionException {
        rc.move(Constants.DIRECTIONS[rng.nextInt()]);
    }

    public static void djikstraBiasedStep(Direction dir) throws GameActionException {
        rc.move(dir);
    }
}
