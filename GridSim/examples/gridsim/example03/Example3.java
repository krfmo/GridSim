package gridsim.example03;

/*
 * Author Anthony Sulistio
 * Date: April 2003
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              this example shows how two GridSim entities interact with each
 *              other.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: Example3.java,v 1.6 2005/09/16 07:02:15 anthony Exp $
 */

import java.util.*;
import gridsim.*;


/**
 * Example3 class creates Gridlets and sends them to the other GridSim
 * entities, i.e. Test class.
 */
class Example3 extends GridSim
{
    private String entityName_;
    private GridletList list_;

    // Gridlet lists received from Test object
    private GridletList receiveList_;


    /**
     * Allocates a new Example3 object
     * @param name  the Entity name
     * @param baud_rate     the communication speed
     * @param list  a list of Gridlets
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or the entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#Init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    Example3(String name, double baud_rate, GridletList list) throws Exception
    {
        super(name);
        this.list_ = list;
        receiveList_ = new GridletList();

        // creates a Test entity, and refer it as "entityName"
        entityName_ = "Test";
        new Test(entityName_, baud_rate);
    }


    /**
     * The core method that handles communications between GridSim entities.
     */
    public void body()
    {
        int size = list_.size();
        Gridlet obj, gridlet;

        // a loop to get one Gridlet at one time and sends it to other GridSim
        // entity
        for (int i = 0; i < size; i++)
        {
            obj = (Gridlet) list_.get(i);
            System.out.println("Inside Example3.body() => Sending Gridlet " +
                    obj.getGridletID());

            // Sends one Gridlet at the time with no delay (by using
            // GridSimTags.SCHEDULE_NOW constant) to the other GridSim entity
            // specified in "entityName"
            super.send(entityName_, GridSimTags.SCHEDULE_NOW,
                       GridSimTags.GRIDLET_SUBMIT, obj);

            // Receiving a Gridlet back
            gridlet = super.gridletReceive();

            System.out.println("Inside Example3.body() => Receiving Gridlet "+
                    gridlet.getGridletID());

            // stores the received Gridlet into a new GridletList object
            receiveList_.add(gridlet);
        }

        // Signals the end of simulation to "entityName"
        super.send(entityName_, GridSimTags.SCHEDULE_NOW,
                   GridSimTags.END_OF_SIMULATION);
    }


    /**
     * Gets the list of Gridlets
     * @return a list of Gridlets
     */
    public GridletList getGridletList() {
        return receiveList_;
    }


    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Example3");
        System.out.println();

        try
        {
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 0;   // number of users need to be created
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


            // Second step: Creates a list of Gridlets
            GridletList list = createGridlet();
            System.out.println("Creating " + list.size() + " Gridlets");


            // Third step: Creates the Example3 object
            Example3 obj = new Example3("Example3", 560.00, list);


            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();

            // Final step: Prints the Gridlets when simulation is over
            GridletList newList = obj.getGridletList();
            printGridletList(newList);

            System.out.println("Finish Example3");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }


    /**
     * This method will show you how to create Gridlets with and without
     * GridSimRandom class.
     * @return a GridletList object
     */
    private static GridletList createGridlet()
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

            // add the Gridlet into a list
            list.add(gridlet);
        }

        return list;
    }

    /**
     * Prints the Gridlet objects
     * @param list  a list of Gridlets
     */
    private static void printGridletList(GridletList list)
    {
        int size = list.size();
        Gridlet gridlet;

        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Gridlet ID" + indent + "STATUS");

        for (int i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            if (gridlet.getGridletStatus() == Gridlet.SUCCESS)
                System.out.println("SUCCESS");
        }
    }
}

