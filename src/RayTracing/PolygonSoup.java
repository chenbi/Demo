package RayTracing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Simple implementation of a loader for a polygon soup
 */
public class PolygonSoup {

    /**
     * Simple vertex class
     */
    public class Vertex {
        public Point3d p = new Point3d();
        public Vector3d n = null;
    }
    
    /**
     * List of vertex objects used in the mesh.
     */
    public List<Vertex> vertexList = new ArrayList<Vertex>();
    
    /**
     * List of faces, where each face is a list indices into the vertex list.
     */
    public List<int[]> faceList = new ArrayList<int[]>();
    
    /**
     * Creates a polygon soup by loading an OBJ file
     * @param file
     */
    public PolygonSoup(String file) {
        try {
            FileInputStream fis = new FileInputStream( file );
            InputStreamReader isr = new InputStreamReader( fis );
            BufferedReader reader = new BufferedReader( isr );
            String line;
            while ((line = reader.readLine()) != null) {
                if ( line.startsWith("v ") ) {
                    vertexList.add( parseVertex(line) );
                } else if ( line.startsWith("f ") ) {
                    faceList.add( parseFace(line) );
                } 
            }
            reader.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a vertex definition from a line in an obj file.
     * Assumes that there are three components.
     * @param newline
     * @return a new vertex object
     */
    private Vertex parseVertex(String newline) {        
        // Remove the tag "v "
        newline = newline.substring(2, newline.length());
        StringTokenizer st = new StringTokenizer(newline, " ");
        Vertex v = new Vertex();
        v.p.x = Double.parseDouble(st.nextToken());
        v.p.y = Double.parseDouble(st.nextToken());
        v.p.z = Double.parseDouble(st.nextToken());
        return v;
    }

    /**
     * Gets the list of indices for a face from a string in an obj file.
     * Simply ignores texture and normal information for simplicity
     * @param newline
     * @return list of indices
     */
    private int[] parseFace(String newline) {
        // Remove the tag "f "
        newline = newline.substring(2, newline.length());
        // vertex/texture/normal tuples are separated by a spaces.
        StringTokenizer st = new StringTokenizer(newline, " ");
        int count = st.countTokens();
        int v[] = new int[count];
        for (int i = 0; i < count; i++) {
            // first token is vertex index... we'll ignore the rest 
            StringTokenizer st2 = new StringTokenizer(st.nextToken(),"/");
            v[i] = Integer.parseInt(st2.nextToken()) - 1; // want zero indexed vertices!            
        }
        return v;
    }

    /**
     * Draw the polygon soup
     * @param drawable
     */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // assume triangular faces (we're doing loop after all!
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        Vector3d n = new Vector3d();
        gl.glBegin( GL.GL_TRIANGLES );
        for ( int[] faceVertex : faceList ) {
            Point3d p0 = vertexList.get( faceVertex[0] ).p;
            Point3d p1 = vertexList.get( faceVertex[1] ).p;
            Point3d p2 = vertexList.get( faceVertex[2] ).p;
            v1.sub( p1,p0 );
            v2.sub( p2,p1 );
            n.cross( v1, v2 );
            gl.glNormal3d( n.x, n.y, n.z );
            gl.glVertex3d( p0.x, p0.y, p0.z );
            gl.glVertex3d( p1.x, p1.y, p1.z );
            gl.glVertex3d( p2.x, p2.y, p2.z );
        }
        gl.glEnd();
    }
}
