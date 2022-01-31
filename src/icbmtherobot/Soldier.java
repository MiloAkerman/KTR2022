package icbmtherobot;

import battlecode.common.*;
import static icbmtherobot.Constants.*;

public class Soldier extends Unit {
    static MapLocation archonTarget = null;
    static int targetIndex = -1;
    static int rushReq;
    static boolean isJumper = false;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        rushReq = spawnFormula(SOLDIER_RUSH_BOUNDS);
        if(addlInfo==14) isJumper = true;
    }

    public static void run() throws GameActionException {
        if(isJumper) {
            Jumper.run();
            return;
        }

        boolean skipMove = false;
        MapLocation currLoc = rc.getLocation();
        int compScore = getCompScore();
        adjustTarget();

        // get direction bias // attack
        MapLocation bias = getAdditionalDirection();
        rc.setIndicatorString("BIASING TO " + bias + ". DEST " + getPathfinding());

        RobotInfo attackLoc = determineBestTarget();
        int enemyScore = getEnemyScore();
        if(attackLoc != null && turnCount > 50) {
            // best practices? I hardly know her!
            if(attackLoc.type == RobotType.ARCHON || attackLoc.type == RobotType.SOLDIER || attackLoc.type == RobotType.WATCHTOWER || attackLoc.type == RobotType.SAGE)
                registerEnemyPos(attackLoc.location);

            boolean regroupCondition = attackLoc.type == RobotType.SOLDIER || attackLoc.type == RobotType.SAGE || attackLoc.type == RobotType.WATCHTOWER;
            if(compScore*1.2 >= enemyScore) {
                if(currLoc.distanceSquaredTo(attackLoc.location) < 13) {
                    if(rc.isActionReady()) rc.attack(attackLoc.location);

                    if(rc.getHealth() < 20 && regroupCondition) regroup(attackLoc);
                    else if(attackLoc.type != RobotType.ARCHON ) skipMove = true;
                } else {
                    if(compScore > enemyScore*1.5) tryMoveTowards(attackLoc.location);
                    else if(regroupCondition) regroup(attackLoc);
                }
            } else {
                if(rc.isActionReady() && currLoc.distanceSquaredTo(attackLoc.location) <= 13) rc.attack(attackLoc.location);
                if(regroupCondition) regroup(attackLoc);
            }
        }

        // we can see one of the designated scouting locations!
        if(archonTarget != null && rc.canSenseLocation(archonTarget)) {
            RobotInfo robotInLoc = rc.senseRobotAtLocation(archonTarget);
            // found you!
            if(robotInLoc != null && robotInLoc.type == RobotType.ARCHON && robotInLoc.team == oppTeam) {
                //System.out.println("Confirmed archon at " + archonTarget);
                updateEnemyArchonExistence(targetIndex, 2);
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
        for(int i = Flag.ENEMY_SOLDIERS.getValue(); i < Flag.ENEMY_SOLDIERS.getValue()+5; i++) {
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
        if(!targetStatus && getCompScore() > rushReq) {
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
    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                // System.out.println("Here!");
            }
        };
    }

    public static RobotInfo determineBestTarget() throws GameActionException {
        // like soldier, but measures full vision range. (so basically better)
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(20, oppTeam);
        MapLocation currLoc = rc.getLocation();

        Pair<RobotInfo, Double> bestOption = null;
        for(RobotInfo enemyRobot : enemyRobots) {
            // prefers enemies on low rubble, low health, of certain type, and within vision range.
            double score = ((100-rc.senseRubble(enemyRobot.location)) * SOLDIER_PRI_RUBBLE_WEIGHT) + ((1-((double)enemyRobot.health/(double)(enemyRobot.type.getMaxHealth(enemyRobot.level)))) * SOLDIER_PRI_HEALTH_WEIGHT) + (ENEMY_PRIORITY[enemyRobot.type.ordinal()] * SOLDIER_PRI_TYPE_WEIGHT) + ((currLoc.distanceSquaredTo(enemyRobot.location) > 25)?SOLDIER_PRI_INRANGE_WEIGHT:0);
            if(bestOption == null || score > bestOption.getValue()) {
                bestOption = new Pair<>(enemyRobot, score);
            }
        }

        if(bestOption == null) {
            return null; //surely this will not cause any issues :clueless:
        } else {
            return bestOption.getKey();
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
            tryMoveTowards(bestBias.getKey());
        } else {
            tryMove(enemyDirection.opposite());
        }
    }

    // we like to group up!
    public static MapLocation getAdditionalDirection() {
        RobotInfo[] robots = rc.senseNearbyRobots(20, myTeam);
        MapLocation currLoc = rc.getLocation();
        Pair<MapLocation, Integer> bestLoc = null;
        for(RobotInfo robot : robots) {
            if(robot.type != RobotType.SAGE && robot.type != RobotType.SOLDIER) continue;
            if(getPathfinding() != null && robot.location.distanceSquaredTo(getPathfinding()) < currLoc.distanceSquaredTo(getPathfinding())) {
                if((bestLoc == null || currLoc.distanceSquaredTo(robot.location) < bestLoc.getValue())) {
                    bestLoc = new Pair<>(robot.location, currLoc.distanceSquaredTo(robot.location));
                }
            }
        }
        if(bestLoc != null && rng.nextInt(5)==0) {
            return bestLoc.getKey();
        } else {
            return null;
        }
    }
}
