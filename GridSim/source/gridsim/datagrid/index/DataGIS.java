/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid.index;

import eduni.simjava.Sim_event;
import gridsim.*;
import gridsim.datagrid.*;
import java.util.LinkedList;


/**
 * A Data GridInformationService (GIS) entity that is responsible only for
 * storing a registration info from a Replica Catalogue (RC) entity.
 * This class is only used by GridSim for notifying the end of a simulation
 * to the registered RC entities.
 * Hence, users and resources communicate to a Data GIS using the
 * {@link gridsim.datagrid.index.DataRegionalGIS} class.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 * @see     gridsim.datagrid.index.DataRegionalGIS
 */
public class DataGIS extends GridInformationService {

    private LinkedList rcList_;     // list of all replica catalog entities

    /**
     * Allocates a new Data GIS
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  communication speed
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     */
    public DataGIS(String name, double baud_rate) throws Exception {
        super(name, baud_rate);
        init();
    }

    /**
     * Allocates a new Data GIS
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     */
    public DataGIS() throws Exception {
        this("DataGIS");
    }

    /**
     * Allocates a new Data GIS with a default baud rate.
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @see gridsim.GridSim#init(int, Calendar, boolean)
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     * @see eduni.simjava.Sim_entity
     */
    public DataGIS(String name) throws Exception {
        super(name, GridSimTags.DEFAULT_BAUD_RATE);
        init();
    }

    /**
     * Initializes all private attributes
     */
    private void init() {
        rcList_ = new LinkedList();
    }

    /**
     * Processes an incoming request for registering a RC entity.
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    protected void processOtherEvent(Sim_event ev) {

        switch (ev.get_tag()) {

            // register all RC entities
            case DataGridTags.REGISTER_REPLICA_CTLG:
                rcList_.add( (Integer) ev.get_data() );
                break;

            default:
                System.out.println(super.get_name() +
                    ".body(): Unable to handle a request from " +
                    GridSim.getEntityName(ev.get_src()) + " with event tag = " +
                    ev.get_tag());
                break;
        }
    }

    /**
     * Notifies the registered entities about the end of simulation.
     */
    protected void processEndSimulation() {

        System.out.println(super.get_name() +
            ": Notify all replica catalog entities for shutting down.");

        super.signalShutdown(rcList_);
        rcList_.clear();
    }

} 

