##########################
# delete.stack.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Stack Name: Display name of the stack to delete.
$opts->{stack_name} = q{$[stack_name]};

# Stack ID: ID of the stack to delete.
$opts->{stack_id} = q{$[stack_id]};

# Results location: property path to store server information.
$opts->{location} = q{$[location]};

# Deploy tag: tag to identify this deployment in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->delete_stack();
exit($opts->{exitcode});
