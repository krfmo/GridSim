
/**
 * Author: Anthony Sulistio
 * Date: November 2004
 */

NOTE: 
* running this experiment might take a lot of memory if the size of trace file
  is big (in terms of number of lines/jobs).
  If you encounter "out of memory" exception, you need to increase JVM heap size
  using 'java -Xmx' option.
  
  For example set the heap size to 300MB:
  
  In Unix/Linux: 
  java -Xmx300000000 -classpath $GRIDSIM/jars/gridsim.jar:. TraceEx01 > file.txt
  
  In Windows:    
  java -Xmx300000000 -classpath %GRIDSIM%\jars\gridsim.jar;. TraceEx01 > file.txt
  
* When you open "output.txt" file, it tells you that this example creates
  two entities: Workload and GridResource. The Workload entity sends Gridlets
  to a particular GridResource entity. 

* When you see the "output.txt" file, there are few warnings about a Gridlet
  requires more than 1 PE. This is because the current GridSim schedulers,
  TimeShared and SpaceShared, only process 1 PE for each Gridlet.
  You are welcome to write your own scheduler that incorporates this 
  QoS (Quality of Service) requirement.


