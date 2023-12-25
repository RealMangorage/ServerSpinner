package org.mangorage.serverspinner.core.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class LazyProcess {
    public static LazyProcess create(String ID, String directory, String args, IOutput output, IStatus status) {
        return new LazyProcess(new ProcessBuilder().directory(new File(directory)).command(args.split(" ")), ID, output, status);
    }

    public static final byte[] lineSeperator = System.lineSeparator().getBytes();
    public final Object lock = new Object();
    private final ProcessBuilder processBuilder;
    private final String id;
    private final StringBuilder log = new StringBuilder();
    private final IStatus status;
    private Process runningProcess;
    private boolean stopping = false;
    private IOutput output;

    private LazyProcess(ProcessBuilder builder, String id, IOutput output, IStatus status) {
        this.processBuilder = builder;
        this.id = id;
        this.output = output;
        this.status = status;

        processBuilder.environment().put("java", "echo hello!");
    }

    public Process getRunningProcess() {
        return runningProcess;
    }

    public String getLazyID() {
        return id;
    }

    public void start() {
        synchronized (lock) {
            if (runningProcess != null) return;
            var thread = new Thread(() -> {
                try {
                    log.delete(0, log.length());
                    runningProcess = processBuilder.start();
                    status.running();
                    System.out.println("[LazyProcess] Server %s started".formatted(id));

                    try (var is = new InputStreamReader(runningProcess.getInputStream())) {
                        try (var reader = new BufferedReader(is)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.append(line).append("\n");
                                output.on(line);
                            }
                        }
                    }

                    runningProcess.waitFor();
                    status.stopped();
                    runningProcess = null; // No longer running
                    stopping = false; // Stopped
                    System.out.println("[LazyProcess] Server %s stopped".formatted(id));
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        }
    }

    public void printInput(String s) {
        if (runningProcess == null) return;
        byte[] data = s.getBytes();
        var os = runningProcess.getOutputStream();
        try {
            os.write(data, 0, data.length);
            os.write(lineSeperator, 0, lineSeperator.length);
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLog() {
        return log.toString();
    }

    public void stopProcess() {
        // Various methods to try and stop it
        printInput("exit");
        printInput("stop");
    }

    public void forceStopProcess() {
        if (runningProcess != null && runningProcess.isAlive()) runningProcess.destroyForcibly();
    }

    public String getStatus() {
        if (runningProcess != null) {
            if (stopping) return "Stopping";
            if (runningProcess.isAlive()) return "Running";
        }
        return "stopped";
    }

    public String getInfo() {
        return "ID: " + id + " Status: " + getStatus();
    }
}
