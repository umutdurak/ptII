/* The graph controller for vergil

 Copyright (c) 1998-2000 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Red (eal@eecs.berkeley.edu)
@AcceptedRating Red (johnr@eecs.berkeley.edu)
*/

package ptolemy.vergil.graph;

// FIXME: Replace with per-class imports.
import ptolemy.actor.*;
import ptolemy.actor.gui.*;
import ptolemy.gui.*;
import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.moml.*;
import ptolemy.vergil.toolbox.FigureAction;

import diva.gui.*;
import diva.gui.toolbox.*;
import diva.graph.GraphPane;
import diva.graph.NodeRenderer;
import diva.canvas.*;
import diva.canvas.connector.*;
import diva.canvas.event.*;
import diva.canvas.interactor.*;
import diva.canvas.toolbox.*;
import diva.util.Filter;
import diva.util.java2d.Polygon2D;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;

//////////////////////////////////////////////////////////////////////////
//// EditorGraphController
/**
A Graph Controller for the Ptolemy II schematic editor.  In addition to the
interaction allowed in the viewer, this controller allows nodes to be
dragged and dropped onto its graph.  Relations can be created by
control-clicking on the background.  Links can be created by control-clicking 
and dragging on a port or a relation.  In addition links can be created by
clicking and dragging on the ports that are inside an entity.
Anything can be deleted by selecting it and pressing
the delete key on the keyboard.

@author Steve Neuendorffer
@version $Id$
 */
public class EditorGraphController extends ViewerGraphController {

    /**
     * Create a new basic controller with default
     * terminal and edge interactors.
     */
    public EditorGraphController() {
	super();
    }

    // FIXME remove this methods.
    public Action getNewPortAction() {
	return _newPortAction;
    }

    // FIXME remove this methods.
    public Action getNewRelationAction() {
	return _newRelationAction;
    }

    /**
     * Initialize all interaction on the graph pane. This method
     * is called by the setGraphPane() method of the superclass.
     * This initialization cannot be done in the constructor because
     * the controller does not yet have a reference to its pane
     * at that time.
     */
    protected void initializeInteraction() {
        super.initializeInteraction();
        GraphPane pane = getGraphPane();

        // Create a listener that creates new relations
	_relationCreator = new RelationCreator();
        _relationCreator.setMouseFilter(_controlFilter);
        pane.getBackgroundEventLayer().addInteractor(_relationCreator);

        // Create a listener that creates new terminals
	//_portCreator = new PortCreator();
        //_portCreator.setMouseFilter(_controlFilter);
        //pane.getBackgroundEventLayer().addInteractor(_portCreator);

        // Create the interactor that drags new edges.
	_linkCreator = new LinkCreator();
	_linkCreator.setMouseFilter(_controlFilter);
	((CompositeInteractor)getPortController().getNodeInteractor()).addInteractor(_linkCreator);
        ((CompositeInteractor)getEntityPortController().getNodeInteractor()).addInteractor(_linkCreator);
	((CompositeInteractor)getRelationController().getNodeInteractor()).addInteractor(_linkCreator);

	LinkCreator linkCreator2 = new LinkCreator();
	linkCreator2.setMouseFilter(
           new MouseFilter(InputEvent.BUTTON1_MASK,0));
	((CompositeInteractor)getEntityPortController().getNodeInteractor()).addInteractor(linkCreator2);


        /*        // Create the interactor that drags new edges.
                  _connectedVertexCreator = new ConnectedVertexCreator();
                  _connectedVertexCreator.setMouseFilter(_shiftFilter);
                  getNodeInteractor().addInteractor(_connectedVertexCreator);
        */
    }

    ///////////////////////////////////////////////////////////////
    //// PortCreator

