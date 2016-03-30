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
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.PartProfile;
import gridsim.parallel.profile.PartitionPredicate;
import gridsim.parallel.profile.ProfileEntry;
import gridsim.parallel.profile.ResourcePartition;
import gridsim.parallel.profile.ScheduleItem;
import gridsim.parallel.util.WorkloadLublin99;


/**
 * This class implements a non-FCFS policy to schedule parallel jobs. The
 * policy is based on aggressive (EASY) backfilling. This policy can use
 * multiple partitions or queues and the jobs can be directed to these partitions
 * using a partition predicate ({@link PartitionPredicate}. A partition
 * can borrow resources from another when it requires and the resources are
 * not being used by the other partition. However, you can change this behaviour
 * by calling {@link #setAllowBorrowing(boolean)}. Additionally, this policy
 * supports priorities. Jobs are ordered according to their priorities; 
 * a high priority job can take the place of a pivot with lower priority. 
 * To change the way that the scheduler assigns priorities to the jobs, please
 * see {@link PrioritySelector}. The implementation of this policy is based 
 * on the following paper:
 * <p>
 * <ul>
 * 		<li> Barry G. Lawson and Evgenia Smirni, Multiple-Queue Backfilling 
 * 		Scheduling with Priorities and Reservations for Parallel Systems, 
 * 		2002 Workshop on Job Scheduling Strategies for Parallel 
 * 		Processing (JSSPP), pp. 72-87, 2002.
 * </ul>
 * <br>
 * We use an availability profile to store the availability of processing 
 * elements. In order to represent the pivots (i.e. the first jobs in the 
 * partitions), we schedule them creating the entries in the availability
 * profile. This way, we do not need to store the pivots' start times 
 * (or shadow times) and extra nodes in different variables. It also makes
 * the search for available resources for a new pivot easier. 
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines must be homogeneous.
 *		<li> Local load is not considered. If you would like to simulate this, 
 * 			 you have to model the local load as jobs. It is more precise 
 * 			 and faster. To do so, please check {@link WorkloadLublin99}.
 *      <li> Gridlets cannot be paused or migrated. 
 *      <li> This policy does not support advance reservations. 
 *  </ul>
 *    
 * @author  Marcos Dias de Assuncao
 * @since   5.0
 * 
 * @see gridsim.AllocPolicy
 * @see ParallelResource
 */

public class AggressiveMultiPartitions extends AllocPolicy {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
    protected SSGridletList waitingJobs = new SSGridletList();  
    protected SSGridletList runningJobs = new SSGridletList();  	
    protected int ratingPE = 0; 		// The rating of one PE    
	protected PartProfile profile;		// The availability profile
	
	// sorts the jobs for backfilling
	protected Comparator<SSGridlet> jobOrder = null; 
	
	protected ResourceDynamics dynamics = null;
	
	// used by the scheduler to specify the priority of the item
	protected PrioritySelector prioritySelector;
	
	// indicates whether the borrowing of resources by the 
	// partitions is allowed or not
	protected boolean allowBorrowing = true;
	
	// indicates whether a job should be returned to the user immediately
	// if it could not be scheduled to its corresponding partition and the 
	// borrowing of resources from other partitions is disabled. That is, if
	// a job arrives to the resource, the partition does not have enough
	// resources, borrowing is disabled and this attribute is true, the job
	// will be returned to the user entity. A user may want to change this 
	// behaviour if he wants to implement manual borrowing or resizing 
	// of the resource partitions
	protected boolean returnJob = true;
	
	// an array list to store the resource partitions
	private ResourcePartition[] partitions;
	
	// To update the schedule when required
	protected static final int UPT_SCHEDULE = 10;
	private double lastSchedUpt = 0.0D;		// time of last schedule update
	private Visualizer visualizer = null;
	
