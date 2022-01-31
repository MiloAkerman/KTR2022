package punttherobot;
import battlecode.common.*;

import static punttherobot.Constants.*;
import java.util.*;

enum MinerMode {
    REGULAR,
    MTS
}

public class Miner extends Unit {
    static boolean lockedOn = false;
    static boolean mining = false;
    static boolean seekingGold = false;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
    }

    public static void run() throws GameActionException {
        movement(null); // always call movement after each turn

        if(rc.getHealth() < rc.getType().getMaxHealth(rc.getLevel())) {
            mining = false;
            lockedOn = false;
            setPathfindingToArchon();
        } else if (getPathfinding() != null && getPathfinding().equals(getHomeArchonPos())) {
            pickWanderLocation();
        }

        // get fellow soldier bias
        Direction bestBias = Direction.CENTER;
        /*
        for(RobotInfo enemy : rc.senseNearbyRobots(20, oppTeam)) {
            if(enemy.type == RobotType.SOLDIER) {
                bestBias = rc.getLocation().directionTo(enemy.location);
                break;
            }
        }*/

        if(isActionAreaUseful(1, 3)) {
            mining = true;
        }

        if(mining) {
            mine();

            if(!isActionAreaUseful(1, 100)) {
                mining = false;
                seekingGold = false;
                pickWanderLocation();
            }
        } else {
            // if we already have a location we don't need another
            if(!lockedOn) {
                Pair<MapLocation, Integer> leadDeposit = searchPbCluster();
                if(leadDeposit.getValue() != 0 && !rc.canSenseRobotAtLocation(leadDeposit.getKey()) && getMinersAtLocation(leadDeposit.getKey(), 3) <= TOO_MANY_MINERS) {
                    lockedOn = true;
                    setPathfinding(leadDeposit.getKey());
                }
            } else {
                if(getPathfinding() != null && rc.canSenseRobotAtLocation(getPathfinding())) {
                    pickWanderLocation();
                }
            }
            if(getPathfinding() == null) pickWanderLocation();
        }

        // mining is ok but we hate soldiers
        RobotInfo[] enemies = rc.senseNearbyRobots(20, oppTeam);
        for(RobotInfo enemy : enemies) {
            if(enemy.type == RobotType.SOLDIER || enemy.type == RobotType.WATCHTOWER || enemy.type == RobotType.SAGE) {
                mining = false;
                lockedOn = false;
                setPathfinding(null);
                tryMove(rc.getLocation().directionTo(enemy.location).opposite());
                break;
            }
        }

        if(bestBias != Direction.CENTER) {
            movement(bestBias);
        }
        if(lockedOn) rc.setIndicatorString("LOCKED ON to " + getPathfinding());
        if(!lockedOn && getPathfinding() != null) rc.setIndicatorString("WANDERING to " + getPathfinding());
    }

    //----------------------- HELPER METHODS -----------------------------

    public static void mine() throws GameActionException {
        int mined = 0;
        mineloop: for(int i = 0; i < 5; i++) {
            for (MapLocation loc : rc.senseNearbyLocationsWithLead(2)) {
                if(rc.canMineLead(loc)) {
                    if(canCommitWarCrime()) {
                        rc.mineLead(loc);
                        rc.setIndicatorLine(rc.getLocation(), loc, 200, 200, 0);
                    } else if(rc.senseLead(loc)>1) {
                        rc.mineLead(loc);
                        rc.setIndicatorLine(rc.getLocation(), loc, 200, 200, 0);
                    }
                    if(++mined == 5) break mineloop;
                }
            }
        }

        // gold loop
        if(seekingGold) {
            for (MapLocation loc : rc.senseNearbyLocationsWithGold(2)) {
                if(rc.canMineGold(loc)) {
                    rc.mineGold(loc);
                    rc.setIndicatorLine(rc.getLocation(), loc, 200, 200, 0);
                }
            }
        }
    }

    // can suck up all lead
    public static boolean canCommitWarCrime() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        for(int i = Flag.ARCHON_INFO.getValue(); i < Flag.ARCHON_INFO.getValue()+4; i++) {
            int rawData = rc.readSharedArray(i);
            if(currLoc.distanceSquaredTo(new MapLocation(rawData/width, rawData%width)) < LEAD_PROT_DIST_SQR) {
                return false;
            }
        }

        return true;
    }
    public static boolean isTargetThere(int target) throws GameActionException {
        // copied from soldier code and the copy chain continues
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+target);
        return (data&0b1)==1;
    }
    public static MapLocation getArchonTargetFromIndex(int index) throws GameActionException {
        int data = rc.readSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+index);
        return new MapLocation((data>>1)/width, (data>>1)%width);
    }
    public static MapLocation getBestEnemyArchon() throws GameActionException {
        MapLocation currLoc = rc.getLocation();
        Pair<MapLocation, Integer> bestTarget = null;
        for(int i = 0; i < 12; i++) {
            if(isTargetThere(i)) {
                return getArchonTargetFromIndex(i);
            }
        }
        return new MapLocation(61, 61);
    }
    public static void pickWanderLocation() throws GameActionException {
        // we like gold! gold is shiny and good and shiny and good and shiny and good and shiny and good and shiny and good and shiny and good and shiny
        int goldData = rc.readSharedArray(Flag.GOLD.getValue());
        if(goldData != 0) {
            setPathfinding(new MapLocation(goldData/width, goldData%width));
            lockedOn = true;
            seekingGold = true;
            rc.writeSharedArray(Flag.GOLD.getValue(), 0);
            return;
        }
        if(trueRng.nextInt(OFFENSIVE_MINER_CHANCE) == 0) {
            MapLocation res = getBestEnemyArchon();
            if(res.x <= 60) {
                setPathfinding(res);
                return;
            }
        }

        lockedOn = false;
        int x = trueRng.nextInt(width);
        int y = trueRng.nextInt(height);
        setPathfinding(new MapLocation(x, y));
    }

    public static void handlePathfindingCallback() {
        pathCallback = reason -> {
            //System.out.println("Pathfinding ended with reason " + reason);
            if(reason == PATH_END_REASON.SUCCESSFUL) {
                if(lockedOn) {
                    lockedOn = false;
                    mining = true;
                } else {
                    if(isActionAreaUseful(30, 7)) mining = true;
                }
            }
        };
    }
    public static MapLocation getHomeArchonPos() throws GameActionException {
        int newLocRaw = rc.readSharedArray(Flag.ARCHON_INFO.getValue()+homeArchonID);
        return new MapLocation(newLocRaw/width, newLocRaw%width);
    }

    // --------------------- LOCATION CHECKING --------------------------

    /**
     * /*
     * @returns: MapLocation with the highest cluster of lead
     * Probably insane bytecode cost so if anything just replace with leadDeposits[0] lol
     */
    public static Pair<MapLocation, Integer> searchPbCluster() throws GameActionException {
        MapLocation[] leadDeposits = rc.senseNearbyLocationsWithLead(20); // vision radius i believe
        if(leadDeposits.length > 10) return new Pair<>(leadDeposits[trueRng.nextInt(leadDeposits.length)], 1); // no time to calculate clusters!

        HashMap<MapLocation, Integer> clusters = new HashMap<>();
        for(MapLocation pbLoc : leadDeposits) {
            if(rc.senseLead(pbLoc) < MIN_CLUSTER_LEAD) continue;
            int clusterSize = 1;
            for(int i = 0; i < DIRECTIONS.length; i++) {
                if(rc.canSenseLocation(pbLoc.add(DIRECTIONS[i])) && rc.senseLead(pbLoc.add(DIRECTIONS[i])) > MIN_CLUSTER_PERIPHERAL_LEAD) {
                    clusterSize += 1;
                }
            }
            clusters.put(pbLoc, clusterSize);
        }
        Object[] clusterArray = clusters.entrySet().toArray();
        Arrays.sort(clusterArray, (Comparator<Object>) (o1, o2) -> ((HashMap.Entry<MapLocation, Integer>) o2).getValue().compareTo(((HashMap.Entry<MapLocation, Integer>) o1).getValue()));
        if(clusterArray.length == 0) {
            return new Pair<>(new MapLocation(0, 0), 0);
        } else {
            HashMap.Entry<MapLocation, Integer> obj = (HashMap.Entry<MapLocation, Integer>)clusterArray[0];
            return new Pair<>(obj.getKey(), obj.getValue());
        }
    }
    public static boolean isActionAreaUseful(int minLead, int cc) throws GameActionException {
        MapLocation me = rc.getLocation();
        for(int dx = -1; dx <= 1; dx++) {
            for(int dy = -1; dy <= 1; dy++) {
                MapLocation maploc = new MapLocation(me.x + dx, me.y + dy);
                if(rc.onTheMap(maploc) && rc.senseLead(maploc) > minLead && rc.senseNearbyRobots(20).length < cc) return true;
            }
        }
        return false;
    }
}
