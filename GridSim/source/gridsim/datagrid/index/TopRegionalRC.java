/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */
 
package gridsim.datagrid.index;

import java.util.*;

import eduni.simjava.*;
import gridsim.datagrid.*;
import gridsim.net.Link;
import gridsim.*;
import gridsim.datagrid.filter.Filter;


/**
 * This class acts as a centralized RC or a root RC in a hierarchical model.
 * It is responsible for generating a unique ID for each file name.
 * Hence, the full name of the file will be "filename+uniqueID"
 * when you use {@link gridsim.datagrid.DataGridUser#getFullFilename(String)}
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 * @see gridsim.datagrid.index.RegionalRC
 */
public class TopRegionalRC extends AbstractRC
{
    private Hashtable catalogueHash_;   // storing replicas
    private Hashtable attrHash_;        // storing file attributes
    private int lastUniqueID;           //  generate a uniqueID


    /** Default name for this RC entity, which is "GridSim_TopRC".
     *  NOTE: This default name is useful when a user forgets to tell the
     * {@link gridsim.datagrid.DataGridUser} or
     * {@link gridsim.datagrid.DataGridResource} entity about the RC id.
     */
    public static final String DEFAULT_NAME = "GridSim_TopRC";

    /**
     * Creates a new Replica Catalogue (RC) entity.
     * @param name  this entity name
     * @param link  the link that this GridSim entity will use to
     *              communicate with other GridSim or Network entities.
     * @throws Exception    This happens when one of the input parameters is
     *                      invalid.
     */
    public TopRegionalRC(String name, Link link) throws Exception
    {
        super(name, link);
        init();
    }

    /**
     * Creates a new Replica Catalogue (RC) entity with a default name.
     * @param link  the link that this GridSim entity will use to
     *              communicate with other GridSim or Network entities.
     * @throws Exception    This happens when the network link is null
     */
    public TopRegionalRC(Link link) throws Exception
    {
        super(TopRegionalRC.DEFAULT_NAME, link);
        init();
    }

    /** Initialization of all atttributes */
    private void init()
    {
        catalogueHash_ = new Hashtable();
        attrHash_ = new Hashtable();
        lastUniqueID = 0;
    }


    //-------MAIN METHOD FOR HANDLING REQUESTS/EVENTS--------------

    /**
     * Processes an incoming request that uses a user-defined tag. This method
     * is useful for creating a new RC entity.
     * @param ev  a Sim_event object (or an incoming event or request)
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    protected boolean processOtherEvent(Sim_event ev)
    {
        boolean result = true;
        switch ( ev.get_tag() )
        {
            //-----------REPLICA MANAGER REQUESTS--------
            case DataGridTags.CTLG_ADD_MASTER:
                processAddMaster(ev);
                break;

            case DataGridTags.CTLG_DELETE_MASTER:
                processDeleteMaster(ev);
                break;

            case DataGridTags.CTLG_ADD_REPLICA:
                processAddReplica(ev);
                break;

            case DataGridTags.CTLG_DELETE_REPLICA:
                processDeleteReplica(ev);
                break;

            //-------USER REQUESTS---------------
            case DataGridTags.CTLG_GET_FILE_ATTR:
                processFileAttrRequest(ev);
                break;

            case DataGridTags.CTLG_GET_REPLICA:
                processGetReplica(ev);
                break;

            case DataGridTags.CTLG_GET_REPLICA_LIST:
                processGetReplicaList(ev);
                break;

            case DataGridTags.CTLG_FILTER:
                processFilterFiles(ev);
                break;

            default:
                System.out.println(super.get_name() +
                    ".processOtherEvent(): Warning - unknown tag = "+ev.get_tag());
                result = false;
                break;
        }

        return result;
    }


    //  -----------------PROCESS REPLICA MANAGER REQUESTS------------

    /**
     * Manages the request for adding a replica to the catalogue. It receives an
     * event with the name of the file and the <code>resID</code> where this
     * file is located.
     * <br>
     * If the catalogue already contains an entry for this file name (i.e.
     * the master file is already registered) than adds the <code>resID</code>
     * to the list of resources with that file.
     * <br>
     * If there is no entry in the catalogue for this file name, then
     * returns an error to the resource.
     *
     * @param ev    a Sim_event object
     */
    private void processAddReplica(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] obj = (Object[]) ev.get_data();
        if (obj == null)
        {
            System.out.println(super.get_name() +
                    ".processAddReplica(): replica is null");
            return;
        }

