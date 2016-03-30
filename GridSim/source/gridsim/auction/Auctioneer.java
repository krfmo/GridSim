/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.net.Link;

import java.util.Hashtable;
import java.util.LinkedList;

/**
 * This class defines the basic behavious of an auctioneer
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.GridSim
 */
public abstract class Auctioneer extends GridSim {
	private Hashtable auctions;
	private Object syncSteps = new Object();

	/**
	 * Constructor
	 * @param name
	 * @throws Exception
	 */
	public Auctioneer(String name) throws Exception {
		super(name);
		auctions = new Hashtable();
	}

	/**
	 * Constructor
	 * @param name
	 * @param baudRate
	 * @throws Exception
	 */
	public Auctioneer(String name, double baudRate) throws Exception {
		super(name, baudRate);
		auctions = new Hashtable();
	}

	/**
	 * @param name
	 * @param link
	 * @throws Exception
	 */
	public Auctioneer(String name, Link link) throws Exception {
		super(name, link);
		auctions = new Hashtable();
	}
	
	/**
	 * Adds an auction to this auctioneer
	 * @param auction
	 */
	public void addAuction(Auction auction){
		Integer key = new Integer(auction.getAuctionID());
		synchronized(syncSteps){
			auctions.put(key, auction);
		}
	}
	
	/*
	 * Removes an auction that has finished
	 * @param auction the auction ID
	 */
	private void removeAuction(int auctionID){
		Integer key = new Integer(auctionID);
		synchronized(syncSteps){
			Auction auc = (Auction)auctions.get(key);
			auctions.remove(key);
			super.send(auc.get_id(), 
					GridSimTags.SCHEDULE_NOW,
					AuctionTags.END_OF_AUCTION);
		}
	}
	
	/**
	 * Starts a given auction already added to the auctioneer
	 * @param auctionID The auction's id
	 */
	public void startAuction(int auctionID){
		Integer key = new Integer(auctionID);
		synchronized(syncSteps){
			Auction auction = (Auction)auctions.get(key);
			if(auction!=null){
				super.send(auction.get_id(),
						GridSimTags.SCHEDULE_NOW, 
						AuctionTags.AUCTION_START);
			}
			else
				System.err.println("Auctioneer.startAution(): "+
						"This auction does not exist. Auction ID = " + auctionID);
		}
	}
	
	 /**
     * Handles external events that are coming to this Auctioneer entity.
     * <p>
     * The services or tags available for this resource are:
     * <ul>
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_POST} </li>
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_START} </li>
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_DELETE} </li>
     *      <li> {$link gridsim.auction.AuctionTags#AUCTION_FINISHED} </li>
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_PROPOSE} </li>
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_REJECT_CALL_FOR_BID} </li>      
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_ASK} </li>      
     *      <li> {@link gridsim.auction.AuctionTags#AUCTION_MATCH_TO_ASK} </li>      
     * </ul>
     * <br>
     * This method also calls these methods in the following order:
     * <ol>
     *      <li> {@link #processOtherEvent(Sim_event)} method
     * </ol>
     *
     * @pre $none
     * @post $none
     */
	public void body(){
        // Process events until END_OF_SIMULATION is received from the
        // GridSimShutdown Entity
		
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION){
                break;
            }

