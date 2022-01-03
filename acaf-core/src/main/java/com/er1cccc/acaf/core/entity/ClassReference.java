package com.er1cccc.acaf.core.entity;

import com.er1cccc.acaf.data.DataFactory;

import java.util.*;

public class ClassReference {
    private final String name;
    private final String superClass;
    private final List<String> interfaces;
    private final boolean isInterface;
    private final List<Member> members;
    private final Set<String> annotations;

    public static class Member {
        private final String name;
        private final int modifiers;
        private final Handle type;

        public Member(String name, int modifiers, Handle type) {
            this.name = name;
            this.modifiers = modifiers;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public int getModifiers() {
            return modifiers;
        }

        public Handle getType() {
            return type;
        }
    }

    public ClassReference(String name, String superClass, List<String> interfaces,
                          boolean isInterface, List<Member> members, Set<String> annotations) {
        this.name = name;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.isInterface = isInterface;
        this.members = members;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public String getSuperClass() {
        return superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public List<Member> getMembers() {
        return members;
    }

    public Handle getHandle() {
        return new Handle(name);
    }

    public Set<String> getAnnotations() {
        return annotations;
    }

    public static class Handle {
        private final String name;

        public Handle(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Handle handle = (Handle) o;
            return Objects.equals(name, handle.name);
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class Factory implements DataFactory<ClassReference> {

        @Override
        public ClassReference parse(String[] fields) {
            String[] interfaces;
            if (fields[2].equals("")) {
                interfaces = new String[0];
            } else {
                interfaces = fields[2].split(",");
            }

            String[] memberEntries = fields[4].split("!");
            int size=memberEntries.length/3;
            List<Member> members = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                members.add(new Member(memberEntries[3*i], Integer.parseInt(memberEntries[3*i+1]),
                        new ClassReference.Handle(memberEntries[3*i+2])));
            }

            String[] annotationEntries = fields[5].split("!");
            Set<String> annotations = new HashSet<>();
            for (int i = 0; i < annotationEntries.length; i++) {
                if(annotationEntries[i]!=null&&!annotationEntries[i].trim().equals(""))
                annotations.add(annotationEntries[i]);
            }

            return new ClassReference(
                    fields[0],
                    fields[1].equals("") ? null : fields[1],
                    new ArrayList<>(Arrays.asList(interfaces)),
                    Boolean.parseBoolean(fields[3]),
                    members,
                    annotations);
        }

        @Override
        public String[] serialize(ClassReference obj) {
            String interfaces;
            if (obj.interfaces.size() > 0) {
                StringBuilder interfacesSb = new StringBuilder();
                for (String iface : obj.interfaces) {
                    interfacesSb.append(",").append(iface);
                }
                interfaces = interfacesSb.substring(1);
            } else {
                interfaces = "";
            }

            StringBuilder members = new StringBuilder();
            for (Member member : obj.members) {
                members.append("!").append(member.getName())
                        .append("!").append(Integer.toString(member.getModifiers()))
                        .append("!").append(member.getType().getName());
            }

            StringBuilder annotations = new StringBuilder();
            for (String annotation : obj.annotations) {
                annotations.append("!").append(annotation);
            }

            return new String[]{
                    obj.name,
                    obj.superClass,
                    interfaces,
                    Boolean.toString(obj.isInterface),
                    members.length() == 0 ? null : members.substring(1),
                    annotations.length()==0? null: annotations.substring(1)
            };
        }
    }

}
