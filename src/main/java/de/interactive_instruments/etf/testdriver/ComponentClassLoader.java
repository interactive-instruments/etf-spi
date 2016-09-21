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
import java.io.InputStream;
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

	private static class ParentDelegationClassClassLoader extends ClassLoader {
		private final Logger logger;

		ParentDelegationClassClassLoader(final ClassLoader parent, final Logger logger) {
			super(parent);
			this.logger=logger;
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			logger.trace("{} : Loading of class {} failed, trying parent class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.findClass(name);
		}

		@Override
		public URL getResource(String name) {
			logger.trace("{} : Loading of resource {} failed, trying parent class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.getResource(name);
		}

		@Override public InputStream getResourceAsStream(final String name) {
			logger.trace("{} : Loading of resource {} failed, trying parent class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.getResourceAsStream(name);
		}
	}

	private static class ChildURLClassLoader extends URLClassLoader {
		private final ParentDelegationClassClassLoader realParent;
		private final Logger logger;

		ChildURLClassLoader(final URL[] urls, final ParentDelegationClassClassLoader realParent, final Logger logger) {
			super(urls, null);
			this.realParent = realParent;
			this.logger=logger;
		}

		@Override
		public Class<?> findClass(final String name) throws ClassNotFoundException {
			try {
				logger.trace("{} : Trying to load class {}", this.getClass().getSimpleName(), name);
				final Class<?> loaded = super.findLoadedClass(name);
				if( loaded != null ) {
					// Already defined
					return loaded;
				}
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				return realParent.loadClass(name);
			}
		}

		@Override
		public URL getResource(String name) {
			logger.trace("{} : Trying to load resource {}", this.getClass().getSimpleName(), name);
			final URL resource = super.getResource(name);
			if(resource!=null) {
				return resource;
			}else{
				return realParent.getResource(name);
			}
		}

		@Override public InputStream getResourceAsStream(final String name) {
			logger.trace("{} : Trying to load resource {}", this.getClass().getSimpleName(), name);
			final InputStream stream = super.getResourceAsStream(name);
			if(stream!=null) {
				return stream;
			}else{
				return realParent.getResourceAsStream(name);
			}
		}
	}

	ComponentClassLoader(List<URL> classpath) {
		super(Thread.currentThread().getContextClassLoader());
		final URL[] urls = classpath.toArray(new URL[classpath.size()]);
		childClassLoader = new ChildURLClassLoader(urls,
				new ParentDelegationClassClassLoader(this.getParent(),logger),logger);
	}

	@Override
	protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		try {
			logger.trace("{} : Trying to load class {}", this.getClass().getSimpleName(), name);
			return childClassLoader.findClass(name);
		} catch (ClassNotFoundException e) {
			logger.trace("{} : Loading of class {} failed, trying class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.loadClass(name, resolve);
		}
	}

	@Override
	public URL getResource(String name) {
		logger.trace("{} : Trying to load resource {}", this.getClass().getSimpleName(), name);
		final URL resource = childClassLoader.getResource(name);
		if(resource!=null) {
			return resource;
		}else{
			logger.trace("{} : Loading of resource {} failed, trying class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.getResource(name);
		}
	}

	@Override public InputStream getResourceAsStream(final String name) {
		logger.trace("{} : Trying to load resource {}", this.getClass().getSimpleName(), name);
		final InputStream stream = childClassLoader.getResourceAsStream(name);
		if(stream!=null) {
			return stream;
		}else{
			logger.trace("{} : Loading of resource {} failed, trying class loader {}",
					this.getClass().getSimpleName(), name, super.getClass().getSimpleName());
			return super.getResourceAsStream(name);
		}
	}

	@Override
	public void close() {
		IFile.closeQuietly(this.childClassLoader);
	}
}
