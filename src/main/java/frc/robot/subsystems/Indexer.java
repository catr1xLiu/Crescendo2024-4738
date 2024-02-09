package frc.robot.subsystems;

import com.revrobotics.CANSparkLowLevel.PeriodicFrame;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.util.Constants.IntakeConstants;
import frc.robot.util.Neo;
import frc.robot.util.Neo.TelemetryPreference;

public class Indexer extends SubsystemBase {
    private final Neo triggerWheel;
    private double desiredPercent;

    public Indexer() {
        triggerWheel = new Neo(IntakeConstants.TRIGGER_WHEEL_CAN_ID);
        desiredPercent = 0;
        configMotor();
    }

    public void configMotor() {
        // See https://docs.revrobotics.com/sparkmax/operating-modes/control-interfaces
        triggerWheel.setSmartCurrentLimit(IntakeConstants.TRIGGER_WHEEL_STALL_CURRENT_LIMIT_AMPS,
                IntakeConstants.TRIGGER_WHEEL_FREE_CURRENT_LIMIT_AMPS);
        triggerWheel.setPeriodicFramePeriod(PeriodicFrame.kStatus3, 65535);
        triggerWheel.setPeriodicFramePeriod(PeriodicFrame.kStatus5, 65535);
        triggerWheel.setInverted(false);

        // sets brake mode
        triggerWheel.setBrakeMode();

        triggerWheel.setTelemetryPreference(TelemetryPreference.NO_ENCODER);
    }

    public double getDesiredPercent() {
        return desiredPercent;
    }

    public void setDesiredPercent(double percent) {
        desiredPercent = percent;
        triggerWheel.setTargetPercent(percent);
    }

    public Command setPercentCommand(double percent) {
        return runOnce(() -> setDesiredPercent(percent));
    }

    public Command toShooter() {
        return setPercentCommand(IntakeConstants.SHOOTER_TRIGGER_WHEEL_PERCENT);
    }

    public Command toElevator() {
        return setPercentCommand(IntakeConstants.TRAP_TRIGGER_WHEEL_PERCENT);
    }

    public Command stop() {
        return setPercentCommand(IntakeConstants.STOP_PERCENT);
    }
}