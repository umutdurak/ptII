/* An ordered list of objects with names.

 Copyright (c) 1997-1999 The Regents of the University of California.
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

@ProposedRating Green (eal@eecs.berkeley.edu)
@AcceptedRating Green (johnr@eecs.berkeley.edu)

NOTE: This class could leverage better
the underlying LinkedList class. E.g., it could be
capable of returning an enumeration sorted alphabetically by name.
This would require extensions to the interface, but not modifications
of the current interface.
*/

package ptolemy.kernel.util;

import collections.CorruptedEnumerationException;

import collections.LinkedList;
import collections.CollectionEnumeration;
import java.util.NoSuchElementException;
import java.io.Serializable;

//////////////////////////////////////////////////////////////////////////
//// NamedList
/**
An ordered list of objects with names.
The objects must implement the Nameable interface.
The names are required to be unique and non-null.
<p>
This class is intended to be used with small lists.
It is implemented as a linked list rather than hash table to
preserve ordering information, and thus would not provide efficient
name lookup for large lists.
<p>
An instance of this class may have a container, but that container
is only used for error reporting.

@author Mudit Goel, Edward A. Lee
@version $Id$
@see Nameable
@see LinkedList
@see collections.LinkedList
*/
public class NamedList implements Cloneable, Serializable {

    /** Construct an empty NamedList with no container.
     */
    public NamedList() {
        super();
    }

    /** Construct an empty list with a Nameable container.
     *  @param container The container (for error reporting).
     */
    public NamedList(Nameable container) {
        super();
        _container = container;
    }

