/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2005, The University of Melbourne, Australia
 */

package gridsim.index;

import java.util.*;
import eduni.simjava.*;
import gridsim.*;
import gridsim.net.Link;


/**
 * RegionalGIS is a simple regional GridInformationService (GIS) entity that
 * performs basic functionalities, such as storing a list of local resources,
 * and asking other regional GIS entities for resources.
 * <p>
 * If you want to implement other complex functionalities, you need to extend
 * this class and to override {@link #processOtherEvent(Sim_event)}
 * and/or {@link #registerOtherEntity()} method.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.2
 * @invariant $none
 */
public class RegionalGIS extends AbstractGIS
{
    /** This entity ID in <tt>Integer</tt> object. */
    protected Integer myID_;

    private ArrayList resList_;         // all resources within this region
    private ArrayList arList_;          // AR resources only within this region
    private ArrayList globalResList_;   // all resources outside this region
    private ArrayList globalResARList_; // AR resources only outside this region
    private LinkedList regionalList_;   // list of regional GIS, incl. myself
    private ArrayList userList_;    // list of users querying for global res
    private ArrayList userARList_;  // list of users querying for global AR res
    private int numRes_;  // counting for num of GIS entities for res request
    private int numAR_;   // counting for num of GIS entities for res AR request


    /**
     * Creates a new regional GIS entity
     * @param name  this regional GIS name
     * @param link  a network link to this entity
     * @throws Exception This happens when creating this entity before
     *                   initializing GridSim package or this entity name is
     *                   <tt>null</tt> or empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public RegionalGIS(String name, Link link) throws Exception
    {
        super(name, link);
        init();
    }

    /**
     * Initialises all attributes
     * @pre $none
     * @post $none
     */
    private void init()
    {
        myID_ = new Integer( super.get_id() );
        resList_ = new ArrayList();
        arList_ = new ArrayList();
        regionalList_ = null;
        globalResList_ = null;
        globalResARList_ = null;
        userList_ = null;
        userARList_ = null;
        numAR_ = -1;
        numRes_ = -1;
    }

    /**
     * Stores the incoming registration ID into the given list. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object.
     *
     * @param ev      a new Sim_event object or incoming registration request
     * @param list    a list storing the registration IDs
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre ev != null
     * @pre list != null
     * @post $none
     */
    protected boolean storeRegistrationID(Sim_event ev, List list)
    {
        boolean result = false;
        if (ev == null || list == null) {
            return result;
        }

        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            Integer id = (Integer) obj;
            list.add(id);
            result = true;
        }

