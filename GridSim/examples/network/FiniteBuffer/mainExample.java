package network.FiniteBuffer;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */


import gridsim.net.fnb.*;
import gridsim.*;
import gridsim.net.*;
import java.util.*;
import gridsim.index.RegionalGIS;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * A main class that runs this example.
 * @author Agustin Caminero and Anthony Sulistio
 */
public class mainExample
{
    /**
     * Creates main() to run this example.
     * Command-line argument:
     * - args[0] = SCHED, for choosing different FNB schedulers.
     *   SCHED = {red, ared, fifo}. Default SCHED is fifo.
     *
     * - args[1] = STATS, for choosing whether to turn on the statistics
     *   on routers or not.
     *   STATS = {true, false}. Default STATS is false.
     *
     */
    public static void main(String args[])
    {
        System.out.println("Starting finite buffers example ...");

        try
        {
            /************
            The network topology is as follows:

                    1Gbps             1Gbps               1Gbps
            Users --------- Router1 ----------- Router0 --------- Resource_0
                                                    |------------ RegionalGIS
                                                        1Gbps
            ************/

            // scheduling algorithm for buffers:
            // GridSimTags.FNB_RED, GridSimTags.FNB_ARED, or GridSimTags.FNB_FIFO;
            int SCHED_ALG = GridSimTags.FNB_FIFO;
            String NETWORK_FILE = "network_example.txt";

            if (args.length >= 1)
            {
                if (args[0].compareToIgnoreCase("red") == 0)
                {
                    SCHED_ALG = GridSimTags.FNB_RED;
                    System.out.println("Using a FNB_RED scheduling.");
                }
                else if (args[0].compareToIgnoreCase("ared") == 0)
                {
                    SCHED_ALG = GridSimTags.FNB_ARED;
                    System.out.println("Using a FNB_ARED scheduling.");
                }
            }
            else
            {
                System.out.println("Using a default FNB_FIFO scheduling.");
            }

            boolean STORE_STATS = false;  // records stats in the router or not
            if (args.length >= 2)
            {
                Boolean boolObj = new Boolean(args[1]);
                STORE_STATS = boolObj.booleanValue();
            }

            int NUM_USERS = 2;  // num of users
            int NUM_GIS = 1;    // num of GIS objects

            int GB = 1000000000;     // 1 GB in bits
            double BW_RES = 1*GB;    // baud rate for the link of Resource_0
            double BW_USERS = 1*GB;  // baud rate for the link of users
            double BW_GIS = 1*GB;    // baud rate for the link of RegionalGIS

            int MAX_TH = 15; // max threshold for the RED algorithm
            int MIN_TH = 5;  // min threshold for the RED algorithm

            // common parameters for all the FNB schedulers
            double MAX_P = 0.02; // maximum value for the dropping probability
            double QUEUE_WEIGHT = 0.0001; // the queue weight

            // the bigger the buffer size, the lower num of packets being rejected
            int MAX_BUF_SIZE = 30;

            int TOTAL_GRIDLET = 5; // gridlets each user has
            int GRIDLET_SIZE_BASE = 42000;
            long GRIDLET_FILESIZE_BASE_IN  = 150000;
            long GRIDLET_FILESIZE_BASE_OUT = 150000;

            int totalResource = 2;  // total number of resources
            int totalMachine = 1;
            int totalPE = 4;
            int rating; // rating (MIPS) of the PEs of the resources


            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.

            // a flag that denotes whether to trace GridSim events or not.
            boolean trace_flag = false;
            Calendar calendar = Calendar.getInstance();

            // Initialize the GridSim package
            GridSim.init(NUM_USERS, calendar, trace_flag);
            GridSim.initNetworkType(GridSimTags.NET_BUFFER_PACKET_LEVEL);


            //////////////////////////////////////////
            // Second step: Builds the network topology among Routers.

            double[] weights = {1, 2}; // for ToS

            System.out.println("Reading network from " + NETWORK_FILE);
            LinkedList routerList = FnbNetworkReader.createSCFQ(NETWORK_FILE,
                    weights, MAX_BUF_SIZE, SCHED_ALG, MIN_TH, MAX_TH,
                    MAX_P, QUEUE_WEIGHT, STORE_STATS);


            //////////////////////////////////////////
            // Third step: Creates one or more RegionalGIS entities,
            // linked to a router of the topology

            String ROUTER_RES = "Router0";   // router where resources will be conected
            String ROUTER_USERS = "Router1"; // router where users will be conected
            String ROUTER_GIS = "Router0";   // router where GIS will be connected

            int gisIndex = 0; // a variable to select the primary GIS entity
            RegionalGIS gis = null;

            double baud_rate;
            double propDelay = 10; // propagation delay in millisecond
            int mtu = 1500; // max. transmission unit in byte

            ArrayList gisList = new ArrayList(NUM_GIS); // list of GIS entities
            ArrayList resList = new ArrayList(totalResource); // a list of resources

            Router router = null;
            int i = 0; // a temp variable for a loop

            for (i = 0; i < NUM_GIS; i++)
            {
                String gisName = "RegGIS_" + i; // regional GIS name
                baud_rate = BW_GIS;

                // a network link attached to this regional GIS entity
                Link link = new SimpleLink(gisName + "_link", baud_rate,
                                           propDelay, mtu);

                gis = new RegionalGIS(gisName, link);
                gisList.add(gis); // store the regional GIS entity into an array

                router = FnbNetworkReader.getRouter(ROUTER_GIS, routerList);

                System.out.println("=== Created " + gisName
                        + " (id: " + gis.get_id()
                        + "), connected to " + router.get_name());

                linkNetworkSCFQ(router, gis, weights, MAX_BUF_SIZE, SCHED_ALG,
                                MIN_TH, MAX_TH, MAX_P, QUEUE_WEIGHT, STORE_STATS);
                System.out.println();
            }


            //////////////////////////////////////////
            // Fourth step: Creates one or more GridResource entities,
            // linked to a router of the topology
            String sched_alg;
            for (i = 0; i < totalResource; i++)
            {
                sched_alg = "SPACE";
                baud_rate = BW_RES;
                totalMachine = 10;
                rating = 49000;

                router = FnbNetworkReader.getRouter(ROUTER_RES, routerList);
                GridResource res = createGridResource("Res_" + i, baud_rate,
                    propDelay, mtu, totalPE, totalMachine, rating, sched_alg);

                gisIndex = 0; // CHANGE if we have more than 1 RegGIS
                gis = (RegionalGIS) gisList.get(gisIndex);

                // set the regional GIS entity
                if (res.setRegionalGIS(gis) == false)
                {
                    System.out.println("FAILURE when setting regional GIS for resource "
                            + res.get_name());
                }

                // add a resource into a list
                resList.add(res);

                System.out.println("=== Created " + GridSim.getEntityName(res.get_id())
                    + ": " + totalMachine + " machines, each with " + totalPE
                    + " PEs.\nConnected to " + router.get_name()
                    + ", and registered to " + gis.get_name());

                // link a resource to this router object
                linkNetworkSCFQ(router, res, weights, MAX_BUF_SIZE, SCHED_ALG,
                                MIN_TH, MAX_TH, MAX_P, QUEUE_WEIGHT, STORE_STATS);
                System.out.println();
            }


            //////////////////////////////////////////
            // Fifth step: Creates one or more user entities,
            // linked to a router of the topology

            // Each user will send to only one resource
            int resID = -1;  // resource ID

            Random random = new Random(); // a random generator
            for (i = 0; i < NUM_USERS; i++)
            {
                String name = "User_" + i;

                // Connect each user with a router, following our topology.
                router = FnbNetworkReader.getRouter(ROUTER_USERS, routerList);
                baud_rate = BW_USERS;

                // schedule the initial sending of gridlets. The sending will
                // start in a ramdom time within 5 min (300 sec)
                double init_time = 3000 * random.nextDouble();

                // a network link attached to this entity
                Link link = new SimpleLink(name+"_link", baud_rate, propDelay, mtu);

                // randomly choose a resource from the list
                int index = random.nextInt(resList.size());
                resID = ((GridResource) resList.get(index)).get_id();

                FnbUser user = new FnbUser(name, link, i, GRIDLET_SIZE_BASE,
                    GRIDLET_FILESIZE_BASE_IN, GRIDLET_FILESIZE_BASE_OUT,
                    init_time, resID);

                user.setGridletNumber(TOTAL_GRIDLET);  // total jobs for execution

                // for each user, determines the Type of Service (ToS)
                // for sending objects over the network. The greater ToS is,
                // the more bandwidth allocation
                int ToS = random.nextInt(weights.length);
                user.setNetServiceLevel(ToS);

                System.out.println("=== Created " + name
                    + " (id: " + user.get_id() + "), connected to "
                    + router.get_name() + ", with " + TOTAL_GRIDLET
                    + " gridlets.\nRegistered to " + gis.get_name()
                    + ". ToS: " + ToS + ". Init time: " + init_time);

                linkNetworkSCFQ(router, user, weights, MAX_BUF_SIZE, SCHED_ALG,
                                MIN_TH, MAX_TH, MAX_P, QUEUE_WEIGHT, STORE_STATS);

                gis = (RegionalGIS) gisList.get(gisIndex);
                user.setRegionalGIS(gis); // set the regional GIS entities
                System.out.println();

            } // create users
            System.out.println();

            //////////////////////////////////////////
            // Final step: Starts the simulation
            GridSim.startGridSimulation();
            System.out.println("\nFinish finite buffers example");

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }


    /**
     * Links a particular entity with a given Router, using SCFQ schedulers.
     * Since we are using the SCFQ packet scheduler, we need to
     * specify the weight for each Type of Service (ToS).
     *
     * @param router    a Router object
     * @param obj       a GridSim entity to be attached to the Router
     * @param weight    a weight of ToS
     * @param max_buf_size maximum buffer size
     */
    private static void linkNetworkSCFQ(Router router, GridSimCore obj,
                double[] weight, int max_buf_size, int drop_alg, int min_th,
                int max_th, double max_p, double queue_weight, boolean stats)
                throws Exception
    {
        if (router == null)
        {
            System.out.println("Error - router is NULL !!");
            return;
        }

        // get the baud rate of a link
        double baud_rate = obj.getLink().getBaudRate();
        PacketScheduler pktObj;

        if (drop_alg == GridSimTags.FNB_ARED)
        {
            System.out.println("linkNetworkSCFQ(): creating ARED scheduler.");

            // create the packet scheduler for this link
            pktObj = new ARED(router.get_name() + "_to_" + obj.get_name(),
                              baud_rate, max_p, max_buf_size, queue_weight, stats);

            ((ARED) pktObj).setWeights(weight);
        }
        else if (drop_alg == GridSimTags.FNB_RED)
        {
            System.out.println("linkNetworkSCFQ(): creating RED scheduler.");

            // create the packet scheduler for this link
            pktObj = new RED(router.get_name() + "_to_" + obj.get_name(),
                             baud_rate, max_buf_size, min_th, max_th,
                             max_p, queue_weight, stats);

            ((RED) pktObj).setWeights(weight);
        }
        else // if (drop_alg == GridSimTags.FNB_FIFO)
        {

            System.out.println("linkNetworkSCFQ(): creating FIFO scheduler.");

            // create the packet scheduler for this link
            pktObj = new FIFO(router.get_name() + "_to_" + obj.get_name(),
                              baud_rate, max_buf_size, queue_weight, stats);

            // This is also needed here, as SCFQ is the scheduler for this link
            ((FIFO) pktObj).setWeights(weight);

        }

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
     * @param totalPE  number of PE per machine
     * @param totalMachine number of machines in this resources
     * @param rating rating of mahcines in this resource
     * @param sched_alg the scheduling algorithm of this resource
     * @return a GridResource object
     */
    private static GridResource createGridResource(String name, double baud_rate,
                        double delay, int MTU, int totalPE, int totalMachine,
                        int rating, String sched_alg)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        for (int i = 0; i < totalMachine; i++)
        {
            // 2. Create one Machine with its id and list of PEs or CPUs
            mList.add(new Machine(i, totalPE, rating));
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra"; // system architecture
        String os = "Solaris"; // operating system
        double time_zone = 9.0; // time zone this resource located
        double cost = 3.0; // the cost of using this resource
        int scheduling_alg = 0; // space_shared or time_shared

        if (sched_alg.equals("SPACE"))
        {
            scheduling_alg = ResourceCharacteristics.SPACE_SHARED;
        }
        else if (sched_alg.equals("TIME"))
        {
            scheduling_alg = ResourceCharacteristics.TIME_SHARED;
        }

        ResourceCharacteristics resConfig = new ResourceCharacteristics(arch, os,
                mList, scheduling_alg, time_zone, cost);


        // 4. Finally, we need to create a GridResource object.
        long seed = 11L * 13 * 17 * 19 * 23 + 1;
        double peakLoad = 0.0; // the resource load during peak hour
        double offPeakLoad = 0.0; // the resource load during off-peak hr
        double holidayLoad = 0.0; // the resource load during holiday

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
                seed, resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends, Holidays);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return gridRes;
    }

} // end class