            // process the received event
            processEvent(ev);
        }

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();
	}
	
    /**
     * Processes events or services that are available to this Auctioneer
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        Auction auc = null;
        Message msg = null;
        Integer auctionID = null;
        
        switch ( ev.get_tag() )
        {
	    	case AuctionTags.AUCTION_POST:
	    		auc = (Auction)ev.get_data();
	    		addAuction(auc); // just adds the auction in the hashtable
	    	break;
        
	    	case AuctionTags.AUCTION_START:
	    		auctionID = (Integer)ev.get_data();
	    		startAuction(auctionID.intValue()); // starts the auction that was previously added
	    	break;
        
	    	case AuctionTags.AUCTION_DELETE:
	    		auctionID = (Integer)ev.get_data(); 
	    		removeAuction(auctionID.intValue());
	    	break;
	    	
	    	case AuctionTags.AUCTION_FINISHED:
	    		auc = (Auction)auctions.get((Integer)ev.get_data());
	    		synchronized(syncSteps){
	    			this.onAuctionClose(auc);
		    		// trigger the event to delete this auction from the list
		    		super.send(get_id(), GridSimTags.SCHEDULE_NOW,
		    				AuctionTags.AUCTION_DELETE, new Integer(auc.getAuctionID()));
	    		}
	    	break;
       
	    	// deal with proposal that has been sent
            case AuctionTags.AUCTION_PROPOSE:
            	Auction auction_p = null;
         		msg = (Message)ev.get_data();
            	MessageBid bid = (MessageBid)msg;
            	auction_p = (Auction)auctions.get(new Integer(bid.getAuctionID()));
	    		synchronized(syncSteps){
	            	if(auction_p != null){
	            		if(auction_p instanceof OneSidedAuction){
		            		if(((OneSidedAuction)auction_p).currentRound() == bid.getRound()){
		            			super.send(auction_p.get_id(), GridSimTags.SCHEDULE_NOW,
		            					AuctionTags.AUCTION_PROPOSE, bid);
		            		}
	            		}
	            		else{
	            			super.send(auction_p.get_id(), GridSimTags.SCHEDULE_NOW,
	            					AuctionTags.AUCTION_PROPOSE, bid);
	            		}
	            	}
	    		}
        	break;
        	
            case AuctionTags.AUCTION_REJECT_CALL_FOR_BID:
            	msg = (Message)ev.get_data();
            	MessageRejectCallForBid rej = (MessageRejectCallForBid)msg;
            	auc = (Auction)auctions.get(new Integer(rej.getAuctionID()));
            	synchronized(syncSteps){
	            	if(auc != null){
	            		if(auc instanceof OneSidedAuction)
	            		if(((OneSidedAuction)auc).currentRound() == rej.getRound()){
	            			super.send(auc.get_id(), GridSimTags.SCHEDULE_NOW,
		    	    				AuctionTags.AUCTION_REJECT_CALL_FOR_BID, rej);
	            		}
	            	}
            	}
        	break;
        	
	    	// deal with ask that has been sent
            case AuctionTags.AUCTION_ASK:
         		msg = (Message)ev.get_data();
            	MessageAsk ask = (MessageAsk)msg;
            	auc = (Auction)auctions.get(new Integer(ask.getAuctionID()));
            	if(auc != null){
            		if(auc instanceof DoubleAuction){
            			super.send(auc.get_id(), GridSimTags.SCHEDULE_NOW,
            					AuctionTags.AUCTION_ASK, ask);
            		}
            	}
        	break;
        	
	    	case AuctionTags.AUCTION_MATCH_TO_ASK:
	    		LinkedList mat = (LinkedList)ev.get_data();
	    		synchronized(syncSteps){
	    			MessageAsk a = (MessageAsk)mat.get(0);
	    			MessageBid b = (MessageBid)mat.get(1);
	    			double p = ((Double)mat.get(2)).doubleValue();
		    		// call the method to process match to ask
		    		this.onResponseToAsk(a,b,p);
	    		}
	    	break;
        	
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }
    
    /**
     * Overrides this method when making a new and different type of auctioneer.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
	protected void processOtherEvent(Sim_event ev){
		if (ev == null){
	    	System.out.println("Auctioneer.processEvent(): " + super.get_name() 
	    			+ " is has asked to process a null event.");
	        return;
	    }
	}
	
    /**
     * This method should be implemented to perform some auction after 
     * some auction has been finished. This method will be called whenever
     * one of the auctions that were initiated by this Auctionerr has been concluded
     * @param auction is the auction that has been concluded 
     */
    protected abstract void onAuctionClose(Auction auction);
    
	/**
	 * This method is called when a match for an ask was found by a double
	 * auction. The auction passes the ask, the bid that matches it and the
	 * price that they will use to trade
	 * @param ask the ask previously sent to the auctioneer
 	 * @param bid the bid that matches the ask
	 * @param price the price used to trade
	 * @post the bid can be null if a match was not found
	 */
    protected abstract void onResponseToAsk(MessageAsk ask, MessageBid bid, double price);
    

}
