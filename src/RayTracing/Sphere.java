package RayTracing;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple sphere class.
 */
public class Sphere extends Intersectable {
    
	/**
	 * Radius of the sphere.
	 */
	public double radius;
    
	/**
	 * Location of the sphere center.
	 */
	public Point3d center;
    
    /**
     * Default constructor. Creates a unit sphere centered at (0,0,0)
     */
    public Sphere() {
    	super();
    	this.radius = 1.0;
    	this.center = new Point3d(0,0,0);
    }
    
    /**
     * Creates a sphere with the request radius and center. 
     * 
     * @param radius
     * @param center
     * @param material
     */
    public Sphere(double radius, Point3d center, Material material) {
    	this.radius = radius;
    	this.center = center;
    	this.material = material;
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: finish this class
		// at^2 + bt + c
		double a, b, c;
		Vector3d disance = new Vector3d();
		a = ray.viewDirection.dot(ray.viewDirection);

		disance.sub(ray.eyePoint, center);
		b = 2 * (ray.viewDirection.dot(disance));

		c = disance.dot(disance) - Math.pow(radius, 2);

		double discriminant = Math.pow(b, 2) - (4 * a * c);

		if (discriminant > 0) {
			// hit
			double r1 = (-b + Math.sqrt(discriminant)) / (a*2);
			double r2 = (-b - Math.sqrt(discriminant)) / (a*2);

			if (r1 >= 0 || r2 >= 0) {
				if (r1 < 0) {
					// root1  behind the camera
					r1 = Double.POSITIVE_INFINITY;
				}
				if (r2 < 0) {
					// root2  behind the camera
					r2 = Double.POSITIVE_INFINITY;
				}
				
				double t = Math.min(r1, r2);
				
				if (t < result.t && t >0 ) {
					result.material = material;
					result.t = t;
					Point3d p = new Point3d();
					ray.getPoint(result.t, p);
					result.p = p;
					Vector3d normal = new Vector3d();
					normal.sub(result.p, center);
					normal.normalize();
					result.n.set(normal);
				}
			}
		} 
    }
    
}
