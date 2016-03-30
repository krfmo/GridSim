/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.gui;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import gridsim.GridSim;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import gridsim.parallel.profile.ScheduleItem;

/**
 * This interface has to be implemented by a visualisation tool. 
 * {@link GridSim} class will notify the visualiser provided about the
 * allocation actions made by the entities. The duty of a visualiser
 * is to notify the interface components about these actions.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see ParallelVisualizer
 */

public abstract class AbstractVisualizer extends JFrame implements Visualizer {
	private static final long serialVersionUID = -7168310225568665130L;
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	private boolean slowMotionMode = false;
    private boolean stepByStepMode = false;
    
    // the GUI settings
    protected static GUISettings settings = GUISettings.getInstance();
    
    /**
     * Enables the slow motion mode (used by the GUI)
     */
    public void enableSlowMotionMode() {
    	slowMotionMode = true;
    }
    
    /**
     * Disables the slow motion mode (used by the GUI)
     */
    public void disableSlowMotionMode() {
    	slowMotionMode = false;
    }
    
    /**
     * Returns <code>true</code> if the slow motion mode is enabled
     * @return <code>true</code> if the slow motion mode is enabled; 
     * <code>false<code> otherwise
     */
    public boolean isSlowMotionModeEnabled() {
    	return slowMotionMode;
    }
   
    /**
     * Enables the step by step mode (used by the GUI)
     */
    public void enableStepByStepMode() {
    	stepByStepMode = true;
    }
    
    /**
     * Disables the step by step mode (used by the GUI)
     */
    public void disableStepByStepMode() {
    	stepByStepMode = false;
    }

    /**
     * Returns <code>true</code> if the step by step mode is enabled
     * @return <code>true</code> if the step by step mode is enabled; 
     * <code>false<code> otherwise
     */
    public boolean isStepByStepEnabled() {
    	return stepByStepMode;
    }

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
    public void notifyListeners(int subjectId, ActionType actionType, 
    		boolean pause, LinkedList<ScheduleItem> itemList) {
    	
   		AllocationAction action = new AllocationAction(actionType);
   		action.setSubject(subjectId);
   		action.setScheduleItems(itemList);
   		placeAction(action, pause);
    }
    
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
    public void notifyListeners(int subjectId, 
    		ActionType actionType, boolean pause, ScheduleItem item) {
    	
    	LinkedList<ScheduleItem> itemList = null;
    	if(item != null) {
    		itemList = new LinkedList<ScheduleItem>();
    		itemList.add(item);
    	}
    	
    	notifyListeners(subjectId, actionType, pause, itemList);
    }
    
    /**
     * Notifies the listeners about the action performed
     * @param subjectId the subject, or entity, that created the action
     * @param actionType the action performed
     * @param shouldPause indicates whether the simulation should be paused after 
     * notifying the listeners. <code>true</code> indicates that it should pause and
     * <code>false</code> means that it should not.
     * @see ActionType
     */
    public void notifyListeners(int subjectId, ActionType actionType, 
    		boolean shouldPause) {
    	
   		AllocationAction action = new AllocationAction(actionType);
   		action.setSubject(subjectId);
   		placeAction(action, shouldPause);
    }
    
    /*
     * Inserts an allocation action in the visualiser's buffer
     */
    private void placeAction(AllocationAction action, boolean shouldPause) {
		notifyListeners(action);

    	if(shouldPause){
    		if(isStepByStepEnabled()){
    			GridSim.pauseSimulation();
	    	}
    		else if(isSlowMotionModeEnabled()){
    			smallSimulationPause();
    		}
   		}
    }
    
    /*
     * Called by an entity to make a small paused during the simulation.
     * This method should be used for debugging purposes only.
     */
    private static void smallSimulationPause() {
    	GridSim.pauseSimulation();
    	try {
			Thread.currentThread().sleep(300);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, "Exception pausing the simulation.", e);
		}
		GridSim.resumeSimulation();
    }
    
    /**
     * Notifies a listener about the action performed
     * @param action the action performed
     * @see ActionType
     */
    public abstract void notifyListeners(AllocationAction action);
    
}
