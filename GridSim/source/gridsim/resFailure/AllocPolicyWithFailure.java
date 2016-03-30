/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Created on: Nov 2006.
 * Copyright (c) 2007, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.resFailure;

/**
 * The structure of an allocation policy supporting resource failure.
 * Use this class in addition to {@link gridsim.AllocPolicy} class.
 * @author       Agustin Caminero
 * @since        GridSim Toolkit 4.1
 * @see gridsim.AllocPolicy
 */

public interface AllocPolicyWithFailure
{
    /**
     * Sets the status of all Gridlets in this resource to <tt>FAILED</tt>.
     * Then sends them back to users, and clean up the relevant lists.
     */
    void setGridletsFailed();

    /**
     * Sets the status of all Gridlets in this machine to <tt>FAILED</tt>.
     * Then sends them back to users, and clean up the relevant lists.
     * @param failedMachID  the id of the failed machine
     */
    void setGridletsFailed(int failedMachID);

} 
