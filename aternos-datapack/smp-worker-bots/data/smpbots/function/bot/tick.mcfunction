function smpbots:bot/craft
execute if score @s sb_phase matches 0 run function smpbots:bot/wood
execute if score @s sb_phase matches 1 run function smpbots:bot/mine
function smpbots:bot/name
