package orbitalstriketherobot;

import battlecode.common.*;

import static orbitalstriketherobot.Constants.*;

enum BuilderRole {
    NONE,
    SUICIDE, // for the greater good!
    LAB;
}

public class Builder extends Unit {
    static BuilderRole currRole;
    static MapLocation assignedLoc = null;
    static RobotType assignedBot = null;
    static boolean lockedOn = false;
    static boolean building = false;
    static boolean mutating = false;
    static int reservedLead = 0;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        currRole = BuilderRole.values()[addlInfo];

        if(currRole == BuilderRole.SUICIDE) {
            rc.setIndicatorString("SC");
            setPathfindingToArchon();
        } else if(currRole == BuilderRole.LAB) {
            rc.setIndicatorString("LAB");
            if(assignment!=0) assignedLoc = new MapLocation(assignment/width, assignment%width);
            setPathfinding(assignedLoc);
        }
    }

    public static void run() throws GameActionException {

        if(!lockedOn && !building && currRole != BuilderRole.NONE && (!mutating && rc.readSharedArray(Flag.BUILDER_POSTING.getValue()) == 0)) {
            if(reservedLead>0) {
                subtractReservedLead(180);
                reservedLead -= 180;
            }
            setRole(-1);
        }

        // For the greater good!
        if(currRole == BuilderRole.SUICIDE) {
            if (rc.senseLead(rc.getLocation()) == 0) rc.disintegrate();
            tryMove(DIRECTIONS[rng.nextInt(DIRECTIONS.length)]);
            movement(null);
            return;
        }

        // Non-suicide
        if(currRole == BuilderRole.NONE) {
            Pair<MapLocation, Integer> job = getJob();
            if(job!=null) {
                System.out.println("took job at " + job.getKey() + " (" + job.getValue() + ")");

                assignedLoc = job.getKey();
                setPathfinding(assignedLoc);
                if(job.getValue() < 0) {
                    mutating = true;
                    setRole(job.getValue() *-1);
                } else {
                    setRole(job.getValue());
                }
            } else {
                tryMove(DIRECTIONS[rng.nextInt(DIRECTIONS.length)]);
            }
        } else {
            movement(null);
        }

        if(getPathfinding() != null && assignedLoc != null) {
            if(rc.getLocation().distanceSquaredTo(assignedLoc) == 0) {
                tryMove(DIRECTIONS[rng.nextInt(8)]);
                lockedOn = true;
                rc.writeSharedArray(Flag.BUILDER_POSTING.getValue(), 0);
            }
            else if (rc.getLocation().distanceSquaredTo(assignedLoc) <= 2) {
                endPathfinding(PATH_END_REASON.SUCCESSFUL);
            }
        }
        rc.setIndicatorString("ROLE: " + currRole + " @ " + assignedLoc + " | BUILDING: " + building + " | MUTATING: " + mutating);
        if(building) buildOrRepair();
    }

    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                if(mutating || rc.readSharedArray(Flag.BUILDER_POSTING.getValue()) != 0) {
                    if(!mutating) rc.writeSharedArray(Flag.BUILDER_POSTING.getValue(), 0);
                    else rc.writeSharedArray(Flag.MUTATION.getValue(), 0);
                    MapLocation nextBestLoc = getBestAdjacentTile(assignedLoc);
                    if(mutating || rc.senseRubble(nextBestLoc) >= rc.senseRubble(assignedLoc)) {
                        addReservedLead(assignedBot.buildCostLead);
                        reservedLead += assignedBot.buildCostLead;
                        building = true;
                        lockedOn = false;
                    } else {
                        assignedLoc = nextBestLoc;
                        setPathfinding(nextBestLoc);
                    }
                }
            }
        };
    }
    public static void buildOrRepair() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        RobotInfo robotAtLoc = rc.senseRobotAtLocation(assignedLoc);

        if(robotAtLoc != null) rc.setIndicatorString(mutating + " | " + (rc.canMutate(robotAtLoc.location)));
        else rc.setIndicatorString(assignedLoc + " :)");
        if(mutating && robotAtLoc != null && rc.canMutate(robotAtLoc.location)) {
            rc.mutate(robotAtLoc.location);
            setRole(-1);
            return;
        }

        if(robotAtLoc == null && rc.getTeamLeadAmount(myTeam) > assignedBot.buildCostLead) {
            if(rc.canBuildRobot(assignedBot, currLoc.directionTo(assignedLoc))) {
                rc.buildRobot(assignedBot, currLoc.directionTo(assignedLoc));
                subtractReservedLead(assignedBot.buildCostLead);
            }
        } else {
            if(robotAtLoc != null && robotAtLoc.type == assignedBot && (robotAtLoc.mode == RobotMode.PROTOTYPE || robotAtLoc.health != robotAtLoc.type.getMaxHealth(robotAtLoc.level))) {
                if(rc.canRepair(assignedLoc)) rc.repair(assignedLoc);
                RobotInfo updatedBot = rc.senseRobotAtLocation(assignedLoc);
                if(updatedBot.mode != RobotMode.PROTOTYPE && updatedBot.health == updatedBot.type.getMaxHealth(updatedBot.level)) {
                    setRole(-1);
                }
            } else if (robotAtLoc != null && robotAtLoc.type == assignedBot) {
                if(robotAtLoc.health == robotAtLoc.type.getMaxHealth(robotAtLoc.level)) {
                    //rc.writeSharedArray(Flag.BUILDER_POSTING.getValue(), 0);
                    setRole(-1);
                }
            }
        }
    }
    public static void setRole(int jobBot) {
        if(jobBot==-1) {
            currRole = BuilderRole.NONE;
            assignedBot = null;
            assignedLoc = null;
            building = false;
            mutating = false;
            setPathfindingToArchon();
            return;
        }

        switch (RobotType.values()[jobBot]) {
            case LABORATORY:
                currRole = BuilderRole.LAB;
                assignedBot = RobotType.LABORATORY;
                break;
        }
    }

    /**
     * Returns a watchtower to build
     * @return MapLocation with watchtower spot
     * @throws GameActionException reg exception
     */
    public static MapLocation getBestWatchtowerLocation() throws GameActionException {
        /*for(int i = 0; i < MAX_REG_WATCHTOWER_LOCS; i++) {
            int rawData = rc.readSharedArray(Flag.WATCHTOWER_POS.getValue()+i);
            if(rawData != 0) {
                rc.writeSharedArray(Flag.WATCHTOWER_POS.getValue()+i, 0);
                return new MapLocation(rawData/width, rawData%width);
            }
        }*/
        return null;
    }
    public static Pair<MapLocation, Integer> getJob() throws GameActionException {
        int rawData = rc.readSharedArray(Flag.BUILDER_POSTING.getValue());
        int mutationData = rc.readSharedArray(Flag.MUTATION.getValue());

        if(mutationData != 0) {
            MapLocation loc = new MapLocation((mutationData>>4)/width, (mutationData>>4)%width);
            rc.writeSharedArray(Flag.MUTATION.getValue(), 0);
            return new Pair<>(loc, (mutationData&0b1111)*-1);
        }
        if(rawData != 0) {
            MapLocation loc = new MapLocation((rawData>>4)/width, (rawData>>4)%width);
            return new Pair<>(loc, rawData&0b1111);
        } else {
            return null;
        }
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
