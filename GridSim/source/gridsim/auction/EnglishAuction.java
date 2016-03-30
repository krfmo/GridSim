/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;


import java.util.LinkedList;

import eduni.simjava.Sim_port;

/**
 * This class represents an English Auction. 
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.OneSidedAuction
 * @see gridsim.auction.AuctionTags
 */

public class EnglishAuction extends OneSidedAuction {
    private MessageBid bestBid;
    private boolean shouldIncrease = false;
    private Object syncIncr = new Object();
    private LinkedList initialBidders;

    /**
     * Constructor
     * @param auctionName a name for the auction
     * @param auctioneerID the GridSim id of the auctioneer 
     * @param durationOfRounds simulation time of the duration of each round 
     * @param totalRound the number of rounds
     * @param output the auctioneer's output port   
     * @throws Exception
     * @see gridsim.GridSim
     */
    public EnglishAuction(String auctionName, int auctioneerID, 
    		double durationOfRounds,int totalRound, Sim_port output) throws Exception {
		super(auctionName, auctioneerID, AuctionTags.REVERSE_ENGLISH_AUCTION, 
				durationOfRounds,totalRound, output);
    }
    
    /**
     * Constructor
     * @param auctionName a name for the auction
     * @param durationOfRounds simulation time of the duration of each round 
     * @param totalRound the number of rounds
     * @throws Exception
     * @see gridsim.GridSim
     */
    public EnglishAuction(String auctionName, 
    		double durationOfRounds,int totalRound) throws Exception {
		super(auctionName, AuctionTags.REVERSE_ENGLISH_AUCTION, 
				durationOfRounds,totalRound);
    }
	
	/**
	 * This method is called when a round is started
	 * @see gridsim.auction.OneSidedAuction#onStart(int)
	 */
	public void onStart(int round){
		if(round == 1){
			initialBidders = super.getBidders();
			super.setCurrentPrice(super.getMinPrice());
		}
		
		MessageCallForBids msg = new MessageCallForBids(super
				.getAuctionID(), super.getAuctionProtocol(), 
				super.getCurrentPrice(), super.currentRound());
		
		msg.setAttributes(super.getAttributes());
		super.broadcastMessage(msg);
	}
	
	/**
	 * This method is invoked when a round finishes
	 * @see gridsim.auction.OneSidedAuction#onClose(int)
	 */
	public void onClose(int round){
		boolean stop = false;
		synchronized (syncIncr) {
			if (!shouldIncrease || round == super.getNumberOfRounds()) {
				stop = true;
			} else {
				shouldIncrease = false;
				double increase = ((super.getMaxPrice() - super.getMinPrice()) 
						/ (super.getNumberOfRounds() - 1));
				super.setCurrentPrice(super.getCurrentPrice() + increase);
			}

			if (stop) {
				if (bestBid != null) {
					double price = bestBid.getPrice();
					if (price >= super.getReservePrice()) {
						super.setFinalPrice(price);
						super.setWinner(bestBid.getBidder());
					} else {
						super.setFinalPrice(super.getCurrentPrice());
					}
				} else {
					super.setFinalPrice(super.getCurrentPrice());
				}
				closeAuction();
			}
		}
	}
	
	/**
	 * This method is called when the auction finishes
	 * @see gridsim.auction.OneSidedAuction#onStop()
	 */
	public void onStop(){
		int winner = super.getWinner();
		MessageInformOutcome iout = new MessageInformOutcome(super
				.getAuctionID(), super.getAuctionProtocol(), winner, super
				.getFinalPrice());
		
		super.setBidders(initialBidders);
		iout.setAttributes(super.getAttributes());
		
		super.broadcastMessage(iout);
	}
	
	/**
	 * This method is called when a bid is received. 
	 * @see gridsim.auction.OneSidedAuction#onReceiveBid(gridsim.auction.MessageBid)
	 */
	public void onReceiveBid(MessageBid bid){
		if (bid.getRound() == super.currentRound()) {
			if (bestBid == null) {
				bestBid = bid;
				synchronized (syncIncr) {
					shouldIncrease = true;
				}
			} else {
				if (bestBid.getRound() < bid.getRound()) {
					bestBid = bid;
					synchronized (syncIncr) {
						shouldIncrease = true;
					}
				}
			}
		}
	}
	
	/**
	 * Called when a reject bid is received.
	 * @see gridsim.auction.OneSidedAuction#onReceiveRejectCallForBid(gridsim.auction.MessageRejectCallForBid)
	 */
	public void onReceiveRejectCallForBid(MessageRejectCallForBid mrej){
		// remove bidder from the list. So, next call for bids will not be sent to this buyer
		Integer bidder = new Integer(mrej.getSourceID());
		LinkedList bidders = super.getBidders();
		if(bidders.contains(bidder))
			bidders.remove(bidder);
		super.setBidders(bidders);
	}
}
