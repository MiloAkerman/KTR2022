package icbmtherobot;
import battlecode.common.*;

import static icbmtherobot.Constants.*;
import static icbmtherobot.MathHelper.*;
import java.util.*;

enum PATH_END_REASON {
    SUCCESSFUL,
    TOO_MANY_BADDIES,
    TOO_MANY_FRIENDS
}
enum UNIT_MOOD {
    SAD,
    HAPPY
}

public class Unit extends RobotPlayer {
    public static int homeArchonID;
    public static int assignment;
    public static int addlInfo;
    public static Random trueRng = new Random();
    public static CallbackInterface pathCallback;
    public static int vsqr = rc.getType().visionRadiusSquared;

    static UNIT_MOOD mood = UNIT_MOOD.SAD;
    static String motivationalMessage;

    private static MapLocation destination = null;
    private static boolean goingToArchon = false;
    public static MapLocation[] borderTiles;

    //------------------------ HELPER METHODS -----------------------------

    public static void readPulse() throws GameActionException {
        // no other way to get archon ID :/
        int lastTurn = Math.max(turnCount-1, 0);
        MapLocation loc = rc.getLocation().add(DIRECTIONS[lastTurn%8].opposite());
        int defaultArchonID = 3;
        if(rc.canSenseRobotAtLocation(loc)) defaultArchonID = rc.senseRobotAtLocation(loc).ID;
        homeArchonID = getBetterArchonID(defaultArchonID);
        int rawInt = rc.readSharedArray(Flag.INIT_PULSE.getValue()+homeArchonID);
        addlInfo = rawInt & 0b1111;
        assignment = rawInt>>4;

        motivationalMessage = motivationReference[rc.readSharedArray(Flag.MOTIVATION.getValue())];
        mood = UNIT_MOOD.HAPPY; // motivated
    }
    public static int getBetterArchonID(int defaultID) throws GameActionException {
        for(int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue()+4; i++) {
            if(rc.readSharedArray(i) == defaultID) return i;
        }
        System.out.println("Final return statement reached on getBetterArchonID. Fifth archon confirmed????");
        //rc.resign();
        return 0; // this should NEVER happen
    }

    // Pathfinding

