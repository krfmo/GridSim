/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.scheduler;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.parallel.ParallelResource;
import gridsim.parallel.SSGridlet;
import gridsim.parallel.gui.ActionType;
import gridsim.parallel.gui.Visualizer;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;
import gridsim.parallel.profile.PERangeList;
import gridsim.parallel.profile.ProfileEntry;
import gridsim.parallel.profile.ScheduleItem;
import gridsim.parallel.profile.SingleProfile;
import gridsim.parallel.profile.TimeSlot;
import gridsim.parallel.reservation.ErrorType;
import gridsim.parallel.reservation.Reservation;
import gridsim.parallel.reservation.ReservationMessage;
import gridsim.parallel.reservation.ReservationPolicy;
import gridsim.parallel.reservation.ReservationStatus;
import gridsim.parallel.reservation.ServerReservation;
import gridsim.parallel.util.WorkloadLublin99;

/**
 * {@link ARConservativeBackfill} class is an allocation policy for 
 * {@link ParallelResource} that implements conservative backfilling and 
 * supports advance reservations. The policy is based on the conservative 
 * backfilling algorithm described in the following papers:
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
 * Similarly to {@link ConservativeBackfill} this scheduler maintains 
 * an availability profile. But the difference is that in many cases
 * an advance reservation will require two entries in the profile,
 * one to mark its start time and another to delimit its finish time.
 * In addition, when a job is cancelled, the advance reservations are not 
 * removed from the availability profile and therefore are not moved 
 * forwards in the scheduling queue. In other words, there is no compression
 * of the scheduling queue.
 * <br>
 * This scheduler supports parallel jobs and some reservation functionalities, 
 * such as:
 * <ul>
 *    <li> process a new reservation
 *    <li> cancel a reservation
 *    <li> commit a reservation
 *    <li> process a reservation status query
 *    <li> list free time over a certain period of time
 *    <li> provide availability information when an advance 
 *    reservation is cancelled.
 * </ul>
 * <p>
 * <b>NOTE THAT:</b><br>
 * 	<ul>
 * 		<li> The list of machines comprising this resource must 
 * 			 be homogeneous.
 * 		<li> Local load is not considered. If you would like to simulate this, 
 * 			 you have to model the local load as jobs. It is more precise 
 * 			 and faster. To do so, please check {@link WorkloadLublin99}.
 *      <li> Jobs cannot be paused or migrated.
 *  </ul>
 * 
 * @author  Marcos Dias de Assuncao
 * @since  5.0
 * 
 * @see gridsim.ResourceCharacteristics
 * @see ReservationPolicy
 * @see SingleProfile
 * @see ConservativeBackfill
 */
