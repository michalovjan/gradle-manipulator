package org.jboss.gm.manipulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.internal.Pair;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.jboss.gm.common.io.ManipulationIO;
import org.jboss.gm.common.model.ManipulationModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

public class SimpleProjectFunctionalTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Rule
    public final TestRule restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void ignoreMissingManipulationFile() throws IOException, URISyntaxException {

        final File simpleProjectRoot = tempDir.newFolder("simple-project");
        TestUtils.copyDirectory("simple-project", simpleProjectRoot);
        assertThat(simpleProjectRoot.toPath().resolve("build.gradle")).exists();
        new File(simpleProjectRoot, ManipulationIO.MANIPULATION_FILE_NAME).delete();

        TestUtils.createGradleRunner()
                .withProjectDir(simpleProjectRoot)
                .withArguments("generatePomFileForMainPublication")
                //.withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build();

        assertThat(systemOutRule.getLog().contains("No manipulation.json found in"));
    }

    @Test
    public void ensureProperPomGenerated() throws IOException, URISyntaxException, XmlPullParserException {
        final File simpleProjectRoot = tempDir.newFolder("simple-project");
        TestUtils.copyDirectory("simple-project", simpleProjectRoot);
        assertThat(simpleProjectRoot.toPath().resolve("build.gradle")).exists();

        final ManipulationModel alignment = ManipulationIO.readManipulationModel(simpleProjectRoot);

        final BuildResult buildResult = TestUtils.createGradleRunner()
                .withProjectDir(simpleProjectRoot)
                .withArguments("--info", "generatePomFileForMainPublication")
                //.withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build();

        assertThat(buildResult.task(":" + "generatePomFileForMainPublication").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        final Pair<Model, ManipulationModel> modelAndModule = TestUtils.getModelAndCheckGAV(simpleProjectRoot, alignment,
                "build/publications/main/pom-default.xml");

        final ManipulationModel module = modelAndModule.getRight();
        assertThat(modelAndModule.getLeft().getDependencies())
                .extracting("artifactId", "version")
                .containsOnly(
                        TestUtils.getAlignedTuple(module, "hibernate-core"),
                        TestUtils.getAlignedTuple(module, "commons-lang3", "3.8.1"),
                        TestUtils.getAlignedTuple(module, "undertow-core"));
    }

    @Test
    public void ensureDocs() throws IOException, URISyntaxException {
        final File simpleProjectRoot = tempDir.newFolder("simple-project");
        TestUtils.copyDirectory("simple-project", simpleProjectRoot);
        assertThat(simpleProjectRoot.toPath().resolve("build.gradle")).exists();

        final BuildResult buildResult = TestUtils.createGradleRunner()
                .withProjectDir(simpleProjectRoot)
                .withArguments("distZip")
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build();

        assertThat(buildResult.task(":" + "distZip").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(simpleProjectRoot.toPath().resolve("build/distributions/dummy-1.0.1-redhat-00001-docs.zip")).exists();
        assertThat(simpleProjectRoot.toPath().resolve("build/distributions/dummy-1.0.1-redhat-00001-dist.zip")).exists();
    }

    @Test
    public void ensurePublish() throws IOException, URISyntaxException {
        final File simpleProjectRoot = tempDir.newFolder("simple-project");
        TestUtils.copyDirectory("simple-project", simpleProjectRoot);
        assertThat(simpleProjectRoot.toPath().resolve("build.gradle")).exists();

        final File publishDirectory = tempDir.newFolder("publish");
        System.setProperty("AProxDeployUrl", "file://" + publishDirectory.toString());

        final BuildResult buildResult = TestUtils.createGradleRunner()
                .withProjectDir(simpleProjectRoot)
                .withArguments("--info", "publish")
                .forwardOutput()
                .withPluginClasspath()
                //.withDebug(true)
                .build();

        assertThat(buildResult.task(":" + "publishMainPublicationToJboss-snapshots-repositoryRepository").getOutcome())
                .isEqualTo(TaskOutcome.SKIPPED);
        assertThat(buildResult.task(":" + "publish").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(publishDirectory).exists();
    }
}
