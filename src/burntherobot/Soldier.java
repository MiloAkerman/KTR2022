package burntherobot;

import battlecode.common.*;

enum SoldierRole {

}

//TODO: Soldier logic
public class Soldier extends Unit {
    static SoldierRole currRole;
    public static void setup() throws GameActionException {
        readPulse();
        currRole = SoldierRole.values()[roleIndex];
    }

    public static void run() throws GameActionException {
        djikstraWander();
    }
}
