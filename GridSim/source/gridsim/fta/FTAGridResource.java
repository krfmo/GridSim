package gridsim.fta;

import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;

import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Machine;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

/**
 * This class extends the {@link GridResource} class to add the failure to the resources.
 *  
 * @author       Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * 
 * @see 	GridResource
 * @see 	FTAllocPolicy
 */

public class FTAGridResource extends GridResource {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);

	public FTAGridResource(String name, double baud_rate,
			ResourceCharacteristics resource, ResourceCalendar calendar,
			AllocPolicy policy) throws Exception {
		super(name, baud_rate, resource, calendar, policy);

	}

	/**
	 * Processes other events or services related to reservations.
	 * 
	 * @param ev
	 *            a Sim_event object
	 * @pre ev != null
	 * @post $none
	 * @see MessageType
	 */
	protected void processOtherEvent(Sim_event ev) {
		int tag = ev.get_tag();

		switch (tag) {
		case GridSimTags.GRIDRESOURCE_FAILURE:
			handleResourceFailure(ev);
			break;

		case GridSimTags.GRIDRESOURCE_RECOVERY:
			handleResourceRecovery(ev);
			break;

		default:
			/****
			 * // NOTE: Give to the scheduler to process other tags
			 * System.out.println(super.get_name() + ".processOtherEvent(): " +
			 * "Unable to handle request from GridSimTags with " + "tag number "
			 * + ev.get_tag() );
			 *****/
			super.policy_.processOtherEvent(ev);
			break;
		}
	}
	/**
	 * Handle the resource failure <br>
	 * send pause to the gridlet which is running on the machine
	 * and set the machine as the failed machine 
	 * @param ev is the event which is included the machine id
	 */
	private void handleResourceFailure(Sim_event ev) {
		int mach_id;
		Object data = null;

		if (ev == null) {
			logger.log(Level.SEVERE, super.get_name() + ".processFailure(): "
					+ "Error - an event is null.");
			return;
		}

		/******************/
		data = ev.get_data(); 		// get the event's data
		mach_id = (Integer) data; 	// get the node ID

		// order is important!
		
		// send the message for Gridlet on this machine
		pauseGridletLists(mach_id);

		// set machine with id, mach_id as failed
		setMachineFailed(true, mach_id);

	}
	/**
	 * Handle the resource recovery <br>
	 * send resume to the gridlet which is running on the machine
	 * and set the machine as the working machine 
	 * @param ev is the event which is included the machine id
	 */

	private void handleResourceRecovery(Sim_event ev) {
		int mach_id;
		Object data = null;
		boolean success;
		boolean ifailed;
		
		if (ev == null) {
			logger.log(Level.SEVERE, super.get_name() + ".processRecovery(): "
					+ "Error - an event is null.");
			return;
		}

		// The machine status in GridSim is as follows:
		// FREE, BUSY, FAILED
		// we have to add some condition to resolve between FAILED to FREE / FAILED to BUSY
		/******************/
		data = ev.get_data(); // get the event's data
		mach_id = (Integer) data; // get the node ID
		
		// if the machine is set to FAILED status
		ifailed = resource_.getMachineList().getMachineInPos(mach_id).getFailed();
		
		// send the message for Gridlet on this machine
		// success: true means that it resumes a paused gridlet successfully 
		success = resumeGridletLists(mach_id);

		// if there is no gridlet paused for this machine
		// or if set to the failed
		// then set machine to the free
		if (!success && ifailed)
			// set machine with id, mach_id as working
			setMachineFailed(false, mach_id);

	}

	/**
	 * Set the status of a machine of this resource.
	 * 
	 * @param b
	 *            the new status of the machine. <tt>true</tt> means the machine
	 *            is failed, <tt>false</tt> means the machine is working
	 *            properly.
	 * @param id
	 *            the id of failed machine
	 * 
	 */
	private void setMachineFailed(boolean b, int id) {
		Machine mach;

		/************/
		String status = null;
		if (b)
			status = "FAILED";
		else
			status = "WORKING";
		/************/

		mach = resource_.getMachineList().getMachineInPos(id);
		if (mach != null) {
		   	logger.log(Level.INFO, " - Machine: " + id
					+ " is set to " + status + " at " + GridSim.clock());
			mach.setFailed(super.get_name(), b);
		} // end if
	}

	/**
	 * This method pauses the gridlet on the failed machine
	 * 
	 * @param machID
	 *            the id of the machine which has failed.
	 * @pre ev != null
	 * @post $none
	 */
	private void pauseGridletLists(int machID) {

		if (policy_ instanceof FTAllocPolicy)
			((FTAllocPolicy) policy_).setGridletsFailed(machID);

	}

	/**
	 * This method resumes the gridlet on the recovered machine
	 * 
	 * @param machID
	 *            the id of the machine which has failed.
	 * @pre ev != null
	 * @post $none
	 */
	private boolean resumeGridletLists(int machID) {
		boolean success = false;

		if (policy_ instanceof FTAllocPolicy)
			success = ((FTAllocPolicy) policy_).setGridletsResumed(machID);

		return success;
	}

}
