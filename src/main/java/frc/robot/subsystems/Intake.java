package frc.robot.subsystems;

import com.revrobotics.CANSparkLowLevel.PeriodicFrame;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.util.Neo;
import frc.robot.util.Constants.IntakeConstants;

public class Intake extends SubsystemBase {
    private final Neo intakeMotor;

    public Intake() {
        intakeMotor = new Neo(IntakeConstants.INTAKE_CAN_ID);
        configMotors();
    }

    public void configMotors() {
        intakeMotor.setSmartCurrentLimit(IntakeConstants.INTAKE_CURRENT_LIMIT_AMPS);
        // See https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces
        intakeMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 65535);
        intakeMotor.setPeriodicFramePeriod(PeriodicFrame.kStatus5, 65535);
    }

    public Command setPercentCommand(double desiredPercent) {
        return runOnce(() -> intakeMotor.setTargetPercent(desiredPercent));
    }

    public Command inCommand() {
        return setPercentCommand(IntakeConstants.INTAKE_PERCENT);
    }

    public Command outCommand() {
        return setPercentCommand(IntakeConstants.OUTTAKE_PERCENT);
    }

    public Command stop() {
        return setPercentCommand(IntakeConstants.STOP_PERCENT);
    }

    public Trigger hasGamePieceTrigger() {
        return new Trigger(() -> intakeMotor.getOutputCurrent() > IntakeConstants.HAS_PIECE_CURRENT_THRESHOLD);
    }

}
