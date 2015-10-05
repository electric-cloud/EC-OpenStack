##########################
# reboot.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Server ID: Id for the server to reboot.
$opts->{server_id} = q{$[server_id]};

# Reboot Type: Type of the reboot action (hard/soft).
$opts->{reboot_type} = q{$[reboot_type]};

$[/myProject/procedure_helpers/preamble]

$openstack->reboot();
exit($opts->{exitcode});
