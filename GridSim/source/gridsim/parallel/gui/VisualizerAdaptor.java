/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.util.LinkedList;

import gridsim.parallel.profile.ScheduleItem;

/**
 * Adaptor that implements {@link Visualizer} interface, 
 * but contains empty methods.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see Visualizer
 */
public class VisualizerAdaptor implements Visualizer {

	/**
     * Notifies the listener about the action performed<br>
     * <b>NOTE:</b> As this class is an adaptor, this method is empty.
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param pause whether the simulation should be paused
     * @param itemList the list of schedule items to provide to the listeners
     * @see ActionType
     */
	public void notifyListeners(int subjectId, ActionType actionType,
			boolean pause, LinkedList<ScheduleItem> itemList) {
	
	}

    /**
     * Notifies the listeners about the action performed<br>
     * <b>NOTE:</b> As this class is an adaptor, this method is empty.
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param pause whether the simulation should be paused
     * @param item the schedule item to provide to the listeners
     * @see ActionType
     */
	public void notifyListeners(int subjectId, ActionType actionType,
			boolean pause, ScheduleItem item) {
	
	}

    /**
     * Notifies the listeners about the action performed<br>
     * <b>NOTE:</b> As this class is an adaptor, this method is empty.
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param pause whether the simulation should be paused
     * @see ActionType
     */
	public void notifyListeners(int subjectId, ActionType actionType,
			boolean pause) {
	
	}

    /**
     * Notifies a listener about the action performed<br>
     * <b>NOTE:</b> As this class is an adaptor, this method is empty.
     * @param action the action performed
     * @see ActionType
     */
	public void notifyListeners(AllocationAction action) {
		
	}
}
