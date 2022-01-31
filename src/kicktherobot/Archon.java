package kicktherobot;
import battlecode.common.*;
import static kicktherobot.Constants.*;

public class Archon extends Unit {
    static int betterArchonID;
    static int numArchons;
    static boolean MTS;

    static MapLocation[] enemyArchonLocations = new MapLocation[3];

    //---------------------------- CODE -----------------------------------

    public static void setup() throws GameActionException {
        betterArchonID = setBetterArchonID();
        enemyArchonLocations = possibleArchonLocations();
        numArchons = rc.getArchonCount();
        CheckIfMTS();
    }

    public static void run() throws GameActionException {
        shiftInitPulse();

        // update archon position (maybe just when we move? who cares, archons only use like 20 bytecode on a bad day)
        MapLocation currLoc = rc.getLocation();
        rc.writeSharedArray(Flag.ARCHON_INFO.getValue()+betterArchonID, currLoc.x*width+currLoc.y);
        Direction dir = DIRECTIONS[turnCount%8];

        // spawn check!
        if(MTS) {
            if(turnCount < 15) {
                if(rc.canBuildRobot(RobotType.MINER, DIRECTIONS[turnCount%8])) {
                    sendInitPulse(turnCount%8, 1);
                    rc.buildRobot(RobotType.MINER, DIRECTIONS[turnCount%8]);
                }
            } else {
                int n = rng.nextInt(enemyArchonLocations.length);
                sendInitPulse(enemyArchonLocations[n], n);
                if(rc.canBuildRobot(RobotType.SOLDIER, dir)) rc.buildRobot(RobotType.SOLDIER, dir);
            }
        } else {
            if(rng.nextInt(3) == 0) {
                int spawn = rng.nextInt(5);
                if(spawn == 0) {
                    sendInitPulse(0, 0);
                    if (rc.canBuildRobot(RobotType.MINER, dir)) rc.buildRobot(RobotType.MINER, dir);
                } else if (spawn == 1) {
                    sendInitPulse(0,1);
                    if (rc.canBuildRobot(RobotType.BUILDER, dir)) rc.buildRobot(RobotType.BUILDER, dir);
                } else {
                    int n = rng.nextInt(enemyArchonLocations.length);
                    sendInitPulse(enemyArchonLocations[n], n);
                    if(rc.canBuildRobot(RobotType.SOLDIER, dir)) rc.buildRobot(RobotType.SOLDIER, dir);
                }
            }
        }
    }

    //------------------------ HELPER METHODS -----------------------------

    /**
     * Allocates a better Archon ID (if we indexed with the actual ID, it would be all over the place)
     * @return better Archon ID allocated
     * @throws GameActionException reg exception
     */
    public static int setBetterArchonID() throws GameActionException {
        // Cycle through each betterID slot looking for an empty one
        for(int i = Flag.ARCHON_ID_ASSIGNMENT.getValue(); i < Flag.ARCHON_ID_ASSIGNMENT.getValue()+4; i++) {
            if(rc.readSharedArray(i) == 0) {
                rc.writeSharedArray(i, rc.getID());
                return i;
            }
        }
        System.out.println("Final return statement reached on setBetterArchonID. Fifth archon confirmed????");
        rc.resign();
        return 0; // this should NEVER happen
    }
    public static void CheckIfMTS() throws GameActionException {
        // NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE NIGHTMARE
        int i = 0;
        for(MapLocation maploc : rc.senseNearbyLocationsWithLead(3)) {
            i += rc.senseLead(maploc);
        }
        if(i > 50) {
            MTS = true;
        }
    }
    /**
     * Sends initial pulse to bot with a role attached
     * @param role the role to send
     * @param addl additional data to send
     * @throws GameActionException reg exception
     */
    public static void sendInitPulse(int role, int addl) throws GameActionException {
        int pulse = (role << 4) | addl;
        rc.writeSharedArray(Flag.INIT_PULSE_UPC.getValue()+betterArchonID, pulse);
    }
    /**
     * Sends initial pulse to bot with a MapLoc attached and additional 4bit info
     * @param location location to send
     * @param addl additional data to send
     * @throws GameActionException reg exception
     */
    public static void sendInitPulse(MapLocation location, int addl) throws GameActionException {
        int pulse = ((location.x*width+location.y) << 4) | addl;
        rc.writeSharedArray(Flag.INIT_PULSE_UPC.getValue()+betterArchonID, pulse);
    }

    /**
     * We need to shift the init pulse so that the next rounds pulse doesnt overwrite the old one
     * @throws GameActionException reg exception
     */
    public static void shiftInitPulse() throws GameActionException {
        int data = rc.readSharedArray(Flag.INIT_PULSE_UPC.getValue()+betterArchonID);
        rc.writeSharedArray(Flag.INIT_PULSE.getValue()+betterArchonID, data);
    }

    /**
     * Calculates the three possible types of symmetry, also writes them to comms
     * @return The three possible archon locations
     */
    public static MapLocation[] possibleArchonLocations() throws GameActionException {
        MapLocation[] locs = new MapLocation[3];
        MapLocation me = rc.getLocation();
        float originX = (width-1)/2.0f;
        float originY = (height-1)/2.0f;
        float relX = me.x-originX;
        float relY = me.y-originY;

        locs[0] = new MapLocation((int)((relX*-1)+originX), (int)((relY*-1)+originY));
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(betterArchonID*3), ((locs[0].x*width+locs[0].y)<<1) | 1);
        locs[1] = new MapLocation((int)((relX*-1)+originX), me.y);
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(betterArchonID*3)+1, ((locs[1].x*width+locs[1].y)<<1) | 1);
        locs[2] = new MapLocation(me.x, (int)((relY*-1)+originY));
        rc.writeSharedArray(Flag.ENEMY_ARCHON_POS.getValue()+(betterArchonID*3)+2, ((locs[2].x*width+locs[2].y)<<1) | 1);

        return locs;
    }
}

