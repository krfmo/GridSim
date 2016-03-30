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
 * RateControlledScheduler.java - Implements a Rate controlled scheduler
 *
 */

package gridsim.net;

import gridsim.net.*;
import eduni.simjava.*;
import gridsim.*;
import java.util.*;


/**
 * RateControlledScheduler is an implementation of a rate-jitter controlling
 * regulator. For more details refer to <b>H. Zhang and D.Ferrari's</b>
 * INFOCOM '93 paper <i>Rate-Controlled Static-Priority Queueing</i>.
 * <p>
 * RateControlledScheduler can be used to control the bandwidth that
 * is assigned to each class of user at a Router. This is a non-work conserving
 * algorithm, which means that the router can remain idle even if there are
 * packets in its queue.
 * <p>
 * At a RateControlledScheduler each class of users is assigned a certain
 * percentage of bandwidth, and the scheduler makes sure that each class remains
 * constrained within its bandwidth limits at all times.
 *
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 * @since  GridSim Toolkit 4.0
 * @invariant $none
 */
public class RateControlledScheduler extends Sim_entity
                                     implements PacketScheduler
{
    private double baudRate_;       // baud rate of a link
    private int routerID_ ;         // router ID that hosts this scheduler
    private double[] rates_;        // rates of each class
    private int numClasses_;        // num of classes or ToS
    private ArrayList[] packets_;   // intermediate queue
    private ArrayList pktList_;     // final queue
    private ArrayList classType_;   // an integer storing the class types

    private static final int DEQUEUE_PACKET = 1000;
    private static final int INTERNAL_DEQUEUE = 1001;


    /**
     * Creates a new RateControlled packet scheduler with the specified name
     * and baud rate (in bits/s).
     * The name can be useful for debugging purposes, but serves
     * no functional purposes.
     * Don't forget to set the rate for each packet class by using the
     * {@link #setRates(double[])} method.
     *
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @param numClasses number of classes for prioritizing a packet
     * @throws ParameterException This happens when the name is null or
     *                   the baud rate <= 0 or num of classes <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @pre numClasses > 0
     * @post $none
     */
    public RateControlledScheduler(String name, double baudRate, int numClasses)
                         throws ParameterException
    {
        super(name);
        if (baudRate <= 0) {
            throw new ParameterException("Baudrate must be greater than 0");
        }

        if (numClasses <= 0) {
            throw new ParameterException("Num of class must be greater than 0");
        }

        routerID_ = -1;
        baudRate_ = baudRate;
        numClasses_ = numClasses;
        init(numClasses);
    }

    /**
     * Creates a new RateControlled packet scheduler with the specified name.
     * The baud rate is left at 0, and should be set with
     * {@link gridsim.net.PacketScheduler#setBaudRate(double)}
     * before the simulation starts.
     * The name can be useful for debugging purposes, but serves
     * no functional purposes.
     * Don't forget to set the rate for each packet class by using the
     * {@link #setRates(double[])} method.
     *
     * @param name       Name of this scheduler
     * @param numClasses number of classes for prioritizing a packet
     * @throws ParameterException This happens when the name is null or
     *                   the baud rate <= 0 or num of classes <= 0
     * @pre name != null
     * @pre numClasses > 0
     * @post $none
     */
    public RateControlledScheduler(String name, int numClasses)
                                   throws ParameterException
    {
        super(name);
        if (numClasses <= 0) {
            throw new ParameterException("Num of class must be greater than 0");
        }

        routerID_ = -1;
        baudRate_ = 0;
        numClasses_ = numClasses;
        init(numClasses);
    }

    /**
     * Creates a new RateControlled packet scheduler with the specified name
     * and baud rate (in bits/s).
     * The name can be useful for debugging purposes, but serves
     * no functional purposes.
     * Don't forget to set the rate for each packet class by using the
     * {@link #setRates(double[])} method.
     *
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @param routerID   the router ID that hosts this packet scheduler
     * @param numClasses number of classes for prioritizing a packet
     * @throws ParameterException This happens when the name is null or
     *                   router ID <= 0 or the baud rate <= 0 or
     *                   num of classes <= 0
     * @pre name != null
     * @pre baudRate > 0
     * @pre routerID > 0
     * @pre numClasses > 0
     * @post $none
     */
    public RateControlledScheduler(String name, double baudRate, int routerID,
                                   int numClasses) throws ParameterException
    {
        super(name);
        if (baudRate <= 0) {
            throw new ParameterException("Baud rate must be greater than 0");
        }

        if (routerID <= 0) {
            throw new ParameterException("Router ID must be greater than 0");
        }

        if (numClasses <= 0) {
            throw new ParameterException("Num of class must be greater than 0");
        }

        baudRate_ = baudRate;
        routerID_ = routerID;
        numClasses_ = numClasses;
        init(numClasses);
    }

    /**
     * Initializes all private attributes
     * @param numClasses    number of classes for prioritizing a packet
     * @pre $none
     * @post $none
     */
    private void init(int numClasses)
    {
        rates_ = null;
        classType_ = new ArrayList(numClasses);
        packets_ = new ArrayList[numClasses];
        pktList_ = new ArrayList();

        for (int i = 0; i < numClasses; i++)
        {
            packets_[i] = new ArrayList();
            classType_.add( new Integer(i) );
        }
    }

    /**
     * Gets the number of classes for prioritizing incoming packets
     * @return number of classes
     * @pre $none
     * @post $none
     */
    public int getNumClass() {
        return numClasses_;
    }

    /**
     * Gets the list of rates for each packet class
     * @return the list of rates or <tt>null</tt> if empty
     * @pre $none
     * @post $none
     */
    public double[] getRate() {
        return rates_;
    }

    /**
     * Handles an incoming events coming from a specified Router
     * @pre $none
     * @post $none
     */
    public void body()
    {
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);     // get the incoming event

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }

            // process the received event
            processEvent(ev);
        }
    }

    /**
     * Processes an incoming event based on its tag name
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        switch (ev.get_tag())
        {
            case GridSimTags.SCHEDULER_ENQUE:
                this.enque(ev);
                break;

            case INTERNAL_DEQUEUE:
                this.internalDequeue(ev);
                break;

            case DEQUEUE_PACKET:
                this.dequeue(ev);
                break;

            default:
                System.out.println(super.get_name() +
                        ".processEvent: Warning - unknown tag name.");
                break;
        }
    }

    /**
     * In this scheduler, the packet is put into the tail of the queue.
     * There is no buffer management, so packets are never dropped, and the
     * queue can grow as long as system memory is available.
     *
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void enque(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        try
        {
            Packet pkt = (Packet) ev.get_data();
            int type = pkt.getNetServiceType();

            // check whether the class type is correct or not
            if (type >= packets_.length)
            {
                System.out.println(super.get_name() +".enque(): Warning - " +
                    " packet class = " + type + ", num of classes = " +
                    packets_.length);
                type = 0;
            }

            packets_[type].add(pkt);  // add a packet into its respective class
            if (packets_[type].size() == 1)
            {
                // send an internal event to itself
                double delay = (pkt.getSize() * NetIO.BITS) / rates_[type];
                Integer obj = (Integer) classType_.get(type);
                super.sim_schedule(super.get_id(),delay,INTERNAL_DEQUEUE,obj);
            }
        }
        catch (Exception e) {
            // ... empty
        }
    }

    /**
     * In this scheduler, the packet returned is always from the head of the
     * queue.
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void internalDequeue(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        try
        {
            Integer classTypeInt = (Integer) ev.get_data();
            int type = classTypeInt.intValue();

            // check whether the class type is correct or not
            if (type >= packets_.length)
            {
                System.out.println(super.get_name() +
                    ".internalDequeue(): Warning - packet class = " + type +
                    ", num of classes = " + packets_.length);
                type = 0;
            }

            // if no packets in the array, then exit
            if (packets_[type].isEmpty() == true) {
                return;
            }

            // add to final buffer
            Packet pkt = (Packet) packets_[type].remove(0);
            pktList_.add(pkt);

            // send an internal event to itself
            double delay = 0;
            if (pktList_.size() == 1)
            {
                delay = (pkt.getSize() * NetIO.BITS) / baudRate_;
                super.sim_schedule(super.get_id(), delay, DEQUEUE_PACKET);
            }

            // schedule the next packet
            if (packets_[type].isEmpty() == false)
            {
                Packet nextPkt = (Packet) packets_[type].get(0);

                // rate limit next packet
                delay = (nextPkt.getSize() * NetIO.BITS) / rates_[type];
                super.sim_schedule(super.get_id(), delay, INTERNAL_DEQUEUE,
                                   classTypeInt);
            }
        }
        catch (Exception e) {
            // .... empty
        }
    }

    /**
     * Dequeues a packet and send it to a router
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private synchronized void dequeue(Sim_event ev)
    {
        if (ev == null || pktList_.isEmpty() == true) {
            return;
        }

        try
        {
            Packet pkt = (Packet) pktList_.remove(0);
            super.sim_schedule(routerID_, 0, GridSimTags.SCHEDULER_DEQUE, pkt);
            if (pktList_.isEmpty() == false)
            {
                Packet nextPkt = (Packet) pktList_.get(0);
                double delay = (nextPkt.getSize() * NetIO.BITS) / baudRate_;
                super.sim_schedule(super.get_id(), delay, DEQUEUE_PACKET);
            }
        }
        catch (Exception e) {
            // ... empty
        }
    }

    /**
     * This method allows you to set different rates for different types of
     * traffic. Traffic of class <tt>n</tt> are assigned a rate of
     * <tt>rates[n]</tt>.
     * The higher the rate of a class, the better the service it receives.
     * NOTE: Each rate must be a positive number.
     *
     * @param rates   a linear array of the rates to be assigned to different
     *                classes of traffic.
     * @pre rates != null
     * @post $none
     */
    public boolean setRates(double[] rates)
    {
        // error checking
        if (rates == null || rates.length != numClasses_) {
            return false;
        }

        // the value of each rate must be a positive number
        for (int i = 0; i < rates.length; i++)
        {
            if (rates[i] <= 0)
            {
                System.out.println(super.get_name() +
                    ".setRates(): Error - the rate must be a positive number.");
                return false;
            }
        }

        this.rates_ = rates;
        return true;
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
    public synchronized boolean isEmpty()
    {
        for (int i = 0; i < numClasses_; i++)
        {
            if (packets_[i].isEmpty() == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines the number of packets that are currently enqueued in this
     * scheduler.
     *
     * @return the number of packets enqueud by this scheduler.
     * @pre $none
     * @post $none
     */
    public synchronized int size()
    {
        int size = 0;
        for (int i = 0; i < numClasses_; i++) {
            size += (packets_[i]).size();
        }

        return size;
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
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @pre rate > 0
     * @post $none
     */
    public boolean setBaudRate(double rate)
    {
        if (rate <= 0) {
            return false;
        }

        baudRate_ = rate;
        return true;
    }

    /**
     * Gets the name of this scheduler.
     * @return the name of this scheduler
     * @pre $none
     * @post $none
     */
    public String getSchedName() {
        return super.get_name();
    }

    /**
     * Gets the ID of this scheduler.
     * @return the ID of this scheduler or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getSchedID() {
        return super.get_id();
    }

    /**
     * Sets the router ID that hosts this scheduler.
     * @param routerID  the router ID that hosts this scheduler
     * @return <tt>true</tt> if successful or <tt>false</tt> otherwise
     * @pre routerID > 0
     * @post $none
     */
    public boolean setRouterID(int routerID)
    {
        if (routerID <= 0) {
            return false;
        }

        this.routerID_ = routerID;
        return true;
    }

    /**
     * Gets the router ID that hosts this scheduler.
     * @return the router ID or <tt>-1</tt> if no ID is found
     * @pre $none
     * @post $none
     */
    public int getRouterID() {
        return routerID_;
    }

    /**
     * Puts a packet into the queue -- <b>This method is not used</b>
     *
     * @param np    A Packet to be enqued by this scheduler.
     * @return <tt>true</tt> if enqued, <tt>false</tt> otherwise
     * @pre pnp != null
     * @post $none
     */
    public boolean enque(Packet np)
    {
        System.out.println(super.get_name()+".enque(): This method is empty.");
        return false;
    }

    /**
     * The method deque() has to decide which queue is to be
     * served next -- <b>This method is not used</b>
     *
     * @return the packet to be sent out or <tt>null</tt> if empty
     * @pre $none
     * @post $none
     */
    public Packet deque()
    {
        System.out.println(super.get_name()+".deque(): This method is empty.");
        return null;
    }

} // end class

