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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.DependencyHolder;

/**
 * Walks through a directory and creates DTO types with the associated {@link TypeBuilder}s. Supports type dependency resolution.
 *
 * Building is realized with the Chain of responsibility pattern, see {@link TypeBuildingFileVisitor#visitFile(Path, BasicFileAttributes)}.
 *
 * @author herrmann@interactive-instruments.de.
 */
public final class TypeBuildingFileVisitor implements FileVisitor<Path> {

	@FunctionalInterface
	public interface TypeBuilder<T extends Dto> {

		/**
		 * Try to build the type and return the type on success
		 *
		 * @param file to process
		 * @return Type if responsible, null otherwise
		 */
		TypeBuilderCmd<T> prepare(final Path file);
	}

	public abstract static class TypeBuilderCmd<T extends Dto> implements DependencyHolder<TypeBuilderCmd<T>> {
		protected final Path path;
		protected String id;
		// HashMap to allow null values -which are overwritten with setKnownBuilder()
		private final HashMap<String, TypeBuilderCmd<T>> dependencies = new HashMap<>();

		protected TypeBuilderCmd(final Path path) {
			this.path = path;
		}

		protected final String getId() {
			return id;
		}

		protected abstract T build();

		protected void dependsOn(final String id) {
			dependencies.put(id, null);
		}

		@Override final public Collection<TypeBuilderCmd<T>> getDependencies() {
			return dependencies!=null ? dependencies.values() : null;
		}

		final void removeAlreadyBuildedDependencies(final Set<String> skipIds) {
			dependencies.keySet().removeAll(skipIds);
		}

		final void setKnownBuilders(final  Map<String, TypeBuilderCmd> builders) {
			dependencies.entrySet().forEach( e -> {
						final TypeBuilderCmd<T> builder = builders.get(e.getKey());
						e.setValue(Objects.requireNonNull(builder,
								"Reference Object with ID "+e.getKey()+", defined in "+ path +" not found!"));
			});
		}
	}

	private final List<TypeBuilder<? extends Dto>> typeBuilders;
	private final Set<String> skipIds;

	// ID -> TypeBuilderCmd, tree map for faster iteration
	private final Map<String, TypeBuilderCmd<? extends Dto>> typeBuilderCmds = new TreeMap<>();

	/**
	 * Creates a new TypeBuildingFileVisitor
	 *
	 * Types are created with the  {@link TypeBuildingFileVisitor#buildAll()} method.
	 *
	 * @param builders List of Type Builders
	 * @param skipIds IDs to skip as String set or null
	 */
	public TypeBuildingFileVisitor(final List<TypeBuilder<? extends Dto>> builders, final Set<String> skipIds) {
		this.typeBuilders = Objects.requireNonNull(builders);
		this.skipIds = skipIds;
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		if(dir.getFileName().toString().startsWith(".")) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
		for (final TypeBuilder builder : typeBuilders) {
			final TypeBuilderCmd<? extends Dto> typeBuilderCmd = builder.prepare(file);
			if (typeBuilderCmd != null) {
				final TypeBuilderCmd duplicateCheck = typeBuilderCmds.get(
						Objects.requireNonNull(typeBuilderCmd.getId(), "TypeBuilder "+typeBuilderCmd+" returned NULL ID"));
				if(duplicateCheck!=null) {
					throw new IllegalStateException("Type with ID \"" + typeBuilderCmd.getId() +
							"\" has already been created during this refresh run of the Test Driver. "
							+ "Types with duplicate Ids created from path \"" +
							file + "\" and path \"" + duplicateCheck.path + "\" !");
				}
				typeBuilderCmds.put(typeBuilderCmd.getId(), typeBuilderCmd);
				break;
			}
		}
		return FileVisitResult.CONTINUE;
	}

	/**
	 * Build and return all types
	 *
	 * @return map containing path paths as keys and DTOs as values
	 */
	public Map<Path, Dto> buildAll() {
		if(typeBuilderCmds.isEmpty()) {
			return null;
		}
		final Collection<TypeBuilderCmd<? extends Dto>> typeBuilderCmdColl = typeBuilderCmds.values();

		// Remove builders for already build types
		if(skipIds!=null) {
			typeBuilderCmdColl.forEach(b -> b.removeAlreadyBuildedDependencies(skipIds));
		}

		// Set all known builders
		final Map<String, TypeBuilderCmd> builders = Collections.unmodifiableMap(typeBuilderCmds);
		typeBuilderCmdColl.forEach( b -> b.setKnownBuilders(builders));

		// Create a dependency graph and build the types in the right order
		final DependencyGraph dependencyGraph = new DependencyGraph(typeBuilderCmdColl);
		final List<TypeBuilderCmd<? extends Dto>> orderedTypeBuilderCmds = dependencyGraph.sort();
		final Map<Path, Dto> buildTypes = new TreeMap<>();
		for (int i = orderedTypeBuilderCmds.size() - 1; i >= 0; i--) {
			final Dto dto = orderedTypeBuilderCmds.get(i).build();
			buildTypes.put(orderedTypeBuilderCmds.get(i).path.toAbsolutePath(), dto);
		}
		return buildTypes;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}
}
