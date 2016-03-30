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
 */

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;
import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.auction.Auction;
import gridsim.auction.AuctionTags;
import gridsim.auction.Auctioneer;
import gridsim.auction.MessageAsk;
import gridsim.auction.MessageBid;
import gridsim.auction.OneSidedAuction;
import gridsim.net.SimpleLink;

import java.util.LinkedList;

/**
 * This class implements a broker for the user. The broker receives
 * jobs from the user, behaves as an auctioneer, creates different
 * kinds of auctions, submits jobs after auction clears.
 * After the jobs are returned, the broker sends them back to the user
 * We used reverse auctions, that is, the job is allocated to the
 * resource provider who makes the lowest bid. To ensure that jobs
 * will be allocated, we set the reserve price high (from 10 to 90) and
 * bidders are willing to execute jobs for lower values (0 to 10).
 *
 * @author Marcos Dias de Assuncao
 *
 */
public class Broker extends Auctioneer {
	private LinkedList auctionsClient;
	private GridletList jobsReturned;
	private int finishedJobs = 0;
	private int notSubmittedJobs = 0;
	private LinkedList resourceIDList = null;
	private int userID = 0;

	/* This variable is used to denote an event that is triggered
	 * in case a job is not submitted. The reason for that is that
	 * there will be not Gridlet return event. So, the broker
	 * needs to know that the auction has finiched and the
	 * job has not been submitted.
	 */
	protected static final int CHECK_EXPERIMENT = 9999;


	/**
	 * Constructor
	 * @param name			a name for the broker
	 * @param baud_rate		the baud rate of the link to which the broker is attached
	 * @param delay			the delay of the link
	 * @param MTU			the maximum transfer unit of the link
	 * @throws Exception
	 * @see gridsim.auction.Auctioneer
	 * @see gridsim.GridSim
	 */
	public Broker(String name, double baud_rate, double delay,
            int MTU) throws Exception{
		super( name, new SimpleLink(name+"_link",baud_rate,delay, MTU) );
	}

	/**
	 * Returns the output port of the broker
	 * @return a port which corresponds to the output port of the entity
	 */
	public Sim_port getOutputPort(){
		return this.output;
	}

	/**
	 * This method is called when an auction is finished
	 * @see gridsim.auction.Auctioneer#onAuctionClose(gridsim.auction.Auction)
	 */
	public synchronized void onAuctionClose(Auction auction){
		OneSidedAuction osauc = (OneSidedAuction)auction;
		int winner = osauc.getWinner();

		Gridlet job = (Gridlet)auction.getAttribute("job");
		job.setUserID(super.get_id());

		System.out.println("Results of the auction" +
				"\nWinner ID: " + winner +
				"\nPrice paid for executing the job " + job.getGridletID() + ": "
				+ osauc.getFinalPrice() +
				"\nSubmitted by user " + this.userID);

		if(winner != -1){
			gridletSubmit(job,winner);
		}
		else{
			notSubmittedJobs++;
       		super.send(super.get_id(), GridSimTags.SCHEDULE_NOW,
       				Broker.CHECK_EXPERIMENT, null);
		}
	}

	/**
	 * This method is called when a match for a double auction is found.
	 * OBS: We don't use double auctions in this example
	 * @see gridsim.auction.Auctioneer#onResponseToAsk(gridsim.auction.MessageAsk, gridsim.auction.MessageBid, double)
	 */
	public synchronized void onResponseToAsk(MessageAsk ask, MessageBid bid, double price){
		return;
	}

	/**
	 * A class that extends Auctioneer needs to extend this method to deal
	 * with other events that the auctioneer is not able to deal with
	 * @param ev
	 */
	public void processOtherEvent(Sim_event ev){
        switch(ev.get_tag()){
	    	// receives the application and submits each one of its
	        // gridlets (jobs) to the broker.
           	case GridSimTags.EXPERIMENT:
        		// Requesting the GridInformationService
                // to send a list of resources
       	        resourceIDList = super.getGridResourceList();
       	        auctionsClient = (LinkedList)((LinkedList)ev.get_data());
            	jobsReturned = new GridletList();

                // Set Gridlets OwnerID as the BrokerID so that Resources
                // knows where to return them.
                this.userID = ev.get_src();

                long seed = 11L*13*17*19*23+1;

            	// To avoid a bunch of reply messages to the auctions,
            	// we add a delay between auctions
                Sim_uniform_obj genDelay = new Sim_uniform_obj("delay",60 * 6, 60 * 60 * 1, seed);

            	try{
            		for(int i=0; i<auctionsClient.size(); i++){
            			OneSidedAuction auction = (OneSidedAuction)auctionsClient.get(i);

            			auction.setAuctioneerID(this.get_id());
            			auction.setOutputPort(this.output);

            			auction.setBidders((LinkedList)resourceIDList.clone());

				        super.send(super.get_id(), GridSimTags.SCHEDULE_NOW, AuctionTags.AUCTION_POST, auction);
				        super.send(super.get_id(), genDelay.sample(),
					              		AuctionTags.AUCTION_START, new Integer(auction.getAuctionID()));

				        System.out.println(super.get_name() + " " +
				        		"starting the auction " + auction.getAuctionID() + " to acquire resource to execute job " +
					        		((Gridlet)auction.getAttribute("job")).getGridletID() + " from user " + userID);
					}
            	}
            	catch(Exception ex){
            			ex.printStackTrace();
            	}
                break;

                // deal with gridlet return
                case GridSimTags.GRIDLET_RETURN:
                	Gridlet result = (Gridlet)ev.get_data();
                	result.setUserID(this.userID);
                	jobsReturned.add(result);
                	this.finishedJobs++;
                	// sends a check experiment event to itself
               		super.send(super.get_id(), GridSimTags.SCHEDULE_NOW,
               				Broker.CHECK_EXPERIMENT, null);
                break;

                // verifies if all the jobs of the experiment have been finished
                case Broker.CHECK_EXPERIMENT:
                	if( (this.finishedJobs + this.notSubmittedJobs) == auctionsClient.size() ){
                		super.send(this.userID, GridSimTags.SCHEDULE_NOW,
                				GridSimTags.EXPERIMENT, jobsReturned);
                	}
                break;

            	default:
            		break;
            }
	}
}
