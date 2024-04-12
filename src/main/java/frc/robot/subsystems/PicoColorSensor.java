package frc.robot.subsystems;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.revrobotics.ColorMatchResult;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.hal.SerialPortJNI;
import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.util.Color;
import frc.robot.util.Constants.ColorSensorConstants;
import monologue.Annotations.Log;
import monologue.Logged;

public class PicoColorSensor implements AutoCloseable, Logged {
    public static class RawColor {
        public RawColor(int r, int g, int b, int _ir) {
            red = r;
            green = g;
            blue = b;
            ir = _ir;
        }

        public RawColor() {}

        public int red;
        public int green;
        public int blue;
        public int ir;
    }

    private static class SingleCharSequence implements CharSequence {
        public byte[] data;

        @Override
        public int length() {
            return data.length;
        }

        @Override
        public char charAt(int index) {
            return (char) data[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new String(data, start, end, StandardCharsets.UTF_8);
        }

    }

    private static class IntRef { int value; }

    int parseIntFromIndex(SingleCharSequence charSeq, int readLen, IntRef lastComma) {
        int nextComma = 0;
        try {
            nextComma = findNextComma(charSeq.data, readLen, lastComma.value);
            int value = Integer.parseInt(charSeq, lastComma.value + 1, nextComma, 10);
            lastComma.value = nextComma;
            return value;
        } catch (Exception ex) {
            return 0;
        }
    }

    private int findNextComma(byte[] data, int readLen, int lastComma) {
        while (true) {
            if (readLen <= lastComma + 1) {
                return readLen;
            }
            lastComma++;
            if (data[lastComma] == ',') {
                break;
            }
        }
        return lastComma;
    }

    private final AtomicBoolean debugPrints = new AtomicBoolean();
    @Log
    private boolean hasColorElevator;
    @Log
    private boolean hasColorShooter;
    @Log
    private int proximityElevator;
    @Log
    private int proximityShooter;
    private final RawColor colorElevator = new RawColor();
    private final RawColor colorShooter = new RawColor();
    private double lastReadTime;
    private final ReentrantLock threadLock = new ReentrantLock();
    private final Thread readThread;
    private final AtomicBoolean threadRunning = new AtomicBoolean(true);

    private Color detectedColorElevator = new Color(0.0, 0.0, 0.0);
    private Color detectedColorShooter = new Color(0.0, 0.0, 0.0);
    @Log
    private String elevatorColorString;
    @Log
    private String shooterColorString;
    @Log
    private boolean hasNoteElevator = false;
    @Log
    private boolean hasNoteShooter = false;
    private ColorMatchResult elevatorMatch = ColorSensorConstants.COLOR_MATCH.matchClosestColor(detectedColorElevator);
    private ColorMatchResult shooterMatch = ColorSensorConstants.COLOR_MATCH.matchClosestColor(detectedColorShooter);
    @Log
    private String matchStringElevator;
    @Log
    private String matchStringShooter;

    private void threadMain() {
        // Using JNI for a non allocating read
        
        int port = SerialPortJNI.serialInitializePort((byte) 1);
        SerialPortJNI.serialSetBaudRate(port, 115200);
        SerialPortJNI.serialSetDataBits(port, (byte) 8);
        SerialPortJNI.serialSetParity(port, (byte) 0);
        SerialPortJNI.serialSetStopBits(port, (byte) 10);

        SerialPortJNI.serialSetTimeout(port, 1);
        SerialPortJNI.serialEnableTermination(port, '\n');

        HAL.report(tResourceType.kResourceType_SerialPort, 2);

        byte[] buffer = new byte[257];
        SingleCharSequence charSeq = new SingleCharSequence();
        charSeq.data = buffer;
        IntRef lastComma = new IntRef();

        RawColor colorElevator = new RawColor();
        RawColor colorShooter = new RawColor();

        while (threadRunning.get()) {

            int read = SerialPortJNI.serialRead(port, buffer, buffer.length - 1);
            if (read <= 0) {
                try {
                    threadLock.lock();
                    this.hasColorElevator = false;
                    this.hasColorShooter = false;
                } finally {
                    threadLock.unlock();
                }
                continue;
            }
            if (!threadRunning.get()) {
                System.out.println("No thread");
                break;
            }

            // Trim trailing newline if exists
            if (buffer[read - 1] == '\n') {
                read--;
            }

            if (read == 0) {
                continue;
            }

            if (debugPrints.get()) {
                System.out.println(new String(buffer, 0, read, StandardCharsets.UTF_8));
            }

            lastComma.value = -1;

            boolean hasColorElevator = parseIntFromIndex(charSeq, read, lastComma) != 0;
            boolean hasColorShooter = parseIntFromIndex(charSeq, read, lastComma) != 0;
            colorElevator.red = parseIntFromIndex(charSeq, read, lastComma);
            colorElevator.green = parseIntFromIndex(charSeq, read, lastComma);
            colorElevator.blue = parseIntFromIndex(charSeq, read, lastComma);
            colorElevator.ir = parseIntFromIndex(charSeq, read, lastComma);
            int proxElevator = parseIntFromIndex(charSeq, read, lastComma);
            colorShooter.red = parseIntFromIndex(charSeq, read, lastComma);
            colorShooter.green = parseIntFromIndex(charSeq, read, lastComma);
            colorShooter.blue = parseIntFromIndex(charSeq, read, lastComma);
            colorShooter.ir = parseIntFromIndex(charSeq, read, lastComma);
            int proxShooter = parseIntFromIndex(charSeq, read, lastComma);

            double ts = Timer.getFPGATimestamp();

            try {
                threadLock.lock();
                
                this.lastReadTime = ts;
                this.hasColorElevator = hasColorElevator;
                this.hasColorShooter = hasColorShooter;

                if (hasColorElevator) {
                    this.colorElevator.red = colorElevator.red;
                    this.colorElevator.green = colorElevator.green;
                    this.colorElevator.blue = colorElevator.blue;
                    this.colorElevator.ir = colorElevator.ir;
                    this.proximityElevator = proxElevator;

                    this.detectedColorElevator = new Color(this.colorElevator.red, this.colorElevator.green, this.colorElevator.blue);
                    this.elevatorMatch = ColorSensorConstants.COLOR_MATCH.matchClosestColor(detectedColorElevator);
                    this.elevatorColorString = detectedColorElevator.toString();

                    if (this.proximityElevator > 150) {
                        this.matchStringElevator = "Note";
                        this.hasNoteElevator = true;
                    } else {
                        this.matchStringElevator = "Surely not a note";
                        hadNoteElevatorLastLoop = hasNoteElevator;
                        this.hasNoteElevator = false;
                    }
                }
                if (hasColorShooter) {
                    this.colorShooter.red = colorShooter.red;
                    this.colorShooter.green = colorShooter.green;
                    this.colorShooter.blue = colorShooter.blue;
                    this.colorShooter.ir = colorShooter.ir;
                    this.proximityShooter = proxShooter;

                    this.detectedColorShooter = new Color(this.colorShooter.red, this.colorShooter.green, this.colorShooter.blue);
                    this.shooterMatch = ColorSensorConstants.COLOR_MATCH.matchClosestColor(this.detectedColorShooter);
                    this.shooterColorString = detectedColorShooter.toString();

                    if (this.proximityShooter > 150) {
                        this.matchStringShooter = "Note";
                        this.hasNoteShooter = true;
                    } else {
                        this.matchStringShooter = "Surely not a note";
                        hadNoteShooterLastLoop = hasNoteShooter;
                        this.hasNoteShooter = false;
                    }

                }
            } finally {
                threadLock.unlock();
            }
        }

        SerialPortJNI.serialClose(port);
    }

