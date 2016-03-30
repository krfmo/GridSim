/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.net.fnb;

/**
 * This class contains relevant information of a dropped 
 * {@link gridsim.Gridlet} or {@link gridsim.datagrid.File} object.
 * Since a gridlet or a file is broken into many packets according to the
 * Maximum Transmission Unit (MTU), this class records the first packet ID,
 * the last packet ID, and the Gridlet or File ID.
 * This class is mainly used by the {@link gridsim.net.fnb.FnbInput} and 
 * {@link gridsim.net.fnb.FnbOutput} classes.
 * <p>
 * For example, a gridlet with id = 3 is made of 20 packets, e.g. from
 * packet id 2 to 21. Then, this class stores the following information:
 * the first packet number is 2, the last packet number is 21, 
 * the ID is 3, and {@link #isFile()} returns <tt>false</tt>.
 * 
 * @deprecated As of GridSim 5.0, this class has been deprecated.
 * Use {@link GridletPackets} instead.
 *
 * @author       Agustin Caminero and Anthony Sulistio
 * @since GridSim Toolkit 4.2
 * @see gridsim.net.Link#DEFAULT_MTU
 * @see gridsim.Gridlet
 * @see gridsim.datagrid.File
 *
 */
public class firstLastPacketsGridlet
{
    private int firstPacket;    // the id of the first packet in a gridlet
    private int lastPacket;     // the id of the last packet in a  gridlet
    private int id_;            // the gridlet or file id
    private boolean isFile;     // whether this class contains a gridlet or a file


    /**
     * Creates a new object of this class. 
     * @param firstPacketID     the first packet id
     * @param lastPacketID      the last packet id
     * @deprecated As of GridSim 5.0, this class has been deprecated.
     * Use {@link GridletPackets} instead.
     */
    public void firstLastPacketsGridlet(int firstPacketID, int lastPacketID)
    {
        this.firstPacket = firstPacketID;
        this.lastPacket = lastPacketID;
        this.id_ = -1;
        this.isFile = false;
    }

    /** 
     * Determines whether this class stores a file ID or a gridlet ID
     * @return <tt>true</tt> this class stores a file ID, <tt>false</tt> otherwise
     * */
    public boolean isFile()
    {
        return isFile;
    }

    /** Gets the id of the first packet.
     * @return the id of the first packet
     * */
    public int getFirstPacketID()
    {
        return firstPacket;
    }

    /** Gets the id of the last packet.
     * @return the id of the last packet
     * */
    public int getLastPacketID()
    {
        return lastPacket;
    }        

    /** Sets the id of the last packet.
     * @param lastID  the id of the last packet.
     * */
    public void setLastPacketID(int lastID)
    {
        lastPacket = lastID;
    }

    /** Sets the id of the first packet.
     * @param firstID   the id of the first packet.
     * */
    public void setFirstPacketID(int firstID)
    {
        firstPacket = firstID;
    }

    /** Sets the gridlet id.
     * @param gridletID     the gridlet id
     * */
    public void setGridletID(int gridletID)
    {
        id_ = gridletID;
        isFile = false;
    }
    
    /** Sets the file id.
     * @param fileID    the file id
     */
    public void setFileID(int fileID)
    {
        id_ = fileID;
        this.isFile = true;
    }
    
    /**
     * Gets a file or gridlet ID.<br>
     * NOTE: If {@link #isFile()} denotes <tt>true</tt>, then it is a file ID, 
     * <tt>false</tt> otherwise
     * @return a file or gridlet ID.
     */
    public int getID()
    {
        return id_;
    }

}
