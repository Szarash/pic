package org.petri;
	
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/**
 * Panel for using the app. Is added as an additional
 * tab in the control panel of Cytoscape and controls
 * both loading input files and firing/resetting the network. 
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class PetriPanel extends JPanel implements CytoPanelComponent {

	private static final long serialVersionUID = -6054408975485644227L;
	private JPanel jPanel;
	private CyNetwork petriNet;
	private PetriUtils petriUtils;
	private CreateEdgeTaskFactory createEdgeTaskFactory;
	private LoadNetworkTaskFactory loadNetworkTaskFactory;
	private LoadInvarTaskFactory loadInvarTaskFactory;
	private CreatePlaceTaskFactory createPlaceTaskFactory;
	private CreateTransitionTaskFactory createTransitionTaskFactory;
	private UpdateViewTaskFactory updateViewTaskFactory;
	private CheckRealizeTaskFactory checkRealizeTaskFactory;
	private ExportTaskFactory exportTaskFactory;
	private boolean firingMode; // Async = false, Sync = true
	private boolean random;

	/**
	 * Constructor
	 * @param cyNetworkManagerServiceRef CyNetworkManager
	 * @param cyNetworkNamingServiceRef CyNetworkNaming
	 * @param cyNetworkFactoryServiceRef CyNetworkFactory
	 * @param cyNetworkViewFactoryServiceRef CyNetworkViewFactory
	 * @param cyNetworkViewManagerServiceRef CyNetworkViewManager
	 * @param eventHelperServiceRef EventHelper
	 * @param cyLayoutAlgorithmManagerRef CyLayoutAlgorithmManager
	 * @param visualMappingManagerRef VisualMappingManager
	 * @param visualMappingFunctionFactoryRefd VisualMappingFunctionFactory for discrete mappings
	 * @param adapter CyAppAdapter
	 */
	public PetriPanel(final CyNetworkManager cyNetworkManagerServiceRef,
			final CyNetworkNaming cyNetworkNamingServiceRef,
			final CyNetworkFactory cyNetworkFactoryServiceRef,
			final CyNetworkViewFactory cyNetworkViewFactoryServiceRef,
			final CyNetworkViewManager cyNetworkViewManagerServiceRef, 
			final CyEventHelper eventHelperServiceRef,
			final CyLayoutAlgorithmManager cyLayoutAlgorithmManagerRef,
			final VisualMappingManager visualMappingManagerRef,
			final VisualMappingFunctionFactory visualMappingFunctionFactoryRefd,
			final CyAppAdapter adapter) {
		super();
		jPanel = new JPanel();					// Main panel, later added to PetriPanel
		jPanel.setBackground(Color.WHITE);
		jPanel.setLayout(new BorderLayout());
		JPanel top = new JPanel();				// Upper Panel of jPanel
		top.setLayout(new GridLayout(0,1));
		top.add(new Label("Control Panel for Petri Net App"));
		final JComboBox<String> invarHolder = new JComboBox<String>();
		loadInvarTaskFactory = new LoadInvarTaskFactory(invarHolder);
		JButton createBut = new JButton("Create new Petri net");
		createBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet != null) {						// Destroy previously loaded Petri Net, only one active at a time
					JFrame f = new JFrame("Warning");
					int choice = JOptionPane.showConfirmDialog(f, "Creating a new Petri net will destroy the previous one. Continue?");
					if (choice != JOptionPane.YES_OPTION) {
						return;
					}
					invarHolder.removeAllItems();
					cyNetworkManagerServiceRef.destroyNetwork(petriNet);
				}
				petriNet = cyNetworkFactoryServiceRef.createNetwork();	// New Network for Petri Net
				cyNetworkManagerServiceRef.addNetwork(petriNet);
				petriUtils = new PetriUtils(petriNet, cyNetworkViewManagerServiceRef,	// Used for updating views later on
						cyNetworkViewFactoryServiceRef, visualMappingManagerRef,
						cyLayoutAlgorithmManagerRef, adapter, visualMappingFunctionFactoryRefd); 
				createEdgeTaskFactory = new CreateEdgeTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				createPlaceTaskFactory = new CreatePlaceTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				createTransitionTaskFactory = new CreateTransitionTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				updateViewTaskFactory = new UpdateViewTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				exportTaskFactory = new ExportTaskFactory(petriNet, petriUtils);
				petriUtils.initializeColumns();
				petriUtils.createVisualStyle();
			}
		});
		top.add(createBut);
		JButton placeBut = new JButton("Create new place");
		placeBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				TaskIterator itr = createPlaceTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(placeBut);
		JButton transBut = new JButton("Create new transition");
		transBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				TaskIterator itr = createTransitionTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(transBut);
		JButton edgeBut = new JButton("Create new edge");
		edgeBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				TaskIterator itr = createEdgeTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(edgeBut);
		JButton viewBut = new JButton("Update views");
		viewBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				TaskIterator itr = updateViewTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(viewBut);
		JButton loadBut = new JButton("Load Petri net");		// Button for loading new Petri Nets
		loadBut.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				if (petriNet != null) {						// Destroy previously loaded Petri Net, only one active at a time
					JFrame f = new JFrame("Warning");
					int choice = JOptionPane.showConfirmDialog(f, "Loading a new Petri net will destroy the previous one. Continue?");
					if (choice != JOptionPane.YES_OPTION) {
						return;
					}
					invarHolder.removeAllItems();
					cyNetworkManagerServiceRef.destroyNetwork(petriNet);
				}
				petriNet = cyNetworkFactoryServiceRef.createNetwork();	// New Network for Petri Net
				cyNetworkManagerServiceRef.addNetwork(petriNet);
				petriUtils = new PetriUtils(petriNet, cyNetworkViewManagerServiceRef,	 // Used for updating views later on
						cyNetworkViewFactoryServiceRef, visualMappingManagerRef,
						cyLayoutAlgorithmManagerRef, adapter, visualMappingFunctionFactoryRefd);
				loadNetworkTaskFactory = new LoadNetworkTaskFactory(cyNetworkManagerServiceRef, cyNetworkNamingServiceRef,
						eventHelperServiceRef, petriNet, petriUtils);
				createEdgeTaskFactory = new CreateEdgeTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				createPlaceTaskFactory = new CreatePlaceTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				createTransitionTaskFactory = new CreateTransitionTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				updateViewTaskFactory = new UpdateViewTaskFactory(cyNetworkViewManagerServiceRef, petriNet);
				exportTaskFactory = new ExportTaskFactory(petriNet, petriUtils);
				TaskIterator petri = loadNetworkTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(petri);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(petri);
			}
		});
		top.add(loadBut);
		JButton loadInvarBut = new JButton("Load invariants");
		loadInvarBut.addActionListener(new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				// Reset InvarHolder on reading new file
				invarHolder.removeAllItems();
				CyNetworkView [] cnvs = new CyNetworkView[1];
				cyNetworkViewManagerServiceRef.getNetworkViews(petriNet).toArray(cnvs);
				CyNetworkView cnv = cnvs[0];
				for (View <CyEdge> edgeview : cnv.getEdgeViews()){ // Clear locked edge colours from previously selected invariant
					edgeview.clearValueLock(BasicVisualLexicon.EDGE_PAINT);
				}
				TaskIterator itr = loadInvarTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(loadInvarBut);
		JButton expoBut = new JButton("Export Petri net");
		expoBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				TaskIterator itr = exportTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(expoBut);
		JButton veriBut = new JButton("Verify Petri net");
		veriBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				petriUtils.verifyNet();
			}
		});
		top.add(veriBut);
		JButton resetBut = new JButton("Reset Petri net");	// Button for resetting tokens and fired
		resetBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				petriUtils.reset();
				TaskIterator itr = updateViewTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(resetBut);
		invarHolder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (invarHolder.getItemCount() == 0) { // Do not update on resetting the container
					return;
				}
				String invar = (String) invarHolder.getSelectedItem();
				CyNetworkView [] cnvs = new CyNetworkView[1];
				cyNetworkViewManagerServiceRef.getNetworkViews(petriNet).toArray(cnvs);
				CyNetworkView cnv = cnvs[0];
				for (View <CyEdge> edgeview : cnv.getEdgeViews()){ // Clear locked edge colours from previously selected invariant
					edgeview.clearValueLock(BasicVisualLexicon.EDGE_PAINT);
				}
				// Gather all transitions belonging to invariant and paint their edges
				String[] transitions = invar.split(", ");
				for (String tName : transitions) {
					boolean once = true;
					CyNode trans = null;
					if (tName.contains(" ")) {
						once = false;
						tName = tName.split(" ")[1];
					}
					for (CyNode n : petriNet.getNodeList()) {
						if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class).equals(tName)) {
							trans = n;
							break;
						}
					}
					if (trans == null) {
						JFrame f5 = new JFrame("Error during colouring of invariants");
						JOptionPane.showMessageDialog(f5, "Couldn't find transition " + tName);
					}
					Iterable<CyEdge>edges = petriNet.getAdjacentEdgeIterable(trans, CyEdge.Type.DIRECTED);
					for (CyEdge edge : edges) {
						View<CyEdge> edgeview = cnv.getEdgeView(edge);
						if (once) {
							edgeview.setLockedValue(BasicVisualLexicon.EDGE_PAINT, Color.CYAN);
						}
						else {
							edgeview.setLockedValue(BasicVisualLexicon.EDGE_PAINT, Color.ORANGE);
						}
					}
				}
			}
		});
		JButton invarBut = new JButton("Compute min. T-Invariants");	// Button for calculating invariants
		invarBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				JFrame f = new JFrame("Warning");
				int choice = JOptionPane.showConfirmDialog(f, "Computing invariants will take an extremely long time without you being able to exit it if your Petri net is really big.");
				if (choice != JOptionPane.YES_OPTION) {
					return;
				}
				invarHolder.removeAllItems();
				CyNode[] cyTransitionArray = petriUtils.getTransitions();
				CyNode[] cyPlaceArray = petriUtils.getPlaces();
				ArrayList<Integer[]> invars = petriUtils.invar(cyTransitionArray, cyPlaceArray);
				petriUtils.invars = invars;
				// Transform invariants for visual representation and add them to container
				for (int index=0; index<invars.size(); index++) {
					Integer[] invar = invars.get(index);
					String empty = "";
					Integer current = 0;
					for (int i=1; i<=invar.length; i++) {
						if (invar[invar.length-i] != 0) {
							String name = "";
							for (CyNode n : petriNet.getNodeList()) {
								if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("internal id", String.class).equals("t"+current.toString())) {
									name = petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class);
									break;
								}
							}
							if ((invar[invar.length-i]) > 1){
								empty += Integer.toString(invar[invar.length-i]) + " " + name + ", ";
							}
							else{
								empty += name + ", ";
							}
						}
						current++;
					}
					invarHolder.addItem(empty.substring(0, empty.lastIndexOf(",")));
				}
				TaskIterator itr = updateViewTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
				petriUtils.is_CTI();
			}
		});
		top.add(invarBut);
		JButton checkRealize = new JButton("Check Realizability");
		checkRealize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame f = new JFrame("Checking Realizability");
				int all = JOptionPane.showConfirmDialog(f, "Compute all realizable permutations?");
				JOptionPane.showMessageDialog(f, all);
				if (all != JOptionPane.NO_OPTION && all != JOptionPane.YES_OPTION) { // 1, 0
					return;
				}
				checkRealizeTaskFactory = new CheckRealizeTaskFactory(petriNet, invarHolder, petriUtils, (all == 0));
				TaskIterator itr = checkRealizeTaskFactory.createTaskIterator();
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
				String output = "";
				for (String perm : petriUtils.realize) {
					output += perm + "\n";	
				}
				// Now make output a little prettier by removing all '[' and ']'
				output.replaceAll("[", "");
				JOptionPane.showMessageDialog(f, "Perms done");
				JOptionPane.showMessageDialog(f, output);
			}
		});
		top.add(checkRealize);
		top.add(new Label("How often do you want to fire?"));
		final TextField times = new TextField("1");			// Used to determine how often to fire on button click
		top.add(times);
		JButton fireBut = new JButton("Fire Petri net"); 		// Button for firing the Petri Net
		fireBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (petriNet == null) {
					JFrame f = new JFrame("Error");
					JOptionPane.showMessageDialog(f, "No Petri net found.");
					return;
				}
				CyNode[] cyTransitionArray = petriUtils.getTransitions();
				for (int i=0; i<Integer.parseInt(times.getText()); i++) {				// Fire Petri Net x times
					petriUtils.fire(cyTransitionArray, firingMode, random);
				}
				TaskIterator itr = updateViewTaskFactory.createTaskIterator();
				adapter.getTaskManager().execute(itr);
				SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
				synTaskMan.execute(itr);
			}
		});
		top.add(fireBut);
		top.add(invarHolder);
		jPanel.add(top, BorderLayout.PAGE_START);
		
		JPanel bot = new JPanel();					// Lower panel of jPanel
		bot.setLayout(new GridLayout(0,2));
		JRadioButton radSync = new JRadioButton("Synchronous firing");
		firingMode = false;
		radSync.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				firingMode = true;
			}
		});
		JRadioButton radAsync = new JRadioButton("Asynchronous firing");
		radAsync.setSelected(true);
		radAsync.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				firingMode = false;
			}
		});
		random = true;
		JCheckBox rndSel = new JCheckBox("Randomize firing order");
		rndSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				random = !random;
			}
		});
		rndSel.setSelected(true);
		ButtonGroup frOpt = new ButtonGroup();
		frOpt.add(radSync);
		frOpt.add(radAsync);
		bot.add(radSync);
		bot.add(radAsync);
		bot.add(rndSel);
		jPanel.add(bot, BorderLayout.PAGE_END);
		this.add(jPanel);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;	// Add to Control Panel
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return "PIC";
	}	
}
