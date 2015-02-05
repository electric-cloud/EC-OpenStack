##########################
# create.stack.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Tenant: Id of the tenant.
$opts->{tenant_id} = q{$[tenant_id]};

# Stack Name: Display name for the new stack.
$opts->{stack_name} = q{$[stack_name]};

# Template: The stack template to instantiate.
$opts->{template} = q{$[template]};

# Template URL: A URI to the location containing the stack template to instantiate. 
$opts->{template_url} = q{$[template_url]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->create_stack();
exit($opts->{exitcode});
