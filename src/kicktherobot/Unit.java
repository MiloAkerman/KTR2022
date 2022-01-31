package kicktherobot;
import battlecode.common.*;

import static kicktherobot.Constants.*;
import java.util.*;

enum PATH_END_REASON {
    SUCCESSFUL,
    TOO_MANY_BADDIES,
    TOO_MANY_FRIENDS
}

public class Unit extends RobotPlayer {
    public static int homeArchonID;
    public static int assignment;
    public static int addlInfo;
    public static CallbackInterface pathCallback;

    private static MapLocation destination = null;
    private static boolean goingToArchon = false;

    //------------------------ HELPER METHODS -----------------------------

    public static void readPulse() throws GameActionException {
        // no other way to get archon ID :/
        int lastTurn = Math.max(turnCount, 0);
        int defaultArchonID = rc.senseRobotAtLocation(rc.getLocation().add(DIRECTIONS[lastTurn%8].opposite())).ID;
        homeArchonID = getBetterArchonID(defaultArchonID);
        int rawInt = rc.readSharedArray(Flag.INIT_PULSE.getValue()+homeArchonID);
        addlInfo = rawInt & 0b1111;
        assignment = rawInt>>4;
    }
    public static int getBetterArchonID(int defaultID) throws GameActionException {
        for(int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue()+4; i++) {
            if(rc.readSharedArray(i) == defaultID) return i;
        }
        System.out.println("Final return statement reached on getBetterArchonID. Fifth archon confirmed????");
        rc.resign();
        return 0; // this should NEVER happen
    }

    // Pathfinding

    public static void movement(boolean cancelIfCrowded) throws GameActionException {
        if(goingToArchon) {
            int newLocRaw = rc.readSharedArray(Flag.ARCHON_INFO.getValue()+homeArchonID);
            MapLocation newLoc = new MapLocation(newLocRaw/width, newLocRaw%width);
            if(!newLoc.equals(destination)) {
                destination = newLoc;
            }
        }

        if(destination != null) {
            //greedy pathfinding
            MapLocation currLoc = rc.getLocation();

            Direction[] sortedDirs = DIRECTIONS.clone();
            Arrays.sort(sortedDirs, Comparator.comparingInt(dir -> currLoc.add(dir).distanceSquaredTo(destination)));

            int currDist = currLoc.distanceSquaredTo(destination);

            Pair<Direction, Integer> bestMove = null;
            for (Direction direction : sortedDirs) {
                MapLocation newLoc = currLoc.add(direction);
                int newDist = newLoc.distanceSquaredTo(destination);
                if (newDist < currDist && rc.canMove(direction)) {
                    int newLocRubble = rc.senseRubble(newLoc);
                    if (bestMove == null || newLocRubble < bestMove.getValue()) {
                        bestMove = new Pair<>(direction, newLocRubble);
                    }
                }
            }
            // sometimes the best choice is to do nothing
            if(bestMove != null && rc.canMove(bestMove.getKey())) rc.move(bestMove.getKey());

            // are we close to our destination?
            finchecks: if(rc.canSenseLocation(destination)) {
                // are we AT our destination?
                if(rc.getLocation().distanceSquaredTo(destination) == 0) {
                    endPathfinding(PATH_END_REASON.SUCCESSFUL);
                    break finchecks;
                }

                // ok, we're not there yet, but since we're close, is anyone there? (currently hardcoded for miners)
                if(cancelIfCrowded && getMinersAtLocation(destination) < TOO_MANY_MINERS) {
                    endPathfinding(PATH_END_REASON.TOO_MANY_FRIENDS);
                }
            }
        }
    }
    public static void tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)) rc.move(dir);
        else if(rc.canMove(dir.rotateLeft())) rc.move(dir.rotateLeft());
        else if(rc.canMove(dir.rotateRight())) rc.move(dir.rotateRight());
    }
    public static void setPathfinding(MapLocation location) {
        destination = location;
    }
    public static void setPathfindingToArchon() {
        goingToArchon = true;
    }
    public static MapLocation getPathfinding() {
        return destination;
    }
    public static void endPathfinding(PATH_END_REASON reason) throws GameActionException {
        destination = null;
        goingToArchon = false;
        pathCallback.onPathfindingEnd(reason);
    }
    public static int getMinersAtLocation(MapLocation loc) {
        RobotInfo[] robots = rc.senseNearbyRobots(loc, 20, myTeam);
        int count = 0;
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.MINER) count++;
        }
        return count;
    }
}
