### Petri net implementation for Cytoscape (PIC) 1.0

Description:

PIC is an app that implements Petri nets for Cytoscape. 
It can both load Petri nets from input files of several
different formats as well as manually create Petri nets
while ensuring their formal correctness, and finally, 
simulate or export them.

Features include:

 - Loading Petri nets from input files
     - supported formats: SBML, PNT, APNN, RL, Metatool
 - Manual creation and modification of Petri nets
 - Export of created and/or modified Petri nets to SBML Level 3 Version 1 format
 - Verification of formal correctness
 - Simulation of Petri nets (synchronous / asynchronous firing, randomized firing order)

Installation:

1. Download the package and unpack it.
2. Move the PIC-x.x.jar into your \Users\YourUsername\CytoscapeConfiguration\3\apps\installed
3. Start Cytoscape. PIC will open automatically as a new tab in your controlbar.

Instructions:

PIC features an easy to learn and work with GUI.
"Create new Petri net" will destroy your current Petri net and create a blank new network which you can build from scratch. A prompt will warn you about destroying your current Petri net.
"Create new place" adds an additional place. A prompt will give you the opportunity to give it a name and an initial token amount.
"Create new transition" adds an additional transition. A prompt will give you the opportunity to give it a name.
"Create new edge" adds an additional edge. A prompt will give you the opportunity to give it a source node, a target node and a weight. It will require the internal ID's of your nodes, which can be found in the Node Table beneath your visual window.
"Update views" updates the visual window to the proper layout, if you decide to edit your Petri net with the built-in Cytoscape possibilities.
"Load Petri net" imports a Petri net. A prompt will give you the opportunity to select the file from which you want to load your Petri net. Supported formats are: SBML, PNT, APNN, RL, Metatool
"Export Petri net" exports your Petri net. A prompt will give you the opportunity to select the directory and the name which will be used for the created SBML Level 3 Version 1 file.
"Verify Petri net" checks if your Petri net fullfills the formal Petri net standards.
"Reset Petri net" resets your Petri net to its initial state. This is useful after Simulations.
Simulation:
In this part of the GUI you can choose how often you want to fire your Petri net via the input field. You can start the simulation by pressing the "Fire Petri net" Button.
Below that you can choose between synchronous firing and asynchronous firing. Synchronous firing will fire every possible transition at the same time, asynchronous firing fires the transitions one by one.
If you check "Randomize firing order" the order of transitions will be randomized, to decide which transition is allowed to use tokens from a place first. If this is unchecked the order will always be the order of transitions in via their internal ID.

Additional comments:

If you decide to manually modify your Petri net with the Cytoscape GUI, you have to press "Update Views" in our GUI to update the visualization. This includes changing values like weight or token amount.

Known Problems:

Adding a edge that does not conform to Petri net formalities throws the appropiate error but does cycle itself in a task which forces the user to forcefully close Cytoscape.

Authors:

Gehrmann M, Kirchner M, Rieser J, Ackermann J, Koch I

You can contact us at marcel.gehrmann@stud.uni-frankfurt.de or mariuskirchner@stud.uni-frankfurt.de for further support or feedback, which would be greatly appreciated.