    // An action to create a new port.
    public class NewPortAction extends FigureAction {
	public NewPortAction() {
	    super("New External Port");
	    putValue("tooltip", "New External Port");
	    String dflt = "";
	    // Creating the renderers this way is rather nasty..
	    // Standard toolbar icons are 25x25 pixels.
	    NodeRenderer renderer = new PortController.PortRenderer();
	    Figure figure = renderer.render(null);
	    
	    FigureIcon icon = new FigureIcon(figure, 25, 25, 1, true);
	    putValue(diva.gui.GUIUtilities.LARGE_ICON, icon);
	}

	public void actionPerformed(ActionEvent e) {
	    super.actionPerformed(e);
	    GraphPane pane = getGraphPane();
	    double x;
	    double y;
	    if(getSourceType() == TOOLBAR_TYPE ||
	       getSourceType() == MENUBAR_TYPE) {	
		// no location in the action, so make something up.
		Point2D point = pane.getSize();    
		x = point.getX()/2;
		y = point.getY()/2;
	    } else {		    
		x = getX();
		y = getY();
	    }
		
	    PtolemyGraphModel graphModel = 
		(PtolemyGraphModel)getGraphModel();
	    final double finalX = x;
	    final double finalY = y;
	    final CompositeEntity toplevel = graphModel.getToplevel();
	    // FIXME use moml.  
	    toplevel.requestChange(new ChangeRequest(this,
		"Creating new Port in " + toplevel.getFullName()) {
		protected void _execute() throws Exception {
		    Port port = 
			toplevel.newPort(toplevel.uniqueName("port"));
		    Location location =
			new Location(port, 
				     port.uniqueName("_location"));
		    
		    double coords[] = new double[2];
		    coords[0] = ((int)finalX);
		    coords[1] = ((int)finalY);
		    location.setLocation(coords);
		}
	    });
	}
    }

    // An action to create a new relation.
    public class NewRelationAction extends FigureAction {
	public NewRelationAction() {
	    super("New Relation");
	    putValue("tooltip", "New Relation");
	    String dflt = "";
	    // Creating the renderers this way is rather nasty..
	    // Standard toolbar icons are 25x25 pixels.
	    NodeRenderer renderer = new RelationController.RelationRenderer();
	    Figure figure = renderer.render(null);
	    
	    FigureIcon icon = new FigureIcon(figure, 25, 25, 1, true);
	    putValue(diva.gui.GUIUtilities.LARGE_ICON, icon);
	}

	public void actionPerformed(ActionEvent e) {
	    super.actionPerformed(e);
	    GraphPane pane = getGraphPane();
	    double x;
	    double y;
	    if(getSourceType() == TOOLBAR_TYPE ||
	       getSourceType() == MENUBAR_TYPE) {	
		// no location in the action, so make something up.
		// FIXME this is a lousy way to do this.
		Point2D point = pane.getSize();    
		x = point.getX()/2;
		y = point.getY()/2;
	    } else {
		x = getX();
		y = getY();
	    }
	    
	    PtolemyGraphModel graphModel = 
		(PtolemyGraphModel)getGraphModel();
	    final double finalX = x;
	    final double finalY = y;
	    final CompositeEntity toplevel = graphModel.getToplevel();
	 		
	    // FIXME use MoML.  If no class is specifed in MoML, it should
	    // use the newRelation method.
	    toplevel.requestChange(new ChangeRequest(this,
		"Creating new Relation in " + toplevel.getFullName()) {
		protected void _execute() throws Exception {
		    Relation relation = 
			toplevel.newRelation(toplevel.uniqueName("relation"));
		    Vertex vertex =
			new Vertex(relation, relation.uniqueName("vertex"));
		    
		    double coords[] = new double[2];
		    coords[0] = ((int)finalX);
		    coords[1] = ((int)finalY);
		    vertex.setLocation(coords);
		}
	    });
	}
    }
 
    protected class PortCreator extends ActionInteractor {
	public PortCreator() {
	    super(_newPortAction);
	}
    }

    ///////////////////////////////////////////////////////////////
    //// RelationCreator
    
    protected class RelationCreator extends ActionInteractor {
	public RelationCreator() {
	    // FIXME don't ref VergilApplication.
	    super(_newRelationAction);
	}
    }
	
