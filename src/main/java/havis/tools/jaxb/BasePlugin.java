package havis.tools.jaxb;

import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

public abstract class BasePlugin extends Plugin {

	private static final String NS = "urn:havis:tools:jaxb:";

	protected abstract String getName();

	protected String getNS() {
		return NS + getName();
	}

	@Override
	public String getOptionName() {
		return "X" + getName();
	}

	@Override
	public List<String> getCustomizationURIs() {
		return Collections.singletonList(getNS());
	}

	@Override
	public boolean isCustomizationTagName(String nsUri, String localName) {
		return nsUri.equals(getNS()) && localName.equals(getName());
	}

	protected abstract void run(ClassOutline classOutline, Element element);

	@Override
	public boolean run(Outline outline, Options opt, ErrorHandler errorHandler)
			throws SAXException {
		for (ClassOutline classOutline : outline.getClasses()) {
			CPluginCustomization customization = classOutline.target
					.getCustomizations().find(getNS(), getName());
			if (customization != null) {
				customization.markAsAcknowledged();
				run(classOutline, customization.element);
			}
		}

		return true;
	}
}