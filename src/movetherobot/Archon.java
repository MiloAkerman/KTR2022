package movetherobot;

import battlecode.common.*;
import movetherobot.Constants.*;

static enum Role {
    ATTACK,
    DEFEND,
    GATHER,
    BUILD
};

public class Archon {
    static int GATHER_INIT_MATERIALS_THRESHOLD = 5;
    static Role currRole;
    public static void setup(RobotController rc) throws GameActionException {
        currRole = Role.GATHER;
    }

    public static void run(RobotController rc) throws GameActionException {
        if(RobotPlayer.turnCount == GATHER_INIT_MATERIALS_THRESHOLD) {
            currRole = Role.ATTACK;
        }

        if(currRole == Role.GATHER) {
            rc.buildRobot(RobotType.MINER, Constants.DIRECTIONS[RobotPlayer.turnCount%8]);
        } else if (currRole == Role.ATTACK) {
            rc.buildRobot(RobotType.SOLDIER);
        }
    }
}
