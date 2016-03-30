/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

/**
 * ARPolicy is an abstract class that handles the internal
 * {@link gridsim.GridResource}
 * allocation policy related to Advanced Reservation functionalities.
 * New scheduling algorithms can be added into a GridResource
 * entity by extending this class and implement the required abstract methods.
 * {@link gridsim.AllocPolicy} abstract methods are also need to be
 * implemented.
 * <p>
 * All the implementation details and the data structures chosen are up to
 * the child class. All the protected methods and attributes are available
 * to code things easier. Child classes should use
 * <tt>replyXXXReservation()</tt> methods
 * where <i>XXX = Cancel / Commit / Query / Time / Create</i>. This is because
 * these methods send a correct format or message to
 * {@link gridsim.AdvanceReservation}
 * class. Sending an incorrect message will caused an exception on the
 * receiver's side.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @see gridsim.AdvanceReservation
 * @see gridsim.AllocPolicy
 * @invariant $none
 */
public abstract class ARPolicy extends AllocPolicy
{
    /** A constant variable that represents 1 second in 1,000 milliseconds. */
    protected static final int MILLI_SEC = 1000;      // 1 sec = 1,000 milli seconds
    private static final int MAX = 2;   // max. array size
    private static final int SIZE = 12;  // int = 4 bytes + overhead = 8 bytes
    private static final int TAG_SIZE = 6;
    private final int[] timeArray_ = {1, 5, 10, 15, 30, 45};  // time interval
    private int[] tagArray_;

    ///////////////////// ABSTRACT METHODS ////////////////////////////////

    /**
     * An abstract method that handles a new immediate reservation request.
     * {@link #replyCreateReservation(int, int, long, int)} method should be
     * used to send a result back to sender or user.
     * <p>
     * An immediate reservation requests a reservation immediately, i.e.
     * the current time is used as the start time with/without specifying
     * duration or end time. <br>
     * Immediate reservation can be done by one of the following ways:
     * <ol>
     * <li> <tt>start time = 0 (in long)</tt> and <tt>duration > 0</tt>.<br>
     *      If successful, expiry time should be set like this:
     *      <tt>expiry time = current time + duration.</tt>
     *      <br><br>
     * <li> <tt>start time = 0 (in long)</tt> and <tt>duration = 0</tt>.<br>
     *      This means a reservation is running as long as there are empty
     *      PEs available. <br> <b>NOTE:</b> using this approach, a reservation
     *      is having a risk of being pre-empted or terminated by a
     *      resource scheduler when new AR requests come. In addition,
     *      due to complexity, a resource's scheduler might not support
     *      this method. Finally, <tt>expiry time = 0</tt> since a scheduler
     *      can not determine it. <br>
     * </ol>
     * @param obj   a reservation object
     * @param senderID  a sender or user ID
     * @param sendTag   a tag to send to the user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleImmediateReservation(ARObject obj,
                                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a new advanced reservation request.
     * {@link #replyCreateReservation(int, int, long, int)} method should be
     * used to send a result back to sender or user.
     * @param obj   a reservation object
     * @param senderID  a sender or user ID
     * @param sendTag   a tag to send to the user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleCreateReservation(ARObject obj,
                                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a modify reservation request.
     * {@link #replyModifyReservation(int, int, int)} method should be used
     * to send a result back to sender or user.
     * @param obj  a reservation object
     * @param senderID   a sender or user ID
     * @param sendTag    a tag to send to the user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleModifyReservation(ARObject obj,
                                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a cancel reservation request.
     * This method cancels only for a particular Gridlet.
     * {@link #replyCancelReservation(int, int, int)} method should be used
     * to send a result back to sender or user.
     * @param reservationID   a reservation ID
     * @param senderID        a sender or user ID
     * @param gridletID       a gridlet ID
     * @param sendTag         a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleCancelReservation(int reservationID,
                                 int senderID, int gridletID, int sendTag);

    /**
     * An abstract method that handles a cancel reservation request.
     * This method cancels a list of Gridlet IDs.
     * {@link #replyCancelReservation(int, int, int)} method should be used
     * to send a result back to sender or user.
     * @param reservationID   a reservation ID
     * @param senderID        a sender or user ID
     * @param list            a list of Gridlet IDs (each ID is represented as
     *                        an Integer object)
     * @param sendTag         a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre list != null
     * @post $none
     */
    public abstract void handleCancelReservation(int reservationID,
                                 int senderID, ArrayList list, int sendTag);

