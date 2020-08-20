/*
 * JEditTextArea.java - jEdit's text component Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice remains
 * intact in all source distributions of this package.
 */

package mars.venus.editors.jeditsyntax;

import java.awt.AWTEvent;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import mars.Globals;
import mars.Settings;
import mars.venus.editors.jeditsyntax.tokenmarker.Token;
import mars.venus.editors.jeditsyntax.tokenmarker.TokenMarker;

/**
 * jEdit's text area component. It is more suited for editing program source
 * code than JEditorPane, because it drops the unnecessary features (images,
 * variable-width lines, and so on) and adds a whole bunch of useful goodies
 * such as:
 * <ul>
 * <li>More flexible key binding scheme
 * <li>Supports macro recorders
 * <li>Rectangular selection
 * <li>Bracket highlighting
 * <li>Syntax highlighting
 * <li>Command repetition
 * <li>Block caret can be enabled
 * </ul>
 * It is also faster and doesn't have as many problems. It can be used in other
 * applications; the only other part of jEdit it depends on is the syntax
 * package.
 * <p>
 * To use it in your app, treat it like any other component, for example:
 *
 * <pre>
 * JEditTextArea ta = new JEditTextArea();
 * ta.setTokenMarker(new JavaTokenMarker());
 * ta.setText("public class Test {\n" + "    public static void main(String[] args) {\n"
 * 		+ "        System.out.println(\"Hello World\");\n" + "    }\n" + "}");
 * </pre>
 *
 * @author Slava Pestov
 * @version $Id: JEditTextArea.java,v 1.36 1999/12/13 03:40:30 sp Exp $
 */
public class JEditTextArea extends JComponent {

	/**
	 *
	 */
	private static final long serialVersionUID = -9100129559027752939L;
	/**
	 * Adding components with this name to the text area will place them left of the
	 * horizontal scroll bar. In jEdit, the status bar is added this way.
	 */
	public static String LEFT_OF_SCROLLBAR = "los";
	public static Color POPUP_HELP_TEXT_COLOR = Color.BLACK;  // DPS 11-July-2014

	// Number of text lines moved for each click of the vertical scrollbar buttons.
	private static final int VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES = 1;
	// Number of text lines moved for each "notch" of the mouse wheel scroller.
	private static final int LINES_PER_MOUSE_WHEEL_NOTCH = 3;

	/**
	 * Creates a new JEditTextArea with the default settings.
	 */
	public JEditTextArea(final JComponent lineNumbers) {
		this(TextAreaDefaults.getDefaults(), lineNumbers);
	}

	/**
	 * Creates a new JEditTextArea with the specified settings.
	 *
	 * @param defaults The default settings
	 */

	private JScrollBar lineNumbersVertical;//************************************

	public JEditTextArea(final TextAreaDefaults defaults, final JComponent lineNumbers) {
		// Enable the necessary events
		enableEvents(AWTEvent.KEY_EVENT_MASK);

		// Initialize some misc. stuff
		painter = new TextAreaPainter(this, defaults);
		documentHandler = new DocumentHandler();
		listenerList = new EventListenerList();
		caretEvent = new MutableCaretEvent();
		lineSegment = new Segment();
		bracketLine = bracketPosition = -1;
		blink = true;
		unredoing = false;

		final JScrollPane lineNumberScroller = new JScrollPane(lineNumbers,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		lineNumberScroller.setBorder(new javax.swing.border.EmptyBorder(1, 1, 1, 1));
		lineNumbersVertical = lineNumberScroller.getVerticalScrollBar();

		// Initialize the GUI
		final JPanel lineNumbersPlusPainter = new JPanel(new BorderLayout());
		lineNumbersPlusPainter.add(painter, BorderLayout.CENTER);
		lineNumbersPlusPainter.add(lineNumberScroller, BorderLayout.WEST);
		setLayout(new ScrollLayout());
		add(CENTER, lineNumbersPlusPainter); //was: painter
		add(RIGHT, vertical = new JScrollBar(Adjustable.VERTICAL));
		add(BOTTOM, horizontal = new JScrollBar(Adjustable.HORIZONTAL));

		// Add some event listeners
		vertical.addAdjustmentListener(new AdjustHandler());
		horizontal.addAdjustmentListener(new AdjustHandler());
		painter.addComponentListener(new ComponentHandler());
		painter.addMouseListener(new MouseHandler());
		painter.addMouseMotionListener(new DragHandler());
		painter.addMouseWheelListener(new MouseWheelHandler()); // DPS 5-5-10
		addFocusListener(new FocusHandler());

		// Load the defaults
		setInputHandler(defaults.inputHandler);
		setDocument(defaults.document);
		editable = defaults.editable;
		caretVisible = defaults.caretVisible;
		caretBlinks = defaults.caretBlinks;
		caretBlinkRate = defaults.caretBlinkRate;
		electricScroll = defaults.electricScroll;

		popup = defaults.popup;

		caretTimer.setDelay(caretBlinkRate);

		// Intercept keystrokes before focus manager gets them.  If in editing window,
		// pass TAB keystrokes on to the key processor instead of letting focus
		// manager use them for focus traversal.
		// One can also accomplish this using: setFocusTraversalKeysEnabled(false);
		// but that seems heavy-handed.
		// DPS 12May2010
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
			if (JEditTextArea.this.isFocusOwner() && e.getKeyCode() == KeyEvent.VK_TAB && e.getModifiers() == 0) {
				processKeyEvent(e);
				return true;
			} else {
				return false;
			}
		});

