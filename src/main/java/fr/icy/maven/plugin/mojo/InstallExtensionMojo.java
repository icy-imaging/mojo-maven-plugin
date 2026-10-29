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

import fr.icy.maven.plugin.mojo.lang.Extension;
import org.apache.maven.model.DeploymentRepository;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Mojo that installs project artifacts as Icy extensions during the Maven build lifecycle.
 * This allows the artifacts to be registered in the Icy platform by creating a configuration
 * file in the Icy home directory.
 * <p>
 * This class is bound to the Maven install phase and requires test-scoped dependency resolution.
 */
@Mojo(name = "install-extension", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class InstallExtensionMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "true", required = true)
    Boolean installIcyExtension;

    private static final String KEY_VERSION = "version";
    private static final String KEY_EXTENSIONS = "extensions";

    private static final Integer CURRENT_VERSION = 0;

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

        DeploymentRepository distribution = null;
        final boolean isSnapshot = project.getVersion().endsWith("-SNAPSHOT");
        if (project.getDistributionManagement() != null) {
            if (!isSnapshot && project.getDistributionManagement().getRepository() != null)
                distribution = project.getDistributionManagement().getRepository();
            else if (isSnapshot && project.getDistributionManagement().getSnapshotRepository() != null)
                distribution = project.getDistributionManagement().getSnapshotRepository();
        }

        final Extension extension = new Extension(
                project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                Extension.DistributionToMap(distribution),
                Boolean.TRUE
        );

        // Writing extension yaml file
        final File extensionsFile = new File(icyHomeDirectory, "extensions.yaml");
        final Map<String, Object> content;
        if (!extensionsFile.exists())
            content = new HashMap<>();
        else {
            Map<String, Object> temp;
            try (final FileInputStream is = new FileInputStream(extensionsFile)) {
                final Yaml yaml = new Yaml();
                final Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                temp = yaml.loadAs(reader, Map.class);
                reader.close();
            }
            catch (final Throwable t) {
                //throw new MojoExecutionException("Failed to read extension file", t);
                getLog().warn("Invalid extensions.yaml file, cleared it…", t);
                temp = new HashMap<>();
            }
            content = temp;
        }

        final Integer version = (Integer) content.getOrDefault(KEY_VERSION, -1);
        if (version < 0 || version > CURRENT_VERSION) {
            content.clear();
            content.put(KEY_VERSION, CURRENT_VERSION);
            content.put(KEY_EXTENSIONS, Map.of(0L, extension.asMap()));
        }
        else {
            if (version < CURRENT_VERSION)
                upgradeYaml(content);

            @SuppressWarnings("unchecked")
            final Map<Integer, Object> extensions = (Map<Integer, Object>) content.getOrDefault(KEY_EXTENSIONS, new HashMap<>());

            boolean found = false;
            for (final Integer index : extensions.keySet()) {
                @SuppressWarnings("unchecked")
                final Extension ext = Extension.fromMap((Map<String, Object>) extensions.get(index));
                if (ext.getGroupId().equals(project.getGroupId()) && ext.getArtifactId().equals(project.getArtifactId())) {
                    getLog().info("Artefact already registered in Icy, replaced it");
                    extensions.put(index, extension.asMap());
                    found = true;
                    break;
                }
            }

            if (!found)
                extensions.put(extensions.size() + 1, extension.asMap());

            content.put(KEY_EXTENSIONS, extensions);
        }

        dumpData(content, extensionsFile);
    }

    /**
     * Write data to the extension YAML file
     */
    private void dumpData(final Map<String, Object> content, final File extensionsFile) throws MojoExecutionException {
        final Yaml yaml = new Yaml();

        try (final FileOutputStream os = new FileOutputStream(extensionsFile)) {
            final Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            yaml.dump(content, writer);
            writer.flush();
            writer.close();
            getLog().info("Extension file written successfully");
        }
        catch (final Throwable t) {
            throw new MojoExecutionException("Failed to write the extensions file", t);
        }
    }

    private void upgradeYaml(final Map<String, Object> content) {
        // TODO: retro-compatibility
    }
}
