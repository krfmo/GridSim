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
 * Contains various static command tags that indicate a type of action that
 * needs to be undertaken by auction entities when they receive or send events.
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.OneSidedAuction
 * @see gridsim.auction.DoubleAuction
 * @see gridsim.auction.AuctionTags
 */
public class AuctionTags {
	
	private static final int BASE = 900;

	/** Event used by messages that inform the start of an auction */
	public static final int AUCTION_INFORM_START = BASE + 1;	
	
	/** Represents the rejects of a proposal */
	public static final int AUCTION_REJECT_CALL_FOR_BID = BASE + 2;		
	
	/** It means a call for bids or proposals */
	public static final int AUCTION_CFP = BASE + 3;					
	
	/** Proposal or bid */
	public static final int AUCTION_PROPOSE = BASE + 4;
	
	/** Represents an ask sent to an auctioneer */
	public static final int AUCTION_ASK = BASE + 5;
	
	/** Used to inform that a bid has been accepted */
	public static final int AUCTION_ACCEPT_PROPOSAL = BASE + 6;		
	
	/** It is used to reject a proposal */
	public static final int AUCTION_REJECT_PROPOSAL = BASE + 7;	
	
	/** This code is used to events that inform the final outcome of an auction */
	public static final int AUCTION_INFORM_OUTCOME = BASE + 8;	
	
	/** Used to inform that a match for an ask has been found */
	public static final int AUCTION_MATCH_TO_ASK = BASE + 9;
	
	/** An auction must be post to an auctioneer. This event has this purpose. */
	public static final int AUCTION_POST = BASE + 10;

	/** Event code used to trigger or start an auction */
	public static final int AUCTION_START = BASE + 11;
	
	/** Event code used to inform auctioneer that an auction has finished */
	public static final int AUCTION_FINISHED = BASE + 12;
	
	/** Used to inform the auctioneer that an auction must be deleted */
	public static final int AUCTION_DELETE = BASE + 13;
	
	/** Used to stop an auction and to stop the execution of its <tt>body()</tt> method. */ 
	public static final int END_OF_AUCTION = BASE + 14;
	
	/** Internal event code internally by auctions to control timeout of rounds and auctions */
	public static final int AUCTION_TIMEOUT = BASE + 15;
	
	//Code for some kinds of auctions
	/** This code is used by First-Price Sealed Bid auctions */
	public static final int FIRST_PRICE_SEALED_AUCTION = 1;
	
	/** This code is used by Reverse First-Price Sealed Bid auctions */
	public static final int REVERSE_FIRST_PRICE_SEALED_AUCTION = 2;
	
	/** This code is used by English auctions */
	public static final int ENGLISH_AUCTION = 3;

	/** This code is used by Reverse English auctions */
	public static final int REVERSE_ENGLISH_AUCTION = 4;
	
	/** This code is used by Dutch auctions */
	public static final int DUTCH_AUCTION = 5;
	
	/** This code is used by Reverse Dutch auctions */
	public static final int REVERSE_DUTCH_AUCTION = 6;
	
	/** This code is used by Continuous Double auctions */
	public static final int CONTINUOUS_DOUBLE_AUCTION = 7;
	
	private AuctionTags() {
		throw new UnsupportedOperationException("AuctionTags cannot be instantiated");
	}
}
