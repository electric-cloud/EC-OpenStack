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

# Name: Name for the keypair.
$opts->{keyname} = q{$[keyname]};

$[/myProject/procedure_helpers/preamble]

$openstack->delete_key_pair();
exit($opts->{exitcode});
