/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Created on: Nov 2006.
 * Copyright (c) 2007, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.resFailure;

import gridsim.index.*;
import java.util.*;
import eduni.simjava.*;
import gridsim.*;
import gridsim.net.Link;
import gridsim.resFailure.FailureMsg;
import eduni.simjava.distributions.DiscreteGenerator;
import eduni.simjava.distributions.ContinuousGenerator;
import gridsim.util.Variate;
import java.io.FileWriter;


/**
 * RegionalGISWithFailure is based on {@link gridsim.index.RegionalGIS}, but
 * with added failure functionalities.
 * RegionalGISWithFailure is a simple regional GridInformationService (GIS)
 * entity that
 * performs basic functionalities, such as storing a list of local resources,
 * and asking other regional GIS entities for resources.
 * <p>
 * If you want to implement other complex functionalities, you need to extend
 * this class and to override {@link #processOtherEvent(Sim_event)}
 * and/or {@link #registerOtherEntity()} method.
 *
 * @author  Agustin Caminero and Anthony Sulistio
 * @since   GridSim Toolkit 4.1
 * @see     gridsim.index.RegionalGIS
 */
public class RegionalGISWithFailure extends AbstractGIS
{
    /** This entity ID in <tt>Integer</tt> object. */
    protected Integer myID_;

    private ArrayList resList_;         // all resources within this region
    private ArrayList arList_;          // AR resources only within this region
    private ArrayList globalResList_;   // all resources outside this region
    private ArrayList globalResARList_; // AR resources only outside this region
    private LinkedList regionalList_;   // list of regional GIS, incl. myself
    private ArrayList userList_;    // list of users querying for global res
    private ArrayList userARList_;  // list of users querying for global AR res
    private int numRes_;  // counting for num of GIS entities for res request
    private int numAR_;   // counting for num of GIS entities for res AR request

    // defines how GIS should decide how many resource fails
    private DiscreteGenerator failureNumResPatternDiscrete_;

    // defines how GIS should decide when the resource will fail
    private DiscreteGenerator failureTimePatternDiscrete_;

    // defines how GIS should decide how long the resource will be out of order
    private DiscreteGenerator failureLengthPatternDiscrete_;


    // defines how GIS should decide how many resource fails
    private ContinuousGenerator failureNumResPatternCont_;

    // defines how GIS should decide when the resource will fail
    private ContinuousGenerator failureTimePatternCont_;

    // defines how GIS should decide how long the resource will be out of order
    private ContinuousGenerator failureLengthPatternCont_;


    // New, as some distibutions coming with SimJava2 does not work
    // defines how GIS should decide how many resource fails
    private Variate failureNumResPatternVariate_;

    // defines how GIS should decide when the resource will fail
    private Variate failureTimePatternVariate_;

    // defines how GIS should decide how long the resource will be out of order
    private Variate failureLengthPatternVariate_;

    // a flag to denote whether to record events into a file or not
    private boolean record_ = false;

    // denotes whether sim has just began or it has been running for some time
    private boolean begining;



    /**
     * Creates a new regional GIS entity
     * @param name  this regional GIS name
     * @param link  a network link to this entity
     * @param failureLengthPattern      defines on how long
     *                                  each resource will be out of order
     * @param failureNumResPattern      defines on how many resource fails
     * @param failureTimePattern        defines on when each resource will fail
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public RegionalGISWithFailure(String name, Link link,
                           DiscreteGenerator failureNumResPattern,
                           DiscreteGenerator failureTimePattern,
                           DiscreteGenerator failureLengthPattern)
                           throws Exception
    {
        super(name, link);
        if (setFailureGenerator(failureNumResPattern, failureTimePattern,
                                failureLengthPattern))
            init();
        else
            throw new Exception(super.get_name() +
                    " : Problem when setting the failure patterns for the " +
                    super.get_name() + " entity");
    }

    /**
     * Creates a new regional GIS entity
     * @param name  this regional GIS name
     * @param link  a network link to this entity
     * @param failureLengthPattern      defines on how long
     *                                  each resource will be out of order
     * @param failureNumResPattern      defines on how many resource fails
     * @param failureTimePattern        defines on when each resource will fail
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public RegionalGISWithFailure(String name, Link link,
                      ContinuousGenerator failureNumResPattern,
                      ContinuousGenerator failureTimePattern,
                      ContinuousGenerator failureLengthPattern)
                      throws Exception
    {
       super(name, link);
       if (setFailureGenerator(failureNumResPattern, failureTimePattern,
                               failureLengthPattern))
           init();
       else
           throw new Exception(super.get_name() +
                   " : Problem when setting the failure patterns for the " +
                   super.get_name() + " entity");
    }

   /**
    * Creates a new regional GIS entity
    * @param name  this regional GIS name
    * @param link  a network link to this entity
    * @param failureLengthPattern      defines on how long
    *                                  each resource will be out of order
    * @param failureNumResPattern      defines on how many resource fails
    * @param failureTimePattern        defines on when each resource will fail
    * @throws Exception This happens when creating this entity before
    *                   initializing GridSim package or this entity name is
    *                   <tt>null</tt> or empty
    * @pre name != null
    * @pre link != null
    * @post $none
    */
   public RegionalGISWithFailure(String name, Link link,
                     Variate failureNumResPattern, Variate failureTimePattern,
                     Variate failureLengthPattern) throws Exception
   {
       super(name, link);
       if (setFailureGenerator(failureNumResPattern, failureTimePattern,
                               failureLengthPattern))
           init();
       else
           throw new Exception(super.get_name() +
                   " : Problem when setting the failure patterns for the " +
                   super.get_name() + " entity");
   }

