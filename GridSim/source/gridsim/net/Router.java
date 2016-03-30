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
 * Router.java - Simulates a network router
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import gridsim.util.SimReport;
import java.util.*;


/**
 * This class implements a Router which forwards data from one entity to
 * another.
 * <p>
 * This abstract class only contains abstract methods to connect other
 * routers and hosts. In addition, common functionalities are available
 * in this class for its children classes to use.
 * However, this class does not provide methods on how to setup forwarding
 * tables, so the design of that is left to the user if he/she wants to
 * create a Router with some specific routing algorithm.
 * <p>
 * Few important notes to consider when extending from this class:
 * <ul>
 * <li>do not override {@link #body()} method as
 *     it contains code/functionality to register this entity to
 *     {@link gridsim.GridInformationService} entity, and finalizing
 *     logging information before exiting the simulation.
 * <li>must overridden {@link #advertiseHosts()} method. It is
 *     needed for advertising all hosts or
 *     entities connected to this entity to adjacent routers.
 * <li>{@link #registerOtherEntity()} method : for registering other
 *     event type/tag to {@link gridsim.GridInformationService}.
 *     This is optional.
 * <li>must overridden {@link #processEvent(Sim_event)} method.
 *     It is needed for processing incoming events.
 * <li>do not forget to implement logging or recording functionalities
 *     for incoming and outgoing packets. <br>
 *     {@link gridsim.util.SimReport} object is created in this class,
 *     hence, you only need to use {@link #write(String)} method.
 * </ul>
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public abstract class Router extends Sim_entity
{
    /** An attribute that denotes the maximum number of hopcount used for 
     * advertising adjacent routers. The default value is 15.
     * This attribute is used to prevent from a count-to-infinity scenario.
     * Note that if you have a large topology with many routers, you can
     * set this attribute to a higher number.
     */
    public static int MAX_HOP_COUNT = 15;

    /** An attribute that logs incoming and outgoing packets into a file.
     * Use {@link #write(String)} to log or record the information.
     */
    protected SimReport reportWriter_;

    /** Denotes a time delay (in second) for sending events in the future. */
    protected static int DELAY = 2;  // in seconds


    /**
     *Creates a new RIPRouter object. By default, <b>no recording or logging</b>
     * is done for packets' activities. If you want to log operations of this
     * entity, please use {@link #Router(String, boolean)}.
     *
     * @param name Name of this router
     * @throws NullPointerException This happens when name is empty or null
     * @see #Router(String, boolean)
     * @pre name != null
     * @post $none
     */
    public Router(String name) throws NullPointerException
    {
        super(name);
        reportWriter_ = null;
    }

    /**
     * Creates a new Router object with logging facility if it is turned on.
     * <br>
     * NOTE: If logging facility is turned on, there are some overheads
     * in terms of performance and memory consumption.
     *
     * @param name      Name of this router
     * @param trace     <tt>true</tt> if you want to record this router's
     *                  activity, <tt>false</tt> otherwise
     * @throws NullPointerException This happens when name is empty or null
     * @pre name != null
     * @post $none
     */
    public Router(String name, boolean trace) throws NullPointerException
    {
        super(name);
        try
        {
            if (trace == true) {
                reportWriter_ = new SimReport(name + "_report");
            }
            else {
                reportWriter_ = null;
            }
        }
        catch (Exception e)
        {
            System.out.println(name + "(): Exception error.");
            System.out.println( e.getMessage() );
            reportWriter_ = null;
        }
    }

    /**
     * Joins two routers with a Link.
     *
     * @param router    The router on the other side to which this one will
     *                  be attached.
     * @param link      This is the link that will be used to connect the two
     *                  routers.
     * @param thisSched The scheduling policy used on this routers egress port
     *                  when sending data through it.
     * @param otherSched    The scheduling policy that will be used on the
     *                      egress port of the router being connected to when
     *                      sending data to this router.
     * @pre router != null
     * @pre link != null
     * @pre thisSched != null
     * @pre otherSched != null
     * @post $none
     */
    public abstract void attachRouter(Router router, Link link,
                PacketScheduler thisSched, PacketScheduler otherSched);

    /**
     * Joins two routers together. This is called by the routers themselves
     * and should not be called by other entities.
     *
     * @param router    The Router to which this router will be connected.
     * @param link      The Link that will be used to join these routers.
     * @param sched     The scheduling policy used on the egress port of the
     *                  router when sending data through this route.
     * @pre router != null
     * @pre link != null
     * @pre sched != null
     * @post $none
     */
    public abstract void attachRouter(Router router, Link link,
                PacketScheduler sched);

    /**
     * Attaches an entity to this router. The link between the router and the
     * entity being attached is taken from
     * {@link gridsim.GridSimCore#getLink()}.
     *
     * @param entity    The entity to be attached.
     * @param sched     The scheduling policy that will be used on the egress
     *                  port when the router sends data to the entity being
     *                  joined.
     * @see gridsim.GridSimCore#getLink()
     * @pre entity != null
     * @pre sched != null
     * @post $none
     */
    public abstract void attachHost(GridSimCore entity, PacketScheduler sched);

    /**
     * Returns the Scheduler associated with a packet. Each packet has a
     * destination. The router returns the scheduler that it would use to
     * scheduler this packet if it were traversing this router. Once a
     * reference to the scheduler is obtained, different parameters
     * (like priorities, weights etc.) could be modified.
     *
     * @param np    NetPacket for which the associated scheduler is to be
     *              returned. This can be used to set weigths, priorities, etc.
     *              as the case may be on the Scheduler.
     * @return the packet's scheduler
     * @pre np != null
     * @post $none
     */
    public abstract PacketScheduler getScheduler(Packet np);

    /**
     * Returns the Scheduler that the router would use to reach a particular
     * destination. Once a reference to the scheduler is obtained, different
     * parameters (like priorities, weights etc.) could be modified.
     *
     * @param dest  id of the destination for which the Scheduler is required.
     * @return the packet's scheduler
     * @pre dest > 0
     * @post $none
     */
    public abstract PacketScheduler getScheduler(int dest);

    /**
     * Returns the Scheduler that the router would use to reach a particular
     * destination. This can be used to set weigths, priorities etc. as the
     * case may be on the Scheduler
     *
     * @param dest Name of the destination for which the Scheduler is required.
     * @return the packet's scheduler
     * @pre dest != null
     * @post $none
     */
    public abstract PacketScheduler getScheduler(String dest);

    /**
     * This method prints out the forwarding table of the router in a human
     * readable form.
     * @pre $none
     * @post $none
     */
    public abstract void printRoutingTable();

    /**
     * Handles incoming requests. <b>DO NOT</b> overrides this method as
     * it contains code/functionality to register this entity to
     * {@link gridsim.GridInformationService} entity, and finalizing
     * logging information before exiting the simulation.
     * <p>
     * This method also calls these methods in the following order:
     * <ol>
     * <li>must overridden {@link #advertiseHosts()} method. It is
     *     needed for advertising all hosts or
     *     entities connected to this entity to adjacent routers.
     * <li> {@link #registerOtherEntity()} method : for registering other
     *      event type/tag to {@link gridsim.GridInformationService}.
     *      This is optional.
     * <li>must overridden {@link #processEvent(Sim_event)} method.
     *     It is needed for processing incoming events.
     * </ol>
     *
     * @pre $none
     * @post $none
     */
    public void body()
    {
        //register oneself
        write("register this entity to GridInformationService entity.");
        super.sim_schedule(GridSim.getGridInfoServiceEntityId(),
                GridSimTags.SCHEDULE_NOW, GridSimTags.REGISTER_ROUTER,
                new Integer(super.get_id()) );

        // methods to be overriden by children classes
        advertiseHosts();
        registerOtherEntity();
        sendInitialEvent();

        // Process incoming events
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            //Sim_event ev = new Sim_event();
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                write("receives, END_OF_SIMULATION, signal, from, " +
                      GridSim.getEntityName(ev.get_src()) );
                processEndSimulation();
                break;
            }

            // process the received event
            processEvent(ev);
        }

        // finalize logging before exiting
        if (reportWriter_ != null) {
            reportWriter_.finalWrite();
        }
    }

    /**
     * All hosts connected to this router are advertised to adjacent routers
     * @pre $none
     * @post $none
     */
    protected abstract void advertiseHosts();

    /**
     * Overrides this method when creating a new type of router.
     * This method is called by {@link #body()} for incoming unknown tags.
     * <p>
     * The services or tags available for this resource are:
     * <ul>
     * <li> {@link gridsim.GridSimTags#PKT_FORWARD}
     * <li> {@link gridsim.GridSimTags#JUNK_PKT}
     * <li> {@link gridsim.GridSimTags#ROUTER_AD}
     * </ul>
     *
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected abstract void processEvent(Sim_event ev);

    /**
     * Overrides this method when making a new type of router.
     * This method is called by the {@link #body()} method 
     * to register to other type of the
     * {@link gridsim.GridInformationService} entity. In doing so, you
     * need to create a new child class extending from the
     * {@link gridsim.GridInformationService} class.
     * <br>
     * <b>NOTE:</b> You do not need to override the {@link #body()} method, if
     * you use this method.
     *
     * @pre $none
     * @post $none
     * @see gridsim.GridInformationService
     */
    protected void registerOtherEntity() {
        // ... empty
    }
    
    /**
     * Overrides this method when sending initial event(s) to itself or others
     * <b>BEFORE</b> receiving incoming events from other entities.
     * <br>
     * <b>NOTE:</b> You do not need to override the {@link #body()} method, if
     * you use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void sendInitialEvent() {
        // ... empty
    }

    /**
     * Writes a debug information to a file.
     * The format of information is left to the coder.
     *
     * @param str  a string message
     * @pre str != null
     * @post $none
     */
    protected void write(String str)
    {
        if (reportWriter_ != null) {
            reportWriter_.write(str);
        }
    }

    /**
     * Informs the registered entities regarding to the end of a simulation.
     * @pre $none
     * @post $none
     */
    protected abstract void processEndSimulation();

} // end class

