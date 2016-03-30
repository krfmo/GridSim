/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import eduni.simjava.Sim_port;

/**
 * This class represents a Reverse First-Price Sealed Bid auction. 
 * In a reverse auction, buyers start the auction and the
 * lowest bid is considered the best. 
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.OneSidedAuction
 * @see gridsim.auction.AuctionTags
 * 
 */
public class ReverseFirstPriceSealedBidAuction extends OneSidedAuction {
	//comparator used to order the bids by price in an increasing order 
    private Comparator comparator;
    
	// list of bids been made. The policy has to keep track of them to select
    // the best one when the auction finishes
	private LinkedList bids;
	
	/**
	 * 
	 * @param auctionName a name for the auction
	 * @param auctioneerID  the GridSim id of the auctioneer
	 * @param durationOfAuction simulation time of the duration of the auction 
     * @param output the auctioneer's output port  
	 * @throws Exception
	 */
	public ReverseFirstPriceSealedBidAuction(String auctionName, int auctioneerID, 
			double durationOfAuction, Sim_port output)throws Exception {
		super(auctionName,auctioneerID,AuctionTags.REVERSE_FIRST_PRICE_SEALED_AUCTION,
				durationOfAuction, 1, output);
		bids = new LinkedList();
		comparator = new OrderPrice();
	}
	
	/**
	 * 
	 * @param auctionName a name for the auction
	 * @param durationOfAuction simulation time of the duration of the auction 
	 * @throws Exception
	 */
	public ReverseFirstPriceSealedBidAuction(String auctionName, 
			double durationOfAuction)throws Exception {
		super(auctionName,AuctionTags.REVERSE_FIRST_PRICE_SEALED_AUCTION,
				durationOfAuction, 1);
		bids = new LinkedList();
		comparator = new OrderPrice();
	}

	/**
	 * This method is called when a round is started
	 * @see gridsim.auction.OneSidedAuction#onStart(int)
	 */
	public void onStart(int round) {
		super.setMinPrice(super.getReservePrice());
		super.setCurrentPrice(super.getReservePrice());

		// Creates a call for proposal that is broadcast to all bidders
		MessageCallForBids msg = new MessageCallForBids(super
				.getAuctionID(), super.getAuctionProtocol(), super
				.getMinPrice(), super.currentRound());
			
		msg.setAttributes(super.getAttributes());
		super.broadcastMessage(msg);
	}
	
	/**
	 * This method is called when the auction finishes
	 * @see gridsim.auction.OneSidedAuction#onStop()
	 */
	public void onStop() {
		int winner = super.getWinner();
			
		// broadcasts a message to all bidders informing about the 
		// outcome of the auction
		MessageInformOutcome iout = new MessageInformOutcome(super
				.getAuctionID(), super.getAuctionProtocol(), winner, super
				.getFinalPrice());

		super.broadcastMessage(iout);
	}

	/**
	 * This method is invoked when a round finishes
	 * @see gridsim.auction.OneSidedAuction#onClose(int)
	 */
	public void onClose(int round) {
			
		// our implementation of the first price sealed auction has
		// a single round. So, since we are using a reverse auction, the
		// lowest bid is the best one 
		MessageBid best = getFirstBid(-1);
			
		if (best != null) {
			double price = best.getPrice();
				
			// if the price of the bid is below the reserve price, it means
			// that the broker will accept the offer and will send the job 
			// to be executed by the resource provider
			if (price <= super.getReservePrice()) {
				super.setFinalPrice(price);
				super.setWinner(best.getBidder());
			} else {
				super.setFinalPrice(super.getCurrentPrice());
			}
		} else {
			// there is no best bid, what means that no resource provider
			// has bidden to execute the job
			super.setFinalPrice(super.getCurrentPrice());
		}
	}
   		
	/**
	 * This method is called when a bid is received. 
	 * @see gridsim.auction.OneSidedAuction#onReceiveBid(gridsim.auction.MessageBid)
	 */
	public void onReceiveBid(MessageBid bid) {
		// upon the receiving of a given bid, it is just 
		// added to the list to be later evaluated
		addBid(bid);
	}

	/**
	 * Called when a reject bid is received.
	 * @see gridsim.auction.OneSidedAuction#onReceiveRejectCallForBid(gridsim.auction.MessageRejectCallForBid)
	 */
	public void onReceiveRejectCallForBid(MessageRejectCallForBid mrej) {
		// do nothing...
	}
	
	private void addBid(MessageBid bid){
		synchronized(this){
			bids.add(bid);
		}
	}
	
	protected synchronized LinkedList getBids(){
		return bids;
	}
	
	
	/*
	 * @param round
	 * @param excBidder the bids done by excBidder will be excluded from the response
	 * @return
	 */
	private synchronized LinkedList getBids(int round, int excBidder){
		LinkedList aux = new LinkedList();
		synchronized(this){
			Iterator iter = bids.iterator();
			while(iter.hasNext()){
				MessageBid bid = (MessageBid)iter.next();
				if(bid.getRound() == round && bid.getBidder() != excBidder){
					aux.add(bid);
				}
			}
		}
		return aux;
	}
	
	/*
	 * Returns the best bid made at a given round. If round is -1, then all 
	 * auction rounds will be considered
	 * @param round
	 * @return
	 */
	private MessageBid getFirstBid(int round){
		LinkedList list = (round == -1) ? getBids() : getBids(round, -1);
		MessageBid best = null;

		synchronized(this){
			Collections.sort(list,comparator);
			try{
				best = (MessageBid)list.getFirst();
			}
			catch(Exception ex){
				best = null;
			}
		}
		return best;
	}
	
	/*
	 * This class implements comparator and is 
	 * used to order the list of bids by price 
	 *
	 * @author Marcos
	 */
	 class OrderPrice implements Comparator
	 {
	 	/**
	 	 * Default constructor
	 	 */
	 	public OrderPrice() {
	 		super();
	    }

        /**
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object a, Object b) throws ClassCastException
	    {
        	if(a == null){
        		return -1;
        	}
        	else if(b == null){
        		return 1;
        	}
        	else if(a == null && b == null ){
        		return 0; 
        	}
        	else{
	            MessageBid bida = (MessageBid) a;
	            MessageBid bidb = (MessageBid) b;

	            Double d_c1 = new Double( bida.getPrice());
	            Double d_c2 = new Double( bidb.getPrice());

	            return d_c1.compareTo(d_c2);
        	}
	 	}
	 }
}
