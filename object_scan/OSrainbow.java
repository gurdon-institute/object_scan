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
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.JEditorPane;
import javax.swing.JFrame;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;

public class OSrainbow{
double change;
int colmax;
private final Image iconimage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_icon.gif"));

public Overlay run(ImagePlus imp,Overlay ol,OSbox box)throws Exception{
try{
int itc = 0;
int OC = box.objects.getCounter();
double r1,r2,g1,g2,b1,b2;
double diff;
String title = imp.getTitle();

double m = -1d;
for(int i=0;i<box.objects.getCounter();i++){
m = Math.max(box.objects.getValue("C1 mean",i),m);
m = Math.max(box.objects.getValue("C2 mean",i),m);
m = Math.max(box.objects.getValue("C3 mean",i),m);
}

int shortR = (int)Math.round(m/6);	//short axis radius
int longR = (int)Math.round(m/4);	//long axis radius
Color[] centroids = new Color[]{	//7,8,7 lattice of spheroids
			new Color(shortR*1,longR*2,longR*1),
	new Color(shortR*2,longR*1,longR*1),new Color(shortR*2,longR*3,longR*1),
			new Color(shortR*3,longR*2,longR*1),
	new Color(shortR*4,longR*1,longR*1),new Color(shortR*4,longR*3,longR*1),
			new Color(shortR*5,longR*2,longR*1),
	
	new Color(shortR*1,longR*1,longR*2),new Color(shortR*1,longR*3,longR*2),
			new Color(shortR*2,longR*2,longR*2),
	new Color(shortR*3,longR*1,longR*2),new Color(shortR*3,longR*3,longR*2),
			new Color(shortR*4,longR*2,longR*2),
	new Color(shortR*5,longR*1,longR*2),new Color(shortR*5,longR*3,longR*2),
	
			new Color(shortR*1,longR*2,longR*3),
	new Color(shortR*2,longR*1,longR*3),new Color(shortR*2,longR*3,longR*3),
			new Color(shortR*3,longR*2,longR*3),
	new Color(shortR*4,longR*1,longR*3),new Color(shortR*4,longR*3,longR*3),
			new Color(shortR*5,longR*2,longR*3),
};

double mergelim = Math.sqrt(Math.pow(m,2)+Math.pow(m,2))/centroids.length;	//diagonal of the colour space divided by number of possible populations
double changelim = 0.01d;
do{
	itc++;
	for(int i2=0;i2<OC;i2++){	//assign box.objects to closest centroid
		IJ.showStatus("Rainbow colour space clustering...");
		double closest = 999999999d;
		for(int i1=0;i1<centroids.length;i1++){
		if(centroids[i1]!=null){
				r1 = centroids[i1].getRed();	r2 = box.objects.getValue("C1 mean",i2);
				g1 = centroids[i1].getGreen();	g2 = box.objects.getValue("C2 mean",i2);
				b1 = centroids[i1].getBlue();	b2 = box.objects.getValue("C3 mean",i2);
				diff = Math.sqrt(Math.pow((r1-r2),2)+Math.pow((g1-g2),2)+Math.pow((b1-b2),2));
					if((diff<closest)){
					box.objects.setValue("Colour Population",i2,i1);		//IJ.log("put object "+box.objects.getValue("Object",i2)+" into cluster "+i1+" at diff "+diff);
					closest=diff;
					}
		}
		}
	}
	for(int i1=0;i1<centroids.length;i1++){	//merge clusters
		IJ.showStatus("Rainbow colour space clustering...");
		if(centroids[i1]!=null){
			for(int i2=0;i2<centroids.length;i2++){
				if((centroids[i1]!=null)&&(centroids[i2]!=null)){
				r1 = centroids[i1].getRed();	r2 = centroids[i2].getRed();
				g1 = centroids[i1].getGreen();	g2 = centroids[i2].getGreen();
				b1 = centroids[i1].getBlue();	b2 = centroids[i2].getBlue();
				diff = Math.sqrt(Math.pow((r1-r2),2)+Math.pow((g1-g2),2)+Math.pow((b1-b2),2));
					if((i1!=i2)&&(diff<=mergelim)){		//IJ.log("merged centroids "+i1+" and "+i2+" at diff "+diff);
						int low = Math.min(i1,i2);
						int high = Math.max(i1,i2);
						centroids[high] = null;
						centroids[low] = new Color((int)(r1+r2)/2,(int)(g1+g2)/2,(int)(b1+b2)/2);
						for(int oi=0;oi<OC;oi++){
							if(box.objects.getValue("Colour Population",oi)==high){
								box.objects.setValue("Colour Population",oi,low);
							}
						}
					}
				}
			}
		}
	}
	for(int i1=0;i1<centroids.length;i1++){	//recalculate centroids
		IJ.showStatus("Rainbow colour space clustering...");
		if(centroids[i1]!=null){
		int thiscount = 0;
		int centR = 0;
		int centG = 0;
		int centB = 0;
		for(int i2=0;i2<OC;i2++){
			if(i1==box.objects.getValue("Colour Population",i2)){
				thiscount++;
				centR += box.objects.getValue("C1 mean",i2);
				centG += box.objects.getValue("C2 mean",i2);
				centB += box.objects.getValue("C3 mean",i2);
			}
		}
		if(thiscount<1){
			centroids[i1] = null;	//set empty centroids to null
		}
		else{
		centR = centR/thiscount;centG = centG/thiscount;centB = centB/thiscount;
		change = Math.sqrt(Math.pow((centroids[i1].getRed()-centR),2)+Math.pow((centroids[i1].getGreen()-centG),2)+Math.pow((centroids[i1].getBlue()-centB),2));
			if(change>changelim){		
			centroids[i1] = new Color(centR,centG,centB);	//set centroid to new values
			}
		}
		}
	}
}while((change>changelim)&&(itc<100));
box.objects.updateResults();
for(int i1=0;i1<box.objects.getCounter();i1++){
	IJ.showStatus("Rainbow mapping...");
	box.objects.setValue("C",i1,0d);
	 for(int i2=0;i2<box.rt.getCounter();i2++){
		if(box.objects.getValue("Object",i1)==box.rt.getValue("Object",i2)){	//get population indices from box.objects and put in box.rt
			box.rt.setValue("Kindex",i2,box.objects.getValue("Colour Population",i1));	//IJ.log("set Kindex "+box.objects.getValue("Colour Population",i1));
		}
	} 
}
for(int i1=0;i1<box.rt.getCounter();i1++){	//ugly way to get population indices from the primary channel and copy to the same object in all channels
	IJ.showStatus("Rainbow mapping...");
	double x1 = box.rt.getValue("X",i1);double y1 = box.rt.getValue("Y",i1);double a1 = box.rt.getValue("Area",i1);double c1 = box.rt.getValue("Ch",i1);
	for(int i2=0;i2<box.rt.getCounter();i2++){
	double x2 = box.rt.getValue("X",i2);double y2 = box.rt.getValue("Y",i2);double a2 = box.rt.getValue("Area",i2);double c2 = box.rt.getValue("Ch",i2);
		if((c1==box.prim)&&(c2!=c1)&&(x1==x2)&&(y1==y2)&&(a1==a2)){
		box.rt.setValue("Kindex",i2,box.rt.getValue("Kindex",i1));
		}
	}
}

colmax = 0;
for(int i=0;i<box.objects.getCounter();i++){
colmax = Math.max(colmax,(int)box.objects.getValue("Colour Population",i));
}

JFrame colframe = new JFrame("Rainbow: "+title); //make colourful objects table
colframe.setUndecorated(true);
colframe.setIconImage(iconimage);
StringBuffer rows = new StringBuffer();
OC = box.objects.getCounter();
double[] r = new double[colmax+1];
double[] g = new double[colmax+1];
double[] b = new double[colmax+1];
String rstring,gstring,bstring =new String();
int[] csets = new int[colmax+1];
for(int a=0;a<box.objects.getCounter();a++){
csets[(int)box.objects.getValue("Colour Population",a)]++;
}
for(int i=0;i<csets.length;i++)if(csets[i]>0){}	//count no. of populations with members
for(int i=0;i<csets.length;i++){
	IJ.showStatus("Rainbow mapping...");
	if(csets[i]>0){	//index of csets is the colour population index
	for(int col=0;col<OC;col++){
		if((int)box.objects.getValue("Colour Population",col)==i){
		r[(int)box.objects.getValue("Colour Population",col)] += box.objects.getValue("C1 mean",col);
		g[(int)box.objects.getValue("Colour Population",col)] += box.objects.getValue("C2 mean",col);
		b[(int)box.objects.getValue("Colour Population",col)] += box.objects.getValue("C3 mean",col);
		}
	}
	rstring=String.valueOf((int)Math.ceil(r[i]/csets[i]));
	gstring=String.valueOf((int)Math.ceil(g[i]/csets[i]));
	bstring=String.valueOf((int)Math.ceil(b[i]/csets[i]));
	String bgcolour = "rgb("+rstring+","+gstring+","+bstring+")";
	String textlabel = "<span style='color:white;font-size:8px;'>"+rstring+","+gstring+","+bstring+"</span>";
	rows.append("<tr><td style='border-style:solid;border-width:1px;border-color:black;background-color:white;text-align:center;'>"+OSbox.alf[i]+"("+String.valueOf(i)+")</td><td style='border-style:solid;border-width:1px;border-color:black;background-color:"+bgcolour+";'>"+textlabel+"</td><td style='border-style:solid;border-width:1px;border-color:black;background-color:rgb("+OSbox.rainbow[i].getRed()+","+OSbox.rainbow[i].getGreen()+","+OSbox.rainbow[i].getBlue()+")'></td><td style='border-style:solid;border-width:1px;'>"+csets[i]+"</td></tr>");
	}
}
String coltext = "<html><body style='background-color:white;'>"+
"<table border='0' cellspacing='0' cellpadding='6' style='background-color:white;color:black;font-size:16px;text-align:center;font-family:Arial;'>"+
"<tr><th style='border-style:solid;border-width:1px;border-color:black;'>Population</th><th style='border-style:solid;border-width:1px;border-color:black;'>Colour</th><th style='border-style:solid;border-width:1px;border-color:black;'>Ref Colour</th><th style='border-style:solid;border-width:1px;border-color:black;'>N</th></tr>"+
rows+"</table></body></html>";
JEditorPane colpane = new JEditorPane("text/html",coltext);
colpane.setText(coltext);
colframe.add(colpane);
colframe.pack();
java.awt.image.BufferedImage colimg = new java.awt.image.BufferedImage(colpane.getSize().width,colpane.getSize().height,java.awt.image.BufferedImage.TYPE_INT_RGB);
colpane.paint(colimg.createGraphics());		//create image of html table
ImagePlus colourimp = new ImagePlus("Rainbow: "+title,colimg);
colourimp.show("");
colframe.dispose();
for(int i=0;i<box.rt.getCounter();i++){		//set roi colours
	int pop = (int)box.rt.getValue("Kindex",i);
	ol.get(i).setStrokeColor(OSbox.rainbow[pop]);
	if(box.fill==true){
	ol.get(i).setFillColor(OSbox.rainbow[pop]);
	}
}

return ol;
}catch(Exception e){throw(new Exception(e.getStackTrace()[0].toString()+e.getStackTrace()[1].toString()+System.getProperty("line.separator")+"(Rainbow k-means) "+e.toString()));}

}
}
