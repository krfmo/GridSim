package WorkloadTrace.example03;

/*
 * Author: Anthony Sulistio
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim
 *              workload trace functionality using a network extension.
 *
 * A Workload entity can be classified as a grid user entity.
 * In this example, we only create workload and resource entities.
 * Each Workload entity sends Gridlets to only one grid resource.
 *
 * In addition, this example creates other GridSim user entities.
 * This example shows that Workload entity can be run together with other
 * entities.
 *
 * Running this experiment might take a lot of memory and very long time
 * if the size of trace file is big (in terms of number of lines/jobs).
 * If you encounter "out of memory" exception, you need to increase JVM heap
 * size using 'java -Xmx' option.
 * For example set the heap size to 300MB:
 * In Unix/Linux:
 *      java -Xmx300000000 -classpath $GRIDSIM/jars/gridsim.jar:. TraceEx03
 * In Windows:
 *      java -Xmx300000000 -classpath %GRIDSIM%\jars\gridsim.jar;. TraceEx03
 *
 * where $GRIDSIM or %GRIDSIM% is the location of the gridsimtoolkit package.
 *
 * When you see the output, there are few warnings about about a Gridlet
 * requires more than 1 PE. This is because the current GridSim schedulers,
 * TimeShared and SpaceShared, only process 1 PE for each Gridlet.
 * You are welcome to write your own scheduler that incorporates this
 * QoS (Quality of Service) requirement.
 */

import gridsim.*;
import gridsim.net.*;
import gridsim.util.Workload;
import java.util.*;


/**
 * Test Driver class for this example
 */
public class TraceEx03
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {        
        try
        {
            //////////////////////////////////////////
            // Step 1: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            
            // number of grid user entities + any Workload entities.
            int num_user = 5;
            Calendar calendar = Calendar.getInstance();

            // a flag that denotes whether to trace GridSim events or not.
            boolean trace_flag = false;

            // Initialize the GridSim package
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Step 2: Creates one or more GridResource entities

            // baud rate and MTU must be big otherwise the simulation
            // runs for a very long period.
            double baud_rate = 1000000;     // 1 Gbits/sec
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 100000;        // max. transmission unit in byte
            int rating = 400;        // rating of each PE in MIPS
            int i = 0;
            
            // more resources can be created by
            // setting totalResource to an appropriate value
            int totalResource = 3;
            ArrayList resList = new ArrayList(totalResource);
            String[] resArray = new String[totalResource];
            
            for (i = 0; i < totalResource; i++)
            {
                String resName = "Res_" + i;
                GridResource res = createGridResource(resName, baud_rate,
                                                      propDelay, mtu, rating);

                // add a resource into a list
                resList.add(res);
                resArray[i] = resName;
            }

            //////////////////////////////////////////
            // Step 3: Get the list of trace files. The format should be:
            // ASCII text, gzip or zip.

            // In this example, I use the trace files from:
            // http://www.cs.huji.ac.il/labs/parallel/workload/index.html
            String[] fileName = {
                "l_lanl_o2k.swf.zip",     // LANL Origin 2000 Cluster (Nirvana)
                "l_sdsc_blue.swf.txt.gz", // SDSC Blue Horizon
            };

            String dir = "../";     // location of these files
            String customFile = "custom_trace.txt"; // custom trace file format
            
            // total number of Workload entities
            int numWorkload = fileName.length + 1;  // including custom trace

            //////////////////////////////////////////
            // Step 4: Creates one or more Workload trace entities.
            // Each Workload entity can only read one trace file and
            // submit its Gridlets to one grid resource entity.

            int resID = 0;
            Random r = new Random();
            ArrayList load = new ArrayList();     
            
            for (i = 0; i < fileName.length; i++)
            {
                resID = r.nextInt(totalResource);
                Workload w = new Workload("Load_"+i, baud_rate, propDelay, mtu, 
                                    dir + fileName[i], resArray[resID], rating);

                // add into a list
                load.add(w);
            }

            // for the custom trace file format
            Workload custom = new Workload("Custom", baud_rate, propDelay, mtu,
                                           dir + customFile, resArray[resID], rating);

            // add into a list
            load.add(custom);

            // tells the Workload entity what to look for.
            // parameters: maxField, jobNum, submitTime, runTime, numPE
            custom.setField(4, 1, 2, 3, 4);
            custom.setComment("#");     // set "#" as a comment

            //////////////////////////////////////////
            // Step 5: Creates one or more grid user entities.

            // number of grid user entities
            int numUserLeft = num_user - numWorkload;
            
            // number of Gridlets that will be sent to the resource
            int totalGridlet = 5;

            // create users
            ArrayList userList = new ArrayList(numUserLeft);
            for (i = 0; i < numUserLeft; i++)
            {
                // if trace_flag is set to "true", then this experiment will
                // create User_i.csv where i = 0 ... (num_user-1)
                NetUser user = new NetUser("User_"+i, totalGridlet, baud_rate,
                                           propDelay, mtu, trace_flag);

                // add a user into a list
                userList.add(user);
            }

            //////////////////////////////////////////
            // Step 6: Builds the network topology among entities.

            // In this example, the topology is:
            // user(s)     --1Gb/s-- r1 --10Gb/s-- r2 --1Gb/s-- GridResource(s)
            //                       |
            // workload(s) --1Gb/s-- |

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
                        
            // connect all Workload entities with r1 with 1Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            Workload w = null;
            for (i = 0; i < load.size(); i++)
            {
                // A First In First Out Scheduler is being used here.
                // SCFQScheduler can be used for more fairness
                FIFOScheduler loadSched = new FIFOScheduler("LoadSched_"+i);
                w = (Workload) load.get(i);
                r1.attachHost(w, loadSched);
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

            // then connect r1 to r2 with 10 Gbits/s connection
            // For each host, specify which PacketScheduler entity to use.
            baud_rate = 10000000;            
            
            Link link = new SimpleLink("r1_r2_link", baud_rate, propDelay, mtu);
            FIFOScheduler r1Sched = new FIFOScheduler("r1_Sched");
            FIFOScheduler r2Sched = new FIFOScheduler("r2_Sched");

            // attach r2 to r1
            r1.attachRouter(r2, link, r1Sched, r2Sched);

            //////////////////////////////////////////
            // Step 7: Starts the simulation
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
                printGridletList(glList, obj.get_name(), trace_flag);
            }
            
            // prints the Gridlets inside a Workload entity
            for (i = 0; i < load.size(); i++)
            {
                w = (Workload) load.get(i);
                w.printGridletList(trace_flag);
            }
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
     * @param rating        a PE rating
     * @return a GridResource object
     */
    private static GridResource createGridResource(String name,
                double baud_rate, double delay, int MTU, int rating)
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

