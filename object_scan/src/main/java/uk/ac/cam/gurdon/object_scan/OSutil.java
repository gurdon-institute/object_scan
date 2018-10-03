package uk.ac.cam.gurdon.object_scan;
/*
This file is part of Object Scan - by Richard Butler, Gurdon Institute Imaging Facility, University of Cambridge
Copyright 2013, 2014 Richard Butler

Object Scan is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Object Scan is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Object Scan.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Format;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ij.ImagePlus;
import ij.process.LUT;

public class OSutil{
private int C;
public static final int BIG = 3;
public static final int MED = 2;
public static final int SMALL = 1;
private static final FocusAdapter focus = new FocusAdapter(){public void focusGained(FocusEvent fe){
			final JTextField source = (JTextField)fe.getSource();
			SwingUtilities.invokeLater(new Runnable(){
			public void run(){source.selectAll();}
			});
			}};

public OSutil(){

}

public JButton buttoner(String label,int type,ActionListener al){
JButton button = new JButton(label);
if(type==BIG){
button.setFont(OSbox.bigfont);
button.setBackground(OSbox.buttoncol1);
}
if(type==MED){
button.setFont(OSbox.medfont);
button.setBackground(OSbox.buttoncol1);
}
if(type==SMALL){
button.setFont(OSbox.smallfont);
button.setBackground(OSbox.buttoncol2);
}
button.setForeground(OSbox.frontcol);
button.setFocusPainted(false);
button.addActionListener(al);
return button;
}

public JLabel fieldlabel(String text){
JLabel label = new JLabel(text);
return label;
}

public JFormattedTextField numberfield(Format format,double value,int columns){
	JFormattedTextField field = new JFormattedTextField(format);
	field.setValue(value);
	field.setColumns(columns);
	field.addFocusListener(focus);
	return field;
}

public JPanel paneller(JComponent label,JComponent control){
	JPanel pan = new JPanel();
	pan.setLayout(new FlowLayout(1,4,0));
	pan.setBackground(OSbox.backcol);
	pan.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
	pan.add(label);
	if(control!=null)pan.add(control);
	return pan;
}

public void colourise(ImagePlus imp,OSbox box){
C = imp.getNChannels();
if(box.colourset=="Bold"){
box.colours1 = new Color[]{null,new Color(0,255,255),    new Color(255,0,255),    new Color(255,255,0),    new Color(255,128,0),   new Color(128,128,128), new Color(192,192,192)};
box.colours2 = new Color[]{null,new Color(0,128,128,255),new Color(128,0,128,255),new Color(128,128,0,255),new Color(128,64,0,255),new Color(64,64,64,255),new Color(128,128,128,255)};
}
else if(box.colourset=="Light"){
box.colours1 = new Color[]{null,new Color(128,255,255),   new Color(255,128,255),    new Color(255,255,128),    new Color(255,192,128),  new Color(192,192,192),    new Color(255,255,255)};
box.colours2 = new Color[]{null,new Color(64,200,200,255),new Color(200,100,200,255),new Color(200,200,0,255),new Color(200,140,100,255),new Color(128,128,128,255),new Color(200,200,200,255)};
}
else if(box.colourset=="Dark"){
box.colours1 = new Color[]{null,new Color(0,96,96),    new Color(96,0,96),    new Color(96,96,0),    new Color(96,32,0),   new Color(64,64,64), new Color(96,96,96)};
box.colours2 = new Color[]{null,new Color(0,64,64,255),new Color(64,0,64,255),new Color(64,64,0,255),new Color(64,16,0,255),new Color(32,32,32,255),new Color(64,64,64,255)};
}
else if(box.colourset=="Channels"){
box.colours1 = new Color[C+1];box.colours2 = new Color[C+1];
LUT[] lutarray = imp.getLuts();
if(lutarray==null){lutarray=new LUT[]{LUT.createLutFromColor(new Color(255,255,0))};}
for(int i=1;i<=lutarray.length;i++){
	box.colours1[i]=new Color(lutarray[i-1].getRed(255),lutarray[i-1].getGreen(255),lutarray[i-1].getBlue(255));
	box.colours2[i]=new Color(lutarray[i-1].getRed(128),lutarray[i-1].getGreen(128),lutarray[i-1].getBlue(128));
}
}
else if(box.colourset=="Inverse"){
box.colours1 = new Color[C+1];box.colours2 = new Color[C+1];
LUT[] lutarray = imp.getLuts();
if(lutarray==null){lutarray=new LUT[]{LUT.createLutFromColor(new Color(255,255,0))};}
for(int i=1;i<=lutarray.length;i++){
	box.colours1[i]=new Color(255-lutarray[i-1].getRed(255),255-lutarray[i-1].getGreen(255),255-lutarray[i-1].getBlue(255));
	box.colours2[i]=new Color(255-lutarray[i-1].getRed(128),255-lutarray[i-1].getGreen(128),255-lutarray[i-1].getBlue(128));
}
}

if(box.outlineset=="Thin"){box.outline = OSbox.thinStroke;}
else if(box.outlineset=="Thick"){box.outline = OSbox.thickStroke;}
else if(box.outlineset=="Dotted"){box.outline = OSbox.dotStroke;}
else if(box.outlineset=="Dashed"){box.outline = OSbox.dashStroke;}
}

}
