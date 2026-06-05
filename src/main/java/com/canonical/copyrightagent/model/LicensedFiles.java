package com.canonical.copyrightagent.model;

import java.util.ArrayList;

public record LicensedFiles(LicenseInfo info, ArrayList<String> files) {
}
