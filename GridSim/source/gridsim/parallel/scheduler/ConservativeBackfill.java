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
import gridsim.parallel.ParallelResource;
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
 * {@link ConservativeBackfill} class is an allocation policy for 
 * {@link ParallelResource} that implements conservative backfilling. 
 * The policy is based on the conservative backfilling algorithm 
 * described in the following papers:
 * <p>
 * <ul>
 * 		<li> Dror G. Feitelson and Ahuva Mu'alem Weil, Utilization and 
 * 		Predictability in Scheduling the IBM SP2 with Backfilling, in 
 * 		Proceedings of the 12th International Parallel Processing Symposium on 
 * 		International Parallel Processing Symposium (IPPS 1998), pp. 542-546.
 * 
 * 		<li>Ahuva W. Mu'alem and Dror G. Feitelson, Utilization, Predictability, 
 * 		Workloads, and User Runtime Estimates in Scheduling the IBM SP2
 * 		with Backfilling. IEEE Transactions on Parallel and Distributed 
 * 		Systems, 12:(6), pp. 529-543, 2001.
 * </ul>
 * 
 * This policy maintains an availability profile. The availability 
 * profile contains information about the ranges of processing elements
 * (PEs) available at future times. The size of the availability profile
 * is proportional to the number of jobs in the running and waiting
 * queues. 
 * <p>
 * To illustrate how the availability profile works, imagine that
 * the simulation has just started and the availability profile is empty.
 * The grid resource has 500 PEs. Therefore, the range of PEs available
 * is [0..499]. At simulation time 100, a job (<tt>Ja</tt>) arrives requiring
 * 100 PEs. The job is expected to execute for 500 seconds. As the
 * resource is not executing any jobs, <tt>Ja</tt> is accepted and given
 * the range [0..99]. The ranges of current PEs available is updated 
 * to [100..499] and one entry is inserted at the profile to indicate
 * that the range [0..499] will be available again at simulation time
 * 600 seconds. 
 * <p>
 * Now suppose that a job (<tt>Jb</tt>) arrives at simulation
 * time 200 requiring 400 PEs and expected to run for 500 seconds. 
 * The policy checks the ranges currently available and find [100..499]. 
 * It then scans the availability profile and analyses all the entries whose
 * time is smaller than the expected termination time of <tt>Jb</tt>. The policy
 * finds the intersections of PEs amongst the entries, process similar
 * to finding intersections of sequences. While scanning the profile, 
 * the policy finds the entry at 600 seconds. The intersection of 
 * [100..499] and [0..499] is [100..499]. That means that there are
 * enough resources to schedule <tt>Jb</tt> and then <tt>Jb</tt> starts 
 * the execution. The policy then updates the ranges of current PEs to [].
 * After that, the policy updates the entry at 600 seconds to [0..99]
 * and inserts an entry with time 700 seconds with the range [0..499].
 * <p>
 * Now consider that a third job (<tt>Jc</tt>) arrives at simulation time 250
 * requiring 500 PEs and expected to run for 100 seconds. As the 
 * current ranges available is [], the policy scans the profile until 
 * it finds an entry with enough PEs available. In this case, the entry 
 * found is at 700 seconds. The policy would continue scanning the profile
 * finding the intersection with other ranges if there were more. In this
 * case, 700 seconds is the last entry in the profile. The job is
 * then set to start execution at time 700 seconds. The policy then 
 * updates the entry at time 700 seconds to [] and creates an entry
 * at 800 with [0..499]. <tt>Jc</tt> is then put in the waiting queue.
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines comprising this resource must be 
 * 			 homogeneous.
 *		<li> Local load is not considered. If you would like to simulate this, 
 * 			 you have to model the local load as jobs; it is more precise 
 * 			 and faster. To do so, please check {@link WorkloadLublin99}.
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

public class ConservativeBackfill extends AllocPolicy {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
    protected SSGridletList waitingJobs = new SSGridletList();  
    protected SSGridletList runningJobs = new SSGridletList();  	
    protected SingleProfile profile;			 	 // availability of PEs			
	protected int ratingPE = 0;					 	 // The rating of one PE
	protected Comparator<SSGridlet> jobOrder = null; // sorts the jobs for backfilling
	protected ResourceDynamics dynamics = null;

