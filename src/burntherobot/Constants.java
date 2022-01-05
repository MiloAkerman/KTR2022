package burntherobot;

import battlecode.common.*;

public class Constants {
    public static final Direction[] DIRECTIONS = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST,
            Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST,
            Direction.NORTHWEST, };
    public static final Direction[] DIAG_DIRECTIONS = { Direction.NORTHEAST, Direction.SOUTHEAST,
            Direction.SOUTHWEST, Direction.NORTHWEST, };
    public static final Direction[] CARD_DIRECTIONS = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST, };
    public static final RobotType[] SPAWNABLE_ROBOTS = {  };

    //format: public static final int STANDARD_DEFEND_POLI_CONVICTION = 20;
    public static int GATHER_INIT_MATERIALS_THRESHOLD = 20;
    public static int RUSH_INIT_THRESHOLD = 35;
    public static int SIGNIFICANT_LEAD_THRESHOLD = 5;
    public static int DEPOSIT_THRESHOLD = 3;
    public static int MIN_MINER_WANDER = 10;
}