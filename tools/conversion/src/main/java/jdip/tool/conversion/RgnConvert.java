//
//  @(#)RgnConvert.java 	2004
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
package jdip.tool.conversion;

/**
	
	VERY early rp-rgn file to svg converter.
	
*/
public class RgnConvert
{
	
	/*
	public static void main(String[] args)
	{
		
		new RgnConvert(new File(args[0]), new File("test.svg"));
		
	}// main()
	*/
	/*
	
	
	public RgnConvert(File in, File out)
	{
		
		FileLine[] lines = null;
		
		
		// read all lines.
		LineNumberReader lnr = null;
		try
		{
			ArrayList tmpLines = new LinkedList();
			lnr = new LineNumberReader(new BufferedReader(new FileReader(input)));
			String line = lnr.readLine();
			int lineNum = lnr.getLineNumber();
			
			while(line != null)
			{
				FileLine fl = FileLine.makeFileLine(line, lineNum);
				if(fl != null)
				{
					tmpLines.add(fl);
				}
				
				line = lnr.readLine();
				lineNum = lnr.getLineNumber();
			}
			
			lines = (FileLine[]) tmpLines.toArray(new FileLine[tmpLines.size()]);
			System.out.println("Read "+lineNum+" lines from "+input);
		}
		finally
		{
			if(lnr != null)
			{
				lnr.close();
			}
		}
		
	}// RgnConvert()
	*/
	
/*

				Unit position <x, y> location of unit in this space
				Name position <x, y> location of name label in this space
				Region Data - description of region in the following format:
					<# scanlines> number of horizontal scanlines in region
					<x1> <y1> <length1> scanline 1
					<x2> <y2> <length2> scanline 2
					<xn> <yn> <lengthn> scanline n 


*/
/*
	private class Polygon
	{
		LinkedList points;
		Point unitPos;
		Point textPos;
		
		protected Polygon()
		{
			points = new LinkedList();
		}// Polygon()
		
		//Convert RLE-encoded lines ("rasters") into a polygon.
		
		public static void parsePolyPoints(FileLine[] lines, int start, int length)
		throws IOException
		{
			if(start+length >= lines.length || start < 0 || length <= 0)
			{
				throw new IllegalArgumentException();
			}
			
			for(int i=start; i<(start+length); i++)
			{
				final FileLine fl = lines[i];
				final int rle[] = new int[3];
				
				
				// parse the 3 points
				// (inefficiently, but easily)
				final StringTokenizer st = new StringTokenizer(fl.getLine(), " ");
				String tok = null;	// for error info
				try
				{
					for(int i=0; i<3; i++)
					{
						if(st.hasMoreTokens())
						{
							tok = st.nextToken();
							rle[i] = Integer.parseInt(tok);
						}
						else
						{
							fl.makeError("3 integers are required: <x> <y> <length>");
						}
					}
				}
				catch(NumberFormatException e)
				{
					fl.makeError("Value \""+tok+"\" is not a valid Integer");
				}
				
				// create the two points.
				Point left 	= new Point();
				Point right = new Point();
				
			}
			
			
		}// parsePoly()
	}// class Polygon
	
	*/
	/*
	
	Algorithm:
	
	1) each raster becomes 2 points
	2) Create a List of points
	3) pick point with SMALLEST y value (highest)
		(e.g., first 2 line in file?)
	4) find points on line with next smallest value (y+1)
	5) connect current point to NEAREST point on next line.
	6) remove that point from list
	
	HOWEVER, we will have to work from top->down for ALL points at a given level.
	
	THIS ALGORITHM will not work.
	
	
	NEW algorithm:
	
	1) create raster image (matrix) from RLE image
		(normalized); upper-left = 0,0
		
		ONLY include EDGE points. If drawn, this should be a HOLLOW shape.
		
	2) pick an edge point
		width/2 : height/2 until first point reached
		
	3) 'keep going' :
		look for nearest point that is adjacent to an edge.
		MAINTAIN a list of points -- so we can't go back. 
			search radius of 1 pixel.
		thus, we search the nearest 8 pixels in the same order.
		but if a pixel was already added, we don't use it.
		
		there SHOULD only be a single neighboring pixel.
		
		add this pixel to the list.
		
		if there is more than 1 nearest pixel:
			a) add the first one
		if there are NO nearest pixels:
			check that we aren't adjacent to START pixel (if so,
			we're done)
		otherwise:
			backtrack a pixel, and add previous pixel to 'bad' list
			and remove from 'used' list.
			
	4) we then have an ORDERED LIST of points to create a polygon.
	
	5) assume all points connected by lines
	
	6) Straight-line simplifier:
		get delta of (b,a); if delta of (c,b) is same, delete point b.
		e.g.:
			b,a: delta 'x'
			c,b	 : x
			d,c	 : x
			e,d  : delta 'x'
			f,e	 : delta 'x2'
		thus:
			points b, c, d eliminated
			line drawn from a to e
		
	later:
	7) Smoother
		uses larger deltas. If too similar, assume same line.
		
	8) de-bumper
		if too close (distance) to two other points, eliminate
		
	
	
	
	
	
	
	
	
	
	
	
	
	*/
	
}// class RgnConvert
