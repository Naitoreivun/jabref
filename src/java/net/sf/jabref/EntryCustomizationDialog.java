/*
Copyright (C) 2002-2003 Nizar N. Batada nbatada@stanford.edu
All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

class EntryCustomizationDialog extends JDialog implements ItemListener
{
    BibtexEntryType type;

    JScrollPane reqSP, optSP;
    JButton ok, cancel, helpButton, delete, genFields;
    JPanel panel=new JPanel(),
	fieldPanel = new JPanel(),
	typePanel = new JPanel();
    int width=10;
    JLabel messageLabel=new JLabel("", SwingConstants.CENTER);

    JTextField name = new JTextField("", width);
    JTextArea req_ta=new JTextArea("",1,width),//10 row, 20 columns
	opt_ta=new JTextArea("",1,width);//10 row, 20 columns
    // need to get FIeld name from somewhere

    JComboBox types_cb = new JComboBox();

    HelpAction help;

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JPanel buttonPanel = new JPanel();

    JabRefFrame parent;
    EntryCustomizationDialog ths = this;

    public EntryCustomizationDialog(JabRefFrame parent)
    {
	//Type=Article, Book etc
	// templateName will be used to put on the dialog frame
	// create 10 default entries
	// return an array
	super(parent,Globals.lang("Customize entry types"), false);
	this.parent = parent;
	help = new HelpAction(parent.helpDiag, GUIGlobals.customEntriesHelp,
			      "Help", GUIGlobals.helpSmallIconFile);
	setTypeSelection();
	setSize(440,400);
	initialize();
	makeButtons();

	reqSP = new JScrollPane(req_ta,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	optSP = new JScrollPane(opt_ta,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	//helpButton = new JButton(help);
	//helpButton.setText(null);
	JToolBar tlb = new JToolBar();
	tlb.setFloatable(false);
	tlb.add(help);
	panel.setBackground(GUIGlobals.lightGray);
	buttonPanel.setBackground(GUIGlobals.lightGray);
	panel.setLayout(gbl);
	typePanel.setLayout(gbl);
	fieldPanel.setLayout(gbl);
	//panel.setBorder(BorderFactory.createEtchedBorder());
	fieldPanel.setBorder(BorderFactory.createEtchedBorder());
	typePanel.setBorder(BorderFactory.createEtchedBorder());

	JLabel lab = new JLabel(Globals.lang("Type")+": "),
	    lab2 = new JLabel(Globals.lang("Name")+": ");
	con.insets = new Insets(5, 5, 5, 5);
	gbl.setConstraints(lab, con);
	gbl.setConstraints(lab2, con);
	gbl.setConstraints(types_cb, con);

	con.weightx = 1;
	con.fill = GridBagConstraints.HORIZONTAL;
	gbl.setConstraints(name, con);
	con.fill = GridBagConstraints.NONE;
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weightx = 0;
	//gbl.setConstraints(helpButton, con);
	gbl.setConstraints(tlb, con);
	con.gridwidth = 1;
	typePanel.add(lab);
	typePanel.add(types_cb);
	typePanel.add(lab2);
	typePanel.add(name);
	//typePanel.add(helpButton);
	typePanel.add(tlb);
	lab = new JLabel(Globals.lang("Required fields"));
	con.fill = GridBagConstraints.BOTH;
	con.weightx = 1;
	gbl.setConstraints(lab, con);
	con.weighty = 1;
	gbl.setConstraints(reqSP, con);
	fieldPanel.add(lab);
	con.gridwidth = GridBagConstraints.REMAINDER;
	lab = new JLabel(Globals.lang("Optional fields"));
	con.weighty = 0;
	gbl.setConstraints(lab, con);
	fieldPanel.add(lab);
	con.weighty = 1;
	gbl.setConstraints(optSP, con);

	fieldPanel.add(reqSP);
	fieldPanel.add(optSP);

	con.gridwidth = GridBagConstraints.REMAINDER;
	con.weighty = 0;
	gbl.setConstraints(typePanel, con);
	con.weighty = 1;
	gbl.setConstraints(fieldPanel, con);
	con.weighty = 0;
	gbl.setConstraints(messageLabel, con);
	panel.add(typePanel);
	panel.add(fieldPanel);
	panel.add(messageLabel);

        // Key bindings:
        ActionMap am = panel.getActionMap();
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(GUIGlobals.exitDialog, "close");
        am.put("close", new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            dispose();
          }
        });

	name.requestFocus();
    }

    public EntryCustomizationDialog(JabRefFrame parent,
				    BibtexEntryType type_) {
	this(parent);
	type = type_;

    }

    void initialize(){

	getContentPane().setLayout(new BorderLayout());
	getContentPane().add( buttonPanel, BorderLayout.SOUTH);
	getContentPane().add( panel, BorderLayout.CENTER);

	messageLabel.setForeground(Color.black);
	messageLabel.setText(Globals.lang("Delimit_1"));

	types_cb.addItemListener(this);
    }

    void save()
    {

	String
	    reqStr = req_ta.getText().replaceAll("\\s+","")
	    .replaceAll("\\n+","").trim(),
	    optStr = opt_ta.getText().replaceAll("\\s+","")
	    .replaceAll("\\n+","").trim();


	String typeName = name.getText().trim();

	if(! typeName.equals("")) {
	    CustomEntryType typ = new CustomEntryType
		(Util.nCase(typeName), reqStr, optStr);
	    BibtexEntryType.ALL_TYPES.put(typeName.toLowerCase(), typ);
	    updateTypesForEntries(typ.getName());
	    setTypeSelection();
	    messageLabel.setText(Globals.lang("Stored definition for type")+
				 " '"+Util.nCase(typ.getName())
				 +"'.");
	}
	else{
	    messageLabel.setText(Globals.lang("You must fill in a name for the entry type."));
	}

    }

    private void setTypeSelection() {
	types_cb.removeAllItems();
	types_cb.addItem("<new>");
	Iterator i = BibtexEntryType.ALL_TYPES.keySet().iterator();
	BibtexEntryType type;
	String toSet;
	while (i.hasNext()) {
	    type = BibtexEntryType.getType((String)i.next());
	    toSet = Util.nCase(type.getName());
	    if (type instanceof CustomEntryType)
		toSet = toSet + " *";
	    types_cb.addItem(toSet);

	}
    }

    void makeButtons(){
	ok = new JButton(Globals.lang("Store"));
	cancel=new JButton(Globals.lang("Close"));
	delete = new JButton(Globals.lang("Delete custom"));
        genFields = new JButton(Globals.lang("Set general fields"));
        buttonPanel.add( ok );
	buttonPanel.add(delete);
        buttonPanel.add(genFields);
	buttonPanel.add( cancel);
	ok.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    save();
		}
	    });
	cancel.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    dispose();
		}
	    });
        genFields.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            GenFieldsCustomizer gf = new GenFieldsCustomizer(parent, ths);
            Util.placeDialog(gf, parent);
            gf.show();
          }
        });
	delete.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    BibtexEntryType type = BibtexEntryType
			.getType(name.getText());
		    if (type == null)
			messageLabel.setText(Globals.lang("There is no entry type")+
					     " '"+Util.nCase(name.getText())+
					     "' "+Globals.lang("defined."));
		    else if (!(type instanceof CustomEntryType))
			messageLabel.setText("'"+type.getName()+"' "+
					     Globals.lang("is a standard type."));
		    else {
			String nm = name.getText();
			if (BibtexEntryType.getStandardType(nm) == null) {
			    int reply = JOptionPane.showConfirmDialog
				(parent, Globals.lang("All entries of this "
						      +"type will be declared "
						      +"typeless. Continue?"),
				 Globals.lang("Delete custom format")+
				 " '"+Util.nCase(nm)+"'", JOptionPane.YES_NO_OPTION,
				 JOptionPane.WARNING_MESSAGE);
			    if (reply != JOptionPane.YES_OPTION)
				return;
			}
			BibtexEntryType.removeType(nm);
			setTypeSelection();
			updateTypesForEntries(Util.nCase(nm));
			messageLabel.setText
			    (Globals.lang("Removed entry type."));
		    }
		}
	    });
    }

    /**
     * Cycle through all databases, and make sure everything is updated with
     * the new type customization. This includes making sure all entries have
     * a valid type, that no obsolete entry editors are around, and that
     * the right-click menus' change type menu is up-to-date.
     */
    private void updateTypesForEntries(String typeName) {
	if (parent.tabbedPane.getTabCount() == 0)
	    return;
	messageLabel.setText(Globals.lang("Updating entries..."));
	BibtexDatabase base;
	Iterator iter;
	for (int i=0; i<parent.tabbedPane.getTabCount(); i++) {
	    BasePanel bp = (BasePanel)parent.tabbedPane.getComponentAt(i);
	    boolean anyChanges = false;
	    bp.entryEditors.remove(typeName);
	    bp.rcm.populateTypeMenu(); // Update type menu for change type.
	    base = bp.database;
	    iter = base.getKeySet().iterator();
	    while (iter.hasNext()) {
		anyChanges = anyChanges |
		    !(base.getEntryById((String)iter.next())).updateType();
	    }
	    if (anyChanges) {
		bp.refreshTable();
		bp.markBaseChanged();
	    }
	}
    }

    public void itemStateChanged(ItemEvent e) {
	if (types_cb.getSelectedIndex() > 0) {
	    // User has selected one of the existing types.
	    String name = (String)types_cb.getSelectedItem();
	    updateToType((name.split(" "))[0]);
	} else {
	    name.setText("");
	    req_ta.setText("");
	    opt_ta.setText("");
	    name.requestFocus();
	}
    }

    public void updateToType(String o) {

	BibtexEntryType type = BibtexEntryType.getType(o);
	name.setText(type.getName());
	req_ta.setText(Util.stringArrayToDelimited
		       (type.getRequiredFields(), ";\n"));
	opt_ta.setText(Util.stringArrayToDelimited
		       (type.getOptionalFields(), ";\n"));

	req_ta.requestFocus();
    }
}
