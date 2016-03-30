package ResFailure.example02;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 *               An example of how to use the failure functionality.
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: UCLM (Spain)
 * Created on: August 2007
 */

import gridsim.resFailure.*;
import gridsim.*;
import gridsim.net.*;
import java.util.*;
import gridsim.util.NetworkReader;
import gridsim.util.HyperExponential;


/**
 * This example shows a basic topology with 20 users, 6 resources and 1 GIS.
 * All of them are connected by a router.
 * There is only one VO in this example.
 * @author       Agustin Caminero and Anthony Sulistio
 * @since        GridSim Toolkit 4.1
 */
public class ResFailureEx02
{
    public static void main(String[] args)
    {
        System.out.println("Starting failure example 2...");

        try
        {
            if (args.length < 1)
            {
                System.out.println("Usage: java ResFailureEx02 network_ex02.txt");
                return;
            }


            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get a run-time exception
            // error.

            // a flag that denotes whether to trace GridSim events or not.
            boolean trace_flag = false;  // dont use SimJava trace
            int num_user = 20; //  number of grid users
            Calendar calendar = Calendar.getInstance();

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);
            trace_flag = true;


            //////////////////////////////////////////
            // Second step: Builds the network topology among Routers.

            String filename = args[0];  // get the network topology
            System.out.println("Reading network from " + filename);
            LinkedList routerList = NetworkReader.createFIFO(filename);


            //////////////////////////////////////////
            // Third step: Creates one RegionalGISWithFailure entity,
            // linked to a router in the topology

            Router router = null;
            double baud_rate = 100000000;   // 100Mbps, i.e. baud rate of links
            double propDelay = 10;      // propagation delay in milliseconds
            int mtu = 1500;             // max. transmission unit in byte
            int totalMachines = 16;     // num of machines each resource has

            String NAME = "Ex02_";  // a common name
            String gisName = NAME + "Regional_GIS";  // GIS name

            // a network link attached to this regional GIS entity
            Link link = new SimpleLink(gisName + "_link", baud_rate,
                                       propDelay, mtu);


            // HyperExponential: mean, standard deviation, stream
            // how many resources will fail
            HyperExponential failureNumResPattern =
                    new HyperExponential(totalMachines / 2, totalMachines, 4);

            // when they will fail
            HyperExponential failureTimePattern =
                    new HyperExponential(25, 100, 4);

            // how long the failure will be
            HyperExponential failureLengthPattern =
                    new HyperExponential(20, 25, 4); // big test: (20, 100, 4);


            // creates a new Regional GIS entity that generates a resource
            // failure message according to these patterns.
            RegionalGISWithFailure gis = new RegionalGISWithFailure(gisName,
                              link, failureNumResPattern, failureTimePattern,
                              failureLengthPattern);
            gis.setTrace(trace_flag);   // record this GIS activity

            // link these GIS to a router
            router = NetworkReader.getRouter("Router0", routerList);
            linkNetwork(router, gis);

            // print some info messages
            System.out.println("Created a REGIONAL GIS with name " + gisName +
                    " and id = " + gis.get_id() + ", connected to " +
                    router.get_name() );


            //////////////////////////////////////////
            // Fourth step: Creates one or more GridResourceWithFailure
            // entities, linked to a router in the topology

            String sched_alg = "SPACE";  // use space-shared policy
            int totalResource = 6; // number of resources
            ArrayList resList = new ArrayList(totalResource);

            // Each resource may have different baud rate,
            // totalMachine, rating, allocation policy and the router to
            // which it will be connected. However, in this example, we assume
            // all have the same properties for simplicity.
            int totalPE = 4;        // num of PEs each machine has
            int rating = 49000;     // rating (MIPS) of PEs
            int GB = 1000000000;    // 1 GB in bits
            baud_rate = 2.5 * GB;

            for (int i = 0; i < totalResource; i++)
            {
                // gets the router object from the list
                router = NetworkReader.getRouter("Router0", routerList);

                // creates a new grid resource
                String resName = NAME + "Res_" + i;
                GridResourceWithFailure res = createGridResource(resName,
                            baud_rate, propDelay, mtu, totalPE, totalMachines,
                            rating, sched_alg);

                // add a resource into a list
                resList.add(res);
                res.setRegionalGIS(gis);    // set the regional GIS entity
                res.setTrace(trace_flag);   // record this resource activity

                // link a resource to this router object
                linkNetwork(router, res);

                System.out.println("Created a RESOURCE (" + totalMachines +
                    " machines, each with " + totalPE + " PEs) with name = " +
                    resName + " and id = " + res.get_id() +
                    ", connected to router " + router.get_name() +
                    " and registered to " + gis.get_name() );
            }


            //////////////////////////////////////////
            // Fifth step: Creates one or more GridUserFailure entities,
            // linked to a router of the topology

            int totalGridlet = 5;    // total jobs
            double pollTime = 100;   // time between polls
            int glSize = 100000;     // the size of gridlets (input/output)
            int glLength = 42000000; // the length (MI) of gridlets

            // gets the router object from the list
            router = NetworkReader.getRouter("Router0", routerList);

            for (int i = 0; i < num_user; i++)
            {
                String userName = NAME + "User_" + i;

                // a network link attached to this entity
                Link link2 = new SimpleLink(userName + "_link", baud_rate,
                                            propDelay, mtu);

                // only keeps track activities from User_0
                if (i != 0) {
                    trace_flag = false;
                }

                GridUserFailureEx02 user = new GridUserFailureEx02(userName, link2,
                        pollTime, glLength, glSize, glSize, trace_flag);

                user.setRegionalGIS(gis); // set the regional GIS entity
                user.setGridletNumber(totalGridlet);

                // link a resource to this router object
                linkNetwork(router, user);

                System.out.println("Created a USER with name " + userName +
                        " and id = " + user.get_id() + ", connected to " +
                        router.get_name() + ", and with " + totalGridlet +
                        " gridlets. Registered to " + gis.get_name() );
            }
            System.out.println();

            //////////////////////////////////////////
            // Sixth step: Starts the simulation
            GridSim.startGridSimulation();
            System.out.println("\nFinish failure example 2... \n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

    /**
     * Links a particular entity with a given Router.
     *
     * @param router    a Router object
     * @param obj       a GridSim entity to be attached to the Router
     */
    private static void linkNetwork(Router router, GridSimCore obj) throws Exception
    {
        if (router == null) {
            System.out.println("Error - router is NULL.");
            return;
        }

        // get the baud rate of a link
        double baud_rate = obj.getLink().getBaudRate();

        // create the packet scheduler for this link
        PacketScheduler pktObj = new FIFOScheduler(router.get_name() +
                                                   "_to_" + obj.get_name() );

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
     *
     * @param name          a Grid Resource name
     * @param baud_rate     the bandwidth of this entity
     * @param delay         the propagation delay
     * @param MTU           Maximum Transmission Unit
     * @param totalPE       number of PE per machine
     * @param totalMachine  number of machines in this resources
     * @param rating        rating of mahcines in this resource
     * @param sched_alg     the scheduling algorithm of this resource
     * @return a GridResource object
     */
    private static GridResourceWithFailure createGridResource(String name,
                        double baud_rate, double delay, int MTU, int totalPE,
                        int totalMachine, int rating, String sched_alg)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        for (int i = 0; i < totalMachine; i++)
        {
            // 2. Create one Machine with its id, number of PEs and rating
            mList.add( new Machine(i, totalPE, rating));
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 9.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using this resource

        // we create Space_Shared_failure or Time_Shared_failure schedulers
        int scheduling_alg = 0;         // space_shared or time_shared
        if (sched_alg.equals("SPACE") == true) {
            scheduling_alg = ResourceCharacteristics.SPACE_SHARED;
        }
        else if (sched_alg.equals("TIME") == true) {
            scheduling_alg = ResourceCharacteristics.TIME_SHARED;
        }

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, scheduling_alg, time_zone, cost);


        // 4. Finally, we need to create a GridResource object.
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
        GridResourceWithFailure gridRes = null;
        try
        {
            // creates a GridResource with a link
            gridRes = new GridResourceWithFailure(name,
                new SimpleLink(name + "_link", baud_rate, delay, MTU),
                seed, resConfig, peakLoad, offPeakLoad, holidayLoad,
                Weekends, Holidays);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return gridRes;
    }

} // end class