    /**
     * An abstract method that handles a cancel reservation request.
     * This method cancels all Gridlets for a given reservation ID.
     * {@link #replyCancelReservation(int, int, int)} method should be used
     * to send a result back to sender or user.
     * @param reservationID   a reservation ID
     * @param senderID        a sender or user ID
     * @param sendTag         a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleCancelReservation(int reservationID,
                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a commit reservation request.
     * This method commits a reservation only. Gridlets are submitted using
     * {@link #handleCommitReservation(int, int, int, Gridlet)} or
     * {@link #handleCommitReservation(int, int, int, GridletList)} method.<br>
     * {@link #replyCommitReservation(int, int, int)} method should be used
     * to send a result back to sender or user.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleCommitOnly(int reservationID,
                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a commit reservation request.
     * This method commits a reservation and submits a Gridlet to be processed
     * as well.
     * {@link #replyCommitReservation(int, int, int)} method should be
     * used to send a result back to sender or user.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @param gridlet        a Gridlet object
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre gridlet != null
     * @post $none
     */
    public abstract void handleCommitReservation(int reservationID,
                                 int senderID, int sendTag, Gridlet gridlet);

    /**
     * An abstract method that handles a commit reservation request.
     * This method commits a reservation and submits a list of  Gridlets
     * to be processed as well.
     * {@link #replyCommitReservation(int, int, int)} method should be
     * used to send a result back to sender or user.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @param list           a list of Gridlet objects
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre list != null
     * @post $none
     */
    public abstract void handleCommitReservation(int reservationID,
                                 int senderID, int sendTag, GridletList list);

    /**
     * An abstract method that handles a query reservation request.
     * {@link #replyQueryReservation(int, int, int)} method should be
     * used to send a result back to sender or user.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleQueryReservation(int reservationID,
                                 int senderID, int sendTag);

    /**
     * An abstract method that handles a query busy time request.
     * {@link #replyTimeReservation(int, int, ArrayList, double)} method should
     * be used to send a result back to sender or user.
     * @param from    starting period time (local resource time)
     * @param to      ending period time (local resource time)
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre from > 0
     * @pre to > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleQueryBusyTime(long from, long to, int senderID,
                                 int sendTag, double userTimeZone);

    /**
     * An abstract method that handles a query free time request.
     * {@link #replyTimeReservation(int, int, ArrayList, double)} method should
     * be used to send a result back to sender or user.
     * @param from    starting period time (local resource time)
     * @param to      ending period time (local resource time)
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre from > 0
     * @pre to > 0
     * @pre senderID > 0
     * @post $none
     */
    public abstract void handleQueryFreeTime(long from, long to, int senderID,
                                 int sendTag, double userTimeZone);

    ///////////////////// END OF ABSTRACT METHODS ////////////////////////////

    /**
     * Allocates a new ARPolicy object. A child class should call this method
     * during its constructor. The name of this entity (or the child class that
     * inherits this class) will be <tt>"resName_entityName"</tt>.
     *
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
     *
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre resourceName != null
     * @pre entityName != null
     * @post $none
     */
    protected ARPolicy(String resourceName, String entityName) throws Exception
    {
        super(resourceName, entityName);

        // size = 3 * Time interval to store tags for sec, min and hour
        tagArray_ = new int[TAG_SIZE + TAG_SIZE + TAG_SIZE];

        // populating the data. Just make sure the tag numbers are decremented
        // by 1
        for (int i = 0; i < tagArray_.length; i++) {
            tagArray_[i]= GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_1_SEC - i;
        }
    }

