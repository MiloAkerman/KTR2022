package orbitalstriketherobot;

import battlecode.common.GameActionException;

public interface CallbackInterface {
    void onPathfindingEnd(PATH_END_REASON reason) throws GameActionException;
}
