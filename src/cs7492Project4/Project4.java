package cs7492Project4;

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import javax.media.opengl.GL2;

import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;

/**
 * cs7492 project 4 
 * 
 * @author john turner DLA and DBM
 * 
 */ 
public class Project4 extends PApplet {
	//project4-specific variables
	public String prjNmLong = "Project4", prjNmShrt = "Prj4";
	
	public mySolver[] slvrs;								//2 solvers, dla and dbm
	public int curSlvrIDX;								//idx in slvrs currently being used
	//ints representing DLA or DBM solvers
	public final int DLAslv = 0,
					 DBMslv = 1;
	//ui idx's repesenting UI objects holding probs for DLA and DBM solvers - these are IDXs in guiobjs array
	public final int UI_DLAslv = 1,
				      UI_DBMslv = 2;
	
	public final int slvrProbOffset = 1;			//offset into list of UI modifiable values coresponding to a particular solver's stickines/eta value	
	public final int numSolvers = 2;
	public myPoint lastDrawnPt;									//last drawn point - so we don't reset occ if we don't  move
	//shader file names
	public String[] shdr2D = new String[]{"DLA_2DSlvr.frag", "DBM_2DSlvr.frag"},
					shdr3D = new String[]{"DLA_3DSlvr.frag", "DBM_3DSlvr.frag"},
					shdrVis3D = new String[]{"DLA_3DVis.frag", "DBM_3DVis.frag"};
			
	//public String mcShdrNm = "marchCube3d.frag", shdr2DName = "ReactDiff2D.glsl";//changed to whichever is appropriate for this project

	public int[] c2sz = new int[]{2,2};													//cell2d size
	public int[] c3sz = new int[]{4,4};													//cell3d size
		
	public float[][] probPresets = new float[][]{{1.0f,.1f, .01f},{0,3,6}};				//3 preset values for each type of solver
	
	public float isoLevel = .7f;														//for marching cubes visualization
	
	public ExecutorService th_exec;
	public void settings(){
		size((int)(displayWidth*.95f), (int)(displayHeight*.9f),P3D); noSmooth();
	}
	public void setup(){
		//size((int)(displayWidth*.95f), (int)(displayHeight*.9f),OPENGL);
		initOnce();
		background(bground[0],bground[1],bground[2],bground[3]);		
	}//setup	
	
	//called once at start of program
	public void initOnce(){
		initVisOnce();						//always first
		flags[runSim] = true;				// set sim to run initially
		flags[showWalkers] = true;
		//flags[showDLATrails] = true;
		guiObjs[gIDX_DLA_Stick].val = 1;
		guiObjs[gIDX_DBM_Eta].val = 3;
		guiObjs[gIDX_RndWlkTail].val = 10;
		guiObjs[gIDX_isoLvl].val = isoLevel;
		focusTar = new myVector(focusVals[(flags[solve3D] ? 1 : 0)]);
		curSlvrIDX = DLAslv;				//start with DLA
		flags[useDLAslv] = true;
		th_exec = Executors.newCachedThreadPool();		
		lastDrawnPt = new myPoint(-1,-1,-1);
		//init solvers
		slvrs = new mySolver[2];
		slvrs[0] = new myDLASolver(this, 0); 
		slvrs[1] = new myDBMSolver(this, 1); 
		for(int i =0; i<this.numSolvers; ++i){	
			slvrs[i].initSolverOnce();}
		initProgram();		
	}//initOnce
	
	public void initProgram(){
		initVisProg();				//always first

		drawCount = 0;
	}//initProgram
	
