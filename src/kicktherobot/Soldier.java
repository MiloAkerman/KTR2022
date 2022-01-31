package kicktherobot;

import battlecode.common.*;
import static kicktherobot.Constants.*;

enum SoldierRole {
    MOVING,
    ATTACKING
}

public class Soldier extends Unit {
    static MapLocation archonTarget;
    static int targetIndex;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        archonTarget = new MapLocation(assignment/width, assignment%width);
        targetIndex = addlInfo;
        setPathfinding(archonTarget);
    }

    public static void run() throws GameActionException {
        boolean skipMove = false;
        adjustTarget();

        MapLocation attackLoc = determineBestTarget();
        if(attackLoc != null) {
            if(rc.canAttack(attackLoc)) {
                rc.attack(attackLoc);
                skipMove = true;
            }
        }

        // we can see one of the designated scouting locations!
        if(rc.canSenseLocation(archonTarget)) {
            RobotInfo robotInLoc = rc.senseRobotAtLocation(archonTarget);
            // found you!
            if(robotInLoc != null && robotInLoc.type == RobotType.ARCHON && robotInLoc.team == oppTeam) {
                //System.out.println("Confirmed archon at " + archonTarget);
            } else {
                //System.out.println("No archon at " + archonTarget);
                updateEnemyArchonExistence(targetIndex, 0);
            }
        }

        if(!skipMove) movement(false);
    }

    //----------------------- HELPER METHODS -----------------------------

    public static void adjustTarget() throws GameActionException {
        boolean targetStatus = isTargetThere(targetIndex+(homeArchonID*3));
        if(!targetStatus) {
            for(int i = 0; i < 12; i++) {
                if(isTargetThere(i)) {
                    targetIndex = i;
                    archonTarget = getArchonTargetFromIndex(i);
                    setPathfinding(archonTarget);
                }
            }
        }
    }
    public static void updateEnemyArchonExistence(int index, int exists) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(homeArchonID*3)+index);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(homeArchonID*3)+index, (data>>1) | exists);
    }
    public static MapLocation getArchonTargetFromIndex(int index) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index);
        return new MapLocation((data>>1)/width, (data>>1)%width);
    }
    public static boolean isTargetThere(int target) throws GameActionException {
        // copied from getArchonTargetFromIndex() but it doesn't matter cuz bytecode
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+target);
        return (data&0b1)==1;
    }
    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                // System.out.println("Here!");
            }
        };
    }

    public static MapLocation determineBestTarget() {
        // only measures action range. if you want to modify it to work with vision range, be my guest.
        // MORE GREEDY ALGORITHMS!!!!!!!!
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(13, oppTeam);
        Pair<MapLocation, Integer> bestOption = null;
        for(RobotInfo enemyRobot : enemyRobots) {
            if(bestOption == null || ENEMY_PRIORITY[enemyRobot.type.ordinal()] > bestOption.getValue()) {
                bestOption = new Pair<>(enemyRobot.location, ENEMY_PRIORITY[enemyRobot.type.ordinal()]);
            }
        }
        if(bestOption == null) {
            return null; //surely this will not cause any issues
        } else {
            return bestOption.getKey();
        }
    }
}
