/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid.index;

import eduni.simjava.*;
import gridsim.*;
import gridsim.datagrid.*;
import gridsim.net.*;

/**
 * An abstract class for the functionality of a Replica Catalogue (RC) entity.
 * The RC entity is a core component of every Data Grid system. The
 * function of a RC is to store the information (metadata) about files and to
 * provide mapping between a filename and its physical location(s).
 * <br>
 * The RC does not have to be a single entity in a Data Grid system.
 * It can also be composed of several distributed components, which, by
 * switching the information among them, provide a transparent service to
 * the users and resources.
 * <br>
 * Currently, GridSim allows two possible catalogue models:
 * <ul>
 * <li> Centralized Model. <br>
 * There is only one RC component in a Data Grid system that handles all
 * registrations and queries sent by resources and users.
 * Hence, the RC maps a filename to a list of resources that stores this file.
 * <br><br>
 * <li> Hierarchical Model. <br>
 * The hierarchical RC model is constructed as a catalogue tree.
 * In this model, some information are stored in the root of the catalogue
 * tree, and the rest in the leaves.
 * </ul>
 * <br>
 * Common functionalities of a RC is encapsulated in this class, an
 * abstract parent class for both {@link gridsim.datagrid.index.TopRegionalRC}
 * and {@link gridsim.datagrid.index.RegionalRC}.
 * The {@link gridsim.datagrid.index.TopRegionalRC} class acts as a
 * centralized RC or a root RC in a hierarchical model. In constrast, the
 * {@link gridsim.datagrid.index.RegionalRC} class represents a local RC
 * and/or a leaf RC in a hierarchical model.
 * Therefore, creating a new RC model can be done by extending this
 * class and implementing the abstract methods.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public abstract class AbstractRC extends GridSimCore {

    /** A flag that denotes whether this entity is located inside a resource
     * or not
     */
    protected boolean localRC_;     // local RC or not

    /** A resource ID that hosts this RC entity (if applicable) */
    protected int resourceID_;      // resource ID that hosts this RC

    private int gisID_;             // GIS entity ID


    /**
     * Creates a new <b>local</b> Replica Catalogue (RC) entity.
     * This constructor should be used if you want this RC entity to be inside
     * a resource's domain, hence known as <i>a Local RC</i>.
     * As a consequence, this entity uses the resource ID and I/O port for
     * identification.
     * <br>
     * The Local RC is responsible for indexing available files on the
     * resource only. It also handles users' queries.
     * However, the Local RC does not serve as a catalogue
     * server to other resources. This should be done by a leaf or regional RC.
     *
     * @param name  this entity name
     * @param resourceID    resource ID that hosts this RC entity
     * @param outputPort    resource's output port
     * @throws Exception    This happens when one of the input parameters is
     *                      invalid.
     */
    protected AbstractRC(String name, int resourceID, Sim_port outputPort)
                         throws Exception
    {
        super(name);
        if (resourceID == -1 || outputPort == null) {
            throw new Exception("AbstractRC(): Error - invalid parameter.");
        }

        resourceID_ = resourceID;
        localRC_ = true;
        super.output = outputPort;
        init();
    }

    /**
     * Creates a new Replica Catalogue (RC) entity.
     * @param name  this entity name
     * @param link  the link that this GridSim entity will use to
     *              communicate with other GridSim or Network entities.
     * @throws Exception    This happens when one of the input parameters is
     *                      invalid.
     */
    protected AbstractRC(String name, Link link) throws Exception
    {
        super(name, link);
        resourceID_ = -1;
        localRC_ = false;
        init();
    }

    /**
     * Sets a regional GIS name for this entity to communicate with
     * @param name  a regional GIS name
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setRegionalGIS(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }

        int id = GridSim.getEntityId(name);
        if (id == -1) {
            return false;
        }

        gisID_ = id;
        return true;
    }

    /**
     * Handles incoming requests to this entity, <b>DO NOT OVERRIDE</b> this
     * method. Implement the {@link #processOtherEvent(Sim_event)} instead.
     */
    public void body() {

        // if it is a local RC, then use its resource entity ID
        int id = -1;
        if (localRC_) {
            id = resourceID_;
        }
        // otherwise, use my entity ID
        else {
            id = super.get_id();
        }

        // register to central/main GIS if no regional GIS exists
        if (gisID_ == -1) {
            gisID_ = GridSim.getGridInfoServiceEntityId();
        }
        // need to wait for few seconds before registering to a regional GIS.
        // This is because to allow all routers to fill in their routing tables
        else
        {
            String name = GridSim.getEntityName(gisID_);
            super.sim_pause(GridSim.PAUSE);
            System.out.println(super.get_name() + ".body(): wait for " +
                GridSim.PAUSE + " seconds before registering to " + name);
        }

        // register to a GIS entity first
        int register = DataGridTags.REGISTER_REPLICA_CTLG;
        super.send(super.output, GridSimTags.SCHEDULE_NOW, register,
                   new IO_data(new Integer(id), DataGridTags.PKT_SIZE, gisID_));

        // Below method is for a child class to override
        registerOtherEntity();

        // Process events until END_OF_SIMULATION is received from the
        // GridSimShutdown Entity
        Sim_event ev = new Sim_event();
        while (Sim_system.running()) {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                processEndSimulation();
                break;
            }

            // process the received event
            processEvent(ev);
        }

        // remove I/O entities created during construction of this entity
        super.terminateIOEntities();
    }

    /**
     * Processes an incoming request that uses a user-defined tag. This method
     * is useful for creating a new RC entity.
     * @param ev  a Sim_event object (or an incoming event or request)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected abstract boolean processOtherEvent(Sim_event ev);

    /**
     * Registers other information to a GIS entity.
     */
    protected abstract void registerOtherEntity();

    /**
     * Performs last activities before the end of a simulation.
     */
    protected abstract void processEndSimulation();

    /**
     * Register a file which is already stored in a resource <b>before</b> the
     * start of simulation
     * @param fAttr     a file attribute object
     * @param id        the owner ID of this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public abstract boolean registerOriginalFile(FileAttribute fAttr, int id);

    /**
     * Initializes any local variables
     */
    private void init() {
        gisID_ = -1;
    }

    /**
     * Processes incoming events one by one
     * @param ev    a Sim_event object
     * @return <tt>true</tt> if successful, <tt>false</tt> if a tag is unknown
     */
    public boolean processEvent(Sim_event ev) {
        boolean result = false;
        switch (ev.get_tag())
        {
            // Ping packet
            case GridSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                result = true;
                break;

            default:
                result = processOtherEvent(ev);
                break;
        }

        return result;
    }

    /**
     * Processes a ping request.
     * @param ev  a Sim_event object
     */
    private void processPingRequest(Sim_event ev) {
        InfoPacket pkt = (InfoPacket) ev.get_data();
        pkt.setTag(GridSimTags.INFOPKT_RETURN);
        pkt.setDestID(pkt.getSrcID());

        // sends back to the sender
        super.send(super.output, GridSimTags.SCHEDULE_NOW,
            GridSimTags.INFOPKT_RETURN,
            new IO_data(pkt, pkt.getSize(), pkt.getSrcID()));
    }

} 

