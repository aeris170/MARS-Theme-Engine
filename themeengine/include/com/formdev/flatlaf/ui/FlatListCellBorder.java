/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package themeengine.include.com.formdev.flatlaf.ui;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Cell border for {@link javax.swing.DefaultListCellRenderer}
 * (used by {@link javax.swing.JList}).
 * <p>
 * Uses separate cell margins from UI defaults to allow easy customizing.
 *
 * @author Karl Tauber
 */
public class FlatListCellBorder
	extends FlatLineBorder
{
	final boolean showCellFocusIndicator = UIManager.getBoolean( "List.showCellFocusIndicator" );

	protected FlatListCellBorder() {
		super( UIManager.getInsets( "List.cellMargins" ), UIManager.getColor( "List.cellFocusColor" ) );
	}

	//---- class Default ------------------------------------------------------

	/**
	 * Border for unselected cell that uses margins, but does not paint focus indicator border.
	 */
	public static class Default
		extends FlatListCellBorder
	{
		@Override
		public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
			// do not paint focus indicator border
		}
	}

	//---- class Focused ------------------------------------------------------

	/**
	 * Border for focused unselected cell that uses margins and paints focus indicator border.
	 */
	public static class Focused
		extends FlatListCellBorder
	{
	}

	//---- class Selected -----------------------------------------------------

	/**
	 * Border for selected cell that uses margins and paints focus indicator border
	 * if enabled (List.showCellFocusIndicator=true) and exactly one item is selected.
	 */
	public static class Selected
		extends FlatListCellBorder
	{
		@Override
		public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
			if( !showCellFocusIndicator )
				return;

			// paint focus indicator border only if exactly one item is selected
			JList<?> list = (JList<?>) SwingUtilities.getAncestorOfClass( JList.class, c );
			if( list != null && list.getMinSelectionIndex() == list.getMaxSelectionIndex() )
				return;

			super.paintBorder( c, g, x, y, width, height );
		}
	}
}
