package gridsim.fta;

/**
 * This is the interface for allocation policy with failure
 *  
 * @author       Bahman Javadi
 * @since        GridSim Toolkit 5.0
 * 
 * @see gridsim.FTAGridResource
 * @see gridsim.AllocPolicy
 */

public interface FTAllocPolicy {

    /**
     * Sets the status of all Gridlets in this machine to <tt>FAILED</tt>.
     * Then sends them back to users, and clean up the relevant lists.
     * @param failedMachID  the id of the failed machine
     */
    void setGridletsFailed(int failedMachID);

    /**
     * Sets the status of all Gridlets in this machine to <tt>RESUME</tt>.
     * Then sends them back to users, and clean up the relevant lists.
     * @param failedMachID  the id of the failed machine
     */
    boolean setGridletsResumed(int failedMachID);
}
