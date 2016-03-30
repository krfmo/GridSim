/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2008, The University of Melbourne, Australia
 * Author: James Broberg
 */

package gridsim.net.flow;


/**
 * This the structure for network flows. 
 *
 * @since GridSim Toolkit 4.2
 * @author James Broberg, The University of Melbourne
 */
public interface Flow
{
    /**
     * Returns a string describing this flow in detail.
     * @return description of this flow
     * @pre $none
     * @post $none
     */
    String toString();

    /**
     * Returns the size of this flow
     * @return size of the flow
     * @pre $none
     * @post $none
     */
    long getSize();

    /**
     * Sets the size of this flow
     * @param size  size of the flow
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre size >= 0
     * @post $none
     */
    boolean setSize(long size);

    /**
     * Returns the destination id of this flow.
     * @return destination id
     * @pre $none
     * @post $none
     */
    int getDestID();

    /**
     * Returns the ID of this flow
     * @return flow ID
     * @pre $none
     * @post $none
     */
    int getID();
    
    /**
     * Returns the ID of the source of this flow.
     * @return source id
     * @pre $none
     * @post $none
     */
    int getSrcID();

    /**
     * Gets the network service type of this flow
     * @return the network service type
     * @pre $none
     * @post $none
     */
    int getNetServiceType();

    /**
     * Sets the network service type of this flow.
     * <p>
     * By default, the service type is 0 (zero). It is depends on the packet
     * scheduler to determine the priority of this service level.
     * @param serviceType   this flow's service type
     * @pre serviceType >= 0
     * @post $none
     */
    void setNetServiceType(int serviceType);

    /**
     * Gets an entity ID from the last hop that this packet has traversed.
     * @return an entity ID
     * @pre $none
     * @post $none
     */
    int getLast();

    /**
     * Sets an entity ID from the last hop that this packet has traversed.
     * @param last  an entity ID from the last hop
     * @pre last > 0
     * @post $none
     */
    void setLast(int last);

    /**
     * Gets this flow tag
     * @return this flow tag
     * @pre $none
     * @post $none
     */
    int getTag();

} // end interface

