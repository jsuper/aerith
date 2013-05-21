package com.sun.animation.transitions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import com.sun.animation.effects.ComponentEffect;
import com.sun.animation.effects.CompositeEffect;
import com.sun.animation.effects.EffectsManager;
import com.sun.animation.effects.Move;
import com.sun.animation.effects.Rotate;

/**
 *
 * @author Chet Haase
 */
public class AnimatingGUI extends JComponent implements ActionListener, TransitionTarget {
    
    class GradientPanel extends JComponent {
        public GradientPanel() {
            setOpaque(false);
        }
        
         public void paintComponent(Graphics g) {
        if (gradientImage == null || prevW != getWidth() || prevH != getHeight()) {
            gradientImage = createImage(getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D)gradientImage.getGraphics();
	    Paint bgGradient = new GradientPaint(0, 0, Color.lightGray, 0, 
                    getHeight(), Color.blue);
            g2d.setPaint(bgGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
            prevW = getWidth();
            prevH = getHeight();
        }
        g.drawImage(gradientImage, 0, 0, null);
        }
    }
    JLabel label = new JLabel("Label");
    JButton button1 = new JButton("1");
    JButton button2 = new JButton("2");
    JButton button3 = new JButton("3");
    JButton button4 = new JButton("4");
    JTextField textField = new JTextField("<sample text>");
    JTable table = new JTable(25, 7);
    JScrollPane scroller = new JScrollPane(table);
    //JComponent transitionContainer = new GradientPanel();
    JComponent transitionContainer = new JPanel(null);
    ScreenTransition transitionPanel = new ScreenTransition(transitionContainer,
            this);
    JButton containedButton = new JButton("blah");
    JPanel buttonContainer = new JPanel();
    JPanel buttonPanel = null;
    JComponent thumbnail = new JPanel();
    static final int FRAME_W = 300;
    static final int FRAME_H = 300;
    int currentScreen = 1;
    Image gradientImage;
    int prevW = -1, prevH = -1;
    
    public AnimatingGUI() {
        transitionContainer.setOpaque(false);
	buttonContainer.add(containedButton);
	buttonContainer.setBorder(new BevelBorder(BevelBorder.LOWERED));
	ComponentEffect moveEffect = new Move(null, null);
	ComponentEffect rotateEffect = new Rotate(null, null, 360, 15, 10);
	CompositeEffect compositeEffect = new CompositeEffect(moveEffect);
	compositeEffect.addEffect(rotateEffect);
	EffectsManager.setEffect(label, compositeEffect, 
		EffectsManager.TransitionType.CHANGING);
	setLayout(new BorderLayout());
	setupButtons();
	//((ScreenTransition)transitionPanel).setContentPane(new GradientContainer());
	add(transitionContainer, BorderLayout.CENTER);
	setupFirstScreen();
	button1.addActionListener(this);
	button2.addActionListener(this);
	button3.addActionListener(this);
	button4.addActionListener(this);
    }
    
    public void setupButtons() {
	buttonPanel = new JPanel();
	add(buttonPanel, BorderLayout.NORTH);
	buttonPanel.add(button1);
	buttonPanel.add(button2);
	buttonPanel.add(button3);
	buttonPanel.add(button4);
    }

    public void setupFirstScreen() {
	transitionContainer.add(label);
	label.setBounds(50, 100, 60, 20);
	transitionContainer.add(textField);
	textField.setBounds(label.getX() + label.getWidth() + 10, label.getY() - 5, 100, 30);
	transitionContainer.add(thumbnail);
	thumbnail.setBounds(0, 150, 50, 60);
    }
    
    public void setupSecondScreen() {
	transitionContainer.add(label);
	label.setBounds(10, 50, 60, 20);
	transitionContainer.add(textField);
	textField.setBounds(label.getX() + label.getWidth() + 5, label.getY() - 5, 100, 30);
	transitionContainer.add(scroller);
	int y = label.getY() + label.getHeight() + 50;
	scroller.setBounds(0, y, getWidth(), getHeight() - y);
	transitionContainer.add(thumbnail);
	thumbnail.setBounds(50, 0, 25, 30);
    }
    
    public void setupThirdScreen() {
	transitionContainer.add(buttonContainer);
	buttonContainer.setBounds(100, 100, 100, 100);
	transitionContainer.add(label);
	label.setBounds(70, 30, 60, 20);
	transitionContainer.add(textField);
	textField.setBounds(label.getX() + label.getWidth() + 5, label.getY() - 5, 100, 30);
    }
    
    public void setupFourthScreen() {
	transitionContainer.add(label);
	label.setBounds(10, 50, 60, 20);
	transitionContainer.add(textField);
	textField.setBounds(label.getX() + label.getWidth() + 5, label.getY() - 5, 100, 30);
	transitionContainer.add(scroller);
	int y = label.getY() + label.getHeight() + 50;
	scroller.setBounds(50, y, getWidth() - 100, getHeight() - y - 50);
	transitionContainer.add(thumbnail);
	thumbnail.setBounds(50, 0, 25, 30);
    }
    
    public void resetCurrentScreen() {
	transitionContainer.removeAll();
    }
    
    public void transitionComplete() {
    }
    
    public void setupNextScreen() {
	switch (currentScreen) {
	    case 1:
		setupFirstScreen();
		break;
	    case 2:
		setupSecondScreen();
		break;
	    case 3:
		setupThirdScreen();
		break;
	    case 4:
		setupFourthScreen();
		break;
	    default:
		System.out.println("unknown screen" + currentScreen);
		break;
	}
    }
    
    public void actionPerformed(ActionEvent ae) {
	
	// set up new GUI state
	Object actionSource = (JButton)ae.getSource();
	if (actionSource.equals(button1)) {
	    currentScreen = 1;
	} else 	if (actionSource.equals(button2)) {
	    currentScreen = 2;
	} else 	if (actionSource.equals(button3)) {
	    currentScreen = 3;
	} else 	if (actionSource.equals(button4)) {
	    currentScreen = 4;
	} else {
	    System.out.println("unknown action event: " + ae);
	    return;
	}
	transitionPanel.startTransition(1000);
    }
    
    public void paintComponent(Graphics g) {
        if (gradientImage == null || prevW != getWidth() || prevH != getHeight()) {
            gradientImage = createImage(getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D)gradientImage.getGraphics();
	    Paint bgGradient = new GradientPaint(0, 0, Color.lightGray, 0, 
                    getHeight(), Color.blue);
            g2d.setPaint(bgGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
            prevW = getWidth();
            prevH = getHeight();
        }
        g.drawImage(gradientImage, 0, 0, null);
    }
    
    private static void createAndShowGUI() {
	JFrame f = new JFrame();
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	f.setSize(300, 300);
	AnimatingGUI component = new AnimatingGUI();
	f.add(component);
	f.setVisible(true);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
	Runnable doCreateAndShowGUI = new Runnable() {
	    public void run() {
		createAndShowGUI();
	    }
	};
	SwingUtilities.invokeLater(doCreateAndShowGUI);
    }
    
}
