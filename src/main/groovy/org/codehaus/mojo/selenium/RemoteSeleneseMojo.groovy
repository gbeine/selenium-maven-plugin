/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License") you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.codehaus.mojo.selenium

import de.gbeine.selenium.results.HtmlResult;

import java.io.FileWriter;

import de.gbeine.selenium.HtmlRemoteCommandProcessor
import de.gbeine.selenium.HtmlRemoteClient
import de.gbeine.selenium.TestSuite
import de.gbeine.selenium.TestSuiteRunner
import org.apache.maven.project.MavenProject
import org.codehaus.gmaven.mojo.GroovyMojo

/**
 * Run a suite of HTML Selenese tests using the remote client.
 *
 * @goal remoteSelenese
 * @since 1.0-beta-2
 *
 * @author <a href="mailto:gerrit.beine@gmx.de">Gerrit Beine</a>
 */
class RemoteSeleneseMojo
    extends GroovyMojo
{
    /**
     * The suite file to run.
     *
     * @parameter
     * @required
     */
    File suite
    
    /**
     * The browser name to run; must be one of the standard valid browser names
     * (and must start with a *), e.g. *firefox, *iexplore, *custom.
     *
     * @parameter
     * @required
     */
    String browser
    
    /**
     * The base URL on which the tests will be run, e.g. http://www.google.com.
     * Note that only the hostname part of this URL will really be used.
     *
     * @parameter
     * @required
     */
    URL startURL

    // TODO: ^^ Rename to browserURL to match HTMLLauncher?
    
    /**
     * The host name where Selenium Server is running.
     *
     * @parameter
     * @required
     */
    String host

    /**
     * The file to which we'll write out our test results.
     *
     * @parameter
     */
    File results
    
    /**
     * The port on which the Selenium Server is listening.
     *
     * @parameter default-value="4444"
     */
    int port
    
    /**
     * Amount of time to wait before we just kill the browser.
     *
     * @parameter default-value="1800"
     */
    int timeoutInSeconds
    
    /**
     * True if the application under test should run in its own window, false if
     * the AUT will run in an embedded iframe.
     *
     * @parameter default-value="false"
     */
    boolean multiWindow
    
    /**
     * A debugging tool that slows down the Selenium Server. (Selenium developers only)
     *
     * @parameter default-value="false"
     */
    boolean slowResources
    
    /**
     * Allows the tests to be skipped.
     *
     * @parameter expression="${maven.test.skip}" default-value="false"
     */
    boolean skip
    
    //
    // Components
    //
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    MavenProject project

    //
    // Mojo
    //

    void execute() {
        if (skip) {
            log.info('Skipping tests')
            return
        }
        
        // Setup the default results file if not specified
        if (!results) {
            String options = (multiWindow ? 'multiWindow-' : '') + (slowResources ? 'slowResources-' : '')
            String name = 'results-' + extractUsableBrowserName() + '-' + options + suite.name
            results = new File(project.build.directory, name)
            log.info("Results will go to: ${results.absolutePath}")
        }
        
        ant.mkdir(dir: results.parentFile)

		def runner = new TestSuiteRunner()
		def testSuite = new TestSuite(suite.absolutePath)
		def commandProcessor = new HtmlRemoteCommandProcessor(host, port, browser, startURL.toString())
		def client = new HtmlRemoteClient(commandProcessor)
		
		commandProcessor.start();
		def result = runner.run(testSuite, client);
		commandProcessor.stop();
		
		new HtmlResult().writeResults(result, new FileWriter(results));
		
		def status = result.getStatus().toString().toUpperCase(); 
		
        if (status != 'PASSED') {
            fail("Tests failed with status ${status}, see result file for details: ${results.absolutePath}")
        }
    }
    
    private String extractUsableBrowserName() {
        def m = browser =~ /\*(\w+)/
        if (m.find()) {
            return m.group(1)
        }
        fail("Could not parse browser string: $browser")
    }
}
