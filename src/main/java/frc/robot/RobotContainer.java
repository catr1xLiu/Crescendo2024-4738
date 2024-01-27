package frc.robot;

import java.util.Optional;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.path.PathPlannerTrajectory;
import com.revrobotics.CANSparkBase;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.commands.Drive;
import frc.robot.commands.autonomous.AutoPathStorage;
import frc.robot.subsystems.*;
import frc.robot.util.PatriBoxController;
import frc.robot.util.PoseCalculations;
import frc.robot.util.Constants.FieldConstants;
import frc.robot.util.Constants.NeoMotorConstants;
import frc.robot.util.Constants.OIConstants;
import monologue.Annotations.Log;
import monologue.Logged;

public class RobotContainer implements Logged {

    private final PatriBoxController driver;
    private final PatriBoxController operator;

    private final Swerve swerve;
    private final Intake intake;
    @SuppressWarnings("unused")
    private final DriverUI driverUI;
    private final Limelight limelight;
    private final AutoPathStorage autoPathStorage;

    private final Climb climb;
    
    public RobotContainer() {
        driver = new PatriBoxController(OIConstants.DRIVER_CONTROLLER_PORT, OIConstants.DRIVER_DEADBAND);
        operator = new PatriBoxController(OIConstants.OPERATOR_CONTROLLER_PORT, OIConstants.OPERATOR_DEADBAND);
        DriverStation.silenceJoystickConnectionWarning(true);

        limelight = new Limelight();
        intake = new Intake();
        climb = new Climb();
        swerve = new Swerve();
        driverUI = new DriverUI();
        autoPathStorage = new AutoPathStorage(driver.b());

        limelight.setDefaultCommand(Commands.run(() -> {
            // Create an "Optional" object that contains the estimated pose of the robot
            // This can be present (sees tag) or not present (does not see tag)
            Optional<Pose2d> result = limelight.getPose2d();
            // The skew of the tag represents how confident the camera is
            // If the result of the estimatedRobotPose exists, 
            // and the skew of the tag is less than 3 degrees, 
            // then we can confirm that the estimated position is realistic
            if (result.isPresent()) {
                swerve.getPoseEstimator().addVisionMeasurement(
                        result.get(),
                        DriverUI.currentTimestamp - limelight.getCombinedLatencySeconds());
            }
        }, limelight));

        swerve.setDefaultCommand(new Drive(
            swerve,
            driver::getLeftY,
            driver::getLeftX,
            () -> -driver.getRightX(),
            () -> !driver.leftBumper().getAsBoolean(),
            () -> (driver.leftBumper().getAsBoolean() && FieldConstants.ALLIANCE.equals(Optional.of(Alliance.Blue)))
        ));

        incinerateMotors();
        configureButtonBindings();

        setupAutoChooser();
        registerNamedCommands();
    }

    private void configureButtonBindings(){
        configureDriverBindings();
        configureOperatorBindings();
    }

    private void configureOperatorBindings() {
        operator.y().onTrue((climb.toTop(PoseCalculations.getChainPosition(swerve.getPose()))));
        operator.a().onTrue((climb.toBottom()));
    }

    private void configureDriverBindings() {

        // Upon hitting start or back,
        // reset the orientation of the robot
        // to be facing away from the driver station
        driver.start().or(driver.back()).onTrue(
            Commands.runOnce(() -> swerve.resetOdometry(
                new Pose2d(
                    swerve.getPose().getTranslation(), 
                    Rotation2d.fromDegrees(
                        FieldConstants.ALLIANCE.equals(Optional.of(Alliance.Red)) 
                        ? 0 
                        : 180))
            ), swerve)
        );

        driver.leftBumper().whileTrue(Commands.run(swerve::getSetWheelsX));

        driver.leftStick().toggleOnTrue(swerve.toggleSpeed());
      
        driver.a().and(intake.hasGamePieceTrigger().negate()).onTrue(intake.inCommand());

        driver.y().onTrue(intake.outCommand());

        driver.x().onTrue(intake.stopCommand());

    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }

    @Log.NT
    public static SendableChooser<Command> autoChooser = new SendableChooser<>();
    private void setupAutoChooser() {
        // TODO: Autos currently start at C1-5, we need to integrate the other paths
        // with the center line schenanigans to make full autos
        autoChooser.addOption("C1-5", autoPathStorage.generateCenterLineComplete(5, 5, true));
    }

    public void onDisabled() {}

    public void onEnabled() {}

    public void registerNamedCommands() {
        NamedCommands.registerCommand("Intake", intake.inCommand());
    }

    private void incinerateMotors() {
        Timer.delay(0.25);
        for (CANSparkBase neo : NeoMotorConstants.motors) {
            neo.burnFlash();
            Timer.delay(0.005);
        }
        Timer.delay(0.25);
    }

}
