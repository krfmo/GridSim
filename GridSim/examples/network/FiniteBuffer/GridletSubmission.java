package network.FiniteBuffer;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */
 

import gridsim.Gridlet;


/**
 * This class implements the submission time of each gridlet.
 * @author Agustin Caminero
 */
public class GridletSubmission
{
    private Gridlet gl;

    // whether this gridlet has been already submitted or not
    private boolean submitted;

    /**
     * A constructor
     * @param   gl          a Gridlet object
     * @param   submitted   whether this gridlet has been already submitted or not
     */
    public GridletSubmission(Gridlet gl, boolean submitted)
    {
        this.gl = gl;
        this.submitted = submitted;
    }

    /**
     * Gets a Gridlet object
     */
    public Gridlet getGridlet()
    {
        return gl;
    }

    /**
     * Gets the gridlet ID
     */
    public int getGridletID()
    {
        return gl.getGridletID();
    }

    /**
     * Finds whether this gridlet has been already submitted or not
     */
    public boolean getSubmitted()
    {
        return submitted;
    }

    /**
     * Sets a Gridlet object
     */
    public void setGridlet(Gridlet g)
    {
        this.gl = g;
    }

    /**
     * Sets the status of this Gridlet, whether it has been submitted or not
     */
    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

} // end class

