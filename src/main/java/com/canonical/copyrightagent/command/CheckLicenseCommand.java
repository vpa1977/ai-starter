package com.canonical.copyrightagent.command;


import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class CheckLicenseCommand {

    @Command(name =  "check-license", description = "Check license headers in the specified path")
    public String checkLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories")
            boolean recursive) {

        // TODO: implement license checking logic
        return "Checking license for path: " + path + (recursive ? " (recursive)" : "");
    }
}
