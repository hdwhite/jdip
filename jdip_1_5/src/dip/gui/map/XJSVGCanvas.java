//  
//  @(#)XJSVGCanvas.java	1/2003
//  
//	NOTE: this is NOT a GPL license
//	
//  Copyright 2003 Zachary DelProposto, zsd@umich.edu All Rights Reserved.
//  The source code and text in this module, JSVGScrollCanvas.java, 
//  may be freely used and modified for any purpose. If you do use
//  this code, I only require that you give me credit
//
//  THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY
//  OF ANY KIND, NOT EVEN THE IMPLIED WARRANTY OF
//  MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE,
//  ASSUMES NO RESPONSIBILITY FOR ANY CONSEQUENCE
//  RESULTING FROM THE USE, MODIFICATION, OR
//  REDISTRIBUTION OF THIS SOFTWARE
//  
//  I would appreciate any changes or improvments to this code that increase
//  functionality or eliminate bugs. It is not a requirement that you submit 
//  changes to me or make your modifications public, but, it would be appreciated.
//  
//
package dip.gui.map;

import dip.gui.dialog.ErrorDialog;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Container;
import java.awt.Window;
import java.awt.Cursor;

import java.awt.geom.AffineTransform;

import javax.swing.Scrollable;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.SVGUserAgent;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.BridgeContext;

// test
import java.awt.*;
import java.awt.geom.*;
import org.apache.batik.swing.*;
import org.apache.batik.bridge.*;
import org.apache.batik.gvt.CanvasGraphicsNode;
import org.w3c.dom.svg.SVGSVGElement;


/**
*	Provides for enhanced functionality over a standard JSVGCanvas.
*	<p>
*	Consists of a modified mouse/key listners and a special transform
*	listener which allows the XSVGScroller to properly function.
*
*
*
*/
public class XJSVGCanvas extends JSVGCanvas 
{
	// constants
	private static final int	MIN_DRAG_DELTA = 5;		// min pixels to count as a drag
	
	// instance variables
	/** The default unit scroll increment */
	private int unitIncrement = 10;
	/** The default block scroll increment */
	private int blockIncrement = 30;
	/** Minimum scale value (if > 0.0) */
	private double minScale = 0.0f;
	/** Maximum scale value (if > 0.0) */
	private double maxScale = 0.0f;
	
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private boolean isValidating = false;
	
	
	/**
    *	Creates a new XJSVGCanvas.
    *
    *	@param ua a SVGUserAgent instance or null.
    *	@param eventsEnabled Whether the GVT tree should be reactive to mouse and
    *	key events.
    *	@param selectableText Whether the text should be selectable.
    */
	public XJSVGCanvas(SVGUserAgent ua, boolean eventsEnabled, boolean selectableText)
	{
		super(ua, eventsEnabled, selectableText);
		setMaximumSize(screenSize);
	}// XJSVGCanvas()
	
	
	/** Creates a new XJSVGCanvas */
	public XJSVGCanvas()
	{
		this(null, false, false);
	}// XJSVGCanvas()
	
	
	/** Sets if this is we should validate SVG or not */
	public void setValidating(boolean value)
	{
		isValidating = value;
	}// setValidating()
	
	
	/**
	*	Overrides createListener() to return our own Listener, with 
	*	several new features we need.
	*
	*/
	protected Listener createListener()
	{
		return new XJSVGCanvasListener();
	}// createListener()
	
	
	/**
	*	Overrides createUserAgent() to return our own UserAgent, which 
	*	allows selectable validation control of the parser.
	*
	*/
	protected UserAgent createUserAgent()
	{
		return new XJSVGUserAgent();
	}// createUserAgent()
	
	
	/** 
	*	Sets the parent component, to which key events are sent.
	*
	*/
	public void setParent(Component c)
	{
		((XJSVGCanvasListener) listener).setParent(c);
	}// setParent()
	
	
	/**
	*	Specialized Listener that provides additional functionality.
	*	<p>
	*	New Features:
	*	<ul>
	*		<li>Short drags are interpreted as clicks
	*		<li>Key events can be passed to the parent (as defined by setParent())
	*	</ul>
	*/
	protected class XJSVGCanvasListener extends JSVGCanvas.CanvasSVGListener
	{
			private int dragX;					// start drag X coord
			private int dragY;					// start drag Y coord
			private boolean inDrag = false;		// 'true' if we are in a drag (versus a click)
			private Component parent = null;	// parent component for events
			
			
			public XJSVGCanvasListener()
			{
			}// XJSVGCanvasListener()
			
			
			public void mouseDragged(MouseEvent e)
			{
				inDrag = true;
				super.mouseDragged(e);
			}// mouseDragged()
			
			
			public void mousePressed(java.awt.event.MouseEvent e)
			{
				// set drag start coordinates
				dragX = e.getX();
				dragY = e.getY();
				super.mousePressed(e);
			}// mousePressed()
			
			
			public void mouseReleased(java.awt.event.MouseEvent e)
			{
				if(inDrag)
				{
					int dx = Math.abs(e.getX() - dragX);
					int dy = Math.abs(e.getY() - dragY);
					
					if(dx < MIN_DRAG_DELTA && dy < MIN_DRAG_DELTA)
					{
						// our drag was short! dispatch a CLICK event.
						//
						MouseEvent click = new MouseEvent(
							e.getComponent(),
							MouseEvent.MOUSE_CLICKED,
							e.getWhen(),
							e.getModifiersEx(),		// modifiers
							e.getX(),
							e.getY(),
							e.getClickCount(),
							e.isPopupTrigger(),
							e.getButton() );
							
						super.mouseClicked(click);
					}
					else
					{
						// not a short drag; return original event
						super.mouseReleased(e);
					}
				}
				
				// reset drag
				inDrag = false;
			}// mouseReleased()
			
			
			public void keyPressed(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyPressed(e);
			}// keyPressed()
			
			
			public void keyReleased(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyReleased(e);
			}// keyReleased()
			
			
			public void keyTyped(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyTyped(e);
			}// keyTyped()
			
			
			/** Set parent to receive key events; null if none. */
			public void setParent(Component c)
			{
				parent = c;
			}// setParent()
			
			
			/**
			*	Override to prevent default behavior of pack(), and instead, use
			*	validate(), which uses the preferred size.
			*
			*/
			public void setMySize(Dimension d)
			{
				//setPreferredSize(d);	// is this required?? seems to work w/o
				//invalidate();			// is this required?? seems to work w/o
				SwingUtilities.getWindowAncestor(XJSVGCanvas.this).validate(); 
			}// setMySize()
			
	}// nested class XJSVGCanvasListener
	
	
	/**
	*	Specialized UserAgent that checks outer class for validation parameter
	*	and subclasses error and message dialogs.
	*/
	protected class XJSVGUserAgent extends JSVGCanvas.CanvasUserAgent
	{
		public XJSVGUserAgent()
		{
			super();
		}// XJSVGUserAgent()
		
