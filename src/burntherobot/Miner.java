package burntherobot;

import battlecode.common.*;
import burntherobot.Constants.*;
import burntherobot.MapLocation;
import java.util.*;

enum MinerRole {
    MINE,
    WANDER,
    SCOUT
    /*
    SCOUT_N,
    SCOUT_NE,
    SCOUT_E,
    SCOUT_SE,
    SCOUT_S,
    SCOUT_SW,
    SCOUT_W,
    SCOUT_NW
     */
}

public class Miner extends Unit {
    static MinerRole currRole;
    static Direction scoutDirection;
    static boolean active = false;
    static int wanderlust = Constants.MIN_MINER_WANDER; // this could have been a simple frame counter but this is more realistic.
    static int lastDirIndex = 0;
    public static void setup() throws GameActionException {
        readPulse();
        if(roleIndex > 1) {
            scoutDirection = Constants.DIRECTIONS[roleIndex-2];
        }
        currRole = MinerRole.values()[roleIndex];
    }

    public static void run() throws GameActionException {
        if(currRole == MinerRole.MINE) {
            MapLocation[] visibleLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 1979); // using high num saves bytecode?
            if(!active) {
                HashMap<Integer, MapLocation> significantDeposits = new HashMap<Integer, MapLocation>();
                for (MapLocation loc : visibleLocs) {
                    int pb = rc.senseLead(loc);
                    if(pb > Constants.SIGNIFICANT_LEAD_THRESHOLD) {
                        significantDeposits.put(pb, loc);
                    }
                }

                if(significantDeposits.size() >= Constants.DEPOSIT_THRESHOLD) {
                    active = true;
                    mine(visibleLocs);
                } else {
                    Object[] depositArray = significantDeposits.entrySet().toArray();
                    if(depositArray.length > 0) {
                        Arrays.sort(depositArray, (Comparator) (o1, o2) -> ((HashMap.Entry<Integer, MapLocation>) o2).getKey()
                                .compareTo(((HashMap.Entry<Integer, MapLocation>) o1).getKey()));
                        destination = ((HashMap.Entry<Integer, MapLocation>)depositArray[0]).getValue();
                    } else {
                        djikstraWander();
                    }
                }
            } else {
                mine(visibleLocs);
            }
        } else if (currRole == MinerRole.WANDER) {
            wanderlust--;
            if(wanderlust < 1) {
                currRole = MinerRole.MINE;
            } else {
                djikstraWander();
            }
        } else if (currRole == MinerRole.SCOUT) {
            djikstraBiasedStep(scoutDirection);
        }
    }

    public static void mine(MapLocation[] visibleLocs) {
        int ores = 0, oreCount = 0;
        for (MapLocation loc : visibleLocs) {
            int pb = rc.senseLead(loc);
            if(rc.canMineLead(loc)) {
                ores++;
                oreCount += pb;
                if(pb>1) rc.mineLead(loc);
            }
        }
        if(oreCount < ores*2) { // let's not get too hasty. move on if we'll deplete ores next turn
            active = false;
            currRole = MinerRole.WANDER;
        }
    }
}
