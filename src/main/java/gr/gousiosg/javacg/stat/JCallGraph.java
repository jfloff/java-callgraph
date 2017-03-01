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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.bcel.classfile.ClassParser;
import java.util.HashMap;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 *
 */
public class JCallGraph {

  private static HashMap<String,ClassVisitor> classes = new HashMap<>();
  public static DirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

  public static ClassVisitor getClassVisitor(String className, String path) {
    // not found visit
    if (!classes.containsKey(path+className)){
      visitClass(className, path);
    }
    return classes.get(path+className);
  }

  private static void visitClass(String name, String arg) {
    if (!name.endsWith(".class")){
      return;
    }
    if (classes.containsKey(name)){
      return;
    }

    try {
      ClassParser cp = new ClassParser(arg, name);
      ClassVisitor visitor = new ClassVisitor(cp.parse(), arg);
      classes.put(arg+visitor.getClassName(), visitor);
      visitor.start();
    } catch (IOException e) {
      System.err.println("Error while processing jar: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      for (String arg : args) {
        arg += '/';
        File f = new File(arg);

        if (!f.exists()) {
          System.err.println("Jar file " + arg + " does not exist");
        }

        JarFile jar = new JarFile(f);

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory()){
            continue;
          }

          visitClass(entry.getName(), arg);
        }
      }
    } catch (IOException e) {
      System.err.println("Error while processing jar: " + e.getMessage());
      e.printStackTrace();
    }

    HashSet<String> visited = new HashSet<>();
    Queue<String> targets = new LinkedList<String>();
    targets.add("io.grpc.MethodDescriptor$Marshaller:stream");

    while (!targets.isEmpty()) {
      String v = targets.remove();
      // skips already visited nodes to avoid infinite loops
      if(!visited.contains(v)){
        visited.add(v);
        for(DefaultEdge e : graph.incomingEdgesOf(v)){
          targets.add(graph.getEdgeSource(e));
          System.out.println(graph.getEdgeSource(e) + " --> " + graph.getEdgeTarget(e));
        }
      }
    }
  }
}
