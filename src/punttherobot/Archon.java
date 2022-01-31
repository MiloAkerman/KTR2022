package punttherobot;

import battlecode.common.*;
import static punttherobot.Constants.*;

public class Archon extends Unit {
    static boolean[] watchtowerLocsAssigned = new boolean[]{false, false, false};
    static int betterArchonID;
    static int numArchons;
    static int moveTurn = -1;
    static int spawnRatio = (int)(-0.0075*(width*height) + 35);

    static MapLocation[] enemyArchonLocations = new MapLocation[3];

    //---------------------------- CODE -----------------------------------

    public static void setup() throws GameActionException {
        betterArchonID = setBetterArchonID();
        enemyArchonLocations = possibleArchonLocations();
        numArchons = rc.getArchonCount();
        handlePathfindingCallback();
    }

    public static void run() throws GameActionException {
        if(spawnTurnCount == 1) {
            removeNearbyPossibleArchons();
        }

        shiftInitPulse();
        if(rc.getArchonCount() != numArchons) {
            flushBetterIDs();
            setBetterArchonID();
            numArchons = rc.getArchonCount();
        }

        // move out of high rubble
        if(rc.senseRubble(rc.getLocation()) > MAX_ACCEPTABLE_ARCHON_RUBBLE && moveTurn == -1) {
            if(rc.canTransform()) {
                moveTurn = 0;
                rc.transform();
            }
        }
        if(moveTurn >= 0 && rc.getMode() == RobotMode.PORTABLE && rc.getMovementCooldownTurns()<=0) {
            if(moveTurn > MAX_ARCHON_MOVE_TURNS) {
                if(rc.canTransform()) rc.transform();
            } else if(rc.senseRubble(rc.getLocation()) > MAX_ACCEPTABLE_ARCHON_RUBBLE || getAggregateSurroundingScore(rc.getLocation()) > MAX_ACCEPTABLE_AVG_RUBBLE) {
                tryMove(getBestMoveDir());
                moveTurn++;
            } else {
                if(rc.canTransform()) rc.transform();
            }
            return;
        }

        // update archon position (maybe just when we move? who cares, archons only use like 20 bytecode on a bad day)
        MapLocation currLoc = rc.getLocation();
        rc.writeSharedArray(Flag.ARCHON_INFO.getValue() + betterArchonID, currLoc.x * width + currLoc.y);
        Direction spawnDir = DIRECTIONS[turnCount % 8];

        int hold = rc.readSharedArray(Flag.SPAWN_QUEUE.getValue());
        // if heartbeat too old we assume archon is dead or smth
        int archons = rc.getArchonCount();
        boolean condition = archons <= 1 || (turnCount % archons == betterArchonID);
        if ((condition || (betterArchonID>turnCount%archons && rc.getTeamLeadAmount(myTeam)>80)) && (hold==0 || (turnCount > ((hold>>2)+MAX_DISTRESS_TURNS)) || (hold&0b11)==betterArchonID)) {
            boolean distressed = false;

            // distress code (we hog up all spawn turns if we're being attacked)
            // future me: if more than one robot has distress, split turns?
            RobotInfo[] enemies = rc.senseNearbyRobots(34, oppTeam);
            if(enemies.length >= MIN_ENEMIES_DISTRESS) {
                // hold
                rc.writeSharedArray(Flag.SPAWN_QUEUE.getValue(), (turnCount<<2) | betterArchonID);
                distressed = true;
            } else {
                rc.writeSharedArray(Flag.SPAWN_QUEUE.getValue(), 0);
            }

            int spawn = rng.nextInt(spawnRatio);
            sendMotivation();
            if ((turnCount < 10 || spawn == 0) && !distressed) {
                if (rc.canBuildRobot(RobotType.MINER, spawnDir)) {
                    sendInitPulse(0, 0);
                    rc.buildRobot(RobotType.MINER, spawnDir);
                }
            } else {
                int n = rng.nextInt(enemyArchonLocations.length);
                sendInitPulse(enemyArchonLocations[n], n+(betterArchonID*3));
                if (rc.canBuildRobot(RobotType.SOLDIER, spawnDir)) {
                    rc.buildRobot(RobotType.SOLDIER, spawnDir);
                }
            }
        // not spawning, might as well heal.
        } else {
            heal();
        }
    }

