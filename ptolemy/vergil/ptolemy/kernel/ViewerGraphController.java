/* The graph controller for the vergil viewer

 Copyright (c) 1999-2001 The Regents of the University of California.
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

package ptolemy.vergil.ptolemy.kernel;

// FIXME: Replace with per-class imports.
import ptolemy.actor.*;
import ptolemy.actor.gui.*;
import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.vergil.*;
import ptolemy.vergil.toolbox.*;
import ptolemy.vergil.ptolemy.*;
import ptolemy.gui.*;
import ptolemy.moml.*;

import diva.gui.*;
import diva.gui.toolbox.*;
import diva.graph.EdgeController;
import diva.graph.AbstractGraphController;
import diva.graph.GraphController;
import diva.graph.GraphPane;
import diva.graph.NodeController;
import diva.graph.NodeInteractor;
import diva.canvas.*;
import diva.canvas.connector.*;
import diva.canvas.event.*;
import diva.canvas.interactor.*;
import diva.canvas.toolbox.*;
import diva.util.Filter;
import diva.util.java2d.Polygon2D;

import java.awt.geom.Rectangle2D;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;

//////////////////////////////////////////////////////////////////////////
//// ViewerGraphController
/**
A graph controller for the Ptolemy II schematic viewer.
This controller allows nodes to be moved and context menus to be created,
but does not provide interaction for adding or removing nodes.
Right-clicking on the background will
create a context-sensitive menu for the graph.

@author Steve Neuendorffer
@version $Id$
*/
public class ViewerGraphController extends AbstractGraphController {

    /** Create a new basic controller with default
     *  terminal and edge interactors and default context menus.
     */
    public ViewerGraphController() {
	_attributeController = new AttributeController(this);
	_entityPortController = new EntityPortController(this);
	_entityController = new EntityController(this);
	_portController = new PortController(this);
	_relationController = new RelationController(this);
	_linkController = new LinkController(this);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return the controller for visible attributes.
     *  @return The controller for visible attributes.
     */
    public AttributeController getAttributeController() {
	return _attributeController;
    }

    /** Return the controller for entities.
     *  @return The controller for entities.
     */
    public EntityController getEntityController() {
	return _entityController;
    }

    /** Return the controller for ports of an entity.
     *  @return The controller for ports of an entity.
     */
    public EntityPortController getEntityPortController() {
	return _entityPortController;
    }

    /** Return the controller for links.
     *  @return The controller for links.
     */
    public LinkController getLinkController() {
        return _linkController;
    }

    /** Return the node controller appropriate for the given object.
     *  If the object is an instance of Vertex, then return the
     *  local relation controller.  If it is an instance of Location,
     *  then determine whether it is an Entity, Attribute, or Port,
     *  and return the appropriate default controller.
     *  If the argument is an instance of Port, then return the
     *  local port controller.
     *  @param object A Vertex, Location, or Port.
     */
    public NodeController getNodeController(Object object) {
	//return _controllerMap.get(getGraphModel().getNodeModel(object));
	if(object instanceof Vertex) {
            return _relationController;
        } else if(object instanceof Location) {
            Object semanticObject = getGraphModel().getSemanticObject(object);
            // Check to see whether
            // this is a NamedObj that contains a NodeControllerFactory.
            // If so, that could be used. If not, use the defaults
            // below.  This allows any object in Ptolemy II to have
            // its own controller, which means its own context menu
            // and its own interactors.
            if (semanticObject instanceof NamedObj) {
                List factoryList = ((NamedObj)semanticObject)
                        .attributeList(NodeControllerFactory.class);
                if(factoryList.size() > 0) {
                    NodeControllerFactory factory = (NodeControllerFactory)
                           factoryList.get(0);
                    NodeController controller = factory.create(this);
                    // Add to the selection dragger.
                    // NOTE: This should not be null, but in case it is,
                    // it is better to just have the selection dragger not
                    // work than to get a null pointer exception.
                    if(_selectionDragger != null) {
                        _selectionDragger.addSelectionInteractor(
                                (SelectionInteractor)controller
                                .getNodeInteractor());
                    }
                    return controller;
                } else {
                    // No controller factory, so return a default one.
                    if (semanticObject instanceof Entity) {
                        return _entityController;
                    } else if (semanticObject instanceof Attribute) {
                        return _attributeController;
                    } else if (semanticObject instanceof Port) {
                        return _portController;
                    } else {
                        throw new RuntimeException(
                       "Unrecognized object: " + semanticObject);
                   }
               }
           }
       } else if(object instanceof Port) {
           return _entityPortController;
       } else {
           throw new RuntimeException(
                    "Node with unknown semantic object: " + object);
       }
       // The compiler seems to lose track of the fact that all branches
       // above either return or throw an exception, so to silence an
       // error, we need this:
       return null;
   }

    /** Return the controller for ports of this composite.
     *  @return The controller for ports of this composite.
     */
    public PortController getPortController() {
	return _portController;
    }

    /** Return the controller for relations.
     *  @return The controller for relations.
     */
    public RelationController getRelationController() {
        return _relationController;
    }

    /** Initialize all interaction on the graph pane. This method
     *  is called by the setGraphPane() method of the superclass.
     *  This initialization cannot be done in the constructor because
     *  the controller does not yet have a reference to its pane
     *  at that time.
     */
    protected void initializeInteraction() {
        GraphPane pane = getGraphPane();

        // Create and set up the selection dragger
        _selectionDragger = new SelectionDragger(pane);
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_entityController.getNodeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_relationController.getNodeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_portController.getNodeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_linkController.getEdgeInteractor());
	_selectionDragger.addSelectionInteractor(
                (SelectionInteractor)_attributeController.getNodeInteractor());

        // MenuCreator
	_menuCreator = new MenuCreator(new SchematicContextMenuFactory(this));
	pane.getBackgroundEventLayer().addInteractor(_menuCreator);

	pane.getBackgroundEventLayer().setConsuming(false);
    }

