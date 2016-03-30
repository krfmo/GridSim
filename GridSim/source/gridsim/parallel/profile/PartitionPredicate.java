/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * This interface is used to filter what jobs/reservations should be put 
 * in a given partition by policies that use multiple partitions or queues. 
 *  
 * @author Marcos Dias de Assuncao
 * 
 * @since 5.0
 * 
 * @see ResourcePartition
 */

public interface PartitionPredicate {

	/**
	 * Checks whether a given job meets the criteria of the partition.
	 * @param item the job/reservation to be considered for scheduling.
	 * @return <code>true</code> if the job can be included in this
	 * partition; <code>false</code> otherwise.
	 */
	boolean match(ScheduleItem item);
	
}
