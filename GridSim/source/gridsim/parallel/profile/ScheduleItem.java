/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * {@link ScheduleItem} represents an item that can be put in
 * a scheduling queue. This class is used to make it easier to update
 * availability profiles, job cancellation in space shared allocation policies,
 * among other things. In addition, this is used by visualisation windows 
 * for GUI purposes. The item can be a job or an advance reservation.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 */

public interface ScheduleItem {
	
	/** Second as the time unit */
	int TIME_UNIT_SECOND = 1;
	
	/** Minute as the time unit */
	int TIME_UNIT_MINUTE = 60;
	
	/** Hour as the time unit */
	int TIME_UNIT_HOUR = 60 * 60;
	
	/**
	 * Returns an ID for this item
	 * @return the item's ID
	 */
	 int getID();
	 
	 /**
	  * Returns the id of the user that created this item
	  * @return the user id
	  */
	 int getSenderID();
	 
	 /**
	  * Returns the status of this item
	  * @return the status of the item
	  */
	 int getStatus();
	 
	 /**
	  * Returns the number of PEs used by this item
	  * @return the number of items
	  */
	 int getNumPE();
	 
	 /**
	  * Returns the time of submission of this item
	  * @return the submission time
	  */
	 double getSubmissionTime();
	 
	/**
     * Gets the item's start time
     * @return start time of an item or <tt>-1.0</tt> if
     * not known
     */
	 double getStartTime();
	
    /**
     * Gets the item's real finish time. That is, the actual
     * time when the item is to finish
     * @return finish time of an item or <tt>-1.0</tt> if
     * not known
     */
	 double getActualFinishTime();
     
     /**
      * Gets the item's expected finish time. That is, this end
      * time is based on the estimate provided by the user and may
      * not reflect the actual finish time of the schedule item.
      * @return finish time of an item or equals to the actual
      * finish time if not known.
      */
	 double getExpectedFinishTime();
     
     /**
      * Gets the priority of this item assigned by the scheduler
      * @return the priority or <tt>-1</tt> if not found
      */
     int getPriority();
     
     /**
      * Gets the id of the partition or queue to which this
      * item was scheduled
      * @return the partition id or <tt>-1</tt> if not found
      */
     int getPartitionID();
     
     /**
      * Gets the list of ranges of PEs used by this item
      * @return a list containing the ranges
      */
     PERangeList getPERangeList();
     
     /**
      * Checks whether the item is an advance reservation or not
      * @return <tt>true</tt> if the item is an advance reservation
      * or <tt>false</tt> if it is not.
      */
     boolean isAdvanceReservation();
     
     /**
      * If the item is a job, this checks whether the 
      * item is associated with an advance reservation or not
      * @return <tt>true</tt> if an advance reservation for this
      * item has been made, or <tt>false</tt> otherwise.
      */
     boolean hasReserved();
     
     /**
      * Creates a String representation of this item 
      * for displaying purposes
      * @param timeUnit the time unit to be used
      * @see ScheduleItem#TIME_UNIT_SECOND
      * @see ScheduleItem#TIME_UNIT_MINUTE
      * @see ScheduleItem#TIME_UNIT_HOUR
      * @return a String representation of this item 
      * for displaying purposes
      */
     String toString(int timeUnit);

}