    /**
     * Allocates a new object. 
     * If the policy is create with only one partition, it will then work as
     * a normal aggressive (EASY) backfilling scheduler.
     * 
     * @param resourceName    the resource entity name that will 
     *                        contain this allocation policy
     * @param entityName      this object entity name
	 * @param numPartitions The number of partitions of the scheduling queue
	 * @throws IllegalArgumentException if number of partitions is <= 0
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
    public AggressiveMultiPartitions(String resourceName, 
    		String entityName, int numPartitions) throws Exception {
        super(resourceName, entityName);

        if(numPartitions <= 0) {
        	throw new IllegalArgumentException("Number of partitions " +
        			"should be larger than 1");
        }
        
        jobOrder = new OrderGridletsByPriority();
        partitions =  new ResourcePartition[numPartitions];
        prioritySelector = null;
    }
    
    /**
     * Creates a new partition in this scheduler.
     * @param partId the id of the partition
     * @param numPE the number of PEs in this partition
     * @param predicate the predicate to be used to select 
     * jobs for this partition.
     * @return <code>true</code> if the partition has been set 
     * correctly; <code>false</code> otherwise.
     * @throws IllegalArgumentException if the number of PEs is 
     * 				<code><= 0</code> or you provide an invalid predicate.
     * @throws IndexOutOfBoundsException if you provide an invalid partition id
     */
    public boolean createPartition(int partId, int numPE, 
    		PartitionPredicate predicate) {
    	
		if(partId >= partitions.length || partId < 0) {
			throw new IndexOutOfBoundsException("It is not possible to " +
					"add a partition with index: " + partId + ".");
		}
		else if(numPE < 0) {
			throw new IllegalArgumentException("Number of PEs should be > 0");
    	}
    	else if(predicate == null) {
    		throw new IllegalArgumentException("Invalid predicate");
    	}
    	
    	EasyBackFillingPartition partition = 
    		new EasyBackFillingPartition(partId, numPE, predicate);
    	
    	partitions[partId] = partition;
    	return true;
    }
    
    /**
     * Sets the heuristic used to order the jobs considered for backfilling
     * @param comparator a comparator implementation that defines 
     * how jobs are ordered.
     * @return <code>true</code> if the heuristic was set correctly; 
     * 		<code>false</code> otherwise.
     */
    public boolean setJobOrderingHeuristic(Comparator<SSGridlet> comparator) {
    	if(comparator == null) {
    		return false;
    	}
    	
    	jobOrder = comparator;
    	return true;
    }
    
    /**
     * Indicates whether the borrowing of resources of the partitions from
     * one another is allowed or not.
     * @param allow <code>true</code> indicates that it is allowed; 
     * <code>false</code> otherwise.
     */
    public void setAllowBorrowing(boolean allow) {
    	allowBorrowing = allow;
    }
    
    /**
     * This method is called to specify the behaviour of the policy if a job
     * cannot be scheduled to its partition due to the lack of enough resources
     * and the borrowing is disabled. If a <code>true</code> value is passed to this
     * method, it indicates that a job should be returned to the user 
     * immediately if it could not be scheduled to its corresponding partition 
     * and the borrowing of resources from other partitions is disabled. That is, 
     * if a job arrives at the resource, the partition does not have enough
     * resources, borrowing is disabled and the returnGridlet behaviour is true, 
     * the job will be returned to the user entity. This prevents the policy
     * from queueing job even if it will never be able to serve them. 
     * A user may want to change this behaviour if he wants to implement manual 
     * borrowing or resizing of the resource partitions. In such a case, the
     * job will be queued even if they cannot be handled by its partition
     * at the time of arrival.
     * @param returnJob <code>true</code> indicates that the job should be
     * returned; <code>false</code> otherwise. 
     */
    public void setReturnGridletBehaviour(boolean returnJob) {
    	this.returnJob = returnJob;
    }
    
    /**
     * Sets the priority selector to be used by this scheduler. The selector
     * defines what priorities the scheduler should assign to the jobs.
     * @param selector the selector to be used.
     * @return <code>true</code> if the selector has been defined successfully or
     * <code>false</code> otherwise.
     */
    public boolean setPrioritySelector(PrioritySelector selector) {
    	if(selector == null || Sim_system.running()) {
    		return false;
    	}
    	
    	prioritySelector = selector;
    	return true;
    }
    
