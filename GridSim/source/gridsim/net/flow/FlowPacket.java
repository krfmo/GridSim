/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg
 */

package gridsim.net.flow;

import java.util.Vector;

import gridsim.*;
import gridsim.net.Link;
import gridsim.net.Packet;


/**
 * Structure of a packet used to encapsulate flow passing through the network.
 *
 * @invariant $none
 * @since GridSim Toolkit 4.2
 * @author James Broberg
 */
public class FlowPacket implements Packet
{
    private int destID;  // where the flow wants to go
    private int srcID;   // sender ID
    private long size;   // flow size (for calculating transmission time)
    private long remSize; // remaining flow size
    private Object obj;  // the actual object, the type depends on context

    private double bandwidth_;     // Bottleneck baud rate
    private int bottleneckID;      // Bottleneck link ID
        
    private Vector baudRates_;      // list of entity's baud rate on path from source to dest
    
    private Vector links_;			// list of entity's links on path from source to dest
    
    // Sum of latency (delay) on path
    private double latency;
    
    // Packet start time
    private double startTime;
    
    // Packet last size update time
    private double updateTime;
    
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
     * Constructs a network flow for data that fits into a single network
     * flow packet.
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
    public FlowPacket(Object data, int pktID, long size, int tag, int srcID,
                     int destID)
    {
        this.obj = data ;
        this.size = size ;
        this.remSize = size ;
        this.startTime = -1.0;
        this.updateTime = -1.0;
        this.tag = tag ;
        this.destID = destID;
        this.srcID = srcID ;
        this.last = srcID ;
        this.pktID_ = pktID;
        this.classType = 0 ;
        this.pktNum = 1;
        this.totalPkts = 1;
        this.desc_ = null;
        this.latency = 0.0;
        this.bandwidth_ = Double.MAX_VALUE;
        this.baudRates_ = new Vector();
        this.links_ = new Vector();
        this.bottleneckID = -1;

    }

    /**
     * This is used to construct a flow that is one in a series. This happens
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
    public FlowPacket(Object data, int pktID, long size, int tag, int srcID,
                     int destID, int netServiceType, int pktNum, int totalPkts)
    {
        this.obj = data;
        this.size = size;
        this.remSize = size ;
        this.startTime = -1.0;
        this.updateTime = -1.0;
        this.tag = tag;
        this.destID = destID;
        this.srcID = srcID;
        this.last = srcID;
        this.classType = netServiceType;
        this.pktNum = pktNum;
        this.pktID_ = pktID;
        this.totalPkts = totalPkts;
        this.desc_ = null;
        this.latency = 0.0;
        this.bandwidth_ = -1;
        this.baudRates_ = new Vector();
        this.links_ = new Vector();
        this.bottleneckID = -1;

    }

    /**
     * Returns a description of this flow
     * @return a description of this flow
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
            } else if (tag == GridSimTags.FLOW_SUBMIT) {
                sb.append("GridSimTags.FLOW_SUBMIT");
            } else if (tag == GridSimTags.FLOW_ACK) {
                sb.append("GridSimTags.FLOW_ACK");
            } else {
                sb.append(tag);
            }

            desc_ = sb.toString();
        }

        return desc_;
    }

    /**
     * Returns the data encapsulated in this FlowPacket
     * @return data encapsulated in this packet
     * @pre $none
     * @post $none
     */
    public Object getData() {
        return obj;
    }

    /**
     * Returns the source ID of this packet. The source ID is where the
     * FlowPacket was originally created.
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
     * Modifies the data encapsulated in this FlowPacket.
     * @param data  the packet's data
     * @pre $none
     * @post $none
     */
    public void setData(Object data) {
        this.obj = data;
    }

    /**
     * Gets the size of this flow packet
     * @return the packet size
     * @pre $none
     * @post $none
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Sets the packet size
     * @param size  the flow packet size
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
     * this flow packet.
     *
     * @return the tag of the data contained.
     * @pre $none
     * @post $none
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the destination ID of this flow packet
     *
     * @return destination ID
     * @pre $none
     * @post $none
     */
    public int getDestID() {
        return destID;
    }

    /**
     * Sets the destination id of this flow packet
     * @param id   the destination id
     * @pre id >= 0
     * @post $none
     */
    public void setDestID(int id) {
        this.destID = id;
    }

    /**
     * Sets the last hop that this FlowPacket traversed. This is used to
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
     * Returns the ID of the last hop that this flow packet traversed. This could be
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
     * Sets the network class type of this flow packet, so that it can receive
     * differentiated services.
     * @param netServiceType  a network service type
     * @pre netServiceType >= 0
     * @post $none
     */
    public void setNetServiceType(int netServiceType) {
        this.classType = netServiceType;
    }

    /**
     * Returns the class type of this flow packet. Used by routers etc. to determine
     * the level of service that this flow packet should obtain.
     *
     * @return the class of this packet
     * @pre $none
     * @post $none
     */
    public int getNetServiceType() {
        return classType;
    }

