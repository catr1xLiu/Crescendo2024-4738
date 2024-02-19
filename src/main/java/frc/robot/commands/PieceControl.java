package frc.robot.commands;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.Indexer;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.elevator.Trapper;
import frc.robot.subsystems.elevator.Elevator;
import frc.robot.util.SpeedAngleTriplet;

public class PieceControl {

    private Intake intake;
    private Indexer indexer;

    private Elevator elevator;
    private Trapper trapper;

    private ShooterCalc shooterCalc;

    public PieceControl(
            Intake intake,
            Indexer indexer,
            Elevator elevator,
            Trapper trapper,
            ShooterCalc shooterCalc) {
        this.intake = intake;
        this.indexer = indexer;
        this.elevator = elevator;
        this.trapper = trapper;
        this.shooterCalc = shooterCalc;
    }

    public Command stopAllMotors() {
        return Commands.parallel(
                intake.stop(),
                indexer.stop(),
                elevator.stop(),
                trapper.stop());
    }

    public Command shootWhenReady(Supplier<Pose2d> poseSupplier, Supplier<ChassisSpeeds> speedSupplier) {
        return Commands.waitUntil(shooterCalc.readyToShootSupplier())
                .andThen(noteToShoot())
                    .alongWith(shooterCalc.getNoteTrajectoryCommand(poseSupplier, speedSupplier));
    }

    // TODO: Possibly split this into two commands where one sends to shooter
    // without waiting
    public Command noteToShoot() {
        // this should be ran while we are aiming with pivot and shooter already
        // start running indexer so it gets up to speed and wait until shooter is at desired 
        // rotation and speed before sending note from trapper into indexer and then into 
        // shooter before stopping trapper and indexer
        return Commands.sequence(
                intake.inCommand(),
                trapper.intake(),
                indexer.toShooter(),
                Commands.waitUntil(intake.possessionTrigger()),
                indexCommand());

    }

    public Command noteToTrap() {
        // this should be ran while we are aiming with pivot and shooter already
        // start running indexer so it gets up to speed and wait until shooter is at desired 
        // rotation and speed before sending note from trapper into indexer and then into 
        // shooter before stopping trapper and indexer
        return Commands.sequence(
                intake.inCommand(),
                trapper.intake(),
                indexer.stop(),
                Commands.waitUntil(intake.possessionTrigger()),
                indexCommand());

    }

    public Command ejectNote() {
        // this should be ran while we are aiming with pivot and shooter already
        // start running indexer so it gets up to speed and wait until shooter is at desired 
        // rotation and speed before sending note from trapper into indexer and then into 
        // shooter before stopping trapper and indexer
        return Commands.sequence(
            intake.stop(),
            trapper.outtake(),
            indexer.toElevator(),
            Commands.waitSeconds(.75),
            stopAllMotors());

        // return Commands.sequence(
        //     intake.stop(),
        //     indexer.toElevator(),
        //     dropPieceCommand(),
        //     stopAllMotors()
        // );

    }

    public Command dropPieceCommand() {
        return Commands.sequence(
            elevator.indexCommand(),
            trapper.outtake(),
            Commands.waitSeconds(0.1),
            elevator.toBottomCommand()
        );
    }

    public Command indexCommand() {
        return elevator.indexCommand()
            .andThen(elevator.toBottomCommand())
            .alongWith(intake.stop());
    }

    public Command intakeAuto() {
        return Commands.sequence(
                intake.inCommand(),
                trapper.intake(),
                indexer.stop());
    }

    public Command noteToTarget() {
        // maybe make setPosition a command ORR Make the Elevator Command
        return elevator.toTopCommand()
                .andThen(Commands.waitUntil(elevator::atDesiredPosition))
                .andThen(trapper.placeCommand())
                .andThen(elevator.toBottomCommand());
    }

    public Command sourceShooterIntake(BooleanSupplier v1Mode) {
        return Commands.sequence(
            Commands.runOnce(() -> shooterCalc.setTriplet(new SpeedAngleTriplet(-300.0, -300.0, v1Mode.getAsBoolean() ? 60.0 : 45.0))),
            indexer.toElevator(),
            trapper.outtake(),
            Commands.waitSeconds(3),
            stopAllMotors()
        ); 
    }

    public Command intakeToTrapper() { 
        return intake.inCommand()
                .alongWith(indexer.toElevator());
    }

    public Command stopIntakeAndIndexer() {
        return intake.stop()
                .alongWith(indexer.stop())
                .alongWith(trapper.stop());
    }
}
