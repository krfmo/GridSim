package gridsim.parallel;

import java.text.DecimalFormat;

import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.ScheduleItem;

/**
 * {@link SSGridlet} represents a Gridlet submitted to a 
 * {@link ParallelResource} for processing (i.e. Server Side). 
 * This class keeps track of the time for all activities in the 
 * {@link ParallelResource} for a specific Gridlet. Before a Gridlet exits 
 * the GridResource, it is RECOMMENDED to call this method {@link #finalizeGridlet()}.
 * <p>
 * It contains a Gridlet object along with its arrival time and
 * the ranges of PEs (Processing Element) allocated to it.
 *
 * @author 	Marcos Dias de Assuncao
 * @since 5.0
 */

public class SSGridlet implements ScheduleItem {
    private Gridlet gridlet;     	// a Gridlet object
    private double arrivalTime;    	// Gridlet arrival time for the first time
    private double finishedSoFar;  	// length of Gridlet finished so far

    // Gridlet execution start time. This attribute will only hold the latest
    // time since a Gridlet can be cancel, paused or resumed.
    private double startExecTime;
    
    // total time to complete this Gridlet
    private double totalCompletionTime;  	
    
    // the time when the Gridlet is to start execution
    private double startTime;
    
    // the finish time of the Gridlet
    private double actualFinishTime;
    
    // estimation of Gridlet finished time
    private double expectedFinishTime;

    // A list of ranges of PEs used by this Gridlet
    private PERangeList peRangeList = null;
    
    // the partition or queue in the resource to which this 
    // gridlet was scheduled
    private int partition;
    
    // the priority of this gridlet assigned by the scheduler
    private int priority;

    // reservation id associated with the Gridlet
    private int reservID;
    
	private static DecimalFormat decFormater = new DecimalFormat("#,##0.00");
    private static final int NOT_FOUND = -1;

    /**
     * Allocates a new object upon the arrival of a Gridlet object.
     * The arriving time is determined by {@link gridsim.GridSim#clock()}.
     * @param gridlet a gridlet object
     * @see gridsim.GridSim#clock()
     * @pre gridlet != null
     * @post $none
     */
    public SSGridlet(Gridlet gridlet) {
        this.gridlet = gridlet;
        this.reservID = gridlet.getReservationID();
        init();
    }
    
    /**
     * Creates a clone of the original gridlet object. The Gridlet object is
     * not copied.
     * @param original the original object
     */
    protected SSGridlet(SSGridlet original) {
    	gridlet = original.gridlet;
    	arrivalTime = original.arrivalTime;
        finishedSoFar = original.finishedSoFar; 
        startExecTime = original.startExecTime;
        totalCompletionTime = original.totalCompletionTime;  	
        startTime = original.startTime;
        actualFinishTime = original.actualFinishTime;
        expectedFinishTime = original.expectedFinishTime;
        peRangeList = original.peRangeList == null ? null : original.peRangeList.clone();
        partition = original.partition;
        priority = original.priority;
        reservID = original.reservID;
    }

    // -------------------- PUBLIC METHODS ---------------------

    /**
	 * Gets the time that a Gridlet is supposed to start. That is,
     * the allocation policy may decide to start the execution of this
     * Gridlet at a particular time in the future. The potential start
     * time represents this time.
     * @return the potential start time 
     */
	public double getStartTime() {
		return startTime;
	}

	/**
	 * Sets the time that a Gridlet is supposed to start. That is,
     * the allocation policy may decide to start the execution of this
     * Gridlet at a particular time in the future. The potential start
     * time represents this time.
	 * @param startTime the potential start time
	 */
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

    /**
     * Gets the number of PEs required to execute this Gridlet.
     * @return  number of PE
     * @pre $none
     * @post $none
     */
    public int getNumPE() {
        return gridlet.getNumPE();
    }

    /**
     * Gets the reservation ID that owns this Gridlet
     * @return a reservation ID
     * @pre $none
     * @post $none
     */
    public int getReservationID() {
        return reservID;
    }

