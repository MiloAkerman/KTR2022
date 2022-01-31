package icbmtherobot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Jumper extends Soldier {
    MapLocation enemyArchonPos;
    static boolean seekingWall = true;
    static MapLocation movingTo = null;
    static MapLocation ultimateGoal = null;
    static boolean hasInit = false;

    public static void run() throws GameActionException {
        if(!hasInit) {
            System.out.println("Im jumper **********************************");
            ultimateGoal = getNearestWall(new MapLocation(assignment/width, assignment%width));
            handlePathfindingCallback();
            hasInit = true;
        }

        jumperMovement();
    }

    public static void jumperMovement() throws GameActionException {
        if(spawnTurnCount==1) {
            setPathfinding(getNearestWall(rc.getLocation()));
        }

        movement(null);
    }

    // super super hacky if you look at this you will burn your retinas
    public static MapLocation getNearestWall(MapLocation loc) {

        MapLocation[] cards = new MapLocation[] {
                new MapLocation(loc.x, height-1),
                new MapLocation(loc.x, 0),
                new MapLocation(0, loc.y),
                new MapLocation(width-1, loc.y)
        };

        Pair<MapLocation, Integer> bestLoc = null;
        for(MapLocation card : cards) {
            if(bestLoc == null || loc.distanceSquaredTo(card) < bestLoc.getValue()) {
                bestLoc = new Pair<>(card, loc.distanceSquaredTo(card));
            }
        }

        return bestLoc.getKey();
    }

    public static MapLocation getNextStop() {
        MapLocation currLoc = rc.getLocation();
        if(currLoc.x == ultimateGoal.x || currLoc.y == ultimateGoal.y) return ultimateGoal;
        else {
            if(currLoc.y==0 || currLoc.y==height-1) return new MapLocation(ultimateGoal.x, currLoc.y);
            if(currLoc.x==0 || currLoc.x==width-1) return new MapLocation(currLoc.x, ultimateGoal.y);
            return getNearestWall(currLoc);
        }
    }

    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                seekingWall = false;
                movingTo = getNextStop();
                setPathfinding(movingTo);
            }
        };
    }
}
