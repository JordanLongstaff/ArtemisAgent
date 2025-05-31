# Artemis Agent 1.3.0

## What is Artemis Agent?

Artemis Agent is an Android app designed to be used as a partial Comms client for Artemis Spaceship
Bridge Simulator. It is the successor to Artemis Messenger, which received and parsed incoming Comms
messages about side missions and organized the information into a neat, accessible and readable
table, making it much easier to keep track of what side missions were available, what rewards they
offered, and how much progress had been made. Later, it was expanded to include status details
reported from ally ships and stations as well, and later still, to also calculate efficient routes
to complete all side missions and assist ally ships that needed help.

Artemis Messenger has become obsolete, and Artemis Agent has taken its place. It restores all the
features of its predecessor and adds new ones. Users can toggle red alert status, activate secret
code cases, send commands to ally ships, track BioMechs and remotely freeze them for a limited time,
track scanned enemy ships, taunt them and request surrenders. The UI has been greatly modernized to
take advantage of Android's newer capabilities.

## Stations

At the start of a simulation, the app will send status request messages to friendly stations. These
messages are resent when certain events occur, such as docking, undocking, and the completion of
missile production, but they can also be resent manually by pressing the Status button. The
information shown in the Stations view includes shield strength, direction and range, replacement
fighters on board, ordnance stocks, production speed, the type of ordnance that is currently being
built, and how much longer its production is expected to take. Pressing the Standby button requests
that the station stand by for docking. You can also choose what type of ordnance the station should
start to build as well as which station has its information displayed.

## Allies

The app also sends hail messages to ally ships at the start of a simulation, which can be resent
manually by pressing the Hail button. Information on ally ships is displayed in a scrollable list,
including shield strengths, direction, range and current status. Commands can also be sent to an
ally ship by pressing the Command button, including directions to move, turn, rendezvous with
another ship or station, or attack a nearby enemy.

In Deep Strike mode, there is only one ally ship; as such, the command buttons will always be shown.

## Missions

A side mission is a task within a simulation that entails the transport of supplies or data from one
location to another, with a reward offered for doing so. Generally, when the player receives a
message about a new side mission, the message begins with the line "Help us help you". It then lists
specifics about the task itself, namely the location to visit first and the reward that is offered
by the sender of the message; the ship or station that sent the message is the location to visit
after the location mentioned in the content of the message. When one of the two locations is visited
in order, that location sends a message that is parsed by the app, and the app updates the status of
the side mission accordingly. Side missions in the list show where to go to progress their
completion, including direction and range, along with the rewards.

There are five types of rewards: Battery Charge (extra energy), Extra Coolant (extra coolant for
Engineering), Nuclear Missiles (two Nuke torpedoes), Production Speed (doubled at the station that
issued the mission) and Shield Boost (stronger front and rear shields). The side missions in the
list can be filtered by their type by going to the Settings page.

## Route

There are two ways to configure the Route view. If it is configured for tasks, the view will list
all the stops to make to complete all side missions and assist ally ships with their needs as
quickly and efficiently as possible. If it is configured for supplies (e.g. restocking on missiles
or replacing lost single-seat craft), the view will simply list the stations that have the required
supplies in order of proximity. Whether you use the Route view or not, communication between the
Helm, Science and Comms officers is key to effective and efficient acquisition of rewards.

## Enemies and BioMechs

The app also lists enemy ships that have been scanned by Science in a scrollable list. Each entry in
the list has buttons for requesting an enemy's surrender or accessing taunts. If the enemy has been
scanned twice, intel will also be displayed to guide the user away from ineffectual taunts.

Similarly, there is also a list for BioMechs. The entries in this list can be used to temporarily
freeze BioMechs just by tapping them.

## How does the app do all of this?

The app communicates with a running Artemis server and receives packets from that server, which it
then parses to obtain all of the information it uses. You can connect to a server if you know its
address, but if the server is running Artemis version 2.7.0 or later, the app can broadcast a server
discovery request and find a server address that way.

## Can I contribute to this project?

Absolutely! This project is open-source, so you can fork it and make PRs. If you have any issues or
detect any bugs, you can report them in the Issues section on GitHub.

## Credits

This app uses [IAN](http://github.com/rjwut/ian) (Interface for Artemis Networking), a Java library written by Robert J.
Walker. It has been adapted for various purposes, including backwards compatibility with Artemis
2.3.0 and later. The 3D modeling features have been removed as they are not needed and are not
compatible with Android.
