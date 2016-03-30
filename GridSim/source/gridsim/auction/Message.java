/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2006, The University of Melbourne, Australia
 */
package gridsim.auction;
import java.util.Hashtable;

/**
 * This class represents a single message exchanged 
 * among auctioneers, bidders or sellers.
 * 
 * @author       Marcos Dias de Assuncao
 * @since        GridSim Toolkit 4.0
 * 
 * @see gridsim.auction.AuctionTags
 */
public class Message {
	private int msgID;
	private int auctionID;
	private int sourceID;
	private int destinationID;
	private Hashtable attributes;
	private int auctionProtocol;
	private double sendAt = 0.0;
		
	//used to generate an automatic id for the message 
	private static int currentID = 0;
	
	/**
	 * This tag is used as destination of messages that are broadcast 
	 */
	public static int TO_ALL_BIDDERS = -1;
	
	/**
	 * 
	 * @param auctionID
	 * @param protocol
	 */
	public Message(int auctionID,int protocol){
		msgID = Message.generateID();
		this.auctionID = auctionID;
		this.auctionProtocol = protocol;
		attributes = new Hashtable();
	}
	
	/*
	 * This method just returns a new message ID
	 */
	private synchronized static int generateID(){
		return currentID++;
	}
	
	/**
	 * Returns an attribute of the message. <br>
	 * Additional attributes may be defined to a message. 
	 * @param key the key for the attribute to be retireved
	 * @return an Object which corresponds to the required attribute
	 */
	public Object getAttribute(Object key){
		return attributes.get(key);
	}
	
	/**
	 * Sets an attribute to the message. <br>
	 * Additional attributes may be defined to a message.
	 * @param key is the key used to retrieve the attribute.
	 * @param value is the object that corresponds to the attribute.
	 * @pre key != null && value != null
	 * @return <tt>true</tt> if the attribute was set correctly
	 */
	public boolean setAttribute(Object key, Object value){
		if(key == null || value == null)
			return false;
		
		attributes.put(key, value);
		return true;
	}
	
	/*
	 * Used to clone message. 
	 */	
	protected Hashtable getAttributes(){
		return attributes;
	}
	
	/*
	 * Used to clone a message.
	 */
	protected void setAttributes(Hashtable attrib){
		attributes = attrib;
	}
	
	/**
	 * Returns the message ID
	 * @return an integer that corresponds to the message ID.
	 */
	public int getMessageID(){
		return msgID;
	}
	
	/**
	 * Sets the message ID
	 * @param id the new message ID
	 * @pre id > 0
	 * @return <tt>true</tt> if the id was properly set
	 */
	protected boolean setMessageID(int id){
		if(id <= 0)
			return false;
		this.msgID = id;
		return true;
	}
	
	/**
	 * Sets the auction ID of this message
	 * @param auctionID the ID of the auction to which the message refers
	 * @pre auctionID > 0
	 * @return <tt>true</tt> if the auctionID was properly set 
	 */
	public boolean setAuctionID(int auctionID){
		if(auctionID <= 0)
			return false;
		
		this.auctionID = auctionID;
		return true;
	}
	
	/**
	 * Retruns the auction ID of this message
	 * @return the ID of the auction to which the message refers.
	 */
	public int getAuctionID(){
		return this.auctionID;
	}
	
	/**
	 * Returns the auction protocol.
	 * @return an int which corresponds to an identification of the auction protocol
	 */
	public int getProtocol(){
		return auctionProtocol;
	}
	
	/**
	 * Sets the auction protocol used this auction
	 * @param protocol
	 * @pre protocol > 0
	 * @return <tt>true</tt> if the protocol was properly set
	 */
	public boolean setProtocol(int protocol){
		if(protocol <= 0)
			return false;
		
		this.auctionProtocol = protocol;
		return true;
	}
	
	/**
	 * Sets the the GridSim entity that generates this message 
	 * @param id the ID of an Gridsim entity
	 * @pre id == some GridSim entity's ID 
	 * @return <tt>true</tt> if the source id was properly set
	 */
	public boolean setSourceID(int id){
		if(id < 0)
			return false;
		
		this.sourceID = id;
		return true;
	}
	
	/**
	 * Returns the ID of the entity that generated the message
	 * @return an int that is a GridSim entity ID.
	 */
	public int getSourceID(){
		return sourceID;
	}
	
	/**
	 * Sets the the GridSim entity that is the recipient of this message
	 * @param id the ID of an Gridsim entity
	 * @pre id == some GridSim entity's ID
	 * @return <tt>true</tt> if the destination id was properly set
	 */
	public boolean setDestinationID(int id){
		if(id < 0)
			return false;

		destinationID = id;
		return true;
	}
	
	/**
	 * Returns the ID of the entity that is the recipient of this message
	 * @return the ID of an Gridsim entity
	 */
	public int getDestinationID(){
		return destinationID;
	}
	
	/**
	 * The message can be scheduled to be sent at a given time.
	 * This way, the entity that dispatches such a message schedules
	 * the message to be sent after the specified time
	 * @param time the time to wait to send the message
	 * @pre time >= 0.0
	 * @return <tt>true</tt> if the scheduled time was properly set
	 */
	public boolean scheduleToBeSent(double time){
		if(time < 0.0D)
			return false;
		
		sendAt = time;
		return true;
	}
	
	/**
	 * Returns how much time to waited to schedule the message
	 * @return the simulation time to wait 
	 */
	public double getScheduleTime(){
		return sendAt;
	}
	
	/**
	 * Converts message to String
	 * @return the string representation of the message 
	 */
	public String toString(){
		return "Message\n" + 
			"\tMessage ID:  " + msgID + "\n" + 
			"\tAuction ID: 	" + auctionID + "\n" +
			"\tSource:		" + sourceID + "\n" +
			"\tDestination:	" + destinationID + "\n" +
			"\tProtocol:	" + auctionProtocol + "\n";
	}
}
