use ElectricCommander;

push (@::gMatchers,
  {
        id =>          "instanceterminated",
        pattern =>     q{^Instance\s(.+)\shas\sbeen\sterminated.},
        action =>           q{
         
                              my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                              $desc .= "Instance $1 has been terminated.";
                              
                              setProperty("summary", $desc . "\n");
                             },
  },
  {
        id =>          "deletekeypair",
        pattern =>     q{^KeyPair\s(.+)\sdeleted},
        action =>           q{
         
                              my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                              $desc .= "KeyPair \'$1\' deleted.";
                              
                              setProperty("summary", $desc . "\n");
                             },
  },
  
   {
        id =>          "createkeypair",
        pattern =>     q{^KeyPair\s(.+)\screated.},
        action =>           q{
         
                              my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                              $desc .= "KeyPair \'$1\' created.";
                              
                              setProperty("summary", $desc . "\n");
                             },
  },
  {
        id =>          "createserver",
        pattern =>     q{^Server\s(.+)\sdeployed.},
        action =>           q{
         
                              my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                              $desc .= "Server \'$1\' deployed.";
                              
                              setProperty("summary", $desc . "\n");
                             },
  },
  {
        id =>          "createvolume",
        pattern =>     q{^Volume\s(.+)\screated.},
        action =>           q{

                                my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');
                                $desc .= "Volume \'$1\' created.";
                                setProperty("summary", $desc . "\n");
                             },
  },
  {
        id =>          "attachvolume",
        pattern =>     q{^Volume\s(.+)\sattached to server},
        action =>           q{

                                my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');
                                $desc .= "Volume \'$1\' attached.";
                                setProperty("summary", $desc . "\n");
                             },
  }    
);
