/*
 * ** Network and Service Differentiation Extensions to GridSim 3.0 **
 *
 * Gokul Poduval & Chen-Khong Tham
 * Computer Communication Networks (CCN) Lab
 * Dept of Electrical & Computer Engineering
 * National University of Singapore
 * September 2004
 *
 * License: GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2004, The University of Melbourne, Australia and National
 * University of Singapore
 * RIPAdPack.java - Used to send route advertisements in RIP
 *
 */

package gridsim.net;

import eduni.simjava.*;
import gridsim.*;
import gridsim.util.*;
import java.util.*;


/**
 * This class is used to send link state advertisements to other
 * routers. Only hosts are advertised, not routers.
 *
 * @invariant $none
 * @since GridSim Toolkit 3.1
 * @author Gokul Poduval & Chen-Khong Tham, National University of Singapore
 */
public class RIPAdPack
{
    private Collection hosts;
    private String sender;
    private int hopcount;


    /**
     * Allocates a new object
     * @param sender    the sender's name
     * @param c         a Collection object
     * @pre sender != null
     * @pre c != null
     * @post $none
     */
    public RIPAdPack(String sender, Collection c)
    {
        this.hosts = c;
        this.sender = sender;
        this.hopcount = 1;
    }

    /**
     * Gets the sender's name
     * @return the sender's name
     * @pre $none
     * @post $none
     */
    public String getSender() {
        return sender;
    }

    /**
     * Set the sender's name
     * @param sender    the sender's name
     * @pre sender != null
     * @post $none
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * Gets a list of hosts
     * @return a Collection of host
     * @pre $none
     * @post $none
     */
    public Collection getHosts() {
        return hosts;
    }

    /**
     * Gets the number of hops
     * @return the number of hops
     * @pre $none
     * @post $none
     */
    public int getHopCount() {
        return hopcount;
    }

    /**
     * Increments the hop counter by one
     * @pre $none
     * @post $none
     */
    public void incrementHopCount() {
        hopcount++;
    }

	/**
	 * Sets the hopcount
	 * @pre $none
	 * @post $none
	 */
	public void setHopCount(int hopcount)
	{
		this.hopcount = hopcount ;
	}

    /**
     * Represents the information of this class into String
     * @return information about this class
     * @pre $none
     * @post $none
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer("Router ");
        s.append(sender);
        s.append(" connects to hosts");

        Iterator i = hosts.iterator();
        while ( i.hasNext() )
        {
            s.append(" ");
            s.append( i.next() );
        }

        s.append(" with hopcount ");
        s.append(hopcount);

        return s.toString();
    }

} // end class

