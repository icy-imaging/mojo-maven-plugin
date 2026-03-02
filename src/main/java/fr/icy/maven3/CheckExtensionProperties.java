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

package fr.icy.maven3;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.regex.Pattern;

@Mojo(name = "check-extension-properties", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CheckExtensionProperties extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Validates the version number of the project using a predefined regular expression pattern.
     * <p>
     * This method checks whether the project's version follows a specific format for stable, alpha,
     * beta, or release candidate versions. If the version does not match any of these patterns, it
     * logs the error messages describing the acceptable formats and throws a {@link MojoFailureException}.
     * <p>
     * Acceptable version formats:
     * <ul>
     * <li>Stable release: <code>X.X.X</code></li>
     * <li>Alpha release: <code>X.X.X-a.X</code></li>
     * <li>Beta release: <code>X.X.X-b.X</code></li>
     * <li>Release candidate: <code>X.X.X-rc.X</code></li>
     * </ul>
     *
     * @throws MojoFailureException if the project's version number is invalid.
     */
    @Override
    public void execute() throws MojoFailureException {
        final String version = project.getVersion();
        final Pattern pattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+(-(a|b|rc)\\.\\d+)?$");
        if (!pattern.matcher(version).matches()) {
            getLog().error("Invalid version number: " + version);
            getLog().error("The version number should match one of the patterns below:");
            getLog().error("- Stable release: X.X.X");
            getLog().error("- Alpha release: X.X.X-a.X");
            getLog().error("- Beta release: X.X.X-b.X");
            getLog().error("- Release candidate: X.X.X-rc.X");
            throw new MojoFailureException("Invalid version number");
        }
    }
}
