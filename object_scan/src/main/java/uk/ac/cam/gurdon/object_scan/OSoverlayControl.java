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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.html.HTMLEditorKit;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class OSoverlayControl implements ActionListener{
int clickX,clickY;
public int Xloc,Yloc;
double cX,cY,cC,cZ,cT,oX,oY,oC,oZ,oT;
double dist,closeDist,rad,vol;
public int closeIndex;
int yShift = 5;
int xShift = 2;
String unit;
boolean doReport = true;
boolean doHead = true;
String locate = "Bottom";
public JFrame control;
public JFrame reporter;
ImagePlus imp;
OSbox box;
Overlay ol;
Overlay origlay;
Color[] colours1,colours2;
Color[] origColours;
boolean filled,label,track;
int txtStart = -1;
int txtEnd = -1;
BasicStroke outline;
MouseListener mickeyMouse;
ComponentListener stick;
String info = "";
JEditorPane htmlPane;
private Color highlightColour;// = Color.MAGENTA;
private final Color hidecol = new Color(0,0,0,0);
private Roi[] orig;
final Image cursorImg = Toolkit.getDefaultToolkit().getImage(getClass().getResource("spy.png"));
final Cursor spyCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(16, 16),"Spy");


public boolean overRoi(MouseEvent me){
if(doReport==true){
	int NF = imp.getNFrames();
        int NS = imp.getNSlices();
        int NC = imp.getNChannels();

        int meX = imp.getCanvas().getCursorLoc().x;	int meY = imp.getCanvas().getCursorLoc().y;
        int meZ = (NS>1)?imp.getZ():0;
        int meT = (NF>1)?imp.getT():0;
        int meC = (NC>1)?imp.getC():0;

        for(int i=0;i<txtStart;i++){
        int olZ = (NS>1)?ol.get(i).getZPosition():0;
        int olT = (NF>1)?ol.get(i).getTPosition():0;
        int olC = (NC>1)?ol.get(i).getCPosition():0;
        if((NF>1)&&(NS==1)&&(NC==1)){olT = ol.get(i).getPosition();}	//Z vs T bug workaround
        if((NF==1)&&(NS>1)&&(NC==1)){olZ = ol.get(i).getPosition();}
			if((ol.get(i).contains(meX,meY))&&(olZ==meZ)&&(olT==meT)&&(olC==meC)){
        		TextRoi tr = (TextRoi)ol.get(i+txtStart);	//get corresponding label for the mouseOver roi	
			String trString = tr.getText().replaceAll("[^0-9]","");	//sanitise roi text
			for(int a=0;a<box.objects.getCounter();a++){
        			if((int)box.objects.getValue("Object",a)==Integer.valueOf(trString)){
        			closeIndex = a;
        			return true;
        			}
        		}
        		}
        }
}
return false;
}

public void resetOverlay(){
if(reporter!=null){reporter.dispose();}
if(imp.getOverlay()==null){return;}
if(imp.getOverlay().size()==0){return;}
info = ""; //reset reporter html string
for(int i=txtStart;i<txtEnd;i++){
	TextRoi thisRoi = (TextRoi)imp.getOverlay().get(i);	
	if(thisRoi.getCurrentFont()==OSbox.biggerfont){
	thisRoi.setCurrentFont(OSbox.bigfont);
	if(label==false){thisRoi.setStrokeColor(hidecol);}
	else if(filled==true){thisRoi.setStrokeColor(origColours[i].darker());}
	else{thisRoi.setStrokeColor(origColours[i]);}
	thisRoi.setLocation(thisRoi.getBounds().x+xShift,thisRoi.getBounds().y+yShift);
	}
}
imp.updateAndDraw();
}

