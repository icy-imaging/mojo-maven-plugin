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
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Mojo(name = "install-icy-extension", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.TEST)
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

        // Finding Icy home directory
        final File icyHomeDirectory = new File(System.getProperty("user.home"), ".icy");
        if (!icyHomeDirectory.exists())
            icyHomeDirectory.mkdirs();

        // Finding extensions directory
        final File extensionsDirectory = new File(icyHomeDirectory, "extensions");
        if (!extensionsDirectory.exists())
            extensionsDirectory.mkdirs();

        // Writing extensions binary file
        final File extensionsBinaryFile = new File(extensionsDirectory, "ext.bin");
        if (!extensionsBinaryFile.exists()) {
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(
                    Map.of(
                            "groupId", project.getGroupId(),
                            "artifactId", project.getArtifactId(),
                            "version", project.getVersion(),
                            "distribution", project.getDistributionManagement().getRepository().getUrl()
                    )
            );
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
                for (final Map<String, Object> map : list) {
                    if (map.get("groupId").equals(project.getGroupId()) && map.get("artifactId").equals(project.getArtifactId())) {
                        list.remove(map);
                        break;
                    }
                }

                list.add(
                        Map.of(
                                "groupId", project.getGroupId(),
                                "artifactId", project.getArtifactId(),
                                "version", project.getVersion(),
                                "distribution", project.getDistributionManagement().getRepository().getUrl()
                        )
                );

                dumpData(list, extensionsBinaryFile);
            }
            catch (final Throwable t) {
                throw new MojoExecutionException("Failed to read extensions binary file", t);
            }
        }
    }

    /**
     * Write data to extensions binary file
     */
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
