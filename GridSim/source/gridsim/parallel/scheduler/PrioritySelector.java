/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modelling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.scheduler;

import gridsim.parallel.profile.ScheduleItem;

/**
 * This interface is used by a scheduler to obtain the priority of a given  
 * schedule item (i.e. job or advance reservation). This information is
 * used by allocation policies that use priorities such as 
 * {@link AggressiveMultiPartitions}.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */

public interface PrioritySelector {
	
	/**
	 * Returns the priority of the item to be assigned by the scheduler 
	 * or allocation policy.
	 * @param item the item whose priority needs to be obtained.
	 * @return the priority of the schedule item or <tt>-1</tt> if unknown. 
	 */
	int getSchedulePriority(ScheduleItem item);
}
