/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ejb3.remote.rmi.prototype;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SerializableInvocationHandler implements InvocationHandler, Serializable
   {
      private static final long serialVersionUID = 1L;

      private Serializable target;
      private Class<?> businessInterface;
      private transient InvocationHandler delegate;

      public SerializableInvocationHandler(Serializable target, Class<?> businessInterface, InvocationHandler delegate)
      {
         this.target = target;
         this.businessInterface = businessInterface;
         this.delegate = delegate;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         return delegate.invoke(proxy, method, args);
      }

      private Object readResolve() throws ObjectStreamException
      {
         InvocationHandlerFactory factory = CurrentInvocationHandlerFactory.get();
         this.delegate = factory.create(target, businessInterface);
         return this;
      }

      /*
      private Object writeReplace() throws ObjectStreamException
      {
         System.out.println("writeReplace");
         new Exception("writeReplace").printStackTrace();
         throw new RuntimeException("NYI");
      }
      */
}
