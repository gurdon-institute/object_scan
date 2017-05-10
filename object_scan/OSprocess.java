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
import ij.WindowManager;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import ij.plugin.frame.RoiManager;

public class OSprocess{
ImageCalculator ic = new ImageCalculator();
double K,xcal,smallsigma,bigsigma;
double m1,m2;
ImagePlus firstDer;
ImageProcessor ip;
String title;

public ImagePlus doProcess(ImagePlus imp,OSbox box) throws Exception{
try{
xcal = imp.getCalibration().pixelWidth;
title = imp.getTitle();

if(box.pronone==false){
	if(box.procanny==true){
		Canny canny = new Canny();
		canny.setGaussianKernelWidth(16);//kernel radius, default=16
		canny.setGaussianKernelRadius(2);//sigma, default=2
		canny.setLowThreshold(2.5f);//hysteresis low(stop), default=2.5
		canny.setHighThreshold(7.5f);//hysteresis high(start), default=7.5
		canny.setContrastNormalised(false);
		canny.run("");
	}
	else{
	m1 = imp.getStatistics().mean;
	if(box.prodog==true){K=4.0;}
	else if(box.prolog==true){K=1.6;}
	smallsigma = (box.estd/xcal)/10;
	bigsigma = smallsigma*K;
	ImagePlus smallimp = new Duplicator().run(imp);
	ImagePlus bigimp = new Duplicator().run(imp);
	IJ.run(smallimp, "Gaussian Blur...", "sigma="+smallsigma);
	IJ.run(bigimp, "Gaussian Blur...", "sigma="+bigsigma);
	ImagePlus dogged = ic.run("Subtract create", smallimp, bigimp);
	imp.setProcessor(dogged.getProcessor());
	dogged.close();
	bigimp.close();
	smallimp.close();
	m2 = imp.getStatistics().mean;
	IJ.run(imp, "Multiply...", "value="+m1/m2);
	}
}

}catch(Exception e){throw(new Exception(e.getStackTrace()[1].toString()+System.getProperty("line.separator")+"(Set.analysis) "+e.toString()));}
finally{return imp;}
}

public ImagePlus doSegment(ImagePlus imp,ImagePlus copy,OSbox box) throws Exception{
try{
int bd = imp.getBitDepth();
double drmax = imp.getDisplayRangeMax();

if(box.wat==true){IJ.run(imp, "Watershed", "");}
if(box.circseg==true){ //morphological segment assuming circular cross-sections
IJ.run(imp, "Select None", "");
ImagePlus bob = new Duplicator().run(imp);
IJ.run(bob, "Ultimate Points", "");	//bob.show();if(2==2){return(0);}
IJ.setThreshold(bob, 5, 255);	//exclude least likely points
IJ.run(bob, "Convert to Mask", "");
IJ.run(bob, "Create Selection", "");
if(bob.getRoi()!=null){
RoiManager seeds = new RoiManager(false);
seeds.addRoi(bob.getRoi());
seeds.select(bob, 0);
WindowManager.setTempCurrentImage(bob);
	if(bob.getRoi().getType()==9){  //9 is constant for composite selection
	seeds.runCommand("Split");
	seeds.select(bob, 0);
	seeds.runCommand("Delete");
	}
WindowManager.setTempCurrentImage(imp);
	for(int seed=0;seed<seeds.getCount();seed++){
	seeds.select(imp, seed);
	boolean tooblack = false;
	int limit = (int)drmax;	//lower limit for mean intensity inside expanding circle
		while(tooblack==false){
		imp.setRoi(new OvalRoi(imp.getRoi().getBounds().x-1,imp.getRoi().getBounds().y-1,imp.getRoi().getBounds().width+2,imp.getRoi().getBounds().height+2));
		if(imp.getStatistics().mean<limit){tooblack=true;}
		}
	seeds.runCommand("Update");
	}
WindowManager.setTempCurrentImage(bob);
bob.setRoi(0,0,bob.getWidth(),bob.getHeight());
IJ.run(bob, "Clear", "slice");
	for(int seed=0;seed<seeds.getCount();seed++){
	seeds.select(bob, seed);
	seeds.runCommand("fill");
	}
seeds.close();
}//roi!=null

IJ.run(bob, "Watershed", "");
IJ.run(bob, "Invert", "");		//bob.show();if(2==2){return(0);}
ImagePlus out = ic.run("Subtract create", imp, bob);
IJ.run(out, "Watershed", "");
imp = out;
out.changes = false;out.close();
bob.changes = false;bob.close();
WindowManager.setTempCurrentImage(imp);
}//morphological segment


if(box.grow==true){ //intensity based region growing segment
IJ.run(imp, "Select None", "");
ImagePlus bob = new Duplicator().run(imp);
IJ.run(bob, "Ultimate Points", "");
IJ.setThreshold(bob, 1, Math.pow(2,bd));	//include all ultimate points
IJ.run(bob, "Convert to Mask", "");
IJ.run(bob, "Create Selection", "");
if(bob.getRoi()!=null){
RoiManager seeds = new RoiManager(false);
seeds.addRoi(bob.getRoi());
seeds.select(bob, 0);
WindowManager.setTempCurrentImage(bob);
	if(bob.getRoi().getType()==9){  //9 is constant for composite selection
	seeds.runCommand("Split");
	seeds.select(bob, 0);
	seeds.runCommand("Delete");
	}
WindowManager.setTempCurrentImage(copy);
Roi[] sps = seeds.getRoisAsArray();
seeds.close();
	for(int seed=0;seed<seeds.getCount();seed++){
	boolean exit = false;
	copy.setRoi(sps[seed]);
	int seedX = sps[seed].getBounds().x;
	int seedY = sps[seed].getBounds().y;
	double tol = 0.0;
	double inc = bd==16?16:1;
	WindowManager.setTempCurrentImage(copy);
	IJ.run(copy, "Select None", "");
	double tollim = drmax/2;
	Roi theRoi;
	Roi lastRoi = copy.getRoi();
	int wide = imp.getWidth()-1;
	int high = imp.getHeight()-1;
		do{	IJ.run(copy, "Select None", "");
			IJ.doWand(seedX, seedY, tol, "8-connected");
			theRoi = copy.getRoi();
			if((tol>=tollim)||(theRoi.contains(0,0))||(theRoi.contains(wide,0))||(theRoi.contains(0,high))||(theRoi.contains(wide,high))){
			copy.setRoi(lastRoi);
			exit=true;
			break;
			}
			tol+=inc;
			Rectangle rect = copy.getRoi().getBounds();
			seedX = rect.x + (rect.width/2);
			seedY = rect.y + (rect.height/2);
			sps[seed] = copy.getRoi();
			lastRoi = copy.getRoi();
		}while(exit==false);
	}//seeds loop
WindowManager.setTempCurrentImage(bob);
bob.setRoi(0,0,bob.getWidth(),bob.getHeight());
IJ.run(bob, "Clear", "slice");
	for(int seed=0;seed<sps.length;seed++){
	if(sps[seed]==null){continue;}
	bob.setRoi(sps[seed]);
	IJ.run(bob, "Fill", "slice");
	IJ.run(bob, "Select None", "");
	}
}//roi!=null
imp = bob;
bob.changes = false;bob.close();
WindowManager.setTempCurrentImage(imp);
}//intensity segment

}catch(Exception e){IJ.log(e.getStackTrace()[0].toString()+System.getProperty("line.separator")+"(Segment) "+e.toString());}
finally{return imp;}
}

}
