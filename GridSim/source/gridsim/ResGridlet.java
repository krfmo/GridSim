/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * GridSim ResGridlet represents a Gridlet submitted to GridResource for
 * processing. This class keeps track the time for all activities in the
 * GridResource for a specific Gridlet. Before a Gridlet exits the
 * GridResource, it is RECOMMENDED to call this method
 * {@link #finalizeGridlet()}.
 * <p>
 * It contains a Gridlet object along with its arrival time and
 * the ID of the machine and the PE (Processing Element) allocated to it.
 * It acts as a placeholder for maintaining the amount of resource share
 * allocated at various times for simulating any
 * scheduling using internal events.
 *
 * @author       Manzur Murshed and Rajkumar Buyya (modified by Bahman Javadi)
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class ResGridlet
{
    private Gridlet gridlet_;       // a Gridlet object
    private double arrivalTime_;    // Gridlet arrival time for the first time
    private double finishedTime_;   // estimation of Gridlet finished time
    private double gridletFinishedSoFar_;  // length of Gridlet finished so far

    // Gridlet execution start time. This attribute will only hold the latest
    // time since a Gridlet can be cancel, paused or resumed.
    private double startExecTime_;
    
    //************** added by: Bahman Javadi
    // Gridlet first execution start time.
    // This attribute helps to find the run time after pausing and resuming 
    private double startExecTimeFirst;
    private boolean FirstTime = true;
   //**************************************
    
    private double totalCompletionTime_;  // total time to complete this Gridlet

    // The below attributes are only be used by the SpaceShared policy
    private int machineID_;   // machine id this Gridlet is assigned to
    private int peID_;        // PE id this Gridlet is assigned to

    private int[] machineArrayID_ = null;   // an array of machine IDs
    private int[] peArrayID_ = null;        // an array of PE IDs
    private int index_;                     // index of machine and PE arrays

    // NOTE: Below attributes are related to AR stuff
    private static final int NOT_FOUND = -1;
    private long startTime_;  // reservation start time
    private int duration_;    // reservation duration time
    private int reservID_;    // reservation id
    private int numPE_;       // num PE needed to execute this Gridlet


    /**
     * Allocates a new ResGridlet object upon the arrival of a Gridlet object.
     * The arriving time is determined by {@link gridsim.GridSim#clock()}.
     * @param gridlet a gridlet object
     * @see gridsim.GridSim#clock()
     * @pre gridlet != null
     * @post $none
     */
    public ResGridlet(Gridlet gridlet)
    {
        // when a new ResGridlet is created, then it will automatically set
        // the submission time and other properties, such as remaining length
        this.gridlet_ = gridlet;
        this.startTime_ = 0;
        this.reservID_ = NOT_FOUND;
        this.duration_ = 0;

        init();
    }

    /**
     * Allocates a new ResGridlet object upon the arrival of a Gridlet object.
     * Use this constructor to store reserved Gridlets, i.e. Gridlets that
     * done reservation before.
     * The arriving time is determined by {@link gridsim.GridSim#clock()}.
     * @param gridlet   a gridlet object
     * @param startTime   a reservation start time. Can also be interpreted
     *                    as starting time to execute this Gridlet.
     * @param duration    a reservation duration time. Can also be interpreted
     *                    as how long to execute this Gridlet.
     * @param reservID    a reservation ID that owns this Gridlet
     * @see gridsim.GridSim#clock()
     * @pre gridlet != null
     * @pre startTime > 0
     * @pre duration > 0
     * @pre reservID > 0
     * @post $none
     */
    public ResGridlet(Gridlet gridlet,long startTime,int duration,int reservID)
    {
        this.gridlet_ = gridlet;
        this.startTime_ = startTime;
        this.reservID_ = reservID;
        this.duration_ = duration;

        init();
    }

    /**
     * Gets the Gridlet or reservation start time.
     * @return Gridlet's starting time
     * @pre $none
     * @post $none
     */
    public long getStartTime() {
        return startTime_;
    }

    /**
     * Gets the reservation duration time.
     * @return reservation duration time
     * @pre $none
     * @post $none
     */
    public int getDurationTime() {
        return duration_;
    }

    /**
     * Gets the number of PEs required to execute this Gridlet.
     * @return  number of PE
     * @pre $none
     * @post $none
     */
    public int getNumPE() {
        return numPE_;
    }

    /**
     * Gets the reservation ID that owns this Gridlet
     * @return a reservation ID
     * @pre $none
     * @post $none
     */
    public int getReservationID() {
        return reservID_;
    }

    /**
     * Checks whether this Gridlet is submitted by reserving or not.
     * @return <tt>true</tt> if this Gridlet has reserved before,
     *         <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean hasReserved()
    {
        if (reservID_ == NOT_FOUND) {
            return false;
        }

        return true;
    }

    /**
     * Initialises all local attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        // get number of PEs required to run this Gridlet
        this.numPE_ = gridlet_.getNumPE();

        // if more than 1 PE, then create an array
        if (numPE_ > 1)
        {
            this.machineArrayID_ = new int[numPE_];
            this.peArrayID_ = new int[numPE_];
        }

        this.arrivalTime_ = GridSim.clock();
        this.gridlet_.setSubmissionTime(arrivalTime_);

        // default values
        this.finishedTime_ = NOT_FOUND;  // Cannot finish in this hourly slot.
        this.machineID_ = NOT_FOUND;
        this.peID_ = NOT_FOUND;
        this.index_ = 0;
        this.totalCompletionTime_ = 0.0;
        this.startExecTime_ = 0.0;

        // In case a Gridlet has been executed partially by some other grid
        // resources.
        this.gridletFinishedSoFar_ = gridlet_.getGridletFinishedSoFar();
    }

    /**
     * Gets this Gridlet entity Id
     * @return the Gridlet entity Id
     * @pre $none
     * @post $none
     */
    public int getGridletID() {
        return gridlet_.getGridletID();
    }

    /**
     * Gets the user or owner of this Gridlet
     * @return the Gridlet's user Id
     * @pre $none
     * @post $none
     */
    public int getUserID() {
        return gridlet_.getUserID();
    }

    /**
     * Gets the Gridlet's length
     * @return Gridlet's length
     * @pre $none
     * @post $none
     */
    public double getGridletLength() {
        return gridlet_.getGridletLength();
    }

    /**
     * Gets the Gridlet's class type
     * @return class type of the Gridlet
     * @pre $none
     * @post $none
     */
    public int getGridletClassType() {
        return gridlet_.getClassType() ;
    }

    /**
     * Sets the Gridlet status.
     * @param status  the Gridlet status
     * @return <tt>true</tt> if the new status has been set, <tt>false</tt>
     *         otherwise
     * @pre status >= 0
     * @post $none
     */
    public boolean setGridletStatus(int status)
    {
        // gets Gridlet's previous status
        int prevStatus = gridlet_.getGridletStatus();

        // if the status of a Gridlet is the same as last time, then ignore
        if (prevStatus == status) {
            return false;
        }

        boolean success = true;
        try
        {
            double clock = GridSim.clock();   // gets the current clock

            // sets Gridlet's current status
            gridlet_.setGridletStatus(status);

            // if a previous Gridlet status is INEXEC
            if (prevStatus == Gridlet.INEXEC)
            {
                // and current status is either CANCELED, PAUSED or SUCCESS
                if (status == Gridlet.CANCELED || status == Gridlet.PAUSED ||
                    status == Gridlet.SUCCESS)
                {
                    // then update the Gridlet completion time
                    totalCompletionTime_ += (clock - startExecTime_);
                    index_ = 0;
                    startExecTime_ = startExecTimeFirst;
                    gridlet_.setExecStartTime(startExecTime_);

                    return true;
                }
            }

            if (prevStatus == Gridlet.RESUMED && status == Gridlet.SUCCESS)
            {
                // then update the Gridlet completion time
                totalCompletionTime_ += (clock - startExecTime_);
                startExecTime_ = startExecTimeFirst;
                gridlet_.setExecStartTime(startExecTime_);

                return true;
            }

            // if a Gridlet is now in execution
            if (status == Gridlet.INEXEC ||
                (prevStatus == Gridlet.PAUSED && status == Gridlet.RESUMED) )
            {
                startExecTime_ = clock;
                if (FirstTime){
                	startExecTimeFirst = clock;
                	FirstTime = false;
                }
                gridlet_.setExecStartTime(startExecTime_);
            }
        }
        catch(Exception e) {
            success = false;
        }

        return success;
    }

    /**
     * Gets the Gridlet's execution start time
     * @return Gridlet's execution start time
     * @pre $none
     * @post $none
     */
    public double getExecStartTime() {
        return gridlet_.getExecStartTime();
    }

    /**
     * Sets this Gridlet's execution parameters. These parameters are set by
     * the GridResource before departure or sending back to the original
     * Gridlet's owner.
     *
     * @param wallClockTime    the time of this Gridlet resides in
     *                         a GridResource (from arrival time until
     *                         departure time).
     * @param actualCPUTime    the total execution time of this Gridlet in a
     *                         GridResource.
     * @pre wallClockTime >= 0.0
     * @pre actualCPUTime >= 0.0
     * @post $none
     */
    public void setExecParam(double wallClockTime, double actualCPUTime) {
        gridlet_.setExecParam(wallClockTime, actualCPUTime);
    }

    /**
     * Sets the machine and PE (Processing Element) ID
     * @param machineID   machine ID
     * @param peID        PE ID
     * @pre machineID >= 0
     * @pre peID >= 0
     * @post $none
     */
    public void setMachineAndPEID(int machineID, int peID)
    {
        // if this job only requires 1 PE
        this.machineID_ = machineID;
        this.peID_ = peID;
        
        // if this job requires many PEs
        if (this.peArrayID_ != null && this.numPE_ > 1)
        {
            this.machineArrayID_[index_] = machineID;
            this.peArrayID_[index_] = peID;
            index_++;
        }
    }

    /**
     * Gets machine ID
     * @return machine ID or <tt>-1</tt> if it is not specified before
     * @pre $none
     * @post $result >= -1
     */
    public int getMachineID() {
        return machineID_;
    }

    /**
     * Gets PE ID
     * @return PE ID or <tt>-1</tt> if it is not specified before
     * @pre $none
     * @post $result >= -1
     */
    public int getPEID() {
        return peID_;
    }

    /**
     * Gets a list of PE IDs. <br>
     * NOTE: To get the machine IDs corresponding to these PE IDs, use
     * {@link #getListMachineID()}.
     *
     * @return an array containing PE IDs.
     * @pre $none
     * @post $none
     */
    public int[] getListPEID() {
        return peArrayID_;
    }

    /**
     * Gets a list of Machine IDs. <br>
     * NOTE: To get the PE IDs corresponding to these machine IDs, use
     * {@link #getListPEID()}.
     *
     * @return an array containing Machine IDs.
     * @pre $none
     * @post $none
     */
    public int[] getListMachineID() {
        return machineArrayID_;
    }

    /**
     * Gets the remaining gridlet length
     * @return gridlet length
     * @pre $none
     * @post $result >= 0
     */
    public double getRemainingGridletLength()
    {
        double length = gridlet_.getGridletLength() - gridletFinishedSoFar_;

        // Remaining Gridlet length can't be negative number. This can be
        // happening when this.updateGridletFinishedSoFar() keep calling.
        if (length < 0.0) {
            length = 0.0;
        }

        return length;
    }

    /**
     * Finalizes all relevant information before <tt>exiting</tt> the
     * GridResource entity. This method sets the final data of:
     * <ul>
     *     <li> wall clock time, i.e. the time of this Gridlet resides in
     *          a GridResource (from arrival time until departure time).
     *     <li> actual CPU time, i.e. the total execution time of this
     *          Gridlet in a GridResource.
     *     <li> Gridlet's finished so far
     * </ul>
     * @pre $none
     * @post $none
     */
    public void finalizeGridlet()
    {
        // Sets the wall clock time and actual CPU time
        double wallClockTime = GridSim.clock() - arrivalTime_;
        gridlet_.setExecParam(wallClockTime, totalCompletionTime_);

        double finished = 0.0;
        if (gridlet_.getGridletLength() < gridletFinishedSoFar_) {
            finished = gridlet_.getGridletLength();
        }
        else {
            finished = gridletFinishedSoFar_;
        }

        gridlet_.setGridletFinishedSoFar(finished);
    }

    /**
     * A method that updates the length of gridlet that has been completed
     * @param miLength gridlet length in Million Instructions (MI)
     * @pre miLength >= 0.0
     * @post $none
     */
    public void updateGridletFinishedSoFar(double miLength) {
        gridletFinishedSoFar_ += miLength;
    }

    /**
     * Gets arrival time of a gridlet
     * @return arrival time
     * @pre $none
     * @post $result >= 0.0
     */
    public double getGridletArrivalTime() {
        return arrivalTime_;
    }

    /**
     * Sets the finish time for this Gridlet. If time is negative, then it is
     * being ignored.
     * @param time   finish time
     * @pre time >= 0.0
     * @post $none
     */
    public void setFinishTime(double time)
    {
        if (time < 0.0) {
            return;
        }

        finishedTime_ = time;
    }

    /**
     * Gets the Gridlet's finish time
     * @return finish time of a gridlet or <tt>-1.0</tt> if
     *         it cannot finish in this hourly slot
     * @pre $none
     * @post $result >= -1.0
     */
    public double getGridletFinishTime() {
        return finishedTime_;
    }

    /**
     * Gets this Gridlet object
     * @return gridlet object
     * @pre $none
     * @post $result != null
     */
    public Gridlet getGridlet() {
        return gridlet_;
    }

    /**
     * Gets the Gridlet status
     * @return Gridlet status
     * @pre $none
     * @post $none
     */
    public int getGridletStatus() {
        return gridlet_.getGridletStatus();
    }

} 

