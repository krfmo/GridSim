package gridsim.example09;

/*
 * Author Anthony Sulistio
 * Date: May 2004
 * Description: 
 *      This example demonstrates how to create and to define your own
 * GridResource and GridInformationService entity.
 * Please read NewGridResource.java and NewGIS.java for more
 * detailed explanations on how to do it.
 *              
 * The scenarios of this example are:
 * - initializing GridSim and creating grid user and resource entities
 * - starting the simulation
 * - a grid resource entity registers new tags to a GridInformationService
 *   entity
 * - a grid user entity sends new tags to a grid resource entity
 * - finally exit the simulation
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 *       GridSim version 3.0 or above is needed to run this example.
 *
 * $Id: Example9.java,v 1.2 2004/05/29 06:53:33 anthony Exp $
 */

import java.util.*;
import gridsim.*;


/**
 * Example9 class. 
 */
class Example9 extends GridSim
{
    // constant variables
    public static final int HELLO = 900;
    public static final int TEST = 901;

    private Integer ID_;     // entity ID of this object
    private String name_;    // entity name of this object
    private int totalResource_;    // number of grid resources avaiable


    /**
     * Allocates a new grid user entity
     * @param name  the Entity name of this object
     * @param baud_rate     the communication speed
     * @param total_resource    the number of grid resources available
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    Example9(String name, double baud_rate, int total_resource)
            throws Exception
    {
        super(name, baud_rate);
        this.name_ = name;
        this.totalResource_ = total_resource;

        // Gets an ID for this entity
        this.ID_ = new Integer( getEntityId(name) );
        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.ID_);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        int resourceID[] = new int[this.totalResource_];
        String resourceName[] = new String[this.totalResource_];

        LinkedList resList;
        ResourceCharacteristics resChar;

        // waiting to get list of resources. Since GridSim package uses
        // multi-threaded environment, your request might arrive earlier
        // before one or more grid resource entities manage to register
        // themselves to GridInformationService (GIS) entity.
        // Therefore, it's better to wait in the first place
        while (true)
        {
            // need to pause for a while to wait GridResources finish
            // registering to GIS
            super.gridSimHold(1.0);    // hold by 1 second

            resList = getGridResourceList();
            if (resList.size() == this.totalResource_)
                break;
            else
            {
                System.out.println(this.name_ +
                        ":Waiting to get list of resources ...");
            }
        }

        int SIZE = 12;   // size of Integer object is roughly 12 bytes
        int i = 0;
        
        // a loop to get all the resources available.
        // Once the resources are known, then send HELLO and TEST tag to each
        // of them.
        for (i = 0; i < this.totalResource_; i++)
        {
            // Resource list contains list of resource IDs not grid resource
            // objects.
            resourceID[i] = ( (Integer) resList.get(i) ).intValue();

            // Requests to resource entity to send its characteristics
            // NOTE: sending directly without using I/O port
            super.send(resourceID[i], GridSimTags.SCHEDULE_NOW,
                       GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);

            // waiting to get a resource characteristics
            resChar = (ResourceCharacteristics) receiveEventObject();
            resourceName[i] = resChar.getResourceName();

            // print that this entity receives a particular resource 
            // characteristics
            System.out.println(this.name_ +
                    ":Received ResourceCharacteristics from " +
                    resourceName[i] + ", with id = " + resourceID[i]);

            // send TEST tag to a resource using I/O port.
            // It will consider transfer time over a network.
            System.out.println(this.name_ + ": Sending TEST tag to " +
                    resourceName[i] + " at time " + GridSim.clock());
            super.send( super.output, GridSimTags.SCHEDULE_NOW, TEST,
                    new IO_data(this.ID_, SIZE, resourceID[i]) );
                    
            // send HELLO tag to a resource using I/O port
            System.out.println(this.name_ + ": Sending HELLO tag to " +
                    resourceName[i] + " at time " + GridSim.clock());
            super.send( super.output, GridSimTags.SCHEDULE_NOW, HELLO,
                    new IO_data(this.ID_, SIZE, resourceID[i]) );
        }

        // need to wait for 10 seconds to allow a resource to process
        // receiving events.
        super.sim_pause(10);
        
        // shut down all the entities, including GridStatistics entity since
        // we used it to record certain events.
        shutdownGridStatisticsEntity();
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(this.name_ + ":%%%% Exiting body()");
    }

    ////////////////////// STATIC METHODS //////////////////////////////

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Example9");

        try
        {
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't r#Iun this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = true;  // true means trace GridSim events

            // Initialize the GridSim package
            // Starting from GridSim 3.0, you can specify different type of
            // initialisation.
            System.out.println("Initializing GridSim package");

            // In this example, initialise GridSim without creating
            // a default GridInformationService (GIS) entity.
            GridSim.init(num_user, calendar, trace_flag, false);

            // Create a new GIS entity    
            NewGIS gis = new NewGIS("NewGIS");

            // You need to call this method before the start of simulation
            GridSim.setGIS(gis);

            // Second step: Creates one or more grid resource entities
            NewGridResource resource0 = createGridResource("Resource_0");
            int total_resource = 1;

            // Third step: Creates one or more grid user entities
            Example9 user0 = new Example9("User_0", 560.00, total_resource);

            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();
            System.out.println("Finish Example9");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

    /**
     * Creates one Grid resource. A Grid resource contains one or more
     * Machines. Similarly, a Machine contains one or more PEs (Processing
     * Elements or CPUs).
     * <p>
     * In this simple example, we are simulating one Grid resource with three
     * Machines that contains one or more PEs.
     * @param name  a Grid Resource name
     * @return a GridResource object
     */
    private static NewGridResource createGridResource(String name)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();

        // 2. Create one Machine with its id, number of PEs and MIPS rating per PE
        //    In this example, we are using a resource from
        //    hpc420.hpcc.jp, AIST, Tokyo, Japan
        //    Note: these data are taken the from GridSim paper, page 25.
        //          In this example, all PEs has the same MIPS (Millions
        //          Instruction Per Second) Rating for a Machine.
        int mipsRating = 377;
        mList.add( new Machine(0, 4, mipsRating));   // First Machine

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 9.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.SPACE_SHARED,
                time_zone, cost);

        // 4. Finally, we need to create a GridResource object.
        double baud_rate = 100.0;           // communication speed
        long seed = 11L*13*17*19*23+1;
        double peakLoad = 0.0;       // the resource load during peak hour
        double offPeakLoad = 0.0;    // the resource load during off-peak hr
        double holidayLoad = 0.0;    // the resource load during holiday

        // incorporates weekends so the grid resource is on 7 days a week
        LinkedList Weekends = new LinkedList();
        Weekends.add(new Integer(Calendar.SATURDAY));
        Weekends.add(new Integer(Calendar.SUNDAY));

        // incorporates holidays. However, no holidays are set in this example
        LinkedList Holidays = new LinkedList();
    
        ResourceCalendar calendar = new  ResourceCalendar(time_zone, peakLoad, 
                offPeakLoad, holidayLoad, Weekends, Holidays, seed);

        NewGridResource gridRes = null;
        try 
        {
            // NOTE: The below code creates a NewGridResource object
            // instead of its parent class.
            gridRes = new NewGridResource(name, baud_rate, resConfig, calendar,
                                null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return gridRes;
    }

} // end class

