package com.canonical.copyrightagent.command;

import org.debian.decopy.Decopy;
import org.debian.decopy.Options;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DecopyCommand {

    @Command(name = "decopy", description = "Prepare d/copyright file")
    public void decopy(
            @Option(shortName = 'X', longName = "exclude", description = "Exclude files/dirs matching full path pattern")
            String exclude,
            @Option(longName = "mode", description = "Processing mode: full or partial", defaultValue = "full")
            String mode,
            @Option(longName = "copyright-file", description = "Path to debian/copyright", defaultValue = "debian/copyright")
            String copyrightFile,
            @Option(longName = "debug", description = "Enable debug output")
            boolean debug,
            @Option(shortName = 'v', longName = "verbose", description = "Enable verbose output")
            boolean verbose,
            @Option(shortName = 'q', longName = "quiet", description = "Suppress non-error output")
            boolean quiet,
            @Option(shortName = 'a', longName = "text", description = "Treat all files as text")
            boolean text,
            @Option(longName = "group-by", description = "Group by: license or copyright", defaultValue = "license")
            String groupBy,
            @Option(longName = "no-split-on-license", description = "Do not split groups on subdirs with licenses")
            boolean noSplitOnLicense,
            @Option(longName = "no-split-debian", description = "Do not split debian/* paragraph")
            boolean noSplitDebian,
            @Option(longName = "no-glob", description = "Do not use glob patterns in output")
            boolean noGlob,
            @Option(longName = "no-progress", description = "Do not show progress bar")
            boolean noProgress,
            @Option(shortName = 'j', longName = "jobs", description = "Parallel jobs (0=auto)", defaultValue = "0")
            int jobs,
            @Option(shortName = 'o', longName = "output", description = "Output file (default: stdout)", defaultValue = "")
            String output,
            @Option(longName = "root", description = "Root directory to scan", defaultValue = ".")
            String root,
            @Option(longName = "files", description = "Files to process")
            List<String> files
    ) {
        // capture options into Options object
        Options opts = new Options(
        exclude, mode, copyrightFile, debug, verbose, quiet, text, groupBy,
         noSplitOnLicense, noSplitDebian, noGlob, noProgress, jobs, output, root, files);
        Decopy.run(opts);
    }


}
