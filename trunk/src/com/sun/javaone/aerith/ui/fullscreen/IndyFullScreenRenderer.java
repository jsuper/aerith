package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import javazoom.jl.decoder.JavaLayerException;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.Animator.RepeatBehavior;
import org.jdesktop.animation.timing.interpolation.KeyFrames;
import org.jdesktop.animation.timing.interpolation.KeyTimes;
import org.jdesktop.animation.timing.interpolation.KeyValues;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.animation.timing.interpolation.SplineInterpolator;
import org.jdesktop.swingx.JXMapViewer;

import com.aetrion.flickr.photos.Photo;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.ui.PhotoWrapper;
import com.sun.javaone.aerith.ui.music.MusicPlayer;
import com.sun.javaone.aerith.util.FileUtils;

public class IndyFullScreenRenderer implements FullScreenRenderer {
	private final BufferedImage image;

	private boolean isDone = false;

	private float zoomLevel = 1.0f;

	private float opacity = 1.0f;

	private boolean screenWentAway = false;

	private Point2D[] points;

	private IndyPanel indyMapPanel;

	private MPAAPanel mpaaPanel;

	private double[] lengths;

	private MusicPlayer player;

	private Animator mapTimer;

	private Animator photoTimer;

	private Animator introductionTimer;

	private Animator mpaaTimer;

	private Thread loadingThread;

	private List<BufferedImage> photos;

	private boolean introPassed = false;

	private boolean paintMap = false;

	private int photosCount = 0;

	private int photosLoaded = 0;

	private boolean paintMPAA = false;

	private URL baseUrl;

	IndyFullScreenRenderer(BufferedImage image, URL baseUrl) {
		BufferedImage mask = createGradientMask(image.getWidth(), image
				.getHeight());
		this.image = createReflectedPicture(image, mask);
		this.baseUrl = baseUrl;
	}

	public static BufferedImage createReflectedPicture(BufferedImage avatar,
			BufferedImage alphaMask) {
		int avatarWidth = avatar.getWidth();
		int avatarHeight = avatar.getHeight();

		BufferedImage buffer = createReflection(avatar, avatarWidth,
				avatarHeight);

		applyAlphaMask(buffer, alphaMask, avatarHeight);

		return buffer;
	}

	private static void applyAlphaMask(BufferedImage buffer,
			BufferedImage alphaMask, int avatarHeight) {

		Graphics2D g2 = buffer.createGraphics();
		g2.setComposite(AlphaComposite.DstOut);
		g2.drawImage(alphaMask, null, 0, avatarHeight);
		g2.dispose();
	}

	private static BufferedImage createReflection(BufferedImage avatar,
			int avatarWidth, int avatarHeight) {

		BufferedImage buffer = GraphicsUtil.createTranslucentCompatibleImage(
				avatarWidth, avatarHeight * 2);
		Graphics2D g = buffer.createGraphics();

		g.drawImage(avatar, null, null);
		g.translate(0, avatarHeight * 2);

		AffineTransform reflectTransform = AffineTransform.getScaleInstance(
				1.0, -1.0);
		g.drawImage(avatar, reflectTransform, null);

		g.dispose();

		return buffer;
	}

