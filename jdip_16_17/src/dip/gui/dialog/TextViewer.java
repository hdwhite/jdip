//
//  @(#)TextViewer.java	1.00	4/1/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
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
package dip.gui.dialog;

import dip.gui.ClientMenu;
import dip.gui.swing.XJTextPane;
import dip.gui.swing.XJEditorPane;
import dip.gui.swing.XJScrollPane;
import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.prefs.GeneralPreferencePanel;
import dip.misc.Utils;
import dip.misc.SimpleFileFilter;
import dip.gui.swing.XJFileChooser;
import dip.misc.Log;

import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;
import javax.swing.text.html.*;
import java.util.regex.*;

import java.io.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;

/**
*	Display and (optionally) edit Text inside a HeaderDialog.
*	<p>
*	May display plain or HTML-formatted text. Has a menu allowing
*	(as appropriate) cut/copy/paste/select all/clear of contents.
*	<p>
*	Note: When constructing a TextViewer, don't forget to set if modality 
*	with setModal() and the title, with setTitle().
*	<p>
*	Nonmodal textboxes have some convenince methods available to allow
*	lazy-loading of text. This improves perceived responsiveness.
*
*/
public class TextViewer extends HeaderDialog
{
	/** "Loading" HTML message */
	private static final String WAIT_MESSAGE = "TextViewer.message.wait";
	
	// CommandBar : Actions
	private static final String SAVEAS_ACTION_CMD = "edit_saveas_action";
	
	// CommandBar : Icons
	private static final String ICON_SAVEAS = "resource/common/icons/24x24/stock_save_as_24.png";
	private static final String ICON_CUT 	= "resource/common/icons/24x24/stock_cut_24.png";
	private static final String ICON_COPY 	= "resource/common/icons/24x24/stock_copy_24.png";
	private static final String ICON_PASTE 	= "resource/common/icons/24x24/stock_paste_24.png";
	
	// CommandBar : Text : resource keys (from dip.gui.ClientMenu.java)
	private static final String TEXT_SAVEAS 	= "FILE_SAVEAS";
	private static final String TEXT_CUT 		= "TV_EDIT_CUT";
	private static final String TEXT_COPY 		= "TV_EDIT_COPY";
	private static final String TEXT_PASTE 		= "TV_EDIT_PASTE";
	private static final String TEXT_SELECTALL 	= "TV_EDIT_SELECTALL";
	
	/** Content Type: text/html */
	public static final String CONTENT_HTML = "text/html";
	
	/** Content Type: text/plain */
	public static final String CONTENT_PLAIN = "text/plain";
	
	/** Default text area font */
	protected static final Font tvFont = new Font("SansSerif", Font.PLAIN, 14);
	/** Text margin */
	private static final int TEXT_INSETS = 5;
	
	private boolean _isAccepted = false;
	private AcceptListener acceptListener = null;
	private JEditorPane textPane;
	private JScrollPane jsp;
	
	private JButton bPaste = null;
	private JButton bCut = null;
	
	/**
	*	Display the TextViewer, and return <code>true</code> if the 
	*	text was acceptable, or false otherwise. If the dialog is 
	*	closed / cancelled, false is returned.
	*
	*/
	public boolean displayDialog()
	{
		pack();
		setSize(Utils.getScreenSize(0.62f));
		Utils.centerIn(this, getParent());
		setVisible(true);
		return _isAccepted;
	}// displayDialog()
	
	/** Create a non-modal TextViewer */
	public TextViewer(JFrame parent)
	{
		this(parent, false, false);
	}// TextViewer()
	
	/** Create a TextViewer */
	public TextViewer(final JFrame parent, boolean isModal)
	{
		this(parent, isModal, false);
	}// TextViewer()
	
