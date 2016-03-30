/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2004, The University of Melbourne, Australia
 */

package gridsim.util;

import eduni.simjava.distributions.ContinuousGenerator;
import eduni.simjava.distributions.DiscreteGenerator;
import gridsim.net.Link;


/**
 * A generator that tells how many packets sent and how big each packet is
 * for every time interval. This generator is used by 
 * {@link gridsim.net.Output} entity
 * to generate junk packets or some background traffic.
 * <p>
 * This class uses <code>eduni.simjava.distributions</code> package which is 
 * available from SimJava2 only. If you want to use other distribution
 * package, then you need to wrap each source file with either 
 * {@link eduni.simjava.distributions.DiscreteGenerator} or
 * {@link eduni.simjava.distributions.ContinuousGenerator} interface class.
 *
 * @see eduni.simjava.distributions.DiscreteGenerator
 * @see eduni.simjava.distributions.ContinuousGenerator
 * @since GridSim Toolkit 3.1
 * @author Anthony Sulistio
 * @invariant $none
 */
public class TrafficGenerator
{
    private int serviceType_;   // level of service for packets
    private int pattern_;       // pattern of distributing packets
    private static final double ROUND_UP = 0.5;

    // use discrete probabilistic distribution
    private DiscreteGenerator discreteSize_;   // packet size
    private DiscreteGenerator discreteTime_;   // inter-arrival time
    private DiscreteGenerator discreteFreq_;   // frequency of created packets

    // use continuous probabilistic distribution
    private ContinuousGenerator conSize_;   // packet size
    private ContinuousGenerator conTime_;   // inter-arrival time
    private ContinuousGenerator conFreq_;   // frequency of created packets

    /** Sends junk packets to all entities at one time, 
     * including resources and/or users
     */
    public static final int SEND_ALL = 1;

    /** Sends junk packets to one of the entities at one time using 
     * a normal distribution from <tt>java.util.Random.nextInt(int)</tt>
     */
    public static final int SEND_ONE_ONLY = 2;    


    /**
     * Creates a new background traffic generator.
     * Each junk packet has a default size 
     * ({@link gridsim.net.Link#DEFAULT_MTU}), a default service type (0),
     * and it will send to all resource entities and users (if applicable) 
     * ({@link #SEND_ALL}).
     *
     * @param freq      a generator for frequency or number of packets sent at 
     *                  one time
     * @param timegen   a generator for inter-arrival sending time (second)
     * @pre freq != null
     * @pre timegen != null
     * @post $none    
     */
    public TrafficGenerator(DiscreteGenerator freq, DiscreteGenerator timegen)
    {
        discreteSize_ = null;
        discreteTime_ = timegen;
        discreteFreq_ = freq;

        serviceType_ = 0;
        pattern_ = SEND_ALL;

        conSize_ = null;
        conTime_ = null;
        conFreq_ = null;
    }

    /**
     * Creates a new background traffic generator.
     * Each junk packet has a default size 
     * ({@link gridsim.net.Link#DEFAULT_MTU}), a default service type (0),
     * and it will send to all resource entities and users (if applicable) 
     * ({@link #SEND_ALL}).
     *
     * @param freq      a generator for frequency or number of packets sent at 
     *                  one time
     * @param timegen   a generator for inter-arrival sending time (second)
     * @pre freq != null
     * @pre timegen != null
     * @post $none    
     */
    public TrafficGenerator(ContinuousGenerator freq, ContinuousGenerator timegen)
    {
        discreteSize_ = null;
        discreteTime_ = null;
        discreteFreq_ = null;

        serviceType_ = 0;
        pattern_ = SEND_ALL;

        conSize_ = null;
        conTime_ = timegen;
        conFreq_ = freq;
    }

    /**
     * Creates a new background traffic generator.
     * Each junk packet has a default size 
     * ({@link gridsim.net.Link#DEFAULT_MTU}), a default service type (0),
     * and it will send to all resource entities and users (if applicable) 
     * ({@link #SEND_ALL}).
     *
     * @param freq      a generator for frequency or number of packets sent at 
     *                  one time
     * @param timegen   a generator for inter-arrival sending time (second)
     * @pre freq != null
     * @pre timegen != null
     * @post $none    
     */
    public TrafficGenerator(DiscreteGenerator freq, ContinuousGenerator timegen)
    {
        discreteSize_ = null;
        discreteTime_ = null;
        discreteFreq_ = freq;

        serviceType_ = 0;
        pattern_ = SEND_ALL;

        conSize_ = null;
        conTime_ = timegen;
        conFreq_ = null;
    }
    
