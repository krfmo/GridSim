/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */

package gridsim.datagrid;

import java.util.Iterator;
import java.util.List;

import eduni.simjava.Sim_entity;
import eduni.simjava.Sim_event;
import eduni.simjava.Sim_port;
import gridsim.AllocPolicy;
import gridsim.GridSim;
import gridsim.GridSimTags;
import gridsim.IO_data;
import gridsim.ParameterException;
import gridsim.datagrid.storage.Storage;

/**
 * This is an abstract class which describes the basic functionality of a
 * Replica Manager in a Data Grid. This class is responsible for all data
 * manipulation on a DataGridResource.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public abstract class ReplicaManager extends Sim_entity
{
    /** The policy of the DataGridResource */
    protected AllocPolicy policy_;

    /**  The output port of the DataGridResource */
    protected Sim_port outputPort_;

    /** ID of the DataGridResource entity */
    protected int resourceID_;

    /** ID of the DataGridResource entity (in Integer object) */
    protected Integer resIdObj_;

    /** ID of the Replica Catalogue entity */
    protected int rcID_;

    /** List of all storage elements */
    protected List storageList_;


    /**
     * Creates a new Replica Manager object
     * @param name          the name to be associated with this entity
     * @param resourceName  the name of the DataGrid resource
     * @throws ParameterException This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> the given name is <tt>null</tt> or empty
     *      </ul>
     * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[],
     *          String)
     */
    protected ReplicaManager(String name, String resourceName)
                             throws ParameterException
    {
        super(name);
        if (resourceName == null) {
            throw new ParameterException("ReplicaManager: Error - invalid name");
        }

        commonInit();
    }

    /** Initializes all attributes */
    private void commonInit()
    {
        resourceID_ = -1;
        resIdObj_ = null;
        rcID_ = -1;
    }

    /**
     * Notifies internal entities regarding to the end of simulation signal.
     * This method should be overridden by the child class.
     */
    public void processEndSimulation() {
        // ....
    }

    /**
     * Initializes the Replica Manager details. This method is called by
     * a DataGrid resource entity.
     * @param output  the output port of a resource which is used to sent
     *                events to other entities
     * @param policy  the resource scheduling policy for executing incoming
     *                Gridlets
     * @param resourceID    the resource ID on which the ResourceManager is
     *                      located
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @see gridsim.datagrid.DataGridResource
     */
    public boolean init(Sim_port output, AllocPolicy policy, int resourceID)
    {
        if (output == null || policy == null || resourceID < 0) {
            return false;
        }
        outputPort_ = output;
        policy_ = policy;
        resourceID_ = resourceID;
        resIdObj_ = new Integer(resourceID);
        return true;
    }

    /**
     * Sets the ReplicaCatalogue for this DataGridResource
     * @param rcName    the name of the ReplicaCatalogue
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(String rcName)
    {
        if (rcName == null || rcName.length() == 0) {
            return false;
        }
        rcID_ = GridSim.getEntityId(rcName);
        return true;
    }

    /**
     * Sets the ID of the ReplicaCatalogue, to which all the requests for adding
     * and deleting files will be sent.
     *
     * @param rcID  the ReplicaCtalogue ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(int rcID) {
        if (rcID < 0) {
            return false;
        }
        rcID_ = rcID;
        return true;
    }

    /**
     * Registers a given file to the designated Replica Catalogue
     * @param file  a file to be registered
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean registerFile(File file)
    {
        if (file == null) {
            return false;
        }
        return registerFile( file.getName() );
    }

    /**
     * Registers a given file to the designated Replica Catalogue
     * @param fileName  a file name to be registered
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean registerFile(String fileName)
    {
        Object[] obj = new Object[2];
        obj[0] = fileName;
        obj[1] = resIdObj_;

        super.sim_schedule(outputPort_, 0, DataGridTags.CTLG_ADD_REPLICA,
                           new IO_data(obj, DataGridTags.PKT_SIZE, rcID_) );
        return true;
    }

    /**
     * Notifies a deleted file to the designated Replica Catalogue (RC)
     * @param fileName  a file name to be de-registered in RC
     * @param tag       a tag to denote the specific instruction to the RC
     * @return an integer number denoting whether this operation is successful
     *         or not
     * @see gridsim.datagrid.DataGridTags#CTLG_DELETE_REPLICA
     * @see gridsim.datagrid.DataGridTags#CTLG_DELETE_MASTER
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_ERROR
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_SUCCESSFUL
     */
    protected int deregisterDeletedFile(String fileName, int tag)
    {
        if (fileName == null || fileName.length() == 0) {
            return DataGridTags.FILE_DELETE_ERROR;
        }

        Object[] obj = new Object[2];
        obj[0] = fileName;
        obj[1] = resIdObj_;

        super.sim_schedule(outputPort_, GridSimTags.SCHEDULE_NOW, tag,
                           new IO_data(obj, DataGridTags.PKT_SIZE, rcID_) );
        return DataGridTags.FILE_DELETE_SUCCESSFUL;
    }

    /**
     * Checks whether the resource has the given file
     * @param file  a file to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(File file)
    {
        if (file == null) {
            return false;
        }
        return contains( file.getName() );
    }

    /**
     * Checks whether the resource has the given file
     * @param fileName  a file name to be searched
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean contains(String fileName)
    {
        if (fileName == null || fileName.length() == 0) {
            return false;
        }

        Iterator it = storageList_.iterator();
        Storage storage = null;
        boolean result = false;

        while (it.hasNext()) {
            storage = (Storage) it.next();
            if (storage.contains(fileName)) {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * Gets the total storage capacity (in MByte) for this DataGrid resource
     * @return total storage capacity (in MB)
     */
    public double getTotalStorageCapacity()
    {
        double capacity = 0;
        Iterator it = storageList_.iterator();
        Storage storage = null;

        while (it.hasNext()) {
            storage = (Storage) it.next();
            capacity += storage.getCapacity();
        }
        return capacity;
    }

    ////////////////////////////////////////////////////////////

    /**
     * Adds a storage element to the DataGrid resource
     * @param storage   the storage element to be added
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public abstract boolean addStorage(Storage storage);

     /**
     * Adds a list of storage elements to the DataGrid resource
     * @param storageList   a list of storage elements to be added
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public abstract boolean addStorage(List storageList);

    /**
     * Adds a file to the local storage. However, the file is not registered
     * to the Replica Catalogue.
     * @param file  a file to be placed on the local resource
     * @return an integer number denoting whether this operation is successful
     *         or not
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_ERROR_STORAGE_FULL
     */
    protected abstract int addFile(File file);

    /**
     * Gets a physical file based on its name
     * @param fileName  the file name to be retrieved
     * @return the physical file or <tt>null</tt> if not found
     */
    protected abstract File getFile(String fileName);

    /**
     * Deletes a file from the local storage, and registers
     * the change to the designated Replica Catalogue.
     * @param fileName  the filename of the file to be deleted.
     * @return an integer number denoting whether this operation is successful
     *         or not
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_ERROR_READ_ONLY
     */
    protected abstract int deleteFile(String fileName);

    /**
     * Processes an incoming event
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public abstract boolean processEvent(Sim_event ev);

    /**
     * Registers all master files that are currently stored in the storage
     * at the beginning of the simulatin
     */
    public abstract void registerAllMasterFiles();

} 

