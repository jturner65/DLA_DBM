package cs7492Project4;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

import processing.core.PApplet;

/**
 * class describing an abstract grid - instances contain algorithm-specific functionality
 * @author john
 */
public class myCellGrid {
	public Project4 p;	
	public mySolver rs;
	public final int gridWidth, gridHeight, gridDepth, gwgh, cellSize;
	public int ctrIdx;
	public float cellRad;

	public ArrayList<Integer> frontier;				//legitimate idx's for walkers to start	
	
	public float phiMin, phiMax;					//global min/max charges	
	
	public ConcurrentSkipListMap<Integer,myCell> fixedCells;			//cells that are fixed as substrate/potential = key is idx in cellgrid, and ref to cell	
	public ConcurrentSkipListMap<Integer,myCell> adjFixedCells;			//cells that are next to the cells that are fixed	

	public myCell[] cellMap;
	
	public int numVals;
	
	public myCellGrid(Project4 _p4, mySolver _rs, int _x, int _y, int _z){
		p = _p4;
		rs = _rs;
		gridWidth = _x;
		gridHeight = _y;
		gridDepth = _z;				
		cellSize = rs.cellSize;
		cellRad = cellSize *.5f;
		gwgh = gridWidth * gridHeight;
		ctrIdx = (gridWidth-1)/2 + (gwgh-1)/2 + ((gwgh-1) * (gridDepth-1)/2);
		fixedCells = new ConcurrentSkipListMap<Integer,myCell>();		
		adjFixedCells = new ConcurrentSkipListMap<Integer,myCell>();
		initCellGrid();	
		
		initBaseFrontier();
	}//myCellGrid constructor
	
	public void initCellGrid(){
		numVals = gridWidth * gridHeight*gridDepth;
		cellMap = new myCell[numVals];
		for (int cellZ = 0; cellZ < gridDepth; ++cellZ) {
			for (int cellX = 0; cellX < gridWidth; ++cellX) {
				for (int cellY = 0; cellY < gridHeight; ++cellY) {
					int idx = idx(cellX,cellY,cellZ);
					cellMap[idx] = new myCell(p, rs, idx,cellX, cellY, cellZ);
				}
			}// for each y cell location
		}// for each x cell location
	}//initCellGrid
	
	public void drawMe2D(){
		p.pushMatrix();
		p.setColorValFill(p.gui_Black);
		p.rect(0, 0, gridWidth*2, gridHeight*2);
		for(Integer x : fixedCells.keySet()){									cellMap[x].draw2D();		}//draw each cell->black for unoccupied, white for substrate		
		if(p.flags[p.showAdjZone]){	for(Integer x : adjFixedCells.keySet()){	cellMap[x].draw2D(Project4.gui_Magenta);}}//draw adj cell, if appropriate
		for(int x=0;x<frontier.size();++x){										cellMap[frontier.get(x)].draw2D(Project4.gui_Green);}
		p.popMatrix();
	}
	
	
	public void drawMe3D(){
		p.pushMatrix();
		for(Integer x : fixedCells.keySet()){										cellMap[x].draw3D(Project4.gui_TransBlue);		}//draw each cell->black for unoccupied, white for substrate		
		if(p.flags[p.showAdjZone]){	for(Integer x : adjFixedCells.keySet()){	cellMap[x].draw3D(Project4.gui_Magenta);}}//draw adj cell, if appropriate
	//	for(int x=0;x<frontier.size();++x){										cellMap[frontier.get(x)].draw3D(Project4.gui_Green);}
		p.popMatrix();
	}
		
	//gridWidth, gridHeight, gridDepth, gwgh;
	public void initBaseFrontier(){
		int frontierSize = ((2*gridWidth) + (2*(gridHeight-2))) * gridDepth;
		frontier = new ArrayList<Integer>(frontierSize);
		for(int z = 0; z < gridDepth; ++z){//each layer in z
			int zOffset = z * gwgh;
			for(int i =0; i<gridWidth; ++i){//each edge across x
				frontier.add(i + zOffset);
				frontier.add(gwgh - 1 - i + zOffset);		
			}
			for(int i=1; i<gridHeight-1; ++i){//each edge across y
				int val = i*gridWidth;
				frontier.add(val + zOffset);
				frontier.add(val + gridWidth -1 +zOffset);			
			}
		}
		for(int i =0; i<frontierSize; ++i){cellMap[frontier.get(i)].setFrontier();}
	}