    /**
     * Returns the serial number of this flow packet.
     *
     * @return packet number
     * @pre $none
     * @post $none
     */
    public int getPacketNum() {
        return pktNum;
    }

    /**
     * Returns the total number of flow packets in this stream. A stream of
     * packets is sent whenever the data is too big to be sent as one
     * packet.
     *
     * @return total number of flow packets in this stream.
     * @pre $none
     * @post $none
     */
    public int getTotalPackets() {
        return totalPkts;
    }

    /**
     * Returns the current sum of latency over the path from source to dest.
     *
     * @return latency
     * @pre $none
     * @post $none
     */
	public double getLatency() {
		return latency;
	}
	
    /**
     * Sets the current latency over the path from source to dest.
     *
     * param latency
     * @pre $none
     * @post $none
     */
	public void setLatency(double latency) {
		this.latency = latency;
	}

    /**
     * Adds to the current sum of latency over the path from source to dest.
     *
     * @param latency  the latency of a given link
     * @pre $none
     * @post $none
     */
	public void addLatency(double latency) {
		this.latency += latency;
	}
	
    /**
     * Adds baud rate of current link, and sets bottleneck
     * bandwidth and ID if the link is this flow's bottleneck
     *
     * @param link  a given link
     * @pre $none
     * @post $none
     */
    public synchronized void addBaudRate(Link link)
    {
    	double baudRate = link.getBaudRate();

        if (baudRates_ == null) {
            return;
        }

        baudRates_.add( new Double(baudRate) );
        if (bandwidth_ < 0 || baudRate < this.bandwidth_) {
            this.setBandwidth(baudRate);
            this.setBottleneckID(link.get_id());
        }
    }

    /**
     * Returns the current bottleneck bandwidth of this flow.
     *
     * @return bandwidth_
     * @pre $none
     * @post $none
     */
	public double getBandwidth() {
		return bandwidth_;
	}

    /**
     * Sets the current bottleneck bandwidth of this flow.
     *
     * param bandwidth_ the current bottleneck bandwidth
     * @pre $none
     * @post $none
     */
	public synchronized void setBandwidth(double bandwidth_) {
		this.bandwidth_ = bandwidth_;
	}
	
    /**
     * Adds current link, and calls addBaudRate() and addLatency()
     *
     * @param link  a given link
     * @pre $none
     * @post $none
     */
	public synchronized void addLink(Link link)
    {
        if (links_ == null) {
            return;
        }

        links_.add( link );
        this.addBaudRate(link);
        this.addLatency(link.getDelay());
    }

    /**
     * Returns the current start time of this flow.
     *
     * @return startTime
     * @pre $none
     * @post $none
     */
	public double getStartTime() {
		return startTime;
	}

    /**
     * Sets the current start time of this flow.
     *
     * @param startTime the time a flow begins holding at the destination
     * @pre $none
     * @post $none
     */
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	 /**
     * Returns the last time a flow was updated (i.e. bottleneck 
     * bandwidth changed and forecast was recomputed)
     *
     * @return updateTime
     * @pre $none
     * @post $none
     */
	public double getUpdateTime() {
		return updateTime;
	}

	 /**
     * Sets the last time a flow was updated (i.e. bottleneck 
     * bandwidth changed and forecast was recomputed)
     *
     * @param updateTime the time a flow's forecast was last updated
     * @pre $none
     * @post $none
     */
	public void setUpdateTime(double updateTime) {
		this.updateTime = updateTime;
	}

	 /**
     * Returns the remaining size of a flow
     *
     * @return remSize
     * @pre $none
     * @post $none
     */
	public long getRemSize() {
		return remSize;
	}

	 /**
     * Sets the remaining size of a flow
     *
     * param remSize the remaining size of a flow
     * @pre $none
     * @post $none
     */
	public void setRemSize(long remSize) {
		this.remSize = remSize;
	}

	 /**
     * Returns a vector of links that make up this flow's path
     *
     * @return links_
     * @pre $none
     * @post $none
     */
	public Vector getLinks() {
		return links_;
	}

	 /**
     * Returns the FlowLink ID of the bottleneck of this flow
     *
     * @return bottleneckID
     * @pre $none
     * @post $none
     */
	public int getBottleneckID() {
		return bottleneckID;
	}

	 /**
     * Sets the FlowLink ID of the bottleneck of this flow
     *
     * param bottleneckID the ID of the bottleneck FlowLink
     * @pre $none
     * @post $none
     */
	public void setBottleneckID(int bottleneckID) {
		this.bottleneckID = bottleneckID;
	}

	 /**
     * Sets the source ID for a FlowPacket
     *
     * param srcID the id of the source of this flow
     * @pre $none
     * @post $none
     */
	public void setSrcID(int srcID) {
		this.srcID = srcID;
	}

    /**
     * Sets the tag of this packet
     * @param tag   the packet's tag
     * @pre $none
     * @post $none
     */
	public void setTag(int tag) {
		this.tag = tag;
	}

} // end class

