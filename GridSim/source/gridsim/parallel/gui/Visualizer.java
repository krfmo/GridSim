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
 * Interface implemented by a visualisation tool.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public interface Visualizer {

    /**
     * Notifies the listener about the action performed
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param pause indicates whether the simulation should be paused after 
     * notifying the listeners. <code>true</code> indicates that it should pause and
     * <code>false</code> means that it should not.
     * @param itemList the list of schedule items to provide to the listeners
     * @see ActionType
     */
    void notifyListeners(int subjectId, ActionType actionType, 
    		boolean pause, LinkedList<ScheduleItem> itemList);
    
    /**
     * Notifies the listeners about the action performed
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param pause indicates whether the simulation should be paused after 
     * notifying the listeners. <code>true</code> indicates that it should pause and
     * <code>false</code> means that it should not.
     * @param item the schedule item to provide to the listeners
     * @see ActionType
     */
    void notifyListeners(int subjectId, ActionType actionType, 
    		boolean pause, ScheduleItem item);
    
    /**
     * Notifies the listeners about the action performed
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param shouldPause indicates whether the simulation should be paused after 
     * notifying the listeners. <code>true</code> indicates that it should pause and
     * <code>false</code> means that it should not.
     * @see ActionType
     */
    void notifyListeners(int subjectId, ActionType actionType, 
    		boolean shouldPause);
    
    /**
     * Notifies a listener about the action performed
     * @param action the action performed
     * @see ActionType
     */
    void notifyListeners(AllocationAction action);

}
