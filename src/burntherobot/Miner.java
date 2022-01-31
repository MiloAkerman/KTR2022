package burntherobot;

import battlecode.common.*;
import java.util.*;

enum MinerRole {
    MINE,
    MINE_LEADER,
    SCOUT
    /*
    SCOUT_N,
    SCOUT_NE,
    SCOUT_E,
    SCOUT_SE,
    SCOUT_S,
    SCOUT_SW,
    SCOUT_W,
    SCOUT_NW
     */
}
class MinerGroupSignal {
    MapLocation loc;
    int members;
    boolean active;

    public String toString() {
        return loc + " with " + members + " members. Active: " + active;
    }
}

public class Miner extends Unit {
    static MinerRole currRole;
    static Direction scoutDirection;
    static boolean active = false;
    static boolean isLeader = false;
    static boolean headingToPossibleLocation = false;
    static MapLocation lastLeaderPos;
    static int lastLeaderHeartbeat = 0;
    static int group = -1;


    public static void setup() throws GameActionException {
        readPulse();
        if(roleIndex > 1) {
            rc.setIndicatorString("SCOUT");
            currRole = MinerRole.SCOUT;
            scoutDirection = Constants.DIRECTIONS[roleIndex - 2];
        } else if (roleIndex == 1) {
            rc.setIndicatorString("LEADER");
            currRole = MinerRole.MINE;
            isLeader = true;
        } else {
            rc.setIndicatorString("PATROL ROLE " + roleIndex);
            currRole = MinerRole.values()[roleIndex];
        }
    }

    public static void run() throws GameActionException {
        // we have a caravan
        if(currRole == MinerRole.MINE) {
            if(group != -1) {
                if(isLeader) {
                    rc.setIndicatorString("Dest " + destination + " Act " + (active?1:0) + " gs " + (headingToPossibleLocation?1:0));
                    // if leader, and not going somewhere or mining, wander

                    if(hasReachedDestination && isActionAreaUseful()) {
                        // we're here, and there's stuff!
                        active = true;
                        setLeaderSignalActive(true);
                        headingToPossibleLocation = false;
                    } else if (hasReachedDestination) {
                        // no stuff. let's keep moving.
                        headingToPossibleLocation = false;
                        minerWander();
                    }


                    // even while wandering we check our surroundings
                    Pair<MapLocation, Integer> possibleLoc = searchPbCluster();
                    if(!active && !headingToPossibleLocation) {
                        if(possibleLoc.getValue() > 0) {
                            // found a good spot to mine! let's take the caravan there
                            if(rc.senseRobotAtLocation(possibleLoc.getKey()) == null) {
                                destination = possibleLoc.getKey();
                                headingToPossibleLocation = true;
                            }
                        }
                    } else if(active & !headingToPossibleLocation) {
                        if(!isActionAreaUseful()) {
                            // stop mining! there's no more good spots
                            active = false;
                            setLeaderSignalActive(false);
                        }
                    }

                    if(destination == null && !active) {
                        minerWander();
                    }

                    updateLeaderSignal();
                } else {
                    // if follower, find best way to get to leader, then go there
                    Pair<MapLocation, Direction> dirToLeader = getLeaderFollowInfo();
                    djikstraBiasedStep(dirToLeader.getValue());

                    // make them anxious to save us bytecode
                    if(lastLeaderPos != null) {
                        if(lastLeaderPos.equals(dirToLeader.getKey()) && !active) {
                            if(++lastLeaderHeartbeat >= Constants.MAX_LEADER_SILENCE) {
                                // hey man, we haven't heard from you in a while, are you alive?
                                leaveGroup();
                            }
                            if(readLeaderSignal().active) {
                                active = true;
                            }
                        } else if(!lastLeaderPos.equals(dirToLeader.getKey()) && active) {
                            if(!readLeaderSignal().active) {
                                active = false;
                            }
                        }
                        if (!lastLeaderPos.equals(dirToLeader.getKey())) {
                            lastLeaderHeartbeat = 0;
                        }
                    }

                    rc.setIndicatorString("FOLLOWER IN GROUP. LD @ " + dirToLeader.getKey());
                    lastLeaderPos = dirToLeader.getKey();
                }
                // you mine, I mine, everyone mines
                if(active) mine();

            } else {
                // INDIVIDUAL CODE
                // mostly similar to leader code, with a bit more independence and a bit less responsibility

                // lonely miners work bad, so look for group
                if(isLeader) {
                    rc.setIndicatorString("SOLO LEADER");
                    int c = tryCreateGroup(); // i was thinking of the letter C
                    if(c != 0) group = c;
                } else {
                    rc.setIndicatorString("SOLO FOLLOWER");
                    int possibleGroupID = lookForGroup();
                    if(possibleGroupID != 0) {
                        // we'll join next frame, no worries
                        joinGroup(possibleGroupID);
                    }
                }
                if(!active) moveAwayFromOthers(); // we're SOLO!!!!!

                if (hasReachedDestination && isActionAreaUseful()) {
                    // we're here, and there's stuff!
                    active = true;
                    headingToPossibleLocation = false;
                } else if(hasReachedDestination) {
                    // no stuff. let's keep moving.
                    headingToPossibleLocation = false;
                    minerWander();
                }

                // even while wandering we check our surroundings
                Pair<MapLocation, Integer> possibleLoc = searchPbCluster();
                if (!active && !headingToPossibleLocation) {
                    if (possibleLoc.getValue() > 0) {
                        // found a good spot to mine! let's go there
                        destination = possibleLoc.getKey();
                        headingToPossibleLocation = true;
                    }
                } else if (active && !headingToPossibleLocation) {
                    if (!isActionAreaUseful()) {
                        // stop mining! there's no more good spots
                        active = false;
                    } else {
                        mine();
                    }
                }

                // if not going somewhere or mining, wander
                if (destination == null && !active) {
                    minerWander();
                }
            }
        } else if (currRole == MinerRole.SCOUT) {
            djikstraBiasedStep(scoutDirection);
        }

        movement();
    }

