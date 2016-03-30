package gridsim.fta;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.AllocPolicy;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.PE;
import gridsim.PEList;
import gridsim.ResGridlet;
import gridsim.ResGridletList;
import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

/**
 * This class is based on {@link gridsim.SpaceShared}, but
 * with added failure functionalities.
 * PerfectCheckPointing class is an allocation policy for GridResource that
 * behaves exactly like First Come First Serve (FCFS). This is a basic and
 * simple scheduler that runs each Gridlet to one Processing Element (PE).
 * If a Gridlet requires more than one PE, then this scheduler only assign
 * this Gridlet to one PE. <br>
 * In the case of failure event, the gridlet on the failed machine will be paused. <br>
 * In the case of recovery event, the gridlet on the recovered machine will be resumed. <br>
 *
 * @author       Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * @see 		 AllocPolicy
 * @see 		 FTAllocPolicy
 * @invariant $none
 */

public class PerfectCheckPointing extends AllocPolicy implements FTAllocPolicy
	{
		private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);

	    private ResGridletList gridletQueueList_;     // Queue list
	    private ResGridletList gridletInExecList_;    // Execution list
	    private ResGridletList gridletPausedList_;    // Pause list
	    private double lastUpdateTime_;    // the last time Gridlets updated
	    private int[] machineRating_;      // list of machine ratings available
	    
	    public PerfectCheckPointing (String resourceName, String entityName) throws Exception
	    {
	        super(resourceName, entityName);

	        // initialises local data structure
	        this.gridletInExecList_ = new ResGridletList();
	        this.gridletPausedList_ = new ResGridletList();
	        this.gridletQueueList_  = new ResGridletList();
	        this.lastUpdateTime_ = 0.0;
	        this.machineRating_ = null;
	    }
	    
	    /**
	     * Handles internal events that are coming to this entity.
	     * @pre $none
	     * @post $none
	     */
	    public void body()
	    {
	        // Gets the PE's rating for each Machine in the list.
	        // Assumed one Machine has same PE rating.
	        MachineList list = super.resource_.getMachineList();
	        int size = list.size();
	        machineRating_ = new int[size];
	        for (int i = 0; i < size; i++) {
	            machineRating_[i] = super.resource_.getMIPSRatingOfOnePE(i, 0);
	        }

	        // a loop that is looking for internal events only
	        Sim_event ev = new Sim_event();
	        while ( Sim_system.running() )
	        {
	            super.sim_get_next(ev);

	            // if the simulation finishes then exit the loop
	            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION ||
	                super.isEndSimulation())
	            {
	                break;
	            }

	            // Internal Event if the event source is this entity
	            if (ev.get_src() == super.myId_ && gridletInExecList_.size() > 0)
	            {
	                updateGridletProcessing();   // update Gridlets
	                checkGridletCompletion();    // check for finished Gridlets
	            }

	            // Internal Event if the event source is this entity
	            if (ev.get_src() == super.myId_ && gridletQueueList_.size() > 0)
	            {
	                updateGridletProcessing();   // update Gridlets
	                checkGridletCompletion();    // check for finished Gridlets
	                allocateQueueGridlet();		// 	check for new Gridlet allocation
	            }
	        }

	        // CHECK for ANY INTERNAL EVENTS WAITING TO BE PROCESSED
	        while (super.sim_waiting() > 0)
	        {
	            // wait for event and ignore since it is likely to be related to
	            // internal event scheduled to update Gridlets processing
	            super.sim_get_next(ev);
	            logger.log(Level.INFO,super.resName_ +
	                    ".PerfectCheckPointing.body(): ignore internal events");
	        }
	    }
	    /**
	     * Schedules a new Gridlet that has been received by the GridResource
	     * entity.
	     * @param   gl    a Gridlet object that is going to be executed
	     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
	     *        whether this operation is success or not, <tt>false</tt>
	     *        otherwise (don't care)
	     * @pre gl != null
	     * @post $none
	     */
	    public synchronized void gridletSubmit(Gridlet gl, boolean ack)
	    {
	        // update the current Gridlets in exec list up to this point in time
	        updateGridletProcessing();

	        // reset number of PE since at the moment, it is not supported
	        if (gl.getNumPE() > 1)
	        {
	            String userName = GridSim.getEntityName( gl.getUserID() );
	            logger.log(Level.INFO,super.get_name() + ".gridletSubmit(): " +
	                " Gridlet #" + gl.getGridletID() + " from " + userName +
	                " user requires " + gl.getNumPE() + " PEs.");
	            logger.log(Level.INFO,"--> Process this Gridlet to 1 PE only.");

	            // also adjusted the length because the number of PEs are reduced
	            int numPE = gl.getNumPE();
	            double len = gl.getGridletLength();
	            gl.setGridletLength(len*numPE);
	            gl.setNumPE(1);
	        }

	        ResGridlet rgl = new ResGridlet(gl);
	        boolean success = false;

	        // if there is an available PE slot, then allocate immediately
	        if (gridletInExecList_.size() < super.totalPE_) {
	            success = allocatePEtoGridlet(rgl);
	        }

	        // if no available PE then put the ResGridlet into a Queue list
	        if (!success)
	        {
	            rgl.setGridletStatus(Gridlet.QUEUED);
	            gridletQueueList_.add(rgl);
	        }

	        // sends back an ack if required
	        if (ack)
	        {
	            super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
	                          gl.getGridletID(), gl.getUserID()
	            );
	        }
	    }

	    /**
	     * Finds the status of a specified Gridlet ID.
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @return the Gridlet status or <tt>-1</tt> if not found
	     * @see gridsim.Gridlet
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @post $none
	     */
	    public synchronized int gridletStatus(int gridletId,int userId)
	    {
	        ResGridlet rgl = null;

	        // Find in EXEC List first
	        int found = gridletInExecList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // Get the Gridlet from the execution list
	            rgl = (ResGridlet) gridletInExecList_.get(found);
	            return rgl.getGridletStatus();
	        }

	        // Find in Paused List
	        found = gridletPausedList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // Get the Gridlet from the execution list
	            rgl = (ResGridlet) gridletPausedList_.get(found);
	            return rgl.getGridletStatus();
	        }

	        // Find in Queue List
	        found = gridletQueueList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // Get the Gridlet from the execution list
	            rgl = (ResGridlet) gridletQueueList_.get(found);
	            return rgl.getGridletStatus();
	        }

	        // if not found in all 3 lists then no found
	        return -1;
	    }

	    /**
	     * Cancels a Gridlet running in this entity.
	     * This method will search the execution, queued and paused list.
	     * The User ID is
	     * important as many users might have the same Gridlet ID in the lists.
	     * <b>NOTE:</b>
	     * <ul>
	     *    <li> Before canceling a Gridlet, this method updates all the
	     *         Gridlets in the execution list. If the Gridlet has no more MIs
	     *         to be executed, then it is considered to be <tt>finished</tt>.
	     *         Hence, the Gridlet can't be canceled.
	     *
	     *    <li> Once a Gridlet has been canceled, it can't be resumed to
	     *         execute again since this method will pass the Gridlet back to
	     *         sender, i.e. the <tt>userId</tt>.
	     *
	     *    <li> If a Gridlet can't be found in both execution and paused list,
	     *         then a <tt>null</tt> Gridlet will be send back to sender,
	     *         i.e. the <tt>userId</tt>.
	     * </ul>
	     *
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @post $none
	     */
	    public synchronized void gridletCancel(int gridletId, int userId)
	    {
	        // cancels a Gridlet
	        ResGridlet rgl = cancel(gridletId, userId);

	        // if the Gridlet is not found
	        if (rgl == null)
	        {
	        	logger.log(Level.INFO, super.resName_ +
	                    ".PerfectCheckPointing.gridletCancel(): Cannot find " +
	                    "Gridlet #" + gridletId + " for User #" + userId);

	            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, null,
	                                    gridletId, userId);
	            return;
	        }

	        // if the Gridlet has finished beforehand then prints an error msg
	        if (rgl.getGridletStatus() == Gridlet.SUCCESS)
	        {
	        	logger.log(Level.INFO,super.resName_
	                    + ".PerfectCheckPointing.gridletCancel(): Cannot cancel"
	                    + " Gridlet #" + gridletId + " for User #" + userId
	                    + " since it has FINISHED.");
	        }

	        // sends the Gridlet back to sender
	        rgl.finalizeGridlet();
	        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, rgl.getGridlet(),
	                                gridletId, userId);
	    }

	    /**
	     * Pauses a Gridlet only if it is currently executing.
	     * This method will search in the execution list. The User ID is
	     * important as many users might have the same Gridlet ID in the lists.
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @param   ack   	   an acknowledgement, i.e. <tt>true</tt> if wanted to know
	     *        whether this operation is success or not, <tt>false</tt>
	     *        otherwise (don't care)
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @post $none
	     */
	    public synchronized void gridletPause(int gridletId, int userId, boolean ack)
	    {
	        boolean status = false;

	        // Find in EXEC List first
	        int found = gridletInExecList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // updates all the Gridlets first before pausing
	            updateGridletProcessing();

	            // Removes the Gridlet from the execution list
	            ResGridlet rgl = (ResGridlet) gridletInExecList_.remove(found);

	            if (rgl.getRemainingGridletLength() == 0.0)
	            {
	                found = -1;  // meaning not found in Queue List
	    	        rgl.setGridletStatus(Gridlet.SUCCESS);
	    	        rgl.finalizeGridlet();
	    	        super.sendFinishGridlet( rgl.getGridlet() );

	    	        logger.log(Level.INFO,super.resName_
	                        + ".PerfectCheckpointing.gridletPause(): Cannot pause"
	                        + " Gridlet #" + gridletId + " for User #" + userId
	                        + " since it has FINISHED.");
	            }
	            else
	            {
	                status = true;
	                rgl.setGridletStatus(Gridlet.PAUSED);  // change the status
	                gridletPausedList_.add(rgl);   // add into the paused list

	            }
	        }
	        else {      // Find in QUEUE list
	            found = gridletQueueList_.indexOf(gridletId, userId);
	        }

	        // if found in the Queue List
	        if (!status && found >= 0)
	        {
	            status = true;

	            // removes the Gridlet from the Queue list
	            ResGridlet rgl = (ResGridlet) gridletQueueList_.remove(found);
	            rgl.setGridletStatus(Gridlet.PAUSED);   // change the status
	            gridletPausedList_.add(rgl);            // add into the paused list
	        }
	        // if not found anywhere in both exec and paused lists
	        else if (found == -1)
	        {
	        	logger.log(Level.INFO,super.resName_ +
	                    ".PerfectCheckpointing.gridletPause(): Error - cannot " +
	                    "find Gridlet #" + gridletId + " for User #" + userId);
	        }

	        // sends back an ack if required
	        if (ack)
	        {
	            super.sendAck(GridSimTags.GRIDLET_PAUSE_ACK, status,
	                          gridletId, userId);
	        }
	    }

	    /**
	     * Moves a Gridlet from this GridResource entity to a different one.
	     * This method will search in both the execution and paused list.
	     * The User ID is important as many Users might have the same Gridlet ID
	     * in the lists.
	     * <p>
	     * If a Gridlet has finished beforehand, then this method will send back
	     * the Gridlet to sender, i.e. the <tt>userId</tt> and sets the
	     * acknowledgment to false (if required).
	     *
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @param destId       a new destination GridResource ID for this Gridlet
	     * @param   ack   	   an acknowledgement, i.e. <tt>true</tt> if wanted to know
	     *        whether this operation is success or not, <tt>false</tt>
	     *        otherwise (don't care)
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @pre destId > 0
	     * @post $none
	     */
	    public synchronized void gridletMove(int gridletId, int userId, int destId, boolean ack)
	    {
	        // cancels the Gridlet
	        ResGridlet rgl = cancel(gridletId, userId);

	        // if the Gridlet is not found
	        if (rgl == null)
	        {
	        	logger.log(Level.INFO, super.resName_ +
	                       ".PerfectCheckpointing.gridletMove(): Cannot find " +
	                       "Gridlet #" + gridletId + " for User #" + userId);

	            if (ack)   // sends back an ack if required
	            {
	                super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, false,
	                              gridletId, userId);
	            }

	            return;
	        }

	        // if the Gridlet has finished beforehand
	        if (rgl.getGridletStatus() == Gridlet.SUCCESS)
	        {
	        	logger.log(Level.INFO, super.resName_
	                + ".PerfectCheckpointing.gridletMove(): Cannot move Gridlet #"
	                + gridletId + " for User #"+ userId+ " since it has FINISHED.");

	            if (ack) // sends back an ack if required
	            {
	                super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, false,
	                              gridletId, userId);
	            }

	            gridletFinish(rgl, Gridlet.SUCCESS);
	        }
	        else   // otherwise moves this Gridlet to a different GridResource
	        {
	            rgl.finalizeGridlet();

	            // Set PE on which Gridlet finished to FREE
	            super.resource_.setStatusPE( PE.FREE, rgl.getMachineID(),
	                                         rgl.getPEID() );

	            super.gridletMigrate(rgl.getGridlet(), destId, ack);
	            allocateQueueGridlet();
	        }
	    }

	    /**
	     * Resumes a Gridlet only in the paused list.
	     * The User ID is important as many Users might have the same Gridlet ID
	     * in the lists.
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @param   ack   	   an acknowledgment, i.e. <tt>true</tt> if wanted to know
	     *        whether this operation is success or not, <tt>false</tt>
	     *        otherwise (don't care)
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @post $none
	     */
	    public synchronized void gridletResume(int gridletId, int userId, boolean ack)
	    {
	        boolean status = false;

	        // finds the Gridlet in the execution list first
	        int found = gridletPausedList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // removes the Gridlet
	            ResGridlet rgl = (ResGridlet) gridletPausedList_.remove(found);
	            rgl.setGridletStatus(Gridlet.RESUMED);

	            // update the Gridlets up to this point in time
	            updateGridletProcessing();
	            status = true;

	            // if there is an available PE slot, then allocate immediately
	            int machID = rgl.getMachineID();
                allocatePEtoGridlet(rgl,machID);

	            logger.log(Level.INFO, super.resName_ + "PerfectCheckPointing.gridletResume():" +
	                    " Gridlet #" + gridletId + " with User ID #" +
	                    userId + " has been sucessfully RESUMED.");
	        }
	        else
	        {
	        	logger.log(Level.INFO, super.resName_ +
	                    "PerfectCheckPointing.gridletResume(): Cannot find " +
	                    "Gridlet #" + gridletId + " for User #" + userId);
	        }

	        // sends back an ack if required
	        if (ack)
	        {
	            super.sendAck(GridSimTags.GRIDLET_RESUME_ACK, status,
	                          gridletId, userId);
	        }
	    }

	    
	    ///////////////////////////// PRIVATE METHODS /////////////////////

	    /**
	     * Allocates the first Gridlet in the Queue list (if any) to execution list
	     * @pre $none
	     * @post $none
	     */
	    private boolean allocateQueueGridlet()
	    {
	    		boolean success = false;
	        // if there are many Gridlets in the QUEUE, then allocate a
	        // PE to the first Gridlet in the list since it follows FCFS
	        // (First Come First Serve) approach. Then removes the Gridlet from
	        // the Queue list
	        if (gridletQueueList_.size() > 0 &&
	            gridletInExecList_.size() < super.totalPE_)
	        {
	            ResGridlet obj = (ResGridlet) gridletQueueList_.get(0);

	            // allocate the Gridlet into an empty PE slot and remove it from
	            // the queue list
	            success = allocatePEtoGridlet(obj);
	            // BUG: in the SpaceSharedWithFailure.java --> it must remove '!' from sucess 
	            if (success) {
	                gridletQueueList_.remove(obj);
	            }
	        }
            return success;
	    }

	    /**
	     * Updates the execution of all Gridlets for a period of time.
	     * The time period is determined from the last update time up to the
	     * current time. Once this operation is successfull, then the last update
	     * time refers to the current time.
	     * @pre $none
	     * @post $none
	     */

	    private synchronized void updateGridletProcessing()
	    {
	        // Identify MI share for the duration (from last event time)
	        double time = GridSim.clock();
	        double timeSpan = time - lastUpdateTime_;

	        // if current time is the same or less than the last update time,
	        // then ignore
	        if (timeSpan <= 0.0) {
	            return;
	        }

	        // Update Current Time as Last Update
	        lastUpdateTime_ = time;

	        // update the GridResource load
	        int size = gridletInExecList_.size();
	        double load = super.calculateTotalLoad(size);
	        super.addTotalLoad(load);

	        // if no Gridlets in execution then ignore the rest
	        if (size == 0) {
	            return;
	        }

	        ResGridlet obj = null;

	        // a loop that allocates MI share for each Gridlet accordingly
	        Iterator<ResGridlet> iter = gridletInExecList_.iterator();
	        while ( iter.hasNext() )
	        {
	            obj = (ResGridlet) iter.next();
	            // Updates the Gridlet length that is currently being executed
	            load = getMIShare( timeSpan, obj.getMachineID() );
	            obj.updateGridletFinishedSoFar(load);
	        }
	    }
	    /**
	     * Identifies MI share (max and min) each Gridlet gets for
	     * a given timeSpan
	     * @param timeSpan     duration
	     * @param machineId    machine ID that executes this Gridlet
	     * @return  the total MI share that a Gridlet gets for a given
	     *          <tt>timeSpan</tt>
	     * @pre timeSpan >= 0.0
	     * @pre machineId > 0
	     * @post $result >= 0.0
	     */
	    
	    private double getMIShare(double timeSpan, int machineId)
	    {
	        // 1 - localLoad_ = available MI share percentage
	        double localLoad = super.resCalendar_.getCurrentLoad();

	        // each Machine might have different PE Rating compare to another
	        // so much look at which Machine this PE belongs to
	        double totalMI = machineRating_[machineId] * timeSpan * (1 - localLoad);
	        return totalMI;
	    }

	    /**
	     * Allocates a Gridlet into a free PE and sets the Gridlet status into
	     * INEXEC and PE status into busy afterwards
	     * @param rgl  a ResGridlet object
	     * @return <tt>true</tt> if there is an empty PE to process this Gridlet,
	     *         <tt>false</tt> otherwise
	     * @pre rgl != null
	     * @post $none
	     */
	    private boolean allocatePEtoGridlet(ResGridlet rgl)
	    {
	        // IDENTIFY MACHINE which has a free PE and add this Gridlet to it.
	        Machine myMachine = resource_.getMachineWithFreePE();

	        // If a Machine is empty then ignore the rest
	        if (myMachine == null) {
	            return false;
	        }

	        // gets the list of PEs and find one empty PE
	        PEList MyPEList = myMachine.getPEList();
	        int freePE = MyPEList.getFreePEID();

	        // ALLOCATE IMMEDIATELY
	        rgl.setGridletStatus(Gridlet.INEXEC);   // change Gridlet status
	        rgl.setMachineAndPEID(myMachine.getMachineID(), freePE);

	        // add this Gridlet into execution list
	        gridletInExecList_.add(rgl);

	        // Set allocated PE to BUSY status
	        super.resource_.setStatusPE(PE.BUSY, rgl.getMachineID(), freePE);

	        // Identify Completion Time and Set Interrupt
	        int rating = machineRating_[ rgl.getMachineID() ];
	        double time = forecastFinishTime( rating ,
	                                          rgl.getRemainingGridletLength() );

	        int roundUpTime = (int) (time+1);   // rounding up
	        rgl.setFinishTime(roundUpTime);

	        // then send this into itself
	        super.sendInternalEvent(roundUpTime);
	        logger.log(Level.INFO, super.get_name() + ".gridletSubmit(): " +
	                " Gridlet #" + rgl.getGridletID() + " submit to MachineID " +  rgl.getMachineID()+
	                " PE ID " + rgl.getPEID() + ", Time: " + GridSim.clock());

	        return true;
	    }

	    /**
	     * Allocates a Gridlet into a free PE and sets the Gridlet status into
	     * INEXEC and PE status into busy afterwards
	     * @param rgl  a ResGridlet object
	     * @return <tt>true</tt> if there is an empty PE to process this Gridlet,
	     *         <tt>false</tt> otherwise
	     * @pre rgl != null
	     * @post $none
	     */
	    private boolean allocatePEtoGridlet(ResGridlet rgl, int machID)
	    {
	        
	        int freePE = rgl.getPEID();
	        
	        // ALLOCATE IMMEDIATELY
	        rgl.setGridletStatus(Gridlet.INEXEC);   // change Gridlet status
	        rgl.setMachineAndPEID(machID, freePE);

	        // add this Gridlet into execution list
	        gridletInExecList_.add(rgl);

	        // Set allocated PE to BUSY status
	        super.resource_.setStatusPE(PE.BUSY, rgl.getMachineID(), freePE);

	        // Identify Completion Time and Set Interrupt
	        int rating = machineRating_[ rgl.getMachineID() ];
	        double time = forecastFinishTime( rating ,
	                                          rgl.getRemainingGridletLength() );

	        rgl.setFinishTime(time);

	        // then send this into itself
	        super.sendInternalEvent(time);
            logger.log(Level.INFO, super.get_name() + ".gridletResume(): " +
	                " Gridlet #" + rgl.getGridletID() + " resume on MachineID " +  rgl.getMachineID()+
	                " PE ID " + rgl.getPEID()+ ", Time: " + GridSim.clock());

	        return true;
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
	    private static double forecastFinishTime(double availableRating, double length)
	    {
	        double finishTime = (length / availableRating);

	        // This is as a safeguard since the finish time can be extremely
	        // small close to 0.0, such as 4.5474735088646414E-14. Hence causing
	        // some Gridlets never to be finished and consequently hang the program
	        if (finishTime < 1.0) {
	            finishTime = 1.0;
	        }

	        return finishTime;
	    }

	    /**
	     * Checks all Gridlets in the execution list whether they are finished or
	     * not.
	     * @pre $none
	     * @post $none
	     */
	    private synchronized void checkGridletCompletion()
	    {
	        ResGridlet obj = null;
	        int i = 0;
	        double len;

	        // NOTE: This one should stay as it is since gridletFinish()
	        // will modify the content of this list if a Gridlet has finished.
	        // Can't use iterator since it will cause an exception
	        while ( i < gridletInExecList_.size() )
	        {
	            obj = (ResGridlet) gridletInExecList_.get(i);
	            len = obj.getRemainingGridletLength();
	            if (len == 0.0)
	            {
	                gridletInExecList_.remove(obj);
	                gridletFinish(obj, Gridlet.SUCCESS);
	                continue;
	            }

	            i++;
	        }

	        // if there are still Gridlets left in the execution
	        // then send this into itself for an hourly interrupt
	        // NOTE: Setting the internal event time too low will make the
	        //       simulation more realistic, BUT will take longer time to
	        //       run this simulation. Also, size of sim_trace will be HUGE!
	        if (gridletInExecList_.size() > 0) {
	            super.sendInternalEvent(60.0*60.0);
	        }
	    }

	    /**
	     * Updates the Gridlet's properties, such as status once a
	     * Gridlet is considered finished.
	     * @param rgl   a ResGridlet object
	     * @param status   the Gridlet status
	     * @pre rgl != null
	     * @pre status >= 0
	     * @post $none
	     */
	    private void gridletFinish(ResGridlet rgl, int status)
	    {
	        // Set PE on which Gridlet finished to FREE
	    	int id = rgl.getPEID();
	    	int mid = rgl.getMachineID();
	    	if (super.resource_.getMachineList().getMachineInPos(mid).getFailed()){
		    	super.resource_.getMachineList().getMachineInPos(mid).setFailed(false);  		
	    	}
	    	super.resource_.setStatusPE(PE.FREE, rgl.getMachineID(), id);

	        // the order is important! Set the status first then finalize
	        // due to timing issues in ResGridlet class
	        rgl.setGridletStatus(status);
	        rgl.finalizeGridlet();
	        super.sendFinishGridlet( rgl.getGridlet() );

	        allocateQueueGridlet();   // move Queued Gridlet into exec list
	    }

	    /**
	     * Handles an operation of canceling a Gridlet in either execution list
	     * or paused list.
	     * @param gridletId    a Gridlet ID
	     * @param userId       the user or owner's ID of this Gridlet
	     * @return a ResGridlet or <tt>null</tt> if this Gridlet is not found
	     * @pre gridletId > 0
	     * @pre userId > 0
	     * @post $none
	     */
	    private ResGridlet cancel(int gridletId, int userId)
	    {
	        ResGridlet rgl = null;

	        // Find in EXEC List first
	        int found = gridletInExecList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            // update the gridlets in execution list up to this point in time
	            updateGridletProcessing();

	            // Get the Gridlet from the execution list
	            rgl = (ResGridlet) gridletInExecList_.remove(found);

	            // if a Gridlet is finished upon cancelling, then set it to success
	            // instead.
	            if (rgl.getRemainingGridletLength() == 0.0) {
	                rgl.setGridletStatus(Gridlet.SUCCESS);
	            }
	            else {
	                rgl.setGridletStatus(Gridlet.CANCELED);
	            }

	            // Set PE on which Gridlet finished to FREE
	            super.resource_.setStatusPE( PE.FREE, rgl.getMachineID(),
	                                        rgl.getPEID() );
	            allocateQueueGridlet();
	            return rgl;
	        }

	        // Find in QUEUE list
	        found = gridletQueueList_.indexOf(gridletId, userId);
	        if (found >= 0)
	        {
	            rgl = (ResGridlet) gridletQueueList_.remove(found);
	            rgl.setGridletStatus(Gridlet.CANCELED);
	        }

	        // if not, then find in the Paused list
	        else
	        {
	            found = gridletPausedList_.indexOf(gridletId, userId);

	            // if found in Paused list
	            if (found >= 0)
	            {
	                rgl = (ResGridlet) gridletPausedList_.remove(found);
	                rgl.setGridletStatus(Gridlet.CANCELED);
	            }

	        }
	        return rgl;
	    }


	    /**
	     * Sets the status of all Gridlets in this machine to <tt>PAUSED</tt>.
	     * 
	     * @param failedMachID  the id of the failed machine
	     */
	    public synchronized void setGridletsFailed(int failedMachID)
	    {

	        int gridletInExecList_size = gridletInExecList_.size();
	        ResGridlet rgl;
	        Gridlet gl;

	        // update the Gridlets up to this point in time
            updateGridletProcessing();

	        // go on with the gridlet in InExec list.
	        int machID;
	        for (int i = 0; i < gridletInExecList_size; i++) {
	            rgl = (ResGridlet) gridletInExecList_.get(i);
	            machID = rgl.getMachineID();

	            // only fail gridlets allocated to the machines which have failed
	            if (machID == failedMachID)
	            {
	                	gl = rgl.getGridlet();                	
	                	gridletPause(gl.getGridletID(),gl.getUserID(),false);               	
	                	return;             	

	            }

	            }
	    }
	    
	    /**
	     * Sets the status of all Gridlets in this machine to <tt>RESUMED</tt>.
	     * 
	     * @param failedMachID  the id of the failed machine
	     * @return <tt>true</tt> if could resume the gridlet. <tt>false</tt> if there is no gridlet to resume.
	     */
	    public synchronized boolean setGridletsResumed(int failedMachID)
	    {

	        int gridletInPausedList_size = gridletPausedList_.size();
	        ResGridlet rgl;
	        Gridlet gl;

            // update the Gridlets up to this point in time
            updateGridletProcessing();

	        // go on with the gridlet in InExec list.
	        int machID;
	        for (int i = 0; i < gridletInPausedList_size; i++) {
	            rgl = (ResGridlet) gridletPausedList_.get(i);
	            machID = rgl.getMachineID();

	            // only fail gridlets allocated to the machines which have failed
	            if (machID == failedMachID)
	            {
	                int status = rgl.getGridletStatus();

	                // if the gridlet has already finished, then just send it back.
	                // Otherwise, set status to RESUMED
	                if (status == Gridlet.PAUSED)
	                {
               	
	                	// resume the execution due to recovery
	                	gl = rgl.getGridlet();
	                	
	                	gridletResume(gl.getGridletID(),gl.getUserID(),false);
	                	return true;
                	
	                }

	            }
	        }
	        // no any paused gridlet for this machine        
	        if (gridletQueueList_.size() > 0) {
	            super.sendInternalEvent(0.0);
	        }
			return false;
	    }

	}

