package cs7492Project4;
import processing.core.PApplet;

	
public class Project4Main {
	public static void main(String[] passedArgs) {
	    String[] appletArgs = new String[] { "cs7492Project4.Project4" };
	    if (passedArgs != null) {
	    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
	    } else {
	    	PApplet.main(appletArgs);
	    }
	}
}


