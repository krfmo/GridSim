/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Calendar;

/**
 * ARObject class represents a reservation object and its properties.
 * {@link gridsim.AdvanceReservation} class creates an object of this class, 
 * then pass it to a resource. When a resource or its scheduler agrees to 
 * accept this reservation, then it will set an unique reservation ID and expiry
 * time.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @invariant $none
 */
public class ARObject
{
    private long startTime_;   // reservation start time
    private int duration_;     // reservation duration time
    private int numPE_;        // number of PEs requested for this reservation
    private int resID_;        // resource id that accepts this reservation
    private int userID_;       // user id that owns this reservation object
    private int transactionID_;    // a unique user's transaction ID
    private double timeZone_;  // user's local time zone
    private int status_;       // status of this reservation
    private static final int NOT_FOUND = -1;   // constant
    private boolean committed_;    // reservation has committed or not

    // these attributes are filled by a resource or its scheduler, and
    // by AdvanceReservation class
    private int reservID_;      // reservation ID
    private long expiryTime_;   // expiry time once a reservation has been made
    private int totalGridlet_;  // total Gridlets executed for this reservation


    /**
     * Copy constructor. If the given object is <tt>null</tt>, then no values
     * will be copied.
     * @param obj  ARObject object
     * @pre obj != null
     * @post $none
     */
    public ARObject(ARObject obj)
    {
        if (obj != null)
        {
            startTime_ = obj.getStartTime();
            duration_ = obj.getDurationTime();
            numPE_ = obj.getNumPE();
            resID_ = obj.getResourceID();
            transactionID_ = obj.getTransactionID();
            timeZone_ = obj.getTimeZone();
            status_ = obj.getStatus();
            reservID_ = obj.getReservationID();
            committed_ = obj.hasCommitted();
            expiryTime_ = obj.getExpiryTime();
            userID_ = obj.getUserID();
            totalGridlet_ = obj.getTotalGridlet();
        }
    }

    /**
     * Determines the size of ARObject object
     * @return the size of this object
     * @pre $none
     * @post $result > 0
     */
    public static int getByteSize()
    {
        int totalInt = 4 * 8;       // contains only 8 ints
        int totalDouble = 8 * 1;    // contains only 1 double
        int totalLong = 8 * 2;      // contains only 2 longs
        int overhead = 8;   // including boolean + an object overhead

        return totalInt + totalDouble + totalLong + overhead;
    }

    /**
     * Allocates a new ARObject object, with a default time zone from
     * <tt>GridSim.getSimulationCalendar()</tt> method. If the Calendar object
     * is <tt>null</tt>, then the time zone will be GMT+0.
     *
     * @param userName   an entity name that owns this reservation object
     * @pre userName != null
     * @post $none
     * @see gridsim.GridSim#getSimulationCalendar()
     */
    public ARObject(String userName)
    {
        Calendar cal = GridSim.getSimulationCalendar();
        if (cal == null) {
            timeZone_ = 0.0;
        }
        else {  // Must convert into hour not milli seconds
            timeZone_ =cal.getTimeZone().getRawOffset()/AdvanceReservation.HOUR;
        }

        init( GridSim.getEntityId(userName) );
    }

    /**
     * Allocates a new ARObject object. If the time zone is invalid, then
     * by default, it will be GMT+0.
     * @param userName   an entity name that owns this reservation object
     * @param timeZone   local time zone of a user that owns this reservation.
     *                   Time zone should be of range [GMT-12 ... GMT+13]
     * @pre userName != null
     * @pre $none
     * @post $none
     */
    public ARObject(String userName, double timeZone)
    {
        if (!AdvanceReservation.validateTimeZone(timeZone)) {
            timeZone_ = 0.0;
        }
        else {
            timeZone_ = timeZone;
        }
        init( GridSim.getEntityId(userName) );
    }


    /**
     * Allocates a new ARObject object. If the time zone is invalid, then
     * by default, it will be GMT+0.
     * @param userID     an entity ID that owns this reservation object
     * @param timeZone   local time zone of a user that owns this reservation.
     *                   Time zone should be of range [GMT-12 ... GMT+13]
     * @pre userID > 0
     * @post $none
     */
    public ARObject(int userID, double timeZone)
    {
        if (!AdvanceReservation.validateTimeZone(timeZone)) {
            timeZone_ = 0.0;
        }
        else {
            timeZone_ = timeZone;
        }
        init(userID);
    }

