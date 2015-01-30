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
  },
  {
          id =>          "detachvolume",
          pattern =>     q{^Volume\sdetached\sfrom\sserver\s(.+)\ssuccessfully.},
          action =>           q{

                               my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                               $desc .= "Volume detached from server \'$1\'.";

                               setProperty("summary", $desc . "\n");
                               },
  },
  {
            id =>          "deletevolume",
            pattern =>     q{^Volume\s(.+)\sdeleted},
            action =>           q{

                                 my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                 $desc .= "Volume \'$1\' deleted.";

                                 setProperty("summary", $desc . "\n");
                                 },
    },
    {
            id =>          "rebootinstance",
            pattern =>     q{^Server\s(.+)\srebooted successfully.Reboot type :\s(.+)},
            action =>           q{

                                 my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                 $desc .= "Server \'$1\' rebooted.";

                                 setProperty("summary", $desc . "\n");
                                 },
    },
    {
             id =>          "createvolumesnapshot",
             pattern =>     q{^Snapshot\s(.+)\screated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Snapshot \'$1\' created.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "createimage",
             pattern =>     q{^Image\s(.+)\screated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Image \'$1\' created.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "createinstancesnapshot",
             pattern =>     q{^Snapshot\sof\sinstance\s(.+)\screated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Snapshot \'$1\' created.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "createstack",
             pattern =>     q{^Stack\s(.+)\screated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Stack \'$1\' created.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "updatestack",
             pattern =>     q{^Stack\s(.+)\supdated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Stack \'$1\' updated.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "deletestack",
             pattern =>     q{^Stack\s(.+)\sdeleted.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc .= "Stack \'$1\' deleted.";

                                  setProperty("summary", $desc . "\n");
                                  },
    }
);
