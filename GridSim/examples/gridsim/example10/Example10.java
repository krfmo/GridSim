package gridsim.example10;

/*
 * Author Anthony Sulistio
 * Date: May 2004 
 * Description: A simple program to demonstrate of how to use basic
 *              advanced reservation functionalities, such as create, commit
 *              and status.
 *
 * NOTE: The values used from this example are taken from the GridSim paper.
 *       http://www.gridbus.org/gridsim/
 * $Id: Example10.java,v 1.2 2004/05/31 04:53:33 anthony Exp $
 */

import java.util.*;
import gridsim.*;

/**
 * This is the main class to run the example.
 */
public class Example10 
{
    /**
     * A main method that initializes GridSim, creating user and grid
     * resource entities before running the simulation.
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Example10");
        try
        {
            // First step: Initialize the GridSim package. It should be called
            // before creating any entities. We can't run this example without
            // initializing GridSim first. We will get run-time exception
            // error.
            int num_user = 5;    // number of user entities

            // Gets the current time as the init simulation time.
            // The time plays a very important role in determining start
            // and end time of a reservation. Both start and end time must
            // be greater than the init simulation time.
            Calendar calendar = Calendar.getInstance();
            System.out.println("Init simulation time = " + calendar.getTime());
        
            boolean trace_flag = false;  // true means trace GridSim events

            // Since GridSim 3.0, there are different ways to initialize the 
            // GridSim package. One of them is below without the need to
            // have GridStatistics entity. Therefore, removing any redundant
            // entities.
            GridSim.init(num_user, calendar, trace_flag);

            //------------------------------------------------
            // Second step: Creates one or more GridResource objects
            // given resource name, total PE, number of Machine, time zone
            // and MIPS Rating. 

            // R0: vpac Compaq AlphaServer
            ARGridResource resource0 = createGridResource("Resource_0", 
                                            4, 1, 10.0, 515);

            // R1: Manjra Linux PC                                            
            ARGridResource resource1 = createGridResource("Resource_1",
                                            13, 1, 10.0, 684); 
                                            
            // R2: Germany                                            
            ARGridResource resource2 = createGridResource("Resource_2",
                                            16, 1, 1.0, 410);
                                            
            // R3: Czech                                            
            ARGridResource resource3 = createGridResource("Resource_3",
                                            6, 1, 1.0, 410);
                                            
            // R4: Chichago                                            
            ARGridResource resource4 = createGridResource("Resource_4",
                                            8, 1, -6.0, 377);

            //------------------------------------------------
            // Third step: Creates grid users
            ARTest[] userList = new ARTest[num_user];
            ARTest user = null;        // a user entity
            double bandwidth = 1000;   // bandwidth of this user
            double timeZone = 0.0;     // user's time zone
            int totalJob = 0;          // total Gridlets owned
            int i = 0;
            
            // a loop that creates a user entity
            for (i = 0; i < num_user; i++)
            {
                // users with an even number have their time zone to GMT+8     
                if (i % 2 == 0) 
                {
                    timeZone = 8.0;    // with respect to GMT or UTC
                    totalJob = 4;
                }
                
                // users with an odd number have their time zone to GMT-3     
                else 
                {
                    timeZone = -3.0;   // with respect to GMT or UTC
                    totalJob = 5;
                }
                
                // creates a user entity
                user = new ARTest("User_" + i, bandwidth, timeZone, totalJob); 

                // put the entity into an array
                userList[i] = user;
            }
            
            //------------------------------------------------
            // Fourth step: Starts the simulation
            GridSim.startGridSimulation();

            //------------------------------------------------
            // Final step: Prints the Gridlets when this simulation is over
            GridletList newList = null;
            for (i = 0; i < num_user; i++)
            {
                user = (ARTest) userList[i];    
                newList = user.getGridletList();
                printGridletList( newList, user.get_name() );
            }
            
            System.out.println("Finish Example10");
        }
        catch (Exception e) 
        {
            System.out.println("Error ...... EXCEPTION");
            e.printStackTrace();
        }
            
    }    
    
    /**
     * Creates a GridResource entity that supports advanced reservation
     * functionalities.
     */
    private static ARGridResource createGridResource(String name, int totalPE,
                        int totalMachine, double timeZone, int rating)
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

        // NOTE: allocation policy in here is set to 
        // ResourceCharacteristics.ADVANCE_RESERVATION not SPACE_SHARED nor
        // TIME_SHARED.
        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.ADVANCE_RESERVATION,
                timeZone, cost);

        // 4. Finally, we need to create a GridResource object.
        double baud_rate = 1000.0;       // communication speed
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
            gridRes = new ARGridResource(name,baud_rate,resConfig,cal,policy);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Creates one Grid resource with name = " + name);
        return gridRes;
    }

    /**
     * Prints the Gridlet objects
     * @param list  list of Gridlets
     */
    private static void printGridletList(GridletList list, String name)
    {
        String indent = "    ";
        System.out.println();
        System.out.println("========== OUTPUT for " + name + " ==========");
        System.out.println("Gridlet ID" + indent + "STATUS" + indent +
                "Resource ID" + indent + "Cost");

        int size = list.size();
        int i = 0;
        Gridlet gridlet = null;
        String status;
        
        // for each user entity, prints out the results. Prints out the
        // table of summary 
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.print(indent + gridlet.getGridletID() + indent
                    + indent);

            status = gridlet.getGridletStatusString();
            System.out.print(status);

            System.out.println( indent + indent + gridlet.getResourceID() +
                    indent + indent + gridlet.getProcessingCost() );
        }

        // for each Gridlet, prints out its detailed operation / report
        /***** NOTE: Not recommended for many Gridlets as each Gridlet
        // report is pretty long
        System.out.println();
        for (i = 0; i < size; i++)
        {
            gridlet = (Gridlet) list.get(i);
            System.out.println( gridlet.getGridletHistory() );
        }
        *********/
    }
    
} // end class

