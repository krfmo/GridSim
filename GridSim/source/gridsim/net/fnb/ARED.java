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

package gridsim.net.fnb;

import gridsim.GridSim;
import java.io.FileWriter;
import gridsim.net.Link;

/**
  * This class implements the Adaptative Random Early Detection (ARED) policy for
  * the management of network buffers at routers.
  * Its basic functionality is as follows:
  * <ul>
  * <li> There is a <tt>ARED</tt> object at each outport in routers.
  * <li> For each incoming packet that reaches that outport port, the policy
  *    decides whether it is enqueued or dropped. This is done by calculating the
  *    average buffer size and comparing it with two thresholds.
  * <li> If the packet is dropped, and it is not a junk packet, we must inform the
  *    user involved in the transmission about the dropping.
  * </ul>
  *
  * For more details, please refer to A. Caminero, A. Sulistio, B. Caminero,
  * C. Carrion, and R. Buyya,
  * <a href="http://www.gridbus.org/papers/BufferManagementNetGrids-ANSS41.pdf">
  * Simulation of Buffer Management Policies in Networks for Grids</a>,
  * Proceedings of the 41th Annual Simulation Symposium (ANSS-41, IEEE CS Press,
  * Los Alamitos, CA, USA), April 14-16, 2008, Ottawa, Canada.
  *
  * @author       Agustin Caminero
  * @since GridSim Toolkit 4.2
  */
public class ARED extends RED
{
    /** Decrease factor*/
    private double BETA;

    /** Increment*/
    private double ALPHA;

    /** Target for AVG */
    private double TARGET_LOW;

    /** Target for AVG*/
    private double TARGET_HIGH;


    /**
     * Creates a new Adaptative Random Early Detection (ARED) policy.
     * @param name       Name of this scheduler
     * @param baudRate   baud rate in bits/s of the port that is using
     *                   this scheduler.
     * @param max_p the maximum dropping probability for an incoming packet
     * @param max_buf_size  maximum buffer size for routers
     * @param queue_weight this parameter reflects how important is the last
     * measurement of the buffer size on the calculation of the average buffer size
     * @param stats whether we want to record some stats or not
     * @throws Exception This happens when the baud rate <= 0
     * @pre baudRate > 0
     * @post $none
     */
    public ARED(String name, double baudRate, double max_p, int max_buf_size,
                double queue_weight, boolean stats) throws Exception
    {
        super(name, baudRate, max_buf_size, 0.0, 0.0, max_p, queue_weight, stats);
        initialize();
    }

    /**
     * This function updates the value of <tt>max_p</tt>, which is the maximum 
     * dropping probability for a packet.
     * It also updates <tt>ALPHA</tt>, as it depends on <tt>max_p</tt>.
     */
    public void updateAREDParameters()
    {
        double max_p = getMaxP();
        if ((getAvg() > TARGET_HIGH) && (getMaxP() <= 0.5))
        {
            // increase max_p
            setMaxP(max_p + ALPHA);
        }
        else if ((getAvg() < TARGET_LOW) && (max_p >= 0.01))
        {
            // decrease max_p
            setMaxP(max_p * BETA);
        }

        if ((max_p / 4) < 0.01)
            ALPHA = max_p / 4;
        else
            ALPHA = 0.01;

    }

    /**
     * Sets the this class and {@link gridsim.net.fnb.RED} thresholds.
     */
    public void setThresholds()
    {
        double minTh;

        double C = super.getBaudRate() / (Link.DEFAULT_MTU * 8);
        // baudRate_is in bits per second, MTU is in bytes.

        double term = -1 / C;
        double term2 = Math.exp(term);
        super.setQueueWeight(1 - term2);

        double DELAY_TARGET = 0.005; // 5 milliseconds
        double var = DELAY_TARGET * C / 2;
        if (5 > var)
        {
            minTh = 5;
        }
        else
        {
            minTh = var;

        }

        super.setMinTh(minTh);
        super.setMaxTh(3 * minTh);
    }

    /**This function initializes the parameters of the buffers policies
     */
    protected void initialize()
    {
        super.initialize();
        setThresholds();

        if ((getMaxP() / 4) < 0.01)
            ALPHA = getMaxP() / 4;
        else
            ALPHA = 0.01;

        BETA = 0.9;
    }


    /**
     * Sets the baud rate that this scheduler will be sending packets at.
     * @param rate the baud rate of this scheduler (in bits/s)
     * @return true if the baud rate has been set properly
     * @pre rate > 0
     * @post $none
     */
    public boolean setBaudRate(double rate)
    {
        if (rate <= 0.0)
        {
            return false;
        }
        super.setBaudRateSCFQ(rate);
        initialize();

        return true;
    }

    /**
     * Prints out the given message into stdout.
     * In addition, writes it into a file.
     * @param msg   a message
     * @param file  file where we want to write
     */
    private static void fw_write(String msg, String file)
    {
        FileWriter fwriter = null;

        try
        {
            fwriter = new FileWriter(file, true);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while opening file " + file);
        }

        try
        {
            fwriter.write(msg);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while writing on file " + file);
        }

        try
        {
            fwriter.close();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            System.out.println("Unwanted errors while closing file " + file);
        }
    }
}

