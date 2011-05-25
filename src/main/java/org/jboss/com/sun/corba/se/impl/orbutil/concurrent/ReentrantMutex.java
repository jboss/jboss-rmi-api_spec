/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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
  File: Mutex.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
*/

package org.jboss.com.sun.corba.se.impl.orbutil.concurrent;

import org.jboss.com.sun.corba.se.impl.orbutil.ORBUtility;
import org.omg.CORBA.INTERNAL;

public class ReentrantMutex implements Sync  {

    /** The thread holding the lock **/
    protected Thread holder_ = null;

    /** number of times thread has acquired the lock **/
    protected int counter_ = 0 ;

    protected boolean debug = false ;

    public ReentrantMutex()
    {
        this( false ) ;
    }

    public ReentrantMutex( boolean debug )
    {
        this.debug = debug ;
    }

    public void acquire() throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized(this) {
            try {
                if (debug)
                    ORBUtility.dprintTrace( this,
                        "acquire enter: holder_=" +
                        ORBUtility.getThreadName(holder_) +
                        " counter_=" + counter_ ) ;

                Thread thr = Thread.currentThread();
                if (holder_ != thr) {
                    try {
                        while (counter_ > 0)
                            wait();

                        // This can't happen, but make sure anyway
                        if (counter_ != 0)
                            throw new INTERNAL(
                                "counter not 0 when first acquiring mutex" ) ;

                        holder_ = thr;
                    } catch (InterruptedException ex) {
                        notify();
                        throw ex;
                    }
                }

                counter_ ++ ;
            } finally {
                if (debug)
                    ORBUtility.dprintTrace( this, "acquire exit: holder_=" +
                    ORBUtility.getThreadName(holder_) + " counter_=" +
                    counter_ ) ;
            }
        }
    }

    void acquireAll( int count ) throws InterruptedException
    {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized(this) {
            try {
                if (debug)
                    ORBUtility.dprintTrace( this,
                        "acquireAll enter: count=" + count + " holder_=" +
                        ORBUtility.getThreadName(holder_) + " counter_=" +
                        counter_ ) ;
                Thread thr = Thread.currentThread();
                if (holder_ == thr) {
                    throw new INTERNAL(
                        "Cannot acquireAll while holding the mutex" ) ;
                } else {
                    try {
                        while (counter_ > 0)
                            wait();

                        // This can't happen, but make sure anyway
                        if (counter_ != 0)
                            throw new INTERNAL(
                                "counter not 0 when first acquiring mutex" ) ;

                        holder_ = thr;
                    } catch (InterruptedException ex) {
                        notify();
                        throw ex;
                    }
                }

                counter_ = count ;
            } finally {
                if (debug)
                    ORBUtility.dprintTrace( this, "acquireAll exit: count=" +
                    count + " holder_=" + ORBUtility.getThreadName(holder_) +
                    " counter_=" + counter_ ) ;
            }
        }
    }

    public synchronized void release()
    {
        try {
            if (debug)
                ORBUtility.dprintTrace( this, "release enter: " +
                    " holder_=" + ORBUtility.getThreadName(holder_) +
                    " counter_=" + counter_ ) ;

            Thread thr = Thread.currentThread();
            if (thr != holder_)
                throw new INTERNAL(
                    "Attempt to release Mutex by thread not holding the Mutex" ) ;
            else
                counter_ -- ;

            if (counter_ == 0) {
                holder_ = null;
                notify();
            }
        } finally {
            if (debug)
                ORBUtility.dprintTrace( this, "release exit: " +
                    " holder_=" + ORBUtility.getThreadName(holder_) +
                    " counter_=" + counter_ ) ;
        }
    }

    synchronized int releaseAll()
    {
        try {
            if (debug)
                ORBUtility.dprintTrace( this, "releaseAll enter: " +
                    " holder_=" + ORBUtility.getThreadName(holder_) +
                    " counter_=" + counter_ ) ;

            Thread thr = Thread.currentThread();
            if (thr != holder_)
                throw new INTERNAL(
                    "Attempt to releaseAll Mutex by thread not holding the Mutex" ) ;

            int result = counter_ ;
            counter_ = 0 ;
            holder_ = null ;
            notify() ;
            return result ;
        } finally {
            if (debug)
                ORBUtility.dprintTrace( this, "releaseAll exit: " +
                    " holder_=" + ORBUtility.getThreadName(holder_) +
                    " counter_=" + counter_ ) ;
        }
    }

    public boolean attempt(long msecs) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized(this) {
            try {
                if (debug)
                    ORBUtility.dprintTrace( this, "attempt enter: msecs=" +
                        msecs + " holder_=" +
                        ORBUtility.getThreadName(holder_) +
                        " counter_=" + counter_ ) ;

                Thread thr = Thread.currentThread() ;

                if (counter_==0) {
                    holder_ = thr;
                    counter_ = 1 ;
                    return true;
                } else if (msecs <= 0) {
                    return false;
                } else {
                    long waitTime = msecs;
                    long start = System.currentTimeMillis();
                    try {
                        for (;;) {
                            wait(waitTime);
                            if (counter_==0) {
                                holder_ = thr;
                                counter_ = 1 ;
                                return true;
                            } else {
                                waitTime = msecs -
                                    (System.currentTimeMillis() - start);

                                if (waitTime <= 0)
                                    return false;
                            }
                        }
                    } catch (InterruptedException ex) {
                        notify();
                        throw ex;
                    }
                }
            } finally {
                if (debug)
                    ORBUtility.dprintTrace( this, "attempt exit: " +
                        " holder_=" + ORBUtility.getThreadName(holder_) +
                        " counter_=" + counter_ ) ;
            }
        }
    }
}
