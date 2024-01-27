// Developed by Reza from Team Spyder 1622

package frc.robot.util;

import com.revrobotics.*;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.RobotController;
import frc.robot.util.Constants.FieldConstants;
import frc.robot.util.Constants.NeoMotorConstants;

import java.util.ArrayList;
import java.util.List;

/*
 * Some of this is adapted from 3005's 2022 Code
 * Original source published at https://github.com/FRC3005/Rapid-React-2022-Public/tree/d499655448ed592c85f9cfbbd78336d8841f46e2
 */

public class Neo extends CANSparkMax {
    public final RelativeEncoder encoder;
    public final SparkPIDController pidController;

    private int reversedMultiplier = 1;

    private List<Neo> followers = new ArrayList<>();

    private ControlLoopType controlType = ControlLoopType.PERCENT;
    private double targetPosition = 0;
    private double targetVelocity = 0;
    private double targetPercent = 0;
    
    /**
     * Creates a new Neo motor
     * @param id CANID of the SparkMax the Neo is connected to.
     */
    public Neo(int id) {
        this(id, false);
    }

    /**
     * Creates a new Neo motor
     * @param id CANID of the SparkMax the Neo is connected to.
     * @param mode The idle mode of the motor. If true, the motor will brake when not powered. If false, the motor will coast when not powered.
     */
    public Neo(int id, CANSparkBase.IdleMode mode) {
        this(id, false, mode);
    }

    /**
     * Creates a new Neo motor
     * @param id CANID of the SparkMax the Neo is connected to.
     * @param reversed Whether the motor is reversed or not.
     * @param mode The idle mode of the motor. If true, the motor will brake when not powered. If false, the motor will coast when not powered.
     */
    public Neo(int id, boolean reversed, CANSparkBase.IdleMode mode) {
        super(id, CANSparkLowLevel.MotorType.kBrushless);
        this.reversedMultiplier = reversed ? -1 : 1;

        // restoreFactoryDefaults();
        // Timer.delay(0.050);

        // If a parameter set fails, this will add more time to alleviate any bus traffic
        // default is 20ms
        setCANTimeout(50);

        register();
        encoder = getEncoder();
        pidController = getPIDController();
    }

    /**
     * Creates a new Neo motor
     * @param id CANID of the SparkMax the Neo is connected to.
     * @param reversed Whether the motor is reversed or not.
     */
    public Neo(int id, boolean reversed) {
        this(id, reversed, CANSparkBase.IdleMode.kBrake);
    }

    /**
     * Sets the target position for the NEO.
     * @param position Position to set the Neo to in rotations.
     * @param arbitraryFeedForward Arbitrary feed forward to add to the motor output.
     */
    public void setTargetPosition(double position, double arbitraryFeedForward, int slot) {
        position *= reversedMultiplier;
        if (!FieldConstants.IS_SIMULATION) {
            pidController.setReference(position, ControlType.kPosition, slot, arbitraryFeedForward, SparkPIDController.ArbFFUnits.kVoltage);
        }
        targetPosition = position;
        controlType = ControlLoopType.POSITION;
    }

    public void setTargetPosition(double position, double arbitraryFeedForward) {
        setTargetPosition(position, arbitraryFeedForward, 0);
    }

    public void setTargetPosition(double position) {
        setTargetPosition(position, 0, 0);
    }

    // Creates method to set the target percent of the speed output for the NEO.
    public void setTargetPercent(double percent) {
        setTargetPercent(percent, 0);
    }

    /**
     * Sets the target percent of speed output for the NEO.
     * @param percent Percent of NEO output speed.
     * @param slot The PID slot.
     */
    public void setTargetPercent(double percent, int slot) {
        percent *= reversedMultiplier;
        if (percent == 0) {
            setVoltage(0);
        } else {
            pidController.setReference(percent, ControlType.kDutyCycle);
        }
        targetPercent = percent;
        controlType = ControlLoopType.PERCENT;
    }

    /**
     * Sets the target velocity for the NEO.
     * @param velocity Velocity to set the Neo to in rotations per minute.
     */
    public void setTargetVelocity(double velocity) {
        setTargetVelocity(velocity, 0, 0);
    }

