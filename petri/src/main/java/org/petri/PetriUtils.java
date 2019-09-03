package org.petri;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.ArrowShapeVisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;

/**
 * Utilities for managing Petri Nets
 * @author M. Gehrmann, M. Kirchner
 *
 */
public class PetriUtils {

	private CyNetwork petriNet;
	private CyNetworkViewManager cnvm;
	private CyNetworkViewFactory cnvf;
	private VisualMappingManager vmm;
	private CyLayoutAlgorithmManager clam;
	private CyAppAdapter adapter;
	private VisualMappingFunctionFactory vmffd;
	protected ArrayList<Integer[]> invars;
	protected ArrayList<String> realize;
	
	/**
	 * Constructor
	 * @param petriNet Petri Net to be filled with data
	 * @param cnvm Used to update views
	 * @param cnvf Used to create new view
	 * @param vmm Used to create visual style
	 * @param clam Used to create layout for new network
	 * @param adapter Used for executing tasks
	 * @param vmffd VisualMappingFunctionFactory for discrete mappings
	 */
	public PetriUtils(CyNetwork petriNet, CyNetworkViewManager cnvm, CyNetworkViewFactory cnvf, VisualMappingManager vmm,
			CyLayoutAlgorithmManager clam, CyAppAdapter adapter, VisualMappingFunctionFactory vmffd) {
		this.petriNet = petriNet;
		this.cnvm = cnvm;
		this.cnvf = cnvf;
		this.vmm = vmm;
		this.clam = clam;
		this.adapter = adapter;
		this.vmffd = vmffd;
		this.invars = new ArrayList<Integer[]>(); // holds the minimal t-invariants, should user decide to have them calculated
		this.realize = new ArrayList<String>();
	}

