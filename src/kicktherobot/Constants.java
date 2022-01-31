package kicktherobot;
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
                                              //ARCHON, BUILDER, LAB, MINER, SAGE, SOLDIER, WATCHTOWER
    public static final int[] ENEMY_PRIORITY = { 6,     2,       1,   0,     5,    3,       4 };
    public enum Flag {
        ARCHON_ID_ASSIGNMENT(0), // ...3
        ARCHON_INFO(4), //...7
        INIT_PULSE_UPC(8), //...11
        INIT_PULSE(12), //...15
        ENEMY_ARCHON_POS(16); //...27

        private final int value;
        Flag(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    public static int TOO_MANY_MINERS = 7;
}