package punttherobot;

import battlecode.common.*;
import static punttherobot.Constants.*;

enum BuilderRole {
    NONE,
    SUICIDE, // for the greater good!
    DEFENSE;
}

public class Builder extends Unit {
    static BuilderRole currRole;
    static MapLocation assignedLoc = null;
    static boolean lockedOn = false;
    static boolean building = false;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        currRole = BuilderRole.values()[addlInfo];

        if(currRole == BuilderRole.SUICIDE) {
            rc.setIndicatorString("SC");
            setPathfindingToArchon();
        } else if(currRole == BuilderRole.DEFENSE) {
            rc.setIndicatorString("DEFENSE");
            if(assignment!=0) assignedLoc = new MapLocation(assignment/width, assignment%width);
            setPathfinding(assignedLoc);
        }
    }

    public static void run() throws GameActionException {
        // For the greater good!
        if(currRole == BuilderRole.SUICIDE) {
            if(rc.senseLead(rc.getLocation()) == 0) rc.disintegrate();
            tryMove(DIRECTIONS[rng.nextInt(DIRECTIONS.length)]);
            movement(null);

        // Build watchtower
        } else if (currRole == BuilderRole.DEFENSE) {
            MapLocation currLoc = rc.getLocation();

            if(assignedLoc != null) {
                if(building) {
                    buildOrRepair();
                } else {
                    if(rc.canSenseLocation(assignedLoc)) {
                        setPathfinding(getBestAdjacentTile(assignedLoc));
                        lockedOn = true;
                    }
                }
            } else {
                // TODO: builder wander code?
                MapLocation newTask = getBestWatchtowerLocation();
                if(newTask != null) {
                    assignedLoc = newTask;
                    setPathfinding(assignedLoc);
                }
            }

            movement(null);
        }
    }

    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                if(lockedOn) {
                    lockedOn = false;
                    building = true;
                }
            }
        };
    }
    public static void buildOrRepair() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        RobotInfo robotAtLoc = rc.senseRobotAtLocation(assignedLoc);
        if(robotAtLoc == null && rc.getTeamLeadAmount(myTeam) > 180) {
            if(rc.canBuildRobot(RobotType.WATCHTOWER, currLoc.directionTo(assignedLoc))) rc.buildRobot(RobotType.WATCHTOWER, currLoc.directionTo(assignedLoc));
        } else {
            if(robotAtLoc != null && robotAtLoc.type == RobotType.WATCHTOWER && robotAtLoc.mode == RobotMode.PROTOTYPE) {
                if(rc.canRepair(assignedLoc)) rc.repair(assignedLoc);
                if(rc.senseRobotAtLocation(assignedLoc).mode != RobotMode.PROTOTYPE) {
                    building = false;
                    assignedLoc = null;
                }
            }
        }
    }

    /**
     * Returns a watchtower to build
     * @return MapLocation with watchtower spot
     * @throws GameActionException reg exception
     */
    public static MapLocation getBestWatchtowerLocation() throws GameActionException {
        for(int i = 0; i < MAX_REG_WATCHTOWER_LOCS; i++) {
            int rawData = rc.readSharedArray(Flag.WATCHTOWER_POS.getValue()+i);
            if(rawData != 0) {
                rc.writeSharedArray(Flag.WATCHTOWER_POS.getValue()+i, 0);
                return new MapLocation(rawData/width, rawData%width);
            }
        }
        return null;
    }

    /**
     * Gets the translation vector for a direction (FOR USE WITH WATCHTOWERS ONLY)
     * @param dir Direction to convert to translation vector
     * @return translation vector as Int pair
     */
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
