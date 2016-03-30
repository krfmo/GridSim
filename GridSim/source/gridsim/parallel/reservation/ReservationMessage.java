/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

import java.util.logging.Level;
import java.util.logging.Logger;

import gridsim.parallel.log.LoggerEnum;
import gridsim.parallel.log.Logging;

/**
 * This class represents a single message exchanged between users 
 * and a resource providers to negotiate an advance reservation.
 * 
 * @author   Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see Reservation
 */

public class ReservationMessage {
	private static Logger logger = Logging.getLogger(LoggerEnum.PARALLEL);
	
	private int srcId;  			// id of the entity that sent the message 
	private int dstId;	 			// id of the entity that will receive the message
	private int msgId;  			// a unique id for the message
	private MessageType msgType; 	// the type of this message
	private ErrorType errorCode; 	// error code associated with the operation
	private Reservation reservation;  	// the negotiation this message is about
    private double price;			    // the price associate with this message				
    private static int lastUniqueID = 0;
    
	/**
	 * Instantiates a new object. 
	 * @param sourceId the id of entity that is the source of this message 
	 * @param destId the id of the entity that is the recipient of this message
	 * @throws IllegalArgumentException is thrown if the IDs are < 0
	 */
	public ReservationMessage(int sourceId, int destId) {
		this(sourceId);
		if(destId < 0) {
			throw new IllegalArgumentException("destId: "+ destId + " is invalid.");
		}
		this.dstId = destId;
	}
	
	/**
	 * Instantiates a new object. 
	 * @param sourceId the id of entity that is the source of this message 
	 * @throws IllegalArgumentException is thrown if the ID is < 0
	 */
	public ReservationMessage(int sourceId) {
		if(sourceId < 0){
			throw new IllegalArgumentException("sourceId: "+ sourceId + " is invalid.");
		}
		
		msgId = ++lastUniqueID;
		srcId = sourceId;
		msgType = MessageType.UNKNOWN;
		errorCode = ErrorType.NO_ERROR;
		reservation = null;
	}
	
	/**
	 * Instantiates a new object. 
	 * @param sourceId the id of entity that is the source of this message  
	 * @param reservation the reservation to which this message refers
	 * @throws IllegalArgumentException is thrown if the IDs are < 0 or 
	 * the reservation object is <code>null</code>
	 */
	public ReservationMessage(int sourceId, Reservation reservation) {
		this(sourceId);

		if(reservation == null){
			throw new IllegalArgumentException("Negotiation is invalid.");
		}
		dstId = -1;
		this.reservation = reservation;
	}
	
	/**
	 * Instantiates a new object.
	 * @param sourceId the id of entity that is the source of this message 
	 * @param destId the id of the entity that is the recipient of this message
	 * @param reservation the reservation to which this message refers
	 * @throws IllegalArgumentException is thrown if the IDs are < 0 or 
	 * the reservation object is <code>null</code>
	 */
	public ReservationMessage(int sourceId, int destId, Reservation reservation) {
		if(sourceId < 0 || destId < 0) {
			throw new IllegalArgumentException("IDs are invalid.");
		}
		if(reservation == null){
			throw new IllegalArgumentException("Negotiation is invalid.");
		}

		msgId = ++lastUniqueID;
		srcId = sourceId;
		dstId = destId;

		this.reservation = reservation;
		errorCode = ErrorType.NO_ERROR;
	}
	
    /**
     * Sets the source's id
     * @param sourceId the id of the source entity
     * @return <code>true</code> if it has been set successfully;
     * <code>false</code> otherwise
     */
    public boolean setSourceID(int sourceId) {
    	if(sourceId < 0) {
    		return false;
    	}
    	srcId = sourceId;
    	return true;
    }
    
    /**
     * Sets the destination's id
     * @param destId the id of the destination entity
     * @return <code>true</code> if it has been set successfully;
     * <code>false</code> otherwise
     */
    public boolean setDestinationID(int destId) {
    	if(destId < 0) {
    		return false;
    	}
    	dstId = destId;
    	return true;
    }

    /**
     * Sets the message type
     * @param type the message type
     */
    public void setMessageType(MessageType type) {
    	msgType = type;
    }
    
    /**
     * Sets the error code of this message
     * @param code the error code
     */
    public void setErrorCode(ErrorType code) {
    	errorCode = code;
    }
    
    /**
     * Gets the source id 
     * @return the source id
     */
    public int getSourceID() {
    	return srcId;
    }
    
    /**
     * Gets the destination ID
     * @return the destination id
     */
    public int getDestinationID() {
    	return dstId;
    }
    
    /**
     * Gets the message's ID
     * @return the message's id
     */
    public int getMessageID() {
    	return msgId;
    }
    
    /**
     * Gets the message's type
     * @return the message's type
     */
    public MessageType getMessageType() {
    	return msgType;
    }
    
    /**
     * Gets the error code of this message
     * @return the error code of this message
     */
    public ErrorType getErrorCode() {
    	return errorCode;
    }
    
    /**
     * Gets the reservation to which this message refers
     * @return the reservation object 
     */
    public Reservation getReservation() {
    	return reservation;
    }
    
    /**
     * Gets the ID of reservation to which this message refers
     * @return the reservation id of <code>-1</code> if not found 
     */
    public int getReservationID() {
    	return reservation != null ? reservation.getID() : -1;
    }
    
    /**
     * Gets the id of the reservation this message is related to
     * @return the reservation id or <code>-1</code> if the reservation is unknown
     */
    public int getNegotiationID() {
    	return reservation != null ? reservation.getID() : -1;
    }
    
    /**
     * Sets the price negotiated
     * @param price the price negotiated
     * @return <code>true</code> if successfully or <code>false</code> otherwise
     */
    public boolean setPrice(double price)  {
    	if(price <= 0.0) {
    		return false;
    	}
    	
    	this.price = price;
    	return true;
    }
    
    /**
     * Returns the price
     * @return price
     */    
    public double getPrice() {
    	return price;
    }

    /**
     * Returns the size in bytes for this message.
     * <b>NOTE:</b> This is used to get network statistics 
     * @return the size in bytes of this message
     */
    public int getMessageSize() {
		// We are assuming that every message has a size of 
		// about 200~220 bytes.
    	return (int)(200 + (Math.random()*21)); 
    }
    
    /**
     * Creates a response for this message. This method
     * creates a new message and sets the received of the message
     * created as the sender of this message and the sender of
     * the created message as the received of this message.
     * @return the message created.
     */
    public ReservationMessage createResponse() {
    	try {
    		ReservationMessage newMessage = 
    			new ReservationMessage(dstId, srcId, reservation);
    		newMessage.msgType = msgType; 
    	    newMessage.price =  price;
    		return newMessage;
    	}
    	catch (Exception ex) {
    		logger.log(Level.WARNING, "Error creating response.", ex);
    		return null;
    	}
    }
    
	/**
	 * Converts the negotiation message to a String. This
	 * method is used for debugging purposes
	 * @return a String representing the negotiation message
	 */
	public String toString() {
		return "{AR Message: " +
						"[Type = " + msgType.getDescription() + "]," +
						"[Source ID = " + srcId + "],"+
						"[Destination ID = " + dstId + "],"+
						"[Message ID = " + msgId + "],"+
						"[Reservation ID = " + 
						((reservation == null) ? -1 : reservation.getID()) + "],"+
						"[Error Code = " + errorCode.getDescription() + "],"+
						"[Price = " + price + "]}";
	}
}
