//  
//  @(#)XJSVGScroller.java	1/2003
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

import dip.misc.Log;
import dip.misc.Utils;

import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.BorderLayout;


import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentAdapter;

import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.AffineTransform;

import java.awt.event.ComponentEvent; 

import javax.swing.JScrollBar;
import javax.swing.Box;
import javax.swing.JPanel;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import org.apache.batik.swing.JSVGCanvas;

import org.apache.batik.swing.svg.SVGUserAgent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;

import org.apache.batik.swing.gvt.JGVTComponentListener;

import org.apache.batik.bridge.*;
import org.w3c.dom.svg.*;

import org.apache.batik.util.SVGConstants;

import org.w3c.dom.svg.SVGPoint;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;
import org.w3c.dom.svg.SVGMatrix;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;

/**
*	Implements the scroll bars that allow scrolling of an XSVGCanvas.
*
*/
public class XJSVGScroller extends JPanel implements EventListener
{
	// i18n
	private static final String I18N_ZOOM_FACTOR	= "XJSVGScroller.zoom.text";
	
	private final JSVGCanvas canvas;
	
	private MapPanel mp;
	private JPanel horizontalPanel;
	private JScrollBar vertical;
	private JScrollBar horizontal;
	private SBListener hsbListener;
	private SBListener vsbListener;
	
	private double matrix[] = new double[6];
	
	private Rectangle2D.Float viewBox = null;		// SVG Root element viewbox 
	private boolean ignoreScrollChange = false;
	
	
	/**
	*	Creates a XJSVGScroller, which will scroll an XJSVGCanvas.
	*
	*/
	public XJSVGScroller(MapPanel mp, JSVGCanvas xcanvas)
	{
		super();
		this.canvas = xcanvas;
		this.mp = mp;
		
		// create components
		vertical = new JScrollBar(JScrollBar.VERTICAL, 0, 0, 0, 0);
		horizontal = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 0);
		
		// create a spacer next to the horizontal bar
		horizontalPanel = new JPanel(new BorderLayout());
		horizontalPanel.add(horizontal, BorderLayout.CENTER);
		horizontalPanel.add(Box.createRigidArea(
						new Dimension( 	vertical.getPreferredSize().width, 
										horizontal.getPreferredSize().height)), 
						BorderLayout.EAST);
		
		// listeners
		hsbListener = new SBListener();
		horizontal.getModel().addChangeListener(hsbListener);
		horizontal.addMouseListener(hsbListener);
		horizontal.addMouseMotionListener(hsbListener);
		
		vsbListener = new SBListener();
		vertical.getModel().addChangeListener(vsbListener);
		vertical.addMouseListener(vsbListener);
		vertical.addMouseMotionListener(vsbListener);
		
		
		// by default, scrollbars are not visible
		horizontalPanel.setVisible(false);
		vertical.setVisible(false);
		
		addMouseWheelListener(new WheelListener());
		
		// layout
		setLayout(new BorderLayout());
		add(canvas, BorderLayout.CENTER);
		add(vertical, BorderLayout.EAST);
		add(horizontalPanel, BorderLayout.SOUTH);
		
