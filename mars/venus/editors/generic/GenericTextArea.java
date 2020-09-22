package mars.venus.editors.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import mars.Globals;
import mars.venus.EditPane;
import mars.venus.editors.MARSTextEditingArea;

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

public class GenericTextArea extends JTextArea implements MARSTextEditingArea {

	/**
	 *
	 */
	private static final long serialVersionUID = -6955822583106912608L;
	private final EditPane editPane;
	private final UndoManager undoManager;
	private final UndoableEditListener undoableEditListener;
	private final JTextArea sourceCode;
	private final JScrollPane editAreaScrollPane;

	private boolean isCompoundEdit = false;
	private CompoundEdit compoundEdit;

	public GenericTextArea(final EditPane editPain, final JComponent lineNumbers) {
		editPane = editPain;
		sourceCode = this;
		setFont(Globals.getSettings().getEditorFont());
		setTabSize(Globals.getSettings().getEditorTabSize());
		setMargin(new Insets(0, 3, 3, 3));
		setCaretBlinkRate(Globals.getSettings().getCaretBlinkRate());

		final JPanel source = new JPanel(new BorderLayout());
		source.add(lineNumbers, BorderLayout.WEST);
		source.add(this, BorderLayout.CENTER);
		editAreaScrollPane = new JScrollPane(
				source,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		editAreaScrollPane.getVerticalScrollBar()
			.setUnitIncrement(sourceCode.getFontMetrics(sourceCode.getFont()).getHeight());

		undoManager = new UndoManager();

		getCaret().addChangeListener(e -> editPane.displayCaretPosition(getCaretPosition()));

		// Needed to support unlimited undo/redo capability
		undoableEditListener = e -> {
			//Remember the edit and update the menus.
			if (isCompoundEdit) {
				compoundEdit.addEdit(e.getEdit());
			} else {
				undoManager.addEdit(e.getEdit());
				editPane.updateUndoState();
				editPane.updateRedoState();
			}
		};
		getDocument().addUndoableEditListener(undoableEditListener);
	}

	/**
	 * Does nothing, but required by the interface. This editor does not support
	 * highlighting of the line currently being edited.
	 */
	@Override
	public void setLineHighlightEnabled(final boolean highlight) {}

	/**
	 * Does nothing, but required by the interface. This editor does not support
	 * syntax styling (colors, bold/italic).
	 */
	@Override
	public void updateSyntaxStyles() {}

	/**
	 * Set the caret blinking rate in milliseconds. If rate is 0 it will not blink.
	 * If negative, do nothing.
	 *
	 * @param rate blinking rate in milliseconds
	 */
	@Override
	public void setCaretBlinkRate(final int rate) {
		if (rate >= 0) {
			getCaret().setBlinkRate(rate);
		}
	}

	@Override
	public Component getOuterComponent() { return editAreaScrollPane; }

	/**
	 * For initalizing the source code when opening an ASM file
	 *
	 * @param s String containing text
	 * @param editable set true if code is editable else false
	 **/

	@Override
	public void setSourceCode(final String s, final boolean editable) {
		setText(s);
		setBackground(editable ? Color.WHITE : Color.GRAY);
		setEditable(editable);
		setEnabled(editable);
		getCaret().setVisible(editable);
		setCaretPosition(0);
		if (editable) {
			this.requestFocusInWindow();
		}
	}

	/**
	 * Tell UndoManager to discard all its collected undoable edits.
	 */
	@Override
	public void discardAllUndoableEdits() { undoManager.discardAllEdits(); }

	/**
	 * Override inherited setText to temporarily remove UndoableEditListener because
	 * this operation is not undoable.
	 *
	 * @param s String with new contents for the editing area. Replaces current
	 * content.
	 */
	@Override
	public void setText(final String s) {
		getDocument().removeUndoableEditListener(undoableEditListener);
		super.setText(s);
		getDocument().addUndoableEditListener(undoableEditListener);
	}

	/**
	 * Control caret visibility
	 *
	 * @param vis true to display caret, false to hide it
	 */
	@Override
	public void setCaretVisible(final boolean vis) { getCaret().setVisible(vis); }

	/**
	 * Control selection visibility
	 *
	 * @param vis true to display selection, false to hide it
	 */
	@Override
	public void setSelectionVisible(final boolean vis) { getCaret().setSelectionVisible(vis); }

	/**
	 * Returns the undo manager for this editing area
	 *
	 * @return the undo manager
	 */
	@Override
	public UndoManager getUndoManager() { return undoManager; }

	/**
	 * Undo previous edit
	 */
	@Override
	public void undo() {
		try {
			undoManager.undo();
		} catch (final CannotUndoException ex) {
			System.out.println(
				"Unable to undo: "
					+ ex);
			ex.printStackTrace();
		}
		setCaretVisible(true);
	}

	/**
	 * Redo previous edit
	 */
	@Override
	public void redo() {
		try {
			undoManager.redo();
		} catch (final CannotRedoException ex) {
			System.out.println(
				"Unable to redo: "
					+ ex);
			ex.printStackTrace();
		}
		setCaretVisible(true);
	}

	//////////////////////////////////////////////////////////////////////////
	//  Methods to support Find/Replace feature
	//
	// Basis for this Find/Replace solution is:
	// http://java.ittoolbox.com/groups/technical-functional/java-l/search-and-replace-using-jtextpane-630964
	// as written by Chris Dickenson in 2005
	//
	// sourceCode is implemented as JTextArea rather than JTextPane but the necessary methods are inherited
	// by both from JTextComponent.

	/**
	 * Finds next occurrence of text in a forward search of a string. Search begins
	 * at the current cursor location, and wraps around when the end of the string
	 * is reached.
	 *
	 * @param find the text to locate in the string
	 * @param caseSensitive true if search is to be case-sensitive, false otherwise
	 * @return TEXT_FOUND or TEXT_NOT_FOUND, depending on the result.
	 */
	@Override
	public int doFindText(final String find, final boolean caseSensitive) {
		final int findPosn = sourceCode.getCaretPosition();
		int nextPosn = 0;
		nextPosn = nextIndex(sourceCode.getText(), find, findPosn, caseSensitive);
		if (nextPosn >= 0) {
			sourceCode.requestFocus(); // guarantees visibility of the blue highlight 
			sourceCode.setSelectionStart(nextPosn); // position cursor at word start
			sourceCode.setSelectionEnd(nextPosn + find.length());
			sourceCode.setSelectionStart(nextPosn); // position cursor at word start
			return TEXT_FOUND;
		} else {
			return TEXT_NOT_FOUND;
		}
	}

	/**
	 * Returns next posn of word in text - forward search. If end of string is
	 * reached during the search, will wrap around to the beginning one time.
	 *
	 * @return next indexed position of found text or -1 if not found
	 * @param input the string to search
	 * @param find the string to find
	 * @param start the character position to start the search
	 * @param caseSensitive true for case sensitive. false to ignore case
	 */
	public int nextIndex(final String input, final String find, final int start, final boolean caseSensitive) {
		int textPosn = -1;
		if (input != null && find != null && start < input.length()) {
			if (caseSensitive) { // indexOf() returns -1 if not found
				textPosn = input.indexOf(find, start);
				// If not found from non-starting cursor position, wrap around
				if (start > 0 && textPosn < 0) {
					textPosn = input.indexOf(find);
				}
			} else {
				final String lowerCaseText = input.toLowerCase();
				textPosn = lowerCaseText.indexOf(find.toLowerCase(), start);
				// If not found from non-starting cursor position, wrap around
				if (start > 0 && textPosn < 0) {
					textPosn = lowerCaseText.indexOf(find.toLowerCase());
				}
			}
		}
		return textPosn;
	}

	/**
	 * Finds and replaces next occurrence of text in a string in a forward search.
	 * If cursor is initially at end of matching selection, will immediately replace
	 * then find and select the next occurrence if any. Otherwise it performs a find
	 * operation. The replace can be undone with one undo operation.
	 *
	 * @param find the text to locate in the string
	 * @param replace the text to replace the find text with - if the find text
	 * exists
	 * @param caseSensitive true for case sensitive. false to ignore case
	 * @return Returns TEXT_FOUND if not initially at end of selected match and
	 * matching occurrence is found. Returns TEXT_NOT_FOUND if the text is not
	 * matched. Returns TEXT_REPLACED_NOT_FOUND_NEXT if replacement is successful
	 * but there are no additional matches. Returns TEXT_REPLACED_FOUND_NEXT if
	 * reaplacement is successful and there is at least one additional match.
	 */
	@Override
	public int doReplace(final String find, final String replace, final boolean caseSensitive) {
		int nextPosn = 0;
		// Will perform a "find" and return, unless positioned at the end of
		// a selected "find" result.
		if (find == null || !find.equals(sourceCode.getSelectedText()) || sourceCode.getSelectionEnd() != sourceCode
			.getCaretPosition()) {
			return doFindText(find, caseSensitive);
		}
		// We are positioned at end of selected "find".  Rreplace and find next.
		nextPosn = sourceCode.getSelectionStart();
		sourceCode.grabFocus();
		sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
		sourceCode.setSelectionEnd(nextPosn + find.length()); //select found text
		isCompoundEdit = true;
		compoundEdit = new CompoundEdit();
		sourceCode.replaceSelection(replace);
		compoundEdit.end();
		undoManager.addEdit(compoundEdit);
		editPane.updateUndoState();
		editPane.updateRedoState();
		isCompoundEdit = false;
		sourceCode.setCaretPosition(nextPosn + replace.length());
		if (doFindText(find, caseSensitive) == TEXT_NOT_FOUND) {
			return TEXT_REPLACED_NOT_FOUND_NEXT;
		} else {
			return TEXT_REPLACED_FOUND_NEXT;
		}
	}

	/**
	 * Finds and replaces <B>ALL</B> occurrences of text in a string in a forward
	 * search. All replacements are bundled into one CompoundEdit, so one Undo
	 * operation will undo all of them.
	 *
	 * @param find the text to locate in the string
	 * @param replace the text to replace the find text with - if the find text
	 * exists
	 * @param caseSensitive true for case sensitive. false to ignore case
	 * @return the number of occurrences that were matched and replaced.
	 */
	@Override
	public int doReplaceAll(final String find, final String replace, final boolean caseSensitive) {
		int nextPosn = 0;
		int findPosn = 0; // *** begin at start of text
		int replaceCount = 0;
		compoundEdit = null; // new one will be created upon first replacement
		isCompoundEdit = true; // undo manager's action listener needs this
		while (nextPosn >= 0) {
			nextPosn = nextIndex(sourceCode.getText(), find, findPosn, caseSensitive);
			if (nextPosn >= 0) {
				// nextIndex() will wrap around, which causes infinite loop if
				// find string is a substring of replacement string.  This
				// statement will prevent that.
				if (nextPosn < findPosn) {
					break;
				}
				sourceCode.grabFocus();
				sourceCode.setSelectionStart(nextPosn); // posn cursor at word start
				sourceCode.setSelectionEnd(nextPosn + find.length()); //select found text
				if (compoundEdit == null) {
					compoundEdit = new CompoundEdit();
				}
				sourceCode.replaceSelection(replace);
				findPosn = nextPosn + replace.length(); // set for next search
				replaceCount++;
			}
		}
		isCompoundEdit = false;
		// Will be true if any replacements were performed
		if (compoundEdit != null) {
			compoundEdit.end();
			undoManager.addEdit(compoundEdit);
			editPane.updateUndoState();
			editPane.updateRedoState();
		}
		return replaceCount;
	}
	//
	/////////////////////////////  End Find/Replace methods //////////////////////////

	@Override
	public void updateColors() {}

}