    /**
     * Asks this resource to record its activities. <br>
     * NOTE: this method should be called <b>BEFORE</b> the simulation starts.
     * If an existing file exists, the new activities will be appended at the
     * end. The file name is this entity name.
     *
     * @param trace     <tt>true</tt> if you want to record this resource's
     *                  activities, <tt>false</tt> otherwise
     */
    public void setTrace(boolean trace)
    {
        record_ = trace;
        initializeReportFile();
    }

    /**
     * Initialises all attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        myID_ = new Integer( super.get_id() );
        resList_ = new ArrayList();
        arList_ = new ArrayList();
        regionalList_ = null;
        globalResList_ = null;
        globalResARList_ = null;
        userList_ = null;
        userARList_ = null;
        numAR_ = -1;
        numRes_ = -1;

        begining = true;
    }

    /**
     * Stores the incoming registration ID into the given list. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object.
     *
     * @param ev      a new Sim_event object or incoming registration request
     * @param list    a list storing the registration IDs
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre ev != null
     * @pre list != null
     * @post $none
     */
    protected boolean storeRegistrationID(Sim_event ev, List list)
    {
        boolean result = false;
        if (ev == null || list == null) {
            return result;
        }

        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            Integer id = (Integer) obj;
            list.add(id);
            result = true;

            if (record_) {
                write("Registering", id.intValue(), GridSim.clock());
                System.out.println();
                System.out.println(super.get_name() + ": registering " +
                    GridSim.getEntityName(id));

                for (int i = 0; i < list.size() ; i++)
                {
                    System.out.println(super.get_name() + ": list["+ i +"] = " +
                        GridSim.getEntityName((Integer) list.get(i)) );
                }
                System.out.println();
            }
        }

