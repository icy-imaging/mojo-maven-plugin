/*
 * Copyright (c) 2010-2026. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.icy.maven.plugin.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

@Deprecated(forRemoval = true)
@Mojo(name = "include-extension-natives", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class IncludeExtensionNativesMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    private static final String[] ARCHITECTURES =  new String[] {
            "x86_64", "arm64"
    };

    private static final String[] OS = new String[] {
            "windows", "macos", "debian", "ubuntu", "archlinux"
    };

    /**
     * Perform whatever build-process behavior this <code>Mojo</code> implements.<br>
     * This is the main trigger for the <code>Mojo</code> inside the <code>Maven</code> system, and allows
     * the <code>Mojo</code> to communicate errors.
     *
     */
    @Override
    public void execute() {
        final File libFolder = new File(project.getBasedir(), "natives");
        if (!libFolder.exists() || !libFolder.isDirectory()) {
            getLog().info("Natives directory not found, skipping.");
            return;
        }

        for (final String architecture : ARCHITECTURES) {
            final File archDir = new File(libFolder, architecture);
            if (!archDir.exists() || !archDir.isDirectory()) {
                getLog().info("Natives for " + architecture + " not found, skipping.");
                continue;
            }
            getLog().info("Searching natives for architecture " + architecture + "...");
            for (final String os : OS) {
                final File osDir = new File(archDir, os);
                if (!osDir.exists() || !osDir.isDirectory()) {
                    getLog().info("Natives for " + os + "_" + architecture + " not found, skipping.");
                    continue;
                }

                final ArrayList<File> natives;
                if (os.equalsIgnoreCase("windows")) {
                    natives = getNatives(osDir, new WindowsNativesFilter());
                }
                else if (os.equalsIgnoreCase("macos")) {
                    natives = getNatives(osDir, new MacosNativesFilter());
                }
                else {
                    natives = getNatives(osDir, new LinuxNativesFilter());
                }

                final File destDir = new File(project.getBuild().getOutputDirectory() + File.separator + "natives" + File.separator + architecture + File.separator + os + "/");
                if (!destDir.exists())
                    destDir.mkdirs();
                for (final File nativeFile : natives) {
                    try {
                        Files.copy(nativeFile.toPath(), new File(destDir, nativeFile.getName()).toPath());
                        getLog().info("Copied native file " + nativeFile.getName());
                    }
                    catch (final IOException e) {
                        getLog().error("Failed to copy native file " + nativeFile.getName(), e);
                    }
                }
            }
        }
    }

    private @NotNull ArrayList<File> getNatives(final @NotNull File directory, final @NotNull FileFilter filter) {
        final ArrayList<File> result = new ArrayList<>();
        final File[] files = directory.listFiles(filter);
        if (files == null) {
            return result;
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                result.addAll(getNatives(file, filter));
            }
            else {
                result.add(file);
            }
        }

        return result;
    }

    private static class WindowsNativesFilter implements FileFilter {
        @Override
        public boolean accept(final @NotNull File pathname) {
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().endsWith(".dll"));
        }
    }

    private static class LinuxNativesFilter implements FileFilter {
        @Override
        public boolean accept(final @NotNull File pathname) {
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().endsWith(".so"));
        }
    }

    private static class MacosNativesFilter implements FileFilter {
        @Override
        public boolean accept(final @NotNull File pathname) {
            return pathname.isDirectory() || (pathname.isFile() && (pathname.getName().endsWith(".dylib") || pathname.getName().startsWith(".jnilib")));
        }
    }
}
