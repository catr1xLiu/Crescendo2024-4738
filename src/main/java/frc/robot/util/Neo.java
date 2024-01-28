// Developed in tandem with Reza from Team Spyder (1622)

package frc.robot.util;

import com.revrobotics.CANSparkBase;
import com.revrobotics.CANSparkLowLevel;
import com.revrobotics.CANSparkMax;
import com.revrobotics.REVLibError;
import com.revrobotics.REVPhysicsSim;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkPIDController;
import com.revrobotics.SparkPIDController.ArbFFUnits;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.wpilibj.Timer;
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

        restoreFactoryDefaults();
        Timer.delay(0.050);

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
     * Sets the target position for the Neo motor
     * with an optional arbitrary feedforward value.
     * 
     * @param position             The target position for the motor.
     * @param arbitraryFeedForward An optional arbitrary feedforward value.
     */
    public void setTargetPosition(double position, double arbitraryFeedForward) {
        setTargetPosition(position, arbitraryFeedForward, 0);
    }

    /**
     * Sets the target position for the Neo motor
     * with an optional arbitrary feedforward value.
     * 
     * @param position The target position for the motor.
     * @param slot     The PID slot for the reference
     */
    public void setTargetPosition(double position, int slot) {
        setTargetPosition(position, 0, slot);
    }

    /**
     * Sets the target position for the Neo motor.
     * 
     * @param position the desired target position
     */
    public void setTargetPosition(double position) {
        setTargetPosition(position, 0, 0);
    }
    
    /**
     * Sets the target position for the Neo motor controller.
     * 
     * @param position             The desired position in encoder units.
     * @param arbitraryFeedForward The arbitrary feedforward value to be applied.
     * @param slot                 The PID slot to use for control.
     */
    public void setTargetPosition(double position, double arbitraryFeedForward, int slot) {
        position *= reversedMultiplier;
        if (!FieldConstants.IS_SIMULATION) {
            pidController.setReference(position, ControlType.kPosition, slot, arbitraryFeedForward, ArbFFUnits.kVoltage);
        }
        targetPosition = position;
        controlType = ControlLoopType.POSITION;
    }

    /**
     * Sets the target percent for the Neo motor.
     * 
     * @param percent the target percent to set
     */
    public void setTargetPercent(double percent) {
        setTargetPercent(percent, 0);
    }

    /**
     * Sets the target percent of speed output for the Neo.
     * 
     * @param percent Percent of Neo output speed.
     * @param slot The PID slot.
     */
    public void setTargetPercent(double percent, int slot) {
        setTargetPercent(percent, 0, slot);
    }

    /**
     * Sets the target percent of speed output for the Neo.
     * 
     * @param percent              Percent of Neo output speed.
     * @param arbitraryFeedForward The FF for the reference
     */
    public void setTargetPercent(double percent, double arbitraryFeedForward) {
        setTargetPercent(percent, arbitraryFeedForward, 0);
    }

    /**
     * Sets the target percent output for the Neo motor controller.
     * 
     * @param percent              The desired percent output (-1.0 to 1.0).
     * @param arbitraryFeedForward The arbitrary feedforward value.
     * @param slot                 The PID slot to use.
     */
    public void setTargetPercent(double percent, double arbitraryFeedForward, int slot) {
        percent *= reversedMultiplier;
        if (percent == 0) {
            setVoltage(0);
        } else {
            pidController.setReference(percent, ControlType.kDutyCycle, slot, arbitraryFeedForward, ArbFFUnits.kPercentOut);
        }
        targetPercent = percent;
        controlType = ControlLoopType.PERCENT;
    }

    /**
     * Sets the target velocity for the Neo.
     * @param velocity Velocity to set the Neo to in rotations per minute.
     */
    public void setTargetVelocity(double velocity) {
        setTargetVelocity(velocity, 0);
    }

    /**
     * Sets the target velocity for the Neo.
     * 
     * @param velocity             Velocity to set the Neo to in rotations per
     *                             minute.
     * @param arbitraryFeedForward The FF for the reference.
     */
    public void setTargetVelocity(double velocity, double arbitraryFeedForward) {
        setTargetVelocity(velocity, arbitraryFeedForward, 0);
    }
    
    /**
     * Sets the target velocity for the Neo motor.
     * 
     * @param velocity the target velocity to set
     * @param slot     the slot to set the target velocity for
     */
    public void setTargetVelocity(double velocity, int slot) {
        setTargetVelocity(velocity, 0, slot);
    }

    /**
     * Sets the target velocity for the Neo.
     * 
     * @param velocity             Velocity to set the Neo to in rotations per
     *                             minute.
     * @param arbitraryFeedForward Arbitrary feed forward to add to the motor
     *                             output.
     */
    public void setTargetVelocity(double velocity, double arbitraryFeedForward, int slot) {
        velocity *= reversedMultiplier;
        if (velocity == 0) {
            setVoltage(0);
        } else {
            pidController.setReference(velocity, ControlType.kVelocity, slot, arbitraryFeedForward, ArbFFUnits.kVoltage);
        }
        targetVelocity = velocity;
        controlType = ControlLoopType.VELOCITY;
    }

    
    /**
     * Sets the output of the Neo motor controller based on a percentage value.
     * Positive values are counter clockwise if there is no reversedMultiplier
     * 
     * @param percent The percentage value to set the motor output to.
     */
    public void set(double percent) {
        percent *= reversedMultiplier;
        super.set(percent);
        controlType = ControlLoopType.PERCENT;
    }

    private boolean shouldCache = false;
    private double position = 0;
    private double velo = 0;
    
    /**
     * Updates the position and velocity of the Neo motor.
     * If caching is enabled, it caches the position and velocity values.
     * If the simulation is active and the control type is position, 
     * it simulates movement on the motor
     */
    public void tick() {
        if (shouldCache) {
            position = encoder.getPosition();
            velo = encoder.getVelocity();
        }

        if ((FieldConstants.IS_SIMULATION) && controlType == ControlLoopType.POSITION) {
            setVoltage(pidController.getP() * (targetPosition - getPosition()));
        }
    }


    /**
     * Registers the Neo motor instance.
     * Adds the motor to the list of registered motors
     * and also adds the motor to the REVPhysicsSim instance
     * if in simulation.
     */
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
        
        double pos = shouldCache ? position : encoder.getPosition();

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
        return shouldCache ? velo : encoder.getVelocity(); 
    }

    /**
     * Sets the position of the Neo encoder.
     * 
     * @param position the desired position to set
     */
    public void setPosition(double position) {
        encoder.setPosition(position * reversedMultiplier);
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

    /**
     * Gets the target percentage of the Neo in... well, %
     * @return The target percent speed of the Neo
     */
    public double getTargetPercent() {
        return targetPercent;
    }

    /**
     * Adds a follower to the Neo motor.
     * 
     * @param follower the Neo motor to be added as a follower
     */
    public void addFollower(Neo follower) {
        addFollower(follower, false);
    }

    /**
     * Adds a follower to the Neo motor.
     * 
     * @param follower The Neo motor to be added as a follower.
     * @param invert   Specifies whether the follower should be inverted or not.
     */
    public void addFollower(Neo follower, boolean invert) {
        followers.add(follower);
        follower.follow(this, invert);
    }

    /**
     * Sets the PID values and output limits for the Neo motor controller.
     * 
     * @param P          the proportional gain
     * @param I          the integral gain
     * @param D          the derivative gain
     * @param minOutput  the minimum output value
     * @param maxOutput  the maximum output value
     */
    public void setPID(double P, double I, double D, double minOutput, double maxOutput) {
        setPID(P, I, D, minOutput, maxOutput, 0);
    }

    /**
     * Sets the PID values and output range for the PID controller.
     * 
     * @param P          the proportional gain
     * @param I          the integral gain
     * @param D          the derivative gain
     * @param minOutput  the minimum output value
     * @param maxOutput  the maximum output value
     * @param slotID     the slot ID of the PID controller
     */
    public void setPID(double P, double I, double D, double minOutput, double maxOutput, int slotID) {
        pidController.setP(P, slotID);
        pidController.setI(I, slotID);
        pidController.setD(D, slotID);

        pidController.setOutputRange(minOutput, maxOutput, slotID);
    }

    /**
     * Sets the position conversion factor for the encoder.
     * This is generally a gear ratio
     * 
     * @param factor the conversion factor to set
     */
    public void setPositionConversionFactor(double factor) {
        encoder.setPositionConversionFactor(factor);
    }

    /**
     * Sets the velocity conversion factor for the encoder.
     * This is generally a gear ratio
     * 
     * @param factor the conversion factor to set
     */
    public void setVelocityConversionFactor(double factor) {
        encoder.setVelocityConversionFactor(factor);
    }

    /**
     * Gets the proportional gain constant for PID controller.
     * @return The proportional gain constant for PID controller.
     */
    public double getP() {
        return pidController.getP();
    }

    /**
     * Gets the integral gain constant for PID controller.
     * @return The integral gain constant for PID controller.
     */
    public double getI() {
        return pidController.getI();
    }

    /**
     * Gets the derivative gain constant for PID controller.
     * @return The derivative gain constant for PID controller.
     */
    public double getD() {
        return pidController.getD();
    }

    /**
     * Gets the I-Zone constant for PID controller.
     * The I-Zone is the zone at which the integral
     * will be applied and "enabled"
     *
     * Example: 
     *   an I-Zone of 0.25
     *   will make the integral apply 
     *   for when the absolute value of
     *   | desired - current | is 0.25 or less.
     * 
     * Think of this like the final stretch of PID
     * 
     * @return The I-Zone constant for PID control.
     */
    public double getIZ() {
        return pidController.getIZone();
    }

    /**
     * Gets the feedforward gain constant for PID controller.
     * @return The feedforward gain constant for PID controller.
     */
    public double getFF() {
        return pidController.getFF();
    }

    /**
     * Represents an error that can occur while using the REVLib library.
     * Rev docs:
     * https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
     * 
     * @param frame  The status frame to reset
     * @param period The update period for the status frame.
     * @return error
     */
    public REVLibError changeStatusFrame(StatusFrame frame, int period) {
        REVLibError error = setPeriodicFramePeriod(frame.getFrame(), period);

        return error;
    }

    /**
     * Resets the status frame of the NEO motor controller to its default period.
     * Rev docs: https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
     * 
     * @param frame the status frame to reset
     * @return the REVLibError indicating the result of the operation
     */
    public REVLibError resetStatusFrame(StatusFrame frame) {
        return changeStatusFrame(frame, frame.getDefaultPeriodms());
    }

    /**
     * Represents the status frames for the Neo class.
     * Rev docs: https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces#periodic-status-frames
     */
    public enum StatusFrame {
        APPLIED_FAULTS_FOLLOWER(PeriodicFrame.kStatus0, 10),
        VELO_TEMP_VOLTAGE_CURRENT(PeriodicFrame.kStatus1, 20),
        MOTOR_POSITION(PeriodicFrame.kStatus2, 20),
        ANALOG_VOLTAGE_VELO_POS(PeriodicFrame.kStatus3, 50),
        ALTERNATE_VELO_POS(PeriodicFrame.kStatus4, 20),
        ABSOLUTE_ENCODER_POS(PeriodicFrame.kStatus5, 200),
        ABSOLUTE_ENCODER_VELO(PeriodicFrame.kStatus6, 200);

        private final PeriodicFrame frame;
        private final int defaultPeriodms;

        /**
         * Constructs a StatusFrame with the specified frame and default period.
         * 
         * @param frame         The periodic frame.
         * @param defaultPeriod The default period in milliseconds.
         */
        StatusFrame(PeriodicFrame frame, int defaultPeriod) {
            this.frame = frame;
            this.defaultPeriodms = defaultPeriod;
        }

        /**
         * Gets the periodic frame associated with this StatusFrame.
         * 
         * @return The periodic frame.
         */
        public PeriodicFrame getFrame() {
            return frame;
        }

        /**
         * Gets the default period in milliseconds for this StatusFrame.
         * 
         * @return The default period in milliseconds.
         */
        public int getDefaultPeriodms() {
            return defaultPeriodms;
        }
    }

    /**
     * Enum representing the control loop types for the Neo motor.
     * The control loop types include POSITION, VELOCITY, and PERCENT.
     */
    public enum ControlLoopType {
        POSITION,
        VELOCITY,
        PERCENT;
    }

    /**
     * Sets the motor to brake mode.
     * This will make it try to stop when not power.
     */
    public void setBrakeMode() {
        this.setIdleMode(CANSparkBase.IdleMode.kCoast);
    }

    /**
     * Sets the motor to coast mode.
     * This will make it spin freely when not powered.
     */
    public void setCoastMode() {
        this.setIdleMode(CANSparkBase.IdleMode.kBrake);
    }
}