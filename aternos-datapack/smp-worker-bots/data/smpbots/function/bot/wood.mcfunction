execute if block ~ ~ ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~ ~ #minecraft:logs run setblock ~ ~ ~ minecraft:air
execute if block ~1 ~ ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~1 ~ ~ #minecraft:logs run setblock ~1 ~ ~ minecraft:air
execute if block ~-1 ~ ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~-1 ~ ~ #minecraft:logs run setblock ~-1 ~ ~ minecraft:air
execute if block ~ ~ ~1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~ ~1 #minecraft:logs run setblock ~ ~ ~1 minecraft:air
execute if block ~ ~ ~-1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~ ~-1 #minecraft:logs run setblock ~ ~ ~-1 minecraft:air
execute if block ~ ~1 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~1 ~ #minecraft:logs run setblock ~ ~1 ~ minecraft:air
execute if block ~1 ~1 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~1 ~1 ~ #minecraft:logs run setblock ~1 ~1 ~ minecraft:air
execute if block ~-1 ~1 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~-1 ~1 ~ #minecraft:logs run setblock ~-1 ~1 ~ minecraft:air
execute if block ~ ~1 ~1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~1 ~1 #minecraft:logs run setblock ~ ~1 ~1 minecraft:air
execute if block ~ ~1 ~-1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~1 ~-1 #minecraft:logs run setblock ~ ~1 ~-1 minecraft:air
execute if block ~ ~2 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~2 ~ #minecraft:logs run setblock ~ ~2 ~ minecraft:air
execute if block ~1 ~2 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~1 ~2 ~ #minecraft:logs run setblock ~1 ~2 ~ minecraft:air
execute if block ~-1 ~2 ~ #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~-1 ~2 ~ #minecraft:logs run setblock ~-1 ~2 ~ minecraft:air
execute if block ~ ~2 ~1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~2 ~1 #minecraft:logs run setblock ~ ~2 ~1 minecraft:air
execute if block ~ ~2 ~-1 #minecraft:logs run scoreboard players add @s sb_logs 1
execute if block ~ ~2 ~-1 #minecraft:logs run setblock ~ ~2 ~-1 minecraft:air
execute if score @s sb_logs matches ..3 run tp @s ~0.4 ~ ~
