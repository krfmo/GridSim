/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

import java.util.Collection;

import gridsim.GridSim;
import gridsim.parallel.profile.TimeSlot;

/**
 * This class represents a reservation and its properties.
 * 
 * @author Marcos Dias de Assuncao
 *  
 * @since 5.0
 */
public class Reservation implements Cloneable, 
					Comparable<Reservation> {
	
    private double startTime;  	// reservation start time
    private int duration;      // reservation duration time
    private int numPE;         // number of PEs requested for this reservation
    private int resID;         // resource id that accepts this reservation
    private int userID;        // user id that owns this reservation object
    private ReservationStatus status;        // status of this reservation

    // these attributes are filled by a resource, its scheduler or the requester
    private int reservID;      	// reservation ID
    private double submissionTime; 	// the time of submission of this advance reservation
    
	// If a Grid resource cannot make a reservation, it may provide options
	// by informing the reservation requester when resource will be available
    private Collection<TimeSlot> resOptions;
    private static final int NOT_FOUND = -1;   
    private static int lastReservationId_ = 0;
    
    /**
     * Allocates a new <tt>Reservation</tt> object
     * @param userName an entity name that owns this reservation object
     */
    public Reservation(String userName) {
        this(GridSim.getEntityId(userName));
    }

    /**
     * Allocates a new <tt>Reservation</tt> object
     * @param userID an entity ID that owns this reservation object
     */
    public Reservation(int userID) {
        if (userID == -1) {
            new IllegalArgumentException("Invalid user ID or user name.");
        }

        reservID = ++lastReservationId_;
        this.userID = userID;
        startTime = NOT_FOUND;
        duration = NOT_FOUND;
        numPE = NOT_FOUND;
        resID = NOT_FOUND;
        status = ReservationStatus.UNKNOWN;
    }

    /**
     * Sets the time of submission of this reservation
     * @param time the submission time
     * @return <code>true</code> if the time has been set or
     * 			<code>false</code> otherwise.
     */
    public boolean setSubmissionTime(double time) {
    	if(time < 0) {
    		return false;
    	}
    	
    	submissionTime = time;
    	return true;
    }
    
    /**
     * Returns the time of submission of this reservation
     * @return the submission time
     */
	public double getSubmissionTime() {
		return submissionTime;
	}
    
    /**
     * Copy the object. All the values are copied into this object.
     * If the object is <code>null</code>, then copy failed.
     * @param obj  a Reservation object
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public boolean copy(Reservation obj) {
        if (obj != null) {
            startTime = obj.startTime;
            duration = obj.duration;
            numPE = obj.numPE;
            resID = obj.resID;
            status = obj.status;
            reservID = obj.reservID;
            userID = obj.userID;
            submissionTime = obj.submissionTime;
            return true;
        }
        return false;
    }

    /**
     * Sets the start time (in seconds) for this reservation.
     * @param startTime the reservation start time in seconds
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public boolean setStartTime(double startTime) {
        if (startTime < 0) {
            return false;
        }

        this.startTime = startTime;
        return true;
    }

    /**
     * Sets the duration time (unit in seconds) for this reservation.
     * @param duration the reservation duration time. Time unit is in seconds.
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @pre duration > 0
     */
    public boolean setDurationTime(int duration) {
        if (duration <= 0) {
            return false;
        }

        this.duration = duration;
        return true;
    }

    /**
     * Sets the number of PEs (Processing Elements) required by this reservation
     * @param numPE number of PEs required
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @pre numPE > 0
     */
    public boolean setNumPE(int numPE) {
        if (numPE <= 0) {
            return false;
        }

        this.numPE = numPE;
        return true;
    }

    /**
     * Gets this object's owner ID
     * @return a user ID that owns this reservation object
     */
    public int getUserID() {
        return userID;
    }

    /**
     * Gets this object's owner name
     * @return a user name that owns this reservation object
     */
    public String getEntityName() {
        return GridSim.getEntityName(userID);
    }

    /**
     * Gets this object's start time in seconds
     * @return the reservation start time in seconds
     */
    public double getStartTime() {
        return startTime;
    }
    
    /**
     * Gets this object's finish time in seconds
     * @return the reservation finish time in seconds
     */
    public double getFinishTime() {
        return (startTime < 0) ? NOT_FOUND : (startTime + duration);
    }

    /**
     * Gets this object's duration time in seconds
     * @return the reservation duration time in seconds
     */
    public int getDurationTime() {
        return duration;
    }

    /**
     * Gets this object's number of PEs.
     * @return the reservation number of PEs requested
     */
    public int getNumPE() {
        return numPE;
    }

    /**
     * Gets this object's resource ID.
     * @return a resource ID
     */
    public int getResourceID() {
        return resID;
    }
    
    /**
     * Sets the resource ID for sending this reservation object.
     * @param id   a resource ID
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @pre id > 0
     */
    public boolean setResourceID(int id) {
        if (id <= 0) {
            return false;
        }

        resID = id;
        return true;
    }

    /**
     * Sets the status of this reservation.
     * @param status this reservation status
     */
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    /**
     * Gets the status of this reservation
     * @return this reservation current status
     */
    public ReservationStatus getStatus() {
        return status;
    }

    /**
     * Gets this object's reservation ID
     * @return a reservation ID
     */
    public int getID() {
        return reservID;
    }
    
    /**
     * Gets the reservation options given by the Grid resource 
     * @return the reservation options
     */
	public Collection<TimeSlot> getReservationOptions() {
		return resOptions;
	}

	/**
	 * Sets the reservation options given by the Grid resource
	 * @param resOptions the reservation options object
	 */
	public void setReservationOptions(Collection<TimeSlot> resOptions) {
		this.resOptions = resOptions;
	}

    /**
     * Returns a clone of this object
     * @return a cloned reservation object 
     */
    public Reservation clone() {
    	Reservation reservation = new Reservation(userID);
    	reservation.copy(this);
        return reservation;
    }

    /**
     * Compares this reservation with the specified reservation for order. 
     * Returns a negative integer, zero, or a positive integer as this 
     * reservation is less than, equal to, or greater than the specified 
     * reservation. 
     * @param reservation the reservation against which this reservation 
     * has to be compared
     * @return a negative number, zero or positive number if this reservation
     * is smaller, equals or greater respectively than the provided reservation
     */
	public int compareTo(Reservation reservation) {
		int result = 0;
		double objStartTime = reservation.getStartTime();
		if(objStartTime < startTime) {
			result = 1;
		} else if(objStartTime > startTime) {
			result = -1;
		} else {
			int objDuration = reservation.getDurationTime();
			if(objDuration < duration) {
				result = 1;
			} else if(objDuration > duration) {
				result = -1;
			}
		}
		
		return result;
	}
	
    /**
     * Creates a string representation of the reservation for debugging purposes.
     * @return a string representation of this reservation. 
     */
    public String toString() {
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("Reservation={Reservation ID: " + reservID);
    	stringBuilder.append(", User ID: " + userID);
    	stringBuilder.append(", Status: " + status.getDescription());
    	stringBuilder.append(", Sub. Time: " + submissionTime);
		stringBuilder.append(", Start Time: " + startTime);
		stringBuilder.append(", FinishTime: " + (startTime + duration));
		stringBuilder.append(", Duration: " + duration);
		stringBuilder.append(", Num. PEs: " + numPE + "}");
    	return stringBuilder.toString();
    }
} 