	/** Create a TextViewer */
	public TextViewer(final JFrame parent, boolean isModal, boolean showCommandBar)
	{
		super(parent, "", isModal);
		
		// text pane
		textPane = new XJEditorPane();
		textPane.setEditable(true);
		textPane.setMargin(new Insets(TEXT_INSETS, TEXT_INSETS, TEXT_INSETS, TEXT_INSETS));
		textPane.setFont(tvFont);
		
		new java.awt.dnd.DropTarget(textPane, new FileDropTargetListener()
		{
			public void processDroppedFiles(File[] files)
			{
				final Document doc = textPane.getDocument();
				
				for(int i=0; i<files.length; i++)
				{
					StringBuffer sb = new StringBuffer();
					BufferedReader br = null;
					
					try
					{
						br = new BufferedReader(new FileReader(files[i]));
						String line = br.readLine();
						while(line != null)
						{
							sb.append(line);
							sb.append('\n');
							line = br.readLine();
						}
					}
					catch(IOException e)
					{
						ErrorDialog.displayFileIO(parent, e, files[i].getName());
					}
					finally
					{
						try
						{
							if(br != null)
							{
								br.close();
							}
						}
						catch(IOException e)
						{
							ErrorDialog.displayFileIO(parent, e, files[i].getName());
						}
					}
					
					try
					{
						doc.insertString(0, sb.toString(), null);
					}
					catch(BadLocationException ble)
					{
						Log.println("TextViewer error: ", ble);
					}
				}
			}// processDroppedFiles()
		});
		
		// allow a modifiable transfer handler
		textPane.setTransferHandler(new javax.swing.TransferHandler()
		{
			public void exportToClipboard(JComponent comp, Clipboard clip, int action) 
			{
				if(comp instanceof JTextComponent)
				{
					try
					{
						JEditorPane textPane = (JEditorPane) comp;
						final int selStart = textPane.getSelectionStart();
						final int selEnd = textPane.getSelectionEnd();
						
						Document doc = textPane.getDocument();
						String text = null;
						
						// don't export as HTML (if we are text/html). Export as filtered text.
						// with newlines as appropriate.
						//
						if(doc instanceof HTMLDocument)
						{
							try
							{
								StringWriter sw = new StringWriter();
								HTMLWriter hw = new HTMLWriter(sw, (HTMLDocument) doc, selStart, (selEnd-selStart));
								hw.write();
								text = filterHTML(filterExportedText(sw.toString()));
							}
							catch(Exception hwe)
							{
								text = null;
							}
						}
						
						// if we are NOT an HTMLDocument, or, the above failed,
						// this is the standard cop-out that always works.
						if(text == null)
						{
							text = doc.getText(selStart, selEnd-selStart);
							text = TextViewer.this.filterExportedText(text);
						}
						
						StringSelection contents = new StringSelection(text);
						clip.setContents(contents, null);
						
						// support for move
                        if(action == TransferHandler.MOVE)
						{
                            doc.remove(selStart, selEnd-selStart);
                        }
						
					}
					catch(BadLocationException ble)
					{
						// do nothing
					}
					catch(IllegalStateException ise)
					{
						// could happen, say, if the clipboard is unavailable
						Log.println("TextViewer::exportToClipboard(): "+ise);
					}
				}
			}
			
			
			public boolean importData(JComponent comp, Transferable t) 
			{
				if(comp instanceof JTextComponent && textPane.isEditable())
				{
					// we don't want the BEST flavor, we want the Java String 
					// flavor. If that doesn't exist, we'll use the "best"
					// text flavor.
					DataFlavor stringDF = null;
					
					DataFlavor[] dfs = t.getTransferDataFlavors();
					for(int i=0; i<dfs.length; i++)
					{
						if(dfs[i].equals(DataFlavor.stringFlavor))
						{
							stringDF = dfs[i];
							break;
						}
					}
					
					
					// Use String flavor by default. It's easiest.
					// 
					if(stringDF != null)
					{
						try
						{
							Object obj = t.getTransferData(stringDF) ;
							if(obj instanceof String)
							{
								String importText = (String) obj;
								textPane.replaceSelection(importText);
								return true;
							}
						}
						catch(Exception e)
						{}	// do nothing
					}
					else
					{
						//	Plan "B". I'm not sure if this is 
						//	really nescessary.
						//						
						DataFlavor bestTextFlavor = DataFlavor.selectBestTextFlavor(t.getTransferDataFlavors());
						if(bestTextFlavor != null)
						{
							// typical / fancy case
							Reader reader = null;
							try
							{
								reader = bestTextFlavor.getReaderForText(t);
								char[] buffer = new char[128];
								StringBuffer sb = new StringBuffer(2048);
								
								int nRead = reader.read(buffer);
								while(nRead != -1)
								{
									sb.append(buffer, 0, nRead);
									nRead = reader.read(buffer);
								}
								textPane.replaceSelection(sb.toString());
								return true;
							}
							catch(Exception e)
							{} // do nothing
							finally
							{
								if(reader != null)
								{
									try 
									{ 
										reader.close(); 
									}
									catch(IOException e)
									{}
								}
							}
						}
					}
				}
				return false;
			}// importData()
			
			public boolean canImport(JComponent comp, DataFlavor[] transferFlavors)
			{
				if(comp instanceof JTextComponent && textPane.isEditable())
				{
					// any text type is acceptable.
					return (DataFlavor.selectBestTextFlavor(transferFlavors) != null);
				}
				return false;
			}// canImport()
		});
		
		// content pane setup
		jsp = new XJScrollPane(textPane);
		
		JPanel p = new JPanel(new BorderLayout());
		p.add(BorderLayout.CENTER, jsp);
		
		if(showCommandBar)
		{
			p.add(BorderLayout.SOUTH, createCommandBar());
		}
		
		createDefaultContentBorder(p);
		setContentPane(p);
	}// TextViewer()
	
	
	
	
	/** 
	*	Allows modification of the exported document text prior to
	*	placing it in the system clipboard. Thus this method is called
	*	after a copy() occurs, but before the data is placed into the
	*	clipboard. This method should NOT return null.
	*	<p>
	*	By default, this method will search for unicode arrow \u2192 
	*	and replace it with "->". 
	*
	*/
	protected String filterExportedText(String in)
	{
		return Utils.replaceAll(in, "\u2192", "->");
	}// filterExportedText()
	
	
	/**
	*	Simple HTML filter. This creates 'plain text' from
	*	a "text/html" MIME type. All this does is exclude
	*	content between angle brackets.
	*/
	protected String filterHTML(String in)
	{
		StringBuffer out = new StringBuffer(in.length());
		
		boolean noCopy = false;
		final int len = in.length();
		for(int i=0; i<len; i++)
		{
			final char c = in.charAt(i);
			if(c == '<')
			{
				noCopy = true;
			}
			else if(c == '>')
			{
				noCopy = false;
			}
			else
			{
				if(!noCopy)
				{
					out.append(c);
				}
			}
		}
		
		return out.toString();
	}// filterHTML()
	