public class ARConservativeBackfill extends ConservativeBackfill 
					implements ReservationPolicy {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
	// outstanding reservations
    private LinkedHashMap<Integer,ServerReservation> reservTable = 
    		new LinkedHashMap<Integer,ServerReservation>(); 
    
    // expired reservations
    private LinkedHashMap<Integer,ServerReservation> expiryTable =  
    		new LinkedHashMap<Integer,ServerReservation>();  		
    
    // default booking/reservation commit period
    private int commitPeriod = 30*60;     
    
    // a tag to denote expiry time
    private static final int EXPIRY_TIME = 12;
	
	// last time the expiry time of reservations was checked
	private double lastCheckExpiryTime = 0.0D;
	
	private Visualizer visualizer = null;

	/**
     * Allocates a new {@link ARConservativeBackfill} object.
     * @param resourceName the grid resource entity name that will contain 
     * 		   this allocation policy
     * @param entityName this object entity name
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     *  <li> Creating this entity before initialising GridSim package
     *  <li> The entity name is <code>null</code> or empty
     * </ul>
     */
	public ARConservativeBackfill(String resourceName, String entityName)
			throws Exception {
		super(resourceName, entityName);
	}
	
	/**
     * Allocates a new {@link ARConservativeBackfill} object.
     * @param resourceName the grid resource entity name that will contain 
     * 		   this allocation policy
     * @param entityName this object entity name
     * @param commitPeriod  a default commit period time for a user to commit
     * a reservation (unit is in second). <b>NOTE:</b> once it is set, you cannot 
     * change the time again.
     * @throws Exception This happens when one of the following scenarios occur:
     * <ul>
     *  <li> Creating this entity before initialising GridSim package
     *  <li> The entity name is <code>null</code> or empty
     * </ul>
     */
	public ARConservativeBackfill(String resourceName, String entityName, 
			int commitPeriod) throws Exception {
		super(resourceName, entityName);
		this.commitPeriod = commitPeriod;
	}
	
	@Override
    public void body() {
		visualizer = GridSim.getVisualizer();
		super.body();
        reservTable.clear();  		
    	expiryTable.clear();
    	lastCheckExpiryTime = 0L;
    	commitPeriod = 30*60;
    }
	
    /**
     * Process and event sent to this entity 
     * @param ev the event to be handled
     */
    public void processOtherEvent(Sim_event ev) {
    	double currentTime = GridSim.clock();
    		
    	if(ev.get_src() == myId_ && ev.get_tag() == EXPIRY_TIME) {
    		if(currentTime > lastCheckExpiryTime) { 
    			checkExpiryTime();
    			lastCheckExpiryTime = currentTime;
    		}
    	} else {
    		super.processOtherEvent(ev);
    	}
    }

    /**
     * Handles an advance reservation request.
     * @param message the advance reservation message received
     * @return <code>true</code> if the reservation was accepted; 
     * <code>false</code> otherwise.
     */
	public synchronized ReservationMessage createReservation(ReservationMessage message) {
		Reservation reserv = message.getReservation();
		ServerReservation sRes = new ServerReservation(reserv); 
		
    	//-------------- FOR DEBUGGING PURPOSES ONLY ---------------
		visualizer.notifyListeners(this.get_id(), ActionType.ITEM_ARRIVED, true, sRes);
        //----------------------------------------------------------

		ReservationMessage response = message.createResponse(); // creates a response

		if(!validateReservation(sRes)) {
    	    reserv.setStatus(ReservationStatus.FAILED);
        	response.setErrorCode(ErrorType.OPERATION_FAILURE);
        	return response;
		}

        double currentTime = GridSim.clock();
		double startTime = Math.max(reserv.getStartTime(), currentTime);
		//TODO: To check this exptime (Marcos)
		double expTime = sRes.getActualFinishTime();
		int duration = reserv.getDurationTime();
        sRes.setStartTime(startTime);
        
        // check the availability in the profile
        ProfileEntry entry = profile.checkAvailability(reserv.getNumPE(), startTime, duration);

        // if entry is null, it means that there are not enough PEs 
        if(entry == null) {
        	logger.warning("Reservation #" + reserv.getID() + " from " + 
        			GridSim.getEntityName(message.getSourceID()) + 
        			" user requires " + reserv.getNumPE() + 
	                " PEs from " + startTime + " to " + (startTime + duration) + 
        			" could not be accepted");

        	// the availability information is passed as scheduling options
        	Collection<TimeSlot> availability = getReservationAlternatives(sRes);
        	reserv.setStatus(ReservationStatus.FAILED);
        	
        	if(availability != null) {
        		reserv.setReservationOptions(availability);
        		response.setErrorCode(ErrorType.OPERATION_FAILURE_BUT_OPTIONS);
        	} else {
        		response.setErrorCode(ErrorType.OPERATION_FAILURE);
        	}
        } else { 
            PERangeList selected = entry.getAvailRanges().selectPEs(reserv.getNumPE());
            profile.allocatePERanges(selected, startTime, sRes.getExpectedFinishTime());
            sRes.setPERangeList(selected);
            
            if(Double.compare(currentTime, startTime)  == 0) {
    	        // if start time = current time, reservation is immediate. A event
            	// is scheduled to start the reservation and it commits the reservation
	        	sRes.setStatus(ReservationStatus.COMMITTED);
		        super.sendInternalEvent(GridSimTags.SCHEDULE_NOW, ConservativeBackfill.UPT_SCHEDULE);
	        } else {
                // expiration time is used only for non-immediate reservations
                // expiry time can't be greater than the reservation's start time
            	expTime = Math.min(startTime, currentTime + commitPeriod);
            	
            	sRes.setStatus(ReservationStatus.NOT_COMMITTED);
		        // to check for expired reservations
		        super.sendInternalEvent(expTime - currentTime, EXPIRY_TIME);
	        }
	        
	        sRes.setExpiryTime(expTime);
	        reservTable.put(new Integer(sRes.getID()), sRes);

	        //-------------- FOR DEBUGGING PURPOSES ONLY  --------------
	        visualizer.notifyListeners(this.get_id(), ActionType.ITEM_SCHEDULED, true, sRes);
	        //----------------------------------------------------------
        }
        
    	return response;
	}
	
    /**
     * This method handles a cancel reservation request.
     * @param message the advance reservation message received requesting
     * the cancellation 
     * @return <code>true</code> if the reservation was cancelled; 
     * 			<code>false</code> otherwise.
     */
	public synchronized boolean cancelReservation(ReservationMessage message) {
		ServerReservation sRes = null;
		int resId = message.getReservationID();
		String msg = "Reservation # " + resId + " cannot be cancelled ";
		
		if(!reservTable.containsKey(resId)
				&& !expiryTable.containsKey(resId)) {
			logger.info(msg + "because the allocation policy could not find it.");
			return false;
		} else if (expiryTable.containsKey(resId)) {
			logger.info(msg + "because it is already in the expiry list.");
    		return false;
		} else if( (sRes = reservTable.get(resId)).getActualFinishTime() <= GridSim.clock() ) {
			logger.info(msg + "it has already finished.");
    		return false;
		}

		boolean inProgress = sRes.getReservationStatus() == ReservationStatus.IN_PROGRESS;
        reservTable.remove(resId);
        expiryTable.put(resId, sRes);

        removeReservation(sRes);
		sRes.setStatus(ReservationStatus.CANCELLED);
		
		//----------------- USED FOR DEBUGGING PURPOSES ONLY -------------------
		visualizer.notifyListeners(this.get_id(), ActionType.ITEM_CANCELLED, true, sRes);
        //----------------------------------------------------------------------
        
		compressSchedule(sRes.getStartTime(), inProgress);
		
    	//----------------- USED FOR DEBUGGING PURPOSES ONLY -------------------
		visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
    	//----------------------------------------------------------------------
		return true;
	}

	/**
     * Handles a query free time request.
     * @param message the advance reservation message received.
     * @return the response message with the availability information
     */
	public synchronized ReservationMessage queryAvailability(ReservationMessage message) {
        Reservation res = message.getReservation();
		ReservationMessage response = message.createResponse();

		Collection<TimeSlot> slots = 
			super.profile.getTimeSlots(res.getStartTime(), res.getDurationTime());
		
		response.getReservation().setReservationOptions(slots);
    	return response;
	}

    /**
     * Handles a query reservation request.
     * @param message the advance reservation message received.
     * @return the response message.
     */
	public synchronized ReservationMessage queryReservation(ReservationMessage message) {
        int resId = message.getReservationID();
		ReservationMessage response = message.createResponse();
		
		if(!reservTable.containsKey(resId) 
				&& !expiryTable.containsKey(resId) ) {
			logger.info("Error querying the status of reservation # " + resId + 
    				" because Grid Resource #" + super.resId_ + 
    				" could not find it.");
			response.setErrorCode(ErrorType.OPERATION_FAILURE);
		}

    	return response;
	}
	
    /**
     * Handles a commit reservation request.
     * @param message the advance reservation message received
     * @return <code>true</code> if the reservation was committed; 
     * 		<code>false</code> otherwise.
     */
	public synchronized boolean commitReservation(ReservationMessage message) {
        int resId = message.getReservationID();
        ServerReservation sRes = null;
		String msg = "Reservation # " + resId + " cannot be committed because it ";
		
        // Tries to find the reservation in the lists
		if(reservTable.containsKey(resId)) {
			sRes = reservTable.get(resId);
		} else if(expiryTable.containsKey(resId)) {
			sRes = expiryTable.get(resId);

			if(sRes.getReservationStatus() == ReservationStatus.CANCELLED) {
				logger.info(msg + "has previously been cancelled by the allocation policy.");
			} else if (sRes.getReservationStatus() == ReservationStatus.FINISHED) {
				logger.info(msg + "has finished.");
			} else {
				logger.info(msg + "is in the expiry list.");
			}
			
			return false;
		} else {
			logger.info(msg + "could not be found.");
    		return false;
		}

        // sets the reservation to committed if it has not been set before
        if(sRes.getReservationStatus() == ReservationStatus.NOT_COMMITTED) {
        	sRes.setStatus(ReservationStatus.COMMITTED);
        	
            // then send this into itself to start the reservation
            super.sendInternalEvent(sRes.getStartTime() - GridSim.clock(), 
            		ConservativeBackfill.UPT_SCHEDULE);
        }

    	//-------------- FOR DEBUGGING PURPOSES ONLY  --------------
    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_STATUS_CHANGED, true, sRes);
        //----------------------------------------------------------
        return true;
	}
	
	/**
     * A method that handles a request to modify a reservation.
     * @param message the advance reservation message received
     * @return <code>true</code> if the reservation was modified; 
     * <code>false</code> otherwise.
     */
	public boolean modifyReservation(ReservationMessage message) {
		logger.warning("Operation not supported");
		return false;
	}
	
	/**
     * Schedules a new job received by the Grid resource entity.
     * @param gridlet a job object to be executed
     * @param ack user wants to know whether this operation is successful 
     * or not, <code>false</code> otherwise.
     */
	@Override
    public void gridletSubmit(Gridlet gridlet, boolean ack) {
    	if(!gridlet.hasReserved()) {
    		super.gridletSubmit(gridlet, ack);
    	} 
    	else {
    		if(!handleReservationJob(gridlet)) {
	    		try {
					gridlet.setGridletStatus(Gridlet.FAILED);
				} catch (Exception e) {
					// should not happen
					logger.log(Level.WARNING, "Error submitting gridlet", e);
				}
	    		super.sendFinishGridlet(gridlet);
    		} 
    		else {
	            if (ack) {
	                // sends back an ack
	            	super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
	            			gridlet.getGridletID(), gridlet.getUserID());
	            }
    		}
    	}
    }
	
    /**
     * This method is called to update the schedule. It removes completed
     * jobs and return them to the users and verifies whether there are 
     * jobs in the waiting list that should start execution. It also 
     * removes old entries from the availability profile.
     */
    @Override
    protected void updateSchedule() {
    	double currentTime = GridSim.clock();
    	int nFinished = 0;
    	int nStarted = 0;
    	
   		nFinished = finishReservations(); 			 // finish reservations first
   		nFinished += super.finishRunningGridlets();  // finish the jobs
   		
    	// remove past entries from the profile
   		profile.removePastEntries(currentTime);
   		
    	nStarted = startReservations();				// start the advance reservations
   		nStarted += super.startQueuedGridlets(); 	// Start the execution of Gridlets
    	
    	//---------------- USED FOR DEBUGGING PURPOSES ONLY --------------------
   		if(nStarted > 0 || nFinished > 0){
   			visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
   		}
    }
	
    /*
     * Checks whether the reservation can be handled by the resource.
     * @param res the server side reservation to be examined
     * @return <code>true</code> if it can be handled; <code>false</code> otherwise.
     */
    private boolean validateReservation(ServerReservation res) {
        if (res.getNumPE() > super.totalPE_) {
        	String userName = GridSim.getEntityName( res.getSenderID() );
        	logger.info("Reservation #" + res.getID() + " from " +
	                userName + " user requires " + res.getNumPE() + 
	                " PEs from " + res.getStartTime() + " to " + res.getActualFinishTime());
        	logger.info("--> The resource has only " + super.totalPE_ + " PEs available.");
            return false;
        }
        
        return true;
    }

    /*
     * Gets the reservation alternatives to be provided to the reservation
     * requester or user in case a given advance reservation is not accepted.<br>
     * <b>NOTE:</b> The default implementation of this method returns the 
     * resource availability information to the user. To do so, the method 
     * queries the availability using the reservation's start time as start time
     * for the query and {@link Integer.MAX_VALUE} as the duration in seconds.
     * @param sRes the server side reservation for which alternatives are to be given
     * @return an availability object containing the resource availability or
     * <code>null</code> in case no options are to be provided.
     */
    private Collection<TimeSlot> getReservationAlternatives(ServerReservation sRes) {
    	return super.profile.getTimeSlots(sRes.getStartTime(), Integer.MAX_VALUE);
    }
    
    /*
     * This method removes/updates all the entries of a reservation from 
     * the profile and updates the ranges of current free PEs if the 
     * reservation was in execution.
     * @param res the Reservation to be removed
     */
    private void removeReservation(ServerReservation res) {
    	boolean inProg = res.getReservationStatus() == ReservationStatus.IN_PROGRESS;
    	
    	// returns the time slot to the profile
    	profile.addTimeSlot(res.getStartTime(), 
    			res.getExpectedFinishTime(), res.getPERangeList());
    	
    	LinkedList<ScheduleItem> removedGridlets = new LinkedList<ScheduleItem>();
    	Iterator<SSGridlet> iterGrl = 
    		inProg ? runningJobs.iterator() : waitingJobs.iterator();

        // removes all gridlets that are associated to the reservation
    	while(iterGrl.hasNext()) {
    		SSGridlet gridlet = iterGrl.next();
    		if(gridlet.getReservationID() == res.getID() 
    				&& gridlet.getStatus() != Gridlet.SUCCESS 
    				&& gridlet.getStatus() != Gridlet.FAILED) {
    			
    			gridlet.setStatus(Gridlet.CANCELED);
    			gridlet.finalizeGridlet();
    			removedGridlets.add(gridlet);
    			iterGrl.remove();
    			super.sendFinishGridlet(gridlet.getGridlet());
    		}
    	}
    	
    	//---------------- USED FOR DEBUGGING PURPOSES ONLY ----------------
    	if(removedGridlets.size() > 0) {
    		visualizer.notifyListeners(super.get_id(), ActionType.ITEM_CANCELLED, 
       				false, removedGridlets);
    	}
    }
    
	/*
     * Schedules a new job received by the Grid resource entity and 
     * for which an advance reservation has been made.
     * @param gridlet a job object to be executed
     */
    private synchronized boolean handleReservationJob(Gridlet gridlet) {
        int reqPE = gridlet.getNumPE();
    	SSGridlet sgl = new SSGridlet(gridlet);
    	long runTime = super.forecastExecutionTime(ratingPE, sgl.getLength());
    	ServerReservation sRes = reservTable.get(gridlet.getReservationID());
    	
    	//-------------------- FOR DEBUGGING PURPOSES ONLY ---------------------
    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_ARRIVED, true, sgl);
    	//----------------------------------------------------------------------
    	
    	if(sRes == null) {
    		String userName = GridSim.getEntityName( gridlet.getUserID() );
    		logger.info("Gridlet #" + gridlet.getGridletID() + " from " +
	                userName + " cannot be accepted because the reservation #" +
		            gridlet.getReservationID() + " has not been found.");
    		return false;
    	}
    	// job requires more PEs than what the reservation currently has
    	else if( sRes.getNumRemainingPE() < reqPE ) {
	    	String userName = GridSim.getEntityName( gridlet.getUserID() );
	    	logger.info("Gridlet #" + gridlet.getGridletID() + " from " +
		            userName + " cannot be accepted because the reservation #" +
			        sRes.getID() + " has only " + sRes.getNumRemainingPE() + " PEs.");
	    	return false;
    	}
    	// job is expected to run for longer than the time previously reserved
    	else if (runTime > sRes.getRemainingTime()) {
    		String userName = GridSim.getEntityName( gridlet.getUserID() );
    		logger.info("Gridlet #" + gridlet.getGridletID() + " from " +
	                userName + " cannot be accepted because the reservation #" +
		            sRes.getID() + " has a remaining time of " + 
		            sRes.getRemainingTime() + " seconds," +
		            " whereas the gridlet is expected to run for " +
		            runTime + " seconds.");
    		return false;
    	}

    	double startTime = Math.max(sRes.getStartTime(), GridSim.clock());
       	PERangeList selected = sRes.selectPERangeList(reqPE);
       	sgl.setPERangeList(selected);
   		sgl.setStartTime(startTime);
   		sgl.setActualFinishTime(startTime + runTime);
   		sgl.setStatus(Gridlet.QUEUED);
   		waitingJobs.add(sgl);
   		
        // if reservation has not been committed, then commit the reservation
        if (sRes.getReservationStatus() == ReservationStatus.NOT_COMMITTED) {
           	sRes.setStatus(ReservationStatus.COMMITTED);
           	super.sendInternalEvent(sRes.getStartTime() - GridSim.clock(), 
           			ConservativeBackfill.UPT_SCHEDULE);
        }
        
        super.sendInternalEvent(startTime - GridSim.clock(), 
        		ConservativeBackfill.UPT_SCHEDULE);
        
    	//------------------ FOR DEBUGGING PURPOSES ONLY ----------------
    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_SCHEDULED, true, sgl);
    	//---------------------------------------------------------------
    	return true;
    }
    
    /*
     * Checks for expiry time of a given reservations in the list.
     * Jobs whose start time is greater than current time will be shifted 
     * forwards in the queue when the compression of the schedule takes place
     */
    private void checkExpiryTime() {  
        double referenceTime = Double.MAX_VALUE;
    	LinkedList<ScheduleItem> removedRes = new LinkedList<ScheduleItem>(); 
        
    	Iterator<ServerReservation> iterRes = reservTable.values().iterator();
    	while(iterRes.hasNext()) {
    		ServerReservation sRes = iterRes.next();
    		ReservationStatus status = sRes.getReservationStatus();

	    	// check for expiry time
	        if (status == ReservationStatus.NOT_COMMITTED &&
	        		sRes.getExpiryTime() <= GridSim.clock()) {
	        	logger.info("Reservation # " + sRes.getID() + " has not been " + 
	        			"committed and has hence been cancelled at time #" + GridSim.clock());
	        	
	        	removeReservation(sRes);  // update the profile
	        	sRes.setStatus(ReservationStatus.CANCELLED);     		
	        	expiryTable.put(sRes.getID(), sRes);
	        	iterRes.remove();
	        	
	        	referenceTime = Math.min(referenceTime, sRes.getStartTime());
	        	removedRes.add(sRes);
	        }
    	}
    	
    	if(removedRes.size() > 0) {

    		//-------------- USED FOR DEBUGGING PURPOSES ONLY -----------------
    		visualizer.notifyListeners(this.get_id(), ActionType.ITEM_CANCELLED, true, removedRes);
            //-----------------------------------------------------------------
    		
    		// performs the compression of the schedule 
    		compressSchedule(referenceTime, false);
    		
	    	//---------------- USED FOR DEBUGGING PURPOSES ONLY ----------------
    		visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
	    	//-----------------------------------------------------------------
    	}
    }
    
    /*
     * Its called to start a reservations
     */
    private int startReservations() {
    	double refTime = GridSim.clock();
    	LinkedList<ScheduleItem> startedRes = new LinkedList<ScheduleItem>();
    	int numStartRes = 0;

    	for (ServerReservation sRes : reservTable.values()) {
    		if(sRes.getStartTime() <= refTime && 
    				sRes.getReservationStatus() == ReservationStatus.COMMITTED) {
    			
    			sRes.setStatus(ReservationStatus.IN_PROGRESS);
	    	  	startedRes.add(sRes);
	            numStartRes++;
	
	            super.sendInternalEvent(sRes.getActualFinishTime()-refTime, 
	            		ConservativeBackfill.UPT_SCHEDULE);
    		}
    	}

    	//------------- USED FOR DEBUGGING PURPOSES ONLY ------------------
    	if(numStartRes > 0) {
	    	visualizer.notifyListeners(this.get_id(), ActionType.ITEM_STATUS_CHANGED, 
	    				true, startedRes);
	    	visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
    	}
    	//------------------------------------------------------------------
    	
    	return numStartRes;
    }
    
    /*
     * This method is called to finish reservations. All reservations 
     * whose finish time is smaller or equals to refTime will be completed
     * @return the number of reservations completed
     */
    private int finishReservations() {
    	int resFinished = 0;
    	
    	Iterator<ServerReservation> iterRes = reservTable.values().iterator();
    	while(iterRes.hasNext()) {
    		ServerReservation sRes = iterRes.next();
    		if(sRes.getActualFinishTime() <= GridSim.clock()) {
    			sRes.setStatus(ReservationStatus.FINISHED);
	        	resFinished++;
	        	iterRes.remove();
	        	expiryTable.put(sRes.getID(), sRes);
    		}
    	}

    	//------------- USED FOR DEBUGGING PURPOSES ONLY ------------------
    	if(resFinished > 0) {
    		visualizer.notifyListeners(this.get_id(), ActionType.SCHEDULE_CHANGED, true);
    	}
    	
    	return resFinished;
    }
    
}
