/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_predicate;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;


/**
 * This class creates a new Sim_predicate to evaluate whether an event is
 * coming through a specific port.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class Sim_from_port extends Sim_predicate
{
    private Sim_port port_;

    /**
     * Finds an incoming event that passes through this port
     * @param port an object of Sim_port
     * @see eduni.simjava.Sim_port
     * @pre port != null
     * @post $none
     */
    public Sim_from_port(Sim_port port) {
        this.port_ = port;
    }

    /**
     * Checks whether an event comes through a given port or not.<br>
     * NOTE: This method is not used directly by the user. Instead, it is
     * called by {@link eduni.simjava.Sim_system}.
     *
     * @param event    an incoming event to compare with
     * @return <tt>true</tt> if an event matches, <tt>false</tt> otherwise
     * @pre event != null
     * @post $none
     */
    public boolean match(Sim_event event)
    {
        if (port_ == null) {
            return false;
        }

        return event.from_port(port_);
    }

} 

