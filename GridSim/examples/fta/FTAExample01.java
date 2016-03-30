/*
 * Author Bahman Javadi

 * Date: October 2010
 * 
 * Description: This example shows how to create a resource with failure. 
 * The failure events are read from a trace file with the FTA format. 
 * See http://fta.inria.fr for the format and more information about the traces.
 * 
 * The jobs also are read from a trace file as well. 
 * We used a perfect check-pointing policy for this example.
 * To test this example, you can use the sample traces included in the fta directory.
 * 
 * For example, in order to run the example on Linux, you should use:
 *	
 * java -cp $GRIDSIM/jars/gridsim.jar:. fta.FTAExample01 \
 *    			$GRIDSIM/examples/fta/workload_bot.txt  $GRIDSIM/examples/fta/fta_tab 
 */

package fta;

import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Gridlet;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.fta.FTAGridResource;
import gridsim.fta.FailureFileReader;
import gridsim.fta.FailureGenerator;
import gridsim.fta.PerfectCheckPointing;
import gridsim.fta.ResourceFileReader;
import gridsim.parallel.util.Workload;
import gridsim.parallel.util.WorkloadFileReader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

public class FTAExample01 {
	 /**
     * Creates main() to run this example
     */
    public static void main(String[] args)
    {
        long startTime = System.currentTimeMillis();
        if(args.length == 0){
        	System.out.println("Please provide the location of the trace files (Workload_trace_filename FTA_Path)!");
        	System.exit(1);
        }
        
        try {
        	
            String Work_fileName = args[0];
            String FTA_Path = args[1];
            String FTA_event_fileName = FTA_Path + "/event_trace.tab";
            String FTA_platform_fileName = FTA_Path + "/platform.tab";
            String FTA_node_fileName = FTA_Path + "/node.tab";
            double TraceStartTime = 0;

            // number of grid user entities + any Workload entities.
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;     // mean trace GridSim events

            // Initialize the GridSim package without any statistical
            // functionalities. Hence, no GridSim_stat.txt file is created.
            System.out.println("Initializing GridSim package");
            GridSim.init(num_user, calendar, trace_flag);
            
            ResourceFileReader res = new ResourceFileReader(FTA_platform_fileName, FTA_node_fileName);
            //////////////////////////////////////////
            // As in the Gridsim, the failure is defined in the level of Machine, we
            // consider a resource of X machines with one PE
            //
            // Creates one GridResource entity
            int rating = 377;       				// rating of each PE in MIPS
            int totalPE = 1;        				// total number of PEs for each Machine
            int totalMachine = res.getNodeNum();   	// total number of Machines

            String resName = res.getPlatformName();
            
            GridResource resource = createGridResource(resName, rating, totalMachine, totalPE);
            
            //////////////////////////////////////////
            // Creates an event list.
            FailureFileReader failure = new FailureFileReader(FTA_event_fileName, TraceStartTime);     
            FailureGenerator failure_list = new FailureGenerator("Failure_1", resource.get_name(), failure);

            //////////////////////////////////////////
            // Creates a Workload trace entity.
            WorkloadFileReader model = new WorkloadFileReader(Work_fileName, rating);
            Workload workload = new Workload("Load_1", resource.get_name(), model);

            //////////////////////////////////////////
            // Starts the simulation in debug mode
            boolean debug = false;
            GridSim.startGridSimulation(debug);

            if(!debug) {
	            long finishTime = System.currentTimeMillis();
	            System.out.println("The simulation took " + (finishTime - startTime) + " milliseconds");
            }
            
            // prints the Gridlets inside a Workload entity
            ArrayList<Gridlet> newList = workload.getGridletList();
            printGridletList(newList);


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
     * @throws Exception 
     */
    private static GridResource createGridResource(String name, int peRating,
                                           int totalMachine, int totalPE) throws Exception {
    	
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
        long seed = 11L*13*17*19*23+1;
        double peakLoad = 0.0;        // the resource load during peak hour
        double offPeakLoad = 0.0;     // the resource load during off-peak hr
        double holidayLoad = 0.0;     // the resource load during holiday

        // incorporates weekends so the grid resource is on 7 days a week
        LinkedList<Integer> Weekends = new LinkedList<Integer>();
        Weekends.add(new Integer(Calendar.SATURDAY));
        Weekends.add(new Integer(Calendar.SUNDAY));
        // incorporates holidays. However, no holidays are set in this example
        LinkedList Holidays = new LinkedList();
        
        ResourceCalendar calendar =  new ResourceCalendar(time_zone, peakLoad, 
        		offPeakLoad, holidayLoad,
                Weekends, Holidays, seed);

        
        FTAGridResource FTARes = null;
        try {
        	PerfectCheckPointing policy = new PerfectCheckPointing(name, "Perfect");
        	FTARes = new FTAGridResource(name, baud_rate, resConfig, calendar,policy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Creates one Grid resource with name = " + name + " with "+ totalMachine + " Machines");
        return FTARes;
    }
    
    /**
     * Prints the Gridlet objects
     * @param list  list of Gridlets
     */
    private static void printGridletList(ArrayList<Gridlet> list)
    {
        int size = list.size();
        Gridlet gridlet;

        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "CPU TIME"  + indent + "Start Time"+ indent + "Finish Time" + indent + "Run Time" + indent + "Submission Time" +indent + "Waiting Time");

        for (int i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
                       
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            if (gridlet.getGridletStatus() == Gridlet.SUCCESS)
                System.out.print("SUCCESS");

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getActualCPUTime()+ indent+ gridlet.getExecStartTime() + indent + indent+ gridlet.getFinishTime() + indent + indent+ (gridlet.getFinishTime() - gridlet.getExecStartTime())+indent + gridlet.getSubmissionTime() +indent + gridlet.getWaitingTime());
        }
    }

}