    public static int lookForGroup() throws GameActionException {
        int offset = 0;
        for(int i = 0; i < rc.getArchonCount(); i++) {
            int headerData = rc.readSharedArray((Constants.Flag.MINER_GROUPS.getValue())+(Constants.MAX_GROUPS_PER_ARCHON*i));// this is our listing!
            if(headerData == homeID) {
                for(int j = 1; j <= Constants.MAX_GROUPS_PER_ARCHON; j++) {
                    int index = (Constants.Flag.MINER_GROUPS.getValue())+(Constants.MAX_GROUPS_PER_ARCHON*i)+j;
                    int arrayObj = rc.readSharedArray(index);
                    if(arrayObj==0) continue;
                    // i know its long, and i know it's ugly. Checks if group isn't full and  leader is within reasonable distance
                    System.out.println(arrayObj + " @ " + index);
                    System.out.println((((arrayObj>>1)&0b111) < Constants.MAX_MEMBERS_PER_GROUP) + " " + new MapLocation((arrayObj>>4)/width, (arrayObj>>4)%width) + " | " + rc.getLocation());
                    if(((arrayObj>>1)&0b111) < Constants.MAX_MEMBERS_PER_GROUP && new MapLocation((arrayObj>>4)/width, (arrayObj>>4)%width).distanceSquaredTo(rc.getLocation()) < Constants.MAX_DISTSQ_JOINGROUP) {
                        System.out.println("Joining group " + index);
                        return index;
                    }
                }
                return 0;
            }
            offset += Constants.MAX_GROUPS_PER_ARCHON+1;
        }
        return 0;
    }
    public static int tryCreateGroup() throws GameActionException {
        int offset = 0;
        for(int i = 0; i < rc.getArchonCount(); i++) {
            int headerData = rc.readSharedArray((Constants.Flag.MINER_GROUPS.getValue())+(Constants.MAX_GROUPS_PER_ARCHON*i));
            System.out.println(((Constants.Flag.MINER_GROUPS.getValue())+(Constants.MAX_GROUPS_PER_ARCHON*i)) + " and " + homeID);

            // this is our listing!
            if(headerData == homeID) {
                for(int j = 1; j <= Constants.MAX_GROUPS_PER_ARCHON; j++) {
                    int index = (Constants.Flag.MINER_GROUPS.getValue())+(Constants.MAX_GROUPS_PER_ARCHON*i)+j;
                    int arrayObj = rc.readSharedArray(index);
                    System.out.println(index + " reads " + arrayObj);
                    // no group here. let's make one
                    if(arrayObj == 0) {
                        MapLocation me = rc.getLocation();
                        int formattedLoc = (me.x)*width+(me.y);

                        int jobPosting = (formattedLoc << 4) | (1 << 4); // inactive bit simplified
                        rc.writeSharedArray(index, jobPosting);
                        System.out.println("Created group " + index);
                        return index;
                    }
                }
                return 0;
            }
            offset += Constants.MAX_GROUPS_PER_ARCHON+1;
        }
        return 0;
    }
    public static void leaveGroup() throws GameActionException {
        MinerGroupSignal previousSig = readLeaderSignal();
        int formattedLoc = (previousSig.loc.x)*width+(previousSig.loc.y);

        if(--previousSig.members <= 0) {
            rc.writeSharedArray(group, 0); // disband :(
        } else {
            int newSig = (formattedLoc<<4) | (previousSig.members<<1) | (previousSig.active?1:0);
            rc.writeSharedArray(group, newSig);
        }
    }
    public static void joinGroup(int gid) throws GameActionException {
        MinerGroupSignal previousSig = readSignal(gid);
        int formattedLoc = (previousSig.loc.x)*width+(previousSig.loc.y);

        int newSig = (formattedLoc<<4) | ((++previousSig.members)<<1) | (active?1:0); // lookForGroup already made sure we're not over the player limit
        rc.writeSharedArray(gid, newSig);
        group = gid;
    }
    public static Pair<MapLocation, Direction> getLeaderFollowInfo() throws GameActionException {
        int rawInt = rc.readSharedArray(group);
        MapLocation leaderLoc = new MapLocation(((rawInt>>4)/width), ((rawInt>>4)%width));
        return new Pair<>(leaderLoc, rc.getLocation().directionTo(leaderLoc));
    }
    public static void setLeaderSignalActive(boolean active) throws GameActionException {
        MinerGroupSignal previousSig = readLeaderSignal();
        MapLocation loc = rc.getLocation();
        int formattedLoc = (loc.x)*width+(loc.y);

        int newSig = (formattedLoc<<4) | (previousSig.members<<1) | (active?1:0);
        rc.writeSharedArray(group, newSig);
        if(active) {
            rc.setIndicatorDot(loc, 252, 123, 3);
        } else {
            rc.setIndicatorDot(loc, 3, 252, 3);
        }
    }
    public static void updateLeaderSignal() throws GameActionException {
        MinerGroupSignal previousSig = readLeaderSignal();
        MapLocation loc = rc.getLocation();
        if(previousSig.active) {
            rc.setIndicatorDot(loc, 252, 123, 3);
        } else {
            rc.setIndicatorDot(loc, 3, 252, 3);
        }
        int formattedLoc = (loc.x)*width+(loc.y);

        int newSig = (formattedLoc<<4) | (previousSig.members<<1) | (previousSig.active?1:0);
        rc.writeSharedArray(group, newSig);
    }
    public static MinerGroupSignal readLeaderSignal() throws GameActionException {
        MinerGroupSignal ms = new MinerGroupSignal();
        int signal = rc.readSharedArray(group);
        ms.active = (signal & 0b1) == 1;
        ms.members = ((signal>>1) & 0b111);
        ms.loc = new MapLocation((signal>>4)/width, (signal>>4)%width);

        return ms;
    }
    public static MinerGroupSignal readSignal(int gid) throws GameActionException {
        MinerGroupSignal ms = new MinerGroupSignal();
        int signal = rc.readSharedArray(gid);
        ms.active = (signal & 0b1) == 1;
        ms.members = ((signal>>1) & 0b111);
        ms.loc = new MapLocation((signal>>4)/width, (signal>>4)%width);

        return ms;
    }
    public static void minerWander() {
        int x = rng.nextInt(60);
        int y = rng.nextInt(60);
        destination = new MapLocation(x, y);
        //System.out.println(currRole + " WANDERING TO " + destination);
    }
    public static void moveAwayFromOthers() throws GameActionException {
        for(RobotInfo robot : rc.senseNearbyRobots(2)) {
            Direction dir = rc.getLocation().directionTo(robot.location).opposite();
            if(rc.canMove(dir)) {
                rc.move(dir);
                minerWander();
                return;
            }
        }
    }