	public void setCellAdj2D(int idx){
		int x = cellMap[idx].x,y = cellMap[idx].y,tmpIdx;
		for(int i = -1; i<2; ++i){
			for(int j = -1; j<2; ++j){
				if((i==0)&&(j==0)){continue;}
				tmpIdx = idx(x+i, y+j);
				if(cellMap[tmpIdx].setAdj()){
					adjFixedCells.put(tmpIdx, cellMap[tmpIdx]);
				}
			}		
		}
	}
	//returns a list of the newly added adjacent cells
	public ArrayList<Integer> setCellAdj2D_DBM(int idx){
		ArrayList<Integer> res = new ArrayList<Integer>();
		int x = cellMap[idx].x,y = cellMap[idx].y,tmpIdx;
		for(int i = -1; i<2; ++i){
			for(int j = -1; j<2; ++j){
				if((i==0)&&(j==0)){continue;}
				tmpIdx = idx(x+i, y+j);
				if((!cellMap[tmpIdx].isNextOcc()) && (cellMap[tmpIdx].setAdj())){
					adjFixedCells.put(tmpIdx, cellMap[tmpIdx]);
					res.add(tmpIdx);
				}
			}		
		}
		return res;
	}
	
	public void setCellAdj3D(int idx){
		int x = cellMap[idx].x,y = cellMap[idx].y,z = cellMap[idx].z,tmpIdx;
		for(int i = -1; i<2; ++i){
			for(int j = -1; j<2; ++j){
				for(int k = -1; k<2; ++k){
					if((i==0)&&(j==0)&&(k==0)){continue;}
					tmpIdx = idx(x+i, y+j, z+k);
					if(cellMap[tmpIdx].setAdj()){
						adjFixedCells.put(tmpIdx, cellMap[tmpIdx]);
					}
				}
			}		
		}
	}
	
	public ArrayList<Integer> setCellAdj3D_DBM(int idx){
		ArrayList<Integer> res = new ArrayList<Integer>();
		int x = cellMap[idx].x,y = cellMap[idx].y,z = cellMap[idx].z,tmpIdx;
		for(int i = -1; i<2; ++i){
			for(int j = -1; j<2; ++j){
				for(int k = -1; k<2; ++k){
					if((i==0)&&(j==0)&&(k==0)){continue;}
					tmpIdx = idx(x+i, y+j, z+k);
					if((!cellMap[tmpIdx].isNextOcc()) && (cellMap[tmpIdx].setAdj())){
						adjFixedCells.put(tmpIdx, cellMap[tmpIdx]);
						res.add(tmpIdx);
					}
				}
			}		
		}
		return res;
	}
	
	public ArrayList<Integer> setCellOcc2DDBM(int idx){
		ArrayList<Integer> res = new ArrayList<Integer>();
		fixedCells.put(idx, cellMap[idx]);
		synchronized(this) {		//only allow this to be accessed synchronously
			res = setCellAdj2D_DBM(idx);	
		}
		myCell resCell = this.adjFixedCells.remove(idx);
		return res;
	}

	public ArrayList<Integer> setCellOcc3DDBM(int idx){
		ArrayList<Integer> res = new ArrayList<Integer>();
		fixedCells.put(idx, cellMap[idx]);		
		synchronized(this) {		//only allow this to be accessed synchronously
			res = setCellAdj3D_DBM(idx);	
		}
		this.adjFixedCells.remove(idx);
		return res;
	}

	public void setCellOcc2D(int idx){
		fixedCells.put(idx, cellMap[idx]);
		synchronized(this) {		//only allow this to be accessed synchronously
			setCellAdj2D(idx);	
		}
		this.adjFixedCells.remove(idx);
	}

	public void setCellOcc3D(int idx){
		fixedCells.put(idx, cellMap[idx]);		
		synchronized(this) {		//only allow this to be accessed synchronously
			setCellAdj3D(idx);	
		}
		this.adjFixedCells.remove(idx);
	}
	
	public void initSingleSeed2D() {//initialize DLA for single seed value at center of grid
		cellMap[ctrIdx].setOcc();	setCellOcc2D(ctrIdx);
	}
	public void initSingleSeed3D() {//initialize DLA for single seed value at center of grid
		cellMap[ctrIdx].setOcc();	setCellOcc3D(ctrIdx);
	}
	//public abstract void drawTrail();
	//get 1-d idx from 2d or 3d idxs
	public int idx(int col, int row){return ((col + row*gridWidth + cellMap.length)%cellMap.length);}
	public int idx(int col, int row, int layer){return ((col + row*gridWidth + layer*(gridWidth *gridHeight)+cellMap.length)%cellMap.length);}
	
}//myCellGrid class
