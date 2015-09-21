package org.risney.dyn4j;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.log4j.BasicConfigurator;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;
import org.risney.dyn4j.Picking.GameObject;


/**
 * Class used to show a simple example of using the dyn4j project using Java2D
 * for rendering.
 * <p>
 * This class can be used as a starting point for projects.
 * 
 * @author Marc Risney
 * @version 3.2.0
 * @since 3.0.0
 */
public class BouncingBall2D extends JFrame implements KeyListener{
	
	private GameObject ball = null;
	private AtomicBoolean thrustOn = new AtomicBoolean(false);

	private Logger log = LoggerFactory.getLogger(BouncingBall2D.class);

	/** The serial version id */
	private static final long serialVersionUID = 5663760293144882635L;

	/** The scale 45 pixels per meter */
	public static final double SCALE = 45.0;

	/** The conversion factor from nano to base */
	public static final double NANO_TO_BASE = 1.0e9;

	private static final double GRAVITY = 980; // cm/s^2

	private Point point = null;

	public final class CustomMouseAdapter extends MouseAdapter {
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
	 * 
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
			this.color = new Color((float) Math.random() * 0.5f + 0.5f, (float) Math.random() * 0.5f + 0.5f,
					(float) Math.random() * 0.5f + 0.5f);
		}