	private static BufferedImage createGradientMask(int avatarWidth,
			int avatarHeight) {
		BufferedImage gradient = GraphicsUtil.createTranslucentCompatibleImage(
				avatarWidth, avatarHeight);
		Graphics2D g = gradient.createGraphics();
		GradientPaint painter = new GradientPaint(0.0f, 0.0f, new Color(1.0f,
				1.0f, 1.0f, 0.55f), 0.0f, avatarHeight * 4.0f / 5.0f,
				new Color(1.0f, 1.0f, 1.0f, 1.0f));
		g.setPaint(painter);
		g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));

		g.dispose();

		return gradient;
	}

	public boolean isDone() {
		return isDone;
	}

	public void render(Graphics g, Rectangle bounds) {
		Graphics2D g2 = (Graphics2D) g;
		setupGraphics(g2);

		if (!screenWentAway) {
			paintBackground(g2, bounds);
			paintSystemScreen(g2, bounds);
		} else {
			if (paintMPAA) {
				paintBackground(g2, bounds);
			}

			// paint black stripes at the top and bottom. The height is based on
			// the
			// aspect ration 16:9
			double newHeight = (9 * bounds.getWidth()) / 16;
			int newY = (int) (bounds.getHeight() - newHeight) / 2;

			// g2.setColor(Color.BLACK);
			// g2.fillRect(0, 0, bounds.width, newY);
			// if (mpaaPanel != null) {
			// float alpha = mapTimer != null && mapTimer.isRunning() ? 1.0f :
			// mpaaPanel.getAlpha();
			// g2.fillRect(0, (int) (bounds.getHeight() - newY * alpha),
			// bounds.width, newY);
			// }
			// g2.fillRect(0, (int) (bounds.getHeight() - newY), bounds.width,
			// newY);

			Shape oldClip = g.getClip();
			Rectangle newBounds = new Rectangle(0, newY, bounds.width,
					bounds.height - (newY * 2));
			g.setClip(newBounds);

			if (paintMap) {
				indyMapPanel.render(g2, newBounds);
			}

			if (paintMPAA) {
				mpaaPanel.render(g2, newBounds);
				Composite oldComposite = g2.getComposite();
				g2.setComposite(AlphaComposite.SrcOver.derive(mpaaPanel
						.getAlpha()));
				RoundRectangle2D casing = new RoundRectangle2D.Double(
						bounds.width / 4.0, bounds.height / 2.0 + 150.0,
						bounds.width / 2.0, 16.0, 16.0, 16.0);
				g2.setColor(Color.WHITE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				Stroke oldStroke = g2.getStroke();
				g2.setStroke(new BasicStroke(2.0f));
				g2.draw(casing);
				g2.setStroke(oldStroke);

				if (photosCount > 0) {
					casing = new RoundRectangle2D.Double(
							bounds.width / 4.0 + 4.0,
							bounds.height / 2.0 + 150.0 + 4.0,
							(bounds.width / 2.0 - 8.0) * photosLoaded
									/ photosCount, 9.0, 9.0, 9.0);
					g2.fill(casing);
				}
				g2.setComposite(oldComposite);
			}

			g.setClip(oldClip);
		}
	}

	public void cancel() {
		if (!introPassed) {
			return;
		}

		if (photoTimer != null) {
			photoTimer.stop();
		}

		if (mapTimer != null) {
			mapTimer.stop();
		}
	}

	private static void setupGraphics(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}

	private void paintSystemScreen(Graphics2D g2, Rectangle bounds) {
		int width = (int) (image.getWidth() * zoomLevel);
		int height = (int) (image.getHeight() * zoomLevel);
		Composite composite = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				opacity));
		g2
				.drawImage(image, (bounds.width - width) / 2,
						(int) (-(height / 7) * (1.0f - zoomLevel)), width,
						height, null);

		g2.setComposite(composite);
	}

	private static void paintBackground(Graphics2D g2, Rectangle bounds) {
		// if (gradientBuffer == null) {
		// gradientBuffer = GraphicsUtil.createCompatibleImage(
		// bounds.width, bounds.height
		// );
		// Graphics2D g2d = gradientBuffer.createGraphics();
		// setupGraphics(g2d);
		// int gradientStart = bounds.height / 2;
		// g2d.setColor(Color.BLACK);
		// g2d.fillRect(bounds.x, bounds.y, bounds.width, gradientStart);
		//
		// g2d.setPaint(new GradientPaint(0.0f, gradientStart, Color.BLACK,
		// 0.0f, bounds.height, new Color(0x4b4b4b),
		// true));
		// g2d.fillRect(bounds.x, bounds.y + gradientStart,
		// bounds.width, bounds.height - gradientStart);
		// g2d.dispose();
		// }
		// g2.drawImage(gradientBuffer, bounds.x, bounds.y, null);
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, bounds.width + 1, bounds.height + 1);
	}

	public void setZoomLevel(float zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public float getZoomLevel() {
		return zoomLevel;
	}

	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}

	public float getOpacity() {
		return opacity;
	}

	public void start() {
		loadingThread = new Thread("Loading Thread") {
			@Override
			public void run() {
				Trip t = null;
				try {
					t = FileUtils.readTrip(new File(new URL(baseUrl
							.toExternalForm()
							+ "saved-trips/").toURI()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				BufferedImage timg = null;
				try {
					timg = ImageIO.read(new File(new URL(baseUrl
							.toExternalForm()
							+ "indy-map.png").toURI()));
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (t == null || timg == null) {
					assert false;
				}

				BufferedImage img = GraphicsUtil.toCompatibleImage(timg);
				timg.flush();
				// noinspection UnusedAssignment
				timg = null;

				boolean useLargePicture = System
						.getProperty("athena.largePictures") != null;
				photosCount = 0;
				for (Trip.Waypoint wp : t.getWaypoints()) {
					photosCount += wp.getPhotoCount();
				}

				photos = new ArrayList<BufferedImage>(photosCount);
				for (Trip.Waypoint wp : t.getWaypoints()) {
					for (PhotoWrapper wrapper : wp.getPhotos()) {
						Photo photo = wrapper.getFlickrPhoto();
						try {
							photos.add(GraphicsUtil
									.loadCompatibleImage(new URL(
											useLargePicture ? photo
													.getLargeUrl() : photo
													.getMediumUrl())));
						} catch (MalformedURLException ex) {
							ex.printStackTrace();
						} catch (IOException ex) {
							ex.printStackTrace();
						} finally {
							photosLoaded++;
						}
					}
				}
				indyMapPanel = new IndyPanel(img, photos
						.toArray(new BufferedImage[0]));
				JXMapViewer map = new JXMapViewer();
				GeneralPath path = TileGrabber.getScaledPath(t.getPath(), map
						.getTileFactory(), 8);
				points = TileGrabber.getPathPoints(path);
				TileGrabber.adjustPathPoints(points, map.getTileFactory());
				lengths = calculatePathLengths(points);
			}
		};
		// loadingThread.setPriority(Thread.MIN_PRIORITY);
		loadingThread.start();

		mpaaPanel = new MPAAPanel();

		try {
			player = MusicPlayer.getMusicPlayer(new File(new URL(baseUrl
					.toExternalForm()
					+ "music/theme.mp3").toURI()).getAbsolutePath());
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (URISyntaxException ex) {
			ex.printStackTrace();
		}
		startIntro();
	}

	private void startIntro() {

		introductionTimer = PropertySetter.createAnimator(2000, this,
				"zoomLevel", 1.0f, 0.25f);
		PropertySetter ps = new PropertySetter(this, "opacity", 1.0f, 0.0f);
		introductionTimer.addTarget(ps);
		introductionTimer.setAcceleration(0.7f);
		introductionTimer.addTarget(new TimingTarget() {

			public void begin() {
				paintMPAA = true;

			}

			public void end() {
				screenWentAway = true;
				startMPAATimer();
			}

			public void repeat() {
				// TODO Auto-generated method stub

			}

			public void timingEvent(float arg0) {
				// TODO Auto-generated method stub

			}
		});
		introductionTimer.start();
	}

	private void startMPAATimer() {
		mpaaTimer = PropertySetter.createAnimator(1000, mpaaPanel, "alpha",
				0.0f, 1.0f);
		mpaaTimer.setAcceleration(.2f);
		mpaaTimer.addTarget(new TimingTarget() {

			public void begin() {
				// TODO Auto-generated method stub

			}

			public void end() {
				new Thread(new Runnable() {
					public void run() {
						try {
							loadingThread.join();
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						new Thread(new Runnable() {
							public void run() {
								try {
									player.play();
								} catch (JavaLayerException e) {
									e.printStackTrace();
								}
							}
						}, "Music Theme").start();
						indyMapPanel.setOffset(points[0]);
						paintMap = true;

						startMPAATimer2();
					}
				}, "Pictures Join Point").start();

			}

			public void repeat() {
			}

			public void timingEvent(float arg0) {
			}
		});
		mpaaTimer.start();
	}

	private void startMPAATimer2() {
		mpaaTimer = PropertySetter.createAnimator(1000, mpaaPanel, "alpha",
				1.0f, 0.0f);
		mpaaTimer.setAcceleration(.2f);
		mpaaTimer.addTarget(new TimingTarget() {

			public void begin() {
			}

			public void end() {
				startMapTimer();
				paintMPAA = false;
			}

			public void repeat() {
			}

			public void timingEvent(float arg0) {
			}
		});
		mpaaTimer.start();
	}

	private void startMapTimer() {
		KeyValues<Point2D> values = KeyValues.create(points);
		mapTimer = PropertySetter.createAnimator(287000, indyMapPanel,
				"offset", new KeyFrames(values, calculateKeyTimes(lengths)));
		mapTimer.addTarget(new TimingTarget() {
			public void begin() {
			}

			public void end() {
				player.stop();
				startOutro();
			}

			public void repeat() {
			}

			public void timingEvent(float arg0) {
			}
		});
		mapTimer.start();

		// fade the image in, fade the image out. So, two cycles per image

		// noinspection unchecked
		KeyValues<Float> alphaValues = KeyValues.create(new Float[] { .5f, .5f,
				0.0f });
		SplineInterpolator splineInterpolator[] = {
				new SplineInterpolator(1, 0, 1, 0),
				new SplineInterpolator(1, 0, 1, 0) };
		KeyFrames keyFrames = new KeyFrames(alphaValues, new KeyTimes(0, 0.7f,
				1), splineInterpolator);
		Animator photoTimer = PropertySetter.createAnimator(287000 / photos
				.size(), indyMapPanel, "currentPhotoAlpha", keyFrames);
		photoTimer.setRepeatCount(photos.size());
		photoTimer.setRepeatBehavior(RepeatBehavior.LOOP);
		photoTimer.setResolution(30);
		photoTimer.addTarget(new TimingTarget() {
			public void begin() {
				introPassed = true;
				// TODO Auto-generated method stub

			}

			public void end() {
				// TODO Auto-generated method stub

			}

			public void repeat() {
				indyMapPanel
						.setCurrentPhoto(indyMapPanel.getCurrentPhoto() + 1);
				// TODO Auto-generated method stub

			}

                        @Override
			public void timingEvent(float arg0) {
				// TODO Auto-generated method stub

			}
		});
		photoTimer.start();
	}

	private void startOutro() {
		KeyValues vals = KeyValues.create(0.25f, 1.0f);
		KeyTimes times = new KeyTimes(0.0f, 1.0f);
		KeyFrames frames = new KeyFrames(vals, times);
		
		Animator timer = PropertySetter.createAnimator(2000, this, "zoomLevel",
				frames);
		timer.setResolution(12);
		timer.setAcceleration(0.7f);

		PropertySetter opacityTarget = new PropertySetter(this, "opacity",
				frames);
		timer.addTarget(opacityTarget);

		timer.addTarget(new TimingTarget() {
			public void begin() {
				screenWentAway = false;
			}

			public void end() {
				new Thread(new Runnable() {
					public void run() {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						screenWentAway = true;
						isDone = true;
					}
				}, "Outro End").start();
			}

			public void repeat() {
				// TODO Auto-generated method stub

			}

			public void timingEvent(float arg0) {
				// TODO Auto-generated method stub

			}
		});
		timer.start();
	}

	private static double[] calculatePathLengths(Point2D[] points) {
		double[] lengths = new double[points.length];
		Point2D prev = null;
		for (int i = 0; i < points.length; i++) {
			Point2D point = points[i];
			if (prev != null) {
				double deltaX = Math.abs(point.getX() - prev.getX());
				double deltaY = Math.abs(point.getY() - prev.getY());
				lengths[i] = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
						+ lengths[i - 1];
			} else {
				lengths[0] = 0;
			}
			prev = point;
		}
		return lengths;
	}

	private static KeyTimes calculateKeyTimes(double[] pathLengths) {
		float[] times = new float[pathLengths.length];
		for (int i = 0; i < pathLengths.length; i++) {
			times[i] = (float) (pathLengths[i] / pathLengths[pathLengths.length - 1]);
		}
		return new KeyTimes(times);
	}

	public void end() {
		if (!player.isComplete()) {
			player.stop();
		}

		if (introductionTimer != null && introductionTimer.isRunning()) {
			introductionTimer.stop();
		}

		if (mpaaTimer != null && mpaaTimer.isRunning()) {
			mpaaTimer.stop();
		}

		if (mapTimer != null && mapTimer.isRunning()) {
			mapTimer.stop();
		}

		if (photoTimer != null && photoTimer.isRunning()) {
			photoTimer.stop();
		}

		image.flush();

		indyMapPanel.end();
	}

	public void init(URL base) {
		this.baseUrl = base;
	}

}
