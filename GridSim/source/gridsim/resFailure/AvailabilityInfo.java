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
 * This class is used by GridSim users to check whether a particular resource
 * is working or is totally failed.
 *
 * @author       Agustin Caminero
 * @since        GridSim Toolkit 4.1
 */

public class AvailabilityInfo
{
    private int resID;
    private int srcID;
    private boolean availability;

    /**
     * Creates a new object of this class
     * @param res   a resource id
     * @param src   the sender id
     */
    public AvailabilityInfo(int res, int src)
    {
        resID = res;
        srcID = src;
        availability = true;    // by default, true
    }

    /**
     * Gets the source or sender id
     * @return sender id
     */
    public int getSrcID() {
        return srcID;
    }

    /**
     * Gets the resource id
     * @return resource id
     */
    public int getResID() {
        return resID;
    }

    /**
     * Checks the availability of this resource
     * @return <tt>true</tt> if available, <tt>false</tt> otherwise
     */
    public boolean getAvailability() {
        return availability;
    }

    /**
     * Sets the source or sender id
     * @param src   the sender id
     */
    public void setSrcID(int src) {
        srcID = src;
    }

    /**
     * Sets the resource id
     * @param res   a resource id
     */
    public void setResID(int res) {
        resID = res;
    }

    /**
     * Sets the resource availability
     * @param av    <tt>true</tt> if available, <tt>false</tt> otherwise
     */
    public void setAvailability(boolean av) {
        availability = av;
    }

} 
