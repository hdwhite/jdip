//
//  @(#)FixedSizeDocFilter.java		9/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package dip.gui.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.text.*;

/**
*	A DocumentFilter that limits the size of a textfield
*/
public class FixedSizeDocFilter extends DocumentFilter
{
	protected int maxSize;
	
	
	/** 
	*	Adds a FixedSizeDocFilter Filter into any JTextComponent
	*/
	public static void setFilter(JTextComponent jtc, int size)
	{
		if(jtc == null || size < 0)
		{
			throw new IllegalArgumentException();
		}
		
		AbstractDocument doc = (AbstractDocument) jtc.getDocument();
		doc.setDocumentFilter(new FixedSizeDocFilter(size));
	}// setFilter()
	
	
	/** Create a DocumentFilter that limits the maximum number of characters */
	protected FixedSizeDocFilter(int limit)
	{
		maxSize = limit;
	}// FixedSizeDocFilter()
	
	
	public void insertString(DocumentFilter.FilterBypass fb, int offset, String str,
			AttributeSet attr) throws BadLocationException
	{
		replace(fb, offset, 0, str, attr);
	}// insertString()
	
	
	public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
			String s, AttributeSet attrs) throws BadLocationException
	{
		int newLength = fb.getDocument().getLength() - length + s.length();
		if (newLength <= maxSize)
		{
			fb.replace(offset, length, s, attrs);
		} 
		else
		{
			throw new BadLocationException("Max size of document exceeded", offset);
		}
	}// replace()

}// class FixedSizeDocFilter