    /** Copy constructor.  Create a copy of the specified list, but
     *  with no container. This is useful to permit enumerations over
     *  a list while the list continues to be modified.
     *  @param original The list to copy.
     */
    public NamedList(NamedList original) {
        super();
        _namedlist.appendElements(original.elements());
        _container = null;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Add an element to the end of the list.
     *  The element is required to have a name that does not coincide with
     *  that of an element already on the list.
     *  @param element Element to be added to the list.
     *  @exception IllegalActionException If the argument has no name.
     *  @exception NameDuplicationException If the name coincides with
     *   an element already on the list.
     */
    public synchronized void append(Nameable element)
            throws IllegalActionException, NameDuplicationException {
        // NOTE: This could use _insertAt(size(), element), but we don't
        // do that because in principle, insertLast should be more efficient.
        String newName = element.getName();
        if (newName == null) {
            throw new IllegalActionException(_container,
                    _NULL_NAME_EXCEPTION_STRING);
        }
        if( get(newName) == null ) {
            _namedlist.insertLast(element);
        } else {
            throw new NameDuplicationException(_container, element);
        }
    }

    /** Build an independent copy of the list.
     *  The elements themselves are not cloned.
     *  @return A new instance of NamedList.
     */
    public synchronized Object clone() {
        return new NamedList(this);
    }

    /** Enumerate the elements of the list.
     *  @see collections.LinkedList#elements()
     *  @return An enumeration of Nameable objects.
     */
    public synchronized CollectionEnumeration elements() {
        return _namedlist.elements();
    }

    /** Get the first element.
     *  @exception NoSuchElementException If the list is empty.
     *  @return The specified element.
     */
    public synchronized Nameable first() throws NoSuchElementException {
	return (Nameable)_namedlist.first();
    }

    /** Get an element by name.
     *  @param name The name of the desired element.
     *  @return The requested element if it is found, and null otherwise.
     */
    public synchronized Nameable get(String name) {
        CollectionEnumeration enum = _namedlist.elements();
        while( enum.hasMoreElements() ) {
            Nameable obj = (Nameable)enum.nextElement();
            if( name.equals(obj.getName()) ) {
                return obj;
            }
        }
        return null;
    }

    /** Return true if the specified object is on the list.
     *  @param element Element to be searched for in the list.
     *  @return A boolean indicating whether the element is on the list.
     */
    public synchronized boolean includes(Nameable element) {
        return _namedlist.includes(element);
    }

    /** Insert a new element after an element with the specified name.
     *  If there is no element with such name, then the new element is
     *  appended to the end of the list.
     *  @param name The name of the element after which to insert.
     *  @param element The element to insert.
     *  @exception IllegalActionException If the element to insert has no name.
     *  @exception NameDuplicationException If the element to insert has a
     *   name that coincides with one already on the list.
     */
    public synchronized void insertAfter(String name, Nameable element)
            throws IllegalActionException, NameDuplicationException {
        int index = _getIndexOf(name);
        if (index == -1) {
            // name doesn't exist in list
            append(element);
        } else {
            // name exists in list
            _insertAt((index+1), element);
        }
    }

    /** Insert a new element before an element with the specified name.
     *  If there is no element with such name, then the new element is
     *  added to the beginning of the list.
     *  @param name The name of the element before which to insert.
     *  @param element The element to insert.
     *  @exception IllegalActionException If the element to insert has no name.
     *  @exception NameDuplicationException If the element to insert has a
     *   name that coincides with one already on the list.
     */
    public synchronized void insertBefore(String name, Nameable element)
            throws IllegalActionException, NameDuplicationException {
        int index = _getIndexOf(name);
        if (index == -1) {
            // name doesn't exist in list
            prepend(element);
        } else {
            // name exists in the list
            _insertAt(index, element);
        }
    }

    /** Get the last element.
     *  @exception NoSuchElementException If the list is empty.
     *  @return The last element.
     */
    public synchronized Nameable last() throws NoSuchElementException {
	return (Nameable)_namedlist.last();
    }

    /** Add an element to the beginning of the list.
     *  The element is required to have a name that does not coincide with
     *  that of an element already on the list.
     *  @param element Element to be added to the list.
     *  @exception IllegalActionException If the argument has no name.
     *  @exception NameDuplicationException If the name coincides with
     *   an element already on the list.
     */
    public synchronized void prepend(Nameable element)
            throws IllegalActionException, NameDuplicationException {
        _insertAt(0, element);
    }

    /** Remove the specified element.  If the element is not on the
     *  list, do nothing.
     *  @param element Element to be removed.
     */
    public synchronized void remove(Nameable element) {
        _namedlist.removeOneOf(element);
    }

    /** Remove an element specified by name.  If no such element exists
     *  on the list, do nothing.
     *  @param name Name of the element to be removed.
     *  @return A reference to the removed object, or null if no
     *   object with the specified name is found.
     */
    public synchronized Nameable remove(String name) {
        Nameable element = get(name);
        if (element != null) {
            remove(element);
            return element;
        }
        return null;
    }

    /** Remove all elements from the list. */
    public synchronized void removeAll() {
        _namedlist.clear();
    }

    /** Return the number of elements in the list.
     *  @return A non-negative integer.
     */
    public synchronized int size() {
        return _namedlist.size();
    }


    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /*  Get the index of the element with the specified name.
     *  This is private because the
     *  interface to this class does not include the notion of indexes.
     *  @param name The name of the desired element.
     *  @return The index of the desired element, or -1 if it not on the list.
     */
    private int _getIndexOf(String name) {
        CollectionEnumeration enum = _namedlist.elements();
        int count = 0;
        while( enum.hasMoreElements() ) {
            Nameable obj = (Nameable)enum.nextElement();
            if( name.equals(obj.getName()) ) {
                return count;
            }
            count += 1;
        }
        return -1;
    }

    /*  Add an element at the specified position index in the list.
     *  The element is inserted just prior to that element that currently
     *  has the specified position index.  The index can range from 0..size()
     * (i.e., one past the current last index). If the index is equal to
     *  size(), the element is appended as the new last element.
     *  This is private because the
     *  interface to this class does not include the notion of indexes.
     *  @param index Where to insert the new element.
     *  @param element The element to insert.
     *  @exception IllegalActionException Element to insert has no name.
     *  @exception NameDuplicationException Element to insert has a
     *   name that coincides with one already on the list.
     */
    private void _insertAt(int index, Nameable element)
            throws IllegalActionException, NameDuplicationException {
        if( element.getName() == null ) {
            throw new IllegalActionException(_container,
                    _NULL_NAME_EXCEPTION_STRING);
        } else if (get(element.getName()) == null) {
            _namedlist.insertAt(index, element);
            return;
        }
        throw new NameDuplicationException(_container, element);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The container (owner) of this list.
    private Nameable _container;

    // A LinkedList containing the elements.
    private LinkedList _namedlist = new LinkedList();

    // Constant strings.
    private static final String _NULL_NAME_EXCEPTION_STRING =
    "Attempt to add an object with a null name to a NamedList.";
}
