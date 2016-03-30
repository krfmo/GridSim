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
 * This class represents an call for bids sent
 * by the auctioneer
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageCallForBids extends Message {
	private double price;
	private int round; 
	
	/**
	 * Constructor
	 * @param auctionID the this CFP belongs to
	 * @param protocol the protocol used
	 * @param price the price of this CFP
	 * @param round the round this CFP refers to
	 */
	public MessageCallForBids(int auctionID, int protocol, 
			double price, int round){
		super(auctionID, protocol);
		this.round = round;
		this.price = price;
	}

	/**
	 * Constructor
	 * @param auctionID the this CFP belongs to
	 * @param protocol the protocol used
	 * @param price the price of this CFP
	 */
	public MessageCallForBids(int auctionID, 
			int protocol, float price){
		this(auctionID, protocol, price, -1);
	}
	
	/**
	 * Sets the price of the CFP
	 * @param price the price
	 * @pre price >= 0.0
	 * @return <tt>true</tt> if the price was properly set
	 */
	public boolean setPrice(float price){
		if(price < 0.0D)
			return false;
		
		this.price = price;
		return true;
	}
	
	/**
	 * Returns the price of the CFP
	 * @return the price
	 */
	public double getPrice(){
		return price;
	}
	
	/**
	 * Returns the round the CFP refers to
	 * @return the round
	 */
	public int getRound(){
		return round;
	}
	
	/**
	 * Sets the auctioneer or source of the CFP
	 * @param auctioneer the auctioneer's ID
	 * @pre bidder == some GridSim entity's id
	 * @return <tt>true</tt> if the auctioneer is properly set
	 */
	public boolean setAuctioneer(int auctioneer){
		if(auctioneer < 0)
			return false;
		
		super.setSourceID(auctioneer);
		return true;
	}
	
	/**
	 * Returns the auctioneer that sent the CFP
	 * @return the auctioneer's ID
	 */
	public int getAuctioneer(){
		return super.getSourceID();
	}
	
	/**
	 * Converts message to String
	 * @return the string representation of the message 
	 */
	public String toString(){
		return super.toString() +  
			"\tPrice: 		" + price + "\n" + 
			"\tRound: 		" + round + "\n" +			
			"\tType: 		  CALL FOR BIDS\n";
	}

}
