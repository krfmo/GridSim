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
import gridsim.auction.AuctionTags;
import gridsim.auction.DoubleAuction;
import gridsim.auction.MessageAsk;
import gridsim.auction.MessageBid;
import gridsim.auction.MessageCallForBids;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * This class represents a Continuos Double Auction. 
 * In Continuous double auctions, the auctioneer 
 * matches asks and bids. The auctioneer maintains 
 * a list of asks ordered in a increasing order 
 * and a list of bids ordered in an decreasing order. 
 * 
 * When the auctioneer receives an ask it proceeds as follows:
 * 1. It compares it with the first bid of the list. 
 *    If the price in the ask is lower than or equal 
 *    to the bid's value, it informs that seller and bidder 
 *    can trade at the price (price ask + price bid) / 2)
 * 2. Otherwise, the auctioneer adds the ask in the list. 
 *    
 * If the auctioneer receives a bid, it does the following:
 * 1. It compares it with the first ask of the list. 
 *    If the price in the ask is lower than or equal 
 *    to the bid's value, it informs that seller and 
 *    bidder can trade at the price (price ask + price bid) / 2). 
 * 2. Otherwise, the auctioneer adds the bid in the list.
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.DoubleAuction
 * @see gridsim.auction.AuctionTags
 */
public class ContinuousDoubleAuction extends DoubleAuction {
	private LinkedList asks;
	private LinkedList bids;
	private Comparator compAsks;
	private Comparator compBids;
	private Object syncObj = new Object();
	
	/**
	 * Constructor  
	 * @param auctionName 			name for the auction
	 * @param auctioneerID	 		auctioneer's ID
	 * @param durationOfAuction		time duration of auction
	 * @param output				the output port to be used by this auction
	 * @throws Exception	
	 */
	public ContinuousDoubleAuction(String auctionName, int auctioneerID, 
			double durationOfAuction, Sim_port output ) throws Exception {
		super(auctionName, auctioneerID,AuctionTags.CONTINUOUS_DOUBLE_AUCTION, 
				durationOfAuction, output);
		compAsks = new OrderAsksByPriceAsc();
		compBids = new OrderBidsByPriceDesc();
		asks = new LinkedList();
		bids = new LinkedList();
	}
	
	/**
	 * Constructor  
	 * @param auctionName 			name for the auction
	 * @param durationOfAuction		time duration of auction
	 * @throws Exception	
	 */
	public ContinuousDoubleAuction(String auctionName,  
			double durationOfAuction ) throws Exception {
		super(auctionName, AuctionTags.CONTINUOUS_DOUBLE_AUCTION, 
				durationOfAuction);
		compAsks = new OrderAsksByPriceAsc();
		compBids = new OrderBidsByPriceDesc();
		asks = new LinkedList();
		bids = new LinkedList();
	}

	/**
	 * This method is called when the auction is started
	 */
	public void onStart(){
		MessageCallForBids msg = new MessageCallForBids(super
				.getAuctionID(), super.getAuctionProtocol(),0D,1);
		super.broadcastMessage(msg);
	}
	
	/**
	 * Called when the auction finishes
	 */
	public void onStop(){
		synchronized (syncObj) {
			for(int i=0;i<asks.size();i++){
				super.match((MessageAsk)asks.get(i), null, 0);
			}
			
			for(int i=0;i<bids.size();i++){
				super.match(null, (MessageBid)bids.get(i), 0);
			}
					
			asks.clear();
			bids.clear();
		}
	}
	
	/**
	 * Called when a ask is sent by a provider.
	 * @param ask the ask sent by the provider
	 */
	public void onReceiveAsk(MessageAsk ask){
		synchronized (syncObj) {
			Collections.sort(bids,compBids);
			if(bids.size() > 0){
				MessageBid bid = (MessageBid)bids.getFirst();
				double priceAsk = ask.getPrice();
				double priceBid = bid.getPrice();
					
				if(priceBid >= priceAsk){
					double finalPrice = (priceAsk + priceBid) / 2;
					super.match(ask,bid,finalPrice);
					
	    			bids.remove(bid);
				}
				else{
					asks.add(ask);
				}
			}
			else{
				asks.add(ask);			
			}			
		}
	}
	
	/**
	 * Called when a bid is received.
	 * @param bid the bid received by the auctioneer
	 */
	public void onReceiveBid(MessageBid bid){
		synchronized (syncObj) {
			Collections.sort(asks,compAsks);
			if(asks.size() > 0){
				MessageAsk ask = (MessageAsk)asks.getFirst();
				double priceAsk = ask.getPrice();
				double priceBid = bid.getPrice();
					
				if(priceBid >= priceAsk){
					double finalPrice = (priceAsk + priceBid) / 2;
					super.match(ask, bid, finalPrice);
	    			asks.remove(ask);
				}
				else{
					bids.add(bid);
				}
			}
			else{
				bids.add(bid);			
			}			
		}
	}

	/**
	 * 
	 * @author Marcos
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	 class OrderAsksByPriceAsc implements Comparator
	 {
	 	/**
	 	 * 
	 	 */
	 	public OrderAsksByPriceAsc() {
	 		super();
	    }

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object a, Object b) throws ClassCastException
	    {
        	if(a == null){
        		return -1;
        	}
        	else if(b == null){
        		return 1;
        	}
        	else if(a == null && b == null ){
        		return 0; 
        	}
        	else{
            	MessageAsk aska = (MessageAsk) a;
	            MessageAsk askb = (MessageAsk) b;
	
	            Double d_c1 = new Double( aska.getPrice());
	            Double d_c2 = new Double( askb.getPrice());
	
	            return d_c1.compareTo(d_c2);
        	}
	 	}
	 }
	 
	 
	 /**
	 * 
	 * @author Marcos
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	 class OrderBidsByPriceDesc implements Comparator
	 {
	 	/**
	 	 * 
	 	 */
	 	public OrderBidsByPriceDesc() {
	 		super();
	    }

        /* (non-Javadoc)
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object a, Object b) throws ClassCastException
	    {
        	if(a == null){
        		return -1;
        	}
        	else if(b == null){
        		return 1;
        	}
        	else if(a == null && b == null ){
        		return 0; 
        	}
        	else{
            	MessageBid bida = (MessageBid) a;
	            MessageBid bidb = (MessageBid) b;
	
	            Double d_c1 = new Double( bida.getPrice());
	            Double d_c2 = new Double( bidb.getPrice());
	
	            return d_c2.compareTo(d_c1);
        	}
	 	}
	}
}