	/**
	*	Sets the content type to "text/HTML", sets the 
	*	loading message (WAIT_MESSAGE), then displays
	*	the dialog. 
	*	<p>
	*	This only works for non-modal dialogs!
	*/
	public void lazyLoadDisplayDialog(TVRunnable r)
	{
		if(r == null)
		{
			throw new IllegalArgumentException();
		}
		
		if(isModal())
		{
			throw new IllegalStateException("lazyLoadDisplayDialog(): only for NON modal dialogs.");
		}
		
		setContentType(CONTENT_HTML);
		setText(Utils.getLocalString(WAIT_MESSAGE));
		displayDialog();
		r.setTV(this);
		Thread t = new Thread(r);
		t.start();
	}// lazyLoad()
	
	
	/** Update dialog text. Lazily. Only works for non-modal dialogs. */
	public void lazyDialogTextUpdate(TVRunnable r)
	{
		if(r == null)
		{
			throw new IllegalArgumentException();
		}
		
		if(isModal())
		{
			throw new IllegalStateException("lazyDialogTextUpdate(): only for NON modal dialogs.");
		}
		
		setText(Utils.getLocalString(WAIT_MESSAGE));
		r.setTV(this);
		Thread t = new Thread(r);
		t.start();
	}// lazyDialogTextUpdate()
	
	
	
	/** Lazy Loading worker thread; must be subclassed */
	public abstract static class TVRunnable implements Runnable
	{
		private TextViewer tv;
		
		/** Create a TVRunnable */
		public TVRunnable()
		{
		}// TVRunnable()
		
		/** This method must be implemented by subclasses */
		public abstract void run();
		
		/** Used internally by lazyLoadDisplayDialog */
		private void setTV(TextViewer tv)
		{
			this.tv = tv;
		}// setTV()
		
		/** Set the text */
		protected final void setText(String text)
		{
			if(tv == null)
			{
				throw new IllegalStateException();
			}
			
			tv.setText(text);
		}// setText()
	}// nested class TVRunnable
	
	
	/** Change how Horizontal scrolling is handled. */
	public void setHorizontalScrollBarPolicy(int policy)
	{
		jsp.setHorizontalScrollBarPolicy(policy);
	}// setHorizontalScrollBarPolicy()
	
	
	/** Set the Content Type (e.g., "text/html", or "text/plain") of the TextViewer */
	public void setContentType(String text)
	{
		textPane.setContentType(text);
		Document doc = textPane.getDocument();
		if(doc instanceof HTMLDocument)
		{
			((HTMLDocument) doc).setBase(Utils.getResourceBase());
		}
	}// setContentType()
	
	/** Set Font. Use is not recommended if content type is "text/html". */
	public void setFont(Font font)
	{
		textPane.setFont(font);
	}// setFont()
	
	
	/** Get the JEditorPane component */
	public JEditorPane getEditorPane()
	{
		return textPane;
	}// getEditorPane()
	
