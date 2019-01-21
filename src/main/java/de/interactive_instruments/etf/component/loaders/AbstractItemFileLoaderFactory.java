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
package de.interactive_instruments.etf.component.loaders;

import java.util.concurrent.ConcurrentHashMap;

import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public abstract class AbstractItemFileLoaderFactory<T extends Dto>
        implements ItemFileLoaderFactory, ItemFileLoaderResultListener<T> {

    private final EidMap<T> items = new DefaultEidMap<>(new ConcurrentHashMap<>());
    protected LoadingContext loadingContext = NullLoadingContext.instance();

    public void setContextLoader(final LoadingContext loadingContext) {
        this.loadingContext = loadingContext;
    }

    protected ItemRegistry getItemRegistry() {
        return loadingContext.getItemRegistry();
    }

    @Override
    public final void eventItemBuilt(final T dto) {
        items.put(dto.getId(), dto);
    }

    @Override
    public final void eventItemDestroyed(final EID id) {
        items.remove(id);
    }

    @Override
    public final void eventItemUpdated(final T dto) {
        items.put(dto.getId(), dto);
    }

    protected final EidMap<T> getItems() {
        return items.unmodifiable();
    }
}
