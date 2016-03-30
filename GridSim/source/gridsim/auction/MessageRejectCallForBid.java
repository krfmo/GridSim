/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;

/**
 * This class represents a reject of call for bids
 * sent by the bidder to the auctioneer
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageRejectCallForBid extends Message{
	private int cfpID;
	private int round;
	
	/**
	 * Constructor
	 * @param auctionID the auction this message refers to
	 * @param protocol the auction protocol in use
	 * @param cfpID the CFP this message is a response to
	 * @param bidder the bidder or source that is sending the message
	 * @param round the round it refers to
	 */
	public MessageRejectCallForBid(int auctionID, int protocol, 
			int cfpID, int bidder, int round){
		super(auctionID, protocol);
		super.setSourceID(bidder);
		this.cfpID = cfpID;
		this.round = round;
	}
	
	/**
	 * Returns the CFP's ID
	 * @return the CFP's ID
	 */
	public int getCfpID(){
		return cfpID;
	}
	
	/**
	 * Returns the round
 	 * @return the round
	 */
	public int getRound(){
		return round;
	}
	
	/**
	 * Sets the bidder's ID or source of the message
	 * @param bidder the bidder's ID
	 * @pre bidder == some GridSim entity's id
	 * @return <tt>true</tt> if the bidder is properly set
	 */
	public boolean setBidder(int bidder){
		if(bidder < 0)
			return false;
		
		super.setSourceID(bidder);
		return true;
	}
	
	/**
	 * Returns the bidder's ID
	 * @return the bidder's ID
	 */
	public int getBidder(){
		return super.getSourceID();
	}
	
	/**
	 * Converts message to String
	 * @return the string representation of the message 
	 */
	public String toString(){
		return super.toString() +  
			"\tCFP ID: 		" + cfpID + "\n" +
			"\tRound: 		" + round + "\n" +			
			"\tType: 		  REJECT CFP\n";
	}

}
