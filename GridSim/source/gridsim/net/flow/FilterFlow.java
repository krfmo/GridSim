/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg
 */

package gridsim.net.flow;

import eduni.simjava.Sim_predicate;
import eduni.simjava.Sim_event;


/**
 * Look for a specific incoming event that matches a given event tag name and a
 * flow id. Incoming events with a matching tag name <b>must</b>
 * contain a data object of type
 * <tt>Integer</tt>, where <tt>Integer</tt> stores the flow id.
 * <br><br>
 *
 * This class is used by {@link eduni.simjava.Sim_system}
 * to select or filter an event already present in the entity's deferred queue
 * (incoming buffer), or used to selectively wait for a future event.
 * <br> <br>
 *
 * @author       James Broberg
 * @since        GridSim Toolkit 4.2
 * @invariant $none
 */
public class FilterFlow extends Sim_predicate
{
    private int tag_;       // event tag to be matched
    private int flowID_;    // event or flow ID


    /**
     * Finds an incoming events that matches with the given flow ID and
     * event tag name
     * @param flowID   a unique flow ID to differentiate
     *                        itself among other events with the same tag name
     * @param tag   a matching event tag name
     * @pre $none
     * @post $none
     */
    public FilterFlow(int flowID, int tag)
    {
        tag_ = tag;
        flowID_ = flowID;
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
            // find an event with a matching tag first
            if ( tag_ == ev.get_tag() )
            {
                Object obj = ev.get_data();

                // if the event's data contains the correct data
                if (obj instanceof Integer)
                {
                    int id = (Integer)obj;

                    // if the data contains the correct ID or value
                    if (id == flowID_) {
                        result = true;
                    }
                }
            }
        }
        catch (Exception e) {
            result = false;
        }

        return result;
    }

} // end class
