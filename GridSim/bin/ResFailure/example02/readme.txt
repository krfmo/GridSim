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

To run the class file:
In Unix/Linux:
    java -classpath $GRIDSIM/jars/gridsim.jar:.  ResFailureEx02 network_ex02.txt > file.txt

In Windows:
    java -classpath %GRIDSIM%\jars\gridsim.jar;. ResFailureEx02 network_ex02.txt > file.txt


The above command means run the program and output the results into a file
named "file.txt" rather than into screen or standard output.
To prevent from overwriting an existing file,
I renamed "file.txt" into "output.txt"

NOTE: This example uses probabilistic distributions and random variables.
      Hence, when running this example many times, the values will be different.


When running the example file, it will produce the following files:

    Ex02_Regional_GIS -> contains one line describing each
        registering/removing an event that takes place at the GIS. Resources
        register in the begining of simulations, and after recovering from a
        failure affecting all their machines. Resources will be removed when
        they suffer a failure affecting all their machines.

    Ex02_Res_0, ... ,  Ex01_Res_5 -> records when the resource
        suffers a failure and when it recovers from the failure. It also records
        how many machines failed, and the time of the fail/recovery.

    Ex02_User_0 -> contains one line for each event regarding to the Gridlets.
        These events are the sending/reception of a gridlet, and
        each line include the status of the gridlet, time and resource.
        Then, when a simulation ends, this file shows the resource
        a Gridlet has been executed, its cost, CPU time and total latency.

NOTE: When you run the program multiple times, the new statistics will be
      appended at the end of a file.
