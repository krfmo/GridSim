/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */


package gridsim;

import java.util.Iterator;


/**
 * GridSim Machine class represents an uniprocessor or shared memory
 * multiprocessor machine. It can contain one or more Processing Elements (PEs).
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class Machine
{
    // |PEs| > 1 is SMP (Shared Memory Multiprocessors)
    private PEList PEList_;
    private int id_;

    // tells whether this machine is working properly or has failed.
    private boolean failed_;

    /**
     * Allocates a new Machine object
     * @param id    the machine ID
     * @param numPE the number of PEs in this machine
     * @param ratingPE the rating in MIPS of a resource in this machine
     * @since 5.0
     * @pre id > 0
     * @pre numPE > 0
     * @pre ratingPE > 0
     * @post $none
     */
    public Machine(int id, int numPE, int ratingPE) {
        this.id_ = id;
        failed_ = false;
        
        PEList_ = new PEList();
        for(int peId=0; peId<numPE; peId++) {
        	PEList_.add(new PE(peId, ratingPE));
        }
    }

    /**
     * Allocates a new Machine object
     * @param id    the machine ID
     * @param list  list of PEs
     * @deprecated as of GridSim version 5.0, you should 
     * 	use {@link #Machine(int,int,int)}
     * @pre id > 0
     * @pre list != null
     * @post $none
     */
    public Machine(int id, PEList list)
    {
        this.id_ = id;
        this.PEList_ = list;
        failed_ = false;
    }

    /**
     * Gets the machine ID
     * @return the machine ID
     * @pre $none
     * @post $result > 0
     */
    public int getMachineID() {
        return id_;
    }

    /**
     * Gets the number of PEs
     * @return the number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getSize() {
        return PEList_.size();
    }

    /**
     * Gets the linked-list of all PEs
     * @return the linked-list of all PEs
     * @see gridsim.PEList
     * @pre $none
     * @post $result != null
     */
    public PEList getPEList() {
        return PEList_;
    }

    /**
     * Gets the Millions Instruction Per Second (MIPS) Rating.
     * However, for Shared Memory Multiprocessors (SMPs), it is is
     * generally assumed that all PEs have the same rating.
     * @return the sum of MIPS rating of all PEs in a machine.
     * @pre $none
     * @post $result >= 0
     */
    public int getMIPSRating()
    {
        int rating = 0;
        PE obj = null;

        // a loop that adds all the PE's MIPS rating
        Iterator<PE> it = PEList_.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();
            rating += obj.getMIPSRating();
        }

        return rating;
    }

    /**
     * Sets the particular PE status on this Machine
     * @param status   PE status, either <tt>PE.FREE</tt> or <tt>PE.BUSY</tt>
     * @param peID     PE id
     * @return <tt>true</tt> if the PE status has changed, <tt>false</tt>
     * otherwise (PE id might not be exist)
     * @pre peID >= 0
     * @post $none
     */
    public boolean setStatusPE(boolean status, int peID) {
        return PEList_.setStatusPE(status, peID);
    }

    /**
     * Gets the number of PE for this Machine
     * @return number of PE
     * @pre $none
     * @post $result >= 0
     */
    public int getNumPE() {
        return PEList_.size();
    }

    /**
     * Gets the number of <tt>FREE</tt> or non-busy PE for this Machine
     * @return number of PE
     * @pre $none
     * @post $result >= 0
     */
    public int getNumFreePE(){
        return PEList_.getNumFreePE();
    }

    /**
     * Gets the number of <tt>BUSY</tt> PE for this Machine
     * @return number of PE
     * @pre $none
     * @post $result >= 0
     */
    public int getNumBusyPE() {
        return PEList_.getNumBusyPE();
    }

    /**
     * Gets the byte size of this class
     * @return the byte size
     * @pre $none
     * @post $result > 0
     */
    public int getByteSize()
    {
        int totalInt = 4;
        return totalInt + PEList_.getByteSize();
    }

    /**
     * Sets the PEs of this machine to a FAILED status.
     * NOTE: <tt>resName</tt> is used for debugging purposes,
     * which is <b>ON</b> by default.
     * Use {@link #setFailed(boolean)} if you do not want
     * this information.
     *
     * @param resName   the name of the resource
     * @param fail      true if this machine fails or false otherwise
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFailed(String resName, boolean fail)
    {
        // all the PEs are failed (or recovered, depending on fail)
        failed_ = fail;
        PEList_.setStatusFailed(resName, id_, failed_);
        return true;
    }

    /**
     * Sets the PEs of this machine to a FAILED status.
     * @param fail   true if this machine fails or false otherwise
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    public boolean setFailed(boolean fail)
    {
        // all the PEs are failed (or recovered, depending on fail)
        failed_ = fail;
        PEList_.setStatusFailed(failed_);
        return true;
    }

    /**
     * Checks whether this machine is failed or not.
     * @return <tt>true</tt> if this machine is failed, <tt>false</tt> otherwise
     */
    public boolean getFailed() {
        return failed_;
    }

} 

