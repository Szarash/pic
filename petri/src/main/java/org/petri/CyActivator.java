package org.petri;

import java.util.Properties;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.osgi.framework.BundleContext;


/**
 * Used for starting app in Cytoscape. Registers PetriPanel with OSGI
 * and gets all necessary services used in the app.
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	/**
	 * Starts the bundle and registers petriPanel
	 */
	public void start(BundleContext bc) {
		CyAppAdapter adapter = getService(bc, CyAppAdapter.class);

		// Network Management
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
		CyNetworkNaming cyNetworkNamingServiceRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc,CyNetworkFactory.class);
		CyEventHelper eventHelperServiceRef = getService(bc,CyEventHelper.class);
		
		// Network View Management
		CyNetworkViewFactory cyNetworkViewFactoryServiceRef = getService(bc, CyNetworkViewFactory.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc,CyNetworkViewManager.class);
		CyLayoutAlgorithmManager cyLayoutAlgorithmManagerRef = getService(bc, CyLayoutAlgorithmManager.class);
		VisualMappingManager visualMappingManagerRef = getService(bc, VisualMappingManager.class);
		VisualMappingFunctionFactory visualMappingFunctionFactoryRefd = getService(bc, VisualMappingFunctionFactory.class,
				"(mapping.type=discrete)");

		// Petri Panel
		PetriPanel petriPanel = new PetriPanel(cyNetworkManagerServiceRef,
				cyNetworkNamingServiceRef,cyNetworkFactoryServiceRef,cyNetworkViewFactoryServiceRef,
				cyNetworkViewManagerServiceRef, eventHelperServiceRef,cyLayoutAlgorithmManagerRef,
				visualMappingManagerRef, visualMappingFunctionFactoryRefd, adapter);
		registerService(bc, petriPanel, CytoPanelComponent.class, new Properties());
	}
}
