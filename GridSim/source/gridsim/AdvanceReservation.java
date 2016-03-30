/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;
import gridsim.filter.FilterCreateAR;
import gridsim.filter.FilterQueryTimeAR;
import gridsim.filter.FilterResult;
import gridsim.net.Link;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * This class handles all Advanced Reservation (AR) functionalities, such as
 * create, modify, cancel and query. There are two types of reservation:
 * <ul>
 *     <li> advanced reservation: requests a reservation in future time.
 *     <li> immediate reservation: requests a reservation immediately, i.e.
 *          the current time is used as the start time with/without specifying
 *          duration or end time. <br>
 *          In this class, immediate reservation can be done by one of the
 *          following ways:
 *          <ol>
 *          <li> <tt>start time = 0 (in long) or null (in Calendar object)</tt>
 *               <b>and</b> <tt>duration > 0</tt>.<br>
 *               If successful, expiry time set by a resource would be:
 *               <tt>expiry time = current time + duration.</tt>
 *               <br><br>
 *          <li> <tt>start time = 0 (in long) or null (in Calendar object)</tt>
 *               <b>and</b> <tt>duration = 0</tt>.<br>
 *               This means a reservation is running as long as there are empty
 *               PEs available. <br>
 *               <b>NOTE:</b> using this approach, a reservation
 *               is having a risk of being pre-empted or terminated by a
 *               resource scheduler when new AR requests come. In addition,
 *               due to complexity, a resource's scheduler might not support
 *               this method. Finally, <tt>expiry time = 0</tt> since a
 *               resource's scheduler can not determine it.<br><br>
 *               Instead of using this approach, you can directly
 *               submit a job or a Gridlet object using
 *               {@link gridsim.GridSim#gridletSubmit(Gridlet, int)} method.
 *          </ol>
 * </ul>
 * <p>
 * The AR methods in this class automatically handle the
 * communication to/from a destinated grid resource entity.
 * To use AR functionalities, you need to create a subclass of this object, i.e.
 * creating a new class that inherits this object. This approach is preferable
 * because the subclass is responsible for collecting Gridlets back from
 * a resource entity.
 * <p>
 * Important properties regarding to this entity are time and
 * time zone. Time in this entity, GridResource entities and simulation clock
 * are all
 * relative to a given init time. In other words, future time should be greater
 * than init time given during
 * {@link gridsim.GridSim#init(int, Calendar, boolean)} method.<br>
 * <p>
 * Imagine the following scenarios:
 * <ul>
 *      <li> Simulation init time:
 *            <tt>Calendar cal = Calendar.getInstance();</tt> <br>
 *           Assume, time and date is <tt>16:00:00 02 May 2004.</tt>
 *      <li> Simulation clock: <tt>double clock = GridSim.clock();</tt><br>
 *           Assume the simulation has been run for 45 seconds (according to
 *           simulation or SimJava clock not the actual running of GridSim).
 *           So the current simulation time and date is
 *           <tt>16:00:45 02 May 2004.</tt>
 *      <li> A user entity reserves a slot at time
 *           <tt>15:00:00 02 May 2004.</tt><br>
 *           Result: rejected instantly since the time has expired.
 *      <li> A user entity reserves a slot at time
 *          <tt>16:10:00 02 May 2004.</tt><br>
 *           Result: accepted or rejected depending on a resource availability.
 *           If it is accepted, assume that expiry period for a reservation is
 *           5 minutes starting from the current time, then a user must
 *           commit before time and date <tt>16:05:45 02 May 2004.</tt>
 * </ul>
 * <p>
 * Time can be represented as <tt>Calendar</tt> object or <tt>long</tt>
 * in milli seconds.
 * <p>
 * User and GridResource entities might have different time zone. Therefore,
 * when communicating between these two entities, upon arrival, the time will
 * be converted by an individual entity into its local time.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @invariant $none
 * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[], String)
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 */
public class AdvanceReservation extends GridSim
{
    private static final int MAX_ID = 4;   // number of variables
    private static final int SIZE_ARRAY = MAX_ID*5;   // size of int[MAX_ID] array
    private long initTime_;        // time in millisec the simulation starts
    private double timeZone_;      // this class or user's time zone
    private int transactionID_;    // transaction ID for uniqueness
    private ArrayList booking_;    // to store a successful reservation
    private static final int MILLI_SEC = 1000;       // 1 sec = 1,000 milli seconds

    /** 1 Hour representation in milliseconds, i.e. 1 hour = 1000*60*60 */
    public static final int HOUR = 1000 * 60 * 60;

    ////////////////// STATIC METHODS /////////////////////////////////////

    /**
     * Converts local time from one time zone to another
     * @param time    local current time in milliseconds
     * @param fromZone   local time zone of range [GMT-12 ... GMT+13]
     * @param toZone     destination time zone of range [GMT-12 ... GMT+13]
     * @return a converted time in milliseconds or <tt>-1</tt> if a time zone
     *         is not within [GMT-12 ... GMT+13]
     * @pre $none
     * @post $none
     */
    public static long convertTimeZone(long time,double fromZone,double toZone)
    {
        int MIN_GMT = -12;
        int MAX_GMT = 13;

        if (fromZone < MIN_GMT || fromZone > MAX_GMT) {
            return -1;
        }

        if (toZone < MIN_GMT || toZone > MAX_GMT) {
            return -1;
        }

        double diff = toZone - fromZone;
        return time + (int) (diff * HOUR);
    }

    /**
     * Checks whether a given time zone is valid or not
     * @param timeZone  a time zone
     * @return <tt>true</tt> if a time zone is valid, or <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    public static boolean validateTimeZone(double timeZone)
    {
        int MIN_GMT = -12;
        int MAX_GMT = 13;

        if (timeZone < MIN_GMT || timeZone > MAX_GMT) {
            return false;
        }

        double decimal = timeZone - (int) timeZone;
        if (decimal >= 0.60) {
            return false;
        }

        return true;
    }

    /**
     * Converts a reservation result from integer into a String.
     * The result is one of GridSimTags.AR_CREATE_XXXX tags,
     * where XXXX = specific tag name.
     * @param result   a result for a new reservation request
     * @return a String representation of the reservation result
     *  or <tt>null</tt> if invalid result.
     * @pre $none
     * @post $none
     * @see gridsim.GridSimTags
     */
    public static String getCreateResult(int result)
    {
        String str = null;
        switch (result)
        {
            case GridSimTags.AR_CREATE_ERROR:
                str = "AR_CREATE_ERROR";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_START_TIME:
                str = "AR_CREATE_ERROR_INVALID_START_TIME";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_END_TIME:
                str = "AR_CREATE_ERROR_INVALID_END_TIME";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_DURATION_TIME:
                str = "AR_CREATE_ERROR_INVALID_DURATION_TIME";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_NUM_PE:
                str = "AR_CREATE_ERROR_INVALID_NUM_PE";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_ID:
                str = "AR_CREATE_ERROR_INVALID_RESOURCE_ID";
                break;

            case GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_NAME:
                str = "AR_CREATE_ERROR_INVALID_RESOURCE_NAME";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_CANT_SUPPORT:
                str = "AR_CREATE_FAIL_RESOURCE_CANT_SUPPORT";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_NOT_ENOUGH_PE:
                str = "AR_CREATE_FAIL_RESOURCE_NOT_ENOUGH_PE";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_1_SEC:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_1_SEC";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_5_SECS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_5_SECS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_10_SECS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_10_SECS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_15_SECS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_15_SECS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_30_SECS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_30_SECS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_45_SECS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_45_SECS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_1_MIN:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_1_MIN";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_5_MINS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_5_MINS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_10_MINS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_10_MINS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_15_MINS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_15_MINS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_30_MINS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_30_MINS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_45_MINS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_45_MINS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_1_HOUR:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_1_HOUR";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_5_HOURS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_5_HOURS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_10_HOURS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_10_HOURS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_15_HOURS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_15_HOURS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_30_HOURS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_30_HOURS";
                break;

            case GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_45_HOURS:
                str = "AR_CREATE_FAIL_RESOURCE_FULL_IN_45_HOURS";
                break;

            default:
                break;
        }

        return str;
    }

    /**
     * Converts a reservation result from integer into a String.
     * The result is one of GridSimTags.AR_CANCEL_XXXX tags,
     * where XXXX = specific tag name.
     * @param result   a result for cancel a reservation
     * @return a String representation of the reservation result
     *  or <tt>null</tt> if invalid result.
     * @pre $none
     * @post $none
     * @see gridsim.GridSimTags
     */
    public static String getCancelResult(int result)
    {
        String str = null;
        switch (result)
        {
            case GridSimTags.AR_CANCEL_ERROR:
                str = "AR_CANCEL_ERROR";
                break;

            case GridSimTags.AR_CANCEL_FAIL:
                str = "AR_CANCEL_FAIL";
                break;

            case GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID:
                str = "AR_CANCEL_FAIL_INVALID_BOOKING_ID";
                break;

            case GridSimTags.AR_CANCEL_FAIL_GRIDLET_FINISHED:
                str = "AR_CANCEL_FAIL_GRIDLET_FINISHED";
                break;

            case GridSimTags.AR_CANCEL_SUCCESS:
                str = "AR_CANCEL_SUCCESS";
                break;

            case GridSimTags.AR_CANCEL_ERROR_RESOURCE_CANT_SUPPORT:
                str = "AR_CANCEL_ERROR_RESOURCE_CANT_SUPPORT";
                break;

            default:
                break;
        }

        return str;
    }

    /**
     * Converts a reservation result from integer into a String.
     * The result is one of GridSimTags.AR_STATUS_XXXX tags,
     * where XXXX = specific tag name.
     * @param result   a result for query a reservation
     * @return a String representation of the reservation result
     *  or <tt>null</tt> if invalid result.
     * @pre $none
     * @post $none
     * @see gridsim.GridSimTags
     */
    public static String getQueryResult(int result)
    {
        String str = null;
        switch (result)
        {
            case GridSimTags.AR_STATUS_NOT_STARTED:
                str = "AR_STATUS_NOT_STARTED";
                break;

            case GridSimTags.AR_STATUS_ACTIVE:
                str = "AR_STATUS_ACTIVE";
                break;

            case GridSimTags.AR_STATUS_COMPLETED:
                str = "AR_STATUS_COMPLETED";
                break;

            case GridSimTags.AR_STATUS_CANCELED:
                str = "AR_STATUS_CANCELED";
                break;

            case GridSimTags.AR_STATUS_ERROR_INVALID_BOOKING_ID:
                str = "AR_STATUS_ERROR_INVALID_BOOKING_ID";
                break;

            case GridSimTags.AR_STATUS_EXPIRED:
                str = "AR_STATUS_EXPIRED";
                break;

            case GridSimTags.AR_STATUS_NOT_COMMITTED:
                str = "AR_STATUS_NOT_COMMITTED";
                break;

            case GridSimTags.AR_STATUS_RESERVATION_DOESNT_EXIST:
                str = "AR_STATUS_RESERVATION_DOESNT_EXIST";
                break;

            case GridSimTags.AR_STATUS_TERMINATED:
                str = "AR_STATUS_TERMINATED";
                break;

            case GridSimTags.AR_STATUS_ERROR:
                str = "AR_STATUS_ERROR";
                break;

            default:
                break;
        }

        return str;
    }

    /**
     * Converts a reservation result from integer into a String.
     * The result is one of GridSimTags.AR_COMMIT_XXXX tags,
     * where XXXX = specific tag name.
     * @param result   a result for commit a reservation
     * @return a String representation of the reservation result
     *  or <tt>null</tt> if invalid result.
     * @pre $none
     * @post $none
     * @see gridsim.GridSimTags
     */
    public static String getCommitResult(int result)
    {
        String str = null;
        switch (result)
        {
            case GridSimTags.AR_COMMIT_SUCCESS:
                str = "AR_COMMIT_SUCCESS";
                break;

            case GridSimTags.AR_COMMIT_FAIL:
                str = "AR_COMMIT_FAIL";
                break;

            case GridSimTags.AR_COMMIT_FAIL_EXPIRED:
                str = "AR_COMMIT_FAIL_EXPIRED";
                break;

            case GridSimTags.AR_COMMIT_FAIL_INVALID_BOOKING_ID:
                str = "AR_COMMIT_FAIL_INVALID_BOOKING_ID";
                break;

            case GridSimTags.AR_COMMIT_ERROR_RESOURCE_CANT_SUPPORT:
                str = "AR_COMMIT_ERROR_RESOURCE_CANT_SUPPORT";
                break;

            case GridSimTags.AR_COMMIT_ERROR:
                str = "AR_COMMIT_ERROR";
                break;

            default:
                break;
        }

        return str;
    }

    /**
     * Converts a reservation result from integer into a String.
     * The result is one of GridSimTags.AR_MODIFY_XXXX tags,
     * where XXXX = specific tag name.
     * @param result   a result for modify a reservation
     * @return a String representation of the reservation result
     *  or <tt>null</tt> if invalid result.
     * @pre $none
     * @post $none
     * @see gridsim.GridSimTags
     */
    public static String getModifyResult(int result)
    {
        String str = null;
        switch (result)
        {
            case GridSimTags.AR_MODIFY_ERROR:
                str = "AR_MODIFY_ERROR";
                break;

            case GridSimTags.AR_MODIFY_SUCCESS:
                str = "AR_MODIFY_SUCCESS";
                break;

            case GridSimTags.AR_MODIFY_FAIL_INVALID_END_TIME:
                str = "AR_MODIFY_FAIL_INVALID_END_TIME";
                break;

            case GridSimTags.AR_MODIFY_FAIL_INVALID_START_TIME:
                str = "AR_MODIFY_FAIL_INVALID_START_TIME";
                break;

            case GridSimTags.AR_MODIFY_FAIL_INVALID_NUM_PE:
                str = "AR_MODIFY_FAIL_INVALID_NUM_PE";
                break;

            case GridSimTags.AR_MODIFY_FAIL_INVALID_BOOKING_ID:
                str = "AR_MODIFY_FAIL_INVALID_BOOKING_ID";
                break;

            case GridSimTags.AR_MODIFY_FAIL_RESERVATION_ACTIVE:
                str = "AR_MODIFY_FAIL_RESERVATION_ACTIVE";
                break;

            case GridSimTags.AR_MODIFY_FAIL_RESOURCE_CANT_SUPPORT:
                str = "AR_MODIFY_FAIL_RESOURCE_CANT_SUPPORT";
                break;

            default:
                break;

        }

        return str;
    }

    ////////////////// PUBLIC METHODS /////////////////////////////////

    /**
     * Allocates a new AdvanceReservation object. Time zone of this object
     * will be a default <tt>Calendar</tt> object given during
     * <tt>GridSim.init()</tt> method. If <tt>Calendar</tt> object is
     * <tt>null</tt>, then time zone is GMT+0
     *
     * @param name   this entity name
     * @param baudRate   the bandwidth of this entity
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public AdvanceReservation(String name, double baudRate) throws Exception
    {
        super(name, baudRate);

        // default current time and its time zone
        Calendar cal = GridSim.getSimulationCalendar();
        if (cal == null) {
            timeZone_ = 0.0;
        }
        else {
            timeZone_ = cal.getTimeZone().getRawOffset() / HOUR;
        }
        init();
    }

    /**
     * Allocates a new AdvanceReservation object with a given time zone
     * @param name   this entity name
     * @param baudRate   the bandwidth of this entity
     * @param timeZone   the time zone of this entity
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> invalid time zone
     *      </ul>
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public AdvanceReservation(String name, double baudRate, double timeZone)
                              throws Exception
    {
        super(name, baudRate);

        if (AdvanceReservation.validateTimeZone(timeZone)) {
            timeZone_ = timeZone;
        }
        else {
            throw new Exception(name + ": Error - invalid time zone");
        }

        init();
    }

    /**
     * Allocates a new AdvanceReservation object. Time zone of this object
     * will be a default <tt>Calendar</tt> object given during
     * <tt>GridSim.init()</tt> method. If <tt>Calendar</tt> object is
     * <tt>null</tt>, then time zone is GMT+0
     *
     * @param name   this entity name
     * @param link   the link that this GridSim entity will use to communicate
     *               with other GridSim or Network entities.
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public AdvanceReservation(String name, Link link) throws Exception
    {
        super(name, link);

        // default current time and its time zone
        Calendar cal = GridSim.getSimulationCalendar();
        if (cal == null) {
            timeZone_ = 0.0;
        }
        else {
            timeZone_ = cal.getTimeZone().getRawOffset() / HOUR;
        }
        init();
    }

    /**
     * Allocates a new AdvanceReservation object with a given time zone
     * @param name   this entity name
     * @param link   the link that this GridSim entity will use to communicate
     *               with other GridSim or Network entities.
     * @param timeZone   the time zone of this entity
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> invalid time zone
     *      </ul>
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public AdvanceReservation(String name, Link link, double timeZone)
                              throws Exception
    {
        super(name, link);

        if (AdvanceReservation.validateTimeZone(timeZone)) {
            timeZone_ = timeZone;
        }
        else {
            throw new Exception(name + ": Error - invalid time zone");
        }

        init();
    }

    /**
     * Gets an expiry time of a reservation
     * @param bookingID   a reservation booking ID
     * @return an expiry time or -1 if not found
     * @pre bookingID != null
     * @post $result > 0
     */
    public long getExpiryTime(String bookingID)
    {
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return -1;
        }

        ARObject obj = searchBooking(id[0], id[1]);
        if (obj == null) {
            return -1;
        }

        return obj.getExpiryTime();
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = null, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> endTime = null, meaning do not have a specific requirement about
     *          completion time.
     * </ul>
     *
     * @param startTime   reservation start time
     * @param endTime     reservation end time
     * @param numPE       number of PEs required for this reservation
     * @param resID       a resource ID
     * @return an unique booking id if successful, otherwise an error message
     * @pre numPE > 0
     * @pre resID > 0
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(Calendar startTime, Calendar endTime,
                                    int numPE, int resID)
    {
        long start = 0;   // start time
        long end = 0;     // end time

        // if start time is not empty
        if (startTime != null) {
            start = startTime.getTimeInMillis();
        }

        // if end time is not empty
        if (endTime != null) {
            end = endTime.getTimeInMillis();
        }

        return createReservation(start, end, numPE, resID);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = null, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> endTime = null, meaning do not have a specific requirement about
     *          completion time.
     * </ul>
     *
     * @param startTime   reservation start time
     * @param endTime     reservation end time
     * @param numPE       number of PEs required for this reservation
     * @param resName     a resource name
     * @return an unique booking id if successful, otherwise an error message
     * @pre numPE > 0
     * @pre resName != null
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(Calendar startTime, Calendar endTime,
                                    int numPE, String resName)
    {
        long start = 0;   // start time
        long end = 0;     // end time

        // if start time is not empty
        if (startTime != null) {
            start = startTime.getTimeInMillis();
        }

        // if end time is not empty
        if (endTime != null) {
            end = endTime.getTimeInMillis();
        }

        return createReservation(start, end, numPE, resName);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = null, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> duration = 0, meaning do not have a specific requirement about
     *          duration or completion time.
     * </ul>
     *
     * @param startTime   reservation start time
     * @param duration    reservation duration time in seconds
     * @param numPE       number of PEs required for this reservation
     * @param resID       a resource ID
     * @return an unique booking id if successful, otherwise an error message
     * @pre duration >= 0
     * @pre numPE > 0
     * @pre resID > 0
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(Calendar startTime, int duration,
                                    int numPE, int resID)
    {
        long start = 0;  // start time

        // check the start time
        if (startTime != null) {
            start = startTime.getTimeInMillis();
        }

        return createReservation(start, duration, numPE, resID);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = null, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> duration = 0, meaning do not have a specific requirement about
     *          duration or completion time.
     * </ul>
     *
     * @param startTime   reservation start time
     * @param duration    reservation duration time in seconds
     * @param numPE       number of PEs required for this reservation
     * @param resName     a resource name
     * @return an unique booking id if successful, otherwise an error message
     * @pre duration >= 0
     * @pre numPE > 0
     * @pre resName != null
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(Calendar startTime, int duration,
                                    int numPE, String resName)
    {
        long start = 0;   // start time

        // check the start time
        if (startTime != null) {
            start = startTime.getTimeInMillis();
        }

        return createReservation(start, duration, numPE, resName);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = 0, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> endTime = 0, meaning do not have a specific requirement about
     *          completion time.
     * </ul>
     *
     * @param startTime   reservation start time in milliseconds
     * @param endTime     reservation end time in milliseconds
     * @param numPE       number of PEs required for this reservation
     * @param resID       a resource ID
     * @return an unique booking id if successful, otherwise an error message
     * @pre startTime >= 0
     * @pre endTime >= 0
     * @pre numPE > 0
     * @pre resID > 0
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(long startTime, long endTime,
                                    int numPE, int resID)
    {
        // sends an immediate reservation
        int tag = 0;
        boolean ar = true;
        if (startTime == 0 || endTime == 0)
        {
            ar = false;
            tag = GridSimTags.SEND_AR_CREATE_IMMEDIATE;
        }
        else {   // sends AR
            tag = GridSimTags.SEND_AR_CREATE;
        }

        // check all the values first
        String errorMsg = validateValue(startTime, endTime, numPE, resID, ar);
        if (errorMsg != null) {
            return errorMsg;
        }

        // otherwise create a new reservation
        ARObject createObj = new ARObject(super.get_id(), timeZone_);
        createObj.setStartTime(startTime);

        // duration = end time - start time
        int duration = (int) (endTime - startTime) / MILLI_SEC;
        createObj.setDurationTime(duration);
        createObj.setNumPE(numPE);
        createObj.setResourceID(resID);

        return sendReservation(resID, createObj, tag);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = 0, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> endTime = 0, meaning do not have a specific requirement about
     *          completion time.
     * </ul>
     *
     * @param startTime   reservation start time in milliseconds
     * @param endTime     reservation end time in milliseconds
     * @param numPE       number of PEs required for this reservation
     * @param resName     a resource name
     * @return an unique booking id if successful, otherwise an error message
     * @pre startTime > 0
     * @pre endTime > 0
     * @pre numPE > 0
     * @pre resName != null
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(long startTime, long endTime,
                                    int numPE, String resName)
    {
        int result = 0;
        boolean error = false;

        // check the resource name
        int resID = 0;
        if (resName == null)
        {
            result = GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_NAME;
            error = true;
        }
        else
        {
            resID = GridSim.getEntityId(resName);
            if (resID == -1)
            {
                result = GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_NAME;
                error = true;
            }
        }

        if (error) {
            return AdvanceReservation.getCreateResult(result);
        }

        return createReservation(startTime, endTime, numPE, resID);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = 0, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> duration = 0, meaning do not have a specific requirement about
     *          duration or completion time.
     * </ul>
     *
     * @param startTime   reservation start time in milliseconds
     * @param duration    reservation end time in seconds
     * @param numPE       number of PEs required for this reservation
     * @param resID       a resource ID
     * @return an unique booking id if successful, otherwise an error message
     * @pre startTime > 0
     * @pre duration > 0
     * @pre numPE > 0
     * @pre resID > 0
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(long startTime, int duration,
                                    int numPE, int resID)
    {
        // sends an immediate reservation
        int tag = 0;
        boolean ar = true;   // a flag to denote whether it is AR or Immediate
        if (startTime == 0 || duration == 0)
        {
            ar = false;
            tag = GridSimTags.SEND_AR_CREATE_IMMEDIATE;
        }
        else {   // sends AR
            tag = GridSimTags.SEND_AR_CREATE;
        }

        // check all the values first
        String errorMsg = validateValue(startTime, duration, numPE, resID, ar);
        if (errorMsg != null) {
            return errorMsg;
        }

        // otherwise create a new reservation
        ARObject createObj = new ARObject(super.get_id(), timeZone_);
        createObj.setStartTime(startTime);
        createObj.setDurationTime(duration);
        createObj.setNumPE(numPE);
        createObj.setResourceID(resID);

        return sendReservation(resID, createObj, tag);
    }

    /**
     * Creates a new reservation and sends the request to a resource.
     * If the new reservation has been accepted by a resource, an unique booking
     * ID is generated in the format of <tt>"resourceID_reservationID"</tt>.
     * Otherwise, an error message or an approximate busy time is returned
     * by using GridSimTags.AR_CREATE_XXXX tags, where XXXX = specific tag name.
     * <p>
     * Immediate reservation can be used by this method by specifying one or
     * both properties:
     * <ul>
     *     <li> startTime = 0, meaning do not care about start time or use
     *          current time as a reservation's start time.
     *     <li> duration = 0, meaning do not have a specific requirement about
     *          duration or completion time.
     * </ul>
     *
     * @param startTime   reservation start time in milliseconds
     * @param duration    reservation end time in seconds
     * @param numPE       number of PEs required for this reservation
     * @param resName     a resource name
     * @return an unique booking id if successful, otherwise an error message
     * @pre startTime > 0
     * @pre duration > 0
     * @pre numPE > 0
     * @pre resName != null
     * @post $result != null
     * @see gridsim.GridSimTags
     */
    public String createReservation(long startTime, int duration,
                                    int numPE, String resName)
    {
        int result = 0;
        boolean error = false;

        // check the resource name
        int resID = 0;
        if (resName == null)
        {
            result = GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_NAME;
            error = true;
        }
        else
        {
            // then check the resource ID as well
            resID = GridSim.getEntityId(resName);
            if (resID == -1)
            {
                result = GridSimTags.AR_CREATE_ERROR_INVALID_RESOURCE_NAME;
                error = true;
            }
        }

        // if invalid resource name or id, then exit
        if (error) {
            return AdvanceReservation.getCreateResult(result);
        }

        return createReservation(startTime, duration, numPE, resID);
    }

    /**
     * Modifies an existing reservation. Modification must be done before
     * reservation's start time.
     * A return value of this method uses one of
     * GridSimTags.AR_MODIFY_XXXX tags, where XXXX = specific tag name.
     *
     * @param bookingID   reservation booking ID
     * @param obj         reservation object that contains new modifications
     * @return an integer tag that denotes success or failure.
     * @pre bookingID != null
     * @pre obj != null
     * @post $none
     * @see gridsim.GridSimTags
     */
    public int modifyReservation(String bookingID, ARObject obj)
    {
        // id[0] = resID, [1] = reservID, [2] = trans ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_MODIFY_FAIL_INVALID_BOOKING_ID;
        }

        // then search for the existing reservation
        ARObject old = searchBooking(id[0], id[1]);
        if (old == null) {
            return GridSimTags.AR_MODIFY_FAIL_INVALID_BOOKING_ID;
        }

        // get the transaction ID for this query
        int tagID = incrementID();
        obj.setTransactionID(tagID);

        // then send it to a grid resource
        super.send(super.output, 0.0, GridSimTags.SEND_AR_MODIFY,
                   new IO_data(obj, obj.getByteSize(), id[0]) );

        // wait for feedback whether the reservation has been accepted or not
        // waiting for a response from the GridResource
        FilterResult tag = new FilterResult(tagID,GridSimTags.RETURN_AR_MODIFY);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        int result = GridSimTags.AR_MODIFY_ERROR;
        try
        {
            int[] array = (int[]) ev.get_data(); // [0] = trans ID, [1] = result
            result = array[1];
            if (result == GridSimTags.AR_MODIFY_SUCCESS)
            {
                // replace the old values with new ones
                obj.setStatus(GridSimTags.AR_STATUS_NOT_STARTED);
                old.copy(obj);
            }
        }
        catch (Exception e) {
            result = GridSimTags.AR_MODIFY_ERROR;
        }

        return result;
    }

    /**
     * Cancels a given reservation. All Gridlets associated with this
     * reservation will automatically be cancelled. Users need to retrieve
     * the Gridlets manually, by using GridSim.gridletReceive().
     * A return value for this method uses one of
     * of GridSimTags.AR_CANCEL_XXXX tags, where XXXX = specific tag name.
     *
     * @param bookingID   this reservation booking ID
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.GridSim#gridletReceive()
     * @see gridsim.GridSim#gridletReceive(int, int, int)
     * @pre bookingID != null
     * @post $none
     */
    public int cancelReservation(String bookingID) {
        return cancelReservation(bookingID, -1);
    }

    /**
     * Cancels a list of Gridlets for a given reservation.
     * Users need to retrieve
     * the Gridlets manually, by using GridSim.gridletReceive().
     * If cancellation of a Gridlet fails, then it will ignore the rest of the
     * list.
     * A return value for this method uses one of
     * of GridSimTags.AR_CANCEL_XXXX tags, where XXXX = specific tag name.
     * <br>
     * <b>NOTE:</b> This method is similar to GridSim.gridletCancel()
     *
     * @param bookingID   this reservation booking ID
     * @param list        a list of Gridlet IDs (each ID is an Integer object).
     *                    Each Gridlet ID should be unique and no duplicate.
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.GridSim#gridletReceive()
     * @see gridsim.GridSim#gridletReceive(int, int, int)
     * @see gridsim.GridSim#gridletCancel(int, int, int, double)
     * @see gridsim.GridSim#gridletCancel(Gridlet, int, double)
     * @pre bookingID != null
     * @pre list != null
     * @post $none
     */
    public int cancelReservation(String bookingID, ArrayList list)
    {
        if (bookingID == null) {
            return GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
        }

        // check the list
        if (list == null) {
            return GridSimTags.AR_CANCEL_FAIL;
        }

        // id[0] = resID, [1] = reservID, [2] = trans ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
        }

        // [0] = reservID, [1] = list, [2] = transID, [3] = sender ID
        Object[] commitObj = new Object[MAX_ID];
        commitObj[0] = new Integer(id[1]);
        commitObj[1] = list;

        int tagID = incrementID();
        commitObj[2] = new Integer(tagID);   // transaction id
        commitObj[3] = new Integer( super.get_id() );  // sender ID

        // Integer object size = 12 (incl. overheads)
        int size = list.size() * 12;

        // sends to a destinated grid resource
        super.send(super.output, 0.0, GridSimTags.SEND_AR_CANCEL,
                   new IO_data(commitObj, size, id[0]) );

        // waiting for a response from the GridResource with an unique tag
        // of transaction id
        FilterResult tag = new FilterResult(tagID,GridSimTags.RETURN_AR_CANCEL);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        // get the result back, sent by GridResource or AllocPolicy object
        int result = 0;
        try
        {
            int[] array = (int[]) ev.get_data(); // [0] = trans ID, [1] = result
            result = array[1];
        }
        catch (Exception e) {
            result = GridSimTags.AR_CANCEL_ERROR;
        }

        return result;
    }

    /**
     * Cancels a Gridlet for a given reservation.
     * Users need to retrieve
     * the Gridlet manually, by using GridSim.gridletReceive().
     * A return value for this method uses one of
     * of GridSimTags.AR_CANCEL_XXXX tags, where XXXX = specific tag name.
     * <br>
     * <b>NOTE:</b> This method is similar to GridSim.gridletCancel()
     *
     * @param bookingID   this reservation booking ID
     * @param gl          a Gridlet object
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.GridSim#gridletReceive()
     * @see gridsim.GridSim#gridletReceive(int, int, int)
     * @see gridsim.GridSim#gridletCancel(int, int, int, double)
     * @see gridsim.GridSim#gridletCancel(Gridlet, int, double)
     * @pre bookingID != null
     * @pre gl != null
     * @post $none
     */
    public int cancelReservation(String bookingID, Gridlet gl)
    {
        if (gl == null) {
            return GridSimTags.AR_CANCEL_ERROR;
        }

        return cancelReservation(bookingID, gl.getGridletID());
    }

    /**
     * Cancels a Gridlet for a given reservation.
     * Users need to retrieve
     * the Gridlet manually, by using GridSim.gridletReceive().
     * A return value for this method uses one of
     * of GridSimTags.AR_CANCEL_XXXX tags, where XXXX = specific tag name.
     * <br>
     * <b>NOTE:</b> This method is similar to GridSim.gridletCancel()
     *
     * @param bookingID   this reservation booking ID
     * @param gridletID   a Gridlet ID
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.GridSim#gridletReceive()
     * @see gridsim.GridSim#gridletReceive(int, int, int)
     * @see gridsim.GridSim#gridletCancel(int, int, int, double)
     * @see gridsim.GridSim#gridletCancel(Gridlet, int, double)
     * @pre bookingID != null
     * @pre gridletID >= 0
     * @post $none
     */
    public int cancelReservation(String bookingID, int gridletID)
    {
        if (bookingID == null) {
            return GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
        }

        // id[0] = resID, [1] = reservID, [2] = trans ID, [3] = sender ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
        }

        int resourceID = id[0];    // get the resource ID
        id[0] = gridletID;         // overrides resource ID with a gridlet id
        id[2] = incrementID();     // transaction id
        id[3] = super.get_id();    // this entity or sender ID

        // send to grid resource to cancel a reservation
        super.send(super.output, 0.0, GridSimTags.SEND_AR_CANCEL,
                   new IO_data(id, SIZE_ARRAY, resourceID) );

        // waiting for a response from the GridResource with an unique tag
        // of transaction id
        FilterResult tag = new FilterResult(id[2],GridSimTags.RETURN_AR_CANCEL);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        // get the result back, sent by GridResource or AllocPolicy object
        int result = 0;
        try
        {
            int[] array = (int[]) ev.get_data();  // [0] = tag ID, [1] = result
            result = array[1];
        }
        catch (Exception e) {
            result = GridSimTags.AR_CANCEL_ERROR;
        }

        return result;
    }

    /**
     * Querys to a resource regarding to list of free time during a period of
     * time. Each object inside ArrayList is <tt>long array[3]</tt>, with:
     * <ul>
     *    <li> array[0] = start time
     *    <li> array[1] = duration time
     *    <li> array[2] = number of PEs
     * </ul>
     *
     * @param resourceID   a resource ID
     * @param from         starting time in milliseconds
     * @param to           ending time in milliseconds
     * @return ArrayList object or <tt>null</tt> if error occurs
     * @pre resourceID != null
     * @pre from > 0
     * @pre to > 0
     * @post $none
     */
    public ArrayList queryFreeTime(Integer resourceID, long from, long to)
    {
        if (resourceID == null) {
            return null;
        }
        return queryFreeTime(resourceID.intValue(), from, to);
    }

    /**
     * Querys to a resource regarding to list of free time during a period of
     * time. Each object inside ArrayList is <tt>long array[3]</tt>, with:
     * <ul>
     *    <li> array[0] = start time
     *    <li> array[1] = duration time
     *    <li> array[2] = number of PEs
     * </ul>
     *
     * @param resourceID   a resource ID
     * @param from         starting time in milliseconds
     * @param to           ending time in milliseconds
     * @return ArrayList object or <tt>null</tt> if error occurs
     * @pre resourceID > 0
     * @pre from > 0
     * @pre to > 0
     * @post $none
     */
    public ArrayList queryFreeTime(int resourceID, long from, long to)
    {
        int tag = GridSimTags.SEND_AR_LIST_FREE_TIME;
        return queryTime(resourceID, from, to, tag);
    }

    /**
     * Querys to a resource regarding to list of busy time during a period of
     * time. Each object inside ArrayList is <tt>long array[3]</tt>, with:
     * <ul>
     *    <li> array[0] = start time
     *    <li> array[1] = duration time
     *    <li> array[2] = number of PEs
     * </ul>
     *
     * @param resourceID   a resource ID
     * @param from         starting time in milliseconds
     * @param to           ending time in milliseconds
     * @return ArrayList object or <tt>null</tt> if error occurs
     * @pre resourceID > 0
     * @pre from > 0
     * @pre to > 0
     * @post $none
     */
    public ArrayList queryBusyTime(int resourceID, long from, long to)
    {
        int tag = GridSimTags.SEND_AR_LIST_BUSY_TIME;
        return queryTime(resourceID, from, to, tag);
    }

    /**
     * Sends a request to a resource regarding to either free or busy time.
     * @param resourceID   a resource ID
     * @param from         starting time
     * @param to           ending time
     * @param tag          a tag for either free or busy time
     * @return ArrayList object or <tt>null</tt> if error occurs
     * @pre resourceID > 0
     * @pre from > 0
     * @pre to > 0
     * @post $none
     */
    private ArrayList queryTime(int resourceID, long from, long to, int tag)
    {
        int DEFAULT = 1;   // don't care about num PE
        String str = validateValue(from, to, DEFAULT, resourceID, true);

        // if there is an error msg, then return null
        if (str != null) {
            return null;
        }

        int id = incrementID();              // transaction ID
        int size = ARObject.getByteSize();   // object size to be sent

        // create a new object
        ARObject data = new ARObject(super.get_id(), timeZone_);
        int duration = (int) (to - from);
        data.setStartTime(from);             // from start time
        data.setDurationTime(duration);      // duration time
        data.setTransactionID(id);           // transaction ID
        data.setResourceID(resourceID);      // resource ID

        // send to a resource ID
        super.send(super.output, 0, tag, new IO_data(data, size, resourceID));

        // wait for feedback whether the reservation has been accepted or not
        // waiting for a response from the GridResource
        tag = GridSimTags.RETURN_AR_QUERY_TIME;
        FilterQueryTimeAR simTag = new FilterQueryTimeAR(id, tag);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(simTag, ev);

        ArrayList obj = null;
        try
        {
            Object[] array = (Object[]) ev.get_data();  // [0] = trans ID
            obj = (ArrayList) array[1];   // [1] = result
        }
        catch (Exception e) {
            obj = null;
        }

        return obj;
    }

    /**
     * Querys to a resource regarding to list of busy time during a period of
     * time. Each object inside ArrayList is <tt>long array[3]</tt>, with:
     * <ul>
     *    <li> array[0] = start time
     *    <li> array[1] = duration time
     *    <li> array[2] = number of PEs
     * </ul>
     *
     * @param resourceID   a resource ID
     * @param from         starting time in milliseconds
     * @param to           ending time in milliseconds
     * @return ArrayList object or <tt>null</tt> if error occurs
     * @pre resourceID != null
     * @pre from > 0
     * @pre to > 0
     * @post $none
     */
    public ArrayList queryBusyTime(Integer resourceID, long from, long to)
    {
        if (resourceID == null) {
            return null;
        }
        return queryBusyTime(resourceID.intValue(), from, to );
    }

    /**
     * Gets a reservation object based on the given booking ID
     * @param bookingID   a reservation booking ID
     * @return ARObject object or <tt>null</tt> if an error occurs
     * @pre bookingID != null
     * @post $none
     */
    public ARObject getReservation(String bookingID)
    {
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return null;
        }

        ARObject obj = searchBooking(id[0], id[1]);
        return obj;
    }

    /**
     * Queries the overall status of a reservation.
     * A return value for this method uses one of
     * of GridSimTags.AR_STATUS_XXXX tags, where XXXX = specific tag name.
     * <p>
     * To find out the status for a specific Gridlet, use
     * {@link gridsim.GridSim#gridletStatus(int, int, int)} method instead.
     *
     * @param bookingID   this reservation booking ID
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.GridSim#gridletStatus(int, int, int)
     * @see gridsim.GridSim#gridletStatus(Gridlet, int)
     * @pre bookingID != null
     * @post $none
     */
    public int queryReservation(String bookingID)
    {
        // id[0] = resID, [1] = reservID, [2] = trans ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_STATUS_ERROR_INVALID_BOOKING_ID;
        }

        // search from the list first
        ARObject obj = searchBooking(id[0], id[1]);
        if (obj == null) {
            return GridSimTags.AR_STATUS_ERROR_INVALID_BOOKING_ID;
        }

        // if a reservation hasn't been committed
        else if (!obj.hasCommitted()) {
            return GridSimTags.AR_STATUS_NOT_COMMITTED;
        }

        // if the reservation status is one of the final states, then no need
        // to enquiry a resource
        int status = obj.getStatus();

        // if a reservation has been committed, then need to ask a resource
        id[2] = incrementID();   // transaction id
        id[3] = super.get_id();

        // send to grid resource to query about the reservation id
        super.send(super.output, 0.0, GridSimTags.SEND_AR_QUERY,
                   new IO_data(id, SIZE_ARRAY, id[0]) );

        // waiting for a response from the GridResource
        FilterResult tag = new FilterResult(id[2], GridSimTags.RETURN_AR_QUERY_STATUS);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        // get the result back from GridResource or AllocPolicy object
        try
        {
            int[] array = (int[]) ev.get_data(); // [0] = trans ID, [1] = result
            status = array[1];
        }
        catch (Exception e) {
            status = GridSimTags.AR_STATUS_ERROR;
        }

        obj.setStatus(status);   // put the latest status
        return status;
    }

    /**
     * Commits a reservation only <b>without</b> sending any Gridlet objects.
     * Once a commit has been successfull, sending the Gridlet objects must be
     * done by using {@link #commitReservation(String, GridletList)} or
     * {@link #commitReservation(String, Gridlet)}.
     * A return value for this method uses one of
     * of GridSimTags.AR_COMMIT_XXXX tags, where XXXX = specific tag name.
     *
     * @param bookingID   a reservation booking ID
     * @return an integer tag that denotes success or failure.
     * @see gridsim.GridSimTags
     * @see gridsim.AdvanceReservation#commitReservation(String, Gridlet)
     * @see gridsim.AdvanceReservation#commitReservation(String, GridletList)
     * @pre bookingID != null
     * @post $none
     */
     public int commitReservation(String bookingID)
     {
         int result = 0;

         // id[0] = resID, [1] = reservID, [2] = trans ID
         int[] id = parseBookingID(bookingID);
         if (id == null) {
             result = GridSimTags.AR_COMMIT_FAIL_INVALID_BOOKING_ID;
         }
         else
         {
             id[2] = incrementID();   // transaction id
             id[3] = super.get_id();  // this entity or sender id

             // sends to a destinated GridResource
             super.send(super.output, 0.0, GridSimTags.SEND_AR_COMMIT_ONLY,
                        new IO_data(id, SIZE_ARRAY, id[0]) );

             // waiting for a response from the GridResource with an unique tag
             // of transaction id
             FilterResult tag = new FilterResult(id[2], GridSimTags.RETURN_AR_COMMIT);

             // only look for this type of ack for same Gridlet ID
             Sim_event ev = new Sim_event();
             super.sim_get_next(tag, ev);

             try
             {
                 // get the result back
                 int[] array = (int[]) ev.get_data(); //[0]=trans ID, [1]=result
                 result = array[1];

                 // update the booking list and return the new result
                 result = updateCommitList(id[0], id[1], result);
             }
             catch (Exception e) {
                 result = GridSimTags.AR_COMMIT_ERROR;
             }
         }

         return result;
     }

    /**
     * Commits a reservation together <b>with</b> a list of Gridlet objects.
     * A return value for this method uses one of
     * of GridSimTags.AR_COMMIT_XXXX tags, where XXXX = specific tag name.
     *
     * @param bookingID  a reservation booking ID
     * @param list       a list of Gridlet objects. Each Gridlet's user ID, by
     *        default is set to this entity ID.
     * @return an integer tag that denotes success or failure.
     * @pre bookingID != null
     * @pre list != null
     * @post $none
     * @see gridsim.Gridlet#setUserID(int)
     * @see gridsim.GridSimTags
     */
    public int commitReservation(String bookingID, GridletList list)
    {
        // id[0] = resID, [1] = reservID, [2] = trans ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_COMMIT_FAIL_INVALID_BOOKING_ID;
        }

        // checks if the list is empty or not
        int result = 0;
        if (list == null) {
            result = GridSimTags.AR_COMMIT_FAIL;
        }
        else
        {
            int size = calculateTotalGridletSize(list);
            if (size == 0) {
                result = GridSimTags.AR_COMMIT_FAIL;
            }
            else {
                result = commit(id[0], id[1], list, size);
            }
        }

        return result;
    }

    /**
     * Commits a reservation together <b>with</b> a Gridlet object.
     * A return value for this method uses one of
     * of GridSimTags.AR_COMMIT_XXXX tags, where XXXX = specific tag name.
     *
     * @param bookingID  a reservation booking ID
     * @param obj        a Gridlet object. A Gridlet's user ID, by default is
     *        set to this entity ID.
     * @return an integer tag that denotes success or failure.
     * @pre bookingID != null
     * @pre obj != null
     * @post $none
     * @see gridsim.Gridlet#setUserID(int)
     * @see gridsim.GridSimTags
     */
    public int commitReservation(String bookingID, Gridlet obj)
    {
        // id[0] = resID, [1] = reservID, [2] = trans ID
        int[] id = parseBookingID(bookingID);
        if (id == null) {
            return GridSimTags.AR_COMMIT_FAIL_INVALID_BOOKING_ID;
        }

        int result = 0;
        if (obj == null) {
            result = GridSimTags.AR_COMMIT_FAIL;
        }
        else
        {
            obj.setUserID( super.get_id() );
            result = commit( id[0], id[1], obj, obj.getGridletFileSize() );
        }

        return result;
    }

    //////////////////////////////////// PRIVATE METHODS ////////////////////

    /**
     * Initialises all the private attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        initTime_ = GridSim.getSimulationCalendar().getTimeInMillis();
        booking_ = new ArrayList();
        transactionID_ = 0;
    }

    /**
     * Increment a transaction id for each reservation method that
     * communicate with a resource
     * @return a unique transaction id
     * @pre $none
     * @post $result > 1
     */
    private int incrementID()
    {
        transactionID_++;
        return transactionID_;
    }

    /**
     * Validates or checks whether one or more given parameters are valid or not
     * @param startTime  a reservation start time
     * @param endTime    a reservation end time
     * @param numPE      number of PE required by a reservation
     * @param resID      a resource ID
     * @param ar         true if it is AR, false if it is immediate reservation
     * @return an error message or <tt>null</tt> if the parameters are all valid
     * @pre $none
     * @post $none
     */
    private String validateValue(long startTime, long endTime, int numPE,
                                 int resID, boolean ar)
    {
        // current time = time to create this object + simulation time
        long currentTime = initTime_ + (int) (GridSim.clock() * MILLI_SEC);

        // checks the start time of a reservation. start time = 0 means
        // it is an immediate reservation rather than AR.
        if (!ar && startTime < 0) {
            return "AR_CREATE_ERROR_INVALID_START_TIME";
        }
        else if (ar && startTime < currentTime) {
            return "AR_CREATE_ERROR_INVALID_START_TIME";
        }

        // checks the end time of a reservation. end time = 0 means it is an
        // immediate reservation rather than AR.
        if (ar == false && endTime < 0) {
            return "AR_CREATE_ERROR_INVALID_END_TIME";
        }
        else if (ar)
        {
            if (endTime < currentTime || endTime < startTime) {
                return "AR_CREATE_ERROR_INVALID_END_TIME";
            }
        }

        // if num PE less than 1
        if (numPE < 1) {
            return "AR_CREATE_ERROR_INVALID_NUM_PE";
        }

        // if a resource doesn't exist
        if (GridSim.isResourceExist(resID) == false) {
            return "AR_CREATE_ERROR_INVALID_RESOURCE_ID";
        }

        // check whether a resource supports AR or not
        if (GridSim.resourceSupportAR(resID) == false) {
            return "AR_CREATE_ERROR_FAIL_RESOURCE_CANT_SUPPORT";
        }

        // if no errors, then return null
        return null;
    }

    /**
     * Validates or checks whether one or more given parameters are valid or not
     * @param startTime  a reservation start time
     * @param duration   a reservation duration time
     * @param numPE      number of PE required by a reservation
     * @param resID      a resource ID
     * @param ar         true if it is AR, false if it is immediate reservation
     * @return an error message or <tt>null</tt> if the parameters are all valid
     * @pre $none
     * @post $none
     */
    private String validateValue(long startTime, int duration, int numPE,
                                 int resID, boolean ar)
    {
        // current time = time to create this object + simulation time
        long currentTime = initTime_ + (int) (GridSim.clock()*MILLI_SEC);

        // checks the start time of a reservation. start time = 0 means it is
        // an immediate reservation not AR.
        if (!ar && startTime < 0) {
            return "AR_CREATE_ERROR_INVALID_START_TIME";
        }
        if (ar && startTime < currentTime) {
            return "AR_CREATE_ERROR_INVALID_START_TIME";
        }

        // checks the duration time of a reservation. duration = 0 means
        // it is an immediate reservation not AR.
        if (ar == false && duration < 0) {
            return "AR_CREATE_ERROR_INVALID_END_TIME";
        }
        else if (ar)
        {
            long endTime = startTime + (duration * MILLI_SEC);
            if (endTime < currentTime || endTime < startTime) {
                return "AR_CREATE_ERROR_INVALID_END_TIME";
            }
        }

        // if num PE less than 1
        if (numPE < 1) {
            return "AR_CREATE_ERROR_INVALID_NUM_PE";
        }

        // if a resource doesn't exist
        if (GridSim.isResourceExist(resID) == false) {
            return "AR_CREATE_ERROR_INVALID_RESOURCE_ID";
        }

        // check whether a resource supports AR or not
        if (GridSim.resourceSupportAR(resID) == false) {
            return "AR_CREATE_ERROR_FAIL_RESOURCE_CANT_SUPPORT";
        }

        // if no errors, then return null
        return null;
    }

    /**
     * Sends a message to a resource for committing this reservation
     * @param resID    a resource ID
     * @param reservationID    a reservation booking ID
     * @param obj      a generic object that contains either a <tt>Gridlet</tt>
     *                 or a <tt>GridletList</tt>
     * @param size     total size of the object
     * @return commit result from the grid resource
     * @pre $none
     * @post $result > 1
     */
    private int commit(int resID, int reservationID, Object obj, long size)
    {
        // [0] = reservID, [1] = gridlet(s), [2] = transID, [3] = sender ID
        Object[] commitObj = new Object[MAX_ID];
        commitObj[0] = new Integer(reservationID);
        commitObj[1] = obj;

        int tagID = incrementID();
        commitObj[2] = new Integer(tagID);   // transaction id
        commitObj[3] = new Integer( super.get_id() );  // sender ID

        // sends to a destinated GridResource
        super.send(super.output, 0.0, GridSimTags.SEND_AR_COMMIT_WITH_GRIDLET,
              new IO_data(commitObj, size, resID) );

        // waiting for a response from the GridResource with an unique tag of
        // transaction id
        FilterResult tag = new FilterResult(tagID,GridSimTags.RETURN_AR_COMMIT);

        // only look for this type of ack for same Gridlet ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        int result = 0;
        try
        {
            // get the result back
            int[] array = (int[]) ev.get_data(); // [0] = trans ID, [1] = result
            result = array[1];

            // update the booking list and return the new result
            result = updateCommitList(resID, reservationID, result);
        }
        catch (Exception e) {
            result = GridSimTags.AR_COMMIT_ERROR;
        }

        return result;
    }

    /**
     * Parses the booking ID. In addition check whether the ID exists or not.
     * @param bookingID   a reservation booking ID
     * @return an integer array
     * @pre $none
     * @post $none
     */
    private int[] parseBookingID(String bookingID)
    {
        int[] idArray = null;
        try
        {
            // exit if bookingID is empty
            if (bookingID == null || bookingID.length() == 0) {
                return null;
            }

            int MAX_ATTR = 2;    // 2 members: resID_reservationID
            String[] list = bookingID.split("_");

            if (list.length != MAX_ATTR) {
                return null;
            }

            // [0] = resID, [1] = reservID, [2] = trans ID, [3] = this entity ID
            idArray = new int[MAX_ID];
            idArray[0] = Integer.parseInt(list[0]);   // to get resID
            idArray[1] = Integer.parseInt(list[1]);   // to get reservationID

            // if the resource id doesn't exist or there are more IDs
            if (GridSim.isResourceExist(idArray[0]) == false) {
                idArray = null;
            }
        }
        catch (Exception e)
        {
            idArray = null;
            System.out.println(super.get_name()
                    + ": Error - Invalid booking ID of " + bookingID);
        }

        return idArray;
    }

    /**
     * Sends a request to a resource for a new reservation.
     * @param resID    a resource ID
     * @param obj      a new reservation object
     * @param tag      a tag to determine whether sending advanced or immediate
     *                 reservation
     * @return the result of requesting a new reservation
     * @pre obj != null
     * @post $result != null
     */
    private String sendReservation(int resID, ARObject obj, int tag)
    {
        int tagID = incrementID();   // transaction ID
        obj.setTransactionID(tagID);

        // then send it to a gridresource
        super.send(super.output, GridSimTags.SCHEDULE_NOW, tag,
                new IO_data(obj, ARObject.getByteSize(), resID) );

        // wait for feedback whether the reservation has been accepted or not
        // waiting for a response from the GridResource
        FilterCreateAR tagObj = new FilterCreateAR(tagID, GridSimTags.RETURN_AR_CREATE);

        // only look for this type of ack for same reservation ID
        Sim_event ev = new Sim_event();
        super.sim_get_next(tagObj, ev);

        // gets the reservation id which must starts at 1 onwards ...
        // (if it is successful). 0 is used to denote a resource is empty
        int result = GridSimTags.AR_CREATE_ERROR;
        try
        {
            long array[] = (long []) ev.get_data();
            result = (int) array[1];   // reservation id

            if (result > 0)
            {
                ARObject reservObj = new ARObject(obj);

                // set reservation id and expiry time
                reservObj.setReservation(result, array[2]);
                reservObj.setStatus(GridSimTags.AR_STATUS_NOT_COMMITTED);

                booking_.add(reservObj);
                return resID + "_" + result;    // booking id = resID_reservID
            }
        }
        catch (Exception e) {
            result = GridSimTags.AR_CREATE_ERROR;
        }

        // if a new reservation failed, then get the error message
        return AdvanceReservation.getCreateResult(result);
    }

    /**
     * Calculates the total size of Gridlets
     * @param obj   a GridletList object
     * @return  total size
     * @pre $none
     * @post $result >= 1
     */
    private int calculateTotalGridletSize(GridletList obj)
    {
        int size = 0;
        try
        {
            Gridlet gl = null;
            Iterator it = obj.iterator();
            int userID = super.get_id();

            // iterates to all the objects
            while ( it.hasNext() )
            {
                gl = (Gridlet) it.next();
                gl.setUserID(userID);   // set user ID to this entity ID
                size += gl.getGridletFileSize();   // get the Gridlet size
            }
        }
        catch (Exception e) {
            size = 0;
        }

        return size;
    }

    /**
     * Updates the status of a particular booking id
     * @param resID    a resource ID
     * @param reservID    a reservation ID
     * @param status   a reservation status
     * @return latest status
     * @pre $none
     * @post $result > 1
     */
    private int updateCommitList(int resID, int reservID, int status)
    {
        int result = GridSimTags.AR_COMMIT_FAIL;
        ARObject data = searchBooking(resID, reservID);

        if (data != null)
        {
            if (status == GridSimTags.AR_COMMIT_SUCCESS)
            {
                data.setCommitted();
                data.setStatus(GridSimTags.AR_STATUS_NOT_STARTED);
                result = status;
            }
            else if (status == GridSimTags.AR_COMMIT_FAIL_EXPIRED)
            {
                data.setStatus(GridSimTags.AR_STATUS_EXPIRED);
                result = status;
            }
            else {
                data.setStatus(GridSimTags.AR_STATUS_ERROR);
            }
        }

        return result;
    }

    /**
     * Searches the booking list for a particular reservation
     * @param resID   a resource ID
     * @param reservID    a reservation ID
     * @return a reservation object or <tt>null</tt if it doesn't exist
     * @pre $none
     * @post $none
     */
    private ARObject searchBooking(int resID, int reservID)
    {
        ARObject data = null;
        boolean found = false;
        try
        {
            Iterator it = booking_.iterator();
            while ( it.hasNext() )
            {
                data = (ARObject) it.next();
                if (data.getReservationID() == reservID &&
                    data.getResourceID() == resID)
                {
                    found = true;
                    break;
                }
            }
        }
        catch (Exception e)
        {
            found = false;
            System.out.println(super.get_name() +
                    ": Error - can't search reservation booking.");
        }

        if (found == false) {
            data = null;
        }

        return data;
    }

} 