    /**
     * Checks whether this Gridlet is submitted by reserving or not.
     * @return <code>true</code> if this Gridlet has reserved before,
     *         <code>false</code> otherwise
     * @pre $none
     * @post $none
     */
    public boolean hasReserved() {
        return (reservID != NOT_FOUND);
    }
    
    /**
     * Checks if this object is an advance reservation or not
     * @return <code>false</code> indicating that it is not
     */
	public boolean isAdvanceReservation() {
		return false;
	}

    /**
     * Gets this Gridlet entity Id
     * @return the Gridlet entity Id
     * @pre $none
     * @post $none
     */
    public int getID() {
        return gridlet.getGridletID();
    }

    /**
     * Gets the user or owner of this Gridlet
     * @return the Gridlet's user Id
     * @pre $none
     * @post $none
     */
    public int getSenderID() {
        return gridlet.getUserID();
    }

    /**
     * Gets the Gridlet's length
     * @return Gridlet's length
     * @pre $none
     * @post $none
     */
    public double getLength() {
        return gridlet.getGridletLength();
    }

    /**
     * Gets the Gridlet's class type
     * @return class type of the Gridlet
     * @pre $none
     * @post $none
     */
    public int getClassType() {
        return gridlet.getClassType() ;
    }

    /**
     * Sets the Gridlet status.
     * @param status  the Gridlet status
     * @return <code>true</code> if the new status has been set, <code>false</code>
     *         otherwise
     * @pre status >= 0
     * @post $none
     */
    public boolean setStatus(int status) {
        // gets Gridlet's previous status
        int prevStatus = gridlet.getGridletStatus();

        // if the status of a Gridlet is the same as last time, then ignore
        if (prevStatus == status) {
            return false;
        }

        boolean success = true;
        try {
        	double clock = GridSim.clock();   // gets the current clock

            // sets Gridlet's current status
            try {
				gridlet.setGridletStatus(status);
			} catch (Exception e) {
				// It should not happen
			}

            // if a previous Gridlet status is INEXEC
            if (prevStatus == Gridlet.INEXEC) {
                // and current status is either CANCELED, PAUSED or SUCCESS
                if (status == Gridlet.CANCELED || status == Gridlet.PAUSED ||
                    status == Gridlet.SUCCESS) {
                    // then update the Gridlet completion time
                    totalCompletionTime += (clock - startExecTime);
                    return true;
                }
            }

            if (prevStatus == Gridlet.RESUMED && status == Gridlet.SUCCESS) {
                // then update the Gridlet completion time
                totalCompletionTime += (clock - startExecTime);
                return true;
            }

            // if a Gridlet is now in execution
            if (status == Gridlet.INEXEC ||
                (prevStatus == Gridlet.PAUSED && status == Gridlet.RESUMED) ) {
                startExecTime = clock;
                gridlet.setExecStartTime(startExecTime);
            }
        }
        catch(IllegalArgumentException e) {
            success = false;
        }

        return success;
    }

    /**
     * Gets the Gridlet's execution start time
     * @return Gridlet's execution start time
     * @pre $none
     * @post $none
     */
    public double getExecStartTime() {
        return gridlet.getExecStartTime();
    }

    /**
     * Sets the ranges of PEs used by this Gridlet
     * @param rangeList the range of PEs
     * @pre rangeList != null
     */
    public void setPERangeList(PERangeList rangeList) {
    	peRangeList = rangeList;
    }
    
    /**
     * Gets the list of ranges of PEs used by this Gridlet
     * @return a list containing the ranges
     */
    public PERangeList getPERangeList() {
        return peRangeList;
    }

    /**
     * Gets the id of the partition or queue to which this
     * gridlet was scheduled
     * @return the partition id or <code>-1</code> if not found
     */
    public int getPartitionID() {
		return partition;
	}

    /**
     * Sets the id of the partition or queue to which this
     * gridlet was scheduled
     * @param partition the partition id
     * @return <code>true</code> if set correctly or <code>false</code> otherwise.
     */
	public boolean setPartitionID(int partition) {
		if(partition < 0) {
			return false;
		}
		
		this.partition = partition;
		return true;
	}
	
