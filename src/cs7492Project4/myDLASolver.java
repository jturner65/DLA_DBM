package cs7492Project4;

import java.util.*;
import java.util.concurrent.*;

import processing.core.PApplet;


public class myDLASolver extends mySolver {
	
	public int numWalkers = 1000;
	
	public myWalker[] walkerIDXs;		//current idx's in cellgrid holding a walker
		
	public final int mtFrameSize = 100;			//# of walkers to send to each thread
	public myMarchingCubes MC;											//to render 3d results using marchingcubes alg

	public myDLASolver(cs7492Proj4 _p, int type) {
		super(_p, type);	
		MC = new myMarchingCubes(p, cell3dSize);
	}

	@Override
	public void initSolverSpecific() {
		numWalkers =  (p.flags[p.solve3D] ? 2000 : 1000);
		walkerIDXs = new myWalker[numWalkers];	
		for(int i =0; i<numWalkers; ++i){walkerIDXs[i] = makeNewRandLocWalker();}
		MC.initData();
	}
	@Override
	//set values to be used by MC to display results
	public void setMCVal(int idx, float val){
		MC.updateCellData(idx, val);
	}
	@Override
	public void calcCPU(){	
		setProbParam();	//get current probability value from UI
		callMTCalcs.clear();
		int frntSz = cellGrid.frontier.size();
		myWalker[] tmpList;		
		for(int c = 0; c < walkerIDXs.length; c+=mtFrameSize){
			int finalLen = (c+mtFrameSize < walkerIDXs.length ? mtFrameSize : walkerIDXs.length - c);
			tmpList = new myWalker[finalLen];
			System.arraycopy(walkerIDXs, c, tmpList, 0, finalLen);		
			callMTCalcs.add(new myDLAWalker(p, this, cellGrid,tmpList, prob,frntSz, p.guiObjs[p.gIDX_RndWlkTail].valAsInt()));
		}							//find next turn's motion for every creature by finding total force to act on creature
		try {
			callMTFutures = p.th_exec.invokeAll(callMTCalcs);
			for(Future<Boolean> f: callMTFutures) { 
				boolean res;
				res = f.get(); 
			}
		} catch (Exception e) { e.printStackTrace(); }		

	}
	
	public void draw2DCPU_Res(){
		super.draw2DCPU_Res();
		if(p.flags[p.showWalkers]){	
			p.pushMatrix();
			for(int i = 0; i< walkerIDXs.length; ++i){walkerIDXs[i].drawMe2D();}//   cellGrid.cellMap[walkerIDXs[i]].draw2DTrail(new int[]{255,255,0,255});}
			p.popMatrix();
		}
	}
	
	public void draw3DCPU_Res(){
		if(p.flags[p.useMCDisp]){	
			p.pushMatrix();p.pushStyle();
			MC.updateMTGrid();
			MC.draw();
			p.popStyle();p.popMatrix();			
		} else {
			super.draw3DCPU_Res();
			if(p.flags[p.showWalkers]){	
				p.pushMatrix();
				for(int i = 0; i< walkerIDXs.length; ++i){walkerIDXs[i].drawMe3D();}//   cellGrid.cellMap[walkerIDXs[i]].draw2DTrail(new int[]{255,255,0,255});}
				p.popMatrix();
			}
		}
	}
	//length of trails for walkers
	public void resetTrailLength(int trLen){for(int t = 0; t< walkerIDXs.length; ++t){walkerIDXs[t].resetTrail(trLen);}}
	public void setIsoLevel(double _isoLvl){MC.isolevel = ((float)_isoLvl);}// p.outStr2Scr("Iso Level " + MC.isolevel);}
	public myWalker makeNewWalker(){return new myWalker(p,cellGrid, cellGrid.frontier.get(ThreadLocalRandom.current().nextInt(cellGrid.frontier.size())), p.guiObjs[p.gIDX_RndWlkTail].valAsInt());}	
	public myWalker makeNewRandLocWalker(){return new myWalker(p,cellGrid,ThreadLocalRandom.current().nextInt(cellGrid.cellMap.length),p.guiObjs[p.gIDX_RndWlkTail].valAsInt());}	
	@Override
	public String getProbStr() {return String.format("%.3f",prob);}

	@Override
	public void drawSolverData() {
		p.drawBoldText(2.0f, p.gui_DarkGreen ,"Type : "+ p.getTypeString(type) + "  " + probType +" = " + this.getProbStr());
	}

	@Override
	public void setTmplSeed(myPoint pt) {		
		int idx = cellGrid.idx((int)pt.x,(int)pt.y,(int)pt.z);
		cellGrid.cellMap[idx].setOcc();
		cellGrid.setCellOcc2D(idx);				
	}
}

class myWalker{
	public cs7492Proj4 p;
	public myCellGrid cellGrid;
	public int idx, trLen;
	public int[] trail;
	public boolean drawTail;
	public myWalker(cs7492Proj4 _p, myCellGrid _cellGrid, int _idx, int _trLen){ 
		p = _p; cellGrid = _cellGrid; idx = _idx; trLen = _trLen;
		trail = new int[trLen];
		Arrays.fill(trail, idx);		
	}	
	
	public void setNewLocAndTrailHead(int newLoc){//add idx to head of trail
		int[] tmpAra = new int[trail.length];		
		tmpAra[0] = idx;		//add current idx to head
		System.arraycopy(trail, 0, tmpAra, 1, trail.length-1);		
		trail = tmpAra;		
		idx = newLoc;
	}
	public boolean trailHasIdx(int _idx){for(int i =0; i<trail.length; ++i){	if(trail[i]==_idx){return true;}}return false;}
	
	public void resetTrail(int trLen){
		int trailLength = trail.length,cpyLen = PApplet.min(trailLength, trLen);
		int[] tmpAra = new int[trLen];
		Arrays.fill(tmpAra, trail[trail.length-1]);//init ara to hold last value of current trail array - have tail increasing in length originate from last val in old tail
		System.arraycopy(trail, 0, tmpAra, 0, cpyLen);		
		trailLength = trLen;
		trail = tmpAra;
	}
	public void drawMe2D() {
		p.pushMatrix();
		cellGrid.cellMap[idx].draw2D(new int[]{255,255,0,255});
		p.popMatrix();
		if(p.flags[p.showDLATrails]){		
			p.pushMatrix();
			for(int i = 1; i<trail.length; ++i){
				cellGrid.cellMap[trail[i]].draw2D(new int[]{0,(1-(i/trail.length)) * 255,(1-(i/trail.length)) * 255,255});			
			}
			p.popMatrix();
		}
	}	
	public void drawMe3D() {
		p.pushMatrix();
		cellGrid.cellMap[idx].draw3D(new int[]{255,255,0,30});
		p.popMatrix();
		if(p.flags[p.showDLATrails]){			
			p.pushMatrix();
			for(int i = 1; i<trail.length; ++i){
				cellGrid.cellMap[trail[i]].draw3D(new int[]{0,(1-(i/trail.length)) * 255,(1-(i/trail.length)) * 255,30});			
			}
			p.popMatrix();
		}
	}	
	public void drawTrail() {
		p.pushMatrix();
		for(int i = 1; i<trail.length; ++i){
			cellGrid.cellMap[trail[i]].draw2D(new int[]{0,(1-(i/trail.length)) * 255,(1-(i/trail.length)) * 255,255});			
		}
		p.popMatrix();
	}
	
}//myWalker