		// inform of ZOOM events (to print size in statusbar)
		canvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter()
		{
			public void documentLoadingCompleted(SVGDocumentLoaderEvent e)
			{
				SVGSVGElement root = canvas.getSVGDocument().getRootElement();
				root.addEventListener(SVGConstants.SVG_SVGZOOM_EVENT_TYPE, XJSVGScroller.this, false);
			}// documentLoadingCompleted()			
		});
		
		// canvas listeners
		XListener xlistener = new XListener();
		canvas.addJGVTComponentListener(xlistener);
		this.addComponentListener(xlistener);
		canvas.addGVTTreeRendererListener(xlistener);
	}// XJSVGScroller()
	
	
	/**
	*	Resets this object (for reloads),
	*	releasing any cached data and recomputing
	*	scroll extents.
	*/
	public void reset()
	{
		viewBox = null;
		horizontalPanel.setVisible(false);
		vertical.setVisible(false);
		revalidate();
	}// reset()
	
	
	/**
	*	Sets the translation portion of the transform based upon the
	*	current scroll bar position
	*/
	private void setScrollPosition()
	{
		AffineTransform at = canvas.getRenderingTransform();
		if(at != null)
		{
			at.getMatrix(matrix);	// order: m00 m10 m01 m11 m02 m12
			
			// set x coord
			matrix[4] = (double) -horizontal.getValue();		// translate X = m02
			
			// set y coord
			matrix[5] = (double) -vertical.getValue();			// translate Y = m12
			
			at = new AffineTransform(matrix);
			canvas.setRenderingTransform(at);
		}
	}// setScrollPosition()
	
	
	/**
	*	MouseWheel Listener
	*	<p>
	*	Provides mouse wheel support. The mouse wheel will scroll the currently
	*	displayed scroll bar, if only one is displayed. If two scrollbars are 
	*	displayed, the mouse wheel will only scroll the vertical scrollbar.
	*/
	private class WheelListener implements MouseWheelListener
	{
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			final JScrollBar sb = (vertical.isVisible()) ? vertical : horizontal;	// vertical is preferred
			
			if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
			{
				final int amt = e.getUnitsToScroll() * sb.getUnitIncrement();
				sb.setValue(sb.getValue() + amt);
			}			
			else if(e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL)
			{
				final int amt = e.getWheelRotation() * sb.getBlockIncrement();
				sb.setValue(sb.getValue() + amt);
			}
			
		}// mouseWheelMoved()
	}// inner class WheelListener
	
	
	/**
	*	Advanced JScrollPane listener. 
	*	<p>
	*	<b>A separate listener must be attached to each scrollbar, since we keep track
	*	of mouse state for each scrollbar separately!</b>
	*	<p>
	*	This coalesces drag events so we don't track them, and 'passes through' click
	*	events. It doesn't coalesce as many events as it should, but it helps considerably.
	*
	*/
	private class SBListener extends MouseAdapter implements ChangeListener, MouseMotionListener
	{
			private boolean inDrag = false;			// 'true' if we are in a drag (versus a click)
			private boolean inClick = false;		// true if we are in a click
			
			public SBListener()
			{
			}// SBListener()
			
			
			public synchronized void mouseDragged(MouseEvent e)
			{
				inDrag = true;
			}// mouseDragged()
			
			
			public synchronized void mousePressed(MouseEvent e)
			{
				// we've pressed the mouse
				inClick = true;
			}// mousePressed()
			
			
			public synchronized void mouseReleased(MouseEvent e)
			{
				if(inDrag)
				{
					// This is the 'end' of a drag
					// 
					setScrollPosition();
				}
				
				// reset drag indicator
				inDrag = false;
				inClick = false;
			}// mouseReleased()
			
			public void mouseMoved(MouseEvent e)
			{
				// do nothing
			}// mouseMoved()
			
			public synchronized void stateChanged(ChangeEvent e)
			{
				// only respond to changes if we are NOT being dragged
				// and ignoreScrollChange is not set
				if(!inDrag && !inClick && !ignoreScrollChange)
				{
					//System.out.println(e);
					//System.out.println(vertical.getModel());
					//System.out.println(horizontal.getModel());
					setScrollPosition();
				}
			}// stateChanged()
			
	}// inner class SBListener
	
	
	
	/** Handle scroll, zoom, and resize events */
	private class XListener extends ComponentAdapter implements JGVTComponentListener, GVTTreeRendererListener
	{
		private boolean isReady = false;
		
		public void componentTransformChanged(ComponentEvent evt)
		{
			if(isReady)
			{
				resizeScrollBars(canvas.getRenderingTransform(), canvas.getViewBoxTransform(), canvas.getSize());
			}
		}// componentTransformChanged()
		
		
		public void componentResized(ComponentEvent evt)
		{
			if(isReady)
			{
				resizeScrollBars(canvas.getRenderingTransform(), canvas.getViewBoxTransform(), canvas.getSize());
			}
		}// componentResized()
		
		
		public void gvtRenderingCompleted(GVTTreeRendererEvent e)
		{
			isReady = true;
		}// gvtRenderingCompleted()
		
		
		public void gvtRenderingCancelled(GVTTreeRendererEvent e)
		{
			// do nothing
		}// gvtRenderingCancelled()
		
		
		public void gvtRenderingFailed(GVTTreeRendererEvent e)
		{
			// do nothing
		}// gvtRenderingFailed()
		
		public void gvtRenderingPrepare(GVTTreeRendererEvent e)
		{
			// do nothing
		}// gvtRenderingPrepare()
		
		public void gvtRenderingStarted(GVTTreeRendererEvent e)
		{
			// do nothing
		}// gvtRenderingStarted()
 		
	}// inner class XListener
	
	
	/**
	*	Compute the scrollbar extents, and determine if 
	*	scrollbars should be visible.
	*
	*/
	private void resizeScrollBars(final AffineTransform crt, final  AffineTransform vbt, Dimension vpSize)
	{
		ignoreScrollChange = true;
		
		/*
		System.out.println("** resizeScrollBars()");
		System.out.println("   crt: "+crt);
		System.out.println("   vbt: "+vbt);
		System.out.println("   vpSize: "+vpSize);
		*/
		
		checkAndSetViewBoxRect();
		
		if(crt == null || vpSize == null || viewBox == null)
		{
			return;
		}
		
		// compute maximum SVG extents, in pixels
		int maxW = 0;
		int maxH = 0;
		
		if(isEqv1f(crt.getScaleX()) && isEqv1f(crt.getScaleY()))
		{
			// case 1: crt scale is approx equal to 1.0f
			maxW = (int) (Math.abs(viewBox.getWidth() - viewBox.getX()) * vbt.getScaleX() * crt.getScaleX());
			maxH = (int) (Math.abs(viewBox.getHeight() - viewBox.getY()) * vbt.getScaleY() * crt.getScaleY());
		}
		else
		{
			// case 2: crt scale is not equiv to 1.0f; we are enlarged or reduced
			maxW = (int) (Math.abs(viewBox.getWidth() - viewBox.getX()) * vbt.getScaleX());
			maxH = (int) (Math.abs(viewBox.getHeight() - viewBox.getY()) * vbt.getScaleY());
		}
		
		//System.out.println("   maxW = "+maxW+"; maxH = "+maxH);
		
		// compute translation
		final int tx = (int) ((crt.getTranslateX() > 0) ? 0 : -crt.getTranslateX());
		final int ty = (int) ((crt.getTranslateY() > 0) ? 0 : -crt.getTranslateY());
		
		//System.out.println("   tx = "+tx+"; ty = "+ty);
		
		// set scroll params
		vertical.setValues(ty, vpSize.height, 0, maxH);
		horizontal.setValues(tx, vpSize.width, 0, maxW);
		
		// set block scroll; this should be equal to a full 'page', 
		// minus a small amount to keep a portion in view
		// that small amount is 10%.
		vertical.setBlockIncrement( (int) (0.9f * vpSize.height) );
		horizontal.setBlockIncrement( (int) (0.9f * vpSize.width) );
		
		// set unit scroll. This is arbitrary, but we define
		// it to be 20% of the current viewport. 
		vertical.setUnitIncrement( (int) (0.2f * vpSize.height) );
		horizontal.setUnitIncrement( (int) (0.2f * vpSize.width) );
		
		// display scrollbars, if appropriate
		// (if scaled document size is larger than viewport size) 
		horizontalPanel.setVisible( (maxW > vpSize.width) );
		vertical.setVisible( (maxH > vpSize.height) );
		
		ignoreScrollChange = false;
		//System.out.println("  -- end resizeScrollBars()");
	}// resizeScrollBars()
	
	
	/** Determine if following float is close to 1.0f (+/- 0.001) */
	private boolean isEqv1f(double in)
	{
		if(in < 0.999 || in > 0.999)
		{
			return false;	// not close enought to 1
		}
		
		return true;	// close to 1
	}// isEqv1f()
	
	
	/** 
	*	Derives the SVG Viewbox from the SVG root element. 
	*	Caches it. Assumes that it will not change.
	*
	*/
	private void checkAndSetViewBoxRect()
	{
		if(viewBox == null)
		{
			SVGDocument doc = canvas.getSVGDocument();
			if(doc != null)
			{
				SVGSVGElement el = doc.getRootElement();
				if(el != null)
				{
					String viewBoxStr = el.getAttributeNS(null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE);
					float[] rect = ViewBox.parseViewBoxAttribute(el, viewBoxStr);
					viewBox = new Rectangle2D.Float(rect[0], rect[1], rect[2], rect[3]); 
					
					//System.out.println("  ** viewBox rect set: "+viewBox);
					//System.out.println("  ** doc size: "+canvas.getSVGDocumentSize());
				}
			}
		}
	}// checkAndSetViewBoxRect()
	
	/** 
	*	Handles registered DOM UI events	
	*/
	public void handleEvent(Event evt)
	{
		if(evt.getTarget() instanceof SVGSVGElement)
		{
			// if it was a ZOOM event, show the magnification factor.
			if(evt.getType() == SVGConstants.SVG_SVGZOOM_EVENT_TYPE)
			{
				SVGSVGElement svg = (SVGSVGElement) evt.getTarget();
				mp.getStatusBar().setText(
					Utils.getLocalString(I18N_ZOOM_FACTOR, new Float(svg.getCurrentScale())) );
			}
		}
	}// handleEvent()
	
}// class XJSVGScroller
