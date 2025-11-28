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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Generate a dependencies.yaml file inside the META-INF folder that lists provided (and specified) dependency.
 *
 * @author Thomas Musset
 * @version 1.0.0-a.5
 * @deprecated Not useful anymore because Icy implement maven resolver to search automatically for dependencies
 */
@Deprecated(forRemoval = true)
@Mojo(name = "generate-dependencies-properties", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class GenerateDependenciesProperties extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/dependencies.yaml", required = true, readonly = true)
    File outputFile;

    @Parameter
    String[] excludeGroupIds;

    @Parameter
    String[] excludeArtifactIds;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs())
            throw new MojoFailureException("Cannot create output directory");

        final Map<String, List<Map<String, Object>>> pds = getDependencies();
        final Yaml yaml = new Yaml();
        try (final FileWriter writer = new FileWriter(outputFile)) {
            yaml.dump(pds, writer);
        }
        catch (final IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private @NotNull @Unmodifiable Map<String, List<Map<String, Object>>> getDependencies() {
        final List<Dependency> dependencies = project.getDependencies();
        final Set<Artifact> artifacts = project.getArtifacts();

        final List<Map<String, Object>> artifactsResolved = new ArrayList<>();
        for (final Artifact artifact : artifacts) {
            if (!artifact.getScope().equals("compile"))
                continue;
            if (artifact.getGroupId().startsWith("org.bioimageanalysis"))
                continue;
            final Map<String, Object> data = new HashMap<>(4);
            data.put("groupId", artifact.getGroupId());
            data.put("artifactId", artifact.getArtifactId());
            data.put("version", artifact.getVersion());

            if (artifact.getRepository() != null) {
                final Map<String, String> repoData = new HashMap<>();
                repoData.put("id", artifact.getRepository().getId());
                repoData.put("url", artifact.getRepository().getUrl());
                repoData.put("protocol", artifact.getRepository().getProtocol());
                data.put("repository", repoData);
            }

            artifactsResolved.add(data);
        }

        final List<Map<String, Object>> dependenciesResolved = new ArrayList<>();
        for (final Dependency dependency : dependencies) {
            if (resolveArtifactId(dependency) && resolveGroupId(dependency) && resolveScope(dependency)) {
                final Map<String, Object> data = new HashMap<>();
                data.put("groupId", dependency.getGroupId());
                data.put("artifactId", dependency.getArtifactId());
                data.put("version", dependency.getVersion());

                dependenciesResolved.add(data);
            }
        }

        return Map.of("external", artifactsResolved, "icy", dependenciesResolved);
    }

    private boolean resolveGroupId(final @NotNull Dependency dependency) {
        if (excludeGroupIds == null || excludeGroupIds.length == 0)
            return true;

        return !Arrays.asList(excludeGroupIds).contains(dependency.getGroupId());
    }

    private boolean resolveArtifactId(final @NotNull Dependency dependency) {
        if (excludeArtifactIds == null || excludeArtifactIds.length == 0)
            return true;

        return !Arrays.asList(excludeArtifactIds).contains(dependency.getArtifactId());
    }

    private boolean resolveScope(final @NotNull Dependency dependency) {
        //return dependency.getScope().equals("provided") || dependency.getScope().equals("compile");
        return dependency.getScope().equals("provided");
        //return !dependency.getScope().equals("test") && dependency.getScope().equals("provided");
    }
}
