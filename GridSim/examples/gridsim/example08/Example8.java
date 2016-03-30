package gridsim.example08;

/*
 * Author Anthony Sulistio
 * Date: December 2003
 * Description: A simple program to demonstrate of how to create your own
 *              allocation policy.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: Example8.java,v 1.3 2004/05/29 05:53:28 anthony Exp $
 */

import java.util.*;
import gridsim.*;


/**
 * Example8 class
 */
class Example8 extends GridSim
{
    private Integer ID_;
    private String name_;
    private GridletList list_;
    private GridletList receiveList_;
    private int totalResource_;


    /**
     * Allocates a new Example8 object
     * @param name  the Entity name of this object
     * @param baud_rate     the communication speed
     * @param total_resource    the number of grid resources available
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    Example8(String name, double baud_rate, int total_resource, int numGridlet)
            throws Exception
    {
        super(name, baud_rate);
        this.name_ = name;
        this.totalResource_ = total_resource;
        this.receiveList_ = new GridletList();

        // Gets an ID for this entity
        this.ID_ = new Integer( getEntityId(name) );
        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.ID_);

        // Creates a list of Gridlets or Tasks for this grid user
        this.list_ = createGridlet(this.ID_.intValue(), numGridlet);
        System.out.println(name + ":Creating "+ this.list_.size() +
                " Gridlets");
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        int resourceID[] = new int[this.totalResource_];
        double resourceCost[] = new double[this.totalResource_];
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

        // a loop to get all the resources available
        int i = 0;
        for (i = 0; i < this.totalResource_; i++)
        {
            // Resource list contains list of resource IDs not grid resource
            // objects.
            resourceID[i] = ( (Integer)resList.get(i) ).intValue();

            // Requests to resource entity to send its characteristics
            send(resourceID[i], GridSimTags.SCHEDULE_NOW,
                    GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);

            // waiting to get a resource characteristics
            resChar = (ResourceCharacteristics) receiveEventObject();
            resourceName[i] = resChar.getResourceName();
            resourceCost[i] = resChar.getCostPerSec();

            System.out.println(this.name_ +
                    ":Received ResourceCharacteristics from " +
                    resourceName[i] + ", with id = " + resourceID[i]);
        }

        /////////////////////////////////////////////////////
        // SUBMITS Gridlets

        Gridlet gridlet = null;
        String info;

        // a loop to get one Gridlet at one time and sends it to a random grid
        // resource entity. Then waits for a reply
        int id = 0;
        boolean success = false;

        for (i = 0; i < this.list_.size(); i++)
        {
            gridlet = (Gridlet) this.list_.get(i);
            info = "Gridlet_" + gridlet.getGridletID();

            System.out.println(this.name_ + ":Sending " + info + " to " +
                    resourceName[id] + " with id = " + resourceID[id] +
                    " at time = " + GridSim.clock() );

            // Sends one Gridlet to a grid resource specified in "resourceID"
            // Gridlets with even numbers are sent and required an ack
            if (i % 2 == 0)
            {
                success = gridletSubmit(gridlet, resourceID[id], 0.0, true);
                System.out.println("Ack = " + success);
                System.out.println();
            }

            // Gridlets with odd numbers are sent but not required an ack
            else {
                success = gridletSubmit(gridlet, resourceID[id], 0.0, false);
            }
        }

        //////////////////////////////////////////////////
        // RECEIVES Gridlets

        super.gridSimHold(20);
        System.out.println("<<<<<< pauses for 20 seconds >>>>>>>>");

        // A loop to receive all the Gridlets back
        for (i = 0; i < this.list_.size(); i++)
        {
            // waiting to receive a Gridlet back from resource entity
            gridlet = (Gridlet) super.receiveEventObject();

            System.out.println(this.name_ + ":Receiving Gridlet " +
                    gridlet.getGridletID() );

            // stores the received Gridlet into a new GridletList object
            this.receiveList_.add(gridlet);
        }

        // shut down this simulation
        shutdownUserEntity();
        terminateIOEntities();
        System.out.println(this.name_ + ":%%%% Exiting body()");
    }

    /**
     * Gets the list of Gridlets
     * @return a list of Gridlets
     */
    public GridletList getGridletList() {
        return this.receiveList_;
    }

