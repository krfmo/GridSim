/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import eduni.simjava.Sim_event;
import gridsim.net.Link;

import java.util.ArrayList;
import java.util.Calendar;


/**
 * ARGridResource class handles all Advanced Reservation functionalities.
 * All the functionalities are done by its internal or private methods.<br>
 * <b>NOTE:</b> It is important to set the allocation policy inside
 * {@link gridsim.ResourceCharacteristics} object into
 * {@link gridsim.ResourceCharacteristics#ADVANCE_RESERVATION}. In addition,
 * a resource's scheduler should be extending from
 * {@link gridsim.ARPolicy} class.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.0
 * @see gridsim.GridResource
 * @see gridsim.GridSim
 * @see gridsim.ResourceCharacteristics
 * @see gridsim.AllocPolicy
 * @see gridsim.ARPolicy
 * @invariant $none
 */
public class ARGridResource extends GridResource
{
    /**
     * Allocates a new GridResource object that supports Advanced Reservation.
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduler for this Grid resource. The scheduler
     *                   should be able to handle Advanced Reservations.
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
     * @see gridsim.AllocPolicy
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre $none
     * @post $none
     */
    public ARGridResource(String name, double baud_rate,
                ResourceCharacteristics resource, ResourceCalendar calendar,
                ARPolicy policy) throws Exception
    {
        super(name, baud_rate, resource, calendar, policy);
    }

    /**
     * Allocates a new GridResource object that supports Advanced Reservation.
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   ARGridResource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduler for this Grid resource. The scheduler
     *                   should be able to handle Advanced Reservations.
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
     * @see gridsim.AllocPolicy
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @pre $none
     * @post $none
     */
    public ARGridResource(String name, Link link,
                ResourceCharacteristics resource, ResourceCalendar calendar,
                ARPolicy policy) throws Exception
    {
        super(name, link, resource, calendar, policy);
    }


    /////////////////////// PROTECTED METHOD /////////////////////////////////

    /**
     * Processes other events or services related to Advanced Reservations.
     * This method overrides from a parent class.
     * <p>
     * The services or tags available for this resource are:
     * <ul>
     *      <li> GridSimTags.SEND_AR_CREATE
     *      <li> GridSimTags.SEND_AR_COMMIT_ONLY
     *      <li> GridSimTags.SEND_AR_COMMIT_WITH_GRIDLET
     *      <li> GridSimTags.SEND_AR_CANCEL
     *      <li> GridSimTags.SEND_AR_QUERY
     *      <li> GridSimTags.SEND_AR_MODIFY
     *      <li> GridSimTags.SEND_AR_LIST_BUSY_TIME
     *      <li> GridSimTags.SEND_AR_LIST_FREE_TIME
     * </ul>
     *
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     * @see gridsim.GridResource#processOtherEvent(Sim_event)
     */
    protected void processOtherEvent(Sim_event ev)
    {
        switch ( ev.get_tag() )
        {
            case GridSimTags.SEND_AR_CREATE:
            case GridSimTags.SEND_AR_CREATE_IMMEDIATE:
                handleCreateReservation(ev);
                break;

            case GridSimTags.SEND_AR_COMMIT_ONLY:
                handleCommitOnly(ev);
                break;

            case GridSimTags.SEND_AR_COMMIT_WITH_GRIDLET:
                handleCommitReservation(ev);
                break;

            case GridSimTags.SEND_AR_CANCEL:
                handleCancelReservation(ev);
                break;

            case GridSimTags.SEND_AR_QUERY:
                handleQueryReservation(ev);
                break;

            case GridSimTags.SEND_AR_MODIFY:
                handleModifyReservation(ev);
                break;

            case GridSimTags.SEND_AR_LIST_BUSY_TIME:
            case GridSimTags.SEND_AR_LIST_FREE_TIME:
                handleQueryTime(ev);
                break;

            default:
                /****   // NOTE: Give to the scheduler to process other tags
                System.out.println(super.get_name() + ".processOtherEvent(): " +
                        "Unable to handle request from GridSimTags with " +
                        "tag number " + ev.get_tag() );
                *****/
                super.policy_.processOtherEvent(ev);
                break;
        }
    }

    //////////////////////////////// PRIVATE METHODS /////////////////////////

