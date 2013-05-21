package com.sun.javaone.aerith;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jdesktop.swingx.mapviewer.LocalResponseCache;
import com.sun.javaone.aerith.ui.MainFrame;
import com.sun.javaone.aerith.ui.TransitionManager;

public class Main {
    private Main() {
    }
    
    public static void main(String[] args) {
        LocalResponseCache.installResponseCache();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException e) {
                }

                MainFrame frame = TransitionManager.createMainFrame();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
