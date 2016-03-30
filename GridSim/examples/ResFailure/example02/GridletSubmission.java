package ResFailure.example02;

/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 *               An example of how to use the failure functionality.
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: UCLM (Spain)
 * Created on: August 2007
 */


import gridsim.Gridlet;

/**
 * This class is just a wrapper to denote
 * whether this gridlet has been already submitted or not
 * @author       Agustin Caminero and Anthony Sulistio
 * @since        GridSim Toolkit 4.1
 */
public class GridletSubmission
{
    private Gridlet gl;
    private boolean submitted;


    public GridletSubmission(Gridlet gl,boolean submitted)
    {
        this.gl = gl;
        this.submitted = submitted;
    }

    public Gridlet getGridlet()
    {
        return gl;
    }

    public boolean getSubmitted()
    {
        return submitted;
    }

    public void setGridlet(Gridlet g)
    {
        this.gl = g;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

} // end class
