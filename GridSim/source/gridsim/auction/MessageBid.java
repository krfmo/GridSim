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
 * This class represents a bid made by a bidder
 * to the auctioneer
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageBid extends Message {
	private int cfpID;
	private double price;
	private int round;
	
	/**
	 * Constructor
	 * @param auctionID the auction to which this bid belongs
	 * @param protocol the protocol used
	 * @param cfpID the call for proposal to which this bid is a response
	 * @param bidder the bidder that is making this bid
	 * @param round the round the bid refers to
	 */
	public MessageBid(int auctionID, int protocol, 
			int cfpID, int bidder, int round){
		super(auctionID,protocol);
		super.setSourceID(bidder);
		this.cfpID = cfpID;
		this.round = round;
	}
	
	/**
	 * Constructor
	 * @param auctionID the auction to which this bid belongs
	 * @param protocol the protocol used
	 * @param cfpID the call for proposal to which this bid is a response
	 * @param bidder the bidder that is making this bid
	 */
	public MessageBid(int auctionID, int protocol, 
			int cfpID, int bidder){
		this(auctionID, protocol, cfpID, bidder, -1);
	}
	
	/**
	 * Sets the price offered in the bid
	 * @param price the price
	 * @pre price >= 0.0
	 * @return <tt>true</tt> if the price was properly set
	 */
	public boolean setPrice(double price){
		if(price < 0.0D)
			return false;
		
		this.price = price;
		return true;
	}
	
	/**
	 * Returns the price offered in the bid
	 * @return the price
	 */
	public double getPrice(){
		return price;
	}
	
	/**
	 * Returns the Call for Proposal's ID the bid refers to
	 * @return the CFP's ID
	 */
	public int getCfpID(){
		return cfpID;
	}
	
	/**
	 * Sets the round the bid refers to
	 * @param round the round
	 * @pre round >= 1
	 * @return <tt>true</tt> if the round is properly set
	 */
	public boolean setRound(int round){
		if(round < 1)
			return false;
		
		this.round = round;
		return true;
	}
	
	/**
	 * Returns the round the bid refers to
	 * @return the round
	 */
	public int getRound(){
		return round;
	}
	
	/**
	 * Set the bidder or source of this bid
	 * @param bidder the GridSim of the bidder
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
	 * Returns the bidder or source of this bid
	 * @return the GridSim entity
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
			"\tPrice: 		" + price + "\n" + 
			"\tCFP ID: 		" + cfpID + "\n" +
			"\tRound: 		" + round + "\n" +			
			"\tType: 		  BID\n";
	}
}
