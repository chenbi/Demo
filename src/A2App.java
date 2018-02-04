

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.FloatBuffer;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Matrix4f;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.swing.ControlFrame;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.EasyViewer;
import mintools.viewer.FancyAxis;
import mintools.viewer.FlatMatrix4f;
import mintools.viewer.TrackBallCamera;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Class for Assignment 2
 * 
 * TODO: See the TODO labels in this class and finish implementing the various objectives
 * 
 * @author Chen Bi
 */
public class A2App implements GLEventListener {

	public static void main( String[] args ) {
		new A2App();
	}

	private FPSAnimator animator;

	private Dimension size = new Dimension(512,512);

	private Dimension controlSize = new Dimension(500, 500);

	public A2App() {
		ControlFrame controlFrame = new ControlFrame("Controls");
		controlFrame.add("Scene", getControls());
		controlFrame.add("Trackball", tbc.getControls() );
		controlFrame.setSelectedTab("Scene");	                                
		controlFrame.setSize(controlSize.width, controlSize.height);
		controlFrame.setLocation(size.width + 20, 0);
		controlFrame.setVisible(true);    

		GLProfile glp = GLProfile.getDefault();        
		GLCapabilities glcap = new GLCapabilities(glp);
		GLCanvas glCanvas = new GLCanvas(glcap);
		glCanvas.setSize( size.width, size.height );
		glCanvas.setIgnoreRepaint( true );
		glCanvas.addGLEventListener( this );
		tbc.attach(glCanvas);	        

		JFrame frame = new JFrame( "Light and Shadow - Chen Bi" );
		frame.getContentPane().setLayout( new BorderLayout() );
		frame.getContentPane().add( glCanvas, BorderLayout.CENTER ); 
		frame.setLocation(0,0);
		frame.setUndecorated( false );
		frame.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				System.exit(0);
			}
		});
		frame.setSize( frame.getContentPane().getPreferredSize() );
		frame.setVisible( true );

		glCanvas.requestFocus();
		animator = new FPSAnimator( glCanvas, 60 );
		animator.start();
	}

	private FancyAxis fa = new FancyAxis();

	private GLU glu = new GLU();

	private ShaderState pflShaderState;

	private ShaderState depthShaderState;

	private int[] depthTexture = new int[1];

	private int[] depthFBO = new int[1];

	private final Dimension depthFBOSize = new Dimension(512,512);
	
	private TrackBallCamera tbc = new TrackBallCamera();

	/**
	 * Creates a GLSL program from the .vp and .fp code provided in the shader directory 
	 * @param drawable
	 * @param name
	 * @return
	 */
	private ShaderState createProgram( GLAutoDrawable drawable, String name ) {
		GL2 gl = drawable.getGL().getGL2();
		ShaderCode vsCode = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader", "shader/bin", name, false );
		ShaderCode fsCode = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader", "shader/bin", name, false );
		ShaderProgram shaderProgram = new ShaderProgram();
		shaderProgram.add( vsCode );
		shaderProgram.add( fsCode );
		if (!shaderProgram.link(gl, System.err)) {
			throw new GLException("Couldn't link program: " + shaderProgram);
		}	
		ShaderState shaderState = new ShaderState();
		shaderState.attachShaderProgram( gl, shaderProgram, false );	
		return shaderState;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		drawable.setGL( new DebugGL2( drawable.getGL().getGL2() ) );
		GL2 gl = drawable.getGL().getGL2();

		// don't need this if we're doing it in the per fragment lighting program
		// but we'll include it for when PFL is not enabled
		gl.glEnable( GL2.GL_NORMALIZE );

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    // Black Background
		gl.glClearDepth(1.0f);                      // Depth Buffer Setup
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable( GL.GL_BLEND );
		gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA );
		gl.glEnable( GL.GL_LINE_SMOOTH );
		gl.glEnable( GL2.GL_POINT_SMOOTH );

		// no extra ambient light by default !
		gl.glLightModelfv( GL2.GL_LIGHT_MODEL_AMBIENT, new float[] {0,0,0,1}, 0);

		// Set some default material parameters
		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, new float[] {1,1,1,1}, 0 );
		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[] {1,1,1,1}, 0 );
		gl.glMaterialf( GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, 50 );

		// CREATE THE FRAGMENT PROGRAM FOR DRAWING DEPTH
		depthShaderState = createProgram( drawable, "depthDraw" );

		// CREATE THE FRAGMENT PROGRAM FOR PER FRAGMENT LIGHTING
		pflShaderState = createProgram( drawable, "perFragmentLighting" );

		// SET UP RENDER TO TEXTURE FOR LIGHT DEPTH OFF-SCREEN RENDERING
		gl.glGenTextures( 1, depthTexture, 0 );
		gl.glBindTexture( GL.GL_TEXTURE_2D, depthTexture[0] );
		// By clamping texture lookups to the border, we can force the use of an arbitrary depth value
		// on the edge and outside of our depth map. {1,1,1,1} is max depth, while {0,0,0,0} is min depth
		// Ultimately, you may alternatively want to deal with clamping issues in a fragment program.
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameterfv(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_BORDER_COLOR, new float[] {1,1,1,1}, 0 );
		// The default filtering parameters not appropriate for depth maps, so we set them here! 
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);  
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		// You can also try GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24 for the internal format.
		// Alternatively GL_DEPTH24_STENCIL8_EXT can be used (GL_EXT_packed_depth_stencil).
		// Here, null means reserve texture memory without initializing the contents.
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_DEPTH_COMPONENT32, depthFBOSize.width, depthFBOSize.height, 0, GL2.GL_DEPTH_COMPONENT, GL.GL_UNSIGNED_INT, null);
		gl.glGenFramebuffers( 1, depthFBO, 0);
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, depthFBO[0] );
		gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D, depthTexture[0], 0);
		gl.glDrawBuffer(GL.GL_NONE);
		gl.glReadBuffer(GL.GL_NONE);
		checkFramebufferStatus( gl );

		// Restore the original screen rendering frame buffer binding
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, 0 );
	}

	private int checkFramebufferStatus(GL gl) {
		String statusString = "";
		int framebufferStatus = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
		switch (framebufferStatus) {
		case GL.GL_FRAMEBUFFER_COMPLETE:
			statusString = "GL_FRAMEBUFFER_COMPLETE"; break;
		case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENTS"; break;
		case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"; break;
		case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS"; break;
		case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_FORMATS"; break;
		case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER"; break;
		case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
			statusString = "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";  break;
		case GL.GL_FRAMEBUFFER_UNSUPPORTED:
			statusString = "GL_FRAMEBUFFER_UNSUPPORTED"; break;
		}
		if ( framebufferStatus != GL.GL_FRAMEBUFFER_COMPLETE ) {
			throw new GLException(statusString);
		}
		return framebufferStatus;
	}

	private DoubleParameter lightPosx = new DoubleParameter( "light pos x", 1, -10, 10 );	
	private DoubleParameter lightPosy = new DoubleParameter( "light pos y", 11, -10, 20 );
	private DoubleParameter lightPosz = new DoubleParameter( "light pos z", 3, -10, 10 );

	public void setupLightsInWorld( GLAutoDrawable drawable ) {
		GL2 gl = drawable.getGL().getGL2();
		float[] position = { lightPosx.getFloatValue(), lightPosy.getFloatValue(), lightPosz.getFloatValue(), 1 };
		float[] colour = { 0.8f, 0.8f, 0.8f, 1 };
		float[] acolour = { 0, 0, 0, 1 };
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_SPECULAR, colour, 0 );
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, colour, 0 );
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, acolour, 0 );
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, position, 0 );
		gl.glEnable( GL2.GL_LIGHT0 );
	}

	/**
	 * The light projection must be provided to the per fragment lighting program 
	 * so that we can look up the depth of the closest surface in the light depth map.  
	 * NOTE: FlatMatrix4f is a convenient wrapper that combines a vecmath Matrix4f, 
	 * as well as methods asArray() and reconstitute(), which are useful for using 
	 * the matrix with OpenGL. 
	 */
	public FlatMatrix4f lightProjectionMatrix = new FlatMatrix4f();        

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		//////////////////////////////////////////////////////////////////        
		// Render to our off-screen depth frame buffer object (render to texture)
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, depthFBO[0] );
		gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
		gl.glViewport( 0, 0, depthFBOSize.width, depthFBOSize.height ); 
		
		// TODO: setup the viewing and projection matrices for drawing the depth in the light view.
		// TODO: Be sure to compute appropriate values for the field of view, aspect, near and far!  	
		
		
		FloatBuffer   mModelViewMatrix = FloatBuffer.allocate(16);		// keep the model view matrix
		FloatBuffer   mProjectionMatrix = FloatBuffer.allocate(16);		// keep the projection matrix
		FloatBuffer   mCombinationMatrix = FloatBuffer.allocate(16);		// keep the combination of the two matrix
		
		
		
		gl.glMatrixMode(GL2.GL_PROJECTION);	
		gl.glPushMatrix();		
		gl.glLoadIdentity();
		glu.gluPerspective(45, 1, 2, 10);
		gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, mProjectionMatrix);
		gl.glPopMatrix();
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		glu.gluLookAt (lightPosx.getFloatValue(), lightPosy.getFloatValue(), lightPosz.getFloatValue(), 0, 0, 0, 0, 1, 0);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mModelViewMatrix);
		gl.glPopMatrix();

		
		gl.glPushMatrix();
		gl.glLoadMatrixf(mProjectionMatrix);
		gl.glMultMatrixf(mModelViewMatrix);
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, mCombinationMatrix);
		gl.glPopMatrix();
		lightProjectionMatrix.getBackingMatrix().set(mCombinationMatrix.array());
		//lightProjectionMatrix.reconstitute();
		lightProjectionMatrix.getBackingMatrix().invert();
		//lightProjectionMatrix.reconstitute();

		drawScene( drawable ); 
		
		
		//////////////////////////////////////////////////////////////////        
		// Render to the screen
		gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, 0 );
		gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
		gl.glViewport( 0, 0, drawable.getWidth(), drawable.getHeight() ); 
		
		tbc.prepareForDisplay(drawable);
		setupLightsInWorld(drawable);

		if ( enablePFL.getValue() ) {
			pflShaderState.useProgram(gl, true);
			int shadowMapTextureUnitID = pflShaderState.getUniformLocation( gl, "shadowMap");
			int lightProjectionID = pflShaderState.getUniformLocation( gl, "lightProjection");	
			// Note that we need to specify the texture unit ID, as opposed to the texture ID.
			// the texture ID is bound to the current texture unit with the bind call above, while
			// other calls would be necessary to use additional texture units.  
			gl.glUniform1i( shadowMapTextureUnitID, 0 );
			gl.glUniformMatrix4fv( lightProjectionID, 1, false, mCombinationMatrix.array(), 0 );
		}
		
		drawScene(drawable);//includes the little box in the center of table
		
		if ( enablePFL.getValue() ) {
			pflShaderState.useProgram(gl, false);
		}

		
		
		if ( drawFrustum.getValue() ) {
			gl.glPushMatrix();		
			// TODO: Set up the appropriate transformations for drawing the light's camera frame		
			
			glu.gluLookAt (lightPosx.getFloatValue(), lightPosy.getFloatValue(), lightPosz.getFloatValue(), 0, 0, 0, 0, 1.0, 0);
			// Here is some code to draw the light's eye frame, with some ambient 			
			// light turned on so we can better see it (the fancy axis is otherwise only
			// lit by the light which is found at its origin.

			gl.glLightModelfv( GL2.GL_LIGHT_MODEL_AMBIENT, new float[] {.5f,.5f,.5f,1}, 0);
			fa.draw(gl); //axis
			gl.glLightModelfv( GL2.GL_LIGHT_MODEL_AMBIENT, new float[] {0,0,0,1}, 0);
			gl.glPopMatrix();
			
			
			// TODO: setup the appropriate matrices to draw the light frustum and the

			gl.glPushMatrix();
			//gl.glLoadIdentity();
		
			glu.gluPerspective(45, 1, 2, 20);
			glu.gluLookAt (lightPosx.getFloatValue(), lightPosy.getFloatValue(), lightPosz.getFloatValue(), 0, 0, 0, 0, 1.0, 0);
			

			tbc.applyInverseViewTransformation(drawable);

			//tbc.applyViewTransformation(drawable);
			//
			//gl.glMultMatrixf(lightProjectionMatrix.asArray(), 0);

			// This code draws a cube for the light view NDC.  To get a frustum you will 
			// need to set up the appropriate inverse projection and viewing transformation first!			
			gl.glDisable( GL2.GL_LIGHTING );
			gl.glColor4f( 1, 1, 1, 0.5f );
			gl.glLineWidth( 3 );       
			EasyViewer.glut.glutWireCube( 2 );
			gl.glEnable( GL2.GL_LIGHTING );

			// draw the depth map on the near plane of the light view frustum
			depthShaderState.useProgram(gl, true);

			gl.glBindTexture( GL.GL_TEXTURE_2D, depthTexture[0] );
			int textureUnitID = depthShaderState.getUniformLocation( gl, "depthTexture" );
			int alphaID = depthShaderState.getUniformLocation( gl, "alpha" );  // unused... can remove
			// Note that we need to specify the texture unit ID, as opposed to the texture ID.
			// the texture ID is bound to the current texture unit with the bind call above, while
			// other calls would be necessary to use additional texture units.  
			gl.glUniform1i( textureUnitID, 0 ); 
			gl.glUniform1f( alphaID,  0.5f ); 		
			gl.glDisable( GL2.GL_LIGHTING );
			gl.glEnable( GL.GL_TEXTURE_2D );
			// Draw a quad with texture coordinates that span the entire texture.  
			// Note that we put the quad on the near plane by providing z = -1
			gl.glBegin( GL2.GL_QUADS );
			gl.glTexCoord2d( 0, 0 ); gl.glVertex3f(-1, -1, -1 );
			gl.glTexCoord2d( 1, 0 ); gl.glVertex3f( 1, -1, -1 );
			gl.glTexCoord2d( 1, 1 ); gl.glVertex3f( 1,  1, -1 );
			gl.glTexCoord2d( 0, 1 ); gl.glVertex3f(-1,  1, -1 );
			gl.glEnd();
			gl.glDisable( GL.GL_TEXTURE_2D );
			gl.glEnable( GL2.GL_LIGHTING );

			depthShaderState.useProgram( gl, false );

			gl.glPopMatrix();
		}

	}

	public void drawScene( GLAutoDrawable drawable ) {
		GL2 gl = drawable.getGL().getGL2();
		final float[] orange = new float[] {1,.5f,0,1};
		final float[] red    = new float[] {1,0,0,1};
		final float[] green  = new float[] {0,1,0,1};
		final float[] blue   = new float[] {0,0,1,1};

		fa.draw(gl);

		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, red, 0 );
		gl.glDisable(GL.GL_CULL_FACE);       
		gl.glPushMatrix();
		gl.glTranslated(-2,0.8,0);
		gl.glRotated(45, 0, 1, 0);
		EasyViewer.glut.glutSolidTeapot(1);
		gl.glPopMatrix();

		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, green, 0 );
		gl.glPushMatrix();
		gl.glTranslated( 2, 1, -1 );
		EasyViewer.glut.glutSolidSphere(1,30,20);
		gl.glPopMatrix();

		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, blue, 0 );
		gl.glPushMatrix();
		gl.glTranslated(0,.5,3);
		EasyViewer.glut.glutSolidCube(1);
		gl.glPopMatrix();

		gl.glMaterialfv( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, orange, 0 );
		gl.glPushMatrix();
		gl.glScaled(15,0.1,15);
		gl.glTranslated(0,-.5,0);
		EasyViewer.glut.glutSolidCube(1);
		gl.glPopMatrix();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// do nothing
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// do nothing
	}

	private BooleanParameter drawFrustum = new BooleanParameter( "draw frustum", false );
	private BooleanParameter enablePFL = new BooleanParameter( "per fragment lighting", false );

	public JPanel getControls() {
		VerticalFlowPanel vfp = new VerticalFlowPanel();
		vfp.add( lightPosx.getSliderControls(false) );
		vfp.add( lightPosy.getSliderControls(false) );
		vfp.add( lightPosz.getSliderControls(false) );
		vfp.add( drawFrustum.getControls() );
		vfp.add( enablePFL.getControls() );		
		return vfp.getPanel();
	}
}
