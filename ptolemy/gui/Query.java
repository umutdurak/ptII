/* Query dialog.

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
@AcceptedRating Red (eal@eecs.berkeley.edu)

*/

package ptolemy.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.IllegalArgumentException;
import javax.swing.*;

import collections.LinkedList;

//////////////////////////////////////////////////////////////////////////
//// Query
/**
Create a query with various types of entry boxes and controls.

@author  Edward A. Lee, Manda Sutijono
@version $Id$
*/
public class Query extends JPanel {

    /** Construct a panel with no queries in it.
     */
    public Query() {
        _grid = new GridBagLayout();
        setLayout(_grid);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Create an on-off check box.
     *  @param name The name used to identify the entry (when calling get).
     *  @param label The label to attach to the entry.
     *  @param defaultValue Default value (true for on).
     */
    public void addCheckBox(String name, String label, boolean defaultValue) {
        JLabel lbl = new JLabel(label + ": ");
        lbl.setBackground(_background);
        JRadioButton checkbox = new JRadioButton();
        checkbox.setBackground(_background);
        checkbox.setSelected(defaultValue);
        _addPair(lbl, checkbox);
        _entries.put(name, checkbox);
        // Add the listener last so that there is no notification
        // of the first value.
        checkbox.addItemListener(new CheckBoxListener(name));
    }

    /** Create a simple one-line text display, a non-editable value that
     *  is set externally using the setDisplay() method.
     *  @param name The name used to identify the entry (when calling get).
     *  @param label The label to attach to the entry.
     *  @param defaultValue Default string to display.
     */
    public void addDisplay(String name, String label, String defaultValue) {
        JLabel lbl = new JLabel(label + ": ");
        lbl.setBackground(_background);
        // NOTE: JLabel would be a reasonable choice here, but at
        // least in the current version of swing, JLabel.setText() does
        // not work.
        JTextArea displayField = new JTextArea(defaultValue, 1, 10);
        displayField.setBackground(_background);
        _addPair(lbl, displayField);
        _entries.put(name, displayField);
    }

    /** Create a single-line entry box with the specified name, label, and
     *  default value.  To control the width of the box, call setTextWidth()
     *  first.  To ensure uniformity, a colon and a space are appended
     *  to the end of the label.
     *  @param name The name used to identify the entry (when accessing
     *   the entry).
     *  @param label The label to attach to the entry.
     *  @param defaultValue Default value to appear in the entry box.
     */
    public void addLine(String name, String label, String defaultValue) {
        JLabel lbl = new JLabel(label + ": ");
        lbl.setBackground(_background);
        JTextField entryBox = new JTextField(defaultValue, _width);
        entryBox.setBackground(_background);
        _addPair(lbl, entryBox);
        _entries.put(name, entryBox);
        // Add the listener last so that there is no notification
        // of the first value.
        entryBox.addActionListener(new LineListener(name));
    }

    /** Add a listener.  The changed() method of the listener will be
     *  called when any of the entries is changed.  Note that "line"
     *  entries only trigger this call when Return is pressed.
     */
    public void addQueryListener(QueryListener listener) {
        if(_listeners == null) _listeners = new LinkedList();
        _listeners.insertLast(listener);
    }

    /** Get the current value in the entry with the given name
     *  and return as a boolean.  If the entry is not a checkbox,
     *  then throw an exception.
     *  @return The state of the checkbox.
     *  @exception NoSuchElementException If there is no item with the
     *   specified name.  Note that this is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception IllegalArgumentException If the entry is not a
     *   radio button.  This is a runtime exception, so it
     *   need not be declared explicitly.
     */
    public boolean booleanValue(String name)
            throws NoSuchElementException, IllegalArgumentException {
        Object result = _entries.get(name);
        if(result == null) {
            throw new NoSuchElementException("No item named \"" +
            name + " \" in the query box.");
        }
        if (result instanceof JRadioButton) {
            return ((JRadioButton)result).isSelected();
        } else {
            throw new IllegalArgumentException("Item named \"" +
            name + "\" is not a radio button, and hence does not have "
            + "a boolean value.");
        }
    }

    /** Get the current value in the entry with the given name
     *  and return as a double value.  If the entry is not a line,
     *  then throw an exception.  If the value of the entry is not
     *  a double, then throw an exception.
     *  @return The value currently in the entry as a double.
     *  @exception NoSuchElementException If there is no item with the
     *   specified name.  Note that this is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception NumberFormatException If the value of the entry cannot
     *   be converted to a double.  This is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception IllegalArgumentException If the entry is not a
     *   line.  This is a runtime exception, so it
     *   need not be declared explicitly.
     */
    public double doubleValue(String name)
            throws IllegalArgumentException, NoSuchElementException,
            NumberFormatException {
        Object result = _entries.get(name);
        if(result == null) {
            throw new NoSuchElementException("No item named \"" +
            name + " \" in the query box.");
        }
        if (result instanceof JTextField) {
            return (new Double(((JTextField)result).getText())).doubleValue();
        } else {
            throw new IllegalArgumentException("Item named \"" +
            name + "\" is not a text line, and hence cannot be converted to "
            + "a double value.");
        }
    }

    /** Get the current value in the entry with the given name
     *  and return as an integer.  If the entry is not a line or
     *  a slider, then throw an exception.
     *  @return The value currently in the entry as an integer.
     *  @exception NoSuchElementException If there is no item with the
     *   specified name.  Note that this is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception NumberFormatException If the value of the entry cannot
     *   be converted to an integer.  This is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception IllegalArgumentException If the entry is not a
     *   line or a slider.  This is a runtime exception, so it
     *   need not be declared explicitly.
     */
    public int intValue(String name)
            throws IllegalArgumentException, NoSuchElementException,
            NumberFormatException {
        Object result = _entries.get(name);
        if(result == null) {
            throw new NoSuchElementException("No item named \"" +
            name + " \" in the query box.");
        }
        if (result instanceof JTextField) {
            return (new Integer(((JTextField)result).getText())).intValue();
        } else {
            throw new IllegalArgumentException("Item named \"" +
            name + "\" is not a text line or slider, and hence "
            + "cannot be converted to "
            + "an integer value.");
        }
    }

    /** Set the background color for all the widgets.
     *  @param color The background color.
     */
    public void setBackground(Color color) {
        super.setBackground(color);
        _background = color;
    }

    /** Set the displayed text of an item that has been added using
     *  addDisplay.
     *  @param name The name of the entry.
     *  @param value The string to display.
     *  @exception NoSuchElementException If there is no item with the
     *   specified name.  Note that this is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception IllegalArgumentException If the entry is not a
     *   line or a slider.  This is a runtime exception, so it
     *   need not be declared explicitly.
     */
    public void setDisplay(String name, String value) {
        Object result = _entries.get(name);
        if(result == null) {
            throw new NoSuchElementException("No item named \"" +
            name + " \" in the query box.");
        }
        if (result instanceof JTextArea) {
            JTextArea label = (JTextArea)result;
            label.setText(value);
        } else {
            throw new IllegalArgumentException("Item named \"" +
            name + "\" is not a display, and hence cannot be set using "
            + "setDisplay().");
        }
    }

    /** Specify the preferred width to be used for entry boxes created
     *  in using addLine().  If this is called multiple times, then
     *  only the largest value specified actually affects the layout.
     *
     *  @param characters The preferred width.
     */
    public void setTextWidth(int characters) {
        _width = characters;
    }

    /** Get the current value in the entry with the given name,
     *  and return as a String.  All entry types support this.
     *  @return The value currently in the entry as a String.
     *  @exception NoSuchElementException If there is no item with the
     *   specified name.  Note that this is a runtime exception, so it
     *   need not be declared explicitly.
     *  @exception IllegalArgumentException If the entry type does not
     *   have a string representation (this should not be thrown).
     */
    public String stringValue(String name) throws NoSuchElementException {
        Object result = _entries.get(name);
        if(result == null) {
            throw new NoSuchElementException("No item named \"" +
                    name + " \" in the query box.");
        }
        // FIXME: Surely there is a better way to do this...
        // We should define a set of inner classes, one for each entry type.
        // Currently, this has to be updated each time a new entry type
        // is added.
        if (result instanceof JTextField) {
            return ((JTextField)result).getText();
        } else if (result instanceof JTextArea) {
            return ((JTextArea)result).getText();
        } else if (result instanceof JRadioButton) {
            JRadioButton radioButton = (JRadioButton)result;
            if (radioButton.isSelected()) {
                return "true";
            } else {
                return "false";
            }
        } else {
            throw new IllegalArgumentException("Query class cannot generate"
            + " a string representation for entries of type "
            + result.getClass());
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    public static final int DEFAULT_ENTRY_WIDTH = 12;

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Add a label and a widget to the panel.
     *  @param label The label.
     *  @param widget The interactive entry to the right of the label.
     */
    protected void _addPair(JLabel label, Component widget) {
        // Surely there is a better layout manager in swing...
        // Note that Box and BoxLayout do not work because they do not
        // support gridded layout.
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        _grid.setConstraints(label, constraints);
        add(label);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        _grid.setConstraints(widget, constraints);
        add(widget);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    /** The background color as set by setBackground().
     *  This defaults to white.
     */
    protected Color _background = Color.white;

    /** Layout control. */
    protected GridBagLayout _grid;

    /** List of registered listeners. */
    protected LinkedList _listeners;

    ///////////////////////////////////////////////////////////////////
    ////                         friendly methods                  ////

    // Notify all registered listeners that something changed.
    void _notifyListeners(String name) {
        if(_listeners != null) {
            Enumeration listeners = _listeners.elements();
            while(listeners.hasMoreElements()) {
                QueryListener queueListener =
                    (QueryListener)(listeners.nextElement());
                queueListener.changed(name);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The hashtable of items in the query
    private Hashtable _entries = new Hashtable();

    // The width of the text boxes.
    private int _width = DEFAULT_ENTRY_WIDTH;

    ///////////////////////////////////////////////////////////////////
    ////                         inner classes                     ////

    /** Listener for "line" entries.
     */
    class LineListener implements ActionListener {
        public LineListener(String name) {
            _name = name;
        }
        /** Call all registered QueryListeners. */
        public void actionPerformed(ActionEvent e) {
            _notifyListeners(_name);
        }
        private String _name;
    }

    /** Listener for "CheckBox" entries.
     */
    class CheckBoxListener implements ItemListener {
        public CheckBoxListener(String name) {
            _name = name;
        }

        /** Call all registered QueryListeners. */
        public void itemStateChanged(ItemEvent e) {
            _notifyListeners(_name);
        }
        private String _name;
    }
}
