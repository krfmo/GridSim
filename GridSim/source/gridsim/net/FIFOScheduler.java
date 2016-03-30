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
 * FIFOScheduler.java - Implements a First in First Out scheduler
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import java.util.*;


/**
 * FIFOScheduler implements a First in First Out Scheduler. This means that
 * all the packets are enqued at the tail of a queue, and packets depart from
 * the head of the queue. Packets are not reordered, and no differentiated
 * service will be provided.
 *
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 * @since  GridSim Toolkit 3.1
 * @invariant $none
 */
public class FIFOScheduler implements PacketScheduler
{
    private String name_;       // this scheduler name
    private double baudRate_;   // baud rate of this scheduler
    private Vector pktList;     // Sorted List of all Packets


    /**
     * Creates a new FIFO packet scheduler with the specified name and baud rate
     * (in bits/s). The name can be useful for debugging purposes, but serves
     * no functional purpose.
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @throws ParameterException This happens when the name is null or
     *                   the baud rate <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @post $none
     */
    public FIFOScheduler(String name, double baudRate) throws ParameterException
    {
        init(name, baudRate);
    }

    /**
     * Creates a new FIFO packet scheduler with the specified baud rate
     * (in bits/s). The name is set to a generic name: <b>"FIFOScheduler"</b>.
     *
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @throws ParameterException This happens when the baud rate <= 0
     * @pre baudRate > 0
     * @post $none
     */
    public FIFOScheduler(double baudRate) throws ParameterException {
        init("FIFOScheduler", baudRate);
    }

    /**
     * Creates a new FIFO packet scheduler with the specified name.
     * The baud rate is left at 0, and should be set with
     * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
     * before the simulation starts.
     *
     * @param name Name of this scheduler
     * @throws ParameterException  This happens when the name is null
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre name != null
     * @post $none
     */
    public FIFOScheduler(String name) throws ParameterException
    {
        if (name == null) {
            throw new ParameterException("FIFOScheduler(): Name is null.");
        }

        name_ = name;
        baudRate_ = 0;
        pktList = new Vector();
    }

    /**
     * Creates a new packet scheduler with the name <b>"FIFOScheduler"</b>.
     * The baud rate is left at 0, and should be set with
     * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
     * before the simulation starts.
     * @throws ParameterException This happens when the name is null
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre $none
     * @post $none
     */
    public FIFOScheduler() throws ParameterException
    {
        name_ = "FIFOScheduler";
        baudRate_ = 0;
        pktList = new Vector();
    }

    /**
     * In this scheduler, the packet is put into the tail of the queue.
     * There is no buffer management, so packets are never dropped, and the
     * queue can grow as long as system memory is available.
     *
     * @param np    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre np != null
     * @post $none
     */
    public synchronized boolean enque(Packet np)
    {
        pktList.add(np);
        return true;
    }

    /**
     * In this scheduler, the packet returned is always from the head of the
     * queue.
     *
     * @return the packet to be sent out, or <tt>null</tt> if a list is empty
     * @pre $none
     * @post $none
     */
    public synchronized Packet deque()
    {
        if (pktList.size() == 0) {
            return null;
        }

        Packet p = (Packet) pktList.remove(0);
        return p;
    }

    /**
     * Determines whether the scheduler is currently keeping any packets in
     * its queue(s).
     *
     * @return <tt>true</tt> if no packets are enqueued, <tt>false</tt>
     *         otherwise
     * @pre $none
     * @post $none
     */
    public synchronized boolean isEmpty() {
        return pktList.isEmpty();
    }

    /**
     * Determines the number of packets that are currently enqueued in this
     * scheduler.
     *
     * @return the number of packets enqueud by this scheduler.
     * @pre $none
     * @post $none
     */
    public synchronized int size() {
        return pktList.size();
    }

    /**
     * Gets the ID of this scheduler.
     * @return the ID of this scheduler or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getSchedID()
    {
        System.out.println(name_ + ".getID(): No ID is set for this object.");
        return -1;
    }

    /**
     * Gets the name of this scheduler.
     * @return the name of this scheduler
     * @pre $none
     * @post $none
     */
    public String getSchedName() {
        return name_;
    }

    /**
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @pre rate > 0
     * @post $none
     */
    public boolean setBaudRate(double rate)
    {
        if (rate <= 0.0) {
            return false;
        }
        baudRate_ = rate;
        return true;
    }

    /**
     * Returns the baud rate of the egress port that is using this scheduler.
     * If the baud rate is zero, it means you haven't set it up.
     * @return the baud rate in bits/s
     * @see gridsim.net.PacketScheduler#setBaudRate(double)
     * @pre $none
     * @post $result >= 0
     */
    public double getBaudRate() {
        return baudRate_;
    }

    /**
     * Sets the router ID that hosts this scheduler.
     * @param routerID  the router ID that hosts this scheduler
     * @return <tt>true</tt> if successful or <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean setRouterID(int routerID)
    {
        System.out.println(name_ + ".setRouterID(): Router ID is not required");
        return false;
    }

    /**
     * Gets the router ID that hosts this scheduler.
     * @return the router ID or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getRouterID() {
        return -1;
    }

    /**
     * Initializes all private attributes
     */
    private void init(String name, double rate) throws ParameterException
    {
        if (name == null) {
            throw new ParameterException("FIFOScheduler(): Name is null.");
        }

        if (rate <= 0) {
		    throw new ParameterException("FIFOScheduler(): Baudrate <= 0.");
        }

        name_ = name;
        baudRate_ = rate;
        pktList = new Vector();
    }


} // end class