	/** 
	*	Set the AcceptListener. If no AcceptListener is desired, 
	*	the AcceptListener may be set to null.
	*/
	public void setAcceptListener(AcceptListener value)
	{	
		acceptListener = value;
	}// setAcceptListener()
	
	
	/** Set if this TextViewer is editable */
	public void setEditable(boolean value) 	
	{
		textPane.setEditable(value);
		
		if(bCut != null)
		{
			bCut.setEnabled(value);
		}
		
		if(bPaste != null)
		{
			bPaste.setEnabled(value);
		}
	}// setEditable()
	
	/** Set if this TextViewer is highlightable */
	public void setHighlightable(boolean value)
	{
		if(!value)
		{
			textPane.setHighlighter(null);
		}
		else
		{
			textPane.setHighlighter(new javax.swing.text.DefaultHighlighter());
		}
	}// setHighlightable()
	
	/** Set the TextViewer text. Note: setContentType() should be called first. */
	public void setText(String value)
	{
		textPane.setText(value);
		textPane.setCaretPosition(0); // scroll to top		
	}// setText()
	
	/** Get the TextViewer text. */
	public String getText()
	{
		return textPane.getText();
	}// getText()
	
	/** AcceptListener: This class is called when the "Accept" button in clicked. */
	public interface AcceptListener
	{
		/**
		*	Determines if the text is acceptable.
		*	<p>
		*	If it is acceptable (true) the dialog will close. 
		*	If it is unacceptable (false), the dialog may close or stay
		*	open, depending upon the value returned by 
		*	getCloseDialogAfterUnacceptable()
		*
		*/
		public boolean isAcceptable(TextViewer t);
		
		
		/**
		* 	If true, the dialog closes after unacceptable input is given (but a warning
		* 	message could be displayed). If false, the dialog is not closed.
		*/
		public boolean getCloseDialogAfterUnacceptable();
	}// inner interface AcceptListener
	
	
	/** Close() override. Calls AcceptListener (if any) on OK or Close actions. */
	protected void close(String actionCommand)
	{
		if(isOKorAccept(actionCommand))
		{
			// if no accept() handler, assume accepted.
			_isAccepted = true;
			if(acceptListener != null)
			{
				_isAccepted = acceptListener.isAcceptable(this);
				if(acceptListener.getCloseDialogAfterUnacceptable())
				{
					dispose();
				}
			}
			
			if(_isAccepted) 
			{
				dispose();
			}
			
			return;
		}
		else
		{
			_isAccepted = false;
			dispose();
		}
	}// close()
	
	
	/** A specialized Listener for registered JTextComponent Actions */
	protected class JTextComponentActionListener implements ActionListener
	{
		private final JTextComponent textComponent;
		
		public JTextComponentActionListener(JTextComponent component) 
		{
			if(component == null) { throw new IllegalArgumentException(); }
			textComponent = component;
		}// JTextComponentActionListener()
		
