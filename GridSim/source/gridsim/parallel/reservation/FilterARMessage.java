/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim.parallel.reservation;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_predicate;
import gridsim.parallel.ParallelResource;

/**
 * This filter is used to look for a specific incoming event that matches 
 * a given event tag name and a reservation id. Incoming events with a 
 * matching tag name <b>must</b> contain a data object of type 
 * {@link ReservationMessage} which stores the reservation and consequently 
 * has the given reservation ID.
 * <p>
 * <b>NOTE:</b> This class can only be used for getting a reply or feedback
 * from a {@link ParallelResource} entity regarding to an advance reservation.
 * <br>
 * This class is used by {@link eduni.simjava.Sim_system} to select or 
 * filter an event already present in the entity's deferred queue
 * (incoming buffer), or used to selectively wait for a future event.
 * 
 * @author  Marcos Dias de Assuncao
 * @since   5.0
 * 
 * @see ReservationRequester
 */

public class FilterARMessage extends Sim_predicate {
    private int tag;
    private int reservId;

    /**
     * Finds an incoming events that matches with the given 
     * reservation ID and event tag name.
     * @param reservId  the id of the reservation to which
     * the message is related
     * @param tag  a matching event tag name
     */
    public FilterARMessage(int reservId, int tag) {
        this.tag = tag;
        this.reservId = reservId;
    }

    /**
     * Checks whether an event matches the required constraints or not.<br>
     * <b>NOTE:</b> This method is not used directly by the user. Instead, it is
     * called by {@link eduni.simjava.Sim_system}.
     *
     * @param ev  an incoming event to compare with
     * @return <code>true</code> if an event matches, <code>false</code> otherwise
     * @pre ev != null
     * @post $none
     */
    public boolean match(Sim_event ev) {
        if (ev == null) {
            return false;
        }

        boolean result = false;
        try {
            // find an event with a matching tag first
            if ( tag == ev.get_tag() ) {
                Object obj = ev.get_data();

                // if the event's data contains the correct data
                if (obj instanceof ReservationMessage) {
                	ReservationMessage msg = (ReservationMessage) obj;

                    // if the data contains the correct ID 
                    if (msg.getReservationID() == reservId) {
                        result = true;
                    }
                }
            }
        } catch (Exception e) {
            result = false;
        }

        return result;
    }
} 
