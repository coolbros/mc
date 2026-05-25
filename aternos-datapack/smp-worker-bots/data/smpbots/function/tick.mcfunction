scoreboard players add #clock sb_tmp 1
execute if score #clock sb_tmp matches 5.. run execute as @e[type=armor_stand,tag=smpbot.worker,scores={sb_run=1..}] at @s run function smpbots:bot/tick
execute if score #clock sb_tmp matches 5.. run scoreboard players set #clock sb_tmp 0