		public void actionPerformed(ActionEvent e)
		{
			final String action = e.getActionCommand();
			final Action a = textComponent.getActionMap().get(action);
			
			if(a != null)
			{
				a.actionPerformed(new ActionEvent(textComponent,
					ActionEvent.ACTION_PERFORMED, null));
				
				if(action.equals(DefaultEditorKit.selectAllAction))
				{
					// bug workaround: DefaultEditorKit.selectAllAction does
					// NOT highlight the selected text. control-A will. I'm 
					// not sure why this occurs. We will override the default
					// behavior here.
					//
					textComponent.getCaret().setSelectionVisible(true);
				}
			}
			else
			{
				if(action.equals(SAVEAS_ACTION_CMD))
				{
					saveContents();
				}
			}
		}// actionPerformed()
	}// nested class JTextComponentActionListener
	
	
	/**
	*	Saves the contents of the Dialog. This saves as HTML if we are
	*	text/HTML, otherwise, it saves as a .txt file.
	*/
	protected void saveContents()
	{
		File file = null;
		if(textPane.getContentType().equals("text/html"))
		{
			file = getFileName(SimpleFileFilter.HTML_FILTER);
		}
		else
		{
			file = getFileName(SimpleFileFilter.TXT_FILTER);
		}
		
		if(file != null)
		{
			FileWriter fw = null;
			
			try
			{
				StringWriter sw = new StringWriter();
				textPane.write(sw);
				String output = inlineStyleSheet(sw.toString());
				
				fw = new FileWriter(file);
				fw.write(output);
			}
			catch(IOException e)
			{
				ErrorDialog.displayFileIO((JFrame) getParent(), e, file.toString());
			}
			finally
			{
				if(fw != null)
				{
					try { fw.close(); } catch(IOException ioe) {}
				}
			}
		}
	}// saveContents()
	
	
	/** Insert (inline) the CSS style sheet (if any) */
	private String inlineStyleSheet(String text)
	{
		if(!textPane.getContentType().equals("text/html"))
		{
			return text;
		}
		
		// setup a regex; our capture group is the css link HREF
		//
		Pattern link = Pattern.compile("(?i)<link\\s+rel=\"stylesheet\"\\s+href=\"([^\"]+)\">");
		Matcher m = link.matcher(text);
		if(m.find())
		{
			// load the link line.
			String cssText = Utils.getText(Utils.getResourceBasePrefix()+m.group(1));
			
			if(cssText != null)
			{
				StringBuffer sb = new StringBuffer(text.length() + 4096);
				sb.append(text.substring(0, m.start()));
				sb.append("<style type=\"text/css\" media=\"screen\">\n\t<!--\n");
				sb.append(cssText);
				sb.append("\n\t-->\n</style>");
				sb.append(text.substring(m.end()));
				return sb.toString();
			}
		}
		
		return text;
	}// inlineStyleSheet()
	
	
	/** 
	*	Popup a file requester; returns the file name, or null if
	*	the requester was cancelled.
	*/
	protected File getFileName(final SimpleFileFilter sff)
	{
		if(sff == null)
		{
			throw new IllegalArgumentException();
		}
		
		// JFileChooser setup 
		XJFileChooser chooser = XJFileChooser.getXJFileChooser();
		chooser.addFileFilter(sff);
		chooser.setFileFilter(sff);
		
		// set default save-game path
		chooser.setCurrentDirectory( GeneralPreferencePanel.getDefaultGameDir() );
		
		// show dialog
		File file = chooser.displaySaveAs(getParent());
		XJFileChooser.dispose();
		
		return file;
	}// getFileName()
	
	
	/**
	*	Creates the JToolbar (if a command bar is enabled). 
	*	Sets up the main listener. Calls
	*	setupCommandBar() to add components.
	*/
	protected JToolBar createCommandBar()
	{
		// toolbar
		JToolBar jtb = new JToolBar();
		jtb.setMargin(new Insets(5,5,5,5));
		jtb.setFloatable(false);
		jtb.setRollover(true);
		//jtb.setBorder(new EtchedBorder(EtchedBorder.LOWERED)); 
		
		// listeners
		final JTextComponentActionListener actionListener = new JTextComponentActionListener(textPane);
		
		// add default components
		setupCommandBar(jtb, actionListener);
		
		return jtb;
	}// createCommandBar()
	
	/**
	*	Adds buttons to the command bar. This is not called if no command bar is
	*	enabled for the dialog.
	*/
	protected void setupCommandBar(JToolBar jtb, JTextComponentActionListener listener)
	{
		// file
		JButton b = new JButton(Utils.getLocalString(TEXT_SAVEAS), Utils.getIcon(ICON_SAVEAS));
		b.setToolTipText(Utils.getLocalString(TEXT_SAVEAS));
		b.setActionCommand(SAVEAS_ACTION_CMD);
		b.addActionListener(listener);
		jtb.add(b);
		
		// cut/copy/paste (in that order)
		jtb.addSeparator();
		
		bCut = new JButton(Utils.getIcon(ICON_CUT));
		bCut.setToolTipText(Utils.getLocalString(TEXT_CUT));
		bCut.setActionCommand(DefaultEditorKit.cutAction);
		bCut.addActionListener(listener);
		jtb.add(bCut);                   
		
		b = new JButton(Utils.getIcon(ICON_COPY));
		b.setToolTipText(Utils.getLocalString(TEXT_COPY));
		b.setActionCommand(DefaultEditorKit.copyAction);
		b.addActionListener(listener);
		jtb.add(b);                   
		
		bPaste = new JButton(Utils.getIcon(ICON_PASTE));
		bPaste.setToolTipText(Utils.getLocalString(TEXT_PASTE));
		bPaste.setActionCommand(DefaultEditorKit.pasteAction);
		bPaste.addActionListener(listener);
		jtb.add(bPaste);
		
		// misc
		jtb.addSeparator();
		
		b = new JButton(Utils.getLocalString(TEXT_SELECTALL));
		b.setToolTipText(Utils.getLocalString(TEXT_SELECTALL));
		b.setActionCommand(DefaultEditorKit.selectAllAction);
		b.addActionListener(listener);
		jtb.add(b);
	}// setupCommandBar()
	
}// class TextViewer

