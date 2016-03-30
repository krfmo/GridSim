package gridsim.example07;

/*
 * Author Anthony Sulistio
 * Date: Dec 2003
 * $Id: Test.java,v 1.3 2004/05/06 04:31:36 anthony Exp $
 */

import java.util.*;
import gridsim.*;


/**
 * This is the example main program that demonstrates how to
 * submit / cancel / resume / pause / move Gridlets to different GridResources.
 * You can play around with this class by adjusting few parameters in main()
 * such as totalUser, totalGridlet, etc.
 */
public class Test
{
    private static final int MIN = 1;  // min number of test cases
    private static final int MAX = 8;  // max number of test cases

    /**
     * Usage in Unix / Linux:
     *     javac -classpath $GRIDSIM/gridsim.jar:. Test.java
     *     java Test [policy: space | time] [test case number: 1 - 8]
     *
     * For example: java Test space 7 --> running Space-Shared for test case #7
     *              java Test time  3 --> running Time-Shared for test case #3
     *
     * The operation of these Test Cases offer are:
     * Test Case 1: Submit Gridlets - then wait until all Finish to collect
     * Test Case 2: Submit Gridlets - Cancel some of them - Finish
     * Test Case 3: Submit Gridlets - Pause some of them - Cancel - Finish
     * Test Case 4: Submit Gridlets - Pause - Resume - Cancel - Finish
     * Test Case 5: Submit Gridlets - Move some of them - Finish
     * Test Case 6: Submit Gridlets - Pause - Move - Finish
     * Test Case 7: Submit Gridlets - Pause - Resume - Move - Finish
     * Test Case 8: Submit Gridlets - Pause - Resume - Move - Cancel - Finish
     *
     * NOTE:
     * - Test Case 1 is the simplest and Test Case 8 is the most complicated.
     *
     * - These Test Cases are quite flexible, meaning, you can adjust how big
     *   these experiments are by increasing/decreasing totalUser, totalPE, etc
     *   from main() only. You don't need to modify
     *   any of the Test Case classes.
     *
     * - Be careful when setting the numbers too high (above 200)
     *   since you might get Java "Out of Memory" exception.
     *
     * - For an effective experiment for Gridlet or Job migration, you need to
     *   have a large number of GridResource entities, say more than 6.
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Test Cases");
        try
        {
            // Parse the command line args
            int policy = 0;
            if ( args[0].equals("t") || args[0].equals("time") ) {
                policy = ResourceCharacteristics.TIME_SHARED;
            }
            else if ( args[0].equals("s") || args[0].equals("space") ) {
                policy = ResourceCharacteristics.SPACE_SHARED;
            }
            else {
                System.out.println("Error -- Invalid allocation policy....");
                return;
            }

            // determine which test case number to choose
            int testNum = Integer.parseInt(args[1]);
            if (testNum < MIN || testNum > MAX) {
                testNum = MIN;
            }

            ////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // true means tracing GridSim events

            // list of files or processing names to be excluded from any
            // statistical measures
            String[] exclude_from_file = { "" };
            String[] exclude_from_processing = { "" };

            // the name of a report file to be written. We don't want to write
            // anything here.
            String report_name = null;

            // initialize all revelant variables
            double baudRate[] = {1000, 5000}; // bandwidth for even, odd
            int peRating[] = {10, 50};   // PE Rating for even, odd
            double price[] = {3.0, 5.0};   // resource for even, odd
            int gridletLength[] = {1000, 2000, 3000, 4000, 5000};

            // Initialize the GridSim package
            int totalUser = 2;    // total Users for this experiment
            GridSim.init(totalUser, calendar, trace_flag, exclude_from_file,
                    exclude_from_processing, report_name);

            //////////////////////////////////////
            // Second step: Creates one or more GridResource objects
            int totalResource = 3;  // total GridResources for this experiment
            int totalMachine = 1;   // total Machines for each GridResource
            int totalPE = 3;        // total PEs for each Machine
            createResource(totalResource, totalMachine, totalPE, baudRate,
                           peRating, price, policy);

            /////////////////////////////////////
            // Third step: Creates grid users
            int totalGridlet = 4;     // total Gridlets for each User
            createUser(totalUser, totalGridlet, gridletLength, baudRate,
                       testNum);

            ////////////////////////////////////
            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();
        }
        catch (Exception e)
        {
            System.out.println("Unwanted errors happen");
            System.out.println( e.getMessage() );
            System.out.println("Usage: java Test [time | space] [1-8]");
        }
        System.out.println("=============== END OF TEST ====================");
    }

    /**
     * Creates many GridResources
     */
    public static void createResource(int totalRes, int totalMachine,
                             int totalPE, double[] baudRate, int[] peRating,
                             double[] price, int policy)
    {
        double bandwidth = 0;
        double cost = 0.0;

        // a loop that creates one or more GridResources
        for (int i = 0; i < totalRes; i++)
        {
            String name = "GridResource_" + i;
            if (i % 2 == 0)
            {
                bandwidth = baudRate[0];
                cost = price[0];
            }
            else
            {
                bandwidth = baudRate[1];
                cost = price[1];
            }

            // creates a GridResource
            createGridResource(name, totalMachine, totalPE, bandwidth,
                               peRating, policy, cost);
        }
    }

