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
 * This class acts as a local RC and/or a leaf RC in a hierarchical model.
 * It is responsible for processing queries from users and resources.<br>
 * NOTE: Generating a unique ID for each file name is done by the
 *       {@link gridsim.datagrid.index.TopRegionalRC} entity only.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 * @see gridsim.datagrid.index.TopRegionalRC
 */
public class RegionalRC extends AbstractRC
{
    private Hashtable catalogueHash_; // Stores information about file locations
    private Hashtable attrHash_;      // Stores the attributes of files
    private int higherRC_;            // id of the higher level RC
    private Integer senderID_;        // id of the sender: this ID or res ID

    // List of requests that are still waiting for confirmation from higher
    // level RC
    private ArrayList waitingMasterRegistration_;   // for registration
    private ArrayList waitingMasterDeletes_;        // for de-registration
    private ArrayList waitingReplicaAddAck_;
    private ArrayList waitingReplicaDeleteAck_;
    private ArrayList waitingReplicaRequest_;


    // -----------------INITIALIZATION--------------------------------
    /**
     * Creates a new <b>local</b> Replica Catalogue (RC) entity.
     * This constructor should be used if you want this RC entity to be inside
     * a resource's domain, hence known as <i>a Local RC</i>.
     * As a consequence, this entity uses the resource ID and I/O port for
     * indentification.
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
    public RegionalRC(String name, int resourceID, Sim_port outputPort)
                      throws Exception
    {
        super(name, resourceID, outputPort);
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
    public RegionalRC(String name, Link link) throws Exception
    {
        super(name, link);
        init();
    }

    /** Initialization of all atttributes */
    private void init()
    {
        higherRC_ = -1;
        catalogueHash_ = new Hashtable();
        attrHash_ = new Hashtable();
        waitingMasterRegistration_ = null;
        waitingMasterDeletes_ = null;
        waitingReplicaAddAck_ = null;
        waitingReplicaDeleteAck_ = null;
        waitingReplicaRequest_ = new ArrayList();

        int id = -1;
        if (localRC_) {     // if this RC is a local one
            id = resourceID_;
        }
        else {  // otherwise, use my entity ID
            id = super.get_id();
        }
        senderID_ = new Integer(id);
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
        if (ev == null) {
            return false;
        }

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

            //----HIGHER LEVEL CATALOGUE RESULTS-----
            case DataGridTags.CTLG_ADD_MASTER_RESULT:
                processAddMasterResult(ev);
                if (this.localRC_) {
                    result = false;
                }
                break;

            case DataGridTags.CTLG_DELETE_MASTER_RESULT:
                processCatalogueDeleteMasterResult(ev);
                if (this.localRC_) {
                    result = false;
                }
                break;

            case DataGridTags.CTLG_ADD_REPLICA_RESULT:
                processCatalogueAddReplicaResult(ev);
                if (this.localRC_) {
                    result = false;
                }
                break;
            case DataGridTags.CTLG_DELETE_REPLICA_RESULT:
                processCatalogueDeleteReplicaResult(ev);
                if (this.localRC_) {
                    result = false;
                }
                break;

            case DataGridTags.CTLG_REPLICA_DELIVERY:
                processReplicaDelivery(ev);
                break;

            default:
                if (!this.localRC_) {
                    System.out.println(super.get_name() +
                    ".processOtherEvent(): Warning - unknown tag = "+ev.get_tag());
                }
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
     * If the catalogue contains an entry for this file name (i.e. a
     * replica or master file was already registered from this RC to a higher
     * level RC), then adds this entry to the catalogue.
     * Otherwise registers this replica to the higher level RC.
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

        int msgTag = DataGridTags.CTLG_ADD_REPLICA_SUCCESSFUL;
        try
        {
            // this RC doesn't have the replica
            // the registration has to be sent to the higher RC
            if (!catalogueHash_.containsKey(name))
            {
                int eventTag = DataGridTags.CTLG_ADD_REPLICA;
                sendMessageToHighLevelRC(name, eventTag);

                // add this event to the list of waiting registrations
                if (waitingReplicaAddAck_ == null) {
                    waitingReplicaAddAck_ = new ArrayList();
                }

                waitingReplicaAddAck_.add(obj);
            }
            // if already registered to the higher level, we just add the new
            // location to the catalogue and send the acknowledge to the user
            else
            {
                ArrayList list = (ArrayList) catalogueHash_.get(name);
                list.add(resID);
                sendMessage(name, DataGridTags.CTLG_ADD_REPLICA_RESULT,
                            msgTag, resID.intValue() );
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ".processAddReplica(): exception error");
        }

    }

