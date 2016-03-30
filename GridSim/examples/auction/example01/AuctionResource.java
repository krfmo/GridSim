package auction.example01;

/*
 * Author: Marcos Dias de Assuncao
 * Date: March 2006
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 */

import eduni.simjava.Sim_event;
import gridsim.AllocPolicy;
import gridsim.GridResource;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.auction.AuctionObserver;
import gridsim.auction.Responder;

/**
 * This class implements a resource that has an observer for an auction
 * @author Marcos Dias de Assuncao
 */
public class AuctionResource extends GridResource {
	private AuctionObserver observer;

    /**
     * Allocates a new GridResource object.
     *
     * @param name       the name to be associated with this entity (as
     *                   required by Sim_entity class from simjava package)
     * @param baud_rate  network communication or bandwidth speed
     * @param resource   an object of ResourceCharacteristics
     * @param calendar   an object of ResourceCalendar
     * @param policy     a scheduling policy for this Grid resource. If no
     *                   scheduling policy is defined, the default one is
     *                   <tt>SpaceShared</tt>
     * @throws Exception This happens when one of the following scenarios occur:
     *      <ul>
     *          <li> creating this entity before initializing GridSim package
     *          <li> this entity name is <tt>null</tt> or empty
     *          <li> this entity has <tt>zero</tt> number of PEs (Processing
     *              Elements). <br>
     *              No PEs mean the Gridlets can't be processed.
     *              A GridResource must contain one or more Machines.
     *              A Machine must contain one or more PEs.
     *      </ul>
     * @see gridsim.GridResource#GridResource(String, double, ResourceCharacteristics, ResourceCalendar, AllocPolicy)
     */
	AuctionResource(String name, double baud_rate,
			ResourceCharacteristics resource,
			ResourceCalendar calendar,
			AllocPolicy policy) throws Exception{

		super(name,baud_rate,resource,calendar,policy);

		Responder responder = new ResponderImpl(resource,calendar,policy);
		observer = new AuctionObserver(this.get_id(), name + "_Observer_1", this.output, responder);
	}

    /**
     * Since we are implementing other resource, this method has to be
     * implemented in order to make resource able to deal with other kind
     * of events.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(Sim_event ev){
    	observer.processEvent(ev);
    }
}