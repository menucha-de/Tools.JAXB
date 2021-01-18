package havis.tools.jaxb;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.junit.Test;

import com.sun.tools.xjc.XJC2Task;

public class PluginTest {

	@Test
	public void test() {
		XJC2Task task = new XJC2Task();
		task.setSchema("file:src/test/resources/Test.xsd");
		File target = new File("target/src");
		target.mkdir();
		task.setDestdir(new File("target/src"));
		task.setPackage("havis.tools.jaxb");
		task.setExtension(true);
		task.createClasspath().createPath().setPath("src/main/java");
		task.createArg().setValue("-npa");
		task.createArg().setValue("-verbose");
		task.createArg().setValue("-Xinterface");
		task.createArg().setValue("-Xconstructor");
		task.createArg().setValue("-Xclone");
		task.createArg().setValue("-Xequals");
		task.createArg().setValue("-XtoString");
		task.setBinding("file:src/test/resources/binding.xml");
		try {
			task.execute();
		} catch (BuildException e) {
			e.printStackTrace();
		}
	}

}
