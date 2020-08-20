package mars.venus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;
import javax.swing.undo.UndoableEdit;

import mars.ErrorList;
import mars.Globals;
import mars.simulator.Simulator;

/*
 * Copyright (c) 2003-2010, Pete Sanderson and Kenneth Vollmar
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
 * Creates the message window at the bottom of the UI.
 *
 * @author Team JSpim
 **/

public class MessagesPane extends JTabbedPane {

	/**
	 *
	 */
	private static final long serialVersionUID = 957110768243826281L;
	JTextArea assemble, run;
	JPanel assembleTab, runTab;
	// These constants are designed to keep scrolled contents of the
	// two message areas from becoming overwhelmingly large (which
	// seems to slow things down as new text is appended).  Once it
	// reaches MAXIMUM_SCROLLED_CHARACTERS in length then cut off
	// the first NUMBER_OF_CHARACTERS_TO_CUT characters.  The latter
	// must obviously be smaller than the former.
	public static final int MAXIMUM_SCROLLED_CHARACTERS = Globals.maximumMessageCharacters;
	public static final int NUMBER_OF_CHARACTERS_TO_CUT = Globals.maximumMessageCharacters / 10; // 10%

	/**
	 * Constructor for the class, sets up two fresh tabbed text areas for program
	 * feedback.
	 **/

	public MessagesPane() {
		super();
		setMinimumSize(new Dimension(0, 0));
		assemble = new JTextArea();
		run = new JTextArea();
		assemble.setEditable(false);
		run.setEditable(false);
		// Set both text areas to mono font.  For assemble
		// pane, will make messages more readable.  For run
		// pane, will allow properly aligned "text graphics"
		// DPS 15 Dec 2008
		final Font monoFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		assemble.setFont(monoFont);
		run.setFont(monoFont);

		final JButton assembleTabClearButton = new JButton("Clear");
		assembleTabClearButton.setToolTipText("Clear the Mars Messages area");
		assembleTabClearButton.addActionListener(e -> assemble.setText(""));
		assembleTab = new JPanel(new BorderLayout());
		assembleTab.add(createBoxForButton(assembleTabClearButton), BorderLayout.WEST);
		assembleTab.add(new JScrollPane(assemble, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		assemble.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				String text;
				int lineStart = 0;
				int lineEnd = 0;
				try {
					final int line = assemble.getLineOfOffset(assemble.viewToModel(e.getPoint()));
					lineStart = assemble.getLineStartOffset(line);
					lineEnd = assemble.getLineEndOffset(line);
					text = assemble.getText(lineStart, lineEnd - lineStart);
				} catch (final BadLocationException ble) {
					text = "";
				}
				if (text.length() > 0) {
					// If error or warning, parse out the line and column number.
					if (text.startsWith(ErrorList.ERROR_MESSAGE_PREFIX) || text.startsWith(
							ErrorList.WARNING_MESSAGE_PREFIX)) {
						assemble.select(lineStart, lineEnd);
						assemble.setSelectionColor(Color.YELLOW);
						assemble.repaint();
						final int separatorPosition = text.indexOf(ErrorList.MESSAGE_SEPARATOR);
						if (separatorPosition >= 0) { text = text.substring(0, separatorPosition); }
						final String[] stringTokens = text.split("\\s"); // tokenize with whitespace delimiter
						final String lineToken = ErrorList.LINE_PREFIX.trim();
						final String columnToken = ErrorList.POSITION_PREFIX.trim();
						String lineString = "";
						String columnString = "";
						for (int i = 0; i < stringTokens.length; i++) {
							if (stringTokens[i].equals(lineToken) && i < stringTokens.length - 1) {
								lineString = stringTokens[i + 1];
							}
							if (stringTokens[i].equals(columnToken) && i < stringTokens.length - 1) {
								columnString = stringTokens[i + 1];
							}
						}
						int line = 0;
						int column = 0;
						try {
							line = Integer.parseInt(lineString);
						} catch (final NumberFormatException nfe) {
							line = 0;
						}
						try {
							column = Integer.parseInt(columnString);
						} catch (final NumberFormatException nfe) {
							column = 0;
						}
						// everything between FILENAME_PREFIX and LINE_PREFIX is filename.
						final int fileNameStart = text.indexOf(ErrorList.FILENAME_PREFIX) + ErrorList.FILENAME_PREFIX
								.length();
						final int fileNameEnd = text.indexOf(ErrorList.LINE_PREFIX);
						String fileName = "";
						if (fileNameStart < fileNameEnd && fileNameStart >= ErrorList.FILENAME_PREFIX.length()) {
							fileName = text.substring(fileNameStart, fileNameEnd).trim();
						}
						if (fileName != null && fileName.length() > 0) {
							selectEditorTextLine(fileName, line, column);
							selectErrorMessage(fileName, line, column);
						}
					}
				}
			}
		});