    /**
     * Creates many Grid Users
     */
    public static void createUser(int totalUser, int totalGridlet,
                             int[] glLength, double[] baudRate, int testNum)
    {
        try
        {
            double bandwidth = 0;
            double delay = 0.0;

            for (int i = 0; i < totalUser; i++)
            {
                String name = "User_" + i;
                if (i % 2 == 0) {
                    bandwidth = baudRate[0];
                    delay = 5.0;
                }
                else {
                    bandwidth = baudRate[1];
                }

                // creates a Grid user
                createTestCase(name, bandwidth, delay, totalGridlet, glLength,
                               testNum);
            }
        }
        catch (Exception e) {
            // ... ignore
        }
    }

    /**
     * A selection of different test cases
     */
    private static void createTestCase(String name, double bandwidth,
                             double delay, int totalGridlet, int[] glLength,
                             int testNum) throws Exception
    {
        switch(testNum)
        {
            case 1:
                new TestCase1(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 2:
                new TestCase2(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 3:
                new TestCase3(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 4:
                new TestCase4(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 5:
                new TestCase5(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 6:
                new TestCase6(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 7:
                new TestCase7(name, bandwidth, delay, totalGridlet, glLength);
                break;

            case 8:
                new TestCase8(name, bandwidth, delay, totalGridlet, glLength);
                break;

            default:
                System.out.println("Not a recognized test case.");
                break;
        }
    }

    /**
     * Creates one Grid resource. A Grid resource contains one or more
     * Machines. Similarly, a Machine contains one or more PEs (Processing
     * Elements or CPUs).
     */
    private static void createGridResource(String name, int totalMachine,
                              int totalPE, double bandwidth, int[] peRating,
                              int policy, double cost)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();

        int rating = 0;
        for (int i = 0; i < totalMachine; i++)
        {
            // even Machines have different PE rating compare to odd ones
            if (i % 2 == 0) {
                rating = peRating[0];
            }
            else {
                rating = peRating[1];
            }

            // 2. Create one Machine with its id, number of PEs and rating
            mList.add( new Machine(i, totalPE, rating) );
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 0.0;         // time zone this resource located

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, policy, time_zone, cost);

        // 4. Finally, we need to create a GridResource object.
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
        try
        {
            GridResource gridRes = new GridResource(name, bandwidth, seed,
                resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends,
                Holidays);
        }
        catch (Exception e)
        {
            System.out.println("Error in creating GridResource.");
            System.out.println( e.getMessage() );
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return;
    }

} // end class

