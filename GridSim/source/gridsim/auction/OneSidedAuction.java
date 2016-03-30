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

/**
 * This class represents an one-sided auction. One-sided auctions
 * send announcements of a good to be sold and receive bids from
 * bidders. It can have several rounds.
 *
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * @see gridsim.auction.Auction
 * @see gridsim.auction.AuctionTags
 */

public abstract class OneSidedAuction extends Auction {
	//reserve price is the minimum price expected to obtain in the auction
	private double reservePrice = 0f;
	
	//minimum price used by the auction. 
	private double minPrice = 0f;
	
	//current price is the current price achieved by the auction
	private double currentPrice = 0f;
	
	//current price is the current price achieved by the auction
	private double finalPrice = 0f;
	
	//maximum price used by the auction
	private double maxPrice = 0f;
	
	//duration of a single round
	private double durationOfRounds;
	
	//an auction may have several rounds
	private int totalRound = 1;
	
	//an auction can have multiple rounds. So, this variable indicates the current round
	private int currentRound = 0;
	
	// this attribute keeps the winner's ID
	private int winnerID = -1;
	
	private Object syncStep = new Object();
	
	// this attribute defines if the auction has been closed or not
	private boolean closed = false;
	
	
	/**
	 * Default constructor
	 * @param auctionName A name for the auction
	 * @param auctioneerID the ID of the auctioneer
	 * @param auctionProtocol the auction protocol
	 * @param durationOfRounds duration in simulation time of each round
	 * @param totalRound the number of rounds
	 * @param output the auctioneer's output port
	 * @throws Exception
	 */
	public OneSidedAuction(String auctionName, int auctioneerID, 
			int auctionProtocol, double durationOfRounds,
			int totalRound, Sim_port output)throws Exception {
		super(auctionName, auctioneerID, auctionProtocol, output);
		this.durationOfRounds = durationOfRounds;
		this.totalRound = totalRound; 
	}
	
	/**
	 * Default constructor
	 * @param auctionName A name for the auction
	 * @param auctionProtocol the auction protocol
	 * @param durationOfRounds duration in simulation time of each round
	 * @param totalRound the number of rounds
	 * @throws Exception
	 */
	public OneSidedAuction(String auctionName,  
			int auctionProtocol, double durationOfRounds,
			int totalRound)throws Exception {
		super(auctionName, auctionProtocol);
		this.durationOfRounds = durationOfRounds;
		this.totalRound = totalRound; 
	}
	
    /**
     * Sets the winner ID
     * @param winnerID
     * @pre winnerID == some GridSim entity's ID || winnerID == -1 if there's no winner
     * @return <tt>true</tt> if the winner's id is properly set
     */
    protected boolean setWinner(int winnerID){
    	if(winnerID < -1)
    		return false;
    	
    	this.winnerID = winnerID;
    	return true;
    }
    
    /**
     * Returns the winner's ID
     * @return the GridSim id of the winner
     */
    public int getWinner(){
    	return winnerID;
    }

	/**
	 * Returns the duration of a round
	 * @return the simulation time of a round
	 */
	protected double getDurationOfRounds(){
		return this.durationOfRounds;
	}
	
	/**
	 * Set the reserve price. The auctioneer may not sell
	 * the good for less than the value specified in reserve price
	 * @param price
	 * @pre price >= 0.0D
	 * @return <tt>true</tt> if the price is properly set
	 */
	public boolean setReservePrice(double price){
		if(price < 0.0D)
			return false;
		
		reservePrice = price;
		return true;
	}
	
	/**
	 * Returns the reserve price
	 * @return the reserve price of this auction
	 */
	public double getReservePrice(){
		return reservePrice;
	}
	
	/**
	 * Sets the final price achieved in the auction
	 * @param price
	 * @pre price >= 0.0D
	 * @return <tt>true</tt> if the price is properly set
	 */
	public boolean setFinalPrice(double price){
		if(price < 0.0D)
			return false;
		
		finalPrice = price;
		return true;
	}
	
	/**
	 * Returns the final price achieved by the auction
	 * @return the final price achieved
	 */
	public double getFinalPrice(){
		return finalPrice;
	}
	
