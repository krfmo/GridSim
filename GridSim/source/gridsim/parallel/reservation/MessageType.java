/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

/**
 * Enumerator with the types of reservation messages. 
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see ReservationMessage
 */
public enum MessageType {
	
    /** Message of unknown type. */
    UNKNOWN(0, "Unknown"), 
    
    /** Requests for a new reservation. */
    CREATE(1,"Create"),
    
    /** Cancels an existing reservation. */
    CANCEL(2,"Cancel"),

    /** Modifies an existing reservation. */
    MODIFY(3,"Modify"),
    
    /** Queries the current status of a reservation. */
    STATUS(4,"Query/inform status"),
    
    /** Commits a reservation previously made. */
    COMMIT(5,"Commit"),
    
    /** Requests a list of free or empty time of a resource. */
    LIST_FREE_TIME(6,"List free time slots");

	private int value;
	private String description;
	
	private MessageType(int value, String description) {
	    // These types are also used as GridSim Tags, thus choose 
	    // a range of tags not used by other modules
		this.value = 5000 + value;
		this.description = description;
	}

	/**
	 * Returns integer value associated with this message type
	 * @return integer value associated with this message type
	 */
	public int intValue() {
		return value;
	}

	/**
	 * Returns a description of the type
	 * @return a string representation of the type
	 */
	public String getDescription() {
		return description;
	}
}
