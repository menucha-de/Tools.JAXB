package havis.tools.jaxb;

import org.w3c.dom.Element;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.util.DOMUtils;

public class InterfacePlugin extends BasePlugin {

	static final String NAME = "interface";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getUsage() {
		return "  -Xinterface        :  adds interfaces";
	}

	@Override
	protected void run(ClassOutline classOutline, Element element) {
		String params = DOMUtils.getElementText(element);
		JCodeModel jCodeModel = new JCodeModel();
		for (String param : params.split("\\s*,\\s*(?![^<>]*>)")) {
			try {
				JType type = jCodeModel
						.parseType(param.trim().replace(" ", ""));
				if (type instanceof JClass) {
					JClass jClass = (JClass) type;
					jCodeModel.ref(jClass.erasure().fullName());
					classOutline.implClass._implements(jClass);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}