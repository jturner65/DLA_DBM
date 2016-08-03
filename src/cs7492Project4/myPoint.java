package cs7492Project4;

public class myPoint {
	public float x,y,z;
	public static final myPoint ZEROPT = new myPoint(0,0,0);

	myPoint(float _x, float _y, float _z){this.x = _x; this.y = _y; this.z = _z;}         //constructor 3 args  
	myPoint(myPoint p){ this(p.x, p.y, p.z); }                                                                                                           	//constructor 1 arg  
	myPoint(myPoint A, myVector B) {this(A.x+B.x,A.y+B.y,A.z+B.z); };
	
	myPoint(myPoint A, float s, myPoint B) {this(A.x+s*(B.x-A.x),A.y+s*(B.y-A.y),A.z+s*(B.z-A.z)); };		//builds a point somewhere in between a and b
	myPoint(){ this(0,0,0);}                                                                                                                               //constructor 0 args
	
	public void set(float _x, float _y, float _z){ this.x = _x;  this.y = _y;  this.z = _z; }                                               //set 3 args 
	public void set(myPoint p){ this.x = p.x; this.y = p.y; this.z = p.z; }                                                                   //set 1 args
	public void set(float _x, float _y, float _z, float _sqMagn){ this.x = _x;  this.y = _y;  this.z = _z; }                                                                     //set 3 args 
	
	public myPoint _mult(float n){ this.x *= n; this.y *= n; this.z *= n; return this; }                                                     //_mult 3 args  
	public static myPoint _mult(myPoint p, float n){ myPoint result = new myPoint(p.x * n, p.y * n, p.z * n); return result;}                          //1 pt, 1 float
	public static void _mult(myPoint p, myPoint q, myPoint r){ myPoint result = new myPoint(p.x *q.x, p.y * q.y, p.z * q.z); r.set(result);}           //2 pt src, 1 pt dest  

	public void _div(float q){this.x /= q; this.y /= q; this.z /= q; }  
	public static myPoint _div(myPoint p, float n){ if(n==0) return p; myPoint result = new myPoint(p.x / n, p.y / n, p.z / n); return result;}                          //1 pt, 1 float
	
	public void _add(float _x, float _y, float _z){ this.x += _x; this.y += _y; this.z += _z;   }                                            //_add 3 args
	public void _add(myPoint v){ this.x += v.x; this.y += v.y; this.z += v.z;   }                                                 //_add 1 arg  
	public static myPoint _add(myPoint O, myVector I){														return new myPoint(O.x+I.x,O.y+I.y,O.z+I.z);}  
	public static myPoint _add(myPoint O, float a, myVector I){												return new myPoint(O.x+a*I.x,O.y+a*I.y,O.z+a*I.z);}                						//2 vec
	public static myPoint _add(myPoint O, float a, myVector I, float b, myVector J) {						return new myPoint(O.x+a*I.x+b*J.x,O.y+a*I.y+b*J.y,O.z+a*I.z+b*J.z);}  					// O+xI+yJ
	public static myPoint _add(myPoint O, float a, myVector I, float b, myVector J, float c, myVector K) {	return new myPoint(O.x+a*I.x+b*J.x+c*K.x,O.y+b*I.y+b*J.y+c*K.y,O.z+b*I.z+b*J.z+c*K.z);} // O+xI+yJ+kZ
	
	//public static void _add(myPoint p, myPoint q, myPoint r){ myPoint result = new myPoint(p.x + q.x, p.y + q.y, p.z + q.z); r.set(result);}       	//2 pt src, 1 pt dest  
	public static myPoint _add(myPoint p, myPoint q, myPoint r){ myPoint result = new myPoint(p.x + q.x + r.x, p.y + q.y+ r.y, p.z + q.z+ r.z); return result;}       	//2 pt src, 1 pt dest  
	
	public void _sub(float _x, float _y, float _z){ this.x -= _x; this.y -= _y; this.z -= _z;  }                                                                   //_sub 3 args
	public void _sub(myPoint v){ this.x -= v.x; this.y -= v.y; this.z -= v.z;  }                                                                           //_sub 1 arg 
	public static void _sub(myPoint p, myPoint q, myPoint r){ myPoint result = new myPoint(p.x - q.x, p.y - q.y, p.z - q.z); r.set(result);}       //2 pt src, 1 pt dest  	

	public myPoint cloneMe(){myPoint retVal = new myPoint(this.x, this.y, this.z); return retVal;}  
	
	public float _L1Dist(myPoint q){return Math.abs((this.x - q.x) + (this.y - q.y) + (this.z - q.z)); }
	public static float _L1Dist(myPoint q, myPoint r){ return Math.abs((r.x - q.x) + (r.y - q.y) + (r.z - q.z));}
	
	public float _SqrDist(myPoint q){ return (((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z))); }
	public static float _SqrDist(myPoint q, myPoint r){  return (((r.x - q.x) *(r.x - q.x)) + ((r.y - q.y) *(r.y - q.y)) + ((r.z - q.z) *(r.z - q.z)));}
	
	public float _dist(myPoint q){ return (float)Math.sqrt( ((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z)) ); }
	public static float _dist(myPoint q, myPoint r){  return (float)Math.sqrt(((r.x - q.x) *(r.x - q.x)) + ((r.y - q.y) *(r.y - q.y)) + ((r.z - q.z) *(r.z - q.z)));}
	
	public float _dist(float qx, float qy, float qz){ return (float)Math.sqrt( ((this.x - qx)*(this.x - qx)) + ((this.y - qy)*(this.y - qy)) + ((this.z - qz)*(this.z - qz)) ); }
	public static float _dist(myPoint r, float qx, float qy, float qz){  return(float) Math.sqrt(((r.x - qx) *(r.x - qx)) + ((r.y - qy) *(r.y - qy)) + ((r.z - qz) *(r.z - qz)));}	
	
	
	/**
	 * returns if this pttor is equal to passed pttor
	 * @param b myPoint to check
	 * @return whether they are equal
	 */
	public boolean equals(Object b){
		if (this == b) return true;
		if (!(b instanceof myPoint)) return false;
		myPoint v = (myPoint)b;
		return ((this.x == v.x) && (this.y == v.y) && (this.z == v.z));		
	}				
	
	public String toString(){return "|(" + this.x + ", " + this.y + ", " + this.z+")";}
}
