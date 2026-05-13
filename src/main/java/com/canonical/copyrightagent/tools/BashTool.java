package com.canonical.copyrightagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BashTool {

    private static final int MAX_OUTPUT_BYTES = 8 * 1024;
    private static final long TIMEOUT_SECONDS = 6000;

    private volatile boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Tool(description = """
            Run a command in a bash subshell on the host and return its combined
            stdout + stderr. Use absolute paths. Each output line is streamed to
            the console as it is produced. The returned string is truncated to
            ~8 KB. The call is killed after 6000 seconds, so slow tools such as
            `decopy` and `licensecheck` on large trees will run to completion.
            """)
    public String bash(
            @ToolParam(description = "The shell command to execute, e.g. 'ls -la /tmp'") String command) {
        System.out.println("[agent] $ " + command);
        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
                    .redirectErrorStream(true);
            // Force line-buffering for Python child processes (e.g. decopy) so
            // their progress / verbose lines stream live through the pipe
            // instead of being block-buffered until the process exits.
            pb.environment().put("PYTHONUNBUFFERED", "1");
            process = pb.start();
        } catch (IOException e) {
            String msg = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            System.out.println("[agent] " + msg);
            return msg;
        }

        StringBuilder collected = new StringBuilder();
        AtomicBoolean truncated = new AtomicBoolean(false);

        boolean echoLines = this.verbose;
        Thread reader = Thread.ofVirtual().name("bash-reader").start(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (echoLines) {
                        System.out.println("[agent] | " + line);
                    }
                    synchronized (collected) {
                        int needed = line.length() + 1;
                        if (collected.length() + needed <= MAX_OUTPUT_BYTES) {
                            collected.append(line).append('\n');
                        } else {
                            truncated.set(true);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        });

        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return "ERROR: interrupted";
        }

        if (!finished) {
            process.destroyForcibly();
            joinQuietly(reader);
            String msg = "ERROR: command timed out after " + TIMEOUT_SECONDS + "s";
            System.out.println("[agent] " + msg);
            return msg;
        }

        joinQuietly(reader);
        int exit = process.exitValue();
        String body;
        int bytes;
        synchronized (collected) {
            body = collected.toString();
            bytes = collected.length();
        }
        System.out.println("[agent] exit=" + exit + " (" + bytes + " bytes)");
        String suffix = truncated.get() ? "\n[...truncated...]" : "";
        return "exit=" + exit + "\n" + body + suffix;
    }

    private static void joinQuietly(Thread t) {
        try {
            t.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
