/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.*;
import java.io.*;
import eduni.simjava.*;

/**
 * A class representing contents of a statistic object
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class Stat
{
    private double time_;
    private String category_;
    private String name_;
    private String data_;


    /**
     * Allocates a new Stat object
     * @param time the time at which Statistic info was recorded.
     *             Generally this value is taken from <code>GridSim.clock()</code>
     * @param category user-defined name for data type
     * @param name of the entity that want to store this data
     * @param data data to be recorded
     * @see gridsim.GridSim#clock()
     * @pre time >= 0.0
     * @pre category != null
     * @pre name != null
     * @pre data != null
     * @post $none
     */
    public Stat(double time, String category, String name, String data)
    {
        this.time_ = time;
        this.category_ = category;
        this.name_ = name;
        this.data_ = data;
    }

    /**
     * Allocates a new Stat object
     * @param time the time at which Statistic info was recorded.
     *             Generally this value is taken from <code>GridSim.clock()</code>
     * @param category user-defined name for data type
     * @param name of the entity that want to store this data
     * @param data data to be recorded
     * @see gridsim.GridSim#clock()
     * @pre time >= 0.0
     * @pre category != null
     * @pre name != null
     * @pre data >= 0
     * @post $none
     */
    public Stat(double time, String category, String name, int data)
    {
        this.time_ = time;
        this.category_ = category;
        this.name_ = name;
        this.data_ = "" + data;
    }

    /**
     * Allocates a new Stat object
     * @param time the time at which Statistic info was recorded.
     *             Generally this value is taken from <code>GridSim.Clock()</code>
     * @param category user-defined name for data type
     * @param name of the entity that want to store this data
     * @param data data to be recorded
     * @see gridsim.GridSim#clock()
     * @pre time >= 0.0
     * @pre category != null
     * @pre name != null
     * @pre data >= 0.0
     * @post $none
     */
    public Stat(double time, String category, String name, double data)
    {
        this.time_ = time;
        this.category_ = category;
        this.name_ = name;
        this.data_ = "" + data;
    }

    /**
     * Allocates a new Stat object
     * @param time the time at which Statistic info was recorded.
     *             Generally this value is taken from <code>GridSim.Clock()</code>
     * @param category user-defined name for data type
     * @param name of the entity that want to store this data
     * @param data data to be recorded
     * @see gridsim.GridSim#clock()
     * @pre time >= 0.0
     * @pre category != null
     * @pre name != null
     * @post $none
     */
    public Stat(double time, String category, String name, boolean data)
    {
        this.time_ = time;
        this.category_ = category;
        this.name_ = name;
        this.data_ = "" + data;
    }

    /**
     * Gets the time at which Statistic info was recorded
     * @return the time at which Statistic info was recorded
     * @pre $none
     * @post $result >= 0.0
     */
    public double getTime() {
        return time_;
    }

    /**
     * Gets the user-defined name for data type
     * @return the user-defined name for data type
     * @pre $none
     * @post $result != null
     */
    public String getCategory() {
        return category_;
    }

    /**
     * Gets the name of the entity that want to store this data
     * @return the name of the entity that want to store this data
     * @pre $none
     * @post $result != null
     */
    public String getName() {
        return name_;
    }

    /**
     * Gets the the data to be recorded
     * @return the data to be recorded
     * @pre $none
     * @post $result != null
     */
    public String getData() {
        return data_;
    }

    /**
     * Gets the the concatenated value of all items as a string
     * @return the concatenated value of all items as a string
     * @pre $none
     * @post $result != null
     */
    public String toString() {
        return "" + time_ + "\t" + category_ + "\t" + name_ + "\t" + data_;
    }

} 

