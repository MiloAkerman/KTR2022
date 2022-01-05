package burntherobot;

import battlecode.common.*;
import burntherobot.Constants.*;
import burntherobot.MapLocation;

public class Unit extends RobotPlayer {
    public static MapLocation home;
    public static MapLocation destination;
    public static int roleIndex;
    public static int lastDirIndex;

    public static void readPulse() {
        int rawInt = rc.readSharedArray(4);
        roleIndex = rawInt & 0b1111;
        home = new MapLocation(((rawInt>>4)/width), ((rawInt>>4)%width));
    }

    public static void djikstraWander() {

    }

    public static void djikstraBiasedStep(Direction dir) {

    }
}
