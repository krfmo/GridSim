/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2006, The University of Melbourne, Australia and
 * University of Ljubljana, Slovenia 
 */

package gridsim.datagrid;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_system;
import gridsim.GridSimTags;
import gridsim.IO_data;
import gridsim.ParameterException;
import gridsim.datagrid.index.AbstractRC;
import gridsim.datagrid.storage.Storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a class which contains the basic functionality of a Replica Manager
 * in a Data Grid. The current functionlity provided by this implementation
 * includes the following:
 * <ul>
 * <li> Adding a master file or a replica to the storage and register it to the
 * RC.
 * <li> Removing master and replica files from the storage and deregister from
 * RC.
 * <li> Sending requested files to users.
 * <li> Managing a {@link gridsim.datagrid.DataGridlet}, i.e. transferring the
 * neccessary files to the local storage and pass the gridlet to the
 * AllocationPolicy for execution.
 * </ul>
 * 
 * @author Uros Cibej and Anthony Sulistio
 * @since GridSim Toolkit 4.0
 */
public class SimpleReplicaManager extends ReplicaManager {
    private ArrayList filesWaitingForAddACK_; // waiting list for add

    private ArrayList filesWaitingForDeleteACK_; // waiting list for delete

    private ArrayList masterFilesWaitingForAddACK_;

    private ArrayList masterFilesWaitingForDeleteACK_;

    private ArrayList priorityFile_;

    // the list of all DataGridlets waiting to acquire the needed files
    private ArrayList waitingDataGridlet_;

    //  -------------------INITIALIZATION-------------------------------

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
    public SimpleReplicaManager(String name, String resourceName)
            throws ParameterException {
        super(name, resourceName);
        commonInit();
    }

    /** Initializes all attributes */
    private void commonInit() {
        super.storageList_ = new ArrayList();
        filesWaitingForAddACK_ = new ArrayList();
        filesWaitingForDeleteACK_ = new ArrayList();
        masterFilesWaitingForAddACK_ = new ArrayList();
        masterFilesWaitingForDeleteACK_ = new ArrayList();
        waitingDataGridlet_ = new ArrayList();
        priorityFile_ = new ArrayList();
    }

    //  -------------------STORAGE/FILE MANIPULATION METHODS------------------

    /**
     * Adds a file to the local storage. However, the file is not registered
     * to the Replica Catalogue.
     * <br>
     * In this implementation, it looks through all the available
     * storages if there is some space. It stores on the first storage that has
     * enough space. In addition, we assume all files are read-only.
     * Hence, existing files can not be overwritten.
     *
     * @param file  a file to be placed on the local resource
     * @return an integer number denoting whether this operation is successful
     *         or not
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_ADD_ERROR_STORAGE_FULL
     */
    protected int addFile(File file) {
        // at the moment we assume all files are read only. To overwrite a file
        // we use FILE_MODIFY
        if (super.contains(file.getName())) {
            return DataGridTags.FILE_ADD_ERROR_EXIST_READ_ONLY;
        }

        // check storage space first
        if (storageList_.size() <= 0) {
            return DataGridTags.FILE_ADD_ERROR_STORAGE_FULL;
        }

        Storage tempStorage = null;
        int msg = DataGridTags.FILE_ADD_ERROR_STORAGE_FULL;

        for (int i = 0; i < storageList_.size(); i++) {
            tempStorage = (Storage) storageList_.get(i);
            if (tempStorage.getAvailableSpace() >= file.getSize()) {
                tempStorage.addFile(file);
                msg = DataGridTags.FILE_ADD_SUCCESSFUL;
                break;
            }
        }

        return msg;
    }

    /**
     * Adds a list of storage elements to the DataGrid resource
     * @param storageList   a list of storage elements to be added
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean addStorage(List storageList) {
        if (storageList == null) {
            return false;
        }

        boolean result = false;
        try {
            storageList_.addAll(storageList);
            result = true;
        } catch (Exception e) {
            result = false;
        }

        return result;
    }

    /**
     * Adds a storage element to the DataGrid resource
     * @param storage   the storage element to be added
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean addStorage(Storage storage) {
        if (storage == null) {
            return false;
        }
        storageList_.add(storage);
        return true;
    }

    /**
     * Deletes a file from the local storage, and registers
     * the change to the designated Replica Catalogue.
     * @param fileName  the filename of the file to be deleted.
     * @return an integer number denoting whether this operation is successful
     *         or not
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_ERROR_READ_ONLY
     */
    protected int deleteFile(String fileName) {
        int msg = deleteFileFromStorage(fileName, false, false);
        if (msg == DataGridTags.FILE_DELETE_SUCCESSFUL) {
            msg = super.deregisterDeletedFile(fileName,
                    DataGridTags.CTLG_DELETE_REPLICA);
        }
        return msg;
    }