    //------------------------ HELPER METHODS -----------------------------

    /**
     * Allocates a better Archon ID (if we indexed with the actual ID, it would be all over the place)
     *
     * @return better Archon ID allocated
     * @throws GameActionException reg exception
     */
    public static int setBetterArchonID() throws GameActionException {
        // Cycle through each betterID slot looking for an empty one
        for (int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue() + 4; i++) {
            if (rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, rc.getID());
                return i;
            }
        }
        System.out.println("Final return statement reached on setBetterArchonID. Fifth archon confirmed????");
        rc.resign();
        return 0; // this should NEVER happen
    }

    public static void flushBetterIDs() throws GameActionException {
        for(int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue() + rc.getArchonCount(); i++) {
            if(rc.readSharedArray(i) == 0) {
                return; // flushed already.
            }
        }
        for(int j = Flag.ARCHON_ID_ASSIGNMENT.getValue(); j < Flag.ARCHON_ID_ASSIGNMENT.getValue() + 4; j++) {
            rc.writeSharedArray(Flag.ARCHON_ID_ASSIGNMENT.getValue() + j, 0);
            rc.writeSharedArray(Flag.ARCHON_INFO.getValue() + j, 0);
        }
    }

    /**
     * Sends initial pulse to bot with a role attached and additional 4bit info
     * @param role the role to send
     * @param addl additional data to send
     * @throws GameActionException reg exception
     */
    public static void sendInitPulse(int role, int addl) throws GameActionException {
        int pulse = (role << 4) | addl;
        rc.writeSharedArray(Flag.INIT_PULSE_UPC.getValue() + betterArchonID, pulse);
    }

    /**
     * Sends initial pulse to bot with a MapLoc attached and additional 4bit info
     * @param location location to send
     * @param addl     additional data to send
     * @throws GameActionException reg exception
     */
    public static void sendInitPulse(MapLocation location, int addl) throws GameActionException {
        int pulse = ((location.x * width + location.y) << 4) | addl;
        rc.writeSharedArray(Flag.INIT_PULSE_UPC.getValue() + betterArchonID, pulse);
    }

    /**
     * We need to shift the init pulse so that the next rounds pulse doesnt overwrite the old one
     * @throws GameActionException reg exception
     */
    public static void shiftInitPulse() throws GameActionException {
        int data = rc.readSharedArray(Flag.INIT_PULSE_UPC.getValue() + betterArchonID);
        rc.writeSharedArray(Flag.INIT_PULSE.getValue() + betterArchonID, data);
    }

    /**
     * Unit morale is important.
     * @throws GameActionException reg exception
     */
    public static void sendMotivation() throws GameActionException {
        int data = trueRng.nextInt(motivationReference.length);
        rc.writeSharedArray(Flag.MOTIVATION.getValue(), data);
    }

    /**
     * Returns a watchtower to build
     * @return MapLocation with watchtower spot
     * @throws GameActionException reg exception
     */
    public static MapLocation getBestWatchtowerLocation() throws GameActionException {
        for (int i = 0; i < MAX_REG_WATCHTOWER_LOCS; i++) {
            int rawData = rc.readSharedArray(Flag.WATCHTOWER_POS.getValue() + i);
            if (rawData != 0) {
                rc.writeSharedArray(Flag.WATCHTOWER_POS.getValue() + i, 0);
                return new MapLocation(rawData / width, rawData % width);
            }
        }

        MapLocation currLoc = rc.getLocation();
        for (int j = 0; j < 3; j++) {
            Pair<Integer, Integer> trans = directionToTranslation(currLoc.directionTo(enemyArchonLocations[j]), WATCHTOWER_LATTICE_SIZE);
            MapLocation newPos = currLoc.translate(trans.getKey(), trans.getValue());
            RobotInfo robotAt = rc.senseRobotAtLocation(newPos);
            if (rc.onTheMap(newPos) && (robotAt == null || robotAt.mode == RobotMode.DROID) && !watchtowerLocsAssigned[j]) {
                watchtowerLocsAssigned[j] = true;
                return newPos;
            }
        }
        return null;
    }

    /**
     * Gets the translation vector for a direction (FOR USE WITH WATCHTOWERS ONLY)
     * @param dir Direction to convert to translation vector
     * @return translation vector as Int pair
     */
    public static Pair<Integer, Integer> directionToTranslation(Direction dir, int mult) {
        if (dir == Direction.NORTH) return new Pair<>(0, mult);
        else if (dir == Direction.NORTHEAST) return new Pair<>(mult, mult);
        else if (dir == Direction.EAST) return new Pair<>(mult, 0);
        else if (dir == Direction.SOUTHEAST) return new Pair<>(mult, -mult);
        else if (dir == Direction.SOUTH) return new Pair<>(0, -mult);
        else if (dir == Direction.SOUTHWEST) return new Pair<>(-mult, -mult);
        else if (dir == Direction.WEST) return new Pair<>(-mult, 0);
        else return new Pair<>(-mult, mult);
    }

    /**
     * Calculates the three possible types of symmetry, also writes them to comms
     * @return The three possible archon locations
     */
    public static MapLocation[] possibleArchonLocations() throws GameActionException {
        MapLocation[] locs = new MapLocation[3];
        MapLocation me = rc.getLocation();
        float originX = (width - 1) / 2.0f;
        float originY = (height - 1) / 2.0f;
        float relX = me.x - originX;
        float relY = me.y - originY;

        locs[0] = new MapLocation((int) ((relX * -1) + originX), (int) ((relY * -1) + originY));
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3), ((locs[0].x * width + locs[0].y) << 1) | 1);
        locs[1] = new MapLocation((int) ((relX * -1) + originX), me.y);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3) + 1, ((locs[1].x * width + locs[1].y) << 1) | 1);
        locs[2] = new MapLocation(me.x, (int) ((relY * -1) + originY));
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3) + 2, ((locs[2].x * width + locs[2].y) << 1) | 1);

        return locs;
    }

    public static int getAggregateSurroundingScore(MapLocation center) throws GameActionException {
        int agg = 0;

        for(int i = 0; i < 8; i++) {
            MapLocation loc = center.add(DIRECTIONS[i]);
            if(rc.onTheMap(loc)) {
                agg += rc.senseRubble(loc);
            } else {
                agg += 200; // out of map penalty
            }
        }

        return agg /= 8;
    }
    public static void heal() throws GameActionException {
        for(RobotInfo robot : rc.senseNearbyRobots(34, myTeam)) {
            if(robot.health < robot.type.getMaxHealth(robot.level) && rc.canRepair(robot.location)) {
                rc.repair(robot.location);
                rc.setIndicatorLine(rc.getLocation(), robot.location, 255, 0, 0);
                return;
            }
        }
    }

    public static Direction getBestMoveDir() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        Triple<Direction, Integer, Integer> bestMove = null;
        for(Direction possibleDir : DIRECTIONS) {
            MapLocation newPos = currLoc.add(possibleDir);
            if(rc.canMove(possibleDir) && (bestMove == null || (rc.senseRubble(newPos) < bestMove.getB() || getAggregateSurroundingScore(newPos) < bestMove.getC()))) {
                bestMove = new Triple<>(possibleDir, rc.senseRubble(newPos), getAggregateSurroundingScore(newPos));
            }
        }
        if(bestMove == null) {
            // im not going to do a for loop for this you get one try or youre out.
            int index = rng.nextInt(8);
            if(rc.canMove(DIRECTIONS[index])) {
                return DIRECTIONS[index];
            } else {
                return Direction.CENTER;
            }
        } else {
            return bestMove.getA();
        }
    }

    public static void removeNearbyPossibleArchons() throws GameActionException {
        for(int i = Flag.ENEMY_ARCHON_POS.getValue(); i < Flag.ENEMY_ARCHON_POS.getValue()+12; i++) {
            int rawData = rc.readSharedArray(i);
            MapLocation pos = new MapLocation((rawData>>1)/width, (rawData>>1)%width);
            if((rawData&0b1) == 1 && rc.canSenseLocation(pos)) {
                RobotInfo robotAtLoc = rc.senseRobotAtLocation(pos);
                if(robotAtLoc == null || robotAtLoc.team == myTeam || robotAtLoc.type != RobotType.ARCHON) {
                    rc.writeSharedArray(i, (pos.x * width + pos.y) << 1); // 0 simplified
                }
            }
        }
    }



    /**
     * Required pathfinding callback for movement
     */
    public static void handlePathfindingCallback() {
        pathCallback = reason -> {

        };
    }
}

