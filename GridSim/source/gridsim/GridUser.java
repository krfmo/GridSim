/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim;

import java.util.List;

import eduni.simjava.Sim_event;
import eduni.simjava.Sim_type_p;
import gridsim.index.AbstractGIS;
import gridsim.net.Link;


/**
 * GridUser class acts as a medium to communicate with
 * {@link gridsim.index.AbstractGIS} entity. For more details on how to use
 * this class, please look at examples/RegionalGIS directory.
 *
 * @author       Anthony Sulistio
 * @since        GridSim Toolkit 3.2
 * @invariant $none
 */
public class GridUser extends GridSim
{
    private String gisName_;  // local GIS entity name

    /** A regional GIS entity ID that is needed for communication */
    protected int gisID_;


    /**
     * Creates a GridUser object
     * @param name  this entity name
     * @param link  a network link connecting this entity
     * @throws java.lang.Exception happens if either name or link is empty
     * @pre name != null
     * @pre link != null
     * @post $none
     */
    public GridUser(String name, Link link) throws Exception
    {
        super(name, link);
        gisID_ = -1;
        gisName_ = null;
    }

    /**
     * Creates a GridUser object
     * @param name  this entity name
     * @param link  a network link connecting this entity
     * @param regionalGIS  a regional GridInformationService (GIS) entity name
     * @throws java.lang.Exception happens if one of the inputs is empty
     * @pre name != null
     * @pre link != null
     * @pre regionalGIS != null
     * @post $none
     */
    public GridUser(String name, Link link, String regionalGIS)
                    throws Exception
    {
        super(name, link);

        // check for validity
        gisID_ = GridSim.getEntityId(regionalGIS);
        if (regionalGIS == null || gisID_ == -1) {
            throw new Exception(name + " : Error - invalid regional GIS name");
        }
        gisName_ = regionalGIS;
    }

    /**
     * Gets the regional GIS entity name
     * @return  regional GIS entity name or <tt>null</tt> if GIS does not exist
     * @pre $none
     * @post $none
     */
    public String getRegionalGISName()
    {
        if (gisID_ == -1) {
            return null;
        }

        return gisName_;
    }

    /**
     * Tells the GridSim that this entity finishes its simulation / experiment.
     * @pre $none
     * @post $none
     */
    public void finishSimulation() {
        finishSimulation(0.0);
    }

    /**
     * Tells the GridSim that this entity finishes its simulation / experiment.
     * @param time  expected finish time
     * @pre time > 0
     * @post $none
     */
    public void finishSimulation(double time)
    {
        if (time > 0) {
            super.sim_pause(time);
        }
        super.shutdownUserEntity();
        super.terminateIOEntities();
    }

    /**
     * Gets the regional GIS entity ID
     * @return regional GIS entity ID or <tt>-1</tt> if GIS does not exist
     * @pre $none
     * @post $none
     */
    public int getRegionalGISId() {
        return gisID_;
    }

    /**
     * Sets a regional GIS entity name for this entity to communicate with
     * @param gisName   a regional GIS entity name
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre gisName != null
     * @post $none
     */
    public boolean setRegionalGIS(String gisName)
    {
        int id = GridSim.getEntityId(gisName);
        if (gisName == null || id == -1) {
            return false;
        }

        gisName_ = gisName;
        gisID_ = id;
        return true;
    }

    /**
     * Sets a regional GIS for this entity to communicate with
     * @param gis   a regional GIS entity
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre gis != null
     * @post $none
     */
    public boolean setRegionalGIS(AbstractGIS gis)
    {
        if (gis == null) {
            return false;
        }

        return setRegionalGIS( gis.get_name() );
    }

    /**
     * Gets a list of all regional GIS entity IDs
     * @return a list of all regional GIS IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if GIS entities do not exist
     * @pre $none
     * @post $none
     */
    public Object[] getRegionalGISList() {
        return this.getList(AbstractGIS.INQUIRY_REGIONAL_GIS);
    }

    /**
     * Gets a list of local resources that support advance reservation
     * from a regional GIS entity, defined in {@link #gisID_}.
     * @return a list of local resource IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if local resources do not exist.
     * @pre $none
     * @post $none
     */
    public Object[] getLocalResourceARList() {
        return this.getList(AbstractGIS.INQUIRY_LOCAL_RESOURCE_AR_LIST);
    }

    /**
     * Gets a list of global resources that support advance reservation.
     * Global resource means a resource that is registered to other
     * regional GIS entities.
     * @return a list of local resource IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if global resources do not exist.
     * @pre $none
     * @post $none
     */
    public Object[] getGlobalResourceARList() {
        return this.getList(AbstractGIS.INQUIRY_GLOBAL_RESOURCE_AR_LIST);
    }

    /**
     * Gets a list of local resources from a regional GIS entity, defined
     * in {@link #gisID_}.
     * @return a list of local resource IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if local resources do not exist.
     * @pre $none
     * @post $none
     */
    public Object[] getLocalResourceList() {
        return this.getList(AbstractGIS.INQUIRY_LOCAL_RESOURCE_LIST);
    }

    /**
     * Gets a list of global resources.
     * Global resource means a resource that is registered to other
     * regional GIS entities.
     *
     * @return a list of local resource IDs in <tt>Integer</tt> object
     *         or <tt>null</tt> if global resources do not exist.
     * @pre $none
     * @post $none
     */
    public Object[] getGlobalResourceList() {
        return this.getList(AbstractGIS.INQUIRY_GLOBAL_RESOURCE_LIST);
    }

    /**
     * Gets a list of other request to the regional GIS entity as defined in
     * {@link #gisID_}. Type of request is defined in the tag name.
     * @param eventTag  an event tag name or type of request
     * @return a list of this request or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    protected Object[] getList(int eventTag) {
        return this.getList(eventTag, gisID_);
    }

    /**
     * Gets a list of other request to the given regional GIS entity ID.
     * Type of request is defined in the tag name.
     * @param eventTag  an event tag name or type of request
     * @param otherRegionalID   a regional GIS or destination ID
     * @return a list of this request or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    protected Object[] getList(int eventTag, int otherRegionalID)
    {
        Object[] array = null;
        if (otherRegionalID == -1)
        {
            System.out.println(super.get_name() +
                ".getList(): Error - destination id = " + otherRegionalID);
            return array;
        }

        Integer id = new Integer( super.get_id() );
        super.send(super.output, 0.0, eventTag,
                   new IO_data(id, Link.DEFAULT_MTU, otherRegionalID) );

        // waiting for a response from the regional GIS
        Sim_type_p tag = new Sim_type_p(eventTag);

        // only look for this type of ack
        Sim_event ev = new Sim_event();
        super.sim_get_next(tag, ev);

        try
        {
            // NOTE: make sure the receiver and sender expecting the same
            // type of object.
            List list = (List) ev.get_data();
            if (list != null) {
                array = list.toArray();
            }
        }
        catch (Exception e)
        {
            array = null;
            System.out.println(super.get_name()+".getList(): Exception error.");
        }

        return array;
    }

} 

