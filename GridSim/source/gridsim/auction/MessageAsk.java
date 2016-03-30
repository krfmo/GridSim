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
 * This class represents an ask sent by a seller
 * to the auctioneer
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageAsk extends Message {
	private double price;
	
	/**
	 * Constructor
	 * @param auctionID the auction to which this ask belongs
	 * @param protocol the auction protocol used
	 * @param price the price of this ask
	 */
	public MessageAsk(int auctionID, 
			int protocol, 
			float price){
		super(auctionID, protocol);
		this.price = price;
	}
	
	/**
	 * Sets the price of this ask
	 * @param price the price of the ask
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
	 * Returns the price of this ask
	 * @return the price
	 */
	public double getPrice(){
		return price;
	}
	
	/**
	 * Converts message to String
	 * @return the string representation of the message 
	 */
	public String toString(){
		return super.toString() +  
			"\tPrice: 		" + price + "\n" + 
			"\tType: 		  ASK\n";
	}
}
