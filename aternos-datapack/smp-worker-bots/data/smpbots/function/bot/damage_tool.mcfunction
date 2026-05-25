scoreboard players remove @s sb_dur 1
execute if score @s sb_dur matches ..0 run scoreboard players set @s sb_tool 0
execute if score @s sb_dur matches ..0 run item replace entity @s weapon.mainhand with minecraft:air
