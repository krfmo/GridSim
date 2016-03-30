package gridsim.regionalGIS;

/*
 * Author: Anthony Sulistio
 * Date: March 2005
 * Description:
 *      This example demonstrates how to create multiple regional GIS as part of
 *      a network topology. In addition, how to query to the assigned regional
 *      GIS entity from the user's point of view.
 *
 * NOTE: GridSim version 3.2 or above is needed to run this example.
 *       In addition, this example does not care much about sending jobs or
 *       Gridlets to resources.
 *
 * $Id: ExampleGIS.java,v 1.1 2005/03/29 02:13:08 anthony Exp $
 */

import gridsim.*;
import gridsim.index.*;
import gridsim.net.*;
import java.util.*;


/**
 * A test driver to run multiple regional GIS in a network.
 * This test driver uses only NetUSerGIS.java file
 */
public class ExampleGIS
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        System.out.println("Starting GIS-network examples ...");

        try
        {
            //////////////////////////////////////////
            // Variables that are important to this example
            int num_user = 1;        // number of grid users
            int num_GIS = 3;         // number of regional GIS entity created
            int totalResource = 8;   // number of grid resources

            Random random = new Random();   // a random generator
            double baud_rate = 1e8;  // 100 Mbps
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 1500;          // max. transmission unit in byte
            int i = 0;               // a temp variable for a loop
            int gisIndex = 0;        // a variable to select a GIS entity
            RegionalGIS gis = null;  // a regional GIS entity

            //////////////////////////////////////////
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get a run-time exception
            // error.
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;     // true means trace GridSim events

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Second step: Creates one or more regional GIS entities
            ArrayList gisList = new ArrayList();  // array
            for (i = 0; i < num_GIS; i++)
            {
                String gisName = "Regional_GIS_" + i;   // regional GIS name

                // a network link attached to this regional GIS entity
                Link link = new SimpleLink(gisName + "_link", baud_rate,
                                           propDelay, mtu);

                // create a new regional GIS entity
                gis = new RegionalGIS(gisName, link);
                System.out.println("Creating a " + gisName + " with id = " +
                                   gis.get_id());

                // store the regional GIS entity into an array
                gisList.add(gis);
            }

            //////////////////////////////////////////
            // Third step: Creates one or more GridResource entities
            System.out.println();
            ArrayList resList = new ArrayList(totalResource);
            int totalPE = 4;        // number of CPUs
            int totalMachine = 1;   // number of machines or nodes
            int rating = 1500;      // an estimation CPU power

            // create a typical resource
            int totalAR = random.nextInt(totalResource);
            for (i = 0; i < (totalResource - totalAR); i++)
            {
                GridResource res = createGridResource("Res_" + i,
                                        totalPE, totalMachine, rating,
                                        baud_rate, propDelay, mtu);

                // allocate this resource to a random regional GIS entity
                gisIndex = random.nextInt(num_GIS);
                gis = (RegionalGIS) gisList.get(gisIndex);
                res.setRegionalGIS(gis);    // set the regional GIS entity
                resList.add(res);           // put this resource into a list

                System.out.println(res.get_name() + " will register to " +
                        gis.get_name());
                System.out.println();
            }

            // create a resource that supports advance reservation
            for (i = 0; i < totalAR; i++)
            {
                double time_zone = i;
                GridResource res = createARGridResource("AR_Res_" + i,
                                        totalPE, totalMachine, rating,
                                        baud_rate, propDelay, mtu);

                // allocate this resource to a random regional GIS entity
                gisIndex = random.nextInt(num_GIS);
                gis = (RegionalGIS) gisList.get(gisIndex);
                res.setRegionalGIS(gis);    // set the regional GIS entity
                resList.add(res);           // put this resource into a list

                System.out.println(res.get_name() + " will register to " +
                        gis.get_name());
                System.out.println();
            }

            //////////////////////////////////////////
            // Fourth step: Creates one or more grid user entities
            System.out.println();
            ArrayList userList = new ArrayList(num_user);
            for (i = 0; i < num_user; i++)
            {
                NetUserGIS user = new NetUserGIS("User_" + i,
                                                 baud_rate, propDelay, mtu);

                // allocate this resource to a random regional GIS entity
                gisIndex = random.nextInt(num_GIS);
                gis = (RegionalGIS) gisList.get(gisIndex);
                user.setRegionalGIS(gis);   // set the regional GIS entity
                userList.add(user);         // put this user into a list
                System.out.println(user.get_name() + " will communicate to " +
                        gis.get_name());
                System.out.println();
            }

            //////////////////////////////////////////
            // Fifth step: Builds the network topology for all entities.

            // In this example, the topology is:
            // with Regional GIS the network topology becomes:
            // User(s) --- r1 --- r2 --- r3 --- GIS(s)
            //                    |
            //                    |---- resource(s)
            createTopology(gisList, resList, userList);

            //////////////////////////////////////////
            // Sixth step: Starts the simulation
            System.out.println();
            GridSim.startGridSimulation();

            //////////////////////////////////////////
            System.out.println();
            System.out.println("Finish GIS-network example ...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

    /**
     * Creates a simple network topology
     * In this example, the topology is:
     * with Regional GIS the network topology becomes:
     * User(s) --- r1 --- r2 --- r3 --- GIS(s)
     *                    |
     *                    |---- resource(s)
     */
    private static void createTopology(ArrayList gisList, ArrayList resList,
                                       ArrayList userList) throws Exception
    {
        int i = 0;
        double baud_rate = 1e8;  // 100 Mbps
        double propDelay = 10;   // propagation delay in millisecond
        int mtu = 1500;          // max. transmission unit in byte

        // create the routers
        Router r1 = new RIPRouter("router1");   // router 1
        Router r2 = new RIPRouter("router2");   // router 2
        Router r3 = new RIPRouter("router3");   // router 3

        // connect all user entities with r1 router
        // For each host, specify which PacketScheduler entity to use.
        NetUserGIS obj = null;
        for (i = 0; i < userList.size(); i++)
        {
            FIFOScheduler userSched = new FIFOScheduler("NetUserSched_"+i);
            obj = (NetUserGIS) userList.get(i);
            r1.attachHost(obj, userSched);
        }

        // connect all resource entities with r2 router
        // For each host, specify which PacketScheduler entity to use.
        GridResource resObj = null;
        for (i = 0; i < resList.size(); i++)
        {
            FIFOScheduler resSched = new FIFOScheduler("GridResSched_"+i);
            resObj = (GridResource) resList.get(i);
            r2.attachHost(resObj, resSched);
        }

        // then connect r1 to r2
        // For each host, specify which PacketScheduler entity to use.
        Link link = new SimpleLink("r1_r2_link", baud_rate, propDelay, mtu);
        FIFOScheduler r1Sched = new FIFOScheduler("r1_Sched");
        FIFOScheduler r2Sched = new FIFOScheduler("r2_Sched");

        // attach r2 to r1
        r1.attachRouter(r2, link, r1Sched, r2Sched);

        // attach r3 to r2
        FIFOScheduler r3Sched = new FIFOScheduler("r3_Sched");
        link = new SimpleLink("r2_r3_link", baud_rate, propDelay, mtu);
        r2.attachRouter(r3, link, r2Sched, r3Sched);

        // attach regional GIS entities to r3 router
        RegionalGIS gis = null;
        for (i = 0; i < gisList.size(); i++)
        {
            FIFOScheduler gisSched = new FIFOScheduler("gis_Sched" + i);
            gis = (RegionalGIS) gisList.get(i);
            r3.attachHost(gis, gisSched);
        }
    }

    /**
     * Creates one Grid resource. A Grid resource contains one or more
     * Machines. Similarly, a Machine contains one or more PEs (Processing
     * Elements or CPUs).
     */
    private static GridResource createGridResource(String name,
                int totalPE, int totalMachine, int rating,
                double baud_rate, double delay, int MTU)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();

        for (int i = 0; i < totalMachine; i++)
        {
            // 2. Create one Machine with its id, number of PEs and rating
            mList.add( new Machine(i, totalPE, rating) );
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Intel";      // system architecture
        String os = "Linux";        // operating system
        double time_zone = 10.0;    // time zone this resource located
        double cost = 3.0;          // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.SPACE_SHARED,
                time_zone, cost);

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
        GridResource gridRes = null;
        try
        {
            // creates a GridResource with a link
            Link link = new SimpleLink(name + "_link", baud_rate, delay, MTU);
            gridRes = new GridResource(name, link, seed, resConfig, peakLoad,
                                offPeakLoad, holidayLoad, Weekends, Holidays);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creating a Grid resource (name: " + name +
                " - id: " + gridRes.get_id() + ")");

        return gridRes;
    }

    /**
     * Creates one Grid resource that support advance reservation.
     * A Grid resource contains one or more
     * Machines. Similarly, a Machine contains one or more PEs (Processing
     * Elements or CPUs).
     */
    private static GridResource createARGridResource(String name,
                int totalPE, int totalMachine, int rating,
                double baud_rate, double delay, int MTU)
    {
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        for (int i = 0; i < totalMachine; i++)
        {
            // 2. Create one Machine with its id, number of PEs and rating
            mList.add( new Machine(i, totalPE, rating) );
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy, time zone and its price
        String arch = "Sun Ultra";   // system architecture
        String os = "Solaris";       // operating system
        double cost = 3.0;           // the cost of using this resource (G$/PE)
        double timeZone = 11;        // time zone

        // NOTE: allocation policy in here is set to
        // ResourceCharacteristics.ADVANCE_RESERVATION not SPACE_SHARED nor
        // TIME_SHARED.
        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.ADVANCE_RESERVATION,
                timeZone, cost);

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
        ARGridResource gridRes = null;

        // creates a resource calendar that handles different loads
        ResourceCalendar cal = new ResourceCalendar(
                resConfig.getResourceTimeZone(), peakLoad, offPeakLoad,
                holidayLoad, Weekends, Holidays, seed);

        try
        {
            // use this AR scheduling algorithm. The name of this entity
            // will be name_scheduler, e.g. Resource0_scheduler.
            String scheduler = "scheduler";
            ARSimpleSpaceShared policy =new ARSimpleSpaceShared(name,scheduler);

            // then creates a grid resource entity.
            // NOTE: You need to use a grid resource entity that supports
            // advanced reservation functionalities. In this case, the entity
            // is called ARGridResource.
            Link link = new SimpleLink(name + "_link", baud_rate, delay, MTU);
            gridRes = new ARGridResource(name, link, resConfig, cal, policy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creating an AR Grid resource (name: " + name +
                " - id: " + gridRes.get_id() + ")");

        return gridRes;
    }

} // end class

