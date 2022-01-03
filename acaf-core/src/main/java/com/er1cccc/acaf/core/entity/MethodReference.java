package com.er1cccc.acaf.core.entity;

import com.er1cccc.acaf.data.DataFactory;

import java.util.Objects;

public class MethodReference {
    private final ClassReference.Handle classReference;
    private final String name;
    private final String desc;
    private final boolean isStatic;

    public MethodReference(ClassReference.Handle classReference,
                           String name, String desc, boolean isStatic) {
        this.classReference = classReference;
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
    }

    public ClassReference.Handle getClassReference() {
        return classReference;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Handle getHandle() {
        return new Handle(classReference, name, desc);
    }

    public static class Handle {
        private final ClassReference.Handle classReference;
        private final String name;
        private final String desc;

        public Handle(ClassReference.Handle classReference, String name, String desc) {
            this.classReference = classReference;
            this.name = name;
            this.desc = desc;
        }

        public ClassReference.Handle getClassReference() {
            return classReference;
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle handle = (Handle) o;
            if (!Objects.equals(classReference, handle.classReference))
                return false;
            if (!Objects.equals(name, handle.name))
                return false;
            return Objects.equals(desc, handle.desc);
        }

        @Override
        public int hashCode() {
            int result = classReference != null ? classReference.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (desc != null ? desc.hashCode() : 0);
            return result;
        }
    }

    public static class Factory implements DataFactory<MethodReference> {

        @Override
        public MethodReference parse(String[] fields) {
            return new MethodReference(
                    new ClassReference.Handle(fields[0]),
                    fields[1],
                    fields[2],
                    Boolean.parseBoolean(fields[3]));
        }

        @Override
        public String[] serialize(MethodReference obj) {
            return new String[] {
                    obj.classReference.getName(),
                    obj.name,
                    obj.desc,
                    Boolean.toString(obj.isStatic),
            };
        }
    }

}
