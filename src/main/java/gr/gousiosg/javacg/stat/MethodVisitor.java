/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.EmptyVisitor;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * The simplest of method visitors, prints any invoked method
 * signature for all method invocations.
 *
 * Class copied with modifications from CJKM: http://www.spinellis.gr/sw/ckjm/
 */
public class MethodVisitor extends EmptyVisitor {

  private ClassVisitor visited;
  private JavaClass visitedClass;
  private MethodGen mg;
  private MethodGen parentmg;
  private ConstantPoolGen cp;
  private String format;
  private String signature;
  private MethodVisitor parent;
  private String vertex;

  public MethodVisitor(MethodGen m, ClassVisitor v) {
    visited = v;
    visitedClass = v.getClazz();
    mg = m;
    signature = buildSignature(m.toString());
    cp = mg.getConstantPool();
    vertex = visitedClass.getClassName() + ":" + mg.getName();
    format = "M:" + visitedClass.getClassName() + ":" + mg.getName() + " " + "(%s)%s:%s";
  }

  private String buildSignature(String orig) {
    String[] nameParts = orig.split(" ");
    for (int i=0; i < nameParts.length; i++){
      if (nameParts[i].equals("abstract")){
        nameParts[i] = "";
      }
      if (nameParts[i].contains(",")) {
        nameParts[i] = "";
        nameParts[i-1] += ",";
      }
      if (nameParts[i].contains(")") && !nameParts[i].contains("(")) {
        nameParts[i] = "";
        nameParts[i-1] += ")";
      }
    }
    return Utils.strJoin(nameParts, " ");
  }

  public String getSignature() {
    return signature;
  }

  public void start() {
    if (visited.hasParent(signature)){
      parent = visited.getParent(signature);
      format = "M:" + parent.visitedClass.getClassName() + ":" + parent.mg.getName() + " " + "(%s)%s:%s";
      vertex = parent.visitedClass.getClassName() + ":" + parent.mg.getName();
    }

    // add to graph first one
    JCallGraph.graph.addVertex(vertex);

    if (mg.isAbstract() || mg.isNative())
        return;
    for (InstructionHandle ih = mg.getInstructionList().getStart();
            ih != null; ih = ih.getNext()) {
      Instruction i = ih.getInstruction();

      if (!visitInstruction(i)){
        i.accept(this);
      }
    }
  }

  private boolean visitInstruction(Instruction i) {
    short opcode = i.getOpcode();

    return ((InstructionConstants.INSTRUCTIONS[opcode] != null)
            && !(i instanceof ConstantPushInstruction)
            && !(i instanceof ReturnInstruction));
  }

  private void addCall(InvokeInstruction i) {
    // add the vertices
    String targetVertex = i.getReferenceType(cp) + ":" + i.getMethodName(cp);
    JCallGraph.graph.addVertex(targetVertex);
    JCallGraph.graph.addEdge(vertex, targetVertex);
  }

  @Override
  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
    addCall(i);
    // System.out.println(String.format(format,"M",i.getReferenceType(cp),i.getMethodName(cp)));
  }

  @Override
  public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
    addCall(i);
    // System.out.println(String.format(format,"I",i.getReferenceType(cp),i.getMethodName(cp)));
  }

  @Override
  public void visitINVOKESPECIAL(INVOKESPECIAL i) {
    addCall(i);
    // System.out.println(String.format(format,"O",i.getReferenceType(cp),i.getMethodName(cp)));
  }

  @Override
  public void visitINVOKESTATIC(INVOKESTATIC i) {
    addCall(i);
    // System.out.println(String.format(format,"S",i.getReferenceType(cp),i.getMethodName(cp)));
  }
}
