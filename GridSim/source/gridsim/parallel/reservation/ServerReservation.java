/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

import java.text.DecimalFormat;

import gridsim.GridSim;
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.ScheduleItem;

/**
 * This class represents a reservation on the resource side (Server Side). 
 * This class keeps track of the time for all activities in the Grid resource 
 * for a specific reservation. Among other things, it contains a Reservation 
 * object along with its arrival time and the ranges of PEs 
 * (Processing Element) allocated to it.
 *
 * @author 	Marcos Dias de Assuncao
 * @since 5.0
 */

public class ServerReservation implements ScheduleItem {
	private Reservation reservation; 	// the original reservation object
    private PERangeList peRanges;		// list of PE ranges used
    private PERangeList availPERanges; 	// remaining ranges available for jobs
    private double expiryTime;			
    
    // partition or queue to which this reservation was scheduled
    private int partition;
    
    // the priority of this reservation (assigned by the scheduler)
    private int priority;
    
	private static final DecimalFormat decFormater = new DecimalFormat("#,##0.00");  
	
	/**
	 * Creates a new <code>ServerReservation</code> object.
	 * @param reservation the reservation that this object
	 * represents. That is, the reservation sent by a requester
	 */
	public ServerReservation(Reservation reservation) {
		this.reservation = reservation;
	    peRanges = null;
	    availPERanges = null;
	    expiryTime = -1;
	}
	
	/**
	 * Returns the reservation sent by the requester, which
	 * led to the creation of this object.
	 * @return the reservation sent by the requester 
	 */
	public Reservation getReservation() {
		return reservation;
	}
	
    /** 
     * Used to check if this schedule item is an advance reservation
     * @return <code>true</code> as it is an advance reservation
     */
	public boolean isAdvanceReservation() {
		return true;
	}
	
	/**
     * Sets the ranges of PEs used by this reservation
     * @param rangeList the range of PEs
     * @return <code>true</code> if the range has been set or
     * <code>false</code> otherwise.
     * @pre rangeList != null
     */
    public boolean setPERangeList(PERangeList rangeList) {
    	if(peRanges == null) {
    		peRanges = rangeList;
    		availPERanges = peRanges.clone();
    	} else {
    		// check whether ranges have already been allocated
    		// to jobs or not
    		if(!peRanges.equals(availPERanges)) {
    			return false;
    		} else {
        		peRanges = rangeList;
        		availPERanges = peRanges.clone();
    		}
    	}
    	return true;
    }
    
    /**
     * Gets the list of ranges of PEs used by this reservation
     * @return a list containing the ranges
     */
    public PERangeList getPERangeList() {
        return peRanges;
    }
    
    /**
     * Returns the number of PEs still available that have not been
     * allocated to jobs
     * @return the number of PEs available
     */
    public int getNumRemainingPE() {
    	return (availPERanges == null) ? 0 : availPERanges.getNumPE();
    }
    
    /**
     * Selects a range of PEs to be used by a job 
     * that arrived to use this advance reservation
     * @param reqPE the number of PEs required.
     * @return the range to be allocated or <code>null</code> if no
     * range suitable is found.
     */
    public PERangeList selectPERangeList(int reqPE) {
    	PERangeList selected = availPERanges.selectPEs(reqPE);
    	if(selected != null) {
    		// removes the selected range from the list of available ranges
    		availPERanges.remove(selected);
    	}
    	return selected;
    }
    
    /**
     * Gets the id of the partition or queue to which this
     * reservation was scheduled
     * @return the partition id or <code>-1</code> if not found
     */
    public int getPartitionID() {
		return partition;
	}

    /**
     * Sets the id of the partition or queue to which this
     * reservation was scheduled
     * @param partition the partition id
     * @return <code>true</code> if set correctly; <code>false</code> otherwise.
     */
	public boolean setPartitionID(int partition) {
		if(partition < 0) {
			return false;
		}
		
		this.partition = partition;
		return true;
	}
	
    /**
     * Gets the priority of this reservation assigned by the scheduler
     * @return the priority or <code>-1</code> if not found
     */
    public int getPriority() {
		return priority;
	}

    /**
     * Sets the priority of this reservation assigned by the scheduler
     * @param priority the priority
     * @return <code>true</code> if set correctly; <code>false</code> otherwise.
     */
	public boolean setPriority(int priority) {
		if(priority < 0) {
			return false;
		}
		
		this.priority = priority;
		return true;
	}
    
    /**
     * Sets the time of submission of this reservation
     * @param time the submission time
     * @return <code>true</code> if the time has been set or
     * <code>false</code> otherwise.
     */
    public boolean setSubmissionTime(double time) {
    	if(reservation == null) {
    		return false;
    	}
    	
    	return reservation.setSubmissionTime(time);
    }
    
    /**
     * Returns the time of submission of this reservation
     * @return the submission time
     */
	public double getSubmissionTime() {
		return reservation.getSubmissionTime();
	}
    
    /**
     * Sets the start time (in seconds) for this reservation.
     * @param startTime   the reservation start time in seconds
     * @return <code>true</code> if successful; <code>false</code> otherwise
     */
    public boolean setStartTime(double startTime) {
    	if(reservation == null) {
    		return false;
    	}
    	
    	return reservation.setStartTime(startTime);
    }
    
    /**
     * Sets the duration time (unit in seconds) for this reservation.
     * @param duration the reservation duration time. Time unit is in seconds.
     * @return <code>true</code> if successful; <code>false</code> otherwise
     * @pre duration > 0
     */
    public boolean setDurationTime(int duration) {
    	if(reservation == null) {
    		return false;
    	}
    	
    	return reservation.setDurationTime(duration);
    }
    
