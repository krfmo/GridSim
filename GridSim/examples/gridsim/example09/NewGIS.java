package gridsim.example09;

/*
 * Author Anthony Sulistio
 * Date: May 2004
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              This example shows how to create and to define your own
 *              GridResource and GridInformationService entity.
 *
 * NOTE: GridSim version 3.0 or above is needed to run this example.
 * $Id: NewGIS.java,v 1.3 2004/05/29 07:44:30 anthony Exp $
 */

import gridsim.*;
import eduni.simjava.*;   // Need to include SimJava libraries 


/**
 * A new GridInformationService (GIS) entity.
 * Although GridSim has its own GIS entity, you might want to add new
 * functionalities into the entity. Here are the steps needed without
 * modifying the existing GIS entity:
 * - creates a new class that inherits from gridsim.GridInformationService class
 * - overrides processOtherEvent() method for processing new tags.
 *   NOTE: make sure that the tag values are not the same as the existing
 *         GridSim tags since this method will be called last.
 *
 * To execute or run this entity:
 * - inside main() method, use 
 *   GridSim.init(int numUser, Calendar obj, boolean traceFlag, boolean gis)
 *   with gis is set to false.
 *   NOTE: gis parameter is set to true means use a default or existing GIS
 *         entity rather than your own entity.
 * - inside main() method, create an object of this entity, e.g.
 *   NewGIS gisEntity = new NewGIS("NewGIS");
 *
 * - inside main() method, use GridSim.setGIS(gisEntity) to store your GIS
 *   entity. NOTE: this method should be call first before running the
 *   simulation, i.e. before calling GridSim.startGridSimulation() method.
 */
public class NewGIS extends GridInformationService
{
    /** Creates a new GIS entity */
    public NewGIS(String name) throws Exception
    {
        super(name, GridSimTags.DEFAULT_BAUD_RATE);
    }
    
    /** Overrides this method to implement new tags or functionalities. 
     * NOTE: The communcation to/from GIS entity to other entities must
     *       be done via I/O port. For more information, pls have a look
     *       at gridsim.GridSimCore API.
     */
    protected void processOtherEvent(Sim_event ev)
    {
        int resID = 0;          // sender ID
        String name = null;     // sender name

        switch ( ev.get_tag() )
        {
            case Example9.HELLO:
                resID = ( (Integer) ev.get_data() ).intValue();
                name = GridSim.getEntityName(resID);
                System.out.println(super.get_name() + 
                        ": Received HELLO tag from " + name +
                        " at time " + GridSim.clock());
                break;
                
            case Example9.TEST:
                resID = ( (Integer) ev.get_data() ).intValue();
                name = GridSim.getEntityName(resID);
                System.out.println(super.get_name() + 
                        ": Received TEST tag from " + name + 
                        " at time " + GridSim.clock());
                break;

            default:
                break;
        }

    }

} // end class


