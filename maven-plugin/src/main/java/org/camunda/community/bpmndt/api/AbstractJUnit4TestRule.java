package org.camunda.community.bpmndt.api;

import static org.camunda.community.bpmndt.api.TestCaseInstance.PROCESS_ENGINE_NAME;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.test.TestHelper;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.assertions.ProcessEngineTests;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.community.bpmndt.api.cfg.BpmndtProcessEnginePlugin;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Abstract superclass for JUnit 4 based test cases. When a test is started, this class builds the
 * process engine, creates the test case instance and deploys the related BPMN resource.
 */
public abstract class AbstractJUnit4TestRule extends TestWatcher {

  protected TestCaseInstance instance;

  /** ID of the BPMN resource deployment. */
  private String deploymentId;
  /** ID of optional annotation based deployment. */
  private String annotationDeploymentId;

  @Override
  protected void starting(Description description) {
    ProcessEngine processEngine = ProcessEngines.getProcessEngine(PROCESS_ENGINE_NAME);
    if (processEngine == null) {
      processEngine = buildProcessEngine();
    }

    ProcessEngineTests.init(processEngine);

    instance = new TestCaseInstance(processEngine, getProcessDefinitionKey(), getStart(), getEnd());

    // deploy BPMN resource
    deploymentId = instance.deploy(getClass().getName(), getBpmnResourceName());

    // perform optional annotation based deployment (via @Deployment) for DMN files
    Class<?> testClass = description.getTestClass();
    String methodName = description.getMethodName();
    Deployment deployment = description.getAnnotation(Deployment.class);

    annotationDeploymentId = TestHelper.annotationDeploymentSetUp(processEngine, testClass, methodName, deployment);
  }

  @Override
  protected void finished(Description description) {
    Mocks.reset();

    if (instance != null) {
      instance.clear();

      // undeploy BPMN resource
      instance.undeploy(deploymentId);
    }

    // undeploy optional annotation based deployment
    Class<?> testClass = description.getTestClass();
    String methodName = description.getMethodName();

    TestHelper.annotationDeploymentTearDown(getProcessEngine(), annotationDeploymentId, testClass, methodName);
  }

  /**
   * Builds the process engine, used to execute the test case. The method registers custom
   * {@link ProcessEnginePlugin}s as well as the {@link BpmndtProcessEnginePlugin}, which is required
   * to configure a conform process engine.
   * 
   * @return The built process engine.
   */
  protected ProcessEngine buildProcessEngine() {
    List<ProcessEnginePlugin> processEnginePlugins = new LinkedList<>();
    processEnginePlugins.addAll(getProcessEnginePlugins());

    // BPMN Driven Testing plugin must be added last
    processEnginePlugins.add(new BpmndtProcessEnginePlugin());

    ProcessEngineConfigurationImpl processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration();
    processEngineConfiguration.setProcessEngineName(PROCESS_ENGINE_NAME);
    processEngineConfiguration.setProcessEnginePlugins(processEnginePlugins);

    return processEngineConfiguration.buildProcessEngine();
  }

  /**
   * Creates a new executor, used to specify variables, business key and/or mocks that are considered
   * during test case execution. After the specification, {@link TestCaseExecutor#execute()} is called
   * to create a new {@link ProcessInstance} and execute the test case.
   * 
   * @return The newly created executor.
   */
  public TestCaseExecutor createExecutor() {
    return new TestCaseExecutor(instance, this::execute);
  }

  /**
   * Executes the test case.
   * 
   * @param pi A process instance, created especially for the test case.
   */
  protected abstract void execute(ProcessInstance pi);

  /**
   * Returns the name of the BPMN resource, which is used to deploy the process definition that is
   * tested.
   * 
   * @return The BPMN resource name, within {@code src/main/resources}.
   */
  protected abstract String getBpmnResourceName();

  /**
   * Returns the ID of the test case's end activity.
   * 
   * @return The end activity ID.
   */
  protected abstract String getEnd();

  /**
   * Returns the key of the process definition that is tested.
   * 
   * @return The process definition key.
   */
  protected abstract String getProcessDefinitionKey();

  /**
   * Returns the process engine, used to execute the test case.
   * 
   * @return The process engine.
   */
  public ProcessEngine getProcessEngine() {
    return instance.getProcessEngine();
  }
  
  /**
   * Provides custom {@link ProcessEnginePlugin}s to be registered when the process engine is built.
   * By default, this method return an empty list. It can be overriden by any extending class.
   * 
   * @return A list of process engine plugins to register.
   * 
   * @see #buildProcessEngine()
   */
  protected List<ProcessEnginePlugin> getProcessEnginePlugins() {
    return Collections.emptyList();
  }

  /**
   * Returns the ID of the test case's start activity.
   * 
   * @return The start activity ID.
   */
  protected abstract String getStart();
}