	// To update the schedule when required
	protected static final int UPT_SCHEDULE = 10;
	private double lastSchedUpt = 0.0D;				 // time of last schedule update
	private Visualizer visualizer = null;
    
	/**
     * Allocates a new {@link ConservativeBackfill} object.
     * @param resourceName the grid resource entity name that will contain 
     * 		   this allocation policy
     * @param entityName this object entity name
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     *  <li> Creating this entity before initialising GridSim package
     *  <li> The entity name is <code>null</code> or empty
     * </ul>
     */
    public ConservativeBackfill(String resourceName, String entityName) 
    					throws Exception {
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
    
    /**
     * Schedules a new job received by the Grid resource entity.
     * @param gridlet a Gridlet object to be executed
     * @param ack an acknowledgement, i.e. <code>true</code> if wanted to know
     * whether this operation is successful or not; 
     * <code>false</code> otherwise (don't care)
     */
    public synchronized void gridletSubmit(Gridlet gridlet, boolean ack) {
    	int reqPE = gridlet.getNumPE();
    	
    	try {
	    	// reject the Gridlet if it requires more PEs than the maximum
	    	if(reqPE > super.totalPE_) {
	    		String userName = GridSim.getEntityName( gridlet.getUserID() );
	    		logger.warning("Gridlet #" + gridlet.getGridletID() + " from " +
    	                userName + " user requires " + gridlet.getNumPE() + " PEs." +
	    	            "\n--> The resource has only " + super.totalPE_ + " PEs.");
	    		gridlet.setGridletStatus(Gridlet.FAILED);
	    		super.sendFinishGridlet(gridlet);
	    		return;
	    	}
    	} catch(Exception ex) {
    		logger.log(Level.WARNING, "Exception on submission of a Gridlet", ex);
    	}
    	
    	// Creates a server side job
    	SSGridlet sgl = new SSGridlet(gridlet);  
    	
    	//-------------- FOR DEBUGGING PURPOSES ONLY  --------------
    	visualizer.notifyListeners(super.get_id(), ActionType.ITEM_ARRIVED, true, sgl);
        //----------------------------------------------------------
    	
        // if job cannot be scheduled immediately, then enqueue it
        if(!startGridlet(sgl)) {
        	enqueueGridlet(sgl);
            waitingJobs.add(sgl);
        }
        
        if (ack) {
            // sends back an ack
        	super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
        			gridlet.getGridletID(), gridlet.getUserID());
        }
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
    }
    
    /**
     * Finds the status of a specified job
     * @param gridletId a job ID
     * @param userId the user or owner's ID of this job
     * @return the job status or <code>-1</code> if not found
     */
    public synchronized int gridletStatus(int gridletId,int userId) {
    	SSGridlet sgl = null;

        // Look for the Gridlet in the running queue
        sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        // Look for the gridlet in the waiting queue
        sgl = waitingJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        return -1; // -1 = unknown
    }
    
