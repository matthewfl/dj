package edu.berkeley.dj.internal.coreclazz.sun.misc;

/**
 * Created by matthewfl
 */

/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */



/*
 * This entire class has to be rewritten in the case that the object is remote.
 */


import edu.berkeley.dj.internal.BridgeGetNative;
import edu.berkeley.dj.internal.InternalInterface;
import edu.berkeley.dj.internal.RewriteAllBut;
import sun.reflect.CallerSensitive;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;


/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * @author John R. Rose
 * @see #getUnsafe
 */

@RewriteAllBut(nonModClasses = {"sun/misc/Unsafe"})
public final class Unsafe00DJ implements BridgeGetNative {

    /*private static native void registerNatives();
    static {
        registerNatives();
        sun.reflect.Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }*/

    public Object __dj_toNative() { return trueUnsafe; }

    public static Object __dj_fromNative(Object o) { return theUnsafe; }

    private static final sun.misc.Unsafe trueUnsafe = InternalInterface.getInternalInterface().getUnsafe();
/*
    static {
        // TODO: fix trueUnsafe, and other TODO about it being commented out
        sun.misc.Unsafe u = null;
        try {
            u = InternalInterface.getInternalInterface().getUnsafe();
        } catch(InterfaceException e) {
            System.err.println("failed to load unsafe");
            //throw new RuntimeException("Unsafe failed to load");
            throw e;
        }
        trueUnsafe = u;

    }

    private Unsafe2() {}
*/

    private Unsafe00DJ() {
        //trueUnsafe = InternalInterface.getInternalInterface().getUnsafe();
    }

    private static final Unsafe00DJ theUnsafe = new Unsafe00DJ();

    /**
     * Provides the caller with the capability of performing unsafe
     * operations.
     *
     * <p> The returned <code>Unsafe</code> object should be carefully guarded
     * by the caller, since it can be used to read and write data at arbitrary
     * memory addresses.  It must never be passed to untrusted code.
     *
     * <p> Most methods in this class are very low-level, and correspond to a
     * small number of hardware instructions (on typical machines).  Compilers
     * are encouraged to optimize these methods accordingly.
     *
     * <p> Here is a suggested idiom for using unsafe operations:
     *
     * <blockquote><pre>
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }
     * </pre></blockquote>
     *
     * (It may assist compilers to make the local variable be
     * <code>final</code>.)
     *
     * @exception  SecurityException  if a security manager exists and its
     *             <code>checkPropertiesAccess</code> method doesn't allow
     *             access to the system properties.
     */
    @CallerSensitive
    public static Unsafe00DJ getUnsafe() {
        // TODO: should check that this matches to some system class
        /*Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        */
        return theUnsafe;
    }

    /// peek and poke operations
    /// (compilers should optimize these to memory ops)

    // These work on object fields in the Java heap.
    // They will not work on elements of packed arrays.

    /**
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object <code>o</code> at the given offset, or (if <code>o</code> is
     * null) from the memory address whose numerical value is the given
     * offset.
     * <p>
     * The results are undefined unless one of the following cases is true:
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link java.lang.reflect.Field} of some Java field and the object
     * referred to by <code>o</code> is of a class compatible with that
     * field's class.
     *
     * <li>The offset and object reference <code>o</code> (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     *
     * <li>The object referred to by <code>o</code> is an array, and the offset
     * is an integer of the form <code>B+N*S</code>, where <code>N</code> is
     * a valid index into the array, and <code>B</code> and <code>S</code> are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the <code>N</code><em>th</em> element of the array.
     *
     * </ul>
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @return the value fetched from the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    public int getInt(Object o, long offset) {
        return trueUnsafe.getInt(o, offset);
    }

    /**
     * Stores a value into a given Java variable.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     * <p>
     * The variable must be of the same type as the method
     * parameter <code>x</code>.
     *
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     * @param x the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    public void putInt(Object o, long offset, int x) {
        trueUnsafe.putInt(0, offset, x);
    }

    /**
     * Fetches a reference value from a given Java variable.
     * @see #getInt(Object, long)
     */
    public Object getObject(Object o, long offset) {
        return trueUnsafe.getObject(o, offset);
    }

