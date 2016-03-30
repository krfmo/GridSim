/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * Defines MIPS (Million Instructions Per Second) rating for a standard
 * PE (Processing Element) or enables the users to define their own
 * MIPS or SPEC (Standard Performance Evaluation Corporation) rating
 * for a standard PE. This value can be used for creating PEs with
 * relative MIPS or SPEC rating for GridSim resources.
 * <p>
 * This class can be used to define the capability resources or the
 * execution time of Gridlets with respect to standard PE.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class GridSimStandardPE
{
    /** The default value of MIPS Rating (if not defined elsewhere) */
    private static int MIPSRating;

    // initialize static value
    static {
        MIPSRating = 100;  // default value
    }

    /**
     * Allocates a new GridSimStandardPE object. Since all methods and
     * attribute are static, so it is better to declare a Constructor to be
     * private.
     * @pre $none
     * @post $none
     */
    private GridSimStandardPE() {
        // empty
    }

    /**
     * Sets standard PE MIPS Rating
     * @param rating  the value of a standard PE MIPS rating
     * @pre rating >= 0
     * @post $none
     */
    public static void setRating(int rating) {
        MIPSRating = rating;
    }

    /**
     * Gets standard PE MIPS Rating
     * @return the value of a standard PE MIPS rating
     * @pre $none
     * @post $result >= 0
     */
    public static int getRating() {
        return MIPSRating;
    }

    /**
     * Converts Execution time in second processor to MIs
     * @param timeInSec the execution time in second w.r.t. standard time
     * @return Equivalent Million Instructions (MIs) for TimeInSec
     * @pre timeInSec >= 0.0
     * @post $result >= 0.0
     */
    public static double toMIs(double timeInSec) {
        return MIPSRating * timeInSec;
    }

} 

