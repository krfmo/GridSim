package auction.example01;

/*
 * Author: Marcos Dias de Assuncao
 * Date: March 2006
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 */

import eduni.simjava.Sim_event;
import gridsim.GridSim;
import gridsim.GridSimRandom;
import gridsim.GridSimStandardPE;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.Machine;
import gridsim.MachineList;
import gridsim.PE;
import gridsim.PEList;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;


/**
 * This example uses reverse auctions (ie. the lowest bid is the better and auctions
 * are started by buyers). In this case the user is the buyer who tries to
 * get resources to execute her jobs. The jobs are passed to her broker, who
 * acts as auctioneer. The broker initiates three different auctions, namelly
 * First-Price Sealed Bid Auction, English Auction and Dutch Auction.
 * To ensure that all jobs will be sent to a resource, we set reserve prices high
 * and the bids made by providers low.
 *
 * @author Marcos Dias de Assuncao
 */
public class ExampleAuction extends GridSim {
	private GridletList jobs;
	private Broker broker;

	/**
	 * Constructor
	 * @param name
	 * @param jobs
	 * @param broker
	 * @throws Exception
	 */
    public ExampleAuction(String name, GridletList jobs, Broker broker) throws Exception {
    	super(name);
    	this.jobs = jobs;
    	this.broker = broker;
    }

    /**
     *
     */
    public void body(){

    	// waits till resources have been registered
    	super.gridSimHold(20.0);

        // Send Application to the Broker.
        super.send(broker.get_id(),
        		GridSimTags.SCHEDULE_NOW,
                GridSimTags.EXPERIMENT, jobs);

		Sim_event ev = new Sim_event();

        // Accept user commands and process them
		sim_get_next(ev);
		while(ev.get_tag() != GridSimTags.EXPERIMENT){
			sim_get_next(ev);
		}

		jobs = (GridletList) ev.get_data();

        super.send(broker.get_id(), GridSimTags.SCHEDULE_NOW,
            	GridSimTags.END_OF_SIMULATION);

     	super.shutdownUserEntity();
    }


	/**
	 * Creates a list of gridlets for a user
	 * @return the list of gridlets
	 */
    private static GridletList createGridletList()
    {
        // Creates a container to store Gridlets
        GridletList list = new GridletList();

        int num_gridlets = 3;

        // We create Gridlets or jobs/tasks that correspond to a given
        // amount of MIPS to be executed
        double length;
        long file_size;
        long output_size;

        // We use GridSimRandom to define the size of imput and
        // for the jobs
        long seed = 11L*13*17*19*23+1;
        Random random = new Random(seed);

        // sets the PE MIPS Rating
        GridSimStandardPE.setRating(100);

        // creates the Gridlets
        for (int i=0; i < num_gridlets; i++){
        	// The job length is fixed in this example
        	length = 5000;

            // determines the Gridlet file size that varies within the range
            // 100 + (10% to 40%)
            file_size = (long) GridSimRandom.real(100, 0.10, 0.40,
                                    random.nextDouble());

            // determines the Gridlet output size that varies within the range
            // 250 + (10% to 50%)
            output_size = (long) GridSimRandom.real(250, 0.10, 0.50,
                                    random.nextDouble());

            // creates a new Gridlet object
            Gridlet gridlet = new Gridlet(i, length, file_size, output_size);

            // add the Gridlet into a list
            list.add(gridlet);
        }
        return list;
    }

    /**
     * Creates a grid resource
     * @param name a name for the resource
     * @param baud_rate the baud rate of the link to which the resource is attached
     * @throws Exception
     */
    private static void createGridResource(String name, double baud_rate) throws Exception {
        // Object of MachineList to store one or more Machines
        MachineList mList = new MachineList();

        // Create one Machine with its id, number of PEs and MIPS rating
        mList.add( new Machine(0, 1, 500) );

        // Create a ResourceCharacteristics object that stores the
        // properties of a Grid resource: architecture, OS, list of
        // Machines, allocation policy: time- or space-shared, time zone and
        // its price (G$/PE time unit).
        String arch = "Sun Ultra";
        String os = "Solaris";
        double time_zone = 0.0;
        double cost = 5;

        ResourceCharacteristics resConfig = new ResourceCharacteristics(
                arch, os, mList, ResourceCharacteristics.TIME_SHARED,
                time_zone, cost);

        // Finally, create a GridResource object.
        long seed = 11L*13*17*19*23+1;
        double peakLoad = 0.0;
        double offPeakLoad = 0.0;
        double holidayLoad = 0.0;

        LinkedList<Integer> weekends = new LinkedList<Integer>();
        weekends.add(new Integer(Calendar.SATURDAY));
        weekends.add(new Integer(Calendar.SUNDAY));
        LinkedList<Integer> holidays = new LinkedList<Integer>();

        ResourceCalendar calendar = new ResourceCalendar(time_zone, peakLoad,
        		offPeakLoad, holidayLoad, weekends, holidays, seed);

        AuctionResource gridRes = new AuctionResource(name, baud_rate, resConfig, calendar, null);
    }

    /**
     *
     * @param args
     */
	public static void main(String[] args) {
		try{
	    	Calendar calendar = Calendar.getInstance();
	    	boolean trace_flag = false;  // mean trace GridSim events
	        double baud_rate = 100.0; // lets be simple for a while

	        // First step: Initialize the GridSim package. It should be called
	        // before creating any entities. We can't run GridResource
	        // entity without initializing GridSim first. We will get run-time
	        // exception error.

	        // Initialize the GridSim package
	        System.out.println("Initializing GridSim package");
	        GridSim.init(1, calendar, trace_flag);

	       	createGridResource("Resource_1", baud_rate);
	       	createGridResource("Resource_2", baud_rate);
	       	createGridResource("Resource_3", baud_rate);
	       	GridletList jobs = createGridletList();

	       	Broker broker = new Broker("Broker", baud_rate);
	       	ExampleAuction user = new ExampleAuction("User", jobs, broker);

	        // Starts the simulation
	        GridSim.startGridSimulation();
		}
		catch(Exception ex){
			System.out.println(" Error executing simulation. Message: " + ex.getMessage());
		}
	}
}