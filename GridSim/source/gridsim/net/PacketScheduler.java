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
 * PacketScheduler.java - Implements a scheduler
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import java.util.*;


/**
 * This class provides a template for schedulers that will be used at routers.
 * Every egress port of a router needs to instantiate a PacketScheduler object
 * which it uses to determine the order in which packets should be sent out.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public interface PacketScheduler
{
    /**
     * Returns the baud rate of the egress port that is using this scheduler.
     * If the baud rate is zero, it means you haven't set it up.
     * @return the baud rate in bits/s
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre $none
     * @post $result >= 0
     */
    public abstract double getBaudRate();

    /**
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @pre rate > 0
     * @post $none
     */
    public abstract boolean setBaudRate(double rate);

    /**
     * This method enques a packet in this scheduler. If the implementing class
     * has buffer management policies too, then it should return <b>true</b> if
     * the packet was successfully enqued. If the packet was dropped, or could
     * not be accomodated due to any other reason, it should return
     * <b>false</b>.
     *
     * @param np    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre np != null
     * @post $none
     */
    public abstract boolean enque(Packet np);

    /**
     * Removes a single packet and returns it. This packet should be sent out
     * by the router from the port using this scheduler.
     *
     * @return the packet to be sent out
     * @pre $none
     * @post $none
     */
    public abstract Packet deque();

    /**
     * Determines whether the scheduler is currently keeping any packets in
     * its queue(s).
     *
     * @return <tt>true</tt> if no packets are enqueued, <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    public abstract boolean isEmpty();

    /**
     * Determines the number of packets that are currently enqueued in this
     * scheduler.
     *
     * @return the number of packets enqueud by this scheduler.
     * @pre $none
     * @post $none
     */
    public abstract int size();

    /**
     * Returns the name of this scheduler, if one was specified during setup.
     * Otherwise "PacketScheduler" is returned. This could be used for debugging
     * purposes.
     *
     * @return the name of this scheduler
     * @pre $none
     * @post $none
     */
    public abstract String getSchedName();

    /**
     * Returns the ID of this scheduler.
     * @return the ID of this scheduler or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public abstract int getSchedID();

    /**
     * Returns the router ID that hosts this scheduler.
     * @return the router ID or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public abstract int getRouterID();

    /**
     * Sets the router ID that hosts this scheduler.
     * @param routerID  the router ID that hosts this scheduler
     * @return <tt>true</tt> if successful or <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public abstract boolean setRouterID(int routerID);

} // end class

