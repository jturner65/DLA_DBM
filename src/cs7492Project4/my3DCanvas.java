package cs7492Project4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import processing.core.PMatrix3D;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;

public class my3DCanvas {
	public Project4 p;
	
	public myPoint drawEyeLoc,													//rx,ry,dz coords where eye was when drawing - set when first drawing and return eye to this location whenever trying to draw again - rx,ry,dz
		scrCtrInWorld,mseLoc, eyeInWorld, oldDfCtr, mseIn3DBox, dfCtr;											//mouse location projected onto current drawing canvas

	public edge camEdge;												//denotes line perp to cam eye, to use for intersections for mouse selection
	public final float canvasDim = 1500; 									//canvas dimension for "virtual" 3d		
	public myPoint[] canvas3D;											//3d plane, normal to camera eye, to be used for drawing - need to be in "view space" not in "world space", so that if camera moves they don't change
	public myVector eyeToMse, 
					eyeToCtr,													//vector from eye to center of cube, to be used to determine which panels of bounding box to show or hide
					canvasNorm, 												//normal of eye-to-mouse toward scene, current drawn object's normal to canvas
					drawSNorm;													//current normal of viewport/screen
	
	public int viewDimW, viewDimH;
	public my3DCanvas(Project4 _p) {
		p = _p;
		viewDimW = p.width; viewDimH = p.height;
		initCanvasOneTime();	
	}
	
	public void initCanvasOneTime(){
		canvas3D = new myPoint[4];		//3 points to define canvas
		canvas3D[0]=new myPoint();canvas3D[1]=new myPoint();canvas3D[2]=new myPoint();canvas3D[3]=new myPoint();		
		camEdge = new edge(p);	
		initCanvas();
	}//initCanvasOneTime
	
	public void initCanvas(){
		drawEyeLoc = new myPoint(-1, -1, -1000);
		eyeInWorld = new myPoint();		
		scrCtrInWorld = new myPoint();									//
		mseLoc = new myPoint();
		eyeInWorld = new myPoint();
		oldDfCtr  = new myPoint();
		mseIn3DBox = new myPoint();
		dfCtr = new myPoint();											//mouse location projected onto current drawing canvas
		eyeToMse = new myVector();		
		eyeToCtr = new myVector();	
		drawSNorm = new myVector();	
		canvasNorm = new myVector(); 						//normal of eye-to-mouse toward scene, current drawn object's normal to canvas	
	}

		//find points to define plane normal to camera eye, at set distance from camera, to use drawing canvas 	
	public void buildCanvas(){
		mseLoc = MouseScr();		
		scrCtrInWorld = pick(viewDimW/2, viewDimH/2);		
		myVector A = new myVector(scrCtrInWorld,  pick(viewDimW, -viewDimH)),	B = new myVector(scrCtrInWorld,  pick(viewDimW, 0));	//ctr to upper right, ctr to lower right		
		drawSNorm = myVector._cross(A,B)._normalize();				 													//normal to canvas that is colinear with view normal to ctr of screen
		eyeInWorld = myPoint._add(new myPoint(scrCtrInWorld), myPoint._dist( pick(0,0), scrCtrInWorld), drawSNorm);								//location of "eye" in world space
		eyeToCtr = new myVector(eyeInWorld, new myPoint(0,0,0));
		eyeToMse = new myVector(eyeInWorld, mseLoc);		//unit vector in world coords of "eye" to mouse location
		eyeToMse._normalize();
		myVector planeTan = myVector._cross(drawSNorm, myVector._normalize(new myVector(drawSNorm.x+10,drawSNorm.y+10,drawSNorm.z+10)))._normalize();			//result of vector crossed with normal will be in plane described by normal
     	for(int i =0;i<canvas3D.length;++i){
     		canvas3D[i] = new myPoint(myVector._mult(planeTan, canvasDim));
     		planeTan = myVector._cross(drawSNorm, planeTan)._normalize();												//this effectively rotates around center point by 90 degrees -builds a square
     	}
     	oldDfCtr = new myPoint(dfCtr);
     	dfCtr = getPlInterSect(mseLoc);
     	mseIn3DBox = new myPoint(dfCtr.x+p.grid3DDimX/2.0f,dfCtr.y+p.grid3DDimY/2.0f,dfCtr.z+p.grid3DDimZ/2.0f);
     	p.drawMseEdge();
	}//buildCanvas()
	
	//find pt in drawing plane that corresponds with mouse location and camera eye normal
	public myPoint getPlInterSect(myPoint pt){
		myPoint dctr = new myPoint(0,0,0);	//actual click location on visible plane
		 // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		p.intersectPl(pt, eyeToMse, canvas3D[0],canvas3D[1],canvas3D[2],  dctr);//find point where mouse ray intersects canvas
		return dctr;		
	}//getPlInterSect	
	
	public myPoint pick(int mX, int mY){
		PGL pgl = p.beginPGL();
		FloatBuffer depthBuffer = ByteBuffer.allocateDirect(1 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		int newMy = viewDimW - mY;
		//pgl.readPixels(mX, grid3DDimY - mY - 1, 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, depthBuffer);
		pgl.readPixels(mX, newMy, 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, depthBuffer);
		float depthValue = depthBuffer.get(0);
		depthBuffer.clear();
		p.endPGL();
	
		//get 3d matrices
		PGraphics3D p3d = (PGraphics3D)p.g;
		PMatrix3D proj = p3d.projection.get();
		PMatrix3D modelView = p3d.modelview.get();
		PMatrix3D modelViewProjInv = proj; modelViewProjInv.apply( modelView ); modelViewProjInv.invert();
	  
		float[] viewport = {0, 0, viewDimW, viewDimH};
		//float[] viewport = {0, 0, p3d.width, p3d.height};
		  
		float[] normalized = new float[4];
		normalized[0] = ((mX - viewport[0]) / viewport[2]) * 2.0f - 1.0f;
		normalized[1] = ((newMy - viewport[1]) / viewport[3]) * 2.0f - 1.0f;
		normalized[2] = depthValue * 2.0f - 1.0f;
		normalized[3] = 1.0f;
	  
		float[] unprojected = new float[4];
	  
		modelViewProjInv.mult( normalized, unprojected );
		return new myPoint( unprojected[0]/unprojected[3], unprojected[1]/unprojected[3], unprojected[2]/unprojected[3] );
	}		
	public myPoint MouseScr() {return pick(p.mouseX,p.mouseY);} 
	
}
//line bounded by verts - from a to b new myPoint(x,y,z); 
class edge{ 
	public Project4 p;
	public myPoint a, b;
	public edge (Project4 _p){this(_p,new myPoint(0,0,0),new myPoint(0,0,0));}
	public edge (Project4 _p, myPoint _a, myPoint _b){p = _p;a=new myPoint(_a); b=new myPoint(_b);}
	public void set(float d, myVector dir, myPoint _p){	set( myPoint._add(_p,-d,new myVector(dir)), myPoint._add(_p,d,new myVector(dir)));} 
	public void set(myPoint _a, myPoint _b){a=new myPoint(_a); b=new myPoint(_b);}
	public myVector v(){return new myVector(b.x-a.x, b.y-a.y, b.z-a.z);}			//vector from a to b
	public myVector dir(){return v()._normalize();}
	public double len(){return  myPoint._dist(a,b);}
	public double distFromPt(myPoint P) {return myVector._det3(dir(),new myVector(a,P)); };
	public void drawMe(){p.line(a.x,a.y,a.z,b.x,b.y,b.z); }
    public String toString(){return "a:"+a+" to b:"+b+" len:"+len();}
}
