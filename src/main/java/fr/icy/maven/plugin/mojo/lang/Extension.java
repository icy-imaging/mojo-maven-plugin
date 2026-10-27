package fr.icy.maven.plugin.mojo.lang;

import org.apache.maven.model.Dependency;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.*;

@ApiStatus.Experimental
public final class Extension {
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_ARTIFACT_ID = "artifactId";
    private static final String KEY_VERSION = "version";
    private static final String KEY_CLASSIFIER = "classifier";
    private static final String KEY_DISTRIBUTION = "distribution";
    private static final String KEY_DEPENDENCIES = "dependencies";
    private static final String KEY_ENABLED = "enabled";

    private final Map<String, Object> content;

    public Extension(
            final String groupId,
            final String artifactId,
            final String version,
            //final String classifier,
            final String distribution,
            final List<Map<String, String>> dependencies,
            final Boolean enabled
    ) {
        content = new HashMap<>();
        content.put(KEY_GROUP_ID, groupId);
        content.put(KEY_ARTIFACT_ID, artifactId);
        content.put(KEY_VERSION, version);
        //content.put(KEY_CLASSIFIER, classifier);
        content.put(KEY_DISTRIBUTION, distribution);
        content.put(KEY_DEPENDENCIES, dependencies);
        content.put(KEY_ENABLED, enabled);
    }

    public Extension() {
        this(
                "",
                "",
                "",
                //"",
                "",
                Collections.emptyList(),
                Boolean.TRUE
        );
    }

    public String getGroupId() {
        return (String) content.get(KEY_GROUP_ID);
    }

    public void setGroupId(final String groupId) {
        content.put(KEY_GROUP_ID, groupId);
    }

    public String getArtifactId() {
        return (String) content.get(KEY_ARTIFACT_ID);
    }

    public void setArtifactId(final String artifactId) {
        content.put(KEY_ARTIFACT_ID, artifactId);
    }

    public String getVersion() {
        return (String) content.get(KEY_VERSION);
    }

    public void setVersion(final String version) {
        content.put(KEY_VERSION, version);
    }

    /*public String getClassifier() {
        return (String) content.get(KEY_CLASSIFIER);
    }*/

    /*public void setClassifier(final String classifier) {
        content.put(KEY_CLASSIFIER, classifier);
    }*/

    public String getDistribution() {
        return (String) content.get(KEY_DISTRIBUTION);
    }

    public void setDistribution(final String distribution) {
        content.put(KEY_DISTRIBUTION, distribution);
    }

    public Boolean getEnabled() {
        return (Boolean) content.get(KEY_ENABLED);
    }

    public void setEnabled(final Boolean enabled) {
        content.put(KEY_ENABLED, enabled);
    }

    @Contract(pure = true)
    public @NonNull @Unmodifiable Map<String, Object> asMap() {
        return Map.copyOf(content);
    }

    private static @NonNull List<Dependency> filterDependencies(final @NonNull List<Dependency> dependencies) {
        final List<Dependency> filtered = new ArrayList<>();
        for (final Dependency dependency : dependencies) {
            switch (dependency.getScope()) {
                case "test", "system", "provided":
                    continue;
            }

            if (dependency.isOptional())
                continue;

            if (dependency.getGroupId().startsWith("fr.icy") && !dependency.getGroupId().startsWith("fr.icy.extension"))
                continue;

            if (dependency.getGroupId().equals("org.jetbrains") && dependency.getArtifactId().equals("annotations"))
                continue;
            if (dependency.getGroupId().equals("org.jspecify") && dependency.getArtifactId().equals("jspecify"))
                continue;

            filtered.add(dependency);
        }
        return filtered;
    }

    public static @NonNull List<Map<String, String>> dependenciesToMapList(final List<Dependency> dependencies) {
        final List<Map<String, String>> converted = new ArrayList<>();
        for (final Dependency dependency : filterDependencies(dependencies)) {
            converted.add(new TreeMap<>(Map.of(
                    KEY_GROUP_ID, dependency.getGroupId(),
                    KEY_ARTIFACT_ID, dependency.getArtifactId(),
                    KEY_VERSION, dependency.getVersion(),
                    KEY_CLASSIFIER, Objects.requireNonNullElse(dependency.getClassifier(), "")
            )));
        }
        return converted;
    }

    @SuppressWarnings("unchecked")
    @Contract("_ -> new")
    public static @NonNull Extension fromMap(final @NonNull Map<String, Object> map) {
        return new Extension(
                (String) map.getOrDefault(KEY_GROUP_ID, ""),
                (String) map.getOrDefault(KEY_ARTIFACT_ID, ""),
                (String) map.getOrDefault(KEY_VERSION, ""),
                //(String) map.getOrDefault(KEY_CLASSIFIER, ""),
                (String) map.getOrDefault(KEY_DISTRIBUTION, ""),
                (List<Map<String, String>>) map.getOrDefault(KEY_DEPENDENCIES, Collections.emptyList()),
                (Boolean) map.getOrDefault(KEY_ENABLED, Boolean.TRUE)
        );
    }
}
