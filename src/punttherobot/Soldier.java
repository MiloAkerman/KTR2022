package punttherobot;

import battlecode.common.*;
import static punttherobot.Constants.*;

public class Soldier extends Unit {
    static MapLocation archonTarget = null;
    static int targetIndex = -1;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        /*archonTarget = new MapLocation(assignment/width, assignment%width);
        targetIndex = addlInfo;
        setPathfinding(archonTarget);*/
    }

    public static void run() throws GameActionException {
        boolean skipMove = false;
        int compScore = getCompScore();
        adjustTarget();

        // get direction bias // attack
        Direction bias = getAdditionalDirection();
        rc.setIndicatorString("BIASING TO " + bias + ". DEST " + getPathfinding());

        Pair<MapLocation, RobotInfo> attackLoc = determineBestTarget();
        int enemyScore = getEnemyScore();
        if(attackLoc != null) {
            // best practices? I hardly know her!
            if(attackLoc.getValue().type == RobotType.ARCHON || attackLoc.getValue().type == RobotType.SOLDIER || attackLoc.getValue().type == RobotType.WATCHTOWER || attackLoc.getValue().type == RobotType.SAGE)
                registerEnemyPos(attackLoc.getKey());

            if(compScore >= enemyScore) {
                if(rc.canAttack(attackLoc.getKey())) {
                    rc.attack(attackLoc.getKey());
                    if(attackLoc.getValue().type != RobotType.ARCHON) skipMove = true;
                }
            } else {
                if(rc.canAttack(attackLoc.getKey())) rc.attack(attackLoc.getKey());
                if(attackLoc.getValue().type == RobotType.SOLDIER) regroup(attackLoc.getValue());
            }
        }

        // we can see one of the designated scouting locations!
        if(archonTarget != null && rc.canSenseLocation(archonTarget)) {
            RobotInfo robotInLoc = rc.senseRobotAtLocation(archonTarget);
            // found you!
            if(robotInLoc != null && robotInLoc.type == RobotType.ARCHON && robotInLoc.team == oppTeam) {
                //System.out.println("Confirmed archon at " + archonTarget);
            } else {
                //System.out.println("No archon at " + archonTarget);
                updateEnemyArchonExistence(targetIndex, 0);
                checkForGold();
            }
        }

        if(!skipMove) movement(bias);
    }

    //----------------------- HELPER METHODS -----------------------------

    public static void adjustTarget() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        boolean newPosSet = false;
        int compScore = getCompScore();

        // go to logged enemy
        Pair<MapLocation, Integer> bestEnemy = null;
        for(int i = Flag.ENEMY_SOLDIERS.getValue(); i < Flag.ENEMY_SOLDIERS.getValue()+6; i++) {
            int data = rc.readSharedArray(i);
            if(data != 0) {
                MapLocation pos = new MapLocation(data/width, data%width);

                if(rc.canSenseLocation(pos)) {
                    RobotInfo robotAtLoc = rc.senseRobotAtLocation(pos);
                    if(robotAtLoc == null || robotAtLoc.team == myTeam) {
                        rc.writeSharedArray(i, 0);
                        continue;
                    }
                }
                if(bestEnemy == null || currLoc.distanceSquaredTo(pos) < bestEnemy.getValue()) {
                    bestEnemy = new Pair<>(pos, currLoc.distanceSquaredTo(pos));
                }
            }
        }
        if(bestEnemy != null) {
            setPathfinding(bestEnemy.getKey());
            return;
        }

        // go to archon
        boolean targetStatus = isTargetThere(targetIndex);
        int newTargetIndex = -1;
        if(!targetStatus && getCompScore() > 2) {
            Pair<MapLocation, Integer> bestTarget = null;
            for(int i = 0; i < 12; i++) {
                if(isTargetThere(i)) {
                    MapLocation possibleAT = getArchonTargetFromIndex(i);
                    if(bestTarget == null || currLoc.distanceSquaredTo(possibleAT) <= bestTarget.getValue()) {
                        newTargetIndex = i;
                        bestTarget = new Pair<>(possibleAT, currLoc.distanceSquaredTo(possibleAT));
                    }
                }
            }

            if(bestTarget != null) {
                targetIndex = newTargetIndex;
                archonTarget = bestTarget.getKey();
                setPathfinding(archonTarget);
                return;
            }
        } else if(targetStatus) {
            setPathfinding(archonTarget);
        }

        if(!targetStatus) tryMove(DIRECTIONS[trueRng.nextInt(8)]);
    }
    public static void registerEnemyPos(MapLocation pos) throws GameActionException {
        for(int i = Flag.ENEMY_SOLDIERS.getValue(); i < Flag.ENEMY_SOLDIERS.getValue()+6; i++) {
            if(rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, pos.x * width + pos.y);
            }
        }
    }
    public static void checkForGold() throws GameActionException {
        MapLocation[] gold = rc.senseNearbyLocationsWithGold(20);
        if(gold.length > 0 && rc.readSharedArray(Flag.GOLD.getValue()) == 0) {
            rc.writeSharedArray(Flag.GOLD.getValue(), gold[0].x * width + gold[0].y);
        }
    }
    public static void updateEnemyArchonExistence(int index, int exists) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(homeArchonID*3)+index);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index, ((data>>1)<<1) | exists);
    }
    public static MapLocation getArchonTargetFromIndex(int index) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index);
        return new MapLocation((data>>1)/width, (data>>1)%width);
    }
    public static boolean isTargetThere(int target) throws GameActionException {
        // copied from getArchonTargetFromIndex() but it doesn't matter cuz bytecode
        if(target==-1) return false;
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+target);
        //if(turnCount > 520 && turnCount < 550) System.out.println("Read " + data + " from " + (Flag.ENEMY_ARCHON_POS.getValue()+target));
        return (data&0b1)==1;
    }
    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                // System.out.println("Here!");
            }
        };
    }

    public static Pair<MapLocation, RobotInfo> determineBestTarget() throws GameActionException {
        // only measures action range. if you want to modify it to work with vision range, be my guest.
        // MORE GREEDY ALGORITHMS!!!!!!!!
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(13, oppTeam);
        Triple<MapLocation, Double, RobotInfo> bestOption = null;
        for(RobotInfo enemyRobot : enemyRobots) {
            double score = ((100-rc.senseRubble(enemyRobot.location)) * SOLDIER_PRI_RUBBLE_WEIGHT) + (((double)enemyRobot.health/(double)(enemyRobot.type.getMaxHealth(enemyRobot.level))) * SOLDIER_PRI_HEALTH_WEIGHT) + (ENEMY_PRIORITY[enemyRobot.type.ordinal()] * SOLDIER_PRI_TYPE_WEIGHT);
            if(bestOption == null || score > bestOption.getB()) {
                bestOption = new Triple<>(enemyRobot.location, score, enemyRobot);
            }
        }
        if(bestOption == null) {
            return null; //surely this will not cause any issues :clueless:
        } else {
            return new Pair<>(bestOption.getA(), bestOption.getC());
        }
    }

    public static int getCompScore() {
        int value = 0;
        RobotInfo[] allies = rc.senseNearbyRobots(13, myTeam);
        for(RobotInfo ally : allies) {
            if(ally.type == RobotType.SOLDIER) value++;
        }
        return value;
    }
    public static int getEnemyScore() {
        int value = 0;
        RobotInfo[] foes = rc.senseNearbyRobots(13, oppTeam);
        for(RobotInfo foe : foes) {
            if(foe.type == RobotType.SOLDIER) value++;
        }
        return value;
    }
    public static void regroup(RobotInfo enemy) throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        Direction enemyDirection = currLoc.directionTo(enemy.location);

        RobotInfo[] allies = rc.senseNearbyRobots(currLoc.translate(enemyDirection.dx*4, enemyDirection.dy*4), 20, myTeam);
        Pair<MapLocation, Integer> bestBias = null;
        for(RobotInfo ally : allies) {
            if(ally.type == RobotType.SOLDIER && (bestBias == null || rc.senseRubble(currLoc.add(currLoc.directionTo(ally.location))) < bestBias.getValue())) {
                bestBias = new Pair<>(ally.location, rc.senseRubble(currLoc.add(currLoc.directionTo(ally.location))));
            }
        }

        if(bestBias != null) {
            tryMove(currLoc.directionTo(bestBias.getKey()));
        } else {
            tryMove(enemyDirection.opposite());
        }
    }

    // we like to group up!
    public static Direction getAdditionalDirection() {
        RobotInfo[] robots = rc.senseNearbyRobots(20, myTeam);
        MapLocation currLoc = rc.getLocation();
        Pair<MapLocation, Integer> bestLoc = null;
        for(RobotInfo robot : robots) {
            if(robot.type != RobotType.SOLDIER) continue;
            if(getPathfinding() != null && robot.location.distanceSquaredTo(getPathfinding()) < currLoc.distanceSquaredTo(getPathfinding())) {
                if((bestLoc == null || currLoc.distanceSquaredTo(robot.location) < bestLoc.getValue())) {
                    bestLoc = new Pair<>(robot.location, currLoc.distanceSquaredTo(robot.location));
                }
            }
        }
        if(bestLoc != null) {
            return currLoc.directionTo(bestLoc.getKey());
        } else {
            return null;
        }
    }
}