public void locateReporter(){
if((reporter!=null)&&(doReport==true)){
if(locate=="Float"){
	Xloc = MouseInfo.getPointerInfo().getLocation().x-(reporter.getWidth()/2);
	Yloc = MouseInfo.getPointerInfo().getLocation().y+20;
}
else if(locate=="tempfloat"){
	Xloc = imp.getWindow().getBounds().x+(imp.getWindow().getBounds().width/2)-(reporter.getWidth()/2);
	Yloc = imp.getWindow().getBounds().y+imp.getWindow().getBounds().height;
	locate="Float";
}
else if(locate=="Top"){
	Xloc = imp.getWindow().getBounds().x+(imp.getWindow().getBounds().width/2)-(reporter.getWidth()/2);
	Yloc = imp.getWindow().getBounds().y-reporter.getHeight();
}
else if(locate=="Bottom"){
	Xloc = imp.getWindow().getBounds().x+(imp.getWindow().getBounds().width/2)-(reporter.getWidth()/2);
	Yloc = imp.getWindow().getBounds().y+imp.getWindow().getBounds().height;
}
else if(locate=="Left"){
	Xloc = imp.getWindow().getBounds().x-reporter.getWidth();
	Yloc = imp.getWindow().getBounds().y+(imp.getWindow().getBounds().height/2)-(reporter.getHeight()/2);
}
else if(locate=="Right"){
	Xloc = imp.getWindow().getBounds().x+imp.getWindow().getBounds().width;
	Yloc = imp.getWindow().getBounds().y+(imp.getWindow().getBounds().height/2)-(reporter.getHeight()/2);
}
reporter.setLocation(Xloc,Yloc);
reporter.toFront();
}
}

public void report(ResultsTable objects,int index,boolean shift){	
try{
if(reporter!=null){reporter.dispose();}
if(doReport==true){
reporter = new JFrame();
reporter.setUndecorated(true);
reporter.getContentPane().setBackground(Color.WHITE);
reporter.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
if(shift==false){info="";}	//reset info on click but not shift-click
if(info==""){doHead=true;}
htmlPane = new JEditorPane("text/html","");
//construct html 3.2 from objects table
String[] head = box.objects.getColumnHeadings().split("\t");
String[] numb = box.objects.getRowAsString(index).split("\t");
if((locate=="Top")||(locate=="Bottom")||(locate=="Float")){
	if(doHead==true){info = info+"<tr>";}
	for(int i=0;i<head.length;i++){
		String ins = (doHead==true)?"<td><b>"+head[i]+"</b></td>":"";
		info = info+ins;
	}
	if(doHead==true){info = info+"</tr>";}
	info = info+"<tr>";
	for(int i=0;i<head.length;i++){
		info = info+"<td>"+numb[i]+"</td>";
	}
	info = info+"</tr>";
doHead=false;
}
else if((locate=="Right")||(locate=="Left")){
	for(int i=0;i<head.length;i++){
		if(doHead==true){
		String ins = "<tr><td><b>"+head[i]+"</b></td>";
		info = info+ins+"<td>"+numb[i]+"</td £#£>";
		info=info+"</tr>";
		}
		else{
		info = info.replaceFirst("</td £#£></tr>","</td><td>"+numb[i]+"</td ~#~></tr>");
		}
	}
	info = info.replaceAll("~#~","£#£");
doHead=false;
}
info = info.replaceAll("\\.0+<","<");	//remove trailing 0s
HTMLEditorKit kit = new HTMLEditorKit();
kit.getStyleSheet().addRule("table{background-color:white;color:black;font-size:10px;font-family:Consolas;border-collapse:collapse;"+
"border-style:solid;border-width:0px;border-color:black;border-spacing:0px;}");
kit.getStyleSheet().addRule("td{border-style:solid;border-width:1px;border-color:black;text-align:center;padding:4px;}");
htmlPane.setEditorKit(kit); //textPane.setDocument(kit.createDefaultDocument());
htmlPane.setText("<table>"+info+"</table>");
htmlPane.setEditable(false);
box.makeDraggable(htmlPane,reporter);
JPanel buttonPanel = new JPanel(new GridLayout(2,1,2,2));
buttonPanel.setBackground(Color.WHITE);
buttonPanel.add(buttoner("Close","reporterclose"));
buttonPanel.add(buttoner("Copy","Copy"));
reporter.add(htmlPane);
reporter.add(buttonPanel);
reporter.pack();
reporter.setSize(new Dimension(htmlPane.getBounds().width+buttonPanel.getBounds().width+8,htmlPane.getBounds().height));
box.makeDraggable(reporter,reporter);
locateReporter();
reporter.setVisible(true);
String obj = IJ.d2s(box.objects.getValue("Object",index),0);
for(int i=txtStart;i<txtEnd;i++){
	TextRoi thisRoi = (TextRoi)imp.getOverlay().get(i);
	Color HC = thisRoi.getStrokeColor();
	highlightColour = new Color(Math.abs(255-HC.getRed()/2),Math.abs(255-HC.getGreen()/2),Math.abs(255-HC.getBlue()/2));
	if((thisRoi.getText().matches("\u00D7"+obj+"\\n*"))&&(thisRoi.getCurrentFont().getSize()==12)){
		thisRoi.setCurrentFont(OSbox.biggerfont);
		thisRoi.setStrokeColor(highlightColour);
		thisRoi.setLocation(thisRoi.getBounds().x-xShift,thisRoi.getBounds().y-yShift);
	}
	else if((shift==false)&&(!thisRoi.getText().matches("\u00D7"+obj+"\\n*"))&&(thisRoi.getCurrentFont()==OSbox.biggerfont)){	//check font to see if roi is currently highlighted
	thisRoi.setCurrentFont(OSbox.bigfont);
	if(label==false){thisRoi.setStrokeColor(hidecol);}
	else if(filled==true){thisRoi.setStrokeColor(origColours[i].darker());}
	else{thisRoi.setStrokeColor(origColours[i]);}
	thisRoi.setLocation(thisRoi.getBounds().x+xShift,thisRoi.getBounds().y+yShift);
	}
}
imp.updateAndDraw();
}//doReport==true
}catch(Exception e){IJ.log(e.getStackTrace()[0].toString()+System.getProperty("line.separator")+e.toString());}
}

