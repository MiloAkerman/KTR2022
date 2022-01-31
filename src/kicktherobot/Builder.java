package kicktherobot;

import battlecode.common.*;
import static kicktherobot.Constants.*;

enum BuilderRole {
    SUICIDE, // for the greater good!
    DEFENSE
}

public class Builder extends Unit {
    static BuilderRole currRole;

    public static void setup() throws GameActionException {
        readPulse();
        if(addlInfo==1) {
            currRole = BuilderRole.SUICIDE;
            setPathfindingToArchon();
        }
    }

    public static void run() throws GameActionException {
        if(currRole == BuilderRole.SUICIDE) {
            if(rc.senseLead(rc.getLocation()) == 0) rc.disintegrate();
            tryMove(DIRECTIONS[rng.nextInt(DIRECTIONS.length)]);
            movement(false);
        }
    }
}
