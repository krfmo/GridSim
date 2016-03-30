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
 * This class represents a message that is sent
 * by the auctioneer to bidders when an auction starts 
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class MessageInformStart extends Message {
	
	/**
	 * Constructor
	 * @param auctionID The auction this message belongs to
	 * @param protocol the auction protocol in use
	 */
	public MessageInformStart(int auctionID, int protocol){
		super(auctionID,protocol);
	}
	
	/**
	 * Sets the auctioneer or source of this message
	 * @param auctioneer the auctioeer's ID
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
	 * Returns the auctioneer or source of this message
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
			"\tType: 		  INFORM START\n";
	}

	
}
