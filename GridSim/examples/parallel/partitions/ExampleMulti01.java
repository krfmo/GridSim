/*
 * Author Marcos Dias de Assuncao
 * Date: May 2009
 * 
 * Description: This example shows how to create a resource using an aggressive 
 * backfilling policy with multiple resource partitions. In this example we 
 * do not create multiple partitions. Thus, the policy should behave like a single
 * partition policy with aggressive backfilling. The jobs are read from a trace file. 
 * To test this example, you can use one of the traces included in the workloads 
 * directory.
 */

package parallel.partitions;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.ParallelResource;
import gridsim.parallel.scheduler.AggressiveMultiPartitions;
import gridsim.parallel.util.Workload;
import gridsim.parallel.util.WorkloadFileReader;

import java.util.Calendar;

/**
 * Test Driver class for this example
 */
public class ExampleMulti01
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        long startTime = System.currentTimeMillis();
        if(args.length == 0){
        	System.out.println("Please provide the location of the workload file!");
        	System.exit(1);
        }
        
        try {
        	
            String fileName = args[0];
        	
            // number of grid user entities + any Workload entities.
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;     // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Creates one GridResource entity
            int rating = 377;       	// rating of each PE in MIPS
            int totalPE = 9;        	// total number of PEs for each Machine
            int totalMachine = 128;   	// total number of Machines

            String resName = "Res_0";
            GridResource resource = createGridResource(resName, rating, totalMachine, totalPE);
            
            //////////////////////////////////////////
            // Creates one  Workload trace entity.
            WorkloadFileReader model = new WorkloadFileReader(fileName, rating);
            Workload workload = new Workload("Load_1", resource.get_name(), model);
            
            // Start the simulation in normal mode
            boolean debug = true;
            GridSim.startGridSimulation(debug);
            
            if(!debug) {
	            long finishTime = System.currentTimeMillis();
	            System.out.println("The simulation took " + (finishTime - startTime) + " milliseconds");
            }
                        
            // prints the Gridlets inside a Workload entity
            // workload.printGridletList(trace_flag);
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
    private static GridResource createGridResource(String name, int peRating,
                                           int totalMachine, int totalPE) {

        //////////////////////////////////////////
        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();

        for (int i = 0; i < totalMachine; i++) {
            //////////////////////////////////////////
            // 2. Create one Machine with its id and list of PEs or CPUs
        	mList.add( new Machine(i, totalPE, peRating) );
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

        // Create the ResourceCharacteristics object 
        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.SPACE_SHARED,
                time_zone, cost);
        
        //////////////////////////////////////////
        // 4. Finally, we need to create a GridResource object.
        double baud_rate = 10000.0;     // communication speed
        
        // incorporates holidays. However, no holidays are set in this example
        ParallelResource parRes = null;
        try {
        	AggressiveMultiPartitions policy = 
        		new AggressiveMultiPartitions(name, "AggressiveMultiPartitions", 1);
            parRes = new ParallelResource(name, baud_rate, resConfig, policy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return parRes;
    }
} 