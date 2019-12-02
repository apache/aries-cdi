package org.apache.aries.cdi.spi.configuration;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An event type fired early by Aries CDI allowing extensions who observe it to get
 * container configuration.
 * <p>
 * <pre>
 * private volatile Configuration configuration;
 * void getConfiguration(@Observes Configuration configuration) {
 *   this.configuration = configuration;
 * }
 * </pre>
 */
@ProviderType
public interface Configuration extends Map<String, Object> {
}
