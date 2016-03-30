package gridsim.example10;

/*
 * Author Anthony Sulistio
 * Date: May 2004 
 * Description: A simple program to demonstrate of how to use basic
 *              advanced reservation functionalities, such as create, commit
 *              and status.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: ARTest.java,v 1.3 2007/08/10 02:35:40 anthony Exp $
 */

import java.util.*;
import gridsim.*;
import eduni.simjava.*;

/**
 * A user entity that reserves a resource in advance.
 * In this example, only explore some functionalities, such as:
 * - requests a new advanced reservation
 * - requests a new immediate reservation. Immediate reservation means use
 *   current time as the starting time
 * - commits an accepted reservation
 * - checks the status of a reservation
 */
public class ARTest extends AdvanceReservation
{
    private GridletList list_;          // a list containing new Gridlets
    private GridletList receiveList_;   // a list containing completed Gridlets
    private int failReservation_;    // number of reservations failed

    // time constant values
    private final int SEC = 1;           // 1 second
    private final int MIN = 60 * SEC;    // 1 min in seconds
    private final int HOUR = 60 * MIN;   // 1 hour in minutes
    private final int DAY = 24 * HOUR;   // 1 day in hours
        

    /**
     * Creates a new grid user entity
     * @param name          the Entity name of this object
     * @param baud_rate     the communication speed
     * @param timeZone      user's local time zone
     * @param totalJob      number of Gridlets to be created
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     */
    public ARTest(String name, double baud_rate, double timeZone, 
                  int totalJob) throws Exception
    {
        super(name, baud_rate, timeZone);
        this.receiveList_ = new GridletList();
        this.failReservation_ = 0;

        // create Gridlets
        list_ = createGridlet( totalJob, super.get_id() );

        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + super.get_id());
        System.out.println(name + ": Creating " + totalJob + " Gridlets.");
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        LinkedList resList;

        // waiting to get list of resources. Since GridSim package uses
        // multi-threaded environment, your request might arrive earlier
        // before one or more grid resource entities manage to register
        // themselves to GridInformationService (GIS) entity.
        // Therefore, it's better to wait in the first place
        while (true)
        {
            // need to pause for a while to wait GridResources finish
            // registering to GIS
            super.gridSimHold(2*SEC);    // wait for 2 seconds

            resList = getGridResourceList();
            if (resList.size() > 0) {
                break;
            }
            else {
                System.out.println(super.get_name() +
                        ": Waiting to get list of resources ...");
            }
        }

        // list of resource IDs that can support Advanced Reservation (AR)
        ArrayList resARList = new ArrayList();   
        
        // list of resource names that can support AR
        ArrayList resNameList = new ArrayList(); 
        
        int totalPE = 0;
        int i = 0;
        Integer intObj = null;   // resource ID
        String name;             // resource name

        // a loop that gets a list of resources that can support AR
        for (i = 0; i < resList.size(); i++)
        {
            intObj = (Integer) resList.get(i);

            // double check whether a resource supports AR or not.
            // In this example, all resources support AR.
            if (GridSim.resourceSupportAR(intObj) == true) 
            {
                // gets the name of a resource
                name = GridSim.getEntityName( intObj.intValue() );

                // gets the total number of PEs owned by a resource
                totalPE = super.getNumPE(intObj);

                // adds the resource ID, name and its total PE into 
                // their respective lists.
                resARList.add(intObj);
                resNameList.add(name);
            }
        }

        //----------------------------------------------------
        // sends one or more reservations to a gridresource entity
        sendReservation(resARList, resNameList);
        
