/*
 * Title:        GridSim Toolkit
 * Description:  GridSim (Grid Simulation) Toolkit for Modeling and Simulation
 *               of Parallel and Distributed Systems such as Clusters and Grids
 *               An example of how to use the failure functionality.
 * License:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Author: Agustin Caminero and Anthony Sulistio
 * Organization: Universidad de Castilla La Mancha (UCLM), Spain.
 * Copyright (c) 2008, The University of Melbourne, Australia and
 * Universidad de Castilla La Mancha (UCLM), Spain
 */

To run the class file:
In Unix/Linux:
    java -classpath $GRIDSIM/jars/gridsim.jar:.  mainExample sched stats

In Windows:
    java -classpath %GRIDSIM%\jars\gridsim.jar;. mainExample sched stats

where sched = {red, ared, fifo}, and stats = {true, false}. 
* sched refers to one of the FNB schedulers.
  By default, sched = fifo if no parameter is included in the program.
  
* stats refers to recording network statistics.
  stats = true  means turn on the network statistics.
  stats = false means turn off the network statistics.


NOTE: This example uses probabilistic distributions and random variables.
      Hence, when running this example many times, the values may be different.

The example may take a long time, even if the statistics collection is off. 
To make the experiment shorter, decrease the number of users, the number 
of jobs per user, jobs IO files sizes, or increase baud rate of links, 
buffer sizes, thresholds of RED/ARED.
This way, less packets will get dropped and experiments will take less time.


When running the example file, it will produce the following files:

    User_0.csv, User_1.csv, ... -> contain statistics regarding to 
    gridlets submission, and the status of each gridlet at the end of simulation.

Next, the below files are only created when the statistics collection is ON 
(i.e. stats = true). Keep in mind that experiments will take longer time 
if stats are ON.

    Router0_to_Res_0_Buffers.csv, Router0_to_Res_1_Buffers.csv, ...
    -> Contains statistics on the progress of buffers parameter 
       (avg. queue size, ...), in intervals.

    Router0_to_Res_0_DroppedPkts.csv, Router0_to_Res_1_DroppedPkts.csv, ...
    -> Contains stats on the amount of droppped pkts, in intervals.

    Router0_to_Res_0_MaxBufferSize.csv, Router0_to_Res_1_MaxBufferSize.csv, ... 
    -> Contains stats on the maximum buffer size, in intervals

NOTE: 
* When you run the program multiple times, the new statistics  
  will overwrite the existing csv files.
  
* You can open these csv files on Excel or any text editors.