        return result;
    }

    /**
     * Process a registration request from a resource entity
     * supporting Advanced Reservation to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected void processRegisterResourceAR(Sim_event ev)
    {
        boolean result1 = storeRegistrationID(ev, arList_);
        boolean result2 = storeRegistrationID(ev, resList_);

        if (!result1 || !result2)
        {
            System.out.println(super.get_name() +
                ".processRegisterResourceAR(): Warning - can't register " +
                "a resource ID.");
        }
    }

    /**
     * Process a registration request from a resource entity to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected void processRegisterResource(Sim_event ev)
    {
        boolean result = storeRegistrationID(ev, resList_);

        if (!result)
        {
            System.out.println(super.get_name() +
                ".processRegisterResource(): Warning - can't register " +
                "a resource ID.");
        }
    }

    /**
     * This function processes an incoming event,
     * whose tag is GRIDRESOURCE_FAILURE.
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processGridResource_Failure(Sim_event ev)
    {
        Random random = new Random(); // a random generator
        Integer res_id_Integer;
        int res_id;

        /*****
        We have different cases:
         - when there are no resources available at this moment, just schedule
           this event for another time in the future
         - if we are the beginning of the simulation, then we have to decide
           how many res will fail, when and for how long.
         - if we are not at the beginning of the sim, we have to choose
           a resource and send the GRIDRESOURCE_FAILURE event.
        *****/

        // If there are no available resources, then we have to
        // schedule ths event for another time in the future
        if (resList_.size() == 0)
        {
            System.out.println(
                "------- GRIDRESOURCE_FAILURE received at the " +
                super.get_name() +
                ", but there is no resources available." +
                " So, scheduling a new GRIDRESOURCE_FAILURE to GIS in 2 mins.");

            super.send(super.get_id(), 120, GridSimTags.GRIDRESOURCE_FAILURE);
        }
        // if the sim has just began, then we have to decide how many res
        // will fail, when and for how long
        else if (begining)
        {
            begining = false; // the sim is already running

            // how many resources will fail
            int numResFail = (int) getNextFailureNumResSample();

            // in case we have chosen too many res to fail
            if (numResFail > resList_.size())
                numResFail = resList_.size();

            /****************/
            if (record_) {
                System.out.println(super.get_name() + ": " + numResFail +
                   " resources will fail in this simulation. " +
                   "Num of failed machines on each resource will be decided later");
            }
            /****************/

            for (int i = 0; i < numResFail; i++)
            {
                // when a resource will fail
                double resTimeFail = getNextFailureTimeSample();

                // Now, schedule an event to this entity, so that when that
                // event happens, RegionalGISWithFailure will
                // choose the resource which will fail and how long the failure
                // will last. Then, it will send the failure signal to the resource.

                super.send(super.get_id(), resTimeFail,
                           GridSimTags.GRIDRESOURCE_FAILURE);

                /*********/
                if (record_) {
                    System.out.println(super.get_name() +
                       ": sends an autogenerated GRIDRESOURCE_FAILURE to itself." +
                       " Clock: " + GridSim.clock() + ", resTimeFail: " +
                       resTimeFail + " seconds");
                }
                /*********/
            }

        } // if (begining)
        else
        {
            // Send the GRIDRESOURCE_FAILURE event to the resources.

            // First, choose how long the failure will be, and the number
            // of machines affected. Convert hours into seconds
            double failureLength = getNextFailureLengthSample() * 3600;
            int numMachFailed = (int) getNextNumMachinesFailedSample();

            // Only do anything if the number of machines and the length of
            // the failure are > 0
            if ((numMachFailed > 0) && (failureLength > 0) )
            {

                ResourceCharacteristics resChar;

                // Second, choose a resource to send the failure event.
                // Only choose a resource whose machines are all of them
                // working properly.
                int res_num;

                boolean isWorking;

                do
                {
                    res_num = random.nextInt(resList_.size());
                    res_id_Integer = (Integer) resList_.get(res_num);
                    res_id = res_id_Integer.intValue();

                    resChar = getResourceCharacteristics(res_id);

                    if (resChar == null)
                    {
                        System.out.println(super.get_name() + " resChar == null");
                        isWorking = false;
                    }
                    else
                    {
                        isWorking = resChar.isWorking();
                    }
                } while (!isWorking);

                FailureMsg resFailure = new FailureMsg(failureLength, res_id);
                resFailure.setNumMachines(numMachFailed);

                // Sends the recovery time for this resource. Sends
                // a deferred event to itself for that.
                super.send(super.get_id(),
                           GridSimTags.SCHEDULE_NOW + failureLength,
                           GridSimTags.GRIDRESOURCE_RECOVERY, resFailure);

                /*****************/
                if (record_) {
                    System.out.println(super.get_name() +
                       ": sends a GRIDRESOURCE_FAILURE event to the resource " +
                       GridSim.getEntityName(res_id) + ". numMachFailed: " +
                       numMachFailed + ". Clock: " + GridSim.clock() +
                       ". Fail duration: " + (failureLength / 3600) +
                       " hours. Some machines may still work or may not.");
                }
                /*****************/

                // Send the GRIDRESOURCE_FAILURE event to the resource.
                super.send(super.output, 0.0, ev.get_tag(),
                           new IO_data(resFailure, Link.DEFAULT_MTU, res_id));
            }

        } // else (if begining)

    }

    /**
     * This function processes an incoming event, whose tag is
     * GRIDRESOURCE_RECOVERY.
     * What the GIS has to do is forward the event to the failed resource,
     * whose id comes with the event.
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processGridResource_Recovery(Sim_event ev)
    {
        Object obj = ev.get_data();
        if (obj instanceof FailureMsg)
        {
            FailureMsg resfail = (FailureMsg) ev.get_data();

            int resource_id = resfail.getRes();

            /***********/
            if (record_) {
                 System.out.println(super.get_name() +
                       ": sends a GRIDRESOURCE_RECOVERY to the resource " +
                       GridSim.getEntityName(resource_id) +
                       ". Clock: " + GridSim.clock());
            }
            /***********/

            // Send the GRIDRESOURCE_RECOVERY event to the resource.
            super.send(super.output, 0.0, ev.get_tag(),
                       new IO_data(resfail, Link.DEFAULT_MTU,
                                   resource_id));

        } // if (obj instanceof FailureMsg)
    }


    /**
     * This function removes a resource from the list of available resources.
     * This is done because an user has detected the failure, and has notified
     *  it to the GIS
     * @param ev  a Sim_event object (or an incoming event or request)*/
    private void processResourceFailed (Sim_event ev)
    {
        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            Integer resID_Int = (Integer) ev.get_data();
            int resID = resID_Int.intValue();

            if (record_) {
                System.out.println(super.get_name() +
                   ": receives a resource failure information event. " +
                   "Failed resource is " + GridSim.getEntityName(resID) +
                   ". resID: " + resID + ". Clock: " + GridSim.clock());
            }

            removeResource(resID);
        }
    }

    /**
     * This function removes a resource from the list of available resources.
     * This is done because an user has detected the failure, and has informed
     * the GIS about that
     * @param resID the id of the resource to be removed
     */
    private void removeResource (int resID)
    {
        for (int j = 0; j < resList_.size(); j++)
        {
            if (((Integer) resList_.get(j)).intValue() == resID)
            {
                resList_.remove(j);

                if (record_) {
                    write("Removing", resID, GridSim.clock());
                    if (resList_.size() == 0)
                    {
                        System.out.println(super.get_name() +
                            ": No resources available at this moment. Clock: " +
                            GridSim.clock());
                    }
                }
            }
        }

        /***********/
        if (record_) {
            System.out.println();
            System.out.println(super.get_name() + ": Resource list after removal");
            for (int j = 0; j < resList_.size(); j++)
            {
                System.out.println(super.get_name() + ": list["+ j +"] = " +
                    GridSim.getEntityName((Integer)resList_.get(j)) );
            }
            System.out.println();
        }
        /**********/
    }

   /**
    * Initializes the results files (put headers over each column)
    */
    private void initializeReportFile()
    {
        if (!record_) {
            return;
        }

        // Initialize the results file
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(super.get_name(), true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }

        try {
            fwriter.write("Event \t ResourceID \t Clock\n");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }

        try {
            fwriter.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }
    }

    /**
     * Write an event of this entity into a file.
     * If an existing file exists, the new event will be appended at the end.
     * The file name is this entity name.
     *
     * @param event     Values: "Removing" or "Registering" a resource
     * @param resID     resource id
     * @param clock     Current time
     */
    protected void write(String event, int resID, double clock)
    {
        if (!record_) {
            return;
        }

        // Write into a results file
        FileWriter fwriter = null;
        try
        {
            fwriter = new FileWriter(super.get_name(), true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while opening file " + super.get_name());
        }

        try
        {
            fwriter.write(event + "\t" + resID + "\t" + clock + "\n");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                   "Unwanted errors while writing on file " + super.get_name());
        }

        try
        {
            fwriter.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println(
                    "Unwanted errors while closing file " + super.get_name());
        }
    }

    /**
     * This function processes an incoming event,
     * whose tag is GRIDRESOURCE_POLLING.
     * What the GIS has to do is poll the resource in its
     * list of available resources,
     * and remove those resources which are not available anymore.
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processGridResource_Polling(Sim_event ev)
    {
        Integer res_id_Integer;
        int res_id;
        AvailabilityInfo resAv = null;

        // Polls the resources that are available right now, as those
        // which are out of order were totally removed
        for (int i = 0; i < resList_.size(); i++)
        {
            res_id_Integer = (Integer) resList_.get(i);
            res_id = res_id_Integer.intValue();

            pollResource(res_id);
        }

        // receive the polling back
        int resListSize = resList_.size();
        for (int i = 0; i < resListSize; i++)
        {
            do
            {
                super.sim_pause(50);
                resAv = pollReturn();
            } while (resAv == null);

            res_id = resAv.getResID();

            /*****************
            // NOTE: this keeps printing at every n seconds interval.
            if (record_ == true) {
                System.out.println(super.get_name() +
                           ": receives a poll response from " +
                           GridSim.getEntityName(res_id) + ". resID: " + res_id +
                           ". Is res available? " + resAv.getAvailability() +
                           ". Clock: " + GridSim.clock());
            }
            ****************/

            // Find the AvailabilityInfo object corresponding to the resource
            // which has answered this poll request
            // and, if the resource is out of order, remove it from the
            // list of available resources.

            for (int j = 0; j < resList_.size(); j++)
            {
                if (((Integer) resList_.get(j)).intValue() == res_id)
                {
                    // Only do anything when the res is out of order
                    if (!resAv.getAvailability())
                    {
                        removeResource(res_id);
                    }
                }
            } // for

        } // for

        /**********/
        if (record_) {
            if (resList_.size() == 0) {
                System.out.println(super.get_name() +
                        ": After polling, no resource in the GIS. ");
            }
        }
        /*********/

        // Schedule the following polling event.
        super.send(super.get_id(), GridSimTags.POLLING_TIME_GIS,
                   GridSimTags.GRIDRESOURCE_POLLING);

    }

    /**
     * Process an incoming request that uses a user-defined tag. <br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev)
    {

        switch (ev.get_tag())
        {
            // Resource failure event: send a failure event to a resource.
            case GridSimTags.GRIDRESOURCE_FAILURE:
                processGridResource_Failure(ev);
                break;

                // Resource recovery event.
            case GridSimTags.GRIDRESOURCE_RECOVERY:
                processGridResource_Recovery(ev);
                break;


                // Time for polling resources
            case GridSimTags.GRIDRESOURCE_POLLING:
                processGridResource_Polling(ev);
                break;

                // A user tells the GIS that a resource is out of order
            case AbstractGIS.NOTIFY_GIS_RESOURCE_FAILURE:
                processResourceFailed(ev);
                break;

        } // switch ( ev.get_tag() )

    }

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs, that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceList(Sim_event ev)
    {
        if (ev == null || ev.get_data() == null) {
            return;
        }

        Integer id = (Integer) ev.get_data();
        int tag = AbstractGIS.GIS_INQUIRY_RESOURCE_RESULT;


        boolean result = sendListToSender(id.intValue(), tag, resList_);
        if (!result)
        {
            System.out.println(super.get_name() +
                ".processGISResourceList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs supporting Advanced Reservation,
     * that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceARList(Sim_event ev)
    {
        if (ev == null || ev.get_data() == null) {
            return;
        }

        Integer id = (Integer) ev.get_data();
        int tag = AbstractGIS.GIS_INQUIRY_RESOURCE_AR_RESULT;


        boolean result = sendListToSender(id.intValue(), tag, arList_);
        if (!result)
        {
            System.out.println(super.get_name() +
                ".processGISResourceARList(): Warning - unable to send a " +
                "list of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list supporting Advanced Reservation. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceARResult(Sim_event ev)
    {
        try
        {
            List list = (List) ev.get_data();    // get the data
            globalResARList_.addAll(list);       // add the result into a list
            numAR_--;       // decrement the counter for GIS entity


            // send back the result to user(s)
            if (numAR_ == 0)
            {
                numAR_ = -1;
                sendBackResult(globalResARList_,
                    AbstractGIS.INQUIRY_GLOBAL_RESOURCE_AR_LIST, userARList_);
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ": Error - expected to send List object in ev.get_data()");
        }
    }

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceResult(Sim_event ev)
    {
        try
        {
            List list = (List) ev.get_data();
            globalResList_.addAll(list);
            numRes_--;


            // send back the result to user(s)
            if (numRes_ == 0)
            {
                numRes_ = -1;
                sendBackResult(globalResList_,
                    AbstractGIS.INQUIRY_GLOBAL_RESOURCE_LIST, userList_);
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ": Error - expected to send List object in ev.get_data()");
        }
    }

    /**
     * Sends the result back to sender
     * @param list  a List object containing resource IDs
     * @param tag   a return tag name
     * @param userList  a list of user IDs
     *
     * @pre userList != null
     * @post $none
     */
    private void sendBackResult(List list, int tag, ArrayList userList)
    {
        if (userList == null) {
            return;
        }

        // send back the result to each user in the list
        Iterator it = userList.iterator();
        while ( it.hasNext() )
        {
            Integer id = (Integer) it.next();
            sendListToSender(id.intValue(), tag, list);
        }

        userList.clear();   // then clear up the list
    }

    /**
     * Process an incoming request about getting a list of regional GIS IDs
     * (including this entity ID), that are registered to the
     * {@link gridsim.GridInformationService} or system GIS.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processInquiryRegionalGIS(Sim_event ev)
    {
        // get regional GIS list from system GIS
        LinkedList regionalList = requestFromSystemGIS();

        // then send the list to sender
        boolean result = sendListToSender(ev, regionalList);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processInquiryRegionalGIS(): Warning - unable to send a " +
                "list of regional GIS IDs to sender.");
        }
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * supporting Advanced Reservation that are registered in other regional
     * GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGlobalResourceARList(Sim_event ev)
    {
        LinkedList regionalList = null;   // regional GIS list
        int eventTag = AbstractGIS.GIS_INQUIRY_RESOURCE_AR_LIST;
        boolean result = false;

        // for a first time request, it needs to call the system GIS first,
        // then asks each regional GIS for its resource IDs.
        if (globalResARList_ == null)
        {
            // get regional GIS list from system GIS first
            regionalList = requestFromSystemGIS();

            // ask a resource list from each regional GIS
            result = getListFromOtherRegional(regionalList, eventTag);
            if (result == true)
            {
                globalResARList_ = new ArrayList();  // storing global AR
                numAR_ = regionalList.size() - 1;    // excluding GIS itself

                // then store the user ID
                Integer id = (Integer) ev.get_data();
                userARList_ = new ArrayList();
                userARList_.add(id);
                return;     // then exit
            }
        }

        // cache the request and store the user ID if it is already sent
        if (numAR_ > 0 && userARList_ != null && userARList_.size() > 0)
        {
            Integer id = (Integer) ev.get_data();
            userARList_.add(id);
            return;     // then exit
        }

        result = sendListToSender(ev, globalResARList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processGlobalResourceARList(): Warning - can't send a " +
                "resource AR list to sender.");
        }
    }

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs, that are registered in other regional GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGlobalResourceList(Sim_event ev)
    {
        /***
        NOTE: possible cases are
        - if there is only 1 local GIS and no other regional GISes
        - if there is only 1 user queries for this
        - if there are 2 or more users query at different time
        ****/

        LinkedList regionalList = null;   // regional GIS list
        int eventTag = AbstractGIS.GIS_INQUIRY_RESOURCE_LIST;
        boolean result = false;

        // for a first time request, it needs to call the system GIS first,
        // then asks each regional GIS for its resource IDs.
        if (globalResList_ == null)
        {
            // get regional GIS list from system GIS first
            regionalList = requestFromSystemGIS();

            // ask a resource list from each regional GIS
            result = getListFromOtherRegional(regionalList, eventTag);
            if (result)
            {
                globalResList_ = new ArrayList();    // storing global resources
                numRes_ = regionalList.size() - 1;   // excluding itself

                // then store the user ID
                Integer id = (Integer) ev.get_data();
                userList_ = new ArrayList();
                userList_.add(id);
                return;     // then exit
            }
        }

        // cache the request and store the user ID if it is already sent
        if (numRes_ > 0 && userList_ != null && userList_.size() > 0)
        {
            Integer id = (Integer) ev.get_data();
            userList_.add(id);
            return;     // then exit
        }

        // send the result back to sender, where the list could be empty
        result = sendListToSender(ev, globalResList_);
        if (!result)
        {
            System.out.println(super.get_name() +
                ".processGlobalResourceList(): Warning - can't send a " +
                "resource list to sender.");
        }
    }

    /**
     * Get a list of IDs specified in the eventTag from other regional GIS
     * @param regionalList  a list of regional GIS IDs
     * @param eventTag      an event tag or type of request
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre regionalList != null
     * @post $none
     */
    protected boolean getListFromOtherRegional(List regionalList, int eventTag)
    {
        // check for input first
        if (regionalList == null || regionalList.size() == 0) {
            return false;
        }

        // a loop to ask each regional GIS for its resource IDs
        Iterator it = regionalList.iterator();
        while ( it.hasNext() )
        {
            Integer obj = (Integer) it.next();

            // can not send to itself
            if (obj.equals(myID_) == true) {
                continue;
            }

            // send a request to a regional GIS
            super.send( super.output, 0.0, eventTag,
                        new IO_data(myID_, Link.DEFAULT_MTU, obj.intValue()) );
        }

        return true;
    }

    /**
     * Asks from {@link gridsim.GridInformationService} or system GIS about
     * a list of regional GIS entity ID.
     * @return a list of regional GIS entity ID
     * @pre $none
     * @post $none
     */
    protected LinkedList requestFromSystemGIS()
    {
        // get the regional GIS list from local cache
        if (regionalList_ != null) {
            return regionalList_;
        }
        else {
            regionalList_ = new LinkedList();
        }

        // for the first time, ask the regional GIS list from system GIS
        int eventTag = GridSimTags.REQUEST_REGIONAL_GIS;
        boolean result = requestFromSystemGIS(eventTag, regionalList_);

        return regionalList_;
    }

    /**
     * Asks from {@link gridsim.GridInformationService} or system GIS about
     * a specific event or request.
     *
     * @param eventTag  an event tag or type of request
     * @param list      a list storing the results
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre list != null
     * @post $none
     */
    protected boolean requestFromSystemGIS(int eventTag, List list)
    {
        boolean result = false;
        if (list == null) {
            return result;
        }

        // send a request to system GIS for a list of other regional GIS
        super.send( super.output, 0.0, eventTag,
                    new IO_data(myID_, Link.DEFAULT_MTU, super.systemGIS_) );

        // waiting for a response from system GIS
        Sim_type_p tag = new Sim_type_p(eventTag);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        try
        {
            List tempList = (List) ev.get_data();
            list.addAll(tempList);
            result = true;
        }
        catch (Exception e)
        {
            result = false;
            System.out.println(super.get_name() +
                    ".requestFromSystemGIS(): Exception error.");
        }

        return result;
    }

    /**
     * Sends a given list to sender
     * @param ev    a Sim_event object
     * @param list  a list to be sent to
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private boolean sendListToSender(Sim_event ev, List list)
    {
        if (ev == null) {
            return false;
        }

        boolean result = false;
        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            Integer id = (Integer) obj;
            result = sendListToSender(id.intValue(), ev.get_tag(), list);
        }

        return result;
    }

    /**
     * Sends a list to sender
     * @param senderID  the sender ID
     * @param tag       an event tag
     * @param list      a list to be sent to
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre senderID != -1
     * @post $none
     */
    protected boolean sendListToSender(int senderID, int tag, List list)
    {
        if (senderID < 0) {
            return false;
        }

        int length = 0;
        if (list == null || list.size() == 0) {
            length = 1;
        }
        else {
            length = list.size();
        }



        // Send the event or message
        super.send( super.output, 0.0, tag,
                    new IO_data(list, Link.DEFAULT_MTU*length, senderID) );

        return true;
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processResourceList(Sim_event ev)
    {
        /*// REMOVE !!!
        System.out.println("---");
        for (int i = 0; i < resList_.size(); i++)
            System.out.println(super.get_name()+ ": resList_.get(" + i + "): " +
                 GridSim.getEntityName(((Integer) resList_.get(i)).intValue()));
        System.out.println("---");
        // UNTIL HERE*/

        boolean result = sendListToSender(ev, resList_);
        if (!result)
        {
            System.out.println(super.get_name() +
                ".processResourceList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * supporting Advanced Reservation that are registered to this regional GIS
     * entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processResourceARList(Sim_event ev)
    {
        boolean result = sendListToSender(ev, arList_);
        if (!result)
        {
            System.out.println(super.get_name() +
                ".processResourceARList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Registers other information to {@link gridsim.GridInformationService} or
     * system GIS.<br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity()
    {
        double resTimeFail = getNextFailureTimeSample();

        // the time when we will decide when a resource will fail
        // Now, schedule an event to this entity, so that when that event
        // happens, RegionalGISWithFailure will
        // initiate the process to decide when a res will fail, which resource
        // will fail and how long the failure will last.
        // Then, it will send  the failure signal
        // to the resource.
        super.send(super.get_id(), resTimeFail, GridSimTags.GRIDRESOURCE_FAILURE);

        /**************/
        if (record_) {
            System.out.println(super.get_name() +
               ": sends an autogenerated GRIDRESOURCE_FAILURE to itself."+
               " Clock(): " + GridSim.clock() + ". resTimeFail: " +
               resTimeFail + " seconds");
        }
        /***************/

        // Now, we have to start the polling functionality
        super.send(super.get_id(), GridSimTags.POLLING_TIME_GIS,
                   GridSimTags.GRIDRESOURCE_POLLING);
    }


    /**
     * Informs the registered entities regarding to the end of a simulation.<br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     * @pre $none
     * @post $none
     */
    protected void processEndSimulation()
    {
        resList_.clear();
        arList_.clear();

        if (regionalList_ != null) {
            regionalList_.clear();
        }

        if (globalResList_ != null) {
            globalResList_.clear();
        }

        if (globalResARList_ != null) {
            globalResARList_.clear();
        }

        if (userList_ != null) {
            userList_.clear();
        }

        if (userARList_ != null) {
            userARList_.clear();
        }
    }

    /**
     * Sets failure generators for this entity.
     * @param failureNumResPattern   to decide on how many resources will fail
     * @param failureTimePattern     to decide on when the fail will be
     * @param failureLengthPattern   to decide on how long the fail will be
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFailureGenerator(DiscreteGenerator failureNumResPattern,
                                       DiscreteGenerator failureTimePattern,
                                       DiscreteGenerator failureLengthPattern)
    {
        if ((failureNumResPattern == null) || (failureTimePattern == null) ||
            (failureLengthPattern == null))
        {
            return false;
        }

        failureNumResPatternDiscrete_ = failureNumResPattern;
        failureTimePatternDiscrete_ = failureTimePattern;
        failureLengthPatternDiscrete_ = failureLengthPattern;

        failureNumResPatternCont_ = null;
        failureTimePatternCont_ = null;
        failureLengthPatternCont_ = null;

        failureNumResPatternVariate_ = null;
        failureTimePatternVariate_ = null;
        failureLengthPatternVariate_ = null;

        return true;
    }


    /**
     * Sets failure generators for this entity.
     * @param failureNumResPattern   to decide on how many resources will fail
     * @param failureTimePattern     to decide on when the fail will be
     * @param failureLengthPattern   to decide on how long the fail will be
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFailureGenerator(ContinuousGenerator failureNumResPattern,
                                       ContinuousGenerator failureTimePattern,
                                       ContinuousGenerator failureLengthPattern)
    {
        if ((failureNumResPattern == null) || (failureTimePattern == null) ||
            (failureLengthPattern == null))
        {
            return false;
        }

        failureNumResPatternDiscrete_ = null;
        failureTimePatternDiscrete_ = null;
        failureLengthPatternDiscrete_ = null;

        failureNumResPatternCont_ = failureNumResPattern;
        failureTimePatternCont_ = failureTimePattern;
        failureLengthPatternCont_ = failureLengthPattern;

        failureNumResPatternVariate_ = null;
        failureTimePatternVariate_ = null;
        failureLengthPatternVariate_ = null;

        return true;
    }


    /**
     * Sets failure generators for this entity.
     * @param failureNumResPattern   to decide on how many resources will fail
     * @param failureTimePattern     to decide on when the fail will be
     * @param failureLengthPattern   to decide on how long the fail will be
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFailureGenerator(Variate failureNumResPattern,
                                       Variate failureTimePattern,
                                       Variate failureLengthPattern)
    {
        if ((failureNumResPattern == null) || (failureTimePattern == null) ||
            (failureLengthPattern == null))
        {
            return false;
        }

        failureNumResPatternDiscrete_ = null;
        failureTimePatternDiscrete_ = null;
        failureLengthPatternDiscrete_ = null;

        failureNumResPatternCont_ = null;
        failureTimePatternCont_ = null;
        failureLengthPatternCont_ = null;

        failureNumResPatternVariate_ = failureNumResPattern;
        failureTimePatternVariate_ = failureTimePattern;
        failureLengthPatternVariate_ = failureLengthPattern;


        return true;
    }


    /**
     * This function returns the next sample for the number of resources
     * which will fail.
     * We take into account whether the pattern for this value is
     * discrete or continuous.
     * @return the sample for the number of resources which will fail
     */
    protected double getNextFailureNumResSample()
    {
        double sample;
        if (failureNumResPatternDiscrete_ != null)
        {
            sample = failureNumResPatternDiscrete_.sample();
            return Math.abs(sample);
        }
        else if (failureNumResPatternCont_ != null)
        {
            sample = failureNumResPatternCont_.sample();
            return Math.abs(sample);
        }
        else if (failureNumResPatternVariate_ != null)
        {
            sample = failureNumResPatternVariate_.gen();
            return Math.abs(sample);
        }
        else
            return -1;
    }

    /**
     * This function returns the next sample for the time when resources
     * will fail.
     * We take into account whether the pattern for this value is
     * discrete or continuous.
     * @return the sample for the time when resources will fail
     */
    protected double getNextFailureTimeSample() {
        double sample;
        if (failureTimePatternDiscrete_ != null)
        {
            sample = failureTimePatternDiscrete_.sample();
            return Math.abs(sample);
        }
        else if (failureTimePatternCont_ != null)
        {
            sample = failureTimePatternCont_.sample();
            return Math.abs(sample);
        }
        else if (failureTimePatternVariate_ != null)
        {
            sample = failureTimePatternVariate_.gen();
            return Math.abs(sample);
        }
        else
            return -1;
    }


    /**
     * This function returns the following sample for the time
     * when resources will fail.
     * We take into account whether the pattern for this value is
     * discrete or continuous.
     * @return the sample for the time when resources will fail
     */
    protected double getNextFailureLengthSample() {
        double sample;
        if (failureLengthPatternDiscrete_ != null)
        {
            sample = failureLengthPatternDiscrete_.sample();
            return Math.abs(sample);
        }
        else if (failureLengthPatternCont_ != null)
        {
            sample = failureLengthPatternCont_.sample();
            return Math.abs(sample);

        }
        else if (failureLengthPatternVariate_ != null)
        {
            sample = failureLengthPatternVariate_.gen();
            return Math.abs(sample);
        }
        else
            return -1;
    }


    /**
     * This function returns the following sample for the number of
     * machines which will fail in a resource.
     * We reuse the same pattern used for the number of resources which
     * will fail.
     * We take into account whether the pattern for this value is
     * discrete or continuous.
     * @return the sample for the number of machines which will fail
     * in a resource
     */
    protected double getNextNumMachinesFailedSample() {
        return getNextFailureNumResSample();

    }

    /**
     * Gets a ResourceCharacteristics object for a given GridResource ID. <br>
     * NOTE: This method returns a reference of ResourceCharacteristics object,
     *       NOT a copy of it. As a result, some of ResourceCharacteristics
     *       attributes, such as {@link gridsim.PE} availability might
     *       change over time.
     *       Use {@link gridsim.GridSim#getNumFreePE(int)}
     *       to determine number of free PE at the time of a request instead.
     *
     * This function is copied from {@link gridsim.GridSim} class.
     *
     * @param resourceID   the resource ID
     * @return An object of ResourceCharacteristics or <tt>null</tt> if a
     *         GridResource doesn't exist or an error occurs
     * @see gridsim.ResourceCharacteristics
     * @pre resourceID > 0
     * @post $none
     */
    private ResourceCharacteristics getResourceCharacteristics(int resourceID)
    {
        // Get Resource Characteristic Info: Send Request and Receive Event/Msg
        send(super.output, 0.0, GridSimTags.RESOURCE_CHARACTERISTICS,
             new IO_data( new Integer(super.get_id()), 12, resourceID)
        );

        try
        {
            // waiting for a response from system GIS
            Sim_type_p tag=new Sim_type_p(GridSimTags.RESOURCE_CHARACTERISTICS);

            // only look for this type of ack
            Sim_event ev = new Sim_event();
            super.sim_get_next(tag, ev);
            return (ResourceCharacteristics) ev.get_data();
        }
        catch (Exception e) {
            System.out.println(super.get_name() +
                    ".getResourceCharacteristics(): Exception error.");
        }

        return null;
    }

    /**
     * This function is to poll the resources in order to check whether
     * they are failed or not
     * @return true if everything works well
     */
    protected boolean pollResource(int resID)
    {
        if (resID < 0)
        {
            System.out.println(super.get_name() + ".pollResource(): Error - " +
                               "invalid entity ID or name.");
            return false;
        }
        AvailabilityInfo resAv = new AvailabilityInfo(resID, super.get_id());

        // send this to the resource
        send(this.output, GridSimTags.SCHEDULE_NOW,
             GridSimTags.GRIDRESOURCE_FAILURE_INFO,
             new IO_data(resAv, Link.DEFAULT_MTU, resID));

        return true;
    }

    /**
     * This function is to receive back the polls from the resources
     * @return a  AvailabilityInfo object
     */
    protected AvailabilityInfo pollReturn()
    {
        Sim_event ev = new Sim_event();

        // waiting for a response from the GridResource
        Sim_type_p tag = new Sim_type_p(GridSimTags.GRIDRESOURCE_FAILURE_INFO);

        // only look for this type of ack
        super.sim_get_next(tag, ev);

        AvailabilityInfo resAv = null;
        try
        {
            resAv = (AvailabilityInfo) ev.get_data();
        }
        catch (Sim_exception sim)
        {
            System.out.print(super.get_name() + ".pollReturn(): Error - ");
            System.out.println("exception occurs. See the below message:");
            System.out.println(sim.getMessage());
            resAv = null;
        }
        catch (Exception e)
        {
            System.out.print(super.get_name() + ".pollReturn(): Error - ");
            System.out.println("exception occurs. See the below message:");
            System.out.println(e.getMessage());
            resAv = null;
        }

        return resAv;
    }

} 
