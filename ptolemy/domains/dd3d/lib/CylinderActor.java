package ptolemy.domains.dd3d.lib;

import ptolemy.kernel.util.*;
import ptolemy.data.*;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.actor.*;
import ptolemy.actor.lib.*;
import ptolemy.domains.dt.kernel.DTDebug;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.GraphicsConfiguration;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;


public class CylinderActor extends Shaded3DActor {

    public CylinderActor(TypedCompositeActor container, String name)
            throws IllegalActionException, NameDuplicationException {

        super(container, name);
        radius = new Parameter(this, "radius", new DoubleToken(0.5));
        height = new Parameter(this, "height", new DoubleToken(0.7));
    }
    
    public Parameter radius;
    public Parameter height;
    
    public Object clone(Workspace workspace) throws CloneNotSupportedException {
        CylinderActor newobj = (CylinderActor)super.clone(workspace);
        return newobj;
    }
    
    protected void _createModel() throws IllegalActionException {
       super._createModel();
       obj = new Cylinder((float) _getRadius(),(float) _getHeight(),Cylinder.GENERATE_NORMALS,_appearance);      
    }
    
    public Node getNodeObject() {
        return (Node) obj;
    }
    
    private double _getRadius() throws IllegalActionException {
        return ((DoubleToken) radius.getToken()).doubleValue();
    }
    private double _getHeight() throws IllegalActionException  {
        return ((DoubleToken) height.getToken()).doubleValue();
    }
    
    private Cylinder obj;
}
