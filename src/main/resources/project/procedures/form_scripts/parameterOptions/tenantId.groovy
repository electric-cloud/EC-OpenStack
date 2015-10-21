/*
 *  Copyright 2015 Electric Cloud, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import groovy.transform.Field
import com.electriccloud.domain.FormalParameterOptionsResult

@Field
final String CONFIGURATION_NAME = "connection_config"

@Field
final String CONFIGURATION_NAME_FROM_CONFIG = "config"

@Field
final String TENANT_ID_FROM_CONFIG = "tenant_id"

def result = new FormalParameterOptionsResult()

// If configs are the same,
// Or if using the passed in config (in which case, the configurationParameters does not have the config name)
// Then use the config tenant_id if set
if (args?.parameters &&
        args.parameters[CONFIGURATION_NAME] &&
        args.configurationParameters &&
        (args.parameters[CONFIGURATION_NAME] == args.configurationParameters[CONFIGURATION_NAME_FROM_CONFIG] ||
            !args.configurationParameters[CONFIGURATION_NAME_FROM_CONFIG])
    ) {

    if (args.configurationParameters[TENANT_ID_FROM_CONFIG]) {
        result.add(args.configurationParameters[TENANT_ID_FROM_CONFIG], args.configurationParameters[TENANT_ID_FROM_CONFIG])
    }
}

result