    /**
     * This method will show you how to create Gridlets with and without
     * GridSimRandom class.
     * @param userID    the user entity ID that owns these Gridlets
     * @return a GridletList object
     */
    private GridletList createGridlet(int userID, int numGridlet)
    {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();

        int data[] = { 900, 600, 200, 300, 400, 500, 600 };
        int size = 0;

        if (numGridlet >= data.length) {
            size = 6;
        }
        else {
            size = numGridlet;
        }

        for (int i = 0; i < size; i++)
        {
            Gridlet gl = new Gridlet(i, data[i], data[i], data[i]);
            gl.setUserID(userID);
            list.add(gl);
        }

        return list;
    }


    ////////////////////// STATIC METHODS //////////////////////////////

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Example8");

        try
        {
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = true;  // mean trace GridSim events

            // list of files or processing names to be excluded from any
            // statistical measures
            String[] exclude_from_file = { "" };
            String[] exclude_from_processing = { "" };

            // the name of a report file to be written. We don't want to write
            // anything here. See other examples of using the ReportWriter
            // class
            String report_name = null;

            // Initialize the GridSim package
            GridSim.init(num_user, calendar, trace_flag, exclude_from_file,
                    exclude_from_processing, report_name);

            // Second step: Creates one or more GridResource objects
            NewPolicy test = new NewPolicy("GridResource_0", "NewPolicy");
            GridResource resTest = createGridResource("GridResource_0", test);

            // Third step: Creates grid users
            int total_resource = 1;
            int numGridlet = 4;
            double bandwidth = 1000.00;
            Example8 user0 = new Example8("User_0", bandwidth, total_resource,
                                          numGridlet);

            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();

            // Final step: Prints the Gridlets when simulation is over
            GridletList newList = null;
            newList = user0.getGridletList();
            printGridletList(newList, "User_0");
            System.out.println("Finish Example8");
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
    private static GridResource createGridResource(String name, AllocPolicy obj)
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

        // 3. Repeat the process from 2 if we want to create more Machines
        //    In this example, the AIST in Japan has 3 Machines with same
        //    MIPS Rating but different PEs.
        // NOTE: if you only want to create one Machine for one Grid resource,
        //       then you could skip this step.
        mList.add( new Machine(1, 4, mipsRating));   // Second Machine
        mList.add( new Machine(2, 2, mipsRating));   // Third Machine

        // 4. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 9.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList,
                //ResourceCharacteristics.OTHER_POLICY_SAME_RATING,
                ResourceCharacteristics.SPACE_SHARED,
                time_zone, cost);

        // 5. Finally, we need to create a GridResource object.
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
        GridResource gridRes = null;
        try
        {
            ResourceCalendar resCalendar = new ResourceCalendar(time_zone,
                peakLoad, offPeakLoad, holidayLoad, Weekends,
                Holidays, seed);

            gridRes = new GridResource(name, baud_rate, resConfig,
                                       resCalendar, obj);
        }
        catch (Exception e) {
            System.out.println("msg = " + e.getMessage() );
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return gridRes;
    }


    /**
     * Prints the Gridlet objects
     * @param list  list of Gridlets
     */
    private static void printGridletList(GridletList list, String name)
    {
        int size = list.size();
        Gridlet gridlet = null;

        String indent = "    ";
        System.out.println();
        System.out.println("============= OUTPUT for " + name + " ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost");

        // a loop to print the overall result
        int i = 0;
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            System.out.print( gridlet.getGridletStatusString() );

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }

        // a loop to print each Gridlet's history
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.println( gridlet.getGridletHistory() );

            System.out.print("Gridlet #" + gridlet.getGridletID() );
            System.out.println(", length = " + gridlet.getGridletLength()
                    + ", finished so far = " +
                    gridlet.getGridletFinishedSoFar() );
            System.out.println("===========================================\n");
        }
    }

} // end class

