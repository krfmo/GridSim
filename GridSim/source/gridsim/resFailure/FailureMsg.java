/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Created on: Nov 2006.
 * Copyright (c) 2007, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */


package gridsim.resFailure;

/**
 * This class is used by
 * {@link gridsim.resFailure.RegionalGISWithFailure} to commnunicate with
 * {@link gridsim.resFailure.GridResourceWithFailure}
 * for simulating a resource failure.
 *
 * @author       Agustin Caminero
 * @since        GridSim Toolkit 4.1
 */

public class FailureMsg
{
    /** The time that the failure will last */
    private double time;

    /** The id of the resource */
    private int res_id;

    /** The number of machines that will fail in this resource */
    private int numMachines;


    /**
     * Creates a new failure message
     * @param time  the time that the failure will last
     * @param rid   the resource id
     */
    public FailureMsg(double time, int rid) {
        this.time = time;
        res_id  = rid;
        numMachines = 0;
    }

    /**
     * This method sets the time attribute of the ResourceFailure object
     * @param time   the time that the failure will last
     */
    public void setTime(double time)
    {
        this.time = time;
    }

    /**
     * This method returns the time attribute of the ResourceFailure object
     * @return time
     */
    public double getTime()
    {
        return time;
    }

    /**
     * This method sets the res_id attribute of the ResourceFailure object
     * @param r the resource id of the resource
     */
    public void setRes(int r)
    {
        res_id = r;
    }

    /**
     * This method returns the res_id attribute of the ResourceFailure object
     * @return the resource id
     */
    public int getRes()
    {
        return res_id;
    }

    /**
     * This method sets the numMachines attribute of the ResourceFailure object
     * @param n the number of machines which will fail in this resource
     */
    public void setNumMachines(int n)
    {
        numMachines = n;
    }

    /**
     * This method returns the number of machines which will fail
     * in this resource
     * @return number of failed machines
     */
    public double getNumMachines()
    {
        return numMachines;
    }

} 
