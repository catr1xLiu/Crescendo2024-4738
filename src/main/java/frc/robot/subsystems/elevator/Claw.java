package frc.robot.subsystems.elevator;

import com.revrobotics.CANSparkLowLevel.PeriodicFrame;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.util.Neo;
import frc.robot.util.Constants.TrapConstants;
import frc.robot.util.Neo.TelemetryPreference;

public class Claw extends SubsystemBase {
    private final Neo claw;
    private boolean hasGamePiece = false;
    private double current = 0;
    private double startIntakingTimestamp = 0;

    /** Creates a new Claw. */
    public Claw() {
        claw = new Neo(TrapConstants.CLAW_CAN_ID);
        configMotors();
    }

    @Override
    public void periodic() {
        this.hasGamePiece = hasGamePiece();
        updateOutputCurrent();
    }

    public Command placeCommand() {
        return runOnce(this::outtake)
                .andThen(
                        Commands.waitSeconds(TrapConstants.OUTTAKE_SECONDS))
                .andThen(
                        runOnce(this::stopCommand));
    }

    public Command intakeFromHandoff() {
        return runOnce(this::intake)
                .andThen(
                        Commands.waitSeconds(TrapConstants.INTAKE_TIME))
                .andThen(
                        runOnce(this::stopCommand));
    }

    public boolean hasGamePiece() {
        // if appliedoutput <= desired/2 and
        // current this loop and last loop > constant
        // desired speed > constant
        // we've been intaking over .25 seconds (make constant);
        if (hasGamePiece) {
            this.hasGamePiece = claw.getTargetVelocity() > 0;
        } else {
            this.hasGamePiece = (TrapConstants.CLAW_HAS_PIECE_LOWER_LIMIT < claw.getAppliedOutput() &&
                    claw.getAppliedOutput() < TrapConstants.CLAW_HAS_PIECE_UPPER_LIMIT) &&
                    (current > TrapConstants.CLAW_HAS_PIECE_MIN_CURRENT &&
                            claw.getOutputCurrent() > TrapConstants.CLAW_HAS_PIECE_MIN_CURRENT)
                    &&
                    (claw.getTargetVelocity() > TrapConstants.CLAW_HAS_PIECE_MIN_TARGET_VELO) &&
                    (Robot.currentTimestamp - startIntakingTimestamp > TrapConstants.CLAW_HAS_PIECE_MIN_TIMESTAMP);
        }
        return hasGamePiece;
    }

    public void setSpeed(double percent) {
        percent = 
            MathUtil.clamp(
                percent, 
                TrapConstants.CLAW_LOWER_PERCENT_LIMIT, 
                TrapConstants.CLAW_UPPER_PERCENT_LIMIT);
        claw.set(percent);
    }

    public void configMotors() {
        // needs motor configs
        claw.setSmartCurrentLimit(TrapConstants.CLAW_CURRENT_LIMIT);
        claw.setInverted(false);
        claw.setBrakeMode();
        claw.setTelemetryPreference(TelemetryPreference.ONLY_RELATIVE_ENCODER);
    }

    public void updateOutputCurrent() {
        current = claw.getOutputCurrent();
    }

    public boolean getHasGamePiece() {
        return this.hasGamePiece;
    }

    public Command outtake() {
        return runOnce(() -> claw.set(TrapConstants.CLAW_OUTTAKE_PERCENT));
    }

    public Command stopCommand() {
        return runOnce(() -> claw.set(TrapConstants.CLAW_STOP_PERCENT));
    }
    public void updateIntakingTimestamp() {
        startIntakingTimestamp = Robot.currentTimestamp;
    }

    public Command intake() {
        return runOnce(() -> updateIntakingTimestamp())
                .andThen(runOnce(() -> claw.set(TrapConstants.CLAW_INTAKE_PERCENT)));
    }

}