        return result;
    }

    /**
     * Process a registration request from a resource entity
     * supporting Advanced Reservation to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected void processRegisterResourceAR(Sim_event ev)
    {
        boolean result1 = storeRegistrationID(ev, arList_);
        boolean result2 = storeRegistrationID(ev, resList_);

        if (result1 == false || result2 == false)
        {
            System.out.println(super.get_name() +
                ".processRegisterResourceAR(): Warning - can't register " +
                "a resource ID.");
        }
    }

    /**
     * Process a registration request from a resource entity to this regional
     * GIS entity. <br>
     * NOTE: <tt>ev.get_data()</tt> should contain an <tt>Integer</tt> object
     * representing the resource ID.
     *
     * @param ev  a Sim_event object (or a registration request)
     * @pre ev != null
     * @post $none
     */
    protected void processRegisterResource(Sim_event ev)
    {
        boolean result = storeRegistrationID(ev, resList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processRegisterResource(): Warning - can't register " +
                "a resource ID.");
        }
    }

    /**
     * Process an incoming request that uses a user-defined tag. <br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev) {
        // empty
    }

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs, that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceList(Sim_event ev)
    {
        if (ev == null || ev.get_data() == null) {
            return;
        }

        Integer id = (Integer) ev.get_data();
        int tag = AbstractGIS.GIS_INQUIRY_RESOURCE_RESULT;

        /*****   // Debug info
        System.out.println(super.get_name() + ".processGISResourceList():" +
            " request from " + GridSim.getEntityName(id.intValue()) +
            " for list = " + resList_ + " tag = " + ev.get_tag());
        *****/

        boolean result = sendListToSender(id.intValue(), tag, resList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processGISResourceList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming request from other GIS entities about getting
     * a list of resource IDs supporting Advanced Reservation,
     * that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceARList(Sim_event ev)
    {
        if (ev == null || ev.get_data() == null) {
            return;
        }

        Integer id = (Integer) ev.get_data();
        int tag = AbstractGIS.GIS_INQUIRY_RESOURCE_AR_RESULT;

        /*****   // Debug info
        System.out.println(super.get_name() + ".processGISResourceARList():" +
            " request from " + GridSim.getEntityName(id.intValue()) +
            " for list = " + resList_ + " tag = " + ev.get_tag());
        *****/

        boolean result = sendListToSender(id.intValue(), tag, arList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processGISResourceARList(): Warning - unable to send a " +
                "list of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list supporting Advanced Reservation. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceARResult(Sim_event ev)
    {
        try
        {
            List list = (List) ev.get_data();    // get the data
            globalResARList_.addAll(list);       // add the result into a list
            numAR_--;       // decrement the counter for GIS entity

            /*****   // Debug info
            System.out.println();
            System.out.println(super.get_name() + " ... AR result tag = " +
                ev.get_tag() + " counter = " + numAR_);
            System.out.println(super.get_name() + " ... AR list = " +
                globalResARList_);
            *****/

            // send back the result to user(s)
            if (numAR_ == 0)
            {
                numAR_ = -1;
                sendBackResult(globalResARList_,
                    AbstractGIS.INQUIRY_GLOBAL_RESOURCE_AR_LIST, userARList_);
            }
        }
        catch (Exception e)
        {
            System.out.println(super.get_name() +
                ": Error - expected to send List object in ev.get_data()");
        }
    }

    /**
     * Process an incoming delivery from other GIS entities about their
     * resource list. <br>
     * NOTE: ev.get_data() should contain <tt>List</tt> containing resource IDs
     * (in <tt>Integer</tt> object).
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGISResourceResult(Sim_event ev)
    {
        try
        {
            List list = (List) ev.get_data();
            globalResList_.addAll(list);
            numRes_--;

            /*****   // Debug info
            System.out.println();
            System.out.println(super.get_name() + " ... EMPTY tag = " +
                ev.get_tag() + " counter = " + numRes_);
            System.out.println(super.get_name()+" ... list = "+globalResList_);
            *****/

            // send back the result to user(s)
            if (numRes_ == 0)
            {
                numRes_ = -1;
                sendBackResult(globalResList_,
                    AbstractGIS.INQUIRY_GLOBAL_RESOURCE_LIST, userList_);
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
     *
     * @pre userList != null
     * @post $none
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
            sendListToSender(id.intValue(), tag, list);
        }

        userList.clear();   // then clear up the list
    }

    /**
     * Process an incoming request about getting a list of regional GIS IDs
     * (including this entity ID), that are registered to the
     * {@link gridsim.GridInformationService} or system GIS.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processInquiryRegionalGIS(Sim_event ev)
    {
        // get regional GIS list from system GIS
        LinkedList regionalList = requestFromSystemGIS();

        // then send the list to sender
        boolean result = sendListToSender(ev, regionalList);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processInquiryRegionalGIS(): Warning - unable to send a " +
                "list of regional GIS IDs to sender.");
        }
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * supporting Advanced Reservation that are registered in other regional
     * GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGlobalResourceARList(Sim_event ev)
    {
        LinkedList regionalList = null;   // regional GIS list
        int eventTag = AbstractGIS.GIS_INQUIRY_RESOURCE_AR_LIST;
        boolean result = false;

        // for a first time request, it needs to call the system GIS first,
        // then asks each regional GIS for its resource IDs.
        if (globalResARList_ == null)
        {
            // get regional GIS list from system GIS first
            regionalList = requestFromSystemGIS();

            // ask a resource list from each regional GIS
            result = getListFromOtherRegional(regionalList, eventTag);
            if (result == true)
            {
                globalResARList_ = new ArrayList();  // storing global AR
                numAR_ = regionalList.size() - 1;    // excluding GIS itself

                // then store the user ID
                Integer id = (Integer) ev.get_data();
                userARList_ = new ArrayList();
                userARList_.add(id);
                return;     // then exit
            }
        }

        // cache the request and store the user ID if it is already sent
        if (numAR_ > 0 && userARList_ != null && userARList_.size() > 0)
        {
            Integer id = (Integer) ev.get_data();
            userARList_.add(id);
            return;     // then exit
        }

        result = sendListToSender(ev, globalResARList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processGlobalResourceARList(): Warning - can't send a " +
                "resource AR list to sender.");
        }
    }

    /**
     * Process an incoming request from users about getting a list of resource
     * IDs, that are registered in other regional GIS entities.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processGlobalResourceList(Sim_event ev)
    {
        /***
        NOTE: possible cases are
        - if there is only 1 local GIS and no other regional GISes
        - if there is only 1 user queries for this
        - if there are 2 or more users query at different time
        ****/

        LinkedList regionalList = null;   // regional GIS list
        int eventTag = AbstractGIS.GIS_INQUIRY_RESOURCE_LIST;
        boolean result = false;

        // for a first time request, it needs to call the system GIS first,
        // then asks each regional GIS for its resource IDs.
        if (globalResList_ == null)
        {
            // get regional GIS list from system GIS first
            regionalList = requestFromSystemGIS();

            // ask a resource list from each regional GIS
            result = getListFromOtherRegional(regionalList, eventTag);
            if (result == true)
            {
                globalResList_ = new ArrayList();    // storing global resources
                numRes_ = regionalList.size() - 1;   // excluding itself

                // then store the user ID
                Integer id = (Integer) ev.get_data();
                userList_ = new ArrayList();
                userList_.add(id);
                return;     // then exit
            }
        }

        // cache the request and store the user ID if it is already sent
        if (numRes_ > 0 && userList_ != null && userList_.size() > 0)
        {
            Integer id = (Integer) ev.get_data();
            userList_.add(id);
            return;     // then exit
        }

        // send the result back to sender, where the list could be empty
        result = sendListToSender(ev, globalResList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processGlobalResourceList(): Warning - can't send a " +
                "resource list to sender.");
        }
    }

    /**
     * Get a list of IDs specified in the eventTag from other regional GIS
     * @param regionalList  a list of regional GIS IDs
     * @param eventTag      an event tag or type of request
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre regionalList != null
     * @post $none
     */
    protected boolean getListFromOtherRegional(List regionalList, int eventTag)
    {
        // check for input first
        if (regionalList == null || regionalList.size() == 0) {
            return false;
        }

        // a loop to ask each regional GIS for its resource IDs
        Iterator it = regionalList.iterator();
        while ( it.hasNext() )
        {
            Integer obj = (Integer) it.next();

            // can not send to itself
            if (obj.equals(myID_) == true) {
                continue;
            }

            // send a request to a regional GIS
            super.send( super.output, 0.0, eventTag,
                        new IO_data(myID_, Link.DEFAULT_MTU, obj.intValue()) );

            /******   // Debug info
            System.out.println(super.get_name()+".getListFromOtherRegional(): "
                    + "query to " + GridSim.getEntityName( obj.intValue() )
                    + ", tag = " + eventTag);
            ******/
        }

        return true;
    }

    /**
     * Asks from {@link gridsim.GridInformationService} or system GIS about
     * a list of regional GIS entity ID.
     * @return a list of regional GIS entity ID
     * @pre $none
     * @post $none
     */
    protected LinkedList requestFromSystemGIS()
    {
        // get the regional GIS list from local cache
        if (regionalList_ != null) {
            return regionalList_;
        }
        else {
            regionalList_ = new LinkedList();
        }

        // for the first time, ask the regional GIS list from system GIS
        int eventTag = GridSimTags.REQUEST_REGIONAL_GIS;
        boolean result = requestFromSystemGIS(eventTag, regionalList_);

        return regionalList_;
    }

    /**
     * Asks from {@link gridsim.GridInformationService} or system GIS about
     * a specific event or request.
     *
     * @param eventTag  an event tag or type of request
     * @param list      a list storing the results
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre list != null
     * @post $none
     */
    protected boolean requestFromSystemGIS(int eventTag, List list)
    {
        boolean result = false;
        if (list == null) {
            return result;
        }

        // send a request to system GIS for a list of other regional GIS
        super.send( super.output, 0.0, eventTag,
                    new IO_data(myID_, Link.DEFAULT_MTU, super.systemGIS_) );

        // waiting for a response from system GIS
        Sim_type_p tag = new Sim_type_p(eventTag);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        try
        {
            List tempList = (List) ev.get_data();
            list.addAll(tempList);
            result = true;
        }
        catch (Exception e)
        {
            result = false;
            System.out.println(super.get_name() +
                    ".requestFromSystemGIS(): Exception error.");
        }

        return result;
    }

    /**
     * Sends a given list to sender
     * @param ev    a Sim_event object
     * @param list  a list to be sent to
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    private boolean sendListToSender(Sim_event ev, List list)
    {
        if (ev == null) {
            return false;
        }

        boolean result = false;
        Object obj = ev.get_data();
        if (obj instanceof Integer)
        {
            Integer id = (Integer) obj;
            result = sendListToSender(id.intValue(), ev.get_tag(), list);
        }

        return result;
    }

    /**
     * Sends a list to sender
     * @param senderID  the sender ID
     * @param tag       an event tag
     * @param list      a list to be sent to
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre senderID != -1
     * @post $none
     */
    protected boolean sendListToSender(int senderID, int tag, List list)
    {
        if (senderID < 0) {
            return false;
        }

        int length = 0;
        if (list == null || list.size() == 0) {
            length = 1;
        }
        else {
            length = list.size();
        }

        /******   // DEBUG info
        System.out.println(super.get_name()+".sendListToSender(): send list = "
            + list + ", tag = " + tag + " to "+GridSim.getEntityName(senderID));
        System.out.println();
        ******/

        // Send the event or message
        super.send( super.output, 0.0, tag,
                    new IO_data(list, Link.DEFAULT_MTU*length, senderID) );

        return true;
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * that are registered to this regional GIS entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processResourceList(Sim_event ev)
    {
        /*****   // Debug info
        Integer id = (Integer) ev.get_data();
        System.out.println(super.get_name() + ".processResourceList():" +
            " request from " + GridSim.getEntityName(id.intValue()) +
            " for list = " + resList_ + " tag = " + ev.get_tag());
        *******/

        boolean result = sendListToSender(ev, resList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processResourceList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Process an incoming request about getting a list of resource IDs
     * supporting Advanced Reservation that are registered to this regional GIS
     * entity.
     *
     * @param ev  a Sim_event object (or an incoming event or request)
     * @pre ev != null
     * @post $none
     */
    protected void processResourceARList(Sim_event ev)
    {
        boolean result = sendListToSender(ev, arList_);
        if (result == false)
        {
            System.out.println(super.get_name() +
                ".processResourceARList(): Warning - unable to send a list " +
                "of resource IDs to sender.");
        }
    }

    /**
     * Registers other information to {@link gridsim.GridInformationService} or
     * system GIS.<br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {
        // empty
    }


    /**
     * Informs the registered entities regarding to the end of a simulation.<br>
     * NOTE: This method can be overridden by its subclasses, provided
     *       that they call this method first. This is required, just in case
     *       this method is not empty.
     * @pre $none
     * @post $none
     */
    protected void processEndSimulation()
    {
        resList_.clear();
        arList_.clear();

        if (regionalList_ != null) {
            regionalList_.clear();
        }

        if (globalResList_ != null) {
            globalResList_.clear();
        }

        if (globalResARList_ != null) {
            globalResARList_.clear();
        }

        if (userList_ != null) {
            userList_.clear();
        }

        if (userARList_ != null) {
            userARList_.clear();
        }
    }

} // end class

