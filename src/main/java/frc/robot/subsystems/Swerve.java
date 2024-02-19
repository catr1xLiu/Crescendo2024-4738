// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.ejml.sparse.csc.factory.FillReductionFactory_DSCC;

import com.ctre.phoenix6.hardware.Pigeon2;
import com.fasterxml.jackson.databind.ser.std.CalendarSerializer;
import com.pathplanner.lib.auto.AutoBuilder;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.commands.Drive;
import frc.robot.commands.DriveHDC;
import frc.robot.util.MAXSwerveModule;
import frc.robot.util.PIDNotConstants;
import frc.robot.util.PatriBoxController;
import frc.robot.util.PoseCalculations;
import frc.robot.util.Constants.AutoConstants;
import frc.robot.util.Constants.DriveConstants;
import frc.robot.util.Constants.FieldConstants;
import frc.robot.util.Constants.ModuleConstants;
import monologue.Logged;
import monologue.Annotations.Log;

public class Swerve extends SubsystemBase implements Logged {

    public static double twistScalar = 4;

    private double speedMultiplier = 1;
    private final MAXSwerveModule frontLeft = new MAXSwerveModule(
            DriveConstants.FRONT_LEFT_DRIVING_CAN_ID,
            DriveConstants.FRONT_LEFT_TURNING_CAN_ID,
            DriveConstants.FRONT_LEFT_CHASSIS_ANGULAR_OFFSET);

    private final MAXSwerveModule frontRight = new MAXSwerveModule(
            DriveConstants.FRONT_RIGHT_DRIVING_CAN_ID,
            DriveConstants.FRONT_RIGHT_TURNING_CAN_ID,
            DriveConstants.FRONT_RIGHT_CHASSIS_ANGULAR_OFFSET);

    private final MAXSwerveModule rearLeft = new MAXSwerveModule(
            DriveConstants.REAR_LEFT_DRIVING_CAN_ID,
            DriveConstants.REAR_LEFT_TURNING_CAN_ID,
            DriveConstants.BACK_LEFT_CHASSIS_ANGULAR_OFFSET);

    private final MAXSwerveModule rearRight = new MAXSwerveModule(
            DriveConstants.REAR_RIGHT_DRIVING_CAN_ID,
            DriveConstants.REAR_RIGHT_TURNING_CAN_ID,
            DriveConstants.BACK_RIGHT_CHASSIS_ANGULAR_OFFSET);

    private PIDNotConstants drivingConstants = new PIDNotConstants(
        ModuleConstants.DRIVING_PID, 
        frontLeft.drivingPIDController, 
        frontRight.drivingPIDController, 
        rearLeft.drivingPIDController,
        rearRight.drivingPIDController);
        
    private PIDNotConstants turningConstants = new PIDNotConstants(
        ModuleConstants.TURNING_PID, 
        frontLeft.turningPIDController, 
        frontRight.turningPIDController, 
        rearLeft.turningPIDController,
        rearRight.turningPIDController);

    @Log
    SwerveModuleState[] swerveMeasuredStates;

    @Log
    SwerveModuleState[] swerveDesiredStates;

    @Log
    Pose3d robotPose3d = new Pose3d();

    @Log
    Pose2d robotPose2d = new Pose2d();

    @Log.NT
    Field2d field2d = new Field2d();

    // The gyro sensor
    private final Pigeon2 gyro = new Pigeon2(DriveConstants.PIGEON_CAN_ID);

    private final MAXSwerveModule[] swerveModules = new MAXSwerveModule[] {
            frontLeft,
            frontRight,
            rearLeft,
            rearRight
    };

    private SwerveDrivePoseEstimator poseEstimator = new SwerveDrivePoseEstimator(
            DriveConstants.DRIVE_KINEMATICS,
            gyro.getRotation2d(),
            getModulePositions(),
            new Pose2d(),
            // Trust the information of the vision more
            // Nat.N1()).fill(0.1, 0.1, 0.1) --> trust more
            // Nat.N1()).fill(1.25, 1.25, 1.25) --> trust less
            // Notice that the theta on the vision is very large,
            // and the state measurement is very small.
            // This is because we assume that the IMU is very accurate.
            // You can visualize these graphs working together here:
            // https://www.desmos.com/calculator/a0kszyrwfe
            VecBuilder.fill(0.1, 0.1, 0.05),
            // State measurement
            // standard deviations
            // X, Y, theta
            VecBuilder.fill(0.8, 0.8, 2.5)
    // Vision measurement
    // standard deviations
    // X, Y, theta
    );

