/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2005, The University of Melbourne, Australia
 */

package gridsim.index;

import gridsim.*;
import gridsim.net.*;
import eduni.simjava.*;
import java.util.*;


/**
 * AbstractGIS is an abstract class which aims to provide skeletons for its
 * chid classes to implement the required base functionalities of a regional
 * GridInformationService (GIS).
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.2
 * @invariant $none
 */
public abstract class AbstractGIS extends GridSimCore
{
    /** System GIS or {@link gridsim.GridInformationService} entity ID. */
    protected int systemGIS_ = -1;

    // if there is a conflict, we can simply change this number
    private static final int GIS_BASE = 1000;

    /** Registers this regional GIS to the
     * {@link gridsim.GridInformationService} or system GIS.
     * This tag should be called from Regional GIS to the system GIS.
     */
    public static final int REGISTER_REGIONAL_GIS = GIS_BASE + 1;

    /** Denotes a grid resource to be registered to this regional GIS entity.
     * This tag is similar to {@link gridsim.GridSimTags#REGISTER_RESOURCE}.
     * This tag should be called from Regional GIS to the system GIS.
     */
    public static final int REGISTER_RESOURCE = GIS_BASE + 2;

    /** Denotes a grid resource, that can support advance reservation, to be
     * registered to this regional GIS entity.
     * This tag is similar to {@link gridsim.GridSimTags#REGISTER_RESOURCE_AR}.
     * This tag should be called from Regional GIS to the system GIS.
     */
    public static final int REGISTER_RESOURCE_AR = GIS_BASE + 3;

    /** Denotes a list of all resources, including the ones that can support
     * advance reservation, that are listed in this regional GIS entity.
     * This tag is similar to {@link gridsim.GridSimTags#RESOURCE_LIST}.
     * This tag should be called from a user to Regional GIS.
     */
    public static final int INQUIRY_LOCAL_RESOURCE_LIST = GIS_BASE + 4;

    /** Denotes a list of resources, that only support
     * advance reservation, that are listed in this regional GIS entity.
     * This tag is similar to {@link gridsim.GridSimTags#RESOURCE_AR_LIST}.
     * This tag should be called from a user to Regional GIS.
     */
    public static final int INQUIRY_LOCAL_RESOURCE_AR_LIST = GIS_BASE + 5;

    /** Denotes a list of resources that are listed in other regional GIS
     * entities.
     * This tag should be called from a user to Regional GIS.
     */
    public static final int INQUIRY_GLOBAL_RESOURCE_LIST = GIS_BASE + 6;

    /** Denotes a list of resources, which support advanced reservation,
     * that are listed in other regional GIS entities.
     * This tag should be called from a user to Regional GIS.
     */
    public static final int INQUIRY_GLOBAL_RESOURCE_AR_LIST = GIS_BASE + 7;

    /** Denotes a list of regional GIS IDs, including this entity ID.
     * This tag should be called from a user to Regional GIS.
     */
    public static final int INQUIRY_REGIONAL_GIS = GIS_BASE + 8;

    /** Denotes an inquiry regarding to a list of local resources.
     * This tag should be called from a Regional GIS to another.
     */
    public static final int GIS_INQUIRY_RESOURCE_LIST = GIS_BASE + 9;

    /** Denotes a result regarding to a list of local resources.
     * This tag should be called from a Regional GIS to a sender Regional GIS.
     */
    public static final int GIS_INQUIRY_RESOURCE_RESULT = GIS_BASE + 10;

    /** Denotes an inquiry regarding to a list of local resources,
     * which supports advanced reservation.
     * This tag should be called from a Regional GIS to another.
     */
    public static final int GIS_INQUIRY_RESOURCE_AR_LIST = GIS_BASE + 11;

    /** Denotes a result regarding to a list of local resources,
     * which supports advanced reservation.
     * This tag should be called from a Regional GIS to a sender Regional GIS.
     */
    public static final int GIS_INQUIRY_RESOURCE_AR_RESULT = GIS_BASE + 12;

    /** Denotes an inquiry regarding to a resource failure.
     * The user sends this tag to the GIS to update its list of resources.
     */
    public static final int NOTIFY_GIS_RESOURCE_FAILURE = GIS_BASE + 13;


