/*
 * @(#) Variate.java     1.3     98/1/20
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
 * Random number generation via an installable random number generator. <br>
 * NOTE: This class is taken from
 * <a href="http://www.cs.uga.edu/~jam/jsim/">JSim</a>.
 * @since GridSim Toolkit 4.1
 */

public class Variate
{
    /////////////////// Immutable Variables \\\\\\\\\\\\\\\\\\\\\\\
    /**
     * Base Random Number Generator (RNG)
     */
    private final LCGRandom rand;    // JSIM's own RNG (an LCG)
    //private final Random rand;     // RNG from standard java library

    /**
     * Array of parameters
     */
    private  final Double []  params = new Double [1];


    /**************************************************************
     * Constructs a random variate.
     * @param  i  stream number
     */
    public Variate (int i)
    {
        rand = new LCGRandom (i);     // stream number i
        params [0] = new Double (i);
        //rand = new Random (i);      // seed value i
    }


    /**************************************************************
     * Increment the stream.
     */
    public void incStream () {
        rand.incrStream ();
    }


    /**************************************************************
     * Get the parameters of the constuctor
     */
    public Double [] getParameters ()
    {
        return  params;
    }


    /**************************************************************
     * Generate a random number in the interval 0 to 1.
     * @return  double  random number
     */
    public double gen ()
    {
        return rand.gen ();
        //return rand.nextDouble ();
    } 
}

