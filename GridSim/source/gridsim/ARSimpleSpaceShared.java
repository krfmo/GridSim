/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.*;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;

/**
 * This is a resource scheduler that handles Advanced Reservation
 * functionalities. This scheduler is also able to handle submitted jobs without
 * reserving in the first place.
 * <p>
 * As this class name suggested, this scheduler is only able to handle some
 * basic AR functionalities, such as:
 * <ul>
 *    <li> process a new advanced reservation functionality
 *    <li> process a new immediate reservation functionality
 *    <li> process a cancel reservation functionality
 *    <li> process a commit reservation functionality
 *    <li> process a reservation status functionality
 * </ul>
 * <p>
 * There are some limitations on this scheduler:
 * <ul>
 *     <li> only able to handle 1 Gridlet on each PE throughout the entire
 *          reservation period. This means that if a
 *          reservation books 3 PEs, then it should submit only 3 Gridlets.
 *          If a reservation sends, e.g. 5 Gridlets, the remaining 2 Gridlets
 *          will be ignored during a commit reservation phase.
 *     <li> not able to split a Gridlet to run into more than one PE if empty
 *          PEs are available.
 *     <li> not able to list busy and free time for a certain period of time.
 *     <li> not able to modify an existing reservation.
 * </ul>
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @invariant $none
 */
public class ARSimpleSpaceShared extends ARPolicy
{
    // NOTE: gridletQueueList_ is for non-AR jobs, where gridletWaitingList_
    // is for AR jobs.
    private ResGridletList gridletQueueList_;     // Queue list
    private ResGridletList gridletInExecList_;    // Execution list
    private ResGridletList gridletPausedList_;    // Pause list
    private ResGridletList gridletWaitingList_;   // waiting list for AR

    private double lastUpdateTime_;    // the last time Gridlets updated
    private int[] machineRating_;      // list of machine ratings available

    private ArrayList reservList_;  // a new reservation list
    private ArrayList expiryList_;  // a list that contains expired reservations

    private int reservID_;          // reservation ID
    private int commitPeriod_;      // default booking/reservation commit period
    private static final int SUCCESS = 1;        // a constant to denote success
    private static final int NOT_FOUND = -1;     // a constant to denote not found
    private static final int EMPTY = -88888888;  // a constant regarding to empty val
    private static final int EXPIRY_TIME = 2;    // a constant to denote expiry time
    private static final int PERFORM_RESERVATION = 3;   // a constant


    /**
     * Creates a new scheduler that handles advanced reservations. This
     * scheduler uses First Come First Serve (FCFS) algorithm.
     * A default commit period time for a user to commit a reservation
     * is set to 30 minutes.
     * @param resourceName    the GridResource entity name that will contain
     *                        this allocation policy
     * @param entityName      this object name. The name of this entity will
     *                        be "resourceName_entityName".
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
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre resourceName != null
     * @pre entityName != null
     * @post $none
     */
    public ARSimpleSpaceShared(String resourceName, String entityName)
                               throws Exception
    {
        super(resourceName, entityName);
        commitPeriod_ = 30*60;  // set the expiry time into 30 mins
        init();
    }

    /**
     * Creates a new scheduler that handles advanced reservations. This
     * scheduler uses First Come First Serve (FCFS) algorithm.
     * @param resourceName    the GridResource entity name that will contain
     *                        this allocation policy
     * @param entityName      this object name. The name of this entity will
     *                        be "resourceName_entityName".
     * @param commitPeriod    a default commit period time for a user to commit
     *        a reservation (unit is in second). NOTE: once it is
     *        set, you can not change the time again.
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
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre resourceName != null
     * @pre entityName != null
     * @post $none
     */
    public ARSimpleSpaceShared(String resourceName, String entityName,
                               int commitPeriod) throws Exception
    {
        super(resourceName, entityName);
        if (commitPeriod <= 0)
        {
            throw new Exception(resourceName + "." + entityName + ": Error -" +
                                " Invalid expiry time.");
        }

        commitPeriod_ = commitPeriod;
        init();
    }

    /**
     * Handles a modify reservation request (NOTE: <b>NOT YET SUPPORTED</b>).
     * @param obj  a reservation object
     * @param senderID   a sender or user ID
     * @param sendTag    a tag to send to the user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public void handleModifyReservation(ARObject obj,int senderID,int sendTag)
    {
        System.out.println(super.get_name() +
               ".handleModifyReservation(): not supported at the moment.");

        super.replyModifyReservation(senderID, sendTag,
                   GridSimTags.AR_MODIFY_FAIL_RESOURCE_CANT_SUPPORT);
    }

    /**
     * Handles an immediate reservation request.<br>
     * NOTE:
     * <ul>
     *      <li>currently able to handle a case where start time = 0 and
     *          duration or end time > 0.
     *      <li>this scheduler <b>is not able to handle</b> a case where
     *          start time = 0 and duration or end time = 0.
     * </ul>
     * @param obj          a reservation object
     * @param senderID     a sender or user ID
     * @param sendTag      a tag to send the result back to user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public void handleImmediateReservation(ARObject obj, int senderID,
                                           int sendTag)
    {
        // check whether the requested PE is sufficient to handle or not
        if (obj.getNumPE() > super.totalPE_)
        {
            super.replyCreateReservation(senderID, sendTag, NOT_FOUND,
                             GridSimTags.AR_CREATE_FAIL_RESOURCE_NOT_ENOUGH_PE);

            return;
        }

        // at the moment, this scheduler doesn't support immediate reservations
        // with duration or end time = 0.
        if (obj.getDurationTime() <= 0)
        {
            System.out.println(super.get_name() +
                   ".handleImmediateReservation(): Error - can't handle " +
                   "duration or end time = 0.");

            super.replyCreateReservation(senderID, sendTag, NOT_FOUND,
                             GridSimTags.AR_CREATE_ERROR_INVALID_END_TIME);

            return;
        }

        // for immediate reservations, start time is the current time
        long startTime = super.getCurrentTime();
        obj.setStartTime(startTime);

        // determine when it will be expired. For immediate reservation,
        // expiry time is the finish or end time
        long expTime = startTime + (obj.getDurationTime() * super.MILLI_SEC);

        // check the start time against the availability
        // case 1: if the list is empty
        if (reservList_.size() == 0) {
            acceptReservation(obj, 0, senderID, sendTag, expTime, false);
        }
        else
        {
            // case 2: if list is not empty, find the available slot
            int pos = findEmptySlot(startTime, expTime, obj.getNumPE());

            // able to find empty slot(s)
            if (pos > NOT_FOUND) {
                acceptReservation(obj, pos, senderID, sendTag, expTime, false);
            }

            // not able to find any slots. Here pos contains a busy time tag.
            else {
                super.replyCreateReservation(senderID, sendTag, NOT_FOUND, pos);
            }
        }
    }

    /**
     * Handles an advanced reservation request.
     * @param obj          a reservation object
     * @param senderID     a sender or user ID
     * @param sendTag      a tag to send the result back to user
     * @pre obj != null
     * @pre senderID > 0
     * @post $none
     */
    public void handleCreateReservation(ARObject obj,int senderID,int sendTag)
    {
        // check whether the requested PE is sufficient to handle or not
        if ( obj.getNumPE() > super.totalPE_ )
        {
            super.replyCreateReservation(senderID, sendTag, NOT_FOUND,
                             GridSimTags.AR_CREATE_FAIL_RESOURCE_NOT_ENOUGH_PE);
            return;
        }

        // convert the start time from user's time into local time
        long startTime = AdvanceReservation.convertTimeZone(obj.getStartTime(),
                            obj.getTimeZone(), resource_.getResourceTimeZone());

        // get the current time
        long currentTime = super.getCurrentTime();

        // check whether the start time has passed or not
        if (startTime < currentTime)
        {
            super.replyCreateReservation(senderID, sendTag, NOT_FOUND,
                             GridSimTags.AR_CREATE_ERROR_INVALID_START_TIME);
            return;
        }

        // then overrides the object start time into local time
        obj.setStartTime(startTime);

        // determine when it will be expired
        long expTime = currentTime + (commitPeriod_ * super.MILLI_SEC);

        // check the start time against the availability
        // case 1: if the list is empty
        if (reservList_.size() == 0) {
            acceptReservation(obj, 0, senderID, sendTag, expTime, true);
        }
        else
        {
            // case 2: if list is not empty, find the available slot
            // remember that duration time is in second. Then need to convert
            // into milli seconds
            long endTime = startTime + (obj.getDurationTime()*super.MILLI_SEC);
            int pos = findEmptySlot(startTime, endTime, obj.getNumPE());

            // able to find empty slot(s)
            if (pos > NOT_FOUND) {
                acceptReservation(obj, pos, senderID, sendTag, expTime, true);
            }

            // not able to find any slots. Here pos contains a busy time tag.
            else {
                super.replyCreateReservation(senderID, sendTag, NOT_FOUND, pos);
            }
        }
    }

