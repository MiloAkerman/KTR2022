package testtherobot;

import battlecode.common.*;

enum Role {
    ATTACK,
    SCOUT,
    GATHER,
    ALPHA_RUSH,
    BETA_RUSH
}
enum Flag {
    DROID_INIT(5);

    private final int value;
    Flag(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}

public class Archon extends Unit {
    static int scoutFinishedRound = 0;
    static boolean scoutWasFinished = false;
    static MapLocation coords;

    static Direction safestDirection;
    static Direction[] frontlines = new Direction[4];
    static Role currRole;
    public static void setup() {
        rc.setIndicatorString("archon up");
        currRole = Role.GATHER;
        coords = rc.getLocation();
        home = coords;
    }

    public static void run() throws GameActionException {
        if(RobotPlayer.turnCount == Constants.GATHER_INIT_MATERIALS_THRESHOLD) {
            currRole = Role.SCOUT;
        } else if(RobotPlayer.turnCount == Constants.GATHER_INIT_MATERIALS_THRESHOLD+8) {
            currRole = Role.GATHER;
        } else if(scoutFinishedRound != 0 && !scoutWasFinished) {
            currRole = Role.ATTACK;
            scoutWasFinished = true;
        } else if(scoutWasFinished && RobotPlayer.turnCount == scoutFinishedRound + Constants.RUSH_INIT_THRESHOLD) {
            currRole = Role.ALPHA_RUSH;
        }

        if(currRole == Role.GATHER) {
            sendInitPulse(1);
            rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8]);
            rc.setIndicatorString("MINER");
        } else if (currRole == Role.SCOUT) {
            sendInitPulse(2+(RobotPlayer.turnCount % 8));
            rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8]);
        } else if (currRole == Role.ATTACK) {
            rc.buildRobot(RobotType.SOLDIER, frontlines[RobotPlayer.turnCount % frontlines.length]);
        } else if (currRole == Role.ALPHA_RUSH) {
            rc.buildRobot(RobotType.BUILDER, safestDirection);
        } else if (currRole == Role.BETA_RUSH) {
            if(rc.getTeamGoldAmount(RobotPlayer.myTeam) >= 50) {
                rc.buildRobot(RobotType.SAGE, safestDirection);
            } else {
                rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount%8]);
            }
        }
    }

    public static void sendInitPulse(int role) throws GameActionException {
        MapLocation loc = rc.getLocation();
        int formattedLoc = (loc.x)*width+(loc.y);
        int pulse = (formattedLoc << 4) | role;
        rc.writeSharedArray(Flag.DROID_INIT.getValue(), pulse);
    }
}
