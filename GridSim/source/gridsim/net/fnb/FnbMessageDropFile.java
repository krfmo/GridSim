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

import gridsim.net.fnb.FnbMessage;

/**
 * This class contains a file ID and its name, that is currently being
 * dropped in the network. 
 * This class is primarily used by the {@link gridsim.net.fnb.FnbOutput} class. 
 * @author Agustin Caminero
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.fnb.FnbOutput
 */
public class FnbMessageDropFile implements FnbMessage
{
    private int fileID_;
    private String filename_;

    /**
     * A constructor
     * @param fileID    a file ID
     * @param filename  a filename
     */
    public FnbMessageDropFile(int fileID, String filename)
    {
        fileID_ = fileID;
        filename_ = filename;
    }

    /**
     * Sets a file ID that is being dropped in the network.
     * @param fileID    a file ID
     */
    public void setEntityID(int fileID)
    {
        fileID_ = fileID;
    }

    /**
     * Gets a file ID 
     * @return a file ID
     */
    public int getEntityID()
    {
        return fileID_;
    }

    /**
     * Sets a filename that is being dropped in the network.
     * @param filename  a filename
     */
    public void setFilename(String filename)
    {
        filename_ = filename;
    }

    /**
     * Gets a filename
     * @return a filename
     */
    public String getFilename()
    {
        return filename_;
    }
    
}