    /**
     * Creates a new DriveSu1stem.
     */
    public Swerve() {

        AutoBuilder.configureHolonomic(
                this::getPose,
                this::resetOdometry,
                this::getRobotRelativeVelocity,
                this::drive,
                AutoConstants.HPFC,
                Robot::isRedAlliance,
                this);

        resetEncoders();
        gyro.setYaw(0);
        setBrakeMode();

        SmartDashboard.putNumber("Swerve/RobotRotation", getPose().getRotation().getDegrees());
    }

    @Override
    public void periodic() {
        poseEstimator.updateWithTime(Robot.currentTimestamp, gyro.getRotation2d(), getModulePositions());
        // System.out.print("angle: " + gyro.getAngle()+ ", yaw: " +
        // gyro.getYaw().getValueAsDouble());
        logPositions();

    }
    public void logPositions() {

        Pose2d currentPose = getPose();

        swerveMeasuredStates = new SwerveModuleState[] {
                frontLeft.getState(), frontRight.getState(), rearLeft.getState(), rearRight.getState()
        };

        ChassisSpeeds speeds = DriveConstants.DRIVE_KINEMATICS.toChassisSpeeds(swerveMeasuredStates);

        if (FieldConstants.IS_SIMULATION) {
            resetOdometry(
                    currentPose.exp(
                            new Twist2d(
                                    0, 0,
                                    speeds.omegaRadiansPerSecond * .02)));
        }

        field2d.setRobotPose(currentPose);
        SmartDashboard.putNumber("Swerve/RobotRotation", currentPose.getRotation().getRadians());

        robotPose2d = currentPose;

        robotPose3d = new Pose3d(
                new Translation3d(
                        currentPose.getX(),
                        currentPose.getY(),
                        Math.hypot(
                                Rotation2d.fromDegrees(gyro.getRoll().refresh().getValue()).getSin()
                                        * DriveConstants.ROBOT_LENGTH_METERS / 2.0,
                                Rotation2d.fromDegrees(gyro.getPitch().refresh().getValue()).getSin() *
                                        DriveConstants.ROBOT_LENGTH_METERS / 2.0)),
                new Rotation3d(0, 0, currentPose.getRotation().getRadians()));

    }

    /**
     * Returns the currently-estimated pose of the robot.
     *
     * @return The pose.
     */
    public Pose2d getPose() {
        return poseEstimator.getEstimatedPosition();
    }
    public PIDNotConstants getTurningPidNotConstants() {
        return turningConstants;
    }
    public PIDNotConstants getDrivingPidNotConstants() {
        return drivingConstants;
    }
    public SwerveDrivePoseEstimator getPoseEstimator() {
        return poseEstimator;
    }

    public void drive(ChassisSpeeds speeds) {
        drive(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond, speeds.omegaRadiansPerSecond, false);
    }

    public void drive(double xSpeed, double ySpeed, double rotSpeed, boolean fieldRelative) {

        xSpeed   *= (DriveConstants.MAX_SPEED_METERS_PER_SECOND * speedMultiplier);
        ySpeed   *= (DriveConstants.MAX_SPEED_METERS_PER_SECOND * speedMultiplier);
        rotSpeed *= (DriveConstants.MAX_ANGULAR_SPEED_RADS_PER_SECOND * speedMultiplier);

        SwerveModuleState[] swerveModuleStates = DriveConstants.DRIVE_KINEMATICS.toSwerveModuleStates(
                fieldRelative
                        ? ChassisSpeeds.discretize(ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rotSpeed,
                                getPose().getRotation()), (Timer.getFPGATimestamp() - Robot.previousTimestamp))
                        : ChassisSpeeds.discretize(new ChassisSpeeds(xSpeed, ySpeed, rotSpeed),
                                (Timer.getFPGATimestamp() - Robot.previousTimestamp)));

