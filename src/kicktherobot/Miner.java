package kicktherobot;
import battlecode.common.*;
import kicktherobot.Pair;

import static  kicktherobot.Constants.*;
import java.util.*;

enum MinerMode {
    REGULAR,
    MTS
}

public class Miner extends Unit {
    static boolean lockedOn = false;
    static boolean mining = false;
    static MinerMode mode;
    static Direction MTSDir;

    public static void setup() throws GameActionException {
        readPulse();
        handlePathfindingCallback();
        mode = addlInfo==1? MinerMode.MTS : MinerMode.REGULAR;
        if(mode==MinerMode.MTS) MTSDir = DIRECTIONS[assignment];
    }

    public static void run() throws GameActionException {
        if(mining) mine(); // pretty self-explanatory

        if(mode == MinerMode.REGULAR) {
            // hey, I see a cluster! This takes priority over any other movement
            Pair<MapLocation, Integer> leadDeposit = searchPbCluster();
            if(leadDeposit.getValue() != 0 && (getMinersAtLocation(leadDeposit.getKey()) < TOO_MANY_MINERS)) { // if there's one or more clusters
                setPathfinding(leadDeposit.getKey());
                lockedOn = true;
            } else {
                if(mining) mining = false;
                if(getMinersAtLocation(leadDeposit.getKey()) >= TOO_MANY_MINERS) pickWanderLocation();
            }

            // we're not doing anything. Get moving!
            if(getPathfinding() == null && !mining) pickWanderLocation();
        } else {
            // maptestsmall code lol
            if(spawnTurnCount<3) tryMove(MTSDir);
            mine();
            if(!isActionAreaUseful(10, 30)) tryMove(MTSDir);
        }

        movement(true); // always call movement after each turn
    }

    //----------------------- HELPER METHODS -----------------------------

    public static void mine() throws GameActionException {
        MapLocation[] visibleLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);
        for (MapLocation loc : visibleLocs) {
            int pb = rc.senseLead(loc);
            if(rc.canMineLead(loc)) {
                if(pb>1) {
                    rc.mineLead(loc);
                    rc.setIndicatorLine(rc.getLocation(), loc, 200, 200, 0);
                }
            }
        }
    }
    public static void pickWanderLocation() {
        int x = rng.nextInt(width);
        int y = rng.nextInt(height);
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

    // --------------------- LOCATION CHECKING --------------------------

    /**
     * /*
     * @returns: MapLocation with the highest cluster of lead
     * Probably insane bytecode cost so if anything just replace with leadDeposits[0] lol
     */
    public static Pair<MapLocation, Integer> searchPbCluster() throws GameActionException {
        MapLocation[] leadDeposits = rc.senseNearbyLocationsWithLead(20); // vision radius i believe
        HashMap<MapLocation, Integer> clusters = new HashMap<>();
        for(MapLocation pbLoc : leadDeposits) {
            if(rc.senseLead(pbLoc) < 30) continue;
            int clusterSize = 1;
            for(int i = 0; i < DIRECTIONS.length; i++) {
                if(rc.canSenseLocation(pbLoc.add(DIRECTIONS[i])) && rc.senseLead(pbLoc.add(DIRECTIONS[i])) > 1) {
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