    /**
     * Sets the target velocity for the NEO.
     * @param velocity Velocity to set the Neo to in rotations per minute.
     * @param arbitraryFeedForward Arbitrary feed forward to add to the motor output.
     */
    public void setTargetVelocity(double velocity, double arbitraryFeedForward, int slot) {
        velocity *= reversedMultiplier;
        if (velocity == 0) {
            setVoltage(0);
        } else {
            pidController.setReference(velocity, ControlType.kVelocity);
        }
        targetVelocity = velocity;
        controlType = ControlLoopType.VELOCITY;
    }

    public void set(double percent) {
        percent *= reversedMultiplier;
        setVoltage(percent * RobotController.getBatteryVoltage());
        controlType = ControlLoopType.PERCENT;
    }

    private boolean shouldCache = false;
    private double position = 0;
    private double velo = 0;

    public void tick() {
        if (shouldCache) {
            position = encoder.getPosition();
            velo = encoder.getVelocity();
        }

        if ((FieldConstants.IS_SIMULATION) && controlType == ControlLoopType.POSITION) {

            setVoltage(pidController.getP() * (targetPosition - getPosition()));
        }
    }

    public void register() {
        NeoMotorConstants.motors.add(this);
        if (FieldConstants.IS_SIMULATION)
            REVPhysicsSim.getInstance().addSparkMax(this, DCMotor.getNEO(1));
            
    }

    /**
     * Gets the position of the Neo in rotations.
     * @return The position of the Neo in rotations relative to the last 0 position.
     */
    public double getPosition() {
        double pos;
        if (shouldCache) {
            pos = position;
        } else {
            pos = encoder.getPosition();
        }

        if ((FieldConstants.IS_SIMULATION) && controlType == ControlLoopType.VELOCITY) {
            pos /= encoder.getVelocityConversionFactor();
        }

        return pos;
    }

    /**
     * Gets the velocity of the Neo in rotations per minute.
     * @return The instantaneous velocity of the Neo in rotations per minute.
     */
    public double getVelocity() {
        if (shouldCache) {
            return velo;
        } else {
            return encoder.getVelocity();
        }
    }

    public void setPosition(double position) {
        position *=  reversedMultiplier;
        encoder.setPosition(position);
    }

    /**
     * Gets the target position of the Neo in rotations.
     * @return The target position of the Neo in rotations.
     */
    public double getTargetPosition() {
        return targetPosition;
    }

    /**
     * Gets the target velocity of the Neo in rotations per minute.
     * @return The target velocity of the Neo in rotations per minute.
     */
    public double getTargetVelocity() {
        return targetVelocity;
    }

    public void addFollower(Neo follower) {
        addFollower(follower, false);
    }

    public void addFollower(Neo follower, boolean invert) {
        followers.add(follower);
        follower.follow(this);
    }

    public void setPID(double P, double I, double D, double minOutput, double maxOutput) {
        setPID(P, I, D, minOutput, maxOutput, 0);
    }

    public void setPID(double P, double I, double D, double minOutput, double maxOutput, int slotID) {
        pidController.setP(P, slotID);
        pidController.setI(I, slotID);
        pidController.setD(D, slotID);

        pidController.setOutputRange(minOutput, maxOutput, slotID);
    }

    public void setPositionConversionFactor(double factor) {
        encoder.setPositionConversionFactor(factor);
    }

    public void setVelocityConversionFactor(double factor) {
        encoder.setVelocityConversionFactor(factor);
    }

    /**
     * Gets the proportional gain constant for PIDFF controller.
     * @return The proportional gain constant for PIDFF controller.
     */
    public double getP() {
        return pidController.getP();
    }

    /**
     * Gets the integral gain constant for PIDFF controller.
     * @return The integral gain constant for PIDFF controller.
     */
    public double getI() {
        return pidController.getI();
    }

    /**
     * Gets the derivative gain constant for PIDFF controller.
     * @return The derivative gain constant for PIDFF controller.
     */
    public double getD() {
        return pidController.getD();
    }

    /**
     * Gets the I-Zone constant for PIDFF controller.
     * @return The I-Zone constant for PIDFF control.
     */
    public double getIZ() {
        return pidController.getIZone();
    }

    /**
     * Gets the feedforward gain constant for PIDFF controller.
     * @return The feedforward gain constant for PIDFF controller.
     */
    public double getFF() {
        return pidController.getFF();
    }

    // Documentation: https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
    public REVLibError changeStatusFrame(StatusFrame frame, int period) {
        REVLibError error = setPeriodicFramePeriod(frame.getFrame(), period);

        return error;
    }