    /**
     * Sends a result of a create reservation request.
     * @param destID   a destination or user ID
     * @param tag      a tag to send to the user
     * @param expiryTime   reservation expiry time. If no empty slots, then
     *        expiryTime should be set to -1
     * @param reservID     a reservation ID. If no empty slots, then reservID
     *        should be set to one of GridSimTags.AR_CREATE_XXXX tags where
     *        XXXX = a specific tag name.
     * @pre destID > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyCreateReservation(int destID, int tag, long expiryTime,
                                          int reservID)
    {
        long[] sendArray = new long[MAX+1];
        sendArray[0] = tag - GridSimTags.RETURN_AR_CREATE;
        sendArray[1] = reservID;     // reservation id
        sendArray[2] = expiryTime;   // expiry time

        int size = (3*8) + 6;  // 3 longs * 8 bytes + overheads
        super.sim_schedule(super.outputPort_, 0, GridSimTags.RETURN_AR_CREATE,
                           new IO_data(sendArray, size, destID) );
    }

    /**
     * Sends a result of a cancel reservation request.
     * @param destID     a destination or user ID
     * @param tag        a tag to send to the user
     * @param result     a result tag. This tag should be one of
     *        GridSimTags.AR_CANCEL_XXXX where XXXX = a specific tag name.
     * @pre destID > 0
     * @pre tag > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyCancelReservation(int destID, int tag, int result)
    {
        int[] array = new int[MAX];
        array[0] = tag - GridSimTags.RETURN_AR_CANCEL;   // transaction ID
        array[1] = result;          // outcome of this functionality

        // for cancel, just send the tag result
        super.sim_schedule(super.outputPort_, 0, GridSimTags.RETURN_AR_CANCEL,
                           new IO_data(array, SIZE, destID) );
    }

    /**
     * Sends a result of a commit reservation request.
     * @param destID     a destination or user ID
     * @param tag        a tag to send to the user
     * @param result     a result tag. This tag should be one of
     *        GridSimTags.AR_COMMIT_XXXX where XXXX = a specific tag name.
     * @pre destID > 0
     * @pre tag > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyCommitReservation(int destID, int tag, int result)
    {
        int[] array = new int[MAX];
        array[0] = tag - GridSimTags.RETURN_AR_COMMIT;   // transaction ID
        array[1] = result;          // outcome of this functionality

        // for commit, just send the tag result, same as cancel
        super.sim_schedule(super.outputPort_, 0, GridSimTags.RETURN_AR_COMMIT,
                           new IO_data(array, SIZE, destID) );
    }

    /**
     * Sends a result of a query reservation request.
     * @param destID     a destination or user ID
     * @param tag        a tag to send to the user
     * @param result     a result tag. This tag should be one of
     *        GridSimTags.AR_STATUS_XXXX where XXXX = a specific tag name.
     * @pre destID > 0
     * @pre tag > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyQueryReservation(int destID, int tag, int result)
    {
        int[] array = new int[MAX];
        array[0] = tag - GridSimTags.RETURN_AR_QUERY_STATUS;  // transaction ID
        array[1] = result;          // outcome of this functionality

        // for query, just send the tag result
        super.sim_schedule(super.outputPort_, 0, GridSimTags.RETURN_AR_QUERY_STATUS,
                           new IO_data(array, SIZE, destID) );
    }

    /**
     * Sends a result of a modify reservation request.
     * @param destID     a destination or user ID
     * @param tag        a tag to send to the user
     * @param result     a result tag. This tag should be one of
     *        GridSimTags.AR_MODIFY_XXXX where XXXX = a specific tag name.
     * @pre destID > 0
     * @pre tag > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyModifyReservation(int destID, int tag, int result)
    {
        int[] array = new int[MAX];
        array[0] = tag - GridSimTags.RETURN_AR_MODIFY;  // transaction ID
        array[1] = result;          // outcome of this functionality

        // for modification, just send the tag result
        super.sim_schedule(super.outputPort_, 0, GridSimTags.RETURN_AR_MODIFY,
                           new IO_data(array, SIZE, destID) );
    }

    // this is in response to queryBusyTime() and queryFreeTime()
    /**
     * Sends a result of a query busy or free time request.
     * @param destID     a destination or user ID
     * @param tag        a tag to send to the user
     * @param result     a list of results.
     * Each object inside ArrayList is <tt>long array[3]</tt>, with:
     * <ul>
     *    <li> array[0] = start time
     *    <li> array[1] = duration time
     *    <li> array[2] = number of PEs
     * </ul>
     *
     * @pre destID > 0
     * @post $none
     * @see gridsim.GridSimTags
     */
    protected void replyTimeReservation(int destID, int tag, ArrayList result,
                            double userTimeZone)
    {
        int size = SIZE;
        if (result != null) {
            size = (4*8) + result.size();  // 3 longs + array size + overheads
        }

        Object[] array = new Object[MAX];
        array[0] = new Integer(tag - GridSimTags.RETURN_AR_QUERY_TIME);
        array[1] = result;

        // NOTE: need to convert all start time into user local time
        convertToUserTime(result, userTimeZone);

        // for query, just send the result list
        super.sim_schedule(super.outputPort_,0,GridSimTags.RETURN_AR_QUERY_TIME,
                           new IO_data(array, size, destID) );
    }

