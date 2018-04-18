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

package org.apache.aries.cdi.test.tb10;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.BindServiceReference;

@Service
@SingleComponent
public class SingleReferenceEventHandler implements Pojo {

	@Inject
	void integers(BindServiceReference<Integer> binder) {
		binder.adding(
			sr -> {
				System.out.println("=====ADDING==>>> " + sr + " " + SingleReferenceEventHandler.this);

				_services.put(sr, "ADDED");
			}
		).modified(
			sr -> {
				System.out.println("=====UPDATING==>>> " + sr + " " + SingleReferenceEventHandler.this);

				_services.put(sr, "UPDATED");
			}
		).removed(
			sr -> {
				System.out.println("=====REMOVING==>>> " + sr + " " + SingleReferenceEventHandler.this);

				_services.remove(sr);
			}
		).bind();
	}

	@Override
	public String foo(String fooInput) {
		return _services.values().toString();
	}

	@Override
	public int getCount() {
		return _services.size();
	}

	private final Map<ServiceReference<Integer>, String> _services = new ConcurrentSkipListMap<>();

}
