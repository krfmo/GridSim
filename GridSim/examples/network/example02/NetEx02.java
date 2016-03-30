package network.example02;

/*
 * Author: Anthony Sulistio
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim
 *              network extension package.
 *              This example shows how to create user and resource
 *              entities connected via a network topology, using link
 *              and router.
 *
 */

import gridsim.*;
import gridsim.net.*;
import java.util.*;


/**
 * Test Driver class for this example
 */
public class NetEx02
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting network example ...");

        try
        {
            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 2;   // number of grid users
            Calendar calendar = Calendar.getInstance();

            // a flag that denotes whether to trace GridSim events or not.
            boolean trace_flag = true;

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Second step: Creates one or more GridResource entities

            double baud_rate = 1000; // bits/sec
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 1500;          // max. transmission unit in byte
            int i = 0;

            // more resources can be created by
            // setting totalResource to an appropriate value
            int totalResource = 1;
            ArrayList resList = new ArrayList(totalResource);
            for (i = 0; i < totalResource; i++)
            {
                GridResource res = createGridResource("Res_"+i, baud_rate,
                                                      propDelay, mtu);

                // add a resource into a list
                resList.add(res);
            }

            //////////////////////////////////////////
            // Third step: Creates one or more grid user entities

            // number of Gridlets that will be sent to the resource
            int totalGridlet = 5;

            // create users
            ArrayList userList = new ArrayList(num_user);
            for (i = 0; i < num_user; i++)
            {
                // if trace_flag is set to "true", then this experiment will
                // create User_i.csv where i = 0 ... (num_user-1)
                NetUser user = new NetUser("User_"+i, totalGridlet, baud_rate,
                                           propDelay, mtu, trace_flag);

                // add a user into a list
                userList.add(user);
            }

            //////////////////////////////////////////
            // Fourth step: Builds the network topology among entities.

            // In this example, the topology is:
            // user(s) --1Mb/s-- r1 --10Mb/s-- r2 --1Mb/s-- GridResource(s)

            // create the routers.
            // If trace_flag is set to "true", then this experiment will create
            // the following files (apart from sim_trace and sim_report):
            // - router1_report.csv
            // - router2_report.csv
            Router r1 = new RIPRouter("router1", trace_flag);   // router 1
            Router r2 = new RIPRouter("router2", trace_flag);   // router 2

            // connect all user entities with r1 with 1Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            NetUser obj = null;
            for (i = 0; i < userList.size(); i++)
            {
                // A First In First Out Scheduler is being used here.
                // SCFQScheduler can be used for more fairness
                FIFOScheduler userSched = new FIFOScheduler("NetUserSched_"+i);
                obj = (NetUser) userList.get(i);
                r1.attachHost(obj, userSched);
            }

            // connect all resource entities with r2 with 1Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            GridResource resObj = null;
            for (i = 0; i < resList.size(); i++)
            {
                FIFOScheduler resSched = new FIFOScheduler("GridResSched_"+i);
                resObj = (GridResource) resList.get(i);
                r2.attachHost(resObj, resSched);
            }

            // then connect r1 to r2 with 10Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            baud_rate = 10000;
            Link link = new SimpleLink("r1_r2_link", baud_rate, propDelay, mtu);
            FIFOScheduler r1Sched = new FIFOScheduler("r1_Sched");
            FIFOScheduler r2Sched = new FIFOScheduler("r2_Sched");

            // attach r2 to r1
            r1.attachRouter(r2, link, r1Sched, r2Sched);

            //////////////////////////////////////////
            // Fifth step: Starts the simulation
            GridSim.startGridSimulation();

            //////////////////////////////////////////
            // Final step: Prints the Gridlets when simulation is over

            // also prints the routing table
            r1.printRoutingTable();
            r2.printRoutingTable();

            GridletList glList = null;
            for (i = 0; i < userList.size(); i++)
            {
                obj = (NetUser) userList.get(i);
                glList = obj.getGridletList();
                printGridletList(glList, obj.get_name(), false);
            }

            System.out.println("\nFinish network example ...");
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
     * @param name          a Grid Resource name
     * @param baud_rate     the bandwidth of this entity
     * @param delay         the propagation delay
     * @param MTU           Maximum Transmission Unit
     * @return a GridResource object
     */
    private static GridResource createGridResource(String name,
                double baud_rate, double delay, int MTU)
    {
        System.out.println();
        System.out.println("Starting to create one Grid resource with " +
                "3 Machines");

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
                arch, os, mList, ResourceCharacteristics.TIME_SHARED,
                time_zone, cost);

        //System.out.println("Creates the properties of a Grid resource and " +
        //        "stores the Machine list");

        // 5. Finally, we need to create a GridResource object.
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
            // creates a GridResource with a link
            gridRes = new GridResource(name,
                new SimpleLink(name + "_link", baud_rate, delay, MTU),
                seed, resConfig, peakLoad, offPeakLoad, holidayLoad,
                Weekends, Holidays);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Finally, creates one Grid resource (name: " + name +
                " - id: " + gridRes.get_id() + ")");
        System.out.println();

        return gridRes;
    }

    /**
     * Prints the Gridlet objects
     */
    private static void printGridletList(GridletList list, String name,
                                         boolean detail)
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

        if (detail == true)
        {
            // a loop to print each Gridlet's history
            for (i = 0; i < size; i++)
            {
                gridlet = (Gridlet) list.get(i);
                System.out.println( gridlet.getGridletHistory() );

                System.out.print("Gridlet #" + gridlet.getGridletID() );
                System.out.println(", length = " + gridlet.getGridletLength()
                        + ", finished so far = " +
                        gridlet.getGridletFinishedSoFar() );
                System.out.println("======================================\n");
            }
        }
    }

} // end class

