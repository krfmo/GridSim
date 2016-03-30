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

import gridsim.*;
import gridsim.net.*;

/**
 * This class contains the structure of a packet
 * for the purpose of finite network buffers.
 * <p>
 * In order to minimise duplications, hence, to reduce memory consumption,
 * common attributes to all packets have been stored in the
 * {@link gridsim.net.fnb.FnbEndToEndPath} class.
 * The common attributes are destination ID, source ID, class type and total
 * number of packets.
 *
 * @see gridsim.net.fnb.FnbEndToEndPath
 * @since GridSim Toolkit 4.2
 * @author Agustin Caminero, Universidad de Castilla-La Mancha (UCLM) (Spain)
 */
public class FnbNetPacket implements Packet
{
    private long size;   // packet size (for calculating transmission time)
    private Object obj;  // the actual object, the type depends on context

    // original tag with which the encapsulated object was submitted
    private int tag;

    // the last entity encountered by the object, used to determine direction
    private int last;

    private String desc_;   // description of this packet
    private int pktNum;     // packet num in one group
    private int pktID_;     // a unique packet ID issued by an entity
    private FnbEndToEndPath conn;

    /**
     * Constructs a network packet for data that fits into a single network
     * packet.
     *
     * @param data   The data to be encapsulated.
     * @param pktID  The ID of this packet
     * @param size   The size of the data (in bytes)
     * @param tag    The original tag which was used with the data, its
     *               reapplied when the data is extracted from the NetPacket.
     * @param srcID  The id of the entity where the packet was created.
     * @pre $none
     * @post $none
     */
    public FnbNetPacket(Object data, int pktID, long size, int tag, int srcID)
    {
        this.obj = data ;
        this.size = size ;
        this.tag = tag ;
        this.last = srcID ;
        this.pktID_ = pktID;
        this.pktNum = 1;
        this.desc_ = null;
        this.conn = null;
    }

    /**
     * This is used to construct a packet that is one in a series. This happens
     * when a large piece of data is required to be broken down into smaller
     * chunks so that they can traverse of links that only support a certain
     * MTU. It also allows setting of a classtype so that network schedulers
     * maybe provide differntial service to it.
     *
     * @param data   The data to be encapsulated.
     * @param pktID  The ID of this packet
     * @param size   The size of the data (in bytes)
     * @param tag    The original tag which was used with the data, its
     *               reapplied when the data is extracted from the NetPacket.
     * @param srcID  The id of the entity where the packet was created.
     * @param pktNum The packet number of this packet in its series. If there
     *               are 10 packets, they should be numbered from 1 to 10.
     * @pre $none
     * @post $none
     */
    public FnbNetPacket(Object data, int pktID, long size, int tag, int srcID, int pktNum)
    {
        this.obj = data;
        this.size = size;
        this.tag = tag;

        this.last = srcID;

        this.pktNum = pktNum;
        this.pktID_ = pktID;

        this.desc_ = null;
    }

    /**
     * Returns a description of this packet
     * @return a description of this packet
     * @pre $none
     * @post $none
     */
    public String toString()
    {
        if (desc_ == null)
        {
            StringBuffer sb = new StringBuffer("Packet #");
            sb.append(pktNum);
            sb.append(", out of, ");
            sb.append(conn.getTotalPkts());
            sb.append(", with id, ");
            sb.append(pktID_);
            sb.append(", from, ");
            sb.append( GridSim.getEntityName(conn.getSrc()) );
            sb.append(", to, ");
            sb.append( GridSim.getEntityName(conn.getDest()) );
            sb.append(", tag, ");

            if (tag == GridSimTags.PKT_FORWARD) {
                sb.append("GridSimTags.PKT_FORWARD");
            }
            else if (tag == GridSimTags.JUNK_PKT) {
                sb.append("GridSimTags.JUNK_PKT");
            }
            else {
                sb.append(tag);
            }

            desc_ = sb.toString();
        }

        return desc_;
    }

    /**
     * Returns the data encapsulated in this NetPacket
     * @return data encapsulated in this packet
     * @pre $none
     * @post $none
     */
    public Object getData() {
        return obj;
    }

    /**
     * Returns the source ID of this packet. The source ID is where the
     * packet was originally created.
     *
     * @return the source id.
     * @pre $none
     * @post $none
     */
    public int getSrcID() {
        return conn.getSrc();
    }

    /**
     * Returns the ID of this packet
     * @return packet ID
     * @pre $none
     * @post $none
     */
    public int getID() {
        return pktID_;
    }

    /**
     * Modifies the data encapsulated in this packet.
     * @param data  the packet's data
     * @pre $none
     * @post $none
     */
    public void setData(Object data) {
        this.obj = data;
    }

    /**
     * Gets the size of this packet
     * @return the packet size
     * @pre $none
     * @post $none
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the packet size
     * @param size  the packet size
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre size >= 0
     * @post $none
     */
    public boolean setSize(long size)
    {
        if (size < 0) {
            return false;
        }

        this.size = size;
        return true;
    }

    /**
     * Returns the tag associated originally with data that was encapsulated in
     * this packet.
     *
     * @return the tag of the data contained.
     * @pre $none
     * @post $none
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the destination ID of this packet
     *
     * @return destination ID
     * @pre $none
     * @post $none
     */
    public int getDestID() {
        return conn.getDest();
    }

    /**
     * Sets the destination id of this packet
     * @param id   the destination id
     * @pre id >= 0
     * @post $none
     */
    public void setDestID(int id) {
        //this.destID = id;
    }

    /**
     * Sets the last hop that this NetPacket traversed. This is used to
     * determine the next hop at routers. Only routers and hosts/GridResources
     * set this, links do not modify it.
     * @param last  the entity ID from the last hop
     * @pre last >= 0
     * @post $none
     */
    public void setLast(int last) {
        this.last = last;
    }

    /**
     * Returns the ID of the last hop that this packet traversed. This could be
     * the ID of a router, host or GridResource.
     *
     * @return ID of the last hop
     * @pre $none
     * @post $none
     */
    public int getLast() {
        return last;
    }

    /**
     * Sets the network class type of this packet, so that it can receive
     * differentiated services.
     * @param netServiceType  a network service type
     * @pre netServiceType >= 0
     * @post $none
     */
    public void setNetServiceType(int netServiceType) {
        conn.setClasstype(netServiceType);
    }

    /**
     * Returns the class type of this packet. Used by routers etc. to determine
     * the level of service that this packet should obtain.
     *
     * @return the class of this packet
     * @pre $none
     * @post $none
     */
    public int getNetServiceType() {
       return conn.getClasstype();
    }

    /**
     * Returns the serial number of this packet.
     *
     * @return packet number
     * @pre $none
     * @post $none
     */
    public int getPacketNum() {
        return pktNum;
    }

    /**
     * Returns the total number of packets in this stream. A stream of
     * packets is sent whenever the data is too big to be sent as one
     * packet.
     *
     * @return total number of packets in this stream.
     * @pre $none
     * @post $none
     */
    public int getTotalPackets() {
        return conn.getTotalPkts();
    }

    /** Establishes the end to end path to another entity
     * @param connection    an end-to-end connection path
     */
    public void setPath(FnbEndToEndPath connection)
    {
        conn = connection;
    }

    /**Returns the gridlet/file to which this packet belongs
     * @return the gridlet/file to which this packet belongs
     */
    public int getObjectID()
    {
        return conn.getObjectID();
    }

    /** Checks whether this packet contains a file or not
     * @return <tt>true</tt> if this is a file, <tt>false</tt> otherwise
     */
    public boolean isFile()
    {
        return conn.isFile();
    }

} // end class