		// We don't seem to get the initial focus event?
		focusedComponent = this;
	}

	/**
	 * Returns if this component can be traversed by pressing the Tab key. This
	 * returns false.
	 */
	//        public final boolean isManagingFocus()
	//       {
	//          return true;
	//       }

	/**
	 * Returns the object responsible for painting this text area.
	 */
	public final TextAreaPainter getPainter() { return painter; }

	/**
	 * Returns the input handler.
	 */
	public final InputHandler getInputHandler() { return inputHandler; }

	/**
	 * Sets the input handler.
	 *
	 * @param inputHandler The new input handler
	 */
	public void setInputHandler(final InputHandler inputHandler) { this.inputHandler = inputHandler; }

	/**
	 * Returns true if the caret is blinking, false otherwise.
	 */
	public final boolean isCaretBlinkEnabled() { return caretBlinks; }

	/**
	 * Toggles caret blinking.
	 *
	 * @param caretBlinks True if the caret should blink, false otherwise
	 */
	public void setCaretBlinkEnabled(final boolean caretBlinks) {
		this.caretBlinks = caretBlinks;
		if (!caretBlinks) { blink = false; }

		painter.invalidateSelectedLines();
	}

	/**
	 * Returns true if the caret is visible, false otherwise.
	 */
	public final boolean isCaretVisible() { return (!caretBlinks || blink) && caretVisible; }

	/**
	 * Sets if the caret should be visible.
	 *
	 * @param caretVisible True if the caret should be visible, false otherwise
	 */
	public void setCaretVisible(final boolean caretVisible) {
		this.caretVisible = caretVisible;
		blink = true;

		painter.invalidateSelectedLines();
	}

	/**
	 * Blinks the caret.
	 */
	public final void blinkCaret() {
		if (caretBlinks) {
			blink = !blink;
			painter.invalidateSelectedLines();
		} else {
			blink = true;
		}
	}

	/**
	 * Returns the number of lines from the top and button of the text area that are
	 * always visible.
	 */
	public final int getElectricScroll() { return electricScroll; }

	/**
	 * Sets the number of lines from the top and bottom of the text area that are
	 * always visible
	 *
	 * @param electricScroll The number of lines always visible from the top or
	 *                       bottom
	 */
	public final void setElectricScroll(final int electricScroll) { this.electricScroll = electricScroll; }

	/**
	 * Updates the state of the scroll bars. This should be called if the number of
	 * lines in the document changes, or when the size of the text are changes.
	 */
	public void updateScrollBars() {
		if (vertical != null && visibleLines != 0) {
			vertical.setValues(firstLine, visibleLines, 0, getLineCount());
			vertical.setUnitIncrement(VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES);
			vertical.setBlockIncrement(visibleLines);

			// Editing area scrollbar has custom model that increments by number of text lines instead of
			// number of pixels. The line number display uses a standard (but invisible) scrollbar based
			// on pixels, so I need to adjust accordingly to keep it in synch with the editing area scrollbar.
			// DPS 4-May-2010
			final int height = painter.getFontMetrics(painter.getFont()).getHeight();
			lineNumbersVertical.setValues(firstLine * height, visibleLines * height, 0, getLineCount() * height);
			lineNumbersVertical.setUnitIncrement(VERTICAL_SCROLLBAR_UNIT_INCREMENT_IN_LINES * height);
			lineNumbersVertical.setBlockIncrement(visibleLines * height);
		}

		final int width = painter.getWidth();
		if (horizontal != null && width != 0) {
			horizontal.setValues(-horizontalOffset, width, 0, width * 5);
			horizontal.setUnitIncrement(painter.getFontMetrics().charWidth('w'));
			horizontal.setBlockIncrement(width / 2);
		}
	}

	/**
	 * Returns the line displayed at the text area's origin.
	 */
	public final int getFirstLine() { return firstLine; }

	/**
	 * Sets the line displayed at the text area's origin and updates the scroll
	 * bars.
	 */
	public void setFirstLine(final int firstLine) {
		if (firstLine == this.firstLine) { return; }
		this.firstLine = firstLine;
		updateScrollBars();
		painter.repaint();
	}

	/**
	 * Returns the number of lines visible in this text area.
	 */
	public final int getVisibleLines() { return visibleLines; }

	/**
	 * Recalculates the number of visible lines. This should not be called directly.
	 */
	public final void recalculateVisibleLines() {
		if (painter == null) { return; }
		final int height = painter.getHeight();
		final int lineHeight = painter.getFontMetrics().getHeight();
		visibleLines = height / lineHeight;
		updateScrollBars();
	}

	/**
	 * Returns the horizontal offset of drawn lines.
	 */
	public final int getHorizontalOffset() { return horizontalOffset; }

	/**
	 * Sets the horizontal offset of drawn lines. This can be used to implement
	 * horizontal scrolling.
	 *
	 * @param horizontalOffset offset The new horizontal offset
	 */
	public void setHorizontalOffset(final int horizontalOffset) {
		if (horizontalOffset == this.horizontalOffset) { return; }
		this.horizontalOffset = horizontalOffset;
		if (horizontalOffset != horizontal.getValue()) { updateScrollBars(); }
		painter.repaint();
	}

	/**
	 * A fast way of changing both the first line and horizontal offset.
	 *
	 * @param firstLine        The new first line
	 * @param horizontalOffset The new horizontal offset
	 * @return True if any of the values were changed, false otherwise
	 */
	public boolean setOrigin(final int firstLine, final int horizontalOffset) {
		boolean changed = false;
		if (horizontalOffset != this.horizontalOffset) {
			this.horizontalOffset = horizontalOffset;
			changed = true;
		}

		if (firstLine != this.firstLine) {
			this.firstLine = firstLine;
			changed = true;
		}

		if (changed) {
			updateScrollBars();
			painter.repaint();
		}

		return changed;
	}

	/**
	 * Ensures that the caret is visible by scrolling the text area if necessary.
	 *
	 * @return True if scrolling was actually performed, false if the caret was
	 *         already visible
	 */
	public boolean scrollToCaret() {
		final int line = getCaretLine();
		final int lineStart = getLineStartOffset(line);
		final int offset = Math.max(0, Math.min(getLineLength(line) - 1, getCaretPosition() - lineStart));

		return scrollTo(line, offset);
	}

	/**
	 * Ensures that the specified line and offset is visible by scrolling the text
	 * area if necessary.
	 *
	 * @param line   The line to scroll to
	 * @param offset The offset in the line to scroll to
	 * @return True if scrolling was actually performed, false if the line and
	 *         offset was already visible
	 */
	public boolean scrollTo(final int line, final int offset) {
		// visibleLines == 0 before the component is realized
		// we can't do any proper scrolling then, so we have
		// this hack...
		if (visibleLines == 0) {
			setFirstLine(Math.max(0, line - electricScroll));
			return true;
		}

		int newFirstLine = firstLine;
		int newHorizontalOffset = horizontalOffset;

		if (line < firstLine + electricScroll) {
			newFirstLine = Math.max(0, line - electricScroll);
		} else if (line + electricScroll >= firstLine + visibleLines) {
			newFirstLine = line - visibleLines + electricScroll + 1;
			if (newFirstLine + visibleLines >= getLineCount()) { newFirstLine = getLineCount() - visibleLines; }
			if (newFirstLine < 0) { newFirstLine = 0; }
		}

		final int x = _offsetToX(line, offset);
		final int width = painter.getFontMetrics().charWidth('w');

		if (x < 0) {
			newHorizontalOffset = Math.min(0, horizontalOffset - x + width + 5);
		} else if (x + width >= painter.getWidth()) {
			newHorizontalOffset = horizontalOffset + painter.getWidth() - x - width - 5;
		}

		return setOrigin(newFirstLine, newHorizontalOffset);
	}

	/**
	 * Converts a line index to a y co-ordinate.
	 *
	 * @param line The line
	 */
	public int lineToY(final int line) {
		final FontMetrics fm = painter.getFontMetrics();
		return (line - firstLine) * fm.getHeight() - (fm.getLeading() + fm.getMaxDescent());
	}

	/**
	 * Converts a y co-ordinate to a line index.
	 *
	 * @param y The y co-ordinate
	 */
	public int yToLine(final int y) {
		final FontMetrics fm = painter.getFontMetrics();
		final int height = fm.getHeight();
		return Math.max(0, Math.min(getLineCount() - 1, y / height + firstLine));
	}

	/**
	 * Converts an offset in a line into an x co-ordinate. This is a slow version
	 * that can be used any time.
	 *
	 * @param line   The line
	 * @param offset The offset, from the start of the line
	 */
	public final int offsetToX(final int line, final int offset) {
		// don't use cached tokens
		painter.currentLineTokens = null;
		return _offsetToX(line, offset);
	}

	/**
	 * Converts an offset in a line into an x co-ordinate. This is a fast version
	 * that should only be used if no changes were made to the text since the last
	 * repaint.
	 *
	 * @param line   The line
	 * @param offset The offset, from the start of the line
	 */
	public int _offsetToX(final int line, final int offset) {
		final TokenMarker tokenMarker = getTokenMarker();

		/* Use painter's cached info for speed */
		FontMetrics fm = painter.getFontMetrics();

		getLineText(line, lineSegment);

		final int segmentOffset = lineSegment.offset;
		int x = horizontalOffset;

		/* If syntax coloring is disabled, do simple translation */
		if (tokenMarker == null) {
			lineSegment.count = offset;
			return x + Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
		}
		/* If syntax coloring is enabled, we have to do this because
		* tokens can vary in width */
		else {
			Token tokens;
			if (painter.currentLineIndex == line && painter.currentLineTokens != null) {
				tokens = painter.currentLineTokens;
			} else {
				painter.currentLineIndex = line;
				tokens = painter.currentLineTokens = tokenMarker.markTokens(lineSegment, line);
			}

			painter.getToolkit();
			final Font defaultFont = painter.getFont();
			final SyntaxStyle[] styles = painter.getStyles();

			for (;;) {
				final byte id = tokens.id;
				if (id == Token.END) { return x; }

				if (id == Token.NULL) {
					fm = painter.getFontMetrics();
				} else {
					fm = styles[id].getFontMetrics(defaultFont);
				}

				final int length = tokens.length;

				if (offset + segmentOffset < lineSegment.offset + length) {
					lineSegment.count = offset - (lineSegment.offset - segmentOffset);
					return x + Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
				} else {
					lineSegment.count = length;
					x += Utilities.getTabbedTextWidth(lineSegment, fm, x, painter, 0);
					lineSegment.offset += length;
				}
				tokens = tokens.next;
			}
		}
	}

	/**
	 * Converts an x co-ordinate to an offset within a line.
	 *
	 * @param line The line
	 * @param x    The x co-ordinate
	 */
	public int xToOffset(final int line, final int x) {
		final TokenMarker tokenMarker = getTokenMarker();

		/* Use painter's cached info for speed */
		FontMetrics fm = painter.getFontMetrics();

		getLineText(line, lineSegment);

		final char[] segmentArray = lineSegment.array;
		final int segmentOffset = lineSegment.offset;
		final int segmentCount = lineSegment.count;

		int width = horizontalOffset;

		if (tokenMarker == null) {
			for (int i = 0; i < segmentCount; i++) {
				final char c = segmentArray[i + segmentOffset];
				int charWidth;
				if (c == '\t') {
					charWidth = (int) painter.nextTabStop(width, i) - width;
				} else {
					charWidth = fm.charWidth(c);
				}

				if (painter.isBlockCaretEnabled()) {
					if (x - charWidth <= width) { return i; }
				} else {
					if (x - charWidth / 2 <= width) { return i; }
				}

				width += charWidth;
			}

			return segmentCount;
		} else {
			Token tokens;
			if (painter.currentLineIndex == line && painter.currentLineTokens != null) {
				tokens = painter.currentLineTokens;
			} else {
				painter.currentLineIndex = line;
				tokens = painter.currentLineTokens = tokenMarker.markTokens(lineSegment, line);
			}

			int offset = 0;
			painter.getToolkit();
			final Font defaultFont = painter.getFont();
			final SyntaxStyle[] styles = painter.getStyles();

			for (;;) {
				final byte id = tokens.id;
				if (id == Token.END) { return offset; }

				if (id == Token.NULL) {
					fm = painter.getFontMetrics();
				} else {
					fm = styles[id].getFontMetrics(defaultFont);
				}

				final int length = tokens.length;

				for (int i = 0; i < length; i++) {
					final char c = segmentArray[segmentOffset + offset + i];
					int charWidth;
					if (c == '\t') {
						charWidth = (int) painter.nextTabStop(width, offset + i) - width;
					} else {
						charWidth = fm.charWidth(c);
					}

					if (painter.isBlockCaretEnabled()) {
						if (x - charWidth <= width) { return offset + i; }
					} else {
						if (x - charWidth / 2 <= width) { return offset + i; }
					}

					width += charWidth;
				}

				offset += length;
				tokens = tokens.next;
			}
		}
	}

	/**
	 * Converts a point to an offset, from the start of the text.
	 *
	 * @param x The x co-ordinate of the point
	 * @param y The y co-ordinate of the point
	 */
	public int xyToOffset(final int x, final int y) {
		final int line = yToLine(y);
		final int start = getLineStartOffset(line);
		return start + xToOffset(line, x);
	}

	/**
	 * Returns the document this text area is editing.
	 */
	public final Document getDocument() { return document; }

	/**
	 * Sets the document this text area is editing.
	 *
	 * @param document The document
	 */
	public void setDocument(final SyntaxDocument document) {
		if (this.document == document) { return; }
		if (this.document != null) { this.document.removeDocumentListener(documentHandler); }
		this.document = document;

		document.addDocumentListener(documentHandler);

		select(0, 0);
		updateScrollBars();
		painter.repaint();
	}

	/**
	 * Returns the document's token marker. Equivalent to calling
	 * <code>getDocument().getTokenMarker()</code>.
	 */
	public final TokenMarker getTokenMarker() { return document.getTokenMarker(); }

	/**
	 * Sets the document's token marker. Equivalent to caling
	 * <code>getDocument().setTokenMarker()</code>.
	 *
	 * @param tokenMarker The token marker
	 */
	public final void setTokenMarker(final TokenMarker tokenMarker) {
		document.setTokenMarker(tokenMarker);
	}

	/**
	 * Returns the length of the document. Equivalent to calling
	 * <code>getDocument().getLength()</code>.
	 */
	public final int getDocumentLength() { return document.getLength(); }

	/**
	 * Returns the number of lines in the document.
	 */
	public final int getLineCount() { return document.getDefaultRootElement().getElementCount(); }

	/**
	 * Returns the line containing the specified offset.
	 *
	 * @param offset The offset
	 */
	public final int getLineOfOffset(final int offset) {
		return document.getDefaultRootElement().getElementIndex(offset);
	}

	/**
	 * Returns the start offset of the specified line.
	 *
	 * @param line The line
	 * @return The start offset of the specified line, or -1 if the line is invalid
	 */
	public int getLineStartOffset(final int line) {
		final Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null) {
			return -1;
		} else {
			return lineElement.getStartOffset();
		}
	}

	/**
	 * Returns the end offset of the specified line.
	 *
	 * @param line The line
	 * @return The end offset of the specified line, or -1 if the line is invalid.
	 */
	public int getLineEndOffset(final int line) {
		final Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null) {
			return -1;
		} else {
			return lineElement.getEndOffset();
		}
	}

	/**
	 * Returns the length of the specified line.
	 *
	 * @param line The line
	 */
	public int getLineLength(final int line) {
		final Element lineElement = document.getDefaultRootElement().getElement(line);
		if (lineElement == null) {
			return -1;
		} else {
			return lineElement.getEndOffset() - lineElement.getStartOffset() - 1;
		}
	}

	/**
	 * Returns the entire text of this text area.
	 */
	public String getText() {
		try {
			return document.getText(0, document.getLength());
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Sets the entire text of this text area.
	 */
	public void setText(final String text) {
		try {
			document.beginCompoundEdit();
			document.remove(0, document.getLength());
			document.insertString(0, text, null);
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
		} finally {
			document.endCompoundEdit();
		}
	}

	/**
	 * Returns the specified substring of the document.
	 *
	 * @param start The start offset
	 * @param len   The length of the substring
	 * @return The substring, or null if the offsets are invalid
	 */
	public final String getText(final int start, final int len) {
		try {
			return document.getText(start, len);
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
			return null;
		}
	}

	/**
	 * Copies the specified substring of the document into a segment. If the offsets
	 * are invalid, the segment will contain a null string.
	 *
	 * @param start   The start offset
	 * @param len     The length of the substring
	 * @param segment The segment
	 */
	public final void getText(final int start, final int len, final Segment segment) {
		try {
			document.getText(start, len, segment);
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
			segment.offset = segment.count = 0;
		}
	}

	/**
	 * Returns the text on the specified line.
	 *
	 * @param lineIndex The line
	 * @return The text, or null if the line is invalid
	 */
	public final String getLineText(final int lineIndex) {
		final int start = getLineStartOffset(lineIndex);
		return getText(start, getLineEndOffset(lineIndex) - start - 1);
	}

	/**
	 * Copies the text on the specified line into a segment. If the line is invalid,
	 * the segment will contain a null string.
	 *
	 * @param lineIndex The line
	 */
	public final void getLineText(final int lineIndex, final Segment segment) {
		final int start = getLineStartOffset(lineIndex);
		getText(start, getLineEndOffset(lineIndex) - start - 1, segment);
	}

	/**
	 * Returns the selection start offset.
	 */
	public final int getSelectionStart() { return selectionStart; }

	/**
	 * Returns the offset where the selection starts on the specified line.
	 */
	public int getSelectionStart(final int line) {
		if (line == selectionStartLine) {
			return selectionStart;
		} else if (rectSelect) {
			final Element map = document.getDefaultRootElement();
			final int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();

			final Element lineElement = map.getElement(line);
			final int lineStart = lineElement.getStartOffset();
			final int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd, lineStart + start);
		} else {
			return getLineStartOffset(line);
		}
	}

	/**
	 * Returns the selection start line.
	 */
	public final int getSelectionStartLine() { return selectionStartLine; }

	/**
	 * Sets the selection start. The new selection will be the new selection start
	 * and the old selection end.
	 *
	 * @param selectionStart The selection start
	 * @see #select(int,int)
	 */
	public final void setSelectionStart(final int selectionStart) {
		select(selectionStart, selectionEnd);
	}

	/**
	 * Returns the selection end offset.
	 */
	public final int getSelectionEnd() { return selectionEnd; }

	/**
	 * Returns the offset where the selection ends on the specified line.
	 */
	public int getSelectionEnd(final int line) {
		if (line == selectionEndLine) {
			return selectionEnd;
		} else if (rectSelect) {
			final Element map = document.getDefaultRootElement();
			final int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

			final Element lineElement = map.getElement(line);
			final int lineStart = lineElement.getStartOffset();
			final int lineEnd = lineElement.getEndOffset() - 1;
			return Math.min(lineEnd, lineStart + end);
		} else {
			return getLineEndOffset(line) - 1;
		}
	}

	/**
	 * Returns the selection end line.
	 */
	public final int getSelectionEndLine() { return selectionEndLine; }

	/**
	 * Sets the selection end. The new selection will be the old selection start and
	 * the new selection end.
	 *
	 * @param selectionEnd The selection end
	 * @see #select(int,int)
	 */
	public final void setSelectionEnd(final int selectionEnd) {
		select(selectionStart, selectionEnd);
	}

	/**
	 * Returns the caret position. This will either be the selection start or the
	 * selection end, depending on which direction the selection was made in.
	 */
	public final int getCaretPosition() { return biasLeft ? selectionStart : selectionEnd; }

	/**
	 * Returns the caret line.
	 */
	public final int getCaretLine() { return biasLeft ? selectionStartLine : selectionEndLine; }

	/**
	 * Returns the mark position. This will be the opposite selection bound to the
	 * caret position.
	 *
	 * @see #getCaretPosition()
	 */
	public final int getMarkPosition() { return biasLeft ? selectionEnd : selectionStart; }

	/**
	 * Returns the mark line.
	 */
	public final int getMarkLine() { return biasLeft ? selectionEndLine : selectionStartLine; }

	/**
	 * Sets the caret position. The new selection will consist of the caret position
	 * only (hence no text will be selected)
	 *
	 * @param caret The caret position
	 * @see #select(int,int)
	 */
	public final void setCaretPosition(final int caret) {
		select(caret, caret);
	}

	/**
	 * Selects all text in the document.
	 */
	public final void selectAll() {
		select(0, getDocumentLength());
	}

	/**
	 * Moves the mark to the caret position.
	 */
	public final void selectNone() {
		select(getCaretPosition(), getCaretPosition());
	}

	/**
	 * Selects from the start offset to the end offset. This is the general
	 * selection method used by all other selecting methods. The caret position will
	 * be start if start &lt; end, and end if end &gt; start.
	 *
	 * @param start The start offset
	 * @param end   The end offset
	 */
	public void select(final int start, final int end) {
		int newStart, newEnd;
		boolean newBias;
		if (start <= end) {
			newStart = start;
			newEnd = end;
			newBias = false;
		} else {
			newStart = end;
			newEnd = start;
			newBias = true;
		}

		if (newStart < 0 || newEnd > getDocumentLength()) {
			throw new IllegalArgumentException("Bounds out of" + " range: " + newStart + "," + newEnd);
		}

		// If the new position is the same as the old, we don't
		// do all this crap, however we still do the stuff at
		// the end (clearing magic position, scrolling)
		if (newStart != selectionStart || newEnd != selectionEnd || newBias != biasLeft) {
			final int newStartLine = getLineOfOffset(newStart);
			final int newEndLine = getLineOfOffset(newEnd);

			if (painter.isBracketHighlightEnabled()) {
				if (bracketLine != -1) { painter.invalidateLine(bracketLine); }
				updateBracketHighlight(end);
				if (bracketLine != -1) { painter.invalidateLine(bracketLine); }
			}

			painter.invalidateLineRange(selectionStartLine, selectionEndLine);
			painter.invalidateLineRange(newStartLine, newEndLine);

			document.addUndoableEdit(new CaretUndo(selectionStart, selectionEnd));

			selectionStart = newStart;
			selectionEnd = newEnd;
			selectionStartLine = newStartLine;
			selectionEndLine = newEndLine;
			biasLeft = newBias;

			fireCaretEvent();
		}

		// When the user is typing, etc, we don't want the caret
		// to blink
		blink = true;
		caretTimer.restart();

		// Disable rectangle select if selection start = selection end
		if (selectionStart == selectionEnd) { rectSelect = false; }

		// Clear the `magic' caret position used by up/down
		magicCaret = -1;
		scrollToCaret();
	}

	/**
	 * Returns the selected text, or null if no selection is active.
	 */
	public final String getSelectedText() {
		if (selectionStart == selectionEnd) { return null; }

		if (rectSelect) {
			// Return each row of the selection on a new line

			final Element map = document.getDefaultRootElement();

			int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();
			int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

			// Certain rectangles satisfy this condition...
			if (end < start) {
				final int tmp = end;
				end = start;
				start = tmp;
			}

			final StringBuffer buf = new StringBuffer();
			final Segment seg = new Segment();

			for (int i = selectionStartLine; i <= selectionEndLine; i++) {
				final Element lineElement = map.getElement(i);
				int lineStart = lineElement.getStartOffset();
				final int lineEnd = lineElement.getEndOffset() - 1;
				int lineLen = lineEnd - lineStart;

				lineStart = Math.min(lineStart + start, lineEnd);
				lineLen = Math.min(end - start, lineEnd - lineStart);

				getText(lineStart, lineLen, seg);
				buf.append(seg.array, seg.offset, seg.count);

				if (i != selectionEndLine) { buf.append('\n'); }
			}

			return buf.toString();
		} else {
			return getText(selectionStart, selectionEnd - selectionStart);
		}
	}

	/**
	 * Replaces the selection with the specified text.
	 *
	 * @param selectedText The replacement text for the selection
	 */
	public void setSelectedText(final String selectedText) {
		if (!editable) { throw new InternalError("Text component" + " read only"); }

		document.beginCompoundEdit();

		try {
			if (rectSelect) {
				final Element map = document.getDefaultRootElement();

				int start = selectionStart - map.getElement(selectionStartLine).getStartOffset();
				int end = selectionEnd - map.getElement(selectionEndLine).getStartOffset();

				// Certain rectangles satisfy this condition...
				if (end < start) {
					final int tmp = end;
					end = start;
					start = tmp;
				}

				int lastNewline = 0;
				int currNewline = 0;

				for (int i = selectionStartLine; i <= selectionEndLine; i++) {
					final Element lineElement = map.getElement(i);
					final int lineStart = lineElement.getStartOffset();
					final int lineEnd = lineElement.getEndOffset() - 1;
					final int rectStart = Math.min(lineEnd, lineStart + start);

					document.remove(rectStart, Math.min(lineEnd - rectStart, end - start));

					if (selectedText == null) { continue; }

					currNewline = selectedText.indexOf('\n', lastNewline);
					if (currNewline == -1) { currNewline = selectedText.length(); }

					document.insertString(rectStart, selectedText.substring(lastNewline, currNewline), null);

					lastNewline = Math.min(selectedText.length(), currNewline + 1);
				}

				if (selectedText != null && currNewline != selectedText.length()) {
					final int offset = map.getElement(selectionEndLine).getEndOffset() - 1;
					document.insertString(offset, "\n", null);
					document.insertString(offset + 1, selectedText.substring(currNewline + 1), null);
				}
			} else {
				document.remove(selectionStart, selectionEnd - selectionStart);
				if (selectedText != null) { document.insertString(selectionStart, selectedText, null); }
			}
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
			throw new InternalError("Cannot replace" + " selection");
		}
		// No matter what happends... stops us from leaving document
		// in a bad state
		finally {
			document.endCompoundEdit();
		}

		setCaretPosition(selectionEnd);
	}

	/**
	 * Returns true if this text area is editable, false otherwise.
	 */
	public final boolean isEditable() { return editable; }

	/**
	 * Sets if this component is editable.
	 *
	 * @param editable True if this text area should be editable, false otherwise
	 */
	public final void setEditable(final boolean editable) { this.editable = editable; }

	/**
	 * Returns the right click popup menu.
	 */
	public final JPopupMenu getRightClickPopup() { return popup; }

	/**
	 * Sets the right click popup menu.
	 *
	 * @param popup The popup
	 */
	public final void setRightClickPopup(final JPopupMenu popup) { this.popup = popup; }

	/**
	 * Returns the `magic' caret position. This can be used to preserve the column
	 * position when moving up and down lines.
	 */
	public final int getMagicCaretPosition() { return magicCaret; }

	/**
	 * Sets the `magic' caret position. This can be used to preserve the column
	 * position when moving up and down lines.
	 *
	 * @param magicCaret The magic caret position
	 */
	public final void setMagicCaretPosition(final int magicCaret) { this.magicCaret = magicCaret; }

	/**
	 * Similar to <code>setSelectedText()</code>, but overstrikes the appropriate
	 * number of characters if overwrite mode is enabled.
	 *
	 * @param str The string
	 * @see #setSelectedText(String)
	 * @see #isOverwriteEnabled()
	 */
	public void overwriteSetSelectedText(final String str) {
		// Don't overstrike if there is a selection
		if (!overwrite || selectionStart != selectionEnd) {
			setSelectedText(str);
			applySyntaxSensitiveHelp();
			return;
		}

		// Don't overstrike if we're on the end of
		// the line
		final int caret = getCaretPosition();
		final int caretLineEnd = getLineEndOffset(getCaretLine());
		if (caretLineEnd - caret <= str.length()) {
			setSelectedText(str);
			applySyntaxSensitiveHelp();
			return;
		}

		document.beginCompoundEdit();

		try {
			document.remove(caret, str.length());
			document.insertString(caret, str, null);
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
		} finally {
			document.endCompoundEdit();
		}
		applySyntaxSensitiveHelp();

	}

	JPopupMenu popupMenu;

	/**
	 * Returns true if overwrite mode is enabled, false otherwise.
	 */
	public final boolean isOverwriteEnabled() { return overwrite; }

	/**
	 * Sets if overwrite mode should be enabled.
	 *
	 * @param overwrite True if overwrite mode should be enabled, false otherwise.
	 */
	public final void setOverwriteEnabled(final boolean overwrite) {
		this.overwrite = overwrite;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns true if the selection is rectangular, false otherwise.
	 */
	public final boolean isSelectionRectangular() { return rectSelect; }

	/**
	 * Sets if the selection should be rectangular.
	 *
	 * @param overwrite True if the selection should be rectangular, false
	 *                  otherwise.
	 */
	public final void setSelectionRectangular(final boolean rectSelect) {
		this.rectSelect = rectSelect;
		painter.invalidateSelectedLines();
	}

	/**
	 * Returns the position of the highlighted bracket (the bracket matching the one
	 * before the caret)
	 */
	public final int getBracketPosition() { return bracketPosition; }

	/**
	 * Returns the line of the highlighted bracket (the bracket matching the one
	 * before the caret)
	 */
	public final int getBracketLine() { return bracketLine; }

	/**
	 * Adds a caret change listener to this text area.
	 *
	 * @param listener The listener
	 */
	public final void addCaretListener(final CaretListener listener) {
		listenerList.add(CaretListener.class, listener);
	}

	/**
	 * Removes a caret change listener from this text area.
	 *
	 * @param listener The listener
	 */
	public final void removeCaretListener(final CaretListener listener) {
		listenerList.remove(CaretListener.class, listener);
	}

	/**
	 * Deletes the selected text from the text area and places it into the
	 * clipboard.
	 */
	public void cut() {
		if (editable) {
			copy();
			setSelectedText("");
		}
	}

	/**
	 * Places the selected text into the clipboard.
	 */
	public void copy() {
		if (selectionStart != selectionEnd) {
			final Clipboard clipboard = getToolkit().getSystemClipboard();

			final String selection = getSelectedText();

			final int repeatCount = inputHandler.getRepeatCount();
			final StringBuffer buf = new StringBuffer();
			for (int i = 0; i < repeatCount; i++) {
				buf.append(selection);
			}

			clipboard.setContents(new StringSelection(buf.toString()), null);
		}
	}

	/**
	 * Inserts the clipboard contents into the text.
	 */
	public void paste() {
		if (editable) {
			final Clipboard clipboard = getToolkit().getSystemClipboard();
			try {
				// The MacOS MRJ doesn't convert \r to \n,
				// so do it here
				String selection = ((String) clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor))
						.replace('\r', '\n');

				final int repeatCount = inputHandler.getRepeatCount();
				final StringBuffer buf = new StringBuffer();
				for (int i = 0; i < repeatCount; i++) {
					buf.append(selection);
				}
				selection = buf.toString();
				setSelectedText(selection);
			} catch (final Exception e) {
				getToolkit().beep();
				System.err.println("Clipboard does not" + " contain a string");
			}
		}
	}

	/**
	 * Called by the AWT when this component is removed from it's parent. This stops
	 * clears the currently focused component.
	 */
	@Override
	public void removeNotify() {
		super.removeNotify();
		if (focusedComponent == this) { focusedComponent = null; }
	}

	/**
	 * Forwards key events directly to the input handler. This is slightly faster
	 * than using a KeyListener because some Swing overhead is avoided.
	 */
	@Override
	public void processKeyEvent(final KeyEvent evt) {
		if (inputHandler == null) { return; }
		switch (evt.getID()) {
		case KeyEvent.KEY_TYPED:
			inputHandler.keyTyped(evt);
			break;
		case KeyEvent.KEY_PRESSED:
			if (!checkPopupCompletion(evt)) { inputHandler.keyPressed(evt); }
			checkPopupMenu(evt);
			break;
		case KeyEvent.KEY_RELEASED:
			inputHandler.keyReleased(evt);
			break;
		}
	}

	// protected members
	protected static String CENTER = "center";
	protected static String RIGHT = "right";
	protected static String BOTTOM = "bottom";

	protected static JEditTextArea focusedComponent;
	protected static Timer caretTimer;

	protected TextAreaPainter painter;

	protected JPopupMenu popup;

	protected EventListenerList listenerList;
	protected MutableCaretEvent caretEvent;

	protected boolean caretBlinks;
	protected boolean caretVisible;
	protected boolean blink;

	protected boolean editable;

	protected int caretBlinkRate;
	protected int firstLine;
	protected int visibleLines;
	protected int electricScroll;

	protected int horizontalOffset;

	protected JScrollBar vertical;
	protected JScrollBar horizontal;
	protected boolean scrollBarsInitialized;

	protected InputHandler inputHandler;
	protected SyntaxDocument document;
	protected DocumentHandler documentHandler;

	protected Segment lineSegment;

	protected int selectionStart;
	protected int selectionStartLine;
	protected int selectionEnd;
	protected int selectionEndLine;
	protected boolean biasLeft;

	protected int bracketPosition;
	protected int bracketLine;

	protected int magicCaret;
	protected boolean overwrite;
	protected boolean rectSelect;
	// "unredoing" is mode used by DocumentHandler's insertUpdate() and removeUpdate()
	// to pleasingly select the text and location of the undo.   DPS 3-May-2010
	protected boolean unredoing = false;

	protected void fireCaretEvent() {
		final Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i--) {
			if (listeners[i] == CaretListener.class) { ((CaretListener) listeners[i + 1]).caretUpdate(caretEvent); }
		}
	}

	protected void updateBracketHighlight(final int newCaretPosition) {
		if (newCaretPosition == 0) {
			bracketPosition = bracketLine = -1;
			return;
		}

		try {
			final int offset = TextUtilities.findMatchingBracket(document, newCaretPosition - 1);
			if (offset != -1) {
				bracketLine = getLineOfOffset(offset);
				bracketPosition = offset - getLineStartOffset(bracketLine);
				return;
			}
		} catch (final BadLocationException bl) {
			bl.printStackTrace();
		}

		bracketLine = bracketPosition = -1;
	}

	protected void documentChanged(final DocumentEvent evt) {
		final DocumentEvent.ElementChange ch = evt.getChange(document.getDefaultRootElement());

		int count;
		if (ch == null) {
			count = 0;
		} else {
			count = ch.getChildrenAdded().length - ch.getChildrenRemoved().length;
		}

		final int line = getLineOfOffset(evt.getOffset());
		if (count == 0) {
			painter.invalidateLine(line);
		}
		// do magic stuff
		else if (line < firstLine) {
			setFirstLine(firstLine + count);
		}
		// end of magic stuff
		else {
			painter.invalidateLineRange(line, firstLine + visibleLines);
			updateScrollBars();
		}
	}

	class ScrollLayout implements LayoutManager {

		@Override
		public void addLayoutComponent(final String name, final Component comp) {
			if (name.equals(CENTER)) {
				center = comp;
			} else if (name.equals(RIGHT)) {
				right = comp;
			} else if (name.equals(BOTTOM)) {
				bottom = comp;
			} else if (name.equals(LEFT_OF_SCROLLBAR)) { leftOfScrollBar.addElement(comp); }
		}

		@Override
		public void removeLayoutComponent(final Component comp) {
			if (center == comp) { center = null; }
			if (right == comp) { right = null; }
			if (bottom == comp) {
				bottom = null;
			} else {
				leftOfScrollBar.removeElement(comp);
			}
		}

		@Override
		public Dimension preferredLayoutSize(final Container parent) {
			final Dimension dim = new Dimension();
			final Insets insets = getInsets();
			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			final Dimension centerPref = center.getPreferredSize();
			dim.width += centerPref.width;
			dim.height += centerPref.height;
			final Dimension rightPref = right.getPreferredSize();
			dim.width += rightPref.width;
			final Dimension bottomPref = bottom.getPreferredSize();
			dim.height += bottomPref.height;

			return dim;
		}

		@Override
		public Dimension minimumLayoutSize(final Container parent) {
			final Dimension dim = new Dimension();
			final Insets insets = getInsets();
			dim.width = insets.left + insets.right;
			dim.height = insets.top + insets.bottom;

			final Dimension centerPref = center.getMinimumSize();
			dim.width += centerPref.width;
			dim.height += centerPref.height;
			final Dimension rightPref = right.getMinimumSize();
			dim.width += rightPref.width;
			final Dimension bottomPref = bottom.getMinimumSize();
			dim.height += bottomPref.height;

			return dim;
		}

		@Override
		public void layoutContainer(final Container parent) {
			final Dimension size = parent.getSize();
			final Insets insets = parent.getInsets();
			final int itop = insets.top;
			int ileft = insets.left;
			final int ibottom = insets.bottom;
			final int iright = insets.right;

			final int rightWidth = right.getPreferredSize().width;
			final int bottomHeight = bottom.getPreferredSize().height;
			final int centerWidth = size.width - rightWidth - ileft - iright;
			final int centerHeight = size.height - bottomHeight - itop - ibottom;

			center.setBounds(ileft, itop, centerWidth, centerHeight);

			right.setBounds(ileft + centerWidth, itop, rightWidth, centerHeight);

			// Lay out all status components, in order
			final Enumeration status = leftOfScrollBar.elements();
			while (status.hasMoreElements()) {
				final Component comp = (Component) status.nextElement();
				final Dimension dim = comp.getPreferredSize();
				comp.setBounds(ileft, itop + centerHeight, dim.width, bottomHeight);
				ileft += dim.width;
			}

			bottom.setBounds(ileft, itop + centerHeight, size.width - rightWidth - ileft - iright, bottomHeight);
		}

		// private members
		private Component center;
		private Component right;
		private Component bottom;
		private final Vector leftOfScrollBar = new Vector();
	}

	static class CaretBlinker implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent evt) {
			if (focusedComponent != null && focusedComponent.hasFocus()) { focusedComponent.blinkCaret(); }
		}
	}

	class MutableCaretEvent extends CaretEvent {

		/**
		 *
		 */
		private static final long serialVersionUID = -1731647586986370865L;

		MutableCaretEvent() {
			super(JEditTextArea.this);
		}

		@Override
		public int getDot() { return getCaretPosition(); }

		@Override
		public int getMark() { return getMarkPosition(); }
	}

	class AdjustHandler implements AdjustmentListener {

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent evt) {
			if (!scrollBarsInitialized) { return; }

			// If this is not done, mousePressed events accumulate
			// and the result is that scrolling doesn't stop after
			// the mouse is released
			SwingUtilities.invokeLater(() -> {
				if (evt.getAdjustable() == vertical) {
					setFirstLine(vertical.getValue());
				} else {
					setHorizontalOffset(-horizontal.getValue());
				}
			});
		}
	}

	class ComponentHandler extends ComponentAdapter {

		@Override
		public void componentResized(final ComponentEvent evt) {
			recalculateVisibleLines();
			scrollBarsInitialized = true;
		}
	}

	class DocumentHandler implements DocumentListener {

		@Override
		public void insertUpdate(final DocumentEvent evt) {
			documentChanged(evt);

			final int offset = evt.getOffset();
			final int length = evt.getLength();

			// If event fired because of undo or redo, select inserted text. DPS 3-May-2010
			if (unredoing) {
				select(offset, offset + length);
				return;
			}

			int newStart;
			int newEnd;

			if (selectionStart > offset || selectionStart == selectionEnd && selectionStart == offset) {
				newStart = selectionStart + length;
			} else {
				newStart = selectionStart;
			}

			if (selectionEnd >= offset) {
				newEnd = selectionEnd + length;
			} else {
				newEnd = selectionEnd;
			}
			select(newStart, newEnd);
		}

		@Override
		public void removeUpdate(final DocumentEvent evt) {
			documentChanged(evt);

			final int offset = evt.getOffset();
			final int length = evt.getLength();

			// If event fired because of undo or redo, move caret to position of removal. DPS 3-May-2010
			if (unredoing) {
				select(offset, offset);
				setCaretPosition(offset);
				return;
			}

			int newStart;
			int newEnd;

			if (selectionStart > offset) {
				if (selectionStart > offset + length) {
					newStart = selectionStart - length;
				} else {
					newStart = offset;
				}
			} else {
				newStart = selectionStart;
			}

			if (selectionEnd > offset) {
				if (selectionEnd > offset + length) {
					newEnd = selectionEnd - length;
				} else {
					newEnd = offset;
				}
			} else {
				newEnd = selectionEnd;
			}
			select(newStart, newEnd);
		}

		@Override
		public void changedUpdate(final DocumentEvent evt) {}
	}

	class DragHandler implements MouseMotionListener {

		@Override
		public void mouseDragged(final MouseEvent evt) {
			if (popup != null && popup.isVisible()) { return; }

			setSelectionRectangular((evt.getModifiers() & InputEvent.CTRL_MASK) != 0);
			select(getMarkPosition(), xyToOffset(evt.getX(), evt.getY()));
		}

		@Override
		public void mouseMoved(final MouseEvent evt) {}
	}

	class FocusHandler implements FocusListener {

		@Override
		public void focusGained(final FocusEvent evt) {
			setCaretVisible(true);
			focusedComponent = JEditTextArea.this;
		}

		@Override
		public void focusLost(final FocusEvent evt) {
			setCaretVisible(false);
			focusedComponent = null;
		}
	}

	// Added by DPS, 5-5-2010. Allows use of mouse wheel to scroll.
	// Scrolling as fast as I could, the most notches I could get in
	// one MouseWheelEvent was 3.  Normally it will be 1.  Nonetheless,
	// this will scroll up to the number in the event, subject to
	// scrollability of the text in its viewport.
	class MouseWheelHandler implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			final int maxMotion = Math.abs(e.getWheelRotation()) * LINES_PER_MOUSE_WHEEL_NOTCH;
			if (e.getWheelRotation() < 0) {
				setFirstLine(getFirstLine() - Math.min(maxMotion, getFirstLine()));
			} else {
				setFirstLine(getFirstLine() + Math.min(maxMotion, Math.max(0, getLineCount() - (getFirstLine()
						+ visibleLines))));
			}
		}
	}

	class MouseHandler extends MouseAdapter {

		@Override
		public void mousePressed(final MouseEvent evt) {
			requestFocus();

			// Focus events not fired sometimes?
			setCaretVisible(true);
			focusedComponent = JEditTextArea.this;

			if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0 && popup != null) {
				popup.show(painter, evt.getX(), evt.getY());
				return;
			}

			final int line = yToLine(evt.getY());
			final int offset = xToOffset(line, evt.getX());
			final int dot = getLineStartOffset(line) + offset;

			switch (evt.getClickCount()) {
			case 1:
				doSingleClick(evt, line, offset, dot);
				break;
			case 2:
				// It uses the bracket matching stuff, so
				// it can throw a BLE
				try {
					doDoubleClick(evt, line, offset, dot);
				} catch (final BadLocationException bl) {
					bl.printStackTrace();
				}
				break;
			case 3:
				doTripleClick(evt, line, offset, dot);
				break;
			}
		}

		private void doSingleClick(final MouseEvent evt, final int line, final int offset, final int dot) {
			if ((evt.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
				rectSelect = (evt.getModifiers() & InputEvent.CTRL_MASK) != 0;
				select(getMarkPosition(), dot);
			} else {
				setCaretPosition(dot);
			}
		}

		private void doDoubleClick(final MouseEvent evt, final int line, final int offset, final int dot)
				throws BadLocationException {
			// Ignore empty lines
			if (getLineLength(line) == 0) { return; }

			try {
				int bracket = TextUtilities.findMatchingBracket(document, Math.max(0, dot - 1));
				if (bracket != -1) {
					int mark = getMarkPosition();
					// Hack
					if (bracket > mark) {
						bracket++;
						mark--;
					}
					select(mark, bracket);
					return;
				}
			} catch (final BadLocationException bl) {
				bl.printStackTrace();
			}

			// Ok, it's not a bracket... select the word
			final String lineText = getLineText(line);
			char ch = lineText.charAt(Math.max(0, offset - 1));

			String noWordSep = (String) document.getProperty("noWordSep");
			if (noWordSep == null) { noWordSep = ""; }

			// If the user clicked on a non-letter char,
			// we select the surrounding non-letters
			final boolean selectNoLetter = !Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1;

			int wordStart = 0;

			for (int i = offset - 1; i >= 0; i--) {
				ch = lineText.charAt(i);
				if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1)) {
					wordStart = i + 1;
					break;
				}
			}

			int wordEnd = lineText.length();
			for (int i = offset; i < lineText.length(); i++) {
				ch = lineText.charAt(i);
				if (selectNoLetter ^ (!Character.isLetterOrDigit(ch) && noWordSep.indexOf(ch) == -1)) {
					wordEnd = i;
					break;
				}
			}

			final int lineStart = getLineStartOffset(line);
			select(lineStart + wordStart, lineStart + wordEnd);
		}

		private void doTripleClick(final MouseEvent evt, final int line, final int offset, final int dot) {
			select(getLineStartOffset(line), getLineEndOffset(line) - 1);
		}
	}

	class CaretUndo extends AbstractUndoableEdit {

		/**
		 *
		 */
		private static final long serialVersionUID = 6175232514094149931L;
		private int start;
		private int end;

		CaretUndo(final int start, final int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public boolean isSignificant() { return false; }

		@Override
		public String getPresentationName() { return "caret move"; }

		@Override
		public void undo() throws CannotUndoException {
			super.undo();

			select(start, end);
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();

			select(start, end);
		}

		@Override
		public boolean addEdit(final UndoableEdit edit) {
			if (edit instanceof CaretUndo) {
				final CaretUndo cedit = (CaretUndo) edit;
				start = cedit.start;
				end = cedit.end;
				cedit.die();

				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Return any relevant tool tip text for token at specified position. Keyword
	 * match must be exact. DPS 24-May-2010
	 *
	 * @param x x-coordinate of current position
	 * @param y y-coordinate of current position
	 * @return String containing appropriate tool tip text. Possibly HTML-encoded.
	 */
	// Is used for tool tip only (not popup menu)
	public String getSyntaxSensitiveToolTipText(final int x, final int y) {
		String result = null;
		final int line = yToLine(y);
		final ArrayList matches = getSyntaxSensitiveHelpAtLineOffset(line, xToOffset(line, x), true);
		if (matches == null) { return null; }
		final int length = PopupHelpItem.maxExampleLength(matches) + 2;
		result = "<html>";
		for (int i = 0; i < matches.size(); i++) {
			final PopupHelpItem match = (PopupHelpItem) matches.get(i);
			result += (i == 0 ? "" : "<br>") + "<tt>" + match.getExamplePaddedToLength(length).replaceAll(" ", "&nbsp;")
					+ "</tt>" + match.getDescription();
		}
		return result + "</html>";
	}

	/**
	 * Constructs string for auto-indent feature. Returns empty string if
	 * auto-intent is disabled or if line has no leading white space. Uses
	 * getLeadingWhiteSpace(). Is used by InputHandler when processing key press for
	 * Enter key. DPS 31-Dec-2010
	 *
	 * @return String containing auto-indent characters to be inserted into text
	 */
	public String getAutoIndent() {
		return Globals.getSettings().getBooleanSetting(Settings.AUTO_INDENT) ? getLeadingWhiteSpace() : "";
	}

	/**
	 * Makes a copy of leading white space (tab or space) from the current line and
	 * returns it. DPS 31-Dec-2010
	 *
	 * @return String containing leading white space of current line. Empty string
	 *         if none.
	 */
	public String getLeadingWhiteSpace() {
		final int line = getCaretLine();
		final int lineLength = getLineLength(line);
		String indent = "";
		if (lineLength > 0) {
			final String text = getText(getLineStartOffset(line), lineLength);
			for (int position = 0; position < text.length(); position++) {
				final char character = text.charAt(position);
				if (character == '\t' || character == ' ') {
					indent += character;
				} else {
					break;
				}
			}
		}
		return indent;
	}

	//////////////////////////////////////////////////////////////////////////////////
	// Get relevant help information at specified position.  Returns ArrayList of
	// PopupHelpItem with one per match, or null if no matches.
	// The "exact" parameter is set depending on whether the match has to be
	// exact or whether a prefix match will do.  The token "s" will not match
	// any instruction names if exact is true, but will match "sw", "sh", etc
	// if exact is false.  The former is helpful for mouse-movement-based tool
	// tips (this is what you have).  The latter is helpful for caret-based tool
	// tips (this is what you can do).
	private ArrayList getSyntaxSensitiveHelpAtLineOffset(final int line, final int offset, final boolean exact) {
		ArrayList matches = null;
		final TokenMarker tokenMarker = getTokenMarker();
		if (tokenMarker != null) {
			final Segment lineSegment = new Segment();
			this.getLineText(line, lineSegment); // fill segment with info from this line
			Token tokens = tokenMarker.markTokens(lineSegment, line);
			final Token tokenList = tokens;
			int tokenOffset = 0;
			Token tokenAtOffset = null;
			// cool for following the tokens...
			//System.out.print("(JEditTextArea.java) Token Stream:");
			Token toke = tokens;
			for (;;) {
				//System.out.print(" "+toke.id+"("+toke.length+")");
				if (toke.id == Token.END) { break; }
				toke = toke.next;
			}
			//System.out.println();

			for (;;) {
				final byte id = tokens.id;
				if (id == Token.END) { break; }
				final int length = tokens.length;
				if (offset > tokenOffset && offset <= tokenOffset + length) {
					tokenAtOffset = tokens;
					break;
				}
				tokenOffset += length;
				tokens = tokens.next;
			}
			if (tokenAtOffset != null) {
				final String tokenText = lineSegment.toString().substring(tokenOffset, tokenOffset
						+ tokenAtOffset.length);
				if (exact) {
					matches = tokenMarker.getTokenExactMatchHelp(tokenAtOffset, tokenText);
				} else {
					matches = tokenMarker.getTokenPrefixMatchHelp(lineSegment.toString(), tokenList, tokenAtOffset,
							tokenText);
				}
			}
		}
		return matches;
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Compose and display syntax-sensitive help. Typically invoked upon typing a key.
	// Results in popup menu.  Is not used for creating tool tips.
	private void applySyntaxSensitiveHelp() {
		if (!mars.Globals.getSettings().getBooleanSetting(mars.Settings.POPUP_INSTRUCTION_GUIDANCE)) { return; }
		final int line = getCaretLine();
		final int lineStart = getLineStartOffset(line);
		final int offset = Math.max(1, Math.min(getLineLength(line), getCaretPosition() - lineStart));
		final ArrayList helpItems = getSyntaxSensitiveHelpAtLineOffset(line, offset, false);
		if (helpItems == null && popupMenu != null) {
			popupMenu.setVisible(false);
			popupMenu = null;
		}
		if (helpItems != null) {
			popupMenu = new JPopupMenu();
			final int length = PopupHelpItem.maxExampleLength(helpItems) + 2;
			for (int i = 0; i < helpItems.size(); i++) {
				final PopupHelpItem item = (PopupHelpItem) helpItems.get(i);
				final JMenuItem menuItem = new JMenuItem("<html><tt>" + item.getExamplePaddedToLength(length)
						.replaceAll(" ", "&nbsp;") + "</tt>" + item.getDescription() + "</html>");
				if (item.getExact()) {
					// The instruction name is completed so the role of the popup changes
					// to that of floating help to assist in operand specification.
					menuItem.setSelected(false);
					// Want menu item to be disabled but that causes rendered text to be hard to see.
					// Spent a couple hours on workaround with no success.  The UI uses
					// UIManager.get("MenuItem.disabledForeground") property to determine rendering
					// color but this is done each time the text is rendered (paintText). There is
					// no setter for the menu item itself.  The UIManager property is used for all
					// menus not just the editor's popup help menu, so you can't just set the disabled
					// foreground color to, say, black and leave it.  Tried several techniques without
					// success.  The only solution I found was a hack:  writing a BasicMenuItem UI
					// subclass that consists of hacked override of its paintText() method.  But even
					// this required use of "SwingUtilities2" class which has been deprecated for years
					// So in the end I decided just to leave the menu item enabled.  It will highlight
					// but does nothing if selected.  DPS 11-July-2014

					// menuItem.setEnabled(false);
				} else {
					// Typing of instruction/directive name is still in progress; the action listener
					// will complete it when its menu item is selected.
					menuItem.addActionListener(new PopupHelpActionListener(item.getTokenText(), item.getExample()));
				}
				popupMenu.add(menuItem);
			}
			popupMenu.pack();
			final int y = lineToY(line);
			final int x = offsetToX(line, offset);
			final int height = painter.getFontMetrics(painter.getFont()).getHeight();
			final int width = painter.getFontMetrics(painter.getFont()).charWidth('w');
			final int menuXLoc = x + width + width + width;
			final int menuYLoc = y + height + height; // display below;
			// Modified to always display popup BELOW the current line.
			// This was done in response to negative student feedback about
			// the popup blocking information they needed to (e.g. operands from
			// previous instructions).  Note that if menu is long enough and
			// current cursor position is low enough, the menu will bottom out at the
			// bottom of the screen and extend above the current line. DPS 23-Dec-2010
			popupMenu.show(this, menuXLoc, menuYLoc);
			this.requestFocusInWindow(); // get cursor back from the menu
		}
	}

	// Carries out the instruction/directive completion when popup menu
	// item is selected.
	private class PopupHelpActionListener implements ActionListener {

		private final String tokenText, text;

		public PopupHelpActionListener(final String tokenText, final String text) {
			this.tokenText = tokenText;
			this.text = text.split(" ")[0];
		}

		// Completion action will insert either a tab or space character following the
		// completed instruction mnemonic.  Inserts a tab if tab key was pressed;
		// space otherwise.  Get this information from the ActionEvent.
		@Override
		public void actionPerformed(final ActionEvent e) {
			final String insert = e.getActionCommand().charAt(0) == '\t' ? "\t" : " ";
			if (tokenText.length() >= text.length()) {
				overwriteSetSelectedText(insert);
			} else {
				overwriteSetSelectedText(text.substring(tokenText.length()) + insert);
			}
		}
	}

	private void checkAutoIndent(final KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			final int line = getCaretLine();
			if (line <= 0) { return; }
			final int previousLine = line - 1;
			final int previousLineLength = getLineLength(previousLine);
			if (previousLineLength <= 0) { return; }
			final String previous = getText(getLineStartOffset(previousLine), previousLineLength);
			String indent = "";
			for (int position = 0; position < previous.length(); position++) {
				final char character = previous.charAt(position);
				if (character == '\t' || character == ' ') {
					indent += character;
				} else {
					break;
				}
			}
			overwriteSetSelectedText(indent);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Called after processing a Key Pressed event. Will make popup menu disappear if
	// Enter or Escape keys pressed.  Will update if Backspace or Delete pressed.
	// Not really concerned with modifiers here.
	private void checkPopupMenu(final KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE || evt.getKeyCode() == KeyEvent.VK_DELETE) {
			applySyntaxSensitiveHelp();
		}
		if ((evt.getKeyCode() == KeyEvent.VK_ENTER || evt.getKeyCode() == KeyEvent.VK_ESCAPE) && popupMenu != null
				&& popupMenu.isVisible()) {
			popupMenu.setVisible(false);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////
	// Called before processing Key Pressed event. If popup menu is visible, will process
	// tab and enter keys to select from the menu, and arrow keys to traverse the menu.
	private boolean checkPopupCompletion(final KeyEvent evt) {
		if ((evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) && popupMenu != null
				&& popupMenu.isVisible() && popupMenu.getComponentCount() > 0) {
			final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
			if (path.length < 1 || !(path[path.length - 1] instanceof AbstractButton)) { return false; }
			final AbstractButton item = (AbstractButton) path[path.length - 1].getComponent();
			if (item.isEnabled()) {
				int index = popupMenu.getComponentIndex(item);
				if (index < 0) { return false; }
				if (evt.getKeyCode() == KeyEvent.VK_UP) {
					index = index == 0 ? popupMenu.getComponentCount() - 1 : index - 1;
				} else {
					index = index == popupMenu.getComponentCount() - 1 ? 0 : index + 1;
				}
				// Neither popupMenu.setSelected() nor popupMenu.getSelectionModel().setSelectedIndex()
				// have the desired effect (changing the menu item selected).  Found references to
				// this in a Sun forum.  http://forums.sun.com/thread.jspa?forumID=57&threadID=641745
				// The solution, as shown here, is to use invokeLater.
				final MenuElement[] newPath = new MenuElement[2];
				newPath[0] = path[0];
				newPath[1] = (MenuElement) popupMenu.getComponentAtIndex(index);
				SwingUtilities.invokeLater(() -> MenuSelectionManager.defaultManager().setSelectedPath(newPath));
				return true;
			} else {
				return false;
			}
		}
		if ((evt.getKeyCode() == KeyEvent.VK_TAB || evt.getKeyCode() == KeyEvent.VK_ENTER) && popupMenu != null
				&& popupMenu.isVisible() && popupMenu.getComponentCount() > 0) {
			final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
			if (path.length < 1 || !(path[path.length - 1] instanceof AbstractButton)) { return false; }
			final AbstractButton item = (AbstractButton) path[path.length - 1].getComponent();
			if (item.isEnabled()) {
				final ActionListener[] listeners = item.getActionListeners();
				if (listeners.length > 0) {
					listeners[0].actionPerformed(new ActionEvent(item, ActionEvent.ACTION_FIRST, evt
							.getKeyCode() == KeyEvent.VK_TAB ? "\t" : " "));
					return true;
				}
			}
		}
		return false;
	}

	static {
		caretTimer = new Timer(500, new CaretBlinker());
		caretTimer.setInitialDelay(500);
		caretTimer.start();
	}
}
