package havis.tools.jaxb;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.util.DOMUtils;

public class ToStringPlugin extends BasePlugin {

	static final String NAME = "toString";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getUsage() {
		return "  -XtoString            :  adds toString";
	}

	boolean addFields(JCodeModel model, Set<String> set, JDefinedClass clazz, JBlock block, JVar var, boolean first) {
		for (JFieldVar field : clazz.fields().values()) {
			if (set.isEmpty() || set.contains(field.name())) {
				block.add(var.invoke("append").arg((first ? "" : ", ") + JExpr.quotify('"', field.name()) + ": "));
				first = false;
				if (field.type().isReference()) {
					JClass type = (JClass) field.type();
					if (model.ref(List.class).narrow(type.getTypeParameters()).equals(type)) {
						// handle list
						JConditional condition = block._if(field.ne(JExpr._null()));
						JBlock _block = condition._then();
						_block.add(var.invoke("append").arg(JExpr.lit('[')));
						JVar _var = _block.decl(model.BOOLEAN, "b", JExpr.FALSE);
						for (JClass _type : type.getTypeParameters()) {
							JBlock __block = _block.forEach(_type, "entry", field).body();
							JConditional _condition = __block._if(_var);
							_condition._then().invoke(var, "append").arg(", ");
							_condition._else().assign(_var, JExpr.TRUE);
							if (_type.compareTo(model.ref(String.class)) == 0) {
								__block.invoke(var, "append").arg(JExpr.lit('"'));
								__block.invoke(var, "append").arg(JExpr.ref("entry"));
								__block.invoke(var, "append").arg(JExpr.lit('"'));
							} else {
								__block.invoke(var, "append").arg(JExpr.ref("entry"));
							}
							continue;
						}
						_block.add(var.invoke("append").arg(JExpr.lit(']')));
						condition._else().add(var.invoke("append").arg("null"));
						continue;
					} else {
						if (type.compareTo(model.ref(String.class)) == 0) {
							block.invoke(var, "append").arg(JExpr.lit('"'));
							block.invoke(var, "append").arg(field);
							block.invoke(var, "append").arg(JExpr.lit('"'));
							continue;
						}
					}
				}
				block.add(var.invoke("append").arg(field));
			}
		}
		return false;
	}

	@Override
	protected void run(ClassOutline classOutline, Element element) {
		JCodeModel model = new JCodeModel();

		Set<String> set = new HashSet<>();
		String params = DOMUtils.getElementText(element);
		if (params != null)
			set.addAll(Arrays.asList(params.split("\\s*,\\s*(?![^<>]*>)")));

		JDefinedClass clazz = classOutline.implClass;

		// create method
		JMethod method = classOutline.implClass.method(JMod.PUBLIC, String.class, "toString");

		JBlock block = method.body();

		JType type = model._ref(StringBuilder.class);
		JVar var = block.decl(type, "s", JExpr._new(type));
		block.invoke(var, "append").arg(JExpr.lit('{'));
		boolean first = true;

		while (clazz != null) {
			first = addFields(model, set, clazz, block, var, first);
			if (classOutline.getSuperClass() == null || clazz.equals(classOutline.getSuperClass().implClass))
				break;
			clazz = classOutline.getSuperClass().implClass;
		}
		block.invoke(var, "append").arg(JExpr.lit('}'));
		method.body()._return(var.invoke("toString"));
	}
}