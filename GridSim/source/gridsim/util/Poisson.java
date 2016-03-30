/* Sim_poisson_obj.java */

package gridsim.util;

import eduni.simjava.distributions.*;

/**
 * A random number generator based on the Poisson distribution. <br>
 * NOTE: This is an updated version that fixes a bug in
 *       {@link eduni.simjava.distributions.Sim_poisson_obj#sample()}
 *       method. In the previous version, it always return a zero value.
 *
 * @version     1.0, 14 May 2002
 * @author      Costas Simatos
 */

public class Poisson implements DiscreteGenerator
{
    private Sim_random_obj source_;
    private double mean_;
    private String name;

    /*** Test driver
    public static void main(String[] args)
    {
        int mean = 10;

        Poisson obj = new Poisson("Poisson", mean);
        for (int i = 0; i < mean; i++) {
            System.out.println("i = " + i + " = [" + obj.sample() + "]");
        }
    }
    *****/

    /**
     * Constructor with which <code>Sim_system</code> is allowed to
     * set the random number generator's seed
     * @param name The name to be associated with this instance
     * @param mean The mean of the distribution
     */
    public Poisson(String name, double mean) {
        if (mean <= 0.0) {
            throw new Sim_parameter_exception(
                "The mean must be greater than 0.");
        }
        source_ = new Sim_random_obj("Internal PRNG");
        this.mean_ = mean;
        this.name = name;
    }

    /**
     * The constructor with which a specific seed is set for the random
     * number generator
     * @param name The name to be associated with this instance
     * @param mean The mean of the distribution
     * @param seed The initial seed for the generator, two instances with
     *             the same seed will generate the same sequence of numbers
     */
    public Poisson(String name, double mean, long seed) {
        if (mean <= 0.0) {
            throw new Sim_parameter_exception(
                "The mean must be greater than 0.");
        }
        source_ = new Sim_random_obj("Internal PRNG", seed);
        this.mean_ = mean;
        this.name = name;
    }

    /**
     * Generate a new random number.
     * @return The next random number in the sequence
     */
    public long sample() {
        long x = -1L;
        double m = Math.exp(-1 * mean_);
        double product = 1;
        do {
            x++;
            product *= source_.sample();
        } while(m < product);
        return x;
    }

    /**
     * Generate a new random number.
     * It is used by other distributions that rely on the Poisson distribution
     * @return The next random number in the sequence
     */
    static long sample(Sim_random_obj source, double mean) {
        long x = -1L;
        double m = Math.exp(-1 * mean);
        double product = 1;
        do {
            x++;
            product *= source.sample();
        } while(m < product);
        return x;
    }

    /**
     * Set the random number generator's seed.
     * @param seed The new seed for the generator
     */
    public void set_seed(long seed) {
        source_.set_seed(seed);
    }

    /**
     * Get the random number generator's seed.
     * @return The generator's seed
     */
    public long get_seed() {
        return source_.get_seed();
    }

    /**
     * Get the random number generator's name.
     * @return The generator's name
     */
    public String get_name() {
        return name;
    }

} 