private JButton buttoner(String label,String cmd){
JButton button = new JButton(label);
button.setActionCommand(cmd);
button.setFont(OSbox.bigfont);
button.setBackground(OSbox.buttoncol1);
button.addActionListener(this);
return button;
}
private JRadioButton RB(String name, boolean state){
JRadioButton rb = new JRadioButton(name, state);
rb.setBackground(OSbox.backcol);rb.setForeground(OSbox.frontcol);
rb.addActionListener(this);
return rb;
}

private boolean OSarrangement(Overlay ol){
if(ol.size()==0){return false;}
if((ol.get(0).getType()!=0)&&(ol.get(ol.size()-1).getType()==0)){ //if 1st roi isn't a rectangle and the last is
return true;
}
else{return false;}
}

public void show(ImagePlus passimp,Overlay passol,Color[] passcolours1,Color[] passcolours2,OSbox passbox){
imp = passimp;
ol = passol;
ol.drawLabels(false);ol.drawNames(false);
imp.setOverlay(ol);
colours1 = passcolours1;
colours2 = passcolours2;
box = passbox;
if(box.objects==null){
	Frame[] winlist = WindowManager.getNonImageWindows();
	for(int w=0;w<winlist.length;w++){
		if((winlist[w].getTitle().matches("^Object Scan: objects in.*"))||(winlist[w].getTitle().matches(".*[Rr]esults.*"))){
		box.objects=((TextWindow)winlist[w]).getTextPanel().getResultsTable();
			if(box.objects.getCounter()!=0){	//stop if a table containing results is found
				box.objects.showRowNumbers(false);
				break;
			}	
		}
	}
}
if(box.outlineset=="Thin"){outline = OSbox.thinStroke;}
else if(box.outlineset=="Thick"){outline = OSbox.thickStroke;}
else if(box.outlineset=="Dotted"){outline = OSbox.dotStroke;}
else if(box.outlineset=="Dashed"){outline = OSbox.dashStroke;}
if(box.analysis=="Track"){track=true;}
origColours = new Color[ol.size()];
for(int i=0;i<ol.size();i++){
origColours[i] = ol.get(i).getStrokeColor();
}
orig = ol.toArray();
filled = box.fill;
label = true;
unit = imp.getCalibration().getUnit();
if ((unit=="micron")||(unit=="microns")||(unit.matches(".*icro.*"))){unit="\u00B5m";}
if(OSarrangement(ol)==true){
txtStart = ol.size()/2;
txtEnd = ol.size();
}
else{
txtStart = ol.size();
txtEnd = ol.size();
}

mickeyMouse = new MouseAdapter(){
        public void mousePressed(MouseEvent me){
if(doReport==true){
		if(overRoi(me)==true){
        	report(box.objects,closeIndex,me.isShiftDown());
        	}
        me.consume();
        IJ.run(imp, "Select None", "");
        }
}
public void mouseReleased(MouseEvent me){
if(doReport==true){
        me.consume();
        IJ.run(imp, "Select None", "");
}
}
public void mouseMoved(MouseEvent me){
if(doReport==true){
	if(overRoi(me)==true){
	imp.getCanvas().setCursor(spyCursor);
        }
me.consume();
}
}
	};//mickeyMouse
imp.getCanvas().addMouseListener(mickeyMouse);
imp.getCanvas().addMouseMotionListener((MouseMotionListener)mickeyMouse);

stick = new ComponentAdapter(){
public void componentMoved(ComponentEvent ce){
if(locate=="Float"){locate="tempfloat";}
locateReporter();
}
public void componentResized(ComponentEvent ce){
if(locate=="Float"){locate="tempfloat";}
locateReporter();
}
};
imp.getWindow().addComponentListener(stick);

try{
control = new JFrame();
imp.getWindow().toFront();
Rectangle br = imp.getWindow().getBounds();
control.setLocation(br.x+br.width,br.y);
control.getContentPane().setBackground(OSbox.backcol);
control.setLayout(new BoxLayout(control.getContentPane(),BoxLayout.Y_AXIS));
control.setUndecorated(true);
JPanel topPanel = new JPanel();
topPanel.setLayout(new BorderLayout());
topPanel.setBackground(OSbox.backcol);
JLabel head1 = new JLabel("Object Scan Overlay",JLabel.CENTER);
head1.setFont(OSbox.medfont);
control.add(head1);
JLabel head2 = new JLabel("<html><p style='width:150px'>"+imp.getTitle()+"</p></html>",JLabel.CENTER);
head2.setFont(OSbox.smallfont);
topPanel.add(BorderLayout.NORTH,head1);
topPanel.add(BorderLayout.CENTER,head2);
control.add(topPanel);
JPanel all = new JPanel(new GridLayout(1,2,2,2));
	all.setPreferredSize(new Dimension(100,40));
	all.setBackground(OSbox.backcol);
	all.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),"Overlay",0,0,OSbox.bigfont,OSbox.frontcol));
	JRadioButton RBall1 = RB("On", true);
	JRadioButton RBall2 = RB("Off", false);
	RBall1.setActionCommand("olon");RBall2.setActionCommand("oloff");
	ButtonGroup RBallgroup = new ButtonGroup();
	RBallgroup.add(RBall1);RBallgroup.add(RBall2);
	all.add(RBall1);all.add(RBall2);
	box.makeDraggable(all,control);
	control.add(all);
