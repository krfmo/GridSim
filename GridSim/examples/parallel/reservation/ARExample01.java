/*
 * Author: Marcos Dias de Assuncao
 * Date: May 2009
 * 
 * Description: A simple program to demonstrate of how to use basic
 *              advanced reservation functionalities, such as create, commit,
 *              get status of advance reservations and submit gridlets.
 */

package parallel.reservation;

import java.util.*;
import gridsim.*;
import gridsim.parallel.ParallelResource;
import gridsim.parallel.scheduler.ARConservativeBackfill;

/**
 * This is the main class to run the example.
 */
public class ARExample01 {
   
	/**
     * A main method that initialises GridSim, creating user and grid
     * resource entities before running the simulation.
     */
    public static void main(String[] args) {
        System.out.println("Starting " + ARExample01.class.getSimpleName());
        try {
            // First step: Initialises the GridSim package.
            int num_user = 1;    // number of user entities
            boolean trace_flag = false;  // true means trace GridSim events

            // Gets the current time as the init simulation time.
            Calendar calendar = Calendar.getInstance();
        
            // Initialise GridSim
            GridSim.init(num_user, calendar, trace_flag);

            //------------------------------------------------
            // Second step: Creates GridResource objects, given resource 
            // name, total PE, number of Machine, time zone and MIPS rating. 
            GridResource resource1 = 
            	createGridResource("Resource_1", 13, 1, 10.0, 684); 
                                            
            //------------------------------------------------
            // Third step: Creates grid users
            ARUser[] userList = new ARUser[num_user];
            ARUser user = null;        // a user entity
            double bandwidth = 1000;   // bandwidth of this user
            int totalJob = 0;          // total Gridlets owned
            int i = 0;
            
            // a loop that creates a user entity
            for (i = 0; i < num_user; i++) {
                // users with an even number have their time zone to GMT+8     
                if (i % 2 == 0) {
                    totalJob = 4;
                }
                
                // users with an odd number have their time zone to GMT-3     
                else {
                    totalJob = 5;
                }
                
                // creates a user entity
                user = new ARUser("User_" + i, bandwidth, totalJob); 

                // put the entity into an array
                userList[i] = user;
            }
            
            boolean debug = true;
            
            // Start Gridsim in debug mode
            GridSim.startGridSimulation(debug);

            if(!debug) {
	            //------------------------------------------------
	            // Final step: Prints the Gridlets when this simulation is over
	            GridletList newList = null;
	            for (i = 0; i < num_user; i++) {
	                user = userList[i];    
	                newList = user.getGridletList();
	                printGridletList( newList, user.get_name() );
	            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    
    /**
     * Creates a GridResource entity that supports advanced reservation
     * functionalities.
     */
    private static GridResource createGridResource(String name, int totalPE,
                        int totalMachine, double timeZone, int rating) {

        // 1. create an object of MachineList to store one or more Machines
    	MachineList mList = new MachineList();

        for (int i = 0; i < totalMachine; i++) {
              // 2. Create one Machine with its id and list of PEs or CPUs
          	mList.add( new Machine(i, totalPE, rating) );
        }

        // 3. Create a ResourceCharacteristics object that stores the
        //    properties of a resource: architecture, OS, list of
        //    Machines, allocation policy, time zone and its price 
        String arch = "Sun Ultra";   // system architecture
        String os = "Solaris";       // operating system
        double cost = 3.0;           // the cost of using this resource (G$/PE)

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.ADVANCE_RESERVATION,
                timeZone, cost);

        // 4. Finally, we need to create a ParallelResource object.
        double baud_rate = 1000.0;       // communication speed
        ParallelResource gridRes = null;
        try {
            // use the conservatibe backfilling based reservation algorithm.
            ARConservativeBackfill scheduler = new ARConservativeBackfill(name, "ARConservative");

            // then creates a grid resource entity.
            gridRes = new ParallelResource(name,baud_rate,resConfig,scheduler);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Created a resource with name: " + name);
        return gridRes;
    }

    /**
     * Prints the Gridlet objects
     * @param list  list of Gridlets
     */
    private static void printGridletList(GridletList list, String name) {
        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT for " + name + " ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost");

        int size = list.size();
        int i = 0;
        Gridlet gridlet = null;
        
        // for each user entity, prints out the results. Prints out the
        // table of summary 
        for (i = 0; i < size; i++) {
            gridlet = list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            System.out.print(gridlet.getGridletStatusString());

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }
    }
} 

