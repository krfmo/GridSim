package auction.example03;

/*
 * Author: Marcos Dias de Assuncao
 * Date: May 2008
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 *              This example shows how to create user, resource, auctioneer
 *              and auction entities connected via a network topology,
 *              using link and router.
 */

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;
import gridsim.GridSimTags;
import gridsim.Gridlet;
import gridsim.auction.Auction;
import gridsim.auction.Auctioneer;
import gridsim.auction.ContinuousDoubleAuction;
import gridsim.auction.MessageAsk;
import gridsim.auction.MessageBid;
import gridsim.net.SimpleLink;

/**
 * This class implements a broker for the user. The broker bids containing jobs
 * from the users, behaves as an auctioneer, creates a double auction, and submits 
 * jobs after a match of ask and bid is found.
 *
 * @author Marcos Dias de Assuncao
 */
public class Broker extends Auctioneer {
	private ContinuousDoubleAuction auction = null;
	private int numUsers;
	private int finishedUsers = 0;
	protected static final int FINISHED_EXPERIMENTS = 7002;
	
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
            int MTU, int numUsers) throws Exception{
		super( name, new SimpleLink(name+"_link",baud_rate,delay, MTU) );
		this.numUsers = numUsers;
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
		return;
	}

	/**
	 * This method is called when a match for a double auction is found.
	 * OBS: We don't use double auctions in this example
	 * @see gridsim.auction.Auctioneer#onResponseToAsk(gridsim.auction.MessageAsk, gridsim.auction.MessageBid, double)
	 */
	public synchronized void onResponseToAsk(MessageAsk ask, MessageBid bid, double price){
		Gridlet job = (Gridlet)bid.getAttribute("job");

		System.out.println("\nA MATCH of an ASK with a BID has been performed:" +
				"\nBID details:" +
				"\nSender's ID: " + bid.getSourceID() +
				"\nPrice offered for executing the job: " + bid.getPrice() +
				"\nASK details:" +
				"\nSender's ID: " + ask.getSourceID() +
				"\nPrice asked for executing the job: " + ask.getPrice() +
				"\nPrice given by the auctioneer: " + price);

		gridletSubmit(job, ask.getSourceID());
	}
	
	public void body() {
		try {
			// waits 10 second to start the auction
			super.gridSimHold(10.0);

			auction = 
				new ContinuousDoubleAuction(super.get_name() + "_CDA", super.get_id(), 
					Double.MAX_VALUE, this.output);
			
			System.out.println("Creating Auction " + auction.getAuctionID());
			
			// add the auction
			super.addAuction(auction);
			
			// start the auction
			super.startAuction(auction.getAuctionID());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		super.body();
	}
	
	public void processOtherEvent(Sim_event ev) {
		
		if(ev.get_tag() == Broker.FINISHED_EXPERIMENTS) {
			finishedUsers++;
		}
		
		if(finishedUsers >= numUsers) {
			super.send(auction.get_id(), GridSimTags.SCHEDULE_NOW, GridSimTags.END_OF_SIMULATION);
			super.send(super.get_id(), 0, GridSimTags.END_OF_SIMULATION);
		}
	}
}
