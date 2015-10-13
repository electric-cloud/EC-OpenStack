import com.electriccloud.domain.FormalParameterOptionsResult

def result = new FormalParameterOptionsResult()

// Add all available resource zones
getZones().sort{it.name}.each{ zone ->
    result.add(zone.name, zone.name)
}

result
