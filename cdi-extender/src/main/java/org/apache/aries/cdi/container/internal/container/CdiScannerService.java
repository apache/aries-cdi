package org.apache.aries.cdi.container.internal.container;

import static java.util.Collections.*;

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