		/**
		 * Draws the body.
		 * <p>
		 * Only coded for polygons and circles.
		 * 
		 * @param g
		 *            the graphics object to render to
		 */
		public void render(Graphics2D g) {
			// save the original transform
			AffineTransform ot = g.getTransform();

			// transform the coordinate system from world coordinates to local
			// coordinates
			AffineTransform lt = new AffineTransform();
			lt.translate(this.transform.getTranslationX() * SCALE, this.transform.getTranslationY() * SCALE);
			lt.rotate(this.transform.getRotation());

			// apply the transform
			g.transform(lt);

			// loop over all the body fixtures for this body
			for (BodyFixture fixture : this.fixtures) {
				// get the shape on the fixture
				Convex convex = fixture.getShape();
				Graphics2DRenderer.render(g, convex, SCALE, color);
			}

			// set the original transform
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
	public BouncingBall2D() {
		super("Bouncing Ball 2D Example");
		// setup the JFrame
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// add a window listener
		this.addWindowListener(new WindowAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.
			 * WindowEvent)
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
		this.add(this.canvas);

		// make the JFrame not resizable
		// (this way I dont have to worry about resize events)
		this.setResizable(false);

		MouseAdapter mouseAdapter = new CustomMouseAdapter();
		this.canvas.addMouseListener(mouseAdapter);

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
	 * Basically the same shapes from the Shapes test in the TestBed.
	 */
	protected void initializeWorld() {
		// create the world
		this.world = new World();

		this.world.setGravity(new Vector2(0.0, -3.8));

		// create all your bodies/joints

		// create the floor
		Rectangle floorRect = new Rectangle(16.0, 0.1);
		GameObject floor = new GameObject();
		floor.addFixture(new BodyFixture(floorRect));
		floor.setMass(Mass.Type.INFINITE);
		// move the floor down a bit
		floor.translate(0.0, -6.0);
		
		
		// create the 2 walls
		Rectangle leftWallRect = new Rectangle(0.1, 12);
		GameObject leftWall = new GameObject();
		
		leftWall.addFixture(new BodyFixture(leftWallRect));
		leftWall.setMass(Mass.Type.INFINITE);
		leftWall.translate(-8.0, 0.0);
		
		Rectangle rightWallRect = new Rectangle(0.1, 12);
		GameObject rightWall = new GameObject();
		
		rightWall.addFixture(new BodyFixture(rightWallRect));
		rightWall.setMass(Mass.Type.INFINITE);
		rightWall.translate(8.0, 0.0);
		
		
		Rectangle topRect = new Rectangle(16.0, 0.1);
		GameObject topWall = new GameObject();
		
		topWall.addFixture(new BodyFixture(topRect));
		topWall.setMass(Mass.Type.INFINITE);

		// move the ceiling up
		topWall.translate(0.0, 6.0);
		

		
		
		this.world.addBody(leftWall);
		this.world.addBody(rightWall);
		this.world.addBody(floor);
		this.world.addBody(topWall);
		
		/*
		 * // create a triangle object Triangle triShape = new Triangle( new
		 * Vector2(0.0, 0.5), new Vector2(-0.5, -0.5), new Vector2(0.5, -0.5));
		 * GameObject triangle = new GameObject();
		 * triangle.addFixture(triShape); triangle.setMass(Mass.Type.NORMAL);
		 * triangle.translate(-1.0, 2.0); // test having a velocity
		 * triangle.getLinearVelocity().set(5.0, 0.0);
		 * this.world.addBody(triangle);
		 */
		// create a circle

		ball = new GameObject();
		Circle ballShape = new Circle(0.5);

		BodyFixture fixtureBody = new BodyFixture(ballShape);
		fixtureBody.setDensity(0.01);
		fixtureBody.setFriction(0.0);
		fixtureBody.setRestitution(1.0);

		ball.addFixture(fixtureBody);
		ball.setMass(Mass.Type.NORMAL);
		ball.getLinearVelocity().set(-0.1, 0.0);
		//ball.setAngularVelocity(Math.toRadians(-20.0));
		this.world.addBody(ball);

		// this.world.addBody(circle);
		/*
		 * // try a rectangle Rectangle rectShape = new Rectangle(1.0, 1.0);
		 * GameObject rectangle = new GameObject();
		 * rectangle.addFixture(rectShape); rectangle.setMass(
		 * Mass.Type.NORMAL); rectangle.translate(0.0, 2.0);
		 * rectangle.getLinearVelocity().set(-5.0, 0.0);
		 * this.world.addBody(rectangle);
		 * 
		 * // try a polygon with lots of vertices Polygon polyShape =
		 * Geometry.createUnitCirclePolygon(10, 1.0); GameObject polygon = new
		 * GameObject(); polygon.addFixture(polyShape);
		 * polygon.setMass(Mass.Type.NORMAL); polygon.translate(-2.5, 2.0); //
		 * set the angular velocity
		 * polygon.setAngularVelocity(Math.toRadians(-20.0));
		 * this.world.addBody(polygon);
		 * 
		 * // try a compound object Circle c1 = new Circle(0.5); BodyFixture
		 * c1Fixture = new BodyFixture(c1); c1Fixture.setDensity(0.5); Circle c2
		 * = new Circle(0.5); BodyFixture c2Fixture = new BodyFixture(c2);
		 * c2Fixture.setDensity(0.5); Rectangle rm = new Rectangle(2.0, 1.0); //
		 * translate the circles in local coordinates c1.translate(-1.0, 0.0);
		 * c2.translate(1.0, 0.0); GameObject capsule = new GameObject();
		 * capsule.addFixture(c1Fixture); capsule.addFixture(c2Fixture);
		 * capsule.addFixture(rm); capsule.setMass(Mass.Type.NORMAL);
		 * capsule.translate(0.0, 4.0); this.world.addBody(capsule);
		 * 
		 * GameObject issTri = new GameObject();
		 * issTri.addFixture(Geometry.createIsoscelesTriangle(1.0, 3.0));
		 * issTri.setMass(Mass.Type.NORMAL); issTri.translate(2.0, 3.0);
		 * this.world.addBody(issTri);
		 * 
		 * GameObject equTri = new GameObject();
		 * equTri.addFixture(Geometry.createEquilateralTriangle(2.0));
		 * equTri.setMass(Mass.Type.NORMAL); equTri.translate(3.0, 3.0);
		 * this.world.addBody(equTri);
		 * 
		 * GameObject rightTri = new GameObject();
		 * rightTri.addFixture(Geometry.createRightTriangle(2.0, 1.0));
		 * rightTri.setMass(Mass.Type.NORMAL); rightTri.translate(4.0, 3.0);
		 * this.world.addBody(rightTri);
		 * 
		 * GameObject cap = new GameObject(); cap.addFixture(new Capsule(1.0,
		 * 0.5)); cap.setMass(Mass.Type.NORMAL); cap.translate(-3.0, 3.0);
		 * this.world.addBody(cap);
		 * 
		 * GameObject slice = new GameObject(); slice.addFixture(new Slice(0.5,
		 * Math.toRadians(120))); slice.setMass(Mass.Type.NORMAL);
		 * slice.translate(-3.0, 3.0); this.world.addBody(slice);
		 */
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
			@Override
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
	 * The method calling the necessary methods to update the game, graphics,
	 * and poll for input.
	 */
	protected void gameLoop() {
		// get the graphics object to render to
		Graphics2D g = (Graphics2D) this.canvas.getBufferStrategy().getDrawGraphics();

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
		double elapsedTime = diff / NANO_TO_BASE;
		// update the world with the elapsed time
		this.world.update(elapsedTime);
		
		//if (this.thrustOn.get()) {
        //	ball.applyForce(new Vector2(0, 1));
        //}
		
	}

	/**
	 * Renders the example.
	 * 
	 * @param g
	 * the graphics object to render to
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
						true,			// we don't need collision info 
						results);
				
				// you could also iterate over the bodys and do a point in body test
//				for (int i = 0; i < this.world.getBodyCount(); i++) {
//					Body b = this.world.getBody(i);
//					if (b.contains(new Vector2(x, y))) {
//						// record this body
//					}
//				}
			}
			boolean tap = false;
			double magnitude = 0.0;
			// draw all the objects in the world
			for (int i = 0; i < this.world.getBodyCount(); i++) {
				// get the object
				GameObject go = (GameObject) this.world.getBody(i);
				
				// render that we found any
				boolean changeColor = false;
				for (DetectResult r : results) {
					GameObject gor = (GameObject) r.getBody();
					if (gor == go) {
						//results.getPenetration().getNormal().getMagnitude()
						
						magnitude = r.getPenetration().getNormal().getMagnitude();
						
						changeColor = true;
						tap = true;
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
				if (tap){
					tapBall(x,y,0.1);
				}
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
	 * 
	 * @return boolean true if stopped
	 */
	public synchronized boolean isStopped() {
		return this.stopped;
	}

	public void keyPressed(KeyEvent e) {
		thrustOn.set(true);
	}
	
	
	public void keyReleased(KeyEvent e) {
		thrustOn.set(false);
	}
	
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public void tapBall(double x, double y,double magnitude){
		// random value, may utilize a slider control on JCanvas to fine tune this value
		double force =  magnitude;
		
		Vector2 tap = new Vector2(x,y);
		Vector2 ballCenter = ball.getWorldCenter();
		Vector2 f = ballCenter.to(tap);
		double direction = ballCenter.getDirection();
		double d = f.normalize() * 10;
		f.multiply(force / d);
		ball.applyForce(f);
		log.debug("mouse click {},{}, in direction :{}, with force : {}",x,y,direction,force);

	}

	/**
	 * Entry point for the example application.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		
		BasicConfigurator.configure(); //enough for configuring log4j
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
		BouncingBall2D bouncingBall2D = new BouncingBall2D();

		// show it
		bouncingBall2D.setVisible(true);

		// start it
		bouncingBall2D.start();
	}

	
}