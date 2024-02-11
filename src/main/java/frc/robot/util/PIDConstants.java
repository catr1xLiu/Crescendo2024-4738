package frc.robot.util;

import monologue.Logged;
import monologue.Annotations.Log;

/** PID constants used to create PID controllers */
public class PIDConstants implements Logged {

    @Log
    private final double P, I, D, FF, iZone, minOutput, maxOutput;

    public PIDConstants(double P) {
        this(P, 0);
    }

    public PIDConstants(double P, double D) {
        this(P, 0, D, 0, 1.0, -1, 1);
    }

    public PIDConstants(double P, double I, double D) {
        this(P, I, D, 0, 1.0, -1, 1);
    }

    public PIDConstants(double P, double I, double D, double FF) {
        this(P, I, D, FF, 1.0, -1, 1);
    }
    
    public PIDConstants(double P, double I, double D, double minOutput, double maxOutput) {
        this(P, I, D, 0, 1.0, minOutput, maxOutput);
    }

    public PIDConstants(double P, double I, double D, double FF, double minOutput, double maxOutput) {
        this(P, I, D, FF, 1.0, minOutput, maxOutput);
    }

    public PIDConstants(double kP, double kI, double kD, double FF, double iZone, double minOutput, double maxOutput) {
        this.P = kP;
        this.I = kI;
        this.D = kD;
        this.FF = FF;
        this.iZone = iZone;
        this.minOutput = minOutput;
        this.maxOutput = maxOutput;
    }

    public double getP() {
        return P;
    }

    public double getI() {
        return I;
    }

    public double getD() {
        return D;
    }

    public double getFF() {
        return FF;
    }

    public double getIZone() {
        return iZone;
    }

    public double getMinOutput() {
        return minOutput;
    }

    public double getMaxOutput() {
        return maxOutput;
    }

    public String toString() {
        return String.format("P: %.4f | I: %.4f | D: %.4f | FF: %.4f | iZone: %.4f | minOutput: %.4f | maxOutput: %.4f",
                this.P, this.I, this.D, this.FF, this.iZone, this.minOutput, this.maxOutput);
    }
}