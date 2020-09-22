package mars.venus.editors.jeditsyntax;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import mars.Globals;
import mars.venus.EditPane;
import mars.venus.editors.MARSTextEditingArea;
import mars.venus.editors.jeditsyntax.tokenmarker.MIPSTokenMarker;

/**
 * Adaptor subclass for JEditTextArea Provides those methods required by the
 * MARSTextEditingArea interface that are not defined by JEditTextArea. This
 * permits JEditTextArea to be used within MARS largely without modification.
 * DPS 4-20-2010
 *
 * @since 4.0
 * @author Pete Sanderson
 */

public class JEditBasedTextArea extends JEditTextArea implements MARSTextEditingArea, CaretListener {

	/**
	 *
	 */
	private static final long serialVersionUID = -411371578743254097L;
	private final EditPane editPane;
	private final UndoManager undoManager;
	private final UndoableEditListener undoableEditListener;
	private boolean isCompoundEdit = false;
	private CompoundEdit compoundEdit;
	private final JEditBasedTextArea sourceCode;

	public JEditBasedTextArea(final EditPane editPain, final JComponent lineNumbers) {
		super(lineNumbers);
		editPane = editPain;
		undoManager = new UndoManager();
		compoundEdit = new CompoundEdit();
		sourceCode = this;

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
		setFont(Globals.getSettings().getEditorFont());
		setTokenMarker(new MIPSTokenMarker());

		addCaretListener(this);
	}

	@Override
	public void setFont(final Font f) { getPainter().setFont(f); }

	@Override
	public Font getFont() { return getPainter().getFont(); }

	// 		public void repaint() {		 getPainter().repaint();		 }
	// 		 public Dimension getSize() { return painter.getSize(); }
	// 		 public void setSize(Dimension d) { painter.setSize(d);}

	/**
	 * Use for highlighting the line currently being edited.
	 *
	 * @param highlight true to enable line highlighting, false to disable.
	 */
	@Override
	public void setLineHighlightEnabled(final boolean highlight) { getPainter().setLineHighlightEnabled(highlight); }

	/**
	 * Set the caret blinking rate in milliseconds. If rate is 0 will disable
	 * blinking. If negative, do nothing.
	 *
	 * @param rate blinking rate in milliseconds
	 */
	@Override
	public void setCaretBlinkRate(final int rate) {
		if (rate == 0) {
			caretBlinks = false;
		}
		if (rate > 0) {
			caretBlinks = true;
			caretBlinkRate = rate;
			caretTimer.setDelay(rate);
			caretTimer.setInitialDelay(rate);
			caretTimer.restart();
		}
	}

	/**
	 * Set the number of characters a tab will expand to.
	 *
	 * @param chars number of characters
	 */
	@Override
	public void setTabSize(final int chars) { painter.setTabSize(chars); }

	/**
	 * Update the syntax style table, which is obtained from SyntaxUtilities.
	 */
	@Override
	public void updateSyntaxStyles() { painter.setStyles(SyntaxUtilities.getCurrentSyntaxStyles()); }

	@Override
	public Component getOuterComponent() { return this; }

	/**
	 * Get rid of any accumulated undoable edits. It is useful to call this method
	 * after opening a file into the text area. The act of setting its text content
	 * upon reading the file will generate an undoable edit. Normally you don't want
	 * a freshly-opened file to appear with its Undo action enabled. But it will
	 * unless you call this after setting the text.
	 */
	@Override
	public void discardAllUndoableEdits() { undoManager.discardAllEdits(); }

	/**
	 * Display caret position on the edit pane.
	 *
	 * @param e A CaretEvent
	 */

	@Override
	public void caretUpdate(final CaretEvent e) { editPane.displayCaretPosition(((MutableCaretEvent) e).getDot()); }

	/**
	 * Same as setSelectedText but named for compatibility with JTextComponent
	 * method replaceSelection. DPS, 14 Apr 2010
	 *
	 * @param replacementText The replacement text for the selection
	 */
	@Override
	public void replaceSelection(final String replacementText) { setSelectedText(replacementText); }

	//
	//
	@Override
	public void setSelectionVisible(final boolean vis) {

	}

	//
	//
	@Override
	public void setSourceCode(final String s, final boolean editable) {
		setText(s);
		setBackground(editable ? Color.WHITE : Color.GRAY);
		setEditable(editable);
		setEnabled(editable);
		//this.getCaret().setVisible(editable);
		setCaretPosition(0);
		if (editable) {
			this.requestFocusInWindow();
		}
	}

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
		// "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
		// to pleasingly mark the text and location of the undo.
		unredoing = true;
		try {
			undoManager.undo();
		} catch (final CannotUndoException ex) {
			System.out.println(
				"Unable to undo: "
					+ ex);
			ex.printStackTrace();
		}
		unredoing = false;
		setCaretVisible(true);
	}

	/**
	 * Redo previous edit
	 */
	@Override
	public void redo() {
		// "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
		// to pleasingly mark the text and location of the redo.
		unredoing = true;
		try {
			undoManager.redo();
		} catch (final CannotRedoException ex) {
			System.out.println(
				"Unable to redo: "
					+ ex);
			ex.printStackTrace();
		}
		unredoing = false;
		setCaretVisible(true);
	}

	//////////////////////////////////////////////////////////////////////////
	//  Methods to support Find/Replace feature
	//
	// Basis for this Find/Replace solution is:
	// http://java.ittoolbox.com/groups/technical-functional/java-l/search-and-replace-using-jtextpane-630964
	// as written by Chris Dickenson in 2005
	//

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
			// Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart.
			sourceCode.setSelectionStart(nextPosn);
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
		// Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart.
		sourceCode.setSelectionStart(nextPosn);
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
				// Need to repeat start due to quirk in JEditTextArea implementation of setSelectionStart.
				sourceCode.setSelectionStart(nextPosn);
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
	public void updateColors() { painter.updateColors(); }

	//
	//////////////////////////////////////////////////////////////////

}
