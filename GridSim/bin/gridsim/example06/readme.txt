/**
 * Author: Anthony Sulistio
 * Date: April 2003
 */

NOTE: When you open "output.txt" file, it tells you that this example creates
      3 grid users and 3 grid resources. Each user has different number of
      Gridlets everytime you run the program (depending on the GridSimRandom
      static object from GridSim class).

      Then each user allocates its Gridlets randomly to different grid
      resources. A grid resource will modify the status of Gridlets into
      "Gridlet.SUCCESS". This example ends by printing the status of all
      Gridlets from grid users.
