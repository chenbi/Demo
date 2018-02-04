

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.BooleanParameter;
import mintools.parameters.IntParameter;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.EasyViewer;
import mintools.viewer.Interactor;
import mintools.viewer.SceneGraphNode;

/**
 * COMP557 - Assignment 3 class.
 */
public class A3App implements SceneGraphNode, Interactor {

    /**
     * Runs the assignment 3 application 
     * @param args
     */
    public static void main(String[] args) {
        new A3App();
    }
    
    /**
     * The currently loaded polygon soup
     */
    private PolygonSoup soup;
    
    /**
     * An array of half edge data structures, going from coarse to the
     * finest level of subdivision.
     */
    private HEDS[] heds;
    
    /**
     * The current half edge to display (to help with debugging)
     */
    private HalfEdge currentHE;
    
    /**
     * Which polygon soup we are currently viewing
     */
    private int whichSoup = 5;
    
    /**
     * A list of polygon soup files names.
     * These files should be in the root directory of your project
     */
    private String[] soupFiles = {
            "tetrahedron.obj",
            "cube.obj",            
            "manifoldTriangle.obj",
            "ico-sphere-tris.obj",
            "top.obj",
            "sphere.obj",
            "prism.obj", 
            "drum.obj",
            "cylinder.obj",
            "ball.obj",
            "torus.obj",
            "cow.obj",
            "bunny.obj",
            "triangle.obj", // from here down, these meshes include boundaries
            "quad.obj",
            "ico-hole.obj",
            "openTetrahedron.obj",
            "monkey.obj",     
        };
    
    /**
     * COMP557 - Assignment 3 class.
     */
    public A3App() {    
        loadSoupBuildAndSubdivide( soupFiles[whichSoup], 3 );
        EasyViewer ev = new EasyViewer("Comp 557 Assignment 3 - YOUR NAME HERE", this, new Dimension(800, 800), new Dimension(800, 800) );
        ev.addInteractor(this);
    }
    
    /**
     * Loads the specified polygon soup and subdivides to the specified number of levels.
     */
    private void loadSoupBuildAndSubdivide( String filename, int levels ) {          
        soup = new PolygonSoup( filename );
        heds = new HEDS[levels];
        heds[0] = new HEDS(soup);
        for ( int i = 1; i < levels; i++ ) {
            heds[i] = Loop.subdivide( heds[i-1] );
        }
        currentHE = null;
        if ( heds[0].faces.size() > 0 ) {
            currentHE = heds[0].faces.get(0);
        }
        drawLevel.setValue(0);
        drawLevel.setMaximum(levels-1);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
    
        if ( !drawWireFrame.getValue()) {
            // if drawing with lighting, we'll set the material
            // properties for the font and back surfaces, and set
            // polygons to render filled.
            gl.glEnable(GL2.GL_LIGHTING);
            final float frontColour[] = {.7f,.7f,0,1};
            final float backColour[] = {0,.7f,.7f,1};
            final float[] shinyColour = new float[] {1f, 1f, 1f, 1};            
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glDisable( GL.GL_CULL_FACE );
            gl.glMaterialfv( GL.GL_FRONT,GL2.GL_AMBIENT_AND_DIFFUSE, frontColour, 0 );
            gl.glMaterialfv( GL.GL_BACK,GL2.GL_AMBIENT_AND_DIFFUSE, backColour, 0 );
            gl.glMaterialfv( GL.GL_FRONT_AND_BACK,GL2.GL_SPECULAR, shinyColour, 0 );
            gl.glMateriali( GL.GL_FRONT_AND_BACK,GL2.GL_SHININESS, 128 );
            gl.glLightModelf(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_FILL );            
        } else {
            // if drawing without lighting, we'll set the colour to white
            // and set polygons to render in wire frame
            gl.glDisable( GL2.GL_LIGHTING );
            gl.glColor4f(.7f,.7f,0.0f,1);
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_LINE );
        }
        
