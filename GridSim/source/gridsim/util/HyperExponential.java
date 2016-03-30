/*
 * @(#) HyperExponential.javag1.3     98/1/20
 *
 * Copyright (c) 2005, John Miller, Zhiwei Zhang
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 * 3. Neither the name of the University of Georgia nor the names
 *    of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior
 *    written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @version     1.3, 28 Jan 2005
 * @author      John Miller, Zhiwei Zhang
 */

package gridsim.util;


/**
 * Generate a hyperexponentially distributed random number mean mu
 * and standard deviation sigma (sigma > mu) using Morse's two-stage
 * hyperexponential distribution. <br>
 * NOTE: This class is taken from
 * <a href="http://www.cs.uga.edu/~jam/jsim/">JSim</a>.
 * @since GridSim Toolkit 4.1
 */

public class HyperExponential extends Variate
{
    /**
     * Mean
     */
    private final double mu;

    /**
     * Parameter derived from coefficient of variation
     */
    private final double p;

    /**
     * Array of parameters
     */
    private  final Double []  params = new Double [3];


    /*************************************************************
     * Constructs a Hyper-Geometric random variable.
     * @param  mu     mean
     * @param  sigma  standard deviation
     * @param  i      stream
     */
    public HyperExponential (double mu, double sigma, int i)
    {
        super (i);
        if (sigma <= 0.0) {
            System.err.println ("Hyperexponential argument error: " +
                    "standard deviation is less than zero");
            System.exit (0);
        } // if
        this.mu    = mu;
        double cv2 = sigma / mu;
        cv2 *= cv2;
        p = 0.5d * (1.0 - Math.sqrt ((cv2 - 1.0) / (cv2 + 1.0)));

        params [0] = new Double (mu);
        params [1] = new Double (sigma);
        params [2] = new Double (i);

    }


    /**************************************************************
     * Get the parameters of the constructor
     * @return the parameters
     */
    public Double [] getParameters ()
    {
        return  params;
    }


    /*************************************************************
     * Generate a hyperexponentially distirbuted random number.
     * @return  double  random number from HyperExponential distribution
     */
    public double gen ()
    {
        double super_gen = super.gen ();
        double z  = (super_gen > p) ? mu / (1.0 - p) : mu / p;
        double value = - 0.5 * z * Math.log (super.gen ());
        return value;

    } 

}

