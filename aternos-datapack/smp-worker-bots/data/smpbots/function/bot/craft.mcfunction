execute if score @s sb_planks matches ..8 if score @s sb_logs matches 1.. run function smpbots:bot/craft_planks
execute if score @s sb_table matches 0 if score @s sb_planks matches 4.. run function smpbots:bot/craft_table
execute if score @s sb_sticks matches ..1 if score @s sb_planks matches 2.. run function smpbots:bot/craft_sticks
execute unless score @s sb_tool matches 2 if score @s sb_cobble matches 3.. if score @s sb_sticks matches 2.. run function smpbots:bot/craft_stone_pick
execute if score @s sb_tool matches 0 if score @s sb_table matches 1.. if score @s sb_planks matches 3.. if score @s sb_sticks matches 2.. run function smpbots:bot/craft_wood_pick
execute if score @s sb_tool matches 1.. run scoreboard players set @s sb_phase 1
execute if score @s sb_tool matches 0 run scoreboard players set @s sb_phase 0