    /**
     * Sends an event to the higher level RC
     * @param name      a filename
     * @param msgTag    a tag name
     */
    private void sendMessageToHighLevelRC(String name, int msgTag)
    {
        Object[] pack = new Object[2];
        pack[0] = name;
        pack[1] = senderID_;

        // send the message to the higher level RC
        super.send(super.output, 0, msgTag,
            new IO_data(pack, DataGridTags.PKT_SIZE, getHigherLevelRCid()) );
    }

    /**
     * Manages the request for deleting a replica from the Replica Catalogue.
     * If the catalogue contains only one entry for the filename, then send
     * a delete request to the higher level RC (later handled by
     * {@link RegionalRC#processCatalogueDeleteReplicaResult(Sim_event)}).
     * Otherwise, just delete the entry from the catalogue and send the
     * response back to the resource.
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
            // if there will be no more files registered on this resource, then
            // first unregister this RC from the higher level RC
            ArrayList list = (ArrayList) catalogueHash_.get(name);
            if (list != null && list.size() == 1)
            {
                int event = DataGridTags.CTLG_DELETE_REPLICA;
                sendMessageToHighLevelRC(name, event);

                // add the event to the list of waiting requests
                if (waitingReplicaDeleteAck_ == null) {
                    waitingReplicaDeleteAck_ = new ArrayList();
                }

                waitingReplicaDeleteAck_.add(obj);
            }
            // otherwise just delete the location from the catalogue
            else
            {
                if (!list.remove(resID)) {
                    msg = DataGridTags.CTLG_DELETE_REPLICA_ERROR_DOESNT_EXIST;
                }

                // notify the sender
                sendMessage(name, DataGridTags.CTLG_DELETE_REPLICA_RESULT, msg,
                            resID.intValue() );
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ".processDeleteReplica(): exception error");
        }
    }

    /**
     * Registers a master file into the Replica Catalogue.
     * The method is similar to {@link #processAddReplica(Sim_event)}.
     * However, in this method, the file is passed to the High level RC
     *
     * @param ev    a Sim_event object
     */
    private void processAddMaster(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        String filename = (String) data[0];     // file name

        // put this registration on hold
        if (waitingMasterRegistration_ == null) {
            waitingMasterRegistration_ = new ArrayList();
        }
        waitingMasterRegistration_.add(data);

        // then send this master file to the higher RC entity
        Object[] obj = new Object[3];
        obj[0] = data[0];  // file name
        obj[1] = data[1];  // location id
        obj[2] = senderID_;    // sender id

        // send the info to higher RC
        super.send(super.output, 0, DataGridTags.CTLG_ADD_MASTER,
            new IO_data(obj, DataGridTags.PKT_SIZE, getHigherLevelRCid()) );
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
            if (list.size() == 1)
            {
                int event = DataGridTags.CTLG_DELETE_MASTER;
                sendMessageToHighLevelRC(name, event);

                if (waitingMasterDeletes_ == null) {
                    waitingMasterDeletes_ = new ArrayList();
                }
                waitingMasterDeletes_.add(obj);
            }
            else
            {
                msg = DataGridTags.CTLG_DELETE_MASTER_REPLICAS_EXIST;
                sendMessage(name, DataGridTags.CTLG_DELETE_MASTER_RESULT, msg,
                            resID.intValue() );
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ".processDeleteMaster(): exception error");
        }
    }


    //  -----------------PROCESS HIGHER LEVEL CATALOGUE RESULTS------------

    /**
     * Handles an incoming message regarding to the result of Master file
     * addition
     * @param ev    a Sim_event object
     */
    private void processAddMasterResult(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0];                 // get name
        int uniqueID = ((Integer) pack[1]).intValue();      // get unique ID
        int msg = ((Integer) pack[2]).intValue();           // get msg

        Integer resourceID = null;
        if (msg == DataGridTags.CTLG_ADD_MASTER_SUCCESSFUL)
        {
            // find the request for uniqueID
            for (int i = 0; i < waitingMasterRegistration_.size(); i++)
            {
                Object[] data = (Object[]) waitingMasterRegistration_.get(i);
                if (filename.equals((String) data[0]) == true)
                {
                    resourceID = (Integer) data[2];
                    FileAttribute attr = (FileAttribute) data[1];
                    attr.setRegistrationID(uniqueID);

                    // save the new entry to the hashtable (also the
                    // FileAttribute)
                    ArrayList list = new ArrayList();
                    list.add(resourceID);
                    catalogueHash_.put(filename + uniqueID, list);
                    attrHash_.put(filename + uniqueID, attr);

                    waitingMasterRegistration_.remove(i);   // remove
                    break;
                }
            } // end for
        } // end if

        //if this is a localRC, then it doesn't need to send a message to
        // the resource
        if (this.localRC_ == false && resourceID != null)
        {
            int id = resourceID.intValue();
            super.send(super.output, 0, DataGridTags.CTLG_ADD_MASTER_RESULT,
                       new IO_data(pack, DataGridTags.PKT_SIZE, id) );
        }
    }

    /**
     * Process the result of deleting a master from a higher level RC. When the
     * result of a succesfull deletion of a master file arrives, the file entry
     * is deleted also from this RC.
     *
     * @param ev    a Sim_event object
     */
    private void processCatalogueDeleteMasterResult(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0];         // get file name
        int msg = ((Integer) pack[1]).intValue();   // get msg result

        int eventSource = findEvent(waitingMasterDeletes_, filename);
        ArrayList list = (ArrayList) catalogueHash_.get(filename);

        if (msg == DataGridTags.CTLG_DELETE_MASTER_SUCCESSFUL)
        {
            list.remove(new Integer(eventSource));  // remove from the list
            if (list.size() == 0) {
                catalogueHash_.remove(filename);
            }
        }

        // if it is not a local RC, then send a msg back
        if (!this.localRC_)
        {
            sendMessage(filename, DataGridTags.CTLG_DELETE_MASTER_RESULT,
                        msg, eventSource);
        }
    }

    /**
     * Process the result of adding a replica from a higher level RC.
     * @param ev
     */
    private void processCatalogueAddReplicaResult(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0];         // get file name
        int msg = ((Integer) pack[1]).intValue();   // get result

        int eventSource = findEvent(waitingReplicaAddAck_, filename);
        ArrayList list = (ArrayList) catalogueHash_.get(filename);

        // if adding replica into top RC is successful
        if (msg == DataGridTags.CTLG_ADD_REPLICA_SUCCESSFUL)
        {
            // create a new entry in the catalogue
            if (list == null)
            {
                list = new ArrayList();
                catalogueHash_.put(filename, list);
            }

            list.add(new Integer(eventSource));
        }

        // if this is a local RC, we should not send the event back
        if (!this.localRC_)
        {
            sendMessage(filename, DataGridTags.CTLG_ADD_REPLICA_RESULT,
                        msg, eventSource);
        }
    }

    /**
     * Process the result of a deletion from a higher level RC. If the deletion
     * was succesful, delete also on this RC. Send the message to the resource
     * where the file was deleted.
     *
     * @param ev    a Sim_event object
     */
    private void processCatalogueDeleteReplicaResult(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0];         // get file name
        int msg = ((Integer) pack[1]).intValue();   // get result

        int eventSource = findEvent(waitingReplicaDeleteAck_, filename);
        ArrayList list = (ArrayList) catalogueHash_.get(filename);

        if (msg == DataGridTags.CTLG_DELETE_REPLICA_SUCCESSFUL)
        {
            list.remove(new Integer(eventSource));
            if (list.size() == 0) {
                catalogueHash_.remove(filename);
            }
        }

        if (!this.localRC_)
        {
            sendMessage(filename, DataGridTags.CTLG_DELETE_REPLICA_RESULT,
                        msg, eventSource);
        }
    }

    /**
     * Process the delivery of a replica destination from a higher level Replica
     * Catalogue. The higher level Replica Catalogue delivers the ID of the
     * Replica Catalogue, where the information about the actual location of a
     * replica is stored. The method sends the request the RC with the ID (that
     * was just received).
     *
     * @param ev
     */
    private void processReplicaDelivery(Sim_event ev)
    {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];     // get file name
        Integer rcID = (Integer) data[1];       // get RC id
        int destination = -1;
        int event = 0;
        int eventSource = findEvent(waitingReplicaRequest_, filename);

        if (rcID.intValue() != -1)
        {
            data[1] = new Integer(eventSource);
            destination = rcID.intValue();
            event = DataGridTags.CTLG_GET_REPLICA;
        }
        else
        {
            destination = eventSource;
            event = DataGridTags.CTLG_REPLICA_DELIVERY;
        }

        super.send(super.output, GridSimTags.SCHEDULE_NOW, event,
                   new IO_data(data, DataGridTags.PKT_SIZE, destination) );
    }

    /**
     * An auxiliary method, used to find an event stored in a certain list based
     * on the filename.
     *
     * @param eventList     an event list
     * @param filename      a file name to be searched
     * @return the source of the event
     */
    private int findEvent(ArrayList eventList, String filename)
    {
        for (int i = 0; i < eventList.size(); i++)
        {
            Object[] dataTemp = (Object[]) eventList.get(i);
            Integer source = (Integer) dataTemp[1];
            if (filename.equals((String) dataTemp[0]) == true)
            {
                eventList.remove(i);
                return source.intValue();
            }
        }

        return -1;
    }


    //-----------------PROCESS USER REQUESTS-------------------------------
    /**
     * Processes the request for a file attribute. If the Replica Catalogue
     * contains the {@link gridsim.datagrid.FileAttribute}
     * of the file then it is sent to the
     * requester. Otherwise, the request is forwarded to the higher level
     * Replica Catalogue.
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

            String name = (String) obj[0];          // get name
            Integer senderID = (Integer) obj[1];    // get sender ID
            requesterID = senderID.intValue();

            attr = (FileAttribute) attrHash_.get(name);
            if (attr != null) {
                size = attr.getAttributeSize();
            }
            else if (this.getHigherLevelRCid() != -1)
            {
                super.send(super.output, 0.0, ev.get_tag(),
                    new IO_data(obj,DataGridTags.PKT_SIZE,getHigherLevelRCid()));
            }
            else {      //if there is no such file registered
                attr = null;
            }
        }
        catch (Exception e) {
            attr = null;
        }

        // send back to requester
        if (requesterID != -1)
        {
            super.send(super.output, 0.0, DataGridTags.CTLG_FILE_ATTR_DELIVERY,
                       new IO_data(attr, DataGridTags.PKT_SIZE, requesterID) );
        }
    }

    /**
     * Processes a request for filtering the files. A user sends a
     * {@link gridsim.datagrid.filter.Filter} to the Replica Catalogue.
     * Only the top level Replica
     * Catalogue runs the filter accross the list of all
     * {@link gridsim.datagrid.FileAttribute}.
     * All the attributes that are filtered out are sent back to the
     * requester.
     *
     * @param ev    the event with the <code>Filter</code>
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

        int destination = ((Integer) data[1]).intValue();
        super.send(super.output, 0, DataGridTags.CTLG_FILTER,
            new IO_data(data, DataGridTags.PKT_SIZE, getHigherLevelRCid()) );
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

        String filename = (String) data[0];         // get name
        Integer sender = (Integer) data[1];         // get destination
        // if our catalogue contains the file, we immediately send it back
        Object[] dataTemp = new Object[2];
        dataTemp[0] = filename;

        /**** // DEBUG
        System.out.println(super.get_name() + ".processGetReplica(): for " +
           filename + " from " + GridSim.getEntityName(sender.intValue()));
        ****/

        ArrayList list = (ArrayList) catalogueHash_.get(filename);
        if (list != null)
        {
            dataTemp[1] = (Integer) list.get(0);
            super.send(super.output, 0, DataGridTags.CTLG_REPLICA_DELIVERY,
                new IO_data(dataTemp,Link.DEFAULT_MTU,sender.intValue()) );
        }
        else
        {
            dataTemp[1] = senderID_;
            waitingReplicaRequest_.add(data);
            super.send(super.output, 0, DataGridTags.CTLG_GET_REPLICA,
                new IO_data(dataTemp, Link.DEFAULT_MTU, getHigherLevelRCid()) );
        }
    }

    /**
     * Sends the list of file replica locations registered on this RC.
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
        Integer sender = (Integer) data[1];         // get sender ID

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
                   new IO_data(pack, DataGridTags.PKT_SIZE, dest) );
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
     * start of simulation
     * @param fAttr     a file attribute object
     * @param sourceID  the owner ID of this file
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
        else
        {
            list = new ArrayList();
            list.add( new Integer(sourceID) );

            catalogueHash_.put(fAttr.getName(), list);
            attrHash_.put(fAttr.getName(), fAttr);
        }

        // register also to the higher level RC
        int higherRC = getHigherLevelRCid();
        AbstractRC rc = (AbstractRC)Sim_system.get_entity(higherRC);
        rc.registerOriginalFile(fAttr, senderID_.intValue());
        return true;
    }

    /**
     * Gets the ID of the Replica Catalogue that is the parent of this
     * catalogue in the hierarchy. If the entity doesn't have a higher level RC
     * registered then the default TopLevel replica catalogue is returned
     *
     * @return the ID of the higher RC
     * @see gridsim.datagrid.index.TopRegionalRC
     */
    public int getHigherLevelRCid()
    {
        int id = -1;
        if (higherRC_ == -1) {
            id = GridSim.getEntityId(TopRegionalRC.DEFAULT_NAME);
        }
        else {
            id = higherRC_;
        }

        return id;
    }

    /**
     * Sets the ID of the Replica Catalogue that is the parent of this
     * catalogue in the hierarchy.
     * @param higherLevelRCid   a Replica Catalogue ID
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setHigherLevelRCid(int higherLevelRCid)
    {
        if (higherLevelRCid < 0) {
            return false;
        }

        higherRC_ = higherLevelRCid;
        return true;
    }

} 