    /**
     * Gets the priority of this gridlet assigned by the scheduler
     * @return the priority or <code>-1</code> if not found
     */
    public int getPriority() {
		return priority;
	}

    /**
     * Sets the priority of this gridlet assigned by the scheduler
     * @param priority the priority
     * @return <code>true</code> if set correctly or <code>false</code> otherwise.
     */
	public boolean setPriority(int priority) {
		if(priority < 0) {
			return false;
		}
		
		this.priority = priority;
		return true;
	}

	/**
     * Gets the remaining gridlet length
     * @return gridlet length
     * @pre $none
     * @post $result >= 0
     */
    public double getRemainingLength() {
        double length = gridlet.getGridletLength() - finishedSoFar;

        // Remaining Gridlet length can't be negative number. This can be
        // happening when this.updateGridletFinishedSoFar() keep calling.
        if (length < 0.0) {
            length = 0.0;
        }

        return length;
    }

    /**
     * Finalises all relevant information before <tt>exiting</tt> the
     * GridResource entity. This method sets the final data of:
     * <ul>
     *     <li> wall clock time, i.e. the time of this Gridlet resides in
     *          a GridResource (from arrival time until departure time).
     *     <li> actual CPU time, i.e. the total execution time of this
     *          Gridlet in a GridResource.
     *     <li> Gridlet's finished so far
     * </ul>
     * @pre $none
     * @post $none
     */
    public void finalizeGridlet() {
        // Sets the wall clock time and actual CPU time
    	double wallClockTime = GridSim.clock() - arrivalTime;
        gridlet.setExecParam(wallClockTime, totalCompletionTime);

        double finished = 0;
        if (gridlet.getGridletLength() < finishedSoFar) {
            finished = gridlet.getGridletLength();
        }
        else {
            finished = finishedSoFar;
        }

        gridlet.setGridletFinishedSoFar(finished);
    }

    /**
     * A method that updates the length of gridlet that has been completed
     * @param miLength gridlet length in Million Instructions (MI)
     * @pre miLength >= 0.0
     * @post $none
     */
    public void updateGridletFinishedSoFar(double miLength) {
        finishedSoFar += miLength;
    }

    /**
     * Gets arrival time of a gridlet
     * @return arrival time
     * @pre $none
     * @post $result >= 0.0
     */
    public double getArrivalTime() {
        return arrivalTime;
    }
    
    /**
     * Gets the submission or arrival time of this Gridlet from
     * the latest GridResource
     * @return the submission time or <code>0.0</code> if none
     */
    public double getSubmissionTime() {
        return gridlet.getSubmissionTime();
    }

    /**
     * Sets the actual finish time for this Gridlet. That is, the time when
     * the gridlet will finish. If time is negative, then it is being ignored.
     * @param time   finish time
     * @pre time >= 0.0
     * @post $none
     */
    public void setActualFinishTime(double time) {
        if (time < 0.0) {
            return;
        }

        actualFinishTime = time;
    }
    
    /**
     * Sets the item's expected finish time. That is, this end
     * time is based on the estimate provided by the user and may
     * not reflect the actual finish time of the schedule item.
     * @param time   the expected finish time
     * @pre time >= 0.0
     * @post $none
     */
    public void setExpectedFinishTime(double time) {
        if (time < 0.0) {
            return;
        }

        expectedFinishTime = time;
    }

	/**
     * Gets the Gridlet's finish time
     * @return finish time of a gridlet or <code>-1.0</code> if
     *         it cannot finish in this hourly slot
     */
    public double getActualFinishTime() {
        return actualFinishTime;
    }
    
    /**
     * Gets the gridlet's expected finish time. That is, this end
     * time is based on the estimate provided by the user and may
     * not reflect the actual finish time of the schedule item.
     * @return finish time of an item or equals to the actual
     * finish time if not known.
     */
    public double getExpectedFinishTime() {
        return expectedFinishTime > 0.0 ? 
        		expectedFinishTime : actualFinishTime;
    }

