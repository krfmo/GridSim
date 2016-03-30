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
 * InfoPacket.java - Implementation of a Information Packet.
 *
 */

package gridsim.net;

import java.text.DecimalFormat;
import java.util.* ;
import gridsim.* ;


/**
 * InfoPacket class can be used to gather information from the network layer.
 * An InfoPacket traverses the network topology similar to a
 * {@link gridsim.net.NetPacket},
 * but it collects information like bandwidths, and Round Trip Time etc. It is
 * the equivalent of ICMP in physical networks.
 * <p>
 * You can set all the parameters to an InfoPacket that can be applied to a
 * NetPacket. So if you want to find out the kind of information that a
 * particular type of NetPacket is experiencing, set the size and network
 * class of an InfoPacket to the same as the NetPacket, and send it to the same
 * destination from the same source.
 *
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 * @invariant $none
 */

public class InfoPacket implements Packet
{
    private String name_;   // packet name
    private long size_;     // size of this packet
    private int packetID_;  // id of this packet
    private int srcID_;     // original sender id
    private int destID_;    // destination id
    private int last_;      // last hop
    private int tag_;       // whether going or returning
    private int numHop_;    // number of hops
    private long pingSize_; // original ping size
    private int netServiceType_;    // level of service type
    private double bandwidth_;      // bottleneck
    private Vector entities_;       // list of entity IDs
    private Vector entryTimes_;     // list of entry times
    private Vector exitTimes_;      // list of exit times
    private Vector baudRates_;      // list of entity's baud rate
    private DecimalFormat num_;     // formatting the decimal points


    /**
     * Constructs a new Information packet.
     *
     * @param name   Name of this packet
     * @param packetID  The ID of this packet
     * @param size   size of the packet
     * @param srcID  The ID of the entity that sends out this packet
     * @param destID The ID of the entity to which this packet is destined
     * @param netServiceType the class of traffic this packet belongs to
     *
     * @pre name != null
     * @post $none
     */
    public InfoPacket(String name, int packetID, long size, int srcID,
                      int destID, int netServiceType)
    {
        this.name_ = name;
        this.packetID_ = packetID;
        this.srcID_ = srcID;
        this.destID_ = destID;
        this.size_ = size;
        this.netServiceType_ = netServiceType;

        init();
    }

    /**
     * Initialises common attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        this.last_ = srcID_;
        this.tag_ = GridSimTags.INFOPKT_SUBMIT;
        this.bandwidth_ = -1;
        this.numHop_ = 0;
        this.pingSize_ = size_;

        if (name_ != null)
        {
            this.entities_ = new Vector();
            this.entryTimes_ = new Vector();
            this.exitTimes_ = new Vector();
            this.baudRates_ = new Vector();
            num_ = new DecimalFormat("#0.000#");   // 4 decimal spaces
        }
    }

    /**
     * Returns the ID of this packet
     * @return packet ID
     * @pre $none
     * @post $none
     */
    public int getID() {
        return packetID_;
    }

    /**
     * Sets original size of ping request
     * @param size  ping data size (in bytes)
     * @pre size >= 0
     * @post $none
     */
    public void setOriginalPingSize(long size) {
        this.pingSize_ = size;
    }

    /**
     * Gets original size of ping request
     * @return original size
     * @pre $none
     * @post $none
     */
    public long getOriginalPingSize() {
        return pingSize_;
    }

