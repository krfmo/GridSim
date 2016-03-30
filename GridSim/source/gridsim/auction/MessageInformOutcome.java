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
 * This class represents an message that
 * informs the outcome of an auction
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageInformOutcome extends Message{
	private int winner;
	private double price;
	
	/**
	 * Constructor 
	 * @param auctionID the auction this message belongs to
	 * @param protocol the auction protocol in use
	 * @param winner the winner of the auction
	 * @param price the price paid
	 */
	public MessageInformOutcome(int auctionID, int protocol, int winner, double price){
		super(auctionID, protocol);
		this.winner = winner;
		this.price = price;
	}
	
	/**
	 * Sets the price of the outcome
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
	 * Returns the price of the outcome 
	 * @return the price
	 */
	public double getPrice(){
		return price;
	}
	
	/**
	 * Returns the winner's ID
	 * @return the winner's ID
	 */
	public int getWinnerID(){
		return winner;
	}
	
	/**
	 * Sets the auctioneer that sent the message
	 * @param auctioneer the auctioneer ID
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
	 * Returns the auctioneer that sent the message
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
			"\tWinner: 		" + winner + "\n" +
			"\tType: 		  INFORM OUTCOME\n";
	}

}
