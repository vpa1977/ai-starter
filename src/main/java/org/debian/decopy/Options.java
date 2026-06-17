package org.debian.decopy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Command-line options for decopy.
 */
public class Options {

    private static final Logger LOG = Logger.getLogger(Options.class.getName());

    // --- defaults ---
    public static final String DEFAULT_EXCLUDE_FILE_REGEX =
            "~$" +
            "|\\A\\.#.*$" +
            "|\\..*\\.swp$" +
            "|\\A,," +
            "|\\A(?:DEADJOE|\\.cvsignore|\\.arch-inventory|\\.bzrignore|\\.gitignore)$" +
            "|\\A(?:CVS|RCS|\\.deps|\\{arch\\}|\\.arch-ids|\\.svn|\\.hg|_darcs|\\.git|" +
            "\\.shelf|_MTN|\\.bzr(?:\\.backup|tags)?)$";

    public static final String DEFAULT_EXCLUDE_DIRECTORY_REGEX =
            "\\A,," +
            "|\\A(?:CVS|RCS|\\.deps|\\{arch\\}|\\.arch-ids|\\.svn|\\.hg|_darcs|\\.git|" +
            "\\.pc|\\.shelf|_MTN|\\.bzr(?:\\.backup|tags)?)$" +
            "|\\A__pycache__$";

    public static final String DEFAULT_EXCLUDE_SPECIAL_REGEX =
            "\\Adebian/(?:copyright|changelog)$";

    public static final String DEFAULT_EXCLUDE_FULLNAME_REGEX = "\\A$";

    // --- fields ---
    public String exclude = DEFAULT_EXCLUDE_FULLNAME_REGEX;
    public String mode = "full";
    public String copyrightFile = "debian/copyright";
    public boolean debug = false;
    public boolean verbose = false;
    public boolean quiet = false;
    public boolean text = false;
    public String groupBy = "license";
    public boolean splitOnLicense = true;
    public boolean splitDebian = true;
    public boolean glob = true;
    public boolean progress = true;
    public int jobs = 0;
    public String output = "";
    public String root = ".";
    public List<String> files = new ArrayList<>();

    // compiled patterns
    public Pattern excludeFullnameRe;
    public Pattern excludeSpecialRe;
    public Pattern excludeFileRe;
    public Pattern excludeDirectoryRe;

    public Options(String exclude, String mode, String copyrightFile, boolean debug, boolean verbose,
            boolean quiet, boolean text, String groupBy, boolean noSplitOnLicense, boolean noSplitDebian,
            boolean noGlob, boolean noProgress, int jobs, String output,
            String root, List<String> files) {
        if (exclude != null) {
            this.exclude = exclude;
        }
        this.mode = mode;
        this.copyrightFile = copyrightFile;
        this.debug = debug;
        this.verbose = verbose;
        this.quiet = quiet;
        this.text = text;
        this.groupBy = groupBy;
        this.splitOnLicense =  !noSplitOnLicense;
        this.splitDebian = !noSplitDebian;
        this.glob = !noGlob;
        this.progress = !noProgress;
        this.jobs = jobs;
        this.output = output;
        this.root = root;
        this.files = files;

        Level logLevel;
        if (this.debug) {
            logLevel = Level.FINE;
        } else if (this.verbose) {
            logLevel = Level.INFO;
        } else if (this.quiet) {
            logLevel = Level.SEVERE;
        } else {
            logLevel = Level.WARNING;
        }
        configureLogging(logLevel);

        // Compile patterns
        this.excludeFullnameRe = Pattern.compile(this.exclude,
                Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
        this.excludeSpecialRe = Pattern.compile(DEFAULT_EXCLUDE_SPECIAL_REGEX,
                Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
        this.excludeFileRe = Pattern.compile(DEFAULT_EXCLUDE_FILE_REGEX,
                Pattern.COMMENTS | Pattern.MULTILINE);
        this.excludeDirectoryRe = Pattern.compile(DEFAULT_EXCLUDE_DIRECTORY_REGEX,
                Pattern.COMMENTS | Pattern.MULTILINE);

        if ("-".equals(this.output)) {
            this.output = "";
        }

        // Normalize root
        this.root = Path.of(this.root).toAbsolutePath().normalize().toString();

        // Process positional files
        for (String f : files) {
            Path p = Path.of(f);
            if (!p.isAbsolute()) {
                p = Path.of(this.root).resolve(p);
            }
            p = p.normalize();
            // Make relative to root
            try {
                Path rootPath = Path.of(this.root);
                if (p.startsWith(rootPath)) {
                    this.files.add(rootPath.relativize(p).toString());
                } else {
                    this.files.add(f);
                }
            } catch (Exception e) {
                this.files.add(f);
            }
        }
    }

    private static void configureLogging(Level level) {
        Logger root = Logger.getLogger("");
        root.setLevel(level);
        for (var handler : root.getHandlers()) {
            handler.setLevel(level);
        }
    }
}
