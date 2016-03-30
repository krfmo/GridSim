/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;

import eduni.simjava.Sim_port;

/**
 * This class represents a Dutch Auction. 
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.OneSidedAuction
 * @see gridsim.auction.AuctionTags
 */
public class DutchAuction extends OneSidedAuction {
    private MessageBid bestBid;
	
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
    public DutchAuction(String auctionName, int auctioneerID, 
    		double durationOfRounds, int totalRound, Sim_port output)throws Exception {
    	super(auctionName, auctioneerID, AuctionTags.REVERSE_DUTCH_AUCTION, 
    	durationOfRounds, totalRound, output); 
    }
    
    /**
     * Constructor
     * @param auctionName a name for the auction
     * @param durationOfRounds simulation time of the duration of each round 
     * @param totalRound the number of rounds
     * @throws Exception
     * @see gridsim.GridSim
     */
    public DutchAuction(String auctionName, 
    		double durationOfRounds, int totalRound)throws Exception {
    	super(auctionName,  AuctionTags.REVERSE_DUTCH_AUCTION, 
    	durationOfRounds, totalRound); 
    }

	/**
	 * This method is called when a round is started
	 * @see gridsim.auction.OneSidedAuction#onStart(int)
	 */
    public void onStart(int round) {
		if (round == 1) {
			super.setCurrentPrice(super.getMaxPrice());
		}

		MessageCallForBids msg = new MessageCallForBids(super
				.getAuctionID(), super.getAuctionProtocol(), 
				super.getCurrentPrice(), super.currentRound());
		
		msg.setAttributes(super.getAttributes());
		super.broadcastMessage(msg);
	}
	
	/**
	 * This method is called when the auction finishes
	 * @see gridsim.auction.OneSidedAuction#onStop()
	 */
	public void onStop() {
		int winner = super.getWinner();
		MessageInformOutcome iout = new MessageInformOutcome(super
				.getAuctionID(), super.getAuctionProtocol(), winner, super
				.getFinalPrice());
		
		iout.setAttributes(super.getAttributes());
		super.broadcastMessage(iout);
	}

	/**
	 * This method is invoked when a round finishes
	 * @see gridsim.auction.OneSidedAuction#onClose(int)
	 */
	public void onClose(int round) {
		if (round >= super.getNumberOfRounds()) {
			if (bestBid == null)
				super.setFinalPrice(super.getCurrentPrice());
		} else {
			double decrease = super.getMaxPrice()
					/ (super.getNumberOfRounds() - 1);
			super.setCurrentPrice((float) super.getCurrentPrice()
					- decrease);
		}
	}

	/**
	 * This method is called when a bid is received. 
	 * @see gridsim.auction.OneSidedAuction#onReceiveBid(gridsim.auction.MessageBid)
	 */
	public void onReceiveBid(MessageBid bid) {
		double price = bid.getPrice();
		super.setFinalPrice(price);
		if (price >= super.getReservePrice()) {
			super.setWinner(bid.getBidder());
		}
		bestBid = bid;
		closeAuction();
	}

	/**
	 * Called when a reject bid is received.
	 * @see gridsim.auction.OneSidedAuction#onReceiveRejectCallForBid(gridsim.auction.MessageRejectCallForBid)
	 */
	public void onReceiveRejectCallForBid(MessageRejectCallForBid mrej) {
		// do nothing for now...
	}
	
}
