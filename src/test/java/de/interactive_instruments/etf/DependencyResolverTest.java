/**
 * Copyright 2017-2019 European Union, interactive instruments GmbH
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.testdriver.CyclicDependencyException;
import de.interactive_instruments.etf.testdriver.DependencyGraph;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DependencyResolverTest {

    @Test
    public void testDependencyResolving() throws StorageException, ObjectWithIdNotFoundException, CyclicDependencyException {

        final DependencyGraph<ExecutableTestSuiteDto> dependencyResolver = new DependencyGraph();

        final ExecutableTestSuiteDto ets1 = TestUtils.createEts(1);
        final ExecutableTestSuiteDto ets2 = TestUtils.createEts(2);
        final ExecutableTestSuiteDto ets3 = TestUtils.createEts(3);
        final ExecutableTestSuiteDto ets4 = TestUtils.createEts(4);
        final ExecutableTestSuiteDto ets5 = TestUtils.createEts(5);
        final ExecutableTestSuiteDto ets6 = TestUtils.createEts(6);
        final ExecutableTestSuiteDto ets7 = TestUtils.createEts(7);

        ets1.addDependency(ets2);
        ets1.addDependency(ets3);
        ets1.addDependency(ets6);

        ets2.addDependency(ets4);
        ets2.addDependency(ets3);

        ets3.addDependency(ets4);
        ets3.addDependency(ets5);

        ets4.addDependency(ets6);

        ets5.addDependency(ets7);

        ets6.addDependency(ets7);

        final ArrayList<ExecutableTestSuiteDto> executableTestSuiteDtos = new ArrayList<ExecutableTestSuiteDto>() {
            {
                add(ets1);
                add(ets2);
                add(ets3);
                add(ets4);
                add(ets5);
                add(ets6);
                add(ets7);
            }
        };

        dependencyResolver.addAllDependencies(executableTestSuiteDtos);
        final List<ExecutableTestSuiteDto> sorted = dependencyResolver.sort();

        assertNotNull(sorted);
        assertEquals(7, sorted.size());

        Assert.assertEquals("ETS.7", sorted.get(6).getLabel());
        Assert.assertEquals("ETS.5", sorted.get(5).getLabel());
        Assert.assertEquals("ETS.6", sorted.get(4).getLabel());
        Assert.assertEquals("ETS.4", sorted.get(3).getLabel());
        Assert.assertEquals("ETS.3", sorted.get(2).getLabel());
        Assert.assertEquals("ETS.2", sorted.get(1).getLabel());
        Assert.assertEquals("ETS.1", sorted.get(0).getLabel());

    }

    @Test(expected = CyclicDependencyException.class)
    public void testCycleDetection() throws StorageException, ObjectWithIdNotFoundException, CyclicDependencyException {
        final DependencyGraph<ExecutableTestSuiteDto> dependencyResolver = new DependencyGraph();

        final ExecutableTestSuiteDto ets1 = TestUtils.createEts(1);
        final ExecutableTestSuiteDto ets2 = TestUtils.createEts(2);
        final ExecutableTestSuiteDto ets3 = TestUtils.createEts(3);
        final ExecutableTestSuiteDto ets4 = TestUtils.createEts(4);

        // Cycle
        ets1.addDependency(ets2);
        ets2.addDependency(ets3);
        ets3.addDependency(ets4);
        ets4.addDependency(ets1);

        final ArrayList<ExecutableTestSuiteDto> executableTestSuiteDtos = new ArrayList<ExecutableTestSuiteDto>() {
            {
                add(ets1);
                add(ets2);
                add(ets3);
                add(ets4);
            }
        };

        dependencyResolver.addAllDependencies(executableTestSuiteDtos);
        dependencyResolver.sort();
    }

    @Test
    public void testIgnoreCycles() throws StorageException, ObjectWithIdNotFoundException, CyclicDependencyException {
        final DependencyGraph<ExecutableTestSuiteDto> dependencyResolver = new DependencyGraph();

        final ExecutableTestSuiteDto ets1 = TestUtils.createEts(1);
        final ExecutableTestSuiteDto ets2 = TestUtils.createEts(2);
        final ExecutableTestSuiteDto ets3 = TestUtils.createEts(3);
        final ExecutableTestSuiteDto ets4 = TestUtils.createEts(4);
        final ExecutableTestSuiteDto ets5 = TestUtils.createEts(5);
        final ExecutableTestSuiteDto ets6 = TestUtils.createEts(6);
        final ExecutableTestSuiteDto ets7 = TestUtils.createEts(7);

        // Cycle
        ets1.addDependency(ets2);
        ets2.addDependency(ets3);
        ets3.addDependency(ets4);
        ets4.addDependency(ets1);

        ets5.addDependency(ets6);

        ets7.addDependency(ets1);

        final ArrayList<ExecutableTestSuiteDto> executableTestSuiteDtos = new ArrayList<ExecutableTestSuiteDto>() {
            {
                add(ets1);
                add(ets2);
                add(ets3);
                add(ets4);

                add(ets5);
                add(ets6);

                add(ets7);
            }
        };

        dependencyResolver.addAllDependencies(executableTestSuiteDtos);
        final List<ExecutableTestSuiteDto> sortedList = dependencyResolver.sortIgnoreCylce();
        System.out.println(sortedList);

    }
}
