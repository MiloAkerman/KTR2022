package punttherobot;
import battlecode.common.*;

public class Constants {
    public static final Direction[] DIRECTIONS = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST,
            Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST,
            Direction.NORTHWEST };
    public static final Direction[] DIAG_DIRECTIONS = { Direction.NORTHEAST, Direction.SOUTHEAST,
            Direction.SOUTHWEST, Direction.NORTHWEST };
    public static final Direction[] CARD_DIRECTIONS = { Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST };
    public static final RobotType[] SPAWNABLE_ROBOTS = {  };
                                              //ARCHON, BUILDER, LAB, MINER, SAGE, SOLDIER, WATCHTOWER
    public static final int[] ENEMY_PRIORITY = { 3,     2,       1,   0,     5,    4,       6 };
    public enum Flag {
        ARCHON_ID_ASSIGNMENT(0), // ...3
        ARCHON_INFO(4), //...7
        INIT_PULSE_UPC(8), //...11
        INIT_PULSE(12), //...15
        ENEMY_ARCHON_POS(16), //...27
        WATCHTOWER_POS(28), // 31
        SPAWN_QUEUE(32),
        GOLD(33),
        ENEMY_SOLDIERS(34), // ..40
        MOTIVATION(41);

        private final int value;
        Flag(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    public static String[] motivationReference = new String[] {
            "You're doing great!",
            "Keep going!",
            "We're almost there!",
            "We're counting on you, %s!",
            "Dulce et decorum est pro patria mori.",
            "Your fellow droids are counting on you!",
            "YOU are the line between CIVILIZATION and DEATH on Mars!",
            "FOR SPARTA!",
            "Good droids follow orders.",
            "STOP the enemy team!",
            "Remember: War crimes are OK if they are done more than 200 units away from Archons.",
            "Your daily ration of polluted cow soup is on the way. Hold tight.",
            "No deserting allowed!",
            "Alert your superior if you see any enemy watchtowers.",
            "No sleeping on duty!",
            "We love you!"
    };

    public static int TOO_MANY_MINERS = 4;
    public static int WATCHTOWER_LATTICE_SIZE = 3; // <=4
    public static int MAX_REG_WATCHTOWER_LOCS = 4;
    public static int MAX_BUILDERS_PER_ARCHON = 8;
    public static int MIN_CLUSTER_LEAD = 11;
    public static int MIN_CLUSTER_PERIPHERAL_LEAD = 1;
    public static int MAX_ACCEPTABLE_ARCHON_RUBBLE = 50;
    public static int MAX_ACCEPTABLE_AVG_RUBBLE = 30;
    public static int MAX_ARCHON_MOVE_TURNS = 10; // min 10 bc cooldown
    public static int MIN_ENEMIES_DISTRESS = 1;
    public static int MAX_DISTRESS_TURNS = 1;
    public static int OFFENSIVE_MINER_CHANCE = 6; // 1/6
    public static int LEAD_PROT_DIST_SQR = 200; // if more than this, suck up all lead.
    public static double SOLDIER_PRI_RUBBLE_WEIGHT = 0.1;
    public static double SOLDIER_PRI_HEALTH_WEIGHT = 20; // works as a percentage of 100
    public static double SOLDIER_PRI_TYPE_WEIGHT = 10;
    public static double RUBBLE_BIAS_THRESHOLD = 1.35; // 35% extra rubble accepted for biased movement
}