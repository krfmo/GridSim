package WorkloadTrace.example02;

/*
 * Author: Anthony Sulistio
 * Date: November 2004
 * Description: A simple program to demonstrate of how to use GridSim
 *              workload trace functionality without using a network extension.
 *
 * A Workload entity can be classified as a grid user entity.
 * In this example, we only create workload and resource entities.
 * Each Workload entity sends Gridlets to only one grid resource.
 *
 * In addition, this example creates other GridSim user entities.
 * This example shows that Workload entity can be run together with other
 * entities.
 *
 * Running this experiment might take a lot of memory if the size of trace
 * file is big (in terms of number of lines/jobs).
 * If you encounter "out of memory" exception, you need to increase JVM heap
 * size using 'java -Xmx' option.
 * For example set the heap size to 300MB:
 * In Unix/Linux:
 *      java -Xmx300000000 -classpath $GRIDSIM/jars/gridsim.jar:. TraceEx02
 * In Windows:
 *      java -Xmx300000000 -classpath %GRIDSIM%\jars\gridsim.jar;. TraceEx02
 *
 * where $GRIDSIM or %GRIDSIM% is the location of the gridsimtoolkit package.
 *
 * When you see the output, there are few warnings about about a Gridlet
 * requires more than 1 PE. This is because the current GridSim schedulers,
 * TimeShared and SpaceShared, only process 1 PE for each Gridlet.
 * You are welcome to write your own scheduler that incorporates this
 * QoS (Quality of Service) requirement.
 */

import java.util.*;
import gridsim.*;
import gridsim.util.*;


/**
 * Test Driver class for this example
 */
public class TraceEx02
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
            boolean trace_flag = false;     // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Step 2: Creates one or more GridResource entities

            int totalResource = 2;  // total number of Grid resources
            int rating = 100;       // rating of each PE in MIPS
            int totalPE = 1;        // total number of PEs for each Machine
            int totalMachine = 1;   // total number of Machines
            int i = 0;

            String[] resArray = new String[totalResource];
            for (i = 0; i < totalResource; i++)
            {
                String resName = "Res_" + i;
                createGridResource(resName, rating, totalMachine, totalPE);

                // add a resource name into an array
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
                Workload w = new Workload("Load_"+i, dir + fileName[i],
                                          resArray[resID], rating);

                // add into a list
                load.add(w);
            }

            // for the custom trace file format
            Workload custom = new Workload("Custom", dir + customFile,
                                           resArray[resID], rating);

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

            int totalGridlet = 5;
            double baud_rate = 100;
            User[] userList = new User[numUserLeft];

            for (i = 0; i < numUserLeft; i++)
            {
                User user = new User("User_"+i, baud_rate, totalGridlet);
                userList[i] = user;
            }

            //////////////////////////////////////////
            // Step 6: Starts the simulation
            GridSim.startGridSimulation();

            //////////////////////////////////////////
            // Final step: Prints the Gridlets when simulation is over

            // prints the Gridlets inside a Workload entity
            for (i = 0; i < load.size(); i++)
            {
                Workload obj = (Workload) load.get(i);
                obj.printGridletList(trace_flag);
            }

            // prints the Gridlet inside a grid user entity
            for (i = 0; i < userList.length; i++)
            {
                User userObj = userList[i];
                userObj.printGridletList(trace_flag);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates one Grid resource. A Grid resource contains one or more
     * Machines. Similarly, a Machine contains one or more PEs (Processing
     * Elements or CPUs).
     * @param name      a Grid Resource name
     * @param peRating  rating of each PE
     * @param totalMachine  total number of Machines
     * @param totalPE       total number of PEs for each Machine
     */
    private static void createGridResource(String name, int peRating,
                                           int totalMachine, int totalPE)
    {
        //////////////////////////////////////////
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();

        int rating = peRating;
        for (int i = 0; i < totalMachine; i++)
        {
            // 2. Create one Machine with its id, number of PEs and rating
            mList.add( new Machine(i, totalPE, rating));
        }

        //////////////////////////////////////////
        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 0.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using this resource

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.SPACE_SHARED,
                time_zone, cost);

        //////////////////////////////////////////
        // 4. Finally, we need to create a GridResource object.
        double baud_rate = 10000.0;           // communication speed
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
        try {
            gridRes = new GridResource(name, baud_rate, seed,
                    resConfig, peakLoad, offPeakLoad, holidayLoad, Weekends,
                    Holidays);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creates one Grid resource with name = " + name);
    }

} // end class

