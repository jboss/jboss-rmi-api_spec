/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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

package org.jboss.com.sun.corba.se.impl.encoding;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.com.sun.corba.se.impl.corba.TypeCodeImpl;
import org.jboss.com.sun.corba.se.spi.ior.iiop.GIOPVersion;
import org.omg.CORBA_2_3.portable.InputStream;

public class TypeCodeInputStream extends EncapsInputStream implements TypeCodeReader
{
    private Map<Integer, TypeCodeImpl> typeMap = null;

    private InputStream enclosure = null;

    public TypeCodeInputStream(org.omg.CORBA.ORB orb, byte[] data, int size)
    {
        super(orb, data, size);
    }

    public TypeCodeInputStream(org.omg.CORBA.ORB orb, byte[] data, int size, boolean littleEndian, GIOPVersion version)
    {
        super(orb, data, size, littleEndian, version);
    }

    public TypeCodeInputStream(org.omg.CORBA.ORB orb, ByteBuffer byteBuffer, int size, boolean littleEndian,
            GIOPVersion version)
    {
        super(orb, byteBuffer, size, littleEndian, version);
    }

    public void addTypeCodeAtPosition(TypeCodeImpl tc, int position)
    {
        if (typeMap == null)
        {
            typeMap = new HashMap<Integer, TypeCodeImpl>(16);
        }
        typeMap.put(position, tc);
    }

    public TypeCodeImpl getTypeCodeAtPosition(int position)
    {
        if (typeMap == null)
            return null;
        return typeMap.get(position);
    }

    public void setEnclosingInputStream(InputStream enclosure)
    {
        this.enclosure = enclosure;
    }

    public TypeCodeReader getTopLevelStream()
    {
        if (enclosure == null)
            return this;
        if (enclosure instanceof TypeCodeReader)
            return ((TypeCodeReader) enclosure).getTopLevelStream();
        return this;
    }

    public int getTopLevelPosition()
    {
        if (enclosure != null && enclosure instanceof TypeCodeReader)
        {
            // The enclosed stream has to consider if the enclosing stream had to read the enclosed stream completely
            // when creating it. This is why the size of the enclosed stream needs to be substracted.
            int topPos = ((TypeCodeReader) enclosure).getTopLevelPosition();
            // Substract getBufferLength from the parents pos because it read this stream from its own when creating it
            int pos = topPos - getBufferLength() + getPosition();
            return pos;
        }
        return getPosition();
    }

    public static TypeCodeInputStream readEncapsulation(InputStream is, org.omg.CORBA.ORB _orb)
    {
        // _REVISIT_ Would be nice if we didn't have to copy the buffer!
        TypeCodeInputStream encap;

        int encapLength = is.read_long();

        // read off part of the buffer corresponding to the encapsulation
        byte[] encapBuffer = new byte[encapLength];
        is.read_octet_array(encapBuffer, 0, encapBuffer.length);

        // create an encapsulation using the marshal buffer
        if (is instanceof CDRInputStream)
        {
            encap = new TypeCodeInputStream(_orb, encapBuffer, encapBuffer.length,
                    ((CDRInputStream) is).isLittleEndian(), ((CDRInputStream) is).getGIOPVersion());
        }
        else
        {
            encap = new TypeCodeInputStream(_orb, encapBuffer, encapBuffer.length);
        }
        encap.setEnclosingInputStream(is);
        encap.makeEncapsulation();
        return encap;
    }

    protected void makeEncapsulation()
    {
        // first entry in an encapsulation is the endianess
        consumeEndian();
    }

    public void printTypeMap()
    {
        System.out.println("typeMap = {");
        Iterator<Integer> i = typeMap.keySet().iterator();
        while (i.hasNext())
        {
            Integer pos = i.next();
            TypeCodeImpl tci = typeMap.get(pos);
            System.out.println("  key = " + pos.intValue() + ", value = " + tci.description());
        }
        System.out.println("}");
    }
}
