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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Mojo that installs project artifacts as Icy extensions during the Maven build lifecycle.
 * This allows the artifacts to be registered in the Icy platform by creating a configuration
 * file in the Icy home directory.
 * <p>
 * This class is bound to the Maven install phase and requires test-scoped dependency resolution.
 */
@Mojo(name = "install-extension", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.TEST)
public class InstallExtensionMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "true", required = true)
    Boolean installIcyExtension;

    /**
     * Default constructor.
     */
    public InstallExtensionMojo() {
        super();
    }

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
            if (icyHomeDirectory.mkdirs())
                getLog().info("Created icy home directory");

        String distribution = "";
        if (project.getDistributionManagement() != null)
            if (project.getDistributionManagement().getRepository() != null)
                distribution = Objects.requireNonNullElse(project.getDistributionManagement().getRepository().getUrl(), "");

        // Writing extensions binary file
        final File extensionsBinaryFile = new File(icyHomeDirectory, "extensions.yml");
        if (!extensionsBinaryFile.exists()) {
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(
                    Map.of(
                            "groupId", project.getGroupId(),
                            "artifactId", project.getArtifactId(),
                            "version", project.getVersion(),
                            "distribution", distribution
                    )
            );
            dumpData(list, extensionsBinaryFile);
        }
        else {
            try (final FileInputStream is = new FileInputStream(extensionsBinaryFile)) {
                //final byte[] readRawData = is.readAllBytes();
                //final byte[] readData = Base64.getDecoder().decode(readRawData);
                //final StringBuilder sb = new StringBuilder();
                //for (final byte readDatum : readData)
                //sb.append((char) readDatum);

                final Yaml yaml = new Yaml();
                //final List<Map<String, Object>> list = yaml.load(sb.toString());
                final Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                final List<Map<String, Object>> list = yaml.load(reader);
                reader.close();
                for (final Map<String, Object> map : list) {
                    if (map.get("groupId").equals(project.getGroupId()) && map.get("artifactId").equals(project.getArtifactId())) {
                        getLog().info("Artefact already registered in Icy, replaced it");
                        list.remove(map);
                        break;
                    }
                }

                list.add(
                        Map.of(
                                "groupId", project.getGroupId(),
                                "artifactId", project.getArtifactId(),
                                "version", project.getVersion(),
                                "distribution", distribution
                        )
                );

                dumpData(list, extensionsBinaryFile);
            }
            catch (final Throwable t) {
                throw new MojoExecutionException("Failed to read extensions file", t);
            }
        }
    }

    /**
     * Write data to extensions binary file
     */
    private void dumpData(final List<Map<String, Object>> list, final File extensionsFile) throws MojoExecutionException {
        final Yaml yaml = new Yaml();
        //final String dump = yaml.dump(list);
        //final byte[] data = Base64.getEncoder().encode(dump.getBytes(StandardCharsets.ISO_8859_1));

        try (final FileOutputStream os = new FileOutputStream(extensionsFile)) {
            //os.write(data);
            final Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            yaml.dump(list, writer);
            writer.flush();
            writer.close();
            getLog().info("Extension file written successfully");
        }
        catch (final Throwable t) {
            throw new MojoExecutionException("Failed to create extensions file", t);
        }
    }
}
