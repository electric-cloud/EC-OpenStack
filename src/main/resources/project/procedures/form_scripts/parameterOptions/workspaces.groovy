import com.electriccloud.domain.FormalParameterOptionsResult

def result = new FormalParameterOptionsResult()

getWorkspaces().sort{it.name}.each{ workspace ->
    result.add(workspace.name, workspace.name)
}

result
