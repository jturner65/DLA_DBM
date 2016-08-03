package cs7492Project4;

import processing.core.PApplet;

/**
*  replacement class for pvector 
*  keeps sqmagnitude, and magnitude
*/
public class myVector extends myPoint{
	public float sqMagn, magn;
	
		//vector constants available to all consumers of myVector
	public static final myVector ZEROVEC = new myVector(0,0,0);
//	public static final myVector UP	=	new myVector(0,1,0);
//	public static final myVector RIGHT = new myVector(1,0,0);
//	public static final myVector FORWARD = new myVector(0,0,1);
	public static final myVector UP	=	new myVector(0,0,1);
	public static final myVector RIGHT = new myVector(0,1,0);
	public static final myVector FORWARD = new myVector(1,0,0);
	
	myVector(float _x, float _y, float _z){super(_x,_y,_z); this._mag();}         //constructor 3 args  
	myVector(myVector p){ this(p.x, p.y, p.z); }                                                                                                           	//constructor 1 arg  
	myVector(){ this(0,0,0);}                                                                                                                               //constructor 0 args
	myVector(myPoint a, myPoint b){this(b.x-a.x,b.y-a.y,b.z-a.z);}			//vector from a->b
	myVector(myPoint a){this(a.x,a.y,a.z);}			//vector from a->b
	
	myVector(myVector a, float _y, myVector b) {super(a,_y,b);this._mag();	}//interp cnstrctr
	public void set(float _x, float _y, float _z){ super.set(_x, _y, _z); this._mag(); }                                               //set 3 args 
	public void set(myVector p){ this.x = p.x; this.y = p.y; this.z = p.z;  this._mag();}                                                                   //set 1 args
	public void set(float _x, float _y, float _z, float _sqMagn){ super.set(_x, _y, _z); this.sqMagn = _sqMagn; }                                                                     //set 3 args 
	
