/**
 * Author: ly
 * Date: March 2016
 */

本例是一个带命令参数的java程序
在命令行中运行，我是不会的。。。但是在百度里找到在eclipse运行该程序的方法
右键Test.java，Run As -> Run Configurations
找到（x）=Arguments，在其中填入参数即可，参数之间用空格隔开

To run the class file:
    In Unix/Linux:
        java -cp $GRIDSIM/jars/gridsim.jar:. Test [space | time] [test num]

    In Windows:
        java -cp %GRIDSIM%\jars\gridsim.jar;. Test [space | time] [test num]

For example:
    In Unix/Linux to run a SpaceShared algorithm for TestCase #8:
        java -classpath $GRIDSIM/jars/gridsim.jar:. Test space 8 > outputSpaceTest8.txt

