/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.testdriver;

import de.interactive_instruments.IFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * A class loader used for the test components.
 *
 * This CL does not search for the loaded class by calling the parent class loader
 * (default Java RT ClassLoader behaviour) but tries to load the class as Jar from
 * its children URL ClassLoader first.
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
final class ComponentClassLoader extends ClassLoader implements Closeable {

	private final Logger logger = LoggerFactory.getLogger(ComponentClassLoader.class);
	private final ChildURLClassLoader childClassLoader;

	private class FindClassClassLoader extends ClassLoader {
		public FindClassClassLoader(final ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			logger.trace("{} : Loading of class {} failed, trying class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.findClass(name);
		}
	}

	private class ChildURLClassLoader extends URLClassLoader {
		private FindClassClassLoader realParent;

		public ChildURLClassLoader(final URL[] urls, final FindClassClassLoader realParent) {
			super(urls, null);

			this.realParent = realParent;
		}

		@Override
		public Class<?> findClass(final String name) throws ClassNotFoundException {
			try {
				// first try to use the URLClassLoader findClass
				logger.trace("{} : Trying to load class {}", this.getClass().getSimpleName(), name);
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				// if that fails, we ask our real parent classloader to load the class (we give up)
				logger.trace("{} : Loading of class {} failed, trying class loader {}",
						this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
				return realParent.loadClass(name);
			}
		}
	}

	ComponentClassLoader(List<URL> classpath) {
		super(Thread.currentThread().getContextClassLoader());
		final URL[] urls = classpath.toArray(new URL[classpath.size()]);
		childClassLoader = new ChildURLClassLoader(urls, new FindClassClassLoader(this.getParent()));
	}

	@Override
	protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		try {
			// first we try to find a class inside the child classloader
			logger.trace("{} : Trying to load class {}", this.getClass().getSimpleName(), name);
			return childClassLoader.findClass(name);
		} catch (ClassNotFoundException e) {
			// didn't find it, try the parent
			logger.trace("{} : Loading of class {} failed, trying class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.loadClass(name, resolve);
		}
	}

	@Override
	public void close() {
		IFile.closeQuietly(this.childClassLoader);
	}
}
