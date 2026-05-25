execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:stone run function smpbots:bot/damage_tool
execute if block ~ ~ ~ minecraft:stone run setblock ~ ~ ~ minecraft:air
execute if score @s sb_tool matches 1.. if block ~ ~-1 ~ minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~ ~-1 ~ minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~ ~-1 ~ minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~ ~-1 ~ minecraft:stone run function smpbots:bot/damage_tool
execute if block ~ ~-1 ~ minecraft:stone run setblock ~ ~-1 ~ minecraft:air
execute if score @s sb_tool matches 1.. if block ~1 ~ ~ minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~1 ~ ~ minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~1 ~ ~ minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~1 ~ ~ minecraft:stone run function smpbots:bot/damage_tool
execute if block ~1 ~ ~ minecraft:stone run setblock ~1 ~ ~ minecraft:air
execute if score @s sb_tool matches 1.. if block ~-1 ~ ~ minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~-1 ~ ~ minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~-1 ~ ~ minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~-1 ~ ~ minecraft:stone run function smpbots:bot/damage_tool
execute if block ~-1 ~ ~ minecraft:stone run setblock ~-1 ~ ~ minecraft:air
execute if score @s sb_tool matches 1.. if block ~ ~ ~1 minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~1 minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~1 minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~1 minecraft:stone run function smpbots:bot/damage_tool
execute if block ~ ~ ~1 minecraft:stone run setblock ~ ~ ~1 minecraft:air
execute if score @s sb_tool matches 1.. if block ~ ~ ~-1 minecraft:stone run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~-1 minecraft:stone run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~-1 minecraft:stone run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~-1 minecraft:stone run function smpbots:bot/damage_tool
execute if block ~ ~ ~-1 minecraft:stone run setblock ~ ~ ~-1 minecraft:air
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:deepslate run scoreboard players add @s sb_cobble 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:deepslate run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:deepslate run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 1.. if block ~ ~ ~ minecraft:deepslate run function smpbots:bot/damage_tool
execute if block ~ ~ ~ minecraft:deepslate run setblock ~ ~ ~ minecraft:air
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:iron_ore run scoreboard players add @s sb_iron 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:iron_ore run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:iron_ore run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:iron_ore run function smpbots:bot/damage_tool
execute if block ~ ~ ~ minecraft:iron_ore run setblock ~ ~ ~ minecraft:air
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:deepslate_iron_ore run scoreboard players add @s sb_iron 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:deepslate_iron_ore run scoreboard players add @s sb_mined 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:deepslate_iron_ore run scoreboard players set #mined sb_tmp 1
execute if score @s sb_tool matches 2 if block ~ ~ ~ minecraft:deepslate_iron_ore run function smpbots:bot/damage_tool
execute if block ~ ~ ~ minecraft:deepslate_iron_ore run setblock ~ ~ ~ minecraft:air
