/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.MIDPOINT_HOME_PROPERTY;
import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.MIDPOINT_SILENT_PROPERTY;

public class ApplicationHomeSetup {

    private static final Trace LOGGER = TraceManager.getTrace(ApplicationHomeSetup.class);

    private boolean silent = false;

    public void init() {
        this.silent = Boolean.getBoolean(MIDPOINT_SILENT_PROPERTY);

        String midpointHomePath = System.getProperty(MIDPOINT_HOME_PROPERTY);

        String homeMessage = MIDPOINT_HOME_PROPERTY + " = " + midpointHomePath;
        LOGGER.info(homeMessage);
        printToSysout(homeMessage);

        createMidpointHomeDirectories(midpointHomePath);
        setupMidpointHomeDirectory(midpointHomePath);
    }

    private void printToSysout(String message) {
        if (!silent) {
            System.out.println(message);
        }
    }

    /**
     * Creates directory structure under root
     * <p/>
     * Directory information based on: http://wiki.evolveum.com/display/midPoint/midpoint.home+-+directory+structure
     */
    private void createMidpointHomeDirectories(String midpointHomePath) {
        if (!checkDirectoryExistence(midpointHomePath)) {
            createDir(midpointHomePath);
        }

        if (!midpointHomePath.endsWith("/")) {
            midpointHomePath = midpointHomePath + "/";
        }
        String[] directories = {
                midpointHomePath + "icf-connectors",
                midpointHomePath + "idm-legacy",
                midpointHomePath + "log",
                midpointHomePath + "schema",
                midpointHomePath + "import",
                midpointHomePath + "export",
                midpointHomePath + "tmp",
                midpointHomePath + "lib",
                midpointHomePath + "trace"
        };

        for (String directory : directories) {
            if (checkDirectoryExistence(directory)) {
                continue;
            }
            LOGGER.warn("Missing midPoint home directory '{}'. Creating.", directory);
            createDir(directory);
        }
    }

    private void setupMidpointHomeDirectory(String midpointHomePath) {
        try {
            ClassPathUtil.extractFilesFromClassPath("initial-midpoint-home", midpointHomePath, false);
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Error copying the content of initial-midpoint-home to {}: {}", midpointHomePath, e.getMessage(), e);
        }

    }

    private boolean checkDirectoryExistence(String dir) {
        File d = new File(dir);
        if (d.isFile()) {
            LOGGER.error(dir + " is file and NOT a directory.");
            throw new SystemException(dir + " is file and NOT a directory !!!");
        }

        if (d.isDirectory()) {
            LOGGER.info("Directory " + dir + " already exists. Reusing it.");
            return true;
        } else {
            return false;
        }

    }

    private void createDir(String dir) {
        File d = new File(dir);
        if (!d.exists() || !d.isDirectory()) {
            boolean created = d.mkdirs();
            if (!created) {
                LOGGER.error("Unable to create directory " + dir + " as user " + System.getProperty("user.name"));
            }
        }
    }
}
