package orbitalstriketherobot;
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
        LAB_COORD(28),
        GOLD(29),
        LEAD(30), // 34
        ENEMY_SOLDIERS(35), // ..40
        MOTIVATION(41),
        WATCHTOWER_EXPAND(42),
        RESERVED_LEAD(43),
        RESERVED_GOLD(44),
        BUILDER_POSTING(45),
        MUTATION(46);

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

    public static int[] MINER_SPAWN_BOUNDS = new int[] { 0, 20 };
    public static int[] SOLDIER_SPAWN_BOUNDS = new int[] { 97, 34 };
    public static int[] REG_BUILDER_SPAWN_BOUNDS = new int[] { 1, 6, 50 };
    public static int[] SUICIDE_BUILDER_SPAWN_BOUNDS = new int[] { 2, 30, 30 };

    public static int[] LAB_POST_CHANCE = new int[] { 25, 4 };
    public static double SIGMOID_LAB_A = -0.19;

    public static int[] SOLDIER_RUSH_BOUNDS = new int[] { 3, 6 };
    public static int[] SAGE_RUSH_BOUNDS = new int[] { 4, 6 };

    public static int MAX_RESERVED_LEAD = 600;
    public static int MAX_RESERVED_GOLD = 100;

    public static int SAGE_CHARGE_THRESH = 6; // IN ENEMIES
    public static int SAGE_ABYSS_THRESH = 150; // IN AGGREGATE LEAD
    public static int SAGE_RADIUS_ABYSS = 400; // IN SQUARE DIST, how close to enemy before envisioning (and far from us, in that edge case)

    public static int MIN_MAPSIZE_AC = 1225;
    public static int MIN_ARCHDIST_AC = 10000;
    public static int MAX_ARCHDIST_AC = 900;

    public static int TOO_MANY_MINERS = 4;
    public static int MIN_LEAD_TO_COMM = 100;
    public static int MAX_MINERS_PER_COMM = 5;
    public static int MINER_THRESHOLD = 3;
    public static int WATCHTOWER_LATTICE_SIZE = 3; // <=4
    public static int MAX_REG_WATCHTOWER_LOCS = 4;
    public static int MAX_BUILDERS_PER_ARCHON = 8;

    public static int MIN_CLUSTER_LEAD = 11;
    public static int MIN_CLUSTER_PERIPHERAL_LEAD = 1;
    public static double MIN_RUBBLE_ARCHONMOVE = 0.7;
    public static int MAX_ARCHON_MOVE_TURNS = 10; // min 10 bc cooldown

    public static int OFFENSIVE_MINER_CHANCE = 10; // 1/10
    public static int LEAD_ATK_DIST_SQR = 50; // if less than this, suck up all lead.
    public static double SOLDIER_PRI_RUBBLE_WEIGHT = 0.1;
    public static double SOLDIER_PRI_HEALTH_WEIGHT = 20; // works as a percentage of 100
    public static double SOLDIER_PRI_TYPE_WEIGHT = 10;
    public static double SOLDIER_PRI_INRANGE_WEIGHT = 100;
    public static double RUBBLE_BIAS_THRESHOLD = 1.35; // 35% extra rubble accepted for biased movement
}