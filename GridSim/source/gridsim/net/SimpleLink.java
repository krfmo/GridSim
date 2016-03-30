/*
 * ** Network and Service Differentiation Extensions to GridSim 2.2 **
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
 * Link.java - Simulates a network link
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import java.util.*;


/**
 * This class realizes a link in its simplest form. It implements a simplex link
 * that just takes a packet in from one end, delays it by a user specified time
 * (i.e. propagation delay) and trasmits it to the other end.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public class SimpleLink extends Link
{
    private Vector q_;
    private double lastUpdateTime_; // a timer to denote the last update time
    private int inEnd1_;
    private int outEnd1_;
    private int inEnd2_;
    private int outEnd2_;


    /**
     * Constructs a Link which simulates a physical link between two entities.
     *
     * @param name      Name of this Link
     * @param baudRate  baud rate of this link (bits/s)
     * @param propDelay Propogation delay of the Link in milli seconds
     * @param MTU       Maximum Transmission Unit of the Link in bytes.
     *                  Packets which are larger than the MTU should be split
     *                  up into MTU size units. <br>
     *                  For e.g. a 1024 byte packet trying to cross a 576 byte
     *                  MTU link should get split into 2 packets of 576 bytes
     *                  and 448 bytes.
     * @throws NullPointerException This happens when name is empty or null
     * @throws ParameterException   This happens for the following conditions:
     *      <ul>
     *          <li> name is null
     *          <li> baudRate <= 0
     *          <li> propDelay <= 0
     *          <li> MTU <= 0
     *      </ul>
     *
     * @pre name != null
     * @pre baudRate > 0
     * @pre propDelay > 0
     * @pre MTU > 0
     * @post $none
     */
    public SimpleLink(String name, double baudRate, double propDelay, int MTU)
                      throws ParameterException, NullPointerException
    {
        super(name, baudRate, propDelay, MTU);
        init();
    }

    /**
     * Constructs a link with some default parameters. It simulates a link with
     * a default value of baud rate, propagation delay and MTU.
     *
     * @param name  Name of this Link
     * @throws NullPointerException This happens when name is empty or null
     * @throws ParameterException   This happens when the given name is empty
     * @see gridsim.net.Link#DEFAULT_BAUD_RATE
     * @see gridsim.net.Link#DEFAULT_PROP_DELAY
     * @see gridsim.net.Link#DEFAULT_MTU
     * @pre name != null
     * @post $none
     */
    public SimpleLink(String name) throws ParameterException,
                                          NullPointerException
    {
        super(name, Link.DEFAULT_BAUD_RATE, Link.DEFAULT_PROP_DELAY,
              Link.DEFAULT_MTU);

        init();
    }

    /**
     * Initialises all attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        lastUpdateTime_ = 0.0;
        q_ = new Vector();
        inEnd1_ = -1;
        outEnd1_ = -1;
        inEnd2_ = -1;
        outEnd2_ = -1;
    }

    /**
     * Connects one entity to another via this link
     * @param end1  an entity
     * @param end2  an entity
     * @pre end1 != null
     * @pre end2 != null
     * @post $none
     */
    public void attach(Sim_entity end1, Sim_entity end2)
    {
        if (end1 == null || end2 == null)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "one or both entities are null.");
            return;
        }

        inEnd1_ = GridSim.getEntityId( "Input_" + end1.get_name() );
        outEnd1_ = GridSim.getEntityId( "Output_" + end1.get_name() );

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd1_ == -1 || outEnd1_ == -1)
        {
            inEnd1_ = end1.get_id();
            outEnd1_ = end1.get_id();
        }

        inEnd2_ = GridSim.getEntityId( "Input_" + end2.get_name() );
        outEnd2_ = GridSim.getEntityId( "Output_" + end2.get_name() );

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd2_ == -1 || outEnd2_ == -1)
        {
            inEnd2_ = end2.get_id();
            outEnd2_ = end2.get_id();
        }
    }

    /**
     * Connects one entity to another via this link
     * @param end1  an Entity name
     * @param end2  an Entity name
     * @pre end1 != null
     * @pre end2 != null
     * @post $none
     */
    public void attach(String end1, String end2)
    {
        if (end1 == null || end2 == null)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "can not connect since one or both entities are null.");
            return;
        }

        if (end1.length() == 0 || end2.length() == 0)
        {
            System.out.println(super.get_name() + ".attach(): Warning - " +
                    "can not connect since one or both entities are null.");
            return;
        }

        inEnd1_ = GridSim.getEntityId("Input_" + end1);
        outEnd1_ = GridSim.getEntityId("Output_" + end1);

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd1_ == -1 || outEnd1_ == -1)
        {
            inEnd1_ = GridSim.getEntityId(end1);
            outEnd1_ = inEnd1_;
        }

        inEnd2_ = GridSim.getEntityId("Input_" + end2);
        outEnd2_ = GridSim.getEntityId("Output_" + end2);

        // if end1 is a router/gateway with no Input and Output port
        if (inEnd2_ == -1 || outEnd2_ == -1)
        {
            inEnd2_ = GridSim.getEntityId(end1);
            outEnd2_ = inEnd2_;
        }
    }

    /**
     * Handles external events that are coming to this link.
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // register oneself to the system GIS
        super.sim_schedule(GridSim.getGridInfoServiceEntityId(),
                           GridSimTags.SCHEDULE_NOW, GridSimTags.REGISTER_LINK,
                           new Integer(super.get_id()) );

        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }

            // process the received event
            processEvent(ev);
            sim_completed(ev);
        }

        while(sim_waiting() > 0)
        {
            // wait for event and ignore
            System.out.println(super.get_name() + ".body(): Ignore !!");
            sim_get_next(ev);
        }
    }

    /**
     * Processes incoming events
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        switch ( ev.get_tag() )
        {
            case GridSimTags.PKT_FORWARD: // for normal packets
            case GridSimTags.JUNK_PKT:    // for background traffic
                enque(ev);
                break;

            case GridSimTags.INSIGNIFICANT:
                processInternalEvent();
                break;

            default:
                System.out.println(super.get_name() + ".body(): Warning - " +
                        "unable to handle request from GridSimTags " +
                        "with constant number " + ev.get_tag());
                break;
        }
    }

    /**
     * Sends an internal event to itself for a certain time period
     * @param time  the delay time
     * @pre time >= 0
     * @post $none
     */
    private synchronized boolean sendInternalEvent(double time)
    {
        if (time < 0.0) {
            return false;
        }

        super.sim_schedule(super.get_id(), time, GridSimTags.INSIGNIFICANT);
        return true;
    }

    /**
     * Processes internal events
     * @pre $none
     * @post $none
     */
    private synchronized void processInternalEvent()
    {
        // this is a constraint that prevents an infinite loop
        // Compare between 2 floating point numbers. This might be incorrect
        // for some hardware platform.
        if ( lastUpdateTime_ == GridSim.clock() ) {
            return;
        }

        lastUpdateTime_ = GridSim.clock();

        if (q_.size() == 0) {
            return;
        }
        else if (q_.size() == 1) {
            deque( (Packet) q_.remove(0) );
        }
        else
        {
            deque( (Packet)q_.remove(0) );
            sendInternalEvent(super.delay_ / super.MILLI_SEC);  // delay in ms
        }
    }

    /**
     * Puts an event into a queue and sends an internal event to itself
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private synchronized void enque(Sim_event ev)
    {
        q_.add( ev.get_data() );
        if (q_.size() == 1) {
            sendInternalEvent(super.delay_ / super.MILLI_SEC); // delay in ms
        }
    }

    /**
     * Sends a packet to the next destination
     * @param np    a packet
     * @pre np != null
     * @post $none
     */
    private synchronized void deque(Packet np)
    {
        int dest = getNextHop(np);
        if (dest == -1) {
            return;
        }

        // other side is a Sim_entity
        int tag = 0;
        if (dest == outEnd2_ || dest == outEnd1_)
        {
            // for junk packets only
            if (np.getTag() == GridSimTags.JUNK_PKT) {
                tag = GridSimTags.JUNK_PKT;
            }
            // for other packets
            else {
                tag = GridSimTags.PKT_FORWARD;
            }
        }
        // other side is a GridSim entity
        else {
            tag = np.getTag();
        }

        // sends the packet
        super.sim_schedule(dest, GridSimTags.SCHEDULE_NOW, tag, np);
    }

    /**
     * Determines which end to send the event to
     * since sending entities are of the form Output_entityName.
     * We need to check whether the source name ends with either end1 or end2
     * the other end is the destination
     * @param np    a packet
     * @pre np != null
     * @post $none
     */
    private synchronized int getNextHop(Packet np)
    {
        int dest = -1;
        int src = np.getLast();

        // check if source is from outEnd1_
        if (src == outEnd1_) {
            dest = inEnd2_;
        }
        // or source is from outEnd2_
        else if (src == outEnd2_) {
            dest = inEnd1_;
        }

        return dest;
    }

} // end class

