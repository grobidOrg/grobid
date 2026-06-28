package org.grobid.core.jni;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class WapitiWrapperTest {

    @Test
    public void testCreateSafeSymlink_noSpaceInName() throws IOException {
        Path tempDir = Files.createTempDirectory("wapiti-test");
        Path target = Files.createTempFile(tempDir, "model", ".bin");
        try {
            Path tempLinkDir = Files.createTempDirectory("wapiti-link");
            try {
                Path link = WapitiWrapper.createSafeSymlink(target, tempLinkDir);
                assertTrue("Link should exist", Files.exists(link));
                assertFalse("Link name should not contain spaces", link.getFileName().toString().contains(" "));
                assertThat(Files.readSymbolicLink(link), is(target.toAbsolutePath()));
            } finally {
                WapitiWrapper.deleteTempDir(tempLinkDir);
            }
        } finally {
            WapitiWrapper.deleteTempDir(tempDir);
        }
    }

    @Test
    public void testCreateSafeSymlink_spaceInName() throws IOException {
        Path tempDir = Files.createTempDirectory("wapiti-test");
        Path target = tempDir.resolve("my model.bin");
        Files.createFile(target);
        try {
            Path tempLinkDir = Files.createTempDirectory("wapiti-link");
            try {
                Path link = WapitiWrapper.createSafeSymlink(target, tempLinkDir);
                assertTrue("Link should exist", Files.exists(link));
                assertFalse("Link name should not contain spaces",
                        link.getFileName().toString().contains(" "));
                assertThat(Files.readSymbolicLink(link), is(target.toAbsolutePath()));
            } finally {
                WapitiWrapper.deleteTempDir(tempLinkDir);
            }
        } finally {
            WapitiWrapper.deleteTempDir(tempDir);
        }
    }

    @Test
    public void testCreateSafeSymlink_nameCollision() throws IOException {
        Path tempDir = Files.createTempDirectory("wapiti-test");
        Path target1 = tempDir.resolve("my model.bin");
        Path target2 = tempDir.resolve("my_model.bin"); // will collide after space replacement
        Files.createFile(target1);
        Files.createFile(target2);
        try {
            Path tempLinkDir = Files.createTempDirectory("wapiti-link");
            try {
                Path link1 = WapitiWrapper.createSafeSymlink(target1, tempLinkDir);
                Path link2 = WapitiWrapper.createSafeSymlink(target2, tempLinkDir);
                assertTrue("First link should exist", Files.exists(link1));
                assertTrue("Second link should exist", Files.exists(link2));
                assertFalse("Link names should be different", link1.equals(link2));
                assertFalse("Link names should not contain spaces",
                        link1.getFileName().toString().contains(" ")
                                || link2.getFileName().toString().contains(" "));
            } finally {
                WapitiWrapper.deleteTempDir(tempLinkDir);
            }
        } finally {
            WapitiWrapper.deleteTempDir(tempDir);
        }
    }

    @Test
    public void testDeleteTempDir_nullSafe() {
        // should not throw
        WapitiWrapper.deleteTempDir(null);
    }

    @Test
    public void testDeleteTempDir_cleansUp() throws IOException {
        Path tempDir = Files.createTempDirectory("wapiti-delete-test");
        Files.createTempFile(tempDir, "file", ".tmp");
        assertTrue("Temp dir should exist before deletion", Files.exists(tempDir));
        WapitiWrapper.deleteTempDir(tempDir);
        assertFalse("Temp dir should be gone after deletion", Files.exists(tempDir));
    }
}
