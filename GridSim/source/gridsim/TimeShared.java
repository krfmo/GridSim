/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;

import java.util.Calendar;
import java.util.Iterator;


/**
 * TimeShared class is an allocation policy for GridResource that behaves
 * similar to a round robin algorithm, except that all Gridlets are
 * executed at the same time.
 * This is a basic and simple
 * scheduler that runs each Gridlet to one Processing Element (PE).
 * If a Gridlet requires more than one PE, then this scheduler only assign
 * this Gridlet to one PE.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio (re-written this class)
 * @author 		 Marcos Dias de Assuncao (has made some methods synchronized)
 * @since        GridSim Toolkit 2.2
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @invariant $none
 */
class TimeShared extends AllocPolicy
{
    private ResGridletList gridletInExecList_;  // storing exec Gridlets
    private ResGridletList gridletPausedList_;  // storing Paused Gridlets
    private double lastUpdateTime_;   // a timer to denote the last update time
    private MIShares share_;   // a temp variable


    /**
     * Allocates a new TimeShared object
     * @param resourceName    the GridResource entity name that will contain
     *                        this allocation policy
     * @param entityName      this object entity name
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre resourceName != null
     * @pre entityName != null
     * @post $none
     */
    TimeShared(String resourceName, String entityName) throws Exception
    {
        super(resourceName, entityName);

        // initialises local data structure
        this.gridletInExecList_ = new ResGridletList();
        this.gridletPausedList_ = new ResGridletList();
        this.share_ = new MIShares();
        this.lastUpdateTime_ = 0.0;
    }

    ////////////////////// INTERNAL CLASS /////////////////////////////////

    /**
     * Gridlets MI share in Time Shared Mode
     */
    private class MIShares
    {
        /**  maximum amount of MI share Gridlets can get */
        public double max;

        /** minimum amount of MI share Gridlets can get when
         * it is executed on a PE that runs one extra Gridlet
         */
        public double min;

        /** Total number of Gridlets that get Max share */
        public int maxCount;

        /**
         * Default constructor that initializes all attributes to 0
         * @pre $none
         * @post $none
         */
        public MIShares()
        {
            max = 0.0;
            min = 0.0;
            maxCount = 0;
        }
    } // end of internal class

    /////////////////////// End of Internal Class /////////////////////////

    /**
     * Handles internal events that are coming to this entity.
     * @pre $none
     * @post $none
     */
    public void body()
    {
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
            if (ev.get_src() == super.myId_) {
                internalEvent();
            }
        }

        // CHECK for ANY INTERNAL EVENTS WAITING TO BE PROCESSED
        while (super.sim_waiting() > 0)
        {
            // wait for event and ignore since it is likely to be related to
            // internal event scheduled to update Gridlets processing
            super.sim_get_next(ev);
            System.out.println(super.resName_ +
                               ".TimeShared.body(): ignoring internal events");
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
        // update Gridlets in execution up to this point in time
        updateGridletProcessing();

        // reset number of PE since at the moment, it is not supported
        if (gl.getNumPE() > 1)
        {
            String userName = GridSim.getEntityName( gl.getUserID() );
            System.out.println();
            System.out.println(super.get_name() + ".gridletSubmit(): " +
                " Gridlet #" + gl.getGridletID() + " from " + userName +
                " user requires " + gl.getNumPE() + " PEs.");
            System.out.println("--> Process this Gridlet to 1 PE only.");
            System.out.println();

            // also adjusted the length because the number of PEs are reduced
            int numPE = gl.getNumPE();
            double len = gl.getGridletLength();
            gl.setGridletLength(len*numPE);
            gl.setNumPE(1);
        }

        // adds a Gridlet to the in execution list
        ResGridlet rgl = new ResGridlet(gl);
        rgl.setGridletStatus(Gridlet.INEXEC); // set the Gridlet status to exec
        gridletInExecList_.add(rgl);   // add into the execution list

        // sends back an ack if required
        if (ack)
        {
            super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
                          gl.getGridletID(), gl.getUserID()
            );
        }

