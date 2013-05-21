package org.progx.twinkle.equation;

import org.progx.math.equation.AbstractEquation;

public class AnimationEquation extends AbstractEquation {
    public static final String PROPERTY_PHASE = "phase";
    public static final String PROPERTY_SIGMA = "sigma";
    
    // exposed parameters
    private double sigma;
    private double phase;
    
    // internal
    private double lowerStitch;
    
    public AnimationEquation(double sigma, double phase) {
        this.sigma = sigma;
        this.phase = phase;
        
        lowerStitch = Math.exp(phase * sigma) / 2.0;
    }

    public double compute(double x) {
        double value;
        
        if (x <= 0.5) {
            value = Math.exp((x * 2 + phase) * sigma) / 2.0;
            value -= lowerStitch;
        } else {
            value = 1.0 - Math.exp(((1.0 - x) * 2 + phase) * sigma) / 2.0;
            value += lowerStitch;
        }
        
        if (value > 1.0) {
            value = 1.0;
        } else if (value < 0.0) {
            value = 0.0;
        }
        
        return value;
    }
    
    public double getSigma() {
        return sigma;
    }
    
    public void setSigma(double sigma) {
        double oldValue = this.sigma;
        this.sigma = sigma;
        lowerStitch = Math.exp(phase * sigma) / 2.0;
        firePropertyChange(PROPERTY_SIGMA, oldValue, sigma);
    }
    
    public double getPhase() {
        return phase;
    }
    
    public void setPhase(double phase) {
        double oldValue = this.phase;
        this.phase = phase;
        lowerStitch = Math.exp(phase * sigma) / 2.0;
        firePropertyChange(PROPERTY_PHASE, oldValue, phase);
    }
}
