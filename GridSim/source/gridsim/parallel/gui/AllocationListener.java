/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

/**
 * {@link AllocationListener} interface has to be implemented by 
 * classes that register with the allocation policy to receive 
 * information about the scheduling of {@link gridsim.Gridlet}s. This is 
 * mainly used by the visualisation tool to display information 
 * about the scheduling queues.
 *  
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */

public interface AllocationListener {

	/**
	 * This method has to be implemented by the listener 
	 * to handle the action
	 * @param action the action taken by an entity
	 * @return <code>true</code> if the action has been handled successfully
	 * or <code>false</code> otherwise. 
	 */
	boolean allocationActionPerformed(AllocationAction action);
	
}
