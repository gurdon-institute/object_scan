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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Format;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.StackStatistics;

public class object_scan implements PlugIn, ActionListener{
	private OSbox box = new OSbox();
	private OSprocess processor = new OSprocess();
	private OScoloc co = new OScoloc();
	private OSutil util = new OSutil();
	private OSoverlayControl ovc;
	private ImagePlus imp,copy;
	private int stackindex,start,end,thresh,clickX,clickY,Z,T,C,bd,sc,sz,st,sw,sh,nobj,focicount;
	private boolean prev,stop;
	private String title,objectstitle;
	private double minr,current,x1,y1,z1,t1,c1,a1,x2,y2,z2,t2,a2,c2,xcal,ycal,zcal,maxindex,
	tcal,thisx,thisy,thisz,thist,thisc,thisindex,thism,thisa,index1,range,rangeAlt,ed,mes;
	private ResultsTable coloctable;
	private JFrame gui,helpFrame,active;
	private JCheckBox ckbxExedge,ckbxConv,ckbxDowns;
	private JComboBox<String> analysisBox,colourBox,outlineBox,cmbPrimproc;
	private JLabel labSigma,labVarreq,labEstd,labPrim,labPrimgrad,labTaillength,labClusr,labPrimproc;
	private JFormattedTextField txtSigma,txtVarreq,txtEstd,txtPrim,txtPrimgrad,txtTaillength,txtClusr;
	private JPanel panClus,panPrim,panPrimProc,panTail;
	private final Format intFormat = new java.text.DecimalFormat("#");
	private final Format doubleFormat = new java.text.DecimalFormat("###.###");
	private JRadioButton RBpronone, RBprodog, RBprolog, RBprocanny, RBsegnone, RBsegwat, RBsegcirc, RBseggrow;
	private Overlay ol = new Overlay();
	private final Image logoimage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_borded_336x104.gif"));
	private final Image iconimage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_icon.gif"));
	private static final int downscale = 3;

