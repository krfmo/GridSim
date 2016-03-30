package auction.example02;

/*
 * Author: Marcos Dias de Assuncao
 * Date: March 2006
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
import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.auction.ReverseDutchAuction;
import gridsim.auction.ReverseEnglishAuction;
import gridsim.auction.ReverseFirstPriceSealedBidAuction;
import gridsim.net.SimpleLink;

import java.util.LinkedList;


/**
 * This class basically creates Gridlets and submits them to a
 * particular Broker in a network topology.
 * 
 * @author Marcos Dias de Assuncao
 */
class NetUser extends GridSim
{
    private int myId_;      			// my entity ID
    private LinkedList  auctions;      	// list of auctions to be posted
    private GridletList jobs;			// list of jobs to be executed
    private GridletList receiveJobs;   	// list of received Gridlets
    private Broker broker;


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
            int MTU, Broker broker) throws Exception
    {
		super( name, new SimpleLink(name+"_link",baud_rate,delay, MTU) );

		this.receiveJobs = new GridletList();
        this.jobs = new GridletList();
        this.broker = broker;
        this.auctions = new LinkedList();

        // Gets an ID for this entity
        this.myId_ = super.getEntityId(name);

        // Creates a list of Gridlets or Tasks for this grid user
        this.createGridlet(myId_, totalGridlet);

    	long seed = 11L*13*17*19*23+1;

        Sim_uniform_obj genReservePrice = new Sim_uniform_obj("price",1, 90, seed);

    	try{
    		int choice = 1;
    		for(int i=0; i<jobs.size(); i++){
    			if(choice == 1){
        			// creates a FPSA with duration of 120.0
	            	ReverseFirstPriceSealedBidAuction fpsa = new ReverseFirstPriceSealedBidAuction(
					      super.get_name() + "_FPSA_" + i, 120.0);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	fpsa.setReservePrice((double)(10 + genReservePrice.sample()));

			        // the auction has a referente to the job/gridlet
	            	fpsa.setAttribute("job",(Gridlet)jobs.get(i));

			        System.out.println(super.get_name() + " " +
			        		"creating a FPSA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + super.get_id());

			        auctions.add(fpsa);
    			}
    			else if(choice == 2){
			        // creates an English Auction with duration of 120.0 each round
			        // and maximum of 10 rounds
	            	ReverseEnglishAuction ea = new ReverseEnglishAuction(
					      super.get_name() + "_EA_" + i, 120.0, 5);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	ea.setReservePrice((double)(10 + genReservePrice.sample()));
	            	ea.setMaxPrice(90);
	            	ea.setMinPrice(0);

			        // the auction has a referente to the job/gridlet
	            	ea.setAttribute("job",(Gridlet)jobs.get(i));

			        System.out.println(super.get_name() + " " +
			        		"creating an EA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + super.get_id());

			        auctions.add(ea);
    			}
    			else if(choice == 3){
			        // creates a Dutch Auction with duration of 120.0 each round
			        // and maximum of 10 rounds
	            	ReverseDutchAuction da = new ReverseDutchAuction(
					      super.get_name() + "_DA_" + i,120.0, 5);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	da.setReservePrice((double)(10 + genReservePrice.sample()));
	            	da.setMaxPrice(90);
	            	da.setMinPrice(0);

			        // the auction has a referente to the job/gridlet
	            	da.setAttribute("job",(Gridlet)jobs.get(i));

			        System.out.println(super.get_name() + " " +
			        		"creating a DA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + super.get_id());

			        auctions.add(da);

			        choice = 0;
    			}
    			choice++;
    		}
    	}
    	catch(Exception ex){
			ex.printStackTrace();
    	}
    }

    /**
     * The core method that handles communications among GridSim entities.
     */
    public void body()
    {
        // wait for a little while for about 10 seconds.
        // This to give a time for GridResource entities to register their
        // services to GIS (GridInformationService) entity.
        super.gridSimHold(10.0);

        ////////////////////////////////////////////////
        // SUBMIT Gridlets

        super.send(broker.get_id(),
        		GridSimTags.SCHEDULE_NOW,
                GridSimTags.EXPERIMENT, auctions);

        // hold for few period - few seconds since the Gridlets length are
        // quite huge for a small bandwidth
        super.gridSimHold(5);

		Sim_event ev = new Sim_event();

        // waits until experiment results are sent back
		sim_get_next(ev);
		while(ev.get_tag() != GridSimTags.EXPERIMENT){
			sim_get_next(ev);
		}

		receiveJobs = (GridletList) ev.get_data();

        ////////////////////////////////////////////////////////
        // shut down I/O ports
        shutdownUserEntity();
        terminateIOEntities();

        super.send(broker.get_id(), GridSimTags.SCHEDULE_NOW,
            	GridSimTags.END_OF_SIMULATION);
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

