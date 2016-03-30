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
 * This class represents a reject of a bid
 * sent by the auctioneer to a bidder
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageRejectBid extends Message{
	private int cfpID;
	private int bidID;
	private int round;
	
	/**
	 * Constructor
	 * @param cfpID the call for proposal to which the bid refers
	 * @param bidID the bid ID
	 * @param auctionID the auction this message belongs to
	 * @param protocol the auction protocol in use
	 * @param round the round this message refers to
	 */
	public MessageRejectBid(int cfpID, int bidID, 
			int auctionID, int protocol, int round){
		super(auctionID, protocol);
		this.bidID = bidID;
		this.cfpID = cfpID;
		this.round = round;
	}
	
	/**
	 * Returns the bid's ID
	 * @return the bids ID
	 */
	public int getBidID(){
		return bidID;
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
	 * Converts message to String
	 * @return the string representation of the message 
	 */
	public String toString(){
		return super.toString() +  
			"\tBid ID: 		" + bidID + "\n" + 
			"\tCFP ID: 		" + cfpID + "\n" +
			"\tRound: 		" + round + "\n" +			
			"\tType: 		  REJECT BID\n";
	}

}
