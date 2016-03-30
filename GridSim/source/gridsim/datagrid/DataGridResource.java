/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */

package gridsim.datagrid;

import java.util.List;

import eduni.simjava.Sim_event;

import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.GridSim;
import gridsim.ParameterException;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.datagrid.index.AbstractRC;
import gridsim.datagrid.index.RegionalRC;
import gridsim.datagrid.storage.Storage;
import gridsim.net.Link;


/**
 * A resource for Data Grids enables users to run their jobs as well as to
 * gain access to available data sets.
 * A Data Grid resource has the following components:
 * <ul>
 * <li><b>Storage:</b> A resource can have one or more different storage
 * elements, such as harddisks or tape drives. An individual storage is
 * responsible for storing, retrieving and deleting files.
 * <br><br>
 * <li><b>Replica Manager:</b> This component is responsible for handling
 * and managing incoming requests about data sets in a resource for one or
 * more storage elements. It also performs registrations of files stored in
 * the resource to a designated RC.
 * <br><br>
 * <li><b>Local Replica Catalogue (RC):</b> This component is an optional part
 * of a resource. The Local RC is responsible for indexing available files on
 * the resource only. It also handles users' queries. However, the Local RC
 * does not serve as a catalogue server to other resources.
 * This should be done by a leaf or regional RC.
 * <br><br>
 * <li><b>Allocation Policy:</b> This component is responsible for executing
 * user jobs to one or more available nodes in a resource.
 * </ul>
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 * @see gridsim.datagrid.storage.Storage
 * @see gridsim.datagrid.ReplicaManager
 * @see gridsim.datagrid.index.AbstractRC
 * @see gridsim.AllocPolicy
 */
public class DataGridResource extends GridResource
{
    private ReplicaManager replicaManager_;
    private String rcName_;     // a replica catalogue entity name
    private int rcID_;          // a replica catalogue entity ID
    private int tierLevel_;     // tier level of this resource
    private RegionalRC localRC_;     // local RC entity


    /**
     * Creates a new DataGrid resource object
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   resource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param replicaManager    a Replica Manager that is responsible for this
     *                          resource
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> the given Replica Manager object is <tt>null</tt>
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A resource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    public DataGridResource(String name, Link link,
            ResourceCharacteristics resource, ResourceCalendar calendar,
            ReplicaManager replicaManager)
            throws Exception
    {
        super(name, link, resource, calendar);
        if (replicaManager == null)
        {
            throw new ParameterException(name +
                ": Error - the Replica Manager entity is null");
        }

        init(replicaManager);
    }

    /**
     * Creates a new DataGrid resource object
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param link       the link that will be used to connect this
     *                   resource to another Entity or Router.
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource.
     * @param replicaManager    a Replica Manager that is responsible for this
     *                          resource
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> the given Replica Manager object is <tt>null</tt>
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A resource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    public DataGridResource(String name, Link link,
            ResourceCharacteristics resource, ResourceCalendar calendar,
            AllocPolicy policy, ReplicaManager replicaManager)
            throws Exception
    {
        super(name, link, resource, calendar, policy);
        if (replicaManager == null)
        {
            throw new ParameterException(name +
                ": Error - the Replica Manager entity is null");
        }

        init(replicaManager);
    }

    /**
     * Initializes all local attributes
     * @param replicaManager    a Replica Manager object
     */
    private void init(ReplicaManager replicaManager)
    {
        rcID_ = -1;
        tierLevel_ = 0;
        localRC_ = null;
        replicaManager_ = replicaManager;
        replicaManager_.init( super.output, policy_, super.get_id() );
    }

    /**
     * Adds one or more Storage elements into the resource.
     * @param storageList   a list of Storage elements
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @see gridsim.datagrid.storage.Storage
     */
    public boolean addStorage(List storageList)
    {
        if (storageList == null) {
            return false;
        }

        return replicaManager_.addStorage(storageList);
    }

    /**
     * Adds a Storage element
     * @param storage   a Storage element
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @see gridsim.datagrid.storage.Storage
     */
    public boolean addStorage(Storage storage)
    {
        if (storage == null) {
            return false;
        }

        return replicaManager_.addStorage(storage);
    }

    /**
     * Gets the total capacity of all Storage elements (in MByte)
     * @return  the total capacity in MB
     */
    public double getTotalStorageCapacity() {
        return replicaManager_.getTotalStorageCapacity();
    }

    /**
     * Adds a file into the resource's storage before the experiment starts.
     * If the file is a master file, then it will be registered to the RC when
     * the experiment begins.
     * @param file  a DataGrid file
     * @return a tag number denoting whether this operation is a success or not
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_ERROR_EMPTY
     */
    public int addFile(File file)
    {
        if (file == null) {
            return DataGridTags.FILE_ADD_ERROR_EMPTY;
        }

        return replicaManager_.addFile(file);
    }

