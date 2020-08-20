package mars.venus;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import mars.Globals;

/*
 * Copyright (c) 2003-2006, Pete Sanderson and Kenneth Vollmar
 *
 * Developed by Pete Sanderson (psanderson@otterbein.edu) and Kenneth Vollmar
 * (kenvollmar@missouristate.edu)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Creates the tabbed areas in the UI and also created the internal windows that
 * exist in them.
 *
 * @author Sanderson and Bumgarner
 **/

public class MainPane extends JTabbedPane {

	/**
	 *
	 */
	private static final long serialVersionUID = 6490697126377688155L;
	EditPane editTab;
	ExecutePane executeTab;
	EditTabbedPane editTabbedPane;

	private final VenusUI mainUI;

	/**
	 * Constructor for the MainPane class.
	 **/

	public MainPane(final VenusUI appFrame, final Editor editor, final RegistersWindow regs,
			final Coprocessor1Window cop1Regs, final Coprocessor0Window cop0Regs) {
		super();
		mainUI = appFrame;
		setTabPlacement(SwingConstants.TOP); //LEFT);
		if (getUI() instanceof BasicTabbedPaneUI) { getUI(); }
		editTabbedPane = new EditTabbedPane(appFrame, editor, this);
		executeTab = new ExecutePane(appFrame, regs, cop1Regs, cop0Regs);
		final String editTabTitle = "Edit"; //"<html><center>&nbsp;<br>E<br>d<br>i<br>t<br>&nbsp;</center></html>";
		final String executeTabTitle = "Execute"; //"<html><center>&nbsp;<br>E<br>x<br>e<br>c<br>u<br>t<br>e<br>&nbsp;</center></html>";
		final Icon editTabIcon = null;//new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.imagesPath+"Edit_tab.jpg")));
		final Icon executeTabIcon = null;//new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.imagesPath+"Execute_tab.jpg")));

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		this.addTab(editTabTitle, editTabIcon, editTabbedPane);

		// this.addTab("<html><center>&nbsp;<br>P<br>r<br>o<br>j<br>&nbsp;<br>1<br&nbsp;</center></html>", null, new JTabbedPane());
		// this.addTab("<html><center>&nbsp;<br>P<br>r<br>o<br>j<br>&nbsp;<br>2<br&nbsp;</center></html>", null, new JTabbedPane());
		// this.addTab("<html><center>&nbsp;<br>P<br>r<br>o<br>j<br>&nbsp;<br>3<br&nbsp;</center></html>", null, new JTabbedPane());
		// this.addTab("<html><center>&nbsp;<br>P<br>r<br>o<br>j<br>&nbsp;<br>4<br&nbsp;</center></html>", null, new JTabbedPane());

		this.addTab(executeTabTitle, executeTabIcon, executeTab);

		setToolTipTextAt(0, "Text editor for composing MIPS programs.");
		setToolTipTextAt(1, "View and control assembly language program execution.  Enabled upon successful assemble.");

		/* Listener has one specific purpose: when Execute tab is selected for the
		 * first time, set the bounds of its internal frames by invoking the
		 * setWindowsBounds() method.  Once this occurs, listener removes itself!
		 * We do NOT want to reset bounds each time Execute tab is selected.
		 * See ExecutePane.setWindowsBounds documentation for more details.
		 */
		addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent ce) {
				final JTabbedPane tabbedPane = (JTabbedPane) ce.getSource();
				final int index = tabbedPane.getSelectedIndex();
				final Component c = tabbedPane.getComponentAt(index);
				final ExecutePane executePane = Globals.getGui().getMainPane().getExecutePane();
				if (c == executePane) {
					executePane.setWindowBounds();
					Globals.getGui().getMainPane().removeChangeListener(this);
				}
			}
		});
	}

	/**
	 * Returns current edit pane. Implementation changed for MARS 4.0 support for
	 * multiple panes, but specification is same.
	 *
	 * @return the editor pane
	 */
	public EditPane getEditPane() { return editTabbedPane.getCurrentEditTab(); }

	/**
	 * Returns component containing editor display
	 *
	 * @return the editor tabbed pane
	 */
	public JComponent getEditTabbedPane() { return editTabbedPane; }

	/**
	 * returns component containing execution-time display
	 *
	 * @return the execute pane
	 */
	public ExecutePane getExecutePane() { return executeTab; }

	/**
	 * returns component containing execution-time display. Same as
	 * getExecutePane().
	 *
	 * @return the execute pane
	 */
	public ExecutePane getExecuteTab() { return executeTab; }

}
