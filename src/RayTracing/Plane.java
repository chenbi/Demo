package RayTracing;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials. If both are defined, a 1x1 tile checker board pattern should be
 *  generated on the plane using the two materials.
 */
public class Plane extends Intersectable {
    
	/**
	 * The second material. If non-null, it is used to produce a checker board pattern.
	 */
	Material material2;
	
	/**
	 * The plane normal.
	 */
	public static final Vector3d n = new Vector3d(0, 1, 0);
    
    /**
     * Default constructor. Creates a unit sphere centered at (0,0,0)
     */
    public Plane() {
    	super();
    	this.material2 = null;
    }

        
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: finish this class   
    	Vector3d position = new Vector3d(0, 0, 0);
		double dotResult = n.dot(ray.viewDirection);
		if (dotResult != 0) {			// not parallel
			double t = (n.dot(position) - n.dot(new Vector3d(ray.eyePoint))) / dotResult;
			Point3d hitPosition = new Point3d();
			ray.getPoint(t, hitPosition);
				
				if (t>0 &&t < result.t) { 
					result.t = t;
					result.n = new Vector3d(n);
					result.p =  new Point3d(hitPosition);
					
					
					//checkerboard material
					if (hitPosition.z*hitPosition.x>=0){					
						if ((((int) hitPosition.x)%2 )	== (((int) hitPosition.z)%2 ) )
							result.material = this.material;
						else{
							if (material2!=null)
								result.material = this.material2;
							else
								result.material = this.material;	
						}
					}
					else {
					
						if ((Math.abs((int) hitPosition.x)%2) == Math.abs(((int) hitPosition.z)%2)){
							if (material2!=null)
								result.material = this.material2;
							else
								result.material = this.material;
						}
						else
							result.material = this.material;
					}
															
				}			
		}
    }    
}
