/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Created on: Nov 2006.
 * Copyright (c) 2007, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

package gridsim.resFailure;

import gridsim.resFailure.*;
import gridsim.*;
import gridsim.net.*;
import gridsim.index.*;
import java.util.LinkedList;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.resFailure.FailureMsg;
import java.io.FileWriter;


/**
 * GridResourceWithFailure is based on {@link gridsim.GridResource}, but with
 * added failure functionalities.
 * GridResourceWithFailure extends the {@link gridsim.GridSimCore} class for
 * gaining communication and concurrent entity capabilities.
 * An instance of this class stimulates a resource
 * with properties defined in an object of
 * {@link gridsim.ResourceCharacteristics} class.
 *
 * @author       Agustin Caminero and Anthony Sulistio
 * @since        GridSim Toolkit 4.1
 * @see gridsim.GridResource
 * @see gridsim.ResourceCharacteristics
 * @see gridsim.resFailure.AllocPolicyWithFailure
 * @invariant $none
 */
public class GridResourceWithFailure extends GridSimCore
{
    /** Characteristics of this resource */
    protected ResourceCharacteristics resource_;

    /** a ResourceCalendar object */
    protected ResourceCalendar resCalendar_;

    /** A resource's scheduler. This object is responsible for scheduling and
     * and executing submitted Gridlets.
     */
    protected AllocPolicy policy_;

    /** A scheduler type of this resource, such as FCFS, Round Robin, etc */
    protected int policyType_;

    /** Integer object size, including its overhead */
    protected final int SIZE = 12;

    /** Regional GIS entity name */
    protected String regionalGISName_;

    // a flag to denote whether to record events into a file or not
    private boolean record_ = false;


    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use
     * {@link #GridResourceWithFailure(String, double, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicyWithFailure)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param seed       the initial seed
     * @param resource   an object of ResourceCharacteristics
     * @param peakLoad   the load during peak times
     * @param offPeakLoad   the load during off peak times
     * @param relativeHolidayLoad   the load during holiday times
     * @param weekends   a linked-list contains the weekend days
     * @param holidays   a linked-list contains the public holidays
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see #GridResourceWithFailure(String, double,
     *      ResourceCharacteristics, ResourceCalendar, AllocPolicyWithFailure)
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @post $none
     */
    public GridResourceWithFailure(String name, double baud_rate, long seed,
            ResourceCharacteristics resource, double peakLoad,
            double offPeakLoad, double relativeHolidayLoad,
            LinkedList weekends, LinkedList holidays) throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;

        resCalendar_ = new ResourceCalendar(resource_.getResourceTimeZone(),
                peakLoad, offPeakLoad, relativeHolidayLoad, weekends,
                holidays, seed);

        policy_ = null;

