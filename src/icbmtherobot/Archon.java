package icbmtherobot;

import battlecode.common.*;
import static icbmtherobot.Constants.*;


public class Archon extends Unit {
    static int betterArchonID;
    static int numArchons;
    static int moveTurn = -1;
    static int totalSpawns = 0;
    static boolean relocating = false;
    static boolean firstBuilder = false;

    static int minerSpawn = 0;
    static int soldierSpawn = 0;
    static int builderSpawn = 0;
    static int suicideBuilderSpawn = 0;
    static int labSpawnChance = 0;

    static MapLocation[] enemyArchonLocations = new MapLocation[3];

    //---------------------------- CODE -----------------------------------

    public static void setup() throws GameActionException {
        betterArchonID = setBetterArchonID();
        enemyArchonLocations = possibleArchonLocations();
        numArchons = rc.getArchonCount();
        handlePathfindingCallback();
        //storeBorderVisTiles();

        minerSpawn = spawnFormula(MINER_SPAWN_BOUNDS);
        soldierSpawn = minerSpawn + spawnFormula(SOLDIER_SPAWN_BOUNDS);
        builderSpawn = soldierSpawn + spawnFormula(REG_BUILDER_SPAWN_BOUNDS);
        suicideBuilderSpawn = builderSpawn + spawnFormula(SUICIDE_BUILDER_SPAWN_BOUNDS);
        labSpawnChance = spawnFormula(LAB_POST_CHANCE, SIGMOID_LAB_A);

        System.out.println("Miner: " + minerSpawn);
        System.out.println("Soldier: " + soldierSpawn);
        System.out.println("Reg builder: " + builderSpawn);
        System.out.println("S builder: " + suicideBuilderSpawn);
        System.out.println("LAB CHANCE: " + labSpawnChance);
    }

