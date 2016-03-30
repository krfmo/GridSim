package gridsim.example08;



import gridsim.*;
import eduni.simjava.*;

/**
 * This class must extend AllocPolicy class and implements
 * 5 abstract methods. In this example, I will show you how to respond to
 * a new Gridlet submitted in this GridResource
 */
class NewPolicy extends AllocPolicy
{
    /**
     * A constructor that contains resource Name and this entity Name
     */
    public NewPolicy(String resourceName, String entityName) throws Exception
    {
        // must pass back to parent class
        super(resourceName, entityName);
        System.out.println("Creating " + entityName);
    }

    public void gridletCancel(int gridletId, int userId)
    {
        // ... your code to implement this functionality
    }

    public void gridletResume(int gridletId, int userId, boolean ack)
    {
        // ... your code to implement this functionality
    }

    public void gridletPause(int gridletId, int userId, boolean ack)
    {
        // ... your code to implement this functionality
    }

    public void gridletMove(int gridletId, int userId, int destId, boolean ack)
    {
        // ... your code to implement this functionality
    }

    public int gridletStatus(int gridletId, int userId)
    {
        // ... your code to implement this functionality
        return 1;
    }

    /**
     * If a Gridlet comes in, then change the status into SUCCESS.
     * Then send an ack if required. Then send the Gridlet object back
     * to sender.
     */
    public void gridletSubmit(Gridlet gl, boolean ack)
    {
        System.out.println();
        System.out.print("NewPolicy.gridletSubmit(): it works .....");
        System.out.println("receiving Gridlet #" + gl.getGridletID() );

        try {
            gl.setGridletStatus(Gridlet.SUCCESS);
        }
        catch (Exception e) {
            // ... ignore
        }

        // If an ack is required
        if (ack == true)
        {
            System.out.println("NewPolicy.gridletSubmit(): sends back " +
                               "an acknowledgement.");

            // sends an ack that this operation has been completed
            // successfully
            super.sendAck(GridSimTags.GRIDLET_SUBMIT_ACK, true,
                          gl.getGridletID(), gl.getUserID() );
        }

        System.out.println("NewPolicy.gridletSubmit(): sends back " +
                           "Gridlet #" + gl.getGridletID() + " to User #" +
                           gl.getUserID() );

        // Sends back the Gridlet object to its user or owner
        super.sendFinishGridlet(gl);
    }


    /**
     * This main purpose of this method is to handle internal events, i.e.
     * events that are sent to the same entity itself. It mainly acts as a
     * time keeper since GridSim is a discreet-event simulation.
     */
    public void body()
    {
        // a loop that is looking for internal events only
        Sim_event ev = new Sim_event();
        while ( Sim_system.running() )
        {
            super.sim_get_next(ev);

            // if the simulation finishes then exit the loop
            if (ev.get_tag() == GridSimTags.END_OF_SIMULATION ||
                super.isEndSimulation() == true)
            {
                break;
            }

        }

        // CHECK for ANY INTERNAL EVENTS WAITING TO BE PROCESSED
        while (super.sim_waiting() > 0)
        {
            // wait for event and ignore since it is likely to be related to
            // internal event scheduled to update Gridlets processing
            super.sim_get_next(ev);
            System.out.println(super.resName_ +
                               ".NewPolicy.body(): ignore internal events");
        }
    }

} // end class

