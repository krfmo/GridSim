/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

/**
 * GridSim Accumulator provides a placeholder for maintaining statistical
 * values of a series of data added to it. It can be queried for mean,
 * min, max, sum, square mean, standard deviation, and the largest and
 * smallest values in the data series.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class Accumulator
{
    private int n_;           // the number of items accumulated
    private double mean_;     // the mean of accumulated items
    private double sqrMean_;  // the square mean of accumulated items
    private double min_;      // the smallest of accumulated items
    private double max_;      // the largest of accumulated items
    private double last_;     // the last accumulated items


    /**
     * Allocates a new Accumulator object
     * @pre $none
     * @post $none
     */
    public Accumulator()
    {
        n_ = 0;
        mean_ = Double.NaN;
        sqrMean_ = Double.NaN;
        min_ = Double.NaN;
        max_ = Double.NaN;
        last_ = Double.NaN;
    }

    /**
     * Adds an item to the Accumulator
     * @param item     an item to be added to the Accumulator
     * @param times    number of time the item value is repeated
     * @pre times > 0
     * @post $none
     */
    public void add(double item, int times)
    {
        if (times < 1) {
            return;
        }

        last_ = item;
        if (n_ <= 0)
        {
            n_ += times;
            mean_ = item;
            min_ = item;
            max_ = item;
            sqrMean_ = item*item;
        }
        else
        {
            n_ += times;
            mean_ = ( (n_ - times) * mean_ + item*times ) / n_;
            sqrMean_ = ( (n_ - times)*sqrMean_ + item*item*times ) / n_;

            if (item < min_) {
                min_ = item;
            }

            if (item > max_) {
                max_ = item;
            }
        }
    }

    /**
     * An overloaded method
     * @param item an item to be added to the accumulator
     * @pre $none
     * @post $none
     */
    public void add(double item) {
        this.add(item, 1);
    }

    /**
     * Calculates the mean of accumulated items
     * @return the mean of accumalated items
     * @pre $none
     * @post $none
     */
    public double getMean() {
        return mean_;
    }

    /**
     * Calculates the standard deviation of accumulated items
     * @return the Standard Deviation of accumulated items
     * @pre $none
     * @post $none
     */
    public double getStandardDeviation() {
        return Math.sqrt( this.getVariance() );
    }

    /**
     * Calculates the variance of accumulated items
     * @return the Standard Deviation of accumulated items
     * @pre $none
     * @post $none
     */
    public double getVariance() {
    	return sqrMean_ - (mean_ * mean_);
    }

    /**
     * Finds the smallest number of accumulated items
     * @return the smallest of accumulated items
     * @pre $none
     * @post $none
     */
    public double getMin() {
        return min_;
    }

    /**
     * Finds the largest number of accumulated items
     * @return the largest of accumulated items
     * @pre $none
     * @post $none
     */
    public double getMax() {
        return max_;
    }

    /**
     * Finds the last accumulated item
     * @return the last accumulated item
     * @pre $none
     * @post $none
     */
    public double getLast() {
        return last_;
    }

    /**
     * Counts the number of items accumulated so far
     * @return the number of items accumulated so far
     * @pre $none
     * @post $result >= 0
     */
    public int getCount() {
        return n_;
    }

    /**
     * Calculates the sum of accumulated items
     * @return the sum of accumulated items
     * @pre $none
     * @post $none
     */
    public double getSum() {
        return n_ * mean_;
    }

    /**
     * Determines the size of Accumulator object
     * @return the size of this object
     * @pre $none
     * @post $result > 0
     */
    public static int getByteSize()
    {
        int totalInt = 4;           // contains only 1 int
        int totalDouble = 5 * 8;    // contains only 5 doubles

        return totalInt + totalDouble;
    }

} 

