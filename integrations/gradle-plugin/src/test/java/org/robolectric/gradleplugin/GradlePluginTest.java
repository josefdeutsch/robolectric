package org.robolectric.gradleplugin;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class GradlePluginTest {

  private File gradleProjectDir;
  private String androidSdkRoot;
  private GradlePlugin plugin;
  private Properties defaultSdks;

  @Before
  public void setUp() throws Exception {
    gradleProjectDir = Helpers.findTestProject();
    androidSdkRoot = Helpers.findAndroidSdkRoot();
    plugin = new GradlePlugin();
    defaultSdks = new Properties();
    defaultSdks.setProperty("17", "sdk17.jar");
    defaultSdks.setProperty("18", "sdk18.jar");
    defaultSdks.setProperty("19", "sdk19.jar");
  }

  @Test
  public void figureSdksAcceptsStrings() throws Exception {
    assertThat(plugin.figureSdks("17", defaultSdks))
        .isEqualTo(mapOf(17, "sdk17.jar"));
  }

  @Test
  public void figureSdksAcceptsInts() throws Exception {
    assertThat(plugin.figureSdks(18, defaultSdks))
        .isEqualTo(mapOf(18, "sdk18.jar"));
  }

  @Test
  public void figureSdksAcceptsIntArrays() throws Exception {
    assertThat(plugin.figureSdks(new int[] {17, 19}, defaultSdks))
        .isEqualTo(mapOf(17, "sdk17.jar", 19, "sdk19.jar"));
  }

  @Test
  public void figureSdksAcceptsIntLists() throws Exception {
    assertThat(plugin.figureSdks(Arrays.asList(17, 19), defaultSdks))
        .isEqualTo(mapOf(17, "sdk17.jar", 19, "sdk19.jar"));
  }

  @Test
  public void figureSdksAcceptsMapWithCoordinates() throws Exception {
    Map<Integer, Object> config = mapOf(
        17, "org.xxx:mysdk:1.2.3",
        19, "/path/to/another19.jar",
        20, new File("/path/somewhere.jar")
        );

    assertThat(plugin.figureSdks(config, defaultSdks))
        .isEqualTo(mapOf(
            17, "org.xxx:mysdk:1.2.3",
            19, new File("/path/to/another19.jar"),
            20, new File("/path/somewhere.jar")
        ));
  }

  @Test
  public void pluginAddsTaskToDownloadAndroidSdks() {
    Project project = ProjectBuilder.builder()
        .withProjectDir(gradleProjectDir).build();
    project.getPluginManager().apply("org.robolectric");

    assertThat(project.getTasks().getByName("robolectricDownloadAndroidSdks"))
        .isInstanceOf(DownloadAndroidSdks.class);
  }

  @Ignore("evaluating project doesn't work") @Test
  public void pluginAddsRobolectricToTestImplementationDependencies() {
    Project project = ProjectBuilder.builder()
        .withProjectDir(gradleProjectDir).build();
    project.getPluginManager().apply("org.robolectric");
    ((DefaultProject) project).evaluate();

    DependencySet testIntegrationDeps =
        project.getConfigurations().getByName("testIntegration").getDependencies();

    List<Dependency> deps = new ArrayList<>();
    testIntegrationDeps.forEach(dependency -> {
          if (dependency.getGroup().equals("org.robolectric")) {
            deps.add(dependency);
          }
        }
    );
    System.out.println("deps = " + deps);
  }

  static <K, V> Map<K, V> mapOf(K k1, V v1) {
    HashMap<K, V> map = new HashMap<>();
    map.put(k1, v1);
    return map;
  }
  
  static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
    HashMap<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }

  static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
    HashMap<K, V> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    return map;
  }
}
