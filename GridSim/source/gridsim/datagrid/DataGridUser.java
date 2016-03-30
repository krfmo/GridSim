/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
package gridsim.datagrid;

import java.util.ArrayList;
import java.util.List;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_type_p;
import gridsim.GridSim;
import gridsim.GridUser;
import gridsim.IO_data;
import gridsim.datagrid.filter.FileNameFilter;
import gridsim.datagrid.filter.Filter;
import gridsim.datagrid.filter.FilterDataResult;
import gridsim.datagrid.index.AbstractRC;
import gridsim.datagrid.index.TopRegionalRC;
import gridsim.net.Link;


/**
 * A class for representing a user in a Data Grid environment
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class DataGridUser extends GridUser {

    private String rcName_;     // replica catalogue name
    private int rcID_;          // replica catalogue ID
    private Integer myID_;      // this entity ID


    /**
     * Creates a new DataGrid user.<br>
     * NOTE: When using this constructor, do not forget to set
     * the regional GIS name and the Replica Catalogue name for this entity.
     * @param   name    the user name
     * @param   link    a network link to connect this user to a network
     * @throws  Exception       happens if one of the inputs is empty or null
     * @see gridsim.GridUser#setRegionalGIS(String)
     * @see gridsim.GridUser#setRegionalGIS(AbstractGIS)
     * @see gridsim.datagrid.DataGridUser#setReplicaCatalogue(String)
     * @see gridsim.datagrid.DataGridUser#setReplicaCatalogue(AbstractRC)
     */
    public DataGridUser(String name, Link link) throws Exception
    {
        super(name, link);
        init();
    }

    /**
     * Creates a new DataGrid user.<br>
     * NOTE: When using this constructor, do not forget to set
     * the Replica Catalogue name for this entity.
     * @param   name    the user name
     * @param   link    a network link to connect this user to a network
     * @param   regionalGIS     a Regional GIS name
     * @throws  Exception       happens if one of the inputs is empty or null
     * @see gridsim.datagrid.DataGridUser#setReplicaCatalogue(String)
     * @see gridsim.datagrid.DataGridUser#setReplicaCatalogue(AbstractRC)
     */
    public DataGridUser(String name, Link link, String regionalGIS)
                        throws Exception
    {
        super(name, link, regionalGIS);
        init();
    }

    /** Initializes all the variables */
    private void init()
    {
        rcName_ = null;
        rcID_ = -1;
        myID_ = new Integer( super.get_id() );
    }

    /**
     * Creates a new DataGrid user
     * @param   name    the user name
     * @param   link    a network link to connect this user to a network
     * @param   rcName  a Replica Catalogue name
     * @param   regionalGIS     a Regional GIS name
     * @throws  Exception       happens if one of the inputs is empty or null
     */
    public DataGridUser(String name, Link link, String rcName,
                        String regionalGIS) throws Exception
    {
        super(name, link, regionalGIS);
        rcID_ = GridSim.getEntityId(rcName);
        if (rcName == null || rcID_ == -1) {
            throw new Exception(name + ": Error - invalid RC name");
        }

        rcName_ = rcName;
        myID_ = new Integer( super.get_id() );
    }

    /**
     * Sets a Replica Catalogue name for this user
     * (the old name will be overwritten).
     * @param   rcName  a Replica Catalogue name
     * @return  <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(String rcName)
    {
        int id = GridSim.getEntityId(rcName);
        if (rcName == null || id == -1) {
            return false;
        }
        rcName_ = rcName;
        rcID_ = id;
        return true;
    }

    /**
     * Sets a Replica Catalogue name for this user
     * (the old name will be overwritten).
     * @param   rc  a Replica Catalogue object
     * @return  <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setReplicaCatalogue(AbstractRC rc)
    {
        if (rc == null) {
            return false;
        }
        return setReplicaCatalogue( rc.get_name() );
    }

    /**
     * Checks whether a Replica Catalogue entity has been allocated to
     * this user or not
     * @return  <tt>true</tt> if the entity has been set,
     *          <tt>false</tt> otherwise
     */
    private boolean checkRC()
    {
        // if no RC entity exists
        boolean result = true;
        if (rcID_ == -1)
        {
            // use the default RC name
            rcID_ = GridSim.getEntityId(TopRegionalRC.DEFAULT_NAME);
            rcName_ = TopRegionalRC.DEFAULT_NAME;

            // If the default RC entity doesn't exist
            if (rcID_ == -1)
            {
                result = false;
                rcName_ = null;     // change the RC name to null
                System.out.println(super.get_name() +
                    ": Error - no TopRegionalRC entity exists.");
            }
        }

        return result;
    }

    /**
     * Gets a Replica Catalogue name
     * @return  a Replica Catalogue name or <tt>null</tt> if it does not exist
     */
    public String getReplicaCatalogueName()
    {
        checkRC();
        return rcName_;
    }

    /**
     * Gets a Replica Catalogue id
     * @return  a Replica Catalogue id or <tt>-1</tt> if it does not exist
     */
    public int getReplicaCatalogueID()
    {
        checkRC();
        return rcID_;
    }

    /**
     * Gets a list of local Replica Catalogue (RC) IDs from a regional
     * GIS entity
     * @return a list of local RC IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if RCs do not exist.
     */
    public Object[] getLocalRCList() {
        return super.getList(DataGridTags.INQUIRY_LOCAL_RC_LIST);
    }

    /**
     * Gets a list of global Replica Catalogue (RC) IDs.
     * Global RC means a RC that is registered to other
     * regional GIS entities.
     * @return a list of global RC IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if RCs do not exist.
     */
    public Object[] getGlobalRCList() {
        return super.getList(DataGridTags.INQUIRY_GLOBAL_RC_LIST);
    }

    /**
     * Gets the first resource ID that has the given logical file name (lfn).
     * <br>NOTE: The rest of resource IDs are ignored. If you want to know
     *           all the resource IDs, then
     *           use {@link #getReplicaLocationList(String)} method instead.
     * In addition, this method only contacts the given/chosen RC entity, not
     * all RCs.
     * @param lfn   a logical file name
     * @return a resource ID or <tt>-1</tt> if not found
     */
    public int getReplicaLocation(String lfn)
    {
        if (lfn == null) {
            return -1;
        }

        int resourceID = -1;
        int eventTag = DataGridTags.CTLG_GET_REPLICA;   // set tag name

        // consult with the RC first
        int rcID = getReplicaCatalogueID();
        if (rcID == -1) {
            return -1;
        }

        // sends a request to this RC
        sendEvent(eventTag, lfn, rcID);

        // waiting for a response from the RC
        Sim_type_p tag = new Sim_type_p(DataGridTags.CTLG_REPLICA_DELIVERY);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        try
        {
            Object[] data = (Object[]) ev.get_data();   // get the data
            Integer resID = (Integer) data[1];          // get the resource ID
            if (resID != null) {
                resourceID = resID.intValue();
            }
        }
        catch (Exception e)
        {
            resourceID = -1;
            System.out.println(super.get_name()
                    + ".getReplicaLocation(): Exception");
        }

        return resourceID;
    }

    /**
     * Gets a list of resource IDs that store the given logical file name (lfn).
     * <br>NOTE: This method only contacts the given/chosen RC entity, not
     * all RCs.
     * @param lfn   a logical file name
     * @return a list of resource IDs or <tt>null</tt> if not found
     */
    public List getReplicaLocationList(String lfn)
    {
        int rcID = getReplicaCatalogueID();
        return getReplicaLocationList(lfn, rcID);
    }

    /**
     * Gets a list of resource IDs that store the given logical file name (lfn).
     * <br>NOTE: This method only contacts the given/chosen RC entity, not
     * all RCs.
     * @param lfn   a logical file name
     * @param rcID  a RC entity ID
     * @return a list of resource IDs or <tt>null</tt> if not found
     */
    public List getReplicaLocationList(String lfn, int rcID)
    {
        if (lfn == null || rcID == -1) {
            return null;
        }

        // send the event to the RC entity
        int eventTag = DataGridTags.CTLG_GET_REPLICA_LIST;
        sendEvent(eventTag, lfn, rcID);

        // waiting for a response from the RC
        Sim_type_p tag =new Sim_type_p(DataGridTags.CTLG_REPLICA_LIST_DELIVERY);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        List resList = null;      // a list of resource IDs storing lfn
        try
        {
            Object[] data = (Object[]) ev.get_data();  // get the data
            resList = (List) data[1];                  // get the resource list
        }
        catch (Exception e)
        {
            resList = null;
            System.out.println(super.get_name()
                    + ".getReplicaLocationList(): Exception.");
        }

        return resList;
    }

    /**
     * Gets an attribute file for a given logical file name (lfn)
     * @param   lfn     a logical file name
     * @return  a FileAttribute object or <tt>null</tt> if not found
     */
    public FileAttribute getFileAttribute(String lfn)
    {
        // check first
        int rcID = getReplicaCatalogueID();
        if (rcID == -1 || lfn == null) {
            return null;
        }

        int eventTag = DataGridTags.CTLG_GET_FILE_ATTR;
        FileAttribute fAttr = null;

        // sends a request to this RC
        sendEvent(eventTag, lfn, rcID);

        // waiting for a response from the RC
        Sim_type_p tag = new Sim_type_p(DataGridTags.CTLG_FILE_ATTR_DELIVERY);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        try {
            fAttr = (FileAttribute) ev.get_data();
        }
        catch (Exception e)
        {
            fAttr = null;
            System.out.println(super.get_name()
                    + ".getFileAttribute(): Exception");
        }
        return fAttr;
    }

    /**
     * Adds a master file to a designated resource
     * @param   file    a master file
     * @param   resID   a resource ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean addMaster(File file, int resID)
    {
        if (file == null || resID == -1) {
            return false;
        }

        int fileSize = file.getSizeInByte();
        Object[] packet = new Object[2];
        packet[0] = file;
        packet[1] = myID_;

        int tag = DataGridTags.FILE_ADD_MASTER;
        super.send(super.output, 0.0, tag, new IO_data(packet,fileSize,resID));

        // wait for the result back
        tag = DataGridTags.FILE_ADD_MASTER_RESULT;
        FilterDataResult type = new FilterDataResult(file.getName(), tag);
        Sim_event ev = new Sim_event();
        super.sim_get_next(type, ev);

        boolean result = false;
        try
        {
            packet = (Object[]) ev.get_data();              // get the data
            String resName = GridSim.getEntityName(resID);  // resource name
            int msg = ((Integer) packet[2]).intValue();     // get the result
            if (msg == DataGridTags.FILE_ADD_SUCCESSFUL)
            {
                result = true;
                System.out.println(super.get_name() + ".addMaster(): " +
                        file.getName() + " has been added to " + resName);
            }
            else {
                System.out.println(super.get_name() + ".addMaster(): " +
                        "Error in adding " + file.getName() + " to " + resName);
            }
        }
        catch (Exception e)
        {
            result = false;
            System.out.println(super.get_name() + ".addMaster(): Exception");
        }

        return result;
    }

    /**
     * Makes a replica of the given master file to another resource
     * @param   master  a master file
     * @param   resID   a resource ID that will be storing the replica file
     * @return  <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean replicateFile(File master, int resID)
    {
        // check for errors first
        if (master == null || resID == -1) {
            return false;
        }

        File file = master.makeReplica();   // makes a replica of this file
        int fileSize = file.getSizeInByte();    // size of the replica file
        Object[] packet = new Object[2];
        packet[0] = file;
        packet[1] = myID_;

        int tag = DataGridTags.FILE_ADD_REPLICA;
        super.send(super.output, 0, tag, new IO_data(packet, fileSize, resID));

        // wait for ACK
        tag = DataGridTags.FILE_ADD_REPLICA_RESULT;
        FilterDataResult type = new FilterDataResult(file.getName(), tag);
        Sim_event ev = new Sim_event();
        super.sim_get_next(type, ev);

        boolean result = false;
        try
        {
            packet = (Object[]) ev.get_data();
            String resName = GridSim.getEntityName(resID);
            String filename = file.getName();

            int msg = ((Integer) packet[1]).intValue();
            if (msg == DataGridTags.FILE_ADD_SUCCESSFUL)
            {
                result = true;
                System.out.println(super.get_name() + ".replicateFile(): " +
                        filename + " has been replicated to " + resName);
            }
            else {
                System.out.println(super.get_name() + ".replicateFile(): " +
                        "There was an error in replicating " + filename +
                        " to " + resName);
            }
        }
        catch (Exception e) {
            System.out.println(super.get_name()+".replicateFile(): Exception");
            result = false;
        }

        return result;
    }

    /**
     * Deletes a given file stored in a resource
     * @param   filename    a file name
     * @param   resID       a resource ID stores the file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean deleteFile(String filename, int resID)
    {
        // check for errors first
        if (resID == -1 || filename == null) {
            return false;
        }

        Object[] packet = new Object[2];
        packet[0] = filename;
        packet[1] = myID_;

        // send the event
        super.send(super.output, 0, DataGridTags.FILE_DELETE_REPLICA,
                   new IO_data(packet, DataGridTags.PKT_SIZE, resID));

        // wait for ACK
        int tag = DataGridTags.FILE_DELETE_REPLICA_RESULT;
        FilterDataResult type = new FilterDataResult(filename, tag);
        Sim_event ev = new Sim_event();
        super.sim_get_next(type, ev);

        boolean result = false;
        try
        {
            packet = (Object[]) ev.get_data();
            String resName = GridSim.getEntityName(resID);
            int msg = ((Integer) packet[1]).intValue();
            if (msg == DataGridTags.FILE_DELETE_SUCCESSFUL) {
                result = true;
                System.out.println(super.get_name() + ".deleteFile(): " +
                        filename + " has been deleted from " + resName);
            }
            else {
                System.out.println(super.get_name() + ".deleteFile(): " +
                        "There was an error in deleting " + filename +
                        " from " + resName);
            }

        }
        catch (Exception e) {
            System.out.println(super.get_name() + ".deleteFile(): Exception");
            result = false;
        }
        return result;
    }

    /**
     * Deletes a master file stored in a resource
     * @param   filename    a file name
     * @param   resID       a resource ID stores the file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean deleteMaster(String filename, int resID)
    {
        // check for errors first
        if (resID == -1 || filename == null) {
            return false;
        }

        Object[] packet = new Object[2];
        packet[0] = filename;
        packet[1] = myID_;

        // send the event
        super.send(super.output, 0, DataGridTags.FILE_DELETE_MASTER,
                   new IO_data(packet, DataGridTags.PKT_SIZE, resID));

        // wait for ACK
        int tag = DataGridTags.FILE_DELETE_MASTER_RESULT;
        FilterDataResult type = new FilterDataResult(filename, tag);
        Sim_event ev = new Sim_event();
        super.sim_get_next(type, ev);

        boolean result = false;
        try
        {
            packet = (Object[]) ev.get_data();
            String resName = GridSim.getEntityName(resID);
            int msg = ((Integer) packet[1]).intValue();
            if (msg == DataGridTags.FILE_DELETE_SUCCESSFUL) {
                result = true;
                System.out.println(super.get_name() + ".deleteMaster(): " +
                        filename + " has been deleted from " + resName);
            }
            else {
                System.out.println(super.get_name() + ".deleteMaster(): " +
                        "There was an error in deleting " + filename +
                        " from " + resName);
            }

        }
        catch (Exception e) {
            System.out.println(super.get_name() + ".masterFile(): Exception");
            result = false;
        }
        return result;
    }

    
    
    /**
     * Gets a list of file attributes from a given filter
     * @param   filter  a filtering function
     * @return a list of results or <tt>null</tt> if not found
     */
    public ArrayList getAttributeList(Filter filter)
    {
        // check for errors first
        int rcID = getReplicaCatalogueID();
        if (filter == null || rcID == -1) {
            return null;
        }

        ArrayList attrList = null;
        int eventTag = DataGridTags.CTLG_FILTER;
        Object[] packet = new Object[2];
        packet[0] = filter;
        packet[1] = myID_;

        // send an event message
        super.send(super.output, 0, eventTag,
                   new IO_data(packet, DataGridTags.PKT_SIZE, rcID));

        // waiting for a response from the RC
        Sim_type_p tag = new Sim_type_p(DataGridTags.CTLG_FILTER_DELIVERY);
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);    // only look for this type of ack

        try {
            attrList = (ArrayList) ev.get_data();
        }
        catch (Exception e) {
            attrList = null;
            System.out.println(super.get_name() +
                    ".getAttributeList(): Exception");
        }
        return attrList;
    }

    /**
     * Gets a file from a specific resource
     * @param   lfn     a logical file name
     * @param   resID   a resource ID that contains the file
     * @return a File object or <tt>null</tt> if not found
     */
    public File getFile(String lfn, int resID)
    {
        if (lfn == null || resID == -1) {
            return null;
        }

        // sends a request to the RC
        int eventTag = DataGridTags.FILE_REQUEST;
        sendEvent(eventTag, lfn, resID);

        // waiting for a response from the resource
        Sim_type_p tag = new Sim_type_p(DataGridTags.FILE_DELIVERY);
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);    // only look for this type of ack

        File file = null;
        try {
            file = (File) ev.get_data();
        }
        catch (Exception e) {
            file = null;
            System.out.println(super.get_name() + ".getFile(): Exception");
        }
        return file;
    }

    /**
     * Gets a full name of the given file.
     * When a user requests an operation with a filename, it usually means a
     * common name. But in the grid this file is known as "common
     * name+uniqueID", e.g. test4 (common name is "test" and uniqueID is "4").
     * This function returns the name upon which the file is known in the grid.
     *
     * @param filename  a file name
     * @return the full filename of the file (i.e. filename+uniqueID) or
     *         <tt>null</tt> if empty
     */
    public String getFullFilename(String filename)
    {
        ArrayList list = getAttributeList(new FileNameFilter(filename));
        if (list != null && list.size() > 0) {
            FileAttribute att = (FileAttribute) list.get(0);
            return att.getName();
        }
        return null;
    }

    /**
     * Sends a new event to a resource
     * @param eventTag  an event tag ID
     * @param lfn       a logical file name
     * @param resID     a resource ID
     */
    private void sendEvent(int eventTag, String lfn, int resID)
    {
        int size = lfn.length();
        if (size < DataGridTags.PKT_SIZE) {
            size = DataGridTags.PKT_SIZE;
        }
        Object[] packet = new Object[2];
        packet[0] = lfn;
        packet[1] = myID_;

        // send a message
        super.send(super.output, 0, eventTag, new IO_data(packet,size,resID));
    }

} 
