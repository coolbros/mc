scoreboard objectives add sb_logs dummy
scoreboard objectives add sb_planks dummy
scoreboard objectives add sb_sticks dummy
scoreboard objectives add sb_table dummy
scoreboard objectives add sb_cobble dummy
scoreboard objectives add sb_iron dummy
scoreboard objectives add sb_mined dummy
scoreboard objectives add sb_tool dummy
scoreboard objectives add sb_dur dummy
scoreboard objectives add sb_phase dummy
scoreboard objectives add sb_run dummy
scoreboard objectives add sb_tmp dummy
scoreboard players set #clock sb_tmp 0
tellraw @a [{"text":"[SMPBots] loaded. Use "},{"text":"/function smpbots:spawn","color":"green"},{"text":" to spawn one worker bot."}]