	public void visibility(){
		box.analysis = (String) analysisBox.getSelectedItem();
		if(box.analysis=="Standard"){			panClus.setVisible(false);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Surrounding Foci"){	panClus.setVisible(false);panPrim.setVisible(true);panPrimProc.setVisible(true);panTail.setVisible(false);	}
		else if(box.analysis=="Contained Foci"){	panClus.setVisible(false);panPrim.setVisible(true);panPrimProc.setVisible(true);panTail.setVisible(false);	}
		else if(box.analysis=="Clustered Foci"){	panClus.setVisible(true);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Object Count"){		panClus.setVisible(false);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Object Colocalisation"){	panClus.setVisible(true);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Contained Signal"){	panClus.setVisible(false);panPrim.setVisible(true);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Rainbow"){		panClus.setVisible(false);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		else if(box.analysis=="Track"){			panClus.setVisible(true);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(true);	}
		else if(box.analysis=="Object Intensity Correlation"){	panClus.setVisible(false);panPrim.setVisible(false);panPrimProc.setVisible(false);panTail.setVisible(false);	}
		gui.pack();
	}

	public void run(String arg){
		try{
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}catch(Exception e1){
			try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception e2){IJ.error("L+F failure");}
		}
		if(WindowManager.getImageCount()==0){IJ.error("No Image","There are no images open.");return;}
		imp = WindowManager.getCurrentImage();
		bd = imp.getBitDepth();
		if(bd==24){IJ.error("Error","24-bit RGB images are not supported, try converting to greyscale or to an RGB stack");return;}
		C = imp.getNChannels();
		T = imp.getNFrames();
		Z = imp.getNSlices();
		xcal = imp.getCalibration().pixelWidth;
		ycal = imp.getCalibration().pixelHeight;
		zcal = imp.getCalibration().pixelDepth;
		tcal = imp.getCalibration().frameInterval;
		if(tcal==0){tcal=1;}
		minr=minr*xcal;
		title = imp.getTitle();
		stackindex = imp.getCurrentSlice();

		box.varreq = (int) Prefs.get("object_scan.box.varreq",10);
		box.primgrad = box.varreq;
		box.unit = imp.getCalibration().getUnit();
		if(box.unit.matches("[Mm]icro.*")){box.unit="\u00B5m";}

		//call menu commands to setup options
		IJ.run("Set Measurements...", "area mean standard centroid stack display redirect=None decimal=2");
		IJ.run("Overlay Options...", "stroke=cyan width=1 fill=none apply set");	//set later, needed here?
		IJ.run(imp,"Labels...", "color=white font=12");
		IJ.run(imp, "Options...", "iterations=1 count=1 black edm=Overwrite");	//binary options
		IJ.run("Misc...", "divide=Infinity run");

		gui = new JFrame();
		gui.setLocation((int)Math.round((Toolkit.getDefaultToolkit().getScreenSize().getWidth()-350)/2),100);
		gui.getContentPane().setBackground(OSbox.backcol);
		((JComponent)gui.getContentPane()).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		gui.setLayout(new BoxLayout(gui.getContentPane(),BoxLayout.Y_AXIS));
		gui.setFont(OSbox.bigfont);
		gui.setResizable(false);
		gui.setUndecorated(true);
		ImageIcon logo = new ImageIcon(logoimage);
		new ImageIcon(iconimage);
		JLabel logolabel = new JLabel(logo);
		JPanel logoPanel = util.paneller(logolabel,null);
		gui.add(logoPanel);
		gui.setIconImage(iconimage);
		labSigma = util.fieldlabel("Scan radius (pixel):");
		txtSigma = util.numberfield(intFormat,Prefs.get("object_scan.box.sigma",1),3);
		gui.add(util.paneller(labSigma,txtSigma));
		labVarreq = util.fieldlabel("Edge gradient (change per pixel):");
		txtVarreq = util.numberfield(intFormat,Prefs.get("object_scan.box.varreq",10),4);
		gui.add(util.paneller(util.paneller(labVarreq,txtVarreq),util.buttoner("suggest",OSutil.SMALL,this)));
		labEstd = util.fieldlabel("Estimated object diameter ("+box.unit+"):");
		txtEstd = util.numberfield(doubleFormat,Prefs.get("object_scan.box.estd",1),5);
		gui.add(util.paneller(util.paneller(labEstd,txtEstd),util.buttoner("measure",OSutil.SMALL,this)));
		analysisBox = new JComboBox<String>(OSbox.analysischoice);
		analysisBox.setMaximumRowCount(15);
		analysisBox.setSelectedItem(Prefs.get("object_scan.box.analysis","Standard"));
		analysisBox.addActionListener(this);
		gui.add(util.paneller(new JLabel("Analysis: "),analysisBox));
		labClusr = util.fieldlabel("Cluster Radius ("+box.unit+"):");
		txtClusr = util.numberfield(doubleFormat,Prefs.get("object_scan.box.clusr",10d*xcal),4);
		panClus = util.paneller(labClusr,txtClusr);
		gui.add(panClus);
		labPrim = util.fieldlabel("Primary Channel:");
		txtPrim = util.numberfield(intFormat,Prefs.get("object_scan.box.prim",1),1);
		labPrimgrad = util.fieldlabel("Primary Gradient:");
		txtPrimgrad = util.numberfield(intFormat,Prefs.get("object_scan.box.primgrad",10),5);
		panPrim = util.paneller(util.paneller(labPrim,txtPrim),util.paneller(labPrimgrad,txtPrimgrad));
		gui.add(panPrim);
		labPrimproc = new JLabel("Primary Processing:");
		cmbPrimproc = new JComboBox<String>(new String[]{"None","DoG","LoG","Canny"});
		cmbPrimproc.setSelectedItem((Object)Prefs.get("object_scan.box.primproc","None"));
		cmbPrimproc.addActionListener(this);
		cmbPrimproc.setBorder(BorderFactory.createMatteBorder(5,2,0,2,OSbox.backcol));
		panPrimProc = util.paneller(labPrimproc,cmbPrimproc);
		gui.add(panPrimProc);
		labTaillength = util.fieldlabel("Tail (frames):");
		txtTaillength = util.numberfield(intFormat,Prefs.get("object_scan.box.taillength",3),2);
		panTail = util.paneller(labTaillength,txtTaillength);
		gui.add(panTail);
		JPanel pro = new JPanel(new GridLayout(1,4,2,2));
		pro.setBackground(OSbox.backcol);
		pro.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),"Processing",0,0,OSbox.bigfont,OSbox.frontcol));
		box.pronone = Prefs.get("object_scan.box.pronone",true); box.prodog = Prefs.get("object_scan.box.prodog",false);
		box.prolog = Prefs.get("object_scan.box.prolog",false); box.procanny = Prefs.get("object_scan.box.procanny",false);
		RBpronone = new JRadioButton("None", box.pronone);RBpronone.setBackground(OSbox.backcol);RBpronone.setForeground(OSbox.frontcol);RBpronone.setActionCommand("pronone");
		RBprodog = new JRadioButton("DoG", box.prodog);RBprodog.setBackground(OSbox.backcol);RBprodog.setForeground(OSbox.frontcol);
		RBprolog = new JRadioButton("LoG", box.prolog);RBprolog.setBackground(OSbox.backcol);RBprolog.setForeground(OSbox.frontcol);
		RBprocanny = new JRadioButton("Canny", box.procanny);RBprocanny.setBackground(OSbox.backcol);RBprocanny.setForeground(OSbox.frontcol);
		ButtonGroup RBprogroup = new ButtonGroup();
		RBprogroup.add(RBpronone);RBprogroup.add(RBprodog);RBprogroup.add(RBprolog);RBprogroup.add(RBprocanny);
		RBpronone.addActionListener(this);RBprodog.addActionListener(this);RBprolog.addActionListener(this);RBprocanny.addActionListener(this);
		pro.add(RBpronone);pro.add(RBprodog);pro.add(RBprolog);pro.add(RBprocanny);
		gui.add(pro);
		JPanel pan = new JPanel(new GridLayout(2,2,2,2));
		pan.setBackground(OSbox.backcol);
		pan.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),"Segmentation",0,0,OSbox.bigfont,OSbox.frontcol));
		box.noseg = Prefs.get("object_scan.box.noseg",true);box.wat = Prefs.get("object_scan.box.wat",false);
		box.circseg = Prefs.get("object_scan.box.circseg",false);box.grow = Prefs.get("object_scan.box.grow",false);
		RBsegnone = new JRadioButton("None", box.noseg);RBsegnone.setBackground(OSbox.backcol);RBsegnone.setForeground(OSbox.frontcol);
		RBsegwat = new JRadioButton("Watershed", box.wat);RBsegwat.setBackground(OSbox.backcol);RBsegwat.setForeground(OSbox.frontcol);
		RBsegcirc = new JRadioButton("Circular Morphology", box.circseg);RBsegcirc.setBackground(OSbox.backcol);RBsegcirc.setForeground(OSbox.frontcol);
		RBseggrow = new JRadioButton("Region Growing", box.grow);RBseggrow.setBackground(OSbox.backcol);RBseggrow.setForeground(OSbox.frontcol);
		ButtonGroup RBgroup = new ButtonGroup();
		RBgroup.add(RBsegnone);RBgroup.add(RBsegwat);RBgroup.add(RBsegcirc);RBgroup.add(RBseggrow);
		RBsegnone.addActionListener(this);RBsegwat.addActionListener(this);RBsegcirc.addActionListener(this);RBseggrow.addActionListener(this);
		pan.add(RBsegnone);pan.add(RBsegwat);pan.add(RBsegcirc);pan.add(RBseggrow);
		gui.add(pan);
		JPanel tickPan = new JPanel();
		tickPan.setLayout(new GridLayout(2,1,2,2));
		ckbxExedge = new JCheckBox("Exclude objects touching image edges", Prefs.get("object_scan.box.exedge",true));
		ckbxExedge.setBackground(OSbox.backcol);
		ckbxExedge.addActionListener(this);
		tickPan.add(util.paneller(new JLabel(""),ckbxExedge));
		ckbxConv = new JCheckBox("Convex hull map",Prefs.get("object_scan.conv",false));
		ckbxConv.setBackground(OSbox.backcol);
		ckbxConv.addActionListener(this);
		ckbxDowns = new JCheckBox("Fast scan",Prefs.get("object_scan.downs",false));
		ckbxDowns.setBackground(OSbox.backcol);
		ckbxDowns.addActionListener(this);
		tickPan.add(util.paneller(ckbxConv,ckbxDowns));
		tickPan.setBackground(OSbox.backcol);
		gui.add(tickPan);
		colourBox = new JComboBox<String>(OSbox.colourchoice);
		colourBox.setSelectedItem(Prefs.get("object_scan.box.colourset","Bold"));
		colourBox.addActionListener(this);
		outlineBox = new JComboBox<String>(OSbox.outlinechoice);
		outlineBox.setSelectedItem(Prefs.get("object_scan.box.outlineset","Thin"));
		outlineBox.addActionListener(this);
		gui.add(util.paneller(util.paneller(new JLabel("Colours:"),colourBox),util.paneller(new JLabel("Lines:"),outlineBox)));
		gui.add(Box.createRigidArea(new Dimension(300,6)));
		JPanel buttonpan1 = new JPanel(new GridLayout(0,3,6,6));
		buttonpan1.setBackground(OSbox.backcol);
		buttonpan1.add(util.buttoner("Save Settings",OSutil.SMALL,this));
		buttonpan1.add(util.buttoner("Export Settings",OSutil.SMALL,this));
		buttonpan1.add(util.buttoner("Help",OSutil.SMALL,this));
		buttonpan1.add(util.buttoner("Preview",OSutil.BIG,this));
		buttonpan1.add(util.buttoner("Clear Preview",OSutil.SMALL,this));
		buttonpan1.add(util.buttoner("Overlay",OSutil.SMALL,this));
		gui.add(buttonpan1);
		gui.add(Box.createRigidArea(new Dimension(300,12)));
		JPanel buttonpan2 = new JPanel(new GridLayout(0,2,6,6));
		buttonpan2.setBackground(OSbox.backcol);
		buttonpan2.add(util.buttoner("OK",OSutil.BIG,this));
		buttonpan2.add(util.buttoner("Cancel",OSutil.BIG,this));
		gui.add(buttonpan2);
		box.makeDraggable(gui,gui);
		gui.pack();gui.setVisible(true);
		visibility();
		util.colourise(imp,box);
	}//run

	public void actionPerformed(ActionEvent event){
		String e = event.getActionCommand();
		try{
			box.sigma=	Integer.valueOf(txtSigma.getText());
			box.varreq=	Integer.valueOf(txtVarreq.getText());
			box.estd=	Double.valueOf(txtEstd.getText());
			box.prim=	Integer.valueOf(txtPrim.getText());
			box.primproc=	(String)cmbPrimproc.getSelectedItem();
			if((box.prim>C)||(box.prim<1)){
				box.prim=1;
				txtPrim.setText("1");
				IJ.showMessage("Error","Primary channel doesn't exist, using channel 1.");
			}
			box.primgrad=	Integer.valueOf(txtPrimgrad.getText());
			box.taillength=	Integer.valueOf(txtTaillength.getText());
			box.clusr=	Double.valueOf(txtClusr.getText());
			visibility();
			box.pronone=RBpronone.isSelected();box.prodog=RBprodog.isSelected();box.prolog=RBprolog.isSelected();box.procanny=RBprocanny.isSelected();
			box.noseg=RBsegnone.isSelected();box.wat=RBsegwat.isSelected();box.circseg=RBsegcirc.isSelected();box.grow=RBseggrow.isSelected();

			box.exedge = (boolean) ckbxExedge.isSelected();
			box.conv = (boolean) ckbxConv.isSelected();
			box.downs = (boolean) ckbxDowns.isSelected();
			box.colourset = (String) colourBox.getSelectedItem();
			box.outlineset = (String) outlineBox.getSelectedItem();
			util.colourise(imp,box);
		}catch(Exception except){IJ.log(except.getStackTrace()[3].toString()+System.getProperty("line.separator")+" (actionPerformed) "+except.toString());}
		if(e=="OK"){
			try{
				box.savePreferences();
				prev=false;
				start=1;
				end=imp.getStackSize();
				gui.dispose();
				Thread worker = new Thread(){public void run(){	scan(imp); }};
				SwingUtilities.invokeLater(new Runnable(){public void run(){ activeIndicator();	}});
				worker.start();
			}catch(Exception except){IJ.log(except.getStackTrace()[0].toString()+System.getProperty("line.separator")+" OKed "+except.toString());}
		}
		else if(e=="Preview"){
			try{
				prev=true;
				start=imp.getCurrentSlice();
				end=imp.getCurrentSlice();
				Thread worker = new Thread(){public void run(){
					scan(imp);
				}};
				SwingUtilities.invokeLater(new Runnable(){public void run(){
					activeIndicator();
				}});
				worker.start();
			}catch(Exception except){IJ.log(except.getStackTrace()[0].toString()+System.getProperty("line.separator")+" OKed "+except.toString());}
		}
		else if(e=="Cancel"){
			gui.dispose();
		}
		else if(e=="Help"){
			try{
				helpFrame = new JFrame();
				helpFrame.setUndecorated(true);
				helpFrame.setLocation(((int)Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2)-500,10);
				Container content = helpFrame.getContentPane();
				content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
				content.setBackground(Color.WHITE);
				JEditorPane helpPane = new JEditorPane("text/html","");
				helpPane.setText(OSbox.help);
				helpPane.setEditable(false);
				box.makeDraggable(helpPane,helpFrame);
				JScrollPane scroll = new JScrollPane(helpPane);
				scroll.setBackground(Color.WHITE);
				scroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2,2,2,2),BorderFactory.createLineBorder(Color.BLACK,2)));
				content.add(scroll);
				JPanel footer = util.paneller(util.buttoner("Close",OSutil.BIG,this),null);
				footer.setBackground(Color.WHITE);
				content.add(footer);
				box.makeDraggable(helpFrame,helpFrame);
				helpFrame.pack();
				helpFrame.setVisible(true);
			}catch(Exception except){IJ.log(except.getStackTrace()[0].toString()+System.getProperty("line.separator")+" help "+except.toString());}
		}
		else if(e=="Close"){helpFrame.dispose();}
		else if(e=="Clear Preview"){
			imp.setOverlay(null);
			IJ.showStatus("");
		}
		else if(e=="Save Settings"){box.savePreferences();}
		else if(e=="Export Settings"){box.exportPreferences();}
		else if(e=="-"){active.setState(Frame.ICONIFIED);}
		else if(e=="Stop"){
			stop = true;
			active.dispose();
		}
		else if(e=="Overlay"){
			if((imp.getOverlay()==null)||(imp.getOverlay().size()==0)){IJ.error("No Overlay","No overlay found.");return;}
			try{
				if(ovc!=null){	ovc.control.dispose();	}
				ovc = new OSoverlayControl();
				ovc.show(imp,imp.getOverlay(),box.colours1,box.colours2,box);
				gui.dispose();
			}catch(Exception except){IJ.log(except.getStackTrace()[0].toString()+System.getProperty("line.separator")+except.toString());}
		}
		else if(e=="suggest"){
			StackStatistics ss = new StackStatistics(imp);
			double num = Math.max(Math.abs(ss.max-ss.mean),Math.abs(ss.min-ss.mean));
			int estvar = (int)Math.round(num/ss.stdDev);	//Grubbs test - maximum absolute deviation
			txtVarreq.setText(String.valueOf(estvar));
		}
		else if(e=="measure"){
			if(imp.getRoi()==null){
				JOptionPane.showMessageDialog(gui,(Object)"Please make a selection to measure.","No Selection Found",JOptionPane.WARNING_MESSAGE);
				return;
			}
			else if(imp.getRoi().getType()==Roi.LINE){
				mes = imp.getRoi().getLength();	
			}
			else{
				mes = Math.max(imp.getRoi().getBounds().width,imp.getRoi().getBounds().height)*xcal;
			}
			txtEstd.setText(IJ.d2s(mes,3));
		}
	}//action listener

	public MouseAdapter maas(){
		MouseAdapter ma = new MouseAdapter(){
			public void mousePressed(MouseEvent me){  
				clickX = me.getX();clickY= me.getY();
			}
			public void mouseDragged(MouseEvent me){
				active.setLocation(active.getLocation().x+me.getX()-clickX,active.getLocation().y+me.getY()-clickY);
			}
		};
		return ma;
	}

	public void activeIndicator(){
		try{
			String tasklabel = (prev==false)?"Scanning...   ":"Previewing...";
			active = new JFrame(tasklabel);
			active.setBounds((Toolkit.getDefaultToolkit().getScreenSize().width/2)-(OSbox.activeW/2),(Toolkit.getDefaultToolkit().getScreenSize().height/4),OSbox.activeW,OSbox.activeH);
			active.setPreferredSize(new Dimension(OSbox.activeW,OSbox.activeH));
			active.setAlwaysOnTop(true);
			active.getContentPane().setBackground(new Color(0,0,0));
			JComponent pane = (JComponent)active.getContentPane();
			pane.setBorder(BorderFactory.createLineBorder(OSbox.brightcol,2));
			active.setUndecorated(true);
			active.setLayout(new FlowLayout(1,2,2));
			JPanel header = new JPanel();
			header.setLayout(new FlowLayout(2,2,2));
			header.setPreferredSize(new Dimension(OSbox.activeW-4,30));
			header.setBackground(new Color(0,0,0));
			JLabel scantitle = new JLabel(tasklabel,2); scantitle.setFont(OSbox.biggerfont); scantitle.setForeground(OSbox.brightcol);
			header.add(scantitle);
			JButton mini = util.buttoner("-",OSutil.BIG,this);
			mini.setMargin(new Insets(0,0,0,0));
			mini.setPreferredSize(new Dimension(20,20));
			header.add(mini);
			active.add(header);
			java.net.URL url = this.getClass().getResource("scan.gif");
			ImageIcon img = new ImageIcon(Toolkit.getDefaultToolkit().createImage(url));
			JLabel icon = new JLabel(img);
			icon.setIcon(img);
			active.add(icon);
			active.add(util.buttoner("Stop",OSutil.MED,this));
			box.makeDraggable(active,active);
			active.pack();
			active.setVisible(true);
			active.paintAll(active.getGraphics());
		}catch(Exception e){IJ.log(e.toString()+System.getProperty("line.separator")+e.getStackTrace()[0].toString());}
	}

	@SuppressWarnings("deprecation")
	public int scan(ImagePlus imp){	//main scan method
		try{	//main try
			if(WindowManager.getImageCount()==0){IJ.error("No Image","There was an image,\nIt is gone like spring blossom,\nScattered by the wind.");return 0;}
			if((imp.getNChannels()!=C)||(imp.getNSlices()!=Z)||(imp.getNFrames()!=T)){
				IJ.error("Image Changed","The image has changed,\nThe river after a flood,\nFinding a new course.");
				imp = WindowManager.getCurrentImage();
				bd = imp.getBitDepth();
				if(bd==(int)24){
					IJ.error("Error","24-bit RGB images are not supported, try converting to greyscale or to an RGB stack");
					return 0;
				}
				/* C = imp.getNChannels();
T = imp.getNFrames();
Z = imp.getNSlices(); */
				if(prev==true){	start = imp.getCurrentSlice();  end = imp.getCurrentSlice();    }
				else{	       	start = 1;			end = imp.getStackSize();	}
			}

			imp.setOverlay(null);
			ol = new Overlay();
			IJ.run(imp, "Select None", "");
			IJ.run("Remove Overlay", "");

			if((prev==false)&&(C<2)&&((box.analysis=="Contained Foci")||(box.analysis=="Object Colocalisation")||(box.analysis=="Contained Signal")||(box.analysis=="Object Intensity Correlation"))){
				IJ.error("Error","Not enough channels in "+title+" for "+box.analysis+" analysis.");
				return 0;
			}
			if((prev==false)&&(box.analysis=="Rainbow")&&(C!=3)){	IJ.error("Error","Rainbow analysis requires three channels");return 0;}
			if((prev==false)&&(box.analysis=="Surrounding Foci")&&(C<2)){	IJ.error("Error","Surrounding Foci analysis requires at least two channels");return 0;}
			if((prev==false)&&(box.analysis=="Track")){
				if(T<=1){IJ.error("Error","Not enought timepoints in "+title+" for object tracking analysis.");return 0;}
				if(box.exedge==false){
					if(IJ.showMessageWithCancel("Warning","The Track algorithm may be confused if objects on image edges are included.\nContinue anyway?")==false){return 0;}
				}
			}

			if((box.analysis=="Rainbow")&&(prev==false)){	//add artificial channel for object mapping
				ImagePlus dup = new Duplicator().run(imp, 1, C, 1, Z, 1, T);
				IJ.run(dup, "Make Composite", "");
				IJ.run(dup, "RGB Color", "slices");
				IJ.run(dup, bd+"-bit", "");
				dup.setTitle("Cmax");
				dup.show();
				IJ.run(imp, "Split Channels", "");
				IJ.run(imp, "Merge Channels...", "c1=[C1-"+title+"] c2=[C2-"+title+"] c3=[C3-"+title+"] c4=[Cmax] create");
				imp = WindowManager.getCurrentImage();
				imp.setTitle(title);
				new ij.macro.MacroRunner("Stack.setDisplayMode(\"color\")").run();
				end = imp.getStackSize();
			}
			stop = false;

			for(int s=start;s<=end;s++){	/////////////////////////////////////////////////////////////////////////main stack loop
				if(stop==true){return 0;}
				IJ.showStatus("Running Object Scan...");
				imp.setPosition(s);
				sc = imp.getC();
				sz = imp.getZ();
				st = imp.getT();
				sw = imp.getWidth();
				sh = imp.getHeight();
				if(((box.analysis=="Contained Foci")||(box.analysis=="Surrounding Foci"))){ //different thresh, minr and processing for primary channel
					if(sc==box.prim){	//use the values from separate primary channel controls
						thresh = box.primgrad;
						box.primproc = (String)cmbPrimproc.getSelectedItem();
						if(box.primproc=="None"){box.pronone=true;box.prodog=false;box.prolog=false;box.procanny=false;}
						else if(box.primproc=="DoG"){box.pronone=false;box.prodog=true;box.prolog=false;box.procanny=false;}
						else if(box.primproc=="LoG"){box.pronone=false;box.prodog=false;box.prolog=true;box.procanny=false;}
						else if(box.primproc=="Canny"){box.pronone=false;box.prodog=false;box.prolog=false;box.procanny=true;}
						if(box.analysis=="Surrounding Foci"){minr=box.estd*5.0;}
						else{minr = box.estd*2.0;}
					}
					else{	//reset to the standard control values
						thresh = box.varreq;
						minr = box.estd/4.0;
						box.prodog=RBprodog.isSelected();box.prolog=RBprolog.isSelected();box.procanny=RBprocanny.isSelected();
					}
				}
				else{						//calculate required variation per patch side from input per pixel value
					thresh = box.varreq;
					minr = box.estd/4.0; //set minimum object radius
				}
				if(box.procanny){thresh=1;}	//ignore gradient input if Canny used
				if((box.analysis=="Contained Signal")){	//dup primary channel only for object mapping
					imp = new Duplicator().run(imp, box.prim, box.prim, sz, sz, st, st);
				}
				if((box.analysis=="Rainbow")&&(prev==false)){	//use artificial 4th channel
					imp = new Duplicator().run(imp, 4, 4, sz, sz, st, st);
					box.prim=4;
				}
				else{imp = new Duplicator().run(imp, sc, sc, sz, sz, st, st);} //dup this slice only to replace imp
				if((box.grow==true)){copy = new Duplicator().run(imp, sc, sc, sz, sz, st, st);} //another copy of this slice
				WindowManager.setTempCurrentImage(imp); //initialises the image without displaying it
				imp.setTitle("scan_proc");
				IJ.run(imp, "Select None", "");

				imp = processor.doProcess(imp,box);

				if(box.downs==true){
					imp.setProcessor(imp.getProcessor().resize(sw/downscale,sh/downscale,false));
					sw = imp.getWidth();
					sh = imp.getHeight();
					thresh = thresh*downscale;	//also scale required energy threshold to compensate for intensity binning effect
				}

				RoiManager rm = new RoiManager(true); //don't show
				int[][] pixels = imp.getProcessor().getIntArray();
				Roi point;
				for(int y=0;y<sh;y++){
					for(int x=0;x<sw;x++){
						if(stop==true){return 0;}
						int N=0;int S=0;int E=0;int W=0; //init patch side vars
						for(int py=-box.sigma;py<=box.sigma;py++){	//patch sampling
							for(int px=-box.sigma;px<=box.sigma;px++){
								IJ.showStatus("Scanning...");
								int getX = x + px;
								int getY = y + py;
								if(getX<0){getX=0;}	//handle patch sampling over edges - take edge pixel value
								else if(getX>sw-1){getX=sw-1;}
								if(getY<0){getY=0;}
								else if(getY>sh-1){getY=sh-1;}
								//assign pixels to the side vars
								if(Math.abs(py)<=Math.ceil(box.sigma/2)){
									if(px<0)     {W+=pixels[getX][getY];}
									else if(px>0){E+=pixels[getX][getY];}
								}
								else if(Math.abs(px)<=Math.ceil(box.sigma/2)){
									if(py<0)     {N+=pixels[getX][getY];}
									else if(py>0){S+=pixels[getX][getY];}
								}
							}
						}//end patch sample
						double energy = (Math.abs(E-W)+Math.abs(N-S))/box.sigma*((2*Math.ceil(box.sigma/2))+1); //sum of side differences/patch area
						if(energy>thresh){								//does the local energy at this point exceed the required edge gradient?
							point = new Roi(x,y,1,1);
							rm.addRoi(point);
						}
					}
				}//pixel loops

				if(rm.getCount()==0){
					imp.changes = false;
					imp.close();
					imp = WindowManager.getImage(title);
					IJ.run(imp, "Select None", "");
					continue;
				}

				IJ.run(imp, "Select All", "");
				IJ.run(imp, "Clear", "slice");
				IJ.run(imp, "Select None", "");
				IJ.run("Colors...", "foreground=white background=black selection=yellow");

				rm.runCommand("fill");
				IJ.run(imp, "Make Binary", "");
				if(box.downs==true){
					imp.setProcessor(imp.getProcessor().resize(sw*downscale,sh*downscale,false));
				}
				if(imp.isInvertedLut()){IJ.run(imp, "Invert LUT", "");} //make sure LUT isn't inverted
				rm.runCommand("Deselect");
				rm.runCommand("Delete");
				IJ.run(imp, "Fill Holes", "");
				if(minr/xcal>1d){	//if the minimum object size is greater than 1 pixel, remove small noisy objects and tidy edges
					IJ.run(imp, "Erode", "");
					IJ.run(imp, "Remove Outliers...", "radius="+minr+" threshold=1 which=Bright");
					IJ.run(imp, "Dilate", "");
				}

				imp = processor.doSegment(imp,copy,box);

				rm = new RoiManager(false);
		
				if(imp.getStatistics().mean==0){	//10/05/17: ThresholdToSelection throws ArrayIndexOutOfBoundsException if binary image is empty
					continue;
				}
				IJ.run(imp, "Create Selection", "");
				if(imp.getRoi()==null){
					imp.changes = false;
					imp.close();
					imp = WindowManager.getImage(title);
					IJ.run(imp, "Select None", "");
					continue;
				}
				rm.addRoi(imp.getRoi());
				IJ.run(imp, bd+"-bit", "");

				imp.changes = false;
				imp.close();
				imp = WindowManager.getImage(title); //reset imp
				WindowManager.setTempCurrentImage(imp);
				IJ.run(imp, "Select None", "");

				rm.select(imp, 0);
				if(imp.getRoi().getType()==Roi.COMPOSITE){
					rm.runCommand("Split");
					rm.select(imp, 0);
					rm.runCommand("Delete");
				}

				imp.setHideOverlay(true);
				try{
					Color stroker = box.colours1[sc];
					imp.setPosition(s);
					IJ.run("Set Measurements...", "area mean standard centroid stack redirect=None decimal=2");
					for(int r=0;r<rm.getCount();r++){
						IJ.showStatus("Measuring objects...");
						rm.select(imp, r);
						if(box.conv==true){imp.setRoi(new PolygonRoi(imp.getRoi().getConvexHull(),Roi.POLYGON));}
						imp.getRoi().setStroke(box.outline);
						imp.getRoi().setStrokeColor(stroker);
						if(imp.getNDimensions()>3){imp.getRoi().setPosition((int)sc,(int)sz,(int)st);}
						else if((imp.getNDimensions()==3)&&(C>1)){imp.getRoi().setPosition((int)sc,(int)sz,(int)st);}
						else if(imp.getNDimensions()==3){imp.getRoi().setPosition((Z>T)?(int)sz:(C>T)?(int)sc:(int)st);}
						imp.setPosition((int)sc,(int)sz,(int)st);	//required in ImageJ 1.47n (~21/03/13)
						rm.runCommand("Update");
						if((imp.getRoi().getLength()>=2*Math.PI*minr)){  //if roi perimeter >= circumference of a circle with radius minr
							if((imp.getRoi().getBounds().getWidth()>=2*minr)||(imp.getRoi().getBounds().getHeight()>=2*minr)){ //if the width or the height of the roi >= 2*minr
								if((box.exedge==false)||((box.exedge==true)&&(imp.getRoi().getBounds().x>0)&&(imp.getRoi().getBounds().y>0)&&(imp.getRoi().getBounds().x+imp.getRoi().getBounds().width<imp.getWidth())&&(imp.getRoi().getBounds().y+imp.getRoi().getBounds().height<imp.getHeight()))){
									if(box.outlineset!="None"){ol.add(imp.getRoi());}
									if(prev==false){ //measure the roi if this isn't a preview
										IJ.run(imp, "Measure", "");
										ResultsTable.getResultsTable().setValue("Ch",ResultsTable.getResultsTable().getCounter()-1,sc);	//bug fix - record C position in 3D stacks without Z or T series
										if(box.analysis=="Object Intensity Correlation"){
											double[] pcc = co.PCC(imp,imp.getRoi());
											double[] moc = co.MOC(imp,imp.getRoi());
											double[] icq = co.ICQ(imp,imp.getRoi());
											for(int pc=1;pc<=C;pc++){
												ResultsTable.getResultsTable().setValue("PCC vs C"+pc,ResultsTable.getResultsTable().getCounter()-1,(pc!=sc)?pcc[pc]:1d);
												ResultsTable.getResultsTable().setValue("MOC vs C"+pc,ResultsTable.getResultsTable().getCounter()-1,(pc!=sc)?moc[pc]:1d);
												ResultsTable.getResultsTable().setValue("ICQ vs C"+pc,ResultsTable.getResultsTable().getCounter()-1,(pc!=sc)?icq[pc]:0.5d);
											}
										}
									}
								}
							}
						}
					}
				}catch(Exception e){throw(new Exception(e.getStackTrace()[0].toString()+System.getProperty("line.separator")+"(Roi measuring) "+e.toString()));}
				finally{IJ.run(imp, "Select None", "");}
			}//scan stack loop

			if(prev==true){IJ.showStatus("Showing Object Scan preview");}
			else{
				box.rt = ResultsTable.getResultsTable();
				box.rt.showRowNumbers(false);
				nobj = box.rt.getCounter();
				if(box.analysis=="Rainbow"){	//remove artificial mapping channel
					imp.setC(4);
					IJ.run(imp, "Delete Slice", "delete=channel");
				}

				IJ.showStatus("Mapping Scanned objects...");
				imp.setPosition(stackindex);

				for(int a=0;a<nobj;a++){		//give rows an initial index and cluster distance
					box.rt.setValue("Object",a,a+1);box.rt.setValue("Cluster Distance"+"("+box.unit+")",a,1d/0d);
					if(box.analysis=="Object Colocalisation"){	//set colocalisations for all channels to 0 (false)
						for(int h=1;h<=C;h++){box.rt.setValue("Coloc in Ch"+h+"?",a,0);}
					}
					if(box.analysis=="Track"){
						box.rt.setValue("Child",a,0);
						box.rt.setValue("Lineage",a,a+1);
						box.rt.setValue("Track Distance"+"("+box.unit+")",a,1d/0d);
					} 
				}

				if((Z==1)&&(C==1)&&(T>1)){
					box.rt.setHeading(box.rt.getColumnIndex("Slice"),"Frame");	//deprecated, workaround for frames->slices bug
				}

				for(int i=0;i<nobj;i++){ 	//clustering to assign 2D maps to 2D/3D objects
					if(stop==true){return 0;}
					index1 = box.rt.getValue("Object",i);
					x1 = box.rt.getValue("X",i);
					y1 = box.rt.getValue("Y",i);
					if(Z>1){z1=box.rt.getValue("Slice",i);}else{z1=1.0;}
					if(T>1){t1=box.rt.getValue("Frame",i);}else{t1=1.0;}
					if(C>1){c1=box.rt.getValue("Ch",i);}else{c1=1.0;}
					a1 = box.rt.getValue("Area",i);
					z1=z1*zcal;	//calibrate slice number to get z coordinate
					if(box.analysis=="Track"){box.rt.getValue("Track Distance"+"("+box.unit+")",i);}
					for(int j=0;j<nobj;j++){
						IJ.showStatus("Clustering Scanned Objects...");
						box.rt.getValue("Object",j);
						x2 = box.rt.getValue("X",j);
						y2 = box.rt.getValue("Y",j);
						if(Z>1){z2=box.rt.getValue("Slice",j);}else{z2=1.0;}
						if(T>1){t2=box.rt.getValue("Frame",j);}else{t2=1.0;}
						if(C>1){c2=box.rt.getValue("Ch",j);}else{c2=1.0;}
						a2 = box.rt.getValue("Area",j);
						z2=z2*zcal;
						ed = Math.sqrt(Math.pow((x1-x2),2)+Math.pow((y1-y2),2)+Math.pow(z1-z2,2));
						current = box.rt.getValue("Cluster Distance"+"("+box.unit+")",j);
						if(box.analysis=="Clustered Foci"){rangeAlt = box.clusr;} //chosen analysis clustering range
						else if(box.analysis=="Object Colocalisation"){rangeAlt = box.clusr;} //chosen analysis clustering range
						range = Math.max(Math.sqrt(a1/Math.PI),Math.sqrt(a2/Math.PI)); //largest of the two object radii
						if((box.analysis=="Clustered Foci")&&(i!=j)&&(t1==t2)&&(c1==c2)&&(ed<=rangeAlt)&&(ed<current)){//criteria for clustering analysis
							box.rt.setValue("Object",j,index1);
							box.rt.setValue("Cluster Distance"+"("+box.unit+")",j,ed);
						}
						else if((box.analysis=="Object Colocalisation")&&(i!=j)&&(t1==t2)&&(c1!=c2)&&(ed<=rangeAlt)){//criteria for being colocalised
							box.rt.setValue("Coloc in Ch"+IJ.d2s(c1,0)+"?",j,1);
						}
						if((i!=j)&&(t1==t2)&&(c1==c2)&&(ed<=range)&&(ed<current)){ //criteria for clustering normal objects
							box.rt.setValue("Object",j,index1);
							box.rt.setValue("Cluster Distance"+"("+box.unit+")",j,ed);
						}
					}
				}

				if((box.analysis=="Rainbow")||(box.analysis=="Contained Signal")){
					for(int i1=0;i1<box.rt.getCounter();i1++){
						x1 = box.rt.getValue("X",i1);
						y1 = box.rt.getValue("Y",i1);
						c1 = box.rt.getValue("Ch",i1);
						a1 = box.rt.getValue("Area",i1);
						for(int i2=0;i2<box.rt.getCounter();i2++){
							x2 = box.rt.getValue("X",i2);
							y2 = box.rt.getValue("Y",i2);
							c2 = box.rt.getValue("Ch",i2);
							a2 = box.rt.getValue("Area",i2);
							if((c1==box.prim)&&(c1!=c2)&&(x1==x2)&&(y1==y2)&&(a1==a2)){
								box.rt.setValue("Primary Object",i2,box.rt.getValue("Object",i1));
							}
						}
					}
				}

				String unique = "";
				String[] indarray;
				for(int i=0;i<nobj;i++){//reassign object indices to be incremental
					IJ.showStatus("Indexing Objects...");
					index1 = box.rt.getValue("Object",i);
					if(unique.matches(".*#"+Double.toString(index1)+"#.*")==false){
						unique = unique.concat("#"+Double.toString(index1)+"#");
					}
				}
				unique = unique.replace("##","-");
				unique = unique.replace("#","");
				indarray = unique.split("-");
				maxindex=0;
				for(int a=0;a<indarray.length;a++){
					for(int b=0;b<nobj;b++){
						IJ.showStatus("Indexing Objects...");
						indarray[a] = indarray[a].replace("-","");
						if(indarray[a].matches(Double.toString(box.rt.getValue("Object",b)))){
							thisindex = (double) a + 1.0;
							box.rt.setValue("Object",b,thisindex);
							maxindex = Math.max(maxindex,thisindex);
						}
					}
				}

				if(box.analysis=="Object Intensity Correlation"){
					co.labelise(box.rt,C,Z,T);
				}

				try{
					box.objects = new ResultsTable();
					box.objects.showRowNumbers(false);
					for(int i=1;i<=maxindex;i++){
						if(stop==true){return 0;}
						int total = 0;
						double objx = 0.0;double objy = 0.0;double objz = 0.0;double objt = 0.0;double objc = 0.0;double objm = 0.0;double obja=0.0;double objsd=0.0;
						for(int n=0;n<box.rt.getCounter();n++){
							IJ.showStatus("Analysing Object Map...");
							thisx = box.rt.getValue("X",n);
							thisy = box.rt.getValue("Y",n);
							thisz = (Z>1)?box.rt.getValue("Slice",n):1.0;
							thist = (T>1)?box.rt.getValue("Frame",n):1.0;
							thisc = (C>1)?box.rt.getValue("Ch",n):1.0;
							thisindex = box.rt.getValue("Object",n);
							thism = box.rt.getValue("Mean",n);
							thisa = box.rt.getValue("Area",n);
							double thissd = box.rt.getValue("StdDev",n);
							if(thisindex==(int)Math.round(i)){
								box.rt.setValue("Object",n,i);
								total++;
								objx += thisx;	objy += thisy;	objz += thisz;	objt = thist;	objc = thisc;
								objm += thism;	obja += thisa;
								objsd = objsd + Math.pow(thissd,2)+Math.pow(thism,2);
							}
						}
						if(total!=0){
							objx = objx/total;	//calculate means
							objy = objy/total;
							objz = (objz/total)*zcal;
							objm = objm/total;
							obja = obja*zcal;	//total area * voxel depth = volume
							objsd = Math.sqrt((objsd/total)-Math.pow(objm,2));	//estimate sd from total sd and mean
							box.objects.incrementCounter();
							box.objects.setValue("Object",box.objects.getCounter()-1,i);
							box.objects.setValue("X",box.objects.getCounter()-1,objx);
							box.objects.setValue("Y",box.objects.getCounter()-1,objy);
							box.objects.setValue("Z",box.objects.getCounter()-1,objz);
							box.objects.setValue("T",box.objects.getCounter()-1,objt);
							box.objects.setValue("C",box.objects.getCounter()-1,objc);
							box.objects.setValue("Mean",box.objects.getCounter()-1,objm);
							box.objects.setValue("StdDev",box.objects.getCounter()-1,objsd);
							if(box.analysis!="Clustered Foci"){box.objects.setValue("Volume"+" ("+box.unit+"\u00B3)",box.objects.getCounter()-1,obja);}
							if(box.analysis=="Clustered Foci"){
								for(int g=0;g<box.rt.getCounter();g++){
									if(i==box.rt.getValue("Object",g)){focicount++;}
								}
								box.objects.setValue("Foci Count",box.objects.getCounter()-1,focicount);
								focicount = 0;	//reset
							}
							if(box.analysis=="Track"){
								box.objects.setValue("Track Distance"+"("+box.unit+")",box.objects.getCounter()-1,1d/0d);
							}
						}//total!=0
					}//end index loop

					if((box.analysis=="Contained Signal")||(box.analysis=="Rainbow")){
						int OC = box.objects.getCounter();
						for(int outer=0;outer<OC;outer++){
							double outO = box.objects.getValue("Object",outer);
							double outX = box.objects.getValue("X",outer);
							double outY = box.objects.getValue("Y",outer);
							double outT = (T>1)?box.objects.getValue("T",outer):1d;
							double outC = box.objects.getValue("C",outer);
							for(int inner=0;inner<OC;inner++){
								double innX = box.objects.getValue("X",inner);
								double innY = box.objects.getValue("Y",inner);
								double innT = (T>1)?box.objects.getValue("T",inner):1d;
								double innC = box.objects.getValue("C",inner);
								double innM = box.objects.getValue("Mean",inner);
								if ((outC==box.prim)&&(innC!=box.prim)&&(outT==innT)&&(outX==innX)&&(outY==innY)){
									box.objects.setValue("C"+IJ.d2s(innC,0)+" mean",outer,innM);
									box.objects.setValue("C"+IJ.d2s(innC,0)+" StdDev",outer,box.objects.getValue("StdDev",inner));
									double changedO = box.objects.getValue("Object",inner);
									box.objects.setValue("Object",inner,outO);
									for(int i=0;i<box.rt.getCounter();i++){
										if(box.rt.getValue("Object",i)==changedO){
											box.rt.setValue("Object",i,outO);
										}
									}
								}
							}
						}
						for(int i=0;i<OC;i++){
							if((box.objects.getValue("C",i)!=box.prim)){box.objects.deleteRow(i);i--;OC--;}
						}
						box.objects.updateResults();
					}

					if(box.analysis=="Rainbow"){
						ol = new OSrainbow().run(imp,ol,box);
					}

					if((box.analysis=="Contained Foci")||(box.analysis=="Surrounding Foci")){
						int OC = box.objects.getCounter();
						double[] current = new double[OC+1]; java.util.Arrays.fill(current,9999999999d);
						for(int outer=0;outer<OC;outer++){
							IJ.showStatus("Analysing Object Relationships...");
							double outO = box.objects.getValue("Object",outer);
							double outX = box.objects.getValue("X",outer);
							double outY = box.objects.getValue("Y",outer);
							double outZ = (Z>1)?box.objects.getValue("Z",outer):1d;
							double outT = (T>1)?box.objects.getValue("T",outer):1d;
							double outC = box.objects.getValue("C",outer);
							double outV = box.objects.getValue("Volume"+" ("+box.unit+"\u00B3)",outer);
							if(outC==box.prim){
								range = Math.cbrt((3*outV)/(4*Math.PI)); 	//radius of a sphere with volume outV
								if(box.analysis=="Surrounding Foci"){ range = 1.5*range;}	//radius for Silvia - add interface option?
								for(int inner=0;inner<OC;inner++){
									double innO = box.objects.getValue("Object",inner);
									double innX = box.objects.getValue("X",inner);
									double innY = box.objects.getValue("Y",inner);
									double innZ = (Z>1)?box.objects.getValue("Z",inner):1d;
									double innT = (T>1)?box.objects.getValue("T",inner):1d;
									double innC = box.objects.getValue("C",inner);
									box.objects.getValue("Volume"+" ("+box.unit+"\u00B3)",inner);
									double ED = Math.sqrt(Math.pow((outX-innX),2)+Math.pow((outY-innY),2)+Math.pow(outZ-innZ,2));
									if((ED<=range)&&(ED<current[inner])&&(innC!=box.prim)&&(outT==innT)&&(outO!=innO)){
										current[inner] = ED;
										box.objects.setValue("Primary Object",inner,outO);
									}
								}
							}
						}
						if(box.objects.columnExists(box.objects.getColumnIndex("Primary Object"))==false){IJ.error(box.analysis,"No primary objects found.");return 0;}
						int[][] surcount = new int[C+1][OC+1];
						double[][] fociintt = new double[C+1][OC+1];
						double[][] focimean = new double[C+1][OC+1];
						double[][] focivolsd = new double[C+1][OC+1];
						double[][] focivol = new double[C+1][OC+1];
						double[][] focivolsqdif = new double[C+1][OC+1];
						double[][] focivolmean = new double[C+1][OC+1];
						double[][] sdsum = new double[C+1][OC+1];
						double[][] focisd = new double[C+1][OC+1];
						for(int i=0;i<OC;i++){
							if(stop==true){return 0;}
							IJ.showStatus("Analysing Object Relationships...");
							int ch = (int)box.objects.getValue("C",i);
							int primIndex = (int)box.objects.getValue("Primary Object",i);
							if(ch!=box.prim){
								surcount[ch][primIndex]++;
								fociintt[ch][primIndex] += box.objects.getValue("Mean",i);
								focivol[ch][primIndex] += box.objects.getValue("Volume"+" ("+box.unit+"\u00B3)",i);
								sdsum[ch][primIndex] += box.objects.getValue("StdDev",i);
							}
						}

						for(int ch=1;ch<=C;ch++){			//ch loop
							for(int i=0;i<=OC;i++){
								focimean[ch][i]=fociintt[ch][i]/surcount[ch][i];
								focivolmean[ch][i]=focivol[ch][i]/surcount[ch][i];
							}
							if(ch==box.prim){continue;}
							for(int i=0;i<OC;i++){
								IJ.showStatus("Analysing Object Relationships...");
								if(box.objects.getValue("C",i)==ch){
									focivolsqdif[ch][(int)box.objects.getValue("Primary Object",i)]+=Math.pow(box.objects.getValue("Volume"+" ("+box.unit+"\u00B3)",i)-focivolmean[ch][(int)box.objects.getValue("Primary Object",i)],2);
								}
							}
							for(int i=0;i<=OC;i++){
								if(surcount[ch][i]>0){
									focisd[ch][i]=Math.sqrt(Math.abs((sdsum[ch][i]/surcount[ch][i])-Math.pow(focimean[ch][i],2)));
									focivolsd[ch][i]=Math.sqrt(focivolsqdif[ch][i]/(surcount[ch][i]-1));
								}
							}
							for(int i=0;i<OC;i++){
								IJ.showStatus("Analysing Object Relationships...");
								if(box.objects.getValue("C",i)==box.prim){
									box.objects.setValue("Foci Count C"+ch,i,surcount[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci Total Intensity C"+ch,i,fociintt[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci Mean Intensity C"+ch,i,focimean[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci Total Volume"+" ("+box.unit+"\u00B3) C"+ch,i,focivol[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci StdDev Intensity C"+ch,i,focisd[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci Mean Volume"+" ("+box.unit+"\u00B3) C"+ch,i,focivolmean[ch][(int)box.objects.getValue("Object",i)]);
									box.objects.setValue("Foci StdDev Volume"+" ("+box.unit+"\u00B3) C"+ch,i,focivolsd[ch][(int)box.objects.getValue("Object",i)]);
								}
							}
						}//ch loop

					}

				}catch(Exception e){throw(new Exception(e.getStackTrace()[0].toString()+System.getProperty("line.separator")+"(Analysis) "+e.toString()));}

				try{
					if(box.analysis=="Surrounding Foci"){
						ResultsTable surround = new ResultsTable();
						surround.showRowNumbers(false);

						for(int i=0;i<box.objects.getCounter();i++){
							surround.incrementCounter();
							for(int ch=1;ch<=C;ch++){					//ch2
								if(ch==box.prim){continue;}
								if(stop==true){return 0;}
								IJ.showStatus("Writing Summary Table...");
								double O = box.objects.getValue("Object",i);
								double Cd = box.objects.getValue("C",i);
								double V = box.objects.getValue("Volume"+" ("+box.unit+"\u00B3)",i);
								double focicount = box.objects.getValue("Foci Count C"+ch,i);
								double fociintt = box.objects.getValue("Foci Total Intensity C"+ch,i);
								double focimean = box.objects.getValue("Foci Mean Intensity C"+ch,i);
								double focivol = box.objects.getValue("Foci Total Volume"+" ("+box.unit+"\u00B3) C"+ch,i);
								double focisd = box.objects.getValue("Foci StdDev Intensity C"+ch,i);
								double focivolsd = box.objects.getValue("Foci StdDev Volume"+" ("+box.unit+"\u00B3) C"+ch,i);
								if((Cd==box.prim)&&(V>=2d)){
									surround.setValue("Object",surround.getCounter()-1,O);
									surround.setValue("Volume"+" ("+box.unit+"\u00B3)",surround.getCounter()-1,V);
									surround.setValue("Foci Count C"+ch,surround.getCounter()-1,focicount);
									surround.setValue("Foci Total Intensity C"+ch,surround.getCounter()-1,fociintt);
									surround.setValue("Foci Mean Intensity C"+ch,surround.getCounter()-1,focimean);
									surround.setValue("Foci StdDev Intensity C"+ch,surround.getCounter()-1,focisd);
									surround.setValue("Foci Total Volume"+" ("+box.unit+"\u00B3) C"+ch,surround.getCounter()-1,focivol);
									surround.setValue("Mean Focus Volume"+" ("+box.unit+"\u00B3) C"+ch,surround.getCounter()-1,focivol/focicount);
									surround.setValue("Foci StdDev Volume"+" ("+box.unit+"\u00B3) C"+ch,surround.getCounter()-1,focivolsd);
									surround.setValue("Foci Integrated Density C"+ch,surround.getCounter()-1,focivol*focimean);
									surround.setValue("Silvia Formula (\u03A3fV*f\u012A*fn)/oV C"+ch,surround.getCounter()-1,(focivol*focimean*focicount)/V);
								}
							}//ch2
						}
						int sc = surround.getCounter();
						for(int u=0;u<sc;u++){
							if(surround.getValue("Object",u)==0){surround.deleteRow(u);u--;sc--;}
						}
						String surtitle = "Surrounding Foci analysis for "+title+" (scan r="+box.sigma+",grad="+box.varreq+",object d="+box.estd+",1° grad="+box.primgrad+")";
						surround.show(surtitle);
						WindowManager.getFrame(surtitle).setBounds(200,100,1000,800);
					}

					if(box.analysis=="Object Intensity Correlation"){
						box.objects = co.combine(box.rt,box.objects);
					}

					if(box.analysis=="Object Colocalisation"){
						int[] count = new int[C+1];
						int[][] colocount = new int[C+1][C+1];
						coloctable = new ResultsTable();
						coloctable.showRowNumbers(false);
						for(int g=0;g<box.rt.getCounter();g++){
							if(stop==true){return 0;}
							for(int k=0;k<box.objects.getCounter();k++){
								if(box.objects.getValue("Object",k)==box.objects.getValue("Object",g)){
									for(int j=1;j<=C;j++){
										box.objects.setValue("Coloc in Ch"+j+"?",k,box.rt.getValue("Coloc in Ch"+j+"?",g));
									}
								}
							}
							count[(int)Math.round(box.rt.getValue("Ch",g))]++;
							for(int i=1;i<=C;i++){
								if(box.rt.getValue("Coloc in Ch"+i+"?",g)==1){colocount[(int)Math.round(box.rt.getValue("Ch",g))][i]++;}
							}
						}
						for(int ch=1;ch<=C;ch++){
							coloctable.incrementCounter();
							coloctable.setLabel("Objects in Ch"+ch,ch-1);
							coloctable.setValue("Count",ch-1,count[ch]);
							for(int co=1;co<=C;co++){
								coloctable.setValue("Coloc in Ch"+co,ch-1,colocount[ch][co]);
							}
						}
					}
					if(box.analysis=="Object Count"){
						int[] count = new int[C+1];
						for(int g=0;g<box.objects.getCounter();g++){
							count[(int)Math.round(box.objects.getValue("C",g))]++;
						}
						for(int co=1;co<=C;co++){
							IJ.log("Object count for "+imp.getTitle()+" C"+co+" = "+count[co]);
						}
					}

					if(box.analysis=="Object Colocalisation"){coloctable.show("Object Colocalisation - radius="+IJ.d2s(box.clusr,3)+box.unit);}

					IJ.run("Overlay Options...", "stroke=cyan width=1 fill=none set");
					for(int d=0;d<box.rt.getCounter();d++){
						thisx = box.rt.getValue("X",d);
						thisy = box.rt.getValue("Y",d);
						if(Z>1){thisz = box.rt.getValue("Slice",d);}else{thisz=1.0;}
						if(T>1){thist = box.rt.getValue("Frame",d);}else{thist=1.0;}
						if(C>1){thisc = box.rt.getValue("Ch",d);}else{thisc=1.0;}
						thisindex = box.rt.getValue("Object",d);
						thisx=thisx/xcal;
						thisy=thisy/ycal;
						String text = new String();
						text="\u00D7"+IJ.d2s(box.rt.getValue("Object",d),0);

						TextRoi txt = new TextRoi(thisx-3,thisy-7,text,OSbox.bigfont); //cross symbol, needs coord adjustments to place character exactly on centroid
						Color txtcolour = box.colours1[(int)thisc];

						if(box.analysis=="Contained Signal"){txtcolour=box.colours1[1];}
						txt.setStrokeColor(txtcolour);
						int txtC = (int)ol.get(d).getCPosition();int txtZ = (int)ol.get(d).getZPosition();int txtT = (int)ol.get(d).getTPosition();
						if(imp.getNDimensions()==3){txt.setPosition((T>Z)?(int)thist:(Z>C)?(int)thisz:(int)thisc);}	//hack to fix txt position bug(s?)
						else{txt.setPosition(txtC,txtZ,txtT);}
						ol.add(txt);
					}

					IJ.run(imp, "Select None", "");

					if(box.analysis=="Track"){
						new OStracker().track(imp,ol,box,box.colours1,box.colours2);
					}
					objectstitle = "Object Scan: objects in "+imp.getTitle()+" (scan radius="+box.sigma+", edge gradient="+box.varreq+", estimated object radius="+box.estd+")";
					if(box.objects.getCounter()==0){return 0;}
					box.objects.show(objectstitle);
					int resw;
					if(box.analysis=="Track"||box.analysis=="Object Colocalisation"){resw = 800;}
					else if((box.analysis=="Surrounding Foci")||(box.analysis=="Rainbow")||(box.analysis=="Object Intensity Correlation")){resw = 1200;}
					else{resw = 550;}
					WindowManager.getFrame(objectstitle).setBounds(0,0,resw,800);
				}catch(Exception e){throw(new Exception(e.getStackTrace()[1].toString()+e.getStackTrace()[1].toString()+System.getProperty("line.separator")+"(Results output) "+e.toString()));}

				try{
					new OSoverlayControl().show(imp,ol,box.colours1,box.colours2,box);
				}catch(Exception e){throw(new Exception(e.getStackTrace()[2].toString()+System.getProperty("line.separator")+"(Results output) "+e.toString()));}

			}//end else not prev
			ol.drawLabels(false);ol.drawNames(false);
			ol.drawBackgrounds(false);
			imp.setOverlay(ol);imp.setHideOverlay(false);
			if(WindowManager.getFrame("Results")!=null){IJ.selectWindow("Results");IJ.run("Close");}	//close default results table last
		}//main try
		catch(Exception except){
			String estding = "Darn, something went wrong. Please e-mail this to Richard:\n" +
					except.toString()+"\n"+except.getStackTrace()[2].toString()+"\n" +
					bd+"-bit,z="+Z+",c="+C+",t="+T+"\n" +
					"prev="+prev+",sigma="+box.sigma+",varreq="+box.varreq+",estd="+box.estd+",pro="+(box.prodog?"DoG":box.prolog?"LoG":box.procanny?"LE":"None")+","+box.analysis;
			IJ.log(estding);
		}
		finally{
			IJ.run("Set Measurements...", "area mean standard centroid stack display redirect=None decimal=2");
			IJ.showStatus("Object Scan Complete");
			if(active!=null){active.dispose();}
			prev=false;
		}
		return 1;
	}//end scan

}//end class