    /**
     * Handles internal events that come to this entity.
     */
    public void body() {
    	//get the visualiser
        visualizer = GridSim.getVisualizer();

    	// get the resource characteristics object to be used
    	dynamics = (ResourceDynamics)super.resource_;
    	
       	// Gets the information on number of PEs and rating 
    	// of one PE assuming that the machines are homogeneous
        ratingPE = dynamics.getMIPSRatingOfOnePE();
        
        int assignedPEs = 0;
        int specified = 0;
        for(ResourcePartition partition : partitions) {
        	if(partition != null) {
        		assignedPEs += partition.getInitialNumPEs();
        		specified++;
        	}
        }
        
        if(partitions.length > 1 && partitions.length != specified) {
        	logger.severe(super.get_name() + " is expected to have " + 
        			partitions.length + " partitions. However," +
        			" only " + specified + " have been defined");
        	return;
        }
    	else if (partitions.length > 1 && assignedPEs != dynamics.getNumPE()) {
    		logger.severe(super.get_name() + " has been assigned a number of PEs " +
    				"for partitions different from that available on the resource.");
    		return;
    	}
        
        // if the user has not specified the partitions and the scheduler is 
        // expected to have only one partition. So, it creates the partition and
        // assigns an accept-all predicate to it.
        if(specified == 0 && partitions.length == 1) {
    		PartitionPredicate predicate = new PartitionPredicate() {
				public boolean match(ScheduleItem gridlet) {
					return true;
				}
    		};
       		
    		EasyBackFillingPartition part = 
    			new EasyBackFillingPartition(0, dynamics.getNumPE(), predicate);
    		
    		partitions[0] = part;
        }
        
		profile = new PartProfile(partitions);

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
        
        for(ResourcePartition partition : partitions) {
        	((EasyBackFillingPartition)partition).pivot = null;
    	}
    }
        
    /*
     * Process and event sent to this entity 
     * @param ev the event to be handled
     */
    private synchronized void processEvent(Sim_event ev) {
    	double now = GridSim.clock();

    	if(ev.get_src() == myId_) {
		    // time to update the schedule, finish jobs 
		   	// finish reservations, start jobs
		   	if (ev.get_tag() == UPT_SCHEDULE) {
		   		if(now > lastSchedUpt) { 
		   			updateSchedule();
	    		}
		   		lastSchedUpt = now;
	    	} else {
		    	processOtherEvent(ev);
		    }
		} else { 
    		processOtherEvent(ev);
		}
    }
    
