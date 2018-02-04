package RayTracing;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;



/**
 * A simple box class. A box is defined by it's lower (@see min) and upper (@see max) corner. 
 */
public class Box extends Intersectable {

	public Point3d max;
	public Point3d min;
	
    /**
     * Default constructor. Creates a 2x2x2 box centered at (0,0,0)
     */
    public Box() {
    	super();
    	this.max = new Point3d(1, 1, 1);
    	this.min = new Point3d(-1, -1, -1);
    }	

    
    

    

	@Override
	public void intersect(Ray ray, IntersectResult result)
	{
		// TODO: finish this class
		Vector3d[] planeNormals = { new Vector3d(1, 0, 0),new Vector3d(-1, 0, 0), new Vector3d(0, 1, 0),new Vector3d(0, -1, 0), new Vector3d(0, 0, 1),new Vector3d(0, 0, -1) };

		Point3d position = midpoint(max,min);
		
		double size = 1 / Math.sqrt(3)*min.distance(max);
		
		Vector3d[] planePoints = {
				new Vector3d(size / 2 + position.x, position.y, position.z),
				new Vector3d(-size / 2 + position.x, position.y, position.z),
				new Vector3d(position.x, size / 2 + position.y, position.z),
				new Vector3d(position.x, -size / 2 + position.y, position.z),
				new Vector3d(position.x, position.y, size / 2 + position.z),
				new Vector3d(position.x, position.y, -size / 2 + position.z) 
				};

		for (int i = 0; i < planePoints.length; i++) {
			intersectQuad(ray, planeNormals[i], planePoints[i], result);
		}
		
	}
	
    private Point3d midpoint(Point3d p1, Point3d p2) {
        double x,y,z;
        x = 0.5 * (p1.x + p2.x);
        y = 0.5 * (p1.y + p2.y);
        z = 0.5 * (p1.z + p2.z);
        return( new Point3d(x,y,z) );
    }
    
	private void intersectQuad(Ray ray, Vector3d normal, Vector3d planPoint,	IntersectResult result) {
		// Test if plane and ray are parallel
		double dotResult = normal.dot(ray.viewDirection);
		if (dotResult != 0) {			// not parallel
			double t = (normal.dot(planPoint) - normal.dot(new Vector3d(ray.eyePoint))) / dotResult;
			Point3d hitPosition = new Point3d();
			ray.getPoint(t+0.0001, hitPosition);
			if (hitPosition.x >= min.x 
				&& hitPosition.x <= max.x
				&& hitPosition.y >= min.y
				&& hitPosition.y <= max.y
				&& hitPosition.z >= min.z
				&& hitPosition.z <= max.z) {
				//hit one of the faces of the box
				if (t < result.t && t>0) {
					result.t = t;
					result.n.set(new Vector3d(normal));
					result.material = this.material;
					result.p = new Point3d(hitPosition);
					
				}
			}
		}
	
	}
	
	

}
