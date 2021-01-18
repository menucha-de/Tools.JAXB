package havis.tools.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.util.DOMUtils;

public class EqualsPlugin extends BasePlugin {

	static final String NAME = "equals";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getUsage() {
		return "  -Xequals            :  adds equals and hashCode";
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

	void addEquals(JCodeModel model, JDefinedClass clazz, Set<String> set) {
		// create method
		JMethod method = clazz.method(JMod.PUBLIC, model.BOOLEAN, "equals");
		method.annotate(Override.class);
		method.param(Object.class, "obj");
		JBlock block = method.body();
		JFieldRef obj = JExpr.ref("obj");
		// references are equal
		block._if(JExpr._this().eq(obj))._then()._return(JExpr.TRUE);

		// reference not same instance
		block._if(obj._instanceof(clazz).not())._then()._return(JExpr.FALSE);

		JVar var = block.decl(clazz, "other", JExpr.cast(clazz, obj));
		
		for (JFieldVar field : clazz.fields().values()) {
			if (set.isEmpty() || set.contains(field.name())) {
				if (field.type().isReference()) {
					JConditional condition = block._if(field.eq(JExpr._null()));
					condition._then()._if(var.ref(field.name()).ne(JExpr._null()))._then()._return(JExpr.FALSE);
					condition._elseif(field.invoke("equals").arg(var.ref(field.name())).not())._then()._return(JExpr.FALSE);
				} else {
					block._if(field.ne(var.ref(field.name())))._then()._return(JExpr.FALSE);
				}
			}
		}
		method.body()._return(JExpr.TRUE);
	}

	void addHashCode(JCodeModel model, JDefinedClass clazz, Set<String> set) {
		// create method
		JMethod method = clazz.method(JMod.PUBLIC, model.INT, "hashCode");
		method.annotate(Override.class);
		JBlock block = method.body();
		JVar var = block.decl(model.INT, "result", JExpr.lit(1));
		for (JFieldVar field : clazz.fields().values()) {
			if (set.isEmpty() || set.contains(field.name())) {
				JExpression expr;
				if (field.type().isReference()) {
					expr = JOp.cond(field.eq(JExpr._null()), JExpr.lit(0), JExpr.invoke(field, "hashCode"));
				} else {
					expr = field;
				}
				block.assign(var, JExpr.lit(31).mul(var).plus(expr));
			}
		}
		method.body()._return(var);

	}

	@Override
	protected void run(ClassOutline classOutline, Element element) {
		JCodeModel model = new JCodeModel();

		Set<String> set = new LinkedHashSet<>();
		String params = DOMUtils.getElementText(element);
		if (params != null)
			set.addAll(Arrays.asList(params.split("\\s*,\\s*(?![^<>]*>)")));

		JDefinedClass clazz = classOutline.implClass;

		addHashCode(model, clazz, set);
		addEquals(model, clazz, set);
	}
}