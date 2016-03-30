/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * GridSim PE (Processing Element) class represents CPU unit,
 * defined in terms of Millions Instructions Per Second (MIPS) rating.<br>
 * <b>ASSUMPTION:<b> All PEs under the same Machine have the same MIPS rating.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class PE
{
    private int id_;             // this PE id
    private int MIPSRating_;     // in SPEC MIPS or LINPACK MFLOPS

    // FOR SPACE SHARED RESOURCE: Jan 21
    private boolean status_;     // Status of PE: FREE or BUSY

    // TODO: this is obviously clash BUSY with FAILED !!! So, change from the status
    // of boolean to integer !!

    /** Denotes PE is FREE for allocation */
    public static final boolean FREE  = true;

    /** Denotes PE is allocated and hence busy in processing Gridlet */
    public static final boolean BUSY = false;

    /** Denotes PE is failed and hence it can't process any Gridlet at this moment.
     * This PE is failed because it belongs to a machine which is also failed.
     */
    public static final boolean FAILED = false;



    /**
     * Allocates a new PE object
     * @param id    the PE ID
     * @param MIPSRating    the capability of the PE. All PEs under the same
     *                      Machine have same MIPS rating.
     * @pre id >= 0
     * @pre MIPSRating >= 0
     * @post $none
     */
    public PE(int id, int MIPSRating)
    {
        this.id_ = id;
        this.MIPSRating_ = MIPSRating;

        // when created it should be set to FREE, i.e. available for use.
        this.status_ = PE.FREE;
    }

    /**
     * Gets the PE ID
     * @return the PE ID
     * @pre $none
     * @post $result >= 0
     */
    public int getID() {
        return id_;
    }

    /**
     * Sets the MIPS Rating of this PE
     * @param rating    the capability of the PE
     * @pre rating >= 0
     * @post $none
     */
    public void setMIPSRating(int rating) {
        this.MIPSRating_ = rating;
    }

    /**
     * Gets the MIPS Rating of this PE
     * @return the MIPS Rating
     * @pre $none
     * @post $result >= 0
     */
    public int getMIPSRating() {
        return MIPSRating_;
    }

    /**
     * Gets the status of this PE
     * @return the status of this PE
     * @pre $none
     * @post $none
     */
    public boolean getStatus() {
        return status_;
    }

    /**
     * Sets PE status to free, meaning it is available for processing.
     * This should be used by SPACE shared resources only.
     * @pre $none
     * @post $none
     */
    public void setStatusFree() {
        status_ = PE.FREE;
    }

    /**
     * Sets PE status to busy, meaning it is already executing Gridlets.
     * This should be used by SPACE shared resources only.
     * @pre $none
     * @post $none
     */
    public void setStatusBusy() {
        status_ = PE.BUSY;
    }

    /**
     * Sets PE status to either <tt>PE.FREE</tt> or <tt>PE.BUSY</tt>
     * @param status     PE status, <tt>true</tt> if it is FREE, <tt>false</tt>
     *                   if BUSY.
     * @pre $none
     * @post $none
     */
    public void setStatus(boolean status) {
        status_ = status;
    }

    /**
     * Gets the byte size of this class
     * @return the byte size
     * @pre $none
     * @post $result > 0
     */
    public static int getByteSize()
    {
        int totalInt = 2 * 4;  // NOTE: static int doesn't count
        return totalInt;
    }

    /**
     * Sets this PE to FAILED.
     * @pre $none
     * @post $none
     */
    public void setStatusFailed() {
        status_ = FAILED;
    }

} 