        String name = (String) obj[0];      // get file name
        Integer resID = (Integer) obj[1];   // get resource id

        int result = DataGridTags.CTLG_ADD_REPLICA_SUCCESSFUL;
        try
        {
            /*****  // DEBUG
            System.out.println(super.get_name() +
                ".processAddReplica(): top RC for " + name);
            *****/

            // if this replica name is a new entry
            ArrayList list = (ArrayList) catalogueHash_.get(name);
            if (list == null)
            {
                /*****  // DEBUG
                System.out.println(super.get_name() +
                    ".processAddReplica(): empty list --> new catalogue for " +
                    name);
                *****/

                list = new ArrayList();   // create a new list
                list.add(resID);          // put resource ID into this list
                catalogueHash_.put(name, list);     // add into catalogue
            }
            else
            {
                list.add(resID);

                /*****  // DEBUG
                System.out.println(super.get_name() +
                    ".processAddReplica(): adds into the list for " + name);
                *****/
            }
        }
        catch (Exception e) {
            result = DataGridTags.CTLG_ADD_REPLICA_ERROR;
        }


        /*****  // DEBUG
        System.out.println(super.get_name() +
                ".processAddReplica(): sends the result back to " +
                GridSim.getEntityName((Integer) obj[1]));
        *****/

        sendMessage(name, DataGridTags.CTLG_ADD_REPLICA_RESULT,
                    result, resID.intValue() );
    }

    /**
     * Manages the request for deleting a replica from the Replica Catalogue.
     * If there is no entry for the filename in the catalogue, then return
     * an error.
     * Otherwise, delete the entry from the catalogue and send a message to
     * the lower level catalogue/or to the resource.
     *
     * @param ev    a Sim_event object
     */
    private void processDeleteReplica(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] obj = (Object[]) ev.get_data();
        if (obj == null)
        {
            System.out.println(super.get_name() +
                    ".processDeleteReplica(): no object is found");
            return;
        }

        String name = (String) obj[0];      // get file name
        Integer resID = (Integer) obj[1];   // get resource id

        int msg = DataGridTags.CTLG_DELETE_REPLICA_SUCCESSFUL;
        try
        {

            int event = DataGridTags.CTLG_DELETE_REPLICA;
            ArrayList list = (ArrayList) catalogueHash_.get(name);
            if (list != null && list.size() > 1) {
                 if (list.remove(resID) == false) {
                    msg = DataGridTags.CTLG_DELETE_REPLICA_ERROR_DOESNT_EXIST;
                }
            }
            else{
                msg = DataGridTags.CTLG_DELETE_REPLICA_ERROR_DOESNT_EXIST;
            }

        }
        catch (Exception e) {
            msg = DataGridTags.CTLG_DELETE_REPLICA_ERROR;
        }

        sendMessage(name, DataGridTags.CTLG_DELETE_REPLICA_RESULT, msg,
                    resID.intValue() );
    }

    /**
     * Registers a master file into the Replica Catalogue.
     * The method is similar to {@link #processAddReplica(Sim_event)}.
     * However, in this method, this file is known as
     * "name+uniqueID", e.g. test4 (name is "test" and uniqueID is "4").
     *
     * @param ev    a Sim_event object
     */
    private void processAddMaster(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        int result = -1;
        Object[] data = (Object[]) ev.get_data();
        if (data == null)
        {
            System.out.println(super.get_name() +
                    ".processAddMaster(): master file name is null");
            return;
        }

        String filename = (String) data[0];             // file name
        FileAttribute attr = (FileAttribute) data[1];   // file attribute
        Integer resourceID = (Integer) data[2];                 // resource ID
        int uniqueID = -1;

        try
        {
            uniqueID = createUniqueID(filename); // create a unique file ID
            result = DataGridTags.CTLG_ADD_MASTER_SUCCESSFUL;

            // put the master file attribute into the catalogue
            ArrayList list = new ArrayList();
            list.add(resourceID);

            catalogueHash_.put(filename + uniqueID, list);
            attrHash_.put(filename + uniqueID, attr);

            /*****  // DEBUG
            System.out.println(super.get_name() +
                ".processAddMaster(): " + filename + " --> id = " + uniqueID);
            *****/
        }
        catch (Exception e) {
            result = DataGridTags.CTLG_ADD_MASTER_ERROR;
        }

        // sends the result back to sender
        data = new Object[3];
        data[0] = filename;
        data[1] = new Integer(uniqueID);
        data[2] = new Integer(result);

        super.send(super.output, 0, DataGridTags.CTLG_ADD_MASTER_RESULT,
            new IO_data(data, DataGridTags.PKT_SIZE, resourceID.intValue()) );
    }

    /**
     * Deletes a master file from the catalogue. A method very
     * similar to the {@link #processDeleteReplica(Sim_event)}. The
     * difference is that the master file is never deleted if there are more
     * than one entry in the catalogue, i.e. replicas of this file still exist.
     * Before the master file can be deleted all the replicas need to be deleted
     * first.
     *
     * @param ev    a Sim_event object
     */
    private void processDeleteMaster(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] obj = (Object[]) ev.get_data();
        if (obj == null)
        {
            System.out.println(super.get_name() +
                    ".processDeleteMaster(): master file name is null");
            return;
        }

        String name = (String) obj[0];      // get file name
        Integer resID = (Integer) obj[1];   // get resource id

        int msg = DataGridTags.CTLG_DELETE_MASTER_SUCCESSFUL;
        try
        {
            ArrayList list = (ArrayList) catalogueHash_.get(name);
            if (list.size() != 1) {
                msg = DataGridTags.CTLG_DELETE_MASTER_REPLICAS_EXIST;
            }
            else if (list.remove(resID) == false) {
                msg = DataGridTags.CTLG_DELETE_MASTER_DOESNT_EXIST;
            }
            else {
                catalogueHash_.remove(name);    // remove from the catalogue
                attrHash_.remove(name);
            }
        }
        catch (Exception e) {
            msg = DataGridTags.CTLG_DELETE_MASTER_ERROR;
        }

        sendMessage(name, DataGridTags.CTLG_DELETE_MASTER_RESULT, msg,
                    resID.intValue() );
    }



    //-----------------PROCESS USER REQUESTS-------------------------------
    /**
     * Processes the request for a file attribute. If the Replica Catalogue
     * contains the {@link gridsim.datagrid.FileAttribute} of the file
     * then it is sent to the requester.
     *
     * @param ev    a Sim_event object
     */
    private void processFileAttrRequest(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        int requesterID = -1;
        int size = 0;
        FileAttribute attr = null;

        try
        {
            Object[] obj = (Object[]) ev.get_data();
            if (obj == null) {
                return;
            }

            String name = (String) obj[0];          // get file name
            Integer senderID = (Integer) obj[1];    // get sender id
            requesterID = senderID.intValue();

            // get the file attribute of a given filename
            attr = (FileAttribute) attrHash_.get(name);
            if (attr != null) {
                size = attr.getAttributeSize();
            }
            else
            {
                size = DataGridTags.PKT_SIZE;
                attr = null;
            }
        }
        catch (Exception e)
        {
            attr = null;
            size = DataGridTags.PKT_SIZE;
        }

        // send back to requester
        if (requesterID != -1)
        {
            super.send(super.output, 0, DataGridTags.CTLG_FILE_ATTR_DELIVERY,
                       new IO_data(attr, size, requesterID) );
        }
    }

    /**
     * Processes a request for filtering the files. A user sends a
     * {@link gridsim.datagrid.filter.Filter} to the Replica Catalogue.
     * Only the top level Replica Catalogue runs the filter accross the list
     * of all {@link gridsim.datagrid.FileAttribute}
     * All the attributes that are filtered out are sent back to the requester.
     *
     * @param ev    a Sim_event object
     */
    private void processFilterFiles(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        Filter f = (Filter) data[0];
        int destination = ((Integer) data[1]).intValue();

        ArrayList list = new ArrayList();
        int size = 0;

        Enumeration attributes = attrHash_.elements();
        while ( attributes.hasMoreElements() )
        {
            FileAttribute attrTemp = (FileAttribute) attributes.nextElement();
            if (f.match(attrTemp) == true)
            {
                list.add(attrTemp);     // add this attribute into the list
                size += attrTemp.getAttributeSize();
            }
        }

        // sends back the result to the sender
        super.send(super.output, 0, DataGridTags.CTLG_FILTER_DELIVERY,
                   new IO_data(list, size, destination) );
    }

    /**
     * Processes the request for a file location. If the catalogue contains the
     * file name, the first location of that file is returned back to the user.
     * If there is no enty for the file name in the catalogue and this is not
     * the top Replica Catalogue, the request is forwarded to the higher level
     * catalogue.
     *
     * @param ev    a Sim_event object
     */
    private void processGetReplica(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];         // get file name
        Integer sender = (Integer) data[1];         // get sender id

        // creates an object that contains the required information
        Object[] dataTemp = new Object[3];
        dataTemp[0] = filename;

        ArrayList list = (ArrayList) catalogueHash_.get(filename);
        if (list != null) {
            dataTemp[1] = (Integer) list.get(0);
        } else {
            dataTemp[1] = new Integer(-1);
        }

        // sends back the result to sender
        super.send(super.output, 0, DataGridTags.CTLG_REPLICA_DELIVERY,
            new IO_data(dataTemp, DataGridTags.PKT_SIZE, sender.intValue()) );
    }

    /**
     * Sends the list of file replica locations that are registered to this RC.
     * @param ev    a Sim_event object
     */
    private void processGetLocalReplicaList(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];         // get file name
        Integer sender = (Integer) data[1];         // get sender id

        // creates an object that contains the required information
        Object[] dataTemp = new Object[2];
        dataTemp[0] = filename;
        dataTemp[1] = (ArrayList) catalogueHash_.get(filename);

        // sends back the result to sender
        super.send(super.output, 0, DataGridTags.CTLG_REPLICA_LIST_DELIVERY,
            new IO_data(dataTemp, DataGridTags.PKT_SIZE, sender.intValue()) );
    }

    /**
     * Sends a list of resource IDs for the location of replicas
     * @param ev    a Sim_event object
     */
    private void processGetReplicaList(Sim_event ev) {
            processGetLocalReplicaList(ev);
    }


    //  -----------------ADDITIONAL METHODS------------

    /**
     * Creates a unique ID of a file. This method is used only on the top level
     * Replica Catalogue, since it is resposible for generating uniqued IDs.
     *
     * @return a unique ID
     */
    private int createUniqueID(String currentFileName)
    {
        lastUniqueID++;
        //test if the new id would create a clash
        ArrayList list = (ArrayList) catalogueHash_.get(currentFileName+lastUniqueID);
        while(list!=null){
            //if it creates a clash, try using a greater ID
            lastUniqueID++;
            list = (ArrayList) catalogueHash_.get(currentFileName+lastUniqueID);
        }
        return lastUniqueID;
    }

    /**
     * Sends a message as a result of adding or deleting a file. <br>
     * <b>Example: </b>the file of the operation is <i>file12 </i>, the event to
     * be sent is <i>CTLG_DELETE_REPLICA_RESULT </i> and the message to be sent
     * is <i>CTLG_DELETE_SUCCESFUL </i>.
     *
     * @param fileName  the name of the file
     * @param event     the event to be sent
     * @param msg       the message to be sent
     * @param dest      the destination of the message
     */
    private void sendMessage(String fileName, int event, int msg, int dest)
    {
        Object pack[] = new Object[2];
        pack[0] = fileName;
        pack[1] = new Integer(msg);

        super.send(super.output, GridSimTags.SCHEDULE_NOW, event,
                   new IO_data(pack, DataGridTags.PKT_SIZE, dest));
    }

    /**
     * Registers other information to a GIS entity -- THIS METHOD IS EMPTY
     */
    protected void registerOtherEntity() {
        // empty ...
    }

    /**
     * Performs last activities before the end of a simulation -- THIS
     * METHOD IS EMPTY
     */
    protected void processEndSimulation() {
        // empty ...
    }

    /**
     * Register a file which is already stored in a resource <b>before</b> the
     * start of simulation. <br>
     * NOTE: A unique id of this file IS NOT available
     *
     * @param fAttr     a FileAttribute object
     * @param sourceID  the entity ID that stores this file
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean registerOriginalFile(FileAttribute fAttr, int sourceID)
    {
        if (fAttr == null || sourceID == -1) {
            return false;
        }

        ArrayList list = (ArrayList) catalogueHash_.get( fAttr.getName() );
        if (list != null) {
            list.add( new Integer(sourceID) );
        }
        else {
            list = new ArrayList();
            list.add( new Integer(sourceID) );

            catalogueHash_.put(fAttr.getName(), list);
            attrHash_.put(fAttr.getName(), fAttr);
        }
        return true;
    }

} 

