package auction.example03;

/*
 * Author: Marcos Dias de Assuncao
 * Date: May 2008
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 *              This example shows how to create user, resource, auctioneer
 *              and auction entities connected via a network topology,
 *              using link and router.
 *
 */

import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.PE;
import gridsim.PEList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.net.FIFOScheduler;
import gridsim.net.Link;
import gridsim.net.RIPRouter;
import gridsim.net.Router;
import gridsim.net.SimpleLink;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;

/**
 * This class creates resources, users and routers and links them
 * in a network topology. This examples uses a continuous double auction.
 *
 * @author Marcos Dias de Assuncao
 */
public class ExampleAuction {
	
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
     * @param cost          the cost of using this resource
     * @param mips          Capability of a CPU in MIPS
     * @return a GridResource object
     */
    private static GridResource createGridResource(String name,
                double baud_rate, double delay, int MTU,
                double cost, int mips, int auctioneerId)
    {
        System.out.println();
        System.out.println("Starting to create one Grid resource with " +
                "3 Machines");

        // Here are the steps needed to create a Grid resource:
        // 1. We need to create an object of MachineList to store one or more
        //    Machines
        MachineList mList = new MachineList();
        //System.out.println("Creates a Machine list");

        // 2. Create a Machine with id, number of PEs and MIPS rating per PE
        mList.add( new Machine(0, 4, mips));   // First Machine

        // 3. Repeat the process from 2 if we want to create more Machines
        mList.add( new Machine(1, 4, mips));   // Second Machine
        mList.add( new Machine(2, 2, mips));   // Third Machine

        // 4. Create a ResourceCharacteristics object that stores the
        //    properties of a Grid resource: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/PE time unit).
        String arch = "Sun Ultra";      // system architecture
        String os = "Solaris";          // operating system
        double time_zone = 9.0;         // time zone this resource located

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
        LinkedList weekends = new LinkedList();
        weekends.add(new Integer(Calendar.SATURDAY));
        weekends.add(new Integer(Calendar.SUNDAY));

        // incorporates holidays. However, no holidays are set in this example
        LinkedList holidays = new LinkedList();

        ResourceCalendar calendar = new ResourceCalendar(time_zone, peakLoad,
        		offPeakLoad, holidayLoad, weekends, holidays, seed);

        AuctionResource gridRes = null;
        try
        {
            // creates a GridResource with a link
            gridRes = new AuctionResource(name,
            					new SimpleLink(name + "_link", baud_rate, delay, MTU),
            					resConfig,calendar, null);
            
            gridRes.setAuctioneerID(auctioneerId);
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
     *
     * @param args
     */
	public static void main(String[] args) {
		try{
	    	Calendar calendar = Calendar.getInstance();
	    	boolean trace_flag = true;  // mean trace GridSim events
	    	int num_user = 3;
	    	int num_resource = 2;
	    	int num_gridlet = 3;

	        // First step: Initialize the GridSim package. It should be called
	        // before creating any entities. We can't run GridResource
	        // entity without initializing GridSim first. We will get run-time
	        // exception error.

	        // Initialize the GridSim package
	        System.out.println("Initializing GridSim package");
	        GridSim.init(num_user, calendar, trace_flag);

            double baud_rate = 1000; // bits/sec
            double propDelay = 10;   // propagation delay in millisecond
            int mtu = 1500;          // max. transmission unit in byte

            long seed = 11L*13*17*19*23+1;

            Sim_uniform_obj genCost = new Sim_uniform_obj("cost",1,2, seed);
            Sim_uniform_obj genMIPS = new Sim_uniform_obj("mips",300,400, seed);


        	Broker broker = new Broker("Broker", baud_rate, propDelay, mtu, num_user);
        	
            // more resources can be created by
            // setting num_resource to an appropriate value
            ArrayList resList = new ArrayList(num_resource);
            for (int i = 0; i < num_resource; i++) {
                GridResource res = createGridResource("Resource_"+i, baud_rate,
                                                      propDelay, mtu, genCost.sample(),
                                                      ((int)genMIPS.sample()), broker.get_id());
                // add a resource into a list
                resList.add(res);
            }
            
            // create users
            ArrayList userList = new ArrayList(num_user);
            for (int i = 0; i < num_user; i++)
            {
                NetUser user = new NetUser("User_" + i, num_gridlet,
                		baud_rate, propDelay, mtu, broker);

                // add a user into a list
                userList.add(user);
            }
            
            //////////////////////////////////////////
            // Fourth step: Builds the network topology among entities.

            // In this example, the topology is:
            // user(s) --1Mb/s-- r1 --10Mb/s-- r2 --1Mb/s-- GridResource(s)

            // create the routers.
            // If trace_flag is set to "true", then this experiment will create
            // the following files (apart from sim_trace and sim_report):
            // - router1_report.csv
            // - router2_report.csv
            Router r1 = new RIPRouter("router1", trace_flag);   // router 1
            Router r2 = new RIPRouter("router2", trace_flag);   // router 2

            FIFOScheduler brokerSched = new FIFOScheduler("NetBrokerSched");
            r1.attachHost(broker, brokerSched);
            
            // connect all user entities with r1 with 1Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            NetUser user = null;
            for (int i = 0; i < userList.size(); i++)
            {
                // A First In First Out Scheduler is being used here.
                // SCFQScheduler can be used for more fairness
                FIFOScheduler userSched = new FIFOScheduler("NetUserSched_"+i);
                user = (NetUser) userList.get(i);
                r1.attachHost(user, userSched);
            }

            // connect all resource entities with r2 with 1Mb/s connection

            // For each host, specify which PacketScheduler entity to use.
            GridResource resObj = null;
            for (int i = 0; i < resList.size(); i++)
            {
                FIFOScheduler resSched = new FIFOScheduler("GridResSched_"+i);
                resObj = (GridResource) resList.get(i);
                r2.attachHost(resObj, resSched);
            }

            // then connect r1 to r2 with 10Mb/s connection
            // For each host, specify which PacketScheduler entity to use.
            baud_rate = 10000;
            Link link = new SimpleLink("r1_r2_link", baud_rate, propDelay, mtu);
            FIFOScheduler r1Sched = new FIFOScheduler("r1_Sched");
            FIFOScheduler r2Sched = new FIFOScheduler("r2_Sched");

            // attach r2 to r1
            r1.attachRouter(r2, link, r1Sched, r2Sched);

            //////////////////////////////////////////
            // Starts the simulation
            GridSim.startGridSimulation();

            //////////////////////////////////////////
            // Final step: Prints the Gridlets when simulation is over

//            // also prints the routing table
//            r1.printRoutingTable();
//            r2.printRoutingTable();
		}
		catch(Exception ex){
			System.out.println(" Error executing simulation. Message: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

}
