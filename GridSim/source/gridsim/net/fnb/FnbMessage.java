/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.net.fnb;

/**
 * This class provides a template for sending a message due to a packet
 * being dropped in the network.
 * @author Agustin Caminero
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.fnb.FnbMessageDropGridlet
 * @see gridsim.net.fnb.FnbMessageDropFile
 */
public interface FnbMessage
{
    /**
     * Sets an entity ID that is being dropped in the network. <br>
     * NOTE: the type of an entity depends on its subclasses. 
     * An entity could represents a Gridlet or a File.
     * @param id    an entity id
     * @see gridsim.net.fnb.FnbMessageDropGridlet#setEntityID(int)
     * @see gridsim.net.fnb.FnbMessageDropFile#setEntityID(int)
     */
    public void setEntityID(int id);

    /**
     * Gets an entity ID of this class.
     * @return an entity ID
     */
    public int getEntityID();
}