        try
        {
            // then get the results or Gridlets back
            int size = list_.size() - failReservation_;
            for (i = 0; i < size; i++)
            {
                Gridlet gl = super.gridletReceive();
                this.receiveList_.add(gl);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // shut down all the entities 
        shutdownGridStatisticsEntity();
        terminateIOEntities();
        shutdownUserEntity();
        System.out.println(super.get_name() + ":%%%% Exiting body() with " +
                "number of failed reservation is " + failReservation_);
    }
    
    /**
     * Creates a new reservation and send it to a resource.
     * One reservation only reserves 1 PE. At the moment, GridSim is only
     * able to execute 1 Gridlet on a single PE.
     */
    private void sendReservation(ArrayList resARList, ArrayList resNameList)
    {
        //  total reservation made. 1 reservation reserves 1 PE.
        int totalPE = 1;
        int totalReservation = list_.size();   // total number of Gridlets
        
        // gets the init simulation time
        Calendar cal = GridSim.getSimulationCalendar();

        // wants to reserve 1 day after the init simulation time
        int MILLI_SEC = 1000;
        long time = cal.getTimeInMillis() + (1 * DAY * MILLI_SEC);
        
        // each reservation requires around 10 minutes
        int duration = 10 * MIN;  
                
        String result = null;
        Gridlet gl = null;
        int val = 0;
        int resID = 0;   // a resource ID
        int totalResource = resARList.size();   // total AR resource available
        String resName = null;
        
        Random randObj = new Random(time);
        
        for (int i = 0; i < totalReservation; i++)
        {
            duration += 5 * MIN;   // duration of a reservation
                
            // gets the resource ID and name
            val = randObj.nextInt(totalResource);
            resID = ( (Integer) resARList.get(val) ).intValue(); 
            resName = (String) resNameList.get(val);
                    
            // try immediate reservation, where starting time is 0 meaning
            // use current time as the start time
            if (val == i) {
                time = 0;   // immediate reservation
            }
            else {
                time = cal.getTimeInMillis() + (1*DAY*MILLI_SEC) 
                            + (duration*MILLI_SEC);
            }
                
            // creates a new or immediate reservation
            result = super.createReservation(time, duration, totalPE, resID);
            System.out.println(super.get_name() + ": reservation result from "+ 
                    resName + " = " + result + " at time = " + GridSim.clock());

            // queries the status of this reservation
            val = super.queryReservation(result);
            System.out.println(super.get_name() + ": query result = " + 
                    AdvanceReservation.getQueryResult(val) );
            
            // if a reservation fails, then continue the next Gridlet
            if (val == GridSimTags.AR_STATUS_ERROR ||
                val == GridSimTags.AR_STATUS_ERROR_INVALID_BOOKING_ID)
            {
                failReservation_++;
                System.out.println("==========================");
                continue;
            }

            // for a reservation with an even number, commits straightaway
            // without sending any Gridlets yet
            if (i % 2 == 0)
            {
                val = super.commitReservation(result);
                System.out.println(super.get_name() + 
                        ": commit only with result = " + 
                        AdvanceReservation.getCommitResult(val) );
            }
            
            // a reservation only needs to reserve 1 PE.
            gl = (Gridlet) list_.get(i);
            val = super.commitReservation(result, gl);
            System.out.println(super.get_name() + ": commit result = " + 
                    AdvanceReservation.getCommitResult(val) );

            // queries the status of this reservation
            val = super.queryReservation(result);
            System.out.println(super.get_name() + ": query result = " + 
                    AdvanceReservation.getQueryResult(val) );
            System.out.println("==========================");
        }
    }
       
    /**
     * Gets the list of Gridlets
     * @return a list of Gridlets
     */
    public GridletList getGridletList() {
        return this.receiveList_;
    }

    /**
     * A Grid user has many Gridlets or jobs to be processed.
     * This method will show you how to create Gridlets with and without
     * GridSimRandom class.
     * @return a GridletList object 
     */
    private GridletList createGridlet(int size, int userID)
    {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();
        int length = 5000;
        for (int i = 0; i < size; i++)
        {
            // creates a new Gridlet object
            Gridlet gridlet = new Gridlet(i, length, 1000, 5000);
            
            // add the Gridlet into a list
            list.add(gridlet);
            gridlet.setUserID(userID);
        }

        return list;
    }

} // end class
 
