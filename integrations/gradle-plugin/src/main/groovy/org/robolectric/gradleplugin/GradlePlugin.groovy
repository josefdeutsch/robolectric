package org.robolectric.gradleplugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.tasks.testing.Test

class GradlePlugin implements Plugin<Project> {
    public static final String downloadTaskName = "robolectricDownloadAndroidSdks"

    static class Config {
        Object sdks
    }

    @Override
    public void apply(Project project) {
        Config config = new Config()
        project.extensions.add("robolectric", config)
        def downloadTask = project.getTasks().create(downloadTaskName, DownloadAndroidSdks.class)

        String robolectricVersion = loadResourceFile("robolectric-version.txt")
        provideSdks(project, config)
        addRobolectricDependencies(project, robolectricVersion)
        enableIncludeAndroidResources(project, downloadTask)
    }

    private addRobolectricDependencies(Project project, robolectricVersion) {
        def selfTestClassPath = System.getenv("gradle-robolectric-plugin.classpath")

        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
                def configuration = project.getConfigurations().getByName("testImplementation")

                Dependency dependency
                if (selfTestClassPath != null) {
                    def parts = selfTestClassPath.split(":")
                    dependency = project.getDependencies().create(project.files(*parts))
                } else {
                    dependency = project.getDependencies().create("org.robolectric:robolectric:$robolectricVersion")
                }
                configuration.getDependencies().add(dependency)

                project.getGradle().removeListener(this)
            }

            @Override
            public void afterResolve(ResolvableDependencies resolvableDependencies) {
            }
        })
    }

    private enableIncludeAndroidResources(Project project, downloadTask) {
        project.afterEvaluate { p ->
            Object androidExt = project.getExtensions().findByName("android")
            if (androidExt == null) {
                throw new GradleException("this isn't an android project?")
            }

            androidExt.testOptions.unitTests.includeAndroidResources = true

            // This needs to happen *after* the Android Gradle plugin creates tasks. Sigh.
            project.afterEvaluate {
                project.getTasks().withType(Test.class).forEach { task ->
                    if (task.name.endsWith("UnitTest")) {
                        task.dependsOn(downloadTask)
                    }
                }
            }
        }
    }

    void provideSdks(Project project, Config config) {
        project.repositories.add(project.repositories.jcenter())

        project.afterEvaluate {
            def defaultSdks = loadPropertiesResourceFile("org.robolectric.GradlePlugin.sdks.properties")
            def enabledSdks = figureSdks(config.sdks, defaultSdks)
            Properties sdkDeps = new Properties()
            enabledSdks.keySet().forEach { Integer apiLevel ->
                def sdkCoords = enabledSdks.get(apiLevel)
                if (sdkCoords instanceof File) {
                    sdkCoords = project.files(sdkCoords)
                }
                def sdkCfg = project.configurations.create("sdk$apiLevel")
                project.dependencies.add("sdk$apiLevel", sdkCoords)

                def sdkFiles = sdkCfg.resolve()
                if (sdkFiles.size() != 1) {
                    throw new IllegalStateException("weird, $sdkCoords returned $sdkFiles, not one file")
                }
                sdkDeps[apiLevel.toString()] = sdkFiles[0].toString()
                println "sdk$apiLevel = ${sdkFiles}"
            }

            def outDir = new File(project.buildDir, "generated/robolectric")
            outDir.mkdirs()
            project.android.sourceSets['test'].resources.srcDir(outDir)
            def outFile = new File(outDir, 'org.robolectric.sdks.properties')
            def out = outFile.newOutputStream()
            sdkDeps.store(out, null)
            out.close()
            println "xxx wrote to $outFile"
            println "props: $sdkDeps"
        }
    }

    Map<Integer, String> figureSdks(Object configSdks, Properties defaultSdks) {
        def map = new HashMap<Integer, String>()

        def add = { Integer apiLevel ->
            def coordinates = defaultSdks.getProperty(apiLevel.toString())
            if (coordinates == null) {
                throw new IllegalArgumentException("Unknown API level $apiLevel")
            }
            map.put(apiLevel, coordinates)
        }

        if (configSdks instanceof String) {
            configSdks.split(",").each { add(Integer.parseInt(it)) }
        } else if (configSdks instanceof Integer) {
            add(configSdks)
        } else if (configSdks instanceof List
                || configSdks instanceof int[]
                || configSdks instanceof Object[]) {
            configSdks.iterator().forEachRemaining {
                if (it instanceof String) add(Integer.parseInt(it)) else add((int) it)
            }
        } else if (configSdks instanceof Map) {
            configSdks.keySet().forEach { key ->
                def coord = configSdks.get(key)
                if (coord instanceof String) {
                    if (coord.contains("/")) coord = new File(coord)
                }
                map.put(key, coord)
            }
        }
        return map
    }

    String loadResourceFile(String name) {
        def resource = GradlePlugin.class.classLoader.getResource(name)
        if (resource == null) throw new IllegalStateException("$name not found")
        return resource.text
    }

    Properties loadPropertiesResourceFile(String name) {
        def props = new Properties()
        def resourceIn = GradlePlugin.class.classLoader.getResourceAsStream(name)
        if (resourceIn == null) throw new IllegalStateException("$name not found")
        try {
            props.load(resourceIn)
        } finally {
            resourceIn.close()
        }
        return props
    }
}
