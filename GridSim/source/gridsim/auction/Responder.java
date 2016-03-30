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
 * This interface must be implemented by the
 * class responsible for defining the other side
 * of an auction (ie. not the auctioneer's side)
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 * @see gridsim.auction.Message
 */
public interface Responder {
	/**
	 * This method is invoked by the observer 
	 * when a Call for Bids is received 
	 * @param msg the Call for Bids
	 * @return the message to be sent back to the auctioneer by the observer
	 */
	Message onReceiveCfb(MessageCallForBids msg);
	
	/**
	 * This method is invoked by the observer when a 
	 * message informing the outcome is received
	 * @param msg the message informing the outcome
	 * @return the message to be sent back to the auctioneer by the observer
	 */
	Message onReceiveInformOutcome(MessageInformOutcome msg);
	
	/**
	 * This message is invoked when the observer receives a message
	 * rejecting a proposal previously sent 
	 * @param msg the reject message
	 * @return the message to be sent back to the auctioneer by the observer
	 */
	Message onReceiveRejectProposal(MessageRejectBid msg);
	
	/**
	 * This message is invoked when the observer receives a message
	 * informing about the start of an auction
	 * @param inform the message informing the start of the auction
	 * @return the message to be sent back to the auctioneer by the observer
	 */
	Message onReceiveStartAuction(MessageInformStart inform);
}