    public PicoColorSensor() {
        readThread = new Thread(this::threadMain);
        readThread.setName("PicoColorSensorThread");
        readThread.start();
    }

    @Log
    public boolean elevatorSensorConnected() {
        try {
            threadLock.lock();
            return hasColorElevator;
        } finally {
            threadLock.unlock();
        }
    }

    @Log
    public boolean shooterSensorConnected() {
        try {
            threadLock.lock();
            return hasColorShooter;
        } finally {
            threadLock.unlock();
        }
    }

    public RawColor getRawColorElevator() {
        try {
            threadLock.lock();
            return new RawColor(colorElevator.red, colorElevator.green, colorElevator.blue, colorElevator.ir);
        } finally {
            threadLock.unlock();
        }
    }

    public void getRawColorElevator(RawColor rawColor) {
        try {
            threadLock.lock();
            rawColor.red = colorElevator.red;
            rawColor.green = colorElevator.green;
            rawColor.blue = colorElevator.blue;
            rawColor.ir = colorElevator.ir;
        } finally {
            threadLock.unlock();
        }
    }

    public int getProximityElevator() {
        try {
            threadLock.lock();
            return proximityElevator;
        } finally {
            threadLock.unlock();
        }
    }

    public boolean hasNoteElevator() {
        try {
            threadLock.lock();
            return this.hasNoteElevator;
        } finally {
            threadLock.unlock();
        }
    }

    private boolean hadNoteElevatorLastLoop = false;
    private boolean hadNoteShooterLastLoop = false;
    public boolean fallingEdgeHasNoteElevator() {
        return hadNoteElevatorLastLoop;
    }

    public boolean fallingEdgeHasNoteShooter() {
        return hadNoteShooterLastLoop;
    }

    public RawColor getRawColorShooter() {
        try {
            threadLock.lock();
            return new RawColor(colorShooter.red, colorShooter.green, colorShooter.blue, colorShooter.ir);
        } finally {
            threadLock.unlock();
        }
    }

    public void getRawColorShooter(RawColor rawColor) {
        try {
            threadLock.lock();
            rawColor.red = colorShooter.red;
            rawColor.green = colorShooter.green;
            rawColor.blue = colorShooter.blue;
            rawColor.ir = colorShooter.ir;
        } finally {
            threadLock.unlock();
        }
    }

    public int getProximityShooter() {
        try {
            threadLock.lock();
            return proximityShooter;
        } finally {
            threadLock.unlock();
        }
    }

    public boolean hasNoteShooter() {
        try {
            threadLock.lock();
            return this.hasNoteShooter;
        } finally {
            threadLock.unlock();
        }
    }

    public double getLastReadTimestampSeconds() {
        try {
            threadLock.lock();
            return lastReadTime;
        } finally {
            threadLock.unlock();
        }
    }

    void setDebugPrints(boolean debug) {
        debugPrints.set(debug);
    }

    @Override
    public void close() throws Exception {
        threadRunning.set(false);
        readThread.join();
    }
}