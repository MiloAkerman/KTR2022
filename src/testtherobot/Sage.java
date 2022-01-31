package testtherobot;
import battlecode.common.*;

enum SageRole {

}

//TODO: Sage logic
public class Sage extends Unit {
    static SageRole currRole;
    public static void setup() throws GameActionException {
        readPulse();
        currRole = SageRole.values()[roleIndex];
    }

    public static void run() throws GameActionException {

    }
}
