/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist.compiler.ast;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;
import javassist.compiler.TokenId;
import javassist.compiler.CompileError;

/**
 * Variable declarator.
 */
public class Declarator extends ASTList implements TokenId {
    protected int varType;
    protected int arrayDim;
    protected int localVar;
    protected String qualifiedClass;    // JVM-internal representation
    protected SignatureAttribute.ClassSignature javaTypeSig;

    public Declarator(int type, int dim) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = null;
    }

    public Declarator(ASTList className, int dim) {
        super(null);
        varType = CLASS;
        arrayDim = dim;
        localVar = -1;
        qualifiedClass = astToClassName(className, '/');
    }

    public Declarator(String javaType) throws CompileError {
        super(null);
        varType = CLASS;
        localVar = -1;
        arrayDim = 0;
        try {
            javaTypeSig = SignatureAttribute.toClassSignature(javaType);
        } catch (BadBytecode e) {
            throw new CompileError(e.toString());
        }
        qualifiedClass = javaTypeSig.getSuperClass().getName().replace('.','/');

    }

    public boolean isGeneric() {
        if(javaTypeSig != null) {
            return javaTypeSig.getSuperClass().getTypeArguments() != null;
        }
        return false;
    }

    /* For declaring a pre-defined? local variable.
     */
    public Declarator(int type, String jvmClassName, int dim,
                      int var, Symbol sym) {
        super(null);
        varType = type;
        arrayDim = dim;
        localVar = var;
        qualifiedClass = jvmClassName;
        setLeft(sym);
        append(this, null);     // initializer
    }

    public Declarator make(Symbol sym, int dim, ASTree init) {
        Declarator d = new Declarator(this.varType, this.arrayDim + dim);
        d.qualifiedClass = this.qualifiedClass;
        d.setLeft(sym);
        append(d, init);
        return d;
    }

    /* Returns CLASS, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT,
     * or DOUBLE (or VOID)
     */
    public int getType() { return varType; }

    public int getArrayDim() { return arrayDim; }

    public void addArrayDim(int d) { arrayDim += d; }

    public String getClassName() {
        // this has to return the class name
        return qualifiedClass;
    }

    public String getTypeSig() {
        if(javaTypeSig != null) {
            return javaTypeSig.encode();
        } else {
            // this has got to be wrong, as what if there is an array.
            return "L"+getClassName()+";";
        }
    }

    public void setClassName(String s) { qualifiedClass = s; }

    public Symbol getVariable() { return (Symbol)getLeft(); }

    public void setVariable(Symbol sym) { setLeft(sym); }

    public ASTree getInitializer() {
        ASTList t = tail();
        if (t != null)
            return t.head();
        else
            return null;
    }

    public void setLocalVar(int n) { localVar = n; }

    public int getLocalVar() { return localVar; }

    public String getTag() { return "decl"; }

    public void accept(Visitor v) throws CompileError {
        v.atDeclarator(this);
    }

    public static String astToClassName(ASTList name, char sep) {
        if (name == null)
            return null;

        StringBuffer sbuf = new StringBuffer();
        astToClassName(sbuf, name, sep);
        return sbuf.toString();
    }

    private static void astToClassName(StringBuffer sbuf, ASTList name,
                                       char sep) {
        for (;;) {
            ASTree h = name.head();
            if (h instanceof Symbol)
                sbuf.append(((Symbol)h).get());
            else if (h instanceof ASTList)
                astToClassName(sbuf, (ASTList)h, sep);

            name = name.tail();
            if (name == null)
                break;

            sbuf.append(sep);
        }
    }
}
