/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.AllocPolicy;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.parallel.ResourceDynamics;
import gridsim.parallel.SSGridlet;
import gridsim.parallel.SSGridletList;
import gridsim.parallel.gui.ActionType;
import gridsim.parallel.gui.Visualizer;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import gridsim.parallel.profile.PERange;
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.ProfileEntry;
import gridsim.parallel.profile.SingleProfile;
import gridsim.parallel.util.WorkloadLublin99;

/**
 * This class is an allocation strategy for {@link gridsim.parallel.ParallelResource} 
 * that implements aggressive backfilling (EASY). The policy is based on the 
 * aggressive backfilling algorithm described in the following paper:
 * <p>
 * <ul>
 * 		<li>Ahuva W. Mu'alem and Dror G. Feitelson, Utilization, Predictability, 
 * 		Workloads, and User Runtime Estimates in Scheduling the IBM SP2
 * 		with Backfilling. IEEE Transactions on Parallel and Distributed 
 * 		Systems, 12:(6), pp. 529-543, 2001.
 * </ul>
 * <br> This policy maintains an availability profile. The availability 
 * profile contains information about the ranges of processing elements
 * (PEs) that will be released when the running jobs complete.
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines comprising this resource must be 
 * 			 homogeneous.
 * 		<li> Local load is not considered. If you would like to 
 * 			 simulate this, you have to model the local load as jobs. 
 * 			 It is more precise and faster. To do so, please check 
 * 			 {@link WorkloadLublin99}.
 *      <li> Jobs cannot be paused or migrated.
 *  </ul>
 * 
 * @author  Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @see gridsim.AllocPolicy
 * @see PERange
 * @see PERangeList
 */