    /**
     * Sets the RC name that is located outside this resource.
     * This method should be used if a local RC exists.
     * @param rcName    a RC entity name
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setHigherReplicaCatalogue(String rcName)
    {
        // if a local RC already exists, then exit
        if (localRC_ == null)
        {
            System.out.println(super.get_name() +
                ".setHigherReplicaCatalogue(): Warning - a local " +
                "Replica Catalogue entity doesn't exist.");

            return false;
        }

        if (rcName == null || GridSim.getEntityId(rcName) == -1) {
            return false;
        }

        localRC_.setHigherLevelRCid( GridSim.getEntityId(rcName) );
        return true;
    }

    /**
     * Sets the RC name for this resource.
     * This method should be used if a local RC DOES NOT exist.
     * @param rcName    a RC entity name
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(String rcName)
    {
        // if a local RC already exists, then exit
        if (localRC_ != null)
        {
            System.out.println(super.get_name() +
                ".setReplicaCatalogue(): Warning - a local " +
                "Replica Catalogue entity already exists.");

            return false;
        }

        if (rcName == null || GridSim.getEntityId(rcName) == -1) {
            return false;
        }

        rcName_ = rcName;
        rcID_ = GridSim.getEntityId(rcName);
        return replicaManager_.setReplicaCatalogue(rcID_);
    }

    /**
     * Sets the RC entity for this resource.
     * This method should be used if a local RC DOES NOT exist.
     * @param rc    a RC entity
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(AbstractRC rc)
    {
        // if a local RC already exists, then exit
        if (localRC_ != null || rc == null) {
            return false;
        }

        return setReplicaCatalogue( rc.get_name() );
    }

   /**
    * Creates a new local RC, meaning it is located inside this resource.
    * Hence, this local RC is using I/O of the resource.
    * Therefore, to communicate to this local RC, an entity will need to send
    * an event to the resource, then the resource will pass the event to the
    * local RC.
    * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
    */
    public boolean createLocalRC()
    {
        // if a local RC already exists
        if (localRC_ != null)
        {
            System.out.println(super.get_name() + ".setLocalRC(): Warning - " +
                "a local Replica Catalogue entity already exists.");

            return false;
        }

        // if a RC has already assigned/allocated for this resource
        if (rcID_ != -1 || super.output == null)
        {
            System.out.println(super.get_name() + ".setLocalRC(): Warning - " +
                "a regional Replica Catalogue entity is already allocated " +
                "to this resource.");

            return false;
        }

        boolean result = false;
        try
        {
            String name = super.get_name() + "_localRC";
            localRC_ = new RegionalRC(name, super.get_id(), super.output);

            // RM has to be aware that the RC is now on the local site
            replicaManager_.setReplicaCatalogue( super.get_id() );
            rcID_ = super.get_id();
            result = true;
        }
        catch (Exception e) {
            result = false;
        }

        return result;
    }

    /**
     * Checks whether this resource has a local RC entity or not.
     * @return <tt>true</tt> if this resource has a local RC entity,
     *         <tt>false</tt> otherwise
     */
    public boolean hasLocalRC()
    {
        boolean result = false;
        if (localRC_ != null) {
            result = true;
        }

        return result;
    }

    /**
     * Gets the local RC entity of this resource.
     * @return the local replica catalogue if it exists, or
     *         <tt>null</tt> otherwise
     */
    public AbstractRC getLocalRC() {
       return localRC_;
    }

    /**
     * Sets the tier level of this resource (in a hierarchical model)
     * @param tierLevel the tier level of this resource
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setTierLevel(int tierLevel)
    {
        if (tierLevel < 0) {
            return false;
        }

        tierLevel_ = tierLevel;
        return true;
    }

    /**
     * Processes events or services that are available for this resource
     * @param ev    a Sim_event object
     */
    protected void processOtherEvent(Sim_event ev)
    {
        boolean result = false;

        // then process at local RC if previously not available
        if (localRC_ != null) {
            result = localRC_.processEvent(ev);
        }

        // process this event at data manager
        if (!result) {
            result = replicaManager_.processEvent(ev);
        }

        if (!result)
        {
            System.out.println(super.get_name() +
                ".body(): Unable to handle a request from " +
                GridSim.getEntityName( ev.get_src() ) +
                " with event tag = " + ev.get_tag() );
        }
    }

    /**
     * Registers other entities when a simulation starts. In this case,
     * all master files are being registered by RM to the designated RC.
     * @see gridsim.datagrid.ReplicaManager#registerAllMasterFiles()
     */
    protected void registerOtherEntity()
    {
        // if no RC entity has been created before the start of simulation
        // then create a local RC entity by default
        if (rcID_ == -1 && localRC_ == null) {
            createLocalRC();
        }

        // wait for the routers to build their routing tables
        super.sim_process(GridSim.PAUSE);
        replicaManager_.registerAllMasterFiles();
    }

    /**
     * Notifies internal entities regarding to the end of simulation signal
     */
    protected void processEndSimulation() {
        replicaManager_.processEndSimulation();
    }

} 


