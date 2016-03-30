/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Random;

/**
 * GridSim Random provides static methods for incorporating randomness in data
 * used for any simulation.
 * <p>
 * <b>NOTE<b> From version 5.0 this class will NOT be created by GridSim upon 
 * initialization of the simulation. Hence, you should instantiate this class
 * according to your needs.
 * <p>
 * Any predicted or estimated data, e.g. number of
 * Gridlets used by an experiment, execution time and output size of a Gridlet,
 * etc. need to present in the prediction or estimation process and the
 * randomness that exists in the nature itself.
 * <p>
 * <tt>Example:</tt> to produce a random number between 18.00 and 22.00, need
 * to invoke <tt>GridSimRandom.real(20.0, 0.1, 0.1, randDouble)</tt> where
 * <tt>randDouble</tt> comes from <b>java.util.Random</b>.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @see gridsim.Gridlet
 * @see java.util.Random
 * @invariant $none
 * @see gridsim.GridSim#init(int, Calendar, boolean)
 * @see gridsim.GridSim#init(int, Calendar, boolean, String[], String[], String)
 */
public class GridSimRandom
{
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 1;
    private static Random random_;

    // Factor values for Network I/O
    private static double lessFactorIO_;
    private static double moreFactorIO_;

    // Factor values for Execution
    private static double lessFactorExec_;
    private static double moreFactorExec_;

    /** Initializes all static variables */
    static
    {
        random_ = new Random();
        lessFactorIO_ = 0.0;
        moreFactorIO_ = 0.0;
        lessFactorExec_ = 0.0;
        moreFactorExec_ = 0.0;
    }

    /**
     * Allocates a new GridSimRandom object
     * @pre $none
     * @post $none
     */
    public GridSimRandom() {
        // empty
    }

    /**
     * Allocates a new GridSimRandom object using a single <tt>long</tt> seed
     * @param seed the initial seed
     * @pre seed >= 0.0
     * @post $none
     */
    public GridSimRandom(long seed) {
        random_.setSeed(seed);
    }

    /**
     * Allocates a new GridSimRandom object with specified parameters
     * @param seed          the initial seed
     * @param lessFactorIO  less factor for Network I/O
     * @param moreFactorIO  more factor for Network I/O
     * @param lessFactorExec   less factor for execution
     * @param moreFactorExec   more factor for execution
     * @pre seed >= 0.0
     * @pre lessFactorIO >= 0.0
     * @pre moreFactorIO >= 0.0
     * @pre lessFactorExec >= 0.0
     * @pre moreFactorExec >= 0.0
     * @post $none
     */
    public GridSimRandom(long seed, double lessFactorIO, double moreFactorIO,
                double lessFactorExec, double moreFactorExec)
    {
        random_.setSeed(seed);
        lessFactorIO_ = lessFactorIO;
        moreFactorIO_ = moreFactorIO;
        lessFactorExec_ = lessFactorExec;
        moreFactorExec_ = moreFactorExec;
    }

    /**
     * Sets the Network I/O and execution values
     * @param lessFactorIOValue  less factor for Network I/O
     * @param moreFactorIOValue  more factor for Network I/O
     * @param lessFactorExecValue   less factor for execution
     * @param moreFactorExecValue   more factor for execution
     * @pre lessFactorIOValue >= 0.0
     * @pre moreFactorIOValue >= 0.0
     * @pre lessFactorExecValue >= 0.0
     * @pre moreFactorExecValue >= 0.0
     * @post $none
     */
    public static void setAllFactors(double lessFactorIOValue,
                double moreFactorIOValue, double lessFactorExecValue,
                double moreFactorExecValue)
    {
        lessFactorIO_ = lessFactorIOValue;
        moreFactorIO_ = moreFactorIOValue;
        lessFactorExec_ = lessFactorExecValue;
        moreFactorExec_ = moreFactorExecValue;
    }

    /**
     * Gets the random int value from <b>java.util.Random</b>
     * @param range the bound on the random number to be returned.
     *              The range must be positive (excluding 0).
     * @return a pseudorandom, uniformly distributed <tt>int</tt> value between
     *         0 (inclusive) and range (exclusive)
     * @throws IllegalArgumentException range is not positive
     * @pre range > 0
     * @post $result >= 0
     */
    public static int intSample(int range) throws IllegalArgumentException {
        return random_.nextInt(range);
    }

    /**
     * Gets the random double value from <b>java.util.Random</b>
     * @return the next pseudorandom, uniformly distributed <tt>double</tt>
     *         value between 0.0 and 1.0 from this random number generator's
     *         sequence
     * @pre $none
     * @post $result >= 0.0 && $result <= 1.0
     */
    public static double doubleSample() {
        return random_.nextDouble();
    }

    /**
     * Sets the less factor of Network I/O
     * @param factor the initial factor
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre factor >= 0.0
     * @post $none
     */
    public static void setLessFactorIO(double factor)
                throws IllegalArgumentException
    {
        if (factor < MIN_VALUE)
        {
            throw new IllegalArgumentException(
                    "GridSimRandom.setLessFactorIO() : Error - factor must" +
                    " be zero or positive value.");
        }
        lessFactorIO_ = factor;
    }

    /**
     * Sets the more factor of Network I/O
     * @param factor the initial factor
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre factor >= 0.0
     * @post $none
     */
    public static void setMoreFactorIO(double factor)
                throws IllegalArgumentException
    {
        if (factor < MIN_VALUE)
        {
            throw new IllegalArgumentException(
                    "GridSimRandom.setMoreFactorIO() : Error - factor must" +
                    " be zero or positive value.");
        }
        moreFactorIO_ = factor;
    }

