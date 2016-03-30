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
 * This class keeps information which are common to all network packets.
 * When a packet is dropped in a router, the router must inform the user/owner
 * about this case.
 * Since many packets from the same gridlet may be dropped, the router only
 * sends one event for the whole process, not for each packet.
 * Thus, we put all of the common information (e.g. source and destination IDs)
 * into this class.
 *
 * @since GridSim Toolkit 4.2
 * @author Agustin Caminero, Universidad de Castilla-La Mancha (UCLM) (Spain)
 */
public class FnbEndToEndPath
{
    private int destID;
    private int srcID;
    private int classtype;
    private int totalPkts; // total num of packet that belongs to a group
    private boolean isFile; // true if this is a file, false otherwise

    // the id of the gridlet/file this packet belongs to, or
    // GridSimTags.FNB_PKT_CONTAINS_FILE if the pkt contains a file
    private int glID;


    /**Creates a new object of this class. This is used by
     * the {@link gridsim.net.fnb.FnbOutput} class.
     * @param destID    destination id
     * @param srcID     source id
     * @param classType network service level
     * @param totalPkts total number of packets this connection is made of
     * @param glID      the gridlet/file id
     * */
    public FnbEndToEndPath(int destID, int srcID, int classType, int totalPkts,
                           int glID)
    {
        this(destID, srcID, classType, totalPkts, glID, false);
    }
    
    /**Creates a new object of this class. This is used by
     * the {@link gridsim.net.fnb.FnbOutput} class.
     * @param destID    destination id
     * @param srcID     source id
     * @param classType network service level
     * @param totalPkts total number of packets this connection is made of
     * @param glID      the gridlet/file id
     * @param isFile    <tt>true</tt> if it contains a file, <tt>false</tt> otherwise
     * */
    public FnbEndToEndPath(int destID, int srcID, int classType, int totalPkts,
                           int glID, boolean isFile)
    {
        this.destID = destID;
        this.srcID = srcID;
        this.classtype = classType;
        this.totalPkts = totalPkts;
        this.glID = glID;
        this.isFile = isFile;
    }


    /**Creates a new object of this class. This is used by
     * the {@link gridsim.net.fnb.FnbOutput} class.
     * @param destID destination id
     * @param srcID source id
     * @param classType network service level
     * @param totalPkts total number of packets this connection is made of
     * */
    public FnbEndToEndPath(int destID, int srcID, int classType, int totalPkts)
    {
        this(destID, srcID, classType, totalPkts, -1, false);
    }

    /** Sets the destination id for a connection.
     * @param id the destination id
     * */
    public void setDest(int id)
    {
        destID = id;
    }


    /** Sets the source id for a connection.
     * @param sourceID the source id
     * */
    public void setSrc(int sourceID)
    {
        srcID = sourceID;
    }


    /** Sets the network service level (or classtype) for a connection.
     * @param classType the network service level id
     * */
    public void setClasstype(int classType)
    {
        classtype = classType;
    }


    /** Sets the total packets for a connection.
     * @param total total packets
     * */
    public void setTotalPkts(int total)
    {
        totalPkts = total;
    }


    /** Checks whether this packet contains a file or not
     * @return <tt>true</tt> if this is a file, <tt>false</tt> otherwise
     */
    public boolean isFile()
    {
        return isFile;
    }

    /** Gets the source id of a connection.
     * @return the source id of the connection
     * */
    public int getSrc()
    {
        return srcID;
    }


    /** Gets the destination id of a connection.
     * @return the destination id of the connection
     * */
    public int getDest()
    {
        return destID;
    }


    /** Gets the classtype of a connection.
     * @return the classtype of the connection
     * */
    public int getClasstype()
    {
        return classtype;
    }


    /** Gets the total number of packets of a connection.
     * @return the total number of packets of the connection
     * */
    public int getTotalPkts()
    {
        return totalPkts;
    }


    /** Sets the gridlet/file id of a connection.
     * @param id the gridlet id of the connection
     * */
    public void setObjectID(int id)
    {
        glID = id;
    }


    /** Gets the gridlet/file id of a connection.
     * @return the gridlet id of the connection
     * */
    public int getObjectID()
    {
        return glID;
    }
}
