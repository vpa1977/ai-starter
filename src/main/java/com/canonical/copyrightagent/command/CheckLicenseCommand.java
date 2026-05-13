package com.canonical.copyrightagent.command;


import com.canonical.copyrightagent.agent.CopyrightAgent;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class CheckLicenseCommand {

    private final CopyrightAgent agent;

    public CheckLicenseCommand(CopyrightAgent agent) {
        this.agent = agent;
    }

    @Command(name =  "check-license", description = "Check license headers in the specified path")
    public String checkLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories")
            boolean recursive,
            @Option(shortName = 'v', longName = "verbose", description = "Stream full output of every command the agent runs")
            boolean verbose) {

        String request = "Audit license headers under: " + path
                + (recursive ? " (recurse into subdirectories)" : " (top-level only)");
        return agent.run(request, verbose);
    }
}
