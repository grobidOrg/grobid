/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.jni;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.utilities.GrobidProperties;

public class PythonEnvironmentConfig {

    private Path virtualEnv;
    private Path sitePackagesPath;
    private Path jepPath;
    private boolean active;
    private String pythonVersion;

    public PythonEnvironmentConfig(
            Path virtualEnv,
            Path sitePackagesPath,
            Path jepPath,
            String pythonVersion,
            boolean active) {
        this.virtualEnv = virtualEnv;
        this.sitePackagesPath = sitePackagesPath;
        this.jepPath = jepPath;
        this.active = active;
        this.pythonVersion = pythonVersion;
    }

    public boolean isEmpty() {
        return this.virtualEnv == null;
    }

    public Path getVirtualEnv() {
        return this.virtualEnv;
    }

    public Path getSitePackagesPath() {
        return this.sitePackagesPath;
    }

    public Path getNativeLibPath() {
        if (this.virtualEnv == null) {
            return null;
        }
        return Paths.get(this.virtualEnv.toString(), "lib");
    }

    public Path[] getNativeLibPaths() {
        if (this.virtualEnv == null) {
            return new Path[0];
        }
        return new Path[]{
                this.getNativeLibPath(),
                this.getJepPath()
        };
    }

    public Path getJepPath() {
        return this.jepPath;
    }

    public boolean isActive() {
        return this.active;
    }

    public static PythonEnvironmentConfig getInstanceForVirtualEnv(String virtualEnv, String activeVirtualEnv)
            throws GrobidResourceException {

        if (StringUtils.isEmpty(virtualEnv) && StringUtils.isEmpty(activeVirtualEnv)) {
            return new PythonEnvironmentConfig(null, null, null, null, false);
        }
        if (StringUtils.isEmpty(virtualEnv)) {
            virtualEnv = activeVirtualEnv;
        }

        List<Path> pythons;
        try {
            pythons = Files.find(
                    Paths.get(virtualEnv, "lib"),
                    1,
                    (path, attr) -> (path.toFile().isDirectory()
                            && path.getFileName().toString().contains("python3")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new GrobidResourceException("failed to get python versions from virtual environment", e);
        }

        List<String> pythonVersions = pythons
                .stream()
                .map(
                        path -> FilenameUtils.getName(path.getFileName().toString())
                                .replace("libpython", "")
                                .replace("python", ""))
                .filter(
                        version -> version.contains("3.7") || version.contains("3.8") || version.contains("3.9")
                                || version.contains("3.10") || version.contains("3.11") || version.contains("3.12"))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(pythonVersions)) {
            throw new GrobidException(
                    "Cannot find a suitable version (3.7 to 3.12) of python in your virtual environment: "
                            +
                            virtualEnv);
        }

        Path sitePackagesPath = Paths.get(pythons.get(0).toString(), "site-packages");
        Path jepPath = Paths.get(sitePackagesPath.toString(), "jep");
        return new PythonEnvironmentConfig(
                Paths.get(virtualEnv),
                sitePackagesPath,
                jepPath,
                pythonVersions.get(0),
                StringUtils.equals(virtualEnv, activeVirtualEnv));
    }

    public static String getActiveVirtualEnv() {
        String activeVirtualEnv = System.getenv("VIRTUAL_ENV");
        if (StringUtils.isEmpty(activeVirtualEnv)) {
            activeVirtualEnv = System.getenv("CONDA_PREFIX");
        }
        return activeVirtualEnv;
    }

    public static PythonEnvironmentConfig getInstance() throws GrobidResourceException {
        return getInstanceForVirtualEnv(
                GrobidProperties.getPythonVirtualEnv(),
                getActiveVirtualEnv());
    }

    public String getPythonVersion() {
        return pythonVersion;
    }
}