 		public boolean isXMLParserValidating()
		{
			return XJSVGCanvas.this.isValidating;
		}// isXMLParserValidating()
		
		/** 
		*	Do nothing. We don't want the Batik 
		*	CursorManager updating our cursor.
		*/
		public void setSVGCursor(Cursor c)
		{
			// do nothing.
		}// setSVGCursor()
		
		
		/** Displays an SVG error Exception using an ErrorDialog */
		public void displayError(Exception ex)
		{
			ErrorDialog.displaySerious(findParent(), ex);
		}// displayError()
		
		
		/** Displays an SVG error String using an ErrorDialog */
		public void displayError(String message)
		{
			ErrorDialog.displaySerious(findParent(), new Exception(message));
		}// message()
		
		
		/** Find the parent frame, if possible. */
		private JFrame findParent()
		{
			// find parent frame, if possible
			Component comp = XJSVGCanvas.this.getParent();
			while(comp != null)
			{
				if(comp instanceof JFrame)
				{
					return (JFrame) comp;
				}
				
				comp = comp.getParent();
			}
			
			return null;
		}// findParent()
		
	}// nested class XJSVGUserAgent
  	
	
	/**
	*	Calls GVTTransformListener.transformChanged(), after setting
	*	the rendering transform of the JSVGCanvas.
    *
	*	@param at an AffineTransform.
	*/
	public void setRenderingTransform(AffineTransform at) 
	{
		// check to see that we are not zooming too little
		if(minScale > 0.0 && (at.getScaleX() < minScale || at.getScaleY() < minScale))
		{
			return; 	// reject transform
		}
		
		// check to see that we are not zooming too much
		if(maxScale > 0.0 && (at.getScaleX() > maxScale || at.getScaleY() > maxScale))
		{
			return;		// reject transform
		}
		
		// proceed with setting the rendering transform...
		super.setRenderingTransform(at);
	}// setRenderingTransform()
	
	
	/** Sets the minimum allowable scale size. 1.0 == no scaling. any negative value or 0 disables. */
	public void setMinimumScale(double value)
	{
		minScale = value;
	}// setMinimumScale()
	
	
	/** Sets the maximum allowable scale size. 1.0 == no scaling. any negative value or 0 disables. */
	public void setMaximumScale(double value)
	{
		maxScale = value;
	}// setMaximumScale()
	
	
	/**
	*	Implements our new resizing behavior. This prevents 
	*	scaling from changing all the time when the window size changes.
	*	<p>
	*  	Updates the value of the transform used for rendering.
	* 	Return true if a repaint is required, otherwise false.
	*/
    protected boolean updateRenderingTransform() 
	{
        if((svgDocument == null) || (gvtRoot == null))
        {   
			return false;
		}
		
		// Code provided by Mark Claassen
		try 
		{
			SVGSVGElement elt = svgDocument.getRootElement();
			Dimension d;
			if(false)
			{
				d = getSize();
			}
			else
			{
				Dimension2D d2 = getSVGDocumentSize();
				d = new Dimension((int)d2.getWidth(),(int)d2.getHeight());
			}
			Dimension oldD = prevComponentSize;
			if(oldD == null) { oldD = d; }
			prevComponentSize = d;
			
			if(d.width  < 1) { d.width  = 1; }
			if(d.height < 1) { d.height = 1; }
			
			AffineTransform at = ViewBox.getViewTransform(fragmentIdentifier, elt, d.width, d.height);
			CanvasGraphicsNode cgn = getCanvasGraphicsNode();
			AffineTransform vt = cgn.getViewingTransform();
			if(at.equals(vt))
			{
				// No new transform
				// Only repaint if size really changed.
				return ((oldD.width != d.width) || (oldD.height != d.height));
			}
		} 
		catch (BridgeException e) 
		{
            userAgent.displayError(e);
        }
        return true;
    }// updateRenderingTransform()
	
	
}// class XJSVGCanvas
