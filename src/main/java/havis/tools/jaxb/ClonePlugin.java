package havis.tools.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.util.DOMUtils;

public class ClonePlugin extends BasePlugin {

	static final String NAME = "clone";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getUsage() {
		return "  -Xclone            :  adds cloneable";
	}

	void addFields(JCodeModel model, Set<String> set, JDefinedClass clazz, JBlock block, JVar var) {
		for (JFieldVar field : clazz.fields().values()) {
			JFieldRef ref = JExpr.ref(var, field);
			if (field.type().isReference()) {
				JClass type = (JClass) field.type();
				if (model.ref(List.class).narrow(type.getTypeParameters()).equals(type)) {
					// handle list
					JBlock _block = block._if(field.ne(JExpr._null()))._then();
					JInvocation invocation = JExpr._new(model.ref(ArrayList.class).narrow(type.getTypeParameters()));
					if (set.contains(field.name())) {
						// deep clone entries
						_block.assign(ref, invocation);
						for (JClass _type : type.getTypeParameters()) {
							_block.forEach(_type, "entry", field).body().invoke(ref, "add").arg(JExpr.cast(_type, JExpr.invoke(JExpr.ref("entry"), "clone")));
							continue;
						}
					} else {
						_block.assign(ref, invocation.arg(field));

					}
					continue;
				} else {
					if (set.contains(field.name())) {
						block._if(field.ne(JExpr._null()))._then().assign(ref, JExpr.cast(type, JExpr.invoke(field, "clone")));
						continue;
					}
				}
			}
			block.assign(ref, field);
		}
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
		JMethod method = classOutline.implClass.method(JMod.PUBLIC, clazz, "clone");

		JBlock block = method.body();
		JVar var = block.decl(clazz, "clone", JExpr._new(clazz));

		while (clazz != null) {
			addFields(model, set, clazz, block, var);
			if (classOutline.getSuperClass() == null || clazz.equals(classOutline.getSuperClass().implClass))
				break;
			clazz = classOutline.getSuperClass().implClass;
		}
		method.body()._return(var);
	}
}