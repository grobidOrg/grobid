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
package org.grobid.core.utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PathUtil.class);

    public static File getOneFile(File root, final String ext) {
        File[] l = root.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(ext);
            }
        });
        if (l == null || l.length == 0) {
            throw new IllegalArgumentException("Cannot find files in " + root + " with extension " + ext);
        }
        return l[0];
    }

    public static List<Path> getAllPaths(Path root, final String... extensions) {
        List<Path> l = new ArrayList<>();
        getAllPaths(l, root, extensions);
        return l;
    }

    public static void getAllPaths(final List<Path> paths, Path root, final String... extensions) {
        try {
            Files.walkFileTree(root.toAbsolutePath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isRegularFile(file) && isSupportedFile(file, extensions)) {
                        paths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                private boolean isSupportedFile(Path file, String[] extensions) {
                    for (String ext : extensions) {
                        if (file.toString().toLowerCase().endsWith("." + ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        } catch (IOException e) {
            //            return;
        }
    }

}
