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

import de.interactive_instruments.etf.model.DependencyHolder;

import java.util.*;

/**
 * A dependency graph which detects cycles and returns a list in topological order
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class DependencyGraph<T extends DependencyHolder<T>> {

	// use linked hash map for deterministic results
	private final Map<T, Set<T>> dependencyNodes = new LinkedHashMap<>();

	public DependencyGraph() {
	}

	public DependencyGraph(final Collection<T> nodes) {
		addAllDependencies(nodes);
	}

	/**
	 * Add an object which has dependencies
	 *
	 * @param node
	 */
	public void addDependency(final T node) {
		Objects.requireNonNull("Dependency node is null");
		addNode(node);
		final Set<T> cycleCheck = new HashSet<>();
		cycleCheck.add(node);
		walkAndAddDependencies(node, cycleCheck);
	}

	private void walkAndAddDependencies(final T node, final Set<T> cycleCheck) {
		if(node.getDependencies()!=null) {
			for (final T dep : node.getDependencies()) {
				addNode(dep);
				addEdge(node, dep);
			}
			node.getDependencies().stream().filter(dep ->
					!cycleCheck.contains(dep)).forEach(dep -> {
						cycleCheck.add(dep);
						walkAndAddDependencies(dep, cycleCheck);
			});
		}
	}


	/**
	 * Add a collection of objects that have dependencies
	 *
	 * @param nodes
	 */
	public void addAllDependencies(final Collection<T> nodes) {
		Objects.requireNonNull("Dependencies are null");
		nodes.forEach(this::addDependency);
	}

	private boolean addNode(final T node) {
		if(dependencyNodes.containsKey(node)) {
			return false;
		}
		dependencyNodes.put(node, new HashSet<>());
		return true;
	}

	private void addEdge(final T source, final T dest) {
		if (!dependencyNodes.containsKey(source) || !dependencyNodes.containsKey(dest)) {
			throw new NoSuchElementException("Source and destination dependency node must exist in dependency graph");
		}
		dependencyNodes.get(source).add(dest);
	}

	private Set<T> edgesFrom(final T node) {
		final Set<T> arcs = dependencyNodes.get(node);
		if (arcs == null)
			throw new NoSuchElementException("Source dependency node does not exist");
		return Collections.unmodifiableSet(arcs);
	}

	/**
	 * Starts a topological sort of the dependencies and returns an ordered list.
	 *
	 * The index n-object, is the one object that does not depend on other objects.
	 *
	 * @return
	 */
	public List<T> sort() {
		final DependencyGraph<T> reversesGraph = reverseGraph();

		final List<T> orderedResult = new ArrayList<>(dependencyNodes.size());
		final Set<T> cycleCheck = new HashSet<>();
		final Set<T> expandedNodes = new HashSet<>();

		reversesGraph.dependencyNodes.keySet().forEach( n ->
				deepSearch(n, reversesGraph, orderedResult, cycleCheck, expandedNodes) );

		return Collections.unmodifiableList(orderedResult);
	}

	private DependencyGraph<T> reverseGraph() {
		final DependencyGraph<T> result = new DependencyGraph<T>();
		this.dependencyNodes.keySet().forEach(result::addNode);

		this.dependencyNodes.keySet().forEach( n ->
				edgesFrom(n).forEach( e ->
						result.addEdge(e, n) ) );

		return result;
	}

	private void deepSearch(final T node, final DependencyGraph<T> reversesGraph,
			final List<T> orderedResult, final Set<T> cycleCheck,
			final Set<T> expandedNodes) {
		if (cycleCheck.contains(node)) {
			if (expandedNodes.contains(node)) {
				// already expanded
				return;
			}
			throw new IllegalStateException("Graph contains a cycle.");
		}
		cycleCheck.add(node);

		reversesGraph.edgesFrom(node).forEach( n ->
				deepSearch(n, reversesGraph, orderedResult, cycleCheck, expandedNodes) );

		orderedResult.add(node);
		expandedNodes.add(node);
	}

}
