package cs7492Project4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;

import processing.core.PApplet;

public class myDBMSolver extends mySolver {
	//public ConcurrentSkipListMap<Integer,myCell> newAdjFixedCells;			//cells that are newly adj to newly fixed cells as substrate/potential - need to be added separately	
	public float minPhi, maxPhi;
	ConcurrentSkipListMap<Float, myCell> probListCells;
	
	public myDBMSolver(Project4 _p, int type) {
		super(_p, type);	
	}
	
	@Override
	public void initSolverSpecific() {
		//newAdjFixedCells = new ConcurrentSkipListMap<Integer,myCell>();
		ArrayList<Integer> tmpNewAdjCells = new ArrayList<Integer>(Arrays.asList(cellGrid.adjFixedCells.keySet().toArray(new Integer[1])));
		updateCellPotentialEq1(tmpNewAdjCells);	//find potential for adjacent cells to seed
		probListCells = new ConcurrentSkipListMap<Float, myCell>();
		minPhi = 0;
		maxPhi = 1;
		float totProb = calcAdjCellProbs(minPhi, maxPhi);
	}	
	//build cells specific to this solver
	@Override
	public void calcCPU() {//calculate DBM algorithm on the CPU
		setProbParam();//adjFixedCells
		probListCells.clear();
		float totProb = calcAdjCellProbs(minPhi, maxPhi);		//prob here takes the place of eta in the paper (exponent of probability weight calc)
		
		float sampleProb = (float) ThreadLocalRandom.current().nextDouble(totProb);
		float hiKey = (null != probListCells.higherKey(sampleProb) ? probListCells.higherKey(sampleProb) : probListCells.lastKey());
		myCell newLoc = probListCells.get(hiKey);
		addFixedCell(newLoc.idx);
	}
	
	public void addFixedCell(int newIdx){
		cellGrid.adjFixedCells.remove(newIdx);												//don't want to update this cell - it's  no longer a candidate anymore
		updateCellPotentialEq2(newIdx);
		cellGrid.cellMap[newIdx].setOcc();			
		ArrayList<Integer> tmpNewAdjCells = cellGrid.setCellOcc2DDBM(newIdx);				//determine new adj cells to this newly added cell
		updateCellPotentialEq1(tmpNewAdjCells);
		//find min and max potentials
		minPhi = 999999999; maxPhi = -99999999;
		for(myCell cAdj : cellGrid.adjFixedCells.values()){				
			minPhi = PApplet.min(minPhi, cAdj.ptnl);
			maxPhi = PApplet.max(maxPhi, cAdj.ptnl);
		}	
	}
	
	
	//calculate the potential at all cells in passed construct, by finding distance from every fixed cell in existing substrate and using eq1 to derive potential
	public void updateCellPotentialEq1(ArrayList<Integer> newCells){
		for(int i = 0; i < newCells.size(); ++i){
			myCell cNew = cellGrid.cellMap[newCells.get(i)];
			float newPt = 0;
			for(myCell cFix : cellGrid.fixedCells.values()){	
				float dist = dist2Cells(cNew.idx, cFix.idx);
				//if((int)dist == 1){System.out.println("Error in distance function for :"+cNew.idx + " with fixed : " + cFix.idx);}
				newPt += (1 - cellGrid.cellRad/dist);				
			}		
			cNew.ptnl = newPt;
		}	
		for(int i = 0; i < newCells.size(); ++i){
			myCell cNew = cellGrid.cellMap[newCells.get(i)];
			//System.out.println(""+ i + " : " + cNew);
		}			
	}
	
	//update potential of existing potential bonding cells by using newly added cell to add to old potential (eq2)
	public void updateCellPotentialEq2(int fixLocIdx){
		for(myCell cAdj : cellGrid.adjFixedCells.values()){				
			cAdj.ptnl += (1 - cellGrid.cellRad/dist2Cells(fixLocIdx, cAdj.idx));				
		}	
	}
	
	public float calcAdjCellProbs(float minPhi, float maxPhi){
		float diffPhi = maxPhi - minPhi, totPhi_eta = 0, res =0;
		//if(diffPhi == 0){System.out.println("Phi Diff 0 is bad");}//divide by 0
		for(myCell cAdj : cellGrid.adjFixedCells.values()){	
			if(cAdj._cSM[cAdj.isOcc]){System.out.println("Error : occ cell in adj cell list : "+ cAdj.idx);}
			cAdj.Phi_eta = (float)Math.pow((cAdj.ptnl - minPhi)/diffPhi,prob);
			totPhi_eta += cAdj.Phi_eta;
		}			
		for(myCell cAdj : cellGrid.adjFixedCells.values()){	
			cAdj.prob = cAdj.Phi_eta / totPhi_eta;
			res += cAdj.prob;
			probListCells.put(res, cAdj);
		}			
		return res;
	}
	
	public float dist2Cells(int aIdx, int bIdx){
		return cellGrid.cellMap[aIdx].loc._dist(cellGrid.cellMap[bIdx].loc);
	}
	
	@Override
	public String getProbStr() {	return String.format("%.1f",prob);}

	@Override
	public void drawSolverData() {
		p.drawBoldText(1.5f, p.gui_DarkRed ,"Type : "+ p.getTypeString(type) + "  " + probType +" = " + this.getProbStr());
//		p.translate(0, 20,0);
//		p.drawBoldText(1.5f, p.gui_DarkRed ,"Min Phi : "+ minPhi +" Max Phi : "+ maxPhi + " # adj cells : "+ cellGrid.adjFixedCells.size());
	}

	@Override
	public void setTmplSeed(myPoint pt) {
		int newIdx = cellGrid.idx((int)pt.x,(int)pt.y,(int)pt.z);
		addFixedCell(newIdx);												//don't want to update this cell - it's  no longer a candidate anymore
	}
}//myDBMSolver