    /**
     * Allocates a new ARObject object, with a default time zone from
     * <tt>GridSim.getSimulationCalendar()</tt> method. If the Calendar object
     * is <tt>null</tt>, then the time zone will be GMT+0.
     * @param userID   an entity ID that owns this reservation object
     * @pre userID > 0
     * @post $none
     * @see gridsim.GridSim#getSimulationCalendar()
     */
    public ARObject(int userID)
    {
        Calendar cal = GridSim.getSimulationCalendar();
        if (cal == null) {
            timeZone_ = 0.0;
        }
        else {  // Must convert into hour not milli seconds
            timeZone_ =cal.getTimeZone().getRawOffset()/AdvanceReservation.HOUR;
        }
        init(userID);
    }

    /**
     * Copy the object. All the values are copied into this object.
     * If the object is <tt>null</tt>, then copy failed.
     * @param obj  an ARObject object
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre obj != null
     * @post $none
     */
    public boolean copy(ARObject obj)
    {
        boolean success = false;
        if (obj != null)
        {
            success = true;
            startTime_ = obj.getStartTime();
            duration_ = obj.getDurationTime();
            numPE_ = obj.getNumPE();
            resID_ = obj.getResourceID();
            transactionID_ = obj.getTransactionID();
            timeZone_ = obj.getTimeZone();
            status_ = obj.getStatus();
            reservID_ = obj.getReservationID();
            committed_ = obj.hasCommitted();
            expiryTime_ = obj.getExpiryTime();
            userID_ = obj.getUserID();
            totalGridlet_ = obj.getTotalGridlet();
        }
        return success;
    }

    /**
     * Sets a transaction ID for this reservation.
     * @param id   a transaction ID
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @pre id > 0
     * @post $none
     */
    public boolean setTransactionID(int id)
    {
        if (id <= 0) {
            return false;
        }

        transactionID_ = id;
        return true;
    }

    /**
     * Sets the start time (in milliseconds) for this reservation.
     * The start time should be greater than simulation init time defined
     * in {@link gridsim.GridSim#init(int, Calendar, boolean)}
     * @param startTime   the reservation start time in milliseconds
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre startTime > 0
     * @post $none
     */
    public boolean setStartTime(long startTime)
    {
        if (startTime <= 0) {
            return false;
        }

        startTime_ = startTime;
        return true;
    }

    /**
     * Sets the duration time (unit in seconds) for this reservation.
     * @param duration   the reservation duration time. Time unit is in seconds.
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @pre duration > 0
     * @post $none
     */
    public boolean setDurationTime(int duration)
    {
        if (duration <= 0) {
            return false;
        }

        duration_ = duration;
        return true;
    }

    /**
     * Sets the number of PEs (Processing Elements) required by this reservation
     * @param numPE   number of PEs required
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @pre numPE > 0
     * @post $none
     */
    public boolean setNumPE(int numPE)
    {
        if (numPE <= 0) {
            return false;
        }

        numPE_ = numPE;
        return true;
    }

    /**
     * Sets the resource ID for sending this reservation object.
     * @param id   a resource ID
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @pre id > 0
     * @post $none
     */
    public boolean setResourceID(int id)
    {
        if (id <= 0) {
            return false;
        }

        resID_ = id;
        return true;
    }

    /**
     * Sets this reservation's time zone based on GMT.
     * @param time  a valid time zone based on GMT
     * @return <tt>true</tt> if successul, <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean setTimeZone(double time)
    {
        if (!AdvanceReservation.validateTimeZone(time)) {
            return false;
        }

        timeZone_ = time;
        return true;
    }

    /**
     * Gets this object's owner ID
     * @return a user ID that owns this reservation object
     * @pre $none
     * @post $none
     */
    public int getUserID() {
        return userID_;
    }

    /**
     * Gets this object's owner name
     * @return a user name that owns this reservation object or <tt>null</tt>
     *     if invalid
     * @pre $none
     * @post $none
     */
    public String getEntityName() {
        return GridSim.getEntityName(userID_);
    }

    /**
     * Gets this object's transaction ID
     * @return a transaction ID
     * @pre $none
     * @post $none
     */
    public int getTransactionID() {
        return transactionID_;
    }

