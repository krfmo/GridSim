/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.profile;

/**
 * This class represents a resource partition for a multiple-partition
 * based availability profile.
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 * 
 * @see PartProfile
 * @see PartitionPredicate
 */

public class ResourcePartition {
   	private int partId; 	// this partition's identifier
	private int numPEs; 	// the initial number of PEs given to the partition

	// to check which jobs can be scheduled in this partition
	private PartitionPredicate predicate;
	
	/**
	 * Creates a new <tt>ResourcePartition</tt> object.
	 * @param queueId the partition ID
	 * @param numPE the number of PEs initially assigned to the partition 
	 * @param predicate the queue predicate
	 * @see PartitionPredicate
	 */
   	public ResourcePartition(int queueId, int numPE, 
   			PartitionPredicate predicate) {
   		this.partId = queueId;
   		numPEs = numPE;
   		this.predicate = predicate;
   	}

   	/**
   	 * Gets the partition ID 
   	 * @return the partition ID
   	 */
	public final int getPartitionId() {
		return partId;
	}
	
	/**
	 * Gets the number of PEs initially assigned to the partition
	 * @return the number of PEs initially assigned to the partition
	 */
	public final int getInitialNumPEs() {
		return numPEs;
	}

	/**
	 * Gets the predicate of this partition
	 * @return the predicate of this partition
	 */
	public final PartitionPredicate getPredicate() {
		return predicate;
	}
}