	/**
	 * Returns the current round of the auction 
	 * @return the active round
	 */
	public synchronized int currentRound(){
		return currentRound;
	}
	
	/**
	 * Returns the number of rounds of the auction
	 * @return the number of rounds
	 */
	public int getNumberOfRounds(){
		return this.totalRound;
	}
	
	/**
	 * Sets the current price in the auction
	 * @param price
	 * @pre price >= 0.0D
	 * @return <tt>true</tt> if the price is properly set
	 */
	public boolean setCurrentPrice(double price){
		if(price < 0.0D)
			return false;
		
		this.currentPrice = price;
		return true;
	}
	
	/**
	 * Returns the current price of this auction
	 * @return the current price
	 */
	public double getCurrentPrice(){
		return currentPrice;
	}
	
	/**
	 * Sets the minimum price for the auction 
	 * @param price the minimun price for the auction
	 * @pre price >= 0.0D
	 * @return <tt>true</tt> if the price is properly set
	 */
	public boolean setMinPrice(double price){
		if(price < 0.0D)
			return false;
		
		this.minPrice = price;
		return true;
	}
	
	/**
	 * Returns the minimun price of the auction
	 * @return the minimun price
	 */
	public double getMinPrice(){
		return minPrice;
	}
	
	/**
	 * Sets the maximum price for the auction 
	 * @param price the maximum price for the auction
	 * @pre price >= 0.0D
	 * @return <tt>true</tt> if the price is properly set
	 */
	public boolean setMaxPrice(double price){
		if(price < 0.0D)
			return false;
		
		this.maxPrice = price;
		return true;
	}
	
	/**
	 * Returns the maximum price of the auction
	 * @return the maximum price
	 */
	public double getMaxPrice(){
		return maxPrice;
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
    	synchronized(syncStep){
	    	// default values
	        setClosed(false);
	
	        // broadcast a message to all bidders informing about the auction
	        MessageInformStart mia = 
	        	new MessageInformStart(super.getAuctionID(), super.getAuctionProtocol());
	        
	        broadcastMessage(mia);
	        
	        setStartingTime(GridSim.clock());
	        
			onStart(++this.currentRound);
	
			// creates events for timeout of the rounds
			for(int i=this.currentRound;i<=this.totalRound;i++){
				super.send(super.get_id(), durationOfRounds * i, 
						AuctionTags.AUCTION_TIMEOUT, new Integer(i));
			}
    	}
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
		
				/* TODO: these messages back to auctioneer might not be needed.
				 * It is needed to find a better way to do it. 
				 */
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
            	int round = ((Integer)ev.get_data()).intValue();
    			synchronized(syncStep){ 
    				if(!isClosed())
    					this.onClose(round);
    				
    				if( (this.currentRound < this.totalRound) && !isClosed()){
   						onStart(++this.currentRound);
    				}
    				else if(!isClosed()){
    					closeAuction();
    				}
    			}
        	break;
        	
            case AuctionTags.AUCTION_PROPOSE:
         		MessageBid bid = (MessageBid)ev.get_data();
    			synchronized(syncStep){
    				if(!isClosed())
    					this.onReceiveBid(bid);
    			}
            break;
            
            case AuctionTags.AUCTION_REJECT_CALL_FOR_BID:
            	MessageRejectCallForBid rej = (MessageRejectCallForBid)ev.get_data();
    			synchronized(syncStep){ 
    				if(!isClosed())
    					this.onReceiveRejectCallForBid(rej);
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
	 * Called when a round is started
	 * @param round the number of the round that has started
	 */
	public abstract void onStart(int round);
	
	/**
	 * Called when a round finishes
	 * @param round the round that has finished
	 */
	public abstract void onClose(int round);
	
	/**
	 * Called when the auction finishes
	 */
	public abstract void onStop();
	
	/**
	 * Called when a bid is received.
	 * @param bid the bid received by the auctioneer
	 */
	public abstract void onReceiveBid(MessageBid bid);
	
	/**
	 * Called when a reject bid is received.
	 * @param mrej the reject received by the auctioneer
	 */
	public abstract void onReceiveRejectCallForBid(MessageRejectCallForBid mrej);
    
}
