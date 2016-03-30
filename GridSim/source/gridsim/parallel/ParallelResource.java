/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel;

import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;

import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.ResourceCharacteristics;
import gridsim.net.Link;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import gridsim.parallel.reservation.ErrorType;
import gridsim.parallel.reservation.MessageType;
import gridsim.parallel.reservation.Reservation;
import gridsim.parallel.reservation.ReservationMessage;
import gridsim.parallel.reservation.ReservationPolicy;
import gridsim.parallel.util.WorkloadLublin99;

/**
 * {@link ParallelResource} extends the {@link GridResource} class for
 * gaining communication and concurrent entity capabilities. An instance of 
 * this class simulates a resource with properties defined in an object of
 * {@link ResourceCharacteristics} class to maintain compatible with GridSim.
 * <p>
 * To create a parallel resource consists you need a list of machines 
 * with PEs (Processing Element) with a suitable MIPS (Million Instructions 
 * Per Second) or SPEC (Standard Performance Evaluation Corporation) rating;
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines comprising this resource must be homogeneous.
 * 		<li> A resource local load is not considered, a resource calendar will
 * 			 not be available for the allocation policy.
 * 		<li> If you would like to simulate the local load, you have to model 
 * 			 the local load as jobs submitted by a user. It is more precise 
 * 			 and faster. To do so, please check {@link WorkloadLublin99}.
 *  </ul>
 *
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see gridsim.GridResource
 * @see gridsim.AllocPolicy
 */
public class ParallelResource extends GridResource {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
    /**
     * Allocates a new object. When making a different type of
     * resource object, use this constructor and then override
     * {@link #processOtherEvent(Sim_event)}.
     * @param name       the name to be associated with this entity
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param policy     a scheduling policy for this resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initialising GridSim package
     *          <li> this entity name is <code>null</code> or empty
     *          <li> this entity has <code>zero</code> number of PEs (Processing
     *              Elements).
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.AllocPolicy
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public ParallelResource(String name, double baud_rate,
                ResourceCharacteristics resource, 
                AllocPolicy policy) throws Exception {
        super(name, baud_rate, new ResourceDynamics(resource), null, policy);
    }

    /**
     * Allocates a new object. When making a different type of
     * resource object, use this constructor and then overrides
     * {@link #processOtherEvent(Sim_event)}.
     * @param name       the name to be associated with this entity
     * @param link       the link that will be used to connect this
     *                   GridResource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initialising GridSim package
     *          <li> this entity name is <code>null</code> or empty
     *          <li> this entity has <code>zero</code> number of PEs (Processing
     *              Elements).
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.AllocPolicy
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public ParallelResource(String name, Link link,
            ResourceCharacteristics resource, 
            AllocPolicy policy) throws Exception {
        super(name,link, new ResourceDynamics(resource), null, policy);
    }
    
    /**
     * Processes other events or services related to reservations.
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     * @see MessageType
     */
    protected void processOtherEvent(Sim_event ev) {
    	int tag = ev.get_tag();
    	
    	if(tag == MessageType.CREATE.intValue() ||
    		tag == MessageType.COMMIT.intValue() ||
    		tag == MessageType.CANCEL.intValue() ||
    		tag == MessageType.STATUS.intValue() ||
    		tag == MessageType.MODIFY.intValue() ||
    		tag == MessageType.LIST_FREE_TIME.intValue()) {
    		handleReservationEvent((ReservationMessage)ev.get_data());	
    	} else {
    		super.policy_.processOtherEvent(ev);	
    	}
    }

    /*
     * Checks whether this resource can support reservations or not
     * @return <code>true</code> if it can support reservations
     */
    private boolean supportReservation() {
    	return super.policy_ instanceof ReservationPolicy;
    }

    /*
     * Handles a reservation request message
     */
    private void handleReservationEvent(ReservationMessage message) {
        ReservationMessage response = null;
        
        if (!supportReservation()) {
        	logger.warning(super.get_name() + " does not support reservations.");
            response = message.createResponse();
            response.setErrorCode(ErrorType.NO_AR_SUPPORT);
            sendReservationMessage(response);
            return;
        }

        MessageType msgType = message.getMessageType();
        boolean handled = false;

        try {
            ReservationPolicy arPolicy_ = (ReservationPolicy)super.policy_;
            
            if(msgType == MessageType.CREATE) {
            	// sets the submission time for the reservation
        		Reservation reservation = message.getReservation();
        		if(reservation != null) {
        			reservation.setSubmissionTime(GridSim.clock());
        		}
        		response = arPolicy_.createReservation(message);
        		handled = true;
            }
            else if (msgType == MessageType.COMMIT) {
            	handled = arPolicy_.commitReservation(message);
           		response = message.createResponse();
            }
            else if (msgType == MessageType.CANCEL) {
            	handled = arPolicy_.cancelReservation(message);
           		response = message.createResponse();
            }
            else if (msgType == MessageType.STATUS) {
            	response = arPolicy_.queryReservation(message);
            	handled = true;
            }
            else if (msgType == MessageType.MODIFY) {
            	handled = arPolicy_.modifyReservation(message);
           		response = message.createResponse();
            }
            else if (msgType == MessageType.LIST_FREE_TIME) {
            	response = arPolicy_.queryAvailability(message);
            	handled = true;
            }
            
        } catch (Exception e) {
        	logger.log(Level.WARNING, "Error handling reservation message.", e);
            handled = false;
        }
        
    	// if there is an exception or other errors, then send back an error msg
        if (!handled) {
        	logger.warning(super.get_name() + 
        			" has failed to handle request: " + msgType.getDescription());
            response = message.createResponse();
            response.setErrorCode(ErrorType.OPERATION_FAILURE);
        }
        
        sendReservationMessage(response);
    }
    
    /*
     * Sends a reservation message.
     * @param message the message to be sent
     */
    private void sendReservationMessage(ReservationMessage message) {
        super.send(message.getDestinationID(), GridSimTags.SCHEDULE_NOW, 
        		message.getMessageType().intValue(), message);
    }
}
