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
import java.awt.Font;
import ij.measure.ResultsTable;
import ij.Prefs;
import ij.IJ;
import java.awt.BasicStroke;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class OSbox{

public boolean pronone=true;
public boolean prodog,prolog,procanny,noseg,wat,circseg,grow,slice;
public ResultsTable rt,objects;
public int sigma,varreq,clickX,clickY;
public double estd;
public String analysis;
public double clusr;
public int prim;
public int primgrad;
public String primproc;
public int taillength;
public boolean exedge,conv,downs;
public boolean nos,fill,view3d;
public String unit,colourset,outlineset;
public boolean stop;
public static final Font smallfont = new Font("Arial",Font.BOLD,10);
public static final Font bigfont = new Font("Arial",Font.BOLD,12);
public static final Font medfont = new Font("Arial",Font.BOLD,16);
public static final Font biggerfont = new Font("Arial",Font.BOLD,20);
public static final Color backcol = new Color(255,220,255);
public static final Color frontcol = new Color(0,0,0);
public static final Color brightcol = new Color(196,0,196);
public static final Color buttoncol1 = new Color(255,96,255);
public static final Color buttoncol2 = new Color(255,150,255);
public static final String[] analysischoice = {"Standard","Surrounding Foci","Contained Foci","Clustered Foci","Object Count","Object Intensity Correlation","Object Colocalisation","Contained Signal","Rainbow","Track"};
public static final String[] colourchoice = {"Bold","Light","Dark","Channels","Inverse"};
public static final String[] outlinechoice = {"Thin","Thick","Dotted","Dashed","None"};
public static final String[] alf = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
public static final int activeW = 180;
public static final int activeH = 240;
public static final BasicStroke thinStroke = new BasicStroke(1f);
public static final BasicStroke thickStroke = new BasicStroke(3f);
public static final BasicStroke dotStroke = new BasicStroke(1f, 0, 0, 10f, new float[]{1f,10f}, 0f);
public static final BasicStroke dashStroke = new BasicStroke(1f, 0, 0, 10f, new float[]{10f}, 0f);
public Color[] colours1,colours2;
public BasicStroke outline;

public static final Color[] rainbow = new Color[]{ 	//based on 22 colours of maximum contrast (Kelly, Color Eng. 3(6) (1965)) 
new Color(255,255,255), new Color(255,255,0),new Color(128,0,128),new Color(255,128,0),new Color(0,255,255),
new Color(255,0,0),new Color(255,192,128),new Color(128,128,128),new Color(0,255,0),new Color(255,128,255),
new Color(0,0,255),new Color(255,192,128),new Color(64,64,255),new Color(255,192,64),new Color(255,64,192),
new Color(192,192,0),new Color(192,0,64),new Color(192,255,0),new Color(192,128,64),new Color(255,64,0),
new Color(64,128,64),new Color(0,128,128)};

public static final String help = 
"<html>"+
"<body style=\"font-family:arial,sans-serif;font-size:14pt;font-weight:normal;width:1000px;background-color:white;padding:10px;\">"+
"<h3 align=\"center\" style=\"font-size:18pt;\">Object Scan Help</h3>"+
"<p>Object Scan uses a patch sampling algorithm to detect intensity edges based on local gradient and uses the generated 2-dimensional masks to map objects. "+
"Mapped objects are then processed for 3-dimensional best-fit clustering to define the final object map, which "+"can be further analysed with one of the "+
"optional algorithms described below.</p><ul style=\"list-style-type:disc;\">"+
"<li style=\"list-style-type:none;\"><b>Scan Settings</b></li>"+
"<li><i>Scan radius</i> - the patch radius around each pixel to be sampled. Increasing this value improves edge mapping in noisy images, but causes "+
"steep gradients to be mapped less precisely. Small values are useful to analyse punctate signal or objects with sharply defined edges, large values "+
"are useful for \"blob detection\" from noisy signal or finding edges defined by a smooth intensity change.</li>"+
"<li><i>Edge gradient (change per pixel)</i> - the intensity gradient required across the patch to assign an edge, calculated as patch intensity difference per pixel.</li>"+
"<li style='list-style-type:none;'><i>Suggest</i> - use Grubb's test (Grubbs, 1969) to calculate the maximum absolute deviation from the mean as a suggested starting point for gradient optimisation.</li>"+
"<li><i>Estimated object diameter</i> - the approximate diameter of the objects to be mapped and measured.</li>"+
"<li style='list-style-type:none;'><i>Measure</i> - get the estimated object diameter from the current selection.</li>"+
"<li style=\"list-style-type:none;\"></li>"+
"<li style=\"list-style-type:none;\"><b>Analysis</b></li>"+
"<li><i>Standard</i> - no additional object map analysis.</li>"+
"<li><i>Surrounding Foci</i> - find small objects surrounding and contained by larger objects in the chosen primary channel, and output a summary table of foci per primary object. "+
"Requires a primary object channel (eg. a nuclear marker).</li> "+
"<li><i>Contained Foci</i> - find small objects contained by larger objects in the chosen primary channel. Requires a primary object channel (eg. a nuclear marker).</li> "+
"<li><i>Clustered Foci</i> - assign small objects as belonging to predicted larger objects within the set cluster radius. Useful when the data does not include a primary object marker.</li> "+
"<li><i>Object Count</i> - outputs the number of objects found to the log window to allow quick counting for multiple images.</li>"+
"<li><i>Object Intensity Correlation</i> - calculate Pearson's Correlation Coefficient (Barlow et al., 2010), Manders Overlap Coefficient (Manders et al., 1993) and Li's Intensity Correlation Quotient (Li et al., 2004) for each mapped "+
"object in comparison to the same volume in the other channels. Draws scatter plots of pixel intensities for each roi measured before clustering.</li> "+
"<li><i>Object Colocalisation</i> - compares objects in all channels and outputs a table of counts per channel and number of colocalised objects in each other channel. The set cluster radius "+
"defines the maximum Euclidean distance between object centroids for them to be classified as \"colocalised\".</li>"+
"<li><i>Contained Signal</i> - maps objects in the container channel and uses the mask for measurement in all channels.</li>"+
"<li><i>Track</i> - track objects over time by assigning objects that are close enough at adjacent time points to the same lineage. Outputs mean speed for each lineage to the log window "+
"and draws tails showing the movement of each object over the number of previous frames set as tail length. This is a simple algorithm intended for tracking of small numbers of continuously mapped objects, "+
"use TrackMate for more complex tracking analysis.</li>"+
"<li><i>Rainbow</i> - specific to images of the Rainbow mouse line. Maps objects in colour space and uses k-means clustering to define populations.</li>"+
"<li style=\"list-style-type:none;\"></li>"+
"<li style=\"list-style-type:none;\"><b>Processing</b></li>"+
"<li><i>None</i> - no processing will be applied before object detection.</li>"+
"<li><i>DoG</i> - a Difference of Gaussians filter where K=4 and using the expected object radius to calculate kernel sigma values.</li> "+
"<li><i>LoG</i> - a DoG filter where K=1.6 to approximate a Laplacian of Gaussian. </li> "+
"<li><i>Canny</i> - Tom Gibara's Canny edge detector algorithm. </li> "+
"<li style=\"list-style-type:none;\"></li>"+
"<li style=\"list-style-type:none;\"><b>Segmentation</b></li>"+
"<li><i>None</i> - no segmentation of overlapping objects.</li> "+
"<li><i>Watershed</i> - the watershed algorithm (Vincent and Soille, 1991) implemented in ImageJ by Christopher Mei.</li> "+
"<li><i>Circular Morphology</i> - fits circular masks inside the detected intensity edges to segment predicted circular/spherical objects independent of their intensity. </li> "+
"<li><i>Region Growing</i> - iterative 8-connected region growing based on pixel intensity values which progressively expands a boundary from ultimate eroded points. "+
"Produces an object map which is extended as far as possible into continuous regions of signal.</li> "+
"<li style=\"list-style-type:none;\"></li>"+
"<li style=\"list-style-type:none;\"><b>Additional Options</b></li>"+
"<li><i>Exclude objects touching image edges</i> - do not map or measure any object that includes pixels on the edge of the image.</li>"+
"<li><i>Convex hull map</i> - map objects as convex hulls. Useful for generating a regular map for blob-like objects, but removes concave edge features.</li>"+
"<li><i>Fast Scan</i> - scan using a downsampled version of the image. Significantly faster, but gives a less accurate edge map and often detects different areas as objects due to the loss of resolution. </li>"+
"<li><i>Save Settings</i> - save the current settings.</li>"+
"<li><i>Export Settings</i> - write the current settings to the log window.</li>"+
"<li><i>Overlay Control</i> - open the overlay control for an existing Object Scan overlay without analysing the image. This window will open automatically when an analysis is complete.</li>"+
"</ul>"+
"</p>"+
"<p style=\"font-size:10pt;font-color:grey;text-align:center;\">Object Scan v2.35<br>by Richard Butler, Gurdon Institute Imaging Facility</p>"+
"</body>"+
"</html>";

public void savePreferences(){
Prefs.set("object_scan.box.sigma",sigma);
Prefs.set("object_scan.box.varreq",varreq);Prefs.set("object_scan.box.estd",estd);Prefs.set("object_scan.box.analysis",analysis);
Prefs.set("object_scan.box.clusr",clusr);Prefs.set("object_scan.box.prim",prim);Prefs.set("object_scan.box.primgrad",primgrad);
Prefs.set("object_scan.box.exedge",exedge);Prefs.set("object_scan.box.nos",nos);Prefs.set("object_scan.box.fill",fill);
Prefs.set("object_scan.conv",conv);Prefs.set("object_scan.downs",downs);
Prefs.set("object_scan.box.view3d",view3d);Prefs.set("object_scan.box.pronone",pronone);Prefs.set("object_scan.box.prodog",prodog);
Prefs.set("object_scan.box.prolog",prolog);Prefs.set("object_scan.box.procanny",procanny);
Prefs.set("object_scan.box.noseg",noseg);Prefs.set("object_scan.box.wat",wat);
Prefs.set("object_scan.box.circseg",circseg);Prefs.set("object_scan.box.grow",grow);Prefs.set("object_scan.box.slice",slice);
Prefs.set("object_scan.box.taillength",taillength);Prefs.set("object_scan.box.colourset",colourset);Prefs.set("object_scan.box.outlineset",outlineset);
Prefs.set("object_scan.box.primproc",primproc);
}

public void exportPreferences(){
try{
String setString = 
"Scan Radius: \t"+sigma+"\n"+
"Edge Gradient: \t"+varreq+"\n"+
"Estimated Object Diameter: \t"+estd+" "+unit+"\n"+
"Analysis: \t"+analysis+"\n"+
((analysis=="Clustered Foci"||analysis=="Object Colocalisation"||analysis=="Track")?"Cluster Radius: \t"+clusr+" "+unit+"\n":"")+
((analysis=="Contained Foci"||analysis=="Surrounding Foci"||analysis=="Contained Signal")?"Primary Channel: \t"+prim+"\n":"")+
((analysis=="Contained Foci"||analysis=="Surrounding Foci")?"Primary Gradient: \t"+primgrad+"\nPrimary Processing: \t"+primproc+"\n":"")+
((analysis=="Track")?"Tail Length: \t"+taillength+" frames"+"\n":"")+
"Processing: \t"+(prodog?"DoG":prolog?"LoG":procanny?"Canny":"None")+"\n"+
"Segmentation: \t"+(wat?"Watershed":circseg?"Morphological":grow?"Region Growing":"None")+"\n"+
"Exclude Objects touching Image Edges: \t"+exedge+"\n"+
"Convex hull map: \t"+conv+"\n"+
"Fast scan: \t"+downs+"\n"+
"Colours: \t"+colourset+"\n"+
"Lines: \t"+outlineset ;
IJ.log(setString);
}catch(Exception e){IJ.log(e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}}

public void makeDraggable(Component passComp,Container passTarget){
	final Component comp = passComp;
	final Container target = passTarget;
	MouseAdapter ma = new MouseAdapter(){
        public void mousePressed(MouseEvent me){  
        clickX = me.getX();clickY= me.getY();
        }
	public void mouseDragged(MouseEvent me){
	target.setLocation(target.getLocation().x+me.getX()-clickX,target.getLocation().y+me.getY()-clickY);
	}
	};
comp.addMouseListener(ma);
comp.addMouseMotionListener(ma);
}

}
