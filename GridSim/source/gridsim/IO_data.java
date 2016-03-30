/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * Class relates to a communication between user entities and resources
 * or user entities and others.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class IO_data
{
    private Object data_;
    private long byteSize_;
    private int destID_;
    private int netServiceLevel_;   // used by network schedulers


    /**
     * Allocates a new IO_data object
     * @param data         the data object
     * @param byteSize     the size of a data (in bytes)
     * @param destID       the destination ID
     * @pre data != null
     * @pre byteSize >= 0
     * @pre destID >= 0
     * @post $none
     */
    public IO_data(Object data, long byteSize, int destID)
    {
        this.data_ = data;
        this.byteSize_ = byteSize;
        this.destID_ = destID;
        this.netServiceLevel_ = 0;

        // if the data is a Gridlet and has a network service level
        if (data instanceof Gridlet)
        {
            Gridlet gl = (Gridlet) data;
            netServiceLevel_ = gl.getNetServiceLevel();
        }
    }

    /**
     * Allocates a new IO_data object with a specific network service level.
     * <p>
     * The network service level of 0 is the normal or default level.
     * Other levels are treated according to the policy being
     * followed by the system.
     * For example, if using {@link gridsim.net.SCFQScheduler} as a packet
     * scheduler, then setting the level to 1 or higher means this object
     * gets a higher priority.
     *
     * @param data         the data object
     * @param byteSize     the size of a data (in bytes)
     * @param destID       the destination ID
     * @param netServiceLevel   determines the kind of service this packet
     *                          receives in the network (applicable to
     *                          selected PacketScheduler class only)
     *
     * @see gridsim.net.SCFQScheduler
     * @pre data != null
     * @pre byteSize >= 0
     * @pre destID >= 0
     * @post $none
     */
    public IO_data(Object data, long byteSize, int destID, int netServiceLevel)
    {
        this.data_ = data;
        this.byteSize_ = byteSize;
        this.destID_ = destID;
        this.netServiceLevel_ = netServiceLevel;    // Best Effort

        // if the data is a Gridlet and a user wishes to use
        // a network service level.
        // NOTE: Gridlet's existing network service level will be overridden
        if (data instanceof Gridlet)
        {
            Gridlet gl = (Gridlet) data;
            gl.setNetServiceLevel(netServiceLevel);
        }
    }

    /**
     * Returns the class type of this IO_data object.
     * @return the classtype
     * @pre $none
     * @post $none
     */
    public int getNetServiceLevel() {
        return this.netServiceLevel_;
    }

    /**
     * Gets the Object data
     * @return the Object data
     * @pre $none
     * @post $result != null
     */
    public Object getData() {
        return data_;
    }

    /**
     * Gets the size of a data
     * @return the data size
     * @pre $none
     * @post $result >= 0
     */
    public long getByteSize() {
        return byteSize_;
    }

    /**
     * Gets the destination ID
     * @return the destination ID
     * @pre $none
     * @post $result >= 0
     */
    public int getDestID() {
        return destID_;
    }

    /**
     * Returns a human-readable information of this object
     * @return a String representation of this object
     * @pre $none
     * @post $none
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer("Object = [");
        str.append(data_);
        str.append("], size = ");
        str.append(byteSize_);
        str.append(" bytes, destination = ");
        str.append( GridSim.getEntityName(destID_) );
        str.append(", network service type = ");
        str.append(netServiceLevel_);

        return str.toString();
    }

} 