    /**
     * Gets this object's start time in milliseconds
     * @return the reservation start time in milliseconds
     * @pre $none
     * @post $none
     */
    public long getStartTime() {
        return startTime_;
    }

    /**
     * Gets this object's duration time in seconds
     * @return the reservation duration time in seconds
     * @pre $none
     * @post $none
     */
    public int getDurationTime() {
        return duration_;
    }

    /**
     * Gets this object's number of PEs.
     * @return the reservation number of PEs requested
     * @pre $none
     * @post $none
     */
    public int getNumPE() {
        return numPE_;
    }

    /**
     * Gets this object's resource ID.
     * @return a resource ID
     * @pre $none
     * @post $none
     */
    public int getResourceID() {
        return resID_;
    }

    /**
     * Gets this object's time zone based on GMT
     * @return a time zone based on GMT
     * @pre $none
     * @post $none
     */
    public double getTimeZone() {
        return timeZone_;
    }

    /**
     * Gets the total Gridlet executed by this reservation
     * @return total Gridlet
     * @pre $none
     * @post $none
     */
    public int getTotalGridlet() {
        return totalGridlet_;
    }

    /**
     * Adds the number of Gridlets executed by this reservation.
     * This method is done by a resource's scheduler.
     * @param num   number of Gridlets
     * @pre num > 0
     * @post $none
     */
    public void addTotalGridlet(int num) {
        totalGridlet_ += num;
    }

    /**
     * Reduces total Gridlet executed by this reservation by one.
     * This method is done by a resource's scheduler.
     * @pre $none
     * @post $none
     */
    public void reduceTotalGridlet()
    {
        if (totalGridlet_ == 0) {
            return;
        }

        totalGridlet_ -= 1;
    }

    /**
     * Sets a reservation's attributes upon acceptance by a resource.
     * This method is mainly used by <tt>ARPolicy</tt> object.
     * @param id    an unique reservation id
     * @param expiryTime   the reservation expiry time
     * @pre id > 0
     * @pre expiryTime > 0
     * @post $none
     */
    public void setReservation(int id, long expiryTime)
    {
        if (id <= 0 || expiryTime <= 0) {
            return;
        }
        reservID_ = id;
        expiryTime_ = expiryTime;
    }

    /**
     * Sets the status of this reservation. This method can be used by
     * both <tt>ARPolicy</tt> and <tt>AdvanceReservation</tt> object.
     * @param status   this reservation status
     * @pre $none
     * @post $none
     */
    public void setStatus(int status) {
        status_ = status;
    }

    /**
     * Sets this object into a committed state. This method can be used by
     * both <tt>ARPolicy</tt> and <tt>AdvanceReservation</tt> object.
     * @pre $none
     * @post $none
     */
    public void setCommitted() {
        committed_ = true;
    }

    /**
     * Gets the status of this reservation
     * @return this reservation current status
     * @pre $none
     * @post $none
     */
    public int getStatus() {
        return status_;
    }

    /**
     * Gets this object's reservation ID
     * @return a reservation ID
     * @pre $none
     * @post $none
     */
    public int getReservationID() {
        return reservID_;
    }

    /**
     * Gets this object's expiry time
     * @return expiry time
     * @pre $none
     * @post $none
     */
    public long getExpiryTime() {
        return expiryTime_;
    }

    /**
     * Checks whether this reservation object has committed or not.
     * @return <tt>true</tt> if this object has committed, <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    public boolean hasCommitted() {
        return committed_;
    }

    //////////////////////// PRIVATE METHODS  //////////////////////////

    /**
     * Initialises all private attributes
     * @param userID   a user ID that owns this reservation object
     * @pre $none
     * @post $none
     */
    private void init(int userID)
    {
        if (userID == -1) {
            System.out.println("ARObject: Invalid userID or userName.");
        }

        userID_ = userID;
        startTime_ = NOT_FOUND;
        duration_ = NOT_FOUND;
        numPE_ = NOT_FOUND;
        resID_ = NOT_FOUND;
        transactionID_ = NOT_FOUND;

        status_ = NOT_FOUND;
        reservID_ = NOT_FOUND;
        expiryTime_ = NOT_FOUND;
        totalGridlet_ = 0;
        committed_ = false;
    }

} 