    /**
     * Checks whether this resource can support Advanced Reservations or not
     * @param src   the sender ID
     * @param returnTag  the tag ID to be returned to sender
     * @param transID    the transaction ID of a sender
     * @param errorTag   type of error tag
     * @param msg   error message
     * @return <tt>true</tt> if all valid, <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    private boolean checkResourceType(int src, int returnTag, int transID,
                                      int errorTag, String msg)
    {
        boolean result = true;
        if (policyType_ != ResourceCharacteristics.ADVANCE_RESERVATION)
        {
            System.out.println(super.get_name() + " : Error - " + msg +
                    ". This resource doesn't support Advance Reservation.");

            result = false;
            int MAX = 2;     // max. array size
            IO_data data = null;
            int size = -1;   // size of an object to be sent

            // if returning create AR event, then use a different array type
            if (returnTag == GridSimTags.RETURN_AR_CREATE)
            {
                long[] sendArray = new long[MAX + 1];
                sendArray[0] = transID;     // [0] = transaction ID
                sendArray[1] = errorTag;    // [1] = reservation ID
                sendArray[2] = -1;          // [2] = expiry time

                size = (3*8) + 6;  // 3 longs + overhead
                data = new IO_data(sendArray, size, src);
            }
            else
            {
                int[] array = new int[MAX];
                array[0] = transID;     // [0] = transaction ID
                array[1] = errorTag;    // [1] = result

                size = (2*4) + 2;  // 2 ints + overhead
                data = new IO_data(array, size, src);
            }

            // send back the array to sender
            super.send(src, 0.0, returnTag, data);
        }

        return result;
    }

    /**
     * Handles a query reservation request.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleQueryReservation(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // id[0] = resID, [1] = reservID, [2] = trans ID, [3] = sender ID
            int[] obj = ( int[] ) ev.get_data();
            int reservID = obj[1];     // get the reservation ID
            src = obj[3];    // get the sender ID

            int returnTag = GridSimTags.RETURN_AR_QUERY_STATUS;
            tag = returnTag + obj[2];  // return tag + transaction ID

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, obj[2],
                      GridSimTags.AR_STATUS_ERROR, "can't query a reservation");

            // if this resource doesn't support AR then exit
            if (!success) {
                return;
            }
            else {
                ((ARPolicy) policy_).handleQueryReservation(reservID, src, tag);
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't query a " +
                  "new reservation.");

            super.send( src, 0.0, GridSimTags.RETURN_AR_QUERY_STATUS,
                new IO_data(new Integer(GridSimTags.AR_STATUS_ERROR),SIZE,src));
        }
    }

    /**
     * Handles a modify reservation request.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleModifyReservation(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // get the data
            ARObject obj = (ARObject) ev.get_data();

            // get the unique tag to send back to recepient
            int returnTag = GridSimTags.RETURN_AR_MODIFY;
            tag = returnTag + obj.getTransactionID();

            src = obj.getUserID();  // get the sender ID

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, obj.getTransactionID(),
                            GridSimTags.AR_MODIFY_FAIL_RESOURCE_CANT_SUPPORT,
                            "can't modify an existing reservation");

            // if this resource doesn't support AR then exit
            if (!success) {
                return;
            }
            else {
                ((ARPolicy) policy_).handleModifyReservation(obj, src, tag);
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't modify an "+
                " existing reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_MODIFY,
                new IO_data(new Integer(GridSimTags.AR_MODIFY_ERROR),SIZE,src));
        }
    }

    /**
     * Handles a query busy/free time request.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleQueryTime(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;
        int transID = -1;

        try
        {
            ARObject obj = (ARObject) ev.get_data();
            double userZone = obj.getTimeZone();
            double resZone  = resource_.getResourceTimeZone();

            long from = obj.getStartTime();
            from = AdvanceReservation.convertTimeZone(from, userZone, resZone);

            long to = from + obj.getDurationTime();
            transID = obj.getTransactionID();
            src = obj.getUserID();

            tag = GridSimTags.RETURN_AR_QUERY_TIME + transID;
            if (policyType_ != ResourceCharacteristics.ADVANCE_RESERVATION) {
                success = false;
            }
            else
            {
                success = true;
                if (ev.get_tag() == GridSimTags.SEND_AR_LIST_FREE_TIME) {
                    ((ARPolicy) policy_).handleQueryFreeTime(from,to,src,tag,userZone);
                }
                else if (ev.get_tag() == GridSimTags.SEND_AR_LIST_BUSY_TIME) {
                    ((ARPolicy) policy_).handleQueryBusyTime(from,to,src,tag,userZone);
                }
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't list busy"+
                " or free reservation time.");

            Object[] array = new Object[2];
            array[0] = new Integer(transID);  // [0] = transaction id
            array[1] = null;                  // [1] = list of times, i.e. null

            super.send(src, 0.0, GridSimTags.RETURN_AR_QUERY_TIME,
                       new IO_data(array, super.SIZE, src));
        }
    }


    /**
     * Handles a cancel reservation request.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleCancelReservation(Sim_event ev)
    {
        int src = -1;   // sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // id[0] = gridletID, [1] = reservID, [2] = transID, [3] = sender ID
            int[] obj = ( int[] ) ev.get_data();

            // get the data inside an array
            int reservationID = obj[1];
            int transactionID = obj[2];
            src = obj[3];

            // gridletID can be -1 if want to cancel ALL Gridlets in 1 reserv
            int gridletID = obj[0];
            int returnTag = GridSimTags.RETURN_AR_CANCEL;
            tag = returnTag + transactionID;

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, transactionID,
                            GridSimTags.AR_CANCEL_ERROR_RESOURCE_CANT_SUPPORT,
                            "can't cancel a reservation");

            if (!success) {
                return;
            }

            // cancels a gridlet of a specific reservation
            else if (success && gridletID != -1)
            {
                ( (ARPolicy) policy_).handleCancelReservation(reservationID,
                                            src, gridletID, tag);
            }

            // cancels ALL gridlets of a specific reservation
            else if (success && gridletID == -1)
            {
                ( (ARPolicy) policy_).handleCancelReservation(reservationID,
                                            src, tag);
            }
        }
        catch (ClassCastException c)
        {
            // cancel a list of Gridlets
            handleCancelList(ev);
            return;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't cancel a "+
                "new reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_CANCEL,
                new IO_data(new Integer(GridSimTags.AR_CANCEL_ERROR),SIZE,src));
        }
    }

    /**
     * Handles a cancel reservation request with a list of Gridlet IDs.
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleCancelList(Sim_event ev)
    {
        boolean success = false;
        int tag = -1;
        int src = 0;

        try
        {
            // [0]=reservID, [1]=list of Gridlet IDs, [2]=transID, [3]=senderID
            Object[] obj = ( Object[] ) ev.get_data();

            // get the data inside an array
            int reservationID = ( (Integer) obj[0] ).intValue();
            ArrayList list = (ArrayList) obj[1];
            int transactionID = ( (Integer) obj[2] ).intValue();
            src = ( (Integer) obj[3] ).intValue();

            // tag = return tag + transaction id
            int returnTag = GridSimTags.RETURN_AR_CANCEL;
            tag = returnTag + transactionID;

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, transactionID,
                            GridSimTags.AR_CANCEL_ERROR_RESOURCE_CANT_SUPPORT,
                            "can't cancel a reservation");

            // if list is empty
            if (list == null) {
                success = false;
            }

            // if successful
            if (success) {
                ( (ARPolicy) policy_).handleCancelReservation(reservationID,
                                      src, list, tag);
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't cancel a "+
                               "new reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_CANCEL,
                new IO_data(new Integer(GridSimTags.AR_CANCEL_ERROR),SIZE,src));
        }
    }

    /**
     * Handles a commit reservation request with no Gridlets.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleCommitOnly(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // id[0] = resID, [1] = reservID, [2] = trans ID, [3] = sender ID
            int[] obj = ( int[] ) ev.get_data();

            // get the data inside the array
            int returnTag = GridSimTags.RETURN_AR_COMMIT;
            tag = returnTag + obj[2];
            src = obj[3];

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, obj[2],
                            GridSimTags.AR_COMMIT_ERROR_RESOURCE_CANT_SUPPORT,
                            "can't commit a reservation");

            // if this resource is not supported AR, then exit
            if (!success) {
                return;
            }
            else {
                ( (ARPolicy) policy_).handleCommitOnly(obj[1], src, tag);
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't commit a "+
                "reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_COMMIT,
                new IO_data(new Integer(GridSimTags.AR_COMMIT_ERROR),SIZE,src));
        }
    }

    /**
     * Handles a commit reservation request with one or more Gridlets.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleCommitReservation(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // [0]=reservID, [1]=gridlet(s), [2]=transID, [3]=senderID
            Object[] obj = ( Object[] ) ev.get_data();

            // get the data inside an array
            int reservationID = ( (Integer) obj[0] ).intValue();
            int transactionID = ( (Integer) obj[2] ).intValue();
            src = ( (Integer) obj[3] ).intValue();

            int returnTag = GridSimTags.RETURN_AR_COMMIT;
            tag = returnTag + transactionID;

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, transactionID,
                            GridSimTags.AR_COMMIT_ERROR_RESOURCE_CANT_SUPPORT,
                            "can't commit a reservation");

            // if a resource doesn't support AR then exit straightaway
            if (!success) {
                return;
            }

            // check again whether obj[1] contains either Gridlet or GridletList
            success = selectCommitMethod(obj[1], reservationID, src, tag);
        }
        catch (ClassCastException c) {
            success = false;
            c.printStackTrace();
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception or other errors, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't commit a"+
                " reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_COMMIT,
                new IO_data(new Integer(GridSimTags.AR_COMMIT_ERROR),SIZE,src));
        }
    }

    /**
     * Chooses whether to commit for one or a list of Gridlets.
     * @param obj    an object containing one Gridlet or a GridletList
     * @param reservID   reservation ID
     * @param src    sender ID
     * @param tag    tag ID
     * @return <tt>true</tt> if this method is successful, <tt>false</tt>
     *         otherwise
     */
    private boolean selectCommitMethod(Object obj,int reservID,int src,int tag)
    {
        boolean flag = true;
        try
        {
            // A case where a reservation only has 1 Gridlet
            Gridlet gridlet = (Gridlet) obj;
            if (checkGridlet(gridlet))
            {
                ( (ARPolicy) policy_).handleCommitReservation(reservID, src,
                        tag, gridlet);
            }
            else {
                flag = false;
            }
        }
        catch (ClassCastException c)
        {
            try
            {
                // A case where a reservation contains 1 or more Gridlets
                GridletList list = (GridletList) obj;
                Gridlet gl = null;

                // For each Gridlet in the list, check whether it has finished
                // before or not
                for (int i = 0; i < list.size(); i++)
                {
                    gl = (Gridlet) list.get(i);
                    if (!checkGridlet(gl))
                    {
                        flag = false;
                        break;
                    }
                }

                if (flag)
                {
                    ( (ARPolicy) policy_).handleCommitReservation(reservID, src,
                        tag, list);
                }
            }
            catch (ClassCastException again) {
                flag = false;
            }
        }
        catch (Exception e) {
            flag = false;
        }

        return flag;
    }

