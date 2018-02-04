package RayTracing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;



import RayTracing.PolygonSoup.Vertex;



public class Mesh extends Intersectable {
	
	/**
	 * Static map storing all meshes by name.
	 */
	public static Map<String,Mesh> meshMap = new HashMap<String,Mesh>();
	
	/**
	 * Name for this mesh. We can reference this to re-use a polygon soup across meshes.
	 */
	public String name;
	
	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.name = "";
		this.soup = null;
	}			
		
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		// TODO: finish this class
		List<Vertex> vertexList = soup.vertexList;
		List<int[]> faceList = soup.faceList;
		// Loop through all the faces
		for (int[] face : faceList) {
			intersectTriangle(vertexList.get(face[0]), vertexList.get(face[1]),	vertexList.get(face[2]), ray, result);
		}

	}
	
	private void intersectTriangle(Vertex p1, Vertex p2, Vertex p3, Ray ray,IntersectResult result) {

		Vector3d normal = new Vector3d();
		Vector3d side1 = new Vector3d();

		Point3d a = new Point3d(p1.p);
		Point3d b = new Point3d(p2.p);
		Point3d c = new Point3d(p3.p);

		// normal of the triangle
		normal.sub(b, a);
		side1.sub(c, a);
		normal.cross(normal, side1);
		normal.normalize();

		double dotResult = normal.dot(ray.viewDirection);
		
		if (dotResult != 0) {
			// not parallel
			double t = (normal.dot(new Vector3d(a)) - normal.dot(new Vector3d(ray.eyePoint))) / dotResult;
			if (t < result.t && t>0) {

				Point3d p = new Point3d();
				ray.getPoint(t, p);
				Vector3d side2 = new Vector3d();

				side1.sub(b, a);
				side2.sub(p, a);
				side1.cross(side1, side2);
				double lambda1 = side1.dot(normal);
				
				side1.sub(c, b);
				side2.sub(p, b);
				side1.cross(side1, side2);
				double lambda2 = side1.dot(normal);

				side1.sub(a, c);
				side2.sub(p, c);
				side1.cross(side1, side2);
				double lambda3 = side1.dot(normal);

				if (lambda1 >= 0 && lambda2 >= 0 && lambda3 >= 0) {
					result.t = t;
					result.n = normal;
					result.p = p;
					result.material = this.material;
				}
			}
		}
	}


}
