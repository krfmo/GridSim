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
 * This class is used by the {@link gridsim.net.fnb.FnbInput} entity, 
 * to make sure that all Gridlet packets arrive at the destination.
 * @author       Agustin Caminero
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.fnb.FnbInput
 */
public class source_pktNum
{
    private int source;
    private int numOfPkts;  // how many pkts have arrived yet
    private int glID;       // the id of the gridslet this pkt belongs to
    private boolean ok;     // tells if the current gridlet is fine or not

    
    /**
     * Creates an object of this class.
     * @param sourceID  the source ID
     * @param gridletID the gridlet ID that this packet belongs to
     */
    public source_pktNum(int sourceID, int gridletID)
    {
        source = sourceID;
        numOfPkts = 0;
        glID = gridletID;
        ok = false;
    }

    /** 
     * Gets the source ID of the gridlet
     * @return source ID of the gridlet 
     */
    public int getSourceID()
    {
        return source;
    }

    /** 
     * Gets the number of packets already received
     * @return number of packets already received 
     */
    public int getNumOfPacket()
    {
        return numOfPkts;
    }

    /** 
     * Checks if the gridlet packets have arrived properly or not.
     * @return <tt>true</tt> if all the packets of a gridlet have  
     *         arrived properly, <tt>false</tt> otherwise.
     */
    public boolean getStatus()
    {
        return ok;
    }

    /** Sets the source ID
     * @param sourceID  the source ID of this gridlet
     */
    public void setSourceID(int sourceID)
    {
        source = sourceID;
    }

    /** Sets the status of the incoming gridlet packets.
     * @param status    <tt>true</tt> if all the packets of a gridlet have  
     *                  arrived properly, <tt>false</tt> otherwise.
     */
    public void setStatus(boolean status)
    {
        this.ok = status;
    }

    /** Sets the number of packets
     * @param num   the number of packets of a gridlet
     */
    public void setNumOfPacket(int num)
    {
        numOfPkts = num;
    }


    /** Sets the gridlet id
     * @param gridletID  gridlet id
     */
    public void setGridletID(int gridletID)
    {
        glID = gridletID;
    }


    /** Gets the gridlet id
     * @return gridlet id 
     */
    public int getGridletID()
    {
        return glID;
    }

    /** Adds the number of received packets by 1. */
    public void addNumOfArrivedPkts()
    {
        numOfPkts++;
    }
    
}
