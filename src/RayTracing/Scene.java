package RayTracing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


/**
 * Simple scene loader based on XML file format.
 */
public class Scene {
    
    /**
     * Flat array of surfaces
     */
    public List<Intersectable> surfaces;
	
	/**
	 * All scene lights.
	 */
	public Map<String,Light> lights;

    /** 
     * Contains information about how to render the scene.
     */
    public Render render;
    
    /** 
     * The ambient colour.
     */
    public Color3f ambient;

    /** 
     * Default constructor.
     */
    public Scene() {
    	this.surfaces = new ArrayList<Intersectable>();
    	this.render = new Render();
    	this.ambient = new Color3f();
    	this.lights = new HashMap<String,Light>();
    }
    
    /**
     * renders the scene
     */
    public void render(boolean showPanel) {

        Camera cam = render.camera; 
        int w = cam.imageSize.width;
        int h = cam.imageSize.height;
        
        render.init(w, h, showPanel);
        
        /* According to Assignment Policy:
         * "You are free to make any modifications and extensions that you please" - Paul
         * 
         * I combined the ray generating and Lighting methods
         * so that the Anti-aliasing and Super-sampling work more efficiently
         * 
         * Sheldon also allows me to have 3 nested loop 
         */
        
        
        for ( int i = 0; i < h && !render.isDone(); i++ ) {
            for ( int j = 0; j < w && !render.isDone(); j++ ) {

        		Color3f c = new Color3f();
				Color3f sum = new Color3f();
				
				// Super-sampling
   				for (int k = 0; k < render.samples; k++) {
					IntersectResult result = new IntersectResult();
					Ray ray = new Ray();
					
					int sHeight = h * render.samples;
					int sWidth = w * render.samples;
					
					float alpha = (float) (((float) sWidth / sHeight) * Math.tan(Math.PI * cam.fovy / (2 * 180)) / (float) (sWidth));
					float beta = (float) Math.tan(Math.PI * cam.fovy / (2 * 180))/ (float) (sHeight);
					
					Vector3d x = new Vector3d();
					Vector3d y = new Vector3d();
					Vector3d z = new Vector3d();
					
					//camera/ray coordinates
					z.set(cam.from.x - cam.to.x, cam.from.y - cam.to.y, cam.from.z - cam.to.z);
					z.normalize();
					x.cross(new Vector3d(cam.up), z);
					y.cross(z, x);
					x.normalize();
					y.normalize();

					double fragmentx = j * render.samples + k;
					double fragmenty = i * render.samples + k;
					
					if (render.jitter) {  //adding offest
					
						fragmentx  = j + Math.random()-0.5;
						fragmenty = i + Math.random()-0.5;
					}
					
					double alphaX = alpha * (2 * fragmentx - sWidth);
					double betaY = beta * (sHeight - 2 * fragmenty);
					
					Vector3d viewDirection = new Vector3d();
					viewDirection.scale(alphaX, x);
					viewDirection.scaleAdd(betaY, y, viewDirection);
					viewDirection.sub(z);
					viewDirection.normalize();
					
					//generate ray parameters
					ray.viewDirection.set(viewDirection);
					ray.eyePoint.set(cam.from);

					//intersect
					surfaces.get(0).intersect(ray, result);
					
					if (result.t < Double.POSITIVE_INFINITY && result.t > 0) {  //hit true
		        		
						double r = 0, g = 0, b = 0; //default shadow color
		        		Point3d intersectionPoint = new Point3d();
		        		intersectionPoint = result.p;
		        		
		        		//Lighting and Shading
		        		for (Light light : lights.values()) {

		        			Vector3d lightVector = new Vector3d(light.from.x - intersectionPoint.x, light.from.y - intersectionPoint.y,	light.from.z - intersectionPoint.z);
		        			lightVector.normalize();

		        			Point3d shadowOrigin = new Point3d();
		        			shadowOrigin.scaleAdd(0.0001, lightVector, intersectionPoint);
		        		        			
		        			//generate shadow ray
		        			Ray lightRay = new Ray(shadowOrigin, lightVector);
		        			IntersectResult lightResult = new IntersectResult();
		        			surfaces.get(0).intersect(lightRay, lightResult);

		        			// need to check if hit is behind the light
		        			double t = (light.from.x - shadowOrigin.x) / lightVector.x;

		        			if (lightResult.t < 0 || lightResult.t == Double.POSITIVE_INFINITY || lightResult.t > t) {
		        				// no shadow 
		           				Material material = result.material;
		        				
		        				//diffuse
		        				double lambert = Math.max(result.n.dot(lightVector), 0.0); 
		        				r += (double) material.diffuse.get().getRed() / 255.0 * lambert	* light.power * light.color.get().getRed() / 255.0;
		        				g += (double) material.diffuse.get().getGreen() / 255.0 * lambert* light.power * light.color.get().getGreen() / 255.0;
		        				b += (double) material.diffuse.get().getBlue() / 255.0 * lambert* light.power * light.color.get().getBlue() / 255.0;

		        				
		        				//Blinn-Phong specular
		        				Vector3d normal =  result.n;
		        				Vector3d v = new Vector3d();
		        				v.negate(ray.viewDirection);
		        				v.normalize();

		        				Vector3d bisector = new Vector3d();
		        				bisector.add(lightVector, v);
		        				bisector.normalize();

		        				double lightScaler = normal.dot(bisector);
		        				lightScaler = Math.max(0.0, Math.pow(lightScaler, material.hardness));
		        				r =r+ (double)(material.specular.get().getRed()/ 255.0 * light.power * light.color.get().getRed() * lightScaler/ 255.0); if (r>=1) r=0.999;
		        				g =g+ (double)(material.specular.get().getGreen()/ 255.0 * light.power *light.color.get().getGreen() * lightScaler/ 255.0);	if (g>=1) g=0.999;
		        				b =b+ (double)(material.specular.get().getBlue()/ 255.0 * light.power *light.color.get().getBlue() * lightScaler/ 255.0); 	if (b>=1) b=0.999;
		        			}
		        		}
        				
        				//ambient
        				r += (double) ambient.get().getRed()/ 255.0 ;	if (r>=1) r=0.999;
        				g += (double) ambient.get().getGreen()/ 255.0;if (g>=1) g=0.999;
        				b += (double) ambient.get().getBlue()/ 255.0;	if (b>=1) b=0.999;

		        		c.set((float) r, (float) g, (float) b);
		            	
					} else {
						// Ray hit the background
						c.set(render.bgcolor);
					}
					// Add the contribution of this sample
					sum.scaleAdd(1 / (float) render.samples, c, sum);
				}


            	// Here is an example of how to calculate the pixel value.            	
   				int r = (int) (255 * sum.x);
				int g = (int) (255 * sum.y);
				int b = (int) (255 * sum.z);
				int a = 255;
				int argb = a << 24 | r << 16 | g << 8 | b;
                // update the render image
                render.setPixel(j, i, argb);
            }
        }
        
        // save the final render image
        render.save();
        
        // wait for render viewer to close
        render.waitDone();

    }
    


}