    /**
     * Creates a new regional GIS entity
     * @param name  this regional GIS name
     * @param link  a network link to this entity
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    protected AbstractGIS(String name, Link link) throws Exception {
        super(name, link);
    }

    /**
     * Handles incoming requests to this entity, <b>DO NOT OVERRIDE</b> this
     * method. Implement the abstract methods instead.
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // register to system GIS first
        systemGIS_ = GridSim.getGridInfoServiceEntityId();
        int register = GridSimTags.REGISTER_REGIONAL_GIS;
        Integer id = new Integer( super.get_id() );
        super.send(super.output, GridSimTags.SCHEDULE_NOW, register,
                   new IO_data(id, Link.DEFAULT_MTU, systemGIS_) );

        // Below method is for a child class to override
        registerOtherEntity();

        // Process incoming events until END_OF_SIMULATION is received from the
        // GridInformationService entity
        while ( Sim_system.running() )
        {
            Sim_event ev = new Sim_event();
            super.sim_get_next(ev);     // get the next event in the queue

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                processEndSimulation();
                break;
            }

            processEvent(ev);   // process the received event
            ev = null;          // reset the event object
        }

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();
    }

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs supporting Advanced Reservation, that are registered in other
     * regional GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGlobalResourceARList(Sim_event ev);

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs, that are registered in other regional GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGlobalResourceList(Sim_event ev);

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs, that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processResourceList(Sim_event ev);

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs supporting Advanced Reservation, that are registered to this
     * regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processResourceARList(Sim_event ev);

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs, that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGISResourceList(Sim_event ev);

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs supporting Advanced Reservation,
     * that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGISResourceARList(Sim_event ev);

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGISResourceResult(Sim_event ev);

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list supporting Advanced Reservation. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processGISResourceARResult(Sim_event ev);

    /**
     * Process a registration request from a resource entity
     * supporting Advanced Reservation to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processRegisterResourceAR(Sim_event ev);

    /**
     * Process a registration request from a resource entity to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processRegisterResource(Sim_event ev);

    /**
     * Process an incoming request about getting a list of regional GIS IDs
     * (including this entity ID), that are registered to the
     * {@link gridsim.GridInformationService} or system GIS.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processInquiryRegionalGIS(Sim_event ev);

    /**
     * Process an incoming request that uses a user-defined tag. This method
     * is useful for creating a new regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected abstract void processOtherEvent(Sim_event ev);

    /**
     * Registers other information to {@link gridsim.GridInformationService} or
     * system GIS.
     * @pre $none
     * @post $none
     */
    protected abstract void registerOtherEntity();

    /**
     * Informs the registered entities regarding to the end of a simulation.
     * @pre $none
     * @post $none
     */
    protected abstract void processEndSimulation();

    /**
     * Process incoming events one by one
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        switch ( ev.get_tag() )
        {
            // register resource
            case GridSimTags.REGISTER_RESOURCE:
            case AbstractGIS.REGISTER_RESOURCE:
                notifySystemGIS(ev, GridSimTags.REGISTER_RESOURCE);
                processRegisterResource(ev);
                break;

            // register AR resource
            case GridSimTags.REGISTER_RESOURCE_AR:
            case AbstractGIS.REGISTER_RESOURCE_AR:
                notifySystemGIS(ev, GridSimTags.REGISTER_RESOURCE_AR);
                processRegisterResourceAR(ev);
                break;

            // get resource list of this entity
            case GridSimTags.RESOURCE_LIST:
            case AbstractGIS.INQUIRY_LOCAL_RESOURCE_LIST:
                processResourceList(ev);
                break;

            // get AR resource list of this entity
            case GridSimTags.RESOURCE_AR_LIST:
            case AbstractGIS.INQUIRY_LOCAL_RESOURCE_AR_LIST:
                processResourceARList(ev);
                break;

            // get resource list of other regional GIS
            case AbstractGIS.INQUIRY_GLOBAL_RESOURCE_LIST:
                processGlobalResourceList(ev);
                break;

            // get AR resource list of other regional GIS
            case AbstractGIS.INQUIRY_GLOBAL_RESOURCE_AR_LIST:
                processGlobalResourceARList(ev);
                break;

            // get a list of regional GIS
            case AbstractGIS.INQUIRY_REGIONAL_GIS:
                processInquiryRegionalGIS(ev);
                break;

            // get resource list from this GIS which is needed by other GIS
            case AbstractGIS.GIS_INQUIRY_RESOURCE_LIST:
                processGISResourceList(ev);
                break;

            // get AR resource list from this GIS which is needed by other GIS
            case AbstractGIS.GIS_INQUIRY_RESOURCE_AR_LIST:
                processGISResourceARList(ev);
                break;

            // get resource list from other GIS
            case AbstractGIS.GIS_INQUIRY_RESOURCE_RESULT:
                processGISResourceResult(ev);
                break;

            // get AR resource list from other GIS
            case AbstractGIS.GIS_INQUIRY_RESOURCE_AR_RESULT:
                processGISResourceARResult(ev);
                break;

            // Ping packet
            case GridSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            // handle other events
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Notify {@link gridsim.GridInformationService} or system GIS about a
     * specific request as defined in the tag name.<br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     *
     * @param ev    a Sim_event object (or requests to be sent to system GIS)
     * @param tag   a tag name or type of request
     * @return <tt>true</tt> if a request has been sent, <tt>false</tt>
     *         otherwise
     * @pre ev != null
     * @post $none
     */
    protected boolean notifySystemGIS(Sim_event ev, int tag)
    {
        boolean result = false;
        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            result = true;
            super.send(super.output, GridSimTags.SCHEDULE_NOW, tag,
                       new IO_data(obj, Link.DEFAULT_MTU, systemGIS_) );
        }
        return result;
    }

   /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPingRequest(Sim_event ev)
    {
        try
        {
            InfoPacket pkt = (InfoPacket) ev.get_data();
            pkt.setTag(GridSimTags.INFOPKT_RETURN);
            pkt.setDestID( pkt.getSrcID() );

            // sends back to the sender
            super.send(super.output, 0.0, GridSimTags.INFOPKT_RETURN,
                       new IO_data(pkt, pkt.getSize(), pkt.getSrcID()) );
        }
        catch (Exception e) {
            // ... empty
        }
    }

} // end class

