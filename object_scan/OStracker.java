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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import java.util.*;
import java.awt.Color;
import ij.measure.ResultsTable;

public class OStracker{

double index1,index2,x1,y1,z1,a1,c1,x2,y2,z2,a2,c2,t1,t2,trackcur,ed,xcal,ycal,zcal,tcal;
int C,Z,T;

public void track(ImagePlus imp,Overlay ol,OSbox box,Color[] colours1,Color[] colours2){
C = imp.getNChannels();
Z = imp.getNSlices();
T = imp.getNFrames();
String unit = imp.getCalibration().getUnit();
String title = imp.getTitle();
xcal = imp.getCalibration().pixelWidth;
ycal = imp.getCalibration().pixelHeight;
zcal = imp.getCalibration().pixelDepth;
tcal = imp.getCalibration().frameInterval;
for(int i=0;i<box.rt.getCounter();i++){
index1 = box.rt.getValue("Object",i);
x1 = box.rt.getValue("X",i);
y1 = box.rt.getValue("Y",i);
if(Z>1){z1=box.rt.getValue("Slice",i);}else{z1=1.0;}
if(T>1){t1=box.rt.getValue("Frame",i);}else{t1=1.0;}
if(C>1){c1=box.rt.getValue("Ch",i);}else{c1=1.0;}
a1 = box.rt.getValue("Area",i);
z1=z1*zcal;	//calibrate slice number to get z coordinate
trackcur=box.rt.getValue("Track Distance"+"("+unit+")",i);
	for(int j=0;j<box.rt.getCounter();j++){
	IJ.showStatus("Mapping Scanned Set.objects...");
	index2 = box.rt.getValue("Object",j);
	x2 = box.rt.getValue("X",j);
	y2 = box.rt.getValue("Y",j);
	if(Z>1){z2=box.rt.getValue("Slice",j);}else{z2=1.0;}
	if(T>1){t2=box.rt.getValue("Frame",j);}else{t2=1.0;}
	if(C>1){c2=box.rt.getValue("Ch",j);}else{c2=1.0;}
	a2 = box.rt.getValue("Area",j);
	z2=z2*zcal;
	ed = Math.sqrt(Math.pow((x1-x2),2)+Math.pow((y1-y2),2)+Math.pow(z1-z2,2));
	double range = box.clusr;	//range entered
		if((t1+1==t2)&&(c1==c2)&&(ed<trackcur)&&(ed<=range)){	//same object over time (i=1,j=2)
			box.rt.setValue("Child",i,index2);
			box.rt.setValue("Track Distance"+"("+unit+")",i,ed);
			trackcur = ed;
		}
	}
}

	for(int i1=0;i1<box.rt.getCounter();i1++){
	IJ.showStatus("Assigning object relationships...");
	double object1 = box.rt.getValue("Object",i1);
	double child1 = box.rt.getValue("Child",i1);
	double dist1 = box.rt.getValue("Track Distance"+"("+unit+")",i1);
		for(int i2=0;i2<box.rt.getCounter();i2++){
		double object2 = box.rt.getValue("Object",i2);
		double child2 = box.rt.getValue("Child",i2);
		double dist2 = box.rt.getValue("Track Distance"+"("+unit+")",i2);
			if((i1!=i2)&&(child1==child2)){
				if(dist1<dist2){
					box.rt.setValue("Child",i2,0);
					child2=0;
				}
				else{
					box.rt.setValue("Child",i1,0);
					child1=0;
				}
			}
			if(child1==object2){
				box.rt.setValue("Parent",i2,object1);
			}
		}	
	}

	for(int i1=0;i1<box.rt.getCounter();i1++){
	IJ.showStatus("Assigning lineages...");
	double object1 = 0.0;
	object1 = box.rt.getValue("Object",i1);
	double lineage1 = box.rt.getValue("Lineage",i1);
	double child1 = box.rt.getValue("Child",i1);
	double dist1 = box.rt.getValue("Track Distance"+"("+unit+")",i1);
		for(int i2=0;i2<box.rt.getCounter();i2++){
		double object2 = box.rt.getValue("Object",i2);
			if(child1==object2){
				box.rt.setValue("Lineage",i2,lineage1);
			}
		}
		for(int i3=0;i3<box.objects.getCounter();i3++){
		double object3 = box.objects.getValue("Object",i3);
			if(object1==object3){
			box.objects.setValue("Lineage",i3,lineage1);
			}
			if(object3==child1){
			box.objects.setValue("Track Distance"+"("+unit+")",i3,dist1);
			}
		}
	}

double[] dist = new double[box.rt.getCounter()+1];
double[] time = new double[box.rt.getCounter()+1];
boolean[][] lins = new boolean[T+1][box.rt.getCounter()+1]; //default values are false
for(int i1=0;i1<box.objects.getCounter();i1++){
	IJ.showStatus("Calculating track movement...");
	double D = 1d/0d;
	D = box.objects.getValue("Track Distance"+"("+unit+")",i1);
	double lineage1 = box.objects.getValue("Lineage",i1);
	double t1 = box.objects.getValue("T",i1);
	if(Double.isInfinite(D)==false){
	dist[(int)lineage1] += D;
	}
	if(lins[(int)t1][(int)lineage1]==false){ //only count each timepoint once for each lineage
	time[(int)lineage1]++;
	lins[(int)t1][(int)lineage1] = true;
	}
}

if(box.taillength>0){
box.taillength = box.taillength<=T?box.taillength:T;
for(int i1=0;i1<box.rt.getCounter();i1++){
IJ.showStatus("Drawing lineages...");
double lin1 = box.rt.getValue("Lineage",i1);
double x1 = box.rt.getValue("X",i1);
double y1 = box.rt.getValue("Y",i1);
if(Z>1){z1 = box.rt.getValue("Slice",i1);}else{z1=1.0;}
if(T>1){t1 = box.rt.getValue("Frame",i1);}else{t1=1.0;}
if(C>1){c1 = box.rt.getValue("Ch",i1);}else{c1=1.0;}
Color arrowcolour = Color.cyan;
int alpha = 255;
ArrayList<javax.vecmath.Point3f> mesh = new ArrayList<javax.vecmath.Point3f>();
arrowcolour = colours2[(int)c1];
	for(int i2=0;i2<box.rt.getCounter();i2++){
	arrowcolour = new Color(arrowcolour.getRed(),arrowcolour.getGreen(),arrowcolour.getBlue(),alpha);
	double lin2 = box.rt.getValue("Lineage",i2);
	double x2 = box.rt.getValue("X",i2);
	double y2 = box.rt.getValue("Y",i2);
	if(Z>1){z2 = box.rt.getValue("Slice",i2);}else{z2=1.0;}
	if(T>1){t2 = box.rt.getValue("Frame",i2);}else{t2=1.0;}
	if(C>1){c2 = box.rt.getValue("Ch",i2);}else{c2=1.0;}
		if((z1==z2)&&(lin1==lin2)&&(t1+1==t2)){			//1->2
			alpha = 255;
			for(int i=0;i<=box.taillength;i++){
			if(i!=0){alpha = alpha-(255/(box.taillength+1));}
			arrowcolour = new Color(arrowcolour.getRed(),arrowcolour.getGreen(),arrowcolour.getBlue(),alpha);
			double wide = box.taillength-i<10?box.taillength-i:10;
			Arrow tail = new Arrow(x1/xcal,y1/ycal,x2/xcal,y2/ycal);
			tail.setStyle(3);
			tail.setStrokeColor(arrowcolour);
			tail.setStrokeWidth(wide);
			if(imp.getNDimensions()>3){tail.setPosition((int)c2,(int)z2,(int)t2+i);}
			else if(imp.getNDimensions()==3){tail.setPosition((Z>T)?(int)z2:(C>T)?(int)c2:(int)t2+i);}
			else{tail.setPosition(0);}
			ol.add(tail);
			
			OvalRoi rounder1 = new OvalRoi((x1/xcal)-(wide/2),(y1/ycal)-(wide/2),wide,wide);
			rounder1.setFillColor(arrowcolour);
			if(imp.getNDimensions()>3){rounder1.setPosition((int)c2,(int)z2,(int)t2+i);}
			else if(imp.getNDimensions()==3){rounder1.setPosition((Z>T)?(int)z2:(C>T)?(int)c2:(int)t2+i);}
			else{rounder1.setPosition(0);}
			ol.add(rounder1);
			OvalRoi rounder2 = new OvalRoi((x2/xcal)-(wide/2),(y2/ycal)-(wide/2),wide,wide);
			rounder2.setFillColor(arrowcolour);
			if(imp.getNDimensions()>3){rounder2.setPosition((int)c2,(int)z2,(int)t2+i);}
			else if(imp.getNDimensions()==3){rounder2.setPosition((Z>T)?(int)z2:(C>T)?(int)c2:(int)t2+i);}
			else{rounder1.setPosition(0);}
			ol.add(rounder2);
			}		
		}
	}
}
}//box.taillength>0
for(int i=0;i<dist.length;i++){
			if(time[i]>=T/2){
	double speed = dist[i]/(time[i]*imp.getCalibration().frameInterval!=0?imp.getCalibration().frameInterval:1.0);
		if(Double.isNaN(speed)==false){
		IJ.log("Lineage "+i+" : "+speed+" "+unit+"/"+imp.getCalibration().getTimeUnit()+" over "+IJ.d2s(time[i],0)+"/"+T+" frames");
		}
			}
	} 

}//track

}