    /**
     * Stores a reference value into a given Java variable.
     * <p>
     * Unless the reference <code>x</code> being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference <code>o</code> is non-null, car marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     * @see #putInt(Object, int, int)
     */
    public void putObject(Object o, long offset, Object x) {
        trueUnsafe.putObject(o, offset, x);
    }

    /** @see #getInt(Object, long) */
    public boolean getBoolean(Object o, long offset) {
        return trueUnsafe.getBoolean(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putBoolean(Object o, long offset, boolean x) {
        trueUnsafe.putBoolean(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public byte    getByte(Object o, long offset) {
        return trueUnsafe.getByte(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putByte(Object o, long offset, byte x) {
        trueUnsafe.putByte(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public short   getShort(Object o, long offset) {
        return trueUnsafe.getShort(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putShort(Object o, long offset, short x) {
        trueUnsafe.putShort(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public char    getChar(Object o, long offset) {
        return trueUnsafe.getChar(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putChar(Object o, long offset, char x) {
        trueUnsafe.putChar(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public long    getLong(Object o, long offset) {
        return trueUnsafe.getLong(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putLong(Object o, long offset, long x) {
        trueUnsafe.putLong(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public float   getFloat(Object o, long offset) {
        return trueUnsafe.getFloat(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putFloat(Object o, long offset, float x) {
        trueUnsafe.putFloat(o, offset, x);
    }
    /** @see #getInt(Object, long) */
    public double  getDouble(Object o, long offset) {
        return trueUnsafe.getDouble(o, offset);
    }
    /** @see #putInt(Object, int, int) */
    public void    putDouble(Object o, long offset, double x) {
        trueUnsafe.putDouble(o, offset, x);
    }

    /**
     * This method, like all others with 32-bit offsets, was native
     * in a previous release but is now a wrapper which simply casts
     * the offset to a long value.  It provides backward compatibility
     * with bytecodes compiled against 1.4.
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public int getInt(Object o, int offset) {
        return getInt(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putInt(Object o, int offset, int x) {
        putInt(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public Object getObject(Object o, int offset) {
        return getObject(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putObject(Object o, int offset, Object x) {
        putObject(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public boolean getBoolean(Object o, int offset) {
        return getBoolean(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putBoolean(Object o, int offset, boolean x) {
        putBoolean(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public byte getByte(Object o, int offset) {
        return getByte(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putByte(Object o, int offset, byte x) {
        putByte(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public short getShort(Object o, int offset) {
        return getShort(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putShort(Object o, int offset, short x) {
        putShort(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public char getChar(Object o, int offset) {
        return getChar(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putChar(Object o, int offset, char x) {
        putChar(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public long getLong(Object o, int offset) {
        return getLong(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putLong(Object o, int offset, long x) {
        putLong(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public float getFloat(Object o, int offset) {
        return getFloat(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putFloat(Object o, int offset, float x) {
        putFloat(o, (long)offset, x);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public double getDouble(Object o, int offset) {
        return getDouble(o, (long)offset);
    }

    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putDouble(Object o, int offset, double x) {
        putDouble(o, (long)offset, x);
    }

    // These work on values in the C heap.

    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #allocateMemory
     */
    public byte    getByte(long address) {
        return trueUnsafe.getByte(address);
    }

    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #getByte(long)
     */
    public void    putByte(long address, byte x) {
        trueUnsafe.putByte(address, x);
    }

    /** @see #getByte(long) */
    public short   getShort(long address) {
        return trueUnsafe.getShort(address);
    }
    /** @see #putByte(long, byte) */
    public void    putShort(long address, short x) {
        trueUnsafe.putShort(address, x);
    }
    /** @see #getByte(long) */
    public char    getChar(long address) {
        return trueUnsafe.getChar(address);
    }
    /** @see #putByte(long, byte) */
    public void    putChar(long address, char x) {
        trueUnsafe.putChar(address, x);
    }
    /** @see #getByte(long) */
    public int     getInt(long address) {
        return trueUnsafe.getInt(address);
    }
    /** @see #putByte(long, byte) */
    public void    putInt(long address, int x) {
        trueUnsafe.putInt(address, x);
    }
    /** @see #getByte(long) */
    public long    getLong(long address) {
        return trueUnsafe.getLong(address);
    }
    /** @see #putByte(long, byte) */
    public void    putLong(long address, long x) {
        trueUnsafe.putLong(address, x);
    }
    /** @see #getByte(long) */
    public float   getFloat(long address) {
        return trueUnsafe.getFloat(address);
    }
    /** @see #putByte(long, byte) */
    public void    putFloat(long address, float x) {
        trueUnsafe.putFloat(address, x);
    }
    /** @see #getByte(long) */
    public double  getDouble(long address) {
        return trueUnsafe.getDouble(address);
    }
    /** @see #putByte(long, byte) */
    public void    putDouble(long address, double x) {
        trueUnsafe.putDouble(address, x);
    }

    /**
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p> If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address maybe determined by consulting {@link
     * #addressSize}.
     *
     * @see #allocateMemory
     */
    public long getAddress(long address) {
        return trueUnsafe.getAddress(address);
    }

    /**
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * <p> The number of bytes actually written at the target address maybe
     * determined by consulting {@link #addressSize}.
     *
     * @see #getAddress(long)
     */
    public void putAddress(long address, long x) {
        trueUnsafe.putAddress(address, x);
    }

    /// wrappers for malloc, realloc, free:

    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    public long allocateMemory(long bytes) {
        return trueUnsafe.allocateMemory(bytes);
    }

    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #allocateMemory
     */
    public long reallocateMemory(long address, long bytes) {
        return trueUnsafe.reallocateMemory(address, bytes);
    }

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     *
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * @since 1.7
     */
    public void setMemory(Object o, long offset, long bytes, byte value) {
        trueUnsafe.setMemory(o, offset, bytes, value);
    }

    /**
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * <p>Equivalent to <code>setMemory(null, address, bytes, value)</code>.
     */
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     *
     * @since 1.7
     */
    public void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes) {
        trueUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }
    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * Equivalent to <code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>.
     */
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        copyMemory(null, srcAddress, null, destAddress, bytes);
    }

    /**
     * Disposes of a block of native memory, as obtained from {@link
     * #allocateMemory} or {@link #reallocateMemory}.  The address passed to
     * this method may be null, in which case no action is taken.
     *
     * @see #allocateMemory
     */
    public void freeMemory(long address) {
        trueUnsafe.freeMemory(address);
    }

    /// random queries

    /**
     * This constant differs from all results that will ever be returned from
     * {@link #staticFieldOffset}, {@link #objectFieldOffset},
     * or {@link #arrayBaseOffset}.
     */
    public static final int INVALID_FIELD_OFFSET   = -1;

    /**
     * Returns the offset of a field, truncated to 32 bits.
     * This method is implemented as follows:
     * <blockquote><pre>
     * public int fieldOffset(Field f) {
     *     if (Modifier.isStatic(f.getModifiers()))
     *         return (int) staticFieldOffset(f);
     *     else
     *         return (int) objectFieldOffset(f);
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldOffset} for static
     * fields and {@link #objectFieldOffset} for non-static fields.
     */
    @Deprecated
    public int fieldOffset(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return (int) staticFieldOffset(f);
        else
            return (int) objectFieldOffset(f);
    }

    /**
     * Returns the base address for accessing some static field
     * in the given class.  This method is implemented as follows:
     * <blockquote><pre>
     * public Object staticFieldBase(Class c) {
     *     Field[] fields = c.getDeclaredFields();
     *     for (int i = 0; i < fields.length; i++) {
     *         if (Modifier.isStatic(fields[i].getModifiers())) {
     *             return staticFieldBase(fields[i]);
     *         }
     *     }
     *     return null;
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldBase(Field)}
     * to obtain the base pertaining to a specific {@link Field}.
     * This method works only for JVMs which store all statics
     * for a given class in one place.
     */
    @Deprecated
    public Object staticFieldBase(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers())) {
                return staticFieldBase(fields[i]);
            }
        }
        return null;
    }

    /**
     * Report the location of a given field in the storage allocation of its
     * class.  Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset and base, and no
     * two distinct fields of the same class will ever have the same offset
     * and base.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * However, JVM implementations which store static fields at absolute
     * addresses can use long offsets and null base pointers to express
     * the field locations in a form usable by {@link #getInt(Object,long)}.
     * Therefore, code which will be ported to such JVMs on 64-bit platforms
     * must preserve all bits of static field offsets.
     * @see #getInt(Object, long)
     */
    public long staticFieldOffset(Field f) {
        return trueUnsafe.staticFieldOffset(f);
    }

    /**
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldBase}.
     * <p>Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * <p>Any given field will always have the same offset, and no two distinct
     * fields of the same class will ever have the same offset.
     *
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * It is hard to imagine a JVM technology which needs more than
     * a few bits to encode an offset within a non-array object,
     * However, for consistency with other methods in this class,
     * this method reports its result as a long value.
     * @see #getInt(Object, long)
     */
    public long objectFieldOffset(Field f) {
        return trueUnsafe.objectFieldOffset(f);
    }

    /**
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldOffset}.
     * <p>Fetch the base "Object", if any, with which static fields of the
     * given class can be accessed via methods like {@link #getInt(Object,
     * long)}.  This value may be null.  This value may refer to an object
     * which is a "cookie", not guaranteed to be a real Object, and it should
     * not be used in any way except as argument to the get and put routines in
     * this class.
     */
    public Object staticFieldBase(Field f) {
        return trueUnsafe.staticFieldBase(f);
    }

    /**
     * Detect if the given class may need to be initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     * @return false only if a call to {@code ensureClassInitialized} would have no effect
     */
    public boolean shouldBeInitialized(Class<?> c) {
        return trueUnsafe.shouldBeInitialized(c);
    }

    /**
     * Ensure the given class has been initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     */
    public void ensureClassInitialized(Class<?> c) {
        trueUnsafe.ensureClassInitialized(c);
    }

    /**
     * Report the offset of the first element in the storage allocation of a
     * given array class.  If {@link #arrayIndexScale} returns a non-zero value
     * for the same class, you may use that scale factor, together with this
     * base offset, to form new offsets to access elements of arrays of the
     * given class.
     *
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    public int arrayBaseOffset(Class<?> arrayClass) {
        return trueUnsafe.arrayBaseOffset(arrayClass);
    }

    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /**
     * Report the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, int)}, so the scale factor for such classes is reported
     * as zero.
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    public int arrayIndexScale(Class<?> arrayClass) {
        return  trueUnsafe.arrayIndexScale(arrayClass);
    }

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

    /**
     * Report the size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the sizes of
     * other primitive types (as stored in native memory blocks) is determined
     * fully by their information content.
     */
    public int addressSize() {
        return trueUnsafe.addressSize();
    }

    /** The value of {@code addressSize()} */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    /**
     * Report the size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     */
    public int pageSize() {
        return trueUnsafe.pageSize();
    }


    /// random trusted operations from JNI:

    /**
     * Tell the VM to define a class, without security checks.  By default, the
     * class loader and protection domain come from the caller's class.
     */
    public Class<?> defineClass(String name, byte[] b, int off, int len,
                                       ClassLoader loader,
                                       ProtectionDomain protectionDomain) {
        return trueUnsafe.defineClass(name, b, off, len, loader, protectionDomain);
    }

    /**
     * Define a class but do not make it known to the class loader or system dictionary.
     * <p>
     * For each CP entry, the corresponding CP patch must either be null or have
     * the a format that matches its tag:
     * <ul>
     * <li>Integer, Long, Float, Double: the corresponding wrapper object type from java.lang
     * <li>Utf8: a string (must have suitable syntax if used as signature or name)
     * <li>Class: any java.lang.Class object
     * <li>String: any object (not just a java.lang.String)
     * <li>InterfaceMethodRef: (NYI) a method handle to invoke on that call site's arguments
     * </ul>
     * @params hostClass context for linkage, access control, protection domain, and class loader
     * @params data      bytes of a class file
     * @params cpPatches where non-null entries exist, they replace corresponding CP entries in data
     */
    public Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches) {
        return trueUnsafe.defineAnonymousClass(hostClass, data, cpPatches);
    }


    /** Allocate an instance but do not run any constructor.
     Initializes the class if it has not yet been. */
    public Object allocateInstance(Class<?> cls)
            throws InstantiationException {
        return trueUnsafe.allocateInstance(cls);
    }

    /** Lock the object.  It must get unlocked via {@link #monitorExit}. */
    public void monitorEnter(Object o) {
        trueUnsafe.monitorEnter(o);
    }

    /**
     * Unlock the object.  It must have been locked via {@link
     * #monitorEnter}.
     */
    public void monitorExit(Object o) {
        trueUnsafe.monitorExit(o);
    }

    /**
     * Tries to lock the object.  Returns true or false to indicate
     * whether the lock succeeded.  If it did, the object must be
     * unlocked via {@link #monitorExit}.
     */
    public boolean tryMonitorEnter(Object o) {
        return trueUnsafe.tryMonitorEnter(o);
    }

    /** Throw the exception without telling the verifier. */
    public void throwException(Throwable ee) {
        trueUnsafe.throwException(ee);
    }


    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * @return <tt>true</tt> if successful
     */
    public final boolean compareAndSwapObject(Object o, long offset,
                                                     Object expected,
                                                     Object x) {
        return trueUnsafe.compareAndSwapObject(o, offset, expected, x);
    }

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * @return <tt>true</tt> if successful
     */
    public final boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x) {
        return trueUnsafe.compareAndSwapInt(o, offset, expected, x);
    }

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * @return <tt>true</tt> if successful
     */
    public final boolean compareAndSwapLong(Object o, long offset,
                                                   long expected,
                                                   long x) {
        return trueUnsafe.compareAndSwapLong(o, offset, expected, x);
    }

    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     */
    public Object getObjectVolatile(Object o, long offset) {
        return trueUnsafe.getObjectVolatile(o, offset);
    }

    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     */
    public void    putObjectVolatile(Object o, long offset, Object x) {
        trueUnsafe.putObjectVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getInt(Object, long)}  */
    public int     getIntVolatile(Object o, long offset) {
        return trueUnsafe.getIntVolatile(o, offset);
    }

    /** Volatile version of {@link #putInt(Object, long, int)}  */
    public void    putIntVolatile(Object o, long offset, int x) {
        trueUnsafe.putInt(o, offset, x);
    }

    /** Volatile version of {@link #getBoolean(Object, long)}  */
    public boolean getBooleanVolatile(Object o, long offset) {
        return trueUnsafe.getBooleanVolatile(o, offset);
    }

    /** Volatile version of {@link #putBoolean(Object, long, boolean)}  */
    public void    putBooleanVolatile(Object o, long offset, boolean x) {
        trueUnsafe.putBooleanVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getByte(Object, long)}  */
    public byte    getByteVolatile(Object o, long offset) {
        return trueUnsafe.getByteVolatile(o, offset);
    }

    /** Volatile version of {@link #putByte(Object, long, byte)}  */
    public void    putByteVolatile(Object o, long offset, byte x) {
        trueUnsafe.putByteVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getShort(Object, long)}  */
    public short   getShortVolatile(Object o, long offset) {
        return trueUnsafe.getShortVolatile(o, offset);
    }

    /** Volatile version of {@link #putShort(Object, long, short)}  */
    public void    putShortVolatile(Object o, long offset, short x) {
        trueUnsafe.putShortVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getChar(Object, long)}  */
    public char    getCharVolatile(Object o, long offset) {
        return trueUnsafe.getCharVolatile(o, offset);
    }

    /** Volatile version of {@link #putChar(Object, long, char)}  */
    public void    putCharVolatile(Object o, long offset, char x) {
        trueUnsafe.putCharVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getLong(Object, long)}  */
    public long    getLongVolatile(Object o, long offset) {
        return trueUnsafe.getLongVolatile(o, offset);
    }

    /** Volatile version of {@link #putLong(Object, long, long)}  */
    public void    putLongVolatile(Object o, long offset, long x) {
        trueUnsafe.putLongVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getFloat(Object, long)}  */
    public float   getFloatVolatile(Object o, long offset) {
        return trueUnsafe.getFloatVolatile(o, offset);
    }

    /** Volatile version of {@link #putFloat(Object, long, float)}  */
    public void    putFloatVolatile(Object o, long offset, float x) {
        trueUnsafe.putFloatVolatile(o, offset, x);
    }

    /** Volatile version of {@link #getDouble(Object, long)}  */
    public double  getDoubleVolatile(Object o, long offset) {
        return trueUnsafe.getDoubleVolatile(o, offset);
    }

    /** Volatile version of {@link #putDouble(Object, long, double)}  */
    public void    putDoubleVolatile(Object o, long offset, double x) {
        trueUnsafe.putDoubleVolatile(o, offset, x);
    }

    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     */
    public void    putOrderedObject(Object o, long offset, Object x) {
        trueUnsafe.putOrderedObject(o, offset, x);
    }

    /** Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}  */
    public void    putOrderedInt(Object o, long offset, int x) {
        trueUnsafe.putOrderedInt(o, offset, x);
    }

    /** Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)} */
    public void    putOrderedLong(Object o, long offset, long x) {
        trueUnsafe.putOrderedLong(o, offset, x);
    }

    /**
     * Unblock the given thread blocked on <tt>park</tt>, or, if it is
     * not blocked, cause the subsequent call to <tt>park</tt> not to
     * block.  Note: this operation is "unsafe" solely because the
     * caller must somehow ensure that the thread has not been
     * destroyed. Nothing special is usually required to ensure this
     * when called from Java (in which there will ordinarily be a live
     * reference to the thread) but this is not nearly-automatically
     * so when calling from native code.
     * @param thread the thread to unpark.
     *
     */
    public void unpark(Object thread) {
        trueUnsafe.unpark(thread);
    }

    /**
     * Block current thread, returning when a balancing
     * <tt>unpark</tt> occurs, or a balancing <tt>unpark</tt> has
     * already occurred, or the thread is interrupted, or, if not
     * absolute and time is not zero, the given time nanoseconds have
     * elapsed, or if absolute, the given deadline in milliseconds
     * since Epoch has passed, or spuriously (i.e., returning for no
     * "reason"). Note: This operation is in the Unsafe class only
     * because <tt>unpark</tt> is, so it would be strange to place it
     * elsewhere.
     */
    public void park(boolean isAbsolute, long time) {
        trueUnsafe.park(isAbsolute, time);
    }

    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given <tt>nelem</tt> samples and
     * assigns to the elements of the given <tt>loadavg</tt> array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     *
     * @params loadavg an array of double of size nelems
     * @params nelems the number of samples to be retrieved and
     *         must be 1 to 3.
     *
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     */
    public int getLoadAverage(double[] loadavg, int nelems) {
        return trueUnsafe.getLoadAverage(loadavg, nelems);
    }

    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object <code>o</code> at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getObjectVolatile(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }


    /**
     * Ensures lack of reordering of loads before the fence
     * with loads or stores after the fence.
     * @since 1.8
     */
    public void loadFence() {
        trueUnsafe.loadFence();
    }

    /**
     * Ensures lack of reordering of stores before the fence
     * with loads or stores after the fence.
     * @since 1.8
     */
    public void storeFence() {
        trueUnsafe.storeFence();
    }

    /**
     * Ensures lack of reordering of loads or stores before the fence
     * with loads or stores after the fence.
     * @since 1.8
     */
    public void fullFence() {
        trueUnsafe.fullFence();
    }

    /**
     * Throws IllegalAccessError; for use by the VM.
     * @since 1.8
     */
    private static void throwIllegalAccessError() {
        throw new IllegalAccessError();
    }

}
