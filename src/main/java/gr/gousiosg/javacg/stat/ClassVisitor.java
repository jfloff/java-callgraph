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

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import java.util.HashMap;
import java.util.HashSet;


/**
 * The simplest of class visitors, invokes the method visitor class for each
 * method found.
 */
class ClassVisitor extends EmptyVisitor {

  private String jarpath;
  private JavaClass clazz;
  private ConstantPoolGen constants;
  private String classReferenceFormat;
  private HashMap<String, MethodVisitor> methods = new HashMap<>();
  private HashMap<String, MethodVisitor> parentMethods = new HashMap<>();
  private HashSet<ClassVisitor> parents = new HashSet<>();

  public ClassVisitor(JavaClass jc, String arg) {
    jarpath = arg;
    clazz = jc;
    constants = new ConstantPoolGen(clazz.getConstantPool());
    classReferenceFormat = "C:" + clazz.getClassName() + " %s";
  }

  public String getClassName() {
    return clazz.getClassName();
  }

  public JavaClass getClazz() {
    return clazz;
  }

  public void visitJavaClass(JavaClass jc) {
    jc.getConstantPool().accept(this);
    Method[] methods = jc.getMethods();
    for (int i = 0; i < methods.length; i++){
      methods[i].accept(this);
    }
  }

  public void visitConstantPool(ConstantPool constantPool) {
    for (int i = 0; i < constantPool.getLength(); i++) {
      Constant constant = constantPool.getConstant(i);
      if (constant == null){
        continue;
      }

      if (constant.getTag() == 7) {
        String referencedClass = constantPool.constantToString(constant);
        // System.out.println(String.format(classReferenceFormat, referencedClass));
      }
    }
  }

  private MethodVisitor getParentMethod(String name) {
    MethodVisitor mv = null;
    for (ClassVisitor cv : parents){
      if(cv.methods.containsKey(name)){
        mv = cv.getParentMethod(name);
        if (mv == null) {
          mv = cv.methods.get(name);
        }
      }
    }
    return mv;
  }

  public boolean hasParent(String methodName) {
    return parentMethods.containsKey(methodName);
  }

  public MethodVisitor getParent(String methodName) {
    return parentMethods.get(methodName);
  }

  public void visitMethod(Method method) {
    MethodGen mg = new MethodGen(method, clazz.getClassName(), constants);
    MethodVisitor visitor = new MethodVisitor(mg, this);
    String methodName = visitor.getSignature();
    MethodVisitor parent = getParentMethod(methodName);
    if (parent != null) {
      parentMethods.put(methodName, parent);
    }
    methods.put(methodName, visitor);
    visitor.start();
  }

  public void start() {
    for (String iname : clazz.getInterfaceNames()) {
      ClassVisitor cv = JCallGraph.getClassVisitor(iname, jarpath);
      if (cv != null) {
        parents.add(cv);
      }
    }
    String sname = clazz.getSuperclassName();
    if(sname != null && !sname.isEmpty()) {
      ClassVisitor cv = JCallGraph.getClassVisitor(sname, jarpath);
      if (cv != null) {
        parents.add(cv);
      }
    }
    visitJavaClass(clazz);
  }
}
