//
//  @(#)GradientJLabel.java		10/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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

import java.awt.*;
import javax.swing.*;

/** 
*	A gradient-shaded background JLabel. It is 
*	drawn inside a JPanel because it's easier to
*	adjust the rendering that way.
*/
public class GradientJLabel extends JPanel
{
	private static final Color[] TEXT_COLORS = {Color.WHITE, Color.BLACK};
	
	boolean autocolor;
	final JLabel label;
	Color gradColor;
	
	/** Create a GradientJLabel */
	public GradientJLabel(String text)
	{
		this(text, SwingConstants.LEFT, null, null, false);
	}// GradientJLabel()
	
	
	/** Create a GradientXJEditorPane */
	public GradientJLabel(String text, int horizontalAlignment, Color textColor, Color gradientColor)
	{
		this(text, horizontalAlignment, textColor, gradientColor, false);
	}// GradientJLabel()
	
	
	/** 
	*	Create a colored GradientJLabel. The Text color will default to
	*	either black or white, depending upon what is most readable.
	*/
	public GradientJLabel(String text, Color gradientColor)
	{
		this(text, SwingConstants.LEFT, null, gradientColor, true);
	}// GradientJLabel()
	
	
	/** Create a GradientXJEditorPane */
	private GradientJLabel(String text, int horizontalAlignment, Color textColor, Color gradientColor, boolean autoColor)
	{
		super(new FlowLayout(FlowLayout.LEFT, 0, 3));
		
		this.autocolor = autoColor;
		
		label = new JLabel(text, horizontalAlignment)
		{
			public boolean isOpaque()	{ return false; }
			public boolean isFocusable()	{ return false; }
		};
		
		setGradientColor(gradientColor);
		setTextColor(textColor);
		
		setOpaque(false);
		label.setOpaque(false);
		
		add(Box.createHorizontalStrut(5));	// should adjust to font .. perhaps width of "N"?
		add(label);
	}// GradientJLabel()	
	
	
	/** Overrides setText() */
	public void setText(String text)
	{
		label.setText(text);
	}// setText()
	
	/** 
	*	Set gradient color. If null, uses default. Should call repaint() 
	*	to ensure a change occurs.
	*/
	public void setGradientColor(Color color)
	{
		gradColor = ((color == null) ? UIManager.getColor("TextField.highlight") : color);
		
		if(autocolor)
		{
			setTextColor(null);
		}
	}// setGradientColor()
	
	/** 
	*	Set text color. If null, the default is used. 
	*	<p>
	*	Note that if we are 'automatically' selecting the text color,
	*	color values passed will be ignored.
	*/
	public void setTextColor(Color color)
	{
		if(autocolor)
		{
			label.setForeground(GUIUtils.findBestBright(gradColor, TEXT_COLORS));
		}
		else
		{
			if(color != null)
			{
				label.setForeground(color);
			}
			else
			{
				label.setForeground(UIManager.getColor("Label.foreground"));
			}
		}
	}// setTextColor()
	
	/** We are not opaque; we will paint the background. */
	public boolean isOpaque()	{ return false; }
	
	/** We are not focusable. */
	public boolean isFocusable()	{ return false; }
	
	/** Overridden to provide painting functionality. */
	protected void paintComponent(Graphics g)
	{
		final int width = getWidth();
		final int height = getHeight();
		
		Graphics2D g2d = (Graphics2D) g;
		
		// get bgColor
		Color bg = (gradColor == null) ? UIManager.getColor("TextField.highlight") : gradColor;
		
		// save old paint.
		Paint oldPaint = g2d.getPaint();
		// paint the gradient.
		g2d.setPaint(new GradientPaint(0, 0, 
			bg, 
			width, height, 
			UIManager.getColor("Label.background"), false));
		g2d.fillRect(0, 0, width, height);
		
		// restore the original paint
		g2d.setPaint(oldPaint);
		
		// paint foreground
		super.paintComponent(g);
	}// paintComponent()
	
}// nested class GradientJLabel