	public myVector _mult(float n){ super._mult(n); this._mag(); return this; }                                                     //_mult 3 args  
	public static myVector _mult(myVector p, float n){ myVector result = new myVector(p.x * n, p.y * n, p.z * n); return result;}                          //1 vec, 1 float
	public static myVector _mult(myVector p, myVector q){ myVector result = new myVector(p.x *q.x, p.y * q.y, p.z * q.z); return result;}                   //2 vec
	public static void _mult(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x *q.x, p.y * q.y, p.z * q.z); r.set(result);}           //2 vec src, 1 vec dest  

	public void _div(float q){super._div(q); this._mag();}  
	public static myVector _div(myVector p, float n){ if(n==0) return p; myVector result = new myVector(p.x / n, p.y / n, p.z / n); return result;}                          //1 pt, 1 float
	
	public void _add(float _x, float _y, float _z){ super._add(_x, _y, _z); this._mag(); }                                            //_add 3 args
	public void _add(myVector v){ this.x += v.x; this.y += v.y; this.z += v.z;  this._mag();  }                                                 //_add 1 arg  
	public static myVector _add(myVector p, myVector q){ myVector result = new myVector(p.x + q.x, p.y + q.y, p.z + q.z); return result;}                	//2 vec
	public static void _add(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x + q.x, p.y + q.y, p.z + q.z); r.set(result);}       	//2 vec src, 1 vec dest  
	
	public void _sub(float _x, float _y, float _z){ super._sub(_x, _y, _z);  this._mag(); }                                                                   //_sub 3 args
	public void _sub(myVector v){ this.x -= v.x; this.y -= v.y; this.z -= v.z;  this._mag(); }                                                                           //_sub 1 arg 
	public static myVector _sub(myPoint p, myPoint q){ return new myVector(p.x - q.x, p.y - q.y, p.z - q.z);}					             //2 pts or 2 vectors or any combo
	public static void _sub(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x - q.x, p.y - q.y, p.z - q.z); r.set(result);}       //2 vec src, 1 vec dest  
	
	public float _mag(){ this.magn = (float)Math.sqrt(this._SqMag()); return magn; }  
	public float _SqMag(){ this.sqMagn =  ((this.x*this.x) + (this.y*this.y) + (this.z*this.z)); return this.sqMagn; }  							//squared magnitude
	
	public void _scale(float _newMag){this._normalize()._mult(_newMag);}
	
	public myVector _normalize(){this._mag();if(magn==0){return this;}this.x = this.x/magn; this.y = this.y/magn; this.z = this.z/magn; return this;}
	public static myVector _normalize(myVector v){float magn = v._mag(); if(magn==0){return v;} myVector newVec = new myVector( v.x /= magn, v.y /= magn, v.z /= magn); newVec._mag(); return newVec;}
	
	public float _norm(){return _mag();}
	public static float _L2Norm(myVector v){return (float)Math.sqrt(v._SqMag());}
	public static float _L2SqNorm(myVector v){return v._SqMag();}
	
	public myVector _normalized(){float magn = this._mag(); myVector newVec = (magn == 0) ? (new myVector(0,0,0)) : (new myVector( this.x /= magn, this.y /= magn, this.z /= magn)); newVec._mag(); return newVec;}

	public myVector cloneMe(){myVector retVal = new myVector(this.x, this.y, this.z); return retVal;}  
	
	public float _L1Dist(myVector q){return Math.abs((this.x - q.x) + (this.y - q.y) + (this.z - q.z)); }
	public static float _L1Dist(myVector q, myVector r){ return Math.abs((r.x - q.x) + (r.y - q.y) + (r.z - q.z));}
	
	public float _SqrDist(myVector q){ return (((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z))); }
	public static float _SqrDist(myVector q, myVector r){  return ((r.x - q.x)*(r.x - q.x)) + ((r.y - q.y)*(r.y - q.y)) + ((r.z - q.z)*(r.z - q.z));}
	
	public float _dist(myVector q){ return (float)Math.sqrt( ((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z)) ); }
	public static float _dist(myVector q, myVector r){  return (float)Math.sqrt(((r.x - q.x) *(r.x - q.x)) + ((r.y - q.y) *(r.y - q.y)) + ((r.z - q.z) *(r.z - q.z)));}
	
	public static float _dist(myVector r, float qx, float qy, float qz){  return (float)Math.sqrt(((r.x - qx) *(r.x - qx)) + ((r.y - qy) *(r.y - qy)) + ((r.z - qz) *(r.z - qz)));}	
	
	public myVector _cross(myVector b){ return new myVector((this.y * b.z) - (this.z*b.y), (this.z * b.x) - (this.x*b.z), (this.x * b.y) - (this.y*b.x));}		//cross product 
	public static myVector _cross(myVector a, myVector b){		return a._cross(b);}
	public static myVector _cross(float ax, float ay, float az, float bx, float by, float bz){		return new myVector((ay*bz)-(az*by), (az*bx)-(ax*bz), (ax*by)-(ay*bx));}
	
	
	public static myVector _elemMult(myVector a, myVector b){return new myVector(a.x*b.x, a.y*b.y, a.z*b.z);}
	
	public static float _det3(myVector U, myVector V) {float udv = U._dot(V); return (float)(Math.sqrt(U._dot(U)*V._dot(V) - (udv*udv))); };                                // U|V det product
	public static float _mixProd(myVector U, myVector V, myVector W) {return U._dot(myVector._cross(V,W)); };                                                 // (UxV)*W  mixed product, determinant - measures 6x the volume of the parallelapiped formed by myVectortors
	public float _dot(myVector b){return ((this.x * b.x) + (this.y * b.y) + (this.z * b.z));}																	//dot product
	public static float _dot(myVector a, myVector b){		return a._dot(b);}
	
	public static float _angleBetween(myVector v1, myVector v2) {
		float 	_v1Mag = v1._mag(), 
				_v2Mag = v2._mag(), 
				dotProd = v1._dot(v2),
				cosAngle = dotProd/(_v1Mag * _v2Mag),
				angle = (float)Math.acos(cosAngle);
		return angle;
	}//_angleBetween
	
	/**
	 * returns if this vector is equal to passed vector
	 * @param b vector to check
	 * @return whether they are equal
	 */
	public boolean equals(Object b){
		if (this == b) return true;
		if (!(b instanceof myVector)) return false;
		myVector v = (myVector)b;
		return ((this.x == v.x) && (this.y == v.y) && (this.z == v.z));		
	}				
	
	public String toString(){return super.toString()+ " | Mag:" + this.magn+ " | sqMag:" + this.sqMagn;}
}//myVector

