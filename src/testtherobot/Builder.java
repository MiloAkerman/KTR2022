package testtherobot;

import battlecode.common.*;

enum BuilderRole {
    REPAIR,
    LAB,
    WATCHTOWER
}

public class Builder extends Unit {
    static Unit entourageFollow;
    static BuilderRole currRole;
    public static void setup() throws GameActionException {
        readPulse();
        currRole = BuilderRole.values()[roleIndex];
    }

    public static void run() throws GameActionException {
        //TODO: builder run function
    }
}
