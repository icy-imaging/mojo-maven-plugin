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

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

/**
 * @author Thomas Musset
 * @version 1.0.0-a.5
 */
@Mojo(name = "generate-extension-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateExtensionPropertiesMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/extension.yaml", required = true, readonly = true)
    File outputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
                throw new MojoFailureException("Unable to create output directory");

        final Yaml yaml = new Yaml();
        try (final FileWriter fw = new FileWriter(outputFile)) {
            final Map<String, Object> data = new HashMap<>();
            data.put("artifactId", project.getArtifactId());
            data.put("groupId", project.getGroupId());
            data.put("version", project.getVersion());
            data.put("name", project.getName());
            data.put("description", project.getDescription());
            data.put("organization", project.getOrganization().getName());
            data.put("organizationUrl", project.getOrganization().getUrl());
            data.put("url", project.getUrl());
            data.put("scm", project.getScm().getUrl());

            data.put("kernelVersion", project.getProperties().getProperty("icy.version"));

            final List<Map<String, Object>> developersData = new ArrayList<>();
            for (final Developer developer : project.getDevelopers()) {
                final Map<String, Object> developerData = new HashMap<>();
                developerData.put("id", developer.getId());
                developerData.put("name", developer.getName());
                developerData.put("email", developer.getEmail());
                developerData.put("url", developer.getUrl());
                developerData.put("organization", developer.getOrganization());
                developerData.put("organizationUrl", developer.getOrganizationUrl());
                developerData.put("timezone", developer.getTimezone());
                developerData.put("roles", developer.getRoles());

                developersData.add(developerData);
            }
            data.put("developers", developersData);

            final List<Map<String, Object>> contributorsData = new ArrayList<>();
            for (final Contributor contributor : project.getContributors()) {
                final Map<String, Object> contributorData = new HashMap<>();
                contributorData.put("name", contributor.getName());
                contributorData.put("email", contributor.getEmail());
                contributorData.put("url", contributor.getUrl());
                contributorData.put("organization", contributor.getOrganization());
                contributorData.put("organizationUrl", contributor.getOrganizationUrl());
                contributorData.put("timezone", contributor.getTimezone());

                contributorsData.add(contributorData);
            }
            data.put("contributors", contributorsData);

            if (project.getDistributionManagement() != null) {
                data.put("downloadUrl", Objects.requireNonNullElse(project.getDistributionManagement().getDownloadUrl(), ""));
                data.put("repository", Objects.requireNonNullElse(project.getDistributionManagement().getRepository().getName(), ""));
                data.put("repositoryUrl", Objects.requireNonNullElse(project.getDistributionManagement().getRepository().getUrl(), ""));
            }

            yaml.dump(data, fw);
            getLog().info("Generated extension properties successfully");
        }
        catch (final Throwable t) {
            throw new MojoExecutionException("Unable to generate extension properties", t);
        }
    }
}