    /**
     * Schedules/adds to the queue a new job received by the resource entity.
     * @param gridlet a job object to be executed
     * @param ack an acknowledgement, i.e. <code>true</code> if wanted to know
     * whether this operation is successful or not; 
     * <code>false</code> otherwise (don't care)
     */
    @Override
    public synchronized void gridletSubmit(Gridlet gridlet, boolean ack) {
    	// Create a server side Gridlet
    	SSGridlet sgl = new SSGridlet(gridlet);
    	if(!validateGridlet(sgl)) {
        	try {
        		// reject the Gridlet 
	    		gridlet.setGridletStatus(Gridlet.FAILED);
	    		super.sendFinishGridlet(gridlet);
	    		return;
	    	}
        	catch(Exception ex) {
        		logger.log(Level.WARNING, "Error on job submission", ex);
        		return;
        	}
    	}
    	
    	if(prioritySelector != null) {
    		sgl.setPriority(prioritySelector.getSchedulePriority(sgl));
    	}
    	
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
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        
        // Notifies the listeners that a Gridlet has been either scheduled
        // to run immediately or put in the waiting queue
    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);

    }
    
    /**
     * Finds the status of a specified job
     * @param gridletId  a job ID
     * @param userId     the user or owner's ID of this job
     * @return the job status or <code>-1</code> if not found
     */
    @Override
    public synchronized int gridletStatus(int gridletId, int userId) {
    	SSGridlet sgl = null;

        sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        sgl = waitingJobs.get(gridletId, userId);
        if (sgl != null) {
            return sgl.getStatus();
        }

        return -1; // -1 means not found
    }
    
	/**
     * Cancels a job running or in the waiting queue.
     * This method will search the running and waiting queues.
     * The User ID is important as many users might have the same 
     * job ID in the lists. If a job is cancelled and the job was
     * either running or was a pivot, then the availability profile is updated.
     * After that, the backfilling algorithm is called to check which
     * jobs can be started or scheduled.
     * <b>NOTE:</b>
     * <ul>
     *    <li> Before cancelling a job, this method updates when
     *    the expected completion time of the job is. If the 
     *    completion time is smaller than the current time, then 
     *    the job is considered to be <tt>finished</tt>. 
     *    Therefore the job cannot be cancelled.
     *    <li> Once a job has been cancelled, it cannot be resumed 
     *    to execute again since this method will pass the job 
     *    back to sender, i.e. the <code>userId</code>.
     *    <li> If a job cannot be found in either execution and 
     *    waiting lists, then a <code>null</code> Gridlet will be send back 
     *    to sender, i.e. the <code>userId</code>.
     * </ul>
     * @param gridletId  a job ID
     * @param userId   the user or owner's ID of this job
     */
    public synchronized void gridletCancel(int gridletId, int userId) {
    	double currentTime = GridSim.clock();
        boolean updateProfile = false;
        
        SSGridlet sgl = runningJobs.get(gridletId, userId);
        if (sgl != null) {
            // Test if job has finished 
            if(sgl.getStatus() == Gridlet.SUCCESS ||
            		sgl.getActualFinishTime() <= currentTime){
            	super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, 
            			null, gridletId, userId);
            	return; 
            } else {
            	// remove from the queue before compressing the schedule
            	runningJobs.remove(sgl);
            	updateProfile = true;
            }
        }

        if(sgl == null) {
        	sgl = waitingJobs.get(gridletId, userId);
        	if (sgl != null) {
        		waitingJobs.remove(sgl);

        		if(sgl.getStartTime() > 0) { // the job is a pivot.
                	EasyBackFillingPartition pt = getPartition(sgl.getPartitionID()); 
                	pt.pivot = null;
                	updateProfile = true;
                }
            }
        }

        // Send a null job if the job could not be found
        if(sgl == null) {
        	logger.info(super.get_name() + " cannot find Gridlet #" + 
        			gridletId + " for user #" + userId);
            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, null, gridletId, userId);
            return;
        }
        
        sgl.setStatus(Gridlet.CANCELED);

        //----------------- USED FOR DEBUGGING PURPOSES ONLY -------------------
        visualizer.notifyListeners(this.get_id(), ActionType.ITEM_CANCELLED, true, sgl);
        //----------------------------------------------------------------------
       
        // remove/update entries of the job in the profile
        if(updateProfile) {
        	removeGridlet(sgl);
        	backfillGridlets();

        	//------------------- USED FOR DEBUGGING PURPOSES ONLY -----------------
        	visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
        	//----------------------------------------------------------------------
        }

    	sgl.finalizeGridlet();
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, sgl.getGridlet(), gridletId, userId);
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
    
    // -------------------- PROTECTED METHODS ----------------------------
     
    /**
     * Checks whether the job can be handled by the resource. That is, 
     * verifies if it does not require more resources than what the resource
     * can provide and if the partition can handle the request.
     * @param sgl the server side gridlet to be examined
     * @return <code>true</code> if it can be handled; <code>false</code> otherwise.
     */
    protected boolean validateGridlet(SSGridlet sgl) {
    	int reqPE = sgl.getNumPE();
    	
    	if(reqPE > super.totalPE_){
    		String userName = GridSim.getEntityName(sgl.getSenderID());
    		logger.info("Gridlet #" + sgl.getID() + " from " +
	                userName + " user requires " + sgl.getNumPE() + " PEs. " +
	                		"The resource has only " + super.totalPE_ + " PEs.");
    		return false;
    	}
    	
    	// the id of the queue where the job can be scheduled
    	int queueId = profile.matchPartition(sgl); 
    	
    	// if no queue can handle the job, then reject it
    	if(queueId == -1) {
    		String userName = GridSim.getEntityName(sgl.getSenderID());
    		logger.info(super.get_name() + " cannot handle gridlet #" + sgl.getID() + 
    				" from " + userName + " as it cannot be assigned to a partition.");
    		return false;
    	}
    	
		// if the partition does not have enough resources, the borrowing is
    	// disabled and the behaviour is to return the job, then returns false, 
    	// forcing the job to be returned to the user
    	else {
    		ResourcePartition partition = partitions[queueId];
    		if(!allowBorrowing && returnJob 
    				&& partition.getInitialNumPEs() < reqPE) {
        		String userName = GridSim.getEntityName(sgl.getSenderID());
        		logger.info(" Gridlet #" + sgl.getID() + " from " +
    	                userName + " user requires " + sgl.getNumPE() + " PEs whereas the " +
    	                		"partition does not have enough resources and borrowing " +
    	                		"is disabled.");
        		return false;
    		}
    	}
    	
    	sgl.setPartitionID(queueId);
    	return true;
    }
    
    /**
     * Tries to start a job.
     * @param sgl the job to be started
     * @return <code>true</code> if the job has been started
     */
    protected boolean startGridlet(SSGridlet sgl) {
    	int reqPE = sgl.getNumPE();
    	int partId = sgl.getPartitionID();
        boolean success = false; 
    	
    	long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());
    	double now = GridSim.clock() ; 
    	double finishTime = now + runTime;
        
        // checks the availability of PEs in the selected partition
    	ProfileEntry entry = profile.checkPartAvailability(partId, now, runTime);
    	PERangeList ranges = entry.getAvailRanges();
    	
        if(entry.getNumPE() >= reqPE) {
        	// allocate PEs for the job
        	ranges = ranges.selectPEs(reqPE);
            profile.allocatePartPERanges(partId, ranges, now, finishTime);
        	success = true;
        }
        // partition can borrow resources from another
        else if (allowBorrowing) { 
        	// check availability in all partitions
            entry = profile.checkAvailability(reqPE, now, runTime);
            if(entry != null && entry.getNumPE() >= reqPE) {
            	// additional ranges required for the job
                PERangeList addRanges = entry.getAvailRanges();
                addRanges.remove(ranges);
                int addRequired = reqPE - ranges.getNumPE();
            	
            	// borrows the additional PEs from the partitions 
            	addRanges = addRanges.selectPEs(addRequired);
            	ranges.addAll(addRanges);
            	
            	if(ranges.getNumPE() > reqPE) {
            		// should not happen
            		logger.warning("Allocating more PEs than required.");
            	}
            	
            	profile.allocatePERanges(ranges, now, finishTime);
            	success = true;
            }
        }
        
        if(!success) {
        	return false; // it was not possible to schedule the gridlet
        }
        
        dynamics.setPEsBusy(ranges); // sets PEs to busy in resource characteristics
        runningJobs.add(sgl);
        sgl.setStartTime(now);
        sgl.setActualFinishTime(finishTime);
        sgl.setStatus(Gridlet.INEXEC);
        sgl.setPERangeList(ranges);
        
        // send event to itself to update the schedule once the job completes
        super.sendInternalEvent(runTime, UPT_SCHEDULE);
        
        //------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(this.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
        return true;
    }
    
    /**
     * Tries to schedule a job. Only the first job in the waiting queue
     * for a given partition is scheduled. If a pivot for that partition already
     * exists, them the method returns <code>null</code>
     * @param sgl the resource job
     * @return <code>true</code> if the job was scheduled.
     */
    protected boolean scheduleGridlet(SSGridlet sgl) {
    	int reqPE = sgl.getNumPE();
    	int queueId = sgl.getPartitionID();
    	EasyBackFillingPartition partition = getPartition(queueId);

    	// if partition has a pivot already, then check whether the job appears
    	// before in the queue after reordering. If it does, then the job
    	// to be scheduled becomes the new pivot
    	if(partition.pivot != null) {
    		int indPivot = waitingJobs.indexOf(partition.pivot);
    		int indexNewGrl = waitingJobs.indexOf(sgl);
    		
    		if(indPivot < indexNewGrl) {
    			return false;
    		} else {
    			removeGridlet(partition.pivot);
    			partition.pivot.setStartTime(-1);
    			partition.pivot.setActualFinishTime(-1);
    			partition.pivot = null;
    		}
    	}

    	long runTime = forecastExecutionTime(ratingPE, sgl.getRemainingLength());

        // check when enough PEs will be available at the partition
        ProfileEntry entryPart = profile.findPartStartTime(queueId, reqPE, runTime);

        // check when enough PEs will be available in all partitions
        ProfileEntry entry = profile.findStartTime(reqPE, runTime);

        double startPart = 
        	(entryPart == null || entryPart.getNumPE() < reqPE) ? 
        			Double.MAX_VALUE : 
        				entryPart.getTime();

        double startAll = !allowBorrowing ? Double.MAX_VALUE : entry.getTime();
        
    	PERangeList ranges = null;
    	double startTime = -1;	
    	double finishTime = -1;	
        
        // Compare which option has the best start time
        if(startPart < Double.MAX_VALUE && startPart <= startAll) {
        	ranges = entryPart.getAvailRanges();
           	ranges = ranges.selectPEs(reqPE);
           	
            startTime = startPart;
            finishTime = startTime + runTime;
            profile.allocatePartPERanges(queueId, ranges, startTime, finishTime);
        }
        // Try to borrow resources from another partition
        else if (startAll < Double.MAX_VALUE) {
        	PERangeList addRanges = entry.getAvailRanges();
            startTime = startAll;
            finishTime = startTime + runTime;
            
            // checks what PEs already belong to the job's partition
            entryPart = profile.checkPartAvailability(queueId, startTime, runTime);
            ranges = entryPart.getAvailRanges();
       		addRanges.remove(ranges);
        	addRanges = addRanges.selectPEs(reqPE - ranges.getNumPE());

        	ranges.addAll(addRanges);
            profile.allocatePERanges(ranges, startTime, finishTime);
        }
        else {
        	return false;
        }
        
        partition.pivot = sgl;
        sgl.setPERangeList(ranges);
        sgl.setStartTime(startTime);
        sgl.setActualFinishTime(finishTime);
        
        //------------------ FOR DEBUGGING PURPOSES ONLY ----------------
        visualizer.notifyListeners(this.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
        return true;
    }
    
	/**
     * This method is called to update the schedule. It removes completed
     * jobs and returns them to the users, then verifies which jobs can be 
     * started or scheduled.
     */
    protected void updateSchedule() {
    	int itemsFinished = finishRunningGridlets();
    	profile.removePastEntries(GridSim.clock());
    	int itemsStarted = backfillGridlets();

    	//---------------- USED FOR DEBUGGING PURPOSES ONLY --------------------
   		if(itemsStarted > 0 || itemsFinished > 0){
   			visualizer.notifyListeners(super.get_id(), ActionType.SCHEDULE_CHANGED, true);
   		}
    }
    
    /**
     * This method finalises the jobs in execution whose time
     * is smaller or equals to the current simulation time.
     * @return the number of jobs completed
     */
    protected int finishRunningGridlets() {
    	int itemsFinished = 0;
    	double now = GridSim.clock();

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
     * This method starts/backfills jobs that are in the queue and pivots 
     * (first jobs in the partitions) whose start time is smaller than the 
     * current simulation time.
     * @return the number of jobs started
     */
    protected int backfillGridlets() {
    	int gridletStarted = 0;
    	double now = GridSim.clock();
    	
    	// first checks whether the pivots can be started
    	for(ResourcePartition partition : partitions) {
    		EasyBackFillingPartition queue = (EasyBackFillingPartition)partition;
    		if(queue.pivot != null && queue.pivot.getStartTime() <= now) {
   				runningJobs.add(queue.pivot);
       			waitingJobs.remove(queue.pivot);
       			gridletStarted++;
       			queue.pivot.setStatus(Gridlet.INEXEC);
       	        super.sendInternalEvent(queue.pivot.getActualFinishTime()-now, UPT_SCHEDULE);
       			queue.pivot = null;
    		}
    	}
    	
    	if(jobOrder != null) {
        	// sorts jobs for backfilling
    		Collections.sort(waitingJobs, jobOrder);
    	}
    	
    	// Start job execution
    	Iterator<SSGridlet> iter = waitingJobs.iterator();
		while (iter.hasNext()) {
			SSGridlet gridlet = iter.next();
    		if(!gridlet.hasReserved() && gridlet.getStartTime() < 0) {
    			boolean success = startGridlet(gridlet);

    	        // if the job could not be scheduled immediately, then enqueue it
    	        if(success) {
        			iter.remove();
        			gridletStarted++;
    	        } else {
    	        	scheduleGridlet(gridlet);
    	        }
    		} else if(gridlet.hasReserved()) {
    			if(gridlet.getStartTime() <= now) {
    				runningJobs.add(gridlet);
    				iter.remove();
    				gridletStarted++;
    		        gridlet.setStatus(Gridlet.INEXEC);
    		        super.sendInternalEvent(gridlet.getActualFinishTime()-now, UPT_SCHEDULE);
    			}
    		}
    	}

    	return gridletStarted;
    }
    
    /**
     * Forecast finish time of a job.
     * <tt>Finish time = length / available rating</tt>
     * @param availableRating   the shared MIPS rating for all jobs
     * @param length remaining job length
     * @return the job's finish time.
     */
    protected long forecastExecutionTime(double availableRating, double length) {
    	long runTime = (long)((length / availableRating) + 1); 
        return Math.max(runTime, 1);
    }
    
    /**
     * Updates the job's properties once a job is considered finished.
     * @param sgl a SSGridlet object
     * @param status the job status
     */
    protected void gridletFinish(SSGridlet sgl, int status) {
        // the order is important! Set the status first then finalise
        // due to timing issues in SSGridlet class (Copied from GridSim)
        sgl.setStatus(status);
        sgl.finalizeGridlet();
        sendFinishGridlet( sgl.getGridlet() );
    }
    
    /**
     * This method removes/updates all the entries of a gridlet from the profile.
     * @param sgl the Gridlet to be removed
     */
    private void removeGridlet(SSGridlet sgl) {
    	if(!sgl.hasReserved()) {
	    	// removes the gridlet from the profile
        	profile.addPartTimeSlot(sgl.getPartitionID(), sgl.getStartTime(), 
        			sgl.getActualFinishTime(), sgl.getPERangeList());
    	}
    }
    
    /*
     * This method obtains the queue from the profile and does a casting
     */
    private EasyBackFillingPartition getPartition(int queueId) {
    	return (EasyBackFillingPartition)partitions[queueId];
    }
    
    /*
     * This class represents an easy backfilling partition
     */
    class EasyBackFillingPartition extends ResourcePartition {
    	// the first job of this queue in the general waiting queue
    	SSGridlet pivot;
    	
		private EasyBackFillingPartition(int queueId, int numPE,
				PartitionPredicate predicate) {
			super(queueId, numPE, predicate);
			pivot = null;
		}
    }
    
    /*
     * Comparator to order jobs according to their priorities 
     * and start time
     */
    class OrderGridletsByPriority implements Comparator<SSGridlet> {

		public int compare(SSGridlet gl0, SSGridlet gl1) {
			Integer priority0 = gl0.getPriority();
			Integer priority1 = gl1.getPriority();
			int result = priority0.compareTo(priority1);

			if(result == 0) {
				Double submission0 = gl0.getSubmissionTime();
				Double submission1 = gl1.getSubmissionTime();
				result = submission0.compareTo(submission1);
			}
			return result;
		}
    }
}
