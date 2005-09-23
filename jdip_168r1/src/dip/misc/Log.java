//
//  @(#)Log.java	1.00	4/1/2002
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
package dip.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
*	
*	A very simple logging class that logs all data to stdout. Note that this
*	was implemented for speed and simplicity, rather than using the J2SDK 
*	intrinsic Logging class.
*	<p>
*	By matching methods to log callers, we eliminate String or StringBuffer
*	construction and Object.toString() invocations, which provides a significant
*	speedup. 
*	<p>
*	
*/
public final class Log
{
	/** Set logging to NONE */
	public static final int LOG_NONE 	= 0;
	
	/** Set logging to memory only */
	public static final int LOG_TO_MEMORY	= 1;
	
	/** Set logging to file only */
	public static final int LOG_TO_FILE 	= 2;
	
	private static final int LOG_BUFFER_SIZE = 150;
	private static int logLevel = LOG_NONE;
	private static boolean isLogging = false; 
	private static BufferedWriter bw = null;
	
	private static String[] buffer = null;
	private static int bufferNext = 0;
	
	/*
		HOW the buffer works
		a) string array of given size
		b) two position (start, next)
			start: start of buffer
			
			next: where next position in buffer is; this is start + 1
				  unless start is at 0; then next == start.
				  if start is at (length-1 '2' for a buffer length of 3)
				  then next = 0;
				  
		this prevents us from shifting the string array around in memory
		e.g.: for a buffer length of 3, using data a-z:
		
		0: a	START (0)
		1: b
		2: c	
		
		at this point our buffer is full; we want to add 'd':
		
		0: d	
		1: b	START (1) 	(start is incremented by 1)
		2: c
		
	
	
	*/
	
	
	/** Private constructor */
	private Log()
	{
	}// Log()
	
	
	/** 
	*	Enables or disables logging; optionally allows logging to a file. 
	*	File may be null; null file with 'LOG_TO_FILE' logs to stdout.
	*
	*/
	public synchronized static void setLogging(int value, File file)
	{
		if(value != LOG_NONE && value != LOG_TO_MEMORY && value != LOG_TO_FILE)
		{
			throw new IllegalArgumentException("Bad setLogging() value: "+value);
		}
		
		if(value == LOG_NONE)
		{
			close();			
			bw = null;
			buffer = null;
			bufferNext = 0;
		}
		else 
		{
			// enable memory buffer
			buffer = new String[LOG_BUFFER_SIZE];
			bufferNext = 0;
		}
		
		if(value == LOG_TO_FILE)
		{
			if(file == null)
			{
				// log to stdout (no file specified)
				System.out.println("*********** logging started ***********");
				System.out.println((new Date()).toString());
				System.out.println("***************************************");
			}
			else
			{
				// log to file
				try
				{
					bw = new BufferedWriter(new FileWriter(file, true));
					bw.newLine();
					bw.write("*********** logging started ***********");
					bw.newLine();
					bw.write((new Date()).toString());
					bw.newLine();
					bw.write("***************************************");
					bw.newLine();
					bw.flush();
				}
				catch(IOException e)
				{
					System.err.println(e);
				}
			}
		}
		
		logLevel = value;
		isLogging = (logLevel != LOG_NONE);
	}// setLogging()
	
	
	/** Enables logging to file. Null file logs to stdout.*/
	public static void setLogging(File file)
	{
		setLogging(LOG_TO_FILE, file);
	}// setLogging()
	
	
	/** Check if logging is enabled or disabled */
	public static boolean isLogging()
	{
		return isLogging;
	}// isLogging()
	
	
	/**
	*	Flushes and closes the log file (if writing to stdout, this has no effect)
	*/
	public synchronized static void close()
	{
		if(bw != null)
		{
			try
			{
				bw.flush();
			}
			catch(IOException e)
			{
				System.err.println(e);
			}
			finally
			{
				try { bw.close(); } catch(IOException e2) {}
			}
		}
	}// close()
	
