package gridsim.example09;

/*
 * Author Anthony Sulistio
 * Date: May 2004
 * Description: A simple program to demonstrate of how to use GridSim package.
 *              This example shows how to create and to define your own
 *              GridResource and GridInformationService entity.
 *
 * NOTE: GridSim version 3.0 or above is needed to run this example.
 * $Id: NewGridResource.java,v 1.2 2004/05/29 07:44:47 anthony Exp $
 */


import gridsim.*;
import eduni.simjava.*;   // Need to include SimJava libraries


/**
 * Creates a new GridResource entity. This class does a simple functionality
 * by register new tags to GridInformationService (GIS) entity first.
 * Then upon receiving a new tag from sender, this class is simply prints out
 * a message saying the tag have been received.
 *
 * Although GridSim has its own GridResource entity, you might want to add new
 * functionalities into the entity. Here are the steps needed without
 * modifying the existing GridResource entity:
 * - creates a new class that inherits from gridsim.GridResource class
 * - overrides registerOtherEntity() method for registering new tags to GIS
 *   entity.
 * - overrides processOtherEvent() method for processing new tags from
 *   other entities.
 *
 * NOTE: make sure that the tag values are not the same as the existing
 *       GridSim tags since this method will be called last.
 */
public class NewGridResource extends GridResource
{
    /**
     * Creates a new GridResource entity. There are different ways to call
     * a parent's constructor. In this example, we choose only one method for
     * simplicity.
     */
    public NewGridResource(String name, double baud_rate,
            ResourceCharacteristics resource, ResourceCalendar calendar,
            ARPolicy policy) throws Exception
    {
        super(name, baud_rate, resource, calendar, policy);
    }

    /**
     * Overrides this method to implement new tags or functionalities. 
     * NOTE: The communcation to/from GridResource entity to other entities must
     *       be done via I/O port. For more information, pls have a look
     *       at gridsim.GridSimCore API.
     */
    protected void processOtherEvent(Sim_event ev)
    {
        try 
        {
            // get the sender ID.
            // NOTE: Sim_event.get_data() carries a generic object. It might
            //       carries Gridlet, String, etc depending on the sender.
            //       Therefore, be careful when casting the correct type of
            //       the object. In this example, the sender is expected to
            //       send an Integer object.
            Integer obj = (Integer) ev.get_data();

            // get the sender name
            String name = GridSim.getEntityName( obj.intValue() );
            switch ( ev.get_tag() )
            {
                case Example9.HELLO:
                    System.out.println(super.get_name() 
                            + ": received HELLO tag from " + name +
                            " at time " + GridSim.clock());
                    break;
                
                case Example9.TEST:
                    System.out.println(super.get_name() 
                            + ": received TEST tag from " + name + 
                            " at time " + GridSim.clock());
                    break;

                default:
                    break;
            }
        }
        catch (ClassCastException c) {
            System.out.println(super.get_name() + 
                    ".processOtherEvent(): Exception occurs.");
        }

    }

    /**
     * Overrides this method to register new tags to GridInformationService
     * (GIS) entity. You need to create a new GIS entity to be able to handle
     * your new tags.
     * NOTE: The communcation to/from GridResource entity to other entities must
     *       be done via I/O port. For more information, pls have a look
     *       at gridsim.GridSimCore API.
     */
    protected void registerOtherEntity()
    {
        int SIZE = 12;  // size of Integer object incl. overhead

        // get the GIS entity ID
        int gisID = GridSim.getGridInfoServiceEntityId();

        // get the GIS entity name
        String gisName = GridSim.getEntityName(gisID);
        
        // register HELLO tag to the GIS entity
        System.out.println(super.get_name() + ".registherOtherEntity(): " +
                "register HELLO tag to " + gisName + 
                " at time " + GridSim.clock());
        
        super.send(super.output, GridSimTags.SCHEDULE_NOW, Example9.HELLO,
                   new IO_data(new Integer(super.get_id()), SIZE, gisID) );

        // register TEST tag to the GIS entity
        System.out.println(super.get_name() + ".registherOtherEntity(): " +
                "register TEST tag to " + gisName +
                " at time " + GridSim.clock());
        
        super.send(super.output, GridSimTags.SCHEDULE_NOW, Example9.TEST,
                   new IO_data(new Integer(super.get_id()), SIZE, gisID) );

    }

} // end class

