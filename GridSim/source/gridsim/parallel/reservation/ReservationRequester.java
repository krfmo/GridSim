/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;

import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.net.Link;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import gridsim.parallel.profile.TimeSlot;

/**
 * This class represents an entity that makes advance reservation requests.
 * This entity initiates operations such as such as create, modify, cancel 
 * and query.
 * 
 * @author  Marcos Dias de Assuncao
 * @since 5.0
 */
public class ReservationRequester extends GridSim {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);

	// to store successful reservations
    private HashMap<Integer,Reservation> reservations_ = new HashMap<Integer,Reservation>();;    

    /**
     * Allocates a new object. 
     * @param name   this entity name
     * @param baudRate   the bandwidth of this entity
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initialising GridSim package
     *          <li> this entity name is <code>null</code> or empty
     *      </ul>
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    protected ReservationRequester(String name, double baudRate) 
  									throws Exception {
        super(name, baudRate);
    }

    /**
     * Allocates a new object.
     * @param name   this entity name
     * @param link   the link that this GridSim entity will use to communicate
     *               with other GridSim or Network entities.
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initialising GridSim package
     *          <li> this entity name is <code>null</code> or empty
     *      </ul>
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    protected ReservationRequester(String name, Link link) 
   									throws Exception {
        super(name, link);
    }
    /**
     * Gets a reservation object based on the given booking ID
     * @param reservationId   a reservation booking ID
     * @return Reservation object or <code>null</code> if an error occurs
     */
    protected Reservation getReservation(int reservationId) {
    	return reservations_.get(reservationId);
    }

    /**
     * Creates a new reservation and sends the request to a resource.<br>
     * Immediate reservation can be used by this method by specifying 
     * startTime = 0, meaning do not care about start time or use
     * current time as a reservation's start time.
     * @param startTime   reservation start time in seconds
     * @param duration    reservation end time in seconds
     * @param numPE       number of PEs required for this reservation
     * @param resID       a resource ID
     * @return an unique booking id if successful, otherwise an error message
     * @pre startTime >= 0
     * @pre duration > 0
     * @pre numPE > 0
     * @pre resID > 0
     */
    public Reservation createReservation(double startTime, int duration, 
    		int numPE, int resID) {
    	
        // check all the values first
        boolean success = validateValue(startTime, duration, numPE);
        if (!success) {
            return null;
        }
        
        Reservation reservation = null;
        try {
        	
        	// create the reservation itself
        	reservation = new Reservation(super.get_id());
        	reservation.setStartTime(startTime);
        	reservation.setDurationTime(duration);
        	reservation.setResourceID(resID);
        	reservation.setNumPE(numPE);
        	
        	// creates the message to be sent to the grid resource
        	ReservationMessage message = new ReservationMessage(super.get_id(), resID, reservation);
        	message.setMessageType(MessageType.CREATE);
        	
        	// gets the reply from the grid resource
            ReservationMessage reply = sendReservationMessage(message);
            
            // If error code is NO_ERROR, the reservation has been successful.
            ErrorType error = reply.getErrorCode();
            if(error == ErrorType.NO_ERROR) {
            	reservations_.put(reservation.getID(), reservation);
            }
            else if(error == ErrorType.OPERATION_FAILURE_BUT_OPTIONS) {
            	reservation = reply.getReservation();
            }
            else {
            	logger.info("Reservation # " + reservation.getID() + 
            			" has not been accepted by resource # " + 
            			resID + " at time = " + GridSim.clock());
            	reservation = null;
            }
        }
        
        catch (Exception ex) {
        	logger.log(Level.WARNING, "Reservation # " +
        			reservation.getID() + " has not been accepted by "+ 
        			"resource # " + resID + " at time = " + GridSim.clock(), ex);
        	reservation = null;
        }
        
        return reservation;
    }

    /**
     * Cancels a given reservation. All jobs associated with this
     * reservation will automatically be cancelled. 
     * @param reservationId the id of the reservation to be cancelled
     * @return a boolean that denotes success or failure.
     */
    public boolean cancelReservation(int reservationId) {
    	Reservation reservation = getReservation(reservationId);
    	if(reservation == null) {
    		logger.info("Error cancelling reservation, ID is invalid");
        	return false;
    	}

    	int resourceId = reservation.getResourceID();
    	
        try {
	        
	        // creates the message to be sent to the grid resource
	    	ReservationMessage message = new ReservationMessage(super.get_id(), resourceId, reservation);
	    	message.setMessageType(MessageType.CANCEL);
	    	
	        // gets the reply from the grid resource
	    	ReservationMessage reply = sendReservationMessage(message);
	    	
	        // If error code is NO_ERROR, the reservation has been successful. 
	        ErrorType error = reply.getErrorCode();
	        if(error == ErrorType.NO_ERROR) {
	        	reservations_.remove(reservation.getID());
	        	return true;
	        }
	        else {
	        	logger.info("Error cancelling reservation.");
	        }
        }
        catch (Exception ex) {
        	logger.log(Level.WARNING,"Error cancelling reservation.", ex);
        }
    	return false;
    }

    /**
     * Queries to a resource regarding to list of free time during a period of
     * time. This method returns a list that contains the availability 
     * of PEs at simulation times between the start and end time. Each entry
     * contains: 
     * <ul>
     *    <li> time of the entry
     *    <li> list of ranges of PEs available at that particular time
     * </ul>
     *
     * @param resID   a resource ID
     * @param startTime  the simulation start time in which 
     * the requester is interested
     * @param duration  duration time in seconds
     * @return a list of entries that describe the ranges of PEs 
     * available at simulation time between the requested time 
     * @pre resourceID > 0
     * @pre startTime >= 0
     * @pre finishTime > 0
     */
    public Collection<TimeSlot> queryFreeTime(double startTime, 
    		int duration, int resID) {

    	// check all the values first
        boolean success = validateValue(startTime, duration, 1);
        if (!success) {
            return null;
        }
        
        Collection<TimeSlot> resOptions = null;
        try {
        	
        	// create the reservation itself
        	Reservation reservation = new Reservation(super.get_id());
        	reservation.setStartTime(startTime);
        	reservation.setDurationTime(duration);
        	reservation.setResourceID(resID);
        	
        	// creates the message to be sent to the grid resource
        	ReservationMessage message = new ReservationMessage(super.get_id(), resID, reservation);
        	message.setMessageType(MessageType.LIST_FREE_TIME);
        	
            // gets the reply from the grid resource
        	ReservationMessage reply = sendReservationMessage(message);
            
        	// If error code is NO_ERROR, the reservation has been successful.
            ErrorType error = (reply == null) ? 
            		ErrorType.OPERATION_FAILURE : reply.getErrorCode();
            
            if(error == ErrorType.NO_ERROR && reply != null) {
            	resOptions = reply.getReservation().getReservationOptions();
            }
            else {
            	logger.info("Resource # " + resID + " could not inform the " +
            			"availability at time " + GridSim.clock());
            	resOptions = null;
            }
        }
        
        catch (Exception ex) {
        	logger.log(Level.WARNING,"Resource # " + resID +
        			" could not inform the availability at time " + GridSim.clock(), ex);
        	resOptions = null;
        }
        
        return resOptions;
    }

    /**
     * Queries the overall status of a reservation.
     * @param reservationId   this reservation booking ID
     * @return an integer tag that denotes success or failure.
     */
    public ReservationStatus queryReservation(int reservationId) {
    	Reservation reservation = getReservation(reservationId);
    	if(reservation == null) {
    		logger.info("Error querying reservation, ID is invalid");
        	return ReservationStatus.UNKNOWN;
    	}

    	int resId = reservation.getResourceID();
    	
        try {
	        
	        // creates the message to be sent to the grid resource
	    	ReservationMessage message = new ReservationMessage(super.get_id(), resId, reservation);
	    	message.setMessageType(MessageType.STATUS);
	    	
	        // gets the reply from the grid resource
	    	ReservationMessage reply = sendReservationMessage(message);
	    	
	    	// If error code is NO_ERROR, the reservation has been successful. 
	        ErrorType error = reply.getErrorCode();
	        if(error == ErrorType.NO_ERROR) {
	        	return reply.getReservation().getStatus();
	        }
	        else {
	        	logger.info("Error querying status of reservation.");
	        }
        }
        catch (Exception ex) {
        	logger.log(Level.WARNING,"Error querying status of reservation.", ex);
        }
        
    	return ReservationStatus.UNKNOWN;
    }

    /**
     * Commits a reservation indicating that the user is willing to used it.
     * @param reservId a reservation booking ID
     * @return <code>true</code> if commit was successful; <code>false</code> otherwise.
     */
     public boolean commitReservation(int reservId) {
    	 Reservation reservation = getReservation(reservId);
     	if(reservation == null) {
     		logger.info("Error commiting reservation, ID is invalid");
         	return false;
     	}

     	int resId = reservation.getResourceID();
     	
         try {
 	        
 	        // creates the message to be sent to the grid resource
 	    	ReservationMessage message = new ReservationMessage(super.get_id(), resId, reservation);
 	    	message.setMessageType(MessageType.COMMIT);
 	    	
 	        // gets the reply from the grid resource
 	    	ReservationMessage reply = sendReservationMessage(message);
 	    	
 	    	// If error code is NO_ERROR, the reservation has been successful.
 	        ErrorType error = reply.getErrorCode();
 	        if(error == ErrorType.NO_ERROR) {
 	        	return true;
 	        }
 	        else {
 	        	logger.info("Reservation # " + reservId +
 	        		" has NOT been committed successfully.");
 	        }
         }
         catch (Exception ex) {
        	 logger.log(Level.WARNING, "Reservation # " + reservId +
	 	        		" has NOT been committed successfully.", ex);
         }
     	return false;
     }
     
     /**
      * Modifies an existing reservation. Modification must be done before
      * reservation's start time.
      * @param reservationId   reservation booking Id
      * @param startTime   reservation start time in seconds
      * @param duration    reservation end time in seconds
      * @param numPE       number of PEs required for this reservation
      * @return a reservation object containing the new reservation or
      * <code>null</code> if an error happened during the request
      */
     public Reservation modifyReservation(int reservationId, double startTime, 
     		int duration, int numPE) {
     	
     	Reservation currReservation = getReservation(reservationId);
     	if(currReservation == null) {
     		logger.info("Error modifying reservation, ID is invalid");
         	return null;
     	}

     	int resId = currReservation.getResourceID();
     	
     	// check all the values first
         boolean success = validateValue(startTime, duration, numPE);
         if (!success) {
             return null;
         }

         Reservation newReservation = currReservation.clone();
         try {
 	        newReservation.setStartTime(startTime);
 	        newReservation.setDurationTime(duration);
 	        newReservation.setNumPE(numPE);
 	        
 	        // creates the message to be sent to the grid resource
 	    	ReservationMessage message = new ReservationMessage(super.get_id(), resId, newReservation);
 	    	message.setMessageType(MessageType.MODIFY);
 	    	
 	    	// sends the message gets the reply from the grid resource
 	    	ReservationMessage reply = sendReservationMessage(message);
 	    	
 	    	// If error code is NO_ERROR, the reservation has been successful. 
 	        ErrorType error = reply.getErrorCode();
 	        if(error == ErrorType.NO_ERROR) {
 	        	reservations_.remove(newReservation.getID());
 	        	reservations_.put(newReservation.getID(), newReservation);
 	        	return newReservation;
 	        }
 	        else {
 	        	logger.info("Error modifying reservation.");
 	        }
         }
         
         catch (Exception ex) {
        	 logger.log(Level.WARNING,"Error modifying reservation.", ex);
         }
     	
     	return null;
     }

    //------------------------ PRIVATE METHODS ------------------------

    /**
     * Validates or checks whether one or more given parameters 
     * are valid or not and prints a message if they are not
     * @param startTime  a reservation start time
     * @param duration   a reservation duration time
     * @param numPE      number of PE required by a reservation
     * @return <code>true</code> if they are valid; <code>false</code> otherwise.
     */
    private static boolean validateValue(double startTime, int duration, int numPE) {
    	double currentTime = GridSim.clock();
    	boolean valid = true;

        if (startTime < 0.0) {
        	logger.info("Error - start time should be greater or equals to zero.");
        	valid = false;
        }
        else if (duration <= 0.0) {
        	logger.info("Error - duration should be greater than zero.");
        	valid = false;
        }
        else if (Double.compare(startTime, 0.0) != 0 && startTime < currentTime) {
        	logger.info("Error - start time cannot be less than current time.");
        	valid = false;
        }
        else if (numPE < 1) {
        	logger.info("Error - you should reserve at least one PE.");
        	valid = false;
        }

        return valid;
    }
    
    /**
     * Sends a reservation message and waits for a response.
     * @param message the message to be sent
     * @return the response message
     */
    protected ReservationMessage sendReservationMessage(ReservationMessage message) {
        super.send(message.getDestinationID(), GridSimTags.SCHEDULE_NOW, 
        		message.getMessageType().intValue(), message);
        
        Reservation res = message.getReservation();
        
    	// wait for feedback on the request
        FilterARMessage tagObj = 
        	new FilterARMessage(res.getID(), message.getMessageType().intValue());

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tagObj, ev);
        
        return (ReservationMessage)ev.get_data();
    }
} 