    /**
     * Return the edge controller appropriate for the given node.
     */
    public EdgeController getEdgeController(Object object) {
        return _linkController;
    }

    /**
     * Set the controller for relations
     */
    public void setEntityController(EntityController controller) {
	_entityController = controller;
    }

    /**
     * Set the controller for relations
     */
    public void setEntityPortController(EntityPortController controller) {
	_entityPortController = controller;
    }

    /**
     * Set the controller for ports
     */
    public void setPortController(PortController controller) {
        _portController = controller;
    }

    /**
     * Set the controller for relations
     */
    public void setRelationController(RelationController controller) {
        _relationController = controller;
    }

    /**
     * Set the controller for links
     */
    public void setLinkController(LinkController controller) {
        _linkController = controller;
    }

    ///////////////////////////////////////////////////////////////////
    ////                        public variables                   ////

    // The selection interactor for drag-selecting nodes
    private SelectionDragger _selectionDragger;


    // The interactor for creating context sensitive menus on the
    // graph itself.
    private MenuCreator _menuCreator;

    // The controllers
    private EntityController _entityController;
    private AttributeController _attributeController;
    private EntityPortController _entityPortController;
    private PortController _portController;
    private RelationController _relationController;
    private LinkController _linkController;

    ///////////////////////////////////////////////////////////////////
    ////                          inner classes                    ////

    public static class SchematicContextMenuFactory
	    extends PtolemyMenuFactory {
	public SchematicContextMenuFactory(GraphController controller) {
	    super(controller);
	    addMenuItemFactory(new EditParametersFactory());
            // NOTE: Removed by EAL. I don't like this interface.
	    // addMenuItemFactory(new FormFrameFactory());
	    addMenuItemFactory(new PortDialogFactory());
	}

	public NamedObj _getObjectFromFigure(Figure source) {
	    return (NamedObj)getController().getGraphModel().getRoot();
	}
    }
}