    /**
     * Gets this Gridlet object
     * @return gridlet object
     * @pre $none
     * @post $result != null
     */
    public Gridlet getGridlet() {
        return gridlet;
    }

    /**
     * Gets the Gridlet status
     * @return Gridlet status
     * @pre $none
     * @post $none
     */
    public int getStatus() {
        return gridlet.getGridletStatus();
    }
    
    /**
     * Creates a String representation of this Gridlet 
     * for debugging purposes
     * @return a string
     */
    public String toString() {
    	String result = "Gridlet {id=" + getID() + 
    		", start time=" + startTime + 
    		", expected finish time=" + expectedFinishTime + 
    		", actual finish time=" + actualFinishTime + 
    		", arrival time=" + arrivalTime + 
    		", n. PEs=" + getNumPE() + 
    		", ranges=" + peRangeList + "}";
    	return result;
    }

    /**
     * Creates a String representation of this Gridlet 
     * for displaying purposes
     * @param timeUnit the time unit to be used
     * @return a string
     * @see ScheduleItem#TIME_UNIT_SECOND
     * @see ScheduleItem#TIME_UNIT_MINUTE
     * @see ScheduleItem#TIME_UNIT_HOUR
     */
    public String toString(int timeUnit){
    	String timeDescr = " " + getTimeDescr(timeUnit);
    	StringBuilder stringBuilder = new StringBuilder(); 
    	stringBuilder.append("Gridlet ID: " + gridlet.getGridletID());
    	stringBuilder.append("\nUser ID: " + gridlet.getUserID());
    	stringBuilder.append("\nStatus: " + Gridlet.getStatusString(gridlet.getGridletStatus()));
    	stringBuilder.append("\nSub. Time: " + formatTime(getSubmissionTime(), timeUnit) + timeDescr);
		stringBuilder.append("\nStart Time: " + formatTime(startTime, timeUnit) + timeDescr);
		stringBuilder.append("\nExp. Finish Time: " + formatTime(expectedFinishTime, timeUnit) + timeDescr);
		stringBuilder.append("\nFinish Time: " + formatTime(actualFinishTime, timeUnit) + timeDescr);
		stringBuilder.append("\nDuration: " + formatTime(actualFinishTime - startTime, timeUnit) + timeDescr);
		stringBuilder.append("\nLength: " + gridlet.getGridletLength() + " MIs");
		stringBuilder.append("\nNum. PEs: " + getNumPE());
    	return stringBuilder.toString();
    }
    
    // -------------------- PRIVATE METHODS ---------------------
   
    /*
     * Initialises all local attributes
     * @pre $none
     * @post $none
     */
    private void init() {

        this.arrivalTime = GridSim.clock();
        this.gridlet.setSubmissionTime(arrivalTime);

        // default values
        this.actualFinishTime = NOT_FOUND;  // Cannot finish in this hourly slot.
        this.expectedFinishTime = NOT_FOUND;
        this.startTime = NOT_FOUND;
        this.totalCompletionTime = 0L;
        this.startExecTime = 0L;
        this.partition = NOT_FOUND;
        this.priority = NOT_FOUND;

        // In case a Gridlet has been executed partially by some other grid
        // resources.
        this.finishedSoFar = gridlet.getGridletFinishedSoFar();
    }
    
    /*
     * Returns a string that represents the description of 
     * a given time unit
     * @param timeUnit the time unit id
     * @return the string containing the description
     */
    private static String getTimeDescr(int timeUnit) {
    	String descr = null;
    	
    	switch(timeUnit) {
    		case ScheduleItem.TIME_UNIT_SECOND:
    			descr = "sec.";
    			break;
    			
    		case ScheduleItem.TIME_UNIT_MINUTE:
    			descr = "min.";
    			break;
    			
    		default:
    			descr = "hours";
				break;
    	}
    	
		return descr;
    }
    
	/*
	 * Converts the time to the time unit in use
	 * @param time the time in seconds
	 * @param timeUnit the time unit id
	 * @return the time in the unit in use
	 */
	private static String formatTime(double time, int timeUnit) {
		return decFormater.format(time / timeUnit);
	}
} 

