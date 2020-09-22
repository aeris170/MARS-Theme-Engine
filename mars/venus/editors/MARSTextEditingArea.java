package mars.venus.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.text.Document;
import javax.swing.undo.UndoManager;

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
 * Specifies capabilities that any test editor used in MARS must have.
 */

public interface MARSTextEditingArea {

	// Used by Find/Replace
	int TEXT_NOT_FOUND = 0;
	int TEXT_FOUND = 1;
	int TEXT_REPLACED_FOUND_NEXT = 2;
	int TEXT_REPLACED_NOT_FOUND_NEXT = 3;

	void copy();

	void cut();

	int doFindText(String find, boolean caseSensitive);

	int doReplace(String find, String replace, boolean caseSensitive);

	int doReplaceAll(String find, String replace, boolean caseSensitive);

	int getCaretPosition();

	Document getDocument();

	String getSelectedText();

	int getSelectionEnd();

	int getSelectionStart();

	void select(int selectionStart, int selectionEnd);

	void selectAll();

	String getText();

	UndoManager getUndoManager();

	void paste();

	void replaceSelection(String str);

	void setCaretPosition(int position);

	void setEditable(boolean editable);

	void setSelectionEnd(int pos);

	void setSelectionStart(int pos);

	void setText(String text);

	void setFont(Font f);

	Font getFont();

	boolean requestFocusInWindow();

	FontMetrics getFontMetrics(Font f);

	void setBackground(Color c);

	void setEnabled(boolean enabled);

	void grabFocus();

	void redo();

	void revalidate();

	void setSourceCode(String code, boolean editable);

	void setCaretVisible(boolean vis);

	void setSelectionVisible(boolean vis);

	void undo();

	void discardAllUndoableEdits();

	void setLineHighlightEnabled(boolean highlight);

	void setCaretBlinkRate(int rate);

	void setTabSize(int chars);

	void updateSyntaxStyles();

	Component getOuterComponent();

	void updateColors();
}
