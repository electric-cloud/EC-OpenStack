import com.electriccloud.domain.FormalParameterOptionsResult

def result = new FormalParameterOptionsResult()

// Add all available resources
getResources().sort{it.name}.each{ resource ->
    result.add(resource.name, resource.name)
}

// Then add all available resource pools
getResourcePools().sort{it.name}.each{ resource ->
    result.add(resource.name, resource.name + ' (Pool)')
}

result