package icbmtherobot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static icbmtherobot.Constants.*;

public class Laboratory extends Unit {
    static int reservedAt = -1;
    static int lastLevel = 1;
    static int mutOnHold = 0;
    static int reservedLead = 0;
    static int reservedGold = 0;

    public static void setup() {

    }

    public static void run() throws GameActionException {
        rc.setIndicatorString("" + rc.getTransmutationRate());

        MapLocation currLoc = rc.getLocation();
        if(rc.canTransmute()) rc.transmute();

        if(rc.getLevel() != lastLevel && rc.getLevel() == 2) {
            lastLevel = rc.getLevel();
            subtractReservedLead(150);
            reservedLead -= 150;
            System.out.println("NEW LE 1 ===================");
            reservedAt = -1;
        } else if (rc.getLevel() != lastLevel && rc.getLevel() == 3) {
            lastLevel = rc.getLevel();
            subtractReservedGold(25);
            reservedGold -= 25;
            System.out.println("NEW LE 2 ===================");
            reservedAt = -1;
        }
        lastLevel = rc.getLevel();

        // cancel reservation if over 100 turns
        if(reservedAt != -1 && reservedAt + 150 < turnCount) {
            System.out.println("Reservation at " + reservedAt + " too old. Resetting...");
            if(rc.getLevel() == 1) {
                subtractReservedLead(150);
                reservedLead -= 150;
            }
            else if (rc.getLevel() == 2) {
                subtractReservedGold(25);
                reservedGold -= 25;
            }
            reservedAt = -1;
        }

        if(spawnTurnCount%90==89 && reservedAt == -1) {
            if(rc.getLevel() == 1) {
                boolean successful = askForMutation();
                if(successful) {
                    System.out.println("Requested mutation 1");
                    addReservedLead(150);
                    reservedLead += 150;
                    reservedAt = turnCount;
                }
            } else if(rc.getLevel() == 2) {
                boolean successful = askForMutation();
                if(successful) {
                    addReservedGold(25);
                    reservedGold += 25;
                    System.out.println("Requested mutation 2");
                    reservedAt = turnCount;
                }
            }
        }
    }
}