public class AggressiveBackfill extends AllocPolicy {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL); 
	
    protected SSGridletList waitingJobs = new SSGridletList();  
    protected SSGridletList runningJobs = new SSGridletList();  	
    protected SingleProfile profile;			 	 // availability of PEs			
	protected int ratingPE = 0;					 	 // The rating of one PE
	protected Comparator<SSGridlet> jobOrder = null; // sorts the jobs for backfilling
	protected ResourceDynamics dynamics = null;

	// Pivot job (i.e. the first job in the queue)
    protected SSGridlet pivot = null;
	
	// To update the schedule when required
	protected static final int UPT_SCHEDULE = 10;
	private double lastSchedUpt = 0.0D;				 // time of last schedule update
	private Visualizer visualizer = null;

	/**
     * Allocates a new {@link AggressiveBackfill} object
     * @param resourceName    the resource entity name that will 
     *                        contain this allocation policy
     * @param entityName      this object entity name
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     *  <li> Creating this entity before initialising GridSim package
     *  <li> The entity name is <code>null</code> or empty
     *  <li> The entity has <code>zero</code> number of PEs (Processing
     *       Elements). <br>
     *       No PEs, which means that the Gridlets cannot be processed.
     *       A GridResource must contain one or more Machines.
     *       A Machine must contain one or more PEs.
     * </ul>
     */
    public AggressiveBackfill(String resourceName, 
    		String entityName) throws Exception{
        super(resourceName, entityName);
    }
    
    /**
     * Handles internal events that come to this entity.
     */
    public void body() {

    	// get the resource characteristics object to be used
    	dynamics = (ResourceDynamics)super.resource_;
    	
       	// Gets the information on number of PEs and rating 
    	// of one PE assuming that the machines are homogeneous
        ratingPE = dynamics.getMIPSRatingOfOnePE();
        
        // creates the profile responsible to keep resource availability info
        profile = new SingleProfile(super.totalPE_);
        visualizer = GridSim.getVisualizer();

        // a loop that is looking for internal events only
        // This loop does not stop when the policy receives an
        // end of simulation event because it needs to handle
        // all the internal events before it finishes its
        // execution. This is particularly important for the
        // GUI to show the completion of advance reservations
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() ) {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION ||
                super.isEndSimulation()) {
                break;
            }
           	
            processEvent(ev);
        }

        // CHECK for ANY INTERNAL EVENTS WAITING TO BE PROCESSED
        while (super.sim_waiting() > 0) {
            // wait for event and ignore
            super.sim_get_next(ev);
            logger.info("Ignore internal events");
        }
        
        waitingJobs.clear();
        runningJobs.clear();
        lastSchedUpt = 0.0D;
        dynamics.resetFreePERanges();
    }
        
    /**
     * Sets the heuristic used to order the jobs considered for backfilling
     * or when a cancellation takes place
     * @param comparator a comparator implementation.
     * @return <code>true</code> if the heuristic was set correctly.
     */
    public boolean setJobOrderingHeuristic(Comparator<SSGridlet> comparator) {
    	if(comparator == null) {
    		return false;
    	}
    	
    	jobOrder = comparator;
    	return true;
    }
    
    /*
     * Process and event sent to this entity 
     * @param ev the event to be handled
     */
    private synchronized void processEvent(Sim_event ev) {
    	double currentTime = GridSim.clock();
    	boolean success = false;

    	if(ev.get_src() == myId_) {
		   	if (ev.get_tag() == UPT_SCHEDULE) {
		   		if(currentTime > lastSchedUpt) {
				    // updates the schedule, finish jobs, etc.
		   			updateSchedule();
		   			lastSchedUpt = currentTime;
		   		}
		   		success = true;
	    	}
		}
    	
    	if(!success) {
    		processOtherEvent(ev);
    	}
    }

	/**
     * Schedules/adds to the queue a new job received by the resource entity.
     * @param gridlet a Gridlet object to be executed
     * @param ack an acknowledgement, i.e. <code>true</code> if wanted to know
     * whether this operation is successful or not; 
     * <code>false</code> otherwise (don't care)
     */
    @Override
    public synchronized void gridletSubmit(Gridlet gridlet, boolean ack) {
    	int reqPE = gridlet.getNumPE();
    	
    	try {
	    	if(reqPE > super.totalPE_) {
	    		String userName = GridSim.getEntityName( gridlet.getUserID() );
	    		logger.info("Gridlet #" + gridlet.getGridletID() + " from " +
    	                userName + " user requires " + gridlet.getNumPE() + " PEs." +
	    	            "\n--> The resource has only " + super.totalPE_ + " PEs.");
	    		gridlet.setGridletStatus(Gridlet.FAILED);
	    		super.sendFinishGridlet(gridlet);
	    		return;
	    	}
    	} catch(Exception ex) {
    		logger.log(Level.WARNING, "Exception on submission of a Gridlet", ex);
    	}
    	
    	SSGridlet sgl = new SSGridlet(gridlet); // the server side gridlet
    	
    	//-------------- FOR DEBUGGING PURPOSES ONLY  --------------
    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_ARRIVED, true, sgl);
        //----------------------------------------------------------

    	sgl.setStatus(Gridlet.QUEUED);
       	waitingJobs.add(sgl);
    	
        if (ack) {
            // sends back an ack
        	super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
        			gridlet.getGridletID(), gridlet.getUserID());
        }
    	
       	backfillGridlets();
    }
    
	/**
     * Cancels a job running or in the waiting queue. If a job is cancelled and 
     * the job was either running or was the first job in the waiting queue (i.e. pivot), 
     * then the availability profile is updated.<br>
     * <b>NOTE THAT:</b>
     * <ul>
     *    <li> This method firstly checks if the job has not finished. If the
     *    job has completed, it cannot be cancelled.
     *    <li> Once a job has been cancelled, it cannot be resumed to execute 
     *    again since this method will pass the job back to sender.
     *    <li> If a job cannot be found in either execution and waiting lists, 
     *    then a <code>null</code> job will be send back to sender.
     * </ul>
     * @param gridletId a job ID
     * @param userId the user or owner's ID of this job
     */
    @Override
    public synchronized void gridletCancel(int gridletId, int userId) {
    	double currentTime = GridSim.clock();
    	SSGridlet sgl = null;			
        boolean updateProfile = false;
        
        sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            if(sgl.getStatus() == Gridlet.SUCCESS ||
            		sgl.getActualFinishTime() <= currentTime){
            	super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, 
            			null, gridletId, userId);
            	return; // job has completed and cannot be cancelled
            } else {
            	runningJobs.remove(sgl);
            	updateProfile = true;
            }
        }

        if(sgl == null) {
        	sgl = waitingJobs.get(gridletId, userId);
        	if (sgl != null) {
                waitingJobs.remove(sgl);
                if(sgl == pivot) {
                	pivot = null;
                	updateProfile = true;
                }
            }
        }

        if(sgl == null) {
            logger.info("Cannot find Gridlet #" + gridletId + " for User #" + userId);
            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, 
            		null, gridletId, userId);
            return;
        }
        
        sgl.setStatus(Gridlet.CANCELED);

    	//----------------- USED FOR DEBUGGING PURPOSES ONLY -------------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_CANCELLED, true, sgl);
        //----------------------------------------------------------------------
       
        if(updateProfile) {
        	removeGridlet(sgl);
        	backfillGridlets();

        	//------------------- USED FOR DEBUGGING PURPOSES ONLY -----------------
       		// Inform the listeners about the new schedule
        	visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
        	//----------------------------------------------------------------------
        }

    	sgl.finalizeGridlet();
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, 
        		sgl.getGridlet(), gridletId, userId);
    }
    
    /**
     * Finds the status of a specified job.
     * @param gridletId a job ID
     * @param userId the user or owner's ID of this job
     * @return the job status or <code>-1</code> if not found
     */
    @Override
    public synchronized int gridletStatus(int gridletId,int userId) {
    	SSGridlet sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        sgl = waitingJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        return -1; // -1 = unknown
    }
    
	@Override
	public void gridletMove(int gridletId, int userId, int destId, boolean ack) {
		logger.warning("Operation not supported.");
	}

	@Override
	public void gridletPause(int gridletId, int userId, boolean ack) {
		logger.warning("Operation not supported.");
	}

	@Override
	public void gridletResume(int gridletId, int userId, boolean ack) {
		logger.warning("Operation not supported.");
	}
    
    /*
     * Tries to schedule a job. That is, make the job the pivot, or the first 
     * job in the queue. If that is not possible, then return false. 
     * @param sgl the job
     * @return <code>true</code> if scheduled; <code>false</code> otherwise.
     */
    private boolean scheduleGridlet(SSGridlet sgl) {
    	// skip if there is a pivot already
    	if(pivot != null) {
    		return false;
    	}
    	
    	int reqPE = sgl.getNumPE();
    	long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        
        ProfileEntry entry = profile.findStartTime(reqPE, runTime);
        PERangeList selected = entry.getAvailRanges().selectPEs(reqPE);

        double startTime = entry.getTime();
        double finishTime = startTime + runTime;
        sgl.setPERangeList(selected);
        profile.allocatePERanges(selected, startTime, finishTime);
        
       	sgl.setStatus(Gridlet.QUEUED);
        sgl.setStartTime(startTime);
        sgl.setActualFinishTime(finishTime);
        pivot = sgl;
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
        return true;
    }
	
    /*
     * This method finalises the jobs in execution whose completion time is 
     * smaller or equals to the current simulation time.
     * @return the number of jobs completed
     */
    private int finishRunningGridlets() {
    	int itemsFinished = 0;

   		Iterator<SSGridlet> iter = runningJobs.iterator();
   		while (iter.hasNext()) {
   			SSGridlet gridlet = iter.next();
    		if(gridlet.getActualFinishTime() <= GridSim.clock()) {
                gridletFinish(gridlet, Gridlet.SUCCESS);
                iter.remove();
                itemsFinished++;
		   	}
	    }
   		
	   	return itemsFinished;
    }
    
    /*
     * This method is called to update the schedule. It removes completed
     * jobs, returns them to the users and tries to backfill using waiting jobs.
     */
    private void updateSchedule() {
    	double currentTime = GridSim.clock();
    	int nFinished = finishRunningGridlets();
    	profile.removePastEntries(currentTime);
    	int nStarted = backfillGridlets();
    	
    	//---------------- USED FOR DEBUGGING PURPOSES ONLY --------------------
    	if(nStarted > 0 || nFinished > 0) {
    		visualizer.notifyListeners(super.get_id(),
    				ActionType.SCHEDULE_CHANGED, true);
       	}
    }
    
    /**
     * This method backfills/starts gridlets that are in the queue
     * @return the number of gridlets started
     */
    protected int backfillGridlets() {
    	int gridletStarted = 0;
    	
    	// checks the pivot first
	    if(pivot != null && pivot.getStartTime() <= GridSim.clock()) {
	    	gridletStarted++;
	    	waitingJobs.remove(pivot);  // pivots should be at the beginning anyway
	    	runningJobs.add(pivot);
	    	
    	    pivot.setStatus(Gridlet.INEXEC);
    	    super.sendInternalEvent(pivot.getActualFinishTime()-GridSim.clock(), UPT_SCHEDULE);
	    	pivot = null;
	    		
	        //-------------- FOR DEBUGGING PURPOSES ONLY  --------------
	        visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
	        //----------------------------------------------------------
    	}
    	
    	if(jobOrder != null) {
    		Collections.sort(waitingJobs, jobOrder);
    	}
    	    	
    	// Start the execution of jobs that are queued
    	Iterator<SSGridlet >iter = waitingJobs.iterator();
		while (iter.hasNext()) {
			SSGridlet gridlet = iter.next();
			
			// required to skip pivot
			if(gridlet == this.pivot) {
				continue;
			}
			
    		boolean success = startGridlet(gridlet);
    	    if(success) {
    	    	iter.remove();
        		gridletStarted++;
    	    } else { 
    	    	success = scheduleGridlet(gridlet);
    	    }

        	//-------------- FOR DEBUGGING PURPOSES ONLY  --------------
	    	if(success) {
            	visualizer.notifyListeners(super.get_id(), 
            			ActionType.ITEM_SCHEDULED, true, gridlet);
	    	}
            //----------------------------------------------------------
    	}
		
		// updates list of free resources available 
		// (hack to be compatible with GridSim's way of maintaining availability) 
		dynamics.resetFreePERanges(profile.checkImmediateAvailability().getAvailRanges());
    	return gridletStarted;
    }
    
    /*
     * Starts a job using free PEs, sets the job status to INEXEC and
     * updates the availability profile
     * @param sgl a SSGridlet object
     * @return <code>true</code> if there if the job has started
     */
    private boolean startGridlet(SSGridlet sgl) {
    	int reqPE = sgl.getNumPE();
        long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        
        double currentTime = GridSim.clock(); 
        double finishTime = currentTime + runTime;
        ProfileEntry entry = profile.checkAvailability(reqPE, currentTime, runTime);
        
        // if entry is null, there are not enough PEs to serve the job
        if(entry == null) {
        	return false;
        }
        
        PERangeList selected = entry.getAvailRanges().selectPEs(reqPE);
        profile.allocatePERanges(selected, currentTime, finishTime);
        
        runningJobs.add(sgl);
        sgl.setStartTime(currentTime);
        sgl.setActualFinishTime(finishTime);
        sgl.setStatus(Gridlet.INEXEC);
        sgl.setPERangeList(selected);
        
        super.sendInternalEvent(runTime, UPT_SCHEDULE);
        return true;
    }
    
    /**
     * Forecast finish time of a Gridlet.<br>
     * <tt>Finish time = length / available rating</tt>
     * @param availableRating   the shared MIPS rating for all Gridlets
     * @param length   remaining Gridlet length
     * @return Gridlet's finish time.
     * @pre availableRating >= 0.0
     * @pre length >= 0.0
     * @post $none
     */
    protected long forecastExecutionTime(double availableRating, double length) {
    	long runTime = (long)((length / availableRating) + 1); 
        return Math.max(runTime, 1);
    }
    
    /**
     * Updates the Gridlet's properties, such as status once a
     * Gridlet is considered finished.
     * @param sgl   a SSGridlet object
     * @param status   the Gridlet status
     */
    protected void gridletFinish(SSGridlet sgl, int status) {
        // the order is important! Set the status first then finalise
        // due to timing issues in SSGridlet class (Copied from GridSim)
        sgl.setStatus(status);
        sgl.finalizeGridlet();
        sendFinishGridlet( sgl.getGridlet() );
    }
    
    /*
     * Returns the time slot allocated to the job back to the profile.
     * @param grl the job to be removed
     */
    private void removeGridlet(SSGridlet grl) {
    	if(!grl.hasReserved()) {
    		profile.addTimeSlot(grl.getStartTime(), grl.getActualFinishTime(), grl.getPERangeList());
    	}
    }
}
