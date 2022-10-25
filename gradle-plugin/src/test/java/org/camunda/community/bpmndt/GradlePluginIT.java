package org.camunda.community.bpmndt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.camunda.community.bpmndt.test.GradlePluginRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.robotframework.RobotFramework;

/**
 * Robot Framework based integration tests.
 */
public class GradlePluginIT {

  @Rule
  public GradlePluginRule gradlePlugin = new GradlePluginRule();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testIntegration() {
    assumeTrue("Gradle plugin has not been installed", gradlePlugin.isInstalled());

    // set console encoding
    System.setProperty("python.console.encoding", StandardCharsets.UTF_8.name());

    List<String> arguments = new LinkedList<>();
    arguments.add("run");
    arguments.add("--consolecolors");
    arguments.add("off");
    arguments.add("-v");
    arguments.add("TEMP:" + temporaryFolder.getRoot().getAbsolutePath());
    arguments.add("-v");
    arguments.add("VERSION:" + gradlePlugin.getVersion());
    arguments.add("--include");
    arguments.add("gradle");
    arguments.add("--exclude");
    arguments.add("ignore");
    arguments.add("--outputdir");
    arguments.add("./target/robot");
    arguments.add("../integration-tests");

    int exitCode = RobotFramework.run(arguments.toArray(new String[arguments.size()]));
    assertThat(exitCode, is(0));
  }
}