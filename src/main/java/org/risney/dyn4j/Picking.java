package org.risney.dyn4j;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.dynamics.World;

import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

/**
 * A simple scene showing how to determine if the mouse touched
 * a body.
 * @author William Bittle
 * @version 3.2.0
 * @since 3.2.0
 */
public class Picking extends JFrame {
	/** The serial version id */
	private static final long serialVersionUID = 5663760293144882635L;
	
	/** The scale 45 pixels per meter */
	public static final double SCALE = 32.0;
	
	/** The conversion factor from nano to base */
	public static final double NANO_TO_BASE = 1.0e9;

	private Point point = null;
	
	private final class CustomMouseAdapter extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			// get the panel-space point
			point = new Point(e.getX(), e.getY());
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			point = null;
		}
	}

	/**
	 * Custom Body class to add drawing functionality.
	 * @author William Bittle
	 * @version 3.0.2
	 * @since 3.0.0
	 */
	public static class GameObject extends Body {
		/** The color of the object */
		protected Color color;
		
		/**
		 * Default constructor.
		 */
		public GameObject() {
			// randomly generate the color
			this.color = new Color(
					(float)Math.random() * 0.5f + 0.5f,
					(float)Math.random() * 0.5f + 0.5f,
					(float)Math.random() * 0.5f + 0.5f);
		}
		
		/**
		 * Draws the body.
		 * <p>
		 * Only coded for polygons and circles.
		 * @param g the graphics object to render to
		 */
		public void render(Graphics2D g) {
			// save the original transform
			AffineTransform ot = g.getTransform();
			
			// transform the coordinate system from world coordinates to local coordinates
			AffineTransform lt = new AffineTransform();
			lt.translate(this.transform.getTranslationX() * SCALE, this.transform.getTranslationY() * SCALE);
			lt.rotate(this.transform.getRotation());
			
			// apply the transform
			g.transform(lt);
			
			// loop over all the body fixtures for this body
			for (BodyFixture fixture : this.fixtures) {
				// get the shape on the fixture
				Convex convex = fixture.getShape();
				Graphics2DRenderer.render(g, convex, SCALE, this.color);
			}
			
			g.setTransform(ot);
		}
	}
	
	/** The canvas to draw to */
	protected Canvas canvas;
	
	/** The dynamics engine */
	protected World world;
	
	/** Wether the example is stopped or not */
	protected boolean stopped;
	
	/** The time stamp for the last iteration */
	protected long last;
	
	/**
	 * Default constructor for the window
	 */
	public Picking() {
		super("Graphics2D Example");
		// setup the JFrame
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// add a window listener
		this.addWindowListener(new WindowAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
			 */
			@Override
			public void windowClosing(WindowEvent e) {
				// before we stop the JVM stop the example
				stop();
				super.windowClosing(e);
			}
		});
		
		// create the size of the window
		Dimension size = new Dimension(800, 600);
		
		// create a canvas to paint to 
		this.canvas = new Canvas();
		this.canvas.setPreferredSize(size);
		this.canvas.setMinimumSize(size);
		this.canvas.setMaximumSize(size);
		
		// add the canvas to the JFrame
		Container container = this.getContentPane();
		container.setLayout(new BorderLayout());
		this.add(this.canvas, BorderLayout.CENTER);
		
		// make the JFrame not resizable
		// (this way I dont have to worry about resize events)
		this.setResizable(false);
		
		MouseAdapter ml = new CustomMouseAdapter();
		this.canvas.addMouseListener(ml);
		
		// size everything
		this.pack();
		
		// make sure we are not stopped
		this.stopped = false;
		
		// setup the world
		this.initializeWorld();
	}
	
	/**
	 * Creates game objects and adds them to the world.
	 * <p>
	 * Basically the same shapes from the Shapes test in
	 * the TestBed.
	 */
	protected void initializeWorld() {
		// create the world
		this.world = new World();
		
	    GameObject floor = new GameObject();
	    floor.addFixture(Geometry.createRectangle(20, 1));
	    floor.setMass(Mass.Type.INFINITE);
	    this.world.addBody(floor);
	    
	 // Triangle
	    GameObject body2 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createTriangle(new Vector2(0.0, 0.5), new Vector2(-0.5, -0.5), new Vector2(0.5, -0.5));
	      BodyFixture bf = new BodyFixture(c);
	      body2.addFixture(bf);
	    }
	    body2.translate(new Vector2(-1.0, 2.0));
	    body2.setLinearVelocity(new Vector2(5.0, 0.0));
	    body2.setMass(Mass.Type.NORMAL);
	    world.addBody(body2);

	    // Circle
	    GameObject body3 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createCircle(0.5);
	      BodyFixture bf = new BodyFixture(c);
	      body3.addFixture(bf);
	    }
	    body3.translate(new Vector2(2.0, 2.0));
	    body3.applyForce(new Vector2(-100.0, 0.0));
	    body3.setLinearDamping(0.05);
	    body3.setMass(Mass.Type.NORMAL);
	    world.addBody(body3);

	    // Segment
	    GameObject body4 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createSegment(new Vector2(0.5, 0.5), new Vector2(-0.5, -0.5));
	      BodyFixture bf = new BodyFixture(c);
	      body4.addFixture(bf);
	    }
	    body4.translate(new Vector2(1.0, 6.0));
	    body4.setMass(Mass.Type.NORMAL);
	    world.addBody(body4);

	    // Horizontal-Segment
	    GameObject body5 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createSegment(new Vector2(0.5, 0.5), new Vector2(-0.5, -0.5));
	      BodyFixture bf = new BodyFixture(c);
	      body5.addFixture(bf);
	    }
	    body5.rotate(Math.toRadians(-45.0));
	    body5.translate(new Vector2(-4.5, 1.0));
	    body5.setMass(Mass.Type.NORMAL);
	    world.addBody(body5);

	    // Square
	    GameObject body6 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createRectangle(1.0, 1.0);
	      BodyFixture bf = new BodyFixture(c);
	      body6.addFixture(bf);
	    }
	    body6.translate(new Vector2(0.0, 2.0));
	    body6.setLinearVelocity(new Vector2(-5.0, 0.0));
	    body6.setMass(Mass.Type.NORMAL);
	    world.addBody(body6);

	    // Decagon
	    GameObject body7 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createPolygon(new Vector2(1.0, 0.0), new Vector2(0.8090169943749475, 0.5877852522924731), new Vector2(0.30901699437494745, 0.9510565162951535), new Vector2(-0.30901699437494734, 0.9510565162951536), new Vector2(-0.8090169943749473, 0.5877852522924732), new Vector2(-1.0, 1.2246467991473532E-16), new Vector2(-0.8090169943749475, -0.587785252292473), new Vector2(-0.30901699437494756, -0.9510565162951535), new Vector2(0.30901699437494723, -0.9510565162951536), new Vector2(0.8090169943749473, -0.5877852522924734));
	      BodyFixture bf = new BodyFixture(c);
	      body7.addFixture(bf);
	    }
	    body7.translate(new Vector2(-2.5, 2.0));
	    body7.setAngularVelocity(Math.toRadians(-20.0));
	    body7.setMass(Mass.Type.NORMAL);
	    world.addBody(body7);

	    // Capsule
	    GameObject body8 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createCircle(0.5);
	      c.translate(new Vector2(-1.0, 0.0));
	      BodyFixture bf = new BodyFixture(c);
	      bf.setDensity(0.5);
	      body8.addFixture(bf);
	    }
	    {// Fixture2
	      Convex c = Geometry.createCircle(0.5);
	      c.translate(new Vector2(1.0, 0.0));
	      BodyFixture bf = new BodyFixture(c);
	      bf.setDensity(0.5);
	      body8.addFixture(bf);
	    }
	    {// Fixture3
	      Convex c = Geometry.createRectangle(2.0, 1.0);
	      BodyFixture bf = new BodyFixture(c);
	      body8.addFixture(bf);
	    }
	    body8.translate(new Vector2(0.0, 4.0));
	    body8.setMass(Mass.Type.NORMAL);
	    world.addBody(body8);

	    // IsoscelesTriangle
	    GameObject body9 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createTriangle(new Vector2(0.0, 2.0), new Vector2(-0.5, -1.0), new Vector2(0.5, -1.0));
	      BodyFixture bf = new BodyFixture(c);
	      body9.addFixture(bf);
	    }
	    body9.translate(new Vector2(1.96875, 3.78125));
	    body9.setMass(Mass.Type.NORMAL);
	    world.addBody(body9);

	    // EquilateralTriangle
	    GameObject body10 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createTriangle(new Vector2(0.0, 1.3333333333333335), new Vector2(-1.1547005383792517, -0.6666666666666665), new Vector2(1.1547005383792517, -0.6666666666666665));
	      BodyFixture bf = new BodyFixture(c);
	      body10.addFixture(bf);
	    }
	    body10.translate(new Vector2(3.71875, 4.59375));
	    body10.setMass(Mass.Type.NORMAL);
	    world.addBody(body10);

	    // RightTriangle
	    GameObject body11 = new GameObject();
	    {// Fixture1
	      Convex c = Geometry.createTriangle(new Vector2(-0.6666666666666666, 0.6666666666666667), new Vector2(-0.6666666666666666, -0.3333333333333333), new Vector2(1.3333333333333335, -0.3333333333333333));
	      BodyFixture bf = new BodyFixture(c);
	      body11.addFixture(bf);
	    }
	    body11.translate(new Vector2(4.0, 3.0));
	    body11.setMass(Mass.Type.NORMAL);
	    world.addBody(body11);
	}
	
	/**
	 * Start active rendering the example.
	 * <p>
	 * This should be called after the JFrame has been shown.
	 */
	public void start() {
		// initialize the last update time
		this.last = System.nanoTime();
		// don't allow AWT to paint the canvas since we are
		this.canvas.setIgnoreRepaint(true);
		// enable double buffering (the JFrame has to be
		// visible before this can be done)
		this.canvas.createBufferStrategy(2);
		// run a separate thread to do active rendering
		// because we don't want to do it on the EDT
		Thread thread = new Thread() {
			public void run() {
				// perform an infinite loop stopped
				// render as fast as possible
				while (!isStopped()) {
					gameLoop();
					// you could add a Thread.yield(); or
					// Thread.sleep(long) here to give the
					// CPU some breathing room
				}
			}
		};
		// set the game loop thread to a daemon thread so that
		// it cannot stop the JVM from exiting
		thread.setDaemon(true);
		// start the game loop
		thread.start();
	}
	
	/**
	 * The method calling the necessary methods to update
	 * the game, graphics, and poll for input.
	 */
	protected void gameLoop() {
		// get the graphics object to render to
		Graphics2D g = (Graphics2D)this.canvas.getBufferStrategy().getDrawGraphics();
		
		// before we render everything im going to flip the y axis and move the
		// origin to the center (instead of it being in the top left corner)
		AffineTransform yFlip = AffineTransform.getScaleInstance(1, -1);
		AffineTransform move = AffineTransform.getTranslateInstance(400, -300);
		g.transform(yFlip);
		g.transform(move);
		
		// now (0, 0) is in the center of the screen with the positive x axis
		// pointing right and the positive y axis pointing up
		
		// render anything about the Example (will render the World objects)
		this.render(g);
		
		// dispose of the graphics object
		g.dispose();
		
		// blit/flip the buffer
		BufferStrategy strategy = this.canvas.getBufferStrategy();
		if (!strategy.contentsLost()) {
			strategy.show();
		}
		
		// Sync the display on some systems.
        // (on Linux, this fixes event queue problems)
        Toolkit.getDefaultToolkit().sync();
        
        // update the World
        
        // get the current time
        long time = System.nanoTime();
        // get the elapsed time from the last iteration
        long diff = time - this.last;
        // set the last time
        this.last = time;
    	// convert from nanoseconds to seconds
    	double elapsedTime = (double)diff / NANO_TO_BASE;
        // update the world with the elapsed time
        this.world.update(elapsedTime);
	}
	
	/**
	 * Renders the example.
	 * @param g the graphics object to render to
	 */
	protected void render(Graphics2D g) {
		// lets draw over everything with a white background
		g.setColor(Color.WHITE);
		g.fillRect(-400, -300, 800, 600);
		
		Convex convex = Geometry.createCircle(0.1);
		Transform transform = new Transform();
		List<DetectResult> results = new ArrayList<DetectResult>();
		double x = 0;
		double y = 0;
		
		// convert the point from panel space to world space
		if (this.point != null) {
			x =  (this.point.getX() - 400.0) / SCALE;
			y = -(this.point.getY() - 300.0) / SCALE;
			transform.translate(x, y);
			// detect bodies under the mouse pointer (we'll radially expand it 
			// so it works a little better by using a circle)
			this.world.detect(
					convex, 
					transform,
					null,			// no filter needed 
					false,			// include sensor fixtures 
					false,			// include inactive bodies
					false,			// we don't need collision info 
					results);
			
			// you could also iterate over the bodys and do a point in body test
//			for (int i = 0; i < this.world.getBodyCount(); i++) {
//				Body b = this.world.getBody(i);
//				if (b.contains(new Vector2(x, y))) {
//					// record this body
//				}
//			}
		}

		// draw all the objects in the world
		for (int i = 0; i < this.world.getBodyCount(); i++) {
			// get the object
			GameObject go = (GameObject) this.world.getBody(i);
			
			// render that we found any
			boolean changeColor = false;
			for (DetectResult r : results) {
				GameObject gor = (GameObject) r.getBody();
				if (gor == go) {
					changeColor = true;
					break;
				}
			}
			
			Color c = go.color;
			if (changeColor) {
				go.color = Color.RED;
			}
			// draw the object
			go.render(g);
			go.color = c;
		}
		
		if (this.point != null) {
			AffineTransform tx = g.getTransform();
			g.translate(x * SCALE, y * SCALE);
			Graphics2DRenderer.render(g, convex, SCALE, Color.GREEN);
			g.setTransform(tx);
		}
	}
	
	/**
	 * Stops the example.
	 */
	public synchronized void stop() {
		this.stopped = true;
	}
	
	/**
	 * Returns true if the example is stopped.
	 * @return boolean true if stopped
	 */
	public synchronized boolean isStopped() {
		return this.stopped;
	}
	
	/**
	 * Entry point for the example application.
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		// set the look and feel to the system look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		// create the example JFrame
		Picking window = new Picking();
		
		// show it
		window.setVisible(true);
		
		// start it
		window.start();
	}
}
