/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

import gridsim.AllocPolicy;

/**
 * {@link ReservationPolicy} is an interface that defines the methods that an
 * allocation policy needs to implement in order to have reservation 
 * functionalities. New scheduling algorithms can be added into a resource 
 * entity by implementing this interface and extending {@link AllocPolicy}.
 * 
 * @author Marcos Dias de Assuncao 
 * @since 5.0
 * 
 * @see ReservationMessage
 */
public interface ReservationPolicy {

    /**
     * A method that handles a new advanced reservation request.
     * @param message the advance reservation message received requesting 
     * the reservation
     * @return <code>true</code> if the reservation was accepted; 
     * <code>false</code> otherwise.
     */
    ReservationMessage createReservation(ReservationMessage message);

    /**
     * A method that handles a cancel reservation request.
     * @param message the advance reservation message received requesting
     * the cancellation 
     * @return <code>true</code> if the reservation was cancelled; 
     * <code>false</code> otherwise.
     */
    boolean cancelReservation(ReservationMessage message);

    /**
     * A method that handles a commit reservation request.
     * @param message the advance reservation message received
     * @return <code>true</code> if the reservation was committed; 
     * <code>false</code> otherwise.
     */
    boolean commitReservation(ReservationMessage message);

    /**
     * A method that handles a request to modify a reservation.
     * @param message the advance reservation message received
     * @return <code>true</code> if the reservation was modified; 
     * <code>false</code> otherwise.
     */
    boolean modifyReservation(ReservationMessage message);
    
    /**
     * A method that handles a query reservation request.
     * @param message the advance reservation message received
     * @return the response message for the request
     */
    ReservationMessage queryReservation(ReservationMessage message);

    /**
     * A method that handles a query free time request.
     * @param message the advance reservation message received
     * @return the response message for the request
     */
    ReservationMessage queryAvailability(ReservationMessage message);
}