    /**
     * Returns a human-readable information of this packet.
     *
     * @return description of this packet
     * @pre $none
     * @post $none
     */
    public String toString()
    {
        if (name_ == null) {
            return "Empty InfoPacket that contains no ping information.";
        }

        int SIZE = 1000;   // number of chars
        StringBuffer sb = new StringBuffer(SIZE);
        sb.append("Ping information for " + name_ + "\n");
        sb.append("Entity Name\tEntry Time\tExit Time\t Bandwidth\n");
        sb.append("----------------------------------------------------------\n");

        String tab = "    ";  // 4 spaces
        for (int i = 0 ; i < entities_.size(); i++)
        {
            int resID = ( (Integer) entities_.get(i) ).intValue();
            sb.append(GridSim.getEntityName(resID) + "\t\t");

            String entry = getData(entryTimes_, i);
            String exit = getData(exitTimes_, i);
            String bw = getData(baudRates_, i);

            sb.append(entry + tab + tab + exit + tab + tab + bw + "\n");
        }

        sb.append("\nRound Trip Time : " +
                  num_.format(this.getTotalResponseTime()) );
        sb.append(" seconds");
        sb.append("\nNumber of Hops  : " + this.getNumHop() );
        sb.append("\nBottleneck Bandwidth : " + bandwidth_ + " bits/s");
        return sb.toString();
    }

    /**
     * Gets relevant data from a list
     * @param v  a list
     * @param index   the location in a list
     * @pre v != null
     * @post index > 0
     */
    private String getData(Vector v, int index)
    {
        String result;
        try
        {
            Double obj = (Double) v.get(index);
            double id = obj.doubleValue();
            result = num_.format(id);
        }
        catch (Exception e) {
            result = "    N/A" ;
        }

        return result;
    }

    /**
     * Gets the size of this packet.
     *
     * @return size of the packet.
     * @pre $none
     * @post $none
     */
    public long getSize() {
        return size_;
    }

    /**
     * Sets the size of this packet
     * @param size  size of the packet
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre size >= 0
     * @post $none
     */
    public boolean setSize(long size)
    {
        if (size < 0) {
            return false;
        }

        this.size_ = size;
        return true;
    }

    /**
     * Gets the id of the entity to which the packet is destined.
     *
     * @return the desination ID
     * @pre $none
     * @post $none
     */
    public int getDestID() {
        return destID_;
    }

    /**
     * Gets the id of the entity that sent out this packet
     *
     * @return the source ID
     * @pre $none
     * @post $none
     */
    public int getSrcID() {
        return srcID_;
    }

    /**
     * Returns the number of hops that this packet has traversed. Since the
     * packet takes a round trip, the same router may have been traversed
     * twice.
     *
     * @return the number of hops this packet has traversed
     * @pre $none
     * @post $none
     */
    public int getNumHop()
    {
        int PAIR = 2;
        return ((numHop_ - PAIR) + 1) / PAIR;
    }

    /**
     * Gets the total time that this packet has spent in the network. This is
     * basically the RTT. Dividing this by half should be the approximate
     * latency.
     * <p>
     * RTT is taken as the final entry time - first exit time.
     * @return total round time
     * @pre $none
     * @post $none
     */
    public double getTotalResponseTime()
    {
        if (exitTimes_ == null || entryTimes_ == null) {
            return 0;
        }

        double time = 0;
        try
        {
            double startTime = ((Double)exitTimes_.firstElement()).doubleValue();
            double receiveTime = ((Double)entryTimes_.lastElement()).doubleValue();
            time = receiveTime - startTime;
        }
        catch(Exception e) {
            time = 0;
        }

        return time;
    }

    /**
     * Returns the bottleneck bandwidth between the source and the destination
     *
     * @return the bottleneck bandwidth
     * @pre $none
     * @post $none
     */
    public double getBaudRate() {
        return bandwidth_;
    }

    /**
     * This method should be called by network entities that count as hops, for
     * e.g. Routers or GridResources. It should not be called by links etc.
     *
     * @param id    the id of the hop that this InfoPacket is traversing
     * @pre id > 0
     * @post $none
     */
    public void addHop(int id)
    {
        if (entities_ == null) {
            return;
        }

        numHop_++;
        entities_.add( new Integer(id) );
    }

    /**
     * This method should be called by routers and other entities
     * when this InfoPacket reaches them along with the current simulation time.
     *
     * @param time  current simulation time, use
     *              {@link gridsim.GridSim#clock()} to obtain this
     * @pre time >= 0
     * @post $none
     */
    public void addEntryTime(double time)
    {
        if (entryTimes_ == null) {
            return;
        }

        if (time < 0) {
            time = 0.0;
        }

        entryTimes_.add( new Double(time) );
    }

