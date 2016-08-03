package cs7492Project4;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import processing.core.*;
import processing.opengl.*;

public abstract class mySolver {
	public Project4 p;
	// structure holding cells in grid
	public myCellGrid cellGrid;	
	
	//stickiness/eta
	public float prob;
	//CPU calculations
	public int gridCellW, gridCellH, gridCellDp, cellSize;		//size of cell used for calc
	public final int cell2dSize;
	public final int cell3dSize;	

	public ArrayList<Callable<Boolean>> callMTCalcs;
	public List<Future<Boolean>> callMTFutures;					//futures for multi-threaded calc

	public final int type;			
	public final String probType;
	
	public boolean[] SM;									//state manager/state machine governing behavior of this solver
	public final int oneSeed = 0;							//this solver uses 1 or multiple seeds
	
	public final int numFlags = 1;								

	public mySolver(Project4 _p, int _type) {
		p=_p;
		type = _type;
		probType = (type == p.DLAslv ? "Stickiness" : "Eta");
		cell2dSize = p.c2sz[type];
		cell3dSize =  p.c3sz[type];

	}	
	//initialized only upon creation
	public void initSolverOnce(){
		SM = new boolean[numFlags];
		for(int i=0; i< numFlags;++i){SM[i]=true;}		
		setProbParam();
		initSolver();
		//initShaders();
	}
	//call every time switching to this solver
	public void initSolver(){
		callMTCalcs = new ArrayList<Callable<Boolean>>();
		callMTFutures = new ArrayList<Future<Boolean>>(); 	
		setProbParam();	//get current probability value from UI
		if(p.flags[p.solve3D]){
			cellSize = cell3dSize;
			gridCellW = (p.grid3DDimX / (cell3dSize));
			gridCellH = (p.grid3DDimY / (cell3dSize));
			gridCellDp = (p.grid3DDimZ / (cell3dSize));
			cellGrid = new myCellGrid(p, this, gridCellW, gridCellH, gridCellDp);
			cellGrid.initSingleSeed3D();
		} else {
			cellSize = cell2dSize;
			gridCellW = (p.grid2D_X / (cell2dSize));
			gridCellH = (p.grid2D_Y / (cell2dSize));
			gridCellDp = 1;
			cellGrid = new myCellGrid(p, this, gridCellW, gridCellH, gridCellDp);
			if(SM[oneSeed]) {cellGrid.initSingleSeed2D();} //else {cellGrid.initTemplateSeed();}			//may want to not initialize with uniform distribution of walkers if using a template			
		}		
		initSolverSpecific();
	}
	////////
	//cpu solver stuff
	////////
	//calculate algorithm using CPU for 2D - implementation specific
	public abstract void calcCPU();		
	//draws values built on CPU from 2D grid - always the same drawing routine
	public void draw2DCPU_Res() {		
		cellGrid.drawMe2D();	
		p.pushMatrix(); p.pushStyle();
		p.translate(0,this.gridCellH*2.1f,0);
		drawSolverData();
		p.popStyle();p.popMatrix();
	}// drawScene function		
	//draws values built on CPU from 2D grid - always the same drawing routine
	public void draw3DCPU_Res() {		
		cellGrid.drawMe3D();	
		p.pushMatrix(); p.pushStyle();
		p.translate(0,this.gridCellH*2.1f,0);
		drawSolverData();
		p.popStyle();p.popMatrix();
	}// drawScene function		
	
	public abstract void drawSolverData();
	public abstract void setTmplSeed(myPoint pt);

	
	//querying values from grid	
	public myPoint findLocInGrid(myPoint pt){
		myPoint res = new myPoint(pt);
		res._div(cellSize);
		return res;
	}
	public boolean outsideArea(myPoint aRaw){
		myPoint a = new myPoint(aRaw);
		a._div(1.0f*this.cellSize);
		return ((a.x < 0) || (a.y < 0) || (a.z<0) ||
			(a.x > gridCellW ) || (a.y  > gridCellH) || (a.z > gridCellDp));
	}
  
    
	//abstract functions
    public abstract String getProbStr();
	public abstract void initSolverSpecific();
	public void setProbParam() {prob = p.guiObjs[type+p.slvrProbOffset].valAsFloat();	}
 

	public FloatBuffer allocateDirectFloatBuffer(int n) {
		return ByteBuffer.allocateDirect(n * Float.SIZE/8).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}
	
}
