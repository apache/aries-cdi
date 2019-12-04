package org.apache.aries.cdi.extension.mp.metrics;

import javax.json.spi.JsonProvider;

import org.apache.johnzon.core.JsonProviderImpl;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(JsonProvider.class)
@SuppressWarnings("serial")
public class StubJsonProvider extends JsonProviderImpl {
}
