/*
 * Author Marcos Dias de Assuncao
 * Date: May 2009
 * 
 * Description: A simple program to demonstrate of how to use basic
 *              advanced reservation functionalities, such as create, commit,
 *              get status of advance reservations and submit gridlets.
 */

package parallel.reservation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.parallel.profile.TimeSlot;
import gridsim.parallel.reservation.Reservation;
import gridsim.parallel.reservation.ReservationRequester;
import gridsim.parallel.reservation.ReservationStatus;

/**
 * A user entity that reserves a resource in advance.
 * In this example, only explore some functionalities, such as:
 * - requests a new advance reservation
 * - requests a new immediate reservation. Immediate reservation means use
 *   current time as the starting time
 * - commits an accepted reservation
 * - checks the status of a reservation
 * - submits gridlets to the resource
 * 
 * @since 5.0
 */
public class ARUser extends ReservationRequester {
    private GridletList jobs;          // a list containing new Gridlets
    private GridletList receiveJobs;   // a list containing completed Gridlets
    private int failReservation;       // number of reservations failed

    // time constant values
    private static final int SEC = 1;           // 1 second
    private static final int MIN = 60 * SEC;    // 1 min in seconds
    private static final int HOUR = 60 * MIN;   // 1 hour in minutes
    private static final int DAY = 24 * HOUR;   // 1 day in hours

    /**
     * Creates a new grid user entity
     * @param name          the Entity name of this object
     * @param baud_rate     the communication speed
     * @param totalJob      number of Gridlets to be created
     * @throws Exception This happens when creating this entity before
     *                   initialising GridSim package or the entity name is
     *                   <code>null</code> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     */
    public ARUser(String name, double baud_rate, int totalJob) throws Exception {
        super(name, baud_rate);
        this.receiveJobs = new GridletList();
        this.failReservation = 0;

        // create Gridlets
        jobs = createGridlet( totalJob, super.get_id() );

        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + super.get_id());
        System.out.println(name + ": Creating " + totalJob + " Gridlets.");
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {
        LinkedList resList;

        // waiting to get list of resources. Since GridSim package uses
        // multi-threaded environment, your request might arrive earlier
        // before one or more grid resource entities manage to register
        // themselves to GridInformationService (GIS) entity.
        // Therefore, it's better to wait in the first place
        
        while (true)  {
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
        ArrayList<Integer> resARList = new ArrayList<Integer>();   
        
        // list of resource names that can support AR
        ArrayList<String> resNameList = new ArrayList<String>(); 
        
        int i = 0;
        Integer intObj = null;   // resource ID
        String name = null;             // resource name

        // a loop that gets a list of resources that can support AR
        for (i = 0; i < resList.size(); i++) {
            intObj = (Integer) resList.get(i);

            // double check whether a resource supports AR or not.
            // In this example, all resources support AR.
            if (GridSim.resourceSupportAR(intObj)) {
                // gets the name of a resource
                name = GridSim.getEntityName( intObj.intValue() );

                // adds the resource ID, name and its total PE into 
                // their respective lists.
                resARList.add(intObj);
                resNameList.add(name);
            }
        }

        //----------------------------------------------------
        // sends one or more reservations to a grid resource entity
        sendReservation(resARList, resNameList);
        
        try {
            // then get the results or Gridlets back
            int size = jobs.size() - failReservation;
            for (i = 0; i < size; i++) {
                Gridlet gl = super.gridletReceive();
                this.receiveJobs.add(gl);
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
                "number of failed reservation is " + failReservation);
    }
    
    /**
     * Gets the list of Gridlets
     * @return a list of Gridlets
     */
    public GridletList getGridletList() {
        return receiveJobs;
    }
    
    /*
     * Creates a new reservation and send it to a resource.
     * One reservation only reserves 1 PE. At the moment, GridSim is only
     * able to execute 1 Gridlet on a single PE.
     */
    private void sendReservation(ArrayList resARList, ArrayList resNameList) {
        //  total reservation made.
        int totalPE = 3;
        int totalReservation = jobs.size() + 2;   // total number of Gridlets
        
        // wants to reserve 1 hour after the init simulation time
        long time = 1 * HOUR;
        
        // each reservation requires around 10 minutes
        int duration = 10 * MIN;  
                
        int val = 0;
        int resID = 0;   // a resource ID
        int totalResource = resARList.size();   // total AR resource available
        String resName = null;
        
        Random randObj = new Random(time);
        
        for (int i = 0; i < totalReservation; i++) {
            duration += 5 * MIN;   // duration of a reservation
                
            // gets the resource ID and name
            val = randObj.nextInt(totalResource);
            resID = ( (Integer) resARList.get(val) ).intValue(); 
            resName = (String) resNameList.get(val);

            // queries the availability of the Grid resource
            Collection<TimeSlot> availability = 
            	super.queryFreeTime(GridSim.clock(), Integer.MAX_VALUE, resID);
            
            System.out.println("Availability information returned by the " +
            		"Grid resource # " + resID + " at time # " + GridSim.clock() + 
            		" is as follows: \n " + availability);
            
            // try immediate reservation, where starting time is 0 meaning
            // use current time as the start time
            if (val == i) {
                time = 0;   // immediate reservation
            }
            else {
                time = 1 * HOUR + duration;
            }
            
            // creates a new reservation
            Reservation rsv = null;
            boolean success = false;
            
            rsv = super.createReservation(time, duration, totalPE, resID);
            if(rsv != null && rsv.getStatus() != ReservationStatus.FAILED) {
	            System.out.println(super.get_name() + ": reservation has been accepted by "+ 
	                    resName + " at time = " + GridSim.clock());
	            success = true;
            }
            else {
            	System.out.println(super.get_name() + ": reservation has not been accepted by "+ 
	                    resName + " at time = " + GridSim.clock());
                failReservation++;
                success = false;
            }

            // confirm the reservation
            if(success) {
            	
            	// query the status of the reservation
            	ReservationStatus status = super.queryReservation(rsv.getID());
            	System.out.println("The status of reservation # " + rsv.getID() +
            			" is # " + status.getDescription());
            	
	            // for a reservation with an even number, commits straight away
	            // without sending any jobs yet
	            success = super.commitReservation(rsv.getID());
	            
	            if(success) {
	                System.out.println("Reservation # "+ rsv.getID() + 
	                		" has been confirmed successfully.");
	            }
	            else {
	                System.out.println("Reservation # "+ rsv.getID() + 
	                	" has NOT been confirmed successfully.");
	            }

            	// query the status of the reservation again
            	status = super.queryReservation(rsv.getID());
            	System.out.println("The status of reservation # " + rsv.getID() +
            			" is # " + status.getDescription());
            }
            
            if(success && i<jobs.size()) {
	            // then sends a gridlet to use the reservation
	            Gridlet grl = this.jobs.get(i);
	            grl.setReservationID(rsv.getID());
	            super.gridletSubmit(grl, resID);
            }
        }
    }

    /*
     * A Grid user has many Gridlets or jobs to be processed.
     * This method will show you how to create Gridlets with and without
     * GridSimRandom class.
     * @return a GridletList object 
     */
    private static GridletList createGridlet(int size, int userID) {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();
        int length = 500000;
        for (int i = 0; i < size; i++) {
            // creates a new Gridlet object
            Gridlet gridlet = new Gridlet(i, length, 1000, 5000);
            
            
            // add the Gridlet into a list
            list.add(gridlet);
            gridlet.setUserID(userID);
        }

        return list;
    }
} 
 
