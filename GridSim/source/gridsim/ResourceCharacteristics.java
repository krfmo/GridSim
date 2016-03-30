/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * GridSim ResourceCharacteristics class represents static properties of a
 * resource such as resource architecture, Operating System (OS), management
 * policy (time- or space-shared), cost and time zone at which the resource
 * is located along resource configuration.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class ResourceCharacteristics
{
    private int id_;     // resource id--setup when Resource is created
    private String architecture_;
    private String OS_;
    private MachineList machineList_;
    private double timeZone_;    // difference from GMT

    // Price/CPU-unit if unit = sec., then G$/CPU-sec.
    private double costPerSec_;

    // Resource Types -- allocation policy
    private int allocationPolicy_;

    /** Time-shared system using Round-Robin algorithm */
    public static final int TIME_SHARED = 0;

    /** Spaced-shared system using First Come First Serve (FCFS) algorithm */
    public static final int SPACE_SHARED = 1;

    /** Assuming all PEs in all Machines have the same rating. */
    public static final int OTHER_POLICY_SAME_RATING = 2;

    /** Assuming all PEs in a Machine have the same rating.
     * However, each Machine has different rating to each other.
     */
    public static final int OTHER_POLICY_DIFFERENT_RATING = 3;

    /** A resource that supports Advanced Reservation mechanisms. */
    public static final int ADVANCE_RESERVATION = 4;

    /**
     * Allocates a new ResourceCharacteristics object.
     * If the time zone is invalid, then by default, it will be GMT+0.
     * @param architecture  the architecture of a resource
     * @param OS            the operating system used
     * @param machineList   list of machines in a resource
     * @param allocationPolicy     the resource allocation policy
     * @param timeZone   local time zone of a user that owns this reservation.
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
    public ResourceCharacteristics(String architecture, String OS,
                MachineList machineList, int allocationPolicy,
                double timeZone, double costPerSec)
    {
        this.id_ = -1;
        this.architecture_ = architecture;
        this.OS_ = OS;
        this.machineList_ = machineList;
        this.allocationPolicy_ = allocationPolicy;
        this.costPerSec_ = costPerSec;

        if (!AdvanceReservation.validateTimeZone(timeZone)) {
            this.timeZone_ = 0.0;
        }
        else {
            this.timeZone_ = timeZone;
        }
    }

    /**
     * Sets the resource ID
     * @param id    the resource ID
     * @pre id >= 0
     * @post $none
     */
    public void setResourceID(int id) {
        this.id_ = id;
    }

    /**
     * Gets the resource ID
     * @return the resource ID
     * @pre $none
     * @post $result >= 0
     */
    public int getResourceID() {
        return id_;
    }

    /**
     * Gets the name of a resource
     * @return the resource name
     * @pre $none
     * @post $result != null
     */
    public String getResourceName() {
        return GridSim.getEntityName(id_);
    }

    /**
     * Gets the resource architecture name
     * @return the architecture name
     * @pre $none
     * @post $result != null
     */
    public String getResourceArch() {
        return architecture_;
    }

    /**
     * Gets the Operating System (OS) this resource is used
     * @return the name of OS
     * @pre $none
     * @post $result != null
     */
    public String getResourceOS() {
        return OS_;
    }

    /**
     * Gets the list of machines in a resouce
     * @return a MachineList object
     * @see gridsim.MachineList
     * @pre $none
     * @post $result != null
     */
    public MachineList getMachineList() {
        return machineList_;
    }

    /**
     * Gets a Machine with at least one empty PE
     * @return a Machine object or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    public Machine getMachineWithFreePE() {
        return machineList_.getMachineWithFreePE();
    }

    /**
     * Gets a Machine with at least a given number of free PE
     * @param numPE  number of PE
     * @return a Machine object or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    public Machine getMachineWithFreePE(int numPE) {
        return machineList_.getMachineWithFreePE(numPE);
    }

    /**
     * Gets the resource allocation policy
     * @return the allocation policy
     * @pre $none
     * @post $result >= 0 && $result <= 2
     */
    public int getResourceAllocationPolicy() {
        return allocationPolicy_;
    }

    /**
     * Gets the resource time zone
     * @return the time zone
     * @pre $none
     * @post $none
     */
    public double getResourceTimeZone() {
        return timeZone_;
    }

    /**
     * Gets Millions Instructions Per Second (MIPS) Rating of a Processing
     * Element (PE). It is assumed all PEs' rating is same in a given machine.
     * @return the MIPS Rating or <tt>-1</tt> if no PEs are exists.
     * @pre $none
     * @post $result >= -1
     */
    public int getMIPSRatingOfOnePE()
    {
        if (machineList_.size() == 0) {
            return -1;
        }

        return machineList_.getMachine(0).getPEList().getMIPSRating(0);
    }

    /**
     * Gets Millions Instructions Per Second (MIPS) Rating of a Processing
     * Element (PE).
     * It is essential to use this method when a resource is made up
     * of heterogenous PEs/machines.
     * @param id        the machine ID
     * @param peID      the PE ID
     * @return the MIPS Rating or <tt>-1</tt> if no PEs are exists.
     * @pre id >= 0
     * @pre peID >= 0
     * @post $result >= -1
     */
    public int getMIPSRatingOfOnePE(int id, int peID)
    {
        if (machineList_.size() == 0) {
            return -1;
        }

        return machineList_.getMachine(id).getPEList().getMIPSRating(peID);
    }

    /**
     * Gets the total MIPS rating, which is the sum of MIPS rating of all
     * machines in a resource.
     * <p>
     * Total MIPS rating for:
     * <ul>
     *     <li>TimeShared = 1 Rating of a PE * Total number of PEs
     *     <li>Other policy same rating = same as TimeShared
     *     <li>SpaceShared = Sum of all PEs in all Machines
     *     <li>Other policy different rating = same as SpaceShared
     *     <li>Advance Reservation = 0 or unknown.
     *         You need to calculate this manually.
     * </ul>
     *
     * @return the sum of MIPS ratings
     * @pre $none
     * @post $result >= 0
     */
    public int getMIPSRating()
    {
        int rating = 0;
        switch (allocationPolicy_)
        {
            // Assuming all PEs in all Machine have same rating.
            case ResourceCharacteristics.TIME_SHARED:
            case ResourceCharacteristics.OTHER_POLICY_SAME_RATING:
                rating = getMIPSRatingOfOnePE() * machineList_.getNumPE();
                break;

            // Assuming all PEs in a given Machine have the same rating.
            // But different machines in a Cluster can have different rating
            case ResourceCharacteristics.SPACE_SHARED:
            case ResourceCharacteristics.OTHER_POLICY_DIFFERENT_RATING:
                for (int i = 0; i < machineList_.size(); i++) {
                    rating += ((Machine) machineList_.get(i)).getMIPSRating();
                }
                break;

            default:
                break;
        }

        return rating;
    }

    /**
     * Gets the CPU time given the specified parameters (only for TIME_SHARED).
     * <tt>NOTE:</tt> The CPU time for SPACE_SHARED and ADVANCE_RESERVATION
     *                are not yet implemented.
     * @param gridletLength     the length of a Gridlet
     * @param load              the load of a Gridlet
     * @return the CPU time
     * @pre gridletLength >= 0.0
     * @pre load >= 0.0
     * @post $result >= 0.0
     */
    public double getCPUTime(double gridletLength, double load)
    {
        double cpuTime = 0.0;

        switch (allocationPolicy_)
        {
            case ResourceCharacteristics.TIME_SHARED:
                cpuTime = gridletLength / ( getMIPSRatingOfOnePE()*(1.0-load) );
                break;

            default:
                break;
        }

        return cpuTime;
    }

    /**
     * Gets the total number of PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumPE() {
        return machineList_.getNumPE();
    }

    /**
     * Gets the total number of <tt>FREE</tt> or non-busy PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumFreePE() {
        return machineList_.getNumFreePE();
    }

    /**
     * Gets the total number of <tt>BUSY</tt> PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumBusyPE() {
        return machineList_.getNumBusyPE();
    }

    /**
     * Sets the particular PE status on a Machine
     * @param status   PE status, either <tt>PE.FREE</tt> or <tt>PE.BUSY</tt>
     * @param machineID    Machine ID
     * @param peID     PE id
     * @return <tt>true</tt> if the PE status has changed, <tt>false</tt>
     * otherwise (Machine id or PE id might not be exist)
     * @pre machineID >= 0
     * @pre peID >= 0
     * @post $none
     */
    public boolean setStatusPE(boolean status, int machineID, int peID) {
        return machineList_.setStatusPE(status, machineID, peID);
    }

    /**
     * Sets the cost per second associated with a resource
     * @param costPerSec   the cost using a resource
     * @pre costPerSec >= 0.0
     * @post $none
     */
    public void setCostPerSec(double costPerSec) {
        this.costPerSec_ = costPerSec;
    }

    /**
     * Gets the cost per second associated with a resource
     * @return the cost using a resource
     * @pre $none
     * @post $result >= 0.0
     */
    public double getCostPerSec() {
        return costPerSec_;
    }

    /**
     * Gets the cost per Millions Instruction (MI) associated with a resource
     * @return the cost using a resource
     * @pre $none
     * @post $result >= 0.0
     */
    public double getCostPerMI() {
        return costPerSec_ / getMIPSRatingOfOnePE();
    }

    /**
     * Gets the byte size of this class
     * @return the byte size
     * @pre $none
     * @post $result > 0
     */
    public int getByteSize()
    {
        // this class overall has: 2 ints, 2 Strings, 1 MachineList and
        //                         2 doubles.
        // NOTE: static attributes do not count
        int totalInt = 2 * 4;
        int totalDouble = 2 * 8;
        int totalSize = architecture_.length() + OS_.length() +
                   machineList_.getByteSize() + totalInt + totalDouble ;

        return totalSize;
    }

    /**
     * Gets the total number of machines.
     * @return total number of machines this resource has.
     */
    public int getNumMachines() {
        return machineList_.size();
    }

    /**
     * Gets the current number of failed machines.
     * @return current number of failed machines this resource has.
     */
    public int getNumFailedMachines()
    {
        int numFailedMachines = 0;
        int numMach = machineList_.size();
        Machine mach;

        for (int i = 0; i < numMach; i++)
        {
            mach = machineList_.getMachineInPos(i);
            if (mach.getFailed()) {
                numFailedMachines++;
            }
        }

        return numFailedMachines;
    }

    /**
     * Checks whether all machines of this resource are working properly or not.
     * @return <tt>true</tt> if all machines are working,
     *         <tt>false</tt> otherwise
     */
    public boolean isWorking()
    {
        boolean result = false;
        if (this.getNumFailedMachines() == 0) {
            result = true;
        }

        return result;
    }

} 

