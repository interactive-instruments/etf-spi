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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public abstract class AbstractPreparedDtoCollection<T extends Dto> implements PreparedDtoCollection<T> {

    protected final DtoResolver<T> dao;
    protected final EidMap<Dto> map;
    protected boolean resolvedAll = false;

    protected AbstractPreparedDtoCollection(final DtoResolver<T> resolver, final EidMap<Dto> map) {
        this.dao = resolver;
        this.map = map;
    }

    private void resolveAll() {
        if (!resolvedAll) {
            final Collection<T> items;
            try {
                items = dao.getByIds(map.keySet());
            } catch (StorageException | ObjectWithIdNotFoundException e) {
                throw new IllegalStateException("Error fetching items", e);
            }
            items.forEach(i -> {
                map.put(i.getId(), i);
            });
            resolvedAll = true;
        }
    }

    private T resolveSingle(final EID eid) {
        try {
            return dao.getById(eid);
        } catch (StorageException | ObjectWithIdNotFoundException e) {
            throw new IllegalStateException("Error fetching items", e);
        }
    }

    @Override
    public Iterator<T> iterator() {
        resolveAll();
        return (Iterator<T>) map.values().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        resolveAll();
        return map.containsValue(value);
    }

    @Override
    public T get(final Object key) {
        T dto = (T) map.get(key);
        if (dto == null) {
            dto = resolveSingle((EID) key);
            map.put((EID) key, dto);
        }
        return dto;
    }

    @Override
    public Set<EID> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<T> values() {
        resolveAll();
        return (Collection<T>) map.values();
    }

    @Override
    public Set<Map.Entry<EID, T>> entrySet() {
        resolveAll();
        final Set<? extends Map.Entry<?, ?>> set = map.entrySet();
        return (Set<Map.Entry<EID, T>>) set;
    }
}
