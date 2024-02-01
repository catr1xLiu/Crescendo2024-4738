package frc.robot.commands;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.Swerve;
import frc.robot.subsystems.shooter.*;
import frc.robot.util.Constants.FieldConstants;
import frc.robot.util.Constants.ShooterConstants;
import monologue.Logged;
import monologue.Annotations.Log;
import frc.robot.util.SpeedAngleTriplet;

public class ShooterCalc implements Logged{

    private Pivot pivot;
    private Shooter shooter;
    private boolean aiming;


    @Log.NT
    double desiredRSpeed, distance;

    @Log.NT
    double desiredAngle = 0;
    

    public ShooterCalc(Shooter shooter, Pivot pivot) {
        this.pivot = pivot;
        this.shooter = shooter;
        this.aiming = false;
    }

    /**
     * The function prepares a fire command by calculating the speed and angle for
     * the robot's shooter
     * based on the robot's pose and whether it should shoot at the speaker.
     * 
     * @param shootAtSpeaker A BooleanSupplier that returns true if the robot should
     *                       shoot at the
     *                       speaker, and false otherwise.
     * @param robotPose      The `robotPose` parameter represents the current pose
     *                       (position and orientation)
     *                       of the robot. It is of type `Pose2d`.
     * @return The method is returning a Command object.
     */ //WORKING?
    public void prepareFireCommand(BooleanSupplier shootAtSpeaker, Pose2d robotPose) {
        SpeedAngleTriplet triplet = calculateSpeed(robotPose, shootAtSpeaker.getAsBoolean());

        desiredAngle = triplet.getAngle();
        desiredRSpeed = triplet.getRightSpeed();

        // System.out.println("uh yeah");
        
        Commands.print(robotPose.toString()).andThen(
                pivot.setAngleCommand(triplet.getAngle())
                .alongWith(shooter.setSpeedCommand(triplet.getSpeeds()))).schedule();
    }

    /**
     * The function prepares a command to fire at a stationary target while moving
     * and continuously aiming until
     * instructed to stop.
     * 
     * If we are currently aiming, toggle aiming to false, cancelling repeated calls
     * to prepareFireCommand
     * If we are not currently aiming, toggle aiming to true,
     * and repeatedly run prepareFireCommand until aiming is toggled to false
     * Allows for toggling on and off of aiming with single button
     * 
     * @param shootAtSpeaker A BooleanSupplier that determines whether the robot
     *                       should shoot at the
     *                       speaker.
     * @param swerve         The "swerve" parameter is an instance of the Swerve
     *                       class. It is used to access the
     *                       current pose (position and orientation) of the swerve
     *                       mechanism.
     * @return The method is returning a Command object.
     */
    public Command prepareFireMovingCommand(BooleanSupplier shootAtSpeaker, Swerve swerve) {
        if (aiming) {
            return Commands.runOnce(() -> toggleAiming());
        }
        return Commands.runOnce(() -> toggleAiming())
                // .andThen(prepareFireCommand(shootAtSpeaker, swerve.getPose())
                        .repeatedly()
                        .until(() -> !aiming);
    }

    /**
     * The function prepares a shooter command by calculating the speed and angle
     * based on the robot's
     * pose and whether it should shoot at the speaker, and then sets the shooter's
     * speed accordingly.
     * 
     * Sets shooter up to speed without regard to pivot angle
     *
     * @param shootAtSpeaker A BooleanSupplier that returns true if the robot should
     *                       shoot at the
     *                       speaker, and false otherwise.
     * @param robotPose      The robotPose parameter represents the current pose
     *                       (position and orientation) of
     *                       the robot. It is used in the calculateSpeed method to
     *                       determine the speed at which the shooter
     *                       should be set.
     * @return The method is returning a Command object.
     */
    public Command prepareShooterCommand(BooleanSupplier shootAtSpeaker, Pose2d robotPose) {
        SpeedAngleTriplet triplet = calculateSpeed(robotPose, shootAtSpeaker.getAsBoolean());
        return shooter.setSpeedCommand(triplet.getSpeeds());
    }

    /**
     * The function prepares a pivot command based on whether the robot should shoot
     * at the speaker and
     * the current robot pose.
     * 
     * @param shootAtSpeaker A BooleanSupplier that returns true if the robot should
     *                       shoot at the
     *                       speaker, and false otherwise.
     * @param robotPose      The `robotPose` parameter represents the current pose
     *                       (position and orientation)
     *                       of the robot. It is of type `Pose2d`.
     * @return The method is returning a Command object.
     */
    public Command preparePivotCommand(BooleanSupplier shootAtSpeaker, Pose2d robotPose) {
        SpeedAngleTriplet triplet = calculateSpeed(robotPose, shootAtSpeaker.getAsBoolean());
        
        return pivot.setAngleCommand(triplet.getAngle());
    }

    /**
     * The function is a command that resets the shooter to a speed of 0 and an
     * angle constant and
     * once it has reached its desired states it sets the shooter to a negative
     * speed to pass the
     * piece back to handoff
     * 
     * @return The method is returning a Command object.
     */
    public Command sendBackCommand() {
        return resetShooter()
                .andThen(Commands.waitUntil(
                        () -> pivot.atDesiredAngle().getAsBoolean() && shooter.atDesiredRPM().getAsBoolean()))
                .andThen(shooter.setSpeedCommand(ShooterConstants.SHOOTER_BACK_SPEED));
    }

    /**
     * Makes aiming false so that we stop any aiming loop currently happening, and
     * then sets the
     * shooter to a speed of 0 and the pivot angle to a predetermined constant
     * 
     * @return The method is returning a Command object.
     */
    public Command resetShooter() {
        return Commands.runOnce(() -> stopAiming())
                .andThen(shooter.stop()
                        .alongWith(pivot.setRestAngleCommand()));
    }

    // Toggles the aiming boolean
    private void toggleAiming() {
        this.aiming = !aiming;
    }

    // Sets the aiming boolean to false
    private void stopAiming() {
        this.aiming = false;
    }

    // Sets the aiming boolean to true
    private void startAiming() {
        this.aiming = true;
    }

    // Gets a SpeedAngleTriplet by interpolating values from a map of already
    // known required speeds and angles for certain poses
    public SpeedAngleTriplet calculateSpeed(Pose2d robotPose, boolean shootingAtSpeaker) {
        // Constants have blue alliance positions at index 0
        // and red alliance positions at index 1
        int positionIndex = FieldConstants.ALLIANCE == Optional.ofNullable(Alliance.Blue) ? 0 : 1;

        // Get our position relative to the desired field element
        if (shootingAtSpeaker) {
            robotPose = robotPose.relativeTo(FieldConstants.SPEAKER_POSITIONS[positionIndex]);
        } else {
            robotPose = robotPose.relativeTo(FieldConstants.AMP_POSITIONS[positionIndex]);
        }

        // Use the distance as our key for interpolation
        double distanceFeet = Units.metersToFeet(robotPose.getTranslation().getNorm());

        this.distance = robotPose.getX();

        return ShooterConstants.INTERPOLATION_MAP.get(distanceFeet);
    }

    public boolean pivotAtDesiredAngle() {
        return pivot.atDesiredAngle().getAsBoolean();
    }

    public boolean shooterAtDesiredRPM() {
        return shooter.atDesiredRPM().getAsBoolean();
    }

    public Command stopMotors() {
        return Commands.parallel(
                shooter.stop(),
                pivot.stop());
    }
}