    ///////////////////////////////////////////////////////////////
    //// LinkCreator

    /** This class is an interactor that interactively drags edges from
     *  one terminal to another, creating a link to connect them.
     */
    protected class LinkCreator extends AbstractInteractor {

        /** Create a new edge when the mouse is pressed. */
        public void mousePressed(LayerEvent event) {
	    // Find the container
	    final CompositeEntity container = 
		    (CompositeEntity)getGraphModel().getRoot();
            Figure source = event.getFigureSource();
            NamedObj sourceObject = (NamedObj) source.getUserObject();
            FigureLayer layer = (FigureLayer) event.getLayerSource();
            final String name = container.uniqueName("link");

	    // Create the new edge.
	    Link link = new Link();
            
            try {
		// Add it to the editor
                addEdge(link,
                        sourceObject,
                        ConnectorEvent.TAIL_END,
                        event.getLayerX(),
                        event.getLayerY());

                // Add it to the selection so it gets a manipulator, and
                // make events go to the grab-handle under the mouse
                Figure ef = getFigure(link);
                getSelectionModel().addSelection(ef);
                ConnectorManipulator cm =
                       (ConnectorManipulator) ef.getParent();
                GrabHandle gh = cm.getHeadHandle();
                layer.grabPointer(event, gh);
            } catch (Exception ex) {
                MessageHandler.error("Drag connection failed:", ex);
            }
	}
    }

    /** An interactor that creates a new Vertex that is connected to a vertex
     *  in a relation
     
    protected class ConnectedVertexCreator extends AbstractInteractor {
	public void mousePressed(LayerEvent e) {
	    FigureLayer layer = (FigureLayer) e.getLayerSource();
	    Figure source = e.getFigureSource();
	    Node sourcenode = (Node) source.getUserObject();
	    NamedObj sourceObject = (NamedObj) sourcenode.getSemanticObject();

            if((sourceObject instanceof Vertex)) {
		Relation relation = (Relation)sourceObject.getContainer();
		Vertex vertex = null;
		try {
		    vertex = new Vertex(relation,
                            relation.uniqueName("vertex"));
                }
		catch (Exception ex) {
		    ex.printStackTrace();
		    throw new RuntimeException(ex.getMessage());
		}
		Node node = getGraphImpl().createNode(vertex);
		//addNode(node, e.getLayerX(), e.getLayerY());

		Edge edge = getGraphImpl().createEdge(null);
		//addEdge(edge,
		//	sourcenode,
		//ConnectorEvent.TAIL_END,
		//	e.getLayerX(),
                //e.getLayerY());

		// Add it to the selection so it gets a manipulator, and
		// make events go to the grab-handle under the mouse
		Figure nf = (Figure) node.getVisualObject();
		getSelectionModel().addSelection(nf);
		//		ConnectorManipulator cm = (ConnectorManipulator) ef.getParent();
		//GrabHandle gh = cm.getHeadHandle();
		layer.grabPointer(e, nf);
	    }
	}
    }

    */
    /** The interactor for creating new relations
     */
    private RelationCreator _relationCreator;

    /** The interactor for creating new vertecies connected
     *  to an existing relation
     */
    //   private ConnectedVertexCreator _connectedVertexCreator;

    /** The interactor for creating new terminals
     */
    private PortCreator _portCreator;

    /** The interactor for creating context sensitive menus.
     */
    private MenuCreator _menuCreator;

    /** The interactor that interactively creates edges
     */
    private LinkCreator _linkCreator;

    private Action _newPortAction = new NewPortAction();
    private Action _newRelationAction = new NewRelationAction();

    /** The filter for control operations
     */
    private MouseFilter _controlFilter = new MouseFilter(
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    /** The filter for shift operations
     */
    private MouseFilter _shiftFilter = new MouseFilter(
            InputEvent.BUTTON1_MASK,
            InputEvent.SHIFT_MASK);
}
