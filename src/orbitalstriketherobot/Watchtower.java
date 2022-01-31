package orbitalstriketherobot;

import battlecode.common.*;

import static orbitalstriketherobot.Constants.*;

//TODO: Watchtower logic
public class Watchtower extends Unit {
    static boolean[] watchtowerLocsAssigned = new boolean[] {false, false, false, false};

    public static void setup() throws GameActionException {
        registerWatchtowerLocs();
    }
    public static void run() throws GameActionException {
        registerWatchtowerLocs();

        MapLocation attackLoc = determineBestTarget();
        if(attackLoc != null) {
            if(rc.canAttack(attackLoc)) {
                rc.attack(attackLoc);
            }
        }
    }

    public static MapLocation determineBestTarget() {
        // only measures action range. if you want to modify it to work with vision range, be my guest.
        // MORE GREEDY ALGORITHMS!!!!!!!!
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(20, oppTeam);
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
    public static void registerWatchtowerLocs() throws GameActionException {
        for(int j = 0; j < 4; j++) {
            int firstOpenSpot = -1;
            /*for(int i = Flag.WATCHTOWER_POS.getValue(); i < Flag.WATCHTOWER_POS.getValue()+MAX_REG_WATCHTOWER_LOCS; i++) {
                if(rc.readSharedArray(i) == 0) firstOpenSpot = i;
            }*/
            if(firstOpenSpot == -1) return;

            if(watchtowerLocsAssigned[j]) {
                continue;
            }
            Pair<Integer, Integer> trans = directionToTranslation(CARD_DIRECTIONS[j]);
            MapLocation newLoc = rc.getLocation().translate(trans.getKey(), trans.getValue());
            if(rc.onTheMap(newLoc)) {
                RobotInfo robotAtLoc = rc.senseRobotAtLocation(newLoc);
                if(robotAtLoc == null || robotAtLoc.mode == RobotMode.DROID) {
                    watchtowerLocsAssigned[j] = true;
                    rc.writeSharedArray(firstOpenSpot, newLoc.x*width+newLoc.y);
                }
            }
        }
    }

    public static Pair<Integer, Integer> directionToTranslation(Direction dir) {
        if(dir == Direction.NORTH) return new Pair<>(0, WATCHTOWER_LATTICE_SIZE);
        else if(dir == Direction.NORTHEAST) return new Pair<>(WATCHTOWER_LATTICE_SIZE, WATCHTOWER_LATTICE_SIZE);
        else if(dir == Direction.EAST) return new Pair<>(WATCHTOWER_LATTICE_SIZE, 0);
        else if(dir == Direction.SOUTHEAST) return new Pair<>(WATCHTOWER_LATTICE_SIZE, -WATCHTOWER_LATTICE_SIZE);
        else if(dir == Direction.SOUTH) return new Pair<>(0, -WATCHTOWER_LATTICE_SIZE);
        else if(dir == Direction.SOUTHWEST) return new Pair<>(-WATCHTOWER_LATTICE_SIZE, -WATCHTOWER_LATTICE_SIZE);
        else if(dir == Direction.WEST) return new Pair<>(-WATCHTOWER_LATTICE_SIZE, 0);
        else return new Pair<>(-WATCHTOWER_LATTICE_SIZE, WATCHTOWER_LATTICE_SIZE);
    }
}