    /**
     * Checks whether a Gridlet has finished previously or not
     * @param gl   a Gridlet object
     * @return <tt>true</tt> if a Gridlet has finished execution beforehand,
     *         <tt>false</tt> otherwise
     */
    private boolean checkGridlet(Gridlet gl)
    {
        // checks whether this Gridlet has finished or not
        if (gl.isFinished())
        {
            System.out.println(super.get_name() + ": Error - Gridlet #" +
                               gl.getGridletID() + " for User #" +
                               gl.getUserID() + " is already finished.");
            return false;
        }

        // process this Gridlet to this GridResource
        gl.setResourceParameter(super.get_id(),
                                super.resource_.getCostPerSec());

        return true;
    }

    /**
     * Handles a create reservation request.
     * @param ev  Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void handleCreateReservation(Sim_event ev)
    {
        int src = -1;   // the sender id
        boolean success = false;
        int tag = -1;

        try
        {
            // get the data
            ARObject obj = (ARObject) ev.get_data();
            src = obj.getUserID();

            // get the unique tag to send back to recepient
            int returnTag = GridSimTags.RETURN_AR_CREATE;
            tag = returnTag + obj.getTransactionID();

            // check whether this resource can support AR or not
            success = checkResourceType(src, returnTag, obj.getTransactionID(),
                            GridSimTags.AR_CREATE_FAIL_RESOURCE_CANT_SUPPORT,
                            "can't create a new reservation");

            // if this resource can't support AR then exit
            if (!success) {
                return;
            }

            // if it is AR
            if (ev.get_tag() == GridSimTags.SEND_AR_CREATE) {
                ( (ARPolicy) policy_).handleCreateReservation(obj, src, tag);
            }
            else {   // if it is an immediate reservation
                ( (ARPolicy) policy_).handleImmediateReservation(obj, src, tag);
            }
        }
        catch (ClassCastException c) {
            success = false;
        }
        catch (Exception e) {
            success = false;
        }

        // if there is an exception, then send back an error msg
        if (!success && tag != -1)
        {
            System.out.println(super.get_name() + " : Error - can't create a"+
                " new reservation.");

            super.send(src, 0.0, GridSimTags.RETURN_AR_CREATE,
                new IO_data(new Integer(GridSimTags.AR_CREATE_ERROR),SIZE,src));
        }
    }

} 

