##########################
# create.image.pl
##########################
use warnings;
use strict;
use Encode;
use utf8;
use open IO => ':encoding(utf8)';

my $opts;

# Configuration: A commander configuration previously created.
$opts->{connection_config} = q{$[connection_config]};

# Name: Display name for the new image.
$opts->{name} = q{$[name]};

# Disk Format: Disk format for the new image.
$opts->{disk_format} = q{$[disk_format]};

# Container Format: Container format for the new image.
$opts->{container_format} = q{$[container_format]};

# is_local ? : If checked, indicates the source image location is local,otherwise the source image location is url.
$opts->{is_local} = q{$[is_local]};

# Image Path: Path of the image on local machine.
$opts->{image_path} = q{$[image_path]};

# Size: Size of raw image file from which new image will be created..
$opts->{size} = q{$[size]};

# Checksum: Checksum value of the raw image file.
$opts->{checksum} = q{$[checksum]};

# Min-ram: Minimum ram requirement in MBs to run this image on a server.
$opts->{min_ram} = q{$[min_ram]};

# Min-disk: Minimum disk space requirement in GBs to run this image on a server.
$opts->{min_disk} = q{$[min_disk]};

# Owner: The ID of the owner, or tenant, of the image..
$opts->{owner_name} = q{$[owner_name]};

# Results location: property path to store information.
$opts->{location} = q{$[location]};

# Results tag: tag to identify this job in the resource location
$opts->{tag} = q{$[tag]};

$[/myProject/procedure_helpers/preamble]

$openstack->create_image();
exit($opts->{exitcode});