	/** 
	*	Print the given Object to the output file / stdout 
	*	via the Object's toString() method. 
	*/
	public static void print(Object s)
	{
		if(isLogging)
		{
			synchronized(Log.class)
			{
				final String str = s.toString();
				memLog(str);
				if(logLevel == LOG_TO_FILE)
				{
					if(bw == null)
					{
						System.out.print(s);
					}
					else
					{
						try
						{
							bw.write(str);
							bw.flush();
						}
						catch(IOException e)
						{
							System.err.print(e);
						}
					}
				}
			}
		}
	}// print()
	
	
	/** 
	*	Print the given Object to the output file / stdout 
	*	via the Object's toString() method. Follows with a 
	*	newline.
	*/
	public static void println(Object s)
	{
		if(isLogging)
		{
			synchronized(Log.class)
			{
				final String str = s.toString();
				memLog(str);
				if(logLevel == LOG_TO_FILE)
				{
					if(bw == null)
					{
						System.out.println(s);
					}
					else
					{
						try
						{
							bw.write(str);
							bw.newLine();
							bw.flush();
						}
						catch(IOException e)
						{
							System.err.println(e);
						}
					}
				}
			}
		}
	}// println()
	
	
	/** Print text followed by a boolean */
	public static void println(Object s0, boolean b)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(b);
			println(sb);
		}
	}// println()
	
	/** Print text followed by an array; comma-seperated array print; can be null */
	public static void println(final Object s0, final Object[] arr)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			if(arr == null)
			{
				sb.append("null");
			}
			else
			{
				sb.append('[');
				for(int i=0; i<arr.length; i++)
				{
					sb.append(arr[i]);
					if(i<arr.length - 1)
					{
						sb.append(',');
					}
				}
				sb.append(']');
			}
			println(sb);
		}
	}// println()
	
	/** Print text followed by an int */
	public static void println(Object s0, int i0)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(i0);
			println(sb);
		}
	}// println()
	
	/** Print text followed timing delta and current time. */
	public static void printTimed(long lastTime, Object s0)
	{
		if(isLogging)
		{
			long now = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(' ');
			sb.append((now - lastTime));
			sb.append(" ms [delta]; current: ");
			sb.append(now);
			println(sb);
		}
	}// println()
	
	/** Print the delta from the given time. Return the new time. */
	public static long printDelta(long lastTime, Object s0)
	{
		if(isLogging)
		{
			final long now = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer(128);
			sb.append(s0);
			sb.append(' ');
			sb.append((now - lastTime));
			sb.append(" ms [delta]");
			sb.append(now);
			println(sb);
			return now;
		}
		
		return 0L;
	}// printDelta()
	
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			println(sb);
		}
	}// println()
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1, Object s2)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			sb.append(s2);
			println(sb);
		}
	}// println()
	
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1, Object s2, Object s3)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			sb.append(s2);
			sb.append(s3);
			println(sb);
		}
	}// println()
	
	
	
	/** Add to the memory buffer. Unsynchronized! */
	private static void memLog(String s)
	{
		buffer[bufferNext] = s;
		bufferNext++;
		bufferNext = (bufferNext >= buffer.length) ? 0 : bufferNext;
	}// memLog()
	
	
	/** Returns null if no memory buffer. */
	public static synchronized String getMemoryBuffer()
	{
		if(buffer == null)
		{
			return null;
		}
		
		StringBuffer sb = new StringBuffer(8192);
		
		// print out buffer contents, starting from 'bufferNext' to end of 
		// array, then continuing from beginning to 'bufferNext -1'.
		// if we hit a null entry, stop. (buffer not full)
		for(int i=bufferNext; i<buffer.length; i++)
		{
			final String s = buffer[i];
			if(s == null)
			{
				break;
			}
			
			sb.append(s);
			sb.append('\n');
		}
		
		for(int i=0; i<bufferNext; i++)
		{
			final String s = buffer[i];
			if(s == null)
			{
				break;
			}
			
			sb.append(s);
			sb.append('\n');
		}
		
		return sb.toString();
	}// getMemoryBuffer()
	
}// class Log


