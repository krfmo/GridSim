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
 * This class is used by a router to inform users of a dropped Gridlet.
 * More specifically, the router tells users on which particular gridlet  
 * has been dropped in the network. 
 * @author       Agustin Caminero
 * @since GridSim Toolkit 4.2
 */
public class FnbDroppedPacketInfo
{
    private int glID; 
    private int userID_;

    /**
     * Creates a new object of this class.
     * @param gridletID     gridlet id
     * @param userID        user id
     */
    public FnbDroppedPacketInfo(int gridletID, int userID)
    {
        glID = gridletID;
        userID_ = userID;
    }

    /** Gets the gridlet id.
     * @return the gridlet id.
     * */
    public int getGridletID()
    {
        return glID;
    }

    /** Gets the user id.
     * @return the user id.
     */
    public int getUserID()
    {
        return userID_;
    }

}
