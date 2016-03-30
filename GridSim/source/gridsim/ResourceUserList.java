/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.LinkedList;

/**
 * GridSim ResourceUserList maintains a linked-list of Grid Resource users
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class ResourceUserList extends LinkedList
{

	/**
     * Adds one Grid Resource user into the list.
     * @param userID Grid Resource user ID
     * @return <b>true</b> if it is a new user, otherwise return <b>false</b>
     * @pre userID >= 0
     * @post $result == true || $result == false
     */
    public boolean add(int userID)
    {
        Integer user = new Integer(userID);

        // check whether the user has already stored into the list
        if (this.contains(user)) {
            return false;
        }

        this.add(user);
        return true;
    }

    /**
     * Removes a particular user from the list
     * @param userID Grid Resource user ID
     * @return <b>true</b> if the list contained the specified element,
     *         otherwise return <b>false</b>
     * @deprecated As of GridSim 2.1, replaced by {@link #removeUser(int)}
     * @pre userID >= 0
     * @post $result == true || $result == false
     */
    public boolean myRemove(int userID) {
        return this.removeUser(userID);
    }

    /**
     * Removes a particular user from the list
     * @param userID Grid Resource user ID
     * @return <b>true</b> if the list contained the specified element,
     *         otherwise return <b>false</b>
     * @pre userID >= 0
     * @post $result == true || $result == false
     */
    public boolean removeUser(int userID) {
        return super.remove( new Integer(userID) );
    }

} 

