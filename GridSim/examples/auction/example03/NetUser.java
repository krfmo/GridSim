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
 * This class was adapted from examples on network
 * package developed by: Anthony Sulistio
 *
 */


import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import eduni.simjava.distributions.ContinuousGenerator;
import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.auction.AuctionTags;
import gridsim.auction.MessageBid;
import gridsim.net.SimpleLink;

import java.util.Iterator;


/**
 * This class basically creates Gridlets and submits them to a
 * particular Broker in a network topology.
 * 
 * @author Marcos Dias de Assuncao
 */
class NetUser extends GridSim
{
    private int myId_;      			// my entity ID
    private GridletList jobs;			// list of jobs to be executed
    private GridletList receiveJobs;   	// list of received Gridlets
    private Broker broker;
	private ContinuousGenerator priceGenerator;
	private int secondsBetweenJobs = 60;

    /**
     * Creates a new NetUser object
     * @param name  this entity name
     * @param totalGridlet  total number of Gridlets to be created
     * @param baud_rate 	the baud rate of the link to which this user is attached
     * @param delay  		the delay of the link
     * @param MTU 			the maximum transfer unit of the link
     * @param broker		the broker associated with this user
     * @throws Exception    This happens when name is null or haven't
     *                      initialized GridSim.
     */
    NetUser(String name, int totalGridlet, double baud_rate, double delay,
            int MTU, Broker broker) throws Exception {
		super( name, new SimpleLink(name+"_link",baud_rate,delay, MTU) );

		this.receiveJobs = new GridletList();
        this.jobs = new GridletList();
        this.broker = broker;

        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);

        // Creates a list of Gridlets or Tasks for this grid user
        this.createGridlet(myId_, totalGridlet);
    	long seed = 11L*13*17*19*23+1;
    	priceGenerator = new Sim_uniform_obj("price",1, 2, seed);
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body() {
        // wait for a little while for about 10 seconds.
        // This to give a time for GridResource entities to register their
        // services to GIS (GridInformationService) entity.
        super.gridSimHold(10.0);
        double previous = 0;
        
        Iterator it = jobs.iterator();
        while(it.hasNext()) {
        	previous += secondsBetweenJobs;
        	
        	MessageBid bid = new MessageBid(0, AuctionTags.CONTINUOUS_DOUBLE_AUCTION, 
        			1, super.get_id()); 
        	
        	bid.setPrice(priceGenerator.sample());
        	bid.setAttribute("job", it.next());
        	
            super.send(broker.get_id(),
            		secondsBetweenJobs,
                    AuctionTags.AUCTION_PROPOSE, bid);
        }
        
        // hold for few period - few seconds since the Gridlets length are
        // quite huge for a small bandwidth
        super.gridSimHold(5);
        
        Object data;
        Gridlet gl;

        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);     // get the next available event
            data = ev.get_data();       // get the event's data

            // get the Gridlet data
            if (data != null && data instanceof Gridlet)
            {
                gl = (Gridlet) data;
                receiveJobs.add(gl);
            }

            // if all the Gridlets have been collected
            if (receiveJobs.size() == jobs.size()) {
                break;
            }
        }
        
        super.send(broker.get_id(), GridSimTags.SCHEDULE_NOW,
        					Broker.FINISHED_EXPERIMENTS);
		
        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();
        

//        super.send(broker.get_id(), GridSimTags.SCHEDULE_NOW,
//            	GridSimTags.END_OF_SIMULATION);
    }

    /**
     * Gets a list of received Gridlets
     * @return a list of received/completed Gridlets
     */
    public GridletList getGridletList() {
        return receiveJobs;
    }

    /**
     * This method will show you how to create Gridlets
     * @param userID        owner ID of a Gridlet
     * @param numGridlet    number of Gridlet to be created
     */
    private void createGridlet(int userID, int numGridlet)
    {
        int data = 500;   // 500KB of data
        for (int i = 0; i < numGridlet; i++)
        {
            // Creates a Gridlet
            Gridlet gl = new Gridlet(i, data, data, data);
            gl.setUserID(userID);

            // add this gridlet into a list
            this.jobs.add(gl);
        }
    }

} // end class

