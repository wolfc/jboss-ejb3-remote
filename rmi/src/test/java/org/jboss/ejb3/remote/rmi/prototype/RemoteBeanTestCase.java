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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class RemoteBeanTestCase
{
   static int num = 0;

   public static interface BeanRemote
   {
      String sayHi(String name);
   }

   public static interface RemoteInvocationHandler extends Remote
   {
      Object invoke(Serializable target, Class<?> invokedBusinessInterface, Map<String, Object> contextData, String methodName, Class<?> parameterTypes[], Object args[]) throws Exception;
   }

   @BeforeClass
   public static void beforeClass() throws Exception
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> interfaces[] = {BeanRemote.class};
      InvocationHandler handler = new InvocationHandler()
      {
         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
         {
            if(method.getName().equals("toString"))
               return this.toString();
            String result = "Hi " + args[0];
            args[0] = "done";
            return result;
         }
      };
      UUID containerUUID = UUID.randomUUID();
      InvocationHandler serializableHandler = new SerializableInvocationHandler(containerUUID, BeanRemote.class, handler);
      BeanRemote local = (BeanRemote) Proxy.newProxyInstance(loader, interfaces, serializableHandler);

      final Map<String, Object> map = new HashMap<String, Object>();
      map.put("key", local);

      final Map<Serializable, Object> targets = new HashMap<Serializable, Object>();
      targets.put(0, map);
      targets.put(containerUUID, handler);
      
      RemoteInvocationHandler remoteHandler = new RemoteInvocationHandler()
      {
         public Object invoke(Serializable target, Class<?> invokedBusinessInterface, Map<String, Object> contextData, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception
         {
            Method method = invokedBusinessInterface.getMethod(methodName, parameterTypes);
            Object obj = targets.get(target);
            if(obj instanceof InvocationHandler)
               return invokeHandler((InvocationHandler) obj, target, method, args);
            return method.invoke(obj, args);
         }
      };

      // make it into a RemoteObjectInvocationHandler
      Remote remoteProxy = UnicastRemoteObject.exportObject(remoteHandler, 34012);

      Registry registry = LocateRegistry.createRegistry(34011);
      registry.bind("Dispatcher", remoteProxy);

      // race condition somewhere, resulting in a connection refused
      Thread.sleep(100);
   }

   private static Object invokeHandler(InvocationHandler handler, Object proxy, Method method, Object args[]) throws Exception
   {
      try
      {
         return handler.invoke(proxy, method, args);
      }
      catch(Error e)
      {
         throw e;
      }
      catch(RuntimeException e)
      {
         throw e;
      }
      catch(Exception e)
      {
         throw e;
      }
      catch(Throwable t)
      {
         throw new Error(t);
      }
   }

   @Test
   public void test1() throws Exception
   {
      Registry registry = LocateRegistry.getRegistry(34011);
      //Map<String, Object> map = (Map<String, Object>) registry.lookup("Map");
      final RemoteInvocationHandler remoteHandler = (RemoteInvocationHandler) registry.lookup("Dispatcher");
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> interfaces[] = {Map.class};
      InvocationHandler handler = new InvocationHandler()
      {
         public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
         {
            CurrentInvocationHandlerFactory.set(new InvocationHandlerFactory() {
               public InvocationHandler create(final Serializable target, final Class<?> businessInterface)
               {
                  if(target == null)
                     throw new IllegalArgumentException("target is null");
                  if(businessInterface == null)
                     throw new IllegalArgumentException("businessInterface is null");
                  return new InvocationHandler() {
                     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
                     {
                        return remoteHandler.invoke(target, businessInterface, null, method.getName(), method.getParameterTypes(), args);
                     }
                  };
               }
            });
            try
            {
               return remoteHandler.invoke(0, Map.class, null, method.getName(), method.getParameterTypes(), args);
            }
            finally
            {
               CurrentInvocationHandlerFactory.remove();
            }
         }
      };
      Map<String, Object> map = (Map<String, Object>) Proxy.newProxyInstance(loader, interfaces, handler);

      System.out.println(Arrays.toString(map.get("key").getClass().getInterfaces()));
      BeanRemote bean = (BeanRemote) map.get("key");
      String x = "me";
      System.out.println(bean.sayHi(x));
      System.out.println(x);
   }
}