    /**
     * Deletes the file from the storage. Also, check whether it is
     * possible to delete the file from the storage.
     *
     * @param fileName      the name of the file to be deleted
     * @param deleteMaster  do we want to delete the master file or not
     * @param justTest      <tt>true</tt> if you just want to test the file, or
     *                      <tt>false</tt> if you want to actually delete it
     * @return the error message as defined in
     *         {@link gridsim.datagrid.DataGridTags}
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_SUCCESSFUL
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_ERROR_ACCESS_DENIED
     * @see gridsim.datagrid.DataGridTags#FILE_DELETE_ERROR
     */
    private int deleteFileFromStorage(String fileName, boolean deleteMaster,
            boolean justTest) {
        Storage tempStorage = null;
        File tempFile = null;
        int msg = DataGridTags.FILE_DELETE_ERROR;

        for (int i = 0; i < storageList_.size(); i++) {
            tempStorage = (Storage) storageList_.get(i);
            tempFile = tempStorage.getFile(fileName);
            if (tempFile != null) {
                // if want to delete a master copy, then you can't
                if (tempFile.isMasterCopy() && !deleteMaster) {
                    msg = DataGridTags.FILE_DELETE_ERROR_ACCESS_DENIED;
                }
                // if a file is a replica, but want to delete a master one
                else if (!tempFile.isMasterCopy() && deleteMaster) {
                    msg = DataGridTags.FILE_DELETE_ERROR_ACCESS_DENIED;
                } else {
                    // if you want to actually delete this file
                    if (!justTest) {
                        tempStorage.deleteFile(fileName, tempFile);
                    }
                    msg = DataGridTags.FILE_DELETE_SUCCESSFUL;
                }
            }
        } // end for

        return msg;
    }

    /**
     * Gets a physical file based on its name
     * @param fileName  the file name to be retrieved
     * @return the physical file or <tt>null</tt> if not found
     */
    protected File getFile(String fileName) {
        Storage tempStorage = null;
        File tempFile = null;

        for (int i = 0; i < storageList_.size(); i++) {
            tempStorage = (Storage) storageList_.get(i);
            tempFile = tempStorage.getFile(fileName);

            if (tempFile != null) {
                break;
            } else {
                tempFile = null;
            }
        }

        return tempFile;
    }

    //  -------------------MAIN METHODS FOR MANAGING EVENTS-------------------

    /**
     * The main method of the data manager, which is responsible for managing
     * all the incoming events.
     */
    public void body() {
        // a loop that is looking for internal events only
        Sim_event ev = new Sim_event();
        while (Sim_system.running()) {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION) {
                break;
            }
            processEvent(ev);
        }

