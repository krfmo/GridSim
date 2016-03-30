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
 * Look for a specific incoming event that matches a given event tag name and a
 * transaction id. Incoming events with a matching tag name <b>must</b>
 * contain a data object of type
 * <tt>int[]</tt>, where <tt>int[0]</tt> stores the transaction id.
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
 * <tt>Entity_B</tt> wants to get an event with a <tt>HIGH_PRIORITY</tt> tag
 * and a transaction id of <tt>10</tt> in this example. <br>
 * Therefore, inside the <tt>Entity_B</tt> code:<br> <br>
 *
 * <code>
 *
 * &nbsp;&nbsp; ... // other code <br><br>
 * &nbsp;&nbsp; int transactionID = 10; <br>
 * &nbsp;&nbsp; int tag = HIGH_PRIORITY; <br>
 * &nbsp;&nbsp; Sim_event ev = new Sim_event(); <br>
 * &nbsp;&nbsp; FilterResult filter = new FilterResult(transactionID, tag); <br>
 * <br>
 * &nbsp;&nbsp; // get an incoming event that matches a given transaction ID and
 * a tag name <br>
 * &nbsp;&nbsp; super.sim_get_next(filter, ev); <br>
 * <br>
 * &nbsp;&nbsp; // get the matching event data <br>
 * &nbsp;&nbsp; int[] data = (int[]) ev.get_data();
 *              // data[0] contains transaction ID <br>
 * </code>
 * <br><br>
 *
 * <b>NOTE:</b>
 * <ul>
 * <li> both <tt>Entity_A</tt> and <tt>Entity_B</tt> must be an instance of
 *      {@link eduni.simjava.Sim_entity} class.
 * <li> <tt>Entity_A</tt> <b>must</b> send the correct data object to
 *      <tt>Entity_B</tt> for events with a matching tag name. <br>
 *      The event data object with a matching tag name
 *      <b>must</b> be of type <tt>int[]</tt>, where
 *      <tt>int[0]</tt> contains the transaction id.
 * <li> if no incoming events match the given condition, then
 *      {@link eduni.simjava.Sim_entity#sim_get_next(Sim_predicate, Sim_event)}
 *      method will wait indefinitely.
 * </ul>
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.2
 * @invariant $none
 */
public class FilterResult extends Sim_predicate
{
    private int tag_;       // event tag to be matched
    private int eventID_;   // event or transaction ID


    /**
     * Finds an incoming events that matches with the given transaction ID and
     * event tag name
     * @param transactionID   a unique transaction ID to differentiate
     *                        itself among other events with the same tag name
     * @param tag   a matching event tag name
     * @pre $none
     * @post $none
     */
    public FilterResult(int transactionID, int tag)
    {
        tag_ = tag;
        eventID_ = transactionID;
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
                if (obj instanceof int[])
                {
                    int[] array = (int[]) obj;

                    // if the data contains the correct ID or value
                    if (array[0] == eventID_) {
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

} 
