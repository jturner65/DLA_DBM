package cs7492Project4;

import java.util.ArrayList;


//object on menu that can be modified via mouse input
public class myGUIObj {
	public int ID;
	public cs7492Proj4 p;
	public myVector start, end;				//x,y coords of start corner, end corner (z==0) for clickable region
	public String name, dispText;

	public double val;
	public final double minVal, maxVal;
	public boolean treatAsInt;
	public int _cVal;
	public double modMultBig, modMultSmall;						//multiplier for mod value
	public int[] bxclr;
	
	public myGUIObj(cs7492Proj4 _p,String _name, myVector _start, myVector _end, double _min, double _max, double _initVal, boolean _tAsInt, double _modMultS, double _modMultB) {
		p=_p;
		ID = p.GUIObjID++;
		name = _name;
		dispText = new String("UI Obj "+ID+" : "+name + " : ");
		start = new myVector(_start); end = new myVector(_end);
		minVal=_min; maxVal = _max; val = _initVal;
		treatAsInt = _tAsInt;
		_cVal = p.gui_Black;
		modMultBig = _modMultB;
		modMultSmall = _modMultS;
		bxclr = p.getRndClr();
	}	
	public myGUIObj(cs7492Proj4 _p, String _name,float _xst, float _yst, float _xend, float _yend, double _min, double _max, double _initVal, boolean _tAsInt, double _modMultS, double _modMultB) {this(_p,_name,new myVector(_xst,_yst,0), new myVector(_xend,_yend,0), _min, _max, _initVal, _tAsInt, _modMultS, _modMultB);	}
	
	public double getVal(){return val;}	
	
	public double setVal(double _newVal){
		val = ((_newVal > minVal)&&(_newVal<maxVal)) ? _newVal : (_newVal < minVal) ? minVal : maxVal;		
		return val;
	}	
	
	public double modVal(double mod, boolean isBig){
		double modMult = (isBig ? modMultBig : modMultSmall);
		val += (mod*modMult);
		if(treatAsInt){val = Math.round(val);}
		if(val<minVal){val = minVal;}
		else if(val>maxVal){val = maxVal;}
		return val;		
	}
	
	public int valAsInt(){return (int)(val) ;}
	public float valAsFloat(){return (float)( val);}
	public boolean clickIn(float _clkx, float _clky){return (_clkx > start.x)&&(_clkx < end.x)&&(_clky > start.y)&&(_clky < end.y);}
	public void draw(){
		p.pushMatrix();p.pushStyle();
			p.translate(start.x, start.y + p.yOff);
			p.setColorValFill(_cVal);
			p.setColorValStroke(_cVal);
			p.pushMatrix();p.pushStyle();
				p.noStroke();
				p.fill(bxclr[0],bxclr[1],bxclr[2],bxclr[3]);
				p.translate(-start.x * .5f, -p.yOff*.25f);
			p.box(5);
			p.popStyle();p.popMatrix();
			p.text(dispText + String.format("%.5f",val), 0,0);
		p.popStyle();p.popMatrix();
	}
		
	public String[] getStrData(){
		ArrayList<String> tmpRes = new ArrayList<String>();
		tmpRes.add("ID : "+ ID+" Name : "+ name + " distText : " + dispText);
		tmpRes.add("Start loc : "+ start + " End loc : "+ end + " Treat as Int  : " + treatAsInt);
		tmpRes.add("Value : "+ val +" Max Val : "+ maxVal + " Min Val : " + minVal+ " Mod mults : small : " + modMultSmall + " big : " + modMultBig);
		return tmpRes.toArray(new String[0]);
	}
}