	public void draw(){	
		cyclModCmp = (drawCount % guiObjs[gIDX_cycModDraw].valAsInt() == 0);		
//		if(flags[useShader]) {				if(flags[solve3D]){		shdr_draw3D_solve3D();} else {	shdr_draw3D_solve2D();}}
//		else {
			if(flags[solve3D]){		draw3D_solve3D();} else {	draw3D_solve2D();}
//			}
		
		drawUI();	
		if (flags[saveAnim]) {	savePic();}
	}//draw
	//cpu calculations
	public void draw3D_solve2D(){
		if((flags[runSim]) && (drawCount%scrMsgTime==0)){if(consoleStrings.size() != 0){consoleStrings.pollFirst();}}
		pushMatrix();pushStyle();
		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
		if ((!cyclModCmp) || (flags[runSim])) {drawCount++;}			//needed to stop draw update so that pausing sim retains animation positions
		if(flags[runSim]){	
			//run 2d version of sim
			slvrs[curSlvrIDX].calcCPU();			
			if(flags[singleStep]){flags[runSim]=false;}
		}
		translate(focusTar.x,focusTar.y,focusTar.z);				//center of screen		
		if (cyclModCmp) {
			background(bground[0],bground[1],bground[2],bground[3]);			
			//draw 2d results
			slvrs[curSlvrIDX].draw2DCPU_Res();
			drawAxes(100,3, new myPoint(-viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
		}
		c.buildCanvas();																//build drawing canvas based upon eye-to-scene vector		
		popStyle();popMatrix(); 
	}//draw3D_solve2D
	
	//cpu calculations
	public void draw3D_solve3D(){
		if((flags[runSim]) && (drawCount%scrMsgTime==0)){if(consoleStrings.size() != 0){consoleStrings.pollFirst();}}
		pushMatrix();pushStyle();
		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
		if ((!cyclModCmp) || (flags[runSim])) {drawCount++;}			//needed to stop draw update so that pausing sim retains animation positions
		if(flags[runSim]){	
			//run 2d version of sim
			slvrs[curSlvrIDX].calcCPU();			
			if(flags[singleStep]){flags[runSim]=false;}
		}
		translate(focusTar.x,focusTar.y,focusTar.z);				//center of screen		
		if (cyclModCmp) {	
			background(bground[0],bground[1],bground[2],bground[3]);	
			pushMatrix();pushStyle();
			translate(-grid3DDimX/2.0f,-grid3DDimY/2.0f,-grid3DDimZ/2.0f);				//center of screen		
			slvrs[curSlvrIDX].draw3DCPU_Res();
			popStyle();popMatrix();
			drawAxes(100,3, new myPoint(-viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
		}
		c.buildCanvas();																//build drawing canvas based upon eye-to-scene vector
		drawBoxBnds();
		popStyle();popMatrix(); 
	}//draw3D	
	
	
	//end for cpu calcuations
	
	//for shader calculations
//	public void shdr_draw3D_solve2D(){
//		if((flags[runSim]) && (drawCount%scrMsgTime==0)){if(consoleStrings.size() != 0){consoleStrings.pollFirst();}}
//		pushMatrix();pushStyle();
//		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
//		if ((!cyclModCmp) || (flags[runSim])) {drawCount++;}			//needed to stop draw update so that pausing sim retains animation positions
//		if(flags[runSim]){	
//			//run 2d version of sim
//			slvrs[curSlvrIDX].calc2DShader();			
//			if(flags[singleStep]){flags[runSim]=false;}
//		}
//		translate(focusTar.x,focusTar.y,focusTar.z);				//center of screen		
//		if (cyclModCmp) {
//			background(bground[0],bground[1],bground[2],bground[3]);			
//			//draw 2d results
//			slvrs[curSlvrIDX].draw2DShdrVisRes();
//			drawAxes(100,3, new myPoint(-viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
//		}
//		c.buildCanvas();																//build drawing canvas based upon eye-to-scene vector		
//		popStyle();popMatrix(); 
//	}//shdr_draw3D_solve2D	
//	public void shdr_draw3D_solve3D(){
//		if((flags[runSim]) && (drawCount%scrMsgTime==0)){if(consoleStrings.size() != 0){consoleStrings.pollFirst();}}
//		pushMatrix();pushStyle();
//		drawSetup();																//initialize camera, lights and scene orientation and set up eye movement
//		if ((!cyclModCmp) || (flags[runSim])) {drawCount++;}			//needed to stop draw update so that pausing sim retains animation positions
//		if(flags[runSim]){	
//			//run shader-based 3d sim		
//			slvrs[curSlvrIDX].calc3dShader();			
//			if(flags[singleStep]){flags[runSim]=false;}
//		}
//		translate(focusTar.x,focusTar.y,focusTar.z);				//center of screen		
//		if (cyclModCmp) {	
//			background(bground[0],bground[1],bground[2],bground[3]);	
//			pushMatrix();pushStyle();
//			translate(-grid3DDimX/2.0f,-grid3DDimY/2.0f,-grid3DDimZ/2.0f);				//center of screen		
//			//draw results of shader-based sim			
//			slvrs[curSlvrIDX].draw3DShdrVisRes();			
//			popStyle();popMatrix();
//			drawAxes(100,3, new myPoint(-viewDimW/2.0f+40,0.0f,0.0f), 200, false); 		//for visualisation purposes and to show movement and location in otherwise empty scene
//		}
//		c.buildCanvas();																//build drawing canvas based upon eye-to-scene vector
//		drawBoxBnds();
//		popStyle();popMatrix(); 
//	}//shdr_draw3D_solve3D
//	//end for shader calculation	
	
	public void drawUI(){
		drawSideBar();					//draw clickable side menu	
		if((!flags[runSim]) || cyclModCmp){
			drawOnScreenData();
		}
	}//drawUI	

	public myPoint bndClkInBox2D(myPoint p){p.set(max(0,min(p.x,grid2D_X)),max(0,min(p.y,grid2D_Y)),0);return p;}
	public myPoint bndClkInBox3D(myPoint p){p.set(max(0,min(p.x,grid3DDimX)), max(0,min(p.y,grid3DDimY)),max(0,min(p.z,grid3DDimZ)));return p;}	
	
	//launch solver from user input setting prob value to be some preset
	public void launchSolver(int typ, int presetIdx){	
		switch (typ){
			case DLAslv  :{		setFlags(this.useDLAslv, true);	break;}
			case DBMslv : {		setFlags(this.useDBMslv, true);	break;}
			default :{			setFlags(this.useDLAslv, true);	break;} //default to dla
		}
		guiObjs[curSlvrIDX + slvrProbOffset].val = probPresets[typ][presetIdx];	//set 	
	}//launchSolver
	
	public String getTypeString(int typ){
		switch (typ){
			case DLAslv  : {return "DLA";}
			case DBMslv : {return "DBM";}
			default : {return "Unknown Type : " +typ;}		
		}
	}
	
	//////////////////////////////////////////////////////
	/// user interaction
	//////////////////////////////////////////////////////	
	public void keyPressed(){
		switch (key){
			case '1' : {launchSolver(DLAslv, 0);break;}//From a single seed, run DLA with a sticking factor of 1 (always sticks).                                 
			case '2' : {launchSolver(DLAslv, 1);break;}//From a single seed, run DLA with a sticking factor of 0.1.                                               
			case '3' : {launchSolver(DLAslv, 2);break;}//From a single seed, run DLA with a sticking factor of 0.01.                                              
			case '4' : {launchSolver(DBMslv, 0);break;}//From a single seed, run DBM with eta = 0.                                                                
			case '5' : {launchSolver(DBMslv, 1);break;}//From a single seed, run DBM with eta = 3.                                                                
			case '6' : {launchSolver(DBMslv, 2);break;}//From a single seed, run DBM with eta = 6.                                                                
			case '7' : {break;}
			case '8' : {break;}
			case '9' : {break;}
			case '0' : {setFlags(drawSeedTmp,!flags[drawSeedTmp]);break;}//Run DLA with a seed pattern and a sticking factor of your own choosing. Do not just use one seed cell!   
			case ' ' : {setFlags(runSim,!flags[runSim]); break;}							//run sim
			case 'a' :
			case 'A' : {setFlags(saveAnim,!flags[saveAnim]);break;}						//start/stop saving every frame for making into animation
			case 'i' : 
			case 'I' : {initProgram();break;}		//re-start program
			case 's' :
			case 'S' : {setFlags(singleStep,!flags[singleStep]); break;}//Take one simulation step		
			case ';' :
			case ':' : {guiObjs[gIDX_cycModDraw].modVal(-1, false); break;}//decrease the number of cycles between each draw, to some lower bound
			case '\'' :
			case '"' : {guiObjs[gIDX_cycModDraw].modVal(1, false); break;}//increase the number of cycles between each draw to some upper bound		
			default : {	}
		}//switch	
		
		if((!flags[shiftKeyPressed])&&(key==CODED)){flags[shiftKeyPressed] = (keyCode  == KeyEvent.VK_SHIFT);}
	}
	public void keyReleased(){
		if((flags[shiftKeyPressed])&&(key==CODED)){ if(keyCode == KeyEvent.VK_SHIFT){clearFlags(new int []{shiftKeyPressed, modView});}}
	}

	//2d range checking of point
	public boolean ptInRange(float x, float y, float minX, float minY, float maxX, float maxY){return ((x > minX)&&(x < maxX)&&(y > minY)&&(y < maxY));}	
	/**
	 * handle mouse presses - print out to console value of particular cell
	 */
	public void mousePressed() {
		if(mouseX<(menuWidth)){//check where mouse is - if in range of side menu, process clicks for ui input and ignore for main window	
			if(mouseX>(menuWidth-15)&&(mouseY<15)){showInfo =!showInfo; return;}			//turn on/off info header - click in bouse box
			if(mouseY<20){return;}															//margin
			int i = (int)((mouseY-(yOff))/(yOff));
			if(clkyFlgs.contains(i)){setFlags(i,!flags[i]);}								//boolean flags - always first
			else if(ptInRange(mouseX, mouseY, minClkX, minGuiClkY,	maxClkX, maxGuiClkY)){//in UI region for clickable objects
				for(int j=0; j<numGuiObjs; ++j){if(guiObjs[j].clickIn(mouseX, mouseY)){	msClkObj=j;return;	}}
			}//1st check if click in range of modifiable fields
		}//handle menu interaction
		else {//in main sim window
			if(!flags[shiftKeyPressed]){
				flags[mouseClicked] = true;
				if(flags[drawSeedTmp]){	
					addDrawnSeed();
//					myPoint cellIdx = slvrs[curSlvrIDX].findLocInGrid(c.dfCtr);
//					if(slvrs[curSlvrIDX].outsideArea(cellIdx)){return;}
//					lastDrawnPt = new myPoint(c.dfCtr.x, c.dfCtr.y, 0);						
//					slvrs[curSlvrIDX].cellGrid.setTmplSeed(cellIdx);
				}//if we are drawing seed template, then reinit template array
			}
		}	
	}// mousepressed	
	
	public void mouseDragged(){
		if(msClkObj!=-1){	guiObjs[msClkObj].modVal((mouseX-pmouseX)+(mouseY-pmouseY)*-5.0f,((flags[shiftKeyPressed])&&(key==CODED)) ); return;}//if not -1 then already modifying value, no need to pass or check values of box
		if(mouseX<(width * menuWidthMult)){	//handle menu interaction not handled during click 
		}
		else {
			if(flags[shiftKeyPressed]){		//changing view when in main window and shift is pressed
				flags[modView]=true;
				if(mouseButton == LEFT){			rx-=PI*(mouseY-pmouseY)/height; ry+=PI*(mouseX-pmouseX)/width;} 
				else if (mouseButton == RIGHT) {	dz-=(double)(mouseY-pmouseY);}
			} else {//drawing template for seed
				if(flags[drawSeedTmp]){
					if(!flags[solve3D]){
						addDrawnSeed();
//						if(inSameCell(c.dfCtr, lastDrawnPt)){return;}
//						myPoint cellIdx = slvrs[curSlvrIDX].findLocInGrid(c.dfCtr);
//						if(slvrs[curSlvrIDX].outsideArea(cellIdx)){return;}
//						lastDrawnPt = new myPoint(c.dfCtr.x, c.dfCtr.y, 0);						
//						slvrs[curSlvrIDX].cellGrid.setTmplSeed(cellIdx);
					} else {}//not handling drawing in 3D space						
				}				
			}
		}
	}//mouseDragged()
	
	public void addDrawnSeed(){
		if(inSameCell(c.dfCtr, lastDrawnPt)){return;}
		myPoint cellIdx = slvrs[curSlvrIDX].findLocInGrid(c.dfCtr);
		if(slvrs[curSlvrIDX].outsideArea(cellIdx)){return;}
		lastDrawnPt = new myPoint(c.dfCtr.x, c.dfCtr.y, 0);						
		slvrs[curSlvrIDX].setTmplSeed(cellIdx);
	}
	
	public boolean inSameCell(myPoint a, myPoint b){return ((abs(a.x-b.x) < 1) && (abs(a.y-b.y) < 1) &&	(abs(a.z-b.z) < 1));}
	
	public void mouseReleased(){
		clearFlags(new int[]{mouseClicked, modView});
		if(msClkObj != -1){
			updateFromUI(msClkObj);		
			msClkObj = -1;		
		}//any updating necessary for click values having been modified
		lastDrawnPt = new myPoint(-1,-1,-1);			
	}
	
	//debug data to display on screen
	//get string array for onscreen display of debug info for each object
	public String[] getDebugData(){
		ArrayList<String> res = new ArrayList<String>();
		List<String>tmp;
		for(int j = 0; j<numGuiObjs; j++){tmp = Arrays.asList(guiObjs[j].getStrData());res.addAll(tmp);}
		return res.toArray(new String[0]);	
	}
	
	public void setFocus(int tar){
		int idx = tar;
		focusTar.set(focusVals[(tar+focusVals.length)%focusVals.length]);
		switch (tar){//special handling for each view
		case 0 : {initProgram();break;} //refocus camera on center
		case 1 : {initProgram();break;}  
		}
	}
	
	public void setCamView(){
		int idx = flags[solve3D] ? 1 : 0;
		rx = cameraInitLocs[idx].x;
		ry = cameraInitLocs[idx].y;
		dz = cameraInitLocs[idx].z;
		setFocus(idx);
	}

	public void updateFromUI(int idx){
		switch(idx){
		case gIDX_RndWlkTail :{
			((myDLASolver)slvrs[DLAslv]).resetTrailLength(guiObjs[gIDX_RndWlkTail].valAsInt());
			break;}
		}		
	}
	
	//////////////////////////////////////////
	/// graphics and base functionality utilities and variables
	//////////////////////////////////////////
	//display-related size variables
	public final int grid2D_X=500, grid2D_Y=500;	
	public final int grid3DDimX = 500, grid3DDimY = 500, grid3DDimZ = 500;				//dimensions of 3d region in pxls

	public myVector[] focusVals = new myVector[]{						//set these values to be different targets of focus
		new myVector(-grid2D_X/2,-grid2D_Y/1.75f,0),
		new myVector(0,0,grid3DDimZ/3.0f)
	};
	
	//static variables - put obj constructor counters here
	public static int GUIObjID = 0;										//counter variable for gui objs
	
	//visualization variables
	// boolean flags used to control various elements of the program 
	public boolean[] flags;
	
	//dev/debug flags
	public final int debugMode 			= 0;			//whether we are in debug mode or not	
	public final int saveAnim 			= 1;			//whether we are in debug mode or not	
	//interface flags	
	public final int shiftKeyPressed 	= 2;			//shift pressed
	public final int mouseClicked 		= 3;			//mouse left button is held down	
	public final int modView	 		= 4;			//shift+mouse click+mouse move being used to modify the view		
	public final int runSim				= 5;			//run simulation (if off localization progresses on single pose
	public final int singleStep 		= 6;			// whether to use single step mode in animation	
	public final int showAdjZone		= 7; 			//show region around substrate that is where new growth will occur
	public final int drawSeedTmp		= 8; 			//whether we should draw, and use, a substrate template for the seed
	
	//solver-specific - DLA
	public final int useDLAslv			= 9;			//set which solver to use - mutually exclusive
	public final int showWalkers 		= 10;
	public final int showDLATrails		= 11;
	//solver-specific - DBM
	public final int useDBMslv			= 12;	
	
	//solver-specific - shaders
	public final int useShader			= 13;			//use shader for this calculation, or use cpu processing
	public final int solve3D			= 14;			//whether to run the 3D solver

	public final int numFlags = 15;
	
	public boolean showInfo;										//whether or not to show start up instructions for code
	
	public myVector focusTar;										//target of focus - used in translate to set where the camera is looking - set array of vector values (focusVals) based on application
	private boolean cyclModCmp;									//comparison every draw of cycleModDraw	
	
	public final int[] bground = new int[]{240,240,240,255};		//bground color
	
	
	public final String[] trueFlagNames = {//needs to be in order of flags
			"Debug Mode",		
			"Save Anim", 		
			"Shift-Key Pressed",
			"Click interact", 	
			"Changing View",	
			"Execute Simulation",
			"Run Sim Single Step",
			"Show Adjacent Zone",
			"Draw Seed Template",
			"Run DLA Algorithm",
			"Show DLA Walkers",
			"Show DLA Walker Trails",
			"Run DBM Algorithm",
			"Use Shader To Solve",
			"Change back to 2D"
			};
	
	public final String[] falseFlagNames = {//needs to be in order of flags
			"Debug Mode",		
			"Save Anim", 		
			"Shift-Key Pressed",
			"Click interact", 	
			"Changing View",	 	
			"Execute Simulation",
			"Run Sim Single Step",
			"Show Adjacent Zone",
			"Draw Seed Template",
			"Run DLA Algorithm",
			"Show DLA Walkers",
			"Show DLA Walker Trails",
			"Run DBM Algorithm",
			"Use CPU To Solve",
			"Change to 3D"
			};	
	
	public int[][] flagColors;
	//flags that can be modified by clicking on screen - order doesn't matter
	public List<Integer> clkyFlgs = Arrays.asList(
			debugMode, saveAnim,runSim,singleStep, showAdjZone, showWalkers,useDLAslv,drawSeedTmp, showDLATrails, useDBMslv, solve3D		
			);			
	float xOff = 20 , yOff = 20;			//offset values to render boolean menu on side of screen	
	public final float minClkX = 17;
	public float minGuiClkY, maxGuiClkY, maxClkY, maxClkX;	
	public int msClkObj;
	
	public myPoint mseCurLoc2D;
	//timestep
	//public double deltaT;
	public final double maxDelT = 7;			//max value that delta t can be set to
	//how many frames to wait to actually refresh/draw
	//public int cycleModDraw = 1;
	public final int maxCycModDraw = 20;	//max val for cyc mod draw
	
	public myGUIObj[] guiObjs;	
	public final int numGuiObjs = 5;		//# of gui objects for ui
	public final double[][] guiMinMaxModVals = new double [][]{
			//min, max, small mod and big mod values
			{1, maxCycModDraw, .1, 1},	
			{0, 1, .00001,.001},														//Stickiness		
			{0, 10000, .1, 10},															//Eta			
			{1,100,1,1},																//random walker trail lengths
			{0, 1, .001, .01}															//Iso level of mc dislay			
	};

	public final String[] guiObjNames = new String[]{"Draw Cycle Length", "DLA Stickiness", "DBM Eta", "Rnd Walker Tail Len", "MC Iso Level"};	
	
	public final boolean[] guiTrtAsInt = new boolean[]{true, false, false, true, false};	
	
	//idx's of objects in gui objs array	
	public final int gIDX_cycModDraw = 0,
					  gIDX_DLA_Stick = 1,
					    gIDX_DBM_Eta = 2,
					 gIDX_RndWlkTail = 3,
					 	 gIDX_isoLvl = 4;
		
	// path and filename to save pictures for animation
	public String animPath, animFileName;
	public int animCounter;	
	public final int scrMsgTime = 50;									//5 seconds to delay a message 60 fps (used against draw count)
	public ArrayDeque<String> consoleStrings;							//data being printed to console - show on screen

	public final float camInitialDist = 0,		//initial distance camera is from scene - needs to be negative
				camInitRy = 0,
				camInitRx = -PI/2.0f;
	
	public myVector[] cameraInitLocs = new myVector[]{						//set these values to be different initial camera locations based on 2d or 3d
			new myVector(camInitRx,camInitRy,camInitialDist),
			new myVector(-0.47f,-0.61f,-grid3DDimZ*.5f)			
		};

	public final int viewDim = 900;
	public int viewDimW, viewDimH;
	public int drawCount;												// counter for draw cycles
	public int simCycles;
	
	public float menuWidthMult = .15f;									//side menu is 15% of screen grid2D_X
	public float menuWidth;

	public ArrayList<String> DebugInfoAra;								//enable drawing dbug info onto screen
	public String debugInfoString;
	
	//animation control variables	
	public float animCntr, animModMult;
	public final float maxAnimCntr = PI*1000.0f, baseAnimSpd = 1.0f;
	
	private float dz=0, 												// distance to camera. Manipulated with wheel or when
	rx=-0.06f*TWO_PI, ry=-0.04f*TWO_PI;									// view angles manipulated when space pressed but not mouse	
	my3DCanvas c;												//3d stuff and mouse tracking
	
	public float[] camVals;
	
	public String dateStr, timeStr;								//used to build directory and file names for screencaps
	
	public PGraphicsOpenGL pg; 
	public PGL pgl;
	//public GL2 gl;
	
	public float[] projMatrix= new float[16];
	public float[] mvMatrix= new float[16]; 
	
	///////////////////////////////////
	/// generic graphics functions and classes
	///////////////////////////////////
		//1 time initialization of things that won't change
	public void initVisOnce(){		
		dateStr = "_"+day() + "-"+ month()+ "-"+year();
		timeStr = "_"+hour()+"-"+minute()+"-"+second();
		
		colorMode(RGB, 255, 255, 255, 255);
		mseCurLoc2D = new myPoint(0,0,0);	
		frameRate(120);
		sphereDetail(4);
		initBoolFlags();
		camVals = new float[]{width/2.0f, height/2.0f, (height/2.0f) / tan(PI/6.0f), width/2.0f, height/2.0f, 0, 0, 1, 0};
		showInfo = true;
		println("sketchPath " + sketchPath(""));
		textureMode(NORMAL);	
		menuWidth = width * menuWidthMult;						//grid2D_X of menu region	
		setupMenuClkRegions();		
		rectMode(CORNER);	
		
		viewDimW = width;viewDimH = height;
		initCamView();
		simCycles = 0;
		animPath = sketchPath("") + "\\"+prjNmLong+"_" + (int) random(1000);
		animFileName = "\\" + prjNmLong;
		consoleStrings = new ArrayDeque<String>();				//data being printed to console		
		c = new my3DCanvas(this);
	}
		
	public void initCamView(){dz=camInitialDist;ry=camInitRy;rx=camInitRx - ry;	}

	//called every time re-initialized
	public void initVisProg(){	
		simCycles = 0;
		drawCount = 0;		
		debugInfoString = "";		
		reInitInfoStr();
	}	
	public void reInitInfoStr(){		DebugInfoAra = new ArrayList<String>();		DebugInfoAra.add("");	}			
	
	//initialize structure to hold modifiable menu regions
	public void setupMenuClkRegions(){
		minGuiClkY = (numFlags+3) * yOff;
		float stClkY = minGuiClkY;
		maxClkX = .99f * menuWidth;
		msClkObj = -1;									//this is the object currently being modified by mouse dragging
		guiObjs = new myGUIObj[numGuiObjs];			//list of modifiable gui objects
		myGUIObj tmp; 
		double stVal;
		for(int i =0; i< numGuiObjs; ++i){
			stVal =guiMinMaxModVals[i][0];
			tmp = new myGUIObj(this, guiObjNames[i], minClkX, stClkY, maxClkX, stClkY+yOff, guiMinMaxModVals[i][0],guiMinMaxModVals[i][1],stVal, guiTrtAsInt[i], guiMinMaxModVals[i][2], guiMinMaxModVals[i][3]);
			stClkY += yOff;
			guiObjs[i] = tmp;
		}
		maxGuiClkY = stClkY;
	}
		//init boolean state machine flags for program
	public void initBoolFlags(){
		flags = new boolean[numFlags];
		flagColors = new int[numFlags][3];
		for (int i = 0; i < numFlags; ++i) { flags[i] = false; flagColors[i] = new int[]{(int) random(150),(int) random(100),(int) random(150)}; }	
	}
	
	//address all flag-setting here, so that if any special cases need to be addressed they can be
	public void setFlags(int idx, boolean val ){
		flags[idx] = val;
		switch (idx){
			case debugMode 			: {  break;}//anything special for debugMode 			
			case saveAnim 			: {  break;}//anything special for saveAnim 			
			case shiftKeyPressed 	: {  break;}//anything special for shiftKeyPressed 	
			case mouseClicked 		: {  break;}//anything special for mouseClicked 		
			case modView	 		: {  break;}//anything special for modView	 
			case runSim				: {  break;}//anything special for runSim
			case showDLATrails		: {  break;}//anything special to show walker trails
			case showWalkers		: {  
				if(!val){flags[this.showDLATrails] = false;		}	//turn off trails if turning off walkers
				break;}//anything special to show walkers
			case solve3D			: { //only use for dla solver
				setCamView(); 
				if(val){//setup 3d stuff
					setFlags(useDLAslv,true);
					//init 3d stuff
				} else {	
					//go back to base 2d view
					setFlags(useDLAslv,true);
					//special 2d stuff
				}			
				break;}
			case drawSeedTmp : {
				if(val){//turning the template drawing function on - reinit solver
					flags[runSim] = false;
					setFlags(showWalkers,false);
					slvrs[curSlvrIDX].initSolver();						
				}
				break;
			}
			case useDLAslv			: {  
				if(val){//don't call setFlags on true - infinite loop
					flags[useDBMslv] = false;
					curSlvrIDX = DLAslv;
					guiObjs[curSlvrIDX + slvrProbOffset].val = probPresets[curSlvrIDX][0];				//idx 0 means always attach
					slvrs[curSlvrIDX].initSolver();
				} else {setFlags(useDBMslv,true);}
				
				break;}//anything special for using dla solver - set useDBMslv as !val 	
			case useDBMslv			: {  
				if(val){//don't call setFlags on true - infinite loop
					flags[useDLAslv] = false;	flags[showWalkers] = false;		flags[showDLATrails] = false;
					curSlvrIDX = DBMslv;
					guiObjs[curSlvrIDX + slvrProbOffset].val = probPresets[curSlvrIDX][1];				//idx 1  has decent starting value
					slvrs[curSlvrIDX].initSolver();
					
				} else {setFlags(useDLAslv,true);}							
				break;}//anything special for using dbm solver - set useDLAslv as !val	
			case useShader 			: {//whether we are using cpu calculations or shader to solve for the algorithm

				break;}	
		}
	}//setFlags  
	public void clearFlags(int[] idxs){		for(int idx : idxs){flags[idx]=false;}	}	

	public int addInfoStr(String str){return addInfoStr(DebugInfoAra.size(), str);}
	public int addInfoStr(int idx, String str){	
		int lstIdx = DebugInfoAra.size();
		if(idx >= lstIdx){		for(int i = lstIdx; i <= idx; ++i){	DebugInfoAra.add(i,"");	}}
		setInfoStr(idx,str);	return idx;
	}
	public void setInfoStr(int idx, String str){DebugInfoAra.set(idx,str);	}
	public void drawInfoStr(float sc){//draw text on main part of screen
		pushMatrix();		pushStyle();
		fill(0,0,0,100);
		translate((menuWidth),0);
		scale(sc,sc);
		for(int i = 0; i < DebugInfoAra.size(); ++i){		text((flags[debugMode]?(i<10?"0":"")+i+":     " : "") +"     "+DebugInfoAra.get(i)+"\n\n",0,(10+(12*i)));	}
		popStyle();	popMatrix();
	}		
	//vector and point functions to be compatible with earlier code from jarek's class or previous projects	
	//draw bounding box for 3d
	public void drawBoxBnds(){
		pushStyle();
		strokeWeight(3f);
		noFill();
		setColorValStroke(gui_TransGray);
		box(grid3DDimX,grid3DDimY,grid3DDimZ);
		popStyle();		
	}		
	//give a black outline to text displayed in graphics area
	public void drawBoldText(float scVal, int clrVal, String str){
		pushMatrix();		pushStyle();
		scale(scVal);
		setColorValFill(clrVal);
		text(str, 0,0 );		
		popStyle();	popMatrix();
	}
	
	//drawsInitial setup for each draw
	public void drawSetup(){
		camera(camVals[0],camVals[1],camVals[2],camVals[3],camVals[4],camVals[5],camVals[6],camVals[7],camVals[8]);       // sets a standard perspective
		translate((float)width/2.0f,(float)height/2.0f,(float)dz); // puts origin of model at screen center and moves forward/away by dz
	    setCamOrient();
            //noLights();
	    //shininess(.1f);
	    ambientLight(55, 55, 55);
	    lightSpecular(111, 111, 111);
	    directionalLight(255, 255, 255, -1,1,-1);
		//specular(111, 111, 111);
	}//drawSetup	
	public void setCamOrient(){rotateX((float)rx);rotateY((float)ry); rotateX((float)PI/(2.0f));		}//sets the rx, ry, pi/2 orientation of the camera eye	
	public void unSetCamOrient(){rotateX((float)-PI/(2.0f)); rotateY((float)-ry);   rotateX((float)-rx); }//reverses the rx,ry,pi/2 orientation of the camera eye - paints on screen and is unaffected by camera movement
	public void drawAxes(float len, float stW, myPoint ctr, int alpha, boolean centered){
		pushMatrix();pushStyle();
			strokeWeight((float)stW);
			stroke(255,0,0,alpha);
			if(centered){line(ctr.x-len*.5f,ctr.y,ctr.z,ctr.x+len*.5f,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y-len*.5f,ctr.z,ctr.x,ctr.y+len*.5f,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z-len*.5f,ctr.x,ctr.y,ctr.z+len*.5f);} 
			else {		line(ctr.x,ctr.y,ctr.z,ctr.x+len,ctr.y,ctr.z);stroke(0,255,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y+len,ctr.z);stroke(0,0,255,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x,ctr.y,ctr.z+len);}
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawAxes(float len, float stW, myPoint ctr, myVector[] _axis, int alpha){
		pushMatrix();pushStyle();
			strokeWeight((float)stW);stroke(255,0,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[0].x)*len,ctr.y+(_axis[0].y)*len,ctr.z+(_axis[0].z)*len);stroke(0,255,0,alpha);line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[1].x)*len,ctr.y+(_axis[1].y)*len,ctr.z+(_axis[1].z)*len);	stroke(0,0,255,alpha);	line(ctr.x,ctr.y,ctr.z,ctr.x+(_axis[2].x)*len,ctr.y+(_axis[2].y)*len,ctr.z+(_axis[2].z)*len);
		popStyle();	popMatrix();	
	}//	drawAxes
	public void drawText(String str, float x, float y, float z, int clr){
		int[] c = getClr(clr);
		pushMatrix();	pushStyle();
			fill(c[0],c[1],c[2],c[3]);
			unSetCamOrient();
			translate((float)x,(float)y,(float)z);
			text(str,0,0,0);		
		popStyle();	popMatrix();	
	}//drawText	
	public void savePic(){		save(animPath + animFileName + ((animCounter < 10) ? "000" : ((animCounter < 100) ? "00" : ((animCounter < 1000) ? "0" : ""))) + animCounter + ".jpg");		animCounter++;		}
	public void line(double x1, double y1, double z1, double x2, double y2, double z2){line((float)x1,(float)y1,(float)z1,(float)x2,(float)y2,(float)z2 );}
	public void drawOnScreenData(){
		if(flags[debugMode]){
			pushMatrix();pushStyle();			
			reInitInfoStr();
			addInfoStr(0,"mse loc on screen : " + new myPoint(mouseX, mouseY,0) + " mse loc in world :"+c.mseLoc +"  Eye loc in world :"+ c.eyeInWorld + " Dist from focus pt : "+ dz); 
			String[] res = getDebugData();
			//for(int s=0;s<res.length;++s) {	addInfoStr(res[s]);}				//add info to string to be displayed for debug
			int numToPrint = min(res.length,80);
			for(int s=0;s<numToPrint;++s) {	addInfoStr(res[s]);}				//add info to string to be displayed for debug
			drawInfoStr(1.0f); 	
			popStyle();	popMatrix();		
		}
		else if(showInfo){
			pushMatrix();pushStyle();			
			reInitInfoStr();	
			if(showInfo){
		      addInfoStr(0,"Click the light green box to the left to toggle showing this message.");
		      addInfoStr(1,"--Shift-Click-Drag to change view.  Shift-RClick-Drag to zoom.");
		      addInfoStr(2,"--BOTH DLA AND DBM allow for custom seed templates.  Hit `0` or click `Draw Seed Template` with either solver selected and then draw the template you wish.");
		      addInfoStr(3,"--You can also add to an existing pattern in the same way, if you keep `Draw Seed Template` active.  Just stop the simulation, draw the desired pattern, and restart.");
             // addInfoStr(3,"Values at Mouse Location : "+ values at mouse location);
			}
			String[] res = consoleStrings.toArray(new String[0]);
			int dispNum = min(res.length, 80);
			for(int i=0;i<dispNum;++i){addInfoStr(res[i]);}
		    drawInfoStr(1.1f); 
			popStyle();	popMatrix();	
		}
	}
	//print informational string data to console, and to screen
	public void outStr2Scr(String str){
		System.out.println(str);
		consoleStrings.add(str);		//add console string output to screen display- decays over time
	}
	public void dispMenuTxt(String txt, int[] clrAra, boolean showSphere){
		setFill(clrAra, 255); 
		translate(xOff*.5f,yOff*.5f);
		if(showSphere){setStroke(clrAra, 255);		sphere(5);	} 
		else {	noStroke();		}
		translate(-xOff*.5f,yOff*.5f);
		text(""+txt,xOff,-yOff*.25f);	
	}

	public void drawButtons(String[] btnNames, int cmpVal){
		float xWidthOffset = menuWidth/(1.0f * btnNames.length), halfWay;
		pushMatrix();pushStyle();
		strokeWeight(.5f);
		stroke(0,0,0,255);
		noFill();
		translate(-xOff*.5f, 0, 0);
		for(int i =0; i<btnNames.length;++i){
			halfWay = (xWidthOffset - textWidth(btnNames[i]))/2.0f;
			if(i==cmpVal){fill(0,255,0,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);} 
			else {			fill(200,200,200,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);}
			fill(0,0,0,255);
			text(btnNames[i], halfWay, yOff*.75f);
			translate(xWidthOffset, 0, 0);
		}
		popStyle();	popMatrix();			
	}//drawSolverButtons

	public void drawDispButtons(String[] btnNames, int cmpVal, boolean hiliteRow){
		float xWidthOffset = menuWidth/(1.0f * btnNames.length), halfWay;
		pushMatrix();pushStyle();
		strokeWeight(.5f);
		stroke(0,0,0,255);
		noFill();
		translate(-xOff*.5f, 0, 0);
		for(int i =0; i<btnNames.length;++i){
			halfWay = (xWidthOffset - textWidth(btnNames[i]))/2.0f;
			if(hiliteRow) {			
				if(i==cmpVal){	fill(100,255,255,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);} 
				else{    	  	fill(100,100,225,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);}}
			else if(i==cmpVal){fill(100,255,100,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);} 
			else {				fill(255,255,255,255);rect(0,0,xWidthOffset, yOff);	fill(0,0,0,255);}
			fill(0,0,0,255);
			text(btnNames[i], halfWay, yOff*.75f);
			translate(xWidthOffset, 0, 0);
		}
		popStyle();	popMatrix();			
	}//drawSolverButtons	
	
	public void drawMouseBox(){
		translate((width * menuWidthMult-10),0);
	    setColorValFill(showInfo ? gui_LightGreen : gui_DarkRed);
		rect(0,0,10, 10);
	}
	public void setFill(int[] clr, int alpha){fill(clr[0],clr[1],clr[2], alpha);}
	public void setStroke(int[] clr, int alpha){stroke(clr[0],clr[1],clr[2], alpha);}
	public void drawSideBarBooleans(){
		//draw booleans and their state
		translate(10,yOff);
		setColorValFill(gui_Black);
		text("Boolean Flags",0,-yOff*.25f);
		for(int i =0; i<numFlags; ++i){
			if(flags[i] ){													dispMenuTxt(trueFlagNames[i],flagColors[i], true);			}
			else {	if(trueFlagNames[i].equals(falseFlagNames[i])) {		dispMenuTxt(trueFlagNames[i],new int[]{180,180,180}, false);}	
					else {													dispMenuTxt(falseFlagNames[i],new int[]{0,255-flagColors[i][1],255-flagColors[i][2]}, true);}		
			}
		}		
	}//drawSideBarBooleans
	
	//draw ui objects
	public void  drawSideBarData(){
		for(int i =0; i<numGuiObjs; ++i){
			guiObjs[i].draw();			
		}
	}//drawSideBarData
	
	public void drawSideBarMenu(){
		pushMatrix();pushStyle();
			drawMouseBox();						//click mse box for info display
		popStyle();	popMatrix();	
		pushMatrix();pushStyle();
			drawSideBarBooleans();				//toggleable booleans 
		popStyle();	popMatrix();	
//		pushMatrix();pushStyle();			
//			drawMenuInfo();						//display algorithm-specific data
//		popStyle();	popMatrix();	
		pushMatrix();pushStyle();
			drawSideBarData();					//draw what user-modifiable fields are currently available
		popStyle();	popMatrix();					
	}
	
	//draw side bar on left side of screen to enable interaction with booleans
	public void drawSideBar(){
		pushMatrix();pushStyle();
		hint(DISABLE_DEPTH_TEST);
		noLights();
		setColorValFill(gui_White);
		rect(0,0,width*menuWidthMult, height);
		drawSideBarMenu();
		hint(ENABLE_DEPTH_TEST);
		popStyle();	popMatrix();	
	}//drawSideBar
			
	public void drawMseEdge(){//draw mouse sphere and edge normal to cam eye through mouse sphere 
		pushMatrix();
		pushStyle();
			strokeWeight(1f);
			stroke(255,0,0,100);
			c.camEdge.set(1000, c.eyeToMse, c.dfCtr);		//build edge through mouse point normal to camera eye	
			c.camEdge.drawMe();
			translate((float)c.dfCtr.x, (float)c.dfCtr.y, (float)c.dfCtr.z);
			//project mouse point on bounding box walls
			if(flags[solve3D]){drawProjOnBox(c.dfCtr, new int[] {gui_Red, gui_Red, gui_Green, gui_Green, gui_Blue, gui_Blue});}
			drawAxes(10000,1f, myPoint.ZEROPT, 100, true);//
			//draw intercept with box
			stroke(0,0,0,255);
			show(myPoint.ZEROPT,3);
			drawText(""+c.dfCtr,4, 15, 4,0);
			scale(1.5f,1.5f,1.5f);
			//drawText(""+text_value_at_Cursor,4, -8, 4,0);
		popStyle();
		popMatrix();		
	}//drawMseEdge	

	//project passed point onto box surface based on location - to help visualize the location in 3d
	public void drawProjOnBox(myPoint p, int[] clr){
		if(clr.length < 6){clr =  new int[]{gui_Black,gui_Black,gui_Black,gui_Black,gui_Black,gui_Black};}
		show(new myPoint((float)-p.x-grid3DDimX/2.0f,0, 0),5, clr[0]);		show(new myPoint((float)-p.x+grid3DDimX/2.0f,0, 0),5, clr[1]);
		show(new myPoint(0,(float)-p.y-grid3DDimY/2.0f, 0),5, clr[2]);		show(new myPoint(0,(float)-p.y+grid3DDimY/2.0f, 0),5, clr[3]);
		show(new myPoint(0,0, (float)-p.z-grid3DDimZ/2.0f),5, clr[4]);		show(new myPoint(0,0, (float)-p.z+grid3DDimZ/2.0f),5, clr[5]);
	}//drawProjOnBox
	public void drawProjOnBox(myPoint p){drawProjOnBox(p, new int[]{gui_Black,gui_Black,gui_Black,gui_Black,gui_Black,gui_Black});}	
	 
	public myPoint Mouse() {return new myPoint(mouseX, mouseY,0);}                                          			// current mouse location
	public myVector MouseDrag() {return new myVector(mouseX-pmouseX,mouseY-pmouseY,0);};                     			// vector representing recent mouse displacement
	
	public myVector U(myVector v){myVector u = new myVector(v); return u._normalize(); }
	public myVector U(myPoint a, myPoint b){myVector u = new myVector(a,b); return u._normalize(); }
	public myVector U(float x, float y, float z) {myVector u = new myVector(x,y,z); return u._normalize();}
	
	public myVector normToPlane(myPoint A, myPoint B, myPoint C) {return myVector._cross(new myVector(A,B),new myVector(A,C)); };   // normal to triangle (A,B,C), not normalized (proportional to area)
	public void gl_normal(myVector V) {normal((float)V.x,(float)V.y,(float)V.z);}                                          // changes normal for smooth shading
	public void gl_vertex(myPoint P) {vertex((float)P.x,(float)P.y,(float)P.z);}                                           // vertex for shading or drawing

	public void show(myPoint P, float r, int clr) {pushMatrix(); pushStyle(); setColorValFill(clr); setColorValStroke(clr);sphereDetail(5);translate((float)P.x,(float)P.y,(float)P.z); sphere((float)r); popStyle(); popMatrix();} // render sphere of radius r and center P)
	public void show(myPoint P, float r){show(P,r, gui_Black);}
	public void show(myPoint P, String s) {text(s, (float)P.x, (float)P.y, (float)P.z); } // prints string s in 3D at P
	public void show(myPoint P, String s, myVector D) {text(s, (float)(P.x+D.x), (float)(P.y+D.y), (float)(P.z+D.z));  } // prints string s in 3D at P+D
	public boolean intersectPl(myPoint E, myVector T, myPoint A, myPoint B, myPoint C, myPoint X) { // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		myVector EA=new myVector(E,A), AB=new myVector(A,B), AC=new myVector(A,C); 		float t = (float)(myVector._mixProd(EA,AC,AB) / myVector._mixProd(T,AC,AB));		X.set(myPoint._add(E,t,T));		return true;
	}	

	public int[][] triColors = new int[][] {{gui_DarkMagenta,gui_DarkBlue,gui_DarkGreen,gui_DarkCyan},
                                                {gui_LightMagenta,gui_LightBlue,gui_LightGreen,gui_TransCyan}};
	
	public void setColorValFill(int colorVal){
		switch (colorVal){
			case gui_rnd				: { fill(random(255),random(255),random(255),255); ambient(120,120,120);break;}
	    	case gui_White  			: { fill(255,255,255,255); ambient(255,255,255); break; }
	    	case gui_Gray   			: { fill(120,120,120,255); ambient(120,120,120); break;}
	    	case gui_Yellow 			: { fill(255,255,0,255); ambient(255,255,0); break; }
	    	case gui_Cyan   			: { fill(0,255,255,255); ambient(0,255,255); break; }
	    	case gui_Magenta			: { fill(255,0,255,255); ambient(255,0,255); break; }
	    	case gui_Red    			: { fill(255,0,0,255); ambient(255,0,0); break; }
	    	case gui_Blue				: { fill(0,0,255,255); ambient(0,0,255); break; }
	    	case gui_Green				: { fill(0,255,0,255); ambient(0,255,0); break; } 
	    	case gui_DarkGray   		: { fill(80,80,80,255); ambient(80,80,80); break;}
	    	case gui_DarkRed    		: { fill(120,0,0,255); ambient(120,0,0); break;}
	    	case gui_DarkBlue   		: { fill(0,0,120,255); ambient(0,0,120); break;}
	    	case gui_DarkGreen  		: { fill(0,120,0,255); ambient(0,120,0); break;}
	    	case gui_DarkYellow 		: { fill(120,120,0,255); ambient(120,120,0); break;}
	    	case gui_DarkMagenta		: { fill(120,0,120,255); ambient(120,0,120); break;}
	    	case gui_DarkCyan   		: { fill(0,120,120,255); ambient(0,120,120); break;}		   
	    	case gui_LightGray   		: { fill(200,200,200,255); ambient(200,200,200); break;}
	    	case gui_LightRed    		: { fill(255,110,110,255); ambient(255,110,110); break;}
	    	case gui_LightBlue   		: { fill(110,110,255,255); ambient(110,110,255); break;}
	    	case gui_LightGreen  		: { fill(110,255,110,255); ambient(110,255,110); break;}
	    	case gui_LightYellow 		: { fill(255,255,110,255); ambient(255,255,110); break;}
	    	case gui_LightMagenta		: { fill(255,110,255,255); ambient(255,110,255); break;}
	    	case gui_LightCyan   		: { fill(110,255,255,255); ambient(110,255,255); break;}	    	
	    	case gui_Black			 	: { fill(0,0,0,255); ambient(0,0,0); break;}//
	    	case gui_TransBlack  	 	: { fill(0x00010100); ambient(0,0,0); break;}//	have to use hex so that alpha val is not lost    	
	    	case gui_FaintGray 		 	: { fill(77,77,77,77); ambient(77,77,77); break;}//
	    	case gui_FaintRed 	 	 	: { fill(110,0,0,100); ambient(110,0,0); break;}//
	    	case gui_FaintBlue 	 	 	: { fill(0,0,110,100); ambient(0,0,110); break;}//
	    	case gui_FaintGreen 	 	: { fill(0,110,0,100); ambient(0,110,0); break;}//
	    	case gui_FaintYellow 	 	: { fill(110,110,0,100); ambient(110,110,0); break;}//
	    	case gui_FaintCyan  	 	: { fill(0,110,110,100); ambient(0,110,110); break;}//
	    	case gui_FaintMagenta  	 	: { fill(110,0,110,100); ambient(110,0,110); break;}//
	    	case gui_TransGray 	 	 	: { fill(120,120,120,30); ambient(120,120,120); break;}//
	    	case gui_TransRed 	 	 	: { fill(255,0,0,150); ambient(255,0,0); break;}//
	    	case gui_TransBlue 	 	 	: { fill(0,0,255,150); ambient(0,0,255); break;}//
	    	case gui_TransGreen 	 	: { fill(0,255,0,150); ambient(0,255,0); break;}//
	    	case gui_TransYellow 	 	: { fill(255,255,0,150); ambient(255,255,0); break;}//
	    	case gui_TransCyan  	 	: { fill(0,255,255,150); ambient(0,255,255); break;}//
	    	case gui_TransMagenta  	 	: { fill(255,0,255,150); ambient(255,0,255); break;}//   	
	    	default         			: { fill(255,255,255,255); ambient(255,255,255); break; }
	    	    	
		}//switch	
	}//setcolorValFill
	public void setColorValStroke(int colorVal){
		switch (colorVal){
	    	case gui_White  	 	    : { stroke(255,255,255,255); break; }
 	    	case gui_Gray   	 	    : { stroke(120,120,120,255); break;}
	    	case gui_Yellow      	    : { stroke(255,255,0,255); break; }
	    	case gui_Cyan   	 	    : { stroke(0,255,255,255); break; }
	    	case gui_Magenta	 	    : { stroke(255,0,255,255);  break; }
	    	case gui_Red    	 	    : { stroke(255,120,120,255); break; }
	    	case gui_Blue		 	    : { stroke(120,120,255,255); break; }
	    	case gui_Green		 	    : { stroke(120,255,120,255); break; }
	    	case gui_DarkGray    	    : { stroke(80,80,80,255); break; }
	    	case gui_DarkRed     	    : { stroke(120,0,0,255); break; }
	    	case gui_DarkBlue    	    : { stroke(0,0,120,255); break; }
	    	case gui_DarkGreen   	    : { stroke(0,120,0,255); break; }
	    	case gui_DarkYellow  	    : { stroke(120,120,0,255); break; }
	    	case gui_DarkMagenta 	    : { stroke(120,0,120,255); break; }
	    	case gui_DarkCyan    	    : { stroke(0,120,120,255); break; }	   
	    	case gui_LightGray   	    : { stroke(200,200,200,255); break;}
	    	case gui_LightRed    	    : { stroke(255,110,110,255); break;}
	    	case gui_LightBlue   	    : { stroke(110,110,255,255); break;}
	    	case gui_LightGreen  	    : { stroke(110,255,110,255); break;}
	    	case gui_LightYellow 	    : { stroke(255,255,110,255); break;}
	    	case gui_LightMagenta	    : { stroke(255,110,255,255); break;}
	    	case gui_LightCyan   		: { stroke(110,255,255,255); break;}		   
	    	case gui_Black				: { stroke(0,0,0,255); break;}
	    	case gui_TransBlack  		: { stroke(1,1,1,1); break;}	    	
	    	case gui_FaintGray 			: { stroke(120,120,120,250); break;}
	    	case gui_FaintRed 	 		: { stroke(110,0,0,250); break;}
	    	case gui_FaintBlue 	 		: { stroke(0,0,110,250); break;}
	    	case gui_FaintGreen 		: { stroke(0,110,0,250); break;}
	    	case gui_FaintYellow 		: { stroke(110,110,0,250); break;}
	    	case gui_FaintCyan  		: { stroke(0,110,110,250); break;}
	    	case gui_FaintMagenta  		: { stroke(110,0,110,250); break;}
	    	case gui_TransGray 	 		: { stroke(150,150,150,60); break;}
	    	case gui_TransRed 	 		: { stroke(255,0,0,120); break;}
	    	case gui_TransBlue 	 		: { stroke(0,0,255,120); break;}
	    	case gui_TransGreen 		: { stroke(0,255,0,120); break;}
	    	case gui_TransYellow 		: { stroke(255,255,0,120); break;}
	    	case gui_TransCyan  		: { stroke(0,255,255,120); break;}
	    	case gui_TransMagenta  		: { stroke(255,0,255,120); break;}
	    	default         			: { stroke(55,55,255,255); break; }
		}//switch	
	}//setcolorValStroke	
	
	//returns one of 30 predefined colors as an array (to support alpha)
	public int[] getClr(int colorVal){
		switch (colorVal){
    	case gui_Gray   		         : { return new int[] {120,120,120,255}; }
    	case gui_White  		         : { return new int[] {255,255,255,255}; }
    	case gui_Yellow 		         : { return new int[] {255,255,0,255}; }
    	case gui_Cyan   		         : { return new int[] {0,255,255,255};} 
    	case gui_Magenta		         : { return new int[] {255,0,255,255};}  
    	case gui_Red    		         : { return new int[] {255,0,0,255};} 
    	case gui_Blue			         : { return new int[] {0,0,255,255};}
    	case gui_Green			         : { return new int[] {0,255,0,255};}  
    	case gui_DarkGray   	         : { return new int[] {80,80,80,255};}
    	case gui_DarkRed    	         : { return new int[] {120,0,0,255};}
    	case gui_DarkBlue  	 	         : { return new int[] {0,0,120,255};}
    	case gui_DarkGreen  	         : { return new int[] {0,120,0,255};}
    	case gui_DarkYellow 	         : { return new int[] {120,120,0,255};}
    	case gui_DarkMagenta	         : { return new int[] {120,0,120,255};}
    	case gui_DarkCyan   	         : { return new int[] {0,120,120,255};}	   
    	case gui_LightGray   	         : { return new int[] {200,200,200,255};}
    	case gui_LightRed    	         : { return new int[] {255,110,110,255};}
    	case gui_LightBlue   	         : { return new int[] {110,110,255,255};}
    	case gui_LightGreen  	         : { return new int[] {110,255,110,255};}
    	case gui_LightYellow 	         : { return new int[] {255,255,110,255};}
    	case gui_LightMagenta	         : { return new int[] {255,110,255,255};}
    	case gui_LightCyan   	         : { return new int[] {110,255,255,255};}
    	case gui_Black			         : { return new int[] {0,0,0,255};}
    	case gui_FaintGray 		         : { return new int[] {110,110,110,255};}
    	case gui_FaintRed 	 	         : { return new int[] {110,0,0,255};}
    	case gui_FaintBlue 	 	         : { return new int[] {0,0,110,255};}
    	case gui_FaintGreen 	         : { return new int[] {0,110,0,255};}
    	case gui_FaintYellow 	         : { return new int[] {110,110,0,255};}
    	case gui_FaintCyan  	         : { return new int[] {0,110,110,255};}
    	case gui_FaintMagenta  	         : { return new int[] {110,0,110,255};}
    	
    	case gui_TransBlack  	         : { return new int[] {1,1,1,100};}  	
    	case gui_TransGray  	         : { return new int[] {110,110,110,100};}
    	case gui_TransLtGray  	         : { return new int[] {180,180,180,100};}
    	case gui_TransRed  	         	 : { return new int[] {110,0,0,100};}
    	case gui_TransBlue  	         : { return new int[] {0,0,110,100};}
    	case gui_TransGreen  	         : { return new int[] {0,110,0,100};}
    	case gui_TransYellow  	         : { return new int[] {110,110,0,100};}
    	case gui_TransCyan  	         : { return new int[] {0,110,110,100};}
    	case gui_TransMagenta  	         : { return new int[] {110,0,110,100};}	
    	case gui_TransWhite  	         : { return new int[] {220,220,220,150};}	
    	default         		         : { return new int[] {255,255,255,255};}    
		}//switch
	}//getClr	
	
	public int getRndClrInt(){return (int)random(0,23);}		//return a random color flag value from below
	public int[] getRndClr(int alpha){return new int[]{(int)random(0,255),(int)random(0,255),(int)random(0,255),alpha};	}
	public int[] getRndClr(){return getRndClr(255);	}		
	public Integer[] getClrMorph(int a, int b, float t){return getClrMorph(getClr(a), getClr(b), t);}    
	public Integer[] getClrMorph(int[] a, int[] b, float t){
		if(t==0){return new Integer[]{a[0],a[1],a[2],a[3]};} else if(t==1){return new Integer[]{b[0],b[1],b[2],b[3]};}
		return new Integer[]{(int)(((1.0f-t)*a[0])+t*b[0]),(int)(((1.0f-t)*a[1])+t*b[1]),(int)(((1.0f-t)*a[2])+t*b[2]),(int)(((1.0f-t)*a[3])+t*b[3])};
	}

	//used to generate random color
	public static final int gui_rnd = -1;
	//color indexes
	public static final int gui_Black 	= 0;
	public static final int gui_White 	= 1;	
	public static final int gui_Gray 	= 2;
	
	public static final int gui_Red 	= 3;
	public static final int gui_Blue 	= 4;
	public static final int gui_Green 	= 5;
	public static final int gui_Yellow 	= 6;
	public static final int gui_Cyan 	= 7;
	public static final int gui_Magenta = 8;
	
	public static final int gui_LightRed = 9;
	public static final int gui_LightBlue = 10;
	public static final int gui_LightGreen = 11;
	public static final int gui_LightYellow = 12;
	public static final int gui_LightCyan = 13;
	public static final int gui_LightMagenta = 14;
	public static final int gui_LightGray = 15;

	public static final int gui_DarkCyan = 16;
	public static final int gui_DarkYellow = 17;
	public static final int gui_DarkGreen = 18;
	public static final int gui_DarkBlue = 19;
	public static final int gui_DarkRed = 20;
	public static final int gui_DarkGray = 21;
	public static final int gui_DarkMagenta = 22;
	
	public static final int gui_FaintGray = 23;
	public static final int gui_FaintRed = 24;
	public static final int gui_FaintBlue = 25;
	public static final int gui_FaintGreen = 26;
	public static final int gui_FaintYellow = 27;
	public static final int gui_FaintCyan = 28;
	public static final int gui_FaintMagenta = 29;
	
	public static final int gui_TransBlack = 30;
	public static final int gui_TransGray = 31;
	public static final int gui_TransMagenta = 32;	
	public static final int gui_TransLtGray = 33;
	public static final int gui_TransRed = 34;
	public static final int gui_TransBlue = 35;
	public static final int gui_TransGreen = 36;
	public static final int gui_TransYellow = 37;
	public static final int gui_TransCyan = 38;	
	public static final int gui_TransWhite = 39;	
	
}