		final JButton runTabClearButton = new JButton("Clear");
		runTabClearButton.setToolTipText("Clear the Run I/O area");
		runTabClearButton.addActionListener(e -> run.setText(""));
		runTab = new JPanel(new BorderLayout());
		runTab.add(createBoxForButton(runTabClearButton), BorderLayout.WEST);
		runTab.add(new JScrollPane(run, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		this.addTab("Mars Messages", assembleTab);
		this.addTab("Run I/O", runTab);
		setToolTipTextAt(0, "Messages produced by Run menu. Click on assemble error message to select erroneous line");
		setToolTipTextAt(1, "Simulated MIPS console input and output");
	}

	// Center given button in a box, centered vertically and 6 pixels on left and right
	private Box createBoxForButton(final JButton button) {
		final Box buttonRow = Box.createHorizontalBox();
		buttonRow.add(Box.createHorizontalStrut(6));
		buttonRow.add(button);
		buttonRow.add(Box.createHorizontalStrut(6));
		final Box buttonBox = Box.createVerticalBox();
		buttonBox.add(Box.createVerticalGlue());
		buttonBox.add(buttonRow);
		buttonBox.add(Box.createVerticalGlue());
		return buttonBox;
	}

	/**
	 * Will select the Mars Messages tab error message that matches the given
	 * specifications, if it is found. Matching is done by constructing a string
	 * using the parameter values and searching the text area for the last
	 * occurrance of that string.
	 *
	 * @param fileName A String containing the file path name.
	 * @param line     Line number for error message
	 * @param column   Column number for error message
	 */
	public void selectErrorMessage(final String fileName, final int line, final int column) {
		final String errorReportSubstring = new java.io.File(fileName).getName() + ErrorList.LINE_PREFIX + line
				+ ErrorList.POSITION_PREFIX + column;
		final int textPosition = assemble.getText().lastIndexOf(errorReportSubstring);
		if (textPosition >= 0) {
			int textLine = 0;
			int lineStart = 0;
			int lineEnd = 0;
			try {
				textLine = assemble.getLineOfOffset(textPosition);
				lineStart = assemble.getLineStartOffset(textLine);
				lineEnd = assemble.getLineEndOffset(textLine);
				assemble.setSelectionColor(Color.YELLOW);
				assemble.select(lineStart, lineEnd);
				assemble.getCaret().setSelectionVisible(true);
				assemble.repaint();
			} catch (final BadLocationException ble) {
				// If there is a problem, simply skip the selection
			}
		}
	}

	/**
	 * Will select the specified line in an editor tab. If the file is open but not
	 * current, its tab will be made current. If the file is not open, it will be
	 * opened in a new tab and made current, however the line will not be selected
	 * (apparent apparent problem with JEditTextArea).
	 *
	 * @param fileName A String containing the file path name.
	 * @param line     Line number for error message
	 * @param column   Column number for error message
	 */
	public void selectEditorTextLine(final String fileName, final int line, final int column) {
		final EditTabbedPane editTabbedPane = (EditTabbedPane) Globals.getGui().getMainPane().getEditTabbedPane();
		EditPane editPane, currentPane = null;
		editPane = editTabbedPane.getEditPaneForFile(new java.io.File(fileName).getPath());
		if (editPane != null) {
			if (editPane != editTabbedPane.getCurrentEditTab()) { editTabbedPane.setCurrentEditTab(editPane); }
			currentPane = editPane;
		} else {	// file is not open.  Try to open it.
			if (editTabbedPane.openFile(new java.io.File(fileName))) {
				currentPane = editTabbedPane.getCurrentEditTab();
			}
		}
		// If editPane == null, it means the desired file was not open.  Line selection
		// does not properly with the JEditTextArea editor in this situation (it works
		// fine for the original generic editor).  So we just won't do it. DPS 9-Aug-2010
		if (editPane != null && currentPane != null) { currentPane.selectLine(line, column); }
	}

	/**
	 * Returns component used to display assembler messages
	 *
	 * @return assembler message text component
	 */
	public JTextArea getAssembleTextArea() { return assemble; }

	/**
	 * Returns component used to display runtime messages
	 *
	 * @return runtime message text component
	 */
	public JTextArea getRunTextArea() { return run; }

	/**
	 * Post a message to the assembler display
	 *
	 * @param message String to append to assembler display text
	 */
	public void postMarsMessage(final String message) {
		assemble.append(message);
		// can do some crude cutting here.  If the document gets "very large",
		// let's cut off the oldest text. This will limit scrolling but the limit
		// can be set reasonably high.
		if (assemble.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
			try {
				assemble.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
			} catch (final BadLocationException ble) {
				// only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
			}
		}
		assemble.setCaretPosition(assemble.getDocument().getLength());
		setSelectedComponent(assembleTab);
	}

	/**
	 * Post a message to the runtime display
	 *
	 * @param message String to append to runtime display text
	 */
	// The work of this method is done by "invokeLater" because
	// its JTextArea is maintained by the main event thread
	// but also used, via this method, by the execution thread for
	// "print" syscalls. "invokeLater" schedules the code to be
	// run under the event-processing thread no matter what.
	// DPS, 23 Aug 2005.
	public void postRunMessage(final String message) {
		final String mess = message;
		SwingUtilities.invokeLater(() -> {
			setSelectedComponent(runTab);
			run.append(mess);
			// can do some crude cutting here.  If the document gets "very large",
			// let's cut off the oldest text. This will limit scrolling but the limit
			// can be set reasonably high.
			if (run.getDocument().getLength() > MAXIMUM_SCROLLED_CHARACTERS) {
				try {
					run.getDocument().remove(0, NUMBER_OF_CHARACTERS_TO_CUT);
				} catch (final BadLocationException ble) {
					// only if NUMBER_OF_CHARACTERS_TO_CUT > MAXIMUM_SCROLLED_CHARACTERS
				}
			}
		});
	}

	/**
	 * Make the assembler message tab current (up front)
	 */
	public void selectMarsMessageTab() {
		setSelectedComponent(assembleTab);
	}

	/**
	 * Make the runtime message tab current (up front)
	 */
	public void selectRunMessageTab() {
		setSelectedComponent(runTab);
	}

	/**
	 * Method used by the SystemIO class to get interactive user input requested by
	 * a running MIPS program (e.g. syscall #5 to read an integer). SystemIO knows
	 * whether simulator is being run at command line by the user, or by the GUI. If
	 * run at command line, it gets input from System.in rather than here. This is
	 * an overloaded method. This version, with the String parameter, is used to get
	 * input from a popup dialog.
	 *
	 * @param prompt Prompt to display to the user.
	 * @return User input.
	 */
	public String getInputString(final String prompt) {
		String input;
		final JOptionPane pane = new JOptionPane(prompt, JOptionPane.QUESTION_MESSAGE, JOptionPane.DEFAULT_OPTION);
		pane.setWantsInput(true);
		final JDialog dialog = pane.createDialog(Globals.getGui(), "MIPS Keyboard Input");
		dialog.setVisible(true);
		input = (String) pane.getInputValue();
		postRunMessage(Globals.userInputAlert + input + "\n");
		return input;
	}

	/**
	 * Method used by the SystemIO class to get interactive user input requested by
	 * a running MIPS program (e.g. syscall #5 to read an integer). SystemIO knows
	 * whether simulator is being run at command line by the user, or by the GUI. If
	 * run at command line, it gets input from System.in rather than here. This is
	 * an overloaded method. This version, with the int parameter, is used to get
	 * input from the MARS Run I/O window.
	 *
	 * @param maxLen: maximum length of input. This method returns when maxLen
	 *                characters have been read. Use -1 for no length restrictions.
	 * @return User input.
	 */
	public String getInputString(final int maxLen) {
		final Asker asker = new Asker(maxLen); // Asker defined immediately below.
		return asker.response();
	}

	////////////////////////////////////////////////////////////////////////////
	// Thread class for obtaining user input in the Run I/O window (MessagesPane)
	// Written by Ricardo Fernández Pascual [rfernandez@ditec.um.es] December 2009.
	class Asker implements Runnable {

		ArrayBlockingQueue<String> resultQueue = new ArrayBlockingQueue<>(1);
		int initialPos;
		int maxLen;

		Asker(final int maxLen) {
			this.maxLen = maxLen;
			// initialPos will be set in run()
		}

		final DocumentListener listener = new DocumentListener() {

			@Override
			public void insertUpdate(final DocumentEvent e) {
				EventQueue.invokeLater(() -> {
					try {
						final String inserted = e.getDocument().getText(e.getOffset(), e.getLength());
						final int i = inserted.indexOf('\n');
						if (i >= 0) {
							final int offset = e.getOffset() + i;
							if (offset + 1 == e.getDocument().getLength()) {
								returnResponse();
							} else {
								// remove the '\n' and put it at the end
								e.getDocument().remove(offset, 1);
								e.getDocument().insertString(e.getDocument().getLength(), "\n", null);
								// insertUpdate will be called again, since we have inserted the '\n' at the end
							}
						} else if (maxLen >= 0 && e.getDocument().getLength() - initialPos >= maxLen) {
							returnResponse();
						}
					} catch (final BadLocationException ex) {
						returnResponse();
					}
				});
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				EventQueue.invokeLater(() -> {
					if ((e.getDocument().getLength() < initialPos || e.getOffset() < initialPos)
							&& e instanceof UndoableEdit) {
						((UndoableEdit) e).undo();
						run.setCaretPosition(e.getOffset() + e.getLength());
					}
				});
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {}
		};
		final NavigationFilter navigationFilter = new NavigationFilter() {

			@Override
			public void moveDot(final FilterBypass fb, int dot, final Bias bias) {
				if (dot < initialPos) { dot = Math.min(initialPos, run.getDocument().getLength()); }
				fb.moveDot(dot, bias);
			}

			@Override
			public void setDot(final FilterBypass fb, int dot, final Bias bias) {
				if (dot < initialPos) { dot = Math.min(initialPos, run.getDocument().getLength()); }
				fb.setDot(dot, bias);
			}
		};
		final Simulator.StopListener stopListener = s -> returnResponse();

		@Override
		public void run() { // must be invoked from the GUI thread
			setSelectedComponent(runTab);
			run.setEditable(true);
			run.requestFocusInWindow();
			run.setCaretPosition(run.getDocument().getLength());
			initialPos = run.getCaretPosition();
			run.setNavigationFilter(navigationFilter);
			run.getDocument().addDocumentListener(listener);
			Simulator.getInstance().addStopListener(stopListener);
		}

		void cleanup() { // not required to be called from the GUI thread
			EventQueue.invokeLater(() -> {
				run.getDocument().removeDocumentListener(listener);
				run.setEditable(false);
				run.setNavigationFilter(null);
				run.setCaretPosition(run.getDocument().getLength());
				Simulator.getInstance().removeStopListener(stopListener);
			});
		}

		void returnResponse() {
			try {
				final int p = Math.min(initialPos, run.getDocument().getLength());
				final int l = Math.min(run.getDocument().getLength() - p, maxLen >= 0 ? maxLen : Integer.MAX_VALUE);
				resultQueue.offer(run.getText(p, l));
			} catch (final BadLocationException ex) {
				// this cannot happen
				resultQueue.offer("");
			}
		}

		String response() {
			EventQueue.invokeLater(this);
			try {
				return resultQueue.take();
			} catch (final InterruptedException ex) {
				return null;
			} finally {
				cleanup();
			}
		}
	}  // Asker class
		////////////////////////////////////////////////////////////////////////////
}
