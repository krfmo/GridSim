/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 */

package gridsim.parallel;

import java.util.LinkedList;

import gridsim.Machine;
import gridsim.MachineList;
import gridsim.ResourceCharacteristics;
import gridsim.parallel.profile.PERange;
import gridsim.parallel.profile.PERangeList;

/**
 * GridSim {@link ResourceDynamics} class represents static 
 * properties of a resource such as resource architecture, Operating 
 * System (OS), management policy (time-shared, space-shared, 
 * parallel-space-shared and parallel-space-shared with advance reservations), 
 * cost and time zone at which the resource is located along resource 
 * configuration.
 * <p>
 * <b>NOTE:</b> This class has been created to maintain the compatibility
 * with the GridSim model in which the {@link ResourceCharacteristics} 
 * implements methods that return the number of PEs available, 
 * among other information.
 *
 * @author  Marcos Dias de Assuncao
 * @since 	5.0
 */
public class ResourceDynamics extends ResourceCharacteristics {
	private PERangeList freePEs;		// ranges of PEs available
	private int numPE;					// number of PEs in the resource
	
    /**
     * Allocates a new {@link ResourceDynamics} object.
     * If the time zone is invalid, then by default, it will be GMT+0.
     * @param architecture  the architecture of a resource
     * @param os            the operating system used
     * @param machineList   list of machines in a resource
     * @param allocationPolicy     the resource allocation policy
     * @param timeZone   local time zone where the resource is.
     *                   Time zone should be of range [GMT-12 ... GMT+13]
     * @param costPerSec    the cost per sec to use this resource
     * @pre architecture != null
     * @pre OS != null
     * @pre machineList != null
     * @pre allocationPolicy >= 0 && allocationPolicy <= 3
     * @pre timeZone >= -12 && timeZone <= 13
     * @pre costPerSec >= 0.0
     * @post $none
     */
    public ResourceDynamics(String architecture, String os,
                MachineList machineList, int allocationPolicy,
                double timeZone, double costPerSec) {
    	super(architecture, os, machineList, allocationPolicy,timeZone, costPerSec);
    	resetFreePERanges();
    	numPE = machineList.getNumPE();
    }

    /**
     * Allocates a new {@link ResourceDynamics} object.
     * @param ch the original resource characteristics object.
     * @pre ch != null
     */
    public ResourceDynamics(ResourceCharacteristics ch) {
    	this(ch.getResourceArch(), ch.getResourceOS(), 
    			ch.getMachineList(), ch.getResourceAllocationPolicy(),
    			ch.getResourceTimeZone(), ch.getCostPerSec());
    }
    
    /**
     * This method resets the ranges of PEs available. That is, it sets all
     * processing elements as available.
     */
    public void resetFreePERanges() {
		freePEs = new PERangeList();
		freePEs.add(new PERange(0, super.getNumPE() - 1));
    }
    
    // -------------------------- PUBLIC METHODS -----------------------
    
    /**
     * Gets a Machine with at least one empty PE<br>
     * <b>NOTE:</b> Not supported.
     * @return a Machine object or <code>null</code> if not found
     */
    @Override
    public Machine getMachineWithFreePE() {
    	throw new UnsupportedOperationException("This class uses ranges of PEs, " +
    			"thus it is not possible to determine which machine has PEs " +
    			"available at the moment");
    }
    
    /**
     * Returns a list of machines with free PEs.<br>
     * <b>NOTE:</b> Not supported.
     * @return the list of machines with free PEs.
     */
    public LinkedList<Machine> getListMachineWithFreePE() {
    	throw new UnsupportedOperationException("This class uses ranges of PEs, " +
    			"thus it is not possible to determine which machines have " +
    			"PEs available at the moment");
    }

    /**
     * Gets a Machine with at least a given number of free PE<br>
     * <b>NOTE:</b> Not supported.
     * @param numPE number of PE
     * @return a Machine object or <code>null</code> if not found
     */
    @Override
    public Machine getMachineWithFreePE(int numPE) {
    	throw new UnsupportedOperationException("This class uses ranges of PEs, " +
    			"thus it is not possible to determine which machine has PEs " +
    			"available at the moment");
    }

    /**
     * Gets the total MIPS rating, which is the sum of MIPS rating of all
     * machines in a resource.
     * @return the sum of MIPS ratings
     */
    @Override
    public int getMIPSRating() {
    	return getMIPSRatingOfOnePE() * getNumPE();
    }

    /**
     * Gets the total number of PEs for all Machines
     * @return number of PEs
     */
    @Override
    public int getNumPE() {
        return numPE;
    }
    
    /**
     * Gets the total number of <code>FREE</code> or non-busy PEs for all Machines
     * @return number of PEs
     */
    @Override
    public int getNumFreePE() {
    	return (freePEs == null) ? 0 : freePEs.getNumPE();
    }

    /**
     * Gets the total number of <code>BUSY</code> PEs for all Machines
     * @return number of PEs
     */
    @Override
    public int getNumBusyPE() {
        return numPE - getNumFreePE();
    }
    
    /**
     * Sets the status of a list of ranges of PEs to busy
     * @param ranges the list of ranges of PEs whose status has to be changed
     */
    public void setPEsBusy(PERangeList ranges) {
    	freePEs.remove(ranges);
    }
    
    /**
     * Sets the status of a list of ranges of PEs to available
     * @param ranges the list of ranges of PEs whose status has to be changed
     */
    public void setPEsAvailable(PERangeList ranges) {
    	if(freePEs == null) {
    		freePEs = new PERangeList();
    	}
    	freePEs.addAll(ranges.clone());
    	freePEs.mergePERanges();
    }
    
    /**
     * Resets the ranges of PEs available and sets it to the list provided
     * @param ranges the list of ranges available
     */
    public void resetFreePERanges(PERangeList ranges) {
    	// clones the list to avoid conflict
    	freePEs = (ranges == null) ? null : ranges.clone(); 
    }
    
    /**
     * Returns the list of ranges of PEs available at the 
     * current simulation time
     * @return a list of ranges of PEs available at the current 
     * simulation time
     */
    public PERangeList getFreePERanges() {
    	return freePEs;
    }

    /**
     * Sets the particular PE status on a Machine<br>
     * <b>NOTE:</b> Not supported.
     * @param status   PE status, either <code>PE.FREE</code> or <code>PE.BUSY</code>
     * @param machineID    Machine ID
     * @param peID     PE id
     * @return <code>true</code> if the PE status has changed, <code>false</code>
     * otherwise (Machine id or PE id might not be exist)
     */
    @Override
    public boolean setStatusPE(boolean status, int machineID, int peID) {
    	throw new UnsupportedOperationException("This class uses ranges of PEs, " +
    			"thus it is not possible to change the status of a particular PE.");
    }
} 