    /**
     * Sets the less factor of Execution
     * @param factor the initial factor
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre factor >= 0.0
     * @post $none
     */
    public static void setLessFactorExec(double factor)
                throws IllegalArgumentException
    {
        if (factor < MIN_VALUE)
        {
            throw new IllegalArgumentException(
                    "GridSimRandom.setLessFactorExec() : Error - factor must" +
                    " be zero or positive value.");
        }

        lessFactorExec_ = factor;
    }

    /**
     * Sets the more factor of Execution
     * @param factor the initial factor
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre factor >= 0.0
     * @post $none
     */
    public static void setMoreFactorExec(double factor)
                throws IllegalArgumentException
    {
        if (factor < MIN_VALUE)
        {
            throw new IllegalArgumentException(
                    "GridSimRandom.setMoreFactorExec() : Error - factor must" +
                    " be zero or positive value.");
        }
        moreFactorExec_ = factor;
    }

    /**
     * Gets the average factor of Network I/O
     * @return factor of Network I/O
     * @pre $none
     * @post $result >= 0.0
     */
    public static double getFactorIO() {
        return (moreFactorIO_ - lessFactorIO_) / 2;
    }

    /**
     * Gets the average factor of Execution
     * @return factor of Execution
     * @pre $none
     * @post $result >= 0.0
     */
    public static double getFactorExec() {
        return (moreFactorExec_ - lessFactorExec_) / 2;
    }

    /**
     * Maps the predicted or estimated <tt>value</tt> to a random real-world
     * number between <tt>(1 - lessFactor) * value</tt> and
     * <tt>(1 + moreFactor) * value</tt>.
     * <p>
     * <b>The formula used is:</b>
     * <br>
     * <tt>value * (1 - lessFactor + (lessFactor + moreFactor) *
     *          randDouble)</tt>
     * <br>
     * where 0.0 <= lessFactor and moreFactor <= 1.0
     * @param value         the estimated value
     * @param lessFactor    less factor for a value
     * @param moreFactor    more factor for a value (the range is between 0.0
     *                      and 1.0)
     * @param randDouble    an uniformly distributed <tt>double</tt> value
     *                      between 0.0 and 1.0
     * @return the real number in <tt>double</tt>
     * @throws IllegalArgumentException <tt>value</tt>, <tt>lessFactor</tt>,
     *              <tt>moreFactor</tt> and <tt>randDouble</tt> are not zero
     *              or positive. In addition, the max. number for
     *              <tt>moreFactor</tt> and <tt>randDouble</tt> is 1.0
     * @pre value >= 0.0
     * @pre lessFactor >= 0.0
     * @pre moreFactor >= 0.0 && moreFactor <= 1.0
     * @pre randDouble >= 0.0 && randDouble <= 1.0
     * @post $none
     */
    public static double real(double value, double lessFactor,
                double moreFactor, double randDouble)
                throws IllegalArgumentException
    {
        String errorMsg = "GridSimRandom.real(): Error - ";
        if (value < MIN_VALUE)
        {
            throw new IllegalArgumentException(errorMsg +
                    "value must be zero or positive value.");
        }

        if (lessFactor < MIN_VALUE)
        {
            throw new IllegalArgumentException(errorMsg +
                    "lessFactor must be zero or positive value.");
        }

        if (moreFactor < MIN_VALUE || moreFactor > MAX_VALUE)
        {
            throw new IllegalArgumentException(errorMsg +
                    "moreFactor must be within [0.0, 1.0].");
        }

        if (randDouble < MIN_VALUE || randDouble > MAX_VALUE)
        {
            throw new IllegalArgumentException(errorMsg +
                    "randDouble must be within [0.0, 1.0].");
        }

        double result = value *
            (MAX_VALUE - lessFactor + (lessFactor + moreFactor) * randDouble);

        return result;
    }

    /**
     * Gets the real number from the factors of Network I/O
     * @param value   the estimated value
     * @return the real number in <tt>double</tt>
     * @throws IllegalArgumentException value is not zero or positive
     * @pre value >= 0.0
     * @post $none
     */
    public static double realIO(double value) throws IllegalArgumentException {
        return real( value, lessFactorIO_, moreFactorIO_, doubleSample() );
    }

    /**
     * Gets the real number from the factors of Execution
     * @param value  the estimated value
     * @return the real number in <tt>double</tt>
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre value >= 0.0
     * @post $none
     */
    public static double realExec(double value)
                throws IllegalArgumentException
    {
        return real( value, lessFactorExec_, moreFactorExec_, doubleSample() );
    }

    /**
     * Gets the expected factor of Network I/O
     * @param value   the estimated value
     * @return the expected number in <tt>double</tt>
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre value >= 0.0
     * @post $none
     */
    public static double expectedIO(double value)
                throws IllegalArgumentException
    {
        return value * ( MAX_VALUE + getFactorIO() );
    }

    /**
     * Gets the expected factor of Execution
     * @param value   the estimated value
     * @return the expected number in <tt>double</tt>
     * @throws IllegalArgumentException factor is not zero or positive
     * @pre value >= 0.0
     * @post $none
     */
    public static double expectedExec(double value)
                throws IllegalArgumentException
    {
        return value * ( MAX_VALUE + getFactorExec() );
    }

} 

