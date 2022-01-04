package movetherobot;

import battlecode.common.*;
import movetherobot.Constants.*;

// v1 is focused on basic movement, etc. (hence the name)
public strictfp class RobotPlayer {
    static RobotController rc;
    static Team myTeam;
    static Team oppTeam;
    static int turnCount = 0;

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        myTeam = rc.getTeam();
        oppTeam = rc.getTeam().opponent();

        while(true) {
            turnCount += 1;
            try { // no blowing up on my watch
                switch(rc.getType()) {
                    case ARCHON:
                        Archon.setup();
                        break;
                    case LABORATORY:
                        Laboratory.setup();
                        break;
                    case WATCHTOWER:
                        Watchtower.setup();
                        break;
                    case MINER:
                        Miner.setup();
                        break;
                    case BUILDER:
                        Builder.setup();
                        break;
                    case SOLDIER:
                        Soldier.setup();
                        break;
                    case SAGE:
                        Sage.setup();
                        break;
                }
            } catch(GameActionException e) {
                System.out.println(rc.getType() + " Game Exception");
                e.printStackTrace();
            } catch(Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
