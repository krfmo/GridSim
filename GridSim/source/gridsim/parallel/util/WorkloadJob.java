/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel.util;

import gridsim.Gridlet;

/**
 * This class represents job read from a a workload trace file. 
 * 
 * @author Marcos Dias de Assuncao
 * @since 5.0
 */
public class WorkloadJob {
	private Gridlet gridlet = null;
	private long submitTime = -1;

	/**
	 * Creates a new object.
	 * @param gl the job loaded from the file.
	 * @param subTime the time at which the job should be submitted
	 */
	public WorkloadJob(Gridlet gl, long subTime) {
		this.gridlet = gl;
		this.submitTime = subTime;
	}
	
	/**
	 * Returns the job loaded from the file.
	 * @return the job loaded from the file.
	 */
	public Gridlet getGridlet() {
		return gridlet;
	}
	
	/**
	 * Returns the time at which the job should be submitted
	 * @return the time at which the job should be submitted
	 */
	public long getSubmissionTime() {
		return submitTime;
	}
}
