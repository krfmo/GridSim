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
 * {@link AllocationAction} corresponds to an allocation decision made 
 * by an allocation policy. When an event happens, about which, an 
 * {@link AllocationListener} has to be notified, an instance of this 
 * class is created and passed to the listener.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */

public class AllocationAction {
	private LinkedList<ScheduleItem> items;
	
	// the id of the entity that generated this action
	private int subject; 
	private ActionType actionType;
		
	/**
	 * Creates a new {@link AllocationAction} object.
	 * @param type the type of action performed
	 */
	public AllocationAction(ActionType type){
		actionType = type;
		items = null;
	}

	/**
	 * Gets the {@link ScheduleItem}s associated with this action. 
	 * @return the gridlets
	 */
	public LinkedList<ScheduleItem> getScheduleItems() {
		return items;
	}

	/**
	 * Sets the schedule items associated with this action
	 * @param list the schedule items
	 * @return <code>true</code> if the schedule items have been
	 * set; <code>false</code> otherwise
	 */
	public boolean setScheduleItems(LinkedList<ScheduleItem> list) {
		if(list == null) {
			return false;
		}
		
		this.items = list;
		return true;
	}

	/**
	 * Get the type of the allocation action
	 * @return the allocation action type
	 */
	public ActionType getActionType() {
		return actionType;
	}

	/**
	 * Sets the type of allocation action
	 * @param type the allocation action
	 */
	public void setActionType(ActionType type) {
		actionType = type;
	}

	/**
	 * Gets the id of the subject or entity that created this action
	 * @return the id of the subject or entity that created this action
	 */
	public int getSubject() {
		return subject;
	}

	/**
	 * Sets the id of the subject or entity that created this action
	 * @param subject the id of the subject or entity that created this action
	 */
	public void setSubject(int subject) {
		this.subject = subject;
	}
}
