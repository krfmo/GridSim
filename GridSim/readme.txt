Readme file

Directory Structure of GridSim Toolkit 5.2
------------------------------------------

$GRIDSIM/               -- the current GridSim directory (top level)
    classes/            -- The GridSim class files
    doc/                -- GridSim and SimJava API Documentation
        eduni/
        gridsim/
    examples/           -- GridSim examples, see examples/readme.txt for details
    jars/               -- jar archives
    source/             -- The GridSim Java source code
        gridsim/*.java
        gridsim/auction/*.java      -- framework for the auction model
        gridsim/datagrid/*.java     -- framework for the Data Grids model
        gridsim/filter/*.java       -- filters incoming events
        gridsim/index/*.java        -- framework for the Grid Info Service model
        gridsim/net/*.java          -- framework for the network model
        gridsim/resFailure/*.java   -- framework for the resource failure model
        gridsim/util/*.java         -- includes some statistics classes.
        gridsim/parallel/*.java     -- includes scheduling of parallel jobs
        gridsim/fta/*.java          -- includes a package for resource failure

GridSim APIs and examples are also available on the GridSim website.

Software Requirements : Java version 1.5 or newer
---------------------
GridSim has been tested and run on Sun's Java version 1.5.0 or newer. Older 
versions of Java are not compatible. You also need to install Ant to compile 
GridSim (explained in more details later).


Installation and Running GridSim Toolkit
----------------------------------------
There is no special program to install GridSim. You just need to
unzip the GridSim file to install.
If you want to remove GridSim, then remove the whole $GRIDSIM directory.

NOTE: You do not need to compile GridSim source code. The JAR file is
      provided to compile and to run GridSim applications.

Description of the following jar files:
* gridsim.jar  -- contains both GridSim and SimJava v2.0 class files
* simjava2.jar -- contains SimJava v2.0 class files only

To compile and run GridSim applications, do the following step:
1) Go the directory where the GridSim's examples reside
   In Unix or Linux: cd $GRIDSIM/examples/
   In Windows:       cd %GRIDSIM%\examples\

2) Compile the Java source file (e.g. example01)
   In Unix or Linux: javac -cp $GRIDSIM/jars/gridsim.jar:. gridsim/example01/Example1.java
   In Windows:       javac -cp %GRIDSIM%\jars\gridsim.jar;. gridsim\example01\Example1.java

3) Running the Java class file
   In Unix or Linux: java -cp $GRIDSIM/jars/gridsim.jar:. gridsim/example01/Example1
   In Windows:       java -cp %GRIDSIM%\jars\gridsim.jar;. gridsim\example01\Example1

NOTE:
* $GRIDSIM or %GRIDSIM% is the location of the GridSim Toolkit package.
* Running GridSim of this version requires a lot of memory since there are many
  objects to be created. Therefore, it is recommended to have at least 512MB RAM
  or increase JVM heap size when running Java for large simulation experiments.
  For example: java -Xmx300m -classpath $GRIDSIM/jars/gridsim.jar:. gridsim/example01/Example1
  (max. heap size is 300MB).
