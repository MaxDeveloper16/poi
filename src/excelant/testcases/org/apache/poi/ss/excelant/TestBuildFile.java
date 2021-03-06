/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.poi.ss.excelant;

import static org.apache.poi.POITestCase.assertContains;
import static org.apache.poi.POITestCase.assertNotContained;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;

import org.apache.poi.POIDataSamples;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *  JUnit test for the ExcelAnt tasks.
 *  Leverages Ant's test framework.
 */
public class TestBuildFile {

    protected Project project;

    private StringBuilder logBuffer;
    private StringBuilder fullLogBuffer;
    private BuildException buildException;


    @Before
    public void setUp() {
        String filename = TestBuildFile.getDataDir() + "/../src/excelant/testcases/org/apache/poi/ss/excelant/tests.xml";
        int logLevel = Project.MSG_DEBUG;

        logBuffer = new StringBuilder();
        fullLogBuffer = new StringBuilder();
        project = new Project();
        project.init();
        project.setNewProperty("data.dir.name", getDataDir());
        File antFile = new File(System.getProperty("root"), filename);
        project.setUserProperty("ant.file", antFile.getAbsolutePath());
        project.addBuildListener(new AntTestListener(logLevel));
        ProjectHelper.configureProject(project, antFile);

    }

    /**
     * Automatically calls the target called "tearDown"
     * from the build file tested if it exits.
     * <p>
     * This allows to use Ant tasks directly in the build file
     * to clean up after each test. Note that no "setUp" target
     * is automatically called, since it's trivial to have a
     * test target depend on it.
     */
    @After
    public void tearDown() {
        if (project == null) {
            /*
             * Maybe the BuildFileTest was subclassed and there is
             * no initialized project. So we could avoid getting a
             * NPE.
             * If there is an initialized project getTargets() does
             * not return null as it is initialized by an empty
             * HashSet.
             */
            return;
        }
        final String tearDown = "tearDown";
        if (project.getTargets().containsKey(tearDown)) {
            project.executeTarget(tearDown);
        }
    }

    /**
     * run a target, expect for any build exception
     *
     * @param target target to run
     * @param cause  information string to reader of report
     */
    public void expectBuildException(String target, String cause) {
        expectSpecificBuildException(target, cause, null);
    }

    /**
     * Assert that the given substring is in the log messages.
     */
    public void assertLogContaining(String substring) {
        assertContains(getLog(), substring);
    }

    /**
     * Assert that the given substring is not in the log messages.
     */
    public void assertLogNotContaining(String substring) {
        assertNotContained(getLog(), substring);
    }

    /**
     * Gets the log the BuildFileTest object.
     * Only valid if configureProject() has been called.
     *
     * @return The log value
     */
    public String getLog() {
        return logBuffer.toString();
    }

    /**
     * Executes a target we have set up
     *
     * @param targetName target to run
     */
    public void executeTarget(String targetName) {
        PrintStream sysOut = System.out;
        PrintStream sysErr = System.err;
        try {
            sysOut.flush();
            sysErr.flush();
            StringBuilder outBuffer = new StringBuilder();
            PrintStream out = new PrintStream(new AntOutputStream(outBuffer));
            System.setOut(out);
            StringBuilder errBuffer = new StringBuilder();
            PrintStream err = new PrintStream(new AntOutputStream(errBuffer));
            System.setErr(err);
            logBuffer = new StringBuilder();
            fullLogBuffer = new StringBuilder();
            buildException = null;
            project.executeTarget(targetName);
        } finally {
            System.setOut(sysOut);
            System.setErr(sysErr);
        }

    }

