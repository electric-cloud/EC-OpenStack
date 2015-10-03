##########################
# update.stack.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Stack Name: Display name of the stack to update.
$opts->{stack_name} = q{$[stack_name]};

# Stack ID: ID of the stack to update.
$opts->{stack_id} = q{$[stack_id]};

# Template: Updated stack template.
$opts->{template} = q{$[template]};

# Template URL: A URI to the location containing the stack template to instantiate. 
$opts->{template_url} = q{$[template_url]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->update_stack();
exit($opts->{exitcode});
