package network.scfq;

/*
 * Author: Anthony Sulistio
 * Date: March 2006
 * Description: A simple program to demonstrate of how to use GridSim
 *              network extension package.
 *              This example shows how to use a SCFQ packet scheduler.
 *              In addition, this example shows how to read a network
 *              topology from a given file.
 */

import gridsim.*;
import gridsim.net.*;
import java.util.*;
import gridsim.util.NetworkReader;   // for reading a network topology


/**
 * Test Driver class for this example
 */
public class SCFQExample
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting network example ...");

        try
        {
            if (args.length < 1) {
                System.out.println("Usage: java SCFQExample network.txt");
                return;
            }

            // get the network topology
            String filename = args[0];

            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 3;   // number of grid users
            Calendar calendar = Calendar.getInstance();

            // a flag that denotes whether to trace GridSim events or not.
            boolean trace_flag = false;

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Second step: Builds the network topology among Routers.

            // use SCFQ packet scheduler. Hence, determine the
            // weights for each ToS (Type of Service). The more weight, 
            // the better.
            // For example: ToS = 0 => weight[0] = 1
            //              ToS = 2 => weight[2] = 3
            double[] weight = {1, 2, 3};  // % of total bandwidth
            System.out.println("Reading network from " + filename);
            LinkedList routerList = NetworkReader.createSCFQ(filename, weight);

            //////////////////////////////////////////
            // Third step: Creates one or more GridResource entities

            double baud_rate = 100000000;  // 100 Mbps
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 1500;          // max. transmission unit in byte
            int i = 0;

            // link the resources into R5
            Router router = NetworkReader.getRouter("R5", routerList);

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

                // link a resource to this router object
                linkNetwork(router, res, weight);
            }

            //////////////////////////////////////////
            // Fourth step: Creates one or more grid user entities

            // number of Gridlets that will be sent to the resource
            int totalGridlet = 5;

            // create users
            ArrayList userList = new ArrayList(num_user);
            ArrayList userNameList = new ArrayList(); // for background traffic
            for (i = 0; i < num_user; i++)
            {
                String name = "User_" + i;

                // if trace_flag is set to "true", then this experiment will
                // create User_i.csv where i = 0 ... (num_user-1)
                NetUser user = new NetUser(name, totalGridlet, baud_rate,
                                           propDelay, mtu, trace_flag);

                // for each user, determines the Type of Service (ToS)
                // for sending objects over the network. The greater ToS is,
                // the more bandwidth allocation
                int ToS = i;
                if (i > weight.length) {
                    ToS = 0;
                }
                user.setNetServiceLevel(ToS);

                // link a User to a router object
                if (i == 0) {
                    router = NetworkReader.getRouter("R1", routerList);
                }
                else if (i == 1) {
                    router = NetworkReader.getRouter("R2", routerList);
                }
                else {
                    router = NetworkReader.getRouter("R3", routerList);
                }
                linkNetwork(router, user, weight);

                // add a user into a list
                userList.add(user);
                userNameList.add(name);
            }

            //////////////////////////////////////////
            // Fifth step: Starts the simulation
            GridSim.startGridSimulation();

            //////////////////////////////////////////
            // Final step: Prints the Gridlets when simulation is over

            // also prints the routing table
            router = NetworkReader.getRouter("R3", routerList);
            router.printRoutingTable();
            router = NetworkReader.getRouter("R4", routerList);
            router.printRoutingTable();

            GridletList glList = null;
            for (i = 0; i < userList.size(); i++)
            {
                NetUser obj = (NetUser) userList.get(i);
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
     * Links a particular entity with a given Router.
     * Since we are using the SCFQ packet scheduler, we need to
     * specify the weight for each Type of Service (ToS).
     *
     * @param router    a Router object
     * @param obj       a GridSim entity to be attached to the Router
     * @param weight    a weight of ToS
     */
    private static void linkNetwork(Router router, GridSimCore obj,
                                    double[] weight) throws Exception
    {
        if (router == null) {
            System.out.println("Error - router is NULL !!");
            return;
        }

        // get the baud rate of a link
        double baud_rate = obj.getLink().getBaudRate();

        // create the packet scheduler for this link
        PacketScheduler pktObj = new SCFQScheduler(router.get_name() +
                                 "_to_" + obj.get_name());

        ( (SCFQScheduler) pktObj).setBaudRate(baud_rate);
        ( (SCFQScheduler) pktObj).setWeights(weight);

        // attach this GridSim entity to a Router
        router.attachHost(obj, pktObj);
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

