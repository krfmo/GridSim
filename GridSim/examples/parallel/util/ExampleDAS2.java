/*
 * Author Bahman Javadi
 * Date:  October 2010
 * 
 * Description: This example shows how to create a resource using an aggressive 
 * backfilling policy and how to use DAS2 workload model to generate
 * jobs that are sent to a grid resource for processing.
 */

package parallel.util;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResGridlet;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.ParallelResource;
import gridsim.parallel.scheduler.AggressiveBackfill;
import gridsim.parallel.util.Workload;
import gridsim.parallel.util.WorkloadDAS2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Test Driver class for this example
 */
public class ExampleDAS2
{
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        long startTime = System.currentTimeMillis();
        try {
        	
            // number of grid user entities + any Workload entities.
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;     // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);

            //////////////////////////////////////////
            // Creates one ParallelResource entity
            int rating = 377;       	// rating of each PE in MIPS
            int totalPE = 9;        	// total number of PEs for each Machine
            int totalMachine = 128;   	// total number of Machines

            String resName = "Res_0";
            GridResource resource = createGridResource(resName, rating, totalMachine, totalPE);
            
            //////////////////////////////////////////
            // Creates one  Workload trace entity.
            long seed = new Random().nextLong();
            WorkloadDAS2 model = new WorkloadDAS2(rating, seed);
            
            // uHi is going to contain log2 of the maximum number of PEs that a
            // parallel job can require
            double uHi = Math.log(totalPE * totalMachine) / Math.log(2d);

            // Here we modify the number of nodes required by parallel jobs
            // as an example 
            double uMed = uHi-3.5;
            model.setParallelJobProbabilities(WorkloadDAS2.ULOW, uMed, uHi,WorkloadDAS2.UPROB);
            
            // Here we modify the inter-arrival time for jobs as an example
            model.setInterArrivalTimeParameters(WorkloadDAS2.AARR, 0.6); 
                
            // sets the workload to create 100 jobs
            model.setNumJobs(100);
            Workload workload = new Workload("Load_1", resource.get_name(), model);
            
            // Start the simulation in normal mode
            boolean debug = false;
            GridSim.startGridSimulation(debug);
            
            if(!debug) {
	            long finishTime = System.currentTimeMillis();
	            System.out.println("The simulation took " + (finishTime - startTime) + " milliseconds");
            }

            // prints the Gridlets inside a Workload entity
            ArrayList<Gridlet> newList = workload.getGridletList();
            printGridletList(newList);

        } catch (Exception e) {
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
        	AggressiveBackfill policy = new AggressiveBackfill(name, "Aggressive");
            parRes = new ParallelResource(name, baud_rate, resConfig, policy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return parRes;
    }


/**
 * Prints the Gridlet objects
 * @param list  list of Gridlets
 */
private static void printGridletList(ArrayList<Gridlet> list)
{
    int size = list.size();
    Gridlet gridlet;
    ResGridlet gl;
    double sr = 0;
    double mr = 0;

    String indent = "    ";
    System.out.println();
    System.out.println("========== OUTPUT ==========");
    System.out.println("Gridlet ID" + indent + "STATUS" + indent +
            "Resource ID" + indent + "CPU TIME" + indent + "Finish Time");

    for (int i = 0; i < size; i++)
    {
        gridlet = (Gridlet) list.get(i);
        
        sr += gridlet.getActualCPUTime()*(gridlet.getFinishTime()-gridlet.getSubmissionTime());
        mr += gridlet.getActualCPUTime();
        
        System.out.print(indent + gridlet.getGridletID() + indent
                + indent);

        if (gridlet.getGridletStatus() == Gridlet.SUCCESS)
            System.out.print("SUCCESS");

        System.out.println( indent + indent + gridlet.getResourceID() +
                indent + indent + gridlet.getActualCPUTime()+indent + gridlet.getFinishTime());
    }
    System.out.println("AWRT: "+sr/mr);
}

}