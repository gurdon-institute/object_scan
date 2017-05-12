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
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import customnode.CustomLineMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.Duplicator;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;
import view4d.Timeline;

public class OS3d{
	
private int C,Z,T,clickX,clickY;
private String title;
private double tcal;
private GenericDialog op3d;

public MouseAdapter maas(){
	MouseAdapter ma = new MouseAdapter(){
        public void mousePressed(MouseEvent me){  
        clickX = me.getX();clickY= me.getY();
        }
	public void mouseDragged(MouseEvent me){
	op3d.setLocation(op3d.getLocation().x+me.getX()-clickX,op3d.getLocation().y+me.getY()-clickY);
	}
	};
return ma;
}

public void show(ImagePlus imp, OSbox box,Color[] colours1,Color[] colours2)throws Exception{
try{
if(box.objects==null){IJ.error("Error","No objects table found.");return;}
C = imp.getNChannels();
Z = imp.getNSlices();
T = imp.getNFrames();
title = imp.getTitle();
tcal = imp.getCalibration().frameInterval;
op3d = new GenericDialog("3D View Options");
int op3dW = 160;int op3dH = 160;
int op3dXpos = (Toolkit.getDefaultToolkit().getScreenSize().width-op3dW)/2;
int op3dYpos = (Toolkit.getDefaultToolkit().getScreenSize().height-op3dH)/4;
op3d.setBounds(op3dXpos,op3dYpos,op3dW,op3dH);
op3d.setBackground(OSbox.backcol.brighter());
op3d.setUndecorated(true);
op3d.setResizable(false);
op3d.addMessage("Display in 3D Viewer (Schmid et al. BMC Bioinformatics 2010)");
op3d.addCheckbox("High quality rendering (slow for large stacks)",true);
op3d.addCheckbox("Show mapped objects (slow for large numbers of objects)",true);
op3d.addCheckbox("Label objects",true);
//op3d.setCancelLabel("Cancel");
op3d.addMouseListener(maas());op3d.addMouseMotionListener(maas());
op3d.showDialog();
if(op3d.wasCanceled()){return;}
else if(op3d.wasOKed()){
boolean highqual = op3d.getNextBoolean();
boolean showobj = op3d.getNextBoolean();
boolean labobj = op3d.getNextBoolean();

IJ.showStatus("Generating 3D view...");
Image3DUniverse univ = new Image3DUniverse(); 	//create a universe
Content cont = new Content("");
//univ.show();
java.util.List<Point3f> mesh = new ArrayList<Point3f>();
boolean[] true3 = {true,true,true};
for(int volt=1;volt<=C;volt++){
ImagePlus foo = new Duplicator().run(imp,volt,volt,1,Z,1,T);
IJ.run(foo, "8-bit", "");
if(highqual==true){cont = univ.addVoltex(foo,new Color3f(java.awt.Color.WHITE),title+""+volt,1,true3,1);}
else{cont = univ.addVoltex(foo,new Color3f(java.awt.Color.WHITE),title+""+volt,1,true3,4);}
cont.showPointList(showobj);
cont.setLandmarkPointSize((float)box.estd);
cont.setLandmarkColor(new Color3f(colours1[volt]));
univ.getContent(title+""+volt).setLocked(true);
}
Timeline tl = univ.getTimeline();
tl.first();
	for(int t=1;t<=T;t++){
for (int i=0; i<box.objects.getCounter(); i++){
if(box.stop==true){return;}
IJ.showStatus("Generating 3D view...");
String oname = new String();
if(labobj==true){oname = IJ.d2s(box.objects.getValue("Object",i),0);}
else{oname = "";}
double ox = box.objects.getValue("X", i);
double oy = box.objects.getValue("Y", i);
double oz = Z>1?box.objects.getValue("Z", i):1.0;
double oc = box.objects.getValue("C", i);
if((box.analysis=="Rainbow")){
	oc=1d;
	oname = OSbox.alf[(int)box.objects.getValue("Colour Population",i)];
}
double ot = T>1?box.objects.getValue("T", i)/tcal:1.0;
double ol = box.analysis=="Track"?box.objects.getValue("Lineage", i):-1.0;
	if(t==ot){
		univ.getContent(title+""+(int)oc).getPointList().add(oname,ox,oy,oz);
	}

		if(box.analysis=="Track"){
		for(int m=0;m<box.objects.getCounter();m++){
			double oox = box.objects.getValue("X", m);
			double ooy = box.objects.getValue("Y", m);
			double ooz = Z>1?box.objects.getValue("Z", m):1.0;
			//double ooc = box.objects.getValue("C", m);
			double oot = T>1?box.objects.getValue("T", m)/tcal:1.0;
			double ool = box.objects.getValue("Lineage", m);
			if((ol==ool)&&(ot+1==oot)&&(oot<=t)&&(t-oot<box.taillength)){
			float fx1=Double.valueOf(ox).floatValue();float fx2=Double.valueOf(oox).floatValue();
			float fy1=Double.valueOf(oy).floatValue();float fy2=Double.valueOf(ooy).floatValue();
			float fz1=Double.valueOf(oz).floatValue();float fz2=Double.valueOf(ooz).floatValue();
			mesh.add(new Point3f(fx1,fy1,fz1));
			mesh.add(new Point3f(fx2,fy2,fz2));
			}
		}
		}

}
		if(box.analysis=="Track"){
		CustomLineMesh lm = new CustomLineMesh(mesh, CustomLineMesh.PAIRWISE);
		lm.setColor(new Color3f(colours2[1]));
		lm.setLineWidth(5f);
		Content lmc = ContentCreator.createContent(lm, "tail"+t, t-1);
		univ.addContent(lmc);
		univ.getContent("tail"+t).setLocked(true);
		mesh = new ArrayList<Point3f>();
		}
	tl.next();
	}
tl.first();
univ.show();
}//3d settings dialog was OKed
}catch(Exception e){throw(new Exception(e.getStackTrace()[0].toString()+System.getProperty("line.separator")+"(3D visualisation) "+e.toString()));}
}//end show

}