        setModuleStates(swerveModuleStates);
    }

    public void stopMotors() {
        drive(0, 0, 0, false);
    }
    
    @Log
    Pose2d desiredHDCPose = new Pose2d();
    public void setDesiredPose(Pose2d pose) {
        desiredHDCPose = pose;
    }

    public ChassisSpeeds getRobotRelativeVelocity() {
        return DriveConstants.DRIVE_KINEMATICS.toChassisSpeeds(getModuleStates());
    }

    public ChassisSpeeds getFieldRelativeVelocity() {
        return ChassisSpeeds.fromRobotRelativeSpeeds(getRobotRelativeVelocity(), getPose().getRotation());
    }

    /**
     * Sets the wheels into an X formation to prevent movement.
     */
    public void setWheelsX() {
        SwerveModuleState[] desiredStates = new SwerveModuleState[4];
        desiredStates[0] = new SwerveModuleState(0, Rotation2d.fromDegrees(-45));
        desiredStates[1] = new SwerveModuleState(0, Rotation2d.fromDegrees(45));
        desiredStates[2] = new SwerveModuleState(0, Rotation2d.fromDegrees(45));
        desiredStates[3] = new SwerveModuleState(0, Rotation2d.fromDegrees(-45));

        setModuleStates(desiredStates);
    }

    public Command getSetWheelsX() {
        return run(this::setWheelsX);
    }

    /**
     * Sets the swerve ModuleStates.
     *
     * @param desiredStates The desired SwerveModule states.
     */
    public void setModuleStates(SwerveModuleState[] desiredStates) {
        SwerveDriveKinematics.desaturateWheelSpeeds(
            desiredStates, 
            DriveConstants.MAX_SPEED_METERS_PER_SECOND
        );
        frontLeft.setDesiredState(desiredStates[0]);
        frontRight.setDesiredState(desiredStates[1]);
        rearLeft.setDesiredState(desiredStates[2]);
        rearRight.setDesiredState(desiredStates[3]);

        this.swerveDesiredStates = desiredStates;
    }

    public void resetOdometry(Pose2d pose) {

        if (Double.isNaN(pose.getX()) || Double.isNaN(pose.getY()) || Double.isNaN(pose.getRotation().getRadians())) {
            return;
        }

        poseEstimator.resetPosition(
                gyro.getRotation2d(),
                getModulePositions(),
                pose);
    }

    public Command resetOdometryCommand(Supplier<Pose2d> pose) {
        return runOnce(() -> resetOdometry(pose.get()));
    }

    public SwerveModuleState[] getModuleStates() {

        SwerveModuleState[] states = new SwerveModuleState[4];

        for (int modNum = 0; modNum < swerveModules.length; modNum++) {
            states[modNum] = swerveModules[modNum].getState();
        }
        return states;

    }

    /**
     * Returns an array of SwerveModulePosition objects representing the positions of all swerve modules.
     * This is the position of the driving encoder and the turning encoder
     *
     * @return an array of SwerveModulePosition objects representing the positions of all swerve modules
     */
    public SwerveModulePosition[] getModulePositions() {

        SwerveModulePosition[] positions = new SwerveModulePosition[4];

        for (int modNum = 0; modNum < swerveModules.length; modNum++) {
            positions[modNum] = swerveModules[modNum].getPosition();
        }
        return positions;

    }
    public PIDNotConstants getTurningModulePID() {
        return this.frontLeft.getTurningPIDNotConstants();
    }
    public void resetEncoders() {
        for (MAXSwerveModule mSwerveMod : swerveModules) {
            mSwerveMod.resetEncoders();
        }
    }

    public Command toggleSpeed() {
        return runOnce(() -> this.speedMultiplier = (this.speedMultiplier == 1) ? 0.35 : 1);
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public Command setAlignmentSpeed() {
        return runOnce(() -> {
            DriveConstants.MAX_SPEED_METERS_PER_SECOND = FieldConstants.ALIGNMENT_SPEED;
        });
    }

    public double getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    /**
     * Sets the brake mode for the drive motors.
     * This is useful for when the robot is enabled
     * So we can stop the robot quickly
     * (This is the default mode)
     */
    public void setBrakeMode() {
        for (MAXSwerveModule mSwerveMod : swerveModules) {
            mSwerveMod.setBrakeMode();
        }
    }

    /**
     * Sets the coast mode for the drive motors.
     * This is useful for when the robot is disabled
     * So we can freely move the robot around
     */
    public void setCoastMode() {
        for (MAXSwerveModule mSwerveMod : swerveModules) {
            mSwerveMod.setCoastMode();
        }
    }

    public Command getAutoAlignmentCommand(Supplier<ChassisSpeeds> autoSpeeds, Supplier<ChassisSpeeds> controllerSpeeds) {
        return new Drive(this, () -> {
            ChassisSpeeds controllerSpeedsGet = controllerSpeeds.get();
            ChassisSpeeds autoSpeedsGet = autoSpeeds.get();
            return new ChassisSpeeds(
                    (controllerSpeedsGet.vxMetersPerSecond + autoSpeedsGet.vxMetersPerSecond),
                    -(controllerSpeedsGet.vyMetersPerSecond + autoSpeedsGet.vyMetersPerSecond),
                    controllerSpeedsGet.omegaRadiansPerSecond + autoSpeedsGet.omegaRadiansPerSecond);
        }, () -> false, () -> false);
    }

    public Command getDriveCommand(Supplier<ChassisSpeeds> speeds, BooleanSupplier fieldRelative) {
        return new Drive(this, speeds, fieldRelative, () -> false);
    }
    
    public DriveHDC getDriveHDCCommand(Supplier<ChassisSpeeds> speeds, BooleanSupplier fieldRelative) {
        return new DriveHDC(this, speeds, fieldRelative, () -> false);
    }

    public double getAlignmentSpeeds(Rotation2d desiredAngle) {
        return MathUtil.applyDeadband(AutoConstants.HDC.getThetaController().calculate(
            getPose().getRotation().getRadians(),
            desiredAngle.getRadians()),  0.02);
    }

    public ChassisSpeeds getAmpAlignmentSpeeds() {
        Pose2d ampPose = FieldConstants.GET_AMP_POSITION();
        Pose2d desiredPose = new Pose2d(
            ampPose.getX(),
            getPose().getY(),
            ampPose.getRotation()
        );
        setDesiredPose(desiredPose);
        return
            AutoConstants.HDC.calculate(
                getPose(),
                desiredPose,
                0,
                desiredPose.getRotation()
            );
    }

    public Command ampAlignmentCommand(DoubleSupplier driverX) {
        return 
            getAutoAlignmentCommand(
                () -> getAmpAlignmentSpeeds(), 
                () -> 
                    ChassisSpeeds.fromFieldRelativeSpeeds(
                        0,
                        driverX.getAsDouble() * (Robot.isRedAlliance() ? 1 : -1),
                        0,
                        getPose().getRotation()
                    )
            );
    }

    public ChassisSpeeds getChainRotationalSpeeds(double driverX, double driverY) {
        Pose2d closestChain = PoseCalculations.getClosestChain(getPose());
        return new ChassisSpeeds(
            driverY * (Robot.isRedAlliance() ? -1 : 1),
            driverX * (Robot.isRedAlliance() ? -1 : 1),
            getAlignmentSpeeds(closestChain.getRotation())
        );
    }

    public Command chainRotationalAlignment(DoubleSupplier driverX, DoubleSupplier driverY) {
        return getDriveCommand(() -> getChainRotationalSpeeds(driverX.getAsDouble(), driverY.getAsDouble()), () -> true);
    }

    public Command resetHDC() {
        return Commands.sequence(
            runOnce(() -> AutoConstants.HDC.getThetaController().reset(getPose().getRotation().getRadians())),
            runOnce(() -> AutoConstants.HDC.getXController().reset()),
            runOnce(() -> AutoConstants.HDC.getYController().reset())
        );
    }
}