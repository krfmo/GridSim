/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Anthony Sulistio
 * Organization: The University of Melbourne, Australia
 * Created on: Wednesday, 22 August 2007
 * Copyright (c) 2008, The University of Melbourne, Australia 
 */

package gridsim.net;

import gridsim.util.TrafficGenerator;
import java.util.Collection;

/**
 * This class contains the structure for Input and Output entities.
 * @since GridSim Toolkit 4.2
 * @author Anthony Sulistio
 */
public interface NetIO
{
    /** A constant that denotes 1 byte in bits */
    int BITS = 8;
    
    /**
     * Sets this entity's link. This should be used only if the network
     * extensions are being used.
     *
     * @param link the link to which this entity should send/receive data
     */
    void addLink(Link link);

    /**
     * Gets the baud rate
     * @return the baud rate
     */
    double getBaudRate();

    /**
     * Sets the background traffic generator for <b>Output</b> entity only.
     * <p>
     * When simulation starts, this entity will automatically sends junk
     * packets to resource entities.
     * @param gen   a background traffic generator
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     */
    boolean setBackgroundTraffic(TrafficGenerator gen);

    /**
     * Sets the background traffic generator for <b>Output</b> entity only.
     * <p>
     * When simulation starts, this entity will automatically sends junk
     * packets to resource entities and other entities. <br>
     * NOTE: Sending background traffic to itself is not supported.
     *
     * @param gen       a background traffic generator
     * @param userName  a collection of user entity name (in String object).
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre gen != null
     * @pre userName != null
     * @post $none
     */
    boolean setBackgroundTraffic(TrafficGenerator gen, Collection userName);

} 

