/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

package org.jboss.com.sun.corba.se.impl.ior.iiop;

import org.jboss.com.sun.corba.se.impl.encoding.CDROutputStream;
import org.jboss.com.sun.corba.se.impl.encoding.EncapsOutputStream;
import org.jboss.com.sun.corba.se.impl.ior.EncapsulationUtility;
import org.jboss.com.sun.corba.se.spi.ior.ObjectId;
import org.jboss.com.sun.corba.se.spi.ior.ObjectKeyTemplate;
import org.jboss.com.sun.corba.se.spi.ior.TaggedProfile;
import org.jboss.com.sun.corba.se.spi.ior.TaggedProfileTemplate;
import org.jboss.com.sun.corba.se.spi.ior.TaggedProfileTemplateBase;
import org.jboss.com.sun.corba.se.spi.ior.iiop.GIOPVersion;
import org.jboss.com.sun.corba.se.spi.ior.iiop.IIOPAddress;
import org.jboss.com.sun.corba.se.spi.ior.iiop.IIOPFactories;
import org.jboss.com.sun.corba.se.spi.ior.iiop.IIOPProfileTemplate;
import org.jboss.com.sun.corba.se.spi.orb.ORB;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;
import org.omg.IOP.TAG_INTERNET_IOP;

/**
 * @author
 * If getMinorVersion==0, this does not contain any tagged components
 */
public class IIOPProfileTemplateImpl extends TaggedProfileTemplateBase
    implements IIOPProfileTemplate
{
    private ORB orb ;
    private GIOPVersion giopVersion ;
    private IIOPAddress primary ;

    public boolean equals( Object obj )
    {
        if (!(obj instanceof IIOPProfileTemplateImpl))
            return false ;

        IIOPProfileTemplateImpl other = (IIOPProfileTemplateImpl)obj ;

        return super.equals( obj ) && giopVersion.equals( other.giopVersion ) &&
            primary.equals( other.primary ) ;
    }

    public int hashCode()
    {
        return super.hashCode() ^ giopVersion.hashCode() ^ primary.hashCode() ;
    }

    public TaggedProfile create( ObjectKeyTemplate oktemp, ObjectId id )
    {
        return IIOPFactories.makeIIOPProfile( orb, oktemp, id, this ) ;
    }

    public GIOPVersion getGIOPVersion()
    {
        return giopVersion ;
    }

    public IIOPAddress getPrimaryAddress()
    {
        return primary ;
    }

    public IIOPProfileTemplateImpl( ORB orb, GIOPVersion version, IIOPAddress primary )
    {
        this.orb = orb ;
        this.giopVersion = version ;
        this.primary = primary ;
        if (giopVersion.getMinor() == 0)
            // Adding tagged components is not allowed for IIOP 1.0,
            // so this template is complete and should be made immutable.
            makeImmutable() ;
    }

    public IIOPProfileTemplateImpl( InputStream istr )
    {
        byte major = istr.read_octet() ;
        byte minor = istr.read_octet() ;
        giopVersion = GIOPVersion.getInstance( major, minor ) ;
        primary = new IIOPAddressImpl( istr ) ;
        orb = (ORB)(istr.orb()) ;
        // Handle any tagged components (if applicable)
        if (minor > 0)
            EncapsulationUtility.readIdentifiableSequence(
                this, orb.getTaggedComponentFactoryFinder(), istr ) ;

        makeImmutable() ;
    }

    public void write( ObjectKeyTemplate okeyTemplate, ObjectId id, OutputStream os)
    {
        giopVersion.write( os ) ;
        primary.write( os ) ;

        // Note that this is NOT an encapsulation: do not marshal
        // the endianness flag.  However, the length is required.
        // Note that this cannot be accomplished with a codec!

        // Use the byte order of the given stream
        OutputStream encapsulatedOS = new EncapsOutputStream( (ORB)os.orb(),
            ((CDROutputStream)os).isLittleEndian() ) ;

        okeyTemplate.write( id, encapsulatedOS ) ;
        EncapsulationUtility.writeOutputStream( encapsulatedOS, os ) ;

        if (giopVersion.getMinor() > 0)
            EncapsulationUtility.writeIdentifiableSequence( this, os ) ;
    }

    /** Write out this IIOPProfileTemplateImpl only.
    */
    public void writeContents( OutputStream os)
    {
        giopVersion.write( os ) ;
        primary.write( os ) ;

        if (giopVersion.getMinor() > 0)
            EncapsulationUtility.writeIdentifiableSequence( this, os ) ;
    }

    public int getId()
    {
        return TAG_INTERNET_IOP.value ;
    }

    public boolean isEquivalent( TaggedProfileTemplate temp )
    {
        if (!(temp instanceof IIOPProfileTemplateImpl))
            return false ;

        IIOPProfileTemplateImpl tempimp = (IIOPProfileTemplateImpl)temp ;

        return primary.equals( tempimp.primary )  ;
    }

}
