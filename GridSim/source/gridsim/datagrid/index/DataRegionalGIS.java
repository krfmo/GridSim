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
import gridsim.index.*;
import gridsim.net.Link;
import java.util.*;


/**
 * A data GridInformationService (GIS) entity that is responsible for
 * storing a registration info from a Replica Catalogue (RC) entity and
 * answering any incoming queries.
 *
 * @author  Uros Cibej and Anthony Sulistio
 * @since   GridSim Toolkit 4.0
 */
public class DataRegionalGIS extends RegionalGIS {

    private ArrayList rcList_;          // list of local RC entities
    private ArrayList globalRCList_;    // list of global RC entities
    private ArrayList userList_;        // list of users querying for global RCs
    private int numRC_;    // counting for num of GIS entities for RC request


    /**
     * Creates a new Data GIS entity
     * @param name  this entity name
     * @param link  the link that this GridSim entity will use to
     *              communicate with other GridSim or Network entities.
     * @throws Exception    This happens when one of the input parameters are
     *                      invalid.
     */
    public DataRegionalGIS(String name, Link link) throws Exception {
        super(name, link);
        rcList_ = new ArrayList();
        globalRCList_ = null;
        userList_ = null;
        numRC_ = -1;
    }

    /**
     * Processes an incoming request related to a RC inquiry.
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    protected void processOtherEvent(Sim_event ev) {

        super.processOtherEvent(ev);
        switch (ev.get_tag()) {

            case DataGridTags.REGISTER_REPLICA_CTLG:
                processRegisterRC(ev);
                break;

            // receives a request about global RCs from a user or resource
            case DataGridTags.INQUIRY_GLOBAL_RC_LIST:
                processInquiryGlobalRC(ev);
                break;

            // receives a request about local RCs from a user or resource
            case DataGridTags.INQUIRY_LOCAL_RC_LIST:
                processInquiryLocalRC(ev);
                break;

            // receives a request about local RCs from other GIS entity
            case DataGridTags.INQUIRY_RC_LIST:
                processRequest(ev);
                break;

            // receives a result of list of RCs from other GIS entity
            case DataGridTags.INQUIRY_RC_RESULT:
                processResult(ev);
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
        super.processEndSimulation();
        rcList_.clear();

        if (globalRCList_ != null) {
            globalRCList_.clear();
        }
    }

    /**
     * Registers a RC to this entity
     * @param ev    a Sim_event object (or an incoming event or request)
     */
    private void processRegisterRC(Sim_event ev) {
        boolean result = super.storeRegistrationID(ev, rcList_);

        if (result) {
            super.notifySystemGIS(ev, ev.get_tag());
        } else {
            System.out.println(super.get_name() +
                ".processRegisterRC(): Warning - can't register a " +
                "Replica Catalogue ID.");
        }
    }

    /**
     * Process an incoming request from users about getting a list of RC
     * IDs, that are registered in other regional data GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processInquiryGlobalRC(Sim_event ev) {

        LinkedList regionalList = null; // regional data GIS list
        int eventTag = DataGridTags.INQUIRY_RC_LIST;
        boolean result = false;

        // for a first time request, it needs to call system GIS then
        // asks individual regional data GIS for its RC IDs.
        if (globalRCList_ == null) {

            // get regional GIS list from system GIS first
            regionalList = super.requestFromSystemGIS();

            // ask the list from each regional GIS
            result = super.getListFromOtherRegional(regionalList, eventTag);
            if (result)
            {
                globalRCList_ = new ArrayList();    // storing global RCs
                numRC_ = regionalList.size() - 1;   // excluding itself

                // then store the user ID
                Integer id = (Integer) ev.get_data();
                userList_ = new ArrayList();
                userList_.add(id);
                return;     // then exit
            }
        }

        // cache the request and store the user ID if it is already sent
        if (numRC_ > 0 && userList_ != null && userList_.size() > 0)
        {
            Integer id = (Integer) ev.get_data();
            userList_.add(id);
            return;     // then exit
        }

        // send the result back to sender, where the list could be empty
        result = sendListToSender(ev, globalRCList_);
        if (!result) {
            System.out.println(super.get_name() +
                ".processInquiryGlobalRC(): Warning - unable to send a " +
                "list of global RCs to sender.");
        }
    }

    /**
     * Process an incoming request about getting a list of local RC entity IDs
     * that are registered to this regional Data GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processInquiryLocalRC(Sim_event ev) {

        /*****   // Debug info
        Integer id = (Integer) ev.get_data();
        System.out.println(super.get_name() + ".processInquiryLocalRC():" +
            " request from " + GridSim.getEntityName(id.intValue()) +
            " for list = " + rcList_ + " tag = " + ev.get_tag());
        *******/

        boolean result = sendListToSender(ev, rcList_);
        if (result == false) {
            System.out.println(super.get_name() +
                ".processInquiryLocalRC(): Warning - unable to send a " +
                "list of local RCs to sender.");
        }
    }

    /**
     * Sends a given list to sender
     * @param ev    a Sim_event object
     * @param list  a list to be sent to
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private boolean sendListToSender(Sim_event ev, List list) {
        if (ev == null) {
            return false;
        }

        boolean result = false;
        Object obj = ev.get_data();

        if (obj instanceof Integer) {
            Integer id = (Integer) obj;
            result = super.sendListToSender(id.intValue(), ev.get_tag(), list);
        }

        return result;
    }

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of RC IDs, that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processRequest(Sim_event ev)
    {
        if (ev == null || ev.get_data() == null) {
            return;
        }

        Integer id = (Integer) ev.get_data();
        int tag = DataGridTags.INQUIRY_RC_RESULT;

        /*****   // Debug info
        System.out.println(super.get_name() + ".processRequest():" +
            " request from " + GridSim.getEntityName(id.intValue()) +
            " for list = " + rcList_ + " tag = " + ev.get_tag());
        *****/

        boolean result = super.sendListToSender(id.intValue(), tag, rcList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processRequest(): Warning - unable to send a list " +
                "of local RCs to sender.");
        }
    }

    /**
     * Process an incoming delivery from other GIS entities about their
     * RC list. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing RC IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     */
    private void processResult(Sim_event ev)
    {
        try
        {
            List list = (List) ev.get_data();
            globalRCList_.addAll(list);
            numRC_--;

            /*****   // Debug info
            System.out.println();
            System.out.println(super.get_name() + " ... EMPTY tag = " +
                ev.get_tag() + " counter = " + numRC_);
            System.out.println(super.get_name()+" ... list = "+globalRCList_);
            *****/

            // send back the result to user(s)
            if (numRC_ == 0)
            {
                numRC_ = -1;
                sendBackResult(globalRCList_,
                               DataGridTags.INQUIRY_GLOBAL_RC_LIST, userList_);
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ": Error - expected to send List object in ev.get_data()");
        }
    }

    /**
     * Sends the result back to sender
     * @param list  a List object containing resource IDs
     * @param tag   a return tag name
     * @param userList  a list of user IDs
     */
    private void sendBackResult(List list, int tag, ArrayList userList)
    {
        if (userList == null) {
            return;
        }

        // send back the result to each user in the list
        Iterator it = userList.iterator();
        while ( it.hasNext() )
        {
            Integer id = (Integer) it.next();
            super.sendListToSender(id.intValue(), tag, list);
        }

        userList.clear();   // then clear up the list
    }

} 

