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
import eduni.simjava.Sim_port;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.IO_data;

import java.util.LinkedList;

/**
 * This class represents a double auction. Double auction are two-sided
 * auctions in which both buyers and sellers can submit bids and asks
 * respectivelly
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.AuctionTags
 */
public abstract class DoubleAuction extends Auction {
	private Object syncStep = new Object();
	
	// this attribute defines if the auction has been closed or not
	private boolean closed = false;
	
	// duration of a single round
	private double durationOfAuction;
	
	/**
	 * Default constructor
	 * @param auctionName A name for the auction
	 * @param auctioneerID the ID of the auctioneer
	 * @param auctionProtocol the auction protocol
	 * @param durationOfAuction duration of the auction in simulation time
	 * @param output the port to be used as output of messages
	 * @throws Exception
	 */
	public DoubleAuction(String auctionName, int auctioneerID, 
			int auctionProtocol, double durationOfAuction, Sim_port output)throws Exception {
		super(auctionName, auctioneerID, auctionProtocol, output);
		this.durationOfAuction = durationOfAuction;
	}
	
	/**
	 * Default constructor
	 * @param auctionName A name for the auction
	 * @param auctionProtocol the auction protocol
	 * @param durationOfAuction duration of the auction in simulation time
	 * @throws Exception
	 */
	public DoubleAuction(String auctionName, 
			int auctionProtocol, double durationOfAuction)throws Exception {
		super(auctionName, auctionProtocol);
		this.durationOfAuction = durationOfAuction;
	}
	
    /*
     * Used to avoid deadlock
     */
    private synchronized void setClosed(boolean value){
   		closed = value;
    }
    
    /*
     * Returns true if the auction is closed 
     * @return
     */
    private synchronized boolean isClosed(){
   		return closed;
    }
	
	/**
	 * Returns the duration of the auction
	 * @return the duration in simulation time of the auction
	 */
	protected double getDurationOfAuction(){
		return this.durationOfAuction;
	}
	
	/*
	 * TODO: I am not 100% whether this method is the 
	 * right way to do it. Are there any other attributes
	 * that are necessary when a match is found?
	 */
	/**
	 * Invoked by the subclasses when a match of an ask and a
	 * bid is found. This method triggers an event to the auctioneer,
	 * who knows that a match was done.
	 * @param ask the ask that was matched
	 * @param bid the bid that was matched
	 * @param price the trade price
	 */
	protected void match(MessageAsk ask, MessageBid bid, double price){
		synchronized(syncStep){ 
		
			//	sends message to the auctioneer informing about the match
			if(ask!=null){
				LinkedList mat = new LinkedList();
				mat.add(ask);
				mat.add(bid);
				mat.add(new Double(price));
				super.send(super.getAuctioneerID(), GridSimTags.SCHEDULE_NOW,
						AuctionTags.AUCTION_MATCH_TO_ASK, mat);
			}
			
			if(bid!=null){
				int winner = -1; // = nobody
				if(ask != null){
					winner = bid.getBidder();
				}
				MessageInformOutcome iout = new MessageInformOutcome(
					super.getAuctionID(), super.getAuctionProtocol(), 
					winner, price);

				/* TODO:
				 * For now, we are assuming that every message has a size of about 100 bytes.
				 * It would be better to consider some FIPA's encoding schema, for example.
				 * Please see: www.fipa.org
				 */
				super.sim_schedule(super.getOutputPort(),GridSimTags.SCHEDULE_NOW, 
	    				AuctionTags.AUCTION_INFORM_OUTCOME, new IO_data(iout,100,bid.getBidder()));
			}
		}
	}
	
    
	/**
	 * This method is called to start the auction and 
	 * initialize the necessary paramenters
	 */
    public void startAuction(){
    	if((super.getOutputPort() == null) || super.getAuctionID() == -1){
    		System.err.println(this.get_name() + 
    				"Error starting the auction. " + 
    				"The output port used by the auction is null or" +
    				"the auctioneer's ID was not provided!");
    		return;
    	}
        // default values
        setClosed(false);

        // broadcast a message to all bidders informing about the auction
        MessageInformStart mia = 
        	new MessageInformStart(super.getAuctionID(), 
        			super.getAuctionProtocol());
        
        broadcastMessage(mia);
        setStartingTime(GridSim.clock());
        
		synchronized(syncStep){ 
			onStart();
		}
		
		// create an event for timeout of the auction
		super.send(get_id(), this.durationOfAuction, 
			AuctionTags.AUCTION_TIMEOUT, null);
    }
    
    /**
     * This method sets the auction as closed 
     */
    protected void closeAuction(){
		synchronized(syncStep){ 
			if(!this.isClosed()){
				onStop();
					
				setClosed(true);
			    	
		    	//sends message to the auctioneer informing about the end of the auction
				super.send(super.getAuctioneerID(), GridSimTags.SCHEDULE_NOW,
						AuctionTags.AUCTION_FINISHED, new Integer(super.getAuctionID()));
			}
		}
    }
    
	 /**
     * Processes events or services that are available for this Auctioneer
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processEvent(Sim_event ev){
       
        switch ( ev.get_tag() )
        {
            case AuctionTags.AUCTION_TIMEOUT:
    			synchronized(syncStep){
    				if(!isClosed())
    					closeAuction();
    			}
        	break;
        	
            case AuctionTags.AUCTION_PROPOSE:
         		MessageBid bid = (MessageBid)ev.get_data();
    			synchronized(syncStep){ 
    				if(!isClosed())
    					this.onReceiveBid(bid);
    			}
            break;
            
            case AuctionTags.AUCTION_ASK:
         		MessageAsk ask = (MessageAsk)ev.get_data();
    			synchronized(syncStep){ 
    				if(!isClosed())
    					this.onReceiveAsk(ask);
    			}
            break;
            
            case AuctionTags.AUCTION_START:
    			synchronized(syncStep){ 
    				if(!isClosed())
    					this.startAuction();
    			}
        	break;
            
            // other unknown tags are processed by this method
            default:
            	processOtherEvent(ev);
            break;
        }
    }
    
    /**
     * Overrides this method when making a new and different policy.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
	protected void processOtherEvent(Sim_event ev){
		if (ev == null){
	    	System.out.println(super.get_name() + ".processOtherEvent(): " +
	        	"Error - an event is null.");
	        return;
	    }
	}
	
	//abstract methods to be implemented by different one-sided auctions
	/**
	 * Called when the auction is started
	 */
	public abstract void onStart();
	
	/**
	 * Called when the auction finishes
	 */
	public abstract void onStop();
	
	/**
	 * Called when a ask is sent by a provider.
	 * @param ask the ask sent by the provider
	 */
	public abstract void onReceiveAsk(MessageAsk ask);
	
	/**
	 * Called when a bid is received.
	 * @param bid the bid received by the auctioneer
	 */
	public abstract void onReceiveBid(MessageBid bid);

}
