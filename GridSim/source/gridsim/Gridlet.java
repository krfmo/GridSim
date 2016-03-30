/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * A Gridlet is a package that contains all the information related to the job
 * and its execution management details such as job length expressed in MI
 * (Millions Instruction), the size of input and output
 * files, and the job owner id.
 * Individual users model their application by creating Gridlets for
 * processing them on Grid resources.
 * <p>
 * These basic parameters help in determining
 * execution time, the time required to transport input and output files between
 * users and remote resources, and returning the processed Gridlets back to the
 * originator along with the results.
 * <p>
 * For a Gridlet that requires many Processing Elements (PEs) or CPUs,
 * the gridlet length is calculated only for 1 PE for simplicity.<br>
 * For example, this Gridlet has a length of 500 MI and requires 2 PEs.
 * This means each PE will execute 500 MI of this Gridlet.
 * <p>
 * From GridSim 3.1, we have also added a classType attribute which can be
 * used to provide differentiated service to scheduling gridlets on a resource.
 * The higher the classType means the higher the priority of scheduling this
 * Gridlet on the resource.
 * However, it is up to the resource's allocation policy to schedule gridlets
 * based on their priority or not.
 * <p>
 * By default, this object records a history of every activity, such as
 * when this Gridlet is being put into a queue, and
 * when it is being executed by a resource.
 * You can find the history by using {@link #getGridletHistory()}.<br>
 * However, this approach requires a significant amount of memory if you have
 * many Gridlet objects. To disable this functionality, use
 * {@link #Gridlet(int, double, long, long, boolean)} constructor instead.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class Gridlet
{
    // the User or Broker ID. It is advisable that broker set this ID
    // with its own ID, so that GridResource returns to it after the execution
    private int userID_;

    // the size of this Gridlet to be executed in a GridResource (unit: in MI)
    private double gridletLength_;

    // the input file size of this Gridlet before execution (unit: in byte)
    private long gridletFileSize_;   // in byte = program + input data size

    // the output file size of this Gridlet after execution (unit: in byte)
    private long gridletOutputSize_;

    private int numPE_;              // num of PE required to execute this job
    private int gridletID_;          // this Gridlet ID
    private int status_;             // status of this Gridlet
    private DecimalFormat num_;      // to format the decimal number
    private double finishTime_;      // the time where this Gridlet completes

    // start time of executing this Gridlet.
    // With new functionalities, such as CANCEL, PAUSED and RESUMED, this
    // attribute only stores the latest execution time. Previous execution time
    // are ignored.
    private double execStartTime_;   // in simulation time
    private int reservationId_ = -1; // the ID of a reservation made for this gridlet

    // records the transaction history for this Gridlet
    private boolean record_;         // record a history or not
    private String newline_;
    private StringBuffer history_;
    private ArrayList<Resource> resList_;
    private int index_;

    // differentiated service
    private int classType_;    // class type of Gridlet for resource scheduling
    private int netToS_;       // ToS for sending Gridlet over the network


    ////////////////////////////////////////////
    // Below are CONSTANTS attributes
    /** The Gridlet has been created and added to the GridletList object */
    public static final int CREATED = 0;

    /** The Gridlet has been assigned to a GridResource object as planned */
    public static final int READY = 1;

    /** The Gridlet has moved to a Grid node */
    public static final int QUEUED = 2;

    /** The Gridlet is in execution in a Grid node */
    public static final int INEXEC = 3;

    /** The Gridlet has been executed successfully */
    public static final int SUCCESS = 4;

    /** The Gridlet is failed */
    public static final int FAILED = 5;

    /** The Gridlet has been canceled.  */
    public static final int CANCELED = 6;

    /** The Gridlet has been paused. It can be resumed by changing the status
     * into <tt>RESUMED</tt>.
     */
    public static final int PAUSED = 7;

    /** The Gridlet has been resumed from <tt>PAUSED</tt> state. */
    public static final int RESUMED = 8;

    /** The gridlet has failed due to a resource failure */
    public static final int FAILED_RESOURCE_UNAVAILABLE = 9;


    /////////////////////////////////////////////////////////////

    /**
     * Allocates a new Gridlet object. The Gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     * By default, this constructor records the history of this object.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @param classType            Sets the class type or priority of this
     *                             gridlet for scheduling on a resource.
     * @pre gridletID >= 0
     * @pre gridletLength >= 0.0
     * @pre gridletFileSize >= 1
     * @pre gridletOutputSize >= 1
     * @post $none
     */
    public Gridlet(int gridletID, double gridletLength,
                long gridletFileSize, long gridletOutputSize, int classType)
    {
        this(gridletID,gridletLength,gridletFileSize,gridletOutputSize,true);
        this.classType_ = classType;
    }

    /**
     * Allocates a new Gridlet object. The Gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @param record               record the history of this object or not
     * @pre gridletID >= 0
     * @pre gridletLength >= 0.0
     * @pre gridletFileSize >= 1
     * @pre gridletOutputSize >= 1
     * @post $none
     */
    public Gridlet(int gridletID, double gridletLength,
                long gridletFileSize, long gridletOutputSize, boolean record)
    {
        this.userID_ = -1;          // to be set by a Broker or user
        this.status_ = CREATED;
        this.gridletID_ = gridletID;
        this.numPE_ = 1;
        this.execStartTime_ = 0.0;
        this.finishTime_ = -1.0;    // meaning this Gridlet hasn't finished yet
        this.classType_ = 0;
        this.netToS_ = 0;

        // Gridlet length, Input and Output size should be at least 1 byte.
        this.gridletLength_ = Math.max(1, gridletLength);
        this.gridletFileSize_ = Math.max(1, gridletFileSize);
        this.gridletOutputSize_ = Math.max(1, gridletOutputSize);

        // Normally, a Gridlet is only executed on a resource without being
        // migrated to others. Hence, to reduce memory consumption, set the
        // size of this ArrayList to be less than the default one.
        this.resList_ = new ArrayList<Resource>(2);
        this.index_ = -1;
        this.record_ = record;

        // history will be created later
        this.num_ = null;
        this.history_ = null;
        this.newline_ = null;
    }

    /**
     * Allocates a new Gridlet object. The Gridlet length, input and output
     * file sizes should be greater than or equal to 1.
     * By default this constructor sets the history of this object.
     *
     * @param gridletID            the unique ID of this Gridlet
     * @param gridletLength        the length or size (in MI) of this Gridlet
     *                             to be executed in a GridResource
     * @param gridletFileSize      the file size (in byte) of this Gridlet
     *                             <tt>BEFORE</tt> submitting to a GridResource
     * @param gridletOutputSize    the file size (in byte) of this Gridlet
     *                             <tt>AFTER</tt> finish executing by
     *                             a GridResource
     * @pre gridletID >= 0
     * @pre gridletLength >= 0.0
     * @pre gridletFileSize >= 1
     * @pre gridletOutputSize >= 1
     * @post $none
     */
    public Gridlet(int gridletID, double gridletLength,
                long gridletFileSize, long gridletOutputSize)
    {
        this(gridletID, gridletLength, gridletFileSize, gridletOutputSize, 0);
        this.record_ = true;
    }



    //////////////////////// INTERNAL CLASS ///////////////////////////////////

    /**
     * Internal class that keeps track Gridlet's movement in different
     * GridResources
     */
    private class Resource
    {
        /** Gridlet's submission time to a GridResource */
        double submissionTime = 0.0;

        /** The time of this Gridlet resides in a GridResource
         * (from arrival time until departure time).
         */
        double wallClockTime = 0.0;

        /** The total execution time of this Gridlet in a GridResource. */
        double actualCPUTime = 0.0;

        /** Cost per second a GridResource charge to execute this Gridlet */
        double costPerSec = 0.0;

        /** Gridlet's length finished so far */
        double finishedSoFar = 0.0;

        /** a GridResource id */
        int resourceId = -1;

        /** a GridResource name */
        String resourceName = null;

    } // end of internal class

    //////////////////////// End of Internal Class //////////////////////////
    
    /**
     * Sets the id of the reservation made for this gridlet
     * @param resId the reservation ID
     * @return <tt>true</tt> if the ID has successfully been set or
     * <tt>false</tt> otherwise.
     */
    public boolean setReservationID(int resId) {
    	if(resId <= 0) {
    		return false;
    	}
    	this.reservationId_ = resId;
    	return true;
    }
    
    /**
     * Gets the reservation ID that owns this Gridlet
     * @return a reservation ID
     * @pre $none
     * @post $none
     */
    public int getReservationID() {
        return reservationId_;
    }
    
    /**
     * Checks whether this Gridlet is submitted by reserving or not.
     * @return <tt>true</tt> if this Gridlet has reserved before,
     *         <tt>false</tt> otherwise
     */
    public boolean hasReserved() {
        if (reservationId_ == -1) {
            return false;
        }
        return true;
    }

    /**
     * Sets the length or size (in MI) of this Gridlet
     * to be executed in a GridResource.
     * This Gridlet length is calculated for 1 PE only <tt>not</tt> the total
     * length.
     *
     * @param gridletLength     the length or size (in MI) of this Gridlet
     *                          to be executed in a GridResource
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre gridletLength > 0
     * @post $none
     */
    public boolean setGridletLength(double gridletLength)
    {
        if (gridletLength <= 0) {
            return false;
        }

        gridletLength_ = gridletLength;
        return true;
    }

    /**
     * Sets the network service level for sending this gridlet over a network
     * @param netServiceLevel   determines the kind of service this gridlet
     *                          receives in the network (applicable to
     *                          selected PacketScheduler class only)
     * @return <code>true</code> if successful.
     * @pre netServiceLevel >= 0
     * @post $none
     */
    public boolean setNetServiceLevel(int netServiceLevel)
    {
        boolean success = false;
        if (netServiceLevel > 0)
        {
            netToS_ = netServiceLevel;
            success = true;
        }

        return success;
    }

    /**
     * Gets the network service level for sending this gridlet over a network
     * @return the network service level
     * @pre $none
     * @post $none
     */
    public int getNetServiceLevel() {
        return netToS_;
    }

    /**
     * Gets the waiting time of this gridlet executed on a resource
     * @return the waiting time
     * @pre $none
     * @post $none
     */
    public double getWaitingTime()
    {
        if (index_ == -1) {
            return 0;
        }

        // use the latest resource submission time
        double subTime = resList_.get(index_).submissionTime;
        return execStartTime_ - subTime;
    }

    /**
     * Sets the classType or priority of this Gridlet for scheduling on a
     * resource.
     * @param classType classType of this Gridlet
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre classType > 0
     * @post $none
     */
    public boolean setClassType(int classType)
    {
        boolean success = false;
        if (classType > 0)
        {
            this.classType_ = classType;
            success = true;
        }

        return success;
    }

    /**
     * Gets the classtype or priority of this Gridlet for scheduling on a
     * resource.
     * @return classtype of this gridlet
     * @pre $none
     * @post $none
     */
    public int getClassType() {
        return classType_;
    }

    /**
     * Sets the number of PEs required to run this Gridlet. <br>
     * NOTE: The Gridlet length is computed only for 1 PE for simplicity. <br>
     * For example, this Gridlet has a length of 500 MI and requires 2 PEs.
     * This means each PE will execute 500 MI of this Gridlet.
     *
     * @param numPE     number of PE
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre numPE > 0
     * @post $none
     */
    public boolean setNumPE(int numPE)
    {
        boolean success = false;
        if (numPE > 0)
        {
            numPE_ = numPE;
            success = true;
        }
        return success;
    }

    /**
     * Gets the number of PEs required to run this Gridlet
     * @return number of PEs
     * @pre $none
     * @post $none
     */
    public int getNumPE() {
        return numPE_;
    }

    /**
     * Gets the history of this Gridlet. The layout of this history is in a
     * readable table column with <tt>time</tt> and <tt>description</tt>
     * as headers.
     * @return a String containing the history of this Gridlet object.
     * @pre $none
     * @post $result != null
     */
    public String getGridletHistory()
    {
        String msg = null;
        if (history_ == null) {
            msg = "No history is recorded for Gridlet #" + gridletID_;
        }
        else {
            msg = history_.toString();
        }

        return msg;
    }

    /**
     * Gets the length of this Gridlet that has been executed so far
     * from the latest GridResource. This
     * method is useful when trying to move this Gridlet into different
     * GridResources or to cancel it.
     * @return the length of a partially executed Gridlet or the full Gridlet
     *         length if it is completed
     * @pre $none
     * @post $result >= 0.0
     */
    public double getGridletFinishedSoFar()
    {
        if (index_ == -1) {
            return gridletLength_;
        }

        double finish = resList_.get(index_).finishedSoFar;
        if (finish > gridletLength_) {
            return gridletLength_;
        }

        return finish;
    }

    /**
     * Checks whether this Gridlet has finished execution or not
     * @return <tt>true</tt> if this Gridlet has finished execution,
     *         <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean isFinished()
    {
        if (index_ == -1) {
            return false;
        }

        boolean completed = false;

        // if result is 0 or -ve then this Gridlet has finished
        double finish = resList_.get(index_).finishedSoFar;
        double result = gridletLength_ - finish;
        if (result <= 0.0) {
            completed = true;
        }

        return completed;
    }

    /**
     * Sets the length of this Gridlet that has been executed so far.
     * This method is used by ResGridlet class when an application
     * is decided to cancel or to move this Gridlet into different
     * GridResources.
     * @param length    length of this Gridlet
     * @see gridsim.AllocPolicy
     * @see gridsim.ResGridlet
     * @pre length >= 0.0
     * @post $none
     */
    public void setGridletFinishedSoFar(double length)
    {
        // if length is -ve then ignore
        if (length < 0.0 || index_ < 0) {
            return;
        }

        Resource res = resList_.get(index_);
        res.finishedSoFar = length;

        if (record_) {
            write("Sets the length's finished so far to " + length);
        }
    }

    /**
     * Gets the Gridlet ID
     * @return the Gridlet ID
     * @pre $none
     * @post $result >= 0
     */
    public int getGridletID() {
        return gridletID_;
    }

    /**
     * Sets the user or owner ID of this Gridlet. It is <tt>VERY</tt> important
     * to set the user ID, otherwise this Gridlet will not be executed in a
     * GridResource.
     * @param id  the user ID
     * @pre id >= 0
     * @post $none
     */
    public void setUserID(int id)
    {
        userID_ = id;
        if (record_)
        {
            write("Assigns the Gridlet to " + GridSim.getEntityName(id) +
                  " (ID #" + id + ")");
        }
    }

    /**
     * Gets the user or owner ID of this Gridlet
     * @return the user ID or <tt>-1</tt> if the user ID has not been set before
     * @pre $none
     * @post $result >= -1
     */
    public int getUserID() {
        return userID_;
    }

    /**
     * Gets the latest resource ID that processes this Gridlet
     * @return the resource ID or <tt>-1</tt> if none
     * @pre $none
     * @post $result >= -1
     */
    public int getResourceID()
    {
        if (index_ == -1) {
            return -1;
        }

        return resList_.get(index_).resourceId;
    }

    /**
     * Gets the input file size of this Gridlet <tt>BEFORE</tt>
     * submitting to a GridResource
     * @return the input file size of this Gridlet
     * @pre $none
     * @post $result >= 1
     */
    public long getGridletFileSize() {
        return gridletFileSize_;
    }

    /**
     * Gets the output size of this Gridlet <tt>AFTER</tt> submitting and
     * executing to a GridResource
     * @return the Gridlet output file size
     * @pre $none
     * @post $result >= 1
     */
    public long getGridletOutputSize() {
        return gridletOutputSize_;
    }

    /**
     * Sets the resource parameters for which this Gridlet is going to be
     * executed. <br>
     * NOTE: This method <tt>should</tt> be called only by a resource entity,
     * not the user or owner of this Gridlet.
     * @param resourceID   the GridResource ID
     * @param cost   the cost running this GridResource per second
     * @pre resourceID >= 0
     * @pre cost > 0.0
     * @post $none
     */
    public void setResourceParameter(int resourceID, double cost)
    {
        Resource res = new Resource();
        res.resourceId = resourceID;
        res.costPerSec = cost;
        res.resourceName = GridSim.getEntityName(resourceID);

        // add into a list if moving to a new grid resource
        resList_.add(res);

        if (index_ == -1 && record_)
        {
            write("Allocates this Gridlet to " + res.resourceName +
                  " (ID #" + resourceID + ") with cost = $" + cost + "/sec");
        }
        else if (record_)
        {
            int id = resList_.get(index_).resourceId;
            String name = resList_.get(index_).resourceName;
            write("Moves Gridlet from " + name + " (ID #" + id + ") to " +
                  res.resourceName + " (ID #" + resourceID +
                  ") with cost = $" + cost + "/sec");
        }

        index_++;  // initially, index_ = -1
    }

    /**
     * Sets the submission or arrival time of this Gridlet into a GridResource
     * @param clockTime     the submission time
     * @pre clockTime >= 0.0
     * @post $none
     */
    public void setSubmissionTime(double clockTime)
    {
        if (clockTime < 0.0 || index_ < 0) {
            return;
        }

        Resource res = resList_.get(index_);
        res.submissionTime = clockTime;

        if (record_) {
            write( "Sets the submission time to " + num_.format(clockTime) );
        }
    }

    /**
     * Gets the submission or arrival time of this Gridlet from
     * the latest GridResource
     * @return the submission time or <tt>0.0</tt> if none
     * @pre $none
     * @post $result >= 0.0
     */
    public double getSubmissionTime()
    {
        if (index_ == -1) {
            return 0.0;
        }

        return resList_.get(index_).submissionTime;
    }

    /**
     * Sets the execution start time of this Gridlet inside a GridResource.
     * <b>NOTE:</b> With new functionalities, such as being able to cancel /
     * to pause / to resume this Gridlet, the execution start time only holds
     * the latest one. Meaning, all previous execution start time are ignored.
     *
     * @param clockTime     the latest execution start time
     * @pre clockTime >= 0.0
     * @post $none
     */
    public void setExecStartTime(double clockTime)
    {
        execStartTime_ = clockTime;
        if (record_) {
            write("Sets the execution start time to " + num_.format(clockTime));
        }
    }

    /**
     * Gets the latest execution start time
     * @return the latest execution start time
     * @pre $none
     * @post $result >= 0.0
     */
    public double getExecStartTime() {
        return execStartTime_;
    }

    /**
     * Sets this Gridlet's execution parameters. These parameters are set by
     * the GridResource before departure or sending back to the original
     * Gridlet's owner.
     *
     * @param wallTime    the time of this Gridlet resides in
     *                         a GridResource (from arrival time until
     *                         departure time).
     * @param actualTime    the total execution time of this Gridlet in a
     *                         GridResource.
     *
     * @pre wallTime >= 0.0
     * @pre actualTime >= 0.0
     * @post $none
     */
    public void setExecParam(double wallTime, double actualTime)
    {
        if (wallTime < 0.0 || actualTime < 0.0 || index_ < 0) {
            return;
        }

        Resource res = resList_.get(index_);
        res.wallClockTime = wallTime;
        res.actualCPUTime = actualTime;

        if (record_)
        {
            write("Sets the wall clock time to "+ num_.format(wallTime)+
                  " and the actual CPU time to " + num_.format(actualTime));
        }
    }

    /**
     * Sets the status code of this Gridlet
     * @param newStatus    the status code of this Gridlet
     * @throws Exception   Invalid range of Gridlet status
     * @pre newStatus >= 0 && newStatus <= 8
     * @post $none
     */
    public void setGridletStatus(int newStatus) throws Exception
    {
        // if the new status is same as current one, then ignore the rest
        if (status_ == newStatus) {
            return;
        }

        // throws an exception if the new status is outside the range
        if (newStatus < Gridlet.CREATED || newStatus > Gridlet.FAILED_RESOURCE_UNAVAILABLE)
        {
            throw new Exception("Gridlet.setGridletStatus() : Error - " +
                    "Invalid integer range for Gridlet status.");
        }

        if (newStatus == Gridlet.SUCCESS) {
            finishTime_ = GridSim.clock();
        }

        if (record_)
        {
            String obj = "Sets Gridlet status from " + getGridletStatusString();
            write( obj + " to " + Gridlet.getStatusString(newStatus) );
        }

        this.status_ = newStatus;
    }

    /**
     * Gets the status code of this Gridlet
     * @return the status code of this Gridlet
     * @pre $none
     * @post $result >= 0
     */
    public int getGridletStatus() {
        return status_;
    }

    /**
     * Gets the string representation of the current Gridlet status code
     * @return the Gridlet status code as a string or <tt>null</tt> if the
     *         status code is unknown
     * @pre $none
     * @post $none
     */
    public String getGridletStatusString() {
        return Gridlet.getStatusString(status_);
    }

    /**
     * Gets the string representation of the given Gridlet status code
     * @param status    the Gridlet status code
     * @return the Gridlet status code as a string or <tt>null</tt> if the
     *         status code is unknown
     * @pre $none
     * @post $none
     */
    public static String getStatusString(int status)
    {
        String statusString = null;
        switch (status)
        {
            case Gridlet.CREATED:
                statusString = "Created";
                break;

            case Gridlet.READY:
                statusString = "Ready";
                break;

            case Gridlet.INEXEC:
                statusString = "InExec";
                break;

            case Gridlet.SUCCESS:
                statusString = "Success";
                break;

            case Gridlet.QUEUED:
                statusString = "Queued";
                break;

            case Gridlet.FAILED:
                statusString = "Failed";
                break;

            case Gridlet.CANCELED:
                statusString = "Canceled";
                break;

            case Gridlet.PAUSED:
                statusString = "Paused";
                break;

            case Gridlet.RESUMED:
                statusString = "Resumed";
                break;

            case Gridlet.FAILED_RESOURCE_UNAVAILABLE :
                statusString = "Failed_resource_unavailable";
                break;

            default:
                break;
        }

        return statusString;
    }

    /**
     * Gets the length of this Gridlet
     * @return the length of this Gridlet
     * @pre $none
     * @post $result >= 0.0
     */
    public double getGridletLength() {
        return gridletLength_;
    }

    /**
     * Gets the total execution time of this Gridlet from the latest
     * GridResource
     * @return the total execution time of this Gridlet in a GridResource
     * @pre $none
     * @post $result >= 0.0
     */
    public double getActualCPUTime()
    {
        if (index_ == -1) {
            return 0.0;
        }

        return resList_.get(index_).actualCPUTime;
    }

    /**
     * Gets the cost running this Gridlet in the latest GridResource
     * @return the cost associated with running this Gridlet
     *         or <tt>0.0</tt> if none
     * @pre $none
     * @post $result >= 0.0
     */
    public double getCostPerSec()
    {
        if (index_ == -1) {
            return 0.0;
        }

        return resList_.get(index_).costPerSec;
    }

    /**
     * Gets the total cost of processing or executing this Gridlet
     * <tt>Processing Cost = actual CPU Time * cost per sec</tt>
     * @return the total cost of processing Gridlet
     * @see gridsim.Gridlet#getActualCPUTime()
     * @see gridsim.Gridlet#getCostPerSec()
     * @pre $none
     * @post $result >= 0.0
     */
    public double getProcessingCost()
    {
        if (index_ == -1) {
            return 0.0;
        }

        Resource res = null;
        double cost = 0.0;
        for (int i = 0; i <= index_; i++)
        {
            res = resList_.get(i);
            cost += (res.actualCPUTime * res.costPerSec);
        }

        return cost;
    }

    /**
     * Gets the time of this Gridlet resides in the latest GridResource
     * (from arrival time until departure time).
     * @return the time of this Gridlet resides in a GridResource
     * @pre $none
     * @post $result >= 0.0
     */
    public double getWallClockTime()
    {
        if (index_ == -1) {
            return 0.0;
        }

        return resList_.get(index_).wallClockTime;
    }

    /**
     * Gets all the GridResource names that executed this Gridlet
     * @return an array of GridResource names or <tt>null</tt> if it has none
     * @pre $none
     * @post $none
     */
    public String[] getAllResourceName()
    {
        int size = resList_.size();
        String[] data = null;

        if (size > 0)
        {
            data = new String[size];
            for (int i = 0; i < size; i++) {
                data[i] = resList_.get(i).resourceName;
            }
        }

        return data;
    }

    /**
     * Gets all the GridResource IDs that executed this Gridlet
     * @return an array of GridResource IDs or <tt>null</tt> if it has none
     * @pre $none
     * @post $none
     */
    public int[] getAllResourceID()
    {
        int size = resList_.size();
        int[] data = null;

        if (size > 0)
        {
            data = new int[size];
            for (int i = 0; i < size; i++) {
                data[i] = resList_.get(i).resourceId;
            }
        }

        return data;
    }

    /**
     * Gets the total execution time of this Gridlet in a given GridResource ID
     * @param resId  a GridResource entity ID
     * @return the total execution time of this Gridlet in a GridResource
     *         or <tt>0.0</tt> if not found
     * @pre resId >= 0
     * @post $result >= 0.0
     */
    public double getActualCPUTime(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.actualCPUTime;
            }
        }

        return 0.0;
    }

    /**
     * Gets the cost running this Gridlet in a given GridResource ID
     * @param resId  a GridResource entity ID
     * @return the cost associated with running this Gridlet
     *         or <tt>0.0</tt> if not found
     * @pre resId >= 0
     * @post $result >= 0.0
     */
    public double getCostPerSec(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.costPerSec;
            }
        }

        return 0.0;
    }

    /**
     * Gets the length of this Gridlet that has been executed so far in a given
     * GridResource ID. This method is useful when trying to move this Gridlet
     * into different GridResources or to cancel it.
     * @param resId  a GridResource entity ID
     * @return the length of a partially executed Gridlet or the full Gridlet
     *         length if it is completed or <tt>0.0</tt> if not found
     * @pre resId >= 0
     * @post $result >= 0.0
     */
    public double getGridletFinishedSoFar(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.finishedSoFar;
            }
        }

        return 0.0;
    }

    /**
     * Gets the submission or arrival time of this Gridlet in the
     * given GridResource ID
     * @param resId  a GridResource entity ID
     * @return the submission time or <tt>0.0</tt> if not found
     * @pre resId >= 0
     * @post $result >= 0.0
     */
    public double getSubmissionTime(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.submissionTime;
            }
        }

        return 0.0;
    }

    /**
     * Gets the time of this Gridlet resides in a given GridResource ID
     * (from arrival time until departure time).
     * @param resId  a GridResource entity ID
     * @return the time of this Gridlet resides in the GridResource
     *         or <tt>0.0</tt> if not found
     * @pre resId >= 0
     * @post $result >= 0.0
     */
    public double getWallClockTime(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.wallClockTime;
            }
        }

        return 0.0;
    }

    /**
     * Gets the GridResource name based on its ID
     * @param resId  a GridResource entity ID
     * @return the GridResource name or <tt>null</tt> if not found
     * @pre resId >= 0
     * @post $none
     */
    public String getResourceName(int resId)
    {
        Resource res = null;
        int size = resList_.size();
        for (int i = 0; i < size; i++)
        {
            res = resList_.get(i);
            if (resId == res.resourceId) {
                return res.resourceName;
            }
        }

        return null;
    }

    /**
     * Gets the finish time of this Gridlet in a GridResource
     * @return the finish or completion time of this Gridlet or <tt>-1</tt> if
     *         not finished yet.
     * @pre $none
     * @post $result >= -1
     */
    public double getFinishTime() {
        return finishTime_;
    }

    ////////////////////////// PROTECTED METHODS //////////////////////////////

    /**
     * Writes this particular history transaction of this Gridlet into a log
     * @param str   a history transaction of this Gridlet
     * @pre str != null
     * @post $none
     */
    protected void write(String str)
    {
        if (!record_) {
            return;
        }

        if (num_ == null || history_ == null)
        {
            // Creates the history or transactions of this Gridlet
            newline_ = System.getProperty("line.separator");
            num_ = new DecimalFormat("#0.00#");   // with 3 decimal spaces
            history_ = new StringBuffer(1000);
            history_.append("Time below denotes the simulation time.");
            history_.append( System.getProperty("line.separator") );
            history_.append("Time (sec)       Description Gridlet #"+gridletID_);
            history_.append( System.getProperty("line.separator") );
            history_.append("------------------------------------------");
            history_.append( System.getProperty("line.separator") );
            history_.append( num_.format(GridSim.clock()) );
            history_.append("   Creates Gridlet ID #" + gridletID_);
            history_.append( System.getProperty("line.separator") );
        }

        history_.append( num_.format(GridSim.clock()) );
        history_.append( "   " + str + newline_);
    }

} 

