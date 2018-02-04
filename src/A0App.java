/*
 * You must always put your name and student number at the top of the file!!!
 */



import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * Assignment 0 - getting up to speed with Eclipse and JOGL
 */
public class A0App extends KeyAdapter implements GLEventListener {
    /* static section of the class */
    private static GLU glu = new GLU(); // for gluPerspective

    private static GLUT glut = new GLUT(); // for glutWireCube 

    private static GLCanvas glCanvas; // handle to the GL plot

    /**
     * Creates a Basic GL Window and links it to a GLEventListener
     * 
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame; // handle to the window

        String windowName = "Assignment 0 - YOUR NAME AND STUDENT NUMBER HERE";
        int initialWidth = 600; // Initial Width
        int initialHeight = 400; // Initial Height
        int x = 0; // X Coordinates of the Top Left Corner
        int y = 0; // Y Coordinates of the Top Left Corner

        GLProfile profile = GLProfile.getDefault();
        glCanvas = new GLCanvas(new GLCapabilities(profile));
        glCanvas.setSize(initialWidth, initialHeight);
        A0App scene = new A0App();
        glCanvas.addGLEventListener(scene);
        
        glCanvas.addKeyListener(scene);

        // either need regular refreshes to draw an animated canvas,
        // or we need to request a repaint any time it has changed.
        // (see the keyboard handling callback)
        // FPSAnimator animator = new FPSAnimator(glCanvas, 60);
        
        frame = new JFrame(windowName);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(glCanvas, BorderLayout.CENTER);
        frame.setLocation(x, y);

        try {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });

            frame.setSize(frame.getContentPane().getPreferredSize());
            frame.setVisible(true);

            glCanvas.requestFocus(); // activates the Event Listeners
            glCanvas.display(); // draw the first scene
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Repaints the GL window
     */
    public static void repaint() {
        glCanvas.repaint();
    }

    /* non static section of the class */

    private float viewAngle = 45.0f;

    private float nearPlane = 0.1f;

    private float farPlane = 100.0f;

    private boolean toggle = false;
    
    /** current width of the window */
    private float width;
    float satelliteAngle=20;
    /** current height of the window */
    private float height;

    @Override
    /* draws the scene */
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Draw text
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, width, height, 0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glRasterPos2d(3.0, 15.0);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, "Hello World! toggle = " + toggle);

        gl.glMatrixMode(GL2.GL_PROJECTION); // Select The Projection Matrix
        gl.glLoadIdentity();
        // Calculate The Aspect Ratio Of The Window
        float aspectRatio = (float) width / (float) height;
        glu.gluPerspective(viewAngle, aspectRatio, nearPlane, farPlane);
        gl.glTranslated(0, 0, -5);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glPushMatrix();

        gl.glRotatef(10, 0, 0, 1);

        // Draw something interesting
        glut.glutWireTeapot(1);
        gl.glPopMatrix();

    }

    /** initializes the class for display */
    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
        GL gl = drawable.getGL();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); // Black Background
        gl.glClearDepth(1.0f); // Depth Buffer Setup
        gl.glEnable(GL.GL_DEPTH_TEST); // Enables Depth Testing
        gl.glDepthFunc(GL.GL_LEQUAL); // The Type Of Depth Testing To Do
        this.height = drawable.getHeight();
        this.width = drawable.getHeight();
        FPSAnimator animator = new FPSAnimator(30);
        animator.start();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Add your code here
        if ( e.getKeyCode() == KeyEvent.VK_SPACE ) {
            toggle = !toggle;
            glCanvas.repaint();
        }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    	// anything to do?
    }
    
    /** Update the size of the window */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        this.height = h;
        this.width = w;
        
    }

}
