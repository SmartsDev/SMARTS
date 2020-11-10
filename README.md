# SMARTS

SMARTS (Scalable Microscopic Adaptive Road Traffic Simulator) is a flexible microscopic traffic simulator developed at the School of Computing and Information Systems, University of Melbourne. The microscopic simulator model vehicles and traffic lights individually. Various road rules and driver behavior are implemented into SMARTS. The simulator can output various types of traffic data. For more information about the simulator, please visit https://projects.eng.unimelb.edu.au/smarts/. 

If you would like to customize the simulator, the following simple programming guide may help you get started. 

The source code of SMARTS contains five packages. The name and purpose of each package are as follows. 

- The ‘common’ package: global settings and some utility methods. The class common.Settings holds the settings that are shared by all processors (server and workers).   

- The ‘osm’ package: extraction of road network information from OpenStreetMap XML files. You need to modify this if you want to extract additional/customized information from OSM files. 

- The ‘processor’ package: architecture of the simulation system.  
       
  - Sub-package processor.server. This sub-package handles the input (map, script and user operation on GUI) and the output (result file and visualization). GUI is included as a sub-package under this. The class Server controls the progress of simulation. Server processes the periodic traffic reports sent by the workers for output. The reports can include a range of information, such as vehicle positions, simulation statistics, etc. The server also handles workload balancing between workers. 
        
  - Sub-package processor.worker. This sub-package updates the simulated traffic at workers. This is where the actual simulation happens. The class Simulation handles the updates at each time step, such as updating the position of vehicles. The updates use the models in the ‘traffic’ package (see below). The processor.worker sub-package also handles exchange of data between workers, such as transferring a vehicle from one worker to another worker when the vehicle crosses the boundary between the simulation areas of the two workers. 
        
  - Sub-package processor.communication. This sub-package contains the communication architecture and the definition of the messages for server-worker communication and worker-worker communication.  
        
  - Class Simulator. This is the entry point for the standalone version. 

- The ‘traffic’ package: various models of the traffic system, such as road network, car-following model, lane-changing model, route assignment, etc. These models are heavily used by the server and the workers.

- The ‘resources’ package: images and data files used by the GUI. 


If you are familiar with the structure of the code, you can customize SMARTS on many fronts. The following are just some examples of what you can do. 


Example 1: Adding a routing algorithm
 
Suggestion: You need to make change in the package ‘traffic.routing’. The class Dijkstra is an example implementation of a routing algorithm. Normally, a routing algorithm needs to use certain attributes in road network (see the package ‘traffic.road’). To use a new routing algorithm, you also need to update the enum Algorithm in traffic.routing.Routing and the routingAlgorithm variable in common.Settings.  



Example 2: Collecting additional data from simulation 

Suggestion: Normally you need to do modification in several parts. First, you need to add variables into the traffic report class (processor.communication.message.Message_WS_TrafficReport). This class holds the data sent from worker to server during a simulation. You may need to create data types for serializing the data. Second, you need to modify the method processor.server.Server.processCachedReceivedTrafficReports(). There you can process the new data arrived at the server side. For example, you can call some methods to print out the new data. At the server side, you may also want to update the result outputting methods in processor.server.FileOutput. Third, you may need to modify the class processor.communication.message.Message_WW_Traffic and the method processor.worker.Worker.ProcessReceivedTraffic() if the additional data needs to be transferred between workers.  



Example 3: Adding new things to GUI
 
Suggestion: Modifying GUI can be very simple or very complex depending on what you want to achieve. Taking Example 1, if you just want to add a new algorithm as an option to GUI, you only need to update a corresponding JComboBox in processor.server.gui.ControlPanel_MiscConfig. If you need to display new things at real time, you need to modify processor.server.gui.MonitorPanel. Each frame shown in real time is a stack of several images. You may need to modify the method prepareObjectsImage() and similar methods in the class. 



Example 4: Adding new options to script file 

Suggestion: Make change in the class processor.server.ScriptLoader. Currently nearly all the options are linked to variables in common.Settings. 

 

 
