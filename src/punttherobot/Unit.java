package punttherobot;
import battlecode.common.*;

import static punttherobot.Constants.*;
import java.util.*;

enum PATH_END_REASON {
    SUCCESSFUL,
    TOO_MANY_BADDIES,
    TOO_MANY_FRIENDS
}
enum UNIT_MOOD {
    SAD,
    HAPPY
}

public class Unit extends RobotPlayer {
    public static int homeArchonID;
    public static int assignment;
    public static int addlInfo;
    public static Random trueRng = new Random();
    public static CallbackInterface pathCallback;

    static UNIT_MOOD mood = UNIT_MOOD.SAD;
    static String motivationalMessage;

    private static MapLocation destination = null;
    private static boolean goingToArchon = false;

    //------------------------ HELPER METHODS -----------------------------

    public static void readPulse() throws GameActionException {
        // no other way to get archon ID :/
        int lastTurn = Math.max(turnCount-1, 0);
        MapLocation loc = rc.getLocation().add(DIRECTIONS[lastTurn%8].opposite());
        int defaultArchonID = 3;
        if(rc.canSenseRobotAtLocation(loc)) defaultArchonID = rc.senseRobotAtLocation(loc).ID;
        homeArchonID = getBetterArchonID(defaultArchonID);
        int rawInt = rc.readSharedArray(Flag.INIT_PULSE.getValue()+homeArchonID);
        addlInfo = rawInt & 0b1111;
        assignment = rawInt>>4;

        motivationalMessage = motivationReference[rc.readSharedArray(Flag.MOTIVATION.getValue())];
        mood = UNIT_MOOD.HAPPY; // motivated
    }
    public static int getBetterArchonID(int defaultID) throws GameActionException {
        for(int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue()+4; i++) {
            if(rc.readSharedArray(i) == defaultID) return i;
        }
        System.out.println("Final return statement reached on getBetterArchonID. Fifth archon confirmed????");
        //rc.resign();
        return 0; // this should NEVER happen
    }

    // Pathfinding

    public static void movement(Direction bias) throws GameActionException {
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
            if(bestMove != null && rc.canMove(bestMove.getKey())) {
                if(bias != null && rc.canMove(bias)) {
                    // NO MORE BIAS. TODAY WE FIGHT
                    /*MapLocation origin = new MapLocation(0, 0);
                    Direction newDir = origin.directionTo(origin.add(bestMove.getKey()).add(bias));

                    int biasRubble = rc.senseRubble(currLoc.add(newDir));
                    int regRubble = rc.senseRubble(currLoc.add(bestMove.getKey()));

                    if(rc.canMove(newDir) && biasRubble < (regRubble * RUBBLE_BIAS_THRESHOLD)) {
                        rc.move(newDir);
                    } else {
                        rc.move(bestMove.getKey());
                    }*/
                    rc.move(bias);
                } else {
                    rc.move(bestMove.getKey());
                }
            }

            // are we close to our destination?
            finchecks: if(rc.canSenseLocation(destination)) {
                // are we AT our destination?
                if(rc.getLocation().distanceSquaredTo(destination) == 0) {
                    endPathfinding(PATH_END_REASON.SUCCESSFUL);
                    break finchecks;
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
        goingToArchon = false;
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
    public static int getMinersAtLocation(MapLocation loc, int radius) {
        RobotInfo[] robots = rc.senseNearbyRobots(loc, radius, myTeam);
        int count = 0;
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.MINER) count++;
        }
        return count;
    }
    public static MapLocation getBestAdjacentTile(MapLocation loc) throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        Pair<MapLocation, Integer> bestPos = null;
        for(Direction dir : DIRECTIONS) {
            if(rc.canSenseLocation(loc.add(dir)) && ( bestPos==null || loc.add(dir).distanceSquaredTo(currLoc) < bestPos.getValue())) {
                bestPos = new Pair<>(loc.add(dir), loc.add(dir).distanceSquaredTo(currLoc));
            }
        }
        return bestPos.getKey();
    }
}