    public static void run() throws GameActionException {
        if(spawnTurnCount == MINER_THRESHOLD) {
            removeNearbyPossibleArchons();
            if(width*height >= MIN_MAPSIZE_AC) initiateArchonCore();
            moveOutOfHighRubble();
        }

        // shift init pulse
        shiftInitPulse();
        // update IDs if an archon dies
        if(rc.getArchonCount() != numArchons) {
            flushBetterIDs();
            setBetterArchonID();
            numArchons = rc.getArchonCount();
        }

        // update archon position (maybe just when we move? who cares, archons only use like 20 bytecode on a bad day)
        MapLocation currLoc = rc.getLocation();
        rc.writeSharedArray(Flag.ARCHON_INFO.getValue() + betterArchonID, currLoc.x * width + currLoc.y);

        // report nearby enemies
        RobotInfo[] enemies = rc.senseNearbyRobots(34, oppTeam);
        for(RobotInfo enemy : enemies) {
            if(enemy.type == RobotType.SOLDIER || enemy.type == RobotType.WATCHTOWER || enemy.type == RobotType.SAGE || enemy.type == RobotType.ARCHON) {
                registerEnemyPosOverride(enemy.location);
                break;
            }
        }

        // post lab job
        if((totalSpawns%labSpawnChance==0) && rc.readSharedArray(Flag.BUILDER_POSTING.getValue()) == 0 && getReservedLead() <= 250) {
            int labs = rc.readSharedArray(Flag.LAB_COORD.getValue());
            rc.writeSharedArray(Flag.LAB_COORD.getValue(), labs+1);
            //MapLocation loc = new MapLocation((labs*8)%width, ((labs*8)/width)*8);
            int x = trueRng.nextInt(20)-10;
            int y = trueRng.nextInt(20)-10;
            MapLocation loc = rc.getLocation().translate(x,y);
            if(loc.x >= 0 && loc.x < width && loc.y >= 0 && loc.y < height) {
                postJob(loc, RobotType.LABORATORY);
                totalSpawns++;
            }
        }

        if(rc.getMode() == RobotMode.PORTABLE) {
            movement(null);
        } else {
            int archons = rc.getArchonCount();
            boolean condition = archons == 1 || (turnCount % archons == betterArchonID);
            if (condition || (betterArchonID>turnCount%archons && rc.getTeamLeadAmount(myTeam)>80)) {
                sendMotivation();

                Direction spawnDir = DIRECTIONS[turnCount % 8];
                RobotType toSpawn = getBestSpawn();
                if(toSpawn != RobotType.ARCHON) {
                    rc.buildRobot(toSpawn, spawnDir);
                    totalSpawns++;
                }

            } else {
                // not spawning, might as well heal.
                heal();
            }
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

    public static boolean canBuildRobot(RobotType type, Direction dir) throws GameActionException {
        int reservedLead = rc.readSharedArray(Flag.RESERVED_LEAD.getValue());
        //System.out.println("Reserved lead: " + reservedLead);
        if(type.buildCostGold > 0) {
            int reservedGold = rc.readSharedArray(Flag.RESERVED_GOLD.getValue());
            return rc.getTeamGoldAmount(myTeam) - reservedGold > type.buildCostGold && rc.canBuildRobot(type, dir);
        } else {
            return rc.getTeamLeadAmount(myTeam) - reservedLead > type.buildCostLead && rc.canBuildRobot(type, dir);
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
    /*public static MapLocation getBestWatchtowerLocation() throws GameActionException {
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
    }*/

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

    public static RobotType getBestSpawn() throws GameActionException {
        Direction spawnDir = DIRECTIONS[turnCount % 8];
        int spawn = rng.nextInt(suicideBuilderSpawn);

        if(rc.getTeamGoldAmount(myTeam) >= 20 && rng.nextInt(10)<9) {
            if(canBuildRobot(RobotType.SAGE, spawnDir)) {
                // regular sages
                sendInitPulse(0, 0); // no idea what roles to put for sages yet
                return RobotType.SAGE;
            }
        } else if (turnCount < (height+width)/8 || spawn <= minerSpawn || rc.getTeamGoldAmount(myTeam) >= 20) {
            if(canBuildRobot(RobotType.MINER, spawnDir)) {
                // regular miners
                sendInitPulse(0, 0);
                return RobotType.MINER;
            }
        } else if (spawn <= soldierSpawn) {
            if (canBuildRobot(RobotType.SOLDIER, spawnDir)) {
                int n = rng.nextInt(enemyArchonLocations.length);
                // jumpers
                //if(rng.nextInt(5)==0 && getConfirmedEnemyArchon() != null) {
                //    sendInitPulse(getConfirmedEnemyArchon(), 14);
                //} else {
                    sendInitPulse(enemyArchonLocations[n], n+(betterArchonID*3));
                //}
                return RobotType.SOLDIER;
            }
        } else if (spawn <= builderSpawn || !firstBuilder) {
            firstBuilder = true;
            if(canBuildRobot(RobotType.BUILDER, spawnDir)) {
                // multipurpose builders
                sendInitPulse(0, 0);
                return RobotType.BUILDER;
            }
        } else if (spawn <= suicideBuilderSpawn) {
            if(canBuildRobot(RobotType.BUILDER, spawnDir)) {
                // suicide builders
                System.out.println("suicide builder");
                sendInitPulse(0, 1);
                return RobotType.BUILDER;
            }
        }

        return RobotType.ARCHON;
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
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3), ((locs[0].x * width + locs[0].y) << 2) | 1);
        locs[1] = new MapLocation((int) ((relX * -1) + originX), me.y);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3) + 1, ((locs[1].x * width + locs[1].y) << 2) | 1);
        locs[2] = new MapLocation(me.x, (int) ((relY * -1) + originY));
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + (betterArchonID * 3) + 2, ((locs[2].x * width + locs[2].y) << 2) | 1);

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

