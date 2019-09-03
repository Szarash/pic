package org.petri;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Factory for UpdateViewTasks
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class UpdateViewTaskFactory extends AbstractTaskFactory{
	private final CyNetworkViewManager cnvm;
	private final CyNetwork petriNet;
	
	/**
	 * Constructor
	 * @param cnvm CyNetworkViewManager
	 * @param petriNet Petri Net to be filled with data
	 */
	public UpdateViewTaskFactory(final CyNetworkViewManager cnvm,final CyNetwork petriNet) {
		this.cnvm = cnvm;
		this.petriNet = petriNet;
	}

	public TaskIterator createTaskIterator(){
		return new TaskIterator(new UpdateViewTask(petriNet, cnvm));
	}	
}
