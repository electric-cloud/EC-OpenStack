import groovy.transform.Field
import com.electriccloud.domain.FormalParameterOptionsResult

@Field
final String CONFIGURATION_NAME = "connection_config"

@Field
final String CONFIGURATION_NAME_FROM_CONFIG = "config"

@Field
final String TENANT_ID_FROM_CONFIG = "tenant_id"

def result = new FormalParameterOptionsResult()

// If configs are the same, use the config tenant_id if set
if (args?.parameters &&
        args.parameters[CONFIGURATION_NAME] &&
        args.parameters[CONFIGURATION_NAME] == args.configurationParameters[CONFIGURATION_NAME_FROM_CONFIG]) {

    if (args.configurationParameters[TENANT_ID_FROM_CONFIG]) {
        result.add(args.configurationParameters[TENANT_ID_FROM_CONFIG], args.configurationParameters[TENANT_ID_FROM_CONFIG])
    }
}

result