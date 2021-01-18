package havis.tools.jaxb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.util.DOMUtils;

public class ConstructorPlugin extends BasePlugin {

	Pattern pattern = Pattern.compile("(?<" + ARGS + ">[^\\{\\}]*)\\s*(\\{(?<"
			+ CODE + ">.*)\\})?\\s*");

	private static final String NAME = "constructor";
	private static final String ARGS = "args";
	private static final String CODE = "code";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getUsage() {
		return "  -Xconstructor      :  adds constructor";
	}

	@Override
	protected void run(ClassOutline classOutline, Element element) {
		String params = DOMUtils.getElementText(element);
		JCodeModel jCodeModel = new JCodeModel();
		if (element.getAttribute("default").equals("true"))
			classOutline.implClass.constructor(JMod.PUBLIC);
		for (String param : params.split("\\s*;\\s*(?![^{}]*})")) {
			Matcher matcher = pattern.matcher(param);
			if (matcher.matches()) {
				try {
					String code = matcher.group(CODE);
					JMethod method = classOutline.implClass
							.constructor(JMod.PUBLIC);
					JBlock block = method.body();
					String args = matcher.group(ARGS).trim();
					for (String p : args.split("\\s*,\\s*(?![^<>]*>)")) {
						String[] s = p.trim().split("\\s* \\s*(?![^<>]*>)", 2);
						if (s.length == 2) {
							JType type = jCodeModel.parseType(s[0].trim()
									.replace(" ", ""));
							if (type instanceof JClass
									&& !((JClass) type)._package().name()
											.isEmpty()) {
								jCodeModel
										.ref(type.erasure().fullName());
								type = jCodeModel.parseType(type.fullName());
							}
							String name = s[1].trim();
							JVar var = method.param(type, name);
							if (code == null)
								block.assign(JExpr._this().ref(name), var);
						}
					}

					if (code != null) {
						block.directStatement(code);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}
}