JPanel lab = new JPanel(new GridLayout(1,2,2,2));
	lab.setPreferredSize(new Dimension(160,40));
	lab.setBackground(OSbox.backcol);
	lab.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Labels",0,0,OSbox.bigfont,OSbox.frontcol));
	JRadioButton RBlab1 = RB("Hide", false);
	JRadioButton RBlab2 = RB("Show", true);
	ButtonGroup RBlabgroup = new ButtonGroup();
	RBlabgroup.add(RBlab1);RBlabgroup.add(RBlab2);
	lab.add(RBlab1);lab.add(RBlab2);
	box.makeDraggable(lab,control);
	control.add(lab);
JPanel lin = new JPanel(new GridLayout(2,3,2,2));
	lin.setPreferredSize(new Dimension(220,80));
	lin.setBackground(OSbox.backcol);
	lin.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Shapes",0,0,OSbox.bigfont,OSbox.frontcol));
	JRadioButton RBlin1 = RB("None", box.outlineset=="None");
	JRadioButton RBlin2 = RB("Thin", box.outlineset=="Thin");
	JRadioButton RBlin3 = RB("Thick", box.outlineset=="Thick");
	JRadioButton RBlin4 = RB("Dashed", box.outlineset=="Dashed");
	JRadioButton RBlin5 = RB("Dotted", box.outlineset=="Dotted");	
	JRadioButton RBlin6 = RB("Filled", box.fill);
	ButtonGroup RBlingroup = new ButtonGroup();
	RBlingroup.add(RBlin1);RBlingroup.add(RBlin2);RBlingroup.add(RBlin3);RBlingroup.add(RBlin4);RBlingroup.add(RBlin5);RBlingroup.add(RBlin6);
	lin.add(RBlin1);lin.add(RBlin2);lin.add(RBlin3);lin.add(RBlin4);lin.add(RBlin5);lin.add(RBlin6);
	box.makeDraggable(lin,control);
	control.add(lin);
