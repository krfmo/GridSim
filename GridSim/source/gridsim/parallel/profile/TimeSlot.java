/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * This class defines a time slot. A time slot can be used to represent a 
 * free fragment in the scheduling queue. This may be useful for users who
 * want to implement policies that perform best-fit, next-fit or worst-fit
 * time slot selection. For a description on how time slots can be utilised,
 * please read the following paper about the MAUI scheduler: 
 * <ul>
 * 	<li> David B. Jackson, Quinn Snell and Mark J. Clement. Core Algorithms 
 * 	of the Maui Scheduler, Revised Papers from the 7th International 
 * 	Workshop on Job Scheduling Strategies for Parallel Processing 
 * 	(JSSPP '01), Lecture Notes in Computer Science, pp. 87-102, London, UK.
 * </ul>
 *     
 * @author  Marcos Dias de Assuncao
 * @since 5.0
 *    
 * @see PERange
 * @see PERangeList
 */
public class TimeSlot {
	private double startTime;
	private double finishTime;
	private PERangeList ranges;
	
	/**
	 * Default constructor sets 
	 * @param startTime the start time of the time slot
	 * @param finishTime the finish time of the time slot 
	 * @param ranges the list of PE ranges available at the slot
	 */
	public TimeSlot(double startTime, double finishTime, PERangeList ranges) {
		this.startTime = startTime;
		this.finishTime = finishTime;
		this.ranges = ranges;
	}

	/**
	 * Sets the start time of the time slot
	 * @param startTime the start time
	 * @return <code>true</code> if set successfully; <code>false</code> otherwise.
	 * @pre startTime >= 0
	 */
	public boolean setStartTime(double startTime) {
		if(startTime < 0) {
			return false;
		}
		
		this.startTime = startTime;
		return true;
	}
	
	/**
	 * Sets the finish time of the time slot
	 * @param finishTime the finish time
	 * @return <code>true</code> if set successfully; <code>false</code> otherwise.
	 * @pre finishTime >= 0
	 */
	public boolean setFinishTime(double finishTime) {
		if(finishTime <0) {
			return false;
		}
		
		this.finishTime = finishTime;
		return true;
	}
	
	/**
	 * Sets the ranges of the time slot
	 * @param ranges the ranges of this time slot
	 * @return <code>true</code> if set successfully; <code>false</code> otherwise.
	 * @pre ranges != <code>null</code>
	 */
	public boolean setPERanges(PERangeList ranges) {
		if(ranges == null) {
			return false;
		}
		
		this.ranges = ranges;
		return true;
	}
	
	/**
	 * Returns the start time of this time slot
	 * @return the start time of this time slot
	 */
	public double getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the finish time of this time slot
	 * @return the finish time of this time slot
	 */
	public double getFinishTime() {
		return finishTime;
	}
	
	/**
	 * Returns the time duration of this time slot.
	 * @return the time duration of this time slot.
	 */
	public double getDuration() {
		return finishTime - startTime;
	}
	
	/**
	 * Returns the PE ranges available at the time slot 
	 * @return the PE ranges available at the time slot
	 */
	public PERangeList getPERanges() {
		return ranges;
	}
	
	/**
	 * Returns the number of PEs available at the time slot
	 * @return the number of PEs available at the time slot
	 */
	public int getNumPE() {
		return ranges == null ? 0 : ranges.getNumPE();
	}
	
	/**
	 * Creates a string representation of the list
	 * @return a string representation
	 */
	public String toString() {
		return "TimeSlot={startTime=" + startTime + 
					", finishTime=" + finishTime + ", ranges=" + ranges + "}";
	}
}
