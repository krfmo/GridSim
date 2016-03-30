package auction.example01;

/*
 * Author: Marcos Dias de Assuncao
 * Date: March 2006
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 */

import eduni.simjava.Sim_event;
import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.GridletList;
import gridsim.auction.Auction;
import gridsim.auction.AuctionTags;
import gridsim.auction.Auctioneer;
import gridsim.auction.ReverseFirstPriceSealedBidAuction;
import gridsim.auction.MessageAsk;
import gridsim.auction.MessageBid;
import gridsim.auction.OneSidedAuction;
import gridsim.auction.ReverseDutchAuction;
import gridsim.auction.ReverseEnglishAuction;

import java.util.LinkedList;

/**
 * This class implements a broker for the user. The broker receives
 * 3 jobs from the user, behaves as an auctioneer, creates 3
 * and different kinds of auctions, submits jobs after auction clears.
 * After the jobs are returned, the broker sends them back to the user
 * We used reverse auctions, that is, the job is allocated to the
 * resource provider who makes the lowest bid. To ensure that jobs
 * will be allocates, we set the reserve price high (from 10 to 100) and
 * bids low (from 0 to 10).
 *
 * @author Marcos Dias de Assuncao
 *
 */
public class Broker extends Auctioneer {
	private GridletList jobs;
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
	 * @throws Exception
	 * @see gridsim.auction.Auctioneer
	 * @see gridsim.GridSim
	 */
	public Broker(String name, double baud_rate) throws Exception{
		super(name,baud_rate);
	}

	/**
	 * This method is called when an auction is finished
	 * @see gridsim.auction.Auctioneer#onAuctionClose(gridsim.auction.Auction)
	 */
	public void onAuctionClose(Auction auction){
		OneSidedAuction osauc = (OneSidedAuction)auction;
		int winner = osauc.getWinner();

		Gridlet job = (Gridlet)auction.getAttribute("job");

		System.out.println("Results of the auction" +
				"\nWinner ID: " + winner +
				"\nPrice paid for executing the job " + job.getGridletID() + ": "
				+ osauc.getFinalPrice());

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
	public void onResponseToAsk(MessageAsk ask, MessageBid bid, double price){
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
            	jobs = (GridletList)ev.get_data();
            	jobsReturned = new GridletList();

                // Set Gridlets OwnerID as the BrokerID so that Resources
                // knows where to return them.
                userID = ev.get_src();

                // sets the broker as the gridlets' user
            	for(int i=0; i<jobs.size(); i++){
            		((Gridlet)jobs.get(i)).setUserID(get_id());
            	}

            	// To avoid a bunch of reply messages to the auctions,
            	// we add a delay between auctions
                Sim_uniform_obj genDelay = new Sim_uniform_obj("delay",120,720);

            	try{
            		int i = 0;

			        // creates a FPSA with duration of 60.0
	            	ReverseFirstPriceSealedBidAuction fpsa = new ReverseFirstPriceSealedBidAuction(
					      super.get_name() + "_FPSA", super.get_id(),60.0, this.output);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	fpsa.setReservePrice((double)(10 + (90 * Math.random())));

			        // the auction has a referente to the job/gridlet
	            	fpsa.setAttribute("job",(Gridlet)jobs.get(i));

			        // set the list of current bidders for this auction
	            	fpsa.setBidders((LinkedList)resourceIDList.clone());

			        super.send(super.get_id(), GridSimTags.SCHEDULE_NOW, AuctionTags.AUCTION_POST, fpsa);
			        super.send(super.get_id(), genDelay.sample(),
			              		AuctionTags.AUCTION_START, new Integer(fpsa.getAuctionID()));

			        System.out.println(super.get_name() + " " +
			        		"starting a FPSA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + userID);


			        i++;

			        // creates an English Auction with duration of 60.0 each round
			        // and maximum of 10 rounds
	            	ReverseEnglishAuction ea = new ReverseEnglishAuction(
					      super.get_name() + "_EA", super.get_id(),60.0, 10,this.output);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	ea.setReservePrice((double)(10 + (90 * Math.random())));
	            	ea.setMaxPrice(90);
	            	ea.setMinPrice(0);

			        // the auction has a referente to the job/gridlet
	            	ea.setAttribute("job",(Gridlet)jobs.get(i));

			        // set the list of current bidders for this auction
	            	ea.setBidders((LinkedList)resourceIDList.clone());

			        super.send(super.get_id(), GridSimTags.SCHEDULE_NOW, AuctionTags.AUCTION_POST, ea);
			        super.send(super.get_id(), genDelay.sample(),
			              		AuctionTags.AUCTION_START, new Integer(ea.getAuctionID()));

			        System.out.println(super.get_name() + " " +
			        		"starting a EA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + userID);


			        i++;


			        // creates a Dutch Auction with duration of 60.0 each round
			        // and maximum of 10 rounds
	            	ReverseDutchAuction da = new ReverseDutchAuction(
					      super.get_name() + "_DA", super.get_id(),60.0, 10, this.output);

			        // To make sure that there will be offers to all jobs,
			        // we set the reserve price to something up to 100
			        // and offers at 10 maximum
	            	da.setReservePrice((double)(10 + (90 * Math.random())));
	            	da.setMaxPrice(90);
	            	da.setMinPrice(0);

			        // the auction has a referente to the job/gridlet
	            	da.setAttribute("job",(Gridlet)jobs.get(i));

			        // set the list of current bidders for this auction
	            	da.setBidders((LinkedList)resourceIDList.clone());

	            	// sends events to auctioneer to post and start the auction
			        super.send(super.get_id(), GridSimTags.SCHEDULE_NOW, AuctionTags.AUCTION_POST, da);
			        super.send(super.get_id(), genDelay.sample(),
			              		AuctionTags.AUCTION_START, new Integer(da.getAuctionID()));

			        System.out.println(super.get_name() + " " +
			        		"starting a DA to acquire resource to execute job " +
			        		((Gridlet)jobs.get(i)).getGridletID() + " from user " + userID);

            	}
            	catch(Exception ex){
            			ex.printStackTrace();
            		}
                break;

                // deal with gridlet return
                case GridSimTags.GRIDLET_RETURN:
                	Gridlet result = (Gridlet)ev.get_data();
                	result.setUserID(userID);
                	jobsReturned.add(result);
                	this.finishedJobs++;
                	// sends a check experiment event to itself
               		super.send(super.get_id(), GridSimTags.SCHEDULE_NOW,
               				Broker.CHECK_EXPERIMENT, null);
               	break;

                // verifies if all the jobs of the experiment have been finished
                case Broker.CHECK_EXPERIMENT:
                	if( (this.finishedJobs + this.notSubmittedJobs) == jobs.size() ){
                		super.send(userID, GridSimTags.SCHEDULE_NOW,
                				GridSimTags.EXPERIMENT, jobsReturned);
                	}
                break;

            	default:
            		break;
            }
	}
}
