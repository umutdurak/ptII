/* An IconLibrary stores icons in PTML files.

 Copyright (c) 1998-1999 The Regents of the University of California.
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

package ptolemy.schematic.util;

import ptolemy.kernel.util.*;
import java.util.Enumeration;
import collections.*;
import java.io.*;

//////////////////////////////////////////////////////////////////////////
//// IconLibrary
/**
An IconLibrary is the hierarchical object for organizing Icons.   An
IconLibrary contains a set of Icons, and a set of other IconLibraries
called suiblibraries.  

@author Steve Neuendorffer, John Reekie
@version $Id$
*/
public class IconLibrary extends PTMLObject {

    /** 
     * Create an IconLibrary object with the name "iconlibrary".
     * The library will have an empty string for the description and version.
     */
    public IconLibrary() {
        this("IconLibrary");
    }

    /** 
     * Create an IconLibrary object with the given name.
     * The library will have an empty string for the description and version.
     */
    public IconLibrary(String name) {
        super(name);
        _sublibraries = (CircularList) new CircularList();
        _icons = (CircularList) new CircularList();
   }

    /**
     * Add an Icon to this library
     */
    public void addIcon(Icon i) {
        _icons.insertFirst(i);
    }

    /**
     * Add a sublibrary to this library.
     */
    public void addSubLibrary(IconLibrary library) {
        _sublibraries.insertFirst(library);
    }

    /**
     * Test if the library contains an Icon with the given name
     */
    public boolean containsIcon(Icon icon) {
        return _icons.includes(icon);
    }

    /**
     * Test if the library contains the sublibrary
     */
    public boolean containsSubLibrary(IconLibrary lib) {
        return _sublibraries.includes(lib);
    }

    /**
     * Get the Icon that is stored in this IconLibrary with the specified
     * type signature
     */
    // public Icon getIcon(EntityType e) {
    //    return (Icon) _icons.at(e);
    //}
    //    public Icon getIcon(String s) {
    //    return (Icon) _icons.at(s);
    // }

    /** 
     * return the URL of the given sublibrary.
     */
    //public IconLibrary getSubLibrary(String name) {
    //    return (IconLibrary) _sublibraries.at(name);
    //}

    /** Return the version of this library.
     */
    //public String getVersion() {
    //    return _version;
    //}

    /**
     * Return the icons that are contained in this icon library.
     *
     * @return an enumeration of Icon
     */
    public Enumeration icons() {
        return _icons.elements();
    }

   /**
     * Remove an icon from this IconLibrary
     */
    public void removeIcon(Icon icon) {
        _sublibraries.removeOneOf(icon);
    }

    /**
     * Remove a sublibrary from this IconLibrary
     */
    public void removeSubLibrary(IconLibrary lib) {
        _sublibraries.removeOneOf(lib);
    }

    /** Set the string that represents the version of this library.
     */
    //public void setVersion(String s) {
    //    _version = s;
    //}

    /**
     * Return the names of subLibraries of this IconLibrary.   These names
     * are URL Strings which can be passed to other IconLibrary objects
     * for parsing.
     * @return an Enumeration of Strings
     */
    public Enumeration subLibraries() {
        return _sublibraries.elements();
    }

    /**
     * Return a string this representing Icon.
     */
    public String toString() {
        Enumeration els = subLibraries();
        String str = getName() + "({";
        while(els.hasMoreElements()) {
            IconLibrary il = (IconLibrary) els.nextElement();
	    str += il.toString();
        }
        str += "}{";
        els = icons();
         while(els.hasMoreElements()) {
            Icon icon = (Icon) els.nextElement();
	    str += "\n" + icon.toString();
        }        
        return str + "})";
    }

    private CircularList _sublibraries;
    private CircularList _icons;
}

