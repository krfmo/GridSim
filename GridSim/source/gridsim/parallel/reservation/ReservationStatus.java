/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

/**
 * Enumerator that represents the possible status of an advance reservation
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public enum ReservationStatus {
	
	/**
	 * Status unknown.
	 */
	UNKNOWN(-1, "Unknown"),
	
	/**
	 * Reservation has been made, but not confirmed.
	 */
	NOT_COMMITTED(0, "Not confirmed"),
	
	/**
	 * Reservation has been made and confirmed.
	 */
    COMMITTED(1, "Confirmed"),
    
    /**
	 * Reservation is in progress.
	 */
    IN_PROGRESS(2, "In progress"),
    
    /**
	 * Reservation has completed.
	 */
    FINISHED(3, "Finished"),
    
    /**
	 * Reservation has expired.
	 */
    EXPIRED(4, "Expired"),
    
    /**
	 * Reservation has been cancelled.
	 */
    CANCELLED(5, "Cancelled"),
    
    /**
	 * Reservation has failed.
	 */
    FAILED(6, "Failed");

	private String description;
	private int value;
	
	private ReservationStatus(int value, String description) {
		this.value = value;
		this.description = description;
	}
	
	/**
	 * Returns the status description.
	 * @return the status description.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the integer value for this status.
	 * @return the integer value for this status.
	 */
	public int intValue() {
		return this.value;
	}
	
	/**
	 * Creates an string representation of the status.
	 * @return an string representation of the status.
	 */
	public String toString() {
		return description;
	}
}
