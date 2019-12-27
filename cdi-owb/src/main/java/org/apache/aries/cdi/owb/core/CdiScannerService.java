/**
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

package org.apache.aries.cdi.owb.core;

import static java.util.Collections.emptySet;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.corespi.se.DefaultBDABeansXmlScanner;
import org.apache.webbeans.spi.BDABeansXmlScanner;
import org.apache.webbeans.spi.ScannerService;

// todo: bda support
@SuppressWarnings("deprecation")
public class CdiScannerService implements ScannerService {
	private final Set<Class<?>> classes;
	private final Set<URL> beansXml;
	private BDABeansXmlScanner bdaBeansXmlScanner = new DefaultBDABeansXmlScanner();

	public CdiScannerService(final Set<Class<?>> beanClassNames,
							final Collection<URL> beansXml) {
		this.classes = beanClassNames;
		this.beansXml = beansXml == null ? emptySet() : new HashSet<>(beansXml);
	}

	@Override
	public void init(final Object object) {
		// no-op
	}

	@Override
	public void scan() {
		// we already scanned
	}

	@Override
	public void release() {
		// no-op
	}

	@Override
	public Set<URL> getBeanXmls() {
		return beansXml;
	}

	@Override
	public Set<Class<?>> getBeanClasses() {
		return classes;
	}

	@Override
	public boolean isBDABeansXmlScanningEnabled() {
		return false;
	}

	@Override
	public BDABeansXmlScanner getBDABeansXmlScanner() {
		return bdaBeansXmlScanner;
	}

}