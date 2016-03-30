/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */

package gridsim.datagrid.filter;

import eduni.simjava.Sim_predicate;
import eduni.simjava.Sim_event;


/**
 * Look for a specific incoming event that matches a given event tag name and a
 * logical file name (lfn). Incoming events with a matching tag name
 * <b>must</b> contain a data object of type
 * <tt>Object[]</tt>, where <tt>Object[0]</tt> stores the lfn.
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
 * and a lfn of <tt>Data01</tt> in this example. <br>
 * Therefore, inside the <tt>Entity_B</tt> code:<br> <br>
 *
 * <code>
 *
 * &nbsp;&nbsp; ... // other code <br><br>
 * &nbsp;&nbsp; String lfn = "Data01"; <br>
 * &nbsp;&nbsp; int tag = HIGH_PRIORITY; <br>
 * &nbsp;&nbsp; Sim_event ev = new Sim_event(); <br>
 * &nbsp;&nbsp; FilterDataResult filter = new FilterDataResult(lfn, tag); <br>
 * <br>
 * &nbsp;&nbsp; // get an incoming event that matches a given filter <br>
 * &nbsp;&nbsp; super.sim_get_next(filter, ev); <br>
 * <br>
 * &nbsp;&nbsp; // get the matching event data <br>
 * &nbsp;&nbsp; Object[] result = (Object[]) ev.get_data(); <br>
 * </code>
 * <br><br>
 *
 * <b>NOTE:</b>
 * <ul>
 * <li> both <tt>Entity_A</tt> and <tt>Entity_B</tt> must be an instance of
 *      {@link eduni.simjava.Sim_entity} class.
 * <li> <tt>Entity_A</tt> <b>must</b> send the correct data object to
 *      <tt>Entity_B</tt> for this type of filtering. <br>
 *      The event data object <b>must</b> be of type <tt>Object[]</tt>.
 * <li> if no incoming events match the given condition, then
 *      {@link eduni.simjava.Sim_entity#sim_get_next(Sim_predicate, Sim_event)}
 *      method will wait indefinitely.
 * </ul>
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class FilterDataResult extends Sim_predicate {
    private int tag_;       // a tag name
    private String lfn_;    // logical file name

    /**
     * Creates a new filter to select an incoming event based
     * on the logical file name (lfn) and tag name
     * @param lfn   the logical file name
     * @param tag   a matching event tag name
     */
    public FilterDataResult(String lfn, int tag) {
        tag_ = tag;
        lfn_ = lfn;
    }

    /**
     * Checks whether an event matches the required constraints or not.<br>
     * NOTE: This method is not used directly by the user. Instead, it is
     * called by {@link eduni.simjava.Sim_system}.
     *
     * @param ev    an incoming event to compare with
     * @return <tt>true</tt> if an event matches, <tt>false</tt> otherwise
     */
    public boolean match(Sim_event ev) {

        // checks for errors first
        if (ev == null || lfn_ == null) {
            return false;
        }

        boolean result = false;
        try {
            // find an event with a matching tag first
            if (ev.get_tag() == tag_) {
                Object obj = ev.get_data();     // get the data

                // if the event's data contains the correct data
                if (obj instanceof Object[]) {
                    Object[] array = (Object[]) obj;
                    String name = (String) array[0];

                    // if the data contains the correct ID or value
                    if (lfn_.equals(name)) {
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

