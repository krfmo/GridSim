/*
 * Author Marcos Dias de Assuncao
 * Date: May 2009
 * 
 * Description: This example shows how to create a resource using an aggressive 
 * backfilling policy and how to use Lublin99 workload model to generate
 * jobs that are sent to a grid resource for processing.
 */

package parallel.util;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.ParallelResource;
import gridsim.parallel.scheduler.AggressiveBackfill;
import gridsim.parallel.util.Workload;
import gridsim.parallel.util.WorkloadLublin99;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Test Driver class for this example
 */
public class ExampleLublin99
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
            WorkloadLublin99 model = new WorkloadLublin99(rating, false, seed);
            
            // uHi is going to contain log2 of the maximum number of PEs that a
            // parallel job can require
            double uHi = Math.log(totalPE * totalMachine) / Math.log(2d);

            // Here we modify the number of nodes required by parallel jobs
            // as an example 
            double uMed = uHi-3.5;
            model.setParallelJobProbabilities(WorkloadLublin99.INTERACTIVE_JOBS, 
            		model.getParallelJobULow(WorkloadLublin99.INTERACTIVE_JOBS), uMed, uHi, 
            		model.getParallelJobUProb(WorkloadLublin99.INTERACTIVE_JOBS));
            
            // Here we modify the inter-arrival time for jobs as an example
//            model.setInterArrivalTimeParameters(WorkloadLublin99.INTERACTIVE_JOBS, 
//            		WorkloadLublin99.AARR, 0.55D, 
//            		WorkloadLublin99.ANUM, WorkloadLublin99.BNUM, 
//            		WorkloadLublin99.ARAR);
                
            // sets the workload to create 3000 jobs
            model.setNumJobs(3000);
            Workload workload = new Workload("Load_1", resource.get_name(), model);
            
            // Start the simulation in normal mode
            boolean debug = true;
            GridSim.startGridSimulation(debug);
            
            if(!debug) {
	            long finishTime = System.currentTimeMillis();
	            System.out.println("The simulation took " + (finishTime - startTime) + " milliseconds");
            }
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
} 