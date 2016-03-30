/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.scheduler;

import java.util.ArrayList;
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
import gridsim.parallel.profile.PartitionPredicate;
import gridsim.parallel.profile.ProfileEntry;
import gridsim.parallel.profile.ScheduleItem;
import gridsim.parallel.profile.SingleProfile;
import gridsim.parallel.util.WorkloadLublin99;

/**
 * This scheduling strategy is the implementation of the backfilling based
 * scheduler describe in the following paper:
 * <p>
 * <ul>
 * 		<li>Srividya Srinivasan, Rajkumar Kettimuthu, Vijay Subramani, and
 *      Ponnuswamy Sadayappan. Selective Reservation Strategies for Backfill
 *      Job Scheduling. In JSSPP 2002, LNCS 2537, Springer-Verlag Berlin Heidelberg,
 *      pp. 55-71, 2002.
 * </ul>
 * According to this scheduling strategy, jobs are not given a reservation
 * (i.e. their start and finish times are not defined or they are not used for
 * backfilling) until their expected slowdown exceeds some threshold, whereupon
 * they get a reservation. In other words, if a job waits enough, then it is 
 * given a reservation. This is done when the eXpansion Factor (XFactor) of
 * the job exceeds some starvation threshold. The XFactor of a job is defined as:
 * <pre>
 * 		XFactor = (Wait time + Estimated run time) / Estimated run time
 * </pre>
 * The XFactor threshold is initially set to <code>1.0</code> and as jobs 
 * complete, it is updated to the average slowdown of the completed jobs. 
 * Alternatively, you can create job categories, each category will have its
 * own starvation threshold. See {@link #addJobCategory(PartitionPredicate)} 
 * <p>
 * This policy maintains an availability profile. The availability 
 * profile contains information about the ranges of processing elements
 * (PEs) that will be released when the running jobs complete.
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines comprising this resource must be homogeneous.
 * 		<li> Local load is not considered. If you would like to 
 * 			 simulate this, you have to model the local load as jobs. 
 * 			 It is more precise and faster. To do so, please check 
 * 			 {@link WorkloadLublin99}.
 *      <li> Jobs cannot be paused nor migrated.
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
public class SelectiveBackfill extends AllocPolicy {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
    protected SSGridletList waitingJobs = new SSGridletList();  
    protected SSGridletList runningJobs = new SSGridletList();  	
    protected SingleProfile profile;			 	 // availability of PEs			
	protected int ratingPE = 0;					 	 // The rating of one PE
	protected Comparator<SSGridlet> jobOrder = null; // sorts the jobs for backfilling
	protected ResourceDynamics dynamics = null;
	protected ArrayList<JobCategory> categories = new ArrayList<JobCategory>();
	
	// To update the schedule when required
	protected static final int UPT_SCHEDULE = 10;
	private double lastSchedUpt = 0.0D;				 // time of last schedule update
	private Visualizer visualizer = null;

	/**
     * Allocates a new {@link SelectiveBackfill} object
     * 
     * @param resName the name of the Grid resource that will contain this scheduler.
     * @param entityName  this object entity name
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     *  <li> Creating this entity before initialising the simulator
     *  <li> The entity name is <code>null</code> or empty
     *  <li> The resource has <code>zero</code> number of PEs (Processing
     *       Elements). <br>
     * </ul>
     */
	public SelectiveBackfill(String resName, String entityName)
			throws Exception {
		super(resName, entityName);
	}
	
	/**
	 * Adds a job category to use to calculate the starvation threshold. 
	 * Different job categories have different starvation thresholds.
	 * @param predicate the job category. If jobs match this predicate, then
	 * they fall into this category.
	 */
	public void addJobCategory(PartitionPredicate predicate) {
		categories.add(new JobCategory(predicate));
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
        
        // Creates a general category if none was specified
        if(categories.size() == 0) {
        	this.addJobCategory(new PartitionPredicate() {
        		public boolean match(ScheduleItem item) { return true; }
        	});
        }

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
    		logger.log(Level.WARNING, "Exception on job submission", ex);
    	}
    	
    	SSGridlet sgl = new SSGridlet(gridlet);  // Create a server side Gridlet
    	
    	for (JobCategory category : categories) {
    		if(category.predicate.match(sgl)) {
    			sgl.setPartitionID(categories.indexOf(category));
    		}
    	}
    	
    	//-------------- FOR DEBUGGING PURPOSES ONLY  --------------
    	visualizer.notifyListeners(super.get_id(), ActionType.ITEM_ARRIVED, true, sgl);
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

	@Override
	public void gridletCancel(int gridletId, int userId) {
		logger.info("Operation not supported");
	}
	
	@Override
	public void gridletMove(int gridletId, int userId, int destId, boolean ack) {
		logger.info("Operation not supported");
	}

	@Override
	public void gridletPause(int gridletId, int userId, boolean ack) {
		logger.info("Operation not supported");
	}

	@Override
	public void gridletResume(int gridletId, int userId, boolean ack) {
		logger.info("Operation not supported");
	}

    /**
     * Finds the status of a specified job.
     * @param gridletId a job ID
     * @param userId the user or owner's ID of this job
     * @return the job status or <code>-1</code> if not found
     */
	@Override
	public synchronized int gridletStatus(int gridletId, int userId) {

		// Look for the job in the running queue
		SSGridlet sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        // Look for the job in the waiting queue
        sgl = waitingJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }
        
        return -1;
	}

    /*
     * Process and event sent to this entity 
     * @param ev the event to be handled
     */
    private synchronized void processEvent(Sim_event ev) {
    	double currentTime = GridSim.clock();
    	boolean success = false;

    	if(ev.get_src() == myId_) {
		    // time to update the schedule, remove finished jobs, 
		   	// removed finished reservations, start reservations, etc.
		   	if (ev.get_tag() == UPT_SCHEDULE) {
		   		if(currentTime > lastSchedUpt) {
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
     * This method backfills/starts jobs that are in the queue
     * @return the number of jobs started
     */
    protected int backfillGridlets() {
    	int nStarted = 0;
    	
    	if(jobOrder != null) {
    		Collections.sort(waitingJobs, jobOrder);
    	}
    	
    	Iterator<SSGridlet> it = waitingJobs.iterator();
    	while(it.hasNext()) {
    		SSGridlet gl = it.next();
    		if(gl.getStartTime() > 0) {
    			continue;
    		}
    		
    		if(startGridlet(gl)) {
    			nStarted++;
    			it.remove();
    		} else {
    			double avSlowdown = this.getXFactorThreshold(gl); 
    			if(getXFactor(gl) > avSlowdown) {
    				scheduleGridlet(gl);
    			}
    		}
    	}
    	
    	return nStarted;
    }
    
    /*
     * This method starts jobs that are in the queue and whose start time is 
     * smaller than the reference time
     * @return the number of gridlets started
     */
    private int startQueuedGridlets() {
    	int nStarted = 0;
    	
    	Iterator<SSGridlet> iter = waitingJobs.iterator();
		while (iter.hasNext()) {
			SSGridlet gridlet = iter.next();
    		if(gridlet.getStartTime() >= 0 && gridlet.getStartTime() <= GridSim.clock()) {
    			runningJobs.add(gridlet);
    			iter.remove();
    			nStarted++;
    	        gridlet.setStatus(Gridlet.INEXEC);
    	        // schedule an update-scheduling-queue event
    	        super.sendInternalEvent(gridlet.getActualFinishTime()-GridSim.clock(), UPT_SCHEDULE);
    		}
    	}

    	return nStarted;
    }
    
    /**
     * Starts a job using free PEs, sets the job status to INEXEC and
     * updates the availability profile
     * @param sgl a SSGridlet object
     * @return <code>true</code> if there if the job has started; 
     * <code>false</code> otherwise
     */
    protected boolean startGridlet(SSGridlet sgl) {
        double currentTime = GridSim.clock() ; 
    	
        // calculate the execution time of the job
        long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        double finishTime = currentTime + runTime;
        
        ProfileEntry entry = profile.checkAvailability(sgl.getNumPE(), currentTime, runTime);
        
        // if entry is null, there are not enough PEs to serve the job
        if(entry == null) {
        	return false;
        }
        
        // selects PEs and updates the availability profile
        PERangeList selected = entry.getAvailRanges().selectPEs(sgl.getNumPE());
        profile.allocatePERanges(selected, currentTime, finishTime);
        
        runningJobs.add(sgl);
        sgl.setStartTime(currentTime);
        sgl.setActualFinishTime(finishTime);
        sgl.setStatus(Gridlet.INEXEC);
        sgl.setPERangeList(selected);
        
        // schedules event to itself to update queues after the job's completion
        super.sendInternalEvent(runTime, UPT_SCHEDULE);
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------

        return true;
    }
    
    /**
     * Enqueues a job. That is, finds the anchor point, which is the point
     * in the availability profile where there are enough processors to execute
     * the job.
     * @param sgl the job to be scheduled
     */
    protected void scheduleGridlet(SSGridlet sgl){
    	int reqPE = sgl.getNumPE();
    	long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
        
        // Gets the information about when the job could start
        ProfileEntry entry = profile.findStartTime(reqPE, runTime);
        
        PERangeList selected = entry.getAvailRanges().selectPEs(reqPE);
        double startTime = entry.getTime();
        double finishTime = startTime + runTime;
        sgl.setPERangeList(selected);
        profile.allocatePERanges(selected, startTime, finishTime);
        
        // updates the job's information
       	sgl.setStatus(Gridlet.QUEUED);
        sgl.setStartTime(startTime);
        sgl.setActualFinishTime(finishTime);
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(super.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
    }
    
    /*
     * This method is called to update the schedule. It removes completed
     * jobs and returns them to the users and verifies whether new jobs can
     * start or be used to backfill.
     */
    private void updateSchedule() {
    	int nFinished = finishRunningGridlets();
    	int nStarted = startQueuedGridlets();
    	profile.removePastEntries(GridSim.clock());
    	nStarted += backfillGridlets();
    	
    	//---------------- USED FOR DEBUGGING PURPOSES ONLY --------------------
      	// If a gridlet has started execution or one has finished,
   		// then inform the listeners
    	if(nStarted > 0 || nFinished > 0) {
    		visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
       	}
    }

    /*
     * This method finalises the jobs in execution whose completion time is 
     * smaller or equals to the current simulation time.
     * @return the number of gridlets completed
     */
    private int finishRunningGridlets() {
    	int nFinished = 0;

   		Iterator<SSGridlet> iter = runningJobs.iterator();
   		while (iter.hasNext()) {
   			SSGridlet gridlet = iter.next();
    		if(gridlet.getActualFinishTime() <= GridSim.clock()) {
                gridletFinish(gridlet, Gridlet.SUCCESS);
                
                // updates the average job slowdown, used as the starvation threshold
                double runTime = Math.max(gridlet.getActualFinishTime() - gridlet.getStartTime(), 1.0D);
                double slowdown = gridlet.getGridlet().getWallClockTime() / runTime;
                
                JobCategory cat = categories.get(gridlet.getPartitionID());                
                cat.numCompJobs++;
                cat.sumSlowdown += slowdown;
                
                iter.remove();
                nFinished++;
		   	}
	    }
   		
	   	return nFinished;
    }

    /**
     * Calculates the eXpansion Factor (XFactor) of a job. The expansion factor
     * guarantees that the jobs do not exceed some starvation threshold.
     * @param sgl the job to be examined.
     * @return the XFactor of the job.
     */
    protected double getXFactor(SSGridlet sgl) {
    	double waitTime = GridSim.clock() - sgl.getSubmissionTime();
    	double runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
    	return (waitTime + runTime) / runTime; 
    }
    
    /**
     * Gets the eXpansion Factor (XFactor) threshold applicable for a particular job 
     * @param sgl the job to be examined
     * @return the XFactor of the job
     */
    protected double getXFactorThreshold(SSGridlet sgl) {
    	// considers the average slowdown of a job category as the threshold
    	JobCategory cat = categories.get(sgl.getPartitionID());
    	return Math.max(1.0D, cat.sumSlowdown / cat.numCompJobs);
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
     * Defines a job category.
     */
    class JobCategory {
    	PartitionPredicate predicate = null;
    	int numCompJobs = 0;
    	double sumSlowdown = 0.0D;
 
    	JobCategory(PartitionPredicate predicate) {
    		this.predicate = predicate;
    	}
    	
    	boolean match(SSGridlet job) {
    		return predicate.match(job);
    	}
    	
    	public String toString() {
    		return "Job Category, n. jobs completed: " + numCompJobs + 
    		", sum slowdown: " + sumSlowdown;
    	}
    }
}
