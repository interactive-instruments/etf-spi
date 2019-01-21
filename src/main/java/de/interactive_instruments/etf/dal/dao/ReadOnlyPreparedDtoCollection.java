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
package de.interactive_instruments.etf.dal.dao;

import java.util.Map;
import java.util.Set;

import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;

/**
 *
 * A LazyLoad reference map
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public abstract class ReadOnlyPreparedDtoCollection<T extends Dto> extends AbstractPreparedDtoCollection<T> {

    public ReadOnlyPreparedDtoCollection(final DtoResolver<T> resolver, final Set<EID> ids) {
        super(resolver, new DefaultEidMap<>());
        ids.forEach(eid -> map.put(eid, null));
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public Dto put(final EID key, final Dto value) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public T remove(final Object key) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public void putAll(final Map<? extends EID, ? extends T> m) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
    }

    /**
     * Unsupported operation
     *
     * @throws UnsupportedOperationException
     *             always
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " is read only");
    }

}
