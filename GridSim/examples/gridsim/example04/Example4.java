package gridsim.example04;

/*
 * Author Anthony Sulistio
 * Date: April 2003
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              This example shows how a grid user submits its Gridlets or
 *              tasks to one grid resource entity.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: Example4.java,v 1.10 2005/09/19 06:04:21 anthony Exp $
 */

import java.util.*;
import gridsim.*;

/**
 * Example4 class creates Gridlets and sends them to a grid resource entity
 */
class Example4 extends GridSim
{
    private Integer ID_;
    private String name_;
    private GridletList list_;
    private GridletList receiveList_;


    /**
     * Allocates a new Example4 object
     * @param name  the Entity name of this object
     * @param baud_rate  the communication speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    Example4(String name, double baud_rate) throws Exception
    {
        super(name, baud_rate);
        this.name_ = name;
        this.receiveList_ = new GridletList();

        // Gets an ID for this entity
        this.ID_ = new Integer( getEntityId(name) );
        System.out.println("Creating a grid user entity with name = " +
                name + ", and id = " + this.ID_);

        // Creates a list of Gridlets or Tasks for this grid user
        this.list_ = createGridlet( this.ID_.intValue() );
        System.out.println("Creating " + this.list_.size() + " Gridlets");
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        int resourceID = 0;
        String resourceName;
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
            resList = super.getGridResourceList();
            if (resList.size() > 0)
            {
                // in this example, we know that we only create one resource.
                // Resource list contains list of resource IDs not grid
                // resource objects.
                Integer num = (Integer) resList.get(0);
                resourceID = num.intValue();

                // Requests to resource entity to send its characteristics
                super.send(resourceID, GridSimTags.SCHEDULE_NOW,
                           GridSimTags.RESOURCE_CHARACTERISTICS, this.ID_);

                // waiting to get a resource characteristics
                resChar = (ResourceCharacteristics) super.receiveEventObject();
                resourceName = resChar.getResourceName();

                System.out.println("Received ResourceCharacteristics from " +
                        resourceName + ", with id = " + resourceID);

                // record this event into "stat.txt" file
                super.recordStatistics("\"Received ResourceCharacteristics " +
                        "from " + resourceName + "\"", "");

                break;
            }
            else
                System.out.println("Waiting to get list of resources ...");
        }

        Gridlet gridlet;
        String info;

        // a loop to get one Gridlet at one time and sends it to a grid
        // resource entity. Then waits for a reply
        for (int i = 0; i < this.list_.size(); i++)
        {
            gridlet = (Gridlet) this.list_.get(i);
            info = "Gridlet_" + gridlet.getGridletID();

            System.out.println("Sending " + info + " to " + resourceName +
                    " with id = " + resourceID);

            // Sends one Gridlet to a grid resource specified in "resourceID"
            super.gridletSubmit(gridlet, resourceID);

            // OR another approach to send a gridlet to a grid resource entity
            //super.send(resourceID, GridSimTags.SCHEDULE_NOW,
            //      GridSimTags.GRIDLET_SUBMIT, gridlet);

            // Recods this event into "stat.txt" file for statistical purposes
            super.recordStatistics("\"Submit " + info + " to " + resourceName +
                    "\"", "");

            // waiting to receive a Gridlet back from resource entity
            gridlet = super.gridletReceive();
            System.out.println("Receiving Gridlet " + gridlet.getGridletID());

            // Recods this event into "GridSim_stat.txt" file for statistical
            // purposes
            super.recordStatistics("\"Received " + info +  " from " +
                    resourceName + "\"", gridlet.getProcessingCost());

            // stores the received Gridlet into a new GridletList object
            this.receiveList_.add(gridlet);
        }

        // shut down all the entities, including GridStatistics entity since
        // we used it to record certain events.
        super.shutdownGridStatisticsEntity();
        super.shutdownUserEntity();
        super.terminateIOEntities();
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
    private GridletList createGridlet(int userID)
    {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();

        // We create three Gridlets or jobs/tasks manually without the help
        // of GridSimRandom
        int id = 0;
        double length = 3500.0;
        long file_size = 300;
        long output_size = 300;
        Gridlet gridlet1 = new Gridlet(id, length, file_size, output_size);
        id++;
        Gridlet gridlet2 = new Gridlet(id, 5000, 500, 500);
        id++;
        Gridlet gridlet3 = new Gridlet(id, 9000, 900, 900);

        // setting the owner of these Gridlets
        gridlet1.setUserID(userID);
        gridlet2.setUserID(userID);
        gridlet3.setUserID(userID);

        // Store the Gridlets into a list
        list.add(gridlet1);
        list.add(gridlet2);
        list.add(gridlet3);

        // We create 5 Gridlets with the help of GridSimRandom and
        // GriSimStandardPE class
        long seed = 11L*13*17*19*23+1;
        Random random = new Random(seed);

        // sets the PE MIPS Rating
        GridSimStandardPE.setRating(100);

        // creates 5 Gridlets
        int count = 5;
        for (int i = 1; i < count+1; i++)
        {
            // the Gridlet length determines from random values and the
            // current MIPS Rating for a PE
            length = GridSimStandardPE.toMIs(random.nextDouble()*50);

            // determines the Gridlet file size that varies within the range
            // 100 + (10% to 40%)
            file_size = (long) GridSimRandom.real(100, 0.10, 0.40,
                                    random.nextDouble());

            // determines the Gridlet output size that varies within the range
            // 250 + (10% to 50%)
            output_size = (long) GridSimRandom.real(250, 0.10, 0.50,
                                    random.nextDouble());

            // creates a new Gridlet object
            Gridlet gridlet = new Gridlet(id + i, length, file_size,
                                    output_size);

            gridlet.setUserID(userID);

            // add the Gridlet into a list
            list.add(gridlet);
        }

        return list;
    }


    ////////////////////////// STATIC METHODS ///////////////////////

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Example4");

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
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag, exclude_from_file,
                    exclude_from_processing, report_name);

            // Second step: Creates one GridResource object
            String name = "Resource_0";
            GridResource resource = createGridResource(name);

            // Third step: Creates the Example4 object
            Example4 obj = new Example4("Example4", 560.00);

            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();

            // Final step: Prints the Gridlets when simulation is over
            GridletList newList = obj.getGridletList();
            printGridletList(newList);

            System.out.println("Finish Example4");
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
    private static GridResource createGridResource(String name)
    {
        System.out.println();
        System.out.println("Starting to create one Grid resource with " +
                "3 Machines");

        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        System.out.println("Creates a Machine list");

        // 2. Create one Machine with its id, number of PEs and MIPS rating per PE
        //    In this example, we are using a resource from
        //    hpc420.hpcc.jp, AIST, Tokyo, Japan
        //    Note: these data are taken the from GridSim paper, page 25.
        //          In this example, all PEs has the same MIPS (Millions
        //          Instruction Per Second) Rating for a Machine.
        int mipsRating = 377;
        mList.add( new Machine(0, 4, mipsRating));   // First Machine
        System.out.println("Creates the 1st Machine that has 4 PEs and " +
                "stores it into the Machine list");

        // 3. Repeat the process from 2 if we want to create more Machines
        //    In this example, the AIST in Japan has 3 Machines with same
        //    MIPS Rating but different PEs.
        // NOTE: if you only want to create one Machine for one Grid resource,
        //       then you could skip this step.
        mList.add( new Machine(1, 4, mipsRating));   // Second Machine
        System.out.println("Creates the 2nd Machine that has 4 PEs and " +
                "stores it into the Machine list");

        mList.add( new Machine(2, 2, mipsRating));   // Third Machine
        System.out.println("Creates the 3rd Machine that has 2 PEs and " +
                "stores it into the Machine list");

        // 4. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 9.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.TIME_SHARED,
                time_zone, cost);

        System.out.println("Creates the properties of a Grid resource and " +
                "stores the Machine list");

        // 5. Finally, we need to create a GridResource object.
        double baud_rate = 100.0;           // communication speed
        long seed = 11L*13*17*19*23+1;
        double peakLoad = 0.0;        // the resource load during peak hour
        double offPeakLoad = 0.0;     // the resource load during off-peak hr
        double holidayLoad = 0.0;     // the resource load during holiday

        // incorporates weekends so the grid resource is on 7 days a week
        LinkedList Weekends = new LinkedList();
        Weekends.add(new Integer(Calendar.SATURDAY));
        Weekends.add(new Integer(Calendar.SUNDAY));

        // incorporates holidays. However, no holidays are set in this example
        LinkedList Holidays = new LinkedList();
        GridResource gridRes = null;
        try
        {
            gridRes = new GridResource(name, baud_rate, seed,
                resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends,
                Holidays);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finally, creates one Grid resource and stores " +
                "the properties of a Grid resource");
        System.out.println();

        return gridRes;
    }

    /**
     * Prints the Gridlet objects
     * @param list  list of Gridlets
     */
    private static void printGridletList(GridletList list)
    {
        int size = list.size();
        Gridlet gridlet;

        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost");

        for (int i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            if (gridlet.getGridletStatus() == Gridlet.SUCCESS)
                System.out.print("SUCCESS");

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }
    }

} // end class

