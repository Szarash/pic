package org.petri;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Constructor for ExportTasks
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class ExportTaskFactory extends AbstractTaskFactory{
	private final CyNetwork petriNet;
	private final PetriUtils petriUtils;
	
	/**
	 * Constructor
	 * @param petriNet Petri net to be exported
	 * @param petriUtils Used for getting places and transitions of petriNet
	 */
	public ExportTaskFactory(final CyNetwork petriNet, final PetriUtils petriUtils) {
		this.petriNet = petriNet;
		this.petriUtils = petriUtils;
	}

	public TaskIterator createTaskIterator(){
		return new TaskIterator(new ExportTask(petriNet, petriUtils.getPlaces(), petriUtils.getTransitions()));
	}	
}