        // forecast all Gridlets in the execution list
        forecastGridlet();
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
    public synchronized int gridletStatus(int gridletId, int userId)
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

        // if not found then find again in Paused List
        found = gridletPausedList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            // Get the Gridlet from the execution list
            rgl = (ResGridlet) gridletPausedList_.get(found);
            return rgl.getGridletStatus();
        }

        // if not found in all lists
        return -1;
    }

    /**
     * Cancels a Gridlet running in this entity.
     * This method will search the execution and paused list. The User ID is
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
        // Finds the gridlet in execution and paused list
        ResGridlet rgl = cancel(gridletId, userId);

        // If not found in both lists then report an error and sends back
        // an empty Gridlet
        if (rgl == null)
        {
            System.out.println(super.resName_ +
                    ".TimeShared.gridletCancel(): Cannot find " +
                    "Gridlet #" + gridletId + " for User #" + userId);

            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, null,
                                    gridletId, userId);
            return;
        }

        // if a Gridlet is found
        rgl.finalizeGridlet();     // finalise Gridlet

        // if a Gridlet has finished execution before canceling, the reports
        // an error msg
        if (rgl.getGridletStatus() == Gridlet.SUCCESS)
        {
            System.out.println(super.resName_
                    + ".TimeShared.gridletCancel(): Cannot cancel"
                    + " Gridlet #" + gridletId + " for User #" + userId
                    + " since it has FINISHED.");
        }

        // sends the Gridlet back to sender
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, rgl.getGridlet(),
                                gridletId, userId);
    }

    /**
     * Pauses a Gridlet only if it is currently executing.
     * This method will search in the execution list. The User ID is
     * important as many users might have the same Gridlet ID in the lists.
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public synchronized void gridletPause(int gridletId, int userId, boolean ack)
    {
        boolean status = false;

        // find this Gridlet in the execution list
        int found = gridletInExecList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            // update Gridlets in execution list up to this point in time
            updateGridletProcessing();

            // get a Gridlet from execution list
            ResGridlet rgl = (ResGridlet) gridletInExecList_.remove(found);

            // if a Gridlet is finished upon pausing, then set it to success
            // instead.
            if (rgl.getRemainingGridletLength() == 0.0)
            {
                System.out.println(super.resName_
                        + ".TimeShared.gridletPause(): Cannot pause"
                        + " Gridlet #" + gridletId + " for User #" + userId
                        + " since it is FINISHED.");

                gridletFinish(rgl, Gridlet.SUCCESS);
            }
            else
            {
                status = true;
                rgl.setGridletStatus(Gridlet.PAUSED);

                // add the Gridlet into the paused list
                gridletPausedList_.add(rgl);
                System.out.println(super.resName_ +
                    ".TimeShared.gridletPause(): Gridlet #" + gridletId +
                    " with User #" + userId + " has been sucessfully PAUSED.");
            }

            // forecast all Gridlets in the execution list
            forecastGridlet();
        }
        else   // if not found in the execution list
        {
            System.out.println(super.resName_ +
                    ".TimeShared.gridletPause(): Cannot find " +
                    "Gridlet #" + gridletId + " for User #" + userId);
        }

        // sends back an ack
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
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @pre destId > 0
     * @post $none
     */
    public synchronized void gridletMove(int gridletId, int userId, int destId, boolean ack)
    {
        // cancel the Gridlet first
        ResGridlet rgl = cancel(gridletId, userId);

        // If no found then print an error msg
        if (rgl == null)
        {
            System.out.println(super.resName_ +
                    ".TimeShared.gridletMove(): Cannot find " +
                    "Gridlet #" + gridletId + " for User #" + userId);

            if (ack)   // sends ack that this operation fails
            {
                super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, false,
                              gridletId, userId);
            }
            return;
        }

        // if found
        rgl.finalizeGridlet();   // finalise Gridlet
        Gridlet gl = rgl.getGridlet();

        // if a Gridlet has finished execution
        if (gl.getGridletStatus() == Gridlet.SUCCESS)
        {
            System.out.println(super.resName_
                    + ".TimeShared.gridletMove(): Cannot move"
                    + " Gridlet #" + gridletId + " for User #" + userId
                    + " since it has FINISHED.");

            if (ack)
            {
                super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, false, gridletId,
                              userId);
            }

            super.sendFinishGridlet(gl);   // sends the Gridlet back to sender
        }
        // moves this Gridlet to another GridResource entity
        else {
            super.gridletMigrate(gl, destId, ack);
        }
    }

    /**
     * Resumes a Gridlet only in the paused list.
     * The User ID is important as many Users might have the same Gridlet ID
     * in the lists.
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public synchronized void gridletResume(int gridletId, int userId, boolean ack)
    {
        boolean success = false;

        // finds in the execution list first
        int found = gridletPausedList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            // need to update Gridlets in execution up to this point in time
            updateGridletProcessing();

            // remove a Gridlet from paused list and change the status
            ResGridlet rgl = (ResGridlet) gridletPausedList_.remove(found);
            rgl.setGridletStatus(Gridlet.RESUMED);

            // add the Gridlet back to in execution list
            gridletInExecList_.add(rgl);

            // then forecast Gridlets in execution list
            forecastGridlet();

            success = true;
            System.out.println(super.resName_ +
                    ".TimeShared.gridletResume(): Gridlet #" + gridletId +
                    " with User #" + userId + " has been sucessfully RESUMED.");
        }
        else  // if no found then prints an error msg
        {
            System.out.println(super.resName_ +
                    ".TimeShared.gridletResume(): Cannot find Gridlet #" +
                    gridletId + " for User #" + userId);
        }

        // sends back an ack to sender
        if (ack)
        {
            super.sendAck(GridSimTags.GRIDLET_RESUME_ACK, success,
                          gridletId, userId);
        }
    }


    ////////////////////// PRIVATE METHODS //////////////////////////////

    /**
     * Updates the execution of all Gridlets for a period of time.
     * The time period is determined from the last update time up to the
     * current time. Once this operation is successfull, then the last update
     * time refers to the current time.
     * @pre $none
     * @post $none
     */
    private void updateGridletProcessing()
    {
        // Identify MI share for the duration (from last event time)
        double time = GridSim.clock();
        double timeSpan = time - lastUpdateTime_;

        // if current time is the same or less than the last update time,
        // then ignore
        if (timeSpan <= 0.0) {
            return;
        }

        // Update Current Time as the Last Update
        lastUpdateTime_ = time;

        // update the GridResource load
        int size = gridletInExecList_.size();
        double load = super.calculateTotalLoad(size);
        super.addTotalLoad(load);       // add the current resource load

        // if no Gridlets in execution then ignore the rest
        if (size == 0) {
            return;
        }

        // gets MI Share for all Gridlets
        MIShares shares = getMIShare(timeSpan, size);
        ResGridlet obj = null;

        // a loop that allocates MI share for each Gridlet accordingly
        // In this algorithm, Gridlets at the front of the list
        // (range = 0 until MIShares.maxCount-1) will be given max MI value
        // For example, 2 PEs and 3 Gridlets. PE #0 processes Gridlet #0
        // PE #1 processes Gridlet #1 and Gridlet #2
        int i = 0;  // a counter
        Iterator iter = gridletInExecList_.iterator();
        while ( iter.hasNext() )
        {
            obj = (ResGridlet) iter.next();

            // Updates the Gridlet length that is currently being executed
            if (i < shares.maxCount) {
                obj.updateGridletFinishedSoFar(shares.max);
            }
            else {
                obj.updateGridletFinishedSoFar(shares.min);
            }

            i++;   // increments i
        }
    }

    /**
     * Identifies MI share (max and min) for all Gridlets in
     * a given time duration
     * @param timeSpan duration
     * @param size    total number of Gridlets in the execution list
     * @return  the total MI share that a Gridlet gets for a given
     *          <tt>timeSpan</tt>
     */
    private MIShares getMIShare(double timeSpan, int size)
    {
        // 1 - localLoad_ = available MI share percentage
        double localLoad = super.resCalendar_.getCurrentLoad();
        double TotalMIperPE = super.resource_.getMIPSRatingOfOnePE() * timeSpan
                              * (1 - localLoad);

        // This TimeShared is not Round Robin where each PE for 1 Gridlet only.
        // a PE can have more than one Gridlet executing.
        // minimum number of Gridlets that each PE runs.
        int glDIVpe = size / super.totalPE_;

        // number of PEs that run one extra Gridlet
        int glMODpe = size % super.totalPE_;

        // If num Gridlets in execution > total PEs in a GridResource,
        // then divide MIShare by the following constraint:
        // - obj.max = MIShare of a PE executing n Gridlets
        // - obj.min = MIShare of a PE executing n+1 Gridlets
        // - obj.maxCount = a threshold number of Gridlets will be assigned to
        //                  max MI value.
        //
        // In this algorithm, Gridlets at the front of the list
        // (range = 0 until maxCount-1) will be given max MI value
        if (glDIVpe > 0)
        {
            // this is for PEs that run one extra Gridlet
            share_.min = TotalMIperPE / (glDIVpe + 1);
            share_.max = TotalMIperPE / glDIVpe;
            share_.maxCount = (super.totalPE_ - glMODpe) * glDIVpe;
        }

        // num Gridlet in Exec < total PEs, meaning it is a
        // full PE share: i.e a PE is dedicated to execute a single Gridlet
        else
        {
            share_.max = TotalMIperPE;
            share_.min = TotalMIperPE;
            share_.maxCount = size;   // number of Gridlet
        }

        return share_;
    }

    /**
     * Determines the smallest completion time of all Gridlets in the execution
     * list. The smallest time is used as an internal event to
     * update Gridlets processing in the future.
     * <p>
     * The algorithm for this method:
     * <ul>
     *     <li> identify the finish time for each Gridlet in the execution list
     *          given the share MIPS rating for all and the remaining Gridlet's
     *          length
     *     <li> find the smallest finish time in the list
     *     <li> send the last Gridlet in the list with
     *          <tt>delay =  smallest finish time - current time</tt>
     * </ul>
     * @pre $none
     * @post $none
     */
    private void forecastGridlet()
    {
        // if no Gridlets available in exec list, then exit this method
        if (gridletInExecList_.size() == 0) {
            return;
        }

        // checks whether Gridlets have finished or not. If yes, then remove
        // them since they will effect the MIShare calculation.
        checkGridletCompletion();

        // Identify MIPS share for all Gridlets for 1 second, considering
        // current Gridlets + No of PEs.
        MIShares share = getMIShare( 1.0, gridletInExecList_.size() );

        ResGridlet rgl = null;
        int i = 0;
        double time = 0.0;
        double rating = 0.0;
        double smallestTime = 0.0;

        // For each Gridlet, determines their finish time
        Iterator iter = gridletInExecList_.iterator();
        while ( iter.hasNext() )
        {
            rgl = (ResGridlet) iter.next();

            // If a Gridlet locates before the max count then it will be given
            // the max. MIPS rating
            if (i < share.maxCount) {
                rating = share.max;
            }
            else {   // otherwise, it will be given the min. MIPS Rating
                rating = share.min;
            }

            time = forecastFinishTime(rating, rgl.getRemainingGridletLength() );

            int roundUpTime = (int) (time+1);   // rounding up
            rgl.setFinishTime(roundUpTime);

            // get the smallest time of all Gridlets
            if (i == 0 || smallestTime > time) {
                smallestTime = time;
            }

            i++;
        }

        // sends to itself as an internal event
        super.sendInternalEvent(smallestTime);
    }

    /**
     * Checks all Gridlets in the execution list whether they are finished or
     * not.
     * @pre $none
     * @post $none
     */
    private void checkGridletCompletion()
    {
        ResGridlet rgl = null;

        // a loop that determine the smallest finish time of a Gridlet
        // Don't use an iterator since it causes an exception because if
        // a Gridlet is finished, gridletFinish() will remove it from the list.
        int i = 0;
        while ( i < gridletInExecList_.size() )
        {
            rgl = (ResGridlet) gridletInExecList_.get(i);

            // if a Gridlet has finished, then remove it from the list
            if (rgl.getRemainingGridletLength() <= 0.0)
            {
                gridletFinish(rgl, Gridlet.SUCCESS);
                continue;  // not increment i coz the list size also decreases
            }

            i++;
        }
    }

    /**
     * Forecast finish time of a Gridlet.
     * <tt>Finish time = length / available rating</tt>
     * @param availableRating   the shared MIPS rating for all Gridlets
     * @param length   remaining Gridlet length
     * @return Gridlet's finish time.
     */
    private static double forecastFinishTime(double availableRating, double length)
    {
        double finishTime = length / availableRating;

        // This is as a safeguard since the finish time can be extremely
        // small close to 0.0, such as 4.5474735088646414E-14. Hence causing
        // some Gridlets never to be finished and consequently hang the program
        if (finishTime < 1.0) {
            finishTime = 1.0;
        }

        return finishTime;
    }

    /**
     * Updates the Gridlet's properties, such as status once a
     * Gridlet is considered finished.
     * @param rgl     a ResGridlet object
     * @param status  the status of this ResGridlet object
     * @pre rgl != null
     * @post $none
     */
    private void gridletFinish(ResGridlet rgl, int status)
    {
        // NOTE: the order is important! Set the status first then finalize
        // due to timing issues in ResGridlet class.
        rgl.setGridletStatus(status);
        rgl.finalizeGridlet();

        // sends back the Gridlet with no delay
        Gridlet gl = rgl.getGridlet();
        super.sendFinishGridlet(gl);

        // remove this Gridlet in the execution
        gridletInExecList_.remove(rgl);
    }

    /**
     * Handles internal event
     * @pre $none
     * @post $none
     */
    private synchronized void internalEvent()
    {
        // this is a constraint that prevents an infinite loop
        // Compare between 2 floating point numbers. This might be incorrect
        // for some hardware platform.
        if ( lastUpdateTime_ == GridSim.clock() ) {
            return;
        }

        // update Gridlets in execution up to this point in time
        updateGridletProcessing();

        // schedule next event
        forecastGridlet();
    }

    /**
     * Handles an operation of canceling a Gridlet in either execution list
     * or paused list.
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @return a ResGridlet object or <tt>null</tt> if this Gridlet is not found
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    private ResGridlet cancel(int gridletId, int userId)
    {
        ResGridlet rgl = null;

        // Check whether the Gridlet is in execution list or not
        int found = gridletInExecList_.indexOf(gridletId, userId);

        // if a Gridlet is in execution list
        if (found >= 0)
        {
            // update the gridlets in execution list up to this point in time
            updateGridletProcessing();

            // Get the Gridlet from the execution list
            rgl = (ResGridlet) gridletInExecList_.remove(found);

            // if a Gridlet is finished upon cancelling, then set it to success
            if (rgl.getRemainingGridletLength() == 0.0) {
                rgl.setGridletStatus(Gridlet.SUCCESS);
            }
            else {
                rgl.setGridletStatus(Gridlet.CANCELED);
            }

            // then forecast the next Gridlet to complete
            forecastGridlet();
        }

        // if a Gridlet is not in exec list, then find it in the paused list
        else
        {
            found = gridletPausedList_.indexOf(gridletId, userId);

            // if a Gridlet is found in the paused list then remove it
            if (found >= 0)
            {
                rgl = (ResGridlet) gridletPausedList_.remove(found);
                rgl.setGridletStatus(Gridlet.CANCELED);
            }
        }

        return rgl;
    }
} 

