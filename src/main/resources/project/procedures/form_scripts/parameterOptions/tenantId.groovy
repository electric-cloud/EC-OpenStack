import groovy.transform.Field
import com.electriccloud.domain.FormalParameterOptionsResult

@Field
final String CONFIGURATION_NAME = "connection_config"

@Field
final String AUGMENTED_ATTR_PREFIX = "augmentedAttr_"

@Field
final String CONFIGURATION_NAME_FROM_CONFIG = AUGMENTED_ATTR_PREFIX + "config"

@Field
final String TENANT_ID_FROM_CONFIG = AUGMENTED_ATTR_PREFIX + "tenant_id"

def result = new FormalParameterOptionsResult()

// If configs are the same, use the config tenant_id if set
if (args.parameters[CONFIGURATION_NAME] &&
        args.parameters[CONFIGURATION_NAME] == args.parameters[CONFIGURATION_NAME_FROM_CONFIG]) {
    if (args.parameters[TENANT_ID_FROM_CONFIG]) {
        result.add(args.parameters[TENANT_ID_FROM_CONFIG], args.parameters[TENANT_ID_FROM_CONFIG])
    }
}

result