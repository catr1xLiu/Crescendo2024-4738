[![Game Manual](https://soflofrc.firstinflorida.org/wp-content/uploads/sites/23/2023/09/FIRST-IN-SHOW_CRESCENDO_FRC_SocialHQPDP_FB_Cover-1.png)](https://firstfrc.blob.core.windows.net/frc2024/Manual/2024GameManual.pdf)

[`src/main/java/frc/robot`](src/main/java/frc/robot) shortcut
![Robot Image](images/robot.gif)
____

# _**The Patribots (FRC 4738)**_
### Visit our website at [patribots.org](https://www.patribots.org)!

The Patribots are a school-based _FIRST&reg; Robotics Competition_ team from Patrick Henry High School, located in San Diego, California. 


This repository is entirely student-created and maintained.
Attached to this repository is a project called [Crescendo 2024](<https://github.com/orgs/Patribots4738/projects/3>) which we utilize as the Agile framework to organize our workflow. Using Agile, we map out the season by dividing it into nine week long sprints. As a team, we agree upon & decide what must be accomplished in each sprint. We declare each assignment by making issues and then implement it in an associated branch. An estimated priority & size are assigned to each issue/assignment which is then filtered into five categories:
  - **Backlog** -> Issues that have no status. Essentially a large to-do list.
  - **Ready** -> Issues that are assigned to a programmer & are ready to begin.
  - **In Progress** -> Issues that are currently being worked on by a programmer.
  - **In Review** -> Issues where the assigned programmer has requested revision by colleagues.
  - **Done** -> Resolved issues with corresponding branches which have merged into our master branch called [`main`](https://github.com/Patribots4738/Crescendo2024/tree/main/src/main).

We also love [drawing boards](<https://www.tldraw.com/r/EolJKYU3QEqxw71uyAqPS?viewport=5486,-1359,3403,1540&page=page:9NTiPVa29oqzjElya5D6n>)!
    
We are a team of students, for students, and we are proud to be a part of the _FIRST&reg;_ community.
Thanks for checking us out, & be sure to star this repository if you find anything helpful or interesting!

### [See how we did!](https://www.statbotics.io/team/4738)


___

## ✨ Highlights ✨
  - April Tag interpretation & note detection using two Limelights
  - Auto alignment to amp with auto shooting to the speaker while driving
  - Trap placement
  - Field-centric swerve drive
  - Modular autonomous routines using limelight
  - Under bumper intake
  - LEDs to communicate various actions
  - Fully simulated motors

## Simulation & Testing 🪄
  It is crucial to both mantain our equipment & our robot. Therefore, we always run our code in a simulation using FRC Driver Station, WPILibs's Sim GUI, and Advantage Scope to check if we either have any breaking changes or if our robot has expected behavior. After our design team has constructed the CAD model of our robot in OnShape, we can export it's moving parts into Advantage Scope. We then use a Json in our robot code to link the parts togethor so that they can move similarly to how they would in real life. Once we know our code works, we merge the branch it was on into the main branch to use on the real robot. This is process is used with most of our code yet was especially crucial for code involving critical math (ie. auto alignment & shooting while driving).

## Discretize Swerve 🛞
  Our robot uses REV Neo swerve drive. Over time as the robot moves and turns, the motors meausered states do not match it's actual state, causing the robot to drift in the direction of its rotation and have a curved path. We wanted the trajectory to be straighter, so we collaborated with other teams to discretize the swerve's motion using 2nd order kinematics. You can learn more about the evolution about it here: [Swerve Drive Skew and Second Order Kinematics - Chief Delphi](<https://www.chiefdelphi.com/t/whitepaper-swerve-drive-skew-and-second-order-kinematics/416964/33>) Our disretize swerve code was then implemented into WPILibs resources!

## Autonomous 🤖
  ### Path Generation & Modular Autonomous Paths 📈
  We use PathPlanner to construct a modular autonomous. In PathPlanner, we use waypoints, scheduled commands, & bezier curves to generate a singular auto path between a starting position, preferable shooting position, or note location. We then link multiple auto paths together to make one predetermined autonomous. Using note detection & logic, also allows us to make a modular autonomous path that can prevent us from going to a note position that has no note detected. This allows the robot to prioritize going to locations with notes. Additionaly, there is no predetermined starting position when starting a chain of autonomous paths. This year's abundance of april tags allow us the generate a path from anywhere in the starting zone as long as our limelight has a clear view of a tag. Feel free to check out our [Modular Auto Drawing Board](<https://www.tldraw.com/v/mBaJ6QzdW6wNaRUvmB3DW?viewport=-121,-188,2715,1378&page=page:page>) :>

  ### Note Detection 👀
  Using Limelight & machine learning, we can detect notes from 13 feet away. Note detection is incorporated in our modular autonomous logic, allowing us to skip over a note location if no note is detected. Those opposing robots are fast!


## Teleoperated 🎮
  ### Shooting While Driving
  Our robot is able to shoot notes into the speaker while moving sideways when relative to it. This feature grants us a shorter cycle time. If you are curious to learn more, check out the [Math](<https://www.tldraw.com/v/mBaJ6QzdW6wNaRUvmB3DW? viewport=-121,-188,2715,1378&page=page:page>) :D
  
  ### Field Centric Swerve Drive
  To make the robot more user-friendly for the driver, our swerve drive is field centric using our Pigeon 2.0 gyroscope to get our orientation on the field.
  
  ### Auto Alignment w/ April Tags
  Another feature for user-friendliness is our robot's ability to align to field objects such as the speaker, stage & amp. When aligned to the speaker, the driver can move the robot anywhere on the field whilst the shooter always faces the speaker. For the amp & stage, the robot becomes locked in a certain axis. This helps the driver with steering & alignment.

<img src="https://github.com/Patribots4738/Crescendo2024/assets/148731136/d47db1e3-67e7-4c6d-ab01-7f75e807eed5" height="400">
![Screenshot 2024-03-14 165424](https://github.com/Patribots4738/Crescendo2024/assets/148731136/73e776c2-dc3e-4e8b-9221-655e205a701a)



_____

## Major Class Functions 🤩
<img src="https://github.com/Patribots4738/Crescendo2024/assets/148731136/5d6d1ea1-1e16-48b8-b9d4-facfed37a290" width="150" height="150">

Our code is formatted in a <ins>command-based</ins> system on VS Code using <ins>Java<ins/>.
 
  - **Subsystems** [`robot/subsystems`](src/main/java/frc/robot/subsystems) Folder containing class file for each subsystem on the robot.
    - **Intake** [`robot/subsystems/intake`](src/main/java/frc/robot/subsystems/intake) An under-the-bumper intake which is run by a Neo 550
    - **Swerve** [`robot/subsystems/swerve`](src/main/java/frc/robot/subsystems/swerve) Drivetrain with four swerve modules using 4 Neo Vortexs and 4 Neo 550s.
    - **Shooter** [`robot/subsystems/shooter`](src/main/java/frc/robot/subsystems/shooter) A shooter that uses 2 Neo Vortexs & pivot which uses 1 Neo 550 with an absolute encoder.
    - **Elevator** [`robot/subsystems/elevator`](src/main/java/frc/robot/subsystems/elevator) Elevator for amp & trap placement which uses 1 Neo v1.1.
    - **Indexer** [`robot/subsystems/indexer`](src/main/java/frc/robot/subsystems/indexer) The Indexer between intake & shooter which uses a Neo 550.
    - **Climb** [`robot/subsystems/climb`](src/main/java/frc/robot/subsystems/climb) Two independently driven climbs that conform to the curve of the unoccupied chain on stage to keep the robot level with the ground. Uses one Neo Vortex each.
    - **LEDS** [`robot/subsystems/limelight`](src/main/java/frc/robot/subsystems/leds) Our WS2812B LED strip.
    - **Limelight** [`robot/subsystems/limelight`](src/main/java/frc/robot/subsystems/limelight) Interaction between the Limelight 2/3 and the robot.
      

   - **Commands** [`robot/commands`](src/main/java/frc/robot/commands) Fodler containing command files that control the robot.
     - **LED Position Indicator (LPI)** [`robot/commands/lpi`](src/main/java/frc/robot/commands/misc/lpi) Command created to aid the technician in placing the robot on the field before auto. The command cycles through the autonomous starting positions listed in [`robot/commands/autonomous/PathPlannerStorage`](src/main/java/frc/robot/commands/autonomous/PathPlannerStorage) & isolates the position closest to the robot's current position. It separates the surrounding area into defined zones & changes the LEDs to reflect the zone it is currently in. Once it is in the correct position, it displays a pattern to help the technician rotate the robot in the correct direction.
     - **Alignment Commands** [`robot/commands/subsystemHelpers/alignmentCmds`](src/main/frc/robot/commands/subsystemHelpers/alignmentCmds) File of commands that help that auto-align the robot to be locked in a certain axis when relative to a field object such as the stage or amp. This helps the driver with steering & alignment.
     - **Network Table PID Tuner** [`robot/commands/subsystemHelpers/NTPIDTuner`](src/main/frc/robot/commands/subsystemHelpers/NTPIDTuner) Command file that allows us to alter & tune the PID values in Advantage Scope's Network Tables for ease of access.\n\n
 
  - **Utilities** [`robot/util`](src/main/java/frc/robot/util)
    - **Constants** [`robot/util/constants`](src/main/java/frc/robot/util/constants) contains constants used throughout the robot code to prevent a mismatch in data & hardcoding values (Ex. PIDFF values & current limits).
    - **Calc** [`robot/util/calc`](src/main/java/frc/robot/util/calc) contains the calculations required for pivot alinment & shooter speeds when shooting while driving.
   
## Controls 🎮
[![Driver](https://github.com/Patribots4738/Crescendo2024/assets/65139378/d2d0000f-54cb-42d2-9b8a-7e635ca79c18)](https://www.tldraw.com/r/EolJKYU3QEqxw71uyAqPS?viewport=2339,251,5877,2813&page=page:page)

[![Operator](https://github.com/Patribots4738/Crescendo2024/assets/65139378/5040695f-deac-4ac9-9ad3-ca7cbfa4748b)](https://www.tldraw.com/r/EolJKYU3QEqxw71uyAqPS?viewport=2339,251,5877,2813&page=page:page)

## Components & Tools 🛠️
[![Hardware](https://github.com/Patribots4738/Crescendo2024/assets/65139378/f9de2a2e-e401-4d6b-b57a-10bbf2dfd340)](https://www.tldraw.com/r/EolJKYU3QEqxw71uyAqPS?viewport=-4823,-6599,9853,4716&page=page:g60UEEXm6O2yBIoLYfVVB)
  
[![Software](https://github.com/Patribots4738/Crescendo2024/assets/148731136/058e53cd-83ff-4463-ba4f-3b58a56a3ead)](https://www.tldraw.com/r/EolJKYU3QEqxw71uyAqPS?viewport=-4823,-6599,9853,4716&page=page:g60UEEXm6O2yBIoLYfVVB)
