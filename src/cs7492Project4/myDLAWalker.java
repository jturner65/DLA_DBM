package cs7492Project4;


import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

//multithread class to execute sample walker
public class myDLAWalker implements Callable<Boolean> {
	public cs7492Proj4 p;
	// structure holding cells in grid
	public myCellGrid cellGrid;	
	public myDLASolver slvr;
	public myWalker[] walkerIDXs;
	
	public final int iters = 1000;
	
	public float stick;
	public int frntSz, trlLen, gridWidth, gridHeight, gridDepth, gwgh, cellMapLen;

	public myDLAWalker(cs7492Proj4 _p, myDLASolver _slv, myCellGrid _grd, myWalker[] _walkers, float _stk, int _frntSz, int _trlLen) {
		p = _p; slvr = _slv; cellGrid = _grd; walkerIDXs = _walkers; stick = _stk;frntSz = _frntSz; trlLen=_trlLen;
		gridWidth = cellGrid.gridWidth;		gridHeight = cellGrid.gridHeight;		gridDepth = cellGrid.gridDepth;
		gwgh = gridWidth*gridHeight;
		cellMapLen = cellGrid.cellMap.length;
	}

	public void resetWalker(myWalker w){
		w.idx = cellGrid.frontier.get(ThreadLocalRandom.current().nextInt(frntSz));
		w.setNewLocAndTrailHead(w.idx);
	}
	//public abstract void drawTrail();
	//get 1-d idx from 2d or 3d idxs
	public int idx(int col, int row, int layer){return ((col + row*gridWidth + (layer*gwgh)+cellMapLen)%cellMapLen);}
	
	//return idx in cellgrid of new location for given initial loc idx and direction dir :
	//dir is >0 for increase, <0 for decrease
	//axis is 1 for x, 2 for y, 3 for z 
	public int modLoc3D(int idxVal, int dir, int axis){
		int x = idxVal % gridWidth, y = (idxVal/gridWidth) % gridHeight, z = (idxVal/gwgh)%gridDepth;
		switch(axis){
		case 0 : {return idx((x + dir + gridWidth)%gridWidth, y,z);}
		case 1 : {return idx(x, (y + dir + gridHeight)%gridHeight,z);}
		case 2 : {return idx(x, y, (z + dir + gridDepth)%gridDepth);}
		default : {return idxVal;}
		}
	}
	public void run2D(){
		int newIdx, randVal;
		//for each walker
		for(int iter = 0; iter<iters; ++iter){
			for(int i =0; i<walkerIDXs.length; ++i){
				randVal = ThreadLocalRandom.current().nextInt(8); 
				newIdx = modLoc3D(walkerIDXs[i].idx, ((randVal & 4) == 4 ? 1 : -1), (randVal & 1));          			//random walk : new_idx = modLoc2D(oldIdx, dir(+/- 1), axis(1,2)){
				if((cellGrid.adjFixedCells.containsKey(newIdx)) || (cellGrid.fixedCells.containsKey(newIdx))){
					if(ThreadLocalRandom.current().nextDouble(1.0) < stick){
						cellGrid.cellMap[newIdx].setOcc();
						cellGrid.setCellOcc2D(newIdx);
						resetWalker(walkerIDXs[i]);
						if(cellGrid.frontier.contains(newIdx)){	p.setFlags(p.runSim,false); }//return;}			//stop when we hit frontier
					} else {
						walkerIDXs[i].setNewLocAndTrailHead(newIdx);  
					}					
				} 
				else  {												walkerIDXs[i].setNewLocAndTrailHead(newIdx);        }
			}
		}
	}	
	public void run3D(){
		int newIdx, randVal;
		//for each walker 
		int tIters = iters/10;
		for(int iter = 0; iter<tIters; ++iter){//run iters times per frame
			for(int i =0; i<walkerIDXs.length; ++i){
				randVal = ThreadLocalRandom.current().nextInt(24); //rand3 = ThreadLocalRandom.current().nextInt(3);		//maybe replace with nextInt(24) to use only 1 random gen
				newIdx = modLoc3D(walkerIDXs[i].idx, ((randVal & 4) == 4 ? 1 : -1), (randVal/8));          			//random walk : new_idx = modLoc2D(oldIdx, dir(+/- 1), axis(1,2)){
				//System.out.println("randval:"+randVal+" +/- :"+((randVal & 4) == 4 ?"+" : "-") +"" +(randVal%3));				
				if((cellGrid.adjFixedCells.containsKey(newIdx)) || (cellGrid.fixedCells.containsKey(newIdx))){
					if(ThreadLocalRandom.current().nextDouble(1.0) < stick){//setting cell occ
						cellGrid.cellMap[newIdx].setOcc();
						cellGrid.setCellOcc3D(newIdx);
						resetWalker(walkerIDXs[i]);
						if(cellGrid.frontier.contains(newIdx)){	p.setFlags(p.runSim,false); }//return;}			//stop when we hit frontier
					} else {
						walkerIDXs[i].setNewLocAndTrailHead(newIdx);  
					}					
				} 
				else  {												walkerIDXs[i].setNewLocAndTrailHead(newIdx);        }
			}
		}
	}
	@Override
	public Boolean call() throws Exception {		
		if(!p.flags[p.solve3D] ) {	run2D();} 
		else {		run3D();		}
		return true;
	}
}
