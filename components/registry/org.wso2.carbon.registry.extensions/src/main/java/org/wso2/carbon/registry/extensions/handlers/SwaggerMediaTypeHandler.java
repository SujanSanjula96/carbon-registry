/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.registry.extensions.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.extensions.handlers.utils.SwaggerProcessor;
import org.wso2.carbon.registry.extensions.utils.CommonUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@SuppressWarnings("unused")
public class SwaggerMediaTypeHandler extends Handler {

	private static final Log log = LogFactory.getLog(SwaggerMediaTypeHandler.class);

	/**
	 * Processes the PUT action for swagger files.
	 *
	 * @param requestContext Information about the current request.
	 * @throws RegistryException If fails due a handler specific error
	 */
	@Override public void put(RequestContext requestContext) throws RegistryException {
		//Acquiring the update lock if available.
		if (!CommonUtil.isUpdateLockAvailable()) {
			return;
		}
		CommonUtil.acquireUpdateLock();

		try {
			Resource resource = requestContext.getResource();

			if (resource == null) {
				throw new RegistryException("Resource does not exist.");
			}

			Object resourceContentObj = resource.getContent();

			if (resourceContentObj == null || !(resourceContentObj instanceof byte[])) {
				throw new RegistryException("Resource content is empty.");
			}

			//TODO: CHECK IF RESOURCE EXISTS IN THE RESOURCE PATH AND HANDLE UPDATE API
			InputStream inputStream = new ByteArrayInputStream((byte[]) resourceContentObj);
			SwaggerProcessor processor = new SwaggerProcessor(requestContext);
			processor.processSwagger(inputStream,
			                         getChrootedLocation(requestContext.getRegistryContext()),
			                         null);
			requestContext.setProcessingComplete(true);
		} finally {
			CommonUtil.releaseUpdateLock();
		}
	}

	/**
	 * Creates a resource in the given path by fetching the resource content from the given URL.
	 *
	 * @param requestContext Information about the current request.
	 * @throws RegistryException If import fails due a handler specific error
	 */
	@Override public void importResource(RequestContext requestContext) throws RegistryException {
		//Acquiring the update lock if available.
		if (!CommonUtil.isUpdateLockAvailable()) {
			return;
		}
		CommonUtil.acquireUpdateLock();

		try {
			String sourceURL = requestContext.getSourceURL();

			if (sourceURL == null) {
				throw new RegistryException("Swagger source url is null.");
			}

			InputStream inputStream;
			try {
				if (sourceURL.toLowerCase().startsWith("file:")) {
					throw new RegistryException(
							"The source URL must not be point to a file in the server's local file system. ");
				}
				//Open a stream to the sourceURL
				inputStream = new URL(sourceURL).openStream();
			} catch (IOException e) {
				throw new RegistryException("The URL " + sourceURL + " is incorrect.", e);
			}

			SwaggerProcessor processor = new SwaggerProcessor(requestContext);
			processor.processSwagger(inputStream,
			                         getChrootedLocation(requestContext.getRegistryContext()),
			                         sourceURL);
			requestContext.setProcessingComplete(true);
		} finally {
			CommonUtil.releaseUpdateLock();
		}
	}

	/**
	 * Returns the root location of the Swagger.
	 *
	 * @param registryContext Registry context
	 * @return the root location of the Swagger.
	 */
	private String getChrootedLocation(RegistryContext registryContext) {
		String relativeLocation = "/apimgt/applicationdata/api-docs/";
		return RegistryUtils.getAbsolutePath(registryContext,
		                                     RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
		                                     relativeLocation);
	}

}
