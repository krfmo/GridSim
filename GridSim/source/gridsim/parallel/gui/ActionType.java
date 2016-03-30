/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

/**
 * This enumerator contains the types of allocation actions.
 * 
 * @author Marcos Dias de Assuncao
 * 
 * @since 5.0
 */
public enum ActionType {
	
	/** Indicates that a schedule item has arrived at the subject */
	ITEM_ARRIVED(0),
	
	/** Indicates that a schedule item has scheduled */
	ITEM_SCHEDULED(1),
	
	/** Denotes that a schedule item has completed execution */
	ITEM_COMPLETED(2),
	
	/** Indicates that a schedule item has been cancelled */
	ITEM_CANCELLED(3), 
	
	/** Denotes that the status of a schedule item has changed */
	ITEM_STATUS_CHANGED(4),
	
	/** Indicates a substantial change in the scheduling queue */
	SCHEDULE_CHANGED(5), 
	
	/** Denotes that the simulation time has changed */
	SIMULATION_TIME_CHANGED(6);
	
	private int value;
	
	private ActionType(int value) {
		this.value = value;
	}

	/**
	 * Returns the integer value of the type.
	 * @return the integer value of the type.
	 */
	public int intValue() {
		return value;
	}
}