    public static MapLocation getConfirmedEnemyArchon() throws GameActionException {
        for(int i = Flag.ENEMY_ARCHON_POS.getValue(); i < Flag.ENEMY_ARCHON_POS.getValue()+12; i++) {
            int rawData = rc.readSharedArray(i);
            if((rawData&0b11) >= 1) {
                return new MapLocation((rawData>>2)/width, (rawData>>2)%width);
            }
        }
        return null;
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

    public static MapLocation getBestArchonLoc() throws GameActionException {
        Pair<MapLocation, Integer> best = null;
        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 34)) {
            if(best == null || rc.senseRubble(loc) < best.getValue() || (rc.senseRubble(loc) <= best.getValue() && rc.getLocation().distanceSquaredTo(best.getKey()) > rc.getLocation().distanceSquaredTo(loc))) {
                best = new Pair<>(loc, rc.senseRubble(loc));
            }
        }

        if(best != null) {
            return best.getKey();
        } else {
            return rc.getLocation();
        }
    }

    public static void removeNearbyPossibleArchons() throws GameActionException {
        for(int i = Flag.ENEMY_ARCHON_POS.getValue(); i < Flag.ENEMY_ARCHON_POS.getValue()+12; i++) {
            int rawData = rc.readSharedArray(i);
            MapLocation pos = new MapLocation((rawData>>2)/width, (rawData>>2)%width);
            if((rawData&0b11) == 1 && rc.canSenseLocation(pos)) {
                RobotInfo robotAtLoc = rc.senseRobotAtLocation(pos);
                if(robotAtLoc == null || robotAtLoc.team == myTeam || robotAtLoc.type != RobotType.ARCHON) {
                    rc.writeSharedArray(i, (pos.x * width + pos.y) << 2); // 0 simplified
                }
            }
        }
    }

    public static void initiateArchonCore() throws GameActionException {
        // more accurate for <=3 archons but i don't make the rules.
        int sumx = 0, sumy = 0;
        int archons = rc.getArchonCount();
        MapLocation currLoc = rc.getLocation();

        if(archons == 1) return; // no need to move

        for(int i = Flag.ARCHON_INFO.getValue(); i < Flag.ARCHON_INFO.getValue() + archons; i++) {
            int rawpos = rc.readSharedArray(i);
            MapLocation pos = new MapLocation(rawpos/width, rawpos%width);

            if((currLoc.distanceSquaredTo(pos) < MIN_ARCHDIST_AC || currLoc.distanceSquaredTo(pos) > MAX_ARCHDIST_AC) && !currLoc.equals(pos)) {
                System.out.println("Dist is " + currLoc.distanceSquaredTo(pos));
                return; // too far or too close, no need to move.
            }

            sumx += (rawpos/width);
            sumy += (rawpos%width);
        }

        MapLocation centroid = new MapLocation(sumx/archons, sumy/archons);
        System.out.println("Centroid at " + centroid);
        MapLocation assignedPos = centroid.translate(DIAG_DIRECTIONS[betterArchonID].dx*2, DIAG_DIRECTIONS[betterArchonID].dy*2); // we make a cool lattice lol

        boolean transformed = false;
        while(!transformed) {
            if(rc.canTransform()) {
                rc.transform();
                transformed = true;
                relocating = true;
            } else {
                Clock.yield();
            }
        }

        setPathfinding(assignedPos);
    }

    public static void moveOutOfHighRubble() throws GameActionException {
        // move out of high rubble
        MapLocation bestLoc = getBestArchonLoc();
        if(rc.senseRubble(rc.getLocation())*MIN_RUBBLE_ARCHONMOVE > rc.senseRubble(bestLoc)) {
            System.out.println("Semi");
            while(!rc.canTransform()) Clock.yield();

            System.out.println("All systems go for launch");
            moveTurn = 0;
            rc.transform();

            while(rc.getLocation().distanceSquaredTo(bestLoc) != 0 || moveTurn < MAX_ARCHON_MOVE_TURNS || !rc.canTransform()) {
                tryMoveTowards(bestLoc);
                moveTurn++;
                Clock.yield();
            }

            System.out.println("Home");
            rc.transform();
        }
    }

    /**
     * Required pathfinding callback for movement
     */
    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            if(relocating) {
                while (relocating) {
                    if(rc.canTransform()) {
                        rc.transform();
                        relocating = false;
                    } else {
                        Clock.yield();
                    }
                }
            }
        };
    }
}

