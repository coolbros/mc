summon armor_stand ~ ~ ~ {Tags:["smpbot.worker"],CustomName:'{"text":"SMPBot","color":"gold"}',CustomNameVisible:1b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b,Silent:1b,ShowArms:1b}
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_logs 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_planks 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_sticks 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_table 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_cobble 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_iron 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_mined 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_tool 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_dur 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_phase 0
scoreboard players set @e[type=armor_stand,tag=smpbot.worker,sort=nearest,limit=1] sb_run 1
tellraw @s [{"text":"Spawned an SMP worker bot. Place it near trees first, then it will craft tools and mine nearby stone/iron.","color":"green"}]
