package burntherobot;

import battlecode.common.*;
import scala.collection.immutable.Stream;

enum Role {
    ATTACK,
    SCOUT,
    GATHER,
    ALPHA_RUSH,
    BETA_RUSH,
    NONE
}

public class Archon extends Unit {
    static boolean hasBuiltFirstMiner = false;

    static Direction safestDirection;
    static Direction[] frontlines = new Direction[4];
    static Role currRole;
    public static void setup() throws GameActionException {
        currRole = Role.GATHER;
        for(int i = 0; i < 4; i++) {
            int foo = Constants.Flag.MINER_GROUPS.getValue()+(i*(Constants.MAX_GROUPS_PER_ARCHON+1)); //is this the real life? is this just fantasy?
            System.out.println("Printed " + rc.getID() + " to " + foo);
            if(rc.readSharedArray(foo) == 0) {
                rc.writeSharedArray(foo, rc.getID());
                break;
            }
        }
    }

    public static void run() throws GameActionException {
        if(turnCount >= 200) currRole = Role.NONE;

        if(currRole == Role.GATHER) {
            if(rc.canBuildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8]) && rng.nextInt(20) <= 2) {
                if(hasBuiltFirstMiner){ //(rng.nextInt(8) != 1) {
                    sendInitPulse(0);
                } else {
                    System.out.println("Leader made!");
                    sendInitPulse(1);
                    hasBuiltFirstMiner = true;
                }
                    rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8]);
            }
        } else if (currRole == Role.SCOUT) {
            if(rc.canBuildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8])) {
                sendInitPulse(2 + (RobotPlayer.turnCount % 8));
                rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount % 8]);
            }
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
        int archonID = rc.getID();
        int pulse = (archonID << 4) | role;
        rc.writeSharedArray(Constants.Flag.DROID_INIT.getValue(), pulse);
        System.out.println("Sent " + archonID);
    }
}
