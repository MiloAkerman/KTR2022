package orbitalstriketherobot;
import battlecode.common.*;

import static orbitalstriketherobot.Constants.*;
import static orbitalstriketherobot.Constants.SOLDIER_PRI_TYPE_WEIGHT;

//TODO: Sage logic
public class Sage extends Unit {
    static MapLocation archonTarget = null;
    static int targetIndex = -1;
    static int archonsInRange = 0;
    static int rushReq;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        rushReq = spawnFormula(SAGE_RUSH_BOUNDS);
    }

    public static void run() throws GameActionException {
        boolean skipMove = false;
        adjustGoal();
        MapLocation currLoc = rc.getLocation();

        RobotInfo bestTarget = determineBestTarget();
        // get direction bias // attack
        MapLocation bias = getAdditionalDirection();
        rc.setIndicatorString("BIASING TO " + bias + ". DEST " + getPathfinding());

        // independent of individual target, take priority
        if(rc.isActionReady()) {
            if(archonsInRange > 0) rc.envision(AnomalyType.FURY);
            else if(getEnemyScore() >= SAGE_CHARGE_THRESH) rc.envision(AnomalyType.CHARGE);
        }

        if(bestTarget != null) {
            // best practices? I hardly know her!
            if(bestTarget.type == RobotType.ARCHON || bestTarget.type == RobotType.SOLDIER || bestTarget.type == RobotType.WATCHTOWER || bestTarget.type == RobotType.SAGE)
                registerEnemyPos(bestTarget.location);

            int compScore = getCompScore();
            int enemyScore = getEnemyScore();

            boolean regroupCondition = bestTarget.type == RobotType.SOLDIER || bestTarget.type == RobotType.SAGE || bestTarget.type == RobotType.WATCHTOWER;
            if(currLoc.distanceSquaredTo(bestTarget.location) <= 25) {
                if(rc.isActionReady()) {
                    rc.attack(bestTarget.location);
                    if(bestTarget.type != RobotType.SOLDIER && bestTarget.type != RobotType.SAGE && bestTarget.type != RobotType.WATCHTOWER) skipMove = true;
                }

                if(regroupCondition) regroup(bestTarget);
            } else {
                if(rc.isActionReady() && rc.getHealth() >= 30 && compScore > enemyScore*1.1) {
                    tryMoveTowards(bestTarget.location);
                } else {
                    if(regroupCondition) regroup(bestTarget);
                }
            }
        } else if(rc.isActionReady() && getEnemyScore() == 0) {
            // abyss is least priority. Only cast if no target and no other anomalies have been cast
            MapLocation home = getArchonHomeFromIndex(homeArchonID);
            if(archonTarget != null && currLoc.distanceSquaredTo(home) > SAGE_RADIUS_ABYSS && currLoc.distanceSquaredTo(getArchonTargetFromIndex(targetIndex)) <= SAGE_RADIUS_ABYSS) {
                rc.envision(AnomalyType.ABYSS);
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

    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                // System.out.println("Here!");
            }
        };
    }

    public static void adjustGoal() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
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
        if(!targetStatus && compScore > rushReq) {
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

        if(getPathfinding() == null) tryMove(DIRECTIONS[trueRng.nextInt(8)]);
    }
    public static RobotInfo determineBestTarget() throws GameActionException {
        // like soldier, but measures full vision range. (so basically better)
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(34, oppTeam);
        MapLocation currLoc = rc.getLocation();
        int archonsVisible = 0;

        Pair<RobotInfo, Double> bestOption = null;
        for(RobotInfo enemyRobot : enemyRobots) {
            if(enemyRobot.type == RobotType.ARCHON && currLoc.distanceSquaredTo(enemyRobot.location) <= 25) archonsVisible++;
            // prefers enemies on low rubble, low health, of certain type, and within vision range.
            double score = ((100-rc.senseRubble(enemyRobot.location)) * SOLDIER_PRI_RUBBLE_WEIGHT) + ((1-((double)enemyRobot.health/(double)(enemyRobot.type.getMaxHealth(enemyRobot.level)))) * SOLDIER_PRI_HEALTH_WEIGHT) + (ENEMY_PRIORITY[enemyRobot.type.ordinal()] * SOLDIER_PRI_TYPE_WEIGHT) + ((currLoc.distanceSquaredTo(enemyRobot.location) > 25)?SOLDIER_PRI_INRANGE_WEIGHT:0);
            if(bestOption == null || score > bestOption.getValue()) {
                bestOption = new Pair<>(enemyRobot, score);
            }
        }

        archonsInRange = archonsVisible;
        if(bestOption == null) {
            return null; //surely this will not cause any issues :clueless:
        } else {
            return bestOption.getKey();
        }
    }

    public static int getCompScore() {
        int value = 0;
        RobotInfo[] allies = rc.senseNearbyRobots(34, myTeam);
        for(RobotInfo ally : allies) {
            if(ally.type == RobotType.SAGE || ally.type == RobotType.SOLDIER) value++;
        }
        return value;
    }
    public static int getEnemyScore() {
        int value = 0;
        RobotInfo[] foes = rc.senseNearbyRobots(34, oppTeam);
        for(RobotInfo foe : foes) {
            if(foe.type == RobotType.SAGE || foe.type == RobotType.SOLDIER) value++;
        }
        return value;
    }

    public static void regroup(RobotInfo enemy) throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        Direction enemyDirection = currLoc.directionTo(enemy.location);

        tryMove(enemyDirection.opposite());
    }
    // we like to group up!
    public static MapLocation getAdditionalDirection() {
        RobotInfo[] robots = rc.senseNearbyRobots(34, myTeam);
        MapLocation currLoc = rc.getLocation();
        Pair<MapLocation, Integer> bestLoc = null;
        for(RobotInfo robot : robots) {
            if(robot.type != RobotType.SAGE && robot.type != RobotType.SOLDIER) continue;
            if(getPathfinding() != null && robot.location.distanceSquaredTo(getPathfinding()) < currLoc.distanceSquaredTo(getPathfinding())) {
                if(((bestLoc == null) || currLoc.distanceSquaredTo(robot.location) < bestLoc.getValue())) {
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
