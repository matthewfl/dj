package edu.berkeley.dj.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by matthewfl
 */
public class StaticFieldHelper {

    private StaticFieldHelper() {}

    static public void writeField_Z(String identifier, boolean val) {
        ByteBuffer b = writeReq(identifier, 1);
        if(val)
            b.put((byte)1);
        else
            b.put((byte)0);
        sendReq(b);
    }

    static public void writeField_C(String identifier, char val) {
        ByteBuffer b = writeReq(identifier, 4);
        b.putChar(val);
        sendReq(b);
    }

    static public void writeField_B(String identifier, byte val) {
        ByteBuffer b = writeReq(identifier, 1);
        b.put(val);
        sendReq(b);
    }

    static public void writeField_S(String identifier, short val) {
        ByteBuffer b = writeReq(identifier, 2);
        b.putShort(val);
        sendReq(b);
    }

    static public void writeField_I(String identifier, int val) {
        ByteBuffer b = writeReq(identifier, 4);
        b.putInt(val);
        sendReq(b);
    }

    static public void writeField_J(String identifier, long val) {
        ByteBuffer b = writeReq(identifier, 8);
        b.putLong(val);
        sendReq(b);
    }

    static public void writeField_F(String identifier, float val) {
        ByteBuffer b = writeReq(identifier, 4);
        b.putFloat(val);
        sendReq(b);
    }

    static public void writeField_D(String identifier, double val) {
        ByteBuffer b = writeReq(identifier, 8);
        b.putDouble(val);
        sendReq(b);
    }

    static public void writeField_A(String identifier, Object obj) {
        byte[] darr = DistributedObjectHelper.getDistributedId(obj).toArr();
        ByteBuffer b = writeReq(identifier, darr.length);
        b.put(darr);
        sendReq(b);
    }

    static private ByteBuffer writeReq(String id, int l) {
        byte[] ida = id.getBytes();
        ByteBuffer b = ByteBuffer.allocate(ida.length + 4 + l);
        b.putInt(ida.length);
        b.put(ida);
        return b;
    }

    static private void sendReq(ByteBuffer b) {
        InternalInterface.getInternalInterface().staticFieldUpdate(b.array());
    }

