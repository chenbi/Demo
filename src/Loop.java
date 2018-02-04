

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;





/**Chen Bi 260319962
 * Class implementing the Loop subdivision scheme
 */
public class Loop {

    /**
     * Subdivides the provided half edge data structure
     * @param heds the mesh to subdivide
     * @return the subdivided mesh
     */
    public static HEDS subdivide( HEDS heds ) {
    	   	
        HEDS heds2 = new HEDS();
       
        for (HalfEdge he:heds.faces){
        	
        	//below calculate even
        	if (he.head.child == null){
        	
	        	int valence = 1;
	        	HalfEdge next;
	        	for (next = he.next; !he.twin.equals(next);valence++)
	        		next = next.twin.next;

	
	            double k =  (1.0 / valence) * (5.0 / 8.0 - Math.pow((3.0 / 8.0 + .25 * Math.cos(2.0 * Math.PI / valence)), 2.0));
	            
	            Point3d even = new Point3d(he.head.p);
	            even.scale(1.0 - valence*k);  
	            
	            for (int i=0; i<valence;i++){    
	            	Point3d neighbor = next.head.p;
	            	even.scaleAdd(k, neighbor, even);
	            	next = next.twin.next;
	            }
	            
	            Vertex child = new Vertex();
	            child.p = even;
	            he.head.child = child;      	
	
	        	for (next = he.next; !he.twin.equals(next);valence++){
	        		next.twin.head.child = child;
	        		next = next.twin.next;
	        	}
            
        	}
            
        	//below calculate odd
            if (he.child1==null || he.child2==null){
        	
	            Point3d mid = new Point3d();
	            Point3d a = he.head.p;
	            Point3d b = he.next.next.head.p; 
		        Point3d c = he.next.head.p;
		        Point3d d = he.twin.next.head.p;
		          
		        mid.scaleAdd(.375d, a, mid);
		        mid.scaleAdd(.375d, b, mid);
		        mid.scaleAdd(.125d, c, mid);
		        mid.scaleAdd(.125d, d, mid);
		          
		        Vertex odd = new Vertex();
		        odd.p =  mid;
				
				HalfEdge child1 = new HalfEdge();
				HalfEdge child2 = new HalfEdge();
				child1.head = odd;
				child2.head = he.head.child ;
				child1.parent = he;
				child2.parent = he;
				he.child1 = child1;
				he.child2 = child2;

				heds2.faces.add(child1);
				heds2.faces.add(child2);
            }
        }
        
        
        
        //connect
        
        int size = heds2.faces.size();
        for (int i = 0; i< size ;i++){
        	        	
        	HalfEdge he = heds2.faces.get(i);
        	
        	if (he.equals(he.parent.child1)){
		
        		HalfEdge next = new HalfEdge();
        		next.head = he.parent.next.next.child1.head;
        		next.next = he.parent.next.next.child2;
        		he.next = next; 
        		        		
        		HalfEdge twin = new HalfEdge();
        		twin.head = he.head;
        		next.twin = twin;
        		twin.twin = next;        		
       		
        		he.twin = he.parent.twin.child2;
        		heds2.faces.add(next);
        		heds2.faces.add(twin);
        	}
        	
        	
        	
        	if (he.equals(he.parent.child2)){      		
        		he.next = he.parent.next.child1;
        		he.twin = he.parent.twin.child1;
        	}
	
        }
        
        
        for (HalfEdge he:heds2.faces){
        	if (he.next == null){       	
        		he.next = he.twin.next.parent.next.next.child1.next.twin;     	
        	}
        }

        
        //normal
        
        for (HalfEdge he:heds2.faces){       	
			Vertex A = he.head;
			Vertex B = he.next.head;
			Vertex C = he.next.next.head;
			Vector3d BA = new Vector3d(B.p.x - A.p.x, B.p.y - A.p.y, B.p.z - A.p.z);
			Vector3d CA = new Vector3d(C.p.x - A.p.x, C.p.y - A.p.y, C.p.z - A.p.z);
			Vector3d normal = new Vector3d();
			normal.cross(BA, CA);
			normal.normalize();
			he.head.n = normal;
        }


        
        return heds2;        
    }
    
}