JPanel rep = new JPanel(new GridLayout(2,3,2,2));
	rep.setPreferredSize(new Dimension(220,80));
	rep.setBackground(OSbox.backcol);
	rep.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),"Object Spy",0,0,OSbox.bigfont,OSbox.frontcol));
	JRadioButton RBrep1 = RB("Off", locate=="Off");
	JRadioButton RBrep2 = RB("Top", locate=="Top");
	JRadioButton RBrep3 = RB("Bottom", locate=="Bottom");
	JRadioButton RBrep4 = RB("Float", locate=="Float");
	JRadioButton RBrep5 = RB("Left", locate=="Left");
	JRadioButton RBrep6 = RB("Right", locate=="Right");
	ButtonGroup RBrepgroup = new ButtonGroup();
	RBrepgroup.add(RBrep1);RBrepgroup.add(RBrep2);RBrepgroup.add(RBrep3);RBrepgroup.add(RBrep4);RBrepgroup.add(RBrep5);RBrepgroup.add(RBrep6);
	rep.add(RBrep1);rep.add(RBrep2);rep.add(RBrep3);rep.add(RBrep4);rep.add(RBrep5);rep.add(RBrep6);
	box.makeDraggable(rep,control);
	control.add(rep);
JPanel buttons = new JPanel();
buttons.setBackground(OSbox.backcol);
buttons.add(buttoner("Close","mainclose"));
buttons.add(buttoner("3D View","3D View"));
control.add(buttons);
box.makeDraggable(buttons,control);
box.makeDraggable(control,control);
control.pack();
control.setVisible(true);
}catch(Exception e){IJ.log(e.toString()+"\n~~~~~\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
}

public void actionPerformed(ActionEvent event){
try{
if (event.getActionCommand().equals("mainclose")){
if((imp.getCanvas()!=null)&&(mickeyMouse!=null)){
imp.getCanvas().removeMouseListener(mickeyMouse);
imp.getCanvas().removeMouseMotionListener((MouseMotionListener)mickeyMouse);
}
resetOverlay();
control.dispose();
}
if (event.getActionCommand().equals("reporterclose")){
resetOverlay();
}
if (event.getActionCommand().equals("Copy")){
htmlPane.selectAll();
htmlPane.copy();
}
if (event.getActionCommand().equals("3D View")){
new OS3d().show(imp,box,colours1,colours2);
}
if (event.getActionCommand().equals("oloff")){imp.setHideOverlay(true);}
if (event.getActionCommand().equals("olon")){imp.setHideOverlay(false);}
if (event.getActionCommand().equals("Hide")){	//labels
	for(int i=txtStart;i<txtEnd;i++){
	ol.get(i).setStrokeColor(hidecol);
	}
	imp.updateAndDraw();
	label = false;
}
if (event.getActionCommand().equals("Show")){	//labels
	for(int i=txtStart;i<txtEnd;i++){
	if(filled==true){ol.get(i).setStrokeColor(origColours[i].darker());}
	else{ol.get(i).setStrokeColor(origColours[i]);}
	}
	imp.updateAndDraw();
	label = true;
}
if (event.getActionCommand().equals("None")){	//shapes
	for(int i=0;i<txtStart;i++){
	ol.get(i).setStrokeColor(hidecol);
	ol.get(i).setFillColor(hidecol);
	}
	if((filled==true)&&(label==true)){
		for(int i=txtStart;i<txtEnd;i++){
		ol.get(i).setStrokeColor(origColours[i]);
		}
	}
	imp.updateAndDraw();
	filled = false;
}
if ((event.getActionCommand().equals("Thin"))||(event.getActionCommand().equals("Thick"))||(event.getActionCommand().equals("Dotted"))||(event.getActionCommand().equals("Dashed"))){	//shapes
	filled = false;
	if (event.getActionCommand().equals("Thin")){outline=OSbox.thinStroke;}
	else if (event.getActionCommand().equals("Thick")){outline=OSbox.thickStroke;}
	else if (event.getActionCommand().equals("Dotted")){outline=OSbox.dotStroke;}
	else if (event.getActionCommand().equals("Dashed")){outline=OSbox.dashStroke;}
	
	IJ.run("Overlay Options...", "fill=none apply set");
	for(int i=0;i<txtStart;i++){
	ol.get(i).setStrokeColor(origColours[i]);
	ol.get(i).setStroke(outline);
	}
	if((label==true)){
		resetOverlay();
	}
	else if(label==false){
		for(int i=txtStart;i<txtEnd;i++){
		ol.get(i).setStrokeColor(hidecol);
		}
	}
	if(track==true){  		 //TODO: fix track tails
		for(int i=0;i<ol.size();i++){
		ol.get(i).setStrokeWidth(orig[i].getStrokeWidth());
		ol.get(i).setStrokeColor(orig[i].getStrokeColor());
		ol.get(i).setFillColor(orig[i].getFillColor());
		}
	}
	imp.updateAndDraw();
}
if (event.getActionCommand().equals("Filled")){	//shapes
	for(int i=0;i<txtStart;i++){
	ol.get(i).setStrokeColor(hidecol);
	ol.get(i).setFillColor(origColours[i]);
	}
	if(label==true){
		for(int i=txtStart;i<txtEnd;i++){
		TextRoi thisRoi = (TextRoi)imp.getOverlay().get(i);
		thisRoi.setStrokeColor(origColours[i].darker());
		thisRoi.setCurrentFont(OSbox.bigfont);
		}
	}
	imp.updateAndDraw();
	filled = true;
}
if(event.getActionCommand().equals("Off")){
	doReport = false;
	locate = "None";
	resetOverlay();
}
if(event.getActionCommand().equals("Float")){
	doReport = true;
	locate = "Float";
	resetOverlay();
}
if(event.getActionCommand().equals("Top")){
	doReport = true;
	locate = "Top";
	resetOverlay();
}
if(event.getActionCommand().equals("Bottom")){
	doReport = true;
	locate = "Bottom";
	resetOverlay();
}
if(event.getActionCommand().equals("Left")){
	doReport = true;
	locate = "Left";
	resetOverlay();
}
if(event.getActionCommand().equals("Right")){
	doReport = true;
	locate = "Right";
	resetOverlay();
}
}catch(Exception e){IJ.log(e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
}

}
