package com.er1cccc.acaf.core.resolver.impl;

import com.er1cccc.acaf.core.entity.Instruction;
import com.er1cccc.acaf.core.entity.impl.VisitMethodInsnInfoInstruction;
import com.er1cccc.acaf.core.resolver.Resolver;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
public class VisitMethodInsnInfoResolver implements Resolver {
    int opcode;
    String owner;
    String name;
    String descriptor;
    boolean isInterface;
    Set<Integer> controllableArgIndex;

    @Override
    public boolean resolve(Instruction instruction) {
        VisitMethodInsnInfoInstruction inst= (VisitMethodInsnInfoInstruction) instruction;
        return owner.equals(inst.getOwner()) &&
                name.equals(inst.getName()) &&
                descriptor.equals(inst.getDescriptor())&&
                Sets.difference(controllableArgIndex, inst.getControllableArgIndex()).size()==0;

    }

}