	/**
	 * Getter for all transitions among the nodes of petriNet
	 * @return CyNode[] containing all transitions
	 */
	public CyNode[] getTransitions() {
		int length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Get length of array
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Transition")) {
				length++;				
			}
		}
		CyNode[] cyTransitionArray = new CyNode[length];
		length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Insert transitions into array
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Transition")) {
				cyTransitionArray[length] = n;
				length++;
			}
		}
		return cyTransitionArray;
	}

	/**
	 * Getter for all places among the nodes of petriNet
	 * @return CyNode[] containing all places
	 */
	public CyNode[] getPlaces() {
		int length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Get length of array
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Place")) {
				length++;				
			}
		}
		CyNode[] cyPlaceArray = new CyNode[length];
		length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Insert places into array
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Place")) {
				cyPlaceArray[length] = n;
				length++;
			}
		}
		return cyPlaceArray;
	}

	/**
	 * Initializes Columns when creating a new Petri Net
	 */
	public void initializeColumns() {
		petriNet.getDefaultNodeTable().createColumn("internal id", String.class, true);
		petriNet.getDefaultNodeTable().createColumn("id", String.class, true);
		petriNet.getDefaultNodeTable().createColumn("tokens", Integer.class, false);
		petriNet.getDefaultNodeTable().createColumn("initial tokens", Integer.class, true);
		petriNet.getDefaultNodeTable().createColumn("type", String.class, true);
		petriNet.getDefaultNodeTable().createColumn("fired", Integer.class, false);
		petriNet.getDefaultEdgeTable().createColumn("internal id", String.class, true);
		petriNet.getDefaultEdgeTable().createColumn("weight", Integer.class, true);
	}
	
	/**
	 * Create Visual Style for new Petri Net
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createVisualStyle() {
		CyNode[] cyPlaceArray = getPlaces();
		CyNode[] cyTransitionArray = getTransitions();
		CyNetworkView cnv = cnvf.createNetworkView(petriNet);		// Setting up view
		Set <View<CyNode>> nodeviews = new HashSet<View<CyNode>>();	// Used for layout
		for (int i = 0; i<cyPlaceArray.length; i++) {	// Views for places, always red, token amount in label
			View<CyNode> nodeview = cnv.getNodeView(cyPlaceArray[i]);
			nodeview.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 35.0);
			nodeview.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.RED);
			nodeview.setLockedValue(BasicVisualLexicon.NODE_LABEL,
					petriNet.getDefaultNodeTable().getRow(nodeview.getModel().getSUID()).get("name", String.class)+"\n"
					+ Integer.toString(petriNet.getDefaultNodeTable().getRow(nodeview.getModel().getSUID()).get("tokens", Integer.class)));
			nodeviews.add(nodeview);
		}
		for (int i=0; i<cyTransitionArray.length; i++) { // Views for transitions, always white, since not fired yet
			View<CyNode> nodeview = cnv.getNodeView(cyTransitionArray[i]);
			nodeview.setLockedValue(BasicVisualLexicon.NODE_WIDTH, 35.0);
			nodeview.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR, Color.WHITE);
			nodeviews.add(nodeview);
		}
		CyEdge [] cyEdgeArray = new CyEdge[petriNet.getEdgeCount()];		//Generate views for edges
		petriNet.getEdgeList().toArray(cyEdgeArray);
		for (int i=0; i<petriNet.getEdgeCount(); i++) {
			View<CyEdge> edgeview = cnv.getEdgeView(cyEdgeArray[i]);
			edgeview.setLockedValue(BasicVisualLexicon.EDGE_TARGET_ARROW_SHAPE, ArrowShapeVisualProperty.ARROW);
		}
		cnvm.addNetworkView(cnv);
		
		VisualStyle vs = vmm.getVisualStyle(cnv);
		vs.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.BLACK);
		vs.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 1.0);
		DiscreteMapping shapeMap = (DiscreteMapping) vmffd.createVisualMappingFunction("type", String.class, BasicVisualLexicon.NODE_SHAPE);
		shapeMap.putMapValue("Transition", NodeShapeVisualProperty.RECTANGLE); // Transitions are squares, places are circles
		shapeMap.putMapValue("Place", NodeShapeVisualProperty.ELLIPSE);
		vs.addVisualMappingFunction(shapeMap);
		
		CyLayoutAlgorithm def = clam.getDefaultLayout(); // Apply default layout
		TaskIterator itr = def.createTaskIterator(cnv, def.getDefaultLayoutContext(), nodeviews, null);
		adapter.getTaskManager().execute(itr);
		SynchronousTaskManager<?> synTaskMan = adapter.getCyServiceRegistrar().getService(SynchronousTaskManager.class);
		synTaskMan.execute(itr);
		vs.apply(cnv);
		cnv.updateView();
	}

	/**
	 * Verify correctness of Petri Net
	 */
	public void verifyNet() {
		CyNode cyNodeArray[] = new CyNode[petriNet.getNodeCount()];
		CyEdge cyEdgeArray[] = new CyEdge[petriNet.getEdgeCount()];
		petriNet.getEdgeList().toArray(cyEdgeArray);
		petriNet.getNodeList().toArray(cyNodeArray);
		ArrayList<String> errors = new ArrayList<String>();
		for (CyNode n : cyNodeArray) {
			// Missing type
			if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class) == null) {
				errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) + ": missing type");
			}
			// Wrong type
			else if (!petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class).equals("Place") &&
					!petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class).equals("Transition")) {
				errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class)+ ": wrong type");
			}
			// Errors for places
			else if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class).equals("Place")) {
				// Missing tokens, no check for non-int because of columns type
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("tokens", Integer.class) == null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) + ": missing tokens");
				}
				// Missing initial tokens, no check for non-int because of columns type
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("initial tokens", Integer.class) == null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) + ": missing initial tokens");				
				}
				// Fired defined for a place even though it shouldn't be
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("fired", Integer.class) != null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) +
							": fired should not be defined for place");
				}
			}
			// Errors for transitions
			else if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class).equals("Transition")) {
				// Missing fired, no check for non-binary value
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("fired", Integer.class) == null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) + ": missing fired");
				}
				// Tokens defined for a transition even though they shouldn't be
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("tokens", Integer.class) != null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) +
							": tokens should not be defined for transition");
				// Initial Tokens defined for a transition even though they shouldn't be	
				}
				if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("initial tokens", Integer.class) != null) {
					errors.add(petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("name", String.class) +
							": initial tokens should not be defined for transition");
				}
			}
		}
		// Errors for edges
		for (CyEdge e : cyEdgeArray) {
			// Weight not defined or less than 1
			if (petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Integer.class) == null ||
					petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Integer.class) < 1) {
				errors.add(petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("name", String.class) + ": missing or negative weight");
			}
			// Do not give additional error messages for source/target node not having a type defined
			if (petriNet.getDefaultNodeTable().getRow(e.getSource().getSUID()).get("type", String.class) == null ||
					petriNet.getDefaultNodeTable().getRow(e.getTarget().getSUID()).get("type", String.class) == null) {
				continue;
			}
			// The nodes connected by this edge have the same type
			else if (petriNet.getDefaultNodeTable().getRow(e.getSource().getSUID()).get("type", String.class).equals(
					petriNet.getDefaultNodeTable().getRow(e.getTarget().getSUID()).get("type", String.class))) {
				errors.add(petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("name", String.class) + ": source and target node have same type");
			}
		}
		// Show errors
		JFrame f = new JFrame("Errors during verification");
		String msg = errors.toString().replaceAll(",", System.lineSeparator());
		msg = msg.replace("[", "").replace("]", "");
		if (msg.equals("")) {
			msg = "No errors found";
		}
		JOptionPane.showMessageDialog(f, msg);
	}
	
	/**
	 * Fire Petri Net. Goes through all transitions and checks which of them can fired, then does so for those.
	 * @param cyTransitionArray Array of transition type nodes
	 * @param random randomize firing order
	 * @param firingMode synchronous (true) or asynchronous (false) firing
	 */
	public void fire(CyNode[] cyTransitionArray, boolean firingMode, boolean random) {
		if (random) {
			List <CyNode> transitions = Arrays.asList(cyTransitionArray);
			Collections.shuffle(transitions);
			transitions.toArray(cyTransitionArray);
		}
		ArrayList<CyNode> fireableTransitions = new ArrayList<CyNode>();
		for (int i=0; i<cyTransitionArray.length; i++){
			Iterable<CyEdge>incomingEdges = petriNet.getAdjacentEdgeIterable(cyTransitionArray[i], CyEdge.Type.INCOMING); // Incoming Edges for a Transition
			boolean fireable = true;
			for (CyEdge incomingEdge: incomingEdges){
				if (petriNet.getDefaultNodeTable().getRow(incomingEdge.getSource().getSUID()).get("tokens", Integer.class) < petriNet.getDefaultEdgeTable().getRow(incomingEdge.getSUID()).get("weight", Integer.class)){
					fireable = false; // Transition can't be fired if the source of an incoming edge does not have enough tokens
					break;
				}
			}
			if (fireable) {
				fireableTransitions.add(cyTransitionArray[i]);
				Iterable<CyEdge>incomingEdges1 = petriNet.getAdjacentEdgeIterable(cyTransitionArray[i], CyEdge.Type.INCOMING);
				// Remove tokens from sources of incoming before continuing for the next transition. Prevents negative token amounts
				for (CyEdge incomingEdge: incomingEdges1){	
					Integer newAmount = petriNet.getDefaultNodeTable().getRow(incomingEdge.getSource().getSUID()).get("tokens", Integer.class)
							- petriNet.getDefaultEdgeTable().getRow(incomingEdge.getSUID()).get("weight", Integer.class);
					petriNet.getDefaultNodeTable().getRow(incomingEdge.getSource().getSUID()).set("tokens", newAmount);
				}
				if (!firingMode) {
					break;
				}
			}
		}
		for (int i = 0; i<fireableTransitions.size(); i++){
			Iterable<CyEdge>outgoingEdges = petriNet.getAdjacentEdgeIterable(fireableTransitions.get(i),  CyEdge.Type.OUTGOING);
			// Add tokens to targets of outgoing edges
			for (CyEdge outgoingEdge: outgoingEdges){
				Integer newAmount = petriNet.getDefaultNodeTable().getRow(outgoingEdge.getTarget().getSUID()).get("tokens", Integer.class)
						+ petriNet.getDefaultEdgeTable().getRow(outgoingEdge.getSUID()).get("weight", Integer.class);
				petriNet.getDefaultNodeTable().getRow(outgoingEdge.getTarget().getSUID()).set("tokens", newAmount);
			}
		}
		for (int i = 0; i<cyTransitionArray.length; i++){
			if (fireableTransitions.contains(cyTransitionArray[i])){	// Updates, which transitions have fired
				petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("fired", 1);
			}
			else {
				petriNet.getDefaultNodeTable().getRow(cyTransitionArray[i].getSUID()).set("fired", 0);	// Reset which transitions were fired
			}
		}	
	}
	
	/**
	 * Resets state of Petri net to default values (tokens = initial tokens f.a. places, fired = 0 f.a. transitions)
	 */
	public void reset() {
		CyNode[] cyTransitionArray = getTransitions();
		for (CyNode n : cyTransitionArray) {									
			petriNet.getDefaultNodeTable().getRow(n.getSUID()).set("fired", 0);	// Reset how often transitions were fired
		}
		CyNode[] cyPlaceArray = getPlaces();
		for (CyNode n : cyPlaceArray) {
			petriNet.getDefaultNodeTable().getRow(n.getSUID()).set("tokens", petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("initial tokens", Integer.class));
		}
	}
	
	/**
	 * Calculates invariants of Petri Net.
	 * The transitions are represented within the array in REVERSE ORDER.
	 * @param cyTransitionArray CyNode[] containing all transitions
	 * @param cyPlaceArray CyNode[] containing all places
	 * @return invariants contains all invariants as Integer[] elements of an ArrayList
	 */
	public ArrayList<Integer[]> invar(CyNode[] cyTransitionArray, CyNode[] cyPlaceArray) {
		//Creating incidenceMatrix
		Integer[][] incidenceMatrix = new Integer[cyTransitionArray.length][cyPlaceArray.length]; 
		for (Integer m = 0; m < cyTransitionArray.length; m++) {
			for (Integer n = 0; n < cyPlaceArray.length; n++){
				incidenceMatrix[m][n] = 0;
				Iterable<CyEdge>incomingEdges = petriNet.getAdjacentEdgeIterable(cyPlaceArray[n], CyEdge.Type.INCOMING);
				Iterable<CyEdge>outgoingEdges = petriNet.getAdjacentEdgeIterable(cyPlaceArray[n], CyEdge.Type.OUTGOING);
				for (CyEdge incomingEdge : incomingEdges){
					if (cyTransitionArray[m].getSUID().equals(incomingEdge.getSource().getSUID())){
						incidenceMatrix[m][n] = petriNet.getDefaultEdgeTable().getRow(incomingEdge.getSUID()).get("weight", Integer.class);
					}
				}
				for (CyEdge outgoingEdge : outgoingEdges){
					if (cyTransitionArray[m].getSUID().equals(outgoingEdge.getTarget().getSUID())){
						incidenceMatrix[m][n] = -1 * petriNet.getDefaultEdgeTable().getRow(outgoingEdge.getSUID()).get("weight", Integer.class);
					}
				}
			}
		}
		ArrayList<Integer[]> incMatList = new ArrayList<Integer[]>(Arrays.asList(incidenceMatrix));
		//Creating identity matrix
		Integer[][] identity = new Integer[cyTransitionArray.length][cyTransitionArray.length];
		for (Integer n = 0; n < cyTransitionArray.length; n++){
			for (Integer n2 = 0; n2 < cyTransitionArray.length; n2++){
				if (n.equals(n2)){
					identity[n][n2] = 1;
				}
				else {
					identity[n][n2] = 0;
				}
			}
		}
		ArrayList<Integer[]> identList = new ArrayList<Integer[]>(Arrays.asList(identity));
		//Iterating over all columns/places to find incoming and outgoing edges that nullify each other
		for (Integer p = cyPlaceArray.length - 1; p > -1; p--){
			ArrayList<Integer> posPositions = new ArrayList<Integer>();
			ArrayList<Integer> negPositions = new ArrayList<Integer>();
			for (Integer t = 0; t < incMatList.size(); t++){
				if (incMatList.get(t)[p] > 0){
					posPositions.add(t);
				} else if (incMatList.get(t)[p] < 0){
					negPositions.add(t);
				}
			}
			//Calculating the new Lines in the incidence and the identity matrix according to the prior found edges and their according transitions
			ArrayList<Integer[]> newLines = new ArrayList<Integer[]>();
			ArrayList<Integer[]> newIdentLines = new ArrayList<Integer[]>();
			for (Integer pos = 0; pos<posPositions.size(); pos++){
				for (Integer neg = 0; neg<negPositions.size(); neg++){
					Integer a = incMatList.get(posPositions.get(pos))[p];
					Integer b = -1 * incMatList.get(negPositions.get(neg))[p];
					if (a != 0){
						while (b != 0){
							if (a > b){
								a = a - b;
							}
							else{
								b = b - a;
							}
						}
					}
					Integer ggT = a;
					Integer kgV = incMatList.get(posPositions.get(pos))[p] * (-1 * incMatList.get(negPositions.get(neg))[p] / ggT);
					Integer posDiv = kgV / incMatList.get(posPositions.get(pos))[p];
					Integer negDiv = -1 * kgV / incMatList.get(negPositions.get(neg))[p];
					Integer[] newLine = new Integer[cyPlaceArray.length];
					Integer[] newIdentLine = new Integer[cyTransitionArray.length];
					for (Integer place=0; place<cyPlaceArray.length; place++){
						newLine[place] = posDiv * incMatList.get(posPositions.get(pos))[place] + negDiv * incMatList.get(negPositions.get(neg))[place];
					}
					for (Integer transition=0; transition<cyTransitionArray.length; transition++){
						newIdentLine[transition] = posDiv * identList.get(posPositions.get(pos))[transition] + negDiv * identList.get(negPositions.get(neg))[transition];
					}
					newLines.add(newLine);
					newIdentLines.add(newIdentLine);
				}
			}
			//Deleting the used rows 
			ArrayList<Integer> positionsToDelete = new ArrayList<Integer>();
			positionsToDelete.addAll(negPositions);
			positionsToDelete.addAll(posPositions);
			Collections.sort(positionsToDelete, Collections.reverseOrder());
			for (int pos: positionsToDelete){
				incMatList.remove(pos);
				identList.remove(pos);
			}
			incMatList.addAll(newLines);
			identList.addAll(newIdentLines);
		}
		//Filtering the invariants out of the finished and processed identity matrix
		ArrayList<Integer[]> invariants = new ArrayList<Integer[]>();
		for (Integer m = 0; m<incMatList.size(); m++){
			Boolean isZero = true;
			for (Integer n = 0; n< cyPlaceArray.length; n++){
				if(incMatList.get(m)[n] != 0){
					isZero = false;
					break;
				}
			}
			if (isZero) {
				invariants.add(identList.get(m));
			}
		}
		ArrayList<Integer> values = new ArrayList<Integer>();
		ArrayList<Integer[]> sortedcand = new ArrayList<Integer[]>();
		for (Integer x = 0; x < invariants.size(); x++){
			Integer tempcount = 0;
			for (Integer y = 0; y < invariants.get(x).length; y++){
				tempcount += invariants.get(x)[y];
			}
			values.add(tempcount);
		}
		Integer max = values.get(values.indexOf(Collections.max(values)));
		for (Integer z = 0; z < values.size(); z++){
			Integer minIndex = values.indexOf(Collections.min(values));
			sortedcand.add(invariants.get(minIndex));
			values.set(minIndex, max+1);
		}
		Integer currRank = 0;
		ArrayList<Integer[]> invariantcont = new ArrayList<Integer[]>();
		for (Integer l = 0; l < sortedcand.size(); l++){
			invariantcont.add(sortedcand.get(l));
			Integer newRank = rank(invariantcont.toArray(new Integer[invariantcont.size()][invariantcont.get(0).length]));
			if (newRank > currRank){
				currRank = newRank;
			}
			else{
				invariantcont.remove(invariantcont.size() - 1);
			}
		}
		return invariantcont;
	}

	public Integer rank(Integer[][] matrix){Double [][] doublemat = new Double[matrix.length][matrix[0].length];
		for (Integer a = 0; a < matrix.length; a++){
			for (Integer b = 0; b < matrix[0].length; b++){
				doublemat[a][b] = Double.valueOf((matrix[a][b]));
			}
		}
		Integer pivot_r = 0;
		Integer pivot_c = 0;
		Integer m = doublemat.length;
		Integer n = doublemat[0].length;
		while (pivot_r < m && pivot_c < n){
			Integer largest = 0;
			for ( Integer i = pivot_r; i < m; i++){
				if (Math.abs(doublemat[i][pivot_c]) > Math.abs(doublemat[largest][pivot_c])){
					largest = i;
				}
			}
			if (doublemat[largest][pivot_c] != 0){
				//swap rows
				for (int x = 0; x < n; x++){
					Double temp_val = doublemat[pivot_r][x];
					doublemat[pivot_r][x] = doublemat[largest][x];
					doublemat[largest][x] = temp_val;
				}
				for (int y = pivot_r + 1; y < m; y++){
					Double div = doublemat[y][pivot_c] / doublemat[pivot_r][pivot_c];
					doublemat[y][pivot_c] = 0.0;
					for (int z = pivot_c + 1; z < n; z++){
						doublemat[y][z] = doublemat[y][z] - (doublemat[pivot_r][z] * div);
					}
				}
				pivot_r++;
				pivot_c++;
			}
			else {
				pivot_c++;
			}
		}
		Integer rank = 0;
		for (Integer r = 0; r < doublemat.length; r++){
			boolean status = false;
			for (Integer c = 0; c < doublemat[0].length; c++){
				if (doublemat[r][c] != 0){
					status = true;
				}
			}
			if (status == true){
				rank++;
			}
		}
		return rank;
	}
	/**
	 * Checks whether network is CTI or not
	 */
	public void is_CTI() {
		int length = 0;
		for (CyNode n : petriNet.getNodeList()) {	// Get length of array
			String ntype = (String) (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("type", String.class));
			if (ntype.equals("Transition")) {
				length++;				
			}
		}
		Integer[] cti = new Integer[length];
		for (int i=0; i<length; i++) {
			cti[i] = 0;
		}
		//iterating over all invariants and reversing them to the correct order, if transition is covered set its according integer in cti to 1
		for (Integer[] invar : invars) {
			Integer[] revInvar = new Integer[invar.length];
			for (int i=1; i<=invar.length; i++) {
				revInvar[invar.length-i] = invar[i-1];
			}
			for (int i=0; i<invar.length; i++) {
				if (revInvar[i]>0) {
					cti[i] = 1;
				}
			}
		}
		boolean all_cti = true;
		//iterate over the created Integerlist cti and check if all transitions are covered, if not set all_cti to false and add the missing transition to the not_cti list
		ArrayList<Integer> not_cti = new ArrayList<Integer>();
		for (int i=0; i<cti.length; i++) {
			if (cti[i] == 0) {
				all_cti = false;
				not_cti.add(i);
			}
		}
		//returning the information to the user 
		JFrame f = new JFrame("Check for CTI");
		if (all_cti) {
			JOptionPane.showMessageDialog(f, "Network is CTI");
		}
		else {
			String msg = "Network is not CTI\nNon-CTI Transitions:\n";
			for (Integer trans : not_cti) {
				CyNode node = null;
				for (CyNode n : petriNet.getNodeList()) {
					if (petriNet.getDefaultNodeTable().getRow(n.getSUID()).get("internal id", String.class).equals("t"+Integer.toString(trans))) {
						node = n;
						break;
					}
				}
				msg += petriNet.getDefaultNodeTable().getRow(node.getSUID()).get("name", String.class) + "\n";
			}
			JOptionPane.showMessageDialog(f, msg);
		}
	}
	
	/**
	 * Checks, whether a string is NOT an integer
	 * @param toCheck string to check
	 * @return true, if not an int, false if int
	 */
	public static boolean not_int(String toCheck) {
		boolean invalid = false;
		Scanner sc = new Scanner(toCheck.trim());
	    if(!sc.hasNextInt(10)) {
	    	invalid = true;
	    }
	    else {
	    sc.nextInt(10);
	    invalid = sc.hasNext();
	    }
	    sc.close();	
	    return invalid;
	}

	@SuppressWarnings("unchecked")
	public void namingsense(ArrayList<CyNode> transitions, ArrayList<String> used,
			HashMap<String, Integer> times, HashMap<String, Integer> tokens,
			ArrayList<String> realize, boolean all) {
		for (CyNode trans : transitions) {
			if (!all && !realize.isEmpty()) { // This one just doesn't seem to work at all, don't get why though
				return;
			}
			// Check if transitions can fire
			boolean canFire = true;
			for (CyEdge e : petriNet.getAdjacentEdgeIterable(trans, CyEdge.Type.INCOMING)) {
				if (tokens.get(petriNet.getDefaultNodeTable().getRow(e.getSource().getSUID()).get("name", String.class))
						< petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Integer.class)) {
					canFire = false;
					break;
				}
			}
			if (!canFire) {
				continue;
			}
			// Copy transitions and times for recursion
			ArrayList<CyNode> newTransitions = (ArrayList<CyNode>) transitions.clone(); // Not sure if shallow copy is enough
			HashMap<String, Integer> newTimes = (HashMap<String, Integer>) times.clone();
			int count = times.get(petriNet.getDefaultNodeTable().getRow(trans.getSUID()).get("name", String.class));
			// Lower count in newTimes or remove trans from newTransitions
			if (count > 1){
				newTimes.put(petriNet.getDefaultNodeTable().getRow(trans.getSUID()).get("name", String.class), count-1);
			}
			else {
				newTransitions.remove(trans);
			}
			// Copy tokens for recursion
			HashMap<String, Integer> newTokens = (HashMap<String, Integer>) tokens.clone();
			// Add tokens for outgoing edges
			for (CyEdge e : petriNet.getAdjacentEdgeIterable(trans, CyEdge.Type.OUTGOING)) {
				newTokens.put(petriNet.getDefaultNodeTable().getRow(e.getTarget().getSUID()).get("name", String.class),
						tokens.get(petriNet.getDefaultNodeTable().getRow(e.getTarget().getSUID()).get("name", String.class)) +
						petriNet.getDefaultEdgeTable().getRow(e.getSUID()).get("weight", Integer.class));
			}
			ArrayList<String> newUsed = (ArrayList<String>) used.clone();
			newUsed.add(petriNet.getDefaultNodeTable().getRow(trans.getSUID()).get("name", String.class));
			if (newTransitions.isEmpty()) { // Recursion is done, leaf has been reached
				realize.add(newUsed.toString());
			}
			else { // Recursion Step, Fix this to search only for one.
				namingsense(newTransitions, newUsed, newTimes, newTokens, realize, all);
			}
		}
		return;
	}
}
