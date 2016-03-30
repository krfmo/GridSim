/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2005, The University of Melbourne, Australia
 */

package gridsim.filter;

import gridsim.Gridlet;
import eduni.simjava.Sim_predicate;
import eduni.simjava.Sim_event;


/**
 * Look for a specific incoming event that carries a <tt>Gridlet</tt> object.
 * <br><br>
 *
 * This class is used by {@link eduni.simjava.Sim_system}
 * to select or filter an event already present in the entity's deferred queue
 * (incoming buffer), or used to selectively wait for a future event.
 * <br> <br>
 *
 * <b>An example on how to use this class:</b><br>
 * Imagine we have a scenario where <tt>Entity_A</tt> sends one or more events
 * to <tt>Entity_B</tt> with different tag names and data objects.<br>
 * <tt>Entity_B</tt> wants to get an event that contains a specific
 * <tt>Gridlet</tt> object in this example. <br>
 * Therefore, inside the <tt>Entity_B</tt> code:<br> <br>
 *
 * <code>
 *
 * &nbsp;&nbsp; ... // other code <br><br>
 * &nbsp;&nbsp; int gridletID = 1;   // e.g. only look for Gridlet #1<br>
 * &nbsp;&nbsp; Sim_event ev = new Sim_event(); <br>
 * &nbsp;&nbsp; FilterGridlet filter = new FilterGridlet(gridletID); <br>
 * <br>
 * &nbsp;&nbsp; // get an incoming event that matches a given gridlet id <br>
 * &nbsp;&nbsp; super.sim_get_next(filter, ev); <br>
 * <br>
 * &nbsp;&nbsp; // get the matching event data <br>
 * &nbsp;&nbsp; Gridlet gl = (Gridlet) ev.get_data(); <br>
 * </code>
 * <br><br>
 *
 * <b>NOTE:</b>
 * <ul>
 * <li> both <tt>Entity_A</tt> and <tt>Entity_B</tt> must be an instance of
 *      {@link eduni.simjava.Sim_entity} class.
 * <li> <tt>Entity_A</tt> <b>must</b> send the correct data object to
 *      <tt>Entity_B</tt> for this type of filtering. <br>
 *      The event data object <b>must</b> be of type <tt>Gridlet</tt>.
 * <li> if no incoming events match the given condition, then
 *      {@link eduni.simjava.Sim_entity#sim_get_next(Sim_predicate, Sim_event)}
 *      method will wait indefinitely.
 * </ul>
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.2
 * @invariant $none
 */
public class FilterGridlet extends Sim_predicate
{
    private int tag_;           // a tag name
    private int gridletID_;     // a gridlet ID
    private int userID_;        // a user ID that owns this Gridlet
    private int resID_;         // a resource ID that executes this Gridlet

    private int searchType_;    // type of filtering (see below)
    private final int FIND_GRIDLET_ID = 1;
    private final int FIND_GRIDLET_WITH_RES_ID = 2;
    private final int FIND_GRIDLET_WITH_USER_AND_RES_ID = 3;


    /**
     * Finds a Gridlet in the incoming buffer of an entity that matches with
     * the given three constraints: gridlet id, user id and resource id.
     *
     * @param gridletID   a gridlet id
     * @param userID      a user id that owns this Gridlet object
     * @param resID       a resource id that executes this Gridlet object
     * @pre $none
     * @post $none
     */
    public FilterGridlet(int gridletID, int userID, int resID)
    {
        gridletID_ = gridletID;
        userID_ = userID;
        resID_ = resID;
        searchType_ = FIND_GRIDLET_WITH_USER_AND_RES_ID;
        tag_ = -1;
    }

    /**
     * Finds a Gridlet in the incoming buffer of an entity that matches with
     * the given two constraints: gridlet id and resource id.
     *
     * @param gridletID   a gridlet id
     * @param resID       a resource id that executes this Gridlet object
     * @pre $none
     * @post $none
     */
    public FilterGridlet(int gridletID, int resID)
    {
        gridletID_ = gridletID;
        userID_ = -1;
        resID_ = resID;
        searchType_ = FIND_GRIDLET_WITH_RES_ID;
        tag_ = -1;
    }

    /**
     * Finds a Gridlet in the incoming buffer of an entity that matches with
     * a given gridlet id.
     *
     * @param gridletID
     * @pre $none
     * @post $none
     */
    public FilterGridlet(int gridletID)
    {
        gridletID_ = gridletID;
        userID_ = -1;
        resID_ = -1;
        searchType_ = FIND_GRIDLET_ID;
        tag_ = -1;
    }

    /**
     * Sets the event matching tag name
     * @param tag   a matching event tag name (must be 0 or a positive integer)
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise.
     * @pre $none
     * @post $none
     */
    public boolean setTag(int tag)
    {
        if (tag < 0) {
            return false;
        }

        tag_ = tag;
        return true;
    }

    /**
     * Checks whether an event matches the required constraints or not.<br>
     * NOTE: This method is not used directly by the user. Instead, it is
     * called by {@link eduni.simjava.Sim_system}.
     *
     * @param ev    an incoming event to compare with
     * @return <tt>true</tt> if an event matches, <tt>false</tt> otherwise
     * @pre ev != null
     * @post $none
     */
    public boolean match(Sim_event ev)
    {
        if (ev == null) {
            return false;
        }

        boolean result = false;
        try
        {
            if ( tag_ == -1 || tag_ == ev.get_tag() )
            {
                Object obj = ev.get_data();
                if (obj instanceof Gridlet)
                {
                    Gridlet gl = (Gridlet) obj;
                    result = findGridlet(gl);
                }
            }
        }
        catch (Exception e) {
            result = false;
        }

        return result;
    }

    /**
     * Finds a Gridlet in an event that matches a given condition
     * @param gl    a Gridlet object
     * @return <tt>true</tt> if found, <tt>false</tt> otherwise
     * @pre gl != null
     * @post $none
     */
    private boolean findGridlet(Gridlet gl)
    {
        if (gl == null) {
            return false;
        }

        boolean result = false;
        int glID = gl.getGridletID();
        int glResID = -1;

        switch(searchType_)
        {
            // find a Gridlet that matches with a given id
            case FIND_GRIDLET_ID:
                if (glID == gridletID_) {
                    result = true;
                }
                break;

            // find a Gridlet that matches with a given id also
            // user and resource id
            case FIND_GRIDLET_WITH_USER_AND_RES_ID:
                int glUserID = gl.getUserID();
                glResID = gl.getResourceID();
                if (glID == gridletID_ && glUserID == userID_ &&
                    glResID == resID_)
                {
                    result = true;
                }
                break;

            // find a Gridlet that matches with a given id and its resource id
            case FIND_GRIDLET_WITH_RES_ID:
                glResID = gl.getResourceID();
                if (glID == gridletID_ && glResID == resID_) {
                    result = true;
                }
                break;

            default:
                result = false;
                break;
        }

        return result;
    }

} 