    /**
     * Sets a reservation expiry time.
     * This method is mainly used by {@link Reservation} object.
     * @param expiryTime   the reservation expiry time
     * @pre expiryTime > 0
     */
    public void setExpiryTime(double expiryTime) {
        if (expiryTime <= 0) {
            return;
        }
        this.expiryTime = expiryTime;
    }
    
    /**
     * Sets the status of this reservation.
     * @param status this reservation's status
     */
    public void setStatus(ReservationStatus status) {
    	if(reservation != null) {
    		reservation.setStatus(status);
    	}
    }

    /**
     * Gets this object's owner ID
     * @return a user ID that owns this reservation object
     */
    public int getSenderID() {
    	return reservation == null ? -1 : reservation.getUserID();
    }
    
    /**
     * Gets this object's start time in seconds
     * @return the reservation start time in seconds
     */
    public double getStartTime() {
    	return reservation == null ? -1 : reservation.getStartTime();
    }
    
    /**
     * Gets this object's finish time in seconds
     * @return the reservation finish time in seconds
     */
    public double getActualFinishTime() {
    	return reservation == null ? -1 : reservation.getFinishTime();
    }
    
    /**
     * Gets the reservation's expected finish time. That is, this end
     * time is based on the estimate provided by the user.<br>
     * <b>NOTE:</b> for advance reservations, the actual and expected finish
     * times are the same.
     * @return finish time of the advance reservation.
     */
    public double getExpectedFinishTime() {
        return getActualFinishTime();
    }
    
    /**
     * Gets this object's duration time in seconds
     * @return the reservation duration time in seconds
     */
    public int getDurationTime() {
    	return reservation == null ? -1 : reservation.getDurationTime();
    }
    
    /**
     * Gets the remaining time until the end of the reservation in seconds
     * @return the reservation remaining time in seconds
     */
    public double getRemainingTime() {
    	return reservation == null ? -1 : getActualFinishTime() - GridSim.clock();
    }

    /**
     * Gets this object's number of PEs.
     * @return the reservation number of PEs requested
     */
    public int getNumPE() {
    	return reservation == null ? -1 : reservation.getNumPE();
    }
    
    /**
     * Gets the status of this reservation
     * @return this reservation current status
     */
    public int getStatus() {
    	return reservation == null ? 
    			ReservationStatus.UNKNOWN.intValue() : 
    				reservation.getStatus().intValue();
    }
    
    /**
     * Gets the status of this reservation
     * @return this reservation current status
     */
    public ReservationStatus getReservationStatus() {
    	return reservation == null ? 
    			ReservationStatus.UNKNOWN : 
    				reservation.getStatus();
    }

    /**
     * Gets this object's reservation ID
     * @return a reservation ID
     */
    public int getID() {
    	return reservation == null ? -1 : reservation.getID();
    }
    
    /**
     * Gets this object's expiry time
     * @return expiry time
     */
    public double getExpiryTime() {
        return expiryTime;
    }
    
    /**
     * Creates a String representation of this reservation
     * for displaying purposes
     * @param timeUnit the time unit to be used
     * @return the string representation
     * @see ScheduleItem#TIME_UNIT_SECOND
     * @see ScheduleItem#TIME_UNIT_MINUTE
     * @see ScheduleItem#TIME_UNIT_HOUR
     */
    public String toString(int timeUnit) {
    	String timeDescr = " " + getTimeDescr(timeUnit);
    	StringBuilder stringBuilder = new StringBuilder();
    	stringBuilder.append("Reservation ID: " +  reservation.getID());
    	stringBuilder.append("\nUser ID: " + getSenderID());
    	stringBuilder.append("\nStatus: " + reservation.getStatus().getDescription());
    	stringBuilder.append("\nSub. Time: " + formatTime(getSubmissionTime(), timeUnit) + timeDescr);
		stringBuilder.append("\nStart Time: " + formatTime(reservation.getStartTime(), timeUnit) + timeDescr);
		stringBuilder.append("\nFinishTime: " + formatTime(getActualFinishTime(), timeUnit) + timeDescr);
		stringBuilder.append("\nDuration: " + formatTime(reservation.getDurationTime(), timeUnit) + timeDescr);
		stringBuilder.append("\nNum. PEs: " + reservation.getNumPE());
    	return stringBuilder.toString();
    }
    
    /**
     * Creates a String representation of this reservation 
     * for debugging purposes
     * @return the string representation
     */
    public String toString() {
    	return toString(ScheduleItem.TIME_UNIT_SECOND);
    }
    
    /**
     * Returns a string that represents the description of 
     * a given time unit
     * @param timeUnit the time unit id
     * @return the string containing the description
     */
    private static String getTimeDescr(int timeUnit) {
		if(timeUnit == ScheduleItem.TIME_UNIT_SECOND) {
			return "sec.";
		} else if(timeUnit == ScheduleItem.TIME_UNIT_MINUTE) {
			return "min.";
		} else {
			return "hours";
		}
    }
    
	/**
	 * Converts the time to the time unit in use
	 * @param time the time in seconds
	 * @param timeUnit the time unit id
	 * @return the time in the unit in use
	 */
	private static String formatTime(double time, int timeUnit) {
		return decFormater.format(time / timeUnit);
	}

	/**
     * If the item is a job, this checks whether the 
     * item is associated with an advance reservation or not
     * @return <code>true</code>
     */
	public boolean hasReserved() {
		return true;  // true as it is an advance reservation
	}
}
