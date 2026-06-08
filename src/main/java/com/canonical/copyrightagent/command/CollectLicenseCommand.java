package com.canonical.copyrightagent.command;

import com.canonical.copyrightagent.model.LicenseInfo;
import com.canonical.copyrightagent.model.LicensedFiles;
import com.canonical.copyrightagent.service.CommentExtractor;
import com.canonical.copyrightagent.service.LicenseExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CollectLicenseCommand {

    @Autowired
    private LicenseExtractor licenseExtractor;
    @Autowired
    private CommentExtractor commentExtractor;

    private final int MAX_LICENSE_LENGTH = 32 * 1024;

    public CollectLicenseCommand() {
    }

    @Command(name = "collect-license", description = "Collect license headers from files in the specified path")
    public String collectLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories", defaultValue = "true")
            boolean recursive) {

        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = recursive ? Files.walk(Path.of(path)) : Files.list(Path.of(path))) {
            paths.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            return "Error walking path '" + path + "': " + e.getMessage();
        }

        if (files.isEmpty()) {
            return "No files found in: " + path;
        }

        Map<String, LicensedFiles> licenseMap = new LinkedHashMap<>();
        List<String> noLicenseFiles = new ArrayList<>();

        var total = scanFiles(files, noLicenseFiles, licenseMap);

        StringBuilder summary = new StringBuilder();
        summary.append("License scan results for: ").append(path).append("\n");
        summary.append("Files scanned: ").append(total).append("\n");
        summary.append("Distinct licenses: ").append(licenseMap.size()).append("\n");
        summary.append("Files without license: ").append(noLicenseFiles.size()).append("\n\n");

        List<String> fullLicenseFiles = new ArrayList<>();
        int licenseIdx = 1;
        for (Map.Entry<String, LicensedFiles> entry : licenseMap.entrySet()) {
            LicensedFiles lf = entry.getValue();
            summary.append("--- License #").append(licenseIdx++).append(" (").append(lf.files().size()).append(" files) ---\n");
            summary.append(lf.info().text()).append("\n");
            if (lf.info().holders() != null && !lf.info().holders().isBlank()) {
                summary.append("Holders: ").append(lf.info().holders()).append("\n");
            }
            if (lf.info().years() != null && !lf.info().years().isBlank()) {
                summary.append("Years: ").append(lf.info().years()).append("\n");
            }
            summary.append("Files:\n");
            for (String f : lf.files()) {
                summary.append("  ").append(f).append("\n");
            }
            summary.append("\n");
            if (lf.info().full()) {
                fullLicenseFiles.addAll(lf.files());
            }
        }

        if (!fullLicenseFiles.isEmpty()) {
            summary.append("--- Full License Files (").append(fullLicenseFiles.size()).append(" files) ---\n");
            for (String f : fullLicenseFiles) {
                summary.append("  ").append(f).append("\n");
            }
            summary.append("\n");
        }

        if (!noLicenseFiles.isEmpty()) {
            summary.append("--- No License (").append(noLicenseFiles.size()).append(" files) ---\n");
            for (String f : noLicenseFiles) {
                summary.append("  ").append(f).append("\n");
            }
        }

        return summary.toString();
    }

    private int scanFiles(List<Path> files, List<String> noLicenseFiles, Map<String, LicensedFiles> licenseMap) {
        var total = files.size();
        while (!files.isEmpty()) {
            var filePath = files.remove(0);
            System.err.println("Processing: " + filePath);
            String content;
            try {
                content = Files.readString(filePath);
            } catch (IOException e) {
                System.err.println("Cannot read file " + filePath + ": " + e.getMessage());
                noLicenseFiles.add(filePath.toString());
                continue;
            }
            if (!content.toLowerCase().contains("copyright")) {
                // TODO: better guard condition, borrow it from decopy
                noLicenseFiles.add(filePath.toString());
                continue;
            }

            var exitingLicense = isExistingLicense(content, licenseMap);
            if (exitingLicense != null) {
                licenseMap.get(exitingLicense).files().add(filePath.toString());
                continue;
            }

            LicenseInfo licenseInfo = extractLicense(content);
            String licenseText = licenseInfo.text();

            if (licenseText == null || licenseText.isBlank()) {
                noLicenseFiles.add(filePath.toString());
            } else {
                String normalizedKey = normalizeLicense(licenseText);
                licenseMap.computeIfAbsent(normalizedKey, k -> new LicensedFiles(licenseInfo, new ArrayList<>())).files().add(filePath.toString());
            }
        }
        return total;
    }

    public LicenseInfo extractLicense(String fileContent) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> comments = commentExtractor.extractComments(fileContent);
        if (comments.isEmpty()) {
            sb.append(fileContent, 0, MAX_LICENSE_LENGTH);
        } else {
            for (var c : comments) {
                sb.append(c);
                sb.append("\n");
            }
        }
        return licenseExtractor.extractLicense(sb.toString(), fileContent.length());
    }

    private String normalizeLicense(String licenseText) {
        return licenseText.replaceAll("[^a-zA-Z0-9]", "");
    }

    private String isExistingLicense(String content, Map<String, LicensedFiles> licenseMap) {
        String normalizedContent = normalizeLicense(content);
        for (String key : licenseMap.keySet()) {
            if (normalizedContent.contains(key)) {
                return key;
            }
        }
        return null;
    }


}