    /**
     * Creates a new background traffic generator.
     * Each junk packet has a default size 
     * ({@link gridsim.net.Link#DEFAULT_MTU}), a default service type (0),
     * and it will send to all resource entities and users (if applicable) 
     * ({@link #SEND_ALL}).
     *
     * @param freq      a generator for frequency or number of packets sent at 
     *                  one time
     * @param timegen   a generator for inter-arrival sending time (second)
     * @pre freq != null
     * @pre timegen != null
     * @post $none    
     */
    public TrafficGenerator(ContinuousGenerator freq, DiscreteGenerator timegen)
    {
        discreteSize_ = null;
        discreteTime_ = timegen;
        discreteFreq_ = null;

        serviceType_ = 0;
        pattern_ = SEND_ALL;

        conSize_ = null;
        conTime_ = null;
        conFreq_ = freq;
    }

    /**
     * Sets the sending packet pattern if one or more entities are known.
     * The pattern is one of the following:
     * <ul>
     *      <li> {@link #SEND_ALL}: sends to all resources and users (if known)
     *      <li> {@link #SEND_ONE_ONLY}: sends to only one of them
     * </ul>
     *
     * @param pattern   
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre $none
     * @post $none
     */
    public boolean setPattern(int pattern)
    {
        if (pattern < SEND_ALL || pattern > SEND_ONE_ONLY) {
            return false;
        }

        pattern_ = pattern;
        return true;
    }

    /**
     * Gets the sending packet pattern
     * @return packet pattern
     * @pre $none
     * @post $none
     */
    public int getPattern() {
        return pattern_;
    }

    /**
     * Sets the size of each junk packet using a continuous distribution
     * @param sizegen   a generator for each packet size
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre sizegen != null
     * @post $none
     */
    public boolean setPacketSize(ContinuousGenerator sizegen) 
    {
        if (sizegen == null) {
            return false;
        }
        
        conSize_ = sizegen;
        return true;
    }

    /**
     * Sets the size of each junk packet using a discrete distribution
     * @param sizegen   a generator for each packet size
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre sizegen != null
     * @post $none
     */
    public boolean setPacketSize(DiscreteGenerator sizegen) 
    {
        if (sizegen == null) {
            return false;
        }
        
        discreteSize_ = sizegen;
        return true;
    }

    /**
     * Gets the next inter-arrival time between sending packets.
     * For using continuous distribution, the value is rounded up to the
     * nearest integer number.
     *
     * @return the next inter-arrival time
     * @pre $none
     * @post $none
     */
    public long getNextPacketTime()
    {
        long result = -1;
        if (discreteTime_ != null) {
            result = discreteTime_.sample();
        }
        else if (conTime_ != null)
        {
            double time = conTime_.sample();
            result = (long) (time + ROUND_UP); // round up
        }

        return result;
    }

    /**
     * Gets the next frequency or number of packets sent for each time.
     * For using continuous distribution, the value is rounded up to the
     * nearest integer number.
     *
     * @return frequence or number of packets sent for each time
     * @pre $none
     * @post $none
     */
    public long getNextPacketFreq()
    {
        long result = 0;
        if (discreteFreq_ != null) {
            result = discreteFreq_.sample();
        }
        else if (conFreq_ != null)
        {
            double time = conFreq_.sample();
            result = (long) (time + ROUND_UP); // round up
        }

        return result;
    }

    /**
     * Gets the next packet size. By default, the packet size is 
     * {@link gridsim.net.Link#DEFAULT_MTU}.
     * For using continuous distribution, the value is rounded up to the
     * nearest integer number.
     *
     * @return the next packet size
     * @pre $none
     * @post $none
     */
    public long getNextPacketSize()
    {
        long result = Link.DEFAULT_MTU;
        if (discreteSize_ != null) {
            result = discreteSize_.sample();
        }
        else if (conSize_ != null)
        {
            double size = conSize_.sample();
            result = (long) (size + ROUND_UP);  // round up
        }

        return result;
    }

    /**
     * Gets the service level of this packet. By default, the value is 
     * 0 (zero).
     * @return the service level of this packet
     * @pre $none
     * @post $none
     */
    public int getServiceType() {
        return serviceType_;
    }

    /**
     * Sets the service level of this packet.
     * @param type  the service level of this packet
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise
     * @pre type >= 0
     * @post $none
     */
    public boolean setServiceType(int type)
    {
        if (type < 0) {
            return false;
        }

        serviceType_ = type;
        return true;
    }

} 

