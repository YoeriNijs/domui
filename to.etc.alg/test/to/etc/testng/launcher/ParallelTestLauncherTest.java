package to.etc.testng.launcher;

import java.io.*;

import org.junit.*;

import to.etc.testng.launcher.misc.*;

/**
 * Tests (and experiment area) for running ParallelTestLauncher tool.
 *
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Aug 1, 2013
 */
public class ParallelTestLauncherTest {
	/*Keep this as ignored - it is not meant to be run as regular scheduled unit test, but as debug launcher until for developers -see xwiki*/
	@Ignore
	@Test
	public void testClassLauncher() throws Exception {
		String[] args = new String[]{ //
		"-root", "/home/vmijic/Data/Projects/Itris/Viewpoint/bzr/vp-split-5.0/vp-5.0", //
			"-m2.repo", "/home/vmijic/.m2", //
			"-project", "vp-selenium-webdriver-tests", //
			//"-skip.package", "nl.itris.vp.webdriver.vp_old_do_not_use", "nl.itris.vp.webdriver.viewpoint.tests.fin", //
			"-include.package", "nl.itris.vp.webdriver.viewpoint.tests.bae", "nl.itris.vp.webdriver.decade", //
			"-parallel", "CLASS", //
			"-threads", "5", //
			"-testng.reporter", "nl.itris.vp.webdriver.core.report.WdVpTestXMLReporter", //
			"-testng.reporter.root", "/home/vmijic/Data/Projects/Itris/Viewpoint/testing/tests/local/parallel", //
			"-testng.browser", "firefox", //
			"-testng.remote.hub", "local", //
			"-testng.server.url", "http://localhost:8080/Itris_VO02/", //
			"-testng.username", "vpc", //
			"-testng.password", "rhijnspoor", //
			"-remove.generated", "false", //
		//"-testProperties", "wd44.properties", //
		};

		new ParallelTestLauncher().run(args);
	}

	@Test
	public void testHelp() throws Exception {
		String[] args = new String[]{ //
		"-help", //
		};

		new ParallelTestLauncher().run(args);
	}

	//@Test
	public void reportFixer() throws Exception {
		ReportFixer r = new ReportFixer();
		r.assambleSingleReports(new File("/home/vmijic/parallel201307311250/"));
	}

	/*Keep this as ignored - it is not meant to be run as regular scheduled unit test, but as debug launcher until for developers -see xwiki*/
	@Ignore
	@Test
	public void testSuiteLauncher() throws Exception {
		String[] args = new String[]{ //
		"-root", "/home/vmijic/Data/Projects/Itris/Viewpoint/bzr/vp-split-5.0/vp-5.0", //
			"-m2.repo", "/home/vmijic/.m2", //
			"-project", "vp-selenium-webdriver-tests", //
			//"-suiteFiles", "vp-selenium-webdriver-tests/src/nl/itris/vp/webdriver/viewpoint/wdParallelSuiteVp.xml", //
			"-suiteFiles", "vp-selenium-webdriver-tests/src/nl/itris/vp/webdriver/viewpoint/wdSuiteDecade.xml", //
			"-threads", "5", //
			"-testng.reporter", "nl.itris.vp.webdriver.core.report.WdVpTestXMLReporter", //
			"-testng.reporter.root", "/home/vmijic/Data/Projects/Itris/Viewpoint/testing/tests/proba/vp-suite", //
			"-testng.browser", "firefox", //
			//"-testng.remote.hub", "http://192.168.0.118:4444/wd/hub",
			"-testng.remote.hub", "local", //
			"-testng.server.url", "http://ws061.execom.co.yu:8080/Itris_VO02/", //
			"-testng.username", "vpc", //
			"-testng.password", "rhijnspoor", //
		//"-testProperties", "wd44.properties", //
		};

		new ParallelTestLauncher().run(args);
	}

	/*Keep this as ignored - it is not meant to be run as regular scheduled unit test, but as debug launcher until for developers -see xwiki http://wiki.hosts.itris.nl/xwiki/bin/view/Ontwikkeling/WebDriverParallelLauncher */
	@Ignore
	@Test
	public void testFileOptionsLauncher() throws Exception {
		String[] args = new String[]{ //
		"-options.file", this.getClass().getResource("runLocalTip50.par").getFile(), "-threads", "4"};

		new ParallelTestLauncher().run(args);
	}


}
