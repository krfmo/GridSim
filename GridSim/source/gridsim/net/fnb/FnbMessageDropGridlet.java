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

import  gridsim.net.fnb.FnbMessage;

/**
 * This class contains a Gridlet ID, that is currently being
 * dropped in the network. 
 * This class is primarily used by the {@link gridsim.net.fnb.FnbOutput} class. 
 * @author Agustin Caminero
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.fnb.FnbOutput
 */
public class FnbMessageDropGridlet implements FnbMessage
{
    private int gridletID_;

    /**
     * A constructor
     * @param gridletID    a gridlet ID
     */
    public FnbMessageDropGridlet (int gridletID)
    {
        gridletID_ = gridletID;
    }

    /**
     * Sets a gridlet ID that is being dropped in the network.
     * @param gridletID    a gridlet ID
     */
    public void setEntityID(int gridletID)
    {
        gridletID_ = gridletID;
    }

    /**
     * Gets a gridlet ID 
     * @return a gridlet ID
     */
    public int getEntityID()
    {
        return gridletID_;
    }

}
