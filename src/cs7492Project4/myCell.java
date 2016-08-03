package cs7492Project4;

import processing.core.PApplet;
import processing.core.PConstants;

public class myCell {
	public Project4 p;	
	public mySolver slv;
	public final int idx, x, y, z,cellSize,			//idx, x and y position in cell grid
				offX, offY, offZ;			//pixel offset in x and y for upper left corner
	
	public myPoint loc;						//location of the center of this cell in world - used for distances in DBM calcs
	public int clr, slvrType;
	
	public boolean[] _cSM;						//cell-related flags
	public final int isOcc = 0,					//cell is occupied/fixed
					isNextOcc = 1,				//cell is adjacent to fixed cell - this is where new cells will be set
					isFrontier = 2;

	public final int numFlags = 3;
	
	public int[] trailIdxs;						//for DLA brownies motion
	//for DBM
	public float ptnl,							//potential at this location based on all fixed cells
				prob,							//prob of landing in this cell
				Phi_eta;						//Cap Phi for this cell raised to eta
	
	public myCell(Project4 _p, mySolver _rs, int _idx, int _x, int _y, int _z){
		p = _p;
		slv = _rs;
		idx = _idx;
		slvrType = slv.type;
		cellSize = slv.cellSize;
		ptnl = 0;
		x = _x;		y = _y;		z = _z;
		offX = x * cellSize;	offY = y * cellSize;	offZ = z * cellSize;		
		float csHalf = .5f*cellSize;
		loc = new myPoint(offX+csHalf,offY+csHalf, offZ+csHalf);
		clr = p.gui_Black;				
		initFlags();
	}//constructor
	
	public void initFlags(){
		_cSM = new boolean[numFlags];
		for(int i = 0; i<numFlags; ++i){_cSM[i]=false;}		
	}
	
	//set this cell to be "occupied"/fixed
	public void setOcc(){
		synchronized(this) {		//only allow this to be accessed synchronously
			_cSM[isOcc] = true;this.clr = p.gui_White; 
			_cSM[isNextOcc] = false;ptnl = 0;
		}
	}
	public boolean setAdj(){if(!_cSM[isOcc]){_cSM[isNextOcc] = true; } else {setOcc();}return _cSM[isNextOcc];}
	public void setFrontier(){_cSM[isFrontier] = true;	ptnl = 1;}
	
	public boolean isNextOcc(){ return _cSM[isNextOcc];	}
	
	public void draw2D(){
		p.pushMatrix();p.pushStyle();
		p.setColorValFill(clr);
		p.noStroke();
		drawSqr();
		p.popStyle();p.popMatrix();				
	}
	
	//draw this cell with a specific color - for trails, adj zone, etc
	public void draw2D(int _clr){
		p.pushMatrix();p.pushStyle();
		p.setColorValFill( _clr);
		p.noStroke();
		drawSqr();
		p.popStyle();p.popMatrix();			
	}
	
	//draw this cell with a specific color - for trails, adj zone, etc
	public void draw2D(int[] _clr){
		p.pushMatrix();p.pushStyle();
		p.setFill( _clr,_clr[3]);
		p.noStroke();
		drawSqr();
		p.popStyle();p.popMatrix();			
	}
	
	private void drawSqr(){
		p.translate(offX, offY);
		p.beginShape(PApplet.QUADS);
		p.vertex(0, 0);
		p.vertex(0, cellSize);
		p.vertex(cellSize, cellSize);
		p.vertex(cellSize, 0);
		p.endShape(PConstants.CLOSE);	
	}
	
	public void draw3D(){
		p.pushMatrix();p.pushStyle();
		p.setColorValFill(clr);
		p.noStroke();
		drawCube();
		p.popStyle();p.popMatrix();				
	}
	
	//draw this cell with a specific color - for trails, adj zone, etc
	public void draw3D(int _clr){
		p.pushMatrix();p.pushStyle();
		p.setColorValFill( _clr);
		p.noStroke();
		drawCube();
		p.popStyle();p.popMatrix();			
	}
	
	//draw this cell with a specific color - for trails, adj zone, etc
	public void draw3D(int[] _clr){
		p.pushMatrix();p.pushStyle();
		p.setFill( _clr,_clr[3]);
		p.noStroke();
		drawCube();
		p.popStyle();p.popMatrix();			
	}
	
	private void drawCube(){
		p.translate(offX, offY, offZ);
		p.box(cellSize);	
	}
	
		
	public String toString(){
		String result = "";
		result += "(" + x + ", " + y + ") IDX: "+idx+ " |Ptnl : "+ String.format("%.3f",ptnl) + " | Prob : "+ String.format("%.3f",prob) + " | CapPhi : " + String.format("%.5f",Phi_eta);
		return result;
	}
	
}//myCell class