    static public void recvWriteField(ByteBuffer buf) {
        try {
            int idlength = buf.getInt();
            String id = new String(buf.array(), 4, idlength);
            buf.position(idlength + 4);
            String ids[] = id.split("::");
            Class<?> cls = AugmentedClassLoader.forName(ids[0]);
            Field f = cls.getDeclaredField(ids[1]);
            f.setAccessible(true);
            if(Modifier.isFinal(f.getModifiers())) {
                // the fact this works is a little bit crazy
                Field fm = Field.class.getDeclaredField("modifiers");
                fm.setAccessible(true);
                fm.set(f, f.getModifiers() & ~Modifier.FINAL);
            }
            Class<?> ftype = f.getType();
            if (ftype == boolean.class) {
                f.setBoolean(null, buf.get() == 1);
            } else if (ftype == char.class) {
                f.setChar(null, buf.getChar());
            } else if (ftype == byte.class) {
                f.setByte(null, buf.get());
            } else if (ftype == short.class) {
                f.setShort(null, buf.getShort());
            } else if (ftype == int.class) {
                f.setInt(null, buf.getInt());
            } else if (ftype == long.class) {
                f.setLong(null, buf.getLong());
            } else if (ftype == float.class) {
                f.setFloat(null, buf.getFloat());
            } else if (ftype == double.class) {
                f.setDouble(null, buf.getDouble());
            } else {
                // we must have an object type
                byte[] oarr = new byte[buf.limit() - 4 - idlength];
                buf.get(oarr, 0, oarr.length);
                Object o = DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(oarr));
                f.set(null, o);
            }
        } catch(ClassNotFoundException|
                NoSuchFieldException|
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static public byte[] getAllStaticFields(String clsname) {
        try {
            Field[] fs = getClassFields(clsname);
            int bufSize = 0;
            byte[][] dids = new byte[fs.length][];
            int at = 0;
            for (Field f : fs) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    break;
                }
                f.setAccessible(true);
                if(f.getName().startsWith("__dj_"))
                    continue;
                Class<?> ftype = f.getType();
                if (ftype == boolean.class) {
                    bufSize += 1;
                } else if (ftype == char.class) {
                    bufSize += 4;
                } else if (ftype == byte.class) {
                    bufSize += 1;
                } else if (ftype == short.class) {
                    bufSize += 2;
                } else if (ftype == int.class) {
                    bufSize += 4;
                } else if (ftype == long.class) {
                    bufSize += 8;
                } else if (ftype == float.class) {
                    bufSize += 4;
                } else if (ftype == double.class) {
                    bufSize += 8;
                } else {
                    // this is an object type
                    byte[] did = DistributedObjectHelper.getDistributedId(f.get(null)).toArr();
                    bufSize += 4 + did.length;
                    dids[at++] = did;
                }
            }
            ByteBuffer ret = ByteBuffer.allocate(bufSize);
            at = 0;
            for (Field f : fs) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    break;
                }
                if(f.getName().startsWith("__dj_"))
                    continue;
                Class<?> ftype = f.getType();
                if (ftype == boolean.class) {
                    if (f.getBoolean(null))
                        ret.put((byte) 1);
                    else
                        ret.put((byte) 0);
                } else if (ftype == char.class) {
                    ret.putChar(f.getChar(null));
                } else if (ftype == byte.class) {
                    ret.put(f.getByte(null));
                } else if (ftype == short.class) {
                    ret.putShort(f.getShort(null));
                } else if (ftype == int.class) {
                    ret.putInt(f.getInt(null));
                } else if (ftype == long.class) {
                    ret.putLong(f.getLong(null));
                } else if (ftype == float.class) {
                    ret.putFloat(f.getFloat(null));
                } else if (ftype == double.class) {
                    ret.putDouble(f.getDouble(null));
                } else {
                    // this is an object type
                    ret.putInt(dids[at].length);
                    ret.put(dids[at++]);
                }
            }
            return ret.array();
        } catch(ClassNotFoundException|
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static public void loadAllStaticFields(String classname, ByteBuffer buf) {
        try {
            Field[] fs = getClassFields(classname);
            for (Field f : fs) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    break;
                }
                if (f.getName().startsWith("__dj_"))
                    continue;
                f.setAccessible(true);
                Class<?> ftype = f.getType();
                if (ftype == boolean.class) {
                    f.setBoolean(null, buf.get() == 1);
                } else if (ftype == char.class) {
                    f.setChar(null, buf.getChar());
                } else if (ftype == byte.class) {
                    f.setByte(null, buf.get());
                } else if (ftype == short.class) {
                    f.setShort(null, buf.getShort());
                } else if (ftype == int.class) {
                    f.setInt(null, buf.getInt());
                } else if (ftype == long.class) {
                    f.setLong(null, buf.getLong());
                } else if (ftype == float.class) {
                    f.setFloat(null, buf.getFloat());
                } else if (ftype == double.class) {
                    f.setDouble(null, buf.getDouble());
                } else {
                    // this is an object type
                    int length = buf.getInt();
                    byte[] did = new byte[length];
                    buf.get(did, 0, length);
                    Object o = DistributedObjectHelper.getObject(new DistributedObjectHelper.DistributedObjectId(did));
                    f.set(null, o);
                }
            }
        } catch(ClassNotFoundException|
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static private Field[] getClassFields(String classname) throws ClassNotFoundException {
        Class<?> cls = AugmentedClassLoader.forName(classname);
        Field[] fs = cls.getDeclaredFields();
        // sort the fields so that the static ones are first
        // and that they are always in the same order using alphabetical order
        Arrays.sort(fs, new Comparator<Field>() {
            @Override
            public int compare(Field field, Field t1) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    return 1;
                }
                if (!Modifier.isStatic(t1.getModifiers())) {
                    return -1;
                }
                return field.getName().compareTo(t1.getName());
            }
        });
        return fs;
    }
}