    /**
     * /*
     * @returns: MapLocation with the highest cluster of lead
     * Probably insane bytecode cost so if anything just replace with leadDeposits[0] lol
     */
    public static Pair<MapLocation, Integer> searchPbCluster() throws GameActionException {
        MapLocation[] leadDeposits = rc.senseNearbyLocationsWithLead(20); // vision radius i believe
        HashMap<MapLocation, Integer> clusters = new HashMap<>();
        for(MapLocation pbLoc : leadDeposits) {
            if(rc.senseLead(pbLoc) == 1) continue;
            int clusterSize = 1;
            for(int i = 0; i < Constants.DIRECTIONS.length; i++) {
                if(rc.canSenseLocation(pbLoc.add(Constants.DIRECTIONS[i])) && rc.senseLead(pbLoc.add(Constants.DIRECTIONS[i])) > 1) {
                    clusterSize += 1;
                }
            }
            clusters.put(pbLoc, clusterSize);
        }
        Object[] clusterArray = clusters.entrySet().toArray();
        Arrays.sort(clusterArray, (Comparator<Object>) (o1, o2) -> ((HashMap.Entry<MapLocation, Integer>) o2).getValue()
                .compareTo(((HashMap.Entry<MapLocation, Integer>) o1).getValue()));
        if(clusterArray.length == 0) {
            return new Pair<>(new MapLocation(0, 0), 0);
        } else {
            HashMap.Entry<MapLocation, Integer> obj = (HashMap.Entry<MapLocation, Integer>)clusterArray[0];
            return new Pair<>(obj.getKey(), obj.getValue());
        }
    }
    public static boolean isActionAreaUseful() throws GameActionException {
        MapLocation me = rc.getLocation();
        for(int dx = -1; dx <= 1; dx++) {
            for(int dy = -1; dy <= 1; dy++) {
                MapLocation maploc = new MapLocation(me.x + dx, me.y + dy);
                if(rc.onTheMap(maploc) && rc.senseLead(maploc) > 30 && rc.senseNearbyRobots(20).length < 7) return true;
            }
        }
        return false;
    }
    public static void mine() throws GameActionException {
        MapLocation[] visibleLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);
        for (MapLocation loc : visibleLocs) {
            int pb = rc.senseLead(loc);
            if(rc.canMineLead(loc)) {
                if(pb>1) {
                    rc.mineLead(loc);
                    rc.setIndicatorLine(rc.getLocation(), loc, 200, 200, 0);
                }
            }
        }
    }
}
