package org.jboss.gm.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.maven.ext.common.ManipulationException;
import org.gradle.api.logging.LogLevel;
import org.jboss.gm.common.rules.LoggingRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginUtilsTest {
    @Rule
    public final LoggingRule loggingRule = new LoggingRule(LogLevel.DEBUG);

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test(expected = ManipulationException.class)
    public void testInvalidPlugin()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "# empty file", Charset.defaultCharset());

        PluginUtils.pluginRemoval(logger, target.getParentFile(), Collections.singleton("gradle-enterprise-2"));
    }

    @Test
    public void testNoRemoval()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "# empty file\n", Charset.defaultCharset());

        PluginUtils.pluginRemoval(logger, target.getParentFile(), Collections.singleton("gradle-enterprise"));

        assertFalse(systemOutRule.getLog()
                .contains("Looking to remove gradle-enterprise with configuration block of gradleEnterprise"));
        assertFalse(systemOutRule.getLog().contains("Removed instances of plugin"));
    }

    @Test
    public void testNoRemoval2()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "# empty file\n", Charset.defaultCharset());

        PluginUtils.pluginRemoval(logger, target.getParentFile(), null);

        assertFalse(systemOutRule.getLog()
                .contains("Looking to remove gradle-enterprise with configuration block of gradleEnterprise"));
        assertFalse(systemOutRule.getLog().contains("Removed instances of plugin"));
    }

    @Test
    public void testRemovalWithUnbalancedBrackets()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "plugins {\n" + "    `gradle-enterprise`\n"
                        + "    id(\"com.github.burrunan.s3-build-cache\")\n"
                        + "}\n" + "\n"
                        + "// This is the name of a current project\n"
                        + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                        + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n"
                        + "    \"bom\",\n" + "    \"benchmarks\",\n"
                        + "    \"postgresql\"\n" + ")\n" + "\n"
                        + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n"
                        + "\n"
                        + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                        + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                        + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n"
                        + "\n" + "fun property(name: String) =\n"
                        + "    when (extra.has(name)) {\n"
                        + "        true -> extra.get(name) as? String\n"
                        + "        else -> null\n" + "    }\n" + "\n"
                        + "val isCiServer = System.getenv().containsKey(\"CI\")\n"
                        + "gradleEnterprise {\n" + "        buildScan {\n"
                        + "        // This { is wrong."
                        + "        termsOfServiceAgree = \"yes\"\n"
                        + "        tag(\"CI\")\n" + "        }\n" + "    }\n",
                Charset.defaultCharset());

        PluginUtils.pluginRemoval(logger, target.getParentFile(), Collections.singleton("gradle-enterprise"));
        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin gradle-enterprise with configuration block of gradleEnterprise"));
        assertTrue(systemOutRule.getLog().contains("Removed instances of plugin"));

        String result = "plugins {\n" + "    id(\"com.github.burrunan.s3-build-cache\")\n" + "}\n" + "\n"
                + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n" + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n" + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n" + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n" + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n";
        assertEquals(result, FileUtils.readFileToString(target, Charset.defaultCharset()));
    }

    @Test
    public void testRemoval1()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "plugins {\n" + "    `gradle-enterprise`\n"
                        + "    id(\"com.github.burrunan.s3-build-cache\")\n"
                        + "}\n" + "\n"
                        + "// This is the name of a current project\n"
                        + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                        + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n"
                        + "    \"bom\",\n" + "    \"benchmarks\",\n"
                        + "    \"postgresql\"\n" + ")\n" + "\n"
                        + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n"
                        + "\n"
                        + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                        + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                        + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n"
                        + "\n" + "fun property(name: String) =\n"
                        + "    when (extra.has(name)) {\n"
                        + "        true -> extra.get(name) as? String\n"
                        + "        else -> null\n" + "    }\n" + "\n"
                        + "val isCiServer = System.getenv().containsKey(\"CI\")\n"
                        + "\n" + "if (isCiServer) {\n"
                        + "    gradleEnterprise {\n" + "        buildScan {\n"
                        + "            termsOfServiceUrl = \"https://gradle.com/terms-of-service\"\n"
                        + "            termsOfServiceAgree = \"yes\"\n"
                        + "            tag(\"CI\")\n" + "        }\n" + "    }\n"
                        + "}\n",
                Charset.defaultCharset());

        PluginUtils.pluginRemoval(logger, target.getParentFile(), Collections.singleton("gradle-enterprise"));

        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin gradle-enterprise with configuration block of gradleEnterprise"));
        assertTrue(systemOutRule.getLog().contains("Removed instances of plugin"));

        String result = "plugins {\n" + "    id(\"com.github.burrunan.s3-build-cache\")\n" + "}\n" + "\n"
                + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n" + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n" + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n" + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n" + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n" + "\n" + "if (isCiServer) {\n" + "}\n";
        assertEquals(result, FileUtils.readFileToString(target, Charset.defaultCharset()));
    }

    @Test
    public void testRemoval2()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target, "plugins {\n" + "    `gradle-enterprise`\n"
                + "    id(\"com.github.burrunan.s3-build-cache\")\n" + "}\n"
                + "\n"
                + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n"
                + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n"
                + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n"
                + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n"
                + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n"
                + "\n" + "if (isCiServer) {\n"
                + "    gradleEnterprise {\n" + "        buildScan {\n"
                + "            termsOfServiceUrl = \"https://gradle.com/terms-of-service\"\n"
                + "            termsOfServiceAgree = \"yes\"\n" + "            tag(\"CI\")\n"
                + "        }\n" + "    }\n"
                + "}\n" + "buildCache {\n" + "    local {\n"
                + "        // Local build cache is dangerous as it might produce inconsistent results\n"
                + "        // in case developer modifies files while the build is running\n"
                + "        enabled = false\n"
                + "    }\n" + "    remote(com.github.burrunan.s3cache.AwsS3BuildCache) {\n"
                + "        region = 'eu-west-1'\n" + "        bucket = 'your-bucket'\n"
                + "        prefix = 'cache/'\n"
                + "        push = isCiServer\n"
                + "        // Credentials will be taken from  S3_BUILD_CACHE_... environment variables\n"
                + "        // anonymous access will be used if environment variables are missing\n"
                + "    }\n"
                + "}\n"
                + "if (true) { }\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("com.github.burrunan.s3-build-cache");
        plugins.add("gradle-enterprise");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        assertTrue(systemOutRule.getLog()
                .contains(
                        "Removed instances of plugin com.github.burrunan.s3-build-cache with configuration block of buildCache"));
        assertTrue(systemOutRule.getLog().contains("Removed instances of plugin"));

        String result = "plugins {\n" + "}\n" + "\n" + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n" + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n" + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n" + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n" + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n" + "\n" + "if (isCiServer) {\n" + "}\n"
                + "if (true) { }\n";
        assertEquals(result, FileUtils.readFileToString(target, Charset.defaultCharset()));
    }

    @Test
    public void testRemoval4()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target, "plugins {\n" + "    `gradle-enterprise`\n"
                + "    id(\"com.github.burrunan.s3-build-cache\")\n" + "}\n"
                + "\n"
                + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n"
                + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n"
                + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n"
                + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n"
                + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n"
                + "\n" + "if (isCiServer) {\n"
                + "    gradleEnterprise {\n" + "        buildScan {\n"
                + "            termsOfServiceUrl = \"https://gradle.com/terms-of-service\"\n"
                + "            termsOfServiceAgree = \"yes\"\n" + "            tag(\"CI\")\n"
                + "        }\n" + "    }\n"
                + "}\n" + "buildCache {\n" + "    local {\n"
                + "        // Local build cache is dangerous as it might produce inconsistent results\n"
                + "        // in case developer modifies files while the build is running\n"
                + "        enabled = false\n"
                + "    }\n" + "    remote(com.github.burrunan.s3cache.AwsS3BuildCache) {\n"
                + "        region = 'eu-west-1'\n" + "        bucket = 'your-bucket'\n"
                + "        prefix = 'cache/'\n"
                + "        push = isCiServer\n"
                + "        // Credentials will be taken from  S3_BUILD_CACHE_... environment variables\n"
                + "        // anonymous access will be used if environment variables are missing\n"
                + "    }\n"
                + "}\nif (true) { }\n",
                Charset.defaultCharset());

        // Avoid singleton as the set is manipulated within the method
        PluginUtils.pluginRemoval(logger, target.getParentFile(), new LinkedHashSet<>(Collections.singleton("ALL")));

        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin gradle-enterprise with configuration block of gradleEnterprise from"));
        assertTrue(systemOutRule.getLog().contains("Removed instances of plugin"));

        String result = "plugins {\n" + "}\n" + "\n" + "// This is the name of a current project\n"
                + "// Note: it cannot be inferred from the directory name as developer might clone pgjdbc to pgjdbc_tmp (or whatever) folder\n"
                + "rootProject.name = \"pgjdbc\"\n" + "\n" + "include(\n" + "    \"bom\",\n" + "    \"benchmarks\",\n"
                + "    \"postgresql\"\n" + ")\n" + "\n" + "project(\":postgresql\").projectDir = file(\"pgjdbc\")\n" + "\n"
                + "// Gradle inherits Ant \"default excludes\", however we do want to archive those files\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitattributes\")\n"
                + "org.apache.tools.ant.DirectoryScanner.removeDefaultExclude(\"**/.gitignore\")\n" + "\n"
                + "fun property(name: String) =\n" + "    when (extra.has(name)) {\n"
                + "        true -> extra.get(name) as? String\n" + "        else -> null\n" + "    }\n" + "\n"
                + "val isCiServer = System.getenv().containsKey(\"CI\")\n" + "\n" + "if (isCiServer) {\n" + "}\n"
                + "if (true) { }\n";
        assertEquals(result, FileUtils.readFileToString(target, Charset.defaultCharset()));
    }

    @Test
    public void testRemoval5()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import org.gradle.api.tasks.testing.logging.TestExceptionFormat\n"
                        + "import org.gradle.api.tasks.testing.logging.TestLogEvent\n" + "\n" + "plugins {\n"
                        + "    id \"org.jruyi.thrift\" version \"0.4.0\"\n" + "    id \"jacoco\"\n"
                        + "    id \"com.github.hierynomus.license\" version \"0.15.0\"\n"
                        + "    id \"com.github.johnrengelman.shadow\" version \"5.0.0\"\n"
                        + "    id \"net.ltgt.errorprone\" version \"0.0.14\"\n"
                        + "    id 'ru.vyarus.animalsniffer' version '1.5.0'\n" + "    id 'java-library'\n"
                        + "    id 'maven-publish'\n" + "    id 'signing'\n"
                        + "    id 'io.codearte.nexus-staging' version '0.20.0'\n"
                        + "    id \"de.marcphilipp.nexus-publish\" version \"0.2.0\" apply false\n"
                        + "    id 'com.github.ben-manes.versions' version '0.21.0'\n"
                        + "    id 'net.researchgate.release' version '2.6.0'\n" + "}\n"
                        + "subprojects {\n"
                        + "    apply plugin: 'ru.vyarus.animalsniffer'\n"
                        + "    apply plugin: 'com.github.hierynomus.license'\n"
                        + "    apply plugin: 'java'\n"
                        + "    apply plugin: 'maven'\n"
                        + "    apply plugin: 'checkstyle'\n"
                        + "    apply plugin: 'de.marcphilipp.nexus-publish'\n",
                Charset.defaultCharset());

        File subfolder = folder.newFolder();
        File subtarget = new File(subfolder, "publish.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(subtarget, "signing {\n" + "    if (isReleaseVersion) {\n"
                + "        sign publishing.publications.mavenJava\n" + "    }\n"
                + "}\n", Charset.defaultCharset());

        // Avoid singleton as the set is manipulated within the method
        PluginUtils.pluginRemoval(logger, target.getParentFile(), new LinkedHashSet<>(Collections.singleton("ALL")));

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin \"signing\" with configuration block of signing from"));
        assertTrue(systemOutRule.getLog().contains("Replacing nexus-publish apply plugin with maven-publish"));
        assertTrue(result.contains("apply plugin: \"maven-publish\""));
        assertFalse(result.contains("com.github.ben-manes.versions"));
        assertTrue(FileUtils.readFileToString(subtarget, Charset.defaultCharset()).trim().isEmpty());
    }

    @Test
    public void testRemoval6()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target, "plugins {\n" + "  `maven-publish`\n" + "  signing\n"
                + "\n" + "  id(\"otel.japicmp-conventions\")\n" + "}\n" + "\n"
                + "publishing {\n" + "  publications {\n"
                + "    register<MavenPublication>(\"mavenPublication\") {\n"
                + "      val release = findProperty(\"otel.release\")\n" + "      if (release != null) {\n"
                + "        val versionParts = version.split('-').toMutableList()\n"
                + "        versionParts[0] += \"-$release\"\n" + "        version = versionParts.joinToString(\"-\")\n"
                + "      }\n" + "      groupId = \"io.opentelemetry\"\n" + "      afterEvaluate {\n"
                + "        // not available until evaluated.\n" + "        artifactId = base.archivesName.get()\n"
                + "        pom.description.set(project.description)\n" + "      }\n"
                + "\n"
                + "      plugins.withId(\"java-platform\") {\n" + "        from(components[\"javaPlatform\"])\n"
                + "      }\n" + "      plugins.withId(\"java-library\") {\n"
                + "        from(components[\"java\"])\n"
                + "      }\n" + "\n" + "      versionMapping {\n" + "        allVariants {\n"
                + "          fromResolutionResult()\n" + "        }\n" + "      }\n"
                + "\n" + "      pom {\n"
                + "        name.set(\"OpenTelemetry Java\")\n"
                + "        url.set(\"https://github.com/open-telemetry/opentelemetry-java\")\n"
                + "\n"
                + "        licenses {\n" + "          license {\n"
                + "            name.set(\"The Apache License, Version 2.0\")\n"
                + "            url.set(\"http://www.apache.org/licenses/LICENSE-2.0.txt\")\n"
                + "          }\n"
                + "        }\n" + "\n" + "        developers {\n" + "          developer {\n"
                + "            id.set(\"opentelemetry\")\n" + "            name.set(\"OpenTelemetry\")\n"
                + "            url.set(\"https://github.com/open-telemetry/community\")\n"
                + "          }\n" + "        }\n"
                + "\n" + "        scm {\n"
                + "          connection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "          developerConnection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "          url.set(\"git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "        }\n"
                + "      }\n" + "    }\n" + "  }\n" + "}\n" + "\n" + "afterEvaluate {\n"
                + "  val publishToSonatype by tasks.getting\n" + "  val release by rootProject.tasks.existing\n"
                + "  release.configure {\n" + "    finalizedBy(publishToSonatype)\n"
                + "  }\n" + "}\n" + "\n"
                + "if (System.getenv(\"CI\") != null) {\n" + "  signing {\n"
                + "    useInMemoryPgpKeys(System.getenv(\"GPG_PRIVATE_KEY\"), System.getenv(\"GPG_PASSWORD\"))\n"
                + "    sign(publishing.publications[\"mavenPublication\"])\n"
                + "  }\n" + "}\n",
                Charset.defaultCharset());

        // Avoid singleton as the set is manipulated within the method
        PluginUtils.pluginRemoval(logger, target.getParentFile(), new LinkedHashSet<>(Collections.singleton("ALL")));

        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin \"signing\" with configuration block of signing from"));

        assertFalse(FileUtils.readFileToString(target, Charset.defaultCharset()).contains("publishToSonatype"));
        assertTrue(FileUtils.readFileToString(target, Charset.defaultCharset()).contains("  id(\"otel.japicmp-conventions\")\n"
                + "}\n" + "\n"
                + "publishing {\n"
                + "  publications {\n"
                + "    register<MavenPublication>(\"mavenPublication\") {\n"
                + "      val release = findProperty(\"otel.release\")\n"
                + "      if (release != null) {\n"
                + "        val versionParts = version.split('-').toMutableList()\n"
                + "        versionParts[0] += \"-$release\"\n"
                + "        version = versionParts.joinToString(\"-\")\n"
                + "      }\n"
                + "      groupId = \"io.opentelemetry\"\n"
                + "      afterEvaluate {\n"
                + "        // not available until evaluated.\n"
                + "        artifactId = base.archivesName.get()\n"
                + "        pom.description.set(project.description)\n"
                + "      }\n" + "\n"
                + "      plugins.withId(\"java-platform\") {\n"
                + "        from(components[\"javaPlatform\"])\n"
                + "      }\n"
                + "      plugins.withId(\"java-library\") {\n"
                + "        from(components[\"java\"])\n"
                + "      }\n" + "\n"
                + "      versionMapping {\n"
                + "        allVariants {\n"
                + "          fromResolutionResult()\n"
                + "        }\n"
                + "      }\n" + "\n"
                + "      pom {\n"
                + "        name.set(\"OpenTelemetry Java\")\n"
                + "        url.set(\"https://github.com/open-telemetry/opentelemetry-java\")\n"
                + "\n"
                + "        licenses {\n"
                + "          license {\n"
                + "            name.set(\"The Apache License, Version 2.0\")\n"
                + "            url.set(\"http://www.apache.org/licenses/LICENSE-2.0.txt\")\n"
                + "          }\n"
                + "        }\n" + "\n"
                + "        developers {\n"
                + "          developer {\n"
                + "            id.set(\"opentelemetry\")\n"
                + "            name.set(\"OpenTelemetry\")\n"
                + "            url.set(\"https://github.com/open-telemetry/community\")\n"
                + "          }\n"
                + "        }\n" + "\n"
                + "        scm {\n"
                + "          connection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "          developerConnection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "          url.set(\"git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                + "        }\n"
                + "      }\n" + "    }\n"
                + "  }\n" + "}\n"));
    }

    @Test
    public void testRemoval7()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "plugins {\n"
                        + "  `maven-publish`\n"
                        + "  signing\n"
                        + "}\n"
                        + "// Sign only if we have a key to do so\n"
                        + "val signingKey: String? = System.getenv(\"GPG_PRIVATE_KEY\")\n"
                        + "// Stub out entire signing block off of CI since Gradle provides no way of lazy configuration of\n"
                        + "// signing tasks.\n"
                        + "if (System.getenv(\"CI\") != null && signingKey != null) {\n"
                        + "  signing \n {\n"
                        + "    useInMemoryPgpKeys(signingKey, System.getenv(\"GPG_PASSWORD\"))\n"
                        + "    sign(publishing.publications[\"maven\"])\n" + "  }\n"
                        + "}\n",
                Charset.defaultCharset());

        // Avoid singleton as the set is manipulated within the method
        PluginUtils.pluginRemoval(logger, target.getParentFile(),
                new LinkedHashSet<>(Collections.singleton("signing")));

        assertTrue(systemOutRule.getLog()
                .contains("Removed instances of plugin \"signing\" with configuration block of signing from"));

        System.out.println(FileUtils.readFileToString(target, Charset.defaultCharset()));
        assertTrue(FileUtils.readFileToString(target, Charset.defaultCharset()).contains("plugins {\n"
                + "  `maven-publish`\n"
                + "  signing\n" + "}\n"
                + "// Sign only if we have a key to do so\n"
                + "val signingKey: String? = System.getenv(\"GPG_PRIVATE_KEY\")\n"
                + "// Stub out entire signing block off of CI since Gradle provides no way of lazy configuration of\n"
                + "// signing tasks.\n"
                + "if (System.getenv(\"CI\") != null && signingKey != null) {\n"
                + "}\n"));
    }

    @Test
    public void testRemoval8()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "pluginManagement {\n" + "  plugins {\n"
                        + "    id(\"com.bmuschko.docker-remote-api\") version \"7.3.0\"\n"
                        + "    id(\"com.github.ben-manes.versions\") version \"0.42.0\"\n"
                        + "    id(\"com.github.jk1.dependency-license-report\") version \"2.1\"\n"
                        + "    id(\"com.google.cloud.tools.jib\") version \"3.2.1\"\n"
                        + "    id(\"com.gradle.plugin-publish\") version \"1.0.0\"\n"
                        + "    id(\"io.github.gradle-nexus.publish-plugin\") version \"1.1.0\"\n"
                        + "    id(\"org.jetbrains.kotlin.jvm\") version \"1.6.20\"\n"
                        + "    id(\"org.unbroken-dome.test-sets\") version \"4.0.0\"\n"
                        + "    id(\"org.xbib.gradle.plugin.jflex\") version \"1.6.0\"\n"
                        + "    id(\"org.unbroken-dome.xjc\") version \"2.0.0\"\n" + "  }\n" + "\n" + "  repositories {\n"
                        + "    gradlePluginPortal()\n" + "    mavenCentral()\n" + "  }\n" + "}\n" + "\n" + "plugins {\n"
                        + "  id(\"com.gradle.enterprise\") version \"3.11.1\"\n"
                        + "  id(\"com.github.burrunan.s3-build-cache\") version \"1.3\"\n"
                        + "  id(\"com.gradle.common-custom-user-data-gradle-plugin\") version \"1.8\"\n" + "}\n" + "\n"
                        + "dependencyResolutionManagement {\n" + "  repositories {\n" + "    mavenCentral()\n"
                        + "    mavenLocal()\n" + "  }\n" + "}\n" + "\n"
                        + "val gradleEnterpriseServer = \"https://ge.opentelemetry.io\"\n"
                        + "val isCI = System.getenv(\"CI\") != null\n"
                        + "val geAccessKey = System.getenv(\"GRADLE_ENTERPRISE_ACCESS_KEY\") ?: \"\"\n" + "\n"
                        + "// if GE access key is not given and we are in CI, then we publish to scans.gradle.com\n"
                        + "val useScansGradleCom = isCI && geAccessKey.isEmpty()\n" + "\n" + "if (useScansGradleCom) {\n"
                        + "  gradleEnterprise {\n" + "    buildScan {\n"
                        + "      termsOfServiceUrl = \"https://gradle.com/terms-of-service\"\n"
                        + "      termsOfServiceAgree = \"yes\"\n" + "      isUploadInBackground = !isCI\n"
                        + "      publishAlways()\n" + "\n" + "      capture {\n" + "        isTaskInputFiles = true\n"
                        + "      }\n" + "    }\n" + "  }\n" + "} else {\n" + "  gradleEnterprise {\n"
                        + "    server = gradleEnterpriseServer\n" + "    buildScan {\n"
                        + "      isUploadInBackground = !isCI\n" + "\n"
                        + "      this as com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures\n"
                        + "      publishIfAuthenticated()\n" + "      publishAlways()\n" + "\n" + "      capture {\n"
                        + "        isTaskInputFiles = true\n" + "      }\n" + "\n"
                        + "      gradle.startParameter.projectProperties[\"testJavaVersion\"]?.let { tag(it) }\n"
                        + "      gradle.startParameter.projectProperties[\"testJavaVM\"]?.let { tag(it) }\n"
                        + "      gradle.startParameter.projectProperties[\"smokeTestSuite\"]?.let {\n"
                        + "        value(\"Smoke test suite\", it)\n" + "      }\n" + "    }\n" + "  }\n" + "}\n" + "\n"
                        + "val geCacheUsername = System.getenv(\"GE_CACHE_USERNAME\") ?: \"\"\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("com.gradle.common-custom-user-data-gradle-plugin");
        plugins.add("com.gradle.enterprise");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());
        assertFalse(result.contains("gradleEnterprise {"));
    }

    @Test
    public void testRemoval9()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "allprojects {\n" + "    group = \"org.postgresql\"\n"
                        + "    version = buildVersion\n" + "\n"
                        + "    apply(plugin = \"com.github.vlsi.gradle-extensions\")\n"
                        + "\n"
                        + "    plugins.withId(\"de.marcphilipp.nexus-publish\") {\n"
                        + "        configure<de.marcphilipp.gradle.nexus.NexusPublishExtension> {\n"
                        + "            clientTimeout.set(java.time.Duration.ofMinutes(15))\n"
                        + "        }\n" + "    }\n" + "\n"
                        + "    plugins.withId(\"io.codearte.nexus-staging\") {\n"
                        + "        configure<io.codearte.gradle.nexus.NexusStagingExtension> {\n"
                        + "            numberOfRetries = 20 * 60 / 2\n"
                        + "            delayBetweenRetriesInMillis = 2000\n"
                        + "        }\n" + "    }\n" + "\n"
                        + "    repositories {\n"
                        + "        if (enableMavenLocal) {\n"
                        + "            mavenLocal()\n" + "        }\n"
                        + "        mavenCentral()\n" + "    }\n" + "\n"
                        + "    val javaMainUsed = file(\"src/main/java\").isDirectory\n"
                        + "\n" + "    plugins.withId(\"java-library\") {\n"
                        + "        dependencies {\n"
                        + "            \"implementation\"(platform(project(\":bom\")))\n"
                        + "        }\n" + "    }\n" + "\n"
                        + "    val kotlinMainUsed = file(\"src/main/kotlin\").isDirectory\n"
                        + "\n"
                        + "    tasks.configureEach<AbstractArchiveTask> {\n"
                        + "        // Ensure builds are reproducible\n"
                        + "        isPreserveFileTimestamps = false\n"
                        + "        isReproducibleFileOrder = true\n"
                        + "        dirMode = \"775\".toInt(8)\n"
                        + "        fileMode = \"664\".toInt(8)\n" + "    }\n"
                        + "\n" + "    plugins.withType<SigningPlugin> {\n"
                        + "        afterEvaluate {\n"
                        + "            configure<SigningExtension> {\n"
                        + "                val release = rootProject.releaseParams.release.get()\n"
                        + "                // Note it would still try to sign the artifacts,\n"
                        + "                // however it would fail only when signing a RELEASE version fails\n"
                        + "                isRequired = release\n"
                        + "                if (useGpgCmd) {\n"
                        + "                    useGpgCmd()\n"
                        + "                }\n" + "            }\n"
                        + "        }\n" + "    }\n" + "}\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertFalse(result.contains("SigningPlugin"));
        assertFalse(result.contains("io.codearte.nexus-staging"));
        assertFalse(result.contains("de.marcphilipp.nexus-publish"));
        assertEquals(StringUtils.countMatches(result, '{'), StringUtils.countMatches(result, '}'));
    }

    @Test
    public void testRemoval10()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "        val publishToSonatype by tasks.getting\n"
                        + "        releaseTask.configure {\n"
                        + "            finalizedBy(publishToSonatype)\n"
                        + "        }\n"
                        + "        rootProject.tasks.named(\"closeAndReleaseRepository\") {\n"
                        + "            mustRunAfter(publishToSonatype)\n"
                        + "        }\n" + "\n"
                        + "        rootProject.tasks.named('closeAndReleaseRepository') {\n"
                        + "            mustRunAfter(publishToSonatype)\n"
                        + "        }\n" + "\n"
                        + "        tasks.withType(Sign::class) {\n"
                        + "            onlyIf { System.getenv(\"CI\") != null }\n"
                        + "        }\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());
        assertFalse(result.contains("closeAndReleaseRepository"));
    }

    @Test
    public void testRemoval11()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import com.google.protobuf.gradle.*\n" + "import de.marcphilipp.gradle.nexus.NexusPublishExtension\n\n"
                        + "    plugins.withId(\"maven-publish\") {\n" + "        plugins.apply(\"signing\")\n" + "\n"
                        + "        plugins.apply(\"de.marcphilipp.nexus-publish\")\n" + "\n"
                        + "        configure<PublishingExtension> {\n" + "            publications {\n"
                        + "                register<MavenPublication>(\"mavenPublication\") {\n"
                        + "                    val release = findProperty(\"otel.release\")\n"
                        + "                    if (release != null) {\n"
                        + "                        val versionParts = version.split('-').toMutableList()\n"
                        + "                        versionParts[0] += \"-${release}\"\n"
                        + "                        version = versionParts.joinToString(\"-\")\n" + "                    }\n"
                        + "                    groupId = \"io.opentelemetry\"\n" + "                    afterEvaluate {\n"
                        + "                        // not available until evaluated.\n"
                        + "                        artifactId = the<BasePluginConvention>().archivesBaseName\n"
                        + "                        pom.description.set(project.description)\n" + "                    }\n"
                        + "\n" + "                    plugins.withId(\"java-platform\") {\n"
                        + "                        from(components[\"javaPlatform\"])\n" + "                    }\n"
                        + "                    plugins.withId(\"java-library\") {\n"
                        + "                        from(components[\"java\"])\n" + "                    }\n" + "\n"
                        + "                    versionMapping {\n" + "                        allVariants {\n"
                        + "                            fromResolutionResult()\n" + "                        }\n"
                        + "                    }\n" + "\n" + "                    pom {\n"
                        + "                        name.set(\"OpenTelemetry Java\")\n"
                        + "                        url.set(\"https://github.com/open-telemetry/opentelemetry-java\")\n"
                        + "\n" + "                        licenses {\n" + "                            license {\n"
                        + "                                name.set(\"The Apache License, Version 2.0\")\n"
                        + "                                url.set(\"http://www.apache.org/licenses/LICENSE-2.0.txt\")\n"
                        + "                            }\n" + "                        }\n" + "\n"
                        + "                        developers {\n" + "                            developer {\n"
                        + "                                id.set(\"opentelemetry\")\n"
                        + "                                name.set(\"OpenTelemetry\")\n"
                        + "                                url.set(\"https://github.com/open-telemetry/community\")\n"
                        + "                            }\n" + "                        }\n" + "\n"
                        + "                        scm {\n"
                        + "                            connection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                        + "                            developerConnection.set(\"scm:git:git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                        + "                            url.set(\"git@github.com:open-telemetry/opentelemetry-java.git\")\n"
                        + "                        }\n" + "                    }\n" + "                }\n"
                        + "            }\n" + "        }\n" + "\n" + "        configure<NexusPublishExtension> {\n"
                        + "            repositories {\n" + "                sonatype()\n" + "            }\n" + "\n"
                        + "            connectTimeout.set(Duration.ofMinutes(5))\n"
                        + "            clientTimeout.set(Duration.ofMinutes(5))\n" + "        }\n" + "\n"
                        + "        val publishToSonatype by tasks.getting\n" + "        releaseTask.configure {\n"
                        + "            finalizedBy(publishToSonatype)\n" + "        }\n"
                        + "        rootProject.tasks.named(\"closeAndReleaseRepository\") {\n"
                        + "            mustRunAfter(publishToSonatype)\n" + "        }\n" + "\n"
                        + "        tasks.withType(Sign::class) {\n"
                        + "            onlyIf { System.getenv(\"CI\") != null }\n" + "        }\n" + "\n"
                        + "        configure<SigningExtension> {\n"
                        + "            useInMemoryPgpKeys(System.getenv(\"GPG_PRIVATE_KEY\"), System.getenv(\"GPG_PASSWORD\"))\n"
                        + "            sign(the<PublishingExtension>().publications[\"mavenPublication\"])\n"
                        + "        }\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());
        assertFalse(result.contains("SigningExtension"));
        assertFalse(result.contains("NexusPublishExtension"));
    }

    @Test
    public void testRemoval12()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask\n"
                        + "\n" + "plugins {\n" + "    `java-platform`\n" + "\n"
                        + "    id(\"com.github.ben-manes.versions\")\n"
                        + "}\ntasks {\n"
                        + "    named<DependencyUpdatesTask>(\"dependencyUpdates\") {\n"
                        + "        revision = \"release\"\n"
                        + "        checkConstraints = true\n" + "\n"
                        + "        rejectVersionIf {\n"
                        + "            isNonStable(candidate.version)\n"
                        + "        }\n" + "    }\n" + "}",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertFalse(result.contains("DependencyUpdatesTask"));
        assertFalse(result.contains("benmanes"));
    }

    @Test
    // https://github.com/ben-manes/caffeine/blob/eda7b1084ebfac76a7e60e8915cc16a23267ff53/settings.gradle#L28
    public void testRemoval13()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "// See https://github.com/vlsi/vlsi-release-plugins\n"
                        + "buildscript {\n" + "  dependencies {\n"
                        + "    classpath('com.github.vlsi.gradle:checksum-dependency-plugin:1.44.0') {\n"
                        + "      // Gradle ships kotlin-stdlib which is good enough\n"
                        + "      exclude(group: \"org.jetbrains.kotlin\", module:\"kotlin-stdlib\")\n"
                        + "    }\n" + "  }\n" + "  repositories {\n"
                        + "    gradlePluginPortal()\n" + "  }\n" + "}\n" + "\n"
                        + "plugins {\n"
                        + "  id \"com.gradle.enterprise\" version \"3.0\"\n"
                        + "}\n" + "\n" + "rootProject.name = 'caffeine'\n" + "\n"
                        + "include 'caffeine'\n" + "include 'guava'\n"
                        + "include 'jcache'\n" + "include 'simulator'\n" + "\n"
                        + "// Note: we need to verify the checksum for checksum-dependency-plugin itself\n"
                        + "def expectedSha512 = [\n"
                        + "  \"18BC69198D8A217BD231A1A35F0A15543236F8F955DC94F49C6C0E438C3EB2B0A522ED4D5218EFE75619013C492EE97071FCE241EB1CA70E563754176DDAA6DD\":\n"
                        + "    \"gradle-enterprise-gradle-plugin-3.0.jar\",\n"
                        + "  \"43BC9061DFDECA0C421EDF4A76E380413920E788EF01751C81BDC004BD28761FBD4A3F23EA9146ECEDF10C0F85B7BE9A857E9D489A95476525565152E0314B5B\":\n"
                        + "    \"bcpg-jdk15on-1.62.jar\",\n"
                        + "  \"2BA6A5DEC9C8DAC2EB427A65815EB3A9ADAF4D42D476B136F37CD57E6D013BF4E9140394ABEEA81E42FBDB8FC59228C7B85C549ED294123BF898A7D048B3BD95\":\n"
                        + "    \"bcprov-jdk15on-1.62.jar\",\n"
                        + "  \"17DAAF511BE98F99007D7C6B3762C9F73ADD99EAB1D222985018B0258EFBE12841BBFB8F213A78AA5300F7A3618ACF252F2EEAD196DF3F8115B9F5ED888FE827\":\n"
                        + "    \"okhttp-4.1.0.jar\",\n"
                        + "  \"93E7A41BE44CC17FB500EA5CD84D515204C180AEC934491D11FC6A71DAEA761FB0EECEF865D6FD5C3D88AAF55DCE3C2C424BE5BA5D43BEBF48D05F1FA63FA8A7\":\n"
                        + "    \"okio-2.2.2.jar\",\n"
                        + "  \"A86B9B2CBA7BA99860EF2F23555F1E1C1D5CB790B1C47536C32FE7A0FDA48A55694A5457B9F42C60B4725F095B90506324BDE0299F08E9E76B5944FB308375AC\":\n"
                        + "    \"checksum-dependency-plugin-1.44.0.jar\",\n"
                        + "]\n" + "\n" + "def sha512(File file) {\n"
                        + "  def md = java.security.MessageDigest.getInstance('SHA-512')\n"
                        + "  file.eachByte(8192) { buffer, length ->\n"
                        + "     md.update(buffer, 0, length)\n" + "  }\n"
                        + "  new BigInteger(1, md.digest()).toString(16).toUpperCase()\n"
                        + "}\n"
                        + "buildScan {\n"
                        + "  termsOfServiceAgree = 'yes'\n"
                        + "  termsOfServiceUrl = 'https://gradle.com/terms-of-service'\n" +
                        "}\n\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertFalse(result.contains("id \"com.gradle.enterprise\" version \"3.0\""));
        assertTrue(result.contains("gradle-enterprise-gradle-plugin"));
        assertFalse(result.contains("buildScan"));
    }

    @Test
    public void testRemoval14()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "plugins {\n"
                        + "  // This adds tasks to auto close or release nexus staging repos\n"
                        + "  // see https://github.com/Codearte/gradle-nexus-staging-plugin/\n"
                        + "  id 'io.codearte.nexus-staging'\n"
                        + "  //OWASP Security Vulnerability Detection\n"
                        + "  id 'org.owasp.dependencycheck'\n" + "}\n" + "\n"
                        + "wrapper {\n"
                        + "  distributionType = Wrapper.DistributionType.ALL\n"
                        + "}\n" + "\n" + "allprojects {\n"
                        + "  version = findProperty('overrideVersion') ?: ehcacheVersion\n"
                        + "}\n" + "\n" + "if (deployUrl.contains('nexus')) {\n"
                        + "  //internal terracotta config, shorten url for this plugin to end at local/\n"
                        + "  project.nexusStaging {\n"
                        + "    serverUrl = deployUrl.replaceAll(~/local\\/.*$/, \"local/\")\n"
                        + "    packageGroup = 'Ehcache OS' //internal staging repository name\n"
                        + "  }\n" + "  ext {\n"
                        + "    deployUser = tcDeployUser\n"
                        + "    deployPwd = tcDeployPassword\n" + "  }\n"
                        + "} else {\n" + "  project.nexusStaging {\n"
                        + "    packageGroup = 'org.ehcache' //Sonatype staging repository name\n"
                        + "  }\n" + "  ext {\n"
                        + "    deployUser = sonatypeUser\n"
                        + "    deployPwd = sonatypePwd\n" + "  }\n" + "}\n" + "\n"
                        + "nexusStaging {\n"
                        + "  username = project.ext.deployUser\n"
                        + "  password = project.ext.deployPwd\n"
                        + "  logger.debug(\"Nexus Staging: Using login ${username} and url ${serverUrl}\")\n"
                        + "  // Sonatype is often very slow in these operations:\n"
                        + "  delayBetweenRetriesInMillis = (findProperty('delayBetweenRetriesInMillis') ?: '10000') as int\n"
                        + "  numberOfRetries = (findProperty('numberOfRetries') ?: '100') as int\n"
                        + "}\n",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertFalse(result.contains("nexusStaging"));
    }

    @Test
    public void testRemoval15()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import org.gradle.api.tasks.testing.logging.TestExceptionFormat\n"
                        + "import org.gradle.api.tasks.testing.logging.TestLogEvent\n" + "\n" + "plugins {\n"
                        + "    id \"org.jruyi.thrift\" version \"0.4.0\"\n" + "    id \"jacoco\"\n"
                        + "    id \"com.github.hierynomus.license\" version \"0.15.0\"\n"
                        + "    id \"com.github.johnrengelman.shadow\" version \"5.0.0\"\n"
                        + "    id \"net.ltgt.errorprone\" version \"0.0.14\"\n"
                        + "    id 'ru.vyarus.animalsniffer' version '1.5.0'\n" + "    id 'java-library'\n"
                        + "    id 'maven-publish'\n"
                        + "    id 'signing'\n"
                        + "    id 'io.codearte.nexus-staging' version '0.20.0'\n"
                        + "    id \"de.marcphilipp.nexus-publish\" version \"0.2.0\" apply false\n"
                        + "    id 'com.github.ben-manes.versions' version '0.21.0'\n"
                        + "    id 'net.researchgate.release' version '2.6.0'\n" + "}\n"
                        + "subprojects {\n"
                        + "    apply plugin: 'ru.vyarus.animalsniffer'\n"
                        + "    apply plugin: 'com.github.hierynomus.license'\n"
                        + "    apply plugin: 'java'\n"
                        + "    apply plugin: 'maven'\n"
                        + "    apply plugin: 'checkstyle'\n"
                        + "    apply plugin: 'de.marcphilipp.nexus-publish'\n",
                Charset.defaultCharset());

        // Avoid singleton as the set is manipulated within the method
        PluginUtils.pluginRemoval(logger, target.getParentFile(),
                new LinkedHashSet<>(Collections.singleton("RECC")));

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        assertTrue(systemOutRule.getLog().contains("Replacing nexus-publish apply plugin with maven-publish"));
        assertTrue(result.contains("id 'signing'"));
        assertTrue(result.contains("apply plugin: \"maven-publish\""));
        assertFalse(result.contains("com.github.ben-manes.versions"));
    }

    @Test
    public void testRemoval16()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "\n" + "plugins {\n" + "    `java-platform`\n" + "\n"
                        + "    id(\"com.github.ben-manes.versions\")\n"
                        + "}\ntasks {\n"
                        + "    tasks.named(\"dependencyUpdates\").configure {\n"
                        + "        revision = \"release\"\n"
                        + "        checkConstraints = true\n" + "\n"
                        + "        rejectVersionIf {\n"
                        + "            isNonStable(candidate.version)\n"
                        + "        }\n"
                        + "    }\n"
                        + "    tasks.named('dependencyUpdates').configure {\n"
                        + "        revision = \"release\"\n"
                        + "        checkConstraints = true\n" + "\n"
                        + "    }\n"
                        + "}",
                Charset.defaultCharset());

        HashSet<String> plugins = new LinkedHashSet<>();
        plugins.add("ALL");
        PluginUtils.pluginRemoval(logger, target.getParentFile(), plugins);

        String result = FileUtils.readFileToString(target, Charset.defaultCharset());

        System.out.println(result);
        assertFalse(result.contains("dependencyUpdates"));
        assertFalse(result.contains("benmanes"));
    }

    @Test
    public void testCheckForSemanticPlugin1()
            throws IOException, ManipulationException {

        File target = folder.newFile("settings.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import org.gradle.util.GradleVersion\n" + "\n"
                        + "buildscript {\n" + "  repositories {\n"
                        + "    maven {\n"
                        + "      url 'https://plugins.gradle.org/m2/'\n"
                        + "    }\n" + "  }\n" + "  dependencies {\n"
                        + "    // Needed to override an old version of Apache HttpClient that was being included by the\n"
                        + "    // net.vivin.gradle-semantic-build-versioning plugin.\n"
                        + "    // See https://www.jfrog.com/jira/browse/GAP-317 for more info.\n"
                        + "    classpath 'org.apache.httpcomponents:httpclient:4.5.13'\n"
                        + "    classpath 'gradle.plugin.net.vivin:gradle-semantic-build-versioning:4.0.0'\n"
                        + "  }\n" + "}\n" + "\n",
                Charset.defaultCharset());

        boolean result = PluginUtils.checkForSemanticBuildVersioning(logger, target.getParentFile());
        assertFalse(result);
        assertFalse(systemOutRule.getLog().contains("Found Semantic Build Versioning Plugin"));
    }

    @Test
    public void testCheckForSemanticPlugin2()
            throws IOException, ManipulationException {

        File target = folder.newFile("settings.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "import org.gradle.util.GradleVersion\n" + "\n"
                        + "buildscript {\n" + "  repositories {\n"
                        + "    maven {\n"
                        + "      url 'https://plugins.gradle.org/m2/'\n"
                        + "    }\n" + "  }\n" + "  dependencies {\n"
                        + "    classpath 'org.apache.httpcomponents:httpclient:4.5.13'\n"
                        + "    classpath 'gradle.plugin.net.vivin:gradle-semantic-build-versioning:4.0.0'\n"
                        + "  }\n" + "}\n" + "\n"
                        + "apply plugin: 'net.vivin.gradle-semantic-build-versioning'\n"
                        + "\n" + "//otherwise it defaults to the folder name\n"
                        + "rootProject.name = 'cruise-control'\n" + "\n"
                        + "include 'cruise-control', "
                        + "'cruise-control-metrics-reporter', "
                        + "'cruise-control-core'\n",
                Charset.defaultCharset());

        boolean result = PluginUtils.checkForSemanticBuildVersioning(logger, target.getParentFile());
        assertTrue(result);
        assertTrue(systemOutRule.getLog().contains("Found Semantic Build Versioning Plugin"));
    }

    @Test
    public void testLenientLockMode1()
            throws IOException, ManipulationException {

        File target = folder.newFile("build.gradle");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "\n" + "buildscript {\n" + "    dependencyLocking {\n"
                        + "        lockAllConfigurations()\n" + "    }\n" + "\n"
                        + "    repositories {\n" + "        mavenCentral()\n"
                        + "        gradlePluginPortal()\n"
                        + "    }\n",
                Charset.defaultCharset());

        PluginUtils.addLenientLockMode(logger, target.getParentFile());
        assertTrue(systemOutRule.getLog().contains("Added LENIENT lockMode"));
        assertTrue(FileUtils.readFileToString(target, Charset.defaultCharset()).contains("buildscript {\n"
                + "    dependencyLocking {\n"
                + " lockMode = LockMode.LENIENT\n"
                + "        lockAllConfigurations()\n"
                + "    }\n"));
    }

    @Test
    public void testLenientLockMode2()
            throws IOException, ManipulationException {
        File target = folder.newFile("build.gradle.kts");
        org.apache.commons.io.FileUtils.writeStringToFile(target,
                "\n" + "buildscript {\n" + "    dependencyLocking {\n"
                        + "        lockAllConfigurations()\n" + "    }\n" + "\n"
                        + "    repositories {\n" + "        mavenCentral()\n"
                        + "        gradlePluginPortal()\n"
                        + "    }\n",
                Charset.defaultCharset());

        PluginUtils.addLenientLockMode(logger, target.getParentFile());
        assertTrue(systemOutRule.getLog().contains("Added LENIENT lockMode"));
        assertTrue(FileUtils.readFileToString(target, Charset.defaultCharset()).contains("buildscript {\n"
                + "    dependencyLocking {\n"
                + " lockMode.set(LockMode.LENIENT) \n"
                + "        lockAllConfigurations()\n"
                + "    }\n"));
    }

    @Test
    public void testParseDokkaVersion() throws ManipulationException {
        assertEquals(PluginUtils.DokkaVersion.parseVersion("0.9.17"), PluginUtils.DokkaVersion.MINIMUM);
        assertEquals(PluginUtils.DokkaVersion.parseVersion("0.10.1"), PluginUtils.DokkaVersion.TEN);
        assertEquals(PluginUtils.DokkaVersion.parseVersion("1.6.0"), PluginUtils.DokkaVersion.POST_ONE);
    }

    @Test(expected = ManipulationException.class)
    public void testParseDokkaInvalidVersion()
            throws ManipulationException {
        PluginUtils.DokkaVersion.parseVersion("");
    }
}
