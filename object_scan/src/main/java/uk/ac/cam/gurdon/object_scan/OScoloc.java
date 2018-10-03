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

import java.awt.Polygon;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.measure.ResultsTable;

public class OScoloc{
private Polygon poly;
double sum1,sum2,mean1,mean2,NUM,DENOM1,DENOM2;
double[] pcc,moc,icq;
int CC,c2;
Plot plot;
static final double labX = 0.05;
static final double labY = 0.05;
public ImageStack plotPile = new ImageStack(600,600);

public double[] PCC(ImagePlus imp,Roi roi){
try{
CC = imp.getNChannels();
c2 = roi.getCPosition();
@SuppressWarnings("unchecked")
ArrayList<Integer>[] holder = new ArrayList[CC+1];
for(int h=1;h<=CC;h++){holder[h]=new ArrayList<Integer>();}
for(int c=1;c<=imp.getNChannels();c++){
	poly = roi.getPolygon();
	holder[c] = new ArrayList<Integer>();
	imp.setPosition(c,roi.getZPosition(),roi.getTPosition());
	for(int x=0;x<imp.getWidth();x++){
		for(int y=0;y<imp.getHeight();y++){
			if(poly.contains(x,y)){
			holder[c].add(imp.getProcessor().getPixel(x,y));
			}
		}
	}
}

pcc = new double[(int)Math.round(Math.pow(CC,2)+1)];
for(int c1=1;c1<=CC;c1++){
	if(c1!=c2){
		int n = holder[c1].size();
		sum1=0;sum2=0;NUM=0;DENOM1=0;DENOM2=0;
		for(int i=0;i<n;i++){
			sum1+=(Integer)holder[c1].get(i);
			sum2+=(Integer)holder[c2].get(i);
		}
		mean1=sum1/n;	mean2=sum2/n;
		for(int i=0;i<n;i++){
			NUM+=((Integer)holder[c1].get(i)-mean1) * ((Integer)holder[c2].get(i)-mean1);
			DENOM1+=Math.pow((Integer)holder[c1].get(i)-mean1,2);
			DENOM2+=Math.pow((Integer)holder[c2].get(i)-mean2,2);
		}
		pcc[c1] = NUM/Math.sqrt(DENOM1*DENOM2);
		if(Double.isNaN(pcc[c1])){pcc[c1]=0d;}
			double[] xd = new double[n];
			double[] yd = new double[n];
			for(int w=0;w<n;w++){
			xd[w] = Double.parseDouble((String)holder[c2].get(w).toString());
			yd[w] = Double.parseDouble((String)holder[c1].get(w).toString());
			}
			int limit = 0;
			for(int i=0;i<imp.getStackSize();i++){
				imp.setSliceWithoutUpdate(i);
				limit = (int)Math.round(Math.max(limit,imp.getStatistics().max));
			}
			plot = new Plot("plot","C"+c2,"C"+c1,new double[1],new double[1],Plot.X_NUMBERS+Plot.Y_NUMBERS+Plot.X_GRID+Plot.Y_GRID);
			//plot.setLimits(0d,imp.getDisplayRangeMax(),0d,imp.getDisplayRangeMax());
			plot.setLimits(0d,limit,0d,limit);
			plot.setSize(600,600);
			plot.addPoints(xd,yd,Plot.X);
			plot.addLabel(labX,labY,"PCC = "+IJ.d2s(pcc[c1],4));
	}
}

}catch(Exception e){IJ.log("PCC: "+e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
return pcc;
}

public double[] MOC(ImagePlus imp,Roi roi){
try{
CC = imp.getNChannels();
c2 = roi.getCPosition();
@SuppressWarnings("unchecked")
ArrayList<Integer>[] holder = new ArrayList[CC+1];
for(int h=1;h<=CC;h++){holder[h]=new ArrayList<Integer>();}
for(int c=1;c<=imp.getNChannels();c++){
	poly = roi.getPolygon();
	holder[c] = new ArrayList<Integer>();
	imp.setPosition(c,roi.getZPosition(),roi.getTPosition());
	for(int x=0;x<imp.getWidth();x++){
		for(int y=0;y<imp.getHeight();y++){
			if(poly.contains(x,y)){
			holder[c].add(imp.getProcessor().getPixel(x,y));
			}
		}
	}
}

moc = new double[(int)Math.round(Math.pow(CC,2)+1)];
for(int c1=1;c1<=CC;c1++){
	if(c1!=c2){
		int n = holder[c1].size();
		NUM=0;DENOM1=0;DENOM2=0;
		for(int i=0;i<n;i++){
			NUM+=(Integer)holder[c1].get(i)*(Integer)holder[c2].get(i);
			DENOM1+=Math.pow((Integer)holder[c1].get(i),2);
			DENOM2+=Math.pow((Integer)holder[c2].get(i),2);
		}
		moc[c1] = NUM/Math.sqrt(DENOM1*DENOM2);
		if(Double.isNaN(moc[c1])){moc[c1]=0d;}
		plot.addLabel(labX,labY*2,"MOC = "+IJ.d2s(moc[c1],4));
	}
}
}catch(Exception e){IJ.log("MOC: "+e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
return moc;
}

public double[] ICQ(ImagePlus imp,Roi roi){
try{
CC = imp.getNChannels();
c2 = roi.getCPosition();
@SuppressWarnings("unchecked")
ArrayList<Integer>[] holder = new ArrayList[CC+1];
for(int h=1;h<=CC;h++){holder[h]=new ArrayList<Integer>();}
for(int c=1;c<=imp.getNChannels();c++){
	poly = roi.getPolygon();
	holder[c] = new ArrayList<Integer>();
	imp.setPosition(c,roi.getZPosition(),roi.getTPosition());
	for(int x=0;x<imp.getWidth();x++){
		for(int y=0;y<imp.getHeight();y++){
			if(poly.contains(x,y)){
			holder[c].add(imp.getProcessor().getPixel(x,y));
			}
		}
	}
}
icq = new double[(int)Math.round(Math.pow(CC,2)+1)];
for(int c1=1;c1<=CC;c1++){
	if(c1!=c2){
		int n = holder[c1].size();
		sum1=0;sum2=0;NUM=0;DENOM1=0;
		for(int i=0;i<n;i++){
			sum1+=(Integer)holder[c1].get(i);
			sum2+=(Integer)holder[c2].get(i);
		}
		mean1=sum1/n;	mean2=sum2/n;
		for(int i=0;i<n;i++){
			if(((Integer)holder[c1].get(i)-mean1) * ((Integer)holder[c2].get(i)-mean2)>0){NUM++;}
			DENOM1++;
		}
		icq[c1] = (NUM/DENOM1)-0.5;
		if(Double.isNaN(icq[c1])){icq[c1]=0d;}
		plot.addLabel(labX,labY*3,"ICQ = "+IJ.d2s(icq[c1],4));
		plotPile.addSlice(plot.getProcessor());
	}
}
}catch(Exception e){IJ.log("ICQ: "+e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
return icq;
}

public void labelise(ResultsTable rt,int C,int Z,int T){
try{
	for(int i=1;i<=plotPile.getSize();i++){
		String label = "Roi from object "+IJ.d2s(rt.getValue("Object",i-1),0)+" : C"+(C>1?IJ.d2s(rt.getValue("Ch",i-1),0):1)+", Z"+(Z>1?IJ.d2s(rt.getValue("Slice",i-1),0):1)+", T"+(T>1?IJ.d2s(rt.getValue("Frame",i-1),0):1);
		plotPile.setSliceLabel(label,i);
		if(WindowManager.getFrame("plot")!=null){WindowManager.getFrame("plot").dispose();}	//dispose all remaining plot windows
	}
ImagePlus plotted = new ImagePlus("Pixel Intensity Plots",plotPile);
plotted.show();
}catch(Exception e){IJ.log("labelise: "+e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
return;
}

public ResultsTable combine(ResultsTable rt,ResultsTable objects){
try{
double thisObj;
double pccTotal = 0d;
double mocTotal = 0d;
double icqTotal = 0d;
int count = 0;
for(int outer=0;outer<objects.getCounter();outer++){
	thisObj = objects.getValue("Object",outer);
for(int c=1;c<=CC;c++){
	count = 0;
	pccTotal = 0d;
	mocTotal = 0d;
	icqTotal = 0d;
for(int inner=0;inner<rt.getCounter();inner++){
	if(thisObj==rt.getValue("Object",inner)){
	count++;
	pccTotal+=rt.columnExists(rt.getColumnIndex("PCC vs C"+c))?rt.getValue("PCC vs C"+c,inner):0d;
	mocTotal+=rt.columnExists(rt.getColumnIndex("MOC vs C"+c))?rt.getValue("MOC vs C"+c,inner):0d;
	icqTotal+=rt.columnExists(rt.getColumnIndex("ICQ vs C"+c))?rt.getValue("ICQ vs C"+c,inner):0d;
	}
}	
	if(count>0){
	objects.setValue("PCC vs C"+c,outer,pccTotal/count);
	objects.setValue("MOC vs C"+c,outer,mocTotal/count);
	objects.setValue("ICQ vs C"+c,outer,icqTotal/count);
	}
}
}
objects.setPrecision(4);
}catch(Exception e){IJ.log("combine: "+e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
return objects;
}

}
