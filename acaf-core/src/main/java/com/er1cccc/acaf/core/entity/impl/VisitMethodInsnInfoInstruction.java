package com.er1cccc.acaf.core.entity.impl;

import com.er1cccc.acaf.core.entity.Instruction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisitMethodInsnInfoInstruction implements Instruction {
    int opcode;
    String owner;
    String name;
    String descriptor;
    boolean isInterface;
    Set<Integer> controllableArgIndex;
}
