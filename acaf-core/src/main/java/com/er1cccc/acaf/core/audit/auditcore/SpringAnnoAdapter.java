package com.er1cccc.acaf.core.audit.auditcore;

import com.er1cccc.acaf.core.entity.SpringParam;
import org.objectweb.asm.AnnotationVisitor;

import java.util.List;

public class SpringAnnoAdapter extends AnnotationVisitor {
    private final List<SpringParam> params;
    private final int index;

    public SpringAnnoAdapter(int api, AnnotationVisitor annotationVisitor,
                             List<SpringParam> params, int parameter) {
        super(api, annotationVisitor);
        this.index = parameter;
        this.params = params;
    }

    @Override
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            SpringParam param = this.params.get(this.index);
            param.setReqName(value.toString());
            param.setParamIndex(this.index);
            this.params.set(this.index,param);
        }
        super.visit(name, value);
    }
}
