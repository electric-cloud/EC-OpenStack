#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

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

        id =>          "extendvolume",
        pattern =>     q{^Volume extended to size\s(.+).},
        action =>           q{

                                my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                $desc .= "Volume extended to size of \'$1\' GB(s).";

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

                                  $desc = "Stack \'$1\' created.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "updatestack",
             pattern =>     q{^Stack\s(.+)\supdated.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc = "Stack \'$1\' updated.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
             id =>          "deletestack",
             pattern =>     q{^Stack\s(.+)\sdeleted.},
             action =>           q{

                                  my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                  $desc = "Stack \'$1\' deleted.";

                                  setProperty("summary", $desc . "\n");
                                  },
    },
    {
                 id =>          "getIntermediateStatus",
                 pattern =>     q{("status":\s)"([a-zA-Z]+)"},
                 action =>           q{

                                      my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                      $desc = "Status : \'$2\' .";

                                      $::gCommander->setProperty("/myJobStep/summary", $desc . "\n");
                                      },
    },
    {
                 id =>          "getStackStatus",
                 pattern =>     q{"stack_status":\s"([a-zA-Z_]+)"},
                 action =>           q{

                                       my $desc = ((defined $::gProperties{"summary"}) ? $::gProperties{"summary"} : '');

                                       $desc = "Stack Status : \'$1\' .";

                                       setProperty("summary", $desc . "\n");
                                       },
    },
    {
                 id =>          "error",
                 pattern =>     q{ERROR\s:|[Ee]rror\s:},
                 action =>      q{
                                    incValue("errors"); diagnostic("", "error", -1);

                                 },
    }
);