    public REVLibError resetStatusFrame(StatusFrame frame) {
        return changeStatusFrame(frame, frame.getDefaultPeriod());
    }

    // Documentation: https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
    public enum StatusFrame {
        APPLIED_FAULTS_FOLLOWER(PeriodicFrame.kStatus0, 10),
        VELO_TEMP_VOLTAGE_CURRENT(PeriodicFrame.kStatus1, 20),
        ENCODER_POSITION(PeriodicFrame.kStatus2, 20),
        ALL_ANALOG_ENCODER(PeriodicFrame.kStatus3, 50),
        ALL_ALTERNATE_ENCODER(PeriodicFrame.kStatus4, 20),
        ABSOLUTE_ENCODER_POS(PeriodicFrame.kStatus5, 200),
        ABSOLUTE_ENCODER_VELO(PeriodicFrame.kStatus6, 200);

        private final PeriodicFrame frame;
        private final int defaultPeriod; // ms
        StatusFrame(PeriodicFrame frame, int defaultPeriod) {
            this.frame = frame;
            this.defaultPeriod = defaultPeriod;
        }

        public PeriodicFrame getFrame() {
            return frame;
        }

        public int getDefaultPeriod() {
            return defaultPeriod;
        }
    }

    public enum ControlLoopType {
        POSITION,
        VELOCITY,
        PERCENT;
    }

    public void setBrakeMode() {
        this.setIdleMode(CANSparkBase.IdleMode.kCoast);
    }

    public void setCoastMode() {
        this.setIdleMode(CANSparkBase.IdleMode.kBrake);
    }

    public enum TelemtryPreference {
        ONLY_ABSOLUTE_ENCODER,
        ONLY_RELATIVE_ENCODER,
        NO_TELEMETRY,
        NO_ENCODER
    }
    
    /**
     * Set the telemetry preference of the Neo
     * This will disable the telemtry status frames 
     * which is found at https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
     * @param type the enum to represent the telemetry preference
     *             this will tell the motor to only send 
     *             that type of telemtry
     */
    public void setTelemetryPreference(TelemtryPreference type) {
        final int maxDelay = NeoMotorConstants.MAX_PERIODIC_STATUS_TIME_MS;
        final int minDelay = NeoMotorConstants.FAST_PERIODIC_STATUS_TIME_MS;

        // No matter what preference, we don't use analog or external encoders.
        changeStatusFrame(StatusFrame.ALL_ALTERNATE_ENCODER, maxDelay);
        changeStatusFrame(StatusFrame.ALL_ANALOG_ENCODER, maxDelay);

        switch(type) {
            // Disable all telemetry that is unrelated to the encoder
            case NO_ENCODER: 
                changeStatusFrame(StatusFrame.ENCODER_POSITION, maxDelay);
                changeStatusFrame(StatusFrame.ALL_ANALOG_ENCODER, maxDelay);
                changeStatusFrame(StatusFrame.ABSOLUTE_ENCODER_VELO, maxDelay);
                break;
            // Disable all telemetry that is unrelated to absolute encoders
            case ONLY_ABSOLUTE_ENCODER:
                changeStatusFrame(StatusFrame.VELO_TEMP_VOLTAGE_CURRENT, maxDelay);
                changeStatusFrame(StatusFrame.ENCODER_POSITION, maxDelay);
                changeStatusFrame(StatusFrame.ABSOLUTE_ENCODER_POS, minDelay);
                changeStatusFrame(StatusFrame.ABSOLUTE_ENCODER_VELO, minDelay);
                break;
            // Disable all telemetry that is unrelated to the relative encoder
            case ONLY_RELATIVE_ENCODER: 
                changeStatusFrame(StatusFrame.ALL_ANALOG_ENCODER, maxDelay);
                changeStatusFrame(StatusFrame.ABSOLUTE_ENCODER_VELO, maxDelay);
                break;
            // Disable everything
            case NO_TELEMETRY:
                changeStatusFrame(StatusFrame.VELO_TEMP_VOLTAGE_CURRENT, maxDelay);
                changeStatusFrame(StatusFrame.ENCODER_POSITION, maxDelay);
                changeStatusFrame(StatusFrame.ALL_ANALOG_ENCODER, maxDelay);
                changeStatusFrame(StatusFrame.ABSOLUTE_ENCODER_VELO, maxDelay);
                break;
        }
    }
}