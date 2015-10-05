##########################
# create.key.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Name: Name for the new kewypair.
$opts->{keyname} = q{$[keyname]};

# Results location: property path to store information.
$opts->{location} = q{$[location]};

# Results tag: tag to identify this job in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->create_key_pair();
exit($opts->{exitcode});