        init();
    }

    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use
     * {@link #GridResourceWithFailure(String, double, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicyWithFailure)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see #GridResourceWithFailure(String, double,
     *      ResourceCharacteristics, ResourceCalendar, AllocPolicyWithFailure)
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @pre calendar != null
     * @post $none
     */
    public GridResourceWithFailure(String name, double baud_rate,
                ResourceCharacteristics resource, ResourceCalendar calendar)
                throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use this constructor and then
     * overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.resFailure.AllocPolicyWithFailure
     * @pre name != null
     * @pre baud_rate > 0
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public GridResourceWithFailure(String name, double baud_rate,
                ResourceCharacteristics resource, ResourceCalendar calendar,
                AllocPolicyWithFailure policy) throws Exception
    {
        super(name, baud_rate);
        resource_ = resource;
        resCalendar_ = calendar;

        // the order between policy and init() is important
        policy_ = (AllocPolicy) policy;
        init();
    }

    ////////////////////////////////////////////

    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use
     * {@link #GridResourceWithFailure(String, Link, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicyWithFailure)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResourceWithFailure to another Entity or Router.
     * @param seed       the initial seed
     * @param resource   an object of ResourceCharacteristics
     * @param peakLoad   the load during peak times
     * @param offPeakLoad   the load during off peak times
     * @param relativeHolidayLoad   the load during holiday times
     * @param weekends   a linked-list contains the weekend days
     * @param holidays   a linked-list contains the public holidays
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @post $none
     */
    public GridResourceWithFailure(String name, Link link, long seed,
                ResourceCharacteristics resource, double peakLoad,
                double offPeakLoad, double relativeHolidayLoad,
                LinkedList weekends, LinkedList holidays) throws Exception
    {
        super(name, link);
        resource_ = resource;

        resCalendar_ = new ResourceCalendar(resource_.getResourceTimeZone(),
                peakLoad, offPeakLoad, relativeHolidayLoad, weekends,
                holidays, seed);

        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use
     * {@link #GridResourceWithFailure(String, Link, ResourceCharacteristics,
     *                      ResourceCalendar, AllocPolicyWithFailure)}
     * and then overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResourceWithFailure to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @pre calendar != null
     * @post $none
     */
    public GridResourceWithFailure(String name, Link link,
                ResourceCharacteristics resource, ResourceCalendar calendar)
                throws Exception
    {
        super(name, link);
        resource_ = resource;

        resCalendar_ = calendar;
        policy_ = null;
        init();
    }

    /**
     * Allocates a new GridResourceWithFailure object. When making a different
     * type of GridResourceWithFailure object, use this constructor and then
     * overrides {@link #processOtherEvent(Sim_event)}.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   GridResourceWithFailure to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResourceWithFailure must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see gridsim.resFailure.AllocPolicyWithFailure
     * @pre name != null
     * @pre link != null
     * @pre resource != null
     * @pre calendar != null
     * @pre policy != null
     * @post $none
     */
    public GridResourceWithFailure(String name, Link link,
            ResourceCharacteristics resource, ResourceCalendar calendar,
            AllocPolicyWithFailure policy) throws Exception
    {
        super(name,link);
        resource_ = resource;
        resCalendar_ = calendar;

        // the order between policy and init() is important
        policy_ = (AllocPolicy) policy;
        init();
    }

    /////////////////////////////////////////////////

    /**
     * Sets a regional GridInformationService (GIS) entity for this resource
     * to communicate with. This resource will then register its ID to the
     * regional GIS entity, rather than {@link GridInformationService} or
     * <tt>system GIS</tt>.
     * @param regionalGIS   name of regional GIS entity
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre regionalGIS != null
     * @post $none
     */
    public boolean setRegionalGIS(String regionalGIS)
    {
        if (regionalGIS == null || GridSim.getEntityId(regionalGIS) == -1) {
            return false;
        }

        regionalGISName_ = regionalGIS;
        return true;
    }

    /**
     * Sets a regional GridInformationService (GIS) entity for this resource
     * to communicate with. This resource will then register its ID to the
     * regional GIS entity, rather than {@link GridInformationService} or
     * <tt>system GIS</tt>.
     * @param gis   regional GIS entity object
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre gis != null
     * @post $none
     */
    public boolean setRegionalGIS(AbstractGIS gis)
    {
        if (gis == null) {
            return false;
        }

        return setRegionalGIS( gis.get_name() );
    }

    /**
     * Asks this resource to record its activities. <br>
     * NOTE: this method should be called <b>BEFORE</b> the simulation starts.
     * If an existing file exists, the new activities will be appended at the
     * end. The file name is this entity name.
     *
     * @param trace     <tt>true</tt> if you want to record this resource's
     *                  activities, <tt>false</tt> otherwise
     */
    public void setTrace(boolean trace) {
        record_ = trace;
        initializeReportFile();
    }

    /**
     * Handles external events that are coming to this GridResourceWithFailure
     * entity. This method also registers the identity of this
     * GridResourceWithFailure entity to <tt>GridInformationService</tt> class.
     * <p>
     * The services or tags available for this resource are:
     * <ul>
     *      <li> {@link gridsim.GridSimTags#RESOURCE_CHARACTERISTICS} </li>
     *      <li> {@link gridsim.GridSimTags#RESOURCE_DYNAMICS} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_SUBMIT} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_CANCEL} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_PAUSE} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_RESUME} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_MOVE} </li>
     *      <li> {@link gridsim.GridSimTags#GRIDLET_STATUS} </li>
     * </ul>
     * <br>
     * This method also calls these methods in the following order:
     * <ol>
     *      <li> {@link #registerOtherEntity()} method
     *      <li> {@link #processOtherEvent(Sim_event)} method
     * </ol>
     *
     * @pre $none
     * @post $none
     */
    public void body()
    {
        // send the registration to GIS
        int register = 0;
        if (policyType_ == ResourceCharacteristics.ADVANCE_RESERVATION) {
            register = GridSimTags.REGISTER_RESOURCE_AR;
        }
        else {
            register = GridSimTags.REGISTER_RESOURCE;
        }

        // this resource should register to regional GIS.
        // However, if not specified, then register to system GIS (the
        // default GridInformationService) entity.
        int gisID = GridSim.getEntityId(regionalGISName_);
        if (gisID == -1) {
            gisID = GridSim.getGridInfoServiceEntityId();
        }
        // need to wait for few seconds before registering to a regional GIS.
        // This is because to allow all routers to fill in their routing tables
        else
        {
            super.sim_pause(GridSim.PAUSE);
            System.out.println(super.get_name() + ".body(): wait for " +
                GridSim.PAUSE + " seconds before registering to " +
                regionalGISName_);
        }

        // send the registration to GIS
        super.send(super.output, GridSimTags.SCHEDULE_NOW, register,
                new IO_data(new Integer(super.get_id()), SIZE, gisID) );

        // Below method is for a child class to override
        registerOtherEntity();

        // Process events until END_OF_SIMULATION is received from the
        // GridSimShutdown Entity
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION)
            {
                System.out.println(get_name()+ ": end of simulation...");
                policy_.setEndSimulation();
                break;
            }

            // process the received event
            processEvent(ev);
        }

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();
    }


    //////////////////// PROTECTED METHODS ///////////////////////////////////

    /**
     * Overrides this method when making a new and different type of resource.
     * This method is called by {@link #body()} for incoming unknown tags.
     * <p>
     * Another approach is to override the
     * {@link gridsim.AllocPolicy#processOtherEvent(Sim_event)} method.
     * This approach is desirable if you do not want to create a new type of
     * grid resource.
     *
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev)
    {
        if (ev == null)
        {
            System.out.println(super.get_name() + ".processOtherEvent(): " +
                    "Error - an event is null.");
            return;
        }

        /****   // NOTE: now a resource passes a new event to the scheduler
        System.out.println(super.get_name()+".processOtherEvent(): Unable to " +
                "handle request from GridSimTags with tag number " +
                ev.get_tag() );
        *****/

        policy_.processOtherEvent(ev);
    }

    /**
     * Overrides this method when making a new and different type of resource.
     * This method is called by {@link #body()} to register other type to
     * {@link gridsim.GridInformationService} entity. In doing so, you
     * need to create a new child class extending from
     * {@link gridsim.GridInformationService}.
     * <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if
     * you use this method.
     *
     * @pre $none
     * @post $none
     * @see gridsim.GridInformationService
     */
    protected void registerOtherEntity() {
        // empty. This should be override by a child class
    }

    //////////////////// PRIVATE METHODS ///////////////////////////////////

    /**
     * Initializes the resource allocation policy
     * @throws Exception    If number of PEs is zero
     * @pre $none
     * @post $none
     */
    private void init() throws Exception
    {
        // If this resource doesn't have any PEs then no useful at all
        if (resource_.getNumPE() == 0)
        {
            throw new Exception(super.get_name() + " : Error - this entity " +
                    "has no PEs. Therefore, can't process any Gridlets.");
        }

        // stores id of this class
        resource_.setResourceID( super.get_id() );

        // if internal allocation policy is used
        policyType_ = resource_.getResourceAllocationPolicy();
        if (policy_ == null)
        {
            switch (policyType_)
            {
                case ResourceCharacteristics.TIME_SHARED:
                    policy_ = new TimeSharedWithFailure(super.get_name(), "TimeShared");
                    break;

                case ResourceCharacteristics.SPACE_SHARED:
                    policy_ = new SpaceSharedWithFailure(super.get_name(), "SpaceShared");
                    break;

                default:
                    throw new Exception(super.get_name()+" : Error - supports"+
                            " only TimeShared or SpaceShared policy.");
            }
        }

        policy_.init(resource_, resCalendar_, super.output);
        record_ = false;
    }

    /**
     * Processes events or services that are available for this
     * GridResourceWithFailure
     * @param ev    a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processEvent(Sim_event ev)
    {
        int src_id = -1;

        // GRIDRESOURCE_RECOVERY and GRIDRESOURCE_FAILURE_INFO (polling request)
        // are ALWAYS processed.
        if (ev.get_tag() == GridSimTags.GRIDRESOURCE_RECOVERY)
        {
            processRecovery(ev);
        }
        else if (ev.get_tag() == GridSimTags.GRIDRESOURCE_FAILURE_INFO)
        {
            processPolling(ev);
        }

        // Only if the resource is not failed, then process other events
        if (!getResourceFailed())
        {
            switch (ev.get_tag())
            {

                // Resource characteristics inquiry
                case GridSimTags.RESOURCE_CHARACTERISTICS:
                    src_id = ((Integer) ev.get_data()).intValue();
                    super.send(super.output, 0.0, ev.get_tag(),
                       new IO_data(resource_, resource_.getByteSize(), src_id));

                    break;

                    // Resource dynamic info inquiry
                case GridSimTags.RESOURCE_DYNAMICS:
                    src_id = ((Integer) ev.get_data()).intValue();
                    super.send(super.output, 0.0, ev.get_tag(),
                               new IO_data(policy_.getTotalLoad(),
                                           Accumulator.getByteSize(), src_id));
                    break;

                case GridSimTags.RESOURCE_NUM_PE:
                    src_id = ((Integer) ev.get_data()).intValue();
                    int numPE = resource_.getNumPE();
                    super.send(super.output, 0.0, ev.get_tag(),
                               new IO_data(new Integer(numPE), SIZE, src_id));
                    break;

                case GridSimTags.RESOURCE_NUM_FREE_PE:
                    src_id = ((Integer) ev.get_data()).intValue();
                    int numFreePE = resource_.getNumFreePE();
                    super.send(super.output, 0.0, ev.get_tag(),
                        new IO_data(new Integer(numFreePE), SIZE, src_id));
                    break;

                    // New Gridlet arrives
                case GridSimTags.GRIDLET_SUBMIT:
                    processGridletSubmit(ev, false);
                    break;

                    // New Gridlet arrives, but the sender asks for an ack
                case GridSimTags.GRIDLET_SUBMIT_ACK:
                    processGridletSubmit(ev, true);
                    break;

                    // Cancels a previously submitted Gridlet
                case GridSimTags.GRIDLET_CANCEL:
                    processGridlet(ev, GridSimTags.GRIDLET_CANCEL);
                    break;

                    // Pauses a previously submitted Gridlet
                case GridSimTags.GRIDLET_PAUSE:
                    processGridlet(ev, GridSimTags.GRIDLET_PAUSE);
                    break;

                    // Pauses a previously submitted Gridlet, but the sender
                    // asks for an acknowledgement
                case GridSimTags.GRIDLET_PAUSE_ACK:
                    processGridlet(ev, GridSimTags.GRIDLET_PAUSE_ACK);
                    break;

                    // Resumes a previously submitted Gridlet
                case GridSimTags.GRIDLET_RESUME:
                    processGridlet(ev, GridSimTags.GRIDLET_RESUME);
                    break;

                    // Resumes a previously submitted Gridlet, but the sender
                    // asks for an acknowledgement
                case GridSimTags.GRIDLET_RESUME_ACK:
                    processGridlet(ev, GridSimTags.GRIDLET_RESUME_ACK);
                    break;

                    // Moves a previously submitted Gridlet to a different res
                case GridSimTags.GRIDLET_MOVE:
                    processGridletMove(ev, GridSimTags.GRIDLET_MOVE);
                    break;

                    // Moves a previously submitted Gridlet to a different res
                case GridSimTags.GRIDLET_MOVE_ACK:
                    processGridletMove(ev, GridSimTags.GRIDLET_MOVE_ACK);
                    break;

                    // Checks the status of a Gridlet
                case GridSimTags.GRIDLET_STATUS:
                    processGridletStatus(ev);
                    break;

                    // Ping packet
                case GridSimTags.INFOPKT_SUBMIT:
                    processPingRequest(ev);
                    break;

                case GridSimTags.GRIDRESOURCE_FAILURE:
                    processFailure(ev);
                    break;

                case GridSimTags.GRIDRESOURCE_RECOVERY:
                    // Do nothing, as we have done it in the "if" statement
                    break;

                    // Get the total number of machines of this resource
                case GridSimTags.RESOURCE_NUM_MACHINES:
                    src_id = ((Integer) ev.get_data()).intValue();
                    int numMachines = resource_.getNumMachines();
                    super.send(super.output, 0.0, ev.get_tag(),
                        new IO_data(new Integer(numMachines), SIZE, src_id));
                    break;

                case GridSimTags.GRIDRESOURCE_FAILURE_INFO:
                    // Do nothing, as we have done it in the "if" statement
                    break;

                    // other unknown tags are processed by this method
                default:
                    processOtherEvent(ev);
                    break;
            }
        }// if (getResourceFailed() == false)
        else
        {
            // If we receive an event while the resource is out of order,
            // we have to send the event back.
            // Otherwise the sender of that event would get stuck.

            Object obj = ev.get_data();
            if (obj instanceof Gridlet)
            {
                Gridlet gl = (Gridlet) ev.get_data();
                try
                {
                    gl.setGridletStatus(Gridlet.FAILED_RESOURCE_UNAVAILABLE);
                    gl.setResourceParameter(super.get_id(), resource_.getCostPerSec());

                }catch(Exception e) {}

                /***********/
                if (record_) {
                   System.out.println(super.get_name() +
                   ": receives an event from " + GridSim.getEntityName(gl.getUserID()) +
                   " while resource is failed, so not processed. Event tag: " +
                   ev.get_tag() + ". Returning GRIDLET_RETURN to sender.");
                }
                /***********/

                super.send(super.output, 0, GridSimTags.GRIDLET_RETURN,
                    new IO_data(gl,gl.getGridletOutputSize(),gl.getUserID()) );

            }// if (obj instanceof Gridlet)
            else if (ev.get_tag() == GridSimTags.RESOURCE_CHARACTERISTICS)
            {
                // Resource characteristics inquiry

                // We do this because the RegionalGISWithFailure entity needs it
                src_id = ((Integer) ev.get_data()).intValue();

                super.send(super.output, 0.0, ev.get_tag(),
                    new IO_data(resource_, resource_.getByteSize(), src_id));
            }
        }
    }

    /**
     * Process the event for an User who wants to know the status of a Gridlet.
     * This GridResourceWithFailure will then send the status back to the User.
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processGridletStatus(Sim_event ev)
    {
        int gridletId = 0;
        int userId = 0;
        int status = -1;

        try
        {
            // if a sender using gridletXXX() methods
            int data[] = (int[]) ev.get_data();
            gridletId = data[0];
            userId = data[1];

            status = policy_.gridletStatus(gridletId, userId);
        }

        // if a sender using normal send() methods
        catch (ClassCastException c)
        {
            try
            {
                Gridlet gl = (Gridlet) ev.get_data();
                gridletId = gl.getGridletID();
                userId = gl.getUserID();

                status = policy_.gridletStatus(gridletId, userId);
            }
            catch (Exception e)
            {
                System.out.println(super.get_name() +
                        ": Error in processing GridSimTags.GRIDLET_STATUS");
                System.out.println( e.getMessage() );
                return;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                    ": Error in processing GridSimTags.GRIDLET_STATUS");
            System.out.println( e.getMessage() );
            return;
        }

        int[] array = new int[2];
        array[0] = gridletId;
        array[1] = status;

        int tag = GridSimTags.GRIDLET_STATUS;
        super.send( super.output, GridSimTags.SCHEDULE_NOW, tag,
                    new IO_data(array, SIZE, userId) );
    }

    /**
     * Processes a Gridlet based on the event type
     * @param ev   a Sim_event object
     * @param type event type
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    private void processGridlet(Sim_event ev, int type)
    {
        int gridletId = 0;
        int userId = 0;

        try
        {
            // if a sender using gridletXXX() methods
            int data[] = (int[]) ev.get_data();
            gridletId = data[0];
            userId = data[1];
        }

        // if a sender using normal send() methods
        catch (ClassCastException c)
        {
            try
            {
                Gridlet gl = (Gridlet) ev.get_data();
                gridletId = gl.getGridletID();
                userId = gl.getUserID();
            }
            catch (Exception e)
            {
                System.out.println(super.get_name() +
                        ": Error in processing Gridlet");
                System.out.println( e.getMessage() );
                return;
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                    ": Error in processing a Gridlet.");
            System.out.println( e.getMessage() );
            return;
        }

        // begins executing ....
        switch (type)
        {
            case GridSimTags.GRIDLET_CANCEL:
                policy_.gridletCancel(gridletId, userId);
                break;

            case GridSimTags.GRIDLET_PAUSE:
                policy_.gridletPause(gridletId, userId, false);
                break;

            case GridSimTags.GRIDLET_PAUSE_ACK:
                policy_.gridletPause(gridletId, userId, true);
                break;

            case GridSimTags.GRIDLET_RESUME:
                policy_.gridletResume(gridletId, userId, false);
                break;

            case GridSimTags.GRIDLET_RESUME_ACK:
                policy_.gridletResume(gridletId, userId, true);
                break;

            default:
                break;
        }

    }

    /**
     * Process the event for an User who wants to know the move of a Gridlet.
     * @param ev   a Sim_event object
     * @param type  event tag
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    private void processGridletMove(Sim_event ev, int type)
    {
        boolean ack = false;
        if (type == GridSimTags.GRIDLET_MOVE_ACK) {
            ack = true;
        }

        try
        {
            // if a sender using gridletMove() methods
            int data[] = (int[]) ev.get_data();
            int gridletId = data[0];
            int userId = data[1];
            int destId = data[2];

            policy_.gridletMove(gridletId, userId, destId, ack);
        }
        catch (Exception e)
        {
            System.out.println(super.get_name()+": Error in moving a Gridlet.");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Processes a Gridlet submission
     * @param ev  a Sim_event object
     * @param ack  an acknowledgement
     * @pre ev != null
     * @post $none
     */
    private void processGridletSubmit(Sim_event ev, boolean ack)
    {
        try
        {
            // gets the Gridlet object
            Gridlet gl = (Gridlet) ev.get_data();

            // checks whether this Gridlet has finished or not
            if (gl.isFinished())
            {
                String name = GridSim.getEntityName( gl.getUserID() );
                System.out.println(super.get_name() + ": Warning - Gridlet #" +
                        gl.getGridletID() + " owned by " + name +
                        " is already completed/finished.");
                System.out.println("Therefore, it is not being executed again");
                System.out.println();

                // NOTE: If a Gridlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause GridSim to be hanged since waiting
                // for this Gridlet back.
                if (ack)
                {
                    int[] array = new int[2];
                    array[0] = gl.getGridletID();
                    array[1] = GridSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = GridSimTags.GRIDLET_SUBMIT_ACK;
                    super.send(super.output, GridSimTags.SCHEDULE_NOW, tag,
                            new IO_data(array, SIZE, gl.getUserID()) );
                }

                super.send(super.output, 0, GridSimTags.GRIDLET_RETURN,
                    new IO_data(gl,gl.getGridletOutputSize(),gl.getUserID()) );

                return;
            }

            // process this Gridlet to this GridResourceWithFailure
            gl.setResourceParameter(super.get_id(), resource_.getCostPerSec());
            policy_.gridletSubmit(gl, ack);
        }
        catch (ClassCastException c)
        {
            System.out.println(super.get_name() + ".processGridletSubmit(): " +
                    "ClassCastException error.");
            System.out.println( c.getMessage() );
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() + ".processGridletSubmit(): " +
                    "Exception error.");
            System.out.println( e.getMessage() );
        }
    }

    /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPingRequest(Sim_event ev)
    {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID(pkt.getSrcID());

        // sends back to the sender
        super.send(super.output, 0.0, GridSimTags.INFOPKT_RETURN,
                   new IO_data(pkt, pkt.getSize(), pkt.getSrcID()));
    }

    ////////////////////////////////////////////

    /**
     * Processes a polling request.
     * @param ev  a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processPolling(Sim_event ev)
    {
        // The resource WILL ALWAYS give a response to the polling requests,
        // even when all the machines of the resource are out of order.
        // When all the machines are out of order,
        // the resource will wait for a period of time before answering
        // the polling request.

        AvailabilityInfo resAv = (AvailabilityInfo) ev.get_data();

        /*******
        // Uncomment this to get more info on the progress of sims
        // NOTE: this keeps printing at every n seconds interval.
        if (record_ == true) {
            System.out.println(super.get_name() +
                ": receives a poll request event from "+
                GridSim.getEntityName(resAv.getSrcID()) +". Clock: " +
                GridSim.clock() +". Is res failed?: "+ getResourceFailed());
        }
        *******/

        // if all the machines of the resource are out of order or not
        if (getResourceFailed())
        {
            resAv.setAvailability(false);
            super.sim_pause(GridSimTags.POLLING_TIME_USER);
        }
        else
        {
            resAv.setAvailability(true);
        }

        // sends back to the sender
        super.send(super.output, 0.0, ev.get_tag(),
                   new IO_data(resAv, Link.DEFAULT_MTU, resAv.getSrcID()));
    }


    /**
     * This method simulates the failure of a resource.
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processFailure(Sim_event ev)
    {
        if (ev == null)
        {
            System.out.println(super.get_name() + ".processFailure(): " +
                    "Error - an event is null.");
            return;
        }

        Object obj = ev.get_data();
        if (obj instanceof FailureMsg)
        {
            FailureMsg resFail = (FailureMsg) obj;

            int numFailedMachines = (int)resFail.getNumMachines();
            int numMachines = resource_.getNumMachines();

            // First, we have to set ome of the machines of the resource
            // as out of order.
            // Then, we have to set the gridlets to FAILED or
            // FAILED_RESOURCE_UNAVAILABLE
            // depending on whether there are still available machines
            // in this resource
            if (numFailedMachines >= numMachines)
            {
                // This resource will not work, as none of its machines
                // are running
                double time = resFail.getTime();

                /*****************/
                // Now, we keep the moment of the failure in a results file
                // for this res
                if (record_ == true) {
                    write("Failure", numMachines);

                    System.out.println(super.get_name() + ".processFailure(): " +
                       "receives an event GRIDRESOURCE_FAILURE. Clock: " +
                       GridSim.clock() + " which will last until clock: " +
                       (GridSim.clock() + time) +
                       ". There are NO working machines in this resource.");
                }
                /******************/

                // set all the machines as failed
                setMachineFailed(true, numMachines);
                emptyGridletLists();

                // as all the machines in this resource have failed, all
                // gridlets are lost (the ones running and the ones waiting)

            }//if (numFailedMachines >= numMachines)
            else
            {
                // This resource will still work, as some of its machines
                // are running

                /*****************/
                // Now, we keep the moment of the failure in a results file
                // for this res
                if (record_) {
                    write("Failure", numFailedMachines);

                    System.out.println(super.get_name() + ".processFailure(): " +
                       "receives an event GRIDRESOURCE_FAILURE. Clock: " +
                       GridSim.clock() +
                       ". There are STILL working machines in this resource.");
                }
                /******************/

                // set numFailedMachines machines as failed
                setMachineFailed(true, numFailedMachines);

                Machine mach;
                int machID;
                // empty only the exec list of the machines which has failed
                for(int i=0; i< numFailedMachines; i++)
                {
                    mach = resource_.getMachineList().getMachineInPos(i);
                    machID = mach.getMachineID();
                    emptyGridletLists(machID);
                }
            }
        }
    }

    /**
     * This method simulates the recovery of a failed machine (or machines).
     * All the machines in this resource are working after calling this method.
     * So, we don't allow overlapping failures.
     * @param ev   a Sim_event object
     * @pre ev != null
     * @post $none
     */
    private void processRecovery(Sim_event ev)
    {
        int mach = resource_.getNumMachines();
        int failedMach = resource_.getNumFailedMachines();

        setMachineFailed(false, resource_.getNumFailedMachines());

        // If all the machines of this resource were out of order,
        // then we have to register again this resource to the GIS
        // Only the regGIS, but not the systemGIS. This is because the
        // systemGIS will always have all the res in its list. Resources are
        // not removed from the systemGIS when they fail.
        if (mach == failedMach)
        {
            int gisID = GridSim.getEntityId(regionalGISName_);

            // send the registration to regGIS
            super.send(super.output, GridSimTags.SCHEDULE_NOW,
                       GridSimTags.REGISTER_RESOURCE,
                       new IO_data(new Integer(super.get_id()), SIZE, gisID));


            System.out.println(super.get_name() + ": Resource recovered." +
                       " Registering the resource at the regional GIS AGAIN" +
                       " after the failure at clock: " + GridSim.clock());

        }
        else {
            System.out.println(super.get_name() +
                ": Resource recovered at clock: " + GridSim.clock());
        }

        if (record_) {
            write("Recovery", 0); // Write in the results file
        }
    }

    /**
     * This method empties the lists containing gridlets in execution
     * and waiting for execution.
     */
    private void emptyGridletLists()
    {
        if (policy_ instanceof AllocPolicyWithFailure)
            ((AllocPolicyWithFailure) policy_).setGridletsFailed();

    }

    /**
     * This method empties the lists containing gridlets in execution,
     * we only erase the gridlets which are running in the failed machine
     * @param machID the id of the machine which has failed.
     * @pre ev != null
     * @post $none
     */
    private void emptyGridletLists(int machID)
    {

        if (policy_ instanceof AllocPolicyWithFailure)
            ((AllocPolicyWithFailure) policy_).setGridletsFailed(machID);

    }

    /**
     * Checks whether all machines in this resource are failed or not.
     * @return <tt>true</tt> if all the machines of the resource
     *         are out of order at this moment.
     */
    private boolean getResourceFailed()
    {
        int numMach = resource_.getMachineList().size();
        Machine mach;
        boolean resFailed = true;

        for(int i=0; i< numMach; i++)
        {
            mach = resource_.getMachineList().getMachineInPos(i);

            if (!mach.getFailed())
                resFailed = false;
        }

        return  resFailed;
    }

    /**
     * Set the status of a machine of this resource.
     * @param b     the new status of the machine.
     *              <tt>true</tt> means the machine is failed,
     *              <tt>false</tt> means the machine is working properly.
     * @param numFailedMachines     the number of failed machines
     *
     */
    private void setMachineFailed(boolean b, int numFailedMachines)
    {
        Machine mach;

        /************/
        String status = null;
        if (record_ == true) {
            if (b)
                status = "FAILED";
            else
                status = "WORKING";
        }
        /************/

        for(int i = 0; i < numFailedMachines; i++)
        {
            mach = resource_.getMachineList().getMachineInPos(i);
            if (mach != null)
            {
                if (record_ == true) {
                    System.out.println(super.get_name() +" - Machine: "
                        + i + " is set to " + status);
                    mach.setFailed(super.get_name(), b);
                }
                else {
                    mach.setFailed(b);
                }
            } // end if
        } // end for
    }

   /**
    * Initializes the results files (put headers over each column)
    */
   private void initializeReportFile()
   {
        if (record_ == false) {
            return;
        }

        // Initialize the results file
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(super.get_name(), true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }

        try {
            fwriter.write("Event \t NumMachines \t Clock\n");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }

        try {
            fwriter.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " +
                super.get_name() + " or " + super.get_name() + "_Fin");
        }
   }

   /**
    * Write an event of this entity into a file.
    * If an existing file exists, the new event will be appended at the end.
    * The file name is this entity name.
    *
    * @param event     Values: "Removing" or "Registering" a resource
    * @param numMachines    number of failed machines
    */
   protected void write(String event, int numMachines)
   {
        if (!record_) {
            return;
        }

        // Write into a results file
        // Now, we keep the moment of the failure in a results file for this res
        FileWriter fwriter = null;
        try
        {
           fwriter = new FileWriter(this.get_name(), true);
        }
        catch (Exception ex)
        {
           ex.printStackTrace();
           System.out.println("Unwanted errors while opening file " +
                              this.get_name());
        }

        try
        {
           fwriter.write(event+ " \t"+ numMachines+ " \t"+GridSim.clock()+"\n");
        }
        catch (Exception ex)
        {
           ex.printStackTrace();
           System.out.println("Unwanted errors while writing on file " +
                              this.get_name());
        }

        try
        {
           fwriter.close();
        }
        catch (Exception ex)
        {
           ex.printStackTrace();
           System.out.println("Unwanted errors while closing file " +
                              this.get_name());
        }
   }

} 
