// Copyright (c) 2011 Paul Butcher
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.borachio

abstract class MockingClassLoader extends ClassLoader {

  type UnderlyingClassLoader <: ClassLoader

  protected val defaultClassLoader: UnderlyingClassLoader = getDefaultClassLoader()

  protected def getDefaultClassLoader(): UnderlyingClassLoader
  protected def createMockClassLoader(): ClassLoaderInternal
  protected def createNormalClassLoader(): ClassLoaderInternal

  var factory = new ThreadLocal[Any]

  protected trait ClassLoaderInternal extends ClassLoader {

    override def loadClass(name: String): Class[_] = MockingClassLoader.this.loadClass(name)

    def loadClassInternal(name: String) = super.loadClass(name)

    def getFactory = factory.get

    // We need to create a new instance of the "normal" class loader to avoid
    // java.lang.LinkageError: loader attempted  duplicate class definition
    // caused by the fact that the pre-existing normal class loader has already
    // been recorded as an initiating class loader for this class. See:
    // http://docs.jboss.org/jbossas/docs/Server_Configuration_Guide/4/html/Class_Loading_and_Types_in_Java-LinkageErrors___Making_Sure_You_Are_Who_You_Say_You_Are.html
    def loadClassNormal(name: String) = createNormalClassLoader.loadClassInternal(name)
  }

  private val mockClassLoader = createMockClassLoader
  private val normalClassLoader = createNormalClassLoader

  override def loadClass(name: String): Class[_] =
    if (useDefault(name)) {
      defaultClassLoader.loadClass(name)
    } else {
      try {
        mockClassLoader.loadClassInternal(name)
      } catch {
        case _: ClassNotFoundException => loadClassNormal(name)
      }
    }

  def loadClassNormal(name: String) =
    try {
      normalClassLoader.loadClassInternal(name)
    } catch {
      case _: ClassNotFoundException => defaultClassLoader.loadClass(name)
    }

  private def useDefault(name: String) =
    name.startsWith("scala.") || name.startsWith("java.") || name.startsWith("org.scalatest.") || name.startsWith("org.specs2.")
}