    /**
     * Handles a cancel reservation request for a given Gridlet ID list.
     * @param reservationID  a reservation ID
     * @param senderID       a sender ID
     * @param list           a list of Gridlet IDs
     * @param sendTag        a tag to send the result back to user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre list != null
     * @post $none
     */
    public void handleCancelReservation(int reservationID, int senderID,
                                        ArrayList list, int sendTag)
    {
        // case 1: check whether a reservation exists or not
        int result = 0;
        int index = super.searchReservation(reservList_, reservationID);
        if (index == NOT_FOUND)
        {
            // check on expiry list
            index = super.searchReservation(expiryList_, reservationID);
            if (index == NOT_FOUND) {
                result = GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
            }
            else {
                result = GridSimTags.AR_CANCEL_SUCCESS;
            }

            super.replyCancelReservation(senderID, sendTag, result);
            return;
        }

        // get AR object
        ARObject ar = (ARObject) reservList_.get(index);

        // if in the record contains no Gridlets committed, then exit
        if (ar.getTotalGridlet() == 0)
        {
            result = GridSimTags.AR_CANCEL_FAIL;
            super.replyCancelReservation(senderID, sendTag, result);
            return;
        }

        try
        {
            // a loop that cancels each Gridlet in the list
            Integer obj = null;
            Iterator it = list.iterator();
            while ( it.hasNext() )
            {
                obj = (Integer) it.next();
                result = cancelReservation(obj.intValue(), senderID);
            }
        }
        catch (Exception e) {
            result = GridSimTags.AR_CANCEL_ERROR;
        }

        super.replyCancelReservation(senderID, sendTag, result);
    }

