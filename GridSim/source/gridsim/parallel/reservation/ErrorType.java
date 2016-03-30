/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.reservation;

/**
 * Types of errors that can occur during reservation requests
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see ReservationMessage
 */
public enum ErrorType {

	/** Indicates that no error has happened */ 
	NO_ERROR("No error"),
	
	/** Indicates that the resource does not support reservations */
	NO_AR_SUPPORT("No support for reservations"),
	
	/** Indicates that the operation requested could not be fulfilled */
	OPERATION_FAILURE("Operation failed"),
	
	/** Indicates that the operation requested could not be fulfilled,
	 * but advance reservation options are provided */
	OPERATION_FAILURE_BUT_OPTIONS("Operation failed, but options provided");
	
	private String description;
	
	private ErrorType(String descr) {
		this.description = descr;
	}

	/**
	 * Returns a description of this error code.
	 * @return a description of this error code.
	 */
	public String getDescription() {
		return description;
	}
}