    /**
     * Runs a target, wait for a build exception.
     *
     * @param target target to run
     * @param cause  information string to reader of report
     * @param msg    the message value of the build exception we are waiting
     *               for set to null for any build exception to be valid
     */
    public void expectSpecificBuildException(String target, String cause, String msg) {
        try {
            executeTarget(target);
        } catch (org.apache.tools.ant.BuildException ex) {
            buildException = ex;
            if ((null != msg) && (!ex.getMessage().equals(msg))) {
                fail("Should throw BuildException because '" + cause
                        + "' with message '" + msg
                        + "' (actual message '" + ex.getMessage() + "' instead)");
            }
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }

    public static String getDataDir() {
        String dataDirName = System.getProperty(POIDataSamples.TEST_PROPERTY);
        return dataDirName == null ? "test-data" : dataDirName;
    }

    /**
     * an output stream which saves stuff to our buffer.
     */
    protected static class AntOutputStream extends java.io.OutputStream {
        private StringBuilder buffer;

        public AntOutputStream(StringBuilder buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(int b) {
            buffer.append((char) b);
        }
    }

    /**
     * Our own personal build listener.
     */
    private class AntTestListener implements BuildListener {
        private int logLevel;

        /**
         * Constructs a test listener which will ignore log events
         * above the given level.
         */
        public AntTestListener(int logLevel) {
            this.logLevel = logLevel;
        }

        /**
         * Fired before any targets are started.
         */
        @Override
        public void buildStarted(BuildEvent event) {
        }

        /**
         * Fired after the last target has finished. This event
         * will still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        @Override
        public void buildFinished(BuildEvent event) {
        }

        /**
         * Fired when a target is started.
         *
         * @see BuildEvent#getTarget()
         */
        @Override
        public void targetStarted(BuildEvent event) {
            //System.out.println("targetStarted " + event.getTarget().getName());
        }

        /**
         * Fired when a target has finished. This event will
         * still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        @Override
        public void targetFinished(BuildEvent event) {
            //System.out.println("targetFinished " + event.getTarget().getName());
        }

        /**
         * Fired when a task is started.
         *
         * @see BuildEvent#getTask()
         */
        @Override
        public void taskStarted(BuildEvent event) {
            //System.out.println("taskStarted " + event.getTask().getTaskName());
        }

        /**
         * Fired when a task has finished. This event will still
         * be throw if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        @Override
        public void taskFinished(BuildEvent event) {
            //System.out.println("taskFinished " + event.getTask().getTaskName());
        }

        /**
         * Fired whenever a message is logged.
         *
         * @see BuildEvent#getMessage()
         * @see BuildEvent#getPriority()
         */
        @Override
        public void messageLogged(BuildEvent event) {
            if (event.getPriority() > logLevel) {
                // ignore event
                return;
            }

            if (event.getPriority() == Project.MSG_INFO ||
                    event.getPriority() == Project.MSG_WARN ||
                    event.getPriority() == Project.MSG_ERR) {
                logBuffer.append(event.getMessage());
            }
            fullLogBuffer.append(event.getMessage());
        }
    }

    @Test
    public void testMissingFilename() {
        expectSpecificBuildException("test-nofile", "required argument not specified",
                                     "fileName attribute must be set!");
    }

    @Test
    public void testFileNotFound() {
        expectSpecificBuildException("test-filenotfound", "required argument not specified",
                                     "Cannot load file invalid.xls. Make sure the path and file permissions are correct.");
    }

    @Test
    public void testEvaluate() {
        executeTarget("test-evaluate");
        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.");
    }

    @Test
    public void testEvaluateNoDetails() {
        executeTarget("test-evaluate-nodetails");
        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogNotContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.");
    }

    @Test
    public void testPrecision() {
        executeTarget("test-precision");

        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.  " +
                                    "It evaluated to 2285.5761494145563 when the value of 2285.576149 with precision of 1.0E-4");
        assertLogContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.  " +
                                    "It evaluated to 2285.5761494145563 when the value of 2285.576149 with precision of 1.0E-5");
        assertLogContaining("Failed to evaluate cell 'MortgageCalculator'!$B$4.  " +
                                    "It evaluated to 2285.5761494145563 when the value of 2285.576149 with precision of 1.0E-10 was expected.");
        assertLogContaining("2/3 tests passed");
    }

    @Test
    public void testPrecisionFail() {
        expectSpecificBuildException("test-precision-fails", "precision not matched",
                                     "\tFailed to evaluate cell 'MortgageCalculator'!$B$4.  It evaluated to 2285.5761494145563 when the value of 2285.576149 with precision of 1.0E-10 was expected.");
    }

    @Test
    public void testPassOnError() {
        executeTarget("test-passonerror");
    }

    @Test
    public void testFailOnError() {
        expectBuildException("test-failonerror", "fail on error");
        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogNotContaining("failed because 1 of 0 evaluations failed to evaluate correctly. Failed to evaluate cell 'MortageCalculatorFunction'!$D$3");
    }

    @Test
    public void testFailOnErrorNoDetails() {
        expectBuildException("test-failonerror-nodetails", "fail on error");
        assertLogNotContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogNotContaining("failed because 1 of 0 evaluations failed to evaluate correctly. Failed to evaluate cell 'MortageCalculatorFunction'!$D$3");
    }

    @Test
    public void testUdf() {
        executeTarget("test-udf");
        assertLogContaining("1/1 tests passed");
    }

    @Test
    public void testSetText() {
        executeTarget("test-settext");
        assertLogContaining("1/1 tests passed");
    }

    @Test
    public void testAddHandler() {
        executeTarget("test-addhandler");
        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.");

        assertNotNull("The workbook should have been passed to the handler", MockExcelAntWorkbookHandler.workbook);
        assertTrue("The handler should have been executed", MockExcelAntWorkbookHandler.executed);
    }

    @Test
    public void testAddHandlerWrongClass() {
        executeTarget("test-addhandler-wrongclass");
        assertLogContaining("Using input file: " + TestBuildFile.getDataDir() + "/spreadsheet/excelant.xls");
        assertLogContaining("Succeeded when evaluating 'MortgageCalculator'!$B$4.");
    }

    @Test
    public void testAddHandlerFails() {
        expectSpecificBuildException("test-addhandler-fails", "NullPointException", null);
    }

}