    public static void movement(MapLocation bias) throws GameActionException {
        if(goingToArchon) {
            MapLocation newLoc = getArchonHomeFromIndex(homeArchonID);
            if(!newLoc.equals(destination)) {
                destination = newLoc;
            }
        }

        if(destination != null) {
            //greedy pathfinding
            MapLocation currLoc = rc.getLocation();

            Direction[] sortedDirs = DIRECTIONS.clone();
            Arrays.sort(sortedDirs, Comparator.comparingInt(dir -> currLoc.add(dir).distanceSquaredTo(destination)));

            int currDist = currLoc.distanceSquaredTo(destination);

            Pair<Direction, Integer> bestMove = null;
            for (Direction direction : sortedDirs) {
                MapLocation newLoc = currLoc.add(direction);
                int newDist = newLoc.distanceSquaredTo(destination);
                if (newDist < currDist && rc.canMove(direction)) {
                    int newLocRubble = rc.senseRubble(newLoc);
                    if (bestMove == null || newLocRubble < bestMove.getValue()) {
                        bestMove = new Pair<>(direction, newLocRubble);
                    }
                }
            }

            // sometimes the best choice is to do nothing
            if(bestMove != null && rc.canMove(bestMove.getKey())) {
                if(bias != null) {
                    // NO MORE BIAS. TODAY WE FIGHT
                    /*MapLocation origin = new MapLocation(0, 0);
                    Direction newDir = origin.directionTo(origin.add(bestMove.getKey()).add(bias));

                    int biasRubble = rc.senseRubble(currLoc.add(newDir));
                    int regRubble = rc.senseRubble(currLoc.add(bestMove.getKey()));

                    if(rc.canMove(newDir) && biasRubble < (regRubble * RUBBLE_BIAS_THRESHOLD)) {
                        rc.move(newDir);
                    } else {
                        rc.move(bestMove.getKey());
                    }*/
                    tryMoveTowards(bias);
                } else {
                    rc.move(bestMove.getKey());
                }
            }

            // are we close to our destination?
            finchecks: if(rc.canSenseLocation(destination)) {
                // are we AT our destination?
                if(rc.getLocation().distanceSquaredTo(destination) == 0) {
                    endPathfinding(PATH_END_REASON.SUCCESSFUL);
                    break finchecks;
                }
            }
        }
    }
    public static void tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)) rc.move(dir);
        else if(rc.canMove(dir.rotateLeft())) rc.move(dir.rotateLeft());
        else if(rc.canMove(dir.rotateRight())) rc.move(dir.rotateRight());
    }
    public static void tryMoveTowards(MapLocation loc) throws GameActionException {
        MapLocation currLoc = rc.getLocation();

        Direction[] sortedDirs = DIRECTIONS.clone();
        Arrays.sort(sortedDirs, Comparator.comparingInt(dir -> currLoc.add(dir).distanceSquaredTo(loc)));

        int currDist = currLoc.distanceSquaredTo(loc);

        Pair<Direction, Integer> bestMove = null;
        for (Direction direction : sortedDirs) {
            MapLocation newLoc = currLoc.add(direction);
            int newDist = newLoc.distanceSquaredTo(loc);
            if (newDist < currDist && rc.canMove(direction)) {
                int newLocRubble = rc.senseRubble(newLoc);
                if (bestMove == null || newLocRubble < bestMove.getValue()) {
                    bestMove = new Pair<>(direction, newLocRubble);
                }
            }
        }

        if(bestMove != null) rc.move(bestMove.key);
    }
    public static void setPathfinding(MapLocation location) {
        goingToArchon = false;
        destination = location;
    }
    public static void setPathfindingToArchon() {
        goingToArchon = true;
    }
    public static MapLocation getPathfinding() {
        return destination;
    }
    public static void endPathfinding(PATH_END_REASON reason) throws GameActionException {
        destination = null;
        goingToArchon = false;
        pathCallback.onPathfindingEnd(reason);
    }
    public static int getMinersAtLocation(MapLocation loc, int radius) {
        RobotInfo[] robots = rc.senseNearbyRobots(loc, radius, myTeam);
        int count = 0;
        for (RobotInfo robot : robots) {
            if (robot.type == RobotType.MINER) count++;
        }
        return count;
    }
    public static MapLocation getBestAdjacentTile(MapLocation loc) throws GameActionException {
        Pair<MapLocation, Integer> bestPos = null;
        for(Direction dir : DIRECTIONS) {
            if(rc.canSenseLocation(loc.add(dir)) && ( bestPos==null || rc.senseRubble(loc.add(dir)) < bestPos.getValue())) {
                bestPos = new Pair<>(loc.add(dir), rc.senseRubble(loc.add(dir)));
            }
        }
        return bestPos.getKey();
    }
    public static Direction dirWithLeastRubble() throws GameActionException {
        MapLocation currLoc = rc.getLocation();

        Pair<Direction, Integer> bestDir = null;
        for(Direction dir : DIRECTIONS) {
            if(rc.canSenseLocation(currLoc.add(dir)) && bestDir==null || rc.senseRubble(currLoc.add(dir)) < bestDir.getValue()) {
                bestDir = new Pair<>(dir, rc.senseRubble(currLoc.add(dir)));
            }
        }
        if(bestDir != null) return bestDir.getKey();
        return Direction.CENTER;
    }
    public static void storeBorderVisTiles() throws GameActionException {
        int bc = Clock.getBytecodesLeft();
        MapLocation currLoc = rc.getLocation();
        List<MapLocation> toStore = new ArrayList<>();

        int counter = 0;
        for(MapLocation maploc : rc.getAllLocationsWithinRadiusSquared(currLoc, vsqr-3)) {
            MapLocation tr = new MapLocation(maploc.x+1, maploc.y+1);
            MapLocation tl = new MapLocation(maploc.x, maploc.y+1);
            MapLocation br = new MapLocation(maploc.x+1, maploc.y);

            Boolean sign = null;
            counter++;
            checks: {
                sign = borderFrml(maploc, currLoc);
                if(borderFrml(tr, currLoc) != sign) break checks;
                if(borderFrml(tl, currLoc) != sign) break checks;
                if(borderFrml(br, currLoc) != sign) break checks;
                continue;
            }

            // if we got here, we're in a border
            toStore.add(maploc);
        }

        borderTiles = toStore.toArray(new MapLocation[0]);
        for(MapLocation m : borderTiles) {
            rc.setIndicatorDot(m, 70, 40, 10);
        }
    }
    public static void storeBorderVisTiles2() {
        int bc = Clock.getBytecodesLeft();
        MapLocation currLoc = rc.getLocation();

        //MapLocation startPos =
    }
    public static boolean borderFrml(MapLocation loc, MapLocation origin) {
        double formula = ((vsqr-3) - Math.pow(loc.x-(origin.x+0.5), 2) - Math.pow(loc.y-(origin.y+0.5), 2));return formula >= 0;
    }
    public static void registerEnemyPos(MapLocation pos) throws GameActionException {
        for(int i = Flag.ENEMY_SOLDIERS.getValue(); i < Flag.ENEMY_SOLDIERS.getValue()+5; i++) {
            int rawData = rc.readSharedArray(i);
            MapLocation loc = new MapLocation(rawData/width, rawData%width);
            if(loc.distanceSquaredTo(pos) <= 10) return; //already logged
            if(rawData == 0) {
                rc.writeSharedArray(i, pos.x * width + pos.y);
                return;
            }
        }
    }
    public static void registerEnemyPosOverride(MapLocation pos) throws GameActionException {
        for(int i = Flag.ENEMY_SOLDIERS.getValue(); i < Flag.ENEMY_SOLDIERS.getValue()+5; i++) {
            int rawData = rc.readSharedArray(i);
            MapLocation loc = new MapLocation(rawData/width, rawData%width);
            if(loc.distanceSquaredTo(pos) <= 10) return; //already logged

            rc.writeSharedArray(i, pos.x * width + pos.y);
            return;
        }
    }
    public static void postJob(MapLocation loc, RobotType turret) throws GameActionException {
        if(rc.readSharedArray(Flag.BUILDER_POSTING.getValue()) == 0) {
            rc.writeSharedArray(Flag.BUILDER_POSTING.getValue(), (loc.x * width + loc.y) << 4 | turret.ordinal());
        }
    }
    public static void checkForGold() throws GameActionException {
        MapLocation[] gold = rc.senseNearbyLocationsWithGold(20);
        if(gold.length > 0 && rc.readSharedArray(Flag.GOLD.getValue()) == 0) {
            rc.writeSharedArray(Flag.GOLD.getValue(), gold[0].x * width + gold[0].y);
        }
    }

    public static void subtractReservedLead(int lead) throws GameActionException {
        int data = rc.readSharedArray(Flag.RESERVED_LEAD.getValue());
        data -= lead;
        if(data>=0) {
            rc.writeSharedArray(Flag.RESERVED_LEAD.getValue(), data);
        }
    }
    public static void addReservedLead(int lead) throws GameActionException {
        int data = rc.readSharedArray(Flag.RESERVED_LEAD.getValue());
        data += lead;
        if(data<=MAX_RESERVED_LEAD) {
            rc.writeSharedArray(Flag.RESERVED_LEAD.getValue(), data);
        }
        else System.out.println("====================== CAPPED!");
    }
    public static int getReservedLead() throws GameActionException {
        return rc.readSharedArray(Flag.RESERVED_LEAD.getValue());
    }
    public static void subtractReservedGold(int gold) throws GameActionException {
        int data = rc.readSharedArray(Flag.RESERVED_GOLD.getValue());
        data -= gold;
        if(data>=0) rc.writeSharedArray(Flag.RESERVED_GOLD.getValue(), data);
    }
    public static void addReservedGold(int gold) throws GameActionException {
        int data = rc.readSharedArray(Flag.RESERVED_GOLD.getValue());
        data += gold;
        if(data<=MAX_RESERVED_GOLD) rc.writeSharedArray(Flag.RESERVED_GOLD.getValue(), data);
    }

    public static boolean askForMutation() throws GameActionException {
        if(rc.readSharedArray(Flag.MUTATION.getValue()) == 0) {
            MapLocation currLoc = rc.getLocation();
            int formattedLoc = currLoc.x*width+currLoc.y;
            rc.writeSharedArray(Flag.MUTATION.getValue(), formattedLoc<<4 | rc.getType().ordinal());
            return true;
        }
        return false;
    }

    public static int spawnFormula(int[] array) {
        int map = width+height;

        if(array.length == 2) {
            return (int)(((double)(array[1]-array[0])/80) * (map-40) + array[0]);
        } else if(array.length == 3) {
            int res = (int)(((double)(array[1]-array[0]))/(80.0*(1-((double)array[2]/100.0))) * (map-(40+(80.0*((double)array[2]/100.0)))) + array[0]);

            if(res<array[0]) {
                return 0;
            } else {
                return res;
            }
        }

        return 0;
    }
    // sigmoid overload
    public static int spawnFormula(int[] array, double sigmoid) {
        int map = width+height;

        double e = 2.71828;
        return (int)(((double)(array[1]-array[0])/(1+Math.pow(e, sigmoid*(map-70)))) + array[0]);
    }

    public static void updateEnemyArchonExistence(int index, int exists) throws GameActionException {
        int data1 = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue() + index);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index, ((data1>>2)<<2) | exists);
        int data2 = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(3)+index);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index, ((data2>>2)<<2) | exists);
        int data3 = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(6)+index);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index, ((data3>>2)<<2) | exists);
    }
    public static MapLocation getArchonTargetFromIndex(int index) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index);
        return new MapLocation((data>>2)/width, (data>>2)%width);
    }
    public static MapLocation getArchonHomeFromIndex(int index) throws GameActionException {
        int newLocRaw = rc.readSharedArray(Flag.ARCHON_INFO.getValue()+index);
        MapLocation newLoc = new MapLocation(newLocRaw/width, newLocRaw%width);
        return newLoc;
    }
    public static boolean isTargetThere(int target) throws GameActionException {
        // copied from getArchonTargetFromIndex() but it doesn't matter cuz bytecode
        if(target==-1) return false;
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+target);
        //if(turnCount > 520 && turnCount < 550) System.out.println("Read " + data + " from " + (Flag.ENEMY_ARCHON_POS.getValue()+target));
        return (data&0b11)>0;
    }
    public static int getLeadAggregate() throws GameActionException {
        int leadAgg = 0;
        for(MapLocation leadDeposit : rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared)) {
            leadAgg += rc.senseLead(leadDeposit);
        }
        return leadAgg;
    }
}