        // CHECK for ANY INTERNAL EVENTS WAITING TO BE processed
        while (super.sim_waiting() > 0) {
            super.sim_get_next(ev);
            System.out.println(super.get_name() + ".body(): Ignoring events");
        }
    }

    /**
     * Processes an incoming event
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean processEvent(Sim_event ev) {
        boolean result = true;
        File file = null;

        switch (ev.get_tag()) {
        //----USER REQUESTS------
        case DataGridTags.FILE_ADD_MASTER:
            processAddMasterFile(ev);
            break;

        case DataGridTags.FILE_DELETE_MASTER:
            processDeleteMasterFile(ev);
            break;

        case DataGridTags.FILE_ADD_REPLICA:
            processAddReplica(ev);
            break;

        case DataGridTags.FILE_DELETE_REPLICA:
            processDeleteReplica(ev);
            break;

        case DataGridTags.FILE_REQUEST:
            processFileRequest(ev);
            break;

        case DataGridTags.FILE_MODIFY:
            System.out
                    .println(super.get_name()
                            + ".processOtherEvent(): FILE_MODIFY is not implemented yet");
            break;

        //------------CATALOGUE RESULTS/RESPONSES----------
        case DataGridTags.CTLG_ADD_REPLICA_RESULT:
            this.processCatalogueAddResult(ev);
            break;

        case DataGridTags.CTLG_DELETE_REPLICA_RESULT:
            this.processCatalogueDeleteResult(ev);
            break;

        case DataGridTags.CTLG_ADD_MASTER_RESULT:
            this.processMasterAddResult(ev);
            break;

        case DataGridTags.CTLG_DELETE_MASTER_RESULT:
            processMasterDeleteResult(ev);
            break;

        case DataGridTags.FILE_DELETE_SUCCESSFUL:
            this.processCatalogueDeleteResult(ev);
            break;

        //------------DATA GRIDLET STUFF----------
        case DataGridTags.CTLG_REPLICA_DELIVERY:
            receiveReplicaLocation(ev);
            break;

        case DataGridTags.DATAGRIDLET_SUBMIT:
            DataGridlet dg = (DataGridlet) ev.get_data();
            receiveDataGridlet(dg);
            break;

        // a file was delivered to this site, it is to be used for execution
        // of a DataGridlet
        case DataGridTags.FILE_DELIVERY:
            file = (File) ev.get_data();
            receiveFileDelivery(file);
            break;

        default:
            System.out.println(super.get_name()
                    + ".processOtherEvent(): Warning - unknown tag = "
                    + ev.get_tag());
            result = false;
            break;
        }

        return result;
    }

    //------------------PROCESS USER REQUESTS----------------------------

    /**
     * Processes the request for adding a master file to the resource.
     * Firstly, the file is added to the storage.
     * <ul>
     * <li>if there are no errors, a request for registration is sent to the
     * Replica Catalogue. The request is saved in an array, to be furher
     * processed when the Replica Catalogue returns a unique ID of this file
     * (see the {@link #processMasterAddResult(Sim_event)} method).
     * <br>
     * <li>if an error occurs while adding the file to the storage, sends an
     * error message back to the sender.
     * </ul>
     *
     * @param ev    the event sent by the sender to be processed
     */
    private void processAddMasterFile(Sim_event ev) {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        File file = (File) pack[0]; // get the file
        file.setMasterCopy(true); // set the file into a master copy
        int sentFrom = ((Integer) pack[1]).intValue(); // get sender ID

        /******     // DEBUG
         System.out.println(super.get_name() + ".addMasterFile(): " +
         file.getName() + " from " + GridSim.getEntityName(sentFrom));
         *******/

        Object[] data = new Object[3];
        data[0] = file.getName();

        int msg = addFile(file); // add the file
        if (msg == DataGridTags.FILE_ADD_SUCCESSFUL) {
            registerMasterFile(file);
            data[1] = new Integer(sentFrom);
            masterFilesWaitingForAddACK_.add(data);
        } else {
            data[1] = new Integer(-1); // no sender id
            data[2] = new Integer(msg); // the result of adding a master file

            sim_schedule(outputPort_, 0, DataGridTags.FILE_ADD_MASTER_RESULT,
                    new IO_data(data, DataGridTags.PKT_SIZE, sentFrom));
        }
    }

    /**
     * Processes the request for adding a replica to the resource. Firstly, the
     * file is added to the storage.
     * <ul>
     * <li>if there are no errors, a request for registration is sent to the
     * Replica Catalogue. The request is saved in an array, to be furher
     * processed when the Replica Catalogue returns the result of the
     * registration (see the {@link #processCatalogueAddResult(Sim_event)}
     * method).
     * <br>
     * <li>if an error occurs while adding the file to the storage, sends an
     * error message to the user.
     * </ul>
     *
     * @param ev    the event sent by the sender to be processed
     */
    private void processAddReplica(Sim_event ev) {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        File file = (File) data[0]; // get file
        file.setMasterCopy(false); // set file as a replica
        int req_source = ((Integer) data[1]).intValue(); // get sender id

        int msg = addFile(file); // add file
        if (msg == DataGridTags.FILE_ADD_SUCCESSFUL) {
            registerFile(file); // register file to RC
            data[0] = file.getName();
            filesWaitingForAddACK_.add(data);
        } else { // if an error occured, the notify the sender
            sendResult(file.getName(), DataGridTags.FILE_ADD_REPLICA_RESULT,
                    msg, req_source);
        }
    }

    /**
     * Deletes a master file from the storage
     * @param ev    a Sim_event object
     */
    private void processDeleteMasterFile(Sim_event ev) {
        processDelete(ev, true);
    }

    /**
     * Deletes a replica file from the storage
     * @param ev    a Sim_event object
     */
    private void processDeleteReplica(Sim_event ev) {
        processDelete(ev, false);
    }

    /**
     * Processes the request for deleting a file (either a master or a replica)
     * from the resource. Firstly, it checks if it is possible to delete.
     * The file is not deleted from the storage until the change is registered
     * with the Replica Catalogue. When the result from the delete test is
     * known, there are two possibilities
     * <ul>
     * <li>if there are no errors, a request for removing the registration is
     * sent to the Replica Catalogue. The request is saved in an array, to be
     * furher processed when the Replica Catalogue returns the result of the
     * operation (see the {@link #processCatalogueDeleteResult(Sim_event)}
     * method).
     * <br>
     * <li>if an error occurs while deleting the file from the storage, send an
     * error message to the user.
     * </ul>
     *
     * @param ev        the event sent by the sender to be processed
     * @param isMaster  is the file to be deleted a master file or a replica
     */
    private void processDelete(Sim_event ev, boolean isMaster) {
        if (ev == null) {
            return;
        }
        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0];
        int req_source = ((Integer) data[1]).intValue();
        int tag = -1;

        // check if this file can be deleted (do not delete is right now)
        int msg = deleteFileFromStorage(filename, isMaster, true);
        if (msg == DataGridTags.FILE_DELETE_SUCCESSFUL) {
            if (isMaster) // if it is a master file
            {
                masterFilesWaitingForDeleteACK_.add(data);
                tag = DataGridTags.CTLG_DELETE_MASTER;
            } else // if it is a replica
            {
                filesWaitingForDeleteACK_.add(data);
                tag = DataGridTags.CTLG_DELETE_REPLICA;
            }

            // deregister this file from RC
            super.deregisterDeletedFile(filename, tag);
        } else // if an error occured, notify user
        {
            tag = DataGridTags.FILE_DELETE_REPLICA_RESULT;
            if (isMaster) {
                tag = DataGridTags.FILE_DELETE_MASTER_RESULT;
            }
            sendResult(filename, tag, msg, req_source);
        }
    }

    /**
     * Sends a file to the user that requested it.
     * @param ev    the event sent by the user to be processed
     */
    private void processFileRequest(Sim_event ev) {
        if (ev == null) {
            return;
        }
        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0]; // get file name
        int req_source = ((Integer) data[1]).intValue(); // get sender

        int ToS = 0; // a priority number for sending over the network
        if (data.length == 3) {
            ToS = ((Integer) data[2]).intValue(); // get ToS
        }

        File file = getFile(filename); // get the file
        int size = 0;
        if (file != null) {
            size = file.getSizeInByte();
        } else { // if file is not found
            size = DataGridTags.PKT_SIZE;
        }
        super.sim_schedule(outputPort_, 0, DataGridTags.FILE_DELIVERY,
                new IO_data(file, size, req_source, ToS));
    }

    //  -------------------PROCESS CATALOGUE RESPONSES/RESULTS-------------
    /**
     * If the addition to the Replica Catalogue is not successful, the file is
     * deleted from the resource. The message is forwarded to the user that
     * requested the addition of the replica.
     *
     * @param ev    the event sent by the Replica Catalogue
     */
    private void processCatalogueAddResult(Sim_event ev) {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0]; // replica name
        int msg = ((Integer) pack[1]).intValue(); // a message: error or not

        /*******  // DEBUG
         System.out.println(super.get_name() + ".processCatalogueAddResult(): " +
         "received result of " + filename);
         ******/

        // find the event in the waiting list
        Object[] dataTemp = searchEvent(filename, this.filesWaitingForAddACK_);
        if (dataTemp != null) {
            // if the addition was not successful, delete the file
            if (msg != DataGridTags.CTLG_ADD_REPLICA_SUCCESSFUL) {
                this.deleteFileFromStorage(filename, false, false);
            } else {
                msg = DataGridTags.FILE_ADD_SUCCESSFUL;
            }

            // delete this event from the waiting list
            filesWaitingForAddACK_.remove(dataTemp);

            // send message (error/success) to the user
            sendResult(filename, DataGridTags.FILE_ADD_REPLICA_RESULT, msg,
                    ((Integer) dataTemp[1]).intValue());
        }
    }

    /**
     * This method is very similar to
     * {@link #processCatalogueAddResult(Sim_event)}. The only
     * difference is that Replica Catalogue returns a uniqueID, i.e. a unique
     * number that prevents having the same name represent different files. This
     * uniqueId is added to the initial name. That means that the file is known
     * by the new name also on the local resource.
     * <br>
     * <b>Example: </b> if the name of the file is "researchResults" and the
     * uniqueID sent by the Replica Catalogue is 17, then the file is renamed to
     * "researchResults17".
     *
     * @param ev    a Sim_event object
     */
    private void processMasterAddResult(Sim_event ev) {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0]; // get file name
        int registrationID = ((Integer) data[1]).intValue(); // get unique ID
        int msg = ((Integer) data[2]).intValue(); // get result

        // if registration is successful
        if (msg == DataGridTags.CTLG_ADD_MASTER_SUCCESSFUL) {
            setID(filename, registrationID); // set the id of this file
        }

        // search this request from the waiting list
        Object[] dataTemp = searchEvent(filename, masterFilesWaitingForAddACK_);
        if (dataTemp != null) {
            // if the addition was not successful, delete the file
            if (msg != DataGridTags.CTLG_ADD_MASTER_SUCCESSFUL) {
                this.deleteFileFromStorage(filename, true, false);
            } else {
                msg = DataGridTags.FILE_ADD_SUCCESSFUL;
            }

            // delete this event from the waiting list
            masterFilesWaitingForAddACK_.remove(dataTemp);

            // send back the result to sender
            int senderID = ((Integer) dataTemp[1]).intValue();
            Object pack[] = new Object[3];
            pack[0] = filename; // file name
            pack[1] = new Integer(registrationID); // unique id
            pack[2] = new Integer(msg); // message

            sim_schedule(outputPort_, 0, DataGridTags.FILE_ADD_MASTER_RESULT,
                    new IO_data(pack, DataGridTags.PKT_SIZE, senderID));
        }
    }

    /**
     * Manages the response of the Replica Catalogue to a delete master request.
     * @param ev    a Sim_event object
     */
    private void processMasterDeleteResult(Sim_event ev) {
        processDeleteResult(ev, true);
    }

    /**
     * Manages the response of the Replica Catalogue to a delete master request.
     * @param ev    a Sim_event object
     */
    private void processCatalogueDeleteResult(Sim_event ev) {
        processDeleteResult(ev, false);
    }

    /**
     * Manages the response of the Replica Catalogue to a delete request. If the
     * delete request is successful the file is deleted from the storage(s) on
     * the resource. A success/error message is sent to the user.
     *
     * @param ev    a Sim_event object
     */
    private void processDeleteResult(Sim_event ev, boolean isMaster) {
        if (ev == null) {
            return;
        }

        Object[] pack = (Object[]) ev.get_data();
        if (pack == null) {
            return;
        }

        String filename = (String) pack[0]; // get file name
        int msg = ((Integer) pack[1]).intValue(); // get result

        // replica request
        int successTag = DataGridTags.CTLG_DELETE_REPLICA_SUCCESSFUL;
        int sendTag = DataGridTags.FILE_DELETE_REPLICA_RESULT;
        ArrayList eventList = filesWaitingForDeleteACK_;

        // master request
        if (isMaster) {
            successTag = DataGridTags.CTLG_DELETE_MASTER_SUCCESSFUL;
            sendTag = DataGridTags.FILE_DELETE_MASTER_RESULT;
            eventList = masterFilesWaitingForDeleteACK_;
        }

        // search this record from the waiting event list
        Object[] dataTemp = searchEvent(filename, eventList);
        if (dataTemp != null) {
            if (msg == successTag) {
                deleteFileFromStorage(filename, false, false);
                msg = DataGridTags.FILE_DELETE_SUCCESSFUL;
            }

            eventList.remove(dataTemp); // remove this request from the list

            // send the result back to sender
            sendResult(filename, sendTag, msg, ((Integer) dataTemp[1])
                    .intValue());
        }
    }

    //  -------------------ADDITIONAL METHODS ------------------------------

    /**
     * Registers all files (as master files) present on the storage(s) when
     * GridSim is started.
     */
    public void registerAllMasterFiles() {
        DataGridResource res = (DataGridResource) Sim_system
                .get_entity(super.resourceID_);
        AbstractRC rc = null;
        if (res.hasLocalRC()) {
            rc = res.getLocalRC();
        } else {
            rc = (AbstractRC) Sim_system.get_entity(super.rcID_);
        }

        if (rc == null) {
            System.out.println(super.get_name() + ".registerAllMasterFiles(): "
                    + "Warning - unable to register master files to a Replica "
                    + "Catalogue entity.");
            return;
        }

        Storage tempStorage = null;
        for (int i = 0; i < storageList_.size(); i++) {
            tempStorage = (Storage) storageList_.get(i);
            ArrayList fileList = (ArrayList) tempStorage.getFileNameList();

            for (int j = 0; j < fileList.size(); j++) {
                String filename = (String) fileList.get(j);
                File file = tempStorage.getFile(filename); // get file
                FileAttribute fAttr = file.getFileAttribute(); // get attribute

                // register before simulation starts, hence no uniqueID
                rc.registerOriginalFile(fAttr, super.resourceID_);
            }
        }
    }

    /**
     * Registers one master file.
     * @param file the master file to be registered
     */
    private void registerMasterFile(File file) {
        // need to check whether this file is a master copy or not
        if (!file.isMasterCopy()) {
            return;
        }

        FileAttribute fAttr = file.getFileAttribute();
        Object[] data = new Object[3];
        data[0] = file.getName(); // set the filename
        data[1] = fAttr; // set the file's attribute
        data[2] = super.resIdObj_; // set the resource ID

        // send this info to the RC entity
        int size = fAttr.getAttributeSize();
        super.sim_schedule(outputPort_, 0, DataGridTags.CTLG_ADD_MASTER,
                new IO_data(data, size, super.rcID_));
    }

    /**
     * Sends an event with the result of adding or deleting a file back to the
     * user.
     *
     * @param fileName
     *            the name of the file that was added/deleted
     * @param event
     *            the event to be sent (e.g. FILE_ADD_RESULT)
     * @param msg
     *            the message to sent with it (e.g. FILE_ADD_SUCCESFUL)
     * @param dest
     *            the destination of the result
     */
    private void sendResult(String fileName, int event, int msg, int dest) {
        // just a safety net
        if (dest == -1) {
            return;
        }

        // send back to sender
        Object pack[] = new Object[2];
        pack[0] = fileName;
        pack[1] = new Integer(msg);

        super.sim_schedule(outputPort_, GridSimTags.SCHEDULE_NOW, event,
                new IO_data(pack, DataGridTags.PKT_SIZE, dest));

    }

    /**
     * When a request is sent from the user, this request is saved to a list.
     * And when a response is received from the Replica Catalogue this event has
     * to be found in this list.
     *
     * @param name  of the file the event was all about.
     * @param list  the list of event where it has to search.
     * @return the event or <tt>null</tt> if empty
     */
    private Object[] searchEvent(String name, ArrayList list) {
        for (int i = 0; i < list.size(); i++) {
            Object[] dataTemp = (Object[]) list.get(i);
            if (name.equals((String) dataTemp[0])) {
                return dataTemp;
            }
        }
        return null;
    }

    /**
     * Sets the unique ID of a file. When the uniqueID is returned by the
     * Replica Catalogue, the file is renamed.
     *
     * @see SimpleReplicaManager#processMasterAddResult(Sim_event)
     * @param fileName      the old name of the file.
     * @param id            the unique ID assigned by the Replica Catalogue
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private boolean setID(String fileName, int id) {
        boolean result = false;
        int i = 0;
        File tempFile = null;
        Storage tempStorage = null;

        while ((i < storageList_.size()) && (tempFile == null)) {
            tempStorage = (Storage) storageList_.get(i);
            tempFile = tempStorage.getFile(fileName);

            if (tempFile != null) {
                tempFile.setRegistrationID(id); // set registration ID
                String newName = tempFile.getName() + id;
                tempStorage.renameFile(tempFile, newName); // rename filename
                tempFile.setName(newName); // set the new lfn
                result = true;
            }
            i++;
        }
        return result;
    }

    //----------------------DATA GRIDLET STUFF-------------------------------

    /**
     * Receives a DataGridlet object.
     * In this approach, a DataGridlet requires n files. If one or more files
     * are not available, then this RM will fetch them.
     * Only if all files are available, then this DataGridlet is ready to
     * be executed by a resource's scheduler.
     *
     * @param dg    a DataGridlet object
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise.
     */
    private boolean receiveDataGridlet(DataGridlet dg) {
        if (dg == null) {
            return false;
        }

        // get a list of required files
        LinkedList list = (LinkedList) (dg.getRequiredFiles()).clone();
        int serviceLevel = dg.getNetServiceLevel(); // get priority

        // for each file, check whether it is available or not
        for (int i = 0; i < list.size(); i++) {
            String filename = (String) list.get(i); // get file name

            // if the file is already on the local storage, the
            // transfer from a remote site is not needed
            if (contains(filename)) {
                dg.deleteRequiredFile(filename); // delete from the list
            }
            // if the file is not available, then make a replica request
            else {
                // if the file should have higher QoS
                if (serviceLevel == 1) {
                    priorityFile_.add(filename);
                }

                Object[] packet = new Object[2];
                packet[0] = filename;
                packet[1] = super.resIdObj_;
                sim_schedule(outputPort_, 0, DataGridTags.CTLG_GET_REPLICA,
                        new IO_data(packet, DataGridTags.PKT_SIZE, super.rcID_));
            }
        }

        // if all files are available locally
        if (dg.getRequiredFiles().size() == 0) {
            dg.setResourceParameter(super.resourceID_, 0);
            policy_.gridletSubmit(dg, false); // start executing this job
        } else { // otherwise, put into the queue
            waitingDataGridlet_.add(dg);
        }

        return true;
    }

    /**
     * A location of the file is returned from the RC. This function sends a
     * request for transferring the file. It also checks the network QoS for
     * this file.
     *
     * @param ev    a Sim_event object
     */
    protected void receiveReplicaLocation(Sim_event ev) {
        if (ev == null) {
            return;
        }

        Object[] data = (Object[]) ev.get_data();
        if (data == null) {
            return;
        }

        String filename = (String) data[0]; // get file name
        Integer resID = (Integer) data[1]; // get resource ID

        // make a request to transfer the given filename
        Object[] packet = new Object[3];
        packet[0] = filename; // request for this file name
        packet[1] = super.resIdObj_; // from this resource

        // it is an urgent request
        if (isPriorityFile(filename)) {
            priorityFile_.remove(filename);
            packet[2] = new Integer(1); // high priority over the network
        } else {
            packet[2] = new Integer(0); // normal priority over the network
        }

        super.sim_schedule(outputPort_, 0, DataGridTags.FILE_REQUEST,
                new IO_data(packet, DataGridTags.PKT_SIZE, resID.intValue()));
    }

    /**
     * A requested file has been delivered by another resource.
     * @param file  a File object
     * @return <tt>true</tt> if this delivery has been acknowledged,
     *         <tt>false</tt> otherwise
     */
    protected boolean receiveFileDelivery(File file) {
        if (file == null) {
            return false;
        }

        // add the file into the storage
        file.setMasterCopy(false); // set file as a replica
        addFile(file);

        for (int i = 0; i < waitingDataGridlet_.size(); i++) {
            DataGridlet dg = (DataGridlet) waitingDataGridlet_.get(i);
            dg.deleteRequiredFile(file.getName());

            // if a job does not need any more files
            if (!dg.requiresFiles()) {
                dg.setResourceParameter(super.resourceID_, 0);
                policy_.gridletSubmit(dg, false); // execute this job
                waitingDataGridlet_.remove(dg); // remove from waiting list
            }
        }

        return true;
    }

    /**
     * Checks whether the given file has a higher priority or not
     * @param filename  a file name
     * @return <tt>true</tt> if this file has a higher priority,
     *         <tt>false</tt> otherwise
     */
    private boolean isPriorityFile(String filename) {
        boolean result = false;
        Iterator iter = priorityFile_.iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next(); // get file name
            if (name.equals(filename)) {
                result = true;
                break;
            }
        }
        return result;
    }

} 

