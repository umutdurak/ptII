/* An analysis for detecting objects that must be aliased to eachother.

 Copyright (c) 2001 The Regents of the University of California.
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
@ProposedRating Red (cxh@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/

package ptolemy.copernicus.kernel;

import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

/**
An analysis that maps each local and field to the set of locals and
fields that may alias that value.  
*/
public class MaybeAliasAnalysis extends ForwardFlowAnalysis {
    public MaybeAliasAnalysis(UnitGraph g) {
        super(g);
        doAnalysis();
    }
    
    /** Return the set of other fields and locals that may reference
     *  the same object as the given field, at a point before
     *  the given unit.  If there is no alias information, meaning the
     *  field can be aliased to anything, then return null.
     */
    public Set getAliasesOfBefore(SootField field, Unit unit) {
        Map map = (Map)getFlowBefore(unit);
        if(map.get(field) == null) {
            return null;
        } else {
            Set set = new HashSet();
            set.addAll((Set)map.get(field));
            set.remove(field);
            return set;
        }
    }

    /** Return the set of other fields and locals that may reference
     *  the same object as the given field, at a point after
     *  the given unit.  If there is no alias information, meaning the
     *  field can be aliased to anything, then return null.
     */
     public Set getAliasesOfAfter(SootField field, Unit unit) {
        Map map = (Map)getFlowAfter(unit);
        if(map.get(field) == null) {
            return null;
        } else {
            Set set = new HashSet();
            set.addAll((Set)map.get(field));
            set.remove(field);
            return set;
        }
    }

    /** Return the set of other fields and locals that may reference
     *  the same object as the given local, at a point before
     *  the given unit.  If there is no alias information, meaning the
     *  field can be aliased to anything, then return null.
     */
    public Set getAliasesOfBefore(Local local, Unit unit) {
        Map map = (Map)getFlowBefore(unit);
        if(map.get(local) == null) {
            return null;
        } else {
            Set set = new HashSet();
            set.addAll((Set)map.get(local));
            set.remove(local);
            return set;
        }
    }

    /** Return the set of other fields and locals that may reference
     *  the same object as the given field, at a point after
     *  the given unit.  If there is no alias information, meaning the
     *  field can be aliased to anything, then return null.
     */
    public Set getAliasesOfAfter(Local local, Unit unit) {
        Map map = (Map)getFlowAfter(unit);
        if(map.get(local) == null) {
            return null;
        } else {
            Set set = new HashSet();
            set.addAll((Set)map.get(local));
            set.remove(local);
            return set;
        }
    }


    // Formulation:
    // The dataflow information is stored in a map from each
    // aliasable object (SootField or Local)
    // to a set of aliases.  Note that for each alias-set there 
    // is exactly one instance of HashSet
    // stored in the map.  This is implemented as a flow-insensitive
    // analysis.  
    // Method calls are handled conservatively, and we assume that
    // they affect the values of all
    // fields (i.e. aliases for all fields are killed.)
    // If no alias information exists for the object (i.e. it could be
    // aliased to everything) then it points to null.
    protected Object newInitialFlow() {
        return new HashMap();
    }

    protected void flowThrough(Object inValue, Object d, Object outValue) {
        Map in = (Map) inValue, out = (Map) outValue;
        Stmt unit = (Stmt)d;

        // By default, the out is equal to the in.
        copy(inValue, outValue);

        // if we have a method invocation, then alias information
        // for all fields is killed.
        // This is a safe flow-insensitive approximation.
        if(unit.containsInvokeExpr()) {
            for(Iterator i = out.keySet().iterator();
                i.hasNext();) {
                Object object = i.next();
                if(object instanceof SootField) {
                    _killAlias(out, object);
                }
            }
        }
        if(unit instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt)unit;

            Value lvalue = assignStmt.getLeftOp();
            Value rvalue = assignStmt.getRightOp();
            Object lobject = _getAliasObject(lvalue);
            Object robject = _getAliasObject(rvalue);
            if(lobject != null) {
                // First remove the left side from its
                // current set of aliases.  (Kill rule)
                _killAlias(out, lobject);

                if(robject != null) {
                    // If the type is aliasable,
                    if(lvalue.getType() instanceof ArrayType ||
                            lvalue.getType() instanceof RefType) {
                        
                        // The left side is now aliased to everything
                        // that the right side was aliased to before.
                        // (Gen rule)
                        _createAlias(out, lobject, robject);
                    }
                }
            }
        }
        // otherwise, the alias info is unchanged.
    }

    protected void copy(Object inValue, Object outValue) {
        Map in = (Map) inValue, out = (Map) outValue;
        // FIXME: is this necessary, since we are supposedly monotonic?
        out.keySet().retainAll(in.keySet());
        for(Iterator i = in.keySet().iterator(); i.hasNext();) {
            Object object = i.next();
            Set inSet = (Set)in.get(object);
            Set outSet = (Set)out.get(object);
            if(inSet == null) {
                out.put(object, null);
            } else {
                if(outSet == null) {
                    outSet = new HashSet();
                    out.put(object, outSet);
                }
                outSet.clear();
                outSet.addAll(inSet);
            }
        }
    }

    protected void merge(Object in1Value, Object in2Value, Object outValue) {
        Map in1 = (Map) in1Value, in2 = (Map) in2Value, out = (Map) outValue;
       
        LinkedList allKeys = new LinkedList();
        allKeys.addAll(in1.keySet());
        allKeys.addAll(in2.keySet());

        // Loop through all the variables.
        while(!allKeys.isEmpty()) {
            // Pick a variable.
            Object object = allKeys.removeFirst();
            // Get its sets of aliases.
            Set in1Set = (Set)in1.get(object);
            Set in2Set = (Set)in2.get(object);
            // If either has all aliases, then the output is all aliases.
            if(in1Set == null || in2Set == null) {
                out.put(object, null);
            } else {
                // Take the union of the two sets.
                // If we have any maybe aliases on either
                // input, then the output is the union.
                Set set = new HashSet();
                set.addAll(in1Set);
                set.addAll(in2Set);
                
                // This set is the alias set for
                // all elements in the set.
                for(Iterator i = set.iterator();
                    i.hasNext();) {
                    Object alias = i.next();
                    allKeys.remove(alias);
                    out.put(alias, set);
                }
                out.put(object, set);
            } 
        }
    }
   
    // Add lobject to the set of things that are aliased by rObject.
    private static void _createAlias(Map map, Object lObject, Object rObject) {
        // Get its new set of aliases.
        Set rset = (Set)map.get(rObject);
        if(rset == null) {
            // If the right side was aliased to everything, then 
            // the left side will also be aliased to everything.
            map.put(lObject, null);
            return;
        }
        
        // Add the object to the new set of aliases.
        rset.add(lObject);
        
        // And set its set of aliases.
        map.put(lObject, rset);
    }    

    private static Object _getAliasObject(Value value) {
        if(value instanceof Local) {
            return value;
        } else if(value instanceof FieldRef) {
            /// NOTE: we can do better 
            // if we return something that is
            // instance-dependent.
            return ((FieldRef)value).getField();
        } else if(value instanceof CastExpr) {
            return ((CastExpr)value).getOp();
        } else if(value instanceof NewExpr ||
                value instanceof NewArrayExpr) {
            return value;
        } else {
            return null;
        }
    }

    private static void _killAlias(Map map, Object lObject) {
        // Get its old set of aliases.
        Set lset = (Set)map.get(lObject);
        if(lset != null) {
            // And remove.
            lset.remove(lObject);
            map.put(lObject, null);
        }
    }
}
