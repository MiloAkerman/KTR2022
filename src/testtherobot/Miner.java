package testtherobot;

import battlecode.common.*;
import java.util.*;

enum MinerRole {
    MINE,
    WANDER,
    SCOUT
    /*
    SCOUT_N,
    SCOUT_NE,
    SCOUT_E,
    SCOUT_SE,
    SCOUT_S,
    SCOUT_SW,
    SCOUT_W,
    SCOUT_NW
     */
}

public class Miner extends Unit {
    static MinerRole currRole;
    static Direction scoutDirection;
    static boolean active = false;
    static int wanderlust = Constants.MIN_MINER_WANDER; // this could have been a simple frame counter but this is more realistic.

    public static void setup() throws GameActionException {
        readPulse();
        if(roleIndex > 1) {
            scoutDirection = Constants.DIRECTIONS[roleIndex-2];
        }
        currRole = MinerRole.values()[roleIndex];
        rc.setIndicatorDot(new MapLocation(1, 58), 255, 255, 255);
    }

    public static void run() throws GameActionException {
        rc.move(Direction.WEST);
        rc.setIndicatorString("Hey!");
    }
}
