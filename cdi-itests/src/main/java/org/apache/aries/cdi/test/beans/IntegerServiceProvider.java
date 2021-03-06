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

package org.apache.aries.cdi.test.beans;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.propertytypes.ServiceRanking;

@ApplicationScoped
public class IntegerServiceProvider {

	private final SecureRandom random = new SecureRandom();

	protected IntegerServiceProvider() throws NoSuchAlgorithmException {
		// no-op: a normal scoped bean MUST have a default constructor to let container create a proxy
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(5000)
	Integer int1() {
		return random.nextInt();
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(12000)
	Integer int2() {
		return random.nextInt();
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(1000)
	Integer int3() {
		return random.nextInt();
	}

	@Produces
	@Service(Integer.class)
	@ServiceRanking(100000)
	Integer int4 = random.nextInt();

}