    // only convert the start time (result[0]) from local/resource time to
    // user time
    private void convertToUserTime(ArrayList result, double userTimeZone)
    {
        if (result == null) {
            return;
        }

        double resTimeZone = resource_.getResourceTimeZone();
        long[] array = null;
        long from = 0;

        try
        {
            // convert the start time of a local time to a user time
            Iterator it = result.iterator();
            while ( it.hasNext() )
            {
                array = (long[]) it.next();
                from = AdvanceReservation.convertTimeZone(array[0], resTimeZone,
                            userTimeZone);
                array[0] = from;
            }
        }
        catch (Exception e) {
            // print error msg
        }
    }

    /**
     * Gets the current time. Time is calculated from simulation init time +
     * (GridSim.clock() * MILLI_SEC), where MILLI_SEC = 1000.
     * @return current time in milliseconds
     * @pre $none
     * @post $result > 0
     */
    protected long getCurrentTime() {
        return initTime_ + (int) (GridSim.clock() * MILLI_SEC);
    }

    /**
     * Search for a particular reservation in a data structure
     * @param obj   a data structure
     * @param reservationID  a reservation ID
     * @return location in the data structure or <tt>-1</tt> if not found
     * @pre obj != null
     * @post $none
     */
    protected int searchReservation(Collection obj, int reservationID)
    {
        ARObject arObj = null;
        int found = -1;     // means the Gridlet is not in the list

        try
        {
            // Search through the list to find the given Gridlet object
            int i = 0;
            Iterator iter = obj.iterator();
            while ( iter.hasNext() )
            {
                arObj = (ARObject) iter.next();

                // Need to check against the userId as well since
                // each user might have same Gridlet id submitted to
                // same GridResource
                if (arObj.getReservationID() == reservationID)
                {
                    found = i;
                    break;
                }

                i++;
            }
        }
        catch (Exception e)
        {
            System.out.println("ARPolicy.findReservedGridlet(): Error occurs.");
            System.out.println( e.getMessage() );
        }

        return found;
    }

    /**
     * Approximates busy time
     * @param time   busy time
     * @return a time interval tag. This tag should be one of
     *         GridSimTags.AR_CREATE_FAIL_RESOURCE_FULL_IN_XXXX where
     *         XXXX = a specific time interval.
     */
    protected int approxBusyTime(long time)
    {
        int SECOND = 1;
        int MINUTE = 60 * SECOND;  // 1 minute = 60 secs
        int HOUR = 60 * MINUTE;    // 1 hour = 60 minute
        int busy = (int) time / MILLI_SEC;   // already in seconds

        // find one of the type: sec or min or hour
        int type = 0;
        if (busy < MINUTE) {
            type = SECOND;
        }
        else if (busy < HOUR) {
            type = MINUTE;
        }
        else {
            type = HOUR;
        }

        // then do a loop to find the nearest time interval. Always rounding
        // up, e.g. time is 6 minutes, so nearest time would be 10 minutes.
        int temp = 0;
        int i = 0;
        for (i = 0; i < timeArray_.length; i++)
        {
            temp = timeArray_[i] * type;
            if (busy <= temp) {
                break;
            }
        }

        // make sure i is not out of range, since array starts at 0
        if (i == timeArray_.length) {
            i = timeArray_.length - 1;
        }

        // then find the exact location to determine the correct tag value
        // tag[0 ... (TAG_SIZE-1)] = time interval for seconds
        if (type == SECOND) {
            temp = 0;
        }
        // tag[TAG_SIZE ... (TAG_SIZE+TAG_SIZE - 1)] = time interval for minutes
        else if (type == MINUTE) {
            temp = TAG_SIZE;
        }
        // the rests are for hour intervals
        else {
            temp = TAG_SIZE + TAG_SIZE;
        }

        return tagArray_[i + temp];
    }

} 
