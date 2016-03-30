/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.scheduler;

import gridsim.parallel.SSGridlet;
import gridsim.parallel.profile.PartitionPredicate;
import gridsim.parallel.profile.ScheduleItem;
import gridsim.parallel.reservation.ServerReservation;

/**
 * This predicate filters jobs according to their runtime
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public class RuntimePredicate implements PartitionPredicate {
	private int minRuntime;
	private int maxRuntime;
	private int resRating;
	
	/**
	 * Default constructor
	 * @param minRuntime jobs with smaller runtime are filtered out by this predicate
	 * @param maxRuntime jobs with greater runtime are filtered out by this predicate
	 * @param rating the rating in MIPS of the resource that uses this predicate 
	 */
	public RuntimePredicate(int minRuntime, 
			int maxRuntime, int rating) {
		this.minRuntime = minRuntime;
		this.maxRuntime = maxRuntime;
		this.resRating = rating;
	}
	
	/**
	 * Checks whether a given job meets the criteria of the partition or category
	 * @param item the job/reservation to be considered for scheduling.
	 * @return <code>true</code> if the job can be included in this
	 * partition; <code>false</code> otherwise.
	 */
	public boolean match(ScheduleItem item) {
		long runtime = 0;
		if(item instanceof SSGridlet) {
			runtime = forecastExecutionTime((SSGridlet)item);
		} else {
			runtime = ((ServerReservation)item).getDurationTime();
		}
		
		if(runtime < minRuntime || runtime >= maxRuntime) {
			return false;
		}
		
		return true;
	}
	
    /*
     * Forecast execution time of a Gridlet.
     * execution time = length / available rating
     */
    private long forecastExecutionTime(SSGridlet gridlet) {
    	long runTime = (long)((gridlet.getLength() / resRating) + 1); 
        return Math.max(runTime, 1);
    }
}