    /**
     * Handles a cancel reservation request. All Gridlets will be cancelled.
     * @param reservationID  a reservation ID
     * @param senderID       a sender ID
     * @param sendTag        a tag to send the result back to user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleCancelReservation(int reservationID, int senderID,
                                        int sendTag)
    {
        // case 1: check whether a reservation exists or not
        int result = 0;
        int index = super.searchReservation(reservList_, reservationID);
        if (index == NOT_FOUND)
        {
            // check on expiry list
            index = super.searchReservation(expiryList_, reservationID);
            if (index == NOT_FOUND) {
                result = GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
            }
            else {
                result = GridSimTags.AR_CANCEL_SUCCESS;
            }

            super.replyCancelReservation(senderID, sendTag, result);
            return;
        }

        // gets the reservation object
        ARObject obj = (ARObject) reservList_.get(index);

        // case 2: if a reservation hasn't been committed
        int totalGridlet = 0;
        if (!obj.hasCommitted()) {
            totalGridlet = 0;
        }
        else {   // if a reservation has been committed
            totalGridlet = obj.getTotalGridlet();
        }

        // case 3: if a reservation has been committed and Gridlets are
        // submitted then need to cancel all the Gridlets
        for (int i = 0; i < totalGridlet; i++)
        {
            ResGridlet rgl = cancelReservationGridlet(reservationID);

            // if for some reason, can't find the gridlet
            if (rgl == null)
            {
                result = GridSimTags.AR_CANCEL_FAIL;
                super.replyCancelReservation(senderID, sendTag, result);
                return;
            }

            rgl.finalizeGridlet();
            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL,rgl.getGridlet(),
                                    rgl.getGridletID(), senderID);
        }

        // change the status and remove the reservation
        obj.setStatus(GridSimTags.AR_STATUS_CANCELED);
        reservList_.remove(obj);
        expiryList_.add(obj);

        // send back the result of this operation
        result = GridSimTags.AR_CANCEL_SUCCESS;
        super.replyCancelReservation(senderID, sendTag, result);
    }

    /**
     * Handles a cancel reservation request. This method
     * cancels only for a given Gridlet ID of a reservation.
     * @param reservationID  a reservation ID
     * @param senderID       a sender ID
     * @param gridletID      a Gridlet ID
     * @param sendTag        a tag to send the result back to user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleCancelReservation(int reservationID, int senderID,
                                        int gridletID, int sendTag)
    {
        // case 1: check whether a reservation exists or not
        int result = 0;
        int index = super.searchReservation(reservList_, reservationID);
        if (index == NOT_FOUND)
        {
            // check on expiry list
            index = super.searchReservation(expiryList_, reservationID);
            if (index == NOT_FOUND) {
                result = GridSimTags.AR_CANCEL_FAIL_INVALID_BOOKING_ID;
            }
            else {
                result = GridSimTags.AR_CANCEL_SUCCESS;
            }

            super.replyCancelReservation(senderID, sendTag, result);
            return;
        }

        // cancel a particular Gridlet
        result = cancelReservation(gridletID, senderID);

        // send the result back
        super.replyCancelReservation(senderID, sendTag, result);
    }

    /**
     * Cancels a particular Gridlet of a reservation
     * @param gridletID  a gridlet ID
     * @param userID     a Gridlet user ID
     * @return a cancel reservation tag
     * @pre gridletID > 0
     * @pre userID > 0
     * @post $none
     */
    private int cancelReservation(int gridletID, int userID)
    {
        int result = 0;

        // if exist, then cancel a gridlet
        ResGridlet rgl = cancel(gridletID, userID);

        // if the Gridlet is not found
        if (rgl == null)
        {
            result = GridSimTags.AR_CANCEL_FAIL;
            return result;
        }

        // if the Gridlet has finished beforehand then prints an error msg
        if (rgl.getGridletStatus() == Gridlet.SUCCESS) {
            result = GridSimTags.AR_CANCEL_FAIL_GRIDLET_FINISHED;
        }
        else {
            result = GridSimTags.AR_CANCEL_SUCCESS;
        }

        // sends the Gridlet back to sender
        rgl.finalizeGridlet();
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, rgl.getGridlet(),
                                gridletID, userID);
        return result;
    }

    /**
     * Cancels a particular Gridlet of a reservation
     * @param reservationID   a reservation ID
     * @return a ResGridlet object
     * @pre reservationID > 0
     * @post $none
     */
    private ResGridlet cancelReservationGridlet(int reservationID)
    {
        ResGridlet rgl = null;

        // Find in EXEC List first
        int found = findGridlet(gridletInExecList_, reservationID);
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
        found = findGridlet(gridletQueueList_, reservationID);
        if (found >= 0)
        {
            rgl = (ResGridlet) gridletQueueList_.remove(found);
            rgl.setGridletStatus(Gridlet.CANCELED);
            return rgl;
        }

        // if not found, then find in the Paused list
        found = findGridlet(gridletPausedList_, reservationID);
        if (found >= 0)
        {
            rgl = (ResGridlet) gridletPausedList_.remove(found);
            rgl.setGridletStatus(Gridlet.CANCELED);
            return rgl;
        }

        // if not found, then find in AR waiting list
        found = findGridlet(gridletWaitingList_, reservationID);
        if (found >= 0)
        {
            rgl = (ResGridlet) gridletWaitingList_.remove(found);
            rgl.setGridletStatus(Gridlet.CANCELED);
            return rgl;
        }

        // if not found
        rgl = null;
        return rgl;
    }

    /**
     * Search for a particular reservation in a data structure
     * @param list   a data structure
     * @param reservationID  a reservation ID
     * @return location in the data structure or <tt>-1</tt> if not found
     * @pre list != null
     * @post $none
     */
    private int findGridlet(LinkedList list, int reservationID)
    {
        ResGridlet rgl = null;
        int found = -1;     // means the Gridlet is not in the list

        try
        {
            // Search through the list to find the given Gridlet object
            int i = 0;
            Iterator iter = list.iterator();
            while ( iter.hasNext() )
            {
                rgl = (ResGridlet) iter.next();
                if (rgl.getReservationID() == reservationID)
                {
                    found = i;
                    break;
                }

                i++;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                               ".findGridlet(): Exception error occurs.");
            System.out.println( e.getMessage() );
        }

        return found;
    }

    /**
     * Handles a query busy time request (NOTE: <b>NOT YET SUPPORTED</b>).
     * @param from    starting period time
     * @param to      ending period time
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre from > 0
     * @pre to > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleQueryBusyTime(long from, long to, int senderID,
                                 int sendTag, double userTimeZone)
    {
        System.out.println(super.get_name() +
             ".handleQueryBusyTime(): not supported at the moment.");

        super.replyTimeReservation(senderID, sendTag, null, userTimeZone);
    }

    /**
     * Handles a query free time request (NOTE: <b>NOT YET SUPPORTED</b>).
     * @param from    starting period time
     * @param to      ending period time
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre from > 0
     * @pre to > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleQueryFreeTime(long from, long to, int senderID,
                                 int sendTag, double userTimeZone)
    {
        System.out.println(super.get_name() +
              ".handleQueryFreeTime(): not supported at the moment.");

        super.replyTimeReservation(senderID, sendTag, null, userTimeZone);
    }

    /**
     * Handles a commit reservation request.
     * This method commits a reservation only. Gridlets are submitted using
     * {@link #handleCommitReservation(int, int, int, Gridlet)} or
     * {@link #handleCommitReservation(int, int, int, GridletList)} method.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleCommitOnly(int reservationID, int senderID, int sendTag)
    {
        int result = doCommitReservation(reservationID, null);
        super.replyCommitReservation(senderID, sendTag, result);
    }

    /**
     * Handles a commit reservation request.
     * This method commits a reservation and submits a Gridlet to be processed
     * as well.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @param list           a list of Gridlet object
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre list != null
     * @post $none
     */
    public void handleCommitReservation(int reservationID, int senderID,
                                        int sendTag, GridletList list)
    {
        int result = doCommitReservation(reservationID, list);
        super.replyCommitReservation(senderID, sendTag, result);
    }

    /**
     * Handles a commit reservation request.
     * This method commits a reservation and submits a list of  Gridlets
     * to be processed as well.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @param gridlet        a Gridlet object
     * @pre reservationID > 0
     * @pre senderID > 0
     * @pre gridlet != null
     * @post $none
     */
    public void handleCommitReservation(int reservationID, int senderID,
                                        int sendTag, Gridlet gridlet)
    {
        int result = doCommitReservation(reservationID, gridlet);
        super.replyCommitReservation(senderID, sendTag, result);
    }

    /**
     * Internal method that handles different type of commit reservation
     * @param reservationID   a reservation ID
     * @param obj             a generic object, might contain Gridlet or
     *                        GridletList object.
     * @return a commit tag
     * @pre reservationID > 0
     * @post $none
     */
    private int doCommitReservation(int reservationID, Object obj)
    {
        // search the reservation first
        int index = super.searchReservation(reservList_, reservationID);
        if (index == NOT_FOUND) {
            return GridSimTags.AR_COMMIT_FAIL_INVALID_BOOKING_ID;
        }

        // need to check the expiry time whether it has passed or not
        ARObject ar = (ARObject) reservList_.get(index);
        long currentTime = super.getCurrentTime();   // get curret time

        // if a reservation is already expired
        if (!ar.hasCommitted() && ar.getExpiryTime() < currentTime)
        {
            // change the status of this reservation into expired
            ar.setStatus(GridSimTags.AR_STATUS_EXPIRED);

            // move the reservation from reservation list into expired list
            expiryList_.add(ar);
            reservList_.remove(ar);
            return GridSimTags.AR_COMMIT_FAIL_EXPIRED;
        }

        // if committing after a reservation slot has finished
        long endTime = ar.getStartTime()+(ar.getDurationTime()*super.MILLI_SEC);
        if (endTime < currentTime)
        {
            // change the status of this reservation into expired
            ar.setStatus(GridSimTags.AR_STATUS_EXPIRED);

            // move the reservation from reservation list into expired list
            expiryList_.add(ar);
            reservList_.remove(ar);
            return GridSimTags.AR_COMMIT_FAIL_EXPIRED;
        }

        int result = 0;  // storing a result of this operation

        // if a user wants to commit only without any Gridlet(s) submitted
        if (obj == null) {
            result = GridSimTags.AR_COMMIT_SUCCESS;
        }
        else   // if a user commits one or more Gridlet objects
        {
            Gridlet gl = null;
            try
            {
                // a case where a user commits with only 1 Gridlet
                gl = (Gridlet) obj;

                // only able to handle 1 PE = 1 Gridlet
                if (ar.getTotalGridlet() == ar.getNumPE()) {
                    return GridSimTags.AR_COMMIT_FAIL;
                }

                ResGridlet rgl = new ResGridlet(gl, ar.getStartTime(),
                                 ar.getDurationTime(), ar.getReservationID());

                // set the result into success
                result = GridSimTags.AR_COMMIT_SUCCESS;

                // store the Gridlet
                index = findPosition(gridletWaitingList_, ar.getStartTime());

                // put into waiting list
                gridletWaitingList_.add(index, rgl);
                ar.addTotalGridlet(1);   // add total Gridlet executed by 1
            }
            catch (ClassCastException c)
            {
                // a case where a user commits with more than 1 Gridlets
                try
                {
                    // creates a new ResGridlet object for each Gridlet object
                    // in the list
                    GridletList list = (GridletList) obj;

                    // only able to handle 1 PE = 1 Gridlet, so if committing
                    // more Gridlets, then need some adjustment.
                    int size = list.size();
                    if (ar.getTotalGridlet() + size > ar.getNumPE()) {
                        size = ar.getNumPE() - ar.getTotalGridlet();
                    }

                    for (int i = 0; i < size; i++)
                    {
                        gl = (Gridlet) list.get(i);
                        ResGridlet rgl = new ResGridlet(gl, ar.getStartTime(),
                            ar.getDurationTime(), ar.getReservationID());

                        // find the exact location to store these Gridlets
                        if (i == 0)
                        {
                            index = findPosition(gridletWaitingList_,
                                                 ar.getStartTime());
                        }

                        // store each gridlet into a waiting list
                        gridletWaitingList_.add(index, rgl);
                    }

                    // set the result into success
                    result = GridSimTags.AR_COMMIT_SUCCESS;

                    // add total Gridlet executed by this reservation
                    ar.addTotalGridlet(size);
                }
                catch (Exception again) {
                    result = GridSimTags.AR_COMMIT_ERROR;
                }
            }
            catch (Exception e) {
                result = GridSimTags.AR_COMMIT_ERROR;
            }
        }

        // if commit is successful, then set the internal event to process
        // a reservation in later time
        if (result == GridSimTags.AR_COMMIT_SUCCESS)
        {
            int status = GridSimTags.AR_STATUS_NOT_STARTED;
            int start = (int) ((ar.getStartTime()-currentTime)/super.MILLI_SEC);

            // start can be -ve for immediate reservation
            if (start <= 0)
            {
                status = GridSimTags.AR_STATUS_ACTIVE;
                start = 0;
            }

            ar.setCommitted();
            ar.setStatus(status);  // set AR status
            super.sendInternalEvent(start, PERFORM_RESERVATION);
        }

        return result;
    }

    /**
     * Finds the location in the list based on a given start time.
     * This is useful for doing an insertion sort based on a reservation's
     * start time.
     * @param list         a list containing reservation objects
     * @param startTime    a reservation start time
     * @return position number in the list
     * @pre list != null
     * @pre startTime > 0
     * @post $none
     */
    private int findPosition(LinkedList list, long startTime)
    {
        int index = 0;

        // if the waiting list is empty, then add to it straightaway
        if (list.size() == 0) {
            return index;
        }

        Iterator it = list.iterator();
        ResGridlet rgl = null;

        // iterates a loop to find the location based on start time
        while ( it.hasNext() )
        {
            rgl = (ResGridlet) it.next();

            // exit the loop if object's reservation start time is greater
            if (rgl.getStartTime() > startTime) {
                break;
            }

            index++;
        }

        // if append at the end of the list
        if ( index > list.size() ) {
            index = list.size();
        }

        return index;
    }

    /**
     * Finds Gridlets that are ready for execution.
     * @pre $none
     * @post $none
     */
    private void performReservation()
    {
        // update the current Gridlets in exec list up to this point in time
        updateGridletProcessing();

        // if no Gridlets in the waiting list, then exit
        if (gridletWaitingList_.size() == 0) {
            return;
        }

        // get current time. NOTE: add 5 seconds for buffer
        long currentTime = super.getCurrentTime() + (5 * super.MILLI_SEC);

        // trying to execute Gridlets from 0 up to index-1.
        // NOTE: "up to" means including
        int index = findPosition(gridletWaitingList_, currentTime);

        // index is 0 means no Gridlets executing
        if (index <= 0) {
            return;
        }

        executeReservedGridlet(index);
    }

    /**
     * Schedules Gridlets to empty PEs.
     * @param index    a position number in the reservation list. This
     *                 variable also refers to number of PEs required.
     * @pre index > 0
     * @post $none
     */
    private void executeReservedGridlet(int index)
    {
        boolean success = false;

        // if there are empty PEs, then assign straight away
        if (gridletInExecList_.size() + index <= super.totalPE_) {
            success = true;
        }

        // if no available PE then put unreserved Gridlets into queue list
        if (!success) {
            clearExecList(index);
        }

        // a loop to execute Gridlets and remove them from waiting list
        ResGridlet rgl = null;
        int i = 0;
        int totalAllocate = 0;

        try
        {
            while (totalAllocate < index)
            {
                rgl = (ResGridlet) gridletWaitingList_.get(i);
                success = allocatePEtoGridlet(rgl);

                // only if a Gridlet has been successfully allocated, then
                // remove it from the waiting list
                if (success)
                {
                    gridletWaitingList_.remove(i);
                    totalAllocate++;
                    continue; // don't increment i
                }
                i++;
            }
        }
        catch (Exception e) {
            // .... do nothing
        }
    }

    /**
     * Removes Gridlets in the execution list to make room for new ones.
     * NOTE: a reservation might contain many Gridlets. Hence, only few
     * Gridlets executing now. The rest are later.
     * @param totalPE   total number of free PEs needed.
     * @return total number of free PEs available.
     * @pre totalPE > 0
     * @post $none
     */
    private int clearExecList(int totalPE)
    {
        int removeSoFar = 0;   // number of PEs removed so far
        ResGridlet obj = null;

        // first, remove unreserved Gridlets
        int i = 0;
        while ( i < gridletInExecList_.size() )
        {
            obj = (ResGridlet) gridletInExecList_.get(i);
            if (removeSoFar <= totalPE && !obj.hasReserved())
            {
                obj.setGridletStatus(Gridlet.QUEUED);
                gridletInExecList_.remove(i);
                gridletQueueList_.add(obj);
                super.resource_.setStatusPE(PE.FREE, obj.getMachineID(),
                                            obj.getPEID());
                removeSoFar++;
                continue;
            }
            i++;
        }

        // if enough, then exit
        if (removeSoFar == totalPE) {
            return removeSoFar;
        }

        // if still not enough, then remove Gridlets that are running longer
        long currentTime = super.getCurrentTime();
        long finishTime = 0;

        // searching through the loop again
        i = 0;
        while ( i < gridletInExecList_.size() )
        {
            // exit the loop if there are enough space already
            if (removeSoFar > totalPE) {
                break;
            }

            obj = (ResGridlet) gridletInExecList_.get(i);

            // look for AR Gridlets that are running longer than estimated
            if (removeSoFar <= totalPE && obj.hasReserved())
            {
                finishTime = obj.getStartTime()+(obj.getDurationTime()*MILLI_SEC);
                if (finishTime <= currentTime)
                {
                    obj.setGridletStatus(Gridlet.QUEUED);
                    gridletInExecList_.remove(i);
                    gridletQueueList_.add(obj);
                    super.resource_.setStatusPE(PE.FREE, obj.getMachineID(),
                                                obj.getPEID());
                    removeSoFar++;
                    continue;
                }
            }

            i++;

        } // end for

        return removeSoFar;
    }

    /**
     * Handles a query reservation request.
     * @param reservationID  a reservation ID
     * @param senderID       a sender or user ID
     * @param sendTag        a tag to send to the user
     * @pre reservationID > 0
     * @pre senderID > 0
     * @post $none
     */
    public void handleQueryReservation(int reservationID, int senderID,
                                       int sendTag)
    {
        ARObject obj = null;
        int result = 0;

        // firstly, look in the reservation list
        int index = super.searchReservation(reservList_, reservationID);
        if (index == NOT_FOUND)
        {
            // if not found, then look in the expiry list
            index = super.searchReservation(expiryList_, reservationID);
            if (index == NOT_FOUND) {
                result = GridSimTags.AR_STATUS_RESERVATION_DOESNT_EXIST;
            }
            else       // if found then get the object status
            {
                obj = (ARObject) expiryList_.get(index);
                result = obj.getStatus();
            }
        }
        else   // if found then get the object status
        {
            obj = (ARObject) reservList_.get(index);
            result = obj.getStatus();
        }

        // sends the result back to user
        super.replyQueryReservation(senderID, sendTag, result);
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
                gridletInExecList_.clear();
                gridletQueueList_.clear();
                gridletWaitingList_.clear();
                reservList_.clear();
                break;
            }

            // checks the expiry time for all reservations
            if (ev.get_src() == super.myId_ && ev.get_tag() == EXPIRY_TIME)
            {
                checkExpiryTime();
                continue;
            }

            // time to execute reservations' Gridlets.
            if (ev.get_src() == super.myId_ &&
                ev.get_tag() == PERFORM_RESERVATION)
            {
                performReservation();
                continue;
            }

            // Internal Event if the event source is this entity
            if (ev.get_src() == super.myId_ && gridletInExecList_.size() > 0)
            {
                updateGridletProcessing();   // update Gridlets
                checkGridletCompletion();    // check for finished Gridlets

                // periodically need to check reservations
                performReservation();
            }
        }

        // CHECK for ANY INTERNAL EVENTS WAITING TO BE PROCESSED
        while (super.sim_waiting() > 0)
        {
            // wait for event and ignore since it is likely to be related to
            // internal event scheduled to update Gridlets processing
            super.sim_get_next(ev);
            System.out.println(super.get_name() +
                               ".body(): ignore internal events");
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
    public void gridletSubmit(Gridlet gl, boolean ack)
    {
        // update the current Gridlets in exec list up to this point in time
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
    public int gridletStatus(int gridletId, int userId)
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

        // Find in the AR Waiting List
        found = gridletWaitingList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            // Get the Gridlet from the execution list
            rgl = (ResGridlet) gridletWaitingList_.get(found);
            return rgl.getGridletStatus();
        }

        // if not found in all lists then no found
        return -1;
    }

    /**
     * Cancels a Gridlet running in this entity.
     * The User ID is
     * important as many users might have the same Gridlet ID in the lists.<br>
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
    public void gridletCancel(int gridletId, int userId)
    {
        // cancels a Gridlet
        ResGridlet rgl = cancel(gridletId, userId);

        // if the Gridlet is not found
        if (rgl == null)
        {
            System.out.println(super.get_name() + ".gridletCancel(): Cannot " +
                    "find Gridlet #" + gridletId + " for User #" + userId);

            super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, null,
                                    gridletId, userId);
            return;
        }

        // if the Gridlet has finished beforehand then prints an error msg
        if (rgl.getGridletStatus() == Gridlet.SUCCESS)
        {
            System.out.println(super.get_name() + ".gridletCancel(): Cannot "
                    + "cancel Gridlet #" + gridletId + " for User #" + userId
                    + " since it has FINISHED.");
        }

        // sends the Gridlet back to sender
        rgl.finalizeGridlet();
        super.sendCancelGridlet(GridSimTags.GRIDLET_CANCEL, rgl.getGridlet(),
                                gridletId, userId);
    }

    /**
     * Pauses a Gridlet only if it is currently executing.
     * This method will search in the execution list, as well as in the queue
     * list. The User ID is
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
    public void gridletPause(int gridletId, int userId, boolean ack)
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

            // if a Gridlet is finished upon cancelling, then set it to success
            // instead.
            if (rgl.getRemainingGridletLength() == 0.0)
            {
                found = -1;  // meaning not found in Queue List
                gridletFinish(rgl, Gridlet.SUCCESS);
                System.out.println(super.get_name()
                        + ".gridletPause(): Cannot pause"
                        + " Gridlet #" + gridletId + " for User #" + userId
                        + " since it has FINISHED.");
            }
            else
            {
                status = true;
                rgl.setGridletStatus(Gridlet.PAUSED);  // change the status
                gridletPausedList_.add(rgl);   // add into the paused list

                // Set the PE on which Gridlet finished to FREE
                super.resource_.setStatusPE( PE.FREE, rgl.getMachineID(),
                                             rgl.getPEID() );

                // empty slot is available, hence process a new Gridlet
                allocateQueueGridlet();
            }
        }
        else {      // Find in QUEUE list
            found = gridletQueueList_.indexOf(gridletId, userId);
        }

        // if found in the Queue List
        if (status == false && found >= 0)
        {
            status = true;

            // removes the Gridlet from the Queue list
            ResGridlet rgl = (ResGridlet) gridletQueueList_.remove(found);
            rgl.setGridletStatus(Gridlet.PAUSED);   // change the status
            gridletPausedList_.add(rgl);            // add into the paused list
        }
        else {     // Find in the AR Waiting List
            found = gridletWaitingList_.indexOf(gridletId, userId);
        }

        // if found in the AR waiting list
        if (status == false && found >= 0)
        {
            status = true;

            // removes the Gridlet from the waiting list
            ResGridlet rgl = (ResGridlet) gridletWaitingList_.remove(found);
            rgl.setGridletStatus(Gridlet.PAUSED);   // change the status
            gridletPausedList_.add(rgl);            // add into the paused list
        }

        // if not found anywhere in both exec and paused lists
        else if (found == -1)
        {
            System.out.println(super.get_name() +
                    ".gridletPause(): Error - cannot " +
                    "find Gridlet #" + gridletId + " for User #" + userId);
        }

        // sends back an ack if required
        if (ack == true)
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
    public void gridletMove(int gridletId, int userId, int destId, boolean ack)
    {
        // cancels the Gridlet
        ResGridlet rgl = cancel(gridletId, userId);

        // if the Gridlet is not found
        if (rgl == null)
        {
            System.out.println(super.get_name() + ".gridletMove(): Cannot " +
                       "find Gridlet #" + gridletId + " for User #" + userId);

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
            System.out.println(super.get_name() + ".gridletMove(): Cannot move"
                    + " Gridlet #" + gridletId + " for User #" + userId
                    + " since it has FINISHED.");

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
     * @param   ack   an acknowledgement, i.e. <tt>true</tt> if wanted to know
     *        whether this operation is success or not, <tt>false</tt>
     *        otherwise (don't care)
     * @pre gridletId > 0
     * @pre userId > 0
     * @post $none
     */
    public void gridletResume(int gridletId, int userId, boolean ack)
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
            boolean success = false;
            if ( gridletInExecList_.size() < super.totalPE_ ) {
                success = allocatePEtoGridlet(rgl);
            }

            // otherwise put into Queue list
            if (!success)
            {
                rgl.setGridletStatus(Gridlet.QUEUED);
                gridletQueueList_.add(rgl);
            }

            System.out.println(super.get_name() + ".gridletResume():" +
                    " Gridlet #" + gridletId + " with User ID #" +
                    userId + " has been sucessfully RESUMED.");
        }
        else
        {
            System.out.println(super.get_name() + ".gridletResume(): Cannot " +
                    "find Gridlet #" + gridletId + " for User #" + userId);
        }

        // sends back an ack if required
        if (ack)
        {
            super.sendAck(GridSimTags.GRIDLET_RESUME_ACK, status,
                          gridletId, userId);
        }
    }

    /**
     * Initialises all private attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        reservID_ = 1;
        reservList_ = new ArrayList();
        expiryList_ = new ArrayList();

        // initialises local data structure
        this.gridletInExecList_ = new ResGridletList();
        this.gridletPausedList_ = new ResGridletList();
        this.gridletQueueList_  = new ResGridletList();
        this.gridletWaitingList_ = new ResGridletList();
        this.lastUpdateTime_ = 0.0;
        this.machineRating_ = null;
    }

    /**
     * Checks for expiry time of all reservation in the list. Also checks
     * whether a reservation has been completed or not.
     * @pre $none
     * @post $none
     */
    private void checkExpiryTime()
    {
        long currentTime = super.getCurrentTime();   // get current time
        ARObject obj = null;

        try
        {
            long endTime = 0;

            // a loop that checks a reservation has been committed before the
            // expiry time or not
            for (int i = 0; i < reservList_.size(); i++)
            {
                if (reservList_.size() == 0) {
                    break;
                }

                obj = (ARObject) reservList_.get(i);

                // check for expiry time
                if (obj.hasCommitted() == false &&
                    obj.getExpiryTime() <= currentTime)
                {
                    obj.setStatus(GridSimTags.AR_STATUS_EXPIRED);
                    expiryList_.add(obj);
                    reservList_.remove(obj);
                    continue;
                }

                // if no Gridlets executed or time has already gone
                // then remove it from the list
                endTime = obj.getStartTime()+(obj.getDurationTime()*MILLI_SEC);
                if (obj.getTotalGridlet() == 0 && endTime <= currentTime)
                {
                    obj.setStatus(GridSimTags.AR_STATUS_COMPLETED);
                    expiryList_.add(obj);
                    reservList_.remove(obj);
                    continue;
                }
            }
        }
        catch (Exception e) {
            // ....
        }
    }

    /**
     * Accepts a given reservation and sends a result back to user
     * @param obj   a reservation object to be stored
     * @param pos   an index or position number in the reservation list
     * @param senderID    a user or sender ID
     * @param sendTag     a tag to send to the user
     * @param expTime     reservation expiry time
     * @param ar          true if it is AR, false if it is immediate reservation
     * @pre obj != null
     * @pre senderID > 0
     * @pre expTime > 0
     * @post $none
     */
    private void acceptReservation(ARObject obj, int pos, int senderID,
                                   int sendTag, long expTime, boolean ar)
    {
        // Create a copy of this object.
        ARObject arObj = new ARObject(obj);

        // if expiry time is more or greater than start time, then need to
        // set it into start time instead.
        if (ar == true && expTime > obj.getStartTime()) {
            expTime = obj.getStartTime();
        }

        arObj.setReservation(reservID_, expTime);   // set the id + expiry time
        reservList_.add(pos, arObj);   // add the object into the list

        // convert the expiry time from local time into user's time
        expTime = AdvanceReservation.convertTimeZone(expTime,
                         resource_.getResourceTimeZone(), obj.getTimeZone() );

        // send back to sender
        super.replyCreateReservation(senderID, sendTag, expTime, reservID_);

        // then send this into itself to check for expiry time
        int start = (int) (expTime - super.getCurrentTime()) / super.MILLI_SEC;
        super.sendInternalEvent(start, EXPIRY_TIME);
        reservID_++;      // increment reservation ID
    }

    /**
     * Finds an empty slot for a particular reservation.
     * @param startTime   reservation start time
     * @param endTime     reservation end time
     * @param numPE       number of PEs required by this reservation
     * @return the position number in the reservation list or a busy time tag.
     * @pre startTime > 0
     * @pre endTime > 0
     * @pre numPE > 0
     * @post $none
     */
    private int findEmptySlot(long startTime, long endTime, int numPE)
    {
        long finishTime = 0;
        int pos = NOT_FOUND;     // the exact position for a new obj to be put
        int begin = NOT_FOUND;   // start interval for checking PE availability
        int end = NOT_FOUND;     // end interval for checking PE availability

        // forward increments that looking for position or index for the new
        // reservation. Also, look for the start interval
        ARObject obj = null;
        Iterator iter = reservList_.iterator();
        int i = 0;
        while ( iter.hasNext() )
        {
            obj = (ARObject) iter.next();

            // calculates the finish time of each reservation in the list
            finishTime = obj.getStartTime()+(obj.getDurationTime()*MILLI_SEC);

            // find exact location to store a new obj
            // also find the start internal
            if ( startTime <= obj.getStartTime() )
            {
                // set the correct position or index in the array
                if (pos == NOT_FOUND) {
                    pos = i;
                }

                // if start time is before both begin and finish time
                if (begin == NOT_FOUND && startTime <= finishTime) {
                    begin = i;
                }
            }

            // if start time is in between begin and finish time
            else if (begin == NOT_FOUND && startTime <= finishTime) {
                begin = i;
            }

            // then find the end interval
            if (obj.getStartTime() <= endTime) {
                end = i;
            }
            else
            {
                end = i - 1;
                break;
            }

            i++;
        }

        // if only 1 job found during the time interval, then make sure the end
        // doesn't overlap with begin time
        if (end == NOT_FOUND || end < begin) {
            end = begin;
        }

        // if no position, then it will be appended at the end of the list
        if (pos == NOT_FOUND) {
            pos = reservList_.size();
        }

        // once a position in the list is determined, then check availability
        int result = isAvailable(begin, end, numPE, startTime, endTime);
        if (result < 0) {
            pos = result;
        }

        return pos;
    }

    /**
     * Finds how many PEs are used within the time interval as indicated by
     * begin and end position of a list
     * @param begin   a begin index or position in a reservation list
     * @param end     an end index or position in a reservation list
     * @param numPE   number of PEs required by this reservation
     * @param startTime   reservation start time
     * @param endTime     reservation end time
     * @return the position number in the reservation list or a busy time tag.
     * @pre $none
     * @post $none
     */
    private int isAvailable(int begin, int end, int numPE, long startTime,
                            long endTime)
    {
        // if the required slot is empty or not occupied
        if (begin == NOT_FOUND) {
            return SUCCESS;
        }

        ARObject obj = null;
        long time = 0;        // a temp time
        int result = 0;

        // if the required slot only has 1 occupants or empty
        if (begin == end)
        {
            obj = (ARObject) reservList_.get(begin);

            // if the slot is empty
            if ( endTime < obj.getStartTime() ) {
                result = SUCCESS;
            }

            // determines the total PE of existing reservation + new one
            else if (obj.getNumPE() + numPE <= totalPE_) {
                result = SUCCESS;
            }
            else
            {
                // if no enough PE then approximates the busy time
                time = obj.getStartTime() +
                       (obj.getDurationTime()*MILLI_SEC) - startTime;
                result = approxBusyTime(time);
            }
        }

        // if the required slot involves more than 1 occupants
        else {
            result = calculateEmptySlot(begin, end, numPE, startTime);
        }

        return result;
    }

    /**
     * Finds how many PEs are used within the time interval as indicated by
     * begin and end position of a list. This involves for more than 1 occupants
     * in a slot.
     * @param begin   a begin index or position in a reservation list
     * @param end     an end index or position in a reservation list
     * @param numPE   number of PEs required by this reservation
     * @param startTime   reservation start time
     * @return the position number in the reservation list or a busy time tag.
     * @pre $none
     * @post $none
     */
    private int calculateEmptySlot(int begin,int end,int numPE,long startTime)
    {
        int i = 0;
        int NEGATIVE = -1;

        // create a new array. Need to add 1 since it is start at 0
        int[] array = new int[ (end - begin + 1) * 2];
        int arrayIndex = 0;

        // clear up the temp array
        for (i = 0; i < array.length; i++) {
            array[i] = EMPTY;
        }

        // do insertion sort for the start and end time of each object
        // during the defined time interval. This basically make a histogram
        // to determine each block.
        ARObject obj = null;
        long time = 0;
        Iterator it = reservList_.iterator();
        i = 0;
        while ( it.hasNext() )
        {
            if (begin <= i && i <= end)
            {
                obj = (ARObject) it.next();
                time = obj.getStartTime() + (obj.getDurationTime()*MILLI_SEC);

                // skip all objects that have finished earlier than start time
                // since the interval might include these objects
                if (time < startTime) {
                    continue;
                }

                // for a first occupant, insert straightaway
                if (arrayIndex == 0)
                {
                    array[0] = i;
                    array[1] = i * NEGATIVE;
                    arrayIndex = 2;
                }
                // for other occupants, do insertion sort
                else {
                    arrayIndex = insertionSort(i, arrayIndex, array);
                }
            }

            // exit the loop
            if (i > end) {
                break;
            }

            i++;
        }

        // After making a histogram, then for each block calculates how many
        // PEs are used.
        boolean tail = false;
        int total = 0;
        i = 0;
        int end_index = NOT_FOUND;  // 1st occurrence of last index

        // a loop that looking for a pattern of start - end
        for (i = 0; i < arrayIndex; i++)
        {
            if (array[i] == 0 && tail == true)
            {
                end_index = i;
                break;
            }

            if (array[i] < 0)
            {
                end_index = i;
                break;
            }

            if (array[i] == 0 && tail == false) {
                tail = true;
            }
        }

        // after finding a pattern, then calculates total PEs used.
        for (i = 0; i < end_index; i++)
        {
            if (array[i] == (end_index * -1)) {
                continue;
            }

            obj = (ARObject) reservList_.get(i);
            total += obj.getNumPE();
        }

        // if there are empty PEs, then return 1 to denote a success otherwise
        // return a busy time tag.
        int result = 0;
        if (total + numPE <= totalPE_) {
            result = 1;
        }
        else {
            result = approxBusyTime(time - startTime);
        }

        return result;
    }

    /**
     * Performs insertion sort in a given array
     * @param pos   a number that denotes a position number in a reservation
     *              list
     * @param indexArray  a current index of the array
     * @param array       an array that stores pos variable
     * @return the latest current index of the array
     */
    private int insertionSort(int pos, int indexArray, int[] array)
    {
        ARObject obj = null;
        ARObject newObj = null;

        int begin = NOT_FOUND;
        int end = NOT_FOUND;
        long finishTime = 0;
        long time = 0;

        // NOTE: for insertion sort, it needs to do twice iterations for
        // start and finish time
        int i = 0;
        int index = 0;
        boolean tail = false;

        // first loop for start time of a reservation
        for (i = 0; i < indexArray; i++)
        {
            newObj = (ARObject) reservList_.get(pos);
            finishTime = newObj.getStartTime() +
                         (newObj.getDurationTime() * MILLI_SEC);

            if (array[i] == EMPTY) {
                break;
            }

            // end time
            if (array[i] < 0 || (array[i] == 0 && tail == true) )
            {
                index = array[i] * -1;
                obj = (ARObject) reservList_.get(index);
                time = obj.getStartTime()+(obj.getDurationTime()*MILLI_SEC);
            }

            // start time
            else {
                obj = (ARObject) reservList_.get( array[i] );
                time = obj.getStartTime();
            }

            // determines the exact begin position
            if (begin == NOT_FOUND && newObj.getStartTime() < time ) {
                begin = i;
            }

            // determines the exact end position
            if (finishTime < time)
            {
                end = i;
                break;
            }

            if (array[i] == 0 && tail == false) {
                tail = true;
            }
        }

        // if inserts in the beginning of the array
        if (begin == NOT_FOUND)
        {
            array[indexArray] = pos;
            array[indexArray+1] = pos * -1;

            return indexArray+2;
        }

        // inserts in the middle or the end of the array
        int inc = 2;
        for (i = indexArray - 1; i < indexArray; i--)
        {
            if (end == NOT_FOUND)
            {
                inc = 1;
                array[indexArray+1] = pos * -1;
            }

            array[i + inc] = array[i];
            if (i == end)
            {
                inc = 1;
                array[i + inc] = pos * -1;
            }

            if (i == begin)
            {
                array[i] = pos;
                break;
            }
        }

        return indexArray+2;
    }

    /**
     * Allocates the first Gridlet in the Queue list (if any) to execution list
     * @pre $none
     * @post $none
     */
    private void allocateQueueGridlet()
    {
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
            boolean success = allocatePEtoGridlet(obj);
            if (success == true) {
                gridletQueueList_.remove(obj);
            }
        }
    }

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
        Iterator iter = gridletInExecList_.iterator();
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
        Machine myMachine = resource_.getMachineWithFreePE( rgl.getNumPE() );

        // If a Machine is empty then ignore the rest
        if (myMachine == null) {
            return false;
        }

        // gets the list of PEs and find one empty PE
        PEList MyPEList = myMachine.getPEList();
        int free = MyPEList.getFreePEID();

        // Set allocated PE to BUSY status
        super.resource_.setStatusPE(PE.BUSY, myMachine.getMachineID(), free);

        // ALLOCATE IMMEDIATELY
        rgl.setGridletStatus(Gridlet.INEXEC);   // change Gridlet status
        rgl.setMachineAndPEID(myMachine.getMachineID(), free);

        // add this Gridlet into execution list
        gridletInExecList_.add(rgl);

        // Identify Completion Time and Set Interrupt
        int rating = machineRating_[ rgl.getMachineID() ];
        double time = forecastFinishTime( rating * rgl.getNumPE(),
                                          rgl.getRemainingGridletLength() );

        int roundUpTime = (int) (time+0.5);   // rounding up
        rgl.setFinishTime(roundUpTime);

        // then send this into itself
        super.sendInternalEvent(roundUpTime);
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
    private void checkGridletCompletion()
    {
        ResGridlet obj = null;
        int i = 0;

        // NOTE: This one should stay as it is since gridletFinish()
        // will modify the content of this list if a Gridlet has finished.
        // Can't use iterator since it will cause an exception
        while ( i < gridletInExecList_.size() )
        {
            obj = (ResGridlet) gridletInExecList_.get(i);

            if (obj.getRemainingGridletLength() == 0.0)
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
            super.sendInternalEvent(60.0*60);
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
        super.resource_.setStatusPE(PE.FREE, rgl.getMachineID(), rgl.getPEID());

        // the order is important! Set the status first then finalize
        // due to timing issues in ResGridlet class
        rgl.setGridletStatus(status);
        rgl.finalizeGridlet();
        super.sendFinishGridlet( rgl.getGridlet() );
        allocateQueueGridlet();   // move Queued Gridlet into exec list

        // once a gridlet has finished, then check whether it is time to
        // remove a reservation
        if (rgl.hasReserved())
        {
            int id=super.searchReservation(reservList_,rgl.getReservationID());
            if (id != -1)
            {
                ARObject obj = (ARObject) reservList_.get(id);
                obj.reduceTotalGridlet();

                // if no Gridlets executed or time has already gone
                // then remove it from the list
                long currentTime = super.getCurrentTime();
                long endTime = obj.getStartTime() +
                               (obj.getDurationTime()*MILLI_SEC);
                if (obj.getTotalGridlet() == 0 && currentTime > endTime)
                {
                    obj.setStatus(GridSimTags.AR_STATUS_COMPLETED);
                    expiryList_.add(obj);
                    reservList_.remove(obj);
                }
            }
        }
    }

    /**
     * Handles an operation of canceling a Gridlet in either execution list
     * or paused list.
     * @param gridletId    a Gridlet ID
     * @param userId       the user or owner's ID of this Gridlet
     * @return an object of ResGridlet or <tt>null</tt> if this Gridlet is not
     *        found
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
            return rgl;
        }

        // if not found, then find in the Paused list
        found = gridletPausedList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            rgl = (ResGridlet) gridletPausedList_.remove(found);
            rgl.setGridletStatus(Gridlet.CANCELED);
            return rgl;
        }

        // if not found, then find in AR waiting list
        found = gridletWaitingList_.indexOf(gridletId, userId);
        if (found >= 0)
        {
            rgl = (ResGridlet) gridletWaitingList_.remove(found);
            rgl.setGridletStatus(Gridlet.CANCELED);
            return rgl;
        }

        // if not found
        rgl = null;
        return rgl;
    }

} 

