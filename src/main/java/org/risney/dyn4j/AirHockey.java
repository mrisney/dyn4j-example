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
import java.awt.event.MouseMotionListener;
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
import org.risney.dyn4j.MouseDrag.GameObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dyn4j.dynamics.joint.MotorJoint;

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
public class AirHockey extends JFrame implements KeyListener {

	private Logger log = LoggerFactory.getLogger(getClass());

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	/** The scale 45 pixels per meter */
	public static final double SCALE = 45.0;

	/** The conversion factor from nano to base */
	public static final double NANO_TO_BASE = 1.0e9;

	private static final double GRAVITY = 980; // cm/s^2

	/** The controller body */
	private GameObject controller;
	
	/** The game body */
	
	private GameObject gameObject;
	


	private Point point = null;

	/**
	 * Converts the screen coordinate to world space.
	 * 
	 * @param x
	 *            screen x
	 * @param y
	 *            screen y
	 * @return {@link Vector2}
	 */
	public Vector2 screenToWorld(Point point) {
		Vector2 v = new Vector2();
		v.x = (point.getX() - WIDTH * 0.5) / SCALE;
		v.y = -((point.getY() - HEIGHT * 0.5) / SCALE);
		return v;
	}

	private final class CustomMouseAdapter extends MouseAdapter {
		// @Override
		// public void mouseMoved(MouseEvent e) {
		// point = new Point(e.getX(), e.getY());
		// super.mouseMoved(e);
		// }
		@Override
		public void mouseDragged(MouseEvent e) {
			point = new Point(e.getX(), e.getY());
			super.mouseDragged(e);
		}
	}

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
	public AirHockey() {
		super("AirHockey");
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
		Dimension size = new Dimension(WIDTH, HEIGHT);

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
		this.canvas.addMouseMotionListener(mouseAdapter);

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

		// player control setup

		this.controller = new GameObject();
		this.controller.color = Color.RED;
		this.controller.addFixture(Geometry.createCircle(0.5));
		this.controller.setMass(Mass.Type.INFINITE);
		this.controller.setAutoSleepingEnabled(false);
		this.world.addBody(this.controller);

		GameObject player = new GameObject();
		player.color = Color.GREEN;
		player.addFixture(Geometry.createCircle(0.5));
		player.setMass(Mass.Type.NORMAL);
		player.setAutoSleepingEnabled(false);
		this.world.addBody(player);

		MotorJoint control = new MotorJoint(player, this.controller);
		control.setCollisionAllowed(false);
		control.setMaximumForce(1000.0);
		control.setMaximumTorque(1000.0);
		this.world.addJoint(control);

		this.gameObject = new GameObject();
		Circle puckShape = new Circle(0.5);

		BodyFixture puckFixtureBody = new BodyFixture(puckShape);
		puckFixtureBody.setDensity(0.01);
		puckFixtureBody.setFriction(0.0);
		puckFixtureBody.setRestitution(1.0);

		this.gameObject.addFixture(puckFixtureBody);
		this.gameObject.setMass(Mass.Type.NORMAL);
		this.gameObject.getLinearVelocity().set(-0.1, 0.0);
		// ball.setAngularVelocity(Math.toRadians(-20.0));
		this.world.addBody(this.gameObject);

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

		if (this.point != null) {
			double x = (this.point.getX() - 400.0) / SCALE;
			double y = -(this.point.getY() - 300.0) / SCALE;
			Transform tx = new Transform();
			tx.translate(x, y);
			this.controller.setTransform(tx);
			this.point = null;
		}

	}

	/**
	 * Renders the example.
	 * 
	 * @param g
	 *            the graphics object to render to
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
			x = (this.point.getX() - 400.0) / SCALE;
			y = -(this.point.getY() - 300.0) / SCALE;
			transform.translate(x, y);

			// detect bodies under the mouse pointer (we'll radially expand it
			// so it works a little better by using a circle)
			this.world.detect(convex, transform, null, // no filter needed
					false, // include sensor fixtures
					false, // include inactive bodies
					true, // we don't need collision info
					results);

			// you could also iterate over the bodys and do a point in body test
			// for (int i = 0; i < this.world.getBodyCount(); i++) {
			// Body b = this.world.getBody(i);
			// if (b.contains(new Vector2(x, y))) {
			// // record this body
			// }
			// }
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
					// results.getPenetration().getNormal().getMagnitude()

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
			if (tap) {

				// mouseJoint.setTarget(new Vector2(x, y));

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

	}

	public void keyReleased(KeyEvent e) {

	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * Entry point for the example application.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {

		BasicConfigurator.configure(); // enough for configuring log4j
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
		AirHockey airHockey = new AirHockey();

		// show it
		airHockey.setVisible(true);

		// start it
		airHockey.start();
	}

}