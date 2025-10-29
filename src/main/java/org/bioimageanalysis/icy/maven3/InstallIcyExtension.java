/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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

package org.bioimageanalysis.icy.maven3;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Mojo(name = "install-icy-extension", defaultPhase = LifecyclePhase.INSTALL)
public class InstallIcyExtension extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "true", required = true)
    Boolean installIcyExtension;

    /**
     * Perform whatever build-process behavior this <code>Mojo</code> implements.<br>
     * This is the main trigger for the <code>Mojo</code> inside the <code>Maven</code> system, and allows
     * the <code>Mojo</code> to communicate errors.
     *
     * @throws MojoExecutionException if an unexpected problem occurs.
     *                                Throwing this exception causes a "BUILD ERROR" message to be displayed.
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (!installIcyExtension)
            return;

        final File jarFile = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-" + project.getVersion() + ".jar");
        if (!jarFile.exists())
            throw new MojoExecutionException("Could not find jar file: " + jarFile.getAbsolutePath());

        final File icyHomeDirectory = new File(System.getProperty("user.home"), ".icy");
        if (!icyHomeDirectory.exists())
            icyHomeDirectory.mkdirs();

        final File extensionsDirectory = new File(icyHomeDirectory, "extensions");
        if (!extensionsDirectory.exists())
            extensionsDirectory.mkdirs();

        final String fullPath = project.getGroupId() + "." + project.getArtifactId();
        final String[] fullPathSplit = fullPath.split("\\.");

        File parent = extensionsDirectory;
        for (final String s : fullPathSplit) {
            final File f = new File(parent, s);
            if (!f.exists())
                f.mkdirs();
            parent = f;
        }

        final File copyJarFile = new File(parent, project.getArtifactId() + ".jar");

        try {
            Files.copy(jarFile.toPath(), copyJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (final Throwable t) {
            throw new MojoExecutionException("Could not copy jar file: " + jarFile.getAbsolutePath(), t);
        }

        final String copyJarPath = fullPath.replace(".", File.separator) + File.separator + project.getArtifactId() + ".jar";

        final File extensionsBinaryFile = new File(extensionsDirectory, "ext.bin");
        if (!extensionsBinaryFile.exists()) {
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(Map.of("path", copyJarPath));
            dumpData(list, extensionsBinaryFile);
        }
        else {
            try (final InputStream is = new FileInputStream(extensionsBinaryFile)) {
                final byte[] readRawData = is.readAllBytes();
                final byte[] readData = Base64.getDecoder().decode(readRawData);
                final StringBuilder sb = new StringBuilder();
                for (final byte readDatum : readData)
                    sb.append((char) readDatum);

                final Yaml yaml = new Yaml();
                final List<Map<String, Object>> list = yaml.load(sb.toString());
                boolean found = false;
                for (final Map<String, Object> map : list) {
                    if (map.get("path").equals(copyJarPath)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    list.add(Map.of("path", copyJarPath));

                    dumpData(list, extensionsBinaryFile);
                }
            }
            catch (final Throwable t) {
                throw new MojoExecutionException("Failed to read extensions binary file", t);
            }
        }
    }

    private void dumpData(final List<Map<String, Object>> list, final File extensionsBinaryFile) throws MojoExecutionException {
        final Yaml yaml = new Yaml();
        final String dump = yaml.dump(list);
        final byte[] data = Base64.getEncoder().encode(dump.getBytes(StandardCharsets.ISO_8859_1));

        try (final OutputStream os = new FileOutputStream(extensionsBinaryFile)) {
            os.write(data);
        }
        catch (final Throwable t) {
            throw new MojoExecutionException("Failed to create extensions binary file", t);
        }
    }
}
