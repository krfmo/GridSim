/*
 * Title:        GridSim Toolkit

 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 */

package gridsim;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * GridSim MachineList simulates a collection of machines. It is up to the
 * GridSim users to define the connectivity among the machines in a collection.
 * Therefore, this class can be instantiated to model a simple LAN to cluster
 * to WAN.
 *
 * @author       Manzur Murshed and Rajkumar Buyya
 * @since        GridSim Toolkit 1.0
 * @invariant $none
 */
public class MachineList extends LinkedList<Machine>
{

    /**
     * Gets the Machine object for a particular ID
     * @param id    the machine ID
     * @return the Machine object or <tt>null</tt> if no machine exists
     * @see gridsim.Machine
     * @pre id >= 0
     * @post $none
     */
    public Machine getMachine(int id)
    {
        Machine obj = null;

        Iterator<Machine> it = super.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();
            if (obj.getMachineID() == id) {
                return obj;
            }
        }

        return null;    // no machine with given id
    }

    /**
     * Gets the total number of PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumPE()
    {
        int totalSize = 0;
        Machine obj = null;

        Iterator<Machine> it = super.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();
            totalSize += obj.getNumPE();
        }

        return totalSize;
    }

    /**
     * Gets the total number of <tt>FREE</tt> or non-busy PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumFreePE()
    {
        int free = 0;
        Machine obj = null;

        Iterator<Machine> it = super.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();
            free += obj.getNumFreePE();
        }

        return free;
    }

    /**
     * Gets the total number of <tt>BUSY</tt> PEs for all Machines
     * @return number of PEs
     * @pre $none
     * @post $result >= 0
     */
    public int getNumBusyPE() {
        return this.getNumPE() - this.getNumFreePE();
    }

    /**
     * Gets a Machine with free PE
     * @return a machine object or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    public Machine getMachineWithFreePE() {
        return this.getMachineWithFreePE(1);
    }

    /**
     * Gets a Machine with a specified number of free PE
     * @param numPE   number of free PE
     * @return a machine object or <tt>null</tt> if not found
     * @pre $none
     * @post $none
     */
    public Machine getMachineWithFreePE(int numPE)
    {
        Machine obj = null;

        Iterator<Machine> it = super.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();

            // If the machine is failed, do nothing. Otherwise...
            if (!obj.getFailed())
            {
                PEList myPElist = obj.getPEList();
                if (myPElist.getNumFreePE() >= numPE) {
                    return obj; // a machine with Free ID is found.
                }
            }
            /**************
            // Uncomment this if you want more info on the progress of sims
            else
            {
                System.out.println("MachineList.getMachineWithFreePE(). MachineID: " +
                    obj.getMachineID() + ". Machine is failed. Try other machine.");
            }*/
        }

        return null;    // none of the machines have free PE.
    }

    /**
     * Sets the particular PE status on a Machine
     * @param status   PE status, either <tt>PE.FREE</tt> or <tt>PE.BUSY</tt>
     * @param machineID    Machine ID
     * @param peID     PE id
     * @return <tt>true</tt> if the PE status has changed, <tt>false</tt>
     * otherwise (Machine id or PE id might not be exist)
     * @pre machineID >= 0
     * @pre peID >= 0
     * @post $none
     */
    public boolean setStatusPE(boolean status, int machineID, int peID)
    {
        Machine obj = getMachine(machineID);
        if (obj == null) {
            return false;
        }

        return obj.setStatusPE(status, peID);
    }

    /**
     * Gets the byte size of this class
     * @return the byte size
     * @pre $none
     * @post $result >= 0
     */
    public int getByteSize()
    {
        int bSize = 0;
        Machine obj = null;

        Iterator<Machine> it = super.iterator();
        while ( it.hasNext() )
        {
            obj = it.next();
            bSize += obj.getByteSize();
        }

        return bSize;
    }

    /**
     * Gets the machine in a given position in the list.
     * @param index     a position index in the list
     * @return the Machine object
     */
    public Machine getMachineInPos(int index)
    {
        Machine obj = null;
        Iterator<Machine> it = super.iterator();
        int count = 0;
        while ( (it.hasNext()) && (count <= index) )
        {
            count++;
            obj = it.next();
        }

        return obj;

    }

} 