        if ( drawCoarse.getValue() ) {
            soup.display( drawable );
        }        
        if ( drawChildVerts.getValue() ) {
            heds[drawLevel.getValue()].drawChildVertices( drawable );
        }
        if ( drawHalfEdge.getValue() && currentHE != null ) {
            currentHE.display( drawable );
        }
        heds[drawLevel.getValue()].display( drawable );        
        
        EasyViewer.beginOverlay(drawable);
        gl.glDisable( GL2.GL_LIGHTING );
        gl.glColor3f(1,1,1);
        EasyViewer.printTextLines(drawable, soupFiles[whichSoup] + "\nlevel " + drawLevel.getValue(), 40,40,20, GLUT.BITMAP_HELVETICA_18 );
        EasyViewer.endOverlay(drawable);
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
		drawable.setGL( new DebugGL2( drawable.getGL().getGL2() ) );
        GL gl = drawable.getGL();        
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_NORMALIZE );
    }

    private BooleanParameter drawHalfEdge = new BooleanParameter( "draw test half edge", true );
    private BooleanParameter drawChildVerts = new BooleanParameter( "draw child vertices", false );
    private BooleanParameter drawWireFrame = new BooleanParameter( "draw wire frame", false );
    // TODO: on the line below, set the drawCoarse default value to false once you've correctly created your half edge data structure
    private BooleanParameter drawCoarse = new BooleanParameter( "draw coarse soup mesh", true );
    private IntParameter drawLevel = new IntParameter("subdivision level to draw", 0, 0, 6 );
    private IntParameter subdivisionLevels = new IntParameter("maximum subdivisions", 3, 3, Integer.MAX_VALUE );
    
    @Override
    public JPanel getControls() {
        VerticalFlowPanel vfp = new VerticalFlowPanel();
        vfp.add( drawWireFrame.getControls() );
        vfp.add( drawCoarse.getControls() );        
        vfp.add( drawChildVerts.getControls() );
        vfp.add( drawHalfEdge.getControls() );
        vfp.add( drawLevel.getSliderControls() );
        vfp.add( subdivisionLevels.getControls() );
        vfp.add( new JLabel("increase maximum subdivision level carefully!" ) );        
        JButton reload = new JButton("Reload and resubdivide");
        vfp.add( reload );
        reload.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
            }
        });
        return vfp.getPanel();
    }

    @Override
    public void attach(Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if ( currentHE.twin != null ) currentHE = currentHE.twin;                    
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if ( currentHE.parent != null ) currentHE = currentHE.parent;
                } else if (e.getKeyCode() == KeyEvent.VK_N) {
                    if ( currentHE.next != null ) currentHE = currentHE.next;
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if ( currentHE.child1 != null ) currentHE = currentHE.child1;                    
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if ( currentHE.child2 != null ) currentHE = currentHE.child2;
                } else if (e.getKeyCode() == KeyEvent.VK_S) {
                    // walks along the mesh two triangles at a time
                    if ( currentHE.next != null ) currentHE = currentHE.next;
                    if ( currentHE.next != null ) currentHE = currentHE.next;
                    if ( currentHE.twin != null ) currentHE = currentHE.twin;
                    if ( currentHE.next != null ) currentHE = currentHE.next;
                    if ( currentHE.twin != null ) currentHE = currentHE.twin;
                } else if ( e.getKeyCode() == KeyEvent.VK_PAGE_UP ) {
                    if ( whichSoup > 0 ) whichSoup--;                    
                    loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
                } else if ( e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ) {
                    if ( whichSoup < soupFiles.length -1 ) whichSoup++;                    
                    loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
                } else if ( e.getKeyCode() == KeyEvent.VK_HOME ) {
                    drawLevel.setValue( drawLevel.getValue() - 1 );
                } else if ( e.getKeyCode() == KeyEvent.VK_END ) {
                    drawLevel.setValue( drawLevel.getValue() + 1 );                    
                }                
            }
        });
    }

}
