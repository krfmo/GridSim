/*
 * ** Network and Service Differentiation Extensions to GridSim 3.0 **
 *
 * Gokul Poduval & Chen-Khong Tham
 * Computer Communication Networks (CCN) Lab
 * Dept of Electrical & Computer Engineering
 * National University of Singapore
 * August 2004
 *
 * License: GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2004, The University of Melbourne, Australia and National
 * University of Singapore
 * NetPacket.java - Implementation of a Network Packet.
 *
 */

package gridsim.net;

import gridsim.*;
import eduni.simjava.*;


/**
 * Structure of a packet used to encapsulate data passing through the network.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public class NetPacket implements Packet
{
    private int destID;  // where the packet wants to go
    private int srcID;   // sender ID
    private long size;   // packet size (for calculating transmission time)
    private Object obj;  // the actual object, the type depends on context

    // original tag with which the encapsulated object was submitted
    private int tag;

    // the last entity encountered by the object, used to determine direction
    private int last;

    private String desc_;   // description of this packet
    private int classType;  // level of service for this packet
    private int pktNum;     // packet num in one group
    private int totalPkts;  // total num of packet that belongs to a group
    private int pktID_;     // a unique packet ID issued by an entity

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
     * @param destID The destination to which the packet has to be sent.
     * @pre $none
     * @post $none
     */
    public NetPacket(Object data, int pktID, long size, int tag, int srcID,
                     int destID)
    {
        this.obj = data ;
        this.size = size ;
        this.tag = tag ;
        this.destID = destID;
        this.srcID = srcID ;
        this.last = srcID ;
        this.pktID_ = pktID;
        this.classType = 0 ;
        this.pktNum = 1;
        this.totalPkts = 1;
        this.desc_ = null;
    }

    /**
     * This is used to construct a packet that is one in a series. This happens
     * when a large piece of data is required to be brokwn down into smaller
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
     * @param destID The destination to which the packet has to be sent.
     * @param netServiceType the network class type of this packet
     * @param pktNum The packet number of this packet in its series. If there
     *               are 10 packets, they should be numbered from 1 to 10.
     * @param totalPkts The total number of packets that the original data was
     *               split into. This is used by the receiver to confirm that
     *               all packets have been received.
     * @pre $none
     * @post $none
     */
    public NetPacket(Object data, int pktID, long size, int tag, int srcID,
                     int destID, int netServiceType, int pktNum, int totalPkts)
    {
        this.obj = data;
        this.size = size;
        this.tag = tag;
        this.destID = destID;
        this.srcID = srcID;
        this.last = srcID;
        this.classType = netServiceType;
        this.pktNum = pktNum;
        this.pktID_ = pktID;
        this.totalPkts = totalPkts;
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
            sb.append(totalPkts);
            sb.append(", with id, ");
            sb.append(pktID_);
            sb.append(", from, ");
            sb.append( GridSim.getEntityName(srcID) );
            sb.append(", to, ");
            sb.append( GridSim.getEntityName(destID) );
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
     * NetPacket was originally created.
     *
     * @return the source id.
     * @pre $none
     * @post $none
     */
    public int getSrcID() {
        return srcID;
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
     * Modifies the data encapsulated in this NetPacket.
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
        return destID;
    }

    /**
     * Sets the destination id of this packet
     * @param id   the destination id
     * @pre id >= 0
     * @post $none
     */
    public void setDestID(int id) {
        this.destID = id;
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
        this.classType = netServiceType;
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
        return classType;
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
        return totalPkts;
    }

} // end class