	/**
     * Cancels a job running or in the waiting queue. This method will search the 
     * running and waiting queues. If a job is cancelled, the availability 
     * profile is shifted forwards. This process ensures that the job will not 
     * have an expected completion time worse than that initially stipulated. 
     * This process if known as the compression of the schedule.<br>
     * <b>NOTE:</b>
     * <ul>
     *    <li> This method firstly checks if the job has not finished. If the
     *    job has completed, it cannot be cancelled.
     *    <li> Once a job has been cancelled, it cannot be resumed to execute 
     *    again since this method will pass the job back to sender.
     *    <li> If a job cannot be found in the queues, then a <code>null</code>
     *    job will be send back to sender.
     *    <li> After the job is cancelled, the jobs are moved forwards in the 
     *    schedule. The entries in the profile incurred by each job are updated. 
     *    This process consists in removing a job, returning its time slot to the
     *    profile and finding the schedule for the job again.
     * </ul>
     * @param gridletId a job ID
     * @param userId the user or owner's ID of this job
     */
    public synchronized void gridletCancel(int gridletId, int userId) {
    	SSGridlet sgl = null;			
        
        // The jobs whose execution time is larger than this time have to be 
    	// shifted forwards. There's no point in changing jobs that are
    	// scheduled to start before this time.
        double refTime = GridSim.clock();

        sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            if(sgl.getStatus() == Gridlet.SUCCESS ||
            		sgl.getActualFinishTime() <= refTime) {
            	super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, 
            			null, gridletId, userId);
            	return; // cannot cancel the job
            } else {
            	runningJobs.remove(sgl);
            }
        }

        if(sgl == null) {
        	sgl = waitingJobs.get(gridletId, userId);
        	if (sgl != null) {
                waitingJobs.remove(sgl);
                refTime = sgl.getStartTime();
            }
        }

        // Job has not been found
        if(sgl == null) {
        	logger.info("Cannot find Gridlet #" + gridletId + " for User #" + userId);
            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, null, gridletId, userId);
            return;
        }

        sgl.setStatus(Gridlet.CANCELED);
        
    	//----------------- USED FOR DEBUGGING PURPOSES ONLY -------------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_CANCELLED, true, sgl);
        //----------------------------------------------------------------------
       
    	if(!sgl.hasReserved()) {
            removeGridlet(sgl);
    		compressSchedule(refTime, sgl.getStatus() == Gridlet.INEXEC);
    	}
        
    	//------------------- USED FOR DEBUGGING PURPOSES ONLY -----------------
    	visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
    	//----------------------------------------------------------------------
	
        // finalise and send the Gridlet back to user
    	sgl.finalizeGridlet();
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, sgl.getGridlet(), gridletId, userId);
    }
    
	@Override
	public void gridletMove(int gridletId, int userId, int destId, boolean ack) {
		logger.warning("Operation not supported");
	}

	@Override
	public void gridletPause(int gridletId, int userId, boolean ack) {
		logger.warning("Operation not supported");
	}

	@Override
	public void gridletResume(int gridletId, int userId, boolean ack) {
		logger.warning("Operation not supported");
	}

    /**
     * This method performs the compression of the schedule. The method iterates 
     * the waiting jobs list. For each job, it removes its entry from the profile
     * and then tries to reinsert the job in the profile. In the worst case, the 
     * job will be put back in the same place. This process ensures no job has
 	 * a worse start time than that initially given.
     * @param refTime jobs whose start time is larger than refTime may be shifted
     * @param execute <code>true</code> means that the job cancelled was running, 
     * so this method will try to start waiting jobs. If not possible they are 
     * reinserted in the waiting queue with the new start time.
     * @return <code>true</code> if the job has been updated.
     */
    protected boolean compressSchedule(double refTime, boolean execute) {
    	if(jobOrder != null) {
    		Collections.sort(waitingJobs, jobOrder);
    	}
    	
    	Iterator<SSGridlet> iterQueue = waitingJobs.iterator();
        while(iterQueue.hasNext()) {
        	SSGridlet queuedSgl = iterQueue.next();
        	
        	// Skip as it cannot get better than this.
        	if(queuedSgl.getStartTime() <= refTime) {
        		continue;
        	}

        	// jobs with reservation cannot be moved
        	if(!queuedSgl.hasReserved()) {
        		profile.addTimeSlot(queuedSgl.getStartTime(), 
        				queuedSgl.getActualFinishTime(), queuedSgl.getPERangeList());
        	
	        	if(execute && startGridlet(queuedSgl)) {
	        		iterQueue.remove();
	        	}
	        	else {
	        		enqueueGridlet(queuedSgl);
	        	}
        	}
        }
        
        return true;
    }
    
    /**
     * This method finalises the jobs that have completed
     * @return the number of jobs completed
     */
    protected int finishRunningGridlets() {
    	double now = GridSim.clock();
    	int itemsFinished = 0;
   		Iterator<SSGridlet> iter = runningJobs.iterator();
   		while (iter.hasNext()) {
   			SSGridlet gridlet = iter.next();
    		if(gridlet.getActualFinishTime() <= now) {
                gridletFinish(gridlet, Gridlet.SUCCESS);
                iter.remove();
                itemsFinished++;
		   	}
	    }
   		
	   	return itemsFinished;
    }
    
    /**
     * This method starts jobs that are in the queue and can be started
     * @return the number of jobs started
     */
    protected int startQueuedGridlets() {
    	int gridletStarted = 0;
    	double now = GridSim.clock();
    	
    	Iterator<SSGridlet> iter = waitingJobs.iterator();
		while (iter.hasNext()) {
			SSGridlet gridlet = iter.next();
    		if(gridlet.getStartTime() <= now) {
    	        gridlet.setStatus(Gridlet.INEXEC);
    			runningJobs.add(gridlet);
    			iter.remove();
    			gridletStarted++;
    			
    	        super.sendInternalEvent(gridlet.getActualFinishTime()-now, 
    	        		UPT_SCHEDULE);
    		}
    	}

    	return gridletStarted;
    }
    
    /**
     * This method is called to update the schedule. It removes completed
     * jobs and returns them to the users and verifies whether there are 
     * jobs in the waiting list that should start execution.
     */
    protected void updateSchedule() {
    	int itemsFinished = finishRunningGridlets();
    	profile.removePastEntries(GridSim.clock());
    	int itemsStarted = startQueuedGridlets();
    	
    	// updates list of free resources available 
		// (hack to be compatible with GridSim's way of maintaining availability)
		dynamics.resetFreePERanges(profile.checkImmediateAvailability().getAvailRanges());
    	
    	//---------------- USED FOR DEBUGGING PURPOSES ONLY --------------------
   		if(itemsStarted > 0 || itemsFinished > 0){
   			visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
   		}
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
				    // updates the schedule, finishes jobs, reservations, etc.
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
     * Allocates a job into free PEs, sets the job status to INEXEC,
     * @param sgl a SSGridlet object
     * @return <code>true</code> if there is are free PE to process this job,
     *         <code>false</code> otherwise
     */
    protected boolean startGridlet(SSGridlet sgl) {
    	int reqPE = sgl.getNumPE();
        long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        double currentTime = GridSim.clock() ;
        double finishTime = currentTime + runTime;

        ProfileEntry entry = profile.checkAvailability(reqPE, currentTime, runTime);
        
        // if entry is null, there are not enough PEs to serve he job
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
        
        // sends an internal event to handle the job completion
        super.sendInternalEvent(runTime, UPT_SCHEDULE);
        
		// updates list of free resources available 
		// (hack to be compatible with GridSim's way of maintaining availability)
        dynamics.resetFreePERanges(profile.checkImmediateAvailability().getAvailRanges());
		return true;
    }
    
    /**
     * Enqueues a gridlet. That is, finds the anchor point, which is the point
     * in the availability profile where there are enough processors to execute it.
     * @param sgl the resource gridlet
     */
    protected void enqueueGridlet(SSGridlet sgl){
    	int reqPE = sgl.getNumPE();
        long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        ProfileEntry entry = profile.findStartTime(reqPE, runTime);
        
        PERangeList selected = entry.getAvailRanges().selectPEs(reqPE);
        double startTime = entry.getTime();
        double finishTime = startTime + runTime;
        
        profile.allocatePERanges(selected, startTime, finishTime);
        sgl.setPERangeList(selected);
       	sgl.setStatus(Gridlet.QUEUED);
        sgl.setStartTime(startTime);
        sgl.setActualFinishTime(finishTime);
    }

    /**
     * Forecast finish time of a Gridlet.
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
     * Returns the time slot given to the job back to the profile
     * @param grl the Gridlet to be removed
     */
    private void removeGridlet(SSGridlet grl) {
    	if(!grl.hasReserved()) {
    		profile.addTimeSlot(grl.getStartTime(), grl.getActualFinishTime(), grl.getPERangeList());
    	}
    }
}