    /**
     * This method should be called by routers and other entities
     * when this InfoPacket is leaving them. It should also supply the current
     * simulation time.
     *
     * @param time  current simulation time, use
     *              {@link gridsim.GridSim#clock()} to obtain this
     * @pre time >= 0
     * @post $none
     */
    public void addExitTime(double time)
    {
        if (exitTimes_ == null) {
            return;
        }

        if (time < 0) {
            time = 0.0;
        }

        exitTimes_.add( new Double(time) );
    }

    /**
     * Every entity that the InfoPacket traverses should add the baud rate of
     * the link on which this packet will be sent out next.
     * @param baudRate  the entity's baud rate in bits/s
     * @pre baudRate > 0
     * @post $none
     */
    public void addBaudRate(double baudRate)
    {
        if (baudRates_ == null) {
            return;
        }

        baudRates_.add( new Double(baudRate) );
        if (bandwidth_ < 0 || baudRate < this.bandwidth_) {
            this.bandwidth_ = baudRate;
        }
    }

    /**
     * Returns the list of all the bandwidths that this packet has traversed
     *
     * @return a Double Array of links bandwidths
     * @pre $none
     * @post $none
     */
    public Object[] getDetailBaudRate()
    {
        if (baudRates_ == null) {
            return null;
        }

        return baudRates_.toArray();
    }

    /**
     * Returns the list of all the hops that this packet has traversed.
     *
     * @return an Integer Array of hop ids
     * @pre $none
     * @post $none
     */
    public Object[] getDetailHops()
    {
        if (entities_ == null) {
            return null;
        }

        return entities_.toArray();
    }

    /**
     * Returns the list of all entry time that this packet has traversed.
     * @return an Integer Array of entry time
     * @pre $none
     * @post $none
     */
    public Object[] getDetailEntryTimes()
    {
        if (entryTimes_ == null) {
            return null;
        }

        return entryTimes_.toArray();
    }

    /**
     * Returns the list of all exit time that this packet has traversed.
     * @return an Integer Array of exit time
     * @pre $none
     * @post $none
     */
    public Object[] getDetailExitTimes()
    {
        if (exitTimes_ == null) {
            return null;
        }

        return exitTimes_.toArray();
    }

    /**
     * Gets an entity ID from the last hop that this packet has traversed.
     * @return an entity ID
     * @pre $none
     * @post $none
     */
    public int getLast() {
        return last_;
    }

    /**
     * Sets an entity ID from the last hop that this packet has traversed.
     * @param last  an entity ID from the last hop
     * @pre last > 0
     * @post $none
     */
    public void setLast(int last) {
        this.last_ = last;
    }

    /**
     * Gets the network service type of this packet
     * @return the network service type
     * @pre $none
     * @post $none
     */
    public int getNetServiceType() {
        return netServiceType_ ;
    }

    /**
     * Sets the network service type of this packet
     * @param netServiceType    the packet's network service type
     * @pre netServiceType >= 0
     * @post $none
     */
    public void setNetServiceType(int netServiceType) {
        this.netServiceType_ = netServiceType ;
    }

    /**
     * Gets this packet tag
     * @return this packet tag
     * @pre $none
     * @post $none
     */
    public int getTag() {
        return tag_ ;
    }

    /**
     * Sets the tag of this packet
     * @param tag   the packet's tag
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre tag > 0
     * @post $none
     */
    public boolean setTag(int tag)
    {
        boolean flag = false;
        switch(tag)
        {
            case GridSimTags.INFOPKT_SUBMIT:
            case GridSimTags.INFOPKT_RETURN:
                this.tag_ = tag;
                flag = true;
                break;

            default:
                flag = false;
                break;
        }

        return flag;
    }

    /**
     * Sets the destination ID for this packet
     * @param id    this packet's destination ID
     * @pre id > 0
     * @post $none
     */
    public void setDestID(int id) {
        this.destID_ = id;
    }

} // end class

